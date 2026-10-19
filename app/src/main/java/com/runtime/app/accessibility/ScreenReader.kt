package com.runtime.app.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.runtime.app.core.Box
import com.runtime.app.core.Element
import com.runtime.app.core.ProgressRange
import com.runtime.app.core.Role
import com.runtime.app.core.ScreenSnapshot
import com.runtime.app.core.YouTubeShortsDetector

object ScreenReader {
    private const val MAX_NODES = 1800
    private const val MAX_DEPTH = 45

    fun read(root: AccessibilityNodeInfo, display: Box, portrait: Boolean, singleWindow: Boolean): ScreenSnapshot? {
        if (root.packageName?.toString() != YouTubeShortsDetector.PACKAGE) return null
        val nodes = mutableListOf<Element>()
        var truncated = false
        fun visit(node: AccessibilityNodeInfo, path: List<Int>) {
            if (nodes.size >= MAX_NODES || path.size > MAX_DEPTH) { truncated = true; return }
            if (!node.isVisibleToUser) return
            val bounds = Rect().also(node::getBoundsInScreen).toBox()
            val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
            // Descriptions are used transiently for known controls, never retained in a snapshot or report.
            val description = node.contentDescription?.toString()?.lowercase().orEmpty()
            val canSetProgress = node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id }
            val role = when {
                id in YouTubeShortsDetector.commentPanelIds -> Role.COMMENTS_PANEL
                id in YouTubeShortsDetector.panelIds -> Role.PANEL
                canSetProgress && node.rangeInfo != null -> Role.SEEK
                id == "reel_comment_button" || description.matches(Regex("comments?(,.*| [0-9,.kmb]+.*)?")) -> Role.COMMENTS_BUTTON
                id in setOf("reel_next_button", "reel_previous_button") || description in setOf("next video", "previous video", "next short", "previous short") -> Role.VIDEO_NAVIGATION
                else -> Role.OTHER
            }
            val range = if (role == Role.SEEK) node.rangeInfo?.let { ProgressRange(it.min, it.max) } else null
            nodes += Element(id, bounds, path, role, node.isClickable, node.isEnabled, range)
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try { visit(child, path + i) } finally { child.recycleCompat() }
            }
        }
        visit(root, emptyList())
        if (truncated || nodes.size >= MAX_NODES) return null // Never enforce against a truncated interface.
        return ScreenSnapshot(root.packageName.toString(), root.windowId,
            Rect().also(root::getBoundsInScreen).toBox(), display, nodes, portrait, singleWindow)
    }

    fun atPath(root: AccessibilityNodeInfo, path: List<Int>): AccessibilityNodeInfo? {
        var current = AccessibilityNodeInfo(root)
        for (index in path) {
            val next = current.getChild(index)
            current.recycleCompat()
            current = next ?: return null
        }
        return current
    }
}

fun Rect.toBox() = Box(left, top, right, bottom)
@Suppress("DEPRECATION")
fun AccessibilityNodeInfo.recycleCompat() = recycle()
