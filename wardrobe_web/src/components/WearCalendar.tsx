import React, { useState, useMemo } from 'react';
import { WearingReport, ReportClothesJunction, ClothesItem } from '../types';
import {
  ChevronLeft, ChevronRight, Calendar as CalendarIcon, Clock,
  Shirt, Droplets, Sparkles, Plus, Eye, CheckCircle2, AlertCircle,
  RotateCcw, X, ArrowRight, Layers
} from 'lucide-react';
import { dataService } from '../services/dataService';
import { resolveImageSource } from '../utils/imageUtils';
import { ConfirmModal } from './ConfirmModal';
import {
  computeDailyWearSummaries,
  computeDailyWashSummaries,
  computeGarmentLifecycle,
  DayWearSummary,
  DayWashSummary,
  toDateKey,
  buildDateKey,
  getTodayDateKey,
  formatCalendarDate
} from '../utils/calendarCalculations';

interface WearCalendarProps {
  reports: WearingReport[];
  junctions: ReportClothesJunction[];
  clothes: ClothesItem[];
  onSelectReport: (report: WearingReport) => void;
  onSelectClothes?: (clothes: ClothesItem) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
  onOpenLogModal?: () => void;
}

export type CalendarViewMode = 'daily_worn' | 'wash_history' | 'clothes_lifecycle';

