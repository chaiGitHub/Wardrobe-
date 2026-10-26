import express from "express";
import path from "path";
import fs from "fs";
import { GoogleGenAI } from "@google/genai";
import { createServer as createViteServer } from "vite";
import dotenv from "dotenv";
import JSZip from "jszip";

dotenv.config();

function handleGeminiError(error: any, res: express.Response, fallbackMessage: string) {
  let errorMsg = error?.message || fallbackMessage;
  let isPrepaymentDepleted = false;

  try {
    const rawText = typeof errorMsg === "string" ? errorMsg : JSON.stringify(error || {});
    if (
      error?.status === 402 ||
      error?.code === 402 ||
      rawText.includes("402") ||
      rawText.includes("prepayment credits are depleted") ||
      rawText.includes("RESOURCE_EXHAUSTED")
    ) {
      isPrepaymentDepleted = true;
      errorMsg = "Google AI Studio prepayment credits are depleted on this project (Error 402). You can manage project credits at https://ai.studio/projects or provide a custom API key with active quota.";
    } else {
      const parsed = rawText.trim().startsWith("{") ? JSON.parse(rawText) : null;
      if (parsed?.error?.message) {
        errorMsg = parsed.error.message;
      }
    }
  } catch {}

  if (isPrepaymentDepleted) {
    console.warn("[Gemini API Warning] Prepayment credits depleted on current project (HTTP 402). Returned structured warning to client.");
  } else {
    console.warn("[Gemini API Warning]:", errorMsg);
  }

  res.status(isPrepaymentDepleted ? 402 : 500).json({
    error: errorMsg,
    isPrepaymentDepleted,
    status: isPrepaymentDepleted ? "PREPAYMENT_DEPLETED" : "ERROR",
    billingUrl: "https://ai.studio/projects",
    apiKeyUrl: "https://aistudio.google.com/apikey"
  });
}

const dataDir = path.join(process.cwd(), "data");
const secretConfigPath = path.join(dataDir, "secret_config.json");

function getSavedSecretConfig(): { geminiApiKey?: string; model?: string; isAiPaused?: boolean } {
  try {
    if (fs.existsSync(secretConfigPath)) {
      const raw = fs.readFileSync(secretConfigPath, "utf-8");
      return JSON.parse(raw);
    }
  } catch {}
  return { isAiPaused: true };
}

function getGeminiClient(customKey?: string, requestedModel?: string): { ai: GoogleGenAI; model: string; keySource: 'request' | 'saved_secret' | 'env' } {
  const secretConfig = getSavedSecretConfig();
  let apiKey = customKey?.trim();
  let keySource: 'request' | 'saved_secret' | 'env' = 'request';

  if (!apiKey) {
    if (secretConfig.geminiApiKey?.trim()) {
      apiKey = secretConfig.geminiApiKey.trim();
      keySource = 'saved_secret';
    } else if (process.env.GEMINI_API_KEY?.trim()) {
      apiKey = process.env.GEMINI_API_KEY.trim();
      keySource = 'env';
    }
  }

  if (!apiKey) {
    throw new Error("GEMINI_API_KEY is not configured in server environment, persistent secrets, or request.");
  }

  const ai = new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        "User-Agent": "aistudio-build",
      },
    },
  });

  const chosenModel = requestedModel || secretConfig.model || "gemini-3.8-flash";
  let model = "gemini-3.8-flash";
  if (chosenModel === "gemini-3.1-flash-lite") {
    model = "gemini-3.1-flash-lite";
  } else {
    model = "gemini-3.8-flash";
  }

  return { ai, model, keySource };
}

function findFileByName(dir: string, targetName: string): string | null {
  if (!fs.existsSync(dir)) return null;
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        const found = findFileByName(full, targetName);
        if (found) return found;
      } else if (entry.isFile()) {
        if (entry.name.toLowerCase() === targetName.toLowerCase()) {
          return full;
        }
      }
    }
  } catch {}
  return null;
}

