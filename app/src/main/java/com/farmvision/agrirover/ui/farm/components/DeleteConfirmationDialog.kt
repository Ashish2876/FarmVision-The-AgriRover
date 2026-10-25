package com.farmvision.agrirover.ui.farm.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.model.farm.DeletionReport
import com.farmvision.agrirover.ui.farm.management.DeletionTarget

/**
 * Cascading deletion confirmation dialog adhering to constraint D4.
 * Shows exact counts of affected dependent records before execution.
 */
@Composable
fun DeleteConfirmationDialog(
    target: DeletionTarget,
    report: DeletionReport,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, entityDescription) = when (target) {
        is DeletionTarget.FarmTarget -> "Delete Farm?" to "Farm \"${target.farm.name}\""
        is DeletionTarget.PlotTarget -> "Delete Plot?" to "Plot \"${target.plot.name}\" (in ${target.farmName})"
        is DeletionTarget.PlantingTarget -> "Delete Planting?" to "Crop \"${target.planting.cropName}\" (in ${target.plotName})"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Are you sure you want to permanently delete $entityDescription?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

                // Breakdown of exact affected dependent entities
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "This action is permanent and will remove:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (report.deletedFarmsCount > 0) {
                            Text(
                                text = "• ${report.deletedFarmsCount} Farm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (report.deletedPlotsCount > 0) {
                            Text(
                                text = "• ${report.deletedPlotsCount} Plot(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (report.deletedPlantingsCount > 0) {
                            Text(
                                text = "• ${report.deletedPlantingsCount} Crop Planting(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            FarmVisionPrimaryButton(
                text = "Delete Permanently",
                onClick = onConfirm,
                modifier = Modifier
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier
    )
}
