package com.appdex.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

// ═══════════════════════════════════════════════════════════════
// Micro-Interactions — Tactile press-scale feedback
// ═══════════════════════════════════════════════════════════════
// "Every tap should feel like pressing a physical button."
// Scale down slightly on press, spring back on release.
// ═══════════════════════════════════════════════════════════════

/**
 * Press-scale + clickable combined modifier.
 *
 * Scales the element down to [pressedScale] when pressed, then springs
 * back with a subtle bounce on release. Includes ripple indication.
 *
 * Use this as a drop-in replacement for `Modifier.clickable()`:
 * ```
 * // Before:
 * Modifier.background(color).clickable(onClick = onClick)
 * // After:
 * Modifier.background(color).bounceClick(onClick = onClick)
 * ```
 *
 * @param enabled Whether the click is enabled
 * @param pressedScale Scale factor when pressed (0.96 = 4% shrink)
 * @param onClick Click handler
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Press-scale modifier that shares an existing [interactionSource].
 *
 * Use this when you already have a `clickable` with a custom interactionSource
 * and just want to add the scale animation:
 * ```
 * val source = remember { MutableInteractionSource() }
 * Modifier
 *     .pressScale(source)
 *     .clickable(interactionSource = source, onClick = onClick)
 * ```
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
