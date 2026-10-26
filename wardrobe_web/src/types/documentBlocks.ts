import { PictureAttachment } from '../types';

export type BlockType = 'markdown' | 'richtext' | 'table' | 'image' | 'field_mapping';

export interface DocumentTableData {
  headers: string[];
  rows: string[][];
}

export interface DocumentContentBlock {
  id: string;
  type: BlockType;
  title?: string;
  content: string; // Markdown, HTML rich-text, or plain text
  tableData?: DocumentTableData;
  images?: PictureAttachment[];
  metadata?: Record<string, any>;
  createdAt?: string;
  updatedAt?: string;
}

export type MappingProblemStatus =
  | 'matched'
  | 'type_mismatch'
  | 'enum_unrecognized'
  | 'size_warning'
  | 'oversized';

export interface FieldMappingProblem {
  fieldKey: string;
  fieldLabel: string;
  rawInput: any;
  rawInputType: string;
  dbExpectedType: 'string' | 'number' | 'string[]' | 'date' | 'enum';
  dbCurrentValue: any;
  status: MappingProblemStatus;
  statusMessage: string;
  parsedValue: any;
  sizeInBytes: number;
  firestoreLimitBytes: number; // 1,048,576 bytes (1MB)
  suggestedAction: 'parse_to_match_db' | 'amend_db_field' | 'store_as_content_block';
  allowedEnumValues?: string[];
}

export interface DataDimensionSummary {
  dimension1_UIInput: {
    format: 'string' | 'rich-text' | 'markdown' | 'table' | 'original-image';
    rawByteCount: number;
    description: string;
  };
  dimension2_Parser: {
    isConversionPossible: boolean;
    convertedType: string;
    sanitizedValue: any;
    compressionApplied: boolean;
  };
  dimension3_Database: {
    dbFieldName: string;
    dbSchemaType: string;
    firestoreDocumentBytes: number;
    firestoreMaxLimitBytes: number;
    isSafeForSync: boolean;
  };
}
