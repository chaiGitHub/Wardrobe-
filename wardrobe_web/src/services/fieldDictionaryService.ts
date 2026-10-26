export interface FieldValueDefinition {
  valueEn: string;
  valueCn: string;
  meaningEn: string;
  meaningCn: string;
}

export interface FieldDefinitionItem {
  fieldNameEn: string;
  fieldNameCn: string;
  category: 'clothes' | 'wearingReport' | 'junction';
  descriptionEn: string;
  descriptionCn: string;
  typicalValues: FieldValueDefinition[];
  enabledForAi?: boolean; // When true or undefined, included in AI analysis. When false, excluded.
}

export interface AIAdvisorReportRecord {
  id: string;
  createdAt: string;
  model: string;
  temperature: string;
  occasion: string;
  physicalPriority: string;
  selectedGarmentIds: string[];
  selectedGarmentNames: string[];
  inquiryPrompt: string;
  advice: string;
  title?: string;
  userNotes?: string;
}

import rawDefaultFieldDefinitions from '../fieldDefinitions.json';

export const DEFAULT_FIELD_DEFINITIONS: FieldDefinitionItem[] = rawDefaultFieldDefinitions as FieldDefinitionItem[];

const STORAGE_KEY_FIELD_DEFS = 'wardrobe_field_definitions_v5';
const STORAGE_KEY_AI_REPORTS = 'wardrobe_ai_advisor_reports_v1';

class FieldDictionaryManager {
  private definitions: FieldDefinitionItem[] = [];
  private aiReports: AIAdvisorReportRecord[] = [];
  private listeners: (() => void)[] = [];
  private initialized: boolean = false;

  constructor() {
    this.load();
    this.syncWithServer().catch(() => {});
  }

  private load(): void {
    if (typeof localStorage === 'undefined') {
      this.definitions = [...DEFAULT_FIELD_DEFINITIONS];
      this.aiReports = [];
      return;
    }
    try {
      const savedDefs = localStorage.getItem(STORAGE_KEY_FIELD_DEFS);
      if (savedDefs) {
        const parsed = JSON.parse(savedDefs);
        if (Array.isArray(parsed) && parsed.length > 0) {
          const existingMap = new Map<string, FieldDefinitionItem>(parsed.map((p: FieldDefinitionItem) => [p.fieldNameEn.toLowerCase(), p]));
          let didAdd = false;
          for (const d of DEFAULT_FIELD_DEFINITIONS) {
            const key = d.fieldNameEn.toLowerCase();
            const existing = existingMap.get(key);
            if (!existing) {
              parsed.push(JSON.parse(JSON.stringify(d)));
              didAdd = true;
            } else {
              // Ensure all typical values from defaults are present
              const tvMap = new Map((existing.typicalValues || []).map(v => [v.valueEn.trim().toLowerCase(), v]));
              let tvAdded = false;
              (d.typicalValues || []).forEach(v => {
                const tvKey = v.valueEn.trim().toLowerCase();
                if (!tvMap.has(tvKey)) {
                  tvMap.set(tvKey, v);
                  tvAdded = true;
                }
              });
              if (tvAdded) {
                existing.typicalValues = Array.from(tvMap.values());
                didAdd = true;
              }
              if (!existing.category && d.category) {
                existing.category = d.category;
                didAdd = true;
              }
              if (!existing.descriptionCn && d.descriptionCn) {
                existing.descriptionCn = d.descriptionCn;
                didAdd = true;
              }
              if (!existing.fieldNameCn && d.fieldNameCn) {
                existing.fieldNameCn = d.fieldNameCn;
                didAdd = true;
              }
            }
          }
          this.definitions = parsed;
          if (didAdd) {
            try {
              localStorage.setItem(STORAGE_KEY_FIELD_DEFS, JSON.stringify(parsed));
            } catch {}
          }
        } else {
          this.definitions = [...DEFAULT_FIELD_DEFINITIONS];
          localStorage.setItem(STORAGE_KEY_FIELD_DEFS, JSON.stringify(this.definitions));
        }
      } else {
        this.definitions = [...DEFAULT_FIELD_DEFINITIONS];
        localStorage.setItem(STORAGE_KEY_FIELD_DEFS, JSON.stringify(this.definitions));
      }
    } catch {
      this.definitions = [...DEFAULT_FIELD_DEFINITIONS];
    }

    try {
      const savedReports = localStorage.getItem(STORAGE_KEY_AI_REPORTS);
      if (savedReports) {
        const parsedRep = JSON.parse(savedReports);
        this.aiReports = Array.isArray(parsedRep) ? parsedRep : [];
      } else {
        this.aiReports = [];
      }
    } catch {
      this.aiReports = [];
    }
  }

