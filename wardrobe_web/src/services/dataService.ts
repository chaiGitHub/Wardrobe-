import initialData from '../initialData.json';
import { ClothesItem, WearingReport, ReportClothesJunction, PictureAttachment } from '../types';
import { db, handleFirestoreError, OperationType, testFirestoreConnection, sanitizeForFirestore } from './firebase';
import { collection, doc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch } from 'firebase/firestore';

const STORAGE_KEY_CLOTHES = 'wardrobe_db_clothes_v2';
const STORAGE_KEY_REPORTS = 'wardrobe_db_reports_v2';
const STORAGE_KEY_JUNCTIONS = 'wardrobe_db_junctions_v2';
const STORAGE_KEY_SYNC_TIME = 'wardrobe_db_last_sync_v2';

export type SyncStatus = 'synced' | 'syncing' | 'offline' | 'error';

/**
 * Formats duration in seconds into a friendly string like "15h 11m" or "4h" or "45m".
 */
export function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) return '0h';
  const totalMins = Math.round(seconds / 60);
  const hours = Math.floor(totalMins / 60);
  const mins = totalMins % 60;
  if (hours > 0 && mins > 0) {
    return `${hours}h ${mins}m`;
  } else if (hours > 0) {
    return `${hours}h`;
  } else {
    return `${mins}m`;
  }
}

/**
 * Resizes and converts an uploaded user image to a high quality base64 data URL
 * so it displays instantly and saves reliably to Firestore & LocalStorage.
 * Supports original quality mode to preserve maximum photographic fidelity.
 */
export async function processImageFile(
  file: File,
  options?: { originalQuality?: boolean }
): Promise<PictureAttachment> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const result = e.target?.result as string;

      // If original quality is requested and file is under 1.2MB, keep original dataUrl directly
      if (options?.originalQuality && file.size <= 1.2 * 1024 * 1024) {
        const id = `pic_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
        resolve({
          id,
          name: file.name,
          filename: file.name,
          url: result,
          originalUrl: result,
          dataUrl: result,
          presignedUrl: result,
          smThumbnailUrl: result,
          lgThumbnailUrl: result,
          thumbnails: {
            small: { url: result },
            large: { url: result }
          },
          mimetype: file.type || 'image/jpeg',
          size: file.size
        });
        return;
      }

      const img = new Image();
      img.onload = () => {
        // In original quality mode, allow up to 2560px resolution with 0.94 quality
        const maxDim = options?.originalQuality ? 2560 : 1600;
        let width = img.width;
        let height = img.height;
        if (width > maxDim || height > maxDim) {
          if (width > height) {
            height = Math.round((height * maxDim) / width);
            width = maxDim;
          } else {
            width = Math.round((width * maxDim) / height);
            height = maxDim;
          }
        }
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          const id = `pic_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
          resolve({
            id,
            name: file.name,
            filename: file.name,
            url: result,
            originalUrl: result,
            dataUrl: result,
            presignedUrl: result,
            smThumbnailUrl: result,
            lgThumbnailUrl: result,
            thumbnails: {
              small: { url: result },
              large: { url: result }
            },
            width: img.width,
            height: img.height,
            mimetype: file.type || 'image/jpeg',
            size: file.size
          });
          return;
        }
        ctx.drawImage(img, 0, 0, width, height);
        const quality = options?.originalQuality ? 0.94 : 0.88;
        const processedDataUrl = canvas.toDataURL('image/jpeg', quality);
        const id = `pic_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
        resolve({
          id,
          name: file.name,
          filename: file.name,
          url: processedDataUrl,
          originalUrl: processedDataUrl,
          dataUrl: processedDataUrl,
          presignedUrl: processedDataUrl,
          smThumbnailUrl: processedDataUrl,
          lgThumbnailUrl: processedDataUrl,
          thumbnails: {
            small: { url: processedDataUrl },
            large: { url: processedDataUrl }
          },
          width,
          height,
          mimetype: 'image/jpeg',
          size: Math.round(processedDataUrl.length * 0.75)
        });
      };
      img.onerror = () => {
        const id = `pic_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
        resolve({
          id,
          name: file.name,
          filename: file.name,
          url: result,
          originalUrl: result,
          dataUrl: result,
          presignedUrl: result,
          smThumbnailUrl: result,
          lgThumbnailUrl: result,
          thumbnails: {
            small: { url: result },
            large: { url: result }
          },
          mimetype: file.type,
          size: file.size
        });
      };
      img.src = result;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

