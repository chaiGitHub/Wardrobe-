package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GarmentEntity
import com.example.util.ImageUtils
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.GarmentDetailDialog
import com.example.ui.drilldown.ALL_AVAILABLE_FIELDS
import com.example.ui.drilldown.DEFAULT_DRILL_PRESETS
import com.example.ui.drilldown.DrillPreset
import com.example.ui.drilldown.DrillRecordCard
import com.example.ui.drilldown.DrillRecordRow
import com.example.ui.drilldown.DrilldownBreadcrumb
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.drilldown.GroupSummary
import com.example.ui.drilldown.PresetEditorModal
import com.example.ui.drilldown.ReportPresetService
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import org.json.JSONArray
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrilldownScreen(
    clothes: List<GarmentEntity>,
    reports: List<ReportEntity> = emptyList(),
    junctions: List<JunctionEntity> = emptyList(),
    onSelectGarment: ((GarmentEntity) -> Unit)? = null,
    onSelectReport: ((ReportEntity) -> Unit)? = null,
    onDeleteGarment: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presetService = remember { ReportPresetService(context) }
    var presets by remember { mutableStateOf(presetService.getPresets()) }
    var activePresetId by remember { mutableStateOf(presetService.getActivePresetId()) }

    val activePreset = remember(presets, activePresetId) {
        presets.find { it.id == activePresetId } ?: presets.firstOrNull() ?: DEFAULT_DRILL_PRESETS.first()
    }

    var breadcrumbs by remember(activePreset.id) { mutableStateOf<List<DrilldownBreadcrumb>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("cards") } // "cards", "split", or "table"
    var selectedSplitGroup by remember(breadcrumbs, activePreset.id) { mutableStateOf<String?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var selectedGarmentForDetail by remember { mutableStateOf<GarmentEntity?>(null) }

    val fieldMap = remember { ALL_AVAILABLE_FIELDS.associateBy { it.key } }

    // Filter clothes by current breadcrumb path
    val branchGarments = remember(clothes, breadcrumbs, junctions) {
        clothes.filter { garment ->
            breadcrumbs.all { crumb ->
                DrilldownUtils.getFieldValue(garment, crumb.field, junctions) == crumb.value
            }
        }
    }

    // Filter by search query
    val searchedGarments = remember(branchGarments, searchQuery) {
        if (searchQuery.isBlank()) {
            branchGarments
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            branchGarments.filter { g ->
                g.name.lowercase(Locale.ROOT).contains(q) ||
                g.clothesType.lowercase(Locale.ROOT).contains(q) ||
                g.material.lowercase(Locale.ROOT).contains(q) ||
                g.lastComment.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    // Sort garments according to active preset sortField & sortOrder
    val sortedGarments = remember(searchedGarments, activePreset.sortField, activePreset.sortOrder, junctions) {
        searchedGarments.sortedWith { a, b ->
            val numA = DrilldownUtils.getNumericalSortValue(a, activePreset.sortField, junctions)
            val numB = DrilldownUtils.getNumericalSortValue(b, activePreset.sortField, junctions)
            val result = if (numA != 0.0 || numB != 0.0) {
                if (activePreset.sortOrder == "asc") numA.compareTo(numB) else numB.compareTo(numA)
            } else {
                val strA = DrilldownUtils.getFieldValue(a, activePreset.sortField, junctions).lowercase(Locale.ROOT)
                val strB = DrilldownUtils.getFieldValue(b, activePreset.sortField, junctions).lowercase(Locale.ROOT)
                if (activePreset.sortOrder == "asc") strA.compareTo(strB) else strB.compareTo(strA)
            }
            result
        }
    }

    val currentDepth = breadcrumbs.size
    val isLeaf = currentDepth >= activePreset.groupTiers.size
    val nextTierField = if (!isLeaf) activePreset.groupTiers[currentDepth] else null

    // Group items for the next hierarchy level if not leaf
    val groupSummaries = remember(branchGarments, nextTierField, junctions) {
        if (nextTierField == null) emptyList()
        else {
            val map = linkedMapOf<String, MutableList<GarmentEntity>>()
            branchGarments.forEach { g ->
                val key = DrilldownUtils.getFieldValue(g, nextTierField, junctions)
                map.getOrPut(key) { mutableListOf() }.add(g)
            }

            map.entries.map { (value, items) ->
                val clean = items.count { !DrilldownUtils.calculateGarmentStats(it.id, junctions).needWash }
                val needWash = items.size - clean
                GroupSummary(
                    field = nextTierField,
                    value = value,
                    count = items.size,
                    cleanCount = clean,
                    needWashCount = needWash,
                    sampleGarments = items.take(4)
                )
            }.sortedByDescending { it.count }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title & Customization button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BrandEmerald),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Drill-Down List Report",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Hierarchical single-branch drill navigation",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandEmerald,
                                modifier = Modifier
                                    .clickable { isEditorOpen = true }
                                    .testTag("customize_preset_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Customize",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Presets Selector Scrollable Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { p ->
                                val isSelected = p.id == activePreset.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        activePresetId = p.id
                                        presetService.setActivePresetId(p.id)
                                        breadcrumbs = emptyList()
                                    },
                                    label = {
                                        Text(
                                            text = if (p.isCustom) "★ ${p.name}" else p.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandPurpleLight,
                                        selectedLabelColor = BrandPurple,
                                        containerColor = Slate100,
                                        labelColor = Slate700
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Tiers Hierarchy Pills Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Tiers:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                            activePreset.groupTiers.forEachIndexed { idx, tierKey ->
                                val label = fieldMap[tierKey]?.label ?: tierKey
                                val isCurrent = idx == currentDepth
                                val isPassed = idx < currentDepth

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPassed) BrandEmerald.copy(alpha = 0.15f)
                                           else if (isCurrent) BrandPurpleLight
                                           else Slate100,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isPassed) BrandEmerald
                                        else if (isCurrent) BrandPurple
                                        else Slate200
                                    )
                                ) {
                                    Text(
                                        text = "L${idx + 1}: $label",
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent || isPassed) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPassed) BrandEmerald
                                                else if (isCurrent) BrandPurple
                                                else Slate600,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                if (idx < activePreset.groupTiers.size - 1) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Slate100)

                        // Sorting Control Row (Full width, clean sizing)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "排序字段与方向:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate100,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                modifier = Modifier.clickable {
                                    val newOrder = if (activePreset.sortOrder == "asc") "desc" else "asc"
                                    val updated = activePreset.copy(sortOrder = newOrder)
                                    presets = presetService.savePreset(updated)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = if (activePreset.sortOrder == "asc") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = BrandEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = (fieldMap[activePreset.sortField]?.label ?: activePreset.sortField),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (activePreset.sortOrder == "asc") "升序 (ASC)" else "降序 (DESC)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate500
                                    )
                                }
                            }
                        }

                        // Drilldown List Presentation Switcher at Bottom of Header Card (instead of right side)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drilldown_list_present_bottom_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    "cards" to "🗂️ 树状卡片 (Tree)",
                                    "split" to "📑 分栏钻取 (Split)",
                                    "table" to "📋 列表呈现 (List)"
                                ).forEach { (mode, label) ->
                                    val isSel = viewMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isSel) BrandEmerald else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewMode = mode }
                                            .testTag("drilldown_present_mode_$mode")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else Slate700,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Breadcrumbs Navigation Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Root Breadcrumb
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (breadcrumbs.isEmpty()) BrandEmerald else Slate100,
                                modifier = Modifier.clickable { breadcrumbs = emptyList() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (breadcrumbs.isEmpty()) Color.White else Slate600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "All Garments (${clothes.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (breadcrumbs.isEmpty()) Color.White else Slate700
                                    )
                                }
                            }

                            // Stepped Breadcrumbs
                            breadcrumbs.forEachIndexed { index, crumb ->
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(14.dp)
                                )

                                val isLast = index == breadcrumbs.size - 1
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isLast) BrandEmerald else Slate100,
                                    modifier = Modifier.clickable {
                                        breadcrumbs = breadcrumbs.take(index + 1)
                                    }
                                ) {
                                    Text(
                                        text = "${fieldMap[crumb.field]?.label ?: crumb.field}: ${crumb.value}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isLast) Color.White else Slate700,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Search Box inside branch
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter current branch (${sortedGarments.size} items)...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = if (searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Slate400,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandEmerald,
                                unfocusedBorderColor = Slate200
                            )
                        )
                    }
                }
            }

            // Hierarchical Groups List (If not leaf)
            if (!isLeaf && viewMode == "split") {
                item {
                    val activeGroupVal = selectedSplitGroup ?: groupSummaries.firstOrNull()?.value
                    val splitGarments = remember(sortedGarments, nextTierField, activeGroupVal, junctions) {
                        if (nextTierField == null || activeGroupVal == null) sortedGarments
                        else sortedGarments.filter { g ->
                            DrilldownUtils.getFieldValue(g, nextTierField, junctions) == activeGroupVal
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left Column: Group Selector (38% width)
                            Column(
                                modifier = Modifier.weight(0.38f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GROUPS (${groupSummaries.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                                groupSummaries.forEach { group ->
                                    val isSel = group.value == activeGroupVal
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) BrandEmerald.copy(alpha = 0.12f) else Slate100,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSel) BrandEmerald else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedSplitGroup = group.value }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = group.value,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) BrandEmerald else Slate800,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${group.count} items",
                                                    fontSize = 9.sp,
                                                    color = Slate500
                                                )
                                                Text(
                                                    text = "Drill →",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandEmerald,
                                                    modifier = Modifier.clickable {
                                                        breadcrumbs = breadcrumbs + DrilldownBreadcrumb(
                                                            tierIndex = currentDepth,
                                                            field = group.field,
                                                            value = group.value
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Right Column: Selected Group Garments (62% width)
                            Column(
                                modifier = Modifier.weight(0.62f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${activeGroupVal ?: "All"} (${splitGarments.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                splitGarments.forEach { garment ->
                                    DrillRecordCard(
                                        garment = garment,
                                        preset = activePreset,
                                        junctions = junctions,
                                        onSelect = {
                                            if (onSelectGarment != null) onSelectGarment(it)
                                            else selectedGarmentForDetail = it
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!isLeaf && viewMode != "table") {
                item {
                    val tierLabel = fieldMap[nextTierField]?.label ?: nextTierField ?: "Group"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Level ${currentDepth + 1}: Group by $tierLabel (${groupSummaries.size} groups)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Tap to drill down",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                items(groupSummaries, key = { it.value }) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("group_card_${group.value}")
                            .clickable {
                                breadcrumbs = breadcrumbs + DrilldownBreadcrumb(
                                    tierIndex = currentDepth,
                                    field = group.field,
                                    value = group.value
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Group Icon Box
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandEmerald.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = BrandEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Group Details & Status
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.value,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BrandEmerald.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${group.count} garments",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (group.needWashCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = BrandAmber.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${group.needWashCount} need wash",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Sample Garment Mini Thumbnails
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((-8).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                group.sampleGarments.take(3).forEach { g ->
                                    val thumbModel = ImageUtils.resolveFirstValidPhoto(g.pictures, context)?.let {
                                        ImageUtils.resolveImageModel(it, context)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Slate200),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (thumbModel != null) {
                                            AsyncImage(
                                                model = thumbModel,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Checkroom,
                                                contentDescription = null,
                                                tint = Slate500,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Drill into group",
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // Leaf Records Listing (Reached end of branch)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Branch Records (${sortedGarments.size} items)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Mode: ${viewMode.uppercase(Locale.ROOT)}",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                if (sortedGarments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No garments found in this branch",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Slate700
                                )
                                Text(
                                    text = "Try adjusting your search query or navigating up a tier",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                } else if (viewMode == "table") {
                    // Render Table Rows
                    items(sortedGarments, key = { it.id }) { garment ->
                        DrillRecordRow(
                            garment = garment,
                            preset = activePreset,
                            junctions = junctions,
                            onSelect = {
                                if (onSelectGarment != null) onSelectGarment(it)
                                else selectedGarmentForDetail = it
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    // Render Cards Grid / Column
                    items(sortedGarments, key = { it.id }) { garment ->
                        DrillRecordCard(
                            garment = garment,
                            preset = activePreset,
                            junctions = junctions,
                            onSelect = {
                                if (onSelectGarment != null) onSelectGarment(it)
                                else selectedGarmentForDetail = it
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item {
                // Bottom List Presentation Toggle Card (Positioned at the bottom of the drilldown list)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                text = "钻取列表呈现方式 (Drilldown List Presentation)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = "共 ${sortedGarments.size} 件",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandEmerald
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "cards" to "卡片呈现 (Cards)",
                                "split" to "分栏呈现 (Split)",
                                "table" to "紧凑列表 (Table)"
                            ).forEach { (mode, label) ->
                                val isSel = viewMode == mode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) BrandEmerald else Slate100,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) BrandEmerald else Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewMode = mode }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Preset Editor Modal
    if (isEditorOpen) {
        PresetEditorModal(
            isOpen = isEditorOpen,
            preset = activePreset,
            onClose = { isEditorOpen = false },
            onSavePreset = { updated ->
                presets = presetService.savePreset(updated)
            },
            onSaveAsNew = { newPreset ->
                presets = presetService.savePreset(newPreset)
                activePresetId = newPreset.id
                presetService.setActivePresetId(newPreset.id)
                breadcrumbs = emptyList()
            },
            onDeletePreset = { idToDelete ->
                presets = presetService.deletePreset(idToDelete)
                activePresetId = presets.first().id
                presetService.setActivePresetId(presets.first().id)
                breadcrumbs = emptyList()
            },
            onResetDefaults = {
                presets = presetService.resetToDefaults()
                activePresetId = presets.first().id
                presetService.setActivePresetId(presets.first().id)
                breadcrumbs = emptyList()
            }
        )
    }

    // Garment Detail Dialog
    selectedGarmentForDetail?.let { garment ->
        GarmentDetailDialog(
            garment = garment,
            onDismiss = { selectedGarmentForDetail = null },
            onDelete = { id ->
                onDeleteGarment?.invoke(id)
                selectedGarmentForDetail = null
            }
        )
    }
}
