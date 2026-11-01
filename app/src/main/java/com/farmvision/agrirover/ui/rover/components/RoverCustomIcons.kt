package com.farmvision.agrirover.ui.rover.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.ui.theme.ParrotGreen

/**
 * Custom vector icons drawn via Compose Canvas for the Rover Control Station.
 * 100% vector-scaled, zero external library dependencies.
 */
object RoverCustomIcons {

    @Composable
    fun Tractor(
        modifier: Modifier = Modifier.size(24.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            // Big rear wheel (lower-left)
            val rearRadius = w * 0.18f
            val rearCenter = Offset(w * 0.30f, h * 0.72f)
            drawCircle(
                color = tint,
                radius = rearRadius,
                center = rearCenter,
                style = Stroke(width = w * 0.08f)
            )
            drawCircle(
                color = tint,
                radius = rearRadius * 0.35f,
                center = rearCenter
            )

            // Small front wheel (lower-right)
            val frontRadius = w * 0.12f
            val frontCenter = Offset(w * 0.78f, h * 0.78f)
            drawCircle(
                color = tint,
                radius = frontRadius,
                center = frontCenter,
                style = Stroke(width = w * 0.08f)
            )

            // Cabin and hood chassis lines
            val bodyPath = Path().apply {
                moveTo(w * 0.28f, h * 0.54f)
                lineTo(w * 0.28f, h * 0.28f) // cabin back
                lineTo(w * 0.52f, h * 0.28f) // roof
                lineTo(w * 0.60f, h * 0.48f) // windshield
                lineTo(w * 0.85f, h * 0.48f) // hood
                lineTo(w * 0.85f, h * 0.68f) // grill
                lineTo(w * 0.65f, h * 0.68f)
                // Cabin divider
                moveTo(w * 0.52f, h * 0.28f)
                lineTo(w * 0.52f, h * 0.54f)
                // Exhaust pipe
                moveTo(w * 0.76f, h * 0.48f)
                lineTo(w * 0.76f, h * 0.32f)
            }
            drawPath(
                path = bodyPath,
                color = tint,
                style = Stroke(
                    width = w * 0.08f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    @Composable
    fun SteeringWheel(
        modifier: Modifier = Modifier.size(24.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.5f)
            val outerRadius = w * 0.42f
            val hubRadius = w * 0.13f

            // Outer wheel rim
            drawCircle(
                color = tint,
                radius = outerRadius,
                center = center,
                style = Stroke(width = w * 0.09f)
            )

            // Center hub
            drawCircle(
                color = tint,
                radius = hubRadius,
                center = center
            )

            // Three spokes: Left, Right, Bottom
            val strokeW = w * 0.08f
            // Left spoke
            drawLine(
                color = tint,
                start = Offset(center.x - outerRadius + strokeW * 0.5f, center.y),
                end = Offset(center.x - hubRadius, center.y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            // Right spoke
            drawLine(
                color = tint,
                start = Offset(center.x + hubRadius, center.y),
                end = Offset(center.x + outerRadius - strokeW * 0.5f, center.y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            // Bottom spoke
            drawLine(
                color = tint,
                start = Offset(center.x, center.y + hubRadius),
                end = Offset(center.x, center.y + outerRadius - strokeW * 0.5f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun Gamepad(
        modifier: Modifier = Modifier.size(24.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val strokeW = w * 0.08f

            // Controller body outline
            val body = Path().apply {
                moveTo(w * 0.28f, h * 0.32f)
                lineTo(w * 0.72f, h * 0.32f)
                quadraticTo(w * 0.90f, h * 0.32f, w * 0.88f, h * 0.60f)
                lineTo(w * 0.82f, h * 0.82f)
                quadraticTo(w * 0.72f, h * 0.88f, w * 0.65f, h * 0.74f)
                lineTo(w * 0.58f, h * 0.60f)
                lineTo(w * 0.42f, h * 0.60f)
                lineTo(w * 0.35f, h * 0.74f)
                quadraticTo(w * 0.28f, h * 0.88f, w * 0.18f, h * 0.82f)
                lineTo(w * 0.12f, h * 0.60f)
                quadraticTo(w * 0.10f, h * 0.32f, w * 0.28f, h * 0.32f)
                close()
            }
            drawPath(
                path = body,
                color = tint,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // D-Pad cross on left
            drawLine(
                color = tint,
                start = Offset(w * 0.24f, h * 0.48f),
                end = Offset(w * 0.38f, h * 0.48f),
                strokeWidth = strokeW * 1.1f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.31f, h * 0.41f),
                end = Offset(w * 0.31f, h * 0.55f),
                strokeWidth = strokeW * 1.1f,
                cap = StrokeCap.Round
            )

            // Action buttons on right
            drawCircle(color = tint, radius = strokeW * 0.6f, center = Offset(w * 0.65f, h * 0.44f))
            drawCircle(color = tint, radius = strokeW * 0.6f, center = Offset(w * 0.74f, h * 0.52f))
        }
    }

    @Composable
    fun Flask(
        modifier: Modifier = Modifier.size(24.dp),
        tint: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height
            val strokeW = w * 0.08f

            val flaskPath = Path().apply {
                moveTo(w * 0.40f, h * 0.15f)
                lineTo(w * 0.60f, h * 0.15f)
                moveTo(w * 0.43f, h * 0.15f)
                lineTo(w * 0.43f, h * 0.40f)
                lineTo(w * 0.18f, h * 0.82f)
                quadraticTo(w * 0.16f, h * 0.88f, w * 0.25f, h * 0.88f)
                lineTo(w * 0.75f, h * 0.88f)
                quadraticTo(w * 0.84f, h * 0.88f, w * 0.82f, h * 0.82f)
                lineTo(w * 0.57f, h * 0.40f)
                lineTo(w * 0.57f, h * 0.15f)
            }
            drawPath(
                path = flaskPath,
                color = tint,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Liquid level line
            drawLine(
                color = tint,
                start = Offset(w * 0.29f, h * 0.66f),
                end = Offset(w * 0.71f, h * 0.66f),
                strokeWidth = strokeW * 0.9f,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun FarmLeaf(
        modifier: Modifier = Modifier.size(24.dp),
        tint: Color = ParrotGreen
    ) {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            val leafPath = Path().apply {
                moveTo(w * 0.18f, h * 0.82f)
                cubicTo(w * 0.22f, h * 0.50f, w * 0.48f, h * 0.20f, w * 0.82f, h * 0.15f)
                cubicTo(w * 0.85f, h * 0.48f, w * 0.60f, h * 0.78f, w * 0.28f, h * 0.82f)
                close()
            }
            drawPath(
                path = leafPath,
                color = tint
            )

            val veinPath = Path().apply {
                moveTo(w * 0.22f, h * 0.80f)
                quadraticTo(w * 0.45f, h * 0.55f, w * 0.78f, h * 0.20f)
            }
            drawPath(
                path = veinPath,
                color = Color(0xFF132218),
                style = Stroke(width = w * 0.06f, cap = StrokeCap.Round)
            )
        }
    }
}
