import React, { useState, useEffect } from 'react';
import { ClothesItem, ReportClothesJunction, WearingReport } from './types';
import { dataService, SyncStatus } from './services/dataService';
import { ClothesStudio } from './components/ClothesStudio';
import { LaundryCare } from './components/LaundryCare';
import { WearJournal } from './components/WearJournal';
import { AIAdvisor } from './components/AIAdvisor';
import { ClothesModal } from './components/ClothesModal';
import { LogOutfitModal } from './components/LogOutfitModal';
import { AddClothesModal } from './components/AddClothesModal';
import { WearingReportDetailModal } from './components/WearingReportDetailModal';
import { DrillDownReport } from './components/drilldown/DrillDownReport';
import { SyncModal } from './components/SyncModal';
import { SyncView } from './components/SyncView';
import { UndoToast } from './components/UndoToast';
import { AndroidInstallModal } from './components/AndroidInstallModal';
import { FontSizeSwitcher } from './components/FontSizeSwitcher';
import { mediaOutbox } from './services/mediaOutbox';
import { syncOutbox } from './services/syncOutbox';
import { useAiGenerationPaused } from './services/aiSettings';
import {
  Shirt, Droplets, BookOpen, Sparkles, Plus, RotateCcw,
  Cloud, CloudOff, RefreshCw, CheckCircle2, AlertCircle, Loader2, X, FolderTree,
  UploadCloud, DownloadCloud, FileDown, Database, Smartphone
} from 'lucide-react';

