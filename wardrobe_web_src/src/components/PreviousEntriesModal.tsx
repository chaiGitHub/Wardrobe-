import React, { useState, useMemo } from 'react';
import { X, Search, History, Check, Plus, Trash2, ArrowLeft } from 'lucide-react';

export interface PreviousEntryItem {
  value: string;
  count: number;
}

interface PreviousEntriesModalProps {
  isOpen: boolean;
  fieldName: string;
  fieldLabel: string;
  currentValue?: string;
  entries: PreviousEntryItem[];
  onSelect: (value: string) => void;
  onClose: () => void;
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
    return entries.filter(e => e.value.toLowerCase().includes(q));
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
      {/* Full-Screen Top Header: Neutral, Clean Border Styling, No Purple */}
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
              placeholder={`Search or enter new ${fieldLabel.toLowerCase()}...`}
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
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {filteredEntries.map((entry, idx) => {
                const isSelected = currentValue === entry.value;
                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      onSelect(entry.value);
                      onClose();
                    }}
                    className={`w-full text-left p-3.5 rounded-2xl border transition-all flex items-center justify-between ${
                      isSelected
                        ? 'border-slate-900 bg-white shadow-xs font-bold'
                        : 'border-slate-200/90 hover:border-slate-400 bg-white'
                    }`}
                  >
                    <div className="flex flex-col items-start gap-1 min-w-0 pr-2">
                      <span className={`text-xs sm:text-sm truncate ${
                        isSelected ? 'text-slate-900 font-bold' : 'text-slate-800'
                      }`}>
                        {entry.value}
                      </span>
                      <span className="text-[10px] font-mono text-slate-500">
                        Used {entry.count} {entry.count === 1 ? 'time' : 'times'}
                      </span>
                    </div>

                    {isSelected ? (
                      <span className="p-1 rounded-lg border border-slate-900 text-slate-900 shrink-0">
                        <Check className="w-3.5 h-3.5" />
                      </span>
                    ) : (
                      <span className="text-[11px] text-slate-400 font-medium">Select</span>
                    )}
                  </button>
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
