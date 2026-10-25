package com.farmvision.agrirover.ui.farm.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionOutlinedButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.model.farm.AreaUnit
import com.farmvision.agrirover.ui.farm.components.FarmVisionTextField
import com.farmvision.agrirover.ui.farm.management.PlotFormDraft

/**
 * Screen for creating or editing a Plot within a Farm parcel,
 * with area conversion, validation feedback, and double-submission protection.
 */
@Composable
fun AddEditPlotScreen(
    draft: PlotFormDraft,
    farmName: String,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAreaChange: (String, AreaUnit) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = draft.plotId != null
    val title = if (isEditing) "Edit Plot" else "Add Plot"

    Column(modifier = modifier.fillMaxWidth()) {
        // Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Cancel and go back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Plot in $farmName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Plot Name Field
        FarmVisionTextField(
            value = draft.name,
            onValueChange = onNameChange,
            label = "Plot Name *",
            placeholder = "e.g. Sector A - Plot 2",
            errorMessage = draft.nameError,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Description Field
        FarmVisionTextField(
            value = draft.description,
            onValueChange = onDescriptionChange,
            label = "Description (Optional)",
            placeholder = "e.g. Alluvial soil row with drip irrigation",
            singleLine = true
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Area Input Field & Unit Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            FarmVisionTextField(
                value = draft.areaInput,
                onValueChange = { onAreaChange(it, draft.areaUnit) },
                label = "Plot Area (${draft.areaUnit.symbol}) (Optional)",
                placeholder = "e.g. 15000",
                leadingIcon = Icons.Rounded.SquareFoot,
                errorMessage = draft.areaError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(FarmVisionSpacing.small))

            // Area Unit Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.small)
            ) {
                AreaUnit.entries.forEach { unit ->
                    val isSelected = draft.areaUnit == unit
                    FilterChip(
                        selected = isSelected,
                        onClick = { onAreaChange(draft.areaInput, unit) },
                        label = { Text(unit.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Notes Field
        FarmVisionTextField(
            value = draft.notes,
            onValueChange = onNotesChange,
            label = "Plot Notes (Optional)",
            placeholder = "Optimal drainage zone, soil type, furrow layout",
            singleLine = false,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FarmVisionSpacing.medium)
        ) {
            FarmVisionOutlinedButton(
                text = "Cancel",
                onClick = onCancel,
                leadingIcon = Icons.Rounded.Close,
                modifier = Modifier.weight(1f)
            )

            FarmVisionPrimaryButton(
                text = if (isEditing) "Save Changes" else "Create Plot",
                onClick = onSubmit,
                leadingIcon = Icons.Rounded.Check,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
    }
}
