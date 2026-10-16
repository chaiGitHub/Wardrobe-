import React, { useState } from 'react';
import { ClothesItem, ReportClothesJunction } from '../types';
import { GarmentCard } from './GarmentCard';
import { Search, Filter, Plus, Sparkles, ThermometerSun, AlertCircle, Shirt, RefreshCw, SlidersHorizontal } from 'lucide-react';

interface WardrobeStudioProps {
  clothes: ClothesItem[];
  junctions: ReportClothesJunction[];
  onSelectGarment: (garment: ClothesItem) => void;
  onOpenAddModal: () => void;
}

export const WardrobeStudio: React.FC<WardrobeStudioProps> = ({
  clothes,
  junctions,
  onSelectGarment,
  onOpenAddModal
}) => {
  const [activeCategory, setActiveCategory] = useState<string>('All');
  const [activePreset, setActivePreset] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [sortBy, setSortBy] = useState<'washes-desc' | 'price-asc' | 'price-desc' | 'name' | 'cpw'>('washes-desc');

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

  // Category counts
  const categories = [
    { id: 'All', label: 'All Items', count: clothes.length },
    { id: 'Dress', label: 'Dress', count: clothes.filter(c => (c['Clothes Type'] || '').toLowerCase() === 'dress').length },
    { id: 'bra', label: 'bra', count: clothes.filter(c => (c['Clothes Type'] || '').toLowerCase() === 'bra').length },
    { id: 'Tshirt', label: 'Tshirt', count: clothes.filter(c => (c['Clothes Type'] || '').toLowerCase().includes('tshirt') || (c['Clothes Type'] || '').toLowerCase().includes('t-shirt')).length },
    { id: 'Pant', label: 'Pant', count: clothes.filter(c => (c['Clothes Type'] || '').toLowerCase().includes('pant')).length }
  ];

  // Filter logic
  const filteredClothes = clothes.filter(c => {
    const cType = c['Clothes Type'] || '';
    const name = c['Name'] || '';
    const mat = c['material'] || '';
    const desc = c['Descriptions'] || '';

    // Category match
    if (activeCategory !== 'All') {
      const typeMatch = cType.toLowerCase() === activeCategory.toLowerCase() ||
        (activeCategory === 'Tshirt' && cType.toLowerCase().includes('tshirt')) ||
        (activeCategory === 'Pant' && cType.toLowerCase().includes('pant'));
      if (!typeMatch) return false;
    }

    // Search query
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchName = name.toLowerCase().includes(q);
      const matchMat = mat.toLowerCase().includes(q);
      const matchDesc = desc.toLowerCase().includes(q);
      if (!matchName && !matchMat && !matchDesc) return false;
    }

    // Presets
    const stats = getStats(c.id);
    if (activePreset === 'not-hot') {
      return (c['Hot at noon'] || '').toLowerCase().includes('not');
    }
    if (activePreset === 'favorites') {
      return (c['beautiful'] || '') === 'Beautiful';
    }
    if (activePreset === 'need-wash') {
      return stats.needWash;
    }
    if (activePreset === 'chiffon') {
      return mat.toLowerCase().includes('chiffon');
    }
    if (activePreset === 'sit-ok') {
      return (c['sit breast ok'] || '').toLowerCase().includes('ok');
    }

    return true;
  });

  // Sort logic
  const sortedClothes = [...filteredClothes].sort((a, b) => {
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
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-slate-50 hover:bg-slate-100 text-slate-600'
                }`}
              >
                {cat.label} ({cat.count})
              </button>
            ))}
          </div>

          {/* Add Garment Button */}
          <button
            onClick={onOpenAddModal}
            className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold shadow-xs transition-colors shrink-0"
          >
            <Plus className="w-4 h-4" />
            Add Garment
          </button>
        </div>

        {/* Preset Chips */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs no-scrollbar">
          <span className="text-slate-400 font-bold text-[10px] uppercase font-mono tracking-wider shrink-0 flex items-center gap-1">
            <SlidersHorizontal className="w-3 h-3" />
            Presets:
          </span>
          {presets.map(p => (
            <button
              key={p.id}
              onClick={() => setActivePreset(p.id)}
              className={`px-3 py-1.5 rounded-xl whitespace-nowrap font-medium text-xs transition-all ${
                activePreset === p.id
                  ? 'bg-purple-100 text-purple-900 border border-purple-200 font-bold'
                  : 'bg-slate-50 text-slate-600 border border-slate-200/60 hover:bg-slate-100'
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>

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

      {/* Garment Grid */}
      {sortedClothes.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {sortedClothes.map(garment => (
            <GarmentCard
              key={garment.id}
              garment={garment}
              stats={getStats(garment.id)}
              onSelect={onSelectGarment}
            />
          ))}
        </div>
      ) : (
        <div className="p-12 text-center rounded-3xl bg-white border border-slate-200 text-slate-500 space-y-2">
          <Shirt className="w-10 h-10 mx-auto text-slate-300 stroke-[1.25]" />
          <p className="font-bold text-slate-800 text-sm">No matching garments found</p>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Try adjusting your search criteria, switching presets, or click "All Items".
          </p>
        </div>
      )}
    </div>
  );
};
