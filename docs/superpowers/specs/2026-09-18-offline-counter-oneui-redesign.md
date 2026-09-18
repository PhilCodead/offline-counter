# Offline Counter 0.14: Kotlin cleanup and One UI redesign

## Context

Version 0.12 is a custom Python-to-DEX release source. Version 0.13 is the maintainable Android Studio/Kotlin port and is the implementation base. Version 0.12 remains a behavior reference for the accessibility workflow and offline guarantees.

The application counts people in a list exposed through Android Accessibility, exports the collected records to XLSX, and optionally shares the most recent export. Its primary working interface is a movable accessibility overlay displayed above the source application. The launcher activity only configures and reports the service state.

## Goals

- Keep all counting and export behavior local to the device with no Internet permission.
- Complete the Kotlin/Gradle migration as a conventional, reproducible Android Studio project.
- Improve readability, separation of responsibilities, naming, and testability.
- Remove dead resources, redundant code, and nonessential comments.
- Give the launcher and overlay a Samsung One UI-inspired appearance with a safe generic Android fallback.
- Preserve comfortable controls: touch targets are at least 48 dp and the overlay must not dominate the screen.
- Use `app.offlinecounter` as the application ID and package namespace.

## Non-goals

- Depending on unofficial SESL or private Samsung libraries.
- Adding networking, analytics, accounts, synchronization, or cloud storage.
- Replacing the accessibility-based collection mechanism with OCR.
- Supporting an arbitrary spreadsheet format beyond the existing XLSX export.
- Rebuilding the application with Jetpack Compose when Android Views are sufficient.

## Selected approach

Use Android Views, AppCompat, Material Components where useful, and project-owned resources for the One UI visual language. This keeps the build public and reproducible while allowing Samsung-first styling and normal behavior on other Android devices.

The launcher follows the approved “Samsung Settings” direction: a large title, grouped rounded sections, clear service status, restrained color, and one primary action. The overlay follows the approved “Glass Toolbar” direction with all frequent actions available in the compact state.

## Project and build

- Base: the attached 0.13 Android Studio project.
- Version: `0.14`, with the next integer version code.
- Namespace and application ID: `app.offlinecounter`.
- Minimum SDK: 29, preserving the current supported range.
- Compile and target SDK: 35, preserving compatibility with the supplied project and expected local toolchain.
- JDK: 17.
- Add a Gradle Wrapper and keep dependency versions explicit.
- Keep `android.permission.INTERNET` absent.
- Exclude IDE, build, local SDK, and temporary design files from the deliverable.

## Architecture

### Launcher

`MainActivity` renders the setup/status screen, opens the system accessibility settings, observes whether the service is enabled, and updates the visible state on resume. It does not own collection state.

### Accessibility service

`CounterAccessibilityService` owns Android lifecycle integration, accessibility event intake, overlay attachment, and orchestration. It delegates counting transitions to a controller and renders immutable UI state through the overlay.

### Collection controller

Extract the phase machine and counters from the service into `CountingController`. Its phases are:

- `Idle`
- `Rewinding`
- `Collecting`
- `Completed`
- `Error`

The controller accepts parsed screen snapshots and scroll-boundary signals, deduplicates people, decides the next scroll action, exposes progress, and produces user-facing status. Platform calls remain in the service so the controller can be tested on the JVM.

### Parsing and models

`PersonParser` stays a focused pure parser. Android tree traversal is separated from text parsing where practical. `Person` remains immutable, with a normalized identity key. Status and sex values are represented consistently and all display strings live in resources.

### Scrolling

`ScrollTracker` continues to track index and coordinate boundaries. Boolean expressions are parenthesized for clarity and coverage includes incomplete event data, reset behavior, top/bottom detection, and contradictory values.

### Export

`ExcelExporter` keeps MediaStore output and the local template. Workbook generation and storage are separated so XML generation, escaping, counts, template replacement, and failure cleanup can be tested independently. The latest exported URI remains service state and is cleared when it is no longer usable.

### Overlay

`OverlayPanel` is split into view construction, rendering, and callbacks. It receives an immutable `OverlayUiState`; it does not infer collection behavior from button labels. Strings, dimensions, colors, shapes, and motion timings move to resources where Android resource lookup is appropriate.

## Overlay interaction design

### Compact state

- Height approximately 64–68 dp, with every interactive target at least 48 dp.
- Width uses content size but never exceeds the available display width minus safe margins.
- Actions: drag handle, primary count/progress button, Excel export, standard three-node Share icon, and animated expand chevron.
- Secondary controls use a nearly opaque cool-gray fill and a subtle outline so they remain distinct on the light glass surface.
- The primary button shows `Подсчёт`, live count, progress, and `Готово` according to state.
- After counting begins, the panel automatically returns to its compact state.

