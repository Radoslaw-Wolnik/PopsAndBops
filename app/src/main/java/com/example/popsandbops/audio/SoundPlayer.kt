package com.example.popsandbops.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.example.popsandbops.data.BuiltInTone
import com.example.popsandbops.data.SoundBlob

class SoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var toneGenerator: ToneGenerator? = null
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
        runCatching { toneGenerator?.stopTone() }
        runCatching { toneGenerator?.release() }
        toneGenerator = null
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
            val tone = when (blob.builtInTone) {
                BuiltInTone.Pop -> ToneGenerator.TONE_PROP_BEEP
                BuiltInTone.Bleep -> ToneGenerator.TONE_PROP_BEEP2
                BuiltInTone.Bounce -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                BuiltInTone.Pulse -> ToneGenerator.TONE_CDMA_CONFIRM
                BuiltInTone.Spark, null -> ToneGenerator.TONE_PROP_ACK
            }
            val toneLength = blob.durationMs.coerceIn(160, 1_500)
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 82).apply {
                startTone(tone, toneLength)
            }
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
}
