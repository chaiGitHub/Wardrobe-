import React, { useState } from 'react';
import {
  Cloud,
  UploadCloud,
  DownloadCloud,
  CheckCircle2,
  AlertCircle,
  Loader2,
  RefreshCw,
  Database,
  Calendar,
  Shirt,
  BookOpen,
  Link2,
  Check,
  ShieldCheck,
  ArrowUpRight,
  ArrowDownLeft,
  FileDown,
  FileUp,
  Copy,
  Layers,
  Sparkles,
  Shield,
  Clock,
  Wifi,
  WifiOff,
  Image as ImageIcon,
  FolderArchive,
  Key,
  Eye,
  EyeOff,
  ExternalLink,
  Trash2,
  RotateCcw,
  ArrowLeft,
  X,
  Plus,
  Edit2,
  Info,
  CheckCheck,
  Sliders,
  Type,
  FileArchive,
  Download
} from 'lucide-react';
import {
  FONT_SCALE_OPTIONS,
  getCurrentFontScale,
  setGlobalFontScale,
  FontScale
} from './FontSizeSwitcher';
import { ConfirmModal } from './ConfirmModal';
import { notebookLmExportService } from '../services/notebookLmExportService';
import { NotebookLmPreviewModal } from './NotebookLmPreviewModal';
import { dataService, SyncStatus, BackupInspectionResult, BackupFieldDiff, RestoreFieldResolutions } from '../services/dataService';
import { syncOutbox } from '../services/syncOutbox';
import { mediaOutbox } from '../services/mediaOutbox';
import {
  getCustomApiKey, setCustomApiKey, clearCustomApiKey, testGeminiApiKey,
  getSelectedModel, setSelectedModel, GeminiModelId,
  useAiGenerationPaused, setAiGenerationPaused
} from '../services/aiSettings';

interface SyncViewProps {
  syncStatus: SyncStatus;
  clothesCount: number;
  reportsCount: number;
  junctionsCount: number;
  onPush?: () => Promise<void>;
  onPull?: () => Promise<void>;
  onRefresh: () => Promise<void>;
  isPushing?: boolean;
  isPulling?: boolean;
  isRefreshing: boolean;
}

