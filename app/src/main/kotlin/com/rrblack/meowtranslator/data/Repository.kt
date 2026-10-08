package com.rrblack.meowtranslator.data

import android.content.Context
import com.rrblack.meowtranslator.core.analysis.SegmentFeatures
import com.rrblack.meowtranslator.core.audio.PcmAudio
import com.rrblack.meowtranslator.core.audio.WavIO
import com.rrblack.meowtranslator.core.classify.LabeledExample
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.translate.AgeGroup
import com.rrblack.meowtranslator.core.translate.CatProfile
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Translation
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** One past translation, kept so it can be replayed, re-translated, or used to teach the app. */
data class HistoryEntry(
    val id: String,
    val timestamp: Long,
    val headline: String,
    val intent: Intent,
    val vocalType: VocalType,
    val confidence: Double,
    val emotionLabel: String,
    /** What the owner said was really happening, if they told us. */
    val userLabel: Intent?,
)

/** Small JSON-file store in the app's private storage. All calls do blocking I/O: use a background dispatcher. */
class Repository(context: Context) {
    private val dir = context.filesDir
    private val recordings = File(dir, "recordings").apply { mkdirs() }
    private val historyFile = File(dir, "history.json")
    private val profileFile = File(dir, "profile.json")
    private val examplesFile = File(dir, "taught_examples.json")

    // ---- Profile -------------------------------------------------------------------------------

    fun loadProfile(): CatProfile {
        val o = readObject(profileFile) ?: return CatProfile()
        return CatProfile(
            name = o.optString("name"),
            ageGroup = runCatching { AgeGroup.valueOf(o.optString("ageGroup")) }.getOrDefault(AgeGroup.ADULT),
            neutered = if (o.has("neutered") && !o.isNull("neutered")) o.getBoolean("neutered") else null,
        )
    }

    fun saveProfile(p: CatProfile) {
        val o = JSONObject().put("name", p.name).put("ageGroup", p.ageGroup.name).put("neutered", p.neutered ?: JSONObject.NULL)
        write(profileFile, o.toString())
    }

    // ---- History -------------------------------------------------------------------------------

    fun loadHistory(): List<HistoryEntry> {
        val arr = readArray(historyFile) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            runCatching {
                HistoryEntry(
                    id = o.getString("id"),
                    timestamp = o.getLong("timestamp"),
                    headline = o.getString("headline"),
                    intent = Intent.valueOf(o.getString("intent")),
                    vocalType = VocalType.valueOf(o.getString("vocalType")),
                    confidence = o.getDouble("confidence"),
                    emotionLabel = o.optString("emotion"),
                    userLabel = o.optString("userLabel").takeIf { it.isNotEmpty() }?.let { Intent.valueOf(it) },
                )
            }.getOrNull()
        }
    }

    /** Stores a new translation and its audio; returns the saved entry. Keeps the newest [MAX_HISTORY]. */
    fun addToHistory(translation: Translation, audio: PcmAudio): HistoryEntry {
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            headline = translation.headline,
            intent = translation.intent,
            vocalType = translation.vocalType,
            confidence = translation.confidence,
            emotionLabel = translation.emotion.label,
            userLabel = null,
        )
        audioFile(entry.id).outputStream().use { WavIO.write(audio, it) }
        val all = listOf(entry) + loadHistory()
        all.drop(MAX_HISTORY).forEach { audioFile(it.id).delete() }
        saveHistory(all.take(MAX_HISTORY))
        return entry
    }

    fun updateHistory(entry: HistoryEntry) = saveHistory(loadHistory().map { if (it.id == entry.id) entry else it })

    fun deleteHistory(id: String) {
        audioFile(id).delete()
        saveHistory(loadHistory().filterNot { it.id == id })
    }

    fun audioFile(id: String) = File(recordings, "$id.wav")

    fun loadAudio(id: String): PcmAudio? = audioFile(id).takeIf { it.exists() }?.inputStream()?.use { WavIO.read(it) }

    private fun saveHistory(entries: List<HistoryEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("timestamp", e.timestamp)
                    .put("headline", e.headline)
                    .put("intent", e.intent.name)
                    .put("vocalType", e.vocalType.name)
                    .put("confidence", e.confidence)
                    .put("emotion", e.emotionLabel)
                    .put("userLabel", e.userLabel?.name ?: ""),
            )
        }
        write(historyFile, arr.toString())
    }

    // ---- Taught examples (personal model) -------------------------------------------------------

    /** Examples whose feature layout matches the current engine (older layouts are ignored, not crashed on). */
    fun loadExamples(): List<LabeledExample> =
        loadExampleRecords().map { it.second }.filter { it.features.size == SegmentFeatures.FEATURE_NAMES.size }

    /** Adds or replaces the owner's label for a recording. */
    fun teach(entryId: String, features: DoubleArray, intent: Intent) {
        val records = loadExampleRecords().filterNot { it.first == entryId } + (entryId to LabeledExample(features, intent))
        val arr = JSONArray()
        for ((id, ex) in records) {
            arr.put(JSONObject().put("id", id).put("intent", ex.intent.name).put("features", JSONArray(ex.features.toList())))
        }
        write(examplesFile, arr.toString())
    }

    fun clearExamples() {
        examplesFile.delete()
        saveHistory(loadHistory().map { it.copy(userLabel = null) })
    }

    private fun loadExampleRecords(): List<Pair<String, LabeledExample>> {
        val arr = readArray(examplesFile) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val f = o.getJSONArray("features")
                o.getString("id") to LabeledExample(DoubleArray(f.length()) { f.getDouble(it) }, Intent.valueOf(o.getString("intent")))
            }.getOrNull()
        }
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private fun readObject(f: File) = if (f.exists()) runCatching { JSONObject(f.readText()) }.getOrNull() else null
    private fun readArray(f: File) = if (f.exists()) runCatching { JSONArray(f.readText()) }.getOrNull() else null

    /** Write-then-rename so a crash mid-write can't corrupt the file. */
    private fun write(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) {
            f.writeText(text)
            tmp.delete()
        }
    }

    companion object {
        const val MAX_HISTORY = 100
    }
}
