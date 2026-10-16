import React, { useState, useRef, useMemo } from 'react';
import { ClothesItem, PictureAttachment } from '../types';
import { dataService, processImageFile } from '../services/dataService';
import { PreviousEntriesModal, PreviousEntryItem } from './PreviousEntriesModal';
import {
  X, Check, Clock, Plus, Shirt, Calendar, Sparkles, Search, History,
  Upload, Trash2, Camera, ChevronDown, CheckCircle2, AlertCircle
} from 'lucide-react';

interface SelectedGarmentConfig {
  id: string;
  durationHours: number;
  hours: number;
  mins: number;
  washStated: 'wash' | 'need wash';
}

interface LogOutfitModalProps {
  clothes: ClothesItem[];
  isOpen: boolean;
  onClose: () => void;
  onSaveReport: (params: {
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
    'Pictures'?: PictureAttachment[];
  }) => void;
}

export const LogOutfitModal: React.FC<LogOutfitModalProps> = ({
  clothes,
  isOpen,
  onClose,
  onSaveReport
}) => {
  if (!isOpen) return null;

  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [sessionTitle, setSessionTitle] = useState('');
  
  // Selected garments with independent duration & washing status
  const [selectedGarments, setSelectedGarments] = useState<SelectedGarmentConfig[]>([]);

  // Search & Filter state for clothes picker
  const [clothesSearch, setClothesSearch] = useState('');
  const [clothesCategory, setClothesCategory] = useState('All');
  const [isPickerDropdownOpen, setIsPickerDropdownOpen] = useState(false);

  // Sensory database fields
  const [shoulderFeel, setShoulderFeel] = useState('relaxed and comfortable');
  const [midBreast, setMidBreast] = useState('good support, no digging');
  const [nightMidBreast, setNightMidBreast] = useState('');
  const [sweat, setSweat] = useState('low / dry');
  const [nightSweat, setNightSweat] = useState('');
  const [legFeel, setLegFeel] = useState('free movement');
  const [nightLegFeel, setNightLegFeel] = useState('');
  const [reportDetail, setReportDetail] = useState('');
  const [pictures, setPictures] = useState<PictureAttachment[]>([]);
  const [uploadingPic, setUploadingPic] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Previous Values Picker Config
  const [previousModalConfig, setPreviousModalConfig] = useState<{
    isOpen: boolean;
    fieldName: string;
    fieldLabel: string;
    entries: PreviousEntryItem[];
    onSelect: (val: string) => void;
  } | null>(null);

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

  const categories = ['All', 'Dress', 'bra', 'Tshirt', 'Pant', 'skirt', 'sweater', 'jacket'];

  // Fast filtered clothes list for the picker dropdown
  const filteredAvailableClothes = useMemo(() => {
    const q = clothesSearch.toLowerCase().trim();
    return clothes.filter(c => {
      const type = (c['Clothes Type'] || c.clothesType || '').toLowerCase();
      const name = (c['Name'] || c.name || '').toLowerCase();
      const material = (c['material'] || '').toLowerCase();

      const matchesCat = clothesCategory === 'All' || type === clothesCategory.toLowerCase();
      const matchesSearch = !q || name.includes(q) || material.includes(q) || type.includes(q);
      return matchesCat && matchesSearch;
    });
  }, [clothes, clothesSearch, clothesCategory]);

  const handleAddGarment = (garmentId: string) => {
    if (!selectedGarments.some(g => g.id === garmentId)) {
      setSelectedGarments(prev => [
        ...prev,
        { id: garmentId, durationHours: 4, hours: 4, mins: 0, washStated: 'need wash' }
      ]);
    }
    setClothesSearch('');
    setIsPickerDropdownOpen(false);
  };

  const handleRemoveGarment = (garmentId: string) => {
    setSelectedGarments(prev => prev.filter(g => g.id !== garmentId));
  };

  const handleUpdateDurationPreset = (garmentId: string, hours: number) => {
    setSelectedGarments(prev =>
      prev.map(g =>
        g.id === garmentId
          ? { ...g, hours, mins: 0, durationHours: hours }
          : g
      )
    );
  };

  const handleUpdateHoursMins = (garmentId: string, hours: number, mins: number) => {
    const validHours = Math.max(0, hours);
    const validMins = Math.max(0, Math.min(59, mins));
    const totalDecimal = validHours + (validMins / 60);
    setSelectedGarments(prev =>
      prev.map(g =>
        g.id === garmentId
          ? { ...g, hours: validHours, mins: validMins, durationHours: totalDecimal }
          : g
      )
    );
  };

  const handleToggleWashStated = (garmentId: string) => {
    setSelectedGarments(prev =>
      prev.map(g =>
        g.id === garmentId
          ? { ...g, washStated: g.washStated === 'need wash' ? 'wash' : 'need wash' }
          : g
      )
    );
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploadingPic(true);
    try {
      const newPics: PictureAttachment[] = [];
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const processed = await processImageFile(file);
        newPics.push(processed);
      }
      setPictures(prev => [...prev, ...newPics]);
    } catch (err) {
      console.error('Failed to process image:', err);
      alert('Error uploading picture. Please try another image file.');
    } finally {
      setUploadingPic(false);
      if (e.target) e.target.value = '';
    }
  };

  const handleRemovePhoto = (picId: string) => {
    setPictures(prev => prev.filter(p => p.id !== picId));
  };

  const handleSave = () => {
    if (selectedGarments.length === 0) {
      alert('Please select at least one garment worn today.');
      return;
    }

    const defaultTitle = `${date} wearing report`;

    onSaveReport({
      'Name': sessionTitle.trim() || defaultTitle,
      'wear date': new Date(date).toISOString(),
      clothesItems: selectedGarments.map(g => ({
        clothesId: g.id,
        durationHours: g.durationHours,
        washStated: g.washStated
      })),
      'shoulder feel': shoulderFeel,
      'mid breast': midBreast,
      'night mid breast': nightMidBreast,
      'sweat': sweat,
      'night sweat': nightSweat,
      'leg feel': legFeel,
      'night leg feel': nightLegFeel,
      'report detail': reportDetail,
      'Pictures': pictures
    });

    onClose();
  };

  return (
    <>
      <div
        id="log-outfit-backdrop"
        className="fixed inset-0 z-50 bg-black/75 backdrop-blur-xs flex items-center justify-center p-0 sm:p-4 pointer-events-auto"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div
          id="log-outfit-modal"
          onClick={(e) => e.stopPropagation()}
          className="w-full h-full sm:max-w-3xl sm:h-[94vh] bg-white sm:rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col animate-in fade-in duration-200"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 bg-slate-50/80 shrink-0">
            <div className="flex items-center gap-2">
              <span className="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                <Sparkles className="w-4 h-4" />
              </span>
              <h3 className="text-base font-bold text-slate-900">Log Wearing Report & Session</h3>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors"
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Scrollable Content */}
          <div className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-6">
            {/* Wear Date & Name */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                  wear date
                </label>
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                />
              </div>

              <div>
                <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                  Name (Report Title)
                </label>
                <input
                  type="text"
                  placeholder={`e.g. ${date} wearing report`}
                  value={sessionTitle}
                  onChange={(e) => setSessionTitle(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                />
              </div>
            </div>

            {/* Optimized Clothes Picker & Multi-Garment Configuration */}
            <div className="p-4 rounded-2xl bg-purple-50/40 border border-purple-100 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <h4 className="text-xs font-mono font-bold uppercase text-purple-900 tracking-wider">
                    Outfit Clothes & Individual Wash Status ({selectedGarments.length})
                  </h4>
                  <p className="text-[11px] text-purple-700/80">
                    Each garment in your report tracks its own duration and wash state.
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() => setIsPickerDropdownOpen(!isPickerDropdownOpen)}
                  className="inline-flex items-center justify-center gap-1.5 px-3.5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs shrink-0"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add Garment to Outfit</span>
                </button>
              </div>

              {/* Fast Clothes Search & Dropdown Picker */}
              {isPickerDropdownOpen && (
                <div className="p-3.5 bg-white rounded-2xl border border-purple-200 shadow-lg space-y-3 animate-in fade-in slide-in-from-top-2 duration-150">
                  <div className="flex items-center justify-between pb-1 border-b border-slate-100">
                    <span className="text-xs font-bold text-slate-800">
                      Search &amp; Pick from Closet ({clothes.length} total)
                    </span>
                    <button
                      type="button"
                      onClick={() => setIsPickerDropdownOpen(false)}
                      className="p-1 rounded-full text-slate-400 hover:text-slate-600"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  {/* Category Pills */}
                  <div className="flex flex-wrap gap-1">
                    {categories.map(cat => (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => setClothesCategory(cat)}
                        className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors ${
                          clothesCategory === cat
                            ? 'bg-purple-700 text-white'
                            : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                        }`}
                      >
                        {cat}
                      </button>
                    ))}
                  </div>

                  {/* Search Input */}
                  <div className="relative">
                    <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      placeholder="Type garment name or material..."
                      value={clothesSearch}
                      onChange={(e) => setClothesSearch(e.target.value)}
                      autoFocus
                      className="w-full pl-9 pr-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                    />
                  </div>

                  {/* Search Results Dropdown List */}
                  <div className="max-h-56 overflow-y-auto divide-y divide-slate-100 border border-slate-100 rounded-xl">
                    {filteredAvailableClothes.length > 0 ? (
                      filteredAvailableClothes.map(c => {
                        const isAlreadySelected = selectedGarments.some(g => g.id === c.id);
                        return (
                          <div
                            key={c.id}
                            onClick={() => !isAlreadySelected && handleAddGarment(c.id)}
                            className={`flex items-center justify-between p-2.5 transition-colors ${
                              isAlreadySelected
                                ? 'bg-purple-50/50 opacity-60 cursor-default'
                                : 'hover:bg-purple-50/80 cursor-pointer active:bg-purple-100'
                            }`}
                          >
                            <div className="min-w-0 pr-2">
                              <span className="text-xs font-semibold text-slate-900 block truncate">
                                {c['Name']}
                              </span>
                              <span className="text-[11px] text-slate-500">
                                {c['Clothes Type']} · {c['material'] || 'Chiffon'}
                              </span>
                            </div>

                            {isAlreadySelected ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold text-purple-700 shrink-0">
                                <Check className="w-3.5 h-3.5" /> Added
                              </span>
                            ) : (
                              <span className="px-2.5 py-1 rounded-lg bg-purple-100 text-purple-700 text-[11px] font-bold shrink-0">
                                + Add
                              </span>
                            )}
                          </div>
                        );
                      })
                    ) : (
                      <div className="p-4 text-center text-xs text-slate-400 italic">
                        No garments match "{clothesSearch}"
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Selected Garments List with Individual Duration & Washing State */}
              {selectedGarments.length > 0 ? (
                <div className="space-y-2.5">
                  {selectedGarments.map((item, idx) => {
                    const garment = clothes.find(c => c.id === item.id);
                    if (!garment) return null;

                    return (
                      <div
                        key={item.id}
                        className="p-3.5 rounded-2xl bg-white border border-purple-200/90 shadow-2xs flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        {/* Garment details */}
                        <div className="space-y-0.5 min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-800 text-[10px] font-bold">
                              {garment['Clothes Type']}
                            </span>
                            <span className="text-xs font-bold text-slate-900 truncate">
                              {garment['Name']}
                            </span>
                          </div>
                          <p className="text-[11px] text-slate-500 truncate">
                            {garment['material']} {garment['thick clothes'] ? `· ${garment['thick clothes']}` : ''}
                          </p>
                        </div>

                        {/* Individual Duration & Wash State Controls */}
                        <div className="flex items-center gap-2.5 flex-wrap shrink-0">
                          {/* Own Duration Control: Exact Key-In [15] hr : [11] min */}
                          <div className="flex items-center gap-1.5 bg-slate-100 p-1.5 rounded-xl border border-slate-200">
                            <Clock className="w-3.5 h-3.5 text-purple-600 ml-1 shrink-0" />
                            {/* Preset Buttons */}
                            <div className="hidden sm:flex items-center gap-1">
                              {[2, 4, 8, 12].map(hrs => (
                                <button
                                  key={hrs}
                                  type="button"
                                  onClick={() => handleUpdateDurationPreset(item.id, hrs)}
                                  className={`px-1.5 py-0.5 rounded-md text-[10px] font-bold transition-all ${
                                    item.hours === hrs && item.mins === 0
                                      ? 'bg-purple-700 text-white shadow-2xs'
                                      : 'text-slate-600 hover:bg-slate-200'
                                  }`}
                                >
                                  {hrs}h
                                </button>
                              ))}
                            </div>

                            {/* Direct Key-In: Hours & Minutes */}
                            <div className="flex items-center gap-1 bg-white px-2 py-0.5 rounded-lg border border-slate-200 text-xs shadow-2xs">
                              <input
                                type="number"
                                min="0"
                                max="99"
                                value={item.hours}
                                onChange={(e) =>
                                  handleUpdateHoursMins(item.id, parseInt(e.target.value) || 0, item.mins)
                                }
                                className="w-10 text-center font-bold text-slate-900 bg-slate-50 rounded py-0.5"
                                title="Hours worn"
                              />
                              <span className="font-semibold text-slate-500">hr</span>
                              <span className="text-slate-400 font-bold">:</span>
                              <input
                                type="number"
                                min="0"
                                max="59"
                                value={item.mins}
                                onChange={(e) =>
                                  handleUpdateHoursMins(item.id, item.hours, parseInt(e.target.value) || 0)
                                }
                                className="w-10 text-center font-bold text-slate-900 bg-slate-50 rounded py-0.5"
                                title="Minutes worn"
                              />
                              <span className="font-semibold text-slate-500">min</span>
                            </div>
                          </div>

                          {/* Own Wash Stated Toggle */}
                          <button
                            type="button"
                            onClick={() => handleToggleWashStated(item.id)}
                            className={`inline-flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                              item.washStated === 'need wash'
                                ? 'bg-amber-100 text-amber-800 border border-amber-300'
                                : 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                            }`}
                          >
                            {item.washStated === 'need wash' ? (
                              <>
                                <AlertCircle className="w-3.5 h-3.5" />
                                <span>need wash</span>
                              </>
                            ) : (
                              <>
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                <span>wash</span>
                              </>
                            )}
                          </button>

                          {/* Remove from session */}
                          <button
                            type="button"
                            onClick={() => handleRemoveGarment(item.id)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors"
                            title="Remove from outfit"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div
                  onClick={() => setIsPickerDropdownOpen(true)}
                  className="p-5 border-2 border-dashed border-purple-200 rounded-2xl bg-white text-center cursor-pointer hover:border-purple-400 transition-colors"
                >
                  <Shirt className="w-8 h-8 text-purple-300 mx-auto mb-1" />
                  <p className="text-xs font-semibold text-purple-900">
                    No clothes added yet
                  </p>
                  <p className="text-[11px] text-purple-600/80">
                    Click "Add Garment to Outfit" to select the clothes you wore
                  </p>
                </div>
              )}
            </div>

            {/* Sensory Fields with Previous Buttons */}
            <div className="p-4 sm:p-5 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-4">
              <h4 className="text-xs font-bold text-slate-700 uppercase font-mono tracking-wider">
                Sensory &amp; Comfort Review
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* shoulder feel */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">shoulder feel</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('shoulder feel', 'Shoulder Feel', setShoulderFeel)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={shoulderFeel}
                    onChange={(e) => setShoulderFeel(e.target.value)}
                    placeholder="e.g. relaxed and comfortable"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* mid breast */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">mid breast</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('mid breast', 'Mid Breast', setMidBreast)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={midBreast}
                    onChange={(e) => setMidBreast(e.target.value)}
                    placeholder="e.g. good support, no digging"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* night mid breast */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">night mid breast</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('night mid breast', 'Night Mid Breast', setNightMidBreast)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={nightMidBreast}
                    onChange={(e) => setNightMidBreast(e.target.value)}
                    placeholder="e.g. comfortable for evening"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* sweat */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">sweat</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('sweat', 'Sweat level', setSweat)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={sweat}
                    onChange={(e) => setSweat(e.target.value)}
                    placeholder="e.g. low / dry"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* night sweat */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">night sweat</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('night sweat', 'Night sweat', setNightSweat)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={nightSweat}
                    onChange={(e) => setNightSweat(e.target.value)}
                    placeholder="e.g. dry and pleasant"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* leg feel */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">leg feel</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('leg feel', 'Leg feel', setLegFeel)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={legFeel}
                    onChange={(e) => setLegFeel(e.target.value)}
                    placeholder="e.g. free movement"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* night leg feel */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">night leg feel</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('night leg feel', 'Night leg feel', setNightLegFeel)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={nightLegFeel}
                    onChange={(e) => setNightLegFeel(e.target.value)}
                    placeholder="e.g. airy and easy"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* report detail */}
                <div className="sm:col-span-2">
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">report detail</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('report detail', 'Report detail', setReportDetail)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 shadow-2xs"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <textarea
                    rows={2}
                    value={reportDetail}
                    onChange={(e) => setReportDetail(e.target.value)}
                    placeholder="Comprehensive wearing notes, weather, activities..."
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                  />
                </div>
              </div>
            </div>

            {/* Photos Upload for Report */}
            <div className="p-4 sm:p-5 rounded-2xl bg-white border border-slate-200 space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1">
                  <Camera className="w-3.5 h-3.5 text-purple-600" />
                  Outfit of the Day Photos ({pictures.length})
                </label>

                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadingPic}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs disabled:opacity-60"
                >
                  <Upload className="w-3.5 h-3.5" />
                  <span>{uploadingPic ? 'Uploading...' : 'Attach Photo'}</span>
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

              {pictures.length > 0 && (
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
                  {pictures.map((pic) => (
                    <div
                      key={pic.id}
                      className="group relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-100 aspect-square shadow-2xs"
                    >
                      <img
                        src={pic.smThumbnailUrl || pic.presignedUrl || (pic as any).dataUrl}
                        alt={pic.name || 'Outfit photo'}
                        className="w-full h-full object-cover"
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
              )}
            </div>
          </div>

          {/* Modal Footer */}
          <div className="p-4 border-t border-slate-100 bg-slate-50 flex items-center justify-between shrink-0">
            <span className="text-xs text-slate-500 font-medium">
              {selectedGarments.length} garment{selectedGarments.length === 1 ? '' : 's'} chosen
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSave}
                disabled={selectedGarments.length === 0}
                className="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white text-xs font-bold transition-all shadow-xs"
              >
                Save Wearing Report
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Previous Values Picker Modal */}
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
    </>
  );
};
