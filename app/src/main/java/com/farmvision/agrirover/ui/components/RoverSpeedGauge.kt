package com.farmvision.agrirover.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.DeepForest
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.MintAccent
import com.farmvision.agrirover.ui.theme.StatusWarning
import com.farmvision.agrirover.ui.theme.TextMutedDark
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision circular arc telemetry gauge for rover speed, motor RPM, and battery status.
 * Visualizes operational velocity with smooth physics-damping animation and calibrated ticks.
 */
@Composable
fun RoverSpeedGauge(
    currentValue: Float,
    maxValue: Float,
    unit: String,
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    secondaryNote: String? = null
) {
    val clampedRatio = (currentValue / maxValue).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(
        targetValue = clampedRatio,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "gaugeSweep"
    )

    val startAngle = 140f
    val totalSweepAngle = 260f
    val currentSweepAngle = totalSweepAngle * animatedRatio

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = size.toPx() - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // 1. Background Arc Track
            drawArc(
                color = DarkSurfaceVariant,
                startAngle = startAngle,
                sweepAngle = totalSweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active Gradient Arc
            if (currentSweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            DeepForest,
                            FreshLeaf,
                            MintAccent,
                            StatusWarning
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Precision Radial Tick Marks
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val tickRadius = (size.toPx() / 2) - strokeWidth - 6.dp.toPx()
            val tickCount = 11

            for (i in 0 until tickCount) {
                val tickFraction = i.toFloat() / (tickCount - 1)
                val angleDeg = startAngle + (tickFraction * totalSweepAngle)
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val startX = center.x + (tickRadius * cos(angleRad)).toFloat()
                val startY = center.y + (tickRadius * sin(angleRad)).toFloat()
                val tickLength = if (i % 2 == 0) 8.dp.toPx() else 4.dp.toPx()
                val endX = center.x + ((tickRadius - tickLength) * cos(angleRad)).toFloat()
                val endY = center.y + ((tickRadius - tickLength) * sin(angleRad)).toFloat()

                val tickColor = if (tickFraction <= animatedRatio) {
                    MintAccent.copy(alpha = 0.8f)
                } else {
                    TextMutedDark.copy(alpha = 0.3f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center Digital Telemetry Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = TextMutedDark
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = String.format("%.1f", currentValue),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = FreshLeaf
            )

            if (secondaryNote != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryNote,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextMutedDark.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(name = "Speed Gauge Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun RoverSpeedGaugePreview() {
    FarmVisionTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            RoverSpeedGauge(
                currentValue = 4.2f,
                maxValue = 6.0f,
                unit = "km/h",
                title = "Speed",
                secondaryNote = "LIMIT: 6.0 km/h"
            )
        }
    }
}
