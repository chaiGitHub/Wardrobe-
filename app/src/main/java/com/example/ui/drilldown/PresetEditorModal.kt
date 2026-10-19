package com.example.ui.drilldown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandRose
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetEditorModal(
    isOpen: Boolean,
    preset: DrillPreset,
    onClose: () -> Unit,
    onSavePreset: (DrillPreset) -> Unit,
    onSaveAsNew: (DrillPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onResetDefaults: () -> Unit
) {
    if (!isOpen) return

    var name by remember(preset) { mutableStateOf(preset.name) }
    var description by remember(preset) { mutableStateOf(preset.description) }
    var groupTiers by remember(preset) { mutableStateOf(preset.groupTiers.toMutableList()) }
    var sortField by remember(preset) { mutableStateOf(preset.sortField) }
    var sortOrder by remember(preset) { mutableStateOf(preset.sortOrder) }
    var visibleFields by remember(preset) { mutableStateOf(preset.visibleFields.toMutableList()) }
    var newTierSelected by remember { mutableStateOf("") }

    val fieldMap = remember { ALL_AVAILABLE_FIELDS.associateBy { it.key } }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("preset_editor_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = BrandEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Customize Drill Preset",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Configure grouping hierarchy, sorting & display columns",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate500
                        )
                    }
                }

                HorizontalDivider(color = Slate200)

                // Modal Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Name & Description
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Preset Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate800
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Preset Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandEmerald,
                                unfocusedBorderColor = Slate200
                            )
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description & Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandEmerald,
                                unfocusedBorderColor = Slate200
                            )
                        )
                    }

                    HorizontalDivider(color = Slate100)

                    // Grouping Tiers Hierarchy
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hierarchical Grouping Tiers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Slate800
                            )
                            Text(
                                text = "${groupTiers.size}/5 tiers",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500
                            )
                        }

                        Text(
                            text = "Levels are drilled sequentially (L1 then L2 then L3). Garments are grouped by these tiers.",
                            fontSize = 11.sp,
                            color = Slate500
                        )

                        // List of Current Tiers
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            groupTiers.forEachIndexed { index, tierKey ->
                                val fieldDef = fieldMap[tierKey]
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = BrandEmerald,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "L${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = fieldDef?.label ?: tierKey,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Slate800
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            // Move Up
                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        val list = groupTiers.toMutableList()
                                                        val temp = list[index]
                                                        list[index] = list[index - 1]
                                                        list[index - 1] = temp
                                                        groupTiers = list
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move up",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (index > 0) Slate700 else Slate400
                                                )
                                            }

                                            // Move Down
                                            IconButton(
                                                onClick = {
                                                    if (index < groupTiers.size - 1) {
                                                        val list = groupTiers.toMutableList()
                                                        val temp = list[index]
                                                        list[index] = list[index + 1]
                                                        list[index + 1] = temp
                                                        groupTiers = list
                                                    }
                                                },
                                                enabled = index < groupTiers.size - 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move down",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (index < groupTiers.size - 1) Slate700 else Slate400
                                                )
                                            }

                                            // Delete Tier
                                            IconButton(
                                                onClick = {
                                                    if (groupTiers.size > 1) {
                                                        val list = groupTiers.toMutableList()
                                                        list.removeAt(index)
                                                        groupTiers = list
                                                    }
                                                },
                                                enabled = groupTiers.size > 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove tier",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (groupTiers.size > 1) BrandRose else Slate400
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add Tier options (if < 5)
                        if (groupTiers.size < 5) {
                            val availableToAdd = ALL_AVAILABLE_FIELDS.filter { !groupTiers.contains(it.key) }
                            if (availableToAdd.isNotEmpty()) {
                                Text(
                                    text = "Add Next Grouping Level:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate600,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    availableToAdd.forEach { field ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                if (groupTiers.size < 5) {
                                                    val list = groupTiers.toMutableList()
                                                    list.add(field.key)
                                                    groupTiers = list
                                                }
                                            },
                                            label = { Text("+ ${field.label}", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = Color.White,
                                                labelColor = Slate700
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // Sorting Rule
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Branch Record Sorting",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate800
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Sort Order:", fontSize = 12.sp, color = Slate600)
                            FilterChip(
                                selected = sortOrder == "desc",
                                onClick = { sortOrder = "desc" },
                                label = { Text("Descending ↓", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = sortOrder == "asc",
                                onClick = { sortOrder = "asc" },
                                label = { Text("Ascending ↑", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Text(text = "Sort Attribute:", fontSize = 12.sp, color = Slate600)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "wearing frequency(how many times legacy washed)" to "Wear Frequency",
                                "Total Washed Count" to "Total Washes",
                                "price" to "Price",
                                "Name" to "Garment Name",
                                "Clothes Type" to "Category",
                                "material" to "Material",
                                "beautiful" to "Aesthetic Rating",
                                "wash stated" to "Wash State"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = sortField == key,
                                    onClick = { sortField = key },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandEmerald,
                                        selectedLabelColor = Color.White,
                                        containerColor = Slate100,
                                        labelColor = Slate700
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // Visible Card / Table Fields
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Visible Fields on Records",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate800
                        )
                        Text(
                            text = "Select which attributes are displayed on record cards and table columns.",
                            fontSize = 11.sp,
                            color = Slate500
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ALL_AVAILABLE_FIELDS.forEach { field ->
                                val isChecked = visibleFields.contains(field.key)
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        val list = visibleFields.toMutableList()
                                        if (isChecked) {
                                            if (list.size > 1) list.remove(field.key)
                                        } else {
                                            list.add(field.key)
                                        }
                                        visibleFields = list
                                    },
                                    label = { Text(field.label, fontSize = 11.sp) },
                                    leadingIcon = if (isChecked) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandEmerald.copy(alpha = 0.15f),
                                        selectedLabelColor = BrandEmerald,
                                        containerColor = Slate100,
                                        labelColor = Slate600
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Footer Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val newPreset = DrillPreset(
                                    id = "custom-${System.currentTimeMillis()}",
                                    name = if (name.isNotBlank()) "$name (Copy)" else "Custom Preset",
                                    description = description,
                                    groupTiers = if (groupTiers.isNotEmpty()) groupTiers else listOf("Clothes Type"),
                                    sortField = sortField,
                                    sortOrder = sortOrder,
                                    visibleFields = visibleFields,
                                    isCustom = true
                                )
                                onSaveAsNew(newPreset)
                                onClose()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save as New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val updated = preset.copy(
                                    name = name.ifBlank { "Custom Preset" },
                                    description = description,
                                    groupTiers = if (groupTiers.isNotEmpty()) groupTiers else listOf("Clothes Type"),
                                    sortField = sortField,
                                    sortOrder = sortOrder,
                                    visibleFields = visibleFields
                                )
                                onSavePreset(updated)
                                onClose()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset defaults button
                        OutlinedButton(
                            onClick = {
                                onResetDefaults()
                                onClose()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Slate600
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Defaults", fontSize = 11.sp, color = Slate700)
                        }

                        // Delete preset button if custom
                        if (preset.isCustom) {
                            OutlinedButton(
                                onClick = {
                                    onDeletePreset(preset.id)
                                    onClose()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRose)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = BrandRose
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 11.sp, color = BrandRose)
                            }
                        }
                    }
                }
            }
        }
    }
}
