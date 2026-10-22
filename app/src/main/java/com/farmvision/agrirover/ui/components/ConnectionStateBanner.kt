package com.farmvision.agrirover.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.ui.theme.DarkSurface
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.MintAccent
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.StatusInfo
import com.farmvision.agrirover.ui.theme.StatusOnline
import com.farmvision.agrirover.ui.theme.StatusWarning
import com.farmvision.agrirover.ui.theme.TextMutedDark

enum class ConnectionProtocol(val label: String, val icon: ImageVector) {
    BluetoothLowEnergy("BLE 5.0", Icons.Rounded.Bluetooth),
    WiFiSoftAP("Wi-Fi Direct", Icons.Rounded.Wifi),
    Disconnected("Offline", Icons.Rounded.WifiOff)
}

enum class LinkQuality(val label: String, val color: Color) {
    Excellent("Strong", StatusOnline),
    Moderate("Fair", StatusWarning),
    Poor("Weak", StatusAlert),
    Offline("Disconnected", StatusAlert)
}

/**
 * Top-anchored status banner displaying the real-time rover communication link,
 * protocol type, packet latency, signal quality, and reconnect triggers.
 */
@Composable
fun ConnectionStateBanner(
    protocol: ConnectionProtocol,
    isConnected: Boolean,
    latencyMs: Int,
    signalDbm: Int,
    quality: LinkQuality,
    modifier: Modifier = Modifier,
    onReconnectClick: (() -> Unit)? = null
) {
    val statusColor = if (isConnected) quality.color else StatusAlert

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Icon & Protocol
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = protocol.icon,
                        contentDescription = protocol.label,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) protocol.label else "ROVER DISCONNECTED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (isConnected) "${quality.label} • ${signalDbm} dBm" else "Telemetry link lost",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }
            }

            // Right Group: Latency & Reconnect Action
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isConnected) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${latencyMs} ms",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (latencyMs < 50) FreshLeaf else StatusWarning
                        )
                        Text(
                            text = "PING",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMutedDark
                        )
                    }
                } else if (onReconnectClick != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable(onClick = onReconnectClick)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reconnect",
                            tint = MintAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RECONNECT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MintAccent
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Connection Banner Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun ConnectionStateBannerPreview() {
    FarmVisionTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ConnectionStateBanner(
                protocol = ConnectionProtocol.WiFiSoftAP,
                isConnected = true,
                latencyMs = 28,
                signalDbm = -54,
                quality = LinkQuality.Excellent
            )
            ConnectionStateBanner(
                protocol = ConnectionProtocol.Disconnected,
                isConnected = false,
                latencyMs = 0,
                signalDbm = 0,
                quality = LinkQuality.Offline,
                onReconnectClick = {}
            )
        }
    }
}
