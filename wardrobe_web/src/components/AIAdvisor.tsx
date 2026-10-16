import React, { useState } from 'react';
import { ClothesItem } from '../types';
import { Sparkles, ThermometerSun, RefreshCw, Send, DollarSign, PieChart, ShieldCheck, Heart, Shirt, CheckCircle2 } from 'lucide-react';

interface AIAdvisorProps {
  clothes: ClothesItem[];
  onSelectGarment: (garment: ClothesItem) => void;
}

export const AIAdvisor: React.FC<AIAdvisorProps> = ({ clothes, onSelectGarment }) => {
  const [temperature, setTemperature] = useState('Hot Sunny Noon (>30°C / 86°F)');
  const [occasion, setOccasion] = useState('Daily casual & long sitting work session');
  const [mood, setMood] = useState('Prioritize zero shoulder fatigue and light breathable airflow');
  const [selectedGarmentIds, setSelectedGarmentIds] = useState<string[]>([]);
  const [customPrompt, setCustomPrompt] = useState('');
  const [loading, setLoading] = useState(false);
  const [adviceOutput, setAdviceOutput] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState('');

  const toggleGarment = (id: string) => {
    setSelectedGarmentIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  };

  const handleAskGemini = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const selectedItems = clothes.filter(c => selectedGarmentIds.includes(c.id));
      const res = await fetch('/api/ai/outfit-advice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt: customPrompt.trim() || 'Provide sensory feedback, thermal predictions, and styling advice for this combination.',
          items: selectedItems.map(c => ({
            name: c['Name'],
            type: c['Clothes Type'],
            material: c['material'],
            hotAtNoon: c['Hot at noon'],
            diaoDai: c['diao dai'],
            sitBreastOk: c['sit breast ok'],
            beautiful: c['beautiful']
          })),
          temperature,
          occasion,
          mood
        })
      });

      const data = await res.json();
      if (res.ok && data.advice) {
        setAdviceOutput(data.advice);
      } else {
        setErrorMsg(data.error || 'Could not retrieve advice from the AI model.');
      }
    } catch (e: any) {
      setErrorMsg(e.message || 'Network error communicating with the server.');
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
    const mat = c['material'] || 'Other';
    materialCounts[mat] = (materialCounts[mat] || 0) + 1;
  });

  const topMaterials = Object.entries(materialCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  return (
    <div id="ai-advisor-view" className="space-y-5 w-full max-w-full">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="p-1.5 rounded-xl bg-purple-100 text-purple-600">
              <Sparkles className="w-4 h-4" />
            </span>
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 font-mono">
              AI Clothes Analysis
            </span>
          </div>
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Sensory Styling & Climate Consultant
          </h2>
          <p className="text-xs text-slate-500 max-w-xl leading-relaxed">
            Synthesize fabric breathability, sitting ergonomics, midday heat ratings, and strap comfort into tailored recommendations.
          </p>
        </div>
      </div>

      {/* Main Interactive Consultant Grid */}
      <div className="bg-white rounded-3xl p-5 sm:p-6 border border-slate-200/80 shadow-xs space-y-4">
        <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-purple-600" />
          Wardrobe Consultant Query
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div>
            <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1">
              Hot at noon / Weather
            </label>
            <select
              value={temperature}
              onChange={(e) => setTemperature(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-white text-xs font-medium text-slate-800"
            >
              <option value="Hot Sunny Noon (>30°C / 86°F)">Hot Sunny Noon (&gt;30°C / 86°F)</option>
              <option value="Warm Mild Afternoon (24°C - 28°C)">Warm Mild Afternoon (24°C - 28°C)</option>
              <option value="Air-Conditioned Office with Noon Commute">Air-Conditioned Office + Noon Commute</option>
              <option value="Cool & Breezy Evening">Cool &amp; Breezy Evening</option>
            </select>
          </div>

          <div>
            <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1">
              Occasion
            </label>
            <select
              value={occasion}
              onChange={(e) => setOccasion(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-white text-xs font-medium text-slate-800"
            >
              <option value="Daily casual & long sitting work session">Daily casual &amp; long sitting work</option>
              <option value="Dining out & aesthetic photo session">Dining out &amp; aesthetic photo session</option>
              <option value="Active walking and errand running">Active walking and errand running</option>
              <option value="Home relaxation & lounge">Home relaxation &amp; lounge</option>
            </select>
          </div>

          <div>
            <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1">
              Physical Priority
            </label>
            <select
              value={mood}
              onChange={(e) => setMood(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-white text-xs font-medium text-slate-800"
            >
              <option value="Prioritize zero shoulder fatigue and light breathable airflow">Zero shoulder fatigue &amp; airflow</option>
              <option value="Flattering breast silhouette with firm side support">Flattering breast silhouette &amp; support</option>
              <option value="Maximum sweat absorption and quick drying">Maximum sweat absorption &amp; quick dry</option>
              <option value="Maximum visual aesthetic / Most beautiful dress">Maximum visual aesthetic &amp; elegance</option>
            </select>
          </div>
        </div>

        {/* Garment Selector */}
        <div>
          <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1.5">
            Evaluate Specific Garments ({selectedGarmentIds.length} selected):
          </label>
          <div className="flex flex-wrap gap-1.5 max-h-36 overflow-y-auto p-2.5 bg-slate-50 rounded-2xl border border-slate-100">
            {clothes.map(c => {
              const isSelected = selectedGarmentIds.includes(c.id);
              return (
                <button
                  key={c.id}
                  type="button"
                  onClick={() => toggleGarment(c.id)}
                  className={`px-2.5 py-1 rounded-xl text-xs font-medium transition-all ${
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
        </div>

        {/* Custom Question Box */}
        <div>
          <label className="text-[10px] font-mono font-bold uppercase text-slate-600 block mb-1">
            Specific Inquiry
          </label>
          <div className="flex flex-col sm:flex-row gap-2">
            <input
              type="text"
              value={customPrompt}
              onChange={(e) => setCustomPrompt(e.target.value)}
              placeholder="e.g. Which bra pairs best with my blue chiffon dress for an all-day desk session?"
              className="flex-1 px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-purple-500/20"
            />
            <button
              onClick={handleAskGemini}
              disabled={loading}
              className="px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 disabled:opacity-60 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-1.5 shrink-0"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              {loading ? 'Consulting...' : 'Get Advice'}
            </button>
          </div>
        </div>

        {errorMsg && (
          <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700">
            {errorMsg}
          </div>
        )}

        {/* Advice Output Box */}
        {adviceOutput && (
          <div className="p-5 rounded-2xl bg-purple-50/50 border border-purple-100 space-y-2">
            <div className="flex items-center gap-2 text-purple-900 font-bold text-xs">
              <Sparkles className="w-4 h-4 text-purple-600" />
              <span>Sensory & Ergonomic Analysis</span>
            </div>
            <div className="text-xs text-slate-700 leading-relaxed whitespace-pre-wrap">
              {adviceOutput}
            </div>
          </div>
        )}
      </div>

      {/* Wardrobe Economics & Sensory Analytics */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Cost Per Wear Leaderboard */}
        <div className="bg-white rounded-3xl p-5 border border-slate-200/80 shadow-xs space-y-3">
          <div className="flex items-center gap-2">
            <DollarSign className="w-4 h-4 text-emerald-600" />
            <div>
              <h4 className="text-sm font-bold text-slate-900">Cost-per-Wear Value</h4>
              <p className="text-[11px] text-slate-400">Garments delivering highest value per wash cycle</p>
            </div>
          </div>

          <div className="space-y-2">
            {itemsWithPrice.slice(0, 5).map(({ garment, cpw, wears }) => (
              <div
                key={garment.id}
                onClick={() => onSelectGarment(garment)}
                className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 hover:bg-slate-100/80 cursor-pointer transition-colors text-xs"
              >
                <div className="min-w-0 pr-2">
                  <span className="font-bold text-slate-800 truncate block">{garment['Name']}</span>
                  <span className="text-[10px] text-slate-400">{garment['Clothes Type']} · {wears} washes</span>
                </div>
                <div className="text-right shrink-0">
                  <span className="font-bold text-emerald-700">${cpw.toFixed(2)}</span>
                  <span className="text-[10px] text-slate-400 block">/ wear</span>
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
              <h4 className="text-sm font-bold text-slate-900">Fabric Composition</h4>
              <p className="text-[11px] text-slate-400">Distribution of sensory materials in wardrobe</p>
            </div>
          </div>

          <div className="space-y-2.5">
            {topMaterials.map(([mat, count]) => {
              const percent = Math.round((count / clothes.length) * 100);
              return (
                <div key={mat} className="space-y-1">
                  <div className="flex justify-between text-xs font-semibold">
                    <span className="text-slate-700 truncate">{mat}</span>
                    <span className="text-slate-500 text-[11px]">{count} items ({percent}%)</span>
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
