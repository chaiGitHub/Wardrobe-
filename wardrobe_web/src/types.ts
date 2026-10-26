export interface PictureAttachment {
  id: string;
  name: string;
  filename?: string;
  url?: string;
  path?: string;
  storagePath?: string;
  originalUrl?: string;
  dataUrl?: string;
  mimetype?: string;
  presignedUrl?: string;
  smThumbnailUrl?: string;
  lgThumbnailUrl?: string;
  thumbnails?: {
    small?: { url: string; width?: number; height?: number };
    large?: { url: string; width?: number; height?: number };
  };
  width?: number;
  height?: number;
  size?: number;
  isLocalPendingSync?: boolean;
}

export interface CloudStoragePayloadRef {
  downloadUrl: string;
  storagePath: string;
  size: number;
  uploadedAt: string;
  isExternalStorage: true;
}

/**
 * Clothes specifications matching exact database field names and values.
 * Database table: clothes
 */
export interface Clothes {
  id: string;
  // Database fields
  'Name': string;
  'Clothes Type': 'Dress' | 'bra' | 'Tshirt' | 'Pant' | string;
  'material': string;
  'thick clothes'?: '01 not thick clothes' | '02 middle thick clothes' | '03 thick clothes' | 'not so thick bra' | 'quick thick material bra' | string;
  'Hot at noon'?: 'Not hot' | 'Middle' | 'Hot' | string;
  'diao dai'?: 'diao dai' | 'not diao dai' | 'wide diao dai' | string;
  'side breast ok'?: 'side breast shape ok' | 'side breast ok' | 'side breast out' | string;
  'side breast ok: with 聚拢 bra'?: string;
  'side breast ok: with side out bra'?: string;
  sideBreastOkWithGatherBra?: string;
  sideBreastOkWithSideOutBra?: string;
  'sit breast ok'?: 'sit breast shape ok' | 'stand breast ugly, because alot zhouwhen' | 'stand breast ugly, because loose tie not as white marry' | '?' | string;
  'sit breast ok: With 聚拢bra'?: string;
  'sit breast ok: with side out bra'?: string;
  sitBreastOkWithGatherBra?: string;
  sitBreastOkWithSideOutBra?: string;
  // Multi-scenario climate and sensory ergonomics
  'when with aircond: hot feeling'?: string;
  'when with aircond: leg feeling'?: string;
  'when without aircond: hot feeling'?: string;
  'when without aircond: leg feeling'?: string;
  'when sleep under fan: hot feeling'?: string;
  'when sleep under fan: leg feeling'?: string;
  'center chest'?: 'no center chest' | 'center chest' | 'not center but side open' | 'yes center chest bra' | string;
  'beautiful'?: 'Beautiful' | 'Middle beautiful' | 'not so beautiful' | string;
  'beatiful reason'?: string;
  'wet'?: 'dry' | 'little wet' | 'wet' | string;
  'wet reason'?: string;
  'Descriptions'?: string;
  'Descriptions 2'?: string;
  'price'?: number;
  'wearing frequency(how many times legacy washed)'?: number;
  'Tracked App Washes'?: number;
  'Total Washed Count'?: number;
  'Last Washed'?: string;
  'AI Clothes Analysis'?: string;
  'Pictures'?: PictureAttachment[];
  'may throw'?: 'may throw' | 'may throw due to waist and sit breast too ugly' | 'may throw, may no throw due to unique' | 'no' | string;
  'last comment or action'?: string;
  'tags'?: string[];
  'created'?: string;
  'long dress'?: 'short dress' | 'middle dress' | 'long dress' | string;
  'unwash time rollup'?: number;
  'wash stated (from Full Wearing Report (new design) clothes list (linked))'?: string[];
  'contentBlocks'?: import('./types/documentBlocks').DocumentContentBlock[];

  // Multiple versioned AI diagnostic reports for this garment
  aiReports?: AIReportVersion[];

  // Explanations / ergonomic notes per field (e.g. dress cutting, lace drying, diao dai pain)
  fieldExplanations?: Record<string, string>;

