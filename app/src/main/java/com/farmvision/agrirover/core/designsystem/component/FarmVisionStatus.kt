package com.farmvision.agrirover.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Standard StatusType model encompassing operational rover states,
 * network connectivity, and simulation modes.
 *
 * NOTE: Components must NEVER default to or imply "Online" or "Connected".
 * Real telemetry and hardware connection states belong to subsequent phases.
 * Simulated or development values must be explicitly marked with [DemoData].
 */
enum class FarmVisionStatusType(
    val defaultLabel: String,
    val icon: ImageVector
) {
    Online("Online", Icons.Rounded.CheckCircle),
    Offline("Offline", Icons.Rounded.Cancel),
    Connecting("Connecting", Icons.Rounded.Sync),
    Active("Active", Icons.Rounded.PlayArrow),
    Idle("Idle", Icons.Rounded.Pause),
    Warning("Warning", Icons.Rounded.Warning),
    Error("Error", Icons.Rounded.Error),
    DemoData("Demo Data", Icons.Rounded.Science)
}

/**
 * Accessible status indicator combining a distinct geometric icon with an explicit label.
 * NEVER renders color alone to ensure full accessibility for color-blind users.
 */
@Composable
fun FarmVisionStatusBadge(
    status: FarmVisionStatusType,
    modifier: Modifier = Modifier,
    customLabel: String? = null
) {
    val statusColors = LocalAgriStatusColors.current

    val (tintColor, containerColor) = when (status) {
        FarmVisionStatusType.Online -> statusColors.online to statusColors.online.copy(alpha = 0.12f)
        FarmVisionStatusType.Offline -> statusColors.offline to statusColors.offline.copy(alpha = 0.12f)
        FarmVisionStatusType.Connecting -> statusColors.connecting to statusColors.connecting.copy(alpha = 0.12f)
        FarmVisionStatusType.Active -> statusColors.active to statusColors.active.copy(alpha = 0.12f)
        FarmVisionStatusType.Idle -> statusColors.idle to statusColors.idle.copy(alpha = 0.12f)
        FarmVisionStatusType.Warning -> statusColors.warning to statusColors.warning.copy(alpha = 0.12f)
        FarmVisionStatusType.Error -> statusColors.error to statusColors.error.copy(alpha = 0.12f)
        FarmVisionStatusType.DemoData -> statusColors.demo to statusColors.demo.copy(alpha = 0.12f)
    }

    val displayLabel = customLabel ?: status.defaultLabel

    Surface(
        modifier = modifier.semantics {
            contentDescription = "Status: $displayLabel"
        },
        shape = RoundedCornerShape(FarmVisionSpacing.small),
        color = containerColor,
        border = BorderStroke(FarmVisionSpacing.borderThin, tintColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmVisionSpacing.small, vertical = FarmVisionSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = status.icon,
                contentDescription = null, // Described by parent semantics
                tint = tintColor,
                modifier = Modifier.size(FarmVisionSpacing.iconSizeSmall)
            )

            Spacer(modifier = Modifier.width(FarmVisionSpacing.extraSmall))

            Text(
                text = displayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
        }
    }
}
