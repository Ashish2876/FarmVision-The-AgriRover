package com.farmvision.agrirover.ui.rover.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.ControlStationTheme
import com.farmvision.agrirover.ui.theme.ParrotGreen

/**
 * Camera Viewport for the Rover Control Station matching the reference station.
 * Renders an agricultural field simulation canvas with sunset sky, crop perspective rows,
 * and HUD framing reticle [ + ].
 */
@Composable
fun RoverCameraView(
    isSimulation: Boolean,
    isHardwareConnected: Boolean,
    selectedCamera: String = "Front Camera",
    onSelectCameraClick: (() -> Unit)? = null,
    onExpandClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ControlStationTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceGlassDeep)
            .border(BorderStroke(1.dp, colors.borderGlow), RoundedCornerShape(14.dp))
    ) {
        // Compose-drawn crop field sunset canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.45f

            // 1. Sunset Sky Gradient (Orange/Golden sunset to soft blue)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6B9AC4), // Dusk blue
                        Color(0xFFE89B5F), // Warm peach
                        Color(0xFFFFB347), // Golden orange
                        Color(0xFFFFCC33)  // Bright horizon glow
                    ),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset.Zero,
                size = Size(w, horizonY)
            )

            // Sun disk at horizon
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFDF0), Color(0x80FFD700), Color.Transparent),
                    center = Offset(w * 0.5f, horizonY * 0.95f),
                    radius = w * 0.12f
                ),
                radius = w * 0.12f,
                center = Offset(w * 0.5f, horizonY * 0.95f)
            )

            // Distant tree silhouette line along horizon
            val treePath = Path().apply {
                moveTo(0f, horizonY)
                var x = 0f
                while (x < w) {
                    val step = (w / 25f)
                    val treeHeight = horizonY * (0.88f + (kotlin.math.sin(x.toDouble()).toFloat() * 0.05f))
                    lineTo(x + step * 0.5f, treeHeight)
                    lineTo(x + step, horizonY)
                    x += step
                }
                lineTo(w, horizonY)
                close()
            }
            drawPath(
                path = treePath,
                color = Color(0xFF233B26)
            )

            // 2. Field Soil & Crop Perspective Rows
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2E241E), // Dark rich soil
                        Color(0xFF1E1712)
                    ),
                    startY = horizonY,
                    endY = h
                ),
                topLeft = Offset(0f, horizonY),
                size = Size(w, h - horizonY)
            )

            // Perspective crop rows radiating from center horizon
            val vanishingPoint = Offset(w * 0.5f, horizonY * 0.98f)
            val rowGreen = Color(0xFF388E3C)
            val cropBrightGreen = Color(0xFF66BB6A)

            for (i in -8..8) {
                val bottomX = vanishingPoint.x + i * (w * 0.14f)
                val rowPath = Path().apply {
                    moveTo(vanishingPoint.x, vanishingPoint.y)
                    lineTo(bottomX - (w * 0.04f), h)
                    lineTo(bottomX + (w * 0.04f), h)
                    close()
                }
                drawPath(
                    path = rowPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(rowGreen.copy(alpha = 0.4f), cropBrightGreen, rowGreen),
                        startY = horizonY,
                        endY = h
                    )
                )
            }

            // 3. Central HUD Focus Reticle [ + ]
            val reticleCenter = Offset(w * 0.5f, h * 0.52f)
            val reticleSize = w * 0.09f
            val cornerLen = reticleSize * 0.35f
            val reticleColor = ParrotGreen
            val strokeW = 2.2f

            // Center plus crosshair
            val crossSize = reticleSize * 0.22f
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x - crossSize, reticleCenter.y),
                end = Offset(reticleCenter.x + crossSize, reticleCenter.y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x, reticleCenter.y - crossSize),
                end = Offset(reticleCenter.x, reticleCenter.y + crossSize),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Top-left bracket corner
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x - reticleSize, reticleCenter.y - reticleSize + cornerLen),
                end = Offset(reticleCenter.x - reticleSize, reticleCenter.y - reticleSize),
                strokeWidth = strokeW
            )
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x - reticleSize, reticleCenter.y - reticleSize),
                end = Offset(reticleCenter.x - reticleSize + cornerLen, reticleCenter.y - reticleSize),
                strokeWidth = strokeW
            )

            // Top-right bracket corner
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x + reticleSize, reticleCenter.y - reticleSize + cornerLen),
                end = Offset(reticleCenter.x + reticleSize, reticleCenter.y - reticleSize),
                strokeWidth = strokeW
            )
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x + reticleSize, reticleCenter.y - reticleSize),
                end = Offset(reticleCenter.x + reticleSize - cornerLen, reticleCenter.y - reticleSize),
                strokeWidth = strokeW
            )

            // Bottom-left bracket corner
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x - reticleSize, reticleCenter.y + reticleSize - cornerLen),
                end = Offset(reticleCenter.x - reticleSize, reticleCenter.y + reticleSize),
                strokeWidth = strokeW
            )
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x - reticleSize, reticleCenter.y + reticleSize),
                end = Offset(reticleCenter.x - reticleSize + cornerLen, reticleCenter.y + reticleSize),
                strokeWidth = strokeW
            )

            // Bottom-right bracket corner
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x + reticleSize, reticleCenter.y + reticleSize - cornerLen),
                end = Offset(reticleCenter.x + reticleSize, reticleCenter.y + reticleSize),
                strokeWidth = strokeW
            )
            drawLine(
                color = reticleColor,
                start = Offset(reticleCenter.x + reticleSize, reticleCenter.y + reticleSize),
                end = Offset(reticleCenter.x + reticleSize - cornerLen, reticleCenter.y + reticleSize),
                strokeWidth = strokeW
            )
        }

        // Top Overlay: Camera Dropdown & Simulation Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera dropdown pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC0D1710))
                    .border(BorderStroke(1.dp, colors.borderSubtle), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCamera,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Honesty status badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC0D1710))
                    .border(BorderStroke(1.dp, ParrotGreen.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isHardwareConnected && !isSimulation) ParrotGreen else Color(0xFF66BB6A))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isSimulation) "DEMO (Simulated)" else if (isHardwareConnected) "LIVE STREAM" else "DISCONNECTED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = ParrotGreen
                    )
                )
            }
        }

        // Top Right: Fullscreen/expand button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xCC0D1710))
                .border(BorderStroke(1.dp, colors.borderSubtle), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Fullscreen,
                contentDescription = "Expand Camera",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

