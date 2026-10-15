package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import com.example.data.model.LanguageMode
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onSpeechPartialResult: (String) -> Unit,
    private val onSpeechFinalResult: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onSpeechError: (String) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : RecognitionListener {

    companion object {
        private const val TAG = "VoiceManager"
        private const val UTTERANCE_ID = "maya_speech_utterance"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    var isMuted: Boolean = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onSpeakingStateChanged(false)
                    }
                })
                textToSpeech?.setSpeechRate(speechRate)
            } else {
                Log.e(TAG, "TextToSpeech init failed with status: $status")
            }
        }
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageMode: LanguageMode) {
        stopSpeaking()

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            val localeTag = when (languageMode) {
                LanguageMode.URDU, LanguageMode.ROMAN_URDU -> "ur-PK"
                LanguageMode.ENGLISH -> "en-US"
                LanguageMode.AUTO -> Locale.getDefault().toLanguageTag()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ur-PK", "en-US"))
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognizer", e)
            onSpeechError("Could not start microphone: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer", e)
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling recognizer", e)
        }
    }

    fun speak(text: String, audioBase64: String? = null, languageMode: LanguageMode = LanguageMode.AUTO) {
        if (isMuted) return

        stopSpeaking()

        // 1. If Gemini Live returned raw audio (e.g. PCM / MP3 / WAV from Live API)
        if (!audioBase64.isNullOrBlank()) {
            val playedAudio = playBase64Audio(audioBase64)
            if (playedAudio) return
        }

        // 2. Play via Android TextToSpeech
        if (!isTtsInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS not ready yet")
            return
        }

        // Configure locale based on language or Urdu detection
        val isUrduScript = text.any { it in '\u0600'..'\u06FF' }
        val locale = when {
            isUrduScript || languageMode == LanguageMode.URDU -> Locale("ur", "PK")
            languageMode == LanguageMode.ROMAN_URDU -> Locale.US // Roman Urdu reads phonetically well with default/US/UK TTS
            languageMode == LanguageMode.ENGLISH -> Locale.US
            else -> Locale.getDefault()
        }

        val langResult = textToSpeech?.isLanguageAvailable(locale)
        if (langResult == TextToSpeech.LANG_AVAILABLE ||
            langResult == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            langResult == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        ) {
            textToSpeech?.language = locale
        } else {
            textToSpeech?.language = Locale.US
        }

        val cleanText = cleanTextForSpeech(text)
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    private fun playBase64Audio(base64Data: String): Boolean {
        return try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val tempFile = File.createTempFile("maya_audio_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener {
                    onSpeakingStateChanged(true)
                    it.start()
                }
                setOnCompletionListener {
                    onSpeakingStateChanged(false)
                    it.release()
                    mediaPlayer = null
                    tempFile.delete()
                }
                setOnErrorListener { mp, _, _ ->
                    onSpeakingStateChanged(false)
                    mp.release()
                    mediaPlayer = null
                    tempFile.delete()
                    false
                }
                prepareAsync()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio stream, falling back to TTS", e)
            false
        }
    }

    fun stopSpeaking() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
            textToSpeech?.stop()
            onSpeakingStateChanged(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("[#*`_~>\\[\\]()!]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying VoiceManager", e)
        }
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {}

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize rmsdB (-2 to 10 typical in Android) to 0f..1f range
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        onRmsChanged(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        onRmsChanged(0f)
    }

    override fun onError(error: Int) {
        onRmsChanged(0f)
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Speech recognition error ($error)"
        }
        Log.w(TAG, "SpeechRecognizer error: $errorMessage")
        onSpeechError(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val recognized = matches?.firstOrNull() ?: ""
        if (recognized.isNotBlank()) {
            onSpeechFinalResult(recognized)
        } else {
            onSpeechError("Didn't catch that. Please tap the mic and try again.")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull() ?: ""
        if (partial.isNotBlank()) {
            onSpeechPartialResult(partial)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
