# Overlay Polish and Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a sharp, reliable floating panel, toast-based feedback, a native expandable setup guide, a direct accessibility-settings route, and the supplied blue launcher icon.

**Architecture:** Keep overlay state decisions in small pure Kotlin functions tested on the JVM, while Android views render those decisions. MainActivity owns the expandable onboarding card and delegates settings-intent selection to a pure request model. The service owns transient toasts and no longer installs a cross-window blur controller.

**Tech Stack:** Kotlin, Android SDK 29–35, Material Components, JUnit 4, Android adaptive icons.

**Spec:** `docs/superpowers/specs/2026-09-18-overlay-polish-and-onboarding.md`

## Global Constraints

- Namespace and application ID remain `app.offlinecounter`.
- The app remains offline and requests no internet permission.
- No cross-window blur or overlay is used while guiding the user through system Settings.
- Existing count, export, share, drag, snap, and saved-position behavior remains intact.

---

### Task 1: Lock overlay behavior with regression tests

**Files:**
- Modify: `app/src/test/java/app/offlinecounter/OverlayUiStateTest.kt`
- Modify: `app/src/main/java/app/offlinecounter/CountingModels.kt`
- Modify: `app/src/main/java/app/offlinecounter/OverlayPanel.kt`
- Modify: `app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt`
- Delete: `app/src/main/java/app/offlinecounter/WindowBlurController.kt`

**Interfaces:**
- Consumes: `OverlayAction.requiresConfirmation`, `glassMode(expanded, blurAvailable)`
- Produces: first-tap disable behavior and opaque compact/expanded surface modes

- [ ] **Step 1: Write failing tests** asserting `OverlayAction.Disable.requiresConfirmation == false` and that `glassMode` returns opaque modes even when blur is reported available.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests app.offlinecounter.OverlayUiStateTest` and verify both new assertions fail for the old confirmation and blur branches.
- [ ] **Step 3: Implement** the minimal state changes, remove `WindowBlurController` lifecycle calls, delete cross-window blur code, set compact/expanded card alpha directly, and make `onClose()` call `disableSelf()` immediately.
- [ ] **Step 4: Run** `./gradlew testDebugUnitTest --tests app.offlinecounter.OverlayUiStateTest` and verify it passes.
- [ ] **Step 5: Commit** as `fix: keep overlay sharp and close immediately`.

### Task 2: Route transient outcomes through toasts

**Files:**
- Modify: `app/src/main/java/app/offlinecounter/CounterAccessibilityService.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: export/share/count completion events
- Produces: `showToast(@StringRes message)` for short, non-expanding feedback

- [ ] **Step 1: Add resource strings** for completion, saved file, export failure, and save-before-share feedback.
- [ ] **Step 2: Replace transient `panel.setStatus(..., true)` calls** in completion/export/share branches with `showToast(...)`; retain in-panel progress and actionable source errors.
- [ ] **Step 3: Run** `./gradlew testDebugUnitTest lintDebug` and verify no regression or resource error.
- [ ] **Step 4: Commit** as `fix: show overlay outcomes as toasts`.

### Task 3: Add expandable setup guidance and direct service settings

**Files:**
- Create: `app/src/main/java/app/offlinecounter/AccessibilitySettingsRequest.kt`
- Create: `app/src/test/java/app/offlinecounter/AccessibilitySettingsRequestTest.kt`
- Modify: `app/src/main/java/app/offlinecounter/MainActivity.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `AccessibilitySettingsRequest.detailsAction`, `componentExtra`, and `fallbackAction`
- Consumes: `ComponentName(packageName, CounterAccessibilityService::class.java.name)` in MainActivity

- [ ] **Step 1: Write a failing JVM test** expecting action `android.settings.ACCESSIBILITY_DETAILS_SETTINGS`, flattened service component, and fallback action `android.settings.ACCESSIBILITY_SETTINGS`.
- [ ] **Step 2: Run** the new test and verify it fails because `AccessibilitySettingsRequest` does not exist.
- [ ] **Step 3: Implement** the pure request model with those literal values and make MainActivity build a details intent using `Intent.EXTRA_COMPONENT_NAME`, falling back when `resolveActivity` returns null or launch fails.
- [ ] **Step 4: Add** a clickable Material card header, chevron, and initially hidden numbered mini-guide; animate visibility and rotate the chevron on card taps.
- [ ] **Step 5: Run** `./gradlew testDebugUnitTest lintDebug` and verify all tests and accessibility lint checks pass.
- [ ] **Step 6: Commit** as `feat: add guided accessibility setup`.

### Task 4: Install the supplied launcher artwork

**Files:**
- Create: `app/src/main/res/drawable-nodpi/ic_launcher_artwork.png`
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Modify: `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- Modify: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Modify: `app/src/main/res/values/colors.xml`

**Interfaces:**
- Consumes: user-supplied `01-12.png`
- Produces: adaptive foreground, legacy launcher drawable, and monochrome themed icon

- [ ] **Step 1: Generate** a transparent, square launcher asset that preserves the supplied artwork and removes only the outer white canvas.
- [ ] **Step 2: Inspect** the asset at full size and a 192-pixel preview, confirming the “12” and all three rows remain legible.
- [ ] **Step 3: Wire** the artwork into legacy and adaptive launcher resources with the blue background and safe-zone scaling; update the monochrome contact-list silhouette.
- [ ] **Step 4: Run** `./gradlew lintDebug assembleDebug` and verify resource compilation and packaging succeed.
- [ ] **Step 5: Commit** as `feat: replace launcher icon artwork`.

### Task 5: Full verification and delivery

**Files:**
- Modify: `CHANGELOG_0.14.md`
- Output: `app/build/outputs/apk/debug/app-debug.apk`

**Interfaces:**
- Consumes: all preceding tasks
- Produces: verified debug APK and updated repository branch

- [ ] **Step 1: Document** the bug fixes, onboarding change, toast behavior, and icon replacement.
- [ ] **Step 2: Run** `./gradlew clean testDebugUnitTest lintDebug assembleDebug`.
- [ ] **Step 3: Inspect** APK metadata and SHA-256, then update the existing deliverable file.
- [ ] **Step 4: Commit and push** the final branch, wait for CI, and verify every workflow job succeeds before delivery.