export const SyncView: React.FC<SyncViewProps> = ({
  syncStatus,
  clothesCount,
  reportsCount,
  junctionsCount,
  onPush = async () => {},
  onPull = async () => {},
  onRefresh,
  isPushing = false,
  isPulling = false,
  isRefreshing
}) => {
  const [activeAction, setActiveAction] = useState<'push' | 'pull' | 'refresh' | 'export' | 'import' | null>(null);
  const [actionProgress, setActionProgress] = useState<{ step: string; percent: number } | null>(null);
  const [bannerNotice, setBannerNotice] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [copiedNotice, setCopiedNotice] = useState(false);
  const [deduping, setDeduping] = useState(false);
  const [dedupeResult, setDedupeResult] = useState<{ before: number; after: number; removed: number } | null>(null);
  const [reportDedupeResult, setReportDedupeResult] = useState<{ before: number; after: number; removed: number; details: string[] } | null>(null);
  const [outboxState, setOutboxState] = useState(() => syncOutbox.getState());
  const [mediaState, setMediaState] = useState(() => mediaOutbox.getState());
  const [isFlushingOutbox, setIsFlushingOutbox] = useState(false);
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  // Full Self-Contained ZIP Backup State
  const [isDownloadingZip, setIsDownloadingZip] = useState(false);
  const [downloadZipProgress, setDownloadZipProgress] = useState<{ step: string; percent: number } | null>(null);
  const [isRestoringZip, setIsRestoringZip] = useState(false);
  const [zipRestoreProgress, setZipRestoreProgress] = useState<{ step: string; percent: number } | null>(null);
  const [zipRestoreResult, setZipRestoreResult] = useState<{ clothesCount: number; reportsCount: number; junctionsCount: number; restoredPhotosCount: number } | null>(null);
  const [pendingRestoreFile, setPendingRestoreFile] = useState<File | null>(null);
  const [showRestoreConfirmModal, setShowRestoreConfirmModal] = useState(false);
  const zipFileInputRef = React.useRef<HTMLInputElement>(null);

  // Backup Inspection & Field Protection Resolution State
  const [isInspectingZip, setIsInspectingZip] = useState(false);
  const [inspectionResult, setInspectionResult] = useState<BackupInspectionResult | null>(null);
  const [fieldActions, setFieldActions] = useState<Record<string, 'keep' | 'rename' | 'delete'>>({});
  const [fieldRenames, setFieldRenames] = useState<Record<string, string>>({});
  const [additionalFieldsToAdd, setAdditionalFieldsToAdd] = useState<Array<{ id: string; name: string; target: 'clothes' | 'reports' | 'both'; defaultValue?: any }>>([]);
  const [newFieldNameInput, setNewFieldNameInput] = useState('');
  const [newFieldTargetInput, setNewFieldTargetInput] = useState<'clothes' | 'reports' | 'both'>('clothes');
  const [newFieldDefaultInput, setNewFieldDefaultInput] = useState('');
  const [showAddCustomFieldInput, setShowAddCustomFieldInput] = useState(false);
  const [restoreMergeStrategy, setRestoreMergeStrategy] = useState<'smart_merge' | 'replace_with_fields'>('smart_merge');

  // Personal Google AI Studio API Key & Model Selection State
  const [customKey, setCustomKey] = useState(() => getCustomApiKey() || '');
  const [selectedModel, setLocalSelectedModel] = useState<GeminiModelId>(() => getSelectedModel());
  const [showKey, setShowKey] = useState(false);
  const [isTestingKey, setIsTestingKey] = useState(false);
  const [keyTestResult, setKeyTestResult] = useState<{ ok: boolean; message: string; model?: string; isPrepaymentDepleted?: boolean } | null>(null);
  const [activeKeySaved, setActiveKeySaved] = useState(false);
  const isAiPaused = useAiGenerationPaused();

  // Database Reset & Empty Modals State
  const [showEmptyConfirmModal, setShowEmptyConfirmModal] = useState(false);
  const [showResetSeedConfirmModal, setShowResetSeedConfirmModal] = useState(false);
  const [deletePhotosWithEmpty, setDeletePhotosWithEmpty] = useState(false);
  const [isResettingDb, setIsResettingDb] = useState(false);
  const [fontScale, setFontScale] = useState<FontScale>(getCurrentFontScale);

  React.useEffect(() => {
    const handleScaleChanged = (e: Event) => {
      const customEvent = e as CustomEvent<FontScale>;
      if (customEvent.detail) {
        setFontScale(customEvent.detail);
      }
    };
    window.addEventListener('wardrobe_font_scale_changed', handleScaleChanged);
    return () => window.removeEventListener('wardrobe_font_scale_changed', handleScaleChanged);
  }, []);

  const handleScaleSelect = (scale: FontScale) => {
    setFontScale(scale);
    setGlobalFontScale(scale);
  };

  const [isNotebookLmModalOpen, setIsNotebookLmModalOpen] = useState(false);
  const [notebookLmTopicId, setNotebookLmTopicId] = useState('catalog');
  const [isDownloadingNotebookLmZip, setIsDownloadingNotebookLmZip] = useState(false);

  const handleDownloadNotebookLmZip = async () => {
    try {
      setIsDownloadingNotebookLmZip(true);
      const zipBlob = await notebookLmExportService.generateNotebookLmKnowledgeZip();
      notebookLmExportService.downloadFile('NotebookLM_Wardrobe_Knowledge_Pack.zip', zipBlob, 'application/zip');
      setBannerNotice({
        type: 'success',
        message: 'NotebookLM Wardrobe Knowledge Pack (.zip) downloaded! Extract and upload into notebooklm.google.com as sources.'
      });
    } catch (err: any) {
      console.error('Failed to create NotebookLM zip:', err);
      setBannerNotice({
        type: 'error',
        message: 'Failed to generate NotebookLM ZIP archive.'
      });
    } finally {
      setIsDownloadingNotebookLmZip(false);
    }
  };

  const handleOpenNotebookLmPreview = (topicId: string) => {
    setNotebookLmTopicId(topicId);
    setIsNotebookLmModalOpen(true);
  };

  const handleSelectModel = (m: GeminiModelId) => {
    setSelectedModel(m);
    setLocalSelectedModel(m);
    setActiveKeySaved(true);
    setTimeout(() => setActiveKeySaved(false), 2500);
  };

  React.useEffect(() => {
    const unsubOutbox = syncOutbox.subscribe(state => {
      setOutboxState(state);
    });
    const unsubMedia = mediaOutbox.subscribe(mState => {
      setMediaState(mState);
    });
    return () => {
      unsubOutbox();
      unsubMedia();
    };
  }, []);

  const totalPending = outboxState.pendingCount + mediaState.pendingCount;

  const handleFlushOutbox = async () => {
    setIsFlushingOutbox(true);
    try {
      await Promise.all([
        syncOutbox.flush(),
        mediaOutbox.processQueue()
      ]);
      setBannerNotice({
        type: 'success',
        message: 'All local changes and photos saved to local disk successfully!'
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to save pending changes.'
      });
    } finally {
      setIsFlushingOutbox(false);
    }
  };

  const lastSyncTime = dataService.getSyncStatus().lastSync;
  const allJunctions = dataService.getJunctions();
  const detectedDuplicateReports = dataService.findDuplicateReports();
  
  // Detect duplicate junction records within any single wearing report session
  const sessionGarmentCounts = new Map<string, number>();
  allJunctions.forEach(j => {
    const repId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
    const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
    const key = `${repId}___${cId}`;
    sessionGarmentCounts.set(key, (sessionGarmentCounts.get(key) || 0) + 1);
  });
  let detectedDuplicateCount = 0;
  sessionGarmentCounts.forEach(count => {
    if (count > 1) detectedDuplicateCount += (count - 1);
  });

  const handleDeduplicateClick = async () => {
    setDeduping(true);
    try {
      const res = await dataService.deduplicateJunctions();
      setDedupeResult({ before: res.beforeCount, after: res.afterCount, removed: res.removedCount });
      setBannerNotice({
        type: 'success',
        message: res.removedCount > 0
          ? `Cleaned ${res.removedCount} duplicate junction records! Rows before: ${res.beforeCount}, after: ${res.afterCount}. All wash dates safely preserved.`
          : 'Database is clean: zero duplicate garments found in any wearing report session.'
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to deduplicate junctions.'
      });
    } finally {
      setDeduping(false);
    }
  };

  const handleDeduplicateReportsClick = async () => {
    setDeduping(true);
    try {
      const res = await dataService.deduplicateWearingReports();
      setReportDedupeResult({
        before: res.beforeCount,
        after: res.afterCount,
        removed: res.removedReportsCount,
        details: res.details
      });
      setBannerNotice({
        type: 'success',
        message: res.removedReportsCount > 0
          ? `Cleaned & merged ${res.removedReportsCount} duplicate wearing report(s)! Reports before: ${res.beforeCount} → after: ${res.afterCount}. All pictures, tags, and garment links merged.`
          : 'Zero duplicate wearing reports found. All journal reports are unique.'
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to deduplicate wearing reports.'
      });
    } finally {
      setDeduping(false);
    }
  };

  const handleDeduplicateAllClick = async () => {
    setDeduping(true);
    try {
      const res = await dataService.deduplicateAll();
      setReportDedupeResult({
        before: res.reports.beforeCount,
        after: res.reports.afterCount,
        removed: res.reports.removedReportsCount,
        details: (res.reports as any).details || []
      });
      setDedupeResult({
        before: res.junctions.beforeCount,
        after: res.junctions.afterCount,
        removed: res.junctions.removedCount
      });
      setBannerNotice({
        type: 'success',
        message: `Complete deduplication finished! Merged ${res.reports.removedReportsCount} duplicate reports and cleaned ${res.junctions.removedCount} duplicate session garments.`
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to run complete deduplication.'
      });
    } finally {
      setDeduping(false);
    }
  };
  
  const handleExecuteEmptyDatabase = async () => {
    setIsResettingDb(true);
    setActionProgress({ step: 'Emptying database records...', percent: 40 });
    try {
      await dataService.emptyDatabase({ deletePhotos: deletePhotosWithEmpty });
      setActionProgress({ step: 'Database emptied!', percent: 100 });
      setShowEmptyConfirmModal(false);
      setBannerNotice({
        type: 'success',
        message: deletePhotosWithEmpty
          ? 'Database wiped completely. All clothes, wearing reports, junction links, and local photos have been removed.'
          : 'Database emptied. All clothes, wearing reports, and wear logs have been removed (photo files preserved).'
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to empty database.'
      });
    } finally {
      setIsResettingDb(false);
      setTimeout(() => setActionProgress(null), 1200);
    }
  };

  const handleExecuteResetToSeed = async () => {
    setIsResettingDb(true);
    setActionProgress({ step: 'Restoring seed data from original dataset...', percent: 40 });
    try {
      const res = await dataService.resetToSeedData();
      setActionProgress({ step: 'Seed dataset loaded!', percent: 100 });
      setShowResetSeedConfirmModal(false);
      setBannerNotice({
        type: 'success',
        message: `Database reset to initial seed data! Loaded ${res.clothes} garments, ${res.reports} wearing reports, and ${res.junctions} wear log junctions.`
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to reset database to seed data.'
      });
    } finally {
      setIsResettingDb(false);
      setTimeout(() => setActionProgress(null), 1200);
    }
  };

  // Calculate junction stats
  const junctionsWithWashDate = allJunctions.filter(j => {
    return Boolean(j['wash date'] || j.washDate || j['washed date'] || j.washedDate);
  }).length;

  const cleanJunctions = allJunctions.filter(j => {
    return j['wash stated'] === 'wash' || j.washStated === 'wash';
  }).length;

  const needWashJunctions = allJunctions.filter(j => {
    return j['wash stated'] === 'need wash' || j.washStated === 'need wash';
  }).length;

  const handlePushClick = async () => {
    setActiveAction('push');
    setActionProgress({ step: 'Saving garments, reports, and wear records to local disk...', percent: 25 });
    setBannerNotice(null);
    try {
      const res = await dataService.pushToCloud((step, percent) => {
        setActionProgress({ step, percent });
      });
      setBannerNotice({
        type: 'success',
        message: `Successfully saved all data to local disk: ${res.clothes} garments, ${res.reports} wearing reports, and ${res.junctions} wear records.`
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to save records to local storage.'
      });
    } finally {
      setActiveAction(null);
      setTimeout(() => setActionProgress(null), 1200);
    }
  };

  const handlePullClick = async () => {
    setActiveAction('pull');
    setActionProgress({ step: 'Loading latest records from local database file...', percent: 25 });
    setBannerNotice(null);
    try {
      const res = await dataService.pullFromCloud((step, percent) => {
        setActionProgress({ step, percent });
      });
      setBannerNotice({
        type: 'success',
        message: `Successfully loaded data from local disk: ${res.clothes} garments, ${res.reports} wearing reports, and ${res.junctions} wear records.`
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to load records from local database.'
      });
    } finally {
      setActiveAction(null);
      setTimeout(() => setActionProgress(null), 1200);
    }
  };

  const handleExportJsonClick = () => {
    try {
      const res = dataService.exportCompleteDatabaseJson('initialData.json');
      setBannerNotice({
        type: 'success',
        message: `Exported initialData.json with ${res.clothesCount} garments, ${res.reportsCount} wearing reports, and ${res.junctionsCount} junction records.`
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: 'Failed to download initialData.json.'
      });
    }
  };

  const handleCopyJsonClick = () => {
    try {
      const res = dataService.exportCompleteDatabaseJson();
      navigator.clipboard.writeText(res.jsonString);
      setCopiedNotice(true);
      setTimeout(() => setCopiedNotice(false), 2000);
    } catch (err) {
      console.warn('Clipboard error:', err);
    }
  };

  const handleImportFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setActiveAction('import');
    setActionProgress({ step: 'Reading and parsing backup file...', percent: 50 });
    setBannerNotice(null);

    try {
      const text = await file.text();
      const res = await dataService.importDatabaseFromJson(text);
      setActionProgress({ step: 'Done!', percent: 100 });
      setBannerNotice({
        type: 'success',
        message: `Database successfully restored! Loaded ${res.clothes} garments, ${res.reports} wearing reports, ${res.junctions} junction records, and ${res.presets || 0} drill-down presets.`
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to parse and import backup file.'
      });
    } finally {
      setActiveAction(null);
      setTimeout(() => setActionProgress(null), 1200);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDownloadZipBackup = async () => {
    setIsDownloadingZip(true);
    setDownloadZipProgress({ step: 'Preparing full archive...', percent: 5 });
    setBannerNotice(null);
    try {
      await dataService.downloadFullBackupZip((step, percent) => {
        setDownloadZipProgress({ step, percent });
      });
      setBannerNotice({
        type: 'success',
        message: 'Full backup ZIP archive downloaded successfully! It includes your complete database, all original photos, and Markdown AI reports.'
      });
    } catch (err: any) {
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to download full backup ZIP archive.'
      });
    } finally {
      setIsDownloadingZip(false);
      setTimeout(() => setDownloadZipProgress(null), 1500);
    }
  };

  const handleRestoreZipFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setPendingRestoreFile(file);
    setShowRestoreConfirmModal(true);
    setIsInspectingZip(true);
    setInspectionResult(null);

    try {
      const result = await dataService.inspectBackupZip(file);
      setInspectionResult(result);

      // Default all detected fields to 'keep' ("The new field no going to delete")
      const initialActions: Record<string, 'keep' | 'rename' | 'delete'> = {};
      const initialRenames: Record<string, string> = {};
      result.allDetectedFields.forEach(f => {
        initialActions[f.fieldName] = 'keep';
        initialRenames[f.fieldName] = '';
      });
      setFieldActions(initialActions);
      setFieldRenames(initialRenames);
      setAdditionalFieldsToAdd([]);
      setShowAddCustomFieldInput(false);
      setRestoreMergeStrategy('smart_merge');
    } catch (err: any) {
      console.warn('Backup inspect notice:', err);
    } finally {
      setIsInspectingZip(false);
    }
  };

  const handleAddFieldResolutionAction = (fieldName: string, action: 'keep' | 'rename' | 'delete') => {
    setFieldActions(prev => ({ ...prev, [fieldName]: action }));
  };

  const handleSetFieldRename = (fieldName: string, newName: string) => {
    setFieldRenames(prev => ({ ...prev, [fieldName]: newName }));
  };

  const handleAddCustomFieldToRestore = () => {
    const trimmed = newFieldNameInput.trim();
    if (!trimmed) return;
    const exists = additionalFieldsToAdd.some(a => a.name.toLowerCase() === trimmed.toLowerCase());
    if (!exists) {
      setAdditionalFieldsToAdd(prev => [
        ...prev,
        {
          id: `new_field_${Date.now()}`,
          name: trimmed,
          target: newFieldTargetInput,
          defaultValue: newFieldDefaultInput.trim() || undefined
        }
      ]);
    }
    setNewFieldNameInput('');
    setNewFieldDefaultInput('');
    setShowAddCustomFieldInput(false);
  };

  const handleRemoveAdditionalField = (id: string) => {
    setAdditionalFieldsToAdd(prev => prev.filter(a => a.id !== id));
  };

  const executeZipRestore = async (fileToRestore: File) => {
    setShowRestoreConfirmModal(false);
    setIsRestoringZip(true);
    setZipRestoreResult(null);
    setZipRestoreProgress({ step: 'Reading archive contents...', percent: 10 });
    setBannerNotice(null);

    try {
      const resolutions: RestoreFieldResolutions = {
        fieldActions,
        fieldRenames,
        additionalFieldsToAdd: additionalFieldsToAdd.map(af => ({
          name: af.name.trim(),
          target: af.target,
          defaultValue: af.defaultValue
        })),
        strategy: restoreMergeStrategy
      };

      const result = await dataService.restoreFullBackupZip(fileToRestore, (step, percent) => {
        setZipRestoreProgress({ step, percent });
      }, resolutions);

      setZipRestoreResult(result);
      try {
        await onRefresh();
      } catch (refreshErr) {
        console.warn('Post-restore refresh notice:', refreshErr);
      }

      const keptCount = Object.values(fieldActions).filter(a => a === 'keep').length + additionalFieldsToAdd.length;
      const renamedCount = Object.values(fieldActions).filter(a => a === 'rename').length;
      const deletedCount = Object.values(fieldActions).filter(a => a === 'delete').length;

      setBannerNotice({
        type: 'success',
        message: `Full backup restored successfully! Restored ${result.clothesCount} garments, ${result.reportsCount} wearing reports, ${result.junctionsCount} wear records, ${result.restoredPhotosCount} original photos, and ${(result as any).restoredPresetsCount ?? 0} drill-down presets. (Protected fields: ${keptCount} kept, ${renamedCount} renamed, ${deletedCount} deleted).`
      });
    } catch (err: any) {
      console.error('ZIP restore error:', err);
      setBannerNotice({
        type: 'error',
        message: err?.message || 'Failed to restore full backup archive.'
      });
    } finally {
      setIsRestoringZip(false);
      setPendingRestoreFile(null);
      setInspectionResult(null);
      setTimeout(() => setZipRestoreProgress(null), 2500);
      if (zipFileInputRef.current) zipFileInputRef.current.value = '';
    }
  };

  const cancelZipRestore = () => {
    setShowRestoreConfirmModal(false);
    setPendingRestoreFile(null);
    setInspectionResult(null);
    setShowAddCustomFieldInput(false);
    if (zipFileInputRef.current) zipFileInputRef.current.value = '';
  };

  const handleTestAndSaveKey = async () => {
    const trimmed = customKey.trim();
    if (!trimmed) {
      clearCustomApiKey();
      setCustomKey('');
      setKeyTestResult({ ok: true, message: 'Cleared custom API key. Reverted to server environment key.' });
      setActiveKeySaved(true);
      setTimeout(() => setActiveKeySaved(false), 3000);
      return;
    }

    setIsTestingKey(true);
    setKeyTestResult(null);
    try {
      const res = await testGeminiApiKey(trimmed, selectedModel);
      setKeyTestResult(res);
      if (res.ok) {
        setCustomApiKey(trimmed);
        setSelectedModel(selectedModel);
        setActiveKeySaved(true);
        setTimeout(() => setActiveKeySaved(false), 3000);
      }
    } catch (err: any) {
      setKeyTestResult({ ok: false, message: err?.message || 'Verification request failed.' });
    } finally {
      setIsTestingKey(false);
    }
  };

  const handleClearCustomKey = () => {
    clearCustomApiKey();
    setCustomKey('');
    setKeyTestResult({ ok: true, message: 'Removed custom key. Reverted to default environment key.' });
    setActiveKeySaved(true);
    setTimeout(() => setActiveKeySaved(false), 3000);
  };

  const isBusy = isPushing || isPulling || isRefreshing || isDownloadingZip || isRestoringZip || activeAction !== null;

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-16 pt-2 animate-in fade-in duration-300">
      {/* Header & Overview */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-xs space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2.5">
              <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-xs">
                <Database className="w-5 h-5" />
              </div>
              <div>
                <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                  Local Storage & Database
                </h1>
                <p className="text-xs text-slate-500 font-medium">
                  100% Local Application — Zero cloud dependency. Data saved to local disk.
                </p>
              </div>
            </div>
          </div>

          {/* Connection Status Pill */}
          <div className="flex items-center gap-2 self-start sm:self-auto">
            <div className="px-3.5 py-1.5 rounded-full bg-slate-100 border border-slate-200 flex items-center gap-2 text-xs font-semibold text-slate-700">
              <span className={`w-2.5 h-2.5 rounded-full ${
                syncStatus === 'synced' ? 'bg-emerald-500 shadow-xs shadow-emerald-300' :
                syncStatus === 'syncing' ? 'bg-amber-500 animate-pulse' :
                'bg-red-500'
              }`} />
              <span>
                {syncStatus === 'synced' ? 'Connected & Synced' :
                 syncStatus === 'syncing' ? 'Syncing...' :
                 'Offline / Check Connection'}
              </span>
              {lastSyncTime && (
                <span className="text-[11px] font-mono text-slate-400 pl-1 border-l border-slate-300">
                  {lastSyncTime}
                </span>
              )}
            </div>

            <button
              type="button"
              onClick={onRefresh}
              disabled={isBusy}
              className="p-2 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 hover:text-slate-900 transition-colors disabled:opacity-50 cursor-pointer"
              title="Refresh connection status"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Reassurance Notice regarding manual wash date preservation */}
        <div className="p-3.5 rounded-2xl bg-emerald-50/80 border border-emerald-200/80 text-xs text-emerald-900 flex items-start gap-2.5">
          <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
          <div className="space-y-0.5">
            <p className="font-bold text-emerald-950">100% Local Storage & Wash Dates Preserved</p>
            <p className="text-emerald-800 text-[11px] leading-relaxed">
              All data and photos are stored directly on your local device / server. All manual wash dates and wearing records are safely preserved without any cloud dependencies.
            </p>
          </div>
        </div>

        {/* Status / Alert Banner */}
        {bannerNotice && (
          <div className={`p-4 rounded-2xl flex items-start gap-3 text-xs font-medium ${
            bannerNotice.type === 'success'
              ? 'bg-emerald-100 border border-emerald-300 text-emerald-900'
              : 'bg-red-100 border border-red-300 text-red-900'
          }`}>
            {bannerNotice.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
            ) : (
              <AlertCircle className="w-4 h-4 text-red-700 shrink-0 mt-0.5" />
            )}
            <div className="flex-1">{bannerNotice.message}</div>
            <button
              type="button"
              onClick={() => setBannerNotice(null)}
              className="text-slate-400 hover:text-slate-600 font-bold"
            >
              ✕
            </button>
          </div>
        )}

        {/* Real-time Progress Bar */}
        {actionProgress && (
          <div className="p-4 rounded-2xl bg-slate-900 text-white space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-medium text-slate-300 flex items-center gap-2">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-indigo-400" />
                {actionProgress.step}
              </span>
              <span className="font-mono font-bold text-indigo-400">{actionProgress.percent}%</span>
            </div>
            <div className="w-full bg-slate-800 rounded-full h-2 overflow-hidden">
              <div
                className="bg-gradient-to-r from-indigo-500 to-purple-500 h-full transition-all duration-300 rounded-full"
                style={{ width: `${actionProgress.percent}%` }}
              />
            </div>
          </div>
        )}
      </div>

      {/* Industrial Local-First Outbox & Reliability Status */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border border-slate-200/90 shadow-xs space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
          <div className="flex items-center gap-3">
            <div className={`w-11 h-11 rounded-2xl flex items-center justify-center shadow-xs ${
              outboxState.pendingCount > 0
                ? 'bg-amber-500 text-white'
                : 'bg-emerald-600 text-white'
            }`}>
              <Shield className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base sm:text-lg font-bold text-slate-950">
                  Local-First Outbox &amp; Sync Ledger
                </h2>
                {outboxState.pendingCount > 0 ? (
                  <span className="px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-800 font-mono font-bold text-[11px] animate-pulse">
                    {outboxState.pendingCount} Queued
                  </span>
                ) : (
                  <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-mono font-bold text-[11px]">
                    100% Synced
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-500">
                Instant 0ms local response + guaranteed asynchronous persistence to local disk.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              <span>Local Database Active</span>
            </span>

            <button
              type="button"
              onClick={handleFlushOutbox}
              disabled={isFlushingOutbox || totalPending === 0}
              className="px-3.5 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-100 disabled:text-slate-400 text-white font-bold text-xs transition-all shadow-xs flex items-center gap-1.5 cursor-pointer disabled:cursor-not-allowed"
            >
              {isFlushingOutbox ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Syncing All...</span>
                </>
              ) : (
                <>
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Sync Outbox & Photos ({totalPending})</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* 3 Core Industrial Reliability Guarantees */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900">
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              <span>Zero-Lag Optimistic UI</span>
            </div>
            <p className="text-[11px] text-slate-600 leading-relaxed">
              Adding clothes, wear reports, and laundry updates update state and run all calculations instantly without network delay.
            </p>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900">
              <span className="w-2 h-2 rounded-full bg-indigo-500" />
              <span>Accidental Close Protection</span>
            </div>
            <p className="text-[11px] text-slate-600 leading-relaxed">
              Active window guard prevents data loss by displaying an exit confirmation if you close the tab while mutations are queued.
            </p>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900">
              <span className="w-2 h-2 rounded-full bg-purple-500" />
              <span>Local Storage Persistence</span>
            </div>
            <p className="text-[11px] text-slate-600 leading-relaxed">
              All records and photos are saved directly to local database storage (data/local_db.json) with automatic backup.
            </p>
          </div>
        </div>

        {/* Pending Queue Inspection */}
        {outboxState.mutations.length > 0 ? (
          <div className="space-y-2 pt-1">
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-slate-800 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-amber-500" />
                Pending Outbox Queue ({outboxState.pendingCount} changes awaiting disk save):
              </span>
              <span className="text-slate-400 font-mono text-[10px]">
                Debounce window: 1.2s
              </span>
            </div>

            <div className="max-h-48 overflow-y-auto rounded-2xl border border-amber-200/80 bg-amber-50/40 divide-y divide-amber-100">
              {outboxState.mutations.map((mutation) => (
                <div key={mutation.id} className="p-3 flex items-center justify-between gap-3 text-xs">
                  <div className="space-y-0.5 min-w-0">
                    <p className="font-semibold text-slate-900 truncate">
                      {mutation.label || `${mutation.action.toUpperCase()} ${mutation.collection}/${mutation.docId}`}
                    </p>
                    <div className="flex items-center gap-2 text-[10px] text-slate-500 font-mono">
                      <span className="capitalize px-1.5 py-0.2 rounded bg-white border border-amber-200 font-bold text-amber-900">
                        {mutation.collection}
                      </span>
                      <span className="truncate max-w-[120px]">ID: {mutation.docId}</span>
                      {mutation.retries > 0 && (
                        <span className="text-amber-700 font-bold">
                          · Retries: {mutation.retries}
                        </span>
                      )}
                    </div>
                  </div>

                  <span className="text-[10px] font-mono text-slate-400 shrink-0">
                    {new Date(mutation.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </span>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="p-3.5 rounded-2xl bg-emerald-50/60 border border-emerald-200/60 flex items-center justify-between text-xs text-emerald-900">
            <span className="font-medium flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              All local modifications have been safely saved to the local database file.
            </span>
            <span className="font-mono text-[11px] text-emerald-700 font-semibold">
              Queue: 0 pending
            </span>
          </div>
        )}

        {/* Pending Media Photos Outbox Queue */}
        {mediaState.items.length > 0 ? (
          <div className="space-y-2 pt-2">
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-slate-800 flex items-center gap-1.5">
                <ImageIcon className="w-3.5 h-3.5 text-purple-600" />
                Pending Media Queue ({mediaState.pendingCount} photo(s) saving to Local Disk):
              </span>
              <span className="text-slate-400 font-mono text-[10px]">
                Two-Tier IDB Buffer
              </span>
            </div>

            <div className="max-h-48 overflow-y-auto rounded-2xl border border-purple-200/80 bg-purple-50/40 divide-y divide-purple-100">
              {mediaState.items.map((media) => (
                <div key={media.id} className="p-3 flex items-center justify-between gap-3 text-xs">
                  <div className="flex items-center gap-3 min-w-0">
                    {media.previewUrl ? (
                      <img
                        src={media.previewUrl}
                        alt={media.filename}
                        className="w-10 h-10 rounded-lg object-cover border border-purple-200 shrink-0 bg-white"
                      />
                    ) : (
                      <div className="w-10 h-10 rounded-lg bg-purple-100 flex items-center justify-center text-purple-600 shrink-0">
                        <ImageIcon className="w-5 h-5" />
                      </div>
                    )}
                    <div className="space-y-0.5 min-w-0">
                      <p className="font-semibold text-slate-900 truncate">
                        {media.filename}
                      </p>
                      <div className="flex items-center gap-2 text-[10px] text-slate-500 font-mono">
                        <span className="capitalize px-1.5 py-0.2 rounded bg-white border border-purple-200 font-bold text-purple-900">
                          {media.targetType}
                        </span>
                        <span>{(media.size / 1024).toFixed(0)} KB</span>
                        {media.status === 'uploading' && (
                          <span className="text-purple-700 font-bold animate-pulse">
                            Uploading ({media.progress}%)
                          </span>
                        )}
                        {media.status === 'failed' && (
                          <span className="text-rose-600 font-bold">
                            Failed: {media.error || 'Retry available'}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    {media.status === 'failed' && (
                      <button
                        type="button"
                        onClick={() => mediaOutbox.retryItem(media.id)}
                        className="px-2 py-1 rounded-md bg-white border border-rose-200 text-rose-700 hover:bg-rose-50 text-[10px] font-bold"
                      >
                        Retry
                      </button>
                    )}
                    <span className="text-[10px] font-mono text-slate-400">
                      {new Date(media.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="p-3.5 rounded-2xl bg-emerald-50/60 border border-emerald-200/60 flex items-center justify-between text-xs text-emerald-900">
            <span className="font-medium flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              All photos are permanently stored on local disk and cached in IndexedDB.
            </span>
            <span className="font-mono text-[11px] text-emerald-700 font-semibold">
              Media: 0 pending
            </span>
          </div>
        )}

        {outboxState.lastError && (
          <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span className="flex-1">
              Sync warning: {outboxState.lastError}. Outbox is holding mutations in local storage and will retry automatically.
            </span>
          </div>
        )}
      </div>

      {/* FULL SELF-CONTAINED BACKUP & RESTORE (.ZIP) CARD */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-emerald-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-emerald-100 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shadow-xs">
              <FolderArchive className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950 flex items-center gap-2">
                <span>Full Self-Contained Backup &amp; Restore (.ZIP)</span>
                <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold font-mono">
                  Recommended
                </span>
              </h3>
              <p className="text-xs text-slate-500">
                Packages everything into an all-in-one ZIP archive: database, high-resolution original photos, and formatted Markdown AI reports.
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 font-bold text-[10px] font-mono self-start sm:self-auto">
            100% Complete &amp; Portable
          </span>
        </div>

        {/* Features Checklist */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 text-xs text-slate-700 bg-emerald-50/50 p-3.5 rounded-2xl border border-emerald-100">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span><strong>High-Res Photos:</strong> All garment &amp; wearing pictures</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span><strong>Database Ledger:</strong> Complete records &amp; junctions</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span><strong>AI Reports:</strong> Formatted Markdown files</span>
          </div>
        </div>

        {/* Progress bar during archive packaging/download */}
        {downloadZipProgress && (
          <div className="space-y-1.5 p-3.5 bg-emerald-50 rounded-2xl border border-emerald-200 animate-in fade-in duration-150">
            <div className="flex justify-between text-xs font-bold text-emerald-900">
              <span className="flex items-center gap-1.5">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-emerald-600" />
                {downloadZipProgress.step}
              </span>
              <span>{downloadZipProgress.percent}%</span>
            </div>
            <div className="w-full bg-emerald-200/60 rounded-full h-2 overflow-hidden">
              <div
                className="bg-emerald-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${downloadZipProgress.percent}%` }}
              />
            </div>
          </div>
        )}

        {/* Progress bar during restore */}
        {zipRestoreProgress && (
          <div className="space-y-1.5 p-3.5 bg-emerald-50 rounded-2xl border border-emerald-200">
            <div className="flex justify-between text-xs font-bold text-emerald-900">
              <span className="flex items-center gap-1.5">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-emerald-600" />
                {zipRestoreProgress.step}
              </span>
              <span>{zipRestoreProgress.percent}%</span>
            </div>
            <div className="w-full bg-emerald-200/60 rounded-full h-2 overflow-hidden">
              <div
                className="bg-emerald-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${zipRestoreProgress.percent}%` }}
              />
            </div>
          </div>
        )}

        {/* Restore Result Card */}
        {zipRestoreResult && (
          <div className="p-3.5 bg-emerald-100/70 border border-emerald-300 rounded-2xl text-xs space-y-1 text-emerald-950 font-medium">
            <div className="font-bold flex items-center gap-1.5 text-emerald-900">
              <Check className="w-4 h-4 text-emerald-700" />
              <span>Full Archive Restored Successfully</span>
            </div>
            <div className="text-[11px] text-emerald-800 font-mono">
              Restored {zipRestoreResult.clothesCount} garments · {zipRestoreResult.reportsCount} reports · {zipRestoreResult.junctionsCount} wear records · {zipRestoreResult.restoredPhotosCount} original photos
            </div>
          </div>
        )}

        {/* Buttons */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
          {/* Download ZIP */}
          <button
            type="button"
            onClick={handleDownloadZipBackup}
            disabled={isBusy}
            className="py-3.5 px-4 rounded-2xl bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs transition-all shadow-md shadow-emerald-200 flex items-center justify-center gap-2 disabled:opacity-50 active:scale-98 cursor-pointer"
          >
            {isDownloadingZip ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-white" />
                <span>{downloadZipProgress ? `${downloadZipProgress.step} (${downloadZipProgress.percent}%)` : 'Packaging Full Backup ZIP...'}</span>
              </>
            ) : (
              <>
                <FileDown className="w-4 h-4 text-emerald-200" />
                <span>Download Full Backup Archive (.ZIP)</span>
              </>
            )}
          </button>

          {/* Restore ZIP */}
          <button
            type="button"
            onClick={() => {
              if (zipFileInputRef.current) zipFileInputRef.current.value = '';
              zipFileInputRef.current?.click();
            }}
            disabled={isBusy}
            className="py-3.5 px-4 rounded-2xl border-2 border-emerald-600 hover:bg-emerald-50 text-emerald-900 font-bold text-xs transition-all flex items-center justify-center gap-2 disabled:opacity-50 active:scale-98 cursor-pointer shadow-xs hover:shadow-md"
            title="Restore entire database and photos from .ZIP backup file"
          >
            {isRestoringZip ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-emerald-700" />
                <span>Restoring Archive...</span>
              </>
            ) : (
              <>
                <FileUp className="w-4 h-4 text-emerald-700" />
                <span>Restore from Full Backup (.ZIP)</span>
              </>
            )}
          </button>
          <input
            ref={zipFileInputRef}
            type="file"
            accept=".zip,application/zip,application/x-zip-compressed,multipart/x-zip"
            onChange={handleRestoreZipFileChange}
            className="hidden"
          />
        </div>
      </div>

      {/* GLOBAL AI GENERATION MASTER CONTROL CARD (PAUSE / RESUME) */}
      <div className={`rounded-3xl p-6 sm:p-7 border-2 transition-all space-y-4 shadow-xs ${
        isAiPaused
          ? 'bg-linear-to-br from-amber-50/70 via-white to-slate-50 border-amber-300/90'
          : 'bg-linear-to-br from-purple-50/70 via-white to-indigo-50/50 border-purple-200'
      }`}>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b pb-4 border-slate-100">
          <div className="flex items-center gap-3">
            <div className={`w-11 h-11 rounded-2xl flex items-center justify-center shadow-xs shrink-0 ${
              isAiPaused ? 'bg-amber-500 text-white shadow-amber-200' : 'bg-purple-600 text-white shadow-purple-200'
            }`}>
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <span className={`text-[10px] font-bold uppercase tracking-wider font-mono ${
                  isAiPaused ? 'text-amber-700' : 'text-purple-700'
                }`}>
                  Global AI Generation Control
                </span>
                <span className="text-slate-300">·</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold font-mono border ${
                  isAiPaused
                    ? 'bg-amber-100 text-amber-800 border-amber-300'
                    : 'bg-emerald-100 text-emerald-800 border-emerald-300'
                }`}>
                  {isAiPaused ? '⏸️ AI Generation Paused (Zero Token Mode)' : '▶️ AI Generation Active'}
                </span>
              </div>
              <h3 className="text-base sm:text-lg font-bold text-slate-900 tracking-tight">
                AI 智能生成总控制开关 (AI Advisor &amp; Generation Master Switch)
              </h3>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              const nextState = !isAiPaused;
              setAiGenerationPaused(nextState);
            }}
            className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-2xl text-xs font-bold transition-all shadow-xs cursor-pointer active:scale-95 shrink-0 ${
              isAiPaused
                ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-200'
                : 'bg-amber-500 hover:bg-amber-600 text-white shadow-amber-200'
            }`}
          >
            <span>{isAiPaused ? '▶️ 一键恢复 AI 智能生成 (Resume AI)' : '⏸️ 暂停全部 AI 生成 (Pause AI)'}</span>
          </button>
        </div>

        <div className={`p-4 rounded-2xl text-xs space-y-2 border ${
          isAiPaused ? 'bg-amber-50/80 border-amber-200/90 text-amber-950' : 'bg-purple-50/60 border-purple-100 text-purple-950'
        }`}>
          <div className="flex items-start gap-2.5">
            <span className="text-sm shrink-0 mt-0.5">{isAiPaused ? '🛡️' : '✨'}</span>
            <div className="space-y-1 leading-relaxed">
              <p className="font-semibold">
                {isAiPaused
                  ? '当前已暂停所有 AI 顾问、单品人体工学诊断、穿戴日记评估与字典代码生成调用。'
                  : 'AI 智能生成已处于启用状态，支持在单品、日记与衣橱顾问中运行 Gemini 进行穿搭与体感推导。'}
              </p>
              <p className="text-[11px] opacity-85">
                {isAiPaused
                  ? '零 Token 消耗、零多余等待，重新构建与载入应用更加快速。所有算法与提示词完整保留未被删除，您可随时再次开启。推荐使用 Google NotebookLM 承担深度分析与播客生成，获得无偏见的纯客观洞察！'
                  : '可随时点击上方按钮暂停，节约个人 API Quota 额度并将原始客观数据导出至 Google NotebookLM 分析。'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* PERSONAL GOOGLE AI STUDIO / GEMINI API KEY CARD */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-purple-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-purple-100 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-purple-600 text-white flex items-center justify-center shadow-xs">
              <Key className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950 flex items-center gap-2">
                <span>Personal Google AI Studio API Key</span>
                <span className="px-2 py-0.5 rounded-full bg-purple-100 text-purple-800 text-[10px] font-bold font-mono">
                  {selectedModel}
                </span>
              </h3>
              <p className="text-xs text-slate-500">
                Use your free personal Google AI Studio key for wearing analysis and clothing diagnostics with your private quota.
              </p>
            </div>
          </div>
          <span className={`px-2.5 py-1 rounded-full font-bold text-[10px] font-mono self-start sm:self-auto ${
            customKey ? 'bg-purple-100 text-purple-900 border border-purple-200' : 'bg-slate-100 text-slate-700'
          }`}>
            {customKey ? 'Personal Key Active' : 'System Default Active'}
          </span>
        </div>

        <div className="bg-purple-50/60 rounded-2xl p-3.5 border border-purple-100 space-y-2 text-xs text-purple-950">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <span className="font-semibold text-purple-900">
              Why use your free personal Google AI Studio key?
            </span>
            <a
              href="https://aistudio.google.com/app/apikey"
              target="_blank"
              rel="noopener noreferrer"
              className="text-purple-700 hover:text-purple-900 font-bold underline flex items-center gap-1 text-[11px]"
            >
              <span>Get Free Key at Google AI Studio</span>
              <ExternalLink className="w-3 h-3" />
            </a>
          </div>
          <p className="text-[11px] text-purple-800 leading-relaxed">
            Google AI Studio offers a generous free tier with zero charge. Providing your own key gives you uninterrupted analysis access, bypasses shared project limits, and guarantees private analysis logs.
          </p>
        </div>

        {/* Gemini Model Selector & Quota Advice */}
        <div className="space-y-2.5 pt-1 border-t border-slate-100">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-slate-800 block">
              Default Gemini Model &amp; Quota Optimization
            </label>
            <span className="text-[11px] font-mono text-purple-700 font-semibold">
              Currently: {selectedModel}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {/* Gemini 3.1 Flash Lite */}
            <div
              onClick={() => handleSelectModel('gemini-3.1-flash-lite')}
              className={`p-3.5 rounded-2xl border-2 cursor-pointer transition-all ${
                selectedModel === 'gemini-3.1-flash-lite'
                  ? 'border-purple-600 bg-purple-50/60 shadow-xs ring-1 ring-purple-500/20'
                  : 'border-slate-200 hover:border-slate-300 bg-white'
              }`}
            >
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs font-bold text-slate-900">Gemini 3.1 Flash Lite</span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold font-mono bg-emerald-100 text-emerald-800 border border-emerald-200">
                  Quota Saver (Lightweight)
                </span>
              </div>
              <p className="text-[11px] text-slate-600 leading-relaxed">
                Uses minimal tokens and lowest latency with generous quota limits. Best for rapid everyday wear queries while conserving project credit budget.
              </p>
            </div>

            {/* Gemini 3.8 Flash */}
            <div
              onClick={() => handleSelectModel('gemini-3.8-flash')}
              className={`p-3.5 rounded-2xl border-2 cursor-pointer transition-all ${
                selectedModel === 'gemini-3.8-flash'
                  ? 'border-purple-600 bg-purple-50/60 shadow-xs ring-1 ring-purple-500/20'
                  : 'border-slate-200 hover:border-slate-300 bg-white'
              }`}
            >
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-xs font-bold text-slate-900">Gemini 3.8 Flash</span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold font-mono bg-purple-100 text-purple-800 border border-purple-200">
                  Deep Reasoning (Standard)
                </span>
              </div>
              <p className="text-[11px] text-slate-600 leading-relaxed">
                Provides deeper ergonomic reasoning and complex sensory inferences across all fabric details and weather conditions.
              </p>
            </div>
          </div>
        </div>

        {/* Input & Action buttons */}
        <div className="space-y-2.5">
          <label className="text-xs font-bold text-slate-800 block">
            Google AI Studio Gemini API Key
          </label>
          <div className="flex flex-col sm:flex-row items-stretch gap-2.5">
            <div className="relative flex-1">
              <input
                type={showKey ? 'text' : 'password'}
                value={customKey}
                onChange={(e) => setCustomKey(e.target.value)}
                placeholder="AIzaSy..."
                className="w-full pl-3.5 pr-10 py-2.5 text-xs font-mono rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-purple-500 bg-white"
              />
              <button
                type="button"
                onClick={() => setShowKey(!showKey)}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer p-1"
                title={showKey ? 'Hide key' : 'Show key'}
              >
                {showKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>

            <button
              type="button"
              onClick={handleTestAndSaveKey}
              disabled={isTestingKey}
              className="py-2.5 px-4 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs transition-all shadow-xs flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 shrink-0"
            >
              {isTestingKey ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Verifying Key...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-3.5 h-3.5 text-purple-200" />
                  <span>Test &amp; Save Key</span>
                </>
              )}
            </button>

            {customKey && (
              <button
                type="button"
                onClick={handleClearCustomKey}
                className="py-2.5 px-3 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 font-bold text-xs transition-all flex items-center justify-center gap-1 cursor-pointer shrink-0"
              >
                <span>Clear</span>
              </button>
            )}
          </div>
        </div>

        {/* Verification Result Feedback */}
        {keyTestResult && (
          <div className="space-y-3">
            <div className={`p-3.5 rounded-2xl text-xs flex items-start gap-2.5 ${
              keyTestResult.ok
                ? 'bg-emerald-50 text-emerald-900 border border-emerald-200'
                : 'bg-rose-50 text-rose-900 border border-rose-200'
            }`}>
              {keyTestResult.ok ? (
                <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              ) : (
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              )}
              <div className="space-y-0.5 flex-1">
                <span className="font-bold block">
                  {keyTestResult.ok ? 'Key Verified & Ready' : (keyTestResult.isPrepaymentDepleted ? 'Prepayment Credits Depleted (Error 402)' : 'Verification Failed')}
                </span>
                <p className="text-[11px] opacity-90 leading-relaxed">{keyTestResult.message}</p>
              </div>
            </div>

            {/* Special Prepayment Billing Guidance Box */}
            {keyTestResult.isPrepaymentDepleted && (
              <div className="p-4 rounded-2xl bg-amber-50/90 border border-amber-200 text-amber-950 space-y-2.5 text-xs">
                <div className="flex items-center gap-2 font-bold text-amber-900">
                  <Sparkles className="w-4 h-4 text-amber-600 shrink-0" />
                  <span>How to Activate Gemini &amp; Clear Prepayment Error:</span>
                </div>
                <p className="text-[11px] leading-relaxed text-amber-900">
                  Your current Google AI Studio / Google Cloud project has a prepayment billing account attached, but its balance is currently $0. Google requires credits on prepaid projects before requests can be processed.
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-1">
                  <a
                    href="https://ai.studio/projects"
                    target="_blank"
                    rel="noreferrer"
                    className="p-3 rounded-xl bg-white border border-amber-200 hover:border-amber-300 hover:shadow-xs transition-all flex flex-col justify-between gap-1 text-slate-800 group"
                  >
                    <span className="font-bold text-[11px] text-amber-900 flex items-center justify-between gap-1">
                      <span>1. Top Up Project Prepay Credits</span>
                      <ExternalLink className="w-3 h-3 text-amber-600 group-hover:translate-x-0.5 transition-transform" />
                    </span>
                    <span className="text-[10px] text-slate-500 leading-normal">
                      Manage billing &amp; add prepaid balance at ai.studio/projects
                    </span>
                  </a>

                  <a
                    href="https://aistudio.google.com/apikey"
                    target="_blank"
                    rel="noreferrer"
                    className="p-3 rounded-xl bg-white border border-amber-200 hover:border-amber-300 hover:shadow-xs transition-all flex flex-col justify-between gap-1 text-slate-800 group"
                  >
                    <span className="font-bold text-[11px] text-amber-900 flex items-center justify-between gap-1">
                      <span>2. Or Use Free Standard Key</span>
                      <ExternalLink className="w-3 h-3 text-amber-600 group-hover:translate-x-0.5 transition-transform" />
                    </span>
                    <span className="text-[10px] text-slate-500 leading-normal">
                      Create key in a standard project without prepayment and paste above
                    </span>
                  </a>
                </div>
              </div>
            )}
          </div>
        )}

        {activeKeySaved && (
          <div className="text-[11px] text-purple-700 font-medium flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5 text-purple-600" />
            <span>Saved to local browser storage. All future AI calls will use your key.</span>
          </div>
        )}
      </div>

      {/* APP FONT SIZE & DISPLAY SCALE SETTINGS CARD */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-xs">
              <Type className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950">
                App Appearance &amp; Font Size
              </h3>
              <p className="text-xs text-slate-500">
                Adjust overall application typography scale. Persists across browser refreshes and mobile devices.
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-700 font-bold text-[11px] font-mono self-start sm:self-auto">
            Current: {FONT_SCALE_OPTIONS.find(o => o.key === fontScale)?.label} ({FONT_SCALE_OPTIONS.find(o => o.key === fontScale)?.badge})
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
          {FONT_SCALE_OPTIONS.map((opt) => {
            const isSelected = opt.key === fontScale;
            return (
              <button
                key={opt.key}
                type="button"
                onClick={() => handleScaleSelect(opt.key)}
                className={`p-4 rounded-2xl border-2 text-left transition-all cursor-pointer flex flex-col justify-between gap-2.5 active:scale-[0.99] ${
                  isSelected
                    ? 'border-indigo-600 bg-indigo-50/70 shadow-xs ring-2 ring-indigo-200'
                    : 'border-slate-200 hover:border-slate-300 bg-slate-50/50'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-sm text-slate-900">{opt.label}</span>
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded-md font-bold ${
                    isSelected ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-700'
                  }`}>
                    {opt.badge} · {opt.rootPx}
                  </span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  {opt.description}
                </p>
                {opt.key === 'comfortable' && (
                  <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-300 self-start">
                    <Sparkles className="w-3 h-3 text-emerald-600" />
                    Recommended Setting
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* GOOGLE NOTEBOOKLM COLLABORATION & AI KNOWLEDGE SOURCES CARD */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-indigo-200/90 shadow-sm space-y-5 relative overflow-hidden">
        {/* Decorative corner glow */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-linear-to-bl from-indigo-100/60 via-purple-50/40 to-transparent pointer-events-none rounded-bl-full" />

        {/* Card Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4 relative z-10">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-linear-to-tr from-indigo-600 via-indigo-700 to-purple-600 text-white flex items-center justify-center shadow-md shadow-indigo-200 shrink-0">
              <BookOpen className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-700 font-mono">
                  AI Collaboration &amp; External RAG
                </span>
                <span className="text-slate-300">·</span>
                <span className="text-[10px] font-semibold text-purple-700 bg-purple-50 px-2 py-0.5 rounded-md border border-purple-200">
                  Google NotebookLM Ready
                </span>
                <span className="text-[10px] font-bold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                  ✓ 100% 纯客观事实 · 已排除所有 AI 预分析报告
                </span>
              </div>
              <h3 className="text-lg font-bold text-slate-950 tracking-tight">
                NotebookLM Knowledge Base Exporter
              </h3>
            </div>
          </div>

          <a
            href="https://notebooklm.google.com"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-50 hover:bg-slate-100 text-indigo-600 hover:text-indigo-800 font-semibold text-xs border border-slate-200 transition-colors self-start sm:self-auto cursor-pointer"
          >
            <span>notebooklm.google.com</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>

        {/* Explanation & Workflow Steps */}
        <div className="space-y-3 relative z-10 text-xs text-slate-600">
          <p className="leading-relaxed">
            协同 <strong>Google NotebookLM</strong> 进行深度智能分析。导出结构化 Markdown 档案，包含您全部衣物客观参数、穿戴体感日记、洗护状态与双语名词定义。<strong>所有 AI 预生成报告已全部被自动排除</strong>，让 NotebookLM 基于 100% 真实纯粹的人类体感数据生成 <strong>Audio Overviews</strong> 播客音频、推导穿搭舒适度并回答问题，彻底避免已有 AI 的二次偏见。
          </p>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-2.5 pt-1">
            <div className="p-3 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1">
              <span className="text-[10px] font-bold text-indigo-600 font-mono uppercase tracking-wide">Step 1</span>
              <h4 className="font-bold text-slate-800 text-xs">Download Knowledge Pack</h4>
              <p className="text-[11px] text-slate-500">Download the bundled ZIP or individual topical Markdown (.md) sources below.</p>
            </div>
            <div className="p-3 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1">
              <span className="text-[10px] font-bold text-indigo-600 font-mono uppercase tracking-wide">Step 2</span>
              <h4 className="font-bold text-slate-800 text-xs">Upload to NotebookLM</h4>
              <p className="text-[11px] text-slate-500">In notebooklm.google.com, click "Add Source" and upload the markdown files.</p>
            </div>
            <div className="p-3 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-1">
              <span className="text-[10px] font-bold text-indigo-600 font-mono uppercase tracking-wide">Step 3</span>
              <h4 className="font-bold text-slate-800 text-xs">Analyze &amp; Listen</h4>
              <p className="text-[11px] text-slate-500">Generate an Audio Overview podcast, ask fit comparisons, or query wash schedules.</p>
            </div>
          </div>
        </div>

        {/* Primary Action Strip: 1-Click ZIP Download */}
        <div className="pt-2 relative z-10">
          <div className="p-4 sm:p-5 rounded-2xl bg-linear-to-r from-indigo-50 via-purple-50/70 to-slate-50 border border-indigo-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="space-y-0.5">
              <div className="flex items-center gap-2">
                <FileArchive className="w-4 h-4 text-indigo-600" />
                <h4 className="font-bold text-slate-900 text-sm">
                  Complete NotebookLM Wardrobe Knowledge Pack (.zip)
                </h4>
              </div>
              <p className="text-xs text-slate-600">
                Includes all 4 topical Markdown files + Quickstart Guide with ready-to-use analysis prompts.
              </p>
            </div>

            <button
              type="button"
              onClick={handleDownloadNotebookLmZip}
              disabled={isDownloadingNotebookLmZip}
              className="py-2.5 px-5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs transition-all shadow-md shadow-indigo-200 flex items-center justify-center gap-2 shrink-0 disabled:opacity-50 active:scale-95 cursor-pointer"
            >
              <Download className="w-4 h-4" />
              <span>{isDownloadingNotebookLmZip ? 'Compiling Pack...' : 'Download Knowledge Pack (ZIP)'}</span>
            </button>
          </div>
        </div>

        {/* 4 Topical Knowledge Sources Grid */}
        <div className="space-y-2 pt-2 relative z-10">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider font-mono">
              Individual Topical Source Files
            </h4>
            <span className="text-[11px] text-slate-400">4 structured files ready for direct upload</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {notebookLmExportService.getAllTopicDocs().map((doc) => (
              <div
                key={doc.id}
                className="p-4 rounded-2xl border border-slate-200 bg-white hover:border-indigo-300 hover:shadow-xs transition-all flex flex-col justify-between space-y-3"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-1.5">
                    <span className="text-[10px] font-mono font-bold text-indigo-600 uppercase tracking-wide px-2 py-0.5 rounded-md bg-indigo-50 border border-indigo-100">
                      {doc.filename}
                    </span>
                    <span className="text-[10px] font-mono text-slate-400">
                      {doc.wordCount.toLocaleString()} words
                    </span>
                  </div>
                  <h5 className="font-bold text-slate-900 text-sm">
                    {doc.title}
                  </h5>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">
                    {doc.description}
                  </p>
                </div>

                <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => notebookLmExportService.downloadFile(doc.filename, doc.content)}
                    className="flex-1 py-2 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer active:scale-98"
                  >
                    <Download className="w-3.5 h-3.5 text-indigo-300" />
                    <span>Download .md</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => handleOpenNotebookLmPreview(doc.id)}
                    className="py-2 px-3 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer active:scale-98"
                    title="Inspect markdown content and copy text"
                  >
                    <BookOpen className="w-3.5 h-3.5 text-slate-500" />
                    <span>Preview &amp; Copy</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* OFFLINE BACKUP & EXPORT initialData.json CARD */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-slate-900 text-white flex items-center justify-center shadow-xs">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950">
                Offline Backup & initialData.json
              </h3>
              <p className="text-xs text-slate-500">
                100% local JSON export containing all garments, reports, and junction records. Use this file directly in Android Studio under <code className="px-1.5 py-0.5 rounded bg-slate-100 font-mono text-[11px] text-slate-800">app/src/main/assets/initialData.json</code>.
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 font-bold text-[10px] font-mono self-start sm:self-auto">
            Ready for Android Room Seed
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
          {/* Download initialData.json */}
          <button
            type="button"
            onClick={handleExportJsonClick}
            disabled={isBusy}
            className="py-3.5 px-4 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs transition-all shadow-md shadow-slate-200 flex items-center justify-center gap-2 disabled:opacity-50 active:scale-98 cursor-pointer"
          >
            <FileDown className="w-4 h-4 text-emerald-400" />
            <span>Download Full initialData.json</span>
          </button>

          {/* Import JSON File */}
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isBusy}
            className="py-3.5 px-4 rounded-2xl border border-slate-300 hover:bg-slate-100 text-slate-800 font-bold text-xs transition-all flex items-center justify-center gap-2 disabled:opacity-50 active:scale-98 cursor-pointer"
          >
            <FileUp className="w-4 h-4 text-indigo-600" />
            <span>Restore / Import JSON File</span>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json"
            onChange={handleImportFileChange}
            className="hidden"
          />
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500 pt-2 border-t border-slate-100 font-mono text-[11px]">
          <span>
            Current snapshot: {clothesCount} garments · {reportsCount} wearing reports · {junctionsCount} junctions
          </span>
          <button
            type="button"
            onClick={handleCopyJsonClick}
            className="text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1 cursor-pointer"
          >
            {copiedNotice ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-600" />
                <span className="text-emerald-600 font-bold">Copied to Clipboard!</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" />
                <span>Copy Entire JSON to Clipboard</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* WEARING REPORTS DEDUPLICATION TOOL */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2.5">
            <div className={`w-10 h-10 rounded-2xl flex items-center justify-center shadow-xs ${detectedDuplicateReports.length > 0 ? 'bg-amber-500 text-white' : 'bg-indigo-600 text-white'}`}>
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950">
                Wearing Reports Deduplication
              </h3>
              <p className="text-xs text-slate-500">
                Reports are strictly identified by their unique record ID. Reports with distinct IDs (including entries with &quot;copy&quot; in the title or similar wear dates) represent separate wearing sessions and are safely preserved. Only duplicate rows sharing the exact same record ID are cleaned.
              </p>
            </div>
          </div>
          <span className={`px-3 py-1 rounded-full text-[11px] font-mono font-bold self-start sm:self-auto ${detectedDuplicateReports.length > 0 ? 'bg-amber-100 text-amber-900' : 'bg-emerald-100 text-emerald-800'}`}>
            {detectedDuplicateReports.length > 0 ? `${detectedDuplicateReports.length} Duplicate Group(s) Detected` : 'All Reports 100% Unique'}
          </span>
        </div>

        {detectedDuplicateReports.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-bold text-slate-800">
              Identified Duplicate Groups to Merge:
            </p>
            <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
              {detectedDuplicateReports.map((g, idx) => (
                <div key={idx} className="p-3 bg-amber-50/70 border border-amber-200 rounded-xl text-xs space-y-1">
                  <div className="flex items-center justify-between font-bold text-amber-950">
                    <span>Canonical: {g.canonical['Name']}</span>
                    <span className="text-[10px] font-mono bg-amber-200/80 px-2 py-0.5 rounded-md">{g.reason}</span>
                  </div>
                  <div className="text-[11px] text-amber-800 font-mono">
                    Duplicates to remove &amp; merge: {g.copies.map(c => c['Name']).join(', ')}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-50 p-4 rounded-2xl border border-slate-200">
          <div className="space-y-1">
            <p className="text-xs font-bold text-slate-800">
              {detectedDuplicateReports.length > 0 
                ? 'Click below to merge duplicate report copies into single clean reports without losing photos, tags, or wash records.' 
                : 'Zero duplicate wearing reports in your journal. All report entries are distinct.'}
            </p>
            <p className="text-[11px] text-slate-500 leading-relaxed">
              Canonical report is retained, all pictures are safely aggregated, and redundant records are permanently purged.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={handleDeduplicateReportsClick}
              disabled={deduping}
              className={`py-2.5 px-4 rounded-xl font-bold text-xs transition-all flex items-center gap-2 shadow-xs cursor-pointer active:scale-95 disabled:opacity-50 ${
                detectedDuplicateReports.length > 0 
                  ? 'bg-amber-600 hover:bg-amber-700 text-white shadow-amber-200' 
                  : 'bg-slate-800 hover:bg-slate-900 text-white'
              }`}
            >
              {deduping ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Deduplicating Reports...</span>
                </>
              ) : (
                <>
                  <Check className="w-3.5 h-3.5" />
                  <span>Merge Duplicate Reports</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={handleDeduplicateAllClick}
              disabled={deduping}
              className="py-2.5 px-4 rounded-xl font-bold text-xs bg-indigo-600 hover:bg-indigo-700 text-white shadow-xs transition-all flex items-center gap-2 cursor-pointer active:scale-95 disabled:opacity-50"
            >
              {deduping ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Cleaning All...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-3.5 h-3.5 text-indigo-200" />
                  <span>Run Full Deduplication (Reports &amp; Sessions)</span>
                </>
              )}
            </button>
          </div>
        </div>

        {reportDedupeResult && (
          <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-900 space-y-1.5 font-mono">
            <div className="flex items-center gap-2 font-bold">
              <Check className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>
                Wearing Reports: Cleaned {reportDedupeResult.removed} duplicate report(s). Total reports before: {reportDedupeResult.before} → after: {reportDedupeResult.after}.
              </span>
            </div>
            {reportDedupeResult.details.length > 0 && (
              <ul className="text-[11px] text-emerald-800 list-disc list-inside space-y-0.5 pt-1">
                {reportDedupeResult.details.map((d, i) => (
                  <li key={i}>{d}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>

      {/* DUPLICATE JUNCTIONS SCAN & CLEANUP TOOL */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2.5">
            <div className={`w-10 h-10 rounded-2xl flex items-center justify-center shadow-xs ${detectedDuplicateCount > 0 ? 'bg-amber-500 text-white' : 'bg-emerald-600 text-white'}`}>
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950">
                Session Garment Uniqueness & Deduplication
              </h3>
              <p className="text-xs text-slate-500">
                Rule: Within any single wearing report, each garment appears at most once. (Same garment in separate sessions on the same day is valid and preserved).
              </p>
            </div>
          </div>
          <span className={`px-3 py-1 rounded-full text-[11px] font-mono font-bold self-start sm:self-auto ${detectedDuplicateCount > 0 ? 'bg-amber-100 text-amber-900' : 'bg-emerald-100 text-emerald-800'}`}>
            {detectedDuplicateCount > 0 ? `${detectedDuplicateCount} Duplicate Rows Detected` : 'All Sessions 100% Unique'}
          </span>
        </div>

        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-50 p-4 rounded-2xl border border-slate-200">
          <div className="space-y-1">
            <p className="text-xs font-bold text-slate-800">
              {detectedDuplicateCount > 0 
                ? 'Duplicates found in one or more wearing reports (e.g. duplicate garments added by previous sync).' 
                : 'Zero duplicate garments in any session. Database integrity verified.'}
            </p>
            <p className="text-[11px] text-slate-500 leading-relaxed">
              When cleaning duplicates, the row with a recorded wash date, highest duration, or most recent timestamp is strictly preserved. Redundant orphan clones are permanently purged.
            </p>
          </div>

          <button
            type="button"
            onClick={handleDeduplicateClick}
            disabled={deduping}
            className={`shrink-0 py-2.5 px-4 rounded-xl font-bold text-xs transition-all flex items-center gap-2 shadow-xs cursor-pointer active:scale-95 disabled:opacity-50 ${
              detectedDuplicateCount > 0 
                ? 'bg-amber-600 hover:bg-amber-700 text-white shadow-amber-200' 
                : 'bg-slate-800 hover:bg-slate-900 text-white'
            }`}
          >
            {deduping ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Scanning & Cleaning...</span>
              </>
            ) : (
              <>
                <Check className="w-3.5 h-3.5" />
                <span>Clean & Merge Duplicates</span>
              </>
            )}
          </button>
        </div>

        {dedupeResult && (
          <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-900 flex items-center gap-2 font-mono">
            <Check className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>
              Cleaned {dedupeResult.removed} duplicate rows. Records before: {dedupeResult.before} → after: {dedupeResult.after}.
            </span>
          </div>
        )}
      </div>

      {/* Database Schema & Junction Wash Date Metrics */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <Database className="w-4 h-4 text-slate-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Database Records & Wash Date Status
            </h3>
          </div>
          <span className="text-[11px] font-mono text-slate-400">
            Local JSON Collections
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
            <div className="flex items-center gap-2 text-slate-500 text-xs">
              <Shirt className="w-3.5 h-3.5" />
              <span>Garments</span>
            </div>
            <div className="text-2xl font-black font-mono text-slate-900">
              {clothesCount}
            </div>
            <div className="text-[10px] text-slate-400">Collection: clothes</div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-1">
            <div className="flex items-center gap-2 text-slate-500 text-xs">
              <BookOpen className="w-3.5 h-3.5" />
              <span>Wearing Reports</span>
            </div>
            <div className="text-2xl font-black font-mono text-slate-900">
              {reportsCount}
            </div>
            <div className="text-[10px] text-slate-400">Collection: wearing_reports</div>
          </div>

          <div className="p-4 rounded-2xl bg-purple-50/60 border border-purple-100 space-y-1">
            <div className="flex items-center gap-2 text-purple-700 text-xs font-medium">
              <Link2 className="w-3.5 h-3.5" />
              <span>Junction Records</span>
            </div>
            <div className="text-2xl font-black font-mono text-purple-950">
              {junctionsCount}
            </div>
            <div className="text-[10px] text-purple-600">Collection: report_clothes_junctions</div>
          </div>
        </div>

        {/* Junction Wash Date Detail Bar */}
        <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-2">
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-medium">
            <span className="text-slate-700 font-bold">Junction Wash Date Breakdown</span>
            <div className="flex items-center gap-3 font-mono text-[11px]">
              <span className="text-emerald-700">Clean: {cleanJunctions}</span>
              <span className="text-amber-700">Need Wash: {needWashJunctions}</span>
              <span className="text-purple-700 font-bold">With Wash Date: {junctionsWithWashDate}</span>
            </div>
          </div>
          <div className="w-full bg-slate-200 h-2.5 rounded-full overflow-hidden flex">
            <div
              className="bg-emerald-500 h-full"
              style={{ width: `${junctionsCount > 0 ? (cleanJunctions / junctionsCount) * 100 : 0}%` }}
              title={`Clean: ${cleanJunctions}`}
            />
            <div
              className="bg-amber-400 h-full"
              style={{ width: `${junctionsCount > 0 ? (needWashJunctions / junctionsCount) * 100 : 0}%` }}
              title={`Need Wash: ${needWashJunctions}`}
            />
          </div>
          <p className="text-[11px] text-slate-500 leading-relaxed">
            All wash dates entered into the junction table remain strictly as you saved them. Saving to local disk preserves these exact values in your local database.
          </p>
        </div>
      </div>

      {/* DATABASE LIFECYCLE MANAGEMENT (EMPTY DATABASE & RESET TO SEED) */}
      <div className="bg-white rounded-3xl p-6 sm:p-7 border-2 border-red-100/80 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-red-50 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center border border-red-200">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-950 flex items-center gap-2">
                <span>Database Lifecycle &amp; Reset Management</span>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-red-100 text-red-800 font-bold uppercase tracking-wider">
                  Admin Tools
                </span>
              </h3>
              <p className="text-xs text-slate-500">
                Instantly empty the entire local database or restore it back to original seed data.
              </p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
          {/* Reset to Seed Data */}
          <div className="p-4 rounded-2xl bg-amber-50/60 border border-amber-200/80 flex flex-col justify-between gap-3">
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-amber-900 font-bold text-sm">
                <RotateCcw className="w-4 h-4 text-amber-600" />
                <span>Reset to Seed Data</span>
              </div>
              <p className="text-xs text-amber-800 leading-relaxed">
                Restore the default curated seed collection (all initial garments, wearing logs, sensory fit analyses, and junction links). Overwrites existing records with original clean seed.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowResetSeedConfirmModal(true)}
              disabled={isBusy || isResettingDb}
              className="py-2.5 px-4 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 active:scale-98"
            >
              <RotateCcw className="w-3.5 h-3.5 text-amber-100" />
              <span>Reset Database to Seed</span>
            </button>
          </div>

          {/* Empty Database */}
          <div className="p-4 rounded-2xl bg-red-50/60 border border-red-200/80 flex flex-col justify-between gap-3">
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-red-950 font-bold text-sm">
                <Trash2 className="w-4 h-4 text-red-600" />
                <span>Empty Database</span>
              </div>
              <p className="text-xs text-red-800 leading-relaxed">
                Wipe all garments, wearing journals, and wear duration logs from the database, giving you a completely blank slate for fresh data entry.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowEmptyConfirmModal(true)}
              disabled={isBusy || isResettingDb}
              className="py-2.5 px-4 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 active:scale-98"
            >
              <Trash2 className="w-3.5 h-3.5 text-red-100" />
              <span>Empty Entire Database</span>
            </button>
          </div>
        </div>
      </div>

      {/* CONFIRM EMPTY DATABASE MODAL */}
      <ConfirmModal
        isOpen={showEmptyConfirmModal}
        onClose={() => setShowEmptyConfirmModal(false)}
        onConfirm={handleExecuteEmptyDatabase}
        title="Empty Entire Database?"
        variant="danger"
        icon="trash"
        confirmLabel="Yes, Empty Database"
        cancelLabel="Cancel"
        isLoading={isResettingDb}
        message={
          <div className="space-y-3 text-xs text-slate-600">
            <p className="text-sm font-semibold text-slate-800">
              Are you sure you want to completely empty your local database?
            </p>
            <p>
              This will remove all <strong className="text-slate-900">{clothesCount} garments</strong>, <strong className="text-slate-900">{reportsCount} wearing journal reports</strong>, and <strong className="text-slate-900">{junctionsCount} wear records</strong> from local storage and disk.
            </p>
            <div className="p-3 bg-red-50 border border-red-200 rounded-xl space-y-2 text-red-900">
              <label className="flex items-center gap-2.5 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={deletePhotosWithEmpty}
                  onChange={(e) => setDeletePhotosWithEmpty(e.target.checked)}
                  className="w-4 h-4 text-red-600 rounded border-red-300 focus:ring-red-500"
                />
                <span className="font-semibold text-xs text-red-950">
                  Also delete local uploaded garment photos from disk
                </span>
              </label>
              <p className="text-[11px] text-red-700">
                Tip: You can use &quot;Download Full Backup (.zip)&quot; or &quot;Download initialData.json&quot; above before emptying if you want a backup.
              </p>
            </div>
          </div>
        }
      />

      {/* CONFIRM RESET TO SEED MODAL */}
      <ConfirmModal
        isOpen={showResetSeedConfirmModal}
        onClose={() => setShowResetSeedConfirmModal(false)}
        onConfirm={handleExecuteResetToSeed}
        title="Reset Database to Seed Data?"
        variant="warning"
        icon="alert"
        confirmLabel="Yes, Reset to Seed"
        cancelLabel="Cancel"
        isLoading={isResettingDb}
        message={
          <div className="space-y-3 text-xs text-slate-600">
            <p className="text-sm font-semibold text-slate-800">
              Are you sure you want to reset the database to original seed data?
            </p>
            <p>
              This will restore the original curated seed database (garments, sensory reports, wash statuses, and wear records). Any new items created outside the seed dataset will be replaced.
            </p>
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-amber-900">
              <p className="text-[11px] leading-relaxed">
                Your original initial dataset will be loaded into local storage and synced to disk immediately.
              </p>
            </div>
          </div>
        }
      />

      {/* RESTORE CONFIRMATION & FIELD RESOLUTION MODAL - FULL SCREEN MOBILE FRIENDLY */}
      {showRestoreConfirmModal && pendingRestoreFile && (
        <div className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto">
          {/* Header */}
          <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between shrink-0 shadow-2xs w-full max-w-full overflow-x-hidden">
            <div className="flex items-center gap-2.5 min-w-0">
              <button
                type="button"
                onClick={cancelZipRestore}
                className="p-1.5 sm:p-2 -ml-1 sm:-ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
                title="Back / Cancel"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <div className="w-9 h-9 rounded-2xl bg-emerald-100 text-emerald-800 flex items-center justify-center shrink-0">
                <FolderArchive className="w-5 h-5 text-emerald-700" />
              </div>
              <div className="min-w-0">
                <h3 className="text-sm sm:text-base font-bold text-slate-900 truncate flex items-center gap-2">
                  <span>Restore Full Backup Archive (.ZIP)</span>
                  <span className="hidden sm:inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                    <ShieldCheck className="w-3 h-3 text-emerald-600" />
                    <span>新字段防删保护 (Field Protection)</span>
                  </span>
                </h3>
                <p className="text-[11px] sm:text-xs text-slate-500 font-mono hidden xs:block truncate">
                  Restore database, wear journals, wash logs, photos &amp; manage new fields
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={cancelZipRestore}
              className="p-1.5 sm:p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Scrollable Body */}
          <div className="flex-1 overflow-y-auto overflow-x-hidden p-4 sm:p-8 bg-slate-50/50 w-full max-w-full">
            <div className="max-w-2xl mx-auto space-y-5 bg-white p-5 sm:p-7 rounded-3xl border border-slate-200/90 shadow-xs">
              {isInspectingZip ? (
                <div className="py-14 flex flex-col items-center justify-center space-y-3 text-center">
                  <Loader2 className="w-8 h-8 animate-spin text-emerald-600" />
                  <p className="text-sm font-bold text-slate-800">
                    正在分析 ZIP 备份并检测新字段差异...
                  </p>
                  <p className="text-xs text-slate-500">
                    Scanning archive records, custom fields, and photos...
                  </p>
                </div>
              ) : (
                <>
                  {/* File & Archive Stats */}
                  <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl space-y-2.5 text-xs">
                    <div className="flex justify-between items-center gap-2">
                      <span className="text-slate-500 font-medium">备份文件 (Archive):</span>
                      <span className="font-bold text-slate-900 font-mono truncate max-w-[260px]" title={pendingRestoreFile.name}>
                        {pendingRestoreFile.name}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-slate-500 font-medium">文件大小 (Size):</span>
                      <span className="font-bold text-emerald-700 font-mono">
                        {(pendingRestoreFile.size / (1024 * 1024)).toFixed(2)} MB
                      </span>
                    </div>

                    {inspectionResult && (
                      <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 pt-2 border-t border-slate-200/70">
                        <div className="p-2 bg-white rounded-xl border border-slate-100 text-center">
                          <div className="text-[10px] text-slate-500">备份衣服</div>
                          <div className="text-xs font-bold text-slate-800 font-mono">{inspectionResult.backupClothesCount} 件</div>
                        </div>
                        <div className="p-2 bg-white rounded-xl border border-slate-100 text-center">
                          <div className="text-[10px] text-slate-500">备份日记</div>
                          <div className="text-xs font-bold text-slate-800 font-mono">{inspectionResult.backupReportsCount} 篇</div>
                        </div>
                        <div className="p-2 bg-white rounded-xl border border-slate-100 text-center">
                          <div className="text-[10px] text-slate-500">备份照片</div>
                          <div className="text-xs font-bold text-slate-800 font-mono">{inspectionResult.backupPhotosCount} 张</div>
                        </div>
                        <div className="p-2 bg-white rounded-xl border border-slate-100 text-center">
                          <div className="text-[10px] text-slate-500">字典字段</div>
                          <div className="text-xs font-bold text-slate-800 font-mono">{inspectionResult.currentFieldDefsCount} 项</div>
                        </div>
                        <div className="p-2 bg-white rounded-xl border border-slate-100 text-center col-span-2 sm:col-span-1">
                          <div className="text-[10px] text-slate-500">穿透预设</div>
                          <div className="text-xs font-bold text-slate-800 font-mono">{inspectionResult.backupPresetsCount ?? inspectionResult.currentPresetsCount ?? 0} 个</div>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Protection Notice Banner */}
                  <div className="p-4 bg-emerald-50/80 border border-emerald-200 rounded-2xl text-xs text-emerald-950 space-y-1.5 shadow-2xs">
                    <div className="flex items-center gap-2 font-bold text-emerald-900 text-sm">
                      <ShieldCheck className="w-5 h-5 text-emerald-700 shrink-0" />
                      <span>恢复保护模式：当前新字段默认不删除 (New Fields Will Not Be Deleted)</span>
                    </div>
                    <p className="text-[11px] leading-relaxed text-emerald-800">
                      从旧版 ZIP 备份恢复时，您在当前系统中创建的新字段（如 aa2, aa3 等）不会被意外覆盖或丢失。系统已默认将所有新字段设为【保留】。您可以逐项确认保留、重命名（如改为 aa2 或 aa3 等其他字段名）或主动删除。
                    </p>
                  </div>

                  {/* FIELD RESOLUTION SECTION */}
                  <div className="space-y-3 pt-1">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Layers className="w-4 h-4 text-slate-700" />
                        <h4 className="text-sm font-bold text-slate-900">
                          字段迁移与确认 (Field Migration &amp; Protection)
                        </h4>
                      </div>
                      <span className="text-[11px] text-slate-500 font-mono">
                        {(inspectionResult?.allDetectedFields.length || 0) + additionalFieldsToAdd.length} 个字段
                      </span>
                    </div>

                    {/* Detected fields list */}
                    {inspectionResult && inspectionResult.allDetectedFields.length > 0 ? (
                      <div className="space-y-2.5 max-h-[320px] overflow-y-auto pr-1">
                        {inspectionResult.allDetectedFields.map((f) => {
                          const action = fieldActions[f.fieldName] || 'keep';
                          const isRenamed = action === 'rename';
                          const targetRename = fieldRenames[f.fieldName] || '';

                          return (
                            <div
                              key={f.fieldName}
                              className={`p-3 rounded-2xl border transition-all space-y-2.5 ${
                                action === 'keep'
                                  ? 'bg-emerald-50/40 border-emerald-200'
                                  : action === 'rename'
                                  ? 'bg-amber-50/50 border-amber-200'
                                  : 'bg-red-50/40 border-red-200 opacity-80'
                              }`}
                            >
                              <div className="flex items-start justify-between gap-2">
                                <div className="min-w-0">
                                  <div className="flex items-center gap-2 flex-wrap">
                                    <span className="font-bold text-xs text-slate-900 font-mono">
                                      {f.fieldName}
                                    </span>
                                    <span className="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-700">
                                      {f.category === 'clothes_custom'
                                        ? '衣服自定义字段'
                                        : f.category === 'report_custom'
                                        ? '穿戴报告自定义字段'
                                        : '数据字典字段'}
                                    </span>
                                    {!f.isInBackup && (
                                      <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-100 text-purple-800">
                                        新字段 (New)
                                      </span>
                                    )}
                                  </div>
                                  <p className="text-[11px] text-slate-500 mt-0.5">
                                    包含于 {f.count} 条记录中
                                    {f.sampleValues.length > 0 && ` (示例值: ${f.sampleValues.join(', ')})`}
                                  </p>
                                </div>

                                {/* Action Pill Buttons */}
                                <div className="flex items-center gap-1 shrink-0 bg-white p-1 rounded-xl border border-slate-200 shadow-2xs">
                                  <button
                                    type="button"
                                    onClick={() => handleAddFieldResolutionAction(f.fieldName, 'keep')}
                                    className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                                      action === 'keep'
                                        ? 'bg-emerald-600 text-white shadow-2xs'
                                        : 'text-slate-600 hover:bg-slate-100'
                                    }`}
                                    title="保留此字段 (Keep field - will not be deleted)"
                                  >
                                    保留 (Keep)
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => handleAddFieldResolutionAction(f.fieldName, 'rename')}
                                    className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                                      action === 'rename'
                                        ? 'bg-amber-600 text-white shadow-2xs'
                                        : 'text-slate-600 hover:bg-slate-100'
                                    }`}
                                    title="重命名或作为其他字段 (Rename / Add as other)"
                                  >
                                    改名/其他
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => handleAddFieldResolutionAction(f.fieldName, 'delete')}
                                    className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                                      action === 'delete'
                                        ? 'bg-red-600 text-white shadow-2xs'
                                        : 'text-slate-600 hover:bg-slate-100'
                                    }`}
                                    title="删除此字段 (Delete from restored records)"
                                  >
                                    删除
                                  </button>
                                </div>
                              </div>

                              {/* Rename Input if selected */}
                              {isRenamed && (
                                <div className="pt-1.5 flex items-center gap-2 bg-white/80 p-2 rounded-xl border border-amber-200">
                                  <span className="text-[11px] font-semibold text-amber-900 shrink-0">
                                    作为其他字段名引入:
                                  </span>
                                  <input
                                    type="text"
                                    placeholder="例如: aa2 或 aa3"
                                    value={targetRename}
                                    onChange={(e) => handleSetFieldRename(f.fieldName, e.target.value)}
                                    className="flex-1 px-2.5 py-1 text-xs rounded-lg border border-amber-300 focus:outline-none focus:ring-1 focus:ring-amber-500 font-mono"
                                  />
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    ) : (
                      <div className="p-3.5 bg-slate-50 border border-slate-200/80 rounded-2xl text-xs text-slate-600 space-y-1">
                        <div className="flex items-center gap-1.5 font-semibold text-slate-800">
                          <CheckCheck className="w-4 h-4 text-emerald-600" />
                          <span>当前数据与旧备份字段兼容</span>
                        </div>
                        <p className="text-[11px] text-slate-500 leading-relaxed">
                          当前全部 42 项 AI 字典定义与真实历史值将受保护，不会被旧备份覆盖。如果您希望为恢复后的记录添加其他字段（如 aa2, aa3），可点击下方按钮直接添加。
                        </p>
                      </div>
                    )}

                    {/* User-added additional fields */}
                    {additionalFieldsToAdd.length > 0 && (
                      <div className="space-y-2 pt-1">
                        <div className="text-[11px] font-bold text-slate-700">已添加的其他自定义字段:</div>
                        {additionalFieldsToAdd.map((af) => (
                          <div
                            key={af.id}
                            className="p-2.5 bg-purple-50/70 border border-purple-200 rounded-2xl flex items-center justify-between text-xs"
                          >
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-purple-950 font-mono">{af.name}</span>
                              <span className="px-1.5 py-0.5 rounded text-[10px] bg-purple-100 text-purple-800 font-semibold">
                                {af.target === 'clothes' ? '衣服' : af.target === 'reports' ? '穿戴日记' : '全部'}
                              </span>
                              {af.defaultValue && (
                                <span className="text-[11px] text-slate-500">
                                  (默认值: {af.defaultValue})
                                </span>
                              )}
                            </div>
                            <button
                              type="button"
                              onClick={() => handleRemoveAdditionalField(af.id)}
                              className="p-1 rounded-lg text-red-600 hover:bg-red-50 cursor-pointer"
                              title="移除此字段"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Add Custom Field Form / Button */}
                    {!showAddCustomFieldInput ? (
                      <button
                        type="button"
                        onClick={() => setShowAddCustomFieldInput(true)}
                        className="w-full py-2.5 px-3 rounded-2xl border border-dashed border-slate-300 hover:border-emerald-500 hover:bg-emerald-50/40 text-slate-700 hover:text-emerald-800 text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer"
                      >
                        <Plus className="w-4 h-4 text-emerald-600" />
                        <span>添加其他自定义字段 (如 aa2, aa3) / Add Other Field</span>
                      </button>
                    ) : (
                      <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-2xl space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-slate-900">
                            添加其他新字段至恢复数据 (如 aa2, aa3)
                          </span>
                          <button
                            type="button"
                            onClick={() => setShowAddCustomFieldInput(false)}
                            className="text-slate-400 hover:text-slate-600 text-xs cursor-pointer"
                          >
                            取消
                          </button>
                        </div>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                          <input
                            type="text"
                            placeholder="字段名 (例如: aa2 或 aa3)"
                            value={newFieldNameInput}
                            onChange={(e) => setNewFieldNameInput(e.target.value)}
                            className="px-2.5 py-1.5 text-xs rounded-xl border border-slate-200 bg-white font-mono focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          />
                          <select
                            value={newFieldTargetInput}
                            onChange={(e) => setNewFieldTargetInput(e.target.value as any)}
                            className="px-2.5 py-1.5 text-xs rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          >
                            <option value="clothes">仅衣服 (Garments)</option>
                            <option value="reports">仅穿戴报告 (Reports)</option>
                            <option value="both">全部衣服与报告 (Both)</option>
                          </select>
                          <input
                            type="text"
                            placeholder="默认初始值 (选填)"
                            value={newFieldDefaultInput}
                            onChange={(e) => setNewFieldDefaultInput(e.target.value)}
                            className="px-2.5 py-1.5 text-xs rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-1 focus:ring-emerald-500"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={handleAddCustomFieldToRestore}
                          disabled={!newFieldNameInput.trim()}
                          className="py-1.5 px-4 rounded-xl bg-emerald-700 hover:bg-emerald-800 disabled:opacity-50 text-white font-bold text-xs cursor-pointer transition-all"
                        >
                          确认添加字段
                        </button>
                      </div>
                    )}
                  </div>

                  {/* RESTORE STRATEGY SELECTOR */}
                  <div className="space-y-2 pt-1 border-t border-slate-100">
                    <div className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                      <Sliders className="w-3.5 h-3.5 text-slate-600" />
                      <span>恢复合并模式 (Restore Strategy)</span>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      <label
                        className={`p-3 rounded-2xl border cursor-pointer transition-all ${
                          restoreMergeStrategy === 'smart_merge'
                            ? 'bg-emerald-50/60 border-emerald-300 ring-1 ring-emerald-500/20'
                            : 'bg-white border-slate-200 hover:bg-slate-50'
                        }`}
                      >
                        <div className="flex items-start gap-2">
                          <input
                            type="radio"
                            name="restoreStrategy"
                            checked={restoreMergeStrategy === 'smart_merge'}
                            onChange={() => setRestoreMergeStrategy('smart_merge')}
                            className="mt-0.5 text-emerald-600 focus:ring-emerald-500"
                          />
                          <div>
                            <div className="text-xs font-bold text-slate-900">
                              智能安全合并 (推荐)
                            </div>
                            <div className="text-[11px] text-slate-500 leading-snug mt-0.5">
                              保留备份后新增的衣物与日记记录，并将所有保留的字段（如 aa2, aa3）无缝融合，0 数据丢失。
                            </div>
                          </div>
                        </div>
                      </label>

                      <label
                        className={`p-3 rounded-2xl border cursor-pointer transition-all ${
                          restoreMergeStrategy === 'replace_with_fields'
                            ? 'bg-emerald-50/60 border-emerald-300 ring-1 ring-emerald-500/20'
                            : 'bg-white border-slate-200 hover:bg-slate-50'
                        }`}
                      >
                        <div className="flex items-start gap-2">
                          <input
                            type="radio"
                            name="restoreStrategy"
                            checked={restoreMergeStrategy === 'replace_with_fields'}
                            onChange={() => setRestoreMergeStrategy('replace_with_fields')}
                            className="mt-0.5 text-emerald-600 focus:ring-emerald-500"
                          />
                          <div>
                            <div className="text-xs font-bold text-slate-900">
                              精确还原并注入字段
                            </div>
                            <div className="text-[11px] text-slate-500 leading-snug mt-0.5">
                              以备份记录为主体，同时将您选定的新字段（如 aa2, aa3）注入保留，新字段不被清除。
                            </div>
                          </div>
                        </div>
                      </label>
                    </div>
                  </div>

                  {/* Summary Bar */}
                  <div className="p-3 bg-slate-50 border border-slate-200/80 rounded-2xl flex flex-wrap items-center justify-between gap-2 text-xs">
                    <div className="flex items-center gap-3 flex-wrap">
                      <span className="flex items-center gap-1 font-bold text-emerald-700">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        <span>
                          {Object.values(fieldActions).filter((a) => a === 'keep').length + additionalFieldsToAdd.length} 个字段保留 (不删除)
                        </span>
                      </span>
                      {Object.values(fieldActions).filter((a) => a === 'rename').length > 0 && (
                        <span className="flex items-center gap-1 font-bold text-amber-700">
                          <Edit2 className="w-3.5 h-3.5 text-amber-600" />
                          <span>
                            {Object.values(fieldActions).filter((a) => a === 'rename').length} 个字段重命名/引入
                          </span>
                        </span>
                      )}
                      {Object.values(fieldActions).filter((a) => a === 'delete').length > 0 && (
                        <span className="flex items-center gap-1 font-bold text-red-600">
                          <Trash2 className="w-3.5 h-3.5 text-red-500" />
                          <span>
                            {Object.values(fieldActions).filter((a) => a === 'delete').length} 个字段删除
                          </span>
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] text-slate-500 font-mono">
                      ✓ 42 项 AI 字典定义安全合并
                    </span>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="px-4 sm:px-8 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] border-t border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-end gap-3 shrink-0 w-full max-w-full shadow-xs">
            <button
              type="button"
              onClick={cancelZipRestore}
              className="py-2 px-4 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold text-xs transition-all cursor-pointer"
            >
              取消 (Cancel)
            </button>
            <button
              type="button"
              onClick={() => executeZipRestore(pendingRestoreFile)}
              disabled={isInspectingZip}
              className="py-2 px-5 rounded-xl bg-emerald-700 hover:bg-emerald-800 disabled:opacity-50 text-white font-bold text-xs shadow-xs transition-all flex items-center gap-2 cursor-pointer active:scale-95"
            >
              <FileUp className="w-4 h-4 text-emerald-100" />
              <span>确认恢复备份 (安全保留新字段)</span>
            </button>
          </div>
        </div>
      )}

      {/* NotebookLM Knowledge Dossier Preview Modal */}
      <NotebookLmPreviewModal
        isOpen={isNotebookLmModalOpen}
        onClose={() => setIsNotebookLmModalOpen(false)}
        initialTopicId={notebookLmTopicId}
      />
    </div>
  );
};
