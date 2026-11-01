package com.farmvision.agrirover.ui.rover

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDemoBanner
import com.farmvision.agrirover.core.designsystem.component.FarmVisionDivider
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionSectionHeader
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusBadge
import com.farmvision.agrirover.core.designsystem.component.FarmVisionStatusType
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.data.model.FarmSettings
import com.farmvision.agrirover.data.model.RoverDriveMode
import com.farmvision.agrirover.data.model.RoverTelemetry
import com.farmvision.agrirover.data.model.SpeedPreset
import com.farmvision.agrirover.ui.rover.controller.JoystickMath
import com.farmvision.agrirover.ui.rover.controller.JoystickVector
import com.farmvision.agrirover.ui.rover.controller.VirtualJoystick
import com.farmvision.agrirover.ui.theme.BorderAccentDark
import com.farmvision.agrirover.ui.theme.BorderSubtleDark
import com.farmvision.agrirover.ui.theme.DarkBackground
import com.farmvision.agrirover.ui.theme.DarkGreen
import com.farmvision.agrirover.ui.theme.DarkSurface
import com.farmvision.agrirover.ui.theme.DarkSurfaceHigh
import com.farmvision.agrirover.ui.theme.DarkSurfaceVariant
import com.farmvision.agrirover.ui.theme.DarkThemeText
import com.farmvision.agrirover.ui.theme.FarmVisionTheme
import com.farmvision.agrirover.ui.theme.LightLime
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors
import com.farmvision.agrirover.ui.theme.PaleGreen
import com.farmvision.agrirover.ui.theme.ParrotGreen
import com.farmvision.agrirover.ui.theme.ParrotGreenDeep
import com.farmvision.agrirover.ui.theme.StatusAlert
import com.farmvision.agrirover.ui.theme.TelemetryValueLarge
import com.farmvision.agrirover.ui.theme.TextMutedDark
import com.farmvision.agrirover.ui.theme.TextPrimary
import com.farmvision.agrirover.ui.viewmodel.FarmViewModel

/**
 * Premium Drone-Style Robotics Rover Controller & Cockpit.
 * Features an immersive three-zone landscape layout:
 * - Top Safety Strip: Exit button, Rover identity, truthful status badges (Demo, Battery, Mode, Camera).
 * - Left Zone: Primary vector movement joystick, drive mode selector, and speed presets with disclaimer.
 * - Center Zone: Simulated camera canvas, camera tilt stepper, prominent industrial Emergency Stop, and latch notice.
 * - Right Zone: Secondary directional D-Pad (with center STOP) + compact telemetry HUD.
 *
 * Full orientation lifecycle management:
 * - Requests landscape orientation and hides system bars while active.
 * - Restores previous orientation and system bars on exit or disposal.
 * - Emits neutral velocity on interruption, backgrounding, or focus loss.
 */
@Composable
fun RoverScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier,
    onNavigateToHardwareCenter: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lifecycleOwner = LocalLifecycleOwner.current

    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current

    // Lifecycle-aware immersive mode in landscape and failsafe neutral emission
    DisposableEffect(lifecycleOwner, isLandscape) {
        val window = activity?.window

        // Hide system bars (immersive sticky mode) when in landscape; keep screen awake
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (isLandscape) {
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        // Failsafe: Release joystick immediately if app is paused or stopped
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onJoystickReleased()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onJoystickReleased()

            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLandscape) DarkBackground else MaterialTheme.colorScheme.background)
            .then(
                if (isLandscape) Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                else Modifier.statusBarsPadding()
            )
    ) {
        if (isLandscape) {
            RoverLandscapeCockpit(
                telemetry = telemetry,
                settings = settings,
                viewModel = viewModel,
                onNavigateToHardwareCenter = onNavigateToHardwareCenter,
                onExit = onExit
            )
        } else {
            RoverPortraitLayout(
                telemetry = telemetry,
                settings = settings,
                viewModel = viewModel,
                onNavigateToHardwareCenter = onNavigateToHardwareCenter,
                onExit = onExit
            )
        }
    }
}

// ============================================================================
// SECTION G — PREVIEWS FOR LANDSCAPE DRONE COCKPIT
// ============================================================================

@Preview(
    name = "Landscape Drone Cockpit - Normal",
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    showBackground = true
)
@Composable
private fun RoverLandscapeNormalPreview() {
    FarmVisionTheme(darkTheme = true) {
        val mockRepo = remember { com.farmvision.agrirover.data.repository.MockFarmRepository() }
        val mockVm = remember { FarmViewModel(mockRepo, null) }
        RoverScreen(viewModel = mockVm)
    }
}

@Preview(
    name = "Landscape Drone Cockpit - Latched E-Stop",
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    showBackground = true
)
@Composable
private fun RoverLandscapeLatchedPreview() {
    FarmVisionTheme(darkTheme = true) {
        val mockRepo = remember {
            com.farmvision.agrirover.data.repository.MockFarmRepository().apply {
                setEmergencyStop(true)
            }
        }
        val mockVm = remember { FarmViewModel(mockRepo, null) }
        RoverScreen(viewModel = mockVm)
    }
}

@Preview(
    name = "Landscape Drone Cockpit - Disconnected",
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    showBackground = true
)
@Composable
private fun RoverLandscapeDisconnectedPreview() {
    FarmVisionTheme(darkTheme = true) {
        val mockRepo = remember { com.farmvision.agrirover.data.repository.MockFarmRepository() }
        val mockVm = remember { FarmViewModel(mockRepo, null) }
        RoverScreen(viewModel = mockVm)
    }
}

@Preview(
    name = "Landscape Drone Cockpit - Compact Phone",
    device = "spec:width=640dp,height=360dp,orientation=landscape",
    showBackground = true
)
@Composable
private fun RoverLandscapeCompactPreview() {
    FarmVisionTheme(darkTheme = true) {
        val mockRepo = remember { com.farmvision.agrirover.data.repository.MockFarmRepository() }
        val mockVm = remember { FarmViewModel(mockRepo, null) }
        RoverScreen(viewModel = mockVm)
    }
}


@Preview(
    name = "Portrait Controller Cockpit",
    device = "spec:width=411dp,height=891dp,orientation=portrait",
    showBackground = true
)
@Composable
private fun RoverPortraitPreview() {
    FarmVisionTheme(darkTheme = false) {
        val mockRepo = remember { com.farmvision.agrirover.data.repository.MockFarmRepository() }
        val mockVm = remember { FarmViewModel(mockRepo, null) }
        RoverScreen(viewModel = mockVm)
    }
}

@Preview(
    name = "Landscape Drone Cockpit - Tablet",
    device = "spec:width=1280dp,height=800dp,orientation=landscape",
    showBackground = true
)
@Composable
private fun RoverLandscapeTabletPreview() {
    FarmVisionTheme(darkTheme = true) {
        val mockRepo = remember { com.farmvision.agrirover.data.repository.MockFarmRepository() }
        val mockVm = remember { FarmViewModel(mockRepo, null) }
        RoverScreen(viewModel = mockVm)
    }
}

@Preview(
    name = "Landscape Drone Cockpit - 1.5x Font Scaling",
    device = "spec:width=891dp,height=411dp,orientation=landscape",
    fontScale = 1.5f,
    showBackground = true
)
@Composable
private fun RoverLandscapeFontScalePreview() {
    FarmVisionTheme(darkTheme = true) {
        val mockRepo = remember { com.farmvision.agrirover.data.repository.MockFarmRepository() }
        val mockVm = remember { FarmViewModel(mockRepo, null) }
        RoverScreen(viewModel = mockVm)
    }
}
