import React, { useState, useMemo } from 'react';
import { ClothesItem, WearingReport, ReportClothesJunction, AIReportVersion } from '../types';
import { dataService } from '../services/dataService';
import { fieldDictionaryService, AIAdvisorReportRecord } from '../services/fieldDictionaryService';
import {
  FileText, Search, Filter, Sparkles, Shirt, Calendar, Clock,
  Edit3, Trash2, Copy, Check, Star, ChevronDown, ChevronUp,
  Save, X, Layers, Tag, Eye, Info, RefreshCw
} from 'lucide-react';

interface UnifiedReportItem {
  id: string; // compound unique id
  type: 'macro' | 'clothes' | 'wearing' | 'junction';
  typeLabel: string;
  sourceEntityId: string;
  sourceEntityTitle: string;
  versionId: string;
  title: string;
  timestamp: string;
  model?: string;
  isPrimary?: boolean;
  content: string;
  metaInfo?: string;
  garment?: ClothesItem;
  report?: WearingReport;
  junction?: ReportClothesJunction;
}

interface UnifiedAIReportArchiveProps {
  clothes: ClothesItem[];
  reports: WearingReport[];
  junctions: ReportClothesJunction[];
  onSelectGarment?: (garment: ClothesItem) => void;
  onSelectReport?: (report: WearingReport) => void;
}

