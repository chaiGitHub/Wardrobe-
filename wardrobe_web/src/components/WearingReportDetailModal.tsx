import React, { useState, useRef, useEffect, useMemo } from 'react';
import { WearingReport, ReportClothesJunction, ClothesItem, PictureAttachment, AIReportVersion } from '../types';
import { dataService, formatDuration, processImageFile } from '../services/dataService';
import { uploadReportPhotoToStorage, deleteFileFromStorage } from '../services/cloudStorageService';
import { resolveImageSource } from '../utils/imageUtils';
import { PreviousEntriesModal, PreviousEntryItem } from './PreviousEntriesModal';
import { AutoResizeTextarea } from './AutoResizeTextarea';
import { ImageViewerModal } from './ImageViewerModal';
import { CustomAttributesManager } from './CustomAttributesManager';
import { toDateKey, formatCalendarDate, getTodayDateKey } from '../utils/calendarCalculations';
import { getCustomApiKey, getSelectedModel, GeminiModelId, useAiGenerationPaused } from '../services/aiSettings';
import { fieldDictionaryService } from '../services/fieldDictionaryService';
import { FieldDictionaryDrawer } from './FieldDictionaryDrawer';
import { FieldExplainBox } from './FieldExplainBox';
import { FontSizeSwitcher } from './FontSizeSwitcher';
import {
  X, Calendar, Clock, Shirt, Sparkles, Tag, ShieldCheck, Heart, Droplets,
  Image as ImageIcon, ChevronRight, Edit3, Trash2, Save, ArrowLeft, History,
  Plus, Upload, CheckCircle2, AlertTriangle, Search, Loader2, Check, RefreshCw, RotateCcw,
  Star, Copy, FileText, Layers, Sun, Moon, ChevronDown, ChevronUp, ZoomIn, CheckSquare, Square,
  BookOpen, MessageSquare
} from 'lucide-react';
import { ConfirmModal } from './ConfirmModal';
import { JunctionAIModal } from './JunctionAIModal';
import { ImageWithFallback } from './ImageWithFallback';

export interface WearingReportDetailModalProps {
  report: WearingReport | null;
  junctions: ReportClothesJunction[];
  clothes: ClothesItem[];
  onClose: () => void;
  onSelectClothes?: (clothes: ClothesItem) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
  onDeleteReport?: (reportId: string) => void;
  onUpdateReport?: (report: WearingReport) => void;
}

export type SessionDetailModalProps = WearingReportDetailModalProps;

interface EditClothesItem {
  rowId: string;
  junctionId?: string;
  junctionAutoNumber?: number | string;
  clothesId: string;
  clothesTitle?: string;
  clothesType?: string;
  durationHours: number; // exact decimal hours
  hours: number;
  mins: number;
  washStated: 'wash' | 'need wash';
  washDate?: string;
}

export type EditGarmentItem = EditClothesItem;

