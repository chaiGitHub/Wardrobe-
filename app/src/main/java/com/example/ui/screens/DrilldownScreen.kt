package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.drilldown.ALL_AVAILABLE_FIELDS
import com.example.ui.drilldown.DEFAULT_DRILL_PRESETS
import com.example.ui.drilldown.DrillPreset
import com.example.ui.drilldown.ReportFieldDef
import com.example.ui.drilldown.DrillRecordCard
import com.example.ui.drilldown.DrillRecordRow
import com.example.ui.drilldown.DrilldownBreadcrumb
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.drilldown.GroupSummary
import com.example.ui.drilldown.PresetEditorModal
import com.example.ui.drilldown.ReportPresetService
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Layers

import com.example.ui.dialogs.GarmentDetailDialog
import com.example.ui.dialogs.SessionDetailModal
import com.example.ui.drilldown.GarmentStats
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.util.ImageUtils
import java.util.Locale

enum class DrilldownSortOption(val id: String, val label: String) {
    LATEST_WEAR("latest", "最新穿着"),
    WEAR_COUNT("count", "穿着次数"),
    UNWASHED_HOURS("unwashed", "待洗时长"),
    TOTAL_HOURS("total_hours", "累计穿着时长"),
    NAME("name", "名称")
}

private fun extractDrillTemp(r: ReportEntity?): String {
    if (r == null) return "22°C"
    val match = Regex("(\\d{1,2})\\s*°?C", RegexOption.IGNORE_CASE).find(r.reportDetail)
    if (match != null) return "${match.groupValues[1]}°C"
    return if (r.sweat.contains("sweat", ignoreCase = true) && !r.sweat.contains("not", ignoreCase = true)) "26°C" else "20°C"
}

