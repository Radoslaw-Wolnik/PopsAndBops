package com.example.popsandbops.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class BlobMapLayoutTest {
    @Test
    fun arrangedPositionStartsAtTopOfFirstRing() {
        val position = BlobMapLayout.arrangedPosition(0)

        assertPointEquals(0f, -BlobMapLayout.FirstRingRadius, position)
    }

    @Test
    fun arrangedPositionMovesToNextRingAfterFirstRingSlots() {
        val firstSecondRingPosition = BlobMapLayout.arrangedPosition(BlobMapLayout.slotsForRing(0))
        val distanceFromCenter = hypot(firstSecondRingPosition.x, firstSecondRingPosition.y)

        assertEquals(
            BlobMapLayout.FirstRingRadius + BlobMapLayout.RingSpacing,
            distanceFromCenter,
            FLOAT_TOLERANCE,
        )
    }

    @Test
    fun snapToArrangeSlotUsesNearestRingSlot() {
        val snapped = BlobMapLayout.snapToArrangeSlot(MapPoint(12f, -174f))

        assertPointEquals(0f, -BlobMapLayout.FirstRingRadius, snapped)
    }

    @Test
    fun guideRadiiAlwaysIncludesAtLeastThreeRings() {
        val radii = BlobMapLayout.guideRadii(maxDistance = 0f)

        assertEquals(3, radii.size)
        assertEquals(BlobMapLayout.FirstRingRadius, radii.first(), FLOAT_TOLERANCE)
        assertTrue(radii.zipWithNext().all { (current, next) -> next > current })
    }

    private fun assertPointEquals(expectedX: Float, expectedY: Float, actual: MapPoint) {
        assertEquals(expectedX, actual.x, FLOAT_TOLERANCE)
        assertEquals(expectedY, actual.y, FLOAT_TOLERANCE)
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
