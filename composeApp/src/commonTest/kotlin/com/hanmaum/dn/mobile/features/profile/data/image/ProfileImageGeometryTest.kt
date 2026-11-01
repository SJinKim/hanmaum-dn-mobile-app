package com.hanmaum.dn.mobile.features.profile.data.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileImageGeometryTest {

    @Test
    fun theCropIsTheCentredSquare() {
        assertEquals(SquareCrop(left = 200, top = 0, size = 800), centreSquareCrop(1200, 800))
        assertEquals(SquareCrop(left = 0, top = 150, size = 600), centreSquareCrop(600, 900))
        assertEquals(SquareCrop(left = 0, top = 0, size = 512), centreSquareCrop(512, 512))
    }

    @Test
    fun decodeSizeJustCoversTheEdgeOnTheShortSide() {
        assertEquals(768 to 512, boundedDecodeSize(3000, 2000, 512))
        assertEquals(512 to 683, boundedDecodeSize(3000, 4000, 512))
    }

    @Test
    fun aSmallImageIsNotEnlargedByTheDecoder() {
        assertEquals(300 to 200, boundedDecodeSize(300, 200, 512))
    }

    @Test
    fun aPanoramaIsCappedOnTheLongEdge() {
        val (w, h) = boundedDecodeSize(40_000, 2_000, 512)
        assertEquals(MAX_DECODE_EDGE, w)
        assertEquals(205, h)
        assertEquals(MAX_DECODE_EDGE, boundedMaxPixelSize(40_000, 2_000, 512))
    }

    @Test
    fun sampleSizeKeepsTheShortSideAtLeastTheEdge() {
        assertEquals(1, sampleSizeFor(800, 600, 512))
        assertEquals(4, sampleSizeFor(4032, 3024, 512))
        assertTrue(3024 / sampleSizeFor(4032, 3024, 512) >= 512)
        assertEquals(1, sampleSizeFor(100, 100, 512))
    }
}
