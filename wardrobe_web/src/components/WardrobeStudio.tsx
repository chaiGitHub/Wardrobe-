import React, { useState, useMemo } from 'react';
import { ClothesItem, ReportClothesJunction } from '../types';
import { ClothesCard } from './ClothesCard';
import { Search, Filter, Plus, Sparkles, ThermometerSun, AlertCircle, Shirt, RefreshCw, SlidersHorizontal, Settings2, X, CheckSquare, Tag, Trash2, CheckCircle2 } from 'lucide-react';
import { ClothesFilterDrawer, filterClothesWithActiveFilters, ALL_CLOTHES_FILTER_FIELDS } from './ClothesFilterDrawer';

interface WardrobeStudioProps {
  clothes: ClothesItem[];
  junctions: ReportClothesJunction[];
  onSelectClothes?: (clothes: ClothesItem) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
  onOpenAddModal: () => void;
}

export const WardrobeStudio: React.FC<WardrobeStudioProps> = ({
  clothes,
  junctions,
  onSelectClothes,
  onSelectGarment,
  onOpenAddModal
}) => {

  const handleSelectClothes = (item: ClothesItem) => {
    if (onSelectClothes) onSelectClothes(item);
    if (onSelectGarment) onSelectGarment(item);
  };

  const [activeCategory, setActiveCategory] = useState<string>('All');
  const [activePreset, setActivePreset] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [sortBy, setSortBy] = useState<'washes-desc' | 'price-asc' | 'price-desc' | 'name' | 'cpw'>('washes-desc');

  // Clothes Filter Drawer states
  const [isFilterDrawerOpen, setIsFilterDrawerOpen] = useState(false);
  const [filterDrawerTab, setFilterDrawerTab] = useState<'filters' | 'manage'>('filters');
  const [activeFilters, setActiveFilters] = useState<Record<string, string[]>>({});

  // Batch Select & Tag Mode states
  const [isSelectMode, setIsSelectMode] = useState(false);
  const [selectedClothesIds, setSelectedClothesIds] = useState<Set<string>>(new Set());
  const [targetTag, setTargetTag] = useState<string>('ai');
  const [tagFeedback, setTagFeedback] = useState<string | null>(null);
  const [isTagProcessing, setIsTagProcessing] = useState(false);

  const totalActiveFilterCount = useMemo(() => {
    return Object.values(activeFilters).reduce((acc, list) => acc + list.length, 0);
  }, [activeFilters]);

  // Batch Select & Tag Mode handlers
  const handleToggleSelect = (id: string) => {
    setSelectedClothesIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const handleSelectAll = () => {
    const allFilteredIds = filteredClothes.map(c => c.id);
    setSelectedClothesIds(new Set(allFilteredIds));
  };

  const handleDeselectAll = () => {
    setSelectedClothesIds(new Set());
  };

  const handleBatchTagAction = async (action: 'add' | 'remove') => {
    const clean = targetTag.trim();
    if (!clean || selectedClothesIds.size === 0) return;
    setIsTagProcessing(true);
    try {
      const { dataService } = await import('../services/dataService');
      const count = await dataService.batchUpdateGarmentTags(
        Array.from(selectedClothesIds),
        clean,
        action
      );
      if (action === 'add') {
        setTagFeedback(`✅ Added tag "${clean}" to ${count} clothes items (${selectedClothesIds.size - count} already had it).`);
      } else {
        setTagFeedback(`🗑️ Removed tag "${clean}" from ${count} clothes items.`);
      }
      setTimeout(() => setTagFeedback(null), 4000);
    } catch (err) {
      console.error('Failed to update tags:', err);
    } finally {
      setIsTagProcessing(false);
    }
  };

  // Compute stats helper
  const getStats = (clothesId: string) => {
    const itemJunctions = junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId === clothesId;
    });
    const needWashJunctions = itemJunctions.filter(j => j['wash stated'] === 'need wash');
    const needWash = needWashJunctions.length > 0;
    const unwashedSeconds = needWashJunctions.reduce((acc, j) => acc + (j['duration'] || 0), 0);
    const totalWears = itemJunctions.length;
    const totalDurationHours = Math.round(itemJunctions.reduce((acc, j) => acc + (j['duration'] || 0), 0) / 3600);

    return {
      needWash,
      unwashedSeconds,
      unwashedHours: (unwashedSeconds / 3600).toFixed(1),
      totalWears,
      totalDurationHours
    };
  };

  // Preset definitions
  const presets = [
    { id: 'all', label: 'All Items' },
    { id: 'not-hot', label: '☀️ Hot at noon: Not hot', desc: 'Breathable and not hot at midday' },
    { id: 'favorites', label: '🌟 beautiful: Beautiful', desc: 'Rated Beautiful' },
    { id: 'need-wash', label: '🧺 wash stated: need wash', desc: 'Currently unwashed in hamper' },
    { id: 'chiffon', label: '🪶 material: chiffon', desc: 'Lightweight chiffon pieces' },
    { id: 'sit-ok', label: '💺 sit breast ok', desc: 'Comfortable while seated' },
  ];

  // Dynamic Category counts derived 100% in-memory from local clothes (<1ms)
  const categories = useMemo(() => {
    const counts = new Map<string, number>();
    clothes.forEach(c => {
      const rawType = (c['Clothes Type'] || '').trim();
      if (rawType) {
        // Normalize capitalization for display
        const normalized = rawType.charAt(0).toUpperCase() + rawType.slice(1);
        counts.set(normalized, (counts.get(normalized) || 0) + 1);
      }
    });

    // Ensure baseline wardrobe defaults exist if not already present
    const baseDefaults = ['Dress', 'Pant', 'Tshirt', 'Bra'];
    baseDefaults.forEach(def => {
      if (!counts.has(def)) {
        // Only show if count > 0 or keep as 0 if empty
        const matches = clothes.filter(c => (c['Clothes Type'] || '').toLowerCase() === def.toLowerCase()).length;
        if (matches > 0) counts.set(def, matches);
      }
    });

    const dynamicList = Array.from(counts.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([typeStr, count]) => ({
        id: typeStr,
        label: typeStr,
        count
      }));

    return [
      { id: 'All', label: 'All Items', count: clothes.length },
      ...dynamicList
    ];
  }, [clothes]);

  // Dynamic Filter logic combining data-driven facet filters, category tabs, presets, and search
  const filteredClothes = useMemo(() => {
    // 1. Data-Driven Dynamic Filters from ClothesFilterDrawer (follows actual clothes dataset values)
    let list = filterClothesWithActiveFilters(clothes, activeFilters, getStats);

    // 2. Dynamic Category match
    if (activeCategory !== 'All') {
      list = list.filter(c => {
        const cType = (c['Clothes Type'] || '').trim();
        return (
          cType.toLowerCase() === activeCategory.toLowerCase() ||
          (activeCategory.toLowerCase() === 'tshirt' &&
            (cType.toLowerCase().includes('tshirt') || cType.toLowerCase().includes('t-shirt'))) ||
          (activeCategory.toLowerCase() === 'pant' && cType.toLowerCase().includes('pant'))
        );
      });
    }

    // 3. Search query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      list = list.filter(c => {
        const name = c['Name'] || '';
        const mat = c['material'] || '';
        const desc = c['Descriptions'] || '';
        return name.toLowerCase().includes(q) || mat.toLowerCase().includes(q) || desc.toLowerCase().includes(q);
      });
    }

    // 4. Presets
    if (activePreset === 'not-hot') {
      list = list.filter(c => (c['Hot at noon'] || '').toLowerCase().includes('not'));
    } else if (activePreset === 'favorites') {
      list = list.filter(c => (c['beautiful'] || '') === 'Beautiful');
    } else if (activePreset === 'need-wash') {
      list = list.filter(c => getStats(c.id).needWash);
    } else if (activePreset === 'chiffon') {
      list = list.filter(c => (c['material'] || '').toLowerCase().includes('chiffon'));
    } else if (activePreset === 'sit-ok') {
      list = list.filter(c => (c['sit breast ok'] || '').toLowerCase().includes('ok'));
    }

    return list;
  }, [clothes, activeFilters, activeCategory, searchQuery, activePreset, junctions]);

  // Sort logic
  const sortedClothes = useMemo(() => {
    return [...filteredClothes].sort((a, b) => {
      const washesA = a['Total Washed Count'] || 0;
      const washesB = b['Total Washed Count'] || 0;
      const priceA = typeof a['price'] === 'number' ? a['price'] : 9999;
      const priceB = typeof b['price'] === 'number' ? b['price'] : 9999;

      if (sortBy === 'washes-desc') {
        return washesB - washesA;
      }
      if (sortBy === 'price-asc') {
        return priceA - priceB;
      }
      if (sortBy === 'price-desc') {
        return priceB - priceA;
      }
      if (sortBy === 'cpw') {
        const cpwA = typeof a['price'] === 'number' ? a['price'] / Math.max(1, washesA || 1) : 9999;
        const cpwB = typeof b['price'] === 'number' ? b['price'] / Math.max(1, washesB || 1) : 9999;
        return cpwA - cpwB;
      }
      return (a['Name'] || '').localeCompare(b['Name'] || '');
    });
  }, [filteredClothes, sortBy]);

  return (
    <div id="wardrobe-studio-view" className="space-y-5 w-full max-w-full">
      {/* Top Controls & Action Bar */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          {/* Category Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
            {categories.map(cat => (
              <button
                key={cat.id}
                onClick={() => setActiveCategory(cat.id)}
                className={`px-3.5 py-1.5 rounded-2xl text-xs font-bold whitespace-nowrap transition-all ${
                  activeCategory === cat.id
                    ? 'bg-purple-600 text-white shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                }`}
              >
                {cat.label} ({cat.count})
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            {/* Select Mode Toggle */}
            <button
              type="button"
              onClick={() => {
                const next = !isSelectMode;
                setIsSelectMode(next);
                if (!next) setSelectedClothesIds(new Set());
              }}
              className={`inline-flex items-center justify-center gap-1.5 px-3.5 py-2 rounded-2xl text-xs font-bold transition-all shadow-xs cursor-pointer border ${
                isSelectMode
                  ? 'bg-purple-700 text-white border-purple-800 ring-2 ring-purple-400 shadow-purple-200'
                  : 'bg-white hover:bg-slate-50 text-slate-700 border-slate-200'
              }`}
              title="Select one or multiple clothes to add/remove tags like ai or unai"
            >
              <CheckSquare className="w-4 h-4" />
              <span>{isSelectMode ? 'Exit Select' : 'Select Mode'}</span>
              {isSelectMode && selectedClothesIds.size > 0 && (
                <span className="px-1.5 py-0.2 rounded-full text-[10px] font-mono bg-white text-purple-700 font-extrabold">
                  {selectedClothesIds.size}
                </span>
              )}
            </button>

            {/* Add Clothes Button */}
            <button
              onClick={onOpenAddModal}
              className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-2xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold shadow-xs transition-colors shrink-0 active:scale-95 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              Add Clothes
            </button>
          </div>
        </div>

        {/* Dynamic Filters & Presets Row */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 pt-1">
          {/* Preset Chips */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs no-scrollbar flex-1">
            <span className="text-slate-400 font-bold text-[10px] uppercase font-mono tracking-wider shrink-0 flex items-center gap-1">
              <SlidersHorizontal className="w-3 h-3" />
              Presets:
            </span>
            {presets.map(p => (
              <button
                key={p.id}
                onClick={() => setActivePreset(p.id)}
                className={`px-3 py-1.5 rounded-xl whitespace-nowrap font-medium text-xs transition-all cursor-pointer ${
                  activePreset === p.id
                    ? 'bg-purple-100 text-purple-900 border border-purple-200 font-bold'
                    : 'bg-slate-50 text-slate-600 border border-slate-200/60 hover:bg-slate-100'
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>

          {/* Clothes Filter Drawer Trigger Buttons */}
          <div className="flex items-center gap-1.5 shrink-0">
            <button
              onClick={() => {
                setFilterDrawerTab('filters');
                setIsFilterDrawerOpen(true);
              }}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl font-bold text-xs transition-all shadow-2xs cursor-pointer border ${
                totalActiveFilterCount > 0
                  ? 'bg-purple-600 text-white border-purple-600 shadow-purple-200'
                  : 'bg-white hover:bg-purple-50 text-purple-700 border-purple-200/90'
              }`}
              title="Filter by any clothes field (material, thickness, breasts, comfort, tags, etc.)"
            >
              <Filter className="w-3.5 h-3.5" />
              <span>Clothes Filters</span>
              {totalActiveFilterCount > 0 && (
                <span className="px-1.5 py-0.2 rounded-full text-[10px] font-mono bg-white text-purple-700 font-extrabold">
                  {totalActiveFilterCount}
                </span>
              )}
            </button>

            <button
              onClick={() => {
                setFilterDrawerTab('manage');
                setIsFilterDrawerOpen(true);
              }}
              className="p-1.5 rounded-xl border border-slate-200 hover:border-purple-300 bg-white hover:bg-purple-50 text-slate-600 hover:text-purple-700 transition-colors cursor-pointer"
              title="Manage Filter Fields (tick/untick fields from clothes filter)"
            >
              <Settings2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Active Filters Ribbon */}
        {totalActiveFilterCount > 0 && (
          <div className="flex flex-wrap items-center gap-2 pt-2.5 pb-2 border-t border-purple-100 bg-purple-50/50 -mx-5 sm:-mx-6 px-5 sm:px-6 rounded-2xl">
            <span className="text-[11px] font-bold text-purple-900 flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5 text-purple-600" />
              Active Filters ({filteredClothes.length} matched):
            </span>
            {Object.entries(activeFilters).map(([fieldKey, vals]) => {
              if (!vals || vals.length === 0) return null;
              const def = ALL_CLOTHES_FILTER_FIELDS.find(f => f.key === fieldKey);
              const label = def ? def.label.split('(')[0].trim() : (fieldKey.startsWith('custom_') ? fieldKey.replace(/^custom_/, '') : fieldKey);
              return (
                <span
                  key={fieldKey}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-purple-100 text-purple-900 border border-purple-200 text-xs font-medium"
                >
                  <span className="font-bold">{label}:</span>
                  <span className="truncate max-w-[150px]">{vals.join(', ')}</span>
                  <button
                    onClick={() => {
                      const next = { ...activeFilters };
                      delete next[fieldKey];
                      setActiveFilters(next);
                    }}
                    className="text-purple-500 hover:text-purple-950 hover:bg-purple-200/80 rounded p-0.5 transition-colors cursor-pointer"
                    title={`Remove ${label} filter`}
                  >
                    <X className="w-3 h-3" />
                  </button>
                </span>
              );
            })}
            <button
              onClick={() => setActiveFilters({})}
              className="text-[11px] font-bold text-rose-600 hover:text-rose-800 underline cursor-pointer ml-1"
            >
              Clear all
            </button>
          </div>
        )}

        {/* Search & Sort Row */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-2.5 pt-3 border-t border-slate-100">
          <div className="relative w-full sm:max-w-md">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by Name, material, thick clothes..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 text-xs rounded-xl border border-slate-200 bg-slate-50/50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-between sm:justify-end">
            <span className="text-xs text-slate-400 shrink-0 font-mono">Sort:</span>
            <select
              value={sortBy}
              onChange={(e: any) => setSortBy(e.target.value)}
              className="px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-medium text-slate-700"
            >
              <option value="washes-desc">Total Washed Count</option>
              <option value="cpw">Best Cost per Wear</option>
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
              <option value="name">Name (A-Z)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Batch Select & Tagging Action Bar */}
      {isSelectMode && (
        <div className="bg-white rounded-3xl p-4 sm:p-5 border-2 border-purple-500 shadow-lg space-y-3 animate-in fade-in slide-in-from-top-2 duration-150">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2 sm:gap-3 flex-wrap">
              <div className="px-3 py-1.5 rounded-xl bg-purple-100 text-purple-800 font-bold text-xs flex items-center gap-1.5">
                <CheckSquare className="w-4 h-4 text-purple-600" />
                <span>{selectedClothesIds.size} of {clothes.length} Selected</span>
              </div>

              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={handleSelectAll}
                  className="text-xs font-bold text-purple-700 hover:text-purple-900 px-2.5 py-1 rounded-lg hover:bg-purple-50 transition-colors cursor-pointer"
                >
                  Select All ({filteredClothes.length})
                </button>
                <span className="text-slate-300">•</span>
                <button
                  type="button"
                  onClick={handleDeselectAll}
                  className="text-xs font-medium text-slate-500 hover:text-slate-800 px-2.5 py-1 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
                >
                  Deselect All
                </button>
              </div>
            </div>

            <button
              type="button"
              onClick={() => {
                setIsSelectMode(false);
                setSelectedClothesIds(new Set());
              }}
              className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs font-bold transition-colors cursor-pointer flex items-center gap-1 self-start sm:self-auto"
            >
              <X className="w-3.5 h-3.5" />
              <span>Exit Selection</span>
            </button>
          </div>

          {/* Tag Configuration Row */}
          <div className="pt-2 border-t border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-3 bg-slate-50/80 p-3 rounded-2xl">
            <div className="flex flex-wrap items-center gap-2 flex-1">
              <span className="text-xs font-bold text-slate-700 flex items-center gap-1 shrink-0">
                <Tag className="w-3.5 h-3.5 text-purple-600" />
                Target Tag:
              </span>

              {/* Quick Pills for ai and unai */}
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => setTargetTag('ai')}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1 ${
                    targetTag.toLowerCase() === 'ai'
                      ? 'bg-purple-600 text-white shadow-2xs'
                      : 'bg-white hover:bg-purple-50 text-purple-700 border border-purple-200'
                  }`}
                >
                  <span>🤖 ai</span>
                </button>

                <button
                  type="button"
                  onClick={() => setTargetTag('unai')}
                  className={`px-3 py-1 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1 ${
                    targetTag.toLowerCase() === 'unai'
                      ? 'bg-slate-800 text-white shadow-2xs'
                      : 'bg-white hover:bg-slate-100 text-slate-700 border border-slate-200'
                  }`}
                >
                  <span>unai</span>
                </button>
              </div>

              {/* Tag Input */}
              <div className="relative flex-1 min-w-[140px] max-w-xs">
                <input
                  type="text"
                  placeholder="Type tag name..."
                  value={targetTag}
                  onChange={(e) => setTargetTag(e.target.value)}
                  className="w-full px-3 py-1 text-xs rounded-xl border border-slate-200 bg-white focus:outline-none focus:border-purple-500 font-medium"
                />
              </div>
            </div>

            {/* Action Buttons: Add Tag vs Remove Tag */}
            <div className="flex items-center gap-2 shrink-0">
              <button
                type="button"
                disabled={selectedClothesIds.size === 0 || !targetTag.trim() || isTagProcessing}
                onClick={() => handleBatchTagAction('add')}
                className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs font-bold transition-all shadow-xs cursor-pointer flex items-center gap-1.5 active:scale-95"
                title="Add tag to selected items that don't have it (existing tagged ones stay tagged)"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Tag "{targetTag || '...'}"</span>
              </button>

              <button
                type="button"
                disabled={selectedClothesIds.size === 0 || !targetTag.trim() || isTagProcessing}
                onClick={() => handleBatchTagAction('remove')}
                className="px-4 py-2 rounded-xl bg-rose-50 hover:bg-rose-100 disabled:opacity-40 disabled:cursor-not-allowed text-rose-700 border border-rose-200 text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 active:scale-95"
                title="Remove tag from all selected items"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Remove Tag</span>
              </button>
            </div>
          </div>

          {/* Feedback notice */}
          {tagFeedback && (
            <div className="px-3.5 py-2 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium animate-in fade-in flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>{tagFeedback}</span>
            </div>
          )}
        </div>
      )}

      {/* Clothes Grid */}
      {sortedClothes.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {sortedClothes.map(item => (
            <ClothesCard
              key={item.id}
              clothes={item}
              stats={getStats(item.id)}
              onSelect={handleSelectClothes}
              isSelectMode={isSelectMode}
              isSelected={selectedClothesIds.has(item.id)}
              onToggleSelect={handleToggleSelect}
            />
          ))}
        </div>
      ) : clothes.length === 0 ? (
        <div className="p-10 sm:p-14 text-center rounded-3xl bg-white border border-slate-200 text-slate-500 space-y-4 max-w-lg mx-auto shadow-xs">
          <div className="w-14 h-14 rounded-3xl bg-purple-50 text-purple-600 flex items-center justify-center mx-auto border border-purple-100">
            <Shirt className="w-7 h-7" />
          </div>
          <div className="space-y-1">
            <h3 className="font-bold text-slate-900 text-base">Your Wardrobe Database is Empty</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              You have an empty database. You can start adding your own clothes from scratch or restore the initial seed dataset at any time.
            </p>
          </div>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-2.5 pt-2">
            <button
              type="button"
              onClick={onOpenAddModal}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold shadow-xs transition-colors cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>Add First Garment</span>
            </button>
            <button
              type="button"
              onClick={async () => {
                const { dataService } = await import('../services/dataService');
                await dataService.resetToSeedData();
              }}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-900 text-xs font-bold transition-colors cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5 text-amber-600" />
              <span>Load Seed Data</span>
            </button>
          </div>
        </div>
      ) : (
        <div className="p-12 text-center rounded-3xl bg-white border border-slate-200 text-slate-500 space-y-3">
          <Shirt className="w-10 h-10 mx-auto text-slate-300 stroke-[1.25]" />
          <p className="font-bold text-slate-800 text-sm">No matching clothes found</p>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            {totalActiveFilterCount > 0
              ? 'No clothes in your wardrobe match the currently active data filters.'
              : 'Try adjusting your search criteria, switching presets, or click "All Items".'}
          </p>
          {totalActiveFilterCount > 0 && (
            <div className="pt-1">
              <button
                type="button"
                onClick={() => setActiveFilters({})}
                className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-colors shadow-xs cursor-pointer inline-flex items-center gap-1.5"
              >
                <span>Clear All Active Filters</span>
              </button>
            </div>
          )}
        </div>
      )}

      {/* Data-Driven Clothes Filter Drawer */}
      <ClothesFilterDrawer
        isOpen={isFilterDrawerOpen}
        onClose={() => setIsFilterDrawerOpen(false)}
        clothes={clothes}
        getStats={getStats}
        activeFilters={activeFilters}
        onChangeFilters={setActiveFilters}
        onClearFilters={() => setActiveFilters({})}
        initialTab={filterDrawerTab}
      />
    </div>
  );
};

export const ClothesStudio = WardrobeStudio;
