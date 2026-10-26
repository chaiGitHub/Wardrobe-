import React, { useState } from 'react';
import { NotebookLmTopicDoc, notebookLmExportService } from '../services/notebookLmExportService';
import {
  X,
  Copy,
  Check,
  Download,
  BookOpen,
  FileText,
  Sparkles,
  ExternalLink,
  Layers
} from 'lucide-react';

interface NotebookLmPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTopicId?: string;
}

export const NotebookLmPreviewModal: React.FC<NotebookLmPreviewModalProps> = ({
  isOpen,
  onClose,
  initialTopicId = 'catalog'
}) => {
  const [selectedTopicId, setSelectedTopicId] = useState<string>(initialTopicId);
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const docs = notebookLmExportService.getAllTopicDocs();
  const currentDoc = docs.find((d) => d.id === selectedTopicId) || docs[0];

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(currentDoc.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (e) {
      console.warn('Clipboard write failed:', e);
    }
  };

  const handleDownload = () => {
    notebookLmExportService.downloadFile(currentDoc.filename, currentDoc.content);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-4xl h-[90vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/80 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-xs shrink-0">
              <BookOpen className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 font-mono">
                  NotebookLM Source Preview
                </span>
                <span className="text-slate-300">·</span>
                <span className="text-xs text-slate-500 font-medium">
                  {currentDoc.wordCount.toLocaleString()} words
                </span>
              </div>
              <h3 className="text-base sm:text-lg font-bold text-slate-900 leading-tight">
                {currentDoc.title}
              </h3>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleCopy}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-xl bg-white border border-slate-200 text-slate-700 hover:text-indigo-600 hover:border-indigo-300 shadow-2xs transition-colors cursor-pointer active:scale-95"
              title="Copy markdown text for pasting directly into NotebookLM"
            >
              {copied ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                  <span className="text-emerald-700">Copied!</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5 text-slate-500" />
                  <span>Copy Markdown</span>
                </>
              )}
            </button>

            <button
              onClick={handleDownload}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-semibold rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 shadow-xs transition-colors cursor-pointer active:scale-95"
              title="Download this markdown file"
            >
              <Download className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Download .md</span>
            </button>

            <button
              onClick={onClose}
              className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors ml-1 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Topic Selector Tabs */}
        <div className="px-4 py-2 border-b border-slate-100 bg-white flex items-center gap-1.5 overflow-x-auto shrink-0 scrollbar-none">
          {docs.map((d) => {
            const isSelected = d.id === currentDoc.id;
            return (
              <button
                key={d.id}
                onClick={() => setSelectedTopicId(d.id)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-indigo-50 text-indigo-700 border border-indigo-200 shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100 border border-transparent'
                }`}
              >
                <FileText className={`w-3.5 h-3.5 ${isSelected ? 'text-indigo-600' : 'text-slate-400'}`} />
                <span>{d.title}</span>
              </button>
            );
          })}
        </div>

        {/* Content Body: Markdown Preview */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 bg-slate-50/50 font-mono text-xs leading-relaxed text-slate-800">
          <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-2xs">
            <pre className="whitespace-pre-wrap font-sans text-xs sm:text-sm text-slate-700 leading-relaxed selection:bg-indigo-100">
              {currentDoc.content}
            </pre>
          </div>
        </div>

        {/* Footer info bar */}
        <div className="px-5 py-3 border-t border-slate-100 bg-white flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-slate-500 shrink-0">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-purple-600" />
            <span>Ready for Google NotebookLM upload · Preserves semantic markdown hierarchy</span>
          </div>
          <a
            href="https://notebooklm.google.com"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-indigo-600 hover:text-indigo-800 font-medium"
          >
            <span>Open notebooklm.google.com</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      </div>
    </div>
  );
};
