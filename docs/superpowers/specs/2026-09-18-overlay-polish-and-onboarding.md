# Overlay Polish and Onboarding Design

## Goal

Fix the reported overlay usability regressions, replace transient status text with Android toasts, install the supplied blue launcher artwork, and make accessibility-service setup easier without drawing over protected system settings.

## Overlay behavior

- Never enable cross-window blur. The app behind the accessibility overlay must remain sharp and usable.
- Use one rounded, high-opacity Material surface for the overlay. The compact state must not expose a separate horizontal strip.
- Keep the expanded state slightly denser than the compact state for legibility.
- The close action disables the service on the first tap. Clear remains the only destructive action that asks for a second tap.
- Counting progress stays in the expanded panel. Completion, export success, export failure, and share-before-export feedback use short Android toasts and do not expand the panel.

## Accessibility onboarding

- Tapping the entire “Плавающая панель” card expands or collapses a short numbered setup guide and rotates a chevron.
- The guide explains the remaining system confirmation and tells the user to return to the source app after enabling the service.
- The enable button first attempts Android's accessibility details action with this service component. If the device does not expose that activity, it falls back to the general accessibility settings screen.
- The app does not attempt to highlight or automate controls in Settings. Before the service is enabled it has no accessibility overlay, and system Settings is a protected external UI whose layout varies by One UI version.

## Launcher icon

- Use the user-supplied blue contact-list artwork as the complete visual identity.
- Remove only the outer white canvas and preserve the blue rounded tile, three people rows, and “12” badge.
- Supply adaptive, legacy, and monochrome resources so launchers can mask and theme the icon correctly.

## Verification

- JVM tests cover immediate close behavior, the absence of blur modes, and construction of the service-specific settings request with fallback.
- Android lint and debug assembly must pass.
- Inspect the generated launcher asset at launcher scale before packaging.