### Expanded state

- Width is approximately 320–360 dp and adapts to small displays.
- A darker, denser translucent surface and stronger blur increase text contrast.
- Shows status, total, women, men, recount/stop, clear, and a close/disable action without crowding the compact toolbar.
- Destructive clear and disable actions require confirmation; routine actions do not.
- The chevron points upward while expanded and downward while collapsed, rotating with a short state animation.

### Movement

- Long-pressing or dragging the handle moves the overlay.
- Coordinates are clamped to visible display bounds and system insets.
- On release, the panel animates to the nearest horizontal edge.
- The normalized position and preferred edge are stored locally and restored when the service reconnects.
- Orientation and display-size changes recalculate bounds without moving the panel off-screen.

### Feedback and accessibility

- Use restrained haptic feedback for primary actions, expansion, and edge snapping.
- Respect the system animator-duration setting and avoid decorative continuous motion.
- Provide content descriptions for icon-only actions and meaningful accessibility roles.
- Maintain adequate contrast in light/dark modes and do not use color as the only state signal.

## Frosted glass behavior

On Android 12 and later, request cross-window blur when the device and current system state support it. The compact surface uses a lighter translucent tint and moderate blur. The expanded surface uses a darker tint and stronger blur.

Window blur can be unavailable because of Android version, device capability, battery saving, media playback, or user/developer settings. The overlay must listen for blur availability where supported and switch to more opaque backgrounds when blur is disabled. On Android 10 and 11 it always uses the opaque fallback. Text and controls must remain readable in both paths.

## State and data flow

1. The service receives an accessibility event and records the active non-system package.
2. The user starts counting from the overlay.
3. The controller resets state and requests rewind actions until the top or a safe stop condition is reached.
4. The service supplies visible accessibility-tree content to the parser.
5. Parsed people are deduplicated and totals update the immutable overlay state.
6. The controller requests forward scrolling until the bottom or another bounded stop condition is reached.
7. Completion changes the overlay state, triggers restrained completion feedback, and enables export.
8. Export writes a local XLSX through MediaStore; Share uses the last valid export URI.

Every loop retains explicit maximums and stale-frame limits so malformed accessibility trees cannot create unbounded scrolling.

## Error handling

- Missing active list: show a concise instruction and remain idle.
- Source package disappears during collection: retry for a bounded interval, then stop with a recoverable message.
- No scrollable node: finish with the data already collected.
- Export failure: delete incomplete MediaStore content and present an actionable error.
- Share before export: reveal the expanded panel and direct the user to export first.
- Overlay attachment/removal failure: keep service cleanup idempotent and avoid duplicate views.
- Unsupported blur: use the opaque fallback without showing an error.

## Icon and visual assets

Replace the current spreadsheet-like launcher icon with the approved One UI-style person-and-count symbol inside an adaptive rounded icon. The background uses a neutral cool slate blue-gray gradient. The foreground is a white outline person with a compact white circular badge containing `12`, positioned like an unread-message badge at the lower right. The badge stays small enough that the person remains dominant. Supply adaptive foreground/background resources and a compatible legacy icon, preserving the same composition and safe-zone proportions at launcher sizes down to 32 px. Use vector drawables for toolbar icons and remove unused assets.

The Share action uses the conventional three connected nodes. The expand icon is a single chevron whose rotation is driven by state rather than separate unrelated glyphs.

## Testing

Development follows test-first changes for extracted logic.

- Parser tests: inline/separate names, invalid dates, missing sex, vaccination status, duplicates, whitespace, and XML-sensitive text.
- Scroll tests: top/bottom by indices and coordinates, reset, incomplete and contradictory events.
- Controller tests: all phase transitions, retry/stale limits, rewind/collect completion, progress, stop, recount, and error recovery.
- Export tests: XML escaping, empty/single/multiple records, sex totals, required workbook entries, and template replacement.
- Overlay state tests: label/icon/expanded-state mapping and destructive-action confirmation decisions.
- Android resource/lint checks: missing strings, content descriptions, adaptive icon resources, and manifest permissions.
- Build verification: unit tests, lint, debug APK assembly, and inspection confirming the absence of Internet permission.

Manual device checks cover a Samsung device in light and dark modes, blur enabled and disabled, compact and expanded overlays, edge snapping, rotation, accessibility settings flow, count/export/share, and fallback behavior on a non-Samsung Android device or emulator.

## Deliverable

Provide a clean Android Studio source archive containing the Gradle Wrapper, Kotlin sources, resources, tests, and updated README. Do not include build outputs, local configuration, temporary design files, credentials, or signing keys. Include a concise migration/change summary and documented build requirements.
