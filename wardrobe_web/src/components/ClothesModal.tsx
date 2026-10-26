import React, { useState, useRef, useEffect, useMemo } from 'react';
import { ClothesItem, ReportClothesJunction, WearingReport, PictureAttachment, AIReportVersion } from '../types';
import { dataService, processImageFile } from '../services/dataService';
import { uploadGarmentPhotoToStorage, deleteFileFromStorage } from '../services/cloudStorageService';
import { PreviousEntriesModal, PreviousEntryItem } from './PreviousEntriesModal';
import { CreateOptionModal } from './CreateOptionModal';
import { FieldExplainBox } from './FieldExplainBox';
import { AutoResizeTextarea } from './AutoResizeTextarea';
import { ImageViewerModal } from './ImageViewerModal';
import { CustomAttributesManager } from './CustomAttributesManager';
import { resolveImageSource } from '../utils/imageUtils';
import { ConfirmModal } from './ConfirmModal';
import { toDateKey, formatCalendarDate, getTodayDateKey } from '../utils/calendarCalculations';
import { getCustomApiKey, getSelectedModel, useAiGenerationPaused } from '../services/aiSettings';
import { fieldDictionaryService } from '../services/fieldDictionaryService';
import { FontSizeSwitcher } from './FontSizeSwitcher';
import {
  X, CheckCircle2, AlertCircle, Sparkles, ThermometerSun, ShieldCheck, DollarSign,
  Clock, RefreshCw, Calendar, Tag, ChevronRight, Droplets, Info, Image as ImageIcon,
  Edit3, Save, Plus, Trash2, Camera, Upload, History, ArrowLeft, Maximize2, Minimize2,
  Database, Loader2, Shirt, Star, Copy, Check, MessageSquare, MessageSquarePlus,
  ChevronDown, ChevronUp
} from 'lucide-react';

export interface ClothesModalProps {
  clothes?: ClothesItem | null;
  garment?: ClothesItem | null;
  stats: {
    needWash: boolean;
    unwashedSeconds: number;
    unwashedHours: string;
    totalWears: number;
    totalDurationHours: number;
  };
  junctions: ReportClothesJunction[];
  reports: WearingReport[];
  onClose: () => void;
  onMarkWashed: (clothesId: string, customWashDate?: string) => void;
  onUpdateClothes?: (clothesId: string, updates: Partial<ClothesItem>) => void;
  onUpdateGarment?: (clothesId: string, updates: Partial<ClothesItem>) => void;
  onSelectReport?: (report: WearingReport) => void;
  onDeleteClothes?: (clothesId: string) => void;
  onDeleteGarment?: (clothesId: string) => void;
}

export type GarmentModalProps = ClothesModalProps;

