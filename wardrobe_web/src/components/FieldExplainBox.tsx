import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  MessageSquare, MessageSquarePlus, ChevronDown, ChevronUp,
  Maximize2, Minimize2, GripHorizontal, Sparkles, History, Check
} from 'lucide-react';
import { dataService } from '../services/dataService';
import { fieldDictionaryService } from '../services/fieldDictionaryService';

export interface FieldExplainBoxProps {
  fieldKey: string;
  fieldLabel: string;
  value: string;
  onChange: (val: string) => void;
  isExpanded: boolean;
  onToggleExpand: () => void;
  customPlaceholder?: string;
  targetCategory?: 'clothes' | 'wearingReport';
}

export const FieldExplainBox: React.FC<FieldExplainBoxProps> = ({
  fieldKey,
  fieldLabel,
  value,
  onChange,
  isExpanded,
  onToggleExpand,
  customPlaceholder,
  targetCategory = 'clothes'
}) => {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const [customHeight, setCustomHeight] = useState<number | null>(null);
  const [isMaximized, setIsMaximized] = useState(false);
  const [syncedToast, setSyncedToast] = useState(false);

  // Multi-level height modes or custom drag
  const fitToFullContent = () => {
    if (!textareaRef.current) return;
    textareaRef.current.style.height = 'auto';
    const targetH = Math.max(120, textareaRef.current.scrollHeight + 16);
    setCustomHeight(targetH);
    setIsMaximized(false);
    textareaRef.current.style.height = `${targetH}px`;
  };

  const setPresetHeight = (h: number) => {
    setCustomHeight(h);
    setIsMaximized(false);
    if (textareaRef.current) {
      textareaRef.current.style.height = `${h}px`;
    }
  };

  // Auto-grow calculation when not manually dragged or maximized
  const autoResize = useCallback(() => {
    if (!textareaRef.current) return;
    if (customHeight !== null || isMaximized) return;

    textareaRef.current.style.height = 'auto';
    const scrollH = textareaRef.current.scrollHeight;
    const targetH = Math.max(100, scrollH + 12);
    textareaRef.current.style.height = `${targetH}px`;
  }, [customHeight, isMaximized]);

  useEffect(() => {
    if (isExpanded) {
      const t = setTimeout(autoResize, 50);
      return () => clearTimeout(t);
    }
  }, [isExpanded, value, autoResize]);

  // Handle Drag to Resize (Mouse and Touch support for mobile & desktop)
  const handleDragStart = (clientY: number) => {
    const startY = clientY;
    const currentH = textareaRef.current?.getBoundingClientRect().height || 100;

    const onMove = (moveClientY: number) => {
      const delta = moveClientY - startY;
      // Allow dragging up to 2400px for long user logs!
      const newH = Math.max(76, Math.min(2400, currentH + delta));
      setCustomHeight(newH);
      setIsMaximized(false);
      if (textareaRef.current) {
        textareaRef.current.style.height = `${newH}px`;
      }
    };

    const mouseMoveHandler = (e: MouseEvent) => {
      e.preventDefault();
      onMove(e.clientY);
    };

    const mouseUpHandler = () => {
      window.removeEventListener('mousemove', mouseMoveHandler);
      window.removeEventListener('mouseup', mouseUpHandler);
    };

    const touchMoveHandler = (e: TouchEvent) => {
      if (e.touches && e.touches[0]) {
        onMove(e.touches[0].clientY);
      }
    };

    const touchEndHandler = () => {
      window.removeEventListener('touchmove', touchMoveHandler);
      window.removeEventListener('touchend', touchEndHandler);
      window.removeEventListener('touchcancel', touchEndHandler);
    };

    window.addEventListener('mousemove', mouseMoveHandler);
    window.addEventListener('mouseup', mouseUpHandler);
    window.addEventListener('touchmove', touchMoveHandler, { passive: true });
    window.addEventListener('touchend', touchEndHandler);
    window.addEventListener('touchcancel', touchEndHandler);
  };

  const onMouseDownDrag = (e: React.MouseEvent) => {
    e.preventDefault();
    handleDragStart(e.clientY);
  };

  const onTouchStartDrag = (e: React.TouchEvent) => {
    if (e.touches && e.touches[0]) {
      handleDragStart(e.touches[0].clientY);
    }
  };

  const handleToggleMaximize = () => {
    if (isMaximized) {
      setIsMaximized(false);
      setCustomHeight(null);
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
      }
    } else {
      setIsMaximized(true);
      // Generous 550px maximize height
      setCustomHeight(550);
      if (textareaRef.current) {
        textareaRef.current.style.height = '550px';
      }
    }
  };

  const handleSyncToAiDict = () => {
    if (!value.trim()) return;
    const ok = fieldDictionaryService.appendErgonomicNote(fieldKey, value.trim());
    if (ok) {
      setSyncedToast(true);
      setTimeout(() => setSyncedToast(false), 3000);
    }
  };

  // Historical explanations suggestions
  const histExplains = dataService.getFieldHistoricalExplanations(fieldKey, targetCategory);

  // COLLAPSED STATE
  if (!isExpanded) {
    return (
      <div className="mt-1.5 w-full">
        {value ? (
          <button
            type="button"
            onClick={onToggleExpand}
            className="group inline-flex items-start gap-2 p-2.5 rounded-xl bg-purple-50/90 hover:bg-purple-100 text-purple-900 border border-purple-200/90 transition-all cursor-pointer text-left w-full shadow-2xs"
            title="点击展开编辑体感说明"
          >
            <MessageSquare className="w-4 h-4 text-purple-600 shrink-0 mt-0.5" />
            <div className="flex-1 min-w-0">
              <span className="font-bold text-purple-900 text-[11px] block mb-0.5">
                {fieldLabel} 体感说明:
              </span>
              <p className="text-xs text-purple-950 font-medium leading-relaxed break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word]">
                {value}
              </p>
            </div>
            <ChevronDown className="w-4 h-4 text-purple-500 shrink-0 mt-0.5 group-hover:translate-y-0.5 transition-transform" />
          </button>
        ) : (
          <button
            type="button"
            onClick={onToggleExpand}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-500 hover:text-purple-700 hover:bg-purple-50 border border-slate-200 hover:border-purple-200 transition-colors cursor-pointer"
            title={`为 ${fieldLabel} 添加体感补充说明 (默认收起)`}
          >
            <MessageSquarePlus className="w-3.5 h-3.5 text-purple-600" />
            <span>+ 说明 (Explain)</span>
          </button>
        )}
      </div>
    );
  }

  // EXPANDED STATE
  return (
    <div className="mt-1.5 p-3 rounded-2xl bg-purple-50/80 border border-purple-200 shadow-xs space-y-2 animate-in fade-in duration-150 w-full">
      {/* Header Bar with Action Controls */}
      <div className="flex items-center justify-between gap-2">
        <label className="text-xs font-bold text-purple-950 flex items-center gap-1.5 min-w-0">
          <MessageSquare className="w-3.5 h-3.5 text-purple-600 shrink-0" />
          <span className="truncate">{fieldLabel} 体感说明 (Explanation / Notes)</span>
        </label>

        <div className="flex items-center gap-1.5 shrink-0 flex-wrap justify-end">
          {/* Quick Fit-to-Full-Content button */}
          <button
            type="button"
            onClick={fitToFullContent}
            className="px-2 py-0.5 rounded-lg text-purple-800 hover:text-purple-950 hover:bg-purple-100 border border-purple-200 bg-white transition-colors cursor-pointer text-[10px] font-bold shadow-2xs"
            title="一键撑开输入框，完整显示所有文本内容 (Auto-fit full text)"
          >
            全文撑满
          </button>

          {/* Quick Preset heights */}
          <div className="hidden sm:flex items-center gap-0.5 bg-white border border-purple-200 rounded-lg p-0.5">
            <button
              type="button"
              onClick={() => setPresetHeight(160)}
              className="px-1.5 py-0.5 rounded text-[10px] font-medium text-slate-600 hover:bg-purple-100 hover:text-purple-900"
              title="设置高度 160px"
            >
              中
            </button>
            <button
              type="button"
              onClick={() => setPresetHeight(320)}
              className="px-1.5 py-0.5 rounded text-[10px] font-medium text-slate-600 hover:bg-purple-100 hover:text-purple-900"
              title="设置高度 320px"
            >
              大
            </button>
            <button
              type="button"
              onClick={() => setPresetHeight(520)}
              className="px-1.5 py-0.5 rounded text-[10px] font-medium text-slate-600 hover:bg-purple-100 hover:text-purple-900"
              title="设置高度 520px"
            >
              特大
            </button>
          </div>

          {/* Maximize / Enlarge Height Button */}
          <button
            type="button"
            onClick={handleToggleMaximize}
            className="p-1 rounded-lg text-purple-700 hover:text-purple-900 hover:bg-purple-100 border border-purple-200 bg-white transition-colors cursor-pointer flex items-center gap-1 text-[10px] font-semibold"
            title={isMaximized ? "还原高度" : "放大至 550px"}
          >
            {isMaximized ? (
              <>
                <Minimize2 className="w-3 h-3 text-purple-600" />
                <span className="hidden sm:inline">还原</span>
              </>
            ) : (
              <>
                <Maximize2 className="w-3 h-3 text-purple-600" />
                <span className="hidden sm:inline">放大</span>
              </>
            )}
          </button>

          {/* Collapse Button */}
          <button
            type="button"
            onClick={onToggleExpand}
            className="px-2 py-0.5 rounded-lg text-purple-700 hover:text-purple-900 hover:bg-purple-100 border border-purple-200 bg-white transition-colors cursor-pointer flex items-center gap-1 text-[10px] font-semibold"
          >
            <span>收起</span>
            <ChevronUp className="w-3 h-3" />
          </button>
        </div>
      </div>

      {/* Auto-Expanding + Touch-Draggable Textarea Container */}
      <div className="relative rounded-xl border border-purple-300/90 bg-white focus-within:ring-2 focus-within:ring-purple-500/20 shadow-inner">
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onPointerDown={() => {
            const initialH = textareaRef.current?.getBoundingClientRect().height || 100;
            const handleUp = () => {
              window.removeEventListener('pointerup', handleUp);
              if (textareaRef.current) {
                const finalH = textareaRef.current.getBoundingClientRect().height;
                if (Math.abs(finalH - initialH) > 4) {
                  setCustomHeight(finalH);
                  setIsMaximized(false);
                }
              }
            };
            window.addEventListener('pointerup', handleUp);
          }}
          placeholder={customPlaceholder || `记录关于 ${fieldLabel} 的真实穿着体感、版型优缺点、紧绷/压胸或透气表现...`}
          style={{
            height: customHeight ? `${customHeight}px` : undefined,
            minHeight: '100px'
          }}
          className="w-full px-3 py-2.5 bg-white text-xs sm:text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap resize-y leading-relaxed block rounded-t-xl"
        />

        {/* Touch & Mouse Drag Bar Handle (Works on Mobile iOS/Android & Desktop!) */}
        <div
          onMouseDown={onMouseDownDrag}
          onTouchStart={onTouchStartDrag}
          className="w-full py-2 px-3 bg-purple-50/90 hover:bg-purple-100 active:bg-purple-200 border-t border-purple-200/80 flex items-center justify-between cursor-row-resize select-none touch-none transition-colors rounded-b-xl"
          title="按住此把手上下拖动，可自由拉大或缩小文本框高度 (拖拽加高最高可达 2400px)"
        >
          <div className="flex items-center gap-1 text-purple-700">
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                const cur = textareaRef.current?.getBoundingClientRect().height || 100;
                setPresetHeight(Math.max(80, cur - 80));
              }}
              className="px-1.5 py-0.5 rounded bg-white border border-purple-200 text-[10px] font-mono hover:bg-purple-100"
              title="减小高度 80px"
            >
              -80px
            </button>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                const cur = textareaRef.current?.getBoundingClientRect().height || 100;
                setPresetHeight(Math.min(1200, cur + 100));
              }}
              className="px-1.5 py-0.5 rounded bg-white border border-purple-200 text-[10px] font-mono hover:bg-purple-100"
              title="增加高度 100px"
            >
              +100px
            </button>
          </div>

          <div className="flex items-center gap-1 text-purple-600">
            <GripHorizontal className="w-4 h-4" />
            <span className="text-[10px] font-mono font-semibold tracking-wider">
              {customHeight ? `${Math.round(customHeight)}px (拖拽中)` : '上下拖动拉大文本框'}
            </span>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                fitToFullContent();
              }}
              className="text-[10px] text-purple-800 font-bold underline cursor-pointer"
            >
              撑满全文
            </button>
            {customHeight && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setCustomHeight(null);
                  setIsMaximized(false);
                }}
                className="text-[10px] text-slate-500 hover:text-slate-700"
              >
                重置
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Historical suggestions & One-click Sync to AI Dictionary */}
      <div className="space-y-2 pt-0.5">
        {histExplains.length > 0 && (
          <div className="space-y-1.5 pt-1">
            <div className="flex items-center justify-between text-[11px] text-purple-900">
              <span className="font-bold flex items-center gap-1">
                <History className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                <span>历史曾用体感说明 (完整换行查看与追加):</span>
              </span>
              <span className="text-[10px] text-slate-500 font-mono">共 {histExplains.length} 条</span>
            </div>
            <div className="space-y-1.5 max-h-72 overflow-y-auto pr-1">
              {histExplains.map((h, hIdx) => {
                const text = (h.text || h.note || '').trim();
                const isAlreadyIncluded = value.includes(text);
                return (
                  <div
                    key={hIdx}
                    className={`p-2.5 rounded-xl border transition-all text-xs leading-relaxed flex flex-col sm:flex-row sm:items-start justify-between gap-2 shadow-2xs ${
                      isAlreadyIncluded
                        ? 'bg-purple-100/70 border-purple-300 text-purple-950 font-medium'
                        : 'bg-white hover:bg-purple-50/70 border-purple-200/90 text-slate-800'
                    }`}
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5 mb-1.5 flex-wrap">
                        {h.sources && h.sources.length > 0 ? (
                          h.sources.map((src, sIdx) => (
                            <span
                              key={sIdx}
                              className="text-[9px] font-semibold px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-900 border border-purple-300 shadow-2xs whitespace-normal break-words"
                              title={`来源衣物: ${src}`}
                            >
                              👕 {src}
                            </span>
                          ))
                        ) : h.garmentName ? (
                          <span className="text-[9px] font-semibold px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-900 border border-purple-300 shadow-2xs whitespace-normal break-words">
                            👕 {h.garmentName}
                          </span>
                        ) : null}
                        {h.count > 1 && (
                          <span className="text-[9px] font-mono text-slate-500">
                            曾使用 {h.count} 次
                          </span>
                        )}
                        {isAlreadyIncluded && (
                          <span className="inline-flex items-center gap-0.5 text-[9px] text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200 font-bold">
                            <Check className="w-2.5 h-2.5" /> 已包含在说明中
                          </span>
                        )}
                      </div>
                      <p className="whitespace-pre-wrap break-words [word-break:normal] [overflow-wrap:break-word] text-xs text-slate-800 leading-relaxed font-sans">
                        {text}
                      </p>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0 self-end sm:self-start pt-1 sm:pt-0">
                      <button
                        type="button"
                        onClick={() => {
                          if (!isAlreadyIncluded) {
                            onChange(value ? `${value}\n${text}` : text);
                          }
                        }}
                        disabled={isAlreadyIncluded}
                        className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[10px] font-bold transition-all shadow-2xs cursor-pointer ${
                          isAlreadyIncluded
                            ? 'bg-purple-200/50 text-purple-400 cursor-not-allowed'
                            : 'bg-purple-600 hover:bg-purple-700 text-white active:scale-95'
                        }`}
                        title={isAlreadyIncluded ? '当前体感说明中已包含此段文字' : '将此段历史说明换行追加到输入框'}
                      >
                        <span className="text-xs leading-none">+</span>
                        <span>追加说明</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          onChange(text);
                        }}
                        className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-[10px] font-semibold bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 transition-colors cursor-pointer"
                        title="用此条说明直接替换输入框当前内容"
                      >
                        <span>直接填入</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Sync to AI Dictionary Meaning CN action */}
        <div className="flex items-center justify-between pt-1 border-t border-purple-200/60 flex-wrap gap-2">
          <div className="text-[10px] text-slate-500 flex items-center gap-1">
            {syncedToast ? (
              <span className="inline-flex items-center gap-1 text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200 animate-in fade-in">
                <Check className="w-3 h-3" />
                已成功写入 AI 字典 Meaning CN
              </span>
            ) : (
              <span>已保存至当前体感说明</span>
            )}
          </div>

          <button
            type="button"
            onClick={handleSyncToAiDict}
            disabled={!value.trim()}
            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl text-[10px] font-bold bg-purple-600 hover:bg-purple-700 disabled:opacity-40 text-white transition-all shadow-xs cursor-pointer active:scale-95"
            title="将此说明直接写入 AI 字典中该字段的 Meaning CN / Description CN"
          >
            <Sparkles className="w-3 h-3" />
            <span>同步至 AI 字典定义</span>
          </button>
        </div>
      </div>
    </div>
  );
};
