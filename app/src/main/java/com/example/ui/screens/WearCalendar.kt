package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.WashGuardDialog
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandRose
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.util.CalendarCalculations
import com.example.util.ContributingSession
import com.example.util.DailyWearEntry
import com.example.util.GarmentDayLifecycle
import com.example.util.GarmentLifecycleSummary
import com.example.util.ImageUtils
import com.example.util.LifecycleDayStatus
import com.example.util.WashHistoryDayEntry
import com.example.util.WashedGarmentSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CalendarViewMode(val id: String, val label: String) {
    DAILY_WORN("daily_worn", "Daily Worn"),
    WASH_HISTORY("wash_history", "Wash & Laundry"),
    CLOTHES_LIFECYCLE("clothes_lifecycle", "Clothes Lifecycle")
}

/**
 * Calendar component matching the original web app (WearCalendar.tsx / Ky):
 * - Unified card containing month-year header ("October 2026"), mode subtitle, [Today], [<] [>] month navigation, and [+ Log Outfit]
 * - 3-mode switcher pill bar (Daily Worn, Wash & Laundry, Clothes Lifecycle) with real-time legend
 * - When in Clothes Lifecycle: Clothes selector dropdown, unwashed/clean badge, and [Wash Now] button triggering WashGuardDialog
 * - 7-column calendar grid (Sun..Sat) with trailing/leading days, today highlight, mode badges, and day status indicators
 * - Day Detail Inspector Card appearing below when a date is selected, with close [X] button
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WearCalendar(
    reports: List<ReportEntity>,
    junctions: List<JunctionEntity>,
    clothes: List<GarmentEntity>,
    onSelectReport: (ReportEntity) -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit,
    onOpenLogModal: (String) -> Unit,
    onMarkGarmentWashed: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current

    // Build data maps matching web app calendarCalculations.ts (Ub, Lb, Hb)
    val dailyWearMap = remember(reports, junctions, clothes) {
        CalendarCalculations.buildDailyWearMap(reports, junctions, clothes)
    }

    val washHistoryMap = remember(junctions, clothes, reports) {
        CalendarCalculations.buildWashHistoryMap(junctions, clothes, reports)
    }

    // Default to the most recent active date in reports/junctions so the user immediately sees data
    val initialDate = remember(dailyWearMap) {
        val latestDateStr = dailyWearMap.keys.maxOrNull()
        if (!latestDateStr.isNullOrBlank()) {
            try {
                LocalDate.parse(latestDateStr)
            } catch (_: Exception) {
                LocalDate.now()
            }
        } else {
            LocalDate.now()
        }
    }

    var viewMode by remember { mutableStateOf(CalendarViewMode.DAILY_WORN) }
    var currentYear by remember { mutableIntStateOf(initialDate.year) }
    var currentMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedDateStr by remember {
        mutableStateOf<String?>(initialDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)))
    }

    // Update initial month/date once data loads if not yet jumped
    var didAutoJump by remember { mutableStateOf(false) }
    LaunchedEffect(dailyWearMap.size) {
        if (!didAutoJump && dailyWearMap.isNotEmpty()) {
            val latest = dailyWearMap.keys.maxOrNull()
            if (!latest.isNullOrBlank()) {
                try {
                    val ld = LocalDate.parse(latest)
                    currentYear = ld.year
                    currentMonth = ld.monthValue
                    selectedDateStr = latest
                    didAutoJump = true
                } catch (_: Exception) {}
            }
        }
    }

    var selectedLifecycleGarmentId by remember(clothes) {
        mutableStateOf(clothes.firstOrNull()?.id ?: "")
    }
    var garmentDropdownExpanded by remember { mutableStateOf(false) }

    val selectedLifecycleGarment = remember(clothes, selectedLifecycleGarmentId) {
        clothes.find { it.id == selectedLifecycleGarmentId } ?: clothes.firstOrNull()
    }

    val garmentLifecycle = remember(selectedLifecycleGarment, junctions, reports) {
        val gId = selectedLifecycleGarment?.id ?: ""
        CalendarCalculations.buildGarmentLifecycleMap(gId, junctions, reports, selectedLifecycleGarment)
    }

    // WashGuardDialog state
    var pendingWashGarment by remember { mutableStateOf<Pair<GarmentEntity, String>?>(null) }

    val yearMonth = YearMonth.of(currentYear, currentMonth)
    val monthTitle = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    val today = LocalDate.now()
    val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))

    // Build complete calendar matrix matching web app: Sunday-start with previous and next month filler days
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1 ... Saturday = 6
    val daysInMonth = yearMonth.lengthOfMonth()
    val prevMonth = yearMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()

    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
    val rowsCount = totalCells / 7

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }

        // =========================================================================
        // 1. UNIFIED MAIN CALENDAR CARD (Matching WearCalendar.tsx / Ky)
        // =========================================================================
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().testTag("wear_calendar_main_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row: Month Name + Subtitle on Left, Today & Nav & Log Outfit on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = Slate800,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = monthTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    maxLines = 1
                                )
                                Text(
                                    text = when (viewMode) {
                                        CalendarViewMode.DAILY_WORN -> "Daily clothes wear summary (by actual wear date)"
                                        CalendarViewMode.WASH_HISTORY -> "Laundry wash days & accumulated wear hours tracker"
                                        CalendarViewMode.CLOTHES_LIFECYCLE -> "Clothes lifecycle span: wear → unwashed days → clean"
                                    },
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right Controls: Today, Prev/Next buttons, Log Outfit button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate100,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentYear = today.year
                                        currentMonth = today.monthValue
                                        selectedDateStr = todayStr
                                    }
                                    .testTag("calendar_today_btn")
                            ) {
                                Text(
                                    text = "Today",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            // Month Arrow Buttons in single container
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.6f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val prev = yearMonth.minusMonths(1)
                                            currentYear = prev.year
                                            currentMonth = prev.monthValue
                                        },
                                        modifier = Modifier.size(32.dp).testTag("calendar_prev_month")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous Month",
                                            tint = Slate600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val next = yearMonth.plusMonths(1)
                                            currentYear = next.year
                                            currentMonth = next.monthValue
                                        },
                                        modifier = Modifier.size(32.dp).testTag("calendar_next_month")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next Month",
                                            tint = Slate600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { onOpenLogModal(selectedDateStr ?: todayStr) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp).testTag("calendar_log_outfit_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Outfit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // Mode Switcher Pills + Mode Legend
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 3-Mode Switcher Pill Container
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Slate100.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    CalendarViewMode.entries.forEach { mode ->
                                        val isSelected = viewMode == mode
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    viewMode = mode
                                                    selectedDateStr = null
                                                }
                                                .testTag("calendar_tab_${mode.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                val icon = when (mode) {
                                                    CalendarViewMode.DAILY_WORN -> Icons.Default.Checkroom
                                                    CalendarViewMode.WASH_HISTORY -> Icons.Default.LocalLaundryService
                                                    CalendarViewMode.CLOTHES_LIFECYCLE -> Icons.Default.History
                                                }
                                                val tint = when {
                                                    !isSelected -> Slate600
                                                    mode == CalendarViewMode.DAILY_WORN -> Slate800
                                                    mode == CalendarViewMode.WASH_HISTORY -> BrandBlue
                                                    else -> BrandAmber
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = tint,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = mode.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Slate900 else Slate600,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Legend row matching active mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (viewMode) {
                                CalendarViewMode.DAILY_WORN -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Slate900))
                                        Text("Worn items", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Slate700)
                                    }
                                    Text("·", fontSize = 11.sp, color = Slate400)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandAmber))
                                        Text("Need wash", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF92400E))
                                    }
                                    Text("·", fontSize = 11.sp, color = Slate400)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandEmerald))
                                        Text("All clean", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF065F46))
                                    }
                                }
                                CalendarViewMode.WASH_HISTORY -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandEmerald))
                                        Text("Wash day (with accumulated hours)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF065F46))
                                    }
                                }
                                CalendarViewMode.CLOTHES_LIFECYCLE -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Slate900))
                                        Text("Wear day", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Slate800)
                                    }
                                    Text("·", fontSize = 11.sp, color = Slate400)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandAmber))
                                        Text("Unwashed waiting span", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF92400E))
                                    }
                                    Text("·", fontSize = 11.sp, color = Slate400)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandEmerald))
                                        Text("Cleaned", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF065F46))
                                    }
                                }
                            }
                        }
                    }

                    // Single Garment Lifecycle Picker Bar (only when in CLOTHES_LIFECYCLE mode)
                    if (viewMode == CalendarViewMode.CLOTHES_LIFECYCLE && selectedLifecycleGarment != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Slate100.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Slate200.copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "SELECT CLOTHES:",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )

                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate300),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { garmentDropdownExpanded = true }
                                                .testTag("lifecycle_garment_select_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${selectedLifecycleGarment.name} (${selectedLifecycleGarment.clothesType.ifBlank { "Clothes" }})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate800,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "Dropdown",
                                                    tint = Slate600,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = garmentDropdownExpanded,
                                            onDismissRequest = { garmentDropdownExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .height(300.dp)
                                                .background(Color.White)
                                        ) {
                                            clothes.forEach { garment ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "${garment.name} (${garment.clothesType.ifBlank { "Clothes" }})",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Slate900
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedLifecycleGarmentId = garment.id
                                                        garmentDropdownExpanded = false
                                                        selectedDateStr = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Status Badge & Wash Now Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isDirty = garmentLifecycle.currentDirtyHours > 0 || garmentLifecycle.currentDirtySessions > 0
                                    if (isDirty) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFEF3C7),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.WaterDrop,
                                                    contentDescription = null,
                                                    tint = Color(0xFFB45309),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                val waitingLabel = if (garmentLifecycle.waitingDays > 0) {
                                                    "${garmentLifecycle.waitingDays}d waiting"
                                                } else {
                                                    "${garmentLifecycle.currentDirtySessions}d"
                                                }
                                                Text(
                                                    text = "Unwashed ($waitingLabel · ${garmentLifecycle.currentDirtyHours}h)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                        }

                                        // [Wash Now] Action Button (Calls WashGuardDialog)
                                        Button(
                                            onClick = {
                                                pendingWashGarment = selectedLifecycleGarment to (selectedDateStr ?: todayStr)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp).testTag("lifecycle_wash_now_btn")
                                        ) {
                                            Icon(
                                                Icons.Default.LocalLaundryService,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Wash Now",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Clean & in Closet",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF065F46)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Calendar Grid Table (Sun .. Sat header + 7-column day matrix)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.8f)),
                        color = Color.White
                    ) {
                        Column {
                            // Weekday Names Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC))
                                    .border(
                                        BorderStroke(0.5.dp, Slate200.copy(alpha = 0.8f))
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                                    Text(
                                        text = d.uppercase(),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate500
                                    )
                                }
                            }

                            // 7-Column Day Matrix with filler days from adjacent months
                            Column {
                                for (r in 0 until rowsCount) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        for (c in 0 until 7) {
                                            val cellIdx = r * 7 + c
                                            val dayOffset = cellIdx - startOffset + 1

                                            val (cellDayNumber, cellDateKey, isCurrentMonth) = when {
                                                dayOffset < 1 -> {
                                                    val pDay = daysInPrevMonth + dayOffset
                                                    val dKey = prevMonth.atDay(pDay).format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                                                    Triple(pDay, dKey, false)
                                                }
                                                dayOffset > daysInMonth -> {
                                                    val nDay = dayOffset - daysInMonth
                                                    val nextMonth = yearMonth.plusMonths(1)
                                                    val dKey = nextMonth.atDay(nDay).format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                                                    Triple(nDay, dKey, false)
                                                }
                                                else -> {
                                                    val dKey = yearMonth.atDay(dayOffset).format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                                                    Triple(dayOffset, dKey, true)
                                                }
                                            }

                                            val isSelected = cellDateKey == selectedDateStr
                                            val isToday = cellDateKey == todayStr

                                            CalendarCellItem(
                                                dayNumber = cellDayNumber,
                                                dateKey = cellDateKey,
                                                isCurrentMonth = isCurrentMonth,
                                                isToday = isToday,
                                                isSelected = isSelected,
                                                viewMode = viewMode,
                                                dailyEntry = dailyWearMap[cellDateKey],
                                                washEntry = washHistoryMap[cellDateKey],
                                                lifecycleEntry = garmentLifecycle.dayStatusMap[cellDateKey],
                                                onClick = {
                                                    selectedDateStr = if (selectedDateStr == cellDateKey) null else cellDateKey
                                                },
                                                modifier = Modifier.weight(1f)
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

        // =========================================================================
        // 2. SELECTED DAY INSPECTOR PANEL (matching bottom card in Ky)
        // =========================================================================
        if (!selectedDateStr.isNullOrBlank()) {
            item {
                SelectedDateInspectorPanel(
                    selectedDateStr = selectedDateStr!!,
                    viewMode = viewMode,
                    dailyEntry = dailyWearMap[selectedDateStr!!],
                    washEntry = washHistoryMap[selectedDateStr!!],
                    selectedLifecycleGarment = selectedLifecycleGarment,
                    garmentLifecycle = garmentLifecycle,
                    allClothes = clothes,
                    allReports = reports,
                    onSelectReport = onSelectReport,
                    onSelectGarment = onSelectGarment,
                    onClose = { selectedDateStr = null }
                )
            }
        }
    }

    // WashGuardDialog trigger
    if (pendingWashGarment != null) {
        val (garmentToWash, washDate) = pendingWashGarment!!
        WashGuardDialog(
            garmentName = garmentToWash.name,
            initialWashDate = washDate,
            onConfirm = { confirmedDate ->
                onMarkGarmentWashed?.invoke(garmentToWash.id, confirmedDate)
                pendingWashGarment = null
            },
            onDismiss = { pendingWashGarment = null }
        )
    }
}

/**
 * Calendar Cell Item matching the styling and badge layout of WearCalendar.tsx
 */
