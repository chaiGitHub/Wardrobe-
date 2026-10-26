import { ClothesItem, ReportPreset } from '../types';
import { dataService } from './dataService';

export interface ReportFieldMeta {
  key: string;
  label: string;
  description: string;
  category: 'classification' | 'comfort' | 'fit' | 'care' | 'economics';
  type: 'string' | 'number' | 'date';
}

export const AVAILABLE_REPORT_FIELDS: ReportFieldMeta[] = [
  // Multi-Scenario Climate & Thermal Ergonomics
  {
    key: 'when with aircond: hot feeling',
    label: 'when with aircond: hot feeling',
    description: 'Thermal sensation in AC room (not cold, not so cold, hot, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'when with aircond: leg feeling',
    label: 'when with aircond: leg feeling',
    description: 'Leg coldness/thermal feel in AC room (not cold, not so cold, hot, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'when without aircond: hot feeling',
    label: 'when without aircond: hot feeling',
    description: 'Upper body thermal feeling without AC (hot, not hot, comfortable, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'when without aircond: leg feeling',
    label: 'when without aircond: leg feeling',
    description: 'Leg feeling at normal room temperature without AC (not cold, hot, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'when sleep under fan: hot feeling',
    label: 'when sleep under fan: hot feeling',
    description: 'Upper body thermal comfort while sleeping under fan (cool, hot, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'when sleep under fan: leg feeling',
    label: 'when sleep under fan: leg feeling',
    description: 'Leg temperature and airflow feeling when sleeping under fan (not cold, hot, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'Hot at noon',
    label: 'Hot at noon',
    description: 'Midday peak heat sensation (Hot, Middle, Not hot, undefined)',
    category: 'comfort',
    type: 'string'
  },

  // Breast & Bra Ergonomics
  {
    key: 'sit breast ok: With 聚拢bra',
    label: 'sit breast ok: With 聚拢bra',
    description: 'Sitting breast silhouette and cleavage stability with gathering/push-up bra (ok, not ok, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'sit breast ok: with side out bra',
    label: 'sit breast ok: with side out bra',
    description: 'Sitting breast shape with side-cut/side-out bra (ok, ugly, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'side breast ok: with 聚拢 bra',
    label: 'side breast ok: with 聚拢 bra',
    description: 'Side profile breast shape & firmness with gathering bra (ok, side out, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'side breast ok: with side out bra',
    label: 'side breast ok: with side out bra',
    description: 'Side profile breast silhouette with side-out bra (ok, side breast out, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'sit breast ok',
    label: 'sit breast ok',
    description: 'General sitting breast posture silhouette (sit breast shape ok, stand breast ugly, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'side breast ok',
    label: 'side breast ok',
    description: 'General side profile breast fullness (side breast shape ok, side breast out, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'center chest',
    label: 'center chest',
    description: 'Center sternum comfort & gore pressure (center chest, no center chest, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'diao dai',
    label: 'diao dai (Strap)',
    description: 'Shoulder strap cut & pain/slip feeling (diao dai, not diao dai, wide diao dai, undefined)',
    category: 'fit',
    type: 'string'
  },

  // Clothes Classification & Properties
  {
    key: 'Clothes Type',
    label: 'Clothes Type',
    description: 'Garment category (Dress, bra, Tshirt, Pant, etc.)',
    category: 'classification',
    type: 'string'
  },
  {
    key: 'material',
    label: 'material',
    description: 'Fabric material composition (chiffon, cotton, lace, silk, etc.)',
    category: 'classification',
    type: 'string'
  },
  {
    key: 'long dress',
    label: 'long dress (Length)',
    description: 'Dress length style (short dress, middle dress, long dress, undefined)',
    category: 'classification',
    type: 'string'
  },
  {
    key: 'thick clothes',
    label: 'thick clothes (Thickness)',
    description: 'Material thickness level (01 not thick, 02 middle thick, 03 thick, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'wet',
    label: 'wet (Moisture)',
    description: 'Sweat wetness and drying condition (dry, little wet, wet, undefined)',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'wet reason',
    label: 'wet reason',
    description: 'Cause of dampness, airflow retention, or machine drying behavior',
    category: 'comfort',
    type: 'string'
  },
  {
    key: 'beautiful',
    label: 'beautiful',
    description: 'Aesthetic evaluation & silhouette flattering score (Beautiful, Middle beautiful, not so beautiful, undefined)',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'beatiful reason',
    label: 'beatiful reason',
    description: 'Specific cut, color, or design factors making the outfit flattering',
    category: 'fit',
    type: 'string'
  },
  {
    key: 'may throw',
    label: 'may throw (Declutter)',
    description: 'Wardrobe audit flag (may throw, no, may throw due to waist, undefined)',
    category: 'classification',
    type: 'string'
  },
  {
    key: 'Name',
    label: 'Name',
    description: 'Garment primary name or title in wardrobe',
    category: 'classification',
    type: 'string'
  },
  {
    key: 'Descriptions',
    label: 'Descriptions',
    description: 'Primary wear & fit observation notes',
    category: 'classification',
    type: 'string'
  },
  {
    key: 'Descriptions 2',
    label: 'Descriptions 2',
    description: 'Secondary ergonomic & maintenance notes',
    category: 'classification',
    type: 'string'
  },
  {
    key: 'last comment or action',
    label: 'last comment or action',
    description: 'Latest user recommendation or wearing verdict',
    category: 'classification',
    type: 'string'
  },

  // Washing & Care
  {
    key: 'wash stated',
    label: 'wash stated',
    description: 'Current laundry state (Clean vs Need Wash)',
    category: 'care',
    type: 'string'
  },
  {
    key: 'Total Washed Count',
    label: 'Total Washed Count',
    description: 'Total cumulative wash cycles (Legacy count + App tracked washes)',
    category: 'care',
    type: 'number'
  },
  {
    key: 'Tracked App Washes',
    label: 'Tracked App Washes',
    description: 'Count of washes recorded within the app',
    category: 'care',
    type: 'number'
  },
  {
    key: 'wearing frequency(how many times legacy washed)',
    label: 'wearing frequency(legacy)',
    description: 'Historical wash/wear frequency recorded prior to app',
    category: 'care',
    type: 'number'
  },
  {
    key: 'price',
    label: 'price',
    description: 'Garment purchase price or financial cost',
    category: 'economics',
    type: 'number'
  },
  {
    key: 'Last Washed',
    label: 'Last Washed',
    description: 'Date timestamp of latest laundry cycle',
    category: 'care',
    type: 'date'
  },
  {
    key: 'created',
    label: 'created',
    description: 'Garment catalog entry creation date',
    category: 'economics',
    type: 'date'
  },
];

