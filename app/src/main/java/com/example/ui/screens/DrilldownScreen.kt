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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.dialogs.GarmentDetailDialog
import com.example.ui.dialogs.InlineFieldDictionarySection
import com.example.ui.dialogs.SessionDetailModal
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.drilldown.GarmentStats
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
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("garment_to_report") } // "garment_to_report" or "report_to_garment"
    var sortOption by remember { mutableStateOf(DrilldownSortOption.LATEST_WEAR) }
    var viewMode by remember { mutableStateOf("cards") } // "cards" (Tree), "split" (Split), "table" (List)

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

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1440.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = if (isWide) 24.dp else 14.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HEADER & CONTROLS CARD (Screenshot 225451)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFAF5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Interactive Data Drilldown Explorer",
                                    fontSize = if (isWide) 20.sp else 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "穿透式数据浏览器：按「衣物 → 穿搭报告」或「穿搭报告 → 衣物」自由钻取，并支持树状卡片、分栏钻取、列表呈现 3 种视图",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Inline Collapsible Field Dictionary (Screenshot 225451)
                        InlineFieldDictionarySection(
                            highlightScope = "all",
                            defaultExpanded = false
                        )

                        // Search Bar + Direction Switcher (side-by-side on Laptop, stacked on Mobile)
                        if (isLaptop) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DrilldownSearchField(
                                    searchQuery = searchQuery,
                                    onSearchChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f)
                                )
                                DrilldownDirectionToggle(
                                    direction = direction,
                                    onDirectionChange = { direction = it }
                                )
                            }
                        } else {
                            DrilldownSearchField(
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DrilldownDirectionToggle(
                                direction = direction,
                                onDirectionChange = { direction = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Sort Pills Row: 排序: 最新穿着 | 穿着次数 | 待洗时长 | 累计穿着时长 | 名称
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "排序:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                            DrilldownSortOption.entries.forEach { opt ->
                                val isSel = sortOption == opt
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFFFAF5FF) else Slate100,
                                    border = if (isSel) BorderStroke(1.dp, Color(0xFFD8B4FE)) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { sortOption = opt }
                                ) {
                                    Text(
                                        text = opt.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) BrandPurple else Slate700,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // FULL-WIDTH BOTTOM VIEW MODE SWITCHER (Screenshot 225451 - Bottom placement)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val modes = listOf(
                                    Triple("cards", "树状卡片 Tree", Icons.Default.AccountTree),
                                    Triple("split", "分栏钻取 Split", Icons.Default.ViewSidebar),
                                    Triple("table", "列表呈现 List", Icons.Default.TableRows)
                                )
                                modes.forEach { (mode, label, icon) ->
                                    val isSel = viewMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isSel) BrandPurple else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable { viewMode = mode }
                                            .testTag("drilldown_present_mode_$mode")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSel) Color.White else Slate600,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
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

            // 2. CONTENT BY VIEW MODE
            when (viewMode) {
                // MODE A: TREE CARDS (Screenshot 225451 & 225454)
                "cards" -> {
                    if (direction == "garment_to_report") {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "共 ${sortedGarments.size} 件衣物（展开任意卡片查看其完整穿搭历史与每次穿着时长/洗护状态）",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "展开第一个",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPurple,
                                        modifier = Modifier.clickable {
                                            expandedIds.clear()
                                            sortedGarments.firstOrNull()?.let { expandedIds[it.id] = true }
                                        }
                                    )
                                    Text(
                                        text = "全部收起",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate500,
                                        modifier = Modifier.clickable { expandedIds.clear() }
                                    )
                                }
                            }
                        }

                        items(sortedGarments, key = { "tree_g_${it.id}" }) { garment ->
                            val isExpanded = expandedIds[garment.id] == true
                            val stats = statsById[garment.id] ?: GarmentStats(
                                needWash = false,
                                unwashedSeconds = 0L,
                                unwashedHours = "0.0",
                                totalWears = 0,
                                totalDurationHours = 0L
                            )
                            val gJunctions = remember(garment.id, junctionsByGarmentId, reportsById) {
                                junctionsByGarmentId[garment.id].orEmpty().sortedByDescending { j ->
                                    reportsById[j.wearingReportId]?.wearDate ?: j.dateCreated
                                }
                            }
                            DrilldownGarmentTreeCard(
                                garment = garment,
                                stats = stats,
                                latestWearDate = latestWearDateById[garment.id].orEmpty(),
                                junctions = gJunctions,
                                reportsById = reportsById,
                                isExpanded = isExpanded,
                                isWide = isWide,
                                onToggleExpand = { expandedIds[garment.id] = !isExpanded },
                                onOpenGarmentDetail = {
                                    if (onSelectGarment != null) onSelectGarment(garment)
                                    else selectedGarmentForDetail = garment
                                },
                                onOpenReportDetail = { r ->
                                    if (onSelectReport != null) onSelectReport(r)
                                    else selectedReportForDetail = r
                                }
                            )
                        }
                    } else {
                        // Report -> Garments Tree Cards
                        items(sortedReports, key = { "tree_r_${it.id}" }) { report ->
                            val isExpanded = expandedIds[report.id] == true
                            val rJunctions = junctionsByReportId[report.id].orEmpty()
                            DrilldownReportTreeCard(
                                report = report,
                                junctions = rJunctions,
                                clothesById = clothesById,
                                isExpanded = isExpanded,
                                onToggleExpand = { expandedIds[report.id] = !isExpanded },
                                onOpenReportDetail = { selectedReportForDetail = report },
                                onOpenGarmentDetail = { g ->
                                    if (onSelectGarment != null) onSelectGarment(g)
                                    else selectedGarmentForDetail = g
                                }
                            )
                        }
                    }
                }

                // MODE B: SPLIT MASTER-DETAIL (Screenshot 225458)
                "split" -> {
                    item {
                        if (direction == "garment_to_report") {
                            val activeGarment = sortedGarments.find { it.id == selectedSplitGarmentId }
                                ?: sortedGarments.firstOrNull()
                            DrilldownSplitGarmentExplorer(
                                garments = sortedGarments,
                                activeGarment = activeGarment,
                                statsById = statsById,
                                junctionsByGarmentId = junctionsByGarmentId,
                                reportsById = reportsById,
                                isWide = isWide,
                                onSelectSplitGarment = { selectedSplitGarmentId = it.id },
                                onOpenGarmentDetail = { g ->
                                    if (onSelectGarment != null) onSelectGarment(g)
                                    else selectedGarmentForDetail = g
                                },
                                onOpenReportDetail = { r -> selectedReportForDetail = r }
                            )
                        } else {
                            val activeReport = sortedReports.find { it.id == selectedSplitReportId }
                                ?: sortedReports.firstOrNull()
                            DrilldownSplitReportExplorer(
                                reports = sortedReports,
                                activeReport = activeReport,
                                junctionsByReportId = junctionsByReportId,
                                clothesById = clothesById,
                                isWide = isWide,
                                onSelectSplitReport = { selectedSplitReportId = it.id },
                                onOpenReportDetail = { r -> selectedReportForDetail = r },
                                onOpenGarmentDetail = { g ->
                                    if (onSelectGarment != null) onSelectGarment(g)
                                    else selectedGarmentForDetail = g
                                }
                            )
                        }
                    }
                }

                // MODE C: FLAT RELATIONAL TABLE (Screenshot 225502)
                else -> {
                    item {
                        DrilldownFlatTableView(
                            garments = sortedGarments,
                            statsById = statsById,
                            latestWearDateById = latestWearDateById,
                            junctionsByGarmentId = junctionsByGarmentId,
                            reportsById = reportsById,
                            minTableWidth = if (isWide) (screenWidth - 48.dp).coerceAtLeast(860.dp) else 860.dp,
                            onOpenGarmentDetail = { g ->
                                if (onSelectGarment != null) onSelectGarment(g)
                                else selectedGarmentForDetail = g
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

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
        modifier = modifier.height(44.dp),
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
                                contentScale = ContentScale.Crop,
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
                                                contentScale = ContentScale.Crop,
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
                                        contentScale = ContentScale.Crop,
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
    minTableWidth: Dp = 860.dp,
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
                                    contentScale = ContentScale.Crop,
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = garment.material.ifBlank { "100% Cotton" },
                                fontSize = 10.sp,
                                color = Slate500,
                                maxLines = 1
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val lHrs = String.format(Locale.US, "%.1fh", (latestJunction?.duration ?: 0L) / 3600.0)
                            Text(
                                text = "$lHrs · 舒适 5/5",
                                fontSize = 10.sp,
                                color = Slate500
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
