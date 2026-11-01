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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Icon
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
 * Tactical Mini-Map Card for the Rover Control Station matching the reference station.
 * Draws a stylized agricultural field grid with rover position, heading marker, and honesty tag.
 */
@Composable
fun MiniMapCard(
    modifier: Modifier = Modifier,
    headingDegrees: Float = 45f
) {
    val colors = ControlStationTheme.colors

    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(12.dp),
        borderColor = colors.borderGlowHigh
    ) {
        // Canvas drawn field grid map
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background terrain gradient
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1B3B24), Color(0xFF0F2215)),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.7f
                )
            )

            // Farm plot field boundaries (diagonal perspective lines)
            val gridColor = Color(0x3378E000)
            val strokeWidth = 1.2f

            // Diagonal crop row guides
            for (i in -4..8) {
                val startX = i * (w / 5)
                drawLine(
                    color = gridColor,
                    start = Offset(startX, 0f),
                    end = Offset(startX + w * 0.5f, h),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = gridColor,
                    start = Offset(startX + w * 0.2f, 0f),
                    end = Offset(startX - w * 0.3f, h),
                    strokeWidth = strokeWidth * 0.8f
                )
            }

            // Highlighting active scanned swath
            val swathPath = Path().apply {
                moveTo(w * 0.40f, h * 0.20f)
                lineTo(w * 0.65f, h * 0.45f)
                lineTo(w * 0.55f, h * 0.65f)
                lineTo(w * 0.30f, h * 0.40f)
                close()
            }
            drawPath(
                path = swathPath,
                color = Color(0x2278E000)
            )

            // Rover Marker (Lime green triangle with drop shadow)
            val markerCenter = Offset(w * 0.52f, h * 0.48f)
            val markerSize = 14f

            val arrowPath = Path().apply {
                moveTo(markerCenter.x, markerCenter.y - markerSize)
                lineTo(markerCenter.x + markerSize * 0.75f, markerCenter.y + markerSize)
                lineTo(markerCenter.x, markerCenter.y + markerSize * 0.4f)
                lineTo(markerCenter.x - markerSize * 0.75f, markerCenter.y + markerSize)
                close()
            }

            // Pulse ring around rover
            drawCircle(
                color = Color(0x4078E000),
                radius = markerSize * 1.8f,
                center = markerCenter
            )

            drawPath(
                path = arrowPath,
                color = ParrotGreen
            )
        }

        // Overlay controls and chips
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            // Top Right: Signal & HD chip
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.SignalCellularAlt,
                    contentDescription = null,
                    tint = ParrotGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "HD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // Bottom Left: Layer toggle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x99000000))
                    .border(BorderStroke(0.8.dp, colors.borderSubtle), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = "Map Layers",
                    tint = colors.textMuted,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Bottom Right: North compass indicator
            Text(
                text = "N",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 2.dp)
            )

            // Top Left: Honesty disclosure
            Text(
                text = "SIMULATED MAP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ParrotGreen.copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xAA0D1710))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }
    }
}

