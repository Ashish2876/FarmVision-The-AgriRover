package com.farmvision.agrirover.ui.hardware.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.data.model.hardware.ControllerTarget
import com.farmvision.agrirover.data.model.hardware.HardwareCatalogSeed
import com.farmvision.agrirover.ui.hardware.components.ControllerEndpointCard
import com.farmvision.agrirover.ui.hardware.components.HardwareComponentCard
import com.farmvision.agrirover.ui.hardware.components.HardwareSummaryRow
import com.farmvision.agrirover.ui.hardware.model.EndpointConnectionStatus
import com.farmvision.agrirover.ui.hardware.model.HardwareSummaryCounts
import com.farmvision.agrirover.ui.hardware.model.HardwareUiDataMapper
import com.farmvision.agrirover.ui.theme.FarmVisionTheme

import androidx.compose.runtime.remember
import com.farmvision.agrirover.data.repository.InMemoryHardwareSettingsRepository
import com.farmvision.agrirover.ui.hardware.viewmodel.HardwareCenterViewModel

@Preview(name = "Hardware Center — Light 1.0x", showBackground = true)
@Composable
fun HardwareCenterLightPreview() {
    FarmVisionTheme(darkTheme = false) {
        val previewVm = remember { HardwareCenterViewModel(InMemoryHardwareSettingsRepository()) }
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            HardwareCenterScreen(onNavigateBack = {}, viewModel = previewVm)
        }
    }
}

@Preview(name = "Hardware Center — Dark 1.0x", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HardwareCenterDarkPreview() {
    FarmVisionTheme(darkTheme = true) {
        val previewVm = remember { HardwareCenterViewModel(InMemoryHardwareSettingsRepository()) }
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            HardwareCenterScreen(onNavigateBack = {}, viewModel = previewVm)
        }
    }
}

@Preview(name = "Hardware Center — Light 2.0x Font Scale", showBackground = true, fontScale = 2.0f)
@Composable
fun HardwareCenterLargeFontLightPreview() {
    FarmVisionTheme(darkTheme = false) {
        val previewVm = remember { HardwareCenterViewModel(InMemoryHardwareSettingsRepository()) }
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            HardwareCenterScreen(onNavigateBack = {}, viewModel = previewVm)
        }
    }
}

@Preview(name = "Hardware Center — Dark 2.0x Font Scale", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 2.0f)
@Composable
fun HardwareCenterLargeFontDarkPreview() {
    FarmVisionTheme(darkTheme = true) {
        val previewVm = remember { HardwareCenterViewModel(InMemoryHardwareSettingsRepository()) }
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            HardwareCenterScreen(onNavigateBack = {}, viewModel = previewVm)
        }
    }
}

@Preview(name = "Component Detail — Light", showBackground = true)
@Composable
fun ComponentDetailLightPreview() {
    val items = HardwareUiDataMapper.createUiItems(isDemoMode = true)
    val probeItem = items.first { it.component.id == "hw-sensor-soil-moisture-probes" }
    FarmVisionTheme(darkTheme = false) {
        ComponentDetailScreen(
            item = probeItem,
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Component Detail — Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ComponentDetailDarkPreview() {
    val items = HardwareUiDataMapper.createUiItems(isDemoMode = true)
    val pumpItem = items.first { it.component.id == "hw-irrigation-pump" }
    FarmVisionTheme(darkTheme = true) {
        ComponentDetailScreen(
            item = pumpItem,
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Component Cards Overview — Light", showBackground = true)
@Composable
fun ComponentCardsLightPreview() {
    val items = HardwareUiDataMapper.createUiItems(isDemoMode = true)
    FarmVisionTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            HardwareComponentCard(item = items[0], onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            HardwareComponentCard(item = items[3], onClick = {})
            Spacer(modifier = Modifier.height(12.dp))
            HardwareComponentCard(item = items[7], onClick = {})
        }
    }
}
