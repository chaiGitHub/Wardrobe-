import { ClothesItem, PictureAttachment } from '../types';
import {
  DocumentContentBlock,
  FieldMappingProblem,
  MappingProblemStatus,
  DataDimensionSummary
} from '../types/documentBlocks';

export const FIRESTORE_MAX_DOCUMENT_BYTES = 1048576; // 1MB limit in Firestore
export const FIRESTORE_SAFE_WARNING_BYTES = 750000; // 750KB warning threshold

export interface KnownFieldSchema {
  key: keyof ClothesItem | string;
  label: string;
  expectedType: 'string' | 'number' | 'string[]' | 'date' | 'enum';
  allowedEnumValues?: string[];
  description?: string;
}

export const KNOWN_CLOTHES_FIELDS: KnownFieldSchema[] = [
  { key: 'Name', label: 'Name', expectedType: 'string' },
  {
    key: 'Clothes Type',
    label: 'Clothes Type',
    expectedType: 'enum',
    allowedEnumValues: ['Dress', 'bra', 'Tshirt', 'Pant', 'skirt', 'sweater', 'jacket', 'coat', 'top', 'blouse', 'cardigan', 'shorts', 'Other']
  },
  { key: 'material', label: 'material', expectedType: 'string' },
  {
    key: 'thick clothes',
    label: 'thick clothes',
    expectedType: 'enum',
    allowedEnumValues: ['01 not thick clothes', '02 middle thick clothes', '03 thick clothes', 'not so thick bra', 'quick thick material bra']
  },
  {
    key: 'Hot at noon',
    label: 'Hot at noon',
    expectedType: 'enum',
    allowedEnumValues: ['Not hot', 'Middle', 'Hot']
  },
  {
    key: 'diao dai',
    label: 'diao dai',
    expectedType: 'enum',
    allowedEnumValues: ['diao dai', 'not diao dai', 'wide diao dai']
  },
  {
    key: 'side breast ok',
    label: 'side breast ok',
    expectedType: 'enum',
    allowedEnumValues: ['side breast shape ok', 'side breast ok', 'side breast out']
  },
  {
    key: 'sit breast ok',
    label: 'sit breast ok',
    expectedType: 'enum',
    allowedEnumValues: ['sit breast shape ok', 'stand breast ugly, because alot zhouwhen', 'stand breast ugly, because loose tie not as white marry', '?']
  },
  {
    key: 'center chest',
    label: 'center chest',
    expectedType: 'enum',
    allowedEnumValues: ['no center chest', 'center chest', 'not center but side open', 'yes center chest bra']
  },
  {
    key: 'beautiful',
    label: 'beautiful',
    expectedType: 'enum',
    allowedEnumValues: ['Beautiful', 'Middle beautiful', 'not so beautiful']
  },
  { key: 'beatiful reason', label: 'beatiful reason', expectedType: 'string' },
  {
    key: 'wet',
    label: 'wet',
    expectedType: 'enum',
    allowedEnumValues: ['dry', 'little wet', 'wet']
  },
  { key: 'wet reason', label: 'wet reason', expectedType: 'string' },
  {
    key: 'long dress',
    label: 'long dress',
    expectedType: 'enum',
    allowedEnumValues: ['short dress', 'middle dress', 'long dress']
  },
  { key: 'price', label: 'price', expectedType: 'number' },
  {
    key: 'may throw',
    label: 'may throw',
    expectedType: 'enum',
    allowedEnumValues: ['may throw', 'may throw due to waist and sit breast too ugly', 'may throw, may no throw due to unique', 'no']
  },
  { key: 'Descriptions', label: 'Descriptions', expectedType: 'string' },
  { key: 'Descriptions 2', label: 'Descriptions 2', expectedType: 'string' },
  { key: 'last comment or action', label: 'last comment or action', expectedType: 'string' },
  { key: 'tags', label: 'tags', expectedType: 'string[]' },
  { key: 'wearing frequency(how many times legacy washed)', label: 'wearing frequency', expectedType: 'number' },
  { key: 'Tracked App Washes', label: 'Tracked App Washes', expectedType: 'number' },
  { key: 'Total Washed Count', label: 'Total Washed Count', expectedType: 'number' }
];

export class SchemaParserService {
  /**
   * Estimates the byte size of any JavaScript object when serialized to JSON
   */
  calculatePayloadBytes(data: any): number {
    try {
      const json = JSON.stringify(data);
      return new TextEncoder().encode(json).length;
    } catch {
      return 0;
    }
  }

