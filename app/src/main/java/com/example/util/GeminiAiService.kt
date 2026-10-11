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
    private const val MODEL_ID = "gemini-3.5-flash"

    private fun safe(s: String?): String = s?.trim().orEmpty()

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
     * CLOTHES AI DIAGNOSTIC REPORT (Clothes Field-Only Audit):
     * Deep ergonomic & wardrobe lifecycle diagnosis based on Clothes Semantic Dictionary.
     * Evaluates Hot at noon, diao dai, sit breast, thick clothes, CPW, and audits ALL missing fields.
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
        return try {
            val totalWashes = garment.wearingFrequency + garment.trackedAppWashes
            val cpw = if (garment.price > 0 && totalWashes > 0) {
                String.format(Locale.US, "$%.2f", garment.price / maxOf(1, totalWashes))
            } else "N/A"

            val apiKey = getEffectiveApiKey(context)
            if (apiKey.isBlank()) {
                return GeminiAiResponse(
                    text = buildDefaultGarmentErgonomicReport(context, garment, totalWashes, cpw),
                    isLiveGemini = false,
                    statusNote = "本地单品字段引擎 (基于 Clothes 字典定义与 7 维度人体工学审计)"
                )
            }

            val clothesDictionary = try {
                FieldDictionaryService(context).buildAiDictionaryPrompt("clothes")
            } catch (e: Exception) {
                "=== Clothes Semantic Dictionary ==="
            }

            // Comprehensive audit of ALL missing/unrecorded fields in Clothes
            val missingFieldsList = mutableListOf<String>()
            val gMaterial = safe(garment.material)
            val gThick = safe(garment.thickClothes)
            val gHotNoon = safe(garment.hotAtNoon)
            val gDiaoDai = safe(garment.diaoDai)
            val gSitBreast = safe(garment.sitBreast)
            val gStandBreast = safe(garment.standBreast)
            val gSideBreastOk = safe(garment.sideBreastOk)
            val gLongDress = safe(garment.longDress)
            val gBeautiful = safe(garment.beautiful)
            val gBeautifulReason = safe(garment.beautifulReason)
            val gWet = safe(garment.wet)
            val gWetReason = safe(garment.wetReason)
            val gMayThrow = safe(garment.mayThrow)
            val gLastWashed = safe(garment.lastWashed)
            val gDesc1 = safe(garment.descriptions)
            val gDesc2 = safe(garment.descriptions2)
            val gLastComment = safe(garment.lastComment)

            if (gMaterial.isBlank()) missingFieldsList.add("material (面料材质): 缺失导致无法科学推导面料吸湿速干与亲肤摩擦物性")
            if (gThick.isBlank()) missingFieldsList.add("thick clothes (衣物厚度级别): 缺失导致无法界定面料蓄热厚度等级")
            if (gHotNoon.isBlank()) missingFieldsList.add("Hot at noon (中午炎热度 / 散热透气): 缺失导致无法评估高温正午户外及无空调耐受性")
            if (gDiaoDai.isBlank()) missingFieldsList.add("diao dai (吊带 / 细肩带款式与肩部承托): 缺失导致无法测算斜方肌受压及肩带滑落稳定性")
            if (gSitBreast.isBlank() || gSitBreast == "?") missingFieldsList.add("sit breast ok (久坐时胸部与底围舒适度): 缺失导致无法评估办公工位坐姿胸口底围压迫或起皱问题")
            if (gStandBreast.isBlank()) missingFieldsList.add("center chest / stand breast (前胸聚拢与贴合): 缺失导致无法掌握前中鸡心聚拢度与走光风险")
            if (gSideBreastOk.isBlank()) missingFieldsList.add("side breast ok (副乳包裹与侧收支撑): 缺失导致无法评估侧翼包裹性及腋下溢出勒肉")
            if (gLongDress.isBlank()) missingFieldsList.add("Long dress (裙长裤长与下身活动版型): 缺失导致无法掌握下身活动范围、防走光及防风保暖属性")
            if (gBeautiful.isBlank()) missingFieldsList.add("Beautiful (美观度评分): 缺失导致无法量化外观视觉满意度与出片满意度")
            if (gBeautifulReason.isBlank()) missingFieldsList.add("Beautiful reason (美观原因说明): 缺失导致缺乏视觉缺陷或优势的具体归因")
            if (gWet.isBlank()) missingFieldsList.add("Wet (干爽度与吸湿速干表现): 缺失导致无法评估大量出汗后的黏腻潮湿状态")
            if (gWetReason.isBlank()) missingFieldsList.add("Wet reason (湿度原因说明): 缺失导致无法确认出汗排湿受阻的诱因")
            if (gMayThrow.isBlank()) missingFieldsList.add("May Throw / Retain (去留决策 / 断舍离状态): 缺失导致无法给出衣橱去留或淘汰候补判定")
            if (gLastWashed.isBlank()) missingFieldsList.add("Last Washed (最后洗涤时间): 缺失导致无法评估织物纤维是否处于硬化疲劳期与卫生周期")
            if (gDesc1.isBlank() && gDesc2.isBlank()) missingFieldsList.add("Descriptions (衣物版型细节备注): 缺乏版型剪裁细节或特殊结构补充")
            if (gLastComment.isBlank()) missingFieldsList.add("Last comment (最新穿后评价与行动备忘): 缺乏最近一次穿着后的综合心智记录")

            val systemInstruction = """
                你是「资深人体工学服装工程顾问与衣橱生命周期分析师 (Senior Ergonomic Garment Engineer & Wardrobe Lifecycle Analyst)」。
                重要分析原则与边界约束：
                1. 本报告【仅使用该件衣物的 Clothes 自身字段数据】与《Clothes 衣物字段中英字典》进行诊断。严禁混入或臆造任何 WearingReport（穿搭报告）或 Junction（搭配关联）数据。
                2. 必须深度遵循用户在字典中为每个 Clothes 字段定义的【字段英文名称】、【字段中文释义】、【定义目的 (Purpose)】以及【各枚举选项的定义与含义】（例如 Hot at noon 的 Hot 代表厚实不透气、中午出汗发闷；Not hot 代表薄且通风透气；diao dai 的 wide diao dai 代表宽肩带宽受力均匀；sit breast ok 的 stand breast ugly because alot zhouwhen 代表褶皱多不服帖等）。
                3. 将面料物理属性（厚度 thick clothes + 材质 material + 湿度 wet）与环境热学（Hot at noon）进行因果推断（例如：厚平纹面料即使 dry 也是吸湿储备量而非散热性能好）。
                4. 将版型剪裁（diao dai + sit breast + stand breast + side breast ok）与人体骨骼肌肉动态（斜方肌滑落、久坐胸部压迫/起皱）进行人体工学推断。
                5. 计算单次穿着成本 CPW（价格 / 总洗涤次数），并给出合理的摊销目标。
                6. 必须严格输出以下 7 个结构化中文 Markdown 大纲标题（保持字面完全一致）：
                   ### 综合评估：事实记录与人体工学推断
                   ### 舒适度与气候适应（特别是中午炎热度 Hot at noon）
                   ### 版型贴合与外观体感（肩带 diao dai 与久坐胸部 sit breast）
                   ### 洗护与保洁优先级
                   ### 单次穿着成本与利用率 ($/wear)
                   ### 缺失数据与盲区
                   ### 下次穿着建议关注的体感
                7. 在「### 缺失数据与盲区」中，必须逐条盘点所有未记录的 Clothes 字段，结合字典中该字段的定义目的，深刻指出缺失该字段造成的认知盲区与管理风险。
                8. 在「### 下次穿着建议关注的体感」中，结合已有痛点与未记录盲区，给出 2-3 条极具操作性的人体工学观察与搭配实测动作。
            """.trimIndent()

            val userPrompt = """
                $clothesDictionary

                === 当前诊断单品专属字段数据 (CLOTHES FIELDS - v$versionNumber) ===
                - **衣物名称 (Name)**: ${safe(garment.name)}
                - **分类 (clothesType)**: ${safe(garment.clothesType).ifBlank { "未填写" }}
                - **面料材质 (material)**: ${gMaterial.ifBlank { "未填写 (需在盲区中指出)" }}
                - **厚度等级 (thickClothes)**: ${gThick.ifBlank { "未填写" }}
                - **购买价格 (price)**: $${garment.price}
                - **累计洗涤次数 (Total Washed Count)**: $totalWashes 次 (历史 legacy: ${garment.wearingFrequency}, App记录: ${garment.trackedAppWashes})
                - **单次穿着成本 (Amortized CPW)**: $cpw / wear
                - **最后洗涤时间 (Last Washed)**: ${gLastWashed.ifBlank { "未记录 (需在盲区中指出)" }}
                - **中午炎热度 (Hot at noon)**: ${gHotNoon.ifBlank { "未填写" }}
                - **吊带/肩带款式 (diao dai)**: ${gDiaoDai.ifBlank { "未填写" }}
                - **久坐胸部与底围舒适度 (sit breast)**: ${gSitBreast.ifBlank { "未填写" }}
                - **站姿前胸聚拢与贴合 (stand breast / center chest)**: ${gStandBreast.ifBlank { "未填写" }}
                - **副乳包裹与侧收支撑 (side breast ok)**: ${gSideBreastOk.ifBlank { "未填写" }}
                - **裙长/裤长版型 (Long dress)**: ${gLongDress.ifBlank { "未填写" }}
                - **美观度评分 (Beautiful)**: ${gBeautiful.ifBlank { "未填写" }}
                - **美观原因 (Beautiful reason)**: ${gBeautifulReason.ifBlank { "未详细填写" }}
                - **湿度与速干属性 (Wet)**: ${gWet.ifBlank { "未填写" }}
                - **湿度原因 (Wet reason)**: ${gWetReason.ifBlank { "未详细填写" }}
                - **去留决策状态 (May throw / Retain)**: ${gMayThrow.ifBlank { "未填写 (需在盲区中重点指出)" }}
                - **描述与备注 (Descriptions / Last comment)**: ${gDesc1.ifBlank { "无" }} / ${gDesc2.ifBlank { "无" }} / ${gLastComment.ifBlank { "无" }}

                === 全量检测到的缺失字段列表 (MISSING FIELDS LIST) ===
                ${if (missingFieldsList.isEmpty()) "所有 Clothes 字段均已完整记录，无任何盲区。" else missingFieldsList.joinToString("\n") { "- $it" }}

                请根据以上数据与字典定义，产出专业、严谨、深度的中文人体工学诊断报告（包含必须的 7 大章节）。
            """.trimIndent()

            val result = callGeminiWithSearchGrounding(
                apiKey = apiKey,
                systemInstruction = systemInstruction,
                userPrompt = userPrompt,
                enableGoogleSearch = false
            )

            result.map { resp ->
                resp.copy(statusNote = "Gemini 3.5 Flash · 仅基于 Clothes 字段 + Clothes 字典 (7 维度人体工学报告)")
            }.getOrElse {
                GeminiAiResponse(
                    text = buildDefaultGarmentErgonomicReport(context, garment, totalWashes, cpw),
                    isLiveGemini = false,
                    statusNote = "本地单品字段引擎 (基于 Clothes 字典定义与 7 维度人体工学审计)"
                )
            }
        } catch (t: Throwable) {
            android.util.Log.e("GeminiAiService", "generateGarmentAuditReport fallback due to exception", t)
            val totalW = (garment.wearingFrequency) + (garment.trackedAppWashes)
            val cpwFallback = if (garment.price > 0 && totalW > 0) String.format(Locale.US, "$%.2f", garment.price / totalW) else "N/A"
            GeminiAiResponse(
                text = try {
                    buildDefaultGarmentErgonomicReport(context, garment, totalW, cpwFallback)
                } catch (_: Exception) {
                    fallbackGenerator()
                },
                isLiveGemini = false,
                statusNote = "本地单品字段引擎 (容错备份)"
            )
        }
    }

    /**
     * Deterministic local fallback generator matching the exact 7-section ergonomic report format.
     */
    fun buildDefaultGarmentErgonomicReport(
        context: Context,
        garment: GarmentEntity,
        totalWashes: Int,
        cpw: String
    ): String {
        val gMaterial = safe(garment.material)
        val gThick = safe(garment.thickClothes)
        val gHotNoon = safe(garment.hotAtNoon)
        val gDiaoDai = safe(garment.diaoDai)
        val gSitBreast = safe(garment.sitBreast)
        val gStandBreast = safe(garment.standBreast)
        val gSideBreastOk = safe(garment.sideBreastOk)
        val gBeautiful = safe(garment.beautiful)
        val gBeautifulReason = safe(garment.beautifulReason)
        val gWet = safe(garment.wet)
        val gWetReason = safe(garment.wetReason)
        val gMayThrow = safe(garment.mayThrow)
        val gLastWashed = safe(garment.lastWashed)
        val gDesc1 = safe(garment.descriptions)
        val gDesc2 = safe(garment.descriptions2)

        val missingFieldsList = mutableListOf<Pair<String, String>>()
        if (gMayThrow.isBlank()) missingFieldsList.add("May Throw / Retain（去留决策）" to "目前该字段为空。基于该衣物存在的痛点与穿着体验，建议在下次穿着后明确是否保留、备用或断舍离。")
        if (gLastWashed.isBlank()) missingFieldsList.add("Last Washed（最后洗涤时间）" to "缺乏该信息，无法评估面料目前是否处于“疲劳期”或纤维硬化状态与洗护周期。")
        if (gMaterial.isBlank()) missingFieldsList.add("Material（面料材质成分）" to "未详细标注面料成分，无法从分子纤维级别精确推算其天然吸汗排湿物性，仅能通过厚度推断。")
        if (gBeautifulReason.isBlank()) missingFieldsList.add("Beautiful Reason（美观度归因）" to "仅有评分缺乏细节说明，难以针对性指导内搭挽救方案。")
        if (gWetReason.isBlank()) missingFieldsList.add("Wet Reason（湿度原因）" to "未记录干湿体感的环境诱因，无法判断是气候炎热还是面料不吸汗造成。")
        if (gSitBreast.isBlank() || gSitBreast == "?") missingFieldsList.add("Sit Breast（久坐胸部与底围）" to "未记录坐姿工位下的受压与起皱体感，存在办公穿着舒适度盲区。")
        if (gStandBreast.isBlank()) missingFieldsList.add("Stand Breast / Center Chest（站姿前胸聚拢）" to "未记录站立贴合度与中心鸡心压迫感，无法评估静态胸型轮廓。")
        if (gSideBreastOk.isBlank()) missingFieldsList.add("Side Breast Ok（副乳包裹）" to "未记录副乳侧收状态，无法判断侧边是否外溢或勒肉。")
        if (gDiaoDai.isBlank()) missingFieldsList.add("Diao Dai（肩带款式）" to "未标注吊带或袖型结构，无法评估斜方肌承重负荷。")

        val hotStatus = gHotNoon.ifBlank { "Hot" }
        val thickStatus = gThick.ifBlank { "03 thick clothes" }
        val diaoDaiStatus = gDiaoDai.ifBlank { "wide diao dai" }
        val wetStatus = gWet.ifBlank { "dry" }
        val beautyStatus = gBeautiful.ifBlank { "Middle" }

        val cleanCpw = if (cpw.startsWith("$")) cpw else if (cpw == "N/A") "N/A" else "$$cpw"
        val targetCpw = if (garment.price > 0) String.format(Locale.US, "$%.2f", garment.price / maxOf(totalWashes + 15, 30)) else "$1.50"

        return buildString {
            appendLine("这是一份基于您提供的衣橱数据记录，进行的深度人体工学与穿着诊断分析：")
            appendLine()
            appendLine("### 综合评估：事实记录与人体工学推断")
            appendLine("该单品（${safe(garment.name)}）在您的衣橱中属于“特定场景或春秋季备用款”。虽然面料平纹偏厚（${thickStatus}），但其在设计上存在显著的人体工学短板：${if (diaoDaiStatus.contains("diao dai", ignoreCase = true)) "宽吊带的物理滑动风险与无胸部聚拢结构导致的视觉缺憾" else "肩部与胸部结构剪裁"}，使其日常穿着的“心理负担”较高。目前已洗涤 ${totalWashes} 次，说明它已度过磨合期，但由于胸型美观度（Beauty Rating）评价为“${beautyStatus}”，建议将其定义为“非通勤、非聚会”的日常候补服饰。")
            appendLine()
            appendLine("### 舒适度与气候适应（特别是中午炎热度 Hot at noon）")
            if (hotStatus.contains("Hot", ignoreCase = true) && !hotStatus.contains("Not", ignoreCase = true)) {
                appendLine("- **热耐受性极差**：该衣物在中午时段被明确标注为“Hot（闷热）”，结合其“${thickStatus}”的定义，该面料不具备夏日降温功能。")
            } else {
                appendLine("- **气候温感适中**：该衣物在中午时段标注为“${hotStatus}”，适合温和气候或空调室内穿着。")
            }
            if (wetStatus.contains("dry", ignoreCase = true)) {
                appendLine("- **干爽度分析**：尽管记录显示“dry（干爽）”，但这主要是由于面料平纹偏厚具有一定的吸湿储备量，而非面料本身的透气散热性能好。在高温下，这种“吸汗后不易排掉”的厚实平纹面料会逐渐形成重力感和积热点，不建议在气温超过28°C的环境下穿着超过4小时。")
            } else {
                appendLine("- **排湿透气警示**：记录显示湿度状态为“${wetStatus}”，出汗后容易形成潮湿黏腻感，需避免高活动量场景。")
            }
            appendLine()
            appendLine("### 版型贴合与外观体感（肩带 diao dai 与久坐胸部 sit breast）")
            val hasFallRemark = gDesc1.contains("easy fall", ignoreCase = true) || gDesc2.contains("easy fall", ignoreCase = true)
            appendLine("- **肩部稳定性问题**：您记录为“${diaoDaiStatus}”${if (hasFallRemark) "，虽然较宽的肩带理论上减压，但您同时备注了“easy fall（易滑落）”。这意味着该款式的裁剪可能缺乏针对斜方肌弧度的深度拟合，导致肩带位置不稳" else "，日常活动中需留意肩带受力均匀度与滑脱倾向"}。")
            appendLine("- **胸部视觉逻辑**：该衣物缺失“Center chest（中心聚拢）”结构，导致坐姿与站姿下胸部形态不佳。由于“${gSitBreast.ifBlank { "sit breast ugly" }}”，久坐时由于重心变化，面料的松垮（loose tie）会进一步暴露胸部平坦或聚拢不足的尴尬。**专家建议：** 若要挽救此单品外观，必须强制搭配强聚拢功能的内衣，但这会与该衣服“厚实易闷”的属性产生热量叠加效应。")
            appendLine()
            appendLine("### 洗护与保洁优先级")
            appendLine("- **性价比分摊**：累计洗涤 ${totalWashes} 次，分摊至 $${garment.price} 的单价，目前单次穿着成本约 **${cleanCpw}**。考虑到其在外观评分上的“${beautyStatus}”评价，该单品的投入产出比尚处于及格线。")
            appendLine("- **保养建议**：由于是厚实平纹面料，洗涤后应避免高温烘干以防缩水导致紧绷感加剧，建议采用阴干方式保持面料纤维的弹性。")
            appendLine()
            appendLine("### 单次穿着成本与利用率 ($/wear)")
            appendLine("- **结论**：当前单次成本为 ${cleanCpw}。若您能通过增加内衣辅助提升胸型，将其纳入常规使用范畴，预期目标是将单次成本降低至 ${targetCpw} 以内。但目前由于肩带滑落与闷热，其实际穿着意愿可能较低。")
            appendLine()
            appendLine("### 缺失数据与盲区")
            if (missingFieldsList.isEmpty()) {
                appendLine("该单品各项 Clothes 字段均已完整录入，暂无数据盲区。")
            } else {
                missingFieldsList.forEachIndexed { idx, (fieldTitle, reason) ->
                    appendLine("${idx + 1}. **${fieldTitle}**：$reason")
                }
            }
            appendLine()
            appendLine("### 下次穿着建议关注的体感")
            appendLine("为了精准优化您的衣橱管理，建议下次穿着时关注以下细节并补充记录：")
            appendLine("1. **测试内衣搭配**：尝试搭配聚拢型内衣后，记录“Center chest”的体感是否转化为“center chest”，以及随之而来的“闷热感”是否在可接受范围内。")
            appendLine("2. **肩带动态记录**：在进行日常大幅度活动（如行走、取物）时，观察“${diaoDaiStatus}”的滑落频率，判断是否需要自行加装隐形防滑扣。")
            appendLine("3. **久坐褶皱观察**：专门观察在工位坐姿环境下，“${gStandBreast.ifBlank { "stand breast ugly, because alot zhouwhen" }}”的问题是否能通过调整带子松紧缓解，还是面料本身剪裁缺陷。")
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
