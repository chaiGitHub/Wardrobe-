import { WearingReport, ReportClothesJunction, ClothesItem } from '../types';

export interface DailyGarmentWear {
  clothesId: string;
  garment?: ClothesItem;
  clothesTitle: string;
  clothesType: string;
  totalDurationSeconds: number;
  totalDurationHours: number;
  sessionCount: number;
  reportIds: string[];
  reportTitles: string[];
  reports: WearingReport[];
  hasNeedWash: boolean;
  allClean: boolean;
}

export interface DayWearSummary {
  dateKey: string; // YYYY-MM-DD
  items: DailyGarmentWear[];
  uniqueClothesCount: number;
  totalDayHours: number;
  hasNeedWash: boolean;
  allClean: boolean;
  reports: WearingReport[];
}

export interface WashEventItem {
  clothesId: string;
  garment?: ClothesItem;
  clothesTitle: string;
  clothesType: string;
  washDate: string; // YYYY-MM-DD
  accumulatedSeconds: number;
  accumulatedHours: number;
  sessionsCount: number;
  contributingSessions: Array<{
    reportId?: string;
    report?: WearingReport;
    wearDate: string;
    reportTitle: string;
    durationHours: number;
  }>;
}

export interface DayWashSummary {
  dateKey: string;
  washedItems: WashEventItem[];
  totalWashedCount: number;
  totalAccumulatedHours: number;
}

export interface GarmentCycleDayStatus {
  dateKey: string;
  status: 'idle' | 'wear_day' | 'unwashed_span' | 'wash_day';
  hoursWornToday?: number;
  daysSinceFirstWear?: number;
  accumulatedHoursSoFar?: number;
  cycleId?: string;
  reportTitles?: string[];
  reports?: WearingReport[];
}

export interface GarmentCycle {
  cycleId: string;
  firstWearDate: string;
  washDate: string | null; // null if currently in laundry basket
  wearDates: string[];
  totalDurationSeconds: number;
  totalDurationHours: number;
  isComplete: boolean;
  daysUnwashed: number;
}

/**
 * Normalizes any date string (ISO, YYYY-MM-DD, DD/MM/YYYY, etc.) to a timezone-safe pure YYYY-MM-DD key.
 * Never causes date shifting across timezones (e.g. UTC+8 or UTC-7).
 */
