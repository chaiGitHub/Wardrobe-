import React, { useState, useMemo } from 'react';
import { WearingReport, ReportClothesJunction, ClothesItem } from '../types';
import { formatDuration } from '../services/dataService';
import {
  ChevronLeft, ChevronRight, Calendar as CalendarIcon, Clock,
  Shirt, Droplets, AlertCircle, CheckCircle2, Sparkles, Plus, Eye
} from 'lucide-react';

interface WearCalendarProps {
  reports: WearingReport[];
  junctions: ReportClothesJunction[];
  clothes: ClothesItem[];
  onSelectReport: (report: WearingReport) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
  onOpenLogModal?: () => void;
}

interface DaySessionInfo {
  report: WearingReport;
  junctions: ReportClothesJunction[];
  garments: Array<{ junction: ReportClothesJunction; garment?: ClothesItem }>;
  totalDurationHours: number;
  hasNeedWash: boolean;
  allClean: boolean;
}

export const WearCalendar: React.FC<WearCalendarProps> = ({
  reports,
  junctions,
  clothes,
  onSelectReport,
  onSelectGarment,
  onOpenLogModal
}) => {
  // Current calendar month navigation
  const [currentDate, setCurrentDate] = useState(() => {
    // If reports exist, default to the month of the latest report; otherwise today
    if (reports.length > 0 && reports[0]['wear date']) {
      const d = new Date(reports[0]['wear date']);
      if (!isNaN(d.getTime())) return new Date(d.getFullYear(), d.getMonth(), 1);
    }
    return new Date();
  });

  const [selectedDayDate, setSelectedDayDate] = useState<string | null>(null);

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
  };

  // Group all reports by ISO date string (YYYY-MM-DD)
  const sessionsByDate = useMemo(() => {
    const map = new Map<string, DaySessionInfo[]>();

    reports.forEach(report => {
      const rawDate = report['wear date'];
      if (!rawDate) return;
      const parsed = new Date(rawDate);
      if (isNaN(parsed.getTime())) return;
      
      const dateKey = parsed.toISOString().slice(0, 10);

      // Find junctions for this report
      const repJunctions = junctions.filter(j => {
        const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
        return rId === report.id;
      });

      const repGarments = repJunctions.map(j => {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        const garment = clothes.find(c => c.id === cId);
        return { junction: j, garment };
      });

      const maxDurationSec = repJunctions.reduce((max, j) => Math.max(max, j['duration'] || 0), 0);
      const totalDurationHours = Math.round((maxDurationSec / 3600) * 10) / 10;
      const hasNeedWash = repJunctions.some(j => j['wash stated'] === 'need wash');
      const allClean = repJunctions.length > 0 && repJunctions.every(j => j['wash stated'] === 'wash');

      const info: DaySessionInfo = {
        report,
        junctions: repJunctions,
        garments: repGarments,
        totalDurationHours: totalDurationHours > 0 ? totalDurationHours : 4,
        hasNeedWash,
        allClean
      };

      const existing = map.get(dateKey) || [];
      existing.push(info);
      map.set(dateKey, existing);
    });

    return map;
  }, [reports, junctions, clothes]);

  // Calendar Grid generation
  const monthStart = new Date(year, month, 1);
  const monthEnd = new Date(year, month + 1, 0);
  const startDayIndex = monthStart.getDay(); // 0 = Sunday
  const daysInMonth = monthEnd.getDate();

  // Previous month trailing days
  const prevMonthEnd = new Date(year, month, 0);
  const prevMonthDaysCount = prevMonthEnd.getDate();

  // Grid cells
  const calendarCells = useMemo(() => {
    const cells: Array<{
      dateKey: string;
      dayNumber: number;
      isCurrentMonth: boolean;
      isToday: boolean;
      sessions: DaySessionInfo[];
    }> = [];

    const todayStr = new Date().toISOString().slice(0, 10);

    // 1. Previous month trailing days
    for (let i = startDayIndex - 1; i >= 0; i--) {
      const dayNum = prevMonthDaysCount - i;
      const d = new Date(year, month - 1, dayNum);
      const dateKey = d.toISOString().slice(0, 10);
      cells.push({
        dateKey,
        dayNumber: dayNum,
        isCurrentMonth: false,
        isToday: dateKey === todayStr,
        sessions: sessionsByDate.get(dateKey) || []
      });
    }

    // 2. Current month days
    for (let dayNum = 1; dayNum <= daysInMonth; dayNum++) {
      const d = new Date(year, month, dayNum);
      const dateKey = d.toISOString().slice(0, 10);
      cells.push({
        dateKey,
        dayNumber: dayNum,
        isCurrentMonth: true,
        isToday: dateKey === todayStr,
        sessions: sessionsByDate.get(dateKey) || []
      });
    }

    // 3. Next month leading days (to complete 35 or 42 grid items)
    const remaining = (7 - (cells.length % 7)) % 7;
    for (let dayNum = 1; dayNum <= remaining; dayNum++) {
      const d = new Date(year, month + 1, dayNum);
      const dateKey = d.toISOString().slice(0, 10);
      cells.push({
        dateKey,
        dayNumber: dayNum,
        isCurrentMonth: false,
        isToday: dateKey === todayStr,
        sessions: sessionsByDate.get(dateKey) || []
      });
    }

    return cells;
  }, [year, month, startDayIndex, daysInMonth, prevMonthDaysCount, sessionsByDate]);

  const monthName = currentDate.toLocaleString('default', { month: 'long', year: 'numeric' });

  // Count month stats
  const monthStats = useMemo(() => {
    let wearCount = 0;
    let needWashDays = 0;
    let totalHours = 0;

    calendarCells.forEach(cell => {
      if (cell.isCurrentMonth && cell.sessions.length > 0) {
        wearCount += cell.sessions.length;
        if (cell.sessions.some(s => s.hasNeedWash)) needWashDays++;
        cell.sessions.forEach(s => totalHours += s.totalDurationHours);
      }
    });

    return {
      wearCount,
      needWashDays,
      totalHours: Math.round(totalHours * 10) / 10
    };
  }, [calendarCells]);

  // Selected Day sessions modal/sheet data
  const selectedDaySessions = selectedDayDate ? sessionsByDate.get(selectedDayDate) || [] : [];

  return (
    <div id="wear-calendar-view" className="space-y-4 w-full">
      {/* Calendar Header Navigation */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-purple-100 text-purple-700">
            <CalendarIcon className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg sm:text-xl font-bold text-slate-900 tracking-tight">
              {monthName}
            </h2>
            <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
              <span>{monthStats.wearCount} wear sessions</span>
              <span>·</span>
              <span>{monthStats.totalHours} hrs logged</span>
              {monthStats.needWashDays > 0 && (
                <>
                  <span>·</span>
                  <span className="text-amber-600 font-semibold flex items-center gap-1">
                    <Droplets className="w-3 h-3" />
                    {monthStats.needWashDays} laundry days
                  </span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Navigation & Controls */}
        <div className="flex items-center gap-2 self-end sm:self-auto">
          <button
            type="button"
            onClick={handleToday}
            className="px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 text-xs font-semibold text-slate-700 transition-colors"
          >
            Today
          </button>

          <div className="flex items-center rounded-xl border border-slate-200 bg-slate-50 p-0.5">
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
              className="inline-flex items-center gap-1 px-3.5 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Log Outfit</span>
            </button>
          )}
        </div>
      </div>

      {/* Legend Bar */}
      <div className="flex flex-wrap items-center justify-between px-3 py-2 bg-slate-50 rounded-2xl border border-slate-200/80 text-[11px] text-slate-600 gap-2">
        <div className="flex items-center gap-4 flex-wrap font-medium">
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-purple-500 ring-2 ring-purple-200" />
            <span>👔 Wear session</span>
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500 ring-2 ring-amber-200" />
            <span>⚠️ Need wash day</span>
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-emerald-200" />
            <span>✨ Washed clean</span>
          </span>
        </div>
        <span className="text-slate-400">
          Click any day or session card to open full report
        </span>
      </div>

      {/* Calendar Grid */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden">
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

        {/* Days Cells */}
        <div className="grid grid-cols-7 auto-rows-fr divide-x divide-y divide-slate-100">
          {calendarCells.map((cell, idx) => {
            const hasSessions = cell.sessions.length > 0;
            const hasNeedWash = cell.sessions.some(s => s.hasNeedWash);
            const isSelected = selectedDayDate === cell.dateKey;

            return (
              <div
                key={idx}
                onClick={() => {
                  if (hasSessions) {
                    if (cell.sessions.length === 1) {
                      onSelectReport(cell.sessions[0].report);
                    } else {
                      setSelectedDayDate(cell.dateKey);
                    }
                  }
                }}
                className={`min-h-[85px] sm:min-h-[115px] p-1.5 sm:p-2 flex flex-col justify-between transition-colors relative group ${
                  !cell.isCurrentMonth
                    ? 'bg-slate-50/40 text-slate-400'
                    : hasSessions
                    ? hasNeedWash
                      ? 'bg-amber-50/30 hover:bg-amber-50/60 cursor-pointer'
                      : 'bg-purple-50/25 hover:bg-purple-50/50 cursor-pointer'
                    : 'bg-white hover:bg-slate-50/60'
                } ${cell.isToday ? 'ring-2 ring-inset ring-purple-600/60' : ''} ${
                  isSelected ? 'bg-purple-100/40 ring-2 ring-inset ring-purple-600' : ''
                }`}
              >
                {/* Day Number Header */}
                <div className="flex items-center justify-between">
                  <span
                    className={`inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-bold ${
                      cell.isToday
                        ? 'bg-purple-600 text-white shadow-xs'
                        : cell.isCurrentMonth
                        ? 'text-slate-800'
                        : 'text-slate-400'
                    }`}
                  >
                    {cell.dayNumber}
                  </span>

                  {/* Indicators / Badge */}
                  {hasSessions && (
                    <div className="flex items-center gap-1">
                      {hasNeedWash ? (
                        <span
                          title="Need wash recorded"
                          className="w-2 h-2 rounded-full bg-amber-500 ring-1 ring-amber-300"
                        />
                      ) : (
                        <span
                          title="Washed clean"
                          className="w-2 h-2 rounded-full bg-emerald-500 ring-1 ring-emerald-300"
                        />
                      )}
                      <span className="text-[10px] font-bold font-mono px-1 rounded bg-slate-200/70 text-slate-700">
                        {cell.sessions.length}
                      </span>
                    </div>
                  )}
                </div>

                {/* Session previews inside day cell */}
                <div className="mt-1 space-y-1 overflow-hidden flex-1 flex flex-col justify-start">
                  {cell.sessions.slice(0, 2).map((s, sIdx) => (
                    <div
                      key={sIdx}
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectReport(s.report);
                      }}
                      className={`px-1.5 py-0.5 rounded-lg border text-[10px] font-medium truncate flex items-center justify-between gap-1 shadow-2xs hover:scale-[1.02] transition-transform ${
                        s.hasNeedWash
                          ? 'bg-amber-100/90 text-amber-900 border-amber-300'
                          : 'bg-purple-100/90 text-purple-900 border-purple-200'
                      }`}
                      title={`${s.report['Name']} (${s.totalDurationHours}h) - Click to view full report`}
                    >
                      <span className="truncate flex-1 font-semibold">
                        {s.report['Name'] || 'Wear Session'}
                      </span>
                      <span className="text-[9px] opacity-75 font-mono shrink-0">
                        {s.totalDurationHours}h
                      </span>
                    </div>
                  ))}

                  {cell.sessions.length > 2 && (
                    <span className="text-[9px] font-bold text-purple-600 block pl-1">
                      +{cell.sessions.length - 2} more sessions
                    </span>
                  )}
                </div>

                {/* Bottom Wash / Wear status icon bar */}
                {hasSessions && (
                  <div className="pt-1 flex items-center gap-1.5 text-[9px] text-slate-500 font-mono">
                    <span className="flex items-center gap-0.5 text-purple-700">
                      <Shirt className="w-2.5 h-2.5" />
                      <span>{cell.sessions.reduce((acc, s) => acc + s.garments.length, 0)} items</span>
                    </span>
                    {hasNeedWash && (
                      <span className="flex items-center gap-0.5 text-amber-600 font-bold ml-auto">
                        <Droplets className="w-2.5 h-2.5" />
                        <span>wash</span>
                      </span>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Popover / Drawer for days with multiple sessions */}
      {selectedDayDate && selectedDaySessions.length > 0 && (
        <div className="bg-white rounded-3xl p-5 border border-purple-200 shadow-lg space-y-4 animate-in fade-in duration-150">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <CalendarIcon className="w-4 h-4 text-purple-600" />
              <h3 className="text-sm font-bold text-slate-900">
                Sessions on {new Date(selectedDayDate).toLocaleDateString(undefined, {
                  weekday: 'short',
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric'
                })}
              </h3>
              <span className="text-xs text-purple-700 font-bold">
                ({selectedDaySessions.length} sessions)
              </span>
            </div>
            <button
              type="button"
              onClick={() => setSelectedDayDate(null)}
              className="text-xs font-semibold text-slate-500 hover:text-slate-800 px-2 py-1 rounded-lg hover:bg-slate-100"
            >
              Close
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {selectedDaySessions.map((s, idx) => (
              <div
                key={idx}
                onClick={() => onSelectReport(s.report)}
                className="p-4 rounded-2xl bg-slate-50 hover:bg-purple-50/70 border border-slate-200 hover:border-purple-300 cursor-pointer transition-all space-y-2 group shadow-2xs"
              >
                <div className="flex items-center justify-between">
                  <h4 className="font-bold text-sm text-slate-900 group-hover:text-purple-700 transition-colors truncate">
                    {s.report['Name']}
                  </h4>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-200 text-slate-700 flex items-center gap-1 font-mono">
                    <Clock className="w-3 h-3" />
                    {s.totalDurationHours} hrs
                  </span>
                </div>

                {/* Garments worn */}
                <div className="flex flex-wrap gap-1">
                  {s.garments.map((g, gIdx) => {
                    const gName = g.garment?.['Name'] || (typeof g.junction['clothes(new)'] === 'object' ? g.junction['clothes(new)'].title : 'Garment');
                    return (
                      <span
                        key={gIdx}
                        onClick={(e) => {
                          if (g.garment && onSelectGarment) {
                            e.stopPropagation();
                            onSelectGarment(g.garment);
                          }
                        }}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-white border border-slate-200 text-[10px] font-medium text-slate-800 hover:border-purple-300"
                      >
                        <Shirt className="w-2.5 h-2.5 text-purple-600" />
                        <span>{gName}</span>
                        <span className={`px-1 py-0.2 rounded text-[8px] font-bold ${
                          g.junction['wash stated'] === 'need wash' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                        }`}>
                          {g.junction['wash stated']}
                        </span>
                      </span>
                    );
                  })}
                </div>

                <div className="pt-1 flex items-center justify-between text-[11px] text-purple-600 font-semibold">
                  <span className="flex items-center gap-1">
                    <Eye className="w-3.5 h-3.5" />
                    Open full-screen report & edit
                  </span>
                  <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
