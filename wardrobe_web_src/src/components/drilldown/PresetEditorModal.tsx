import React, { useState } from 'react';
import { ReportPreset } from '../../types';
import {
  AVAILABLE_REPORT_FIELDS,
  ReportFieldMeta,
  reportPresetService
} from '../../services/reportPresetService';
import {
  X,
  Plus,
  Trash2,
  ArrowUp,
  ArrowDown,
  ArrowUpDown,
  Check,
  RotateCcw,
  Sparkles,
  SlidersHorizontal,
  BookmarkPlus,
  Layers
} from 'lucide-react';

interface PresetEditorModalProps {
  isOpen: boolean;
  onClose: () => void;
  preset: ReportPreset;
  onSavePreset: (savedPreset: ReportPreset) => void;
  onDeletePreset?: (presetId: string) => void;
  onResetDefaults?: () => void;
}

export const PresetEditorModal: React.FC<PresetEditorModalProps> = ({
  isOpen,
  onClose,
  preset,
  onSavePreset,
  onDeletePreset,
  onResetDefaults
}) => {
  if (!isOpen) return null;

  const [name, setName] = useState(preset.name);
  const [description, setDescription] = useState(preset.description || '');
  const [groupTiers, setGroupTiers] = useState<string[]>([...preset.groupTiers]);
  const [sortField, setSortField] = useState(preset.sortField);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>(preset.sortOrder);
  const [visibleFields, setVisibleFields] = useState<string[]>([...preset.visibleFields]);
  const [fieldToAdd, setFieldToAdd] = useState('');

  // Handle tier reordering
  const handleMoveTier = (index: number, direction: 'up' | 'down') => {
    const newTiers = [...groupTiers];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= newTiers.length) return;
    const temp = newTiers[index];
    newTiers[index] = newTiers[targetIndex];
    newTiers[targetIndex] = temp;
    setGroupTiers(newTiers);
  };

  const handleRemoveTier = (index: number) => {
    if (groupTiers.length <= 1) return; // keep at least 1 tier
    setGroupTiers(groupTiers.filter((_, i) => i !== index));
  };

  const handleAddTier = () => {
    if (!fieldToAdd) return;
    if (groupTiers.includes(fieldToAdd)) return;
    if (groupTiers.length >= 5) return; // max 5 tiers
    setGroupTiers([...groupTiers, fieldToAdd]);
    setFieldToAdd('');
  };

  const handleToggleVisibleField = (key: string) => {
    if (visibleFields.includes(key)) {
      if (visibleFields.length > 1) {
        setVisibleFields(visibleFields.filter((k) => k !== key));
      }
    } else {
      setVisibleFields([...visibleFields, key]);
    }
  };

  const handleSaveCurrent = () => {
    const updated: ReportPreset = {
      ...preset,
      name: name.trim() || 'Custom Drill Preset',
      description: description.trim(),
      groupTiers,
      sortField,
      sortOrder,
      visibleFields
    };
    onSavePreset(updated);
    onClose();
  };

  const handleSaveAsNew = () => {
    const newPreset: ReportPreset = {
      id: `custom-preset-${Date.now()}`,
      name: name.trim() ? `${name.trim()} (Copy)` : 'Custom Preset',
      description: description.trim(),
      isCustom: true,
      groupTiers,
      sortField,
      sortOrder,
      visibleFields
    };
    onSavePreset(newPreset);
    onClose();
  };

  // Group available fields by category for clean presentation
  const availableFieldsMap = new Map(AVAILABLE_REPORT_FIELDS.map((f) => [f.key, f]));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
      <div
        className="relative w-full max-w-2xl bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[92vh] my-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between bg-gradient-to-r from-slate-50 via-white to-indigo-50/40">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-xs">
              <SlidersHorizontal className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 text-base">Customize Report Preset</h3>
              <p className="text-xs text-slate-500">
                Adjust multi-tier grouping, sorting rule, and record card fields
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scrollable Body */}
        <div className="p-5 overflow-y-auto space-y-6 text-sm text-slate-700">
          {/* Preset Name & Description */}
          <div className="space-y-3">
            <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider">
              Preset Information
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Preset Title
                </span>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Summer Climate & Heat"
                  className="w-full px-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
                />
              </div>
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Description
                </span>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g. Group by category and thickness"
                  className="w-full px-3.5 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
                />
              </div>
            </div>
          </div>

          {/* Multi-Tier Grouping Section */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
                <Layers className="w-3.5 h-3.5 text-indigo-500" />
                <span>Drill-Down Grouping Hierarchy (Max 5 Tiers)</span>
              </label>
              <span className="text-[11px] text-slate-400 font-mono">
                {groupTiers.length}/5 tiers
              </span>
            </div>

            <p className="text-xs text-slate-500">
              When drilling into any tier branch, unselected sibling branches collapse immediately to focus on the selected path.
            </p>

            {/* Current Tiers List */}
            <div className="space-y-2">
              {groupTiers.map((tierKey, index) => {
                const meta = availableFieldsMap.get(tierKey);
                return (
                  <div
                    key={`${tierKey}-${index}`}
                    className="flex items-center justify-between p-2.5 bg-slate-50 border border-slate-200/80 rounded-2xl group hover:border-indigo-200 transition-colors"
                  >
                    <div className="flex items-center gap-2.5">
                      <div className="w-6 h-6 rounded-lg bg-indigo-100 text-indigo-700 text-xs font-bold flex items-center justify-center shrink-0">
                        {index + 1}
                      </div>
                      <div>
                        <div className="font-semibold text-slate-800 text-xs sm:text-sm">
                          {meta?.label || tierKey}
                        </div>
                        <div className="text-[10px] text-slate-400 font-mono">
                          Key: {tierKey}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1">
                      <button
                        type="button"
                        onClick={() => handleMoveTier(index, 'up')}
                        disabled={index === 0}
                        title="Move Up"
                        className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 disabled:opacity-30 disabled:pointer-events-none transition-colors"
                      >
                        <ArrowUp className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleMoveTier(index, 'down')}
                        disabled={index === groupTiers.length - 1}
                        title="Move Down"
                        className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 disabled:opacity-30 disabled:pointer-events-none transition-colors"
                      >
                        <ArrowDown className="w-3.5 h-3.5" />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleRemoveTier(index)}
                        disabled={groupTiers.length <= 1}
                        title="Remove Tier"
                        className="p-1 rounded-lg text-rose-400 hover:text-rose-700 hover:bg-rose-50 disabled:opacity-30 disabled:pointer-events-none transition-colors ml-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Add New Tier Selector */}
            {groupTiers.length < 5 && (
              <div className="flex items-center gap-2 pt-1">
                <select
                  value={fieldToAdd}
                  onChange={(e) => setFieldToAdd(e.target.value)}
                  className="flex-1 px-3 py-2 text-xs sm:text-sm bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium text-slate-700"
                >
                  <option value="">Select field to add as next subgroup tier...</option>
                  {AVAILABLE_REPORT_FIELDS.filter((f) => !groupTiers.includes(f.key)).map((f) => (
                    <option key={f.key} value={f.key}>
                      {f.label} ({f.category})
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={handleAddTier}
                  disabled={!fieldToAdd}
                  className="flex items-center gap-1 px-3.5 py-2 text-xs font-semibold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 disabled:opacity-40 disabled:pointer-events-none rounded-xl border border-indigo-200 transition-colors shrink-0"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add Tier</span>
                </button>
              </div>
            )}
          </div>

          {/* Record Sorting Configuration */}
          <div className="space-y-3">
            <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
              <ArrowUpDown className="w-3.5 h-3.5 text-indigo-500" />
              <span>Record List Sorting Rule</span>
            </label>
            <p className="text-xs text-slate-500">
              Controls how garments are sorted within leaf branches and record listings.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 items-center">
              <div className="sm:col-span-2">
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Sort Records By Field
                </span>
                <select
                  value={sortField}
                  onChange={(e) => setSortField(e.target.value)}
                  className="w-full px-3 py-2 text-xs sm:text-sm bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium text-slate-700"
                >
                  {AVAILABLE_REPORT_FIELDS.map((f) => (
                    <option key={f.key} value={f.key}>
                      {f.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Direction
                </span>
                <button
                  type="button"
                  onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                  className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs sm:text-sm font-semibold bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl transition-colors text-slate-800"
                >
                  {sortOrder === 'asc' ? (
                    <>
                      <ArrowUp className="w-3.5 h-3.5 text-indigo-600" />
                      <span>Ascending (A-Z, 0-9)</span>
                    </>
                  ) : (
                    <>
                      <ArrowDown className="w-3.5 h-3.5 text-indigo-600" />
                      <span>Descending (Z-A, 9-0)</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Visible Metadata Fields on Cards */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider">
                Visible Card Badges & Metadata ({visibleFields.length} selected)
              </label>
              <button
                type="button"
                onClick={() =>
                  setVisibleFields(
                    visibleFields.length === AVAILABLE_REPORT_FIELDS.length
                      ? ['Clothes Type', 'material', 'thick clothes']
                      : AVAILABLE_REPORT_FIELDS.map((f) => f.key)
                  )
                }
                className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800 transition-colors"
              >
                {visibleFields.length === AVAILABLE_REPORT_FIELDS.length ? 'Minimize' : 'Select All'}
              </button>
            </div>
            <p className="text-xs text-slate-500">
              Selected fields will appear as formatted info badges on garment cards in this preset.
            </p>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {AVAILABLE_REPORT_FIELDS.map((field) => {
                const isSelected = visibleFields.includes(field.key);
                return (
                  <button
                    type="button"
                    key={field.key}
                    onClick={() => handleToggleVisibleField(field.key)}
                    className={`flex items-center gap-2 p-2 rounded-xl text-left text-xs transition-all border ${
                      isSelected
                        ? 'bg-indigo-50/80 border-indigo-300 text-indigo-900 font-semibold shadow-2xs'
                        : 'bg-white border-slate-200/80 text-slate-600 hover:border-slate-300'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-md flex items-center justify-center shrink-0 border ${
                        isSelected
                          ? 'bg-indigo-600 border-indigo-600 text-white'
                          : 'border-slate-300 bg-white'
                      }`}
                    >
                      {isSelected && <Check className="w-2.5 h-2.5 stroke-[3]" />}
                    </div>
                    <span className="truncate">{field.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Modal Footer Actions */}
        <div className="px-5 py-3.5 border-t border-slate-100 bg-slate-50 flex flex-wrap items-center justify-between gap-2.5">
          <div className="flex items-center gap-2">
            {onResetDefaults && (
              <button
                type="button"
                onClick={() => {
                  if (window.confirm('Reset all presets back to system defaults?')) {
                    onResetDefaults();
                    onClose();
                  }
                }}
                className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-slate-500 hover:text-slate-800 hover:bg-slate-200/60 rounded-xl transition-colors"
                title="Reset to original presets"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reset Defaults</span>
              </button>
            )}

            {preset.isCustom && onDeletePreset && (
              <button
                type="button"
                onClick={() => {
                  if (window.confirm(`Delete custom preset "${preset.name}"?`)) {
                    onDeletePreset(preset.id);
                    onClose();
                  }
                }}
                className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-rose-600 hover:text-rose-800 hover:bg-rose-50 rounded-xl transition-colors"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Delete Preset</span>
              </button>
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleSaveAsNew}
              className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold bg-white text-indigo-700 hover:bg-indigo-50/60 border border-indigo-200 rounded-xl shadow-2xs transition-colors"
            >
              <BookmarkPlus className="w-3.5 h-3.5" />
              <span>Save As New Preset</span>
            </button>

            <button
              type="button"
              onClick={handleSaveCurrent}
              className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold bg-indigo-600 text-white hover:bg-indigo-700 rounded-xl shadow-xs transition-colors"
            >
              <Check className="w-3.5 h-3.5" />
              <span>Apply &amp; Save</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