export const UnifiedAIReportArchive: React.FC<UnifiedAIReportArchiveProps> = ({
  clothes,
  reports,
  junctions,
  onSelectGarment,
  onSelectReport
}) => {
  const [filterType, setFilterType] = useState<'all' | 'macro' | 'clothes' | 'wearing' | 'junction'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // In-place edit state
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Trigger re-render on data service updates
  const [, setTick] = useState(0);
  const forceUpdate = () => setTick(t => t + 1);

  // Aggregate all reports across all 4 sources
  const allReports = useMemo<UnifiedReportItem[]>(() => {
    const list: UnifiedReportItem[] = [];

    // 1. Macro AI Reports
    const macroReports = fieldDictionaryService.getAiReports();
    macroReports.forEach(m => {
      list.push({
        id: `macro_${m.id}`,
        type: 'macro',
        typeLabel: '全局穿搭顾问 (Macro AI)',
        sourceEntityId: m.id,
        sourceEntityTitle: m.temperature || '全局衣橱顾问',
        versionId: m.id,
        title: m.title || `全局穿搭报告 (${new Date(m.createdAt).toLocaleDateString()})`,
        timestamp: m.createdAt,
        model: m.model || 'gemini-3.6-flash',
        isPrimary: true,
        content: m.advice,
        metaInfo: m.selectedGarmentNames && m.selectedGarmentNames.length > 0
          ? `关联 ${m.selectedGarmentNames.length} 件单品: ${m.selectedGarmentNames.join(', ')}`
          : '全衣橱智能扫描'
      });
    });

    // 2. Clothes AI Reports
    clothes.forEach(g => {
      const gTitle = g['Name'] || `单品 #${g.id}`;
      const reportsList: AIReportVersion[] = Array.isArray(g.aiReports) && g.aiReports.length > 0
        ? g.aiReports
        : (g['AI Clothes Analysis'] ? [{
            id: 'legacy_clothes',
            timestamp: new Date().toISOString(),
            title: 'Report #1',
            model: 'gemini-3.6-flash',
            content: g['AI Clothes Analysis'],
            isPrimary: true
          }] : []);

      reportsList.forEach((v, idx) => {
        list.push({
          id: `clothes_${g.id}_${v.id}`,
          type: 'clothes',
          typeLabel: '单件衣服诊断 (Clothes AI)',
          sourceEntityId: g.id,
          sourceEntityTitle: gTitle,
          versionId: v.id,
          title: v.title || `单品诊断 #${idx + 1}`,
          timestamp: v.timestamp,
          model: v.model,
          isPrimary: v.isPrimary,
          content: v.content,
          metaInfo: `${g['Clothes Type'] || '衣服'} · ${g['material'] || '面料'} · 炎热度: ${g['Hot at noon'] || '未填'}`,
          garment: g
        });
      });
    });

    // 3. Wearing Report AI Reports
    reports.forEach(r => {
      const rTitle = r['Name'] || (r['wear date'] ? `穿着记录 ${new Date(r['wear date']).toLocaleDateString()}` : `会话 #${r.id}`);
      const reportsList: AIReportVersion[] = Array.isArray(r.aiReports) && r.aiReports.length > 0
        ? r.aiReports
        : (r['AI Wearing Analysis'] ? [{
            id: 'legacy_report',
            timestamp: r.created || new Date().toISOString(),
            title: 'Report #1',
            model: 'gemini-3.6-flash',
            content: r['AI Wearing Analysis'],
            isPrimary: true
          }] : []);

      reportsList.forEach((v, idx) => {
        list.push({
          id: `wearing_${r.id}_${v.id}`,
          type: 'wearing',
          typeLabel: '穿着记录诊断 (Wearing AI)',
          sourceEntityId: r.id,
          sourceEntityTitle: rTitle,
          versionId: v.id,
          title: v.title || `穿着诊断 #${idx + 1}`,
          timestamp: v.timestamp,
          model: v.model,
          isPrimary: v.isPrimary,
          content: v.content,
          metaInfo: `穿着日期: ${r['wear date'] ? new Date(r['wear date']).toLocaleDateString() : '未填'} · 肩感: ${r['shoulder feel'] || '未填'}`,
          report: r
        });
      });
    });

    // 4. Junction AI Reports
    junctions.forEach((j, idx) => {
      const matchedGarment = clothes.find(c => {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        return c.id === cId;
      });
      const gTitle = matchedGarment?.['Name'] || j.clothesTitle || `单品 (${j.clothesId || idx + 1})`;
      const reportsList: AIReportVersion[] = Array.isArray(j.aiReports) && j.aiReports.length > 0
        ? j.aiReports
        : (j['AI Junction Analysis'] ? [{
            id: 'legacy_junction',
            timestamp: new Date().toISOString(),
            title: 'Report #1',
            model: 'gemini-3.6-flash',
            content: j['AI Junction Analysis'],
            isPrimary: true
          }] : []);

      reportsList.forEach((v, vIdx) => {
        list.push({
          id: `junction_${j.id}_${v.id}`,
          type: 'junction',
          typeLabel: '单品搭配诊断 (Junction AI)',
          sourceEntityId: j.id,
          sourceEntityTitle: `${gTitle} (Junction #${j.name || idx + 1})`,
          versionId: v.id,
          title: v.title || `搭配诊断 #${vIdx + 1}`,
          timestamp: v.timestamp,
          model: v.model,
          isPrimary: v.isPrimary,
          content: v.content,
          metaInfo: `时长: ${((j['duration'] || 0) / 3600).toFixed(1)}h · 状态: ${j['wash stated'] || 'normal'}`,
          junction: j,
          garment: matchedGarment
        });
      });
    });

    // Sort by timestamp descending
    return list.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  }, [clothes, reports, junctions]);

  // Filtered reports
  const filteredReports = useMemo(() => {
    return allReports.filter(item => {
      if (filterType !== 'all' && item.type !== filterType) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchTitle = item.title.toLowerCase().includes(q);
        const matchSource = item.sourceEntityTitle.toLowerCase().includes(q);
        const matchContent = item.content.toLowerCase().includes(q);
        const matchMeta = item.metaInfo?.toLowerCase().includes(q) || false;
        if (!matchTitle && !matchSource && !matchContent && !matchMeta) return false;
      }
      return true;
    });
  }, [allReports, filterType, searchQuery]);

  const handleStartEdit = (item: UnifiedReportItem) => {
    setEditingId(item.id);
    setEditTitle(item.title);
    setEditContent(item.content);
    setExpandedId(item.id);
  };

  const handleSaveEdit = async (item: UnifiedReportItem) => {
    if (!editContent.trim()) return;

    if (item.type === 'macro') {
      fieldDictionaryService.updateAiReport(item.sourceEntityId, {
        title: editTitle.trim(),
        advice: editContent.trim()
      });
    } else if (item.type === 'clothes') {
      await dataService.updateGarmentAIReportVersion(item.sourceEntityId, item.versionId, {
        title: editTitle.trim(),
        content: editContent.trim()
      });
    } else if (item.type === 'wearing') {
      await dataService.updateAIReportVersion(item.sourceEntityId, item.versionId, {
        title: editTitle.trim(),
        content: editContent.trim()
      });
    } else if (item.type === 'junction') {
      await dataService.updateJunctionAIReportVersion(item.sourceEntityId, item.versionId, {
        title: editTitle.trim(),
        content: editContent.trim()
      });
    }

    setEditingId(null);
    forceUpdate();
  };

  const handleDelete = async (item: UnifiedReportItem) => {
    if (!confirm(`确定要彻底删除该份报告吗？\n[${item.typeLabel}] ${item.title}`)) return;

    if (item.type === 'macro') {
      fieldDictionaryService.deleteAiReport(item.sourceEntityId);
    } else if (item.type === 'clothes') {
      await dataService.deleteGarmentAIReportVersion(item.sourceEntityId, item.versionId);
    } else if (item.type === 'wearing') {
      await dataService.deleteAIReportVersion(item.sourceEntityId, item.versionId);
    } else if (item.type === 'junction') {
      await dataService.deleteJunctionAIReportVersion(item.sourceEntityId, item.versionId);
    }

    if (expandedId === item.id) setExpandedId(null);
    if (editingId === item.id) setEditingId(null);
    forceUpdate();
  };

  const handleSetPrimary = async (item: UnifiedReportItem) => {
    if (item.type === 'clothes') {
      await dataService.setPrimaryGarmentAIReportVersion(item.sourceEntityId, item.versionId);
    } else if (item.type === 'wearing') {
      await dataService.setPrimaryAIReportVersion(item.sourceEntityId, item.versionId);
    } else if (item.type === 'junction') {
      await dataService.setPrimaryJunctionAIReportVersion(item.sourceEntityId, item.versionId);
    }
    forceUpdate();
  };

  const handleCopy = (content: string, id: string) => {
    navigator.clipboard.writeText(content);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const countsByType = {
    all: allReports.length,
    macro: allReports.filter(r => r.type === 'macro').length,
    clothes: allReports.filter(r => r.type === 'clothes').length,
    wearing: allReports.filter(r => r.type === 'wearing').length,
    junction: allReports.filter(r => r.type === 'junction').length,
  };

  return (
    <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-4">
      {/* Top Title Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100">
        <div>
          <div className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-purple-600" />
            <h3 className="text-base font-bold text-slate-900">
              全维度 AI 报告管理归档中心 (All AI Reports Archive)
            </h3>
            <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-purple-100 text-purple-800 font-mono">
              共 {allReports.length} 份报告
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            聚合全局穿搭顾问 (Macro)、单件衣服 (Clothes)、穿着记录 (Wearing) 与搭配 (Junction) 的全部多版本报告，支持集中查阅、编辑、删除与主报告设定。
          </p>
        </div>

        {/* Search Input */}
        <div className="relative min-w-[220px]">
          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索报告标题、衣物或内容..."
            className="w-full pl-9 pr-3 py-1.5 rounded-xl border border-slate-200 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-hidden focus:ring-2 focus:ring-purple-500 bg-slate-50"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
            >
              <X className="w-3 h-3" />
            </button>
          )}
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex flex-wrap items-center gap-1.5 p-1 bg-slate-100/80 rounded-2xl border border-slate-200/60 text-xs">
        <button
          type="button"
          onClick={() => setFilterType('all')}
          className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
            filterType === 'all' ? 'bg-white text-purple-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          全部报告 ({countsByType.all})
        </button>
        <button
          type="button"
          onClick={() => setFilterType('macro')}
          className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
            filterType === 'macro' ? 'bg-white text-purple-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          全局顾问 ({countsByType.macro})
        </button>
        <button
          type="button"
          onClick={() => setFilterType('clothes')}
          className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
            filterType === 'clothes' ? 'bg-white text-purple-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          单件衣服 ({countsByType.clothes})
        </button>
        <button
          type="button"
          onClick={() => setFilterType('wearing')}
          className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
            filterType === 'wearing' ? 'bg-white text-purple-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          穿着记录 ({countsByType.wearing})
        </button>
        <button
          type="button"
          onClick={() => setFilterType('junction')}
          className={`px-3 py-1.5 rounded-xl font-bold transition-all cursor-pointer ${
            filterType === 'junction' ? 'bg-white text-purple-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          单品搭配 ({countsByType.junction})
        </button>
      </div>

      {/* Reports List */}
      {filteredReports.length === 0 ? (
        <div className="p-12 rounded-2xl bg-slate-50 border border-dashed border-slate-300 text-center space-y-2">
          <p className="text-xs text-slate-600 font-semibold">
            {allReports.length === 0 ? '暂无任何 AI 分析报告' : '未找到匹配的 AI 报告记录'}
          </p>
          <p className="text-[11px] text-slate-400 max-w-md mx-auto">
            {allReports.length === 0
              ? '您可以在「全局穿搭顾问」、「单件衣服诊断」或穿戴记录的「AI 单品搭配诊断」中生成多版本报告，所有报告将永久保存于此。'
              : '请尝试更换搜索关键字或切换上方筛选标签。'}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredReports.map((item) => {
            const isExpanded = expandedId === item.id;
            const isEditing = editingId === item.id;

            const badgeBg =
              item.type === 'macro'
                ? 'bg-amber-100 text-amber-900 border-amber-200'
                : item.type === 'clothes'
                ? 'bg-purple-100 text-purple-900 border-purple-200'
                : item.type === 'wearing'
                ? 'bg-indigo-100 text-indigo-900 border-indigo-200'
                : 'bg-emerald-100 text-emerald-900 border-emerald-200';

            return (
              <div
                key={item.id}
                className="rounded-2xl bg-white border border-slate-200/90 shadow-2xs overflow-hidden transition-all hover:border-purple-300/80"
              >
                {/* Header Summary Row */}
                <div
                  onClick={() => setExpandedId(isExpanded ? null : item.id)}
                  className="p-3.5 sm:px-4 flex items-center justify-between gap-3 hover:bg-slate-50/70 cursor-pointer select-none transition-colors"
                >
                  <div className="min-w-0 flex items-center gap-2.5 flex-wrap">
                    <span className={`px-2 py-0.5 rounded-lg text-[10px] font-bold border font-mono ${badgeBg}`}>
                      {item.typeLabel.split(' ')[0]}
                    </span>

                    <span className="font-bold text-xs sm:text-sm text-slate-800 truncate">
                      {item.title}
                    </span>

                    <span className="text-[11px] text-purple-700 font-semibold truncate">
                      {item.sourceEntityTitle}
                    </span>

                    <span className="text-[10px] font-mono text-slate-400">
                      {new Date(item.timestamp).toLocaleString([], {
                        year: 'numeric',
                        month: 'numeric',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </span>

                    {item.isPrimary && (
                      <span className="inline-flex items-center gap-1 px-1.5 py-0.2 rounded text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 font-mono">
                        <Star className="w-2.5 h-2.5 fill-amber-500 text-amber-500" />
                        主版本
                      </span>
                    )}

                    {item.model && (
                      <span className="px-1.5 py-0.2 rounded text-[10px] font-mono bg-slate-100 text-slate-600">
                        {item.model}
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0" onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      onClick={() => handleCopy(item.content, item.id)}
                      className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
                      title="复制报告正文"
                    >
                      {copiedId === item.id ? (
                        <Check className="w-3.5 h-3.5 text-emerald-600" />
                      ) : (
                        <Copy className="w-3.5 h-3.5" />
                      )}
                    </button>

                    <button
                      type="button"
                      onClick={() => handleStartEdit(item)}
                      className="p-1.5 text-slate-400 hover:text-purple-700 hover:bg-purple-50 rounded-lg transition-colors cursor-pointer"
                      title="编辑报告内容"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                    </button>

                    <button
                      type="button"
                      onClick={() => handleDelete(item)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                      title="删除此报告"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>

                    <button
                      type="button"
                      onClick={() => setExpandedId(isExpanded ? null : item.id)}
                      className="p-1 text-slate-400 hover:text-slate-600"
                    >
                      {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {/* Expanded Details Body */}
                {isExpanded && (
                  <div className="p-4 sm:p-5 border-t border-slate-100 bg-slate-50/50 space-y-3.5">
                    {item.metaInfo && (
                      <div className="p-2.5 rounded-xl bg-white border border-slate-200/80 text-[11px] text-slate-600 flex items-center justify-between gap-2">
                        <div className="flex items-center gap-1.5 truncate">
                          <Info className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                          <span className="truncate">{item.metaInfo}</span>
                        </div>
                        {item.garment && onSelectGarment && (
                          <button
                            type="button"
                            onClick={() => onSelectGarment(item.garment!)}
                            className="text-purple-700 font-bold hover:underline shrink-0 text-[11px]"
                          >
                            打开单品卡片 →
                          </button>
                        )}
                        {item.report && onSelectReport && (
                          <button
                            type="button"
                            onClick={() => onSelectReport(item.report!)}
                            className="text-indigo-700 font-bold hover:underline shrink-0 text-[11px]"
                          >
                            打开穿着记录 →
                          </button>
                        )}
                      </div>
                    )}

                    {isEditing ? (
                      <div className="space-y-3 bg-white p-4 rounded-2xl border border-purple-200">
                        <div>
                          <label className="block text-[11px] font-bold text-slate-700 uppercase tracking-wider mb-1">
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
                          <label className="block text-[11px] font-bold text-slate-700 uppercase tracking-wider mb-1">
                            报告正文 (Markdown):
                          </label>
                          <textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            rows={10}
                            className="w-full p-3 rounded-xl border border-slate-300 text-xs font-mono text-slate-800 leading-relaxed focus:ring-2 focus:ring-purple-500 focus:outline-hidden"
                          />
                        </div>
                        <div className="flex items-center justify-end gap-2 pt-1">
                          <button
                            type="button"
                            onClick={() => setEditingId(null)}
                            className="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
                          >
                            取消
                          </button>
                          <button
                            type="button"
                            onClick={() => handleSaveEdit(item)}
                            className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition-all shadow-xs"
                          >
                            <Save className="w-3.5 h-3.5" />
                            <span>保存更改</span>
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="p-4 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-3">
                        <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-2">
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-bold text-slate-800">
                              {item.title}
                            </span>
                            {!item.isPrimary && item.type !== 'macro' && (
                              <button
                                type="button"
                                onClick={() => handleSetPrimary(item)}
                                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-white text-slate-600 hover:text-amber-600 hover:border-amber-200 border border-slate-200 transition-colors cursor-pointer"
                              >
                                <Star className="w-2.5 h-2.5" />
                                设为主要版本
                              </button>
                            )}
                          </div>

                          <button
                            type="button"
                            onClick={() => handleStartEdit(item)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-purple-700 hover:bg-purple-50 transition-colors cursor-pointer"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                            <span>编辑</span>
                          </button>
                        </div>

                        <div className="text-xs sm:text-sm text-slate-800 leading-relaxed whitespace-pre-wrap font-sans max-h-96 overflow-y-auto">
                          {item.content}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
