package com.farmvision.agrirover.ui.alert.screens

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.alert.AlertViewModel
import com.farmvision.agrirover.ui.alert.components.AlertFilterPills
import com.farmvision.agrirover.ui.alert.components.AlertItemCard
import com.farmvision.agrirover.ui.alert.components.AlertSummaryHeader
import com.farmvision.agrirover.ui.alert.components.EmptyAlertsPlaceholder
import com.farmvision.agrirover.ui.theme.DarkGreen

/**
 * Master Alert Centre screen providing unified alert monitoring, filtering, and resolution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCenterScreen(
    viewModel: AlertViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAlerts by viewModel.filteredActiveAlerts.collectAsState()
    val resolvedAlerts by viewModel.filteredResolvedAlerts.collectAsState()
    val summary by viewModel.summaryState.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSeverity by viewModel.selectedSeverityFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Alert Centre",
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Alert Settings"
                        )
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

            // Summary Header Cards
            AlertSummaryHeader(
                totalActive = summary.totalActive,
                criticalCount = summary.criticalCount,
                warningCount = summary.warningCount,
                resolvedCount = summary.resolvedCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search alerts, sensors, rules...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
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

            // Tabs: Active vs Resolved
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectedTab.value = 0 },
                    text = {
                        Text(
                            text = "Active (${summary.totalActive})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectedTab.value = 1 },
                    text = {
                        Text(
                            text = "Resolved (${summary.resolvedCount})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) DarkGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alert List Content
            val displayList = if (selectedTab == 0) activeAlerts else resolvedAlerts

            if (displayList.isEmpty()) {
                val placeholderTitle = if (selectedTab == 0) "No Active Alerts" else "No Resolved History"
                val placeholderSub = if (selectedTab == 0) {
                    "All telemetry metrics are currently within nominal operating thresholds."
                } else {
                    "No resolved historical alerts match your active search or filter selection."
                }
                EmptyAlertsPlaceholder(
                    title = placeholderTitle,
                    subtitle = placeholderSub
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList, key = { it.id }) { alert ->
                        AlertItemCard(
                            alert = alert,
                            onClick = {
                                viewModel.selectAlert(alert)
                                onNavigateToDetails(alert.id)
                            },
                            onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

