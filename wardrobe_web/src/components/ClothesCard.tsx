import React from 'react';
import { Clothes } from '../types';
import { Sparkles, ThermometerSun, ShieldCheck, Shirt, DollarSign, Droplets, CheckCircle2, Clock, AlertCircle, Tag, CheckSquare, Square, Check } from 'lucide-react';
import { ImageWithFallback } from './ImageWithFallback';
import { resolveImageSource, useResolvedImageUrl } from '../utils/imageUtils';

export interface ClothesCardProps {
  clothes: Clothes;
  stats: {
    needWash: boolean;
    unwashedSeconds: number;
    unwashedHours: string;
    totalWears: number;
    totalDurationHours: number;
  };
  onSelect: (item: Clothes) => void;
  onQuickWashToggle?: (clothesId: string) => void;
  isSelectMode?: boolean;
  isSelected?: boolean;
  onToggleSelect?: (clothesId: string) => void;
}

export const ClothesCard: React.FC<ClothesCardProps> = ({
  clothes: item,
  stats,
  onSelect,
  onQuickWashToggle,
  isSelectMode = false,
  isSelected = false,
  onToggleSelect
}) => {
  const pictures = item['Pictures'] || [];
  const pic = pictures.length > 0 ? pictures[0] : null;
  const imageUrl = useResolvedImageUrl(pic);

  const priceVal = typeof item['price'] === 'number' ? item['price'] : undefined;
  const washCount = item['Total Washed Count'] || stats.totalWears;
  const costPerWear = priceVal && washCount > 0
    ? (priceVal / washCount).toFixed(2)
    : null;

  const clothesType = item['Clothes Type'] || item.clothesType || 'Dress';
  const name = item['Name'] || item.name;
  const material = item['material'] || item.materialProp;
  const thickClothes = item['thick clothes'];
  const hotAtNoon = item['Hot at noon'];
  const diaoDai = item['diao dai'];
  const sitBreastOk = item['sit breast ok: With 聚拢bra'] || item['sit breast ok: with side out bra'] || item['sit breast ok'];
  const sideBreastOk = item['side breast ok: with 聚拢 bra'] || item['side breast ok: with side out bra'] || item['side breast ok'];
  const wet = item['wet'];
  const beautiful = item['beautiful'];

  const handleClick = (e: React.MouseEvent) => {
    if (isSelectMode) {
      e.stopPropagation();
      if (onToggleSelect) {
        onToggleSelect(item.id);
      }
    } else {
      onSelect(item);
    }
  };

  // Category accent colors
  const getCategoryTheme = (type: string) => {
    switch ((type || '').toLowerCase()) {
      case 'dress':
        return { bg: 'bg-rose-50', text: 'text-rose-700', border: 'border-rose-200' };
      case 'bra':
        return { bg: 'bg-violet-50', text: 'text-violet-700', border: 'border-violet-200' };
      case 'tshirt':
      case 't-shirt':
        return { bg: 'bg-sky-50', text: 'text-sky-700', border: 'border-sky-200' };
      case 'pant':
      case 'pants':
        return { bg: 'bg-amber-50', text: 'text-amber-700', border: 'border-amber-200' };
      default:
        return { bg: 'bg-slate-50', text: 'text-slate-700', border: 'border-slate-200' };
    }
  };

  const theme = getCategoryTheme(clothesType);

  return (
    <div
      id={`clothes-card-${item.id}`}
      onClick={handleClick}
      className={`group relative bg-white rounded-3xl transition-all duration-200 cursor-pointer overflow-hidden flex flex-col active:scale-[0.99] ${
        isSelectMode && isSelected
          ? 'ring-2 ring-purple-600 border-2 border-purple-500 shadow-md bg-purple-50/15'
          : isSelectMode
          ? 'border-2 border-slate-200 hover:border-purple-300 hover:shadow-sm'
          : 'border border-slate-200/80 hover:border-slate-300 hover:shadow-md'
      }`}
    >
      {/* Top Media / Thumbnail preview */}
      <div className="relative h-48 w-full bg-slate-50 flex items-center justify-center overflow-hidden p-2">
        <ImageWithFallback
          src={imageUrl}
          alt={name}
          clothesId={item.id}
          className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
          fallbackIconClass="w-12 h-12"
        />

        {/* Select Mode Checkbox (Top Left) */}
        {isSelectMode ? (
          <div className="absolute top-3 left-3 z-10">
            {isSelected ? (
              <div className="w-6 h-6 rounded-lg bg-purple-600 text-white flex items-center justify-center shadow-md animate-in zoom-in-75 duration-150">
                <Check className="w-4 h-4 stroke-[3]" />
              </div>
            ) : (
              <div className="w-6 h-6 rounded-lg bg-white/95 border-2 border-slate-400 flex items-center justify-center shadow-xs group-hover:border-purple-500 transition-colors" />
            )}
          </div>
        ) : (
          /* Laundry Status Pill (Top Left when not in select mode) */
          <div className="absolute top-3 left-3 flex items-center gap-1.5">
            {stats.needWash ? (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/90 text-white shadow-xs backdrop-blur-xs">
                <AlertCircle className="w-3 h-3" />
                need wash ({stats.unwashedHours}h)
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-600/90 text-white shadow-xs backdrop-blur-xs">
                <CheckCircle2 className="w-3 h-3" />
                Clean
              </span>
            )}
          </div>
        )}

        {/* Category Pill (Top Right) */}
        <div className="absolute top-3 right-3">
          <span className={`px-2.5 py-1 rounded-full text-[11px] font-bold shadow-xs ${theme.bg} ${theme.text} border ${theme.border}`}>
            {clothesType}
          </span>
        </div>

        {/* Beauty rating star overlay (Bottom Left) */}
        {beautiful === 'Beautiful' && (
          <div className="absolute bottom-2.5 left-3">
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-white/95 text-amber-700 shadow-xs backdrop-blur-xs">
              <Sparkles className="w-3 h-3 text-amber-500 fill-amber-400" />
              Beautiful
            </span>
          </div>
        )}
      </div>

      {/* Card Body */}
      <div className="p-4 flex-1 flex flex-col justify-between">
        <div>
          {/* Clothes Title */}
          <h3 className="font-bold text-slate-900 text-sm sm:text-base leading-snug line-clamp-1 group-hover:text-purple-700 transition-colors">
            {name}
          </h3>

          {/* Material & Fabric description */}
          <p className="text-xs text-slate-500 mt-1 line-clamp-1">
            <span className="font-semibold text-slate-700">{material || 'No material data'}</span>
            {thickClothes ? ` · ${thickClothes}` : ' · standard thickness'}
          </p>

          {/* Tags Pills: prominent badges for 'ai', 'unai', etc. */}
          {item.tags && item.tags.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-2">
              {item.tags.map((tag, idx) => {
                const isAi = tag.toLowerCase() === 'ai';
                const isUnai = tag.toLowerCase() === 'unai';
                return (
                  <span
                    key={idx}
                    className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold ${
                      isAi
                        ? 'bg-purple-100 text-purple-800 border border-purple-300 shadow-2xs font-mono'
                        : isUnai
                        ? 'bg-slate-100 text-slate-700 border border-slate-300 font-mono'
                        : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    }`}
                  >
                    <Tag className="w-2.5 h-2.5" />
                    <span>{tag}</span>
                  </span>
                );
              })}
            </div>
          )}

          {/* Sensory & Comfort tags with exact database field values */}
          <div className="flex flex-wrap gap-1.5 mt-2.5">
            {hotAtNoon && (
              <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-medium ${
                hotAtNoon.toLowerCase().includes('not')
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : hotAtNoon.toLowerCase().includes('hot')
                  ? 'bg-orange-50 text-orange-700 border border-orange-200'
                  : 'bg-slate-100 text-slate-600'
              }`}>
                <ThermometerSun className="w-3 h-3" />
                {hotAtNoon}
              </span>
            )}

            {diaoDai && (
              <span className="px-2 py-0.5 rounded-md text-[10px] font-medium bg-slate-100 text-slate-600">
                {diaoDai}
              </span>
            )}

            {sitBreastOk && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-medium bg-purple-50 text-purple-700 border border-purple-200 truncate max-w-[140px]" title={`Sit breast: ${sitBreastOk}`}>
                <ShieldCheck className="w-3 h-3 shrink-0" />
                {sitBreastOk}
              </span>
            )}

            {sideBreastOk && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-medium bg-pink-50 text-pink-700 border border-pink-200 truncate max-w-[140px]" title={`Side breast: ${sideBreastOk}`}>
                <Sparkles className="w-3 h-3 shrink-0" />
                {sideBreastOk}
              </span>
            )}

            {wet && (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-medium bg-blue-50 text-blue-700 border border-blue-200">
                <Droplets className="w-3 h-3" />
                {wet}
              </span>
            )}
          </div>
        </div>

        {/* Footer Metrics */}
        <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1 text-[11px]" title="Total lifetime wash count">
              <Clock className="w-3.5 h-3.5 text-slate-400" />
              {item['Total Washed Count'] || 0} washes
            </span>
          </div>

          <div className="flex items-center gap-2">
            {typeof priceVal === 'number' ? (
              <span className="font-bold text-slate-900 text-xs">
                ${priceVal}
                {costPerWear && (
                  <span className="text-[10px] font-normal text-slate-500 ml-1">
                    (${costPerWear}/w)
                  </span>
                )}
              </span>
            ) : (
              <span className="text-slate-400 text-[10px] italic">
                price: No data
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

// Backward-compatibility alias
export const GarmentCard = ClothesCard;
