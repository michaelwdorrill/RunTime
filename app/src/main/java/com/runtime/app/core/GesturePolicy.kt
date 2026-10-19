package com.runtime.app.core

import kotlin.math.abs
import kotlin.math.max

sealed interface TouchDecision {
    data class Tap(val point: Point) : TouchDecision
    data class Seek(val element: Element, val fraction: Float) : TouchDecision
    data object Block : TouchDecision
    data object Ignore : TouchDecision
}

/** Holds the entire interaction, so moving back to the start cannot disguise a swipe as a tap. */
class GestureTrace(val start: Point, val startedAt: Long, val seek: Element? = null) {
    var furthestX = 0f
        private set
    var furthestY = 0f
        private set
    var multiplePointers = false
        private set

    fun observe(point: Point, pointerCount: Int) {
        furthestX = max(furthestX, abs(point.x - start.x))
        furthestY = max(furthestY, abs(point.y - start.y))
        multiplePointers = multiplePointers || pointerCount > 1
    }

    fun finish(point: Point, endedAt: Long, slop: Float, longPressMillis: Long): TouchDecision {
        observe(point, 1)
        if (multiplePointers || endedAt < startedAt) return TouchDecision.Block
        val moved = furthestX > slop || furthestY > slop
        if (!moved) {
            return if (endedAt - startedAt < longPressMillis) TouchDecision.Tap(point) else TouchDecision.Block
        }
        val target = seek
        // Never replay a native drag: horizontal seeking is expressed as an accessibility action.
        if (target?.range != null && furthestX > slop && furthestY <= slop && target.bounds.width > 0) {
            return TouchDecision.Seek(target, ((point.x - target.bounds.left) / target.bounds.width).coerceIn(0f, 1f))
        }
        return TouchDecision.Block
    }
}
