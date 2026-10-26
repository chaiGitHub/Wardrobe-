import React, { useState, useEffect, useRef, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Type, Check, X, Sparkles } from 'lucide-react';

export type FontScale = 'default' | 'comfortable' | 'large';

export interface FontScaleOption {
  key: FontScale;
  label: string;
  badge: string;
  rootPx: string;
  description: string;
  previewClass: string;
}

export const FONT_SCALE_OPTIONS: FontScaleOption[] = [
  {
    key: 'default',
    label: 'Standard',
    badge: '100%',
    rootPx: '16px',
    description: 'Original density & compact layout',
    previewClass: 'text-xs'
  },
  {
    key: 'comfortable',
    label: 'Comfortable',
    badge: '+18%',
    rootPx: '18.5px',
    description: 'Comfortable reading for wear records & cards',
    previewClass: 'text-sm font-semibold'
  },
  {
    key: 'large',
    label: 'Large',
    badge: '+32%',
    rootPx: '21px',
    description: 'High visibility & maximum readability',
    previewClass: 'text-base font-bold'
  }
];

export const FONT_SCALE_STORAGE_KEY = 'wardrobe_font_scale';

export const getCurrentFontScale = (): FontScale => {
  if (typeof window !== 'undefined') {
    try {
      const saved = localStorage.getItem(FONT_SCALE_STORAGE_KEY) as FontScale | null;
      if (saved && (saved === 'default' || saved === 'comfortable' || saved === 'large')) {
        return saved;
      }
    } catch {}
  }
  return 'comfortable';
};

export const setGlobalFontScale = (scale: FontScale) => {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-font-size', scale);
    try {
      localStorage.setItem(FONT_SCALE_STORAGE_KEY, scale);
    } catch {}
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('wardrobe_font_scale_changed', { detail: scale }));
    }
  }
};

