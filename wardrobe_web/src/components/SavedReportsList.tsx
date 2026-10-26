import React, { useState } from 'react';
import { AIAdvisorReportRecord, fieldDictionaryService } from '../services/fieldDictionaryService';
import {
  FileText, Calendar, Trash2, Edit2, Check, X, ChevronDown, ChevronUp,
  Sparkles, Tag, Clock, Droplets, ThermometerSun, AlertCircle, Copy
} from 'lucide-react';

interface SavedReportsListProps {
  reports: AIAdvisorReportRecord[];
  onSelectGarments?: (ids: string[]) => void;
}

export const SavedReportsList: React.FC<SavedReportsListProps> = ({ reports, onSelectGarments }) => {
  const [expandedId, setExpandedId] = useState<string | null>(reports[0]?.id || null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editAdvice, setEditAdvice] = useState('');
  const [editNotes, setEditNotes] = useState('');
  const [copySuccess, setCopySuccess] = useState<string | null>(null);

  if (reports.length === 0) {
    return (
      <div className="p-8 rounded-2xl bg-slate-50/60 border border-dashed border-slate-200 text-center space-y-1.5">
        <p className="text-xs font-semibold text-slate-700">暂无已保存的独立 AI 穿搭与体感分析报告</p>
        <p className="text-[11px] text-slate-400">
          在上方配置好天气（如仅选 Hot at noon）、场合、优先级或衣服后点击「获取穿搭与体感分析」，生成的报告将自动独立存档于此，并支持查看、编辑与删除。
        </p>
      </div>
    );
  }

  const handleStartEdit = (rep: AIAdvisorReportRecord) => {
    setEditingId(rep.id);
    setEditTitle(rep.title || `分析报告 (${new Date(rep.createdAt).toLocaleDateString()})`);
    setEditAdvice(rep.advice);
    setEditNotes(rep.userNotes || '');
  };

  const handleSaveEdit = (id: string) => {
    fieldDictionaryService.updateAiReport(id, {
      title: editTitle.trim(),
      advice: editAdvice.trim(),
      userNotes: editNotes.trim()
    });
    setEditingId(null);
  };

  const handleDelete = (id: string, title?: string) => {
    if (window.confirm(`确定要删除此 AI 报告记录吗？${title ? `(${title})` : ''}`)) {
      fieldDictionaryService.deleteAiReport(id);
    }
  };

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopySuccess(id);
    setTimeout(() => setCopySuccess(null), 2000);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <FileText className="w-4 h-4 text-purple-600" />
          <h4 className="text-xs sm:text-sm font-bold text-slate-800">
            已归档的独立 AI 穿搭与体感报告记录 ({reports.length})
          </h4>
        </div>
        <span className="text-[10px] font-mono text-slate-400">
          包含在完整 ZIP 备份中
        </span>
      </div>

      <div className="space-y-2.5">
        {reports.map((rep) => {
          const isExpanded = expandedId === rep.id;
          const isEditing = editingId === rep.id;

          return (
            <div
              key={rep.id}
              className="rounded-2xl bg-white border border-slate-200/90 shadow-2xs overflow-hidden transition-all"
            >
              {/* Card Header Bar */}
              <div
                onClick={() => setExpandedId(isExpanded ? null : rep.id)}
                className="p-3.5 sm:px-4 flex items-center justify-between gap-3 hover:bg-slate-50/60 cursor-pointer select-none"
              >
                <div className="min-w-0 flex items-center gap-2.5 flex-wrap">
                  <span className="w-2 h-2 rounded-full bg-purple-500 shrink-0" />
                  <span className="font-bold text-xs sm:text-sm text-slate-800 truncate">
                    {rep.title || `AI 穿搭体感报告 #${rep.id.slice(-4)}`}
                  </span>
                  <span className="text-[10px] font-mono text-slate-400">
                    {new Date(rep.createdAt).toLocaleString()}
                  </span>

                  {/* Condition Badges */}
                  <div className="flex items-center gap-1.5 flex-wrap">
                    {rep.temperature && rep.temperature !== 'none' && (
                      <span className="px-2 py-0.5 rounded-md bg-amber-50 text-amber-800 text-[10px] font-medium border border-amber-200/60">
                        {rep.temperature}
                      </span>
                    )}
                    {rep.occasion && rep.occasion !== 'none' && (
                      <span className="px-2 py-0.5 rounded-md bg-blue-50 text-blue-800 text-[10px] font-medium border border-blue-200/60 truncate max-w-[120px]">
                        {rep.occasion}
                      </span>
                    )}
                    {rep.physicalPriority && rep.physicalPriority !== 'none' && (
                      <span className="px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-800 text-[10px] font-medium border border-emerald-200/60 truncate max-w-[140px]">
                        {rep.physicalPriority}
                      </span>
                    )}
                    <span className="px-1.5 py-0.5 rounded-md bg-slate-100 text-slate-600 text-[10px] font-mono">
                      {rep.selectedGarmentNames.length > 0
                        ? `${rep.selectedGarmentNames.length}件指定衣服`
                        : '全衣橱推荐'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0 text-slate-400">
                  <span className="text-[11px] hidden sm:inline text-slate-500">
                    {isExpanded ? '收起' : '展开'}
                  </span>
                  {isExpanded ? <ChevronUp className="w-4 h-4 text-purple-600" /> : <ChevronDown className="w-4 h-4" />}
                </div>
              </div>

              {/* Card Body */}
              {isExpanded && (
                <div className="p-4 sm:p-5 border-t border-slate-100 bg-slate-50/30 space-y-3.5">
                  {isEditing ? (
                    <div className="space-y-3 bg-white p-4 rounded-xl border border-purple-200 shadow-2xs">
                      <div>
                        <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1">
                          报告标题 (Title)
                        </label>
                        <input
                          type="text"
                          value={editTitle}
                          onChange={(e) => setEditTitle(e.target.value)}
                          className="w-full px-3 py-1.5 text-xs rounded-xl border border-slate-300 font-semibold"
                        />
                      </div>

                      <div>
                        <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1">
                          AI 报告正文 (Markdown Content)
                        </label>
                        <textarea
                          rows={8}
                          value={editAdvice}
                          onChange={(e) => setEditAdvice(e.target.value)}
                          className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 font-mono leading-relaxed"
                        />
                      </div>

                      <div>
                        <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1">
                          个人心得或追记备注 (User Notes)
                        </label>
                        <input
                          type="text"
                          value={editNotes}
                          onChange={(e) => setEditNotes(e.target.value)}
                          placeholder="例如：后来下班时试了确实很凉爽，下次可以再搭这件..."
                          className="w-full px-3 py-1.5 text-xs rounded-xl border border-slate-300"
                        />
                      </div>

                      <div className="flex items-center justify-end gap-2 pt-1">
                        <button
                          type="button"
                          onClick={() => setEditingId(null)}
                          className="px-3 py-1.5 rounded-lg border border-slate-300 text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
                        >
                          取消
                        </button>
                        <button
                          type="button"
                          onClick={() => handleSaveEdit(rep.id)}
                          className="px-4 py-1.5 rounded-lg bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold shadow-xs cursor-pointer"
                        >
                          保存修改
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      {/* Inquiry Context */}
                      {rep.inquiryPrompt && (
                        <div className="p-2.5 rounded-xl bg-purple-50/70 border border-purple-100 text-xs text-purple-900">
                          <span className="font-bold">咨询诉求: </span>
                          <span>{rep.inquiryPrompt}</span>
                        </div>
                      )}

                      {/* Selected Clothes Names */}
                      {rep.selectedGarmentNames.length > 0 && (
                        <div className="flex flex-wrap gap-1.5 items-center">
                          <span className="text-[11px] text-slate-500 font-medium">评估单品:</span>
                          {rep.selectedGarmentNames.map((name, i) => (
                            <span key={i} className="px-2 py-0.5 rounded-md bg-white border border-slate-200 text-slate-700 text-[10px] font-semibold">
                              {name}
                            </span>
                          ))}
                        </div>
                      )}

                      {/* Advice Markdown text */}
                      <div className="p-4 rounded-xl bg-white border border-slate-200 text-xs text-slate-800 leading-relaxed whitespace-pre-wrap font-sans">
                        {rep.advice}
                      </div>

                      {/* User Notes */}
                      {rep.userNotes && (
                        <div className="p-2.5 rounded-xl bg-amber-50/80 border border-amber-200 text-xs text-amber-900">
                          <span className="font-bold">个人追记: </span>
                          <span>{rep.userNotes}</span>
                        </div>
                      )}

                      {/* Actions */}
                      <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => handleCopy(rep.advice, rep.id)}
                            className="px-2.5 py-1 rounded-lg border border-slate-200 hover:bg-slate-100 text-slate-600 text-xs font-medium flex items-center gap-1 cursor-pointer transition-colors"
                          >
                            <Copy className="w-3.5 h-3.5" />
                            <span>{copySuccess === rep.id ? '已复制' : '复制正文'}</span>
                          </button>
                        </div>

                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => handleStartEdit(rep)}
                            className="px-2.5 py-1 rounded-lg text-purple-700 hover:bg-purple-50 text-xs font-bold flex items-center gap-1 cursor-pointer transition-colors"
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                            <span>编辑</span>
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDelete(rep.id, rep.title)}
                            className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg cursor-pointer transition-colors"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
