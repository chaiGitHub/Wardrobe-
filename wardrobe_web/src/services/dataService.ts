import { Clothes, ClothesItem, WearingReport, ClothesJunction, ReportClothesJunction, PictureAttachment, AIReportVersion } from '../types';
import { getTodayDateKey, toDateKey } from '../utils/calendarCalculations';
import { syncOutbox, OutboxMutation, OutboxState } from './syncOutbox';
import { fieldDictionaryService } from './fieldDictionaryService';
import { ensureImageCacheLoaded, onImageCacheUpdate, notifyImageCacheListeners } from './localImageCache';
import { reportPresetService } from './reportPresetService';
import initialData from '../initialData.json';

export const COLLECTION_CLOTHES = 'clothes';
export const COLLECTION_WEARING_REPORT = 'wearingReport';
export const COLLECTION_CLOTHES_JUNCTION = 'clothesJunction';

const LEGACY_COLLECTION_WEARING_REPORTS = 'wearing_reports';
const LEGACY_COLLECTION_REPORT_CLOTHES = 'report_clothes_junctions';

const STORAGE_KEY_CLOTHES = 'wardrobe_db_clothes_v2';
const STORAGE_KEY_REPORTS = 'wardrobe_db_reports_v2';
const STORAGE_KEY_JUNCTIONS = 'wardrobe_db_junctions_v2';
const STORAGE_KEY_SYNC_TIME = 'wardrobe_db_last_sync_v2';

export function sortReportsDescending(reports: WearingReport[]): WearingReport[] {
  return [...reports].sort((a, b) => {
    const dateA = a['wear date'] ? new Date(a['wear date']).getTime() : 0;
    const dateB = b['wear date'] ? new Date(b['wear date']).getTime() : 0;
    if (dateB !== dateA) return dateB - dateA;
    const createdA = a['created'] ? new Date(a['created']).getTime() : 0;
    const createdB = b['created'] ? new Date(b['created']).getTime() : 0;
    return createdB - createdA;
  });
}

export type SyncStatus = 'synced' | 'syncing' | 'offline' | 'error';

export interface WashUndoRecord {
  id: string;
  timestamp: number;
  type: 'single' | 'all';
  description: string;
  garments: {
    id: string;
    name: string;
    prevTrackedWashes: number;
    prevTotalWashed: number;
    prevLastWashed?: string;
    prevWet?: 'dry' | 'wet' | string;
  }[];
  junctions: {
    id: string;
    prevWashStated?: 'wash' | 'need wash' | string;
    prevWashDate?: string;
    prevWashedDate?: string;
  }[];
}

export interface BackupFieldDiff {
  fieldName: string;
  category: 'clothes_custom' | 'report_custom' | 'dictionary' | 'top_level';
  itemType: 'garment' | 'report' | 'field_definition';
  count: number;
  sampleValues: string[];
  isInBackup: boolean;
  isInCurrent: boolean;
}

export interface BackupInspectionResult {
  fileName: string;
  fileSize: number;
  backupClothesCount: number;
  backupReportsCount: number;
  backupPhotosCount: number;
  backupFieldDefsCount: number;
  backupPresetsCount?: number;
  currentClothesCount: number;
  currentReportsCount: number;
  currentFieldDefsCount: number;
  currentPresetsCount?: number;
  newFieldsInCurrent: BackupFieldDiff[];
  allDetectedFields: BackupFieldDiff[];
  rawParsedDb?: any;
}

export interface RestoreFieldResolutions {
  fieldActions?: Record<string, 'keep' | 'rename' | 'delete'>;
  fieldRenames?: Record<string, string>; // e.g. { 'aa2': 'aa3' }
  additionalFieldsToAdd?: Array<{ name: string; target: 'clothes' | 'reports' | 'both'; defaultValue?: any }>;
  strategy?: 'smart_merge' | 'replace_with_fields';
}

/**
 * Formats duration in seconds into a friendly string like "15h 11m" or "4h" or "45m".
 */
export function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) return '0h';
  const totalMins = Math.round(seconds / 60);
  const hours = Math.floor(totalMins / 60);
  const mins = totalMins % 60;
  if (hours > 0 && mins > 0) {
    return `${hours}h ${mins}m`;
  } else if (hours > 0) {
    return `${hours}h`;
  } else {
    return `${mins}m`;
  }
}

/**
 * Standard file reader fallback for image attachments.
 * Always saves to local server disk storage or IndexedDB blob cache.
 * NEVER returns huge base64 strings that crash browser LocalStorage.
 */
export async function processImageFile(file: File): Promise<PictureAttachment> {
  const ext = file.name.split('.').pop() || 'jpg';
  const id = `pic_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  const storagePath = `garments/photos/${id}.${ext}`;

  try {
    const { uploadFileToStorage } = await import('./cloudStorageService');
    const uploaded = await uploadFileToStorage(file, storagePath);
    return {
      id,
      name: file.name,
      filename: file.name,
      url: uploaded.downloadUrl,
      originalUrl: uploaded.downloadUrl,
      presignedUrl: uploaded.downloadUrl,
      smThumbnailUrl: uploaded.downloadUrl,
      lgThumbnailUrl: uploaded.downloadUrl,
      storagePath: uploaded.storagePath,
      mimetype: uploaded.contentType || file.type || 'image/jpeg',
      size: uploaded.size || file.size
    };
  } catch (uploadErr) {
    console.warn('[processImageFile] Upload failed, caching blob in IndexedDB:', uploadErr);
  }

  // Fallback to IndexedDB Blob URL
  try {
    const { cacheImageBlob } = await import('./localImageCache');
    const blobUrl = URL.createObjectURL(file);
    await cacheImageBlob(id, file);
    return {
      id,
      name: file.name,
      filename: file.name,
      url: blobUrl,
      originalUrl: blobUrl,
      presignedUrl: blobUrl,
      smThumbnailUrl: blobUrl,
      lgThumbnailUrl: blobUrl,
      storagePath,
      mimetype: file.type || 'image/jpeg',
      size: file.size
    };
  } catch {
    const blobUrl = URL.createObjectURL(file);
    return {
      id,
      name: file.name,
      filename: file.name,
      url: blobUrl,
      originalUrl: blobUrl,
      presignedUrl: blobUrl,
      smThumbnailUrl: blobUrl,
      lgThumbnailUrl: blobUrl,
      storagePath,
      mimetype: file.type || 'image/jpeg',
      size: file.size
    };
  }
}

// Standardize Clothes with exact DB field names and values
function extractClimateScenarioField(f: any, scenario: 'withAircond' | 'withoutAircond' | 'sleepFan', feeling: 'hot' | 'leg'): string {
  if (!f || typeof f !== 'object') return '';
  const custom = { ...(f.customFields || {}), ...(f.extraAttributes || {}) };

  let canonical = '';
  if (scenario === 'withAircond') {
    canonical = feeling === 'hot' ? 'when with aircond: hot feeling' : 'when with aircond: leg feeling';
  } else if (scenario === 'withoutAircond') {
    canonical = feeling === 'hot' ? 'when without aircond: hot feeling' : 'when without aircond: leg feeling';
  } else {
    canonical = feeling === 'hot' ? 'when sleep under fan: hot feeling' : 'when sleep under fan: leg feeling';
  }

  // 1. Direct canonical lookup
  if (typeof f[canonical] === 'string' && f[canonical].trim()) return f[canonical].trim();
  if (typeof custom[canonical] === 'string' && custom[canonical].trim()) return custom[canonical].trim();

  // 2. Direct camelCase lookup
  if (scenario === 'withAircond') {
    const cc = feeling === 'hot' ? (f.whenWithAircondHot || f.whenWithAircondHotFeeling) : (f.whenWithAircondLeg || f.whenWithAircondLegFeeling);
    if (typeof cc === 'string' && cc.trim()) return cc.trim();
  } else if (scenario === 'withoutAircond') {
    const cc = feeling === 'hot' ? (f.whenWithoutAircondHot || f.whenWithoutAircondHotFeeling) : (f.whenWithoutAircondLeg || f.whenWithoutAircondLegFeeling);
    if (typeof cc === 'string' && cc.trim()) return cc.trim();
  } else {
    const cc = feeling === 'hot' ? (f.whenSleepUnderFanHot || f.whenSleepUnderFanHotFeeling) : (f.whenSleepUnderFanLeg || f.whenSleepUnderFanLegFeeling);
    if (typeof cc === 'string' && cc.trim()) return cc.trim();
  }

  // 3. Flexible normalized key search across all entries
  const isMatch = (key: string): boolean => {
    const k = key.toLowerCase().replace(/[^a-z0-9]/g, '');
    let sc = false;
    if (scenario === 'withAircond') {
      sc = (k.includes('withaircond') || k.includes('withairconditioning')) && !k.includes('without');
    } else if (scenario === 'withoutAircond') {
      sc = k.includes('withoutaircond') || k.includes('withoutairconditioning') || k.includes('noaircond');
    } else if (scenario === 'sleepFan') {
      sc = k.includes('sleepunderfan') || k.includes('sleepfan') || k.includes('underfan');
    }
    const fe = k.includes(feeling + 'feeling') || k.endsWith(feeling);
    return sc && fe;
  };

  for (const [k, v] of [...Object.entries(f), ...Object.entries(custom)]) {
    if (typeof v === 'string' && v.trim() && isMatch(k)) {
      return v.trim();
    }
  }

  return '';
}

export function normalizeGarment(r: any): ClothesItem {
  const f = r.fields || r;
  const id = r.id || f.id || `garment_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  
  const legacyWashed = typeof f['wearing frequency(how many times legacy washed)'] === 'number'
    ? f['wearing frequency(how many times legacy washed)']
    : (typeof f.wearingFrequency === 'number' ? f.wearingFrequency : (typeof f['Total Washed Count'] === 'number' ? f['Total Washed Count'] : 0));
  const trackedAppWashes = typeof f['Tracked App Washes'] === 'number'
    ? f['Tracked App Washes']
    : (typeof f.trackedAppWashes === 'number' ? f.trackedAppWashes : 0);
  // User formula: Total Washed Count = wearing frequency(legacy) + Tracked App Washes
  const totalWashed = legacyWashed + trackedAppWashes;

  // Collect all dynamic custom attributes from customFields and extraAttributes
  const explicitCustom: Record<string, any> = {
    ...(typeof f.extraAttributes === 'object' && f.extraAttributes !== null ? f.extraAttributes : {}),
    ...(typeof f.customFields === 'object' && f.customFields !== null ? f.customFields : {})
  };

  // Also collect any top-level non-standard dynamic custom attributes present in f (e.g. Color, Brand, Stretch, Fabric Weight)
  const standardGarmentKeys = new Set([
    'id', 'fields', 'name', 'Name', 'clothesType', 'Clothes Type', 'material', 'materialProp',
    'thickClothes', 'thick clothes', 'hotAtNoon', 'Hot at noon', 'diaoDai', 'diao dai',
    'sideBreastOk', 'side breast ok', 'side breast ok: with 聚拢 bra', 'side breast ok: With 聚拢bra',
    'sideBreastOkWithGatherBra', 'side breast ok: with side out bra', 'side breast ok: With side out bra',
    'sideBreastOkWithSideOutBra', 'sitBreastOk', 'sit breast ok', 'sit breast ok: With 聚拢bra',
    'sit breast ok: with 聚拢bra', 'sitBreastOkWithGatherBra', 'sit breast ok: with side out bra',
    'sit breast ok: With side out bra', 'sitBreastOkWithSideOutBra',
    'when with aircond: hot feeling', 'when with aircond: leg feeling',
    'when without aircond: hot feeling', 'when without aircond: leg feeling',
    'when sleep under fan: hot feeling', 'when sleep under fan: leg feeling',
    'whenWithAircondHot', 'whenWithAircondHotFeeling', 'whenWithAircondLeg', 'whenWithAircondLegFeeling',
    'whenWithoutAircondHot', 'whenWithoutAircondHotFeeling', 'whenWithoutAircondLeg', 'whenWithoutAircondLegFeeling',
    'whenSleepUnderFanHot', 'whenSleepUnderFanHotFeeling', 'whenSleepUnderFanLeg', 'whenSleepUnderFanLegFeeling',
    'centerChest', 'center chest', 'beautiful', 'beatifulReason', 'beatiful reason', 'wet', 'wetReason', 'wet reason',
    'descriptions', 'Descriptions', 'descriptions2', 'Descriptions 2', 'price',
    'wearingFrequency', 'wearing frequency(how many times legacy washed)', 'trackedAppWashes', 'Tracked App Washes',
    'Total Washed Count', 'lastWashed', 'Last Washed', 'aiClothesAnalysis', 'AI Clothes Analysis',
    'pictures', 'Pictures', 'mayThrow', 'may throw', 'lastComment', 'lastCommentOrAction', 'last comment or action',
    'tags', 'created', 'longDress', 'long dress', 'unwash time rollup',
    'wash stated (from Full Wearing Report (new design) clothes list (linked))',
    'fieldExplanations', 'field_explanations', 'customFields', 'extraAttributes', 'aiReports', 'contentBlocks'
  ]);

  for (const [k, v] of Object.entries(f)) {
    if (!standardGarmentKeys.has(k) && !k.startsWith('_') && !k.endsWith('_explain') && typeof v !== 'function') {
      if (v !== undefined && v !== null && String(v).trim() !== '') {
        if (explicitCustom[k] === undefined || explicitCustom[k] === null || String(explicitCustom[k]).trim() === '') {
          explicitCustom[k] = v;
        }
      }
    }
  }

  const garment: ClothesItem = {
    ...f,
    id,
    'Name': f['Name'] || f.name || 'Untitled Garment',
    'Clothes Type': f['Clothes Type'] || f.clothesType || 'Dress',
    'material': f['material'] || 'Chiffon',
    'thick clothes': f['thick clothes'] || f.thickClothes || '01 not thick clothes',
    'Hot at noon': f['Hot at noon'] || f.hotAtNoon || 'Not hot',
    'diao dai': f['diao dai'] || f.diaoDai || 'not diao dai',
    'side breast ok': f['side breast ok'] || f.sideBreastOk || 'side breast shape ok',
    'side breast ok: with 聚拢 bra': f['side breast ok: with 聚拢 bra'] || f['side breast ok: With 聚拢bra'] || f.sideBreastOkWithGatherBra || f['side breast ok'] || f.sideBreastOk || 'side breast shape ok',
    'side breast ok: with side out bra': f['side breast ok: with side out bra'] || f['side breast ok: With side out bra'] || f.sideBreastOkWithSideOutBra || 'side breast out',
    'sit breast ok': f['sit breast ok'] || f.sitBreastOk || 'sit breast shape ok',
    'sit breast ok: With 聚拢bra': f['sit breast ok: With 聚拢bra'] || f['sit breast ok: with 聚拢bra'] || f.sitBreastOkWithGatherBra || f['sit breast ok'] || f.sitBreastOk || 'sit breast shape ok',
    'sit breast ok: with side out bra': f['sit breast ok: with side out bra'] || f['sit breast ok: With side out bra'] || f.sitBreastOkWithSideOutBra || f['sit breast ok'] || f.sitBreastOk || 'ugly',
    'when with aircond: hot feeling': extractClimateScenarioField(f, 'withAircond', 'hot'),
    'when with aircond: leg feeling': extractClimateScenarioField(f, 'withAircond', 'leg'),
    'when without aircond: hot feeling': extractClimateScenarioField(f, 'withoutAircond', 'hot'),
    'when without aircond: leg feeling': extractClimateScenarioField(f, 'withoutAircond', 'leg'),
    'when sleep under fan: hot feeling': extractClimateScenarioField(f, 'sleepFan', 'hot'),
    'when sleep under fan: leg feeling': extractClimateScenarioField(f, 'sleepFan', 'leg'),
    fieldExplanations: typeof f.fieldExplanations === 'object' && f.fieldExplanations !== null
      ? { ...f.fieldExplanations }
      : (typeof f.field_explanations === 'object' && f.field_explanations !== null ? { ...f.field_explanations } : {}),
    'center chest': f['center chest'] || f.centerChest || 'no center chest',
    'beautiful': f['beautiful'] || f.beautiful || 'Beautiful',
    'beatiful reason': f['beatiful reason'] || f.beatifulReason || '',
    'wet': f['wet'] || f.wet || 'dry',
    'wet reason': f['wet reason'] || f.wetReason || '',
    'Descriptions': f['Descriptions'] || f.descriptions || '',
    'Descriptions 2': f['Descriptions 2'] || f.descriptions2 || '',
    'price': typeof f['price'] === 'number' ? f['price'] : undefined,
    'wearing frequency(how many times legacy washed)': legacyWashed,
    'Tracked App Washes': trackedAppWashes,
    'Total Washed Count': totalWashed,
    'Last Washed': f['Last Washed'] || f.lastWashed || '',
    'AI Clothes Analysis': f['AI Clothes Analysis'] || f.aiClothesAnalysis || '',
    'Pictures': Array.isArray(f['Pictures']) ? f['Pictures'] : (Array.isArray(f.pictures) ? f.pictures : []),
    'may throw': f['may throw'] || f.mayThrow || '',
    'last comment or action': f['last comment or action'] || f.lastCommentOrAction || '',
    'tags': Array.isArray(f['tags']) ? f['tags'] : (Array.isArray(f.tags) ? f.tags : []),
    'created': f['created'] || f.created || new Date().toISOString(),
    'long dress': f['long dress'] || f.longDress || 'short dress',
    'unwash time rollup': typeof f['unwash time rollup'] === 'number' ? f['unwash time rollup'] : 0,
    'wash stated (from Full Wearing Report (new design) clothes list (linked))': Array.isArray(f['wash stated (from Full Wearing Report (new design) clothes list (linked))'])
      ? f['wash stated (from Full Wearing Report (new design) clothes list (linked))']
      : [],
    customFields: explicitCustom,
    extraAttributes: explicitCustom
  };

  // Mirror all custom attributes onto top-level garment properties
  for (const [k, v] of Object.entries(explicitCustom)) {
    if ((garment as any)[k] === undefined || (garment as any)[k] === null || String((garment as any)[k]).trim() === '') {
      (garment as any)[k] = v;
    }
  }

  // Normalize multi-version AI diagnostic reports for garment
  const rawGarmentAiReports = Array.isArray(f.aiReports) ? f.aiReports : [];
  let garmentAiReports: AIReportVersion[] = rawGarmentAiReports.map((v: any, idx: number) => ({
    id: v.id || `garment_ai_${Date.now()}_${idx}`,
    timestamp: v.timestamp || garment['created'] || new Date().toISOString(),
    title: v.title || `Report #${idx + 1}`,
    model: v.model || 'gemini-3.6-flash',
    content: v.content || '',
    isPrimary: Boolean(v.isPrimary)
  }));

  const legacyGarmentAnalysis = garment['AI Clothes Analysis'] || '';
  if (garmentAiReports.length === 0 && legacyGarmentAnalysis.trim()) {
    garmentAiReports = [{
      id: 'garment_initial_version',
      timestamp: garment['created'] || new Date().toISOString(),
      title: 'Initial Report',
      model: 'gemini-3.6-flash',
      content: legacyGarmentAnalysis.trim(),
      isPrimary: true
    }];
  }

  if (garmentAiReports.length > 0 && !garmentAiReports.some(r => r.isPrimary)) {
    garmentAiReports[0].isPrimary = true;
  }

  garment.aiReports = garmentAiReports;
  const primaryGarmentReport = garmentAiReports.find(r => r.isPrimary) || garmentAiReports[0];
  if (primaryGarmentReport) {
    garment['AI Clothes Analysis'] = primaryGarmentReport.content;
  }

  // Attach normalized aliases for legacy component access
  garment.name = garment['Name'];
  garment.clothesType = garment['Clothes Type'];
  garment.materialProp = garment['material'];
  return garment;
}

