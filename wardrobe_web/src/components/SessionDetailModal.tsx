import React, { useState, useRef, useEffect } from 'react';
import { WearingReport, ReportClothesJunction, ClothesItem, PictureAttachment } from '../types';
import { dataService, formatDuration, processImageFile } from '../services/dataService';
import { resolveImageSource } from '../utils/imageUtils';
import { PreviousEntriesModal, PreviousEntryItem } from './PreviousEntriesModal';
import { ImageViewerModal } from './ImageViewerModal';
import {
  X, Calendar, Clock, Shirt, Sparkles, Tag, ShieldCheck, Heart, Droplets,
  Image as ImageIcon, ChevronRight, Edit3, Trash2, Save, ArrowLeft, History,
  Plus, Upload, CheckCircle2, AlertTriangle, Search
} from 'lucide-react';

interface SessionDetailModalProps {
  report: WearingReport | null;
  junctions: ReportClothesJunction[];
  clothes: ClothesItem[];
  onClose: () => void;
  onSelectGarment: (garment: ClothesItem) => void;
  onDeleteReport?: (reportId: string) => void;
  onUpdateReport?: (report: WearingReport) => void;
}

interface EditGarmentItem {
  clothesId: string;
  durationHours: number; // exact decimal hours
  hours: number;
  mins: number;
  washStated: 'wash' | 'need wash';
}

