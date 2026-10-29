package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GarmentEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.DictionaryFieldItem
import com.example.ui.dialogs.FieldDictionaryService
import com.example.ui.dialogs.PreviousEntriesModal
import com.example.ui.dialogs.PreviousEntry
import com.example.ui.dialogs.TypicalValueItem
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import org.json.JSONObject

data class FieldHistoryEntry(
    val text: String,
    val garmentName: String,
    val count: Int,
    val sources: List<String>,
    val clothesSource: String = "",
    val reportSource: String = ""
)

/**
 * Extracts all past ergonomic explanations across clothes and reports for a specific field key,
 * linking each wearing report to the actual clothes worn via junctions.
 */
fun getHistoricalExplanations(
    fieldKey: String,
    clothes: List<GarmentEntity> = emptyList(),
    reports: List<ReportEntity> = emptyList(),
    allJunctions: List<com.example.data.model.JunctionEntity> = emptyList()
): List<FieldHistoryEntry> {
    val cleanKey = fieldKey.trim()
    val candidateKeys = setOf(
        cleanKey,
        cleanKey.lowercase(),
        cleanKey.replace(" ", ""),
        cleanKey.replace(" ", "_"),
        cleanKey.replace("-", ""),
        cleanKey.replace(":", "")
    )

    // Build report -> garments lookup from junctions
    val reportToGarmentsMap = mutableMapOf<String, MutableList<String>>()
    val clothesById = clothes.associateBy { it.id }
    allJunctions.forEach { j ->
        val g = clothesById[j.clothesId]
        val desc = if (g != null) {
            "${g.name}${if (g.clothesType.isNotBlank()) " (${g.clothesType})" else ""}"
        } else {
            j.clothesTitle.trim()
        }
        if (desc.isNotBlank()) {
            if (j.wearingReportId.isNotBlank()) {
                val list = reportToGarmentsMap.getOrPut(j.wearingReportId) { mutableListOf() }
                if (!list.contains(desc)) list.add(desc)
            }
            if (j.wearingReportTitle.isNotBlank()) {
                val list = reportToGarmentsMap.getOrPut(j.wearingReportTitle.trim().lowercase()) { mutableListOf() }
                if (!list.contains(desc)) list.add(desc)
            }
        }
    }

    data class AggregateInfo(
        var count: Int,
        val sources: MutableList<String>,
        var clothesSource: String,
        var reportSource: String
    )
    val map = mutableMapOf<String, AggregateInfo>()

    clothes.forEach { g ->
        var note = ""
        try {
            if (g.customFields.isNotBlank() && g.customFields != "{}") {
                val obj = JSONObject(g.customFields)
                val sub = obj.optJSONObject("_fieldExplanations")
                if (sub != null) {
                    for (k in candidateKeys) {
                        val candidate = sub.optString(k, "").trim()
                        if (candidate.isNotBlank() && candidate != "No data" && candidate != "N/A") {
                            note = candidate
                            break
                        }
                    }
                }
                if (note.isBlank()) {
                    for (k in candidateKeys) {
                        val candidate = obj.optString("${k}_explain", "").trim()
                        if (candidate.isNotBlank() && candidate != "No data" && candidate != "N/A") {
                            note = candidate
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (note.isNotBlank() && note != "No data" && note != "N/A") {
            val gName = "${g.name}${if (g.clothesType.isNotBlank()) " (${g.clothesType})" else ""}".ifBlank { "Garment" }
            val existing = map[note]
            val src = "👕 衣服: $gName"
            if (existing != null) {
                if (!existing.sources.contains(src)) {
                    existing.sources.add(src)
                }
                if (existing.clothesSource.isBlank()) {
                    existing.clothesSource = gName
                }
                existing.count++
            } else {
                map[note] = AggregateInfo(
                    count = 1,
                    sources = mutableListOf(src),
                    clothesSource = gName,
                    reportSource = ""
                )
            }
        }
    }

    reports.forEach { r ->
        var note = ""
        try {
            if (r.customFields.isNotBlank() && r.customFields != "{}") {
                val obj = JSONObject(r.customFields)
                val sub = obj.optJSONObject("_fieldExplanations")
                if (sub != null) {
                    for (k in candidateKeys) {
                        val candidate = sub.optString(k, "").trim()
                        if (candidate.isNotBlank() && candidate != "No data" && candidate != "N/A") {
                            note = candidate
                            break
                        }
                    }
                }
                if (note.isBlank()) {
                    for (k in candidateKeys) {
                        val candidate = obj.optString("${k}_explain", "").trim()
                        if (candidate.isNotBlank() && candidate != "No data" && candidate != "N/A") {
                            note = candidate
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        if (note.isNotBlank() && note != "No data" && note != "N/A") {
            val rName = r.name.ifBlank { r.wearDate.ifBlank { "Wearing Report" } }
            val garmentsForReport = (reportToGarmentsMap[r.id]
                ?: reportToGarmentsMap[r.name.trim().lowercase()]
                ?: emptyList()).distinct()
            val clothesLabel = if (garmentsForReport.isNotEmpty()) {
                garmentsForReport.joinToString(" + ")
            } else {
                ""
            }
            val src = if (clothesLabel.isNotBlank()) {
                "👕 衣服: $clothesLabel · 📅 报告: $rName"
            } else {
                "📅 报告: $rName"
            }
            val existing = map[note]
            if (existing != null) {
                if (!existing.sources.contains(src)) {
                    existing.sources.add(src)
                }
                if (existing.clothesSource.isBlank() && clothesLabel.isNotBlank()) {
                    existing.clothesSource = clothesLabel
                }
                if (existing.reportSource.isBlank()) {
                    existing.reportSource = rName
                }
                existing.count++
            } else {
                map[note] = AggregateInfo(
                    count = 1,
                    sources = mutableListOf(src),
                    clothesSource = clothesLabel,
                    reportSource = rName
                )
            }
        }
    }

    return map.entries.map { (text, info) ->
        FieldHistoryEntry(
            text = text,
            garmentName = if (info.clothesSource.isNotBlank()) info.clothesSource else info.reportSource.ifBlank { "Source" },
            count = info.count,
            sources = info.sources,
            clothesSource = info.clothesSource,
            reportSource = info.reportSource
        )
    }.sortedByDescending { it.count }
}

/**
 * Ergonomic Explain Field component matching webapp's xd component:
 * - Collapsed: "+ 说明 (Explain)" button or purple card preview if text already exists.
 * - Expanded: Full explanation textarea, size presets (中/大/特大/撑满),
 *   historical notes with "+ 追加说明" / "直接填入", and "同步至 AI 字典定义".
 */
@Composable
fun ErgonomicExplainField(
    fieldKey: String,
    fieldLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    modifier: Modifier = Modifier,
    allClothes: List<GarmentEntity> = emptyList(),
    allReports: List<ReportEntity> = emptyList(),
    allJunctions: List<com.example.data.model.JunctionEntity> = emptyList(),
    customPlaceholder: String? = null,
    dictionaryService: FieldDictionaryService? = null,
    onOpenPrevious: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isAiPaused = remember(context) { com.example.util.GeminiAiService.isAiPaused(context) }
    var heightMode by remember { mutableIntStateOf(140) } // dp: 140=中, 240=大, 380=特大, -1=撑满
    var isFullExpand by remember { mutableStateOf(false) }
    var showPreviousModal by remember { mutableStateOf(false) }
    var internalExpanded by remember { mutableStateOf(isExpanded || value.isNotBlank()) }
    var historySearchQuery by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(isExpanded) {
        internalExpanded = isExpanded
    }
    val effectiveExpanded = internalExpanded

    val history = remember(fieldKey, allClothes, allReports, allJunctions) {
        getHistoricalExplanations(fieldKey, allClothes, allReports, allJunctions)
    }

    val filteredHistory = remember(history, historySearchQuery) {
        if (historySearchQuery.isBlank()) history
        else {
            val q = historySearchQuery.trim().lowercase(java.util.Locale.ROOT)
            history.filter { item ->
                item.text.lowercase(java.util.Locale.ROOT).contains(q) ||
                item.clothesSource.lowercase(java.util.Locale.ROOT).contains(q) ||
                item.reportSource.lowercase(java.util.Locale.ROOT).contains(q) ||
                item.garmentName.lowercase(java.util.Locale.ROOT).contains(q) ||
                item.sources.any { s -> s.lowercase(java.util.Locale.ROOT).contains(q) }
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (!effectiveExpanded) {
            // Collapsed view
            if (value.isNotBlank()) {
                // Purple preview banner when explanation text is present
                Surface(
                    onClick = {
                        internalExpanded = true
                        onToggleExpand()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFBF7FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("explain_preview_${fieldKey}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${fieldLabel.removeSuffix(":")} explain:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B21A8)
                            )
                            Text(
                                text = value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3B0764),
                                lineHeight = 16.sp,
                                maxLines = 4
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Clean prominent button "+ 说明 (Explain)" when blank
                Surface(
                    onClick = {
                        internalExpanded = true
                        onToggleExpand()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("explain_toggle_btn_${fieldKey}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "+ 说明 (Explain)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurple
                        )
                    }
                }
            }
        } else {
            // Expanded full editing card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFAF5FF),
                border = BorderStroke(1.5.dp, Color(0xFFD8B4FE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Title + Prominent Previous button + Collapse button (Always clear & never clipped!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${fieldLabel.removeSuffix(":")} 体感说明",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // PROMINENT PREVIOUS BUTTON (Always accessible, never squeezed)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEDE9FE),
                                border = BorderStroke(1.dp, Color(0xFFC4B5FD)),
                                modifier = Modifier
                                    .clickable {
                                        if (onOpenPrevious != null) {
                                            onOpenPrevious()
                                        } else {
                                            showPreviousModal = true
                                        }
                                    }
                                    .testTag("explain_previous_btn_${fieldKey}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Previous explanations",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Previous",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D28D9)
                                    )
                                }
                            }

                            // Collapse button
                            Surface(
                                onClick = {
                                    internalExpanded = false
                                    onToggleExpand()
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFD8B4FE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "收起",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandLess,
                                        contentDescription = null,
                                        tint = Slate700,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Sizing toolbar directly above input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Full expand button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isFullExpand) Color(0xFF7E22CE) else Color.White,
                                border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                modifier = Modifier.clickable {
                                    isFullExpand = !isFullExpand
                                    if (isFullExpand) heightMode = -1 else heightMode = 140
                                }
                            ) {
                                Text(
                                    text = "全文撑满",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFullExpand) Color.White else Color(0xFF6B21A8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Height presets: 中 / 大 / 特大
                            listOf(
                                120 to "中",
                                220 to "大",
                                360 to "特大"
                            ).forEach { (h, label) ->
                                val isSelected = !isFullExpand && heightMode == h
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Color(0xFF7E22CE) else Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                    modifier = Modifier.clickable {
                                        isFullExpand = false
                                        heightMode = h
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Slate700,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isFullExpand) "撑满全文中" else "${heightMode}dp",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7E22CE)
                        )
                    }

                    // Multiline Outlined Text Field
                    val textFieldModifier = if (isFullExpand || heightMode <= 0) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(heightMode.dp)
                    }

                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = {
                            Text(
                                text = customPlaceholder
                                    ?: "记录关于 $fieldLabel 的真实穿着体感、版型优缺点、紧绷/压胸或透气表现...",
                                fontSize = 12.sp,
                                color = Slate400,
                                lineHeight = 16.sp
                            )
                        },
                        modifier = textFieldModifier.testTag("explain_input_${fieldKey}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFD8B4FE),
                            focusedBorderColor = Color(0xFF7E22CE)
                        )
                    )

                    // Quick height adjustment bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3E8FF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                modifier = Modifier.clickable {
                                    isFullExpand = false
                                    heightMode = (heightMode - 60).coerceAtLeast(80)
                                }
                            ) {
                                Text(
                                    text = "-60dp",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF6B21A8),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                modifier = Modifier.clickable {
                                    isFullExpand = false
                                    heightMode = (heightMode + 80).coerceAtMost(600)
                                }
                            ) {
                                Text(
                                    text = "+80dp",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF6B21A8),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isFullExpand) "撑满全文中" else "${heightMode}dp · 调整高度",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7E22CE)
                        )

                        Text(
                            text = "重置",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600,
                            modifier = Modifier.clickable {
                                isFullExpand = false
                                heightMode = 140
                            }
                        )
                    }

                    // Historical Previously Used Explanations
                    if (history.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = BrandPurple,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "历史曾用体感说明:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF581C87)
                                    )
                                }
                                Text(
                                    text = if (historySearchQuery.isBlank()) "共 ${history.size} 条" else "筛选 ${filteredHistory.size}/${history.size} 条",
                                    fontSize = 10.sp,
                                    color = Slate500,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Real-time search filter for historical explanations
                            OutlinedTextField(
                                value = historySearchQuery,
                                onValueChange = { historySearchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "🔍 搜索历史说明、衣服名称 (如 dress, bra) 或报告...",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.sp, color = Slate800),
                                trailingIcon = {
                                    if (historySearchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { historySearchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear search",
                                                tint = Slate400,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF9333EA),
                                    unfocusedBorderColor = Color(0xFFE9D5FF)
                                )
                            )

                            if (filteredHistory.isEmpty()) {
                                Text(
                                    text = "未找到包含「$historySearchQuery」的历史体感说明",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    filteredHistory.forEach { item ->
                                        val isIncluded = value.contains(item.text)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isIncluded) Color(0xFFF3E8FF) else Color.White,
                                            border = BorderStroke(1.dp, if (isIncluded) Color(0xFFC084FC) else Color(0xFFE9D5FF)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        // Explicit Clothes Source Pill (fixes report title bug!)
                                                        if (item.clothesSource.isNotBlank()) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFF3E8FF),
                                                                border = BorderStroke(1.dp, Color(0xFFD8B4FE))
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Checkroom,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFF7E22CE),
                                                                        modifier = Modifier.size(11.dp)
                                                                    )
                                                                    Text(
                                                                        text = "衣服: ${item.clothesSource}",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF581C87)
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFEDE9FE)
                                                            ) {
                                                                Text(
                                                                    text = "👕 ${item.garmentName}",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF581C87),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        // Report Source Pill
                                                        if (item.reportSource.isNotBlank()) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Slate100
                                                            ) {
                                                                Text(
                                                                    text = "📅 ${item.reportSource}",
                                                                    fontSize = 9.sp,
                                                                    color = Slate600,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        if (item.count > 1) {
                                                            Text(
                                                                text = "曾使用 ${item.count} 次",
                                                                fontSize = 9.sp,
                                                                color = Slate500,
                                                                fontFamily = FontFamily.Monospace,
                                                                modifier = Modifier.align(Alignment.CenterVertically)
                                                            )
                                                        }
                                                    }

                                                    if (isIncluded) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color(0xFF047857),
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Text(
                                                                text = "已包含",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF047857)
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = item.text,
                                                    fontSize = 11.sp,
                                                    color = Slate800,
                                                    lineHeight = 15.sp
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Append button (+ 追加说明)
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (isIncluded) Slate100 else Color(0xFF7E22CE),
                                                        modifier = Modifier.clickable(enabled = !isIncluded) {
                                                            val next = if (value.isBlank()) item.text else "$value\n${item.text}"
                                                            onValueChange(next)
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = null,
                                                                tint = if (isIncluded) Slate400 else Color.White,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                text = "追加说明",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isIncluded) Slate400 else Color.White
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    // Replace button (直接填入)
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(1.dp, Slate200),
                                                        modifier = Modifier.clickable {
                                                            onValueChange(item.text)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "直接填入",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Slate700,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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

                    // Bottom action row: Status + 同步至 AI 字典定义
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (value.isNotBlank()) "✓ 已保存至当前体感说明" else "输入即可实时记录说明",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (value.isNotBlank()) Color(0xFF047857) else Slate500
                        )

                        if (!isAiPaused) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (value.isNotBlank()) Color(0xFF7E22CE) else Slate100,
                                modifier = Modifier.clickable(enabled = value.isNotBlank()) {
                                    try {
                                        val service = dictionaryService ?: FieldDictionaryService(context)
                                        val item = DictionaryFieldItem(
                                            name = fieldKey,
                                            category = "clothes",
                                            labelCn = fieldLabel,
                                            descriptionCn = value.trim()
                                        )
                                        service.saveDefinition(item)
                                        Toast.makeText(context, "已将「$fieldLabel」说明同步至 AI 字典！", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "同步失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = if (value.isNotBlank()) Color.White else Slate400,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "同步至 AI 字典定义",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (value.isNotBlank()) Color.White else Slate400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPreviousModal) {
        val previousEntries = remember(history) {
            history.map {
                PreviousEntry(
                    value = it.text,
                    count = it.count,
                    sources = it.sources
                )
            }
        }
        PreviousEntriesModal(
            isOpen = true,
            fieldLabel = "$fieldLabel explain",
            entries = previousEntries,
            currentValue = value,
            onSelect = { chosenVal ->
                onValueChange(chosenVal)
                showPreviousModal = false
            },
            onClose = { showPreviousModal = false }
        )
    }
}
