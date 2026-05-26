package com.example.popsandbops.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

data class PressFeedback(
    val interactionSource: MutableInteractionSource,
    val isPressed: Boolean,
    val scale: Float,
)

@Composable
fun rememberPressFeedback(
    pressedScale: Float = 0.94f,
): PressFeedback {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press scale",
    )

    return PressFeedback(
        interactionSource = interactionSource,
        isPressed = isPressed,
        scale = scale,
    )
}
