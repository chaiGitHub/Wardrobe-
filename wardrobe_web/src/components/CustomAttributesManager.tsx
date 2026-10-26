import React, { useState } from 'react';
import { Plus, Trash2, Edit2, Check, X, Tag, Sparkles } from 'lucide-react';
import { ConfirmModal } from './ConfirmModal';

interface CustomAttributesManagerProps {
  title?: string;
  description?: string;
  attributes: Record<string, any>;
  onAddAttribute: (key: string, value: any) => Promise<void> | void;
  onUpdateAttribute: (key: string, value: any) => Promise<void> | void;
  onDeleteAttribute: (key: string) => Promise<void> | void;
  suggestedKeys?: string[];
  entityType?: 'garment' | 'wearing report';
}

export const CustomAttributesManager: React.FC<CustomAttributesManagerProps> = ({
  title = 'Extra Attributes & Specifications',
  description = 'Add, edit, or remove custom specifications and observational fields.',
  attributes = {},
  onAddAttribute,
  onUpdateAttribute,
  onDeleteAttribute,
  suggestedKeys = [],
  entityType = 'garment'
}) => {
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);

  const entries = Object.entries(attributes || {}).filter(([k]) => {
    // Exclude internal metadata and internal _fieldExplanations from generic attribute list
    return !['id', 'fields', 'createdTime', 'customFields', 'extraAttributes'].includes(k) && !k.startsWith('_');
  });

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanKey = newKey.trim();
    const cleanVal = newValue.trim();
    if (!cleanKey || !cleanVal) return;

    setIsSubmitting(true);
    try {
      await onAddAttribute(cleanKey, cleanVal);
      setNewKey('');
      setNewValue('');
      setShowAddForm(false);
    } catch (err) {
      console.error('Failed to add attribute:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSaveEdit = async (key: string) => {
    setIsSubmitting(true);
    try {
      await onUpdateAttribute(key, editingValue.trim());
      setEditingKey(null);
    } catch (err) {
      console.error('Failed to update attribute:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const [deleteKeyConfirm, setDeleteKeyConfirm] = useState<string | null>(null);

  const confirmDelete = async () => {
    if (!deleteKeyConfirm) return;
    const key = deleteKeyConfirm;
    setDeleteKeyConfirm(null);
    setIsSubmitting(true);
    try {
      await onDeleteAttribute(key);
    } catch (err) {
      console.error('Failed to delete attribute:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-slate-50/80 rounded-2xl p-4 sm:p-5 border border-slate-200 space-y-3.5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
        <div className="flex items-start sm:items-center gap-2.5">
          <div className="w-7 h-7 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold shrink-0 mt-0.5 sm:mt-0">
            <Tag className="w-3.5 h-3.5" />
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h4 className="text-xs font-bold text-slate-900 uppercase font-mono tracking-wider">
                {title}
              </h4>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 font-mono">
                <Sparkles className="w-2.5 h-2.5 text-emerald-600" />
                <span>实时纳入 AI 诊断报告</span>
              </span>
            </div>
            <p className="text-[11px] text-slate-500 mt-0.5">
              {description}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setShowAddForm(!showAddForm)}
          className="inline-flex items-center justify-center gap-1 px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs shadow-xs transition-all active:scale-95 cursor-pointer shrink-0"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Add Attribute</span>
        </button>
      </div>

      {/* Suggested Quick Keys */}
      {showAddForm && suggestedKeys.length > 0 && (
        <div className="flex flex-wrap items-center gap-1.5 p-2.5 bg-purple-50/60 rounded-xl border border-purple-100">
          <span className="text-[10px] font-mono text-purple-700 uppercase font-bold mr-1">
            Suggestions:
          </span>
          {suggestedKeys.map(sKey => (
            <button
              key={sKey}
              type="button"
              onClick={() => setNewKey(sKey)}
              className="px-2 py-0.5 rounded-lg bg-white hover:bg-purple-100 text-[10px] text-purple-900 border border-purple-200 font-medium transition-all cursor-pointer"
            >
              +{sKey}
            </button>
          ))}
        </div>
      )}

      {/* Add Attribute Form */}
      {showAddForm && (
        <form onSubmit={handleAdd} className="p-3.5 bg-white rounded-xl border border-purple-200 shadow-xs space-y-3 animate-in fade-in">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div>
              <label className="text-[10px] font-mono uppercase text-slate-500 font-bold block mb-1">
                Attribute Name (字段名)
              </label>
              <input
                type="text"
                placeholder={entityType === 'garment' ? 'e.g. Color, Fabric Weight, Origin' : 'e.g. Weather, Room Temp, Humidity'}
                value={newKey}
                onChange={(e) => setNewKey(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs focus:ring-2 focus:ring-purple-500 focus:outline-hidden"
                required
              />
            </div>
            <div>
              <label className="text-[10px] font-mono uppercase text-slate-500 font-bold block mb-1">
                Value (取值/详细内容)
              </label>
              <textarea
                rows={2}
                placeholder={entityType === 'garment' ? 'e.g. Blueeeee, 220 GSM, Hand wash only' : 'e.g. Sunny 28°C, AC on high'}
                value={newValue}
                onChange={(e) => setNewValue(e.target.value)}
                className="w-full px-3 py-1.5 rounded-xl border border-slate-300 text-xs focus:ring-2 focus:ring-purple-500 focus:outline-hidden leading-relaxed"
                required
              />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-1 border-t border-slate-100">
            <button
              type="button"
              onClick={() => setShowAddForm(false)}
              className="px-3 py-1 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !newKey.trim() || !newValue.trim()}
              className="px-3.5 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs shadow-xs disabled:opacity-50 cursor-pointer"
            >
              Save Attribute
            </button>
          </div>
        </form>
      )}

      {/* Attributes List */}
      {entries.length === 0 ? (
        <div className="py-4 px-4 rounded-xl bg-white/60 border border-dashed border-slate-200 text-center">
          <p className="text-xs text-slate-400 italic">
            No custom attributes added yet. Click &ldquo;Add Attribute&rdquo; above to record extra specifications.
          </p>
        </div>
      ) : (
        <div className="space-y-2">
          {entries.map(([key, val]) => {
            const isEditingThis = editingKey === key;
            const displayStr = typeof val === 'object' ? JSON.stringify(val, null, 2) : String(val);
            return (
              <div
                key={key}
                className="p-3 bg-white rounded-xl border border-slate-200 hover:border-purple-200 transition-all shadow-2xs space-y-2 min-w-0 w-full"
              >
                <div className="flex items-center justify-between gap-2 min-w-0">
                  <div className="flex items-center gap-1.5 min-w-0">
                    <span className="font-mono font-bold text-purple-950 bg-purple-50 border border-purple-200/80 px-2 py-0.5 rounded-md text-[11px] break-words [word-break:normal] [overflow-wrap:break-word]">
                      {key}:
                    </span>
                    <span className="text-[10px] text-slate-400 font-mono hidden sm:inline">
                      (已纳入 AI 诊断)
                    </span>
                  </div>

                  <div className="flex items-center gap-1 shrink-0">
                    {isEditingThis ? (
                      <>
                        <button
                          type="button"
                          onClick={() => handleSaveEdit(key)}
                          disabled={isSubmitting}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-[11px] font-bold shadow-2xs cursor-pointer transition-colors"
                          title="Save Changes"
                        >
                          <Check className="w-3.5 h-3.5" />
                          <span>Save</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => setEditingKey(null)}
                          className="p-1 rounded-lg hover:bg-slate-100 text-slate-500 transition-colors cursor-pointer"
                          title="Cancel"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          type="button"
                          onClick={() => {
                            setEditingKey(key);
                            setEditingValue(displayStr);
                          }}
                          className="p-1.5 rounded-lg hover:bg-purple-50 text-slate-500 hover:text-purple-700 transition-colors cursor-pointer"
                          title="Edit value"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteKeyConfirm(key)}
                          disabled={isSubmitting}
                          className="p-1.5 rounded-lg hover:bg-rose-50 text-slate-400 hover:text-rose-600 transition-colors cursor-pointer"
                          title="Delete attribute"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </>
                    )}
                  </div>
                </div>

                {isEditingThis ? (
                  <div className="pt-1 space-y-1.5">
                    <label className="text-[10px] font-mono text-purple-700 font-bold block uppercase">
                      编辑字段取值 (Edit Literal Value):
                    </label>
                    <textarea
                      value={editingValue}
                      onChange={(e) => setEditingValue(e.target.value)}
                      rows={Math.min(12, Math.max(4, editingValue.split('\n').length))}
                      className="w-full px-3 py-2.5 rounded-xl border-2 border-purple-400 text-xs sm:text-sm text-slate-900 font-medium leading-relaxed focus:ring-2 focus:ring-purple-500 focus:outline-hidden resize-y min-h-[96px] max-h-[1600px] break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap bg-white shadow-inner"
                      autoFocus
                    />
                    <div className="text-[11px] text-slate-500 flex justify-between items-center">
                      <span>可直接按住右下角向下拉伸文本框</span>
                      <span className="font-mono bg-slate-100 px-1.5 py-0.5 rounded text-[10px]">{editingValue.length} 字符</span>
                    </div>
                  </div>
                ) : (
                  <div className="min-w-0 w-full space-y-1">
                    <span className="text-[10px] font-mono font-bold uppercase text-slate-400 block tracking-wider">
                      真实登记取值 (Literal Value):
                    </span>
                    <div className="text-xs sm:text-sm text-slate-900 font-medium break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap leading-relaxed select-text p-3 rounded-xl bg-slate-50 border border-slate-200/90 shadow-2xs">
                      {displayStr}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {deleteKeyConfirm && (
        <ConfirmModal
          isOpen={Boolean(deleteKeyConfirm)}
          onClose={() => setDeleteKeyConfirm(null)}
          onConfirm={confirmDelete}
          title={`Delete Attribute "${deleteKeyConfirm}"?`}
          variant="danger"
          confirmLabel="Yes, Delete Attribute"
          message={`Are you sure you want to delete the custom attribute "${deleteKeyConfirm}"? This specification will be removed from this ${entityType}.`}
        />
      )}
    </div>
  );
};
