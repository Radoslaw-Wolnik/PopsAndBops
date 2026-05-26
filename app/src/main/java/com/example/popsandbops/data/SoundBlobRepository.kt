package com.example.popsandbops.data

import android.content.Context
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
        preferences.edit().putString(KEY_BLOBS, json.toString()).apply()
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
            .put("waveform", JSONArray(waveform))
            .put("trimStartMs", trimStartMs)
            .put("trimEndMs", trimEndMs)
            .put("sourceDurationMs", sourceDurationMs)
            .put("isPinned", isPinned)
            .put("audioPath", audioPath)
            .put("builtInTone", builtInTone?.name)
    }

    private fun JSONObject.toSoundBlob(): SoundBlob {
        return SoundBlob(
            id = getString("id"),
            name = getString("name"),
            createdAtMillis = optLong("createdAtMillis", System.currentTimeMillis()),
            position = MapPoint(optDouble("x", 0.0).toFloat(), optDouble("y", 0.0).toFloat()),
            colorArgb = optLong("colorArgb", BlobDefaults.palette.first()),
            shapePreset = enumValueOrDefault(optString("shapePreset"), BlobShapePreset.Splash),
            shapePoints = optJSONArray("shapePoints")?.toFloatList().orEmpty()
                .ifEmpty { BlobDefaults.shapeLibrary.first().second },
            waveform = optJSONArray("waveform")?.toFloatList().orEmpty()
                .ifEmpty { BlobDefaults.generatedWaveform(id.hashCode(), 52) },
            trimStartMs = optInt("trimStartMs", 0),
            trimEndMs = optInt("trimEndMs", 1000),
            sourceDurationMs = optInt("sourceDurationMs", optInt("trimEndMs", 1000)),
            isPinned = optBoolean("isPinned", true),
            audioPath = optString("audioPath").takeIf { it.isNotBlank() && it != "null" },
            builtInTone = optString("builtInTone").takeIf { it.isNotBlank() && it != "null" }
                ?.let { enumValueOrNull<BuiltInTone>(it) },
        )
    }

    private fun JSONArray.toFloatList(): List<Float> {
        return List(length()) { index -> optDouble(index, 0.5).toFloat() }
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
    }
}
