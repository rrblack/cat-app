package com.rrblack.meowtranslator

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rrblack.meowtranslator.audio.AudioDecoder
import com.rrblack.meowtranslator.audio.AudioRecorder
import com.rrblack.meowtranslator.audio.MicrophoneUnavailableException
import com.rrblack.meowtranslator.data.AppLanguage
import com.rrblack.meowtranslator.core.MeowTranslatorEngine
import com.rrblack.meowtranslator.core.audio.PcmAudio
import com.rrblack.meowtranslator.core.classify.LabeledExample
import com.rrblack.meowtranslator.core.classify.VocalType
import com.rrblack.meowtranslator.core.dsp.Resampler
import com.rrblack.meowtranslator.core.translate.CatProfile
import com.rrblack.meowtranslator.core.translate.Intent
import com.rrblack.meowtranslator.core.translate.Translation
import com.rrblack.meowtranslator.data.HistoryEntry
import com.rrblack.meowtranslator.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Phase { Idle, Listening, Analyzing }

/** Snackbar messages; rendered in the current language by the UI. */
enum class UiMessage { MIC_PERMISSION, MIC_UNAVAILABLE, RECORDING_FAILED, FILE_UNREADABLE, RECORDING_MISSING }

data class Message(val kind: UiMessage, val detail: String? = null)

