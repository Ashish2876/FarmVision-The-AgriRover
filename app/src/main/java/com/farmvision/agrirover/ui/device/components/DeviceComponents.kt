package com.farmvision.agrirover.ui.device.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.domain.device.model.ConnectionState
import com.farmvision.agrirover.domain.device.model.TransportType

val ParrotGreen = Color(0xFF78E000)
val DarkSurface = Color(0xFF141915)
val DarkCardBg = Color(0xFF1E2420)
val StatusAmber = Color(0xFFFFB703)
val StatusRed = Color(0xFFE63946)
val StatusGray = Color(0xFF8A9A86)

@Composable
fun ConnectionStateBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (connectionState) {
        is ConnectionState.Connected -> Quad(
            ParrotGreen.copy(alpha = 0.2f),
            ParrotGreen,
            "CONNECTED",
            Icons.Default.CheckCircle
        )
        is ConnectionState.Connecting -> Quad(
            StatusAmber.copy(alpha = 0.2f),
            StatusAmber,
            "CONNECTING (Attempt ${connectionState.attempt})",
            Icons.Default.Refresh
        )
        is ConnectionState.Handshaking -> Quad(
            StatusAmber.copy(alpha = 0.2f),
            StatusAmber,
            "HANDSHAKING",
            Icons.Default.Refresh
        )
        is ConnectionState.Reconnecting -> Quad(
            StatusAmber.copy(alpha = 0.2f),
            StatusAmber,
            "RECONNECTING (${connectionState.attempt}/${connectionState.maxAttempts})",
            Icons.Default.Refresh
        )
        is ConnectionState.Failed -> Quad(
            StatusRed.copy(alpha = 0.2f),
            StatusRed,
            "FAILED",
            Icons.Default.Warning
        )
        ConnectionState.Disconnected -> Quad(
            StatusGray.copy(alpha = 0.2f),
            StatusGray,
            "DISCONNECTED",
            Icons.Default.Info
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun WifiSignalBars(
    rssiDbm: Int,
    modifier: Modifier = Modifier
) {
    val bars = when {
        rssiDbm >= -50 -> 4
        rssiDbm >= -65 -> 3
        rssiDbm >= -80 -> 2
        rssiDbm >= -95 -> 1
        else -> 0
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..4) {
            val isActive = i <= bars
            val height = (i * 4 + 4).dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) ParrotGreen else Color.DarkGray)
            )
        }
    }
}

@Composable
fun TransportTypeSelector(
    selectedType: TransportType,
    onSelect: (TransportType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCardBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TransportType.values().forEach { transport ->
            val isSelected = transport == selectedType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ParrotGreen.copy(alpha = 0.25f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) ParrotGreen else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(transport) }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (transport == TransportType.DirectEsp32SoftAp) "Wi-Fi Direct (SoftAP)" else "Cloud Gateway (FastAPI)",
                    color = if (isSelected) ParrotGreen else Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun DiagnosticMetricTile(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ParrotGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

