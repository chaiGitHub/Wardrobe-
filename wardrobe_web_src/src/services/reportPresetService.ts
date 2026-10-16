import { ClothesItem, ReportPreset } from '../types';
import { dataService } from './dataService';

export interface ReportFieldMeta {
  key: string;
  label: string;
  category: 'classification' | 'comfort' | 'fit' | 'care' | 'economics';
  type: 'string' | 'number' | 'date';
}

export const AVAILABLE_REPORT_FIELDS: ReportFieldMeta[] = [
  { key: 'Clothes Type', label: 'Category / Type', category: 'classification', type: 'string' },
  { key: 'material', label: 'Material', category: 'classification', type: 'string' },
  { key: 'thick clothes', label: 'Thickness', category: 'comfort', type: 'string' },
  { key: 'Hot at noon', label: 'Noon Heat Feel', category: 'comfort', type: 'string' },
  { key: 'beautiful', label: 'Aesthetic Score', category: 'fit', type: 'string' },
  { key: 'diao dai', label: 'Strap Design', category: 'fit', type: 'string' },
  { key: 'side breast ok', label: 'Side Fit', category: 'fit', type: 'string' },
  { key: 'sit breast ok', label: 'Seated Bust Fit', category: 'fit', type: 'string' },
  { key: 'center chest', label: 'Center Chest Cut', category: 'fit', type: 'string' },
  { key: 'wet', label: 'Moisture / Dryness', category: 'comfort', type: 'string' },
  { key: 'long dress', label: 'Length Cut', category: 'classification', type: 'string' },
  { key: 'may throw', label: 'Retention / Discard', category: 'classification', type: 'string' },
  { key: 'wash stated', label: 'Laundry State', category: 'care', type: 'string' },
  { key: 'wearing frequency(how many times legacy washed)', label: 'Wear Frequency', category: 'care', type: 'number' },
  { key: 'Total Washed Count', label: 'Total Washed Count', category: 'care', type: 'number' },
  { key: 'Tracked App Washes', label: 'Tracked Washes', category: 'care', type: 'number' },
  { key: 'price', label: 'Purchase Price ($)', category: 'economics', type: 'number' },
  { key: 'Last Washed', label: 'Last Washed Date', category: 'care', type: 'date' },
  { key: 'created', label: 'Date Added', category: 'economics', type: 'date' },
  { key: 'Name', label: 'Garment Name', category: 'classification', type: 'string' },
];

export const DEFAULT_REPORT_PRESETS: ReportPreset[] = [
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
    id: 'preset-wardrobe-audit',
    name: 'Wardrobe Audit & Retention',
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

const PRESETS_STORAGE_KEY = 'wardrobe_drilldown_report_presets_v1';
const ACTIVE_PRESET_ID_KEY = 'wardrobe_drilldown_active_preset_id_v1';

export const reportPresetService = {
  getPresets(): ReportPreset[] {
    try {
      const stored = localStorage.getItem(PRESETS_STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      }
    } catch {
      // ignore
    }
    return DEFAULT_REPORT_PRESETS;
  },

  savePresets(presets: ReportPreset[]): void {
    try {
      localStorage.setItem(PRESETS_STORAGE_KEY, JSON.stringify(presets));
    } catch {
      // ignore
    }
  },

  savePreset(preset: ReportPreset): ReportPreset[] {
    const presets = this.getPresets();
    const index = presets.findIndex((p) => p.id === preset.id);
    let updated: ReportPreset[];
    if (index >= 0) {
      updated = [...presets];
      updated[index] = preset;
    } else {
      updated = [...presets, preset];
    }
    this.savePresets(updated);
    return updated;
  },

  deletePreset(id: string): ReportPreset[] {
    const presets = this.getPresets().filter((p) => p.id !== id);
    // Ensure at least defaults remain
    const result = presets.length > 0 ? presets : DEFAULT_REPORT_PRESETS;
    this.savePresets(result);
    return result;
  },

  resetToDefaults(): ReportPreset[] {
    this.savePresets(DEFAULT_REPORT_PRESETS);
    return DEFAULT_REPORT_PRESETS;
  },

  getActivePresetId(): string {
    return localStorage.getItem(ACTIVE_PRESET_ID_KEY) || DEFAULT_REPORT_PRESETS[0].id;
  },

  setActivePresetId(id: string): void {
    localStorage.setItem(ACTIVE_PRESET_ID_KEY, id);
  },

  /**
   * Resolves value of any field for grouping, sorting, or display.
   */
  getFieldValue(item: ClothesItem, fieldKey: string): string {
    if (fieldKey === 'wash stated') {
      const stats = dataService.getGarmentStats(item.id);
      return stats.needWash ? 'Need Wash' : 'Clean';
    }

    if (fieldKey === 'Total Washed Count') {
      const stats = dataService.getGarmentStats(item.id);
      return String(stats.totalWears || item['Total Washed Count'] || 0);
    }

    const val = (item as any)[fieldKey];
    if (val === undefined || val === null || val === '') {
      return '(Not specified)';
    }

    if (Array.isArray(val)) {
      return val.join(', ') || '(None)';
    }

    return String(val).trim();
  },

  /**
   * Resolves numerical value for sorting.
   */
  getNumericalSortValue(item: ClothesItem, fieldKey: string): number {
    if (fieldKey === 'wash stated') {
      const stats = dataService.getGarmentStats(item.id);
      return stats.needWash ? 1 : 0;
    }

    if (fieldKey === 'Total Washed Count') {
      const stats = dataService.getGarmentStats(item.id);
      return stats.totalWears || item['Total Washed Count'] || 0;
    }

    const val = (item as any)[fieldKey];
    if (typeof val === 'number') return val;
    if (typeof val === 'string') {
      const parsed = parseFloat(val);
      if (!isNaN(parsed)) return parsed;
      // If it's a date string
      const dateVal = Date.parse(val);
      if (!isNaN(dateVal)) return dateVal;
    }
    return 0;
  }
};
