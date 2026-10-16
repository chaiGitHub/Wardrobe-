import React, { useState, useRef } from 'react';
import { ClothesItem, ReportClothesJunction, WearingReport, PictureAttachment } from '../types';
import { dataService, processImageFile } from '../services/dataService';
import { PreviousEntriesModal, PreviousEntryItem } from './PreviousEntriesModal';
import { ImageViewerModal } from './ImageViewerModal';
import { resolveImageSource } from '../utils/imageUtils';
import {
  X, CheckCircle2, AlertCircle, Sparkles, ThermometerSun, ShieldCheck, DollarSign,
  Clock, RefreshCw, Calendar, Tag, ChevronRight, Droplets, Info, Image as ImageIcon,
  Edit3, Save, Plus, Trash2, Camera, Upload, History, ArrowLeft, Maximize2, Minimize2
} from 'lucide-react';

interface GarmentModalProps {
  garment: ClothesItem | null;
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
  onMarkWashed: (clothesId: string) => void;
  onUpdateGarment: (clothesId: string, updates: Partial<ClothesItem>) => void;
  onSelectReport?: (report: WearingReport) => void;
  onDeleteGarment?: (clothesId: string) => void;
}

export const GarmentModal: React.FC<GarmentModalProps> = ({
  garment,
  stats,
  junctions,
  reports,
  onClose,
  onMarkWashed,
  onUpdateGarment,
  onSelectReport,
  onDeleteGarment
}) => {
  if (!garment) return null;

  // Edit Mode State
  const [isEditing, setIsEditing] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [aiAnalysis, setAiAnalysis] = useState(garment['AI Clothes Analysis'] || '');
  const [errorMsg, setErrorMsg] = useState('');
  const [uploadingPic, setUploadingPic] = useState(false);

  // Editable Form Fields
  const [editName, setEditName] = useState(garment['Name'] || '');
  const [editClothesType, setEditClothesType] = useState(garment['Clothes Type'] || 'Dress');
  const [editMaterial, setEditMaterial] = useState(garment['material'] || '');
  const [editThickClothes, setEditThickClothes] = useState(garment['thick clothes'] || '');
  const [editHotAtNoon, setEditHotAtNoon] = useState(garment['Hot at noon'] || '');
  const [editDiaoDai, setEditDiaoDai] = useState(garment['diao dai'] || '');
  const [editSideBreastOk, setEditSideBreastOk] = useState(garment['side breast ok'] || '');
  const [editSitBreastOk, setEditSitBreastOk] = useState(garment['sit breast ok'] || '');
  const [editCenterChest, setEditCenterChest] = useState(garment['center chest'] || '');
  const [editBeautiful, setEditBeautiful] = useState(garment['beautiful'] || '');
  const [editBeatifulReason, setEditBeatifulReason] = useState(garment['beatiful reason'] || '');
  const [editWet, setEditWet] = useState(garment['wet'] || 'dry');
  const [editWetReason, setEditWetReason] = useState(garment['wet reason'] || '');
  const [editLongDress, setEditLongDress] = useState(garment['long dress'] || '');
  const [editPrice, setEditPrice] = useState<string>(garment['price'] !== undefined ? String(garment['price']) : '');
  const [editMayThrow, setEditMayThrow] = useState(garment['may throw'] || '');
  const [editDescriptions, setEditDescriptions] = useState(garment['Descriptions'] || '');
  const [editDescriptions2, setEditDescriptions2] = useState(garment['Descriptions 2'] || '');
  const [editLastComment, setEditLastComment] = useState(garment['last comment or action'] || '');
  const [editTags, setEditTags] = useState<string[]>(garment['tags'] || []);
  const [newTagInput, setNewTagInput] = useState('');
  const [editPictures, setEditPictures] = useState<PictureAttachment[]>(garment['Pictures'] || []);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const viewFileInputRef = useRef<HTMLInputElement>(null);

  // Fullscreen Modal state (defaults to true for immersive full-screen experience)
  const [isFullScreen, setIsFullScreen] = useState(true);

  // Original photo quality toggle
  const [uploadOriginalQuality, setUploadOriginalQuality] = useState(true);

  // Fullscreen Image Viewer state
  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerInitialIndex, setViewerInitialIndex] = useState(0);

  // Delete Garment Confirmation state
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // Previous Entries Picker State
  const [previousModalConfig, setPreviousModalConfig] = useState<{
    isOpen: boolean;
    fieldName: string;
    fieldLabel: string;
    entries: PreviousEntryItem[];
    onSelect: (val: string) => void;
  } | null>(null);

  // Open Previous Value Picker for a given field
  const openPreviousPicker = (
    fieldKey: string,
    label: string,
    onSelectVal: (v: string) => void
  ) => {
    let historical = dataService.getFieldPreviousValues(fieldKey, 'clothes');
    if (fieldKey === 'Clothes Type') {
      const defaultTypes = ['Dress', 'bra', 'Tshirt', 'Pant', 'skirt', 'sweater', 'jacket', 'coat', 'top', 'blouse', 'cardigan', 'shorts', 'Other'];
      defaultTypes.forEach(dt => {
        if (!historical.some(h => h.value.toLowerCase() === dt.toLowerCase())) {
          historical.push({ value: dt, count: 0 });
        }
      });
    }
    setPreviousModalConfig({
      isOpen: true,
      fieldName: fieldKey,
      fieldLabel: label,
      entries: historical,
      onSelect: onSelectVal
    });
  };

  // Find all wearing reports where this garment was included
  const garmentJunctions = junctions.filter(j => {
    const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
    return cId === garment.id;
  });

  const wornReports = garmentJunctions.map(j => {
    const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
    const rep = reports.find(r => r.id === rId || (r['Name'] && r['Name'] === (typeof j['wearing report id'] === 'object' ? j['wearing report id'].title : '')));
    return {
      junction: j,
      report: rep || ({
        id: rId || `rep_fallback_${Date.now()}`,
        Name: typeof j['wearing report id'] === 'object' ? j['wearing report id'].title : 'Wear Session',
        'wear date': new Date().toISOString()
      } as WearingReport)
    };
  });

  const priceVal = typeof garment['price'] === 'number' ? garment['price'] : undefined;
  const washCount = garment['Total Washed Count'] || stats.totalWears;
  const costPerWear = priceVal && washCount > 0
    ? (priceVal / washCount).toFixed(2)
    : null;

  const handleRunAiAnalysis = async () => {
    setAnalyzing(true);
    setErrorMsg('');
    try {
      const res = await fetch('/api/ai/analyze-garment', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ garment })
      });
      const data = await res.json();
      if (res.ok && data.analysis) {
        setAiAnalysis(data.analysis);
        onUpdateGarment(garment.id, {
          'AI Clothes Analysis': data.analysis
        });
      } else {
        setErrorMsg(data.error || 'Failed to generate analysis.');
      }
    } catch (e: any) {
      setErrorMsg(e.message || 'Error communicating with AI service.');
    } finally {
      setAnalyzing(false);
    }
  };

  // Picture file upload handler (converts to base64 DataURL)
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>, directSave = false) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploadingPic(true);
    try {
      const newPics: PictureAttachment[] = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const processed = await processImageFile(file, { originalQuality: uploadOriginalQuality });
        newPics.push(processed);
      }

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
      setUploadingPic(false);
      if (e.target) e.target.value = '';
    }
  };

  const handleRemovePhoto = (picId: string) => {
    setEditPictures(prev => prev.filter(p => p.id !== picId));
  };

  const handleDirectDeletePhoto = (picId: string) => {
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
      'side breast ok': editSideBreastOk,
      'sit breast ok': editSitBreastOk,
      'center chest': editCenterChest,
      'beautiful': editBeautiful,
      'beatiful reason': editBeatifulReason,
      'wet': editWet as any,
      'wet reason': editWetReason,
      'long dress': editLongDress,
      'price': isNaN(numPrice as number) ? undefined : numPrice,
      'may throw': editMayThrow,
      'Descriptions': editDescriptions,
      'Descriptions 2': editDescriptions2,
      'last comment or action': editLastComment,
      'tags': editTags,
      'Pictures': editPictures
    };

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
    setEditSitBreastOk(garment['sit breast ok'] || '');
    setEditCenterChest(garment['center chest'] || '');
    setEditBeautiful(garment['beautiful'] || '');
    setEditBeatifulReason(garment['beatiful reason'] || '');
    setEditWet(garment['wet'] || 'dry');
    setEditWetReason(garment['wet reason'] || '');
    setEditLongDress(garment['long dress'] || '');
    setEditPrice(garment['price'] !== undefined ? String(garment['price']) : '');
    setEditMayThrow(garment['may throw'] || '');
    setEditDescriptions(garment['Descriptions'] || '');
    setEditDescriptions2(garment['Descriptions 2'] || '');
    setEditLastComment(garment['last comment or action'] || '');
    setEditTags(garment['tags'] || []);
    setEditPictures(garment['Pictures'] || []);
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

  return (
    <>
      <div
        id="garment-modal-backdrop"
        className="fixed inset-0 z-50 bg-black/75 backdrop-blur-xs flex items-center justify-center p-0 sm:p-4 pointer-events-auto"
        onClick={(e) => {
          if (e.target === e.currentTarget && !isEditing) {
            onClose();
          }
        }}
      >
        {/* Full-screen dialog by default or generous dialog */}
        <div
          id="garment-detail-dialog"
          onClick={(e) => e.stopPropagation()}
          className={
            isFullScreen
              ? "fixed inset-0 z-50 w-screen h-screen max-w-none max-h-none rounded-none bg-white overflow-hidden flex flex-col animate-in fade-in duration-150"
              : "w-full h-full sm:max-w-5xl sm:h-[94vh] bg-white sm:rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col animate-in fade-in duration-200"
          }
        >
          {/* Header Bar */}
          <div className="flex items-center justify-between px-4 sm:px-6 py-3.5 border-b border-slate-200/80 bg-slate-50/90 shrink-0">
            <div className="flex items-center gap-2 min-w-0">
              {isEditing ? (
                <div className="flex items-center gap-1.5 text-purple-900 font-bold text-sm">
                  <Edit3 className="w-4 h-4 text-purple-600" />
                  <span>Editing Garment Record</span>
                </div>
              ) : (
                <>
                  <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-200 text-slate-800 shrink-0">
                    {garment['Clothes Type']}
                  </span>
                  {garment['beautiful'] && (
                    <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200 truncate">
                      <Sparkles className="w-3 h-3 text-amber-500 fill-amber-400 shrink-0" />
                      <span className="truncate">{garment['beautiful']}</span>
                    </span>
                  )}
                </>
              )}
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsFullScreen(!isFullScreen)}
                className="p-1.5 rounded-full text-slate-500 hover:text-slate-800 hover:bg-slate-200/60 transition-colors"
                title={isFullScreen ? "Exit Full Screen" : "Full Screen"}
              >
                {isFullScreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
              </button>

              {!isEditing ? (
                <>
                  <button
                    type="button"
                    onClick={() => setShowDeleteConfirm(true)}
                    className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-semibold text-rose-600 hover:bg-rose-50 hover:text-rose-700 transition-colors border border-transparent hover:border-rose-200"
                    title="Delete Garment"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span className="hidden sm:inline">Delete</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsEditing(true)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200 transition-colors shadow-2xs"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                    <span>Edit Garment</span>
                  </button>
                  <button
                    onClick={onClose}
                    className="p-1.5 rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors"
                    aria-label="Close modal"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </>
              ) : (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={cancelEdit}
                    className="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleSaveEdit}
                    className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition-all shadow-xs"
                  >
                    <Save className="w-3.5 h-3.5" />
                    <span>Save Changes</span>
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* Modal Scrollable Body */}
          <div className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-6">
            {isEditing ? (
              /* ================= EDIT MODE ================= */
              <div className="space-y-6">
                {/* Basic Information */}
                <div className="p-4 sm:p-5 rounded-2xl bg-purple-50/40 border border-purple-100 space-y-4">
                  <h4 className="text-xs font-bold text-purple-900 uppercase font-mono tracking-wider">
                    Core Information
                  </h4>

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
                      <input
                        type="text"
                        value={editMaterial}
                        onChange={(e) => setEditMaterial(e.target.value)}
                        placeholder="e.g. Chiffon, Cotton, Silk"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                      />
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
                    </div>
                  </div>
                </div>

                {/* Garment Attribute Properties with Previous buttons */}
                <div className="p-4 sm:p-5 rounded-2xl bg-slate-50/70 border border-slate-200/80 space-y-4">
                  <h4 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                    Garment Attributes & Ergonomics
                  </h4>

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
                      <input
                        type="text"
                        value={editThickClothes}
                        onChange={(e) => setEditThickClothes(e.target.value)}
                        placeholder="e.g. 01 not thick clothes"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
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
                      <input
                        type="text"
                        value={editHotAtNoon}
                        onChange={(e) => setEditHotAtNoon(e.target.value)}
                        placeholder="e.g. noon not hot / cool"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
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
                      <input
                        type="text"
                        value={editDiaoDai}
                        onChange={(e) => setEditDiaoDai(e.target.value)}
                        placeholder="e.g. not diao dai / diao dai"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
                    </div>

                    {/* side breast ok */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">side breast ok</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('side breast ok', 'Side breast ok', setEditSideBreastOk)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editSideBreastOk}
                        onChange={(e) => setEditSideBreastOk(e.target.value)}
                        placeholder="e.g. side breast shape ok"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
                    </div>

                    {/* sit breast ok */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">sit breast ok</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('sit breast ok', 'Sit breast ok', setEditSitBreastOk)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editSitBreastOk}
                        onChange={(e) => setEditSitBreastOk(e.target.value)}
                        placeholder="e.g. sit breast shape ok"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
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
                      <input
                        type="text"
                        value={editCenterChest}
                        onChange={(e) => setEditCenterChest(e.target.value)}
                        placeholder="e.g. center chest comfortable"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
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
                      <input
                        type="text"
                        value={editBeautiful}
                        onChange={(e) => setEditBeautiful(e.target.value)}
                        placeholder="e.g. Beautiful, Cute, Elegant"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
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
                      <input
                        type="text"
                        value={editBeatifulReason}
                        onChange={(e) => setEditBeatifulReason(e.target.value)}
                        placeholder="e.g. classic cut, slimming waist"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
                    </div>

                    {/* wet & wet reason */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">wet</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('wet', 'Wet status', setEditWet)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                        >
                          <History className="w-3 h-3 text-purple-600" />
                          Previous
                        </button>
                      </div>
                      <select
                        value={editWet}
                        onChange={(e) => setEditWet(e.target.value)}
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      >
                        <option value="dry">dry</option>
                        <option value="wet">wet</option>
                        <option value="need dry">need dry</option>
                      </select>
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
                      <input
                        type="text"
                        value={editWetReason}
                        onChange={(e) => setEditWetReason(e.target.value)}
                        placeholder="e.g. quick-dry air hung"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
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
                      <input
                        type="text"
                        value={editLongDress}
                        onChange={(e) => setEditLongDress(e.target.value)}
                        placeholder="e.g. long dress / short dress"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
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
                      <input
                        type="text"
                        value={editMayThrow}
                        onChange={(e) => setEditMayThrow(e.target.value)}
                        placeholder="e.g. no / maybe / donate"
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                      />
                    </div>
                  </div>
                </div>

                {/* Descriptions & Notes */}
                <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200 space-y-4">
                  <h4 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                    Descriptions & Observations
                  </h4>

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
                    <textarea
                      rows={2}
                      value={editDescriptions}
                      onChange={(e) => setEditDescriptions(e.target.value)}
                      placeholder="Detailed notes on silhouette, feel, or pairings..."
                      className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm focus:ring-2 focus:ring-purple-500/20"
                    />
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
                    <textarea
                      rows={2}
                      value={editDescriptions2}
                      onChange={(e) => setEditDescriptions2(e.target.value)}
                      placeholder="Secondary notes, care instructions..."
                      className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm focus:ring-2 focus:ring-purple-500/20"
                    />
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
                    <input
                      type="text"
                      value={editLastComment}
                      onChange={(e) => setEditLastComment(e.target.value)}
                      placeholder="e.g. Iron before next wear"
                      className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                    />
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
                      <label className="inline-flex items-center gap-1.5 text-[11px] text-slate-600 font-medium cursor-pointer select-none">
                        <input
                          type="checkbox"
                          checked={uploadOriginalQuality}
                          onChange={(e) => setUploadOriginalQuality(e.target.checked)}
                          className="rounded text-purple-600 focus:ring-purple-500 w-3.5 h-3.5"
                        />
                        <span>Original photo quality</span>
                      </label>

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

                  {editPictures.length > 0 ? (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      {editPictures.map((pic) => (
                        <div
                          key={pic.id}
                          className="group relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-100 aspect-square shadow-2xs"
                        >
                          <img
                            src={resolveImageSource(pic)}
                            alt={pic.name || 'Garment photo'}
                            className="w-full h-full object-cover"
                            referrerPolicy="no-referrer"
                          />
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
                  <div className="flex items-center gap-2 shrink-0">
                    {stats.needWash ? (
                      <button
                        onClick={() => onMarkWashed(garment.id)}
                        className="inline-flex items-center gap-2 px-4 py-2 rounded-2xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold transition-all shadow-xs active:scale-95"
                      >
                        <AlertCircle className="w-4 h-4" />
                        <span>need wash ({stats.unwashedHours}h) · Wash Now</span>
                      </button>
                    ) : (
                      <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-2xl bg-emerald-50 text-emerald-800 border border-emerald-200 text-xs font-semibold">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                        <span>wash (clean)</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Primary Photo Banner with instant upload button */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5 font-mono">
                      <ImageIcon className="w-3.5 h-3.5 text-purple-600" />
                      Garment Photos ({pictures.length})
                    </span>

                    <div className="flex items-center gap-3">
                      <label className="inline-flex items-center gap-1.5 text-[11px] text-slate-600 font-medium cursor-pointer select-none">
                        <input
                          type="checkbox"
                          checked={uploadOriginalQuality}
                          onChange={(e) => setUploadOriginalQuality(e.target.checked)}
                          className="rounded text-purple-600 focus:ring-purple-500 w-3.5 h-3.5"
                        />
                        <span>Original quality</span>
                      </label>

                      {/* Direct Add Picture button */}
                      <button
                        type="button"
                        onClick={() => viewFileInputRef.current?.click()}
                        disabled={uploadingPic}
                        className="inline-flex items-center gap-1 px-3 py-1 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 text-xs font-bold transition-all shadow-2xs disabled:opacity-60"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        <span>{uploadingPic ? 'Uploading...' : 'Add Picture'}</span>
                      </button>
                    </div>
                    <input
                      ref={viewFileInputRef}
                      type="file"
                      accept="image/*"
                      multiple
                      className="hidden"
                      onChange={(e) => handlePhotoUpload(e, true)}
                    />
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
                            <img
                              src={src}
                              alt={p.name || `Photo ${idx + 1}`}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                              referrerPolicy="no-referrer"
                              onError={(e) => {
                                const target = e.target as HTMLImageElement;
                                target.onerror = null;
                                target.style.opacity = '0.4';
                              }}
                            />
                            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center pointer-events-none">
                              <span className="p-2 rounded-full bg-black/60 text-white opacity-0 group-hover:opacity-100 transition-opacity shadow-sm">
                                <Maximize2 className="w-4 h-4" />
                              </span>
                            </div>
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDirectDeletePhoto(p.id);
                              }}
                              className="absolute top-2 right-2 p-1.5 rounded-full bg-black/60 hover:bg-rose-600 text-white opacity-0 group-hover:opacity-100 transition-all shadow-xs z-10"
                              title="Delete photo"
                            >
                              <Trash2 className="w-3 h-3" />
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div
                      onClick={() => viewFileInputRef.current?.click()}
                      className="p-5 rounded-2xl bg-purple-50/40 border border-dashed border-purple-200 flex flex-col items-center justify-center cursor-pointer hover:bg-purple-50/70 transition-colors text-center"
                    >
                      <Camera className="w-7 h-7 text-purple-400 mb-1" />
                      <p className="text-xs font-semibold text-purple-900">
                        No photos attached yet
                      </p>
                      <p className="text-[11px] text-purple-600/80">
                        Click here to add picture from your camera or photo library
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
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">Clothes Type:</span>
                      <span className="font-semibold text-slate-800">{garment['Clothes Type'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">material:</span>
                      <span className="font-semibold text-slate-800">{garment['material'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">thick clothes:</span>
                      <span className="font-semibold text-slate-800">{garment['thick clothes'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">Hot at noon:</span>
                      <span className="font-semibold text-slate-800">{garment['Hot at noon'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">diao dai:</span>
                      <span className="font-semibold text-slate-800">{garment['diao dai'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">side breast ok:</span>
                      <span className="font-semibold text-slate-800">{garment['side breast ok'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">sit breast ok:</span>
                      <span className="font-semibold text-slate-800">{garment['sit breast ok'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">center chest:</span>
                      <span className="font-semibold text-slate-800">{garment['center chest'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">beautiful:</span>
                      <span className="font-semibold text-slate-800">{garment['beautiful'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">wet:</span>
                      <span className="font-semibold text-slate-800">{garment['wet'] || <span className="text-slate-400 font-normal italic">dry</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">long dress:</span>
                      <span className="font-semibold text-slate-800">{garment['long dress'] || <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">may throw:</span>
                      <span className="font-semibold text-slate-800">{garment['may throw'] || <span className="text-slate-400 font-normal italic">no</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">price:</span>
                      <span className="font-semibold text-slate-800">{typeof garment['price'] === 'number' ? `$${garment['price']}` : <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">wearing frequency(legacy):</span>
                      <span className="font-semibold text-slate-800">{garment['wearing frequency(how many times legacy washed)'] !== undefined ? garment['wearing frequency(how many times legacy washed)'] : <span className="text-slate-400 font-normal italic">0</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">Total Washed Count:</span>
                      <span className="font-semibold text-slate-800">{garment['Total Washed Count'] ?? 0}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">Tracked App Washes:</span>
                      <span className="font-semibold text-slate-800">{garment['Tracked App Washes'] ?? 0}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">Last Washed:</span>
                      <span className="font-semibold text-slate-800">{garment['Last Washed'] ? new Date(garment['Last Washed']).toLocaleDateString() : <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60">
                      <span className="font-mono text-slate-500">unwash time rollup:</span>
                      <span className="font-semibold text-slate-800">{garment['unwash time rollup'] !== undefined ? `${garment['unwash time rollup']}s` : <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                    <div className="flex justify-between py-1.5 border-b border-slate-200/60 sm:col-span-2">
                      <span className="font-mono text-slate-500">created:</span>
                      <span className="font-semibold text-slate-800">{garment['created'] ? new Date(garment['created']).toLocaleDateString() : <span className="text-slate-400 font-normal italic">No data</span>}</span>
                    </div>
                  </div>

                  <div className="pt-2 text-xs border-t border-slate-200/60">
                    <span className="font-mono text-slate-500 block font-bold">wet reason:</span>
                    <p className={`mt-0.5 ${garment['wet reason'] ? 'text-slate-800 font-medium' : 'text-slate-400 italic'}`}>
                      {garment['wet reason'] || 'No data'}
                    </p>
                  </div>

                  <div className="pt-2 text-xs border-t border-slate-200/60">
                    <span className="font-mono text-slate-500 block font-bold">beatiful reason:</span>
                    <p className={`mt-0.5 ${garment['beatiful reason'] ? 'text-slate-800 font-medium' : 'text-slate-400 italic'}`}>
                      {garment['beatiful reason'] || 'No data'}
                    </p>
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
                      <p className="text-slate-700 font-medium">{garment['Descriptions']}</p>
                    ) : (
                      <p className="text-slate-400 italic">No data</p>
                    )}
                  </div>
                  <div className="p-3.5 rounded-2xl bg-white border border-slate-200 text-xs leading-relaxed">
                    <span className="font-mono font-bold text-slate-400 block mb-1">Descriptions 2:</span>
                    {garment['Descriptions 2'] ? (
                      <p className="text-slate-700 font-medium">{garment['Descriptions 2']}</p>
                    ) : (
                      <p className="text-slate-400 italic">No data</p>
                    )}
                  </div>
                  <div className="p-3 rounded-xl bg-slate-50 text-xs border border-slate-100">
                    <span className="font-mono font-bold text-slate-400 block mb-0.5">last comment or action:</span>
                    {garment['last comment or action'] ? (
                      <p className="text-slate-700 font-medium">{garment['last comment or action']}</p>
                    ) : (
                      <p className="text-slate-400 italic">No data</p>
                    )}
                  </div>
                </div>

                {/* Tags */}
                <div className="space-y-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5 font-mono">
                    <Tag className="w-3.5 h-3.5 text-purple-600" />
                    tags:
                  </span>
                  {garment['tags'] && garment['tags'].length > 0 ? (
                    <div className="flex flex-wrap gap-1.5">
                      {garment['tags'].map((tag, idx) => (
                        <span key={idx} className="px-2.5 py-1 rounded-lg bg-purple-50 text-purple-700 text-xs font-medium border border-purple-100">
                          {tag}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 italic">No data</p>
                  )}
                </div>

                {/* AI Clothes Analysis Card */}
                <div className="p-4 rounded-2xl bg-purple-50/50 border border-purple-100 space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <Sparkles className="w-4 h-4 text-purple-600" />
                      <h4 className="text-xs font-bold text-slate-900 uppercase font-mono">
                        AI Clothes Analysis
                      </h4>
                    </div>
                    <button
                      onClick={handleRunAiAnalysis}
                      disabled={analyzing}
                      className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-semibold bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-60 transition-all shadow-xs"
                    >
                      <RefreshCw className={`w-3.5 h-3.5 ${analyzing ? 'animate-spin' : ''}`} />
                      {analyzing ? 'Analyzing...' : 'Re-assess'}
                    </button>
                  </div>

                  {errorMsg && (
                    <p className="text-xs text-rose-600 bg-rose-50 p-2 rounded-lg">{errorMsg}</p>
                  )}

                  {aiAnalysis ? (
                    <div className="text-xs text-slate-700 leading-relaxed bg-white/90 p-4 rounded-xl border border-purple-100 max-h-56 overflow-y-auto whitespace-pre-wrap">
                      {aiAnalysis}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 italic py-1">
                      No AI assessment recorded yet. Click Re-assess to generate an ergonomic and sensory review.
                    </p>
                  )}
                </div>

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
                                {junction['wash stated']}
                              </span>
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
          <div className="p-4 border-t border-slate-100 bg-slate-50 flex items-center justify-between shrink-0">
            <span className="text-xs text-slate-400 font-mono">
              ID: {garment.id}
            </span>

            <div className="flex items-center gap-2">
              {isEditing ? (
                <>
                  <button
                    type="button"
                    onClick={cancelEdit}
                    className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleSaveEdit}
                    className="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs"
                  >
                    Save Changes
                  </button>
                </>
              ) : (
                <button
                  onClick={onClose}
                  className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition-all shadow-xs"
                >
                  Close
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Previous Entries Picker Modal */}
      {previousModalConfig && (
        <PreviousEntriesModal
          isOpen={previousModalConfig.isOpen}
          fieldName={previousModalConfig.fieldName}
          fieldLabel={previousModalConfig.fieldLabel}
          entries={previousModalConfig.entries}
          onSelect={previousModalConfig.onSelect}
          onClose={() => setPreviousModalConfig(null)}
        />
      )}

      {/* Fullscreen Photo Viewer Modal */}
      <ImageViewerModal
        images={pictures}
        initialIndex={viewerInitialIndex}
        isOpen={viewerOpen}
        onClose={() => setViewerOpen(false)}
      />

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div
          className="fixed inset-0 z-[70] bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 animate-in fade-in"
          onClick={() => !isDeleting && setShowDeleteConfirm(false)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-slate-200 space-y-4 text-center animate-in zoom-in-95 duration-150"
          >
            <div className="w-12 h-12 rounded-2xl bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
              <Trash2 className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-slate-900">Delete Garment?</h3>
              <p className="text-xs text-slate-500 mt-1">
                Are you sure you want to delete <span className="font-semibold text-slate-800">"{garment['Name']}"</span>?
                This action cannot be undone and will remove it from your closet records.
              </p>
            </div>
            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                type="button"
                disabled={isDeleting}
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 py-2.5 px-4 rounded-xl border border-slate-200 text-slate-700 font-bold text-xs hover:bg-slate-100 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isDeleting}
                onClick={handleConfirmDelete}
                className="flex-1 py-2.5 px-4 rounded-xl bg-rose-600 text-white font-bold text-xs hover:bg-rose-700 transition-colors shadow-sm disabled:opacity-50"
              >
                {isDeleting ? 'Deleting...' : 'Yes, Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
