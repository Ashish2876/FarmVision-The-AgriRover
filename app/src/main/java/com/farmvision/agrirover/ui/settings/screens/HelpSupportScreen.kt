package com.farmvision.agrirover.ui.settings.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionCard
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.settings.components.SettingsTopBar

/**
 * Module 7 — Help & Support Screen.
 * Operational user guides, diagnostic troubleshooting, agronomic FAQ, and GitHub issue tracker link.
 */
@Composable
fun HelpSupportScreen(
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
                title = "Help & Support",
                subtitle = "User Guides • Troubleshooting • FAQ",
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
            // Getting Started Guides
            FarmVisionSectionHeader(
                title = "Getting Started",
                description = "Core functionality overview and workflow guide"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            FaqAccordionItem(
                question = "How do I navigate between sections?",
                answer = "The bottom navigation bar provides 5 primary destinations: Home (Telemetry Dashboard), Rover (Robotics Cockpit), My Farm (Plant Profiles), Analytics (Historical Trend Charts), and Settings (Configuration Management)."
            )
            FaqAccordionItem(
                question = "How does the AI Assistant use my sensor data?",
                answer = "When enabled in Privacy Settings, the AI Assistant receives your current crop type, uncalibrated soil moisture ADC count, pH, and climate readings. It provides customized agronomic guidance based on your field's live state."
            )
            FaqAccordionItem(
                question = "How are alerts generated and acknowledged?",
                answer = "The Alert Engine evaluates live telemetry against configured safety limits. Threshold breaches confirmed after 3 consecutive debounce cycles trigger persistent alerts in the Alert Centre."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Diagnostic Troubleshooting
            FarmVisionSectionHeader(
                title = "Troubleshooting Guide",
                description = "Solutions for common network and AI error states"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            FaqAccordionItem(
                question = "Backend connection failed (HTTP 10.0.2.2:8000)?",
                answer = "Ensure the FastAPI backend daemon is running on your host machine ('python -m uvicorn app.main:app --host 0.0.0.0 --port 8000'). On the Android Emulator, '10.0.2.2' bridges directly to the host loopback."
            )
            FaqAccordionItem(
                question = "AI Assistant reports 'Quota Exhausted' (HTTP 429)?",
                answer = "The Google AI Studio Free Tier allows 20 requests per day per model. When reached, the assistant displays the expected reset duration. You can also enable 'Simulation Mock Mode' to continue testing offline without cloud quota."
            )
            FaqAccordionItem(
                question = "Why does soil moisture show raw ADC numbers instead of percentage?",
                answer = "Capacitive sensors measure electrical permittivity (voltage), not literal water volume. Uncalibrated ADC counts vary by soil texture (sand vs clay). FarmVision enforces sensor honesty by requiring calibration curves before displaying percentages."
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // Actuator Safety Invariant
            FarmVisionSectionHeader(
                title = "Robotics & Hardware Safety",
                description = "Software-only control boundaries"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "FarmVision AgriRover enforces ACTUATORS_ENABLED = 0. All motor driving, chemical spraying, and pump actuations are physically blocked by default. The AI Assistant has ZERO physical control capability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

            // GitHub Support Link
            FarmVisionSectionHeader(
                title = "Official Issue Tracker",
                description = "Report bugs or request enhancements on GitHub"
            )
            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
            FarmVisionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "If you encounter unexpected bugs, crashes, or firmware protocol discrepancies, please submit an issue on the official FarmVision repository.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
                Button(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Ashish2876/FarmVision-The-AgriRover/issues")
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
                    Text("Open GitHub Issues")
                }
            }

            Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
        }
    }
}

@Composable
private fun FaqAccordionItem(
    question: String,
    answer: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    FarmVisionCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                    FarmVisionDivider()
                    Spacer(modifier = Modifier.height(FarmVisionSpacing.small))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

