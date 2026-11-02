package com.farmvision.agrirover.ui.app

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.data.alert.repository.JsonFileAlertRepository
import com.farmvision.agrirover.data.alert.repository.PreferencesAlertSettingsRepository
import com.farmvision.agrirover.data.assistant.provider.BackendAssistantProvider
import com.farmvision.agrirover.data.assistant.repository.JsonFileAssistantRepository
import com.farmvision.agrirover.data.device.repository.ProductionDeviceCommunicationRepository
import com.farmvision.agrirover.data.local.SettingsPreferencesManager
import com.farmvision.agrirover.data.repository.JsonFileTelemetryHistoryRepository
import com.farmvision.agrirover.data.repository.MockFarmRepository
import com.farmvision.agrirover.ui.alert.AlertViewModel
import com.farmvision.agrirover.ui.alert.notification.AlertNotificationManager
import com.farmvision.agrirover.ui.alert.screens.AlertCenterScreen
import com.farmvision.agrirover.ui.alert.screens.AlertDetailsScreen
import com.farmvision.agrirover.ui.alert.screens.AlertHistoryScreen
import com.farmvision.agrirover.ui.alert.screens.AlertSettingsScreen
import com.farmvision.agrirover.ui.analytics.AnalyticsScreen
import com.farmvision.agrirover.ui.assistant.AssistantViewModel
import com.farmvision.agrirover.ui.assistant.screens.AssistantChatScreen
import com.farmvision.agrirover.ui.device.DeviceManagementViewModel
import com.farmvision.agrirover.ui.device.screens.ConnectionDiagnosticsScreen
import com.farmvision.agrirover.ui.device.screens.ConnectionSetupScreen
import com.farmvision.agrirover.ui.device.screens.DeviceConfigurationScreen
import com.farmvision.agrirover.ui.device.screens.DeviceDetailsScreen
import com.farmvision.agrirover.ui.device.screens.DeviceManagementScreen
import com.farmvision.agrirover.ui.farm.MyFarmScreen
import com.farmvision.agrirover.ui.hardware.screens.HardwareCenterScreen
import com.farmvision.agrirover.ui.home.HomeScreen
import com.farmvision.agrirover.ui.rover.RoverScreen
import com.farmvision.agrirover.ui.settings.SettingsScreen
import com.farmvision.agrirover.ui.settings.SettingsViewModel
import com.farmvision.agrirover.ui.settings.screens.AboutFarmVisionScreen
import com.farmvision.agrirover.ui.settings.screens.AiAssistantSettingsScreen
import com.farmvision.agrirover.ui.settings.screens.DataManagementScreen
import com.farmvision.agrirover.ui.settings.screens.GeneralSettingsScreen
import com.farmvision.agrirover.ui.settings.screens.HelpSupportScreen
import com.farmvision.agrirover.ui.settings.screens.NotificationSettingsScreen
import com.farmvision.agrirover.ui.settings.screens.PrivacySecuritySettingsScreen
import com.farmvision.agrirover.ui.settings.screens.RoverDeviceSettingsScreen
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel
import java.io.File

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
 * Integrates Phase 13 Settings & Configuration Center with 8 specialized sub-modules.
 */
