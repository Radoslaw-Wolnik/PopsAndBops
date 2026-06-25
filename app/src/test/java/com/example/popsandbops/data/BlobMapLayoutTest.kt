package com.example.popsandbops.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

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
        assertEdgeMargins(positions)
    }

    @Test
    fun arrangedPositionsKeepButtonEdgesApartForDenseScatter() {
        val positions = BlobMapLayout.arrangedPositions(28)

        assertEdgeMargins(positions)
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

        assertTrue(BlobMapLayout.hasClearEdges(resolved, listOf(occupied)))
    }

    @Test
    fun resolveOverlapsUsesNearestClearPositionBetweenTwoButtons() {
        val spacing = BlobMapLayout.MinimumBlobSpacing
        val occupied = listOf(
            MapPoint(0f, 0f),
            MapPoint(spacing, 0f),
        )
        val resolved = BlobMapLayout.resolveOverlaps(
            position = MapPoint(spacing / 2f, 0f),
            occupiedPositions = occupied,
        )
        val expectedY = sqrt(spacing * spacing - (spacing / 2f) * (spacing / 2f))

        assertEquals(spacing / 2f, resolved.x, EDGE_TOLERANCE)
        assertEquals(expectedY, abs(resolved.y), EDGE_TOLERANCE)
        assertTrue(BlobMapLayout.hasClearEdges(resolved, occupied))
    }

    @Test
    fun edgeGapBetweenMeasuresButtonEdgesNotJustCenters() {
        val first = MapPoint(0f, 0f)
        val second = MapPoint(
            x = BlobMapLayout.BlobButtonDiameter + BlobMapLayout.BlobCollisionMargin,
            y = 0f,
        )

        assertEquals(
            BlobMapLayout.BlobCollisionMargin,
            BlobMapLayout.edgeGapBetween(first, second),
            FLOAT_TOLERANCE,
        )
        assertTrue(BlobMapLayout.hasClearEdges(first, listOf(second)))
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

    private fun assertEdgeMargins(positions: List<MapPoint>) {
        positions.forEachIndexed { index, position ->
            positions.drop(index + 1).forEach { other ->
                val edgeGap = BlobMapLayout.edgeGapBetween(position, other)
                assertTrue(edgeGap >= BlobMapLayout.BlobCollisionMargin - EDGE_TOLERANCE)
            }
        }
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
        const val EDGE_TOLERANCE = 0.02f
    }
}
