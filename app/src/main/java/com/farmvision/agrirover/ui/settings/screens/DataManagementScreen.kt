package com.farmvision.agrirover.ui.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.settings.SettingsViewModel
import com.farmvision.agrirover.ui.settings.components.SettingsConfirmationDialog
import com.farmvision.agrirover.ui.settings.components.SettingsInfoRow
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 6 — Data Management Screen.
 * Storage utilization auditing, AI conversation clearing, telemetry log pruning, and cache management.
 */
@Composable
fun DataManagementScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.storageStats.collectAsState()
    val scrollState = rememberScrollState()

    var showClearConversationsDialog by remember { mutableStateOf(false) }
    var showClearTelemetryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStorageStats(context)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            SettingsTopBar(
                title = "Data Management",
                subtitle = "Storage Audit • History • Cache",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Storage Audit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FarmVisionSectionHeader(
                        title = "Local Storage Footprint",
                        description = "Private application files on internal storage"
                    )
                }
                IconButton(onClick = { viewModel.refreshStorageStats(context) }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh storage statistics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                val convKb = (stats.conversationFileSizeBytes / 1024.0).let { "%.1f KB".format(it) }
                val teleKb = (stats.telemetryHistorySizeBytes / 1024.0).let { "%.1f KB".format(it) }
                val cacheKb = (stats.cacheSizeBytes / 1024.0).let { "%.1f KB".format(it) }

                SettingsInfoRow(label = "AI Conversation Records", value = "${stats.conversationCount} ($convKb)")
                SettingsInfoRow(label = "Telemetry History Records", value = "${stats.telemetryHistoryCount} ($teleKb)")
                SettingsInfoRow(label = "Temporary Cache Files", value = cacheKb)
                SettingsInfoRow(label = "Storage Location", value = "Internal Private App Sandbox")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // AI Conversation Management
            FarmVisionSectionHeader(
                title = "AI Conversation History",
                description = "Manage saved agronomic dialogues and consultations"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "FarmVision AI stores conversation messages locally in private storage to preserve advisory context across sessions. Deleting conversations does not alter your crop profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                OutlinedButton(
                    onClick = { showClearConversationsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All AI Conversations")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Historical Telemetry Management
            FarmVisionSectionHeader(
                title = "Historical Telemetry Logs",
                description = "Manage sensor time-series records"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Telemetry samples are automatically pruned according to the 30-day bounded retention policy. You can manually purge historical logs if needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                OutlinedButton(
                    onClick = { showClearTelemetryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Purge Telemetry History")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Cache Cleaning
            FarmVisionSectionHeader(
                title = "Application Cache",
                description = "Clean temporary files and image render buffers"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Clearing cache safely removes temporary chart render buffers and temporary files without deleting any preferences or saved data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                Button(
                    onClick = { showClearCacheDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clean Temporary Cache")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }

    if (showClearConversationsDialog) {
        SettingsConfirmationDialog(
            title = "Clear AI Conversations?",
            message = "This will permanently delete all stored AI chat messages and advice from your device. Your crop profile and settings will not be affected.",
            confirmText = "Delete History",
            isDestructive = true,
            onConfirm = { viewModel.clearAiConversations(context) },
            onDismiss = { showClearConversationsDialog = false }
        )
    }

    if (showClearTelemetryDialog) {
        SettingsConfirmationDialog(
            title = "Purge Telemetry History?",
            message = "This will permanently delete all recorded sensor time-series records. 24-hour and 7-day trend charts will reset until new data is recorded.",
            confirmText = "Purge Records",
            isDestructive = true,
            onConfirm = { viewModel.clearTelemetryHistory(context) },
            onDismiss = { showClearTelemetryDialog = false }
        )
    }

    if (showClearCacheDialog) {
        SettingsConfirmationDialog(
            title = "Clean Application Cache?",
            message = "This safely clears temporary cache files without touching your settings or farm data.",
            confirmText = "Clean Cache",
            isDestructive = false,
            onConfirm = { viewModel.clearAppCache(context) },
            onDismiss = { showClearCacheDialog = false }
        )
    }
}

