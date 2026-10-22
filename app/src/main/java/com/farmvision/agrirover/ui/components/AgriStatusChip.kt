package com.farmvision.agrirover.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.StatusInfo
import com.farmvision.agrirover.ui.theme.StatusOnline
import com.farmvision.agrirover.ui.theme.StatusStandby
import com.farmvision.agrirover.ui.theme.StatusWarning

/**
 * Semantic status classification for rover telemetry, hardware nodes, and sensor thresholds.
 */
enum class AgriStatusType(
    val color: Color,
    val defaultLabel: String
) {
    Nominal(StatusOnline, "NOMINAL"),
    Warning(StatusWarning, "WARNING"),
    Alert(StatusAlert, "ALERT"),
    Info(StatusInfo, "SYNCING"),
    Standby(StatusStandby, "STANDBY")
}

/**
 * Reusable AgriStatusChip displaying live system status with a subtle animated pulse dot.
 */
@Composable
fun AgriStatusChip(
    type: AgriStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier,
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, type.color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse Beacon Dot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(10.dp)
            ) {
                if (isPulsing) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(type.color)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(type.color)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = customLabel ?: type.defaultLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.75.sp,
                color = type.color
            )
        }
    }
}

@Preview(name = "Status Chips Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun AgriStatusChipPreview() {
    FarmVisionTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            AgriStatusChip(type = AgriStatusType.Nominal)
            Spacer(modifier = Modifier.width(8.dp))
            AgriStatusChip(type = AgriStatusType.Warning)
            Spacer(modifier = Modifier.width(8.dp))
            AgriStatusChip(type = AgriStatusType.Alert)
        }
    }
}
