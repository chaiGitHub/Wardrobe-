export interface PictureAttachment {
  id: string;
  name: string;
  filename?: string;
  url?: string;
  path?: string;
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
}

/**
 * Garment specifications matching exact database field names and values.
 */
export interface ClothesItem {
  id: string;
  // Database fields
  'Name': string;
  'Clothes Type': 'Dress' | 'bra' | 'Tshirt' | 'Pant' | string;
  'material': string;
  'thick clothes'?: '01 not thick clothes' | '02 middle thick clothes' | '03 thick clothes' | 'not so thick bra' | 'quick thick material bra' | string;
  'Hot at noon'?: 'Not hot' | 'Middle' | 'Hot' | string;
  'diao dai'?: 'diao dai' | 'not diao dai' | 'wide diao dai' | string;
  'side breast ok'?: 'side breast shape ok' | 'side breast ok' | 'side breast out' | string;
  'sit breast ok'?: 'sit breast shape ok' | 'stand breast ugly, because alot zhouwhen' | 'stand breast ugly, because loose tie not as white marry' | '?' | string;
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

  // Optional normalized accessors for UI compatibility
  name?: string;
  clothesType?: string;
  materialProp?: string;
}

/**
 * Wearing report / session specifications matching exact database field names and values.
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
}

/**
 * Wearing report clothes junction matching exact database field names and values.
 */
export interface ReportClothesJunction {
  id: string;
  'name': number;
  'wearing report id': { id: string; title: string } | string;
  'clothes(new)': { id: string; title: string } | string;
  'duration': number; // in seconds, e.g. 14400 = 4 hours
  'wash stated': 'wash' | 'need wash' | string;
  'Clothes Type (from clothes(new))'?: string;

  // Normalized helpers
  clothesId?: string;
  clothesTitle?: string;
  wearingReportId?: string;
  wearingReportTitle?: string;
  washStated?: string;
  durationVal?: number;
}

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
}

export interface DrillStep {
  field: string;
  value: string;
}

