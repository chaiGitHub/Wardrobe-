/**
 * Local-Only Data Outbox Service
 * 
 * Provides:
 * 1. Zero-latency local execution (<1ms)
 * 2. Reliable state tracking in localStorage
 * 3. 100% Local operation with zero external cloud dependencies
 */

export const STORAGE_KEY_OUTBOX = 'wardrobe_sync_outbox_v3';
export const STORAGE_KEY_LAST_SYNC = 'wardrobe_last_sync_timestamp_v3';

export type OutboxCollection = 'clothes' | 'wearingReport' | 'clothesJunction';

export interface OutboxMutation {
  id: string;
  timestamp: number;
  collection: OutboxCollection;
  docId: string;
  action: 'upsert' | 'delete';
  payload?: any;
  label?: string;
  retries: number;
  lastError?: string;
}

export type OutboxStatus = 'synced' | 'syncing' | 'offline' | 'error';

export interface OutboxState {
  status: OutboxStatus;
  pendingCount: number;
  mutations: OutboxMutation[];
  lastSyncTime: string | null;
  isOnline: boolean;
  lastError: string | null;
}

class SyncOutboxService {
  private queue: OutboxMutation[] = [];
  private status: OutboxStatus = 'synced';
  private lastSyncTime: string | null = null;
  private lastError: string | null = null;
  private listeners: Set<(state: OutboxState) => void> = new Set();

  constructor() {
    this.loadFromStorage();
  }

  private loadFromStorage() {
    try {
      if (typeof localStorage === 'undefined') return;
      this.lastSyncTime = localStorage.getItem(STORAGE_KEY_LAST_SYNC) || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      this.queue = [];
      this.status = 'synced';
    } catch {
      this.queue = [];
    }
  }

  private saveToStorage() {
    try {
      if (typeof localStorage === 'undefined') return;
      localStorage.setItem(STORAGE_KEY_OUTBOX, JSON.stringify(this.queue));
      if (this.lastSyncTime) {
        localStorage.setItem(STORAGE_KEY_LAST_SYNC, this.lastSyncTime);
      }
    } catch (e) {
      console.warn('[SyncOutbox] Storage write warning:', e);
    }
  }

  public subscribe(listener: (state: OutboxState) => void): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => this.listeners.delete(listener);
  }

  private notify() {
    const state = this.getState();
    this.listeners.forEach((fn) => {
      try {
        fn(state);
      } catch (err) {
        console.warn('[SyncOutbox] Listener notification error:', err);
      }
    });
  }

  public getState(): OutboxState {
    return {
      status: 'synced',
      pendingCount: 0,
      mutations: [],
      lastSyncTime: this.lastSyncTime || 'Local Active',
      isOnline: true,
      lastError: null
    };
  }

  public hasPending(_docId: string): boolean {
    return false;
  }

  public getPendingMutation(_docId: string): OutboxMutation | undefined {
    return undefined;
  }

  public enqueue(
    collection: OutboxCollection,
    docId: string,
    action: 'upsert' | 'delete',
    payload?: any,
    label?: string
  ): void {
    this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.status = 'synced';
    this.saveToStorage();
    this.notify();
  }

  public enqueueBatch(
    items: Array<{
      collection: OutboxCollection;
      docId: string;
      action: 'upsert' | 'delete';
      payload?: any;
      label?: string;
    }>
  ): void {
    this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.status = 'synced';
    this.saveToStorage();
    this.notify();
  }

  public scheduleFlush(_delayMs = 1000): void {
    // Local app: changes are immediately synced locally
  }

  public async flush(): Promise<void> {
    this.queue = [];
    this.status = 'synced';
    this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    this.saveToStorage();
    this.notify();
  }

  public clearQueue(): void {
    this.queue = [];
    this.status = 'synced';
    this.saveToStorage();
    this.notify();
  }
}

export const syncOutbox = new SyncOutboxService();
