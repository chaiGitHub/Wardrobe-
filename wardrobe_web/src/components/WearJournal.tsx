import React, { useState, useMemo } from 'react';
import { WearingReport, ReportClothesJunction, ClothesItem } from '../types';
import {
  Calendar, Clock, Plus, Sparkles, Shirt, ChevronRight, Eye, ShieldCheck,
  Layers, CalendarDays, ListFilter, Droplets, ChevronDown, ChevronUp, Image as ImageIcon
} from 'lucide-react';
import { SessionDetailModal } from './SessionDetailModal';
import { WearCalendar } from './WearCalendar';
import { resolveImageSource } from '../utils/imageUtils';

interface WearJournalProps {
  reports: WearingReport[];
  junctions: ReportClothesJunction[];
  clothes: ClothesItem[];
  onOpenLogModal: () => void;
  onSelectGarment: (garment: ClothesItem) => void;
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
  onSelectGarment,
  selectedReport,
  onSelectReport,
  onDeleteReport,
  onUpdateReport
}) => {
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [groupBy, setGroupBy] = useState<GroupByMode>('none');
  const [searchQuery, setSearchQuery] = useState('');
  const [localSelectedReport, setLocalSelectedReport] = useState<WearingReport | null>(null);

  // Track collapsed groups
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});

  const toggleGroupCollapse = (groupKey: string) => {
    setCollapsedGroups(prev => ({
      ...prev,
      [groupKey]: !prev[groupKey]
    }));
  };

  const activeReport = selectedReport !== undefined ? selectedReport : localSelectedReport;
  const setActiveReport = (rep: WearingReport | null) => {
    if (onSelectReport) {
      onSelectReport(rep);
    } else {
      setLocalSelectedReport(rep);
    }
  };

  // Map each report to its junction records and clothes items
  const enhancedReports = useMemo(() => {
    return reports.map(report => {
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
  }, [reports, junctions, clothes]);

  // Filtered reports
  const filteredReports = useMemo(() => {
    return enhancedReports.filter(({ report, sessionClothes }) => {
      if (!searchQuery) return true;
      const q = searchQuery.toLowerCase();
      const matchesTitle = (report['Name'] || '').toLowerCase().includes(q);
      const matchesDetail = (report['report detail'] || '').toLowerCase().includes(q);
      const matchesSensory = (report['shoulder feel'] || '').toLowerCase().includes(q) ||
                             (report['mid breast'] || '').toLowerCase().includes(q) ||
                             (report['sweat'] || '').toLowerCase().includes(q) ||
                             (report['leg feel'] || '').toLowerCase().includes(q);
      const matchesGarments = sessionClothes.some(sc => (sc.garment?.['Name'] || '').toLowerCase().includes(q));
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
        const d = item.report['wear date'] ? new Date(item.report['wear date']) : new Date();
        const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
        const title = d.toLocaleString('default', { month: 'long', year: 'numeric' });
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
        const d = item.report['wear date'] ? new Date(item.report['wear date']) : new Date();
        const yearStr = String(d.getFullYear());
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
      // Group reports by each distinct garment
      const map = new Map<string, { garment?: ClothesItem; name: string; items: typeof filteredReports }>();

      filteredReports.forEach(item => {
        if (item.sessionClothes.length === 0) {
          const key = 'unassigned';
          const existing = map.get(key) || { name: 'No Garments Linked', items: [] };
          existing.items.push(item);
          map.set(key, existing);
        } else {
          item.sessionClothes.forEach(sc => {
            const gId = sc.garment?.id || (typeof sc.junction['clothes(new)'] === 'object' ? sc.junction['clothes(new)'].id : String(sc.junction['clothes(new)']));
            const gName = sc.garment?.['Name'] || (typeof sc.junction['clothes(new)'] === 'object' ? sc.junction['clothes(new)'].title : 'Garment');
            const existing = map.get(gId) || { garment: sc.garment, name: gName, items: [] };
            if (!existing.items.some(it => it.report.id === item.report.id)) {
              existing.items.push(item);
            }
            map.set(gId, existing);
          });
        }
      });

      // Sort by garments with most wearing sessions first
      const sortedEntries = Array.from(map.entries()).sort((a, b) => b[1].items.length - a[1].items.length);
      return sortedEntries.map(([k, val]) => {
        return {
          key: k,
          title: val.name,
          garment: val.garment,
          subtitle: `${val.items.length} wear sessions`,
          count: val.items.length,
          items: val.items
        };
      });
    }

    return [];
  }, [groupBy, filteredReports]);

  return (
    <div id="wear-journal-view" className="space-y-6 w-full max-w-full">
      {/* Full-screen Session Detail Modal */}
      <SessionDetailModal
        report={activeReport}
        junctions={junctions}
        clothes={clothes}
        onClose={() => setActiveReport(null)}
        onSelectGarment={(garment) => {
          setActiveReport(null);
          onSelectGarment(garment);
        }}
        onDeleteReport={(id) => {
          setActiveReport(null);
          if (onDeleteReport) onDeleteReport(id);
        }}
        onUpdateReport={(rep) => {
          if (onUpdateReport) onUpdateReport(rep);
        }}
      />

      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs flex flex-col gap-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="p-2.5 rounded-2xl bg-purple-100 text-purple-700 shrink-0">
              <Calendar className="w-5 h-5" />
            </span>
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-purple-600 font-mono">
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
            <div className="flex items-center p-1 rounded-2xl bg-slate-100 border border-slate-200/60 text-xs font-semibold">
              <button
                type="button"
                onClick={() => setViewMode('list')}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl transition-all ${
                  viewMode === 'list'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Layers className="w-3.5 h-3.5 text-purple-600" />
                <span>Journal List</span>
              </button>

              <button
                type="button"
                onClick={() => setViewMode('calendar')}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl transition-all ${
                  viewMode === 'calendar'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <CalendarDays className="w-3.5 h-3.5 text-indigo-600" />
                <span>Calendar View</span>
              </button>
            </div>

            <button
              onClick={onOpenLogModal}
              className="px-4 py-2 rounded-2xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs shadow-xs transition-all flex items-center gap-1.5 shrink-0"
            >
              <Plus className="w-4 h-4" />
              <span>Log Outfit</span>
            </button>
          </div>
        </div>

        <p className="text-xs text-slate-500 leading-relaxed">
          {viewMode === 'calendar'
            ? 'Visual calendar displaying wear sessions and wash requirements day-by-day. Tap any day or session to inspect or edit.'
            : 'Tap any wearing session to view full details with complete database fields, sensory ratings, full-screen photos, and editable entries.'}
        </p>

        {/* Search Bar & Group By Selector (in List View) */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 pt-1">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search report by name, sensory notes, or garment..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full px-4 py-2.5 rounded-2xl border border-slate-200 bg-slate-50/60 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-500"
            />
          </div>

          {/* Group By Filter Buttons */}
          {viewMode === 'list' && (
            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-2xl border border-slate-200/60 text-xs shrink-0 overflow-x-auto">
              <span className="px-2 text-[11px] font-mono font-bold text-slate-500 uppercase flex items-center gap-1">
                <ListFilter className="w-3 h-3" />
                Group:
              </span>
              <button
                type="button"
                onClick={() => setGroupBy('none')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition-all ${
                  groupBy === 'none'
                    ? 'bg-white text-slate-900 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                None
              </button>
              <button
                type="button"
                onClick={() => setGroupBy('month')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition-all ${
                  groupBy === 'month'
                    ? 'bg-white text-slate-900 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Month
              </button>
              <button
                type="button"
                onClick={() => setGroupBy('year')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition-all ${
                  groupBy === 'year'
                    ? 'bg-white text-slate-900 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Year
              </button>
              <button
                type="button"
                onClick={() => setGroupBy('clothes')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition-all ${
                  groupBy === 'clothes'
                    ? 'bg-white text-slate-900 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
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
          onSelectGarment={onSelectGarment}
          onOpenLogModal={onOpenLogModal}
        />
      ) : (
        /* List View (with optional grouping by month / year / clothes) */
        <div className="space-y-6">
          <div className="flex items-center justify-between px-1 text-xs text-slate-500">
            <span className="font-semibold text-slate-700">
              {groupBy === 'none'
                ? 'All Wearing Reports & Sessions'
                : `Grouped by ${groupBy.charAt(0).toUpperCase() + groupBy.slice(1)}`}
            </span>
            <span>{filteredReports.length} sessions</span>
          </div>

          {groupedData.map((group) => {
            const isCollapsed = Boolean(collapsedGroups[group.key]);

            return (
              <div key={group.key} className="space-y-3">
                {/* Group Header (if grouped) */}
                {groupBy !== 'none' && (
                  <div
                    onClick={() => toggleGroupCollapse(group.key)}
                    className="flex items-center justify-between px-4 py-3 bg-white hover:bg-slate-50 border border-slate-200/90 rounded-2xl shadow-2xs cursor-pointer transition-colors select-none"
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      {group.garment ? (
                        <div className="w-8 h-8 rounded-xl bg-purple-100 flex items-center justify-center shrink-0 overflow-hidden">
                          {group.garment['Pictures'] && group.garment['Pictures'].length > 0 ? (
                            <img
                              src={resolveImageSource(group.garment['Pictures'][0])}
                              alt={group.title}
                              className="w-full h-full object-cover"
                              referrerPolicy="no-referrer"
                            />
                          ) : (
                            <Shirt className="w-4 h-4 text-purple-600" />
                          )}
                        </div>
                      ) : (
                        <div className="w-8 h-8 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center shrink-0 font-bold">
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
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-purple-50 text-purple-700 border border-purple-200">
                        {group.count} sessions
                      </span>
                      {isCollapsed ? (
                        <ChevronDown className="w-4 h-4 text-slate-400" />
                      ) : (
                        <ChevronUp className="w-4 h-4 text-slate-400" />
                      )}
                    </div>
                  </div>
                )}

                {/* Group Items */}
                {!isCollapsed && (
                  <div className="space-y-3">
                    {group.items.map(({ report, sessionClothes, durationHours }) => (
                      <div
                        key={report.id}
                        onClick={() => setActiveReport(report)}
                        className="bg-white rounded-3xl border border-slate-200/80 p-4 sm:p-5 shadow-xs hover:border-purple-300 hover:shadow-md transition-all space-y-3 cursor-pointer group relative active:scale-[0.99]"
                      >
                        {/* Top Row: Date, Report Name & Duration */}
                        <div className="flex items-start justify-between gap-2 border-b border-slate-100 pb-3">
                          <div className="space-y-0.5 min-w-0">
                            <div className="flex items-center gap-2">
                              <Calendar className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                              <span className="font-bold text-slate-900 text-sm truncate">
                                {new Date(report['wear date']).toLocaleDateString(undefined, {
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
                            <span className="flex items-center gap-1 bg-slate-100 px-2.5 py-1 rounded-full text-slate-700 font-semibold text-[11px]">
                              <Clock className="w-3 h-3 text-slate-500" />
                              {durationHours} hrs
                            </span>
                            <span className="p-1.5 rounded-xl bg-purple-50 text-purple-600 group-hover:bg-purple-100 transition-colors">
                              <Eye className="w-3.5 h-3.5" />
                            </span>
                          </div>
                        </div>

                        {/* Outfits in this session */}
                        <div>
                          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1.5">
                            clothes(new) · Outfit Items
                          </span>
                          <div className="flex flex-wrap gap-1.5">
                            {sessionClothes.map(({ junction, garment }, idx) => {
                              const garmentName = garment?.['Name'] || (typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].title : 'Garment');
                              const clothesType = garment?.['Clothes Type'] || junction['Clothes Type (from clothes(new))'];
                              return (
                                <div
                                  key={idx}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    if (garment) onSelectGarment(garment);
                                    else setActiveReport(report);
                                  }}
                                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl border border-slate-200 bg-slate-50 hover:bg-purple-50 text-xs font-medium text-slate-800 transition-colors"
                                >
                                  <Shirt className="w-3 h-3 text-purple-500 shrink-0" />
                                  <span className="font-semibold text-slate-900 truncate max-w-[140px]">
                                    {garmentName}
                                  </span>
                                  {clothesType && (
                                    <span className="text-[10px] text-slate-400">({clothesType})</span>
                                  )}
                                  <span className={`px-1.5 py-0.2 rounded text-[9px] font-bold ${
                                    junction['wash stated'] === 'need wash' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                                  }`}>
                                    {junction['wash stated']}
                                  </span>
                                </div>
                              );
                            })}
                          </div>
                        </div>

                        {/* Sensory feedback snippets with exact DB names */}
                        <div className="p-3 rounded-2xl bg-slate-50 border border-slate-100/80 space-y-1.5 text-xs">
                          <div className="flex items-baseline gap-1.5">
                            <span className="text-[10px] font-bold font-mono text-slate-400 uppercase shrink-0">
                              shoulder feel:
                            </span>
                            <span className={`text-[11px] truncate ${report['shoulder feel'] ? 'text-slate-700' : 'text-slate-400 italic'}`}>
                              {report['shoulder feel'] || 'No data'}
                            </span>
                          </div>
                          <div className="flex items-baseline gap-1.5">
                            <span className="text-[10px] font-bold font-mono text-slate-400 uppercase shrink-0">
                              mid breast:
                            </span>
                            <span className={`text-[11px] truncate ${report['mid breast'] ? 'text-slate-700' : 'text-slate-400 italic'}`}>
                              {report['mid breast'] || 'No data'}
                            </span>
                          </div>
                          <div className="flex items-baseline gap-1.5">
                            <span className="text-[10px] font-bold font-mono text-slate-400 uppercase shrink-0">
                              sweat:
                            </span>
                            <span className={`text-[11px] truncate ${report['sweat'] ? 'text-slate-700' : 'text-slate-400 italic'}`}>
                              {report['sweat'] || 'No data'}
                            </span>
                          </div>
                          <div className="flex items-baseline gap-1.5">
                            <span className="text-[10px] font-bold font-mono text-slate-400 uppercase shrink-0">
                              leg feel:
                            </span>
                            <span className={`text-[11px] truncate ${report['leg feel'] ? 'text-slate-700' : 'text-slate-400 italic'}`}>
                              {report['leg feel'] || 'No data'}
                            </span>
                          </div>
                        </div>

                        {/* Tap to view full detail callout */}
                        <div className="flex items-center justify-between pt-1 text-[11px] text-purple-600 font-semibold group-hover:text-purple-700">
                          <span className="flex items-center gap-1">
                            <Eye className="w-3.5 h-3.5" />
                            Tap to view full detail & edit this session
                          </span>
                          <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
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
