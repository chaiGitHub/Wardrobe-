import React, { useState, useEffect } from 'react';
import { ClothesItem, ReportClothesJunction, WearingReport } from './types';
import { dataService, SyncStatus } from './services/dataService';
import { WardrobeStudio } from './components/WardrobeStudio';
import { LaundryCare } from './components/LaundryCare';
import { WearJournal } from './components/WearJournal';
import { AIAdvisor } from './components/AIAdvisor';
import { GarmentModal } from './components/GarmentModal';
import { LogOutfitModal } from './components/LogOutfitModal';
import { AddGarmentModal } from './components/AddGarmentModal';
import { SessionDetailModal } from './components/SessionDetailModal';
import { DrillDownReport } from './components/drilldown/DrillDownReport';
import {
  Shirt, Droplets, BookOpen, Sparkles, Plus, RotateCcw,
  Cloud, CloudOff, RefreshCw, UploadCloud, DownloadCloud, CheckCircle2, AlertCircle, Loader2, X, FolderTree
} from 'lucide-react';

export function App() {
  const [activeTab, setActiveTab] = useState<'closet' | 'laundry' | 'journal' | 'drilldown' | 'advisor'>('closet');
  const [clothes, setClothes] = useState<ClothesItem[]>(dataService.getClothes());
  const [reports, setReports] = useState<WearingReport[]>(dataService.getReports());
  const [junctions, setJunctions] = useState<ReportClothesJunction[]>(dataService.getJunctions());
  const [syncState, setSyncState] = useState<{ status: SyncStatus; lastSync: string | null }>(dataService.getSyncStatus());

  // Two distinct sync button states & Toast
  const [isPushing, setIsPushing] = useState(false);
  const [isPulling, setIsPulling] = useState(false);
  const [syncToast, setSyncToast] = useState<{ message: string; type: 'success' | 'info' | 'error' } | null>(null);

  // Real-time Sync Progress State
  const [syncProgress, setSyncProgress] = useState<{
    active: boolean;
    type: 'push' | 'pull';
    step: string;
    percent: number;
    completed: boolean;
  } | null>(null);

  // Modals state
  const [selectedGarment, setSelectedGarment] = useState<ClothesItem | null>(null);
  const [selectedSessionReport, setSelectedSessionReport] = useState<WearingReport | null>(null);
  const [isLogModalOpen, setIsLogModalOpen] = useState(false);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  const showToast = (message: string, type: 'success' | 'info' | 'error' = 'success') => {
    setSyncToast({ message, type });
    setTimeout(() => {
      setSyncToast(null);
    }, 4000);
  };

  const handlePushToCloud = async () => {
    setIsPushing(true);
    setSyncProgress({
      active: true,
      type: 'push',
      step: 'Connecting to Firestore...',
      percent: 5,
      completed: false
    });

    try {
      const res = await dataService.pushToCloud((step, percent) => {
        setSyncProgress({
          active: true,
          type: 'push',
          step,
          percent,
          completed: percent >= 100
        });
      });

      setSyncProgress({
        active: true,
        type: 'push',
        step: `Push completed! Uploaded ${res.clothes} garments, ${res.reports} reports.`,
        percent: 100,
        completed: true
      });
      showToast(`Pushed ${res.clothes} garments and ${res.reports} reports to Firestore!`, 'success');

      setTimeout(() => {
        setSyncProgress(prev => (prev?.completed ? null : prev));
      }, 3500);
    } catch (err: any) {
      console.error('Push error:', err);
      setSyncProgress({
        active: true,
        type: 'push',
        step: 'Push failed: ' + (err.message || 'Check network / Firestore rules'),
        percent: 100,
        completed: false
      });
      showToast('Push to Firestore failed. Check network or security rules.', 'error');
      setTimeout(() => setSyncProgress(null), 4000);
    } finally {
      setIsPushing(false);
    }
  };

  const handlePullFromCloud = async () => {
    setIsPulling(true);
    setSyncProgress({
      active: true,
      type: 'pull',
      step: 'Connecting to Firestore...',
      percent: 5,
      completed: false
    });

    try {
      const res = await dataService.pullFromCloud((step, percent) => {
        setSyncProgress({
          active: true,
          type: 'pull',
          step,
          percent,
          completed: percent >= 100
        });
      });

      setSyncProgress({
        active: true,
        type: 'pull',
        step: `Pull completed! Loaded ${res.clothes} garments, ${res.reports} reports.`,
        percent: 100,
        completed: true
      });
      showToast(`Pulled ${res.clothes} garments and ${res.reports} reports from Firestore!`, 'success');

      setTimeout(() => {
        setSyncProgress(prev => (prev?.completed ? null : prev));
      }, 3500);
    } catch (err: any) {
      console.error('Pull error:', err);
      setSyncProgress({
        active: true,
        type: 'pull',
        step: 'Pull failed: ' + (err.message || 'Check network / Firestore rules'),
        percent: 100,
        completed: false
      });
      showToast('Pull from Firestore failed. Check network or security rules.', 'error');
      setTimeout(() => setSyncProgress(null), 4000);
    } finally {
      setIsPulling(false);
    }
  };

  // Subscribe to reactive dataService changes
  useEffect(() => {
    const unsubscribe = dataService.subscribe(() => {
      setClothes(dataService.getClothes());
      setReports(dataService.getReports());
      setJunctions(dataService.getJunctions());
      setSyncState(dataService.getSyncStatus());
    });
    return unsubscribe;
  }, []);

  // Prevent background scrolling and clicking behind screen when any modal is open
  const isAnyModalOpen = Boolean(selectedGarment || isLogModalOpen || isAddModalOpen || selectedSessionReport);
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

  // Update selectedGarment if clothes array changes
  useEffect(() => {
    if (selectedGarment) {
      const updated = clothes.find(c => c.id === selectedGarment.id);
      if (updated) setSelectedGarment(updated);
    }
  }, [clothes]);

  // Update selectedSessionReport if reports array changes
  useEffect(() => {
    if (selectedSessionReport) {
      const updated = reports.find(r => r.id === selectedSessionReport.id);
      if (updated) setSelectedSessionReport(updated);
    }
  }, [reports]);

  // Derived metrics using exact DB wash stated
  const needWashCount = clothes.filter(c => dataService.getGarmentStats(c.id).needWash).length;

  const handleMarkWashed = (clothesId: string) => {
    dataService.markGarmentWashed(clothesId);
  };

  const handleMarkAllWashed = () => {
    dataService.markAllLaundryWashed();
  };

  const handleUpdateGarment = (clothesId: string, updates: Partial<ClothesItem>) => {
    dataService.updateGarment(clothesId, updates);
  };

  const handleDeleteGarment = async (clothesId: string) => {
    await dataService.deleteGarment(clothesId);
    if (selectedGarment?.id === clothesId) {
      setSelectedGarment(null);
    }
  };

  const handleDeleteReport = async (reportId: string) => {
    await dataService.deleteWearingReport(reportId);
    if (selectedSessionReport?.id === reportId) {
      setSelectedSessionReport(null);
    }
  };

  const handleUpdateReport = async (report: WearingReport) => {
    await dataService.updateWearingReport(report.id, report);
  };

  const handleAddGarment = async (item: Omit<ClothesItem, 'id'>) => {
    const created = await dataService.addGarment(item);
    setSelectedGarment(created);
  };

  const handleSaveReport = async (params: {
    'Name': string;
    'wear date': string;
    clothesItems: Array<{ clothesId: string; durationHours: number; washStated: 'wash' | 'need wash' }>;
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
  }) => {
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
        'Pictures': params['Pictures'] || []
      },
      params.clothesItems
    );
  };

  const handleResetSeed = () => {
    if (window.confirm('Reset database back to the base records?')) {
      dataService.resetToSeed();
    }
  };

  const handleManualSync = () => {
    dataService.syncWithFirestore();
  };

  const selectedGarmentStats = selectedGarment
    ? dataService.getGarmentStats(selectedGarment.id)
    : { needWash: false, unwashedSeconds: 0, unwashedHours: '0', totalWears: 0, totalDurationHours: 0 };

  return (
    <div className="w-full max-w-full overflow-x-hidden min-h-[100dvh] bg-slate-100/70 text-slate-900 flex flex-col font-sans selection:bg-purple-100 selection:text-purple-900 touch-pan-y">
      {/* Top Header */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/80 shrink-0 select-none">
        <div className="w-full max-w-7xl mx-auto px-3.5 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-14 sm:h-16 gap-2">
            {/* Brand Logo & Name */}
            <div 
              onClick={() => setActiveTab('closet')}
              className="flex items-center gap-2.5 cursor-pointer min-w-0"
            >
              <div className="w-9 h-9 rounded-2xl bg-gradient-to-br from-purple-700 to-indigo-800 text-white flex items-center justify-center shadow-xs shrink-0">
                <Shirt className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <span className="font-bold text-slate-900 text-sm sm:text-base tracking-tight block leading-tight truncate">
                  Wardrobe &amp; Wear Journal
                </span>
                <span className="text-[10px] text-slate-500 font-mono tracking-wide truncate block">
                  Local-First · Google Firestore
                </span>
              </div>
            </div>

            {/* Desktop Navigation Tabs */}
            <nav className="hidden md:flex items-center gap-1 p-1 bg-slate-100/90 rounded-2xl border border-slate-200/60 text-xs font-semibold">
              <button
                onClick={() => setActiveTab('closet')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'closet'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Shirt className="w-3.5 h-3.5 text-purple-600" />
                <span>Closet ({clothes.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('laundry')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all relative ${
                  activeTab === 'laundry'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Droplets className="w-3.5 h-3.5 text-blue-500" />
                <span>Laundry Care</span>
                {needWashCount > 0 && (
                  <span className="px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-amber-500 text-white shadow-xs">
                    {needWashCount}
                  </span>
                )}
              </button>

              <button
                onClick={() => setActiveTab('journal')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'journal'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <BookOpen className="w-3.5 h-3.5 text-indigo-500" />
                <span>Wear Journal ({reports.length})</span>
              </button>

              <button
                onClick={() => setActiveTab('drilldown')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'drilldown'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <FolderTree className="w-3.5 h-3.5 text-emerald-600" />
                <span>Drill-Down Report</span>
              </button>

              <button
                onClick={() => setActiveTab('advisor')}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl transition-all ${
                  activeTab === 'advisor'
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                <span>AI Styling</span>
              </button>
            </nav>

            {/* Sync & Action Area: Two Distinct Buttons for Push & Pull */}
            <div className="flex items-center gap-1 sm:gap-2 shrink-0">
              {/* Connection Status Badge */}
              <div
                title={
                  syncState.status === 'synced'
                    ? 'Firestore connected and in sync'
                    : syncState.status === 'syncing'
                    ? 'Syncing with Firestore...'
                    : 'Working local-first (offline cache)'
                }
                className="hidden lg:flex items-center gap-1 px-2 py-1 rounded-xl text-[10px] font-mono font-medium border border-slate-200 bg-slate-50 text-slate-600"
              >
                <span
                  className={`w-2 h-2 rounded-full ${
                    syncState.status === 'synced'
                      ? 'bg-emerald-500 ring-2 ring-emerald-200'
                      : syncState.status === 'syncing'
                      ? 'bg-purple-500 animate-pulse'
                      : 'bg-amber-400'
                  }`}
                />
                <span>{syncState.status === 'synced' ? 'Cloud Connected' : syncState.status === 'syncing' ? 'Syncing...' : 'Local First'}</span>
              </div>

              {/* 1. Distinct Button: Pull from Cloud */}
              <button
                type="button"
                onClick={handlePullFromCloud}
                disabled={isPulling || isPushing}
                title="Pull and fetch latest wardrobe data from Firestore into local app"
                className="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1.5 rounded-xl border border-blue-200 bg-blue-50/80 hover:bg-blue-100 text-blue-800 text-[11px] sm:text-xs font-bold transition-all disabled:opacity-50 shadow-2xs active:scale-95"
              >
                <DownloadCloud className={`w-3.5 h-3.5 text-blue-600 shrink-0 ${isPulling ? 'animate-bounce' : ''}`} />
                <span className="hidden sm:inline">Pull from Cloud</span>
                <span className="sm:hidden">Pull</span>
              </button>

              {/* 2. Distinct Button: Push to Cloud */}
              <button
                type="button"
                onClick={handlePushToCloud}
                disabled={isPulling || isPushing}
                title="Push and upload all local wardrobe, reports & photos up into Firestore"
                className="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1.5 rounded-xl border border-emerald-200 bg-emerald-50/80 hover:bg-emerald-100 text-emerald-800 text-[11px] sm:text-xs font-bold transition-all disabled:opacity-50 shadow-2xs active:scale-95"
              >
                <UploadCloud className={`w-3.5 h-3.5 text-emerald-600 shrink-0 ${isPushing ? 'animate-pulse' : ''}`} />
                <span className="hidden sm:inline">Push to Cloud</span>
                <span className="sm:hidden">Push</span>
              </button>

              {/* Log Outfit Action */}
              <button
                type="button"
                onClick={() => setIsLogModalOpen(true)}
                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold shadow-xs transition-all active:scale-95 shrink-0"
              >
                <Plus className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Log Outfit</span>
                <span className="sm:hidden">Log</span>
              </button>
            </div>
          </div>
        </div>

        {/* Real-Time Sync Progress Bar & Status Banner */}
        {syncProgress && syncProgress.active && (
          <div
            id="sync-progress-banner"
            className={`w-full border-t border-b px-4 py-2.5 transition-all animate-in slide-in-from-top-2 duration-200 ${
              syncProgress.completed
                ? 'bg-emerald-50/95 border-emerald-200 text-emerald-900'
                : syncProgress.percent === 100 && !syncProgress.completed
                ? 'bg-rose-50/95 border-rose-200 text-rose-900'
                : syncProgress.type === 'push'
                ? 'bg-purple-50/95 border-purple-200 text-purple-950'
                : 'bg-blue-50/95 border-blue-200 text-blue-950'
            }`}
          >
            <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                {syncProgress.completed ? (
                  <div className="w-7 h-7 rounded-full bg-emerald-500 text-white flex items-center justify-center shrink-0 shadow-xs">
                    <CheckCircle2 className="w-4 h-4" />
                  </div>
                ) : syncProgress.percent === 100 && !syncProgress.completed ? (
                  <div className="w-7 h-7 rounded-full bg-rose-500 text-white flex items-center justify-center shrink-0 shadow-xs">
                    <AlertCircle className="w-4 h-4" />
                  </div>
                ) : (
                  <div className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 shadow-xs ${
                    syncProgress.type === 'push' ? 'bg-purple-600 text-white' : 'bg-blue-600 text-white'
                  }`}>
                    {syncProgress.type === 'push' ? (
                      <UploadCloud className="w-4 h-4 animate-pulse" />
                    ) : (
                      <DownloadCloud className="w-4 h-4 animate-bounce" />
                    )}
                  </div>
                )}

                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-xs">
                      {syncProgress.completed
                        ? 'Sync Completed'
                        : syncProgress.type === 'push'
                        ? 'Pushing to Cloud Firestore'
                        : 'Pulling from Cloud Firestore'}
                    </span>
                    <span className={`px-2 py-0.2 rounded-full text-[10px] font-mono font-bold ${
                      syncProgress.completed
                        ? 'bg-emerald-200 text-emerald-800'
                        : 'bg-white/80 border border-slate-200 text-slate-700'
                    }`}>
                      {syncProgress.completed ? '100% Completed' : `${Math.round(syncProgress.percent)}%`}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-600 truncate mt-0.5 font-medium">
                    {syncProgress.step}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3 w-full sm:w-64 shrink-0">
                {/* Visual Progress Bar Track */}
                <div className="flex-1 bg-white/80 rounded-full h-2.5 overflow-hidden border border-slate-200/80 shadow-inner">
                  <div
                    className={`h-full transition-all duration-300 rounded-full ${
                      syncProgress.completed
                        ? 'bg-emerald-500'
                        : syncProgress.percent === 100 && !syncProgress.completed
                        ? 'bg-rose-500'
                        : syncProgress.type === 'push'
                        ? 'bg-purple-600'
                        : 'bg-blue-600'
                    }`}
                    style={{ width: `${Math.max(5, Math.min(100, syncProgress.percent))}%` }}
                  />
                </div>

                {syncProgress.completed && (
                  <button
                    type="button"
                    onClick={() => setSyncProgress(null)}
                    className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-black/5 transition-colors"
                    title="Dismiss"
                  >
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Sync Toast Notification Banner */}
        {syncToast && !syncProgress && (
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
      <main className="flex-1 w-full max-w-7xl mx-auto px-3.5 sm:px-6 lg:px-8 py-4 sm:py-6 pb-24 md:pb-12 overflow-x-hidden">
        {activeTab === 'closet' && (
          <WardrobeStudio
            clothes={clothes}
            junctions={junctions}
            onSelectGarment={(garment) => setSelectedGarment(garment)}
            onOpenAddModal={() => setIsAddModalOpen(true)}
          />
        )}

        {activeTab === 'laundry' && (
          <LaundryCare
            clothes={clothes}
            junctions={junctions}
            onMarkWashed={handleMarkWashed}
            onMarkAllWashed={handleMarkAllWashed}
            onSelectGarment={(garment) => setSelectedGarment(garment)}
          />
        )}

        {activeTab === 'journal' && (
          <WearJournal
            reports={reports}
            junctions={junctions}
            clothes={clothes}
            onOpenLogModal={() => setIsLogModalOpen(true)}
            onSelectGarment={(garment) => setSelectedGarment(garment)}
            selectedReport={selectedSessionReport}
            onSelectReport={setSelectedSessionReport}
            onDeleteReport={handleDeleteReport}
            onUpdateReport={handleUpdateReport}
          />
        )}

        {activeTab === 'drilldown' && (
          <DrillDownReport
            clothes={clothes}
            reports={reports}
            junctions={junctions}
            onSelectGarment={(garment) => setSelectedGarment(garment)}
            onSelectReport={(report) => {
              setSelectedSessionReport(report);
              setActiveTab('journal');
            }}
          />
        )}

        {activeTab === 'advisor' && (
          <AIAdvisor
            clothes={clothes}
            onSelectGarment={(garment) => setSelectedGarment(garment)}
          />
        )}
      </main>

      {/* Mobile-First Fixed Bottom Navigation Bar */}
      <nav
        id="mobile-bottom-navigation"
        className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 py-1.5 px-3 flex items-center justify-around shadow-lg select-none"
      >
        <button
          onClick={() => setActiveTab('closet')}
          className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-2xl transition-all ${
            activeTab === 'closet' ? 'text-purple-700 font-bold' : 'text-slate-500'
          }`}
        >
          <Shirt className="w-5 h-5" />
          <span className="text-[10px]">Closet</span>
        </button>

        <button
          onClick={() => setActiveTab('laundry')}
          className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-2xl relative transition-all ${
            activeTab === 'laundry' ? 'text-blue-600 font-bold' : 'text-slate-500'
          }`}
        >
          <div className="relative">
            <Droplets className="w-5 h-5" />
            {needWashCount > 0 && (
              <span className="absolute -top-1 -right-2 px-1 rounded-full text-[9px] font-bold bg-amber-500 text-white">
                {needWashCount}
              </span>
            )}
          </div>
          <span className="text-[10px]">Laundry</span>
        </button>

        <button
          onClick={() => setActiveTab('journal')}
          className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-2xl transition-all ${
            activeTab === 'journal' ? 'text-indigo-600 font-bold' : 'text-slate-500'
          }`}
        >
          <BookOpen className="w-5 h-5" />
          <span className="text-[10px]">Journal</span>
        </button>

        <button
          onClick={() => setActiveTab('drilldown')}
          className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-2xl transition-all ${
            activeTab === 'drilldown' ? 'text-emerald-600 font-bold' : 'text-slate-500'
          }`}
        >
          <FolderTree className="w-5 h-5" />
          <span className="text-[10px]">Reports</span>
        </button>

        <button
          onClick={() => setActiveTab('advisor')}
          className={`flex flex-col items-center gap-0.5 py-1 px-3 rounded-2xl transition-all ${
            activeTab === 'advisor' ? 'text-amber-600 font-bold' : 'text-slate-500'
          }`}
        >
          <Sparkles className="w-5 h-5" />
          <span className="text-[10px]">AI Advisor</span>
        </button>
      </nav>

      {/* Desktop / Tablet Footer */}
      <footer className="hidden md:block bg-white border-t border-slate-200/80 py-5 text-xs text-slate-500 shrink-0">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-800">Wardrobe &amp; Wear Journal</span>
            <span>·</span>
            <span>{clothes.length} garments</span>
            <span>·</span>
            <span>{reports.length} wearing reports</span>
            <span>·</span>
            <span className="text-emerald-700 font-mono">Google Firestore</span>
          </div>

          <div className="flex items-center gap-4">
            <button
              onClick={handleResetSeed}
              className="text-slate-400 hover:text-slate-700 flex items-center gap-1 transition-colors"
              title="Reset data back to initial seed"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Reset Database Seed
            </button>
          </div>
        </div>
      </footer>

      {/* Modals with event stop and backdrop barrier */}
      <GarmentModal
        garment={selectedGarment}
        stats={selectedGarmentStats}
        junctions={junctions}
        reports={reports}
        onClose={() => setSelectedGarment(null)}
        onMarkWashed={handleMarkWashed}
        onUpdateGarment={handleUpdateGarment}
        onDeleteGarment={handleDeleteGarment}
        onSelectReport={(report) => {
          setSelectedGarment(null);
          setSelectedSessionReport(report);
          setActiveTab('journal');
        }}
      />

      <LogOutfitModal
        clothes={clothes}
        isOpen={isLogModalOpen}
        onClose={() => setIsLogModalOpen(false)}
        onSaveReport={handleSaveReport}
      />

      <AddGarmentModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onAddGarment={handleAddGarment}
      />

      {selectedSessionReport && (
        <SessionDetailModal
          report={selectedSessionReport}
          junctions={junctions}
          clothes={clothes}
          onClose={() => setSelectedSessionReport(null)}
          onSelectGarment={(garment) => {
            setSelectedSessionReport(null);
            setSelectedGarment(garment);
          }}
          onDeleteReport={handleDeleteReport}
          onUpdateReport={handleUpdateReport}
        />
      )}
    </div>
  );
}

export default App;
