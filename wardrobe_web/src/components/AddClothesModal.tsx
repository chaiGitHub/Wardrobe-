import React, { useState, useRef, useMemo } from 'react';
import { Clothes, PictureAttachment } from '../types';
import { dataService, processImageFile } from '../services/dataService';
import { fieldDictionaryService } from '../services/fieldDictionaryService';
import { uploadGarmentPhotoToStorage, deleteFileFromStorage } from '../services/cloudStorageService';
import { PreviousEntriesModal, PreviousEntryItem } from './PreviousEntriesModal';
import { CreateOptionModal } from './CreateOptionModal';
import { FieldExplainBox } from './FieldExplainBox';
import { AutoResizeTextarea } from './AutoResizeTextarea';
import { resolveImageSource } from '../utils/imageUtils';
import {
  X, Plus, Sparkles, Shirt, DollarSign, ThermometerSun, ShieldCheck,
  Upload, Trash2, Camera, History, Tag, Image as ImageIcon, Loader2, ArrowLeft,
  MessageSquare, MessageSquarePlus, ChevronDown, ChevronUp, Droplets
} from 'lucide-react';

export interface AddClothesModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAddClothes?: (clothes: Omit<Clothes, 'id'>) => void;
  onAddGarment?: (garment: Omit<Clothes, 'id'>) => void;
}