export const WearCalendar: React.FC<WearCalendarProps> = ({
  reports,
  junctions,
  clothes,
  onSelectReport,
  onSelectClothes,
  onSelectGarment,
  onOpenLogModal
}) => {
  const handleSelectClothesItem = (item: ClothesItem) => {
    if (onSelectClothes) onSelectClothes(item);
    else if (onSelectGarment) onSelectGarment(item);
  };

  // Calendar View Switcher (iOS Segmented Control)
  const [activeViewMode, setActiveViewMode] = useState<CalendarViewMode>('daily_worn');

  // Lifecycle View Selected Garment
  const [selectedGarmentId, setSelectedGarmentId] = useState<string>(() => {
    // Default to the first garment in closet, or first one with wear history
    if (clothes.length > 0) return clothes[0].id;
    return '';
  });

  // Current calendar month navigation
  const [currentDate, setCurrentDate] = useState(() => {
    if (reports.length > 0 && reports[0]['wear date']) {
      const key = toDateKey(reports[0]['wear date']);
      if (key) {
        const [y, m] = key.split('-').map(Number);
        return new Date(y, m - 1, 1);
      }
    }
    return new Date();
  });

  const [selectedDayDate, setSelectedDayDate] = useState<string | null>(null);
  const [isRecordingWash, setIsRecordingWash] = useState(false);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const handlePrevMonth = () => {
    setCurrentDate(new Date(year, month - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentDate(new Date(year, month + 1, 1));
  };

  const handleToday = () => {
    const today = new Date();
    setCurrentDate(new Date(today.getFullYear(), today.getMonth(), 1));
    setSelectedDayDate(getTodayDateKey());
  };

  // 1. View 1 Computations: Daily Clothes Wear (grouped strictly by report['wear date'])
  const dailyWearMap = useMemo(() => {
    return computeDailyWearSummaries(reports, junctions, clothes);
  }, [reports, junctions, clothes]);

  // 2. View 2 Computations: Daily Wash Events & Accumulated Wear Duration
  const dailyWashMap = useMemo(() => {
    return computeDailyWashSummaries(reports, junctions, clothes);
  }, [reports, junctions, clothes]);

  // 3. View 3 Computations: Selected Garment Lifecycle Span (wear -> unwashed -> wash)
  const lifecycleData = useMemo(() => {
    if (!selectedGarmentId) return null;
    return computeGarmentLifecycle(selectedGarmentId, reports, junctions, clothes);
  }, [selectedGarmentId, reports, junctions, clothes]);

  // Calendar Grid generation
  const monthStart = new Date(year, month, 1);
  const monthEnd = new Date(year, month + 1, 0);
  const startDayIndex = monthStart.getDay(); // 0 = Sunday
  const daysInMonth = monthEnd.getDate();

  const prevMonthEnd = new Date(year, month, 0);
  const prevMonthDaysCount = prevMonthEnd.getDate();

  const calendarCells = useMemo(() => {
    const cells: Array<{
      dateKey: string;
      dayNumber: number;
      isCurrentMonth: boolean;
      isToday: boolean;
      wearSummary?: DayWearSummary;
      washSummary?: DayWashSummary;
      lifecycleStatus?: ReturnType<typeof computeGarmentLifecycle> extends null ? undefined : ReturnType<typeof computeGarmentLifecycle>['dayStatusMap'] extends Map<string, infer V> ? V : undefined;
    }> = [];

    const todayStr = getTodayDateKey();

    // 1. Trailing days from previous month
    for (let i = startDayIndex - 1; i >= 0; i--) {
      const dayNum = prevMonthDaysCount - i;
      const prevDate = new Date(year, month - 1, dayNum);
      const dateKey = buildDateKey(prevDate.getFullYear(), prevDate.getMonth(), prevDate.getDate());
      cells.push({
        dateKey,
        dayNumber: dayNum,
        isCurrentMonth: false,
        isToday: dateKey === todayStr,
        wearSummary: dailyWearMap.get(dateKey),
        washSummary: dailyWashMap.get(dateKey),
        lifecycleStatus: lifecycleData?.dayStatusMap.get(dateKey)
      });
    }

    // 2. Current month days
    for (let dayNum = 1; dayNum <= daysInMonth; dayNum++) {
      const dateKey = buildDateKey(year, month, dayNum);
      cells.push({
        dateKey,
        dayNumber: dayNum,
        isCurrentMonth: true,
        isToday: dateKey === todayStr,
        wearSummary: dailyWearMap.get(dateKey),
        washSummary: dailyWashMap.get(dateKey),
        lifecycleStatus: lifecycleData?.dayStatusMap.get(dateKey)
      });
    }

    // 3. Leading days of next month
    const remaining = (7 - (cells.length % 7)) % 7;
    for (let dayNum = 1; dayNum <= remaining; dayNum++) {
      const nextDate = new Date(year, month + 1, dayNum);
      const dateKey = buildDateKey(nextDate.getFullYear(), nextDate.getMonth(), nextDate.getDate());
      cells.push({
        dateKey,
        dayNumber: dayNum,
        isCurrentMonth: false,
        isToday: dateKey === todayStr,
        wearSummary: dailyWearMap.get(dateKey),
        washSummary: dailyWashMap.get(dateKey),
        lifecycleStatus: lifecycleData?.dayStatusMap.get(dateKey)
      });
    }

    return cells;
  }, [year, month, startDayIndex, daysInMonth, prevMonthDaysCount, dailyWearMap, dailyWashMap, lifecycleData]);

  const monthName = currentDate.toLocaleString('default', { month: 'long', year: 'numeric' });

  const [washConfirmConfig, setWashConfirmConfig] = useState<{
    isOpen: boolean;
    clothesId: string;
    garmentName: string;
    date: string;
  } | null>(null);

  // Quick laundry washer for selected day
  const requestQuickWashOnDay = (clothesId: string) => {
    const garment = clothes.find(c => c.id === clothesId);
    const garmentName = garment ? garment['Name'] : 'Garment';
    setWashConfirmConfig({
      isOpen: true,
      clothesId,
      garmentName,
      date: selectedDayDate || getTodayDateKey()
    });
  };

  const handleConfirmQuickWash = async () => {
    if (!washConfirmConfig) return;
    const { clothesId, date } = washConfirmConfig;
    setWashConfirmConfig(null);
    setIsRecordingWash(true);
    try {
      await dataService.markGarmentWashed(clothesId, date);
    } finally {
      setIsRecordingWash(false);
    }
  };

  // Currently unwashed clothes (for quick wash helper)
  const unwashedClothes = useMemo(() => {
    const needWashSet = new Set<string>();
    junctions.forEach(j => {
      if (j['wash stated'] === 'need wash') {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        needWashSet.add(cId);
      }
    });
    return clothes.filter(c => needWashSet.has(c.id));
  }, [clothes, junctions]);

  return (
    <div id="wear-calendar-view" className="space-y-4 w-full">
      {/* ========================================================================= */}
      {/* 1. iOS Style Header Card with View Switcher & Navigation */}
      {/* ========================================================================= */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200/80 shadow-xs space-y-4">
        {/* Top Row: Title, Month Navigation & Primary Action */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-slate-100 text-slate-800">
              <CalendarIcon className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg sm:text-xl font-bold text-slate-900 tracking-tight">
                {monthName}
              </h2>
              <p className="text-xs text-slate-500 font-medium">
                {activeViewMode === 'daily_worn' && 'Daily clothes wear summary (by actual wear date)'}
                {activeViewMode === 'wash_history' && 'Laundry wash days & accumulated wear hours tracker'}
                {activeViewMode === 'clothes_lifecycle' && 'Clothes lifecycle span: wear → unwashed days → clean'}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 self-end sm:self-auto">
            <button
              type="button"
              onClick={handleToday}
              className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold transition-colors active:scale-95"
            >
              Today
            </button>

            <div className="flex items-center rounded-xl bg-slate-100 p-0.5 border border-slate-200/60">
              <button
                type="button"
                onClick={handlePrevMonth}
                className="p-1.5 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-white transition-colors"
                title="Previous Month"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                type="button"
                onClick={handleNextMonth}
                className="p-1.5 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-white transition-colors"
                title="Next Month"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            {onOpenLogModal && (
              <button
                type="button"
                onClick={onOpenLogModal}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-xs transition-all active:scale-95"
              >
                <Plus className="w-4 h-4 text-white" />
                <span>Log Outfit</span>
              </button>
            )}
          </div>
        </div>

        {/* Second Row: iOS Native Segmented Control */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-1 border-t border-slate-100">
          <div className="bg-slate-100/90 p-1 rounded-2xl flex items-center gap-1 border border-slate-200/60 w-full sm:w-auto text-xs font-medium">
            <button
              type="button"
              onClick={() => {
                setActiveViewMode('daily_worn');
                setSelectedDayDate(null);
              }}
              className={`flex-1 sm:flex-initial inline-flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-xl transition-all ${
                activeViewMode === 'daily_worn'
                  ? 'bg-white text-slate-900 font-bold shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Shirt className="w-3.5 h-3.5 text-slate-700" />
              <span>Daily Worn</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setActiveViewMode('wash_history');
                setSelectedDayDate(null);
              }}
              className={`flex-1 sm:flex-initial inline-flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-xl transition-all ${
                activeViewMode === 'wash_history'
                  ? 'bg-white text-slate-900 font-bold shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Droplets className="w-3.5 h-3.5 text-blue-600" />
              <span>Wash &amp; Laundry</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setActiveViewMode('clothes_lifecycle');
                setSelectedDayDate(null);
              }}
              className={`flex-1 sm:flex-initial inline-flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-xl transition-all ${
                activeViewMode === 'clothes_lifecycle'
                  ? 'bg-white text-slate-900 font-bold shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <RotateCcw className="w-3.5 h-3.5 text-amber-600" />
              <span>Clothes Lifecycle</span>
            </button>
          </div>

          {/* Quick instructions / legend indicator */}
          <div className="flex items-center gap-2 text-[11px] text-slate-500 flex-wrap">
            {activeViewMode === 'daily_worn' && (
              <>
                <span className="inline-flex items-center gap-1 font-medium">
                  <span className="w-2 h-2 rounded-full bg-slate-900" /> Worn items
                </span>
                <span>·</span>
                <span className="inline-flex items-center gap-1 font-medium text-amber-800">
                  <span className="w-2 h-2 rounded-full bg-amber-500" /> Need wash
                </span>
              </>
            )}

            {activeViewMode === 'wash_history' && (
              <span className="inline-flex items-center gap-1 font-medium text-emerald-800">
                <span className="w-2 h-2 rounded-full bg-emerald-500" /> Wash day (with accumulated hours)
              </span>
            )}

            {activeViewMode === 'clothes_lifecycle' && (
              <>
                <span className="inline-flex items-center gap-1 font-medium text-slate-800">
                  <span className="w-2 h-2 rounded-full bg-slate-900" /> Wear day
                </span>
                <span>·</span>
                <span className="inline-flex items-center gap-1 font-medium text-amber-700">
                  <span className="w-2 h-2 rounded-full bg-amber-400" /> Unwashed waiting span
                </span>
                <span>·</span>
                <span className="inline-flex items-center gap-1 font-medium text-emerald-700">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" /> Cleaned
                </span>
              </>
            )}
          </div>
        </div>

        {/* ========================================================================= */}
        {/* 2. Clothes Selector Card (Active in Clothes Lifecycle View) */}
        {/* ========================================================================= */}
        {activeViewMode === 'clothes_lifecycle' && (
          <div className="p-3.5 rounded-2xl bg-slate-50/80 border border-slate-200/80 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2.5 flex-1 min-w-0">
              <span className="text-xs font-bold font-mono text-slate-600 uppercase shrink-0">
                Select Clothes:
              </span>
              <select
                value={selectedGarmentId}
                onChange={(e) => {
                  setSelectedGarmentId(e.target.value);
                  setSelectedDayDate(null);
                }}
                className="flex-1 max-w-md px-3 py-1.5 rounded-xl border border-slate-300 bg-white text-xs font-semibold text-slate-800 shadow-2xs focus:outline-none focus:border-slate-600"
              >
                {clothes.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item['Name']} ({item['Clothes Type'] || 'Clothes'})
                  </option>
                ))}
              </select>
            </div>

            {/* Lifecycle Status Pill for Selected Garment */}
            {lifecycleData?.garment && (
              <div className="flex items-center gap-2 shrink-0">
                {lifecycleData.currentStatus === 'unwashed' ? (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-amber-50 text-amber-800 border border-amber-200 text-xs font-semibold">
                    <AlertCircle className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                    <span>
                      Unwashed ({lifecycleData.currentUnwashedDays}d waiting · {lifecycleData.currentUnwashedHours}h)
                    </span>
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-emerald-50 text-emerald-800 border border-emerald-200 text-xs font-semibold">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    <span>Clean &amp; in Closet</span>
                  </span>
                )}

                {lifecycleData.currentStatus === 'unwashed' && (
                  <button
                    type="button"
                    onClick={() => requestQuickWashOnDay(selectedGarmentId)}
                    disabled={isRecordingWash}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-xs transition-all active:scale-95 disabled:opacity-60 cursor-pointer"
                  >
                    <Droplets className="w-3.5 h-3.5" />
                    <span>Wash Now</span>
                  </button>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* 3. Mobile-First Calendar Grid */}
      {/* ========================================================================= */}
      <div className="bg-white rounded-3xl border border-slate-200/80 shadow-xs overflow-hidden">
        {/* Days of Week Header */}
        <div className="grid grid-cols-7 border-b border-slate-200 bg-slate-50/90 text-center font-mono text-[11px] font-bold text-slate-500 uppercase tracking-wider py-2.5">
          <span>Sun</span>
          <span>Mon</span>
          <span>Tue</span>
          <span>Wed</span>
          <span>Thu</span>
          <span>Fri</span>
          <span>Sat</span>
        </div>

        {/* Days Cells Grid */}
        <div className="grid grid-cols-7 auto-rows-fr divide-x divide-y divide-slate-100">
          {calendarCells.map((cell, idx) => {
            const isSelected = selectedDayDate === cell.dateKey;

            // Mode-specific cell status
            const wearSummary = cell.wearSummary;
            const washSummary = cell.washSummary;
            const lifecycleStatus = cell.lifecycleStatus;

            const hasWear = Boolean(wearSummary && wearSummary.items.length > 0);
            const hasWash = Boolean(washSummary && washSummary.washedItems.length > 0);

            // Is cell clickable?
            const isClickable =
              (activeViewMode === 'daily_worn' && hasWear) ||
              (activeViewMode === 'wash_history' && hasWash) ||
              (activeViewMode === 'clothes_lifecycle' && Boolean(lifecycleStatus));

            // Dynamic cell background
            let cellBg = 'bg-white hover:bg-slate-50/60';
            if (!cell.isCurrentMonth) {
              cellBg = 'bg-slate-50/40 text-slate-400';
            } else if (activeViewMode === 'daily_worn' && hasWear) {
              cellBg = wearSummary?.hasNeedWash
                ? 'bg-amber-50/30 hover:bg-amber-50/60 cursor-pointer'
                : 'bg-slate-50/60 hover:bg-slate-100/70 cursor-pointer';
            } else if (activeViewMode === 'wash_history' && hasWash) {
              cellBg = 'bg-emerald-50/40 hover:bg-emerald-50/70 cursor-pointer';
            } else if (activeViewMode === 'clothes_lifecycle' && lifecycleStatus) {
              if (lifecycleStatus.status === 'wear_day') {
                cellBg = 'bg-slate-100 hover:bg-slate-200/70 cursor-pointer';
              } else if (lifecycleStatus.status === 'unwashed_span') {
                cellBg = 'bg-amber-50/60 hover:bg-amber-100/60 cursor-pointer';
              } else if (lifecycleStatus.status === 'wash_day') {
                cellBg = 'bg-emerald-50/60 hover:bg-emerald-100/60 cursor-pointer';
              }
            }

            return (
              <div
                key={idx}
                onClick={() => {
                  if (isClickable) {
                    setSelectedDayDate(selectedDayDate === cell.dateKey ? null : cell.dateKey);
                  }
                }}
                className={`min-h-[72px] sm:min-h-[96px] p-1.5 sm:p-2 flex flex-col justify-between transition-colors relative group select-none ${cellBg} ${
                  cell.isToday ? 'ring-2 ring-inset ring-slate-800' : ''
                } ${isSelected ? 'ring-2 ring-inset ring-blue-600 bg-blue-50/40' : ''}`}
              >
                {/* Day Number Row */}
                <div className="flex items-center justify-between pointer-events-none">
                  <span
                    className={`inline-flex items-center justify-center w-5 h-5 sm:w-6 sm:h-6 rounded-full text-xs font-bold ${
                      cell.isToday
                        ? 'bg-slate-900 text-white shadow-xs'
                        : cell.isCurrentMonth
                        ? 'text-slate-800'
                        : 'text-slate-400'
                    }`}
                  >
                    {cell.dayNumber}
                  </span>

                  {/* Mode 1 Corner Indicators */}
                  {activeViewMode === 'daily_worn' && hasWear && (
                    <div className="flex items-center gap-1">
                      {wearSummary?.hasNeedWash ? (
                        <span className="w-1.5 h-1.5 sm:w-2 sm:h-2 rounded-full bg-amber-500" title="Need wash recorded" />
                      ) : (
                        <span className="w-1.5 h-1.5 sm:w-2 sm:h-2 rounded-full bg-emerald-500" title="All clean" />
                      )}
                      <span className="text-[9px] font-mono font-bold text-slate-500">
                        {wearSummary?.uniqueClothesCount}
                      </span>
                    </div>
                  )}

                  {/* Mode 2 Corner Indicator */}
                  {activeViewMode === 'wash_history' && hasWash && (
                    <span className="w-2 h-2 rounded-full bg-emerald-500 ring-2 ring-emerald-200" title="Laundry washed" />
                  )}
                </div>

                {/* Body Content depending on Active View Mode */}
                <div className="mt-1 flex-1 flex flex-col justify-center pointer-events-none overflow-hidden">
                  {/* MODE 1: DAILY WORN */}
                  {activeViewMode === 'daily_worn' && hasWear && wearSummary && (
                    <div className="space-y-1">
                      <div className="px-1.5 py-0.5 rounded-md bg-white border border-slate-200/90 text-[10px] sm:text-xs font-semibold text-slate-800 truncate flex items-center justify-between shadow-2xs">
                        <span className="truncate flex items-center gap-1">
                          <Shirt className="w-2.5 h-2.5 sm:w-3 sm:h-3 text-slate-600 shrink-0" />
                          <span className="truncate">{wearSummary.uniqueClothesCount} {wearSummary.uniqueClothesCount === 1 ? 'item' : 'items'}</span>
                        </span>
                        <span className="text-[9px] sm:text-[10px] font-mono font-bold text-slate-600 shrink-0">
                          {wearSummary.totalDayHours}h
                        </span>
                      </div>
                    </div>
                  )}

                  {/* MODE 2: WASH HISTORY */}
                  {activeViewMode === 'wash_history' && hasWash && washSummary && (
                    <div className="px-1.5 py-0.5 rounded-md bg-emerald-50 border border-emerald-200 text-[10px] sm:text-xs font-semibold text-emerald-900 flex items-center justify-between shadow-2xs">
                      <span className="truncate flex items-center gap-1">
                        <Droplets className="w-2.5 h-2.5 sm:w-3 sm:h-3 text-emerald-600 shrink-0" />
                        <span className="truncate">{washSummary.totalWashedCount} {washSummary.totalWashedCount === 1 ? 'clean' : 'clean'}</span>
                      </span>
                      <span className="text-[9px] sm:text-[10px] font-mono font-bold text-emerald-700 shrink-0">
                        {washSummary.totalAccumulatedHours}h
                      </span>
                    </div>
                  )}

                  {/* MODE 3: CLOTHES LIFECYCLE SPAN */}
                  {activeViewMode === 'clothes_lifecycle' && lifecycleStatus && (
                    <div className="text-center">
                      {lifecycleStatus.status === 'wear_day' && (
                        <div className="px-1 py-0.5 rounded-md bg-slate-900 text-white text-[9px] sm:text-[10px] font-bold truncate flex items-center justify-center gap-1 shadow-2xs">
                          <Shirt className="w-2.5 h-2.5" />
                          <span>Worn {lifecycleStatus.hoursWornToday}h</span>
                        </div>
                      )}

                      {lifecycleStatus.status === 'unwashed_span' && (
                        <div className="px-1 py-0.5 rounded-md bg-amber-100/80 text-amber-900 text-[9px] sm:text-[10px] font-medium border border-amber-300/80 truncate">
                          <span>Day {lifecycleStatus.daysSinceFirstWear} unwashed</span>
                        </div>
                      )}

                      {lifecycleStatus.status === 'wash_day' && (
                        <div className="px-1 py-0.5 rounded-md bg-emerald-600 text-white text-[9px] sm:text-[10px] font-bold truncate flex items-center justify-center gap-1 shadow-2xs">
                          <Droplets className="w-2.5 h-2.5" />
                          <span>Washed</span>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Subtle bottom footer helper */}
                <div className="h-1" />
              </div>
            );
          })}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* 4. Selected Day Inspector Drawer (iOS Inset Grouped Card Style) */}
      {/* ========================================================================= */}
      {selectedDayDate && (
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-sm space-y-4 animate-in fade-in duration-150">
          {/* Drawer Header Bar */}
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2 min-w-0">
              <CalendarIcon className="w-4 h-4 text-slate-700 shrink-0" />
              <h3 className="text-sm sm:text-base font-bold text-slate-900 truncate">
                {formatCalendarDate(selectedDayDate, {
                  weekday: 'short',
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric'
                })}
              </h3>
            </div>

            <button
              type="button"
              onClick={() => setSelectedDayDate(null)}
              className="p-1 rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* ========================================================= */}
          {/* Content for MODE 1: Daily Clothes Worn (Deduplicated)     */}
          {/* ========================================================= */}
          {activeViewMode === 'daily_worn' && (
            <div className="space-y-3">
              {dailyWearMap.get(selectedDayDate) ? (
                <>
                  <div className="flex items-center justify-between text-xs text-slate-600 font-medium">
                    <span>
                      {dailyWearMap.get(selectedDayDate)!.uniqueClothesCount} unique clothes worn on this date
                    </span>
                    <span className="font-mono font-bold text-slate-900">
                      Total: {dailyWearMap.get(selectedDayDate)!.totalDayHours} hrs logged
                    </span>
                  </div>

                  {/* Dedicated Wearing Reports logged on this date */}
                  {dailyWearMap.get(selectedDayDate)!.reports.length > 0 && (
                    <div className="p-3 bg-indigo-50/60 rounded-2xl border border-indigo-100/90 space-y-2">
                      <div className="flex items-center justify-between text-xs font-bold text-indigo-950">
                        <span className="flex items-center gap-1.5">
                          <Eye className="w-3.5 h-3.5 text-indigo-600" />
                          <span>Wearing Reports on {selectedDayDate} ({dailyWearMap.get(selectedDayDate)!.reports.length})</span>
                        </span>
                        <span className="text-[10px] text-indigo-500 font-normal">Click report to view</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        {dailyWearMap.get(selectedDayDate)!.reports.map((rep) => (
                          <button
                            key={rep.id}
                            type="button"
                            onClick={() => onSelectReport(rep)}
                            className="w-full flex items-center justify-between p-2.5 rounded-xl bg-white hover:bg-indigo-50/90 border border-indigo-200/80 hover:border-indigo-400 text-left transition-all shadow-2xs group cursor-pointer active:scale-[0.99]"
                            title={`Click to view wearing report: ${rep['Name']}`}
                          >
                            <div className="min-w-0 pr-2">
                              <p className="text-xs font-bold text-slate-900 group-hover:text-indigo-700 truncate">
                                {rep['Name']}
                              </p>
                              <p className="text-[10px] text-slate-500 truncate">
                                {rep['wear date'] || selectedDayDate} {rep['Temperature'] ? `• ${rep['Temperature']}` : ''}
                              </p>
                            </div>
                            <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-indigo-600 shrink-0 group-hover:translate-x-0.5 transition-transform bg-indigo-50 px-2 py-0.5 rounded-lg border border-indigo-200/60">
                              <span>View Report</span>
                              <ChevronRight className="w-3 h-3" />
                            </span>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {dailyWearMap.get(selectedDayDate)!.items.map((item) => {
                      const imageSrc = item.garment?.Pictures && item.garment.Pictures.length > 0
                        ? resolveImageSource(item.garment.Pictures[0])
                        : null;

                      return (
                        <div
                          key={item.clothesId}
                          className="p-3 sm:p-3.5 rounded-2xl bg-slate-50/60 border border-slate-200/80 hover:border-slate-400 transition-all flex items-start gap-3 group"
                        >
                          {/* Garment Image / Icon */}
                          <div
                            onClick={() => item.garment && onSelectGarment?.(item.garment)}
                            className="w-12 h-12 rounded-xl overflow-hidden bg-slate-200 shrink-0 cursor-pointer flex items-center justify-center border border-slate-200"
                          >
                            {imageSrc ? (
                              <img src={imageSrc} alt="" className="w-full h-full object-contain p-0.5" />
                            ) : (
                              <Shirt className="w-5 h-5 text-slate-400" />
                            )}
                          </div>

                          {/* Garment Details & Hours */}
                          <div className="flex-1 min-w-0 space-y-1">
                            <div className="flex items-center justify-between gap-1">
                              <h4
                                onClick={() => item.garment && onSelectGarment?.(item.garment)}
                                className="text-xs sm:text-sm font-bold text-slate-900 hover:text-slate-600 cursor-pointer truncate"
                              >
                                {item.clothesTitle}
                              </h4>
                              <span className="text-xs font-mono font-bold text-slate-900 shrink-0 px-2 py-0.5 rounded-md bg-white border border-slate-200 shadow-2xs">
                                {item.totalDurationHours} hrs
                              </span>
                            </div>

                            <div className="flex items-center gap-1.5 flex-wrap">
                              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-200/70 text-slate-700">
                                {item.clothesType}
                              </span>

                              {item.hasNeedWash ? (
                                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                                  need wash
                                </span>
                              ) : (
                                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                                  clean
                                </span>
                              )}

                              {item.sessionCount > 1 && (
                                <span className="text-[10px] font-mono text-slate-500">
                                  (Worn in {item.sessionCount} reports on this day)
                                </span>
                              )}
                            </div>

                            {/* Reports List */}
                            <div className="pt-1.5 flex items-center gap-1.5 flex-wrap">
                              <span className="text-[10px] font-mono font-bold text-slate-500 uppercase">Wearing Reports:</span>
                              {item.reports.map((rep) => (
                                <button
                                  key={rep.id}
                                  type="button"
                                  onClick={() => onSelectReport(rep)}
                                  className="inline-flex items-center gap-1.5 px-2 py-1 rounded-lg bg-white hover:bg-indigo-50 text-[11px] font-semibold text-slate-800 hover:text-indigo-700 border border-slate-200 hover:border-indigo-300 transition-all shadow-2xs cursor-pointer active:scale-95"
                                  title={`Click to view wearing report: ${rep['Name']}`}
                                >
                                  <Eye className="w-3 h-3 text-indigo-600" />
                                  <span className="truncate max-w-[140px]">{rep['Name']}</span>
                                </button>
                              ))}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </>
              ) : (
                <p className="text-xs text-slate-400 italic py-2">
                  No clothes worn on this date.
                </p>
              )}
            </div>
          )}

          {/* ========================================================= */}
          {/* Content for MODE 2: Laundry Wash Events & Accumulated     */}
          {/* ========================================================= */}
          {activeViewMode === 'wash_history' && (
            <div className="space-y-4">
              {dailyWashMap.get(selectedDayDate) ? (
                <>
                  <div className="flex items-center justify-between text-xs text-slate-600 font-medium">
                    <span className="flex items-center gap-1 text-emerald-800 font-bold">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      {dailyWashMap.get(selectedDayDate)!.totalWashedCount} clothes washed on this day
                    </span>
                    <span className="font-mono font-bold text-slate-900">
                      Total Accumulated: {dailyWashMap.get(selectedDayDate)!.totalAccumulatedHours} hrs cleaned
                    </span>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {dailyWashMap.get(selectedDayDate)!.washedItems.map((item) => {
                      const imageSrc = item.garment?.Pictures && item.garment.Pictures.length > 0
                        ? resolveImageSource(item.garment.Pictures[0])
                        : null;

                      return (
                        <div
                          key={item.clothesId}
                          className="p-3.5 rounded-2xl bg-emerald-50/30 border border-emerald-200/80 space-y-2.5"
                        >
                          <div className="flex items-start gap-3">
                            <div className="w-12 h-12 rounded-xl overflow-hidden bg-slate-200 shrink-0 flex items-center justify-center border border-slate-200">
                              {imageSrc ? (
                                <img src={imageSrc} alt="" className="w-full h-full object-contain p-0.5" />
                              ) : (
                                <Shirt className="w-5 h-5 text-slate-400" />
                              )}
                            </div>

                            <div className="flex-1 min-w-0 space-y-1">
                              <div className="flex items-center justify-between">
                                <h4 className="text-xs sm:text-sm font-bold text-slate-900 truncate">
                                  {item.clothesTitle}
                                </h4>
                                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                                  Clean
                                </span>
                              </div>

                              <p className="text-xs font-mono font-bold text-emerald-900">
                                {item.accumulatedHours} hrs accumulated wear
                              </p>
                              <p className="text-[11px] text-slate-500">
                                Across {item.sessionsCount} wear {item.sessionsCount === 1 ? 'session' : 'sessions'} since last clean
                              </p>
                            </div>
                          </div>

                          {/* Breakdown of wear sessions contributing to this wash */}
                          {item.contributingSessions.length > 0 && (
                            <div className="pt-2.5 border-t border-emerald-100/90 space-y-2">
                              <div className="flex items-center justify-between text-[10px] font-bold font-mono text-slate-600 uppercase">
                                <span className="flex items-center gap-1.5">
                                  <Eye className="w-3.5 h-3.5 text-emerald-600" />
                                  <span>Contributing Wearing Reports ({item.contributingSessions.length}):</span>
                                </span>
                                <span className="text-[10px] text-emerald-700 font-semibold bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200/80">
                                  click to view
                                </span>
                              </div>
                              <div className="space-y-1.5">
                                {item.contributingSessions.map((session, sIdx) => {
                                  const targetReport = session.report ||
                                    (session.reportId ? reports.find(r => r.id === session.reportId) : undefined) ||
                                    (session.reportTitle ? reports.find(r => r['Name'] === session.reportTitle) : undefined) ||
                                    (session.wearDate ? reports.find(r => toDateKey(r['wear date']) === session.wearDate) : undefined);

                                  return (
                                    <button
                                      key={sIdx}
                                      type="button"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        if (targetReport) {
                                          onSelectReport(targetReport);
                                        }
                                      }}
                                      className={`w-full flex items-center justify-between p-2.5 rounded-xl text-left bg-white hover:bg-emerald-50/90 border border-emerald-200 hover:border-emerald-400 transition-all shadow-2xs group/rep active:scale-[0.99] cursor-pointer ${
                                        targetReport ? '' : 'opacity-80'
                                      }`}
                                      title={targetReport ? `Click to view wearing report: ${session.reportTitle}` : session.reportTitle}
                                    >
                                      <div className="flex items-center gap-2.5 min-w-0 pr-2">
                                        <div className="p-1.5 rounded-lg bg-emerald-100 text-emerald-800 shrink-0 group-hover/rep:bg-emerald-200 transition-colors">
                                          <Eye className="w-3.5 h-3.5" />
                                        </div>
                                        <div className="min-w-0">
                                          <p className="text-xs font-bold text-slate-900 group-hover/rep:text-emerald-950 group-hover/rep:underline truncate">
                                            {session.reportTitle}
                                          </p>
                                          <p className="text-[10px] text-slate-500 font-mono">
                                            {session.wearDate ? `Worn on ${formatCalendarDate(session.wearDate)}` : 'Worn session'}
                                          </p>
                                        </div>
                                      </div>

                                      <div className="flex items-center gap-2 shrink-0">
                                        <span className="text-[10px] font-mono font-bold text-emerald-800 bg-emerald-100/80 px-2 py-0.5 rounded-md">
                                          {session.durationHours}h
                                        </span>
                                        <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-1 rounded-lg border border-emerald-200 group-hover/rep:bg-emerald-100 transition-colors">
                                          <span>View</span>
                                          <ChevronRight className="w-3 h-3 group-hover/rep:translate-x-0.5 transition-transform" />
                                        </span>
                                      </div>
                                    </button>
                                  );
                                })}
                              </div>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </>
              ) : (
                <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 text-center space-y-2">
                  <p className="text-xs text-slate-500">
                    No laundry was recorded washed on {selectedDayDate}.
                  </p>
                  {unwashedClothes.length > 0 && (
                    <div className="pt-2 space-y-2">
                      <span className="text-xs font-bold text-slate-700 block">
                        Want to record laundry washed on this date?
                      </span>
                      <div className="flex items-center justify-center gap-1.5 flex-wrap">
                        {unwashedClothes.map((c) => (
                          <button
                            key={c.id}
                            type="button"
                            onClick={() => requestQuickWashOnDay(c.id)}
                            disabled={isRecordingWash}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-white hover:bg-emerald-50 text-emerald-800 border border-emerald-300 text-xs font-semibold transition-all shadow-2xs active:scale-95 disabled:opacity-50 cursor-pointer"
                          >
                            <Droplets className="w-3 h-3 text-emerald-600" />
                            <span>Wash {c['Name']}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* ========================================================= */}
          {/* Content for MODE 3: Selected Garment Lifecycle Span       */}
          {/* ========================================================= */}
          {activeViewMode === 'clothes_lifecycle' && lifecycleData?.garment && (
            <div className="space-y-3">
              <div className="flex items-center justify-between text-xs text-slate-600">
                <span className="font-bold text-slate-800">
                  Lifecycle Status for: {lifecycleData.garment['Name']}
                </span>
                <span className="font-mono">
                  {selectedDayDate}
                </span>
              </div>

              {lifecycleData.dayStatusMap.get(selectedDayDate) ? (
                <div className="p-4 rounded-2xl border bg-slate-50/80 space-y-2">
                  {lifecycleData.dayStatusMap.get(selectedDayDate)!.status === 'wear_day' && (
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 text-slate-900 font-bold text-sm">
                        <Shirt className="w-4 h-4 text-slate-800" />
                        <span>Active Wear Day</span>
                      </div>
                      <p className="text-xs text-slate-600">
                        Worn on this day for <span className="font-bold text-slate-900">{lifecycleData.dayStatusMap.get(selectedDayDate)!.hoursWornToday} hours</span>.
                        This started or continued the unwashed wearing cycle.
                      </p>
                      {lifecycleData.dayStatusMap.get(selectedDayDate)!.reports && lifecycleData.dayStatusMap.get(selectedDayDate)!.reports!.length > 0 ? (
                        <div className="pt-2 flex items-center gap-1.5 flex-wrap">
                          <span className="text-[10px] font-mono font-bold text-slate-500 uppercase">Wearing Reports:</span>
                          {lifecycleData.dayStatusMap.get(selectedDayDate)!.reports!.map((rep) => (
                            <button
                              key={rep.id}
                              type="button"
                              onClick={() => onSelectReport(rep)}
                              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-white hover:bg-indigo-50 text-xs font-semibold text-slate-800 hover:text-indigo-700 border border-slate-300 hover:border-indigo-400 shadow-2xs transition-all cursor-pointer active:scale-95"
                              title={`Click to view wearing report: ${rep['Name']}`}
                            >
                              <Eye className="w-3.5 h-3.5 text-indigo-600" />
                              <span className="truncate max-w-[200px]">{rep['Name']}</span>
                            </button>
                          ))}
                        </div>
                      ) : lifecycleData.dayStatusMap.get(selectedDayDate)!.reportTitles ? (
                        <div className="pt-2 flex items-center gap-1.5 flex-wrap">
                          <span className="text-[10px] font-mono font-bold text-slate-500 uppercase">Wearing Reports:</span>
                          {lifecycleData.dayStatusMap.get(selectedDayDate)!.reportTitles!.map((title, tIdx) => {
                            const rep = reports.find(r => r['Name'] === title);
                            return (
                              <button
                                key={tIdx}
                                type="button"
                                onClick={() => rep && onSelectReport(rep)}
                                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-white hover:bg-indigo-50 text-xs font-semibold text-slate-800 hover:text-indigo-700 border border-slate-300 hover:border-indigo-400 shadow-2xs transition-all cursor-pointer active:scale-95"
                                title={`Click to view wearing report: ${title}`}
                              >
                                <Eye className="w-3.5 h-3.5 text-indigo-600" />
                                <span className="truncate max-w-[200px]">{title}</span>
                              </button>
                            );
                          })}
                        </div>
                      ) : null}
                    </div>
                  )}

                  {lifecycleData.dayStatusMap.get(selectedDayDate)!.status === 'unwashed_span' && (
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 text-amber-800 font-bold text-sm">
                        <Clock className="w-4 h-4 text-amber-600" />
                        <span>Unwashed Waiting Span (Day {lifecycleData.dayStatusMap.get(selectedDayDate)!.daysSinceFirstWear})</span>
                      </div>
                      <p className="text-xs text-slate-600">
                        The garment was worn previously and waiting to be washed on this day.
                      </p>
                    </div>
                  )}

                  {lifecycleData.dayStatusMap.get(selectedDayDate)!.status === 'wash_day' && (
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 text-emerald-800 font-bold text-sm">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        <span>Washed &amp; Cleaned Day</span>
                      </div>
                      <p className="text-xs text-slate-600">
                        The garment was washed clean on this date, concluding this cycle and resetting accumulated unwashed time.
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-xs text-slate-400 italic py-2">
                  The garment was clean in closet and not active on this date.
                </p>
              )}
            </div>
          )}
        </div>
      )}

      {/* Wash Confirmation Modal */}
      {washConfirmConfig && (
        <ConfirmModal
          isOpen={washConfirmConfig.isOpen}
          onClose={() => setWashConfirmConfig(null)}
          onConfirm={handleConfirmQuickWash}
          title={`Wash "${washConfirmConfig.garmentName}"?`}
          variant="primary"
          confirmLabel="Yes, Mark as Washed"
          message={
            <div className="space-y-2">
              <p>
                Mark <strong>"{washConfirmConfig.garmentName}"</strong> as washed on <strong>{washConfirmConfig.date}</strong>?
              </p>
              <div className="p-2.5 rounded-xl bg-blue-50 border border-blue-100 text-xs text-blue-900 flex items-center gap-2">
                <Droplets className="w-4 h-4 text-blue-600 shrink-0" />
                <span>Wash date will be recorded as <strong>{washConfirmConfig.date}</strong>.</span>
              </div>
              <p className="text-[11px] text-slate-500">
                This will reset its unwashed wear duration and increment its wash count. You can easily undo if clicked by mistake.
              </p>
            </div>
          }
        />
      )}
    </div>
  );
};
