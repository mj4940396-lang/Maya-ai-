package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceManager
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.LanguageMode
import com.example.data.model.MessageSender
import com.example.network.GeminiLiveClient
import com.example.network.GeminiResponseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MayaViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiClient = GeminiLiveClient()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val initialWelcomeMessage = ChatMessage(
        sender = MessageSender.MAYA,
        text = "Assalam-o-Alaikum! I am Maya AI. How can I help you today?"
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(initialWelcomeMessage))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _languageMode = MutableStateFlow(LanguageMode.AUTO)
    val languageMode: StateFlow<LanguageMode> = _languageMode.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _speechSpeed = MutableStateFlow(1.0f)
    val speechSpeed: StateFlow<Float> = _speechSpeed.asStateFlow()

    private val _isApiKeyConfigured = MutableStateFlow(geminiClient.isApiKeyConfigured())
    val isApiKeyConfigured: StateFlow<Boolean> = _isApiKeyConfigured.asStateFlow()

    private val _activeModel = MutableStateFlow(GeminiLiveClient.MODEL_LIVE_AUDIO)
    val activeModel: StateFlow<String> = _activeModel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val voiceManager: VoiceManager = VoiceManager(
        context = application.applicationContext,
        onSpeechPartialResult = { partial ->
            _currentTranscript.value = partial
        },
        onSpeechFinalResult = { finalResult ->
            _currentTranscript.value = finalResult
            processUserInput(finalResult)
        },
        onRmsChanged = { rms ->
            _audioAmplitude.value = rms
        },
        onSpeechError = { errorMsg ->
            _audioAmplitude.value = 0f
            _assistantState.value = AssistantState.IDLE
            _errorMessage.value = errorMsg
        },
        onSpeakingStateChanged = { isSpeaking ->
            if (isSpeaking) {
                _assistantState.value = AssistantState.SPEAKING
            } else if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.IDLE
                _audioAmplitude.value = 0f
            }
        }
    )

    init {
        // Speak initial greeting once initialized (if not muted)
        // Kept gentle so users don't get startled on first launch
    }

    fun startListening() {
        _errorMessage.value = null
        _currentTranscript.value = ""
        _assistantState.value = AssistantState.LISTENING
        voiceManager.startListening(_languageMode.value)
    }

    fun stopListening() {
        voiceManager.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun interruptSpeaking() {
        voiceManager.stopSpeaking()
        _assistantState.value = AssistantState.IDLE
        _audioAmplitude.value = 0f
    }

    fun processUserInput(text: String) {
        if (text.isBlank()) {
            _assistantState.value = AssistantState.IDLE
            return
        }

        voiceManager.stopSpeaking()
        _assistantState.value = AssistantState.THINKING
        _errorMessage.value = null

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = text.trim()
        )
        _messages.value = _messages.value + userMessage
        _currentTranscript.value = ""

        viewModelScope.launch {
            val responseResult = geminiClient.generateVoiceResponse(
                userInput = text,
                history = _messages.value.dropLast(1),
                languageMode = _languageMode.value,
                requestAudio = true
            )

            when (responseResult) {
                is GeminiResponseResult.Success -> {
                    _activeModel.value = responseResult.modelUsed
                    val mayaMessage = ChatMessage(
                        sender = MessageSender.MAYA,
                        text = responseResult.text,
                        audioBase64 = responseResult.audioBase64
                    )
                    _messages.value = _messages.value + mayaMessage

                    // Speak response
                    if (!_isMuted.value) {
                        _assistantState.value = AssistantState.SPEAKING
                        voiceManager.speak(
                            text = responseResult.text,
                            audioBase64 = responseResult.audioBase64,
                            languageMode = _languageMode.value
                        )
                    } else {
                        _assistantState.value = AssistantState.IDLE
                    }
                }
                is GeminiResponseResult.Error -> {
                    _assistantState.value = AssistantState.ERROR
                    _errorMessage.value = responseResult.message
                    val errorReply = ChatMessage(
                        sender = MessageSender.MAYA,
                        text = "I encountered an issue: ${responseResult.message}. Please check your connection or API key in AI Studio."
                    )
                    _messages.value = _messages.value + errorReply
                }
            }
        }
    }

    fun replayMessage(message: ChatMessage) {
        if (message.sender == MessageSender.MAYA) {
            voiceManager.speak(
                text = message.text,
                audioBase64 = message.audioBase64,
                languageMode = _languageMode.value
            )
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        _languageMode.value = mode
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        voiceManager.isMuted = newMute
        if (newMute) {
            voiceManager.stopSpeaking()
            if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    fun setSpeechSpeed(speed: Float) {
        _speechSpeed.value = speed
        voiceManager.speechRate = speed
    }

    fun clearHistory() {
        voiceManager.stopSpeaking()
        _assistantState.value = AssistantState.IDLE
        _messages.value = listOf(initialWelcomeMessage)
        _currentTranscript.value = ""
        _errorMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
        if (_assistantState.value == AssistantState.ERROR) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
