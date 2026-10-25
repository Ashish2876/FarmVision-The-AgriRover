package com.farmvision.agrirover.ui.hardware.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.ui.theme.LocalAgriStatusColors

/**
 * Prominent, calm architectural disclaimer card highlighting physical inventory boundaries.
 * Explicitly informs users and reviewers what physical sensors or feedback mechanisms are absent.
 */
@Composable
fun HardwareHonestyCard(
    disclaimerText: String,
    modifier: Modifier = Modifier
) {
    val statusColors = LocalAgriStatusColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Hardware Capability Boundary: $disclaimerText"
            },
        shape = RoundedCornerShape(FarmVisionSpacing.medium),
        colors = CardDefaults.cardColors(
            containerColor = statusColors.warning.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, statusColors.warning.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FarmVisionSpacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Hardware Boundary Notice",
                tint = statusColors.warning,
                modifier = Modifier
                    .size(FarmVisionSpacing.iconSizeMedium)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(FarmVisionSpacing.small))

            Column {
                Text(
                    text = "Hardware Capability Boundary",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColors.warning
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = disclaimerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
