import express from "express";
import path from "path";
import { GoogleGenAI } from "@google/genai";
import { createServer as createViteServer } from "vite";
import dotenv from "dotenv";

dotenv.config();

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json({ limit: "10mb" }));

  // Health check
  app.get("/api/health", (req, res) => {
    res.json({ status: "ok", timestamp: new Date().toISOString() });
  });

  // AI Outfit & Sensory Advisor endpoint (Server-side Gemini)
  app.post("/api/ai/outfit-advice", async (req, res) => {
    try {
      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(500).json({ error: "GEMINI_API_KEY is not configured in server environment." });
      }

      const { prompt, items, temperature, occasion, mood } = req.body;
      const ai = new GoogleGenAI({ apiKey });

      const systemInstruction = `You are a personalized, perceptive fashion and wardrobe sensory consultant. 
The user tracks their wardrobe with great attention to:
1. Physical comfort: fabric breathability (e.g., chiffon vs lace), midday heat tolerance ("Hot at noon"), and drying speed.
2. Fit ergonomics: shoulder fatigue, strap style ("diao dai"), chest/bust fit when sitting vs standing, and side-bust support.
3. Aesthetic value: rating beauty honestly, matching the right occasion, and evaluating whether an item is worth keeping or wearing.
4. Laundry hygiene: tracking unwashed wear time and ensuring garments don't get worn when sweaty without washing.

Provide structured, candid, and encouraging styling and sensory advice. Keep your response concise, elegant, formatted with clear markdown headings and bullet points.`;

      const userMessage = `Selected garments: ${JSON.stringify(items || [])}
Forecast/Weather: ${temperature || "Warm day, hot at noon"}
Occasion: ${occasion || "Daily casual wear"}
Current sensory concerns: ${mood || "Need breathable and non-restrictive fit"}
Specific user question: ${prompt || "How suitable is this outfit, what sensations should I anticipate, and how should I care for it?"}`;

      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: [
          { role: "user", parts: [{ text: `${systemInstruction}\n\n${userMessage}` }] }
        ]
      });

      res.json({ advice: response.text || "No advice generated." });
    } catch (error: any) {
      console.error("AI Advisor error:", error);
      res.status(500).json({ error: error.message || "Failed to generate AI advice." });
    }
  });

  // AI Garment Assessment Generator
  app.post("/api/ai/analyze-garment", async (req, res) => {
    try {
      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(500).json({ error: "GEMINI_API_KEY is not configured." });
      }

      const { garment } = req.body;
      const ai = new GoogleGenAI({ apiKey });

      const promptText = `Analyze this wardrobe garment from a sensory and ergonomics perspective:
Garment Name: ${garment.name}
Type: ${garment.clothesType}
Material: ${garment.material}
Thickness: ${garment.thickClothes}
Heat at Noon: ${garment.hotAtNoon}
Strap/Neck: ${garment.diaoDai}
Bust Fit (Sitting): ${garment.sitBreastOk}
Bust Fit (Side): ${garment.sideBreastOk}
Center Chest: ${garment.centerChest}
Beauty Rating: ${garment.beautiful}
Current Wash Count: ${garment.totalWashedCount}
Price: $${garment.price || 0}

Please provide:
1. ### Overall assessment
2. ### Comfort and climate
3. ### Fit and appearance
4. ### Care and wearing tips

Keep it succinct, honest, and formatted nicely in clean markdown.`;

      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: [{ role: "user", parts: [{ text: promptText }] }]
      });

      res.json({ analysis: response.text || "No analysis generated." });
    } catch (error: any) {
      console.error("AI Garment analysis error:", error);
      res.status(500).json({ error: error.message || "Failed to analyze garment." });
    }
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

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Wardrobe App Server running on http://localhost:${PORT}`);
  });
}

startServer();
