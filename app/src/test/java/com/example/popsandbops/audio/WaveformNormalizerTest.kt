package com.example.popsandbops.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformNormalizerTest {
    @Test
    fun liveWaveformUsesRecentSamplesWithoutPeakNormalizingEverything() {
        val waveform = WaveformNormalizer.live(
            samples = listOf(0.01f, 0.04f, 0.09f, 1f),
            maxBars = 4,
        )

        assertEquals(4, waveform.size)
        assertTrue(waveform[0] < waveform[1])
        assertTrue(waveform[1] < waveform[2])
        assertEquals(1f, waveform[3], FLOAT_TOLERANCE)
    }

    @Test
    fun liveWaveformAmplifiesQuietSpeechLevels() {
        val waveform = WaveformNormalizer.live(
            samples = listOf(0.002f, 0.02f),
            maxBars = 4,
        )

        assertTrue(waveform.last() > 0.4f)
    }

    @Test
    fun summaryWaveformDoesNotPadShortRecordingsWithRepeatedBars() {
        val waveform = WaveformNormalizer.summary(
            samples = listOf(0.1f, 0.2f, 0.3f),
            maxBars = 72,
        )

        assertEquals(3, waveform.size)
    }

    @Test
    fun emptyInputCreatesEmptyWaveform() {
        assertTrue(WaveformNormalizer.live(emptyList(), maxBars = 64).isEmpty())
        assertTrue(WaveformNormalizer.summary(emptyList(), maxBars = 72).isEmpty())
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.001f
    }
}
