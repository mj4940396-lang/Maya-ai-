package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.MayaCoral
import com.example.ui.theme.MayaCyan
import com.example.ui.theme.MayaEmerald
import com.example.ui.theme.MayaViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MayaOrb(
    state: AssistantState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Smooth amplitude transitions
    val animatedAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        animatedAmp.animateTo(
            targetValue = amplitude.coerceIn(0f, 1f),
            animationSpec = tween(100)
        )
    }

    val primaryGlow = when (state) {
        AssistantState.LISTENING -> MayaCyan
        AssistantState.THINKING -> MayaViolet
        AssistantState.SPEAKING -> MayaEmerald
        AssistantState.ERROR -> MayaCoral
        AssistantState.IDLE -> MayaCyan
    }

    val secondaryGlow = when (state) {
        AssistantState.LISTENING -> MayaViolet
        AssistantState.THINKING -> MayaCyan
        AssistantState.SPEAKING -> MayaViolet
        AssistantState.ERROR -> MayaViolet
        AssistantState.IDLE -> MayaViolet
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .testTag("maya_orb")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.28f
            val dynamicScale = pulseScale + (animatedAmp.value * 0.35f)
            val coreRadius = baseRadius * dynamicScale

            // 1. Outermost Ambient Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.35f),
                        secondaryGlow.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 2.2f
                ),
                radius = coreRadius * 2.2f,
                center = center
            )

            // 2. Animated Particle Rings (when listening or speaking)
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING || state == AssistantState.THINKING) {
                val ringCount = 3
                for (i in 0 until ringCount) {
                    val ringRadius = coreRadius * (1.2f + i * 0.22f + (animatedAmp.value * 0.2f))
                    val ringAlpha = (0.4f - i * 0.1f).coerceAtLeast(0.05f)
                    drawCircle(
                        color = primaryGlow.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = (2.5f - i * 0.5f).dp.toPx())
                    )
                }
            }

            // 3. Orbiting light points (during thinking/processing)
            if (state == AssistantState.THINKING) {
                val orbitRadius = coreRadius * 1.35f
                for (angleOffset in listOf(0f, 120f, 240f)) {
                    val totalAngle = Math.toRadians((rotationAngle + angleOffset).toDouble())
                    val x = center.x + (orbitRadius * cos(totalAngle)).toFloat()
                    val y = center.y + (orbitRadius * sin(totalAngle)).toFloat()
                    drawCircle(
                        color = primaryGlow,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // 4. Middle Glowing Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        primaryGlow.copy(alpha = 0.85f),
                        secondaryGlow.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 1.15f
                ),
                radius = coreRadius,
                center = center
            )

            // 5. Central Bright Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryGlow.copy(alpha = 0.8f)
                    ),
                    center = center,
                    radius = coreRadius * 0.55f
                ),
                radius = coreRadius * 0.5f,
                center = center
            )
        }
    }
}
