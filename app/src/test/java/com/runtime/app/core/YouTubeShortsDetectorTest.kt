package com.runtime.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic structural fixtures: these test rules, not actual YouTube compatibility. */
class YouTubeShortsDetectorTest {
    private val detector = YouTubeShortsDetector()
    private val display = Box(0, 0, 1080, 2400)
    private val app = Box(0, 70, 1080, 2320)
    private fun viewer(extra: List<Element> = emptyList()) = ScreenSnapshot(
        YouTubeShortsDetector.PACKAGE, 1, app, display,
        listOf(Element("reel_recycler", app, listOf(0)), Element("reel_player_page_container", app, listOf(0, 0))) + extra,
    )

    @Test fun structuralFullscreenViewerIsGuardable() {
        assertTrue(detector.detect(viewer()).guardable)
    }
    @Test fun ordinaryWatchPageIsNotGuarded() {
        val frame = viewer().copy(elements = listOf(Element("watch_player", app), Element("player", app)))
        assertFalse(detector.detect(frame).guardable)
    }
    @Test fun shortsShelfOnHomeIsNotAFullscreenViewer() {
        val frame = viewer().copy(elements = listOf(Element("reel_recycler", Box(0, 900, 1080, 1300))))
        assertFalse(detector.detect(frame).guardable)
    }
    @Test fun loneGenericViewerMarkerIsInsufficient() {
        assertFalse(detector.detect(viewer().copy(elements = listOf(Element("reel_recycler", app)))).guardable)
    }
    @Test fun otherPackagesCannotActivateTheGuard() {
        assertFalse(detector.detect(viewer().copy(packageName = "com.example.other")).guardable)
    }
    @Test fun landscapeReleasesTheGuard() {
        assertEquals(ScreenKind.UNSUPPORTED, detector.detect(viewer().copy(portrait = false)).kind)
    }
    @Test fun multipleApplicationWindowsReleaseTheGuard() {
        assertFalse(detector.detect(viewer().copy(singleWindow = false)).guardable)
    }
    @Test fun smallOrFloatingWindowIsNotGuarded() {
        assertFalse(detector.detect(viewer().copy(appBounds = Box(100, 200, 700, 1600))).guardable)
    }
    @Test fun knownCommentsPanelLeavesVideoGuarded() {
        val panel = Box(0, 1000, 1080, 2320)
        val result = detector.detect(viewer(listOf(Element("reel_comments", panel, listOf(1), Role.COMMENTS_PANEL))))
        assertEquals(ScreenKind.SHORT_WITH_COMMENTS, result.kind)
        assertEquals(panel, result.nativePanel)
        assertTrue(result.guardRegions().all { it.intersect(panel).empty })
        assertEquals(app.area - panel.area, result.guardRegions().sumOf { it.area })
    }
    @Test fun unknownPanelReleasesRatherThanSwallowingItsControls() {
        val result = detector.detect(viewer(listOf(Element("bottom_sheet", Box(0, 1000, 1080, 2320), role = Role.PANEL))))
        assertEquals(ScreenKind.UNSUPPORTED, result.kind)
        assertFalse(result.guardable)
    }
    @Test fun fullscreenCommentsAreNotMisreportedAsProtected() {
        val result = detector.detect(viewer(listOf(Element("reel_comments", app, role = Role.COMMENTS_PANEL))))
        assertEquals(ScreenKind.UNSUPPORTED, result.kind)
    }
    @Test fun diagnosticsContainOnlyAllowlistedIds() {
        val result = detector.detect(viewer(listOf(Element("unknown_private_resource", app))))
        assertFalse(result.markerIds.contains("unknown_private_resource"))
        assertTrue(result.markerIds.all { it in YouTubeShortsDetector.diagnosticIds })
    }
}
