import React, { useState, useMemo } from 'react';
import { WearingReport, ReportClothesJunction, ClothesItem } from '../types';
import {
  Calendar, Clock, Plus, Sparkles, Shirt, ChevronRight, Eye, ShieldCheck,
  Layers, CalendarDays, ListFilter, Droplets, ChevronDown, ChevronUp, Image as ImageIcon,
  Copy, CheckCircle2, Loader2, RefreshCw
} from 'lucide-react';
import { WearingReportDetailModal } from './WearingReportDetailModal';
import { WearCalendar } from './WearCalendar';
import { resolveImageSource } from '../utils/imageUtils';
import { toDateKey, formatCalendarDate, getTodayDateKey } from '../utils/calendarCalculations';
import { dataService } from '../services/dataService';

interface WearJournalProps {
  reports: WearingReport[];
  junctions: ReportClothesJunction[];
  clothes: ClothesItem[];
  onOpenLogModal: () => void;
  onSelectClothes?: (clothes: ClothesItem) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
  selectedReport?: WearingReport | null;
  onSelectReport?: (report: WearingReport | null) => void;
  onDeleteReport?: (reportId: string) => void;
  onUpdateReport?: (report: WearingReport) => void;
}

type GroupByMode = 'none' | 'month' | 'year' | 'clothes';
type ViewMode = 'list' | 'calendar';

