package com.example.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import org.json.JSONArray
import org.json.JSONObject

data class DictionaryFieldItem(
    val name: String,
    val category: String, // "clothes", "wearing_report", or "junction"
    val labelCn: String = "",
    val description: String = "",
    val valueMeanings: List<String> = emptyList(),
    val fieldType: String = "Dropdown Select",
    val isStandard: Boolean = true,
    val options: List<String> = emptyList()
)

class FieldDictionaryService(context: Context) {
    private val prefs = context.getSharedPreferences("wardrobe_field_dictionary_prefs", Context.MODE_PRIVATE)

    fun getFields(): List<DictionaryFieldItem> {
        val raw = prefs.getString("fields_v2", null) ?: return DEFAULT_DICTIONARY_FIELDS
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<DictionaryFieldItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.optJSONArray("options") ?: JSONArray()
                val opts = mutableListOf<String>()
                for (j in 0 until optsArr.length()) {
                    opts.add(optsArr.getString(j))
                }
                val meaningsArr = obj.optJSONArray("valueMeanings") ?: JSONArray()
                val meanings = mutableListOf<String>()
                for (j in 0 until meaningsArr.length()) {
                    meanings.add(meaningsArr.getString(j))
                }
                val rawCat = obj.optString("category", "clothes")
                val normCat = if (rawCat == "journal") "wearing_report" else rawCat
                result.add(
                    DictionaryFieldItem(
                        name = obj.optString("name"),
                        category = normCat,
                        labelCn = obj.optString("labelCn", ""),
                        description = obj.optString("description", ""),
                        valueMeanings = if (meanings.isNotEmpty()) meanings else opts,
                        fieldType = obj.optString("fieldType", "Dropdown Select"),
                        isStandard = obj.optBoolean("isStandard", true),
                        options = opts
                    )
                )
            }
            if (result.isEmpty()) DEFAULT_DICTIONARY_FIELDS else result
        } catch (e: Exception) {
            DEFAULT_DICTIONARY_FIELDS
        }
    }

    fun saveFields(fields: List<DictionaryFieldItem>) {
        val arr = JSONArray()
        fields.forEach { item ->
            val obj = JSONObject().apply {
                put("name", item.name)
                put("category", item.category)
                put("labelCn", item.labelCn)
                put("description", item.description)
                put("valueMeanings", JSONArray(item.valueMeanings))
                put("fieldType", item.fieldType)
                put("isStandard", item.isStandard)
                put("options", JSONArray(item.options))
            }
            arr.put(obj)
        }
        prefs.edit().putString("fields_v2", arr.toString()).apply()
    }

    fun resetDefaults(): List<DictionaryFieldItem> {
        prefs.edit().remove("fields_v2").apply()
        return DEFAULT_DICTIONARY_FIELDS
    }

    /**
     * Builds the formatted AI prompt dictionary string for a specific scope:
     * - "clothes": Only Clothes field definitions
     * - "wearing_report": Only WearingReport field definitions
     * - "junction": Only Junction field definitions
     * - "all": All 30 field definitions (used by Macro AI)
     */
    fun buildAiDictionaryPrompt(scope: String = "all"): String {
        val all = getFields()
        val filtered = when (scope.lowercase()) {
            "clothes" -> all.filter { it.category.equals("clothes", ignoreCase = true) }
            "wearing_report", "journal" -> all.filter { it.category.equals("wearing_report", ignoreCase = true) }
            "junction" -> all.filter { it.category.equals("junction", ignoreCase = true) }
            else -> all
        }
        return buildString {
            appendLine("=== 字段与数据中英字典 (FIELD & DATA SEMANTIC DICTIONARY - SCOPE: ${scope.uppercase()}) ===")
            appendLine("请严格按照以下用户定义的字段含义与枚举值解释来理解数据，切勿自行臆测英文简写或中式英语词组含义：")
            filtered.forEach { f ->
                val catBadge = f.category.uppercase()
                val cnTitle = if (f.labelCn.isNotBlank()) " (${f.labelCn})" else ""
                appendLine("- [${f.name}]$cnTitle [$catBadge]: ${f.description}")
                if (f.valueMeanings.isNotEmpty()) {
                    appendLine("  枚举值含义: ${f.valueMeanings.joinToString(" | ")}")
                }
            }
        }
    }

    companion object {
        // 30 Semantic Field Definitions matching Screenshots 14, 15, 16, 18, 19:
        // Clothes (8) + WearingReport (20) + Junction (2) = 30 items
        val DEFAULT_DICTIONARY_FIELDS = listOf(
            // ==========================================
            // 1. CLOTHES 衣物 (8 items)
            // ==========================================
            DictionaryFieldItem(
                name = "Hot at noon",
                category = "clothes",
                labelCn = "中午炎热度 / 散热透气",
                description = "评估中午高温烈日时（通常>30°C）该衣物的透气、散热及是否容易闷热捂汗。",
                valueMeanings = listOf(
                    "Not hot: 不热 / 凉爽透气",
                    "Middle: 中等温感 / 适中",
                    "Hot: 闷热 / 不耐高温"
                ),
                options = listOf("Not hot", "Middle", "Hot", "little hot", "very hot", "cool")
            ),
            DictionaryFieldItem(
                name = "diao dai",
                category = "clothes",
                labelCn = "吊带 / 细肩带款式与肩部承托",
                description = "专指吊带/细肩带款式，重点考察肩带是否勒肩、压迫斜方肌或导致酸痛疲倦。",
                valueMeanings = listOf(
                    "diao dai: 细吊带款式",
                    "not diao dai: 非吊带 / 常规肩袖",
                    "wide diao dai: 宽吊带设计"
                ),
                options = listOf("diao dai", "not diao dai", "wide diao dai", "wide strap")
            ),
            DictionaryFieldItem(
                name = "side breast ok",
                category = "clothes",
                labelCn = "副乳包裹与侧收支撑",
                description = "评估侧边包裹性、是否有副乳溢出或侧边勒肉。",
                valueMeanings = listOf(
                    "side breast shape ok: 副乳包裹良好无溢出",
                    "side breast out: 副乳易外溢或侧边包覆不足"
                ),
                options = listOf("side breast shape ok", "side breast out", "side breast ok", "little tight")
            ),
            DictionaryFieldItem(
                name = "center chest",
                category = "clothes",
                labelCn = "胸口中心聚拢与压迫感",
                description = "考察前中鸡心部位是否有压迫感，领口是否过紧或镂空走光。",
                valueMeanings = listOf(
                    "no center chest: 无中心压迫感 / 舒适平顺",
                    "center chest: 聚拢或前中贴合明显"
                ),
                options = listOf("no center chest", "center chest", "stand breast ok", "comfortable")
            ),
            DictionaryFieldItem(
                name = "thick clothes",
                category = "clothes",
                labelCn = "衣物厚度级别",
                description = "面料厚薄度与保暖透气等级。",
                valueMeanings = listOf(
                    "01 not thick clothes: 轻薄透气款",
                    "02 middle thick clothes: 中等常规厚度",
                    "03 thick clothes: 偏厚实面料"
                ),
                options = listOf("01 not thick clothes", "02 middle thick clothes", "03 thick clothes", "01 thin", "02 normal")
            ),
            DictionaryFieldItem(
                name = "material",
                category = "clothes",
                labelCn = "面料材质",
                description = "面料成分，直接影响亲肤软糯感、透气度与吸汗排湿性能（如棉、丝、雪纺、聚酯纤维等）。",
                valueMeanings = listOf(
                    "Chiffon: 雪纺轻盈透气",
                    "lace: 蕾丝精致修饰",
                    "pure cotton: 纯棉吸汗亲肤",
                    "plain thick: 平纹厚实面料"
                ),
                options = listOf("Chiffon", "lace", "pure cotton", "not pure cotton", "plain thick", "middle chiffon", "silk", "linen", "knit")
            ),
            DictionaryFieldItem(
                name = "Total Washed Count",
                category = "clothes",
                labelCn = "累计洗涤次数与耐穿折旧",
                description = "记录单件衣物历史总洗涤次数（含初始频次与App内追踪洗涤），用于计算单次洗涤成本($/w)与评估寿命。",
                valueMeanings = listOf(
                    "100+ washes: 高频核心主力衣物",
                    "10-99 washes: 常规轮换单品",
                    "<10 washes: 低频或新购入单品"
                ),
                options = listOf("High frequency", "Medium frequency", "New / Low frequency")
            ),
            DictionaryFieldItem(
                name = "sit breast",
                category = "clothes",
                labelCn = "久坐胸部体感与胸型保持",
                description = "评估长时间办公或久坐状态下，胸部下围与杯面是否压迫、勒痕或变形。",
                valueMeanings = listOf(
                    "sit breast shape ok: 久坐胸型自然且无压迫",
                    "sit breast ok: 久坐体感舒适",
                    "little tight: 久坐微紧",
                    "tight: 久坐压迫感明显"
                ),
                options = listOf("sit breast shape ok", "sit breast ok", "little tight", "tight", "comfortable", "no bra ok")
            ),

            // ==========================================
            // 2. WEARING REPORT 穿着 (20 items)
            // ==========================================
            DictionaryFieldItem(
                name = "shoulder feel",
                category = "wearing_report",
                labelCn = "日间肩部与斜方肌体感",
                description = "记录当次穿着过程中肩带或袖笼对肩膀、斜方肌的压迫、摩擦或轻松程度。",
                valueMeanings = listOf(
                    "relaxed and comfortable: 肩部放松无负担",
                    "no diao dai make shoulder no pain: 非吊带设计减轻肩部酸痛",
                    "sore shoulder: 肩部酸胀疲惫"
                ),
                options = listOf("relaxed and comfortable", "comfortable", "little tired", "sore shoulder", "strap slipping")
            ),
            DictionaryFieldItem(
                name = "mid breast",
                category = "wearing_report",
                labelCn = "日间胸部中段支撑与舒适度",
                description = "记录日间活动与久坐时胸口及内衣/连衣裙胸围处的贴合、支撑与呼吸顺畅度。",
                valueMeanings = listOf(
                    "good support, no digging: 支撑良好不勒肉",
                    "mid breast is ok size: 胸围尺寸合身舒适",
                    "little tight: 胸口略微紧绷"
                ),
                options = listOf("good support, no digging", "comfortable", "little tight", "oppressive", "breathable")
            ),
            DictionaryFieldItem(
                name = "night mid breast",
                category = "wearing_report",
                labelCn = "夜间胸部体感与放松度",
                description = "记录傍晚至夜间长时间穿着后，胸部是否有勒痕累积、压迫或依然保持轻松。",
                valueMeanings = listOf(
                    "comfortable for evening: 晚间依然舒适无压迫",
                    "relaxed fit: 夜间贴合自然"
                ),
                options = listOf("comfortable for evening", "no pressure at night", "relaxed fit")
            ),
            DictionaryFieldItem(
                name = "sweat",
                category = "wearing_report",
                labelCn = "日间出汗量与排湿表现",
                description = "记录白天气温与活动下的出汗情况，以及衣物是否粘腻或快速排汗。",
                valueMeanings = listOf(
                    "low / dry: 干爽少汗",
                    "sweaty at noon: 正午出汗明显",
                    "breathable and cool: 透气凉爽"
                ),
                options = listOf("low / dry", "no sweat", "little sweat", "sweaty at noon", "breathable and cool")
            ),
            DictionaryFieldItem(
                name = "night sweat",
                category = "wearing_report",
                labelCn = "夜间出汗与温控表现",
                description = "记录夜间或空调环境下的皮肤干爽度与微气候冷暖平衡。",
                valueMeanings = listOf(
                    "dry and pleasant: 夜间干爽宜人",
                    "cool in aircon: 空调房内微凉舒适"
                ),
                options = listOf("dry and pleasant", "dry", "cool", "warm")
            ),
            DictionaryFieldItem(
                name = "leg feel",
                category = "wearing_report",
                labelCn = "日间腿部活动与裙摆体感",
                description = "记录行走、上下楼梯或久坐时裙摆/裤装对腿部的束缚感、通风度及防寒表现。",
                valueMeanings = listOf(
                    "free movement: 步态自如无束缚",
                    "no so cold leg: 裙长与厚度适中腿部不冷"
                ),
                options = listOf("free movement", "soft drape on legs", "airy and comfortable")
            ),
            DictionaryFieldItem(
                name = "night leg feel",
                category = "wearing_report",
                labelCn = "夜间腿部舒适与保暖度",
                description = "记录夜晚降温或室内冷气下腿部的温度感受与轻盈度。",
                valueMeanings = listOf(
                    "airy and easy: 轻盈自在",
                    "warm enough for night: 夜间保暖适度"
                ),
                options = listOf("airy and easy", "warm enough for night")
            ),
            DictionaryFieldItem(
                name = "report detail",
                category = "wearing_report",
                labelCn = "综合穿着笔记与搭配复盘",
                description = "记录当日天气、出行场景、内搭组合（如搭配哪件bra）及整体满意度的核心叙事日志。",
                valueMeanings = listOf(
                    "[[garment]]: 关联的具体单品组合表现",
                    "weather & activity: 当日气候与活动场景记录"
                ),
                options = listOf("Comfortable indoor office and walk session", "Worn during sunny afternoon commute")
            ),
            DictionaryFieldItem(
                name = "wear date",
                category = "wearing_report",
                labelCn = "实际穿着日期",
                description = "记录该套穿搭实际发生的日历日期，用于季节趋势与高频穿搭周期分析。",
                valueMeanings = listOf("YYYY-MM-DD: 标准穿着日期记录"),
                options = emptyList()
            ),
            DictionaryFieldItem(
                name = "name (report title)",
                category = "wearing_report",
                labelCn = "穿着报告标题 (含自动时间戳)",
                description = "单次穿着记录的标识名称，支持自动生成日期时间（如 2026-09-30 1023 wearing report）。",
                valueMeanings = listOf("Auto Date/Time: 自动时间戳命名"),
                options = emptyList()
            ),
            DictionaryFieldItem(
                name = "weather",
                category = "wearing_report",
                labelCn = "当日天气与微气候环境",
                description = "穿着时的外部气象与室内冷暖气状态（烈日、微风、阴雨、空调房等）。",
                valueMeanings = listOf("Sunny & Hot: 晴热高温", "Mild & Breezy: 温和微风", "AC Indoors: 室内空调环境"),
                options = listOf("Sunny & Hot", "Mild & Breezy", "Cloudy", "Rainy", "AC Indoors")
            ),
            DictionaryFieldItem(
                name = "occasion",
                category = "wearing_report",
                labelCn = "穿着场景与活动类型",
                description = "当次穿搭的主要生活或工作场景（办公久坐、外出步行、聚餐拍照、居家休闲）。",
                valueMeanings = listOf("Office / Sitting: 办公久坐", "Walking / Errands: 步行外出", "Home Lounge: 居家休闲"),
                options = listOf("Office / Sitting", "Casual Outing", "Dining / Photo", "Walking / Errands", "Home Lounge")
            ),
            DictionaryFieldItem(
                name = "temperature",
                category = "wearing_report",
                labelCn = "环境温度感受 (°C)",
                description = "穿着时的环境温度区间，直接影响面料闷热度与出汗评估。",
                valueMeanings = listOf(">30°C: 盛夏高温", "24-28°C: 温和舒适", "<22°C: 凉爽或冷气充足"),
                options = listOf(">30°C Hot", "25-29°C Warm", "20-24°C Mild")
            ),
            DictionaryFieldItem(
                name = "humidity",
                category = "wearing_report",
                labelCn = "空气湿度与粘腻感",
                description = "环境湿度对衣物排汗速度与贴肤感的影响。",
                valueMeanings = listOf("High humidity: 潮湿易粘肤", "Dry / AC: 干爽透气"),
                options = listOf("Dry / AC", "Normal", "Humid")
            ),
            DictionaryFieldItem(
                name = "bra pairing",
                category = "wearing_report",
                labelCn = "内衣与外裙/上衣配伍度",
                description = "评估特定内衣（如 purple old bra、small bra、hugo bra）与外穿连衣裙共同穿着时的胸型与舒适协同效应。",
                valueMeanings = listOf("Good match: 胸型自然且不勒", "Mismatch: 肩带外露或胸口压迫"),
                options = listOf("Good match", "Shoulder strap conflict", "Slightly tight together")
            ),
            DictionaryFieldItem(
                name = "walk feel",
                category = "wearing_report",
                labelCn = "步行动态稳定性",
                description = "走动时裙摆是否贴腿、肩带是否滑落、内衣是否移位。",
                valueMeanings = listOf("Stable & airy: 步态轻盈稳定", "Rides up / slips: 易移位或滑肩"),
                options = listOf("Stable & airy", "Easy stride", "Slight strap slip")
            ),
            DictionaryFieldItem(
                name = "sitting comfort",
                category = "wearing_report",
                labelCn = "连续久坐综合舒适度",
                description = "连续久坐 2-6 小时后腰腹、胸部下围及大腿后侧的压迫与折痕感。",
                valueMeanings = listOf("No waist/bust pinch: 久坐无勒感", "Tight when seated: 久坐微勒"),
                options = listOf("No waist/bust pinch", "Comfortable", "Tight when seated")
            ),
            DictionaryFieldItem(
                name = "skin friction",
                category = "wearing_report",
                labelCn = "蕾丝/接缝皮肤摩擦感",
                description = "评估蕾丝边缘、内衬接缝或吊带扣环对敏感肌肤是否有刺痒或红印。",
                valueMeanings = listOf("Soft & smooth: 亲肤无刺痒", "Slight itch: 局部微摩擦"),
                options = listOf("Soft & smooth", "No friction", "Slight seam friction")
            ),
            DictionaryFieldItem(
                name = "overall mood",
                category = "wearing_report",
                labelCn = "穿搭愉悦度与自信感",
                description = "当日穿搭带来的整体心情、美观自信度与精力状态。",
                valueMeanings = listOf("Confident & happy: 美观自信且舒适", "Tired / distracted: 因体感不适分心"),
                options = listOf("Confident & happy", "Relaxed", "Normal")
            ),
            DictionaryFieldItem(
                name = "outfit photos",
                category = "wearing_report",
                labelCn = "当日穿搭实拍记录 (OOTD)",
                description = "关联至当次穿着报告的视觉实拍照片，用于复盘版型与垂坠效果。",
                valueMeanings = listOf("Attached photo: 含实拍照片记录"),
                options = emptyList()
            ),

            // ==========================================
            // 3. JUNCTION 洗护关联 (2 items)
            // ==========================================
            DictionaryFieldItem(
                name = "wash stated",
                category = "junction",
                labelCn = "单品洗护状态 (Clean / need wash)",
                description = "记录单件衣物在当次穿着后的洗护状态，决定是否进入待洗篮及累计未洗时长。",
                valueMeanings = listOf(
                    "Clean: 已清洗洁净 / 可随时穿着",
                    "need wash: 待清洗 / 处于脏衣篮中"
                ),
                options = listOf("Clean", "need wash")
            ),
            DictionaryFieldItem(
                name = "duration & wash date",
                category = "junction",
                labelCn = "单次穿着时长与清洗日期",
                description = "精确记录每件衣物在当次报告中的穿着时长（小时/分钟）及完成洗涤的日期（wash date）。",
                valueMeanings = listOf(
                    "duration (hrs): 当次实际穿着小时数",
                    "wash date: 完成洗涤并归位洁净状态的日期"
                ),
                options = emptyList()
            )
        )
    }
}