export const FontSizeSwitcher: React.FC<{ className?: string }> = ({ className = '' }) => {
  const [currentScale, setCurrentScale] = useState<FontScale>(getCurrentFontScale);
  const [isOpen, setIsOpen] = useState(false);
  const [coords, setCoords] = useState<{ top: number; left: number; width: number } | null>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Sync state with global scale changes
  useEffect(() => {
    const handleScaleChanged = (e: Event) => {
      const customEvent = e as CustomEvent<FontScale>;
      if (customEvent.detail) {
        setCurrentScale(customEvent.detail);
      }
    };
    window.addEventListener('wardrobe_font_scale_changed', handleScaleChanged);
    return () => window.removeEventListener('wardrobe_font_scale_changed', handleScaleChanged);
  }, []);

  // Compute optimal dropdown coordinates
  const updatePosition = useCallback(() => {
    if (!buttonRef.current || typeof window === 'undefined') return;
    const rect = buttonRef.current.getBoundingClientRect();
    const dropdownWidth = Math.min(340, Math.max(280, window.innerWidth - 24));

    // Align dropdown right edge with button right edge, clamped strictly inside viewport
    let left = rect.right - dropdownWidth;
    if (left < 12) left = 12;
    if (left + dropdownWidth > window.innerWidth - 12) {
      left = Math.max(12, window.innerWidth - 12 - dropdownWidth);
    }

    let top = Math.max(52, Math.round(rect.bottom + 8));
    // If opening downwards would push the card off the bottom, cap it safely
    if (top + 280 > window.innerHeight && rect.top > 300) {
      top = Math.max(12, Math.round(rect.top - 290));
    }

    setCoords({ top, left, width: dropdownWidth });
  }, []);

  const toggleDropdown = () => {
    if (!isOpen) {
      updatePosition();
    }
    setIsOpen(!isOpen);
  };

  // Recalculate on scroll, resize, or open
  useEffect(() => {
    if (!isOpen) return;
    updatePosition();
    window.addEventListener('resize', updatePosition);
    window.addEventListener('scroll', updatePosition, true);
    return () => {
      window.removeEventListener('resize', updatePosition);
      window.removeEventListener('scroll', updatePosition, true);
    };
  }, [isOpen, updatePosition]);

  // Dismiss on Escape key
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  // Dismiss on outside pointerdown
  useEffect(() => {
    if (!isOpen) return;
    const handlePointerDown = (e: MouseEvent | TouchEvent) => {
      const target = e.target as Node | null;
      if (!target) return;
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(target) &&
        buttonRef.current &&
        !buttonRef.current.contains(target)
      ) {
        setIsOpen(false);
      }
    };
    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [isOpen]);

  const activeOption = FONT_SCALE_OPTIONS.find(o => o.key === currentScale) || FONT_SCALE_OPTIONS[1];

  const handleSelectScale = (scale: FontScale) => {
    setCurrentScale(scale);
    setGlobalFontScale(scale);
    setIsOpen(false);
  };

  return (
    <div className={`relative inline-block text-left ${className}`}>
      {/* Trigger Button - Polished high-contrast styling */}
      <button
        ref={buttonRef}
        type="button"
        onClick={toggleDropdown}
        aria-label="Change application font size"
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        title={`App font size: ${activeOption.label} (${activeOption.badge}) — Click to change`}
        className={`flex items-center gap-1.5 px-2 sm:px-3 min-h-[38px] sm:min-h-[40px] rounded-xl text-xs font-bold border-2 transition-all active:scale-95 cursor-pointer select-none ${
          isOpen
            ? 'border-indigo-600 bg-indigo-50 text-indigo-950 shadow-xs ring-2 ring-indigo-200'
            : 'border-indigo-200/90 bg-white hover:bg-indigo-50/60 hover:border-indigo-400 text-slate-800 shadow-2xs'
        }`}
      >
        <span className="flex items-center justify-center w-5 h-5 rounded-lg bg-indigo-600 text-white font-black text-[11px] tracking-tight shrink-0 shadow-2xs">
          Aa
        </span>
        <span className="font-bold text-indigo-950 text-[11px] sm:text-xs shrink-0">
          {activeOption.badge}
        </span>
      </button>

      {/* Portal-Rendered Dialog with maximum z-index ensuring it is never under other screens */}
      {isOpen && typeof document !== 'undefined' && createPortal(
        <div className="fixed inset-0 z-[999998] select-none pointer-events-auto">
          {/* Subtle backdrop layer for dismissing on outside tap */}
          <div
            className="fixed inset-0 bg-slate-900/25 backdrop-blur-[1px] transition-opacity cursor-pointer"
            onClick={() => setIsOpen(false)}
            aria-hidden="true"
          />

          {/* Floating Dropdown Card with guaranteed in-viewport positioning */}
          <div
            ref={dropdownRef}
            style={{
              top: `${coords?.top ?? 64}px`,
              left: `${coords?.left ?? 16}px`,
              width: `${coords?.width ?? 320}px`,
              maxHeight: 'calc(100dvh - 80px)'
            }}
            role="dialog"
            aria-modal="true"
            aria-label="Application Font Size Settings"
            className="fixed z-[999999] bg-white rounded-3xl border-2 border-indigo-200 shadow-2xl p-4 overflow-y-auto"
          >
            {/* Header */}
            <div className="flex items-center justify-between pb-2.5 mb-2.5 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <span className="p-1.5 rounded-xl bg-indigo-50 text-indigo-600">
                  <Type className="w-4 h-4" />
                </span>
                <div>
                  <h4 className="font-bold text-slate-900 text-xs sm:text-sm leading-tight">
                    App Font Size
                  </h4>
                  <span className="text-[10px] text-slate-500 font-mono">
                    Base: {activeOption.rootPx} ({activeOption.badge})
                  </span>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer"
                aria-label="Close font size dialog"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-[11px] text-slate-500 mb-3 leading-relaxed">
              Scale all text instantly across closet garments, wear journals, laundry care, and drill-down reports.
            </p>

            {/* Scale Options List */}
            <div className="space-y-2">
              {FONT_SCALE_OPTIONS.map((opt) => {
                const isSelected = opt.key === currentScale;
                return (
                  <button
                    key={opt.key}
                    type="button"
                    onClick={() => handleSelectScale(opt.key)}
                    className={`w-full text-left p-3 rounded-2xl border-2 transition-all flex items-center justify-between cursor-pointer active:scale-[0.99] ${
                      isSelected
                        ? 'bg-indigo-50/90 border-indigo-500 text-indigo-950 shadow-xs ring-2 ring-indigo-200'
                        : 'bg-slate-50/80 hover:bg-slate-100/90 border-slate-200 text-slate-700'
                    }`}
                  >
                    <div className="min-w-0 pr-2">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-xs sm:text-sm text-slate-900">
                          {opt.label}
                        </span>
                        <span
                          className={`text-[10px] font-mono px-1.5 py-0.5 rounded-md font-bold ${
                            isSelected
                              ? 'bg-indigo-600 text-white shadow-2xs'
                              : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {opt.badge}
                        </span>
                        {opt.key === 'comfortable' && (
                          <span className="inline-flex items-center gap-1 text-[9px] font-bold text-emerald-800 bg-emerald-50 px-1.5 py-0.5 rounded-md border border-emerald-300">
                            <Sparkles className="w-2.5 h-2.5 text-emerald-600" />
                            Recommended
                          </span>
                        )}
                      </div>
                      <div className="text-[11px] text-slate-500 mt-1 leading-snug">
                        {opt.description}
                      </div>
                    </div>

                    <div className={`shrink-0 flex items-center justify-center w-6 h-6 rounded-full border shadow-2xs ${
                      isSelected ? 'bg-indigo-600 border-indigo-600 text-white' : 'bg-white border-slate-300'
                    }`}>
                      {isSelected ? (
                        <Check className="w-3.5 h-3.5 stroke-[3]" />
                      ) : (
                        <span className="w-2 h-2 rounded-full bg-slate-300" />
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
};