export const DEFAULT_REPORT_PRESETS: ReportPreset[] = [
  {
    id: 'preset-aircond-hot',
    name: 'Aircond Suitable: Upper Body (not cold / hot)',
    description: 'L1: Clothes Type, L2: wet, L3: aircond hot feeling, L4: aircond leg feeling, L5: beautiful, L6: beatiful reason',
    groupTiers: [
      'Clothes Type',
      'wet',
      'when with aircond: hot feeling',
      'when with aircond: leg feeling',
      'beautiful',
      'beatiful reason'
    ],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'when with aircond: hot feeling',
      'when with aircond: leg feeling',
      'material',
      'thick clothes'
    ]
  },
  {
    id: 'preset-aircond-leg',
    name: 'Aircond Suitable: Leg Feeling (not cold leg)',
    description: 'Tier 1: when with aircond: leg feeling (not cold, not so cold, hot, undefined). Tier 2: Clothes Type.',
    groupTiers: ['when with aircond: leg feeling', 'Clothes Type'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'when with aircond: leg feeling',
      'when with aircond: hot feeling',
      'material',
      'long dress'
    ]
  },
  {
    id: 'preset-without-aircond-hot',
    name: 'Normal / No Aircond: Upper Body (hot feeling)',
    description: 'Tier 1: when without aircond: hot feeling (not cold, hot, undefined). Tier 2: Clothes Type (dress/bra).',
    groupTiers: ['when without aircond: hot feeling', 'Clothes Type'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'when without aircond: hot feeling',
      'when without aircond: leg feeling',
      'material',
      'thick clothes'
    ]
  },
  {
    id: 'preset-without-aircond-leg',
    name: 'Normal / No Aircond: Leg Feeling (not cold leg)',
    description: 'Tier 1: when without aircond: leg feeling (not cold, not so cold, hot, undefined). Tier 2: Clothes Type.',
    groupTiers: ['when without aircond: leg feeling', 'Clothes Type'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'when without aircond: leg feeling',
      'when without aircond: hot feeling',
      'material',
      'thick clothes'
    ]
  },
  {
    id: 'preset-sleep-fan-hot',
    name: 'Sleep Under Fan: Upper Body (hot feeling)',
    description: 'Tier 1: when sleep under fan: hot feeling (not cold, hot, undefined). Tier 2: Clothes Type.',
    groupTiers: ['when sleep under fan: hot feeling', 'Clothes Type'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'when sleep under fan: hot feeling',
      'when sleep under fan: leg feeling',
      'material',
      'long dress'
    ]
  },
  {
    id: 'preset-sleep-fan-leg',
    name: 'Sleep Under Fan: Leg Feeling (not cold leg)',
    description: 'Tier 1: when sleep under fan: leg feeling (not cold, not so cold, hot, undefined). Tier 2: Clothes Type.',
    groupTiers: ['when sleep under fan: leg feeling', 'Clothes Type'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'when sleep under fan: leg feeling',
      'when sleep under fan: hot feeling',
      'material',
      'long dress'
    ]
  },
  {
    id: 'preset-wet-performance',
    name: 'Clothes Wet / Dry Performance',
    description: 'Tier 1: Clothes Type (dress/bra). Tier 2: wet (dry, little wet, wet, undefined).',
    groupTiers: ['Clothes Type', 'wet'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'wet',
      'wet reason',
      'material',
      'Hot at noon'
    ]
  },
  {
    id: 'preset-breast-gather-bra',
    name: 'Breast Fit: With 聚拢bra',
    description: 'Tier 1: Clothes Type (dress/bra). Tier 2: sit breast ok: With 聚拢bra.',
    groupTiers: ['Clothes Type', 'sit breast ok: With 聚拢bra'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'sit breast ok: With 聚拢bra',
      'side breast ok: with 聚拢 bra',
      'beautiful',
      'diao dai'
    ]
  },
  {
    id: 'preset-thickness-climate',
    name: 'Thickness & Climate',
    description: 'Hierarchical drill by clothing category, noon thermal feel, and fabric thickness.',
    groupTiers: ['Clothes Type', 'Hot at noon', 'thick clothes'],
    sortField: 'wearing frequency(how many times legacy washed)',
    sortOrder: 'desc',
    visibleFields: [
      'Clothes Type',
      'material',
      'thick clothes',
      'Hot at noon',
      'beautiful',
      'wearing frequency(how many times legacy washed)',
      'wash stated'
    ]
  },
  {
    id: 'preset-aesthetic-fit',
    name: 'Aesthetic & Fit Ratings',
    description: 'Drill down by category, aesthetic score, and strap/chest silhouette.',
    groupTiers: ['Clothes Type', 'beautiful', 'diao dai'],
    sortField: 'Name',
    sortOrder: 'asc',
    visibleFields: [
      'Clothes Type',
      'beautiful',
      'diao dai',
      'side breast ok',
      'material',
      'price'
    ]
  },
  {
    id: 'preset-wash-care',
    name: 'Wash Care & Laundry Priority',
    description: 'Exclusive drill by wash backlog state, clothing category, and fabric material.',
    groupTiers: ['wash stated', 'Clothes Type', 'material'],
    sortField: 'wearing frequency(how many times legacy washed)',
    sortOrder: 'desc',
    visibleFields: [
      'wash stated',
      'Clothes Type',
      'material',
      'Tracked App Washes',
      'Total Washed Count',
      'Last Washed'
    ]
  },
  {
    id: 'preset-sensory-heat',
    name: 'Sensory Heat & Moisture',
    description: 'Drill by midday heat sensitivity, moisture performance, and thickness.',
    groupTiers: ['Hot at noon', 'wet', 'thick clothes'],
    sortField: 'wearing frequency(how many times legacy washed)',
    sortOrder: 'desc',
    visibleFields: [
      'Clothes Type',
      'Hot at noon',
      'wet',
      'thick clothes',
      'material',
      'beautiful'
    ]
  },
  {
    id: 'preset-declutter-audit',
    name: 'Declutter & Discard Audit',
    description: 'Audit garments flagged for possible decluttering or disposal by category.',
    groupTiers: ['may throw', 'Clothes Type', 'beautiful'],
    sortField: 'wearing frequency(how many times legacy washed)',
    sortOrder: 'asc',
    visibleFields: [
      'may throw',
      'Clothes Type',
      'material',
      'beautiful',
      'price',
      'wearing frequency(how many times legacy washed)'
    ]
  }
];

