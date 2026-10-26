/**
 * Helper to reliably resolve image source URLs from any picture attachment format:
 * Firebase Cloud Storage, IndexedDB local cache, Teable records, local uploads, thumbnails, presigned URLs or data URLs.
 */

import { useState, useEffect } from 'react';
import { getSyncCachedImageUrl, onImageCacheUpdate, getCachedImageUrl } from '../services/localImageCache';

/**
 * Checks if a given URL is an Amazon S3 presigned URL that has already expired.
 */
export function isExpiredPresignedUrl(url: string): boolean {
  if (!url || typeof url !== 'string') return false;

  // Check if it's an AWS S3 URL with X-Amz-Date and X-Amz-Expires
  if (url.includes('X-Amz-Date=') && url.includes('X-Amz-Expires=')) {
    try {
      const matchDate = url.match(/X-Amz-Date=(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})Z/);
      const matchExpires = url.match(/X-Amz-Expires=(\d+)/);

      if (matchDate && matchExpires) {
        const year = parseInt(matchDate[1], 10);
        const month = parseInt(matchDate[2], 10) - 1;
        const day = parseInt(matchDate[3], 10);
        const hours = parseInt(matchDate[4], 10);
        const minutes = parseInt(matchDate[6] || matchDate[5], 10);
        const seconds = parseInt(matchDate[6], 10);

        const startDate = new Date(Date.UTC(year, month, day, hours, minutes, seconds));
        const expiresSec = parseInt(matchExpires[1], 10);
        const expiryDate = new Date(startDate.getTime() + expiresSec * 1000);

        return Date.now() > expiryDate.getTime();
      }
    } catch {
      return false;
    }
  }

  return false;
}

/**
 * Resolves the best available image source URL, prioritizing instant IndexedDB Object URLs.
 */
export function resolveImageSource(img: any): string {
  if (!img) return '';
  if (typeof img === 'string') {
    if (img.startsWith('blob:')) return img;
    const cached = getSyncCachedImageUrl(img);
    if (cached) return cached;
    return isExpiredPresignedUrl(img) ? '' : img;
  }

  // 1. Check in-memory IndexedDB Object URL cache for 0ms offline rendering across all key permutations
  const storageClean = img.storagePath ? img.storagePath.replace(/^\/+/, '').replace(/^uploads\//, '') : '';
  const cachedUrl =
    (storageClean ? (
      getSyncCachedImageUrl(storageClean) ||
      getSyncCachedImageUrl(`/uploads/${storageClean}`) ||
      getSyncCachedImageUrl(`photos/${storageClean}`) ||
      getSyncCachedImageUrl(`/uploads/photos/${storageClean}`)
    ) : null) ||
    (img.url ? getSyncCachedImageUrl(img.url) : null) ||
    getSyncCachedImageUrl(img.id) ||
    getSyncCachedImageUrl(img.filename) ||
    getSyncCachedImageUrl(img.name);

  if (cachedUrl) return cachedUrl;

  // Check if img.url is a stale blob URL from a previous browser session
  const isStaleBlob = typeof img.url === 'string' && img.url.startsWith('blob:') && !getSyncCachedImageUrl(img.url);

  // Construct canonical server upload URLs
  const storageServerUrl = storageClean ? `/uploads/${storageClean}` : null;
  const storagePhotosUrl = storageClean ? `/uploads/photos/${storageClean}` : null;

  // 2. Candidate URLs in order of preference
  const candidates: (string | undefined | null)[] = [
    isStaleBlob ? null : img.url,
    storageServerUrl,
    storagePhotosUrl,
    img.originalUrl,
    img.lgThumbnailUrl,
    img.smThumbnailUrl,
    img.thumbnails?.large?.url,
    img.thumbnails?.small?.url,
    img.dataUrl,
    img.presignedUrl,
    img.path,
    img.filename ? `/uploads/${img.filename}` : null,
    img.name ? `/uploads/${img.name}` : null,
    isStaleBlob ? img.url : null // last resort if blob
  ];

  for (const candidate of candidates) {
    if (candidate && typeof candidate === 'string') {
      if (isExpiredPresignedUrl(candidate)) {
        continue;
      }
      return candidate;
    }
  }

  return '';
}

/**
 * React hook that reactively resolves an image source URL and automatically updates
 * when IndexedDB image cache warms up in background.
 */
export function useResolvedImageUrl(img: any): string {
  const [url, setUrl] = useState<string>(() => resolveImageSource(img));

  useEffect(() => {
    const current = resolveImageSource(img);
    setUrl(current);

    // If initial sync lookup didn't find cached blob, attempt async lookup
    if (!current && img) {
      const storageClean = typeof img === 'object' && img.storagePath ? img.storagePath.replace(/^\/+/, '').replace(/^uploads\//, '') : '';
      const key = (typeof img === 'string' ? img : (storageClean || img.url || img.filename || img.name || img.id)) || '';
      if (key) {
        getCachedImageUrl(key).then(cached => {
          if (cached) setUrl(cached);
        }).catch(() => {});
      }
    }

    const unsub = onImageCacheUpdate(() => {
      setUrl(resolveImageSource(img));
    });

    return unsub;
  }, [img]);

  return url;
}

/**
 * Checks whether an attachment record has a photo (even if expired or needing re-upload).
 */
export function hasPhotoAttachment(img: any): boolean {
  if (!img) return false;
  if (typeof img === 'string') return img.length > 0;
  return Boolean(
    img.url ||
    img.storagePath ||
    img.originalUrl ||
    img.presignedUrl ||
    img.lgThumbnailUrl ||
    img.smThumbnailUrl ||
    img.dataUrl ||
    img.name ||
    img.filename ||
    img.path
  );
}

/**
 * Checks whether the photo source is known to be expired.
 * Note: If local storagePath or uploaded copy exists, it is NOT considered expired.
 */
export function isAttachmentExpired(img: any): boolean {
  if (!img) return false;
  if (typeof img === 'object' && (img.storagePath || (typeof img.url === 'string' && img.url.startsWith('/uploads/')))) {
    return false;
  }
  const rawUrl = typeof img === 'string' ? img : (img.url || img.presignedUrl || img.originalUrl || '');
  return isExpiredPresignedUrl(rawUrl);
}
