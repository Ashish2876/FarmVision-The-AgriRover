package com.farmvision.agrirover.ui.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.DataOrigin
import com.farmvision.agrirover.data.model.TelemetrySample
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Paginated reverse-chronological list of telemetry records (Default D12).
 * Shows individual sample cards with stable keys, provenance badges, and 'Load More' action.
 */
@Composable
fun HistoricalRecordsList(
    records: List<TelemetrySample>,
    totalCount: Int,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        FarmVisionSectionHeader(
            title = "Historical Log Entries",
            description = "Reverse chronological record stream ($totalCount total logged)"
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        if (records.isEmpty()) {
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No historical records logged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)) {
                records.forEach { sample ->
                    RecordItemCard(
                        sample = sample,
                        formatter = formatter
                    )
                }

                if (hasMore) {
                    Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                    Button(
                        onClick = onLoadMore,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("load_more_history_button")
                    ) {
                        Text(
                            text = "Load More Entries",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordItemCard(
    sample: TelemetrySample,
    formatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val timeStr = Instant.ofEpochMilli(sample.timestampEpochMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    val badgeType = if (sample.origin == DataOrigin.LiveHardware) {
        FarmVisionStatusType.Active
    } else {
        FarmVisionStatusType.DemoData
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_card_${sample.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sample.metric.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sample.metric.isRaw) {
                    Text(
                        text = "Uncalibrated • ${sample.calibrationState.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.US, "%.1f %s", sample.canonicalValue, sample.metric.canonicalUnit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                FarmVisionStatusBadge(status = badgeType)
            }
        }
    }
}