@Composable
private fun CalendarCellItem(
    dayNumber: Int,
    dateKey: String,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    viewMode: CalendarViewMode,
    dailyEntry: DailyWearEntry?,
    washEntry: WashHistoryDayEntry?,
    lifecycleEntry: GarmentDayLifecycle?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasWear = dailyEntry?.garments?.isNotEmpty() == true || dailyEntry?.reports?.isNotEmpty() == true
    val hasNeedWash = dailyEntry?.junctions?.any { it.washStated.equals("need wash", ignoreCase = true) } == true
    val hasWash = (washEntry?.totalWashedCount ?: 0) > 0

    // Dynamic background colors matching web app
    val cellBg = when {
        !isCurrentMonth -> Color(0xFFF8FAFC).copy(alpha = 0.6f)
        viewMode == CalendarViewMode.DAILY_WORN && hasWear -> {
            if (hasNeedWash) Color(0xFFFFFBEB).copy(alpha = 0.6f) else Color(0xFFF8FAFC)
        }
        viewMode == CalendarViewMode.WASH_HISTORY && hasWash -> Color(0xFFECFDF5).copy(alpha = 0.7f)
        viewMode == CalendarViewMode.CLOTHES_LIFECYCLE && lifecycleEntry != null -> {
            when (lifecycleEntry.status) {
                LifecycleDayStatus.WORN_DIRTY -> Color(0xFFF1F5F9)
                LifecycleDayStatus.WORN_AND_WASHED -> Color(0xFFF5F3FF)
                LifecycleDayStatus.WASHED_CLEAN -> Color(0xFFECFDF5).copy(alpha = 0.8f)
                LifecycleDayStatus.UNWASHED_WAITING -> Color(0xFFFFFBEB).copy(alpha = 0.5f)
            }
        }
        else -> Color.White
    }

    // Border highlights
    val cellBorder = when {
        isSelected -> BorderStroke(2.dp, Color(0xFF2563EB))
        isToday -> BorderStroke(2.dp, Slate800)
        else -> BorderStroke(0.5.dp, Slate200.copy(alpha = 0.7f))
    }

    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .background(if (isSelected) Color(0xFFEFF6FF) else cellBg)
            .border(cellBorder)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("calendar_day_$dateKey")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Day number (highlighted if today) + Dot status & count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Slate900),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNumber.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = dayNumber.toString(),
                        fontSize = 11.sp,
                        fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentMonth) Slate800 else Slate400
                    )
                }

                // Top right status indicator
                when (viewMode) {
                    CalendarViewMode.DAILY_WORN -> {
                        if (isCurrentMonth && hasWear) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (hasNeedWash) BrandAmber else BrandEmerald)
                                )
                                Text(
                                    text = "${dailyEntry?.garments?.size ?: 0}",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                            }
                        }
                    }
                    CalendarViewMode.WASH_HISTORY -> {
                        if (isCurrentMonth && hasWash) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(BrandEmerald)
                                    .border(1.dp, Color(0xFFA7F3D0), CircleShape)
                            )
                        }
                    }
                    CalendarViewMode.CLOTHES_LIFECYCLE -> {
                        if (isCurrentMonth && lifecycleEntry != null) {
                            val dotColor = when (lifecycleEntry.status) {
                                LifecycleDayStatus.WORN_DIRTY -> Slate900
                                LifecycleDayStatus.WORN_AND_WASHED -> BrandPurple
                                LifecycleDayStatus.WASHED_CLEAN -> BrandEmerald
                                LifecycleDayStatus.UNWASHED_WAITING -> BrandAmber
                            }
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }

            // Center Badge matching original web app styling
            when (viewMode) {
                CalendarViewMode.DAILY_WORN -> {
                    if (isCurrentMonth && hasWear) {
                        val itemsCount = dailyEntry?.garments?.size ?: 0
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        Icons.Default.Checkroom,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Text(
                                        text = "$itemsCount",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${dailyEntry?.totalDurationHours ?: 0.0}h",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                CalendarViewMode.WASH_HISTORY -> {
                    if (isCurrentMonth && hasWash) {
                        val count = washEntry?.totalWashedCount ?: 0
                        val hours = washEntry?.washedGarments?.sumOf { it.accumulatedWearHours } ?: 0.0
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocalLaundryService,
                                        contentDescription = null,
                                        tint = BrandEmerald,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Text(
                                        text = "$count",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", hours)}h",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }

                CalendarViewMode.CLOTHES_LIFECYCLE -> {
                    if (isCurrentMonth && lifecycleEntry != null) {
                        when (lifecycleEntry.status) {
                            LifecycleDayStatus.WORN_DIRTY -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Slate900,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Checkroom,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        val cleanHours = if (lifecycleEntry.wearHoursOnDay % 1.0 == 0.0) {
                                            "${lifecycleEntry.wearHoursOnDay.toInt()}h"
                                        } else {
                                            "${lifecycleEntry.wearHoursOnDay}h"
                                        }
                                        Text(
                                            text = "Worn $cleanHours",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            LifecycleDayStatus.UNWASHED_WAITING -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Day ${lifecycleEntry.dayInCycle} unw...",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF92400E),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 1.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            LifecycleDayStatus.WORN_AND_WASHED -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF3E8FF),
                                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Worn+Wash",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF6B21A8),
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                            LifecycleDayStatus.WASHED_CLEAN -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF059669),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Washed",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
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

/**
 * Bottom Inspector Panel matching the exact layout and views from Ky in WearCalendar.tsx
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedDateInspectorPanel(
    selectedDateStr: String,
    viewMode: CalendarViewMode,
    dailyEntry: DailyWearEntry?,
    washEntry: WashHistoryDayEntry?,
    selectedLifecycleGarment: GarmentEntity?,
    garmentLifecycle: GarmentLifecycleSummary,
    allClothes: List<GarmentEntity>,
    allReports: List<ReportEntity>,
    onSelectReport: (ReportEntity) -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val parsedDate = try {
        LocalDate.parse(selectedDateStr)
    } catch (_: Exception) {
        LocalDate.now()
    }
    val formattedDate = parsedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US))
    val expandedGarmentReports = remember { mutableStateMapOf<String, Boolean>() }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("calendar_day_inspector")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Inspector Header: Calendar Icon + Date + Close [X] Button
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Slate800,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = formattedDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("inspector_close_btn")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Inspector",
                        tint = Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            // Mode Content
            when (viewMode) {
                // =========================================================================
                // 1. DAILY WORN INSPECTOR
                // =========================================================================
                CalendarViewMode.DAILY_WORN -> {
                    val reports = dailyEntry?.reports.orEmpty()
                    val garments = dailyEntry?.garments.orEmpty()

                    if (reports.isEmpty() && garments.isEmpty()) {
                        Text(
                            text = "No wearing reports or clothes logged on $selectedDateStr.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    } else {
                        // Summary row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${garments.size} unique clothes worn on this date",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate600
                            )
                            Text(
                                text = "Total: ${dailyEntry?.totalDurationHours ?: 0.0} hrs logged",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }

                        // Wearing Reports banner (indigo container)
                        if (reports.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEEF2FF).copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE).copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth()
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Wearing Reports on $selectedDateStr (${reports.size})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF312E81)
                                            )
                                        }
                                        Text(
                                            text = "Click report to view",
                                            fontSize = 10.sp,
                                            color = Color(0xFF6366F1)
                                        )
                                    }

                                    reports.forEach { r ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onSelectReport(r) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = r.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate900,
                                                        maxLines = 1
                                                    )
                                                    if (r.wearDate.isNotBlank()) {
                                                        Text(
                                                            text = r.wearDate,
                                                            fontSize = 10.sp,
                                                            color = Slate500
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFEEF2FF),
                                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "View Report",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF4F46E5)
                                                        )
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.ArrowForward,
                                                            contentDescription = null,
                                                            tint = Color(0xFF4F46E5),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Worn Clothes List
                        if (garments.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                garments.forEach { g ->
                                    val junctionsForG = dailyEntry?.junctions.orEmpty().filter { it.clothesId == g.id }
                                    val totalDur = junctionsForG.sumOf { it.duration } / 3600.0
                                    val isNeedWash = junctionsForG.any { it.washStated.equals("need wash", ignoreCase = true) }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onSelectGarment(g) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val firstPhoto = ImageUtils.parsePhotoList(g.pictures).firstOrNull()
                                            val photoModel = ImageUtils.resolvePhotoModel(context, firstPhoto)
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Slate200),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (photoModel != null) {
                                                    AsyncImage(
                                                        model = photoModel,
                                                        contentDescription = g.name,
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Checkroom, contentDescription = null, tint = Slate400)
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = g.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = g.clothesType.ifBlank { "Garment" },
                                                    fontSize = 11.sp,
                                                    color = Slate500
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", totalDur)}h",
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isNeedWash) Color(0xFFFEF3C7) else Color(0xFFECFDF5)
                                                ) {
                                                    Text(
                                                        text = if (isNeedWash) "Need wash" else "Clean",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isNeedWash) Color(0xFF92400E) else Color(0xFF065F46),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                // =========================================================================
                // 2. WASH HISTORY INSPECTOR
                // =========================================================================
                CalendarViewMode.WASH_HISTORY -> {
                    val washed = washEntry?.washedGarments.orEmpty()
                    if (washed.isEmpty()) {
                        Text(
                            text = "No laundry washes recorded on $selectedDateStr.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    } else {
                        // Summary row
                        val totalCleaned = washEntry?.totalWashedCount ?: washed.size
                        val totalHours = washed.sumOf { it.accumulatedWearHours }
                        val totalHoursStr = if (totalHours % 1.0 == 0.0) "${totalHours.toInt()} hrs" else String.format(Locale.US, "%.1f hrs", totalHours)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$totalCleaned clothes washed\non this day",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    lineHeight = 16.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Accumulated:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "$totalHoursStr cleaned",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                        }

                        // Washed garments list
                        washed.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFF9FDFB),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Garment Header Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectGarment(item.garment) },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val firstPhoto = ImageUtils.parsePhotoList(item.garment.pictures).firstOrNull()
                                        val photoModel = ImageUtils.resolvePhotoModel(context, firstPhoto)
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFE2E8F0).copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (photoModel != null) {
                                                AsyncImage(
                                                    model = photoModel,
                                                    contentDescription = item.garment.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Checkroom,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.garment.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val accHoursStr = if (item.accumulatedWearHours % 1.0 == 0.0) {
                                                "${item.accumulatedWearHours.toInt()} hrs"
                                            } else {
                                                String.format(Locale.US, "%.1f hrs", item.accumulatedWearHours)
                                            }
                                            Text(
                                                text = "$accHoursStr accumulated wear",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46)
                                            )
                                            Text(
                                                text = "Across ${item.wearSessionsCount} wear session${if (item.wearSessionsCount == 1) "" else "s"} since last clean",
                                                fontSize = 11.sp,
                                                color = Slate600
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = "Clean",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Contributing reports section
                                    val isExpanded = expandedGarmentReports[item.garment.id] ?: true
                                    val sessionCount = item.contributingSessions.size

                                    if (sessionCount > 0) {
                                        HorizontalDivider(color = Color(0xFFA7F3D0).copy(alpha = 0.5f))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedGarmentReports[item.garment.id] = !isExpanded },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "CONTRIBUTING WEARING REPORTS ($sessionCount):",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF065F46),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFECFDF5),
                                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                            ) {
                                                Text(
                                                    text = if (isExpanded) "CLICK TO HIDE" else "CLICK TO VIEW",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF059669),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (isExpanded) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                item.contributingSessions.forEach { session ->
                                                    val sessionDurStr = if (session.durationHours % 1.0 == 0.0) {
                                                        "${session.durationHours.toInt()}h"
                                                    } else {
                                                        String.format(Locale.US, "%.1fh", session.durationHours)
                                                    }

                                                    val formattedWearDate = try {
                                                        val ld = LocalDate.parse(session.wearDate.take(10))
                                                        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US).format(ld)
                                                    } catch (_: Exception) {
                                                        session.wearDate
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(34.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color(0xFFD1FAE5)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Visibility,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF059669),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = session.reportTitle.ifBlank { "Wearing report" },
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Slate900,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "Worn on $formattedWearDate",
                                                                    fontSize = 10.sp,
                                                                    color = Slate600
                                                                )
                                                            }

                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Color(0xFFD1FAE5)
                                                            ) {
                                                                Text(
                                                                    text = sessionDurStr,
                                                                    fontSize = 11.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF065F46),
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                                )
                                                            }

                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = Color.White,
                                                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                                                modifier = Modifier.clickable {
                                                                    val rep = session.report ?: allReports.find { it.id == session.reportId }
                                                                    if (rep != null) {
                                                                        onSelectReport(rep)
                                                                    }
                                                                }
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "View",
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF059669)
                                                                    )
                                                                    Icon(
                                                                        Icons.AutoMirrored.Filled.ArrowForward,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF059669),
                                                                        modifier = Modifier.size(10.dp)
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
                        }
                    }
                }

                // =========================================================================
                // 3. CLOTHES LIFECYCLE INSPECTOR
                // =========================================================================
                CalendarViewMode.CLOTHES_LIFECYCLE -> {
                    if (selectedLifecycleGarment == null) {
                        Text(
                            text = "Please select a garment above to inspect its lifecycle.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    } else {
                        val dayLifecycle = garmentLifecycle.dayStatusMap[selectedDateStr]

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lifecycle Status for: ${selectedLifecycleGarment.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = selectedDateStr,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate500
                            )
                        }

                        if (dayLifecycle != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (dayLifecycle.status) {
                                        LifecycleDayStatus.WORN_DIRTY -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Checkroom, contentDescription = null, tint = Slate900, modifier = Modifier.size(16.dp))
                                                Text("Active Wear Day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                            }
                                            Text(
                                                text = "Worn on this day for ${dayLifecycle.wearHoursOnDay} hours. This started or continued the unwashed wearing cycle.",
                                                fontSize = 12.sp,
                                                color = Slate600
                                            )
                                        }
                                        LifecycleDayStatus.WORN_AND_WASHED -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.LocalLaundryService, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(16.dp))
                                                Text("Worn & Washed on Same Day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                                            }
                                            Text(
                                                text = "Garment was worn for ${dayLifecycle.wearHoursOnDay}h and washed clean on this same date.",
                                                fontSize = 12.sp,
                                                color = Slate600
                                            )
                                        }
                                        LifecycleDayStatus.WASHED_CLEAN -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.LocalLaundryService, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(16.dp))
                                                Text("Wash & Laundry Day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                            }
                                            Text(
                                                text = "Garment was washed on this date, resetting all unwashed accumulation back to clean.",
                                                fontSize = 12.sp,
                                                color = Slate600
                                            )
                                        }
                                        LifecycleDayStatus.UNWASHED_WAITING -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Unwashed Waiting Span (Day ${dayLifecycle.dayInCycle})",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                            Text(
                                                text = "The garment was worn previously and was waiting to be washed on this day. Total accumulated unwashed wear time: ${dayLifecycle.cumulativeDirtyHours}h across ${dayLifecycle.sessions.size} session(s).",
                                                fontSize = 12.sp,
                                                color = Slate700
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFECFDF5).copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "Clean & in Closet (no wear sessions or dirty time logged on this date)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF065F46)
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
