import React, { useState, useRef } from 'react';
import {
  Cloud,
  CheckCircle2,
  AlertCircle,
  X,
  Loader2,
  RefreshCw,
  Database,
  Calendar,
  Shirt,
  Sparkles,
  FileDown,
  FileUp,
  Copy,
  Check,
  ArrowLeft
} from 'lucide-react';
import { dataService } from '../services/dataService';

interface SyncModalProps {
  isOpen: boolean;
  onClose: () => void;
  clothesCount: number;
  reportsCount: number;
  junctionsCount: number;
}

export const SyncModal: React.FC<SyncModalProps> = ({
  isOpen,
  onClose,
  clothesCount,
  reportsCount,
  junctionsCount
}) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [currentAction, setCurrentAction] = useState<'export' | 'import' | null>(null);
  const [progressStep, setProgressStep] = useState<string>('');
  const [progressPercent, setProgressPercent] = useState<number>(0);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [copiedNotification, setCopiedNotification] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleExportJson = () => {
    try {
      const res = dataService.exportCompleteDatabaseJson('initialData.json');
      setStatusMessage({
        type: 'success',
        text: `Downloaded initialData.json with ${res.clothesCount} garments, ${res.reportsCount} wearing reports, and ${res.junctionsCount} junction rows!`
      });
    } catch (err: any) {
      setStatusMessage({
        type: 'error',
        text: 'Failed to export backup file.'
      });
    }
  };

  const handleCopyJson = () => {
    try {
      const res = dataService.exportCompleteDatabaseJson();
      navigator.clipboard.writeText(res.jsonString);
      setCopiedNotification(true);
      setTimeout(() => setCopiedNotification(false), 2000);
    } catch (err) {
      console.warn('Clipboard copy error:', err);
    }
  };

  const handleImportFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsProcessing(true);
    setCurrentAction('import');
    setProgressStep('Reading and restoring backup file...');
    setProgressPercent(40);

    try {
      const text = await file.text();
      const res = await dataService.importDatabaseFromJson(text);
      setProgressPercent(100);
      setStatusMessage({
        type: 'success',
        text: `Database successfully restored! Loaded ${res.clothes} garments, ${res.reports} wearing reports, ${res.junctions} junction rows, and ${res.presets || 0} drill-down presets.`
      });
    } catch (err: any) {
      setStatusMessage({
        type: 'error',
        text: err?.message || 'Failed to parse and import backup file.'
      });
    } finally {
      setIsProcessing(false);
      setCurrentAction(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const syncStatus = dataService.getSyncStatus();

  return (
    <div
      id="sync-modal-fullscreen"
      className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto"
    >
      {/* Full-Screen Top Header */}
      <div className="px-3 sm:px-6 py-2.5 sm:py-3 border-b border-slate-200/80 bg-gradient-to-r from-slate-900 to-indigo-950 text-white flex items-center justify-between shrink-0 shadow-xs w-full max-w-full overflow-x-hidden">
        <div className="flex items-center gap-2 sm:gap-3 min-w-0">
          <button
            type="button"
            onClick={onClose}
            disabled={isProcessing}
            className="p-1.5 -ml-1 sm:ml-0 rounded-xl text-white/80 hover:text-white hover:bg-white/10 transition-colors disabled:opacity-30 shrink-0"
            title="Back"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-2xl bg-indigo-500/30 border border-indigo-400/40 flex items-center justify-center text-indigo-300 shrink-0">
            <Database className="w-4 h-4 sm:w-5 sm:h-5" />
          </div>
          <div className="min-w-0">
            <h3 className="text-sm sm:text-base font-bold tracking-tight text-white truncate">
              Offline Backup &amp; JSON Manager
            </h3>
            <p className="text-[10px] sm:text-xs text-indigo-200 truncate hidden xs:block">
              100% Local Application — Export and restore JSON backups
            </p>
          </div>
        </div>
        <button
          onClick={onClose}
          disabled={isProcessing}
          className="p-1.5 rounded-xl hover:bg-white/10 text-white/70 hover:text-white transition-colors disabled:opacity-30 shrink-0"
          aria-label="Close"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Modal Body */}
      <div className="p-3.5 sm:p-6 space-y-5 overflow-y-auto overflow-x-hidden flex-1 w-full max-w-full">
        {/* Local Stats Banner */}
        <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <span
              className="w-2.5 h-2.5 rounded-full bg-emerald-500 ring-4 ring-emerald-100"
            />
            <div>
              <p className="text-xs font-bold text-slate-800">
                Storage Status:{' '}
                <span className="capitalize text-emerald-700 font-semibold">
                  100% Local & Synced
                </span>
              </p>
              <p className="text-[11px] text-slate-500">
                {`Last active: ${syncStatus.lastSync || 'Just now'} • Local-first storage`}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3 text-xs font-mono text-slate-600">
            <span className="flex items-center gap-1">
              <Shirt className="w-3.5 h-3.5 text-purple-600" />
              {clothesCount} clothes
            </span>
            <span className="flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5 text-indigo-600" />
              {reportsCount} reports
            </span>
          </div>
        </div>

        {/* Live Progress Bar when importing */}
        {isProcessing && (
          <div className="p-4 rounded-2xl bg-purple-50 border border-purple-200 space-y-2">
            <div className="flex items-center justify-between text-xs font-bold text-purple-900">
              <span className="flex items-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin text-purple-600" />
                {progressStep || 'Processing backup file...'}
              </span>
              <span className="font-mono text-purple-700">{progressPercent}%</span>
            </div>
            <div className="w-full bg-purple-200 rounded-full h-2.5 overflow-hidden">
              <div
                className="bg-gradient-to-r from-purple-600 to-indigo-600 h-2.5 rounded-full transition-all duration-300"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </div>
        )}

        {/* Status Message Notification */}
        {statusMessage && (
          <div
            className={`p-3.5 rounded-2xl text-xs font-semibold flex items-start gap-2.5 ${
              statusMessage.type === 'success'
                ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                : 'bg-rose-50 text-rose-800 border border-rose-200'
            }`}
          >
            {statusMessage.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            )}
            <div className="flex-1">{statusMessage.text}</div>
          </div>
        )}

        {/* Local Backup & Export (initialData.json) */}
        <div className="p-4 rounded-2xl border-2 border-slate-200 bg-gradient-to-b from-slate-50/70 to-white transition-all space-y-3">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-slate-800 text-white flex items-center justify-center shrink-0 shadow-xs">
                <Database className="w-4 h-4" />
              </div>
              <div>
                <h4 className="font-bold text-slate-900 text-sm">Offline Backup &amp; initialData.json</h4>
                <p className="text-xs text-slate-500 leading-relaxed mt-0.5">
                  Download all your data as a single JSON file. You can put this into Android Studio under <code className="px-1.5 py-0.5 rounded bg-slate-100 font-mono text-[11px] text-slate-800">assets/initialData.json</code> for instant offline loading.
                </p>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-1">
            {/* Download initialData.json */}
            <button
              type="button"
              onClick={handleExportJson}
              disabled={isProcessing}
              className="py-2.5 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-all shadow-md shadow-slate-200 flex items-center justify-center gap-1.5 disabled:opacity-50 active:scale-98 cursor-pointer"
            >
              <FileDown className="w-3.5 h-3.5 text-emerald-400" />
              <span>Download initialData.json</span>
            </button>

            {/* Import from Backup File */}
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isProcessing}
              className="py-2.5 px-3 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 text-xs font-bold transition-all flex items-center justify-center gap-1.5 disabled:opacity-50 active:scale-98 cursor-pointer"
            >
              <FileUp className="w-3.5 h-3.5 text-indigo-600" />
              <span>Restore from JSON File</span>
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept=".json"
              onChange={handleImportFileChange}
              className="hidden"
            />
          </div>

          <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1 border-t border-slate-100">
            <span className="font-mono">
              {clothesCount} garments · {reportsCount} reports · {junctionsCount} junctions
            </span>
            <button
              type="button"
              onClick={handleCopyJson}
              className="text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1 cursor-pointer"
            >
              {copiedNotification ? (
                <>
                  <Check className="w-3 h-3 text-emerald-600" />
                  <span className="text-emerald-600">Copied!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3 h-3" />
                  <span>Copy Raw JSON</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Full-Screen Sticky Footer */}
      <div className="px-4 sm:px-6 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] bg-slate-50 border-t border-slate-200/80 flex items-center justify-between shrink-0 w-full max-w-full shadow-xs">
        <span className="text-xs text-slate-500 hidden sm:inline">All wardrobe data saved directly to local storage</span>
        <button
          type="button"
          onClick={onClose}
          disabled={isProcessing}
          className="ml-auto px-4 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 text-xs font-bold transition-all disabled:opacity-50 shadow-2xs cursor-pointer"
        >
          Close
        </button>
      </div>
    </div>
  );
};
