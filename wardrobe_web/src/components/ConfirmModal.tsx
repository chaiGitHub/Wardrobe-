import React, { useEffect } from 'react';
import { AlertTriangle, Droplets, Trash2, X, AlertCircle, Loader2, ArrowLeft } from 'lucide-react';

export interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: string;
  message: React.ReactNode;
  subtitle?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'primary' | 'warning';
  icon?: 'trash' | 'droplets' | 'alert' | 'custom';
  customIcon?: React.ReactNode;
  isLoading?: boolean;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  subtitle,
  confirmLabel,
  cancelLabel = 'Cancel',
  variant = 'danger',
  icon,
  customIcon,
  isLoading = false,
}) => {
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isLoading) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isLoading, onClose]);

  if (!isOpen) return null;

  const resolvedIconType = icon || (variant === 'danger' ? 'trash' : variant === 'primary' ? 'droplets' : 'alert');

  const getIcon = () => {
    if (customIcon) return customIcon;
    switch (resolvedIconType) {
      case 'trash':
        return <Trash2 className="w-8 h-8" />;
      case 'droplets':
        return <Droplets className="w-8 h-8" />;
      case 'alert':
      default:
        return variant === 'danger' ? <AlertTriangle className="w-8 h-8" /> : <AlertCircle className="w-8 h-8" />;
    }
  };

  const getIconContainerStyle = () => {
    switch (variant) {
      case 'primary':
        return 'bg-blue-100 text-blue-700 border-blue-200';
      case 'warning':
        return 'bg-amber-100 text-amber-700 border-amber-200';
      case 'danger':
      default:
        return 'bg-rose-100 text-rose-600 border-rose-200';
    }
  };

  const getConfirmButtonStyle = () => {
    switch (variant) {
      case 'primary':
        return 'bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white shadow-xs focus:ring-blue-500';
      case 'warning':
        return 'bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white shadow-xs focus:ring-amber-500';
      case 'danger':
      default:
        return 'bg-rose-600 hover:bg-rose-700 active:bg-rose-800 text-white shadow-xs focus:ring-rose-500';
    }
  };

  const resolvedConfirmLabel = confirmLabel || (variant === 'danger' ? 'Confirm Delete' : variant === 'primary' ? 'Confirm Wash' : 'Confirm');

  return (
    <div
      id="confirm-action-modal"
      className="fixed inset-0 z-[120] w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto"
      role="dialog"
      aria-modal="true"
    >
      {/* Sticky Header */}
      <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between shrink-0 w-full max-w-full overflow-x-hidden">
        <div className="flex items-center gap-2.5 min-w-0">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="p-1.5 sm:p-2 -ml-1 sm:-ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
            title="Back / Cancel"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="min-w-0">
            <h2 className="text-sm sm:text-base font-bold text-slate-900 truncate">
              {title}
            </h2>
            {subtitle && (
              <p className="text-[11px] text-slate-500 font-mono truncate">
                {subtitle}
              </p>
            )}
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          disabled={isLoading}
          className="p-1.5 sm:p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
          title="Close"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 overflow-y-auto overflow-x-hidden p-6 sm:p-10 flex flex-col items-center justify-center bg-slate-50/60 w-full max-w-full">
        <div className="max-w-md w-full mx-auto space-y-6 text-center">
          <div className={`w-20 h-20 rounded-3xl mx-auto flex items-center justify-center shadow-xs border ${getIconContainerStyle()}`}>
            {getIcon()}
          </div>

          <div className="space-y-1.5">
            <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight">
              {title}
            </h3>
            {subtitle && (
              <span className="inline-block px-3 py-1 rounded-full bg-slate-200/70 text-slate-700 text-xs font-mono font-semibold">
                {subtitle}
              </span>
            )}
          </div>

          <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-2xs text-xs sm:text-sm text-slate-700 leading-relaxed text-left">
            {message}
          </div>
        </div>
      </div>

      {/* Sticky Bottom Actions */}
      <div className="p-4 sm:p-6 pb-[max(1rem,env(safe-area-inset-bottom))] border-t border-slate-200 bg-white flex items-center justify-center shrink-0 w-full max-w-full shadow-xs">
        <div className="max-w-md w-full flex items-center gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="flex-1 py-3 px-4 rounded-xl text-xs sm:text-sm font-semibold text-slate-700 hover:text-slate-900 hover:bg-slate-100 border border-slate-300 transition-all cursor-pointer active:scale-95 disabled:opacity-50"
          >
            {cancelLabel}
          </button>

          <button
            type="button"
            onClick={async () => {
              await onConfirm();
            }}
            disabled={isLoading}
            className={`flex-1 py-3 px-4 rounded-xl text-xs sm:text-sm font-bold transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-95 disabled:opacity-50 shadow-xs ${getConfirmButtonStyle()}`}
          >
            {isLoading && <Loader2 className="w-4 h-4 animate-spin" />}
            <span>{resolvedConfirmLabel}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