// Standardize Clothes with exact DB field names and values
export function normalizeGarment(r: any): ClothesItem {
  const f = r.fields || r;
  const id = r.id || f.id || `garment_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  
  const garment: ClothesItem = {
    id,
    'Name': f['Name'] || f.name || 'Untitled Garment',
    'Clothes Type': f['Clothes Type'] || f.clothesType || 'Dress',
    'material': f['material'] || 'Chiffon',
    'thick clothes': f['thick clothes'] || f.thickClothes || '01 not thick clothes',
    'Hot at noon': f['Hot at noon'] || f.hotAtNoon || 'Not hot',
    'diao dai': f['diao dai'] || f.diaoDai || 'not diao dai',
    'side breast ok': f['side breast ok'] || f.sideBreastOk || 'side breast shape ok',
    'sit breast ok': f['sit breast ok'] || f.sitBreastOk || 'sit breast shape ok',
    'center chest': f['center chest'] || f.centerChest || 'no center chest',
    'beautiful': f['beautiful'] || f.beautiful || 'Beautiful',
    'beatiful reason': f['beatiful reason'] || f.beatifulReason || '',
    'wet': f['wet'] || f.wet || 'dry',
    'wet reason': f['wet reason'] || f.wetReason || '',
    'Descriptions': f['Descriptions'] || f.descriptions || '',
    'Descriptions 2': f['Descriptions 2'] || f.descriptions2 || '',
    'price': typeof f['price'] === 'number' ? f['price'] : undefined,
    'wearing frequency(how many times legacy washed)': typeof f['wearing frequency(how many times legacy washed)'] === 'number'
      ? f['wearing frequency(how many times legacy washed)']
      : (typeof f.wearingFrequency === 'number' ? f.wearingFrequency : 0),
    'Tracked App Washes': typeof f['Tracked App Washes'] === 'number'
      ? f['Tracked App Washes']
      : (typeof f.trackedAppWashes === 'number' ? f.trackedAppWashes : 0),
    'Total Washed Count': typeof f['Total Washed Count'] === 'number'
      ? f['Total Washed Count']
      : (typeof f.totalWashedCount === 'number' ? f.totalWashedCount : 0),
    'Last Washed': f['Last Washed'] || f.lastWashed || '',
    'AI Clothes Analysis': f['AI Clothes Analysis'] || f.aiClothesAnalysis || '',
    'Pictures': Array.isArray(f['Pictures']) ? f['Pictures'] : (Array.isArray(f.pictures) ? f.pictures : []),
    'may throw': f['may throw'] || f.mayThrow || '',
    'last comment or action': f['last comment or action'] || f.lastCommentOrAction || '',
    'tags': Array.isArray(f['tags']) ? f['tags'] : (Array.isArray(f.tags) ? f.tags : []),
    'created': f['created'] || f.created || new Date().toISOString(),
    'long dress': f['long dress'] || f.longDress || 'short dress',
    'unwash time rollup': typeof f['unwash time rollup'] === 'number' ? f['unwash time rollup'] : 0,
    'wash stated (from Full Wearing Report (new design) clothes list (linked))': Array.isArray(f['wash stated (from Full Wearing Report (new design) clothes list (linked))'])
      ? f['wash stated (from Full Wearing Report (new design) clothes list (linked))']
      : []
  };

  // Attach normalized aliases for legacy component access
  garment.name = garment['Name'];
  garment.clothesType = garment['Clothes Type'];
  garment.materialProp = garment['material'];
  return garment;
}

// Standardize Reports with exact DB field names and values
export function normalizeReport(r: any): WearingReport {
  const f = r.fields || r;
  const id = r.id || f.id || `report_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;

  const report: WearingReport = {
    id,
    'Name': f['Name'] || f.name || `${new Date().toISOString().slice(0, 10)} wearing report`,
    'wear date': f['wear date'] || f.wearDate || f['created'] || new Date().toISOString(),
    'created': f['created'] || f.created || new Date().toISOString(),
    'categories': Array.isArray(f['categories']) ? f['categories'] : (Array.isArray(f.categories) ? f.categories : ['[[Notes]]', '[[Wearing Report]]']),
    'tags': Array.isArray(f['tags']) ? f['tags'] : (Array.isArray(f.tags) ? f.tags : ['#journal', '#wearingreport']),
    'shoulder feel': f['shoulder feel'] || f.shoulderFeel || '',
    'mid breast': f['mid breast'] || f.midBreast || '',
    'night mid breast': f['night mid breast'] || f.nightMidBreast || '',
    'sweat': f['sweat'] || f.sweat || '',
    'night sweat': f['night sweat'] || f.nightSweat || '',
    'leg feel': f['leg feel'] || f.legFeel || '',
    'night leg feel': f['night leg feel'] || f.nightLegFeel || '',
    'report detail': f['report detail'] || f.reportDetail || '',
    'AI Wearing Analysis': f['AI Wearing Analysis'] || f.aiWearingAnalysis || '',
    'Pictures': Array.isArray(f['Pictures']) ? f['Pictures'] : (Array.isArray(f.pictures) ? f.pictures : [])
  };

  report.name = report['Name'];
  report.wearDate = report['wear date'];
  report.reportDetail = report['report detail'];
  report.shoulderFeel = report['shoulder feel'];
  report.midBreast = report['mid breast'];
  report.nightMidBreast = report['night mid breast'];
  report.sweatProp = report['sweat'];
  report.nightSweat = report['night sweat'];
  report.legFeel = report['leg feel'];
  report.nightLegFeel = report['night leg feel'];
  report.aiWearingAnalysis = report['AI Wearing Analysis'];
  return report;
}

