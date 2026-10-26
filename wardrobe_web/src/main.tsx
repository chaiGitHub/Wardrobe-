import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import { requestPersistentStorage } from './services/storagePersistence';

// Guard local IndexedDB & Web Storage against iOS low-space cache eviction
requestPersistentStorage().catch(() => {});

// Register PWA service worker for Android/Moto Edge 30 native app installability
if ('serviceWorker' in navigator && process.env.NODE_ENV !== 'test') {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch((err) => {
      console.warn('PWA service worker registration notice:', err);
    });
  });
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

