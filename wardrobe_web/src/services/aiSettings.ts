/**
 * Helper to manage Google AI Studio / Gemini API key.
 * Now supports saving permanently to server secret configuration (/data/secret_config.json)
 * and syncing with browser local storage, so the user NEVER has to key it in every time!
 */

import { useEffect, useState } from 'react';

const KEY_STORAGE = 'gemini_api_key_custom';
const MODEL_STORAGE = 'gemini_model_selected';
const AI_PAUSED_STORAGE = 'wardrobe_ai_generation_paused';

export type GeminiModelId = 'gemini-3.8-flash' | 'gemini-3.1-flash-lite';

export interface AIKeyStatus {
  hasKey: boolean;
  source: 'saved_secret' | 'env' | 'none';
  maskedKey: string;
  model: GeminiModelId;
  isAiPaused?: boolean;
}

let cachedStatus: AIKeyStatus = {
  hasKey: false,
  source: 'none',
  maskedKey: '',
  model: 'gemini-3.8-flash'
};

const listeners = new Set<(status: AIKeyStatus) => void>();

export function subscribeKeyStatus(listener: (status: AIKeyStatus) => void): () => void {
  listeners.add(listener);
  listener(cachedStatus);
  return () => {
    listeners.delete(listener);
  };
}

function notifyListeners() {
  listeners.forEach(fn => {
    try { fn(cachedStatus); } catch {}
  });
}

export async function fetchKeyStatus(): Promise<AIKeyStatus> {
  try {
    const res = await fetch('/api/ai/key-status');
    if (res.ok) {
      const data = await res.json();
      cachedStatus = {
        hasKey: Boolean(data.hasKey),
        source: data.source || 'none',
        maskedKey: data.maskedKey || '',
        model: (data.model === 'gemini-3.1-flash-lite' ? 'gemini-3.1-flash-lite' : 'gemini-3.8-flash')
      };
      notifyListeners();
      return cachedStatus;
    }
  } catch {}
  
  // Fallback to local storage check
  const local = getCustomApiKey();
  cachedStatus = {
    hasKey: Boolean(local),
    source: local ? 'saved_secret' : 'none',
    maskedKey: local && local.length > 8 ? `${local.slice(0, 4)}...${local.slice(-4)}` : (local ? '****' : ''),
    model: getSelectedModel()
  };
  notifyListeners();
  return cachedStatus;
}

// Immediately query server status upon module initialization
if (typeof window !== 'undefined') {
  setTimeout(() => {
    fetchKeyStatus();
  }, 100);
}

export function getSelectedModel(): GeminiModelId {
  if (typeof localStorage === 'undefined') return 'gemini-3.8-flash';
  const saved = localStorage.getItem(MODEL_STORAGE);
  if (saved === 'gemini-3.1-flash-lite') return 'gemini-3.1-flash-lite';
  if (saved === 'gemini-3.8-flash') return 'gemini-3.8-flash';
  // Legacy migration
  if (saved === 'gemini-3.6-flash' || saved === 'gemini-2.5-flash') {
    localStorage.setItem(MODEL_STORAGE, 'gemini-3.8-flash');
    return 'gemini-3.8-flash';
  }
  return 'gemini-3.8-flash';
}

export function setSelectedModel(model: GeminiModelId): void {
  if (typeof localStorage === 'undefined') return;
  localStorage.setItem(MODEL_STORAGE, model);
  cachedStatus.model = model;
  notifyListeners();
  // Also sync model choice to server secret config
  fetch('/api/ai/save-key', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ model })
  }).catch(() => {});
}

export function getCustomApiKey(): string | undefined {
  if (typeof localStorage === 'undefined') return undefined;
  const key = localStorage.getItem(KEY_STORAGE)?.trim();
  return key || undefined;
}

// -------------------------------------------------------------
// Global AI Generation Master Pause State
// Stops in-app Gemini generation, saves tokens & execution time.
// Defaults to true (paused) per user instruction.
// -------------------------------------------------------------

let cachedAiPaused: boolean = true;
if (typeof localStorage !== 'undefined') {
  const saved = localStorage.getItem(AI_PAUSED_STORAGE);
  if (saved !== null) {
    cachedAiPaused = saved === 'true';
  } else {
    // Default to true (paused)
    localStorage.setItem(AI_PAUSED_STORAGE, 'true');
    cachedAiPaused = true;
  }
}