private fun extractDrillOccasion(r: ReportEntity?): String {
    if (r == null) return "Home"
    val n = r.name.lowercase(Locale.US)
    return when {
        n.contains("work") || n.contains("office") -> "Work"
        n.contains("home") || n.contains("casual") -> "Home"
        n.contains("out") || n.contains("walk") -> "Outdoor"
        n.contains("social") || n.contains("dinner") -> "Social"
        else -> "Home"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrilldownScreen(
    clothes: List<GarmentEntity>,
    reports: List<ReportEntity> = emptyList(),
    junctions: List<JunctionEntity> = emptyList(),
    onSelectGarment: ((GarmentEntity) -> Unit)? = null,
    onSelectReport: ((ReportEntity) -> Unit)? = null,
    onDeleteGarment: ((String) -> Unit)? = null,
    viewModel: com.example.ui.WardrobeViewModel? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf("garment_to_report") } // "garment_to_report" or "report_to_garment"
    var sortOption by rememberSaveable { mutableStateOf(DrilldownSortOption.LATEST_WEAR) }
    var viewMode by rememberSaveable { mutableStateOf("table") } // "table" (Dense List, default), "cards" (Grid)
    var presetDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val presetService = remember { ReportPresetService(context) }
    var presets by remember { mutableStateOf(presetService.getPresets()) }
    var activePresetId by rememberSaveable { mutableStateOf(presetService.getActivePresetId()) }

    // Synchronize presets with repository / restore updates
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val latest = presetService.getPresets()
        presets = latest
    }
    androidx.compose.runtime.LaunchedEffect(clothes.size, reports.size) {
        val latest = presetService.getPresets()
        presets = latest
    }

    val currentPreset = remember(presets, activePresetId) {
        presets.find { it.id == activePresetId } ?: presets.firstOrNull() ?: DEFAULT_DRILL_PRESETS.first()
    }
    var showPresetEditor by rememberSaveable { mutableStateOf(false) }

    val vmDrillPath = viewModel?.drillPath?.collectAsState()?.value
    var localDrillPath by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.listSaver(
            save = { list -> list.map { "${it.tierIndex}|||${it.field}|||${it.value}" } },
            restore = { strList ->
                strList.mapNotNull {
                    val parts = it.split("|||")
                    if (parts.size == 3) DrilldownBreadcrumb(parts[0].toIntOrNull() ?: 0, parts[1], parts[2]) else null
                }
            }
        )
    ) { mutableStateOf<List<DrilldownBreadcrumb>>(emptyList()) }

    val drillPath = if (viewModel != null) (vmDrillPath ?: emptyList()) else localDrillPath
    val updateDrillPath: (List<DrilldownBreadcrumb>) -> Unit = { newPath ->
        if (viewModel != null) {
            viewModel.setDrillPath(newPath)
        }
        localDrillPath = newPath
    }

    var branchSearchQuery by remember { mutableStateOf("") }
    val fieldMap = remember { ALL_AVAILABLE_FIELDS.associateBy { it.key } }



    val expandedIds = remember { mutableStateMapOf<String, Boolean>() }
    var selectedSplitGarmentId by remember { mutableStateOf<String?>(null) }
    var selectedSplitReportId by remember { mutableStateOf<String?>(null) }

    var selectedGarmentForDetail by remember { mutableStateOf<GarmentEntity?>(null) }
    var selectedReportForDetail by remember { mutableStateOf<ReportEntity?>(null) }

    val reportsById = remember(reports) { reports.associateBy { it.id } }
    val clothesById = remember(clothes) { clothes.associateBy { it.id } }
    val junctionsByGarmentId = remember(junctions) { junctions.groupBy { it.clothesId } }
    val junctionsByReportId = remember(junctions) { junctions.groupBy { it.wearingReportId } }

    val statsById = remember(clothes, junctions) {
        clothes.associate { g -> g.id to DrilldownUtils.calculateGarmentStats(g.id, junctions) }
    }

    // Current branch garments matching all tiers selected in drillPath
    val currentBranchGarments = remember(clothes, drillPath, junctions) {
        clothes.filter { g ->
            drillPath.all { bc ->
                val v = DrilldownUtils.getFieldValue(g, bc.field, junctions)
                v.equals(bc.value, ignoreCase = true)
            }
        }
    }

    // Next tier subgroups if not at leaf level
    val currentTierIndex = drillPath.size
    val currentTierField = if (currentTierIndex < currentPreset.groupTiers.size) currentPreset.groupTiers[currentTierIndex] else ""
    val currentTierFieldDef = fieldMap[currentTierField]
    val currentTierLabel = currentTierFieldDef?.label ?: currentTierField

    val subgroups = remember(currentBranchGarments, currentTierField, junctions) {
        if (currentTierField.isBlank()) emptyList()
        else {
            currentBranchGarments.groupBy { g ->
                DrilldownUtils.getFieldValue(g, currentTierField, junctions)
            }.map { (v, gList) ->
                val needWash = gList.count { g ->
                    DrilldownUtils.calculateGarmentStats(g.id, junctions).needWash
                }
                GroupSummary(
                    field = currentTierField,
                    value = v,
                    count = gList.size,
                    cleanCount = gList.size - needWash,
                    needWashCount = needWash,
                    sampleGarments = gList.take(3)
                )
            }.sortedByDescending { it.count }
        }
    }

    // Sorted garments in the current branch
    val sortedBranchGarments = remember(currentBranchGarments, branchSearchQuery, currentPreset, junctions) {
        val q = branchSearchQuery.trim().lowercase(Locale.ROOT)
        val filtered = if (q.isEmpty()) currentBranchGarments else {
            currentBranchGarments.filter { g ->
                val gJuncs = junctionsByGarmentId[g.id].orEmpty()
                val reportMatch = gJuncs.any { j ->
                    val r = reportsById[j.wearingReportId]
                    (r?.name?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                        (r?.reportDetail?.lowercase(Locale.ROOT)?.contains(q) == true)
                }
                g.name.lowercase(Locale.ROOT).contains(q) ||
                    g.clothesType.lowercase(Locale.ROOT).contains(q) ||
                    g.material.lowercase(Locale.ROOT).contains(q) ||
                    g.lastComment.lowercase(Locale.ROOT).contains(q) ||
                    reportMatch
            }
        }
        if (currentPreset.sortOrder.equals("asc", ignoreCase = true)) {
            filtered.sortedWith(
                compareBy<GarmentEntity> { DrilldownUtils.getNumericalSortValue(it, currentPreset.sortField, junctions) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
        } else {
            filtered.sortedWith(
                compareByDescending<GarmentEntity> { DrilldownUtils.getNumericalSortValue(it, currentPreset.sortField, junctions) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
        }
    }

    // Latest wear date per garment
    val latestWearDateById = remember(clothes, junctionsByGarmentId, reportsById) {
        clothes.associate { g ->
            val gJuncs = junctionsByGarmentId[g.id].orEmpty()
            val latest = gJuncs.mapNotNull { j ->
                reportsById[j.wearingReportId]?.wearDate?.takeIf { it.isNotBlank() }
                    ?: j.dateCreated.take(10).takeIf { it.isNotBlank() }
            }.maxOrNull() ?: ""
            g.id to latest
        }
    }

    // Filtered & Sorted Garments
    val sortedGarments = remember(clothes, searchQuery, sortOption, statsById, latestWearDateById, junctionsByGarmentId, reportsById) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        val filtered = if (q.isEmpty()) {
            clothes
        } else {
            clothes.filter { g ->
                val gJuncs = junctionsByGarmentId[g.id].orEmpty()
                val reportMatch = gJuncs.any { j ->
                    val r = reportsById[j.wearingReportId]
                    (r?.name?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                        (r?.reportDetail?.lowercase(Locale.ROOT)?.contains(q) == true)
                }
                g.name.lowercase(Locale.ROOT).contains(q) ||
                    g.clothesType.lowercase(Locale.ROOT).contains(q) ||
                    g.material.lowercase(Locale.ROOT).contains(q) ||
                    g.lastComment.lowercase(Locale.ROOT).contains(q) ||
                    reportMatch
            }
        }

        when (sortOption) {
            DrilldownSortOption.LATEST_WEAR -> filtered.sortedWith(
                compareByDescending<GarmentEntity> { latestWearDateById[it.id] ?: "" }
                    .thenByDescending { statsById[it.id]?.totalWears ?: 0 }
            )
            DrilldownSortOption.WEAR_COUNT -> filtered.sortedByDescending { statsById[it.id]?.totalWears ?: 0 }
            DrilldownSortOption.UNWASHED_HOURS -> filtered.sortedByDescending { statsById[it.id]?.unwashedSeconds ?: 0L }
            DrilldownSortOption.TOTAL_HOURS -> filtered.sortedByDescending { statsById[it.id]?.totalDurationSeconds ?: 0L }
            DrilldownSortOption.NAME -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    // Filtered & Sorted Reports (for Report -> Garments direction)
    val sortedReports = remember(reports, searchQuery, sortOption, junctionsByReportId, clothesById) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        val filtered = if (q.isEmpty()) {
            reports
        } else {
            reports.filter { r ->
                val rJuncs = junctionsByReportId[r.id].orEmpty()
                val garmentMatch = rJuncs.any { j ->
                    val g = clothesById[j.clothesId]
                    (g?.name ?: j.clothesTitle).lowercase(Locale.ROOT).contains(q)
                }
                r.name.lowercase(Locale.ROOT).contains(q) ||
                    r.wearDate.lowercase(Locale.ROOT).contains(q) ||
                    r.reportDetail.lowercase(Locale.ROOT).contains(q) ||
                    garmentMatch
            }
        }
        filtered.sortedByDescending { it.wearDate }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenWidth = maxWidth
        val isTablet = screenWidth >= 600.dp && screenWidth < 960.dp
        val isLaptop = screenWidth >= 960.dp
        val isWide = isTablet || isLaptop

        val subgroupColumns = when {
            screenWidth >= 1200.dp -> 3
            screenWidth >= 640.dp -> 2
            else -> 1
        }
        val recordColumns = when {
            screenWidth >= 1400.dp -> 4
            screenWidth >= 1000.dp -> 3
            screenWidth >= 640.dp -> 2
            else -> 1
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1440.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (isWide) 20.dp else 10.dp,
                end = if (isWide) 20.dp else 10.dp,
                top = 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. COMPACT HEADER & CONTROLS CARD (IMG_9979.png)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title Block
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "ANALYTICS & CATEGORIZATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = BrandEmerald,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Drill-Down List Report",
                                    fontSize = if (isWide) 20.sp else 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                            }
                        }
                        Text(
                            text = "Hierarchical single-branch drill navigation. Selecting a subgroup option immediately filters and lists branch records.",
                            fontSize = 11.sp,
                            color = Slate500,
                            lineHeight = 15.sp
                        )

                        // Preset Dropdown Selector (Full width, elegant button with ChevronDown)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { presetDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentPreset.name + if (currentPreset.isCustom) " (Custom)" else "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Preset",
                                        tint = Slate500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = presetDropdownExpanded,
                                onDismissRequest = { presetDropdownExpanded = false },
                                modifier = Modifier
                                    .background(Color.White)
                                    .widthIn(min = 280.dp)
                            ) {
                                presets.forEach { p ->
                                    val isSel = p.id == activePresetId
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = p.name + if (p.isCustom) " (Custom)" else "",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) BrandEmerald else Slate900
                                                )
                                                if (isSel) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = BrandEmerald,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            activePresetId = p.id
                                            presetService.setActivePresetId(p.id)
                                            updateDrillPath(emptyList())
                                            presetDropdownExpanded = false
                                        }
                                    )
                                }

                                HorizontalDivider(color = Slate200)

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WbSunny,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "☀️ 恢复并切换: 无空调上身体感 (Hot feeling)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                                Text(
                                                    text = "L1: when without aircond: hot feeling > L2: Clothes Type",
                                                    fontSize = 10.sp,
                                                    color = Slate500
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        val restored = presetService.restoreNoAircondHotPreset()
                                        presets = restored
                                        activePresetId = "preset-without-aircond-hot"
                                        presetService.setActivePresetId("preset-without-aircond-hot")
                                        updateDrillPath(emptyList())
                                        presetDropdownExpanded = false
                                        android.widget.Toast.makeText(context, "已恢复并切换至: 无空调上身体感预设", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestartAlt,
                                                contentDescription = null,
                                                tint = Color(0xFF7C3AED),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "🔄 恢复/重置全部标准预设",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6D28D9)
                                                )
                                                Text(
                                                    text = "含无空调体感、空调体感、睡眠等完整13预设",
                                                    fontSize = 10.sp,
                                                    color = Slate500
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        val restored = presetService.resetToDefaults()
                                        presets = restored
                                        val target = restored.find { it.id == "preset-without-aircond-hot" } ?: restored.first()
                                        activePresetId = target.id
                                        presetService.setActivePresetId(target.id)
                                        updateDrillPath(emptyList())
                                        presetDropdownExpanded = false
                                        android.widget.Toast.makeText(context, "已恢复全部13个标准预设（已切换至无空调体感预设）", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // Quick Preset Selection & 1-Tap Restoration Bar (Always accessible, never lost)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        ) {
                            // 1. 无空调: 上身体感 (Hot feeling)
                            val isNoAircondHot = currentPreset.id == "preset-without-aircond-hot"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isNoAircondHot) Color(0xFFFEF3C7) else Color.White,
                                border = BorderStroke(1.dp, if (isNoAircondHot) Color(0xFFD97706) else Slate200),
                                modifier = Modifier.clickable {
                                    val restored = presetService.restoreNoAircondHotPreset()
                                    presets = restored
                                    activePresetId = "preset-without-aircond-hot"
                                    presetService.setActivePresetId("preset-without-aircond-hot")
                                    updateDrillPath(emptyList())
                                    android.widget.Toast.makeText(context, "已切换至: 无空调上身体感 (Hot feeling)", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "☀️ 无空调: 上身体感",
                                        fontSize = 11.sp,
                                        fontWeight = if (isNoAircondHot) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isNoAircondHot) Color(0xFFB45309) else Slate700
                                    )
                                }
                            }

                            // 2. 无空调: 腿部体感 (Leg feeling)
                            val isNoAircondLeg = currentPreset.id == "preset-without-aircond-leg"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isNoAircondLeg) Color(0xFFFEF3C7) else Color.White,
                                border = BorderStroke(1.dp, if (isNoAircondLeg) Color(0xFFD97706) else Slate200),
                                modifier = Modifier.clickable {
                                    val target = presets.find { it.id == "preset-without-aircond-leg" }
                                    if (target != null) {
                                        activePresetId = target.id
                                        presetService.setActivePresetId(target.id)
                                        updateDrillPath(emptyList())
                                    }
                                }
                            ) {
                                Text(
                                    text = "☀️ 无空调: 腿部体感",
                                    fontSize = 11.sp,
                                    fontWeight = if (isNoAircondLeg) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNoAircondLeg) Color(0xFFB45309) else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            // 3. 空调: 上身
                            val isAircondHot = currentPreset.id == "preset-aircond-hot"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAircondHot) Color(0xFFE0F2FE) else Color.White,
                                border = BorderStroke(1.dp, if (isAircondHot) Color(0xFF0284C7) else Slate200),
                                modifier = Modifier.clickable {
                                    val target = presets.find { it.id == "preset-aircond-hot" }
                                    if (target != null) {
                                        activePresetId = target.id
                                        presetService.setActivePresetId(target.id)
                                        updateDrillPath(emptyList())
                                    }
                                }
                            ) {
                                Text(
                                    text = "❄️ 空调: 上身",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAircondHot) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAircondHot) Color(0xFF0369A1) else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            // 4. 空调: 腿部
                            val isAircondLeg = currentPreset.id == "preset-aircond-leg"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAircondLeg) Color(0xFFE0F2FE) else Color.White,
                                border = BorderStroke(1.dp, if (isAircondLeg) Color(0xFF0284C7) else Slate200),
                                modifier = Modifier.clickable {
                                    val target = presets.find { it.id == "preset-aircond-leg" }
                                    if (target != null) {
                                        activePresetId = target.id
                                        presetService.setActivePresetId(target.id)
                                        updateDrillPath(emptyList())
                                    }
                                }
                            ) {
                                Text(
                                    text = "❄️ 空调: 腿部",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAircondLeg) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAircondLeg) Color(0xFF0369A1) else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }

                            // 5. 一键恢复全部预设 (Restore All)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF3E8FF),
                                border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                modifier = Modifier.clickable {
                                    val restored = presetService.resetToDefaults()
                                    presets = restored
                                    activePresetId = "preset-without-aircond-hot"
                                    presetService.setActivePresetId("preset-without-aircond-hot")
                                    updateDrillPath(emptyList())
                                    android.widget.Toast.makeText(context, "已恢复全部13个默认预设并激活无空调(Hot feeling)预设", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "🔄 恢复默认预设",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D28D9)
                                    )
                                }
                            }
                        }

                        // Customize Preset Button (Full-width emerald button)
                        Button(
                            onClick = { showPresetEditor = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Customize Preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Tiers representation: L1 > L2 > L3... wrapped dynamically across rows with little space between them
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TIERS:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Slate400,
                                modifier = Modifier.padding(end = 2.dp, top = 3.dp)
                            )
                            currentPreset.groupTiers.forEachIndexed { idx, tKey ->
                                val tDef = fieldMap[tKey]
                                val isPassed = idx < drillPath.size
                                val isCurrent = idx == drillPath.size
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPassed) Color(0xFFD1FAE5) else if (isCurrent) Color(0xFFECFDF5) else Slate100,
                                        border = BorderStroke(1.dp, if (isPassed) Color(0xFF6EE7B7) else if (isCurrent) Color(0xFFA7F3D0) else Slate200)
                                    ) {
                                        Text(
                                            text = "L${idx + 1}: ${tDef?.label ?: tKey}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isPassed || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isPassed) Color(0xFF065F46) else if (isCurrent) Color(0xFF047857) else Slate600,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (idx < currentPreset.groupTiers.size - 1) {
                                        Text(">", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Controls Toolbar: Sort button on left + View switch on right (IMG_9979.png)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Sort Order Button: [ ↑ (ASC) ] / [ ↓ (DESC) ]
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.clickable {
                                    val newOrder = if (currentPreset.sortOrder.equals("asc", ignoreCase = true)) "desc" else "asc"
                                    val updated = currentPreset.copy(sortOrder = newOrder)
                                    presets = presetService.savePreset(updated)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (currentPreset.sortOrder == "asc") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = BrandEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "(${currentPreset.sortOrder.uppercase(Locale.US)})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800
                                    )
                                }
                            }

                            // View Switch: [ ≡  ⊞ ] (Dense List vs Grid Cards)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier.padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Table / Dense List option
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (viewMode == "table") BrandEmerald else Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewMode = "table" }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TableRows,
                                                contentDescription = "List View",
                                                tint = if (viewMode == "table") Color.White else Slate600,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Grid Cards option
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (viewMode == "cards") BrandEmerald else Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewMode = "cards" }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GridView,
                                                contentDescription = "Grid View",
                                                tint = if (viewMode == "cards") Color.White else Slate600,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. BREADCRUMB & ACTIVE LEAF BANNER (IMG_9979.png)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Middle Path (Breadcrumbs): all clothes (32) > Clothes Type: Dress > wet: dry > ...
                        // Dynamically wraps to next row if not enough width, matching top_drill_fix_a201c6ba.PNG
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Root breadcrumb: all clothes (32)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (drillPath.isEmpty()) BrandEmerald else Slate100,
                                border = BorderStroke(1.dp, if (drillPath.isEmpty()) BrandEmerald else Slate200),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { updateDrillPath(emptyList()) }
                            ) {
                                Text(
                                    text = "all clothes (${clothes.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (drillPath.isEmpty()) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (drillPath.isEmpty()) Color.White else Slate800,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Dynamic drill steps
                            drillPath.forEachIndexed { i, bc ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = ">",
                                        fontSize = 12.sp,
                                        color = Slate400,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val fDef = fieldMap[bc.field]
                                    val isLast = i == drillPath.size - 1
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isLast) BrandEmerald else Slate100,
                                        border = BorderStroke(1.dp, if (isLast) BrandEmerald else Slate200),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { updateDrillPath(drillPath.take(i + 1)) }
                                    ) {
                                        Text(
                                            text = "${fDef?.label ?: bc.field}: ${bc.value}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isLast) Color.White else Slate800,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Trailing Reset button
                            if (drillPath.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Slate100,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { updateDrillPath(emptyList()) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Reset",
                                            tint = Slate600,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Reset",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate700
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SUBGROUPS SELECTION (IMG_9987.png)
            if (subgroups.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = BrandEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$currentTierLabel:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Text(
                                text = "Tier ${drillPath.size + 1} of ${currentPreset.groupTiers.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate600,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Subgroup option cards (IMG_9987 style: full-width mint cards with count badge on right)
                items(subgroups, key = { "sg_${it.field}_${it.value}" }) { sg ->
                    val isUndefined = sg.value.equals("undefined", ignoreCase = true) || sg.value.isBlank()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isUndefined) Color(0xFFF8FAFC) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, if (isUndefined) Slate200 else Color(0xFFA7F3D0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                updateDrillPath(drillPath + DrilldownBreadcrumb(drillPath.size, sg.field, sg.value))
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUndefined) "undefined" else sg.value,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUndefined) Slate500 else Color(0xFF064E3B),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUndefined) Color(0xFFE2E8F0) else Color(0xFFD1FAE5),
                                border = BorderStroke(0.5.dp, if (isUndefined) Color(0xFFCBD5E1) else Color(0xFF6EE7B7))
                            ) {
                                Text(
                                    text = "${sg.count}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUndefined) Slate700 else Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. BRANCH RECORDS SECTION (IMG_9987.png)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortDef = fieldMap[currentPreset.sortField]
                    val sortLabel = sortDef?.label ?: currentPreset.sortField
                    val sortOrderText = currentPreset.sortOrder.lowercase(Locale.US)

                    // Header: Branch Records (32) · Sorted by Name (asc)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Branch Records (${currentBranchGarments.size}) · Sorted by $sortLabel ($sortOrderText)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                    }

                    // Search Input: Filter records in this branch...
                    OutlinedTextField(
                        value = branchSearchQuery,
                        onValueChange = { branchSearchQuery = it },
                        placeholder = {
                            Text("Filter records in this branch...", fontSize = 12.sp, color = Slate400)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = if (branchSearchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { branchSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(14.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandEmerald,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                    )
                }
            }

            // Records Presentation (Table as default, Cards on toggle)
            if (viewMode == "cards") {
                if (recordColumns == 1) {
                    items(sortedBranchGarments, key = { "branch_card_${it.id}" }) { g ->
                        DrillRecordCard(
                            garment = g,
                            preset = currentPreset,
                            junctions = junctions,
                            onSelect = { onSelectGarment?.invoke(it) ?: run { selectedGarmentForDetail = it } }
                        )
                    }
                } else {
                    items(sortedBranchGarments.chunked(recordColumns), key = { row -> "rec_row_${row.first().id}" }) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            rowItems.forEach { g ->
                                DrillRecordCard(
                                    garment = g,
                                    preset = currentPreset,
                                    junctions = junctions,
                                    onSelect = { onSelectGarment?.invoke(it) ?: run { selectedGarmentForDetail = it } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(recordColumns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // "table" / Dense List view (Default) matching IMG_9987.png
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Table Header Row: CLOTHES (FULL NAME) | WASH STATUS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CLOTHES (FULL NAME)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                                Text(
                                    text = "WASH STATUS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                            }
                            HorizontalDivider(color = Slate200)

                            if (sortedBranchGarments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No garments match the current branch filter.",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                }
                            } else {
                                sortedBranchGarments.forEachIndexed { idx, g ->
                                    val gStats = statsById[g.id] ?: DrilldownUtils.calculateGarmentStats(g.id, junctions)
                                    val resolvedPhoto = remember(g.pictures) {
                                        ImageUtils.resolveFirstValidPhoto(g.pictures, context)?.let {
                                            ImageUtils.resolveImageModel(it, context)
                                        }
                                    }
                                    val isNeedWash = gStats.needWash

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (onSelectGarment != null) onSelectGarment(g)
                                                else selectedGarmentForDetail = g
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Thumbnail + Name + Clothes Type & Material Subtitle
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Slate100),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (resolvedPhoto != null) {
                                                    AsyncImage(
                                                        model = resolvedPhoto,
                                                        contentDescription = g.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Checkroom,
                                                        contentDescription = null,
                                                        tint = Slate400,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = g.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val subParts = buildList {
                                                    if (g.clothesType.isNotBlank()) add("Clothes Type: ${g.clothesType}")
                                                    if (g.material.isNotBlank()) add("Material: ${g.material}")
                                                }
                                                if (subParts.isNotEmpty()) {
                                                    Text(
                                                        text = subParts.joinToString(" · "),
                                                        fontSize = 11.sp,
                                                        color = Slate500,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Right: Wash Status Pill Badge
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isNeedWash) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, if (isNeedWash) Color(0xFFFECACA) else Color(0xFFA7F3D0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isNeedWash) Icons.Default.Warning else Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (isNeedWash) Color(0xFFDC2626) else Color(0xFF059669),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isNeedWash) "Need Wash" else "Clean",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isNeedWash) Color(0xFFB91C1C) else Color(0xFF047857)
                                                )
                                            }
                                        }
                                    }

                                    if (idx < sortedBranchGarments.size - 1) {
                                        HorizontalDivider(color = Slate100)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Preset Editor Modal
        PresetEditorModal(
            isOpen = showPresetEditor,
            preset = currentPreset,
            onClose = { showPresetEditor = false },
            onSavePreset = { updated ->
                val updatedList = presetService.savePreset(updated)
                presets = updatedList
                activePresetId = updated.id
                showPresetEditor = false
            },
            onSaveAsNew = { newPreset ->
                val updatedList = presetService.savePreset(newPreset)
                presets = updatedList
                activePresetId = newPreset.id
                showPresetEditor = false
            },
            onDeletePreset = { delId ->
                val updatedList = presetService.deletePreset(delId)
                presets = updatedList
                activePresetId = presets.firstOrNull()?.id ?: DEFAULT_DRILL_PRESETS.first().id
                showPresetEditor = false
            },
            onResetDefaults = {
                val updatedList = presetService.resetToDefaults()
                presets = updatedList
                activePresetId = updatedList.first().id
                showPresetEditor = false
            }
        )

        // Fallback Garment Detail Modal
        selectedGarmentForDetail?.let { garment ->
            GarmentDetailDialog(
                garment = garment,
                existingClothes = clothes,
                junctions = junctions,
                reports = reports,
                onDismiss = { selectedGarmentForDetail = null },
                onDelete = { id ->
                    onDeleteGarment?.invoke(id)
                    selectedGarmentForDetail = null
                },
                onSaveGarment = { updated ->
                    selectedGarmentForDetail = updated
                }
            )
        }

        // Report Detail Modal when clicking a report inside drilldown
        selectedReportForDetail?.let { report ->
            val rJuncs = junctionsByReportId[report.id].orEmpty()
            SessionDetailModal(
                isOpen = true,
                report = report,
                junctions = rJuncs,
                clothes = clothes,
                onClose = { selectedReportForDetail = null },
                onEditReport = { selectedReportForDetail = null },
                onDeleteReport = { selectedReportForDetail = null },
                onUpdateWashStatus = { _, _ -> },
                onSelectGarment = { g ->
                    selectedReportForDetail = null
                    if (onSelectGarment != null) onSelectGarment(g)
                    else selectedGarmentForDetail = g
                }
            )
        }
    }
}

@Composable
private fun DrilldownSearchField(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = {
            Text(
                text = "搜索衣物名称、分类、材质、报告标题或感受备注...",
                fontSize = 12.sp,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
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
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else null,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPurple,
            unfocusedBorderColor = Slate200,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8FAFC)
        )
    )
}

@Composable
private fun DrilldownDirectionToggle(
    direction: String,
    onDirectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate100,
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isGarmentFirst = direction == "garment_to_report"
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (isGarmentFirst) Slate900 else Color.Transparent,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onDirectionChange("garment_to_report") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = if (isGarmentFirst) Color.White else Slate600,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "衣物 → 穿搭报告 (Garment → Reports)",
                        fontSize = 11.sp,
                        fontWeight = if (isGarmentFirst) FontWeight.Bold else FontWeight.Medium,
                        color = if (isGarmentFirst) Color.White else Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val isReportFirst = direction == "report_to_garment"
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (isReportFirst) Slate900 else Color.Transparent,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onDirectionChange("report_to_garment") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isReportFirst) Color.White else Slate600,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "穿搭报告 → 衣物 (Report → Garments)",
                        fontSize = 11.sp,
                        fontWeight = if (isReportFirst) FontWeight.Bold else FontWeight.Medium,
                        color = if (isReportFirst) Color.White else Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-225451.png & Screenshot_20261001-225454.png:
 * Expandable Tree Card for Garment -> Reports
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrilldownGarmentTreeCard(
    garment: GarmentEntity,
    stats: GarmentStats,
    latestWearDate: String,
    junctions: List<JunctionEntity>,
    reportsById: Map<String, ReportEntity>,
    isExpanded: Boolean,
    isWide: Boolean,
    onToggleExpand: () -> Unit,
    onOpenGarmentDetail: () -> Unit,
    onOpenReportDetail: (ReportEntity) -> Unit
) {
    val context = LocalContext.current
    val resolvedPhoto = remember(garment.pictures) {
        ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
            ImageUtils.resolveImageModel(it, context)
        }
    }
    val totalWashes = garment.wearingFrequency + garment.trackedAppWashes
    val unwashedCount = junctions.count { it.washStated.equals("need wash", ignoreCase = true) }
    val avgHours = if (stats.totalWears > 0) {
        String.format(Locale.US, "%.1fh", (stats.totalDurationSeconds / 3600.0) / stats.totalWears)
    } else "0.0h"
    val cpw = if (garment.price > 0 && totalWashes > 0) {
        String.format(Locale.US, "$%.2f/洗", garment.price / totalWashes)
    } else "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isExpanded) Color(0xFFD8B4FE) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Always Visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resolvedPhoto != null) {
                            AsyncImage(
                                model = resolvedPhoto,
                                contentDescription = garment.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = garment.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = garment.clothesType.ifBlank { "Tops" },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (garment.price > 0) {
                                Text(
                                    text = "$${garment.price.toInt()}",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (stats.needWash) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (stats.needWash) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (stats.needWash) Color(0xFFD97706) else Color(0xFF059669),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = if (stats.needWash) "Dirty (${stats.unwashedHours}h)" else "Clean",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stats.needWash) Color(0xFFB45309) else Color(0xFF047857)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Summary Metrics + Expand Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stats.totalWears} 次穿着",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate800
                        )
                        Text(
                            text = "最近: ${latestWearDate.ifBlank { "—" }}",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stats.totalHours}h",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (stats.needWash) Color(0xFFD97706) else Color(0xFF059669)
                        )
                        Text(
                            text = "待洗: ${stats.unwashedHours}h",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isExpanded) BrandPurpleLight else Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isExpanded) BrandPurple else Slate600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded Detail Section (Screenshot 225454)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFC))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Slate200)

                    // 4 Stat Boxes (4-col on Tablet/Laptop, 2x2 on Mobile)
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TreeStatBox("穿着次数 (WEAR COUNT)", "${stats.totalWears} 次", "累计洗涤 $totalWashes 次", Color(0xFFF8FAFC), Slate200, Slate900, Modifier.weight(1f))
                            TreeStatBox("累计时长 (TOTAL HOURS)", "${stats.totalHours}h", "平均每次 $avgHours", Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF047857), Modifier.weight(1f))
                            TreeStatBox("待洗累计 (UNWASHED)", "${stats.unwashedHours}h", "$unwashedCount 条待洗记录", if (stats.needWash) Color(0xFFFFFBEB) else Color(0xFFF0FDF4), if (stats.needWash) Color(0xFFFDE68A) else Color(0xFFBBF7D0), if (stats.needWash) Color(0xFFB45309) else Color(0xFF047857), Modifier.weight(1f))
                            TreeStatBox("关联记录 (JUNCTIONS)", "${junctions.size} 条", "单价成本 $cpw", Color(0xFFFAF5FF), Color(0xFFE9D5FF), BrandPurple, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                TreeStatBox("穿着次数 (WEAR COUNT)", "${stats.totalWears} 次", "累计洗涤 $totalWashes 次", Color(0xFFF8FAFC), Slate200, Slate900, Modifier.weight(1f))
                                TreeStatBox("累计时长 (TOTAL HOURS)", "${stats.totalHours}h", "平均每次 $avgHours", Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF047857), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                TreeStatBox("待洗累计 (UNWASHED)", "${stats.unwashedHours}h", "$unwashedCount 条待洗记录", if (stats.needWash) Color(0xFFFFFBEB) else Color(0xFFF0FDF4), if (stats.needWash) Color(0xFFFDE68A) else Color(0xFFBBF7D0), if (stats.needWash) Color(0xFFB45309) else Color(0xFF047857), Modifier.weight(1f))
                                TreeStatBox("关联记录 (JUNCTIONS)", "${junctions.size} 条", "单价成本 $cpw", Color(0xFFFAF5FF), Color(0xFFE9D5FF), BrandPurple, Modifier.weight(1f))
                            }
                        }
                    }

                    // Section Header + [查看衣物完整档案] Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "关联穿搭报告明细 (${junctions.size} 条记录)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenGarmentDetail() }
                        ) {
                            Text(
                                text = "查看衣物完整档案",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Linked Wearing Report Cards (#1 .. #N)
                    junctions.forEachIndexed { idx, j ->
                        val report = reportsById[j.wearingReportId]
                        val rDate = report?.wearDate?.ifBlank { j.dateCreated.take(10) } ?: j.dateCreated.take(10)
                        val rTitle = report?.name?.ifBlank { j.wearingReportTitle } ?: j.wearingReportTitle.ifBlank { "casual" }
                        val isDirty = j.washStated.equals("need wash", ignoreCase = true)
                        val durHrs = String.format(Locale.US, "%.1fh", j.duration / 3600.0)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { if (report != null) onOpenReportDetail(report) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFAF5FF)
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandPurple,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = rDate,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Slate900
                                            )
                                            Text(
                                                text = rTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandPurple
                                            )
                                            Text(
                                                text = "🌡 ${extractDrillTemp(report)}  📍 ${extractDrillOccasion(report)}",
                                                fontSize = 10.sp,
                                                color = Slate500
                                            )
                                        }
                                        val note = report?.reportDetail?.ifBlank { "Comfortable session." } ?: "Comfortable session."
                                        Text(
                                            text = "\"$note\"",
                                            fontSize = 11.sp,
                                            color = Slate500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "舒适度 5/5   合身度 5/5   活动度 5/5",
                                            fontSize = 10.sp,
                                            color = Slate600
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "本次穿着 $durHrs",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                    ) {
                                        Text(
                                            text = if (isDirty) "待洗" else "已洗 (${j.washDate.ifBlank { rDate }})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeStatBox(
    title: String,
    mainValue: String,
    subValue: String,
    bgColor: Color,
    borderColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mainValue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
            Text(
                text = subValue,
                fontSize = 9.sp,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DrilldownReportTreeCard(
    report: ReportEntity,
    junctions: List<JunctionEntity>,
    clothesById: Map<String, GarmentEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenReportDetail: () -> Unit,
    onOpenGarmentDetail: (GarmentEntity) -> Unit
) {
    val totalHrs = remember(junctions) {
        val sec = junctions.maxOfOrNull { it.duration } ?: 0L
        String.format(Locale.US, "%.1fh", sec / 3600.0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isExpanded) Color(0xFFD8B4FE) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFAF5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${report.wearDate} · ${report.name.ifBlank { "Wearing Report" }}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "${junctions.size} 件关联衣物 · 单次时长 $totalHrs",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Slate500
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFC))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Slate200)
                    junctions.forEach { j ->
                        val g = clothesById[j.clothesId]
                        val gName = g?.name ?: j.clothesTitle.ifBlank { "Garment" }
                        val isDirty = j.washStated.equals("need wash", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (g != null) onOpenGarmentDetail(g) else onOpenReportDetail() }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = gName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = if (isDirty) "待洗 (${String.format(Locale.US, "%.1fh", j.duration / 3600.0)})" else "已洗 (${j.washDate})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-225458.png Split View:
 * Left pane: 衣物列表 (52)
 * Right pane: Selected garment header + 穿透式穿搭与洗护记录 (4)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrilldownSplitGarmentExplorer(
    garments: List<GarmentEntity>,
    activeGarment: GarmentEntity?,
    statsById: Map<String, GarmentStats>,
    junctionsByGarmentId: Map<String, List<JunctionEntity>>,
    reportsById: Map<String, ReportEntity>,
    isWide: Boolean,
    onSelectSplitGarment: (GarmentEntity) -> Unit,
    onOpenGarmentDetail: (GarmentEntity) -> Unit,
    onOpenReportDetail: (ReportEntity) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Garment List (36% on wide, 40% on mobile)
        Card(
            modifier = Modifier.weight(if (isWide) 0.35f else 0.40f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "衣物列表 (${garments.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate600,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
                HorizontalDivider(color = Slate100)
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = if (isWide) 700.dp else 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    garments.forEach { g ->
                        val isSelected = g.id == activeGarment?.id
                        val st = statsById[g.id]
                        val resolvedPhoto = ImageUtils.resolveFirstValidPhoto(g.pictures, context)?.let {
                            ImageUtils.resolveImageModel(it, context)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFAF5FF) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) BrandPurple else Slate100
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSplitGarment(g) }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (resolvedPhoto != null) {
                                            AsyncImage(
                                                model = resolvedPhoto,
                                                contentDescription = g.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Checkroom,
                                                contentDescription = null,
                                                tint = Slate400,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = g.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BrandPurple else Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${st?.totalWears ?: 0}次 · ${st?.totalHours ?: "0.0"}h",
                                            fontSize = 9.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (st?.needWash == true) Color(0xFFF59E0B) else Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Selected Garment Drilldown Detail (65% on wide, 60% on mobile)
        Card(
            modifier = Modifier.weight(if (isWide) 0.65f else 0.60f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            if (activeGarment != null) {
                val stats = statsById[activeGarment.id] ?: GarmentStats(
                    needWash = false,
                    unwashedSeconds = 0L,
                    unwashedHours = "0.0",
                    totalWears = 0,
                    totalDurationHours = 0L
                )
                val gJunctions = remember(activeGarment.id, junctionsByGarmentId, reportsById) {
                    junctionsByGarmentId[activeGarment.id].orEmpty().sortedByDescending { j ->
                        reportsById[j.wearingReportId]?.wearDate ?: j.dateCreated
                    }
                }
                val unwashedCount = gJunctions.count { it.washStated.equals("need wash", ignoreCase = true) }
                val resolvedPhoto = ImageUtils.resolveFirstValidPhoto(activeGarment.pictures, context)?.let {
                    ImageUtils.resolveImageModel(it, context)
                }

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Garment Summary Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                if (resolvedPhoto != null) {
                                    AsyncImage(
                                        model = resolvedPhoto,
                                        contentDescription = activeGarment.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Checkroom,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = activeGarment.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${activeGarment.clothesType.ifBlank { "Tops" }} · ${activeGarment.material.ifBlank { "100% Cotton" }} · $${activeGarment.price.toInt()}",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (stats.needWash) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                    ) {
                                        Text(
                                            text = if (stats.needWash) "Dirty" else "Clean",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (stats.needWash) Color(0xFFB45309) else Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                                        Text(
                                            text = "穿着 ${stats.totalWears} 次",
                                            fontSize = 9.sp,
                                            color = Slate700,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF0FDF4)) {
                                        Text(
                                            text = "总时长 ${stats.totalHours}h",
                                            fontSize = 9.sp,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                                        Text(
                                            text = "待洗 ${stats.unwashedHours}h (${unwashedCount}条)",
                                            fontSize = 9.sp,
                                            color = Slate700,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenGarmentDetail(activeGarment) }
                        ) {
                            Text(
                                text = "单件详情",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    Text(
                        text = "穿透式穿搭与洗护记录 (${gJunctions.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate700
                    )

                    // Linked Report Cards inside Right Split Pane (Screenshot 225458)
                    gJunctions.forEachIndexed { idx, j ->
                        val report = reportsById[j.wearingReportId]
                        val rDate = report?.wearDate?.ifBlank { j.dateCreated.take(10) } ?: j.dateCreated.take(10)
                        val rTitle = report?.name?.ifBlank { j.wearingReportTitle } ?: j.wearingReportTitle.ifBlank { "casual" }
                        val isDirty = j.washStated.equals("need wash", ignoreCase = true)
                        val durHrs = String.format(Locale.US, "%.1fh", j.duration / 3600.0)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { if (report != null) onOpenReportDetail(report) }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFAF5FF)
                                        ) {
                                            Text(
                                                text = "#${idx + 1}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = BrandPurple,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = rDate,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = rTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple
                                        )
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color.White) {
                                            Text(
                                                text = extractDrillTemp(report),
                                                fontSize = 9.sp,
                                                color = Slate600,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color.White) {
                                            Text(
                                                text = extractDrillOccasion(report),
                                                fontSize = 9.sp,
                                                color = Slate600,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = durHrs,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate800
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = if (isDirty) "待洗" else "已洗 ${j.washDate.ifBlank { rDate }}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                val note = report?.reportDetail?.ifBlank { "Comfortable wearing session." } ?: "Comfortable wearing session."
                                Text(
                                    text = "\"$note\"",
                                    fontSize = 10.sp,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Slate100),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("整体舒适 5/5", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                        Text("合身度 5/5", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                        Text("活动自如 5/5", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrilldownSplitReportExplorer(
    reports: List<ReportEntity>,
    activeReport: ReportEntity?,
    junctionsByReportId: Map<String, List<JunctionEntity>>,
    clothesById: Map<String, GarmentEntity>,
    isWide: Boolean,
    onSelectSplitReport: (ReportEntity) -> Unit,
    onOpenReportDetail: (ReportEntity) -> Unit,
    onOpenGarmentDetail: (GarmentEntity) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Card(
            modifier = Modifier.weight(if (isWide) 0.35f else 0.40f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "穿搭报告列表 (${reports.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate600
                )
                HorizontalDivider(color = Slate100)
                reports.forEach { r ->
                    val isSel = r.id == activeReport?.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) Color(0xFFFAF5FF) else Color.White,
                        border = BorderStroke(1.dp, if (isSel) BrandPurple else Slate100),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSplitReport(r) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(r.wearDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) BrandPurple else Slate900)
                            Text(r.name.ifBlank { "casual" }, fontSize = 10.sp, color = Slate500, maxLines = 1)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.weight(if (isWide) 0.65f else 0.60f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200)
        ) {
            if (activeReport != null) {
                val rJuncs = junctionsByReportId[activeReport.id].orEmpty()
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activeReport.wearDate} · ${activeReport.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAF5FF),
                            modifier = Modifier.clickable { onOpenReportDetail(activeReport) }
                        ) {
                            Text(
                                text = "报告详情",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = Slate100)
                    rJuncs.forEach { j ->
                        val g = clothesById[j.clothesId]
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (g != null) onOpenGarmentDetail(g) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(g?.name ?: j.clothesTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(String.format(Locale.US, "%.1fh", j.duration / 3600.0), fontSize = 11.sp, color = BrandPurple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-225502.png Flat Relational Table View:
 * Columns: 衣物 (GARMENT) | 分类 | 状态 | 次数 | 总时长 | 待洗时长 | 最近穿着 & 体感摘要 | 操作
 */
@Composable
private fun DrilldownFlatTableView(
    garments: List<GarmentEntity>,
    statsById: Map<String, GarmentStats>,
    latestWearDateById: Map<String, String>,
    junctionsByGarmentId: Map<String, List<JunctionEntity>>,
    reportsById: Map<String, ReportEntity>,
    minTableWidth: Dp = 1150.dp,
    onOpenGarmentDetail: (GarmentEntity) -> Unit
) {
    val context = LocalContext.current
    val hScroll = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScroll)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .width(minTableWidth)
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("衣物 (GARMENT)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(2.2f))
                Text("分类", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.9f))
                Text("状态", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.0f))
                Text("次数", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.7f))
                Text("总时长", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.8f))
                Text("待洗时长", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.9f))
                Text("最近穿着 & 体感摘要", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.8f))
                Text("操作", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.7f))
            }
            HorizontalDivider(color = Slate200)

            garments.forEach { garment ->
                val stats = statsById[garment.id]
                val isDirty = stats?.needWash == true
                val latestDate = latestWearDateById[garment.id].orEmpty()
                val gJunctions = junctionsByGarmentId[garment.id].orEmpty()
                val latestJunction = gJunctions.maxByOrNull { j ->
                    reportsById[j.wearingReportId]?.wearDate ?: j.dateCreated
                }
                val latestReport = latestJunction?.let { reportsById[it.wearingReportId] }
                val resolvedPhoto = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
                    ImageUtils.resolveImageModel(it, context)
                }

                Row(
                    modifier = Modifier
                        .width(minTableWidth)
                        .clickable { onOpenGarmentDetail(garment) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // GARMENT
                    Row(
                        modifier = Modifier.weight(2.2f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100),
                            contentAlignment = Alignment.Center
                        ) {
                            if (resolvedPhoto != null) {
                                AsyncImage(
                                    model = resolvedPhoto,
                                    contentDescription = garment.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = garment.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                softWrap = false
                            )
                            Text(
                                text = garment.material.ifBlank { "100% Cotton" },
                                fontSize = 10.sp,
                                color = Slate500,
                                softWrap = false
                            )
                        }
                    }

                    // 分类
                    Box(modifier = Modifier.weight(0.9f)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                            Text(
                                text = garment.clothesType.ifBlank { "Tops" },
                                fontSize = 10.sp,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 状态
                    Box(modifier = Modifier.weight(1.0f)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = if (isDirty) "Dirty (${stats?.unwashedHours ?: "0.0"}h)" else "Clean",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 次数
                    Text(
                        text = "${stats?.totalWears ?: 0} 次",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.weight(0.7f)
                    )

                    // 总时长
                    Text(
                        text = "${stats?.totalHours ?: "0.0"}h",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        modifier = Modifier.weight(0.8f)
                    )

                    // 待洗时长
                    Text(
                        text = "${stats?.unwashedHours ?: "0.0"}h",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDirty) Color(0xFFD97706) else Slate400,
                        modifier = Modifier.weight(0.9f)
                    )

                    // 最近穿着 & 体感摘要
                    Column(modifier = Modifier.weight(1.8f).padding(end = 8.dp)) {
                        if (latestDate.isNotBlank()) {
                            Text(
                                text = "$latestDate · ${latestReport?.name?.ifBlank { "casual" } ?: "casual"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800,
                                softWrap = false
                            )
                            val lHrs = String.format(Locale.US, "%.1fh", (latestJunction?.duration ?: 0L) / 3600.0)
                            Text(
                                text = "$lHrs · 舒适 5/5",
                                fontSize = 10.sp,
                                color = Slate500,
                                softWrap = false
                            )
                        } else {
                            Text("—", fontSize = 11.sp, color = Slate400)
                        }
                    }

                    // 操作
                    Box(modifier = Modifier.weight(0.7f)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAF5FF),
                            modifier = Modifier.clickable { onOpenGarmentDetail(garment) }
                        ) {
                            Text(
                                text = "详情",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}

@Composable
private fun SubgroupCard(
    sg: GroupSummary,
    fieldDef: ReportFieldDef?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldLabel = fieldDef?.label ?: sg.field
    val isUndefined = sg.value.equals("undefined", ignoreCase = true) || sg.value.isBlank()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isUndefined) Color(0xFFE2E8F0) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isUndefined) Color(0xFFF1F5F9) else Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (isUndefined) Slate400 else BrandEmerald,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Field description label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Field: $fieldLabel",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate600,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (isUndefined) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF2F2)
                            ) {
                                Text(
                                    text = "undefined (no value)",
                                    fontSize = 10.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (!fieldDef?.description.isNullOrBlank()) {
                        Text(
                            text = fieldDef?.description ?: "",
                            fontSize = 10.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Subgroup Value & Count Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isUndefined) "undefined" else sg.value,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUndefined) Slate500 else Slate900,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFECFDF5)
                        ) {
                            Text(
                                text = "${sg.count} garments",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (sg.needWashCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "${sg.needWashCount} need wash",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
