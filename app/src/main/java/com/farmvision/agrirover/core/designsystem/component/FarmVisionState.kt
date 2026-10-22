package com.farmvision.agrirover.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Standard empty state container communicating absence of data with optional recovery action.
 */
@Composable
fun FarmVisionEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Inbox,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FarmVisionSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(FarmVisionSpacing.large)
                    .size(FarmVisionSpacing.iconSizeLarge)
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.extraSmall))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))
            FarmVisionPrimaryButton(
                text = actionText,
                onClick = onActionClick
            )
        }
    }
}

/**
 * Standard loading state supporting both indeterminate and determinate progress.
 */
@Composable
fun FarmVisionLoadingState(
    message: String,
    modifier: Modifier = Modifier,
    progressFraction: Float? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FarmVisionSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (progressFraction != null) {
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Recoverable error state displaying the failure cause and providing a retry callback.
 */
@Composable
fun FarmVisionErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val statusColors = LocalAgriStatusColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FarmVisionSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = statusColors.error.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = statusColors.error,
                modifier = Modifier
                    .padding(FarmVisionSpacing.large)
                    .size(FarmVisionSpacing.iconSizeLarge)
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.extraSmall))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(FarmVisionSpacing.large))
            FarmVisionPrimaryButton(
                text = "Retry Action",
                onClick = onRetry,
                leadingIcon = Icons.Rounded.Refresh
            )
        }
    }
}

/**
 * State component displayed when hardware sensors or physical rover nodes are unavailable.
 */
@Composable
fun FarmVisionUnavailableState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FarmVisionSpacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.extraSmall))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Prominent banner explicitly marking simulated telemetry and demo modes.
 * Ensures simulated movement or readings are never mistaken for physical hardware confirmation.
 */
@Composable
fun FarmVisionDemoBanner(
    modifier: Modifier = Modifier,
    message: String = "SIMULATION ACTIVE: Telemetry is simulated. Physical rover is not connected."
) {
    val statusColors = LocalAgriStatusColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = statusColors.demo.copy(alpha = 0.12f),
        border = BorderStroke(FarmVisionSpacing.borderThin, statusColors.demo.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FarmVisionSpacing.medium, vertical = FarmVisionSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Science,
                contentDescription = "Demo Mode",
                tint = statusColors.demo,
                modifier = Modifier.size(FarmVisionSpacing.iconSizeMedium)
            )

            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = statusColors.demo
            )
        }
    }
}
