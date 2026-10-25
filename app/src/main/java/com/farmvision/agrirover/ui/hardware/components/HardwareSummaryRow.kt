package com.farmvision.agrirover.ui.hardware.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.hardware.model.HardwareSummaryCounts
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Summary counter row strictly adhering to Section 8 counting rules:
 * - Counts only electronic devices that can report connectivity (13 total).
 * - Connected = authenticated live heartbeat (0 without hardware).
 * - Disconnected = known unreachable (0 if unpolled).
 * - Warning = device-reported warning or fault (0).
 * - Unknown / Draft = unconfigured/unpolled electronic hardware (13).
 * - Passive / Mechanical = structural/plumbing items (10).
 * - Zero overall health scores.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HardwareSummaryRow(
    counts: HardwareSummaryCounts,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalAgriStatusColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Hardware Inventory Status: " +
                    "${counts.confirmedConnected} Connected, " +
                    "${counts.disconnected} Disconnected, " +
                    "${counts.warningOrFault} Warning, " +
                    "${counts.unknownOrDraft} Unknown, " +
                    "${counts.passiveInventory} Passive items"
            },
        shape = RoundedCornerShape(FarmVisionSpacing.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(FarmVisionSpacing.borderThin, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventory Health & Discovery",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "23 Components Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // Responsive flow of count badges
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                SummaryPill(
                    count = counts.confirmedConnected,
                    label = "Connected",
                    icon = Icons.Rounded.CheckCircle,
                    color = statusColors.online
                )
                SummaryPill(
                    count = counts.disconnected,
                    label = "Disconnected",
                    icon = Icons.Rounded.Cancel,
                    color = statusColors.offline
                )
                SummaryPill(
                    count = counts.warningOrFault,
                    label = "Warning",
                    icon = Icons.Rounded.Warning,
                    color = statusColors.warning
                )
                SummaryPill(
                    count = counts.unknownOrDraft,
                    label = "Unknown / Draft",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SummaryPill(
                    count = counts.passiveInventory,
                    label = "Passive / Assembly",
                    icon = Icons.Rounded.AccountTree,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(
    count: Int,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(FarmVisionSpacing.small),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmVisionSpacing.small, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(FarmVisionSpacing.iconSizeSmall)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
