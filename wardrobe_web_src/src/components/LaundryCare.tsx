import React, { useState } from 'react';
import { ClothesItem, ReportClothesJunction } from '../types';
import { Sparkles, CheckCircle2, AlertCircle, Clock, Droplets, ArrowRight, ShieldCheck, RefreshCw } from 'lucide-react';

interface LaundryCareProps {
  clothes: ClothesItem[];
  junctions: ReportClothesJunction[];
  onMarkWashed: (clothesId: string) => void;
  onMarkAllWashed: () => void;
  onSelectGarment: (garment: ClothesItem) => void;
}

export const LaundryCare: React.FC<LaundryCareProps> = ({
  clothes,
  junctions,
  onMarkWashed,
  onMarkAllWashed,
  onSelectGarment
}) => {
  const [successToast, setSuccessToast] = useState('');

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
  const cleanItems = laundryItems.filter(i => !i.needWash);

  const totalUnwashedHours = (hamperItems.reduce((acc, i) => acc + i.unwashedSeconds, 0) / 3600).toFixed(1);
  const totalWashesRecorded = clothes.reduce((acc, c) => acc + (c['Total Washed Count'] || 0), 0);

  const handleWashAll = () => {
    onMarkAllWashed();
    setSuccessToast('All items in the laundry basket marked as washed (clean)!');
    setTimeout(() => setSuccessToast(''), 4000);
  };

  const handleSingleWash = (garmentId: string, garmentName: string) => {
    onMarkWashed(garmentId);
    setSuccessToast(`"${garmentName}" marked as washed!`);
    setTimeout(() => setSuccessToast(''), 3000);
  };

  return (
    <div id="laundry-care-view" className="space-y-5 w-full max-w-full">
      {/* Toast feedback */}
      {successToast && (
        <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
          <button onClick={() => setSuccessToast('')} className="text-emerald-700 hover:text-emerald-900 text-[11px]">
            Dismiss
          </button>
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
          <button
            onClick={handleWashAll}
            className="w-full sm:w-auto px-5 py-2.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-1.5 shrink-0"
          >
            <Droplets className="w-4 h-4" />
            Mark All as Washed ({hamperItems.length})
          </button>
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
            <p className="text-xs text-slate-500">Flagged from your outfit sessions</p>
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
                  onClick={() => onSelectGarment(garment)}
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
                  onClick={() => handleSingleWash(garment.id, garment['Name'])}
                  className="px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs transition-colors flex items-center gap-1 shrink-0"
                  title="Mark as wash"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  Washed
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 text-center rounded-2xl bg-emerald-50/50 border border-emerald-100 flex flex-col items-center">
            <CheckCircle2 className="w-10 h-10 text-emerald-500 mb-1.5" />
            <h4 className="text-sm font-bold text-emerald-900">Your Hamper is Empty!</h4>
            <p className="text-xs text-emerald-700 max-w-md mt-1">
              All garments in your wardrobe are clean and ready in your closet.
            </p>
          </div>
        )}
      </div>

      {/* Ready & Clean Items in Closet */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-3">
        <div>
          <h3 className="text-base font-bold text-slate-900">Clean & In Closet ({cleanItems.length})</h3>
          <p className="text-xs text-slate-500">Ready for daily wear</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {cleanItems.slice(0, 15).map(({ garment }) => (
            <div
              key={garment.id}
              onClick={() => onSelectGarment(garment)}
              className="p-3 rounded-2xl bg-slate-50 hover:bg-slate-100/80 border border-slate-100 cursor-pointer transition-colors flex items-center justify-between"
            >
              <div className="min-w-0 pr-2">
                <span className="text-[10px] font-mono font-bold uppercase text-slate-400 block">
                  {garment['Clothes Type']}
                </span>
                <span className="text-xs font-bold text-slate-800 truncate block">
                  {garment['Name']}
                </span>
                <span className="text-[11px] text-slate-500 truncate block">
                  {garment['material']}
                </span>
              </div>

              <div className="text-right shrink-0">
                <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-100">
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  wash
                </span>
                <span className="text-[10px] text-slate-400 block mt-1">
                  {garment['Total Washed Count'] || 0} washes
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
