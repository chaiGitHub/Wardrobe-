import React, { useState, useEffect, useMemo } from 'react';
import { ClothesItem, WearingReport, ReportClothesJunction } from '../types';
import { dataService } from '../services/dataService';
import { getCustomApiKey, getSelectedModel, useAiGenerationPaused, setAiGenerationPaused } from '../services/aiSettings';
import { toDateKey } from '../utils/calendarCalculations';
import { fieldDictionaryService, AIAdvisorReportRecord } from '../services/fieldDictionaryService';
import { FieldDictionaryDrawer } from './FieldDictionaryDrawer';
import { SavedReportsList } from './SavedReportsList';
import { AISecretKeyBanner } from './AISecretKeyBanner';
import { UnifiedAIReportArchive } from './UnifiedAIReportArchive';
import {
  Sparkles, ThermometerSun, RefreshCw, Send, DollarSign, PieChart,
  ShieldCheck, Heart, Shirt, CheckCircle2, AlertCircle, FileText,
  Clock, Droplets, ChevronRight, Activity, Tag, HelpCircle, Layers,
  Check, Save, Trash2, Edit2, BookOpen
} from 'lucide-react';

interface AIAdvisorProps {
  clothes: ClothesItem[];
  reports?: WearingReport[];
  junctions?: ReportClothesJunction[];
  onSelectClothes?: (clothes: ClothesItem) => void;
  onSelectGarment?: (garment: ClothesItem) => void;
  onSelectReport?: (report: WearingReport) => void;
}

