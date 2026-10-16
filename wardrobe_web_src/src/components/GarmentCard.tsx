import React from 'react';
import { ClothesItem } from '../types';
import { Sparkles, ThermometerSun, ShieldCheck, Shirt, DollarSign, Droplets, CheckCircle2, Clock, AlertCircle } from 'lucide-react';

interface GarmentCardProps {
  garment: ClothesItem;
  stats: {
    needWash: boolean;
    unwashedSeconds: number;
    unwashedHours: string;
    totalWears: number;
    totalDurationHours: number;
  };
  onSelect: (garment: ClothesItem) => void;
  onQuickWashToggle?: (clothesId: string) => void;
}

export const GarmentCard: React.FC<GarmentCardProps> = ({ garment, stats, onSelect, onQuickWashToggle }) => {
  const pictures = garment['Pictures'] || [];
  const pic = pictures.length > 0 ? pictures[0] : null;
  const imageUrl = pic?.smThumbnailUrl || pic?.presignedUrl;

  const priceVal = typeof garment['price'] === 'number' ? garment['price'] : undefined;
  const washCount = garment['Total Washed Count'] || stats.totalWears;
  const costPerWear = priceVal && washCount > 0
    ? (priceVal / washCount).toFixed(2)
    : null;

  const clothesType = garment['Clothes Type'] || garment.clothesType || 'Dress';
  const name = garment['Name'] || garment.name;
  const material = garment['material'] || garment.materialProp;
  const thickClothes = garment['thick clothes'];
  const hotAtNoon = garment['Hot at noon'];
  const diaoDai = garment['diao dai'];
  const sitBreastOk = garment['sit breast ok'];
  const wet = garment['wet'];
  const beautiful = garment['beautiful'];

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
      id={`garment-card-${garment.id}`}
      onClick={() => onSelect(garment)}
      className="group relative bg-white rounded-3xl border border-slate-200/80 hover:border-slate-300 hover:shadow-md transition-all duration-200 cursor-pointer overflow-hidden flex flex-col active:scale-[0.99]"
    >
      {/* Top Media / Thumbnail preview */}
      <div className="relative h-44 w-full bg-gradient-to-br from-slate-100 to-slate-200 flex items-center justify-center overflow-hidden">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            referrerPolicy="no-referrer"
            onError={(e) => {
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
        ) : (
          <div className="flex flex-col items-center justify-center p-4 text-slate-400">
            <Shirt className="w-12 h-12 stroke-[1.25] text-slate-400/80 mb-2" />
            <span className="text-[11px] font-mono text-slate-400 tracking-wide uppercase">{material || clothesType}</span>
          </div>
        )}

        {/* Laundry Status Pill (Top Left) */}
        <div className="absolute top-3 left-3 flex items-center gap-1.5">
          {stats.needWash ? (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-amber-500/90 text-white shadow-xs backdrop-blur-xs">
              <AlertCircle className="w-3 h-3" />
              need wash ({stats.unwashedHours}h)
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-bold bg-emerald-600/90 text-white shadow-xs backdrop-blur-xs">
              <CheckCircle2 className="w-3 h-3" />
              wash
            </span>
          )}
        </div>

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
          {/* Garment Title */}
          <h3 className="font-bold text-slate-900 text-sm sm:text-base leading-snug line-clamp-1 group-hover:text-purple-700 transition-colors">
            {name}
          </h3>

          {/* Material & Fabric description */}
          <p className="text-xs text-slate-500 mt-1 line-clamp-1">
            <span className="font-semibold text-slate-700">{material || 'No material data'}</span>
            {thickClothes ? ` · ${thickClothes}` : ' · standard thickness'}
          </p>

          {/* Sensory & Comfort tags with exact database field values */}
          <div className="flex flex-wrap gap-1.5 mt-3">
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
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-medium bg-purple-50 text-purple-700 border border-purple-200 truncate max-w-[140px]">
                <ShieldCheck className="w-3 h-3 shrink-0" />
                {sitBreastOk}
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
              {garment['Total Washed Count'] || 0} washes
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