const pauseListeners = new Set<(paused: boolean) => void>();

export function isAiGenerationPaused(): boolean {
  if (typeof localStorage !== 'undefined') {
    const saved = localStorage.getItem(AI_PAUSED_STORAGE);
    if (saved !== null) {
      return saved === 'true';
    }
  }
  return cachedAiPaused;
}

export function setAiGenerationPaused(paused: boolean): void {
  cachedAiPaused = paused;
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(AI_PAUSED_STORAGE, paused ? 'true' : 'false');
  }
  pauseListeners.forEach(fn => {
    try { fn(paused); } catch {}
  });
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('wardrobe_ai_pause_changed', { detail: { paused } }));
  }
  // Sync with server
  fetch('/api/ai/set-paused', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ paused })
  }).catch(() => {});
}

export function subscribeAiPaused(listener: (paused: boolean) => void): () => void {
  pauseListeners.add(listener);
  listener(isAiGenerationPaused());
  return () => {
    pauseListeners.delete(listener);
  };
}

export function useAiGenerationPaused(): boolean {
  const [paused, setPaused] = useState<boolean>(isAiGenerationPaused());

  useEffect(() => {
    const unsubscribe = subscribeAiPaused(setPaused);
    const handleEvent = (e: any) => {
      if (e?.detail?.paused !== undefined) {
        setPaused(e.detail.paused);
      } else {
        setPaused(isAiGenerationPaused());
      }
    };
    if (typeof window !== 'undefined') {
      window.addEventListener('wardrobe_ai_pause_changed', handleEvent);
    }
    return () => {
      unsubscribe();
      if (typeof window !== 'undefined') {
        window.removeEventListener('wardrobe_ai_pause_changed', handleEvent);
      }
    };
  }, []);

  return paused;
}

/**
 * Permanently save API key to server secret and local storage
 */
export async function saveApiKeyToSecret(key: string, model?: GeminiModelId): Promise<boolean> {
  const trimmed = key.trim();
  if (typeof localStorage !== 'undefined') {
    if (trimmed) {
      localStorage.setItem(KEY_STORAGE, trimmed);
    } else {
      localStorage.removeItem(KEY_STORAGE);
    }
  }

  try {
    const res = await fetch('/api/ai/save-key', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        apiKey: trimmed,
        model: model || getSelectedModel()
      })
    });
    await fetchKeyStatus();
    return res.ok;
  } catch {
    await fetchKeyStatus();
    return false;
  }
}

export function setCustomApiKey(key: string): void {
  saveApiKeyToSecret(key);
}

export async function clearCustomApiKey(): Promise<void> {
  if (typeof localStorage !== 'undefined') {
    localStorage.removeItem(KEY_STORAGE);
  }
  try {
    await fetch('/api/ai/delete-key', { method: 'POST' });
  } catch {}
  await fetchKeyStatus();
}

export async function testGeminiApiKey(apiKey?: string, model?: GeminiModelId): Promise<{ ok: boolean; message: string; model?: string; isPrepaymentDepleted?: boolean }> {
  try {
    const chosenModel = model || getSelectedModel();
    const effectiveKey = apiKey ? apiKey.trim() : (getCustomApiKey() || '');
    const res = await fetch('/api/ai/test-key', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ apiKey: effectiveKey, model: chosenModel })
    });
    const data = await res.json();
    if (res.ok && (data.status === 'ok' || data.ok)) {
      // If a valid key was passed and tested successfully, save it directly to secret!
      if (apiKey && apiKey.trim()) {
        await saveApiKeyToSecret(apiKey.trim(), chosenModel);
      }
      return { ok: true, message: data.reply || data.message || 'API connection verified successfully!', model: data.model };
    }
    const isPrepayment = !!data.isPrepaymentDepleted || data.error?.includes('prepayment') || data.error?.includes('402');
    return {
      ok: false,
      message: data.error || 'API Key verification failed.',
      isPrepaymentDepleted: isPrepayment
    };
  } catch (err: any) {
    return { ok: false, message: err?.message || 'Network error while testing key.' };
  }
}
