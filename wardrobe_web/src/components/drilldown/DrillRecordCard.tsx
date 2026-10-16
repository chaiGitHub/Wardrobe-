import React from 'react';
import { ClothesItem, ReportPreset } from '../../types';
import { resolveImageSource } from '../../utils/imageUtils';
import { reportPresetService, AVAILABLE_REPORT_FIELDS } from '../../services/reportPresetService';
import { dataService, formatDuration } from '../../services/dataService';
import { Shirt, Sparkles, Droplets, Clock, DollarSign, Tag, CheckCircle2, AlertCircle } from 'lucide-react';

interface DrillRecordCardProps {
  garment: ClothesItem;
  preset: ReportPreset;
  onSelect: (garment: ClothesItem) => void;
}

export const DrillRecordCard: React.FC<DrillRecordCardProps> = ({ garment, preset, onSelect }) => {
  const pictures = garment['Pictures'] || [];
  const pic = pictures.length > 0 ? pictures[0] : null;
  const imageUrl = resolveImageSource(pic);

  const stats = dataService.getGarmentStats(garment.id);
  const clothesType = garment['Clothes Type'] || garment.clothesType || 'Garment';
  const name = garment['Name'] || garment.name || 'Unnamed Garment';
  const price = garment['price'];
  const washCount = stats.totalWears || garment['Total Washed Count'] || garment['wearing frequency(how many times legacy washed)'] || 0;
  const costPerWear = price && washCount > 0 ? (price / washCount).toFixed(2) : null;

  // Map for fast label lookup
  const fieldMetaMap = new Map(AVAILABLE_REPORT_FIELDS.map((f) => [f.key, f]));

  return (
    <div
      onClick={() => onSelect(garment)}
      className="group relative bg-white rounded-2xl border border-slate-200/80 hover:border-indigo-300 hover:shadow-md transition-all duration-200 cursor-pointer overflow-hidden flex flex-col active:scale-[0.99]"
    >
      {/* Top Image & Type Header */}
      <div className="relative aspect-4/3 w-full bg-slate-100 overflow-hidden flex items-center justify-center">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={name}
            loading="lazy"
            referrerPolicy="no-referrer"
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="w-full h-full flex flex-col items-center justify-center text-slate-300 bg-slate-50 gap-1.5">
            <Shirt className="w-10 h-10 stroke-[1.2]" />
            <span className="text-[11px] font-medium text-slate-400">No Photo</span>
          </div>
        )}

        {/* Category Pill */}
        <div className="absolute top-2.5 left-2.5">
          <span className="px-2.5 py-1 rounded-xl text-[10px] font-bold tracking-wide uppercase bg-white/95 text-slate-800 shadow-xs backdrop-blur-xs border border-white/60">
            {clothesType}
          </span>
        </div>

        {/* Wash Status Badge */}
        <div className="absolute top-2.5 right-2.5">
          {stats.needWash ? (
            <span className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-[10px] font-bold bg-amber-500 text-white shadow-xs">
              <AlertCircle className="w-3 h-3" />
              <span>Need Wash</span>
            </span>
          ) : (
            <span className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-[10px] font-bold bg-emerald-600/90 text-white shadow-xs backdrop-blur-xs">
              <CheckCircle2 className="w-3 h-3" />
              <span>Clean</span>
            </span>
          )}
        </div>
      </div>

      {/* Card Content Area */}
      <div className="p-3.5 flex-1 flex flex-col justify-between space-y-3">
        <div>
          {/* Garment Title */}
          <div className="flex items-start justify-between gap-1.5 mb-1.5">
            <h4 className="font-bold text-slate-900 text-sm leading-snug line-clamp-1 group-hover:text-indigo-600 transition-colors">
              {name}
            </h4>
            {price !== undefined && price > 0 && (
              <span className="text-xs font-mono font-bold text-slate-700 shrink-0">
                ${price.toFixed(2)}
              </span>
            )}
          </div>

          {/* Preset Driven Visible Fields Badges */}
          <div className="flex flex-wrap gap-1.5 mt-2">
            {preset.visibleFields
              .filter((f) => f !== 'Name') // Name is already in title
              .map((fieldKey) => {
                const meta = fieldMetaMap.get(fieldKey);
                const rawVal = reportPresetService.getFieldValue(garment, fieldKey);
                if (rawVal === '(Not specified)' || rawVal === '(None)') return null;

                return (
                  <div
                    key={fieldKey}
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-medium bg-slate-100 text-slate-700 border border-slate-200/60 max-w-full"
                    title={`${meta?.label || fieldKey}: ${rawVal}`}
                  >
                    <span className="text-slate-400 font-normal">{meta?.label || fieldKey}:</span>
                    <span className="font-semibold text-slate-900 truncate">{rawVal}</span>
                  </div>
                );
              })}
          </div>
        </div>

        {/* Footer Meta Row */}
        <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
          <span className="flex items-center gap-1 font-mono">
            <Clock className="w-3 h-3 text-slate-400" />
            <span>{washCount} wears</span>
          </span>

          {costPerWear && (
            <span className="font-mono text-indigo-600 font-semibold">
              ${costPerWear}/wear
            </span>
          )}
        </div>
      </div>
    </div>
  );
};
