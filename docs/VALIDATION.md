# Validation record — 2026-10-19

Build: **0.1.0-alpha01** (`com.runtime.app`, version code 1).

## Local checks

Command from the repository root, using Android Studio JBR 21:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --console plain
```

- Debug APK: built successfully.
- Unit tests: **28 passed**, zero failures/errors (12 gesture policy, 12 detector, 4 geometry).
- Android lint: **0 errors**, 8 dependency/toolchain update advisories. Existing compatible SDK 36 / AGP 8.13.2 / Gradle 8.13 versions are intentional for the installed Android Studio. No warning baseline was introduced.
- Packaged permissions: only the app's own signature permission used by AndroidX for non-exported receivers. No INTERNET, general overlay, usage-access, or screenshot permission/capability.
- Gradle wrapper distribution checksum pinned to the official Gradle 8.13 SHA-256.

APK: `app/build/outputs/apk/debug/app-debug.apk`

SHA-256: `246E9261075AC4BD0D08DA0DB991DF43B9DB520D7C19468EB56AFDBF9B4AEDB5`

## Emulator smoke test

Temporary hidden, read-only `Medium_Phone_API_36.1` AVD (reports Android API 36), software rendering. No snapshot saved. Screenshots and temporary UI captures remain under ignored `.gradle/emulator-check/`, not in source control.

- APK installed and MainActivity launched successfully during the smoke test.
- Dashboard rendered; dark-system-bar contrast corrected after visual inspection.
- In-app disclosure displayed, with both Cancel and Agree and continue reachable.
- Public Android accessibility settings opened and listed RunTime video guard.
- Android permission approval completed in the emulator.
- Accessibility service bound successfully; dumpsys reported no crashed service; app process had no AndroidRuntime crash.
- App displayed Ready for a Short after service activation.
- Updating the APK retained the test preferences and launched successfully. This exposed a cached enabled-service query lag in the UI; the live service connection now takes precedence. The final status fix was subsequently compile/test/lint validated.
- Temporary emulator closed after testing. No phone was connected or modified.

The emulator initially required the supported SwiftShader renderer and briefly showed a System UI startup timeout, which recovered. Also, `uiautomator dump` temporarily suppresses other accessibility services during capture; verify the normal connected state with screenshots/dumpsys after it exits. This is a test-harness limitation, not proof of a service failure.

## Outstanding acceptance evidence

**This smoke test does not establish video enforcement in the official YouTube app.** Physical Galaxy S25 FE / Android 17 testing, actual Shorts recognition, first-swipe timing, tap-then-swipe strictness, native control behavior, comments, and lifecycle behavior while actively guarding are pending. Automatic advancement is not prevented by this prototype. X and Instagram are not implemented yet.

Follow [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md) before accepting milestone 1. If strict blocking fails, evaluate the touch-controller backend rather than treating post-transition recovery as prevention.
