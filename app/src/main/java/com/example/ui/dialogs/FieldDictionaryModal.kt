package com.example.ui.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.GarmentEntity
import com.example.data.model.ReportEntity
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class TypicalValueItem(
    val valueEn: String = "",
    val valueCn: String = "",
    val meaningEn: String = "",
    val meaningCn: String = ""
)

data class DictionaryFieldItem(
    val name: String,
    val category: String, // "clothes", "wearingReport", "junction"
    val labelCn: String = "",
    val descriptionCn: String = "",
    val descriptionEn: String = "",
    val typicalValues: List<TypicalValueItem> = emptyList()
) {
    // Backward compatibility properties
    val description: String get() = descriptionCn.ifBlank { descriptionEn }
    val valueMeanings: List<String> get() = typicalValues.map {
        val cn = if (it.valueCn.isNotBlank()) ": " + it.valueCn else ""
        it.valueEn + cn
    }
    val options: List<String> get() = typicalValues.map { it.valueEn }
}

class FieldDictionaryService(context: Context) {
    private val prefs = context.getSharedPreferences("wardrobe_field_dictionary_prefs", Context.MODE_PRIVATE)

    fun getFields(): List<DictionaryFieldItem> {
        val raw = prefs.getString("fields_v3", null) ?: return DEFAULT_DICTIONARY_FIELDS
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<DictionaryFieldItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tvArr = obj.optJSONArray("typicalValues") ?: JSONArray()
                val tvList = mutableListOf<TypicalValueItem>()
                for (j in 0 until tvArr.length()) {
                    val tvObj = tvArr.getJSONObject(j)
                    tvList.add(
                        TypicalValueItem(
                            valueEn = tvObj.optString("valueEn", ""),
                            valueCn = tvObj.optString("valueCn", ""),
                            meaningEn = tvObj.optString("meaningEn", ""),
                            meaningCn = tvObj.optString("meaningCn", "")
                        )
                    )
                }
                val rawCat = obj.optString("category", "clothes")
                val normCat = when (rawCat.lowercase()) {
                    "wearing_report", "wearingreport", "journal" -> "wearingReport"
                    "junction" -> "junction"
                    else -> "clothes"
                }
                result.add(
                    DictionaryFieldItem(
                        name = obj.optString("name", obj.optString("fieldNameEn")),
                        category = normCat,
                        labelCn = obj.optString("labelCn", obj.optString("fieldNameCn")),
                        descriptionCn = obj.optString("descriptionCn", obj.optString("description")),
                        descriptionEn = obj.optString("descriptionEn", ""),
                        typicalValues = tvList
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
                put("fieldNameEn", item.name)
                put("category", item.category)
                put("labelCn", item.labelCn)
                put("fieldNameCn", item.labelCn)
                put("descriptionCn", item.descriptionCn)
                put("descriptionEn", item.descriptionEn)
                val tvArr = JSONArray()
                item.typicalValues.forEach { tv ->
                    tvArr.put(
                        JSONObject().apply {
                            put("valueEn", tv.valueEn)
                            put("valueCn", tv.valueCn)
                            put("meaningEn", tv.meaningEn)
                            put("meaningCn", tv.meaningCn)
                        }
                    )
                }
                put("typicalValues", tvArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("fields_v3", arr.toString()).apply()
    }

    fun saveDefinition(item: DictionaryFieldItem): List<DictionaryFieldItem> {
        val current = getFields().toMutableList()
        val index = current.indexOfFirst { it.name.equals(item.name, ignoreCase = true) }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        saveFields(current)
        return current
    }

    fun removeDefinition(fieldName: String): List<DictionaryFieldItem> {
        val updated = getFields().filterNot { it.name.equals(fieldName, ignoreCase = true) }
        saveFields(updated)
        return updated
    }

    fun resetDefaults(): List<DictionaryFieldItem> {
        prefs.edit().remove("fields_v3").apply()
        return DEFAULT_DICTIONARY_FIELDS
    }

    fun getFieldPreviousValues(
        fieldName: String,
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity>
    ): List<Pair<String, Int>> {
        val counts = mutableMapOf<String, Int>()
        val targetName = fieldName.trim().lowercase(Locale.ROOT)

        // 1. Scan clothes
        clothes.forEach { g ->
            val v = when (targetName) {
                "hot at noon" -> g.hotAtNoon
                "diao dai" -> g.diaoDai
                "side breast ok" -> g.sideBreastOk
                "stand breast" -> g.standBreast
                "sit breast ok", "sit breast" -> g.sitBreast
                "material" -> g.material
                "thick clothes" -> g.thickClothes
                "wet" -> g.wet
                "wet reason" -> g.wetReason
                "beautiful" -> g.beautiful
                "beatiful reason", "beautiful reason" -> g.beautifulReason
                "descriptions" -> g.descriptions
                "descriptions 2", "descriptions2" -> g.descriptions2
                "long dress" -> g.longDress
                "may throw" -> g.mayThrow
                else -> {
                    try {
                        JSONObject(g.customFields).optString(fieldName, "")
                    } catch (_: Exception) { "" }
                }
            }.trim()
            if (v.isNotBlank()) {
                counts[v] = (counts[v] ?: 0) + 1
            }
        }

        // 2. Scan wearing reports
        reports.forEach { r ->
            val v = when (targetName) {
                "shoulder feel" -> r.shoulderFeel
                "mid breast" -> r.midBreast
                "night mid breast" -> r.nightMidBreast
                "sweat" -> r.sweat
                "night sweat" -> r.nightSweat
                "leg feel" -> r.legFeel
                "night leg feel" -> r.nightLegFeel
                "report detail" -> r.reportDetail
                else -> {
                    try {
                        JSONObject(r.customFields).optString(fieldName, "")
                    } catch (_: Exception) { "" }
                }
            }.trim()
            if (v.isNotBlank()) {
                counts[v] = (counts[v] ?: 0) + 1
            }
        }

        return counts.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    fun syncHistoricalValues(
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity>
    ): Pair<Int, List<DictionaryFieldItem>> {
        var totalAdded = 0
        val currentFields = getFields()
        val updatedFields = currentFields.map { field ->
            val historical = getFieldPreviousValues(field.name, clothes, reports)
            if (historical.isEmpty()) {
                field
            } else {
                val existingMap = field.typicalValues.associateBy { it.valueEn.trim().lowercase(Locale.ROOT) }.toMutableMap()
                var fieldAdded = 0
                historical.forEach { (histVal, count) ->
                    val key = histVal.trim().lowercase(Locale.ROOT)
                    if (!existingMap.containsKey(key)) {
                        existingMap[key] = TypicalValueItem(
                            valueEn = histVal,
                            valueCn = histVal,
                            meaningEn = "Real historical record (${count}x)",
                            meaningCn = "真实穿搭历史记录 (出现 ${count} 次)"
                        )
                        fieldAdded++
                    }
                }
                if (fieldAdded > 0) {
                    totalAdded += fieldAdded
                    field.copy(typicalValues = existingMap.values.toList())
                } else {
                    field
                }
            }
        }
        if (totalAdded > 0) {
            saveFields(updatedFields)
        }
        return totalAdded to updatedFields
    }

    fun buildAiDictionaryPrompt(scope: String = "all"): String {
        val all = getFields()
        val filtered = when (scope.lowercase()) {
            "clothes" -> all.filter { it.category.equals("clothes", ignoreCase = true) }
            "wearing_report", "wearingreport", "journal" -> all.filter {
                it.category.equals("wearingreport", ignoreCase = true) || it.category.equals("wearing_report", ignoreCase = true)
            }
            "junction" -> all.filter { it.category.equals("junction", ignoreCase = true) }
            else -> all
        }
        return buildString {
            appendLine("=== 字段与数据中英字典 (FIELD & DATA SEMANTIC DICTIONARY - SCOPE: ${scope.uppercase()}) ===")
            appendLine("请严格按照以下用户定义的字段含义与枚举值解释来理解数据，切勿自行臆测英文简写或中式英语词组含义：")
            filtered.forEach { f ->
                val catBadge = f.category.uppercase()
                val cnTitle = if (f.labelCn.isNotBlank()) " (${f.labelCn})" else ""
                val desc = f.descriptionCn.ifBlank { f.descriptionEn }
                appendLine("- [${f.name}]$cnTitle [$catBadge]: $desc")
                if (f.typicalValues.isNotEmpty()) {
                    val vals = f.typicalValues.map { tv ->
                        val pair = "${tv.valueEn} -> ${tv.valueCn}"
                        val meaning = tv.meaningCn.ifBlank { tv.meaningEn }
                        if (meaning.isNotBlank()) "$pair ($meaning)" else pair
                    }
                    appendLine("  枚举值对应: ${vals.joinToString(" | ")}")
                }
            }
        }
    }

    companion object {
        val DEFAULT_DICTIONARY_FIELDS = listOf(
    DictionaryFieldItem(
        name = "Hot at noon",
        category = "clothes",
        labelCn = "中午炎热度 / 散热透气",
        descriptionCn = "评估中午高温烈日时（通常>30°C）该衣物的透气、散热及是否容易闷热捂汗。",
        descriptionEn = "Evaluates heat tolerance, breathability, and sun exposure during hot midday peak hours (>30°C).",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "Not hot",
                valueCn = "不热 / 凉爽透气",
                meaningEn = "Cool and airy, suitable for peak noon heat",
                meaningCn = "薄且通风透气，中午在户外或无空调也不容易闷热"
            ),
            TypicalValueItem(
                valueEn = "Middle",
                valueCn = "中等温感 / 适中",
                meaningEn = "Moderate thermal retention, fine for short walks",
                meaningCn = "温度适中，室内空调或短时间户外行走尚可"
            ),
            TypicalValueItem(
                valueEn = "Hot",
                valueCn = "闷热 / 不耐高温",
                meaningEn = "Heavy or low-airflow fabric, sweltering at midday",
                meaningCn = "厚实或不透气面料，中午会出汗发闷，极不推荐高温穿着"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "diao dai",
        category = "clothes",
        labelCn = "吊带 / 细肩带款式与肩部承托",
        descriptionCn = "专指吊带/细肩带款式，重点考察肩带是否勒肩、压迫斜方肌或导致酸痛疲倦。",
        descriptionEn = "Tracks strap width and shoulder trapezoid muscle strain (shoulder fatigue or digging into skin).",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "diao dai",
                valueCn = "细吊带款式",
                meaningEn = "Thin shoulder straps, evaluates digging and pressure on shoulders",
                meaningCn = "细肩带，容易压迫斜方肌，需警惕勒肩酸痛"
            ),
            TypicalValueItem(
                valueEn = "not diao dai",
                valueCn = "非吊带 / 有袖款",
                meaningEn = "Standard sleeves or sleeveless with broad shoulder coverage",
                meaningCn = "普通短袖或宽肩无袖，肩部受力分散，无勒感"
            ),
            TypicalValueItem(
                valueEn = "wide diao dai",
                valueCn = "宽吊带款式",
                meaningEn = "Wider straps distributing weight evenly across shoulders",
                meaningCn = "较宽的肩带，受力较均匀，比细吊带更舒适"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "sit breast ok",
        category = "clothes",
        labelCn = "久坐时胸部与底围舒适度",
        descriptionCn = "考察在工位长时间坐姿办公时，胸口与底围钢圈是否有压迫感、胸型是否起皱或变丑。",
        descriptionEn = "Evaluates chest, underwire, or band comfort while sitting at a desk for extended hours (wrinkles, tightness, pressure).",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "sit breast shape ok",
                valueCn = "坐姿胸型自然舒适",
                meaningEn = "Comfortable with natural support even when seated for hours",
                meaningCn = "久坐办公不勒胸，底围无压迫，胸型自然"
            ),
            TypicalValueItem(
                valueEn = "stand breast ugly, because alot zhouwhen",
                valueCn = "站立/坐着褶皱多影响外观",
                meaningEn = "Creasing/wrinkles visible, loose or improper cups",
                meaningCn = "版型不服帖或褶皱过多，影响胸部形态美观"
            ),
            TypicalValueItem(
                valueEn = "?",
                valueCn = "体感未明确 / 待体验",
                meaningEn = "Observation not yet finalized or requires testing",
                meaningCn = "尚未充分体验，需要下次久坐时专门感受记录"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "side breast ok",
        category = "clothes",
        labelCn = "副乳包裹与侧收支撑",
        descriptionCn = "评估侧边包裹性、是否有副乳溢出或侧边勒肉。",
        descriptionEn = "Assesses side-bust containment and whether fabric cuts into underarm/side tissue.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "side breast shape ok",
                valueCn = "副乳包裹良好舒适",
                meaningEn = "Smooth side profile, no spilling over the cup edges",
                meaningCn = "侧边平整收拢，不挤压副乳"
            ),
            TypicalValueItem(
                valueEn = "side breast out",
                valueCn = "副乳外溢 / 包裹不足",
                meaningEn = "Tissue bulges at armpit area, cup coverage too narrow",
                meaningCn = "罩杯或侧翼过窄，副乳外溢明显"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "center chest",
        category = "clothes",
        labelCn = "胸口中心聚拢与压迫感",
        descriptionCn = "考察前中鸡心部位是否有压迫感，领口是否过紧或镂空走光。",
        descriptionEn = "Tracks center gore pressure and neckline gap/coverage.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "no center chest",
                valueCn = "无中心压迫 / 平整",
                meaningEn = "Zero pressure on the sternum area",
                meaningCn = "鸡心平顺，完全不压迫胸骨"
            ),
            TypicalValueItem(
                valueEn = "center chest",
                valueCn = "聚拢或中心有承托",
                meaningEn = "Central support present, check for sternum marks",
                meaningCn = "前中有聚拢结构或压迫感"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "thick clothes",
        category = "clothes",
        labelCn = "衣物厚度级别",
        descriptionCn = "面料厚薄度与保暖透气等级。",
        descriptionEn = "Fabric density and thermal weight category.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "01 not thick clothes",
                valueCn = "超薄单衣",
                meaningEn = "Ultra-thin lightweight single layer (e.g. chiffon, light cotton)",
                meaningCn = "轻薄透气单层，适合夏日高温"
            ),
            TypicalValueItem(
                valueEn = "02 middle thick clothes",
                valueCn = "适中厚度",
                meaningEn = "Medium thickness, suitable for temperate days",
                meaningCn = "春秋或室内空调厚度"
            ),
            TypicalValueItem(
                valueEn = "03 thick clothes",
                valueCn = "偏厚实 / 较厚",
                meaningEn = "Thick, substantial fabric with thermal retention",
                meaningCn = "厚实面料，保暖防凉，高温天会闷"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "material",
        category = "clothes",
        labelCn = "面料材质",
        descriptionCn = "面料成分，直接影响亲肤软糯感、透气度与吸汗排湿性能（如棉、丝、雪纺、聚酯纤维等）。",
        descriptionEn = "Fabric composition affecting softness, skin breathability, and sweat absorption.",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "Total Washed Count",
        category = "clothes",
        labelCn = "累计洗涤总次数",
        descriptionCn = "历史累计水洗总次数。结合买入价格可计算出每次穿着的真实使用成本（性价比）。",
        descriptionEn = "Total number of washes across all wear history. Used with price to compute true cost-per-wear ($/wear).",
        typicalValues = listOf()
    ),
    DictionaryFieldItem(
        name = "May Throw / Retain",
        category = "clothes",
        labelCn = "去留决策 / 断舍离状态",
        descriptionCn = "明确衣物的去留处置：保留日常穿、特定场景备用候补、或淘汰丢弃断舍离。",
        descriptionEn = "Wardrobe lifecycle retention status: keep in rotation, backup for specific occasions, or discard/donate.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "keep",
                valueCn = "保留日常穿",
                meaningEn = "Active in routine seasonal rotation",
                meaningCn = "主力服饰，各维度表现满意，积极保留"
            ),
            TypicalValueItem(
                valueEn = "may throw",
                valueCn = "考虑淘汰 / 观察期",
                meaningEn = "Probation period due to comfort/aesthetic issues",
                meaningCn = "存在明显短板（如勒胸、滑肩、闷热），若无法改造则淘汰"
            ),
            TypicalValueItem(
                valueEn = "throw",
                valueCn = "淘汰断舍离",
                meaningEn = "Discarded, donated, or removed from wardrobe",
                meaningCn = "彻底移出衣橱，不再穿着"
            )
        )
    ),
    DictionaryFieldItem(
        name = "Beautiful",
        category = "clothes",
        labelCn = "美观度与胸型视觉评分",
        descriptionCn = "穿上后整体造型美观度、胸型聚拢挺拔度及视觉出片效果评价。",
        descriptionEn = "Aesthetic evaluation, bust contouring beauty, and visual appeal.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "Beautiful",
                valueCn = "美观出片 / 胸型挺拔",
                meaningEn = "Flattering fit, visually appealing bust contour",
                meaningCn = "胸型好看聚拢，版型优美，拍照或出门满意"
            ),
            TypicalValueItem(
                valueEn = "Middle",
                valueCn = "中等平淡 / 尚可接受",
                meaningEn = "Average look, acceptable for non-formal occasions",
                meaningCn = "外观中规中矩，无惊艳点，日常可穿但不出彩"
            ),
            TypicalValueItem(
                valueEn = "not so beautiful",
                valueCn = "不够美观 / 胸型欠佳",
                meaningEn = "Unflattering silhouette, flattening or creasing",
                meaningCn = "容易显平坦、起皱或外溢，需特定内搭挽救"
            )
        )
    ),
    DictionaryFieldItem(
        name = "Wet",
        category = "clothes",
        labelCn = "干爽度与吸湿速干表现",
        descriptionCn = "衣物在出汗或潮湿环境下的排湿透气表现，是否易发潮发重。",
        descriptionEn = "Moisture management, breathability, and quick-dry characteristics.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "dry",
                valueCn = "干爽舒适 / 不黏身",
                meaningEn = "Remains dry and non-sticky even during prolonged wear",
                meaningCn = "排汗透气良好或面料有蓄湿储备，体感干爽"
            ),
            TypicalValueItem(
                valueEn = "wet",
                valueCn = "闷湿出汗 / 黏腻发重",
                meaningEn = "Absorbs sweat poorly or clings to body uncomfortably",
                meaningCn = "排汗差，汗液积聚形成湿冷黏腻感"
            )
        )
    ),
    DictionaryFieldItem(
        name = "Long dress",
        category = "clothes",
        labelCn = "裙长裤长与下身活动版型",
        descriptionCn = "下身裁剪长度对行动便利度、坐姿防走光与腿部防晒透气的影响。",
        descriptionEn = "Garment length affecting mobility, seated coverage, and leg breathability.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "long dress",
                valueCn = "过膝长款 / 防走光防晒",
                meaningEn = "Maxi length providing complete coverage and UV protection",
                meaningCn = "长度充裕，防走光与防空调冷风，但需防绊脚"
            ),
            TypicalValueItem(
                valueEn = "short dress",
                valueCn = "短款 / 轻便活泼",
                meaningEn = "Above knee, light and highly mobile",
                meaningCn = "行动极为方便，透气凉爽，但坐姿需注意防走光"
            ),
            TypicalValueItem(
                valueEn = "middle dress",
                valueCn = "中长款 / 齐膝通勤",
                meaningEn = "Knee length, balanced versatility for office and casual",
                meaningCn = "齐膝适中，适合办公通勤各场景"
            )
        )
    ),
    DictionaryFieldItem(
        name = "Last Washed",
        category = "clothes",
        labelCn = "最后洗涤时间 / 洗护周期",
        descriptionCn = "记录该单品最近一次水洗清洁日期，用于评估纤维疲劳硬化程度与待洗周期。",
        descriptionEn = "Date of last laundry cycle, used to track fabric fatigue and hygiene schedule.",
        typicalValues = listOf()
    ),
    DictionaryFieldItem(
        name = "shoulder feel",
        category = "wearingReport",
        labelCn = "肩膀感受 (穿着报告)",
        descriptionCn = "本次穿着后肩膀、颈椎斜方肌的酸痛受压体感记录。",
        descriptionEn = "Real-time fatigue or soreness reported on shoulders and neck after this wearing session.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "comfortable",
                valueCn = "舒适无酸痛",
                meaningEn = "Zero fatigue or strap pressure felt on shoulders",
                meaningCn = "肩膀轻松，无勒痕无酸痛"
            ),
            TypicalValueItem(
                valueEn = "tired",
                valueCn = "肩膀酸累",
                meaningEn = "Shoulder fatigue experienced after several hours",
                meaningCn = "肩部沉重，肩带拉扯感明显"
            ),
            TypicalValueItem(
                valueEn = "painful",
                valueCn = "勒痛 / 压痕明显",
                meaningEn = "Straps cut into the skin or neck strain",
                meaningCn = "肩带有勒痛感，下班后有明显压痕"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "sweat",
        category = "wearingReport",
        labelCn = "白天出汗情况",
        descriptionCn = "白天正常活动或通勤时的出汗量及衣物吸汗排湿情况。",
        descriptionEn = "Daytime perspiration intensity and fabric moisture retention.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "no sweat",
                valueCn = "无汗 / 干爽",
                meaningEn = "Remained dry and fresh throughout the day",
                meaningCn = "整天干爽无异味"
            ),
            TypicalValueItem(
                valueEn = "little sweat",
                valueCn = "轻微出汗",
                meaningEn = "Slight perspiration around underarms/back, dried quickly",
                meaningCn = "微汗，面料较快散发"
            ),
            TypicalValueItem(
                valueEn = "alot sweat",
                valueCn = "大量出汗 / 湿透",
                meaningEn = "Heavy sweating, fabric got wet and sticky",
                meaningCn = "出汗多，衣物受潮，必须尽快换洗"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "night sweat",
        category = "wearingReport",
        labelCn = "夜间出汗情况",
        descriptionCn = "傍晚居家或夜间睡觉时的出汗发热情况。",
        descriptionEn = "Perspiration during evening relaxation or sleeping.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "no sweat",
                valueCn = "干爽舒适",
                meaningEn = "Dry and fresh throughout the evening and sleep",
                meaningCn = "夜间干爽透气，睡眠无出汗"
            ),
            TypicalValueItem(
                valueEn = "sweaty / warm",
                valueCn = "微热出汗",
                meaningEn = "Mild sweating or warmth during sleep",
                meaningCn = "被窝内轻微发热出汗"
            ),
            TypicalValueItem(
                valueEn = "sticky / alot sweat",
                valueCn = "盗汗湿热 / 黏腻",
                meaningEn = "Heavy night perspiration requiring garment change",
                meaningCn = "出汗多贴肉黏身，醒来感觉潮热不适"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "mid breast",
        category = "wearingReport",
        labelCn = "白天胸口体感",
        descriptionCn = "白天活动期间胸口受力、底围钢圈有无压迫感、胸型承托自然度与透气度。",
        descriptionEn = "Bust support, underwire/band comfort, and gore pressure experienced throughout daytime activities.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "comfortable & natural",
                valueCn = "舒适自然 / 承托适中",
                meaningEn = "Natural support with zero digging or breast pressure",
                meaningCn = "全天活动舒适，底围不勒肉，承托平整自如"
            ),
            TypicalValueItem(
                valueEn = "oppressive / tight",
                valueCn = "压迫紧绷 / 勒胸",
                meaningEn = "Band cuts into skin or center gore digs into sternum",
                meaningCn = "底围过紧或钢圈压迫胸骨，久坐有窒息感"
            ),
            TypicalValueItem(
                valueEn = "empty / loose",
                valueCn = "空杯 / 承托不足",
                meaningEn = "Cups gaping or fabric sagging without support",
                meaningCn = "罩杯不贴合容易空杯晃动"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "night mid breast",
        category = "wearingReport",
        labelCn = "夜间胸口体感",
        descriptionCn = "晚上休息、靠坐或平躺睡眠时胸口是否被束缚或宛如无感的彻底放松感。",
        descriptionEn = "Chest comfort and zero-pressure relaxation when lounging or lying down at night.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "zero pressure",
                valueCn = "完全无感 / 彻底放松",
                meaningEn = "Zero constraint on ribcage and chest while relaxing or sleeping",
                meaningCn = "胸廓毫无压迫，呼吸深长顺畅，宛如无穿"
            ),
            TypicalValueItem(
                valueEn = "mildly tight",
                valueCn = "微有束缚",
                meaningEn = "Noticeable tightness around band or straps when lying down",
                meaningCn = "平躺时感觉胸部仍有束缚或边沿勒肉"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "leg feel",
        category = "wearingReport",
        labelCn = "白天腿部与下身舒适度",
        descriptionCn = "白天行走或久坐时的腿部舒展度、大腿内侧摩擦、腰头勒腹感及下身透气性。",
        descriptionEn = "Evaluates leg mobility, thigh friction, waistband ease, and airflow while walking or sitting during daytime.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "comfortable / free",
                valueCn = "舒适自如 / 迈步轻松",
                meaningEn = "Legs move freely, no thigh friction or hemline pulling",
                meaningCn = "行走大步自如，大腿内侧不磨皮，坐下腰臀不勒"
            ),
            TypicalValueItem(
                valueEn = "a bit tight",
                valueCn = "微紧 / 稍有摩擦",
                meaningEn = "Slight friction or waistband tightness during extended walking",
                meaningCn = "腰腹或大腿稍有紧绷，久坐有轻微勒痕"
            ),
            TypicalValueItem(
                valueEn = "tight / restrictive",
                valueCn = "紧绷迈不开步 / 勒腹",
                meaningEn = "Tight fit restricting stride or cutting into waist/hips",
                meaningCn = "版型过紧或无弹力，迈不开腿，坐下压迫胃腹部"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "night leg feel",
        category = "wearingReport",
        labelCn = "夜间腿部与下身放松度",
        descriptionCn = "傍晚居家休息、蜷腿或睡眠时下身腿部的柔软亲肤感与无拘束度。",
        descriptionEn = "Assesses lower-body comfort, relaxation, and lack of constriction when lounging or resting at night.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "relaxed & soft",
                valueCn = "极度放松 / 柔软亲肤",
                meaningEn = "Zero constraint on lower body, soft breathable fabric for evening lounge",
                meaningCn = "下身毫无压力，面料细腻亲肤，极适合居家休息"
            ),
            TypicalValueItem(
                valueEn = "warm & cozy",
                valueCn = "温暖舒适",
                meaningEn = "Pleasantly warm during evening chill without overheating",
                meaningCn = "夜间防凉保暖适中，不闷热"
            ),
            TypicalValueItem(
                valueEn = "stuffy / sticky",
                valueCn = "闷热或粘身",
                meaningEn = "Fabric traps heat or clings uncomfortably to legs at night",
                meaningCn = "排湿透气差，裤腿黏腿或闷热烦躁"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "report detail",
        category = "wearingReport",
        labelCn = "穿着体验日志与随笔",
        descriptionCn = "本次整套搭配的综合上身体验随笔、外界评价反馈、温湿度应对反思以及搭配心得记录。",
        descriptionEn = "Personal styling reflections, compliments received, unexpected weather changes, and outfit diary notes.",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "categories",
        category = "wearingReport",
        labelCn = "穿搭场合与场景分类",
        descriptionCn = "本次穿着适用的具体生活与社交场景（如：职场通勤、休闲逛街、约会聚餐、差旅出行、居家等）。",
        descriptionEn = "Context or dress code of the day (e.g. Office/Work, Casual, Dining/Date, Workout, Travel).",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "Office / Work",
                valueCn = "职场通勤",
                meaningEn = "Professional workplace dress code",
                meaningCn = "严肃或半正式办公、重要商务或客户会议场景"
            ),
            TypicalValueItem(
                valueEn = "Casual / Daily",
                valueCn = "日常休闲",
                meaningEn = "Relaxed weekend or neighborhood activities",
                meaningCn = "周末逛街、居家闲适或户外漫步"
            ),
            TypicalValueItem(
                valueEn = "Date / Dining",
                valueCn = "约会聚餐",
                meaningEn = "Social gatherings, romantic dinner or brunch",
                meaningCn = "聚会拍照、餐饮约会，兼顾美感与自如"
            ),
            TypicalValueItem(
                valueEn = "Travel / Commute",
                valueCn = "差旅出行",
                meaningEn = "Long sitting or walking during transit",
                meaningCn = "高铁飞机长途跋涉，注重抗皱与耐脏"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "tags",
        category = "wearingReport",
        labelCn = "风格与美学体感标签",
        descriptionCn = "本次搭配的风格美学与情绪体感标签（如：显瘦、极简、复古、高级感、松弛感等）。",
        descriptionEn = "Multi-select aesthetic and sensory tags capturing outfit style, silhouette, and emotional impression.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "Slimming",
                valueCn = "显瘦高挑",
                meaningEn = "Elongating visual silhouette with clean lines",
                meaningCn = "视觉拉长身形，遮肉显精神"
            ),
            TypicalValueItem(
                valueEn = "Minimalist",
                valueCn = "极简高级",
                meaningEn = "Clean cuts and restrained neutral tones",
                meaningCn = "素雅克制，低调质感"
            ),
            TypicalValueItem(
                valueEn = "Comfort First",
                valueCn = "舒适优先",
                meaningEn = "Zero pressure sensory comfort all day long",
                meaningCn = "身体第一，全天无感自由"
            ),
            TypicalValueItem(
                valueEn = "Chic & Layered",
                valueCn = "时髦层次",
                meaningEn = "Sophisticated textural or color layering",
                meaningCn = "层次丰富，富有造型感"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "wear date",
        category = "wearingReport",
        labelCn = "穿着日期",
        descriptionCn = "本次整套衣服上身穿着的具体公历日期，关联气温季节与历史穿着周期。",
        descriptionEn = "The calendar date when this outfit was worn, linking to seasonal weather and wear history.",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "Name",
        category = "wearingReport",
        labelCn = "报告名称 / 穿搭主题",
        descriptionCn = "本次穿搭报告的自定义主题标题（例如：周一重要提案装、秋日午后咖啡漫步）。",
        descriptionEn = "Descriptive title or theme for this outfit session (e.g. 2026-09-10 Work Outfit, Autumn Coffee Walk).",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "AI Wearing Analysis",
        category = "wearingReport",
        labelCn = "AI 穿着多维度诊断分析",
        descriptionCn = "由 AI 生成的多维度整套穿搭评测，涵盖配色协调度、温湿度体感适配与洗护优化建议。",
        descriptionEn = "Multi-dimensional grounded AI evaluation of styling coherence, color harmony, thermal fit, and laundry recommendations.",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "Pictures",
        category = "wearingReport",
        labelCn = "穿搭上身照片与视觉记录",
        descriptionCn = "本次穿着的全身穿搭照、局部特写或镜面自拍，记录真实垂坠感与搭配效果。",
        descriptionEn = "Full-body outfit photos or detail shots documenting the visual silhouette, draping, and real-life look.",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "weather",
        category = "wearingReport",
        labelCn = "天气与气候环境",
        descriptionCn = "穿搭当天的室外天气环境（如：晴天艳阳、小雨、多云阴天、阵风等），影响面料抗湿与防风表现。",
        descriptionEn = "Ambient outdoor weather during the wear session (e.g. Sunny, Rain, Cloudy, Overcast, Breeze).",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "Sunny / Clear",
                valueCn = "晴朗明媚",
                meaningEn = "Bright sunlight, UV protection required",
                meaningCn = "晴天少云，需要透气或防晒"
            ),
            TypicalValueItem(
                valueEn = "Cloudy / Overcast",
                valueCn = "多云阴天",
                meaningEn = "Mild indirect sunlight with moderate humidity",
                meaningCn = "光线柔和，气温平稳"
            ),
            TypicalValueItem(
                valueEn = "Rainy / Humid",
                valueCn = "小雨潮湿",
                meaningEn = "Wet conditions requiring water-resistant or quick-dry garments",
                meaningCn = "下雨湿冷，需注意防溅水与速干"
            ),
            TypicalValueItem(
                valueEn = "Breezy / Windy",
                valueCn = "大风降温",
                meaningEn = "Strong air movement necessitating outer windbreaker",
                meaningCn = "风大，需要防风外套或适当添衣"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "temperature",
        category = "wearingReport",
        labelCn = "气温与体感温标",
        descriptionCn = "当天环境温度或冷热体感指标（如：舒适凉爽 22°C、初秋微凉 16°C、炎炎夏日 33°C 等）。",
        descriptionEn = "Recorded temperature range or perceived thermal comfort (e.g. 24°C pleasant, 15°C chilly, 32°C hot).",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "Cool / Chilly (<18°C)",
                valueCn = "凉爽微寒 (18°C以下)",
                meaningEn = "Requires knitwear or jacket layers",
                meaningCn = "需添毛衣或夹克外套"
            ),
            TypicalValueItem(
                valueEn = "Pleasant & Mild (18-25°C)",
                valueCn = "舒适宜人 (18-25°C)",
                meaningEn = "Goldilocks thermal zone, single layer or light shirt",
                meaningCn = "最舒适温度，衬衫或单衣即可"
            ),
            TypicalValueItem(
                valueEn = "Warm to Hot (>26°C)",
                valueCn = "温热闷热 (26°C以上)",
                meaningEn = "Lightweight linen or cotton, maximum breathability",
                meaningCn = "需穿透气纯棉或亚麻，散热排汗"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "overall satisfaction",
        category = "wearingReport",
        labelCn = "综合穿搭满意度",
        descriptionCn = "对整套穿搭在上身颜值、自信气场与生理舒适度维度的综合满意度评星。",
        descriptionEn = "Holistic self-rating of the outfit taking into account aesthetics, confidence, and physical comfort.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "5-Star Loved",
                valueCn = "极其满意 (满分爱穿)",
                meaningEn = "Perfect synergy of comfort and style, will definitely re-wear",
                meaningCn = "极度惊艳且整天毫无束缚，列入高频循环清单"
            ),
            TypicalValueItem(
                valueEn = "4-Star Good",
                valueCn = "良好推荐 (舒适协调)",
                meaningEn = "Very satisfactory outfit with minor room for accessories",
                meaningCn = "整体大方得体，体验很好"
            ),
            TypicalValueItem(
                valueEn = "3-Star Average",
                valueCn = "普通中规中矩",
                meaningEn = "Acceptable but feels ordinary or slightly uncomfortable",
                meaningCn = "中规中矩，无功无过"
            ),
            TypicalValueItem(
                valueEn = "Needs Adjustment",
                valueCn = "有待改进 (踩雷或不合身)",
                meaningEn = "Uncomfortable or mismatched silhouette, should rethink styling",
                meaningCn = "单品间有冲突或某件勒痛磨皮，下次需调整单品搭配"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "activities",
        category = "wearingReport",
        labelCn = "主要活动与行动轨迹",
        descriptionCn = "本次穿着期间的主要身体活动形式（如：久坐办公、长途步行通勤、聚餐约会、健身运动等）。",
        descriptionEn = "Primary physical activities performed during this session (e.g. Desk office work, Walking/Transit, Dining, Workout).",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "Office / Desk Work",
                valueCn = "久坐办公",
                meaningEn = "Extended sitting, checks waistband and back comfort",
                meaningCn = "长时间伏案坐着，检验腰腹舒适度"
            ),
            TypicalValueItem(
                valueEn = "Walking / Commute",
                valueCn = "步行通勤",
                meaningEn = "Active walking testing shoes, stride, and thigh rub",
                meaningCn = "大步快走，检验迈步自由度与裤腿摩擦"
            ),
            TypicalValueItem(
                valueEn = "Dining / Social",
                valueCn = "聚会餐饮",
                meaningEn = "Relaxed dining, social photos, and stomach expansion",
                meaningCn = "饱餐进食，考验腰头延展度与拍照上镜度"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "aiReports",
        category = "wearingReport",
        labelCn = "历史 AI 诊断版本集",
        descriptionCn = "本次报告历次由 AI 生成的造型诊断、改款建议与色彩分析的历史版本集合。",
        descriptionEn = "Multi-version history of grounded AI advisor evaluations and styling diagnostics saved across iterations.",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "created",
        category = "wearingReport",
        labelCn = "记录创建时间",
        descriptionCn = "本条穿着报告在系统数据库中生成的创建时间戳。",
        descriptionEn = "Exact timestamp when this wearing log was created in the wardrobe database.",
        typicalValues = listOf(
        )
    ),
    DictionaryFieldItem(
        name = "wash stated",
        category = "junction",
        labelCn = "洗护标记状态",
        descriptionCn = "标记本次穿着后该件单品是已洗净还是待洗（需洗涤）。",
        descriptionEn = "Indicates whether the specific garment worn in this session was washed or still unwashed.",
        typicalValues = listOf(
            TypicalValueItem(
                valueEn = "wash",
                valueCn = "已清洗 (Clean)",
                meaningEn = "Item was laundered after this wearing session",
                meaningCn = "本场次穿着后已放入洗衣并完成清洗"
            ),
            TypicalValueItem(
                valueEn = "need wash",
                valueCn = "待洗 (Dirty/Unwashed)",
                meaningEn = "Item is dirty or accumulated wear time and must be washed",
                meaningCn = "未清洗，已累积穿着时长或沾汗，进入待洗清单"
            ),
        )
    ),
    DictionaryFieldItem(
        name = "duration",
        category = "junction",
        labelCn = "穿着时长 (秒数)",
        descriptionCn = "本次 session 该单品的实际穿着时间（秒数换算，例如 14400 秒 = 4 小时）。",
        descriptionEn = "Duration in seconds this garment was worn in this session (e.g. 14400s = 4h).",
        typicalValues = listOf(
        )
    )
)

    }
}

