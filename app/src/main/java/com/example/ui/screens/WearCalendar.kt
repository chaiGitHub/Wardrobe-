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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
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
import com.example.util.CalendarCalculations
import com.example.util.DailyWearEntry
import com.example.util.GarmentDayLifecycle
import com.example.util.ImageUtils
import com.example.util.LifecycleDayStatus
import com.example.util.WashHistoryDayEntry
import com.example.util.WashedGarmentSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CalendarViewMode(val id: String, val label: String) {
    DAILY_WORN("daily_worn", "Daily Worn Clothes"),
    WASH_HISTORY("wash_history", "Wash History & Batches"),
    CLOTHES_LIFECYCLE("clothes_lifecycle", "Single Garment Lifecycle")
}

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Build maps using exact web app calendarCalculations.ts logic (Lb, Pb, Vb)
    val dailyWearMap = remember(reports, junctions, clothes) {
        CalendarCalculations.buildDailyWearMap(reports, junctions, clothes)
    }

    val washHistoryMap = remember(junctions, clothes, reports) {
        CalendarCalculations.buildWashHistoryMap(junctions, clothes, reports)
    }

    // Default to the most recent active date in reports/junctions so the user immediately sees their data
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
        mutableStateOf(initialDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)))
    }

    // Update initial month/date once seed data finishes loading
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

    val yearMonth = YearMonth.of(currentYear, currentMonth)
    val monthPrefix = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth)
    val monthTitle = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))

    // Build calendar cells (Sunday-start 7-column grid matching web app)
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0, Monday = 1 ... Saturday = 6
    val daysInMonth = yearMonth.lengthOfMonth()
    val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))

    // Month summary metrics
    val monthWearDays = dailyWearMap.keys.count { it.startsWith(monthPrefix) && (dailyWearMap[it]?.garments?.isNotEmpty() == true || dailyWearMap[it]?.reports?.isNotEmpty() == true) }
    val monthOutfitsCount = dailyWearMap.entries.filter { it.key.startsWith(monthPrefix) }.sumOf { it.value.junctions.size }
    val monthWearHours = dailyWearMap.entries.filter { it.key.startsWith(monthPrefix) }.sumOf { it.value.totalDurationHours }

    val monthWashDays = washHistoryMap.keys.count { it.startsWith(monthPrefix) }
    val monthWashedGarments = washHistoryMap.entries.filter { it.key.startsWith(monthPrefix) }.sumOf { it.value.totalWashedCount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER & 3-MODE SWITCHER CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wear & Laundry Calendar",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = "Track daily outfits, laundry batches & unwash time rollups",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }

                        Button(
                            onClick = { onOpenLogModal(selectedDateStr) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("calendar_log_wear_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Wear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3-Mode Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalendarViewMode.entries.forEach { mode ->
                            val isSelected = viewMode == mode
                            val activeColor = when (mode) {
                                CalendarViewMode.DAILY_WORN -> BrandPurple
                                CalendarViewMode.WASH_HISTORY -> BrandBlue
                                CalendarViewMode.CLOTHES_LIFECYCLE -> BrandEmerald
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) activeColor else Slate100,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) activeColor else Slate200
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewMode = mode }
                                    .testTag("calendar_mode_${mode.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val icon = when (mode) {
                                        CalendarViewMode.DAILY_WORN -> Icons.Default.Checkroom
                                        CalendarViewMode.WASH_HISTORY -> Icons.Default.LocalLaundryService
                                        CalendarViewMode.CLOTHES_LIFECYCLE -> Icons.Default.History
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Slate600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = mode.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Slate700
                                    )
                                }
                            }
                        }
                    }

                    // Single Garment Lifecycle Picker Bar
                    if (viewMode == CalendarViewMode.CLOTHES_LIFECYCLE && selectedLifecycleGarment != null) {
                        HorizontalDivider(color = Slate100)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "INSPECT GARMENT LIFECYCLE:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate500
                                )
                                if (garmentLifecycle.currentDirtyHours > 0 && onMarkGarmentWashed != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrandBlue.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onMarkGarmentWashed(selectedLifecycleGarment.id, selectedDateStr)
                                            }
                                            .testTag("lifecycle_mark_washed_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.LocalLaundryService,
                                                contentDescription = null,
                                                tint = BrandBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Wash on $selectedDateStr",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandBlue
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Slate100,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { garmentDropdownExpanded = true }
                                        .testTag("lifecycle_garment_selector")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            val thumb = ImageUtils.parsePhotoList(selectedLifecycleGarment.pictures).firstOrNull()
                                            val model = ImageUtils.resolvePhotoModel(context, thumb)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (model != null) {
                                                    AsyncImage(
                                                        model = model,
                                                        contentDescription = selectedLifecycleGarment.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Checkroom, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = selectedLifecycleGarment.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate900,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${selectedLifecycleGarment.clothesType.ifBlank { "Garment" }} • ${garmentLifecycle.totalWears} wears • ${garmentLifecycle.totalWashes} washes",
                                                    fontSize = 11.sp,
                                                    color = Slate500
                                                )
                                            }
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = Slate600)
                                    }
                                }

                                DropdownMenu(
                                    expanded = garmentDropdownExpanded,
                                    onDismissRequest = { garmentDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.88f)
                                        .height(340.dp)
                                        .background(Color.White)
                                ) {
                                    clothes.forEach { item ->
                                        val gStats = DrilldownUtils.calculateGarmentStats(item.id, junctions)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = Slate900,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${item.clothesType} • ${gStats.totalWears} sessions",
                                                            fontSize = 11.sp,
                                                            color = Slate500
                                                        )
                                                    }
                                                    if (gStats.needWash) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFFEF3C7)
                                                        ) {
                                                            Text(
                                                                text = "${gStats.unwashedHours}h dirty",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFB45309),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedLifecycleGarmentId = item.id
                                                garmentDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Lifecycle KPI Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MiniCalendarStatPill(
                                    label = "Sessions Worn",
                                    value = "${garmentLifecycle.totalWears}",
                                    color = BrandPurple,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniCalendarStatPill(
                                    label = "Total Washes",
                                    value = "${garmentLifecycle.totalWashes}",
                                    color = BrandBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniCalendarStatPill(
                                    label = "Unwashed Rollup",
                                    value = "${garmentLifecycle.currentDirtyHours}h (${garmentLifecycle.currentDirtySessions})",
                                    color = if (garmentLifecycle.currentDirtyHours > 0) BrandAmber else BrandEmerald,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. MONTHLY CALENDAR GRID CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Month Header Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val prev = yearMonth.minusMonths(1)
                                    currentYear = prev.year
                                    currentMonth = prev.monthValue
                                },
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = Slate700)
                            }
                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            IconButton(
                                onClick = {
                                    val next = yearMonth.plusMonths(1)
                                    currentYear = next.year
                                    currentMonth = next.monthValue
                                },
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = Slate700)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Jump to Latest Data button
                            val latestDataDate = dailyWearMap.keys.maxOrNull()
                            if (!latestDataDate.isNullOrBlank() && !latestDataDate.startsWith(monthPrefix)) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val ld = LocalDate.parse(latestDataDate)
                                            currentYear = ld.year
                                            currentMonth = ld.monthValue
                                            selectedDateStr = latestDataDate
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Latest Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val now = LocalDate.now()
                                    currentYear = now.year
                                    currentMonth = now.monthValue
                                    selectedDateStr = todayStr
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("calendar_today_btn")
                            ) {
                                Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode-specific Month KPI Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (viewMode) {
                            CalendarViewMode.DAILY_WORN -> {
                                MiniCalendarStatPill("Wear Days", "$monthWearDays d", BrandPurple, Modifier.weight(1f))
                                MiniCalendarStatPill("Items Worn", "$monthOutfitsCount", BrandEmerald, Modifier.weight(1f))
                                MiniCalendarStatPill("Total Hours", String.format(Locale.US, "%.1fh", monthWearHours), BrandAmber, Modifier.weight(1f))
                            }
                            CalendarViewMode.WASH_HISTORY -> {
                                val dirtyCount = clothes.count { g ->
                                    DrilldownUtils.calculateGarmentStats(g.id, junctions).needWash
                                }
                                MiniCalendarStatPill("Wash Days", "$monthWashDays d", BrandBlue, Modifier.weight(1f))
                                MiniCalendarStatPill("Washed in Month", "$monthWashedGarments", BrandEmerald, Modifier.weight(1f))
                                MiniCalendarStatPill("Need Wash Now", "$dirtyCount items", BrandAmber, Modifier.weight(1f))
                            }
                            CalendarViewMode.CLOTHES_LIFECYCLE -> {
                                val mWorn = garmentLifecycle.dayStatusMap.entries.count {
                                    it.key.startsWith(monthPrefix) && (it.value.status == LifecycleDayStatus.WORN_DIRTY || it.value.status == LifecycleDayStatus.WORN_AND_WASHED)
                                }
                                val mWashed = garmentLifecycle.dayStatusMap.entries.count {
                                    it.key.startsWith(monthPrefix) && it.value.washEvent
                                }
                                MiniCalendarStatPill("Worn in Month", "$mWorn d", BrandAmber, Modifier.weight(1f))
                                MiniCalendarStatPill("Washed in Month", "$mWashed d", BrandBlue, Modifier.weight(1f))
                                MiniCalendarStatPill("Unwashed Now", "${garmentLifecycle.currentDirtyHours}h", BrandPurple, Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Weekday Headers (Sun .. Sat)
                    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdays.forEach { dayLabel ->
                            Text(
                                text = dayLabel.uppercase(),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 7-Column Calendar Days Grid
                    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
                    val rowsCount = totalCells / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rowsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until 7) {
                                    val cellIdx = r * 7 + c
                                    val dayNum = cellIdx - startOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, dayNum)
                                        val isSelected = dateKey == selectedDateStr
                                        val isToday = dateKey == todayStr

                                        CalendarDayCellView(
                                            dayNum = dayNum,
                                            dateKey = dateKey,
                                            isSelected = isSelected,
                                            isToday = isToday,
                                            viewMode = viewMode,
                                            dailyEntry = dailyWearMap[dateKey],
                                            washEntry = washHistoryMap[dateKey],
                                            lifecycleEntry = garmentLifecycle.dayStatusMap[dateKey],
                                            onClick = { selectedDateStr = dateKey },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.78f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Slate100.copy(alpha = 0.35f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (viewMode) {
                            CalendarViewMode.DAILY_WORN -> {
                                LegendDot(color = BrandPurple, label = "Outfits Worn")
                                LegendDot(color = BrandAmber, label = "Has Unwashed Item")
                                LegendDot(color = BrandEmerald, label = "All Clean")
                            }
                            CalendarViewMode.WASH_HISTORY -> {
                                LegendDot(color = BrandBlue, label = "Laundry Wash Batch")
                                LegendDot(color = BrandEmerald, label = "Washed & Clean Reset")
                            }
                            CalendarViewMode.CLOTHES_LIFECYCLE -> {
                                LegendDot(color = BrandAmber, label = "Worn (Dirty Buildup)")
                                LegendDot(color = BrandPurple, label = "Worn & Washed Same Day")
                                LegendDot(color = BrandBlue, label = "Washed Clean")
                            }
                        }
                    }
                }
            }
        }

        // 3. SELECTED DAY INSPECTOR PANEL
        item {
            SelectedDateInspectorCard(
                selectedDateStr = selectedDateStr,
                viewMode = viewMode,
                dailyEntry = dailyWearMap[selectedDateStr],
                washEntry = washHistoryMap[selectedDateStr],
                selectedLifecycleGarment = selectedLifecycleGarment,
                garmentLifecycle = garmentLifecycle,
                allClothes = clothes,
                allJunctions = junctions,
                allReports = reports,
                onSelectReport = onSelectReport,
                onSelectGarment = onSelectGarment,
                onOpenLogModal = { onOpenLogModal(selectedDateStr) },
                onMarkGarmentWashed = onMarkGarmentWashed
            )
        }
    }
}

@Composable
private fun CalendarDayCellView(
    dayNum: Int,
    dateKey: String,
    isSelected: Boolean,
    isToday: Boolean,
    viewMode: CalendarViewMode,
    dailyEntry: DailyWearEntry?,
    washEntry: WashHistoryDayEntry?,
    lifecycleEntry: GarmentDayLifecycle?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bgColor = when {
        isSelected -> BrandPurple.copy(alpha = 0.10f)
        viewMode == CalendarViewMode.DAILY_WORN && (dailyEntry?.garments?.isNotEmpty() == true || dailyEntry?.reports?.isNotEmpty() == true) ->
            Color(0xFFF5F3FF)
        viewMode == CalendarViewMode.WASH_HISTORY && (washEntry?.totalWashedCount ?: 0) > 0 ->
            Color(0xFFEFF6FF)
        viewMode == CalendarViewMode.CLOTHES_LIFECYCLE && lifecycleEntry != null -> {
            when (lifecycleEntry.status) {
                LifecycleDayStatus.WORN_DIRTY -> Color(0xFFFFFBEB)
                LifecycleDayStatus.WORN_AND_WASHED -> Color(0xFFF3E8FF)
                LifecycleDayStatus.WASHED_CLEAN -> Color(0xFFEFF6FF)
            }
        }
        else -> Color.White
    }

    val borderColor = when {
        isSelected -> BrandPurple
        isToday -> BrandBlue
        viewMode == CalendarViewMode.CLOTHES_LIFECYCLE && lifecycleEntry != null -> {
            when (lifecycleEntry.status) {
                LifecycleDayStatus.WORN_DIRTY -> Color(0xFFFCD34D)
                LifecycleDayStatus.WORN_AND_WASHED -> Color(0xFFC084FC)
                LifecycleDayStatus.WASHED_CLEAN -> Color(0xFF93C5FD)
            }
        }
        else -> Slate200
    }

    Column(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("calendar_day_$dateKey"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: Day number + status dot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayNum.toString(),
                fontSize = 11.sp,
                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isSelected) BrandPurple else Slate800
            )

            when (viewMode) {
                CalendarViewMode.DAILY_WORN -> {
                    val hasDirty = dailyEntry?.junctions?.any { it.washStated.equals("need wash", ignoreCase = true) } == true
                    if (hasDirty) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BrandAmber)
                        )
                    } else if ((dailyEntry?.garments?.size ?: 0) > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BrandEmerald)
                        )
                    }
                }
                CalendarViewMode.WASH_HISTORY -> {
                    val count = washEntry?.totalWashedCount ?: 0
                    if (count > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandBlue
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                CalendarViewMode.CLOTHES_LIFECYCLE -> {
                    if (lifecycleEntry != null) {
                        val dotColor = when (lifecycleEntry.status) {
                            LifecycleDayStatus.WORN_DIRTY -> BrandAmber
                            LifecycleDayStatus.WORN_AND_WASHED -> BrandPurple
                            LifecycleDayStatus.WASHED_CLEAN -> BrandBlue
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

        // Bottom content depending on mode
        when (viewMode) {
            CalendarViewMode.DAILY_WORN -> {
                val garments = dailyEntry?.garments.orEmpty()
                if (garments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val firstGarment = garments.first()
                            val firstThumb = ImageUtils.parsePhotoList(firstGarment.pictures).firstOrNull()
                            val model = ImageUtils.resolvePhotoModel(context, firstThumb)
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Checkroom, contentDescription = null, tint = Slate400, modifier = Modifier.size(10.dp))
                                }
                            }
                            if (garments.size > 1) {
                                Text(
                                    text = "+${garments.size - 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPurple
                                )
                            }
                        }
                        Text(
                            text = "${dailyEntry?.totalDurationHours ?: 0.0}h",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600,
                            maxLines = 1
                        )
                    }
                } else if (dailyEntry?.reports?.isNotEmpty() == true) {
                    Text(
                        text = "${dailyEntry.reports.size} log",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPurple
                    )
                }
            }

            CalendarViewMode.WASH_HISTORY -> {
                val washed = washEntry?.washedGarments.orEmpty()
                if (washed.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val firstThumb = ImageUtils.parsePhotoList(washed.first().garment.pictures).firstOrNull()
                        val model = ImageUtils.resolvePhotoModel(context, firstThumb)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (model != null) {
                                    AsyncImage(
                                        model = model,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.LocalLaundryService, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(10.dp))
                                }
                            }
                            if (washed.size > 1) {
                                Text(
                                    text = "+${washed.size - 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                        }
                        Text(
                            text = "Washed",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            maxLines = 1
                        )
                    }
                }
            }

            CalendarViewMode.CLOTHES_LIFECYCLE -> {
                if (lifecycleEntry != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        val tagText = when (lifecycleEntry.status) {
                            LifecycleDayStatus.WORN_DIRTY -> "${lifecycleEntry.wearHoursOnDay}h"
                            LifecycleDayStatus.WORN_AND_WASHED -> "W&W"
                            LifecycleDayStatus.WASHED_CLEAN -> "Clean"
                        }
                        val tagColor = when (lifecycleEntry.status) {
                            LifecycleDayStatus.WORN_DIRTY -> Color(0xFFB45309)
                            LifecycleDayStatus.WORN_AND_WASHED -> BrandPurple
                            LifecycleDayStatus.WASHED_CLEAN -> BrandBlue
                        }
                        Text(
                            text = tagText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tagColor,
                            maxLines = 1
                        )
                        if (lifecycleEntry.cumulativeDirtyHours > 0) {
                            Text(
                                text = "∑${lifecycleEntry.cumulativeDirtyHours}h",
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate600,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedDateInspectorCard(
    selectedDateStr: String,
    viewMode: CalendarViewMode,
    dailyEntry: DailyWearEntry?,
    washEntry: WashHistoryDayEntry?,
    selectedLifecycleGarment: GarmentEntity?,
    garmentLifecycle: com.example.util.GarmentLifecycleSummary,
    allClothes: List<GarmentEntity>,
    allJunctions: List<JunctionEntity>,
    allReports: List<ReportEntity>,
    onSelectReport: (ReportEntity) -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit,
    onOpenLogModal: () -> Unit,
    onMarkGarmentWashed: ((String, String) -> Unit)?
) {
    val context = LocalContext.current
    val formattedDate = remember(selectedDateStr) {
        try {
            LocalDate.parse(selectedDateStr).format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US))
        } catch (_: Exception) {
            selectedDateStr
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Inspector Header
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandPurple.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = formattedDate,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = when (viewMode) {
                                CalendarViewMode.DAILY_WORN -> "Daily Outfit & Wear Sessions Breakdown"
                                CalendarViewMode.WASH_HISTORY -> "Laundry Batch & Contributing Sessions Rollup"
                                CalendarViewMode.CLOTHES_LIFECYCLE -> "Garment Wear-to-Wash Lifecycle Inspector"
                            },
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            when (viewMode) {
                // =========================================================
                // MODE 1: DAILY WORN CLOTHES
                // =========================================================
                CalendarViewMode.DAILY_WORN -> {
                    val dayReports = dailyEntry?.reports.orEmpty()
                    val dayJunctions = dailyEntry?.junctions.orEmpty()
                    val clothesById = remember(allClothes) { allClothes.associateBy { it.id } }

                    if (dayReports.isEmpty() && dayJunctions.isEmpty()) {
                        EmptyDayPrompt(
                            title = "No Wear Logs Recorded on $selectedDateStr",
                            subtitle = "Tap 'Log Wear' above to record an outfit and sensory notes for this date.",
                            actionLabel = "Log Outfit on $selectedDateStr",
                            onAction = onOpenLogModal
                        )
                    } else {
                        // Summary Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniCalendarStatPill("Reports", "${dayReports.size}", BrandPurple, Modifier.weight(1f))
                            MiniCalendarStatPill("Garments", "${dailyEntry?.garments?.size ?: 0}", BrandBlue, Modifier.weight(1f))
                            MiniCalendarStatPill("Total Wear", "${dailyEntry?.totalDurationHours ?: 0.0} hrs", BrandEmerald, Modifier.weight(1f))
                        }

                        // Wearing Reports Section
                        if (dayReports.isNotEmpty()) {
                            Text(
                                text = "WEARING JOURNAL REPORTS (${dayReports.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate500
                            )
                            dayReports.forEach { report ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectReport(report) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = report.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = report.wearDate,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate500
                                            )
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (report.shoulderFeel.isNotBlank()) {
                                                SensoryMiniTag("Shoulder: ${report.shoulderFeel}", BrandPurple)
                                            }
                                            if (report.midBreast.isNotBlank()) {
                                                SensoryMiniTag("Mid Breast: ${report.midBreast}", BrandBlue)
                                            }
                                            if (report.sweat.isNotBlank()) {
                                                SensoryMiniTag("Sweat: ${report.sweat}", BrandAmber)
                                            }
                                            if (report.legFeel.isNotBlank()) {
                                                SensoryMiniTag("Leg: ${report.legFeel}", BrandEmerald)
                                            }
                                        }
                                        if (report.reportDetail.isNotBlank()) {
                                            Text(
                                                text = report.reportDetail,
                                                fontSize = 12.sp,
                                                color = Slate600,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Garments Worn Section
                        if (dayJunctions.isNotEmpty()) {
                            Text(
                                text = "GARMENTS WORN ON THIS DAY (${dayJunctions.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate500
                            )

                            dayJunctions.forEach { j ->
                                val garment = clothesById[j.clothesId]
                                val durHrs = String.format(Locale.US, "%.1f", j.duration / 3600.0)
                                val isNeedWash = j.washStated.equals("need wash", ignoreCase = true)
                                val resolvedWashDate = j.washDate.ifBlank { j.washedDate }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (isNeedWash) Color(0xFFFDE68A) else Slate200),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { garment?.let { onSelectGarment(it) } }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val firstPhoto = garment?.let { ImageUtils.parsePhotoList(it.pictures).firstOrNull() }
                                        val photoModel = ImageUtils.resolvePhotoModel(context, firstPhoto)
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Slate100),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (photoModel != null) {
                                                AsyncImage(
                                                    model = photoModel,
                                                    contentDescription = garment?.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Default.Checkroom, contentDescription = null, tint = Slate400)
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = garment?.name ?: j.clothesTitle.ifBlank { "Garment" },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = (garment?.clothesType ?: j.clothesType).ifBlank { "Garment" },
                                                    fontSize = 11.sp,
                                                    color = Slate500
                                                )
                                                Text("•", fontSize = 11.sp, color = Slate400)
                                                Text(
                                                    text = "$durHrs hrs worn",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = BrandPurple
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isNeedWash) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = if (isNeedWash) "need wash" else "Clean",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (!isNeedWash && resolvedWashDate.isNotBlank()) {
                                                    Text(
                                                        text = "wash date: ${CalendarCalculations.normalizeDate(resolvedWashDate)}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Slate500
                                                    )
                                                }
                                            }
                                        }

                                        if (isNeedWash && onMarkGarmentWashed != null && garment != null) {
                                            OutlinedButton(
                                                onClick = { onMarkGarmentWashed(garment.id, selectedDateStr) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocalLaundryService,
                                                    contentDescription = "Mark Washed",
                                                    tint = BrandBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Wash", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // MODE 2: WASH HISTORY & BATCHES
                // =========================================================
                CalendarViewMode.WASH_HISTORY -> {
                    val washedGarments = washEntry?.washedGarments.orEmpty()

                    if (washedGarments.isNotEmpty()) {
                        Text(
                            text = "WASHED GARMENTS ON $selectedDateStr (${washedGarments.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandBlue
                        )

                        washedGarments.forEach { summary ->
                            WashedGarmentBatchCard(
                                summary = summary,
                                onSelectGarment = onSelectGarment,
                                onSelectReport = onSelectReport
                            )
                        }
                    } else {
                        EmptyDayPrompt(
                            title = "No Laundry Recorded on $selectedDateStr",
                            subtitle = "Select any dirty garment below to record a wash batch on $selectedDateStr.",
                            actionLabel = null,
                            onAction = {}
                        )
                    }

                    // Quick-Action: Record Wash on Selected Date for currently dirty garments
                    val dirtyGarments = remember(allClothes, allJunctions) {
                        allClothes.mapNotNull { g ->
                            val stats = DrilldownUtils.calculateGarmentStats(g.id, allJunctions)
                            if (stats.needWash) Pair(g, stats) else null
                        }
                    }

                    if (dirtyGarments.isNotEmpty() && onMarkGarmentWashed != null) {
                        HorizontalDivider(color = Slate100)
                        Text(
                            text = "RECORD WASH ON $selectedDateStr (${dirtyGarments.size} DIRTY GARMENTS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate500
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            dirtyGarments.forEach { (g, stats) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onMarkGarmentWashed(g.id, selectedDateStr) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocalLaundryService,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${g.name} (${stats.unwashedHours}h)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // MODE 3: SINGLE GARMENT LIFECYCLE
                // =========================================================
                CalendarViewMode.CLOTHES_LIFECYCLE -> {
                    val dayLife = garmentLifecycle.dayStatusMap[selectedDateStr]
                    if (selectedLifecycleGarment == null) {
                        Text("No garment selected.", fontSize = 13.sp, color = Slate500)
                    } else {
                        if (dayLife != null) {
                            val badgeColor = when (dayLife.status) {
                                LifecycleDayStatus.WORN_DIRTY -> BrandAmber
                                LifecycleDayStatus.WORN_AND_WASHED -> BrandPurple
                                LifecycleDayStatus.WASHED_CLEAN -> BrandBlue
                            }
                            val statusTitle = when (dayLife.status) {
                                LifecycleDayStatus.WORN_DIRTY -> "Worn — Unwashed Time Accumulated"
                                LifecycleDayStatus.WORN_AND_WASHED -> "Worn & Washed Clean on Same Day"
                                LifecycleDayStatus.WASHED_CLEAN -> "Washed Clean — Unwash Time Reset to 0h"
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = badgeColor.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = statusTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            text = "Worn on Day: ${dayLife.wearHoursOnDay} hrs",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate700
                                        )
                                        Text(
                                            text = "Cumulative Unwashed: ${dayLife.cumulativeDirtyHours} hrs",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate700
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No wear or wash event for ${selectedLifecycleGarment.name} on $selectedDateStr.",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }

                        // Full Chronological Lifecycle History for this garment
                        val allLifecycleDays = garmentLifecycle.dayStatusMap.values.sortedByDescending { it.dateKey }
                        if (allLifecycleDays.isNotEmpty()) {
                            HorizontalDivider(color = Slate100)
                            Text(
                                text = "COMPLETE WEAR-TO-WASH TIMELINE (${allLifecycleDays.size} DATES)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate500
                            )
                            allLifecycleDays.forEach { entry ->
                                val isCurr = entry.dateKey == selectedDateStr
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurr) Color(0xFFF5F3FF) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isCurr) BrandPurple else Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = entry.dateKey,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900
                                            )
                                            val label = when (entry.status) {
                                                LifecycleDayStatus.WORN_DIRTY -> "Worn (+${entry.wearHoursOnDay}h → ∑${entry.cumulativeDirtyHours}h dirty)"
                                                LifecycleDayStatus.WORN_AND_WASHED -> "Worn ${entry.wearHoursOnDay}h & Washed Clean"
                                                LifecycleDayStatus.WASHED_CLEAN -> "Washed Clean (Reset ${entry.cumulativeDirtyHours}h → 0h)"
                                            }
                                            val col = when (entry.status) {
                                                LifecycleDayStatus.WORN_DIRTY -> Color(0xFFB45309)
                                                LifecycleDayStatus.WORN_AND_WASHED -> BrandPurple
                                                LifecycleDayStatus.WASHED_CLEAN -> BrandBlue
                                            }
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = col
                                            )
                                        }
                                        entry.sessions.forEach { sess ->
                                            Text(
                                                text = "↳ ${sess.reportTitle} (${sess.durationHours} hrs)",
                                                fontSize = 11.sp,
                                                color = Slate600
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

@Composable
private fun WashedGarmentBatchCard(
    summary: WashedGarmentSummary,
    onSelectGarment: (GarmentEntity) -> Unit,
    onSelectReport: (ReportEntity) -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectGarment(summary.garment) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val firstPhoto = ImageUtils.parsePhotoList(summary.garment.pictures).firstOrNull()
                val photoModel = ImageUtils.resolvePhotoModel(context, firstPhoto)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoModel != null) {
                        AsyncImage(
                            model = photoModel,
                            contentDescription = summary.garment.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Checkroom, contentDescription = null, tint = Slate400)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = summary.garment.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = "Clean",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${summary.accumulatedWearHours} hrs accumulated wear • Across ${summary.wearSessionsCount} wear sessions since last clean",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandBlue
                    )
                }
            }

            if (summary.contributingSessions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Contributing Wearing Reports (${summary.contributingSessions.size}):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate500
                    )
                    summary.contributingSessions.forEach { sess ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sess.report?.let { onSelectReport(it) } },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sess.wearDate} • ${sess.reportTitle}",
                                fontSize = 11.sp,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${sess.durationHours} hrs",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCalendarStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Slate600
        )
    }
}

@Composable
private fun SensoryMiniTag(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyDayPrompt(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Slate700
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Slate500,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
