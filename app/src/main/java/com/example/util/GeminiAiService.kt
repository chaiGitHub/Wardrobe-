package com.example.util

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.FieldDictionaryService
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.drilldown.GarmentStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GroundingSource(
    val title: String,
    val uri: String
)

data class GeminiAiResponse(
    val text: String,
    val sources: List<GroundingSource> = emptyList(),
    val isLiveGemini: Boolean = false,
    val statusNote: String? = null
)

object GeminiAiService {

    private const val PREFS_NAME = "wardrobe_gemini_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
    private const val MODEL_ID = "gemini-2.5-flash"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getEffectiveApiKey(context: Context?): String {
        val customKey = context
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_CUSTOM_API_KEY, "")
            ?.trim()
            .orEmpty()
        if (customKey.isNotBlank()) return customKey

        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank() &&
            buildKey != "MY_GEMINI_API_KEY" &&
            buildKey != "YOUR_API_KEY" &&
            buildKey != "null"
        ) {
            return buildKey
        }
        return ""
    }

    fun hasValidApiKey(context: Context?): Boolean {
        return getEffectiveApiKey(context).isNotBlank()
    }

    fun saveCustomApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_API_KEY, apiKey.trim())
            .apply()
    }

    /**
     * Core call to Gemini 2.5 Flash REST API with Google Search Grounding (`{"google_search": {}}`).
     */
    suspend fun callGeminiWithSearchGrounding(
        apiKey: String,
        systemInstruction: String,
        userPrompt: String,
        enableGoogleSearch: Boolean = true
    ): Result<GeminiAiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_ID:generateContent?key=$apiKey"

            val payload = JSONObject().apply {
                if (systemInstruction.isNotBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    })
                }
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                }))
                if (enableGoogleSearch) {
                    put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
                }
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (enableGoogleSearch && (response.code == 400 || response.code == 403 || response.code == 429)) {
                        return@withContext callGeminiWithSearchGrounding(
                            apiKey = apiKey,
                            systemInstruction = systemInstruction,
                            userPrompt = userPrompt,
                            enableGoogleSearch = false
                        )
                    }
                    return@withContext Result.failure(
                        Exception("Gemini API Error (${response.code}): ${responseBody.take(240)}")
                    )
                }

                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Empty response from Gemini API"))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val partText = parts.optJSONObject(i)?.optString("text", "").orEmpty()
                        if (partText.isNotBlank()) {
                            textBuilder.append(partText)
                        }
                    }
                }

                val sources = mutableListOf<GroundingSource>()
                val groundingChunks = firstCandidate
                    .optJSONObject("groundingMetadata")
                    ?.optJSONArray("groundingChunks")
                if (groundingChunks != null) {
                    val seenUris = mutableSetOf<String>()
                    for (i in 0 until groundingChunks.length()) {
                        val web = groundingChunks.optJSONObject(i)?.optJSONObject("web")
                        if (web != null) {
                            val uri = web.optString("uri", "").trim()
                            val title = web.optString("title", uri).trim()
                            if (uri.isNotBlank() && seenUris.add(uri)) {
                                sources.add(GroundingSource(title = title.ifBlank { uri }, uri = uri))
                            }
                        }
                    }
                }

                val finalText = textBuilder.toString().trim()
                if (finalText.isBlank()) {
                    return@withContext Result.failure(Exception("No text content in Gemini response"))
                }

                Result.success(
                    GeminiAiResponse(
                        text = finalText,
                        sources = sources,
                        isLiveGemini = true,
                        statusNote = if (sources.isNotEmpty()) {
                            "Live Gemini 2.5 Flash + Google Search (${sources.size} 网页来源) + 字段语义字典"
                        } else {
                            "Live Gemini 2.5 Flash + 字段语义字典"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Builds the full Macro AI context combining:
     * 1. All 30 Semantic Field Dictionary definitions (Clothes + WearingReport + Junction)
     * 2. Target Clothes (either user-selected clothes or the entire wardrobe / all dresses if none selected)
     * 3. Wearing Reports
     * 4. Junction records
     */
    private fun buildMacroWardrobeContext(
        context: Context,
        allClothes: List<GarmentEntity>,
        selectedClothes: List<GarmentEntity>,
        reports: List<ReportEntity>,
        junctions: List<JunctionEntity>
    ): String {
        val sb = StringBuilder()
        val dictionaryPrompt = FieldDictionaryService(context).buildAiDictionaryPrompt("all")
        sb.appendLine(dictionaryPrompt)
        sb.appendLine()

        val targetClothes = if (selectedClothes.isNotEmpty()) selectedClothes else allClothes
        val scopeLabel = if (selectedClothes.isNotEmpty()) {
            "用户指定候选衣物 (${selectedClothes.size} 件)"
        } else {
            "全衣橱所有连衣裙与单品 (Whole Dress / Full Wardrobe: ${allClothes.size} 件)"
        }

        val statsByGarment = allClothes.associate { g ->
            g.id to DrilldownUtils.calculateGarmentStats(g.id, junctions)
        }
        val needWashCount = targetClothes.count { statsByGarment[it.id]?.needWash == true }
        val cleanCount = targetClothes.size - needWashCount

        sb.appendLine("=== 1. MACRO ANALYSIS SCOPE ($scopeLabel) ===")
        sb.appendLine("候选衣物总数: ${targetClothes.size} | 干净可穿: $cleanCount | 待清洗 (Need Wash): $needWashCount")
        sb.appendLine("历史穿搭报告总数 (Wearing Reports): ${reports.size} | 关联记录总数 (Junctions): ${junctions.size}")
        sb.appendLine()

        sb.appendLine("=== 2. CLOTHES 衣物字段数据 (${targetClothes.size} 件) ===")
        targetClothes.take(65).forEach { g ->
            val st = statsByGarment[g.id]
            val totalWashes = g.wearingFrequency + g.trackedAppWashes
            val cpw = if (g.price > 0 && totalWashes > 0) {
                String.format(Locale.US, "$%.2f", g.price / maxOf(1, totalWashes))
            } else "N/A"
            val washStatus = if (st?.needWash == true) "need wash (${st.unwashedHours}h unwashed)" else "washed/clean"

            sb.appendLine(
                "- [${g.name}] Type=${g.clothesType.ifBlank { "Dress" }} | Material=${g.material.ifBlank { "N/A" }} | " +
                    "Thick=${g.thickClothes.ifBlank { "N/A" }} | Price=$${g.price} (CPW=$cpw, Washes=$totalWashes) | " +
                    "Hot at noon=${g.hotAtNoon.ifBlank { "N/A" }} | diao dai=${g.diaoDai.ifBlank { "N/A" }} | " +
                    "side breast ok=${g.sideBreastOk.ifBlank { "N/A" }} | sit breast=${g.sitBreast.ifBlank { "N/A" }} | " +
                    "stand breast=${g.standBreast.ifBlank { "N/A" }} | Long dress=${g.longDress.ifBlank { "N/A" }} | " +
                    "Beautiful=${g.beautiful.ifBlank { "N/A" }} (${g.beautifulReason.ifBlank { "-" }}) | " +
                    "Wet=${g.wet.ifBlank { "dry" }} (${g.wetReason.ifBlank { "-" }}) | May throw=${g.mayThrow.ifBlank { "keep" }} | " +
                    "WashState=$washStatus | Notes=${g.descriptions.ifBlank { g.lastComment }}"
            )
        }
        sb.appendLine()

        val targetIds = targetClothes.map { it.id }.toSet()
        val relevantJunctions = if (selectedClothes.isNotEmpty()) {
            junctions.filter { it.clothesId in targetIds }
        } else {
            junctions
        }
        val relevantReportIds = relevantJunctions.map { it.wearingReportId }.toSet()
        val targetReports = if (selectedClothes.isNotEmpty() && relevantReportIds.isNotEmpty()) {
            reports.filter { it.id in relevantReportIds }
        } else {
            reports
        }

        sb.appendLine("=== 3. WEARING REPORT 穿搭报告字段数据 (${targetReports.size} 条) ===")
        targetReports.sortedByDescending { it.wearDate.ifBlank { it.created } }.take(40).forEach { r ->
            sb.appendLine(
                "- Report [${r.name.ifBlank { "Untitled" }}] Date=${r.wearDate.ifBlank { r.created }} | " +
                    "shoulderFeel=${r.shoulderFeel.ifBlank { "normal" }} | midBreast=${r.midBreast.ifBlank { "normal" }} | " +
                    "nightMidBreast=${r.nightMidBreast.ifBlank { "normal" }} | sweat=${r.sweat.ifBlank { "none" }} | " +
                    "nightSweat=${r.nightSweat.ifBlank { "none" }} | legFeel=${r.legFeel.ifBlank { "normal" }} | " +
                    "nightLegFeel=${r.nightLegFeel.ifBlank { "normal" }} | tags=${r.tags} | " +
                    "customFields=${r.customFields} | reportDetail=${r.reportDetail.take(120)}"
            )
        }
        sb.appendLine()

        sb.appendLine("=== 4. JUNCTION 衣物×穿搭关联记录 (${relevantJunctions.size} 条) ===")
        relevantJunctions.take(60).forEach { j ->
            val hrs = String.format(Locale.US, "%.1f", j.duration / 3600.0)
            sb.appendLine(
                "- Junction: Garment=[${j.clothesTitle}] × Report=[${j.wearingReportTitle}] | " +
                    "wearDate=${j.wearDate} | duration=${hrs}h | washStated=${j.washStated.ifBlank { "need wash" }} | " +
                    "washDate=${j.washDate.ifBlank { j.washedDate.ifBlank { "unwashed" } }}"
            )
        }

        return sb.toString()
    }

    /**
     * MACRO AI REPORT:
     * Uses Clothes + WearingReport + Junction + All 30 Dictionary Definitions.
     * Supports selecting 1+ candidate clothes OR selecting none (evaluates whole dress / full wardrobe).
     */
    suspend fun askWardrobeAdvisor(
        context: Context,
        query: String,
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity>,
        junctions: List<JunctionEntity>,
        selectedClothes: List<GarmentEntity> = emptyList(),
        fallbackGenerator: () -> String
    ): GeminiAiResponse {
        val apiKey = getEffectiveApiKey(context)
        if (apiKey.isBlank()) {
            return GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地全量字典引擎 (配置 GEMINI_API_KEY 可开启实时联网深度分析)"
            )
        }

        val systemInstruction = """
            你是智能衣橱的「全局穿搭与体感宏观顾问 (Macro AI Advisor)」。
            你同时掌握以下三大数据源与《字段与数据中英字典》：
            1. Clothes (衣物属性字段)
            2. WearingReport (历史穿搭与生理体感报告字段)
            3. Junction (每次穿着时长与清洗状态关联记录)
            
            核心规则：
            - 必须严格遵循《字段与数据中英字典》中对每个英文字段与枚举值（如 Hot at noon, diao dai, side breast ok, sit breast, Wet, shoulderFeel, midBreast, sweat, legFeel, washStated 等）的中文解释，绝不能误解或臆测字段含义。
            - 如果用户勾选了特定候选衣物，则重点对比评估这些候选衣物；如果用户未勾选任何衣物，则自动扫描并评估全衣橱所有连衣裙/单品 (Whole Dress / Full Closet)。
            - 使用清晰的中文 Markdown 输出（包含 ### 标题、要点列表与具体衣物名称引用）。
        """.trimIndent()

        val macroContext = buildMacroWardrobeContext(
            context = context,
            allClothes = clothes,
            selectedClothes = selectedClothes,
            reports = reports,
            junctions = junctions
        )

        val fullPrompt = """
            $macroContext
            
            === 用户本次宏观分析诉求 ===
            $query
            
            请结合上述字典定义、衣物字段、穿搭报告与清洗关联记录，生成一份结构化的中文 Markdown 宏观穿搭与体感分析报告。
        """.trimIndent()

        val result = callGeminiWithSearchGrounding(
            apiKey = apiKey,
            systemInstruction = systemInstruction,
            userPrompt = fullPrompt,
            enableGoogleSearch = true
        )

        return result.getOrElse {
            GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地全量字典引擎 (离线回退)"
            )
        }
    }

    /**
     * CLOTHES AI REPORT (Single Garment Field-Only Report):
     * Strictly uses ONLY the single garment's own Clothes fields + Clothes Semantic Dictionary (8 fields).
     * Does NOT mix in WearingReport or Junction rows so the AI evaluates pure garment field data without confusion.
     */
    suspend fun generateGarmentAuditReport(
        context: Context,
        garment: GarmentEntity,
        stats: GarmentStats? = null,
        garmentJunctions: List<JunctionEntity> = emptyList(),
        reportsById: Map<String, ReportEntity> = emptyMap(),
        allClothes: List<GarmentEntity> = emptyList(),
        versionNumber: Int = 1,
        fallbackGenerator: () -> String
    ): GeminiAiResponse {
        val apiKey = getEffectiveApiKey(context)
        if (apiKey.isBlank()) {
            return GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地单品字段引擎 (仅使用 Clothes 字段 + Clothes 字典定义)"
            )
        }

        val clothesDictionary = FieldDictionaryService(context).buildAiDictionaryPrompt("clothes")
        val totalWashes = garment.wearingFrequency + garment.trackedAppWashes
        val cpw = if (garment.price > 0 && totalWashes > 0) {
            String.format(Locale.US, "$%.2f", garment.price / maxOf(1, totalWashes))
        } else "N/A"

        val systemInstruction = """
            你是「单品字段专属 AI 诊断师 (Clothes Field-Only AI Auditor)」。
            重要边界约束：
            1. 本报告【仅使用该件衣物的 Clothes 自身字段数据】与《Clothes 衣物字段中英字典》进行诊断。
            2. 严禁混入或臆造任何 WearingReport（穿搭报告）或 Junction（关联记录）数据。
            3. 严格依据字典中对 Hot at noon、diao dai、side breast ok、sit breast、stand breast、Long dress、Beautiful、Wet 等字段的定义解释每个字段值的含义。
        """.trimIndent()

        val userPrompt = """
            $clothesDictionary

            === 单品专属字段数据 (CLOTHES FIELDS ONLY - v$versionNumber) ===
            - **衣物名称 (Name)**: ${garment.name}
            - **分类 (clothesType)**: ${garment.clothesType.ifBlank { "未填写" }}
            - **材质 (material)**: ${garment.material.ifBlank { "未填写" }}
            - **厚度 (thickClothes)**: ${garment.thickClothes.ifBlank { "未填写" }}
            - **价格 (price)**: $${garment.price} | **累计洗涤次数**: $totalWashes 次 (单次成本 CPW: $cpw)
            - **中午炎热度 (Hot at noon)**: ${garment.hotAtNoon.ifBlank { "未填写" }}
            - **吊带/肩带款式 (diao dai)**: ${garment.diaoDai.ifBlank { "未填写" }}
            - **副乳包裹与侧收 (side breast ok)**: ${garment.sideBreastOk.ifBlank { "未填写" }}
            - **坐姿胸部舒适度 (sit breast)**: ${garment.sitBreast.ifBlank { "未填写" }}
            - **站姿前胸贴合度 (stand breast)**: ${garment.standBreast.ifBlank { "未填写" }}
            - **裙长/版型 (Long dress)**: ${garment.longDress.ifBlank { "未填写" }}
            - **美观度评价 (Beautiful)**: ${garment.beautiful.ifBlank { "未填写" }} (原因 Beautiful reason: ${garment.beautifulReason.ifBlank { "无" }})
            - **湿度/速干属性 (Wet)**: ${garment.wet.ifBlank { "dry" }} (原因 Wet reason: ${garment.wetReason.ifBlank { "无" }})
            - **断舍离状态 (May throw)**: ${garment.mayThrow.ifBlank { "保留" }}
            - **备注与描述 (Descriptions / Last comment)**: ${garment.descriptions} / ${garment.descriptions2} / ${garment.lastComment}

            请严格仅根据以上 Clothes 字段数据与字典定义，输出结构化中文 Markdown 报告：
            ### 1. 单品核心档案与性价比评估
            ### 2. 版型与人体工学字段解读 (diao dai / side breast ok / sit & stand breast / Long dress)
            ### 3. 材质透气与温湿度表现 (Hot at noon / Wet / Material & Thickness)
            ### 4. 美观度、适用场景与断舍离建议 (Beautiful / May throw)
        """.trimIndent()

        val result = callGeminiWithSearchGrounding(
            apiKey = apiKey,
            systemInstruction = systemInstruction,
            userPrompt = userPrompt,
            enableGoogleSearch = false
        )

        return result.map { resp ->
            resp.copy(statusNote = "Gemini 2.5 Flash · 仅基于 Clothes 字段 + Clothes 字典")
        }.getOrElse {
            GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地单品字段引擎 (仅使用 Clothes 字段 + Clothes 字典)"
            )
        }
    }

    /**
     * WEARING REPORT AI ANALYSIS (WearingReport Field-Only Report):
     * Strictly uses ONLY the WearingReport's own fields + WearingReport Semantic Dictionary (20 fields).
     * Does NOT mix in Clothes inventory or Junction tables.
     */
    suspend fun generateSessionReportAnalysis(
        context: Context,
        report: ReportEntity,
        sessionJunctions: List<JunctionEntity> = emptyList(),
        clothes: List<GarmentEntity> = emptyList(),
        versionNumber: Int = 1,
        fallbackGenerator: () -> String
    ): GeminiAiResponse {
        val apiKey = getEffectiveApiKey(context)
        if (apiKey.isBlank()) {
            return GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地穿搭报告引擎 (仅使用 WearingReport 字段 + 字典定义)"
            )
        }

        val reportDictionary = FieldDictionaryService(context).buildAiDictionaryPrompt("wearing_report")

        val systemInstruction = """
            你是「穿搭报告专属 AI 分析师 (WearingReport Field-Only Analyst)」。
            重要边界约束：
            1. 本分析【仅使用该条穿搭报告自身的 WearingReport 字段数据】与《WearingReport 穿搭报告字段中英字典》。
            2. 严禁混入 Clothes 衣橱表或 Junction 关联表数据，专注于解读本次记录的气温、湿度、日间与夜间体感、出汗、肩胸腿感受、情绪精力与睡眠指标。
            3. 严格按照字典定义区分日间字段（shoulderFeel, midBreast, sweat, legFeel）与夜间字段（nightMidBreast, nightSweat, nightLegFeel）及 customFields 中的扩展指标。
        """.trimIndent()

        val userPrompt = """
            $reportDictionary

            === 穿搭报告专属字段数据 (WEARING REPORT FIELDS ONLY - v$versionNumber) ===
            - **报告名称 (name)**: ${report.name.ifBlank { "未命名穿搭报告" }}
            - **穿着日期 (wearDate)**: ${report.wearDate.ifBlank { report.created }}
            - **肩部感受 (shoulderFeel)**: ${report.shoulderFeel.ifBlank { "未填写" }}
            - **日间胸部感受 (midBreast)**: ${report.midBreast.ifBlank { "未填写" }}
            - **夜间胸部感受 (nightMidBreast)**: ${report.nightMidBreast.ifBlank { "未填写" }}
            - **日间出汗情况 (sweat)**: ${report.sweat.ifBlank { "未填写" }}
            - **夜间盗汗/出汗 (nightSweat)**: ${report.nightSweat.ifBlank { "未填写" }}
            - **日间腿部感受 (legFeel)**: ${report.legFeel.ifBlank { "未填写" }}
            - **夜间腿部感受 (nightLegFeel)**: ${report.nightLegFeel.ifBlank { "未填写" }}
            - **场景与标签 (tags)**: ${report.tags.ifBlank { "无" }}
            - **环境温湿度与扩展生理字段JSON (customFields)**: ${report.customFields.ifBlank { "{}" }}
            - **穿搭详情备注 (reportDetail)**: ${report.reportDetail.ifBlank { "无" }}

            请严格仅基于上述 WearingReport 字段与字典定义，输出结构化中文 Markdown 报告：
            ### 1. 环境温湿度与微气候负荷评估
            ### 2. 日间 vs 夜间局部体感对比分析 (肩部 / 胸部 / 腿部 / 出汗)
            ### 3. 精力、情绪、睡眠与身体酸痛综合解读
            ### 4. 基于本次体感反馈的后续穿衣调整建议
        """.trimIndent()

        val result = callGeminiWithSearchGrounding(
            apiKey = apiKey,
            systemInstruction = systemInstruction,
            userPrompt = userPrompt,
            enableGoogleSearch = false
        )

        return result.map { resp ->
            resp.copy(statusNote = "Gemini 2.5 Flash · 仅基于 WearingReport 字段 + WearingReport 字典")
        }.getOrElse {
            GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地穿搭报告引擎 (仅使用 WearingReport 字段 + 字典定义)"
            )
        }
    }

    /**
     * JUNCTION AI REPORT:
     * Uses Junction fields + Junction Semantic Dictionary (2 fields: duration & washStated) + linked titles.
     */
    suspend fun generateJunctionAnalysis(
        context: Context,
        junction: JunctionEntity,
        garment: GarmentEntity? = null,
        report: ReportEntity? = null,
        fallbackGenerator: () -> String
    ): GeminiAiResponse {
        val apiKey = getEffectiveApiKey(context)
        if (apiKey.isBlank()) {
            return GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地关联记录引擎 (基于 Junction 字段 + Junction 字典)"
            )
        }

        val junctionDictionary = FieldDictionaryService(context).buildAiDictionaryPrompt("junction")
        val hrs = String.format(Locale.US, "%.2f", junction.duration / 3600.0)

        val systemInstruction = """
            你是「衣物×报告关联记录 AI 诊断师 (Junction Field Analyst)」。
            请严格依据《Junction 关联记录字段字典》（特别是 duration 穿着时长秒数折算与 washStated 洗涤状态：need wash / washed）分析该条关联记录的穿着暴露时长与卫生洗涤需求。
        """.trimIndent()

        val userPrompt = """
            $junctionDictionary

            === JUNCTION 关联记录字段数据 ===
            - **关联衣物名称 (clothesTitle)**: ${junction.clothesTitle.ifBlank { garment?.name ?: "未知衣物" }} (${junction.clothesType.ifBlank { garment?.clothesType ?: "衣物" }})
            - **关联穿搭报告 (wearingReportTitle)**: ${junction.wearingReportTitle.ifBlank { report?.name ?: "未知报告" }}
            - **穿着日期 (wearDate)**: ${junction.wearDate.ifBlank { report?.wearDate ?: "未记录" }}
            - **穿着时长 (duration)**: ${junction.duration} 秒 (折合 $hrs 小时)
            - **洗涤状态 (washStated)**: ${junction.washStated.ifBlank { "need wash" }}
            - **计划/记录洗护日期 (washDate / washedDate)**: ${junction.washDate.ifBlank { junction.washedDate.ifBlank { "尚未清洗" } }}

            请输出简洁清晰的中文 Markdown 关联诊断：
            ### 1. 穿着时长负荷解读 (Duration Exposure)
            ### 2. 洗涤状态与卫生评估 (Wash State: ${junction.washStated.ifBlank { "need wash" }})
            ### 3. 洗护操作建议
        """.trimIndent()

        return callGeminiWithSearchGrounding(
            apiKey = apiKey,
            systemInstruction = systemInstruction,
            userPrompt = userPrompt,
            enableGoogleSearch = false
        ).map { resp ->
            resp.copy(statusNote = "Gemini 2.5 Flash · 基于 Junction 字段 + Junction 字典")
        }.getOrElse {
            GeminiAiResponse(
                text = fallbackGenerator(),
                isLiveGemini = false,
                statusNote = "本地关联记录引擎 (基于 Junction 字段 + Junction 字典)"
            )
        }
    }
}
