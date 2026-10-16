import React from 'react';
import { ClothesItem, ReportPreset } from '../../types';
import { resolveImageSource } from '../../utils/imageUtils';
import { reportPresetService } from '../../services/reportPresetService';
import { dataService } from '../../services/dataService';
import { Shirt, ChevronRight, AlertCircle, CheckCircle2 } from 'lucide-react';

interface DrillRecordRowProps {
  garment: ClothesItem;
  preset: ReportPreset;
  onSelect: (garment: ClothesItem) => void;
}

export const DrillRecordRow: React.FC<DrillRecordRowProps> = ({ garment, preset, onSelect }) => {
  const pictures = garment['Pictures'] || [];
  const pic = pictures.length > 0 ? pictures[0] : null;
  const imageUrl = resolveImageSource(pic);

  const stats = dataService.getGarmentStats(garment.id);
  const clothesType = garment['Clothes Type'] || garment.clothesType || 'Garment';
  const name = garment['Name'] || garment.name || 'Unnamed Garment';

  return (
    <tr
      onClick={() => onSelect(garment)}
      className="border-b border-slate-100 hover:bg-indigo-50/40 transition-colors cursor-pointer text-xs text-slate-700 group"
    >
      {/* Thumbnail + Name */}
      <td className="py-2.5 px-3 min-w-[180px]">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-200/80 flex items-center justify-center">
            {imageUrl ? (
              <img
                src={imageUrl}
                alt={name}
                referrerPolicy="no-referrer"
                loading="lazy"
                className="w-full h-full object-cover"
              />
            ) : (
              <Shirt className="w-4 h-4 text-slate-400" />
            )}
          </div>
          <div>
            <div className="font-semibold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-1">
              {name}
            </div>
            <div className="text-[10px] text-slate-400 font-mono">{clothesType}</div>
          </div>
        </div>
      </td>

      {/* Laundry Status */}
      <td className="py-2.5 px-3 whitespace-nowrap">
        {stats.needWash ? (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200">
            <AlertCircle className="w-2.5 h-2.5" />
            Need Wash
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-2.5 h-2.5" />
            Clean
          </span>
        )}
      </td>

      {/* Dynamic columns based on visibleFields */}
      {preset.visibleFields
        .filter((f) => f !== 'Name' && f !== 'wash stated')
        .slice(0, 5)
        .map((fieldKey) => {
          const val = reportPresetService.getFieldValue(garment, fieldKey);
          return (
            <td key={fieldKey} className="py-2.5 px-3 whitespace-nowrap text-slate-600 max-w-[150px] truncate">
              {val}
            </td>
          );
        })}

      {/* Action Arrow */}
      <td className="py-2.5 px-3 text-right">
        <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-indigo-600 group-hover:translate-x-0.5 transition-all inline-block" />
      </td>
    </tr>
  );
};
