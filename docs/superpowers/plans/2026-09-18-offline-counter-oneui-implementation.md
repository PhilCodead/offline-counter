# Offline Counter 0.14 One UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a clean, reproducible Kotlin Android Studio project whose accessibility overlay counts and exports people locally while presenting the approved Samsung One UI-inspired launcher, glass toolbar, interactions, and adaptive icon.

**Architecture:** Keep Android lifecycle and accessibility APIs in a thin service, move counting decisions into a pure Kotlin controller, and render immutable overlay state through a focused view. Use standard Android resources and public cross-window blur APIs with an opaque fallback; do not depend on unofficial Samsung libraries.

**Tech Stack:** Kotlin 2.0.21, Android Gradle Plugin 8.7.3, Gradle 8.9, JDK 17, Android Views, AppCompat 1.7.0, Material Components 1.12.0, JUnit 4.13.2, Android SDK 35.

**Spec:** `docs/superpowers/specs/2026-09-18-offline-counter-oneui-redesign.md`

## Global Constraints

- Namespace and application ID are exactly `app.offlinecounter`.
- Version code is `14`; version name is `0.14`.
- Minimum SDK is 29; compile and target SDK are 35; JVM target is 17.
- `android.permission.INTERNET` must remain absent.
- Do not add unofficial SESL, private Samsung libraries, analytics, networking, OCR, Compose, or cloud storage.
- All interactive overlay targets are at least 48 dp; the compact panel is approximately 64–68 dp high and the expanded panel is 320–360 dp wide within display bounds.
- Keep the attached XLSX template and MediaStore export flow.
- Remove dead resources, redundant implementation, and nonessential comments.
- Production behavior changes require a failing test first whenever the behavior can be isolated on the JVM.

## File map