/**
 * Expandable Drawer Component for Field Definitions (matching FieldDictionaryDrawer.tsx)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InlineFieldDictionaryCard(
    dictionaryService: FieldDictionaryService,
    modifier: Modifier = Modifier,
    clothes: List<GarmentEntity> = emptyList(),
    reports: List<ReportEntity> = emptyList(),
    fields: List<DictionaryFieldItem> = dictionaryService.getFields(),
    onFieldsUpdated: ((List<DictionaryFieldItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("all") } // "all", "clothes", "wearingReport", "junction"
    var currentFields by remember(fields) { mutableStateOf(fields) }

    var editingField by remember { mutableStateOf<DictionaryFieldItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val filteredFields = remember(currentFields, selectedTab) {
        when (selectedTab) {
            "all" -> currentFields
            "clothes" -> currentFields.filter { it.category.equals("clothes", ignoreCase = true) }
            "wearingReport" -> currentFields.filter {
                it.category.equals("wearingreport", ignoreCase = true) || it.category.equals("wearing_report", ignoreCase = true)
            }
            "junction" -> currentFields.filter { it.category.equals("junction", ignoreCase = true) }
            else -> currentFields
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inline_field_dictionary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Expandable Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📖 字段与数据中英字典 (Field & Data Dictionary)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                            ) {
                                Text(
                                    text = "${currentFields.size} 个配置项",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7E22CE),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "指导 AI 精准理解中英文输入字段（如 Hot at noon、diao dai、sit breast 等）真实体感含义，折叠不占空间",
                            fontSize = 11.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isExpanded) "收起抽屉" else "展开查看与配置",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isExpanded) Color(0xFF7E22CE) else Slate500
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isExpanded) Color(0xFF7E22CE) else Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC).copy(alpha = 0.5f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Controls: Category Tabs on Left + Add/Reset on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Tabs Pill Container
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val tabs = listOf(
                                    "all" to "全部 (${currentFields.size})",
                                    "clothes" to "Clothes 衣物 (${fields.count { it.category.equals("clothes", ignoreCase = true) }})",
                                    "wearingReport" to "WearingReport 穿着 (${fields.count { it.category.equals("wearingreport", ignoreCase = true) || it.category.equals("wearing_report", ignoreCase = true) }})",
                                    "junction" to "Junction 洗护关联 (${fields.count { it.category.equals("junction", ignoreCase = true) }})"
                                )

                                tabs.forEach { (catKey, label) ->
                                    val isSel = selectedTab == catKey
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) Color(0xFF9333EA) else Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedTab = catKey }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else Slate600,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Right: Sync Historical + Add New Field & Reset Default Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    val (addedCount, updatedFields) = dictionaryService.syncHistoricalValues(clothes, reports)
                                    currentFields = updatedFields
                                    onFieldsUpdated?.invoke(updatedFields)
                                    if (addedCount > 0) {
                                        Toast.makeText(context, "已成功将 $addedCount 条真实输入值同步至 AI 字段定义中！", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "所有真实穿搭历史记录已在字典中，无新增条目。", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp).testTag("sync_historical_values_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("同步穿搭输入值", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { isCreatingNew = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp).testTag("add_field_def_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加字段", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    currentFields = dictionaryService.resetDefaults()
                                    onFieldsUpdated?.invoke(currentFields)
                                    Toast.makeText(context, "已重置为默认中英文定义字典", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                                    .testTag("reset_field_def_btn")
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "恢复开箱即用默认字典",
                                    tint = Slate500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Field Definition Cards
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredFields.forEach { item ->
                            FieldCardView(
                                item = item,
                                onEdit = { editingField = item },
                                onDelete = {
                                    currentFields = dictionaryService.removeDefinition(item.name)
                                    onFieldsUpdated?.invoke(currentFields)
                                    Toast.makeText(context, "已删除字段定义: ${item.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-Screen / Modal Editor Dialog matching user screenshot
    if (editingField != null || isCreatingNew) {
        val initialField = editingField ?: DictionaryFieldItem(
            name = "",
            category = if (selectedTab == "all") "clothes" else selectedTab,
            labelCn = "",
            descriptionCn = "",
            descriptionEn = "",
            typicalValues = emptyList()
        )

        FieldDefinitionEditDialog(
            initial = initialField,
            isNew = isCreatingNew,
            onDismiss = {
                editingField = null
                isCreatingNew = false
            },
            onSave = { saved ->
                currentFields = dictionaryService.saveDefinition(saved)
                onFieldsUpdated?.invoke(currentFields)
                editingField = null
                isCreatingNew = false
                Toast.makeText(context, "已保存字段定义: ${saved.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Individual Field Definition Card in Drawer
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldCardView(
    item: DictionaryFieldItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth().testTag("field_def_card_${item.name}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Field English Name + Chinese Label + Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B21A8)
                    )
                    if (item.labelCn.isNotBlank()) {
                        Text(
                            text = item.labelCn,
                            fontSize = 12.sp,
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
                        text = when (item.category.lowercase()) {
                            "clothes" -> "CLOTHES"
                            "wearingreport", "wearing_report" -> "WEARINGREPORT"
                            "junction" -> "JUNCTION"
                            else -> item.category.uppercase()
                        },
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Description (Chinese and English)
            if (item.descriptionCn.isNotBlank()) {
                Text(
                    text = item.descriptionCn,
                    fontSize = 12.sp,
                    color = Slate600,
                    lineHeight = 17.sp
                )
            }
            if (item.descriptionEn.isNotBlank() && item.descriptionEn != item.descriptionCn) {
                Text(
                    text = item.descriptionEn,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Slate500,
                    lineHeight = 15.sp
                )
            }

            // Typical Values Chips
            if (item.typicalValues.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item.typicalValues.forEach { tv ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Text(
                                text = if (tv.valueCn.isNotBlank()) "${tv.valueEn}: ${tv.valueCn}" else tv.valueEn,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100)

            // Bottom Actions: Edit & Delete buttons
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
                        .clickable { onEdit() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "编辑",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDelete() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = Slate400,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "删除",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
            }
        }
    }
}

/**
 * Full-Screen Field Definition Edit Dialog matching Screenshot_20261003-121745.png, 121759.png, 121804.png
 */
