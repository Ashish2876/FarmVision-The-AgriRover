package com.farmvision.agrirover.ui.alert.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.alert.model.AlertCategory
import com.farmvision.agrirover.domain.alert.model.AlertItem
import com.farmvision.agrirover.domain.alert.model.AlertSeverity
import com.farmvision.agrirover.domain.alert.model.AlertStatus
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.StatusDemo
import com.farmvision.agrirover.ui.theme.StatusInfo
import com.farmvision.agrirover.ui.theme.StatusOnline
import com.farmvision.agrirover.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visual badge conveying alert severity with distinct geometry, icon, and text.
 * Strictly adheres to accessibility rules: never conveys severity through color alone.
 */
@Composable
fun AlertSeverityBadge(
    severity: AlertSeverity,
    modifier: Modifier = Modifier
) {
    val (icon, tintColor, label) = when (severity) {
        AlertSeverity.Critical -> Triple(Icons.Rounded.Error, StatusAlert, "CRITICAL")
        AlertSeverity.Warning -> Triple(Icons.Rounded.Warning, StatusWarning, "WARNING")
        AlertSeverity.Information -> Triple(Icons.Rounded.Info, StatusInfo, "INFO")
    }

    Surface(
        modifier = modifier.semantics { contentDescription = "Severity: $label" },
        shape = RoundedCornerShape(6.dp),
        color = tintColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tintColor,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Data provenance badge stamping whether an alert originated from Live Hardware or Simulated Demo.
 */
@Composable
fun AlertProvenanceBadge(
    isSimulated: Boolean,
    modifier: Modifier = Modifier
) {
    val text = if (isSimulated) "SIMULATED (DEMO)" else "LIVE HARDWARE"
    val color = if (isSimulated) StatusDemo else StatusOnline

    Surface(
        modifier = modifier.semantics { contentDescription = "Data Origin: $text" },
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Interactive card displaying an individual alert item in Alert Centre and History lists.
 */
@Composable
fun AlertItemCard(
    alert: AlertItem,
    onClick: () -> Unit,
    onAcknowledge: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUnread = !alert.isRead
    val timeFormatted = formatTime(alert.lastUpdatedTimestampEpochMillis)

    FarmVisionCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(FarmVisionSpacing.medium)) {
            // Header Row: Badges, Origin, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ParrotGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    AlertSeverityBadge(severity = alert.severity)
                    Spacer(modifier = Modifier.width(8.dp))
                    AlertProvenanceBadge(isSimulated = alert.isSimulated)
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Occurrence Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (alert.occurrenceCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "×${alert.occurrenceCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message Body
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Sensor Source & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source: ${alert.sourceSensor} • ${alert.category.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (alert.status == AlertStatus.Active && onAcknowledge != null) {
                        OutlinedButton(
                            onClick = onAcknowledge,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = DarkGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Acknowledge",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Summary KPI Row for active, critical, warning, and resolved counts.
 */
@Composable
fun AlertSummaryHeader(
    totalActive: Int,
    criticalCount: Int,
    warningCount: Int,
    resolvedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryKpiBox(
            label = "Active",
            count = totalActive,
            color = if (totalActive > 0) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SummaryKpiBox(
            label = "Critical",
            count = criticalCount,
            color = if (criticalCount > 0) StatusAlert else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SummaryKpiBox(
            label = "Warning",
            count = warningCount,
            color = if (warningCount > 0) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SummaryKpiBox(
            label = "Resolved",
            count = resolvedCount,
            color = StatusInfo,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryKpiBox(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Horizontal filter chips for severity and category filtering.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertFilterPills(
    selectedSeverity: AlertSeverity?,
    onSelectSeverity: (AlertSeverity?) -> Unit,
    selectedCategory: AlertCategory?,
    onSelectCategory: (AlertCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = selectedSeverity == null && selectedCategory == null,
            onClick = {
                onSelectSeverity(null)
                onSelectCategory(null)
            },
            label = { Text("All Alerts") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ParrotGreen.copy(alpha = 0.25f),
                selectedLabelColor = DarkGreen
            )
        )

        FilterChip(
            selected = selectedSeverity == AlertSeverity.Critical,
            onClick = { onSelectSeverity(if (selectedSeverity == AlertSeverity.Critical) null else AlertSeverity.Critical) },
            label = { Text("Critical") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StatusAlert.copy(alpha = 0.2f),
                selectedLabelColor = StatusAlert
            )
        )

        FilterChip(
            selected = selectedSeverity == AlertSeverity.Warning,
            onClick = { onSelectSeverity(if (selectedSeverity == AlertSeverity.Warning) null else AlertSeverity.Warning) },
            label = { Text("Warning") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StatusWarning.copy(alpha = 0.2f),
                selectedLabelColor = StatusWarning
            )
        )

        AlertCategory.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(if (selectedCategory == category) null else category) },
                label = { Text(category.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ParrotGreen.copy(alpha = 0.2f),
                    selectedLabelColor = DarkGreen
                )
            )
        }
    }
}

/**
 * Placeholder displayed when no alerts match active filters or when all alerts are resolved.
 */
@Composable
fun EmptyAlertsPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.NotificationsNone,
            contentDescription = null,
            tint = DarkGreen.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(epochMillis: Long): String {
    val date = Date(epochMillis)
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    return formatter.format(date)
}

