package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantState
import com.example.ui.components.ConversationSection
import com.example.ui.components.MayaOrb
import com.example.ui.components.MayaTopBar
import com.example.ui.components.MicButton
import com.example.ui.components.QuickPrompts
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.MayaCardBg
import com.example.ui.theme.MayaCoral
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaEmerald
import com.example.ui.theme.MayaMidnight
import com.example.ui.theme.MayaSurfaceElevated
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.MayaViewModel
import kotlinx.coroutines.launch

@Composable
fun MayaScreen(
    viewModel: MayaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val assistantState by viewModel.assistantState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentTranscript by viewModel.currentTranscript.collectAsState()
    val audioAmplitude by viewModel.audioAmplitude.collectAsState()
    val languageMode by viewModel.languageMode.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val speechSpeed by viewModel.speechSpeed.collectAsState()
    val isApiKeyConfigured by viewModel.isApiKeyConfigured.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var typedMessage by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for voice conversation.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleMicAction() {
        when (assistantState) {
            AssistantState.LISTENING -> {
                viewModel.stopListening()
            }
            AssistantState.SPEAKING -> {
                viewModel.interruptSpeaking()
            }
            AssistantState.THINKING -> {
                // Keep processing
            }
            AssistantState.IDLE, AssistantState.ERROR -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    viewModel.startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
                viewModel.dismissError()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MayaMidnight),
        containerColor = MayaMidnight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MayaTopBar(
                languageMode = languageMode,
                isMuted = isMuted,
                onLanguageClick = { showSettingsDialog = true },
                onMuteToggle = { viewModel.toggleMute() },
                onClearClick = { showClearConfirm = true },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Interactive Hero Orb Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                MayaOrb(
                    state = assistantState,
                    amplitude = audioAmplitude,
                    onClick = { handleMicAction() }
                )
            }

            // Status Badge & Live Transcription
            StatusPill(
                state = assistantState,
                liveTranscript = currentTranscript,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            // Quick Voice Suggestion Prompts
            QuickPrompts(
                onPromptSelected = { promptText ->
                    viewModel.processUserInput(promptText)
                },
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Conversation Messages History
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ConversationSection(
                    messages = messages,
                    onReplayMessage = { msg -> viewModel.replayMessage(msg) }
                )
            }

            // Expandable Text Input Bar
            AnimatedVisibility(
                visible = showTextInput,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MayaCardBg)
                        .border(1.dp, MayaCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = {
                            Text(
                                text = "Message Maya (English, Urdu, Roman Urdu)…",
                                color = TextMutedDark,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            cursorColor = MayaCyan
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (typedMessage.isNotBlank()) {
                                viewModel.processUserInput(typedMessage)
                                typedMessage = ""
                                showTextInput = false
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_input_field")
                    )

                    IconButton(
                        onClick = {
                            if (typedMessage.isNotBlank()) {
                                viewModel.processUserInput(typedMessage)
                                typedMessage = ""
                                showTextInput = false
                            }
                        },
                        modifier = Modifier.testTag("send_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MayaCyan
                        )
                    }

                    IconButton(
                        onClick = { showTextInput = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close input",
                            tint = TextMutedDark
                        )
                    }
                }
            }

            // Bottom Voice & Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keyboard toggle button
                IconButton(
                    onClick = { showTextInput = !showTextInput },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MayaSurfaceElevated)
                        .testTag("toggle_keyboard_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Type a message",
                        tint = if (showTextInput) MayaCyan else TextSecondaryDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Centered Animated Mic Button
                MicButton(
                    state = assistantState,
                    amplitude = audioAmplitude,
                    onClick = { handleMicAction() }
                )

                // Interrupt / Stop Speaking Button (when active) or Language quick view
                if (assistantState == AssistantState.SPEAKING) {
                    IconButton(
                        onClick = { viewModel.interruptSpeaking() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MayaCoral.copy(alpha = 0.2f))
                            .border(1.dp, MayaCoral, CircleShape)
                            .testTag("interrupt_voice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Maya",
                            tint = MayaCoral,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    // Language shortcut
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MayaSurfaceElevated)
                            .clickable { showSettingsDialog = true }
                            .testTag("lang_badge_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (languageMode) {
                                com.example.data.model.LanguageMode.URDU -> "اردو"
                                com.example.data.model.LanguageMode.ROMAN_URDU -> "RU"
                                com.example.data.model.LanguageMode.ENGLISH -> "EN"
                                com.example.data.model.LanguageMode.AUTO -> "AUTO"
                            },
                            color = MayaCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Settings & Language Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentLanguage = languageMode,
            currentSpeed = speechSpeed,
            isApiKeyConfigured = isApiKeyConfigured,
            activeModel = activeModel,
            onLanguageSelect = { mode ->
                viewModel.setLanguageMode(mode)
            },
            onSpeedSelect = { speed ->
                viewModel.setSpeechSpeed(speed)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Clear Conversation Confirm Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(text = "Clear Conversation?", color = TextPrimaryDark)
            },
            text = {
                Text(
                    text = "This will reset your conversation with Maya AI.",
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MayaCoral)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = MayaSurfaceElevated,
            modifier = Modifier.testTag("clear_dialog")
        )
    }
}
