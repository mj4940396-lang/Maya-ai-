package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.MayaCoral
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaEmerald
import com.example.ui.theme.MayaMidnight
import com.example.ui.theme.MayaViolet

@Composable
fun MicButton(
    state: AssistantState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val animatedAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        animatedAmp.animateTo(
            targetValue = amplitude.coerceIn(0f, 1f),
            animationSpec = tween(80)
        )
    }

    val isListening = state == AssistantState.LISTENING
    val isSpeaking = state == AssistantState.SPEAKING
    val isThinking = state == AssistantState.THINKING

    val primaryColor = when {
        isListening -> MayaCyan
        isSpeaking -> MayaEmerald
        isThinking -> MayaViolet
        state == AssistantState.ERROR -> MayaCoral
        else -> MayaCyan
    }

    val secondaryColor = when {
        isListening -> MayaViolet
        isSpeaking -> MayaCyan
        else -> MayaViolet
    }

    Box(
        modifier = modifier
            .size(92.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ripple ring when listening or speaking
        if (isListening || isSpeaking) {
            val rippleScale = if (isListening) pulseScale + (animatedAmp.value * 0.25f) else pulseScale
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .scale(rippleScale)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.2f))
                    .border(2.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
            )
        }

        // Main interactive circle button
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .testTag("mic_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White)
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = state,
                label = "mic_icon_transition"
            ) { targetState ->
                when (targetState) {
                    AssistantState.LISTENING -> {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Listening - tap to stop",
                            tint = MayaMidnight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    AssistantState.THINKING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MayaMidnight,
                            strokeWidth = 3.dp
                        )
                    }
                    AssistantState.SPEAKING -> {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop speaking",
                            tint = MayaMidnight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    AssistantState.IDLE, AssistantState.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Tap to speak",
                            tint = MayaMidnight,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
