package com.example.popsandbops.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class BlobMapLayoutTest {
    @Test
    fun arrangedPositionStartsOutsideTheRecordButtonField() {
        val position = BlobMapLayout.arrangedPosition(0)
        val distanceFromCenter = hypot(position.x, position.y)

        assertTrue(distanceFromCenter > BlobMapLayout.MinimumBlobSpacing)
        assertTrue(distanceFromCenter < BlobMapLayout.MinimumBlobSpacing * 1.5f)
    }

    @Test
    fun arrangedPositionSpreadsLaterBlobsFartherOut() {
        val firstPosition = BlobMapLayout.arrangedPosition(0)
        val firstSecondRingPosition = BlobMapLayout.arrangedPosition(BlobMapLayout.slotsForRing(0))
        val firstDistanceFromCenter = hypot(firstPosition.x, firstPosition.y)
        val distanceFromCenter = hypot(firstSecondRingPosition.x, firstSecondRingPosition.y)

        assertTrue(distanceFromCenter >= firstDistanceFromCenter + BlobMapLayout.MinimumBlobSpacing)
    }

    @Test
    fun firstEightArrangedPositionsStayCompactWithoutOverlapping() {
        val positions = BlobMapLayout.arrangedPositions(8)
        val distancesFromCenter = positions.map { hypot(it.x, it.y) }

        assertTrue(distancesFromCenter.all { it < BlobMapLayout.MinimumBlobSpacing * 1.5f })
        positions.forEachIndexed { index, position ->
            positions.drop(index + 1).forEach { other ->
                val distance = hypot(position.x - other.x, position.y - other.y)
                assertTrue(distance >= BlobMapLayout.MinimumBlobSpacing - FLOAT_TOLERANCE)
            }
        }
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
