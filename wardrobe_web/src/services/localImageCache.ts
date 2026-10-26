/**
 * Local-First IndexedDB Image Cache & Browser Media Persistence
 * 
 * Provides permanent, high-capacity client-side binary storage for clothing photos.
 * Keeps photos available 100% offline and instantly accessible with 0ms network latency.
 */

const DB_NAME = 'wardrobe_media_cache_v1';
const STORE_NAME = 'image_blobs';
const DB_VERSION = 1;

let dbPromise: Promise<IDBDatabase> | null = null;

function getIDB(): Promise<IDBDatabase> {
  if (dbPromise) return dbPromise;

  dbPromise = new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || !window.indexedDB) {
      return reject(new Error('IndexedDB not supported in this environment'));
    }

    const request = window.indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME);
      }
    };

    request.onsuccess = () => {
      resolve(request.result);
    };

    request.onerror = () => {
      reject(request.error || new Error('Failed to open IndexedDB'));
    };
  });

  return dbPromise;
}

// In-memory Object URL cache for high-speed rendering
const objectUrlMemoryCache = new Map<string, string>();

// Event listeners for cache mutations and warm-up
const cacheListeners = new Set<() => void>();

export function onImageCacheUpdate(fn: () => void): () => void {
  cacheListeners.add(fn);
  return () => {
    cacheListeners.delete(fn);
  };
}

export function notifyImageCacheListeners() {
  cacheListeners.forEach((fn) => {
    try {
      fn();
    } catch (e) {
      console.warn('[LocalImageCache] Listener error:', e);
    }
  });
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('wardrobe_image_cache_updated'));
  }
}

/**
 * Stores multiple Blobs into IndexedDB in a single readwrite transaction.
 */
