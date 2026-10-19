package com.runtime.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {
    private val whole = Box(0, 0, 100, 200)
    @Test fun subtractionLeavesNoTouchInterceptionOverNativePanel() {
        val panel = Box(10, 50, 90, 180)
        val regions = whole.subtract(panel)
        assertEquals(whole.area - panel.area, regions.sumOf { it.area })
        assertTrue(regions.all { it.intersect(panel).empty })
        regions.forEachIndexed { i, first -> regions.drop(i + 1).forEach { second -> assertTrue(first.intersect(second).empty) } }
    }
    @Test fun panelOutsideScreenDoesNotRemoveGuard() {
        assertEquals(listOf(whole), whole.subtract(Box(200, 0, 300, 100)))
    }
    @Test fun panelCoveringScreenLeavesNoGuardWindows() {
        assertTrue(whole.subtract(Box(-100, -100, 300, 300)).isEmpty())
    }
    @Test fun panelPartlyOutsideScreenIsClipped() {
        assertEquals(listOf(Box(0, 0, 100, 100)), whole.subtract(Box(-10, 100, 110, 300)))
    }
}
