import React, { useState, useMemo, useEffect } from 'react';
import { ClothesItem, WearingReport, ReportClothesJunction, ReportPreset, DrillStep } from '../../types';
import {
  reportPresetService,
  deduplicatePresets,
  AVAILABLE_REPORT_FIELDS,
  DEFAULT_REPORT_PRESETS
} from '../../services/reportPresetService';
import { DrillRecordCard } from './DrillRecordCard';
import { DrillRecordRow } from './DrillRecordRow';
import { PresetEditorModal } from './PresetEditorModal';
import {
  FolderTree,
  ChevronRight,
  SlidersHorizontal,
  ArrowUp,
  ArrowDown,
  LayoutGrid,
  List,
  Search,
  Shirt,
  ChevronDown,
  Layers,
  X
} from 'lucide-react';

interface DrillDownReportProps {
  clothes: ClothesItem[];
  reports: WearingReport[];
  junctions: ReportClothesJunction[];
  onSelectClothes?: (clothes: ClothesItem) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
  onSelectReport?: (report: WearingReport) => void;
}

export const DrillDownReport: React.FC<DrillDownReportProps> = ({
  clothes,
  reports,
  junctions,
  onSelectClothes,
  onSelectGarment,
  onSelectReport
}) => {
  const handleSelectClothesItem = (item: ClothesItem) => {
    if (onSelectClothes) onSelectClothes(item);
    else if (onSelectGarment) onSelectGarment(item);
  };

  // Presets state (guaranteed deduplicated by ID and normalized name)
  const [presets, setPresets] = useState<ReportPreset[]>(() => deduplicatePresets(reportPresetService.getPresets()));
  const [activePresetId, setActivePresetId] = useState<string>(() => {
    try {
      const saved = localStorage.getItem('wardrobe_web_active_preset_id') || sessionStorage.getItem('wardrobe_web_active_preset_id');
      if (saved) return saved;
    } catch {}
    return reportPresetService.getActivePresetId();
  });
  const [isEditorOpen, setIsEditorOpen] = useState(false);

  useEffect(() => {
    try {
      localStorage.setItem('wardrobe_web_active_preset_id', activePresetId);
      sessionStorage.setItem('wardrobe_web_active_preset_id', activePresetId);
    } catch {}
  }, [activePresetId]);

  // Subscribe to cloud updates for presets
  useEffect(() => {
    const unsub = reportPresetService.subscribe((updatedPresets) => {
      setPresets(deduplicatePresets(updatedPresets));
    });
    return unsub;
  }, []);

  // Active preset object
  const activePreset = useMemo(() => {
    return presets.find((p) => p.id === activePresetId) || presets[0] || DEFAULT_REPORT_PRESETS[0];
  }, [presets, activePresetId]);

  // Exclusive Drill Path state: list of steps user has drilled into (persisted across rotation / window resizing)
  const [drillPath, setDrillPath] = useState<DrillStep[]>(() => {
    try {
      const saved = localStorage.getItem('wardrobe_web_drill_path') || sessionStorage.getItem('wardrobe_web_drill_path');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      const str = JSON.stringify(drillPath);
      localStorage.setItem('wardrobe_web_drill_path', str);
      sessionStorage.setItem('wardrobe_web_drill_path', str);
    } catch {}
  }, [drillPath]);

  // Search filter query within current branch
  const [searchQuery, setSearchQuery] = useState('');

  // View mode: default to 'table' (list view) as requested by user
  const [viewMode, setViewMode] = useState<'cards' | 'table'>('table');

  // When active preset changes, check if current drillPath is compatible with new groupTiers
  const groupTiersKey = activePreset.groupTiers.join('|');
  const prevPresetIdRef = React.useRef(activePreset.id);
  const prevTiersKeyRef = React.useRef(groupTiersKey);

  useEffect(() => {
    // Only evaluate reset if the preset or tiers were actually changed after mount
    if (prevPresetIdRef.current === activePreset.id && prevTiersKeyRef.current === groupTiersKey) {
      return;
    }
    prevPresetIdRef.current = activePreset.id;
    prevTiersKeyRef.current = groupTiersKey;

    if (drillPath.length === 0) return;

    const isCompatible = drillPath.every((step, idx) => {
      return activePreset.groupTiers[idx] === step.field;
    });
    if (!isCompatible) {
      setDrillPath([]);
    }
  }, [activePreset.id, groupTiersKey]);

  // Calculate garments that match the current active drill path
  const currentBranchClothes = useMemo(() => {
    return clothes.filter((item) => {
      for (const step of drillPath) {
        const itemVal = reportPresetService.getFieldValue(item, step.field);
        if (itemVal !== step.value) return false;
      }
      return true;
    });
  }, [clothes, drillPath]);

  // Filter and sort matching garments according to active preset and search query
  const sortedAndFilteredRecords = useMemo(() => {
    let result = [...currentBranchClothes];

    // Search query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter((item) => {
        const name = (item['Name'] || item.name || '').toLowerCase();
        const material = (item['material'] || item.materialProp || '').toLowerCase();
        const type = (item['Clothes Type'] || item.clothesType || '').toLowerCase();
        const desc = (item['Descriptions'] || '').toLowerCase();
        return name.includes(q) || material.includes(q) || type.includes(q) || desc.includes(q);
      });
    }

    // Sort according to activePreset sortField and sortOrder
    result.sort((a, b) => {
      const field = activePreset.sortField;
      const numA = reportPresetService.getNumericalSortValue(a, field);
      const numB = reportPresetService.getNumericalSortValue(b, field);

      if (numA !== 0 || numB !== 0) {
        return activePreset.sortOrder === 'asc' ? numA - numB : numB - numA;
      }

      const strA = reportPresetService.getFieldValue(a, field).toLowerCase();
      const strB = reportPresetService.getFieldValue(b, field).toLowerCase();

      return activePreset.sortOrder === 'asc'
        ? strA.localeCompare(strB)
        : strB.localeCompare(strA);
    });

    return result;
  }, [currentBranchClothes, searchQuery, activePreset.sortField, activePreset.sortOrder]);

  // Current tier index in the hierarchy
  const currentTierIndex = drillPath.length;
  const isLeafTier = currentTierIndex >= activePreset.groupTiers.length;
  const nextGroupField = !isLeafTier ? activePreset.groupTiers[currentTierIndex] : null;

  // Next tier subgroups calculation
  const nextTierGroups = useMemo(() => {
    if (!nextGroupField) return [];

    const groupMap = new Map<string, { field: string; value: string; count: number }>();

    currentBranchClothes.forEach((item) => {
      const val = reportPresetService.getFieldValue(item, nextGroupField) || 'None';
      if (!groupMap.has(val)) {
        groupMap.set(val, { field: nextGroupField, value: val, count: 0 });
      }
      groupMap.get(val)!.count += 1;
    });

    const groups = Array.from(groupMap.values());
    // Sort by count descending
    groups.sort((a, b) => b.count - a.count);
    return groups;
  }, [currentBranchClothes, nextGroupField]);

  // Navigation handlers
  const handleDrillIntoGroup = (field: string, value: string) => {
    setDrillPath([...drillPath, { field, value }]);
    setSearchQuery('');
  };

  const handleJumpToBreadcrumb = (index: number) => {
    if (index === -1) {
      setDrillPath([]);
    } else {
      setDrillPath(drillPath.slice(0, index + 1));
    }
    setSearchQuery('');
  };

  // Preset switch handler
  const handleSelectPreset = (id: string) => {
    setActivePresetId(id);
    reportPresetService.setActivePresetId(id);
  };

  // Quick toggle sort direction
  const handleToggleSortOrder = () => {
    const updated: ReportPreset = {
      ...activePreset,
      sortOrder: activePreset.sortOrder === 'asc' ? 'desc' : 'asc'
    };
    const updatedPresets = reportPresetService.savePreset(updated);
    setPresets(updatedPresets);
  };

  // Preset save handler
  const handleSavePreset = (savedPreset: ReportPreset) => {
    const updatedPresets = reportPresetService.savePreset(savedPreset);
    setPresets(updatedPresets);
    setActivePresetId(savedPreset.id);
    reportPresetService.setActivePresetId(savedPreset.id);
  };

  // Preset delete handler
  const handleDeletePreset = (id: string) => {
    const updatedPresets = reportPresetService.deletePreset(id);
    setPresets(updatedPresets);
    setActivePresetId(updatedPresets[0].id);
    reportPresetService.setActivePresetId(updatedPresets[0].id);
  };

  // Reset defaults handler
  const handleResetDefaults = () => {
    const defaults = reportPresetService.resetToDefaults();
    setPresets(defaults);
    setActivePresetId(defaults[0].id);
    reportPresetService.setActivePresetId(defaults[0].id);
  };

  // Field meta map
  const fieldMetaMap = useMemo(
    () => new Map(AVAILABLE_REPORT_FIELDS.map((f) => [f.key, f])),
    []
  );

  const activeSortFieldMeta = fieldMetaMap.get(activePreset.sortField);

  return (
    <div id="drill-down-report-view" className="space-y-4 sm:space-y-5 w-full max-w-full">
      {/* Top Header & Preset Control Strip */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-xs space-y-4">
        {/* Title + Preset Selector Row */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shadow-xs shrink-0">
                <FolderTree className="w-5 h-5 text-white" />
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 font-mono">
                  Analytics &amp; Categorization
                </span>
                <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                  Drill-Down List Report
                </h2>
              </div>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Hierarchical single-branch drill navigation. Selecting a subgroup option immediately filters and lists branch records.
            </p>
          </div>

          {/* Preset Buttons & Customizer Button */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 w-full sm:w-auto">
            {/* Presets Dropdown */}
            <div className="relative flex-1 sm:flex-initial">
              <select
                value={activePreset.id}
                onChange={(e) => handleSelectPreset(e.target.value)}
                className="w-full sm:w-auto appearance-none bg-slate-100 hover:bg-slate-200 text-slate-900 text-xs sm:text-sm font-bold pl-3.5 pr-8 py-2.5 sm:py-2 rounded-2xl border border-slate-200 focus:outline-none focus:border-emerald-600 cursor-pointer transition-colors shadow-2xs"
              >
                {presets.some((p) => p.isCustom) && (
                  <optgroup label="★ My Custom Presets">
                    {presets
                      .filter((p) => p.isCustom)
                      .map((p) => (
                        <option key={p.id} value={p.id}>
                          ★ {p.name}
                        </option>
                      ))}
                  </optgroup>
                )}
                <optgroup label="Climate & Temperature Presets">
                  {presets
                    .filter((p) => !p.isCustom && (p.id.includes('aircond') || p.id.includes('fan') || p.id.includes('climate') || p.id.includes('heat')))
                    .map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                </optgroup>
                <optgroup label="Breast Fit, Bra & Moisture Presets">
                  {presets
                    .filter((p) => !p.isCustom && (p.id.includes('breast') || p.id.includes('wet') || p.id.includes('aesthetic')))
                    .map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                </optgroup>
                <optgroup label="Care, Laundry & Audit Presets">
                  {presets
                    .filter((p) => !p.isCustom && !p.id.includes('aircond') && !p.id.includes('fan') && !p.id.includes('climate') && !p.id.includes('heat') && !p.id.includes('breast') && !p.id.includes('wet') && !p.id.includes('aesthetic'))
                    .map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                </optgroup>
              </select>
              <ChevronDown className="w-3.5 h-3.5 text-slate-500 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>

            {/* Customize Preset Button */}
            <button
              onClick={() => setIsEditorOpen(true)}
              className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-2xl shadow-xs transition-all cursor-pointer active:scale-95"
              title="Customize grouping tiers, sorting rule, and card fields"
            >
              <SlidersHorizontal className="w-3.5 h-3.5 text-white" />
              <span>Customize Preset</span>
            </button>
          </div>
        </div>

        {/* Current Active Preset Spec Summary Bar */}
        <div className="flex flex-col gap-2.5 pt-3 border-t border-slate-100 text-xs">
          {/* Hierarchy Tiers Indicator: wraps dynamically across rows with little space between them */}
          <div className="flex flex-wrap items-center gap-1.5 text-slate-600">
            <span className="font-semibold text-slate-400 uppercase tracking-wider text-[10px] font-mono">
              Tiers:
            </span>
            {activePreset.groupTiers.map((tierKey, idx) => {
              const meta = fieldMetaMap.get(tierKey);
              const isActiveInDrill = idx < drillPath.length;
              const isCurrentTier = idx === drillPath.length;
              return (
                <div key={tierKey} className="flex items-center gap-1">
                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg border text-[11px] ${
                      isActiveInDrill
                        ? 'bg-emerald-100 text-emerald-900 border-emerald-300 font-bold'
                        : isCurrentTier
                        ? 'bg-emerald-50 text-emerald-800 border-emerald-200 font-semibold'
                        : 'bg-slate-100 text-slate-500 border-slate-200'
                    }`}
                  >
                    <span className="text-[9px] opacity-70">L{idx + 1}:</span>
                    <span>{meta?.label || tierKey}</span>
                  </span>
                  {idx < activePreset.groupTiers.length - 1 && (
                    <ChevronRight className="w-3.5 h-3.5 text-slate-300 shrink-0" />
                  )}
                </div>
              );
            })}
          </div>

          {/* Quick Sort & View Mode Controls */}
          <div className="flex items-center gap-2">
            {/* Quick Sort Direction Button */}
            <button
              onClick={handleToggleSortOrder}
              className="flex items-center gap-1 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 rounded-xl border border-slate-200 text-slate-800 text-[11px] font-semibold transition-colors shadow-2xs"
              title={`Sorted by ${activeSortFieldMeta?.label || activePreset.sortField} (${
                activePreset.sortOrder === 'asc' ? 'Ascending' : 'Descending'
              }). Click to reverse.`}
            >
              {activePreset.sortOrder === 'asc' ? (
                <ArrowUp className="w-3 h-3 text-emerald-600" />
              ) : (
                <ArrowDown className="w-3 h-3 text-emerald-600" />
              )}
              <span className="hidden sm:inline">{activeSortFieldMeta?.label || activePreset.sortField}</span>
              <span className="uppercase text-[9px] text-slate-400 font-mono">({activePreset.sortOrder})</span>
            </button>

            {/* View Mode Toggle */}
            <div className="flex items-center p-1 rounded-2xl bg-slate-100 border border-slate-200">
              <button
                onClick={() => setViewMode('table')}
                className={`p-1.5 rounded-xl transition-all ${
                  viewMode === 'table'
                    ? 'bg-white text-emerald-950 font-bold shadow-xs'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
                title="Dense List / Table View"
              >
                <List className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => setViewMode('cards')}
                className={`p-1.5 rounded-xl transition-all ${
                  viewMode === 'cards'
                    ? 'bg-white text-emerald-950 font-bold shadow-xs'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
                title="Grid Cards View"
              >
                <LayoutGrid className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Row 1: Breadcrumb Navigation Trail */}
      <div className="bg-white rounded-2xl p-3 sm:p-4 border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-wrap items-center gap-2 text-xs text-slate-700">
          <button
            type="button"
            onClick={() => handleJumpToBreadcrumb(-1)}
            className={`flex items-center gap-1 px-3.5 py-1.5 rounded-xl border transition-all text-xs font-bold cursor-pointer ${
              drillPath.length === 0
                ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border-slate-200'
            }`}
          >
            <span>all clothes ({clothes.length})</span>
          </button>

          {drillPath.map((step, idx) => {
            const isCurrent = idx === drillPath.length - 1;
            const meta = fieldMetaMap.get(step.field);
            return (
              <React.Fragment key={`${step.field}-${step.value}-${idx}`}>
                <ChevronRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                <button
                  type="button"
                  onClick={() => handleJumpToBreadcrumb(idx)}
                  className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl border transition-all text-xs cursor-pointer ${
                    isCurrent
                      ? 'bg-emerald-600 text-white border-emerald-600 font-bold shadow-xs'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border-slate-200 font-semibold'
                  }`}
                >
                  <span className={isCurrent ? 'font-normal opacity-90' : 'text-slate-600 font-normal'}>
                    {meta?.label || step.field}:
                  </span>
                  <span className="font-bold">{step.value}</span>
                </button>
              </React.Fragment>
            );
          })}

          {drillPath.length > 0 && (
            <button
              type="button"
              onClick={() => handleJumpToBreadcrumb(-1)}
              className="ml-auto inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold border border-slate-200 transition-colors cursor-pointer"
              title="Reset drill path back to root"
            >
              <X className="w-3.5 h-3.5" />
              <span>Reset</span>
            </button>
          )}
        </div>

        {/* Row 2: Subgroup list at dedicated row above the record list */}
        <div className="pt-2.5 border-t border-slate-100 flex flex-col gap-2 text-xs">
          <div className="flex flex-wrap items-center justify-between gap-1.5">
            <div className="flex items-center gap-1.5 text-slate-800 font-bold">
              <Layers className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>
                {nextGroupField
                  ? `${fieldMetaMap.get(nextGroupField)?.label || nextGroupField}:`
                  : 'Subgroup:'}
              </span>
              {nextGroupField && fieldMetaMap.get(nextGroupField)?.description && (
                <span className="text-[11px] text-slate-500 font-normal hidden sm:inline">
                  — {fieldMetaMap.get(nextGroupField)?.description}
                </span>
              )}
            </div>
            {nextGroupField && (
              <span className="text-[10px] text-slate-400 font-mono">
                Tier {currentTierIndex + 1} of {activePreset.groupTiers.length}
              </span>
            )}
          </div>
          {nextGroupField && fieldMetaMap.get(nextGroupField)?.description && (
            <p className="text-[11px] text-slate-500 font-normal sm:hidden -mt-1">
              {fieldMetaMap.get(nextGroupField)?.description}
            </p>
          )}

          {!isLeafTier ? (
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 flex-wrap py-0.5 w-full">
              {nextTierGroups.map((group) => {
                const isUndefined = group.value === 'undefined';
                return (
                  <button
                    key={`${group.field}-${group.value}`}
                    type="button"
                    onClick={() => handleDrillIntoGroup(group.field, group.value)}
                    className={`w-full sm:w-auto flex items-center justify-between gap-3 px-3.5 py-2.5 rounded-2xl border text-xs text-left font-semibold transition-all active:scale-[0.99] shadow-2xs ${
                      isUndefined
                        ? 'border-slate-300 bg-slate-100 hover:bg-slate-200 text-slate-700'
                        : 'border-emerald-200 bg-emerald-50/80 hover:bg-emerald-100 text-emerald-950'
                    }`}
                  >
                    <span
                      className={`text-left flex-1 min-w-0 leading-relaxed break-words [word-break:normal] [overflow-wrap:break-word] ${
                        isUndefined ? 'font-mono italic text-slate-600' : ''
                      }`}
                    >
                      {group.value}
                    </span>
                    <span
                      className={`shrink-0 ml-auto text-[11px] font-mono font-bold px-2 py-0.5 rounded-lg ${
                        isUndefined
                          ? 'text-slate-600 bg-slate-200/90'
                          : 'text-emerald-800 bg-emerald-100 border border-emerald-200/90'
                      }`}
                    >
                      {group.count}
                    </span>
                  </button>
                );
              })}
            </div>
          ) : (
            <span className="text-slate-400 text-[11px] italic">
              Leaf level reached — all {activePreset.groupTiers.length} grouping tiers applied.
            </span>
          )}
        </div>
      </div>

      {/* Row 3: Auto list all branch records (default in list view) */}
      <div className="space-y-3">
        {/* Search & Counter Strip */}
        <div className="bg-white rounded-2xl p-3 sm:p-4 border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-slate-800">
              Branch Records ({sortedAndFilteredRecords.length})
            </span>
            <span className="text-xs text-slate-400">·</span>
            <span className="text-xs text-slate-500">
              Sorted by {activeSortFieldMeta?.label || activePreset.sortField} ({activePreset.sortOrder})
            </span>
          </div>

          {/* Search within active branch */}
          <div className="relative max-w-xs w-full">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Filter records in this branch..."
              className="w-full pl-8 pr-3 py-1.5 text-xs bg-transparent border border-slate-300 rounded-xl focus:outline-none focus:border-slate-600 font-medium text-slate-800"
            />
          </div>
        </div>

        {/* Record Display: Table (default) or Cards */}
        {sortedAndFilteredRecords.length === 0 ? (
          <div className="bg-white rounded-2xl p-10 border border-slate-200 text-center space-y-2">
            <Shirt className="w-10 h-10 text-slate-300 mx-auto stroke-[1.2]" />
            <h3 className="font-bold text-slate-700 text-sm">No clothes found</h3>
            <p className="text-xs text-slate-400">
              {searchQuery
                ? `No items match "${searchQuery}" in this branch.`
                : 'No clothes exist in this exact branch.'}
            </p>
          </div>
        ) : viewMode === 'table' ? (
          <div className="bg-white rounded-2xl border border-slate-300 shadow-xs overflow-hidden">
            <div className="overflow-x-auto w-full scrollbar-thin">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-slate-50/90 border-b border-slate-200 text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                    <th className="py-2.5 px-3 min-w-[280px] sm:min-w-[340px]">Clothes (Full Name)</th>
                    <th className="py-2.5 px-3 whitespace-nowrap">Wash Status</th>
                    {activePreset.visibleFields
                      .filter((f) => f !== 'Name' && f !== 'wash stated')
                      .slice(0, 5)
                      .map((fieldKey) => (
                        <th key={fieldKey} className="py-2.5 px-3">
                          {fieldMetaMap.get(fieldKey)?.label || fieldKey}
                        </th>
                      ))}
                    <th className="py-2.5 px-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedAndFilteredRecords.map((item) => (
                    <DrillRecordRow
                      key={item.id}
                      garment={item}
                      preset={activePreset}
                      onSelect={handleSelectClothesItem}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
            {sortedAndFilteredRecords.map((item) => (
              <DrillRecordCard
                key={item.id}
                garment={item}
                preset={activePreset}
                onSelect={handleSelectClothesItem}
              />
            ))}
          </div>
        )}
      </div>

      {/* Preset Customizer Modal */}
      {isEditorOpen && (
        <PresetEditorModal
          isOpen={isEditorOpen}
          onClose={() => setIsEditorOpen(false)}
          preset={activePreset}
          onSavePreset={handleSavePreset}
          onDeletePreset={handleDeletePreset}
          onResetDefaults={handleResetDefaults}
        />
      )}
    </div>
  );
};
