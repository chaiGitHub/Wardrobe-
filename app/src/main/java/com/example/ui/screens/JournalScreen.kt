package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.AddReportDialog
import com.example.ui.dialogs.SessionDetailModal
import com.example.ui.dialogs.formatReportDate
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.dialogs.WashGuardDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    reports: List<ReportEntity>,
    junctions: List<JunctionEntity>,
    clothes: List<GarmentEntity> = emptyList(),
    onAddReportClick: () -> Unit,
    onDeleteReportClick: (String) -> Unit,
    onSelectGarment: ((GarmentEntity) -> Unit)? = null,
    onUpdateWashStatus: ((String, String) -> Unit)? = null,
    onMarkGarmentWashed: ((String, String) -> Unit)? = null,
    onDeduplicateClick: (() -> Unit)? = null,
    onSaveOrUpdateReport: ((ReportEntity, List<Pair<String, Long>>, Map<String, String>) -> Unit)? = null,
    onUpdateReportOnly: ((ReportEntity) -> Unit)? = null,
    onDeleteJunctionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf("list") } // "list" or "calendar"
    var searchQuery by remember { mutableStateOf("") }
    var groupMode by remember { mutableStateOf("None") } // "None", "Month", "Year", "Clothes"

    var selectedReportForModal by remember { mutableStateOf<ReportEntity?>(null) }
    var editingReport by remember { mutableStateOf<ReportEntity?>(null) }
    var pendingWashTarget by remember { mutableStateOf<Pair<JunctionEntity, String>?>(null) }

    // Expanded state for grouped headers (collapsed by default per instructions)
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    // Filter reports by searchQuery
    val filteredReports = remember(reports, junctions, clothes, searchQuery) {
        if (searchQuery.isBlank()) {
            reports
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            reports.filter { r ->
                val rJunctions = junctions.filter { it.wearingReportId == r.id }
                val matchesClothes = rJunctions.any { j ->
                    val g = clothes.find { it.id == j.clothesId }
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

    // Group reports when groupMode != "None"
    val groupedReports = remember(filteredReports, junctions, clothes, groupMode) {
        when (groupMode) {
            "Month" -> {
                filteredReports.groupBy { r ->
                    if (r.wearDate.length >= 7) r.wearDate.substring(0, 7) else "Unspecified Month"
                }
            }
            "Year" -> {
                filteredReports.groupBy { r ->
                    if (r.wearDate.length >= 4) r.wearDate.substring(0, 4) else "Unspecified Year"
                }
            }
            "Clothes" -> {
                val map = linkedMapOf<String, MutableList<ReportEntity>>()
                filteredReports.forEach { r ->
                    val rJuncs = junctions.filter { it.wearingReportId == r.id }
                    if (rJuncs.isEmpty()) {
                        map.getOrPut("Unlinked Sessions") { mutableListOf() }.add(r)
                    } else {
                        rJuncs.forEach { j ->
                            val gName = clothes.find { it.id == j.clothesId }?.name
                                ?: j.clothesTitle.ifBlank { "Garment" }
                            map.getOrPut(gName) { mutableListOf() }.add(r)
                        }
                    }
                }
                map
            }
            else -> emptyMap()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        if (viewMode == "calendar") {
            Column(modifier = Modifier.fillMaxSize()) {
                JournalHeaderCard(
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    groupMode = groupMode,
                    onGroupModeChange = { groupMode = it },
                    onLogOutfitClick = onAddReportClick,
                    onDeduplicateClick = { onDeduplicateClick?.invoke() }
                )
                WearCalendar(
                    reports = reports,
                    junctions = junctions,
                    clothes = clothes,
                    onSelectReport = { selectedReportForModal = it },
                    onSelectGarment = { garment -> onSelectGarment?.invoke(garment) },
                    onOpenLogModal = { _ -> onAddReportClick() },
                    onMarkGarmentWashed = onMarkGarmentWashed,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Top Header Card (IMG_9718)
                item {
                    JournalHeaderCard(
                        viewMode = viewMode,
                        onViewModeChange = { viewMode = it },
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        groupMode = groupMode,
                        onGroupModeChange = { groupMode = it },
                        onLogOutfitClick = onAddReportClick,
                        onDeduplicateClick = { onDeduplicateClick?.invoke() }
                    )
                }

                // 2. "All Wearing Reports" + "54 wearing reports" subheader (IMG_9718 & IMG_9719)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (groupMode == "None") "All Wearing Reports" else "Grouped by $groupMode",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "${filteredReports.size} wearing reports",
                            fontSize = 13.sp,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 3. Report Cards or Grouped Sections
                if (filteredReports.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = "No wearing reports found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Slate700
                                )
                                Text(
                                    text = "Tap '+ Log Outfit' above to record today's outfit & sensory notes.",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                } else if (groupMode == "None") {
                    items(filteredReports, key = { it.id }) { report ->
                        val reportJunctions = junctions.filter { it.wearingReportId == report.id }
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ReportCard(
                                report = report,
                                junctions = reportJunctions,
                                clothes = clothes,
                                onClick = { selectedReportForModal = report },
                                onSelectGarment = { g -> onSelectGarment?.invoke(g) },
                                onWashBadgeClick = { j, gName -> pendingWashTarget = j to gName }
                            )
                        }
                    }
                } else {
                    groupedReports.forEach { (groupTitle, groupItems) ->
                        item(key = "group_$groupTitle") {
                            val isExpanded = expandedGroups[groupTitle] ?: false
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedGroups[groupTitle] = !isExpanded }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5)
                                            )
                                            Text(
                                                text = groupTitle,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = Slate900
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFEEF2FF)
                                        ) {
                                            Text(
                                                text = "${groupItems.size} reports",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4F46E5),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .padding(bottom = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            groupItems.forEach { report ->
                                                val reportJunctions = junctions.filter { it.wearingReportId == report.id }
                                                ReportCard(
                                                    report = report,
                                                    junctions = reportJunctions,
                                                    clothes = clothes,
                                                    onClick = { selectedReportForModal = report },
                                                    onSelectGarment = { g -> onSelectGarment?.invoke(g) },
                                                    onWashBadgeClick = { j, gName -> pendingWashTarget = j to gName }
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

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddReportClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_report_fab"),
            containerColor = Color(0xFF4F46E5),
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Log Outfit")
        }
    }

    // Session Detail Modal (IMG_9720–IMG_9726)
    selectedReportForModal?.let { currentModalReport ->
        // Always observe latest version of report from reports list
        val liveReport = reports.find { it.id == currentModalReport.id } ?: currentModalReport
        SessionDetailModal(
            isOpen = true,
            report = liveReport,
            junctions = junctions,
            clothes = clothes,
            onClose = { selectedReportForModal = null },
            onSelectGarment = { g ->
                selectedReportForModal = null
                onSelectGarment?.invoke(g)
            },
            onUpdateWashStatus = { jId, status ->
                onUpdateWashStatus?.invoke(jId, status)
            },
            onDeleteReport = { rId ->
                onDeleteReportClick(rId)
                selectedReportForModal = null
            },
            onEditReport = { repToEdit ->
                selectedReportForModal = null
                editingReport = repToEdit
            },
            onUpdateReport = { updated ->
                onUpdateReportOnly?.invoke(updated)
            },
            onDeleteJunction = { jId ->
                onDeleteJunctionClick?.invoke(jId)
            }
        )
    }

    // Edit Wearing Report Dialog (reuses AddReportDialog with initialReport & initialJunctions)
    editingReport?.let { rep ->
        val repJunctions = junctions.filter { it.wearingReportId == rep.id }
        AddReportDialog(
            clothes = clothes,
            existingReports = reports,
            initialReport = rep,
            initialJunctions = repJunctions,
            onDismiss = { editingReport = null },
            onSave = { updatedRep, links ->
                onSaveOrUpdateReport?.invoke(updatedRep, links, emptyMap())
                editingReport = null
            },
            onSaveWithWashMap = { updatedRep, links, washMap ->
                onSaveOrUpdateReport?.invoke(updatedRep, links, washMap)
                editingReport = null
            }
        )
    }

    // Wash Guard Confirmation Dialog
    pendingWashTarget?.let { (junc, gName) ->
        val washDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        WashGuardDialog(
            garmentName = gName,
            initialWashDate = washDateStr,
            onConfirm = { date ->
                onUpdateWashStatus?.invoke(junc.id, "Clean")
                pendingWashTarget = null
            },
            onDismiss = { pendingWashTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalHeaderCard(
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    groupMode: String,
    onGroupModeChange: (String) -> Unit,
    onLogOutfitClick: () -> Unit,
    onDeduplicateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Header Icon + Title (Full width so "Wearing Report Journal" is never truncated)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4F46E5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Wearing Report Journal",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DATABASE RECORD",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4F46E5),
                        letterSpacing = 0.6.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Wearing Report Journal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1
                    )
                }
            }

            // Row 2: Action Buttons Row: [Deduplicate] + [+ Log Outfit]
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDeduplicateClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFFFFBEB),
                        contentColor = Color(0xFF92400E)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Deduplicate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onLogOutfitClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_log_outfit_header")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Log Outfit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // 3. Subtitle text
            Text(
                text = if (viewMode == "calendar") {
                    "Visual calendar displaying wearing reports and wash requirements day-by-day. Tap any day to inspect details in the panel below."
                } else {
                    "Wearing reports organized by date or group. Reports are collapsed by default; click any group header to expand."
                },
                fontSize = 13.sp,
                color = Slate600,
                lineHeight = 19.sp
            )

            // 4. View Switcher Pill: List vs Calendar (placed below the wearing report text)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isList = viewMode == "list"
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isList) Color.White else Color.Transparent,
                        shadowElevation = if (isList) 1.dp else 0.dp,
                        modifier = Modifier
                            .testTag("tab_view_mode_list")
                            .clickable { onViewModeChange("list") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (isList) Color(0xFF4F46E5) else Slate600,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "List",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isList) Slate900 else Slate600,
                                maxLines = 1
                            )
                        }
                    }

                    val isCal = viewMode == "calendar"
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isCal) Color.White else Color.Transparent,
                        shadowElevation = if (isCal) 1.dp else 0.dp,
                        modifier = Modifier
                            .testTag("tab_view_mode_calendar")
                            .clickable { onViewModeChange("calendar") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isCal) Color(0xFF4F46E5) else Slate600,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Calendar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCal) Slate900 else Slate600,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 5. Search Bar (IMG_9718)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = "Search report by name, sensory notes, or clothes...",
                        fontSize = 13.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4F46E5),
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                )
            )

            // 6. GROUP Filter Bar: Only shown when in List mode matching original web app
            if (viewMode == "list") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "▾ GROUP:",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        listOf("None", "Month", "Year", "Clothes").forEach { mode ->
                            val isSelected = groupMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                border = if (isSelected) null else BorderStroke(1.dp, Slate200),
                                modifier = Modifier.clickable { onGroupModeChange(mode) }
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportCard(
    report: ReportEntity,
    junctions: List<JunctionEntity>,
    clothes: List<GarmentEntity>,
    onClick: () -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit,
    onWashBadgeClick: ((JunctionEntity, String) -> Unit)? = null
) {
    val formattedDate = remember(report.wearDate) {
        formatReportDate(report.wearDate, "EEE, MMM d, yyyy")
    }

    val totalHrs = remember(junctions) {
        val maxSec = junctions.maxOfOrNull { it.duration } ?: 0L
        val sec = if (maxSec > 0) maxSec else 1200L
        String.format(Locale.US, "%.1f hrs", sec / 3600.0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_card_${report.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header: Calendar icon + Formatted Date + Duration pill + Eye button (IMG_9719)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formattedDate,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report.name.ifBlank { "Wearing Report" },
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate500
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Duration badge (e.g. "0.3 hrs")
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEEF2FF),
                        border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = totalHrs,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA)
                            )
                        }
                    }

                    // Eye icon circle button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View Report",
                                tint = Slate600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            // 2. CLOTHES(NEW) · CLOTHES ITEMS section (IMG_9719)
            Text(
                text = "CLOTHES(NEW)  •  CLOTHES ITEMS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 0.5.sp
            )

            if (junctions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    junctions.forEach { j ->
                        val garment = clothes.find { it.id == j.clothesId }
                        val gName = garment?.name ?: j.clothesTitle.ifBlank { "Garment" }
                        val gType = (garment?.clothesType ?: j.clothesType).ifBlank { "Item" }
                        val isDirty = j.washStated.contains("need wash", ignoreCase = true) || j.washStated.isBlank()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier.clickable {
                                if (garment != null) onSelectGarment(garment) else onClick()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = Color(0xFF9333EA),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = gName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "($gType)",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF9333EA)
                                )

                                // Clean / need wash inner badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDirty) Color(0xFFF59E0B) else Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.clickable {
                                        if (isDirty) {
                                            onWashBadgeClick?.invoke(j, gName)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isDirty) "need wash" else "Clean",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Sensory Monospace Summary Box (IMG_9719)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SensorySummaryLine("SHOULDER FEEL", report.shoulderFeel)
                    SensorySummaryLine("MID BREAST", report.midBreast)
                    SensorySummaryLine("SWEAT", report.sweat)
                    SensorySummaryLine("LEG FEEL", report.legFeel)
                }
            }

            // 4. Bottom Action Prompt: "👁 Tap to view full detail & edit this wearing report >" (IMG_9719)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Tap to view full detail & edit this wearing report",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SensorySummaryLine(label: String, value: String) {
    val hasData = value.isNotBlank()
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Slate500
                )
            ) {
                append("$label: ")
            }
            if (hasData) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Slate800
                    )
                ) {
                    append(value)
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = Slate400
                    )
                ) {
                    append("No data")
                }
            }
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