export const WearJournal: React.FC<WearJournalProps> = ({
  reports,
  junctions,
  clothes,
  onOpenLogModal,
  onSelectClothes,
  onSelectGarment,
  selectedReport,
  onSelectReport,
  onDeleteReport,
  onUpdateReport
}) => {
  const handleSelectClothesItem = (item: ClothesItem) => {
    if (onSelectClothes) onSelectClothes(item);
    else if (onSelectGarment) onSelectGarment(item);
  };
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [groupBy, setGroupBy] = useState<GroupByMode>('none');
  const [searchQuery, setSearchQuery] = useState('');
  const [localSelectedReport, setLocalSelectedReport] = useState<WearingReport | null>(null);
  const [isDeduplicating, setIsDeduplicating] = useState(false);
  const [dedupeMessage, setDedupeMessage] = useState<string | null>(null);

  // Detect duplicate wearing reports and duplicate clothes links in the current state
  const detectedDuplicateReports = useMemo(() => dataService.findDuplicateReports(), [reports, junctions]);
  
  const redundantJunctionsCount = useMemo(() => {
    const reportClothesCounts = new Map<string, number>();
    junctions.forEach(j => {
      const repId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      const key = `${repId}___${cId}`;
      reportClothesCounts.set(key, (reportClothesCounts.get(key) || 0) + 1);
    });
    let count = 0;
    reportClothesCounts.forEach(cnt => {
      if (cnt > 1) count += (cnt - 1);
    });
    return count;
  }, [junctions]);

  const handleRunDeduplication = async () => {
    setIsDeduplicating(true);
    setDedupeMessage(null);
    try {
      const res = await dataService.deduplicateAll();
      const totalRemoved = res.reports.removedReportsCount + res.junctions.removedCount;
      if (totalRemoved > 0) {
        setDedupeMessage(`Cleaned ${res.reports.removedReportsCount} duplicate wearing reports & ${res.junctions.removedCount} redundant clothes links.`);
      } else {
        setDedupeMessage('All clean! No duplicate wearing reports or redundant clothes links found.');
      }
      setTimeout(() => setDedupeMessage(null), 5000);
    } catch (err: any) {
      setDedupeMessage('Error during deduplication: ' + (err?.message || 'Unknown error'));
    } finally {
      setIsDeduplicating(false);
    }
  };

  // Track expanded groups - default to empty set so all groups are collapsed by default
  const [expandedGroupKeys, setExpandedGroupKeys] = useState<Set<string>>(new Set());

  const toggleGroupCollapse = (groupKey: string) => {
    setExpandedGroupKeys(prev => {
      const next = new Set(prev);
      if (next.has(groupKey)) {
        next.delete(groupKey);
      } else {
        next.add(groupKey);
      }
      return next;
    });
  };

  const activeReport = selectedReport !== undefined ? selectedReport : localSelectedReport;
  const setActiveReport = (rep: WearingReport | null) => {
    if (onSelectReport) {
      onSelectReport(rep);
    } else {
      setLocalSelectedReport(rep);
    }
  };

  // Map each report to its junction records and clothes items, sorted newest wear date first
  const enhancedReports = useMemo(() => {
    const list = reports.map(report => {
      const sessionJunctions = junctions.filter(j => {
        const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
        return rId === report.id;
      });

      const sessionClothes = sessionJunctions.map(j => {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        const garment = clothes.find(c => c.id === cId);
        return {
          junction: j,
          garment
        };
      });

      const maxDurationSec = sessionJunctions.reduce((max, j) => Math.max(max, j['duration'] || 0), 0);
      const durationHours = (maxDurationSec / 3600).toFixed(1);

      return {
        report,
        sessionClothes,
        durationHours: Number(durationHours) > 0 ? durationHours : '4.0',
        maxDurationSec
      };
    });

    // Ensure newest wear date is always at the top
    return list.sort((a, b) => {
      const dateA = a.report['wear date'] ? new Date(a.report['wear date']).getTime() : 0;
      const dateB = b.report['wear date'] ? new Date(b.report['wear date']).getTime() : 0;
      if (dateB !== dateA) return dateB - dateA;
      const createdA = a.report['created'] ? new Date(a.report['created']).getTime() : 0;
      const createdB = b.report['created'] ? new Date(b.report['created']).getTime() : 0;
      return createdB - createdA;
    });
  }, [reports, junctions, clothes]);

  // Filtered reports with comprehensive field matching
  const filteredReports = useMemo(() => {
    return enhancedReports.filter(({ report, sessionClothes }) => {
      if (!searchQuery) return true;
      const q = searchQuery.toLowerCase().trim();
      const matchesTitle = (report['Name'] || '').toLowerCase().includes(q);
      const matchesDetail = (report['report detail'] || '').toLowerCase().includes(q);
      const matchesSensory = (report['shoulder feel'] || '').toLowerCase().includes(q) ||
                             (report['mid breast'] || '').toLowerCase().includes(q) ||
                             (report['night mid breast'] || '').toLowerCase().includes(q) ||
                             (report['sweat'] || '').toLowerCase().includes(q) ||
                             (report['night sweat'] || '').toLowerCase().includes(q) ||
                             (report['leg feel'] || '').toLowerCase().includes(q) ||
                             (report['night leg feel'] || '').toLowerCase().includes(q) ||
                             (report['AI Wearing Analysis'] || '').toLowerCase().includes(q);
      const matchesGarments = sessionClothes.some(sc => {
        const gName = (sc.garment?.['Name'] || '').toLowerCase();
        const jTitle = (sc.junction.clothesTitle || '').toLowerCase();
        const jObjTitle = (typeof sc.junction['clothes(new)'] === 'object' ? sc.junction['clothes(new)']?.title || '' : '').toLowerCase();
        return gName.includes(q) || jTitle.includes(q) || jObjTitle.includes(q);
      });
      return matchesTitle || matchesDetail || matchesSensory || matchesGarments;
    });
  }, [enhancedReports, searchQuery]);

  // Grouped Reports structure
  const groupedData = useMemo(() => {
    if (groupBy === 'none') {
      return [{ key: 'all', title: 'All Reports', count: filteredReports.length, items: filteredReports }];
    }

    if (groupBy === 'month') {
      const map = new Map<string, typeof filteredReports>();
      filteredReports.forEach(item => {
        const dateKey = toDateKey(item.report['wear date']) || getTodayDateKey();
        const [y, m] = dateKey.split('-').map(Number);
        const key = `${y}-${String(m).padStart(2, '0')}`;
        const existing = map.get(key) || [];
        existing.push(item);
        map.set(key, existing);
      });

      // Sort descending by month
      const sortedKeys = Array.from(map.keys()).sort((a, b) => b.localeCompare(a));
      return sortedKeys.map(k => {
        const items = map.get(k)!;
        const [y, m] = k.split('-');
        const dateObj = new Date(parseInt(y, 10), parseInt(m, 10) - 1, 1);
        const title = dateObj.toLocaleString('default', { month: 'long', year: 'numeric' });
        const totalHrs = items.reduce((sum, it) => sum + parseFloat(it.durationHours), 0).toFixed(1);
        return {
          key: k,
          title,
          subtitle: `${totalHrs} hrs total worn`,
          count: items.length,
          items
        };
      });
    }

    if (groupBy === 'year') {
      const map = new Map<string, typeof filteredReports>();
      filteredReports.forEach(item => {
        const dateKey = toDateKey(item.report['wear date']) || getTodayDateKey();
        const yearStr = dateKey.slice(0, 4);
        const existing = map.get(yearStr) || [];
        existing.push(item);
        map.set(yearStr, existing);
      });

      const sortedYears = Array.from(map.keys()).sort((a, b) => b.localeCompare(a));
      return sortedYears.map(yr => {
        const items = map.get(yr)!;
        const totalHrs = items.reduce((sum, it) => sum + parseFloat(it.durationHours), 0).toFixed(1);
        return {
          key: yr,
          title: `Year ${yr}`,
          subtitle: `${totalHrs} hrs total worn`,
          count: items.length,
          items
        };
      });
    }

    if (groupBy === 'clothes') {
      // Group reports by each distinct clothes item
      const map = new Map<string, { garment?: ClothesItem; name: string; items: typeof filteredReports }>();

      filteredReports.forEach(item => {
        if (item.sessionClothes.length === 0) {
          const key = 'unassigned';
          const existing = map.get(key) || { name: 'No Clothes Linked', items: [] };
          existing.items.push(item);
          map.set(key, existing);
        } else {
          item.sessionClothes.forEach(sc => {
            const gId = sc.garment?.id || (typeof sc.junction['clothes(new)'] === 'object' ? sc.junction['clothes(new)'].id : String(sc.junction['clothes(new)']));
            const gName = sc.garment?.['Name'] || (typeof sc.junction['clothes(new)'] === 'object' ? sc.junction['clothes(new)'].title : 'Clothes');
            const existing = map.get(gId) || { garment: sc.garment, name: gName, items: [] };
            if (!existing.items.some(it => it.report.id === item.report.id)) {
              existing.items.push(item);
            }
            map.set(gId, existing);
          });
        }
      });

      // Sort by clothes with most wearing reports first
      const sortedEntries = Array.from(map.entries()).sort((a, b) => b[1].items.length - a[1].items.length);
      return sortedEntries.map(([k, val]) => {
        return {
          key: k,
          title: val.name,
          garment: val.garment,
          subtitle: `${val.items.length} wearing reports`,
          count: val.items.length,
          items: val.items
        };
      });
    }

    return [];
  }, [groupBy, filteredReports]);

  return (
    <div id="wear-journal-view" className="space-y-6 w-full max-w-full">
      {/* Full-screen Wearing Report Detail Modal (only when unmanaged by parent App) */}
      {selectedReport === undefined && activeReport && (
        <WearingReportDetailModal
          report={activeReport}
          junctions={junctions}
          clothes={clothes}
          onClose={() => setActiveReport(null)}
          onSelectClothes={(clothesItem) => {
            setActiveReport(null);
            handleSelectClothesItem(clothesItem);
          }}
          onDeleteReport={(id) => {
            setActiveReport(null);
            if (onDeleteReport) onDeleteReport(id);
          }}
          onUpdateReport={(rep) => {
            if (onUpdateReport) onUpdateReport(rep);
          }}
        />
      )}

      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-[#3265a9] shadow-xs flex flex-col gap-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-xs shrink-0">
              <Calendar className="w-5 h-5 text-white" />
            </div>
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 font-mono">
                Database Record
              </span>
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                Wearing Report Journal
              </h2>
            </div>
          </div>

          {/* Action & View Mode Toggles */}
          <div className="flex items-center gap-2 flex-wrap sm:flex-nowrap">
            {/* View Mode Toggle: List vs Calendar */}
            <div className="flex items-center p-1 rounded-2xl bg-slate-100 border border-slate-200 text-xs font-semibold">
              <button
                type="button"
                onClick={() => setViewMode('list')}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl transition-all ${
                  viewMode === 'list'
                    ? 'bg-white text-indigo-950 font-bold shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <Layers className={`w-3.5 h-3.5 ${viewMode === 'list' ? 'text-indigo-600' : 'text-slate-500'}`} />
                <span>Journal List</span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('calendar')}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl transition-all ${
                  viewMode === 'calendar'
                    ? 'bg-white text-indigo-950 font-bold shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/50'
                }`}
              >
                <CalendarDays className={`w-3.5 h-3.5 ${viewMode === 'calendar' ? 'text-indigo-600' : 'text-slate-500'}`} />
                <span>Calendar View</span>
              </button>
            </div>

            {/* One-Click Deduplication Tool */}
            <button
              type="button"
              onClick={handleRunDeduplication}
              disabled={isDeduplicating}
              title="Scan and merge duplicate wearing reports sharing the same date, and eliminate duplicate clothes appearances"
              className="px-3 py-2 rounded-2xl bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-900 font-bold text-xs shadow-xs transition-all flex items-center gap-1.5 shrink-0 active:scale-95 disabled:opacity-60"
            >
              {isDeduplicating ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin text-amber-700" />
              ) : (
                <Copy className="w-3.5 h-3.5 text-amber-700" />
              )}
              <span>Deduplicate</span>
              {(detectedDuplicateReports.length > 0 || redundantJunctionsCount > 0) && (
                <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-amber-600 text-white font-bold ml-0.5">
                  {detectedDuplicateReports.length + redundantJunctionsCount}
                </span>
              )}
            </button>

            <button
              onClick={onOpenLogModal}
              className="px-4 py-2 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-xs transition-all flex items-center gap-1.5 shrink-0 active:scale-95"
            >
              <Plus className="w-4 h-4 text-white" />
              <span>Log Outfit</span>
            </button>
          </div>
        </div>

        {dedupeMessage && (
          <div className="p-3 rounded-2xl bg-amber-50/90 border border-amber-200 text-amber-900 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium">{dedupeMessage}</span>
          </div>
        )}

        <p className="text-xs text-slate-500 leading-relaxed">
          {viewMode === 'calendar'
            ? 'Visual calendar displaying wearing reports and wash requirements day-by-day. Tap any day to inspect details in the panel below.'
            : 'Wearing reports organized by date or group. Reports are collapsed by default; click any group header to expand.'}
        </p>

        {/* Search Bar & Group By Selector (in List View) */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 pt-1">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search report by name, sensory notes, or clothes..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full px-4 py-2 rounded-xl border border-slate-200 bg-slate-50 hover:bg-white focus:bg-white text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-indigo-500 shadow-2xs transition-all"
            />
          </div>

          {/* Group By Filter Buttons */}
          {viewMode === 'list' && (
            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-2xl border border-slate-200 text-xs shrink-0 overflow-x-auto">
              <span className="px-2 text-[11px] font-mono font-bold text-slate-500 uppercase flex items-center gap-1">
                <ListFilter className="w-3 h-3 text-slate-500" />
                Group:
              </span>
              <button
                type="button"
                onClick={() => setGroupBy('none')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  groupBy === 'none'
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-white hover:bg-slate-50 text-slate-700 shadow-2xs'
                }`}
              >
                None
              </button>
              <button
                type="button"
                onClick={() => setGroupBy('month')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  groupBy === 'month'
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-white hover:bg-slate-50 text-slate-700 shadow-2xs'
                }`}
              >
                Month
              </button>
              <button
                type="button"
                onClick={() => setGroupBy('year')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  groupBy === 'year'
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-white hover:bg-slate-50 text-slate-700 shadow-2xs'
                }`}
              >
                Year
              </button>
              <button
                type="button"
                onClick={() => setGroupBy('clothes')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  groupBy === 'clothes'
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-white hover:bg-slate-50 text-slate-700 shadow-2xs'
                }`}
              >
                Clothes
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Render Calendar View or List View */}
      {viewMode === 'calendar' ? (
        <WearCalendar
          reports={reports}
          junctions={junctions}
          clothes={clothes}
          onSelectReport={(rep) => setActiveReport(rep)}
          onSelectGarment={handleSelectClothesItem}
          onOpenLogModal={onOpenLogModal}
        />
      ) : (
        /* List View (with optional grouping by month / year / clothes) */
        <div className="space-y-6">
          <div className="flex items-center justify-between px-1 text-xs text-slate-500">
            <span className="font-semibold text-slate-700">
              {groupBy === 'none'
                ? 'All Wearing Reports'
                : `Grouped by ${groupBy.charAt(0).toUpperCase() + groupBy.slice(1)}`}
            </span>
            <div className="flex items-center gap-3">
              <span>{filteredReports.length} wearing reports</span>
              {groupBy !== 'none' && groupedData.length > 0 && (
                <button
                  type="button"
                  onClick={() => {
                    if (expandedGroupKeys.size === groupedData.length) {
                      setExpandedGroupKeys(new Set());
                    } else {
                      setExpandedGroupKeys(new Set(groupedData.map(g => g.key)));
                    }
                  }}
                  className="px-2 py-0.5 rounded-lg border border-slate-300 hover:border-slate-500 text-[11px] font-semibold text-slate-700 bg-transparent transition-colors"
                >
                  {expandedGroupKeys.size === groupedData.length ? 'Collapse All' : 'Expand All'}
                </button>
              )}
            </div>
          </div>

          {groupedData.map((group) => {
            const isExpanded = groupBy === 'none' ? true : expandedGroupKeys.has(group.key);

            return (
              <div key={group.key} className="space-y-3">
                {/* Group Header (if grouped) - default collapsed */}
                {groupBy !== 'none' && (
                  <div
                    onClick={() => toggleGroupCollapse(group.key)}
                    className="flex items-center justify-between px-4 py-3 bg-white hover:bg-slate-50 border border-slate-300 rounded-2xl shadow-2xs cursor-pointer transition-colors select-none"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      {group.garment ? (
                        <div className="w-8 h-8 rounded-xl border border-slate-200 flex items-center justify-center shrink-0 overflow-hidden">
                          {(() => {
                            const imgSrc = group.garment['Pictures']?.[0] ? resolveImageSource(group.garment['Pictures'][0]) : '';
                            return imgSrc ? (
                              <img
                                src={imgSrc}
                                alt={group.title}
                                className="w-full h-full object-contain p-0.5"
                                referrerPolicy="no-referrer"
                              />
                            ) : (
                              <Shirt className="w-4 h-4 text-slate-400" />
                            );
                          })()}
                        </div>
                      ) : (
                        <div className="w-8 h-8 rounded-xl border border-slate-200 text-slate-700 flex items-center justify-center shrink-0 font-bold">
                          <Calendar className="w-4 h-4" />
                        </div>
                      )}

                      <div className="min-w-0">
                        <h3 className="font-bold text-slate-900 text-sm truncate">
                          {group.title}
                        </h3>
                        {group.subtitle && (
                          <p className="text-[11px] text-slate-500 font-mono">
                            {group.subtitle}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <span className="px-2.5 py-1 rounded-xl text-xs font-bold bg-indigo-100 text-indigo-800">
                        {group.count} wearing reports
                      </span>
                      {isExpanded ? (
                        <ChevronUp className="w-4 h-4 text-slate-500" />
                      ) : (
                        <ChevronDown className="w-4 h-4 text-slate-500" />
                      )}
                    </div>
                  </div>
                )}

                {/* Group Items: narrowed right side border so user easily identifies them under group header */}
                {isExpanded && (
                  <div className={`space-y-3 ${groupBy !== 'none' ? 'pl-3 sm:pl-4 pr-5 sm:pr-8 border-l-2 border-slate-200 ml-3 sm:ml-4' : ''}`}>
                    {group.items.map(({ report, sessionClothes, durationHours }) => (
                      <div
                        key={report.id}
                        onClick={() => setActiveReport(report)}
                        className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-2xs hover:border-indigo-400 hover:shadow-xs transition-all space-y-3 cursor-pointer group relative active:scale-[0.99]"
                      >
                        {/* Top Row: Date, Report Name & Duration */}
                        <div className="flex items-start justify-between gap-2 border-b border-slate-100 pb-3">
                          <div className="space-y-0.5 min-w-0">
                            <div className="flex items-center gap-2">
                              <Calendar className="w-3.5 h-3.5 text-indigo-600 shrink-0" />
                              <span className="font-bold text-slate-900 text-sm truncate">
                                {formatCalendarDate(report['wear date'], {
                                  weekday: 'short',
                                  year: 'numeric',
                                  month: 'short',
                                  day: 'numeric'
                                })}
                              </span>
                            </div>
                            <p className="text-[11px] text-slate-500 font-mono truncate">
                              {report['Name']}
                            </p>
                          </div>

                          <div className="flex items-center gap-2 shrink-0">
                            <span className="flex items-center gap-1 bg-indigo-50 border border-indigo-200 px-2.5 py-1 rounded-xl text-indigo-800 font-bold text-[11px]">
                              <Clock className="w-3 h-3 text-indigo-600" />
                              {durationHours} hrs
                            </span>
                            <span className="p-1.5 rounded-xl bg-slate-100 group-hover:bg-indigo-100 text-slate-600 group-hover:text-indigo-700 transition-colors">
                              <Eye className="w-3.5 h-3.5" />
                            </span>
                          </div>
                        </div>

                        {/* Clothes in this wearing report */}
                        <div>
                          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1.5 font-mono">
                            clothes(new) · Clothes Items
                          </span>
                          <div className="flex flex-wrap gap-1.5">
                            {sessionClothes.map(({ junction, garment }, idx) => {
                              const clothesName = garment?.['Name'] || (typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].title : 'Clothes');
                              const clothesType = garment?.['Clothes Type'] || junction['Clothes Type (from clothes(new))'];
                              return (
                                <div
                                  key={idx}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    if (garment) handleSelectClothesItem(garment);
                                    else setActiveReport(report);
                                  }}
                                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl border border-purple-200 bg-purple-50 hover:bg-purple-100 text-xs font-medium text-purple-950 transition-colors shadow-2xs"
                                >
                                  <Shirt className="w-3 h-3 text-purple-600 shrink-0" />
                                  <span className="font-semibold text-purple-950 truncate max-w-[140px]">
                                    {clothesName}
                                  </span>
                                  {clothesType && (
                                    <span className="text-[10px] text-purple-600">({clothesType})</span>
                                  )}
                                  <span className={`px-1.5 py-0.5 rounded-md text-[9px] font-bold ${
                                    junction['wash stated'] === 'need wash' ? 'bg-amber-100 text-amber-900 border border-amber-300' : 'bg-emerald-100 text-emerald-900 border border-emerald-300'
                                  }`}>
                                    {junction['wash stated'] === 'wash' ? 'Clean' : junction['wash stated']}
                                  </span>
                                </div>
                              );
                            })}
                          </div>
                        </div>

                        {/* Sensory feedback snippets with exact DB names */}
                        <div className="p-3 rounded-xl bg-slate-50/70 border border-slate-200 space-y-1.5 text-xs">
                          <div className="flex items-baseline justify-between gap-1.5">
                            <div className="flex items-baseline gap-1.5 min-w-0">
                              <span className="text-[10px] font-bold font-mono text-slate-500 uppercase shrink-0">
                                shoulder feel:
                              </span>
                              <span className={`text-[11px] truncate ${report['shoulder feel'] ? 'text-slate-800 font-mono' : 'text-slate-400 italic'}`}>
                                {report['shoulder feel'] || 'No data'}
                              </span>
                            </div>
                            {(report.fieldExplanations?.['shoulder feel'] || report.customFields?._fieldExplanations?.['shoulder feel']) && (
                              <span className="text-[10px] text-purple-600 font-medium shrink-0 truncate max-w-[130px]" title={report.fieldExplanations?.['shoulder feel'] || report.customFields?._fieldExplanations?.['shoulder feel']}>
                                💬 {report.fieldExplanations?.['shoulder feel'] || report.customFields?._fieldExplanations?.['shoulder feel']}
                              </span>
                            )}
                          </div>
                          <div className="flex items-baseline justify-between gap-1.5">
                            <div className="flex items-baseline gap-1.5 min-w-0">
                              <span className="text-[10px] font-bold font-mono text-slate-500 uppercase shrink-0">
                                mid breast:
                              </span>
                              <span className={`text-[11px] truncate ${report['mid breast'] ? 'text-slate-800 font-mono' : 'text-slate-400 italic'}`}>
                                {report['mid breast'] || 'No data'}
                              </span>
                            </div>
                            {(report.fieldExplanations?.['mid breast'] || report.customFields?._fieldExplanations?.['mid breast']) && (
                              <span className="text-[10px] text-purple-600 font-medium shrink-0 truncate max-w-[130px]" title={report.fieldExplanations?.['mid breast'] || report.customFields?._fieldExplanations?.['mid breast']}>
                                💬 {report.fieldExplanations?.['mid breast'] || report.customFields?._fieldExplanations?.['mid breast']}
                              </span>
                            )}
                          </div>
                          <div className="flex items-baseline justify-between gap-1.5">
                            <div className="flex items-baseline gap-1.5 min-w-0">
                              <span className="text-[10px] font-bold font-mono text-slate-500 uppercase shrink-0">
                                sweat:
                              </span>
                              <span className={`text-[11px] truncate ${report['sweat'] ? 'text-slate-800 font-mono' : 'text-slate-400 italic'}`}>
                                {report['sweat'] || 'No data'}
                              </span>
                            </div>
                            {(report.fieldExplanations?.['sweat'] || report.customFields?._fieldExplanations?.['sweat']) && (
                              <span className="text-[10px] text-purple-600 font-medium shrink-0 truncate max-w-[130px]" title={report.fieldExplanations?.['sweat'] || report.customFields?._fieldExplanations?.['sweat']}>
                                💬 {report.fieldExplanations?.['sweat'] || report.customFields?._fieldExplanations?.['sweat']}
                              </span>
                            )}
                          </div>
                          <div className="flex items-baseline justify-between gap-1.5">
                            <div className="flex items-baseline gap-1.5 min-w-0">
                              <span className="text-[10px] font-bold font-mono text-slate-500 uppercase shrink-0">
                                leg feel:
                              </span>
                              <span className={`text-[11px] truncate ${report['leg feel'] ? 'text-slate-800 font-mono' : 'text-slate-400 italic'}`}>
                                {report['leg feel'] || 'No data'}
                              </span>
                            </div>
                            {(report.fieldExplanations?.['leg feel'] || report.customFields?._fieldExplanations?.['leg feel']) && (
                              <span className="text-[10px] text-purple-600 font-medium shrink-0 truncate max-w-[130px]" title={report.fieldExplanations?.['leg feel'] || report.customFields?._fieldExplanations?.['leg feel']}>
                                💬 {report.fieldExplanations?.['leg feel'] || report.customFields?._fieldExplanations?.['leg feel']}
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Tap to view full detail callout */}
                        <div className="flex items-center justify-between pt-1 text-[11px] text-slate-700 font-semibold group-hover:text-slate-900">
                          <span className="flex items-center gap-1">
                            <Eye className="w-3.5 h-3.5 text-slate-600" />
                            Tap to view full detail &amp; edit this wearing report
                          </span>
                          <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform text-slate-400" />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}

          {filteredReports.length === 0 && (
            <div className="p-8 text-center rounded-3xl bg-white border border-slate-200 text-slate-500">
              <Calendar className="w-10 h-10 mx-auto text-slate-300 mb-2" />
              <p className="font-bold text-slate-700 text-sm">No wearing report entries found</p>
              <p className="text-xs text-slate-400 mt-1">Try another search query or log a new outfit above.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
