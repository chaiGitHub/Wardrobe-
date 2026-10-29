package com.example.util

import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

data class CalendarDay(
    val dayOfMonth: Int,
    val dateString: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val wearCount: Int,
    val totalDurationSeconds: Long,
    val hasUnwashed: Boolean,
    val garmentThumbnails: List<String> = emptyList()
)

data class CalendarMonthMatrix(
    val year: Int,
    val month: Int,
    val monthName: String,
    val weeks: List<List<CalendarDay>>,
    val totalMonthWears: Int,
    val activeDaysCount: Int
)

data class DayWearSummary(
    val dateString: String,
    val formattedDate: String,
    val sessions: List<JunctionEntity>,
    val garments: List<GarmentEntity>,
    val totalDurationSeconds: Long,
    val totalWears: Int,
    val hasUnwashed: Boolean
)

/**
 * Data structures ported directly from web app calendarCalculations.ts (Lb, Pb, Vb)
 */
data class DailyWearEntry(
    val dateKey: String,
    val reports: List<ReportEntity>,
    val junctions: List<JunctionEntity>,
    val garments: List<GarmentEntity>,
    val totalDurationHours: Double
)

data class ContributingSession(
    val junctionId: String,
    val reportId: String,
    val reportTitle: String,
    val wearDate: String,
    val durationHours: Double,
    val report: ReportEntity? = null
)

data class WashedGarmentSummary(
    val garment: GarmentEntity,
    val washDate: String,
    val accumulatedWearHours: Double,
    val wearSessionsCount: Int,
    val contributingSessions: List<ContributingSession>
)

data class WashHistoryDayEntry(
    val dateKey: String,
    val washedGarments: List<WashedGarmentSummary>,
    val totalWashedCount: Int
)

enum class LifecycleDayStatus {
    WORN_DIRTY,
    WORN_AND_WASHED,
    WASHED_CLEAN,
    UNWASHED_WAITING
}

data class GarmentDayLifecycle(
    val dateKey: String,
    val status: LifecycleDayStatus,
    val wearHoursOnDay: Double,
    val cumulativeDirtyHours: Double,
    val sessions: List<ContributingSession>,
    val washEvent: Boolean,
    val dayInCycle: Int = 1
)

data class GarmentLifecycleSummary(
    val garmentId: String,
    val dayStatusMap: Map<String, GarmentDayLifecycle>,
    val totalWears: Int,
    val totalWashes: Int,
    val currentDirtyHours: Double,
    val currentDirtySessions: Int,
    val waitingDays: Long = 0L,
    val isCurrentlyUnwashed: Boolean = false
)

object CalendarCalculations {