  public async syncWithServer(): Promise<void> {
    try {
      const res = await fetch('/api/field-definitions');
      if (res.ok) {
        const serverDefs = await res.json();
        const map = new Map<string, FieldDefinitionItem>();
        
        // 1. Seed with comprehensive built-in defaults (42 items with all historical values)
        for (const def of DEFAULT_FIELD_DEFINITIONS) {
          map.set(def.fieldNameEn.toLowerCase(), JSON.parse(JSON.stringify(def)));
        }
        
        // 2. Overlay server definitions, preserving and merging typicalValues
        if (Array.isArray(serverDefs)) {
          for (const s of serverDefs) {
            if (s && s.fieldNameEn) {
              const key = s.fieldNameEn.toLowerCase();
              const existing = map.get(key);
              if (existing) {
                const mergedTvMap = new Map((existing.typicalValues || []).map(v => [v.valueEn.trim().toLowerCase(), v]));
                (s.typicalValues || []).forEach((v: FieldValueDefinition) => {
                  if (v && v.valueEn) mergedTvMap.set(v.valueEn.trim().toLowerCase(), v);
                });
                map.set(key, { ...existing, ...s, typicalValues: Array.from(mergedTvMap.values()) });
              } else {
                map.set(key, s);
              }
            }
          }
        }

        // 3. Overlay local definitions, preserving and merging typicalValues
        for (const l of this.definitions) {
          if (l && l.fieldNameEn) {
            const key = l.fieldNameEn.toLowerCase();
            const existing = map.get(key);
            if (existing) {
              const mergedTvMap = new Map((existing.typicalValues || []).map(v => [v.valueEn.trim().toLowerCase(), v]));
              (l.typicalValues || []).forEach(v => {
                if (v && v.valueEn) mergedTvMap.set(v.valueEn.trim().toLowerCase(), v);
              });
              map.set(key, { ...existing, ...l, typicalValues: Array.from(mergedTvMap.values()) });
            } else {
              map.set(key, l);
            }
          }
        }

        const merged = Array.from(map.values());
        const mergedJson = JSON.stringify(merged);
        const currentJson = JSON.stringify(this.definitions);

        if (mergedJson !== currentJson || (Array.isArray(serverDefs) && JSON.stringify(serverDefs) !== mergedJson)) {
          this.definitions = merged;
          if (typeof localStorage !== 'undefined') {
            try {
              localStorage.setItem(STORAGE_KEY_FIELD_DEFS, mergedJson);
            } catch {}
          }
          this.notify();

          // Push merged definition back to server to keep it synced
          await fetch('/api/field-definitions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: mergedJson
          }).catch(() => {});
        }
      }
    } catch (e) {
      console.warn('[FieldDictionary] Server defs sync notice:', e);
    }

