package com.example.ui.drilldown

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun PresetEditorModal(
    isOpen: Boolean,
    preset: DrillPreset,
    onClose: () -> Unit,
    onSavePreset: (DrillPreset) -> Unit,
    onSaveAsNew: (DrillPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onRestorePreset: ((String) -> Unit)? = null
) {
    if (!isOpen) return

    var name by remember(preset) { mutableStateOf(preset.name) }
    var description by remember(preset) { mutableStateOf(preset.description) }
    var groupTiers by remember(preset) { mutableStateOf(preset.groupTiers.toMutableList()) }
    var sortField by remember(preset) { mutableStateOf(preset.sortField) }
    var sortOrder by remember(preset) { mutableStateOf(preset.sortOrder) }
    var visibleFields by remember(preset) { mutableStateOf(preset.visibleFields.toMutableList()) }

    var selectedFieldToAdd by remember { mutableStateOf<String?>(null) }
    var showAddTierDropdown by remember { mutableStateOf(false) }
    var showSortFieldDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val fieldMap = remember { ALL_AVAILABLE_FIELDS.associateBy { it.key } }

    val sortFieldOptions = remember {
        listOf(
            "Name" to "Name — Garment primary name or title in wardrobe",
            "Last Washed" to "Last Washed Date (上次清洗日期)",
            "Total Washed Count" to "Total Washed Count (Legacy + App Tracked)",
            "wearing frequency(how many times legacy washed)" to "wearing frequency(how many times legacy washed)",
            "Tracked App Washes" to "Tracked App Washes (Recorded in app)",
            "price" to "Price — Original retail purchase cost",
            "Clothes Type" to "Clothes Type (Dress, Bra, Pants, etc.)",
            "material" to "Material — Primary fabric blend",
            "beautiful" to "beautiful — Aesthetic style rating",
            "wash stated" to "wash stated — Wash status (Need Wash / Clean)"
        )
    }

    // Auto-save function so tapping Back (<-), X, or system back never loses user customizations
    val saveAndClose = {
        val updated = preset.copy(
            name = name.ifBlank { preset.name },
            description = description,
            groupTiers = if (groupTiers.isNotEmpty()) groupTiers else preset.groupTiers,
            sortField = sortField,
            sortOrder = sortOrder,
            visibleFields = if (visibleFields.isNotEmpty()) visibleFields else preset.visibleFields
        )
        onSavePreset(updated)
        onClose()
    }

    // Intercept physical or gesture back button to auto-save customizations
    BackHandler(enabled = isOpen) {
        saveAndClose()
    }

    Dialog(
        onDismissRequest = { saveAndClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val maxCardHeight = maxHeight * 0.90f
            val isTablet = maxWidth >= 600.dp

            Card(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 580.dp)
                    .fillMaxWidth(if (isTablet) 0.72f else 0.95f)
                    .heightIn(max = maxCardHeight)
                    .wrapContentHeight()
                    .testTag("preset_editor_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                // Header: Back Arrow (<-), Purple Tune Icon, Title, Close Button (X)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { saveAndClose() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Save and Back",
                                tint = Slate700,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6D28D9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Customize Report Preset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { saveAndClose() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Slate200)

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 1: PRESET INFORMATION
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PRESET INFORMATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Slate700,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Preset Title",
                            fontSize = 11.sp,
                            color = Slate600
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Enter preset title...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPurple,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        Text(
                            text = "Description",
                            fontSize = 11.sp,
                            color = Slate600,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Enter preset description...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandPurple,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = Slate100)

                    // SECTION 2: DRILL-DOWN GROUPING HIERARCHY (MAX 10 TIERS)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "DRILL-DOWN GROUPING HIERARCHY (MAX 10 TIERS)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = BrandPurple,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = "${groupTiers.size}/10 tiers",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500,
                                softWrap = false,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = "When drilling into any tier branch, unselected sibling branches collapse immediately to focus on the selected path.",
                            fontSize = 11.sp,
                            color = Slate500,
                            lineHeight = 15.sp
                        )

                        // Tier Cards List
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            groupTiers.forEachIndexed { index, tierKey ->
                                val fieldDef = fieldMap[tierKey]
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Slate200)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header Row: Badge + Title + Subtitle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4F46E5)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = fieldDef?.label ?: tierKey,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Slate900
                                                )
                                                if (!fieldDef?.description.isNullOrBlank()) {
                                                    Text(
                                                        text = fieldDef!!.description,
                                                        fontSize = 11.sp,
                                                        color = Slate500,
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Database Key:",
                                                        fontSize = 10.sp,
                                                        color = Slate500
                                                    )
                                                    Surface(
                                                        color = Slate100,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = tierKey,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Slate700,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Action Row: Up, Down, Delete buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
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
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    border = BorderStroke(1.dp, if (index > 0) Slate200 else Color(0xFFF1F5F9))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowUpward,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp),
                                                        tint = if (index > 0) Slate700 else Slate400
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Up",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (index > 0) Slate700 else Slate400
                                                    )
                                                }

                                                OutlinedButton(
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
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    border = BorderStroke(1.dp, if (index < groupTiers.size - 1) Slate200 else Color(0xFFF1F5F9))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDownward,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp),
                                                        tint = if (index < groupTiers.size - 1) Slate700 else Slate400
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Down",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (index < groupTiers.size - 1) Slate700 else Slate400
                                                    )
                                                }
                                            }

                                            // Delete Tier button
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFFF1F2),
                                                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .clickable(enabled = groupTiers.size > 1) {
                                                        if (groupTiers.size > 1) {
                                                            val list = groupTiers.toMutableList()
                                                            list.removeAt(index)
                                                            groupTiers = list
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete tier",
                                                        tint = BrandRose,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Delete",
                                                        color = BrandRose,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add Next Subgroup Tier Box (up to 10 tiers)
                        if (groupTiers.size < 10) {
                            val availableFields = ALL_AVAILABLE_FIELDS.filter { !groupTiers.contains(it.key) }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "+ Add Next Subgroup Tier (Level ${groupTiers.size + 1})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF4F46E5)
                                        )
                                        Text(
                                            text = "${groupTiers.size}/10 tiers used",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Slate500,
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }

                                    // Dropdown selector for field to add
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showAddTierDropdown = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val selectedDef = fieldMap[selectedFieldToAdd]
                                                Text(
                                                    text = selectedDef?.label ?: "Select field to add as next subgroup tier...",
                                                    fontSize = 11.sp,
                                                    color = if (selectedDef != null) Slate900 else Slate400,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = Slate500,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = showAddTierDropdown,
                                            onDismissRequest = { showAddTierDropdown = false },
                                            modifier = Modifier
                                                .background(Color.White)
                                                .widthIn(min = 280.dp)
                                        ) {
                                            availableFields.forEach { field ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(
                                                                text = field.label,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Slate900
                                                            )
                                                            if (field.description.isNotBlank()) {
                                                                Text(
                                                                    text = field.description,
                                                                    fontSize = 10.sp,
                                                                    color = Slate500,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedFieldToAdd = field.key
                                                        showAddTierDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Add Subgroup Tier button
                                    Button(
                                        onClick = {
                                            val keyToAdd = selectedFieldToAdd
                                            if (!keyToAdd.isNullOrBlank() && groupTiers.size < 10 && !groupTiers.contains(keyToAdd)) {
                                                val list = groupTiers.toMutableList()
                                                list.add(keyToAdd)
                                                groupTiers = list
                                                selectedFieldToAdd = null
                                            }
                                        },
                                        enabled = !selectedFieldToAdd.isNullOrBlank() && groupTiers.size < 10,
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFE2E8F0),
                                            contentColor = Color(0xFF475569),
                                            disabledContainerColor = Color(0xFFF1F5F9),
                                            disabledContentColor = Slate400
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+ Add Subgroup Tier",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Slate100)

                    // SECTION 3: RECORD LIST SORTING RULE
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "RECORD LIST SORTING RULE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrandPurple,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = "Controls how garments are sorted within leaf branches and record listings.",
                            fontSize = 11.sp,
                            color = Slate500
                        )

                        Text(
                            text = "Sort Records By Field",
                            fontSize = 11.sp,
                            color = Slate600
                        )

                        // Sort Field Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showSortFieldDropdown = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentLabel = sortFieldOptions.find { it.first == sortField }?.second ?: sortField
                                    Text(
                                        text = currentLabel,
                                        fontSize = 11.sp,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortFieldDropdown,
                                onDismissRequest = { showSortFieldDropdown = false },
                                modifier = Modifier
                                    .background(Color.White)
                                    .widthIn(min = 280.dp)
                            ) {
                                sortFieldOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (sortField == key) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sortField == key) BrandPurple else Slate900
                                            )
                                        },
                                        onClick = {
                                            sortField = key
                                            showSortFieldDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Direction",
                            fontSize = 11.sp,
                            color = Slate600,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        // Sort Direction Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    sortOrder = if (sortOrder == "asc") "desc" else "asc"
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (sortOrder == "asc") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (sortOrder == "asc") "↑ Ascending (A-Z, 0-9)" else "↓ Descending (9-0, Z-A)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Slate100)

                    // SECTION 4: VISIBLE CARD BADGES & METADATA (SINGLE COLUMN, FULL TEXT, NO TRUNCATION)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VISIBLE CARD BADGES & METADATA (${visibleFields.size} SELECTED)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrandPurple,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.weight(1f)
                            )

                            TextButton(
                                onClick = {
                                    if (visibleFields.size == ALL_AVAILABLE_FIELDS.size) {
                                        visibleFields = mutableListOf("Clothes Type")
                                    } else {
                                        visibleFields = ALL_AVAILABLE_FIELDS.map { it.key }.toMutableList()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (visibleFields.size == ALL_AVAILABLE_FIELDS.size) "Deselect All" else "Select All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPurple,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }

                        Text(
                            text = "Selected fields will appear as formatted info badges on garment cards in this preset.",
                            fontSize = 11.sp,
                            color = Slate500
                        )

                        // SINGLE COLUMN METADATA LIST (NO TRUNCATING WORDS)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ALL_AVAILABLE_FIELDS.forEach { field ->
                                val isChecked = visibleFields.contains(field.key)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val list = visibleFields.toMutableList()
                                            if (isChecked) {
                                                if (list.size > 1) list.remove(field.key)
                                            } else {
                                                list.add(field.key)
                                            }
                                            visibleFields = list
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChecked) Color(0xFFF5F3FF) else Color.White,
                                    border = BorderStroke(1.dp, if (isChecked) Color(0xFF8B5CF6) else Slate200)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                val list = visibleFields.toMutableList()
                                                if (checked) {
                                                    list.add(field.key)
                                                } else {
                                                    if (list.size > 1) list.remove(field.key)
                                                }
                                                visibleFields = list
                                            },
                                            modifier = Modifier.size(20.dp),
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = BrandPurple,
                                                uncheckedColor = Slate400
                                            )
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = field.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isChecked) Color(0xFF4C1D95) else Slate800,
                                                softWrap = true
                                            )
                                            if (field.description.isNotBlank()) {
                                                Text(
                                                    text = field.description,
                                                    fontSize = 10.sp,
                                                    color = Slate500,
                                                    lineHeight = 13.sp,
                                                    softWrap = true
                                                )
                                            }
                                        }

                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = BrandPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                HorizontalDivider(thickness = 1.dp, color = Slate200)

                // Footer Actions: Clean, compact buttons anchored at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("Save as New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                saveAndClose()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Secondary actions: Restore Default, Reset All, and Delete Preset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Restore single canonical preset if built-in
                            val isDefaultPreset = DEFAULT_DRILL_PRESETS.any { it.id == preset.id }
                            if (isDefaultPreset && onRestorePreset != null) {
                                OutlinedButton(
                                    onClick = {
                                        onRestorePreset(preset.id)
                                        onClose()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFFD97706)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Restore Default", fontSize = 10.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                }
                            }

                            // Reset all defaults button
                            OutlinedButton(
                                onClick = {
                                    onResetDefaults()
                                    onClose()
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Slate600
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Reset All", fontSize = 10.sp, color = Slate700)
                            }
                        }

                        // Prominent Delete Preset button for ANY preset
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandRose,
                                containerColor = Color(0xFFFFF1F2)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = BrandRose
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Delete Preset", fontSize = 10.sp, color = BrandRose, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Preset",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete preset '${name}'? This cannot be undone.",
                    fontSize = 12.sp,
                    color = Slate600
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeletePreset(preset.id)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Slate600, fontSize = 12.sp)
                }
            }
        )
    }
}
