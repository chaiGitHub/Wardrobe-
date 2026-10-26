import React, { useState } from 'react';
import { ReportClothesJunction, ClothesItem, WearingReport, AIReportVersion } from '../types';
import { dataService } from '../services/dataService';
import { getCustomApiKey, getSelectedModel } from '../services/aiSettings';
import { fieldDictionaryService } from '../services/fieldDictionaryService';
import {
  X, Sparkles, RefreshCw, Star, Edit3, Trash2, Copy, Check, Save,
  AlertCircle, Shirt, Calendar, Clock, Droplets, Info, ArrowLeft
} from 'lucide-react';

interface JunctionAIModalProps {
  isOpen: boolean;
  junction: ReportClothesJunction | null;
  garment?: ClothesItem | null;
  report: WearingReport | null;
  onClose: () => void;
  onUpdateJunction?: (updated: ReportClothesJunction) => void;
}

export const JunctionAIModal: React.FC<JunctionAIModalProps> = ({
  isOpen,
  junction,
  garment,
  report,
  onClose,
  onUpdateJunction
}) => {
  const garmentTitle = garment?.['Name'] || junction?.clothesTitle || 'Garment';
  const reportTitle = report?.['Name'] || 'Wearing Session';
  const durationHours = (((junction?.['duration'] || 0)) / 3600).toFixed(1);

  // Derive reports array
  const junctionAiReports: AIReportVersion[] = Array.isArray(junction?.aiReports) && junction.aiReports.length > 0
    ? junction.aiReports
    : (junction?.['AI Junction Analysis'] ? [{
        id: 'junction_initial',
        timestamp: new Date().toISOString(),
        title: 'Report #1',
        model: 'gemini-3.6-flash',
        content: junction['AI Junction Analysis'],
        isPrimary: true
      }] : []);

  const [activeVersionId, setActiveVersionId] = useState<string | null>(() => {
    const primary = junctionAiReports.find(v => v.isPrimary);
    return primary?.id || junctionAiReports[0]?.id || null;
  });

  const [analyzing, setAnalyzing] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [copiedVersionId, setCopiedVersionId] = useState<string | null>(null);

  const activeVersion = junctionAiReports.find(v => v.id === activeVersionId) || junctionAiReports[0] || null;

  const handleRunJunctionAnalysis = async () => {
    setAnalyzing(true);
    setErrorMsg('');
    try {
      const dictionaryPrompt = `${fieldDictionaryService.buildDictionaryPrompt('junction')}\n${fieldDictionaryService.buildDictionaryPrompt('clothes')}\n${fieldDictionaryService.buildDictionaryPrompt('wearingReport')}`;
      const res = await fetch('/api/ai/analyze-junction', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          junction,
          garment: garment || undefined,
          report,
          apiKey: getCustomApiKey(),
          model: getSelectedModel(),
          dictionaryPrompt
        })
      });

      const data = await res.json();
      if (res.ok && data.analysis) {
        const updated = await dataService.addJunctionAIReportVersion(junction.id, {
          content: data.analysis,
          title: `Report #${junctionAiReports.length + 1}`,
          model: data.model || getSelectedModel(),
          isPrimary: junctionAiReports.length === 0
        });

        if (updated) {
          if (onUpdateJunction) onUpdateJunction(updated);
          const newVer = updated.aiReports?.[0];
          if (newVer) {
            setActiveVersionId(newVer.id);
          }
        }
      } else {
        setErrorMsg(data.error || 'Failed to generate junction analysis.');
      }
    } catch (e: any) {
      setErrorMsg(e.message || 'Error communicating with AI service.');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleStartEdit = (version: AIReportVersion) => {
    setIsEditing(true);
    setEditTitle(version.title || '');
    setEditContent(version.content || '');
  };

  const handleSaveEdit = async (versionId: string) => {
    if (!editContent.trim()) return;
    const updated = await dataService.updateJunctionAIReportVersion(junction.id, versionId, {
      title: editTitle.trim(),
      content: editContent.trim()
    });
    if (updated && onUpdateJunction) {
      onUpdateJunction(updated);
    }
    setIsEditing(false);
  };

  const handleSetPrimary = async (versionId: string) => {
    const updated = await dataService.setPrimaryJunctionAIReportVersion(junction.id, versionId);
    if (updated) {
      if (onUpdateJunction) onUpdateJunction(updated);
      setActiveVersionId(versionId);
    }
  };

  const handleDeleteVersion = async (versionId: string) => {
    if (!confirm('确定要删除此版本的单品穿着搭配诊断报告吗？')) return;
    const updated = await dataService.deleteJunctionAIReportVersion(junction.id, versionId);
    if (updated) {
      if (onUpdateJunction) onUpdateJunction(updated);
      const remaining = updated.aiReports || [];
      const newActive = remaining.find(v => v.isPrimary) || remaining[0];
      setActiveVersionId(newActive?.id || null);
    }
    setIsEditing(false);
  };

  const handleCopy = (content: string, versionId: string) => {
    navigator.clipboard.writeText(content);
    setCopiedVersionId(versionId);
    setTimeout(() => setCopiedVersionId(null), 2000);
  };

  if (!isOpen || !junction || !report) return null;

  return (
    <div className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto">
      {/* Header */}
      <div className="px-3 sm:px-8 py-2.5 sm:py-3.5 bg-linear-to-r from-purple-700 via-indigo-700 to-purple-800 text-white flex items-center justify-between shrink-0 shadow-xs w-full max-w-full overflow-x-hidden">
        <div className="flex items-center gap-2 sm:gap-3 min-w-0">
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 -ml-1 sm:ml-0 rounded-xl text-white/80 hover:text-white hover:bg-white/10 transition-colors cursor-pointer shrink-0"
            title="Back"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="min-w-0">
            <div className="flex items-center gap-2 min-w-0">
              <h3 className="text-sm sm:text-base font-bold tracking-tight truncate">
                单品穿着体感与搭配诊断报告 (Junction AI)
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/20 text-purple-100 font-mono hidden sm:inline shrink-0">
                {junctionAiReports.length > 0 ? `${junctionAiReports.length} 份版本` : '多版本独立存档'}
              </span>
            </div>
            <p className="text-[11px] sm:text-xs text-purple-200 font-sans truncate max-w-[220px] sm:max-w-md">
              针对「{garmentTitle}」在本次「{reportTitle}」中的专项人体工学诊断
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="p-1.5 text-white/80 hover:text-white hover:bg-white/10 rounded-xl transition-colors cursor-pointer shrink-0"
          title="关闭"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

        {/* Pair Summary Bar */}
        <div className="px-3 sm:px-6 py-2.5 sm:py-3 bg-slate-50 border-b border-slate-200/80 flex flex-wrap items-center justify-between gap-2 text-xs shrink-0 w-full max-w-full overflow-x-hidden">
          <div className="flex items-center gap-4 flex-wrap">
            <div className="flex items-center gap-1.5 text-slate-700 font-medium">
              <Shirt className="w-4 h-4 text-purple-600" />
              <span>{garmentTitle}</span>
              {garment?.['material'] && (
                <span className="text-[11px] font-mono text-slate-400">({garment['material']})</span>
              )}
            </div>
            <div className="flex items-center gap-1.5 text-slate-700 font-medium">
              <Calendar className="w-4 h-4 text-purple-600" />
              <span>{report['wear date'] ? new Date(report['wear date']).toLocaleDateString() : 'Wear Date'}</span>
            </div>
            <div className="flex items-center gap-1.5 text-slate-700 font-medium">
              <Clock className="w-4 h-4 text-purple-600" />
              <span>{durationHours} 小时</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                junction['wash stated'] === 'need wash' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
              }`}>
                {junction['wash stated'] === 'need wash' ? '⚠️ 需要洗涤' : '✨ 已清洗'}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={handleRunJunctionAnalysis}
            disabled={analyzing}
            className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition-all shadow-xs disabled:opacity-60 cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${analyzing ? 'animate-spin' : ''}`} />
            <span>{analyzing ? '生成中...' : (junctionAiReports.length > 0 ? '+ 生成新版本' : '生成单品穿着诊断')}</span>
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden p-3.5 sm:p-6 space-y-4 w-full max-w-full">
          {errorMsg && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs space-y-2">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-rose-500" />
                <span className="font-medium">{errorMsg}</span>
              </div>
              {(errorMsg.includes('prepayment') || errorMsg.includes('402')) && (
                <div className="pt-1 text-[11px] text-slate-700 flex flex-wrap items-center gap-2">
                  <span className="font-bold text-slate-900">提示:</span>
                  <a href="https://ai.studio/projects" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                    在 AI Studio 管理配额与充值 ↗
                  </a>
                  <span>或</span>
                  <a href="https://aistudio.google.com/apikey" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                    获取个人 API Key (免重复输入)
                  </a>
                </div>
              )}
            </div>
          )}

          {/* Versions Tabs */}
          {junctionAiReports.length > 0 && (
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 pt-1 border-b border-slate-100">
              <span className="text-[11px] font-bold text-slate-400 shrink-0 mr-1 font-mono uppercase">
                版本列表 ({junctionAiReports.length}):
              </span>
              {junctionAiReports.map((v, idx) => {
                const isActive = activeVersion?.id === v.id;
                return (
                  <button
                    key={v.id}
                    type="button"
                    onClick={() => {
                      setActiveVersionId(v.id);
                      setIsEditing(false);
                    }}
                    className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                      isActive
                        ? 'bg-purple-600 text-white shadow-xs font-bold'
                        : 'bg-slate-100 text-slate-600 hover:bg-purple-100 hover:text-purple-900 border border-slate-200'
                    }`}
                  >
                    <span>{v.title || `Report #${idx + 1}`}</span>
                    {v.isPrimary && (
                      <Star className={`w-2.5 h-2.5 ${isActive ? 'fill-amber-300 text-amber-300' : 'fill-amber-400 text-amber-500'}`} />
                    )}
                  </button>
                );
              })}
            </div>
          )}

          {/* Active Version Card */}
          {activeVersion ? (
            <div className="rounded-2xl bg-white border border-slate-200 shadow-2xs overflow-hidden">
              {/* Header Toolbar */}
              <div className="p-3.5 bg-slate-50 border-b border-slate-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-xs font-bold text-slate-800">
                    {activeVersion.title || 'Report Version'}
                  </span>
                  <span className="text-[11px] font-mono text-slate-400">
                    {new Date(activeVersion.timestamp).toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-slate-200/70 text-slate-700">
                    {activeVersion.model || 'gemini-3.6-flash'}
                  </span>

                  {activeVersion.isPrimary ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 font-mono">
                      <Star className="w-2.5 h-2.5 fill-amber-500 text-amber-500" />
                      默认主报告
                    </span>
                  ) : (
                    <button
                      type="button"
                      onClick={() => handleSetPrimary(activeVersion.id)}
                      className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-white text-slate-600 hover:text-amber-600 hover:border-amber-200 border border-slate-200 transition-colors cursor-pointer"
                    >
                      <Star className="w-2.5 h-2.5" />
                      设为主报告
                    </button>
                  )}
                </div>

                <div className="flex items-center gap-1.5 self-end sm:self-auto">
                  {!isEditing ? (
                    <>
                      <button
                        type="button"
                        onClick={() => handleStartEdit(activeVersion)}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-200 transition-colors cursor-pointer"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                        <span>编辑</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => handleCopy(activeVersion.content, activeVersion.id)}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-200 transition-colors cursor-pointer"
                      >
                        {copiedVersionId === activeVersion.id ? (
                          <>
                            <Check className="w-3.5 h-3.5 text-emerald-600" />
                            <span className="text-emerald-600 font-bold">已复制</span>
                          </>
                        ) : (
                          <>
                            <Copy className="w-3.5 h-3.5" />
                            <span>复制</span>
                          </>
                        )}
                      </button>

                      <button
                        type="button"
                        onClick={() => handleDeleteVersion(activeVersion.id)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                        title="删除此报告"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </>
                  ) : (
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => handleSaveEdit(activeVersion.id)}
                        className="inline-flex items-center gap-1 px-3 py-1 rounded-lg text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white transition-colors cursor-pointer"
                      >
                        <Save className="w-3.5 h-3.5" />
                        <span>保存</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setIsEditing(false)}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-slate-500 hover:bg-slate-200 transition-colors cursor-pointer"
                      >
                        <X className="w-3.5 h-3.5" />
                        <span>取消</span>
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* Version Content */}
              <div className="p-5">
                {isEditing ? (
                  <div className="space-y-3">
                    <div>
                      <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                        报告标题:
                      </label>
                      <input
                        type="text"
                        value={editTitle}
                        onChange={(e) => setEditTitle(e.target.value)}
                        className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs font-bold text-slate-900 focus:ring-2 focus:ring-purple-500 focus:outline-hidden"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                        报告正文 (Markdown):
                      </label>
                      <textarea
                        value={editContent}
                        onChange={(e) => setEditContent(e.target.value)}
                        rows={12}
                        className="w-full p-3.5 rounded-xl border border-slate-300 text-xs font-mono text-slate-800 leading-relaxed focus:ring-2 focus:ring-purple-500 focus:outline-hidden resize-y"
                      />
                    </div>
                  </div>
                ) : (
                  <div className="text-xs sm:text-sm text-slate-800 leading-relaxed max-h-96 overflow-y-auto whitespace-pre-wrap font-sans">
                    {activeVersion.content}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="p-8 rounded-2xl bg-slate-50 border border-dashed border-slate-300 text-center space-y-2">
              <p className="text-xs text-slate-600 font-semibold">
                该单品在本次穿着中暂未生成独立的搭配体感诊断
              </p>
              <p className="text-[11px] text-slate-400 max-w-md mx-auto">
                点击上方 <strong>“生成单品穿着诊断”</strong>，AI 将结合本场穿着环境、身体出汗情况与该单品的物理工学指标生成针对性评估。
              </p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] bg-slate-50 border-t border-slate-200 flex items-center justify-end shrink-0 w-full max-w-full shadow-xs">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-200 hover:bg-slate-300 text-slate-800 transition-colors cursor-pointer"
          >
            关闭
          </button>
        </div>
      </div>
  );
};
