import React, { useState, useRef } from 'react';
import { ClothesItem, PictureAttachment } from '../types';
import { dataService, processImageFile } from '../services/dataService';
import { PreviousEntriesModal, PreviousEntryItem } from './PreviousEntriesModal';
import {
  X, Plus, Sparkles, Shirt, DollarSign, ThermometerSun, ShieldCheck,
  Upload, Trash2, Camera, History, Tag, Image as ImageIcon
} from 'lucide-react';

interface AddGarmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAddGarment: (garment: Omit<ClothesItem, 'id'>) => void;
}

export const AddGarmentModal: React.FC<AddGarmentModalProps> = ({
  isOpen,
  onClose,
  onAddGarment
}) => {
  if (!isOpen) return null;

  const [name, setName] = useState('');
  const [clothesType, setClothesType] = useState('Dress');
  const [material, setMaterial] = useState('soft chiffon');
  const [thickClothes, setThickClothes] = useState('01 not thick clothes');
  const [hotAtNoon, setHotAtNoon] = useState('Not hot');
  const [diaoDai, setDiaoDai] = useState('diao dai');
  const [sitBreastOk, setSitBreastOk] = useState('sit breast shape ok');
  const [sideBreastOk, setSideBreastOk] = useState('side breast shape ok');
  const [centerChest, setCenterChest] = useState('no center chest');
  const [beautiful, setBeautiful] = useState('Beautiful');
  const [beatifulReason, setBeatifulReason] = useState('');
  const [wet, setWet] = useState<'dry' | 'wet' | 'need dry'>('dry');
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
    const historical = dataService.getFieldPreviousValues(fieldKey, 'clothes');
    setPreviousModalConfig({
      isOpen: true,
      fieldName: fieldKey,
      fieldLabel: label,
      entries: historical,
      onSelect: onSelectVal
    });
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
      alert('Please enter a garment name.');
      return;
    }

    const numPrice = price ? parseFloat(price) : undefined;

    onAddGarment({
      'Name': name.trim(),
      'Clothes Type': clothesType,
      'material': material.trim(),
      'thick clothes': thickClothes,
      'Hot at noon': hotAtNoon,
      'diao dai': diaoDai,
      'side breast ok': sideBreastOk,
      'sit breast ok': sitBreastOk,
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
      'tags': tags
    });

    onClose();
  };

  return (
    <>
      <div
        id="add-garment-backdrop"
        className="fixed inset-0 z-50 bg-black/75 backdrop-blur-xs flex items-center justify-center p-0 sm:p-4 pointer-events-auto"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div
          id="add-garment-modal"
          onClick={(e) => e.stopPropagation()}
          className="w-full h-full sm:max-w-2xl sm:h-[94vh] bg-white sm:rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col animate-in fade-in duration-200"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 bg-slate-50/80 shrink-0">
            <div className="flex items-center gap-2">
              <span className="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                <Shirt className="w-4 h-4" />
              </span>
              <h3 className="text-base font-bold text-slate-900">Add Wardrobe Garment</h3>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors"
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form Scrollable Body */}
          <form onSubmit={handleSubmit} className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-6">
            {/* Core Details */}
            <div className="p-4 rounded-2xl bg-purple-50/40 border border-purple-100 space-y-4">
              <h4 className="text-xs font-bold text-purple-900 uppercase font-mono tracking-wider">
                Basic Garment Information
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
                </div>

                <div>
                  <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                    Clothes Type *
                  </label>
                  <select
                    value={clothesType}
                    onChange={(e) => setClothesType(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm text-slate-900 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500/20"
                  >
                    <option value="Dress">Dress</option>
                    <option value="bra">bra</option>
                    <option value="Tshirt">Tshirt</option>
                    <option value="Pant">Pant</option>
                    <option value="skirt">skirt</option>
                    <option value="sweater">sweater</option>
                    <option value="jacket">jacket</option>
                    <option value="Other">Other</option>
                  </select>
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
                </div>
              </div>
            </div>

            {/* Garment Attributes with Previous Buttons */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 space-y-4">
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
                      onClick={() => openPreviousPicker('thick clothes', 'Thick clothes', setThickClothes)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={thickClothes}
                    onChange={(e) => setThickClothes(e.target.value)}
                    placeholder="01 not thick clothes"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
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
                  <input
                    type="text"
                    value={hotAtNoon}
                    onChange={(e) => setHotAtNoon(e.target.value)}
                    placeholder="Not hot"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
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
                  <input
                    type="text"
                    value={diaoDai}
                    onChange={(e) => setDiaoDai(e.target.value)}
                    placeholder="diao dai / not diao dai"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* side breast ok */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">side breast ok</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('side breast ok', 'Side breast ok', setSideBreastOk)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={sideBreastOk}
                    onChange={(e) => setSideBreastOk(e.target.value)}
                    placeholder="side breast shape ok"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* sit breast ok */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">sit breast ok</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('sit breast ok', 'Sit breast ok', setSitBreastOk)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={sitBreastOk}
                    onChange={(e) => setSitBreastOk(e.target.value)}
                    placeholder="sit breast shape ok"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
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
                  <input
                    type="text"
                    value={centerChest}
                    onChange={(e) => setCenterChest(e.target.value)}
                    placeholder="no center chest"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
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
                  <input
                    type="text"
                    value={beautiful}
                    onChange={(e) => setBeautiful(e.target.value)}
                    placeholder="Beautiful"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
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
                  <input
                    type="text"
                    value={beatifulReason}
                    onChange={(e) => setBeatifulReason(e.target.value)}
                    placeholder="flattering fit, vibrant color"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
                </div>

                {/* wet & wet reason */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-xs font-mono font-bold uppercase text-slate-600">wet</label>
                    <button
                      type="button"
                      onClick={() => openPreviousPicker('wet', 'Wet status', (v) => setWet(v as any))}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <select
                    value={wet}
                    onChange={(e) => setWet(e.target.value as any)}
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
                      onClick={() => openPreviousPicker('wet reason', 'Drying reason', setWetReason)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200"
                    >
                      <History className="w-3 h-3 text-purple-600" />
                      Previous
                    </button>
                  </div>
                  <input
                    type="text"
                    value={wetReason}
                    onChange={(e) => setWetReason(e.target.value)}
                    placeholder="quick-drying fabric"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
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
                  <input
                    type="text"
                    value={longDress}
                    onChange={(e) => setLongDress(e.target.value)}
                    placeholder="short dress / long dress"
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white"
                  />
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
                <textarea
                  rows={2}
                  placeholder="Styling notes, fit details..."
                  value={descriptions}
                  onChange={(e) => setDescriptions(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                />
              </div>

              <div>
                <label className="text-xs font-mono font-bold uppercase text-slate-600 block mb-1">
                  last comment or action
                </label>
                <input
                  type="text"
                  placeholder="e.g. Needs gentle cycle"
                  value={lastComment}
                  onChange={(e) => setLastComment(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm"
                />
              </div>
            </div>

            {/* Pictures Upload */}
            <div className="p-4 rounded-2xl bg-white border border-slate-200 space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1">
                  <ImageIcon className="w-3.5 h-3.5 text-purple-600" />
                  Garment Photos ({pictures.length})
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

              {pictures.length > 0 ? (
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  {pictures.map((pic) => (
                    <div
                      key={pic.id}
                      className="group relative rounded-2xl overflow-hidden border border-slate-200 bg-slate-100 aspect-square shadow-2xs"
                    >
                      <img
                        src={pic.smThumbnailUrl || pic.presignedUrl || (pic as any).dataUrl}
                        alt={pic.name || 'Garment photo'}
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
          <div className="p-4 border-t border-slate-100 bg-slate-50 flex items-center justify-end gap-2 shrink-0">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/70 transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              className="px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs"
            >
              Add to Wardrobe
            </button>
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
    </>
  );
};
