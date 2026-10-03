package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.dialogs.AddReportDialog
import com.example.ui.dialogs.InlineFieldDictionarySection
import com.example.ui.dialogs.SessionDetailModal
import com.example.ui.theme.BrandEmerald
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

private fun deriveReportComfort(r: ReportEntity): Int {
    if (r.shoulderFeel.contains("not", ignoreCase = true) || r.sweat.contains("sweat", ignoreCase = true) && !r.sweat.contains("not", ignoreCase = true)) {
        return 4
    }
    return 5
}

private fun deriveReportFit(r: ReportEntity): Int {
    if (r.midBreast.contains("not", ignoreCase = true) || r.TurnBody.contains("not", ignoreCase = true)) {
        return 4
    }
    return 5
}

private fun deriveReportMobility(r: ReportEntity): Int {
    if (r.legFeel.contains("not", ignoreCase = true) || r.liftHand.contains("not", ignoreCase = true)) {
        return 4
    }
    return 5
}

private fun deriveReportTemp(r: ReportEntity): String {
    val detail = r.reportDetail
    val regex = Regex("(\\d{1,2})\\s*°?C", RegexOption.IGNORE_CASE)
    val match = regex.find(detail)
    if (match != null) return "${match.groupValues[1]}°C"
    return if (r.sweat.contains("sweat", ignoreCase = true) && !r.sweat.contains("not", ignoreCase = true)) "26°C" else "22°C"
}