- `app/src/main/java/app/offlinecounter/MainActivity.kt`: launcher lifecycle and accessibility-service status.
- `app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt`: platform event bridge and orchestration only.
- `app/src/main/java/app/offlinecounter/CountingController.kt`: pure state machine and bounded collection decisions.
- `app/src/main/java/app/offlinecounter/CountingModels.kt`: phases, commands, snapshots, and immutable overlay state.
- `app/src/main/java/app/offlinecounter/Person.kt`: immutable person and normalized key.
- `app/src/main/java/app/offlinecounter/PersonParser.kt`: accessibility text parsing.
- `app/src/main/java/app/offlinecounter/ScrollTracker.kt`: scroll-boundary signals.
- `app/src/main/java/app/offlinecounter/ExcelExporter.kt`: MediaStore persistence only.
- `app/src/main/java/app/offlinecounter/WorkbookBuilder.kt`: pure XLSX sheet generation and template replacement.
- `app/src/main/java/app/offlinecounter/OverlayPanel.kt`: view construction, callbacks, and state rendering.
- `app/src/main/java/app/offlinecounter/OverlayPlacement.kt`: pure clamping and nearest-edge calculations.
- `app/src/main/java/app/offlinecounter/OverlayPreferences.kt`: persisted normalized overlay position.
- `app/src/main/java/app/offlinecounter/WindowBlurController.kt`: Android 12+ blur availability and fallback opacity.
- `app/src/main/res/layout/activity_main.xml`: One UI-inspired setup/status screen.
- `app/src/main/res/values/{strings,colors,dimens,themes,styles}.xml`: centralized visual and copy resources.
- `app/src/main/res/drawable/*`: shapes, toolbar vectors, and adaptive icon foreground.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`: adaptive icon declaration.
- `app/src/test/java/app/offlinecounter/*Test.kt`: JVM coverage for pure logic.

---

### Task 1: Reproducible project and package migration

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Move: `app/src/main/java/com/example/offlinecounter/*.kt` to `app/src/main/java/app/offlinecounter/`
- Move: `app/src/test/java/com/example/offlinecounter/*.kt` to `app/src/test/java/app/offlinecounter/`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradle/wrapper/gradle-wrapper.properties`

**Interfaces:**
- Consumes: supplied 0.13 Android Studio project.
- Produces: buildable project with package `app.offlinecounter`, `BuildConfig.VERSION_NAME == "0.14"`, and wrapper command `./gradlew`.

- [ ] **Step 1: Add a failing manifest/build invariant test**

Create `app/src/test/java/app/offlinecounter/ProjectInvariantTest.kt`:

```kotlin
package app.offlinecounter

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectInvariantTest {
    @Test fun manifestIsOfflineAndUsesNewPackage() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains(".CounterAccessibilityService"))
    }
}
```

- [ ] **Step 2: Move packages and update build metadata**

Set the application block exactly as follows:

```kotlin
defaultConfig {
    applicationId = "app.offlinecounter"
    minSdk = 29
    targetSdk = 35
    versionCode = 14
    versionName = "0.14"
}
```

Change every Kotlin declaration to `package app.offlinecounter`, set `namespace = "app.offlinecounter"`, and update test paths accordingly.

- [ ] **Step 3: Generate and pin the wrapper**

Run:

```bash
gradle wrapper --gradle-version 8.9 --distribution-type bin
chmod +x gradlew
```

Expected: `gradle/wrapper/gradle-wrapper.properties` references `gradle-8.9-bin.zip`.

- [ ] **Step 4: Run the migrated baseline**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: all existing tests pass and a debug APK is produced.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle app
git commit -m "build: migrate app namespace and add Gradle wrapper"
```

---

### Task 2: Pure counting state machine

**Files:**
- Create: `app/src/main/java/app/offlinecounter/CountingModels.kt`
- Create: `app/src/main/java/app/offlinecounter/CountingController.kt`
- Create: `app/src/test/java/app/offlinecounter/CountingControllerTest.kt`

**Interfaces:**
- Consumes: `Person`, scroll boundary booleans, and visible parsed people.
- Produces: `CountingController.start()`, `onFrame(FrameSnapshot)`, `stop()`, `clear()`, `state: OverlayUiState`, and `CountingCommand`.

- [ ] **Step 1: Write failing transition tests**

```kotlin
class CountingControllerTest {
    private val controller = CountingController()

    @Test fun startRewindsThenCollects() {
        assertEquals(CountingCommand.ScrollBackward, controller.start())
        val command = controller.onFrame(FrameSnapshot(emptyList(), atTop = true, atBottom = false, fingerprint = 1))
        assertEquals(CountingCommand.Wait, command)
        assertEquals(CountingPhase.Collecting, controller.state.phase)
    }

    @Test fun collectionDeduplicatesAndCompletesAtBottom() {
        controller.start()
        controller.onFrame(FrameSnapshot(emptyList(), true, false, 1))
        val person = Person("Анна Смирнова", "01.01.1990", "Ж", "")
        val command = controller.onFrame(FrameSnapshot(listOf(person, person), false, true, 2))
        assertEquals(CountingCommand.Complete, command)
        assertEquals(1, controller.state.total)
        assertEquals(CountingPhase.Completed, controller.state.phase)
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.offlinecounter.CountingControllerTest`

Expected: compilation fails because the controller types do not exist.

- [ ] **Step 3: Implement minimal models and controller**

```kotlin
enum class CountingPhase { Idle, Rewinding, Collecting, Completed, Error }

sealed interface CountingCommand {
    data object ScrollBackward : CountingCommand
    data object ScrollForward : CountingCommand
    data object Wait : CountingCommand
    data object Complete : CountingCommand
    data class Fail(val message: String) : CountingCommand
}

data class FrameSnapshot(
    val people: List<Person>,
    val atTop: Boolean,
    val atBottom: Boolean,
    val fingerprint: Int,
)

data class OverlayUiState(
    val phase: CountingPhase = CountingPhase.Idle,
    val total: Int = 0,
    val women: Int = 0,
    val men: Int = 0,
    val status: String = "",
    val expanded: Boolean = false,
)
```

Implement bounded constants `MAX_REWIND_STEPS = 350`, `MAX_SCROLL_STEPS = 1000`, `MAX_STALE_PASSES = 15`, and keep deduplicated people in a `LinkedHashMap<String, Person>`.

- [ ] **Step 4: Extend tests for stop, clear, retry, stale, and error paths**

Add assertions that `stop()` returns to `Idle` without clearing results, `clear()` empties results, repeated fingerprints finish safely, and bounds cannot be exceeded. Run the full controller test until green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/offlinecounter/CountingModels.kt app/src/main/java/app/offlinecounter/CountingController.kt app/src/test/java/app/offlinecounter/CountingControllerTest.kt
git commit -m "feat: extract bounded counting controller"
```

---

### Task 3: Parser and scroll hardening

**Files:**
- Modify: `app/src/main/java/app/offlinecounter/Person.kt`
- Modify: `app/src/main/java/app/offlinecounter/PersonParser.kt`
- Modify: `app/src/main/java/app/offlinecounter/ScrollTracker.kt`
- Modify: `app/src/test/java/app/offlinecounter/PersonParserTest.kt`
- Modify: `app/src/test/java/app/offlinecounter/ScrollTrackerTest.kt`

**Interfaces:**
- Consumes: ordered accessibility text and scroll event fields.
- Produces: `parse(text: List<String>): List<Person>`, `isAtTop()`, `isAtBottom()`, and `reset()`.

- [ ] **Step 1: Add failing parser edge-case tests**

```kotlin
@Test fun rejectsMalformedDatesAndPreservesStatus() {
    val people = parser.parse(listOf(
        "Анна Смирнова ж, 34 года (31-12-1990)",
        "Ирина Волкова ж, 30 лет (01.02.1994)",
        "Не вакцинирован",
    ))
    assertEquals(1, people.size)
    assertEquals("Не вакцинирован", people.single().status)
}

@Test fun normalizedKeyCollapsesWhitespaceAndCase() {
    assertEquals(
        Person(" Анна  Смирнова ", "01.01.1990", "Ж", "").key,
        Person("анна смирнова", "01.01.1990", "Ж", "").key,
    )
}
```

- [ ] **Step 2: Add failing scroll tests**

```kotlin
@Test fun incompleteValuesAreNotBoundaries() {
    val tracker = ScrollTracker()
    tracker.update(-1, -1, -1, -1, -1)
    assertFalse(tracker.isAtTop())
    assertFalse(tracker.isAtBottom())
}

@Test fun resetClearsEverySignal() {
    val tracker = ScrollTracker()
    tracker.update(0, 9, 10, 0, 100)
    tracker.reset()
    assertFalse(tracker.isAtTop())
    assertFalse(tracker.isAtBottom())
}
```

- [ ] **Step 3: Run targeted tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.offlinecounter.PersonParserTest --tests app.offlinecounter.ScrollTrackerTest`

Expected: normalized whitespace test fails before implementation.

- [ ] **Step 4: Implement normalization and explicit boundary logic**

Normalize names with `trim().split(Regex("\\s+")).joinToString(" ")`, use the normalized name for `fio` and `key`, and rewrite boundaries with explicit parentheses:

```kotlin
fun isAtTop() = (itemCount > 0 && fromIndex == 0) || (maxScrollY > 0 && scrollY == 0)
fun isAtBottom() = (itemCount > 0 && toIndex == itemCount - 1) || (maxScrollY > 0 && scrollY >= maxScrollY)
```

Run the targeted tests until green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/offlinecounter/Person.kt app/src/main/java/app/offlinecounter/PersonParser.kt app/src/main/java/app/offlinecounter/ScrollTracker.kt app/src/test/java/app/offlinecounter
git commit -m "refactor: harden parsing and scroll boundaries"
```

---

### Task 4: Testable XLSX generation

**Files:**
- Create: `app/src/main/java/app/offlinecounter/WorkbookBuilder.kt`
- Modify: `app/src/main/java/app/offlinecounter/ExcelExporter.kt`
- Create: `app/src/test/java/app/offlinecounter/WorkbookBuilderTest.kt`

**Interfaces:**
- Consumes: XLSX template bytes and `Collection<Person>`.
- Produces: `WorkbookBuilder.sheetXml(people): String`, `replaceSheet(template, people): ByteArray`, and `isValid(bytes): Boolean`; `ExcelExporter.export(context, people): Uri` persists output.

- [ ] **Step 1: Write failing workbook tests**

```kotlin
class WorkbookBuilderTest {
    @Test fun escapesXmlAndWritesTotals() {
        val person = Person("Иванов & Петров", "01.01.1990", "М", "<готов>")
        val xml = WorkbookBuilder.sheetXml(listOf(person))
        assertTrue(xml.contains("Иванов &amp; Петров"))
        assertTrue(xml.contains("&lt;готов&gt;"))
        assertTrue(xml.contains("Общее: 1"))
        assertTrue(xml.contains("М: 1"))
        assertTrue(xml.contains("Ж: 0"))
    }

    @Test fun emptyDataStillWritesBothSexTotals() {
        val xml = WorkbookBuilder.sheetXml(emptyList())
        assertTrue(xml.contains("М: 0"))
        assertTrue(xml.contains("Ж: 0"))
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.offlinecounter.WorkbookBuilderTest`

Expected: compilation fails because `WorkbookBuilder` does not exist.

- [ ] **Step 3: Move pure workbook logic**

Move XML generation, ZIP replacement, validation, and XML escaping from `ExcelExporter` into `WorkbookBuilder`. Compute sex totals once before `buildString`; do not call `count` for every row.

- [ ] **Step 4: Keep exporter focused on MediaStore and verify**

`ExcelExporter.export` opens the template, calls `WorkbookBuilder.replaceSheet`, validates, inserts a pending download, writes it, clears pending state, and deletes the URI on any failure. Run `./gradlew testDebugUnitTest` and expect all tests to pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/offlinecounter/WorkbookBuilder.kt app/src/main/java/app/offlinecounter/ExcelExporter.kt app/src/test/java/app/offlinecounter/WorkbookBuilderTest.kt
git commit -m "refactor: isolate and test XLSX generation"
```

---

### Task 5: Overlay state rendering and action model

**Files:**
- Modify: `app/src/main/java/app/offlinecounter/CountingModels.kt`
- Rewrite: `app/src/main/java/app/offlinecounter/OverlayPanel.kt`
- Create: `app/src/test/java/app/offlinecounter/OverlayUiStateTest.kt`
- Create: `app/src/main/res/values/dimens.xml`
- Create: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/drawable/overlay_compact_background.xml`
- Create: `app/src/main/res/drawable/overlay_expanded_background.xml`
- Create: `app/src/main/res/drawable/overlay_button_background.xml`
- Replace: `app/src/main/res/drawable/ic_share.xml`
- Replace: `app/src/main/res/drawable/ic_expand_more.xml`
- Delete: `app/src/main/res/drawable/ic_expand_less.xml`

**Interfaces:**
- Consumes: immutable `OverlayUiState`.
- Produces: `OverlayPanel.render(state)`, `setExpanded(expanded, animate)`, and callbacks in `OverlayActions`.

- [ ] **Step 1: Write failing presentation tests**

Add pure presentation properties to `OverlayUiState` and test them:

```kotlin
@Test fun primaryLabelReflectsPhaseAndCount() {
    assertEquals("Подсчёт", OverlayUiState().primaryLabel)
    assertEquals("42", OverlayUiState(phase = CountingPhase.Collecting, total = 42).primaryLabel)
    assertEquals("Готово", OverlayUiState(phase = CountingPhase.Completed, total = 42).primaryLabel)
}

@Test fun destructiveActionsRequireConfirmation() {
    assertTrue(OverlayAction.Clear.requiresConfirmation)
    assertTrue(OverlayAction.Disable.requiresConfirmation)
    assertFalse(OverlayAction.Share.requiresConfirmation)
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.offlinecounter.OverlayUiStateTest`

Expected: missing presentation properties and `OverlayAction`.

- [ ] **Step 3: Implement state mapping and resources**

Create `OverlayAction { Count, Export, Share, Expand, StopOrRecount, Clear, Disable }` with `requiresConfirmation`. Move all visible strings and dimensions to resources. Define `overlay_touch_target = 48dp`, `overlay_compact_height = 68dp`, `overlay_expanded_width = 344dp`, `overlay_corner_radius = 26dp`.

- [ ] **Step 4: Rewrite panel rendering**

Build the approved compact row: drag handle, blue primary button, clearly filled/outlined Excel button, conventional three-node Share button, and one chevron view rotated `0f/180f`. Expanded content shows status and totals plus stop/recount, clear, and disable. `render` updates views only from state; it does not decide collection transitions.

Use `ViewPropertyAnimator.rotation()` and `TransitionManager.beginDelayedTransition()` only when system animation scale permits. Set content descriptions for every icon-only action and keep all targets at least 48 dp.

- [ ] **Step 5: Verify and commit**

Run: `./gradlew testDebugUnitTest lintDebug`

Expected: tests pass and lint reports no missing content descriptions or string-externalization errors.

```bash
git add app/src/main/java/app/offlinecounter/CountingModels.kt app/src/main/java/app/offlinecounter/OverlayPanel.kt app/src/main/res app/src/test/java/app/offlinecounter/OverlayUiStateTest.kt
git commit -m "feat: build accessible One UI glass toolbar"
```

---

### Task 6: Placement, edge snapping, and persistence

**Files:**
- Create: `app/src/main/java/app/offlinecounter/OverlayPlacement.kt`
- Create: `app/src/main/java/app/offlinecounter/OverlayPreferences.kt`
- Create: `app/src/test/java/app/offlinecounter/OverlayPlacementTest.kt`
- Modify: `app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt`

**Interfaces:**
- Consumes: raw coordinates, overlay/display dimensions, safe insets, and stored normalized coordinates.
- Produces: `clamp(position, bounds)`, `nearestEdge(position, bounds)`, `normalize(position, bounds)`, and `denormalize(saved, bounds)`.

- [ ] **Step 1: Write failing geometry tests**

```kotlin
class OverlayPlacementTest {
    private val bounds = OverlayBounds(width = 1080, height = 2200, panelWidth = 600, panelHeight = 160, insetTop = 80, insetBottom = 120)

    @Test fun clampsPanelInsideVisibleBounds() {
        assertEquals(OverlayPosition(0, 80), OverlayPlacement.clamp(OverlayPosition(-50, -20), bounds))
        assertEquals(OverlayPosition(480, 1920), OverlayPlacement.clamp(OverlayPosition(900, 2200), bounds))
    }

    @Test fun choosesNearestHorizontalEdge() {
        assertEquals(OverlayEdge.Start, OverlayPlacement.nearestEdge(OverlayPosition(100, 500), bounds))
        assertEquals(OverlayEdge.End, OverlayPlacement.nearestEdge(OverlayPosition(450, 500), bounds))
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.offlinecounter.OverlayPlacementTest`

Expected: missing placement types.

- [ ] **Step 3: Implement pure placement math**

Use integer pixel coordinates for current placement and floats in `[0f, 1f]` for persistence. Clamp every result before returning. Store normalized `x/y` and `OverlayEdge.name` in private SharedPreferences through `OverlayPreferences`.

- [ ] **Step 4: Integrate drag, snap, restore, and display changes**

The service drag listener updates within bounds, snaps with a short decelerate animation on release, emits a single haptic tick, and persists after snapping. Recalculate bounds after layout and configuration changes. Long-press/drag is only attached to the handle so toolbar actions remain reliable.

- [ ] **Step 5: Verify and commit**

Run: `./gradlew testDebugUnitTest`

```bash
git add app/src/main/java/app/offlinecounter/OverlayPlacement.kt app/src/main/java/app/offlinecounter/OverlayPreferences.kt app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt app/src/test/java/app/offlinecounter/OverlayPlacementTest.kt
git commit -m "feat: persist and snap overlay position"
```

---

### Task 7: Cross-window blur with readable fallback

**Files:**
- Create: `app/src/main/java/app/offlinecounter/WindowBlurController.kt`
- Modify: `app/src/main/java/app/offlinecounter/OverlayPanel.kt`
- Modify: `app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt`
- Modify: `app/src/main/res/values/colors.xml`

**Interfaces:**
- Consumes: `WindowManager`, overlay `LayoutParams`, SDK level, blur availability, and expanded state.
- Produces: `attach()`, `apply(expanded)`, and `detach()`; calls `OverlayPanel.setBlurAvailable(Boolean)`.

- [ ] **Step 1: Define deterministic visual modes**

Add `GlassMode` to `CountingModels.kt` and a mapping test in `OverlayUiStateTest.kt`:

```kotlin
@Test fun opaqueFallbackWinsWhenBlurIsUnavailable() {
    assertEquals(GlassMode.CompactOpaque, glassMode(expanded = false, blurAvailable = false))
    assertEquals(GlassMode.ExpandedOpaque, glassMode(expanded = true, blurAvailable = false))
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.offlinecounter.OverlayUiStateTest`

Expected: `GlassMode` and `glassMode` are missing.

- [ ] **Step 3: Implement mapping and Android controller**

For SDK 31+, register `addCrossWindowBlurEnabledListener`, add/remove `FLAG_BLUR_BEHIND`, and call `setBlurBehindRadius`. Use moderate compact and stronger expanded radii, capped below 150 px. For older SDKs or disabled blur, clear the blur flag and select backgrounds with higher alpha.

- [ ] **Step 4: Integrate lifecycle and state changes**

Attach after the overlay window is added, call `apply(expanded)` on expansion changes, and remove the listener before the view/window is destroyed. Do not show an error when blur is unavailable.

- [ ] **Step 5: Verify and commit**

Run: `./gradlew testDebugUnitTest lintDebug`

```bash
git add app/src/main/java/app/offlinecounter/WindowBlurController.kt app/src/main/java/app/offlinecounter/CountingModels.kt app/src/main/java/app/offlinecounter/OverlayPanel.kt app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt app/src/main/res/values/colors.xml app/src/test/java/app/offlinecounter/OverlayUiStateTest.kt
git commit -m "feat: add adaptive cross-window blur"
```

---

### Task 8: Service/controller integration and safe confirmations

**Files:**
- Rewrite: `app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt`
- Modify: `app/src/main/java/app/offlinecounter/OverlayPanel.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: accessibility events, controller commands, parser results, tracker boundaries, and overlay actions.
- Produces: bounded scrolling, live overlay state, count/export/share flow, and idempotent cleanup.

- [ ] **Step 1: Add controller tests for unavailable source and auto-collapse**

```kotlin
@Test fun repeatedUnavailableFramesEndWithRecoverableError() {
    controller.start()
    repeat(20) { controller.onSourceUnavailable() }
    assertEquals(CountingPhase.Error, controller.state.phase)
}

@Test fun startCollapsesExpandedPanel() {
    controller.setExpanded(true)
    controller.start()
    assertFalse(controller.state.expanded)
}
```

- [ ] **Step 2: Run tests and verify RED, then implement controller methods**

Run the targeted controller test, add bounded unavailable ticks and `setExpanded`, then rerun until green.

- [ ] **Step 3: Replace duplicated service state with controller state**

The service owns only platform objects, target/last package names, scheduling, last export URI, and lifecycle. Each cycle builds `FrameSnapshot`, calls the controller, renders `controller.state`, and translates commands into accessibility actions and scheduling delays.

- [ ] **Step 4: Add confirmation UI for Clear and Disable**

Use a small themed `AlertDialog` or an overlay-contained confirmation state. Exact copy:

```text
Очистить результаты?
Собранные данные текущего подсчёта будут удалены.

Отключить панель?
Для повторного запуска потребуется снова включить службу специальных возможностей.
```

Do not confirm Count, Stop, Export, Share, Expand, or Recount.

- [ ] **Step 5: Verify and commit**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`

```bash
git add app/src/main/java/app/offlinecounter app/src/main/res/values/strings.xml app/src/test/java/app/offlinecounter/CountingControllerTest.kt
git commit -m "refactor: integrate controller with accessibility service"
```

---

### Task 9: One UI launcher screen and service status

**Files:**
- Rewrite: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/app/offlinecounter/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values/styles.xml`
- Create: `app/src/main/res/drawable/settings_group_background.xml`

**Interfaces:**
- Consumes: system accessibility-service enabled state.
- Produces: approved Samsung Settings-style launcher with status, privacy/export row, and primary settings action.

- [ ] **Step 1: Extract and test enabled-service matching**

Create `AccessibilityServiceStatus.kt` with a pure parser and test:

```kotlin
@Test fun matchesFlattenedEnabledServiceName() {
    assertTrue(AccessibilityServiceStatus.contains(
        enabled = "other.pkg/.Service:app.offlinecounter/.CounterAccessibilityService",
        packageName = "app.offlinecounter",
        className = "app.offlinecounter.CounterAccessibilityService",
    ))
}
```

- [ ] **Step 2: Run test and verify RED, then implement parser**

Split the enabled string on `:` and compare normalized `ComponentName` values. Run the targeted test until green.

- [ ] **Step 3: Build launcher layout**

Use a large two-line title, subtitle, rounded settings group with service and local-export rows, and a 56 dp primary button. Use system sans-serif, 24 dp horizontal padding, comfortable top whitespace, and light/dark resources. Remove the standalone close-app action as nonessential.

- [ ] **Step 4: Update activity lifecycle**

On resume, read `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`, update icon/text/action state, and open `ACTION_ACCESSIBILITY_SETTINGS` from the primary button with a restrained haptic tap.

- [ ] **Step 5: Verify and commit**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`

```bash
git add app/src/main/java/app/offlinecounter/AccessibilityServiceStatus.kt app/src/main/java/app/offlinecounter/MainActivity.kt app/src/main/res app/src/test/java/app/offlinecounter/AccessibilityServiceStatusTest.kt
git commit -m "feat: redesign launcher in One UI style"
```

---

### Task 10: Final adaptive icon and resource cleanup

**Files:**
- Replace: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Replace: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Replace: `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Delete: unused drawables identified by `aapt2`/resource lint.

**Interfaces:**
- Consumes: approved person-and-count icon direction.
- Produces: neutral cool slate blue-gray adaptive icon with white person and compact `12` notification badge, identical composition at launcher sizes.

- [ ] **Step 1: Add resource assertions**

Extend `ProjectInvariantTest`:

```kotlin
@Test fun adaptiveIconIncludesMonochromeAndCountBadge() {
    val adaptive = File("src/main/res/mipmap-anydpi-v26/ic_launcher.xml").readText()
    val foreground = File("src/main/res/drawable/ic_launcher_foreground.xml").readText()
    assertTrue(adaptive.contains("monochrome"))
    assertTrue(foreground.contains("launcher_badge"))
}
```

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew testDebugUnitTest --tests app.offlinecounter.ProjectInvariantTest`

Expected: existing resources contain neither monochrome layer nor `launcher_badge` grouping.

- [ ] **Step 3: Implement vector/adaptive resources**

Use a 108×108 viewport. Keep all meaningful foreground geometry inside the adaptive-icon safe zone. Draw the white outline person as paths and the compact white circular badge in the lower-right foreground; draw `12` as vector paths rather than a text element. Use a neutral slate background color/gradient approximation compatible with adaptive resources and keep the badge roughly 25–28% of icon width.

- [ ] **Step 4: Inspect launcher-size renders and remove dead resources**

Render or rasterize at 96, 64, 48, and 32 px. Verify that the person remains dominant, the badge does not merge into the person, and `12` is recognizable at 48 px. Run `./gradlew lintDebug` and delete only resources confirmed unused.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res app/src/test/java/app/offlinecounter/ProjectInvariantTest.kt
git commit -m "feat: add adaptive person count launcher icon"
```

---

### Task 11: Documentation, offline audit, and release verification

**Files:**
- Rewrite: `README.md`
- Create: `CHANGELOG_0.14.md`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: completed project and verification results.
- Produces: clean source archive with reproducible build instructions and audit evidence.

- [ ] **Step 1: Update documentation**

Document JDK 17, SDK 35, `./gradlew testDebugUnitTest lintDebug assembleDebug`, service setup, overlay controls, export location, blur fallback, and the no-Internet guarantee. Summarize 0.12 custom DEX source → 0.13 Kotlin port → 0.14 cleanup/redesign.

- [ ] **Step 2: Run the full verification suite**

Run:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

Expected: exit code 0, zero test failures, and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Audit manifest and APK permissions**

Run:

```bash
rg -n "INTERNET|java\.net|okhttp|retrofit|WebView" app/src || true
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

Expected: no `android.permission.INTERNET` and no networking stack references.

- [ ] **Step 4: Audit source cleanliness and archive inputs**

Run:

```bash
git status --short
git ls-files | rg -v '(^|/)(build|\.gradle|\.idea)/'
```

Expected: only intended source/docs are tracked; no build outputs, local properties, temporary designs, credentials, signing keys, or generated archives.

- [ ] **Step 5: Commit and package**

```bash
git add README.md CHANGELOG_0.14.md .gitignore
git commit -m "docs: document Offline Counter 0.14"
git archive --format=zip --output=offline-counter-android-studio-v14.zip HEAD
```

Verify the archive listing and SHA-256 before delivery.
