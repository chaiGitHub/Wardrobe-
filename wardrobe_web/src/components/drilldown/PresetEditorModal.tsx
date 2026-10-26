import React, { useState } from 'react';
import { ReportPreset } from '../../types';
import {
  AVAILABLE_REPORT_FIELDS,
  ReportFieldMeta,
  reportPresetService
} from '../../services/reportPresetService';
import { ConfirmModal } from '../ConfirmModal';
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
  Layers,
  ArrowLeft
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
  const [confirmConfig, setConfirmConfig] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    confirmLabel: string;
    variant: 'primary' | 'danger' | 'warning';
    onConfirm: () => void;
  } | null>(null);

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
    if (groupTiers.length >= 10) return; // max 10 tiers
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

  // Group available fields by category for clean presentation (dynamic from preset service)
  const allFields = reportPresetService.getAllAvailableFields();
  const availableFieldsMap = new Map(allFields.map((f) => [f.key, f]));

  return (
    <div className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto">
      {/* Modal Header */}
      <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200/80 flex items-center justify-between bg-white shrink-0 shadow-2xs w-full max-w-full overflow-x-hidden">
        <div className="flex items-center gap-2.5 min-w-0">
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 sm:p-2 -ml-1 sm:-ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
            title="Back"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="w-9 h-9 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-xs shrink-0">
            <SlidersHorizontal className="w-4 h-4" />
          </div>
          <div className="min-w-0">
            <h3 className="font-bold text-slate-900 text-sm sm:text-base truncate">Customize Report Preset</h3>
            <p className="text-[11px] sm:text-xs text-slate-500 truncate hidden xs:block">
              Adjust multi-tier grouping, sorting rule, and record card fields
            </p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 sm:p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Modal Scrollable Body */}
      <div className="flex-1 p-3.5 sm:p-8 overflow-y-auto overflow-x-hidden bg-slate-50/50 w-full max-w-full">
        <div className="max-w-3xl mx-auto space-y-6 bg-white p-5 sm:p-8 rounded-3xl border border-slate-200/90 shadow-xs text-sm text-slate-700">
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
                <span>Drill-Down Grouping Hierarchy (Max 10 Tiers)</span>
              </label>
              <span className="text-[11px] text-slate-400 font-mono">
                {groupTiers.length}/10 tiers
              </span>
            </div>

            <p className="text-xs text-slate-500">
              When drilling into any tier branch, unselected sibling branches collapse immediately to focus on the selected path.
            </p>

            {/* Current Tiers List */}
            <div className="space-y-2.5">
              {groupTiers.map((tierKey, index) => {
                const meta = availableFieldsMap.get(tierKey);
                return (
                  <div
                    key={`${tierKey}-${index}`}
                    className="p-3.5 bg-slate-50 border border-slate-200/90 rounded-2xl hover:border-indigo-200 transition-colors shadow-2xs space-y-3"
                  >
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="w-7 h-7 rounded-xl bg-indigo-600 text-white text-xs font-bold flex items-center justify-center shrink-0 shadow-2xs mt-0.5">
                        {index + 1}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="font-bold text-slate-900 text-sm">
                          {meta?.label || tierKey}
                        </div>
                        {meta?.description && (
                          <div className="text-xs text-slate-600 mt-0.5 leading-snug">
                            {meta.description}
                          </div>
                        )}
                        <div className="text-[11px] text-slate-500 font-mono mt-1 flex items-center gap-1.5 flex-wrap">
                          <span>Database Key:</span>
                          <code className="bg-slate-200/80 text-slate-700 px-1.5 py-0.5 rounded text-[11px] font-semibold">
                            {tierKey}
                          </code>
                        </div>
                      </div>
                    </div>

                    {/* Large, finger-friendly Up, Down, and Delete action buttons */}
                    <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-200/70">
                      <button
                        type="button"
                        onClick={() => handleMoveTier(index, 'up')}
                        disabled={index === 0}
                        title="Move Tier Up"
                        className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white text-slate-700 hover:text-indigo-600 hover:bg-indigo-50 border border-slate-200 shadow-2xs disabled:opacity-35 disabled:pointer-events-none transition-all text-xs font-bold active:scale-95 cursor-pointer"
                      >
                        <ArrowUp className="w-4 h-4 text-indigo-600" />
                        <span>Up</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleMoveTier(index, 'down')}
                        disabled={index === groupTiers.length - 1}
                        title="Move Tier Down"
                        className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white text-slate-700 hover:text-indigo-600 hover:bg-indigo-50 border border-slate-200 shadow-2xs disabled:opacity-35 disabled:pointer-events-none transition-all text-xs font-bold active:scale-95 cursor-pointer"
                      >
                        <ArrowDown className="w-4 h-4 text-indigo-600" />
                        <span>Down</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => handleRemoveTier(index)}
                        disabled={groupTiers.length <= 1}
                        title="Delete Subgroup Tier"
                        className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-rose-50 text-rose-700 hover:bg-rose-100 hover:text-rose-800 border border-rose-200 shadow-2xs disabled:opacity-35 disabled:pointer-events-none transition-all text-xs font-bold active:scale-95 cursor-pointer ml-auto sm:ml-0"
                      >
                        <Trash2 className="w-4 h-4 text-rose-600" />
                        <span>Delete</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Add New Tier Selector Card */}
            {groupTiers.length < 10 ? (
              <div className="p-3.5 sm:p-4 bg-indigo-50/70 border border-indigo-200/90 rounded-2xl space-y-2.5 shadow-2xs">
                <div className="flex items-center justify-between text-xs font-bold text-indigo-950">
                  <span className="flex items-center gap-1.5">
                    <Plus className="w-4 h-4 text-indigo-600" />
                    <span>Add Next Subgroup Tier (Level {groupTiers.length + 1})</span>
                  </span>
                  <span className="text-[11px] font-semibold text-indigo-700 font-mono">
                    {groupTiers.length}/10 tiers used
                  </span>
                </div>

                <div className="flex flex-col sm:flex-row gap-2.5 items-stretch">
                  <div className="flex-1 min-w-0">
                    <select
                      value={fieldToAdd}
                      onChange={(e) => setFieldToAdd(e.target.value)}
                      className="w-full min-w-0 px-3.5 py-2.5 text-xs sm:text-sm bg-white border border-indigo-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium text-slate-800 shadow-2xs"
                    >
                      <option value="">Select field to add as next subgroup tier...</option>
                      {allFields.filter((f) => !groupTiers.includes(f.key)).map((f) => (
                        <option key={f.key} value={f.key}>
                          {f.label} — {f.description}
                        </option>
                      ))}
                    </select>
                  </div>

                  <button
                    type="button"
                    onClick={handleAddTier}
                    disabled={!fieldToAdd}
                    className="w-full sm:w-auto px-5 py-2.5 text-xs sm:text-sm font-bold bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-200 disabled:text-slate-400 disabled:border-slate-200 text-white rounded-xl shadow-xs transition-all flex items-center justify-center gap-2 shrink-0 cursor-pointer disabled:cursor-not-allowed active:scale-95"
                  >
                    <Plus className="w-4 h-4" />
                    <span>+ Add Subgroup Tier</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-3 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl text-xs flex items-center gap-2">
                <span>Maximum limit of 10 grouping tiers reached. Remove a tier to add another.</span>
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
                      {f.label} — {f.description}
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
                    title={`${field.label}: ${field.description} (Database Key: ${field.key})`}
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
        <div className="px-4 sm:px-8 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] border-t border-slate-100 bg-slate-50 flex flex-wrap items-center justify-between gap-2.5 w-full max-w-full shadow-xs">
          <div className="flex items-center gap-2">
            {onResetDefaults && (
              <button
                type="button"
                onClick={() => {
                  setConfirmConfig({
                    isOpen: true,
                    title: 'Reset All Presets?',
                    message: 'Are you sure you want to reset all presets back to system defaults? Any custom sorting or groupings will revert.',
                    confirmLabel: 'Yes, Reset Defaults',
                    variant: 'warning',
                    onConfirm: () => {
                      setConfirmConfig(null);
                      onResetDefaults();
                      onClose();
                    }
                  });
                }}
                className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-slate-500 hover:text-slate-800 hover:bg-slate-200/60 rounded-xl transition-colors cursor-pointer"
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
                  setConfirmConfig({
                    isOpen: true,
                    title: `Delete Preset "${preset.name}"?`,
                    message: `Are you sure you want to delete custom preset "${preset.name}"? This cannot be undone.`,
                    confirmLabel: 'Yes, Delete Preset',
                    variant: 'danger',
                    onConfirm: () => {
                      setConfirmConfig(null);
                      onDeletePreset(preset.id);
                      onClose();
                    }
                  });
                }}
                className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-rose-600 hover:text-rose-800 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer"
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
              className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold bg-white text-indigo-700 border border-indigo-200 hover:bg-indigo-50 rounded-2xl shadow-2xs transition-all active:scale-95"
            >
              <BookmarkPlus className="w-3.5 h-3.5 text-indigo-600" />
              <span>Save As New</span>
            </button>

            <button
              type="button"
              onClick={handleSaveCurrent}
              className="flex items-center gap-1.5 px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl shadow-xs transition-all active:scale-95"
            >
              <Check className="w-3.5 h-3.5 text-white" />
              <span>Apply &amp; Save</span>
            </button>
          </div>
        </div>
      </div>

      {confirmConfig && (
        <ConfirmModal
          isOpen={confirmConfig.isOpen}
          onClose={() => setConfirmConfig(null)}
          onConfirm={confirmConfig.onConfirm}
          title={confirmConfig.title}
          message={confirmConfig.message}
          confirmLabel={confirmConfig.confirmLabel}
          variant={confirmConfig.variant}
        />
      )}
    </div>
  );
};