export const SessionDetailModal: React.FC<SessionDetailModalProps> = ({
  report,
  junctions,
  clothes,
  onClose,
  onSelectGarment,
  onDeleteReport,
  onUpdateReport
}) => {
  if (!report) return null;

  // Edit Mode State
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [uploadingPic, setUploadingPic] = useState(false);
  const [uploadOriginalQuality, setUploadOriginalQuality] = useState(true);

  // Editable Form Fields
  const [editName, setEditName] = useState(report['Name'] || '');
  const [editWearDate, setEditWearDate] = useState(
    report['wear date'] ? new Date(report['wear date']).toISOString().slice(0, 10) : ''
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
    entries: PreviousEntryItem[];
    onSelect: (val: string) => void;
  } | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Find linked junctions for this report
  const sessionJunctions = junctions.filter(j => {
    const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
    return rId === report.id;
  });

  const sessionClothes = sessionJunctions.map(j => {
    const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
    const garment = clothes.find(c => c.id === cId);
    return { junction: j, garment };
  });

  // Calculate session duration stats
  const maxDurationSec = sessionJunctions.reduce((max, j) => Math.max(max, j['duration'] || 0), 0);
  const formattedSessionDuration = formatDuration(maxDurationSec);

  // Initialize edit fields when entering edit mode
  const startEdit = () => {
    setEditName(report['Name'] || '');
    setEditWearDate(report['wear date'] ? new Date(report['wear date']).toISOString().slice(0, 10) : '');
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

    // Map junctions to editable garments with exact hours and minutes
    const mapped: EditGarmentItem[] = sessionJunctions.map(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      const totalSec = j['duration'] || 0;
      const totalMins = Math.round(totalSec / 60);
      const h = Math.floor(totalMins / 60);
      const m = totalMins % 60;
      return {
        clothesId: cId,
        durationHours: totalSec / 3600,
        hours: h,
        mins: m,
        washStated: (j['wash stated'] as 'wash' | 'need wash') || 'need wash'
      };
    });
    setEditGarments(mapped);
    setIsEditing(true);
  };

  const cancelEdit = () => {
    setIsEditing(false);
    setIsAddingClothes(false);
  };

  const openPreviousPicker = (
    fieldKey: string,
    label: string,
    onSelectVal: (v: string) => void
  ) => {
    const historical = dataService.getFieldPreviousValues(fieldKey, 'reports');
    setPreviousModalConfig({
      isOpen: true,
      fieldName: fieldKey,
      fieldLabel: label,
      entries: historical,
      onSelect: onSelectVal
    });
  };

  const handleUpdateGarmentHoursMins = (clothesId: string, hours: number, mins: number) => {
    const validHours = Math.max(0, hours);
    const validMins = Math.max(0, Math.min(59, mins));
    const totalDecimalHours = validHours + validMins / 60;

    setEditGarments(prev =>
      prev.map(g =>
        g.clothesId === clothesId
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

  const handleToggleWashStated = (clothesId: string) => {
    setEditGarments(prev =>
      prev.map(g =>
        g.clothesId === clothesId
          ? { ...g, washStated: g.washStated === 'need wash' ? 'wash' : 'need wash' }
          : g
      )
    );
  };

  const handleRemoveGarment = (clothesId: string) => {
    setEditGarments(prev => prev.filter(g => g.clothesId !== clothesId));
  };

  const handleAddGarmentToSession = (clothesId: string) => {
    if (editGarments.some(g => g.clothesId === clothesId)) return;
    setEditGarments(prev => [
      ...prev,
      {
        clothesId,
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
    try {
      const newPics: PictureAttachment[] = [];
      for (let i = 0; i < files.length; i++) {
        const processed = await processImageFile(files[i], { originalQuality: uploadOriginalQuality });
        newPics.push(processed);
      }
      setEditPictures(prev => [...prev, ...newPics]);
    } catch (err) {
      console.error('Error uploading photo:', err);
      alert('Error uploading picture. Please try another image file.');
    } finally {
      setUploadingPic(false);
      if (e.target) e.target.value = '';
    }
  };

  const handleRemovePhoto = (picId: string) => {
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
      const updated = await dataService.updateWearingReport(
        report.id,
        {
          'Name': editName.trim() || report['Name'],
          'wear date': new Date(editWearDate).toISOString(),
          'shoulder feel': editShoulderFeel.trim(),
          'mid breast': editMidBreast.trim(),
          'night mid breast': editNightMidBreast.trim(),
          'sweat': editSweat.trim(),
          'night sweat': editNightSweat.trim(),
          'leg feel': editLegFeel.trim(),
          'night leg feel': editNightLegFeel.trim(),
          'report detail': editReportDetail.trim(),
          'tags': editTags,
          'Pictures': editPictures
        },
        editGarments.map(g => ({
          clothesId: g.clothesId,
          durationHours: g.durationHours,
          washStated: g.washStated
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
    c => !editGarments.some(eg => eg.clothesId === c.id) &&
      (c['Name'] || '').toLowerCase().includes(addClothesSearch.toLowerCase())
  );

  const displayPictures = isEditing ? editPictures : (report['Pictures'] || []);

  return (
    <>
      <div
        id="full-screen-session-modal"
        className="fixed inset-0 z-50 bg-white flex flex-col h-screen w-screen overflow-hidden animate-in fade-in duration-200"
      >
        {/* Sticky Full-Screen Header */}
        <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200 bg-white/95 backdrop-blur-md flex items-center justify-between gap-3 shrink-0 shadow-2xs">
          <div className="flex items-center gap-3 min-w-0">
            <button
              onClick={isEditing ? cancelEdit : onClose}
              className="p-2 -ml-2 rounded-xl text-slate-500 hover:text-slate-900 hover:bg-slate-100 transition-colors"
              title="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>

            <div className="space-y-0.5 min-w-0">
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 rounded-md border border-slate-300 text-slate-700 text-[10px] font-mono font-bold uppercase tracking-wider">
                  Wearing Report
                </span>
                <span className="text-xs text-slate-500 font-medium font-mono">
                  {new Date(report['wear date']).toLocaleDateString(undefined, {
                    weekday: 'short',
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                  })}
                </span>
                <span className="text-xs text-slate-700 font-bold hidden sm:inline font-mono">
                  · {formattedSessionDuration}
                </span>
              </div>
              <h2 className="text-base sm:text-xl font-bold text-slate-900 truncate">
                {isEditing ? editName || 'Editing Wearing Report' : report['Name']}
              </h2>
            </div>
          </div>

          {/* Action Toolbar */}
          <div className="flex items-center gap-2 shrink-0">
            {isEditing ? (
              <>
                <button
                  type="button"
                  onClick={cancelEdit}
                  className="px-3 sm:px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:text-slate-900 border border-slate-300 hover:border-slate-400 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveEdit}
                  disabled={isSaving}
                  className="inline-flex items-center gap-1.5 px-4 sm:px-5 py-2 rounded-xl border border-slate-800 hover:border-black text-slate-900 text-xs font-bold transition-all shadow-2xs disabled:opacity-50"
                >
                  <Save className="w-3.5 h-3.5" />
                  <span>{isSaving ? 'Saving...' : 'Save Changes'}</span>
                </button>
              </>
            ) : (
              <>
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(true)}
                  className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                  title="Delete wearing report"
                >
                  <Trash2 className="w-4 h-4" />
                </button>

                <button
                  type="button"
                  onClick={startEdit}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl border border-slate-300 hover:border-slate-500 text-slate-800 text-xs font-bold transition-all"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                  <span>Edit Report</span>
                </button>

                <button
                  type="button"
                  onClick={onClose}
                  className="p-2 rounded-xl text-slate-500 hover:text-slate-800 border border-slate-300 hover:border-slate-400 transition-colors"
                  title="Close report"
                >
                  <X className="w-5 h-5" />
                </button>
              </>
            )}
          </div>
        </div>

        {/* Full-Screen Scrollable Content Area */}
        <div className="flex-1 overflow-y-auto w-full bg-slate-50/50">
          <div className="max-w-5xl mx-auto w-full px-4 sm:px-8 py-6 space-y-6">

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
                      const garment = clothes.find(c => c.id === item.clothesId);
                      return (
                        <div
                          key={item.clothesId}
                          className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col md:flex-row md:items-center justify-between gap-4"
                        >
                          <div className="space-y-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-800 text-[10px] font-bold">
                                {garment?.['Clothes Type'] || 'Garment'}
                              </span>
                              <span className="text-sm font-bold text-slate-900 truncate">
                                {garment?.['Name'] || 'Garment'}
                              </span>
                            </div>
                            <p className="text-[11px] text-slate-500">
                              {garment?.['material']} · {garment?.['thick clothes'] || ''}
                            </p>
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
                                    handleUpdateGarmentHoursMins(item.clothesId, parseInt(e.target.value) || 0, item.mins)
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
                                    handleUpdateGarmentHoursMins(item.clothesId, item.hours, parseInt(e.target.value) || 0)
                                  }
                                  className="w-10 text-center font-bold text-slate-900 bg-slate-50 rounded border border-slate-200 py-0.5"
                                  title="Minutes"
                                />
                                <span className="font-semibold text-slate-500">m</span>
                              </div>
                            </div>

                            {/* Wash Stated Toggle */}
                            <button
                              type="button"
                              onClick={() => handleToggleWashStated(item.clothesId)}
                              className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
                                item.washStated === 'need wash'
                                  ? 'bg-amber-100 text-amber-900 border-amber-300'
                                  : 'bg-emerald-100 text-emerald-900 border-emerald-300'
                              }`}
                            >
                              {item.washStated === 'need wash' ? '⚠️ Needs Wash' : '✨ Clean / Wash'}
                            </button>

                            {/* Remove item from report */}
                            <button
                              type="button"
                              onClick={() => handleRemoveGarment(item.clothesId)}
                              className="p-1.5 text-slate-400 hover:text-rose-600 transition-colors"
                              title="Remove garment from session"
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
                          onClick={() => openPreviousPicker('shoulder feel', 'Shoulder feel', setEditShoulderFeel)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editShoulderFeel}
                        onChange={(e) => setEditShoulderFeel(e.target.value)}
                        placeholder="e.g. relaxed, no shoulder pressure"
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                      />
                    </div>

                    {/* mid breast */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">mid breast</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('mid breast', 'Mid breast feel', setEditMidBreast)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editMidBreast}
                        onChange={(e) => setEditMidBreast(e.target.value)}
                        placeholder="e.g. comfortable support"
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                      />
                    </div>

                    {/* night mid breast */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">night mid breast</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('night mid breast', 'Night mid breast', setEditNightMidBreast)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editNightMidBreast}
                        onChange={(e) => setEditNightMidBreast(e.target.value)}
                        placeholder="e.g. evening comfort check"
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                      />
                    </div>

                    {/* sweat */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">sweat</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('sweat', 'Sweat rate', setEditSweat)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editSweat}
                        onChange={(e) => setEditSweat(e.target.value)}
                        placeholder="e.g. low / dry / breathable"
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                      />
                    </div>

                    {/* night sweat */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">night sweat</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('night sweat', 'Night sweat', setEditNightSweat)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editNightSweat}
                        onChange={(e) => setEditNightSweat(e.target.value)}
                        placeholder="e.g. cool, ventilated"
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                      />
                    </div>

                    {/* leg feel */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">leg feel</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('leg feel', 'Leg feel', setEditLegFeel)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editLegFeel}
                        onChange={(e) => setEditLegFeel(e.target.value)}
                        placeholder="e.g. easy stride, flowy"
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                      />
                    </div>

                    {/* night leg feel */}
                    <div>
                      <div className="flex items-center justify-between mb-1">
                        <label className="text-xs font-mono font-bold uppercase text-slate-600">night leg feel</label>
                        <button
                          type="button"
                          onClick={() => openPreviousPicker('night leg feel', 'Night leg feel', setEditNightLegFeel)}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-semibold border border-slate-300 hover:border-slate-500 text-slate-700 bg-transparent transition-colors"
                        >
                          <History className="w-3 h-3 text-slate-500" />
                          Previous
                        </button>
                      </div>
                      <input
                        type="text"
                        value={editNightLegFeel}
                        onChange={(e) => setEditNightLegFeel(e.target.value)}
                        placeholder="e.g. light and non-binding"
                        className="w-full px-3.5 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                      />
                    </div>
                  </div>

                  {/* report detail */}
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="text-xs font-mono font-bold uppercase text-slate-600">report detail</label>
                      <button
                        type="button"
                        onClick={() => openPreviousPicker('report detail', 'Report detail', setEditReportDetail)}
                        className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                      >
                        <History className="w-3 h-3 text-purple-600" />
                        Previous
                      </button>
                    </div>
                    <textarea
                      rows={3}
                      value={editReportDetail}
                      onChange={(e) => setEditReportDetail(e.target.value)}
                      placeholder="Weather notes, daily events, outfit pairing verdict..."
                      className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm"
                    />
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

                  {editPictures.length > 0 ? (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      {editPictures.map((pic, idx) => (
                        <div
                          key={pic.id || idx}
                          className="relative group rounded-2xl overflow-hidden aspect-square border border-slate-200 bg-slate-100"
                        >
                          <img
                            src={resolveImageSource(pic)}
                            alt={pic.filename || pic.name || 'Outfit photo'}
                            referrerPolicy="no-referrer"
                            className="w-full h-full object-cover cursor-pointer"
                            onClick={() => {
                              setViewerInitialIndex(idx);
                              setViewerOpen(true);
                            }}
                          />
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
                {/* Outfits in this Session */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200/80 shadow-xs space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600 font-mono flex items-center gap-1.5">
                      <Shirt className="w-4 h-4 text-purple-600" />
                      Garments in this Outfit ({sessionClothes.length})
                    </h3>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {sessionClothes.map(({ junction, garment }, idx) => {
                      const title = garment?.['Name'] || (typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].title : 'Garment');
                      const clothesType = garment?.['Clothes Type'] || junction['Clothes Type (from clothes(new))'] || 'Dress';
                      const washState = junction['wash stated'];
                      const junctionDuration = formatDuration(junction['duration'] || 0);

                      return (
                        <div
                          key={idx}
                          onClick={() => garment && onSelectGarment(garment)}
                          className="flex items-center justify-between p-4 rounded-2xl bg-slate-50 hover:bg-purple-50/70 border border-slate-200/80 hover:border-purple-300 cursor-pointer transition-all group shadow-2xs"
                        >
                          <div className="flex items-center gap-3 min-w-0">
                            <div className="w-11 h-11 rounded-2xl bg-purple-100 flex items-center justify-center shrink-0 text-purple-700 font-bold">
                              <Shirt className="w-5 h-5" />
                            </div>
                            <div className="min-w-0">
                              <p className="font-bold text-sm text-slate-900 group-hover:text-purple-700 transition-colors truncate">
                                {title}
                              </p>
                              <p className="text-xs text-slate-500">
                                {clothesType} · {junctionDuration}
                              </p>
                            </div>
                          </div>

                          <div className="flex items-center gap-2 shrink-0">
                            <span
                              className={`px-2.5 py-1 rounded-lg text-[11px] font-bold ${
                                washState === 'need wash'
                                  ? 'bg-amber-100 text-amber-900 border border-amber-200'
                                  : 'bg-emerald-100 text-emerald-900 border border-emerald-200'
                              }`}
                            >
                              {washState === 'need wash' ? '⚠️ Need wash' : '✨ Clean'}
                            </span>
                            <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-purple-600 transition-colors" />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Wearing Report Specification & Database Fields (Exact DB Names, All Fields Visible) */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200/80 shadow-xs space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <div>
                      <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 font-mono">
                        wearing report specification and data
                      </h3>
                      <p className="text-[11px] text-slate-500 font-mono mt-0.5">
                        Exact database field names and raw values (all fields listed)
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {/* Name */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        Name
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['Name'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* wear date */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        wear date
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900 font-mono">
                        {report['wear date'] ? new Date(report['wear date']).toISOString().slice(0, 10) : <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* created */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        created
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900 font-mono">
                        {report['created'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* categories */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        categories
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {(report['categories'] && report['categories'].length > 0) ? report['categories'].join(', ') : <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* tags */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        tags
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {(report['tags'] && report['tags'].length > 0) ? report['tags'].join(', ') : <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* shoulder feel */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        shoulder feel
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['shoulder feel'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* mid breast */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        mid breast
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['mid breast'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* night mid breast */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        night mid breast
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['night mid breast'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* sweat */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        sweat
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['sweat'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* night sweat */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        night sweat
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['night sweat'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* leg feel */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        leg feel
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['leg feel'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>

                    {/* night leg feel */}
                    <div className="p-3.5 rounded-2xl bg-white border border-slate-200">
                      <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-0.5">
                        night leg feel
                      </span>
                      <p className="text-xs sm:text-sm font-semibold text-slate-900">
                        {report['night leg feel'] || <span className="text-slate-400 font-mono">(empty)</span>}
                      </p>
                    </div>
                  </div>

                  {/* report detail */}
                  <div className="p-4 rounded-2xl bg-white border border-slate-200">
                    <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-1">
                      report detail
                    </span>
                    <div className="text-xs sm:text-sm text-slate-800 leading-relaxed whitespace-pre-wrap">
                      {report['report detail'] || <span className="text-slate-400 font-mono">(empty)</span>}
                    </div>
                  </div>

                  {/* AI Wearing Analysis */}
                  <div className="p-4 rounded-2xl bg-white border border-slate-200">
                    <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-1">
                      AI Wearing Analysis
                    </span>
                    <div className="text-xs sm:text-sm text-slate-800 leading-relaxed whitespace-pre-wrap">
                      {report['AI Wearing Analysis'] || <span className="text-slate-400 font-mono">(empty)</span>}
                    </div>
                  </div>

                  {/* Pictures count & listing */}
                  <div className="p-4 rounded-2xl bg-white border border-slate-200">
                    <span className="text-[10px] font-mono font-bold uppercase text-slate-500 block mb-1">
                      Pictures
                    </span>
                    <p className="text-xs sm:text-sm font-mono text-slate-800">
                      {(report['Pictures'] && report['Pictures'].length > 0)
                        ? `${report['Pictures'].length} image(s) recorded in database`
                        : <span className="text-slate-400 font-mono">(empty)</span>}
                    </p>
                  </div>
                </div>

                {/* Attached Outfit Pictures (Click to Fullscreen Zoom & Drag) */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200/80 shadow-xs space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600 font-mono flex items-center gap-1.5">
                      <ImageIcon className="w-4 h-4 text-purple-600" />
                      Outfit Photos ({displayPictures.length})
                    </h3>
                    {displayPictures.length > 0 && (
                      <span className="text-[11px] text-purple-600 font-medium">
                        Click any photo to open full-screen zoom & drag viewer
                      </span>
                    )}
                  </div>

                  {displayPictures.length > 0 ? (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      {displayPictures.map((pic, idx) => (
                        <div
                          key={pic.id || idx}
                          onClick={() => {
                            setViewerInitialIndex(idx);
                            setViewerOpen(true);
                          }}
                          className="group relative rounded-2xl overflow-hidden aspect-square border border-slate-200 bg-slate-100 cursor-pointer shadow-2xs hover:shadow-md transition-all"
                        >
                          <img
                            src={resolveImageSource(pic)}
                            alt={pic.filename || pic.name || 'Outfit photo'}
                            referrerPolicy="no-referrer"
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                          />
                          <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
                            <span className="opacity-0 group-hover:opacity-100 bg-black/70 text-white text-[11px] px-2.5 py-1 rounded-full font-mono transition-opacity">
                              Zoom & View
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-400 italic">No photos recorded for this wearing session.</p>
                  )}
                </div>

                {/* Tags & Metadata */}
                <div className="p-5 sm:p-6 rounded-3xl bg-white border border-slate-200/80 shadow-xs flex flex-wrap items-center justify-between gap-4">
                  <div className="flex items-center gap-2 flex-wrap">
                    <Tag className="w-4 h-4 text-purple-600" />
                    {report['tags'] && report['tags'].length > 0 ? (
                      report['tags'].map(tag => (
                        <span
                          key={tag}
                          className="px-2.5 py-1 rounded-full text-xs font-semibold bg-purple-50 text-purple-700 border border-purple-200"
                        >
                          {tag}
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-slate-400 italic">No tags</span>
                    )}
                  </div>

                  <span className="text-xs text-slate-400 font-mono">
                    Report ID: {report.id}
                  </span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Delete Confirmation Modal */}
        {showDeleteConfirm && (
          <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl p-6 max-w-sm w-full space-y-4 shadow-2xl border border-slate-200 animate-in fade-in zoom-in-95">
              <div className="flex items-center gap-3 text-rose-600">
                <div className="p-2.5 rounded-2xl bg-rose-100">
                  <AlertTriangle className="w-6 h-6" />
                </div>
                <div>
                  <h4 className="font-bold text-base text-slate-900">Delete Wearing Report?</h4>
                  <p className="text-xs text-slate-500">This action cannot be undone.</p>
                </div>
              </div>

              <p className="text-xs text-slate-600 leading-relaxed">
                Are you sure you want to delete <strong className="text-slate-900">"{report['Name']}"</strong>?
                This report and its wear records will be removed from your wardrobe and Firestore.
              </p>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowDeleteConfirm(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleDelete}
                  disabled={isDeleting}
                  className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-all shadow-xs disabled:opacity-50"
                >
                  {isDeleting ? 'Deleting...' : 'Confirm Delete'}
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
      </div>
    </>
  );
};
