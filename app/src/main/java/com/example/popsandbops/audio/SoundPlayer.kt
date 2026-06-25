package com.example.popsandbops.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.example.popsandbops.data.BuiltInTone
import com.example.popsandbops.data.SoundBlob
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

class SoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var finishRunnable: Runnable? = null

    fun play(blob: SoundBlob, onFinished: () -> Unit): Boolean {
        stop()
        return if (blob.audioPath != null) {
            playRecording(blob, onFinished)
        } else {
            playTone(blob, onFinished)
        }
    }

    fun stop() {
        finishRunnable?.let(handler::removeCallbacks)
        finishRunnable = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { audioTrack?.stop() }
        runCatching { audioTrack?.release() }
        audioTrack = null
    }

    private fun playRecording(blob: SoundBlob, onFinished: () -> Unit): Boolean {
        val audioPath = blob.audioPath ?: return false
        return runCatching {
            val trim = blob.trimRange
            val player = MediaPlayer().apply {
                setDataSource(audioPath)
                setOnCompletionListener { finish(onFinished) }
                prepare()
                seekTo(trim.startMs)
                start()
            }
            mediaPlayer = player
            finishRunnable = Runnable { finish(onFinished) }
            handler.postDelayed(finishRunnable!!, trim.durationMs.toLong())
        }.onFailure {
            stop()
        }.isSuccess
    }

    private fun playTone(blob: SoundBlob, onFinished: () -> Unit): Boolean {
        return runCatching {
            val toneLength = blob.durationMs.coerceIn(180, 1_500)
            val samples = synthesizeTone(blob.builtInTone ?: BuiltInTone.Pop, toneLength)
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                samples.size * BYTES_PER_SAMPLE,
                AudioTrack.MODE_STATIC,
            ).apply {
                write(samples, 0, samples.size)
                play()
            }
            audioTrack = track
            finishRunnable = Runnable { finish(onFinished) }
            handler.postDelayed(finishRunnable!!, toneLength.toLong())
        }.onFailure {
            stop()
        }.isSuccess
    }

    private fun finish(onFinished: () -> Unit) {
        stop()
        onFinished()
    }

    private fun synthesizeTone(tone: BuiltInTone, durationMs: Int): ShortArray {
        val sampleCount = (SAMPLE_RATE * durationMs / 1_000f).roundToInt().coerceAtLeast(1)
        return ShortArray(sampleCount) { index ->
            val t = index / SAMPLE_RATE.toFloat()
            val progress = index / sampleCount.toFloat()
            val value = when (tone) {
                BuiltInTone.Pop -> pop(progress, t)
                BuiltInTone.Bleep -> bubbleZap(progress, t)
                BuiltInTone.Bounce -> velvetBoing(progress, t)
                BuiltInTone.Pulse -> tinyOrbit(progress, t)
                BuiltInTone.Spark -> lemonPulse(progress, t)
                BuiltInTone.Chime -> glassChime(progress, t)
                BuiltInTone.Pluck -> softPluck(progress, t)
                BuiltInTone.Flutter -> fireflyFlutter(progress, t)
            }.coerceIn(-1f, 1f)
            (value * Short.MAX_VALUE * 0.70f).roundToInt().toShort()
        }
    }

    private fun pop(progress: Float, t: Float): Float {
        val envelope = percussiveEnvelope(progress, attack = 0.02f, decay = 9.5f)
        val pitch = sweep(from = 620f, to = 180f, progress = progress)
        return sine(pitch, t) * envelope
    }

    private fun bubbleZap(progress: Float, t: Float): Float {
        val envelope = percussiveEnvelope(progress, attack = 0.04f, decay = 5.8f)
        val wobble = 1f + 0.12f * sine(10f, t)
        val pitch = sweep(from = 340f, to = 940f, progress = progress) * wobble
        return (sine(pitch, t) * 0.78f + sine(pitch * 1.5f, t) * 0.22f) * envelope
    }

    private fun velvetBoing(progress: Float, t: Float): Float {
        val envelope = percussiveEnvelope(progress, attack = 0.05f, decay = 4.6f)
        val boing = 1f + sin(progress * PI.toFloat() * 5f) * (1f - progress) * 0.32f
        val pitch = sweep(from = 240f, to = 390f, progress = progress) * boing
        return (sine(pitch, t) * 0.65f + sine(pitch * 0.5f, t) * 0.28f) * envelope
    }

    private fun tinyOrbit(progress: Float, t: Float): Float {
        val envelope = softEnvelope(progress)
        val gate = if ((progress * 8f).roundToInt() % 2 == 0) 0.92f else 0.42f
        val orbit = sine(440f, t) * 0.55f + sine(660f + sine(7f, t) * 50f, t) * 0.32f
        return orbit * envelope * gate
    }

    private fun lemonPulse(progress: Float, t: Float): Float {
        val envelope = softEnvelope(progress)
        val pulse = 0.55f + 0.45f * (sine(8f, t) * 0.5f + 0.5f)
        return (sine(520f, t) * 0.72f + sine(1_040f, t) * 0.22f) * envelope * pulse
    }

    private fun glassChime(progress: Float, t: Float): Float {
        val envelope = percussiveEnvelope(progress, attack = 0.01f, decay = 3.7f)
        val shimmer = sine(880f, t) * 0.42f + sine(1_320f, t) * 0.34f + sine(1_760f, t) * 0.20f
        return shimmer * envelope
    }

    private fun softPluck(progress: Float, t: Float): Float {
        val envelope = percussiveEnvelope(progress, attack = 0.015f, decay = 7.2f)
        val pitch = 330f + sine(5f, t) * 6f
        return (triangle(pitch, t) * 0.72f + sine(pitch * 2f, t) * 0.18f) * envelope
    }

    private fun fireflyFlutter(progress: Float, t: Float): Float {
        val envelope = softEnvelope(progress)
        val flutter = 0.52f + 0.48f * (sine(13f, t) * 0.5f + 0.5f)
        val pitch = sweep(from = 700f, to = 1_080f, progress = progress) + sine(19f, t) * 45f
        return (sine(pitch, t) * 0.62f + sine(pitch * 1.25f, t) * 0.20f) * envelope * flutter
    }

    private fun percussiveEnvelope(progress: Float, attack: Float, decay: Float): Float {
        val attackGain = (progress / attack).coerceIn(0f, 1f)
        return attackGain * exp(-decay * progress)
    }

    private fun softEnvelope(progress: Float): Float {
        val attack = (progress / 0.08f).coerceIn(0f, 1f)
        val release = ((1f - progress) / 0.22f).coerceIn(0f, 1f)
        return attack * release
    }

    private fun sine(frequency: Float, t: Float): Float {
        return sin(2f * PI.toFloat() * frequency * t)
    }

    private fun triangle(frequency: Float, t: Float): Float {
        val phase = (frequency * t) % 1f
        return 4f * kotlin.math.abs(phase - 0.5f) - 1f
    }

    private fun sweep(from: Float, to: Float, progress: Float): Float {
        return from + (to - from) * progress.coerceIn(0f, 1f)
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val BYTES_PER_SAMPLE = 2
    }
}