  /**
   * Format byte count into human-readable string (e.g. "4.2 KB", "1.1 MB")
   */
  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  /**
   * Detect raw input type from JavaScript value
   */
  detectInputType(value: any): string {
    if (value === null || value === undefined) return 'empty';
    if (Array.isArray(value)) return 'array';
    if (typeof value === 'number') return 'number';
    if (typeof value === 'boolean') return 'boolean';
    if (typeof value === 'string') {
      const str = value.trim();
      if (!str) return 'empty';
      // Check if it's rich HTML
      if (/<[a-z][\s\S]*>/i.test(str)) return 'html/richtext';
      // Check if it's markdown table
      if (/\|.*\|.*\|/m.test(str)) return 'markdown_table';
      // Check if numeric string
      if (/^[$€¥]?\s*-?\d+(\.\d+)?\s*$/.test(str)) return 'numeric_string';
      return 'string';
    }
    if (typeof value === 'object') return 'object';
    return typeof value;
  }

  /**
   * Tries to parse a raw string/input to the target DB type
   */
  parseToDbType(rawInput: any, expectedType: 'string' | 'number' | 'string[]' | 'date' | 'enum'): {
    success: boolean;
    parsedValue: any;
    note: string;
  } {
    if (rawInput === null || rawInput === undefined || rawInput === '') {
      return { success: true, parsedValue: expectedType === 'number' ? undefined : '', note: 'Empty value' };
    }

    if (expectedType === 'number') {
      if (typeof rawInput === 'number') {
        return { success: true, parsedValue: rawInput, note: 'Valid number' };
      }
      if (typeof rawInput === 'string') {
        // Strip currency symbols, spaces, commas
        const cleaned = rawInput.replace(/[^0-9.-]/g, '');
        const num = parseFloat(cleaned);
        if (!isNaN(num)) {
          return { success: true, parsedValue: num, note: `Converted "${rawInput}" to number ${num}` };
        }
      }
      return { success: false, parsedValue: undefined, note: `Cannot parse "${rawInput}" as a valid number` };
    }

    if (expectedType === 'string[]') {
      if (Array.isArray(rawInput)) {
        return { success: true, parsedValue: rawInput.map(String), note: 'Array of strings' };
      }
      if (typeof rawInput === 'string') {
        // Split by comma or semicolon
        const split = rawInput.split(/[,;\n]+/).map(s => s.trim()).filter(Boolean);
        return { success: true, parsedValue: split, note: `Split string into array of ${split.length} items` };
      }
      return { success: true, parsedValue: [String(rawInput)], note: 'Wrapped in array' };
    }

    if (expectedType === 'enum') {
      const strVal = String(rawInput).trim();
      return { success: true, parsedValue: strVal, note: `String value: ${strVal}` };
    }

    // Default string
    if (typeof rawInput === 'string') {
      return { success: true, parsedValue: rawInput, note: 'Valid string' };
    }
    return { success: true, parsedValue: JSON.stringify(rawInput), note: 'Serialized to string' };
  }