@Composable
fun FarmVisionApp(
    modifier: Modifier = Modifier,
    viewModel: FarmViewModel? = null,
    intent: Intent? = null
) {
    val context = LocalContext.current
    val historyRepo = remember { JsonFileTelemetryHistoryRepository(context) }
    val assistantRepo = remember {
        val storageFile = File(context.filesDir, "assistant_conversations.json")
        JsonFileAssistantRepository(storageFile)
    }
    val alertRepo = remember { JsonFileAlertRepository(context) }
    val alertSettingsRepo = remember { PreferencesAlertSettingsRepository(context) }
    val prefs = remember { SettingsPreferencesManager(context) }

    val farmViewModel = viewModel ?: remember {
        val repo = MockFarmRepository(telemetryHistory = historyRepo)
        val notifManager = AlertNotificationManager(context)
        FarmViewModel(
            repository = repo,
            preferencesManager = prefs,
            alertRepository = alertRepo,
            alertSettingsRepository = alertSettingsRepo,
            alertNotificationManager = notifManager
        )
    }

    val alertViewModel = remember(farmViewModel) {
        AlertViewModel(
            alertRepository = alertRepo,
            settingsRepository = alertSettingsRepo
        )
    }

    val settingsViewModel = remember {
        SettingsViewModel(
            preferencesManager = prefs,
            assistantRepository = assistantRepo,
            alertSettingsRepository = alertSettingsRepo,
            telemetryHistoryRepository = historyRepo
        )
    }

    var currentDestination by rememberSaveable { mutableStateOf(FarmVisionDestination.Home) }
    var isHardwareCenterOpen by rememberSaveable { mutableStateOf(false) }
    var isAlertCenterOpen by rememberSaveable { mutableStateOf(false) }
    var selectedAlertDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAlertSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isAlertHistoryOpen by rememberSaveable { mutableStateOf(false) }

    var isDeviceManagementOpen by rememberSaveable { mutableStateOf(false) }
    var isDeviceDetailsOpen by rememberSaveable { mutableStateOf(false) }
    var isConnectionSetupOpen by rememberSaveable { mutableStateOf(false) }
    var isDeviceConfigOpen by rememberSaveable { mutableStateOf(false) }
    var isConnectionDiagnosticsOpen by rememberSaveable { mutableStateOf(false) }
    var isAssistantOpen by rememberSaveable { mutableStateOf(false) }

    // Phase 13: Settings Sub-screens
    var isGeneralSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isRoverSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isAiSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isPrivacySettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isNotificationSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var isDataManagementOpen by rememberSaveable { mutableStateOf(false) }
    var isHelpSupportOpen by rememberSaveable { mutableStateOf(false) }
    var isAboutOpen by rememberSaveable { mutableStateOf(false) }

    val deviceViewModel = remember(farmViewModel) {
        val repo = (farmViewModel.deviceRegistry as? ProductionDeviceCommunicationRepository)
            ?: ProductionDeviceCommunicationRepository()
        DeviceManagementViewModel(communicationRepository = repo)
    }

    val assistantViewModel = remember(farmViewModel) {
        val provider = BackendAssistantProvider()
        AssistantViewModel(
            repository = assistantRepo,
            assistantProvider = provider,
            telemetryProvider = { farmViewModel.telemetry.value },
            isStaleProvider = { false }
        )
    }

    val isNotificationSheetOpen by farmViewModel.isNotificationSheetOpen.collectAsState()

    // Handle deep-link intent from Android Notification shade
    LaunchedEffect(intent) {
        val destination = intent?.getStringExtra(AlertNotificationManager.EXTRA_DESTINATION)
        if (destination == AlertNotificationManager.DESTINATION_ALERTS) {
            isAlertCenterOpen = true
            val alertId = intent.getStringExtra(AlertNotificationManager.EXTRA_ALERT_ID)
            if (!alertId.isNullOrBlank()) {
                alertViewModel.selectAlertById(alertId)
                selectedAlertDetailId = alertId
            }
        }
    }

    // --- Back-Navigation Policy ---
    BackHandler(enabled = isNotificationSheetOpen) {
        farmViewModel.setNotificationSheetOpen(false)
    }

    BackHandler(enabled = isAssistantOpen && !isNotificationSheetOpen) {
        isAssistantOpen = false
    }

    // Settings Sub-screens BackHandlers
    BackHandler(enabled = isGeneralSettingsOpen) { isGeneralSettingsOpen = false }
    BackHandler(enabled = isRoverSettingsOpen) { isRoverSettingsOpen = false }
    BackHandler(enabled = isAiSettingsOpen) { isAiSettingsOpen = false }
    BackHandler(enabled = isPrivacySettingsOpen) { isPrivacySettingsOpen = false }
    BackHandler(enabled = isNotificationSettingsOpen) { isNotificationSettingsOpen = false }
    BackHandler(enabled = isDataManagementOpen) { isDataManagementOpen = false }
    BackHandler(enabled = isHelpSupportOpen) { isHelpSupportOpen = false }
    BackHandler(enabled = isAboutOpen) { isAboutOpen = false }

    BackHandler(enabled = selectedAlertDetailId != null && !isNotificationSheetOpen) {
        selectedAlertDetailId = null
        alertViewModel.selectAlert(null)
    }

    BackHandler(enabled = isAlertSettingsOpen && selectedAlertDetailId == null && !isNotificationSheetOpen) {
        isAlertSettingsOpen = false
    }

    BackHandler(enabled = isDeviceDetailsOpen) { isDeviceDetailsOpen = false }
    BackHandler(enabled = isConnectionSetupOpen) { isConnectionSetupOpen = false }
    BackHandler(enabled = isDeviceConfigOpen) { isDeviceConfigOpen = false }
    BackHandler(enabled = isConnectionDiagnosticsOpen) { isConnectionDiagnosticsOpen = false }
    BackHandler(enabled = isDeviceManagementOpen && !isDeviceDetailsOpen && !isConnectionSetupOpen && !isDeviceConfigOpen && !isConnectionDiagnosticsOpen) {
        isDeviceManagementOpen = false
    }

    BackHandler(enabled = isAlertHistoryOpen && selectedAlertDetailId == null && !isNotificationSheetOpen) {
        isAlertHistoryOpen = false
    }

    BackHandler(enabled = isAlertCenterOpen && !isAlertSettingsOpen && !isAlertHistoryOpen && selectedAlertDetailId == null && !isNotificationSheetOpen) {
        isAlertCenterOpen = false
    }

    BackHandler(enabled = isHardwareCenterOpen && !isAlertCenterOpen && !isNotificationSheetOpen) {
        isHardwareCenterOpen = false
    }

    val isAnySettingsSubscreenOpen = isGeneralSettingsOpen || isRoverSettingsOpen || isAiSettingsOpen ||
        isPrivacySettingsOpen || isNotificationSettingsOpen || isDataManagementOpen || isHelpSupportOpen || isAboutOpen

    BackHandler(
        enabled = currentDestination != FarmVisionDestination.Home &&
            !isNotificationSheetOpen &&
            !isHardwareCenterOpen &&
            !isAlertCenterOpen &&
            selectedAlertDetailId == null &&
            !isAlertSettingsOpen &&
            !isAlertHistoryOpen &&
            !isDeviceManagementOpen &&
            !isAnySettingsSubscreenOpen
    ) {
        currentDestination = FarmVisionDestination.Home
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isRoverControllerActive = currentDestination == FarmVisionDestination.Rover && !isHardwareCenterOpen
    val isAlertScreenActive = isAlertCenterOpen || isAlertSettingsOpen || isAlertHistoryOpen || selectedAlertDetailId != null
    val isDeviceScreenActive = isDeviceManagementOpen || isDeviceDetailsOpen || isConnectionSetupOpen || isDeviceConfigOpen || isConnectionDiagnosticsOpen
    val shouldHideBottomBar = isHardwareCenterOpen || isAlertScreenActive || isDeviceScreenActive || isAnySettingsSubscreenOpen || (isRoverControllerActive && isLandscape)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!shouldHideBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
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
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
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
                .then(
                    if ((isRoverControllerActive && isLandscape) || isAlertScreenActive || isAnySettingsSubscreenOpen) Modifier
                    else Modifier.padding(innerPadding)
                )
        ) {
            when {
                // Settings Sub-screens
                isGeneralSettingsOpen -> {
                    GeneralSettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { isGeneralSettingsOpen = false }
                    )
                }
                isRoverSettingsOpen -> {
                    RoverDeviceSettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { isRoverSettingsOpen = false }
                    )
                }
                isAiSettingsOpen -> {
                    AiAssistantSettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { isAiSettingsOpen = false },
                        onNavigateToPrivacy = { isPrivacySettingsOpen = true }
                    )
                }
                isPrivacySettingsOpen -> {
                    PrivacySecuritySettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { isPrivacySettingsOpen = false }
                    )
                }
                isNotificationSettingsOpen -> {
                    NotificationSettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { isNotificationSettingsOpen = false },
                        onNavigateToAlertSettings = { isAlertSettingsOpen = true }
                    )
                }
                isDataManagementOpen -> {
                    DataManagementScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { isDataManagementOpen = false }
                    )
                }
                isHelpSupportOpen -> {
                    HelpSupportScreen(
                        onNavigateBack = { isHelpSupportOpen = false }
                    )
                }
                isAboutOpen -> {
                    AboutFarmVisionScreen(
                        onNavigateBack = { isAboutOpen = false }
                    )
                }

                // Existing Alert & Device Overlays
                selectedAlertDetailId != null -> {
                    AlertDetailsScreen(
                        viewModel = alertViewModel,
                        onNavigateBack = {
                            selectedAlertDetailId = null
                            alertViewModel.selectAlert(null)
                        },
                        onNavigateToAnalytics = {
                            selectedAlertDetailId = null
                            alertViewModel.selectAlert(null)
                            isAlertCenterOpen = false
                            currentDestination = FarmVisionDestination.Analytics
                        },
                        onExplainAlertWithAi = { alert ->
                            assistantViewModel.setAlertForDiagnosis(alert)
                            isAssistantOpen = true
                        }
                    )
                }
                isAlertSettingsOpen -> {
                    AlertSettingsScreen(
                        viewModel = alertViewModel,
                        onNavigateBack = { isAlertSettingsOpen = false }
                    )
                }
                isAlertHistoryOpen -> {
                    AlertHistoryScreen(
                        viewModel = alertViewModel,
                        onNavigateBack = { isAlertHistoryOpen = false },
                        onNavigateToDetails = { alertId ->
                            alertViewModel.selectAlertById(alertId)
                            selectedAlertDetailId = alertId
                        }
                    )
                }
                isAlertCenterOpen -> {
                    AlertCenterScreen(
                        viewModel = alertViewModel,
                        onNavigateBack = { isAlertCenterOpen = false },
                        onNavigateToDetails = { alertId ->
                            alertViewModel.selectAlertById(alertId)
                            selectedAlertDetailId = alertId
                        },
                        onNavigateToSettings = { isAlertSettingsOpen = true }
                    )
                }
                isDeviceDetailsOpen -> {
                    DeviceDetailsScreen(
                        viewModel = deviceViewModel,
                        onNavigateBack = { isDeviceDetailsOpen = false }
                    )
                }
                isConnectionSetupOpen -> {
                    ConnectionSetupScreen(
                        viewModel = deviceViewModel,
                        onNavigateBack = { isConnectionSetupOpen = false }
                    )
                }
                isDeviceConfigOpen -> {
                    DeviceConfigurationScreen(
                        viewModel = deviceViewModel,
                        onNavigateBack = { isDeviceConfigOpen = false }
                    )
                }
                isConnectionDiagnosticsOpen -> {
                    ConnectionDiagnosticsScreen(
                        viewModel = deviceViewModel,
                        onNavigateBack = { isConnectionDiagnosticsOpen = false }
                    )
                }
                isDeviceManagementOpen -> {
                    DeviceManagementScreen(
                        viewModel = deviceViewModel,
                        onNavigateBack = { isDeviceManagementOpen = false },
                        onNavigateToDetails = { isDeviceDetailsOpen = true },
                        onNavigateToSetup = { isConnectionSetupOpen = true },
                        onNavigateToConfig = { isDeviceConfigOpen = true },
                        onNavigateToDiagnostics = { isConnectionDiagnosticsOpen = true }
                    )
                }
                isAssistantOpen -> {
                    AssistantChatScreen(
                        viewModel = assistantViewModel,
                        onNavigateBack = { isAssistantOpen = false }
                    )
                }
                isHardwareCenterOpen -> {
                    HardwareCenterScreen(
                        onNavigateBack = { isHardwareCenterOpen = false },
                        telemetryHistoryRepository = farmViewModel.telemetryHistory
                    )
                }
                else -> {
                    Crossfade(
                        targetState = currentDestination,
                        label = "DestinationCrossfade"
                    ) { target ->
                        when (target) {
                            FarmVisionDestination.Home -> HomeScreen(
                                viewModel = farmViewModel,
                                onNavigateToRover = { currentDestination = FarmVisionDestination.Rover },
                                onNavigateToFarm = { currentDestination = FarmVisionDestination.MyFarm },
                                onNavigateToHardwareCenter = { isHardwareCenterOpen = true },
                                onNavigateToAlertCenter = { isAlertCenterOpen = true },
                                onNavigateToDeviceManagement = { isDeviceManagementOpen = true },
                                onNavigateToAssistant = { isAssistantOpen = true }
                            )
                            FarmVisionDestination.Rover -> RoverScreen(
                                viewModel = farmViewModel,
                                onNavigateToHardwareCenter = { isHardwareCenterOpen = true },
                                onExit = { currentDestination = FarmVisionDestination.Home }
                            )
                            FarmVisionDestination.MyFarm -> MyFarmScreen(
                                viewModel = farmViewModel
                            )
                            FarmVisionDestination.Analytics -> AnalyticsScreen(
                                viewModel = farmViewModel,
                                onAnalyzeTrendsWithAi = { metrics ->
                                    assistantViewModel.setHistoricalAnalyticsContext(metrics)
                                    isAssistantOpen = true
                                }
                            )
                            FarmVisionDestination.Settings -> SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateToGeneral = { isGeneralSettingsOpen = true },
                                onNavigateToRoverDevice = { isRoverSettingsOpen = true },
                                onNavigateToAiAssistant = { isAiSettingsOpen = true },
                                onNavigateToPrivacy = { isPrivacySettingsOpen = true },
                                onNavigateToNotifications = { isNotificationSettingsOpen = true },
                                onNavigateToDataManagement = { isDataManagementOpen = true },
                                onNavigateToHelpSupport = { isHelpSupportOpen = true },
                                onNavigateToAbout = { isAboutOpen = true },
                                onNavigateToHardwareCenter = { isHardwareCenterOpen = true },
                                onNavigateToAlertCenter = { isAlertCenterOpen = true },
                                onNavigateToDeviceManagement = { isDeviceManagementOpen = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
