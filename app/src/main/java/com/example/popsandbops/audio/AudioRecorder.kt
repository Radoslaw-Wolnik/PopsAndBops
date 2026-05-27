package com.example.popsandbops.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

data class RecordingResult(
    val file: File,
    val durationMs: Int,
    val waveform: List<Float>,
)

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L
    private val samples = mutableListOf<Float>()

    val isRecording: Boolean
        get() = recorder != null

    fun start(file: File) {
        stopQuietly()
        samples.clear()
        outputFile = file
        recorder = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(128_000)
            setMaxDuration(MAX_RECORDING_MS)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        startedAtMs = System.currentTimeMillis()
    }

    fun elapsedMs(): Int {
        return if (startedAtMs == 0L) 0 else (System.currentTimeMillis() - startedAtMs).toInt()
    }

    fun recordSample(): Float {
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        val normalized = (amplitude / Short.MAX_VALUE.toFloat()).coerceIn(0f, 1f)
        samples.add(normalized)
        return normalized
    }

    fun liveWaveform(): List<Float> {
        return WaveformNormalizer.live(samples, LIVE_WAVEFORM_BARS)
    }

    fun stop(): RecordingResult? {
        val file = outputFile ?: return null
        val duration = elapsedMs().coerceIn(0, MAX_RECORDING_MS)
        val safeStop = runCatching { recorder?.stop() }.isSuccess
        release()
        return if (safeStop && file.exists() && duration >= MIN_RECORDING_MS) {
            RecordingResult(
                file = file,
                durationMs = duration,
                waveform = WaveformNormalizer.summary(samples, SAVED_WAVEFORM_BARS),
            )
        } else {
            file.delete()
            null
        }
    }

    fun cancel() {
        val file = outputFile
        release()
        file?.delete()
    }

    private fun stopQuietly() {
        if (recorder != null) {
            runCatching { recorder?.stop() }
            release()
        }
    }

    private fun release() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile = null
        startedAtMs = 0L
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    companion object {
        const val MAX_RECORDING_MS = 10_000
        const val MIN_RECORDING_MS = 250
        private const val LIVE_WAVEFORM_BARS = 64
        private const val SAVED_WAVEFORM_BARS = 72
    }
}
