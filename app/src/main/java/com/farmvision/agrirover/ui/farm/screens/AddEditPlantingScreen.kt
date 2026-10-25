package com.farmvision.agrirover.ui.farm.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.farmvision.agrirover.core.designsystem.component.FarmVisionOutlinedButton
import com.farmvision.agrirover.core.designsystem.component.FarmVisionPrimaryButton
import com.farmvision.agrirover.core.designsystem.theme.FarmVisionSpacing
import com.farmvision.agrirover.domain.model.farm.Farm
import com.farmvision.agrirover.domain.model.farm.GrowthStage
import com.farmvision.agrirover.domain.model.farm.PlantHealthStatus
import com.farmvision.agrirover.domain.model.farm.Plot
import com.farmvision.agrirover.ui.farm.components.FarmVisionTextField
import com.farmvision.agrirover.ui.farm.management.PlantingFormDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Screen for creating or editing a Planting (Crop record),
 * supporting nullable dates, future planned dates (D5), growth stages,
 * health status, and harvest projections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlantingScreen(
    draft: PlantingFormDraft,
    farms: List<Farm>,
    plots: List<Plot>,
    isSubmitting: Boolean,
    onFarmChange: (String) -> Unit,
    onPlotChange: (String) -> Unit,
    onCropNameChange: (String) -> Unit,
    onVarietyChange: (String) -> Unit,
    onPlantingDateChange: (LocalDate?) -> Unit,
    onGrowthStageChange: (GrowthStage) -> Unit,
    onHealthStatusChange: (PlantHealthStatus) -> Unit,
    onDaysToHarvestChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = draft.plantingId != null
    val title = if (isEditing) "Edit Crop Planting" else "Add Crop Planting"

    var isDatePickerOpen by remember { mutableStateOf(false) }
    var isStageMenuOpen by remember { mutableStateOf(false) }
    var isHealthMenuOpen by remember { mutableStateOf(false) }
    var isPlotMenuOpen by remember { mutableStateOf(false) }

    val currentFarm = farms.find { it.id == draft.farmId }
    val currentPlot = plots.find { it.id == draft.plotId }
    val candidatePlots = plots.filter { it.farmId == draft.farmId }

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
                    text = "${currentFarm?.name ?: "Farm"} • ${currentPlot?.name ?: "Plot"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.large))

        // Target Plot Dropdown Selector
        if (candidatePlots.size > 1) {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentPlot?.name ?: "Select Plot",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Plot") },
                    trailingIcon = {
                        IconButton(onClick = { isPlotMenuOpen = true }) {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select Plot")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPlotMenuOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                DropdownMenu(
                    expanded = isPlotMenuOpen,
                    onDismissRequest = { isPlotMenuOpen = false }
                ) {
                    candidatePlots.forEach { plot ->
                        DropdownMenuItem(
                            text = { Text(plot.name) },
                            onClick = {
                                onPlotChange(plot.id)
                                isPlotMenuOpen = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))
        }

        // Crop Name Field
        FarmVisionTextField(
            value = draft.cropName,
            onValueChange = onCropNameChange,
            label = "Crop Name *",
            placeholder = "e.g. Roma Tomato, Sweet Corn",
            errorMessage = draft.nameError,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Variety Field
        FarmVisionTextField(
            value = draft.variety,
            onValueChange = onVarietyChange,
            label = "Variety / Botanical Classification (Optional)",
            placeholder = "e.g. Solanum lycopersicum",
            errorMessage = draft.varietyError,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Planting Date Picker (supports null dates and future dates labeled Planned)
        val isPlannedDate = draft.plantingDate != null && draft.plantingDate.isAfter(LocalDate.now())
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = when {
                    draft.plantingDate == null -> "Not recorded (tap to pick)"
                    isPlannedDate -> "${draft.plantingDate} (Planned Future Date)"
                    else -> draft.plantingDate.toString()
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Planting Date (Optional)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (draft.plantingDate != null) {
                        IconButton(onClick = { onPlantingDateChange(null) }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear Planting Date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(onClick = { isDatePickerOpen = true }) {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Open Date Picker")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDatePickerOpen = true },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        if (isDatePickerOpen) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = draft.plantingDate
                    ?.atStartOfDay(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli() ?: System.currentTimeMillis()
            )

            DatePickerDialog(
                onDismissRequest = { isDatePickerOpen = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedMillis = datePickerState.selectedDateMillis
                            if (selectedMillis != null) {
                                val localDate = Instant.ofEpochMilli(selectedMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                onPlantingDateChange(localDate)
                            }
                            isDatePickerOpen = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDatePickerOpen = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Growth Stage Dropdown Selector
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = draft.growthStage.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Growth Stage") },
                trailingIcon = {
                    IconButton(onClick = { isStageMenuOpen = true }) {
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select Growth Stage")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isStageMenuOpen = true },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            DropdownMenu(
                expanded = isStageMenuOpen,
                onDismissRequest = { isStageMenuOpen = false }
            ) {
                GrowthStage.entries.forEach { stage ->
                    DropdownMenuItem(
                        text = { Text(stage.label) },
                        onClick = {
                            onGrowthStageChange(stage)
                            isStageMenuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Health Status Dropdown Selector
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = draft.userHealthStatus.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Health Status Assessment") },
                trailingIcon = {
                    IconButton(onClick = { isHealthMenuOpen = true }) {
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Select Health Status")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isHealthMenuOpen = true },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            DropdownMenu(
                expanded = isHealthMenuOpen,
                onDismissRequest = { isHealthMenuOpen = false }
            ) {
                PlantHealthStatus.entries.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status.label) },
                        onClick = {
                            onHealthStatusChange(status)
                            isHealthMenuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Days to Harvest Field
        FarmVisionTextField(
            value = draft.daysToHarvestInput,
            onValueChange = onDaysToHarvestChange,
            label = "Days to Harvest (Optional)",
            placeholder = "e.g. 35",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(FarmVisionSpacing.medium))

        // Notes Field
        FarmVisionTextField(
            value = draft.userNotes,
            onValueChange = onNotesChange,
            label = "Planting Notes (Optional)",
            placeholder = "e.g. Flowering stage (Day 42). Hydration optimal.",
            errorMessage = draft.notesError,
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
                text = if (isEditing) "Save Changes" else "Record Planting",
                onClick = onSubmit,
                leadingIcon = Icons.Rounded.Check,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(FarmVisionSpacing.huge))
    }
}
