package com.farmvision.agrirover.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Year

/**
 * Material 3 Custom Date Range Picker Dialog adhering to Section 7 & Defaults D6/D7.
 * Features:
 * - Selectable date range clamped to current system time (no future dates).
 * - Maximum span guidance (30-day retention limit).
 * - Guaranteed 48dp touch targets and test tags for accessibility & QA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeDialog(
    isOpen: Boolean,
    initialStartMillis: Long? = null,
    initialEndMillis: Long? = null,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val selectableDates = remember {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= Year.now().value
            }
        }
    }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis,
        selectableDates = selectableDates
    )

    val start = dateRangePickerState.selectedStartDateMillis
    val end = dateRangePickerState.selectedEndDateMillis
    val isValid = start != null && end != null && end >= start

    val maxSpanMillis = 30L * 24 * 60 * 60 * 1000L
    val isSpanExceeded = isValid && ((end!! - start!!) > maxSpanMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid && start != null && end != null) {
                        val now = System.currentTimeMillis()
                        val endOfDay = end + 86_399_999L // 23:59:59.999 UTC
                        val clampedEnd = minOf(endOfDay, now)
                        val finalStart = if (isSpanExceeded) maxOf(start, clampedEnd - maxSpanMillis) else start
                        onConfirm(finalStart, clampedEnd)
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                    .testTag("date_range_confirm_button")
                    .semantics { contentDescription = "Apply custom date range" }
            ) {
                Text(
                    text = "Apply",
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                    .testTag("date_range_dismiss_button")
                    .semantics { contentDescription = "Cancel custom date range selection" }
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.testTag("custom_date_range_dialog"),
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f, fill = false),
                title = {
                    Text(
                        text = "Select Telemetry Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
                    )
                },
                headline = {
                    Text(
                        text = "Max 30 days retention window",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                    )
                }
            )

            if (isSpanExceeded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Range exceeds 30-day retention; start will be clamped upon applying.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

