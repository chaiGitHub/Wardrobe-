import React, { useState, useMemo } from 'react';
import { X, Search, History, Check, Plus, Trash2, ArrowLeft, Shirt, Sparkles } from 'lucide-react';

export interface PreviousEntryItem {
  value: string;
  count: number;
  labelCn?: string;
  meaningCn?: string;
  sources?: string[];
}

interface PreviousEntriesModalProps {
  isOpen: boolean;
  fieldName?: string;
  fieldLabel: string;
  currentValue?: string;
  entries: PreviousEntryItem[];
  onSelect: (value: string) => void;
  onClose: () => void;
  onAddOption?: (value: string, labelCn?: string, meaningCn?: string) => void;
}

export const PreviousEntriesModal: React.FC<PreviousEntriesModalProps> = ({
  isOpen,
  fieldLabel,
  currentValue,
  entries,
  onSelect,
  onClose
}) => {
  if (!isOpen) return null;

  const [searchQuery, setSearchQuery] = useState('');

  const filteredEntries = useMemo(() => {
    if (!searchQuery.trim()) return entries;
    const q = searchQuery.toLowerCase().trim();
    return entries.filter(e =>
      e.value.toLowerCase().includes(q) ||
      (e.labelCn && e.labelCn.toLowerCase().includes(q)) ||
      (e.meaningCn && e.meaningCn.toLowerCase().includes(q)) ||
      (e.sources && e.sources.some(s => s.toLowerCase().includes(q)))
    );
  }, [entries, searchQuery]);

  const hasExactMatch = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return entries.some(e => e.value.toLowerCase() === q);
  }, [entries, searchQuery]);

  return (
    <div
      id="previous-entries-fullscreen"
      className="fixed inset-0 z-50 bg-white flex flex-col h-screen w-screen overflow-hidden animate-in fade-in duration-150"
    >
      {/* Full-Screen Top Header: Neutral, Clean Border Styling */}
      <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200 bg-white flex items-center justify-between gap-3 shrink-0 shadow-2xs">
        <div className="flex items-center gap-3 min-w-0">
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 sm:p-2 -ml-2 rounded-xl text-slate-600 hover:text-slate-900 border border-slate-300 hover:border-slate-400 bg-transparent transition-colors"
            title="Back / Close"
          >
            <ArrowLeft className="w-4 h-4 sm:w-5 sm:h-5" />
          </button>

          <div className="min-w-0">
            <h2 className="text-base sm:text-lg font-bold text-slate-900 truncate">
              Choose {fieldLabel}
            </h2>
            <p className="text-xs text-slate-500 hidden sm:block">
              {entries.length} recorded values found. Select an existing entry or key in a new one.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="p-1.5 sm:p-2 rounded-xl text-slate-500 hover:text-slate-900 border border-slate-300 hover:border-slate-400 bg-transparent transition-colors"
          aria-label="Close"
        >
          <X className="w-4 h-4 sm:w-5 sm:h-5" />
        </button>
      </div>

      {/* Full-Screen Search Bar */}
      <div className="px-4 sm:px-8 py-3 border-b border-slate-200 bg-slate-50/70 shrink-0">
        <div className="max-w-4xl mx-auto flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder={`Search or enter new ${fieldLabel.toLowerCase()} (or search source clothes e.g. dress)...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoFocus
              className="w-full pl-10 pr-8 py-2.5 rounded-xl bg-white border border-slate-300 text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:border-slate-700"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-0.5 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {searchQuery.trim() && !hasExactMatch && (
            <button
              type="button"
              onClick={() => {
                onSelect(searchQuery.trim());
                onClose();
              }}
              className="inline-flex items-center gap-1 px-3 sm:px-4 py-2.5 rounded-xl border border-slate-700 text-slate-900 hover:border-slate-900 bg-transparent text-xs font-bold transition-colors shrink-0"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Use "{searchQuery.trim()}"</span>
            </button>
          )}
        </div>
      </div>

      {/* Full-Screen Options List */}
      <div className="flex-1 overflow-y-auto px-4 sm:px-8 py-4 bg-slate-50/30">
        <div className="max-w-4xl mx-auto space-y-2">
          {/* Quick Clear option */}
          <button
            type="button"
            onClick={() => {
              onSelect('');
              onClose();
            }}
            className="w-full text-left p-3 rounded-xl border border-slate-200 hover:border-slate-400 bg-white transition-colors flex items-center justify-between text-xs font-medium"
          >
            <span className="flex items-center gap-2 text-slate-600">
              <Trash2 className="w-3.5 h-3.5 text-slate-400" />
              Clear / Leave Empty
            </span>
            {!currentValue && <Check className="w-4 h-4 text-slate-800" />}
          </button>

          {filteredEntries.length > 0 ? (
            <div className="space-y-3 w-full">
              {filteredEntries.map((entry, idx) => {
                const isSelected = currentValue === entry.value;
                return (
                  <div
                    key={idx}
                    onClick={() => {
                      onSelect(entry.value);
                      onClose();
                    }}
                    className={`w-full text-left p-4 sm:p-5 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-start justify-between gap-3 group cursor-pointer ${
                      isSelected
                        ? 'border-purple-600 bg-purple-50/80 shadow-xs ring-2 ring-purple-600/30'
                        : 'border-slate-200 hover:border-purple-300 bg-white hover:bg-slate-50/70 shadow-2xs'
                    }`}
                  >
                    <div className="flex-1 min-w-0 space-y-2">
                      <div className="flex items-start gap-2 flex-wrap justify-between">
                        <div className="text-xs sm:text-sm leading-relaxed whitespace-pre-wrap break-words [overflow-wrap:break-word] text-slate-900 font-semibold select-text flex-1 min-w-0">
                          {entry.value}
                        </div>
                        {entry.labelCn && entry.labelCn !== entry.value && (
                          <span className="text-[11px] text-purple-700 bg-purple-100/70 px-2 py-0.5 rounded-full font-medium shrink-0">
                            {entry.labelCn}
                          </span>
                        )}
                      </div>

                      {entry.meaningCn && (
                        <div className="text-[11px] text-slate-700 bg-purple-50/90 rounded-xl px-3 py-2 border border-purple-200/80 text-left w-full space-y-1">
                          <span className="font-semibold text-purple-950 flex items-center gap-1">
                            <Sparkles className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                            <span>Meaning CN / 体感定义:</span>
                          </span>
                          <p className="whitespace-pre-wrap break-words [overflow-wrap:break-word] text-slate-800 leading-relaxed font-sans">
                            {entry.meaningCn}
                          </p>
                        </div>
                      )}

                      {/* Badges: Usage frequency and Garment Sources */}
                      <div className="flex items-center gap-2 pt-1 flex-wrap">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-mono font-medium bg-slate-100 text-slate-600 border border-slate-200/80 shrink-0">
                          {entry.count > 0 ? `Used ${entry.count} ${entry.count === 1 ? 'time' : 'times'}` : 'Preset Option'}
                        </span>

                        {entry.sources && entry.sources.length > 0 && (
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="text-[10px] font-bold text-slate-600 flex items-center gap-1 shrink-0">
                              <Shirt className="w-3.5 h-3.5 text-purple-600" />
                              <span>来源衣物:</span>
                            </span>
                            {entry.sources.map((src, sIdx) => (
                              <span
                                key={sIdx}
                                className="inline-flex items-center px-2 py-0.5 rounded-lg text-[10px] font-semibold bg-purple-100/90 text-purple-900 border border-purple-300 shadow-2xs whitespace-normal break-words"
                                title={`来自历史衣橱记录: ${src}`}
                              >
                                👕 {src}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="shrink-0 flex items-center gap-2 self-end sm:self-start pt-1 sm:pt-0">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelect(entry.value);
                          onClose();
                        }}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer ${
                          isSelected
                            ? 'bg-purple-700 text-white ring-2 ring-purple-300'
                            : 'bg-purple-600 hover:bg-purple-700 text-white active:scale-95'
                        }`}
                      >
                        {isSelected ? '✓ 当前已选' : '选择此项 (Use)'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="py-16 text-center px-4">
              <History className="w-10 h-10 text-slate-300 mx-auto mb-3" />
              <p className="text-sm text-slate-600 font-medium">
                {searchQuery ? `No matching options for "${searchQuery}"` : 'No previous entries recorded yet'}
              </p>
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => {
                    onSelect(searchQuery.trim());
                    onClose();
                  }}
                  className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-slate-700 hover:border-slate-900 text-slate-900 text-xs font-bold"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Create "{searchQuery.trim()}"
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Footer */}
      <div className="px-4 sm:px-8 py-3 border-t border-slate-200 bg-white flex items-center justify-between shrink-0">
        <span className="text-xs text-slate-500">
          Showing {filteredEntries.length} options
        </span>
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-1.5 rounded-xl border border-slate-300 hover:border-slate-400 text-slate-700 text-xs font-semibold"
        >
          Cancel
        </button>
      </div>
    </div>
  );
};
