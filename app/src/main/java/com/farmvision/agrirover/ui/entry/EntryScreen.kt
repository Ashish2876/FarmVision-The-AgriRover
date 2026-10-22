package com.farmvision.agrirover.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farmvision.agrirover.R
import com.farmvision.agrirover.ui.theme.BorderAccentDark
import com.farmvision.agrirover.ui.theme.BorderSubtleDark
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkSurface
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.DeepForest
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.FreshLeaf
import com.farmvision.agrirover.ui.theme.MintAccent
import com.farmvision.agrirover.ui.theme.PrimaryGreen
import com.farmvision.agrirover.ui.theme.StatusOnline
import com.farmvision.agrirover.ui.theme.TextMutedDark
import com.farmvision.agrirover.ui.debug.DebugShowcaseLauncher

/**
 * Minimal branded entry screen for FarmVision AgriRover.
 * Establishes the visual identity, deliberate hierarchy, and technical foundation
 * for Phase 1 without presenting fabricated telemetry or unbacked controls.
 */
@Composable
fun EntryScreen(
    modifier: Modifier = Modifier,
    onNavigateToCatalog: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF0F1E17),
                        DarkBackground
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Stage Identifier Badge
            PhaseBadge()

            Spacer(modifier = Modifier.height(28.dp))

            // Hero Brand Emblem
            BrandEmblem()

            Spacer(modifier = Modifier.height(20.dp))

            // Application Identity & Typography
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "AGRIROVER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    color = FreshLeaf
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedDark,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Technical Foundation Verification Card
            TechnicalBaselineCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Architectural Roadmap & Scope Card
            RoadmapStatusCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Debug Component Showcase (Available in debug builds, excluded in release)
            DebugShowcaseLauncher()

            if (onNavigateToCatalog != null) {
                Spacer(modifier = Modifier.height(12.dp))
                com.farmvision.agrirover.ui.components.TactileButton(
                    text = "Explore Reusable Components (Phase 2)",
                    onClick = onNavigateToCatalog,
                    variant = com.farmvision.agrirover.ui.components.TactileButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Development Attribution & Safe Boundary Notice
            Text(
                text = "Ashish2876 / FarmVision-The-AgriRover",
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedDark.copy(alpha = 0.7f)
            )
            Text(
                text = "Phase 2 Active • Hardware & ESP32 Integration Deferred to Post-App Milestones",
                style = MaterialTheme.typography.labelSmall,
                color = TextMutedDark.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun PhaseBadge() {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderAccentDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(StatusOnline)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PHASE 2 OF 15 : REUSABLE COMPONENTS ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MintAccent
            )
        }
    }
}

@Composable
private fun BrandEmblem() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DeepForest,
                        Color(0xFF0F261B)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(FreshLeaf, BorderSubtleDark)
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Icon(
            imageVector = Icons.Rounded.Agriculture,
            contentDescription = "FarmVision AgriRover Logo",
            tint = MintAccent,
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun TechnicalBaselineCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = FreshLeaf,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Verified Technical Baseline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BaselineRow(
                icon = Icons.Rounded.Memory,
                title = "Runtime & Toolchain",
                detail = "Kotlin 2.0.21 • Java 17 • Gradle 8.11"
            )
            BaselineRow(
                icon = Icons.Rounded.Palette,
                title = "UI Framework",
                detail = "Jetpack Compose BOM • Material 3 Design Tokens"
            )
            BaselineRow(
                icon = Icons.Rounded.Sensors,
                title = "SDK Target & Architecture",
                detail = "CompileSdk 35 • TargetSdk 35 • MinSdk 26"
            )
        }
    }
}

@Composable
private fun BaselineRow(
    icon: ImageVector,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Verified",
            tint = PrimaryGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedDark
            )
        }
    }
}

@Composable
private fun RoadmapStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtleDark)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Roadmap Status & Scope Boundary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MintAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Phase 1 establishes the verified Android foundation, version catalog, design tokens, and documentation. Features including full navigation, rover controls, sensor monitoring, and hardware telemetry belong to subsequent phases.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedDark,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(name = "Dark Theme Preview", showBackground = true, backgroundColor = 0xFF0B1410)
@Composable
private fun EntryScreenDarkPreview() {
    FarmVisionTheme(darkTheme = true) {
        EntryScreen()
    }
}

@Preview(name = "Light Theme Preview", showBackground = true, backgroundColor = 0xFFF5F8F3)
@Composable
private fun EntryScreenLightPreview() {
    FarmVisionTheme(darkTheme = false) {
        EntryScreen()
    }
}