  /**
   * Analyzes an entire item's fields and returns the Problem Map Table
   */
  analyzeItemFieldMappings(
    item: ClothesItem,
    pendingEdits?: Record<string, any>
  ): {
    problems: FieldMappingProblem[];
    totalDocumentBytes: number;
    syncLimitPercent: number;
    isSafeForFirestore: boolean;
  } {
    const totalBytes = this.calculatePayloadBytes(item);
    const syncLimitPercent = Math.min(100, Math.round((totalBytes / FIRESTORE_MAX_DOCUMENT_BYTES) * 100));
    const isSafeForFirestore = totalBytes < FIRESTORE_MAX_DOCUMENT_BYTES;

    const problems: FieldMappingProblem[] = [];

    KNOWN_CLOTHES_FIELDS.forEach(schema => {
      const fieldKey = schema.key;
      const rawVal = pendingEdits && fieldKey in pendingEdits
        ? pendingEdits[fieldKey]
        : (item as any)[fieldKey];

      const currentDbVal = (item as any)[fieldKey];
      const rawType = this.detectInputType(rawVal);
      const valBytes = this.calculatePayloadBytes({ [fieldKey]: rawVal });

      let status: MappingProblemStatus = 'matched';
      let statusMessage = 'Matches DB field specifications';
      let suggestedAction: FieldMappingProblem['suggestedAction'] = 'parse_to_match_db';

      const parseResult = this.parseToDbType(rawVal, schema.expectedType);

      // Check for type mismatch
      if (schema.expectedType === 'number') {
        if (typeof rawVal === 'string' && rawVal.trim() !== '') {
          if (!parseResult.success) {
            status = 'type_mismatch';
            statusMessage = `Expected number, but received text "${rawVal}".`;
          } else if (typeof currentDbVal === 'number' && typeof rawVal !== 'number') {
            status = 'type_mismatch';
            statusMessage = `String "${rawVal}" can be parsed to number ${parseResult.parsedValue}.`;
          }
        }
      }

      // Check for enum matching
      if (schema.expectedType === 'enum' && schema.allowedEnumValues && rawVal) {
        const strVal = String(rawVal).trim();
        const matchesAllowed = schema.allowedEnumValues.some(
          allowed => allowed.toLowerCase() === strVal.toLowerCase()
        );
        if (!matchesAllowed) {
          status = 'enum_unrecognized';
          statusMessage = `"${strVal}" is not in default DB enum list. Can be stored or added to schema.`;
          suggestedAction = 'amend_db_field';
        }
      }

      // Check for rich text / html in plain string field
      if (rawType === 'html/richtext' || rawType === 'markdown_table') {
        status = 'size_warning';
        statusMessage = `Contains rich-text / table markup. Best handled via Document Content Block.`;
        suggestedAction = 'store_as_content_block';
      }

      // Check for size limit warnings
      if (valBytes > 250000) { // single field over 250KB
        status = 'size_warning';
        statusMessage = `Field is large (${this.formatBytes(valBytes)}). Approaching single-field cloud safety threshold.`;
      }

      if (totalBytes > FIRESTORE_MAX_DOCUMENT_BYTES) {
        status = 'oversized';
        statusMessage = `Total document exceeds 1MB limit (${this.formatBytes(totalBytes)}). Firestore sync will fail.`;
      } else if (totalBytes > FIRESTORE_SAFE_WARNING_BYTES) {
        if (status === 'matched') {
          status = 'size_warning';
          statusMessage = `Total document is at ${syncLimitPercent}% of 1MB Firestore limit.`;
        }
      }

      problems.push({
        fieldKey: String(fieldKey),
        fieldLabel: schema.label,
        rawInput: rawVal,
        rawInputType: rawType,
        dbExpectedType: schema.expectedType,
        dbCurrentValue: currentDbVal,
        status,
        statusMessage,
        parsedValue: parseResult.parsedValue,
        sizeInBytes: valBytes,
        firestoreLimitBytes: FIRESTORE_MAX_DOCUMENT_BYTES,
        suggestedAction,
        allowedEnumValues: schema.allowedEnumValues
      });
    });

    return {
      problems,
      totalDocumentBytes: totalBytes,
      syncLimitPercent,
      isSafeForFirestore
    };
  }

  /**
   * Convert pasted markdown or HTML text into structured Document Content Blocks
   */
  parseContentIntoBlocks(rawContent: string, title?: string): DocumentContentBlock[] {
    const blocks: DocumentContentBlock[] = [];
    if (!rawContent || !rawContent.trim()) return blocks;

    const trimmed = rawContent.trim();

    // Check if it has a markdown table
    const tableRegex = /\|(.+)\|[\r\n]+\|([-: |]+)\|[\r\n]+((?:\|.*\|[\r\n]*)+)/g;
    let match: RegExpExecArray | null;

    let lastIndex = 0;
    while ((match = tableRegex.exec(trimmed)) !== null) {
      // Add preceding text as markdown block
      const preText = trimmed.substring(lastIndex, match.index).trim();
      if (preText) {
        blocks.push({
          id: `block_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
          type: 'markdown',
          title: title || 'Notes',
          content: preText
        });
      }

      // Parse table
      const headerLine = match[1];
      const headers = headerLine.split('|').map(s => s.trim()).filter(Boolean);
      const rowsRaw = match[3].trim().split('\n');
      const rows = rowsRaw.map(r => r.split('|').map(c => c.trim()).filter(Boolean));

      blocks.push({
        id: `block_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
        type: 'table',
        title: 'Extracted Table',
        content: match[0],
        tableData: { headers, rows }
      });

      lastIndex = match.index + match[0].length;
    }

    // Remaining text after last table
    const remaining = trimmed.substring(lastIndex).trim();
    if (remaining) {
      const isHtml = /<[a-z][\s\S]*>/i.test(remaining);
      blocks.push({
        id: `block_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
        type: isHtml ? 'richtext' : 'markdown',
        title: title || 'Specification Document',
        content: remaining
      });
    }

    return blocks;
  }
}

export const schemaParserService = new SchemaParserService();
