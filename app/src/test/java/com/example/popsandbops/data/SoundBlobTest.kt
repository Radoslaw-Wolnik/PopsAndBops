package com.example.popsandbops.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundBlobTest {
    @Test
    fun sanitizeTrimRangeClampsStartAndEndInsideDuration() {
        val trim = sanitizeTrimRange(
            startMs = -100,
            endMs = 2_000,
            sourceDurationMs = 1_000,
        )

        assertEquals(0, trim.startMs)
        assertEquals(1_000, trim.endMs)
        assertEquals(1_000, trim.durationMs)
    }

    @Test
    fun sanitizeTrimRangeKeepsMinimumPlayableDuration() {
        val trim = sanitizeTrimRange(
            startMs = 900,
            endMs = 950,
            sourceDurationMs = 1_000,
        )

        assertEquals(750, trim.startMs)
        assertEquals(1_000, trim.endMs)
        assertEquals(MIN_SOUND_DURATION_MS, trim.durationMs)
    }

    @Test
    fun blobDurationUsesSanitizedTrimRange() {
        val blob = soundBlob(
            trimStartMs = 900,
            trimEndMs = 950,
            sourceDurationMs = 1_000,
        )

        assertEquals(MIN_SOUND_DURATION_MS, blob.durationMs)
        assertEquals(750, blob.trimRange.startMs)
        assertEquals(1_000, blob.trimRange.endMs)
    }

    @Test
    fun recordedStateDependsOnAudioPath() {
        assertFalse(soundBlob(audioPath = null).isRecorded)
        assertTrue(soundBlob(audioPath = "/tmp/pop.m4a").isRecorded)
    }

    @Test
    fun defaultSoundBlobsArePlayableAndUnique() {
        val blobs = BlobDefaults.defaultSoundBlobs(now = 1_000_000L)

        assertEquals(5, blobs.size)
        assertEquals(blobs.size, blobs.map { it.id }.toSet().size)
        assertTrue(blobs.all { it.isPinned })
        assertTrue(blobs.all { it.builtInTone != null })
        assertTrue(blobs.all { it.waveform.isNotEmpty() })
        assertTrue(blobs.all { it.durationMs >= MIN_SOUND_DURATION_MS })
    }

    private fun soundBlob(
        trimStartMs: Int = 0,
        trimEndMs: Int = 1_000,
        sourceDurationMs: Int = 1_000,
        audioPath: String? = null,
    ): SoundBlob {
        return SoundBlob(
            id = "test",
            name = "Test",
            createdAtMillis = 0L,
            position = MapPoint(0f, 0f),
            colorArgb = BlobDefaults.palette.first(),
            shapePreset = BlobShapePreset.Pebble,
            shapePoints = BlobDefaults.shapeLibrary.first().second,
            waveform = listOf(0.5f, 0.8f),
            trimStartMs = trimStartMs,
            trimEndMs = trimEndMs,
            sourceDurationMs = sourceDurationMs,
            audioPath = audioPath,
        )
    }
}
