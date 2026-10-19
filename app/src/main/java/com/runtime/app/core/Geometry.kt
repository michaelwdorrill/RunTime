package com.runtime.app.core

import kotlin.math.max
import kotlin.math.min

data class Point(val x: Float, val y: Float)

data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = (right - left).coerceAtLeast(0)
    val height get() = (bottom - top).coerceAtLeast(0)
    val area get() = width.toLong() * height
    val empty get() = width == 0 || height == 0
    fun contains(point: Point) = point.x >= left && point.x < right && point.y >= top && point.y < bottom
    fun intersect(other: Box) = Box(max(left, other.left), max(top, other.top), min(right, other.right), min(bottom, other.bottom))

    /** Rectangles are disjoint: a native panel must never overlap a guard window. */
    fun subtract(other: Box): List<Box> {
        val cut = intersect(other)
        if (cut.empty) return listOf(this)
        return listOf(
            Box(left, top, right, cut.top),
            Box(left, cut.bottom, right, bottom),
            Box(left, cut.top, cut.left, cut.bottom),
            Box(cut.right, cut.top, right, cut.bottom),
        ).filterNot { it.empty }
    }
}