    try {
      const res = await fetch('/api/ai-advisor-reports');
      if (res.ok) {
        const serverReports = await res.json();
        if (Array.isArray(serverReports) && serverReports.length > 0) {
          if (this.aiReports.length === 0 || serverReports.length > this.aiReports.length) {
            this.aiReports = serverReports;
            if (typeof localStorage !== 'undefined') {
              localStorage.setItem(STORAGE_KEY_AI_REPORTS, JSON.stringify(serverReports));
            }
            this.notify();
          } else if (this.aiReports.length > serverReports.length) {
            await fetch('/api/ai-advisor-reports', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(this.aiReports)
            });
          }
        }
      }
    } catch (e) {
      console.warn('[FieldDictionary] Server reports sync notice:', e);
    }
  }

  public getDefinitions(): FieldDefinitionItem[] {
    return [...this.definitions];
  }

  public getCategoryDefinitions(category: 'clothes' | 'wearingReport' | 'junction'): FieldDefinitionItem[] {
    return this.definitions.filter(d => d.category === category);
  }

  public saveDefinition(item: FieldDefinitionItem): void {
    const idx = this.definitions.findIndex(d => d.fieldNameEn.toLowerCase() === item.fieldNameEn.toLowerCase());
    if (idx >= 0) {
      this.definitions[idx] = { ...item };
    } else {
      this.definitions.push(item);
    }
    this.persistDefs();
    this.notify();
  }

  public removeDefinition(fieldNameEn: string): void {
    this.definitions = this.definitions.filter(d => d.fieldNameEn.toLowerCase() !== fieldNameEn.toLowerCase());
    this.persistDefs();
    this.notify();
  }

  public getFieldDefinition(fieldName: string): FieldDefinitionItem | undefined {
    if (!fieldName) return undefined;
    const clean = fieldName.trim().toLowerCase();
    return this.definitions.find(d => {
      const en = d.fieldNameEn.toLowerCase();
      const cn = (d.fieldNameCn || '').toLowerCase();
      return en === clean || cn === clean || en.replace(/\s+/g, '') === clean || en.replace(/_/g, ' ') === clean;
    });
  }

  public getFieldTypicalValues(fieldName: string): FieldValueDefinition[] {
    const def = this.getFieldDefinition(fieldName);
    return def?.typicalValues || [];
  }

  public addTypicalValue(fieldName: string, valueDef: Partial<FieldValueDefinition> & { valueEn: string }): boolean {
    if (!fieldName || !valueDef?.valueEn?.trim()) return false;
    let def = this.getFieldDefinition(fieldName);
    if (!def) {
      def = {
        fieldNameEn: fieldName.trim(),
        fieldNameCn: fieldName.trim(),
        category: 'clothes',
        descriptionEn: `Attribute ${fieldName}`,
        descriptionCn: `属性 ${fieldName}`,
        typicalValues: []
      };
      this.definitions.push(def);
    }
    const tvs = def.typicalValues ? [...def.typicalValues] : [];
    const valKey = valueDef.valueEn.trim().toLowerCase();
    const existingIdx = tvs.findIndex(v => v.valueEn.trim().toLowerCase() === valKey);
    const itemToAdd: FieldValueDefinition = {
      valueEn: valueDef.valueEn.trim(),
      valueCn: valueDef.valueCn?.trim() || valueDef.valueEn.trim(),
      meaningEn: valueDef.meaningEn?.trim() || '',
      meaningCn: valueDef.meaningCn?.trim() || ''
    };
    if (existingIdx >= 0) {
      tvs[existingIdx] = { ...tvs[existingIdx], ...itemToAdd };
    } else {
      tvs.push(itemToAdd);
    }
    def.typicalValues = tvs;
    this.saveDefinition(def);
    return true;
  }

  /**
   * Appends or updates an ergonomic explain note directly into a field's dictionary definition.
   */
  public appendErgonomicNote(fieldName: string, note: string, valueEn?: string): boolean {
    if (!fieldName || !note || !note.trim()) return false;
    const clean = fieldName.trim().toLowerCase();
    const item = this.definitions.find(d => {
      const en = d.fieldNameEn.toLowerCase();
      return en === clean || en.replace(/\s+/g, '') === clean || en.replace(/_/g, ' ') === clean;
    });
    if (!item) return false;

    const trimmedNote = note.trim();
    if (valueEn && valueEn.trim()) {
      const targetVal = valueEn.trim().toLowerCase();
      const tv = item.typicalValues?.find(v => v.valueEn.toLowerCase() === targetVal || v.valueCn.toLowerCase() === targetVal);
      if (tv) {
        tv.meaningCn = tv.meaningCn ? `${tv.meaningCn}\n${trimmedNote}` : trimmedNote;
        this.saveDefinition(item);
        return true;
      }
    }

    item.descriptionCn = item.descriptionCn ? `${item.descriptionCn}\n${trimmedNote}` : trimmedNote;
    this.saveDefinition(item);
    return true;
  }

  public isFieldEnabled(fieldName: string): boolean {
    if (!fieldName) return true;
    const clean = fieldName.trim().toLowerCase();
    const item = this.definitions.find(d => {
      const en = d.fieldNameEn.toLowerCase();
      const cn = d.fieldNameCn.toLowerCase();
      return en === clean || cn === clean || clean === en.replace(/\s+/g, '') || clean === en.replace(/_/g, ' ');
    });
    if (!item) return true; // default include if not in dictionary
    return item.enabledForAi !== false;
  }

  public setFieldEnabled(fieldNameEn: string, enabled: boolean): void {
    const item = this.definitions.find(d => d.fieldNameEn.toLowerCase() === fieldNameEn.toLowerCase());
    if (item) {
      item.enabledForAi = enabled;
      this.persistDefs();
      this.notify();
    }
  }

  public setCategoryAllEnabled(category: 'all' | 'clothes' | 'wearingReport' | 'junction', enabled: boolean): void {
    for (const d of this.definitions) {
      if (category === 'all' || d.category === category) {
        d.enabledForAi = enabled;
      }
    }
    this.persistDefs();
    this.notify();
  }

  public getEnabledCount(): { total: number; enabled: number } {
    const total = this.definitions.length;
    const enabled = this.definitions.filter(d => d.enabledForAi !== false).length;
    return { total, enabled };
  }

  public resetToDefault(): void {
    this.definitions = JSON.parse(JSON.stringify(DEFAULT_FIELD_DEFINITIONS));
    this.persistDefs();
    this.notify();
  }

  public importDefinitions(items: FieldDefinitionItem[]): void {
    if (!Array.isArray(items) || items.length === 0) return;
    // Safe merge: keep all existing definitions (never delete) and overlay imported ones
    const map = new Map<string, FieldDefinitionItem>();
    // 1. Keep all current definitions
    for (const d of this.definitions) {
      if (d && d.fieldNameEn) map.set(d.fieldNameEn.toLowerCase(), { ...d });
    }
    // 2. Ensure all built-in defaults are preserved
    for (const def of DEFAULT_FIELD_DEFINITIONS) {
      if (!map.has(def.fieldNameEn.toLowerCase())) {
        map.set(def.fieldNameEn.toLowerCase(), JSON.parse(JSON.stringify(def)));
      }
    }
    // 3. Merge imported definitions and their typicalValues
    for (const item of items) {
      if (item && item.fieldNameEn) {
        const key = item.fieldNameEn.toLowerCase();
        const existing = map.get(key);
        if (existing) {
          const tvMap = new Map((existing.typicalValues || []).map(v => [v.valueEn.trim().toLowerCase(), v]));
          (item.typicalValues || []).forEach(v => {
            if (v && v.valueEn) tvMap.set(v.valueEn.trim().toLowerCase(), v);
          });
          map.set(key, { ...existing, ...item, typicalValues: Array.from(tvMap.values()) });
        } else {
          map.set(key, item);
        }
      }
    }
    this.definitions = Array.from(map.values());
    this.persistDefs();
    this.notify();
  }

  public importAiReports(reports: AIAdvisorReportRecord[]): void {
    if (!Array.isArray(reports) || reports.length === 0) return;
    this.aiReports = reports;
    this.persistReports();
    this.notify();
  }

  private persistDefs(): void {
    const jsonStr = JSON.stringify(this.definitions);
    if (typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem(STORAGE_KEY_FIELD_DEFS, jsonStr);
      } catch (e) {
        console.error('Failed to persist field definitions to localStorage', e);
      }
    }
    // Also persist permanently to server disk file
    fetch('/api/field-definitions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: jsonStr
    }).catch(err => console.warn('[FieldDictionary] Persistent save error:', err));
  }

  // --- AI Reports Records (View, Edit, Delete, Persist) ---
  public getAiReports(): AIAdvisorReportRecord[] {
    return [...this.aiReports].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  public addAiReport(report: Omit<AIAdvisorReportRecord, 'id' | 'createdAt'>): AIAdvisorReportRecord {
    const newRecord: AIAdvisorReportRecord = {
      ...report,
      id: `ai_rep_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      createdAt: new Date().toISOString()
    };
    this.aiReports.unshift(newRecord);
    this.persistReports();
    this.notify();
    return newRecord;
  }

  public updateAiReport(id: string, updates: Partial<Pick<AIAdvisorReportRecord, 'advice' | 'title' | 'userNotes'>>): void {
    const idx = this.aiReports.findIndex(r => r.id === id);
    if (idx >= 0) {
      this.aiReports[idx] = { ...this.aiReports[idx], ...updates };
      this.persistReports();
      this.notify();
    }
  }

  public deleteAiReport(id: string): void {
    this.aiReports = this.aiReports.filter(r => r.id !== id);
    this.persistReports();
    this.notify();
  }

  private persistReports(): void {
    const jsonStr = JSON.stringify(this.aiReports);
    if (typeof localStorage !== 'undefined') {
      try {
        localStorage.setItem(STORAGE_KEY_AI_REPORTS, jsonStr);
      } catch (e) {
        console.error('Failed to persist AI reports to localStorage', e);
      }
    }
    // Also persist permanently to server disk file
    fetch('/api/ai-advisor-reports', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: jsonStr
    }).catch(err => console.warn('[FieldDictionary] Persistent AI report save error:', err));
  }

  // Generate Bilingual System Prompt Segment for Gemini (only includes fields checked/enabled for AI)
  public buildDictionaryPrompt(categoryFilter?: 'clothes' | 'wearingReport' | 'junction'): string {
    const rawItems = categoryFilter ? this.getCategoryDefinitions(categoryFilter) : this.definitions;
    const items = rawItems.filter(item => item.enabledForAi !== false);
    if (items.length === 0) return '';

    let prompt = `\n=== 用户自定义数据与字段字典规范（中英文双语映射对照，严禁偏离） ===\n`;
    prompt += `用户在日常衣橱记录中采用英文输入字段和数值。你必须严格按照下列中英文双语对照来准确理解每个字段的真实体感与人体工学含义，绝对不能从字面草率推断：\n\n`;

    for (const item of items) {
      prompt += `【字段名】: ${item.fieldNameEn} (中文含义: ${item.fieldNameCn})\n`;
      prompt += `  - 核心定义: ${item.descriptionCn} (${item.descriptionEn})\n`;
      if (item.typicalValues && item.typicalValues.length > 0) {
        prompt += `  - 常见输入值与实际体感对应关系:\n`;
        for (const val of item.typicalValues) {
          prompt += `    * 输入 "${val.valueEn}" (${val.valueCn}) -> 真实含义: ${val.meaningCn} (${val.meaningEn})\n`;
        }
      }
      prompt += `\n`;
    }
    return prompt;
  }

  public subscribe(fn: () => void): () => void {
    this.listeners.push(fn);
    return () => {
      this.listeners = this.listeners.filter(l => l !== fn);
    };
  }

  private notify(): void {
    for (const listener of this.listeners) {
      try {
        listener();
      } catch (e) {
        console.error('Listener notify error:', e);
      }
    }
  }
}

export const fieldDictionaryService = new FieldDictionaryManager();