const PRESETS_STORAGE_KEY = 'wardrobe_drilldown_report_presets_v7';
const ACTIVE_PRESET_ID_KEY = 'wardrobe_drilldown_active_preset_id_v7';

/**
 * Normalizes and deduplicates a list of presets.
 * - Deduplicates by ID AND normalized name (case-insensitive, trimmed).
 * - Deduplicates array fields: groupTiers and visibleFields (no repeated 'diao dai' or 'beautiful').
 * - Preserves built-in presets order and ensures all default presets exist.
 */
export function deduplicatePresets(presets: ReportPreset[]): ReportPreset[] {
  if (!Array.isArray(presets) || presets.length === 0) {
    return [...DEFAULT_REPORT_PRESETS];
  }

  const defaultMap = new Map<string, ReportPreset>(DEFAULT_REPORT_PRESETS.map((dp) => [dp.id, dp]));
  const defaultNameMap = new Map<string, ReportPreset>(
    DEFAULT_REPORT_PRESETS.map((dp) => [dp.name.toLowerCase().trim(), dp])
  );

  const seenIds = new Set<string>();
  const seenNames = new Set<string>();
  const cleanList: ReportPreset[] = [];

  const sanitizePreset = (p: ReportPreset): ReportPreset => {
    return {
      ...p,
      name: (p.name || '').trim(),
      groupTiers: Array.from(new Set((p.groupTiers || []).map((t) => (t || '').trim()).filter(Boolean))),
      visibleFields: Array.from(new Set((p.visibleFields || []).map((f) => (f || '').trim()).filter(Boolean)))
    };
  };

  // 1. Process all default presets first, taking user-edited customizations if matching
  DEFAULT_REPORT_PRESETS.forEach((dp) => {
    const normName = dp.name.toLowerCase().trim();
    const userMatch = presets.find(
      (p) => p && (p.id === dp.id || (p.name && p.name.toLowerCase().trim() === normName))
    );
    const chosen = sanitizePreset(userMatch ? { ...dp, ...userMatch, id: dp.id } : dp);
    cleanList.push(chosen);
    seenIds.add(chosen.id);
    seenNames.add(chosen.name.toLowerCase().trim());
  });

  // 2. Add custom presets, avoiding duplicate IDs and duplicate names
  presets.forEach((p) => {
    if (!p || !p.id || !p.name) return;
    const normId = p.id;
    const normName = p.name.toLowerCase().trim();

    if (seenIds.has(normId) || seenNames.has(normName) || defaultMap.has(normId) || defaultNameMap.has(normName)) {
      // If it's a match to an already processed item, update properties if custom
      const existingIdx = cleanList.findIndex(
        (e) => e.id === normId || e.name.toLowerCase().trim() === normName
      );
      if (existingIdx !== -1 && (p.isCustom || cleanList[existingIdx].isCustom)) {
        cleanList[existingIdx] = sanitizePreset({ ...cleanList[existingIdx], ...p });
      }
      return;
    }

    const sanitized = sanitizePreset(p);
    seenIds.add(normId);
    seenNames.add(normName);
    cleanList.push(sanitized);
  });

  return cleanList;
}