    private val ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    private fun round1Decimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }

    fun resolveJunctionWearDate(
        junction: JunctionEntity,
        reportsById: Map<String, ReportEntity>
    ): String {
        val reportDate = reportsById[junction.wearingReportId]?.let {
            it.wearDate.ifBlank { it.created }
        }.orEmpty()
        val raw = if (reportDate.isNotBlank()) reportDate else junction.wearDate
        return normalizeDate(raw)
    }

    /**
     * Port of Lb(reports, junctions, clothes) from calendarCalculations.ts
     * Builds a map of YYYY-MM-DD -> DailyWearEntry
     */
    fun buildDailyWearMap(
        reports: List<ReportEntity>,
        junctions: List<JunctionEntity>,
        clothes: List<GarmentEntity>
    ): Map<String, DailyWearEntry> {
        val reportsById = reports.associateBy { it.id }
        val clothesById = clothes.associateBy { it.id }

        val tempReports = mutableMapOf<String, MutableList<ReportEntity>>()
        val tempJunctions = mutableMapOf<String, MutableList<JunctionEntity>>()
        val tempGarments = mutableMapOf<String, MutableList<GarmentEntity>>()
        val tempHours = mutableMapOf<String, Double>()

        for (r in reports) {
            val dKey = normalizeDate(r.wearDate.ifBlank { r.created })
            if (dKey.isBlank()) continue
            tempReports.getOrPut(dKey) { mutableListOf() }.add(r)
            tempJunctions.putIfAbsent(dKey, mutableListOf())
            tempGarments.putIfAbsent(dKey, mutableListOf())
            tempHours.putIfAbsent(dKey, 0.0)
        }

        for (j in junctions) {
            val dKey = resolveJunctionWearDate(j, reportsById)
            if (dKey.isBlank()) continue

            val rep = reportsById[j.wearingReportId]
            if (rep != null) {
                val rList = tempReports.getOrPut(dKey) { mutableListOf() }
                if (rList.none { it.id == rep.id }) {
                    rList.add(rep)
                }
            }

            tempJunctions.getOrPut(dKey) { mutableListOf() }.add(j)
            val durHours = j.duration / 3600.0
            tempHours[dKey] = (tempHours[dKey] ?: 0.0) + durHours

            val g = clothesById[j.clothesId]
            if (g != null) {
                val gList = tempGarments.getOrPut(dKey) { mutableListOf() }
                if (gList.none { it.id == g.id }) {
                    gList.add(g)
                }
            }
        }

        val allDates = (tempReports.keys + tempJunctions.keys).distinct()
        val result = mutableMapOf<String, DailyWearEntry>()
        for (dKey in allDates) {
            result[dKey] = DailyWearEntry(
                dateKey = dKey,
                reports = tempReports[dKey] ?: emptyList(),
                junctions = tempJunctions[dKey] ?: emptyList(),
                garments = tempGarments[dKey] ?: emptyList(),
                totalDurationHours = round1Decimal(tempHours[dKey] ?: 0.0)
            )
        }
        return result
    }

    /**
     * Port of Pb(junctions, clothes, reports) from calendarCalculations.ts
     * Builds a map of YYYY-MM-DD (washDate) -> WashHistoryDayEntry
     */
    fun buildWashHistoryMap(
        junctions: List<JunctionEntity>,
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity>
    ): Map<String, WashHistoryDayEntry> {
        val clothesById = clothes.associateBy { it.id }
        val reportsById = reports.associateBy { it.id }

        val summariesByDate = mutableMapOf<String, MutableList<WashedGarmentSummary>>()

        // Group junctions by garment
        val junctionsByGarment = junctions.groupBy { it.clothesId }

        for ((clothesId, gJunctions) in junctionsByGarment) {
            val garment = clothesById[clothesId] ?: continue

            // Group washed junctions by their wash date
            val washedByDate = mutableMapOf<String, MutableList<JunctionEntity>>()
            for (j in gJunctions) {
                val isDirty = j.washStated.equals("need wash", ignoreCase = true)
                val rawWashDate = j.washDate.ifBlank { j.washedDate }
                val wDate = normalizeDate(rawWashDate)
                if (!isDirty && wDate.isNotBlank()) {
                    washedByDate.getOrPut(wDate) { mutableListOf() }.add(j)
                }
            }

            for ((washDateKey, washedSessions) in washedByDate) {
                val contributing = washedSessions.map { j ->
                    val rep = reportsById[j.wearingReportId]
                    val wDate = resolveJunctionWearDate(j, reportsById)
                    ContributingSession(
                        junctionId = j.id,
                        reportId = j.wearingReportId,
                        reportTitle = rep?.name?.ifBlank { j.wearingReportTitle } ?: j.wearingReportTitle.ifBlank { "Wear Session" },
                        wearDate = wDate,
                        durationHours = round1Decimal(j.duration / 3600.0),
                        report = rep
                    )
                }.sortedBy { it.wearDate }

                val totalHours = round1Decimal(washedSessions.sumOf { it.duration } / 3600.0)
                val summary = WashedGarmentSummary(
                    garment = garment,
                    washDate = washDateKey,
                    accumulatedWearHours = totalHours,
                    wearSessionsCount = contributing.size,
                    contributingSessions = contributing
                )
                summariesByDate.getOrPut(washDateKey) { mutableListOf() }.add(summary)
            }
        }

        // Also include garments that have a lastWashed date not yet represented
        for (garment in clothes) {
            val lw = normalizeDate(garment.lastWashed)
            if (lw.isNotBlank()) {
                val list = summariesByDate.getOrPut(lw) { mutableListOf() }
                if (list.none { it.garment.id == garment.id }) {
                    list.add(
                        WashedGarmentSummary(
                            garment = garment,
                            washDate = lw,
                            accumulatedWearHours = 0.0,
                            wearSessionsCount = 0,
                            contributingSessions = emptyList()
                        )
                    )
                }
            }
        }

        return summariesByDate.mapValues { (dateKey, list) ->
            WashHistoryDayEntry(
                dateKey = dateKey,
                washedGarments = list,
                totalWashedCount = list.size
            )
        }
    }

    /**
     * Port of Vb(garmentId, junctions, reports, garment) from calendarCalculations.ts
     * Builds the single-garment lifecycle map and stats for Mode 3 ("Single Garment Lifecycle")
     */
    fun buildGarmentLifecycleMap(
        garmentId: String,
        junctions: List<JunctionEntity>,
        reports: List<ReportEntity>,
        garment: GarmentEntity?
    ): GarmentLifecycleSummary {
        val reportsById = reports.associateBy { it.id }
        val gJunctions = junctions.filter { it.clothesId == garmentId }

        data class InternalSession(
            val junction: JunctionEntity,
            val wearDate: String,
            val washDate: String,
            val isNeedWash: Boolean,
            val sessionObj: ContributingSession
        )

        val internalSessions = gJunctions.map { j ->
            val rep = reportsById[j.wearingReportId]
            val wearDate = resolveJunctionWearDate(j, reportsById)
            val rawWash = normalizeDate(j.washDate.ifBlank { j.washedDate })
            val isNeedWash = j.washStated.equals("need wash", ignoreCase = true)
            var washDate = if (!isNeedWash) {
                if (rawWash.isNotBlank()) rawWash
                else if (garment != null && garment.lastWashed.isNotBlank()) normalizeDate(garment.lastWashed)
                else ""
            } else ""

            // Safety check: a wash date cannot be before the wear date
            if (washDate.isNotBlank() && wearDate.isNotBlank() && washDate < wearDate) {
                washDate = ""
            }

            InternalSession(
                junction = j,
                wearDate = wearDate,
                washDate = washDate,
                isNeedWash = isNeedWash,
                sessionObj = ContributingSession(
                    junctionId = j.id,
                    reportId = j.wearingReportId,
                    reportTitle = rep?.name?.ifBlank { j.wearingReportTitle } ?: j.wearingReportTitle.ifBlank { "Wear Session" },
                    wearDate = wearDate,
                    durationHours = round1Decimal(j.duration / 3600.0),
                    report = rep
                )
            )
        }.sortedBy { it.wearDate }

        // Build wash cycles
        val dayStatusMap = mutableMapOf<String, GarmentDayLifecycle>()

        // 1. Group sessions into washed cycles and current unwashed cycle
        val washedSessionsByWashDate = mutableMapOf<String, MutableList<InternalSession>>()
        val unwashedSessions = mutableListOf<InternalSession>()

        for (s in internalSessions) {
            if (!s.isNeedWash && s.washDate.isNotBlank()) {
                washedSessionsByWashDate.getOrPut(s.washDate) { mutableListOf() }.add(s)
            } else {
                unwashedSessions.add(s)
            }
        }

        // Helper to populate a contiguous date range for a cycle
        fun populateCycleDays(
            sessionsInCycle: List<InternalSession>,
            washDate: String?
        ) {
            if (sessionsInCycle.isEmpty() && washDate.isNullOrBlank()) return

            val wearDates = sessionsInCycle.map { it.wearDate }.filter { it.isNotBlank() }
            val firstWearDate = wearDates.minOrNull()
            if (firstWearDate == null && !washDate.isNullOrBlank()) {
                // Standalone wash event without logged wear sessions
                val washedObj = sessionsInCycle.map { it.sessionObj }
                dayStatusMap[washDate] = GarmentDayLifecycle(
                    dateKey = washDate,
                    status = LifecycleDayStatus.WASHED_CLEAN,
                    wearHoursOnDay = 0.0,
                    cumulativeDirtyHours = 0.0,
                    sessions = washedObj,
                    washEvent = true,
                    dayInCycle = 1
                )
                return
            }

            if (firstWearDate == null) return

            val startLd = try { LocalDate.parse(firstWearDate) } catch (_: Exception) { return }
            val endLd = if (!washDate.isNullOrBlank()) {
                try { LocalDate.parse(washDate) } catch (_: Exception) { startLd }
            } else {
                val latestWearLd = try { LocalDate.parse(wearDates.maxOrNull() ?: firstWearDate) } catch (_: Exception) { startLd }
                val today = LocalDate.now()
                if (latestWearLd.isAfter(today)) latestWearLd else today
            }

            var currLd = startLd
            var runningDirtyHours = 0.0

            while (!currLd.isAfter(endLd)) {
                val dKey = currLd.format(ISO_DATE_FORMATTER)
                val dayInCycle = (ChronoUnit.DAYS.between(startLd, currLd) + 1).toInt()

                val wornToday = sessionsInCycle.filter { it.wearDate == dKey }
                val wearHoursToday = round1Decimal(wornToday.sumOf { it.junction.duration } / 3600.0)
                runningDirtyHours = round1Decimal(runningDirtyHours + wearHoursToday)

                val isWashDay = washDate != null && washDate == dKey
                val status = when {
                    wornToday.isNotEmpty() && isWashDay -> LifecycleDayStatus.WORN_AND_WASHED
                    isWashDay -> LifecycleDayStatus.WASHED_CLEAN
                    wornToday.isNotEmpty() -> LifecycleDayStatus.WORN_DIRTY
                    else -> LifecycleDayStatus.UNWASHED_WAITING
                }

                // Cumulative sessions active in this cycle up to this day
                val activeSessions = sessionsInCycle
                    .filter { it.wearDate.isNotBlank() && it.wearDate <= dKey }
                    .map { it.sessionObj }
                    .distinctBy { it.junctionId }

                dayStatusMap[dKey] = GarmentDayLifecycle(
                    dateKey = dKey,
                    status = status,
                    wearHoursOnDay = wearHoursToday,
                    cumulativeDirtyHours = runningDirtyHours,
                    sessions = activeSessions,
                    washEvent = isWashDay,
                    dayInCycle = dayInCycle
                )

                if (isWashDay) {
                    runningDirtyHours = 0.0
                }

                currLd = currLd.plusDays(1)
            }
        }

        // Populate completed washed cycles
        for ((wDate, wSessions) in washedSessionsByWashDate) {
            populateCycleDays(wSessions, wDate)
        }

        // Also check if garment has lastWashed that wasn't in washedSessionsByWashDate
        garment?.lastWashed?.let { lw ->
            val normLw = normalizeDate(lw)
            if (normLw.isNotBlank() && !dayStatusMap.containsKey(normLw)) {
                dayStatusMap[normLw] = GarmentDayLifecycle(
                    dateKey = normLw,
                    status = LifecycleDayStatus.WASHED_CLEAN,
                    wearHoursOnDay = 0.0,
                    cumulativeDirtyHours = 0.0,
                    sessions = emptyList(),
                    washEvent = true,
                    dayInCycle = 1
                )
            }
        }

        // Populate current open unwashed cycle
        if (unwashedSessions.isNotEmpty()) {
            populateCycleDays(unwashedSessions, null)
        }

        val dirtyJunctions = gJunctions.filter { it.washStated.equals("need wash", ignoreCase = true) }
        val currentDirtyHours = round1Decimal(dirtyJunctions.sumOf { it.duration } / 3600.0)
        val currentDirtySessions = dirtyJunctions.size

        val earliestUnwashedWear = unwashedSessions.map { it.wearDate }.filter { it.isNotBlank() }.minOrNull()
        val waitingDays = if (!earliestUnwashedWear.isNullOrBlank()) {
            try {
                val startLd = LocalDate.parse(earliestUnwashedWear)
                maxOf(1L, ChronoUnit.DAYS.between(startLd, LocalDate.now()) + 1)
            } catch (_: Exception) {
                unwashedSessions.size.toLong()
            }
        } else 0L

        val distinctWashDates = (washedSessionsByWashDate.keys + listOfNotNull(garment?.lastWashed?.let { normalizeDate(it).takeIf { d -> d.isNotBlank() } })).distinct().size
        val totalWashes = if (garment != null) {
            maxOf(garment.wearingFrequency + garment.trackedAppWashes, distinctWashDates)
        } else {
            distinctWashDates
        }

        return GarmentLifecycleSummary(
            garmentId = garmentId,
            dayStatusMap = dayStatusMap,
            totalWears = gJunctions.size,
            totalWashes = totalWashes,
            currentDirtyHours = currentDirtyHours,
            currentDirtySessions = currentDirtySessions,
            waitingDays = waitingDays,
            isCurrentlyUnwashed = dirtyJunctions.isNotEmpty()
        )
    }

    /**
     * Builds a 7x(4-6) calendar grid for a given year and month.
     * Starts on Monday (standard Monday start with Sunday at 7).
     */
    fun calculateMonthMatrix(
        year: Int,
        month: Int,
        junctions: List<JunctionEntity>,
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity> = emptyList()
    ): CalendarMonthMatrix {
        val yearMonth = YearMonth.of(year, month)
        val firstDayOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val today = LocalDate.now()

        val reportsById = reports.associateBy { it.id }
        val wearByDate = junctions.groupBy { resolveJunctionWearDate(it, reportsById) }
        val clothesMap = clothes.associateBy { it.id }

        val weeks = mutableListOf<List<CalendarDay>>()
        var currentWeek = mutableListOf<CalendarDay>()

        val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1..7 (Monday=1, Sunday=7)
        val leadingDaysCount = (startDayOfWeek - 1)

        val prevYearMonth = yearMonth.minusMonths(1)
        val prevMonthDays = prevYearMonth.lengthOfMonth()
        for (i in (prevMonthDays - leadingDaysCount + 1)..prevMonthDays) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", prevYearMonth.year, prevYearMonth.monthValue, i)
            val dayJunctions = wearByDate[dateStr].orEmpty()
            currentWeek.add(
                CalendarDay(
                    dayOfMonth = i,
                    dateString = dateStr,
                    isCurrentMonth = false,
                    isToday = dateStr == today.format(ISO_DATE_FORMATTER),
                    wearCount = dayJunctions.size,
                    totalDurationSeconds = dayJunctions.sumOf { it.duration },
                    hasUnwashed = dayJunctions.any { it.washStated.equals("need wash", ignoreCase = true) }
                )
            )
        }

        var totalMonthWears = 0
        var activeDays = 0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dateStr = date.format(ISO_DATE_FORMATTER)
            val dayJunctions = wearByDate[dateStr].orEmpty()
            val hasUnwashed = dayJunctions.any { it.washStated.equals("need wash", ignoreCase = true) }
            val wearCount = dayJunctions.size
            val durationSec = dayJunctions.sumOf { it.duration }

            if (wearCount > 0) {
                totalMonthWears += wearCount
                activeDays++
            }

            val thumbs: List<String> = dayJunctions.mapNotNull { j ->
                val garment = clothesMap[j.clothesId]
                garment?.let { ImageUtils.parsePhotoList(it.pictures).firstOrNull() }
            }.distinct()

            currentWeek.add(
                CalendarDay(
                    dayOfMonth = day,
                    dateString = dateStr,
                    isCurrentMonth = true,
                    isToday = date == today,
                    wearCount = wearCount,
                    totalDurationSeconds = durationSec,
                    hasUnwashed = hasUnwashed,
                    garmentThumbnails = thumbs
                )
            )

            if (currentWeek.size == 7) {
                weeks.add(currentWeek)
                currentWeek = mutableListOf()
            }
        }

        if (currentWeek.isNotEmpty()) {
            val nextYearMonth = yearMonth.plusMonths(1)
            var nextDay = 1
            while (currentWeek.size < 7) {
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", nextYearMonth.year, nextYearMonth.monthValue, nextDay)
                val dayJunctions = wearByDate[dateStr].orEmpty()
                currentWeek.add(
                    CalendarDay(
                        dayOfMonth = nextDay,
                        dateString = dateStr,
                        isCurrentMonth = false,
                        isToday = dateStr == today.format(ISO_DATE_FORMATTER),
                        wearCount = dayJunctions.size,
                        totalDurationSeconds = dayJunctions.sumOf { it.duration },
                        hasUnwashed = dayJunctions.any { it.washStated.equals("need wash", ignoreCase = true) }
                    )
                )
                nextDay++
            }
            weeks.add(currentWeek)
        }

        return CalendarMonthMatrix(
            year = year,
            month = month,
            monthName = firstDayOfMonth.format(MONTH_YEAR_FORMATTER),
            weeks = weeks,
            totalMonthWears = totalMonthWears,
            activeDaysCount = activeDays
        )
    }

    fun calculateDayWearSummary(
        dateString: String,
        junctions: List<JunctionEntity>,
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity> = emptyList()
    ): DayWearSummary {
        val normDate = normalizeDate(dateString)
        val reportsById = reports.associateBy { it.id }
        val dayJunctions = junctions.filter { resolveJunctionWearDate(it, reportsById) == normDate }
        val clothesMap = clothes.associateBy { it.id }
        val dayGarments = dayJunctions.mapNotNull { clothesMap[it.clothesId] }.distinctBy { it.id }

        val totalDuration = dayJunctions.sumOf { it.duration }
        val hasUnwashed = dayJunctions.any { it.washStated.equals("need wash", ignoreCase = true) }

        val formattedDate = try {
            val ld = LocalDate.parse(normDate, ISO_DATE_FORMATTER)
            ld.format(DISPLAY_DATE_FORMATTER)
        } catch (_: Exception) {
            normDate
        }

        return DayWearSummary(
            dateString = normDate,
            formattedDate = formattedDate,
            sessions = dayJunctions,
            garments = dayGarments,
            totalDurationSeconds = totalDuration,
            totalWears = dayJunctions.size,
            hasUnwashed = hasUnwashed
        )
    }

    fun calculateWearStreak(
        junctions: List<JunctionEntity>,
        reports: List<ReportEntity> = emptyList()
    ): Pair<Int, Int> {
        val reportsById = reports.associateBy { it.id }
        val datesWithWear = junctions
            .mapNotNull {
                try {
                    LocalDate.parse(resolveJunctionWearDate(it, reportsById), ISO_DATE_FORMATTER)
                } catch (_: Exception) {
                    null
                }
            }
            .distinct()
            .sortedDescending()

        if (datesWithWear.isEmpty()) return Pair(0, 0)

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        var currentStreak = 0
        val checkDate = if (datesWithWear.contains(today)) today else if (datesWithWear.contains(yesterday)) yesterday else null

        if (checkDate != null) {
            var curr: LocalDate? = checkDate
            while (curr != null && datesWithWear.contains(curr)) {
                currentStreak++
                curr = curr.minusDays(1)
            }
        }

        val ascendingDates = datesWithWear.sorted()
        var maxStreak = 0
        var streakCounter = 0
        var prevDate: LocalDate? = null

        for (d in ascendingDates) {
            if (prevDate == null || ChronoUnit.DAYS.between(prevDate, d) == 1L) {
                streakCounter++
            } else if (ChronoUnit.DAYS.between(prevDate, d) > 1L) {
                streakCounter = 1
            }
            if (streakCounter > maxStreak) {
                maxStreak = streakCounter
            }
            prevDate = d
        }

        return Pair(currentStreak, maxOf(currentStreak, maxStreak))
    }

    fun calculateGarmentRestDays(
        garmentId: String,
        junctions: List<JunctionEntity>,
        reports: List<ReportEntity> = emptyList()
    ): Int {
        val reportsById = reports.associateBy { it.id }
        val garmentWearDates = junctions
            .filter { it.clothesId == garmentId }
            .mapNotNull {
                try {
                    LocalDate.parse(resolveJunctionWearDate(it, reportsById), ISO_DATE_FORMATTER)
                } catch (_: Exception) {
                    null
                }
            }
            .sortedDescending()

        val lastWear = garmentWearDates.firstOrNull() ?: return -1
        return ChronoUnit.DAYS.between(lastWear, LocalDate.now()).toInt()
    }

    fun getHeatmapLevel(wearCount: Int): Int {
        return when {
            wearCount <= 0 -> 0
            wearCount == 1 -> 1
            wearCount == 2 -> 2
            else -> 3
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0m"
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun normalizeDate(dateStr: String): String {
        val trimmed = dateStr.trim()
        return if (trimmed.length >= 10) trimmed.substring(0, 10) else trimmed
    }
}