export async function cacheManyImageBlobs(entries: { key: string; blob: Blob }[]): Promise<void> {
  if (!entries.length) return;
  try {
    const idb = await getIDB();
    await new Promise<void>((resolve, reject) => {
      const tx = idb.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      for (const item of entries) {
        if (item.key && item.blob) {
          store.put(item.blob, item.key);
          const objUrl = URL.createObjectURL(item.blob);
          objectUrlMemoryCache.set(item.key, objUrl);
          const cleanKey = item.key.replace(/^\/+/, '').replace(/^uploads\//, '');
          if (cleanKey && cleanKey !== item.key) {
            objectUrlMemoryCache.set(cleanKey, objUrl);
            objectUrlMemoryCache.set(`/uploads/${cleanKey}`, objUrl);
          }
        }
      }
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
    notifyImageCacheListeners();
  } catch (err) {
    console.warn('[LocalImageCache] Batch cache warning:', err);
  }
}

/**
 * Stores a Blob or DataURL into IndexedDB under the given key (e.g. picture ID or storage path).
 */
export async function cacheImageBlob(key: string, data: Blob | string): Promise<string> {
  if (!key) return '';

  try {
    let blobToStore: Blob;

    if (typeof data === 'string') {
      if (data.startsWith('data:')) {
        // Convert DataURL to Blob to save massive RAM and disk space
        const response = await fetch(data);
        blobToStore = await response.blob();
      } else {
        // Just a URL string, nothing to cache in IDB
        return data;
      }
    } else {
      blobToStore = data;
    }

    const idb = await getIDB();
    await new Promise<void>((resolve, reject) => {
      const tx = idb.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.put(blobToStore, key);
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });

    // Create an instant in-memory Object URL
    const objUrl = URL.createObjectURL(blobToStore);
    objectUrlMemoryCache.set(key, objUrl);

    // Also populate under standard normalized keys
    const cleanKey = key.replace(/^\/+/, '').replace(/^uploads\//, '');
    if (cleanKey && cleanKey !== key) {
      objectUrlMemoryCache.set(cleanKey, objUrl);
      objectUrlMemoryCache.set(`/uploads/${cleanKey}`, objUrl);
    }

    // Try to cache in CacheStorage for instant PWA HTTP interception
    if (typeof window !== 'undefined' && 'caches' in window) {
      try {
        const cache = await window.caches.open('wardrobe-pwa-v2');
        const headers = { 'Content-Type': blobToStore.type || 'image/jpeg', 'Cache-Control': 'public, max-age=31536000' };
        if (cleanKey) {
          await cache.put(new Request(`/uploads/${cleanKey}`), new Response(blobToStore, { headers })).catch(() => {});
        }
      } catch {}
    }

    notifyImageCacheListeners();
    return objUrl;
  } catch (err) {
    console.warn('[LocalImageCache] Failed to store blob in IndexedDB:', err);
    return typeof data === 'string' ? data : '';
  }
}

/**
 * Retrieves a stored Blob from IndexedDB and returns a fast browser Object URL.
 */
export async function getCachedImageUrl(key: string): Promise<string | null> {
  if (!key) return null;

  // Check memory cache first (<1ms)
  if (objectUrlMemoryCache.has(key)) {
    return objectUrlMemoryCache.get(key)!;
  }

  const cleanKey = key.replace(/^\/+/, '').replace(/^uploads\//, '');
  if (cleanKey && objectUrlMemoryCache.has(cleanKey)) {
    return objectUrlMemoryCache.get(cleanKey)!;
  }
  if (cleanKey && objectUrlMemoryCache.has(`/uploads/${cleanKey}`)) {
    return objectUrlMemoryCache.get(`/uploads/${cleanKey}`)!;
  }

  try {
    const idb = await getIDB();
    const blob = await new Promise<Blob | undefined>((resolve, reject) => {
      const tx = idb.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const req = store.get(key);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });

    if (blob && blob instanceof Blob) {
      const objUrl = URL.createObjectURL(blob);
      objectUrlMemoryCache.set(key, objUrl);
      if (cleanKey) {
        objectUrlMemoryCache.set(cleanKey, objUrl);
        objectUrlMemoryCache.set(`/uploads/${cleanKey}`, objUrl);
      }
      return objUrl;
    }
    return null;
  } catch (err) {
    console.warn('[LocalImageCache] Failed to read blob from IndexedDB:', err);
    return null;
  }
}

/**
 * Synchronously returns an in-memory Object URL if already loaded.
 */
export function getSyncCachedImageUrl(key?: string): string | null {
  if (!key) return null;
  if (objectUrlMemoryCache.has(key)) {
    return objectUrlMemoryCache.get(key)!;
  }
  const cleanKey = key.replace(/^\/+/, '').replace(/^uploads\//, '');
  if (cleanKey) {
    if (objectUrlMemoryCache.has(cleanKey)) return objectUrlMemoryCache.get(cleanKey)!;
    if (objectUrlMemoryCache.has(`/uploads/${cleanKey}`)) return objectUrlMemoryCache.get(`/uploads/${cleanKey}`)!;
    if (objectUrlMemoryCache.has(`photos/${cleanKey}`)) return objectUrlMemoryCache.get(`photos/${cleanKey}`)!;
  }
  return null;
}

let initPromise: Promise<void> | null = null;

/**
 * Warms up all cached images from IndexedDB into RAM Object URLs.
 */
export async function ensureImageCacheLoaded(): Promise<void> {
  if (initPromise) return initPromise;

  initPromise = (async () => {
    if (typeof window === 'undefined' || !window.indexedDB) return;

    try {
      const map = await getAllCachedImages();
      let added = 0;
      map.forEach((blob, key) => {
        try {
          const objUrl = URL.createObjectURL(blob);
          objectUrlMemoryCache.set(key, objUrl);
          const cleanKey = key.replace(/^\/+/, '').replace(/^uploads\//, '');
          if (cleanKey && cleanKey !== key) {
            objectUrlMemoryCache.set(cleanKey, objUrl);
            objectUrlMemoryCache.set(`/uploads/${cleanKey}`, objUrl);
          }
          added++;
        } catch {}
      });

      if (added > 0) {
        notifyImageCacheListeners();
      }
    } catch (err) {
      console.warn('[LocalImageCache] Warm-up warning:', err);
    }
  })();

  return initPromise;
}

// Immediate eager background warm-up
if (typeof window !== 'undefined' && window.indexedDB) {
  ensureImageCacheLoaded().catch(() => {});
}

/**
 * Deletes a cached image from IndexedDB.
 */
export async function deleteCachedImage(key: string): Promise<void> {
  if (!key) return;

  if (objectUrlMemoryCache.has(key)) {
    try {
      URL.revokeObjectURL(objectUrlMemoryCache.get(key)!);
    } catch {}
    objectUrlMemoryCache.delete(key);
  }

  try {
    const idb = await getIDB();
    await new Promise<void>((resolve, reject) => {
      const tx = idb.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.delete(key);
      req.onsuccess = () => resolve();
      req.onerror = () => reject(req.error);
    });
    notifyImageCacheListeners();
  } catch (err) {
    console.warn('[LocalImageCache] Failed to delete blob from IndexedDB:', err);
  }
}

/**
 * Counts total cached images in IndexedDB.
 */
export async function countCachedImages(): Promise<number> {
  try {
    const idb = await getIDB();
    return await new Promise<number>((resolve) => {
      const tx = idb.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const req = store.count();
      req.onsuccess = () => resolve(req.result || 0);
      req.onerror = () => resolve(0);
    });
  } catch {
    return 0;
  }
}

/**
 * Retrieves all cached image blobs from IndexedDB.
 */
export async function getAllCachedImages(): Promise<Map<string, Blob>> {
  const result = new Map<string, Blob>();
  try {
    const idb = await getIDB();
    await new Promise<void>((resolve, reject) => {
      const tx = idb.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const req = store.openCursor();
      req.onsuccess = (e) => {
        const cursor = (e.target as IDBRequest).result as IDBCursorWithValue;
        if (cursor) {
          if (cursor.value instanceof Blob) {
            result.set(String(cursor.key), cursor.value);
          }
          cursor.continue();
        } else {
          resolve();
        }
      };
      req.onerror = () => reject(req.error);
    });
  } catch (err) {
    console.warn('[LocalImageCache] Failed to get all cached images:', err);
  }
  return result;
}
