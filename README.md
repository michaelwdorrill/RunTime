# RunTime

Watch the video you opened. Leave the next one for another decision.

Native Android experiment for blocking the swipe from one short video to another inside the official apps. Target test device: Samsung Galaxy S25 FE SM-S731U1, Android 17 / One UI 9.0.

## Current milestone

**0.1.0-alpha01 is the YouTube Shorts feasibility prototype.** X and Instagram are planned; they are not implemented or advertised as protected. Detection rules are unverified against the owner's YouTube version until the physical-device test. This is not a claim of working enforcement on all Shorts screens.

The prototype recognizes known structural Shorts IDs, covers the video with an accessibility touch guard, consumes movement, forwards short taps, and uses accessible seek controls where available. A small **ONE VIDEO** pill indicates that the guard is actually installed. Tap **RunTime** in the pill to return to settings. Comments receive a bounded native touch region when their panel is recognized. Unknown overlays, orientation, and screens release interception and report the limitation. Auto-advance prevention and universal seeking are not established by this prototype.

## Open and run

1. Open this repository's root folder in Android Studio. Use the bundled JDK (17 or newer; verified locally with Studio's JBR 21). Let Gradle sync.
2. Connect the Galaxy, enable USB debugging, and approve its debugging prompt.
3. Select the `app` run configuration and the phone, then Run.
4. Read and accept the in-app accessibility disclosure. Enable **RunTime video guard** in Android's Accessibility settings, return, and enable protection.
5. Open a YouTube Short. Look for the **ONE VIDEO** indicator before testing swipes. Follow [the device test plan](docs/DEVICE_TEST_PLAN.md).

Android 17 Advanced Protection can restrict non-verified accessibility services. If the service cannot be enabled, record the exact system message; do not work around this by falsely declaring the app an accessibility tool. RunTime does not alter security settings.

### Command-line validation (PowerShell)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The project uses the installed SDK 36 / AGP 8.13.2 / Gradle 8.13 toolchain and runs on Android 14+. Targeting SDK 36 does not prevent testing on Android 17. SDK 37 migration is separate from proving the input mechanism. `local.properties` is machine-specific and ignored.

## Privacy

No internet permission, server, analytics, screenshots, screen recording, social sign-in, or content logs. Screen traversal is limited to YouTube. Foreground window metadata is used to release protection on exit. Diagnostic sharing is explicit and contains only allowlisted structural IDs, device/app versions, guard state, and counters. Do not commit UI dumps or account content.

## Project notes

- [Product and implementation spec](docs/SPEC.md)
- [Physical-device test plan](docs/DEVICE_TEST_PLAN.md)
- [Implementation decisions and known limits](docs/ARCHITECTURE.md)
- [Build and emulator validation record](docs/VALIDATION.md)

Source changes are kept local for review. No release or Play Store distribution is configured.
