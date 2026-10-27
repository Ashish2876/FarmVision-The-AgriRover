package com.farmvision.agrirover.ui.rover.controller

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.ui.theme.BorderSubtle
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.LightLime
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.ParrotGreenDeep
import com.farmvision.agrirover.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Premium Virtual 2-Axis Robotics Joystick for tactile vector piloting.
 *
 * Features:
 * - Circular clamping to outer perimeter (vector magnitude <= 1.0).
 * - Continuous radial dead zone (0.08) with smooth linear rescaling.
 * - Single-pointer tracking: Ignores extra touch fingers so a second finger can hit Emergency Stop.
 * - Physics spring return animation to center on pointer release.
 * - High-contrast visual state indicators: Active (emerald glow), Idle, and Disabled (E-Stop cutoff).
 * - Full accessibility semantics and TalkBack announcements.
 */
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    knobSize: Dp = 56.dp,
    isEnabled: Boolean = true,
    driveMode: RoverDriveMode = RoverDriveMode.Manual,
    onVectorChanged: (JoystickVector) -> Unit,
    onRelease: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var currentVector by remember { mutableStateOf(JoystickVector.NEUTRAL) }

    val baseRadiusPx = remember(size, knobSize) {
        val totalRadius = (size.value / 2f)
        val knobRadius = (knobSize.value / 2f)
        // Allow the knob center to travel up to baseRadius - knobRadius
        (totalRadius - knobRadius).coerceAtLeast(10f)
    }

    val statusDescription = when {
        !isEnabled -> "Disabled, Emergency Stop engaged or manual control locked"
        driveMode != RoverDriveMode.Manual -> "Disabled, active mode is ${driveMode.label}"
        isDragging -> "Active, vector (${String.format(java.util.Locale.US, "%.2f", currentVector.x)}, ${String.format(java.util.Locale.US, "%.2f", currentVector.y)})"
        else -> "Ready, centered at zero"
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("rover_ctrl_joystick")
            .semantics {
                role = Role.Button
                contentDescription = "Virtual Piloting Joystick"
                stateDescription = statusDescription
            },
        contentAlignment = Alignment.Center
    ) {
        // Base Bounding Circle Canvas
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
                .border(
                    width = 2.dp,
                    color = if (isEnabled && isDragging) ParrotGreen else outlineColor,
                    shape = CircleShape
                )
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f

            // Concentric Range Rings (25%, 50%, 75%, 100%)
            val ringAlphas = listOf(0.12f, 0.18f, 0.24f)
            ringAlphas.forEachIndexed { index, alpha ->
                val r = outerRadius * (0.25f * (index + 1))
                drawCircle(
                    color = outlineColor.copy(alpha = alpha),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // Radial Dead Zone Marker Ring (0.08 threshold)
            drawCircle(
                color = if (isEnabled) DarkGreen.copy(alpha = 0.35f) else Color.Gray.copy(alpha = 0.2f),
                radius = outerRadius * JoystickVector.DEFAULT_DEAD_ZONE,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Crosshair Guide Lines
            val crosshairColor = outlineColor.copy(alpha = 0.35f)
            drawLine(
                color = crosshairColor,
                start = Offset(center.x, 0f),
                end = Offset(center.x, this.size.height),
                strokeWidth = 1f
            )
            drawLine(
                color = crosshairColor,
                start = Offset(0f, center.y),
                end = Offset(this.size.width, center.y),
                strokeWidth = 1f
            )
        }

        // Draggable Thumb Knob
        val knobOffset = IntOffset(
            x = animatedOffsetX.value.roundToInt(),
            y = animatedOffsetY.value.roundToInt()
        )

        val knobModifier = if (isEnabled && driveMode.allowsManualDrive) {
            Modifier.pointerInput(baseRadiusPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        // Calculate vector relative to center
                        val centerPx = size.toPx() / 2f
                        val rawVector = JoystickMath.screenToCartesian(
                            touchX = offset.x,
                            touchY = offset.y,
                            centerX = centerPx,
                            centerY = centerPx,
                            maxRadius = baseRadiusPx
                        )
                        val deadZoned = JoystickMath.applyRadialDeadZone(rawVector)
                        currentVector = deadZoned
                        onVectorChanged(deadZoned)

                        // Update knob visual position
                        coroutineScope.launch {
                            animatedOffsetX.snapTo(rawVector.x * baseRadiusPx)
                            animatedOffsetY.snapTo(-rawVector.y * baseRadiusPx) // Invert back for screen display
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = animatedOffsetX.value + dragAmount.x
                        val newY = animatedOffsetY.value + dragAmount.y

                        // Circular clamp knob inside base bounds
                        val dist = sqrt(newX * newX + newY * newY)
                        val clampedOffset = if (dist > baseRadiusPx && dist > 0f) {
                            Offset((newX / dist) * baseRadiusPx, (newY / dist) * baseRadiusPx)
                        } else {
                            Offset(newX, newY)
                        }

                        coroutineScope.launch {
                            animatedOffsetX.snapTo(clampedOffset.x)
                            animatedOffsetY.snapTo(clampedOffset.y)
                        }

                        // Normalize to [-1.0, 1.0] with inverted Y
                        val normX = (clampedOffset.x / baseRadiusPx).coerceIn(-1.0f, 1.0f)
                        val normY = (-clampedOffset.y / baseRadiusPx).coerceIn(-1.0f, 1.0f)
                        val rawVector = JoystickMath.clampToCircle(normX, normY)
                        val deadZoned = JoystickMath.applyRadialDeadZone(rawVector)

                        currentVector = deadZoned
                        onVectorChanged(deadZoned)
                    },
                    onDragEnd = {
                        isDragging = false
                        currentVector = JoystickVector.NEUTRAL
                        onVectorChanged(JoystickVector.NEUTRAL)
                        onRelease()

                        // Smooth spring return animation
                        coroutineScope.launch {
                            animatedOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        }
                        coroutineScope.launch {
                            animatedOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        currentVector = JoystickVector.NEUTRAL
                        onVectorChanged(JoystickVector.NEUTRAL)
                        onRelease()

                        coroutineScope.launch {
                            animatedOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            animatedOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }
                )
            }
        } else {
            // Disabled state: spring return to center immediately
            if (animatedOffsetX.value != 0f || animatedOffsetY.value != 0f) {
                coroutineScope.launch {
                    animatedOffsetX.snapTo(0f)
                    animatedOffsetY.snapTo(0f)
                }
            }
            Modifier
        }

        Box(
            modifier = Modifier
                .offset { knobOffset }
                .size(knobSize)
                .shadow(elevation = if (isEnabled && isDragging) 8.dp else 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = if (isEnabled) {
                        if (isDragging) {
                            Brush.radialGradient(listOf(ParrotGreen, ParrotGreenDeep))
                        } else {
                            Brush.radialGradient(listOf(LightLime, BorderSubtle))
                        }
                    } else {
                        Brush.radialGradient(listOf(Color.LightGray, Color.Gray))
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (isEnabled) {
                        if (isDragging) DarkGreen else DarkGreen.copy(alpha = 0.5f)
                    } else {
                        Color.Gray.copy(alpha = 0.5f)
                    },
                    shape = CircleShape
                )
                .then(knobModifier),
            contentAlignment = Alignment.Center
        ) {
            // Center Grip Nub / Directional Indicator
            Canvas(modifier = Modifier.size(16.dp)) {
                drawCircle(
                    color = if (isEnabled) {
                        if (isDragging) TextPrimary else DarkGreen
                    } else {
                        Color.Gray
                    },
                    radius = 4.dp.toPx()
                )
            }
        }
    }
}