/**
 * Inline Collapsible Field & Data Dictionary Card matching Screenshots 14, 15, 16, 18, 19.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineFieldDictionarySection(
    initialCategory: String = "all",
    highlightScope: String = initialCategory,
    defaultExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val service = remember { FieldDictionaryService(context) }
    var fields by remember { mutableStateOf(service.getFields()) }
    InlineFieldDictionaryCard(
        dictionaryService = service,
        fields = fields,
        onFieldsUpdated = { fields = it },
        initialCategory = highlightScope,
        defaultExpanded = defaultExpanded,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineFieldDictionaryCard(
    dictionaryService: FieldDictionaryService,
    fields: List<DictionaryFieldItem>,
    onFieldsUpdated: (List<DictionaryFieldItem>) -> Unit,
    initialCategory: String = "all",
    defaultExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    var selectedTab by remember(initialCategory) { mutableStateOf(initialCategory) } // "all", "clothes", "wearing_report", "junction"

    var editingItem by remember { mutableStateOf<DictionaryFieldItem?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val clothesCount = remember(fields) { fields.count { it.category.equals("clothes", ignoreCase = true) } }
    val wearingCount = remember(fields) { fields.count { it.category.equals("wearing_report", ignoreCase = true) } }
    val junctionCount = remember(fields) { fields.count { it.category.equals("junction", ignoreCase = true) } }

    val displayedFields = remember(fields, selectedTab) {
        when (selectedTab) {
            "clothes" -> fields.filter { it.category.equals("clothes", ignoreCase = true) }
            "wearing_report" -> fields.filter { it.category.equals("wearing_report", ignoreCase = true) }
            "junction" -> fields.filter { it.category.equals("junction", ignoreCase = true) }
            else -> fields
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inline_field_dictionary_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Collapsible trigger)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📖 字段与数据中英字典 (Field & Data Dictionary)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            lineHeight = 19.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                        ) {
                            Text(
                                text = "${fields.size} 个配置项",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "指导 AI 精准理解中英文输入字段（如 Hot at noon...",
                            fontSize = 12.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Slate400,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Slate100)

                    // 4-Scope Segmented Filter Bar (全部 (30) | Clothes 衣物 (8) | WearingReport 穿着 (20) | Junction 洗护关联 (2))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf(
                                Triple("all", "全\n部", "(${fields.size})"),
                                Triple("clothes", "Clothes\n衣物", "($clothesCount)"),
                                Triple("wearing_report", "WearingReport\n穿着 ($wearingCount)", ""),
                                Triple("junction", "Junction\n洗护关\n联 ($junctionCount)", "")
                            )
                            tabs.forEach { (key, title, sub) ->
                                val isSel = selectedTab == key
                                val weightVal = when (key) {
                                    "all" -> 0.7f
                                    "clothes" -> 1f
                                    "wearing_report" -> 1.35f
                                    else -> 1f
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) Color(0xFF9333EA) else Color.Transparent,
                                    modifier = Modifier
                                        .weight(weightVal)
                                        .clickable { selectedTab = key }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (sub.isNotEmpty()) "$title\n$sub" else title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else Slate700,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right-aligned Action Row: [+ 添加新字段定义] and [↻] Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isAddingNew = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "添加新字段定义",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    val reset = dictionaryService.resetDefaults()
                                    onFieldsUpdated(reset)
                                    Toast.makeText(context, "已恢复默认 30 项中英文字典配置", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Dictionary",
                                    tint = Slate600,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Field Definition Cards
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        displayedFields.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Top Row: English Key + Category Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontSize = 15.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF581C87)
                                            )
                                            if (item.labelCn.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.labelCn,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate800
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, Slate200)
                                        ) {
                                            Text(
                                                text = when (item.category) {
                                                    "clothes" -> "CLOTHES"
                                                    "wearing_report" -> "WEARINGREPORT"
                                                    "junction" -> "JUNCTION"
                                                    else -> item.category.uppercase()
                                                },
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate600,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Semantic Description
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            fontSize = 13.sp,
                                            color = Slate600,
                                            lineHeight = 19.sp
                                        )
                                    }

                                    // Value Meaning Chips
                                    if (item.valueMeanings.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            item.valueMeanings.forEach { vm ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, Slate200)
                                                ) {
                                                    Text(
                                                        text = vm,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Slate700,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Slate100)

                                    // Bottom Right Edit & Delete Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { editingItem = item }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "编辑",
                                                tint = Slate500,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "编辑",
                                                fontSize = 12.sp,
                                                color = Slate600,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                val updated = fields.filterNot {
                                                    it.name == item.name && it.category == item.category
                                                }
                                                dictionaryService.saveFields(updated)
                                                onFieldsUpdated(updated)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "删除",
                                                tint = Slate400,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Field Dialog
    if (isAddingNew || editingItem != null) {
        val initial = editingItem
        var fieldName by remember(initial) { mutableStateOf(initial?.name ?: "") }
        var fieldCat by remember(initial) {
            mutableStateOf(initial?.category ?: if (selectedTab == "all") "clothes" else selectedTab)
        }
        var labelCn by remember(initial) { mutableStateOf(initial?.labelCn ?: "") }
        var desc by remember(initial) { mutableStateOf(initial?.description ?: "") }
        var meaningsText by remember(initial) {
            mutableStateOf(initial?.valueMeanings?.joinToString("\n") ?: "")
        }

        AlertDialog(
            onDismissRequest = {
                isAddingNew = false
                editingItem = null
            },
            title = {
                Text(
                    text = if (initial != null) "编辑字段定义: ${initial.name}" else "添加新字段定义",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "clothes" to "Clothes 衣物",
                            "wearing_report" to "WearingReport",
                            "junction" to "Junction"
                        ).forEach { (catKey, catLabel) ->
                            val sel = fieldCat == catKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) Color(0xFF9333EA) else Slate100,
                                modifier = Modifier.clickable { fieldCat = catKey }
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fieldName,
                        onValueChange = { fieldName = it },
                        label = { Text("字段英文名 (如 Hot at noon)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = labelCn,
                        onValueChange = { labelCn = it },
                        label = { Text("中文标题 (如 中午炎热度 / 散热透气)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("AI 语义说明 (指导 AI 如何理解该字段)", fontSize = 12.sp) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = meaningsText,
                        onValueChange = { meaningsText = it },
                        label = { Text("选项及含义 (每行一个，如 Not hot: 不热)", fontSize = 12.sp) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fieldName.isNotBlank()) {
                            val parsedMeanings = meaningsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                            val parsedOptions = parsedMeanings.map { it.substringBefore(":").trim() }.filter { it.isNotBlank() }
                            val newItem = DictionaryFieldItem(
                                name = fieldName.trim(),
                                category = fieldCat,
                                labelCn = labelCn.trim(),
                                description = desc.trim(),
                                valueMeanings = parsedMeanings,
                                options = parsedOptions,
                                isStandard = initial?.isStandard ?: false
                            )
                            val updated = if (initial != null) {
                                fields.map {
                                    if (it.name == initial.name && it.category == initial.category) newItem else it
                                }
                            } else {
                                listOf(newItem) + fields
                            }
                            dictionaryService.saveFields(updated)
                            onFieldsUpdated(updated)
                            isAddingNew = false
                            editingItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("保存定义")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAddingNew = false
                        editingItem = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun FieldDictionaryModal(
    onDismiss: () -> Unit,
    onFieldsUpdated: ((List<DictionaryFieldItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    val service = remember { FieldDictionaryService(context) }
    var fields by remember { mutableStateOf(service.getFields()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("field_dictionary_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandPurpleLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "字段与数据中英字典 (${fields.size})",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Clothes · WearingReport · Junction",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Slate400
                            )
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        InlineFieldDictionaryCard(
                            dictionaryService = service,
                            fields = fields,
                            onFieldsUpdated = {
                                fields = it
                                onFieldsUpdated?.invoke(it)
                            }
                        )
                    }
                }

                HorizontalDivider(color = Slate200)

                Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