private fun deriveReportOccasion(r: ReportEntity): String {
    val n = r.name.lowercase(Locale.US)
    return when {
        n.contains("work") || n.contains("office") -> "Work"
        n.contains("home") || n.contains("casual") -> "Home"
        n.contains("out") || n.contains("walk") || n.contains("travel") -> "Outdoor"
        n.contains("dinner") || n.contains("date") || n.contains("party") -> "Social"
        else -> "Casual"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalScreen(
    reports: List<ReportEntity>,
    junctions: List<JunctionEntity>,
    clothes: List<GarmentEntity> = emptyList(),
    onAddReportClick: () -> Unit,
    onDeleteReportClick: (String) -> Unit,
    onSelectGarment: ((GarmentEntity) -> Unit)? = null,
    onUpdateWashStatus: ((String, String) -> Unit)? = null,
    onDeduplicateClick: (() -> Unit)? = null,
    onSaveOrUpdateReport: ((ReportEntity, List<Pair<String, Long>>, Map<String, String>) -> Unit)? = null,
    onUpdateReportOnly: ((ReportEntity) -> Unit)? = null,
    onDeleteJunctionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var journalViewMode by remember { mutableStateOf("cards") } // "cards", "grid", "table"
    var searchQuery by remember { mutableStateOf("") }
    var washFilter by remember { mutableStateOf("all") } // "all", "need_wash", "clean"

    var selectedReportForModal by remember { mutableStateOf<ReportEntity?>(null) }
    var editingReport by remember { mutableStateOf<ReportEntity?>(null) }

    val junctionsByReportId = remember(junctions) {
        junctions.groupBy { it.wearingReportId }
    }
    val clothesById = remember(clothes) {
        clothes.associateBy { it.id }
    }

    // Sort reports newest first
    val sortedReports = remember(reports) {
        reports.sortedByDescending { it.wearDate }
    }

    // Filter reports by searchQuery and washFilter
    val filteredReports = remember(sortedReports, junctionsByReportId, clothesById, searchQuery, washFilter) {
        sortedReports.filter { r ->
            val rJunctions = junctionsByReportId[r.id].orEmpty()
            val hasNeedWash = rJunctions.any { it.washStated.equals("need wash", ignoreCase = true) }

            val filterMatch = when (washFilter) {
                "need_wash" -> hasNeedWash
                "clean" -> !hasNeedWash
                else -> true
            }

            if (!filterMatch) return@filter false

            if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase(Locale.ROOT)
                val matchesClothes = rJunctions.any { j ->
                    val g = clothesById[j.clothesId]
                    (g?.name ?: j.clothesTitle).lowercase(Locale.ROOT).contains(q) ||
                        (g?.clothesType ?: j.clothesType).lowercase(Locale.ROOT).contains(q)
                }
                r.name.lowercase(Locale.ROOT).contains(q) ||
                    r.wearDate.lowercase(Locale.ROOT).contains(q) ||
                    r.shoulderFeel.lowercase(Locale.ROOT).contains(q) ||
                    r.midBreast.lowercase(Locale.ROOT).contains(q) ||
                    r.sweat.lowercase(Locale.ROOT).contains(q) ||
                    r.legFeel.lowercase(Locale.ROOT).contains(q) ||
                    r.reportDetail.lowercase(Locale.ROOT).contains(q) ||
                    matchesClothes
            }
        }
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

        val cardsColumns = if (isLaptop) 2 else 1
        val gridColumns = when {
            screenWidth >= 1100.dp -> 4
            screenWidth >= 650.dp -> 3
            else -> 2
        }

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
            // 1. TOP HEADER ROW + INLINE FIELD DICTIONARY (Screenshot 224918)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Wearing & Sensory Journal",
                                fontSize = if (isWide) 24.sp else 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = "${filteredReports.size} outfit reports · Click any report or garment to inspect details",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }

                        // Right Controls: [Cards | Grid | Table] Switcher + [+ New Report] Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            JournalViewModeSwitcher(
                                viewMode = journalViewMode,
                                onModeChange = { journalViewMode = it }
                            )

                            Button(
                                onClick = onAddReportClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("btn_log_outfit_header")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New Report",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Inline Collapsible Field Dictionary (Screenshot 224918)
                    InlineFieldDictionarySection(
                        highlightScope = "wearing_reports",
                        defaultExpanded = false
                    )
                }
            }

            // 2. SEARCH & FILTER CARD (Screenshot 224918)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    if (isWide) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            JournalSearchInput(
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                modifier = Modifier.weight(1f)
                            )
                            JournalWashFilterPills(
                                washFilter = washFilter,
                                onFilterChange = { washFilter = it }
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            JournalSearchInput(
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            JournalWashFilterPills(
                                washFilter = washFilter,
                                onFilterChange = { washFilter = it }
                            )
                        }
                    }
                }
            }

            // 3. REPORT DISPLAY MODES: Cards (224918), Grid (225021), or Table (225035)
            when (journalViewMode) {
                "grid" -> {
                    items(
                        filteredReports.chunked(gridColumns),
                        key = { row -> "journal_grid_${row.first().id}" }
                    ) { rowReports ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            rowReports.forEach { report ->
                                val rJunctions = junctionsByReportId[report.id].orEmpty()
                                JournalCompactGridCard(
                                    report = report,
                                    junctions = rJunctions,
                                    clothesById = clothesById,
                                    onClick = { selectedReportForModal = report },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(gridColumns - rowReports.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                "table" -> {
                    item {
                        JournalTableView(
                            reports = filteredReports,
                            junctionsByReportId = junctionsByReportId,
                            clothesById = clothesById,
                            minTableWidth = if (isWide) (screenWidth - 48.dp).coerceAtLeast(860.dp) else 860.dp,
                            onReportClick = { selectedReportForModal = it },
                            onSelectGarment = { g -> onSelectGarment?.invoke(g) }
                        )
                    }
                }

                else -> {
                    if (cardsColumns == 1) {
                        items(filteredReports, key = { it.id }) { report ->
                            val rJunctions = junctionsByReportId[report.id].orEmpty()
                            JournalFullReportCard(
                                report = report,
                                junctions = rJunctions,
                                clothesById = clothesById,
                                isWide = isWide,
                                onClick = { selectedReportForModal = report },
                                onDeleteClick = { onDeleteReportClick(report.id) },
                                onSelectGarment = { g -> onSelectGarment?.invoke(g) }
                            )
                        }
                    } else {
                        items(
                            filteredReports.chunked(cardsColumns),
                            key = { row -> "journal_card_row_${row.first().id}" }
                        ) { rowReports ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                rowReports.forEach { report ->
                                    val rJunctions = junctionsByReportId[report.id].orEmpty()
                                    JournalFullReportCard(
                                        report = report,
                                        junctions = rJunctions,
                                        clothesById = clothesById,
                                        isWide = false,
                                        onClick = { selectedReportForModal = report },
                                        onDeleteClick = { onDeleteReportClick(report.id) },
                                        onSelectGarment = { g -> onSelectGarment?.invoke(g) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(cardsColumns - rowReports.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Session Detail Modal
        selectedReportForModal?.let { report ->
            val currentReport = reports.find { it.id == report.id } ?: report
            val rJunctions = junctions.filter { it.wearingReportId == currentReport.id }
            SessionDetailModal(
                isOpen = true,
                report = currentReport,
                junctions = rJunctions,
                clothes = clothes,
                onClose = { selectedReportForModal = null },
                onEditReport = {
                    editingReport = currentReport
                    selectedReportForModal = null
                },
                onDeleteReport = { id ->
                    onDeleteReportClick(id)
                    selectedReportForModal = null
                },
                onSelectGarment = { garment ->
                    selectedReportForModal = null
                    onSelectGarment?.invoke(garment)
                },
                onUpdateWashStatus = { jId, status ->
                    onUpdateWashStatus?.invoke(jId, status)
                },
                onUpdateReport = { updated ->
                    onUpdateReportOnly?.invoke(updated)
                },
                onDeleteJunction = { jId ->
                    onDeleteJunctionClick?.invoke(jId)
                }
            )
        }

        // Edit Report Dialog
        editingReport?.let { reportToEdit ->
            val rJunctions = junctions.filter { it.wearingReportId == reportToEdit.id }
            AddReportDialog(
                clothes = clothes,
                existingReports = reports,
                initialReport = reportToEdit,
                initialJunctions = rJunctions,
                onDismiss = { editingReport = null },
                onSave = { updatedReport, links ->
                    onSaveOrUpdateReport?.invoke(updatedReport, links, emptyMap())
                    editingReport = null
                },
                onSaveWithWashMap = { updatedReport, links, washMap ->
                    onSaveOrUpdateReport?.invoke(updatedReport, links, washMap)
                    editingReport = null
                }
            )
        }
    }
}

@Composable
private fun JournalViewModeSwitcher(
    viewMode: String,
    onModeChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val modes = listOf(
                Triple("cards", "Cards", Icons.Default.GridView),
                Triple("grid", "Grid", Icons.Default.Apps),
                Triple("table", "Table", Icons.Default.TableRows)
            )
            modes.forEach { (mode, label, icon) ->
                val isSelected = viewMode == mode
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) Slate900 else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onModeChange(mode) }
                        .testTag("journal_view_$mode")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Slate600,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Slate600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalSearchInput(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = {
            Text(
                text = "Search by report title, notes, occasion, or garment...",
                fontSize = 12.sp,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        },
        singleLine = true,
        modifier = modifier
            .height(44.dp)
            .testTag("journal_search_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandEmerald,
            unfocusedBorderColor = Slate200,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8FAFC)
        )
    )
}

@Composable
private fun JournalWashFilterPills(
    washFilter: String,
    onFilterChange: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            "all" to "All",
            "need_wash" to "Need Wash",
            "clean" to "All Clean"
        ).forEach { (key, label) ->
            val isSelected = washFilter == key
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Slate900 else Slate100,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onFilterChange(key) }
                    .testTag("journal_filter_$key")
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else Slate700,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-224918.png Cards View:
 * - Green calendar badge + Date (2026-06-21) + Temp (22°C) + Occasion (Home) + Total: 7.0h
 * - Right side: All Clean (1) or Need Wash badge + Delete button
 * - Title (casual) + notes
 * - Comfort: 5/5 | Fit: 5/5 | Mobility: 5/5 pills
 * - Garments Worn in This Session (1) box with garment row cards
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalFullReportCard(
    report: ReportEntity,
    junctions: List<JunctionEntity>,
    clothesById: Map<String, GarmentEntity>,
    isWide: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalHours = remember(junctions) {
        val totalSec = junctions.maxOfOrNull { it.duration } ?: 0L
        String.format(Locale.US, "%.1fh", totalSec / 3600.0)
    }
    val needWashCount = remember(junctions) {
        junctions.count { it.washStated.equals("need wash", ignoreCase = true) }
    }
    val tempStr = remember(report) { deriveReportTemp(report) }
    val occasionStr = remember(report) { deriveReportOccasion(report) }
    val comfort = remember(report) { deriveReportComfort(report) }
    val fit = remember(report) { deriveReportFit(report) }
    val mobility = remember(report) { deriveReportMobility(report) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("report_card_${report.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Calendar icon + Date & Meta on Left, Wash Badge + Delete on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = report.wearDate.ifBlank { "—" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = tempStr,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = occasionStr,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Total: $totalHours",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                }

                // Right side: All Clean / Need Wash badge + Delete icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isDirty = needWashCount > 0
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, if (isDirty) Color(0xFFFDE68A) else Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isDirty) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDirty) Color(0xFFD97706) else Color(0xFF059669),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isDirty) "$needWashCount Need Wash" else "All Clean (${junctions.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Report",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Title & Notes
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = report.name.ifBlank { "Wearing Report" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                val notes = report.reportDetail.ifBlank {
                    listOfNotNull(
                        report.shoulderFeel.takeIf { it.isNotBlank() }?.let { "Shoulder: $it" },
                        report.midBreast.takeIf { it.isNotBlank() }?.let { "Chest: $it" },
                        report.sweat.takeIf { it.isNotBlank() }?.let { "Sweat: $it" },
                        report.legFeel.takeIf { it.isNotBlank() }?.let { "Leg: $it" }
                    ).joinToString(" · ").ifBlank { "Comfortable wearing session." }
                }
                Text(
                    text = notes,
                    fontSize = 12.sp,
                    color = Slate600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Comfort / Fit / Mobility Rating Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SensoryScorePill("Comfort", "$comfort/5")
                SensoryScorePill("Fit", "$fit/5")
                SensoryScorePill("Mobility", "$mobility/5")
            }

            HorizontalDivider(color = Slate100)

            // Garments Worn in This Session
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Garments Worn in This Session (${junctions.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                }

                val garmentRows = if (isWide && junctions.size > 1) junctions.chunked(2) else junctions.chunked(1)
                garmentRows.forEach { rowJuncs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowJuncs.forEach { j ->
                            val garment = clothesById[j.clothesId]
                            val gName = garment?.name ?: j.clothesTitle.ifBlank { "Garment" }
                            val gType = (garment?.clothesType ?: j.clothesType).ifBlank { "Tops" }
                            val isDirty = j.washStated.equals("need wash", ignoreCase = true)
                            val durHrs = String.format(Locale.US, "%.1fh", j.duration / 3600.0)
                            val resolvedPhoto = garment?.let {
                                ImageUtils.resolveFirstValidPhoto(it.pictures, context)?.let { p ->
                                    ImageUtils.resolveImageModel(p, context)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (garment != null) onSelectGarment(garment) else onClick()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
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
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (resolvedPhoto != null) {
                                                AsyncImage(
                                                    model = resolvedPhoto,
                                                    contentDescription = gName,
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
                                                text = gName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = gType,
                                                fontSize = 10.sp,
                                                color = Slate500
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = durHrs,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                            ) {
                                                Text(
                                                    text = if (isDirty) "Need Wash" else "Clean",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (!isDirty && j.washDate.isNotBlank()) {
                                            Text(
                                                text = "Washed: ${j.washDate}",
                                                fontSize = 9.sp,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (isWide && junctions.size > 1 && rowJuncs.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensoryScorePill(label: String, score: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$label:",
                fontSize = 11.sp,
                color = Slate500
            )
            Text(
                text = score,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
        }
    }
}

/**
 * Matches Screenshot_20261001-225021.png 2-Column (and Tablet/Laptop multi-column) Compact Grid Card
 */
@Composable
private fun JournalCompactGridCard(
    report: ReportEntity,
    junctions: List<JunctionEntity>,
    clothesById: Map<String, GarmentEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalHours = remember(junctions) {
        val totalSec = junctions.maxOfOrNull { it.duration } ?: 0L
        String.format(Locale.US, "%.1fh", totalSec / 3600.0)
    }
    val needWashCount = remember(junctions) {
        junctions.count { it.washStated.equals("need wash", ignoreCase = true) }
    }
    val isDirty = needWashCount > 0
    val tempStr = remember(report) { deriveReportTemp(report) }
    val occasionStr = remember(report) { deriveReportOccasion(report) }
    val comfort = remember(report) { deriveReportComfort(report) }
    val fit = remember(report) { deriveReportFit(report) }

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("journal_grid_card_${report.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Date + Clean/Wash Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.wearDate.ifBlank { "—" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF059669)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                ) {
                    Text(
                        text = if (isDirty) "$needWashCount Wash" else "${junctions.size} Clean",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Title & Meta
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = report.name.ifBlank { "Wearing Report" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$tempStr · $occasionStr · $totalHours",
                    fontSize = 10.sp,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mini Garment Thumbnails Strip
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(38.dp)
            ) {
                junctions.take(4).forEach { j ->
                    val g = clothesById[j.clothesId]
                    val resolvedPhoto = g?.let {
                        ImageUtils.resolveFirstValidPhoto(it.pictures, context)?.let { p ->
                            ImageUtils.resolveImageModel(p, context)
                        }
                    }
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
                }
                if (junctions.size > 4) {
                    Text(
                        text = "+${junctions.size - 4}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            // Footer: Comfort 5/5 | Fit 5/5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comfort $comfort/5",
                    fontSize = 10.sp,
                    color = Slate600
                )
                Text(
                    text = "Fit $fit/5",
                    fontSize = 10.sp,
                    color = Slate600
                )
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-225035.png Table View:
 * Columns: DATE | REPORT TITLE | TEMP / OCCASION | GARMENTS WORN | TOTAL DURATION | COMFORT / FIT | WASH STATUS
 */
@Composable
private fun JournalTableView(
    reports: List<ReportEntity>,
    junctionsByReportId: Map<String, List<JunctionEntity>>,
    clothesById: Map<String, GarmentEntity>,
    minTableWidth: Dp = 860.dp,
    onReportClick: (ReportEntity) -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit
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
                Text("DATE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.0f))
                Text("REPORT TITLE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.6f))
                Text("TEMP / OCCASION", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.1f))
                Text("GARMENTS WORN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(2.2f))
                Text("TOTAL DURATION", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.0f))
                Text("COMFORT / FIT", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.0f))
                Text("WASH STATUS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.0f))
            }
            HorizontalDivider(color = Slate200)

            reports.forEach { report ->
                val rJunctions = junctionsByReportId[report.id].orEmpty()
                val totalHours = remember(rJunctions) {
                    val totalSec = rJunctions.maxOfOrNull { it.duration } ?: 0L
                    String.format(Locale.US, "%.1fh", totalSec / 3600.0)
                }
                val needWashCount = rJunctions.count { it.washStated.equals("need wash", ignoreCase = true) }
                val isDirty = needWashCount > 0

                Row(
                    modifier = Modifier
                        .width(minTableWidth)
                        .clickable { onReportClick(report) }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DATE
                    Text(
                        text = report.wearDate.ifBlank { "—" },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669),
                        modifier = Modifier.weight(1.0f)
                    )

                    // REPORT TITLE
                    Column(modifier = Modifier.weight(1.6f).padding(end = 8.dp)) {
                        Text(
                            text = report.name.ifBlank { "Wearing Report" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sub = report.reportDetail.ifBlank { "Comfortable session." }
                        Text(
                            text = sub,
                            fontSize = 10.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // TEMP / OCCASION
                    Text(
                        text = "${deriveReportTemp(report)} · ${deriveReportOccasion(report)}",
                        fontSize = 11.sp,
                        color = Slate600,
                        modifier = Modifier.weight(1.1f)
                    )

                    // GARMENTS WORN
                    Row(
                        modifier = Modifier.weight(2.2f).padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rJunctions.take(2).forEach { j ->
                            val g = clothesById[j.clothesId]
                            val gName = g?.name ?: j.clothesTitle.ifBlank { "Garment" }
                            val resolvedPhoto = g?.let {
                                ImageUtils.resolveFirstValidPhoto(it.pictures, context)?.let { p ->
                                    ImageUtils.resolveImageModel(p, context)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.clickable {
                                    if (g != null) onSelectGarment(g) else onReportClick(report)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Slate100),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (resolvedPhoto != null) {
                                            AsyncImage(
                                                model = resolvedPhoto,
                                                contentDescription = gName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Checkroom,
                                                contentDescription = null,
                                                tint = Slate400,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = gName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )
                                }
                            }
                        }
                        if (rJunctions.size > 2) {
                            Text(
                                text = "+${rJunctions.size - 2}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                        }
                    }

                    // TOTAL DURATION
                    Text(
                        text = totalHours,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.weight(1.0f)
                    )

                    // COMFORT / FIT
                    Text(
                        text = "C:${deriveReportComfort(report)}/5 · F:${deriveReportFit(report)}/5",
                        fontSize = 11.sp,
                        color = Slate600,
                        modifier = Modifier.weight(1.0f)
                    )

                    // WASH STATUS
                    Box(modifier = Modifier.weight(1.0f)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = if (isDirty) "$needWashCount Need Wash" else "All Clean",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}