export function toDateKey(dateStr?: string | null): string | null {
  if (!dateStr || typeof dateStr !== 'string') return null;
  const trimmed = dateStr.trim();
  if (!trimmed) return null;

  // 1. ISO or YYYY-MM-DD prefix (e.g. "2026-09-13", "2026-09-13T00:00:00.000Z", "2026/09/13")
  const ymdMatch = trimmed.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})/);
  if (ymdMatch) {
    const y = ymdMatch[1];
    const m = String(parseInt(ymdMatch[2], 10)).padStart(2, '0');
    const d = String(parseInt(ymdMatch[3], 10)).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  // 2. Day/Month/Year (e.g. "13/09/2026", "13/9/2026", "13-09-2026")
  const dmyMatch = trimmed.match(/^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})/);
  if (dmyMatch) {
    const d = String(parseInt(dmyMatch[1], 10)).padStart(2, '0');
    const m = String(parseInt(dmyMatch[2], 10)).padStart(2, '0');
    const y = dmyMatch[3];
    return `${y}-${m}-${d}`;
  }

  // 3. Fallback standard Date parsing using local components
  const parsed = new Date(trimmed);
  if (!isNaN(parsed.getTime())) {
    const y = parsed.getFullYear();
    const m = String(parsed.getMonth() + 1).padStart(2, '0');
    const d = String(parsed.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  return null;
}

/**
 * Builds a pure YYYY-MM-DD date key from year, 0-indexed month, and day.
 * Eliminates all UTC timezone displacement bugs.
 */
export function buildDateKey(year: number, month0Indexed: number, day: number): string {
  const d = new Date(year, month0Indexed, day);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const dayStr = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${dayStr}`;
}

/**
 * Returns today's local date as YYYY-MM-DD.
 */
export function getTodayDateKey(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * Formats any date string or Date object for UI display without timezone displacement.
 * Example: "2026-09-13" -> "Sun, Sep 13, 2026"
 */
export function formatCalendarDate(
  dateStr?: string | null,
  options?: Intl.DateTimeFormatOptions
): string {
  const key = toDateKey(dateStr);
  if (!key) return 'No date';
  const [y, m, d] = key.split('-').map(Number);
  const localDate = new Date(y, m - 1, d);
  return localDate.toLocaleDateString(undefined, options || {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });
}

/**
 * Calculates whole calendar days between two YYYY-MM-DD date keys.
 */
export function calculateDaysBetween(startKey: string, endKey: string): number {
  const [y1, m1, d1] = startKey.split('-').map(Number);
  const [y2, m2, d2] = endKey.split('-').map(Number);
  const dStart = new Date(y1, m1 - 1, d1);
  const dEnd = new Date(y2, m2 - 1, d2);
  const diffMs = dEnd.getTime() - dStart.getTime();
  return Math.max(0, Math.round(diffMs / (1000 * 60 * 60 * 24)));
}

/**
 * Computes View 1: Daily Clothes Wear Summary strictly grouped by report['wear date'].
 * Combines duration for duplicate clothes worn in multiple reports on the same wear date.
 */
export function computeDailyWearSummaries(
  reports: WearingReport[],
  junctions: ReportClothesJunction[],
  clothes: ClothesItem[]
): Map<string, DayWearSummary> {
  const map = new Map<string, DayWearSummary>();

  // Map each junction to its parent report's wear date
  const reportById = new Map<string, WearingReport>();
  reports.forEach(r => reportById.set(r.id, r));

  // Map clothes by ID
  const clothesById = new Map<string, ClothesItem>();
  clothes.forEach(c => clothesById.set(c.id, c));

  // Group junctions by their report's wear date
  const junctionsByWearDate = new Map<string, Array<{ junction: ReportClothesJunction; report: WearingReport }>>();

  junctions.forEach(j => {
    const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
    const report = reportById.get(rId);
    if (!report) return;

    const wearDateKey = toDateKey(report['wear date']);
    if (!wearDateKey) return;

    const existing = junctionsByWearDate.get(wearDateKey) || [];
    existing.push({ junction: j, report });
    junctionsByWearDate.set(wearDateKey, existing);
  });

  // Also catch reports with no junctions just in case
  reports.forEach(report => {
    const wearDateKey = toDateKey(report['wear date']);
    if (!wearDateKey) return;
    if (!junctionsByWearDate.has(wearDateKey)) {
      junctionsByWearDate.set(wearDateKey, []);
    }
  });

  junctionsByWearDate.forEach((pairs, dateKey) => {
    const clothesWearMap = new Map<string, DailyGarmentWear>();
    const dateReportsSet = new Set<WearingReport>();

    // Add any reports belonging to this date
    reports.forEach(r => {
      if (toDateKey(r['wear date']) === dateKey) {
        dateReportsSet.add(r);
      }
    });

    pairs.forEach(({ junction, report }) => {
      dateReportsSet.add(report);
      const cId = typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].id : String(junction['clothes(new)']);
      const garment = clothesById.get(cId);
      const title = garment?.['Name'] || (typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].title : 'Garment');
      const type = garment?.['Clothes Type'] || junction['Clothes Type (from clothes(new))'] || 'Dress';
      const durationSec = junction['duration'] || 14400;
      const isNeedWash = junction['wash stated'] === 'need wash';

      const existing = clothesWearMap.get(cId);
      if (existing) {
        existing.totalDurationSeconds += durationSec;
        existing.totalDurationHours = Math.round((existing.totalDurationSeconds / 3600) * 10) / 10;
        existing.sessionCount += 1;
        if (!existing.reportIds.includes(report.id)) {
          existing.reportIds.push(report.id);
          existing.reportTitles.push(report['Name'] || 'Wear Session');
          existing.reports.push(report);
        }
        if (isNeedWash) existing.hasNeedWash = true;
        if (junction['wash stated'] !== 'wash') existing.allClean = false;
      } else {
        clothesWearMap.set(cId, {
          clothesId: cId,
          garment,
          clothesTitle: title,
          clothesType: type,
          totalDurationSeconds: durationSec,
          totalDurationHours: Math.round((durationSec / 3600) * 10) / 10,
          sessionCount: 1,
          reportIds: [report.id],
          reportTitles: [report['Name'] || 'Wear Session'],
          reports: [report],
          hasNeedWash: isNeedWash,
          allClean: junction['wash stated'] === 'wash'
        });
      }
    });

    const items = Array.from(clothesWearMap.values());
    const totalDaySec = items.reduce((acc, it) => acc + it.totalDurationSeconds, 0);
    const hasNeedWash = items.some(it => it.hasNeedWash);
    const allClean = items.length > 0 && items.every(it => it.allClean);

    map.set(dateKey, {
      dateKey,
      items,
      uniqueClothesCount: items.length,
      totalDayHours: Math.round((totalDaySec / 3600) * 10) / 10,
      hasNeedWash,
      allClean,
      reports: Array.from(dateReportsSet)
    });
  });

  return map;
}

/**
 * Computes View 2: Wash Events & Accumulated Wear Time
 * Measures the total accumulated duration for each garment washed on a particular date.
 */
export function computeDailyWashSummaries(
  reports: WearingReport[],
  junctions: ReportClothesJunction[],
  clothes: ClothesItem[]
): Map<string, DayWashSummary> {
  const map = new Map<string, DayWashSummary>();
  const clothesById = new Map<string, ClothesItem>();
  clothes.forEach(c => clothesById.set(c.id, c));

  const reportById = new Map<string, WearingReport>();
  reports.forEach(r => reportById.set(r.id, r));

  // Find all distinct wash dates recorded
  const washEventsByDate = new Map<string, Map<string, WashEventItem>>();

  // 1. Check junction records with 'wash date' or 'washed date'
  junctions.forEach(j => {
    // A junction only represents an actual wash event if 'wash stated' is 'wash'
    if (j['wash stated'] !== 'wash') return;

    const rawWashDate = j['wash date'] || j.washDate || j['washed date'] || j.washedDate;
    if (!rawWashDate) return;
    const washDateKey = toDateKey(rawWashDate);
    if (!washDateKey) return;

    const cId = (typeof j['clothes(new)'] === 'object' && j['clothes(new)']?.id) ? j['clothes(new)'].id : String(j['clothes(new)'] || j.clothesId || '');
    if (!cId || cId === 'undefined') return;
    const garment = clothesById.get(cId);
    const title = garment?.['Name'] || (typeof j['clothes(new)'] === 'object' && j['clothes(new)']?.title ? j['clothes(new)'].title : (j.clothesTitle || 'Garment'));
    const type = garment?.['Clothes Type'] || 'Dress';

    const repObj = j['wearing report id'];
    const rId = (typeof repObj === 'object' && repObj && 'id' in repObj)
      ? (repObj as { id: string; title?: string }).id
      : String(repObj || j.wearingReportId || '');
    const objTitle = (typeof repObj === 'object' && repObj && 'title' in repObj)
      ? (repObj as { id?: string; title: string }).title
      : undefined;
    const report = reportById.get(rId) || (objTitle ? reports.find(r => r['Name'] === objTitle) : undefined);
    const wearDateKey = report ? toDateKey(report['wear date']) || '' : '';
    const durationSec = j['duration'] || 14400;

    if (!washEventsByDate.has(washDateKey)) {
      washEventsByDate.set(washDateKey, new Map());
    }

    const dayGarments = washEventsByDate.get(washDateKey)!;
    const existing = dayGarments.get(cId);

    const reportTitle = report?.['Name'] || objTitle || j.wearingReportTitle || 'Wear Session';

    if (existing) {
      existing.accumulatedSeconds += durationSec;
      existing.accumulatedHours = Math.round((existing.accumulatedSeconds / 3600) * 10) / 10;
      existing.sessionsCount += 1;
      // Avoid duplicate contributing session entries for identical report
      if (!existing.contributingSessions.some(s => s.reportId === rId && s.reportTitle === reportTitle)) {
        existing.contributingSessions.push({
          reportId: rId,
          report,
          wearDate: wearDateKey,
          reportTitle,
          durationHours: Math.round((durationSec / 3600) * 10) / 10
        });
      }
    } else {
      dayGarments.set(cId, {
        clothesId: cId,
        garment,
        clothesTitle: title,
        clothesType: type,
        washDate: washDateKey,
        accumulatedSeconds: durationSec,
        accumulatedHours: Math.round((durationSec / 3600) * 10) / 10,
        sessionsCount: 1,
        contributingSessions: [{
          reportId: rId,
          report,
          wearDate: wearDateKey,
          reportTitle,
          durationHours: Math.round((durationSec / 3600) * 10) / 10
        }]
      });
    }
  });

  // Wash days are strictly determined by confirmed wash records in wearing reports/junctions
  // (j['wash stated'] === 'wash'). No phantom wash days are inferred from orphaned garment timestamps.

  // Convert to Map<string, DayWashSummary>
  washEventsByDate.forEach((garmentMap, dateKey) => {
    const washedItems = Array.from(garmentMap.values());
    const totalAccumulatedHours = washedItems.reduce((acc, it) => acc + it.accumulatedHours, 0);
    map.set(dateKey, {
      dateKey,
      washedItems,
      totalWashedCount: washedItems.length,
      totalAccumulatedHours: Math.round(totalAccumulatedHours * 10) / 10
    });
  });

  return map;
}

/**
 * Computes View 3: Single Garment Lifecycle (Wear Start -> Unwashed Span -> Washed/Clean)
 */
export function computeGarmentLifecycle(
  clothesId: string,
  reports: WearingReport[],
  junctions: ReportClothesJunction[],
  clothes: ClothesItem[]
): {
  garment?: ClothesItem;
  dayStatusMap: Map<string, GarmentCycleDayStatus>;
  cycles: GarmentCycle[];
  currentStatus: 'clean' | 'unwashed';
  currentUnwashedDays: number;
  currentUnwashedHours: number;
  currentUnwashedSessions: number;
} {
  const garment = clothes.find(c => c.id === clothesId);
  const reportById = new Map<string, WearingReport>();
  reports.forEach(r => reportById.set(r.id, r));

  // Find all junctions for this garment
  const garmentJunctions = junctions.filter(j => {
    const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
    return cId === clothesId;
  });

  // Collect wearing records: { wearDate: YYYY-MM-DD, durationSec, report, junction }
  const wearRecords: Array<{
    wearDateKey: string;
    durationSec: number;
    report: WearingReport;
    junction: ReportClothesJunction;
    washedDateKey: string | null;
    isNeedWash: boolean;
  }> = [];

  garmentJunctions.forEach(j => {
    const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
    const rep = reportById.get(rId);
    if (!rep) return;
    const wearDateKey = toDateKey(rep['wear date']);
    if (!wearDateKey) return;

    const isWashed = j['wash stated'] === 'wash';
    const rawWash = isWashed ? (j['wash date'] || j.washDate || j['washed date'] || j.washedDate) : null;
    const washedDateKey = isWashed
      ? (toDateKey(rawWash) || (garment?.['Last Washed'] ? toDateKey(garment['Last Washed']) : null))
      : null;

    wearRecords.push({
      wearDateKey,
      durationSec: j['duration'] || 14400,
      report: rep,
      junction: j,
      washedDateKey,
      isNeedWash: j['wash stated'] === 'need wash'
    });
  });

  // Sort wear records chronologically
  wearRecords.sort((a, b) => a.wearDateKey.localeCompare(b.wearDateKey));

  const dayStatusMap = new Map<string, GarmentCycleDayStatus>();
  const cycles: GarmentCycle[] = [];

  // Group into wear cycles
  // A cycle spans from first wear date -> unwashed span -> wash date
  let activeCycle: {
    cycleId: string;
    firstWearDate: string;
    washDate: string | null;
    wearDates: string[];
    totalDurationSeconds: number;
  } | null = null;

  wearRecords.forEach((rec, idx) => {
    if (!activeCycle) {
      activeCycle = {
        cycleId: `cycle_${rec.wearDateKey}_${idx}`,
        firstWearDate: rec.wearDateKey,
        washDate: rec.washedDateKey,
        wearDates: [rec.wearDateKey],
        totalDurationSeconds: rec.durationSec
      };
    } else {
      activeCycle.wearDates.push(rec.wearDateKey);
      activeCycle.totalDurationSeconds += rec.durationSec;
      if (rec.washedDateKey) {
        activeCycle.washDate = rec.washedDateKey;
      }
    }

    if (activeCycle.washDate && activeCycle.washDate >= rec.wearDateKey) {
      // Completed cycle
      const d1 = new Date(activeCycle.firstWearDate).getTime();
      const d2 = new Date(activeCycle.washDate).getTime();
      const daysUnwashed = Math.max(1, Math.round((d2 - d1) / (1000 * 60 * 60 * 24)));

      cycles.push({
        cycleId: activeCycle.cycleId,
        firstWearDate: activeCycle.firstWearDate,
        washDate: activeCycle.washDate,
        wearDates: activeCycle.wearDates,
        totalDurationSeconds: activeCycle.totalDurationSeconds,
        totalDurationHours: Math.round((activeCycle.totalDurationSeconds / 3600) * 10) / 10,
        isComplete: true,
        daysUnwashed
      });
      activeCycle = null;
    }
  });

  // If there is an ongoing unwashed cycle
  if (activeCycle) {
    const todayKey = getTodayDateKey();
    const daysUnwashed = Math.max(1, calculateDaysBetween((activeCycle as any).firstWearDate, todayKey));

    cycles.push({
      cycleId: (activeCycle as any).cycleId,
      firstWearDate: (activeCycle as any).firstWearDate,
      washDate: null,
      wearDates: (activeCycle as any).wearDates,
      totalDurationSeconds: (activeCycle as any).totalDurationSeconds,
      totalDurationHours: Math.round(((activeCycle as any).totalDurationSeconds / 3600) * 10) / 10,
      isComplete: false,
      daysUnwashed
    });
  }

  // Populate dayStatusMap for each day across all cycles
  const todayKey = getTodayDateKey();

  cycles.forEach(c => {
    const [sy, sm, sd] = c.firstWearDate.split('-').map(Number);
    const startDate = new Date(sy, sm - 1, sd);

    const endDateStr = c.washDate || todayKey;
    const [ey, em, ed] = endDateStr.split('-').map(Number);
    const endDate = new Date(ey, em - 1, ed);

    // Iterate through every single calendar day from startDate to endDate
    const current = new Date(startDate);
    let dayCount = 0;

    while (current <= endDate) {
      const dateKey = buildDateKey(current.getFullYear(), current.getMonth(), current.getDate());
      dayCount++;

      const isWearDay = c.wearDates.includes(dateKey);
      const isWashDay = c.washDate === dateKey;

      if (isWashDay) {
        dayStatusMap.set(dateKey, {
          dateKey,
          status: 'wash_day',
          cycleId: c.cycleId,
          daysSinceFirstWear: dayCount
        });
      } else if (isWearDay) {
        // Find wear duration on this day
        const dayRecs = wearRecords.filter(r => r.wearDateKey === dateKey);
        const daySec = dayRecs.reduce((sum, r) => sum + r.durationSec, 0);
        dayStatusMap.set(dateKey, {
          dateKey,
          status: 'wear_day',
          hoursWornToday: Math.round((daySec / 3600) * 10) / 10,
          cycleId: c.cycleId,
          daysSinceFirstWear: dayCount,
          reportTitles: dayRecs.map(r => r.report['Name']),
          reports: dayRecs.map(r => r.report)
        });
      } else {
        // Intermediate unwashed span day!
        dayStatusMap.set(dateKey, {
          dateKey,
          status: 'unwashed_span',
          cycleId: c.cycleId,
          daysSinceFirstWear: dayCount
        });
      }

      current.setDate(current.getDate() + 1);
    }
  });

  // Calculate current status
  const currentUnwashedJunctions = garmentJunctions.filter(j => j['wash stated'] === 'need wash');
  const isCurrentlyUnwashed = currentUnwashedJunctions.length > 0;

  let currentUnwashedHours = 0;
  let currentUnwashedDays = 0;
  if (isCurrentlyUnwashed && cycles.length > 0) {
    const latestCycle = cycles[cycles.length - 1];
    if (!latestCycle.isComplete) {
      currentUnwashedHours = latestCycle.totalDurationHours;
      currentUnwashedDays = latestCycle.daysUnwashed;
    } else {
      const totalSec = currentUnwashedJunctions.reduce((sum, j) => sum + (j['duration'] || 14400), 0);
      currentUnwashedHours = Math.round((totalSec / 3600) * 10) / 10;
      currentUnwashedDays = 1;
    }
  }

  return {
    garment,
    dayStatusMap,
    cycles,
    currentStatus: isCurrentlyUnwashed ? 'unwashed' : 'clean',
    currentUnwashedDays,
    currentUnwashedHours,
    currentUnwashedSessions: currentUnwashedJunctions.length
  };
}
