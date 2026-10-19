package com.runtime.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturePolicyTest {
    private val start = Point(200f, 400f)
    private val seek = Element("seekbar", Box(0, 380, 1000, 430), role = Role.SEEK, range = ProgressRange(0f, 100f))

    @Test fun stationaryShortTouchIsATap() {
        assertEquals(TouchDecision.Tap(start), GestureTrace(start, 0).finish(start, 80, 10f, 500))
    }
    @Test fun smallFingerJitterStillAllowsTap() {
        val trace = GestureTrace(start, 0).apply { observe(Point(204f, 405f), 1) }
        assertTrue(trace.finish(start, 80, 10f, 500) is TouchDecision.Tap)
    }
    @Test fun upwardVideoSwipeIsConsumed() {
        assertEquals(TouchDecision.Block, GestureTrace(start, 0).finish(Point(200f, 50f), 150, 10f, 500))
    }
    @Test fun downwardVideoSwipeIsConsumed() {
        assertEquals(TouchDecision.Block, GestureTrace(start, 0).finish(Point(200f, 900f), 150, 10f, 500))
    }
    @Test fun diagonalSwipeIsConsumed() {
        assertEquals(TouchDecision.Block, GestureTrace(start, 0).finish(Point(500f, 100f), 150, 10f, 500))
    }
    @Test fun returningToStartDoesNotTurnSwipeIntoTap() {
        val trace = GestureTrace(start, 0).apply { observe(Point(200f, 800f), 1) }
        assertEquals(TouchDecision.Block, trace.finish(start, 200, 10f, 500))
    }
    @Test fun multiTouchNeverBecomesATapAfterSecondFingerLifts() {
        val trace = GestureTrace(start, 0).apply { observe(start, 2) }
        assertEquals(TouchDecision.Block, trace.finish(start, 100, 10f, 500))
    }
    @Test fun longPressDoesNotOpenUnknownNativeMenus() {
        assertEquals(TouchDecision.Block, GestureTrace(start, 0).finish(start, 800, 10f, 500))
    }
    @Test fun horizontalSeekingUsesProgressRatherThanReplayingASwipe() {
        val action = GestureTrace(start, 0, seek).finish(Point(800f, 402f), 300, 10f, 500)
        assertEquals(TouchDecision.Seek(seek, 0.8f), action)
    }
    @Test fun verticalDragBeginningOnSeekbarStillCannotNavigate() {
        assertEquals(TouchDecision.Block, GestureTrace(start, 0, seek).finish(Point(800f, 100f), 300, 10f, 500))
    }
    @Test fun horizontalDragOutsideSeekbarIsConsumed() {
        assertEquals(TouchDecision.Block, GestureTrace(start, 0).finish(Point(800f, 400f), 300, 10f, 500))
    }
    @Test fun seekProgressClampsToTheNativeRange() {
        assertEquals(TouchDecision.Seek(seek, 1f), GestureTrace(start, 0, seek).finish(Point(1500f, 400f), 300, 10f, 500))
    }
}
