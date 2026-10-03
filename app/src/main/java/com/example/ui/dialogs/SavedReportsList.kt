package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standard AI Report Version representation matching web app schema:
 * { id, timestamp, title, model, content, isPrimary }
 */
data class AiReportVersion(
    val id: String,
    val timestamp: String,
    val title: String,
    val model: String = "gemini-3.6-flash",
    val content: String,
    val isPrimary: Boolean = false
)

object AiReportVersionHelper {
    fun parseVersions(rawJson: String, fallbackContent: String = "", fallbackTitle: String = "Initial Report"): List<AiReportVersion> {
        val list = mutableListOf<AiReportVersion>()
        if (rawJson.isNotBlank() && rawJson != "[]") {
            try {
                val arr = JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        AiReportVersion(
                            id = obj.optString("id", "ver_${System.currentTimeMillis()}_$i"),
                            timestamp = obj.optString("timestamp", ""),
                            title = obj.optString("title", "Report #${i + 1}"),
                            model = obj.optString("model", "gemini-3.6-flash"),
                            content = obj.optString("content", ""),
                            isPrimary = obj.optBoolean("isPrimary", i == 0)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty() && fallbackContent.isNotBlank()) {
            list.add(
                AiReportVersion(
                    id = "legacy_init",
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    title = fallbackTitle,
                    model = "gemini-3.6-flash",
                    content = fallbackContent,
                    isPrimary = true
                )
            )
        }
        return list
    }

    fun serializeVersions(versions: List<AiReportVersion>): String {
        val arr = JSONArray()
        versions.forEach { v ->
            val obj = JSONObject().apply {
                put("id", v.id)
                put("timestamp", v.timestamp)
                put("title", v.title)
                put("model", v.model)
                put("content", v.content)
                put("isPrimary", v.isPrimary)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun formatDisplayDate(raw: String): String {
        if (raw.isBlank()) return SimpleDateFormat("M/d/yyyy, h:mm a", Locale.getDefault()).format(Date())
        return try {
            if (raw.contains("T")) {
                val parser = SimpleDateFormat("yyyy-MM-ddTHH:mm:ss", Locale.getDefault())
                val d = parser.parse(raw.substringBefore("."))
                SimpleDateFormat("M/d/yyyy, h:mm a", Locale.getDefault()).format(d ?: Date())
            } else {
                raw
            }
        } catch (_: Exception) {
            raw
        }
    }
}

/**
 * Universal Unified AI Report Item across Macro, Clothes, Wearing, and Junction
 */
data class UniversalAiReportItem(
    val id: String,
    val type: String, // "macro", "clothes", "wearing", "junction"
    val typeLabel: String,
    val sourceEntityId: String,
    val sourceEntityTitle: String,
    val versionId: String,
    val title: String,
    val timestamp: String,
    val model: String = "gemini-3.6-flash",
    val isPrimary: Boolean = false,
    val content: String,
    val metaInfo: String = "",
    val garment: GarmentEntity? = null,
    val report: ReportEntity? = null,
    val junction: JunctionEntity? = null
)

// Legacy item for backward compatibility
data class SavedAiReportItem(
    val id: String,
    val title: String,
    val category: String,
    val dateCreated: String,
    val content: String
)

/**
 * 全维度 AI 报告管理归档中心 (All AI Reports Archive)
 * Matching Screenshot_20261003-122012.png, 122016.png, 122019.png
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllAiReportsArchiveView(
    clothes: List<GarmentEntity> = emptyList(),
    reports: List<ReportEntity> = emptyList(),
    junctions: List<JunctionEntity> = emptyList(),
    macroReports: List<UniversalAiReportItem> = emptyList(),
    onUpdateReport: (UniversalAiReportItem, String, String) -> Unit, // item, newTitle, newContent
    onDeleteReport: (UniversalAiReportItem) -> Unit,
    onSetPrimary: (UniversalAiReportItem) -> Unit,
    onSelectGarment: ((GarmentEntity) -> Unit)? = null,
    onSelectReport: ((ReportEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeCategoryFilter by remember { mutableStateOf("all") } // "all", "macro", "clothes", "wearing", "junction"
    var searchQuery by remember { mutableStateOf("") }
    var expandedReportId by remember { mutableStateOf<String?>(null) }
    var editingReportId by remember { mutableStateOf<String?>(null) }
    var editTitleText by remember { mutableStateOf("") }
    var editContentText by remember { mutableStateOf("") }
    var copiedReportId by remember { mutableStateOf<String?>(null) }
    var reportToDelete by remember { mutableStateOf<UniversalAiReportItem?>(null) }

    // Aggregate all AI reports across Macro, Clothes, Wearing, and Junction
    val allUnifiedReports = remember(clothes, reports, junctions, macroReports) {
        val list = mutableListOf<UniversalAiReportItem>()

        // 1. Macro AI Reports
        list.addAll(macroReports)

        // 2. Clothes AI Reports
        clothes.forEach { g ->
            val versions = AiReportVersionHelper.parseVersions(g.aiReports, g.aiClothesAnalysis, "Initial Report")
            versions.forEachIndexed { idx, v ->
                list.add(
                    UniversalAiReportItem(
                        id = "clothes_${g.id}_${v.id}",
                        type = "clothes",
                        typeLabel = "单件衣服诊断",
                        sourceEntityId = g.id,
                        sourceEntityTitle = g.name.ifBlank { "单品 #${g.id.take(6)}" },
                        versionId = v.id,
                        title = v.title.ifBlank { "单品诊断 #${idx + 1}" },
                        timestamp = v.timestamp,
                        model = v.model,
                        isPrimary = v.isPrimary,
                        content = v.content,
                        metaInfo = "${g.clothesType.ifBlank { "衣服" }} · ${g.material.ifBlank { "面料" }} · 炎热度: ${g.hotAtNoon.ifBlank { "未填" }}",
                        garment = g
                    )
                )
            }
        }

        // 3. Wearing Reports
        reports.forEach { r ->
            val versions = AiReportVersionHelper.parseVersions(r.aiReports, r.aiWearingAnalysis, "Initial Report")
            versions.forEachIndexed { idx, v ->
                val displayTitle = r.name.ifBlank {
                    if (r.wearDate.isNotBlank()) "穿着记录 ${r.wearDate}" else "记录 #${r.id.take(6)}"
                }
                list.add(
                    UniversalAiReportItem(
                        id = "wearing_${r.id}_${v.id}",
                        type = "wearing",
                        typeLabel = "穿着记录诊断",
                        sourceEntityId = r.id,
                        sourceEntityTitle = displayTitle,
                        versionId = v.id,
                        title = v.title.ifBlank { "穿着诊断 #${idx + 1}" },
                        timestamp = v.timestamp,
                        model = v.model,
                        isPrimary = v.isPrimary,
                        content = v.content,
                        metaInfo = "穿着日期: ${r.wearDate.ifBlank { "未填" }} · 肩感: ${r.shoulderFeel.ifBlank { "未填" }}",
                        report = r
                    )
                )
            }
        }

        // 4. Junction Reports
        junctions.forEachIndexed { jIdx, j ->
            val versions = AiReportVersionHelper.parseVersions("", j.aiJunctionAnalysis, "搭配诊断 #1")
            val relatedGarment = clothes.find { it.id == j.clothesId }
            val garmentName = relatedGarment?.name ?: j.clothesTitle.ifBlank { "搭配单品" }
            versions.forEachIndexed { idx, v ->
                list.add(
                    UniversalAiReportItem(
                        id = "junction_${j.id}_${v.id}",
                        type = "junction",
                        typeLabel = "单品搭配诊断",
                        sourceEntityId = j.id,
                        sourceEntityTitle = "$garmentName (Junction #${j.name.takeIf { it > 0 } ?: (jIdx + 1)})",
                        versionId = v.id,
                        title = v.title.ifBlank { "搭配诊断 #${idx + 1}" },
                        timestamp = v.timestamp,
                        model = v.model,
                        isPrimary = v.isPrimary,
                        content = v.content,
                        metaInfo = "时长: ${(j.duration / 3600f).coerceAtLeast(0f)}h · 状态: ${j.washStated}",
                        junction = j,
                        garment = relatedGarment
                    )
                )
            }
        }

        // Sort descending by timestamp / date
        list.sortedByDescending { it.timestamp }
    }

    // Counts for Category Tabs
    val countAll = allUnifiedReports.size
    val countMacro = allUnifiedReports.count { it.type == "macro" }
    val countClothes = allUnifiedReports.count { it.type == "clothes" }
    val countWearing = allUnifiedReports.count { it.type == "wearing" }
    val countJunction = allUnifiedReports.count { it.type == "junction" }

    // Filter by category and search
    val filteredReports = allUnifiedReports.filter { item ->
        val matchesCategory = when (activeCategoryFilter) {
            "macro" -> item.type == "macro"
            "clothes" -> item.type == "clothes"
            "wearing" -> item.type == "wearing"
            "junction" -> item.type == "junction"
            else -> true
        }
        val query = searchQuery.trim().lowercase()
        val matchesSearch = query.isBlank() ||
                item.title.lowercase().contains(query) ||
                item.sourceEntityTitle.lowercase().contains(query) ||
                item.content.lowercase().contains(query) ||
                item.metaInfo.lowercase().contains(query)
        matchesCategory && matchesSearch
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("all_ai_reports_archive_view")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================================
            // 1. TOP HEADER: Purple Layered Icon + Title + Count Badge + Subtitle
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFF7E22CE),
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "全维度 AI 报告管理归档中心 (All AI Reports Archive)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3E8FF)
                            ) {
                                Text(
                                    text = "共 $countAll 份报告",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B21A8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "聚合全局穿搭顾问 (Macro)、单件衣服 (Clothes)、穿着记录 (Wearing) 与搭配 (Junction) 的全部多版本报告，支持集中查阅、编辑、删除与主报告设定。",
                            fontSize = 12.sp,
                            color = Slate500,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // =================================================================
            // 2. SEARCH BAR: Magnifying Glass + Placeholder + Clear [X]
            // =================================================================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "搜索报告标题、衣物或内容...",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Slate400,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF9333EA),
                    unfocusedBorderColor = Slate200
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("archive_search_input")
            )

            // =================================================================
            // 3. 5 CATEGORY FILTER TABS: 全部(85) | 全局顾问(2) | 单件衣服(32) | 穿着记录(51) | 单品搭配(0)
            // =================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "all" to "全部报告 ($countAll)",
                        "macro" to "全局顾问 ($countMacro)",
                        "clothes" to "单件衣服 ($countClothes)",
                        "wearing" to "穿着记录 ($countWearing)",
                        "junction" to "单品搭配 ($countJunction)"
                    )
                    tabs.forEach { (catId, label) ->
                        val isSelected = activeCategoryFilter == catId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color.White else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, Slate200) else null,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { activeCategoryFilter = catId }
                                .testTag("archive_filter_tab_$catId")
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF581C87) else Slate600,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 4. REPORT CARDS LIST
            // =================================================================
            if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (allUnifiedReports.isEmpty()) "暂无任何 AI 分析报告" else "未找到匹配的 AI 报告记录",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                        Text(
                            text = if (allUnifiedReports.isEmpty())
                                "您可以在「全局穿搭顾问」、「单件衣服诊断」或穿戴记录中生成多版本报告，所有报告将永久保存于此。"
                            else
                                "请尝试更换搜索关键字或切换上方筛选标签。",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredReports.forEach { item ->
                        UniversalReportCard(
                            item = item,
                            isExpanded = expandedReportId == item.id,
                            isEditing = editingReportId == item.id,
                            editTitleText = editTitleText,
                            editContentText = editContentText,
                            copied = copiedReportId == item.id,
                            onToggleExpand = {
                                expandedReportId = if (expandedReportId == item.id) null else item.id
                            },
                            onStartEdit = {
                                editingReportId = item.id
                                editTitleText = item.title
                                editContentText = item.content
                                expandedReportId = item.id
                            },
                            onCancelEdit = {
                                editingReportId = null
                            },
                            onSaveEdit = {
                                onUpdateReport(item, editTitleText, editContentText)
                                editingReportId = null
                                Toast.makeText(context, "已更新报告内容", Toast.LENGTH_SHORT).show()
                            },
                            onTitleChange = { editTitleText = it },
                            onContentChange = { editContentText = it },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AI Report", item.content)
                                clipboard.setPrimaryClip(clip)
                                copiedReportId = item.id
                                Toast.makeText(context, "报告正文已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                reportToDelete = item
                            },
                            onSetPrimary = {
                                onSetPrimary(item)
                                Toast.makeText(context, "已设为主版本报告", Toast.LENGTH_SHORT).show()
                            },
                            onSelectGarment = onSelectGarment,
                            onSelectReport = onSelectReport
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Delete
    reportToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = {
                Text(
                    text = "删除此报告版本",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate900
                )
            },
            text = {
                Text(
                    text = "确定要彻底删除该份报告吗？\n[${target.typeLabel}] ${target.title}",
                    fontSize = 13.sp,
                    color = Slate600
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReport(target)
                        reportToDelete = null
                        if (expandedReportId == target.id) expandedReportId = null
                        if (editingReportId == target.id) editingReportId = null
                        Toast.makeText(context, "已删除报告版本", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("彻底删除", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) {
                    Text("取消", color = Slate600, fontSize = 12.sp)
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White
        )
    }
}

/**
 * Individual Card in the Archive matching Screenshots 1, 2, 3
 */
@Composable
private fun UniversalReportCard(
    item: UniversalAiReportItem,
    isExpanded: Boolean,
    isEditing: Boolean,
    editTitleText: String,
    editContentText: String,
    copied: Boolean,
    onToggleExpand: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit,
    onSelectGarment: ((GarmentEntity) -> Unit)?,
    onSelectReport: ((ReportEntity) -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_card_${item.id}")
    ) {
        Column {
            // Header Row (Clickable to toggle expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Category Chip
                    val (badgeBg, badgeText) = when (item.type) {
                        "macro" -> Color(0xFFFEF3C7) to Color(0xFF92400E) // Amber/Yellow
                        "clothes" -> Color(0xFFF3E8FF) to Color(0xFF6B21A8) // Purple
                        "wearing" -> Color(0xFFE0E7FF) to Color(0xFF3730A3) // Indigo/Blue
                        else -> Color(0xFFD1FAE5) to Color(0xFF065F46) // Emerald
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeText.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = item.typeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Title
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle / Entity Title (in Purple)
                    if (item.sourceEntityTitle.isNotBlank()) {
                        Text(
                            text = item.sourceEntityTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E22CE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Date & Time
                    Text(
                        text = AiReportVersionHelper.formatDisplayDate(item.timestamp),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate400
                    )

                    // Badges: Primary ★ and Model tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (item.isPrimary) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "主版本",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }

                        if (item.model.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Text(
                                    text = item.model,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Top Right Action Buttons: Copy, Edit, Delete, Expand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "复制报告正文",
                            tint = if (copied) BrandEmerald else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onStartEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑报告",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除此报告",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expanded Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Meta Info Banner + Navigation Buttons
                    if (item.metaInfo.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.metaInfo,
                                    fontSize = 11.sp,
                                    color = Slate600,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.garment != null && onSelectGarment != null) {
                                    Text(
                                        text = "打开单品卡片 →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7E22CE),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onSelectGarment(item.garment) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                } else if (item.report != null && onSelectReport != null) {
                                    Text(
                                        text = "打开穿着记录 →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4338CA),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onSelectReport(item.report) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // EDIT MODE vs VIEW MODE
                    if (isEditing) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "报告标题:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                    OutlinedTextField(
                                        value = editTitleText,
                                        onValueChange = onTitleChange,
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF9333EA),
                                            unfocusedBorderColor = Slate300
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "报告正文 (Markdown):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                    OutlinedTextField(
                                        value = editContentText,
                                        onValueChange = onContentChange,
                                        minLines = 6,
                                        maxLines = 14,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF9333EA),
                                            unfocusedBorderColor = Slate300
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = onCancelEdit) {
                                        Text("取消", color = Slate600, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = onSaveEdit,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("保存更改", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // VIEW MODE
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (!item.isPrimary && item.type != "macro") {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Slate300),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onSetPrimary() }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFFD97706),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "设为主要版本",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate700
                                                    )
                                                }
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF3E8FF),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onStartEdit() }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = Color(0xFF7E22CE),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "编辑",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7E22CE)
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = Slate100)

                                Text(
                                    text = item.content,
                                    fontSize = 12.sp,
                                    color = Slate800,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal wrapper for SavedReportsList matching previous dialog calls
 */
@Composable
fun SavedReportsListModal(
    reports: List<SavedAiReportItem> = emptyList(),
    clothes: List<GarmentEntity> = emptyList(),
    wearingReports: List<ReportEntity> = emptyList(),
    junctions: List<JunctionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onDeleteReport: (String) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        AllAiReportsArchiveView(
                            clothes = clothes,
                            reports = wearingReports,
                            junctions = junctions,
                            macroReports = reports.map { r ->
                                UniversalAiReportItem(
                                    id = "macro_${r.id}",
                                    type = "macro",
                                    typeLabel = "全局穿搭顾问",
                                    sourceEntityId = r.id,
                                    sourceEntityTitle = r.category,
                                    versionId = r.id,
                                    title = r.title,
                                    timestamp = r.dateCreated,
                                    content = r.content,
                                    isPrimary = true
                                )
                            },
                            onUpdateReport = { _, _, _ -> },
                            onDeleteReport = { item -> onDeleteReport(item.versionId) },
                            onSetPrimary = {}
                        )
                    }
                }
            }
        }
    }
}