@Composable
fun FieldDefinitionEditDialog(
    initial: DictionaryFieldItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (DictionaryFieldItem) -> Unit
) {
    var fieldNameEn by remember { mutableStateOf(initial.name) }
    var fieldNameCn by remember { mutableStateOf(initial.labelCn) }
    var category by remember { mutableStateOf(initial.category) }
    var descriptionCn by remember { mutableStateOf(initial.descriptionCn) }
    var descriptionEn by remember { mutableStateOf(initial.descriptionEn) }
    var typicalValues by remember { mutableStateOf(initial.typicalValues.toMutableList()) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // 1. Top Bar: Back Arrow + Purple Pencil Icon + Title ("编辑字段定义: ...") + Close [X]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate700
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF9333EA),
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = if (isNew) "添加新字段中英文定义" else "编辑字段定义: ${fieldNameEn.ifBlank { "未命名" }}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Slate400
                    )
                }
            }

            HorizontalDivider(color = Slate200.copy(alpha = 0.8f))

            // 2. Scrollable Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC).copy(alpha = 0.6f))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Field 1: 英文原字段名 (FIELDNAME EN) *
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "英文原字段名 (FIELDNAME EN) *",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            OutlinedTextField(
                                value = fieldNameEn,
                                onValueChange = { fieldNameEn = it },
                                placeholder = { Text("e.g. Hot at noon, diao dai", fontSize = 12.sp, color = Slate400) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_fieldname_en"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF9333EA),
                                    unfocusedBorderColor = Slate300
                                )
                            )
                        }

                        // Field 2: 中文对照名称 (FIELDNAME CN) *
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "中文对照名称 (FIELDNAME CN) *",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            OutlinedTextField(
                                value = fieldNameCn,
                                onValueChange = { fieldNameCn = it },
                                placeholder = { Text("e.g. 中午炎热度 / 散热透气", fontSize = 12.sp, color = Slate400) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_fieldname_cn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF9333EA),
                                    unfocusedBorderColor = Slate300
                                )
                            )
                        }

                        // Field 3: 所属模块类别 (CATEGORY)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "所属模块类别 (CATEGORY)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Slate300),
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { categoryDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (category.lowercase()) {
                                                "clothes" -> "Clothes (衣服字段)"
                                                "wearingreport", "wearing_report" -> "WearingReport (穿着报告字段)"
                                                "junction" -> "Junction (穿戴关联/洗护字段)"
                                                else -> category
                                            },
                                            fontSize = 13.sp,
                                            color = Slate800
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Slate600
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Clothes (衣服字段)") },
                                        onClick = {
                                            category = "clothes"
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("WearingReport (穿着报告字段)") },
                                        onClick = {
                                            category = "wearingReport"
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Junction (穿戴关联/洗护字段)") },
                                        onClick = {
                                            category = "junction"
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Field 4: 中文核心定义与人体工学含义 (DESCRIPTION CN)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "中文核心定义与人体工学含义 (DESCRIPTION CN)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            OutlinedTextField(
                                value = descriptionCn,
                                onValueChange = { descriptionCn = it },
                                placeholder = {
                                    Text(
                                        "告诉 AI 这个字段到底代表身体的什么感觉（例如：考察久坐办公时底围是否有压迫感）",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("input_description_cn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF9333EA),
                                    unfocusedBorderColor = Slate300
                                )
                            )
                        }

                        // Field 5: 英文辅助描述 (DESCRIPTION EN)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "英文辅助描述 (DESCRIPTION EN)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            OutlinedTextField(
                                value = descriptionEn,
                                onValueChange = { descriptionEn = it },
                                placeholder = {
                                    Text(
                                        "English description for AI reasoning prompt",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("input_description_en"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF9333EA),
                                    unfocusedBorderColor = Slate300
                                )
                            )
                        }

                        HorizontalDivider(color = Slate200.copy(alpha = 0.8f))

                        // Section 6: 常见英文输入值与真实体感对应 (Typical Values & Meanings)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "常见英文输入值与真实体感对应 (Typical Values & Meanings)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFAF5FF),
                                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            typicalValues = (typicalValues + TypicalValueItem()).toMutableList()
                                        }
                                        .testTag("btn_add_typical_value")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF7E22CE),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "添加输入值",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7E22CE)
                                        )
                                    }
                                }
                            }

                            // Typical values cards list matching Screenshot
                            if (typicalValues.isEmpty()) {
                                Text(
                                    text = "暂无配置具体取值。AI 将按上述核心定义理解此字段。",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    typicalValues.forEachIndexed { index, tv ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Input 1: English Value (e.g. Not hot)
                                                OutlinedTextField(
                                                    value = tv.valueEn,
                                                    onValueChange = { newVal ->
                                                        val updated = typicalValues.toMutableList()
                                                        updated[index] = tv.copy(valueEn = newVal)
                                                        typicalValues = updated
                                                    },
                                                    placeholder = { Text("英文输入值 (e.g. Hot)", fontSize = 12.sp, color = Slate400) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF9333EA),
                                                        unfocusedBorderColor = Slate300,
                                                        focusedContainerColor = Color.White,
                                                        unfocusedContainerColor = Color.White
                                                    )
                                                )

                                                // Input 2: Chinese Value (e.g. 不热 / 凉爽透气)
                                                OutlinedTextField(
                                                    value = tv.valueCn,
                                                    onValueChange = { newVal ->
                                                        val updated = typicalValues.toMutableList()
                                                        updated[index] = tv.copy(valueCn = newVal)
                                                        typicalValues = updated
                                                    },
                                                    placeholder = { Text("中文值 (e.g. 闷热)", fontSize = 12.sp, color = Slate400) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF9333EA),
                                                        unfocusedBorderColor = Slate300,
                                                        focusedContainerColor = Color.White,
                                                        unfocusedContainerColor = Color.White
                                                    )
                                                )

                                                // Input 3: Chinese Meaning (e.g. 薄且通风透气，中午在户外或无空调也不容易闷热)
                                                OutlinedTextField(
                                                    value = tv.meaningCn,
                                                    onValueChange = { newVal ->
                                                        val updated = typicalValues.toMutableList()
                                                        updated[index] = tv.copy(meaningCn = newVal)
                                                        typicalValues = updated
                                                    },
                                                    placeholder = { Text("中文体感含义 (e.g. 中午极热捂汗)", fontSize = 12.sp, color = Slate400) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF9333EA),
                                                        unfocusedBorderColor = Slate300,
                                                        focusedContainerColor = Color.White,
                                                        unfocusedContainerColor = Color.White
                                                    )
                                                )

                                                // Input 4 + Trash Delete Icon Row
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = tv.meaningEn,
                                                        onValueChange = { newVal ->
                                                            val updated = typicalValues.toMutableList()
                                                            updated[index] = tv.copy(meaningEn = newVal)
                                                            typicalValues = updated
                                                        },
                                                        placeholder = { Text("Meaning EN", fontSize = 12.sp, color = Slate400) },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = Color(0xFF9333EA),
                                                            unfocusedBorderColor = Slate300,
                                                            focusedContainerColor = Color.White,
                                                            unfocusedContainerColor = Color.White
                                                        )
                                                    )

                                                    IconButton(
                                                        onClick = {
                                                            val updated = typicalValues.toMutableList()
                                                            updated.removeAt(index)
                                                            typicalValues = updated
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "删除该输入值",
                                                            tint = Slate400,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Actions: 取消 & 保存定义
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Slate300),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("取消", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate700)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    if (fieldNameEn.isNotBlank()) {
                                        onSave(
                                            DictionaryFieldItem(
                                                name = fieldNameEn.trim(),
                                                category = category,
                                                labelCn = fieldNameCn.trim(),
                                                descriptionCn = descriptionCn.trim(),
                                                descriptionEn = descriptionEn.trim(),
                                                typicalValues = typicalValues.filter {
                                                    it.valueEn.isNotBlank() || it.valueCn.isNotBlank()
                                                }
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                modifier = Modifier.height(38.dp).testTag("btn_save_field_def")
                            ) {
                                Text("保存定义", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog wrapper for Field Dictionary
 */
@Composable
fun FieldDictionaryModal(
    onDismiss: () -> Unit,
    onFieldsUpdated: ((List<DictionaryFieldItem>) -> Unit)? = null
) {
    val context = LocalContext.current
    val service = remember { FieldDictionaryService(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate700)
                    }
                    Text(
                        text = "中英双语字段字典配置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                }
            }

            HorizontalDivider(color = Slate200)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                InlineFieldDictionaryCard(
                    dictionaryService = service,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun InlineFieldDictionarySection(
    highlightScope: String = "all",
    defaultExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dictionaryService = remember { FieldDictionaryService(context) }
    InlineFieldDictionaryCard(
        dictionaryService = dictionaryService,
        modifier = modifier
    )
}

