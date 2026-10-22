package com.farmvision.agrirover.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCardFooter
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCardHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionEmptyState
import com.farmvision.agrirover.core.designsystem.component.FarmVisionErrorState
import com.farmvision.agrirover.core.designsystem.component.FarmVisionIconButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionIconContainer
import com.farmvision.agrirover.core.designsystem.component.FarmVisionLoadingState
import com.farmvision.agrirover.core.designsystem.component.FarmVisionMetricCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionMetricType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionOutlinedButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSecondaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.component.FarmVisionTextButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionTrendDirection
import com.farmvision.agrirover.core.designsystem.component.FarmVisionUnavailableState
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing

/**
 * Phase 2 Component Showcase Screen.
 * Demonstrates the actual reusable component library in live action with dynamic state updates.
 * Excluded from release builds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebugShowcaseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var isButtonLoading by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }
    var selectedStateTabIndex by remember { mutableIntStateOf(0) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FarmVisionIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to Entry Screen",
                    onClick = onBack
                )

                Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

                Column {
                    Text(
                        text = "Phase 2 Component Showcase",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Development Showcase • All Telemetry Simulated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Mandatory Demo Banner
            FarmVisionDemoBanner(
                message = "DEVELOPMENT SHOWCASE: All components below display simulated DEMO DATA. No physical rover or sensors are connected."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // ==========================================
            // 1. BUTTONS & INTERACTIVE TARGETS
            // ==========================================
            FarmVisionSectionHeader(
                title = "1. Buttons & Touch Targets",
                description = "Enforced >= 48dp minimum touch target for outdoor accessibility"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                FarmVisionPrimaryButton(
                    text = "Primary Filled Button",
                    onClick = { },
                    leadingIcon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )

                FarmVisionSecondaryButton(
                    text = if (isButtonLoading) "Processing..." else "Tonal Button (Tap to toggle loading)",
                    onClick = { isButtonLoading = !isButtonLoading },
                    isLoading = isButtonLoading,
                    leadingIcon = Icons.Rounded.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
                ) {
                    FarmVisionOutlinedButton(
                        text = "Outlined Button",
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )

                    FarmVisionTextButton(
                        text = "Text Action",
                        onClick = { },
                        leadingIcon = Icons.Rounded.Eco
                    )

                    FarmVisionIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "Simulated Refresh",
                        onClick = { }
                    )
                }

                FarmVisionPrimaryButton(
                    text = "Disabled Primary Button",
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))
            FarmVisionDivider()
            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // ==========================================
            // 2. MODULAR CARDS
            // ==========================================
            FarmVisionSectionHeader(
                title = "2. Modular Cards & Surfaces",
                description = "Base surface composed with standardized header and footer slots"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            FarmVisionCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                FarmVisionCardHeader(
                    title = "Rootzone Moisture Sensor",
                    supportingText = "I2C Bus Slot #3 • Depth: 15cm",
                    leadingIcon = Icons.Rounded.WaterDrop,
                    trailingAction = {
                        FarmVisionStatusBadge(status = FarmVisionStatusType.DemoData)
                    }
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                Text(
                    text = "Soil capacitance probes report high water retention in Sector B. Optimal hydration levels maintained across all 4 sampling probes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                FarmVisionCardFooter(
                    statusContent = {
                        FarmVisionStatusBadge(
                            status = FarmVisionStatusType.Active,
                            customLabel = "SAMPLING (10 Hz)"
                        )
                    },
                    actionContent = {
                        FarmVisionTextButton(
                            text = "Calibrate",
                            onClick = { }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))
            FarmVisionDivider()
            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // ==========================================
            // 3. SEMANTIC STATUS BADGES
            // ==========================================
            FarmVisionSectionHeader(
                title = "3. Semantic Status Indicators",
                description = "All 8 states pair distinct geometric icons with text (never color dot alone)"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                FarmVisionStatusType.entries.forEach { statusType ->
                    FarmVisionStatusBadge(status = statusType)
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))
            FarmVisionDivider()
            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // ==========================================
            // 4. SENSOR METRIC CARDS
            // ==========================================
            FarmVisionSectionHeader(
                title = "4. Sensor Metrics & Tabular Figures",
                description = "Monospace tabular numbers prevent horizontal layout jitter during updates"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                FarmVisionMetricCard(
                    label = FarmVisionMetricType.SoilMoisture.defaultLabel,
                    value = "41.6",
                    unit = FarmVisionMetricType.SoilMoisture.defaultUnit,
                    icon = FarmVisionMetricType.SoilMoisture.icon,
                    trend = FarmVisionTrendDirection.Rising,
                    trendLabel = "+1.4% / 1h",
                    isSampleData = true
                )

                FarmVisionMetricCard(
                    label = FarmVisionMetricType.Temperature.defaultLabel,
                    value = "26.4",
                    unit = FarmVisionMetricType.Temperature.defaultUnit,
                    icon = FarmVisionMetricType.Temperature.icon,
                    trend = FarmVisionTrendDirection.Stable,
                    trendLabel = "Nominal",
                    isSampleData = true
                )

                FarmVisionMetricCard(
                    label = FarmVisionMetricType.AirHumidity.defaultLabel,
                    value = "64.8",
                    unit = FarmVisionMetricType.AirHumidity.defaultUnit,
                    icon = FarmVisionMetricType.AirHumidity.icon,
                    trend = FarmVisionTrendDirection.Falling,
                    trendLabel = "-2.1% / 1h",
                    isSampleData = true
                )

                FarmVisionMetricCard(
                    label = FarmVisionMetricType.LightIntensity.defaultLabel,
                    value = "48,200",
                    unit = FarmVisionMetricType.LightIntensity.defaultUnit,
                    icon = FarmVisionMetricType.LightIntensity.icon,
                    trend = FarmVisionTrendDirection.Rising,
                    trendLabel = "Peak Sunlight",
                    isSampleData = true
                )

                FarmVisionMetricCard(
                    label = FarmVisionMetricType.BatteryPercentage.defaultLabel,
                    value = "88",
                    unit = FarmVisionMetricType.BatteryPercentage.defaultUnit,
                    icon = FarmVisionMetricType.BatteryPercentage.icon,
                    trend = FarmVisionTrendDirection.Falling,
                    trendLabel = "-4% / 1h",
                    isSampleData = true
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))
            FarmVisionDivider()
            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // ==========================================
            // 5. STATE COMPONENTS
            // ==========================================
            FarmVisionSectionHeader(
                title = "5. State Components & Feedback",
                description = "Clear explanations of system conditions with actionable recovery triggers"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // State Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.extraSmall)
            ) {
                listOf("Loading", "Empty", "Error", "Unavailable").forEachIndexed { index, label ->
                    val isSelected = selectedStateTabIndex == index
                    Surface(
                        onClick = { selectedStateTabIndex = index },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                when (selectedStateTabIndex) {
                    0 -> FarmVisionLoadingState(
                        message = "Simulating sensor bus calibration (72% complete)...",
                        progressFraction = 0.72f
                    )
                    1 -> FarmVisionEmptyState(
                        title = "No Soil Samples Recorded",
                        message = "No sensor logs exist for the selected sector. Tap below to simulate a scan mission.",
                        actionText = "Simulate Scan",
                        onActionClick = { }
                    )
                    2 -> FarmVisionErrorState(
                        title = "I2C Sensor Timeout (Simulated)",
                        message = "Failed to receive acknowledge frame from soil probe at address 0x36 (Attempts: $retryCount).",
                        onRetry = { retryCount++ }
                    )
                    3 -> FarmVisionUnavailableState(
                        title = "Spectrometer Payload Offline",
                        message = "Hardware payload slot #2 reports disconnected. Attach sensor module to rover bus to activate."
                    )
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))
            FarmVisionDivider()
            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // ==========================================
            // 6. HELPERS & UTILITIES
            // ==========================================
            FarmVisionSectionHeader(
                title = "6. Helpers & Icon Containers",
                description = "Standardized icon badge surfaces and non-intrusive layout dividers"
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FarmVisionIconContainer(
                    icon = Icons.Rounded.Sensors,
                    contentDescription = "Sensor Icon"
                )

                FarmVisionIconContainer(
                    icon = Icons.Rounded.Grass,
                    contentDescription = "Crop Icon",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                FarmVisionIconContainer(
                    icon = Icons.Rounded.Science,
                    contentDescription = "Lab Icon",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer
                )

                Text(
                    text = "Standard 44dp icon containers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}
