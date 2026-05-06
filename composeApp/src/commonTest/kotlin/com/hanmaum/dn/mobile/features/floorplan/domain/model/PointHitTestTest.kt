package com.hanmaum.dn.mobile.features.floorplan.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PointHitTestTest {

    private val square = listOf(
        Point(0f, 0f), Point(1f, 0f), Point(1f, 1f), Point(0f, 1f)
    )

    @Test
    fun hitTest_returnsTrueForPointInsideSquare() {
        assertTrue(square.hitTest(Point(0.5f, 0.5f)))
    }

    @Test
    fun hitTest_returnsFalseForPointOutsideSquare() {
        assertFalse(square.hitTest(Point(1.5f, 0.5f)))
    }

    @Test
    fun hitTest_returnsFalseForPointAboveSquare() {
        assertFalse(square.hitTest(Point(0.5f, -0.1f)))
    }

    @Test
    fun hitTest_returnsFalseForEmptyPolygon() {
        assertFalse(emptyList<Point>().hitTest(Point(0.5f, 0.5f)))
    }

    @Test
    fun hitTest_returnsTrueForPointInsideTriangle() {
        val triangle = listOf(Point(0f, 0f), Point(1f, 0f), Point(0.5f, 1f))
        assertTrue(triangle.hitTest(Point(0.5f, 0.4f)))
    }

    @Test
    fun hitTest_returnsFalseForPointOutsideTriangle() {
        val triangle = listOf(Point(0f, 0f), Point(1f, 0f), Point(0.5f, 1f))
        assertFalse(triangle.hitTest(Point(0.1f, 0.9f)))
    }
}
