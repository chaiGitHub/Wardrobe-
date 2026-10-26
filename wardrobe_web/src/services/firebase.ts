/**
 * Local Application - Cloud services decoupled
 */

export const db = null;
export const auth = null;
export const storage = null;

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
  STORAGE_UPLOAD = 'storage_upload',
  STORAGE_DOWNLOAD = 'storage_download',
  STORAGE_DELETE = 'storage_delete',
}

export function handleFirestoreError(error: unknown, operationType: OperationType, path: string | null) {
  console.warn('Local Operation Info:', operationType, path, error);
  return { error: String(error), operationType, path, authInfo: {} };
}

export function sanitizeForFirestore<T = any>(obj: T): T {
  return obj;
}

export async function testFirestoreConnection(): Promise<boolean> {
  return true;
}
