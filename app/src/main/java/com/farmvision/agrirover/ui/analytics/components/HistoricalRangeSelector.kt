package com.farmvision.agrirover.ui.analytics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.AnalyticsTimeRange

/**
 * Range selector for 24 Hours, 7 Days, and 30 Days rolling windows (Default D6).
 * Guaranteed 48dp minimum touch target and accessible selection semantics.
 */
@Composable
fun HistoricalRangeSelector(
    selectedRange: AnalyticsTimeRange,
    onRangeSelected: (AnalyticsTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
    ) {
        AnalyticsTimeRange.entries.forEach { range ->
            val isSelected = range == selectedRange

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onRangeSelected(range) }
                    )
                    .testTag("range_chip_${range.name}")
            ) {
                Text(
                    text = range.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