const WearingReportDetailModalContent: React.FC<WearingReportDetailModalProps & { report: WearingReport }> = ({
  report,
  junctions,
  clothes,
  onClose,
  onSelectClothes,
  onSelectGarment,
  onDeleteReport,
  onUpdateReport
}) => {
  const handleSelectClothesItem = (item: ClothesItem) => {
    if (onSelectClothes) {
      onSelectClothes(item);
    } else if (onSelectGarment) {
      onSelectGarment(item);
    }
  };

  // Edit Mode State
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [uploadingPic, setUploadingPic] = useState(false);
  const [uploadPercent, setUploadPercent] = useState<number | null>(null);

  // Editable Form Fields
  const [editName, setEditName] = useState(report['Name'] || '');
  const [editWearDate, setEditWearDate] = useState(
    report['wear date'] ? toDateKey(report['wear date']) || '' : ''
  );
  const [editShoulderFeel, setEditShoulderFeel] = useState(report['shoulder feel'] || '');
  const [editMidBreast, setEditMidBreast] = useState(report['mid breast'] || '');
  const [editNightMidBreast, setEditNightMidBreast] = useState(report['night mid breast'] || '');
  const [editSweat, setEditSweat] = useState(report['sweat'] || '');
  const [editNightSweat, setEditNightSweat] = useState(report['night sweat'] || '');
  const [editLegFeel, setEditLegFeel] = useState(report['leg feel'] || '');
  const [editNightLegFeel, setEditNightLegFeel] = useState(report['night leg feel'] || '');
  const [editReportDetail, setEditReportDetail] = useState(report['report detail'] || '');
  const [editTags, setEditTags] = useState<string[]>(report['tags'] || []);
  const [newTagInput, setNewTagInput] = useState('');
  const [editPictures, setEditPictures] = useState<PictureAttachment[]>(report['Pictures'] || []);

  // Field Explanations / Ergonomic Notes for this wearing report
  const [editFieldExplanations, setEditFieldExplanations] = useState<Record<string, string>>(() => {
    return report.fieldExplanations || report.customFields?._fieldExplanations || {};
  });
  const [expandedExplainFields, setExpandedExplainFields] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setEditFieldExplanations(report.fieldExplanations || report.customFields?._fieldExplanations || {});
  }, [report]);

  // Editable Garments in this session
  const [editGarments, setEditGarments] = useState<EditGarmentItem[]>([]);
  const [addClothesSearch, setAddClothesSearch] = useState('');
  const [isAddingClothes, setIsAddingClothes] = useState(false);

  // Fullscreen Image Viewer state
  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerInitialIndex, setViewerInitialIndex] = useState(0);

  // Previous Entries Picker
  const [previousModalConfig, setPreviousModalConfig] = useState<{
    isOpen: boolean;
    fieldName: string;
    fieldLabel: string;
    currentValue?: string;
    entries: PreviousEntryItem[];
    onSelect: (val: string) => void;
  } | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Single garment wash state and feedback for independent garment wash
  const [singleWashDates, setSingleWashDates] = useState<Record<string, string>>({});
  const [washActionSuccess, setWashActionSuccess] = useState<string>('');

  // Junction table inline wash date editing state
  const [editingJunctionId, setEditingJunctionId] = useState<string | null>(null);
  const [inlineWashDate, setInlineWashDate] = useState<string>('');
  const [isUpdatingJunction, setIsUpdatingJunction] = useState<boolean>(false);

  const handleStartEditJunctionDate = (jId: string, currentVal: string) => {
    setEditingJunctionId(jId);
    setInlineWashDate(currentVal || getTodayDateKey());
  };

  const handleSaveJunctionDate = async (jId: string) => {
    setIsUpdatingJunction(true);
    try {
      await dataService.updateJunctionWashDate(jId, inlineWashDate);
      setEditingJunctionId(null);
    } catch (err) {
      console.error('Failed to update junction wash date:', err);
    } finally {
      setIsUpdatingJunction(false);
    }
  };

  const [deletingJunctionId, setDeletingJunctionId] = useState<string | null>(null);
  const [confirmModalConfig, setConfirmModalConfig] = useState<{
    isOpen: boolean;
    title: string;
    message: React.ReactNode;
    confirmLabel: string;
    variant: 'primary' | 'danger' | 'warning';
    onConfirm: () => void | Promise<void>;
  } | null>(null);

  const handleDeleteSingleJunction = (jId: string, garmentTitle: string, autoNumber?: number | string) => {
    const label = autoNumber ? `#${autoNumber} (${jId})` : jId;
    setConfirmModalConfig({
      isOpen: true,
      title: 'Delete Junction Record?',
      variant: 'danger',
      confirmLabel: 'Yes, Delete Record',
      message: (
        <div className="space-y-1.5">
          <p>
            Delete junction record <strong>{label}</strong> for <strong>"{garmentTitle}"</strong>?
          </p>
          <p className="text-[11px] text-slate-500">
            This will remove ONLY this individual junction link and will keep other garment and wearing records intact.
          </p>
        </div>
      ),
      onConfirm: async () => {
        setConfirmModalConfig(null);
        setDeletingJunctionId(jId);
        try {
          await dataService.deleteJunction(jId);
          const updatedReport = dataService.getReports().find(r => r.id === report.id);
          if (updatedReport && onUpdateReport) {
            onUpdateReport(updatedReport);
          }
        } catch (err) {
          console.error('Failed to delete junction:', err);
        } finally {
          setDeletingJunctionId(null);
        }
      }
    });
  };

  const handleWashSingleGarment = (clothesId: string, garmentName: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    const dateToUse = singleWashDates[clothesId] || getTodayDateKey();
    setConfirmModalConfig({
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
            <span>Wash date will be recorded as <strong>{dateToUse}</strong>.</span>
          </div>
          <p className="text-[11px] text-slate-500">
            This will reset its unwashed wear duration and increment its wash counter. You can easily undo if clicked by mistake.
          </p>
        </div>
      ),
      onConfirm: async () => {
        setConfirmModalConfig(null);
        await dataService.markGarmentWashed(clothesId, dateToUse);
        setWashActionSuccess(`Washed "${garmentName}" on ${dateToUse}. Total washed count updated.`);
        setTimeout(() => setWashActionSuccess(''), 5000);
      }
    });
  };

  // Robust multi-key and multi-format matching of linked junctions for this wearing report
  const sessionJunctions = React.useMemo(() => {
    const repId = (report.id || '').trim().toLowerCase();
    const repName = (report['Name'] || report.name || '').trim().toLowerCase();

    return junctions.filter(j => {
      const rawRep = j['wearing report id'];
      let rId = '';
      let rTitle = '';

      if (Array.isArray(rawRep) && rawRep.length > 0) {
        rId = rawRep[0]?.id || '';
        rTitle = rawRep[0]?.title || '';
      } else if (typeof rawRep === 'object' && rawRep !== null) {
        rId = (rawRep as any).id || '';
        rTitle = (rawRep as any).title || '';
      } else if (typeof rawRep === 'string') {
        rId = rawRep;
      }

      if (!rId && j.wearingReportId) rId = j.wearingReportId;
      if (!rTitle && j.wearingReportTitle) rTitle = j.wearingReportTitle;

      const rIdLower = (rId || '').trim().toLowerCase();
      const rTitleLower = (rTitle || '').trim().toLowerCase();

      // 1. Direct ID matches
      if (repId && rIdLower && rIdLower === repId) return true;
      // 2. Direct name matches (junction title equals report name)
      if (repName && rTitleLower && rTitleLower === repName) return true;
      // 3. Cross matches: junction ID equals report Name, or junction title equals report ID
      if (repName && rIdLower && rIdLower === repName) return true;
      if (repId && rTitleLower && rTitleLower === repId) return true;
      // 4. Substring / fuzzy match if report name has significant length
      if (repName && repName.length >= 5 && rTitleLower && (rTitleLower.includes(repName) || repName.includes(rTitleLower))) return true;
      if (repName && repName.length >= 5 && rIdLower && (rIdLower.includes(repName) || repName.includes(rIdLower))) return true;

      return false;
    });
  }, [junctions, report.id, report['Name'], (report as any).name]);

  // Helper to resolve clothes ID, Title, Type and authoritative GarmentEntity across varied data formats
  const extractJunctionClothesInfo = React.useCallback((j: ReportClothesJunction) => {
    const rawC = j['clothes(new)'];
    let cId = '';
    let cTitle = '';

    if (Array.isArray(rawC) && rawC.length > 0) {
      cId = rawC[0]?.id || '';
      cTitle = rawC[0]?.title || '';
    } else if (typeof rawC === 'object' && rawC !== null) {
      cId = (rawC as any).id || '';
      cTitle = (rawC as any).title || '';
    } else if (typeof rawC === 'string') {
      cId = rawC;
    }

    if (!cId && j.clothesId) cId = j.clothesId;
    if (!cTitle && j.clothesTitle) cTitle = j.clothesTitle;

    // Search clothes list by ID or by name
    const foundGarment = clothes.find(c => {
      const cName = (c['Name'] || c.name || '').trim().toLowerCase();
      return (
        (cId && c.id === cId) ||
        (cTitle && cName === cTitle.trim().toLowerCase()) ||
        (cId && cName === cId.trim().toLowerCase()) ||
        (cTitle && cName && (cName.includes(cTitle.trim().toLowerCase()) || cTitle.trim().toLowerCase().includes(cName)))
      );
    });

    const finalClothesId = foundGarment?.id || cId;
    const finalTitle = foundGarment?.['Name'] || foundGarment?.name || cTitle || 'Garment';
    const finalType = foundGarment?.['Clothes Type'] || foundGarment?.clothesType || j['Clothes Type (from clothes(new))'] || 'Dress';

    return {
      garment: foundGarment || null,
      clothesId: finalClothesId,
      clothesTitle: finalTitle,
      clothesType: finalType
    };
  }, [clothes]);

  const sessionClothes = React.useMemo(() => {
    return sessionJunctions.map(j => {
      const info = extractJunctionClothesInfo(j);
      return {
        junction: j,
        garment: info.garment,
        clothesTitle: info.clothesTitle,
        clothesType: info.clothesType
      };
    });
  }, [sessionJunctions, extractJunctionClothesInfo]);

  // State for opening dedicated Junction AI Diagnostic modal
  const [junctionForAiModal, setJunctionForAiModal] = useState<{
    junction: ReportClothesJunction;
    garment?: ClothesItem | null;
  } | null>(null);

  // Teable-Style AI Wearing Analysis Multi-Version State & Handlers
  const isAiPaused = useAiGenerationPaused();
  const [isAnalyzingWearing, setIsAnalyzingWearing] = useState(false);
  const [wearingAnalysisError, setWearingAnalysisError] = useState('');

  // Versions derived from report.aiReports or legacy 'AI Wearing Analysis'
  const reportVersions: AIReportVersion[] = React.useMemo(() => {
    if (Array.isArray(report.aiReports) && report.aiReports.length > 0) {
      return report.aiReports;
    }
    const legacy = report['AI Wearing Analysis'] || report.aiWearingAnalysis || '';
    if (legacy.trim()) {
      return [{
        id: 'initial_version',
        timestamp: report.created || new Date().toISOString(),
        title: 'Report #1',
        model: 'gemini-3.8-flash',
        content: legacy.trim(),
        isPrimary: true
      }];
    }
    return [];
  }, [report.aiReports, report['AI Wearing Analysis'], report.aiWearingAnalysis, report.created]);

  const [activeVersionId, setActiveVersionId] = useState<string>(() => {
    const primary = reportVersions.find(v => v.isPrimary);
    return primary ? primary.id : (reportVersions[0]?.id || '');
  });

  // Ensure activeVersionId stays valid
  useEffect(() => {
    if (reportVersions.length > 0 && !reportVersions.some(v => v.id === activeVersionId)) {
      const primary = reportVersions.find(v => v.isPrimary);
      setActiveVersionId(primary ? primary.id : reportVersions[0].id);
    }
  }, [reportVersions, activeVersionId]);

  const activeVersion = reportVersions.find(v => v.id === activeVersionId) || reportVersions[0];

  // In-place edit state for the selected AI report version
  const [isEditingAiReport, setIsEditingAiReport] = useState(false);
  const [editedTitle, setEditedTitle] = useState('');
  const [editedContent, setEditedContent] = useState('');
  const [isSavingAiReport, setIsSavingAiReport] = useState(false);
  const [copiedVersionId, setCopiedVersionId] = useState<string | null>(null);

  // Model selection for next generation (defaults to global setting)
  const [generationModel, setGenerationModel] = useState<GeminiModelId>(() => getSelectedModel());
  const [, setCustomFieldsVersion] = useState(0);

  // Generate a brand new version (preserves all previous reports)
  const handleGenerateNewReportVersion = async (modelToUse?: GeminiModelId) => {
    setIsAnalyzingWearing(true);
    setWearingAnalysisError("");
    const targetModel = modelToUse || generationModel;
    try {
      const dictionaryPrompt = `${fieldDictionaryService.buildDictionaryPrompt("wearingReport")}\n${fieldDictionaryService.buildDictionaryPrompt("junction")}\n${fieldDictionaryService.buildDictionaryPrompt("clothes")}`;

      const freshReport = dataService.getReports().find(r => r.id === report.id) || report;
      const combinedCustom = {
        ...(report.customFields || {}),
        ...(report.extraAttributes || {}),
        ...(freshReport.customFields || {}),
        ...(freshReport.extraAttributes || {})
      };
      const effectiveReport = {
        ...freshReport,
        ...report,
        customFields: combinedCustom,
        extraAttributes: combinedCustom
      };

      const res = await fetch("/api/ai/analyze-wearing-report", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          report: effectiveReport,
          junctions: sessionJunctions,
          clothes: sessionClothes.map(c => c.garment).filter(Boolean),
          apiKey: getCustomApiKey(),
          model: targetModel,
          dictionaryPrompt
        })
      });
      const data = await res.json();
      if (res.ok && data.analysis) {
        const newTitle = `Report #${reportVersions.length + 1}`;
        const updated = await dataService.addAIReportVersion(report.id, {
          content: data.analysis,
          title: newTitle,
          model: data.model || targetModel,
          isPrimary: reportVersions.length === 0
        });
        if (updated) {
          onUpdateReport?.(updated);
          const firstVer = updated.aiReports?.[0];
          if (firstVer) setActiveVersionId(firstVer.id);
        }
      } else {
        setWearingAnalysisError(data.error || "Failed to generate wearing report analysis.");
      }
    } catch (err: any) {
      setWearingAnalysisError(err?.message || "Error communicating with AI service.");
    } finally {
      setIsAnalyzingWearing(false);
    }
  };

  const handleStartEditingAiReport = (version: AIReportVersion) => {
    setEditedTitle(version.title || `Report`);
    setEditedContent(version.content || '');
    setIsEditingAiReport(true);
  };

  const handleSaveEditedAiReport = async () => {
    if (!activeVersion) return;
    setIsSavingAiReport(true);
    try {
      const updated = await dataService.updateAIReportVersion(report.id, activeVersion.id, {
        title: editedTitle.trim() || activeVersion.title,
        content: editedContent
      });
      if (updated) {
        onUpdateReport?.(updated);
      }
      setIsEditingAiReport(false);
    } catch (err: any) {
      setWearingAnalysisError(err?.message || 'Failed to save edited report.');
    } finally {
      setIsSavingAiReport(false);
    }
  };

  const handleSetPrimaryVersion = async (versionId: string) => {
    const updated = await dataService.setPrimaryAIReportVersion(report.id, versionId);
    if (updated) {
      onUpdateReport?.(updated);
    }
  };

  const handleDeleteAiReportVersion = async (versionId: string) => {
    const updated = await dataService.deleteAIReportVersion(report.id, versionId);
    if (updated) {
      onUpdateReport?.(updated);
      const remaining = updated.aiReports || [];
      if (remaining.length > 0) {
        const primary = remaining.find(v => v.isPrimary) || remaining[0];
        setActiveVersionId(primary.id);
      }
    }
  };

  const handleCopyAiReport = (content: string, id: string) => {
    navigator.clipboard.writeText(content);
    setCopiedVersionId(id);
    setTimeout(() => setCopiedVersionId(null), 2000);
  };

  // Custom Attributes & Specifications Handlers for this Wearing Report
  const handleAddReportCustomField = async (key: string, val: any) => {
    await dataService.setReportCustomField(report.id, key, val);
    const updated = { ...(report.customFields || report.extraAttributes || {}), [key]: val };
    report.customFields = updated;
    report.extraAttributes = updated;
    (report as any)[key] = val;
    setCustomFieldsVersion(v => v + 1);
    onUpdateReport?.({ ...report, customFields: updated, extraAttributes: updated, [key]: val });
  };

  const handleUpdateReportCustomField = async (key: string, val: any) => {
    await dataService.setReportCustomField(report.id, key, val);
    const updated = { ...(report.customFields || report.extraAttributes || {}), [key]: val };
    report.customFields = updated;
    report.extraAttributes = updated;
    (report as any)[key] = val;
    setCustomFieldsVersion(v => v + 1);
    onUpdateReport?.({ ...report, customFields: updated, extraAttributes: updated, [key]: val });
  };

  const handleDeleteReportCustomField = async (key: string) => {
    await dataService.removeReportCustomField(report.id, key);
    const updated = { ...(report.customFields || report.extraAttributes || {}) };
    delete updated[key];
    delete (updated as any)[key];
    delete (report as any)[key];
    report.customFields = updated;
    report.extraAttributes = updated;
    setCustomFieldsVersion(v => v + 1);
    onUpdateReport?.({ ...report, customFields: updated, extraAttributes: updated });
  };

  // Calculate session duration stats
  const maxDurationSec = sessionJunctions.reduce((max, j) => Math.max(max, j['duration'] || 0), 0);
  const formattedSessionDuration = formatDuration(maxDurationSec);
  const hasNeedsWash = sessionClothes.some(sc => sc.junction['wash stated'] === 'need wash');

  // Initialize edit fields when entering edit mode
  const startEdit = () => {
    setEditName(report['Name'] || '');
    setEditWearDate(report['wear date'] ? toDateKey(report['wear date']) || '' : '');
    setEditShoulderFeel(report['shoulder feel'] || '');
    setEditMidBreast(report['mid breast'] || '');
    setEditNightMidBreast(report['night mid breast'] || '');
    setEditSweat(report['sweat'] || '');
    setEditNightSweat(report['night sweat'] || '');
    setEditLegFeel(report['leg feel'] || '');
    setEditNightLegFeel(report['night leg feel'] || '');
    setEditReportDetail(report['report detail'] || '');
    setEditTags(report['tags'] || []);
    setEditPictures(report['Pictures'] || []);

    // Map junctions to editable garments with exact hours and minutes, wash date, clothes title & type
    const mapped: EditGarmentItem[] = sessionJunctions.map((j, idx) => {
      const info = extractJunctionClothesInfo(j);
      const totalSec = j['duration'] || 0;
      const totalMins = Math.round(totalSec / 60);
      const h = Math.floor(totalMins / 60);
      const m = totalMins % 60;
      const rawWashDate = j['wash date'] || j.washDate || j['washed date'] || j.washedDate || '';
      return {
        rowId: j.id || `row_${idx}_${Date.now()}`,
        junctionId: j.id,
        junctionAutoNumber: j.name,
        clothesId: info.clothesId,
        clothesTitle: info.clothesTitle,
        clothesType: info.clothesType,
        durationHours: totalSec / 3600,
        hours: h,
        mins: m,
        washStated: (j['wash stated'] as 'wash' | 'need wash') || 'need wash',
        washDate: rawWashDate ? (toDateKey(rawWashDate) || String(rawWashDate)) : ''
      };
    });
    setEditGarments(mapped);
    setEditFieldExplanations(report.fieldExplanations || report.customFields?._fieldExplanations || {});
    setExpandedExplainFields({});
    setIsEditing(true);
  };

  const cancelEdit = () => {
    setEditFieldExplanations(report.fieldExplanations || report.customFields?._fieldExplanations || {});
    setExpandedExplainFields({});
    setIsEditing(false);
    setIsAddingClothes(false);
  };

  const renderFieldExplainBox = (
    fieldKey: string,
    fieldLabel: string,
    customPlaceholder?: string
  ) => {
    return (
      <FieldExplainBox
        fieldKey={fieldKey}
        fieldLabel={fieldLabel}
        value={editFieldExplanations[fieldKey] || ''}
        onChange={(text) => {
          setEditFieldExplanations(prev => ({
            ...prev,
            [fieldKey]: text
          }));
        }}
        isExpanded={expandedExplainFields[fieldKey] || false}
        onToggleExpand={() => {
          setExpandedExplainFields(prev => ({
            ...prev,
            [fieldKey]: !prev[fieldKey]
          }));
        }}
        customPlaceholder={customPlaceholder}
        targetCategory="wearingReport"
      />
    );
  };

  const getFieldExplainText = (key: string): string => {
    return report.fieldExplanations?.[key] ||
      report.customFields?._fieldExplanations?.[key] ||
      report.customFields?.[`${key}_explain`] ||
      '';
  };

  const openPreviousPicker = (
    fieldKey: string,
    label: string,
    onSelectVal: (v: string) => void,
    currentVal?: string
  ) => {
    const resultMap = new Map<string, PreviousEntryItem>();

    // 1. Current value if present
    if (currentVal && currentVal.trim()) {
      const cv = currentVal.trim();
      resultMap.set(cv.toLowerCase(), { value: cv, count: 1 });
    }

    // 2. Typical values from AI dictionary
    const dictTypical = fieldDictionaryService.getFieldTypicalValues(fieldKey);
    dictTypical.forEach(dt => {
      const k = dt.valueEn.trim().toLowerCase();
      resultMap.set(k, {
        value: dt.valueEn.trim(),
        count: 0,
        labelCn: dt.valueCn,
        meaningCn: dt.meaningCn
      });
    });

    // 3. Historical occurrences (searches reports with worn garment sources as well as clothes)
    const historical = dataService.getFieldPreviousValues(fieldKey, 'all');
    historical.forEach(h => {
      const k = h.value.trim().toLowerCase();
      const existing = resultMap.get(k);
      if (existing) {
        existing.count = h.count;
        if (h.sources) existing.sources = h.sources;
      } else {
        resultMap.set(k, { value: h.value.trim(), count: h.count, sources: h.sources });
      }
    });

    setPreviousModalConfig({
      isOpen: true,
      fieldName: fieldKey,
      fieldLabel: label,
      currentValue: currentVal,
      entries: Array.from(resultMap.values()),
      onSelect: onSelectVal
    });
  };

  const handleUpdateGarmentHoursMins = (rowId: string, hours: number, mins: number) => {
    const validHours = Math.max(0, hours);
    const validMins = Math.max(0, Math.min(59, mins));
    const totalDecimalHours = validHours + validMins / 60;

    setEditGarments(prev =>
      prev.map(g =>
        g.rowId === rowId
          ? {
              ...g,
              hours: validHours,
              mins: validMins,
              durationHours: totalDecimalHours
            }
          : g
      )
    );
  };

  const handleToggleWashStated = (rowId: string) => {
    setEditGarments(prev =>
      prev.map(g => {
        if (g.rowId === rowId) {
          const nextState = g.washStated === 'need wash' ? 'wash' : 'need wash';
          return {
            ...g,
            washStated: nextState,
            washDate: nextState === 'wash' ? (g.washDate || editWearDate || getTodayDateKey()) : ''
          };
        }
        return g;
      })
    );
  };

  const handleUpdateGarmentWashDate = (rowId: string, washDateVal: string) => {
    setEditGarments(prev =>
      prev.map(g =>
        g.rowId === rowId
          ? {
              ...g,
              washDate: washDateVal,
              washStated: washDateVal ? 'wash' : g.washStated
            }
          : g
      )
    );
  };

  const handleRemoveGarment = (rowId: string) => {
    setEditGarments(prev => prev.filter(g => g.rowId !== rowId));
  };

  const handleAddGarmentToSession = (clothesId: string) => {
    const garment = clothes.find(c => c.id === clothesId);
    const uniqueRowId = `row_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    setEditGarments(prev => [
      ...prev,
      {
        rowId: uniqueRowId,
        clothesId,
        clothesTitle: garment?.['Name'] || garment?.name || 'Garment',
        clothesType: garment?.['Clothes Type'] || garment?.clothesType || 'Dress',
        durationHours: 4,
        hours: 4,
        mins: 0,
        washStated: 'need wash'
      }
    ]);
    setIsAddingClothes(false);
    setAddClothesSearch('');
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploadingPic(true);
    setUploadPercent(10);
    try {
      const newPics: PictureAttachment[] = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        try {
          // Rule 1: Large files (images) uploaded directly to Firebase Cloud Storage with progress callback
          const storagePic = await uploadReportPhotoToStorage(report?.id || 'report', file, (pct) => {
            const basePct = Math.round((i / files.length) * 100);
            const fileContrib = Math.round((pct / files.length));
            setUploadPercent(Math.min(99, basePct + fileContrib));
          });
          newPics.push(storagePic);
        } catch (storageErr) {
          console.warn('Cloud storage upload error, fallback to local format:', storageErr);
          const processed = await processImageFile(file);
          newPics.push(processed);
        }
      }
      setUploadPercent(100);
      setEditPictures(prev => [...prev, ...newPics]);
    } catch (err) {
      console.error('Error uploading photo:', err);
      alert('Error uploading picture. Please try another image file.');
    } finally {
      setTimeout(() => {
        setUploadingPic(false);
        setUploadPercent(null);
      }, 350);
      if (e.target) e.target.value = '';
    }
  };

  const handleRemovePhoto = (picId: string) => {
    const target = editPictures.find(p => p.id === picId);
    if (target?.storagePath) {
      deleteFileFromStorage(target.storagePath).catch(err => console.warn('Storage cleanup error:', err));
    }
    setEditPictures(prev => prev.filter(p => p.id !== picId));
  };

  const handleAddTag = () => {
    if (!newTagInput.trim()) return;
    const cleanTag = newTagInput.trim().startsWith('#') ? newTagInput.trim() : `#${newTagInput.trim()}`;
    if (!editTags.includes(cleanTag)) {
      setEditTags([...editTags, cleanTag]);
    }
    setNewTagInput('');
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setEditTags(editTags.filter(t => t !== tagToRemove));
  };

  const handleSaveEdit = async () => {
    if (editGarments.length === 0) {
      alert('A wearing report must contain at least one garment.');
      return;
    }

    setIsSaving(true);
    try {
      const existingCustom = { ...(report.customFields || report.extraAttributes || {}) };
      Object.entries(editFieldExplanations).forEach(([k, v]) => {
        if (v && v.trim()) existingCustom[`${k}_explain`] = v.trim();
        else delete existingCustom[`${k}_explain`];
      });
      existingCustom._fieldExplanations = editFieldExplanations;

      const updated = await dataService.updateWearingReport(
        report.id,
        {
          'Name': editName.trim() || report['Name'],
          'wear date': editWearDate ? `${editWearDate}T00:00:00.000Z` : report['wear date'],
          'shoulder feel': editShoulderFeel.trim(),
          'mid breast': editMidBreast.trim(),
          'night mid breast': editNightMidBreast.trim(),
          'sweat': editSweat.trim(),
          'night sweat': editNightSweat.trim(),
          'leg feel': editLegFeel.trim(),
          'night leg feel': editNightLegFeel.trim(),
          'report detail': editReportDetail.trim(),
          'tags': editTags,
          'Pictures': editPictures,
          fieldExplanations: editFieldExplanations,
          customFields: existingCustom,
          extraAttributes: existingCustom
        },
        editGarments.map(g => ({
          junctionId: g.junctionId,
          junctionAutoNumber: g.junctionAutoNumber,
          clothesId: g.clothesId,
          durationHours: g.durationHours,
          washStated: g.washStated,
          washDate: g.washStated === 'wash' ? (g.washDate || editWearDate || getTodayDateKey()) : undefined
        }))
      );

      if (onUpdateReport) {
        onUpdateReport(updated);
      }
      setIsEditing(false);
    } catch (err) {
      console.error('Error saving wearing report:', err);
      alert('Failed to save wearing report changes. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await dataService.deleteWearingReport(report.id);
      if (onDeleteReport) {
        onDeleteReport(report.id);
      }
      onClose();
    } catch (err) {
      console.error('Error deleting wearing report:', err);
      alert('Failed to delete wearing report.');
    } finally {
      setIsDeleting(false);
    }
  };

  const availableClothesToAdd = clothes.filter(
    c => !editGarments.some(eg =>
      (eg.clothesId && eg.clothesId === c.id) ||
      (eg.clothesTitle && (c['Name'] || c.name || '').trim().toLowerCase() === eg.clothesTitle.trim().toLowerCase())
    ) && (c['Name'] || c.name || '').toLowerCase().includes(addClothesSearch.toLowerCase())
  );

  const displayPictures = isEditing ? editPictures : (report['Pictures'] || []);

  return (
    <>
      <div
        id="full-screen-session-modal"
        className="fixed inset-0 z-50 bg-white flex flex-col h-full w-full max-w-full overflow-hidden animate-in fade-in duration-150"
      >
        {/* Sticky Full-Screen Header */}
        <div className="px-3 sm:px-8 py-2.5 sm:py-3.5 border-b border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between gap-2 sm:gap-4 shrink-0 shadow-2xs w-full max-w-full overflow-x-hidden">
          <div className="flex items-center gap-2 sm:gap-3 min-w-0">
            <button
              onClick={isEditing ? cancelEdit : onClose}
              className="p-1.5 sm:p-2 -ml-1 sm:-ml-2 rounded-xl text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors shrink-0 cursor-pointer"
              title="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>

            <div className="space-y-0.5 min-w-0">
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="px-1.5 sm:px-2 py-0.5 rounded-md border border-purple-200 bg-purple-50 text-purple-700 text-[10px] font-mono font-bold uppercase tracking-wider">
                  Report
                </span>
                <span className="text-[11px] sm:text-xs text-slate-600 font-semibold font-mono">
                  {formatCalendarDate(report['wear date'], {
                    weekday: 'short',
                    month: 'short',
                    day: 'numeric'
                  })}
                </span>
                {formattedSessionDuration && (
                  <span className="text-xs text-slate-500 font-medium font-mono hidden md:inline">
                    · {formattedSessionDuration}
                  </span>
                )}
              </div>
              <h2 className="text-xs sm:text-base font-bold text-slate-900 truncate max-w-[130px] sm:max-w-md">
                {isEditing ? editName || 'Editing Report' : report['Name']}
              </h2>
            </div>
          </div>

          {/* Action Toolbar */}
          <div className="flex items-center gap-1 sm:gap-2 shrink-0">
            <FontSizeSwitcher />
            {isEditing ? (
              <>
                <button
                  type="button"
                  onClick={cancelEdit}
                  className="px-2.5 sm:px-4 py-1.5 sm:py-2 rounded-xl text-xs font-semibold text-slate-600 hover:text-slate-900 border border-slate-300 hover:border-slate-400 transition-colors cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveEdit}
                  disabled={isSaving}
                  className="inline-flex items-center gap-1.5 px-3 sm:px-5 py-1.5 sm:py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs disabled:opacity-50 cursor-pointer"
                >
                  <Save className="w-3.5 h-3.5" />
                  <span>{isSaving ? 'Saving...' : 'Save'}</span>
                </button>
              </>
            ) : (
              <>
                {!isAiPaused && (
                  <button
                    type="button"
                    onClick={() => handleGenerateNewReportVersion()}
                    disabled={isAnalyzingWearing}
                    className="inline-flex items-center gap-1 px-2 sm:px-3 py-1.5 sm:py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs disabled:opacity-60 cursor-pointer active:scale-95"
                    title="Generate a new AI report version for this wearing session"
                  >
                    {isAnalyzingWearing ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Sparkles className="w-3.5 h-3.5 text-purple-200" />
                    )}
                    <span className="hidden sm:inline">
                      {isAnalyzingWearing ? 'Diagnosing...' : (reportVersions.length > 0 ? '+ New AI Report' : 'AI Analysis')}
                    </span>
                    <span className="sm:hidden text-[11px]">
                      {isAnalyzingWearing ? '...' : '+AI'}
                    </span>
                  </button>
                )}

                <button
                  type="button"
                  onClick={startEdit}
                  className="inline-flex items-center gap-1 p-1.5 sm:px-3 sm:py-2 rounded-xl border border-slate-300 hover:border-slate-500 text-slate-800 text-xs font-bold transition-all cursor-pointer"
                  title="Edit report"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">Edit</span>
                </button>

                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  className="p-1.5 sm:p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                  title="Delete wearing report"
                >
                  <Trash2 className="w-4 h-4" />
                </button>

                <button
                  type="button"
                  onClick={onClose}
                  className="p-1.5 sm:p-2 rounded-xl text-slate-500 hover:text-slate-800 border border-slate-200 hover:border-slate-300 transition-colors cursor-pointer"
                  title="Close report"
                >
                  <X className="w-4 h-4 sm:w-5 sm:h-5" />
                </button>
              </>
            )}
          </div>
        </div>

        {/* Full-Screen Scrollable Content Area */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden w-full max-w-full bg-slate-50/50">
          <div className="max-w-5xl mx-auto w-full px-3.5 sm:px-8 py-5 sm:py-6 space-y-6">

            {/* EDIT MODE FORM */}
            {isEditing ? (
              <div className="space-y-6">
                {/* Basic Session Info */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
                  <h3 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                    Report Details
                  </h3>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div className="sm:col-span-2">
                      <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                        Report Title *
                      </label>
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        placeholder="e.g. 2026-09-10 Work Outfit"
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-semibold focus:ring-2 focus:ring-purple-500/20"
                      />
                      {renderFieldExplainBox('Name', 'Report Title', '补充本次穿着命名的原因或备注...')}
                    </div>

                    <div>
                      <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                        Wear Date *
                      </label>
                      <input
                        type="date"
                        value={editWearDate}
                        onChange={(e) => setEditWearDate(e.target.value)}
                        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:ring-2 focus:ring-purple-500/20 bg-white"
                      />
                      {renderFieldExplainBox('wear date', 'Wear Date', '补充穿着日期时间段、天气或特殊日子...')}
                    </div>
                  </div>
                </div>

                {/* Garments in this Report with Key-in 15h : 11m */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                        Outfits Worn in Session ({editGarments.length})
                      </h3>
                      <p className="text-[11px] text-slate-400">
                        Set exact hours and minutes worn (e.g. 15h : 11m) and laundry status for each item
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={() => setIsAddingClothes(!isAddingClothes)}
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-700 text-xs font-bold border border-purple-200"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>Add Garment</span>
                    </button>
                  </div>

                  {/* Add Garment Search Box */}
                  {isAddingClothes && (
                    <div className="p-4 rounded-2xl bg-purple-50/50 border border-purple-200 space-y-3">
                      <div className="relative">
                        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                        <input
                          type="text"
                          value={addClothesSearch}
                          onChange={(e) => setAddClothesSearch(e.target.value)}
                          placeholder="Search wardrobe to add item to this report..."
                          className="w-full pl-9 pr-4 py-2 rounded-xl border border-purple-200 bg-white text-xs text-slate-900"
                        />
                      </div>

                      <div className="max-h-48 overflow-y-auto space-y-1.5">
                        {availableClothesToAdd.slice(0, 10).map(c => (
                          <div
                            key={c.id}
                            onClick={() => handleAddGarmentToSession(c.id)}
                            className="flex items-center justify-between p-2.5 rounded-xl bg-white hover:bg-purple-100/60 border border-purple-100 cursor-pointer text-xs"
                          >
                            <span className="font-bold text-slate-900">{c['Name']}</span>
                            <span className="text-[11px] text-purple-700 font-semibold">{c['Clothes Type']} + Add</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* List of garments in this session */}
                  <div className="space-y-3">
                    {editGarments.map((item) => {
                      const garment = clothes.find(c => {
                        const cName = (c['Name'] || c.name || '').trim().toLowerCase();
                        return (
                          (item.clothesId && c.id === item.clothesId) ||
                          (item.clothesTitle && cName === item.clothesTitle.trim().toLowerCase()) ||
                          (item.clothesId && cName === item.clothesId.trim().toLowerCase())
                        );
                      });
                      const displayTitle = garment?.['Name'] || garment?.name || item.clothesTitle || 'Garment';
                      const displayType = garment?.['Clothes Type'] || garment?.clothesType || item.clothesType || 'Garment';
                      const displayMaterial = garment?.['material'] || garment?.material || '';
                      const displayThick = garment?.['thick clothes'] || garment?.thickClothes || '';
                      return (
                        <div
                          key={item.rowId}
                          className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col md:flex-row md:items-center justify-between gap-4"
                        >
                          <div className="space-y-1 min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-800 text-[10px] font-bold">
                                {displayType}
                              </span>
                              <span className="text-sm font-bold text-slate-900 truncate">
                                {displayTitle}
                              </span>
                              {item.junctionId && (
                                <span
                                  className="px-2 py-0.5 rounded-md text-[10px] font-mono bg-purple-50 text-purple-700 border border-purple-200 font-semibold"
                                  title={`Junction ID: ${item.junctionId}`}
                                >
                                  #{item.junctionAutoNumber || ''} · {item.junctionId}
                                </span>
                              )}
                            </div>
                            {(displayMaterial || displayThick) && (
                              <p className="text-[11px] text-slate-500">
                                {[displayMaterial, displayThick].filter(Boolean).join(' · ')}
                              </p>
                            )}
                          </div>

                          {/* Key-in 15h : 11m and Wash State */}
                          <div className="flex items-center gap-3 flex-wrap">
                            {/* Key-in Exact Duration */}
                            <div className="flex items-center gap-1.5 bg-white px-2.5 py-1.5 rounded-xl border border-slate-200 shadow-2xs">
                              <Clock className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                              <div className="flex items-center gap-1 text-xs">
                                <input
                                  type="number"
                                  min="0"
                                  max="99"
                                  value={item.hours}
                                  onChange={(e) =>
                                    handleUpdateGarmentHoursMins(item.rowId, parseInt(e.target.value) || 0, item.mins)
                                  }
                                  className="w-10 text-center font-bold text-slate-900 bg-slate-50 rounded border border-slate-200 py-0.5"
                                  title="Hours"
                                />
                                <span className="font-semibold text-slate-500">h</span>
                                <span className="text-slate-400 font-bold">:</span>
                                <input
                                  type="number"
                                  min="0"
                                  max="59"
                                  value={item.mins}
                                  onChange={(e) =>
                                    handleUpdateGarmentHoursMins(item.rowId, item.hours, parseInt(e.target.value) || 0)
                                  }
                                  className="w-10 text-center font-bold text-slate-900 bg-slate-50 rounded border border-slate-200 py-0.5"
                                  title="Minutes"
                                />
                                <span className="font-semibold text-slate-500">m</span>
                              </div>
                            </div>

                            {/* Wash Stated Toggle and Wash Date Key-in */}
                            <div className="flex items-center gap-2 flex-wrap">
                              <button
                                type="button"
                                onClick={() => handleToggleWashStated(item.rowId)}
                                className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
                                  item.washStated === 'need wash'
                                    ? 'bg-amber-100 text-amber-900 border-amber-300 hover:bg-amber-200'
                                    : 'bg-emerald-100 text-emerald-900 border-emerald-300 hover:bg-emerald-200'
                                }`}
                              >
                                {item.washStated === 'need wash' ? '⚠️ Needs Wash' : '✨ Clean'}
                              </button>

                              {item.washStated === 'wash' && (
                                <div className="flex items-center gap-1.5 bg-white px-2.5 py-1 rounded-xl border border-emerald-300 shadow-2xs">
                                  <span className="text-[10px] font-mono font-bold text-emerald-700">wash date:</span>
                                  <input
                                    type="date"
                                    value={item.washDate || editWearDate || getTodayDateKey()}
                                    onChange={(e) => handleUpdateGarmentWashDate(item.rowId, e.target.value)}
                                    className="text-xs font-mono font-semibold text-slate-800 bg-transparent outline-hidden"
                                    title="Wash date for this garment"
                                  />
                                </div>
                              )}
                            </div>

                            {/* Remove item from report */}
                            <button
                              type="button"
                              onClick={() => handleRemoveGarment(item.rowId)}
                              className="p-1.5 text-slate-400 hover:text-rose-600 transition-colors"
                              title={`Remove this specific row (${item.junctionId || 'new'})`}
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Sensory & Ergonomic Fields with Previous Buttons */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
                  <h3 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                    Comfort & Sensory Log
                  </h3>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* shoulder feel */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">shoulder feel</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('shoulder feel', 'Shoulder feel', setEditShoulderFeel, editShoulderFeel)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editShoulderFeel}
                        onChange={(e) => setEditShoulderFeel(e.target.value)}
                        placeholder="e.g. relaxed, no shoulder pressure"
                      />
                      {renderFieldExplainBox('shoulder feel', 'Shoulder feel', '记录肩部拉力、勒痕或肩带受压体感...')}
                    </div>

                    {/* mid breast */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">mid breast</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('mid breast', 'Mid breast feel', setEditMidBreast, editMidBreast)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editMidBreast}
                        onChange={(e) => setEditMidBreast(e.target.value)}
                        placeholder="e.g. comfortable support"
                      />
                      {renderFieldExplainBox('mid breast', 'Mid breast', '记录前中/胸骨受力、日间承托包裹感...')}
                    </div>

                    {/* night mid breast */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">night mid breast</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('night mid breast', 'Night mid breast', setEditNightMidBreast, editNightMidBreast)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editNightMidBreast}
                        onChange={(e) => setEditNightMidBreast(e.target.value)}
                        placeholder="e.g. evening comfort check"
                      />
                      {renderFieldExplainBox('night mid breast', 'Night mid breast', '记录晚间/睡前胸部放松感、有无压迫...')}
                    </div>

                    {/* sweat */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">sweat</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('sweat', 'Sweat rate', setEditSweat, editSweat)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editSweat}
                        onChange={(e) => setEditSweat(e.target.value)}
                        placeholder="e.g. low / dry / breathable"
                      />
                      {renderFieldExplainBox('sweat', 'Sweat rate', '记录日间出汗透气性、腋下/胸下干爽度...')}
                    </div>

                    {/* night sweat */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">night sweat</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('night sweat', 'Night sweat', setEditNightSweat, editNightSweat)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editNightSweat}
                        onChange={(e) => setEditNightSweat(e.target.value)}
                        placeholder="e.g. cool, ventilated"
                      />
                      {renderFieldExplainBox('night sweat', 'Night sweat', '记录晚间闷热度、排汗透气表现...')}
                    </div>

                    {/* leg feel */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">leg feel</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('leg feel', 'Leg feel', setEditLegFeel, editLegFeel)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editLegFeel}
                        onChange={(e) => setEditLegFeel(e.target.value)}
                        placeholder="e.g. easy stride, flowy"
                      />
                      {renderFieldExplainBox('leg feel', 'Leg feel', '记录裤腿/裙下摆活动自由度、步幅舒适感...')}
                    </div>

                    {/* night leg feel */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">night leg feel</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('night leg feel', 'Night leg feel', setEditNightLegFeel, editNightLegFeel)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editNightLegFeel}
                        onChange={(e) => setEditNightLegFeel(e.target.value)}
                        placeholder="e.g. light and non-binding"
                      />
                      {renderFieldExplainBox('night leg feel', 'Night leg feel', '记录晚间下肢放松、束缚或保暖感受...')}
                    </div>
                  </div>

                  {/* report detail */}
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="text-xs font-mono font-bold uppercase text-slate-600">report detail</label>
                      <button
                        type="button"
                        onClick={() => openPreviousPicker('report detail', 'Report detail', setEditReportDetail, editReportDetail)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                      >
                        <History className="w-3 h-3 text-purple-600" />
                        Previous
                      </button>
                    </div>
                    <AutoResizeTextarea
                      minHeight={72}
                      value={editReportDetail}
                      onChange={(e) => setEditReportDetail(e.target.value)}
                      placeholder="Weather notes, daily events, outfit pairing verdict..."
                    />
                    {renderFieldExplainBox('report detail', 'Report detail', '整体穿着搭配体验、天气温湿度、活动场合重点补充...')}
                  </div>
                </div>

                {/* Pictures Upload & Management */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-4">
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1.5">
                      <ImageIcon className="w-4 h-4 text-purple-600" />
                      Attached Photos ({editPictures.length})
                    </label>

                    <div className="flex items-center gap-3">
                      <button
                        type="button"
                        onClick={() => fileInputRef.current?.click()}
                        disabled={uploadingPic}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs"
                      >
                        <Upload className="w-3.5 h-3.5" />
                        <span>{uploadingPic ? 'Uploading...' : 'Upload Photo'}</span>
                      </button>
                    </div>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      multiple
                      onChange={handlePhotoUpload}
                      className="hidden"
                    />
                  </div>

                  {/* Upload Progress Indicator with Real Percent Status */}
                  {uploadingPic && (
                    <div className="p-3 bg-purple-50 rounded-2xl border border-purple-200 space-y-1.5 animate-in fade-in">
                      <div className="flex items-center justify-between text-xs font-bold text-purple-900">
                        <span className="flex items-center gap-1.5">
                          <Loader2 className="w-3.5 h-3.5 animate-spin text-purple-600" />
                          Uploading &amp; Processing Photo...
                        </span>
                        <span className="font-mono text-purple-700 font-extrabold">{uploadPercent ?? 10}%</span>
                      </div>
                      <div className="w-full bg-purple-200 rounded-full h-2 overflow-hidden">
                        <div
                          className="bg-gradient-to-r from-purple-600 to-indigo-600 h-2 rounded-full transition-all duration-200"
                          style={{ width: `${uploadPercent ?? 15}%` }}
                        />
                      </div>
                    </div>
                  )}

                  {editPictures.length > 0 ? (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      {editPictures.map((pic, idx) => (
                        <div
                          key={pic.id || idx}
                          className="relative group rounded-2xl overflow-hidden aspect-square border border-slate-200 bg-slate-100"
                        >
                          {(() => {
                            const src = resolveImageSource(pic);
                            return src ? (
                              <img
                                src={src}
                                alt={pic.filename || pic.name || 'Outfit photo'}
                                referrerPolicy="no-referrer"
                                className="w-full h-full object-contain p-1 cursor-pointer"
                                onClick={() => {
                                  setViewerInitialIndex(idx);
                                  setViewerOpen(true);
                                }}
                              />
                            ) : (
                              <div
                                className="w-full h-full flex flex-col items-center justify-center p-2 text-center bg-slate-50 cursor-pointer"
                                onClick={() => {
                                  setViewerInitialIndex(idx);
                                  setViewerOpen(true);
                                }}
                              >
                                <ImageIcon className="w-6 h-6 text-slate-300 mb-1" />
                                <span className="text-[9px] text-amber-700 font-mono">Original Expired</span>
                              </div>
                            );
                          })()}
                          <button
                            type="button"
                            onClick={() => handleRemovePhoto(pic.id)}
                            className="absolute top-2 right-2 p-1.5 rounded-full bg-rose-600 text-white shadow-md hover:bg-rose-700 opacity-90 transition-opacity"
                            title="Remove picture"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 italic">No photos attached yet.</p>
                  )}
                </div>

                {/* Tags Management */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200 shadow-xs space-y-3">
                  <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1.5">
                    <Tag className="w-4 h-4 text-purple-600" />
                    Tags
                  </label>

                  <div className="flex flex-wrap gap-1.5">
                    {editTags.map(tag => (
                      <span
                        key={tag}
                        className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="hover:text-rose-600"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    ))}
                  </div>

                  <div className="flex items-center gap-2 max-w-sm">
                    <input
                      type="text"
                      value={newTagInput}
                      onChange={(e) => setNewTagInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          handleAddTag();
                        }
                      }}
                      placeholder="Add tag (e.g. #rainy, #meeting)"
                      className="flex-1 px-3 py-1.5 rounded-xl border border-slate-200 text-xs"
                    />
                    <button
                      type="button"
                      onClick={handleAddTag}
                      className="px-3 py-1.5 rounded-xl bg-slate-900 text-white text-xs font-bold"
                    >
                      Add
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              /* VIEW MODE */
              <div className="space-y-6">
                {/* Wash action feedback banner */}
                {washActionSuccess && (
                  <div className="p-3.5 rounded-2xl bg-blue-50 border border-blue-200 text-blue-900 text-xs font-bold flex items-center justify-between shadow-xs animate-in fade-in">
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                      <span>{washActionSuccess}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={async () => {
                          const res = await dataService.undoWash();
                          if (res.success) {
                            setWashActionSuccess('Wash undone! Restored garment status.');
                            setTimeout(() => setWashActionSuccess(''), 4000);
                          }
                        }}
                        className="px-2.5 py-1 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-[11px] font-bold flex items-center gap-1 cursor-pointer shadow-2xs active:scale-95"
                        title="Undo wash action"
                      >
                        <RotateCcw className="w-3 h-3" />
                        Undo
                      </button>
                      <button onClick={() => setWashActionSuccess('')} className="text-blue-600 hover:text-blue-900 text-[11px] cursor-pointer">
                        Dismiss
                      </button>
                    </div>
                  </div>
                )}

                {/* Hero Editorial Outfit Card */}
                <div className="relative overflow-hidden rounded-3xl bg-linear-to-br from-slate-900 via-purple-950 to-indigo-950 text-white p-6 sm:p-8 shadow-md">
                  {/* Subtle decorative background glow */}
                  <div className="absolute top-0 right-0 -mt-10 -mr-10 w-72 h-72 bg-purple-500/15 rounded-full blur-3xl pointer-events-none" />

                  <div className="relative space-y-4">
                    {/* Top Meta Chips */}
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/15 text-purple-200 text-xs font-semibold">
                        <Calendar className="w-3.5 h-3.5 text-purple-300" />
                        <span>
                          {formatCalendarDate(report['wear date'], {
                            weekday: 'short',
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric'
                          })}
                        </span>
                        {getFieldExplainText('wear date') && (
                          <span className="text-[10px] text-purple-300/90 font-normal border-l border-white/20 pl-1.5" title={getFieldExplainText('wear date')}>
                            {getFieldExplainText('wear date')}
                          </span>
                        )}
                      </span>

                      {formattedSessionDuration && (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/15 text-slate-200 text-xs font-semibold font-mono">
                          <Clock className="w-3.5 h-3.5 text-slate-300" />
                          <span>{formattedSessionDuration}</span>
                        </span>
                      )}

                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/15 text-slate-200 text-xs font-semibold">
                        <Shirt className="w-3.5 h-3.5 text-purple-300" />
                        <span>{sessionClothes.length} {sessionClothes.length === 1 ? 'Garment' : 'Garments'}</span>
                      </span>

                      {hasNeedsWash ? (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-400/20 border border-amber-400/30 text-amber-200 text-xs font-bold">
                          <AlertTriangle className="w-3.5 h-3.5 text-amber-300" />
                          <span>Laundry Pending</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-400/20 border border-emerald-400/30 text-emerald-200 text-xs font-bold">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-300" />
                          <span>All Clean</span>
                        </span>
                      )}
                    </div>

                    {/* Outfit Name */}
                    <div>
                      <h1 className="text-xl sm:text-3xl font-extrabold tracking-tight text-white leading-tight">
                        {report['Name']}
                      </h1>
                      {getFieldExplainText('Name') && (
                        <div className="mt-1.5 inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-purple-500/25 border border-purple-400/30 text-purple-100 text-xs">
                          <MessageSquare className="w-3.5 h-3.5 text-purple-300 shrink-0" />
                          <span className="font-medium italic">&ldquo;{getFieldExplainText('Name')}&rdquo;</span>
                        </div>
                      )}
                    </div>

                    {/* Tags & Categories Ribbon */}
                    {((report['categories'] && report['categories'].length > 0) || (report['tags'] && report['tags'].length > 0)) && (
                      <div className="flex items-center gap-2 flex-wrap pt-1">
                        {report['categories'] && report['categories'].length > 0 && report['categories'].map(cat => (
                          <span
                            key={cat}
                            className="px-2.5 py-0.5 rounded-lg bg-purple-500/25 border border-purple-400/30 text-purple-200 text-[11px] font-bold uppercase tracking-wider font-mono"
                          >
                            {cat}
                          </span>
                        ))}

                        {report['tags'] && report['tags'].length > 0 && report['tags'].map(tag => (
                          <span
                            key={tag}
                            className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-white/10 text-slate-200 border border-white/15 text-xs font-medium"
                          >
                            <Tag className="w-3 h-3 text-purple-300" />
                            <span>{tag}</span>
                          </span>
                        ))}
                      </div>
                    )}

                    {/* Diary Notes Callout */}
                    {(report['report detail'] || getFieldExplainText('report detail')) && (
                      <div className="pt-2 space-y-2">
                        {report['report detail'] && (
                          <div className="p-4 rounded-2xl bg-white/10 backdrop-blur-md border border-white/15 text-slate-200 text-xs sm:text-sm leading-relaxed whitespace-pre-wrap italic">
                            "{report['report detail']}"
                          </div>
                        )}
                        {getFieldExplainText('report detail') && (
                          <div className="p-3 rounded-2xl bg-purple-500/20 backdrop-blur-md border border-purple-400/30 text-purple-100 text-xs leading-relaxed flex items-start gap-2">
                            <MessageSquare className="w-4 h-4 text-purple-300 shrink-0 mt-0.5" />
                            <div>
                              <span className="font-bold text-[10px] text-purple-200 uppercase tracking-wider block font-mono">体感说明 (Explanation):</span>
                              <p className="whitespace-pre-wrap">{getFieldExplainText('report detail')}</p>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Hero Outfit Photos Gallery */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200/80 shadow-xs space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-xl bg-purple-50 text-purple-700">
                        <ImageIcon className="w-4 h-4" />
                      </div>
                      <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 font-mono">
                          Outfit Photos ({displayPictures.length})
                        </h3>
                        <p className="text-[11px] text-slate-500 font-mono">
                          {displayPictures.length > 0 ? 'Tap any photo to zoom & inspect' : 'Visual log for this wearing session'}
                        </p>
                      </div>
                    </div>
                    {displayPictures.length > 0 && (
                      <span className="text-[11px] text-purple-600 font-medium hidden sm:inline">
                        Full-screen zoom &amp; inspect
                      </span>
                    )}
                  </div>

                  {displayPictures.length > 0 ? (
                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3.5">
                      {displayPictures.map((pic, idx) => {
                        const src = resolveImageSource(pic);
                        return (
                          <div
                            key={pic.id || idx}
                            onClick={() => {
                              setViewerInitialIndex(idx);
                              setViewerOpen(true);
                            }}
                            className="group relative rounded-2xl overflow-hidden aspect-4/5 sm:aspect-square border border-slate-200 bg-slate-100 cursor-pointer shadow-2xs hover:shadow-md transition-all hover:scale-[1.01]"
                          >
                            {src ? (
                              <img
                                src={src}
                                alt={pic.filename || pic.name || 'Outfit photo'}
                                referrerPolicy="no-referrer"
                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                              />
                            ) : (
                              <div className="w-full h-full flex flex-col items-center justify-center p-2 text-center bg-slate-50">
                                <ImageIcon className="w-6 h-6 text-slate-300 mb-1" />
                                <span className="text-[10px] text-amber-700 font-mono">Original Expired</span>
                              </div>
                            )}
                            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-colors flex items-center justify-center">
                              <span className="opacity-0 group-hover:opacity-100 bg-black/75 text-white text-[11px] px-3 py-1.5 rounded-full font-medium transition-opacity flex items-center gap-1.5 shadow-sm">
                                <ZoomIn className="w-3.5 h-3.5" />
                                <span>Zoom</span>
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div className="p-4 rounded-2xl bg-slate-50/70 border border-slate-200/70 flex items-center justify-between text-xs text-slate-500">
                      <div className="flex items-center gap-2">
                        <ImageIcon className="w-4 h-4 text-slate-400" />
                        <span>No outfit photos captured for this session yet.</span>
                      </div>
                      <button
                        type="button"
                        onClick={startEdit}
                        className="text-purple-700 hover:text-purple-800 font-bold text-xs inline-flex items-center gap-1 cursor-pointer"
                      >
                        <Upload className="w-3 h-3" />
                        <span>Add Photo</span>
                      </button>
                    </div>
                  )}
                </div>

                {/* Garments in this Outfit */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200/80 shadow-xs space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-xl bg-purple-50 text-purple-700">
                        <Shirt className="w-4 h-4" />
                      </div>
                      <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 font-mono">
                          Garments in this Outfit ({sessionClothes.length})
                        </h3>
                        <p className="text-[11px] text-slate-500 font-mono">
                          Individual items worn, laundry tracking &amp; AI diagnostics
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
                    {sessionClothes.map(({ junction, garment }, idx) => {
                      const title = garment?.['Name'] || (typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].title : 'Garment');
                      const clothesType = garment?.['Clothes Type'] || junction['Clothes Type (from clothes(new))'] || 'Dress';
                      const washState = junction['wash stated'];
                      const junctionDuration = formatDuration(junction['duration'] || 0);
                      const rawWashDate = junction['wash date'] || junction.washDate || junction['washed date'] || junction.washedDate;
                      const formattedWashDate = rawWashDate ? (toDateKey(rawWashDate) || String(rawWashDate)) : '';
                      const cId = junction.clothesId || garment?.id || (typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].id : String(junction['clothes(new)']));
                      const garmentPic = garment?.Pictures?.[0];
                      const garmentImgUrl = garmentPic ? resolveImageSource(garmentPic) : '';

                      return (
                        <div
                          key={idx}
                          className="p-4 sm:p-5 rounded-3xl bg-white border border-slate-200/80 hover:border-purple-200 shadow-2xs hover:shadow-xs transition-all space-y-3.5"
                        >
                          {/* Top Info Row */}
                          <div
                            onClick={() => garment && handleSelectClothesItem(garment)}
                            className="flex items-start gap-3 cursor-pointer group select-none"
                          >
                            {/* Garment Image / Thumbnail */}
                            <div className="w-16 h-16 sm:w-18 sm:h-18 rounded-2xl overflow-hidden bg-slate-50 border border-slate-200/80 shrink-0 p-1 flex items-center justify-center group-hover:border-purple-300 transition-colors shadow-2xs">
                              {garmentImgUrl ? (
                                <ImageWithFallback
                                  src={garmentImgUrl}
                                  alt={title}
                                  clothesId={garment?.id}
                                  className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-200"
                                  fallbackIconClass="w-8 h-8 text-purple-400"
                                />
                              ) : (
                                <div className="w-full h-full rounded-xl bg-purple-50 flex items-center justify-center text-purple-600">
                                  <Shirt className="w-7 h-7" />
                                </div>
                              )}
                            </div>

                            {/* Title and Metadata */}
                            <div className="min-w-0 flex-1 space-y-1">
                              <div className="flex items-center justify-between gap-1.5 flex-wrap">
                                <div className="flex items-center gap-1.5 flex-wrap">
                                  <span className="px-2 py-0.5 rounded-lg text-[10px] font-bold font-mono uppercase bg-purple-50 text-purple-800 border border-purple-200/60">
                                    {clothesType}
                                  </span>
                                  <span className="text-[11px] font-semibold text-slate-500 font-mono flex items-center gap-1">
                                    <Clock className="w-3 h-3 text-slate-400" />
                                    {junctionDuration}
                                  </span>
                                </div>

                                {/* Laundry Status Pill */}
                                <span
                                  className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold shrink-0 ${
                                    washState === 'need wash'
                                      ? 'bg-amber-50 text-amber-900 border border-amber-300'
                                      : 'bg-emerald-50 text-emerald-900 border border-emerald-300'
                                  }`}
                                >
                                  {washState === 'need wash' ? (
                                    <>
                                      <AlertTriangle className="w-3 h-3 text-amber-600" />
                                      <span>Need wash</span>
                                    </>
                                  ) : (
                                    <>
                                      <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                                      <span>Clean</span>
                                    </>
                                  )}
                                </span>
                              </div>

                              <div className="flex items-center justify-between gap-2 pt-0.5">
                                <h4 className="font-bold text-sm text-slate-900 group-hover:text-purple-700 transition-colors line-clamp-2 leading-snug">
                                  {title}
                                </h4>
                                <div className="flex items-center gap-1 shrink-0">
                                  <button
                                    type="button"
                                    disabled={deletingJunctionId === junction.id}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleDeleteSingleJunction(junction.id, title, junction.name);
                                    }}
                                    className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer"
                                    title={`Remove "${title}" from this outfit`}
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
                                  </button>
                                  <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-purple-600 transition-colors" />
                                </div>
                              </div>
                            </div>
                          </div>

                          {/* Independent Per-Garment Wash Action & Diagnostic */}
                          <div className="pt-2.5 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                            {washState === 'need wash' ? (
                              <div
                                onClick={(e) => e.stopPropagation()}
                                className="flex items-center gap-2 flex-wrap"
                              >
                                <span className="text-[10px] font-mono text-slate-500">Wash date:</span>
                                <input
                                  type="date"
                                  value={singleWashDates[cId] || getTodayDateKey()}
                                  onChange={(e) => setSingleWashDates(prev => ({ ...prev, [cId]: e.target.value }))}
                                  className="text-xs font-mono font-medium px-2 py-1 rounded-xl border border-slate-200 bg-slate-50 text-slate-800 focus:bg-white focus:outline-hidden focus:ring-1 focus:ring-purple-500"
                                />
                                <button
                                  type="button"
                                  onClick={(e) => garment && handleWashSingleGarment(cId, title, e)}
                                  className="px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 active:scale-95 text-white font-bold text-xs flex items-center gap-1 shadow-2xs transition-all cursor-pointer"
                                >
                                  <Droplets className="w-3.5 h-3.5" />
                                  <span>Mark Washed</span>
                                </button>
                              </div>
                            ) : (
                              <div className="flex items-center gap-2 text-xs text-emerald-800 font-medium">
                                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                                <span>Cleaned</span>
                                {formattedWashDate && (
                                  <span className="font-mono bg-emerald-50 border border-emerald-200 text-emerald-900 px-2 py-0.5 rounded-md text-[11px] font-semibold">
                                    {formattedWashDate}
                                  </span>
                                )}
                              </div>
                            )}

                            {/* Junction AI Diagnostic Button */}
                            {!isAiPaused && (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setJunctionForAiModal({ junction, garment });
                                }}
                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-purple-50 hover:bg-purple-100 active:scale-95 text-purple-700 border border-purple-200/80 transition-all cursor-pointer shadow-2xs self-start sm:self-auto"
                                title="View or generate single-garment AI diagnosis"
                              >
                                <Sparkles className="w-3.5 h-3.5 text-purple-600" />
                                <span>AI Outfit Diagnosis</span>
                                {junction.aiReports && junction.aiReports.length > 0 && (
                                  <span className="px-1.5 py-0.2 rounded-full bg-purple-200/80 text-[10px] text-purple-900 font-mono font-bold">
                                    {junction.aiReports.length}
                                  </span>
                                )}
                              </button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Teable-Style Multi-Version AI Wearing Diagnostics Card - Hidden when AI generation is paused */}
                {!isAiPaused && (
                <div className="p-5 sm:p-6 rounded-3xl bg-linear-to-br from-purple-50/80 via-white to-indigo-50/50 border border-purple-200/90 shadow-xs space-y-4">
                  {/* Header with Title and Generation Controls */}
                  <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-3 border-b border-purple-100">
                    <div className="flex items-center gap-3">
                      <div className="p-2.5 rounded-2xl bg-purple-100 text-purple-700 shadow-2xs">
                        <Layers className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2 flex-wrap">
                          <h4 className="text-xs font-bold text-slate-900 uppercase font-mono tracking-wider">
                            AI Wearing Diagnostics
                          </h4>
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-100 text-purple-700 border border-purple-200 font-mono">
                            {reportVersions.length} {reportVersions.length === 1 ? 'Report Version' : 'Report Versions'}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          Multi-version AI styling and comfort assessment for this outfit combination.
                        </p>
                      </div>
                    </div>

                    {/* Generator Bar */}
                    <div className="flex items-center gap-2 self-start lg:self-auto flex-wrap">
                      {/* Model selector dropdown */}
                      <div className="flex items-center bg-white rounded-xl border border-purple-200 p-0.5 shadow-2xs">
                        <span className="text-[10px] font-mono font-bold text-slate-400 px-2 uppercase">Model</span>
                        <select
                          value={generationModel}
                          onChange={(e) => setGenerationModel(e.target.value as GeminiModelId)}
                          className="text-xs font-medium font-mono text-slate-800 bg-transparent py-1 pr-2 pl-0 border-none focus:outline-hidden cursor-pointer"
                          title="Choose AI model for next generated report version"
                        >
                          <option value="gemini-3.8-flash">Gemini 3.8 Flash (High Reasoning)</option>
                          <option value="gemini-3.1-flash-lite">Gemini 3.1 Flash Lite (Lite · Quota Saver)</option>
                        </select>
                      </div>

                      {/* Generate New Version Button */}
                      <button
                        type="button"
                        onClick={() => handleGenerateNewReportVersion()}
                        disabled={isAnalyzingWearing}
                        className="inline-flex items-center justify-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition-all shadow-xs disabled:opacity-60 active:scale-95 cursor-pointer shrink-0"
                        title="Generate a new AI report version using selected model without overwriting previous versions"
                      >
                        <RefreshCw className={`w-3.5 h-3.5 ${isAnalyzingWearing ? 'animate-spin' : ''}`} />
                        <span>{isAnalyzingWearing ? 'Generating...' : (reportVersions.length > 0 ? '+ New Report Version' : 'Run First AI Analysis')}</span>
                      </button>
                    </div>
                  </div>
                      {wearingAnalysisError && (
                    <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs space-y-2">
                      <div className="flex items-center gap-2">
                        <AlertTriangle className="w-4 h-4 shrink-0 text-rose-600" />
                        <span className="font-medium">{wearingAnalysisError}</span>
                      </div>
                      {(wearingAnalysisError.includes('prepayment') || wearingAnalysisError.includes('402')) && (
                        <div className="pt-1 text-[11px] text-slate-700 flex flex-wrap items-center gap-2">
                          <span className="font-bold text-slate-900">To activate:</span>
                          <a
                            href="https://ai.studio/projects"
                            target="_blank"
                            rel="noreferrer"
                            className="font-bold text-purple-700 underline hover:text-purple-800"
                          >
                            Top up credits at ai.studio/projects
                          </a>
                          <span>or</span>
                          <a
                            href="https://aistudio.google.com/apikey"
                            target="_blank"
                            rel="noreferrer"
                            className="font-bold text-purple-700 underline hover:text-purple-800"
                          >
                            Get a free key at aistudio.google.com/apikey
                          </a>
                        </div>
                      )}
                    </div>
                  )}

                  {reportVersions.length > 0 ? (
                    <div className="space-y-4">
                      {/* Version Tab Strip */}
                      <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-thin">
                        {reportVersions.map((v, idx) => {
                          const isSelected = v.id === activeVersion?.id;
                          return (
                            <button
                              key={v.id || idx}
                              type="button"
                              onClick={() => {
                                setActiveVersionId(v.id);
                                setIsEditingAiReport(false);
                              }}
                              className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-medium transition-all whitespace-nowrap cursor-pointer border ${
                                isSelected
                                  ? 'bg-purple-600 text-white border-purple-600 shadow-xs'
                                  : 'bg-white hover:bg-purple-50/70 text-slate-700 border-slate-200 hover:border-purple-200'
                              }`}
                            >
                              {v.isPrimary && (
                                <Star className={`w-3 h-3 ${isSelected ? 'text-amber-300 fill-amber-300' : 'text-amber-500 fill-amber-500'}`} />
                              )}
                              <span className="font-semibold">{v.title || `Report #${idx + 1}`}</span>
                              <span className={`text-[10px] font-mono px-1.5 py-0.2 rounded-md ${
                                isSelected ? 'bg-purple-700 text-purple-100' : 'bg-slate-100 text-slate-500'
                              }`}>
                                {v.model?.includes('3.8') ? '3.8 Flash' : '2.5 Flash'}
                              </span>
                            </button>
                          );
                        })}
                      </div>

                      {/* Active Version Body & Management Toolbar */}
                      {activeVersion && (
                        <div className="rounded-2xl bg-white border border-purple-100 shadow-2xs overflow-hidden">
                          {/* Toolbar */}
                          <div className="p-3 sm:p-4 bg-slate-50/70 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="text-xs font-bold text-slate-800">
                                {activeVersion.title || 'Report Version'}
                              </span>
                              <span className="text-[11px] font-mono text-slate-400">
                                {new Date(activeVersion.timestamp).toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                              </span>
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-medium bg-slate-100 text-slate-600 border border-slate-200">
                                {activeVersion.model || 'gemini-3.8-flash'}
                              </span>

                              {activeVersion.isPrimary ? (
                                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 font-mono">
                                  <Star className="w-2.5 h-2.5 fill-amber-500 text-amber-500" />
                                  Primary Summary
                                </span>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() => handleSetPrimaryVersion(activeVersion.id)}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-white text-slate-600 hover:text-amber-600 hover:border-amber-200 border border-slate-200 transition-colors cursor-pointer"
                                  title="Make this report version the primary summary for this session"
                                >
                                  <Star className="w-2.5 h-2.5" />
                                  Set as Primary
                                </button>
                              )}
                            </div>

                            {/* Action buttons */}
                            <div className="flex items-center gap-1.5 self-end sm:self-auto">
                              {!isEditingAiReport ? (
                                <>
                                  <button
                                    type="button"
                                    onClick={() => handleStartEditingAiReport(activeVersion)}
                                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-200/60 transition-colors cursor-pointer"
                                    title="Edit this report version content"
                                  >
                                    <Edit3 className="w-3.5 h-3.5" />
                                    <span>Edit</span>
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => handleCopyAiReport(activeVersion.content, activeVersion.id)}
                                    className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-200/60 transition-colors cursor-pointer"
                                    title="Copy Markdown to clipboard"
                                  >
                                    {copiedVersionId === activeVersion.id ? (
                                      <>
                                        <Check className="w-3.5 h-3.5 text-emerald-600" />
                                        <span className="text-emerald-600 font-bold">Copied!</span>
                                      </>
                                    ) : (
                                      <>
                                        <Copy className="w-3.5 h-3.5" />
                                        <span>Copy</span>
                                      </>
                                    )}
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => {
                                      if (confirm(`Delete "${activeVersion.title || 'this report version'}"? This action cannot be undone.`)) {
                                        handleDeleteAiReportVersion(activeVersion.id);
                                      }
                                    }}
                                    className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                                    title="Delete this report version"
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
                                  </button>
                                </>
                              ) : (
                                <>
                                  <button
                                    type="button"
                                    onClick={() => setIsEditingAiReport(false)}
                                    className="px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-500 hover:text-slate-700 hover:bg-slate-200/60 transition-colors cursor-pointer"
                                  >
                                    Cancel
                                  </button>
                                  <button
                                    type="button"
                                    onClick={handleSaveEditedAiReport}
                                    disabled={isSavingAiReport}
                                    className="inline-flex items-center gap-1 px-3 py-1 rounded-lg text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition-all shadow-2xs disabled:opacity-50 cursor-pointer"
                                  >
                                    {isSavingAiReport ? (
                                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                                    ) : (
                                      <Save className="w-3.5 h-3.5" />
                                    )}
                                    <span>{isSavingAiReport ? 'Saving...' : 'Save Edits'}</span>
                                  </button>
                                </>
                              )}
                            </div>
                          </div>

                          {/* Report Display / Editor */}
                          <div className="p-4 sm:p-5">
                            {isEditingAiReport ? (
                              <div className="space-y-3">
                                <div>
                                  <label className="text-[11px] font-mono font-bold uppercase text-slate-500 block mb-1">
                                    Version Title / Label
                                  </label>
                                  <input
                                    type="text"
                                    value={editedTitle}
                                    onChange={(e) => setEditedTitle(e.target.value)}
                                    placeholder="e.g. Detailed Night Fit Analysis"
                                    className="w-full px-3 py-2 rounded-xl text-xs sm:text-sm font-medium border border-slate-300 focus:outline-hidden focus:ring-2 focus:ring-purple-400/40"
                                  />
                                </div>

                                <div>
                                  <label className="text-[11px] font-mono font-bold uppercase text-slate-500 block mb-1">
                                    Report Markdown Content
                                  </label>
                                  <textarea
                                    value={editedContent}
                                    onChange={(e) => setEditedContent(e.target.value)}
                                    rows={14}
                                    placeholder="Write or edit wearing report notes..."
                                    className="w-full px-3.5 py-3 rounded-xl text-xs sm:text-sm font-sans leading-relaxed border border-slate-300 focus:outline-hidden focus:ring-2 focus:ring-purple-400/40 resize-y break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap"
                                  />
                                </div>
                              </div>
                            ) : (
                              <div className="text-xs sm:text-sm text-slate-800 leading-relaxed max-h-96 overflow-y-auto whitespace-pre-wrap font-sans break-words [word-break:normal] [overflow-wrap:break-word]">
                                {activeVersion.content}
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="p-6 rounded-2xl bg-white/90 border border-dashed border-purple-200 text-center space-y-2.5">
                      <p className="text-xs font-semibold text-slate-700">
                        No wearing diagnostic generated yet for this session.
                      </p>
                      <p className="text-[11px] text-slate-400 max-w-md mx-auto">
                        Click <strong>"Run First AI Analysis"</strong> above to generate a grounded evaluation. You can generate multiple reports with different models or prompts and switch between them anytime.
                      </p>
                    </div>
                  )}
                </div>
                )}

                {/* Comfort & Fit Sensory Log */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200/80 shadow-xs space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-xl bg-rose-50 text-rose-600">
                        <Heart className="w-4 h-4" />
                      </div>
                      <div>
                        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-800 font-mono">
                          Comfort &amp; Fit Experience
                        </h3>
                        <p className="text-[11px] text-slate-500 font-mono">
                          Day &amp; night sensory wear feelings
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                    {/* Shoulder Feel */}
                    <div className="p-3.5 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-1">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">
                        Shoulder Feel
                      </span>
                      <p className="text-xs font-semibold text-slate-900 leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                        {report['shoulder feel'] || <span className="text-slate-400 font-mono italic">None</span>}
                      </p>
                      {getFieldExplainText('shoulder feel') && (
                        <div className="mt-1 flex items-start gap-1 p-1.5 rounded-lg bg-purple-50 border border-purple-200/70 text-purple-900 text-[11px] leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                          <MessageSquare className="w-3 h-3 text-purple-600 shrink-0 mt-0.5" />
                          <span className="font-medium">&ldquo;{getFieldExplainText('shoulder feel')}&rdquo;</span>
                        </div>
                      )}
                    </div>

                    {/* Mid Breast */}
                    <div className="p-3.5 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-1.5">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">
                        Mid Breast
                      </span>
                      <div className="space-y-1 text-xs">
                        <div className="flex items-start gap-1.5 text-slate-800">
                          <Sun className="w-3.5 h-3.5 text-amber-500 shrink-0 mt-0.5" />
                          <span className="font-medium leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] flex-1 min-w-0">{report['mid breast'] || <span className="text-slate-400 italic">Day: None</span>}</span>
                        </div>
                        <div className="flex items-start gap-1.5 text-slate-800">
                          <Moon className="w-3.5 h-3.5 text-indigo-500 shrink-0 mt-0.5" />
                          <span className="font-medium leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] flex-1 min-w-0">{report['night mid breast'] || <span className="text-slate-400 italic">Night: None</span>}</span>
                        </div>
                      </div>
                      {getFieldExplainText('mid breast') && (
                        <div className="mt-1 flex items-start gap-1 p-1.5 rounded-lg bg-purple-50 border border-purple-200/70 text-purple-900 text-[11px] leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                          <MessageSquare className="w-3 h-3 text-purple-600 shrink-0 mt-0.5" />
                          <span className="font-medium">日间: &ldquo;{getFieldExplainText('mid breast')}&rdquo;</span>
                        </div>
                      )}
                      {getFieldExplainText('night mid breast') && (
                        <div className="mt-1 flex items-start gap-1 p-1.5 rounded-lg bg-indigo-50 border border-indigo-200/70 text-indigo-900 text-[11px] leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                          <MessageSquare className="w-3 h-3 text-indigo-600 shrink-0 mt-0.5" />
                          <span className="font-medium">晚间: &ldquo;{getFieldExplainText('night mid breast')}&rdquo;</span>
                        </div>
                      )}
                    </div>

                    {/* Sweat */}
                    <div className="p-3.5 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-1.5">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">
                        Sweat &amp; Breathability
                      </span>
                      <div className="space-y-1 text-xs">
                        <div className="flex items-start gap-1.5 text-slate-800">
                          <Sun className="w-3.5 h-3.5 text-amber-500 shrink-0 mt-0.5" />
                          <span className="font-medium leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] flex-1 min-w-0">{report['sweat'] || <span className="text-slate-400 italic">Day: None</span>}</span>
                        </div>
                        <div className="flex items-start gap-1.5 text-slate-800">
                          <Moon className="w-3.5 h-3.5 text-indigo-500 shrink-0 mt-0.5" />
                          <span className="font-medium leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] flex-1 min-w-0">{report['night sweat'] || <span className="text-slate-400 italic">Night: None</span>}</span>
                        </div>
                      </div>
                      {getFieldExplainText('sweat') && (
                        <div className="mt-1 flex items-start gap-1 p-1.5 rounded-lg bg-purple-50 border border-purple-200/70 text-purple-900 text-[11px] leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                          <MessageSquare className="w-3 h-3 text-purple-600 shrink-0 mt-0.5" />
                          <span className="font-medium">日间: &ldquo;{getFieldExplainText('sweat')}&rdquo;</span>
                        </div>
                      )}
                      {getFieldExplainText('night sweat') && (
                        <div className="mt-1 flex items-start gap-1 p-1.5 rounded-lg bg-indigo-50 border border-indigo-200/70 text-indigo-900 text-[11px] leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                          <MessageSquare className="w-3 h-3 text-indigo-600 shrink-0 mt-0.5" />
                          <span className="font-medium">晚间: &ldquo;{getFieldExplainText('night sweat')}&rdquo;</span>
                        </div>
                      )}
                    </div>

                    {/* Leg Feel */}
                    <div className="p-3.5 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-1.5">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">
                        Leg Comfort
                      </span>
                      <div className="space-y-1 text-xs">
                        <div className="flex items-start gap-1.5 text-slate-800">
                          <Sun className="w-3.5 h-3.5 text-amber-500 shrink-0 mt-0.5" />
                          <span className="font-medium leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] flex-1 min-w-0">{report['leg feel'] || <span className="text-slate-400 italic">Day: None</span>}</span>
                        </div>
                        <div className="flex items-start gap-1.5 text-slate-800">
                          <Moon className="w-3.5 h-3.5 text-indigo-500 shrink-0 mt-0.5" />
                          <span className="font-medium leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] flex-1 min-w-0">{report['night leg feel'] || <span className="text-slate-400 italic">Night: None</span>}</span>
                        </div>
                      </div>
                      {getFieldExplainText('leg feel') && (
                        <div className="mt-1 flex items-start gap-1 p-1.5 rounded-lg bg-purple-50 border border-purple-200/70 text-purple-900 text-[11px] leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                          <MessageSquare className="w-3 h-3 text-purple-600 shrink-0 mt-0.5" />
                          <span className="font-medium">日间: &ldquo;{getFieldExplainText('leg feel')}&rdquo;</span>
                        </div>
                      )}
                      {getFieldExplainText('night leg feel') && (
                        <div className="mt-1 flex items-start gap-1 p-1.5 rounded-lg bg-indigo-50 border border-indigo-200/70 text-indigo-900 text-[11px] leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                          <MessageSquare className="w-3 h-3 text-indigo-600 shrink-0 mt-0.5" />
                          <span className="font-medium">晚间: &ldquo;{getFieldExplainText('night leg feel')}&rdquo;</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Dynamic Custom Attributes */}
                  <div className="pt-2">
                    <CustomAttributesManager
                      title="Environmental &amp; Custom Context"
                      description="Record weather, room temperature, activity or commute details for this wearing session."
                      attributes={report.customFields || report.extraAttributes || {}}
                      onAddAttribute={handleAddReportCustomField}
                      onUpdateAttribute={handleUpdateReportCustomField}
                      onDeleteAttribute={handleDeleteReportCustomField}
                      suggestedKeys={['Weather', 'Room Temperature', 'AC Level', 'Humidity', 'Activity Type', 'Commute Mode']}
                    />
                  </div>
                </div>

                {/* Collapsible Technical & Database Sync Records */}
                <details className="group rounded-3xl border border-slate-200/80 bg-white overflow-hidden shadow-xs transition-all">
                  <summary className="p-5 sm:p-6 flex items-center justify-between cursor-pointer hover:bg-slate-50/80 select-none list-none">
                    <div className="flex items-center gap-3">
                      <div className="p-2.5 rounded-2xl bg-slate-100 text-slate-600">
                        <Tag className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700 font-mono">
                            Technical &amp; Cloud Junction Data
                          </h4>
                          <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[10px] font-mono">
                            {sessionJunctions.length} Junction Rows
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 font-mono mt-0.5">
                          Tap to inspect raw junction table records, wash date sync fields, and database schema properties
                        </p>
                      </div>
                    </div>
                    <ChevronDown className="w-4 h-4 text-slate-400 group-open:rotate-180 transition-transform shrink-0" />
                  </summary>

                  <div className="p-5 sm:p-6 border-t border-slate-100 space-y-6 bg-slate-50/40">
                    {/* Junction Table */}
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-[11px] font-mono font-bold uppercase text-slate-600">
                          Report Clothes Junction Table Records
                        </span>
                        <span className="px-2 py-0.5 rounded bg-purple-50 text-purple-700 font-mono text-[10px] font-bold">
                          {sessionJunctions.length} {sessionJunctions.length === 1 ? 'row' : 'rows'}
                        </span>
                      </div>

                      <div className="overflow-x-auto bg-white rounded-2xl border border-slate-200">
                        <table className="w-full text-left border-collapse text-xs">
                          <thead>
                            <tr className="border-b border-slate-200 bg-slate-50/80 text-[10px] font-mono font-bold uppercase text-slate-500">
                              <th className="py-2.5 px-3">#</th>
                              <th className="py-2.5 px-3">clothes(new)</th>
                              <th className="py-2.5 px-3">duration</th>
                              <th className="py-2.5 px-3">wash stated</th>
                              <th className="py-2.5 px-3 bg-purple-100/70 text-purple-950 font-extrabold border-x border-purple-200">
                                wash date
                              </th>
                              <th className="py-2.5 px-3">wear date</th>
                              <th className="py-2.5 px-3">junction id</th>
                              <th className="py-2.5 px-3 text-right">actions</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100">
                            {sessionJunctions.map((j, idx) => {
                              const cTitle = j.clothesTitle || (typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].title : j.clothesId) || 'Garment';
                              const washState = j['wash stated'];
                              const rawWashDate = j['wash date'] || j.washDate || j['washed date'] || j.washedDate;
                              const formattedWashDate = rawWashDate ? (toDateKey(rawWashDate) || String(rawWashDate)) : '';
                              const durationStr = formatDuration(j['duration'] || 0);

                              return (
                                <tr key={j.id || idx} className="hover:bg-slate-50/80 transition-colors font-mono">
                                  <td className="py-2.5 px-3 text-slate-400 font-bold">{j.name || (idx + 1)}</td>
                                  <td className="py-2.5 px-3 font-sans font-semibold text-slate-900">
                                    <div>{cTitle}</div>
                                    <div className="text-[10px] font-mono text-slate-400">{j.clothesId || (typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']))}</div>
                                  </td>
                                  <td className="py-2.5 px-3 text-slate-700">
                                    {durationStr}
                                    <span className="text-[10px] text-slate-400 block">({j['duration'] || 0}s)</span>
                                  </td>
                                  <td className="py-2.5 px-3 font-sans">
                                    <span
                                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                        washState === 'need wash'
                                          ? 'bg-amber-100 text-amber-900 border border-amber-200'
                                          : 'bg-emerald-100 text-emerald-900 border border-emerald-200'
                                      }`}
                                    >
                                      {washState === 'wash' ? 'Clean' : washState}
                                    </span>
                                  </td>
                                  <td className="py-2.5 px-3 bg-purple-50/50 border-x border-purple-200/60">
                                    {editingJunctionId === j.id ? (
                                      <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                                        <input
                                          type="date"
                                          value={inlineWashDate}
                                          onChange={(e) => setInlineWashDate(e.target.value)}
                                          className="text-[11px] font-mono font-medium px-1.5 py-0.5 rounded border border-purple-400 bg-white text-purple-950 focus:outline-hidden focus:ring-1 focus:ring-purple-500 max-w-[130px]"
                                        />
                                        <button
                                          type="button"
                                          disabled={isUpdatingJunction}
                                          onClick={() => handleSaveJunctionDate(j.id)}
                                          className="px-2 py-0.5 rounded bg-purple-700 hover:bg-purple-800 text-white font-bold text-[10px] transition-colors cursor-pointer"
                                          title="Save wash date to junction and cloud"
                                        >
                                          Save
                                        </button>
                                        <button
                                          type="button"
                                          disabled={isUpdatingJunction}
                                          onClick={() => setEditingJunctionId(null)}
                                          className="px-1.5 py-0.5 rounded bg-slate-200 hover:bg-slate-300 text-slate-700 text-[10px] transition-colors cursor-pointer"
                                          title="Cancel"
                                        >
                                          ✕
                                        </button>
                                      </div>
                                    ) : (
                                      <div className="flex items-center justify-between gap-1 group/jdate">
                                        {formattedWashDate ? (
                                          <span className="font-bold text-purple-900 bg-purple-100 px-2 py-0.5 rounded border border-purple-300 inline-block font-mono">
                                            {formattedWashDate}
                                          </span>
                                        ) : (
                                          <span className="text-slate-400 italic">
                                            (empty - {washState === 'need wash' ? 'needs wash' : 'unrecorded'})
                                          </span>
                                        )}
                                        <button
                                          type="button"
                                          onClick={() => handleStartEditJunctionDate(j.id, formattedWashDate)}
                                          className="opacity-70 group-hover/jdate:opacity-100 p-1 text-purple-600 hover:text-purple-950 hover:bg-purple-100 rounded transition-all cursor-pointer"
                                          title="Edit junction wash date"
                                        >
                                          <Edit3 className="w-3 h-3" />
                                        </button>
                                      </div>
                                    )}
                                  </td>
                                  <td className="py-2.5 px-3 text-slate-600">
                                    {j['wear date'] ? toDateKey(j['wear date']) || String(j['wear date']) : (report['wear date'] ? toDateKey(report['wear date']) : '-')}
                                  </td>
                                  <td className="py-2.5 px-3 text-[10px] text-slate-400 truncate max-w-[120px]" title={j.id}>
                                    {j.id}
                                  </td>
                                  <td className="py-2.5 px-3 text-right font-sans">
                                    <div className="flex items-center justify-end gap-1">
                                      <button
                                        type="button"
                                        onClick={() => {
                                          const matchedGarment = clothes.find(c => {
                                            const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
                                            return c.id === cId;
                                          });
                                          setJunctionForAiModal({ junction: j, garment: matchedGarment });
                                        }}
                                        className="p-1.5 text-purple-600 hover:text-purple-900 hover:bg-purple-100 rounded-lg transition-colors cursor-pointer"
                                        title="View/Generate AI Diagnosis"
                                      >
                                        <Sparkles className="w-3.5 h-3.5" />
                                      </button>
                                      <button
                                        type="button"
                                        disabled={deletingJunctionId === j.id}
                                        onClick={() => handleDeleteSingleJunction(j.id, cTitle, j.name)}
                                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer disabled:opacity-40"
                                        title={`Delete ONLY junction record #${j.name || ''} (${j.id})`}
                                      >
                                        <Trash2 className="w-3.5 h-3.5" />
                                      </button>
                                    </div>
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    </div>

                    {/* Raw DB Fields Grid */}
                    <div className="space-y-3">
                      <span className="text-[11px] font-mono font-bold uppercase text-slate-600">
                        Exact Database Fields &amp; Schema Values
                      </span>
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
                        <div className="p-3 rounded-xl bg-white border border-slate-200">
                          <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">Name</span>
                          <span className="text-xs font-semibold text-slate-800">{report['Name'] || '(empty)'}</span>
                        </div>
                        <div className="p-3 rounded-xl bg-white border border-slate-200">
                          <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">wear date</span>
                          <span className="text-xs font-mono text-slate-800">{report['wear date'] ? toDateKey(report['wear date']) : '(empty)'}</span>
                        </div>
                        <div className="p-3 rounded-xl bg-white border border-slate-200">
                          <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">created</span>
                          <span className="text-xs font-mono text-slate-800">{report['created'] || '(empty)'}</span>
                        </div>
                        <div className="p-3 rounded-xl bg-white border border-slate-200">
                          <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">categories</span>
                          <span className="text-xs text-slate-800">{(report['categories'] && report['categories'].length > 0) ? report['categories'].join(', ') : '(empty)'}</span>
                        </div>
                        <div className="p-3 rounded-xl bg-white border border-slate-200">
                          <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">tags</span>
                          <span className="text-xs text-slate-800">{(report['tags'] && report['tags'].length > 0) ? report['tags'].join(', ') : '(empty)'}</span>
                        </div>
                        <div className="p-3 rounded-xl bg-white border border-slate-200">
                          <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block">Pictures</span>
                          <span className="text-xs font-mono text-slate-800">{(report['Pictures'] && report['Pictures'].length > 0) ? `${report['Pictures'].length} image(s)` : '(empty)'}</span>
                        </div>
                      </div>
                    </div>

                    <div className="pt-2 flex items-center justify-between text-xs text-slate-400 font-mono">
                      <span>Report ID: {report.id}</span>
                      <span>Records Synced: {sessionJunctions.length}</span>
                    </div>
                  </div>
                </details>
              </div>
            )}
          </div>
        </div>

        {/* Full-Screen Sticky Footer Bar */}
        <div className="px-4 sm:px-8 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] border-t border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between shrink-0 w-full max-w-full shadow-xs">
          <div className="flex items-center gap-2 min-w-0">
            <span className="text-xs text-slate-500 font-mono truncate">
              {formatCalendarDate(report['wear date'], { month: 'short', day: 'numeric', year: 'numeric' })}
            </span>
          </div>

          <div className="flex items-center gap-2">
            {isEditing ? (
              <>
                <button
                  type="button"
                  onClick={cancelEdit}
                  className="px-3.5 sm:px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 border border-slate-200 transition-colors cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveEdit}
                  disabled={isSaving}
                  className="px-4 sm:px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white text-xs font-bold transition-all shadow-xs cursor-pointer"
                >
                  {isSaving ? 'Saving...' : 'Save Changes'}
                </button>
              </>
            ) : (
              <>
                <button
                  type="button"
                  onClick={startEdit}
                  className="px-3.5 sm:px-4 py-2 rounded-xl border border-slate-300 hover:border-slate-400 text-slate-700 text-xs font-semibold transition-colors cursor-pointer"
                >
                  Edit Report
                </button>
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 sm:px-5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-all shadow-xs cursor-pointer"
                >
                  Close
                </button>
              </>
            )}
          </div>
        </div>

        {/* Full-Screen Delete Confirmation Modal */}
        {showDeleteConfirm && (
          <div className="fixed inset-0 z-60 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto">
            {/* Header */}
            <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between shrink-0 w-full max-w-full overflow-x-hidden">
              <div className="flex items-center gap-2.5">
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={isDeleting}
                  className="p-2 -ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer"
                  title="Back"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <h2 className="text-sm sm:text-base font-bold text-slate-900">
                  Delete Wearing Report
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={isDeleting}
                className="p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-6 sm:p-10 flex flex-col items-center justify-center bg-slate-50/60">
              <div className="max-w-md w-full mx-auto space-y-6 text-center">
                <div className="w-20 h-20 rounded-3xl mx-auto flex items-center justify-center bg-rose-100 text-rose-600 border border-rose-200 shadow-xs">
                  <Trash2 className="w-8 h-8" />
                </div>

                <div className="space-y-1.5">
                  <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight">
                    Delete Wearing Report?
                  </h3>
                  <p className="text-xs text-slate-500 font-mono">
                    This action is permanent and cannot be undone.
                  </p>
                </div>

                <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-2xs space-y-3 text-left">
                  <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                    <span className="text-xs text-slate-500">Report</span>
                    <span className="text-xs font-bold text-slate-900 font-mono truncate max-w-[200px]">{report['Name']}</span>
                  </div>
                  <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                    <span className="text-xs text-slate-500">Wear Date</span>
                    <span className="text-xs font-semibold text-slate-800 font-mono">{report['wear date'] ? toDateKey(report['wear date']) : '-'}</span>
                  </div>
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs text-slate-500">Linked Garments</span>
                    <span className="text-xs font-semibold text-slate-800 font-mono">{sessionClothes.length} item(s)</span>
                  </div>
                  <p className="text-xs text-rose-600 font-medium pt-1">
                    Removing this report will detach its junction wear records from your wardrobe statistics.
                  </p>
                </div>
              </div>
            </div>

            {/* Bottom Actions */}
            <div className="p-4 sm:p-6 border-t border-slate-200 bg-white flex items-center justify-center shrink-0">
              <div className="max-w-md w-full flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={isDeleting}
                  className="flex-1 py-3 px-4 rounded-xl text-xs sm:text-sm font-semibold text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-300 transition-all cursor-pointer active:scale-95 disabled:opacity-50"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={isDeleting}
                  className="flex-1 py-3 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs sm:text-sm transition-all shadow-xs disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer active:scale-95"
                >
                  {isDeleting && <Loader2 className="w-4 h-4 animate-spin" />}
                  <span>{isDeleting ? 'Deleting...' : 'Delete Report'}</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Previous Entries Picker Modal */}
        {previousModalConfig && (
          <PreviousEntriesModal
            isOpen={previousModalConfig.isOpen}
            fieldName={previousModalConfig.fieldName}
            fieldLabel={previousModalConfig.fieldLabel}
            currentValue={previousModalConfig.currentValue}
            entries={previousModalConfig.entries}
            onSelect={previousModalConfig.onSelect}
            onClose={() => setPreviousModalConfig(null)}
          />
        )}

        {/* Fullscreen Zoom & Drag Image Viewer */}
        <ImageViewerModal
          images={displayPictures}
          initialIndex={viewerInitialIndex}
          isOpen={viewerOpen}
          onClose={() => setViewerOpen(false)}
        />

        {/* Generic Action Confirmation Modal */}
        {confirmModalConfig && (
          <ConfirmModal
            isOpen={confirmModalConfig.isOpen}
            onClose={() => setConfirmModalConfig(null)}
            onConfirm={confirmModalConfig.onConfirm}
            title={confirmModalConfig.title}
            message={confirmModalConfig.message}
            confirmLabel={confirmModalConfig.confirmLabel}
            variant={confirmModalConfig.variant}
          />
        )}

        {/* Junction AI Diagnostic Modal */}
        {junctionForAiModal && (
          <JunctionAIModal
            isOpen={Boolean(junctionForAiModal)}
            junction={junctionForAiModal.junction}
            garment={junctionForAiModal.garment}
            report={report}
            onClose={() => setJunctionForAiModal(null)}
            onUpdateJunction={(updatedJunction) => {
              setJunctionForAiModal(prev => prev ? { ...prev, junction: updatedJunction } : null);
            }}
          />
        )}
      </div>
    </>
  );
};

export const WearingReportDetailModal: React.FC<WearingReportDetailModalProps> = (props) => {
  if (!props.report) return null;
  return <WearingReportDetailModalContent {...props} report={props.report} />;
};

export const SessionDetailModal = WearingReportDetailModal;
