# RunTime v0.1 specification

## Agreed product behavior

One video per visit to the native video viewer. Watching, pausing, replaying, muting, and seeking remain available where native controls permit them. Forward and backward video-navigation swipes are blocked. Back/close exits normally. Leaving and explicitly opening another video starts a new visit. No timer or daily quota.

Official apps only: X video viewer, Instagram Reels, YouTube Shorts. Ordinary feeds and messages, Instagram Stories/carousels, and regular YouTube videos are outside scope. Opening a Reels/Shorts tab permits its initial video. Comments should scroll independently while the video remains guarded. Finishing may stop or loop the video; advancing to another video is not desired.

## Milestones

1. YouTube Shorts mechanism and recognition prototype on the Galaxy S25 FE.
2. Separate Instagram and X screen adapters after the mechanism passes the first device test.
3. Persistence/lifecycle hardening, broader screen coverage, and daily-use validation.

## First prototype acceptance gate

- Physical phone can enable the service.
- A Short is identified without matching ordinary YouTube screens or Shorts shelves.
- Guard activates before the user's first navigation swipe.
- Slow, fast, repeated, diagonal, and multi-touch movements cannot change video while guarded.
- Native short taps, detected seek controls, comments, and Back remain usable.
- Comments do not allow swiping on the remaining video area.
- Home, notification shade, keyboard, lock/unlock, app switching, and service disable cannot leave an invisible blocker.
- Return from comments or another app re-evaluates the current screen.
- No next video appears or begins playing as a consequence of a blocked gesture.

These are device acceptance requirements, not properties established by unit tests. Automatic advancement, entry races, and platform-specific touch dispatch must be measured. Failure should be reported rather than silently counted as success.

## Limits for alpha01

YouTube only, English interface recognition, portrait, single full-screen phone window. No split-screen, PiP, DeX, landscape, long-press/double-tap equivalence, or guarantee against automatic advancement. Swiping on an exposed native seekbar could navigate, so seeking uses ACTION_SET_PROGRESS under the guard where available. Unknown panels and unknown screens fail open with an unavailable state. No anti-uninstall or forced device restrictions.