const ClothesModalContent: React.FC<ClothesModalProps & { garment: ClothesItem }> = ({
  garment,
  clothes,
  stats,
  junctions,
  reports,
  onClose,
  onMarkWashed,
  onUpdateClothes,
  onUpdateGarment: propOnUpdateGarment,
  onSelectReport,
  onDeleteClothes,
  onDeleteGarment: propOnDeleteGarment
}) => {
  const onUpdateGarment = (id: string, upd: Partial<ClothesItem>) => {
    if (onUpdateClothes) onUpdateClothes(id, upd);
    if (propOnUpdateGarment) propOnUpdateGarment(id, upd);
  };
  const onDeleteGarment = (id: string) => {
    if (onDeleteClothes) onDeleteClothes(id);
    if (propOnDeleteGarment) propOnDeleteGarment(id);
  };

  // Edit Mode State
  const [isEditing, setIsEditing] = useState(false);
  const isAiPaused = useAiGenerationPaused();
  const [analyzing, setAnalyzing] = useState(false);
  const [aiAnalysis, setAiAnalysis] = useState(garment['AI Clothes Analysis'] || '');
  const [errorMsg, setErrorMsg] = useState('');
  const [uploadingPic, setUploadingPic] = useState(false);
  const [uploadPercent, setUploadPercent] = useState<number | null>(null);
  const [selectedWashDate, setSelectedWashDate] = useState(() => getTodayDateKey());

  // Multi-version AI reports state for this garment
  const garmentAiReports: AIReportVersion[] = garment.aiReports || (garment['AI Clothes Analysis'] ? [{
    id: 'garment_initial',
    timestamp: garment['created'] || new Date().toISOString(),
    title: 'Report #1',
    model: 'gemini-3.6-flash',
    content: garment['AI Clothes Analysis'],
    isPrimary: true
  }] : []);

  const [activeVersionId, setActiveVersionId] = useState<string | null>(() => {
    const primary = garmentAiReports.find(v => v.isPrimary);
    return primary?.id || garmentAiReports[0]?.id || null;
  });
  const [isEditingAiReport, setIsEditingAiReport] = useState(false);
  const [editingReportContent, setEditingReportContent] = useState('');
  const [editingReportTitle, setEditingReportTitle] = useState('');
  const [copiedVersionId, setCopiedVersionId] = useState<string | null>(null);

  // Active version resolved
  const activeVersion = garmentAiReports.find(v => v.id === activeVersionId) || garmentAiReports[0] || null;

  // Editable Form Fields
  const [editName, setEditName] = useState(garment['Name'] || '');
  const [editClothesType, setEditClothesType] = useState(garment['Clothes Type'] || 'Dress');
  const [editMaterial, setEditMaterial] = useState(garment['material'] || '');
  const [editThickClothes, setEditThickClothes] = useState(garment['thick clothes'] || '');
  const [editHotAtNoon, setEditHotAtNoon] = useState(garment['Hot at noon'] || '');
  const [editDiaoDai, setEditDiaoDai] = useState(garment['diao dai'] || '');
  const [editSideBreastOk, setEditSideBreastOk] = useState(garment['side breast ok'] || '');
  const [editSideBreastOkWithGatherBra, setEditSideBreastOkWithGatherBra] = useState(
    garment['side breast ok: with 聚拢 bra'] || garment['side breast ok: With 聚拢bra'] || garment.sideBreastOkWithGatherBra || garment['side breast ok'] || ''
  );
  const [editSideBreastOkWithSideOutBra, setEditSideBreastOkWithSideOutBra] = useState(
    garment['side breast ok: with side out bra'] || garment['side breast ok: With side out bra'] || garment.sideBreastOkWithSideOutBra || ''
  );
  const [editSitBreastOkWithGatherBra, setEditSitBreastOkWithGatherBra] = useState(
    garment['sit breast ok: With 聚拢bra'] || garment['sit breast ok: with 聚拢bra'] || garment.sitBreastOkWithGatherBra || garment['sit breast ok'] || ''
  );
  const [editSitBreastOkWithSideOutBra, setEditSitBreastOkWithSideOutBra] = useState(
    garment['sit breast ok: with side out bra'] || garment['sit breast ok: With side out bra'] || garment.sitBreastOkWithSideOutBra || ''
  );

  // Multi-scenario climate and sensory ergonomics states
  const [editWhenAircondHot, setEditWhenAircondHot] = useState(garment['when with aircond: hot feeling'] || '');
  const [editWhenAircondLeg, setEditWhenAircondLeg] = useState(garment['when with aircond: leg feeling'] || '');
  const [editWhenNoAircondHot, setEditWhenNoAircondHot] = useState(garment['when without aircond: hot feeling'] || '');
  const [editWhenNoAircondLeg, setEditWhenNoAircondLeg] = useState(garment['when without aircond: leg feeling'] || '');
  const [editWhenSleepFanHot, setEditWhenSleepFanHot] = useState(garment['when sleep under fan: hot feeling'] || '');
  const [editWhenSleepFanLeg, setEditWhenSleepFanLeg] = useState(garment['when sleep under fan: leg feeling'] || '');
  const [editCenterChest, setEditCenterChest] = useState(garment['center chest'] || '');
  const [editBeautiful, setEditBeautiful] = useState(garment['beautiful'] || '');
  const [editBeatifulReason, setEditBeatifulReason] = useState(garment['beatiful reason'] || '');
  const [editWet, setEditWet] = useState(garment['wet'] || 'dry');
  const [editWetReason, setEditWetReason] = useState(garment['wet reason'] || '');
  const [editLongDress, setEditLongDress] = useState(garment['long dress'] || '');
  const [editPrice, setEditPrice] = useState<string>(garment['price'] !== undefined ? String(garment['price']) : '');
  const [editMayThrow, setEditMayThrow] = useState(garment['may throw'] || '');
  const [editLastWashed, setEditLastWashed] = useState(garment['Last Washed'] ? toDateKey(garment['Last Washed']) || '' : '');
  const [editDescriptions, setEditDescriptions] = useState(garment['Descriptions'] || '');
  const [editDescriptions2, setEditDescriptions2] = useState(garment['Descriptions 2'] || '');
  const [editLastComment, setEditLastComment] = useState(garment['last comment or action'] || '');
  const [editTags, setEditTags] = useState<string[]>(garment['tags'] || []);
  const [newTagInput, setNewTagInput] = useState('');
  const [editPictures, setEditPictures] = useState<PictureAttachment[]>(garment['Pictures'] || []);

  // Field Explanations / Ergonomic Notes (e.g. dress cutting, lace drying, diao dai pain)
  const [editFieldExplanations, setEditFieldExplanations] = useState<Record<string, string>>(() => {
    return garment.fieldExplanations || garment.customFields?._fieldExplanations || {};
  });
  const [expandedExplainFields, setExpandedExplainFields] = useState<Record<string, boolean>>({});
  const [dictVersion, setDictVersion] = useState(0);
  const [dataVersion, setDataVersion] = useState(0);

  useEffect(() => {
    const unsubDict = fieldDictionaryService.subscribe(() => {
      setDictVersion(v => v + 1);
    });
    const unsubData = dataService.subscribe(() => {
      setDataVersion(v => v + 1);
    });
    return () => {
      unsubDict();
      unsubData();
    };
  }, []);

  const [createOptionConfig, setCreateOptionConfig] = useState<{
    isOpen: boolean;
    fieldName: string;
    fieldLabel: string;
    onSave: (val: string) => void;
  } | null>(null);

  const wetOptions = React.useMemo(() => {
    const dictValues = fieldDictionaryService.getFieldTypicalValues('wet');
    const histValues = dataService.getFieldPreviousValues('wet', 'clothes');
    const optMap = new Map<string, { value: string; label: string; meaningCn?: string }>();

    // Standard baseline options
    const standard = [
      { value: 'dry', label: 'dry (干爽 / 不易受潮)', meaningCn: '不易吸湿发黏或透气快干，穿着全程保持干爽舒适' },
      { value: 'middle', label: 'middle (适中 / 半干半湿)', meaningCn: '排湿透气适中，微有体感但未明显积汗黏腻' },
      { value: 'little wet', label: 'little wet (微湿 / 局部微潮)', meaningCn: '局部有轻微潮气或微汗微湿，但尚不至于大面积贴身黏腻' },
      { value: 'wet', label: 'wet (潮湿 / 易浸湿黏身)', meaningCn: '容易受汗浸湿、排湿不良或面料厚重受潮，穿着后有湿热黏身感' },
      { value: 'need dry', label: 'need dry (需晾干 / 需风干)', meaningCn: '穿着后受潮或有汗气，需要挂起晾干或通风吹干' }
    ];
    standard.forEach(s => optMap.set(s.value.toLowerCase(), s));

    // Overlay dictionary typical values
    dictValues.forEach(dv => {
      const k = dv.valueEn.trim().toLowerCase();
      const existing = optMap.get(k);
      const labelText = dv.valueCn && dv.valueCn !== dv.valueEn ? `${dv.valueEn} (${dv.valueCn})` : dv.valueEn;
      if (existing) {
        existing.label = labelText;
        if (dv.meaningCn) existing.meaningCn = dv.meaningCn;
      } else {
        optMap.set(k, {
          value: dv.valueEn.trim(),
          label: labelText,
          meaningCn: dv.meaningCn
        });
      }
    });

    // Overlay historical clothes values
    histValues.forEach(hv => {
      const k = hv.value.trim().toLowerCase();
      if (!optMap.has(k)) {
        optMap.set(k, {
          value: hv.value.trim(),
          label: hv.value.trim()
        });
      }
    });

    // Ensure current editWet is always in list
    if (editWet && !optMap.has(editWet.trim().toLowerCase())) {
      optMap.set(editWet.trim().toLowerCase(), {
        value: editWet.trim(),
        label: editWet.trim()
      });
    }

    return Array.from(optMap.values());
  }, [editWet, dictVersion]);

  useEffect(() => {
    setEditFieldExplanations(garment.fieldExplanations || garment.customFields?._fieldExplanations || {});
  }, [garment]);

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
        targetCategory="clothes"
      />
    );
  };

  const fileInputRef = useRef<HTMLInputElement>(null);
  const viewFileInputRef = useRef<HTMLInputElement>(null);

  // Fullscreen Modal state (defaults to true for immersive full-screen experience)
  const [isFullScreen, setIsFullScreen] = useState(true);

  // Fullscreen Image Viewer state
  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerInitialIndex, setViewerInitialIndex] = useState(0);

  // Delete Garment Confirmation state
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // Wash Garment Confirmation state
  const [showWashConfirm, setShowWashConfirm] = useState(false);

  // Previous Entries Picker State
  const [previousModalConfig, setPreviousModalConfig] = useState<{
    isOpen: boolean;
    fieldName: string;
    fieldLabel: string;
    currentValue?: string;
    entries: PreviousEntryItem[];
    onSelect: (val: string) => void;
  } | null>(null);

  // Open Previous Value Picker for a given field
  const openPreviousPicker = (
    fieldKey: string,
    label: string,
    onSelectVal: (v: string) => void,
    currentVal?: string
  ) => {
    let historical = dataService.getFieldPreviousValues(fieldKey, 'all');
    const dictValues = fieldDictionaryService.getFieldTypicalValues(fieldKey);
    const resultMap = new Map<string, PreviousEntryItem>();

    // 1. Add typicalValues from AI dictionary (with labelCn and meaningCn!)
    dictValues.forEach(dv => {
      const k = dv.valueEn.trim().toLowerCase();
      resultMap.set(k, {
        value: dv.valueEn.trim(),
        count: 0,
        labelCn: dv.valueCn && dv.valueCn !== dv.valueEn ? dv.valueCn : undefined,
        meaningCn: dv.meaningCn || undefined
      });
    });

    // 2. Special defaults for wet
    if (fieldKey === 'wet') {
      const defaultWet = [
        { value: 'dry', labelCn: '干爽 / 不易受潮', meaningCn: '不易吸湿发黏或透气快干，穿着全程保持干爽舒适' },
        { value: 'middle', labelCn: '适中 / 半干半湿', meaningCn: '排湿透气适中，微有体感但未明显积汗黏腻' },
        { value: 'little wet', labelCn: '微湿 / 局部微潮', meaningCn: '局部有轻微潮气或微汗微湿，但尚不至于大面积贴身黏腻' },
        { value: 'wet', labelCn: '潮湿 / 易浸湿黏身', meaningCn: '容易受汗浸湿、排湿不良或面料厚重受潮，穿着后有湿热黏身感' },
        { value: 'need dry', labelCn: '需晾干 / 需风干', meaningCn: '穿着后受潮或有汗气，需要挂起晾干或通风吹干' }
      ];
      defaultWet.forEach(dw => {
        const k = dw.value.toLowerCase();
        if (!resultMap.has(k)) {
          resultMap.set(k, { value: dw.value, count: 0, labelCn: dw.labelCn, meaningCn: dw.meaningCn });
        }
      });
    }

    // 3. Special defaults for Clothes Type
    if (fieldKey === 'Clothes Type') {
      const defaultTypes = ['Dress', 'bra', 'Tshirt', 'Pant', 'skirt', 'sweater', 'jacket', 'coat', 'top', 'blouse', 'cardigan', 'shorts', 'Other'];
      defaultTypes.forEach(dt => {
        const k = dt.toLowerCase();
        if (!resultMap.has(k)) {
          resultMap.set(k, { value: dt, count: 0 });
        }
      });
    }

    // 4. Overlay historical occurrences
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

  // Find all wearing reports where this garment was included (comprehensive multi-key match)
  const allReportsForModal = useMemo(() => {
    const map = new Map<string, WearingReport>();
    (reports || []).forEach(r => { if (r?.id) map.set(r.id, r); });
    dataService.getReports().forEach(r => { if (r?.id) map.set(r.id, r); });
    return Array.from(map.values());
  }, [reports, dataVersion]);

  const allJunctionsForModal = useMemo(() => {
    const map = new Map<string, ReportClothesJunction>();
    (junctions || []).forEach(j => { if (j?.id) map.set(j.id, j); });
    dataService.getJunctions().forEach(j => { if (j?.id) map.set(j.id, j); });
    return Array.from(map.values());
  }, [junctions, dataVersion]);

  const garmentJunctions = useMemo(() => {
    const targetId = (garment.id || '').trim();
    const targetName = (garment.Name || garment.name || '').trim().toLowerCase();

    return allJunctionsForModal.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : (j.clothesId || j['clothes(new)']);
      const cTitle = (typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : (j.clothesTitle || '')).trim().toLowerCase();
      if (targetId && cId === targetId) return true;
      if (targetName && cTitle) {
        if (cTitle === targetName) return true;
        if (cTitle.includes(targetName) || targetName.includes(cTitle)) return true;
      }
      return false;
    });
  }, [allJunctionsForModal, garment]);

  const wornReports = useMemo(() => {
    const targetId = (garment.id || '').trim();
    const targetName = (garment.Name || garment.name || '').trim().toLowerCase();

    const matchedReportsMap = new Map<string, { junction?: ReportClothesJunction; report: WearingReport }>();

    garmentJunctions.forEach(j => {
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

      const rep = allReportsForModal.find(r => {
        const repName = (r['Name'] || r.name || '').trim().toLowerCase();
        const repId = (r.id || '').trim().toLowerCase();
        return (
          (repId && rIdLower && repId === rIdLower) ||
          (repName && rTitleLower && repName === rTitleLower) ||
          (repName && rIdLower && repName === rIdLower) ||
          (repId && rTitleLower && repId === rTitleLower) ||
          (repName && repName.length >= 5 && rTitleLower && (rTitleLower.includes(repName) || repName.includes(rTitleLower)))
        );
      });

      const finalRep = rep || ({
        id: rId || `rep_fallback_${Date.now()}`,
        Name: rTitle || 'Wear Session',
        'wear date': j['wear date'] || new Date().toISOString()
      } as WearingReport);
      matchedReportsMap.set(finalRep.id, { junction: j, report: finalRep });
    });

    // Also check if any reports directly link to this garment (by ID, name, worn lists, or text details)
    allReportsForModal.forEach(r => {
      if (!matchedReportsMap.has(r.id)) {
        const rawJson = JSON.stringify(r).toLowerCase();
        const hasIdMatch = Boolean(targetId && rawJson.includes(targetId.toLowerCase()));
        const hasNameMatch = Boolean(targetName && rawJson.includes(targetName));
        const hasWornListMatch = (r.wornGarmentTitles || []).some(t => t.toLowerCase() === targetName || (targetName && t.toLowerCase().includes(targetName)));
        const hasJunctionMatch = allJunctionsForModal.some(j => {
          const jRepId = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : (j.wearingReportId || j['wearing report id']);
          const jClothesId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : (j.clothesId || j['clothes(new)']);
          return jRepId === r.id && jClothesId === targetId;
        });

        if (hasIdMatch || hasNameMatch || hasWornListMatch || hasJunctionMatch) {
          const matchingJunc = allJunctionsForModal.find(j => {
            const jRepId = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : (j.wearingReportId || j['wearing report id']);
            const jClothesId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : (j.clothesId || j['clothes(new)']);
            return jRepId === r.id && (jClothesId === targetId || (targetName && (j.clothesTitle || '').toLowerCase() === targetName));
          });
          matchedReportsMap.set(r.id, { junction: matchingJunc, report: r });
        }
      }
    });

    return Array.from(matchedReportsMap.values()).sort((a, b) => {
      const dateA = a.report?.['wear date'] ? new Date(a.report['wear date']).getTime() : 0;
      const dateB = b.report?.['wear date'] ? new Date(b.report['wear date']).getTime() : 0;
      return dateB - dateA;
    });
  }, [garmentJunctions, allReportsForModal, allJunctionsForModal, garment]);

  const priceVal = typeof garment['price'] === 'number' ? garment['price'] : undefined;
  const washCount = garment['Total Washed Count'] || stats.totalWears;
  const costPerWear = priceVal && washCount > 0
    ? (priceVal / washCount).toFixed(2)
    : null;

  const handleRunAiAnalysis = async () => {
    setAnalyzing(true);
    setErrorMsg('');
    try {
      const dictionaryPrompt = fieldDictionaryService.buildDictionaryPrompt('clothes');

      // Fetch freshest garment data from dataService
      const freshGarment = dataService.getGarment(garment.id) || garment;
      const combinedCustom = {
        ...(garment.customFields || {}),
        ...(garment.extraAttributes || {}),
        ...(freshGarment.customFields || {}),
        ...(freshGarment.extraAttributes || {}),
        _fieldExplanations: editFieldExplanations
      };

      // Consolidate current edited state with fresh garment properties
      const currentGarment = {
        ...freshGarment,
        ...garment,
        Name: editName || garment['Name'] || garment.name,
        name: editName || garment['Name'] || garment.name,
        'Clothes Type': editClothesType || garment['Clothes Type'] || garment.clothesType,
        clothesType: editClothesType || garment['Clothes Type'] || garment.clothesType,
        material: editMaterial || garment['material'] || garment.materialProp,
        'thick clothes': editThickClothes || garment['thick clothes'] || garment.thickClothes,
        thickClothes: editThickClothes || garment['thick clothes'] || garment.thickClothes,
        'Hot at noon': editHotAtNoon || garment['Hot at noon'] || garment.hotAtNoon,
        hotAtNoon: editHotAtNoon || garment['Hot at noon'] || garment.hotAtNoon,
        'diao dai': editDiaoDai || garment['diao dai'] || garment.diaoDai,
        diaoDai: editDiaoDai || garment['diao dai'] || garment.diaoDai,
        'side breast ok: with 聚拢 bra': editSideBreastOkWithGatherBra || garment['side breast ok: with 聚拢 bra'] || garment['side breast ok: With 聚拢bra'] || garment.sideBreastOkWithGatherBra || garment['side breast ok'],
        sideBreastOkWithGatherBra: editSideBreastOkWithGatherBra || garment['side breast ok: with 聚拢 bra'] || garment['side breast ok: With 聚拢bra'] || garment.sideBreastOkWithGatherBra || garment['side breast ok'],
        'side breast ok: with side out bra': editSideBreastOkWithSideOutBra || garment['side breast ok: with side out bra'] || garment['side breast ok: With side out bra'] || garment.sideBreastOkWithSideOutBra,
        sideBreastOkWithSideOutBra: editSideBreastOkWithSideOutBra || garment['side breast ok: with side out bra'] || garment['side breast ok: With side out bra'] || garment.sideBreastOkWithSideOutBra,
        'side breast ok': editSideBreastOkWithGatherBra || editSideBreastOkWithSideOutBra || garment['side breast ok'] || garment.sideBreastOk,
        sideBreastOk: editSideBreastOkWithGatherBra || editSideBreastOkWithSideOutBra || garment['side breast ok'] || garment.sideBreastOk,
        'sit breast ok: With 聚拢bra': editSitBreastOkWithGatherBra || garment['sit breast ok: With 聚拢bra'] || garment['sit breast ok: with 聚拢bra'] || garment.sitBreastOkWithGatherBra || garment['sit breast ok'],
        sitBreastOkWithGatherBra: editSitBreastOkWithGatherBra || garment['sit breast ok: With 聚拢bra'] || garment['sit breast ok: with 聚拢bra'] || garment.sitBreastOkWithGatherBra || garment['sit breast ok'],
        'sit breast ok: with side out bra': editSitBreastOkWithSideOutBra || garment['sit breast ok: with side out bra'] || garment['sit breast ok: With side out bra'] || garment.sitBreastOkWithSideOutBra,
        sitBreastOkWithSideOutBra: editSitBreastOkWithSideOutBra || garment['sit breast ok: with side out bra'] || garment['sit breast ok: With side out bra'] || garment.sitBreastOkWithSideOutBra,
        'sit breast ok': editSitBreastOkWithGatherBra || editSitBreastOkWithSideOutBra || garment['sit breast ok'] || garment.sitBreastOk,
        sitBreastOk: editSitBreastOkWithGatherBra || editSitBreastOkWithSideOutBra || garment['sit breast ok'] || garment.sitBreastOk,
        // Multi-scenario climate ergonomics
        'when with aircond: hot feeling': editWhenAircondHot || garment['when with aircond: hot feeling'],
        'when with aircond: leg feeling': editWhenAircondLeg || garment['when with aircond: leg feeling'],
        'when without aircond: hot feeling': editWhenNoAircondHot || garment['when without aircond: hot feeling'],
        'when without aircond: leg feeling': editWhenNoAircondLeg || garment['when without aircond: leg feeling'],
        'when sleep under fan: hot feeling': editWhenSleepFanHot || garment['when sleep under fan: hot feeling'],
        'when sleep under fan: leg feeling': editWhenSleepFanLeg || garment['when sleep under fan: leg feeling'],
        'center chest': editCenterChest || garment['center chest'] || garment.centerChest,
        centerChest: editCenterChest || garment['center chest'] || garment.centerChest,
        beautiful: editBeautiful || garment['beautiful'] || garment.beautiful,
        'beatiful reason': editBeatifulReason || garment['beatiful reason'] || garment.beatifulReason,
        wet: editWet || garment['wet'] || garment.wet,
        'wet reason': editWetReason || garment['wet reason'] || garment.wetReason,
        'long dress': editLongDress || garment['long dress'] || garment.longDress,
        longDress: editLongDress || garment['long dress'] || garment.longDress,
        price: editPrice ? parseFloat(editPrice) : garment.price,
        'may throw': editMayThrow || garment['may throw'] || garment.mayThrow,
        mayThrow: editMayThrow || garment['may throw'] || garment.mayThrow,
        'Last Washed': editLastWashed || garment['Last Washed'] || garment.lastWashed,
        Descriptions: editDescriptions !== undefined ? editDescriptions : (garment['Descriptions'] || garment.descriptions),
        descriptions: editDescriptions !== undefined ? editDescriptions : (garment['Descriptions'] || garment.descriptions),
        'Descriptions 2': editDescriptions2 !== undefined ? editDescriptions2 : (garment['Descriptions 2'] || garment.descriptions2),
        descriptions2: editDescriptions2 !== undefined ? editDescriptions2 : (garment['Descriptions 2'] || garment.descriptions2),
        'last comment or action': editLastComment !== undefined ? editLastComment : (garment['last comment or action'] || garment.lastComment),
        lastComment: editLastComment !== undefined ? editLastComment : (garment['last comment or action'] || garment.lastComment),
        tags: editTags,
        fieldExplanations: editFieldExplanations,
        customFields: combinedCustom,
        extraAttributes: combinedCustom
      };

      // Extract all comprehensively linked reports and junctions from wornReports
      const linkedReports = wornReports.map(w => w.report).filter(Boolean);

      const res = await fetch('/api/ai/analyze-garment', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          garment: currentGarment,
          reports: linkedReports,
          junctions: allJunctionsForModal,
          apiKey: getCustomApiKey(),
          model: getSelectedModel(),
          dictionaryPrompt
        })
      });
      const data = await res.json();
      if (res.ok && data.analysis) {
        setAiAnalysis(data.analysis);
        const existing = garment.aiReports || [];
        const updatedGarment = await dataService.addGarmentAIReportVersion(garment.id, {
          content: data.analysis,
          title: `Report #${existing.length + 1}`,
          model: data.model || getSelectedModel(),
          isPrimary: existing.length === 0
        });
        if (updatedGarment) {
          onUpdateGarment(garment.id, updatedGarment);
          const newVer = updatedGarment.aiReports?.[0];
          if (newVer) {
            setActiveVersionId(newVer.id);
          }
        }
      } else {
        setErrorMsg(data.error || 'Failed to generate analysis.');
      }
    } catch (e: any) {
      setErrorMsg(e.message || 'Error communicating with AI service.');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleStartEditingAiReport = (version: AIReportVersion) => {
    setIsEditingAiReport(true);
    setEditingReportTitle(version.title || '');
    setEditingReportContent(version.content || '');
  };

  const handleSaveEditingAiReport = async (versionId: string) => {
    if (!editingReportContent.trim()) return;
    const updatedGarment = await dataService.updateGarmentAIReportVersion(garment.id, versionId, {
      title: editingReportTitle.trim(),
      content: editingReportContent.trim()
    });
    if (updatedGarment) {
      onUpdateGarment(garment.id, updatedGarment);
    }
    setIsEditingAiReport(false);
  };

  const handleCancelEditingAiReport = () => {
    setIsEditingAiReport(false);
    setEditingReportTitle('');
    setEditingReportContent('');
  };

  const handleSetPrimaryVersion = async (versionId: string) => {
    const updatedGarment = await dataService.setPrimaryGarmentAIReportVersion(garment.id, versionId);
    if (updatedGarment) {
      onUpdateGarment(garment.id, updatedGarment);
      setActiveVersionId(versionId);
    }
  };

  const handleDeleteAiReportVersion = async (versionId: string) => {
    const updatedGarment = await dataService.deleteGarmentAIReportVersion(garment.id, versionId);
    if (updatedGarment) {
      onUpdateGarment(garment.id, updatedGarment);
      const remaining = updatedGarment.aiReports || [];
      const newActive = remaining.find(v => v.isPrimary) || remaining[0];
      setActiveVersionId(newActive?.id || null);
      if (remaining.length === 0) {
        setAiAnalysis('');
      }
    }
    setIsEditingAiReport(false);
  };

  const handleCopyAiReport = (content: string, versionId: string) => {
    navigator.clipboard.writeText(content);
    setCopiedVersionId(versionId);
    setTimeout(() => setCopiedVersionId(null), 2000);
  };

  // Custom Dynamic Attributes Handlers for this Garment
  const handleAddGarmentCustomField = async (key: string, val: any) => {
    await dataService.setGarmentCustomField(garment.id, key, val);
    const updated = { ...(garment.customFields || garment.extraAttributes || {}), [key]: val };
    garment.customFields = updated;
    garment.extraAttributes = updated;
    (garment as any)[key] = val;
    setDataVersion(v => v + 1);
    onUpdateGarment(garment.id, { customFields: updated, extraAttributes: updated, [key]: val });
  };

  const handleUpdateGarmentCustomField = async (key: string, val: any) => {
    await dataService.setGarmentCustomField(garment.id, key, val);
    const updated = { ...(garment.customFields || garment.extraAttributes || {}), [key]: val };
    garment.customFields = updated;
    garment.extraAttributes = updated;
    (garment as any)[key] = val;
    setDataVersion(v => v + 1);
    onUpdateGarment(garment.id, { customFields: updated, extraAttributes: updated, [key]: val });
  };

  const handleDeleteGarmentCustomField = async (key: string) => {
    await dataService.removeGarmentCustomField(garment.id, key);
    const updated = { ...(garment.customFields || garment.extraAttributes || {}) };
    delete updated[key];
    delete (updated as any)[key];
    delete (garment as any)[key];
    garment.customFields = updated;
    garment.extraAttributes = updated;
    setDataVersion(v => v + 1);
    onUpdateGarment(garment.id, { customFields: updated, extraAttributes: updated });
  };

  // Picture file upload handler (Uploads to Firebase Cloud Storage with local fallback)
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>, directSave = false) => {
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
          const storagePic = await uploadGarmentPhotoToStorage(garment.id, file, (pct) => {
            const basePct = Math.round((i / files.length) * 100);
            const fileContrib = Math.round((pct / files.length));
            setUploadPercent(Math.min(99, basePct + fileContrib));
          });
          newPics.push(storagePic);
        } catch (storageErr) {
          console.warn('Direct Cloud Storage upload encountered an error, using fallback:', storageErr);
          const processed = await processImageFile(file);
          newPics.push(processed);
        }
      }

      setUploadPercent(100);
      if (directSave) {
        const updatedPics = [...(garment['Pictures'] || []), ...newPics];
        onUpdateGarment(garment.id, { 'Pictures': updatedPics });
      } else {
        setEditPictures(prev => [...prev, ...newPics]);
      }
    } catch (err) {
      console.error('Failed to process image:', err);
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
      deleteFileFromStorage(target.storagePath).catch(err => console.warn('Storage delete cleanup error:', err));
    }
    setEditPictures(prev => prev.filter(p => p.id !== picId));
  };

  const handleDirectDeletePhoto = (picId: string) => {
    const target = (garment['Pictures'] || []).find(p => p.id === picId);
    if (target?.storagePath) {
      deleteFileFromStorage(target.storagePath).catch(err => console.warn('Storage delete cleanup error:', err));
    }
    const updatedPics = (garment['Pictures'] || []).filter(p => p.id !== picId);
    onUpdateGarment(garment.id, { 'Pictures': updatedPics });
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

  const handleSaveEdit = () => {
    const numPrice = editPrice ? parseFloat(editPrice) : undefined;
    const updates: Partial<ClothesItem> = {
      'Name': editName.trim() || garment['Name'],
      'Clothes Type': editClothesType,
      'material': editMaterial,
      'thick clothes': editThickClothes,
      'Hot at noon': editHotAtNoon,
      'diao dai': editDiaoDai,
      'side breast ok': editSideBreastOkWithGatherBra || editSideBreastOkWithSideOutBra || garment['side breast ok'],
      'side breast ok: with 聚拢 bra': editSideBreastOkWithGatherBra,
      'side breast ok: with side out bra': editSideBreastOkWithSideOutBra,
      'sit breast ok': editSitBreastOkWithGatherBra || editSitBreastOkWithSideOutBra || garment['sit breast ok'],
      'sit breast ok: With 聚拢bra': editSitBreastOkWithGatherBra,
      'sit breast ok: with side out bra': editSitBreastOkWithSideOutBra,
      // Multi-scenario climate ergonomics
      'when with aircond: hot feeling': editWhenAircondHot,
      'when with aircond: leg feeling': editWhenAircondLeg,
      'when without aircond: hot feeling': editWhenNoAircondHot,
      'when without aircond: leg feeling': editWhenNoAircondLeg,
      'when sleep under fan: hot feeling': editWhenSleepFanHot,
      'when sleep under fan: leg feeling': editWhenSleepFanLeg,
      'center chest': editCenterChest,
      'beautiful': editBeautiful,
      'beatiful reason': editBeatifulReason,
      'wet': editWet as any,
      'wet reason': editWetReason,
      'long dress': editLongDress,
      'price': isNaN(numPrice as number) ? undefined : numPrice,
      'may throw': editMayThrow,
      'Last Washed': editLastWashed ? `${editLastWashed}T00:00:00.000Z` : '',
      'Descriptions': editDescriptions,
      'Descriptions 2': editDescriptions2,
      'last comment or action': editLastComment,
      'tags': editTags,
      'Pictures': editPictures,
      fieldExplanations: editFieldExplanations
    };

    // Mirror to customFields for backwards compatibility and easy backup inspection
    const existingCustom = { ...(garment.customFields || garment.extraAttributes || {}) };
    Object.entries(editFieldExplanations).forEach(([k, v]) => {
      if (v && v.trim()) existingCustom[`${k}_explain`] = v.trim();
      else delete existingCustom[`${k}_explain`];
    });
    existingCustom._fieldExplanations = editFieldExplanations;
    updates.customFields = existingCustom;
    updates.extraAttributes = existingCustom;

    onUpdateGarment(garment.id, updates);
    setIsEditing(false);
  };

  const cancelEdit = () => {
    // Reset to current garment values
    setEditName(garment['Name'] || '');
    setEditClothesType(garment['Clothes Type'] || 'Dress');
    setEditMaterial(garment['material'] || '');
    setEditThickClothes(garment['thick clothes'] || '');
    setEditHotAtNoon(garment['Hot at noon'] || '');
    setEditDiaoDai(garment['diao dai'] || '');
    setEditSideBreastOk(garment['side breast ok'] || '');
    setEditSideBreastOkWithGatherBra(
      garment['side breast ok: with 聚拢 bra'] || garment['side breast ok: With 聚拢bra'] || garment.sideBreastOkWithGatherBra || garment['side breast ok'] || ''
    );
    setEditSideBreastOkWithSideOutBra(
      garment['side breast ok: with side out bra'] || garment['side breast ok: With side out bra'] || garment.sideBreastOkWithSideOutBra || ''
    );
    setEditSitBreastOkWithGatherBra(
      garment['sit breast ok: With 聚拢bra'] || garment['sit breast ok: with 聚拢bra'] || garment.sitBreastOkWithGatherBra || garment['sit breast ok'] || ''
    );
    setEditSitBreastOkWithSideOutBra(
      garment['sit breast ok: with side out bra'] || garment['sit breast ok: With side out bra'] || garment.sitBreastOkWithSideOutBra || ''
    );
    setEditWhenAircondHot(garment['when with aircond: hot feeling'] || '');
    setEditWhenAircondLeg(garment['when with aircond: leg feeling'] || '');
    setEditWhenNoAircondHot(garment['when without aircond: hot feeling'] || '');
    setEditWhenNoAircondLeg(garment['when without aircond: leg feeling'] || '');
    setEditWhenSleepFanHot(garment['when sleep under fan: hot feeling'] || '');
    setEditWhenSleepFanLeg(garment['when sleep under fan: leg feeling'] || '');
    setEditCenterChest(garment['center chest'] || '');
    setEditBeautiful(garment['beautiful'] || '');
    setEditBeatifulReason(garment['beatiful reason'] || '');
    setEditWet(garment['wet'] || 'dry');
    setEditWetReason(garment['wet reason'] || '');
    setEditLongDress(garment['long dress'] || '');
    setEditPrice(garment['price'] !== undefined ? String(garment['price']) : '');
    setEditMayThrow(garment['may throw'] || '');
    setEditLastWashed(garment['Last Washed'] ? toDateKey(garment['Last Washed']) || '' : '');
    setEditDescriptions(garment['Descriptions'] || '');
    setEditDescriptions2(garment['Descriptions 2'] || '');
    setEditLastComment(garment['last comment or action'] || '');
    setEditTags(garment['tags'] || []);
    setEditPictures(garment['Pictures'] || []);
    setEditFieldExplanations(garment.fieldExplanations || garment.customFields?._fieldExplanations || {});
    setExpandedExplainFields({});
    setIsEditing(false);
  };

  const pictures = garment['Pictures'] || [];

  const handleConfirmDelete = async () => {
    if (!garment) return;
    setIsDeleting(true);
    try {
      if (onDeleteGarment) {
        await onDeleteGarment(garment.id);
      } else {
        await dataService.deleteGarment(garment.id);
      }
      setShowDeleteConfirm(false);
      onClose();
    } catch (err) {
      console.error('Failed to delete garment', err);
    } finally {
      setIsDeleting(false);
    }
  };

  const getFieldExplainText = (key: string): string => {
    return garment.fieldExplanations?.[key] ||
      garment.customFields?._fieldExplanations?.[key] ||
      garment.customFields?.[`${key}_explain`] ||
      '';
  };

  const renderViewAttr = (key: string, label: string, valueNode: React.ReactNode, isFullWidth = false) => {
    const explain = getFieldExplainText(key);
    return (
      <div className={`py-2.5 border-b border-slate-200/60 min-w-0 ${isFullWidth ? 'sm:col-span-2' : ''}`}>
        <div className="flex flex-col gap-1 min-w-0">
          <span className="font-mono text-slate-500 shrink-0 text-xs font-semibold">{label}:</span>
          <div className="text-left font-semibold text-slate-900 break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap text-xs sm:text-sm min-w-0 w-full">{valueNode}</div>
        </div>
        {explain && (
          <div className="mt-2 flex items-start gap-2 p-2.5 rounded-xl bg-purple-50/90 border border-purple-200/70 text-purple-950 text-xs leading-relaxed break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap min-w-0">
            <MessageSquare className="w-3.5 h-3.5 text-purple-600 shrink-0 mt-0.5" />
            <div className="flex-1 min-w-0">
              <span className="font-bold text-[10px] text-purple-700 block uppercase font-mono mb-0.5">专属体感说明 (Explain):</span>
              <span className="font-medium select-text break-words [word-break:normal] [overflow-wrap:break-word]">{explain}</span>
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      {/* Full-screen dialog (Not floating, edge-to-edge) */}
      <div
        id="garment-detail-dialog"
        className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto"
      >
        {/* Header Bar */}
        <div className="flex items-center justify-between px-3 sm:px-6 py-2.5 sm:py-3 border-b border-slate-200/80 bg-white/95 backdrop-blur-md shrink-0 w-full max-w-full overflow-x-hidden">
          <div className="flex items-center gap-1.5 sm:gap-2 min-w-0">
            <button
              type="button"
              onClick={isEditing ? () => setIsEditing(false) : onClose}
              className="p-1.5 -ml-1 sm:ml-0 rounded-xl text-slate-600 hover:text-slate-900 hover:bg-slate-200/70 transition-colors shrink-0 cursor-pointer"
              title="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            {isEditing ? (
              <div className="flex items-center gap-1.5 text-slate-900 font-bold text-xs sm:text-sm truncate">
                <Edit3 className="w-4 h-4 text-slate-700 shrink-0" />
                <span className="truncate">Editing Garment</span>
              </div>
            ) : (
              <div className="flex items-center gap-1.5 min-w-0 truncate">
                <span className="px-2 sm:px-2.5 py-0.5 sm:py-1 rounded-full text-[11px] sm:text-xs font-bold bg-slate-200 text-slate-800 shrink-0 truncate max-w-[90px] sm:max-w-none">
                  {garment['Clothes Type']}
                </span>
                {garment['beautiful'] && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 sm:py-1 rounded-full text-[11px] sm:text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200 truncate hidden xs:inline-flex">
                    <Sparkles className="w-3 h-3 text-amber-500 fill-amber-400 shrink-0" />
                    <span className="truncate">{garment['beautiful']}</span>
                  </span>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center gap-1 sm:gap-2 shrink-0">
            <FontSizeSwitcher />
            {!isEditing ? (
              <>
                <button
                  type="button"
                  onClick={handleRunAiAnalysis}
                  disabled={analyzing}
                  className="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1.5 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition-all shadow-xs disabled:opacity-60 active:scale-95 cursor-pointer"
                  title="Run Teable-style ergonomic, climate & fit AI diagnostic"
                >
                  <Sparkles className={`w-3.5 h-3.5 ${analyzing ? 'animate-spin' : 'text-purple-200'}`} />
                  <span className="hidden sm:inline">{analyzing ? 'Analyzing...' : 'Teable AI'}</span>
                  <span className="sm:hidden text-[11px]">{analyzing ? '...' : 'AI'}</span>
                </button>

                <button
                  type="button"
                  onClick={() => setIsEditing(true)}
                  className="inline-flex items-center gap-1 px-2.5 sm:px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-800 hover:border-slate-500 border border-slate-300 bg-transparent transition-colors active:scale-95 cursor-pointer"
                >
                  <Edit3 className="w-3.5 h-3.5 text-slate-600" />
                  <span className="hidden xs:inline">Edit</span>
                </button>

                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  className="p-1.5 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                  title="Delete Garment"
                >
                  <Trash2 className="w-4 h-4" />
                </button>

                <button
                  onClick={onClose}
                  className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors cursor-pointer"
                  aria-label="Close modal"
                >
                  <X className="w-5 h-5" />
                </button>
              </>
            ) : (
              <div className="flex items-center gap-1.5 sm:gap-2">
                <button
                  type="button"
                  onClick={cancelEdit}
                  className="px-2.5 sm:px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 border border-slate-300 bg-transparent transition-colors cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveEdit}
                  className="inline-flex items-center gap-1 px-3 sm:px-4 py-1.5 rounded-xl text-xs font-bold border border-slate-900 text-slate-900 bg-transparent hover:border-slate-700 transition-all active:scale-95 cursor-pointer"
                >
                  <Save className="w-3.5 h-3.5 text-slate-900" />
                  <span className="hidden xs:inline">Save Changes</span>
                  <span className="xs:hidden">Save</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Modal Scrollable Body */}
        <div className="p-3.5 sm:p-6 overflow-y-auto overflow-x-hidden flex-1 space-y-6 w-full max-w-full">
            {isEditing ? (
              /* ================= EDIT MODE ================= */
              <div className="space-y-6">
                {/* Basic Information */}
                <div className="p-4 sm:p-5 rounded-2xl bg-purple-50/40 border border-purple-100 space-y-4">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-bold text-purple-900 uppercase font-mono tracking-wider">
                      Core Information
                    </h4>
                    <button
                      type="button"
                      onClick={() => {
                        const coreKeys = ['Name', 'Clothes Type', 'material', 'price'];
                        const anyExpanded = coreKeys.some(k => expandedExplainFields[k]);
                        setExpandedExplainFields(prev => {
                          const next = { ...prev };
                          coreKeys.forEach(k => { next[k] = !anyExpanded; });
                          return next;
                        });
                      }}
                      className="text-[10px] font-semibold text-purple-700 hover:text-purple-900 px-2 py-0.5 rounded-lg hover:bg-purple-100/70 border border-purple-200 transition-colors cursor-pointer flex items-center gap-1"
                    >
                      <MessageSquare className="w-3 h-3" />
                      <span>{['Name', 'Clothes Type', 'material', 'price'].some(k => expandedExplainFields[k]) ? '收起说明' : '展开说明'}</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                        Garment Name *
                      </label>
                      <input
                        type="text"
                        value={editName}
                        onChange={(e) => setEditName(e.target.value)}
                        placeholder="e.g. 062 black knitted top"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                      />
                      {renderFieldExplainBox('Name', 'Garment Name', '补充命名原因或此单品特别备注...')}
                    </div>

                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">
                          Clothes Type *
                        </label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('Clothes Type', 'Clothes Type', setEditClothesType)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 transition-colors shadow-2xs"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <div className="relative flex items-center">
                        <input
                          type="text"
                          value={editClothesType}
                          onChange={(e) => setEditClothesType(e.target.value)}
                          placeholder="e.g. Dress, bra, Tshirt, Pant, coat..."
                          className="w-full px-3 py-2 pr-20 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                        />
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('Clothes Type', 'Clothes Type', setEditClothesType)}
                          className="absolute right-1.5 px-2.5 py-1 text-[11px] font-bold text-purple-700 bg-purple-50 hover:bg-purple-100 rounded-lg border border-purple-200 transition-colors"
                        >
                          Choose
                        </button>
                      </div>
                      {renderFieldExplainBox('Clothes Type', 'Clothes Type', '例如: dress normally more feminine. Got better waist and breast cutting.')}
                    </div>

                    {/* Material with Previous button */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">
                          material
                        </label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('material', 'Material', setEditMaterial)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 transition-colors shadow-2xs"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editMaterial}
                        onChange={(e) => setEditMaterial(e.target.value)}
                        placeholder="e.g. Chiffon, Cotton, Silk"
                      />
                      {renderFieldExplainBox('material', 'Material', '例如: lace after washing machine more likely dry not wet. But lace also more hot at noon.')}
                    </div>

                    {/* Price */}
                    <div>
                      <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                        price ($)
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        value={editPrice}
                        onChange={(e) => setEditPrice(e.target.value)}
                        placeholder="e.g. 49.99"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                      />
                      {renderFieldExplainBox('price', 'Price', '价格、购入渠道或性价比体感备注...')}
                    </div>
                  </div>
                </div>

                {/* Garment Attribute Properties with Previous buttons */}
                <div className="p-4 sm:p-5 rounded-2xl bg-slate-50/70 border border-slate-200/80 space-y-4">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                      Garment Attributes &amp; Ergonomics
                    </h4>
                    <button
                      type="button"
                      onClick={() => {
                        const ergoKeys = [
                          'thick clothes', 'Hot at noon', 'diao dai',
                          'side breast ok: with 聚拢 bra', 'side breast ok: with side out bra',
                          'sit breast ok: With 聚拢bra', 'sit breast ok: with side out bra', 'center chest', 'beautiful', 'beatiful reason',
                          'wet', 'wet reason', 'long dress', 'may throw', 'Last Washed',
                          'when with aircond: hot feeling', 'when with aircond: leg feeling',
                          'when without aircond: hot feeling', 'when without aircond: leg feeling',
                          'when sleep under fan: hot feeling', 'when sleep under fan: leg feeling'
                        ];
                        const anyExpanded = ergoKeys.some(k => expandedExplainFields[k]);
                        setExpandedExplainFields(prev => {
                          const next = { ...prev };
                          ergoKeys.forEach(k => { next[k] = !anyExpanded; });
                          return next;
                        });
                      }}
                      className="text-[10px] font-semibold text-purple-700 hover:text-purple-900 px-2 py-0.5 rounded-lg hover:bg-purple-100/70 border border-slate-200 transition-colors cursor-pointer flex items-center gap-1"
                    >
                      <MessageSquare className="w-3 h-3" />
                      <span>{['thick clothes', 'Hot at noon', 'diao dai'].some(k => expandedExplainFields[k]) ? '收起说明' : '展开说明'}</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* thick clothes */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">thick clothes</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('thick clothes', 'Thick clothes', setEditThickClothes)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editThickClothes}
                        onChange={(e) => setEditThickClothes(e.target.value)}
                        placeholder="e.g. 01 not thick clothes"
                      />
                      {renderFieldExplainBox('thick clothes', 'Thick Clothes', '面料厚度、保暖或透气实测感受...')}
                    </div>

                    {/* Hot at noon */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">Hot at noon</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('Hot at noon', 'Hot at noon', setEditHotAtNoon)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editHotAtNoon}
                        onChange={(e) => setEditHotAtNoon(e.target.value)}
                        placeholder="e.g. noon not hot / cool"
                      />
                      {renderFieldExplainBox('Hot at noon', 'Hot at noon', '中午炎热度体感（如: noon not hot / cool but fabric thin）...')}
                    </div>

                    {/* diao dai */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">diao dai</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('diao dai', 'Diao dai', setEditDiaoDai)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editDiaoDai}
                        onChange={(e) => setEditDiaoDai(e.target.value)}
                        placeholder="e.g. not diao dai / diao dai"
                      />
                      {renderFieldExplainBox('diao dai', 'Diao dai', '例如: diao dai not hook at noon, but pain in shoulder.')}
                    </div>

                    {/* side breast ok: with 聚拢 bra */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">side breast ok: with 聚拢 bra</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('side breast ok: with 聚拢 bra', 'side breast ok: with 聚拢 bra', setEditSideBreastOkWithGatherBra)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editSideBreastOkWithGatherBra}
                        onChange={(e) => setEditSideBreastOkWithGatherBra(e.target.value)}
                        placeholder="e.g. side breast shape ok"
                      />
                      {renderFieldExplainBox('side breast ok: with 聚拢 bra', 'side breast ok: with 聚拢 bra', '例如: 配合聚拢bra侧面胸型优美饱满 / side breast shape ok...')}
                    </div>

                    {/* side breast ok: with side out bra */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">side breast ok: with side out bra</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('side breast ok: with side out bra', 'side breast ok: with side out bra', setEditSideBreastOkWithSideOutBra)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editSideBreastOkWithSideOutBra}
                        onChange={(e) => setEditSideBreastOkWithSideOutBra(e.target.value)}
                        placeholder="e.g. side breast out / ugly"
                      />
                      {renderFieldExplainBox('side breast ok: with side out bra', 'side breast ok: with side out bra', '例如: side breast out, 配合侧开内衣侧面副乳外溢或扁塌...')}
                    </div>

                    {/* sit breast ok: With 聚拢bra */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">sit breast ok: With 聚拢bra</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('sit breast ok: With 聚拢bra', 'sit breast ok: With 聚拢bra', setEditSitBreastOkWithGatherBra)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editSitBreastOkWithGatherBra}
                        onChange={(e) => setEditSitBreastOkWithGatherBra(e.target.value)}
                        placeholder="e.g. sit breast shape ok"
                      />
                      {renderFieldExplainBox('sit breast ok: With 聚拢bra', 'sit breast ok: With 聚拢bra', '例如: 配合聚拢bra胸型美观稳定 / sit breast shape ok...')}
                    </div>

                    {/* sit breast ok: with side out bra */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">sit breast ok: with side out bra</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('sit breast ok: with side out bra', 'sit breast ok: with side out bra', setEditSitBreastOkWithSideOutBra)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editSitBreastOkWithSideOutBra}
                        onChange={(e) => setEditSitBreastOkWithSideOutBra(e.target.value)}
                        placeholder="e.g. ugly / sit breast shape ok / stand breast ugly, because loose tie"
                      />
                      {renderFieldExplainBox('sit breast ok: with side out bra', 'sit breast ok: with side out bra', '例如: ugly, 没有聚拢效果显扁塌 / stand breast ugly...')}
                    </div>

                    {/* center chest */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">center chest</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('center chest', 'Center chest', setEditCenterChest)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editCenterChest}
                        onChange={(e) => setEditCenterChest(e.target.value)}
                        placeholder="e.g. center chest comfortable"
                      />
                      {renderFieldExplainBox('center chest', 'Center chest', '正中胸骨受压或聚拢舒适度...')}
                    </div>

                    {/* beautiful & beatiful reason */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">beautiful</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('beautiful', 'Appearance', setEditBeautiful)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editBeautiful}
                        onChange={(e) => setEditBeautiful(e.target.value)}
                        placeholder="e.g. Beautiful, Cute, Elegant"
                      />
                      {renderFieldExplainBox('beautiful', 'Beautiful', '外观拍照美感、上身体型修饰度...')}
                    </div>

                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">beatiful reason</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('beatiful reason', 'Appearance reason', setEditBeatifulReason)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editBeatifulReason}
                        onChange={(e) => setEditBeatifulReason(e.target.value)}
                        placeholder="e.g. classic cut, slimming waist"
                      />
                      {renderFieldExplainBox('beatiful reason', 'Beautiful Reason', '版型设计美观或显瘦的具体原因...')}
                    </div>

                    {/* wet & wet reason */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600 flex items-center gap-1">
                          <Droplets className="w-3.5 h-3.5 text-blue-500" />
                          wet
                        </label>
                        <div className="flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => setCreateOptionConfig({
                              isOpen: true,
                              fieldName: 'wet',
                              fieldLabel: 'Wet status (潮湿状态)',
                              onSave: (val) => setEditWet(val)
                            })}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 transition-colors shadow-2xs cursor-pointer"
                            title="为 wet 字段新建选项 (如 middle)"
                          >
                            <Plus className="w-3 h-3 text-purple-600" />
                            + 新建选项
                          </button>
                          <button
                            type="button"
                            onClick={() => openPreviousPicker('wet', 'Wet status', setEditWet, editWet)}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 cursor-pointer"
                          >
                            <History className="w-3 h-3 text-purple-600" />
                            Previous
                          </button>
                        </div>
                      </div>

                      <div className="space-y-1.5">
                        <select
                          value={editWet}
                          onChange={(e) => {
                            if (e.target.value === '__NEW_OPTION__') {
                              setCreateOptionConfig({
                                isOpen: true,
                                fieldName: 'wet',
                                fieldLabel: 'Wet status (潮湿状态)',
                                onSave: (val) => setEditWet(val)
                              });
                            } else {
                              setEditWet(e.target.value);
                            }
                          }}
                          className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white font-medium text-slate-900 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                        >
                          {wetOptions.map(opt => (
                            <option key={opt.value} value={opt.value}>
                              {opt.label}
                            </option>
                          ))}
                          <option value="__NEW_OPTION__" className="text-purple-700 font-bold bg-purple-50">
                            ➕ + 新建自定义选项 (如 middle / custom)...
                          </option>
                        </select>

                        <div className="flex items-center justify-between text-[11px] text-slate-500 px-1">
                          <span>
                            当前选中: <strong className="font-mono text-purple-700 font-bold">{editWet || '未选择'}</strong>
                          </span>
                          <button
                            type="button"
                            onClick={() => {
                              setCreateOptionConfig({
                                isOpen: true,
                                fieldName: 'wet',
                                fieldLabel: 'Wet status (潮湿状态)',
                                onSave: (val) => setEditWet(val)
                              });
                            }}
                            className="text-purple-600 hover:text-purple-800 hover:underline cursor-pointer text-[10px] font-semibold flex items-center gap-0.5"
                          >
                            ➕ 新建 / 添加自定义选项
                          </button>
                        </div>
                      </div>
                      {renderFieldExplainBox('wet', 'Wet status', '出汗潮湿或速干排汗实测表现...')}
                    </div>

                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">wet reason</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('wet reason', 'Drying reason', setEditWetReason)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editWetReason}
                        onChange={(e) => setEditWetReason(e.target.value)}
                        placeholder="e.g. quick-dry air hung"
                      />
                      {renderFieldExplainBox('wet reason', 'Wet Reason', '透气性、晾晒风干速度等体感记录...')}
                    </div>

                    {/* long dress */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">long dress</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('long dress', 'Dress length', setEditLongDress)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editLongDress}
                        onChange={(e) => setEditLongDress(e.target.value)}
                        placeholder="e.g. long dress / short dress"
                      />
                      {renderFieldExplainBox('long dress', 'Dress length', '裙长行走舒适度、防走光或活动性...')}
                    </div>

                    {/* 🌡️ Multi-Scenario Climate & Sensory Ergonomics (when with aircond / without aircond / sleep under fan) */}
                    <div className="sm:col-span-2 p-3.5 sm:p-4 rounded-2xl bg-indigo-50/60 border border-indigo-200/80 space-y-3.5 my-1">
                      <div className="flex items-center justify-between">
                        <div>
                          <h5 className="text-xs font-bold text-indigo-950 uppercase font-mono tracking-wider flex items-center gap-1.5">
                            <ThermometerSun className="w-3.5 h-3.5 text-indigo-600" />
                            多场景气候温感与腿部体测 (Climate Scenarios)
                          </h5>
                          <p className="text-[11px] text-indigo-700/80 mt-0.5">
                            记录单品在有空调、无空调/常温、以及风扇睡眠三种真实环境下的体感 (hot feeling) 与下肢感受 (leg feeling)。
                          </p>
                        </div>
                      </div>

                      <div className="space-y-3">
                        {/* Scenario 1: when with aircond */}
                        <div className="p-3 rounded-xl bg-white border border-blue-200/80 shadow-2xs space-y-2.5">
                          <div className="flex items-center gap-2">
                            <span className="px-2 py-0.5 rounded-md text-[11px] font-bold bg-blue-100 text-blue-800 font-mono">
                              ❄️ when with aircond: (空调环境)
                            </span>
                          </div>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {/* hot feeling */}
                            <div>
                              <div className="flex items-center justify-between mb-1">
                                <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                                  &gt; hot feeling
                                </label>
                                <button
                                  type="button"
                                  onClick={() => openPreviousPicker('when with aircond: hot feeling', 'when with aircond: hot feeling', setEditWhenAircondHot)}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                                >
                                  <History className="w-3 h-3 text-purple-600" />
                                  Previous
                                </button>
                              </div>
                              <AutoResizeTextarea
                                value={editWhenAircondHot}
                                onChange={(e) => setEditWhenAircondHot(e.target.value)}
                                placeholder="e.g. not so cold / cold / comfortable"
                              />
                              {renderFieldExplainBox('when with aircond: hot feeling', 'when with aircond: hot feeling', '例如: due to thick chiffon material, thick clothes so no so cold...')}
                            </div>

                            {/* leg feeling */}
                            <div>
                              <div className="flex items-center justify-between mb-1">
                                <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                                  &gt; leg feeling
                                </label>
                                <button
                                  type="button"
                                  onClick={() => openPreviousPicker('when with aircond: leg feeling', 'when with aircond: leg feeling', setEditWhenAircondLeg)}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                                >
                                  <History className="w-3 h-3 text-purple-600" />
                                  Previous
                                </button>
                              </div>
                              <AutoResizeTextarea
                                value={editWhenAircondLeg}
                                onChange={(e) => setEditWhenAircondLeg(e.target.value)}
                                placeholder="e.g. not so cold / cold leg"
                              />
                              {renderFieldExplainBox('when with aircond: leg feeling', 'when with aircond: leg feeling', '例如: due to middle long dress, thick chiffon material, thick clothes...')}
                            </div>
                          </div>
                        </div>

                        {/* Scenario 2: when without aircond */}
                        <div className="p-3 rounded-xl bg-white border border-amber-200/80 shadow-2xs space-y-2.5">
                          <div className="flex items-center gap-2">
                            <span className="px-2 py-0.5 rounded-md text-[11px] font-bold bg-amber-100 text-amber-900 font-mono">
                              ☀️ when without aircond: (无空调/常温/中午)
                            </span>
                          </div>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {/* hot feeling */}
                            <div>
                              <div className="flex items-center justify-between mb-1">
                                <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                                  &gt; hot feeling
                                </label>
                                <button
                                  type="button"
                                  onClick={() => openPreviousPicker('when without aircond: hot feeling', 'when without aircond: hot feeling', setEditWhenNoAircondHot)}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                                >
                                  <History className="w-3 h-3 text-purple-600" />
                                  Previous
                                </button>
                              </div>
                              <AutoResizeTextarea
                                value={editWhenNoAircondHot}
                                onChange={(e) => setEditWhenNoAircondHot(e.target.value)}
                                placeholder="e.g. hot / middle hot / not hot"
                              />
                              {renderFieldExplainBox('when without aircond: hot feeling', 'when without aircond: hot feeling', '例如: due to thick chiffon material, thick clothes...')}
                            </div>

                            {/* leg feeling */}
                            <div>
                              <div className="flex items-center justify-between mb-1">
                                <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                                  &gt; leg feeling
                                </label>
                                <button
                                  type="button"
                                  onClick={() => openPreviousPicker('when without aircond: leg feeling', 'when without aircond: leg feeling', setEditWhenNoAircondLeg)}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                                >
                                  <History className="w-3 h-3 text-purple-600" />
                                  Previous
                                </button>
                              </div>
                              <AutoResizeTextarea
                                value={editWhenNoAircondLeg}
                                onChange={(e) => setEditWhenNoAircondLeg(e.target.value)}
                                placeholder="e.g. middle hot / hot and sweaty"
                              />
                              {renderFieldExplainBox('when without aircond: leg feeling', 'when without aircond: leg feeling', '例如: due to middle long dress, thick chiffon material, thick clothes.little more swear...')}
                            </div>
                          </div>
                        </div>

                        {/* Scenario 3: when sleep under fan */}
                        <div className="p-3 rounded-xl bg-white border border-teal-200/80 shadow-2xs space-y-2.5">
                          <div className="flex items-center gap-2">
                            <span className="px-2 py-0.5 rounded-md text-[11px] font-bold bg-teal-100 text-teal-900 font-mono">
                              🌀 when sleep under fan: (风扇睡眠环境)
                            </span>
                          </div>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                            {/* hot feeling */}
                            <div>
                              <div className="flex items-center justify-between mb-1">
                                <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                                  &gt; hot feeling
                                </label>
                                <button
                                  type="button"
                                  onClick={() => openPreviousPicker('when sleep under fan: hot feeling', 'when sleep under fan: hot feeling', setEditWhenSleepFanHot)}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                                >
                                  <History className="w-3 h-3 text-purple-600" />
                                  Previous
                                </button>
                              </div>
                              <AutoResizeTextarea
                                value={editWhenSleepFanHot}
                                onChange={(e) => setEditWhenSleepFanHot(e.target.value)}
                                placeholder="e.g. not hot / cool / hot"
                              />
                              {renderFieldExplainBox('when sleep under fan: hot feeling', 'when sleep under fan: hot feeling', '例如: under fan wind bigger and cold bed help reduce feeling of hot...')}
                            </div>

                            {/* leg feeling */}
                            <div>
                              <div className="flex items-center justify-between mb-1">
                                <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                                  &gt; leg feeling
                                </label>
                                <button
                                  type="button"
                                  onClick={() => openPreviousPicker('when sleep under fan: leg feeling', 'when sleep under fan: leg feeling', setEditWhenSleepFanLeg)}
                                  className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                                >
                                  <History className="w-3 h-3 text-purple-600" />
                                  Previous
                                </button>
                              </div>
                              <AutoResizeTextarea
                                value={editWhenSleepFanLeg}
                                onChange={(e) => setEditWhenSleepFanLeg(e.target.value)}
                                placeholder="e.g. middle cold / cold foot / warm"
                              />
                              {renderFieldExplainBox('when sleep under fan: leg feeling', 'when sleep under fan: leg feeling', '例如: due to middle long dress, inner thin, easy fold up 裙底，even it is thick clothes outer...')}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* may throw */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">
                          may throw
                        </label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('may throw', 'May Throw', setEditMayThrow)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 transition-colors shadow-2xs"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <AutoResizeTextarea
                        value={editMayThrow}
                        onChange={(e) => setEditMayThrow(e.target.value)}
                        placeholder="e.g. no / maybe / donate"
                      />
                      {renderFieldExplainBox('may throw', 'May throw', '断舍离考量、淘汰原因或保留理由...')}
                    </div>

                    {/* Last Washed Date */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600 flex items-center gap-1">
                          <Droplets className="w-3 h-3 text-blue-500" />
                          Last Washed Date
                        </label>
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => setEditLastWashed(getTodayDateKey())}
                            className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 transition-colors"
                          >
                            Today
                          </button>
                          {editLastWashed && (
                            <button
                              type="button"
                              onClick={() => setEditLastWashed('')}
                              className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 hover:bg-slate-200 text-slate-600 border border-slate-200 transition-colors"
                            >
                              Clear
                            </button>
                          )}
                        </div>
                      </div>
                      <input
                        type="date"
                        value={editLastWashed}
                        onChange={(e) => setEditLastWashed(e.target.value)}
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white font-mono"
                      />
                      {renderFieldExplainBox('Last Washed', 'Last Washed Date', '洗涤方式（手洗/机洗/洗衣袋）或洗后变化...')}
                    </div>
                  </div>
                </div>

                {/* Descriptions & Notes */}
                <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200 space-y-4">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                      Descriptions &amp; Observations
                    </h4>
                    <button
                      type="button"
                      onClick={() => {
                        const descKeys = ['Descriptions', 'Descriptions 2', 'last comment or action'];
                        const anyExpanded = descKeys.some(k => expandedExplainFields[k]);
                        setExpandedExplainFields(prev => {
                          const next = { ...prev };
                          descKeys.forEach(k => { next[k] = !anyExpanded; });
                          return next;
                        });
                      }}
                      className="text-[10px] font-semibold text-purple-700 hover:text-purple-900 px-2 py-0.5 rounded-lg hover:bg-purple-100/70 border border-slate-200 transition-colors cursor-pointer flex items-center gap-1"
                    >
                      <MessageSquare className="w-3 h-3" />
                      <span>{['Descriptions', 'Descriptions 2', 'last comment or action'].some(k => expandedExplainFields[k]) ? '收起说明' : '展开说明'}</span>
                    </button>
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="text-xs font-mono font-bold uppercase text-slate-600">Descriptions</label>
                      <button
                        type="button"
                        onClick={() => openPreviousPicker('Descriptions', 'Descriptions', setEditDescriptions)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                      >
                        <History className="w-3 h-3 text-purple-600" />
                        Previous
                      </button>
                    </div>
                    <AutoResizeTextarea
                      minHeight={56}
                      value={editDescriptions}
                      onChange={(e) => setEditDescriptions(e.target.value)}
                      placeholder="Detailed notes on silhouette, feel, or pairings..."
                    />
                    {renderFieldExplainBox('Descriptions', 'Descriptions', '整体版型或搭配体感重点补充...')}
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="text-xs font-mono font-bold uppercase text-slate-600">Descriptions 2</label>
                      <button
                        type="button"
                        onClick={() => openPreviousPicker('Descriptions 2', 'Descriptions 2', setEditDescriptions2)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                      >
                        <History className="w-3 h-3 text-purple-600" />
                        Previous
                      </button>
                    </div>
                    <AutoResizeTextarea
                      minHeight={56}
                      value={editDescriptions2}
                      onChange={(e) => setEditDescriptions2(e.target.value)}
                      placeholder="Secondary notes, care instructions..."
                    />
                    {renderFieldExplainBox('Descriptions 2', 'Descriptions 2', '次要体感细节或长期磨损观察...')}
                  </div>

                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="text-xs font-mono font-bold uppercase text-slate-600">
                        last comment or action
                      </label>
                      <button
                        type="button"
                        onClick={() => openPreviousPicker('last comment or action', 'Last comment or action', setEditLastComment)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 transition-colors shadow-2xs"
                      >
                        <History className="w-3 h-3 text-purple-600" />
                        Previous
                      </button>
                    </div>
                    <AutoResizeTextarea
                      value={editLastComment}
                      onChange={(e) => setEditLastComment(e.target.value)}
                      placeholder="e.g. Iron before next wear"
                    />
                    {renderFieldExplainBox('last comment or action', 'Last comment or action', '最新处理意见或下次穿着提醒...')}
                  </div>
                </div>

                {/* Tags Section */}
                <div className="p-4 sm:p-5 rounded-2xl bg-slate-50/70 border border-slate-200/80 space-y-3">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1">
                      <Tag className="w-3.5 h-3.5 text-purple-600" />
                      Tags
                    </label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('tags', 'Tags', (val) => {
                        if (!editTags.includes(val)) setEditTags([...editTags, val]);
                      })}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Pick Previous Tag
                    </button>
                  </div>

                  <div className="flex gap-2">
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
                      placeholder="Type tag (e.g. #summer) & press Enter"
                      className="flex-1 px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                    />
                    <button
                      type="button"
                      onClick={handleAddTag}
                      className="px-3.5 py-2 rounded-xl bg-purple-600 text-white text-xs font-semibold hover:bg-purple-700 transition-colors"
                    >
                      Add
                    </button>
                  </div>

                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {editTags.map((tag, idx) => (
                      <span
                        key={idx}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-purple-50 text-purple-700 text-xs font-medium border border-purple-200"
                      >
                        <span>{tag}</span>
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="hover:text-rose-600 transition-colors"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                </div>

                {/* Pictures Management */}
                <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200 space-y-3">
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1">
                      <ImageIcon className="w-3.5 h-3.5 text-purple-600" />
                      Garment Photos ({editPictures.length})
                    </label>

                    <div className="flex items-center gap-3">
                      <button
                        type="button"
                        onClick={() => fileInputRef.current?.click()}
                        disabled={uploadingPic}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs disabled:opacity-60"
                      >
                        <Upload className="w-3.5 h-3.5" />
                        <span>{uploadingPic ? 'Processing...' : 'Upload Photo'}</span>
                      </button>
                    </div>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      multiple
                      className="hidden"
                      onChange={(e) => handlePhotoUpload(e, false)}
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
                      {editPictures.map((pic) => (
                        <div
                          key={pic.id}
                          className="group relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-100 aspect-square shadow-2xs"
                        >
                          {(() => {
                            const src = resolveImageSource(pic);
                            return src ? (
                              <img
                                src={src}
                                alt={pic.name || 'Garment photo'}
                                className="w-full h-full object-contain p-1"
                                referrerPolicy="no-referrer"
                              />
                            ) : (
                              <div className="w-full h-full flex flex-col items-center justify-center p-2 text-center bg-slate-50">
                                <Shirt className="w-6 h-6 text-slate-300 mb-1" />
                                <span className="text-[9px] text-amber-700 font-mono">Original Expired</span>
                              </div>
                            );
                          })()}
                          <button
                            type="button"
                            onClick={() => handleRemovePhoto(pic.id)}
                            className="absolute top-2 right-2 p-1.5 rounded-full bg-rose-600/90 text-white hover:bg-rose-700 transition-colors shadow-xs"
                            title="Remove photo"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div
                      onClick={() => fileInputRef.current?.click()}
                      className="p-6 border-2 border-dashed border-purple-200 hover:border-purple-400 rounded-2xl bg-purple-50/40 text-center cursor-pointer transition-colors"
                    >
                      <Camera className="w-8 h-8 text-purple-400 mx-auto mb-1.5" />
                      <p className="text-xs font-semibold text-purple-900">
                        No photos added yet
                      </p>
                      <p className="text-[11px] text-purple-600/80 mt-0.5">
                        Click here to upload garment pictures from camera or files
                      </p>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              /* ================= VIEW MODE ================= */
              <>
                {/* Main Title & Wash Status Action */}
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                  <div className="space-y-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 font-mono">
                      Garment Record · Name
                    </span>
                    <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                      {garment['Name']}
                    </h2>
                    <p className="text-xs text-slate-500">
                      <span className="font-semibold text-slate-700">{garment['material']}</span>
                      {garment['thick clothes'] && <span> · {garment['thick clothes']}</span>}
                      {garment['long dress'] && <span> · {garment['long dress']}</span>}
                    </p>
                  </div>

                  {/* Wash State Action */}
                  <div className="flex items-center gap-2 shrink-0 flex-wrap">
                    {stats.needWash ? (
                      <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1 bg-white px-2 py-1 rounded-xl border border-amber-300 shadow-2xs">
                          <span className="text-[11px] font-mono text-slate-500">Date:</span>
                          <input
                            type="date"
                            value={selectedWashDate}
                            onChange={(e) => setSelectedWashDate(e.target.value)}
                            className="text-xs font-mono text-slate-800 bg-transparent outline-hidden"
                            title="Wash date"
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => setShowWashConfirm(true)}
                          className="inline-flex items-center gap-2 px-4 py-2 rounded-2xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold transition-all shadow-xs active:scale-95 cursor-pointer"
                        >
                          <AlertCircle className="w-4 h-4" />
                          <span>Wash Now ({stats.unwashedHours}h)</span>
                        </button>
                      </div>
                    ) : (
                      <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-2xl bg-emerald-50 text-emerald-800 border border-emerald-200 text-xs font-semibold">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        <span>Clean</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Photos Gallery in View Mode (No upload button in view mode; click photo to zoom) */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5 font-mono">
                      <ImageIcon className="w-3.5 h-3.5 text-purple-600" />
                      Garment Photos ({pictures.length})
                    </span>
                    {pictures.length > 0 && (
                      <span className="text-[11px] text-purple-600 font-medium">
                        Click photo to view fullscreen
                      </span>
                    )}
                  </div>

                  {pictures.length > 0 ? (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      {pictures.map((p, idx) => {
                        const src = resolveImageSource(p);
                        return (
                          <div
                            key={p.id || idx}
                            onClick={() => {
                              setViewerInitialIndex(idx);
                              setViewerOpen(true);
                            }}
                            className="group relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-100 aspect-square shadow-2xs cursor-pointer hover:border-purple-400 transition-all"
                            title="Click to view full photo"
                          >
                            {src ? (
                              <img
                                src={src}
                                alt={p.name || `Photo ${idx + 1}`}
                                className="w-full h-full object-contain p-1 group-hover:scale-105 transition-transform duration-300"
                                referrerPolicy="no-referrer"
                                onError={(e) => {
                                  const target = e.target as HTMLImageElement;
                                  target.onerror = null;
                                  target.style.opacity = '0.4';
                                }}
                              />
                            ) : (
                              <div className="w-full h-full flex flex-col items-center justify-center p-2 text-center bg-slate-50">
                                <Shirt className="w-7 h-7 text-slate-300 mb-1" />
                                <span className="text-[10px] text-amber-700 font-mono">Original Expired</span>
                              </div>
                            )}
                            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center pointer-events-none">
                              <span className="p-2 rounded-full bg-black/60 text-white opacity-0 group-hover:opacity-100 transition-opacity shadow-sm">
                                <Maximize2 className="w-4 h-4" />
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div className="p-6 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col items-center justify-center text-center">
                      <ImageIcon className="w-8 h-8 text-slate-300 mb-1.5" />
                      <p className="text-xs font-semibold text-slate-600">
                        No photos recorded for this garment
                      </p>
                      <p className="text-[11px] text-slate-400 mt-0.5">
                        Click &quot;Edit Garment&quot; above to upload pictures
                      </p>
                    </div>
                  )}
                </div>

                {/* Usage & Analytics Metrics */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 font-mono block mb-1">
                      Total Wears
                    </span>
                    <span className="text-xl font-bold text-slate-900">
                      {stats.totalWears} times
                    </span>
                  </div>

                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 font-mono block mb-1">
                      Total Duration
                    </span>
                    <span className="text-xl font-bold text-slate-900">
                      {stats.totalDurationHours} hrs
                    </span>
                  </div>

                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 font-mono block mb-1">
                      Washes Count
                    </span>
                    <span className="text-xl font-bold text-slate-900">
                      {garment['Total Washed Count'] ?? 0}
                    </span>
                  </div>

                  <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 font-mono block mb-1">
                      Cost Per Wear
                    </span>
                    <span className="text-xl font-bold text-purple-700">
                      {costPerWear ? `$${costPerWear}` : 'N/A'}
                    </span>
                  </div>
                </div>

                {/* Complete Database Properties Inspection */}
                <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-3">
                  <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider font-mono">
                    Garment Attributes & Ergonomics
                  </h4>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-xs">
                    {renderViewAttr('Clothes Type', 'Clothes Type', garment['Clothes Type'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('material', 'material', garment['material'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('thick clothes', 'thick clothes', garment['thick clothes'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('Hot at noon', 'Hot at noon', garment['Hot at noon'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('diao dai', 'diao dai', garment['diao dai'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr(
                      'side breast ok: with 聚拢 bra',
                      'side breast ok: with 聚拢 bra',
                      garment['side breast ok: with 聚拢 bra'] || garment['side breast ok: With 聚拢bra'] || garment.sideBreastOkWithGatherBra || (garment['side breast ok'] ? `${garment['side breast ok']} (legacy)` : <span className="text-slate-400 font-normal italic">No data</span>)
                    )}
                    {renderViewAttr(
                      'side breast ok: with side out bra',
                      'side breast ok: with side out bra',
                      garment['side breast ok: with side out bra'] || garment['side breast ok: With side out bra'] || garment.sideBreastOkWithSideOutBra || <span className="text-slate-400 font-normal italic">No data</span>
                    )}
                    {renderViewAttr(
                      'sit breast ok: With 聚拢bra',
                      'sit breast ok: With 聚拢bra',
                      garment['sit breast ok: With 聚拢bra'] || garment['sit breast ok: with 聚拢bra'] || garment.sitBreastOkWithGatherBra || (garment['sit breast ok'] ? `${garment['sit breast ok']} (legacy)` : <span className="text-slate-400 font-normal italic">No data</span>)
                    )}
                    {renderViewAttr(
                      'sit breast ok: with side out bra',
                      'sit breast ok: with side out bra',
                      garment['sit breast ok: with side out bra'] || garment['sit breast ok: With side out bra'] || garment.sitBreastOkWithSideOutBra || <span className="text-slate-400 font-normal italic">No data</span>
                    )}
                    {renderViewAttr('center chest', 'center chest', garment['center chest'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('beautiful', 'beautiful', garment['beautiful'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('wet', 'wet', garment['wet'] || <span className="text-slate-400 font-normal italic">dry</span>)}
                    {renderViewAttr('long dress', 'long dress', garment['long dress'] || <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('may throw', 'may throw', garment['may throw'] || <span className="text-slate-400 font-normal italic">no</span>)}
                    {renderViewAttr('price', 'price', typeof garment['price'] === 'number' ? `$${garment['price']}` : <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('wearing frequency(how many times legacy washed)', 'wearing frequency(legacy)', garment['wearing frequency(how many times legacy washed)'] !== undefined ? garment['wearing frequency(how many times legacy washed)'] : <span className="text-slate-400 font-normal italic">0</span>)}
                    {renderViewAttr('Total Washed Count', 'Total Washed Count', garment['Total Washed Count'] ?? 0)}
                    {renderViewAttr('Tracked App Washes', 'Tracked App Washes', garment['Tracked App Washes'] ?? 0)}
                    {renderViewAttr('Last Washed', 'Last Washed', garment['Last Washed'] ? formatCalendarDate(garment['Last Washed']) : <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('unwash time rollup', 'unwash time rollup', garment['unwash time rollup'] !== undefined ? `${garment['unwash time rollup']}s` : <span className="text-slate-400 font-normal italic">No data</span>)}
                    {renderViewAttr('created', 'created', garment['created'] ? new Date(garment['created']).toLocaleDateString() : <span className="text-slate-400 font-normal italic">No data</span>, true)}

                    {/* Dynamic Custom Attributes entries in View Grid */}
                    {Object.entries({
                      ...(garment.customFields || {}),
                      ...(garment.extraAttributes || {}),
                      ...((dataService.getGarment(garment.id)?.customFields) || {}),
                      ...((dataService.getGarment(garment.id)?.extraAttributes) || {})
                    })
                      .filter(([k]) => !['id', 'fields', 'createdTime', 'customFields', 'extraAttributes'].includes(k) && !k.startsWith('_') && !k.endsWith('_explain'))
                      .map(([k, v]) => (
                        <React.Fragment key={k}>
                          {renderViewAttr(
                            k,
                            `🏷️ ${k}`,
                            <span className="text-purple-950 font-bold bg-purple-50 px-2 py-0.5 rounded-lg border border-purple-200 break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap select-text">
                              {typeof v === 'object' ? JSON.stringify(v) : String(v)}
                            </span>,
                            String(v).length > 25
                          )}
                        </React.Fragment>
                      ))}
                  </div>

                  <div className="pt-2 text-xs border-t border-slate-200/60">
                    <span className="font-mono text-slate-500 block font-bold">wet reason:</span>
                    <p className={`mt-0.5 break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap ${garment['wet reason'] ? 'text-slate-800 font-medium' : 'text-slate-400 italic'}`}>
                      {garment['wet reason'] || 'No data'}
                    </p>
                    {getFieldExplainText('wet reason') && (
                      <div className="mt-1.5 flex items-start gap-2 p-2 rounded-xl bg-purple-50/90 border border-purple-200/70 text-purple-950 text-xs leading-relaxed break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">
                        <MessageSquare className="w-3.5 h-3.5 text-purple-600 shrink-0 mt-0.5" />
                        <div className="flex-1 min-w-0">
                          <span className="font-bold text-[10px] text-purple-700 block uppercase font-mono mb-0.5">wet reason 专属说明:</span>
                          <span className="font-medium select-text break-words [word-break:normal] [overflow-wrap:break-word]">{getFieldExplainText('wet reason')}</span>
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="pt-2 text-xs border-t border-slate-200/60">
                    <span className="font-mono text-slate-500 block font-bold">beatiful reason:</span>
                    <p className={`mt-0.5 break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap ${garment['beatiful reason'] ? 'text-slate-800 font-medium' : 'text-slate-400 italic'}`}>
                      {garment['beatiful reason'] || 'No data'}
                    </p>
                    {getFieldExplainText('beatiful reason') && (
                      <div className="mt-1.5 flex items-start gap-2 p-2 rounded-xl bg-purple-50/90 border border-purple-200/70 text-purple-950 text-xs leading-relaxed break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">
                        <MessageSquare className="w-3.5 h-3.5 text-purple-600 shrink-0 mt-0.5" />
                        <div className="flex-1 min-w-0">
                          <span className="font-bold text-[10px] text-purple-700 block uppercase font-mono mb-0.5">beatiful reason 专属说明:</span>
                          <span className="font-medium select-text break-words [word-break:normal] [overflow-wrap:break-word]">{getFieldExplainText('beatiful reason')}</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                {/* 🌡️ Multi-Scenario Climate & Sensory Ergonomics View Card */}
                <div className="p-4 rounded-2xl bg-indigo-50/50 border border-indigo-200/80 space-y-3">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-bold text-indigo-950 uppercase tracking-wider font-mono flex items-center gap-1.5">
                      <ThermometerSun className="w-3.5 h-3.5 text-indigo-600" />
                      多场景气候温感与腿部体测 (Climate Scenarios)
                    </h4>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
                    {/* Scenario 1: when with aircond */}
                    <div className="p-3 rounded-xl bg-white border border-blue-200/80 space-y-2">
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-blue-100 text-blue-800 font-mono inline-block">
                        ❄️ when with aircond:
                      </span>
                      <div className="space-y-1.5">
                        <div>
                          <span className="text-[11px] font-mono text-slate-500 font-bold block">&gt; hot feeling:</span>
                          <span className="font-semibold text-slate-800">
                            {garment['when with aircond: hot feeling'] || <span className="text-slate-400 font-normal italic">No data</span>}
                          </span>
                          {getFieldExplainText('when with aircond: hot feeling') && (
                            <p className="mt-0.5 text-[10px] text-purple-700 bg-purple-50 p-1.5 rounded-md border border-purple-100 break-words">
                              💡 {getFieldExplainText('when with aircond: hot feeling')}
                            </p>
                          )}
                        </div>
                        <div>
                          <span className="text-[11px] font-mono text-slate-500 font-bold block">&gt; leg feeling:</span>
                          <span className="font-semibold text-slate-800">
                            {garment['when with aircond: leg feeling'] || <span className="text-slate-400 font-normal italic">No data</span>}
                          </span>
                          {getFieldExplainText('when with aircond: leg feeling') && (
                            <p className="mt-0.5 text-[10px] text-purple-700 bg-purple-50 p-1.5 rounded-md border border-purple-100 break-words">
                              💡 {getFieldExplainText('when with aircond: leg feeling')}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Scenario 2: when without aircond */}
                    <div className="p-3 rounded-xl bg-white border border-amber-200/80 space-y-2">
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-amber-100 text-amber-900 font-mono inline-block">
                        ☀️ when without aircond:
                      </span>
                      <div className="space-y-1.5">
                        <div>
                          <span className="text-[11px] font-mono text-slate-500 font-bold block">&gt; hot feeling:</span>
                          <span className="font-semibold text-slate-800">
                            {garment['when without aircond: hot feeling'] || <span className="text-slate-400 font-normal italic">No data</span>}
                          </span>
                          {getFieldExplainText('when without aircond: hot feeling') && (
                            <p className="mt-0.5 text-[10px] text-purple-700 bg-purple-50 p-1.5 rounded-md border border-purple-100 break-words">
                              💡 {getFieldExplainText('when without aircond: hot feeling')}
                            </p>
                          )}
                        </div>
                        <div>
                          <span className="text-[11px] font-mono text-slate-500 font-bold block">&gt; leg feeling:</span>
                          <span className="font-semibold text-slate-800">
                            {garment['when without aircond: leg feeling'] || <span className="text-slate-400 font-normal italic">No data</span>}
                          </span>
                          {getFieldExplainText('when without aircond: leg feeling') && (
                            <p className="mt-0.5 text-[10px] text-purple-700 bg-purple-50 p-1.5 rounded-md border border-purple-100 break-words">
                              💡 {getFieldExplainText('when without aircond: leg feeling')}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Scenario 3: when sleep under fan */}
                    <div className="p-3 rounded-xl bg-white border border-teal-200/80 space-y-2">
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-teal-100 text-teal-900 font-mono inline-block">
                        🌀 when sleep under fan:
                      </span>
                      <div className="space-y-1.5">
                        <div>
                          <span className="text-[11px] font-mono text-slate-500 font-bold block">&gt; hot feeling:</span>
                          <span className="font-semibold text-slate-800">
                            {garment['when sleep under fan: hot feeling'] || <span className="text-slate-400 font-normal italic">No data</span>}
                          </span>
                          {getFieldExplainText('when sleep under fan: hot feeling') && (
                            <p className="mt-0.5 text-[10px] text-purple-700 bg-purple-50 p-1.5 rounded-md border border-purple-100 break-words">
                              💡 {getFieldExplainText('when sleep under fan: hot feeling')}
                            </p>
                          )}
                        </div>
                        <div>
                          <span className="text-[11px] font-mono text-slate-500 font-bold block">&gt; leg feeling:</span>
                          <span className="font-semibold text-slate-800">
                            {garment['when sleep under fan: leg feeling'] || <span className="text-slate-400 font-normal italic">No data</span>}
                          </span>
                          {getFieldExplainText('when sleep under fan: leg feeling') && (
                            <p className="mt-0.5 text-[10px] text-purple-700 bg-purple-50 p-1.5 rounded-md border border-purple-100 break-words">
                              💡 {getFieldExplainText('when sleep under fan: leg feeling')}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Descriptions & Observations */}
                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider font-mono">
                    Descriptions & Observations
                  </h4>
                  <div className="p-3.5 rounded-2xl bg-white border border-slate-200 text-xs leading-relaxed">
                    <span className="font-mono font-bold text-slate-400 block mb-1">Descriptions:</span>
                    {garment['Descriptions'] ? (
                      <p className="text-slate-700 font-medium break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">{garment['Descriptions']}</p>
                    ) : (
                      <p className="text-slate-400 italic">No data</p>
                    )}
                    {getFieldExplainText('Descriptions') && (
                      <div className="mt-2 flex items-start gap-2 p-2 rounded-xl bg-purple-50/90 border border-purple-200/70 text-purple-950 text-xs leading-relaxed break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">
                        <MessageSquare className="w-3.5 h-3.5 text-purple-600 shrink-0 mt-0.5" />
                        <div className="flex-1 min-w-0">
                          <span className="font-bold text-[10px] text-purple-700 block uppercase font-mono mb-0.5">Descriptions 专属说明:</span>
                          <span className="font-medium select-text break-words [word-break:normal] [overflow-wrap:break-word]">{getFieldExplainText('Descriptions')}</span>
                        </div>
                      </div>
                    )}
                  </div>
                  <div className="p-3.5 rounded-2xl bg-white border border-slate-200 text-xs leading-relaxed">
                    <span className="font-mono font-bold text-slate-400 block mb-1">Descriptions 2:</span>
                    {garment['Descriptions 2'] ? (
                      <p className="text-slate-700 font-medium break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">{garment['Descriptions 2']}</p>
                    ) : (
                      <p className="text-slate-400 italic">No data</p>
                    )}
                    {getFieldExplainText('Descriptions 2') && (
                      <div className="mt-2 flex items-start gap-2 p-2 rounded-xl bg-purple-50/90 border border-purple-200/70 text-purple-950 text-xs leading-relaxed break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">
                        <MessageSquare className="w-3.5 h-3.5 text-purple-600 shrink-0 mt-0.5" />
                        <div className="flex-1 min-w-0">
                          <span className="font-bold text-[10px] text-purple-700 block uppercase font-mono mb-0.5">Descriptions 2 专属说明:</span>
                          <span className="font-medium select-text break-words [word-break:normal] [overflow-wrap:break-word]">{getFieldExplainText('Descriptions 2')}</span>
                        </div>
                      </div>
                    )}
                  </div>
                  <div className="p-3.5 rounded-2xl bg-white border border-slate-200 text-xs leading-relaxed">
                    <span className="font-mono font-bold text-slate-400 block mb-1">last comment or action:</span>
                    {garment['last comment or action'] ? (
                      <p className="text-slate-700 font-medium break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">{garment['last comment or action']}</p>
                    ) : (
                      <p className="text-slate-400 italic">No data</p>
                    )}
                    {getFieldExplainText('last comment or action') && (
                      <div className="mt-2 flex items-start gap-2 p-2 rounded-xl bg-purple-50/90 border border-purple-200/70 text-purple-950 text-xs leading-relaxed break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap">
                        <MessageSquare className="w-3.5 h-3.5 text-purple-600 shrink-0 mt-0.5" />
                        <div className="flex-1 min-w-0">
                          <span className="font-bold text-[10px] text-purple-700 block uppercase font-mono mb-0.5">last comment or action 专属说明:</span>
                          <span className="font-medium select-text break-words [word-break:normal] [overflow-wrap:break-word]">{getFieldExplainText('last comment or action')}</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                {/* Document Content Blocks (Rich Text / Tables / Pasted Notes) */}
                {garment.contentBlocks && garment.contentBlocks.length > 0 && (
                  <div className="space-y-3 p-4 rounded-2xl border border-slate-200 bg-slate-50/40">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5 font-mono">
                        <Database className="w-3.5 h-3.5 text-slate-600" />
                        Document Content Blocks ({garment.contentBlocks.length})
                      </span>
                    </div>

                    <div className="space-y-2">
                      {garment.contentBlocks.map((block) => (
                        <div key={block.id} className="p-3 rounded-xl border border-slate-200 bg-white text-xs space-y-1">
                          <div className="flex items-center gap-2">
                            <span className="px-1.5 py-0.5 rounded text-[9px] font-mono font-bold uppercase border border-slate-200 text-slate-600">
                              {block.type}
                            </span>
                            <span className="font-bold text-slate-800">{block.title}</span>
                          </div>
                          {block.type === 'table' && block.tableData ? (
                            <div className="overflow-x-auto border border-slate-200 rounded-lg mt-1">
                              <table className="w-full text-left text-xs border-collapse">
                                <thead>
                                  <tr className="bg-slate-50 border-b border-slate-200 font-bold text-slate-700">
                                    {block.tableData.headers.map((h, i) => (
                                      <th key={i} className="py-1 px-2 font-mono text-[10px]">{h}</th>
                                    ))}
                                  </tr>
                                </thead>
                                <tbody>
                                  {block.tableData.rows.map((r, ri) => (
                                    <tr key={ri} className="hover:bg-slate-50/50">
                                      {r.map((cell, ci) => (
                                        <td key={ci} className="py-1 px-2 font-mono text-[10px] text-slate-700">{cell}</td>
                                      ))}
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          ) : (
                            <p className="text-slate-700 whitespace-pre-wrap mt-1">{block.content}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Tags */}
                <div className="space-y-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5 font-mono">
                    <Tag className="w-3.5 h-3.5 text-slate-500" />
                    tags:
                  </span>
                  {garment['tags'] && garment['tags'].length > 0 ? (
                    <div className="flex flex-wrap gap-1.5">
                      {garment['tags'].map((tag, idx) => (
                        <span key={idx} className="px-2.5 py-1 rounded-lg bg-transparent text-slate-700 text-xs font-medium border border-slate-300">
                          {tag}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 italic">No data</p>
                  )}
                </div>

                {/* Dynamic Custom Attributes & Specifications (Color, Fabric Weight, Stretch, Store, etc.) */}
                <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-2xs">
                  <CustomAttributesManager
                    title="Garment Dynamic Custom Attributes"
                    description="Define new fields (e.g. Color, Fabric Weight, Stretch Level, Purchase Store) dynamically without schema migrations."
                    attributes={garment.customFields || garment.extraAttributes || {}}
                    onAddAttribute={handleAddGarmentCustomField}
                    onUpdateAttribute={handleUpdateGarmentCustomField}
                    onDeleteAttribute={handleDeleteGarmentCustomField}
                    suggestedKeys={['Color', 'Fabric Weight (gsm)', 'Stretch Level', 'Purchase Store', 'Season', 'Care Instructions', 'Fit Category']}
                  />
                </div>

                {/* AI Clothes Analysis Card (Teable-Style Multi-Version Garment Diagnostic) - Hidden when AI generation is paused */}
                {!isAiPaused && (
                <div className="p-5 rounded-3xl bg-linear-to-br from-purple-50/70 via-white to-indigo-50/40 border border-purple-200/80 shadow-xs space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <div className="p-2 rounded-xl bg-purple-100 text-purple-700">
                        <Sparkles className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="text-xs font-bold text-slate-900 uppercase font-mono tracking-wider">
                            AI 衣物人体工学与体感分析报告
                          </h4>
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-100 text-purple-700 border border-purple-200 font-mono">
                            {garmentAiReports.length > 0 ? `${garmentAiReports.length} 份版本存档` : '多版本独立存档'}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500">
                          基于材质、厚薄度、中午炎热耐受(Hot at noon)、肩带(diao dai)、久坐胸部(sit breast ok)等多维字段的专项诊断。支持多版本生成、查看、编辑与删除。
                        </p>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={handleRunAiAnalysis}
                      disabled={analyzing}
                      className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition-all shadow-xs disabled:opacity-60 active:scale-95 cursor-pointer shrink-0"
                    >
                      <RefreshCw className={`w-3.5 h-3.5 ${analyzing ? 'animate-spin' : ''}`} />
                      <span>{analyzing ? '正在分析生成中...' : (garmentAiReports.length > 0 ? '+ 生成新版本诊断报告' : '生成 AI 诊断报告')}</span>
                    </button>
                  </div>

                  {errorMsg && (
                    <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs space-y-2">
                      <div className="flex items-center gap-2">
                        <AlertCircle className="w-4 h-4 shrink-0 text-rose-500" />
                        <span className="font-medium">{errorMsg}</span>
                      </div>
                      {(errorMsg.includes('prepayment') || errorMsg.includes('402')) && (
                        <div className="pt-1 text-[11px] text-slate-700 flex flex-wrap items-center gap-2">
                          <span className="font-bold text-slate-900">提示:</span>
                          <a href="https://ai.studio/projects" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                            在 AI Studio 管理额度
                          </a>
                          <span>或</span>
                          <a href="https://aistudio.google.com/apikey" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                            获取并保存个人专属 API Key (永久保存免重复输入)
                          </a>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Version Pills / Tab Bar if multiple reports exist */}
                  {garmentAiReports.length > 0 && (
                    <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-1 border-b border-purple-100/80">
                      <span className="text-[11px] font-bold text-slate-400 shrink-0 mr-1 font-mono uppercase">
                        版本列表 ({garmentAiReports.length}):
                      </span>
                      {garmentAiReports.map((v, idx) => {
                        const isActive = activeVersion?.id === v.id;
                        return (
                          <button
                            key={v.id}
                            type="button"
                            onClick={() => {
                              setActiveVersionId(v.id);
                              setIsEditingAiReport(false);
                            }}
                            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                              isActive
                                ? 'bg-purple-600 text-white shadow-xs font-bold'
                                : 'bg-white/90 text-slate-600 hover:bg-purple-100/70 hover:text-purple-900 border border-purple-200/70'
                            }`}
                          >
                            <span>{v.title || `Report #${idx + 1}`}</span>
                            {v.isPrimary && (
                              <Star className={`w-2.5 h-2.5 ${isActive ? 'fill-amber-300 text-amber-300' : 'fill-amber-400 text-amber-500'}`} />
                            )}
                          </button>
                        );
                      })}
                    </div>
                  )}

                  {/* Active Version Display & Management Card */}
                  {activeVersion ? (
                    <div className="rounded-2xl bg-white border border-purple-100/90 shadow-2xs overflow-hidden">
                      {/* Version Toolbar */}
                      <div className="p-3 sm:p-4 bg-slate-50/70 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-xs font-bold text-slate-800">
                            {activeVersion.title || 'Report Version'}
                          </span>
                          <span className="text-[11px] font-mono text-slate-400">
                            {new Date(activeVersion.timestamp).toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                          </span>
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-medium bg-slate-100 text-slate-600 border border-slate-200">
                            {activeVersion.model || 'gemini-3.6-flash'}
                          </span>

                          {activeVersion.isPrimary ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 font-mono">
                              <Star className="w-2.5 h-2.5 fill-amber-500 text-amber-500" />
                              默认主报告
                            </span>
                          ) : (
                            <button
                              type="button"
                              onClick={() => handleSetPrimaryVersion(activeVersion.id)}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-white text-slate-600 hover:text-amber-600 hover:border-amber-200 border border-slate-200 transition-colors cursor-pointer"
                              title="设为该单品的默认主摘要报告"
                            >
                              <Star className="w-2.5 h-2.5" />
                              设为主报告
                            </button>
                          )}
                        </div>

                        {/* Toolbar Buttons: Edit, Copy, Delete */}
                        <div className="flex items-center gap-1.5 self-end sm:self-auto">
                          {!isEditingAiReport ? (
                            <>
                              <button
                                type="button"
                                onClick={() => handleStartEditingAiReport(activeVersion)}
                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-200/60 transition-colors cursor-pointer"
                                title="编辑此报告版本标题和内容"
                              >
                                <Edit3 className="w-3.5 h-3.5" />
                                <span>编辑</span>
                              </button>

                              <button
                                type="button"
                                onClick={() => handleCopyAiReport(activeVersion.content, activeVersion.id)}
                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-200/60 transition-colors cursor-pointer"
                                title="复制 Markdown 内容"
                              >
                                {copiedVersionId === activeVersion.id ? (
                                  <>
                                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                                    <span className="text-emerald-600 font-bold">已复制</span>
                                  </>
                                ) : (
                                  <>
                                    <Copy className="w-3.5 h-3.5" />
                                    <span>复制</span>
                                  </>
                                )}
                              </button>

                              <button
                                type="button"
                                onClick={() => {
                                  if (confirm(`确定要删除报告版本「${activeVersion.title || '此版本'}」吗？删除后不可撤销。`)) {
                                    handleDeleteAiReportVersion(activeVersion.id);
                                  }
                                }}
                                className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                                title="删除此报告版本"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </>
                          ) : (
                            <div className="flex items-center gap-1.5">
                              <button
                                type="button"
                                onClick={() => handleSaveEditingAiReport(activeVersion.id)}
                                className="inline-flex items-center gap-1 px-3 py-1 rounded-lg text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white transition-colors cursor-pointer shadow-2xs"
                              >
                                <Save className="w-3.5 h-3.5" />
                                <span>保存</span>
                              </button>
                              <button
                                type="button"
                                onClick={handleCancelEditingAiReport}
                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-500 hover:bg-slate-200 transition-colors cursor-pointer"
                              >
                                <X className="w-3.5 h-3.5" />
                                <span>取消</span>
                              </button>
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Version Content: View or Edit Mode */}
                      <div className="p-4 sm:p-5">
                        {isEditingAiReport ? (
                          <div className="space-y-3">
                            <div>
                              <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                                报告标题:
                              </label>
                              <input
                                type="text"
                                value={editingReportTitle}
                                onChange={(e) => setEditingReportTitle(e.target.value)}
                                className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs font-bold text-slate-900 focus:outline-hidden focus:ring-2 focus:ring-purple-500"
                                placeholder="如：盛夏中午炎热度与细肩带诊断报告"
                              />
                            </div>
                            <div>
                              <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                                报告正文 (Markdown):
                              </label>
                              <textarea
                                value={editingReportContent}
                                onChange={(e) => setEditingReportContent(e.target.value)}
                                rows={14}
                                className="w-full p-3.5 rounded-xl border border-slate-300 text-xs font-mono text-slate-800 leading-relaxed focus:outline-hidden focus:ring-2 focus:ring-purple-500 resize-y"
                                placeholder="输入或修改诊断报告内容..."
                              />
                            </div>
                          </div>
                        ) : (
                          <div className="text-xs sm:text-sm text-slate-800 leading-relaxed max-h-96 overflow-y-auto whitespace-pre-wrap font-sans">
                            {activeVersion.content}
                          </div>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="p-6 rounded-2xl bg-white/80 border border-dashed border-purple-200 text-center space-y-2">
                      <p className="text-xs text-slate-600 font-medium">
                        当前单品暂无 AI 诊断报告
                      </p>
                      <p className="text-[11px] text-slate-400 max-w-md mx-auto">
                        点击上方 <strong>“生成 AI 诊断报告”</strong> 按钮，系统将根据你所记录的衣物物理与工学字段（面料、厚度、Hot at noon、diao dai、sit breast 等）生成专属诊断报告，可随时生成多个版本、查看、编辑与删除。
                      </p>
                    </div>
                  )}
                </div>
                )}

                {/* Linked Wearing Sessions - CLICKABLE */}
                <div className="space-y-2.5">
                  <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center justify-between font-mono">
                    <span>Wearing History ({wornReports.length} sessions)</span>
                    <span className="text-[11px] font-normal text-purple-600 lowercase font-sans">
                      click session to view full report
                    </span>
                  </h4>

                  {wornReports.length > 0 ? (
                    <div className="space-y-2">
                      {wornReports.map(({ junction, report }, idx) => {
                        const hours = ((junction['duration'] || 0) / 3600).toFixed(1);
                        return (
                          <div
                            key={idx}
                            onClick={() => {
                              if (report && onSelectReport) {
                                onSelectReport(report);
                              }
                            }}
                            className={`group flex items-center justify-between p-3.5 rounded-2xl bg-slate-50 hover:bg-purple-50/70 border border-slate-200/80 hover:border-purple-300 transition-all text-xs active:scale-[0.99] ${
                              report && onSelectReport ? 'cursor-pointer' : ''
                            }`}
                          >
                            <div className="space-y-1 min-w-0 pr-2">
                              <div className="flex items-center gap-1.5 font-bold text-slate-900 group-hover:text-purple-900 transition-colors truncate">
                                <Calendar className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                                <span>
                                  {report?.['wear date']
                                    ? new Date(report['wear date']).toLocaleDateString()
                                    : 'Wear Session'}
                                </span>
                              </div>
                              <p className="text-[11px] text-slate-500 group-hover:text-purple-700/80 truncate">
                                {report?.['Name'] || (typeof junction['wearing report id'] === 'object' ? junction['wearing report id'].title : '')}
                              </p>
                            </div>

                            <div className="flex items-center gap-2 shrink-0">
                              <span className="text-slate-500 font-medium">{hours} hrs</span>
                              <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                                junction['wash stated'] === 'need wash' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                              }`}>
                                {junction['wash stated'] === 'wash' ? 'Clean' : junction['wash stated']}
                              </span>
                              {(junction['wash date'] || junction.washDate || junction['washed date'] || junction.washedDate) && (
                                <span className="text-[10px] font-mono font-bold text-purple-800 bg-purple-100/90 px-1.5 py-0.5 rounded border border-purple-200">
                                  wash date: {toDateKey(junction['wash date'] || junction.washDate || junction['washed date'] || junction.washedDate) || String(junction['wash date'] || junction['washed date'])}
                                </span>
                              )}
                              <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-purple-600 transition-colors" />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 italic">No wearing reports logged for this item yet.</p>
                  )}
                </div>
              </>
            )}
          </div>

          {/* Modal Footer */}
          <div className="px-4 sm:px-6 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] border-t border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between shrink-0 w-full max-w-full shadow-xs">
            <span className="text-xs text-slate-400 font-mono truncate max-w-[140px] sm:max-w-none">
              ID: {garment.id}
            </span>

            <div className="flex items-center gap-2">
              {isEditing ? (
                <>
                  <button
                    type="button"
                    onClick={cancelEdit}
                    className="px-3.5 sm:px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 border border-slate-200 transition-colors cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleSaveEdit}
                    className="px-4 sm:px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs cursor-pointer"
                  >
                    Save Changes
                  </button>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    onClick={() => setIsEditing(true)}
                    className="px-3.5 sm:px-4 py-2 rounded-xl border border-slate-300 hover:border-slate-400 text-slate-700 text-xs font-semibold transition-colors cursor-pointer"
                  >
                    Edit Garment
                  </button>
                  <button
                    onClick={onClose}
                    className="px-4 sm:px-5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-all shadow-xs cursor-pointer"
                  >
                    Close
                  </button>
                </>
              )}
            </div>
          </div>
        </div>

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

      {/* Quick Create Option Modal with Meaning CN */}
      {createOptionConfig && (
        <CreateOptionModal
          isOpen={createOptionConfig.isOpen}
          fieldName={createOptionConfig.fieldName}
          fieldLabel={createOptionConfig.fieldLabel}
          onSave={(val) => {
            createOptionConfig.onSave(val);
          }}
          onClose={() => setCreateOptionConfig(null)}
        />
      )}

      {/* Fullscreen Photo Viewer Modal */}
      <ImageViewerModal
        images={pictures}
        initialIndex={viewerInitialIndex}
        isOpen={viewerOpen}
        onClose={() => setViewerOpen(false)}
      />

      {/* Full-Screen Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div
          className="fixed inset-0 z-[70] w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto"
        >
          {/* Header */}
          <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between shrink-0 w-full max-w-full overflow-x-hidden">
            <div className="flex items-center gap-2.5">
              <button
                type="button"
                onClick={() => !isDeleting && setShowDeleteConfirm(false)}
                disabled={isDeleting}
                className="p-2 -ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer"
                title="Back"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <h2 className="text-sm sm:text-base font-bold text-slate-900">
                Delete Garment
              </h2>
            </div>
            <button
              type="button"
              onClick={() => !isDeleting && setShowDeleteConfirm(false)}
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
                  Delete Garment?
                </h3>
                <p className="text-xs text-slate-500 font-mono">
                  This action is permanent and cannot be undone.
                </p>
              </div>

              <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-2xs space-y-3 text-left">
                <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                  <span className="text-xs text-slate-500">Garment</span>
                  <span className="text-xs font-bold text-slate-900 font-mono truncate max-w-[200px]">{garment['Name']}</span>
                </div>
                <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2.5">
                  <span className="text-xs text-slate-500">Category</span>
                  <span className="text-xs font-semibold text-slate-800 font-mono">{garment['Clothes Type'] || '-'}</span>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs text-slate-500">Total Washes</span>
                  <span className="text-xs font-semibold text-slate-800 font-mono">{garment['Total Washed Count'] || 0} time(s)</span>
                </div>
                <p className="text-xs text-rose-600 font-medium pt-1">
                  This piece and its historical records will be permanently removed from your wardrobe.
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
                onClick={handleConfirmDelete}
                disabled={isDeleting}
                className="flex-1 py-3 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs sm:text-sm transition-all shadow-xs disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer active:scale-95"
              >
                {isDeleting && <Loader2 className="w-4 h-4 animate-spin" />}
                <span>{isDeleting ? 'Deleting...' : 'Delete Garment'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Wash Confirmation Modal */}
      {showWashConfirm && (
        <ConfirmModal
          isOpen={showWashConfirm}
          onClose={() => setShowWashConfirm(false)}
          onConfirm={() => {
            setShowWashConfirm(false);
            onMarkWashed(garment.id, selectedWashDate);
          }}
          title={`Wash "${garment['Name']}"?`}
          variant="primary"
          confirmLabel="Yes, Mark as Washed"
          message={
            <div className="space-y-2">
              <p>
                Are you sure you want to mark <strong>"{garment['Name']}"</strong> as washed?
              </p>
              <div className="p-2.5 rounded-xl bg-blue-50 border border-blue-100 text-xs text-blue-900 flex items-center gap-2">
                <Droplets className="w-4 h-4 text-blue-600 shrink-0" />
                <span>Wash date will be recorded as <strong>{selectedWashDate}</strong>.</span>
              </div>
              <p className="text-[11px] text-slate-500">
                This will reset its unwashed wear duration ({stats.unwashedHours}h) and increment its wash counter. You can undo if pressed by mistake.
              </p>
            </div>
          }
        />
      )}
    </>
  );
};

export const ClothesModal: React.FC<ClothesModalProps> = (props) => {
  const garment = props.clothes || props.garment;
  if (!garment) return null;
  return <ClothesModalContent {...props} garment={garment} />;
};

export const GarmentModal = ClothesModal;
