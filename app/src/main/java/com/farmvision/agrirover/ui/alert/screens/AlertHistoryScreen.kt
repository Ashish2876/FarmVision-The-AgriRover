package com.farmvision.agrirover.ui.alert.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.alert.AlertViewModel
import com.farmvision.agrirover.ui.alert.components.AlertFilterPills
import com.farmvision.agrirover.ui.alert.components.AlertItemCard
import com.farmvision.agrirover.ui.alert.components.EmptyAlertsPlaceholder
import com.farmvision.agrirover.ui.theme.StatusAlert

/**
 * Dedicated historical audit screen displaying previous, acknowledged, and resolved alerts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistoryScreen(
    viewModel: AlertViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedAlerts by viewModel.filteredResolvedAlerts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSeverity by viewModel.selectedSeverityFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alert History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (resolvedAlerts.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Clear History",
                                tint = StatusAlert
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = FarmVisionSpacing.medium)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search resolved alerts...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            AlertFilterPills(
                selectedSeverity = selectedSeverity,
                onSelectSeverity = { viewModel.selectedSeverityFilter.value = it },
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.selectedCategoryFilter.value = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (resolvedAlerts.isEmpty()) {
                EmptyAlertsPlaceholder(
                    title = "No History Records",
                    subtitle = "There are no resolved alerts in persistent storage matching your filter."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(resolvedAlerts, key = { it.id }) { alert ->
                        AlertItemCard(
                            alert = alert,
                            onClick = {
                                viewModel.selectAlert(alert)
                                onNavigateToDetails(alert.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Alert History?") },
            text = { Text("This will permanently delete all resolved alert records from persistent storage. Active alerts will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearResolvedHistory()
                    }
                ) {
                    Text("Clear All", color = StatusAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

