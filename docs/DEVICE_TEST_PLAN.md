# Galaxy S25 FE acceptance test

Target: SM-S731U1, Android 17, One UI 9.0. Install with Android Studio. Record YouTube version from RunTime's shared report. Start with English YouTube UI, portrait, ordinary full-screen mode.

**A green ONE VIDEO pill means guard windows are attached. It does not prove the mechanism passed this plan.** If the pill never appears, recognition is not compatible yet; do not call an unguarded swipe an interception failure. Share the diagnostic report, which includes known IDs without screen text.

## Setup and exit

- Accept the disclosure and enable RunTime video guard in Accessibility. Record the exact OS message if blocked, including whether Android 17 Advanced Protection is enabled. Do not mislabel RunTime as an accessibility tool to circumvent a restriction.
- Confirm master and YouTube switches are enabled. Open a Short and note whether the pill appears before you can swipe.
- Tap RunTime in the pill. Settings must open and all interception must release.
- Pause protection, then verify normal YouTube scrolling returns.

## Entry paths

For each, test a swipe immediately after opening, before waiting for the pill, as well as after it appears:

- Shorts tab.
- A Short chosen from the home feed.
- A specific Short from search or a channel.
- A shared link opened in the YouTube app.
- Return to an existing Short after Home/app switch.

Any next-video appearance or playback is a failed strictness test, even if RunTime later returns to the original video.

## Guarded interaction

- Slow and fast upward/downward swipes.
- Repeated and diagonal swipes, swipes starting near either edge and over the right-side action buttons.
- Move far away then return to the starting point before lifting.
- Two fingers, including lifting one and continuing with the other.
- Tap pause/play, replay, and available mute controls.
- Tap, then immediately swipe while the native-tap fallback may be running. Repeat rapidly.
- Use the exposed seekbar horizontally, then try a vertical/diagonal swipe beginning on it.
- Open comments, scroll and type, close comments, and swipe again. Swipes on any remaining video area must still be blocked. If the panel is unknown/full-screen, RunTime should report unsupported rather than trap it.
- Let the video end. Record loop/stop/advance. Auto-advance is not solved in alpha01.
- Back must exit. Explicitly open a different Short and confirm that it becomes the new protected visit.

## Unrelated screens and recovery

- Ordinary YouTube video, home/search, Shorts shelves, channel page: no guard.
- Instagram, X, browser, other apps: no guard in this milestone.
- Notification shade, navigation Back/Home, keyboard, lock/unlock, rotation, app switch.
- Disable the accessibility service while guarding; ensure no blocker remains.
- Rotate to landscape, use split-screen/floating/PiP: guard releases; unsupported state is acceptable.
- Reboot/relaunch: preferences persist; service connection and actual protection status remain truthful.

## Report results

In RunTime, open Test details > Share diagnostic report and review it before sharing. Include: entry path; whether the pill appeared; exact failed action; whether any new video appeared/played; whether Back worked. Keep screenshots or raw UI dumps out of the repository. Do not record account messages or comments just to demonstrate a gesture.

## Acceptance record

- Local compilation/unit tests/lint: see repository validation notes.
- Physical-device execution: pending.
- Recognition compatibility: pending.
- Strict first-swipe and tap-then-swipe prevention: pending.
- Playback controls/comments/system exits: pending.