async function startServer() {
  const app = express();
  const PORT = Number(process.env.PORT) || 3000;

  app.use("/api/backup/restore-zip", express.raw({ type: () => true, limit: "500mb" }));
  app.use(express.json({ limit: "150mb" }));
  app.use(express.raw({ type: ["application/zip", "application/octet-stream", "application/x-zip-compressed"], limit: "300mb" }));

  // Ensure uploads directory exists and mount static serving
  const uploadsDir = path.join(process.cwd(), "uploads");
  if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
  }
  app.use("/uploads", express.static(uploadsDir));

  // Smart fallback middleware for /uploads/* (handles path variations, missing/extra photos/ prefixes, and filename lookups)
  app.use("/uploads", (req, res, next) => {
    try {
      const reqPath = decodeURIComponent(req.path || "").replace(/^\/+/, "");
      if (!reqPath) return next();

      // 1. Try stripping leading "photos/": e.g. /uploads/photos/garments/... -> uploads/garments/...
      if (reqPath.startsWith("photos/")) {
        const alt1 = path.join(uploadsDir, reqPath.slice("photos/".length));
        if (fs.existsSync(alt1) && fs.statSync(alt1).isFile()) {
          return res.sendFile(alt1);
        }
      }

      // 2. Try adding leading "photos/": e.g. /uploads/garments/... -> uploads/photos/garments/...
      const alt2 = path.join(uploadsDir, "photos", reqPath);
      if (fs.existsSync(alt2) && fs.statSync(alt2).isFile()) {
        return res.sendFile(alt2);
      }

      // 3. Search uploadsDir by exact filename
      const baseName = path.basename(reqPath);
      const found = findFileByName(uploadsDir, baseName);
      if (found) {
        return res.sendFile(found);
      }

      // 4. Try stripping timestamp prefix (e.g. 1789983386243_abc.jpg -> abc.jpg)
      const withoutTs = baseName.replace(/^\d+_/, "");
      if (withoutTs !== baseName) {
        const foundWithoutTs = findFileByName(uploadsDir, withoutTs);
        if (foundWithoutTs) {
          return res.sendFile(foundWithoutTs);
        }
      }
    } catch (e) {
      console.warn("Uploads fallback warning:", e);
    }
    next();
  });

  // Health check
  app.get("/api/health", (req, res) => {
    res.json({ status: "ok", timestamp: new Date().toISOString() });
  });

  // Storage Upload Endpoint (Instant, reliable file storage)
  app.post("/api/storage/upload", async (req, res) => {
    try {
      const { storagePath, fileData, filename, contentType } = req.body;
      if (!storagePath || !fileData) {
        return res.status(400).json({ error: "storagePath and fileData are required." });
      }

      const buffer = Buffer.from(fileData, "base64");
      const targetDir = path.join(uploadsDir, path.dirname(storagePath));
      await fs.promises.mkdir(targetDir, { recursive: true });
      const fullFilePath = path.join(uploadsDir, storagePath);
      await fs.promises.writeFile(fullFilePath, buffer);

      const downloadUrl = `/uploads/${storagePath}`;
      res.json({
        downloadUrl,
        storagePath,
        filename: filename || path.basename(storagePath),
        size: buffer.length,
        contentType: contentType || "image/jpeg",
        uploadedAt: new Date().toISOString(),
      });
    } catch (err: any) {
      console.error("Storage upload error:", err);
      res.status(500).json({ error: err.message || "Failed to save file." });
    }
  });

  // Storage Delete Endpoint
  app.post("/api/storage/delete", async (req, res) => {
    try {
      const { storagePath } = req.body;
      if (storagePath) {
        const fullFilePath = path.join(uploadsDir, storagePath);
        if (fs.existsSync(fullFilePath)) {
          await fs.promises.unlink(fullFilePath);
        }
      }
      res.json({ status: "ok" });
    } catch (err: any) {
      console.error("Storage delete error:", err);
      res.status(500).json({ error: err.message });
    }
  });

  // Local-Only Database file path
  const localDbPath = path.join(dataDir, "local_db.json");
  const initialDataPath = path.join(process.cwd(), "src", "initialData.json");
  const fieldDefsPath = path.join(dataDir, "field_definitions.json");
  const srcFieldDefsPath = path.join(process.cwd(), "src", "fieldDefinitions.json");
  const aiReportsPath = path.join(dataDir, "ai_advisor_reports.json");
  const drilldownPresetsPath = path.join(dataDir, "drilldown_presets.json");

  // Get AI key status & configuration
  app.get("/api/ai/key-status", (req, res) => {
    try {
      const config = getSavedSecretConfig();
      const envKey = process.env.GEMINI_API_KEY?.trim();
      let hasKey = false;
      let source: 'saved_secret' | 'env' | 'none' = 'none';
      let maskedKey = '';

      if (config.geminiApiKey?.trim()) {
        hasKey = true;
        source = 'saved_secret';
        const k = config.geminiApiKey.trim();
        maskedKey = k.length > 8 ? `${k.slice(0, 4)}...${k.slice(-4)}` : '****';
      } else if (envKey) {
        hasKey = true;
        source = 'env';
        maskedKey = envKey.length > 8 ? `${envKey.slice(0, 4)}...${envKey.slice(-4)}` : '****';
      }

      res.json({
        hasKey,
        source,
        maskedKey,
        model: config.model || 'gemini-3.8-flash'
      });
    } catch (e: any) {
      res.status(500).json({ error: e.message || 'Failed to check key status' });
    }
  });

  // Save AI API key and preferences into persistent secret file (Never requires re-keying)
  app.post("/api/ai/save-key", async (req, res) => {
    try {
      const { apiKey, model } = req.body;
      await fs.promises.mkdir(dataDir, { recursive: true });
      const current = getSavedSecretConfig();
      const updated = {
        ...current,
        ...(apiKey !== undefined ? { geminiApiKey: String(apiKey).trim() } : {}),
        ...(model !== undefined ? { model: String(model).trim() } : {})
      };
      await fs.promises.writeFile(secretConfigPath, JSON.stringify(updated, null, 2), "utf-8");
      res.json({ status: "ok", message: "API key saved to system secret permanently." });
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to save secret key." });
    }
  });

  // Delete saved secret key
  app.post("/api/ai/delete-key", async (req, res) => {
    try {
      if (fs.existsSync(secretConfigPath)) {
        const current = getSavedSecretConfig();
        delete current.geminiApiKey;
        await fs.promises.writeFile(secretConfigPath, JSON.stringify(current, null, 2), "utf-8");
      }
      res.json({ status: "ok" });
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to remove secret key." });
    }
  });

  // AI Pause Status & Control (Master Switch)
  app.get("/api/ai/pause-status", (req, res) => {
    const config = getSavedSecretConfig();
    res.json({ paused: config.isAiPaused !== false });
  });

  app.post("/api/ai/set-paused", async (req, res) => {
    try {
      const { paused } = req.body;
      await fs.promises.mkdir(dataDir, { recursive: true });
      const current = getSavedSecretConfig();
      const updated = {
        ...current,
        isAiPaused: Boolean(paused)
      };
      await fs.promises.writeFile(secretConfigPath, JSON.stringify(updated, null, 2), "utf-8");
      res.json({ status: "ok", paused: updated.isAiPaused });
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to update pause state." });
    }
  });

  // Get field definitions
  app.get("/api/field-definitions", async (req, res) => {
    try {
      if (fs.existsSync(fieldDefsPath)) {
        const content = await fs.promises.readFile(fieldDefsPath, "utf-8");
        return res.json(JSON.parse(content));
      }
      if (fs.existsSync(srcFieldDefsPath)) {
        const content = await fs.promises.readFile(srcFieldDefsPath, "utf-8");
        return res.json(JSON.parse(content));
      }
      res.json([]);
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to read field definitions" });
    }
  });

  // Save field definitions
  app.post("/api/field-definitions", async (req, res) => {
    try {
      await fs.promises.mkdir(dataDir, { recursive: true });
      const jsonString = JSON.stringify(req.body || [], null, 2);
      await fs.promises.writeFile(fieldDefsPath, jsonString, "utf-8");
      try {
        await fs.promises.writeFile(srcFieldDefsPath, jsonString, "utf-8");
      } catch (srcErr) {
        console.warn("src/fieldDefinitions.json backup warning:", srcErr);
      }
      res.json({ status: "ok" });
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to save field definitions" });
    }
  });

  // Get AI advisor independent reports
  app.get("/api/ai-advisor-reports", async (req, res) => {
    try {
      if (fs.existsSync(aiReportsPath)) {
        const content = await fs.promises.readFile(aiReportsPath, "utf-8");
        return res.json(JSON.parse(content));
      }
      res.json([]);
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to read AI advisor reports" });
    }
  });

  // Save AI advisor independent reports
  app.post("/api/ai-advisor-reports", async (req, res) => {
    try {
      await fs.promises.mkdir(dataDir, { recursive: true });
      await fs.promises.writeFile(aiReportsPath, JSON.stringify(req.body || [], null, 2), "utf-8");
      res.json({ status: "ok" });
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to save AI advisor reports" });
    }
  });

  // Get drill-down report presets from server disk
  app.get("/api/drilldown-presets", async (req, res) => {
    try {
      if (fs.existsSync(drilldownPresetsPath)) {
        const content = await fs.promises.readFile(drilldownPresetsPath, "utf-8");
        return res.json(JSON.parse(content));
      }
      res.json([]);
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to read drilldown presets" });
    }
  });

  // Save drill-down report presets to server disk
  app.post("/api/drilldown-presets", async (req, res) => {
    try {
      await fs.promises.mkdir(dataDir, { recursive: true });
      const presets = Array.isArray(req.body) ? req.body : [];
      await fs.promises.writeFile(drilldownPresetsPath, JSON.stringify(presets, null, 2), "utf-8");
      res.json({ status: "ok", count: presets.length });
    } catch (e: any) {
      res.status(500).json({ error: e.message || "Failed to save drilldown presets" });
    }
  });

  // Get local database from disk
  app.get("/api/local-db", async (req, res) => {
    try {
      if (fs.existsSync(localDbPath)) {
        const content = await fs.promises.readFile(localDbPath, "utf-8");
        const parsed = JSON.parse(content);
        if ((!parsed.drilldownPresets || parsed.drilldownPresets.length === 0) && fs.existsSync(drilldownPresetsPath)) {
          try {
            parsed.drilldownPresets = JSON.parse(await fs.promises.readFile(drilldownPresetsPath, "utf-8"));
          } catch {}
        }
        return res.json(parsed);
      }
      if (fs.existsSync(initialDataPath)) {
        const content = await fs.promises.readFile(initialDataPath, "utf-8");
        const parsed = JSON.parse(content);
        if ((!parsed.drilldownPresets || parsed.drilldownPresets.length === 0) && fs.existsSync(drilldownPresetsPath)) {
          try {
            parsed.drilldownPresets = JSON.parse(await fs.promises.readFile(drilldownPresetsPath, "utf-8"));
          } catch {}
        }
        return res.json(parsed);
      }
      res.json({ clothes: [], reports: [], junctions: [] });
    } catch (err: any) {
      console.error("Failed to read local database:", err);
      res.status(500).json({ error: "Failed to read local database" });
    }
  });

  // Save local database to disk (guaranteed persistent local storage with safety guard)
  app.post("/api/local-db", async (req, res) => {
    try {
      const payload = req.body;
      if (!payload || !Array.isArray(payload.clothes) || !Array.isArray(payload.reports)) {
        return res.status(400).json({ error: "Invalid database structure." });
      }

      // Safety Guard: prevent accidentally overwriting populated database with empty array
      if (fs.existsSync(localDbPath)) {
        try {
          const currentContent = await fs.promises.readFile(localDbPath, "utf-8");
          const currentDb = JSON.parse(currentContent);
          const currClothes = Array.isArray(currentDb.clothes) ? currentDb.clothes.length : 0;
          const currReports = Array.isArray(currentDb.reports) ? currentDb.reports.length : 0;

          if (
            (currClothes > 5 && payload.clothes.length === 0) ||
            (currReports > 5 && payload.reports.length === 0)
          ) {
            if (!payload.forceEmpty) {
              console.warn(`[LocalDB Guard] Blocked empty overwrite attempt! Current has ${currClothes} clothes, ${currReports} reports.`);
              return res.status(400).json({
                error: `Safety Guard: Refused to overwrite populated database (${currClothes} garments, ${currReports} reports) with an empty dataset.`
              });
            }
          }
        } catch (guardErr) {
          console.warn("[LocalDB Guard] Notice while verifying existing DB:", guardErr);
        }
      }

      await fs.promises.mkdir(dataDir, { recursive: true });
      const jsonString = JSON.stringify(payload, null, 2);
      await fs.promises.writeFile(localDbPath, jsonString, "utf-8");

      // Also persist drilldownPresets if included in database payload
      if (Array.isArray(payload.drilldownPresets) && payload.drilldownPresets.length > 0) {
        try {
          await fs.promises.writeFile(drilldownPresetsPath, JSON.stringify(payload.drilldownPresets, null, 2), "utf-8");
        } catch (presetErr) {
          console.warn("[LocalDB] Presets write warning:", presetErr);
        }
      }

      // Also keep initialData.json up to date as backup
      try {
        await fs.promises.writeFile(initialDataPath, jsonString, "utf-8");
      } catch (backupErr) {
        console.warn("initialData.json sync warning:", backupErr);
      }

      res.json({
        status: "ok",
        savedAt: new Date().toISOString(),
        clothesCount: payload.clothes.length,
        reportsCount: payload.reports.length,
        junctionsCount: (payload.junctions || []).length
      });
    } catch (err: any) {
      console.error("Failed to save local database:", err);
      res.status(500).json({ error: err.message || "Failed to save local database" });
    }
  });

  // Empty Database endpoint: Wipes all garments, reports, and junctions
  app.post("/api/database/empty", async (req, res) => {
    try {
      const nowIso = new Date().toISOString();
      const emptyPayload = {
        updatedAt: nowIso,
        clothes: [],
        reports: [],
        junctions: []
      };

      await fs.promises.mkdir(dataDir, { recursive: true });
      const jsonString = JSON.stringify(emptyPayload, null, 2);
      await fs.promises.writeFile(localDbPath, jsonString, "utf-8");

      // Optional: purge uploads if requested
      if (req.body?.deletePhotos && fs.existsSync(uploadsDir)) {
        try {
          const files = await fs.promises.readdir(uploadsDir);
          for (const f of files) {
            const p = path.join(uploadsDir, f);
            await fs.promises.rm(p, { recursive: true, force: true });
          }
        } catch (photoErr) {
          console.warn("Failed to clear uploads during database empty:", photoErr);
        }
      }

      console.log("[Database API] Database successfully emptied.");
      res.json({
        status: "ok",
        message: "Database emptied successfully.",
        timestamp: nowIso,
        clothesCount: 0,
        reportsCount: 0,
        junctionsCount: 0
      });
    } catch (err: any) {
      console.error("Failed to empty database:", err);
      res.status(500).json({ error: err.message || "Failed to empty database." });
    }
  });

  // Reset to Seed Data endpoint: Restores initial seed dataset
  app.post("/api/database/reset-seed", async (req, res) => {
    try {
      const backupPath = path.join(dataDir, "seed_data_backup.json");
      let seedSource = "";

      if (fs.existsSync(backupPath)) {
        seedSource = await fs.promises.readFile(backupPath, "utf-8");
      } else if (fs.existsSync(initialDataPath)) {
        seedSource = await fs.promises.readFile(initialDataPath, "utf-8");
      } else {
        return res.status(404).json({ error: "Seed data source not found." });
      }

      const parsed = JSON.parse(seedSource);
      const clothes = Array.isArray(parsed.clothes) ? parsed.clothes : [];
      const reports = Array.isArray(parsed.reports) ? parsed.reports : [];
      const junctions = Array.isArray(parsed.junctions)
        ? parsed.junctions
        : (Array.isArray(parsed.reportClothes) ? parsed.reportClothes : []);

      const nowIso = new Date().toISOString();
      const payload = {
        updatedAt: nowIso,
        clothes,
        reports,
        junctions
      };

      await fs.promises.mkdir(dataDir, { recursive: true });
      const jsonString = JSON.stringify(payload, null, 2);
      await fs.promises.writeFile(localDbPath, jsonString, "utf-8");
      try {
        await fs.promises.writeFile(initialDataPath, jsonString, "utf-8");
      } catch (err) {
        console.warn("initialData.json sync warning:", err);
      }

      console.log(`[Database API] Database reset to seed: ${clothes.length} clothes, ${reports.length} reports, ${junctions.length} junctions.`);
      res.json({
        status: "ok",
        message: "Database reset to initial seed data successfully.",
        timestamp: nowIso,
        clothesCount: clothes.length,
        reportsCount: reports.length,
        junctionsCount: junctions.length,
        data: payload
      });
    } catch (err: any) {
      console.error("Failed to reset database to seed:", err);
      res.status(500).json({ error: err.message || "Failed to reset database to seed data." });
    }
  });

  // ==========================================
  // FULL BACKUP ARCHIVE ENDPOINTS (ZIP)
  // ==========================================

  // Export Full Archive: Database + High-Resolution Photos + Markdown AI Reports
  app.get("/api/backup/download-zip", async (req, res) => {
    try {
      let dbData = { clothes: [], reports: [], junctions: [] };
      if (fs.existsSync(localDbPath)) {
        dbData = JSON.parse(await fs.promises.readFile(localDbPath, "utf-8"));
      } else if (fs.existsSync(initialDataPath)) {
        dbData = JSON.parse(await fs.promises.readFile(initialDataPath, "utf-8"));
      }

      let presetsList: any[] = [];
      if (fs.existsSync(drilldownPresetsPath)) {
        try {
          presetsList = JSON.parse(await fs.promises.readFile(drilldownPresetsPath, "utf-8"));
        } catch {}
      }

      const zip = new JSZip();

      // 1. Core Database (Full JSON fidelity)
      const dbPayload = {
        exportedAt: new Date().toISOString(),
        version: "2.0",
        app: "Wardrobe & Wear Journal",
        totalClothes: dbData.clothes.length,
        totalReports: dbData.reports.length,
        totalJunctions: (dbData.junctions || []).length,
        totalDrilldownPresets: presetsList.length,
        clothes: dbData.clothes,
        reports: dbData.reports,
        junctions: dbData.junctions || [],
        drilldownPresets: presetsList
      };
      zip.file("database.json", JSON.stringify(dbPayload, null, 2));

      // 2. High-Resolution Photos
      const photosFolder = zip.folder("photos");
      let photoCount = 0;

      async function addDirFilesToZip(currentDir: string, zipFolder: JSZip) {
        if (!fs.existsSync(currentDir)) return;
        const entries = await fs.promises.readdir(currentDir, { withFileTypes: true });
        for (const entry of entries) {
          const fullPath = path.join(currentDir, entry.name);
          if (entry.isDirectory()) {
            // Avoid adding nested "photos" directory inside uploadsDir to prevent "photos/photos/..."
            if (entry.name === "photos" && currentDir === uploadsDir) {
              continue;
            }
            const subFolder = zipFolder.folder(entry.name);
            if (subFolder) {
              await addDirFilesToZip(fullPath, subFolder);
            }
          } else if (entry.isFile()) {
            const fileData = await fs.promises.readFile(fullPath);
            zipFolder.file(entry.name, fileData);
            photoCount++;
          }
        }
      }

      if (photosFolder && fs.existsSync(uploadsDir)) {
        await addDirFilesToZip(uploadsDir, photosFolder);
      }

      // 3. Formatted Markdown AI Reports
      const aiReportsFolder = zip.folder("ai_reports");
      if (aiReportsFolder) {
        let indexMd = `# Wardrobe Wearing Reports & AI Diagnostics Archive\nExported: ${new Date().toLocaleString()}\nTotal Reports: ${dbData.reports.length}\n\n`;

        for (const r of (dbData.reports as any[])) {
          const repName = r["Name"] || r.name || "Wearing Report";
          const repDate = r["wear date"] || r.wearDate || "Unknown Date";
          const safeDate = String(repDate).slice(0, 10);
          const safeFileName = `${safeDate}_${repName.replace(/[^a-zA-Z0-9_-]/g, "_")}.md`;

          const linkedJunctions = (dbData.junctions as any[] || []).filter((j: any) => {
            const rId = typeof j["wearing report id"] === "object" ? j["wearing report id"]?.id : String(j["wearing report id"]);
            return rId === r.id;
          });

          const linkedClothesDetails = linkedJunctions.map((j: any) => {
            const cId = typeof j["clothes(new)"] === "object" ? j["clothes(new)"]?.id : String(j["clothes(new)"]);
            const garment = (dbData.clothes as any[] || []).find((c: any) => c.id === cId);
            const durationHrs = j["duration"] ? (j["duration"] / 3600).toFixed(1) : "Unknown";
            const washStated = j["wash stated"] || "need wash";
            const washDate = j["wash date"] || j["washed date"] || "(pending)";
            return `- **${garment ? (garment["Name"] || garment.name) : (j.clothesTitle || "Garment")}**: ${durationHrs}h worn | Status: **${washStated}** (${washDate})`;
          }).join("\n");

          let aiDiagnosticsSection = "";
          if (Array.isArray(r.aiReports) && r.aiReports.length > 0) {
            aiDiagnosticsSection = r.aiReports.map((v: any, idx: number) => {
              const vTitle = v.title || `Report Version ${idx + 1}`;
              const vModel = v.model ? ` [Model: ${v.model}]` : "";
              const vTime = v.timestamp ? ` — ${new Date(v.timestamp).toLocaleString()}` : "";
              const star = v.isPrimary ? " ⭐ [PRIMARY]" : "";
              return `### ${vTitle}${vModel}${vTime}${star}\n\n${v.content || "(empty)"}\n`;
            }).join("\n---\n\n");
          } else {
            aiDiagnosticsSection = r["AI Wearing Analysis"] || r.aiWearingAnalysis || "*(No AI analysis run on this session yet)*";
          }

          const reportMd = `# ${repName}
- **Wear Date**: ${repDate}
- **Recorded**: ${r["created"] || r.created || "N/A"}
- **Categories**: ${Array.isArray(r.categories) ? r.categories.join(", ") : (r.categories || "N/A")}
- **Tags**: ${Array.isArray(r.tags) ? r.tags.join(", ") : (r.tags || "N/A")}

---

## 1. Sensory & Fit Observations
- **Shoulder Feel**: ${r["shoulder feel"] || r.shoulderFeel || "(unrecorded)"}
- **Mid Breast (Daytime)**: ${r["mid breast"] || r.midBreast || "(unrecorded)"}
- **Night Mid Breast**: ${r["night mid breast"] || r.nightMidBreast || "(unrecorded)"}
- **Sweat (Daytime)**: ${r["sweat"] || r.sweat || "(unrecorded)"}
- **Night Sweat**: ${r["night sweat"] || r.nightSweat || "(unrecorded)"}
- **Leg Feel (Daytime)**: ${r["leg feel"] || r.legFeel || "(unrecorded)"}
- **Night Leg Feel**: ${r["night leg feel"] || r.nightLegFeel || "(unrecorded)"}

## 2. Session Detail Notes
${r["report detail"] || r.reportDetail || "No freeform details recorded for this session."}

## 3. Linked Garments in this Outfit
${linkedClothesDetails || "No linked garment records for this session."}

## 4. Teable-Style Grounded AI Wearing Diagnostics (Saved Versions)
${aiDiagnosticsSection}
`;
          aiReportsFolder.file(safeFileName, reportMd);
          indexMd += `- [${repName} (${safeDate})](./${safeFileName})\n`;
        }

        aiReportsFolder.file("README_INDEX.md", indexMd);
      }

      // 4. Metadata and Readme
      let aiReportsList = [];
      let fieldDefsList = [];
      if (fs.existsSync(aiReportsPath)) {
        try {
          aiReportsList = JSON.parse(await fs.promises.readFile(aiReportsPath, "utf-8"));
        } catch {}
      }
      if (fs.existsSync(fieldDefsPath)) {
        try {
          fieldDefsList = JSON.parse(await fs.promises.readFile(fieldDefsPath, "utf-8"));
        } catch {}
      }

      zip.file("ai_advisor_reports.json", JSON.stringify(aiReportsList, null, 2));
      zip.file("field_definitions.json", JSON.stringify(fieldDefsList, null, 2));
      zip.file("drilldown_presets.json", JSON.stringify(presetsList, null, 2));

      const metadata = {
        app: "Wardrobe & Wear Journal",
        backupGeneratedAt: new Date().toISOString(),
        version: "2.0",
        stats: {
          totalGarments: dbData.clothes.length,
          totalWearingReports: dbData.reports.length,
          totalJunctions: (dbData.junctions || []).length,
          totalPhotosInArchive: photoCount,
          totalAiReportsInArchive: dbData.reports.length,
          totalAdvisorReports: Array.isArray(aiReportsList) ? aiReportsList.length : 0,
          totalFieldDefinitions: Array.isArray(fieldDefsList) ? fieldDefsList.length : 0,
          totalDrilldownPresets: Array.isArray(presetsList) ? presetsList.length : 0
        },
        aiEngine: "Google AI Studio Gemini API (gemini-3.8-flash)",
      };
      zip.file("metadata.json", JSON.stringify(metadata, null, 2));

      const readmeTxt = `=====================================================
WARDROBE & WEAR JOURNAL - FULL ARCHIVE BACKUP
=====================================================
Exported: ${new Date().toLocaleString()}
Version: 2.0

ARCHIVE CONTENTS:
-----------------
1. database.json
   - Contains your full database of garments, wearing reports,
     wear duration records, and wash dates.

2. ai_advisor_reports.json & field_definitions.json
   - Contains all independent AI Advisor reports, custom definitions,
     and bilingual field-value dictionaries.

3. photos/
   - High-resolution, uncompressed original images for your clothes
     and wearing reports.

4. ai_reports/
   - Formatted Markdown (.md) documents for EVERY wearing report,
     including all sensory notes (shoulder feel, chest, sweat, leg feel)
     and Teable-style grounded AI Diagnostics.
   - Usable in Obsidian, Notion, Logseq, or any markdown reader!

5. metadata.json
   - Summary statistics of this archive.

HOW TO RESTORE:
---------------
Open the app -> Go to "Sync & Backup" -> Click "Restore Archive (.zip)"
and select this file. All your clothes, photos, definitions, and AI reports will be restored!
=====================================================`;
      zip.file("README.txt", readmeTxt);

      const buffer = await zip.generateAsync({
        type: "nodebuffer",
        compression: "DEFLATE",
        compressionOptions: { level: 6 },
      });

      const dateStr = new Date().toISOString().slice(0, 10);
      res.setHeader("Content-Type", "application/zip");
      res.setHeader("Content-Disposition", `attachment; filename="wardrobe-full-backup-${dateStr}.zip"`);
      res.setHeader("Content-Length", buffer.length);
      res.send(buffer);
    } catch (err: any) {
      console.error("Backup export error:", err);
      res.status(500).json({ error: err.message || "Failed to generate backup archive." });
    }
  });

  // Restore Full Archive (.zip or .json)
  app.post("/api/backup/restore-zip", async (req, res) => {
    try {
      let zipBuffer: Buffer | null = null;
      if (Buffer.isBuffer(req.body)) {
        zipBuffer = req.body;
      } else if (req.body && typeof req.body.zipBase64 === "string") {
        zipBuffer = Buffer.from(req.body.zipBase64, "base64");
      }

      if (!zipBuffer || zipBuffer.length === 0) {
        return res.status(400).json({ error: "No ZIP data provided. Provide binary stream or zipBase64." });
      }

      const zip = await JSZip.loadAsync(zipBuffer);

      // Extract database.json (support root or subfolder, database.json or initialData.json or local_db.json)
      const allFiles = Object.keys(zip.files);
      const dbEntryKey = allFiles.find(
        (f) => !zip.files[f].dir && (
          f === "database.json" ||
          f.endsWith("/database.json") ||
          f === "initialData.json" ||
          f.endsWith("/initialData.json") ||
          f === "local_db.json" ||
          f.endsWith("/local_db.json")
        )
      );

      if (!dbEntryKey) {
        return res.status(400).json({ error: "Invalid backup archive: missing database.json inside archive." });
      }

      const dbFile = zip.file(dbEntryKey);
      if (!dbFile) {
        return res.status(400).json({ error: "Could not read database.json from archive." });
      }

      const dbText = await dbFile.async("string");
      const parsedDb = JSON.parse(dbText);

      const clothes = Array.isArray(parsedDb.clothes) ? parsedDb.clothes : [];
      const reports = Array.isArray(parsedDb.reports) ? parsedDb.reports : [];
      const junctions = Array.isArray(parsedDb.junctions)
        ? parsedDb.junctions
        : (Array.isArray(parsedDb.reportClothes) ? parsedDb.reportClothes : []);

      if (clothes.length === 0 && reports.length === 0) {
        return res.status(400).json({ error: "Archive contains no clothes or wearing reports." });
      }

      // Build index of picture records from database
      const pictureRecords: any[] = [];
      for (const c of clothes) {
        if (Array.isArray(c.Pictures)) {
          for (const p of c.Pictures) {
            pictureRecords.push({ parent: c, type: "garment", pic: p });
          }
        }
      }
      for (const r of reports) {
        if (Array.isArray(r.Pictures)) {
          for (const p of r.Pictures) {
            pictureRecords.push({ parent: r, type: "report", pic: p });
          }
        }
      }

      // Extract all photos into uploadsDir safely
      let restoredPhotosCount = 0;
      const photoEntries = allFiles.filter((f) => {
        if (zip.files[f].dir || f.endsWith("/")) return false;
        const lower = f.toLowerCase();
        if (
          lower.endsWith(".json") ||
          lower.endsWith(".md") ||
          lower.endsWith(".txt") ||
          lower.includes(".ds_store")
        ) {
          return false;
        }
        return (
          lower.endsWith(".jpg") ||
          lower.endsWith(".jpeg") ||
          lower.endsWith(".png") ||
          lower.endsWith(".webp") ||
          lower.endsWith(".gif") ||
          lower.endsWith(".heic") ||
          lower.endsWith(".svg") ||
          lower.endsWith(".bmp") ||
          lower.endsWith(".avif")
        );
      });

      for (const filePath of photoEntries) {
        try {
          const fileInZip = zip.file(filePath);
          if (!fileInZip) continue;

          let normPath = filePath.replace(/\\/g, "/").replace(/^\/+/, "");
          if (normPath.endsWith("/")) continue;

          let targetSubPath = normPath;
          if (targetSubPath.startsWith("uploads/")) {
            targetSubPath = targetSubPath.slice("uploads/".length);
          }
          while (targetSubPath.startsWith("photos/")) {
            targetSubPath = targetSubPath.slice("photos/".length);
          }
          targetSubPath = targetSubPath.replace(/^\/+/, "");
          if (!targetSubPath) continue;

          const baseName = path.basename(targetSubPath);
          if (!baseName || baseName === "." || baseName === "..") continue;
          const strippedBase = baseName.replace(/^\d+_/, "");

          const fileContent = await fileInZip.async("nodebuffer");

          // Find if any picture record in database corresponds to this file
          const matchedPicRec = pictureRecords.find(({ pic }) => {
            if (pic.storagePath) {
              const cleanSp = pic.storagePath.replace(/\\/g, "/").replace(/^\/+/, "");
              if (cleanSp === targetSubPath || path.basename(cleanSp) === baseName || path.basename(cleanSp) === strippedBase) return true;
            }
            if (pic.url) {
              const cleanUrl = pic.url.replace(/\\/g, "/");
              if (cleanUrl.includes(targetSubPath) || path.basename(cleanUrl) === baseName || path.basename(cleanUrl) === strippedBase) return true;
            }
            if (pic.filename === baseName || pic.filename === strippedBase) return true;
            if (pic.name === baseName || pic.name === strippedBase) return true;
            return false;
          });

          const targetPaths = new Set<string>();

          // Primary file path inside uploads
          const p1 = path.join(uploadsDir, targetSubPath);
          if (p1 !== uploadsDir && !p1.endsWith(path.sep)) targetPaths.add(p1);

          // If matched with picture record's storagePath, also write to exact storagePath
          if (matchedPicRec && matchedPicRec.pic.storagePath) {
            const matchedSp = matchedPicRec.pic.storagePath.replace(/\\/g, "/").replace(/^\/+/, "").replace(/^uploads\//, "");
            if (matchedSp) {
              const p2 = path.join(uploadsDir, matchedSp);
              if (p2 !== uploadsDir && !p2.endsWith(path.sep)) targetPaths.add(p2);
            }
          }

          // Flat basename fallback
          const p3 = path.join(uploadsDir, baseName);
          if (p3 !== uploadsDir && !p3.endsWith(path.sep)) targetPaths.add(p3);

          for (const fullP of targetPaths) {
            // Guard against writing to directory or outside uploadsDir
            if (fullP === uploadsDir || !fullP.startsWith(uploadsDir)) continue;
            if (fs.existsSync(fullP) && fs.statSync(fullP).isDirectory()) continue;

            await fs.promises.mkdir(path.dirname(fullP), { recursive: true });
            await fs.promises.writeFile(fullP, fileContent);
          }
          restoredPhotosCount++;

          // Update matched picture in database if URL was missing or dead blob
          if (matchedPicRec) {
            const effStoragePath = matchedPicRec.pic.storagePath || targetSubPath;
            const cleanEff = effStoragePath.replace(/^\/+/, "").replace(/^uploads\//, "");
            matchedPicRec.pic.storagePath = cleanEff;
            matchedPicRec.pic.url = `/uploads/${cleanEff}`;
          }
        } catch (photoErr) {
          console.warn(`[Restore] Skipped photo entry ${filePath}:`, photoErr);
        }
      }

      // Heal all picture records across clothes and reports
      for (const { pic } of pictureRecords) {
        // Fix dead blob: URL or empty URL
        if (!pic.url || (typeof pic.url === "string" && pic.url.startsWith("blob:"))) {
          if (pic.storagePath) {
            const cleanSp = pic.storagePath.replace(/^\/+/, "").replace(/^uploads\//, "");
            pic.storagePath = cleanSp;
            pic.url = `/uploads/${cleanSp}`;
          } else if (pic.filename || pic.name) {
            const foundOnDisk = findFileByName(uploadsDir, pic.filename || pic.name);
            if (foundOnDisk) {
              const rel = path.relative(uploadsDir, foundOnDisk).replace(/\\/g, "/");
              pic.storagePath = rel;
              pic.url = `/uploads/${rel}`;
            }
          }
        }
        // Normalize any /uploads/photos/garments/... to /uploads/garments/...
        if (typeof pic.url === "string" && pic.url.startsWith("/uploads/photos/")) {
          const stripped = pic.url.slice("/uploads/photos/".length);
          if (stripped.startsWith("garments/") || stripped.startsWith("reports/")) {
            pic.url = `/uploads/${stripped}`;
          }
        }
      }

      // Safe Database Merge: Read existing database on server disk if present to preserve any new fields, custom attributes, or newer records
      await fs.promises.mkdir(dataDir, { recursive: true });
      let existingDb: any = null;
      if (fs.existsSync(localDbPath)) {
        try {
          existingDb = JSON.parse(await fs.promises.readFile(localDbPath, "utf-8"));
        } catch {}
      }

      let mergedClothes = [...clothes];
      let mergedReports = [...reports];
      let mergedJunctions = [...junctions];

      if (existingDb) {
        // 1. Merge clothes: preserve customFields, extraAttributes, and any new custom properties from existing clothes
        const existingClothesMap = new Map<string, any>(
          (Array.isArray(existingDb.clothes) ? existingDb.clothes : []).map((c: any) => [c.id, c])
        );
        for (const c of mergedClothes) {
          const existing = existingClothesMap.get(c.id);
          if (existing) {
            const existingCustom = { ...(existing.customFields || {}), ...(existing.extraAttributes || {}) };
            const incomingCustom = { ...(c.customFields || {}), ...(c.extraAttributes || {}) };
            const mergedCustom = { ...existingCustom, ...incomingCustom };
            for (const [k, v] of Object.entries(existingCustom)) {
              if (v !== undefined && v !== null && String(v).trim() !== "") {
                if (mergedCustom[k] === undefined || mergedCustom[k] === null || String(mergedCustom[k]).trim() === "") {
                  mergedCustom[k] = v;
                }
              }
            }
            c.customFields = mergedCustom;
            c.extraAttributes = mergedCustom;
            for (const [mk, mv] of Object.entries(mergedCustom)) {
              if (c[mk] === undefined || c[mk] === null || String(c[mk]).trim() === "") {
                c[mk] = mv;
              }
            }

            if (existing.fieldExplanations && typeof existing.fieldExplanations === 'object') {
              c.fieldExplanations = {
                ...existing.fieldExplanations,
                ...(c.fieldExplanations || {})
              };
            }
            for (const [k, v] of Object.entries(existing)) {
              if (v !== undefined && v !== null && String(v).trim() !== "") {
                const incomingVal = c[k];
                if (incomingVal === undefined || incomingVal === null || String(incomingVal).trim() === "") {
                  c[k] = v;
                }
              }
            }
          }
        }
        // Also preserve any newer clothes created after backup
        const backupClothesIds = new Set(mergedClothes.map((c: any) => c.id));
        for (const existingC of existingDb.clothes || []) {
          if (!backupClothesIds.has(existingC.id)) {
            mergedClothes.push(existingC);
          }
        }

        // 2. Merge reports: preserve customFields, extraAttributes, and any new custom properties
        const existingReportsMap = new Map<string, any>(
          (Array.isArray(existingDb.reports) ? existingDb.reports : []).map((r: any) => [r.id, r])
        );
        for (const r of mergedReports) {
          const existing = existingReportsMap.get(r.id);
          if (existing) {
            const existingCustom = { ...(existing.customFields || {}), ...(existing.extraAttributes || {}) };
            const incomingCustom = { ...(r.customFields || {}), ...(r.extraAttributes || {}) };
            const mergedCustom = { ...existingCustom, ...incomingCustom };
            for (const [k, v] of Object.entries(existingCustom)) {
              if (v !== undefined && v !== null && String(v).trim() !== "") {
                if (mergedCustom[k] === undefined || mergedCustom[k] === null || String(mergedCustom[k]).trim() === "") {
                  mergedCustom[k] = v;
                }
              }
            }
            r.customFields = mergedCustom;
            r.extraAttributes = mergedCustom;
            for (const [mk, mv] of Object.entries(mergedCustom)) {
              if (r[mk] === undefined || r[mk] === null || String(r[mk]).trim() === "") {
                r[mk] = mv;
              }
            }

            if (existing.fieldExplanations && typeof existing.fieldExplanations === 'object') {
              r.fieldExplanations = {
                ...existing.fieldExplanations,
                ...(r.fieldExplanations || {})
              };
            }
            for (const [k, v] of Object.entries(existing)) {
              if (v !== undefined && v !== null && String(v).trim() !== "") {
                const incomingVal = r[k];
                if (incomingVal === undefined || incomingVal === null || String(incomingVal).trim() === "") {
                  r[k] = v;
                }
              }
            }
          }
        }
        // Also preserve any newer reports created after backup
        const backupReportIds = new Set(mergedReports.map((r: any) => r.id));
        for (const existingR of existingDb.reports || []) {
          if (!backupReportIds.has(existingR.id)) {
            mergedReports.push(existingR);
          }
        }

        // 3. Merge junctions
        const backupJunctionIds = new Set(mergedJunctions.map((j: any) => j.id));
        for (const existingJ of existingDb.junctions || []) {
          if (!backupJunctionIds.has(existingJ.id)) {
            mergedJunctions.push(existingJ);
          }
        }
      }

      // Write sanitized merged database to disk
      const nowIso = new Date().toISOString();
      const sanitizedPayload = {
        updatedAt: nowIso,
        clothes: mergedClothes,
        reports: mergedReports,
        junctions: mergedJunctions,
      };
      await fs.promises.writeFile(localDbPath, JSON.stringify(sanitizedPayload, null, 2), "utf-8");
      try {
        await fs.promises.writeFile(initialDataPath, JSON.stringify(sanitizedPayload, null, 2), "utf-8");
      } catch (e) {
        console.warn("initialData.json write warning:", e);
      }

      // Extract ai_advisor_reports.json if present
      const aiReportsKey = allFiles.find((f) => !zip.files[f].dir && f.endsWith("ai_advisor_reports.json"));
      if (aiReportsKey) {
        try {
          const aiReportsZipFile = zip.file(aiReportsKey);
          if (aiReportsZipFile) {
            const aiReportsText = await aiReportsZipFile.async("string");
            await fs.promises.writeFile(aiReportsPath, aiReportsText, "utf-8");
          }
        } catch {}
      }

      // Safely merge field_definitions.json so new definitions and historical typicalValues are NEVER deleted
      const fieldDefsKey = allFiles.find((f) => !zip.files[f].dir && f.endsWith("field_definitions.json"));
      if (fieldDefsKey) {
        try {
          const fieldDefsZipFile = zip.file(fieldDefsKey);
          if (fieldDefsZipFile) {
            const fieldDefsText = await fieldDefsZipFile.async("string");
            const backupDefs = JSON.parse(fieldDefsText);
            if (Array.isArray(backupDefs)) {
              // Read current definitions on disk
              let currentDefs: any[] = [];
              if (fs.existsSync(srcFieldDefsPath)) {
                try {
                  currentDefs = JSON.parse(await fs.promises.readFile(srcFieldDefsPath, "utf-8"));
                } catch {}
              }
              if (currentDefs.length === 0 && fs.existsSync(fieldDefsPath)) {
                try {
                  currentDefs = JSON.parse(await fs.promises.readFile(fieldDefsPath, "utf-8"));
                } catch {}
              }
              // Merge map keyed by lowercase fieldNameEn
              const mergedMap = new Map<string, any>();
              // Keep all current definitions (never deleted!)
              currentDefs.forEach((d: any) => {
                if (d && d.fieldNameEn) mergedMap.set(d.fieldNameEn.toLowerCase(), d);
              });
              // Overlay backup definitions
              backupDefs.forEach((d: any) => {
                if (d && d.fieldNameEn) {
                  const key = d.fieldNameEn.toLowerCase();
                  const existing = mergedMap.get(key);
                  if (existing) {
                    const tvMap = new Map<string, any>(
                      (existing.typicalValues || []).map((t: any) => [t.valueEn?.toLowerCase(), t])
                    );
                    (d.typicalValues || []).forEach((t: any) => {
                      if (t && t.valueEn) tvMap.set(t.valueEn.toLowerCase(), t);
                    });
                    mergedMap.set(key, { ...existing, ...d, typicalValues: Array.from(tvMap.values()) });
                  } else {
                    mergedMap.set(key, d);
                  }
                }
              });
              const mergedDefs = Array.from(mergedMap.values());
              const mergedDefsJson = JSON.stringify(mergedDefs, null, 2);
              await fs.promises.writeFile(fieldDefsPath, mergedDefsJson, "utf-8");
              try {
                await fs.promises.writeFile(srcFieldDefsPath, mergedDefsJson, "utf-8");
              } catch {}
            }
          }
        } catch (fieldDefErr) {
          console.warn("[Restore] Field definitions merge warning:", fieldDefErr);
        }
      }

      // Safely extract and merge drilldown_presets.json if present in backup archive
      let restoredPresetsCount = 0;
      const presetsKey = allFiles.find((f) => !zip.files[f].dir && (f.endsWith("drilldown_presets.json") || f.endsWith("report_presets.json")));
      let backupPresets: any[] = [];
      if (presetsKey) {
        try {
          const presetsZipFile = zip.file(presetsKey);
          if (presetsZipFile) {
            const presetsText = await presetsZipFile.async("string");
            const parsed = JSON.parse(presetsText);
            if (Array.isArray(parsed)) backupPresets = parsed;
          }
        } catch (e) {
          console.warn("[Restore] Presets extract warning:", e);
        }
      } else if (Array.isArray(parsedDb.drilldownPresets)) {
        backupPresets = parsedDb.drilldownPresets;
      }

      if (backupPresets.length > 0) {
        try {
          let currentPresets: any[] = [];
          if (fs.existsSync(drilldownPresetsPath)) {
            try {
              currentPresets = JSON.parse(await fs.promises.readFile(drilldownPresetsPath, "utf-8"));
            } catch {}
          }
          const presetMap = new Map<string, any>();
          // Preserve existing server presets
          currentPresets.forEach((p: any) => {
            if (p && p.id) presetMap.set(p.id, p);
          });
          // Merge backup presets (always restore incoming presets from backup archive)
          backupPresets.forEach((p: any) => {
            if (p && p.id) {
              const existing = presetMap.get(p.id);
              if (!existing) {
                presetMap.set(p.id, p);
              } else {
                presetMap.set(p.id, { ...existing, ...p });
              }
            }
          });
          const mergedPresets = Array.from(presetMap.values());
          await fs.promises.writeFile(drilldownPresetsPath, JSON.stringify(mergedPresets, null, 2), "utf-8");
          restoredPresetsCount = mergedPresets.length;
        } catch (presetMergeErr) {
          console.warn("[Restore] Drilldown presets merge warning:", presetMergeErr);
        }
      }

      res.json({
        status: "ok",
        restoredAt: nowIso,
        clothesCount: mergedClothes.length,
        reportsCount: mergedReports.length,
        junctionsCount: mergedJunctions.length,
        restoredPhotosCount,
        restoredPresetsCount,
      });
    } catch (err: any) {
      console.error("Backup restore error:", err);
      res.status(500).json({ error: err.message || "Failed to restore backup archive." });
    }
  });

  // Test Personal Google AI Studio API Key
  app.post("/api/ai/test-key", async (req, res) => {
    try {
      const { apiKey, model: reqModel } = req.body;
      const { ai, model } = getGeminiClient(apiKey, reqModel);
      const response = await ai.models.generateContent({
        model,
        contents: [{ role: "user", parts: [{ text: "Respond with: Gemini API connection verified successfully." }] }],
      });
      res.json({ status: "ok", model, reply: response.text?.trim() });
    } catch (err: any) {
      handleGeminiError(err, res, "Failed to connect with provided API key.");
    }
  });

  function isAiGenerationPausedServer(req: any): boolean {
    if (req.body?.bypassPause) return false;
    const config = getSavedSecretConfig();
    return config.isAiPaused !== false;
  }

  // AI Outfit & Sensory Advisor endpoint (Macro AI Advisor - Full 3-Layer Synthesis: Clothes + 20-Field Wearing Reports + Junction Records)
  app.post("/api/ai/outfit-advice", async (req, res) => {
    try {
      if (isAiGenerationPausedServer(req)) {
        return res.json({
          status: "paused",
          paused: true,
          error: "AI generation is paused in settings to save tokens and execution time. You can review existing records or use Google NotebookLM for analysis.",
          advice: "AI generation is currently paused in Settings.",
          analysis: "AI generation is currently paused in Settings."
        });
      }
      const {
        prompt,
        items,
        wearingReports,
        junctions,
        temperature,
        occasion,
        mood,
        apiKey,
        model: reqModel,
        dictionaryPrompt
      } = req.body;
      const { ai, model } = getGeminiClient(apiKey, reqModel);

      // Normalize candidate/all garments
      const formattedItems = (items || []).map((c: any) => ({
        id: c.id,
        name: c.name || c['Name'],
        type: c.type || c.clothesType || c['Clothes Type'],
        material: c.material || c['material'] || c.materialProp || 'Chiffon',
        thickClothes: c.thickClothes || c['thick clothes'] || '02 middle thick clothes',
        hotAtNoon: c.hotAtNoon || c['Hot at noon'] || '(未记录中午炎热度)',
        diaoDai: c.diaoDai || c['diao dai'] || '(未记录吊带款式)',
        sitBreastOk: c.sitBreastOk || c['sit breast ok'] || '(未记录)',
        sideBreastOk: c.sideBreastOk || c['side breast ok'] || '(未记录)',
        centerChest: c.centerChest || c['center chest'] || '(未记录)',
        wet: c.wet || c['wet'] || 'dry',
        wetReason: c.wetReason || c['wetReason'] || c['wet reason'] || '',
        beautiful: c.beautiful || c['beautiful'] || '(未记录)',
        beatifulReason: c.beatifulReason || c['beatifulReason'] || c['beatiful reason'] || '',
        longDress: c.longDress || c['long dress'] || '',
        descriptions: c.descriptions || c['Descriptions'] || c.description || c['Description'] || '',
        descriptions2: c.descriptions2 || c['Descriptions 2'] || c.description2 || c['Description 2'] || '',
        lastComment: c.lastComment || c['last comment or action'] || c.lastAction || c['last action'] || '',
        mayThrow: c.mayThrow || c['may throw'] || '(未记录)',
        price: c.price || c['price'] || 0,
        totalWashedCount: c.totalWashedCount ?? c['Total Washed Count'] ?? 0,
        legacyWashed: c.legacyWashed ?? c['wearing frequency(how many times legacy washed)'] ?? 0,
        unwashedDurationCount: c.unwashedDurationCount || c['unwashed duration count'] || c['unwash time rollup'] || 0,
        lastWashed: c.lastWashed || c['Last Washed'] || '(未记录)',
        customFields: c.customFields || c.extraAttributes || {},
        fieldExplanations: c.fieldExplanations || c.customFields?._fieldExplanations || {}
      }));

      // Normalize all 20 Wearing Report fields
      const formattedReports = (wearingReports || []).map((r: any) => ({
        id: r.id,
        // 1. Name
        name: r.name || r['Name'] || '(未命名穿搭)',
        // 2. wear date
        wearDate: r.wearDate || r['wear date'] || '(未记录日期)',
        // 3. shoulder feel
        shoulderFeel: r.shoulderFeel || r['shoulder feel'] || '(未记录)',
        // 4. mid breast (daytime)
        midBreast: r.midBreast || r['mid breast'] || '(未记录)',
        // 5. night mid breast
        nightMidBreast: r.nightMidBreast || r['night mid breast'] || '(未记录)',
        // 6. sweat (daytime)
        sweat: r.sweat || r.sweatProp || r['sweat'] || '(未记录)',
        // 7. night sweat
        nightSweat: r.nightSweat || r['night sweat'] || '(未记录)',
        // 8. leg feel (daytime)
        legFeel: r.legFeel || r['leg feel'] || '(未记录)',
        // 9. night leg feel
        nightLegFeel: r.nightLegFeel || r['night leg feel'] || '(未记录)',
        // 10. report detail
        reportDetail: r.reportDetail || r['report detail'] || '(无额外日志随笔)',
        // 11. categories
        categories: r.categories || r['categories'] || [],
        // 12. tags
        tags: r.tags || r['tags'] || [],
        // 13. AI Wearing Analysis (historical summary)
        aiWearingAnalysis: r.aiWearingAnalysis || r['AI Wearing Analysis'] || '(无历史AI诊断)',
        // 14. Pictures
        picturesCount: Array.isArray(r['Pictures']) ? r['Pictures'].length : (r.picturesCount || 0),
        // 15. weather
        weather: r.weather || r['weather'] || r.customFields?.['weather'] || '(未明确天气)',
        // 16. temperature
        temperature: r.temperature || r['temperature'] || r.customFields?.['temperature'] || '(未明确气温)',
        // 17. overall satisfaction
        overallSatisfaction: r.overallSatisfaction || r['overall satisfaction'] || r.customFields?.['overall satisfaction'] || '(未评分)',
        // 18. activities
        activities: r.activities || r['activities'] || r.customFields?.['activities'] || '(未记录具体活动)',
        // 19. aiReports (version count)
        aiReportsCount: Array.isArray(r.aiReports) ? r.aiReports.length : (r.aiReportsCount || 0),
        // 20. created timestamp
        created: r.created || r['created'] || '',
        // Extra dynamic session fields
        customFields: r.customFields || r.extraAttributes || {},
        // List of worn garments directly recorded in this report
        wornGarmentTitles: Array.isArray(r.wornGarmentTitles) ? r.wornGarmentTitles : [],
        wornGarments: Array.isArray(r.wornGarments) ? r.wornGarments : []
      }));

      // Normalize Clothes Junction Records (linking items to sessions, wear duration & laundry status)
      const formattedJunctions = (junctions || []).map((j: any) => {
        const rId = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : (j.reportId || j['wearing report id']);
        const rTitle = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.title : (j.reportTitle || '');
        const cId = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : (j.clothesId || j['clothes(new)']);
        const cTitle = typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : (j.clothesTitle || '');
        const durSec = typeof j.duration === 'number' ? j.duration : (j.durationSeconds || 0);
        return {
          id: j.id,
          name: j.name || '#1',
          reportId: rId,
          reportTitle: rTitle,
          clothesId: cId,
          clothesTitle: cTitle,
          durationHours: +(durSec / 3600).toFixed(1),
          durationSeconds: durSec,
          washStated: j.washStated || j['wash stated'] || 'need wash',
          clothesType: j.clothesType || j['Clothes Type (from clothes(new))'] || '',
          wearDate: j.wearDate || j['wear date'] || '',
          washDate: j.washDate || j['wash date'] || j.washedDate || j['washed date'] || ''
        };
      });

      // Pre-aggregate exact historical wearing experiences for EACH candidate garment
      const candidateDetailedHistory = formattedItems.map((candidate: any) => {
        const cId = String(candidate.id || '').trim();
        const cName = String(candidate.name || '').trim().toLowerCase();

        // 1. Find all junctions linking to this candidate
        const matchedJunctions = formattedJunctions.filter((j: any) => {
          const jClothesId = String(j.clothesId || '').trim();
          const jClothesTitle = String(j.clothesTitle || '').trim().toLowerCase();
          return (jClothesId && jClothesId === cId) || (jClothesTitle && jClothesTitle === cName);
        });

        // 2. Find all reports linked via junctions or directly
        const reportIds = new Set(matchedJunctions.map((j: any) => String(j.reportId || '').trim()).filter(Boolean));
        const matchedReports = formattedReports.filter((r: any) => {
          if (reportIds.has(String(r.id || '').trim())) return true;
          if (r.wornGarmentTitles && r.wornGarmentTitles.some((t: string) => String(t || '').trim().toLowerCase() === cName)) return true;
          return false;
        });

        // 3. Extract exact sensory feedbacks recorded by user & companion garments (e.g. bra worn together)
        const historicalSessions = matchedReports.map((r: any) => {
          const junc = matchedJunctions.find((j: any) => String(j.reportId || '').trim() === String(r.id || '').trim());
          
          // Find companion garments worn in the exact same wearing report session (e.g. bras, bottoms, outer layers)
          const sessionCompanionJunctions = formattedJunctions.filter((cj: any) => 
            String(cj.reportId || '').trim() === String(r.id || '').trim() &&
            String(cj.clothesId || '').trim() !== cId &&
            String(cj.clothesTitle || '').trim().toLowerCase() !== cName
          );
          const companionGarments = sessionCompanionJunctions.map((cj: any) => {
            const rawGarment = (items || []).find((g: any) => 
              String(g.id || '').trim() === String(cj.clothesId || '').trim() ||
              String(g.name || g['Name'] || '').trim().toLowerCase() === String(cj.clothesTitle || '').trim().toLowerCase()
            );
            return {
              id: cj.clothesId,
              title: cj.clothesTitle,
              type: cj.clothesType || rawGarment?.type || rawGarment?.clothesType || rawGarment?.['Clothes Type'] || '',
              centerChest: rawGarment?.['center chest'] || rawGarment?.centerChest || '',
              sideBreastOk: rawGarment?.['side breast ok'] || rawGarment?.sideBreastOk || '',
              sitBreastOk: rawGarment?.['sit breast ok'] || rawGarment?.sitBreastOk || '',
              descriptions: rawGarment?.Descriptions || rawGarment?.descriptions || ''
            };
          });

          return {
            reportId: r.id,
            reportName: r.name,
            wearDate: r.wearDate,
            durationHours: junc ? junc.durationHours : undefined,
            washStated: junc ? junc.washStated : undefined,
            companionGarmentsWornTogether: companionGarments,
            userSensoryFeedback: {
              shoulderFeel: r.shoulderFeel,
              midBreast: r.midBreast,
              nightMidBreast: r.nightMidBreast,
              sweat: r.sweat,
              nightSweat: r.nightSweat,
              legFeel: r.legFeel,
              nightLegFeel: r.nightLegFeel,
              reportDetail: r.reportDetail,
              overallSatisfaction: r.overallSatisfaction,
              activities: r.activities
            }
          };
        });

        const latestJunction = matchedJunctions[matchedJunctions.length - 1];

        return {
          garmentId: candidate.id,
          garmentName: candidate.name,
          recordedProperties: {
            material: candidate.material,
            thickClothes: candidate.thickClothes,
            hotAtNoon: candidate.hotAtNoon,
            diaoDai: candidate.diaoDai,
            sitBreastOk: candidate.sitBreastOk,
            sideBreastOk: candidate.sideBreastOk,
            centerChest: candidate.centerChest,
            wet: candidate.wet,
            wetReason: candidate.wetReason,
            beautiful: candidate.beautiful,
            beatifulReason: candidate.beatifulReason,
            longDress: candidate.longDress,
            descriptions: candidate.descriptions,
            descriptions2: candidate.descriptions2,
            lastComment: candidate.lastComment,
            customFields: candidate.customFields,
            fieldExplanations: candidate.fieldExplanations,
            totalWashedCount: candidate.totalWashedCount,
            legacyWashed: candidate.legacyWashed,
            unwashedDurationCount: candidate.unwashedDurationCount
          },
          currentCleanlinessState: {
            latestWashStated: latestJunction?.washStated || (candidate.unwashedDurationCount > 0 ? 'need wash' : 'wash'),
            totalRecordedWearSessions: matchedJunctions.length,
            totalHistoricalWashes: candidate.totalWashedCount,
            unwashedHours: +(candidate.unwashedDurationCount / 3600).toFixed(1)
          },
          historicalSessions
        };
      });

      const candidateNamesList = formattedItems.map((c: any) => `"${c.name}"`).join('、');
      const systemInstruction = `You are a personalized, highly perceptive wardrobe intelligence and ergonomics consultant (Macro AI Advisor).

【CRITICAL CANDIDATE TARGET LOCK / 候选目标绝对锁定第一铁律】:
The user has explicitly specified the following candidate garment(s) for this decision:
👉 【 ${candidateNamesList || "全衣橱候选"} 】
You MUST focus your evaluation, recommendation, wearing report citations, and laundry checks EXCLUSIVELY on the candidate garment(s) specified above!
DO NOT switch subjects or recommend unselected dresses from the wardrobe (for example, NEVER analyze or recommend "blue diao dai thick" when the user selected "pink flower rm30" or other garments)!

CRITICAL ARCHITECTURAL CONSTITUTION:
You MUST base your analysis 100% on the real user data provided for the TARGET candidate garment(s). NEVER hallucinate fictional report IDs, dates, or body feedback!
Specifically, you are provided with:
1. 👗 Garment Attributes & Recorded Descriptions of the Target Candidate(s):
   - Physical thickness, fabric composition ("material", "thick clothes", "long dress").
   - Peak midday heat tolerance ("Hot at noon" - cool/middle/hot).
   - Strap ergonomics ("diao dai" - e.g. "not diao dai" vs "wide diao dai" vs "thin diao dai").
   - Bust fit feedback ("sit breast ok", "side breast ok", "center chest").
   - User notes ("Descriptions", "Descriptions 2", "last comment or action", "beatiful reason", "wet reason").
   - Custom attributes & field ergonomic explanations ("customFields", "fieldExplanations").
   - Wash counts: Total Washed Count, legacy washed, unwashed duration count.
2. 📝 Historical Real-Life Wearing Reports & Exact User Quotes for the Target Candidate(s):
   - Exact sensory entries from the user's wear logs across 20 dimensions:
     * shoulder feel (引用该目标衣物专属的真实肩部感受，例如 "the not diao dai make the shoulder no so irrate and pain." 或其他真实记录)
     * mid breast (引用该目标衣物专属的真实胸口体感，例如关于剪裁褶皱、中位聚拢的真实记录)
     * sweat (引用该目标衣物专属的真实出汗记录)
     * leg feel (引用该目标衣物专属的真实腿部/裙长感受，例如 "short, so middle cold leg")
     * report detail, wear date, report name.
3. ⏱️ Clothes-Junction Pairing & Cleanliness Status of the Target Candidate(s):
   - Exact recorded duration in hours (e.g. 5.5h, 8.7h 等该单品的实际历史时长).
   - Cleanliness state ("wash" = clean/washed, "need wash" = dirty/pending wash).
   - Total washes completed (Total Washed Count).

${dictionaryPrompt || ""}

STRICT FACTUAL GROUNDING RULES:
1. DO NOT HALLUCINATE: Never invent report IDs (such as "rec_report_xxx" or random timestamps). Always cite the EXACT report name and date from the candidate's actual linked data (e.g. "2026-06-23 0833 wearing report", "2026-06-25 2140 wearing report").
2. QUOTE USER'S EXACT WORDS & DESCRIPTIONS:
   - When analyzing past experiences, quote the user's actual recorded entries for THIS garment.
   - Deeply examine and cite the candidate's "Descriptions" and "Descriptions 2" (including past underwear pairings, room air conditioning/fan/blanket usage, and folding/wrinkle behaviors).
3. ACCURATE CLEANLINESS & WASH NUMBERS:
   - Check the target garment's Total Washed Count and its latest Junction washStated.
   - Accurately report whether it is clean ("wash") or pending laundry ("need wash").
4. ACCURATE STRAP & DRESS SPECS:
   - Check the exact "diao dai" field: If it is "not diao dai" (非吊带，如普通袖或宽肩设计)，explain that shoulders experience no strap pressure or pain ("shoulder no so irrate and pain"). If it is "wide diao dai", mention wide strap slip tendencies. NEVER confuse "not diao dai" with strap dresses!
5. 🧶 PHYSICAL MATERIAL & THICKNESS MUST BE FAITHFULLY READ (严禁虚假声称材质与厚度缺失):
   - You MUST read and recognize the garment's recorded "material" (如 "Chiffon" 雪纺) and "thick clothes" (如 "02 middle thick clothes" 中等厚度) directly from 【TARGET CANDIDATE SPECS】.
   - If the candidate garment has material = "Chiffon" and thickClothes = "02 middle thick clothes", you MUST state them explicitly!
   - NEVER CLAIM THAT "目前数据缺失了该衣物的材质属性记录(Material/ThickClothes)"! The material and thickness are ALREADY fully provided in the data. Any claim that material or thickness is unrecorded is a severe violation.
6. 👙 GARMENT-TO-BRA SYNERGY & CHEST SUPPORT PRINCIPLES (文胸与裙装聚拢协同规律):
   - Check the target garment's 'center chest', 'sit breast ok', and user 'Descriptions'.
   - If the candidate dress has 'center chest' === 'no center chest':
     * The dress itself provides ZERO center chest gather/cleavage support.
     * Check historical companion bras: If history notes "big bra better for this clothes", explicitly recommend pairing with a supportive big bra / center-gathering bra ("yes center chest") to resolve chest wrinkling ("zhouwen") or sagging!
     * NEVER recommend "无钢圈轻薄型内衣" or "小型无聚拢内衣" for a dress that lacks center support!
7. FLUENT CHINESE OUTPUT STRUCTURE:
   You MUST output in fluent, crystal-clear CHINESE (简体中文) focusing 100% on 【 ${candidateNamesList || "当前候选"} 】 using exactly these Markdown headings:
   ### 🌟 智能穿搭推荐方案与搭配理由（针对当前候选 ${candidateNamesList}，明确指出其材质 ${formattedItems.map((c: any) => c.material).join('、')} 与厚度 ${formattedItems.map((c: any) => c.thickClothes).join('、')}，重点阐述文胸与裙装的聚拢支撑协同方案）
   ### 🔍 历史 20 维真实体感复盘与避坑预警（引用针对 ${candidateNamesList} 过往真实 Wearing Report 日期、报告名称与肩部、胸口、出汗、腿部真实反馈与搭配的文胸）
   ### ⏱️ 穿着时长负荷与耐受度预判（引用针对 ${candidateNamesList} 的 Junction 真实历史单次穿着时长耐受表现）
   ### 🧺 洗护与可用状态核实（核实针对 ${candidateNamesList} 的真实总洗护次数与最新洗护标记状态）
   ### 📌 缺失体感盲区与下次穿搭观察指引（绝对不得将已有的材质、厚度、或者在Descriptions中已记录的内容误列为盲区，只可建议观察未记录的细微体感）
`;

      const userMessage = `=== 1. 【本次唯一指定的候选衣物档案】 (TARGET CANDIDATE GROUNDED HISTORY) ===
${JSON.stringify(candidateDetailedHistory, null, 2)}

=== 2. 【本次候选衣物属性明细】 (TARGET CANDIDATE SPECS) ===
${JSON.stringify(formattedItems, null, 2)}

=== 3. 关联穿搭日志明细 (LINKED WEARING REPORTS FOR CANDIDATE) ===
${JSON.stringify(formattedReports, null, 2)}

=== 4. 关联 Junction 记录 (LINKED JUNCTION RECORDS FOR CANDIDATE) ===
${JSON.stringify(formattedJunctions, null, 2)}

=== 5. 本次穿搭决策环境与诉求 ===
- 指定候选衣物: ${candidateNamesList}
- 中午炎热度/天气设置 (Temperature / Weather): ${temperature || "不限 / 常规"}
- 穿着场合 (Occasion): ${occasion || "不限 / 常规"}
- 核心体感诉求 (Mood / Physical Priority): ${mood || "不限 / 常规"}
- 用户具体问题 (User Prompt): ${prompt || "请基于上述当前选定的候选衣物真实指标、历史20维穿搭体验与Junction时长洗护数据，给出最适宜的搭配方案、体感避坑复盘与洗护核对。"}
`;

      const response = await ai.models.generateContent({
        model,
        contents: [
          { role: "user", parts: [{ text: `${systemInstruction}\n\n${userMessage}` }] }
        ],
        config: {
          maxOutputTokens: 8192,
          temperature: 0.2
        }
      });

      res.json({ advice: response.text || "No advice generated.", model });
    } catch (error: any) {
      handleGeminiError(error, res, "Failed to generate AI advice.");
    }
  });

  // AI Garment Assessment Generator (Teable-Style Deep Grounded Diagnostics)
  app.post("/api/ai/analyze-garment", async (req, res) => {
    try {
      if (isAiGenerationPausedServer(req)) {
        return res.json({
          status: "paused",
          paused: true,
          error: "AI generation is paused in settings to save tokens and execution time. You can review existing records or use Google NotebookLM for analysis.",
          analysis: "AI generation is currently paused in Settings."
        });
      }
      const { garment, apiKey, model: reqModel, dictionaryPrompt, reports: linkedReports, junctions: linkedJunctions } = req.body;
      if (!garment) {
        return res.status(400).json({ error: "Garment data is required." });
      }

      const { ai, model } = getGeminiClient(apiKey, reqModel);

      // Extract all properties with extensive aliases
      const name = garment.name || garment['Name'] || garment.title || '(未命名单品)';
      const clothesType = garment.clothesType || garment['Clothes Type'] || garment.type || 'Dress';
      const material = garment.material || garment['material'] || garment.materialProp || '(未记录面料)';
      const thickClothes = garment.thickClothes || garment['thick clothes'] || garment.thickness || '(未记录厚度)';
      const hotAtNoon = garment.hotAtNoon || garment['Hot at noon'] || '(未记录中午炎热度)';
      const diaoDai = garment.diaoDai || garment['diao dai'] || '(未记录吊带款式)';
      const sitBreastOkGather = garment['sit breast ok: With 聚拢bra'] || garment['sit breast ok: with 聚拢bra'] || garment.sitBreastOkWithGatherBra || '';
      const sitBreastOkSideOut = garment['sit breast ok: with side out bra'] || garment['sit breast ok: With side out bra'] || garment.sitBreastOkWithSideOutBra || '';
      const sitBreastOk = garment.sitBreastOk || garment['sit breast ok'] || '(未记录坐姿胸型)';
      const sideBreastOkGather = garment['side breast ok: with 聚拢 bra'] || garment['side breast ok: With 聚拢bra'] || garment.sideBreastOkWithGatherBra || '';
      const sideBreastOkSideOut = garment['side breast ok: with side out bra'] || garment['side breast ok: With side out bra'] || garment.sideBreastOkWithSideOutBra || '';
      const sideBreastOk = garment.sideBreastOk || garment['side breast ok'] || '(未记录侧面胸型)';
      const whenAircondHot = garment['when with aircond: hot feeling'] || '';
      const whenAircondLeg = garment['when with aircond: leg feeling'] || '';
      const whenNoAircondHot = garment['when without aircond: hot feeling'] || '';
      const whenNoAircondLeg = garment['when without aircond: leg feeling'] || '';
      const whenSleepFanHot = garment['when sleep under fan: hot feeling'] || '';
      const whenSleepFanLeg = garment['when sleep under fan: leg feeling'] || '';
      const centerChest = garment.centerChest || garment['center chest'] || '(未记录中位聚拢)';
      const wet = garment.wet || garment['wet'] || 'dry';
      const wetReason = garment.wetReason || garment['wet reason'] || garment['wetReason'] || '';
      const beautiful = garment.beautiful || garment['beautiful'] || '(未评分)';
      const beatifulReason = garment.beatifulReason || garment['beatiful reason'] || garment['beautiful reason'] || garment.beautifulReason || '';
      const longDress = garment.longDress || garment['long dress'] || garment.skirtLength || '';
      const mayThrow = garment.mayThrow || garment['may throw'] || '';
      const price = garment.price !== undefined && garment.price !== '' ? `$${garment.price}` : '(未记录买入价格)';
      const totalWashedCount = garment.totalWashedCount ?? garment['Total Washed Count'] ?? 0;
      const legacyWashed = garment.legacyWashed ?? garment['wearing frequency(how many times legacy washed)'] ?? 0;
      const lastWashed = garment.lastWashed || garment['Last Washed'] || '(无最近洗涤记录)';

      // Long descriptions and notes (description, description 2, last action)
      const desc1 = garment.descriptions || garment['Descriptions'] || garment.description || garment['Description'] || garment.discription || garment['discription'] || '';
      const desc2 = garment.descriptions2 || garment['Descriptions 2'] || garment.description2 || garment['Description 2'] || garment.discription2 || garment['discription 2'] || '';
      const lastAction = garment.lastComment || garment['last comment or action'] || garment.lastAction || garment['last action'] || garment['last comment'] || garment.lastCommentOrAction || '';

      // Field explanations (ergonomic dimension-specific notes)
      const fieldExplanations = garment.fieldExplanations || garment.customFields?._fieldExplanations || {};
      let fieldExplanationsText = '';
      if (fieldExplanations && typeof fieldExplanations === 'object' && Object.keys(fieldExplanations).length > 0) {
        fieldExplanationsText = Object.entries(fieldExplanations)
          .filter(([k, v]) => Boolean(v))
          .map(([k, v]) => `  - 【${k}】: ${v}`)
          .join('\n');
      }

      // Custom attributes and extra fields
      const customObj: Record<string, any> = {
        ...(garment.customFields || {}),
        ...(garment.extraAttributes || {})
      };
      delete customObj._fieldExplanations;

      // Also gather any non-standard custom attributes present on garment
      const standardKeys = new Set([
        'id', 'name', 'Name', 'title', 'clothesType', 'Clothes Type', 'type', 'material', 'materialProp',
        'thick clothes', 'thickClothes', 'thickness', 'Hot at noon', 'hotAtNoon', 'diao dai', 'diaoDai',
        'side breast ok', 'sideBreastOk',
        'side breast ok: with 聚拢 bra', 'side breast ok: With 聚拢bra', 'sideBreastOkWithGatherBra',
        'side breast ok: with side out bra', 'side breast ok: With side out bra', 'sideBreastOkWithSideOutBra',
        'sit breast ok', 'sitBreastOk',
        'sit breast ok: With 聚拢bra', 'sit breast ok: with 聚拢bra', 'sitBreastOkWithGatherBra',
        'sit breast ok: with side out bra', 'sit breast ok: With side out bra', 'sitBreastOkWithSideOutBra',
        'when with aircond: hot feeling', 'when with aircond: leg feeling',
        'when without aircond: hot feeling', 'when without aircond: leg feeling',
        'when sleep under fan: hot feeling', 'when sleep under fan: leg feeling',
        'center chest', 'centerChest',
        'beautiful', 'beatiful reason', 'beatifulReason', 'beautiful reason', 'wet', 'wet reason', 'wetReason',
        'long dress', 'longDress', 'price', 'wearing frequency(how many times legacy washed)', 'Tracked App Washes',
        'Total Washed Count', 'totalWashedCount', 'legacyWashed', 'Last Washed', 'lastWashed', 'AI Clothes Analysis',
        'aiReports', 'Pictures', 'may throw', 'mayThrow', 'last comment or action', 'lastComment', 'last action',
        'tags', 'created', 'unwash time rollup', 'wash stated (from Full Wearing Report (new design) clothes list (linked))',
        'customFields', 'extraAttributes', 'fieldExplanations', 'descriptions', 'Descriptions', 'descriptions2',
        'Descriptions 2', 'description', 'Description', 'description2', 'Description 2', 'discription', 'discription2'
      ]);
      for (const [k, v] of Object.entries(garment)) {
        if (!standardKeys.has(k) && v !== undefined && v !== null && v !== '') {
          customObj[k] = v;
        }
      }

      // Separate generic custom fields from field explanations (*_explain)
      const genericCustomEntries: Array<[string, any]> = [];
      const explainCustomEntries: Array<[string, any]> = [];
      for (const [k, v] of Object.entries(customObj)) {
        if (k.endsWith('_explain')) {
          explainCustomEntries.push([k, v]);
        } else if (!k.startsWith('_')) {
          genericCustomEntries.push([k, v]);
        }
      }

      let customFieldsText = '';
      if (genericCustomEntries.length > 0) {
        customFieldsText = genericCustomEntries
          .map(([k, v]) => `  - 【单品自定义属性: "${k}"】 => 【用户实际填写的真实取值: "${typeof v === 'object' ? JSON.stringify(v) : v}"】 (注: 必须原样引用此字面真实值进行分析，绝对严禁根据衣服名称脑补猜测颜色或特性！)`)
          .join('\n');
      }

      let explainFieldsText = '';
      if (explainCustomEntries.length > 0) {
        explainFieldsText = explainCustomEntries
          .map(([k, v]) => `  - 【维度体感专属说明: "${k}"】 => 【用户真实记录: "${typeof v === 'object' ? JSON.stringify(v) : v}"】`)
          .join('\n');
      }

      // Collect environmental and custom attributes across all linked wearing reports
      const standardReportKeys = new Set([
        'id', 'name', 'Name', 'wear date', 'wearDate', 'created', 'duration', 'reportDetail', 'report detail',
        'shoulder feel', 'shoulderFeel', 'mid breast', 'midBreast', 'night mid breast', 'nightMidBreast',
        'sweat', 'sweatProp', 'night sweat', 'nightSweat', 'leg feel', 'legFeel', 'night leg feel', 'nightLegFeel',
        'tags', 'categories', 'Pictures', 'picturesCount', 'customFields', 'extraAttributes', 'fieldExplanations',
        'wornGarmentTitles', 'aiReports', 'AI Wearing Analysis'
      ]);
      const reportCustomSummaryList: string[] = [];
      if (Array.isArray(linkedReports)) {
        linkedReports.forEach((r: any, idx: number) => {
          const rCustom: Record<string, any> = {
            ...(r.customFields || {}),
            ...(r.extraAttributes || {})
          };
          delete rCustom._fieldExplanations;
          for (const [k, v] of Object.entries(r)) {
            if (!standardReportKeys.has(k) && v !== undefined && v !== null && v !== '') {
              rCustom[k] = v;
            }
          }
          const valid = Object.entries(rCustom).filter(([k]) => !k.startsWith('_'));
          if (valid.length > 0) {
            const date = r.wearDate || r['wear date'] || r.created || '(未注明日期)';
            const items = valid.map(([k, v]) => `【${k}: "${typeof v === 'object' ? JSON.stringify(v) : v}"】`).join(', ');
            reportCustomSummaryList.push(`  - 穿搭实测报告 #${idx + 1} (${date}): ${items}`);
          }
        });
      }
      const allReportCustomStr = reportCustomSummaryList.join('\n');

      // Linked wearing reports & companion items detailed ergonomics
      const reportCount = Array.isArray(linkedReports) ? linkedReports.length : 0;
      let sessionsSummaryText = '';
      if (reportCount > 0) {
        sessionsSummaryText = linkedReports.map((r: any, idx: number) => {
          const date = r.wearDate || r['wear date'] || r.created || '(未注明日期)';
          const reportName = r.name || r['Name'] || `Wearing Report #${idx + 1}`;
          const durationVal = r.duration ? `${Math.round(r.duration / 3600 * 10) / 10}小时` : '';
          const detail = r.reportDetail || r['report detail'] || '';
          
          // Companions list
          let companion = Array.isArray(r.wornGarmentTitles) ? r.wornGarmentTitles.join(' + ') : (r.companion || '');
          if (!companion && Array.isArray(linkedJunctions)) {
            const rId = r.id;
            const rName = (r.Name || r.name || '').trim().toLowerCase();
            const sameSessionCompanions = linkedJunctions.filter((j: any) => {
              const jRepId = typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : (j.wearingReportId || j['wearing report id']);
              const jRepTitle = (typeof j['wearing report id'] === 'object' ? j['wearing report id']?.title : (j.wearingReportTitle || '')).trim().toLowerCase();
              return (rId && jRepId === rId) || (rName && jRepTitle === rName);
            }).map((j: any) => {
              return typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : (j.clothesTitle || '');
            }).filter((t: string) => Boolean(t) && t.toLowerCase() !== name.toLowerCase());

            if (sameSessionCompanions.length > 0) {
              companion = Array.from(new Set(sameSessionCompanions)).join(' + ');
            }
          }

          // Ergonomic dimensions from the report
          const midBreast = r.midBreast || r['mid breast'] || '';
          const nightMidBreast = r.nightMidBreast || r['night mid breast'] || '';
          const shoulderFeel = r.shoulderFeel || r['shoulder feel'] || '';
          const sweat = r.sweatProp || r['sweat'] || '';
          const nightSweat = r.nightSweat || r['night sweat'] || '';
          const legFeel = r.legFeel || r['leg feel'] || '';
          const nightLegFeel = r.nightLegFeel || r['night leg feel'] || '';
          
          // Custom attributes on the wearing report (e.g. Weather, Temperature, AC, etc.)
          const rCustom: Record<string, any> = {
            ...(r.customFields || {}),
            ...(r.extraAttributes || {})
          };
          delete rCustom._fieldExplanations;
          for (const [k, v] of Object.entries(r)) {
            if (!standardReportKeys.has(k) && v !== undefined && v !== null && v !== '') {
              rCustom[k] = v;
            }
          }
          const rCustomEntries = Object.entries(rCustom).filter(([k]) => !k.startsWith('_'));
          const rCustomStr = rCustomEntries.length > 0 
            ? rCustomEntries.map(([k, v]) => `【${k}: "${typeof v === 'object' ? JSON.stringify(v) : v}"】`).join(', ')
            : '';

          const parts = [
            `【穿搭实测报告 ${idx + 1}】: ${reportName} (穿搭日期: ${date}${durationVal ? `, 时长: ${durationVal}` : ''})`,
            companion ? `  - 同穿/搭配内衣与单品: ${companion}` : '  - 同穿/搭配内衣与单品: (未特别记录同穿款)',
            detail ? `  - 记录时段/随笔描述: ${detail}` : null,
            midBreast ? `  - 前中/日间胸部受力 (Mid breast): ${midBreast}` : null,
            nightMidBreast ? `  - 晚间胸部放松/文胸贴合 (Night mid breast): ${nightMidBreast}` : null,
            shoulderFeel ? `  - 肩部与肩带受力感受 (Shoulder feel): ${shoulderFeel}` : null,
            sweat ? `  - 日间透气排汗与炎热度 (Sweat): ${sweat}` : null,
            nightSweat ? `  - 晚间排汗与风扇体感 (Night sweat): ${nightSweat}` : null,
            legFeel ? `  - 下身/裙下摆活动体感 (Leg feel): ${legFeel}` : null,
            nightLegFeel ? `  - 晚间下肢体感与凌晨防寒 (Night leg feel): ${nightLegFeel}` : null,
            rCustomStr ? `  - 穿着环境与自定义属性 (Environmental & Custom Attributes): ${rCustomStr}` : null,
          ].filter(Boolean);

          return parts.join('\n');
        }).join('\n\n');
      }

      const promptText = `You are an expert, meticulous wardrobe diagnostics and ergonomics advisor modeled on the user's Teable AI diagnostic system.
Your job is to produce a deeply grounded, specific analysis of this garment based EXACTLY on all the fields the user has recorded, including physical comfort, thermal behavior, strap ergonomics, AND ALL USER-RECORDED LONG DESCRIPTIONS, DISCRIPTION 2, LAST ACTIONS, AND CUSTOM ATTRIBUTES, AS WELL AS ALL LINKED WEARING REPORTS (共 ${reportCount} 篇实测穿着报告，必须从报告 1 一直到报告 ${reportCount} 全部逐一复盘！).
DO NOT provide generic marketing fluff. Ground your inferences in the exact values provided below.

${dictionaryPrompt || ""}

CRITICAL OUTPUT LANGUAGE & STRICT INTEGRATION RULES:
1. You MUST output your ENTIRE analysis in fluent, natural, crystal-clear CHINESE (简体中文).
2. 【MANDATORY: 深度融入并引用用户详述与复盘 (DESCRIPTIONS, DESCRIPTIONS 2 & LAST ACTION)】:
   - The user has spent time writing rich, long data in Descriptions (描述1), Descriptions 2 (描述2), and Last Action/Comment (最新行动与评语).
   - You MUST read, quote, and thoroughly analyze these texts in the report!
   - 严禁对用户填写的长篇详述视而不见！
3. 【MANDATORY: 完整逐一复盘下方全部 ${reportCount} 篇关联实测穿着报告 (ALL ${reportCount} WEARING REPORTS, FROM REPORT 1 TO REPORT ${reportCount})】:
   - 系统在下方提供了全部 ${reportCount} 篇关联实测报告数据（无论有 3 篇、5 篇、10 篇、还是 20 篇报告！）。
   - 你必须在“关联穿搭实测报告深度复盘”章节中，对从【穿搭实测报告 1】一直到【穿搭实测报告 ${reportCount}】的每一篇报告，逐一单独设立编号条目（如 1. 【穿搭实测报告 1】... 10. 【穿搭实测报告 10】... ${reportCount}. 【穿搭实测报告 ${reportCount}】）进行全面细致的复盘！
   - 绝对严禁因为报告篇数较多就中途停止、打包截断或跳过中间报告！每一篇都必须完整列出其穿搭日期、内衣与同穿单品搭配、日间与夜间胸部受力、排汗、裙摆防寒，以及该篇报告登记的全部环境与自定义属性（如 Weather 等）！
4. 【CRITICAL MANDATORY: 100% 完整覆盖并逐一剖析所有自定义属性，绝对严禁遗漏任何一个自定义属性，严禁脑补与猜想 (ABSOLUTE 100% FIDELITY & INCLUSION OF ALL CUSTOM ATTRIBUTES)】:
   - 系统在下方【🏷️ 单品专属自定义属性与扩展字段】、【📐 维度体感与面料扩展说明】以及【🌦️ 关联穿着报告的环境与自定义属性汇总】中提供了用户登记的所有动态属性。
   - 【无遗漏强制要求 (NO OMISSION RULE)】:
     * 你必须在“最新行动与自定义属性分析”章节中，把下方列出的每一个自定义属性（例如 Color、Stretch、material_explain、thick clothes_explain、Weather 等）都逐一单独作为一个子条目（Bullet Point）进行分析！
     * 绝对严禁只分析 1-2 个属性就停下！用户填写的每一个属性都必须 100% 全部单独列出并深度分析！
   - 【严禁脑补与猜想（STRICT PROHIBITION OF GUESSING）】:
     * 必须且只能依据用户登记的实际字段字面取值（User's Actual Value）进行分析！
     * 绝对严禁根据衣服名称擅自猜想或脑补属性取值！
     * 例如：如果用户登记了属性【Color】（颜色），其实际取值为用户填写的具体内容，你必须严格使用用户填写的那个字面值进行分析，绝对严禁因为衣服叫 "apricot french rm30" 或 "blue diao dai thick" 就自作聪明脑补为 "Apricot/杏色" 或 "Blue/蓝色"！用户强烈投诉：“我加的 color 属性取值不是颜色，AI 却自己脑补猜颜色，不遵循我的实际字段数据！”
     * 无论用户填入的是特定文字、数字、说明、符号或代码，你必须原汁原味地引用并在分析中列出该真实值！
   - 在报告中，针对每一个自定义属性，必须使用标准格式输出：
     - **属性名（用户登记真实取值：xxx）：** [基于该真实取值的具体深度分析]
5. 【STRICT BLIND SPOT INTEGRATION (严禁将已有事实谎称为缺失或盲区)】:
   - If information (such as fabric material, thickness, bra pairing synergy, temperature response, or wrinkling) is ALREADY recorded in Descriptions, Descriptions 2, Last Action, Custom Attributes, or Wearing Reports, you MUST NEVER claim that it is "missing" or an "unknown blind spot" in the "缺失数据与盲区" section!

=== GARMENT CORE PROFILE (单品核心指标档案) ===
- 衣服名称 (Name): ${name}
- 类别 (Type): ${clothesType}
- 材质面料 (Material): ${material}
- 厚薄等级 (Thickness / Thick Clothes): ${thickClothes}
- 裙长与款式规格 (Long Dress / Style): ${longDress || '(未专门记录)'}
- 中午炎热度 (Heat at Noon): ${hotAtNoon}
- 领口与吊带设计 (Strap / Diao Dai): ${diaoDai}
- 坐姿正面胸型体感 (Sit Breast Ok):
  * 配合聚拢bra (sit breast ok: With 聚拢bra): ${sitBreastOkGather || '(未单独记录，参考历史记录: ' + sitBreastOk + ')'}
  * 配合侧开/非聚拢bra (sit breast ok: with side out bra): ${sitBreastOkSideOut || '(未单独记录，参考历史记录: ' + sitBreastOk + ')'}
  * 综合/历史总览 (Sit Breast Ok): ${sitBreastOk}
- 侧面胸型体感 (Side Breast Ok):
  * 配合聚拢bra (side breast ok: with 聚拢 bra): ${sideBreastOkGather || '(未单独记录，参考历史记录: ' + sideBreastOk + ')'}
  * 配合侧开/非聚拢bra (side breast ok: with side out bra): ${sideBreastOkSideOut || '(未单独记录，参考历史记录: ' + sideBreastOk + ')'}
  * 综合/历史总览 (Side Breast Ok): ${sideBreastOk}
- 🌡️ 多场景气候与温感/下肢体感 (Multi-Scenario Climate & Sensory Ergonomics):
  * ❄️ 空调环境 (when with aircond):
    - hot feeling (热感/冷感): ${whenAircondHot || '(未单独记录)'}
    - leg feeling (腿部感受): ${whenAircondLeg || '(未单独记录)'}
  * ☀️ 无空调/常温/中午环境 (when without aircond):
    - hot feeling (热感/汗感): ${whenNoAircondHot || hotAtNoon || '(未单独记录)'}
    - leg feeling (腿部感受): ${whenNoAircondLeg || '(未单独记录)'}
  * 🌀 风扇睡眠环境 (when sleep under fan):
    - hot feeling (睡眠热感): ${whenSleepFanHot || '(未单独记录)'}
    - leg feeling (睡眠腿部体感/翻卷冷脚): ${whenSleepFanLeg || '(未单独记录)'}
- 中位聚拢支撑 (Center Chest): ${centerChest}
- 出汗与干爽表现 (Wet / Dry Behavior): ${wet} ${wetReason ? `(出汗/潮湿原因: ${wetReason})` : ''}
- 美观度评分 (Beauty Rating): ${beautiful} ${beatifulReason ? `(美观原因: ${beatifulReason})` : ''}
- 处置倾向 (May Throw / Retain): ${mayThrow || '(保留)'}
- 买入单价 (Price): ${price}
- 累计洗护次数 (Total Washes): ${totalWashedCount} (Legacy Washes: ${legacyWashed})
- 最近洗护日期 (Last Washed): ${lastWashed}

=== 📖 用户深度穿着记录与长篇详述 (DESCRIPTIONS & REVIEWS) ===
【用户主观穿着体验与详述 1 (Descriptions)】:
${desc1 ? desc1 : '(暂无用户描述1)'}

【深度历史穿搭复盘与详述 2 (Descriptions 2)】:
${desc2 ? desc2 : '(暂无用户描述2)'}

【最新处置行动与状态备注 (Last Comment or Action / Last Action)】:
${lastAction ? lastAction : '(暂无最新行动或评论)'}

=== 🏷️ 单品专属动态自定义属性与扩展字段 (USER-RECORDED GARMENT CUSTOM ATTRIBUTES) ===
${customFieldsText || '(暂无单品动态自定义属性)'}

=== 📐 各维度专属人体工学与体感说明 (FIELD EXPLANATIONS / ERGONOMIC NOTES) ===
${fieldExplanationsText || explainFieldsText || '(暂无专属维度体感说明)'}

=== 🌦️ 关联穿着报告的环境与自定义属性汇总 (WEARING REPORTS ENVIRONMENTAL & CUSTOM ATTRIBUTES) ===
${allReportCustomStr || '(各关联穿着报告中未登记额外环境或自定义属性)'}

=== 📑 全部关联实测穿着报告详情 (LINKED WEARING REPORTS: 共 ${reportCount} 篇实测日志) ===
${sessionsSummaryText || '(暂无关联穿着报告数据)'}

=== OUTPUT STRUCTURE (请严格使用下列中文 Markdown 标题与结构输出) ===
### 综合评估：事实记录与人体工学推断
- 提炼客观事实与人体工学推断。结合面料材质、厚度、版型剪裁及买入单价评估在衣橱中的定位与性价比。

### 用户详述与深度穿搭复盘（融会 Descriptions 与 Descriptions 2）
- 深度提取并专门引用用户在 Descriptions（描述1）和 Descriptions 2（描述2）中记录的所有长篇日志与复盘细节。
- 重点剖析用户提到的历史同穿单品（如特定内衣组合、侧开 vs 聚拢协同效果）、空调与风扇环境体感、起皱情况等真实反馈。
- 如果用户未记录长篇详述，明确说明“当前尚未登记长篇穿搭详述”。

### 关联穿搭实测报告深度复盘（全部 ${reportCount} 篇实测报告 与 内衣/环境协同）
- 必须逐一完整复盘上方传入的全部 ${reportCount} 篇实测穿着报告（从【穿搭实测报告 1】一直到【穿搭实测报告 ${reportCount}】，包含全部 1..${reportCount} 篇目，绝不漏掉任何一篇！）。
- 每一篇实测报告必须单独编号列出：
  * **同穿搭配与协同**：分析同穿单品（如特定内衣组合、侧开 vs 聚拢协同效果）。
  * **日间与夜间体感**：胸部承托、肩带受力（Shoulder feel）、排汗（Sweat/Night sweat）、裙摆活动与防寒（Leg feel/Night leg feel）。
  * **实测报告专属自定义属性与环境**：如果该穿着报告记录了环境或自定义属性（如 Weather 天气、温度等），必须以【字段名: "真实取值"】形式完整列出并分析！
- 若暂无关联报告，则简要说明。

### 最新行动与自定义属性分析（Last Action & Custom Attributes）
- 细致复盘用户填写的最新操作/评注（Last Comment or Action）。
- 重点逐一剖析用户登记的每一个自定义属性（包括单品自定义属性、维度体感说明、以及关联穿着报告中的关键自定义属性！严格依据【用户实际填写的真实取值】，绝对严禁脑补为衣服名称相关的颜色或词汇！每一个属性都必须标出其真实取值并进行客观分析）：
  * **属性名（用户登记真实取值：xxx）：** [基于该真实取值的具体深度分析]
- 结合处置倾向评估用户当前的可行策略（例如是否需抗皱打理、特定场合专属穿搭、或者保留/调整）。

### 舒适度与气候适应（特别是三大生活场景温控表现 与 透气排汗）
- 重点对三大生活环境的温控表现展开深度对比诊断（无遗漏）：
  * 【❄️ when with aircond】（开空调房：剖析 hot feeling 热感与 leg feeling 腿部感受，结合面料厚度、长裙覆盖与保温效果）
  * 【☀️ when without aircond】（无空调/常温/中午高温：剖析 hot feeling 炎热度与 leg feeling 腿部感受，透气吸湿与排汗闷热表现）
  * 【🌀 when sleep under fan】（风扇睡眠环境：剖析夜间 hot feeling 与 leg feeling 腿部体感，风扇风力散热 vs 裙底易翻卷/内衬短薄导致冷脚分析）
- 剖析厚薄度、中午炎热耐受度、透气吸湿表现及出汗原因（wet reason）。

### 版型贴合与外观体感（肩带 diao dai、久坐胸部 sit breast、侧面胸型 side breast 与裙长 long dress）
- 重点针对两大内衣搭配方案，分别对坐姿正面胸型与侧面胸型展开深度对比分析：
  * 【配合聚拢bra】：分析正面【sit breast ok: With 聚拢bra】与侧面【side breast ok: with 聚拢 bra】的立体挺拔度与承托稳定性
  * 【配合侧开/非聚拢bra】：分析正面【sit breast ok: with side out bra】与侧面【side breast ok: with side out bra】的上部坍塌、扁平及副乳外扩风险
- 细致评估肩带对斜方肌压迫感（若非吊带则明确其无滑肩与无勒痛优势）、工位久坐时胸部底围压迫感与副乳包裹、以及中位聚拢（Center Chest）与裙长覆盖。
- 结合美观度评分及原因（beatiful reason）分析视觉呈现。

### 洗护与保洁优先级
- 依据洗涤次数（Total Washes）与待洗状态提出保养建议，结合易起皱或厚薄特性给出洗涤脱水与晾晒指引。

### 单次穿着成本与利用率 ($/wear)
- 计算单次穿着成本（买入价 ÷ 洗涤/穿着次数）与性价比。

### 缺失数据与盲区
- 明确指出该单品目前尚为空白或未明确的物理与体感维度。
- 注意：凡是在 Descriptions、Descriptions 2、Last Action、Custom Attributes 或 Wearing Reports 中已记录的内容，绝对不得列为盲区！

### 下次穿着建议关注的体感
- 给出清晰具体的行动指引（例如下次在特定气温、久坐时长或搭配特定文胸时专门观察的体感指标）。`;

      const response = await ai.models.generateContent({
        model,
        contents: [{ role: "user", parts: [{ text: promptText }] }],
        config: {
          maxOutputTokens: 8192,
          temperature: 0.2
        }
      });

      res.json({ analysis: response.text || "No analysis generated." });
    } catch (error: any) {
      handleGeminiError(error, res, "Failed to analyze garment.");
    }
  });

  // AI Wearing Report Assessment Generator (Teable-Style Deep Session Grounding)
  app.post("/api/ai/analyze-wearing-report", async (req, res) => {
    try {
      if (isAiGenerationPausedServer(req)) {
        return res.json({
          status: "paused",
          paused: true,
          error: "AI generation is paused in settings to save tokens and execution time. You can review existing records or use Google NotebookLM for analysis.",
          analysis: "AI generation is currently paused in Settings."
        });
      }
      const { report, junctions, clothes, apiKey, model: reqModel, dictionaryPrompt } = req.body;
      if (!report) {
        return res.status(400).json({ error: "Wearing report data is required." });
      }

      const { ai, model } = getGeminiClient(apiKey, reqModel);

      // Extract custom fields & non-standard fields on this wearing report
      const standardReportKeys = new Set([
        'id', 'name', 'Name', 'wear date', 'wearDate', 'created', 'duration', 'reportDetail', 'report detail',
        'shoulder feel', 'shoulderFeel', 'mid breast', 'midBreast', 'night mid breast', 'nightMidBreast',
        'sweat', 'sweatProp', 'night sweat', 'nightSweat', 'leg feel', 'legFeel', 'night leg feel', 'nightLegFeel',
        'tags', 'categories', 'Pictures', 'picturesCount', 'customFields', 'extraAttributes', 'fieldExplanations',
        'wornGarmentTitles', 'aiReports', 'AI Wearing Analysis'
      ]);
      const reportCustomObj: Record<string, any> = {
        ...(report.customFields || {}),
        ...(report.extraAttributes || {})
      };
      delete reportCustomObj._fieldExplanations;
      for (const [k, v] of Object.entries(report)) {
        if (!standardReportKeys.has(k) && v !== undefined && v !== null && v !== '') {
          reportCustomObj[k] = v;
        }
      }

      const reportCustomEntries: Array<[string, any]> = [];
      const reportExplainEntries: Array<[string, any]> = [];
      for (const [k, v] of Object.entries(reportCustomObj)) {
        if (k.endsWith('_explain')) {
          reportExplainEntries.push([k, v]);
        } else if (!k.startsWith('_')) {
          reportCustomEntries.push([k, v]);
        }
      }

      let reportCustomText = '';
      if (reportCustomEntries.length > 0) {
        reportCustomText = reportCustomEntries
          .map(([k, v]) => `  - 【实测报告自定义属性/环境: "${k}"】 => 【用户实际填写的真实取值: "${typeof v === 'object' ? JSON.stringify(v) : v}"】 (注: 必须原样引用此字面真实值进行分析，绝对严禁脑补猜测！)`)
          .join('\n');
      }

      let reportExplainText = '';
      if (reportExplainEntries.length > 0) {
        reportExplainText = reportExplainEntries
          .map(([k, v]) => `  - 【维度专属说明: "${k}"】 => 【用户真实说明: "${typeof v === 'object' ? JSON.stringify(v) : v}"】`)
          .join('\n');
      }

      const promptText = `You are a personalized sensory clothing and ergonomics analyst modeled on the user's Teable AI wearing report evaluation system.
Evaluate this wearing report session deeply and honestly based STRICTLY on the recorded session fields, thermal feedback, day vs. night sensations, and linked clothes/bras.
DO NOT use generic fluff. You must evaluate the exact values recorded.

${dictionaryPrompt || ""}

CRITICAL USER TERMINOLOGY RULES (用户真实自然语言用词规则，严禁曲解):
1. "swear" 是用户的真实输入简写，代表 "sweat" (出汗/冒汗)，例如 "more hot and swear" 即指面料闷热导致出汗。
2. "ji xiong" 代表 "拨胸 / 聚胸" (manual scooping/repositioning breast tissue into cups for center gore support)。
3. "zhouwhen" 代表 "褶皱 / 起皱不平整" (wrinkling/creasing affecting cup or dress aesthetics)。
4. "diao dai" 特指 "细肩带款式" (thin shoulder straps that may dig into trapezoid muscles); "not diao dai" 指 "有袖款或宽肩款式" (sleeved or broad straps with distributed weight and no digging)。
5. "side out" 指副乳外溢或侧边包裹不足 (side spillage)。
6. "under fan still ok not so sweat": 用户依靠电风扇通风散热，开风扇时出汗得到控制。

CRITICAL USER EXCLUSION RULE:
Any field, clothing attribute, or record marked "(excluded by user from this AI analysis)" or "(none provided or excluded by user)" was intentionally omitted by the user to avoid confusion, conflicting notes, or bias. You MUST NOT make assumptions, guess, or penalize the evaluation for any excluded field. Simply base your assessment strictly on the data fields that ARE provided.

CRITICAL MANDATORY CUSTOM ATTRIBUTES FIDELITY RULES:
1. 【100% 完整覆盖并剖析所有自定义属性 (NO OMISSION)】:
   - 针对下方【🏷️ 本次穿搭专属自定义属性与环境条件】中列出的每一个自定义属性（例如 Weather 天气、温度、Color、Activity 等），必须在“环境与自定义属性剖析”章节中 100% 逐一单独作为 Bullet Point 输出深度分析！绝对严禁遗漏任何一个自定义属性！
2. 【严格依据用户真实填写的字面值，严禁脑补与猜测 (ABSOLUTE FIDELITY)】:
   - 必须且只能依据用户登记的实际字段字面取值（User's Actual Value）进行分析！
   - 绝对严禁根据单品名称或通用常识擅自脑补猜想！例如：如果用户登记了属性【Color】或【Weather】，其实际取值为用户填写的具体内容，你必须严格使用用户填写的那个字面值进行分析，绝对严禁自作聪明脑补！
   - 必须使用标准格式输出：
     - **属性名（用户登记真实取值：xxx）：** [基于该真实取值的具体深度分析]

CRITICAL OUTPUT LANGUAGE RULE:
You MUST output your ENTIRE analysis in fluent, natural, crystal-clear CHINESE (简体中文).
All headings and explanations MUST be in Chinese.

=== WEARING REPORT SESSION DATA ===
Report Name: ${report['Name'] !== undefined ? (report['Name'] || report.name || '(unnamed)') : '(excluded by user from this AI analysis)'}
Wear Date: ${report['wear date'] !== undefined ? (report['wear date'] || report.wearDate || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Tags: ${report['tags'] !== undefined ? (Array.isArray(report['tags']) ? report['tags'].join(', ') : report['tags']) : '(excluded by user from this AI analysis)'}
Categories: ${report['categories'] !== undefined ? (Array.isArray(report['categories']) ? report['categories'].join(', ') : report['categories']) : '(excluded by user from this AI analysis)'}
Attached Photos: ${report['picturesCount'] !== undefined ? report['picturesCount'] : '(excluded by user from this AI analysis)'}
Shoulder Feel: ${report['shoulder feel'] !== undefined ? (report['shoulder feel'] || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Mid Breast (Daytime): ${report['mid breast'] !== undefined ? (report['mid breast'] || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Night Mid Breast: ${report['night mid breast'] !== undefined ? (report['night mid breast'] || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Sweat (Daytime): ${report['sweat'] !== undefined ? (report['sweat'] || report.sweatProp || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Night Sweat: ${report['night sweat'] !== undefined ? (report['night sweat'] || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Leg Feel (Daytime): ${report['leg feel'] !== undefined ? (report['leg feel'] || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Night Leg Feel: ${report['night leg feel'] !== undefined ? (report['night leg feel'] || '(unrecorded)') : '(excluded by user from this AI analysis)'}
Report Details / Notes: ${report['report detail'] !== undefined ? (report['report detail'] || '(no additional details)') : '(excluded by user from this AI analysis)'}

=== 🏷️ 本次穿搭专属自定义属性与环境条件 (CUSTOM SESSION ATTRIBUTES & ENVIRONMENTAL CONTEXT) ===
${reportCustomText || '(暂无实测报告动态自定义属性)'}

=== 📐 维度专属体感说明 (FIELD EXPLANATIONS) ===
${reportExplainText || '(暂无维度说明)'}

=== LINKED GARMENTS IN THIS SESSION ===
${junctions && junctions.length > 0 ? JSON.stringify(junctions, null, 2) : '(none provided or excluded by user)'}

=== KNOWN GARMENT PROFILES (SPECIFIC ATTRIBUTES SELECTED BY USER) ===
${clothes && clothes.length > 0 ? JSON.stringify(clothes, null, 2) : '(none provided or excluded by user)'}

=== OUTPUT STRUCTURE (请使用下列中文 Markdown 标题输出) ===
## 穿戴概要
- 记录日期、穿戴单品（内衣/外搭）、穿着时长及场合环境。

## 舒适度与人体工学（肩膀与胸部）
- 细致分析肩部受压酸痛、白天与夜间胸部承托与底围体感、腿部温感。

## 炎热与排汗表现
- 对比白天出汗与夜间出汗情况，结合中午气温与通风分析。

## 🏷️ 环境与自定义属性剖析 (Environmental & Custom Attributes)
- 针对本次穿戴登记的全部自定义属性与环境指标逐一深入剖析，严禁遗漏！每一个自定义属性必须按标准格式输出：
  * **属性名（用户登记真实取值：xxx）：** [基于该真实取值的具体深度分析]

## 洗护判断与状态核实
- 针对本次搭配中的每一件单品给出清洗与换洗建议。

## 追踪建议与缺失体感记录
- 指出本次穿着未记录或遗漏的体感字段，指导下次穿搭时重点记录的观察点。`;

      const response = await ai.models.generateContent({
        model,
        contents: [{ role: "user", parts: [{ text: promptText }] }],
        config: {
          maxOutputTokens: 8192,
          temperature: 0.2
        }
      });

      res.json({ analysis: response.text || "No analysis generated.", model });
    } catch (error: any) {
      handleGeminiError(error, res, "Failed to analyze wearing report.");
    }
  });

  // AI Single Clothes-Junction Assessment Generator (Specific Garment Performance in a specific session)
  app.post("/api/ai/analyze-junction", async (req, res) => {
    try {
      if (isAiGenerationPausedServer(req)) {
        return res.json({
          status: "paused",
          paused: true,
          error: "AI generation is paused in settings to save tokens and execution time. You can review existing records or use Google NotebookLM for analysis.",
          analysis: "AI generation is currently paused in Settings."
        });
      }
      const { junction, garment, report, apiKey, model: reqModel, dictionaryPrompt } = req.body;
      if (!junction) {
        return res.status(400).json({ error: "Junction data is required." });
      }

      const { ai, model } = getGeminiClient(apiKey, reqModel);

      const durationHours = junction['duration'] ? (junction['duration'] / 3600).toFixed(1) : '(未知)';
      const washState = junction['wash stated'] || 'need wash';
      const washDate = junction['wash date'] || junction['washed date'] || '(未记录)';
      const wearDate = junction['wear date'] || report?.['wear date'] || '(未记录)';

      const promptText = `You are a personalized clothing pairing and ergonomics diagnostic expert.
Your task is to analyze this SPECIFIC CLOTHES JUNCTION (单品单次穿着关联记录).
This record represents the exact pairing of a single garment within a specific wearing session.
Analyze how THIS garment performed during THIS session based ONLY on the recorded fields.

${dictionaryPrompt || ""}

CRITICAL OUTPUT LANGUAGE RULE:
You MUST output your ENTIRE analysis in fluent, natural, crystal-clear CHINESE (简体中文).
DO NOT use English headers or explanations.

=== JUNCTION DETAILS (关联记录) ===
Junction Record ID: ${junction.id}
Record Index Name: #${junction.name || 1}
Wear Date (穿着日期): ${wearDate}
Wear Duration (穿着时长): ${durationHours} 小时 (${junction['duration'] || 0} 秒)
Wash Status (清洗状态): ${washState}
Recorded Wash Date (清洗日期): ${washDate}

=== ASSOCIATED GARMENT DETAILS (关联单品) ===
Garment Name: ${garment ? (garment['Name'] || garment.name) : (junction['clothes(new)']?.title || 'Garment')}
Garment Type: ${garment ? (garment['Clothes Type'] || garment.clothesType) : junction['Clothes Type (from clothes(new))'] || 'Dress'}
Material: ${garment?.['material'] || '(未记录)'}
Thickness: ${garment?.['thick clothes'] || '(未记录)'}
Hot at noon (中午炎热度): ${garment?.['Hot at noon'] || '(未记录)'}
Strap Style (diao dai): ${garment?.['diao dai'] || '(未记录)'}
Sit Breast Comfort (久坐胸部体感): ${garment?.['sit breast ok'] || '(未记录)'}
Wet/Sweat Behavior: ${garment?.['wet'] || 'dry'}
Lifetime Washes: ${garment?.['Total Washed Count'] || 0} 次

=== SESSION FEEDBACK CONTEXT (本场穿着的体感背景) ===
Report Name: ${report?.['Name'] || 'Wearing Session'}
Shoulder Feel (肩部酸痛/压迫): ${report?.['shoulder feel'] || '(未记录)'}
Mid Breast (白天胸口/底围): ${report?.['mid breast'] || '(未记录)'}
Night Mid Breast (夜间胸口): ${report?.['night mid breast'] || '(未记录)'}
Daytime Sweat (白天出汗): ${report?.['sweat'] || '(未记录)'}
Night Sweat (夜间出汗): ${report?.['night sweat'] || '(未记录)'}
Leg Feel: ${report?.['leg feel'] || '(未记录)'}
Night Leg Feel: ${report?.['night leg feel'] || '(未记录)'}
Session Notes: ${report?.['report detail'] || '(无)'}

=== OUTPUT STRUCTURE (请使用下列中文 Markdown 标题输出) ===
### 1. 本次单品穿着时长与负荷表现
- 评估持续穿着 ${durationHours} 小时对该衣物（材质、厚度）及身体接触部位造成的生理负荷与疲劳度。

### 2. 气温与排汗适应性诊断
- 结合单品本身的中午炎热耐受（Hot at noon）与本场出汗表现（白天出汗/夜间出汗），评估该单品在本场环境下的透气吸湿表现。

### 3. 版型压迫与人体工学分析
- 结合肩带（diao dai）、久坐胸部体感（sit breast ok）与肩部体感（shoulder feel），具体分析本单品是否引起勒痕、酸痛或移位。

### 4. 洗护状态与再穿适宜性判定
- 当前状态：${washState === 'wash' ? '已标记为清洗完成 (Clean)' : '已标记为待洗 (Need wash)'}
- 基于 ${durationHours} 小时出汗与贴肤情况，给出专业的洗护建议：是应当立即深层清洗，还是在干燥通风后可合理再穿。

### 5. 建议与下次配搭改进点
- 针对这件单品的下次穿搭场景与时长给出优化提示。`;

      const response = await ai.models.generateContent({
        model,
        contents: [{ role: "user", parts: [{ text: promptText }] }],
        config: {
          maxOutputTokens: 8192,
          temperature: 0.2
        }
      });

      res.json({ analysis: response.text || "No analysis generated.", model });
    } catch (error: any) {
      handleGeminiError(error, res, "Failed to analyze clothes junction.");
    }
  });

  // AI Wardrobe Macro Diagnostics (Comprehensive 3-Layer Synthesis: Clothes + 20-Field Wearing Reports + Junction Records)
  app.post("/api/ai/analyze-wardrobe", async (req, res) => {
    try {
      if (isAiGenerationPausedServer(req)) {
        return res.json({
          status: "paused",
          paused: true,
          error: "AI generation is paused in settings to save tokens and execution time. You can review existing records or use Google NotebookLM for analysis.",
          advice: "AI generation is currently paused in Settings."
        });
      }
      const {
        clothes,
        clothesSummary,
        wearingReports,
        junctions,
        reportsCount,
        backlogCount,
        apiKey,
        model: reqModel,
        dictionaryPrompt
      } = req.body;
      const { ai, model } = getGeminiClient(apiKey, reqModel);

      // Normalize garments summary & items
      const garmentProfiles = (clothes || []).map((c: any) => ({
        id: c.id,
        name: c.name || c['Name'],
        type: c.clothesType || c['Clothes Type'],
        material: c.material || c['material'] || '(unrecorded)',
        thickClothes: c.thickClothes || c['thick clothes'] || '(unrecorded)',
        hotAtNoon: c.hotAtNoon || c['Hot at noon'] || '(unrecorded)',
        diaoDai: c.diaoDai || c['diao dai'] || '(unrecorded)',
        sitBreastOk: c.sitBreastOk || c['sit breast ok'] || '(unrecorded)',
        sideBreastOk: c.sideBreastOk || c['side breast ok'] || '(unrecorded)',
        price: c.price || c['price'] || 0,
        totalWashedCount: c.totalWashedCount || c['Total Washed Count'] || 0,
        unwashedDurationCount: c.unwashedDurationCount || c['unwashed duration count'] || 0,
        mayThrow: c.mayThrow || c['may throw'] || '(unrecorded)',
        beautiful: c.beautiful || c['beautiful'] || '(unrecorded)'
      }));

      // Normalize all 20 Wearing Report fields
      const formattedReports = (wearingReports || []).map((r: any) => ({
        id: r.id,
        name: r.name || r['Name'],
        wearDate: r.wearDate || r['wear date'],
        shoulderFeel: r.shoulderFeel || r['shoulder feel'] || '',
        midBreast: r.midBreast || r['mid breast'] || '',
        nightMidBreast: r.nightMidBreast || r['night mid breast'] || '',
        sweat: r.sweat || r.sweatProp || r['sweat'] || '',
        nightSweat: r.nightSweat || r['night sweat'] || '',
        legFeel: r.legFeel || r['leg feel'] || '',
        nightLegFeel: r.nightLegFeel || r['night leg feel'] || '',
        reportDetail: r.reportDetail || r['report detail'] || '',
        categories: r.categories || r['categories'] || [],
        tags: r.tags || r['tags'] || [],
        weather: r.weather || r['weather'] || r.customFields?.['weather'] || '',
        temperature: r.temperature || r['temperature'] || r.customFields?.['temperature'] || '',
        overallSatisfaction: r.overallSatisfaction || r['overall satisfaction'] || r.customFields?.['overall satisfaction'] || '',
        activities: r.activities || r['activities'] || r.customFields?.['activities'] || '',
        aiWearingAnalysis: r.aiWearingAnalysis || r['AI Wearing Analysis'] || '',
        picturesCount: Array.isArray(r['Pictures']) ? r['Pictures'].length : (r.picturesCount || 0),
        aiReportsCount: Array.isArray(r.aiReports) ? r.aiReports.length : (r.aiReportsCount || 0),
        created: r.created || r['created'] || ''
      }));

      // Normalize Junction records (durations & wash status)
      const formattedJunctions = (junctions || []).map((j: any) => {
        const durSec = typeof j.duration === 'number' ? j.duration : (j.durationSeconds || 0);
        return {
          id: j.id,
          reportId: typeof j['wearing report id'] === 'object' ? j['wearing report id']?.id : (j.reportId || j['wearing report id']),
          clothesId: typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.id : (j.clothesId || j['clothes(new)']),
          clothesTitle: typeof j['clothes(new)'] === 'object' ? j['clothes(new)']?.title : (j.clothesTitle || ''),
          durationHours: +(durSec / 3600).toFixed(1),
          durationSeconds: durSec,
          washStated: j.washStated || j['wash stated'] || 'need wash',
          wearDate: j.wearDate || j['wear date'] || '',
          washDate: j.washDate || j['wash date'] || j.washedDate || j['washed date'] || ''
        };
      });

      const promptText = `You are the master wardrobe diagnostic auditor (Teable-Style Macro Wardrobe Intelligence Advisor).
Provide a deeply comprehensive, holistic macroscopic diagnostic report by cross-analyzing:
1. 👗 Entire Wardrobe Inventory (${garmentProfiles.length} items): Materials, midday heat tolerance, strap ergonomics, sit breast comfort, prices, wash frequencies.
2. 📝 Historical Wearing Reports (${formattedReports.length} reports): ALL 20 empirical sensory dimensions (shoulder feel, daytime & night bust support, daytime & night sweat, daytime & night leg feel, details, categories, tags, weather, temperature, satisfaction rating, activities, past AI evaluations).
3. ⏱️ Junction Duration & Laundry Records (${formattedJunctions.length} pairings): Actual wear hours, unwashed durations, wash compliance, pairing synergy.

${dictionaryPrompt || ""}

CRITICAL OUTPUT LANGUAGE RULE:
You MUST output your ENTIRE analysis in fluent, natural, crystal-clear CHINESE (简体中文).
DO NOT use English headers. All sections must use precise, professional Chinese Markdown.

=== COLLECTIVE DATA REPOSITORY ===
Total Garments in Wardrobe: ${clothesSummary?.totalGarments || garmentProfiles.length}
Total Wearing Reports Recorded: ${reportsCount || formattedReports.length}
Garments Currently Pending Wash (Backlog): ${backlogCount || 0}
Material Breakdown: ${JSON.stringify(clothesSummary?.materials || {})}
Comfort Issues Identified from Garments: ${JSON.stringify(clothesSummary?.comfortIssues || [])}

GARMENT PROFILES SAMPLE / SUMMARY:
${JSON.stringify(garmentProfiles, null, 2)}

HISTORICAL WEARING REPORTS (ALL 20 DIMENSIONS):
${JSON.stringify(formattedReports, null, 2)}

JUNCTION DURATION & LAUNDRY PAIRINGS:
${JSON.stringify(formattedJunctions, null, 2)}

=== OUTPUT STRUCTURE (请使用下列中文 Markdown 标题与结构完整输出) ===
### 1. 👚 衣橱全景与生命周期周转健康度
- 统计衣橱总规模、核心面料分布（如雪纺、纯棉、蕾丝等透气性占比）。
- 结合历史穿着报告量与洗涤积压件数，评估衣服的使用轮转速度（Rotation Velocity）与洗护及时性（是否经常积压未洗）。

### 2. 🧬 历史穿搭 20 维真实体感大数据画像
- 汇总统计所有 Wearing Reports 中记录的 20 个维度的体感痛点与高光：
  * **肩部承托与疲劳分布**：细肩带 (diao dai) 导致肩部酸痛 (shoulder feel) 的频次与规律。
  * **工位久坐与胸部体感**：白天工作久坐 (mid breast / sit breast ok) 的底围压迫感 vs 夜间放松 (night mid breast) 感受。
  * **气温与排汗模式**：中午高温 (Hot at noon) 下白天出汗 (sweat) 与夜间出汗 (night sweat) 的表现分布。
  * **腿部与下身舒适度**：白天迈步摩擦/腰腹勒痕 (leg feel) vs 夜间亲肤放松 (night leg feel)。
  * **综合满意度与场景适配**：5星极高满意度的穿搭特征，以及哪些活动（办公、长途步行、聚餐）容易踩雷。

### 3. ⏱️ Junction 穿着时长与负荷表现
- 分析 Junction 单次平均穿着时长（如 4小时、6小时、8+小时）。
- 识别被长时间连续超负荷穿着的单品，指出其疲劳累积点与清洁周转周期。
- 核查洗护标记执行情况（'wash' 已洗 vs 'need wash' 待洗的规范度）。

### 4. 🗑️ 优选保留与精简淘汰清单
- 结合买入单价、历史总洗涤次数、单次穿着成本 ($/wear) 以及 20 维满意度评分：
  * **核心主力保留款 (Top Retain)**：性价比极高、高频穿搭且体感满分的优质单品。
  * **建议精简淘汰款 (Weed-out / Throw)**：长期闲置、单次成本奇高或多次引发肩部/久坐不适的踩雷单品。

### 5. 🎯 20 维数据资产完整度与盲区改进建议
- 评估用户在 20 项穿着报告字段中的填写完整度（指出哪些字段常为空白，如夜间体感或活动类型）。
- 给出后续记录与数据资产沉淀的具体行动指引。`;

      const response = await ai.models.generateContent({
        model,
        contents: [{ role: "user", parts: [{ text: promptText }] }],
        config: {
          maxOutputTokens: 8192,
          temperature: 0.2
        }
      });

      res.json({ analysis: response.text || "No analysis generated.", model });
    } catch (error: any) {
      handleGeminiError(error, res, "Failed to analyze wardrobe.");
    }
  });

  // Android Capacitor Project ZIP Download Endpoint
  app.get("/api/android/download-project", async (_req, res) => {
    try {
      const zip = new JSZip();
      const androidDir = path.join(process.cwd(), "android");
      const capacitorConfig = path.join(process.cwd(), "capacitor.config.ts");

      function addDirectoryToZip(dirPath: string, zipFolder: JSZip) {
        if (!fs.existsSync(dirPath)) return;
        const entries = fs.readdirSync(dirPath, { withFileTypes: true });
        for (const entry of entries) {
          // Skip build outputs and caches to keep ZIP download compact and fast
          if (entry.name === ".gradle" || entry.name === "build" || entry.name === ".idea") continue;
          const fullPath = path.join(dirPath, entry.name);
          if (entry.isDirectory()) {
            const nestedFolder = zipFolder.folder(entry.name);
            if (nestedFolder) addDirectoryToZip(fullPath, nestedFolder);
          } else {
            try {
              const fileData = fs.readFileSync(fullPath);
              zipFolder.file(entry.name, fileData);
            } catch {}
          }
        }
      }

      if (fs.existsSync(androidDir)) {
        const androidZipFolder = zip.folder("android");
        if (androidZipFolder) addDirectoryToZip(androidDir, androidZipFolder);
      }

      if (fs.existsSync(capacitorConfig)) {
        zip.file("capacitor.config.ts", fs.readFileSync(capacitorConfig, "utf-8"));
      }

      zip.file(
        "README_MOTO_EDGE_30.md",
        `# Wardrobe & Wear Journal - Android Capacitor Project for Motorola Edge 30

## 方式 1: 直接使用手机安装 (最快推荐)
直接在 Moto Edge 30 手机的 Chrome 浏览器中访问应用网页：
点击右上角菜单 -> "安装应用" (或 "添加到主屏幕")，Android 系统会自动生成原生 WebAPK，无需连接电脑。

## 方式 2: 使用 Android Studio 调试/打包 APK
1. 解压本压缩包
2. 用 Android Studio 打开解压后的 \`android\` 文件夹
3. 手机开启 "开发者选项" 与 "USB 调试"，用数据线连接电脑
4. 在 Android Studio 点击右上角的 "Run" 绿色三角形按钮，应用会自动安装到您的 Moto Edge 30！
或者在终端中运行：
\`\`\`bash
./gradlew assembleDebug
\`\`\`
生成的 APK 位于：\`android/app/build/outputs/apk/debug/app-debug.apk\`
`
      );

      const buffer = await zip.generateAsync({ type: "nodebuffer", compression: "DEFLATE" });
      res.setHeader("Content-Disposition", 'attachment; filename="wardrobe-moto-edge-30-android.zip"');
      res.setHeader("Content-Type", "application/zip");
      res.send(buffer);
    } catch (err: any) {
      console.error("Failed to generate Android ZIP:", err);
      res.status(500).json({ error: "Failed to generate Android project ZIP." });
    }
  });

  // Direct APK download endpoint (if built)
  app.get(["/download/app-debug.apk", "/api/android/download-apk"], (_req, res) => {
    const candidates = [
      path.join(process.cwd(), "app-debug.apk"),
      path.join(process.cwd(), "android/app/build/outputs/apk/debug/app-debug.apk")
    ];
    for (const p of candidates) {
      if (fs.existsSync(p)) {
        res.setHeader("Content-Disposition", 'attachment; filename="WardrobeDiary-MotoEdge30.apk"');
        res.setHeader("Content-Type", "application/vnd.android.package-archive");
        return res.sendFile(p);
      }
    }
    res.status(404).json({
      error: "APK not generated on cloud. Please use the 1-click PWA direct install on your Moto Edge 30 or download the Capacitor project ZIP.",
      pwaUrl: "/"
    });
  });

  // Vite middleware in dev; static serving in prod
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  const server = app.listen(PORT, "0.0.0.0", () => {
    console.log(`Wardrobe App Server running on http://localhost:${PORT}`);
  });

  const shutdown = () => {
    server.close(() => {
      process.exit(0);
    });
  };
  process.on("SIGTERM", shutdown);
  process.on("SIGINT", shutdown);
}

startServer();
