import React, { useEffect, useState } from 'react';
import { RotateCcw, X, CheckCircle2, Droplets, Sparkles, Loader2 } from 'lucide-react';
import { dataService, WashUndoRecord } from '../services/dataService';

export const UndoToast: React.FC = () => {
  const [activeUndo, setActiveUndo] = useState<WashUndoRecord | null>(null);
  const [isUndoing, setIsUndoing] = useState(false);
  const [undoneFeedback, setUndoneFeedback] = useState<string | null>(null);

  useEffect(() => {
    // Subscribe to undo changes from dataService
    const unsubscribe = dataService.onWashUndoChange((record) => {
      setActiveUndo(record);
      if (record) {
        setUndoneFeedback(null);
      }
    });

    // Check if there is already an undo record available
    const existing = dataService.getLatestUndoRecord();
    if (existing) {
      setActiveUndo(existing);
    }

    return () => {
      unsubscribe();
    };
  }, []);

  // Auto-dismiss after 20 seconds if user does not interact
  useEffect(() => {
    if (!activeUndo) return;
    const timer = setTimeout(() => {
      dataService.dismissUndo(activeUndo.id);
      setActiveUndo(null);
    }, 20000);
    return () => clearTimeout(timer);
  }, [activeUndo]);

  const handleUndo = async () => {
    if (!activeUndo || isUndoing) return;
    setIsUndoing(true);
    try {
      const result = await dataService.undoWash(activeUndo.id);
      if (result.success) {
        setUndoneFeedback(`Wash undone! Reverted ${activeUndo.garments.length === 1 ? `"${activeUndo.garments[0].name}"` : `${activeUndo.garments.length} items`} back to laundry.`);
        setActiveUndo(null);
        setTimeout(() => {
          setUndoneFeedback(null);
        }, 5000);
      }
    } catch (err) {
      console.error('Failed to undo wash:', err);
    } finally {
      setIsUndoing(false);
    }
  };

  const handleDismiss = () => {
    if (activeUndo) {
      dataService.dismissUndo(activeUndo.id);
    }
    setActiveUndo(null);
  };

  if (!activeUndo && !undoneFeedback) return null;

  return (
    <div
      id="wash-undo-toast"
      className="fixed bottom-5 left-1/2 -translate-x-1/2 z-[130] w-[92vw] max-w-md animate-in slide-in-from-bottom-5 fade-in duration-200"
    >
      {undoneFeedback ? (
        <div className="bg-emerald-950/95 text-emerald-100 border border-emerald-500/40 backdrop-blur-md rounded-2xl p-3.5 sm:p-4 shadow-2xl flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="p-1.5 rounded-xl bg-emerald-500/20 text-emerald-400 shrink-0">
              <CheckCircle2 className="w-4 h-4" />
            </div>
            <p className="text-xs font-medium truncate text-white">
              {undoneFeedback}
            </p>
          </div>
          <button
            type="button"
            onClick={() => setUndoneFeedback(null)}
            className="p-1 text-emerald-400 hover:text-white rounded-lg hover:bg-emerald-800/40 transition-colors"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ) : activeUndo ? (
        <div className="bg-slate-900/95 text-white border border-slate-700/80 backdrop-blur-md rounded-2xl p-3.5 sm:p-4 shadow-2xl flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="p-2 rounded-xl bg-blue-500/20 text-blue-400 shrink-0 border border-blue-500/30">
              <Droplets className="w-4 h-4" />
            </div>
            <div className="min-w-0 pr-1">
              <p className="text-xs font-bold text-slate-100 truncate">
                {activeUndo.description}
              </p>
              <p className="text-[10px] text-slate-400 font-mono">
                Mis-pressed? Click Undo to restore to hamper
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            <button
              type="button"
              onClick={handleUndo}
              disabled={isUndoing}
              className="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 active:scale-95 text-white text-xs font-bold shadow-xs transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
              title="Undo this wash and restore to laundry"
            >
              {isUndoing ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <RotateCcw className="w-3.5 h-3.5" />
              )}
              <span>Undo</span>
            </button>

            <button
              type="button"
              onClick={handleDismiss}
              className="p-1.5 text-slate-400 hover:text-slate-200 rounded-lg hover:bg-slate-800 transition-colors cursor-pointer"
              title="Dismiss"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
};
