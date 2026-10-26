import { PictureAttachment } from '../types';
import { cacheImageBlob } from './localImageCache';

export interface StorageUploadResult {
  downloadUrl: string;
  storagePath: string;
  filename: string;
  size: number;
  contentType: string;
  uploadedAt: string;
}

export interface LargeTextPayloadReference {
  downloadUrl: string;
  storagePath: string;
  size: number;
  uploadedAt: string;
  isExternalStorage: true;
}

/**
 * Reads a File object into a base64 string without any lossy compression.
 */
function readFileAsBase64(file: File): Promise<{ base64: string; dataUrl: string }> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      const base64 = dataUrl.split(',')[1] || '';
      resolve({ base64, dataUrl });
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

/**
 * Uploads any File directly to local server storage (/uploads/...) and caches in IndexedDB.
 * 100% Local - zero reliance on external cloud storage.
 */
export async function uploadFileToStorage(
  file: File,
  storagePath: string,
  _customMetadata?: Record<string, string>,
  onProgress?: (percent: number) => void
): Promise<StorageUploadResult> {
  onProgress?.(10);

  // 1. Immediately cache in local IndexedDB so image works 100% offline
  try {
    await cacheImageBlob(storagePath, file);
  } catch (cacheErr) {
    console.warn('[LocalStorage] IndexedDB cache warning:', cacheErr);
  }

  // 2. Upload to local server disk endpoint (/api/storage/upload)
  try {
    onProgress?.(30);
    const { base64 } = await readFileAsBase64(file);
    onProgress?.(60);

    const response = await fetch('/api/storage/upload', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        storagePath,
        filename: file.name,
        contentType: file.type || 'image/jpeg',
        fileData: base64
      })
    });

    if (response.ok) {
      const data = await response.json();
      cacheImageBlob(data.downloadUrl, file).catch(() => {});
      onProgress?.(100);
      return {
        downloadUrl: data.downloadUrl,
        storagePath: data.storagePath || storagePath,
        filename: data.filename || file.name,
        size: data.size || file.size,
        contentType: data.contentType || file.type || 'image/jpeg',
        uploadedAt: data.uploadedAt || new Date().toISOString()
      };
    }
  } catch (serverErr) {
    console.warn('[LocalStorage] Server storage upload fallback to local blob:', serverErr);
  }

  // 3. Resilient browser offline fallback: local Blob URL
  const localBlobUrl = URL.createObjectURL(file);
  cacheImageBlob(localBlobUrl, file).catch(() => {});
  onProgress?.(100);

  return {
    downloadUrl: localBlobUrl,
    storagePath,
    filename: file.name,
    size: file.size,
    contentType: file.type || 'image/jpeg',
    uploadedAt: new Date().toISOString()
  };
}

/**
 * Uploads garment picture to local storage.
 * Instant local persistence with zero cloud dependencies.
 */
export async function uploadGarmentPhotoToStorage(
  clothesId: string,
  file: File,
  onProgress?: (percent: number) => void
): Promise<PictureAttachment> {
  const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
  const timestamp = Date.now();
  const storagePath = `garments/${clothesId}/photos/${timestamp}_${safeName}`;

  const result = await uploadFileToStorage(
    file,
    storagePath,
    { clothesId, category: 'garment_photo' },
    onProgress
  );

  const id = `pic_${timestamp}_${Math.random().toString(36).substring(2, 7)}`;

  return {
    id,
    name: file.name,
    filename: file.name,
    url: result.downloadUrl,
    originalUrl: result.downloadUrl,
    presignedUrl: result.downloadUrl,
    smThumbnailUrl: result.downloadUrl,
    lgThumbnailUrl: result.downloadUrl,
    storagePath: result.storagePath,
    mimetype: result.contentType,
    size: result.size
  };
}

/**
 * Uploads wearing report picture to local storage.
 */
export async function uploadReportPhotoToStorage(
  reportId: string,
  file: File,
  onProgress?: (percent: number) => void
): Promise<PictureAttachment> {
  const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
  const timestamp = Date.now();
  const storagePath = `reports/${reportId}/photos/${timestamp}_${safeName}`;

  const result = await uploadFileToStorage(
    file,
    storagePath,
    { reportId, category: 'wearing_report_photo' },
    onProgress
  );

  const id = `pic_${timestamp}_${Math.random().toString(36).substring(2, 7)}`;

  return {
    id,
    name: file.name,
    filename: file.name,
    url: result.downloadUrl,
    originalUrl: result.downloadUrl,
    presignedUrl: result.downloadUrl,
    smThumbnailUrl: result.downloadUrl,
    lgThumbnailUrl: result.downloadUrl,
    storagePath: result.storagePath,
    mimetype: result.contentType,
    size: result.size
  };
}

/**
 * Uploads text payloads to local storage.
 */
export async function uploadLargeTextPayloadToStorage(
  storagePath: string,
  textContent: string
): Promise<LargeTextPayloadReference> {
  try {
    const base64 = btoa(unescape(encodeURIComponent(textContent)));
    const response = await fetch('/api/storage/upload', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        storagePath,
        filename: storagePath.split('/').pop() || 'payload.md',
        contentType: 'text/markdown; charset=utf-8',
        fileData: base64
      })
    });

    if (response.ok) {
      const data = await response.json();
      return {
        downloadUrl: data.downloadUrl,
        storagePath: data.storagePath,
        size: data.size,
        uploadedAt: data.uploadedAt,
        isExternalStorage: true
      };
    }
  } catch (err) {
    console.warn('Local storage for text payload failed, using data URL fallback:', err);
  }

  return {
    downloadUrl: `data:text/markdown;charset=utf-8,${encodeURIComponent(textContent)}`,
    storagePath,
    size: textContent.length,
    uploadedAt: new Date().toISOString(),
    isExternalStorage: true
  };
}

/**
 * Fetches text content from storage download URL.
 */
export async function fetchTextPayloadFromStorage(downloadUrl: string): Promise<string> {
  if (downloadUrl.startsWith('data:')) {
    const commaIndex = downloadUrl.indexOf(',');
    return decodeURIComponent(downloadUrl.substring(commaIndex + 1));
  }
  const res = await fetch(downloadUrl);
  if (!res.ok) {
    throw new Error(`Failed to load text payload: ${res.statusText}`);
  }
  return await res.text();
}

/**
 * Deletes a file from local server storage.
 */
export async function deleteFileFromStorage(storagePath: string): Promise<void> {
  if (!storagePath) return;
  try {
    await fetch('/api/storage/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ storagePath })
    });
  } catch (err) {
    console.warn('Failed to delete from local storage:', err);
  }
}