class ReportPresetService {
  private presets: ReportPreset[] = [];
  private listeners: ((presets: ReportPreset[]) => void)[] = [];

  constructor() {
    this.init();
  }

  private init() {
    try {
      if (typeof localStorage !== 'undefined') {
        let stored = localStorage.getItem(PRESETS_STORAGE_KEY);
        // Fallback / migrate from v5 or v4
        if (!stored) {
          stored = localStorage.getItem('wardrobe_drilldown_report_presets_v5') ||
                   localStorage.getItem('wardrobe_drilldown_report_presets_v4');
        }
        if (stored) {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed) && parsed.length > 0) {
            const clean = deduplicatePresets(parsed);
            this.presets = clean;
            this.savePresetsLocal(clean);
          }
        }
      }
    } catch {
      // Fallback
    }
    if (this.presets.length === 0) {
      this.presets = [...DEFAULT_REPORT_PRESETS];
    }
    // Always asynchronously synchronize with server disk presets (e.g. from restored backup or container disk)
    this.fetchServerPresets();
  }

  /**
   * Fetches persistent presets from server disk (/api/drilldown-presets)
   * and merges them safely with local presets.
   */
  public async fetchServerPresets(): Promise<ReportPreset[]> {
    try {
      if (typeof window !== 'undefined') {
        const res = await fetch('/api/drilldown-presets');
        if (res.ok) {
          const serverPresets = await res.json();
          if (Array.isArray(serverPresets) && serverPresets.length > 0) {
            return this.importPresets(serverPresets, false, false);
          } else if (this.presets.length > 0) {
            // First time seeding server disk
            this.syncToServer(this.presets);
          }
        }
      }
    } catch (err) {
      console.warn('[ReportPresetService] Failed to load presets from server:', err);
    }
    return this.presets;
  }

  /**
   * Syncs current presets to the server filesystem so backups and cross-device sessions stay intact.
   */
  public async syncToServer(presets: ReportPreset[]): Promise<void> {
    try {
      if (typeof window !== 'undefined') {
        await fetch('/api/drilldown-presets', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(deduplicatePresets(presets))
        });
      }
    } catch (err) {
      console.warn('[ReportPresetService] Failed to sync presets to server:', err);
    }
  }

  public subscribe(fn: (presets: ReportPreset[]) => void): () => void {
    this.listeners.push(fn);
    return () => {
      this.listeners = this.listeners.filter(l => l !== fn);
    };
  }

  private notify() {
    this.listeners.forEach(fn => fn(this.presets));
  }

  public getPresets(): ReportPreset[] {
    if (this.presets.length === 0) {
      return [...DEFAULT_REPORT_PRESETS];
    }
    return this.presets;
  }

  private savePresetsLocal(presets: ReportPreset[]): void {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(PRESETS_STORAGE_KEY, JSON.stringify(presets));
      }
    } catch (e) {
      console.warn('[ReportPresetService] LocalStorage write error:', e);
    }
  }

  /**
   * Safely imports and merges incoming presets from a backup file or server database.
   * Matches by ID and normalized name, updates if newer/restoring, and eliminates all duplicates.
   * When isRestore is true, always applies the preset configurations from the backup archive.
   */
  public importPresets(incoming: ReportPreset[], syncServer = true, isRestore = false): ReportPreset[] {
    if (!Array.isArray(incoming) || incoming.length === 0) {
      return this.presets;
    }

    // Combine current and incoming presets, giving precedence to incoming when restoring
    const combined = isRestore
      ? [...incoming, ...this.presets]
      : [...this.presets, ...incoming];

    const deduplicated = deduplicatePresets(combined);

    this.presets = deduplicated;
    this.savePresetsLocal(deduplicated);
    this.notify();

    if (syncServer) {
      this.syncToServer(deduplicated);
    }
    return deduplicated;
  }

  public exportPresetsJson(): string {
    return JSON.stringify(this.getPresets(), null, 2);
  }

  public savePreset(preset: ReportPreset): ReportPreset[] {
    const presetWithMeta: ReportPreset = {
      ...preset,
      updatedAt: new Date().toISOString()
    };

    const index = this.presets.findIndex((p) => p.id === preset.id);
    let updated: ReportPreset[];
    if (index >= 0) {
      updated = [...this.presets];
      updated[index] = presetWithMeta;
    } else {
      updated = [...this.presets, presetWithMeta];
    }

    this.presets = updated;
    this.savePresetsLocal(updated);
    this.syncToServer(updated);
    this.notify();

    // Persist to local database file so full backup archive immediately contains this updated preset
    try {
      dataService.saveToLocalDisk().catch(() => {});
    } catch {}

    return updated;
  }

  public deletePreset(id: string): ReportPreset[] {
    const updated = this.presets.filter((p) => p.id !== id);
    const result = updated.length > 0 ? updated : [...DEFAULT_REPORT_PRESETS];

    this.presets = result;
    this.savePresetsLocal(result);
    this.syncToServer(result);
    this.notify();

    try {
      dataService.saveToLocalDisk().catch(() => {});
    } catch {}

    return result;
  }

  public resetToDefaults(): ReportPreset[] {
    this.presets = [...DEFAULT_REPORT_PRESETS];
    this.savePresetsLocal(this.presets);
    this.syncToServer(this.presets);
    this.notify();

    try {
      dataService.saveToLocalDisk().catch(() => {});
    } catch {}

    return this.presets;
  }

  /**
   * Returns all available fields for grouping tiers and visible columns,
   * dynamically merging built-in fields with custom garment/report fields.
   */
  public getAllAvailableFields(): ReportFieldMeta[] {
    const list = [...AVAILABLE_REPORT_FIELDS];
    const existingKeys = new Set(list.map((f) => f.key.trim().toLowerCase()));

    try {
      const clothes = dataService.getClothes();
      clothes.forEach((c) => {
        const customs = { ...(c.customFields || {}), ...(c.extraAttributes || {}) };
        Object.keys(customs).forEach((k) => {
          const trimmed = k ? k.trim() : '';
          if (trimmed && !existingKeys.has(trimmed.toLowerCase())) {
            existingKeys.add(trimmed.toLowerCase());
            list.push({
              key: trimmed,
              label: trimmed,
              description: `Custom Garment Field (${trimmed})`,
              category: 'classification',
              type: 'string'
            });
          }
        });
      });

      const reports = dataService.getReports();
      reports.forEach((r) => {
        const customs = { ...(r.customFields || {}), ...(r.extraAttributes || {}) };
        Object.keys(customs).forEach((k) => {
          const trimmed = k ? k.trim() : '';
          if (trimmed && !existingKeys.has(trimmed.toLowerCase())) {
            existingKeys.add(trimmed.toLowerCase());
            list.push({
              key: trimmed,
              label: trimmed,
              description: `Custom Wear Report Field (${trimmed})`,
              category: 'comfort',
              type: 'string'
            });
          }
        });
      });
    } catch {}

    return list;
  }

  public getActivePresetId(): string {
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem(ACTIVE_PRESET_ID_KEY) || (this.presets[0]?.id || DEFAULT_REPORT_PRESETS[0].id);
    }
    return this.presets[0]?.id || DEFAULT_REPORT_PRESETS[0].id;
  }

  public setActivePresetId(id: string): void {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(ACTIVE_PRESET_ID_KEY, id);
      }
    } catch {}
  }

  /**
   * Resolves value of any field for grouping, sorting, or display.
   */
  public getFieldValue(item: ClothesItem, fieldKey: string): string {
    if (!item) return 'undefined';

    if (fieldKey === 'wash stated') {
      const stats = dataService.getGarmentStats(item.id);
      return stats.needWash ? 'Need Wash' : 'Clean';
    }

    if (fieldKey === 'Total Washed Count') {
      const legacy = typeof item['wearing frequency(how many times legacy washed)'] === 'number'
        ? item['wearing frequency(how many times legacy washed)']
        : 0;
      const tracked = typeof item['Tracked App Washes'] === 'number' ? item['Tracked App Washes'] : 0;
      return String(legacy + tracked);
    }

    let val = (item as any)[fieldKey];
    if (val === undefined || val === null || val === '') {
      // Fallback check for normalized / camelCase keys or custom attributes
      if (item.customFields && item.customFields[fieldKey] !== undefined) {
        val = item.customFields[fieldKey];
      } else if (item.extraAttributes && item.extraAttributes[fieldKey] !== undefined) {
        val = item.extraAttributes[fieldKey];
      } else if (fieldKey === 'Clothes Type' && item.clothesType) {
        val = item.clothesType;
      } else if (fieldKey === 'Name' && item.name) {
        val = item.name;
      } else if (fieldKey === 'material' && item.materialProp) {
        val = item.materialProp;
      } else if (fieldKey === 'sit breast ok: With 聚拢bra' && (item as any).sitBreastOkWithGatherBra) {
        val = (item as any).sitBreastOkWithGatherBra;
      } else if (fieldKey === 'sit breast ok: with side out bra' && (item as any).sitBreastOkWithSideOutBra) {
        val = (item as any).sitBreastOkWithSideOutBra;
      } else if (fieldKey === 'side breast ok: with 聚拢 bra' && (item as any).sideBreastOkWithGatherBra) {
        val = (item as any).sideBreastOkWithGatherBra;
      } else if (fieldKey === 'side breast ok: with side out bra' && (item as any).sideBreastOkWithSideOutBra) {
        val = (item as any).sideBreastOkWithSideOutBra;
      }
    }

    if (val === undefined || val === null || val === '' || String(val).trim() === '') {
      return 'undefined';
    }

    if (Array.isArray(val)) {
      return val.join(', ') || 'undefined';
    }

    return String(val).trim();
  }

  /**
   * Resolves numerical value for sorting.
   */
  public getNumericalSortValue(item: ClothesItem, fieldKey: string): number {
    if (fieldKey === 'wash stated') {
      const stats = dataService.getGarmentStats(item.id);
      return stats.needWash ? 1 : 0;
    }

    if (fieldKey === 'Total Washed Count') {
      const legacy = typeof item['wearing frequency(how many times legacy washed)'] === 'number'
        ? item['wearing frequency(how many times legacy washed)']
        : 0;
      const tracked = typeof item['Tracked App Washes'] === 'number' ? item['Tracked App Washes'] : 0;
      return legacy + tracked;
    }

    const val = (item as any)[fieldKey];
    if (typeof val === 'number') return val;
    if (typeof val === 'string') {
      const parsed = parseFloat(val);
      if (!isNaN(parsed)) return parsed;
      const dateVal = Date.parse(val);
      if (!isNaN(dateVal)) return dateVal;
    }
    return 0;
  }
}

export const reportPresetService = new ReportPresetService();
