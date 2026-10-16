import React, { useState, useMemo, useEffect } from 'react';
import { ClothesItem, WearingReport, ReportClothesJunction, ReportPreset, DrillStep } from '../../types';
import {
  reportPresetService,
  AVAILABLE_REPORT_FIELDS,
  DEFAULT_REPORT_PRESETS
} from '../../services/reportPresetService';
import { resolveImageSource } from '../../utils/imageUtils';
import { DrillRecordCard } from './DrillRecordCard';
import { DrillRecordRow } from './DrillRecordRow';
import { PresetEditorModal } from './PresetEditorModal';
import {
  FolderTree,
  ChevronRight,
  ArrowLeft,
  SlidersHorizontal,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  LayoutGrid,
  List,
  Search,
  Layers,
  Sparkles,
  Shirt,
  Info,
  RotateCcw,
  Check,
  ChevronDown
} from 'lucide-react';

interface DrillDownReportProps {
  clothes: ClothesItem[];
  reports: WearingReport[];
  junctions: ReportClothesJunction[];
  onSelectGarment: (garment: ClothesItem) => void;
  onSelectReport?: (report: WearingReport) => void;
}

export const DrillDownReport: React.FC<DrillDownReportProps> = ({
  clothes,
  reports,
  junctions,
  onSelectGarment,
  onSelectReport
}) => {
  // Presets state
  const [presets, setPresets] = useState<ReportPreset[]>(() => reportPresetService.getPresets());
  const [activePresetId, setActivePresetId] = useState<string>(() => reportPresetService.getActivePresetId());
  const [isEditorOpen, setIsEditorOpen] = useState(false);

  // Active preset object
  const activePreset = useMemo(() => {
    return presets.find((p) => p.id === activePresetId) || presets[0] || DEFAULT_REPORT_PRESETS[0];
  }, [presets, activePresetId]);

  // Exclusive Drill Path state: list of steps user has drilled into
  // When drillPath is updated, sibling branches collapse because we only render children of the active branch!
  const [drillPath, setDrillPath] = useState<DrillStep[]>([]);

  // Search filter query within current branch
  const [searchQuery, setSearchQuery] = useState('');

  // View mode: cards or dense table
  const [viewMode, setViewMode] = useState<'cards' | 'table'>('cards');

  // Intermediate view choice: when at non-leaf level, user can view subgroups or view all branch records
  const [showDirectRecords, setShowDirectRecords] = useState(false);

  // When active preset changes, check if current drillPath is compatible with new groupTiers
  useEffect(() => {
    // If drillPath doesn't match new groupTiers, reset to root
    const isCompatible = drillPath.every((step, idx) => {
      return activePreset.groupTiers[idx] === step.field;
    });
    if (!isCompatible) {
      setDrillPath([]);
      setShowDirectRecords(false);
    }
  }, [activePreset.id, activePreset.groupTiers]);

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

  // Next tier subgroups calculation (only for the active single branch, collapsing all others)
  const nextTierGroups = useMemo(() => {
    if (!nextGroupField) return [];

    const groupMap = new Map<string, { value: string; items: ClothesItem[] }>();

    currentBranchClothes.forEach((item) => {
      const val = reportPresetService.getFieldValue(item, nextGroupField);
      if (!groupMap.has(val)) {
        groupMap.set(val, { value: val, items: [] });
      }
      groupMap.get(val)!.items.push(item);
    });

    // Convert map to array and sort groups
    const groups = Array.from(groupMap.values()).map((g) => ({
      field: nextGroupField,
      value: g.value,
      count: g.items.length,
      percentage: currentBranchClothes.length > 0 ? Math.round((g.items.length / currentBranchClothes.length) * 100) : 0,
      previewItems: g.items.slice(0, 4)
    }));

    // Sort by count descending
    groups.sort((a, b) => b.count - a.count);
    return groups;
  }, [currentBranchClothes, nextGroupField]);

  // Navigation handlers
  const handleDrillIntoGroup = (field: string, value: string) => {
    // Sibling branches collapse immediately: we append this step to the path
    setDrillPath([...drillPath, { field, value }]);
    setShowDirectRecords(false);
    setSearchQuery('');
  };

  const handleStepBack = () => {
    if (drillPath.length > 0) {
      setDrillPath(drillPath.slice(0, -1));
      setShowDirectRecords(false);
      setSearchQuery('');
    }
  };

  const handleJumpToBreadcrumb = (index: number) => {
    if (index === -1) {
      setDrillPath([]);
    } else {
      setDrillPath(drillPath.slice(0, index + 1));
    }
    setShowDirectRecords(false);
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
    <div className="space-y-4 sm:space-y-6">
      {/* Top Header & Preset Control Strip */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200/80 shadow-xs space-y-4">
        {/* Title + Preset Selector Row */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shadow-xs">
                <FolderTree className="w-4 h-4" />
              </div>
              <h2 className="text-lg sm:text-xl font-bold text-slate-900 tracking-tight">
                Drill-Down List Report
              </h2>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Hierarchical single-branch drill navigation. Selecting a category isolates that branch and collapses siblings.
            </p>
          </div>

          {/* Preset Buttons & Customizer Button */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Presets Dropdown */}
            <div className="relative inline-block">
              <select
                value={activePreset.id}
                onChange={(e) => handleSelectPreset(e.target.value)}
                className="appearance-none bg-slate-50 hover:bg-slate-100 text-slate-800 text-xs sm:text-sm font-semibold pl-3.5 pr-8 py-2 rounded-2xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-emerald-500 cursor-pointer transition-colors"
              >
                {presets.map((p) => (
                  <option key={p.id} value={p.id}>
                    Preset: {p.name} {p.isCustom ? '★' : ''}
                  </option>
                ))}
              </select>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>

            {/* Customize Preset Button */}
            <button
              onClick={() => setIsEditorOpen(true)}
              className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-slate-700 hover:text-indigo-700 bg-slate-50 hover:bg-indigo-50/60 rounded-2xl border border-slate-200 hover:border-indigo-200 shadow-2xs transition-all cursor-pointer"
              title="Customize grouping tiers, sorting rule, and card fields"
            >
              <SlidersHorizontal className="w-3.5 h-3.5 text-indigo-600" />
              <span>Customize Preset</span>
            </button>
          </div>
        </div>

        {/* Current Active Preset Spec Summary Bar */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 pt-3 border-t border-slate-100 text-xs">
          {/* Hierarchy Tiers Indicator */}
          <div className="flex flex-wrap items-center gap-1.5 text-slate-600">
            <span className="font-semibold text-slate-400 uppercase tracking-wider text-[10px]">
              Tiers:
            </span>
            {activePreset.groupTiers.map((tierKey, idx) => {
              const meta = fieldMetaMap.get(tierKey);
              const isActiveInDrill = idx < drillPath.length;
              const isCurrentTier = idx === drillPath.length;
              return (
                <div key={tierKey} className="flex items-center gap-1">
                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-xl font-medium border text-[11px] ${
                      isActiveInDrill
                        ? 'bg-emerald-50 border-emerald-300 text-emerald-800 font-semibold'
                        : isCurrentTier
                        ? 'bg-indigo-50 border-indigo-300 text-indigo-800 font-bold shadow-2xs ring-2 ring-indigo-200/50'
                        : 'bg-slate-50 border-slate-200/80 text-slate-600'
                    }`}
                  >
                    <span className="text-[9px] opacity-70">L{idx + 1}:</span>
                    <span>{meta?.label || tierKey}</span>
                  </span>
                  {idx < activePreset.groupTiers.length - 1 && (
                    <ChevronRight className="w-3 h-3 text-slate-300 shrink-0" />
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
              className="flex items-center gap-1 px-2.5 py-1 bg-slate-50 hover:bg-slate-100 rounded-xl border border-slate-200 text-slate-700 text-[11px] font-semibold transition-colors"
              title={`Sorted by ${activeSortFieldMeta?.label || activePreset.sortField} (${
                activePreset.sortOrder === 'asc' ? 'Ascending' : 'Descending'
              }). Click to reverse.`}
            >
              {activePreset.sortOrder === 'asc' ? (
                <ArrowUp className="w-3 h-3 text-indigo-600" />
              ) : (
                <ArrowDown className="w-3 h-3 text-indigo-600" />
              )}
              <span className="hidden sm:inline">{activeSortFieldMeta?.label || activePreset.sortField}</span>
              <span className="uppercase text-[9px] text-slate-400 font-mono">({activePreset.sortOrder})</span>
            </button>

            {/* View Mode Toggle */}
            <div className="flex items-center p-0.5 bg-slate-100 rounded-xl border border-slate-200">
              <button
                onClick={() => setViewMode('cards')}
                className={`p-1 rounded-lg transition-colors ${
                  viewMode === 'cards'
                    ? 'bg-white text-slate-900 shadow-2xs font-bold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
                title="Grid Cards View"
              >
                <LayoutGrid className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => setViewMode('table')}
                className={`p-1 rounded-lg transition-colors ${
                  viewMode === 'table'
                    ? 'bg-white text-slate-900 shadow-2xs font-bold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
                title="Dense Table View"
              >
                <List className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Breadcrumb Navigation & Back Header (Single-Branch Focus) */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200/80 shadow-xs space-y-3">
        {/* Navigation Step Trail */}
        <div className="flex flex-wrap items-center gap-1.5 text-xs text-slate-600">
          <button
            onClick={() => handleJumpToBreadcrumb(-1)}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-xl transition-colors font-semibold ${
              drillPath.length === 0
                ? 'bg-slate-900 text-white shadow-xs'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            <span>All Clothes ({clothes.length})</span>
          </button>

          {drillPath.map((step, idx) => {
            const isCurrent = idx === drillPath.length - 1;
            const meta = fieldMetaMap.get(step.field);
            return (
              <React.Fragment key={`${step.field}-${step.value}-${idx}`}>
                <ChevronRight className="w-3.5 h-3.5 text-slate-300 shrink-0" />
                <button
                  onClick={() => handleJumpToBreadcrumb(idx)}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-xl transition-colors ${
                    isCurrent
                      ? 'bg-indigo-600 text-white font-bold shadow-xs'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200 font-semibold'
                  }`}
                >
                  <span className="opacity-80 text-[10px]">{meta?.label || step.field}:</span>
                  <span>{step.value}</span>
                </button>
              </React.Fragment>
            );
          })}
        </div>

        {/* Exclusive Drill Back Header Banner */}
        {drillPath.length > 0 && (
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2 border-t border-slate-100">
            <div className="flex items-center gap-2.5">
              <button
                onClick={handleStepBack}
                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-2xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition-all shadow-2xs hover:shadow-xs active:scale-95"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>
                  Back to {drillPath.length === 1 ? 'All Categories' : drillPath[drillPath.length - 2].value}
                </span>
              </button>

              <div className="text-xs text-slate-500 font-medium">
                Active branch:{' '}
                <span className="font-bold text-slate-900">
                  {drillPath[drillPath.length - 1].value}
                </span>{' '}
                ({currentBranchClothes.length} garments)
              </div>
            </div>

            {/* Direct Records Toggle (Available at intermediate branch levels) */}
            {!isLeafTier && (
              <div className="flex items-center gap-1 text-xs">
                <button
                  onClick={() => setShowDirectRecords(false)}
                  className={`px-3 py-1 rounded-xl font-semibold transition-colors ${
                    !showDirectRecords
                      ? 'bg-indigo-50 text-indigo-700 border border-indigo-200 font-bold'
                      : 'text-slate-500 hover:text-slate-800'
                  }`}
                >
                  Subgroups ({nextTierGroups.length})
                </button>
                <button
                  onClick={() => setShowDirectRecords(true)}
                  className={`px-3 py-1 rounded-xl font-semibold transition-colors ${
                    showDirectRecords
                      ? 'bg-indigo-50 text-indigo-700 border border-indigo-200 font-bold'
                      : 'text-slate-500 hover:text-slate-800'
                  }`}
                >
                  All Branch Records ({currentBranchClothes.length})
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Main Content: Either Subgroup Branches (with sibling collapse) OR Record List */}
      {!isLeafTier && !showDirectRecords ? (
        /* Subgroup Branches Grid */
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-500 px-1">
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-slate-800 text-sm">
                Next Tier: {fieldMetaMap.get(nextGroupField!)?.label || nextGroupField}
              </span>
              <span>·</span>
              <span>{nextTierGroups.length} branches</span>
            </div>
            <span className="text-[11px] text-slate-400">
              Tap any branch to drill in (other branches will collapse)
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
            {nextTierGroups.map((group) => {
              return (
                <div
                  key={`${group.field}-${group.value}`}
                  onClick={() => handleDrillIntoGroup(group.field, group.value)}
                  className="group relative bg-white hover:bg-indigo-50/20 rounded-3xl p-4 sm:p-5 border border-slate-200/80 hover:border-indigo-300 hover:shadow-md transition-all duration-200 cursor-pointer flex flex-col justify-between active:scale-[0.99]"
                >
                  {/* Branch Group Top */}
                  <div>
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="min-w-0">
                        <span className="text-[10px] uppercase font-bold tracking-wider text-slate-400 block mb-0.5">
                          {fieldMetaMap.get(group.field)?.label || group.field}
                        </span>
                        <h3 className="font-bold text-slate-900 text-base sm:text-lg group-hover:text-indigo-600 transition-colors truncate">
                          {group.value}
                        </h3>
                      </div>

                      <span className="px-2.5 py-1 rounded-xl text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200/60 shrink-0">
                        {group.count} {group.count === 1 ? 'item' : 'items'}
                      </span>
                    </div>

                    {/* Percentage Progress Bar */}
                    <div className="mt-2.5">
                      <div className="flex items-center justify-between text-[10px] text-slate-400 font-mono mb-1">
                        <span>Share of branch</span>
                        <span>{group.percentage}%</span>
                      </div>
                      <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className="h-full bg-indigo-500 rounded-full transition-all duration-500"
                          style={{ width: `${Math.max(group.percentage, 4)}%` }}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Branch Previews & Drill Trigger */}
                  <div className="pt-4 mt-3 border-t border-slate-100 flex items-center justify-between">
                    {/* Small preview avatars */}
                    <div className="flex items-center -space-x-2 overflow-hidden">
                      {group.previewItems.map((item) => {
                        const pic = item['Pictures']?.[0];
                        const imgUrl = resolveImageSource(pic);
                        return (
                          <div
                            key={item.id}
                            className="w-7 h-7 rounded-full bg-slate-200 ring-2 ring-white overflow-hidden shrink-0 flex items-center justify-center text-[9px] text-slate-600 font-bold"
                          >
                            {imgUrl ? (
                              <img
                                src={imgUrl}
                                alt={item['Name'] || item.name}
                                referrerPolicy="no-referrer"
                                className="w-full h-full object-cover"
                              />
                            ) : (
                              (item['Name'] || item.name || 'G').charAt(0).toUpperCase()
                            )}
                          </div>
                        );
                      })}
                      {group.count > 4 && (
                        <div className="w-7 h-7 rounded-full bg-slate-100 ring-2 ring-white flex items-center justify-center text-[10px] font-bold text-slate-500">
                          +{group.count - 4}
                        </div>
                      )}
                    </div>

                    {/* Drill-in Arrow */}
                    <span className="flex items-center gap-1 text-xs font-bold text-indigo-600 group-hover:translate-x-1 transition-transform">
                      <span>Drill in</span>
                      <ChevronRight className="w-4 h-4" />
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        /* Leaf Level (or Direct Branch View): The Record List */
        <div className="space-y-4">
          {/* Records Bar with Search & Counter */}
          <div className="bg-white rounded-3xl p-3 sm:p-4 border border-slate-200/80 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-800">
                Matching Garments ({sortedAndFilteredRecords.length})
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
                placeholder="Search branch records..."
                className="w-full pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 font-medium"
              />
            </div>
          </div>

          {/* Record Display: Cards or Table */}
          {sortedAndFilteredRecords.length === 0 ? (
            <div className="bg-white rounded-3xl p-10 border border-slate-200/80 text-center space-y-2">
              <Shirt className="w-10 h-10 text-slate-300 mx-auto stroke-[1.2]" />
              <h3 className="font-bold text-slate-700 text-sm">No garments found</h3>
              <p className="text-xs text-slate-400">
                {searchQuery
                  ? `No items match "${searchQuery}" in this branch.`
                  : 'No garments exist in this exact sub-branch.'}
              </p>
            </div>
          ) : viewMode === 'cards' ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
              {sortedAndFilteredRecords.map((item) => (
                <DrillRecordCard
                  key={item.id}
                  garment={item}
                  preset={activePreset}
                  onSelect={onSelectGarment}
                />
              ))}
            </div>
          ) : (
            <div className="bg-white rounded-3xl border border-slate-200/80 shadow-xs overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-slate-50/80 border-b border-slate-200/80 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                      <th className="py-2.5 px-3">Garment</th>
                      <th className="py-2.5 px-3">Wash Status</th>
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
                        onSelect={onSelectGarment}
                      />
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

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
