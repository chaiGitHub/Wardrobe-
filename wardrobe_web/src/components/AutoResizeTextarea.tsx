import React, { useRef, useEffect, useCallback, useState } from 'react';

export interface AutoResizeTextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  value: string;
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  minHeight?: number;
  maxHeight?: number;
}

export const AutoResizeTextarea: React.FC<AutoResizeTextareaProps> = ({
  value,
  onChange,
  className = '',
  placeholder,
  minHeight = 44,
  maxHeight = 2400,
  rows = 1,
  style,
  ...props
}) => {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const [manualHeight, setManualHeight] = useState<number | null>(null);
  const isDraggingRef = useRef(false);

  const adjustHeight = useCallback(() => {
    const el = textareaRef.current;
    if (!el || isDraggingRef.current) return;

    if (manualHeight !== null) {
      el.style.height = `${manualHeight}px`;
      return;
    }

    // Auto calculate true scrollHeight to show complete content
    el.style.height = 'auto';
    const computedH = Math.max(minHeight, Math.min(maxHeight, el.scrollHeight + 4));
    el.style.height = `${computedH}px`;
  }, [minHeight, maxHeight, manualHeight]);

  useEffect(() => {
    adjustHeight();
  }, [value, adjustHeight]);

  // Handle native or drag resizing observation
  const handlePointerDown = () => {
    isDraggingRef.current = true;
    const initialH = textareaRef.current?.getBoundingClientRect().height || minHeight;

    const handlePointerUp = () => {
      isDraggingRef.current = false;
      window.removeEventListener('pointerup', handlePointerUp);
      if (textareaRef.current) {
        const finalH = textareaRef.current.getBoundingClientRect().height;
        if (Math.abs(finalH - initialH) > 4) {
          setManualHeight(Math.max(minHeight, finalH));
        }
      }
    };
    window.addEventListener('pointerup', handlePointerUp);
  };

  return (
    <div className="relative w-full min-w-0">
      <textarea
        ref={textareaRef}
        value={value}
        onChange={(e) => {
          onChange(e);
          adjustHeight();
        }}
        onPointerDown={handlePointerDown}
        placeholder={placeholder}
        rows={rows}
        style={{
          minHeight: `${minHeight}px`,
          height: manualHeight ? `${manualHeight}px` : undefined,
          ...style
        }}
        className={`w-full px-3 py-2.5 rounded-xl border border-slate-200 text-xs sm:text-sm bg-white text-slate-900 placeholder:text-slate-400 break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap resize-y leading-relaxed focus:outline-none focus:ring-2 focus:ring-purple-500/20 focus:border-purple-300 transition-[border-color,box-shadow] ${className}`}
        {...props}
      />
    </div>
  );
};