export function generateDefaultReportName(wearDateVal?: string): string {
  const d = wearDateVal ? String(wearDateVal).slice(0, 10) : new Date().toISOString().slice(0, 10);
  const now = new Date();
  const hh = String(now.getHours()).padStart(2, '0');
  const mm = String(now.getMinutes()).padStart(2, '0');
  return `${d} ${hh}${mm} wearing report`;
}

// Standardize Reports with exact DB field names and values
export function normalizeReport(r: any): WearingReport {
  const f = r.fields || r;
  const id = r.id || f.id || `report_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;

  // Priority to wear date fields, NEVER accidentally prefer created date over wear date
  const rawWearDate = f['wear date'] || f.wearDate || f['wear_date'] || f['Wear Date'] || f['Wear date'] || f['wearing date'] || f['wearing_date'] || f['Date'];
  const rawCreated = f['created'] || f.created || f['createdTime'] || f['createdAt'];

  // Normalize multiple AI report versions if present, or backward-convert legacy single report
  const rawAiReports = Array.isArray(f.aiReports) ? f.aiReports : [];
  let aiReports: AIReportVersion[] = rawAiReports.map((v: any, idx: number) => ({
    id: v.id || `ai_ver_${Date.now()}_${idx}`,
    timestamp: v.timestamp || rawCreated || new Date().toISOString(),
    title: v.title || `Report #${idx + 1}`,
    model: v.model || 'gemini-3.6-flash',
    content: v.content || '',
    isPrimary: Boolean(v.isPrimary)
  }));

  const legacyAnalysis = f['AI Wearing Analysis'] || f.aiWearingAnalysis || '';
  if (aiReports.length === 0 && legacyAnalysis.trim()) {
    aiReports = [{
      id: 'initial_version',
      timestamp: rawCreated || new Date().toISOString(),
      title: 'Initial Report',
      model: 'gemini-3.8-flash',
      content: legacyAnalysis.trim(),
      isPrimary: true
    }];
  }

  // Ensure one primary report is designated
  if (aiReports.length > 0 && !aiReports.some(r => r.isPrimary)) {
    aiReports[0].isPrimary = true;
  }

  const primaryReport = aiReports.find(r => r.isPrimary) || aiReports[0];
  const activeAnalysis = primaryReport ? primaryReport.content : legacyAnalysis;

  const explicitReportCustom: Record<string, any> = {
    ...(typeof f.extraAttributes === 'object' && f.extraAttributes !== null ? f.extraAttributes : {}),
    ...(typeof f.customFields === 'object' && f.customFields !== null ? f.customFields : {})
  };

  const standardReportKeys = new Set([
    'id', 'fields', 'name', 'Name', 'wearDate', 'wear date', 'created', 'createdTime', 'createdAt',
    'categories', 'tags', 'shoulderFeel', 'shoulder feel', 'midBreast', 'mid breast',
    'nightMidBreast', 'night mid breast', 'sweat', 'sweatProp', 'nightSweat', 'night sweat',
    'legFeel', 'leg feel', 'nightLegFeel', 'night leg feel', 'reportDetail', 'report detail',
    'aiWearingAnalysis', 'AI Wearing Analysis', 'pictures', 'Pictures', 'aiReports',
    'fieldExplanations', 'customFields', 'extraAttributes'
  ]);

  for (const [k, v] of Object.entries(f)) {
    if (!standardReportKeys.has(k) && !k.startsWith('_') && !k.endsWith('_explain') && typeof v !== 'function') {
      if (v !== undefined && v !== null && String(v).trim() !== '') {
        if (explicitReportCustom[k] === undefined || explicitReportCustom[k] === null || String(explicitReportCustom[k]).trim() === '') {
          explicitReportCustom[k] = v;
        }
      }
    }
  }

  const report: WearingReport = {
    ...f,
    id,
    'Name': f['Name'] || f.name || generateDefaultReportName(rawWearDate),
    'wear date': rawWearDate || rawCreated || new Date().toISOString(),
    'created': rawCreated || new Date().toISOString(),
    'categories': Array.isArray(f['categories']) ? f['categories'] : (Array.isArray(f.categories) ? f.categories : ['[[Notes]]', '[[Wearing Report]]']),
    'tags': Array.isArray(f['tags']) ? f['tags'] : (Array.isArray(f.tags) ? f.tags : ['#journal', '#wearingreport']),
    'shoulder feel': f['shoulder feel'] || f.shoulderFeel || '',
    'mid breast': f['mid breast'] || f.midBreast || '',
    'night mid breast': f['night mid breast'] || f.nightMidBreast || '',
    'sweat': f['sweat'] || f.sweat || '',
    'night sweat': f['night sweat'] || f.nightSweat || '',
    'leg feel': f['leg feel'] || f.legFeel || '',
    'night leg feel': f['night leg feel'] || f.nightLegFeel || '',
    'report detail': f['report detail'] || f.reportDetail || '',
    'AI Wearing Analysis': activeAnalysis,
    'Pictures': Array.isArray(f['Pictures']) ? f['Pictures'] : (Array.isArray(f.pictures) ? f.pictures : []),
    aiReports,
    fieldExplanations: f.fieldExplanations || f.customFields?._fieldExplanations || {},
    customFields: explicitReportCustom,
    extraAttributes: explicitReportCustom
  };

  // Mirror all custom attributes onto top-level report properties
  for (const [k, v] of Object.entries(explicitReportCustom)) {
    if ((report as any)[k] === undefined || (report as any)[k] === null || String((report as any)[k]).trim() === '') {
      (report as any)[k] = v;
    }
  }

  report.name = report['Name'];
  report.wearDate = report['wear date'];
  report.reportDetail = report['report detail'];
  report.shoulderFeel = report['shoulder feel'];
  report.midBreast = report['mid breast'];
  report.nightMidBreast = report['night mid breast'];
  report.sweatProp = report['sweat'];
  report.nightSweat = report['night sweat'];
  report.legFeel = report['leg feel'];
  report.nightLegFeel = report['night leg feel'];
  report.aiWearingAnalysis = report['AI Wearing Analysis'];
  report.fieldExplanations = f.fieldExplanations || f.customFields?._fieldExplanations || {};
  return report;
}

function extractLinkField(raw: any, fallbackTitle: string = ''): { id: string; title: string } {
  if (!raw) return { id: '', title: fallbackTitle };
  if (Array.isArray(raw)) {
    if (raw.length === 0) return { id: '', title: fallbackTitle };
    return extractLinkField(raw[0], fallbackTitle);
  }
  if (typeof raw === 'object' && raw !== null) {
    const id = String(raw.id || raw.recordId || raw._id || (raw.title && !raw.id ? raw.title : '') || '');
    const title = String(raw.title || raw.name || raw.Name || fallbackTitle || id);
    return { id: id || title, title: title || id };
  }
  const str = String(raw);
  return { id: str, title: fallbackTitle || str };
}

// Standardize Report Clothes Junctions with exact DB field names
export function normalizeJunction(r: any): ReportClothesJunction {
  const f = r.fields || r;
  const id = r.id || f.id || `junction_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;

  const repIdObj = f['wearing report id'] || f.wearingReportId;
  const clothesObj = f['clothes(new)'] || f.clothesId;

  const washStatedVal = f['wash stated'] || f.washStated || 'wash';
  const isWashed = washStatedVal === 'wash';
  const rawWashDate = isWashed
    ? (f['wash date'] || f.washDate || f['washed date'] || f.washedDate || undefined)
    : undefined;

  const repLink = extractLinkField(repIdObj, f.wearingReportTitle || '');
  const clothesLink = extractLinkField(clothesObj, f.clothesTitle || '');

  const junction: ReportClothesJunction = {
    ...f,
    id,
    'name': typeof f['name'] === 'number' ? f['name'] : (typeof f.name === 'number' ? f.name : 1),
    'wearing report id': repLink,
    'clothes(new)': clothesLink,
    'duration': typeof f['duration'] === 'number' ? f['duration'] : (typeof f.duration === 'number' ? f.duration : 14400),
    'wash stated': washStatedVal,
    'Clothes Type (from clothes(new))': f['Clothes Type (from clothes(new))'] || f.clothesType || 'Dress',
    'wash date': rawWashDate,
    'washed date': rawWashDate,
    'wear date': f['wear date'] || f.wearDate || undefined,
    'AI Junction Analysis': f['AI Junction Analysis'] || f.aiJunctionAnalysis || ''
  };

  // Normalize multi-version AI reports for clothes junction
  const rawJunctionAiReports = Array.isArray(f.aiReports) ? f.aiReports : [];
  let junctionAiReports: AIReportVersion[] = rawJunctionAiReports.map((v: any, idx: number) => ({
    id: v.id || `junction_ai_${Date.now()}_${idx}`,
    timestamp: v.timestamp || new Date().toISOString(),
    title: v.title || `Report #${idx + 1}`,
    model: v.model || 'gemini-3.6-flash',
    content: v.content || '',
    isPrimary: Boolean(v.isPrimary)
  }));

  const legacyJunctionAnalysis = junction['AI Junction Analysis'] || '';
  if (junctionAiReports.length === 0 && legacyJunctionAnalysis.trim()) {
    junctionAiReports = [{
      id: 'junction_initial_version',
      timestamp: new Date().toISOString(),
      title: 'Initial Report',
      model: 'gemini-3.6-flash',
      content: legacyJunctionAnalysis.trim(),
      isPrimary: true
    }];
  }

  if (junctionAiReports.length > 0 && !junctionAiReports.some(r => r.isPrimary)) {
    junctionAiReports[0].isPrimary = true;
  }

  junction.aiReports = junctionAiReports;
  const primaryJunctionReport = junctionAiReports.find(r => r.isPrimary) || junctionAiReports[0];
  if (primaryJunctionReport) {
    junction['AI Junction Analysis'] = primaryJunctionReport.content;
  }

  junction.clothesId = clothesLink.id;
  junction.clothesTitle = clothesLink.title;
  junction.wearingReportId = repLink.id;
  junction.wearingReportTitle = repLink.title;
  junction.washStated = junction['wash stated'];
  junction.durationVal = junction['duration'];
  junction.washDate = rawWashDate;
  junction.washedDate = rawWashDate;
  junction.wearDate = junction['wear date'];
  return junction;
}

