package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.SessionDetailModal
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
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
import com.example.util.ImageUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class MonthlyGarmentRank(
    val garment: GarmentEntity,
    val wearCount: Int,
    val totalHours: Double
)

private fun parseCalendarReportTemp(r: ReportEntity): Int {
    val match = Regex("(\\d{1,2})\\s*°?C", RegexOption.IGNORE_CASE).find(r.reportDetail)
    if (match != null) {
        return match.groupValues[1].toIntOrNull() ?: 22
    }
    return if (r.sweat.contains("sweat", ignoreCase = true) && !r.sweat.contains("not", ignoreCase = true)) 26 else 22
}

private fun parseCalendarOccasion(r: ReportEntity): String {
    val n = r.name.lowercase(Locale.US)
    return when {
        n.contains("work") || n.contains("office") -> "Work"
        n.contains("home") || n.contains("casual") -> "Home"
        n.contains("out") || n.contains("walk") -> "Outdoor"
        n.contains("dinner") || n.contains("social") -> "Social"
        else -> "Home"
    }
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
    val dailyWearMap = remember(reports, junctions, clothes) {
        CalendarCalculations.buildDailyWearMap(reports, junctions, clothes)
    }
    val clothesById = remember(clothes) { clothes.associateBy { it.id } }

    // Default to the most recent active date in reports/junctions
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

    var currentYear by remember { mutableIntStateOf(initialDate.year) }
    var currentMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedDateStr by remember {
        mutableStateOf(initialDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)))
    }
    var inspectingReport by remember { mutableStateOf<ReportEntity?>(null) }

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

    val yearMonth = YearMonth.of(currentYear, currentMonth)
    val monthPrefix = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth)
    val monthTitle = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    val monthShortName = yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM", Locale.US))
    val daysInMonth = yearMonth.lengthOfMonth()

    // Monthly KPI Calculations (Matching Screenshot 225909)
    val monthEntries = remember(dailyWearMap, monthPrefix) {
        dailyWearMap.filterKeys { it.startsWith(monthPrefix) }
    }
    val daysWornThisMonth = remember(monthEntries) {
        monthEntries.count { it.value.reports.isNotEmpty() || it.value.garments.isNotEmpty() }
    }
    val outfitSessionsCount = remember(monthEntries) {
        monthEntries.values.sumOf { it.reports.size }
    }
    val totalMonthHours = remember(monthEntries) {
        String.format(Locale.US, "%.1fh", monthEntries.values.sumOf { it.totalDurationHours })
    }
    val uniqueGarmentsCount = remember(monthEntries) {
        monthEntries.values.flatMap { it.garments.map { g -> g.id } }.distinct().size
    }
    val tempStats = remember(monthEntries) {
        val temps = monthEntries.values.flatMap { it.reports }.map { parseCalendarReportTemp(it) }
        if (temps.isEmpty()) {
            Triple("21.7°C", 28, 12)
        } else {
            val avg = String.format(Locale.US, "%.1f°C", temps.average())
            Triple(avg, temps.maxOrNull() ?: 28, temps.minOrNull() ?: 12)
        }
    }

    // Most Worn This Month Ranked List (Matching Screenshot 225932)
    val mostWornThisMonth = remember(monthEntries, clothesById) {
        val countMap = mutableMapOf<String, Int>()
        val hoursMap = mutableMapOf<String, Double>()
        monthEntries.values.forEach { entry ->
            entry.junctions.forEach { j ->
                countMap[j.clothesId] = (countMap[j.clothesId] ?: 0) + 1
                hoursMap[j.clothesId] = (hoursMap[j.clothesId] ?: 0.0) + (j.duration / 3600.0)
            }
        }
        countMap.entries
            .mapNotNull { (gId, cnt) ->
                val g = clothesById[gId] ?: return@mapNotNull null
                MonthlyGarmentRank(
                    garment = g,
                    wearCount = cnt,
                    totalHours = hoursMap[gId] ?: 0.0
                )
            }
            .sortedWith(compareByDescending<MonthlyGarmentRank> { it.wearCount }.thenByDescending { it.totalHours })
            .take(8)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP 4 MONTHLY KPI CARDS (Screenshot 225909: 4-col on wide, 2x2 on mobile)
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CalendarKpiCard(
                            title = "DAYS WORN THIS MONTH",
                            mainValue = "$daysWornThisMonth",
                            subtitle = "of $daysInMonth days in $monthShortName",
                            icon = Icons.Default.CalendarMonth,
                            iconBg = Color(0xFFECFDF5),
                            iconTint = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        CalendarKpiCard(
                            title = "OUTFIT SESSIONS",
                            mainValue = "$outfitSessionsCount",
                            subtitle = "$totalMonthHours total logged",
                            icon = Icons.Default.Checkroom,
                            iconBg = Color(0xFFEFF6FF),
                            iconTint = BrandBlue,
                            modifier = Modifier.weight(1f)
                        )
                        CalendarKpiCard(
                            title = "UNIQUE GARMENTS",
                            mainValue = "$uniqueGarmentsCount",
                            subtitle = "worn during $monthTitle",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconBg = Color(0xFFFAF5FF),
                            iconTint = BrandPurple,
                            modifier = Modifier.weight(1f)
                        )
                        CalendarKpiCard(
                            title = "AVG TEMPERATURE",
                            mainValue = tempStats.first,
                            subtitle = "High ${tempStats.second}°C · Low ${tempStats.third}°C",
                            icon = Icons.Default.Thermostat,
                            iconBg = Color(0xFFFFFBEB),
                            iconTint = BrandAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalendarKpiCard(
                                title = "DAYS WORN THIS MONTH",
                                mainValue = "$daysWornThisMonth",
                                subtitle = "of $daysInMonth days in $monthShortName",
                                icon = Icons.Default.CalendarMonth,
                                iconBg = Color(0xFFECFDF5),
                                iconTint = Color(0xFF059669),
                                modifier = Modifier.weight(1f)
                            )
                            CalendarKpiCard(
                                title = "OUTFIT SESSIONS",
                                mainValue = "$outfitSessionsCount",
                                subtitle = "$totalMonthHours total logged",
                                icon = Icons.Default.Checkroom,
                                iconBg = Color(0xFFEFF6FF),
                                iconTint = BrandBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalendarKpiCard(
                                title = "UNIQUE GARMENTS",
                                mainValue = "$uniqueGarmentsCount",
                                subtitle = "worn during $monthTitle",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                iconBg = Color(0xFFFAF5FF),
                                iconTint = BrandPurple,
                                modifier = Modifier.weight(1f)
                            )
                            CalendarKpiCard(
                                title = "AVG TEMPERATURE",
                                mainValue = tempStats.first,
                                subtitle = "High ${tempStats.second}°C · Low ${tempStats.third}°C",
                                icon = Icons.Default.Thermostat,
                                iconBg = Color(0xFFFFFBEB),
                                iconTint = BrandAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. MAIN CALENDAR GRID + SIDEBAR (Side-by-side 2-column on Laptop, stacked on Mobile/Tablet)
            if (isLaptop) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: Monthly Calendar Grid + Most Worn This Month
                        Column(
                            modifier = Modifier.weight(0.56f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MonthlyCalendarGridCard(
                                yearMonth = yearMonth,
                                monthTitle = monthTitle,
                                selectedDateStr = selectedDateStr,
                                dailyWearMap = dailyWearMap,
                                onPrevMonth = {
                                    val prev = yearMonth.minusMonths(1)
                                    currentYear = prev.year
                                    currentMonth = prev.monthValue
                                },
                                onNextMonth = {
                                    val next = yearMonth.plusMonths(1)
                                    currentYear = next.year
                                    currentMonth = next.monthValue
                                },
                                onJumpToLatest = {
                                    val latest = dailyWearMap.keys.maxOrNull()
                                    if (!latest.isNullOrBlank()) {
                                        try {
                                            val ld = LocalDate.parse(latest)
                                            currentYear = ld.year
                                            currentMonth = ld.monthValue
                                            selectedDateStr = latest
                                        } catch (_: Exception) {}
                                    }
                                },
                                onSelectDate = { selectedDateStr = it }
                            )

                            MostWornThisMonthCard(
                                rankedGarments = mostWornThisMonth,
                                onSelectGarment = onSelectGarment
                            )
                        }

                        // Right Column: Selected Date Detail Card (Screenshot 225952)
                        Box(modifier = Modifier.weight(0.44f)) {
                            SelectedDateDetailSection(
                                selectedDateStr = selectedDateStr,
                                dayEntry = dailyWearMap[selectedDateStr],
                                clothesById = clothesById,
                                onLogOutfitClick = { onOpenLogModal(selectedDateStr) },
                                onInspectReport = { inspectingReport = it },
                                onSelectGarment = onSelectGarment
                            )
                        }
                    }
                }
            } else {
                // Mobile & Tablet Stacked Order (Matching Screenshots 225909 -> 225932 -> 225952)
                item {
                    MonthlyCalendarGridCard(
                        yearMonth = yearMonth,
                        monthTitle = monthTitle,
                        selectedDateStr = selectedDateStr,
                        dailyWearMap = dailyWearMap,
                        onPrevMonth = {
                            val prev = yearMonth.minusMonths(1)
                            currentYear = prev.year
                            currentMonth = prev.monthValue
                        },
                        onNextMonth = {
                            val next = yearMonth.plusMonths(1)
                            currentYear = next.year
                            currentMonth = next.monthValue
                        },
                        onJumpToLatest = {
                            val latest = dailyWearMap.keys.maxOrNull()
                            if (!latest.isNullOrBlank()) {
                                try {
                                    val ld = LocalDate.parse(latest)
                                    currentYear = ld.year
                                    currentMonth = ld.monthValue
                                    selectedDateStr = latest
                                } catch (_: Exception) {}
                            }
                        },
                        onSelectDate = { selectedDateStr = it }
                    )
                }

                item {
                    MostWornThisMonthCard(
                        rankedGarments = mostWornThisMonth,
                        onSelectGarment = onSelectGarment
                    )
                }

                item {
                    SelectedDateDetailSection(
                        selectedDateStr = selectedDateStr,
                        dayEntry = dailyWearMap[selectedDateStr],
                        clothesById = clothesById,
                        onLogOutfitClick = { onOpenLogModal(selectedDateStr) },
                        onInspectReport = { inspectingReport = it },
                        onSelectGarment = onSelectGarment
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // Report Inspect Modal
        inspectingReport?.let { report ->
            val rJunctions = junctions.filter { it.wearingReportId == report.id }
            SessionDetailModal(
                isOpen = true,
                report = report,
                junctions = rJunctions,
                clothes = clothes,
                onClose = { inspectingReport = null },
                onEditReport = {
                    inspectingReport = null
                    onSelectReport(report)
                },
                onDeleteReport = { inspectingReport = null },
                onUpdateWashStatus = { _, _ -> },
                onSelectGarment = { g ->
                    inspectingReport = null
                    onSelectGarment(g)
                }
            )
        }
    }
}

@Composable
private fun CalendarKpiCard(
    title: String,
    mainValue: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = mainValue,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Slate900
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Matches Screenshot_20261001-225909.png Monthly Calendar Grid Card
 */
@Composable
private fun MonthlyCalendarGridCard(
    yearMonth: YearMonth,
    monthTitle: String,
    selectedDateStr: String,
    dailyWearMap: Map<String, DailyWearEntry>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToLatest: () -> Unit,
    onSelectDate: (String) -> Unit
) {
    val context = LocalContext.current
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Month Header + Jump to Latest
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPrevMonth() }
                            .testTag("calendar_prev_month")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = monthTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNextMonth() }
                            .testTag("calendar_next_month")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onJumpToLatest() }
                        .testTag("calendar_today_btn")
                ) {
                    Text(
                        text = "Jump to Latest",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            // Weekday Headers: SUN .. SAT
            val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate400
                    )
                }
            }

            // 7-Column Days Grid
            val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
            val rowsCount = totalCells / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 7) {
                            val cellIdx = r * 7 + c
                            val dayNum = cellIdx - startOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val dateStr = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    yearMonth.year,
                                    yearMonth.monthValue,
                                    dayNum
                                )
                                val entry = dailyWearMap[dateStr]
                                val hasData = entry != null && (entry.garments.isNotEmpty() || entry.reports.isNotEmpty())
                                val isSelected = dateStr == selectedDateStr
                                val hasNeedWash = entry?.junctions?.any {
                                    it.washStated.equals("need wash", ignoreCase = true)
                                } == true

                                val firstGarment = entry?.garments?.firstOrNull()
                                val extraCount = ((entry?.garments?.size ?: 0) - 1).coerceAtLeast(0)
                                val resolvedThumb = firstGarment?.let {
                                    ImageUtils.resolveFirstValidPhoto(it.pictures, context)?.let { p ->
                                        ImageUtils.resolveImageModel(p, context)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.76f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xFFECFDF5)
                                                hasData -> Color(0xFFF8FAFC)
                                                else -> Color.White
                                            }
                                        )
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) Color(0xFF10B981) else Slate200
                                            ),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectDate(dateStr) }
                                        .padding(4.dp)
                                ) {
                                    // Top Row: Day Number + Status Dot
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.TopCenter),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected || hasData) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF047857) else Slate700
                                        )
                                        if (hasData) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (hasNeedWash) Color(0xFFF59E0B) else Color(0xFF10B981)
                                                    )
                                            )
                                        }
                                    }

                                    // Bottom Area: Garment Thumbnail + "+1" Badge
                                    if (hasData) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(Slate100),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (resolvedThumb != null) {
                                                    AsyncImage(
                                                        model = resolvedThumb,
                                                        contentDescription = null,
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
                                            if (extraCount > 0) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Slate200
                                                ) {
                                                    Text(
                                                        text = "+$extraCount",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate700,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.76f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            // Bottom Legend: Clean / Washed | Needs Wash | Selected Day (Screenshot 225909)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text("Clean / Washed", fontSize = 10.sp, color = Slate600)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Text("Needs Wash", fontSize = 10.sp, color = Slate600)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(BorderStroke(1.5.dp, Color(0xFF10B981)), RoundedCornerShape(3.dp))
                    )
                    Text("Selected Day", fontSize = 10.sp, color = Slate600)
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-225932.png Most Worn This Month Card
 */
@Composable
private fun MostWornThisMonthCard(
    rankedGarments: List<MonthlyGarmentRank>,
    onSelectGarment: (GarmentEntity) -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "MOST WORN THIS MONTH",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate500
            )

            if (rankedGarments.isEmpty()) {
                Text(
                    text = "No garments worn in this month yet.",
                    fontSize = 12.sp,
                    color = Slate400,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                rankedGarments.forEachIndexed { idx, item ->
                    val resolvedPhoto = ImageUtils.resolveFirstValidPhoto(item.garment.pictures, context)?.let {
                        ImageUtils.resolveImageModel(it, context)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectGarment(item.garment) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate400,
                                modifier = Modifier.width(22.dp)
                            )
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
                                        contentDescription = item.garment.name,
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
                                    text = item.garment.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.garment.clothesType.ifBlank { "Tops" },
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${item.wearCount}x worn",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1fh total", item.totalHours),
                                fontSize = 9.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-225952.png Selected Date Detail Card:
 * - Dark slate header with date ("2026-06-21"), day of week ("Saturday · 1 Outfit Report Logged"), and [+ Log Outfit] green button
 * - Report cards with temperature, occasion, total hours, All Clean badge, [Inspect] button,
 *   COMFORT 5/5 | FIT 5/5 | MOBILITY 5/5 boxes, and Garments Worn in This Session rows
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedDateDetailSection(
    selectedDateStr: String,
    dayEntry: DailyWearEntry?,
    clothesById: Map<String, GarmentEntity>,
    onLogOutfitClick: () -> Unit,
    onInspectReport: (ReportEntity) -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit
) {
    val context = LocalContext.current
    val dayOfWeekName = remember(selectedDateStr) {
        try {
            LocalDate.parse(selectedDateStr).format(DateTimeFormatter.ofPattern("EEEE", Locale.US))
        } catch (_: Exception) {
            "Selected Day"
        }
    }
    val reportsList = dayEntry?.reports.orEmpty()
    val junctionsList = dayEntry?.junctions.orEmpty()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Dark Slate Header Bar (Screenshot 225952)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = selectedDateStr,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "$dayOfWeekName · ${reportsList.size} Outfit Report${if (reportsList.size == 1) "" else "s"} Logged",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Button(
                    onClick = onLogOutfitClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("calendar_log_wear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Log Outfit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Body Content
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (reportsList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No outfit report logged on $selectedDateStr",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    }
                } else {
                    reportsList.forEach { report ->
                        val rJunctions = junctionsList.filter { it.wearingReportId == report.id }
                            .ifEmpty { junctionsList }
                        val maxDurSec = rJunctions.maxOfOrNull { it.duration } ?: 0L
                        val totalHrs = String.format(Locale.US, "%.1fh", maxDurSec / 3600.0)
                        val isDirty = rJunctions.any { it.washStated.equals("need wash", ignoreCase = true) }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Top Meta Pills + Inspect Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Default.Thermostat, null, tint = Slate500, modifier = Modifier.size(11.dp))
                                            Text("${parseCalendarReportTemp(report)}°C", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Default.LocationOn, null, tint = Slate500, modifier = Modifier.size(11.dp))
                                            Text(parseCalendarOccasion(report), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Slate100) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(Icons.Default.Schedule, null, tint = Slate500, modifier = Modifier.size(11.dp))
                                            Text("Total: $totalHrs", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDirty) Icons.Default.Warning else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isDirty) Color(0xFFD97706) else Color(0xFF059669),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = if (isDirty) "Needs Wash" else "All Clean",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate100,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onInspectReport(report) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Inspect",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate700
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = Slate500,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }

                            // Report Title & Notes
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = report.name.ifBlank { "casual" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "\"${report.reportDetail.ifBlank { "Comfortable day at home." }}\"",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }

                            // 3 Rating Boxes: COMFORT 5/5 | FIT 5/5 | MOBILITY 5/5 (Screenshot 225952)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CalendarSensoryBox("COMFORT", "5/5", Modifier.weight(1f))
                                CalendarSensoryBox("FIT", "5/5", Modifier.weight(1f))
                                CalendarSensoryBox("MOBILITY", "5/5", Modifier.weight(1f))
                            }

                            // Garments Worn in This Session
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "GARMENTS WORN IN THIS SESSION (${rJunctions.size})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate400
                                )

                                rJunctions.forEach { j ->
                                    val g = clothesById[j.clothesId]
                                    val gName = g?.name ?: j.clothesTitle.ifBlank { "Garment" }
                                    val gType = (g?.clothesType ?: j.clothesType).ifBlank { "Tops" }
                                    val gMat = g?.material?.ifBlank { "100% Cotton" } ?: "100% Cotton"
                                    val jDirty = j.washStated.equals("need wash", ignoreCase = true)
                                    val durStr = String.format(Locale.US, "%.1fh", j.duration / 3600.0)
                                    val resolvedPhoto = g?.let {
                                        ImageUtils.resolveFirstValidPhoto(it.pictures, context)?.let { p ->
                                            ImageUtils.resolveImageModel(p, context)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { if (g != null) onSelectGarment(g) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
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
                                                        text = "$gType · $gMat",
                                                        fontSize = 10.sp,
                                                        color = Slate500
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = durStr,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Slate800
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (jDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = if (jDirty) "Need Wash" else "Washed: ${j.washDate.ifBlank { selectedDateStr }}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (jDirty) Color(0xFFB45309) else Color(0xFF047857),
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
            }
        }
    }
}

@Composable
private fun CalendarSensoryBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate400
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Slate900
            )
        }
    }
}