// Standardize Report Clothes Junctions with exact DB field names
export function normalizeJunction(r: any): ReportClothesJunction {
  const f = r.fields || r;
  const id = r.id || f.id || `junction_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;

  const repIdObj = f['wearing report id'] || f.wearingReportId;
  const clothesObj = f['clothes(new)'] || f.clothesId;

  const junction: ReportClothesJunction = {
    id,
    'name': typeof f['name'] === 'number' ? f['name'] : (typeof f.name === 'number' ? f.name : 1),
    'wearing report id': typeof repIdObj === 'object' && repIdObj !== null ? repIdObj : { id: String(repIdObj || ''), title: f.wearingReportTitle || '' },
    'clothes(new)': typeof clothesObj === 'object' && clothesObj !== null ? clothesObj : { id: String(clothesObj || ''), title: f.clothesTitle || '' },
    'duration': typeof f['duration'] === 'number' ? f['duration'] : (typeof f.duration === 'number' ? f.duration : 14400),
    'wash stated': f['wash stated'] || f.washStated || 'wash',
    'Clothes Type (from clothes(new))': f['Clothes Type (from clothes(new))'] || f.clothesType || 'Dress'
  };

  junction.clothesId = typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].id : String(junction['clothes(new)']);
  junction.clothesTitle = typeof junction['clothes(new)'] === 'object' ? junction['clothes(new)'].title : '';
  junction.wearingReportId = typeof junction['wearing report id'] === 'object' ? junction['wearing report id'].id : String(junction['wearing report id']);
  junction.wearingReportTitle = typeof junction['wearing report id'] === 'object' ? junction['wearing report id'].title : '';
  junction.washStated = junction['wash stated'];
  junction.durationVal = junction['duration'];
  return junction;
}

export class DataService {
  private clothes: ClothesItem[] = [];
  private reports: WearingReport[] = [];
  private junctions: ReportClothesJunction[] = [];
  private syncStatus: SyncStatus = 'syncing';
  private lastSyncTime: string | null = null;
  private listeners: (() => void)[] = [];

  constructor() {
    this.init();
  }

  private async init() {
    // 1. Local-First: Load immediately from LocalStorage
    try {
      const storedClothes = localStorage.getItem(STORAGE_KEY_CLOTHES);
      const storedReports = localStorage.getItem(STORAGE_KEY_REPORTS);
      const storedJunctions = localStorage.getItem(STORAGE_KEY_JUNCTIONS);
      this.lastSyncTime = localStorage.getItem(STORAGE_KEY_SYNC_TIME);

      if (storedClothes && storedReports && storedJunctions) {
        this.clothes = JSON.parse(storedClothes).map(normalizeGarment);
        this.reports = JSON.parse(storedReports).map(normalizeReport);
        this.junctions = JSON.parse(storedJunctions).map(normalizeJunction);
      } else {
        this.resetToSeed(false);
      }
    } catch {
      this.resetToSeed(false);
    }

    this.notify();

    // 2. Connect to Google Firestore in background
    this.syncWithFirestore();
  }

  public resetToSeed(save = true) {
    this.clothes = (initialData.clothes || []).map(normalizeGarment);
    this.reports = (initialData.reports || []).map(normalizeReport);
    this.junctions = (initialData.reportClothes || []).map(normalizeJunction);
    if (save) {
      this.persistLocal();
      this.notify();
    }
  }

  private persistLocal() {
    try {
      localStorage.setItem(STORAGE_KEY_CLOTHES, JSON.stringify(this.clothes));
      localStorage.setItem(STORAGE_KEY_REPORTS, JSON.stringify(this.reports));
      localStorage.setItem(STORAGE_KEY_JUNCTIONS, JSON.stringify(this.junctions));
      if (this.lastSyncTime) {
        localStorage.setItem(STORAGE_KEY_SYNC_TIME, this.lastSyncTime);
      }
    } catch (e) {
      console.warn('Local storage write warning', e);
    }
  }

  public subscribe(fn: () => void) {
    this.listeners.push(fn);
    return () => {
      this.listeners = this.listeners.filter(l => l !== fn);
    };
  }

  private notify() {
    this.listeners.forEach(fn => fn());
  }

  public getSyncStatus(): { status: SyncStatus; lastSync: string | null } {
    return {
      status: this.syncStatus,
      lastSync: this.lastSyncTime
    };
  }

  /**
   * Bi-directional Local-First sync with Google Firestore.
   */
  public async syncWithFirestore(): Promise<void> {
    this.syncStatus = 'syncing';
    this.notify();

    try {
      const isOnline = await testFirestoreConnection();
      if (!isOnline) {
        this.syncStatus = 'offline';
        this.notify();
        return;
      }

      // 1. Fetch remote collections
      const clothesColl = collection(db, 'clothes');
      const reportsColl = collection(db, 'wearing_reports');
      const junctionsColl = collection(db, 'report_clothes_junctions');

      let clothesSnap, reportsSnap, junctionsSnap;
      try {
        clothesSnap = await getDocs(clothesColl);
        reportsSnap = await getDocs(reportsColl);
        junctionsSnap = await getDocs(junctionsColl);
      } catch (err) {
        handleFirestoreError(err, OperationType.GET, 'collections');
        this.syncStatus = 'offline';
        this.notify();
        return;
      }

      // If Firestore is empty, seed it with local data!
      if (clothesSnap.empty && this.clothes.length > 0) {
        console.log('Seeding initial clothes to Firestore...');
        const batch = writeBatch(db);
        this.clothes.forEach(item => {
          const docRef = doc(db, 'clothes', item.id);
          batch.set(docRef, sanitizeForFirestore(item));
        });
        await batch.commit();

        const reportBatch = writeBatch(db);
        this.reports.forEach(item => {
          const docRef = doc(db, 'wearing_reports', item.id);
          reportBatch.set(docRef, sanitizeForFirestore(item));
        });
        await reportBatch.commit();

        // Junctions in chunks of 200
        for (let i = 0; i < this.junctions.length; i += 200) {
          const chunk = this.junctions.slice(i, i + 200);
          const jBatch = writeBatch(db);
          chunk.forEach(j => {
            const docRef = doc(db, 'report_clothes_junctions', j.id);
            jBatch.set(docRef, sanitizeForFirestore(j));
          });
          await jBatch.commit();
        }
      } else if (!clothesSnap.empty) {
        // Merge remote items into local
        const remoteClothes = clothesSnap.docs.map(d => normalizeGarment({ id: d.id, ...d.data() }));
        const remoteReports = reportsSnap.docs.map(d => normalizeReport({ id: d.id, ...d.data() }));
        const remoteJunctions = junctionsSnap.docs.map(d => normalizeJunction({ id: d.id, ...d.data() }));

        if (remoteClothes.length > 0) {
          // Merge preserving local changes if any
          const clothesMap = new Map(this.clothes.map(c => [c.id, c]));
          remoteClothes.forEach(rc => clothesMap.set(rc.id, rc));
          this.clothes = Array.from(clothesMap.values());
        }

        if (remoteReports.length > 0) {
          const reportsMap = new Map(this.reports.map(r => [r.id, r]));
          remoteReports.forEach(rr => reportsMap.set(rr.id, rr));
          this.reports = Array.from(reportsMap.values());
        }

        if (remoteJunctions.length > 0) {
          const juncMap = new Map(this.junctions.map(j => [j.id, j]));
          remoteJunctions.forEach(rj => juncMap.set(rj.id, rj));
          this.junctions = Array.from(juncMap.values());
        }
      }

      this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      this.syncStatus = 'synced';
      this.persistLocal();
      this.notify();
    } catch (e) {
      console.error('Error syncing with Firestore:', e);
      this.syncStatus = 'error';
      this.notify();
    }
  }

  /**
   * Explicit Pull: Fetches fresh data from Firestore and updates local state.
   */
  public async pullFromCloud(
    onProgress?: (step: string, percent: number) => void
  ): Promise<{ clothes: number; reports: number; junctions: number }> {
    this.syncStatus = 'syncing';
    this.notify();

    try {
      onProgress?.('Fetching wardrobe records from cloud...', 20);
      const clothesColl = collection(db, 'clothes');
      const reportsColl = collection(db, 'wearing_reports');
      const junctionsColl = collection(db, 'report_clothes_junctions');

      onProgress?.('Reading documents and history...', 45);
      const [clothesSnap, reportsSnap, junctionsSnap] = await Promise.all([
        getDocs(clothesColl),
        getDocs(reportsColl),
        getDocs(junctionsColl)
      ]);

      onProgress?.('Processing records and images...', 75);
      const remoteClothes = clothesSnap.docs.map(d => normalizeGarment({ id: d.id, ...d.data() }));
      const remoteReports = reportsSnap.docs.map(d => normalizeReport({ id: d.id, ...d.data() }));
      const remoteJunctions = junctionsSnap.docs.map(d => normalizeJunction({ id: d.id, ...d.data() }));

      if (remoteClothes.length > 0) {
        this.clothes = remoteClothes;
      }
      if (remoteReports.length > 0) {
        this.reports = remoteReports;
      }
      if (remoteJunctions.length > 0) {
        this.junctions = remoteJunctions;
      }

      onProgress?.('Updating local cache...', 95);
      this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      this.syncStatus = 'synced';
      this.persistLocal();
      this.notify();

      onProgress?.('Pull completed successfully!', 100);

      return {
        clothes: remoteClothes.length,
        reports: remoteReports.length,
        junctions: remoteJunctions.length
      };
    } catch (e) {
      console.error('Pull from Firestore error:', e);
      this.syncStatus = 'error';
      this.notify();
      throw e;
    }
  }

  /**
   * Explicit Push: Uploads all local clothes, wearing reports, and junctions to Firestore.
   */
  public async pushToCloud(
    onProgress?: (step: string, percent: number) => void
  ): Promise<{ clothes: number; reports: number; junctions: number }> {
    this.syncStatus = 'syncing';
    this.notify();

    try {
      onProgress?.('Preparing upload...', 10);

      // 1. Push Clothes in batches of 200
      for (let i = 0; i < this.clothes.length; i += 200) {
        const chunk = this.clothes.slice(i, i + 200);
        const batch = writeBatch(db);
        chunk.forEach(item => {
          const docRef = doc(db, 'clothes', item.id);
          batch.set(docRef, sanitizeForFirestore(item), { merge: true });
        });
        await batch.commit();
      }
      onProgress?.('Uploaded garments to Firestore...', 45);

      // 2. Push Reports in batches of 200
      for (let i = 0; i < this.reports.length; i += 200) {
        const chunk = this.reports.slice(i, i + 200);
        const batch = writeBatch(db);
        chunk.forEach(item => {
          const docRef = doc(db, 'wearing_reports', item.id);
          batch.set(docRef, sanitizeForFirestore(item), { merge: true });
        });
        await batch.commit();
      }
      onProgress?.('Uploaded wearing reports...', 80);

      // 3. Push Junctions in batches of 200
      for (let i = 0; i < this.junctions.length; i += 200) {
        const chunk = this.junctions.slice(i, i + 200);
        const batch = writeBatch(db);
        chunk.forEach(item => {
          const docRef = doc(db, 'report_clothes_junctions', item.id);
          batch.set(docRef, sanitizeForFirestore(item), { merge: true });
        });
        await batch.commit();
      }
      onProgress?.('Finalizing sync...', 95);

      this.lastSyncTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      this.syncStatus = 'synced';
      this.notify();

      onProgress?.('Push completed successfully!', 100);

      return {
        clothes: this.clothes.length,
        reports: this.reports.length,
        junctions: this.junctions.length
      };
    } catch (e) {
      console.error('Push to Firestore error:', e);
      this.syncStatus = 'error';
      this.notify();
      throw e;
    }
  }

  /**
   * Scans all database records to gather previously entered unique values with their usage frequency.
   * Matches the Teable previous value selector design.
   */
  public getFieldPreviousValues(field: string, target: 'clothes' | 'reports' = 'clothes'): Array<{ value: string; count: number }> {
    const counts = new Map<string, number>();
    const items = target === 'clothes' ? this.clothes : this.reports;

    items.forEach((item: any) => {
      const val = item[field];
      if (Array.isArray(val)) {
        val.forEach(v => {
          if (typeof v === 'string') {
            const str = v.trim();
            if (str && str !== 'No data' && str !== 'N/A') {
              counts.set(str, (counts.get(str) || 0) + 1);
            }
          }
        });
      } else if (typeof val === 'string') {
        const str = val.trim();
        if (str && str !== 'No data' && str !== 'N/A') {
          counts.set(str, (counts.get(str) || 0) + 1);
        }
      }
    });

    return Array.from(counts.entries())
      .map(([value, count]) => ({ value, count }))
      .sort((a, b) => b.count - a.count);
  }

  // Getters
  public getClothes(): ClothesItem[] {
    return this.clothes;
  }

  public getReports(): WearingReport[] {
    return [...this.reports].sort((a, b) => {
      const dateA = new Date(a['wear date'] || a.created || 0).getTime();
      const dateB = new Date(b['wear date'] || b.created || 0).getTime();
      return dateB - dateA;
    });
  }

  public getJunctions(): ReportClothesJunction[] {
    return this.junctions;
  }

  public getGarmentStats(clothesId: string) {
    const itemJunctions = this.junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId === clothesId;
    });
    const needWashJunctions = itemJunctions.filter(j => j['wash stated'] === 'need wash');
    const needWash = needWashJunctions.length > 0;
    const unwashedSeconds = needWashJunctions.reduce((acc, j) => acc + (j['duration'] || 0), 0);
    const totalWears = itemJunctions.length;
    const totalDurationHours = Math.round(itemJunctions.reduce((acc, j) => acc + (j['duration'] || 0), 0) / 3600);

    return {
      needWash,
      unwashedSeconds,
      unwashedHours: (unwashedSeconds / 3600).toFixed(1),
      totalWears,
      totalDurationHours
    };
  }

  public getLaundryQueue() {
    return this.clothes
      .map(c => ({
        garment: c,
        stats: this.getGarmentStats(c.id)
      }))
      .filter(entry => entry.stats.needWash);
  }

  // Local-First Mutations with Async Firestore persistence
  public async markGarmentWashed(clothesId: string) {
    let changed = false;

    this.junctions = this.junctions.map(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      if (cId === clothesId && j['wash stated'] === 'need wash') {
        changed = true;
        return {
          ...j,
          'wash stated': 'wash' as const,
          washStated: 'wash'
        };
      }
      return j;
    });

    let updatedGarment: ClothesItem | null = null;
    this.clothes = this.clothes.map(c => {
      if (c.id === clothesId) {
        changed = true;
        const newTracked = (c['Tracked App Washes'] || 0) + 1;
        const newTotal = (c['Total Washed Count'] || 0) + 1;
        const now = new Date().toISOString();
        updatedGarment = {
          ...c,
          'Tracked App Washes': newTracked,
          'Total Washed Count': newTotal,
          'Last Washed': now,
          'wet': 'dry' as const
        };
        return updatedGarment;
      }
      return c;
    });

    if (changed) {
      this.persistLocal();
      this.notify();

      // Background sync to Firestore
      try {
        if (updatedGarment) {
          const docRef = doc(db, 'clothes', clothesId);
          await updateDoc(docRef, {
            'Tracked App Washes': updatedGarment['Tracked App Washes'],
            'Total Washed Count': updatedGarment['Total Washed Count'],
            'Last Washed': updatedGarment['Last Washed'],
            'wet': 'dry'
          });
        }
      } catch (err) {
        handleFirestoreError(err, OperationType.UPDATE, `clothes/${clothesId}`);
      }
    }
  }

  public async markAllLaundryWashed() {
    const needWashIds = new Set<string>();
    this.junctions.forEach(j => {
      if (j['wash stated'] === 'need wash') {
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
        needWashIds.add(cId);
      }
    });

    this.junctions = this.junctions.map(j => {
      if (j['wash stated'] === 'need wash') {
        return {
          ...j,
          'wash stated': 'wash' as const,
          washStated: 'wash'
        };
      }
      return j;
    });

    const now = new Date().toISOString();
    this.clothes = this.clothes.map(c => {
      if (needWashIds.has(c.id)) {
        return {
          ...c,
          'Tracked App Washes': (c['Tracked App Washes'] || 0) + 1,
          'Total Washed Count': (c['Total Washed Count'] || 0) + 1,
          'Last Washed': now,
          'wet': 'dry' as const
        };
      }
      return c;
    });

    this.persistLocal();
    this.notify();

    // Firestore batch update
    try {
      const batch = writeBatch(db);
      needWashIds.forEach(id => {
        const docRef = doc(db, 'clothes', id);
        const item = this.clothes.find(c => c.id === id);
        if (item) {
          batch.update(docRef, {
            'Tracked App Washes': item['Tracked App Washes'],
            'Total Washed Count': item['Total Washed Count'],
            'Last Washed': item['Last Washed'],
            'wet': 'dry'
          });
        }
      });
      await batch.commit();
    } catch (err) {
      console.warn('Background wash sync warning:', err);
    }
  }

  public async addGarment(newGarment: Omit<ClothesItem, 'id'>): Promise<ClothesItem> {
    const id = 'rec_garment_' + Date.now();
    const item: ClothesItem = normalizeGarment({
      ...newGarment,
      id
    });

    this.clothes.unshift(item);
    this.persistLocal();
    this.notify();

    // Persist to Firestore
    try {
      const docRef = doc(db, 'clothes', id);
      await setDoc(docRef, sanitizeForFirestore(item));
    } catch (err) {
      handleFirestoreError(err, OperationType.CREATE, `clothes/${id}`);
    }

    return item;
  }

  public async updateGarment(id: string, partial: Partial<ClothesItem>): Promise<void> {
    this.clothes = this.clothes.map(c => {
      if (c.id === id) {
        return normalizeGarment({ ...c, ...partial });
      }
      return c;
    });
    this.persistLocal();
    this.notify();

    try {
      const docRef = doc(db, 'clothes', id);
      await setDoc(docRef, sanitizeForFirestore(partial as any), { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.UPDATE, `clothes/${id}`);
    }
  }

  public async addWearingReport(
    reportData: Omit<WearingReport, 'id'>,
    selectedGarmentIds: { clothesId: string; durationHours: number; washStated: 'wash' | 'need wash' }[]
  ): Promise<WearingReport> {
    const reportId = 'rec_report_' + Date.now();
    const newReport: WearingReport = normalizeReport({
      ...reportData,
      id: reportId
    });

    const newJunctions: ReportClothesJunction[] = selectedGarmentIds.map((item, idx) => {
      const garment = this.clothes.find(c => c.id === item.clothesId);
      const junctionId = `rec_junction_${Date.now()}_${idx}`;
      return normalizeJunction({
        id: junctionId,
        name: this.junctions.length + idx + 1,
        'wearing report id': { id: reportId, title: newReport['Name'] },
        'clothes(new)': { id: item.clothesId, title: garment?.['Name'] || 'Garment' },
        'duration': Math.round(item.durationHours * 3600),
        'wash stated': item.washStated,
        'Clothes Type (from clothes(new))': garment?.['Clothes Type'] || 'Dress'
      });
    });

    this.reports.unshift(newReport);
    this.junctions.unshift(...newJunctions);
    this.persistLocal();
    this.notify();

    // Persist to Firestore
    try {
      const reportDocRef = doc(db, 'wearing_reports', reportId);
      await setDoc(reportDocRef, sanitizeForFirestore(newReport));

      const batch = writeBatch(db);
      newJunctions.forEach(j => {
        const jDocRef = doc(db, 'report_clothes_junctions', j.id);
        batch.set(jDocRef, sanitizeForFirestore(j));
      });
      await batch.commit();
    } catch (err) {
      handleFirestoreError(err, OperationType.CREATE, `wearing_reports/${reportId}`);
    }

    return newReport;
  }

  public async deleteGarment(clothesId: string): Promise<void> {
    // 1. Remove from local clothes
    this.clothes = this.clothes.filter(c => c.id !== clothesId);

    // 2. Remove orphaned junctions
    const removedJunctions = this.junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId === clothesId;
    });
    this.junctions = this.junctions.filter(j => {
      const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)'].id : String(j['clothes(new)']);
      return cId !== clothesId;
    });

    this.persistLocal();
    this.notify();

    // 3. Remove from Firestore
    try {
      await deleteDoc(doc(db, 'clothes', clothesId));
      if (removedJunctions.length > 0) {
        const batch = writeBatch(db);
        removedJunctions.forEach(j => {
          batch.delete(doc(db, 'report_clothes_junctions', j.id));
        });
        await batch.commit();
      }
    } catch (err) {
      console.warn('Firestore deleteGarment error:', err);
    }
  }

  public async deleteWearingReport(reportId: string): Promise<void> {
    // 1. Remove from local reports
    this.reports = this.reports.filter(r => r.id !== reportId);

    // 2. Remove associated junctions
    const removedJunctions = this.junctions.filter(j => {
      const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
      return rId === reportId;
    });
    this.junctions = this.junctions.filter(j => {
      const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
      return rId !== reportId;
    });

    this.persistLocal();
    this.notify();

    // 3. Remove from Firestore
    try {
      await deleteDoc(doc(db, 'wearing_reports', reportId));
      if (removedJunctions.length > 0) {
        const batch = writeBatch(db);
        removedJunctions.forEach(j => {
          batch.delete(doc(db, 'report_clothes_junctions', j.id));
        });
        await batch.commit();
      }
    } catch (err) {
      console.warn('Firestore deleteWearingReport error:', err);
    }
  }

  public async updateWearingReport(
    reportId: string,
    partialReport: Partial<WearingReport>,
    updatedClothesItems?: Array<{ clothesId: string; durationHours: number; washStated: 'wash' | 'need wash' }>
  ): Promise<WearingReport> {
    let updated: WearingReport | null = null;
    this.reports = this.reports.map(r => {
      if (r.id === reportId) {
        updated = normalizeReport({ ...r, ...partialReport });
        return updated;
      }
      return r;
    });

    // If updatedClothesItems was provided, rebuild junctions for this report
    let newJunctions: ReportClothesJunction[] = [];
    if (updatedClothesItems && updatedClothesItems.length > 0) {
      // Find old junctions to delete from Firestore
      const oldJunctions = this.junctions.filter(j => {
        const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
        return rId === reportId;
      });

      // Filter out old from local
      this.junctions = this.junctions.filter(j => {
        const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id'].id : String(j['wearing report id']);
        return rId !== reportId;
      });

      newJunctions = updatedClothesItems.map((item, idx) => {
        const garment = this.clothes.find(c => c.id === item.clothesId);
        const junctionId = `rec_junction_${Date.now()}_${idx}`;
        return normalizeJunction({
          id: junctionId,
          name: this.junctions.length + idx + 1,
          'wearing report id': { id: reportId, title: (updated as any)?.['Name'] || 'Wearing Report' },
          'clothes(new)': { id: item.clothesId, title: garment?.['Name'] || 'Garment' },
          'duration': Math.round(item.durationHours * 3600),
          'wash stated': item.washStated,
          'Clothes Type (from clothes(new))': garment?.['Clothes Type'] || 'Dress'
        });
      });

      this.junctions.unshift(...newJunctions);

      // Batch update Firestore for junctions
      try {
        const batch = writeBatch(db);
        oldJunctions.forEach(oj => {
          batch.delete(doc(db, 'report_clothes_junctions', oj.id));
        });
        newJunctions.forEach(nj => {
          batch.set(doc(db, 'report_clothes_junctions', nj.id), sanitizeForFirestore(nj));
        });
        await batch.commit();
      } catch (err) {
        console.warn('Junction sync warning during updateWearingReport:', err);
      }
    }

    this.persistLocal();
    this.notify();

    // Persist wearing report update to Firestore
    try {
      const reportDocRef = doc(db, 'wearing_reports', reportId);
      await updateDoc(reportDocRef, sanitizeForFirestore(partialReport as any));
    } catch (err) {
      console.warn('Firestore updateWearingReport error:', err);
    }

    return updated || this.reports.find(r => r.id === reportId)!;
  }
}

export const dataService = new DataService();