export class DataService {
  private clothes: ClothesItem[] = [];
  private reports: WearingReport[] = [];
  private junctions: ReportClothesJunction[] = [];
  private syncStatus: SyncStatus = 'synced';
  private lastSyncTime: string | null = null;
  private listeners: (() => void)[] = [];
  private washUndoStack: WashUndoRecord[] = [];
  private undoListeners: Set<(record: WashUndoRecord | null) => void> = new Set();
  private saveDebounceTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.init();
    onImageCacheUpdate(() => {
      this.notify();
    });
    syncOutbox.subscribe(outboxState => {
      this.syncStatus = 'synced';
      if (outboxState.lastSyncTime) {
        this.lastSyncTime = outboxState.lastSyncTime;
      }
      this.notify();
    });
  }

  private async init() {
    // Warm up offline image cache from IndexedDB
    ensureImageCacheLoaded().then(() => this.notify()).catch(() => {});

    // 1. Local-First: Load immediately from device storage (LocalStorage/IndexedDB) so user data is never wiped
    let loadedFromLocal = false;
    let localUpdatedAt = 0;
    try {
      const storedClothes = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY_CLOTHES) : null;
      const storedReports = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY_REPORTS) : null;
      const storedJunctions = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY_JUNCTIONS) : null;
      this.lastSyncTime = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY_SYNC_TIME) : null;
      const storedTime = typeof localStorage !== 'undefined' ? localStorage.getItem('wardrobe_db_updated_at') : null;
      if (storedTime) localUpdatedAt = parseInt(storedTime, 10) || 0;

      if (storedClothes && storedReports && storedJunctions) {
        const parsedClothes = JSON.parse(storedClothes);
        const parsedReports = JSON.parse(storedReports);
        if (Array.isArray(parsedClothes) && parsedClothes.length > 0) {
          this.clothes = parsedClothes.map(normalizeGarment);
          // Preserve all wearing reports by their unique record ID
          const reportMap = new Map<string, any>();
          (parsedReports || []).forEach((r: any) => {
            if (r && r.id) reportMap.set(r.id, r);
          });
          this.reports = sortReportsDescending(Array.from(reportMap.values()).map(normalizeReport));
          this.junctions = JSON.parse(storedJunctions).map(normalizeJunction);
          this.syncStatus = 'synced';
          this.notify();
          loadedFromLocal = true;
        }
      }
    } catch (e) {
      console.warn('[DataService] LocalStorage read error:', e);
    }

    // 2. Local-disk verification & auto-healing across container rebuilds
    try {
      const res = await fetch('/api/local-db');
      if (res.ok) {
        const serverData = await res.json();
        const serverUpdatedAt = serverData.updatedAt ? new Date(serverData.updatedAt).getTime() : 0;

        if (loadedFromLocal) {
          // Reconcile non-empty fields from serverData into this.clothes (e.g. climate fields, fieldExplanations)
          if (Array.isArray(serverData.clothes) && serverData.clothes.length > 0) {
            const serverClothesMap = new Map<string, any>(serverData.clothes.map((sc: any) => [sc.id, sc]));
            this.clothes.forEach(localC => {
              const sC = serverClothesMap.get(localC.id);
              if (sC) {
                for (const [k, v] of Object.entries(sC)) {
                  if (v !== undefined && v !== null && String(v).trim() !== '') {
                    const localVal = (localC as any)[k];
                    if (localVal === undefined || localVal === null || String(localVal).trim() === '') {
                      (localC as any)[k] = v;
                    }
                  }
                }
                if (sC.fieldExplanations && typeof sC.fieldExplanations === 'object') {
                  localC.fieldExplanations = {
                    ...sC.fieldExplanations,
                    ...(localC.fieldExplanations || {})
                  };
                }
              }
            });
            this.persistLocalOnly();
          }

          // If server was freshly rebuilt or client local storage has more/newer edits:
          if (!serverData.clothes || serverData.clothes.length === 0 || this.clothes.length > serverData.clothes.length || localUpdatedAt >= serverUpdatedAt) {
            console.log('[DataService] Preserving latest user local data to container disk...');
            this.reconcileAllGarmentsWashStats();
            await this.saveToLocalDisk();
            return;
          } else if (serverUpdatedAt > localUpdatedAt && Array.isArray(serverData.clothes) && serverData.clothes.length > 0) {
            console.log('[DataService] Server disk is newer than browser storage. Syncing from server...');
            this.clothes = serverData.clothes.map(normalizeGarment);
            this.reports = sortReportsDescending((serverData.reports || []).map(normalizeReport));
            this.junctions = (serverData.junctions || []).map(normalizeJunction);
            if (Array.isArray(serverData.drilldownPresets) && serverData.drilldownPresets.length > 0) {
              reportPresetService.importPresets(serverData.drilldownPresets, false, false);
            }
            if (typeof localStorage !== 'undefined') {
              localStorage.setItem('wardrobe_db_updated_at', String(serverUpdatedAt));
            }
            this.reconcileAllGarmentsWashStats();
            this.persistLocalOnly();
            this.notify();
            return;
          }
        } else {
          // No local data yet; load authoritative server data
          if (Array.isArray(serverData.clothes) && serverData.clothes.length > 0) {
            this.clothes = serverData.clothes.map(normalizeGarment);
            this.reports = sortReportsDescending((serverData.reports || []).map(normalizeReport));
            this.junctions = (serverData.junctions || []).map(normalizeJunction);
            if (Array.isArray(serverData.drilldownPresets) && serverData.drilldownPresets.length > 0) {
              reportPresetService.importPresets(serverData.drilldownPresets, false, false);
            }
            if (typeof localStorage !== 'undefined') {
              localStorage.setItem('wardrobe_db_updated_at', String(serverUpdatedAt || Date.now()));
            }
            this.reconcileAllGarmentsWashStats();
            this.syncStatus = 'synced';
            this.persistLocalOnly();
            this.notify();
            return;
          }
        }
      }
    } catch (err) {
      console.warn('[DataService] Server disk read at init:', err);
    }

    if (!loadedFromLocal) {
      // 4. Fallback to bundled initialData for brand new installs
      this.clothes = (initialData.clothes || []).map(normalizeGarment);
      this.reports = sortReportsDescending((initialData.reports || []).map(normalizeReport));
      this.junctions = (((initialData as any).reportClothes || (initialData as any).junctions) || []).map(normalizeJunction);
      if (Array.isArray((initialData as any).drilldownPresets) && (initialData as any).drilldownPresets.length > 0) {
        reportPresetService.importPresets((initialData as any).drilldownPresets, false, false);
      }
      this.reconcileAllGarmentsWashStats();
      this.syncStatus = 'synced';
      this.persistLocalOnly();
      this.notify();
    } else {
      this.reconcileAllGarmentsWashStats();
    }
  }

  public async pullFromLocalDisk(): Promise<{ clothes: number; reports: number; junctions: number }> {
    try {
      const res = await fetch('/api/local-db');
      if (res.ok) {
        const data = await res.json();
        let changed = false;
        if (Array.isArray(data.clothes) && data.clothes.length > 0) {
          this.clothes = data.clothes.map(normalizeGarment);
          changed = true;
        }
        if (Array.isArray(data.reports) && data.reports.length > 0) {
          this.reports = sortReportsDescending(data.reports.map(normalizeReport));
          changed = true;
        }
        if (Array.isArray(data.junctions) && data.junctions.length > 0) {
          this.junctions = data.junctions.map(normalizeJunction);
          changed = true;
        }
        if (Array.isArray(data.drilldownPresets) && data.drilldownPresets.length > 0) {
          reportPresetService.importPresets(data.drilldownPresets, false, false);
        }
        if (changed) {
          this.reconcileAllGarmentsWashStats();
          this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
          this.syncStatus = 'synced';
          this.persistLocalOnly();
          this.notify();
        }
      }
    } catch (err) {
      console.warn('[DataService] pullFromLocalDisk warning:', err);
    }
    return {
      clothes: this.clothes.length,
      reports: this.reports.length,
      junctions: this.junctions.length
    };
  }

  public async saveToLocalDisk(onProgress?: (step: string, percent: number) => void): Promise<{ clothes: number; reports: number; junctions: number }> {
    onProgress?.('Saving to local database...', 25);
    try {
      const nowIso = new Date().toISOString();
      const currentPresets = reportPresetService.getPresets();
      const res = await fetch('/api/local-db', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          updatedAt: nowIso,
          clothes: this.clothes,
          reports: this.reports,
          junctions: this.junctions,
          drilldownPresets: currentPresets
        })
      });
      onProgress?.('Writing to local disk...', 75);
      if (res.ok) {
        if (typeof localStorage !== 'undefined') {
          localStorage.setItem('wardrobe_db_updated_at', String(new Date(nowIso).getTime()));
        }
        this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        this.syncStatus = 'synced';
        this.notify();
      }
      onProgress?.('Saved to local storage!', 100);
    } catch (err) {
      console.warn('[DataService] saveToLocalDisk warning:', err);
    }
    return {
      clothes: this.clothes.length,
      reports: this.reports.length,
      junctions: this.junctions.length
    };
  }

  public async checkAndMigrateLegacyCollections(): Promise<void> {
    // 100% Local app - no legacy cloud migration needed
  }

  public initRealtimeFirestore() {
    // 100% Local app - zero cloud dependencies
    this.syncStatus = 'synced';
    this.notify();
  }

  public async emptyDatabase(options?: { deletePhotos?: boolean }): Promise<{ clothes: number; reports: number; junctions: number }> {
    try {
      const res = await fetch('/api/database/empty', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deletePhotos: Boolean(options?.deletePhotos) })
      });
      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error || 'Server failed to empty database.');
      }
    } catch (err) {
      console.warn('[DataService] emptyDatabase server API error, wiping locally:', err);
    }

    // Clear in-memory collections
    this.clothes = [];
    this.reports = [];
    this.junctions = [];
    this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.syncStatus = 'synced';

    // Clear local storage entries
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('wardrobe_db_updated_at', String(Date.now()));
      localStorage.setItem(STORAGE_KEY_CLOTHES, JSON.stringify([]));
      localStorage.setItem(STORAGE_KEY_REPORTS, JSON.stringify([]));
      localStorage.setItem(STORAGE_KEY_JUNCTIONS, JSON.stringify([]));
      if (this.lastSyncTime) {
        localStorage.setItem(STORAGE_KEY_SYNC_TIME, this.lastSyncTime);
      }
    }

    this.notify();
    return {
      clothes: 0,
      reports: 0,
      junctions: 0
    };
  }

  public async resetToSeedData(): Promise<{ clothes: number; reports: number; junctions: number }> {
    let loadedFromServer = false;
    try {
      const res = await fetch('/api/database/reset-seed', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });
      if (res.ok) {
        const body = await res.json();
        const payload = body.data || body;
        if (Array.isArray(payload.clothes)) {
          this.clothes = payload.clothes.map(normalizeGarment);
          this.reports = sortReportsDescending((payload.reports || []).map(normalizeReport));
          this.junctions = (payload.junctions || []).map(normalizeJunction);
          loadedFromServer = true;
        }
      }
    } catch (err) {
      console.warn('[DataService] reset-seed endpoint failed, using bundled seed:', err);
    }

    if (!loadedFromServer) {
      // Fallback to bundled initialData.json
      this.clothes = (initialData.clothes || []).map(normalizeGarment);
      this.reports = sortReportsDescending((initialData.reports || []).map(normalizeReport));
      this.junctions = (((initialData as any).reportClothes || (initialData as any).junctions) || []).map(normalizeJunction);
      await this.saveToLocalDisk().catch(() => {});
    }

    const nowIso = new Date().toISOString();
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('wardrobe_db_updated_at', String(new Date(nowIso).getTime()));
    }
    this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.syncStatus = 'synced';
    this.persistLocalOnly();
    this.notify();

    return {
      clothes: this.clothes.length,
      reports: this.reports.length,
      junctions: this.junctions.length
    };
  }

  public async resetToSeed() {
    return this.resetToSeedData();
  }

  private persistLocalOnly() {
    try {
      if (typeof localStorage !== 'undefined') {
        // Strip large base64 data URLs before writing to LocalStorage to stay far below the 5MB browser quota
        const sanitizedClothes = this.clothes.map(c => ({
          ...c,
          Pictures: (c.Pictures || []).map(p => ({
            id: p.id,
            name: p.name,
            filename: p.filename,
            url: p.url?.startsWith('data:') ? '' : p.url,
            storagePath: p.storagePath,
            mimetype: p.mimetype,
            size: p.size
          }))
        }));

        const sanitizedReports = this.reports.map(r => ({
          ...r,
          Pictures: (r.Pictures || []).map(p => ({
            id: p.id,
            name: p.name,
            filename: p.filename,
            url: p.url?.startsWith('data:') ? '' : p.url,
            storagePath: p.storagePath,
            mimetype: p.mimetype,
            size: p.size
          }))
        }));

        localStorage.setItem(STORAGE_KEY_CLOTHES, JSON.stringify(sanitizedClothes));
        localStorage.setItem(STORAGE_KEY_REPORTS, JSON.stringify(sanitizedReports));
        localStorage.setItem(STORAGE_KEY_JUNCTIONS, JSON.stringify(this.junctions));
        if (this.lastSyncTime) {
          localStorage.setItem(STORAGE_KEY_SYNC_TIME, this.lastSyncTime);
        }
      }
    } catch (e) {
      console.warn('Local storage write warning', e);
    }
  }

  /**
   * Triggers download of the comprehensive full backup archive.
   * Includes:
   * - Full database JSON (garments, reports, junctions)
   * - High-resolution original photos from local IndexedDB and disk
   * - Formatted Markdown (.md) AI Wearing Reports for every session
   * - AI Advisor independent reports & custom field definitions
   * - Archive metadata & README
   */
  public async downloadFullBackupZip(
    onProgress?: (step: string, percent: number) => void
  ): Promise<void> {
    const blob = await this.createFullBackupBlob(onProgress);

    // Sync full archive to server storage in background so server uploads/ is also populated
    try {
      fetch('/api/backup/restore-zip', {
        method: 'POST',
        headers: { 'Content-Type': 'application/octet-stream' },
        body: blob
      }).catch(() => {});
    } catch {}

    const dateStr = new Date().toISOString().slice(0, 10);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `wardrobe-full-backup-${dateStr}.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  }

  /**
   * Inspects a ZIP backup archive before restoring, extracting counts and detecting
   * fields that exist in the current database vs the backup archive.
   */
  public async inspectBackupZip(fileOrBlob: File | Blob): Promise<BackupInspectionResult> {
    const JSZip = (await import('jszip')).default;
    const zip = await JSZip.loadAsync(fileOrBlob);
    const allFiles = Object.keys(zip.files);
    const dbEntryKey = allFiles.find(
      f => !zip.files[f].dir && (
        f === 'database.json' ||
        f.endsWith('/database.json') ||
        f === 'initialData.json' ||
        f.endsWith('/initialData.json') ||
        f === 'local_db.json' ||
        f.endsWith('/local_db.json')
      )
    );

    if (!dbEntryKey) {
      throw new Error('Invalid backup archive: missing database.json inside archive.');
    }

    const dbFile = zip.file(dbEntryKey);
    if (!dbFile) {
      throw new Error('Could not read database.json from backup archive.');
    }

    const dbText = await dbFile.async('text');
    let parsed: any;
    try {
      parsed = JSON.parse(dbText);
    } catch {
      throw new Error('Database file in archive is corrupted or not valid JSON.');
    }

    const backupClothes: any[] = Array.isArray(parsed.clothes) ? parsed.clothes : [];
    const backupReports: any[] = Array.isArray(parsed.reports) ? parsed.reports : [];

    // Photos count
    const photoCount = allFiles.filter(f => {
      if (zip.files[f].dir || f.endsWith('/')) return false;
      const lower = f.toLowerCase();
      return (
        lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.png') ||
        lower.endsWith('.webp') || lower.endsWith('.gif') || lower.endsWith('.heic') ||
        lower.endsWith('.svg') || lower.endsWith('.bmp') || lower.endsWith('.avif')
      );
    }).length;

    // Field definitions in backup
    let backupDefsCount = 0;
    const backupDefNames = new Set<string>();
    const fieldDefsKey = allFiles.find(f => !zip.files[f].dir && f.endsWith('field_definitions.json'));
    if (fieldDefsKey) {
      try {
        const text = await zip.file(fieldDefsKey)!.async('text');
        const defs = JSON.parse(text);
        if (Array.isArray(defs)) {
          backupDefsCount = defs.length;
          defs.forEach(d => { if (d?.fieldNameEn) backupDefNames.add(d.fieldNameEn.toLowerCase()); });
        }
      } catch {}
    }

    // Backup custom fields and attributes
    const backupClothesFields = new Set<string>();
    const backupReportsFields = new Set<string>();
    backupClothes.forEach(c => {
      if (c.customFields) Object.keys(c.customFields).forEach(k => backupClothesFields.add(k.toLowerCase()));
      if (c.extraAttributes) Object.keys(c.extraAttributes).forEach(k => backupClothesFields.add(k.toLowerCase()));
    });
    backupReports.forEach(r => {
      if (r.customFields) Object.keys(r.customFields).forEach(k => backupReportsFields.add(k.toLowerCase()));
      if (r.extraAttributes) Object.keys(r.extraAttributes).forEach(k => backupReportsFields.add(k.toLowerCase()));
    });

    // Current fields scan
    const currentClothesFieldMap = new Map<string, { count: number; sampleValues: Set<string> }>();
    this.clothes.forEach(c => {
      const customs = { ...(c.customFields || {}), ...(c.extraAttributes || {}) };
      for (const [k, v] of Object.entries(customs)) {
        if (!currentClothesFieldMap.has(k)) {
          currentClothesFieldMap.set(k, { count: 0, sampleValues: new Set() });
        }
        const entry = currentClothesFieldMap.get(k)!;
        entry.count++;
        if (v !== undefined && v !== null && String(v).trim()) {
          entry.sampleValues.add(String(v).slice(0, 40));
        }
      }
    });

    const currentReportsFieldMap = new Map<string, { count: number; sampleValues: Set<string> }>();
    this.reports.forEach(r => {
      const customs = { ...(r.customFields || {}), ...(r.extraAttributes || {}) };
      for (const [k, v] of Object.entries(customs)) {
        if (!currentReportsFieldMap.has(k)) {
          currentReportsFieldMap.set(k, { count: 0, sampleValues: new Set() });
        }
        const entry = currentReportsFieldMap.get(k)!;
        entry.count++;
        if (v !== undefined && v !== null && String(v).trim()) {
          entry.sampleValues.add(String(v).slice(0, 40));
        }
      }
    });

    const allDiffs: BackupFieldDiff[] = [];

    // 1. Clothes custom fields
    currentClothesFieldMap.forEach((info, fieldName) => {
      const inBackup = backupClothesFields.has(fieldName.toLowerCase());
      allDiffs.push({
        fieldName,
        category: 'clothes_custom',
        itemType: 'garment',
        count: info.count,
        sampleValues: Array.from(info.sampleValues).slice(0, 3),
        isInBackup: inBackup,
        isInCurrent: true
      });
    });

    // 2. Reports custom fields
    currentReportsFieldMap.forEach((info, fieldName) => {
      const inBackup = backupReportsFields.has(fieldName.toLowerCase());
      allDiffs.push({
        fieldName,
        category: 'report_custom',
        itemType: 'report',
        count: info.count,
        sampleValues: Array.from(info.sampleValues).slice(0, 3),
        isInBackup: inBackup,
        isInCurrent: true
      });
    });

    // 3. Field definitions present in current dictionary but missing in backup
    const currentDefs = fieldDictionaryService.getDefinitions();
    currentDefs.forEach(d => {
      if (d && d.fieldNameEn) {
        const inBackup = backupDefNames.has(d.fieldNameEn.toLowerCase());
        if (!inBackup && backupDefsCount > 0) {
          allDiffs.push({
            fieldName: `${d.fieldNameEn} (${d.fieldNameCn})`,
            category: 'dictionary',
            itemType: 'field_definition',
            count: d.typicalValues?.length || 0,
            sampleValues: (d.typicalValues || []).slice(0, 3).map(v => v.valueEn),
            isInBackup: false,
            isInCurrent: true
          });
        }
      }
    });

    // Presets in backup
    let backupPresetsCount = 0;
    const presetsKey = allFiles.find(f => !zip.files[f].dir && (f.endsWith('drilldown_presets.json') || f.endsWith('report_presets.json')));
    if (presetsKey) {
      try {
        const text = await zip.file(presetsKey)!.async('text');
        const pList = JSON.parse(text);
        if (Array.isArray(pList)) backupPresetsCount = pList.length;
      } catch {}
    } else if (Array.isArray(parsed?.drilldownPresets)) {
      backupPresetsCount = parsed.drilldownPresets.length;
    }

    const currentPresetsCount = reportPresetService.getPresets().length;

    const newFieldsInCurrent = allDiffs.filter(d => !d.isInBackup);

    return {
      fileName: (fileOrBlob as File).name || 'backup.zip',
      fileSize: fileOrBlob.size,
      backupClothesCount: backupClothes.length,
      backupReportsCount: backupReports.length,
      backupPhotosCount: photoCount,
      backupFieldDefsCount: backupDefsCount,
      backupPresetsCount,
      currentClothesCount: this.clothes.length,
      currentReportsCount: this.reports.length,
      currentFieldDefsCount: currentDefs.length,
      currentPresetsCount,
      newFieldsInCurrent,
      allDetectedFields: allDiffs,
      rawParsedDb: parsed
    };
  }

  /**
   * Restores a full backup archive (.zip) onto the server disk and reloads app data.
   * Protects new fields by default and applies user field resolutions (keep, rename/add as other, delete).
   */
  public async restoreFullBackupZip(
    file: File | Blob,
    onProgress?: (step: string, percent: number) => void,
    resolutions?: RestoreFieldResolutions
  ): Promise<{ clothesCount: number; reportsCount: number; junctionsCount: number; restoredPhotosCount: number; restoredPresetsCount?: number }> {
    return this.restoreBackupFromBlob(file, onProgress, resolutions);
  }

  /**
   * Generates a complete standalone ZIP Blob of the entire database,
   * high-resolution photos, formatted Markdown AI reports, and AI advisor reports.
   */
  public async createFullBackupBlob(
    onProgress?: (step: string, percent: number) => void
  ): Promise<Blob> {
    onProgress?.('Preparing database records...', 5);
    await this.saveToLocalDisk().catch(() => {});

    const JSZip = (await import('jszip')).default;
    const zip = new JSZip();

    const currentPresets = reportPresetService.getPresets();

    // 1. Core Database (Full JSON fidelity)
    const dbPayload = {
      exportedAt: new Date().toISOString(),
      version: '2.0',
      app: 'Wardrobe & Wear Journal',
      totalClothes: this.clothes.length,
      totalReports: this.reports.length,
      totalJunctions: this.junctions.length,
      totalDrilldownPresets: currentPresets.length,
      clothes: this.clothes,
      reports: this.reports,
      junctions: this.junctions,
      drilldownPresets: currentPresets
    };
    zip.file('database.json', JSON.stringify(dbPayload, null, 2));

    // 2. High-Resolution Photos
    onProgress?.('Accessing local image storage (IndexedDB)...', 15);
    const photosFolder = zip.folder('photos');
    let photoCount = 0;

    const { getAllCachedImages } = await import('./localImageCache');
    const cachedImagesMap = await getAllCachedImages();

    // Collect all pictures from garments and reports
    const allPictures: any[] = [];
    this.clothes.forEach(c => (c.Pictures || []).forEach((p: any) => allPictures.push({ parent: c, type: 'garment', pic: p })));
    this.reports.forEach(r => (r.Pictures || []).forEach((p: any) => allPictures.push({ parent: r, type: 'report', pic: p })));

    const processedBlobs = new Set<string>();

    for (let i = 0; i < allPictures.length; i++) {
      const item = allPictures[i];
      const p = item.pic;
      const pct = Math.round(15 + (i / Math.max(1, allPictures.length)) * 35);
      onProgress?.(`Packaging photo ${i + 1} of ${allPictures.length}...`, pct);

      let blob: Blob | null = null;
      let targetPath = '';

      if (p.storagePath) {
        targetPath = p.storagePath.replace(/\\/g, '/').replace(/^\/+/, '');
      } else if (p.filename) {
        targetPath = `${item.type === 'garment' ? 'garments' : 'reports'}/${item.parent.id}/photos/${p.filename}`;
      } else if (p.name) {
        targetPath = `${item.type === 'garment' ? 'garments' : 'reports'}/${item.parent.id}/photos/${p.name}`;
      } else {
        targetPath = `photos/${p.id || `photo_${i}`}.jpeg`;
      }

      if (targetPath.startsWith('uploads/')) {
        targetPath = targetPath.slice('uploads/'.length);
      }
      if (targetPath.startsWith('photos/')) {
        targetPath = targetPath.slice('photos/'.length);
      }

      const candidateKeys = [
        p.storagePath,
        p.url,
        `/uploads/${targetPath}`,
        `uploads/${targetPath}`,
        targetPath,
        p.filename,
        p.name,
        p.id
      ].filter(Boolean);

      for (const k of candidateKeys) {
        if (cachedImagesMap.has(k)) {
          blob = cachedImagesMap.get(k)!;
          break;
        }
      }

      // Fallback: If not in local cache, try fetching
      if (!blob && p.url) {
        try {
          if (p.url.startsWith('data:')) {
            const fetchRes = await fetch(p.url);
            blob = await fetchRes.blob();
          } else if (p.url.startsWith('blob:') || p.url.startsWith('/uploads/') || p.url.startsWith('http')) {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 4000);
            const fetchRes = await fetch(p.url, { signal: controller.signal });
            clearTimeout(timeoutId);
            if (fetchRes.ok) {
              blob = await fetchRes.blob();
            }
          }
        } catch {}
      }

      if (blob && photosFolder) {
        const baseName = targetPath.split('/').pop() || `photo_${i}.jpeg`;
        photosFolder.file(targetPath, blob);
        if (targetPath.includes('/')) {
          photosFolder.file(baseName, blob);
        }
        processedBlobs.add(targetPath);
        processedBlobs.add(baseName);
        photoCount++;
      }
    }

    // Include any additional cached images that were not explicitly linked
    cachedImagesMap.forEach((blob, key) => {
      let cleanKey = key.replace(/\\/g, '/').replace(/^\/+/, '');
      if (cleanKey.startsWith('uploads/')) cleanKey = cleanKey.slice('uploads/'.length);
      if (cleanKey.startsWith('photos/')) cleanKey = cleanKey.slice('photos/'.length);
      const baseName = cleanKey.split('/').pop() || '';

      if (!processedBlobs.has(cleanKey) && !processedBlobs.has(baseName) && photosFolder) {
        photosFolder.file(cleanKey || `cached_${photoCount}.jpeg`, blob);
        processedBlobs.add(cleanKey);
        photoCount++;
      }
    });

    // 3. Formatted Markdown AI Reports
    onProgress?.('Generating Markdown AI diagnostic reports...', 55);
    const aiReportsFolder = zip.folder('ai_reports');
    if (aiReportsFolder) {
      let indexMd = `# Wardrobe Wearing Reports & AI Diagnostics Archive\nExported: ${new Date().toLocaleString()}\nTotal Reports: ${this.reports.length}\n\n`;

      for (let rIdx = 0; rIdx < this.reports.length; rIdx++) {
        const r = this.reports[rIdx];
        const repName = r['Name'] || r.name || 'Wearing Report';
        const repDate = r['wear date'] || r.wearDate || 'Unknown Date';
        const safeDate = String(repDate).slice(0, 10);
        const safeFileName = `${safeDate}_${repName.replace(/[^a-zA-Z0-9_-]/g, '_')}.md`;

        const linkedJunctions = (this.junctions || []).filter((j: any) => {
          const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : String(j['wearing report id']);
          return rId === r.id;
        });

        const linkedClothesDetails = linkedJunctions.map((j: any) => {
          const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : String(j['clothes(new)']);
          const garment = this.clothes.find((c: any) => c.id === cId);
          const durationHrs = j['duration'] ? (j['duration'] / 3600).toFixed(1) : 'Unknown';
          const washStated = j['wash stated'] || 'need wash';
          const washDate = j['wash date'] || j['washed date'] || '(pending)';
          return `- **${garment ? (garment['Name'] || garment.name) : (j.clothesTitle || 'Garment')}**: ${durationHrs}h worn | Status: **${washStated}** (${washDate})`;
        }).join('\n');

        let aiDiagnosticsSection = '';
        if (Array.isArray(r.aiReports) && r.aiReports.length > 0) {
          aiDiagnosticsSection = r.aiReports.map((v: any, idx: number) => {
            const vTitle = v.title || `Report Version ${idx + 1}`;
            const vModel = v.model ? ` [Model: ${v.model}]` : '';
            const vTime = v.timestamp ? ` — ${new Date(v.timestamp).toLocaleString()}` : '';
            const star = v.isPrimary ? ' ⭐ [PRIMARY]' : '';
            return `### ${vTitle}${vModel}${vTime}${star}\n\n${v.content || '(empty)'}\n`;
          }).join('\n---\n\n');
        } else {
          aiDiagnosticsSection = r['AI Wearing Analysis'] || r.aiWearingAnalysis || '*(No AI analysis run on this session yet)*';
        }

        const reportMd = `# ${repName}
- **Wear Date**: ${repDate}
- **Recorded**: ${r['created'] || r.created || 'N/A'}
- **Categories**: ${Array.isArray(r.categories) ? r.categories.join(', ') : (r.categories || 'N/A')}
- **Tags**: ${Array.isArray(r.tags) ? r.tags.join(', ') : (r.tags || 'N/A')}

---

## 1. Sensory & Fit Observations
- **Shoulder Feel**: ${r['shoulder feel'] || r.shoulderFeel || '(unrecorded)'}
- **Mid Breast (Daytime)**: ${r['mid breast'] || r.midBreast || '(unrecorded)'}
- **Night Mid Breast**: ${r['night mid breast'] || r.nightMidBreast || '(unrecorded)'}
- **Sweat (Daytime)**: ${r['sweat'] || r.sweat || '(unrecorded)'}
- **Night Sweat**: ${r['night sweat'] || r.nightSweat || '(unrecorded)'}
- **Leg Feel (Daytime)**: ${r['leg feel'] || r.legFeel || '(unrecorded)'}
- **Night Leg Feel**: ${r['night leg feel'] || r.nightLegFeel || '(unrecorded)'}

## 2. Session Detail Notes
${r['report detail'] || r.reportDetail || 'No freeform details recorded for this session.'}

## 3. Linked Garments in this Outfit
${linkedClothesDetails || 'No linked garment records for this session.'}

## 4. Teable-Style Grounded AI Wearing Diagnostics (Saved Versions)
${aiDiagnosticsSection}
`;
        aiReportsFolder.file(safeFileName, reportMd);
        indexMd += `- [${repName} (${safeDate})](./${safeFileName})\n`;
      }

      aiReportsFolder.file('README_INDEX.md', indexMd);
    }

    // 4. Metadata, AI Advisor Reports, and Field Definitions
    onProgress?.('Packaging AI Advisor reports and field dictionaries...', 75);
    const { fieldDictionaryService } = await import('./fieldDictionaryService');
    const aiAdvisorReports = fieldDictionaryService.getAiReports();
    const fieldDefinitions = fieldDictionaryService.getDefinitions();

    zip.file('ai_advisor_reports.json', JSON.stringify(aiAdvisorReports, null, 2));
    zip.file('field_definitions.json', JSON.stringify(fieldDefinitions, null, 2));
    zip.file('drilldown_presets.json', JSON.stringify(currentPresets, null, 2));

    const metadata = {
      app: 'Wardrobe & Wear Journal',
      backupGeneratedAt: new Date().toISOString(),
      version: '2.0',
      stats: {
        totalGarments: this.clothes.length,
        totalWearingReports: this.reports.length,
        totalJunctions: this.junctions.length,
        totalPhotosInArchive: photoCount,
        totalAiReportsInArchive: this.reports.length,
        totalAdvisorReports: aiAdvisorReports.length,
        totalFieldDefinitions: fieldDefinitions.length,
        totalDrilldownPresets: currentPresets.length
      },
      aiEngine: 'Google AI Studio Gemini API (gemini-3.8-flash)'
    };
    zip.file('metadata.json', JSON.stringify(metadata, null, 2));

    const readmeTxt = `=====================================================
WARDROBE & WEAR JOURNAL - FULL ARCHIVE BACKUP
=====================================================
Exported: ${new Date().toLocaleString()}
Version: 2.0

ARCHIVE CONTENTS:
-----------------
1. database.json
   - Contains your full database of garments, wearing reports,
     wear duration records, and wash dates.

2. ai_advisor_reports.json & field_definitions.json
   - Contains all independent AI Advisor reports, custom definitions,
     and bilingual field-value dictionaries.

3. photos/
   - High-resolution, uncompressed original images for your clothes
     and wearing reports.

4. ai_reports/
   - Formatted Markdown (.md) documents for EVERY wearing report,
     including all sensory notes (shoulder feel, chest, sweat, leg feel)
     and Teable-style grounded AI Diagnostics.
   - Usable in Obsidian, Notion, Logseq, or any markdown reader!

5. metadata.json
   - Summary statistics of this archive.

HOW TO RESTORE:
---------------
Open the app -> Go to "Sync & Backup" -> Click "Restore from Full Backup (.ZIP)"
and select this file. All your clothes, photos, definitions, and AI reports will be restored!
=====================================================`;
    zip.file('README.txt', readmeTxt);

    onProgress?.('Compressing complete ZIP archive...', 85);
    const zipBlob = await zip.generateAsync({
      type: 'blob',
      compression: 'DEFLATE',
      compressionOptions: { level: 6 }
    });

    onProgress?.('Archive ready!', 100);
    return zipBlob;
  }

  /**
   * Restores database and pictures from a ZIP Blob.
   */
  public async restoreBackupFromBlob(
    blob: Blob,
    onProgress?: (step: string, percent: number) => void,
    resolutions?: RestoreFieldResolutions
  ): Promise<{ clothesCount: number; reportsCount: number; junctionsCount: number; restoredPhotosCount: number; restoredPresetsCount?: number }> {
    onProgress?.('Reading backup archive...', 15);
    const JSZip = (await import('jszip')).default;
    const zip = await JSZip.loadAsync(blob);

    // 1. Locate database.json anywhere in archive (supports root, nested folder, initialData.json, local_db.json)
    const allFiles = Object.keys(zip.files);
    const dbEntryKey = allFiles.find(
      f => !zip.files[f].dir && (
        f === 'database.json' ||
        f.endsWith('/database.json') ||
        f === 'initialData.json' ||
        f.endsWith('/initialData.json') ||
        f === 'local_db.json' ||
        f.endsWith('/local_db.json')
      )
    );

    if (!dbEntryKey) {
      throw new Error('Invalid backup archive: missing database.json inside archive.');
    }

    onProgress?.('Extracting database records...', 30);
    const dbFile = zip.file(dbEntryKey);
    if (!dbFile) {
      throw new Error('Could not read database.json from backup archive.');
    }

    const dbText = await dbFile.async('text');
    let parsed: any;
    try {
      parsed = JSON.parse(dbText);
    } catch (e) {
      throw new Error('Database file inside archive is corrupted or not valid JSON.');
    }

    const rawClothes = Array.isArray(parsed.clothes) ? parsed.clothes : [];
    const rawReports = Array.isArray(parsed.reports) ? parsed.reports : [];
    const rawJunctions = Array.isArray(parsed.junctions)
      ? parsed.junctions
      : (Array.isArray(parsed.reportClothes) ? parsed.reportClothes : []);

    if (rawClothes.length === 0 && rawReports.length === 0) {
      throw new Error('Archive contains no garments or wearing reports.');
    }

    const strategy = resolutions?.strategy || 'smart_merge';
    const fieldActions = resolutions?.fieldActions || {};
    const fieldRenames = resolutions?.fieldRenames || {};
    const additionalFields = resolutions?.additionalFieldsToAdd || [];

    // --- Safe Clothes Merge with Field Protection ---
    const currentClothesMap = new Map<string, ClothesItem>(this.clothes.map(c => [c.id, c]));
    const mergedClothes: ClothesItem[] = [];
    const restoredClothesIds = new Set<string>();

    for (const rawC of rawClothes) {
      const c = normalizeGarment(rawC);
      restoredClothesIds.add(c.id);
      const existing = currentClothesMap.get(c.id);

      if (existing) {
        // Merge custom fields from existing item
        const existingCustom = { ...(existing.customFields || {}), ...(existing.extraAttributes || {}) };
        const mergedCustom = { ...(c.customFields || {}), ...(c.extraAttributes || {}) };

        for (const [key, val] of Object.entries(existingCustom)) {
          const action = fieldActions[key] || 'keep'; // Default keep: "the new field no going to delete"
          if (action === 'delete') {
            delete mergedCustom[key];
          } else if (action === 'rename') {
            const newKey = (fieldRenames[key] || key).trim();
            if (newKey) mergedCustom[newKey] = val;
          } else {
            // Keep
            if (val !== undefined && val !== null && String(val).trim() !== '') {
              if (mergedCustom[key] === undefined || mergedCustom[key] === null || String(mergedCustom[key]).trim() === '') {
                mergedCustom[key] = val;
              }
            }
          }
        }
        c.customFields = mergedCustom;
        c.extraAttributes = mergedCustom;

        // Also ensure every dynamic custom attribute in mergedCustom is accessible as a top-level property on c
        for (const [mk, mv] of Object.entries(mergedCustom)) {
          if ((c as any)[mk] === undefined || (c as any)[mk] === null || String((c as any)[mk]).trim() === '') {
            (c as any)[mk] = mv;
          }
        }

        // Preserve and merge fieldExplanations (e.g. climate rationale, fit notes)
        if (existing.fieldExplanations && typeof existing.fieldExplanations === 'object') {
          c.fieldExplanations = {
            ...existing.fieldExplanations,
            ...(c.fieldExplanations || {})
          };
        }

        // Safe Field Preservation: NEVER overwrite non-empty existing data with empty strings/blank data!
        for (const [k, v] of Object.entries(existing)) {
          if (v !== undefined && v !== null && String(v).trim() !== '') {
            const action = fieldActions[k] || 'keep';
            if (action === 'delete') continue;
            const targetKey = action === 'rename' ? (fieldRenames[k] || k).trim() : k;
            if (!targetKey) continue;

            const incomingVal = (c as any)[targetKey];
            // If incoming has no value or an empty string, preserve existing value!
            if (incomingVal === undefined || incomingVal === null || String(incomingVal).trim() === '') {
              (c as any)[targetKey] = v;
            }
          }
        }
      }

      // Attach any user-added custom fields (e.g. aa2, aa3)
      for (const af of additionalFields) {
        if (af.name && (af.target === 'clothes' || af.target === 'both')) {
          if (!c.customFields) c.customFields = {};
          c.customFields[af.name] = af.defaultValue ?? '';
          if (!c.extraAttributes) c.extraAttributes = {};
          c.extraAttributes[af.name] = af.defaultValue ?? '';
        }
      }

      mergedClothes.push(c);
    }

    // In smart_merge, also preserve any newer clothes created after backup
    if (strategy === 'smart_merge') {
      for (const existing of this.clothes) {
        if (!restoredClothesIds.has(existing.id)) {
          mergedClothes.push(existing);
        }
      }
    }
    this.clothes = mergedClothes;

    // --- Safe Reports Merge with Field Protection ---
    const currentReportsMap = new Map<string, WearingReport>(this.reports.map(r => [r.id, r]));
    const mergedReports: WearingReport[] = [];
    const restoredReportIds = new Set<string>();

    for (const rawR of rawReports) {
      const r = normalizeReport(rawR);
      restoredReportIds.add(r.id);
      const existing = currentReportsMap.get(r.id);

      if (existing) {
        const existingCustom = { ...(existing.customFields || {}), ...(existing.extraAttributes || {}) };
        const mergedCustom = { ...(r.customFields || {}), ...(r.extraAttributes || {}) };

        for (const [key, val] of Object.entries(existingCustom)) {
          const action = fieldActions[key] || 'keep'; // Default keep
          if (action === 'delete') {
            delete mergedCustom[key];
          } else if (action === 'rename') {
            const newKey = (fieldRenames[key] || key).trim();
            if (newKey) mergedCustom[newKey] = val;
          } else {
            if (val !== undefined && val !== null && String(val).trim() !== '') {
              if (mergedCustom[key] === undefined || mergedCustom[key] === null || String(mergedCustom[key]).trim() === '') {
                mergedCustom[key] = val;
              }
            }
          }
        }
        r.customFields = mergedCustom;
        r.extraAttributes = mergedCustom;

        // Also ensure every dynamic custom attribute in mergedCustom is accessible as a top-level property on r
        for (const [mk, mv] of Object.entries(mergedCustom)) {
          if ((r as any)[mk] === undefined || (r as any)[mk] === null || String((r as any)[mk]).trim() === '') {
            (r as any)[mk] = mv;
          }
        }

        // Preserve and merge fieldExplanations for reports
        if (existing.fieldExplanations && typeof existing.fieldExplanations === 'object') {
          r.fieldExplanations = {
            ...existing.fieldExplanations,
            ...(r.fieldExplanations || {})
          };
        }

        // Safe Field Preservation for Reports: NEVER overwrite non-empty existing data with empty strings!
        for (const [k, v] of Object.entries(existing)) {
          if (v !== undefined && v !== null && String(v).trim() !== '') {
            const action = fieldActions[k] || 'keep';
            if (action === 'delete') continue;
            const targetKey = action === 'rename' ? (fieldRenames[k] || k).trim() : k;
            if (!targetKey) continue;

            const incomingVal = (r as any)[targetKey];
            if (incomingVal === undefined || incomingVal === null || String(incomingVal).trim() === '') {
              (r as any)[targetKey] = v;
            }
          }
        }
      }

      for (const af of additionalFields) {
        if (af.name && (af.target === 'reports' || af.target === 'both')) {
          if (!r.customFields) r.customFields = {};
          r.customFields[af.name] = af.defaultValue ?? '';
          if (!r.extraAttributes) r.extraAttributes = {};
          r.extraAttributes[af.name] = af.defaultValue ?? '';
        }
      }

      mergedReports.push(r);
    }

    if (strategy === 'smart_merge') {
      for (const existing of this.reports) {
        if (!restoredReportIds.has(existing.id)) {
          mergedReports.push(existing);
        }
      }
    }
    this.reports = sortReportsDescending(mergedReports);

    // --- Junctions Merge ---
    const mergedJunctions: ReportClothesJunction[] = [];
    const restoredJunctionIds = new Set<string>();
    for (const rawJ of rawJunctions) {
      const j = normalizeJunction(rawJ);
      restoredJunctionIds.add(j.id);
      mergedJunctions.push(j);
    }
    if (strategy === 'smart_merge') {
      for (const existing of this.junctions) {
        if (!restoredJunctionIds.has(existing.id)) {
          mergedJunctions.push(existing);
        }
      }
    }
    this.junctions = mergedJunctions;

    // 2. Extract photos directly into local IndexedDB for immediate 0ms rendering
    onProgress?.('Extracting and caching photos...', 40);
    let restoredPhotos = 0;
    const { cacheManyImageBlobs } = await import('./localImageCache');
    const photoFiles = allFiles.filter(f => {
      if (zip.files[f].dir || f.endsWith('/')) return false;
      const lower = f.toLowerCase();
      if (
        lower.endsWith('.json') ||
        lower.endsWith('.md') ||
        lower.endsWith('.txt') ||
        lower.includes('.ds_store')
      ) {
        return false;
      }
      return (
        lower.endsWith('.jpg') ||
        lower.endsWith('.jpeg') ||
        lower.endsWith('.png') ||
        lower.endsWith('.webp') ||
        lower.endsWith('.gif') ||
        lower.endsWith('.heic') ||
        lower.endsWith('.svg') ||
        lower.endsWith('.bmp') ||
        lower.endsWith('.avif')
      );
    });

    const allPictures: any[] = [];
    this.clothes.forEach(c => (c.Pictures || []).forEach((p: any) => allPictures.push(p)));
    this.reports.forEach(r => (r.Pictures || []).forEach((p: any) => allPictures.push(p)));

    // Extract photos in batches to keep UI responsive and optimize IndexedDB write transactions
    const BATCH_SIZE = 25;
    for (let i = 0; i < photoFiles.length; i += BATCH_SIZE) {
      const chunk = photoFiles.slice(i, i + BATCH_SIZE);
      const batchEntries: { key: string; blob: Blob }[] = [];

      for (const pPath of chunk) {
        const fileObj = zip.file(pPath);
        if (!fileObj) continue;

        try {
          const pBlob = await fileObj.async('blob');
          let cleanRelative = pPath.replace(/\\/g, '/').replace(/^\/+/, '');
          if (cleanRelative.startsWith('uploads/')) {
            cleanRelative = cleanRelative.slice('uploads/'.length);
          } else if (cleanRelative.startsWith('photos/')) {
            cleanRelative = cleanRelative.slice('photos/'.length);
          }
          while (cleanRelative.startsWith('photos/')) {
            cleanRelative = cleanRelative.slice('photos/'.length);
          }

          const baseName = cleanRelative.split('/').pop() || '';
          const strippedBaseName = baseName.replace(/^\d+_/, '');

          // Add primary keys to batch
          batchEntries.push({ key: pPath, blob: pBlob });
          batchEntries.push({ key: cleanRelative, blob: pBlob });
          batchEntries.push({ key: `/uploads/${cleanRelative}`, blob: pBlob });
          if (baseName) {
            batchEntries.push({ key: baseName, blob: pBlob });
            batchEntries.push({ key: `/uploads/${baseName}`, blob: pBlob });
          }

          // Match with any picture record in database
          const matched = allPictures.filter((p: any) => {
            if (p.storagePath) {
              const spClean = p.storagePath.replace(/\\/g, '/').replace(/^\/+/, '');
              if (spClean === cleanRelative || spClean.endsWith(baseName) || (strippedBaseName && spClean.endsWith(strippedBaseName))) return true;
            }
            if (p.url) {
              const urlClean = p.url.replace(/\\/g, '/');
              if (urlClean.includes(cleanRelative) || urlClean.endsWith(baseName) || (strippedBaseName && urlClean.endsWith(strippedBaseName))) return true;
            }
            if (p.filename && (p.filename === baseName || p.filename === strippedBaseName)) return true;
            if (p.name && (p.name === baseName || p.name === strippedBaseName)) return true;
            return false;
          });

          for (const m of matched) {
            if (m.id) batchEntries.push({ key: m.id, blob: pBlob });
            if (m.storagePath) {
              const sp = m.storagePath.replace(/^\/+/, '').replace(/^uploads\//, '');
              batchEntries.push({ key: sp, blob: pBlob });
              batchEntries.push({ key: `/uploads/${sp}`, blob: pBlob });
            }
            if (m.filename) batchEntries.push({ key: m.filename, blob: pBlob });
            if (m.url && !m.url.startsWith('blob:')) batchEntries.push({ key: m.url, blob: pBlob });

            // Heal dead blob or missing url
            if (!m.url || m.url.startsWith('blob:')) {
              m.storagePath = m.storagePath || cleanRelative;
              m.url = `/uploads/${m.storagePath.replace(/^\/+/, '').replace(/^uploads\//, '')}`;
            }
          }

          restoredPhotos++;
        } catch (err) {
          console.warn('Photo extraction warning:', pPath, err);
        }
      }

      if (batchEntries.length > 0) {
        await cacheManyImageBlobs(batchEntries);
      }

      const percent = Math.min(75, 40 + Math.round(((i + chunk.length) / photoFiles.length) * 35));
      onProgress?.(`Caching photos (${Math.min(i + chunk.length, photoFiles.length)} / ${photoFiles.length})...`, percent);
    }

    notifyImageCacheListeners();

    // Secondary pass to heal any remaining picture records
    for (const p of allPictures) {
      if (!p.url || p.url.startsWith('blob:')) {
        if (p.storagePath) {
          p.url = `/uploads/${p.storagePath.replace(/^\/+/, '').replace(/^uploads\//, '')}`;
        }
      }
    }

    // 3. Extract field_definitions.json and ai_advisor_reports.json if present
    const fieldDefsKey = allFiles.find(f => !zip.files[f].dir && f.endsWith('field_definitions.json'));
    if (fieldDefsKey) {
      try {
        const fieldDefsText = await zip.file(fieldDefsKey)!.async('text');
        const parsedDefs = JSON.parse(fieldDefsText);
        if (Array.isArray(parsedDefs)) {
          fieldDictionaryService.importDefinitions(parsedDefs);
        }
      } catch (err) {
        console.warn('Field definitions restore warning:', err);
      }
    }

    const aiReportsKey = allFiles.find(f => !zip.files[f].dir && f.endsWith('ai_advisor_reports.json'));
    if (aiReportsKey) {
      try {
        const aiReportsText = await zip.file(aiReportsKey)!.async('text');
        const parsedAiReports = JSON.parse(aiReportsText);
        if (Array.isArray(parsedAiReports)) {
          fieldDictionaryService.importAiReports(parsedAiReports);
        }
      } catch (err) {
        console.warn('AI advisor reports restore warning:', err);
      }
    }

    // 4. Extract and restore drilldown_presets.json if present
    let restoredPresetsCount = 0;
    const presetsKey = allFiles.find(f => !zip.files[f].dir && (f.endsWith('drilldown_presets.json') || f.endsWith('report_presets.json')));
    let backupPresetsToImport: any[] = [];
    if (presetsKey) {
      try {
        const presetsText = await zip.file(presetsKey)!.async('text');
        const parsedPresets = JSON.parse(presetsText);
        if (Array.isArray(parsedPresets)) backupPresetsToImport = parsedPresets;
      } catch (err) {
        console.warn('Drilldown presets restore warning:', err);
      }
    } else if (Array.isArray(parsed?.drilldownPresets)) {
      backupPresetsToImport = parsed.drilldownPresets;
    }

    if (backupPresetsToImport.length > 0) {
      try {
        const merged = reportPresetService.importPresets(backupPresetsToImport, true, true);
        restoredPresetsCount = merged.length;
      } catch (presetErr) {
        console.warn('[Restore] Failed to import presets:', presetErr);
      }
    }

    // 5. Update local storage timestamp and records
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('wardrobe_db_updated_at', String(Date.now()));
    }
    this.persistLocalOnly();
    this.notify();
    await this.saveToLocalDisk().catch(() => {});

    // 6. Persist archive to local server filesystem (/uploads & /data)
    onProgress?.('Saving archive to local server filesystem...', 82);
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 60000); // 60s timeout

      const res = await fetch('/api/backup/restore-zip', {
        method: 'POST',
        headers: { 'Content-Type': 'application/octet-stream' },
        body: blob,
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        onProgress?.('Local server filesystem synchronized!', 96);
      } else {
        const errJson = await res.json().catch(() => ({}));
        console.warn('[Restore] Server restore-zip returned error:', res.status, errJson);
        // Fallback: save JSON representation directly to local disk
        await this.saveToLocalDisk();
      }
    } catch (uploadErr) {
      console.warn('[Restore] Server upload completed/timed out, persisting to local disk:', uploadErr);
      await this.saveToLocalDisk().catch(() => {});
    }

    onProgress?.('Restore completed successfully!', 100);

    return {
      clothesCount: this.clothes.length,
      reportsCount: this.reports.length,
      junctionsCount: this.junctions.length,
      restoredPhotosCount: restoredPhotos,
      restoredPresetsCount
    };
  }

  private persistLocal() {
    this.persistLocalOnly();

    // Debounce background write to local server disk (/api/local-db)
    if (this.saveDebounceTimer) {
      clearTimeout(this.saveDebounceTimer);
    }
    this.saveDebounceTimer = setTimeout(() => {
      this.saveToLocalDisk().catch(() => {});
    }, 300);
  }

  public subscribe(fn: () => void) {
    this.listeners.push(fn);
    return () => {
      this.listeners = this.listeners.filter(l => l !== fn);
    };
  }

  private notify() {
    this.listeners.forEach(fn => fn());
  }

  public getSyncStatus(): {
    status: SyncStatus;
    lastSync: string | null;
    pendingCount: number;
    pendingOutbox: OutboxMutation[];
    isOnline: boolean;
    lastError: string | null;
  } {
    const outboxState = syncOutbox.getState();
    return {
      status: outboxState.status,
      lastSync: outboxState.lastSyncTime || this.lastSyncTime,
      pendingCount: outboxState.pendingCount,
      pendingOutbox: outboxState.mutations,
      isOnline: outboxState.isOnline,
      lastError: outboxState.lastError
    };
  }

  public async flushPendingSync(): Promise<void> {
    await syncOutbox.flush();
  }

  public getOutboxState(): OutboxState {
    return syncOutbox.getState();
  }

  public getGarments(): Clothes[] {
    return this.clothes;
  }

  public getGarment(id: string): Clothes | undefined {
    return this.clothes.find(c => c.id === id);
  }


  /**
   * Local storage sync: Saves all current wardrobe data to the local disk file.
   */
  public async syncWithFirestore(): Promise<void> {
    await this.saveToLocalDisk();
  }

  /**
   * Reload from Local Disk: Reads fresh data from local database file and updates state.
   */
  public async pullFromCloud(
    onProgress?: (step: string, percent: number) => void
  ): Promise<{ clothes: number; reports: number; junctions: number }> {
    this.syncStatus = 'syncing';
    this.notify();

    onProgress?.('Reading local wardrobe database...', 30);
    const result = await this.pullFromLocalDisk();
    onProgress?.('Loaded local database!', 100);
    this.syncStatus = 'synced';
    this.notify();
    return result;
  }

  /**
   * Updates an individual junction's wash date directly and syncs to Firestore.
   */
  public async updateJunctionWashDate(junctionId: string, washDate: string): Promise<ReportClothesJunction | null> {
    let updated: ReportClothesJunction | null = null;
    const cleanWashDate = washDate ? toDateKey(washDate) || washDate : '';
    const newWashStated: 'wash' | 'need wash' = cleanWashDate ? 'wash' : 'need wash';
    this.junctions = this.junctions.map(j => {
      if (j.id === junctionId) {
        updated = {
          ...j,
          'wash date': cleanWashDate || undefined,
          'washed date': cleanWashDate || undefined,
          washDate: cleanWashDate || undefined,
          washedDate: cleanWashDate || undefined,
          'wash stated': newWashStated,
          washStated: newWashStated
        };
        return updated;
      }
      return j;
    });

    if (updated) {
      const cId = typeof (updated as any)['clothes(new)'] === 'object' ? (updated as any)['clothes(new)'].id : String((updated as any)['clothes(new)']);
      if (cId) {
        this.recalculateGarmentWashStats(cId);
      }
      this.persistLocal();
      this.notify();

      syncOutbox.enqueue(
        'clothesJunction',
        junctionId,
        'upsert',
        {
          'wash date': cleanWashDate || null,
          'washed date': cleanWashDate || null,
          'washDate': cleanWashDate || null,
          'wash stated': newWashStated
        },
        `Update wash date for junction ${junctionId}`
      );
    }
    return updated;
  }

  public updateClothesJunctionWashDate(junctionId: string, washDateStr: string | null): Promise<ClothesJunction | null> {
    return this.updateJunctionWashDate(junctionId, washDateStr || '');
  }

  /**
   * Updates any fields of a junction directly and syncs with Firestore.
   */
  public async updateJunction(junctionId: string, partial: Partial<ReportClothesJunction>): Promise<ReportClothesJunction | null> {
    let updated: ReportClothesJunction | null = null;
    this.junctions = this.junctions.map(j => {
      if (j.id === junctionId) {
        updated = normalizeJunction({ ...j, ...partial });
        return updated;
      }
      return j;
    });

    if (updated) {
      const cId = typeof (updated as any)['clothes(new)'] === 'object' ? (updated as any)['clothes(new)'].id : String((updated as any)['clothes(new)']);
      if (cId) {
        this.recalculateGarmentWashStats(cId);
      }
      this.persistLocal();
      this.notify();

      syncOutbox.enqueue(
        'clothesJunction',
        junctionId,
        'upsert',
        updated,
        `Update junction ${junctionId}`
      );
    }
    return updated;
  }

  public updateClothesJunction(junctionId: string, partial: Partial<ClothesJunction>): Promise<ClothesJunction | null> {
    return this.updateJunction(junctionId, partial);
  }

  /**
   * Explicit Save to Local Disk: Saves all local clothes, wearing reports, and junctions to local database file.
   */
  public async pushToCloud(
    onProgress?: (step: string, percent: number) => void
  ): Promise<{ clothes: number; reports: number; junctions: number }> {
    return await this.saveToLocalDisk(onProgress);
  }

  /**
   * Scans all junctions and merges duplicates within each wearing report session.
   * Rule: In any single wearing report, a garment (clothesId) can appear AT MOST ONCE.
   * Across the same day, multiple separate wearing reports (sessions) are valid and preserved.
   * When duplicates are found (e.g. June 24 report having two 'Blue Old Bra' or two 'Pink Flower RM30'):
   * 1. Preserves the record with a valid wash date (never lose wash dates!).
   * 2. Preserves the highest duration or most recent record.
   * 3. Deletes redundant orphan duplicate records from local storage.
   */
  public async deduplicateJunctions(): Promise<{ beforeCount: number; afterCount: number; removedCount: number }> {
    const beforeCount = this.junctions.length;
    const sessionMap = new Map<string, ReportClothesJunction[]>();

    this.junctions.forEach(j => {
      const repId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      const key = `${repId}___${cId}`;
      if (!sessionMap.has(key)) {
        sessionMap.set(key, []);
      }
      sessionMap.get(key)!.push(j);
    });

    const keptJunctions: ReportClothesJunction[] = [];
    const removedJunctions: ReportClothesJunction[] = [];

    sessionMap.forEach((duplicates) => {
      if (duplicates.length === 1) {
        keptJunctions.push(duplicates[0]);
      } else {
        duplicates.sort((a, b) => {
          const aWashDate = a['wash date'] || a['washed date'] || a.washDate;
          const bWashDate = b['wash date'] || b['washed date'] || b.washDate;
          if (aWashDate && !bWashDate) return -1;
          if (!aWashDate && bWashDate) return 1;

          if (a['wash stated'] === 'wash' && b['wash stated'] !== 'wash') return -1;
          if (b['wash stated'] === 'wash' && a['wash stated'] !== 'wash') return 1;

          const aDur = a['duration'] || a.durationVal || 0;
          const bDur = b['duration'] || b.durationVal || 0;
          return bDur - aDur;
        });

        const winner = duplicates[0];
        keptJunctions.push(winner);
        for (let i = 1; i < duplicates.length; i++) {
          removedJunctions.push(duplicates[i]);
        }
      }
    });

    this.junctions = keptJunctions;
    this.persistLocal();
    this.notify();

    return {
      beforeCount,
      afterCount: this.junctions.length,
      removedCount: removedJunctions.length
    };
  }

  /**
   * Identifies duplicate wearing reports in the database:
   * Reports are uniquely identified by their record ID (Airtable primary key).
   * Reports with different record IDs represent separate journal entries and must NEVER be deleted.
   * Only returns duplicates if duplicate array rows with the exact same record ID exist.
   */
  public findDuplicateReports(): Array<{
    canonical: WearingReport;
    copies: WearingReport[];
    reason: string;
  }> {
    const seenIds = new Map<string, WearingReport>();
    const dupes: WearingReport[] = [];

    this.reports.forEach(r => {
      if (seenIds.has(r.id)) {
        dupes.push(r);
      } else {
        seenIds.set(r.id, r);
      }
    });

    if (dupes.length === 0) return [];

    return [{
      canonical: seenIds.get(dupes[0].id) || dupes[0],
      copies: dupes,
      reason: `${dupes.length} duplicate report rows with identical IDs found`
    }];
  }

  /**
   * Deduplicates wearing report array strictly by unique record ID.
   * Never merges or purges reports with distinct IDs.
   */
  public async deduplicateWearingReports(): Promise<{
    beforeCount: number;
    afterCount: number;
    removedReportsCount: number;
    mergedGroupsCount: number;
    details: string[];
  }> {
    const beforeCount = this.reports.length;
    const uniqueMap = new Map<string, WearingReport>();
    this.reports.forEach(r => {
      if (r && r.id && !uniqueMap.has(r.id)) {
        uniqueMap.set(r.id, r);
      }
    });
    this.reports = Array.from(uniqueMap.values());
    const afterCount = this.reports.length;
    const removedReportsCount = beforeCount - afterCount;

    this.persistLocal();
    this.notify();

    return {
      beforeCount,
      afterCount,
      removedReportsCount,
      mergedGroupsCount: removedReportsCount > 0 ? 1 : 0,
      details: removedReportsCount > 0 ? [`Removed ${removedReportsCount} duplicate rows sharing the exact same report ID.`] : []
    };
  }

  /**
   * Complete deduplication of both wearing reports and garments within sessions.
   */
  public async deduplicateAll(): Promise<{
    reports: { beforeCount: number; afterCount: number; removedReportsCount: number };
    junctions: { beforeCount: number; afterCount: number; removedCount: number };
  }> {
    const repRes = await this.deduplicateWearingReports();
    const juncRes = await this.deduplicateJunctions();
    return {
      reports: repRes,
      junctions: juncRes
    };
  }

  /**
   * Adds or updates a dynamic custom field on a garment.
   */
  public async setGarmentCustomField(clothesId: string, fieldName: string, fieldValue: any): Promise<void> {
    const garment = this.clothes.find(c => c.id === clothesId);
    if (!garment) return;

    const currentCustom = { ...(garment.customFields || garment.extraAttributes || {}) };
    currentCustom[fieldName] = fieldValue;

    await this.updateGarment(clothesId, {
      customFields: currentCustom,
      extraAttributes: currentCustom,
      [fieldName]: fieldValue
    });
  }

  /**
   * Removes a dynamic custom field from a garment.
   */
  public async removeGarmentCustomField(clothesId: string, fieldName: string): Promise<void> {
    const garment = this.clothes.find(c => c.id === clothesId);
    if (!garment) return;

    const currentCustom = { ...(garment.customFields || garment.extraAttributes || {}) };
    delete currentCustom[fieldName];

    await this.updateGarment(clothesId, {
      customFields: currentCustom,
      extraAttributes: currentCustom
    });
  }

  /**
   * Adds or updates a dynamic custom field on a wearing report.
   */
  public async setReportCustomField(reportId: string, fieldName: string, fieldValue: any): Promise<void> {
    const report = this.reports.find(r => r.id === reportId);
    if (!report) return;

    const currentCustom = { ...(report.customFields || report.extraAttributes || {}) };
    currentCustom[fieldName] = fieldValue;

    await this.updateWearingReport(reportId, {
      customFields: currentCustom,
      extraAttributes: currentCustom,
      [fieldName]: fieldValue
    });
  }

  /**
   * Removes a dynamic custom field from a wearing report.
   */
  public async removeReportCustomField(reportId: string, fieldName: string): Promise<void> {
    const report = this.reports.find(r => r.id === reportId);
    if (!report) return;

    const currentCustom = { ...(report.customFields || report.extraAttributes || {}) };
    delete currentCustom[fieldName];

    await this.updateWearingReport(reportId, {
      customFields: currentCustom,
      extraAttributes: currentCustom
    });
  }

  /**
   * Deletes a single junction record by its unique junction ID.
   */
  public async deleteJunction(junctionId: string): Promise<void> {
    this.junctions = this.junctions.filter(j => j.id !== junctionId);
    this.persistLocal();
    this.notify();

    syncOutbox.enqueue(
      'clothesJunction',
      junctionId,
      'delete',
      undefined,
      `Delete junction ${junctionId}`
    );
  }

  public deleteClothesJunction(junctionId: string): Promise<void> {
    return this.deleteJunction(junctionId);
  }

  /**
   * Generates full JSON backup of all tables (clothes, reports, reportClothes/junctions)
   * and triggers an immediate download in the browser as 'initialData.json' or custom filename.
   */
  public exportCompleteDatabaseJson(filename = 'initialData.json'): {
    clothesCount: number;
    reportsCount: number;
    junctionsCount: number;
    presetsCount?: number;
    jsonString: string;
  } {
    const presets = reportPresetService.getPresets();
    const exportPayload = {
      clothes: this.clothes,
      reports: this.reports,
      reportClothes: this.junctions,
      drilldownPresets: presets,
      exportedAt: new Date().toISOString(),
      recordCounts: {
        clothes: this.clothes.length,
        reports: this.reports.length,
        reportClothes: this.junctions.length,
        drilldownPresets: presets.length
      }
    };

    const jsonString = JSON.stringify(exportPayload, null, 2);

    try {
      if (typeof window !== 'undefined' && typeof document !== 'undefined') {
        const blob = new Blob([jsonString], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = filename;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(url);
      }
    } catch (err) {
      console.warn('Browser download trigger error:', err);
    }

    return {
      clothesCount: this.clothes.length,
      reportsCount: this.reports.length,
      junctionsCount: this.junctions.length,
      presetsCount: presets.length,
      jsonString
    };
  }

  /**
   * Imports and merges or replaces local records from an uploaded JSON backup file.
   */
  public async importDatabaseFromJson(jsonContent: string): Promise<{ clothes: number; reports: number; junctions: number; presets?: number }> {
    const parsed = JSON.parse(jsonContent);
    const rawClothes = parsed.clothes || [];
    const rawReports = parsed.reports || [];
    const rawJunctions = parsed.reportClothes || parsed.junctions || parsed.report_clothes_junctions || [];

    if (!Array.isArray(rawClothes) || !Array.isArray(rawReports)) {
      throw new Error('Invalid backup file format: missing clothes or reports arrays.');
    }

    // Safe Merge strategy: update with backup items without dropping any newer un-backed-up records,
    // and protect existing custom attributes and non-empty values from ever being overwritten with empty/blank strings!
    const currentClothesMap = new Map<string, ClothesItem>(this.clothes.map(c => [c.id, c]));
    const mergedClothes: ClothesItem[] = [];
    const restoredClothesIds = new Set<string>();

    for (const rawC of rawClothes) {
      const c = normalizeGarment(rawC);
      restoredClothesIds.add(c.id);
      const existing = currentClothesMap.get(c.id);

      if (existing) {
        // Merge custom attributes and extraAttributes
        const existingCustom = { ...(existing.customFields || {}), ...(existing.extraAttributes || {}) };
        const incomingCustom = { ...(c.customFields || {}), ...(c.extraAttributes || {}) };
        const mergedCustom = { ...existingCustom, ...incomingCustom };

        for (const [key, val] of Object.entries(existingCustom)) {
          if (val !== undefined && val !== null && String(val).trim() !== '') {
            if (mergedCustom[key] === undefined || mergedCustom[key] === null || String(mergedCustom[key]).trim() === '') {
              mergedCustom[key] = val;
            }
          }
        }
        c.customFields = mergedCustom;
        c.extraAttributes = mergedCustom;

        // Mirror all merged custom attributes to top-level properties
        for (const [mk, mv] of Object.entries(mergedCustom)) {
          if ((c as any)[mk] === undefined || (c as any)[mk] === null || String((c as any)[mk]).trim() === '') {
            (c as any)[mk] = mv;
          }
        }

        // Preserve field explanations (fit notes, climate rationale)
        if (existing.fieldExplanations && typeof existing.fieldExplanations === 'object') {
          c.fieldExplanations = {
            ...existing.fieldExplanations,
            ...(c.fieldExplanations || {})
          };
        }

        // Safe Field Preservation: NEVER overwrite non-empty existing data with empty strings/blank data!
        for (const [k, v] of Object.entries(existing)) {
          if (v !== undefined && v !== null && String(v).trim() !== '') {
            const incomingVal = (c as any)[k];
            if (incomingVal === undefined || incomingVal === null || String(incomingVal).trim() === '') {
              (c as any)[k] = v;
            }
          }
        }
      }

      mergedClothes.push(c);
    }

    // Preserve any newer clothes in the current app not present in the backup
    for (const existing of this.clothes) {
      if (!restoredClothesIds.has(existing.id)) {
        mergedClothes.push(existing);
      }
    }
    this.clothes = mergedClothes;

    // Safe Reports Merge
    const currentReportsMap = new Map<string, WearingReport>(this.reports.map(r => [r.id, r]));
    const mergedReports: WearingReport[] = [];
    const restoredReportIds = new Set<string>();

    for (const rawR of rawReports) {
      const r = normalizeReport(rawR);
      restoredReportIds.add(r.id);
      const existing = currentReportsMap.get(r.id);

      if (existing) {
        const existingCustom = { ...(existing.customFields || {}), ...(existing.extraAttributes || {}) };
        const incomingCustom = { ...(r.customFields || {}), ...(r.extraAttributes || {}) };
        const mergedCustom = { ...existingCustom, ...incomingCustom };

        for (const [key, val] of Object.entries(existingCustom)) {
          if (val !== undefined && val !== null && String(val).trim() !== '') {
            if (mergedCustom[key] === undefined || mergedCustom[key] === null || String(mergedCustom[key]).trim() === '') {
              mergedCustom[key] = val;
            }
          }
        }
        r.customFields = mergedCustom;
        r.extraAttributes = mergedCustom;

        for (const [mk, mv] of Object.entries(mergedCustom)) {
          if ((r as any)[mk] === undefined || (r as any)[mk] === null || String((r as any)[mk]).trim() === '') {
            (r as any)[mk] = mv;
          }
        }

        if (existing.fieldExplanations && typeof existing.fieldExplanations === 'object') {
          r.fieldExplanations = {
            ...existing.fieldExplanations,
            ...(r.fieldExplanations || {})
          };
        }

        for (const [k, v] of Object.entries(existing)) {
          if (v !== undefined && v !== null && String(v).trim() !== '') {
            const incomingVal = (r as any)[k];
            if (incomingVal === undefined || incomingVal === null || String(incomingVal).trim() === '') {
              (r as any)[k] = v;
            }
          }
        }
      }

      mergedReports.push(r);
    }

    for (const existing of this.reports) {
      if (!restoredReportIds.has(existing.id)) {
        mergedReports.push(existing);
      }
    }
    this.reports = sortReportsDescending(mergedReports);

    const junctionsMap = new Map<string, ReportClothesJunction>();
    this.junctions.forEach(j => junctionsMap.set(j.id, j));
    rawJunctions.map(normalizeJunction).forEach(j => junctionsMap.set(j.id, j));
    this.junctions = Array.from(junctionsMap.values());

    // Restore drill-down presets if present in backup
    const rawPresets = parsed.drilldownPresets || parsed.reportPresets || parsed.presets;
    let presetsCount = 0;
    if (Array.isArray(rawPresets) && rawPresets.length > 0) {
      try {
        const merged = reportPresetService.importPresets(rawPresets, true, true);
        presetsCount = merged.length;
      } catch (err) {
        console.warn('Failed to import presets from JSON backup:', err);
      }
    }

    this.persistLocal();
    this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.notify();

    // Enqueue all restored items into the outbox to ensure cloud Firestore is synchronized
    this.clothes.forEach(c => {
      syncOutbox.enqueue('clothes', c.id, 'upsert', c, `Restore garment: ${c['Name'] || c.id}`);
    });
    this.reports.forEach(r => {
      syncOutbox.enqueue('wearingReport', r.id, 'upsert', r, `Restore report: ${r['Name'] || r.id}`);
    });
    this.junctions.forEach(j => {
      syncOutbox.enqueue('clothesJunction', j.id, 'upsert', j, `Restore junction: ${j.id}`);
    });

    return {
      clothes: this.clothes.length,
      reports: this.reports.length,
      junctions: this.junctions.length,
      presets: presetsCount
    };
  }

  /**
   * Helper to resolve all garment names for a wearing report from linked junctions, Clothes, Bra, and clothesTitle fields.
   */
  public getGarmentNamesForReport(reportId?: string, reportItem?: any): string[] {
    const names = new Set<string>();

    if (reportItem) {
      const checkProp = (val: any) => {
        if (!val) return;
        if (typeof val === 'string') {
          const trimmed = val.replace(/^\[\[/, '').replace(/\]\]$/, '').replace(/\.$/, '').trim();
          if (trimmed && trimmed !== 'No data' && trimmed !== 'N/A' && trimmed !== 'Not provided') {
            names.add(trimmed);
          }
        } else if (Array.isArray(val)) {
          val.forEach(v => checkProp(v));
        } else if (typeof val === 'object' && val.title) {
          checkProp(val.title);
        }
      };

      checkProp(reportItem['Clothes']);
      checkProp(reportItem['clothes']);
      checkProp(reportItem['Bra']);
      checkProp(reportItem['bra']);
      checkProp(reportItem['clothesTitle']);
    }

    const targetId = reportId || reportItem?.id;
    if (targetId) {
      const linkedJunctions = this.junctions.filter(j => {
        const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
        return rId === targetId;
      });

      linkedJunctions.forEach(j => {
        const jTitle = j.clothesTitle || (typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : undefined);
        if (jTitle && typeof jTitle === 'string') {
          const trimmed = jTitle.replace(/^\[\[/, '').replace(/\]\]$/, '').replace(/\.$/, '').trim();
          if (trimmed) names.add(trimmed);
        } else {
          const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
          const garment = this.clothes.find(c => c.id === cId);
          if (garment && (garment['Name'] || garment.name)) {
            const trimmed = (garment['Name'] || garment.name).trim();
            if (trimmed) names.add(trimmed);
          }
        }
      });
    }

    return Array.from(names);
  }

  /**
   * Scans all database records to gather previously entered unique values with their usage frequency and source garment/report names.
   * Matches the Teable previous value selector design and surfaces actual garment sources (e.g. "white marry old", "hugo bra").
   */
  public getFieldPreviousValues(
    field: string,
    target: 'clothes' | 'reports' | 'all' = 'clothes'
  ): Array<{ value: string; count: number; sources?: string[] }> {
    const counts = new Map<string, number>();
    const sourcesMap = new Map<string, Set<string>>();

    const toCamel = (s: string) => s.toLowerCase().replace(/[^a-zA-Z0-9]+(.)/g, (_, chr) => chr.toUpperCase());
    const candidateKeys = Array.from(new Set([
      field,
      field.toLowerCase(),
      field.replace(/\s+/g, ''),
      field.replace(/\s+/g, '_'),
      toCamel(field),
      field.replace(/\s+([a-z])/g, (_, c) => c.toUpperCase()),
      field.replace(/-/g, ''),
      ...(field.toLowerCase().includes('clothes type') ? ['Clothes Type', 'clothesType', 'clothes_type', 'type'] : []),
      ...(field.toLowerCase().includes('material') ? ['material', 'materialProp', 'Material'] : []),
      ...(field.toLowerCase().includes('thick clothes') ? ['thick clothes', 'thickClothes', 'thick_clothes'] : []),
      ...(field.toLowerCase().includes('hot at noon') || field.toLowerCase().includes('without aircond: hot') ? ['Hot at noon', 'hotAtNoon', 'noAircondHotFeeling', 'when without aircond: hot feeling'] : []),
      ...(field.toLowerCase().includes('without aircond: leg') ? ['noAircondLegFeeling', 'when without aircond: leg feeling'] : []),
      ...(field.toLowerCase().includes('with aircond: hot') ? ['aircondHotFeeling', 'when with aircond: hot feeling'] : []),
      ...(field.toLowerCase().includes('with aircond: leg') ? ['aircondLegFeeling', 'when with aircond: leg feeling'] : []),
      ...(field.toLowerCase().includes('sleep under fan: hot') ? ['fanSleepHotFeeling', 'when sleep under fan: hot feeling'] : []),
      ...(field.toLowerCase().includes('sleep under fan: leg') ? ['fanSleepLegFeeling', 'when sleep under fan: leg feeling'] : []),
      ...(field.toLowerCase().includes('side breast') ? ['side breast ok', 'sideBreastOk', 'sideBreastGatherBra', 'sideBreastOkWithGatherBra', 'side breast ok: with 聚拢 bra', 'side breast ok: with side out bra', 'sideBreastSideOutBra'] : []),
      ...(field.toLowerCase().includes('sit breast') ? ['sit breast ok', 'sitBreastOk', 'sitBreastGatherBra', 'sitBreastOkWithGatherBra', 'sit breast ok: With 聚拢bra', 'sit breast ok: with side out bra', 'sitBreastSideOutBra', 'sitBreast'] : []),
      ...(field.toLowerCase().includes('diao dai') ? ['diao dai', 'diaoDai', 'diao_dai'] : []),
      ...(field.toLowerCase().includes('center chest') ? ['center chest', 'centerChest', 'standBreast'] : []),
      ...(field.toLowerCase().includes('description') ? ['Descriptions', 'descriptions', 'lastComment', 'last comment or action'] : [])
    ]));

    const processItem = (item: any, isReport: boolean) => {
      let itemSources: string[] = [];
      if (isReport) {
        const gNames = this.getGarmentNamesForReport(item.id, item);
        if (gNames.length > 0) {
          itemSources = gNames;
        } else if (item['Name'] || item.name) {
          itemSources = [(item['Name'] || item.name).trim()];
        }
      } else {
        const gName = (item['Name'] || item.name || '').trim();
        if (gName) itemSources = [gName];
      }

      let val: any = undefined;
      for (const k of candidateKeys) {
        if (item[k] !== undefined && item[k] !== null && item[k] !== '') {
          val = item[k];
          break;
        }
      }
      if ((val === undefined || val === null || val === '') && item.customFields) {
        val = item.customFields[field] ?? item.customFields[field.toLowerCase()];
      }

      const recordValue = (str: string) => {
        counts.set(str, (counts.get(str) || 0) + 1);
        if (itemSources.length > 0) {
          if (!sourcesMap.has(str)) {
            sourcesMap.set(str, new Set<string>());
          }
          itemSources.forEach(s => sourcesMap.get(str)!.add(s));
        }
      };

      if (Array.isArray(val)) {
        val.forEach(v => {
          if (typeof v === 'string') {
            const str = v.trim();
            if (str && str !== 'No data' && str !== 'N/A' && str !== '??') {
              recordValue(str);
            }
          }
        });
      } else if (typeof val === 'string') {
        const str = val.trim();
        if (str && str !== 'No data' && str !== 'N/A' && str !== '??') {
          recordValue(str);
        }
      }
    };

    if (target === 'clothes' || target === 'all') {
      this.clothes.forEach(item => processItem(item, false));
    }
    if (target === 'reports' || target === 'all') {
      this.reports.forEach(item => processItem(item, true));
    }

    return Array.from(counts.entries())
      .map(([value, count]) => ({
        value,
        count,
        sources: sourcesMap.has(value) ? Array.from(sourcesMap.get(value)!) : []
      }))
      .sort((a, b) => b.count - a.count);
  }

  /**
   * Retrieves all unique historical explanation notes recorded across clothes and wearing reports for a specific field.
   * Looks into garment/report.fieldExplanations[field], customFields._fieldExplanations[field],
   * and customFields[`${field}_explain`], preserving source garment names (e.g. "white marry old", "hugo bra").
   */
  public getFieldHistoricalExplanations(
    field: string,
    targetCategory?: 'clothes' | 'wearingReport' | 'all'
  ): Array<{ text: string; note: string; garmentName?: string; sources?: string[]; count: number }> {
    const map = new Map<string, { text: string; note: string; garmentName?: string; sources?: string[]; count: number }>();
    const candidateKeys = Array.from(new Set([
      field,
      field.toLowerCase(),
      field.replace(/\s+/g, ''),
      field.replace(/\s+/g, '_'),
      field.replace(/-/g, '')
    ]));

    const checkItem = (item: any, isReport: boolean, defaultName: string) => {
      let explanationText = '';
      const explanations = item.fieldExplanations || item.customFields?._fieldExplanations || {};
      for (const k of candidateKeys) {
        if (explanations[k] && typeof explanations[k] === 'string' && explanations[k].trim()) {
          explanationText = explanations[k].trim();
          break;
        }
      }
      if (!explanationText && item.customFields) {
        for (const k of candidateKeys) {
          const directExplainKey = `${k}_explain`;
          if (item.customFields[directExplainKey] && typeof item.customFields[directExplainKey] === 'string' && item.customFields[directExplainKey].trim()) {
            explanationText = item.customFields[directExplainKey].trim();
            break;
          }
        }
      }

      if (explanationText && explanationText !== 'No data' && explanationText !== 'N/A') {
        let garmentDisplayName = (item['Name'] || item.name || defaultName).trim();
        let sources: string[] = [];
        if (isReport) {
          const garmentNames = this.getGarmentNamesForReport(item.id, item);
          if (garmentNames.length > 0) {
            garmentDisplayName = garmentNames.join(', ');
            sources = garmentNames;
          } else {
            sources = [garmentDisplayName];
          }
        } else {
          sources = [garmentDisplayName];
        }

        const existing = map.get(explanationText);
        if (existing) {
          existing.count += 1;
          sources.forEach(s => {
            if (!existing.sources?.includes(s)) {
              existing.sources = [...(existing.sources || []), s];
            }
          });
        } else {
          map.set(explanationText, {
            text: explanationText,
            note: explanationText,
            garmentName: garmentDisplayName,
            sources,
            count: 1
          });
        }
      }
    };

    if (targetCategory !== 'wearingReport') {
      this.clothes.forEach((item: Clothes) => checkItem(item, false, 'Garment'));
    }
    if (targetCategory !== 'clothes') {
      this.reports.forEach((item: WearingReport) => checkItem(item, true, 'Wearing Report'));
    }

    return Array.from(map.values()).sort((a, b) => b.count - a.count);
  }

  // Getters
  public getClothes(): Clothes[] {
    return this.clothes;
  }

  public getReports(): WearingReport[] {
    return [...this.reports].sort((a, b) => {
      const dateA = new Date(a['wear date'] || a.created || 0).getTime();
      const dateB = new Date(b['wear date'] || b.created || 0).getTime();
      return dateB - dateA;
    });
  }

  public getWearingReports(): WearingReport[] {
    return this.getReports();
  }

  public getJunctions(): ClothesJunction[] {
    return this.junctions;
  }

  public getClothesJunctions(): ClothesJunction[] {
    return this.junctions;
  }

  public getClothesStats(clothesId: string) {
    return this.getGarmentStats(clothesId);
  }

  public getGarmentStats(clothesId: string) {
    const itemJunctions = this.junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId === clothesId;
    });
    const needWashJunctions = itemJunctions.filter(j => j['wash stated'] === 'need wash');
    const needWash = needWashJunctions.length > 0;
    const unwashedSeconds = needWashJunctions.reduce((acc, j) => acc + (j['duration'] || 0), 0);
    const totalWears = itemJunctions.length;
    const totalDurationHours = Math.round(itemJunctions.reduce((acc, j) => acc + (j['duration'] || 0), 0) / 3600);

    return {
      needWash,
      unwashedSeconds,
      unwashedHours: (unwashedSeconds / 3600).toFixed(1),
      totalWears,
      totalDurationHours
    };
  }

  public getLaundryQueue() {
    return this.clothes
      .map(c => ({
        garment: c,
        stats: this.getGarmentStats(c.id)
      }))
      .filter(entry => entry.stats.needWash);
  }

  /**
   * Reconciles a garment's wash stats (Last Washed, Tracked App Washes, Total Washed Count)
   * based strictly on its actual confirmed wash junction records in the database.
   * If all junctions are 'need wash' (or no wash junctions exist), Last Washed is cleared
   * so phantom wash dates are never retained.
   */
  public recalculateGarmentWashStats(clothesId: string): ClothesItem | null {
    const garment = this.clothes.find(c => c.id === clothesId);
    if (!garment) return null;

    const garmentJunctions = this.junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId === clothesId;
    });

    const washJunctions = garmentJunctions.filter(j => j['wash stated'] === 'wash');
    const hasNeedWash = garmentJunctions.some(j => j['wash stated'] === 'need wash');

    // Find the latest wash date among confirmed wash junctions
    let latestWashDate = '';
    washJunctions.forEach(j => {
      const wDate = j['wash date'] || j.washDate || j['washed date'] || j.washedDate;
      const key = toDateKey(wDate);
      if (key && (!latestWashDate || key > latestWashDate)) {
        latestWashDate = key;
      }
    });

    const trackedAppWashes = washJunctions.length;
    const legacyWashed = typeof garment['wearing frequency(how many times legacy washed)'] === 'number'
      ? garment['wearing frequency(how many times legacy washed)']
      : 0;
    const totalWashed = legacyWashed + trackedAppWashes;

    const updatedGarment: ClothesItem = {
      ...garment,
      'Last Washed': latestWashDate ? `${latestWashDate}T00:00:00.000Z` : '',
      'Tracked App Washes': trackedAppWashes,
      'Total Washed Count': totalWashed,
      'wet': hasNeedWash ? 'dry' : garment['wet']
    };

    this.clothes = this.clothes.map(c => c.id === clothesId ? updatedGarment : c);
    return updatedGarment;
  }

  /**
   * Reconciles wash stats across all garments against their junction records.
   * Cleans up any orphaned Last Washed timestamps (such as garments reverted to unwashed).
   */
  public reconcileAllGarmentsWashStats(): void {
    let changed = false;
    this.clothes.forEach(garment => {
      const garmentJunctions = this.junctions.filter(j => {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        return cId === garment.id;
      });

      const washJunctions = garmentJunctions.filter(j => j['wash stated'] === 'wash');
      let latestWashDate = '';
      washJunctions.forEach(j => {
        const wDate = j['wash date'] || j.washDate || j['washed date'] || j.washedDate;
        const key = toDateKey(wDate);
        if (key && (!latestWashDate || key > latestWashDate)) {
          latestWashDate = key;
        }
      });

      const expectedLastWashed = latestWashDate ? `${latestWashDate}T00:00:00.000Z` : '';
      const expectedTracked = washJunctions.length;
      const legacyWashed = typeof garment['wearing frequency(how many times legacy washed)'] === 'number'
        ? garment['wearing frequency(how many times legacy washed)']
        : 0;
      const expectedTotal = legacyWashed + expectedTracked;

      // If garment was incorrectly showing Last Washed without any wash junctions, fix it
      if (
        (garmentJunctions.length > 0 && garment['Last Washed'] !== expectedLastWashed) ||
        (garment['Tracked App Washes'] !== expectedTracked) ||
        (garment['Total Washed Count'] !== expectedTotal)
      ) {
        changed = true;
        garment['Last Washed'] = expectedLastWashed;
        garment['Tracked App Washes'] = expectedTracked;
        garment['Total Washed Count'] = expectedTotal;
      }
    });

    if (changed) {
      this.persistLocalOnly();
      this.notify();
    }
  }

  // Local-First Mutations with Async Firestore persistence & Full Undo Capability
  public async markGarmentWashed(clothesId: string, customWashDate?: string): Promise<WashUndoRecord | null> {
    let changed = false;
    const washDateStr = customWashDate ? (toDateKey(customWashDate) || customWashDate) : getTodayDateKey();
    const affectedJunctions: ReportClothesJunction[] = [];

    // Snapshot prior state for UNDO
    const targetGarment = this.clothes.find(c => c.id === clothesId);
    const garmentName = targetGarment?.['Name'] || 'Garment';
    const priorJunctionsSnapshot: WashUndoRecord['junctions'] = [];

    this.junctions.forEach(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      if (cId === clothesId && j['wash stated'] === 'need wash') {
        priorJunctionsSnapshot.push({
          id: j.id,
          prevWashStated: j['wash stated'],
          prevWashDate: j['wash date'],
          prevWashedDate: j['washed date']
        });
      }
    });

    const priorGarmentSnapshot: WashUndoRecord['garments'] = targetGarment ? [{
      id: targetGarment.id,
      name: garmentName,
      prevTrackedWashes: targetGarment['Tracked App Washes'] || 0,
      prevTotalWashed: targetGarment['Total Washed Count'] || 0,
      prevLastWashed: targetGarment['Last Washed'] || '',
      prevWet: targetGarment['wet'] || 'dry'
    }] : [];

    this.junctions = this.junctions.map(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      if (cId === clothesId && j['wash stated'] === 'need wash') {
        changed = true;
        const updated: ReportClothesJunction = {
          ...j,
          'wash stated': 'wash' as const,
          washStated: 'wash',
          'wash date': washDateStr,
          'washed date': washDateStr,
          washDate: washDateStr,
          washedDate: washDateStr
        };
        affectedJunctions.push(updated);
        return updated;
      }
      return j;
    });

    let updatedGarment: ClothesItem | null = null;
    this.clothes = this.clothes.map(c => {
      if (c.id === clothesId) {
        changed = true;
        const legacyWashed = typeof c['wearing frequency(how many times legacy washed)'] === 'number'
          ? c['wearing frequency(how many times legacy washed)']
          : 0;
        const newTracked = (c['Tracked App Washes'] || 0) + 1;
        // Rule: Total Washed Count = wearing frequency legacy + tracked app wash (tracked + 1)
        const newTotal = legacyWashed + newTracked;
        const washIso = `${washDateStr}T00:00:00.000Z`;
        updatedGarment = {
          ...c,
          'Tracked App Washes': newTracked,
          'Total Washed Count': newTotal,
          'Last Washed': washIso,
          'wet': 'dry' as const
        };
        return updatedGarment;
      }
      return c;
    });

    if (changed) {
      this.persistLocal();
      this.notify();

      // Record undo item
      const undoRecord: WashUndoRecord = {
        id: `wash_undo_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
        timestamp: Date.now(),
        type: 'single',
        description: `Washed "${garmentName}" on ${washDateStr}`,
        garments: priorGarmentSnapshot,
        junctions: priorJunctionsSnapshot
      };
      this.washUndoStack.push(undoRecord);
      if (this.washUndoStack.length > 20) this.washUndoStack.shift();
      this.notifyUndo(undoRecord);

      // Enqueue to Outbox
      affectedJunctions.forEach(j => {
        syncOutbox.enqueue(
          'clothesJunction',
          j.id,
          'upsert',
          {
            'wash stated': 'wash',
            'wash date': washDateStr,
            'washed date': washDateStr
          },
          `Wash junction ${j.id}`
        );
      });

      if (updatedGarment) {
        syncOutbox.enqueue(
          'clothes',
          clothesId,
          'upsert',
          {
            'Tracked App Washes': (updatedGarment as ClothesItem)['Tracked App Washes'],
            'Total Washed Count': (updatedGarment as ClothesItem)['Total Washed Count'],
            'Last Washed': (updatedGarment as ClothesItem)['Last Washed'],
            'wet': 'dry'
          },
          `Wash garment ${clothesId}`
        );
      }

      return undoRecord;
    }
    return null;
  }

  public async markAllLaundryWashed(customWashDate?: string): Promise<WashUndoRecord | null> {
    const washDateStr = customWashDate ? (toDateKey(customWashDate) || customWashDate) : getTodayDateKey();
    const needWashIds = new Set<string>();
    const affectedJunctions: ReportClothesJunction[] = [];

    const priorJunctionsSnapshot: WashUndoRecord['junctions'] = [];
    this.junctions.forEach(j => {
      if (j['wash stated'] === 'need wash') {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        needWashIds.add(cId);
        priorJunctionsSnapshot.push({
          id: j.id,
          prevWashStated: j['wash stated'],
          prevWashDate: j['wash date'],
          prevWashedDate: j['washed date']
        });
      }
    });

    if (needWashIds.size === 0 && priorJunctionsSnapshot.length === 0) {
      return null;
    }

    const priorGarmentsSnapshot: WashUndoRecord['garments'] = [];
    this.clothes.forEach(c => {
      if (needWashIds.has(c.id)) {
        priorGarmentsSnapshot.push({
          id: c.id,
          name: c['Name'] || 'Garment',
          prevTrackedWashes: c['Tracked App Washes'] || 0,
          prevTotalWashed: c['Total Washed Count'] || 0,
          prevLastWashed: c['Last Washed'] || '',
          prevWet: c['wet'] || 'dry'
        });
      }
    });

    this.junctions = this.junctions.map(j => {
      if (j['wash stated'] === 'need wash') {
        const updated: ReportClothesJunction = {
          ...j,
          'wash stated': 'wash' as const,
          washStated: 'wash',
          'wash date': washDateStr,
          'washed date': washDateStr,
          washDate: washDateStr,
          washedDate: washDateStr
        };
        affectedJunctions.push(updated);
        return updated;
      }
      return j;
    });

    const washIso = `${washDateStr}T00:00:00.000Z`;
    const updatedGarmentList: ClothesItem[] = [];
    this.clothes = this.clothes.map(c => {
      if (needWashIds.has(c.id)) {
        const legacyWashed = typeof c['wearing frequency(how many times legacy washed)'] === 'number'
          ? c['wearing frequency(how many times legacy washed)']
          : 0;
        const newTracked = (c['Tracked App Washes'] || 0) + 1;
        // Rule: Total Washed Count = wearing frequency legacy + tracked app wash (tracked + 1)
        const newTotal = legacyWashed + newTracked;
        const updatedItem = {
          ...c,
          'Tracked App Washes': newTracked,
          'Total Washed Count': newTotal,
          'Last Washed': washIso,
          'wet': 'dry' as const
        };
        updatedGarmentList.push(updatedItem);
        return updatedItem;
      }
      return c;
    });

    this.persistLocal();
    this.notify();

    const undoRecord: WashUndoRecord = {
      id: `wash_undo_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      timestamp: Date.now(),
      type: 'all',
      description: `Marked all ${needWashIds.size} hamper items as washed on ${washDateStr}`,
      garments: priorGarmentsSnapshot,
      junctions: priorJunctionsSnapshot
    };
    this.washUndoStack.push(undoRecord);
    if (this.washUndoStack.length > 20) this.washUndoStack.shift();
    this.notifyUndo(undoRecord);

    // Outbox batch update
    updatedGarmentList.forEach(item => {
      syncOutbox.enqueue(
        'clothes',
        item.id,
        'upsert',
        {
          'Tracked App Washes': item['Tracked App Washes'],
          'Total Washed Count': item['Total Washed Count'],
          'Last Washed': item['Last Washed'],
          'wet': 'dry'
        },
        `Wash garment ${item.id}`
      );
    });
    affectedJunctions.forEach(j => {
      syncOutbox.enqueue(
        'clothesJunction',
        j.id,
        'upsert',
        {
          'wash stated': 'wash',
          'wash date': washDateStr,
          'washed date': washDateStr
        },
        `Wash junction ${j.id}`
      );
    });

    return undoRecord;
  }

  /**
   * Reverts the most recent wash action (or specified undoId)
   * Restores both local and Firestore state back to 'need wash' and prior wash counts.
   */
  public async undoWash(undoId?: string): Promise<{ success: boolean; description: string }> {
    if (this.washUndoStack.length === 0) {
      return { success: false, description: 'No wash actions to undo' };
    }

    const index = undoId
      ? this.washUndoStack.findIndex(r => r.id === undoId)
      : this.washUndoStack.length - 1;

    if (index === -1) {
      return { success: false, description: 'Undo action not found or already expired' };
    }

    const [record] = this.washUndoStack.splice(index, 1);
    this.notifyUndo(null);

    const revertedJunctionMap = new Map(record.junctions.map(j => [j.id, j]));
    const revertedGarmentMap = new Map(record.garments.map(g => [g.id, g]));

    // 1. Revert junctions in memory
    this.junctions = this.junctions.map(j => {
      const rev = revertedJunctionMap.get(j.id);
      if (rev) {
        const prevWasWashed = rev.prevWashStated === 'wash';
        return {
          ...j,
          'wash stated': rev.prevWashStated,
          washStated: rev.prevWashStated,
          'wash date': prevWasWashed ? rev.prevWashDate : undefined,
          'washed date': prevWasWashed ? rev.prevWashedDate : undefined,
          washDate: prevWasWashed ? rev.prevWashDate : undefined,
          washedDate: prevWasWashed ? rev.prevWashedDate : undefined
        };
      }
      return j;
    });

    // 2. Revert garments in memory
    this.clothes = this.clothes.map(c => {
      const rev = revertedGarmentMap.get(c.id);
      if (rev) {
        return {
          ...c,
          'Tracked App Washes': rev.prevTrackedWashes,
          'Total Washed Count': rev.prevTotalWashed,
          'Last Washed': rev.prevLastWashed,
          'wet': rev.prevWet
        };
      }
      return c;
    });

    this.persistLocal();
    this.notify();

    // 3. Sync reverted state via Outbox
    record.junctions.forEach(rev => {
      const prevWasWashed = rev.prevWashStated === 'wash';
      syncOutbox.enqueue(
        'clothesJunction',
        rev.id,
        'upsert',
        {
          'wash stated': rev.prevWashStated,
          'wash date': prevWasWashed ? (rev.prevWashDate || null) : null,
          'washed date': prevWasWashed ? (rev.prevWashedDate || null) : null
        },
        `Revert junction ${rev.id}`
      );
    });

    record.garments.forEach(rev => {
      syncOutbox.enqueue(
        'clothes',
        rev.id,
        'upsert',
        {
          'Tracked App Washes': rev.prevTrackedWashes,
          'Total Washed Count': rev.prevTotalWashed,
          'Last Washed': rev.prevLastWashed || null,
          'wet': rev.prevWet
        },
        `Revert garment ${rev.id}`
      );
    });

    return { success: true, description: record.description };
  }

  public getLatestUndoRecord(): WashUndoRecord | null {
    return this.washUndoStack.length > 0 ? this.washUndoStack[this.washUndoStack.length - 1] : null;
  }

  public dismissUndo(undoId?: string) {
    if (undoId) {
      this.washUndoStack = this.washUndoStack.filter(r => r.id !== undoId);
    } else {
      this.washUndoStack = [];
    }
    this.notifyUndo(null);
  }

  public onWashUndoChange(callback: (record: WashUndoRecord | null) => void): () => void {
    this.undoListeners.add(callback);
    return () => this.undoListeners.delete(callback);
  }

  private notifyUndo(record: WashUndoRecord | null) {
    this.undoListeners.forEach(cb => {
      try { cb(record); } catch (e) { console.error('Error in undo listener', e); }
    });
  }

  public async addGarment(newGarment: Omit<Clothes, 'id'>): Promise<Clothes> {
    const id = 'rec_clothes_' + Date.now();
    const item: Clothes = normalizeGarment({
      ...newGarment,
      id
    });

    this.clothes.unshift(item);
    this.persistLocal();
    this.notify();

    // Enqueue to Outbox
    syncOutbox.enqueue(
      'clothes',
      id,
      'upsert',
      item,
      `Add garment "${item['Name'] || id}"`
    );

    return item;
  }

  public addClothes(newClothes: Omit<Clothes, 'id'>): Promise<Clothes> {
    return this.addGarment(newClothes);
  }

  public async updateGarment(id: string, partial: Partial<Clothes>): Promise<void> {
    const sanitizedPartial: Partial<Clothes> = { ...partial };
    if (sanitizedPartial['Last Washed'] !== undefined) {
      if (sanitizedPartial['Last Washed']) {
        const dKey = toDateKey(sanitizedPartial['Last Washed']) || sanitizedPartial['Last Washed'];
        sanitizedPartial['Last Washed'] = `${dKey}T00:00:00.000Z`;
      } else {
        sanitizedPartial['Last Washed'] = '';
      }
    }

    this.clothes = this.clothes.map(c => {
      if (c.id === id) {
        return normalizeGarment({ ...c, ...sanitizedPartial });
      }
      return c;
    });

    // If Last Washed was explicitly updated, keep related junctions in sync
    let matchingJunctionToSync: ReportClothesJunction | undefined;
    if (sanitizedPartial['Last Washed']) {
      const dKey = toDateKey(sanitizedPartial['Last Washed']);
      if (dKey) {
        const matchingJunction = this.junctions
          .filter(j => {
            const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
            return cId === id;
          })
          .pop();
        if (matchingJunction) {
          matchingJunction['wash date'] = dKey;
          matchingJunction['washed date'] = dKey;
          matchingJunction.washDate = dKey;
          matchingJunction.washedDate = dKey;
          matchingJunction['wash stated'] = 'wash';
          matchingJunction.washStated = 'wash';
          matchingJunctionToSync = matchingJunction;
        }
      }
    }

    this.persistLocal();
    this.notify();

    // Enqueue to Outbox
    syncOutbox.enqueue(
      'clothes',
      id,
      'upsert',
      sanitizedPartial,
      `Update garment "${id}"`
    );

    if (matchingJunctionToSync) {
      const dKey = toDateKey(sanitizedPartial['Last Washed']);
      syncOutbox.enqueue(
        'clothesJunction',
        matchingJunctionToSync.id,
        'upsert',
        { 'wash date': dKey, 'washed date': dKey, 'wash stated': 'wash' },
        `Sync wash date on junction ${matchingJunctionToSync.id}`
      );
    }
  }

  public updateClothes(id: string, partial: Partial<Clothes>): Promise<void> {
    return this.updateGarment(id, partial);
  }

  /**
   * Batch adds or removes a tag on multiple garments.
   * If action is 'add', garments without the tag have it appended; garments already having it retain it.
   * If action is 'remove', the tag is filtered out from the garments.
   */
  public async batchUpdateGarmentTags(
    clothesIds: string[],
    tag: string,
    action: 'add' | 'remove'
  ): Promise<number> {
    const cleanTag = tag.trim();
    if (!cleanTag || clothesIds.length === 0) return 0;

    let modifiedCount = 0;
    for (const id of clothesIds) {
      const garment = this.clothes.find(c => c.id === id);
      if (!garment) continue;

      const currentTags = Array.isArray(garment.tags) ? [...garment.tags] : [];
      const hasTag = currentTags.some(t => t.toLowerCase() === cleanTag.toLowerCase());

      if (action === 'add' && !hasTag) {
        currentTags.push(cleanTag);
        await this.updateGarment(id, { tags: currentTags });
        modifiedCount++;
      } else if (action === 'remove' && hasTag) {
        const nextTags = currentTags.filter(t => t.toLowerCase() !== cleanTag.toLowerCase());
        await this.updateGarment(id, { tags: nextTags });
        modifiedCount++;
      }
    }
    return modifiedCount;
  }

  public async addWearingReport(
    reportData: Omit<WearingReport, 'id'>,
    selectedGarmentIds: { clothesId: string; durationHours: number; washStated: 'wash' | 'need wash'; washDate?: string }[]
  ): Promise<WearingReport> {
    const reportId = 'rec_report_' + Date.now();
    const finalReportName = reportData['Name']?.trim() || generateDefaultReportName(reportData['wear date']);
    const newReport: WearingReport = normalizeReport({
      ...reportData,
      'Name': finalReportName,
      id: reportId
    });

    const wearDateKey = toDateKey(newReport['wear date']) || getTodayDateKey();

    // Enforce strict uniqueness: in any single wearing report, each garment can appear at most once
    const uniqueMap = new Map<string, typeof selectedGarmentIds[0]>();
    selectedGarmentIds.forEach(item => {
      if (!uniqueMap.has(item.clothesId)) {
        uniqueMap.set(item.clothesId, item);
      }
    });
    const dedupedGarments = Array.from(uniqueMap.values());

    const newJunctions: ReportClothesJunction[] = dedupedGarments.map((item, idx) => {
      const garment = this.clothes.find(c => c.id === item.clothesId);
      const junctionId = `rec_junction_${Date.now()}_${idx}`;
      const rawWashDate = item.washStated === 'wash'
        ? (item.washDate || (item as any)['wash date'] || (item as any).washedDate || (item as any)['washed date'] || wearDateKey)
        : undefined;

      return normalizeJunction({
        id: junctionId,
        name: this.junctions.length + idx + 1,
        'wearing report id': { id: reportId, title: newReport['Name'] },
        'clothes(new)': { id: item.clothesId, title: garment?.['Name'] || 'Garment' },
        'duration': Math.round(item.durationHours * 3600),
        'wash stated': item.washStated,
        'Clothes Type (from clothes(new))': garment?.['Clothes Type'] || 'Dress',
        'wash date': rawWashDate,
        'washed date': rawWashDate,
        'wear date': wearDateKey
      });
    });

    this.reports.unshift(newReport);
    this.junctions.unshift(...newJunctions);

    // If any garments were logged directly in 'wash' status, increment their wash count according to formula
    const directlyWashedItems = selectedGarmentIds.filter(i => i.washStated === 'wash');
    if (directlyWashedItems.length > 0) {
      this.clothes = this.clothes.map(c => {
        const matchingItem = directlyWashedItems.find(i => i.clothesId === c.id);
        if (matchingItem) {
          const legacyWashed = typeof c['wearing frequency(how many times legacy washed)'] === 'number'
            ? c['wearing frequency(how many times legacy washed)']
            : 0;
          const newTracked = (c['Tracked App Washes'] || 0) + 1;
          const newTotal = legacyWashed + newTracked;
          const washDateStr = matchingItem.washDate || wearDateKey;
          return {
            ...c,
            'Tracked App Washes': newTracked,
            'Total Washed Count': newTotal,
            'Last Washed': `${washDateStr}T00:00:00.000Z`,
            'wet': 'dry' as const
          };
        }
        return c;
      });
    }

    this.persistLocal();
    this.notify();

    // Enqueue mutations to Outbox
    syncOutbox.enqueue(
      'wearingReport',
      reportId,
      'upsert',
      newReport,
      `Add wearing report "${newReport['Name'] || reportId}"`
    );

    newJunctions.forEach(j => {
      syncOutbox.enqueue(
        'clothesJunction',
        j.id,
        'upsert',
        j,
        `Link garment in report (${j.id})`
      );
    });

    directlyWashedItems.forEach(item => {
      const updatedGarment = this.clothes.find(c => c.id === item.clothesId);
      if (updatedGarment) {
        syncOutbox.enqueue(
          'clothes',
          item.clothesId,
          'upsert',
          {
            'Tracked App Washes': updatedGarment['Tracked App Washes'],
            'Total Washed Count': updatedGarment['Total Washed Count'],
            'Last Washed': updatedGarment['Last Washed'],
            'wet': 'dry'
          },
          `Update wash count for ${item.clothesId}`
        );
      }
    });

    return newReport;
  }

  public createWearingReport(
    reportData: Omit<WearingReport, 'id'>,
    selectedGarmentIds: { clothesId: string; durationHours: number; washStated: 'wash' | 'need wash'; washDate?: string }[]
  ): Promise<WearingReport> {
    return this.addWearingReport(reportData, selectedGarmentIds);
  }

  public async deleteGarment(clothesId: string): Promise<void> {
    // 1. Remove from local clothes
    this.clothes = this.clothes.filter(c => c.id !== clothesId);

    // 2. Remove orphaned junctions
    const removedJunctions = this.junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId === clothesId;
    });
    this.junctions = this.junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId !== clothesId;
    });

    this.persistLocal();
    this.notify();

    // 3. Enqueue deletes to Outbox
    syncOutbox.enqueue('clothes', clothesId, 'delete', undefined, `Delete garment ${clothesId}`);
    removedJunctions.forEach(j => {
      syncOutbox.enqueue('clothesJunction', j.id, 'delete', undefined, `Delete junction ${j.id}`);
    });
  }

  public deleteClothes(clothesId: string): Promise<void> {
    return this.deleteGarment(clothesId);
  }

  public async deleteWearingReport(reportId: string): Promise<void> {
    const targetReport = this.reports.find(r => r.id === reportId);
    const targetName = (targetReport?.Name || '').trim();

    // 1. Remove from local reports
    this.reports = this.reports.filter(r => r.id !== reportId);

    const isReportJunction = (j: ReportClothesJunction) => {
      const link = extractLinkField(j['wearing report id'] || j.wearingReportId, j.wearingReportTitle);
      return link.id === reportId ||
        (targetName !== '' && (link.title === targetName || link.id === targetName)) ||
        (link.title !== '' && link.title === reportId);
    };

    // 2. Remove associated junctions
    const removedJunctions = this.junctions.filter(isReportJunction);
    this.junctions = this.junctions.filter(j => !isReportJunction(j));

    this.persistLocal();
    this.notify();

    // 3. Enqueue deletes to Outbox
    syncOutbox.enqueue('wearingReport', reportId, 'delete', undefined, `Delete report ${reportId}`);
    removedJunctions.forEach(j => {
      syncOutbox.enqueue('clothesJunction', j.id, 'delete', undefined, `Delete junction ${j.id}`);
    });
  }

  public async updateWearingReport(
    reportId: string,
    partialReport: Partial<WearingReport>,
    updatedClothesItems?: Array<{
      clothesId: string;
      durationHours: number;
      washStated: 'wash' | 'need wash';
      washDate?: string;
      junctionId?: string;
      junctionAutoNumber?: number | string;
    }>
  ): Promise<WearingReport> {
    let updated: WearingReport | null = null;
    this.reports = this.reports.map(r => {
      if (r.id === reportId) {
        updated = normalizeReport({ ...r, ...partialReport });
        return updated;
      }
      return r;
    });

    // If updatedClothesItems was provided, rebuild junctions for this report
    let newJunctions: ReportClothesJunction[] = [];
    let oldJunctions: ReportClothesJunction[] = [];
    let newIds = new Set<string>();

    if (updatedClothesItems && updatedClothesItems.length > 0) {
      const targetName = ((updated as any)?.Name || '').trim();
      const isReportJunction = (j: ReportClothesJunction) => {
        const link = extractLinkField(j['wearing report id'] || j.wearingReportId, j.wearingReportTitle);
        return link.id === reportId ||
          (targetName !== '' && (link.title === targetName || link.id === targetName)) ||
          (link.title !== '' && link.title === reportId);
      };

      // Find old junctions to compare
      oldJunctions = this.junctions.filter(isReportJunction);

      // Filter out old from local
      this.junctions = this.junctions.filter(j => !isReportJunction(j));

      const repWearDate = toDateKey((updated as any)?.['wear date']) || getTodayDateKey();

      newJunctions = updatedClothesItems.map((item, idx) => {
        const garment = this.clothes.find(c => c.id === item.clothesId);
        const matchingOld = item.junctionId
          ? oldJunctions.find(oj => oj.id === item.junctionId)
          : oldJunctions.find(oj => {
              const ojCId = typeof oj['clothes(new)'] === 'object' ? oj['clothes(new)'].id : String(oj['clothes(new)']);
              return ojCId === item.clothesId;
            });

        const junctionId = item.junctionId || matchingOld?.id || `rec_junction_${Date.now()}_${idx}`;
        const autoNumber = item.junctionAutoNumber || matchingOld?.name || (this.junctions.length + idx + 1);

        const isItemWashed = item.washStated === 'wash';
        const preservedWashDate = isItemWashed
          ? (item.washDate
            || (item as any)['wash date']
            || (item as any).washedDate
            || (item as any)['washed date']
            || matchingOld?.['wash date']
            || matchingOld?.['washed date']
            || repWearDate)
          : undefined;

        return normalizeJunction({
          id: junctionId,
          name: autoNumber,
          'wearing report id': { id: reportId, title: (updated as any)?.['Name'] || 'Wearing Report' },
          'clothes(new)': { id: item.clothesId, title: garment?.['Name'] || 'Garment' },
          'duration': Math.round(item.durationHours * 3600),
          'wash stated': item.washStated,
          'Clothes Type (from clothes(new))': garment?.['Clothes Type'] || 'Dress',
          'wash date': preservedWashDate,
          'washed date': preservedWashDate,
          'wear date': repWearDate
        });
      });

      this.junctions.unshift(...newJunctions);
      newIds = new Set(newJunctions.map(nj => nj.id));
    }

    this.persistLocal();
    this.notify();

    // Enqueue wearing report update to Outbox
    syncOutbox.enqueue(
      'wearingReport',
      reportId,
      'upsert',
      partialReport,
      `Update report ${reportId}`
    );

    if (updatedClothesItems && updatedClothesItems.length > 0) {
      oldJunctions.forEach(oj => {
        if (!newIds.has(oj.id)) {
          syncOutbox.enqueue('clothesJunction', oj.id, 'delete', undefined, `Delete removed junction ${oj.id}`);
        }
      });
      newJunctions.forEach(nj => {
        syncOutbox.enqueue('clothesJunction', nj.id, 'upsert', nj, `Update junction ${nj.id}`);
      });

      // Reconcile wash stats for all affected clothes
      const affectedClothesIds = new Set<string>();
      oldJunctions.forEach(oj => {
        const cId = typeof oj['clothes(new)'] === 'object' ? oj['clothes(new)'].id : String(oj['clothes(new)']);
        if (cId) affectedClothesIds.add(cId);
      });
      newJunctions.forEach(nj => {
        const cId = typeof nj['clothes(new)'] === 'object' ? nj['clothes(new)'].id : String(nj['clothes(new)']);
        if (cId) affectedClothesIds.add(cId);
      });

      affectedClothesIds.forEach(cId => {
        const updatedG = this.recalculateGarmentWashStats(cId);
        if (updatedG) {
          syncOutbox.enqueue(
            'clothes',
            cId,
            'upsert',
            {
              'Tracked App Washes': updatedG['Tracked App Washes'],
              'Total Washed Count': updatedG['Total Washed Count'],
              'Last Washed': updatedG['Last Washed'],
              'wet': updatedG['wet']
            },
            `Reconcile wash stats for ${cId}`
          );
        }
      });
    }

    return updated || this.reports.find(r => r.id === reportId)!;
  }

  public async addAIReportVersion(
    reportId: string,
    version: { content: string; title?: string; model?: string; isPrimary?: boolean }
  ): Promise<WearingReport | null> {
    const report = this.reports.find(r => r.id === reportId);
    if (!report) return null;

    const currentVersions: AIReportVersion[] = Array.isArray(report.aiReports) ? [...report.aiReports] : [];
    const shouldBePrimary = version.isPrimary ?? (currentVersions.length === 0);
    if (shouldBePrimary) {
      currentVersions.forEach(v => { v.isPrimary = false; });
    }

    const newVersion: AIReportVersion = {
      id: `ai_ver_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      timestamp: new Date().toISOString(),
      title: version.title || `Report #${currentVersions.length + 1}`,
      model: version.model || 'gemini-3.6-flash',
      content: version.content,
      isPrimary: shouldBePrimary
    };

    currentVersions.unshift(newVersion);
    const primaryVersion = currentVersions.find(v => v.isPrimary) || currentVersions[0];

    return await this.updateWearingReport(reportId, {
      aiReports: currentVersions,
      'AI Wearing Analysis': primaryVersion?.content || version.content,
      aiWearingAnalysis: primaryVersion?.content || version.content
    });
  }

  public async updateAIReportVersion(
    reportId: string,
    versionId: string,
    updates: { content?: string; title?: string; isPrimary?: boolean }
  ): Promise<WearingReport | null> {
    const report = this.reports.find(r => r.id === reportId);
    if (!report || !Array.isArray(report.aiReports)) return null;

    const currentVersions = report.aiReports.map(v => {
      if (v.id === versionId) {
        return {
          ...v,
          ...(updates.content !== undefined ? { content: updates.content } : {}),
          ...(updates.title !== undefined ? { title: updates.title } : {}),
          ...(updates.isPrimary !== undefined ? { isPrimary: updates.isPrimary } : {})
        };
      }
      if (updates.isPrimary) {
        return { ...v, isPrimary: false };
      }
      return v;
    });

    const primaryVersion = currentVersions.find(v => v.isPrimary) || currentVersions[0];

    return await this.updateWearingReport(reportId, {
      aiReports: currentVersions,
      'AI Wearing Analysis': primaryVersion?.content || '',
      aiWearingAnalysis: primaryVersion?.content || ''
    });
  }

  public async deleteAIReportVersion(
    reportId: string,
    versionId: string
  ): Promise<WearingReport | null> {
    const report = this.reports.find(r => r.id === reportId);
    if (!report || !Array.isArray(report.aiReports)) return null;

    let remaining = report.aiReports.filter(v => v.id !== versionId);
    if (remaining.length > 0 && !remaining.some(v => v.isPrimary)) {
      remaining[0].isPrimary = true;
    }

    const primaryVersion = remaining.find(v => v.isPrimary) || remaining[0];

    return await this.updateWearingReport(reportId, {
      aiReports: remaining,
      'AI Wearing Analysis': primaryVersion ? primaryVersion.content : '',
      aiWearingAnalysis: primaryVersion ? primaryVersion.content : ''
    });
  }

  public async setPrimaryAIReportVersion(
    reportId: string,
    versionId: string
  ): Promise<WearingReport | null> {
    return this.updateAIReportVersion(reportId, versionId, { isPrimary: true });
  }

  // --- GARMENT MULTI-VERSION AI REPORTS ---
  public async addGarmentAIReportVersion(
    garmentId: string,
    version: { content: string; title?: string; model?: string; isPrimary?: boolean }
  ): Promise<ClothesItem | null> {
    const garment = this.clothes.find(c => c.id === garmentId);
    if (!garment) return null;

    const existingVersions: AIReportVersion[] = Array.isArray(garment.aiReports) ? [...garment.aiReports] : [];
    const newVersionId = `garment_ai_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
    const shouldBePrimary = version.isPrimary !== undefined ? version.isPrimary : existingVersions.length === 0;

    const updatedVersions: AIReportVersion[] = existingVersions.map(v => ({
      ...v,
      isPrimary: shouldBePrimary ? false : Boolean(v.isPrimary)
    }));

    const newVersionEntry: AIReportVersion = {
      id: newVersionId,
      timestamp: new Date().toISOString(),
      title: version.title || `Report #${existingVersions.length + 1}`,
      model: version.model || 'gemini-3.6-flash',
      content: version.content,
      isPrimary: shouldBePrimary
    };

    updatedVersions.unshift(newVersionEntry);

    const primaryVersion = updatedVersions.find(v => v.isPrimary) || updatedVersions[0];

    await this.updateGarment(garmentId, {
      aiReports: updatedVersions,
      'AI Clothes Analysis': primaryVersion?.content || ''
    });
    return this.clothes.find(c => c.id === garmentId) || null;
  }

  public async updateGarmentAIReportVersion(
    garmentId: string,
    versionId: string,
    updates: { content?: string; title?: string; isPrimary?: boolean }
  ): Promise<ClothesItem | null> {
    const garment = this.clothes.find(c => c.id === garmentId);
    if (!garment || !Array.isArray(garment.aiReports)) return null;

    let currentVersions = garment.aiReports.map(v => {
      if (v.id === versionId) {
        return {
          ...v,
          ...(updates.content !== undefined ? { content: updates.content } : {}),
          ...(updates.title !== undefined ? { title: updates.title } : {}),
          ...(updates.isPrimary !== undefined ? { isPrimary: updates.isPrimary } : {})
        };
      }
      if (updates.isPrimary) {
        return { ...v, isPrimary: false };
      }
      return v;
    });

    const primaryVersion = currentVersions.find(v => v.isPrimary) || currentVersions[0];

    await this.updateGarment(garmentId, {
      aiReports: currentVersions,
      'AI Clothes Analysis': primaryVersion?.content || ''
    });
    return this.clothes.find(c => c.id === garmentId) || null;
  }

  public async deleteGarmentAIReportVersion(
    garmentId: string,
    versionId: string
  ): Promise<ClothesItem | null> {
    const garment = this.clothes.find(c => c.id === garmentId);
    if (!garment || !Array.isArray(garment.aiReports)) return null;

    let remaining = garment.aiReports.filter(v => v.id !== versionId);
    if (remaining.length > 0 && !remaining.some(v => v.isPrimary)) {
      remaining[0].isPrimary = true;
    }

    const primaryVersion = remaining.find(v => v.isPrimary) || remaining[0];

    await this.updateGarment(garmentId, {
      aiReports: remaining,
      'AI Clothes Analysis': primaryVersion ? primaryVersion.content : ''
    });
    return this.clothes.find(c => c.id === garmentId) || null;
  }

  public async setPrimaryGarmentAIReportVersion(
    garmentId: string,
    versionId: string
  ): Promise<ClothesItem | null> {
    return this.updateGarmentAIReportVersion(garmentId, versionId, { isPrimary: true });
  }

  // --- JUNCTION & JUNCTION MULTI-VERSION AI REPORTS ---
  public async addJunctionAIReportVersion(
    junctionId: string,
    version: { content: string; title?: string; model?: string; isPrimary?: boolean }
  ): Promise<ReportClothesJunction | null> {
    const junction = this.junctions.find(j => j.id === junctionId);
    if (!junction) return null;

    const existingVersions: AIReportVersion[] = Array.isArray(junction.aiReports) ? [...junction.aiReports] : [];
    const newVersionId = `junction_ai_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
    const shouldBePrimary = version.isPrimary !== undefined ? version.isPrimary : existingVersions.length === 0;

    const updatedVersions: AIReportVersion[] = existingVersions.map(v => ({
      ...v,
      isPrimary: shouldBePrimary ? false : Boolean(v.isPrimary)
    }));

    const newVersionEntry: AIReportVersion = {
      id: newVersionId,
      timestamp: new Date().toISOString(),
      title: version.title || `Report #${existingVersions.length + 1}`,
      model: version.model || 'gemini-3.6-flash',
      content: version.content,
      isPrimary: shouldBePrimary
    };

    updatedVersions.unshift(newVersionEntry);

    const primaryVersion = updatedVersions.find(v => v.isPrimary) || updatedVersions[0];

    return await this.updateJunction(junctionId, {
      aiReports: updatedVersions,
      'AI Junction Analysis': primaryVersion?.content || ''
    });
  }

  public async updateJunctionAIReportVersion(
    junctionId: string,
    versionId: string,
    updates: { content?: string; title?: string; isPrimary?: boolean }
  ): Promise<ReportClothesJunction | null> {
    const junction = this.junctions.find(j => j.id === junctionId);
    if (!junction || !Array.isArray(junction.aiReports)) return null;

    let currentVersions = junction.aiReports.map(v => {
      if (v.id === versionId) {
        return {
          ...v,
          ...(updates.content !== undefined ? { content: updates.content } : {}),
          ...(updates.title !== undefined ? { title: updates.title } : {}),
          ...(updates.isPrimary !== undefined ? { isPrimary: updates.isPrimary } : {})
        };
      }
      if (updates.isPrimary) {
        return { ...v, isPrimary: false };
      }
      return v;
    });

    const primaryVersion = currentVersions.find(v => v.isPrimary) || currentVersions[0];

    return await this.updateJunction(junctionId, {
      aiReports: currentVersions,
      'AI Junction Analysis': primaryVersion?.content || ''
    });
  }

  public async deleteJunctionAIReportVersion(
    junctionId: string,
    versionId: string
  ): Promise<ReportClothesJunction | null> {
    const junction = this.junctions.find(j => j.id === junctionId);
    if (!junction || !Array.isArray(junction.aiReports)) return null;

    let remaining = junction.aiReports.filter(v => v.id !== versionId);
    if (remaining.length > 0 && !remaining.some(v => v.isPrimary)) {
      remaining[0].isPrimary = true;
    }

    const primaryVersion = remaining.find(v => v.isPrimary) || remaining[0];

    return await this.updateJunction(junctionId, {
      aiReports: remaining,
      'AI Junction Analysis': primaryVersion ? primaryVersion.content : ''
    });
  }

  public async setPrimaryJunctionAIReportVersion(
    junctionId: string,
    versionId: string
  ): Promise<ReportClothesJunction | null> {
    return this.updateJunctionAIReportVersion(junctionId, versionId, { isPrimary: true });
  }
}

export const dataService = new DataService();