export const AddClothesModal: React.FC<AddClothesModalProps> = ({
  isOpen,
  onClose,
  onAddClothes,
  onAddGarment
}) => {
  const [name, setName] = useState('');
  const [clothesType, setClothesType] = useState('Dress');
  const [material, setMaterial] = useState('soft chiffon');
  const [thickClothes, setThickClothes] = useState('01 not thick clothes');
  const [hotAtNoon, setHotAtNoon] = useState('Not hot');
  const [diaoDai, setDiaoDai] = useState('diao dai');
  const [sideBreastOkWithGatherBra, setSideBreastOkWithGatherBra] = useState('side breast shape ok');
  const [sideBreastOkWithSideOutBra, setSideBreastOkWithSideOutBra] = useState('side breast out');
  const [sitBreastOkWithGatherBra, setSitBreastOkWithGatherBra] = useState('sit breast shape ok');
  const [sitBreastOkWithSideOutBra, setSitBreastOkWithSideOutBra] = useState('ugly');
  const [sideBreastOk, setSideBreastOk] = useState('side breast shape ok');

  // Multi-scenario climate and sensory ergonomics states
  const [whenAircondHot, setWhenAircondHot] = useState('not so cold');
  const [whenAircondLeg, setWhenAircondLeg] = useState('not so cold');
  const [whenNoAircondHot, setWhenNoAircondHot] = useState('hot');
  const [whenNoAircondLeg, setWhenNoAircondLeg] = useState('middle hot');
  const [whenSleepFanHot, setWhenSleepFanHot] = useState('not hot');
  const [whenSleepFanLeg, setWhenSleepFanLeg] = useState('middle cold');
  const [centerChest, setCenterChest] = useState('no center chest');
  const [beautiful, setBeautiful] = useState('Beautiful');
  const [beatifulReason, setBeatifulReason] = useState('');
  const [wet, setWet] = useState<string>('dry');
  const [wetReason, setWetReason] = useState('');
  const [longDress, setLongDress] = useState('short dress');
  const [price, setPrice] = useState<string>('');
  const [descriptions, setDescriptions] = useState('');
  const [descriptions2, setDescriptions2] = useState('');
  const [lastComment, setLastComment] = useState('');
  const [tags, setTags] = useState<string[]>(['#custom']);
  const [newTagInput, setNewTagInput] = useState('');
  const [pictures, setPictures] = useState<PictureAttachment[]>([]);
  const [uploadingPic, setUploadingPic] = useState(false);
  const [uploadPercent, setUploadPercent] = useState<number | null>(null);

  const [dictVersion, setDictVersion] = useState(0);
  React.useEffect(() => {
    return fieldDictionaryService.subscribe(() => {
      setDictVersion(v => v + 1);
    });
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

    // Ensure current wet is in list
    if (wet && !optMap.has(wet.trim().toLowerCase())) {
      optMap.set(wet.trim().toLowerCase(), {
        value: wet.trim(),
        label: wet.trim()
      });
    }

    return Array.from(optMap.values());
  }, [wet, dictVersion]);

  // Field Explanations / Ergonomic Notes (default collapsed)
  const [fieldExplanations, setFieldExplanations] = useState<Record<string, string>>({});
  const [expandedExplainFields, setExpandedExplainFields] = useState<Record<string, boolean>>({});

  const renderFieldExplainBox = (
    fieldKey: string,
    fieldLabel: string,
    customPlaceholder?: string
  ) => {
    return (
      <FieldExplainBox
        fieldKey={fieldKey}
        fieldLabel={fieldLabel}
        value={fieldExplanations[fieldKey] || ''}
        onChange={(text) => {
          setFieldExplanations(prev => ({
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

  // Previous Values Picker Config
  const [previousModalConfig, setPreviousModalConfig] = useState<{
    isOpen: boolean;
    fieldName: string;
    fieldLabel: string;
    currentValue?: string;
    entries: PreviousEntryItem[];
    onSelect: (val: string) => void;
  } | null>(null);

  // Available clothes types dynamically extracted from local memory in 0ms
  const availableTypes = useMemo(() => {
    const defaultTypes = ['Dress', 'Pant', 'Tshirt', 'bra', 'skirt', 'sweater', 'jacket', 'coat', 'top', 'blouse', 'cardigan', 'shorts', 'Other'];
    const historical = dataService.getFieldPreviousValues('Clothes Type', 'clothes').map(h => h.value);
    const set = new Set([...defaultTypes, ...historical]);
    return Array.from(set);
  }, [isOpen]);

  const openPreviousPicker = (
    fieldKey: string,
    label: string,
    onSelectVal: (v: string) => void,
    currentVal?: string
  ) => {
    let historical = dataService.getFieldPreviousValues(fieldKey, 'all');
    const dictValues = fieldDictionaryService.getFieldTypicalValues(fieldKey);
    const resultMap = new Map<string, PreviousEntryItem>();

    // 1. Add typicalValues from AI dictionary
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
      const defaultTypes = ['Dress', 'Pant', 'Tshirt', 'bra', 'skirt', 'sweater', 'jacket', 'coat', 'top', 'blouse', 'cardigan', 'shorts', 'Other'];
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

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploadingPic(true);
    setUploadPercent(10);
    const tempClothesId = `new_clothes_${Date.now()}`;
    try {
      const newPics: PictureAttachment[] = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        try {
          // Large files (images) uploaded directly to Firebase Cloud Storage with real percent callback
          const storagePic = await uploadGarmentPhotoToStorage(tempClothesId, file, (pct) => {
            const basePct = Math.round((i / files.length) * 100);
            const fileContrib = Math.round((pct / files.length));
            setUploadPercent(Math.min(99, basePct + fileContrib));
          });
          newPics.push(storagePic);
        } catch (storageErr) {
          console.warn('Storage upload error, fallback to optimized local format:', storageErr);
          const processed = await processImageFile(file);
          newPics.push(processed);
        }
      }
      setUploadPercent(100);
      setPictures(prev => [...prev, ...newPics]);
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
    const target = pictures.find(p => p.id === picId);
    if (target?.storagePath) {
      deleteFileFromStorage(target.storagePath).catch(err => console.warn('Storage cleanup error:', err));
    }
    setPictures(prev => prev.filter(p => p.id !== picId));
  };

  const handleAddTag = () => {
    if (!newTagInput.trim()) return;
    const cleanTag = newTagInput.trim().startsWith('#') ? newTagInput.trim() : `#${newTagInput.trim()}`;
    if (!tags.includes(cleanTag)) {
      setTags([...tags, cleanTag]);
    }
    setNewTagInput('');
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(t => t !== tagToRemove));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      alert('Please enter a clothes name.');
      return;
    }

    const numPrice = price ? parseFloat(price) : undefined;

    const existingCustom: Record<string, any> = {};
    Object.entries(fieldExplanations).forEach(([k, v]) => {
      if (v && v.trim()) existingCustom[`${k}_explain`] = v.trim();
    });
    existingCustom._fieldExplanations = fieldExplanations;

    const payload: Omit<Clothes, 'id'> = {
      'Name': name.trim(),
      'Clothes Type': clothesType,
      'material': material.trim(),
      'thick clothes': thickClothes,
      'Hot at noon': hotAtNoon,
      'diao dai': diaoDai,
      'side breast ok': sideBreastOkWithGatherBra || sideBreastOkWithSideOutBra,
      'side breast ok: with 聚拢 bra': sideBreastOkWithGatherBra,
      'side breast ok: with side out bra': sideBreastOkWithSideOutBra,
      'sit breast ok': sitBreastOkWithGatherBra || sitBreastOkWithSideOutBra,
      'sit breast ok: With 聚拢bra': sitBreastOkWithGatherBra,
      'sit breast ok: with side out bra': sitBreastOkWithSideOutBra,
      // Multi-scenario climate ergonomics
      'when with aircond: hot feeling': whenAircondHot,
      'when with aircond: leg feeling': whenAircondLeg,
      'when without aircond: hot feeling': whenNoAircondHot,
      'when without aircond: leg feeling': whenNoAircondLeg,
      'when sleep under fan: hot feeling': whenSleepFanHot,
      'when sleep under fan: leg feeling': whenSleepFanLeg,
      'center chest': centerChest,
      'beautiful': beautiful as any,
      'beatiful reason': beatifulReason.trim(),
      'wet': wet,
      'wet reason': wetReason.trim(),
      'long dress': longDress,
      'price': isNaN(numPrice as number) ? undefined : numPrice,
      'Descriptions': descriptions.trim(),
      'Descriptions 2': descriptions2.trim(),
      'last comment or action': lastComment.trim(),
      'wearing frequency(how many times legacy washed)': 0,
      'Tracked App Washes': 0,
      'Total Washed Count': 0,
      'Pictures': pictures,
      'tags': tags,
      fieldExplanations,
      customFields: existingCustom,
      extraAttributes: existingCustom
    };

    if (onAddClothes) {
      onAddClothes(payload);
    } else if (onAddGarment) {
      onAddGarment(payload);
    }

    onClose();
  };

  if (!isOpen) return null;

  return (
    <>
      <div
        id="add-clothes-modal"
        className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-3 sm:px-6 py-2.5 sm:py-3.5 border-b border-slate-200/80 bg-white/95 backdrop-blur-md shrink-0 w-full max-w-full overflow-x-hidden">
          <div className="flex items-center gap-2 min-w-0">
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 sm:p-2 -ml-1 sm:-ml-2 rounded-xl text-slate-500 hover:text-slate-900 hover:bg-slate-200/60 transition-colors shrink-0"
              title="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <span className="p-1.5 rounded-xl bg-purple-100 text-purple-700 shrink-0">
              <Shirt className="w-4 h-4" />
            </span>
            <h3 className="text-sm sm:text-base font-bold text-slate-900 truncate">Add New Clothes</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors shrink-0"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Scrollable Body */}
        <form onSubmit={handleSubmit} className="p-3.5 sm:p-6 overflow-y-auto overflow-x-hidden flex-1 space-y-6 w-full max-w-full">
            {/* Core Details */}
            <div className="p-4 rounded-2xl bg-purple-50/40 border border-purple-100 space-y-4">
              <h4 className="text-xs font-bold text-purple-900 uppercase font-mono tracking-wider">
                Basic Clothes Information
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                    Name *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. 062 black knitted top"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20"
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
                      onClick={() => openPreviousPicker('Clothes Type', 'Clothes Type', setClothesType)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 transition-colors shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    required
                    list="add-clothes-type-datalist"
                    value={clothesType}
                    onChange={(e) => setClothesType(e.target.value)}
                    placeholder="e.g. Dress, Pant, Shoe, Tshirt, Bra..."
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                  />
                  <datalist id="add-clothes-type-datalist">
                    {availableTypes.map(t => (
                      <option key={t} value={t} />
                    ))}
                  </datalist>
                  {renderFieldExplainBox('Clothes Type', 'Clothes Type', '例如: dress normally more feminine. Got better waist and breast cutting.')}
                </div>

                {/* Material with Previous */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">material</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('material', 'Material', setMaterial)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={material}
                    onChange={(e) => setMaterial(e.target.value)}
                    placeholder="e.g. soft chiffon, silk"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
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
                    placeholder="e.g. 59.90"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                  {renderFieldExplainBox('price', 'Price', '价格、购入渠道或性价比体感备注...')}
                </div>
              </div>
            </div>

            {/* Clothes Attributes with Previous Buttons */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-4">
              <h4 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                Clothes Attributes & Ergonomics
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* thick clothes */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">thick clothes</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('thick clothes', 'Thick clothes', setThickClothes)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={thickClothes}
                    onChange={(e) => setThickClothes(e.target.value)}
                    placeholder="01 not thick clothes"
                  />
                  {renderFieldExplainBox('thick clothes', 'Thick Clothes', '面料厚度、保暖或透气实测感受...')}
                </div>

                {/* Hot at noon */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">Hot at noon</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('Hot at noon', 'Hot at noon', setHotAtNoon)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={hotAtNoon}
                    onChange={(e) => setHotAtNoon(e.target.value)}
                    placeholder="Not hot"
                  />
                  {renderFieldExplainBox('Hot at noon', 'Hot at noon', '中午炎热度体感（如: noon not hot / cool but fabric thin）...')}
                </div>

                {/* diao dai */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">diao dai</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('diao dai', 'Diao dai', setDiaoDai)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={diaoDai}
                    onChange={(e) => setDiaoDai(e.target.value)}
                    placeholder="diao dai / not diao dai"
                  />
                  {renderFieldExplainBox('diao dai', 'Diao dai', '例如: diao dai not hook at noon, but pain in shoulder.')}
                </div>

                {/* side breast ok: with 聚拢 bra */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">side breast ok: with 聚拢 bra</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('side breast ok: with 聚拢 bra', 'side breast ok: with 聚拢 bra', setSideBreastOkWithGatherBra)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={sideBreastOkWithGatherBra}
                    onChange={(e) => setSideBreastOkWithGatherBra(e.target.value)}
                    placeholder="side breast shape ok"
                  />
                  {renderFieldExplainBox('side breast ok: with 聚拢 bra', 'side breast ok: with 聚拢 bra', '例如: 配合聚拢bra侧面胸型优美饱满 / side breast shape ok...')}
                </div>

                {/* side breast ok: with side out bra */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">side breast ok: with side out bra</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('side breast ok: with side out bra', 'side breast ok: with side out bra', setSideBreastOkWithSideOutBra)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={sideBreastOkWithSideOutBra}
                    onChange={(e) => setSideBreastOkWithSideOutBra(e.target.value)}
                    placeholder="side breast out / ugly"
                  />
                  {renderFieldExplainBox('side breast ok: with side out bra', 'side breast ok: with side out bra', '例如: side breast out, 配合侧开内衣侧面副乳外溢或扁塌...')}
                </div>

                {/* sit breast ok: With 聚拢bra */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">sit breast ok: With 聚拢bra</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('sit breast ok: With 聚拢bra', 'sit breast ok: With 聚拢bra', setSitBreastOkWithGatherBra)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={sitBreastOkWithGatherBra}
                    onChange={(e) => setSitBreastOkWithGatherBra(e.target.value)}
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
                      onClick={() => openPreviousPicker('sit breast ok: with side out bra', 'sit breast ok: with side out bra', setSitBreastOkWithSideOutBra)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={sitBreastOkWithSideOutBra}
                    onChange={(e) => setSitBreastOkWithSideOutBra(e.target.value)}
                    placeholder="e.g. ugly / sit breast shape ok"
                  />
                  {renderFieldExplainBox('sit breast ok: with side out bra', 'sit breast ok: with side out bra', '例如: ugly, 没有聚拢效果显扁塌 / sit breast shape ok...')}
                </div>

                {/* center chest */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">center chest</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('center chest', 'Center chest', setCenterChest)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={centerChest}
                    onChange={(e) => setCenterChest(e.target.value)}
                    placeholder="no center chest"
                  />
                  {renderFieldExplainBox('center chest', 'Center chest', '正中胸骨受压或聚拢舒适度...')}
                </div>

                {/* beautiful & beatiful reason */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">beautiful</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('beautiful', 'Appearance', setBeautiful)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={beautiful}
                    onChange={(e) => setBeautiful(e.target.value)}
                    placeholder="Beautiful"
                  />
                  {renderFieldExplainBox('beautiful', 'Beautiful', '外观拍照美感、上身体型修饰度...')}
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">beatiful reason</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('beatiful reason', 'Appearance reason', setBeatifulReason)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={beatifulReason}
                    onChange={(e) => setBeatifulReason(e.target.value)}
                    placeholder="flattering fit, vibrant color"
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
                          onSave: (val) => setWet(val)
                        })}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 transition-colors shadow-2xs cursor-pointer"
                        title="为 wet 字段新建选项 (如 middle)"
                      >
                        <Plus className="w-3 h-3 text-purple-600" />
                        + 新建选项
                      </button>
                      <button
                        type="button"
                        onClick={() => openPreviousPicker('wet', 'Wet status', (v) => setWet(v), wet)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 cursor-pointer"
                      >
                        <History className="w-3 h-3 text-purple-600" />
                        Previous
                      </button>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <select
                      value={wet}
                      onChange={(e) => {
                        if (e.target.value === '__NEW_OPTION__') {
                          setCreateOptionConfig({
                            isOpen: true,
                            fieldName: 'wet',
                            fieldLabel: 'Wet status (潮湿状态)',
                            onSave: (val) => setWet(val)
                          });
                        } else {
                          setWet(e.target.value);
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
                        当前选中: <strong className="font-mono text-purple-700 font-bold">{wet || '未选择'}</strong>
                      </span>
                      <button
                        type="button"
                        onClick={() => {
                          const manualVal = window.prompt('直接输入或修改 wet 选项值 (例如 middle, dry, wet 等):', wet);
                          if (manualVal !== null && manualVal.trim()) {
                            const clean = manualVal.trim();
                            setWet(clean);
                            fieldDictionaryService.addTypicalValue('wet', {
                              valueEn: clean,
                              valueCn: clean,
                              meaningCn: ''
                            });
                          }
                        }}
                        className="text-purple-600 hover:text-purple-800 hover:underline cursor-pointer text-[10px] font-semibold flex items-center gap-0.5"
                      >
                        ⌨️ 直接键入 / 自由输入
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
                      onClick={() => openPreviousPicker('wet reason', 'Drying reason', setWetReason)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={wetReason}
                    onChange={(e) => setWetReason(e.target.value)}
                    placeholder="quick-drying fabric"
                  />
                  {renderFieldExplainBox('wet reason', 'Wet Reason', '透气性、晾晒风干速度等体感记录...')}
                </div>

                {/* long dress */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">long dress</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('long dress', 'Dress length', setLongDress)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <AutoResizeTextarea
                    value={longDress}
                    onChange={(e) => setLongDress(e.target.value)}
                    placeholder="short dress / long dress"
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
                              onClick={() => openPreviousPicker('when with aircond: hot feeling', 'when with aircond: hot feeling', setWhenAircondHot)}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                            >
                              <History className="w-3 h-3 text-purple-600" />
                              Previous
                            </button>
                          </div>
                          <AutoResizeTextarea
                            value={whenAircondHot}
                            onChange={(e) => setWhenAircondHot(e.target.value)}
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
                              onClick={() => openPreviousPicker('when with aircond: leg feeling', 'when with aircond: leg feeling', setWhenAircondLeg)}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                            >
                              <History className="w-3 h-3 text-purple-600" />
                              Previous
                            </button>
                          </div>
                          <AutoResizeTextarea
                            value={whenAircondLeg}
                            onChange={(e) => setWhenAircondLeg(e.target.value)}
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
                              onClick={() => openPreviousPicker('when without aircond: hot feeling', 'when without aircond: hot feeling', setWhenNoAircondHot)}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                            >
                              <History className="w-3 h-3 text-purple-600" />
                              Previous
                            </button>
                          </div>
                          <AutoResizeTextarea
                            value={whenNoAircondHot}
                            onChange={(e) => setWhenNoAircondHot(e.target.value)}
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
                              onClick={() => openPreviousPicker('when without aircond: leg feeling', 'when without aircond: leg feeling', setWhenNoAircondLeg)}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                            >
                              <History className="w-3 h-3 text-purple-600" />
                              Previous
                            </button>
                          </div>
                          <AutoResizeTextarea
                            value={whenNoAircondLeg}
                            onChange={(e) => setWhenNoAircondLeg(e.target.value)}
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
                              onClick={() => openPreviousPicker('when sleep under fan: hot feeling', 'when sleep under fan: hot feeling', setWhenSleepFanHot)}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                            >
                              <History className="w-3 h-3 text-purple-600" />
                              Previous
                            </button>
                          </div>
                          <AutoResizeTextarea
                            value={whenSleepFanHot}
                            onChange={(e) => setWhenSleepFanHot(e.target.value)}
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
                              onClick={() => openPreviousPicker('when sleep under fan: leg feeling', 'when sleep under fan: leg feeling', setWhenSleepFanLeg)}
                              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                            >
                              <History className="w-3 h-3 text-purple-600" />
                              Previous
                            </button>
                          </div>
                          <AutoResizeTextarea
                            value={whenSleepFanLeg}
                            onChange={(e) => setWhenSleepFanLeg(e.target.value)}
                            placeholder="e.g. middle cold / cold foot / warm"
                          />
                          {renderFieldExplainBox('when sleep under fan: leg feeling', 'when sleep under fan: leg feeling', '例如: due to middle long dress, inner thin, easy fold up 裙底，even it is thick clothes outer...')}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Descriptions */}
            <div className="p-4 rounded-2xl bg-white border border-slate-200 space-y-4">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-mono font-bold uppercase text-slate-600">Descriptions</label>
                  <button
                    type="button"
                    onClick={() => openPreviousPicker('Descriptions', 'Descriptions', setDescriptions)}
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                  >
                    <History className="w-3 h-3 text-purple-600" />
                    Previous
                  </button>
                </div>
                <AutoResizeTextarea
                  minHeight={56}
                  placeholder="Styling notes, fit details..."
                  value={descriptions}
                  onChange={(e) => setDescriptions(e.target.value)}
                />
                {renderFieldExplainBox('Descriptions', 'Descriptions', '整体版型或搭配体感重点补充...')}
              </div>

              <div>
                <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                  last comment or action
                </label>
                <AutoResizeTextarea
                  placeholder="e.g. Needs gentle cycle"
                  value={lastComment}
                  onChange={(e) => setLastComment(e.target.value)}
                />
                {renderFieldExplainBox('last comment or action', 'Last comment or action', '最新处理意见或下次穿着提醒...')}
              </div>
            </div>

            {/* Pictures Upload */}
            <div className="p-4 rounded-2xl bg-white border border-slate-200 space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1">
                  <ImageIcon className="w-3.5 h-3.5 text-purple-600" />
                  Clothes Photos ({pictures.length})
                </label>

                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadingPic}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs disabled:opacity-60"
                >
                  <Upload className="w-3.5 h-3.5" />
                  <span>{uploadingPic ? 'Uploading...' : 'Upload Photo'}</span>
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  multiple
                  className="hidden"
                  onChange={handlePhotoUpload}
                />
              </div>

              {/* Upload Progress Indicator with Real Percent Status */}
              {uploadingPic && (
                <div className="p-3 bg-purple-50 rounded-2xl border border-purple-200 space-y-1.5 animate-in fade-in">
                  <div className="flex items-center justify-between text-xs font-bold text-purple-900">
                    <span className="flex items-center gap-1.5">
                      <Loader2 className="w-3.5 h-3.5 animate-spin text-purple-600" />
                      Uploading &amp; Processing Image...
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

              {pictures.length > 0 ? (
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {pictures.map((pic) => (
                    <div
                      key={pic.id}
                      className="group relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-100 aspect-square shadow-2xs"
                    >
                      <img
                        src={resolveImageSource(pic)}
                        alt={pic.name || 'Clothes photo'}
                        className="w-full h-full object-contain p-1"
                        referrerPolicy="no-referrer"
                      />
                      <button
                        type="button"
                        onClick={() => handleRemovePhoto(pic.id)}
                        className="absolute top-2 right-2 p-1.5 rounded-full bg-rose-600/90 text-white hover:bg-rose-700 transition-colors shadow-xs"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <div
                  onClick={() => fileInputRef.current?.click()}
                  className="p-5 border-2 border-dashed border-purple-200 rounded-2xl bg-purple-50/40 text-center cursor-pointer hover:border-purple-400 transition-colors"
                >
                  <Camera className="w-7 h-7 text-purple-400 mx-auto mb-1" />
                  <p className="text-xs font-semibold text-purple-900">
                    No photos uploaded yet
                  </p>
                  <p className="text-[11px] text-purple-600/80">
                    Click here to attach photos from your camera or files
                  </p>
                </div>
              )}
            </div>
          </form>

          {/* Modal Footer */}
          <div className="px-4 sm:px-6 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] border-t border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-end gap-2 shrink-0 w-full max-w-full shadow-xs">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 border border-slate-200 transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              className="px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs cursor-pointer active:scale-95"
            >
              Add to Wardrobe
            </button>
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
    </>
  );
};

export const AddGarmentModal = AddClothesModal;
