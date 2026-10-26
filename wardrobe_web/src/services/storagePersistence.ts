/**
 * iOS & Mobile Persistent Storage Enforcer
 * 
 * Invokes the StorageManager API (navigator.storage.persist()) to prevent
 * iOS Safari WebKit and mobile browsers from evicting IndexedDB and localStorage
 * caches when the device experiences low disk space.
 */

export interface StorageStatus {
  isPersisted: boolean;
  quota?: number;
  usage?: number;
  supported: boolean;
}

export async function requestPersistentStorage(): Promise<boolean> {
  if (typeof window === 'undefined' || !navigator.storage || !navigator.storage.persist) {
    console.log('[StoragePersistence] StorageManager API not supported in this browser.');
    return false;
  }

  try {
    const isAlreadyPersisted = await navigator.storage.persisted();
    if (isAlreadyPersisted) {
      console.log('[StoragePersistence] Storage is already marked as persistent.');
      return true;
    }

    const granted = await navigator.storage.persist();
    if (granted) {
      console.log('[StoragePersistence] Persistent storage GRANTED. App is immune to iOS low-storage eviction.');
    } else {
      console.log('[StoragePersistence] Persistent storage request was not granted by the browser.');
    }
    return granted;
  } catch (err) {
    console.warn('[StoragePersistence] Failed to request persistent storage:', err);
    return false;
  }
}

export async function getStorageEstimate(): Promise<StorageStatus> {
  if (typeof window === 'undefined' || !navigator.storage) {
    return { isPersisted: false, supported: false };
  }

  try {
    const isPersisted = navigator.storage.persisted ? await navigator.storage.persisted() : false;
    if (navigator.storage.estimate) {
      const estimate = await navigator.storage.estimate();
      return {
        isPersisted,
        quota: estimate.quota,
        usage: estimate.usage,
        supported: true
      };
    }
    return { isPersisted, supported: true };
  } catch {
    return { isPersisted: false, supported: false };
  }
}
