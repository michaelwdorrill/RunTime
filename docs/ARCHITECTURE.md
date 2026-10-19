# Implementation decisions

## Scope and toolchain

The first build is the YouTube Shorts feasibility experiment. X/Instagram adapters are deliberately deferred until the input mechanism passes the Galaxy test. Native Kotlin, Compose settings, one Gradle application module. SDK 36 and AGP 8.13.2 match the installed Android Studio toolchain. Android 17 behavior still requires device validation.

## Recognition

The service receives window metadata across apps so it can release interception immediately on exit. It never reads other apps' event text or source trees. Only a foreground root with YouTube's package is traversed. Snapshots retain structural IDs, node paths, rectangles, control roles, and numeric seek ranges. Known control descriptions are interpreted transiently; captions, post text, and comments are not stored. Diagnostic IDs use an allowlist.

Known fullscreen Shorts container IDs plus corroborating player/action structure are required. The IDs are candidates, not verified selectors for every app version. A Shorts label alone, a home shelf, or a single generic marker cannot activate protection. Large unknown panels, non-portrait, multiple application windows, and unsupported bounds release the guard.

## Why the first backend is an overlay

`TYPE_ACCESSIBILITY_OVERLAY` can receive touches without making underlying nodes unavailable. No general SYSTEM_ALERT_WINDOW permission is needed. The prototype installs real rectangular windows covering the application region minus recognized comments bounds. System bars remain outside the guard; system gesture exclusions are not requested. No hidden touchable-region API is used.

Each complete interaction is captured from DOWN to UP. Movement is never replayed as a native swipe. A trace remembers maximum displacement and multiple pointers, so returning to the start or lifting a second finger cannot turn scrolling into a tap. Native seek controls use ACTION_SET_PROGRESS rather than forwarding a drag.

Known clickable nodes receive ACTION_CLICK after their window, resource ID, node path, and rectangle are rechecked. Other short taps use a synthetic stationary tap. For that fallback the overlay is removed, the foreground is rechecked after 32 ms, and the guard is re-evaluated after completion/cancellation or a 350 ms timeout. **This is an experimental release window, not an atomic touch filter.** Rapid tap-then-swipe testing is essential; if this permits a transition, the first milestone fails. Do not claim a hard guarantee from the current backend.

A touch-interaction-controller backend is a possible follow-up if the overlay is inadequate. It requires touch-exploration mode and its own compatibility experiment. The prototype releases if another service has touch exploration enabled.

## Lifecycle

Window events cause immediate queued re-evaluation. A 750 ms watchdog catches missing events and verifies foreground status. It is a recovery mechanism, not proof of zero entry latency. Screen-off, app switches, configuration changes, service interrupts, unbind, and destroy remove guard windows. Injection callbacks use a generation token so an old callback cannot restore protection in a new context. Guard attachment failure is shown as unavailable rather than active.

## Known limitations

- No physical-device evidence yet for recognition, first-swipe timing, native-control usability, or system-gesture priority.
- No content identity lock or reliable prevention of app-driven automatic advancement.
- English description fallbacks; structural IDs are preferred.
- Comments only pass through when bounded panels are recognized; unknown panels fail open.
- Seeking requires an exposed actionable seek range. Hidden/custom seek controls may not work.
- Long presses and double-tap behavior are not preserved equivalently.
- Landscape, split-screen, floating windows, PiP, DeX, and touch-exploration coexistence are unsupported.
- Synthetic tap fallback briefly releases interception. Actual strictness depends on the phone test.
- Counters/history are in memory and reset on process restart.

## References

- Android accessibility overlays: https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo#TYPE_ACCESSIBILITY_OVERLAY
- Touch interaction controller and its touch-exploration requirement: https://developer.android.com/reference/android/accessibilityservice/TouchInteractionController
- Accessibility gesture dispatch: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#dispatchGesture(android.accessibilityservice.GestureDescription,%20android.accessibilityservice.AccessibilityService.GestureResultCallback,%20android.os.Handler)
- AGP 8.13 compatibility: https://developer.android.com/build/releases/agp-8-13-0-release-notes
- Android 17 Advanced Protection restriction: https://blog.google/security/android-advanced-protection-updates/