export function App() {
  const isAiPaused = useAiGenerationPaused();
  const [activeTab, setActiveTab] = useState<'closet' | 'laundry' | 'journal' | 'drilldown' | 'advisor' | 'sync'>('closet');
  const [clothes, setClothes] = useState<ClothesItem[]>(dataService.getClothes());
  const [reports, setReports] = useState<WearingReport[]>(dataService.getReports());
  const [junctions, setJunctions] = useState<ReportClothesJunction[]>(dataService.getJunctions());
  const [syncState, setSyncState] = useState<{
    status: SyncStatus;
    lastSync: string | null;
    pendingCount?: number;
  }>(dataService.getSyncStatus());

  // Cloud Sync & Toast state
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isSyncModalOpen, setIsSyncModalOpen] = useState(false);
  const [syncToast, setSyncToast] = useState<{ message: string; type: 'success' | 'info' | 'error' } | null>(null);
  const [mediaState, setMediaState] = useState(() => mediaOutbox.getState());
  const [isManualSyncing, setIsManualSyncing] = useState(false);

  // Modals state
  const [selectedClothes, setSelectedClothes] = useState<ClothesItem | null>(null);
  const [returnToClothes, setReturnToClothes] = useState<ClothesItem | null>(null);
  const [selectedWearingReport, setSelectedWearingReport] = useState<WearingReport | null>(null);
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [showAndroidInstall, setShowAndroidInstall] = useState(false);

  const showToast = (message: string, type: 'success' | 'info' | 'error' = 'success') => {
    setSyncToast({ message, type });
    setTimeout(() => {
      setSyncToast(null);
    }, 4000);
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await dataService.pullFromCloud();
      showToast('Database reloaded from disk', 'success');
    } catch (err: any) {
      console.error('Refresh error:', err);
      showToast('Refresh encountered an issue. Using cached memory data.', 'error');
    } finally {
      setIsRefreshing(false);
    }
  };

  // Subscribe to reactive dataService and mediaOutbox changes
  useEffect(() => {
    const unsubData = dataService.subscribe(() => {
      setClothes(dataService.getClothes());
      setReports(dataService.getReports());
      setJunctions(dataService.getJunctions());
      setSyncState(dataService.getSyncStatus());
    });
    const unsubMedia = mediaOutbox.subscribe((mState) => {
      setMediaState(mState);
    });
    return () => {
      unsubData();
      unsubMedia();
    };
  }, []);

  const docPendingCount = syncState.pendingCount || 0;
  const mediaPendingCount = mediaState.pendingCount || 0;
  const totalPendingCount = docPendingCount + mediaPendingCount;

  const handleManualSyncNow = async (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setIsManualSyncing(true);
    showToast('Saving pending items and photos to local disk...', 'info');
    try {
      await Promise.all([
        syncOutbox.flush(),
        mediaOutbox.processQueue()
      ]);
      showToast('All items and photos saved to local disk successfully!', 'success');
    } catch (err: any) {
      showToast('Save error: ' + (err?.message || 'Storage error'), 'error');
    } finally {
      setIsManualSyncing(false);
    }
  };

  // Prevent background scrolling and clicking behind screen when any modal is open
  const isAnyModalOpen = Boolean(selectedClothes || isLogModalOpen || isAddModalOpen || selectedWearingReport || isSyncModalOpen || showAndroidInstall);
  useEffect(() => {
    if (isAnyModalOpen) {
      document.body.style.overflow = 'hidden';
      document.body.style.touchAction = 'none';
    } else {
      document.body.style.overflow = '';
      document.body.style.touchAction = 'pan-y';
    }
    return () => {
      document.body.style.overflow = '';
      document.body.style.touchAction = 'pan-y';
    };
  }, [isAnyModalOpen]);

  // Update selectedClothes if clothes array changes
  useEffect(() => {
    if (selectedClothes) {
      const updated = clothes.find(c => c.id === selectedClothes.id);
      if (updated) setSelectedClothes(updated);
    }
  }, [clothes]);

  // Update selectedWearingReport if reports array changes
  useEffect(() => {
    if (selectedWearingReport) {
      const updated = reports.find(r => r.id === selectedWearingReport.id);
      if (updated) setSelectedWearingReport(updated);
    }
  }, [reports]);

  // Derived metrics using exact DB wash stated
  const needWashCount = clothes.filter(c => dataService.getGarmentStats(c.id).needWash).length;

  const handleMarkWashed = (clothesId: string, customWashDate?: string) => {
    dataService.markGarmentWashed(clothesId, customWashDate);
  };

  const handleMarkAllWashed = (customWashDate?: string) => {
    dataService.markAllLaundryWashed(customWashDate);
  };

  const handleUpdateClothes = (clothesId: string, updates: Partial<ClothesItem>) => {
    dataService.updateGarment(clothesId, updates);
  };

  const handleDeleteClothes = async (clothesId: string) => {
    await dataService.deleteGarment(clothesId);
    if (selectedClothes?.id === clothesId) {
      setSelectedClothes(null);
    }
  };

  const handleDeleteReport = async (reportId: string) => {
    await dataService.deleteWearingReport(reportId);
    if (selectedWearingReport?.id === reportId) {
      setSelectedWearingReport(null);
    }
  };

  const handleUpdateReport = async (report: WearingReport) => {
    await dataService.updateWearingReport(report.id, report);
  };

  const handleAddClothes = async (item: Omit<ClothesItem, 'id'>) => {
    const created = await dataService.addGarment(item);
    setSelectedClothes(created);
  };

  const handleSaveReport = async (params: {
    'Name': string;
    'wear date': string;
    clothesItems: Array<{ clothesId: string; durationHours: number; washStated: 'wash' | 'need wash'; washDate?: string }>;
    'shoulder feel'?: string;
    'mid breast'?: string;
    'night mid breast'?: string;
    'sweat'?: string;
    'night sweat'?: string;
    'leg feel'?: string;
    'night leg feel'?: string;
    'report detail'?: string;
    'tags'?: string[];
    'Pictures'?: any[];
    fieldExplanations?: Record<string, string>;
  }) => {
    const existingCustom: Record<string, any> = {};
    if (params.fieldExplanations) {
      Object.entries(params.fieldExplanations).forEach(([k, v]) => {
        if (v && v.trim()) existingCustom[`${k}_explain`] = v.trim();
      });
      existingCustom._fieldExplanations = params.fieldExplanations;
    }

    await dataService.addWearingReport(
      {
        'Name': params['Name'],
        'wear date': params['wear date'],
        'shoulder feel': params['shoulder feel'],
        'mid breast': params['mid breast'],
        'night mid breast': params['night mid breast'],
        'sweat': params['sweat'],
        'night sweat': params['night sweat'],
        'leg feel': params['leg feel'],
        'night leg feel': params['night leg feel'],
        'report detail': params['report detail'],
        'tags': params['tags'] || ['#journal'],
        'Pictures': params['Pictures'] || [],
        fieldExplanations: params.fieldExplanations || {},
        customFields: existingCustom,
        extraAttributes: existingCustom
      },
      params.clothesItems
    );
  };

  const handleRefreshCloud = async () => {
    await dataService.pullFromCloud();
  };

  const handleManualSync = () => {
    dataService.pullFromCloud();
  };

  const selectedClothesStats = selectedClothes
    ? dataService.getGarmentStats(selectedClothes.id)
    : { needWash: false, unwashedSeconds: 0, unwashedHours: '0', totalWears: 0, totalDurationHours: 0 };

  return (
    <div className="w-full max-w-full overflow-x-hidden min-h-[100dvh] bg-slate-100/70 text-slate-900 flex flex-col font-sans selection:bg-purple-100 selection:text-purple-900 touch-pan-y">
      {/* Top Header */}
      <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200/80 shrink-0 select-none w-full max-w-full pt-[env(safe-area-inset-top,0px)]">
        <div className="w-full px-2.5 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14 sm:h-16 gap-1.5 sm:gap-2">
            {/* Brand Logo & Name */}
            <div 
              onClick={() => setActiveTab('closet')}
              className="flex items-center gap-1.5 sm:gap-2.5 cursor-pointer min-w-0"
            >
              <div className="w-8 h-8 sm:w-9 sm:h-9 rounded-xl bg-gradient-to-br from-purple-700 to-indigo-800 text-white flex items-center justify-center shadow-xs shrink-0">
                <Shirt className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <span className="font-bold text-slate-900 text-xs sm:text-base tracking-tight block leading-tight truncate">
                  Wardrobe Journal
                </span>
                <span className="text-[10px] text-slate-500 font-mono tracking-wide truncate hidden sm:block">
                  100% Local Application · Disk Persistence
                </span>
              </div>
            </div>

            {/* Desktop Navigation Tabs */}
            <nav className="hidden md:flex items-center gap-1 p-1 bg-slate-100/90 rounded-2xl border border-slate-200/60 text-xs font-semibold">
              <button
                onClick={() => setActiveTab('closet')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'closet'
                    ? 'bg-purple-600 text-white shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Shirt className={`w-3.5 h-3.5 ${activeTab === 'closet' ? 'text-white' : 'text-purple-600'}`} />
                <span>Closet ({clothes.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('laundry')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all relative ${
                  activeTab === 'laundry'
                    ? 'bg-blue-600 text-white shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Droplets className={`w-3.5 h-3.5 ${activeTab === 'laundry' ? 'text-white' : 'text-blue-500'}`} />
                <span>Laundry Care</span>
                {needWashCount > 0 && (
                  <span className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold shadow-xs ${
                    activeTab === 'laundry' ? 'bg-white text-blue-700' : 'bg-amber-500 text-white'
                  }`}>
                    {needWashCount}
                  </span>
                )}
              </button>

              <button
                onClick={() => setActiveTab('journal')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'journal'
                    ? 'bg-indigo-600 text-white shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <BookOpen className={`w-3.5 h-3.5 ${activeTab === 'journal' ? 'text-white' : 'text-indigo-500'}`} />
                <span>Wear Journal ({reports.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('drilldown')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'drilldown'
                    ? 'bg-emerald-600 text-white shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <FolderTree className={`w-3.5 h-3.5 ${activeTab === 'drilldown' ? 'text-white' : 'text-emerald-600'}`} />
                <span>Drill-Down Report</span>
              </button>

              <button
                onClick={() => setActiveTab('advisor')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'advisor'
                    ? 'bg-amber-600 text-white shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Sparkles className={`w-3.5 h-3.5 ${activeTab === 'advisor' ? 'text-white' : 'text-amber-500'}`} />
                <span>AI Styling</span>
                {isAiPaused && (
                  <span className={`text-[9px] px-1.5 py-0.2 rounded-full font-bold uppercase tracking-wider ${
                    activeTab === 'advisor' ? 'bg-amber-800 text-amber-100' : 'bg-amber-100 text-amber-900 border border-amber-300'
                  }`}>
                    Paused
                  </span>
                )}
              </button>

              <button
                onClick={() => setActiveTab('sync')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'sync'
                    ? 'bg-purple-700 text-white shadow-xs font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Database className={`w-3.5 h-3.5 ${activeTab === 'sync' ? 'text-white' : 'text-purple-600'}`} />
                <span>Local Storage</span>
              </button>
            </nav>

            {/* Status Indicator Pill / Local Storage Link & Action Buttons */}
            <div className="flex items-center gap-1 sm:gap-2 shrink-0">
              {/* App Font Size Switcher */}
              <FontSizeSwitcher />

              {/* Android (Moto Edge 30) Install Button */}
              <button
                type="button"
                onClick={() => setShowAndroidInstall(true)}
                title="安装到 Moto Edge 30 Android 手机"
                className="flex items-center gap-1 px-2 sm:px-3 py-1.5 rounded-xl text-[11px] font-bold border border-emerald-300 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 shadow-2xs transition-all active:scale-95 cursor-pointer"
              >
                <Smartphone className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                <span className="hidden sm:inline">Moto Edge 30</span>
                <span className="sm:hidden text-[10px]">App</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('sync')}
                title={
                  totalPendingCount > 0
                    ? `${totalPendingCount} changes pending disk write · Click to view Local Storage`
                    : '100% Local Storage · All Data Saved to Disk · Click to open Storage Manager'
                }
                className={`flex items-center gap-1 px-2 sm:px-2.5 py-1.5 rounded-xl text-[11px] font-mono font-medium border shadow-2xs transition-all active:scale-95 cursor-pointer ${
                  totalPendingCount > 0
                    ? 'border-amber-300 bg-amber-50 text-amber-900'
                    : 'border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700'
                }`}
              >
                <span
                  className={`w-2 h-2 rounded-full shrink-0 ${
                    totalPendingCount > 0
                      ? 'bg-amber-500 animate-pulse ring-2 ring-amber-200'
                      : syncState.status === 'synced'
                      ? 'bg-emerald-500 ring-2 ring-emerald-200'
                      : syncState.status === 'syncing'
                      ? 'bg-purple-500 animate-pulse'
                      : 'bg-slate-400'
                  }`}
                />
                <span className="font-sans font-semibold text-xs hidden xs:inline sm:inline">
                  {totalPendingCount > 0
                    ? `${totalPendingCount} Pending`
                    : syncState.status === 'synced'
                    ? 'All Synced'
                    : syncState.status === 'syncing'
                    ? 'Syncing...'
                    : 'Offline Safe'}
                </span>
              </button>

              {/* Instant Manual "Sync Now" Button when changes or photos are pending */}
              {totalPendingCount > 0 && (
                <button
                  type="button"
                  onClick={handleManualSyncNow}
                  disabled={isManualSyncing}
                  title="Save all pending local changes and photo uploads to local disk right now"
                  className="flex items-center gap-1.5 px-2.5 sm:px-3 py-1.5 rounded-xl text-xs font-semibold bg-purple-700 hover:bg-purple-800 text-white shadow-xs transition-all active:scale-95 cursor-pointer disabled:opacity-60"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${isManualSyncing ? 'animate-spin' : ''}`} />
                  <span className="hidden sm:inline">Sync Now</span>
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Sync Toast Notification Banner */}
        {syncToast && (
          <div
            className={`w-full py-2 px-4 text-xs font-semibold text-center border-t transition-all flex items-center justify-center gap-2 ${
              syncToast.type === 'success'
                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                : syncToast.type === 'error'
                ? 'bg-rose-50 text-rose-800 border-rose-200'
                : 'bg-blue-50 text-blue-800 border-blue-200'
            }`}
          >
            {syncToast.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            )}
            <span>{syncToast.message}</span>
          </div>
        )}
      </header>

      {/* Main Full-Screen Content Area */}
      <main className="flex-1 w-full px-2.5 sm:px-4 md:px-6 lg:px-8 py-3 sm:py-5 pb-20 md:pb-6 overflow-x-hidden flex flex-col">
        {activeTab === 'closet' && (
          <ClothesStudio
            clothes={clothes}
            junctions={junctions}
            onSelectClothes={(item) => setSelectedClothes(item)}
            onOpenAddModal={() => setIsAddModalOpen(true)}
          />
        )}

        {activeTab === 'laundry' && (
          <LaundryCare
            clothes={clothes}
            junctions={junctions}
            onMarkWashed={handleMarkWashed}
            onMarkAllWashed={handleMarkAllWashed}
            onSelectClothes={(item) => setSelectedClothes(item)}
          />
        )}

        {activeTab === 'journal' && (
          <WearJournal
            reports={reports}
            junctions={junctions}
            clothes={clothes}
            onOpenLogModal={() => setIsLogModalOpen(true)}
            onSelectClothes={(item) => setSelectedClothes(item)}
            selectedReport={selectedWearingReport}
            onSelectReport={setSelectedWearingReport}
            onDeleteReport={handleDeleteReport}
            onUpdateReport={handleUpdateReport}
          />
        )}

        {activeTab === 'drilldown' && (
          <DrillDownReport
            clothes={clothes}
            reports={reports}
            junctions={junctions}
            onSelectClothes={(item) => setSelectedClothes(item)}
            onSelectReport={(report) => {
              setSelectedWearingReport(report);
              setActiveTab('journal');
            }}
          />
        )}

        {activeTab === 'advisor' && (
          <AIAdvisor
            clothes={clothes}
            reports={reports}
            junctions={junctions}
            onSelectClothes={(item) => setSelectedClothes(item)}
            onSelectReport={(report) => {
              setSelectedWearingReport(report);
              setActiveTab('journal');
            }}
          />
        )}

        {activeTab === 'sync' && (
          <SyncView
            syncStatus={syncState.status}
            clothesCount={clothes.length}
            reportsCount={reports.length}
            junctionsCount={junctions.length}
            onRefresh={handleRefresh}
            isRefreshing={isRefreshing}
          />
        )}
      </main>

      {/* Mobile-First Fixed Bottom Navigation Bar */}
      <nav
        id="mobile-bottom-navigation"
        className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200/90 py-1 px-1 pb-[max(0.35rem,env(safe-area-inset-bottom))] flex items-center justify-between shadow-lg select-none w-full max-w-full overflow-hidden"
      >
        <button
          onClick={() => setActiveTab('closet')}
          className={`flex-1 min-w-0 flex flex-col items-center justify-center py-1.5 px-0.5 rounded-xl transition-all ${
            activeTab === 'closet' ? 'text-purple-700 font-bold bg-purple-50/80' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Shirt className="w-5 h-5 shrink-0" />
          <span className="text-[10px] leading-tight truncate mt-0.5">Closet</span>
        </button>

        <button
          onClick={() => setActiveTab('laundry')}
          className={`flex-1 min-w-0 flex flex-col items-center justify-center py-1.5 px-0.5 rounded-xl transition-all relative ${
            activeTab === 'laundry' ? 'text-blue-700 font-bold bg-blue-50/80' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <div className="relative">
            <Droplets className="w-5 h-5 shrink-0" />
            {needWashCount > 0 && (
              <span className="absolute -top-1 -right-2 px-1 rounded-full text-[9px] font-bold bg-amber-500 text-white">
                {needWashCount}
              </span>
            )}
          </div>
          <span className="text-[10px] leading-tight truncate mt-0.5">Laundry</span>
        </button>

        <button
          onClick={() => setActiveTab('journal')}
          className={`flex-1 min-w-0 flex flex-col items-center justify-center py-1.5 px-0.5 rounded-xl transition-all ${
            activeTab === 'journal' ? 'text-indigo-700 font-bold bg-indigo-50/80' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <BookOpen className="w-5 h-5 shrink-0" />
          <span className="text-[10px] leading-tight truncate mt-0.5">Journal</span>
        </button>

        <button
          onClick={() => setActiveTab('drilldown')}
          className={`flex-1 min-w-0 flex flex-col items-center justify-center py-1.5 px-0.5 rounded-xl transition-all ${
            activeTab === 'drilldown' ? 'text-emerald-700 font-bold bg-emerald-50/80' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <FolderTree className="w-5 h-5 shrink-0" />
          <span className="text-[10px] leading-tight truncate mt-0.5">Reports</span>
        </button>

        <button
          onClick={() => setActiveTab('advisor')}
          className={`flex-1 min-w-0 flex flex-col items-center justify-center py-1.5 px-0.5 rounded-xl transition-all ${
            activeTab === 'advisor' ? 'text-amber-700 font-bold bg-amber-50/80' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Sparkles className="w-5 h-5 shrink-0" />
          <span className="text-[10px] leading-tight truncate mt-0.5">
            {isAiPaused ? 'AI (Paused)' : 'AI Advisor'}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('sync')}
          className={`flex-1 min-w-0 flex flex-col items-center justify-center py-1.5 px-0.5 rounded-xl transition-all ${
            activeTab === 'sync' ? 'text-purple-800 font-bold bg-purple-50/80' : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Database className="w-5 h-5 shrink-0" />
          <span className="text-[10px] leading-tight truncate mt-0.5">Storage</span>
        </button>
      </nav>

      {/* Desktop / Tablet Footer */}
      <footer className="hidden md:block bg-white border-t border-slate-200/80 py-4 text-xs text-slate-500 shrink-0 w-full">
        <div className="w-full px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-800">Wardrobe &amp; Wear Journal</span>
            <span>·</span>
            <span>{clothes.length} clothes</span>
            <span>·</span>
            <span>{reports.length} wearing reports</span>
            <span>·</span>
            <span>{junctions.length} clothes junctions</span>
            <span>·</span>
            <span className="text-emerald-700 font-mono">100% Local Storage</span>
          </div>

          <div className="flex items-center gap-4">
            <button
              onClick={handleRefreshCloud}
              className="text-slate-400 hover:text-indigo-600 flex items-center gap-1 transition-colors"
              title="Reload latest data from local disk"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Reload from Disk
            </button>
          </div>
        </div>
      </footer>

      {/* Modals with event stop and backdrop barrier */}
      {selectedClothes && (
        <ClothesModal
          clothes={selectedClothes}
          stats={selectedClothesStats}
          junctions={junctions}
          reports={reports}
          onClose={() => {
            setSelectedClothes(null);
            setReturnToClothes(null);
          }}
          onMarkWashed={handleMarkWashed}
          onUpdateClothes={handleUpdateClothes}
          onDeleteClothes={handleDeleteClothes}
          onSelectReport={(report) => {
            setReturnToClothes(selectedClothes);
            setSelectedClothes(null);
            setSelectedWearingReport(report);
          }}
        />
      )}

      {isLogModalOpen && (
        <LogOutfitModal
          clothes={clothes}
          isOpen={isLogModalOpen}
          onClose={() => setIsLogModalOpen(false)}
          onSaveReport={handleSaveReport}
        />
      )}

      {isAddModalOpen && (
        <AddClothesModal
          isOpen={isAddModalOpen}
          onClose={() => setIsAddModalOpen(false)}
          onAddClothes={handleAddClothes}
        />
      )}

      {selectedWearingReport && (
        <WearingReportDetailModal
          report={selectedWearingReport}
          junctions={junctions}
          clothes={clothes}
          onClose={() => {
            setSelectedWearingReport(null);
            if (returnToClothes) {
              setSelectedClothes(returnToClothes);
              setReturnToClothes(null);
            }
          }}
          onSelectClothes={(item) => {
            setSelectedWearingReport(null);
            setReturnToClothes(null);
            setSelectedClothes(item);
          }}
          onDeleteReport={handleDeleteReport}
          onUpdateReport={handleUpdateReport}
        />
      )}

      {/* Cloud Synchronization Dialog */}
      {isSyncModalOpen && (
        <SyncModal
          isOpen={isSyncModalOpen}
          onClose={() => setIsSyncModalOpen(false)}
          clothesCount={clothes.length}
          reportsCount={reports.length}
          junctionsCount={junctions.length}
        />
      )}

      {/* Android (Moto Edge 30) Install & Capacitor Modal */}
      {showAndroidInstall && (
        <AndroidInstallModal
          isOpen={showAndroidInstall}
          onClose={() => setShowAndroidInstall(false)}
        />
      )}

      {/* Global Wash Undo Toast */}
      <UndoToast />
    </div>
  );
}

export default App;
