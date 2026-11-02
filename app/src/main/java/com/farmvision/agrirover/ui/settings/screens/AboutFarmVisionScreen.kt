package com.farmvision.agrirover.ui.settings.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.settings.components.SettingsInfoRow
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 8 — About FarmVision Screen.
 * Application metadata, architecture specifications, open source notices, and repository links.
 * Incorporates subtle developer attribution strictly compliant with design requirements.
 */
@Composable
fun AboutFarmVisionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            SettingsTopBar(
                title = "About FarmVision",
                subtitle = "Specifications • Architecture • Licenses",
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
                .padding(horizontal = FarmVisionSpacing.large, vertical = FarmVisionSpacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

            // App Brand Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Agriculture,
                    contentDescription = "FarmVision AgriRover Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            Text(
                text = "FarmVision — The AgriRover",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Smart Agriculture & AI-Powered Rover System",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Application Build Specs
            FarmVisionSectionHeader(
                title = "Application Specifications",
                description = "Version, runtime environment, and package parameters"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(label = "Application ID", value = "com.farmvision.agrirover")
                SettingsInfoRow(label = "Version", value = "1.0.0")
                SettingsInfoRow(label = "Build Number", value = "1")
                SettingsInfoRow(label = "Target SDK", value = "Android 15 (API 35)")
                SettingsInfoRow(label = "Min SDK", value = "Android 8.0 (API 26)")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Technology Architecture Stack
            FarmVisionSectionHeader(
                title = "Technology Architecture",
                description = "Core frameworks and engineering stack"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(label = "UI Architecture", value = "Jetpack Compose • Material 3")
                SettingsInfoRow(label = "Design System", value = "Parrot Green (#78E000)")
                SettingsInfoRow(label = "Core Stack", value = "Kotlin Coroutines & Flow • MVVM")
                SettingsInfoRow(label = "Backend Gateway", value = "Python FastAPI • Uvicorn")
                SettingsInfoRow(label = "Cloud AI Model", value = "Google Gemini 3.5 Flash Lite")
                SettingsInfoRow(label = "Firmware Target", value = "ESP32 FreeRTOS Dual-Core")
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Repository Link
            FarmVisionSectionHeader(
                title = "Source Repository",
                description = "Official project repository and open source tracking"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "FarmVision is managed as an agricultural technology development platform. Access documentation, protocol contracts, and releases on GitHub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Ashish2876/FarmVision-The-AgriRover")
                        )
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Visit GitHub Repository")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Legal & Open Source Notice
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "© 2026 FarmVision — The AgriRover Project. Licensed under the Apache License, Version 2.0. ACTUATORS_ENABLED = 0 physical hardware interlock is strictly enforced.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.extraLarge))

            // Subtle Developer Credit (Strictly low visual prominence, small typography, single occurrence)
            Text(
                text = "Developed by Ashish Sampat Pawar",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

