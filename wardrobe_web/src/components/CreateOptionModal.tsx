import React, { useState, useEffect } from 'react';
import { X, Plus, Sparkles, Check, Tag } from 'lucide-react';
import { fieldDictionaryService } from '../services/fieldDictionaryService';

interface CreateOptionModalProps {
  isOpen: boolean;
  fieldName: string;
  fieldLabel: string;
  initialValue?: string;
  onSave: (valueEn: string, labelCn?: string, meaningCn?: string) => void;
  onClose: () => void;
}

export const CreateOptionModal: React.FC<CreateOptionModalProps> = ({
  isOpen,
  fieldName,
  fieldLabel,
  initialValue = '',
  onSave,
  onClose
}) => {
  const [valueEn, setValueEn] = useState(initialValue);
  const [valueCn, setValueCn] = useState('');
  const [meaningCn, setMeaningCn] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setValueEn(initialValue);
      setValueCn('');
      setMeaningCn('');
      setError('');
    }
  }, [isOpen, initialValue]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanEn = valueEn.trim();
    if (!cleanEn) {
      setError('请输入选项名称 (英文/英文代号)');
      return;
    }

    const cleanCn = valueCn.trim() || cleanEn;
    const cleanMeaning = meaningCn.trim();

    // 1. Save directly into field dictionary service so it embeds in AI definition Meaning CN
    fieldDictionaryService.addTypicalValue(fieldName, {
      valueEn: cleanEn,
      valueCn: cleanCn,
      meaningCn: cleanMeaning,
      meaningEn: ''
    });

    // 2. Callback to parent component
    onSave(cleanEn, cleanCn, cleanMeaning);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-[80] bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4 animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between bg-gradient-to-r from-purple-50/70 to-slate-50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-purple-100 rounded-xl text-purple-700">
              <Plus className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-bold text-slate-900">
                新建选项 · {fieldLabel || fieldName}
              </h3>
              <p className="text-xs text-slate-500">
                创建后将自动保存并同步至 AI 字典 Meaning CN
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {error && (
            <div className="px-3 py-2 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-mono font-bold uppercase text-slate-700 mb-1">
              选项名称 (Option Value) <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              autoFocus
              value={valueEn}
              onChange={(e) => {
                setValueEn(e.target.value);
                if (error) setError('');
              }}
              placeholder="例如: middle, dry, wet, need dry..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:border-purple-600 focus:ring-1 focus:ring-purple-600 bg-white"
            />
            <p className="mt-1 text-[11px] text-slate-400">
              在系统和数据中存储的核心选项值（如 middle）
            </p>
          </div>

          <div>
            <label className="block text-xs font-mono font-bold uppercase text-slate-700 mb-1">
              中文显示名称 (Chinese Label)
            </label>
            <input
              type="text"
              value={valueCn}
              onChange={(e) => setValueCn(e.target.value)}
              placeholder="例如: 适中 / 半干半湿 (选填)"
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:border-purple-600 focus:ring-1 focus:ring-purple-600 bg-white"
            />
            <p className="mt-1 text-[11px] text-slate-400">
              用于下拉菜单展示的友好中文释义，留空则使用选项名称
            </p>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-mono font-bold uppercase text-slate-700 flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5 text-purple-600" />
                中文体感与人体工学详细含义 (Meaning CN)
              </label>
              <span className="text-[10px] text-purple-700 font-semibold bg-purple-50 px-2 py-0.5 rounded-full border border-purple-100">
                AI 字典内嵌
              </span>
            </div>
            <textarea
              rows={4}
              value={meaningCn}
              onChange={(e) => setMeaningCn(e.target.value)}
              placeholder="例如: 排湿透气适中，微有体感但未明显积汗黏腻；或者该版型在穿着过程中的舒适度、受汗发黏实测记录..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:border-purple-600 focus:ring-1 focus:ring-purple-600 bg-white resize-y min-h-[88px] leading-relaxed break-words whitespace-pre-wrap"
            />
            <div className="flex items-center justify-between mt-1 text-[11px] text-slate-400">
              <span>可自由拖动右下角拉大文本框</span>
              <span className="text-purple-600 font-medium">支持任意长度体感与工学细节</span>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="pt-2 flex items-center justify-end gap-2.5 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-600 text-xs font-semibold transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              className="inline-flex items-center gap-1.5 px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs cursor-pointer active:scale-95"
            >
              <Check className="w-3.5 h-3.5" />
              确认创建并选中
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