  // Dynamic custom attributes/fields (extensible over time by the user)
  customFields?: Record<string, any>;
  extraAttributes?: Record<string, any>;

  // Optional normalized accessors for UI compatibility
  name?: string;
  clothesType?: string;
  materialProp?: string;
  [key: string]: any;
}

// Alias for Clothes
export type ClothesItem = Clothes;

/**
 * Versioned AI report entries so users can generate, view, edit, and keep
 * multiple AI diagnostic reports per wearing session without losing previous versions.
 */
export interface AIReportVersion {
  id: string;
  timestamp: string; // ISO string
  title?: string;
  model?: string; // 'gemini-3.6-flash' | 'gemini-3.8-flash'
  content: string;
  isPrimary?: boolean;
}

/**
 * Wearing Report specifications matching exact database field names and values.
 * Database table: wearingReport
 * Each Wearing Report can have one or many Clothes Junction.
 */
export interface WearingReport {
  id: string;
  'Name': string;
  'wear date': string;
  'created'?: string;
  'categories'?: string[];
  'tags'?: string[];
  'shoulder feel'?: string;
  'mid breast'?: string;
  'night mid breast'?: string;
  'sweat'?: string;
  'night sweat'?: string;
  'leg feel'?: string;
  'night leg feel'?: string;
  'report detail'?: string;
  'AI Wearing Analysis'?: string;
  'Pictures'?: PictureAttachment[];

  // Field explanations and ergonomic notes for this wearing session
  fieldExplanations?: Record<string, string>;

  // Multiple versioned AI reports history for this wearing session
  aiReports?: AIReportVersion[];

  // Dynamic custom session observation fields (extensible over time, e.g. Weather, Room Temp)
  customFields?: Record<string, any>;
  extraAttributes?: Record<string, any>;

  // Optional normalized accessors
  name?: string;
  wearDate?: string;
  reportDetail?: string;
  shoulderFeel?: string;
  midBreast?: string;
  nightMidBreast?: string;
  sweatProp?: string;
  nightSweat?: string;
  legFeel?: string;
  nightLegFeel?: string;
  aiWearingAnalysis?: string;
  [key: string]: any;
}

/**
 * Clothes Junction matching exact database field names and values.
 * Database table: clothesJunction
 * Links a Wearing Report to exactly one Clothes item.
 */
export interface ClothesJunction {
  id: string;
  'name': number;
  'wearing report id': { id: string; title: string } | string;
  'clothes(new)': { id: string; title: string } | string;
  'duration': number; // in seconds, e.g. 14400 = 4 hours
  'wash stated': 'wash' | 'need wash' | string;
  'Clothes Type (from clothes(new))'?: string;
  'wash date'?: string;   // Exact field name: wash date (YYYY-MM-DD when washed)
  'washed date'?: string; // YYYY-MM-DD date when washed
  'wear date'?: string;   // YYYY-MM-DD wear date
  'AI Junction Analysis'?: string;

  // Multiple versioned AI reports for this specific garment wear pairing
  aiReports?: AIReportVersion[];

  // Normalized helpers
  clothesId?: string;
  clothesTitle?: string;
  wearingReportId?: string;
  wearingReportTitle?: string;
  washStated?: string;
  durationVal?: number;
  washDate?: string;
  washedDate?: string;
  wearDate?: string;
}

// Alias for ClothesJunction
export type ReportClothesJunction = ClothesJunction;

export interface ActivePreset {
  id: string;
  name: string;
  description: string;
  filter: (item: ClothesItem, junctionStats: { needWash: boolean; unwashedSec: number }) => boolean;
}

export interface ReportPreset {
  id: string;
  name: string;
  description: string;
  isCustom?: boolean;
  groupTiers: string[];
  sortField: string;
  sortOrder: 'asc' | 'desc';
  visibleFields: string[];
  updatedAt?: string;
}

export interface DrillStep {
  field: string;
  value: string;
}

