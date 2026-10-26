import React, { useState, useEffect, useMemo } from 'react';
import {
  fieldDictionaryService,
  FieldDefinitionItem,
  FieldValueDefinition,
  DEFAULT_FIELD_DEFINITIONS
} from '../services/fieldDictionaryService';
import { useAiGenerationPaused } from '../services/aiSettings';
import { dataService } from '../services/dataService';
import {
  BookOpen, ChevronDown, ChevronUp, Sparkles, Plus, Edit2, Trash2,
  RotateCcw, Check, X, Info, Tag, Shirt, FileText, Link2, ArrowLeft,
  History, RefreshCw, Search, LayoutGrid, Table, AlignLeft, MessageSquare,
  Maximize2, Minimize2
} from 'lucide-react';

interface FieldDictionaryDrawerProps {
  initialOpen?: boolean;
}

export const FieldDictionaryDrawer: React.FC<FieldDictionaryDrawerProps> = ({ initialOpen = false }) => {
  const [isOpen, setIsOpen] = useState(initialOpen);
  const isAiPaused = useAiGenerationPaused();
  const [activeCategory, setActiveCategory] = useState<'all' | 'clothes' | 'wearingReport' | 'junction'>('all');
  const [definitions, setDefinitions] = useState<FieldDefinitionItem[]>(() => fieldDictionaryService.getDefinitions());
  const [searchQuery, setSearchQuery] = useState('');
  const [typicalValuesLayout, setTypicalValuesLayout] = useState<'card' | 'table'>('card');
  const [editingItem, setEditingItem] = useState<FieldDefinitionItem | null>(null);
  const [isAddingNew, setIsAddingNew] = useState(false);
  const [successToast, setSuccessToast] = useState('');
  const [expandedMeaningCnMap, setExpandedMeaningCnMap] = useState<Record<number, boolean>>({});

  useEffect(() => {
    const unsub = fieldDictionaryService.subscribe(() => {
      setDefinitions(fieldDictionaryService.getDefinitions());
    });
    return unsub;
  }, []);

  const triggerToast = (msg: string) => {
    setSuccessToast(msg);
    setTimeout(() => setSuccessToast(''), 3000);
  };

  const filteredDefs = useMemo(() => {
    let list = activeCategory === 'all'
      ? definitions
      : definitions.filter(d => d.category === activeCategory);

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      list = list.filter(d =>
        d.fieldNameEn.toLowerCase().includes(q) ||
        (d.fieldNameCn && d.fieldNameCn.toLowerCase().includes(q)) ||
        (d.descriptionCn && d.descriptionCn.toLowerCase().includes(q)) ||
        (d.descriptionEn && d.descriptionEn.toLowerCase().includes(q)) ||
        (d.typicalValues && d.typicalValues.some(tv => tv.valueEn.toLowerCase().includes(q) || tv.valueCn.toLowerCase().includes(q) || tv.meaningCn.toLowerCase().includes(q)))
      );
    }
    return list;
  }, [definitions, activeCategory, searchQuery]);

  const handleSaveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingItem || !editingItem.fieldNameEn.trim()) return;
    fieldDictionaryService.saveDefinition(editingItem);
    setEditingItem(null);
    setIsAddingNew(false);
    triggerToast('已保存字段中英对照定义');
  };

  const handleResetDefaults = () => {
    if (window.confirm('确定要恢复默认中英双语字段定义吗？')) {
      fieldDictionaryService.resetToDefault();
      triggerToast('已重置为默认中英文定义字典');
    }
  };

  const handleAddTypicalValue = () => {
    if (!editingItem) return;
    const newValues: FieldValueDefinition[] = [
      ...(editingItem.typicalValues || []),
      { valueEn: '', valueCn: '', meaningEn: '', meaningCn: '' }
    ];
    setEditingItem({ ...editingItem, typicalValues: newValues });
  };

  const handleRemoveTypicalValue = (idx: number) => {
    if (!editingItem) return;
    const newValues = (editingItem.typicalValues || []).filter((_, i) => i !== idx);
    setEditingItem({ ...editingItem, typicalValues: newValues });
  };

  const handleSyncAllRealDataValues = () => {
    let totalAdded = 0;
    const currentDefs = fieldDictionaryService.getDefinitions();
    const updatedDefs = currentDefs.map(def => {
      const target = def.category === 'clothes' ? 'clothes' : 'reports';
      const historical = dataService.getFieldPreviousValues(def.fieldNameEn, target);
      if (historical.length === 0) return def;

      const tvMap = new Map((def.typicalValues || []).map(tv => [tv.valueEn.trim().toLowerCase(), tv]));
      let defAdded = 0;
      historical.forEach(h => {
        const k = h.value.trim().toLowerCase();
        if (!tvMap.has(k)) {
          tvMap.set(k, {
            valueEn: h.value,
            valueCn: h.value,
            meaningEn: `Real historical record (${h.count}x)`,
            meaningCn: `真实穿搭历史记录 (出现 ${h.count} 次)`
          });
          defAdded++;
        }
      });

      if (defAdded > 0) {
        totalAdded += defAdded;
        const newDef = { ...def, typicalValues: Array.from(tvMap.values()) };
        fieldDictionaryService.saveDefinition(newDef);
        return newDef;
      }
      return def;
    });

    setDefinitions(updatedDefs);
    triggerToast(totalAdded > 0 ? `已成功将 ${totalAdded} 条历史真实输入值同步至 AI 字段定义中！` : '所有历史真实输入值均已存在于 AI 字典中。');
  };

  const currentFieldHistorical = useMemo(() => {
    if (!editingItem || !editingItem.fieldNameEn) return [];
    const target = editingItem.category === 'clothes' ? 'clothes' : 'reports';
    return dataService.getFieldPreviousValues(editingItem.fieldNameEn, target);
  }, [editingItem?.fieldNameEn, editingItem?.category]);

  const currentFieldHistoricalExplains = useMemo(() => {
    if (!editingItem || !editingItem.fieldNameEn) return [];
    return dataService.getFieldHistoricalExplanations(editingItem.fieldNameEn);
  }, [editingItem?.fieldNameEn]);

  return (
    <div className="w-full bg-white rounded-3xl border border-slate-200/90 shadow-xs overflow-hidden transition-all">
      {/* Drawer Toggle Header Bar - Sleek & Unobtrusive */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-5 py-3.5 flex items-center justify-between gap-3 text-left hover:bg-slate-50/80 transition-colors cursor-pointer select-none"
      >
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="w-8 h-8 rounded-xl bg-purple-100/90 text-purple-700 flex items-center justify-center shrink-0">
            <BookOpen className="w-4 h-4" />
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-bold text-slate-800 text-xs sm:text-sm">
                📖 字段与数据字典 &amp; AI 输入勾选 (Field Dictionary &amp; AI Check)
              </span>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                已勾选送入 AI: {definitions.filter(d => d.enabledForAi !== false).length} / {definitions.length} 项
              </span>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-purple-50 text-purple-700 border border-purple-200">
                {definitions.length} 个字段定义
              </span>
              {isAiPaused && (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-amber-50 text-amber-800 border border-amber-300">
                  ⏸️ 纯客观数据字典模式 · AI 实时生成已暂停 (NotebookLM 专供)
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-500 truncate mt-0.5">
              {isAiPaused
                ? '纯客观数据字典管理模式：保留所有中英文释义与典型值，已暂停自动生成调用，可完整导出至 NotebookLM'
                : '直接勾选/取消勾选送入 AI 分析的字段，并指导 AI 精准理解中英文输入含义与历史真实值'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-slate-400 shrink-0">
          <span className="text-[11px] font-medium hidden sm:inline text-slate-500">
            {isOpen ? '收起字典' : '展开查看与勾选'}
          </span>
          {isOpen ? <ChevronUp className="w-4 h-4 text-purple-600" /> : <ChevronDown className="w-4 h-4" />}
        </div>
      </button>

      {/* Expanded Content Drawer */}
      {isOpen && (
        <div className="p-4 sm:p-6 border-t border-slate-100 bg-slate-50/40 space-y-4">
          {/* Top Actions: Category Filter & Add New / Reset */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-1.5 p-1 bg-white rounded-xl border border-slate-200 shadow-2xs text-xs font-semibold overflow-x-auto">
              <button
                type="button"
                onClick={() => setActiveCategory('all')}
                className={`px-3 py-1.5 rounded-lg transition-all cursor-pointer ${
                  activeCategory === 'all'
                    ? 'bg-purple-600 text-white shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                全部 ({definitions.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveCategory('clothes')}
                className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                  activeCategory === 'clothes'
                    ? 'bg-purple-600 text-white shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Shirt className="w-3.5 h-3.5" />
                <span>Clothes 衣物 ({definitions.filter(d => d.category === 'clothes').length})</span>
              </button>
              <button
                type="button"
                onClick={() => setActiveCategory('wearingReport')}
                className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                  activeCategory === 'wearingReport'
                    ? 'bg-purple-600 text-white shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <FileText className="w-3.5 h-3.5" />
                <span>Wearing Report 穿戴 ({definitions.filter(d => d.category === 'wearingReport').length})</span>
              </button>
              <button
                type="button"
                onClick={() => setActiveCategory('junction')}
                className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
                  activeCategory === 'junction'
                    ? 'bg-purple-600 text-white shadow-2xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Link2 className="w-3.5 h-3.5" />
                <span>Junction 洗护关联 ({definitions.filter(d => d.category === 'junction').length})</span>
              </button>
            </div>

            <div className="flex items-center gap-1.5 self-end sm:self-auto flex-wrap">
              {/* Batch Check / Uncheck for AI */}
              <button
                type="button"
                onClick={() => {
                  fieldDictionaryService.setCategoryAllEnabled(activeCategory, true);
                  triggerToast(`已全选当前分类字段送入 AI 分析`);
                }}
                className="px-2.5 py-1.5 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 text-xs font-bold transition-all shadow-2xs cursor-pointer"
                title="勾选当前组所有字段送入 AI 分析"
              >
                ✓ 全选送入 AI
              </button>
              <button
                type="button"
                onClick={() => {
                  fieldDictionaryService.setCategoryAllEnabled(activeCategory, false);
                  triggerToast(`已取消勾选当前分类字段`);
                }}
                className="px-2.5 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 text-xs font-semibold transition-all shadow-2xs cursor-pointer"
                title="取消勾选当前组字段"
              >
                ✕ 全部取消
              </button>
              <button
                type="button"
                onClick={handleSyncAllRealDataValues}
                title="自动扫描并同步所有历史穿戴日记与衣物数据输入值至 AI 字典"
                className="px-3 py-1.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-700 border border-purple-200 text-xs font-bold transition-all shadow-2xs flex items-center gap-1.5 cursor-pointer"
              >
                <Sparkles className="w-3.5 h-3.5 text-purple-600" />
                <span>同步历史值至 AI</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setEditingItem({
                    fieldNameEn: '',
                    fieldNameCn: '',
                    category: activeCategory === 'all' ? 'clothes' : activeCategory,
                    descriptionEn: '',
                    descriptionCn: '',
                    typicalValues: [],
                    enabledForAi: true
                  });
                  setIsAddingNew(true);
                }}
                className="px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-all shadow-xs flex items-center gap-1.5 cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>添加新字段</span>
              </button>
              <button
                type="button"
                onClick={handleResetDefaults}
                title="恢复开箱即用默认字典"
                className="p-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-500 hover:text-slate-700 text-xs transition-colors cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Search bar & Live filter summary */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 bg-white p-2.5 rounded-2xl border border-slate-200 shadow-2xs">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                placeholder="搜索字段名称、中文含义或输入值 (例如: Hot at noon, 细肩带, 闷热)..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-8 py-1.5 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-1 focus:ring-purple-500 placeholder:text-slate-400"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5 rounded cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            <div className="flex items-center gap-2 text-xs text-slate-500 shrink-0 px-1">
              <span>共找到 <strong className="text-purple-700">{filteredDefs.length}</strong> 个字段</span>
              <span>·</span>
              <span className="text-emerald-700 font-semibold">
                其中 <strong className="text-emerald-800">{filteredDefs.filter(d => d.enabledForAi !== false).length}</strong> 项已送入 AI
              </span>
            </div>
          </div>

          {/* Toast */}
          {successToast && (
            <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
              <Check className="w-4 h-4 text-emerald-600 shrink-0" />
              <span className="font-medium">{successToast}</span>
            </div>
          )}

          {/* Full-Screen Edit / Add Modal */}
          {editingItem && (
            <div className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto">
              <div className="px-4 sm:px-8 py-3.5 border-b border-slate-200/80 bg-white/95 backdrop-blur-md flex items-center justify-between shrink-0 shadow-2xs w-full max-w-full overflow-x-hidden">
                <div className="flex items-center gap-2.5 min-w-0">
                  <button
                    type="button"
                    onClick={() => setEditingItem(null)}
                    className="p-1.5 sm:p-2 -ml-1 sm:-ml-2 rounded-xl text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
                    title="Back"
                  >
                    <ArrowLeft className="w-5 h-5" />
                  </button>
                  <span className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2 truncate">
                    <Edit2 className="w-4 h-4 text-purple-600 shrink-0" />
                    <span className="truncate">{isAddingNew ? '添加新字段中英文定义' : `编辑字段定义: ${editingItem.fieldNameEn}`}</span>
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setEditingItem(null)}
                  className="p-1.5 sm:p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer shrink-0"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto overflow-x-hidden p-3.5 sm:p-6 bg-slate-50/50 w-full max-w-full">
                <div className="max-w-5xl xl:max-w-6xl mx-auto bg-white p-4 sm:p-7 rounded-3xl border border-slate-200/90 shadow-xs w-full">
                  <form onSubmit={handleSaveEdit} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="text-[11px] font-mono font-bold uppercase text-slate-600 block mb-1">
                      英文原字段名 (FieldName EN) *
                    </label>
                    <input
                      type="text"
                      required
                      value={editingItem.fieldNameEn}
                      onChange={e => setEditingItem({ ...editingItem, fieldNameEn: e.target.value })}
                      placeholder="e.g. Hot at noon, diao dai"
                      className="w-full px-3 py-2 text-xs font-mono rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-mono font-bold uppercase text-slate-600 block mb-1">
                      中文对照名称 (FieldName CN) *
                    </label>
                    <input
                      type="text"
                      required
                      value={editingItem.fieldNameCn}
                      onChange={e => setEditingItem({ ...editingItem, fieldNameCn: e.target.value })}
                      placeholder="e.g. 中午炎热度 / 细肩带"
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-mono font-bold uppercase text-slate-600 block mb-1">
                      所属模块类别 (Category)
                    </label>
                    <select
                      value={editingItem.category}
                      onChange={e => setEditingItem({ ...editingItem, category: e.target.value as any })}
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                    >
                      <option value="clothes">Clothes (衣服字段)</option>
                      <option value="wearingReport">Wearing Report (穿着日记字段)</option>
                      <option value="junction">Junction (穿戴关联/洗护字段)</option>
                    </select>
                  </div>
                </div>

                {/* AI Inclusion Toggle in Edit Form */}
                <div className="flex items-center justify-between p-3 rounded-2xl bg-purple-50/60 border border-purple-100 text-xs">
                  <div>
                    <span className="font-bold text-purple-950 block">送入 AI 分析 (Include in AI Analysis)</span>
                    <span className="text-[11px] text-purple-700">勾选时 AI 将接收此字段并进行穿搭与体感分析</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setEditingItem({ ...editingItem, enabledForAi: editingItem.enabledForAi === false ? true : false })}
                    className={`px-3 py-1.5 rounded-xl font-bold text-xs transition-all cursor-pointer ${
                      editingItem.enabledForAi !== false ? 'bg-emerald-600 text-white shadow-2xs' : 'bg-slate-200 text-slate-700'
                    }`}
                  >
                    {editingItem.enabledForAi !== false ? '✓ 已启用送入 AI' : '✕ 已排除'}
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                        中文核心定义与人体工学含义 (Description CN)
                      </label>
                      <span className="text-[9px] text-slate-400 font-mono hidden sm:inline">可直接拖拽右下角放大</span>
                    </div>
                    <textarea
                      rows={Math.max(3, Math.min(8, (editingItem.descriptionCn || '').split('\n').length + 1))}
                      value={editingItem.descriptionCn}
                      onChange={e => setEditingItem({ ...editingItem, descriptionCn: e.target.value })}
                      placeholder="告诉 AI 这个字段到底代表身体的什么感觉（例如：考察久坐办公时底围是否有压迫感）"
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500 break-words whitespace-pre-wrap resize-y min-h-[72px] leading-relaxed"
                    />
                  </div>
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <label className="text-[11px] font-mono font-bold uppercase text-slate-600">
                        英文辅助描述 (Description EN)
                      </label>
                      <span className="text-[9px] text-slate-400 font-mono hidden sm:inline">可直接拖拽右下角放大</span>
                    </div>
                    <textarea
                      rows={Math.max(3, Math.min(8, (editingItem.descriptionEn || '').split('\n').length + 1))}
                      value={editingItem.descriptionEn}
                      onChange={e => setEditingItem({ ...editingItem, descriptionEn: e.target.value })}
                      placeholder="English description for AI reasoning prompt"
                      className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-purple-500 font-mono break-words whitespace-pre-wrap resize-y min-h-[72px] leading-relaxed"
                    />
                  </div>
                </div>

                {/* Real Historical Data Values from Database - Full Text Wrap (No Truncation) */}
                {currentFieldHistorical.length > 0 && (
                  <div className="p-3.5 bg-purple-50/70 rounded-2xl border border-purple-200/90 space-y-2.5">
                    <div className="flex items-center justify-between flex-wrap gap-2">
                      <div className="flex items-center gap-1.5">
                        <History className="w-4 h-4 text-purple-700 shrink-0" />
                        <div>
                          <span className="text-xs font-bold text-purple-950 block">
                            从历史穿戴数据中提取的真实输入值 ({currentFieldHistorical.length} 条真实记录)
                          </span>
                          <span className="text-[10px] text-purple-700">
                            已开启完整文本换行，点击任意一项直接填入下方输入值列表
                          </span>
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          const tvMap = new Map((editingItem.typicalValues || []).map(tv => [tv.valueEn.trim().toLowerCase(), tv]));
                          currentFieldHistorical.forEach(h => {
                            const k = h.value.trim().toLowerCase();
                            if (!tvMap.has(k)) {
                              tvMap.set(k, {
                                valueEn: h.value,
                                valueCn: h.value,
                                meaningEn: `Historical journal observation (${h.count}x)`,
                                meaningCn: `真实历史记录值 (出现 ${h.count} 次)`
                              });
                            }
                          });
                          setEditingItem({ ...editingItem, typicalValues: Array.from(tvMap.values()) });
                          triggerToast(`已同步 ${currentFieldHistorical.length} 条历史真实输入值`);
                        }}
                        className="px-2.5 py-1.5 rounded-xl text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white shadow-2xs transition-colors cursor-pointer flex items-center gap-1"
                      >
                        <Sparkles className="w-3.5 h-3.5" />
                        <span>一键填入所有历史值至输入值列表</span>
                      </button>
                    </div>

                    <div className="flex flex-wrap gap-2 max-h-48 overflow-y-auto pr-1">
                      {currentFieldHistorical.map((h, hIdx) => {
                        const isAdded = (editingItem.typicalValues || []).some(
                          v => v.valueEn.trim().toLowerCase() === h.value.trim().toLowerCase()
                        );
                        return (
                          <button
                            key={hIdx}
                            type="button"
                            onClick={() => {
                              if (isAdded) return;
                              const newTv: FieldValueDefinition = {
                                valueEn: h.value,
                                valueCn: h.value,
                                meaningEn: `Historical journal observation (${h.count}x)`,
                                meaningCn: `真实历史记录值 (出现 ${h.count} 次)`
                              };
                              setEditingItem({
                                ...editingItem,
                                typicalValues: [...(editingItem.typicalValues || []), newTv]
                              });
                            }}
                            className={`p-2 rounded-xl text-xs font-mono transition-all text-left flex items-start gap-1.5 cursor-pointer max-w-full break-words whitespace-normal leading-relaxed ${
                              isAdded
                                ? 'bg-purple-100 text-purple-900 border border-purple-300 font-semibold cursor-default'
                                : 'bg-white hover:bg-purple-50 text-slate-800 border border-slate-200 hover:border-purple-300 shadow-2xs'
                            }`}
                            title={isAdded ? '已添加在下方输入值列表中' : `点击添加 "${h.value}" 至 AI 输入值`}
                          >
                            {isAdded && <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0 mt-0.5" />}
                            <div className="flex-1 min-w-0">
                              <span className="break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] block">{h.value}</span>
                              {h.sources && h.sources.length > 0 && (
                                <div className="mt-1 flex items-center gap-1 flex-wrap">
                                  {h.sources.map((src, sIdx) => (
                                    <span key={sIdx} className="text-[9px] font-sans font-semibold px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-900 border border-purple-200">
                                      👕 {src}
                                    </span>
                                  ))}
                                </div>
                              )}
                            </div>
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 font-bold shrink-0 self-start">
                              {h.count}x
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Real Historical Explain Notes from Garments - One-Click Embedding to Meaning CN */}
                {currentFieldHistoricalExplains.length > 0 && (
                  <div className="p-4 bg-linear-to-r from-purple-50/90 to-indigo-50/70 rounded-2xl border border-purple-200/90 space-y-3 shadow-2xs">
                    <div className="flex items-center justify-between flex-wrap gap-2">
                      <div className="flex items-center gap-2">
                        <div className="p-1.5 rounded-xl bg-purple-600 text-white shadow-2xs">
                          <MessageSquare className="w-4 h-4" />
                        </div>
                        <div>
                          <span className="text-xs font-bold text-purple-950 block">
                            💡 衣物历史体感说明笔记库 ({currentFieldHistoricalExplains.length} 条真实心得)
                          </span>
                          <span className="text-[10px] text-purple-700">
                            从您衣物真实记录中提取的说明笔记（例如 bra diao dai normal / tight）。可一键建为新输入项，或在下方各输入值「Meaning CN」中直接引用！
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                      {currentFieldHistoricalExplains.map((exp, expIdx) => (
                        <div
                          key={expIdx}
                          className="p-2.5 rounded-xl bg-white border border-purple-200/70 text-xs text-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-2 shadow-2xs hover:border-purple-400 transition-all"
                        >
                          <div className="flex-1 min-w-0">
                            {exp.sources && exp.sources.length > 0 ? (
                              <div className="flex items-center gap-1 flex-wrap mb-1">
                                {exp.sources.map((src, sIdx) => (
                                  <span key={sIdx} className="text-[10px] font-sans font-bold px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-900 border border-purple-200">
                                    👕 {src}
                                  </span>
                                ))}
                              </div>
                            ) : exp.garmentName ? (
                              <span className="text-[10px] font-sans font-bold px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-900 border border-purple-200 mr-1.5 inline-block mb-1">
                                👕 {exp.garmentName}
                              </span>
                            ) : null}
                            <span className="font-semibold text-slate-900 break-words whitespace-pre-wrap [word-break:normal] [overflow-wrap:break-word] leading-relaxed block">
                              &ldquo;{exp.text}&rdquo;
                            </span>
                            {exp.count > 1 && (
                              <span className="text-[10px] text-slate-400 mt-0.5 font-mono block">
                                ({exp.count}件引用)
                              </span>
                            )}
                          </div>

                          <div className="flex items-center gap-1.5 shrink-0 self-end sm:self-center">
                            <button
                              type="button"
                              onClick={() => {
                                const newTv: FieldValueDefinition = {
                                  valueEn: exp.garmentName || exp.text.slice(0, 30),
                                  valueCn: exp.text.slice(0, 30),
                                  meaningEn: 'Ergonomic garment experience notes',
                                  meaningCn: exp.text
                                };
                                setEditingItem({
                                  ...editingItem,
                                  typicalValues: [...(editingItem.typicalValues || []), newTv]
                                });
                                triggerToast('已以此体感说明创建新的输入项');
                              }}
                              className="px-2.5 py-1 rounded-lg text-[11px] font-bold bg-purple-600 hover:bg-purple-700 text-white shadow-2xs transition-colors cursor-pointer flex items-center gap-1"
                              title="直接将此条说明新建为一个完整的输入值"
                            >
                              <Plus className="w-3 h-3" />
                              <span>新建为输入项</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                const prev = editingItem.descriptionCn || '';
                                const updated = prev ? `${prev}\n${exp.text}` : exp.text;
                                setEditingItem({ ...editingItem, descriptionCn: updated });
                                triggerToast('已追加至该字段中文核心定义 (Description CN)');
                              }}
                              className="px-2 py-1 rounded-lg text-[11px] font-bold bg-purple-100 hover:bg-purple-200 text-purple-800 transition-colors cursor-pointer"
                              title="追加至上方字段整体描述"
                            >
                              <span>追加至字段定义</span>
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Typical Values List - With Text Wrapping & Layout Switcher */}
                <div className="space-y-3 pt-3 border-t border-slate-200/80">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">
                          常见英文输入值与真实体感对应 (Typical Values &amp; Meanings)
                        </span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-purple-100 text-purple-800">
                          {editingItem.typicalValues?.length || 0} 项
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        全字段已开启多行自动换行 (Text Wrap) 与横向滚动支持，长文本在笔记本屏幕上清晰完整展示
                      </p>
                    </div>

                    <div className="flex items-center gap-2 flex-wrap">
                      {/* View Mode Toggle: Card Wrap vs Horizontal Table */}
                      <div className="flex items-center p-0.5 bg-slate-100 rounded-xl border border-slate-200 text-xs">
                        <button
                          type="button"
                          onClick={() => setTypicalValuesLayout('card')}
                          className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all flex items-center gap-1 cursor-pointer ${
                            typicalValuesLayout === 'card'
                              ? 'bg-white text-purple-900 shadow-2xs'
                              : 'text-slate-600 hover:text-slate-900'
                          }`}
                          title="换行卡片视图 - 适合大屏/笔记本清晰阅读完整多行文本"
                        >
                          <LayoutGrid className="w-3.5 h-3.5" />
                          <span>换行卡片</span>
                        </button>
                        <button
                          type="button"
                          onClick={() => setTypicalValuesLayout('table')}
                          className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all flex items-center gap-1 cursor-pointer ${
                            typicalValuesLayout === 'table'
                              ? 'bg-white text-purple-900 shadow-2xs'
                              : 'text-slate-600 hover:text-slate-900'
                          }`}
                          title="横向展开表格 - 类似移动端横向滑动查看所有字段"
                        >
                          <Table className="w-3.5 h-3.5" />
                          <span>横向表格</span>
                        </button>
                      </div>

                      <button
                        type="button"
                        onClick={handleAddTypicalValue}
                        className="px-3 py-1.5 rounded-xl text-xs font-bold bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200 cursor-pointer flex items-center gap-1 shadow-2xs transition-all"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        <span>添加输入值</span>
                      </button>
                    </div>
                  </div>

                  {editingItem.typicalValues && editingItem.typicalValues.length > 0 ? (
                    typicalValuesLayout === 'card' ? (
                      /* LAYOUT 1: WRAPPED CARD VIEW (Auto-wrap, Multi-line, Highly Readable on Laptop & Mobile) */
                      <div className="space-y-3 max-h-[500px] overflow-y-auto pr-1">
                        {editingItem.typicalValues.map((v, vIdx) => (
                          <div
                            key={vIdx}
                            className="p-3.5 sm:p-4 rounded-2xl bg-slate-50/80 border border-slate-200 space-y-3 shadow-2xs transition-all hover:border-purple-300"
                          >
                            <div className="flex items-center justify-between pb-2 border-b border-slate-200/70 text-xs">
                              <div className="flex items-center gap-2">
                                <span className="w-6 h-6 rounded-xl bg-purple-100 text-purple-800 text-xs font-bold flex items-center justify-center font-mono">
                                  {vIdx + 1}
                                </span>
                                <span className="font-bold text-slate-800 font-mono break-words">
                                  {v.valueEn || `输入项 #${vIdx + 1}`}
                                </span>
                                {v.valueCn && (
                                  <span className="text-[11px] text-slate-500 font-sans break-words">
                                    — {v.valueCn}
                                  </span>
                                )}
                              </div>
                              <button
                                type="button"
                                onClick={() => handleRemoveTypicalValue(vIdx)}
                                className="px-2 py-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg cursor-pointer transition-colors flex items-center gap-1 text-xs"
                                title="删除此输入值"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                                <span>删除</span>
                              </button>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                              <div>
                                <label className="text-[11px] font-mono font-bold text-slate-700 block mb-1">
                                  🔤 英文输入值 (Value EN) <span className="text-slate-400 font-normal font-sans">- 自动换行/可拉伸</span>
                                </label>
                                <textarea
                                  rows={Math.max(2, Math.min(6, (v.valueEn || '').split('\n').length + 1))}
                                  placeholder="英文输入值 (例如: stand breast ugly, because loose ties around...)"
                                  value={v.valueEn}
                                  onChange={e => {
                                    const copy = [...editingItem.typicalValues];
                                    copy[vIdx].valueEn = e.target.value;
                                    setEditingItem({ ...editingItem, typicalValues: copy });
                                  }}
                                  className="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-mono break-words whitespace-pre-wrap resize-y min-h-[56px] focus:outline-none focus:ring-2 focus:ring-purple-500"
                                />
                              </div>

                              <div>
                                <label className="text-[11px] font-bold text-slate-700 block mb-1">
                                  🇨🇳 中文值 (Value CN) <span className="text-slate-400 font-normal">- 自动换行/可拉伸</span>
                                </label>
                                <textarea
                                  rows={Math.max(2, Math.min(6, (v.valueCn || '').split('\n').length + 1))}
                                  placeholder="中文对应值 (例如: 站立/坐着胸型褶皱多影响外观)"
                                  value={v.valueCn}
                                  onChange={e => {
                                    const copy = [...editingItem.typicalValues];
                                    copy[vIdx].valueCn = e.target.value;
                                    setEditingItem({ ...editingItem, typicalValues: copy });
                                  }}
                                  className="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs break-words whitespace-pre-wrap resize-y min-h-[56px] focus:outline-none focus:ring-2 focus:ring-purple-500"
                                />
                              </div>

                              <div className="md:col-span-2">
                                <div className="flex items-center justify-between mb-1">
                                  <label className="text-[11px] font-bold text-slate-700 flex items-center gap-1">
                                    <span>💡 中文体感与人体工学详细含义 (Meaning CN)</span>
                                    <span className="text-slate-400 font-normal font-sans">- 自动换行</span>
                                  </label>
                                  <div className="flex items-center gap-2">
                                    {currentFieldHistoricalExplains.length > 0 && (
                                      <span className="text-[10px] text-purple-700 font-semibold hidden sm:inline">
                                        可一键引用或合并历史说明
                                      </span>
                                    )}
                                    <button
                                      type="button"
                                      onClick={() => setExpandedMeaningCnMap(prev => ({ ...prev, [vIdx]: !prev[vIdx] }))}
                                      className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold text-purple-700 hover:bg-purple-100 bg-purple-50 border border-purple-200 transition-colors cursor-pointer"
                                      title={expandedMeaningCnMap[vIdx] ? "还原默认高度" : "放大文本框高度查看全部内容"}
                                    >
                                      {expandedMeaningCnMap[vIdx] ? (
                                        <>
                                          <Minimize2 className="w-3 h-3 text-purple-600" />
                                          <span>还原高度</span>
                                        </>
                                      ) : (
                                        <>
                                          <Maximize2 className="w-3 h-3 text-purple-600" />
                                          <span>拉大文本框</span>
                                        </>
                                      )}
                                    </button>
                                  </div>
                                </div>
                                <textarea
                                  rows={expandedMeaningCnMap[vIdx] ? 8 : Math.max(3, Math.min(6, (v.meaningCn || '').split('\n').length + 1))}
                                  placeholder="中文体感含义 (例如: Bra diao dai normal - is another type of diao dai. Normally not pain at shoulder, but new or tight adjust bra diao dai will pain at shoulder...)"
                                  value={v.meaningCn}
                                  onChange={e => {
                                    const copy = [...editingItem.typicalValues];
                                    copy[vIdx].meaningCn = e.target.value;
                                    setEditingItem({ ...editingItem, typicalValues: copy });
                                  }}
                                  className="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs break-words whitespace-pre-wrap resize-y min-h-[76px] focus:outline-none focus:ring-2 focus:ring-purple-500 font-sans leading-relaxed"
                                />
                                <div className="flex items-center justify-between text-[10px] text-slate-400 mt-0.5 px-1">
                                  <span>支持右下角鼠标/触控拖动拉伸高度 (Drag bottom-right to enlarge)</span>
                                  {v.meaningCn ? <span>{v.meaningCn.length} 字</span> : null}
                                </div>

                                {/* One-click Embed Options for this specific Meaning CN */}
                                {currentFieldHistoricalExplains.length > 0 && (
                                  <div className="mt-1.5 p-2 rounded-xl bg-purple-50/70 border border-purple-200/80 space-y-1.5">
                                    <div className="flex items-center justify-between">
                                      <span className="text-[10px] font-bold text-purple-900 flex items-center gap-1">
                                        <MessageSquare className="w-3 h-3 text-purple-600" />
                                        <span>📌 快速嵌入历史体感说明 ({currentFieldHistoricalExplains.length} 条备选):</span>
                                      </span>
                                      <span className="text-[9px] text-purple-600">点击「替换填入」或「+ 追加合并」</span>
                                    </div>
                                    <div className="flex flex-col gap-1 max-h-36 overflow-y-auto pr-0.5">
                                      {currentFieldHistoricalExplains.map((exp, expIdx) => (
                                        <div
                                          key={expIdx}
                                          className="p-1.5 rounded-lg bg-white border border-purple-100 hover:border-purple-300 text-[11px] text-slate-800 flex items-start justify-between gap-2 shadow-2xs"
                                        >
                                          <div className="flex-1 min-w-0">
                                            {exp.garmentName && (
                                              <span className="text-[9px] font-mono font-semibold px-1 py-0.2 rounded bg-purple-100 text-purple-800 mr-1 inline-block">
                                                {exp.garmentName}
                                              </span>
                                            )}
                                            <span className="break-words font-medium">&ldquo;{exp.text}&rdquo;</span>
                                          </div>
                                          <div className="flex items-center gap-1 shrink-0 self-start">
                                            <button
                                              type="button"
                                              onClick={() => {
                                                const copy = [...editingItem.typicalValues];
                                                copy[vIdx].meaningCn = exp.text;
                                                setEditingItem({ ...editingItem, typicalValues: copy });
                                                triggerToast('已替换填入此体感说明');
                                              }}
                                              className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-100 hover:bg-purple-200 text-purple-800 transition-colors cursor-pointer"
                                              title="直接替换填入当前 Meaning CN"
                                            >
                                              替换填入
                                            </button>
                                            <button
                                              type="button"
                                              onClick={() => {
                                                const copy = [...editingItem.typicalValues];
                                                const prev = copy[vIdx].meaningCn || '';
                                                copy[vIdx].meaningCn = prev ? `${prev}\n${exp.text}` : exp.text;
                                                setEditingItem({ ...editingItem, typicalValues: copy });
                                                triggerToast('已换行追加合并此体感说明');
                                              }}
                                              className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-600 hover:bg-emerald-700 text-white transition-colors cursor-pointer"
                                              title="在现有内容后换行追加合并（例如合并 normal 与 tight 两款说明）"
                                            >
                                              + 追加合并
                                            </button>
                                          </div>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}
                              </div>

                              <div>
                                <label className="text-[11px] font-mono font-bold text-slate-700 block mb-1">
                                  🌐 英文辅助含义 (Meaning EN) <span className="text-slate-400 font-normal font-sans">- English prompt reason</span>
                                </label>
                                <textarea
                                  rows={2}
                                  placeholder="English meaning for AI reasoning (e.g. Creasing/wrinkles around bust...)"
                                  value={v.meaningEn}
                                  onChange={e => {
                                    const copy = [...editingItem.typicalValues];
                                    copy[vIdx].meaningEn = e.target.value;
                                    setEditingItem({ ...editingItem, typicalValues: copy });
                                  }}
                                  className="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs font-mono break-words whitespace-pre-wrap resize-y focus:outline-none focus:ring-2 focus:ring-purple-500"
                                />
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      /* LAYOUT 2: HORIZONTAL SCROLL TABLE VIEW ("Horizontal like mobile") */
                      <div className="overflow-x-auto border border-slate-200 rounded-2xl bg-white shadow-2xs max-h-[500px]">
                        <table className="w-full text-left text-xs min-w-[960px] divide-y divide-slate-200">
                          <thead className="bg-slate-50 font-bold text-slate-700 sticky top-0 z-10">
                            <tr>
                              <th className="py-2.5 px-3 w-12 text-center bg-slate-50">#</th>
                              <th className="py-2.5 px-3 min-w-[220px] bg-slate-50">🔤 英文输入值 (Value EN)</th>
                              <th className="py-2.5 px-3 min-w-[200px] bg-slate-50">🇨🇳 中文值 (Value CN)</th>
                              <th className="py-2.5 px-3 min-w-[280px] bg-slate-50">💡 中文体感含义 (Meaning CN)</th>
                              <th className="py-2.5 px-3 min-w-[220px] bg-slate-50">🌐 英文含义 (Meaning EN)</th>
                              <th className="py-2.5 px-3 w-14 text-center bg-slate-50">操作</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100">
                            {editingItem.typicalValues.map((v, vIdx) => (
                              <tr key={vIdx} className="hover:bg-slate-50/70 transition-colors">
                                <td className="py-2.5 px-3 text-center font-mono text-slate-400 font-bold align-top pt-3">
                                  {vIdx + 1}
                                </td>
                                <td className="py-2 px-3 align-top min-w-[160px]">
                                  <textarea
                                    rows={Math.max(2, Math.min(6, (v.valueEn || '').split('\n').length + 1))}
                                    placeholder="Value EN"
                                    value={v.valueEn}
                                    onChange={e => {
                                      const copy = [...editingItem.typicalValues];
                                      copy[vIdx].valueEn = e.target.value;
                                      setEditingItem({ ...editingItem, typicalValues: copy });
                                    }}
                                    className="w-full px-2.5 py-1.5 bg-white border border-slate-200 rounded-xl text-xs font-mono break-words whitespace-pre-wrap resize-y min-h-[52px] focus:outline-none focus:ring-1 focus:ring-purple-500"
                                  />
                                </td>
                                <td className="py-2 px-3 align-top min-w-[160px]">
                                  <textarea
                                    rows={Math.max(2, Math.min(6, (v.valueCn || '').split('\n').length + 1))}
                                    placeholder="Value CN"
                                    value={v.valueCn}
                                    onChange={e => {
                                      const copy = [...editingItem.typicalValues];
                                      copy[vIdx].valueCn = e.target.value;
                                      setEditingItem({ ...editingItem, typicalValues: copy });
                                    }}
                                    className="w-full px-2.5 py-1.5 bg-white border border-slate-200 rounded-xl text-xs break-words whitespace-pre-wrap resize-y min-h-[52px] focus:outline-none focus:ring-1 focus:ring-purple-500"
                                  />
                                </td>
                                <td className="py-2 px-3 align-top min-w-[280px]">
                                  <div className="flex items-center justify-between mb-1">
                                    <span className="text-[10px] text-slate-500 font-mono">Meaning CN</span>
                                    <button
                                      type="button"
                                      onClick={() => setExpandedMeaningCnMap(prev => ({ ...prev, [vIdx]: !prev[vIdx] }))}
                                      className="text-[9px] text-purple-700 hover:underline cursor-pointer"
                                    >
                                      {expandedMeaningCnMap[vIdx] ? '还原' : '拉大'}
                                    </button>
                                  </div>
                                  <textarea
                                    rows={expandedMeaningCnMap[vIdx] ? 8 : Math.max(3, Math.min(6, (v.meaningCn || '').split('\n').length + 1))}
                                    placeholder="Meaning CN"
                                    value={v.meaningCn}
                                    onChange={e => {
                                      const copy = [...editingItem.typicalValues];
                                      copy[vIdx].meaningCn = e.target.value;
                                      setEditingItem({ ...editingItem, typicalValues: copy });
                                    }}
                                    className="w-full px-2.5 py-1.5 bg-white border border-slate-200 rounded-xl text-xs break-words whitespace-pre-wrap resize-y min-h-[64px] focus:outline-none focus:ring-1 focus:ring-purple-500"
                                  />
                                  {currentFieldHistoricalExplains.length > 0 && (
                                    <div className="mt-1 flex items-center gap-1 flex-wrap">
                                      <span className="text-[9px] text-purple-700 font-bold shrink-0">引用历史体感:</span>
                                      {currentFieldHistoricalExplains.slice(0, 4).map((exp, expIdx) => (
                                        <button
                                          key={expIdx}
                                          type="button"
                                          onClick={() => {
                                            const copy = [...editingItem.typicalValues];
                                            const prev = copy[vIdx].meaningCn || '';
                                            copy[vIdx].meaningCn = prev ? `${prev}\n${exp.text}` : exp.text;
                                            setEditingItem({ ...editingItem, typicalValues: copy });
                                            triggerToast('已追加此体感说明至 Meaning CN');
                                          }}
                                          className="px-1.5 py-0.5 rounded bg-purple-50 hover:bg-purple-100 text-purple-800 border border-purple-200 text-[9px] font-medium max-w-[150px] truncate cursor-pointer transition-colors"
                                          title={`点击换行追加: ${exp.text}`}
                                        >
                                          + {exp.text.slice(0, 16)}...
                                        </button>
                                      ))}
                                    </div>
                                  )}
                                </td>
                                <td className="py-2 px-3 align-top">
                                  <textarea
                                    rows={2}
                                    placeholder="Meaning EN"
                                    value={v.meaningEn}
                                    onChange={e => {
                                      const copy = [...editingItem.typicalValues];
                                      copy[vIdx].meaningEn = e.target.value;
                                      setEditingItem({ ...editingItem, typicalValues: copy });
                                    }}
                                    className="w-full px-2.5 py-1.5 bg-white border border-slate-200 rounded-xl text-xs font-mono break-words whitespace-pre-wrap resize-y focus:outline-none focus:ring-1 focus:ring-purple-500"
                                  />
                                </td>
                                <td className="py-2 px-3 text-center align-top pt-3">
                                  <button
                                    type="button"
                                    onClick={() => handleRemoveTypicalValue(vIdx)}
                                    className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg cursor-pointer transition-colors"
                                    title="删除"
                                  >
                                    <Trash2 className="w-4 h-4" />
                                  </button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )
                  ) : (
                    <div className="p-4 rounded-2xl bg-slate-50 border border-dashed border-slate-300 text-center text-xs text-slate-500">
                      暂无配置具体取值。AI 将按上述核心定义理解此字段。点击上方「+ 添加输入值」或「一键填入历史值」进行配置。
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setEditingItem(null)}
                    className="px-4 py-2 rounded-xl border border-slate-300 text-slate-700 text-xs sm:text-sm font-semibold hover:bg-slate-100 cursor-pointer transition-colors"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    className="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs sm:text-sm font-bold shadow-xs cursor-pointer transition-all active:scale-95"
                  >
                    保存定义
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

          {/* Definitions Grid View */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {filteredDefs.map(d => {
              const historicalCount = dataService.getFieldPreviousValues(d.fieldNameEn, d.category === 'clothes' ? 'clothes' : 'reports').length;
              const isEnabled = d.enabledForAi !== false;
              return (
              <div
                key={d.fieldNameEn}
                className={`p-3.5 rounded-2xl bg-white border shadow-2xs space-y-2 flex flex-col justify-between transition-all ${
                  isEnabled
                    ? 'border-slate-200/90 hover:border-purple-300'
                    : 'border-slate-200/50 bg-slate-50/50 opacity-70'
                }`}
              >
                <div className="space-y-1.5">
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0 flex-1">
                      <span className="font-mono font-bold text-xs text-purple-950 block break-words whitespace-normal">
                        {d.fieldNameEn}
                      </span>
                      <span className="text-[11px] font-semibold text-slate-700 block break-words whitespace-normal">
                        {d.fieldNameCn}
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      {/* Direct Check/Uncheck toggle button for AI */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          fieldDictionaryService.setFieldEnabled(d.fieldNameEn, !isEnabled);
                        }}
                        className={`px-2 py-0.5 rounded-lg text-[10px] font-bold transition-all flex items-center gap-1 cursor-pointer active:scale-95 ${
                          isEnabled
                            ? 'bg-emerald-600 text-white shadow-2xs hover:bg-emerald-700'
                            : 'bg-slate-100 text-slate-500 hover:bg-slate-200 border border-slate-300'
                        }`}
                        title={isEnabled ? '已勾选：送入 AI 分析 (点击取消勾选)' : '已排除：不送入 AI 分析 (点击勾选送入)'}
                      >
                        {isEnabled ? (
                          <>
                            <Check className="w-3 h-3 text-white" />
                            <span>送入 AI</span>
                          </>
                        ) : (
                          <>
                            <X className="w-3 h-3 text-slate-400" />
                            <span>排除</span>
                          </>
                        )}
                      </button>
                      <span className="px-1.5 py-0.5 rounded-md text-[9px] font-mono uppercase bg-slate-100 text-slate-600 border border-slate-200">
                        {d.category}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5 flex-wrap text-[10px]">
                    <span className="px-1.5 py-0.5 rounded bg-purple-50 text-purple-700 font-medium border border-purple-200">
                      {d.typicalValues?.length || 0} 个配置输入值
                    </span>
                    {historicalCount > 0 && (
                      <span className="px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-700 font-medium border border-emerald-200 flex items-center gap-1">
                        <History className="w-2.5 h-2.5" />
                        <span>{historicalCount} 条真实记录</span>
                      </span>
                    )}
                  </div>

                  <p className="text-[11px] text-slate-600 leading-snug break-words">
                    {d.descriptionCn || d.descriptionEn}
                  </p>

                  {d.typicalValues && d.typicalValues.length > 0 && (
                    <div className="pt-1.5 border-t border-slate-100 flex flex-wrap gap-1.5">
                      {d.typicalValues.slice(0, 4).map((tv, idx) => (
                        <span
                          key={idx}
                          title={`${tv.valueEn} -> ${tv.meaningCn}`}
                          className="px-2 py-0.5 rounded bg-slate-100 text-[10px] font-mono text-slate-700 break-words whitespace-normal max-w-full leading-tight"
                        >
                          <strong>{tv.valueEn}:</strong> {tv.valueCn}
                        </span>
                      ))}
                      {d.typicalValues.length > 4 && (
                        <span className="text-[10px] text-slate-400 self-center">
                          +{d.typicalValues.length - 4}项
                        </span>
                      )}
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-end gap-1.5 pt-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => {
                      setEditingItem(JSON.parse(JSON.stringify(d)));
                      setIsAddingNew(false);
                    }}
                    className="p-1 text-slate-500 hover:text-purple-700 hover:bg-purple-50 rounded-lg text-[11px] font-medium flex items-center gap-1 cursor-pointer transition-colors"
                  >
                    <Edit2 className="w-3 h-3" />
                    <span>编辑</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`确定要移除 ${d.fieldNameEn} 字典项吗？`)) {
                        fieldDictionaryService.removeDefinition(d.fieldNameEn);
                        triggerToast(`已移除 ${d.fieldNameEn}`);
                      }
                    }}
                    className="p-1 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg cursor-pointer transition-colors"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                </div>
              </div>
            );
          })}
          </div>
        </div>
      )}
    </div>
  );
};
