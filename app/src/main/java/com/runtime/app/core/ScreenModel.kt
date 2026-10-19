package com.runtime.app.core

enum class Role { OTHER, COMMENTS_BUTTON, COMMENTS_PANEL, PANEL, SEEK, VIDEO_NAVIGATION }

data class ProgressRange(val min: Float, val max: Float)

data class Element(
    val id: String,
    val bounds: Box,
    val path: List<Int> = emptyList(),
    val role: Role = Role.OTHER,
    val clickable: Boolean = false,
    val enabled: Boolean = true,
    val range: ProgressRange? = null,
)

data class ScreenSnapshot(
    val packageName: String,
    val windowId: Int,
    val appBounds: Box,
    val displayBounds: Box,
    val elements: List<Element>,
    val portrait: Boolean = true,
    val singleWindow: Boolean = true,
)

enum class ScreenKind { OTHER, SHORT, SHORT_WITH_COMMENTS, UNSUPPORTED }

data class Detection(
    val kind: ScreenKind,
    val reason: String,
    val guardBounds: Box? = null,
    val nativePanel: Box? = null,
    val markerIds: List<String> = emptyList(),
) {
    val guardable get() = kind == ScreenKind.SHORT || kind == ScreenKind.SHORT_WITH_COMMENTS
    fun guardRegions(): List<Box> {
        val bounds = guardBounds ?: return emptyList()
        return nativePanel?.let { bounds.subtract(it) } ?: listOf(bounds)
    }
}

class YouTubeShortsDetector {
    companion object {
        const val PACKAGE = "com.google.android.youtube"
        // Candidate IDs, not a compatibility claim. Verify against the physical phone.
        val viewerIds = setOf("reel_recycler", "reel_watch_fragment_root", "reel_player_page_container")
        val playerIds = setOf("reel_player_overlay", "reel_watch_player", "reel_player_page_container", "reel_player_view")
        val actionIds = setOf("reel_like_button", "reel_dislike_button", "reel_comment_button", "reel_share_button")
        val commentPanelIds = setOf("reel_comments", "comments_panel", "comment_recycler_view", "comments_recycler_view")
        val panelIds = setOf("engagement_panel", "engagement_panel_content", "bottom_sheet", "bottom_sheet_container")
        val diagnosticIds = viewerIds + playerIds + actionIds + commentPanelIds + panelIds
    }

    fun detect(snapshot: ScreenSnapshot): Detection {
        if (snapshot.packageName != PACKAGE) return Detection(ScreenKind.OTHER, "Outside YouTube")
        if (!snapshot.portrait || !snapshot.singleWindow) {
            return Detection(ScreenKind.UNSUPPORTED, "Use portrait and a single full-screen window")
        }
        val bounds = snapshot.appBounds.intersect(snapshot.displayBounds)
        if (bounds.width < snapshot.displayBounds.width * 0.9 || bounds.height < snapshot.displayBounds.height * 0.7) {
            return Detection(ScreenKind.UNSUPPORTED, "Window size is not supported")
        }
        val visible = snapshot.elements.filter { !it.bounds.intersect(bounds).empty }
        val markers = visible.map { it.id }.filter { it in diagnosticIds }.distinct().sorted()
        val viewer = visible.any {
            it.id in viewerIds && it.bounds.intersect(bounds).let { b -> b.width >= bounds.width * 0.75 && b.height >= bounds.height * 0.45 }
        }
        val player = visible.any { it.id in playerIds && it.bounds.intersect(bounds).height >= bounds.height * 0.4 }
        val actions = visible.map { it.id }.filter { it in actionIds }.distinct().size
        if (!viewer || (!player && actions < 2)) {
            return Detection(ScreenKind.OTHER, "No supported Shorts viewer detected", markerIds = markers)
        }
        val panels = visible.filter { it.role == Role.PANEL || it.role == Role.COMMENTS_PANEL }
            .filter { it.bounds.width >= bounds.width * 0.7 && it.bounds.height >= bounds.height * 0.2 }
        val commentPanel = panels.filter { panel ->
            panel.role == Role.COMMENTS_PANEL || visible.any {
                it.id in commentPanelIds && it.path.take(panel.path.size) == panel.path
            }
        }.maxByOrNull { it.bounds.area }
        if (commentPanel != null) {
            val panel = commentPanel.bounds.intersect(bounds)
            if (panel.top < bounds.top + bounds.height * 0.15 || panel.height > bounds.height * 0.85) {
                return Detection(ScreenKind.UNSUPPORTED, "Full-screen comments are not supported yet", markerIds = markers)
            }
            return Detection(ScreenKind.SHORT_WITH_COMMENTS, "Comments open; video area guarded", bounds, panel, markers)
        }
        if (panels.isNotEmpty()) {
            return Detection(ScreenKind.UNSUPPORTED, "Unrecognized panel; guard released", markerIds = markers)
        }
        return Detection(ScreenKind.SHORT, "Shorts viewer recognized", bounds, markerIds = markers)
    }
}
