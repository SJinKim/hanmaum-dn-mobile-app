package com.hanmaum.dn.mobile.features.floorplan.domain.model

data class Point(val x: Float, val y: Float)

fun List<Point>.hitTest(tap: Point): Boolean {
    if (isEmpty()) return false
    var inside = false
    var j = size - 1
    for (i in indices) {
        val xi = this[i].x; val yi = this[i].y
        val xj = this[j].x; val yj = this[j].y
        if ((yi > tap.y) != (yj > tap.y) &&
            tap.x < (xj - xi) * (tap.y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}
