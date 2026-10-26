import React, { useState } from 'react';
import { ClothesItem, ReportClothesJunction } from '../types';
import { Sparkles, CheckCircle2, AlertCircle, Clock, Droplets, ArrowRight, ShieldCheck, RefreshCw, Calendar, RotateCcw } from 'lucide-react';
import { getTodayDateKey } from '../utils/calendarCalculations';
import { ConfirmModal } from './ConfirmModal';
import { dataService } from '../services/dataService';

interface LaundryCareProps {
  clothes: ClothesItem[];
  junctions: ReportClothesJunction[];
  onMarkWashed: (clothesId: string, customWashDate?: string) => void;
  onMarkAllWashed: (customWashDate?: string) => void;
  onSelectClothes?: (clothes: ClothesItem) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
}

export const LaundryCare: React.FC<LaundryCareProps> = ({
  clothes,
  junctions,
  onMarkWashed,
  onMarkAllWashed,
  onSelectClothes,
  onSelectGarment
}) => {
  const handleSelectClothesItem = (item: ClothesItem) => {
    if (onSelectClothes) onSelectClothes(item);
    else if (onSelectGarment) onSelectGarment(item);
  };
  const [successToast, setSuccessToast] = useState('');
  const [washDate, setWashDate] = useState(() => getTodayDateKey());
  const [confirmConfig, setConfirmConfig] = useState<{
    isOpen: boolean;
    title: string;
    message: React.ReactNode;
    confirmLabel: string;
    variant: 'primary' | 'danger' | 'warning';
    onConfirm: () => void | Promise<void>;
  } | null>(null);

  // Calculate laundry stats for each garment using exact DB fields
  const laundryItems = clothes.map(garment => {
    const itemJunctions = junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId === garment.id;
    });
    const needWashJunctions = itemJunctions.filter(j => j['wash stated'] === 'need wash');
    const needWash = needWashJunctions.length > 0;
    const unwashedSeconds = needWashJunctions.reduce((acc, j) => acc + (j['duration'] || 0), 0);
    const unwashedHours = (unwashedSeconds / 3600).toFixed(1);

    return {
      garment,
      needWash,
      unwashedSeconds,
      unwashedHours,
      totalWears: itemJunctions.length
    };
  });

  const hamperItems = laundryItems.filter(i => i.needWash);

  const totalUnwashedHours = (hamperItems.reduce((acc, i) => acc + i.unwashedSeconds, 0) / 3600).toFixed(1);
  const totalWashesRecorded = clothes.reduce((acc, c) => acc + (c['Total Washed Count'] || 0), 0);

  const promptWashAll = () => {
    if (hamperItems.length === 0) return;
    setConfirmConfig({
      isOpen: true,
      title: `Wash All ${hamperItems.length} Items?`,
      variant: 'primary',
      confirmLabel: 'Yes, Wash All',
      message: (
        <div className="space-y-2">
          <p>
            Are you sure you want to mark all <strong>{hamperItems.length} items</strong> in your hamper as washed?
          </p>
          <div className="p-2.5 rounded-xl bg-blue-50 border border-blue-100 text-xs text-blue-900 flex items-center gap-2">
            <Droplets className="w-4 h-4 text-blue-600 shrink-0" />
            <span>Wash date: <strong>{washDate}</strong></span>
          </div>
          <p className="text-[11px] text-slate-500">
            This resets unwashed wear hours and increments wash counts. You can easily undo if clicked by mistake.
          </p>
        </div>
      ),
      onConfirm: async () => {
        setConfirmConfig(null);
        await onMarkAllWashed(washDate);
        setSuccessToast(`All ${hamperItems.length} items marked as washed on ${washDate}!`);
        setTimeout(() => setSuccessToast(''), 5000);
      }
    });
  };

  const promptSingleWash = (garmentId: string, garmentName: string) => {
    setConfirmConfig({
      isOpen: true,
      title: `Wash "${garmentName}"?`,
      variant: 'primary',
      confirmLabel: 'Yes, Mark as Washed',
      message: (
        <div className="space-y-2">
          <p>
            Are you sure you want to mark <strong>"{garmentName}"</strong> as washed?
          </p>
          <div className="p-2.5 rounded-xl bg-blue-50 border border-blue-100 text-xs text-blue-900 flex items-center gap-2">
            <Droplets className="w-4 h-4 text-blue-600 shrink-0" />
            <span>Wash date: <strong>{washDate}</strong></span>
          </div>
          <p className="text-[11px] text-slate-500">
            This resets unwashed wear hours for this garment and records this wash cycle. You can easily undo if clicked by mistake.
          </p>
        </div>
      ),
      onConfirm: async () => {
        setConfirmConfig(null);
        await onMarkWashed(garmentId, washDate);
        setSuccessToast(`"${garmentName}" marked as washed on ${washDate}!`);
        setTimeout(() => setSuccessToast(''), 5000);
      }
    });
  };

  return (
    <div id="laundry-care-view" className="space-y-5 w-full max-w-full">
      {/* Toast feedback with instant Undo */}
      {successToast && (
        <div className="p-3.5 rounded-2xl bg-blue-50 border border-blue-200 text-blue-900 text-xs font-bold flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
            <span>{successToast}</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={async () => {
                const res = await dataService.undoWash();
                if (res.success) {
                  setSuccessToast('Wash undone! Restored garment(s) back to laundry.');
                  setTimeout(() => setSuccessToast(''), 4000);
                }
              }}
              className="px-2.5 py-1 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-[11px] font-bold flex items-center gap-1 cursor-pointer shadow-2xs active:scale-95"
              title="Undo wash action"
            >
              <RotateCcw className="w-3 h-3" />
              Undo
            </button>
            <button onClick={() => setSuccessToast('')} className="text-blue-600 hover:text-blue-900 text-[11px] cursor-pointer">
              Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Header & Stats Banner */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-xl bg-blue-50 text-blue-600">
              <Droplets className="w-4 h-4" />
            </span>
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 font-mono">
              wash stated · Care Cycle
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Laundry Basket & Hygiene
          </h2>
          <p className="text-xs text-slate-500 max-w-lg leading-relaxed">
            Track unwashed wear duration and maintain delicate fabric hygiene.
          </p>
        </div>

        {hamperItems.length > 0 && (
          <div className="flex flex-col sm:flex-row items-center gap-2 w-full sm:w-auto">
            <div className="flex items-center gap-1.5 bg-white px-3 py-2 rounded-2xl border border-slate-200 shadow-2xs w-full sm:w-auto">
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              <span className="text-xs font-mono text-slate-500">Wash Date:</span>
              <input
                type="date"
                value={washDate}
                onChange={(e) => setWashDate(e.target.value)}
                className="text-xs font-mono font-medium text-slate-800 bg-transparent outline-hidden"
              />
              <button
                type="button"
                onClick={() => setWashDate(getTodayDateKey())}
                className="text-[10px] font-bold text-blue-600 hover:text-blue-700 ml-1 px-1.5 py-0.5 rounded bg-blue-50"
              >
                Today
              </button>
            </div>
            <button
              onClick={promptWashAll}
              className="w-full sm:w-auto px-5 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 active:scale-95 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer"
            >
              <Droplets className="w-4 h-4" />
              Wash All Now ({hamperItems.length})
            </button>
          </div>
        )}
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 text-[10px] font-mono font-bold uppercase tracking-wider">
            <span>wash stated: need wash</span>
            <AlertCircle className={`w-3.5 h-3.5 ${hamperItems.length > 0 ? 'text-amber-500' : 'text-slate-400'}`} />
          </div>
          <div className="mt-1 flex items-baseline gap-1.5">
            <span className="text-2xl font-bold text-slate-900">{hamperItems.length}</span>
            <span className="text-xs text-slate-500">items</span>
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">Waiting in basket</span>
        </div>

        <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 text-[10px] font-mono font-bold uppercase tracking-wider">
            <span>unwash time rollup</span>
            <Clock className="w-3.5 h-3.5 text-blue-500" />
          </div>
          <div className="mt-1 flex items-baseline gap-1.5">
            <span className="text-2xl font-bold text-slate-900">{totalUnwashedHours}</span>
            <span className="text-xs text-slate-500">hours</span>
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">Active unwashed wear</span>
        </div>

        <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 text-[10px] font-mono font-bold uppercase tracking-wider">
            <span>Total Washed Count</span>
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
          </div>
          <div className="mt-1 flex items-baseline gap-1.5">
            <span className="text-2xl font-bold text-slate-900">{totalWashesRecorded}</span>
            <span className="text-xs text-slate-500">total washes</span>
          </div>
          <span className="text-[10px] text-slate-400 mt-0.5 block">Across all wardrobe items</span>
        </div>
      </div>

      {/* Main Section: Items in Hamper needing wash */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-900">Items Needing Wash ({hamperItems.length})</h3>
            <p className="text-xs text-slate-500">Flagged from your wearing reports</p>
          </div>
        </div>

        {hamperItems.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {hamperItems.map(({ garment, unwashedHours, totalWears }) => (
              <div
                key={garment.id}
                className="p-3.5 rounded-2xl border border-amber-200 bg-amber-50/40 hover:bg-amber-50/70 transition-colors flex items-center justify-between gap-3"
              >
                <div
                  onClick={() => handleSelectClothesItem(garment)}
                  className="cursor-pointer flex-1 min-w-0"
                >
                  <div className="flex items-center gap-1.5">
                    <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-white text-slate-700 border border-slate-200">
                      {garment['Clothes Type']}
                    </span>
                    <span className="text-[11px] text-amber-800 font-semibold">
                      {unwashedHours} hrs worn
                    </span>
                  </div>
                  <h4 className="text-sm font-bold text-slate-900 mt-1 truncate hover:text-blue-600 transition-colors">
                    {garment['Name']}
                  </h4>
                  <p className="text-xs text-slate-500 mt-0.5 truncate">
                    {garment['material']} · {garment['thick clothes'] || 'Standard'}
                  </p>
                </div>

                <button
                  onClick={() => promptSingleWash(garment.id, garment['Name'])}
                  className="px-3.5 py-1.5 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-700 active:scale-95 text-white shadow-xs transition-all flex items-center gap-1.5 shrink-0 cursor-pointer"
                  title={`Wash ${garment['Name']} now`}
                >
                  <Droplets className="w-3.5 h-3.5" />
                  Wash Now
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 text-center rounded-2xl bg-emerald-50/50 border border-emerald-100 flex flex-col items-center">
            <CheckCircle2 className="w-10 h-10 text-emerald-500 mb-1.5" />
            <h4 className="text-sm font-bold text-emerald-900">Your Hamper is Empty!</h4>
            <p className="text-xs text-emerald-700 max-w-md mt-1">
              All clothes in your wardrobe are clean and ready in your closet.
            </p>
          </div>
        )}
      </div>

      {/* Confirmation Modal */}
      {confirmConfig && (
        <ConfirmModal
          isOpen={confirmConfig.isOpen}
          onClose={() => setConfirmConfig(null)}
          onConfirm={confirmConfig.onConfirm}
          title={confirmConfig.title}
          message={confirmConfig.message}
          confirmLabel={confirmConfig.confirmLabel}
          variant={confirmConfig.variant}
        />
      )}
    </div>
  );
};