export const AIAdvisor: React.FC<AIAdvisorProps> = ({
  clothes,
  reports = [],
  junctions = [],
  onSelectClothes,
  onSelectGarment,
  onSelectReport
}) => {
  const handleSelectClothesItem = (item: ClothesItem) => {
    if (onSelectClothes) onSelectClothes(item);
    else if (onSelectGarment) onSelectGarment(item);
  };

  // Mode switch: 'wardrobe-consultant' (Macro AI Advisor) | 'garment-diagnostic' (Clothes AI) | 'report-diagnostic' (Wearing Report AI) | 'all-reports-archive' (All Reports Unified Archive)
  const isAiPaused = useAiGenerationPaused();
  const [advisorMode, setAdvisorMode] = useState<'wardrobe-consultant' | 'garment-diagnostic' | 'report-diagnostic' | 'all-reports-archive'>('wardrobe-consultant');

  // Garment Teable Diagnostic state (Clothes AI - strictly this garment fields only)
  const [selectedGarmentForAnalysis, setSelectedGarmentForAnalysis] = useState<string>(clothes[0]?.id || '');
  const [isAnalyzingGarment, setIsAnalyzingGarment] = useState(false);
  const [garmentAnalysisResult, setGarmentAnalysisResult] = useState<string | null>(null);
  const [garmentAnalysisError, setGarmentAnalysisError] = useState('');

  // Wearing Report Teable Diagnostic state (Wearing Report AI - strictly report + its junctions only)
  const [selectedReportForAnalysis, setSelectedReportForAnalysis] = useState<string>(reports[0]?.id || '');
  const [isAnalyzingReport, setIsAnalyzingReport] = useState(false);
  const [reportAnalysisResult, setReportAnalysisResult] = useState<string | null>(null);
  const [reportAnalysisError, setReportAnalysisError] = useState('');

  // Wardrobe Consultant query state (Macro AI Advisor - whole wardrobe / all clothes or selected, respects None)
  const [temperature, setTemperature] = useState('Hot Sunny Noon (>30°C / 86°F)');
  const [occasion, setOccasion] = useState('none');
  const [mood, setMood] = useState('none');
  const [selectedGarmentIds, setSelectedGarmentIds] = useState<string[]>([]);
  const [customPrompt, setCustomPrompt] = useState('');
  const [loading, setLoading] = useState(false);
  const [adviceOutput, setAdviceOutput] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState('');

  // Saved AI Advisor reports state
  const [savedAdvisorReports, setSavedAdvisorReports] = useState<AIAdvisorReportRecord[]>(() => fieldDictionaryService.getAiReports());

  const [dataVersion, setDataVersion] = useState(0);

  useEffect(() => {
    const unsubDict = fieldDictionaryService.subscribe(() => {
      setSavedAdvisorReports(fieldDictionaryService.getAiReports());
    });
    const unsubData = dataService.subscribe(() => {
      setDataVersion(v => v + 1);
    });
    return () => {
      unsubDict();
      unsubData();
    };
  }, []);

  const targetGarment = clothes.find(c => c.id === selectedGarmentForAnalysis) || clothes[0];
  const targetReport = reports.find(r => r.id === selectedReportForAnalysis) || reports[0];

  // Linked junctions and wearing reports for targetGarment (comprehensive multi-key match)
  const allReportsForAdvisor = useMemo(() => {
    const map = new Map<string, WearingReport>();
    (reports || []).forEach(r => { if (r?.id) map.set(r.id, r); });
    dataService.getReports().forEach(r => { if (r?.id) map.set(r.id, r); });
    return Array.from(map.values());
  }, [reports, dataVersion]);

  const allJunctionsForAdvisor = useMemo(() => {
    const map = new Map<string, ReportClothesJunction>();
    (junctions || []).forEach(j => { if (j?.id) map.set(j.id, j); });
    dataService.getJunctions().forEach(j => { if (j?.id) map.set(j.id, j); });
    return Array.from(map.values());
  }, [junctions, dataVersion]);

  const targetGarmentLinkedJunctions = useMemo(() => {
    if (!targetGarment) return [];
    const targetId = (targetGarment.id || '').trim();
    const targetName = (targetGarment.Name || targetGarment.name || '').trim().toLowerCase();

    return allJunctionsForAdvisor.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : (j.clothesId || j['clothes(new)']);
      const cTitle = (typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : (j.clothesTitle || '')).trim().toLowerCase();
      if (targetId && cId === targetId) return true;
      if (targetName && cTitle) {
        if (cTitle === targetName) return true;
        if (cTitle.includes(targetName) || targetName.includes(cTitle)) return true;
      }
      return false;
    });
  }, [allJunctionsForAdvisor, targetGarment]);

  const targetGarmentLinkedReports = useMemo(() => {
    if (!targetGarment) return [];
    const targetId = (targetGarment.id || '').trim();
    const targetName = (targetGarment.Name || targetGarment.name || '').trim().toLowerCase();

    const matchedReportsMap = new Map<string, WearingReport>();

    targetGarmentLinkedJunctions.forEach(j => {
      const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : (j.wearingReportId || String(j['wearing report id']));
      const rTitle = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.title : (j.wearingReportTitle || '');
      const rep = allReportsForAdvisor.find(r => r.id === rId || (rTitle && (r['Name'] || r.name || '').trim().toLowerCase() === rTitle.trim().toLowerCase()));
      const finalRep = rep || ({
        id: rId || `rep_fallback_${Date.now()}`,
        Name: rTitle || 'Wear Session',
        'wear date': j['wear date'] || new Date().toISOString()
      } as WearingReport);
      matchedReportsMap.set(finalRep.id, finalRep);
    });

    // Also check if any reports directly link to or mention this garment
    allReportsForAdvisor.forEach(r => {
      if (!matchedReportsMap.has(r.id)) {
        const rawJson = JSON.stringify(r).toLowerCase();
        if ((targetId && rawJson.includes(targetId.toLowerCase())) || (targetName && rawJson.includes(targetName))) {
          matchedReportsMap.set(r.id, r);
        }
      }
    });

    return Array.from(matchedReportsMap.values()).sort((a, b) => {
      const dateA = a['wear date'] ? new Date(a['wear date']).getTime() : 0;
      const dateB = b['wear date'] ? new Date(b['wear date']).getTime() : 0;
      return dateB - dateA;
    });
  }, [allReportsForAdvisor, targetGarmentLinkedJunctions, targetGarment]);

  // 1. Single Clothes AI - strictly single garment data + enabled clothes field definitions
  const handleRunTeableGarmentAnalysis = async () => {
    if (!targetGarment) return;
    setIsAnalyzingGarment(true);
    setGarmentAnalysisError('');
    try {
      const dictionaryPrompt = fieldDictionaryService.buildDictionaryPrompt('clothes');

      // Fetch freshest garment data from dataService
      const freshGarment = dataService.getGarment(targetGarment.id) || targetGarment;
      const effectiveGarment = {
        ...targetGarment,
        ...freshGarment,
        customFields: {
          ...(targetGarment.customFields || {}),
          ...(targetGarment.extraAttributes || {}),
          ...(freshGarment.customFields || {}),
          ...(freshGarment.extraAttributes || {})
        },
        extraAttributes: {
          ...(targetGarment.extraAttributes || {}),
          ...(targetGarment.customFields || {}),
          ...(freshGarment.customFields || {}),
          ...(freshGarment.extraAttributes || {})
        }
      };

      const res = await fetch('/api/ai/analyze-garment', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          garment: effectiveGarment,
          reports: targetGarmentLinkedReports,
          junctions: allJunctionsForAdvisor,
          apiKey: getCustomApiKey(),
          model: getSelectedModel(),
          dictionaryPrompt
        })
      });
      const data = await res.json();
      if (res.ok && data.analysis) {
        setGarmentAnalysisResult(data.analysis);
        targetGarment['AI Clothes Analysis'] = data.analysis;
        const existingVersions = targetGarment.aiReports || [];
        await dataService.addGarmentAIReportVersion(targetGarment.id, {
          content: data.analysis,
          title: `Report #${existingVersions.length + 1}`,
          model: data.model || getSelectedModel(),
          isPrimary: existingVersions.length === 0
        });
      } else {
        setGarmentAnalysisError(data.error || '单件衣服分析生成失败，请检查 API 额度或设置。');
      }
    } catch (e: any) {
      setGarmentAnalysisError(e.message || '与 AI 服务通信失败');
    } finally {
      setIsAnalyzingGarment(false);
    }
  };

  // 2. Single Wearing Report AI - strictly that report + its junctions
  const handleRunTeableReportAnalysis = async () => {
    if (!targetReport) return;
    setIsAnalyzingReport(true);
    setReportAnalysisError('');
    try {
      const sessionJunctions = junctions.filter(j => {
        const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
        return rId === targetReport.id;
      });
      const sessionClothes = sessionJunctions.map(j => {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        return clothes.find(c => c.id === cId);
      }).filter(Boolean);

      const dictionaryPrompt = `${fieldDictionaryService.buildDictionaryPrompt('wearingReport')}\n${fieldDictionaryService.buildDictionaryPrompt('junction')}`;

      const freshReport = dataService.getReports().find(r => r.id === targetReport.id) || targetReport;
      const combinedCustom = {
        ...(targetReport.customFields || {}),
        ...(targetReport.extraAttributes || {}),
        ...(freshReport.customFields || {}),
        ...(freshReport.extraAttributes || {})
      };

      // Apply filtered fields to report
      const filteredReport: Record<string, any> = {
        ...freshReport,
        ...targetReport,
        id: targetReport.id,
        customFields: combinedCustom,
        extraAttributes: combinedCustom
      };
      if (fieldDictionaryService.isFieldEnabled('Name')) filteredReport['Name'] = targetReport['Name'] || targetReport.name;
      if (fieldDictionaryService.isFieldEnabled('wear date')) filteredReport['wear date'] = targetReport['wear date'] || targetReport.wearDate;
      if (fieldDictionaryService.isFieldEnabled('shoulder feel')) filteredReport['shoulder feel'] = targetReport['shoulder feel'] || targetReport.shoulderFeel;
      if (fieldDictionaryService.isFieldEnabled('mid breast')) filteredReport['mid breast'] = targetReport['mid breast'] || targetReport.midBreast;
      if (fieldDictionaryService.isFieldEnabled('night mid breast')) filteredReport['night mid breast'] = targetReport['night mid breast'] || targetReport.nightMidBreast;
      if (fieldDictionaryService.isFieldEnabled('sweat')) filteredReport['sweat'] = targetReport['sweat'] || targetReport.sweatProp || targetReport.sweat;
      if (fieldDictionaryService.isFieldEnabled('night sweat')) filteredReport['night sweat'] = targetReport['night sweat'] || targetReport.nightSweat;
      if (fieldDictionaryService.isFieldEnabled('leg feel')) filteredReport['leg feel'] = targetReport['leg feel'] || targetReport.legFeel;
      if (fieldDictionaryService.isFieldEnabled('night leg feel')) filteredReport['night leg feel'] = targetReport['night leg feel'] || targetReport.nightLegFeel;
      if (fieldDictionaryService.isFieldEnabled('report detail')) filteredReport['report detail'] = targetReport['report detail'] || targetReport.reportDetail;
      if (fieldDictionaryService.isFieldEnabled('tags')) filteredReport['tags'] = targetReport['tags'] || targetReport.tags;
      if (fieldDictionaryService.isFieldEnabled('categories')) filteredReport['categories'] = targetReport['categories'] || targetReport.categories;

      const res = await fetch('/api/ai/analyze-wearing-report', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          report: filteredReport,
          junctions: sessionJunctions,
          clothes: sessionClothes,
          apiKey: getCustomApiKey(),
          model: getSelectedModel(),
          dictionaryPrompt
        })
      });
      const data = await res.json();
      if (res.ok && data.analysis) {
        setReportAnalysisResult(data.analysis);
        targetReport['AI Wearing Analysis'] = data.analysis;
        const existingVersions = targetReport.aiReports || [];
        await dataService.addAIReportVersion(targetReport.id, {
          content: data.analysis,
          title: `Report #${existingVersions.length + 1}`,
          model: data.model || getSelectedModel(),
          isPrimary: existingVersions.length === 0
        });
      } else {
        setReportAnalysisError(data.error || '穿着记录分析生成失败，请检查 API 额度或设置。');
      }
    } catch (e: any) {
      setReportAnalysisError(e.message || '与 AI 服务通信失败');
    } finally {
      setIsAnalyzingReport(false);
    }
  };

  const toggleGarment = (id: string) => {
    setSelectedGarmentIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  };

  const selectAllGarments = () => {
    setSelectedGarmentIds(clothes.map(c => c.id));
  };

  const clearGarmentSelection = () => {
    setSelectedGarmentIds([]);
  };

  // 3. Macro AI Advisor - Uses all clothes (or selected) + all 20 wearing report dimensions + junction records + dictionary rules
  const handleAskGemini = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      // If 0 items selected, automatically evaluate the ENTIRE wardrobe
      const candidateItems = selectedGarmentIds.length > 0
        ? clothes.filter(c => selectedGarmentIds.includes(c.id))
        : clothes;

      const dictionaryPrompt = fieldDictionaryService.buildDictionaryPrompt();

      // Format conditions for Chinese prompt
      const tempLabel = temperature === 'none' ? '不限温度/常规天气' : temperature;
      const occLabel = occasion === 'none' ? '不限场合 (None)' : occasion;
      const moodLabel = mood === 'none' ? '不限特殊体感优先 (None)' : mood;

      // Serialize Wearing Report fields with exact empirical sensory fields & linked worn garments
      const serializedReports = reports.map(r => {
        const reportJunctions = junctions.filter(j => {
          const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : String(j['wearing report id']);
          return rId === r.id;
        });
        const wornGarments = reportJunctions.map(j => {
          const title = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : (j.clothesTitle || '');
          const id = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : (j.clothesId || '');
          const type = j['Clothes Type (from clothes(new))'] || '';
          const duration = j.duration || 0;
          const washStated = j['wash stated'] || 'need wash';
          return {
            id: String(id).trim(),
            title: String(title).trim(),
            type: String(type).trim(),
            durationHours: +(duration / 3600).toFixed(1),
            washStated
          };
        });

        return {
          id: r.id,
          name: fieldDictionaryService.isFieldEnabled('Name') ? (r['Name'] || r.name) : '(excluded by user filter)',
          wearDate: fieldDictionaryService.isFieldEnabled('wear date') ? (r['wear date'] || r.wearDate) : '(excluded by user filter)',
          created: r['created'],
          categories: fieldDictionaryService.isFieldEnabled('categories') ? r['categories'] : undefined,
          tags: fieldDictionaryService.isFieldEnabled('tags') ? r['tags'] : undefined,
          shoulderFeel: fieldDictionaryService.isFieldEnabled('shoulder feel') ? (r['shoulder feel'] || r.shoulderFeel || '(未记录)') : '(excluded by user filter)',
          midBreast: fieldDictionaryService.isFieldEnabled('mid breast') ? (r['mid breast'] || r.midBreast || '(未记录)') : '(excluded by user filter)',
          nightMidBreast: fieldDictionaryService.isFieldEnabled('night mid breast') ? (r['night mid breast'] || r.nightMidBreast || '(未记录)') : '(excluded by user filter)',
          sweat: fieldDictionaryService.isFieldEnabled('sweat') ? (r['sweat'] || r['sweatProp'] || r.sweatProp || r.sweat || '(未记录)') : '(excluded by user filter)',
          nightSweat: fieldDictionaryService.isFieldEnabled('night sweat') ? (r['night sweat'] || r.nightSweat || '(未记录)') : '(excluded by user filter)',
          legFeel: fieldDictionaryService.isFieldEnabled('leg feel') ? (r['leg feel'] || r.legFeel || '(未记录)') : '(excluded by user filter)',
          nightLegFeel: fieldDictionaryService.isFieldEnabled('night leg feel') ? (r['night leg feel'] || r.nightLegFeel || '(未记录)') : '(excluded by user filter)',
          reportDetail: fieldDictionaryService.isFieldEnabled('report detail') ? (r['report detail'] || r.reportDetail || '(无额外日志随笔)') : '(excluded by user filter)',
          aiWearingAnalysis: fieldDictionaryService.isFieldEnabled('AI Wearing Analysis') ? (r['AI Wearing Analysis'] || r.aiWearingAnalysis) : undefined,
          picturesCount: fieldDictionaryService.isFieldEnabled('Pictures') ? (Array.isArray(r['Pictures']) ? r['Pictures'].length : 0) : '(excluded)',
          weather: fieldDictionaryService.isFieldEnabled('weather') ? (r['weather'] || r.customFields?.['weather']) : undefined,
          temperature: fieldDictionaryService.isFieldEnabled('temperature') ? (r['temperature'] || r.customFields?.['temperature']) : undefined,
          overallSatisfaction: fieldDictionaryService.isFieldEnabled('overall satisfaction') ? (r['overall satisfaction'] || r.customFields?.['overall satisfaction']) : undefined,
          activities: fieldDictionaryService.isFieldEnabled('activities') ? (r['activities'] || r.customFields?.['activities']) : undefined,
          aiReportsCount: Array.isArray(r.aiReports) ? r.aiReports.length : 0,
          customFields: fieldDictionaryService.isFieldEnabled('customFields') ? (r.customFields || r.extraAttributes) : undefined,
          wornGarments,
          wornGarmentTitles: wornGarments.map(g => g.title).filter(Boolean)
        };
      });

      // Serialize Junctions respecting user's checked fields in the Dictionary
      const serializedJunctions = junctions.map(j => ({
        id: j.id,
        name: j.name,
        reportId: typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : String(j['wearing report id']),
        clothesId: typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : String(j['clothes(new)']),
        clothesTitle: typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : (j.clothesTitle || undefined),
        durationSeconds: fieldDictionaryService.isFieldEnabled('duration') ? (j['duration'] || 0) : undefined,
        durationHours: fieldDictionaryService.isFieldEnabled('duration') ? (j['duration'] ? +(j['duration'] / 3600).toFixed(1) : 0) : undefined,
        washStated: fieldDictionaryService.isFieldEnabled('wash stated') ? (j['wash stated'] || 'need wash') : undefined,
        clothesType: j['Clothes Type (from clothes(new))'],
        wearDate: fieldDictionaryService.isFieldEnabled('wear date') ? j['wear date'] : undefined,
        washDate: fieldDictionaryService.isFieldEnabled('wash date') ? (j['wash date'] || j['washed date']) : undefined
      }));

      const res = await fetch('/api/ai/outfit-advice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt: customPrompt.trim() || '请根据以上天气设定（特别是中午炎热度 Hot at noon）与设定的条件，全面对比候选衣物，并深度结合过往20维真实穿搭日志与Junction时长洗护数据，推荐最适宜穿搭，指出体感预警与洗护建议。',
          items: candidateItems.map(c => ({
            id: c.id,
            name: c['Name'] || c.name,
            type: c['Clothes Type'] || c.clothesType,
            material: c['material'] || c.materialProp || c.material,
            thickClothes: c['thick clothes'] || c.thickClothes,
            longDress: c['long dress'] || c.longDress,
            hotAtNoon: c['Hot at noon'] || c.hotAtNoon,
            diaoDai: c['diao dai'] || c.diaoDai,
            sitBreastOk: c['sit breast ok'] || c.sitBreastOk,
            sideBreastOk: c['side breast ok'] || c.sideBreastOk,
            centerChest: c['center chest'] || c.centerChest,
            wet: c['wet'] || c.wet,
            wetReason: c['wet reason'] || c.wetReason || '',
            beautiful: c['beautiful'] || c.beautiful,
            beatifulReason: c['beatiful reason'] || c.beatifulReason || '',
            descriptions: c['Descriptions'] || c.descriptions || '',
            descriptions2: c['Descriptions 2'] || c.descriptions2 || '',
            lastComment: c['last comment or action'] || c.lastComment || '',
            mayThrow: c['may throw'] || c.mayThrow || '',
            tags: c.tags || [],
            totalWashedCount: c['Total Washed Count'] ?? c.totalWashedCount ?? 0,
            legacyWashed: c['wearing frequency(how many times legacy washed)'] ?? 0,
            unwashedDurationCount: c['unwashed duration count'] || c['unwash time rollup'] || 0,
            price: c['price'],
            customFields: c.customFields || {},
            fieldExplanations: c.fieldExplanations || c.customFields?._fieldExplanations || {}
          })),
          allClothes: clothes.map(c => ({
            id: c.id,
            name: c['Name'] || c.name,
            type: c['Clothes Type'] || c.clothesType,
            material: c['material'],
            thickClothes: c['thick clothes'],
            hotAtNoon: c['Hot at noon'],
            diaoDai: c['diao dai'],
            tags: c.tags || [],
            totalWashedCount: c['Total Washed Count'] || 0
          })),
          wearingReports: serializedReports,
          junctions: serializedJunctions,
          temperature: tempLabel,
          occasion: occLabel,
          mood: moodLabel,
          apiKey: getCustomApiKey(),
          model: getSelectedModel(),
          dictionaryPrompt: dictionaryPrompt
        })
      });

      const data = await res.json();
      if (res.ok && data.advice) {
        setAdviceOutput(data.advice);

        // Save as an independent report record
        const selectedNames = selectedGarmentIds.length > 0
          ? clothes.filter(c => selectedGarmentIds.includes(c.id)).map(c => c['Name'])
          : [];

        const defaultTitle = `穿搭体感报告 - ${temperature === 'none' ? '常规天气' : temperature.slice(0, 16)} (${new Date().toLocaleDateString()})`;

        fieldDictionaryService.addAiReport({
          model: getSelectedModel(),
          temperature: tempLabel,
          occasion: occLabel,
          physicalPriority: moodLabel,
          selectedGarmentIds,
          selectedGarmentNames: selectedNames,
          inquiryPrompt: customPrompt.trim() || '常规穿搭与炎热度体感分析',
          advice: data.advice,
          title: defaultTitle
        });
      } else {
        setErrorMsg(data.error || '获取 AI 穿搭顾问建议失败，请检查 API 额度与网络。');
      }
    } catch (e: any) {
      setErrorMsg(e.message || '与服务器通信发生网络错误');
    } finally {
      setLoading(false);
    }
  };

  // Cost per wear leaders
  const itemsWithPrice = clothes
    .filter(c => typeof c['price'] === 'number' && (c['price'] as number) > 0 && (c['Total Washed Count'] || 0) > 0)
    .map(c => {
      const wears = Math.max(1, c['Total Washed Count'] || 1);
      const cpw = (c['price'] as number) / wears;
      return { garment: c, cpw, wears };
    })
    .sort((a, b) => a.cpw - b.cpw);

  // Material summary
  const materialCounts: { [key: string]: number } = {};
  clothes.forEach(c => {
    const mat = c['material'] || '其他面料';
    materialCounts[mat] = (materialCounts[mat] || 0) + 1;
  });

  const topMaterials = Object.entries(materialCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  return (
    <div id="ai-advisor-view" className="space-y-5 w-full max-w-full pb-12">
      {/* Header Banner - Chinese UI */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-xl bg-purple-100 text-purple-600">
              <Sparkles className="w-4 h-4" />
            </span>
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 font-mono">
              AI 智能衣橱体感与穿搭顾问 (AI Wardrobe Advisor)
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            精准体感穿搭评估与气候诊断
          </h2>
          <p className="text-xs text-slate-500 max-w-xl leading-relaxed">
            告别泛泛空话，严格依照您的中英输入字段（如中午炎热度 Hot at noon、细肩带 diao dai、久坐胸部体感等）进行深层人体工学推导与穿搭推荐。
          </p>
        </div>

        {/* Advisor Mode Selector Buttons */}
        <div className="flex flex-wrap items-center gap-1.5 p-1 bg-slate-100 rounded-2xl border border-slate-200/80">
          <button
            type="button"
            onClick={() => setAdvisorMode('wardrobe-consultant')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              advisorMode === 'wardrobe-consultant'
                ? 'bg-white text-purple-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            全局穿搭顾问 (Macro AI)
          </button>
          <button
            type="button"
            onClick={() => setAdvisorMode('garment-diagnostic')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              advisorMode === 'garment-diagnostic'
                ? 'bg-white text-purple-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            单件衣服诊断 (Clothes AI)
          </button>
          <button
            type="button"
            onClick={() => setAdvisorMode('report-diagnostic')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              advisorMode === 'report-diagnostic'
                ? 'bg-white text-purple-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            穿着记录诊断 (Wearing AI)
          </button>
          <button
            type="button"
            onClick={() => setAdvisorMode('all-reports-archive')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              advisorMode === 'all-reports-archive'
                ? 'bg-white text-purple-900 shadow-xs font-extrabold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            多维度报告中心 (All Reports)
          </button>
        </div>
      </div>

      {/* Gemini API Key Secret Configuration Banner (Permanent, No Repeated Key-ins) */}
      <AISecretKeyBanner defaultOpen={false} />

      {/* Field & Data Bilingual Dictionary & AI Input Check (Single Unified Drawer, Zero Duplication) */}
      <FieldDictionaryDrawer initialOpen={false} />

      {/* AI GENERATION PAUSED NOTICE BANNER */}
      {isAiPaused && (
        <div className="p-4 sm:p-5 rounded-3xl bg-linear-to-r from-amber-50 via-white to-amber-50/60 border-2 border-amber-300/90 text-amber-950 space-y-2 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-amber-500 text-white flex items-center justify-center text-lg shadow-xs shrink-0">
                ⏸️
              </div>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-amber-800 font-mono">
                    System Control State
                  </span>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold font-mono bg-amber-200/80 text-amber-900 border border-amber-300">
                    AI Generation Paused (Zero Token Mode)
                  </span>
                </div>
                <h4 className="font-bold text-sm text-slate-900">
                  AI 智能生成当前处于暂停状态 · 推荐使用 Google NotebookLM
                </h4>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setAiGenerationPaused(false)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold bg-amber-600 hover:bg-amber-700 text-white shadow-xs transition-all cursor-pointer active:scale-95 shrink-0 self-start sm:self-auto"
            >
              <span>▶️ 一键恢复 AI 生成 (Resume)</span>
            </button>
          </div>

          <p className="text-xs text-amber-900/90 leading-relaxed pt-1">
            为节省处理时间与 API Token 消耗，单品诊断、穿戴日记评估及穿搭顾问调用已全局暂停。所有算法代码及历史保存报告完好无损。您可在「Local Storage」中导出排除 AI 预生成报告的 100% 纯客观事实知识库，直接交由 Google NotebookLM 进行深度推理与播客生成！
          </p>
        </div>
      )}

      {/* MODE 1: MACRO WARDROBE ADVISOR (Supports None, 0 select = All Clothes) */}
      {advisorMode === 'wardrobe-consultant' && (
        <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <div>
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-purple-600" />
                <span>智能穿搭模拟与体感决策 (Macro AI Advisor)</span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                可仅单选「中午炎热度」，其余设为「不限 (None)」；未勾选任何单品时将自动对全衣橱进行智能筛选推荐。
              </p>
            </div>

            <button
              type="button"
              onClick={async () => {
                setLoading(true);
                setErrorMsg('');
                try {
                  const dictionaryPrompt = fieldDictionaryService.buildDictionaryPrompt();
                  // Serialize all 20 Wearing Report fields
                  const serializedReports = reports.map(r => ({
                    id: r.id,
                    name: r['Name'] || r.name,
                    wearDate: r['wear date'] || r.wearDate,
                    created: r['created'],
                    categories: r['categories'],
                    tags: r['tags'],
                    shoulderFeel: r['shoulder feel'] || r.shoulderFeel,
                    midBreast: r['mid breast'] || r.midBreast,
                    nightMidBreast: r['night mid breast'] || r.nightMidBreast,
                    sweat: r['sweat'] || r.sweatProp || r.sweat,
                    nightSweat: r['night sweat'] || r.nightSweat,
                    legFeel: r['leg feel'] || r.legFeel,
                    nightLegFeel: r['night leg feel'] || r.nightLegFeel,
                    reportDetail: r['report detail'] || r.reportDetail,
                    aiWearingAnalysis: r['AI Wearing Analysis'] || r.aiWearingAnalysis,
                    picturesCount: Array.isArray(r['Pictures']) ? r['Pictures'].length : 0,
                    weather: r['weather'] || r.customFields?.['weather'],
                    temperature: r['temperature'] || r.customFields?.['temperature'],
                    overallSatisfaction: r['overall satisfaction'] || r.customFields?.['overall satisfaction'],
                    activities: r['activities'] || r.customFields?.['activities'],
                    aiReportsCount: Array.isArray(r.aiReports) ? r.aiReports.length : 0,
                    customFields: r.customFields || r.extraAttributes
                  }));

                  // Serialize Junctions (durations & wash status)
                  const serializedJunctions = junctions.map(j => ({
                    id: j.id,
                    name: j.name,
                    reportId: typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : String(j['wearing report id']),
                    clothesId: typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : String(j['clothes(new)']),
                    clothesTitle: typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : undefined,
                    durationSeconds: j['duration'] || 0,
                    durationHours: j['duration'] ? +(j['duration'] / 3600).toFixed(1) : 0,
                    washStated: j['wash stated'] || 'need wash',
                    clothesType: j['Clothes Type (from clothes(new))'],
                    wearDate: j['wear date'],
                    washDate: j['wash date'] || j['washed date']
                  }));

                  const res = await fetch('/api/ai/analyze-wardrobe', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                      clothes: clothes.map(c => ({
                        id: c.id,
                        name: c['Name'],
                        clothesType: c['Clothes Type'],
                        material: c['material'],
                        thickClothes: c['thick clothes'],
                        hotAtNoon: c['Hot at noon'],
                        diaoDai: c['diao dai'],
                        sitBreastOk: c['sit breast ok'],
                        sideBreastOk: c['side breast ok'],
                        price: c['price'],
                        totalWashedCount: c['Total Washed Count'],
                        unwashedDurationCount: c['unwashed duration count'],
                        mayThrow: c['may throw'],
                        beautiful: c['beautiful']
                      })),
                      clothesSummary: {
                        totalGarments: clothes.length,
                        materials: materialCounts,
                        comfortIssues: clothes
                          .filter(c => (c['sit breast ok'] && c['sit breast ok'] !== 'Comfortable') || (c['Hot at noon'] && c['Hot at noon'].includes('hot')))
                          .map(c => ({ name: c['Name'], issue: `${c['sit breast ok'] || ''} ${c['Hot at noon'] || ''}` }))
                      },
                      wearingReports: serializedReports,
                      junctions: serializedJunctions,
                      reportsCount: reports.length,
                      backlogCount: clothes.filter(c => (c['unwashed duration count'] || 0) > 0).length,
                      apiKey: getCustomApiKey(),
                      model: getSelectedModel(),
                      dictionaryPrompt
                    })
                  });
                  const data = await res.json();
                  if (res.ok && data.analysis) {
                    setAdviceOutput(data.analysis);
                  } else {
                    setErrorMsg(data.error || '衣橱宏观健康诊断生成失败。');
                  }
                } catch (err: any) {
                  setErrorMsg(err?.message || '网络通信异常。');
                } finally {
                  setLoading(false);
                }
              }}
              disabled={loading}
              className="px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold shadow-xs transition-all flex items-center gap-1.5 cursor-pointer active:scale-95 disabled:opacity-50"
            >
              <Sparkles className="w-3.5 h-3.5 text-amber-400" />
              <span>全衣橱健康总评</span>
            </button>
          </div>

          {/* 3-Pillar Data Stream Indicators */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 p-3 rounded-2xl bg-purple-50/70 border border-purple-100/90 text-xs">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-purple-600 shrink-0"></span>
              <span className="font-semibold text-slate-800">👗 候选衣物池:</span>
              <span className="text-purple-700 font-mono font-bold">
                {selectedGarmentIds.length === 0 ? `全衣橱 ${clothes.length} 件` : `${selectedGarmentIds.length} / ${clothes.length} 件`}
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-600 shrink-0"></span>
              <span className="font-semibold text-slate-800">📝 历史穿搭日志:</span>
              <span className="text-emerald-700 font-bold">
                {reports.length} 篇 (全部 20 维体感已接入)
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-amber-500 shrink-0"></span>
              <span className="font-semibold text-slate-800">⏱️ Junction 穿戴关联:</span>
              <span className="text-amber-700 font-bold">
                {junctions.length} 条 (含时长与洗护状态)
              </span>
            </div>
          </div>

          {/* Condition Selectors with "None" Support */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div>
              <label className="text-[11px] font-mono font-bold uppercase text-slate-700 block mb-1">
                ☀️ 中午炎热度 / 天气预报 (Hot at noon)
              </label>
              <select
                value={temperature}
                onChange={(e) => setTemperature(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-slate-300 bg-white text-xs font-medium text-slate-800 focus:ring-2 focus:ring-purple-500"
              >
                <option value="none">-- 不限天气 / 室内恒温 (None) --</option>
                <option value="Hot Sunny Noon (>30°C / 86°F)">中午炎热烈日 (&gt;30°C / 酷热考验)</option>
                <option value="Warm Mild Afternoon (24°C - 28°C)">温暖温和下午 (24°C - 28°C 适中)</option>
                <option value="Air-Conditioned Office with Noon Commute">空调办公室 + 中午短暂通勤</option>
                <option value="Cool & Breezy Evening">凉爽通风傍晚 / 阴雨天</option>
              </select>
            </div>

            <div>
              <label className="text-[11px] font-mono font-bold uppercase text-slate-700 block mb-1">
                🏷️ 穿着场合 (Occasion)
              </label>
              <select
                value={occasion}
                onChange={(e) => setOccasion(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-slate-300 bg-white text-xs font-medium text-slate-800 focus:ring-2 focus:ring-purple-500"
              >
                <option value="none">-- 不限场合 / 无特殊要求 (None) --</option>
                <option value="Daily casual & long sitting work session">日常休闲 &amp; 长时间工位久坐办公</option>
                <option value="Dining out & aesthetic photo session">外出聚餐 &amp; 美观拍照穿搭</option>
                <option value="Active walking and errand running">户外走动 &amp; 办事散步 (需防汗)</option>
                <option value="Home relaxation & lounge">居家休息 &amp; 休闲无束缚</option>
              </select>
            </div>

            <div>
              <label className="text-[11px] font-mono font-bold uppercase text-slate-700 block mb-1">
                🧘 身体重点优先 (Physical Priority)
              </label>
              <select
                value={mood}
                onChange={(e) => setMood(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-slate-300 bg-white text-xs font-medium text-slate-800 focus:ring-2 focus:ring-purple-500"
              >
                <option value="none">-- 不限体感 / 均衡综合 (None) --</option>
                <option value="Prioritize zero shoulder fatigue and light breathable airflow">肩膀零疲惫 (避免细肩带勒肩) &amp; 轻盈通风</option>
                <option value="Flattering breast silhouette with firm side support">久坐胸部不紧绷 &amp; 侧乳副乳包裹服帖</option>
                <option value="Maximum sweat absorption and quick drying">强效吸汗排湿 &amp; 快速干燥</option>
                <option value="Maximum visual aesthetic / Most beautiful dress">高颜值美观度优先 (优雅出众)</option>
              </select>
            </div>
          </div>

          {/* Garment Selector with "All Clothes" Logic */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-[11px] font-mono font-bold uppercase text-slate-700 block">
                指定候选衣物池 ({selectedGarmentIds.length === 0 ? '未勾选任何单品，默认分析全衣橱' : `已勾选 ${selectedGarmentIds.length} 件`})
              </label>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={selectAllGarments}
                  className="text-[11px] text-purple-700 hover:underline font-semibold cursor-pointer"
                >
                  全选 ({clothes.length})
                </button>
                <span className="text-slate-300">|</span>
                <button
                  type="button"
                  onClick={clearGarmentSelection}
                  className="text-[11px] text-slate-500 hover:text-slate-800 font-semibold cursor-pointer"
                >
                  清空选择 (分析全衣橱)
                </button>
              </div>
            </div>

            <div className="flex flex-wrap gap-1.5 max-h-36 overflow-y-auto p-2.5 bg-slate-50 rounded-2xl border border-slate-200">
              {clothes.map(c => {
                const isSelected = selectedGarmentIds.includes(c.id);
                return (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => toggleGarment(c.id)}
                    className={`px-2.5 py-1 rounded-xl text-xs font-medium transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-purple-600 text-white shadow-xs'
                        : 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    {c['Name']} ({c['Clothes Type']})
                  </button>
                );
              })}
            </div>
            {selectedGarmentIds.length === 0 && (
              <p className="text-[11px] text-purple-700 font-medium bg-purple-50 px-3 py-1.5 rounded-xl border border-purple-100">
                ✨ 提示：当前未勾选特定单品，AI 将针对您设定的温度（如中午炎热度），直接从全衣橱 ({clothes.length} 件衣服) 中为您匹配并挑选最适宜穿搭！
              </p>
            )}
          </div>

          {/* Custom Question Box */}
          <div>
            <label className="text-[11px] font-mono font-bold uppercase text-slate-700 block mb-1">
              补充诉求或具体提问 (可选)
            </label>
            <div className="flex flex-col sm:flex-row gap-2">
              <input
                type="text"
                value={customPrompt}
                onChange={(e) => setCustomPrompt(e.target.value)}
                placeholder="例如：中午很热要在外走动半小时，哪套搭配最不容易闷汗勒肩？"
                className="flex-1 px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
              <button
                onClick={handleAskGemini}
                disabled={loading}
                className="px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 disabled:opacity-60 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-1.5 shrink-0 cursor-pointer"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                <span>{loading ? '正在分析计算...' : '获取穿搭与体感分析'}</span>
              </button>
            </div>
          </div>

          {errorMsg && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 space-y-2">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
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
                    获取个人 API Key 并保存到上方配置栏
                  </a>
                </div>
              )}
            </div>
          )}

          {/* Advice Output Box */}
          {adviceOutput && (
            <div className="p-5 rounded-2xl bg-purple-50/50 border border-purple-200 space-y-2">
              <div className="flex items-center justify-between border-b border-purple-100 pb-2">
                <div className="flex items-center gap-2 text-purple-900 font-bold text-xs">
                  <Sparkles className="w-4 h-4 text-purple-600" />
                  <span>最新生成的穿搭体感分析报告 (已自动存入下方报告档案)</span>
                </div>
                <span className="text-[10px] text-purple-700 font-semibold bg-purple-100 px-2 py-0.5 rounded-md">
                  独立记录已生成
                </span>
              </div>
              <div className="text-xs sm:text-sm text-slate-800 leading-relaxed whitespace-pre-wrap font-sans">
                {adviceOutput}
              </div>
            </div>
          )}

          {/* Saved Independent AI Reports List */}
          <div className="pt-4 border-t border-slate-200/80">
            <SavedReportsList reports={savedAdvisorReports} />
          </div>
        </div>
      )}

      {/* MODE 2: CLOTHES DIAGNOSTIC (Strictly this clothes fields only) */}
      {advisorMode === 'garment-diagnostic' && (
        <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <Shirt className="w-4 h-4 text-purple-600" />
                <h3 className="text-base font-bold text-slate-900">
                  单件衣服专属 AI 诊断 (Clothes AI)
                </h3>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200 font-mono">
                  严格仅分析此衣服字段
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                只提取该单品在数据库中登记的专属字段（面料、厚薄度、Hot at noon、diao dai、久坐胸部体感等），严格隔离其他干扰数据。
              </p>
            </div>

            <div className="flex items-center gap-2">
              <select
                value={selectedGarmentForAnalysis}
                onChange={(e) => {
                  setSelectedGarmentForAnalysis(e.target.value);
                  const g = clothes.find(c => c.id === e.target.value);
                  setGarmentAnalysisResult(g?.['AI Clothes Analysis'] || null);
                  setGarmentAnalysisError('');
                }}
                className="px-3 py-2 rounded-xl border border-slate-300 text-xs font-bold bg-white text-slate-900 max-w-xs shadow-2xs"
              >
                {clothes.map(c => (
                  <option key={c.id} value={c.id}>
                    {c['Name']} ({c['Clothes Type']})
                  </option>
                ))}
              </select>

              <button
                type="button"
                onClick={handleRunTeableGarmentAnalysis}
                disabled={isAnalyzingGarment || !targetGarment}
                className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white text-xs font-bold shadow-xs transition-all flex items-center gap-1.5 shrink-0 cursor-pointer active:scale-95"
              >
                <Sparkles className={`w-3.5 h-3.5 ${isAnalyzingGarment ? 'animate-spin' : ''}`} />
                <span>{isAnalyzingGarment ? '诊断分析中...' : '生成/更新诊断'}</span>
              </button>
            </div>
          </div>

          {/* Quick Garment Field Preview */}
          {targetGarment && (
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/90 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-800">
                  当前单品登记字段明细: {targetGarment['Name']}
                </span>
                <span className="text-[11px] text-purple-700 font-semibold cursor-pointer hover:underline" onClick={() => handleSelectClothesItem(targetGarment)}>
                  打开衣物详情弹窗 ↗
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">中午炎热度 (Hot at noon)</span>
                  <span className="font-bold text-slate-800">{targetGarment['Hot at noon'] || '未记录'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">肩带款式 (diao dai)</span>
                  <span className="font-bold text-slate-800">{targetGarment['diao dai'] || '未记录'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">久坐与侧面胸型</span>
                  <span className="font-bold text-slate-800">{targetGarment['sit breast ok'] || '未记录'} / {targetGarment['side breast ok'] || '未记录'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">面料与厚度</span>
                  <span className="font-bold text-slate-800">{targetGarment['material'] || '未填'} · {targetGarment['thick clothes'] || '未填'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">中位聚拢 (Center Chest)</span>
                  <span className="font-bold text-slate-800">{targetGarment['center chest'] || '未记录'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">出汗与干爽 (Wet)</span>
                  <span className="font-bold text-slate-800">{targetGarment['wet'] || 'dry'} {targetGarment['wet reason'] ? `(${targetGarment['wet reason']})` : ''}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">裙长与款式 (Long Dress)</span>
                  <span className="font-bold text-slate-800">{targetGarment['long dress'] || '未特别标注'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">洗护次数与单价</span>
                  <span className="font-bold text-slate-800">{targetGarment['Total Washed Count'] ?? 0} 次 · {targetGarment['price'] ? `$${targetGarment['price']}` : '未填'}</span>
                </div>
              </div>

              {/* Long Descriptions & Actions Grounding Inspection */}
              {(targetGarment['Descriptions'] || targetGarment.descriptions || targetGarment['Descriptions 2'] || targetGarment.descriptions2 || targetGarment['last comment or action'] || targetGarment.lastComment || (targetGarment.customFields && Object.keys(targetGarment.customFields).filter(k => k !== '_fieldExplanations').length > 0)) && (
                <div className="p-2.5 rounded-xl bg-purple-50/80 border border-purple-100 text-xs space-y-1.5">
                  <div className="flex items-center gap-1.5 font-bold text-purple-900 text-[11px]">
                    <Sparkles className="w-3 h-3 text-purple-600" />
                    <span>已接入 AI 诊断的用户长篇详述与自定义属性:</span>
                  </div>
                  {(targetGarment['Descriptions'] || targetGarment.descriptions) && (
                    <div className="text-[11px] text-slate-700 bg-white/80 p-1.5 rounded-lg border border-purple-100">
                      <strong className="text-purple-800">描述 1 (Descriptions): </strong>
                      <span className="line-clamp-2">{targetGarment['Descriptions'] || targetGarment.descriptions}</span>
                    </div>
                  )}
                  {(targetGarment['Descriptions 2'] || targetGarment.descriptions2) && (
                    <div className="text-[11px] text-slate-700 bg-white/80 p-1.5 rounded-lg border border-purple-100">
                      <strong className="text-purple-800">描述 2 (Descriptions 2): </strong>
                      <span className="line-clamp-2">{targetGarment['Descriptions 2'] || targetGarment.descriptions2}</span>
                    </div>
                  )}
                  {(targetGarment['last comment or action'] || targetGarment.lastComment) && (
                    <div className="text-[11px] text-slate-700 bg-white/80 p-1.5 rounded-lg border border-purple-100">
                      <strong className="text-purple-800">最新行动 (Last Action): </strong>
                      <span>{targetGarment['last comment or action'] || targetGarment.lastComment}</span>
                    </div>
                  )}
                  {targetGarment.customFields && Object.keys(targetGarment.customFields).filter(k => k !== '_fieldExplanations').length > 0 && (
                    <div className="text-[11px] text-slate-700 bg-white/90 p-2 rounded-xl border border-purple-100 space-y-1">
                      <div className="flex items-center gap-1 font-bold text-purple-900 text-[11px]">
                        <Tag className="w-3 h-3 text-purple-600" />
                        <span>动态自定义属性 (含未来新增属性，已全部纳入 AI 分析):</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 pt-0.5">
                        {Object.entries(targetGarment.customFields).filter(([k]) => k !== '_fieldExplanations').map(([k, v]) => (
                          <div key={k} className="p-1.5 rounded-lg bg-purple-50/70 border border-purple-200/60 min-w-0">
                            <span className="font-mono font-bold text-purple-900 text-[10px] block truncate">{k}:</span>
                            <span className="text-[11px] text-slate-800 font-medium break-words [word-break:normal] [overflow-wrap:break-word] whitespace-pre-wrap select-text block">
                              {typeof v === 'object' ? JSON.stringify(v) : String(v)}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Linked Wearing Reports Preview */}
                  {targetGarmentLinkedReports.length > 0 && (
                    <div className="text-[11px] text-slate-700 bg-white/90 p-2 rounded-xl border border-indigo-100 space-y-1">
                      <div className="flex items-center gap-1 font-bold text-indigo-950 text-[11px]">
                        <Sparkles className="w-3 h-3 text-indigo-600" />
                        <span>已关联实测穿搭报告 ({targetGarmentLinkedReports.length} 份，全部纳入 AI 综合诊断):</span>
                      </div>
                      <div className="space-y-1 pt-0.5">
                        {targetGarmentLinkedReports.slice(0, 3).map((r, idx) => (
                          <div key={r.id || idx} className="p-1.5 rounded-lg bg-indigo-50/50 border border-indigo-100/80 text-[11px] space-y-0.5">
                            <div className="flex items-center justify-between font-bold text-indigo-900">
                              <span>【实测报告 {idx + 1}】{r.name || r['Name'] || r.id}</span>
                              <span className="font-mono text-[10px] text-indigo-600 font-normal">{r.wearDate || r['wear date'] || ''}</span>
                            </div>
                            <div className="text-slate-600 text-[10px] space-x-2 truncate">
                              {r['mid breast'] && <span>胸部: {r['mid breast']}</span>}
                              {r['night mid breast'] && <span>晚间: {r['night mid breast']}</span>}
                              {r['night sweat'] && <span>排汗: {r['night sweat']}</span>}
                              {r['night leg feel'] && <span>下身: {r['night leg feel']}</span>}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {garmentAnalysisError && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 space-y-2">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
                <span className="font-medium">{garmentAnalysisError}</span>
              </div>
              {(garmentAnalysisError.includes('prepayment') || garmentAnalysisError.includes('402')) && (
                <div className="pt-1 text-[11px] text-slate-700 flex flex-wrap items-center gap-2">
                  <span className="font-bold text-slate-900">提示:</span>
                  <a href="https://ai.studio/projects" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                    在 AI Studio 管理配额与充值 ↗
                  </a>
                  <span>或</span>
                  <a href="https://aistudio.google.com/apikey" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                    获取个人 API Key 并保存到上方配置栏
                  </a>
                </div>
              )}
            </div>
          )}

          {/* Diagnostic Result Box */}
          {garmentAnalysisResult ? (
            <div className="p-5 rounded-2xl bg-linear-to-br from-purple-50/50 via-white to-slate-50 border border-purple-200/80 shadow-2xs space-y-3">
              <div className="flex items-center justify-between border-b border-purple-100 pb-2">
                <div className="flex items-center gap-2 text-purple-900 font-bold text-xs font-mono uppercase">
                  <Sparkles className="w-4 h-4 text-purple-600" />
                  <span>单品专属诊断报告 (自动同步至单品卡片)</span>
                </div>
              </div>
              <div className="text-xs sm:text-sm text-slate-800 leading-relaxed whitespace-pre-wrap font-sans">
                {garmentAnalysisResult}
              </div>
            </div>
          ) : (
            <div className="p-8 rounded-2xl bg-slate-50/60 border border-dashed border-slate-200 text-center space-y-2">
              <p className="text-xs text-slate-600 font-medium">
                该单品尚未生成专属 AI 诊断报告
              </p>
              <p className="text-[11px] text-slate-400">
                点击右上角<strong>「生成/更新诊断」</strong>，AI 将严格对照其中英文登记字段输出中文人体工学分析。
              </p>
            </div>
          )}
        </div>
      )}

      {/* MODE 3: WEARING REPORT DIAGNOSTIC (Strictly wearing report + its junctions) */}
      {advisorMode === 'report-diagnostic' && (
        <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <FileText className="w-4 h-4 text-purple-600" />
                <h3 className="text-base font-bold text-slate-900">
                  单次穿着记录专属 AI 诊断 (Wearing Report AI)
                </h3>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-purple-50 text-purple-700 border border-purple-200 font-mono">
                  严格仅分析本次记录与其关联穿戴
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                严格基于本次记录登记的体感（肩酸、昼夜出汗、胸口）及关联的穿戴单品与洗护状态，提供真实客观的诊断反馈。
              </p>
            </div>

            <div className="flex items-center gap-2">
              <select
                value={selectedReportForAnalysis}
                onChange={(e) => {
                  setSelectedReportForAnalysis(e.target.value);
                  const r = reports.find(x => x.id === e.target.value);
                  setReportAnalysisResult(r?.['AI Wearing Analysis'] || null);
                  setReportAnalysisError('');
                }}
                className="px-3 py-2 rounded-xl border border-slate-300 text-xs font-bold bg-white text-slate-900 max-w-xs shadow-2xs"
              >
                {reports.map(r => (
                  <option key={r.id} value={r.id}>
                    {r['Name'] || `Session ${toDateKey(r['wear date'])}`} ({toDateKey(r['wear date'])})
                  </option>
                ))}
              </select>

              <button
                type="button"
                onClick={handleRunTeableReportAnalysis}
                disabled={isAnalyzingReport || !targetReport}
                className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white text-xs font-bold shadow-xs transition-all flex items-center gap-1.5 shrink-0 cursor-pointer active:scale-95"
              >
                <Sparkles className={`w-3.5 h-3.5 ${isAnalyzingReport ? 'animate-spin' : ''}`} />
                <span>{isAnalyzingReport ? '诊断分析中...' : '生成/更新诊断'}</span>
              </button>
            </div>
          </div>

          {/* Quick Wearing Report Session Fields Preview */}
          {targetReport && (
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/90 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-800">
                  当前穿着记录明细: {targetReport['Name']} ({toDateKey(targetReport['wear date'])})
                </span>
                <span className="text-[11px] text-purple-700 font-semibold cursor-pointer hover:underline" onClick={() => onSelectReport?.(targetReport)}>
                  打开穿着详情弹窗 ↗
                </span>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">肩膀感受 (shoulder feel)</span>
                  <span className="font-bold text-slate-800">{targetReport['shoulder feel'] || '未记录'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">昼间出汗 (sweat)</span>
                  <span className="font-bold text-slate-800">{targetReport['sweat'] || '未记录'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">胸口体感 (mid breast)</span>
                  <span className="font-bold text-slate-800">{targetReport['mid breast'] || '未记录'}</span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-slate-200">
                  <span className="text-[10px] font-mono text-slate-400 block">关联单品数量</span>
                  <span className="font-bold text-slate-800">
                    {junctions.filter(j => {
                      const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
                      return rId === targetReport.id;
                    }).length} 件衣服
                  </span>
                </div>
              </div>
            </div>
          )}

          {reportAnalysisError && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700 space-y-2">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
                <span className="font-medium">{reportAnalysisError}</span>
              </div>
              {(reportAnalysisError.includes('prepayment') || reportAnalysisError.includes('402')) && (
                <div className="pt-1 text-[11px] text-slate-700 flex flex-wrap items-center gap-2">
                  <span className="font-bold text-slate-900">提示:</span>
                  <a href="https://ai.studio/projects" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                    在 AI Studio 管理配额与充值 ↗
                  </a>
                  <span>或</span>
                  <a href="https://aistudio.google.com/apikey" target="_blank" rel="noreferrer" className="font-bold text-purple-700 underline hover:text-purple-800">
                    获取个人 API Key 并保存到上方配置栏
                  </a>
                </div>
              )}
            </div>
          )}

          {/* Diagnostic Result Box */}
          {reportAnalysisResult ? (
            <div className="p-5 rounded-2xl bg-linear-to-br from-indigo-50/50 via-white to-purple-50 border border-indigo-200/80 shadow-2xs space-y-3">
              <div className="flex items-center justify-between border-b border-indigo-100 pb-2">
                <div className="flex items-center gap-2 text-indigo-900 font-bold text-xs font-mono uppercase">
                  <Sparkles className="w-4 h-4 text-indigo-600" />
                  <span>穿着专属诊断报告 (自动同步至穿着记录)</span>
                </div>
              </div>
              <div className="text-xs sm:text-sm text-slate-800 leading-relaxed whitespace-pre-wrap font-sans">
                {reportAnalysisResult}
              </div>
            </div>
          ) : (
            <div className="p-8 rounded-2xl bg-slate-50/60 border border-dashed border-slate-200 text-center space-y-2">
              <p className="text-xs text-slate-600 font-medium">
                该穿着场次尚未生成专属 AI 诊断报告
              </p>
              <p className="text-[11px] text-slate-400">
                点击右上角<strong>「生成/更新诊断」</strong>，AI 将根据该次记录的体感输入与关联穿搭生成报告。
              </p>
            </div>
          )}
        </div>
      )}

      {/* MODE 4: UNIFIED MULTI-VERSION AI REPORT ARCHIVE (Clothes, Wearing, Junction, Macro) */}
      {advisorMode === 'all-reports-archive' && (
        <UnifiedAIReportArchive
          clothes={clothes}
          reports={reports}
          junctions={junctions}
          onSelectGarment={onSelectGarment}
          onSelectReport={onSelectReport}
        />
      )}

      {/* Wardrobe Economics & Sensory Analytics */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Cost Per Wear Leaderboard */}
        <div className="bg-white rounded-3xl p-5 border border-slate-200/80 shadow-xs space-y-3">
          <div className="flex items-center gap-2">
            <DollarSign className="w-4 h-4 text-emerald-600" />
            <div>
              <h4 className="text-sm font-bold text-slate-900">单次穿着成本性价比榜 (Cost-per-Wear)</h4>
              <p className="text-[11px] text-slate-400">结合买入价格与实际穿着洗涤次数计算出的高利用率单品</p>
            </div>
          </div>

          <div className="space-y-2">
            {itemsWithPrice.slice(0, 5).map(({ garment, cpw, wears }) => (
              <div
                key={garment.id}
                onClick={() => onSelectGarment?.(garment)}
                className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 hover:bg-slate-100/80 cursor-pointer transition-colors text-xs"
              >
                <div className="min-w-0 pr-2">
                  <span className="font-bold text-slate-800 truncate block">{garment['Name']}</span>
                  <span className="text-[10px] text-slate-400">{garment['Clothes Type']} · 累计洗涤 {wears} 次</span>
                </div>
                <div className="text-right shrink-0">
                  <span className="font-bold text-emerald-700">${cpw.toFixed(2)}</span>
                  <span className="text-[10px] text-slate-400 block">/ 次穿着</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Fabric Breakdown */}
        <div className="bg-white rounded-3xl p-5 border border-slate-200/80 shadow-xs space-y-3">
          <div className="flex items-center gap-2">
            <Shirt className="w-4 h-4 text-purple-600" />
            <div>
              <h4 className="text-sm font-bold text-slate-900">衣橱面料分布 (Fabric Composition)</h4>
              <p className="text-[11px] text-slate-400">掌握衣橱材质占比，精准搭配透气与保暖需求</p>
            </div>
          </div>

          <div className="space-y-2.5">
            {topMaterials.map(([mat, count]) => {
              const percent = Math.round((count / clothes.length) * 100);
              return (
                <div key={mat} className="space-y-1">
                  <div className="flex justify-between text-xs font-semibold">
                    <span className="text-slate-700 truncate">{mat}</span>
                    <span className="text-slate-500 text-[11px]">{count} 件 ({percent}%)</span>
                  </div>
                  <div className="w-full h-2 rounded-full bg-slate-100 overflow-hidden">
                    <div
                      className="h-full bg-purple-600 rounded-full"
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
