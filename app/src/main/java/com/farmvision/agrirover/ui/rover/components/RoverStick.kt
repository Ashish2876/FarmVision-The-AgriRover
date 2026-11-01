package com.farmvision.agrirover.ui.rover.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

enum class StickAxisLock {
    Vertical,   // Move: Throttle (Y-only)
    Horizontal, // Steer: Steering (X-only)
    Free        // Omnidirectional
}

/**
 * Premium tactile rover joystick with glowing green outer ring, axis isolation,
 * multi-touch pointer tracking, draw-phase offset animation, and spring-back centering.
 */
@Composable
fun RoverStick(
    axisLock: StickAxisLock,
    label: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    knobSize: Dp = 62.dp,
    enabled: Boolean = true,
    testTag: String? = null,
    centerContent: @Composable () -> Unit
) {
    val colors = ControlStationTheme.colors
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val sizePx = with(density) { size.toPx() }
    val knobSizePx = with(density) { knobSize.toPx() }
    val maxRadiusPx = (sizePx - knobSizePx) / 2f

    // Animated knob offset for zero-recomposition draw updates and smooth spring return
    val knobOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // Failsafe: on disposal or disabled, ensure neutral is emitted and knob reset
    DisposableEffect(enabled) {
        if (!enabled) {
            coroutineScope.launch { knobOffset.snapTo(Offset.Zero) }
            onValueChange(0f)
        }
        onDispose {
            onValueChange(0f)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .semantics {
                    contentDescription = "$label control stick"
                    role = Role.Button
                }
                .pointerInput(enabled, maxRadiusPx) {
                    if (!enabled) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val trackingPointerId = down.id
                        var currentDragOffset = Offset.Zero

                        down.consume()

                        do {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.firstOrNull { it.id == trackingPointerId }

                            if (pointerChange != null && pointerChange.pressed) {
                                val delta = pointerChange.positionChange()
                                val rawNewOffset = currentDragOffset + delta

                                // Apply axis lock
                                val lockedOffset = when (axisLock) {
                                    StickAxisLock.Vertical -> Offset(0f, rawNewOffset.y)
                                    StickAxisLock.Horizontal -> Offset(rawNewOffset.x, 0f)
                                    StickAxisLock.Free -> rawNewOffset
                                }

                                // Clamp to max radius
                                val distance = sqrt(lockedOffset.x * lockedOffset.x + lockedOffset.y * lockedOffset.y)
                                val clampedOffset = if (distance > maxRadiusPx && distance > 0f) {
                                    val scale = maxRadiusPx / distance
                                    Offset(lockedOffset.x * scale, lockedOffset.y * scale)
                                } else {
                                    lockedOffset
                                }

                                currentDragOffset = clampedOffset
                                pointerChange.consume()

                                coroutineScope.launch {
                                    knobOffset.snapTo(clampedOffset)
                                }

                                // Calculate scalar value:
                                // Vertical: screen upward (negative Y) is +1.0 (Forward), downward is -1.0 (Reverse)
                                // Horizontal: screen right (positive X) is +1.0 (Right), left is -1.0 (Left)
                                val normalizedVal = when (axisLock) {
                                    StickAxisLock.Vertical -> -clampedOffset.y / maxRadiusPx
                                    StickAxisLock.Horizontal -> clampedOffset.x / maxRadiusPx
                                    StickAxisLock.Free -> -clampedOffset.y / maxRadiusPx
                                }
                                onValueChange(normalizedVal.coerceIn(-1.0f, 1.0f))
                            }
                        } while (event.changes.any { it.id == trackingPointerId && it.pressed })

                        // Release / Cancel -> spring return to center and emit neutral
                        onValueChange(0f)
                        coroutineScope.launch {
                            knobOffset.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Glowing outer circular background pad & ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(sizePx / 2f, sizePx / 2f)
                val outerRadius = sizePx / 2f - 4f

                // Under-glow radial gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3378E000), Color(0x0578E000), Color.Transparent),
                        center = center,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = center
                )

                // Dark glass base fill
                drawCircle(
                    color = Color(0xCC0D1710),
                    radius = outerRadius,
                    center = center
                )

                // Outer parrot-green glowing ring
                drawCircle(
                    color = ParrotGreen,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // Concentric inner boundary ring
                drawCircle(
                    color = Color(0x4078E000),
                    radius = maxRadiusPx,
                    center = center,
                    style = Stroke(width = 1.0f)
                )
            }

            // 2. Cardinal Chevrons
            val isVerticalActive = axisLock == StickAxisLock.Vertical || axisLock == StickAxisLock.Free
            val isHorizontalActive = axisLock == StickAxisLock.Horizontal || axisLock == StickAxisLock.Free

            // Top Chevron (Forward)
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = if (isVerticalActive) colors.stickChevron else colors.stickChevronDimmed,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .size(20.dp)
            )

            // Bottom Chevron (Reverse)
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isVerticalActive) colors.stickChevron else colors.stickChevronDimmed,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(20.dp)
            )

            // Left Chevron (Left)
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = if (isHorizontalActive) colors.stickChevron else colors.stickChevronDimmed,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(20.dp)
            )

            // Right Chevron (Right)
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isHorizontalActive) colors.stickChevron else colors.stickChevronDimmed,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(20.dp)
            )

            // 3. Central floating knob (translated via graphicsLayer for zero recomposition)
            Box(
                modifier = Modifier
                    .size(knobSize)
                    .graphicsLayer {
                        translationX = knobOffset.value.x
                        translationY = knobOffset.value.y
                    }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFFFF), colors.stickKnob, Color(0xFFC8D6C8))
                        )
                    )
                    .border(BorderStroke(2.dp, colors.stickKnobBorder), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                centerContent()
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stick label underneath
        Text(
            text = "$label ↹",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.textPrimary
            )
        )
    }
}

