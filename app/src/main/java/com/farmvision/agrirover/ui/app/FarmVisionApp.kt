package com.farmvision.agrirover.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.data.local.SettingsPreferencesManager
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.analytics.AnalyticsScreen
import com.farmvision.agrirover.ui.farm.MyFarmScreen
import com.farmvision.agrirover.ui.hardware.screens.HardwareCenterScreen
import com.farmvision.agrirover.ui.home.HomeScreen
import com.farmvision.agrirover.ui.rover.RoverScreen
import com.farmvision.agrirover.ui.settings.SettingsScreen
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * Top-level application destinations for the FarmVision AgriRover bottom navigation shell.
 * Defines canonical route identifiers, display labels, semantic icons, and stable test tags.
 */
enum class FarmVisionDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    Home(
        route = "home",
        label = "Home",
        icon = Icons.Rounded.Home,
        testTag = "nav_home"
    ),
    Rover(
        route = "rover",
        label = "Rover",
        icon = Icons.Rounded.Agriculture,
        testTag = "nav_rover"
    ),
    MyFarm(
        route = "my_farm",
        label = "My Farm",
        icon = Icons.Rounded.Grass,
        testTag = "nav_my_farm"
    ),
    Analytics(
        route = "analytics",
        label = "Analytics",
        icon = Icons.Rounded.BarChart,
        testTag = "nav_analytics"
    ),
    Settings(
        route = "settings",
        label = "Settings",
        icon = Icons.Rounded.Settings,
        testTag = "nav_settings"
    )
}

/**
 * Production-style App Shell for FarmVision AgriRover.
 * Hosts the Material 3 bottom navigation bar with 5 primary destinations:
 * Home, Rover, My Farm, Analytics, and Settings.
 * Single source of truth is maintained by [FarmViewModel].
 * Implements strict back-navigation policy and state retention across tab switches.
 */
@Composable
fun FarmVisionApp(
    modifier: Modifier = Modifier,
    viewModel: FarmViewModel? = null
) {
    val context = LocalContext.current
    val farmViewModel = viewModel ?: remember {
        val repo = MockFarmRepository()
        val prefs = SettingsPreferencesManager(context)
        FarmViewModel(repository = repo, preferencesManager = prefs)
    }

    var currentDestination by rememberSaveable { mutableStateOf(FarmVisionDestination.Home) }
    var isHardwareCenterOpen by rememberSaveable { mutableStateOf(false) }
    val isNotificationSheetOpen by farmViewModel.isNotificationSheetOpen.collectAsState()

    // --- Back-Navigation Policy ---
    // Priority 1: If a modal (notification sheet) is open, Back closes it first.
    BackHandler(enabled = isNotificationSheetOpen) {
        farmViewModel.setNotificationSheetOpen(false)
    }

    // Priority 2: If Hardware Center is open, Back closes it and returns to previous screen.
    BackHandler(enabled = isHardwareCenterOpen && !isNotificationSheetOpen) {
        isHardwareCenterOpen = false
    }

    // Priority 3: On a non-Home top-level tab (with no modal or Hardware Center open), Back returns to Home.
    BackHandler(enabled = currentDestination != FarmVisionDestination.Home && !isNotificationSheetOpen && !isHardwareCenterOpen) {
        currentDestination = FarmVisionDestination.Home
    }

    // Priority 4: On Home with nothing open, default Android Back proceeds (leaves app).

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isHardwareCenterOpen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    FarmVisionDestination.entries.forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            modifier = Modifier.testTag(destination.testTag),
                            selected = isSelected,
                            onClick = {
                                if (currentDestination != destination) {
                                    currentDestination = destination
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isHardwareCenterOpen) {
                HardwareCenterScreen(
                    onNavigateBack = { isHardwareCenterOpen = false }
                )
            } else {
                Crossfade(
                    targetState = currentDestination,
                    label = "DestinationCrossfade"
                ) { target ->
                    when (target) {
                        FarmVisionDestination.Home -> HomeScreen(
                            viewModel = farmViewModel,
                            onNavigateToRover = { currentDestination = FarmVisionDestination.Rover },
                            onNavigateToFarm = { currentDestination = FarmVisionDestination.MyFarm },
                            onNavigateToHardwareCenter = { isHardwareCenterOpen = true }
                        )
                        FarmVisionDestination.Rover -> RoverScreen(
                            viewModel = farmViewModel,
                            onNavigateToHardwareCenter = { isHardwareCenterOpen = true }
                        )
                        FarmVisionDestination.MyFarm -> MyFarmScreen(
                            viewModel = farmViewModel
                        )
                        FarmVisionDestination.Analytics -> AnalyticsScreen(
                            viewModel = farmViewModel
                        )
                        FarmVisionDestination.Settings -> SettingsScreen(
                            viewModel = farmViewModel,
                            onNavigateToHardwareCenter = { isHardwareCenterOpen = true }
                        )
                    }
                }
            }
        }
    }
}
