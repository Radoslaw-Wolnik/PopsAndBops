package com.example.popsandbops.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SoundBlobRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("sound_blobs", Context.MODE_PRIVATE)
    private val recordingsDir = File(appContext.filesDir, "recordings").apply { mkdirs() }

    fun loadBlobs(): List<SoundBlob> {
        val raw = preferences.getString(KEY_BLOBS, null) ?: return BlobDefaults.defaultSoundBlobs()
        return runCatching {
            val json = JSONArray(raw)
            List(json.length()) { index -> json.getJSONObject(index).toSoundBlob() }
        }.getOrElse { BlobDefaults.defaultSoundBlobs() }
    }

    fun saveBlobs(blobs: List<SoundBlob>) {
        val json = JSONArray()
        blobs.forEach { json.put(it.toJson()) }
        preferences.edit { putString(KEY_BLOBS, json.toString()) }
    }

    fun createRecordingFile(): File {
        return File(recordingsDir, "pops_${System.currentTimeMillis()}.m4a")
    }

    fun createRecordingBlob(
        audioPath: String,
        durationMs: Int,
        waveform: List<Float>,
        existingCount: Int,
    ): SoundBlob {
        val now = System.currentTimeMillis()
        val shape = BlobDefaults.shapeLibrary[existingCount % BlobDefaults.shapeLibrary.size]
        return SoundBlob(
            id = UUID.randomUUID().toString(),
            name = defaultRecordingName(now),
            createdAtMillis = now,
            position = BlobDefaults.edgePosition(existingCount),
            colorArgb = BlobDefaults.palette[existingCount % BlobDefaults.palette.size],
            shapePreset = shape.first,
            shapePoints = shape.second,
            curveTension = shape.curveTension,
            waveform = waveform.ifEmpty { BlobDefaults.generatedWaveform(existingCount + 19, 64) },
            trimStartMs = 0,
            trimEndMs = durationMs.coerceIn(250, MAX_RECORDING_MS),
            audioPath = audioPath,
        )
    }

    fun defaultRecordingName(now: Long): String {
        return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(now))
    }

    private fun SoundBlob.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("createdAtMillis", createdAtMillis)
            .put("x", position.x)
            .put("y", position.y)
            .put("colorArgb", colorArgb)
            .put("shapePreset", shapePreset.name)
            .put("shapePoints", JSONArray(shapePoints))
            .put("curveTension", curveTension)
            .put("waveform", JSONArray(waveform))
            .put("trimStartMs", trimStartMs)
            .put("trimEndMs", trimEndMs)
            .put("sourceDurationMs", sourceDurationMs)
            .put("isPinned", isPinned)
            .put("audioPath", audioPath)
            .put("builtInTone", builtInTone?.name)
    }

    private fun JSONObject.toSoundBlob(): SoundBlob {
        val blobId = getString("id")
        val sourceDurationMs = optInt("sourceDurationMs", optInt("trimEndMs", 1000))
            .coerceAtLeast(MIN_SOUND_DURATION_MS)
        val trim = sanitizeTrimRange(
            startMs = optInt("trimStartMs", 0),
            endMs = optInt("trimEndMs", sourceDurationMs),
            sourceDurationMs = sourceDurationMs,
        )
        val fallbackShape = BlobDefaults.shapeLibrary.first().second
        return SoundBlob(
            id = blobId,
            name = getString("name"),
            createdAtMillis = optLong("createdAtMillis", System.currentTimeMillis()),
            position = MapPoint(optDouble("x", 0.0).toFloat(), optDouble("y", 0.0).toFloat()),
            colorArgb = optLong("colorArgb", BlobDefaults.palette.first()),
            shapePreset = enumValueOrDefault(optString("shapePreset"), BlobShapePreset.Splash),
            shapePoints = optJSONArray("shapePoints")?.toFloatList().orEmpty()
                .sanitizeShapePoints(fallbackShape),
            curveTension = optDouble("curveTension", DEFAULT_BLOB_CURVE_TENSION.toDouble())
                .toFloat()
                .coerceIn(MIN_CURVE_TENSION, MAX_CURVE_TENSION),
            waveform = optJSONArray("waveform")?.toFloatList().orEmpty()
                .sanitizeWaveform(blobId.hashCode()),
            trimStartMs = trim.startMs,
            trimEndMs = trim.endMs,
            sourceDurationMs = sourceDurationMs,
            isPinned = optBoolean("isPinned", true),
            audioPath = optString("audioPath").takeIf { it.isNotBlank() && it != "null" },
            builtInTone = optString("builtInTone").takeIf { it.isNotBlank() && it != "null" }
                ?.let { enumValueOrNull<BuiltInTone>(it) },
        )
    }

    private fun JSONArray.toFloatList(): List<Float> {
        return List(length()) { index -> optDouble(index, 0.5).toFloat() }
    }

    private fun List<Float>.sanitizeShapePoints(fallback: List<Float>): List<Float> {
        val points = take(MAX_SHAPE_POINTS).map { it.coerceIn(MIN_SHAPE_POINT, MAX_SHAPE_POINT) }
        return points.takeIf { it.size >= MIN_SHAPE_POINTS } ?: fallback
    }

    private fun List<Float>.sanitizeWaveform(seed: Int): List<Float> {
        return map { it.coerceIn(MIN_WAVEFORM_POINT, 1f) }
            .take(MAX_WAVEFORM_POINTS)
            .ifEmpty { BlobDefaults.generatedWaveform(seed, 52) }
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? {
        return enumValues<T>().firstOrNull { it.name == name }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, fallback: T): T {
        return enumValueOrNull<T>(name) ?: fallback
    }

    private companion object {
        const val KEY_BLOBS = "blobs"
        const val MAX_RECORDING_MS = 10_000
        const val MIN_SHAPE_POINTS = 5
        const val MAX_SHAPE_POINTS = 24
        const val MIN_SHAPE_POINT = 0.48f
        const val MAX_SHAPE_POINT = 1.52f
        const val MIN_CURVE_TENSION = 0.04f
        const val MAX_CURVE_TENSION = 0.38f
        const val MIN_WAVEFORM_POINT = 0.08f
        const val MAX_WAVEFORM_POINTS = 128
    }
}