data class UiState(
    val phase: Phase = Phase.Idle,
    val level: Float = 0f,
    val elapsedSec: Float = 0f,
    val heardSound: Boolean = false,
    val result: Translation? = null,
    /** History entry backing [result], used for replay and teaching. */
    val resultEntry: HistoryEntry? = null,
    val history: List<HistoryEntry> = emptyList(),
    val profile: CatProfile = CatProfile(),
    val examples: List<LabeledExample> = emptyList(),
    val playingId: String? = null,
    val message: Message? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
) {
    val canTeach: Boolean get() = result?.vocalType == VocalType.MEOW && result.learnableFeatures != null && resultEntry != null
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val engine = MeowTranslatorEngine()
    private val recorder = AudioRecorder()
    private var recordJob: Job? = null
    private var player: MediaPlayer? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                UiState(history = repo.loadHistory(), profile = repo.loadProfile(), examples = repo.loadExamples(), language = repo.loadLanguage())
            }
            _state.update {
                it.copy(history = loaded.history, profile = loaded.profile, examples = loaded.examples, language = loaded.language)
            }
        }
    }

    fun startListening() {
        if (_state.value.phase != Phase.Idle) return
        stopPlayback()
        _state.update { it.copy(phase = Phase.Listening, level = 0f, elapsedSec = 0f, heardSound = false, message = null) }
        recordJob = viewModelScope.launch {
            try {
                val audio = recorder.record { p ->
                    _state.update {
                        it.copy(level = AudioRecorder.normalizedLevel(p.levelDb), elapsedSec = p.elapsedSec, heardSound = p.heardSound)
                    }
                }
                translate(audio, save = true)
            } catch (e: MicrophoneUnavailableException) {
                _state.update { it.copy(phase = Phase.Idle, message = Message(UiMessage.MIC_UNAVAILABLE)) }
            } catch (e: Exception) {
                _state.update { it.copy(phase = Phase.Idle, message = Message(UiMessage.RECORDING_FAILED, e.message)) }
            }
        }
    }

    fun stopListening() = recorder.requestStop()

    fun importAudio(uri: Uri) {
        if (_state.value.phase != Phase.Idle) return
        stopPlayback()
        _state.update { it.copy(phase = Phase.Analyzing, message = null) }
        viewModelScope.launch {
            try {
                val audio = withContext(Dispatchers.IO) { AudioDecoder.decode(getApplication(), uri) }
                translate(audio, save = true)
            } catch (e: Exception) {
                _state.update { it.copy(phase = Phase.Idle, message = Message(UiMessage.FILE_UNREADABLE, e.message)) }
            }
        }
    }

    /** Re-translates a saved recording (with everything the app has learned since). */
    fun openHistory(entry: HistoryEntry) {
        if (_state.value.phase != Phase.Idle) return
        _state.update { it.copy(phase = Phase.Analyzing, message = null) }
        viewModelScope.launch {
            val audio = withContext(Dispatchers.IO) { repo.loadAudio(entry.id) }
            if (audio == null) {
                _state.update { it.copy(phase = Phase.Idle, message = Message(UiMessage.RECORDING_MISSING)) }
                return@launch
            }
            val translation = withContext(Dispatchers.Default) { runEngine(audio) }
            _state.update { it.copy(phase = Phase.Idle, result = translation, resultEntry = entry) }
        }
    }

    private suspend fun translate(audio: PcmAudio, save: Boolean) {
        _state.update { it.copy(phase = Phase.Analyzing) }
        val translation = withContext(Dispatchers.Default) { runEngine(audio) }
        val entry = if (save && translation.primary != null) {
            withContext(Dispatchers.IO) { repo.addToHistory(translation, compactForStorage(audio)) }
        } else {
            null
        }
        _state.update {
            it.copy(
                phase = Phase.Idle,
                result = translation,
                resultEntry = entry,
                history = if (entry != null) listOf(entry) + it.history.take(Repository.MAX_HISTORY - 1) else it.history,
            )
        }
    }

    private fun runEngine(audio: PcmAudio): Translation {
        val s = _state.value
        return engine.translate(audio.samples, audio.sampleRate, s.profile, s.examples)
    }

    /** 16 kHz is plenty for replay and re-analysis (analysis runs at 8 kHz). */
    private fun compactForStorage(audio: PcmAudio): PcmAudio =
        if (audio.sampleRate <= 16000) audio else PcmAudio(Resampler.resample(audio.samples, audio.sampleRate, 16000), 16000)

    /** The owner tells us what was really going on; this becomes a training example for their cat. */
    fun teach(intent: Intent) {
        val s = _state.value
        val features = s.result?.learnableFeatures ?: return
        val entry = s.resultEntry ?: return
        viewModelScope.launch {
            val updated = entry.copy(userLabel = intent)
            val examples = withContext(Dispatchers.IO) {
                repo.teach(entry.id, features, intent)
                repo.updateHistory(updated)
                repo.loadExamples()
            }
            _state.update { st ->
                st.copy(
                    examples = examples,
                    resultEntry = updated,
                    history = st.history.map { if (it.id == entry.id) updated else it },
                )
            }
        }
    }

    fun resetLearning() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.clearExamples() }
            _state.update { st ->
                st.copy(
                    examples = emptyList(),
                    history = st.history.map { it.copy(userLabel = null) },
                    resultEntry = st.resultEntry?.copy(userLabel = null),
                )
            }
        }
    }

    fun deleteHistory(entry: HistoryEntry) {
        if (_state.value.playingId == entry.id) stopPlayback()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deleteHistory(entry.id) }
            _state.update { st ->
                st.copy(
                    history = st.history.filterNot { it.id == entry.id },
                    resultEntry = st.resultEntry?.takeUnless { it.id == entry.id },
                )
            }
        }
    }

    fun updateProfile(profile: CatProfile) {
        _state.update { it.copy(profile = profile) }
        viewModelScope.launch(Dispatchers.IO) { repo.saveProfile(profile) }
    }

    fun togglePlayback(entryId: String) {
        if (_state.value.playingId == entryId) {
            stopPlayback()
            return
        }
        stopPlayback()
        val file = repo.audioFile(entryId)
        if (!file.exists()) return
        player = MediaPlayer().apply {
            setDataSource(file.path)
            setOnCompletionListener { stopPlayback() }
            prepare()
            start()
        }
        _state.update { it.copy(playingId = entryId) }
    }

    private fun stopPlayback() {
        player?.release()
        player = null
        _state.update { it.copy(playingId = null) }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun showMessage(kind: UiMessage) = _state.update { it.copy(message = Message(kind)) }

    fun setLanguage(language: AppLanguage) {
        _state.update { it.copy(language = language) }
        viewModelScope.launch(Dispatchers.IO) { repo.saveLanguage(language) }
    }

    override fun onCleared() {
        recordJob?.cancel()
        player?.release()
    }
}
