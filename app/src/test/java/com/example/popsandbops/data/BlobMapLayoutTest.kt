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
    fun resolveOverlapsKeepsClearPositionsUntouched() {
        val position = MapPoint(240f, 20f)
        val resolved = BlobMapLayout.resolveOverlaps(
            position = position,
            occupiedPositions = listOf(MapPoint(-240f, 0f)),
        )

        assertPointEquals(position.x, position.y, resolved)
    }

    @Test
    fun resolveOverlapsPushesAwayFromOccupiedPositions() {
        val occupied = MapPoint(100f, 100f)
        val resolved = BlobMapLayout.resolveOverlaps(
            position = MapPoint(100f, 100f),
            occupiedPositions = listOf(occupied),
        )
        val distance = hypot(resolved.x - occupied.x, resolved.y - occupied.y)

        assertTrue(distance >= BlobMapLayout.MinimumBlobSpacing - FLOAT_TOLERANCE)
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
