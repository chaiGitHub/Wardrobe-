package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.FieldDictionaryService
import com.example.ui.dialogs.InlineFieldDictionaryCard
import com.example.ui.dialogs.SavedAiReportItem
import com.example.ui.dialogs.SavedReportsListModal
import com.example.ui.dialogs.AllAiReportsArchiveView
import com.example.ui.dialogs.UniversalAiReportItem
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
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
import com.example.util.GeminiAiService
import com.example.util.GroundingSource
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvisorScreen(
    clothes: List<GarmentEntity>,
    junctions: List<JunctionEntity> = emptyList(),
    reports: List<ReportEntity> = emptyList(),
    selectedGarment: GarmentEntity?,
    aiAdvice: String?,
    isGenerating: Boolean,
    onSelectGarment: (GarmentEntity?) -> Unit,
    onOpenGarmentDetail: ((GarmentEntity) -> Unit)? = null,
    onSelectReport: ((ReportEntity) -> Unit)? = null,
    viewModel: com.example.ui.WardrobeViewModel? = null,
    savedReports: List<SavedAiReportItem> = emptyList(),
    onSaveReport: ((title: String, category: String, content: String) -> Unit)? = null,
    onDeleteSavedReport: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val mainScrollState = rememberScrollState()

    val dictionaryService = remember { FieldDictionaryService(context) }
    val macroReports = viewModel?.macroAiReports?.collectAsState(initial = emptyList())?.value ?: emptyList()
    var dictionaryFields by remember { mutableStateOf(dictionaryService.getFields()) }

    var showSavedReportsModal by remember { mutableStateOf(false) }
    var showApiKeyModal by remember { mutableStateOf(false) }
    var customApiKeyInput by remember { mutableStateOf(GeminiAiService.getEffectiveApiKey(context)) }
    var hasApiKey by remember { mutableStateOf(GeminiAiService.hasValidApiKey(context)) }

    // 4-Tab Switcher: "macro", "clothes", "report", "junction"
    var activeAdvisorTab by remember { mutableStateOf("macro") }

    // =========================================================================
    // TAB 1: MACRO AI ADVISOR STATE (Clothes + WearingReport + Junction)
    // =========================================================================
    var macroTempInput by remember { mutableStateOf("28") }
    var macroOccasion by remember { mutableStateOf("日常休闲 / 居家") }
    var macroPreference by remember { mutableStateOf("透气凉爽 · 防闷汗") }
    var macroCustomQuestion by remember { mutableStateOf("") }
    var macroCandidateSearch by remember { mutableStateOf("") }
    var macroCategoryFilter by remember { mutableStateOf("All") }
    val selectedCandidateIds = remember { mutableStateListOf<String>() }

    var isGeneratingMacro by remember { mutableStateOf(false) }
    var generatedMacroReport by remember { mutableStateOf<String?>(null) }
    var generatedMacroSources by remember { mutableStateOf<List<GroundingSource>>(emptyList()) }
    var generatedMacroNote by remember { mutableStateOf<String?>(null) }
    var hasSavedMacro by remember { mutableStateOf(false) }

    // =========================================================================
    // TAB 2: CLOTHES AI STATE (Clothes Field-Only)
    // =========================================================================
    var clothesSearchQuery by remember { mutableStateOf("") }
    var clothesTypeFilter by remember { mutableStateOf("All") }
    var activeClothesTarget by remember(selectedGarment, clothes) {
        mutableStateOf(selectedGarment ?: clothes.firstOrNull())
    }
    var isGeneratingClothesAi by remember { mutableStateOf(false) }
    var generatedClothesReport by remember(activeClothesTarget?.id, aiAdvice) {
        mutableStateOf<String?>(activeClothesTarget?.aiClothesAnalysis?.ifBlank { aiAdvice })
    }
    var generatedClothesNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedGarment) {
        if (selectedGarment != null) {
            activeClothesTarget = selectedGarment
        }
    }

    // =========================================================================
    // TAB 3: WEARING REPORT AI STATE (WearingReport Field-Only)
    // =========================================================================
    var reportSearchQuery by remember { mutableStateOf("") }
    var activeReportTarget by remember(reports) { mutableStateOf(reports.firstOrNull()) }
    var isGeneratingReportAi by remember { mutableStateOf(false) }
    var generatedReportAnalysis by remember(activeReportTarget?.id) {
        mutableStateOf<String?>(activeReportTarget?.aiWearingAnalysis?.ifBlank { null })
    }
    var generatedReportNote by remember { mutableStateOf<String?>(null) }

    // =========================================================================
    // TAB 4: JUNCTION AI STATE (Junction Field-Only)
    // =========================================================================
    var activeJunctionTarget by remember(junctions) { mutableStateOf(junctions.firstOrNull()) }
    var isGeneratingJunctionAi by remember { mutableStateOf(false) }
    var generatedJunctionAnalysis by remember(activeJunctionTarget?.id) {
        mutableStateOf(activeJunctionTarget?.aiJunctionAnalysis?.ifBlank { null })
    }
    var generatedJunctionNote by remember { mutableStateOf<String?>(null) }

    val occasionOptions = listOf(
        "日常休闲 / 居家",
        "户外出行 / 散步",
        "通勤 / 办公",
        "聚会 / 约会",
        "运动 / 高活动量",
        "睡眠 / 夜间休息"
    )

    val preferenceOptions = listOf(
        "透气凉爽 · 防闷汗",
        "护肩减压 · 不勒肩",
        "胸部舒适 · 坐姿不紧",
        "亲肤柔软 · 防磨腿",
        "显瘦美观 · 拍照出片"
    )

    val categoryCounts = remember(clothes) {
        val grouped = clothes.groupBy { it.clothesType.trim().ifBlank { "Dress" } }
        val result = mutableListOf("All" to clothes.size)
        grouped.entries.sortedByDescending { it.value.size }.forEach { (k, v) ->
            result.add(k to v.size)
        }
        result
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(mainScrollState)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. HEADER CARD (Matching Screenshots 13-19)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AI 智能穿搭与单品顾问",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "融合字段中英字典、衣物属性、穿搭报告与清洗记录的精准诊断",
                            fontSize = 11.sp,
                            color = Slate500,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandPurpleLight,
                        border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                        modifier = Modifier
                            .clickable { showSavedReportsModal = true }
                            .testTag("saved_reports_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "已归档 (${savedReports.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasApiKey) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, if (hasApiKey) Color(0xFFA7F3D0) else Color(0xFFFDE68A)),
                        modifier = Modifier
                            .clickable { showApiKeyModal = true }
                            .testTag("gemini_api_key_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (hasApiKey) BrandEmerald else BrandAmber,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (hasApiKey) "Gemini 已就绪" else "配置 API Key",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasApiKey) Color(0xFF047857) else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
        }

        // 2. INLINE COLLAPSIBLE FIELD & DATA DICTIONARY (30 CONFIG ITEMS: Clothes 8 + WearingReport 20 + Junction 2)
        InlineFieldDictionaryCard(
            dictionaryService = dictionaryService,
            clothes = clothes,
            reports = reports,
            fields = dictionaryFields,
            onFieldsUpdated = { dictionaryFields = it }
        )

        // 3. 4-TAB SEGMENTED SWITCHER (Macro AI | Clothes AI | WearingReport AI | Junction AI)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("macro", "🌐 全局顾问", "Macro Advisor"),
                    Triple("clothes", "👗 单件衣服", "Garment AI"),
                    Triple("report", "📋 穿着记录", "Wearing AI"),
                    Triple("junction", "🔗 单品搭配", "Junction AI"),
                    Triple("archive", "📂 报告归档", "All Reports Archive")
                ).forEach { (tabId, title, subtitle) ->
                    val isSelected = activeAdvisorTab == tabId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BrandPurple else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) BrandPurple else Slate200),
                        modifier = Modifier
                            .clickable { activeAdvisorTab = tabId }
                            .testTag("advisor_tab_$tabId")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Slate800
                            )
                            Text(
                                text = subtitle,
                                fontSize = 9.sp,
                                color = if (isSelected) Color(0xFFF3E8FF) else Slate500
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 1 CONTENT: 全局穿搭顾问 (Macro AI) - Screenshots 13, 17
        // =====================================================================
        if (activeAdvisorTab == "macro") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "全局宏观穿搭与体感分析 (Macro AI Report)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = "综合 Clothes 衣物字段 + WearingReport 穿搭报告 + Junction 关联记录 + 30 项字段字典",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    // Weather Temp + Occasion Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(105.dp)) {
                            Text(
                                text = "今日温度 (°C)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = macroTempInput,
                                onValueChange = { macroTempInput = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forecast_input"),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = Slate200,
                                    focusedBorderColor = BrandPurple
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "穿搭场景 / 场合",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                occasionOptions.forEach { occ ->
                                    val sel = macroOccasion == occ
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (sel) BrandPurple else Color(0xFFF1F5F9),
                                        modifier = Modifier.clickable { macroOccasion = occ }
                                    ) {
                                        Text(
                                            text = occ,
                                            fontSize = 11.sp,
                                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (sel) Color.White else Slate700,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sensory Preference Pills
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "体感诉求偏好",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            preferenceOptions.forEach { pref ->
                                val sel = macroPreference == pref
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (sel) Color(0xFFF3E8FF) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (sel) BrandPurple else Slate200),
                                    modifier = Modifier.clickable { macroPreference = pref }
                                ) {
                                    Text(
                                        text = pref,
                                        fontSize = 11.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (sel) BrandPurple else Slate700,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // Candidate Garment Selection (Select 1+ OR leave empty for Whole Dress / Entire Wardrobe)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCandidateIds.isEmpty()) {
                                "指定候选衣物 (可选 · 当前未选，默认评估全衣橱连衣裙/单品)"
                            } else {
                                "指定候选衣物 (已选 ${selectedCandidateIds.size} 件)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedCandidateIds.isNotEmpty()) {
                            Text(
                                text = "清空选择 (恢复全衣橱)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple,
                                modifier = Modifier
                                    .clickable { selectedCandidateIds.clear() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Search + Category Filter for Candidate Garments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = macroCandidateSearch,
                            onValueChange = { macroCandidateSearch = it },
                            placeholder = { Text("搜索候选衣物名称或材质...", fontSize = 11.sp, color = Slate400) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Slate900),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Slate200,
                                focusedBorderColor = BrandPurple
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryCounts.forEach { (cat, count) ->
                            val sel = macroCategoryFilter == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) Slate900 else Slate100,
                                modifier = Modifier.clickable { macroCategoryFilter = cat }
                            ) {
                                Text(
                                    text = "$cat ($count)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else Slate600,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    val filteredMacroCandidates = remember(clothes, macroCandidateSearch, macroCategoryFilter) {
                        clothes.filter { g ->
                            val cat = g.clothesType.trim().ifBlank { "Dress" }
                            val matchesCat = macroCategoryFilter == "All" || cat.equals(macroCategoryFilter, ignoreCase = true)
                            val q = macroCandidateSearch.trim().lowercase(Locale.ROOT)
                            val matchesQ = q.isBlank() ||
                                g.name.lowercase(Locale.ROOT).contains(q) ||
                                g.material.lowercase(Locale.ROOT).contains(q)
                            matchesCat && matchesQ
                        }
                    }

                    // Horizontal scrollable candidate chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredMacroCandidates.take(35).forEach { g ->
                            val isChecked = g.id in selectedCandidateIds
                            val needWash = DrilldownUtils.calculateGarmentStats(g.id, junctions).needWash
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecked) Color(0xFFFAF5FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = if (isChecked) 1.5.dp else 1.dp,
                                    color = if (isChecked) BrandPurple else Slate200
                                ),
                                modifier = Modifier.clickable {
                                    if (isChecked) selectedCandidateIds.remove(g.id)
                                    else selectedCandidateIds.add(g.id)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    if (isChecked) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = BrandPurple,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = g.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isChecked) BrandPurple else Slate800,
                                        maxLines = 1
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (needWash) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = if (needWash) "待洗" else "干净",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (needWash) Color(0xFFB45309) else Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Purple Hint Banner explaining 0-selection vs 1+ selection (Matching Screenshot 17)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFAF5FF),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedCandidateIds.isEmpty()) {
                                "💡 未勾选特定衣物时，宏观 AI 将自动评估全衣橱所有连衣裙/单品 (${clothes.size} 件)，并融合全部穿搭报告 (${reports.size} 条) 与关联记录 (${junctions.size} 条) 生成全盘报告。"
                            } else {
                                "🎯 已指定 ${selectedCandidateIds.size} 件候选衣物，宏观 AI 将聚焦这些单品的 Clothes 字段、历史 WearingReport 与 Junction 记录进行深度对比。"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF6B21A8),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Optional specific question
                    OutlinedTextField(
                        value = macroCustomQuestion,
                        onValueChange = { macroCustomQuestion = it },
                        placeholder = {
                            Text(
                                text = "补充诉求或具体提问（可选，如：对比哪件连衣裙中午最不闷汗且不勒肩？）",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Slate900),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Slate200,
                            focusedBorderColor = BrandPurple
                        )
                    )

                    // Generate Macro AI Report Button
                    val selectedMacroGarments = clothes.filter { it.id in selectedCandidateIds }
                    Button(
                        onClick = {
                            if (isGeneratingMacro) return@Button
                            isGeneratingMacro = true
                            hasSavedMacro = false

                            val scopeTitle = if (selectedMacroGarments.isEmpty()) {
                                "全衣橱连衣裙/单品 (${clothes.size}件)"
                            } else {
                                selectedMacroGarments.joinToString("、") { it.name }.take(36)
                            }

                            val fallbackBuilder = {
                                val pool = if (selectedMacroGarments.isNotEmpty()) selectedMacroGarments else clothes
                                val cleanPool = pool.filter {
                                    !DrilldownUtils.calculateGarmentStats(it.id, junctions).needWash
                                }
                                val picks = (if (cleanPool.isNotEmpty()) cleanPool else pool)
                                    .sortedByDescending { it.wearingFrequency + it.trackedAppWashes }
                                    .take(5)

                                buildString {
                                    appendLine("### 🌐 宏观穿搭与体感综合报告 ($scopeTitle)")
                                    appendLine("- **环境与诉求**: 温度 **${macroTempInput}°C** · 场景 **$macroOccasion** · 偏好 **$macroPreference**")
                                    appendLine("- **数据融合范围**: 衣物 **${pool.size}** 件 (干净可穿 ${cleanPool.size} 件) + 穿搭报告 **${reports.size}** 条 + 关联记录 **${junctions.size}** 条 + 30 项字段中英字典")
                                    appendLine()
                                    appendLine("### 👗 核心推荐与字段交叉评估 (Clothes × WearingReport × Junction)")
                                    picks.forEachIndexed { index, g ->
                                        val st = DrilldownUtils.calculateGarmentStats(g.id, junctions)
                                        val totalW = g.wearingFrequency + g.trackedAppWashes
                                        val cpw = if (g.price > 0 && totalW > 0) String.format(Locale.US, "$%.2f", g.price / totalW) else "N/A"
                                        appendLine(
                                            "${index + 1}. **${g.name}** (${g.clothesType.ifBlank { "Dress" }} · ${g.material.ifBlank { "棉" }})\n" +
                                                "   - **Clothes 字段**: Without AC hot=`${(g.noAircondHotFeeling.ifBlank { g.hotAtNoon }).ifBlank { "Not hot" }}` | With AC=`${g.aircondHotFeeling.ifBlank { "-" }}` | Fan Sleep=`${g.fanSleepHotFeeling.ifBlank { "-" }}` | diao dai=`${g.diaoDai.ifBlank { "not diao dai" }}` | sit breast=`${g.sitBreast.ifBlank { "ok" }}` | side breast 聚拢=`${(g.sideBreastGatherBra.ifBlank { g.sideBreastOk }).ifBlank { "ok" }}` | side breast side out=`${g.sideBreastSideOutBra.ifBlank { "ok" }}` | Wet=`${g.wet.ifBlank { "dry" }}`\n" +
                                                "   - **Junction & 成本**: 累计洗涤 $totalW 次 (CPW $cpw) · 关联记录 ${st.totalWears} 次 (${st.totalDurationHours}h) · 状态: **${if (st.needWash) "待清洗 (need wash)" else "干净 (washed)"}**"
                                        )
                                    }
                                    appendLine()
                                    appendLine("### 🧘 历史体感反馈总结 (WearingReport 维度)")
                                    appendLine("- 基于最近 ${reports.size} 条穿搭报告，高温时优先选择 `Hot at noon = Not hot` 且 `diao dai = not diao dai` 的纯棉/透气连衣裙，可显著降低午间出汗 (`sweat`) 与肩部酸痛 (`shoulderFeel`)。")
                                }
                            }

                            coroutineScope.launch {
                                val queryText = buildString {
                                    append("今日温度: ${macroTempInput}°C，穿搭场景: $macroOccasion，体感诉求偏好: $macroPreference。")
                                    if (selectedMacroGarments.isEmpty()) {
                                        append("未勾选特定衣物，请综合全衣橱所有连衣裙/衣物 (Whole Dress / Full Wardrobe)、WearingReport 穿搭报告与 Junction 关联记录进行宏观评估与推荐。")
                                    } else {
                                        append("用户指定了以下候选衣物：${selectedMacroGarments.joinToString(", ") { it.name }}，请结合它们的 Clothes 字段、关联的 WearingReport 与 Junction 记录进行对比分析。")
                                    }
                                    if (macroCustomQuestion.isNotBlank()) {
                                        append(" 补充提问：$macroCustomQuestion")
                                    }
                                }
                                val resp = GeminiAiService.askWardrobeAdvisor(
                                    context = context,
                                    query = queryText,
                                    clothes = clothes,
                                    reports = reports,
                                    junctions = junctions,
                                    selectedClothes = selectedMacroGarments,
                                    fallbackGenerator = fallbackBuilder
                                )
                                generatedMacroReport = resp.text
                                generatedMacroSources = resp.sources
                                generatedMacroNote = resp.statusNote
                                isGeneratingMacro = false
                                if (resp.text.isNotBlank()) {
                                    viewModel?.addMacroAiReport(
                                        title = "穿搭体感报告 - ${macroOccasion.ifBlank { "常规天气" }}",
                                        occasion = macroOccasion.ifBlank { "常规天气" },
                                        content = resp.text,
                                        model = "gemini-3.1-flash-lite",
                                        metaInfo = if (selectedMacroGarments.isEmpty()) "全衣橱智能扫描" else "已选 ${selectedMacroGarments.size} 件单品"
                                    )
                                }
                            }
                        },
                        enabled = !isGeneratingMacro,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("get_outfit_button")
                    ) {
                        if (isGeneratingMacro) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在融合 Clothes + Report + Junction 生成宏观报告...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedCandidateIds.isEmpty()) {
                                    "获取穿搭与体感分析 (全衣橱宏观报告)"
                                } else {
                                    "获取穿搭与体感分析 (已选 ${selectedCandidateIds.size} 件)"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Generated Macro Report Result Box
                    if (!generatedMacroReport.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "MACRO AI 宏观穿搭与体感报告",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandPurple
                                        )
                                        if (!generatedMacroNote.isNullOrBlank()) {
                                            Text(
                                                text = generatedMacroNote!!,
                                                fontSize = 9.sp,
                                                color = Slate500
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cb.setPrimaryClip(ClipData.newPlainText("Macro AI Report", generatedMacroReport))
                                                Toast.makeText(context, "已复制宏观报告", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("复制", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val titleScope = if (selectedMacroGarments.isEmpty()) "全衣橱" else "${selectedMacroGarments.size}件候选"
                                                onSaveReport?.invoke(
                                                    "${macroTempInput}°C $macroOccasion ($titleScope)",
                                                    "Macro AI 宏观报告",
                                                    generatedMacroReport ?: ""
                                                )
                                                hasSavedMacro = true
                                                Toast.makeText(context, "已归档到独立 AI 报告记录", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (hasSavedMacro) BrandEmerald else BrandPurple
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(if (hasSavedMacro) "已归档" else "归档保存", fontSize = 11.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = generatedMacroReport ?: "",
                                    fontSize = 12.sp,
                                    color = Slate800,
                                    lineHeight = 18.sp
                                )

                                if (generatedMacroSources.isNotEmpty()) {
                                    HorizontalDivider(color = Color(0xFFE9D5FF))
                                    Text(
                                        text = "联网检索参考来源 (${generatedMacroSources.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPurple
                                    )
                                    generatedMacroSources.take(4).forEach { src ->
                                        Text(
                                            text = "🔗 ${src.title}",
                                            fontSize = 11.sp,
                                            color = BrandPurple,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.clickable {
                                                try { uriHandler.openUri(src.uri) } catch (_: Exception) {}
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ARCHIVED MACRO AI REPORTS SECTION (Matching Screenshot 17: 已归档的独立 AI 穿搭与体感报告记录)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "已归档的独立 AI 穿搭与体感报告记录 (${savedReports.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                        }
                    }

                    if (savedReports.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "暂无已归档报告。生成宏观报告或单品报告后点击「归档保存」即可永久保存在此并随 ZIP 备份导出。",
                                fontSize = 11.sp,
                                color = Slate500,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        savedReports.forEach { item ->
                            var expanded by remember(item.id) { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expanded = !expanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFF3E8FF)
                                                ) {
                                                    Text(
                                                        text = item.category,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandPurple,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = item.dateCreated,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Slate400
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = item.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cb.setPrimaryClip(ClipData.newPlainText(item.title, item.content))
                                                    Toast.makeText(context, "已复制报告内容", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "复制",
                                                    tint = Slate500,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteSavedReport?.invoke(item.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "删除",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Icon(
                                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Slate400,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (expanded) {
                                        HorizontalDivider(color = Slate200)
                                        Text(
                                            text = item.content,
                                            fontSize = 12.sp,
                                            color = Slate700,
                                            lineHeight = 18.sp
                                        )
                                    } else {
                                        Text(
                                            text = item.content.replace("\n", " "),
                                            fontSize = 11.sp,
                                            color = Slate500,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2 CONTENT: 单品字段专属报告 (Clothes Field-Only AI)
        // =====================================================================
        if (activeAdvisorTab == "clothes") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "单品字段专属 AI 报告 (Clothes Field-Only AI)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = "严格仅使用该件衣物的 Clothes 字段数据 + Clothes 字典定义 (8项) 生成诊断，不混入其它表数据",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    OutlinedTextField(
                        value = clothesSearchQuery,
                        onValueChange = { clothesSearchQuery = it },
                        placeholder = { Text("搜索衣物名称、材质或属性...", fontSize = 12.sp, color = Slate400) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audit_garment_search"),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Slate900),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Slate200,
                            focusedBorderColor = BrandPurple
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryCounts.forEach { (cat, count) ->
                            val sel = clothesTypeFilter == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) BrandPurple else Slate100,
                                modifier = Modifier.clickable { clothesTypeFilter = cat }
                            ) {
                                Text(
                                    text = "$cat ($count)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    val filteredClothesList = remember(clothes, clothesSearchQuery, clothesTypeFilter) {
                        clothes.filter { g ->
                            val cat = g.clothesType.trim().ifBlank { "Dress" }
                            val matchesCat = clothesTypeFilter == "All" || cat.equals(clothesTypeFilter, ignoreCase = true)
                            val q = clothesSearchQuery.trim().lowercase(Locale.ROOT)
                            val matchesQ = q.isBlank() ||
                                g.name.lowercase(Locale.ROOT).contains(q) ||
                                g.material.lowercase(Locale.ROOT).contains(q)
                            matchesCat && matchesQ
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredClothesList.take(40).forEach { g ->
                            val isSelected = activeClothesTarget?.id == g.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) BrandPurple else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) BrandPurple else Slate200),
                                modifier = Modifier.clickable {
                                    activeClothesTarget = g
                                    onSelectGarment(g)
                                    generatedClothesReport = g.aiClothesAnalysis.ifBlank { null }
                                }
                            ) {
                                Text(
                                    text = g.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Slate800,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    activeClothesTarget?.let { target ->
                        val totalWashes = target.wearingFrequency + target.trackedAppWashes
                        val cpw = if (target.price > 0 && totalWashes > 0) {
                            String.format(Locale.US, "$%.2f", target.price / maxOf(1, totalWashes))
                        } else "N/A"

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "当前单品字段数据: ${target.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900
                                    )
                                    if (onOpenGarmentDetail != null) {
                                        Text(
                                            text = "查看单品详情 ➔",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple,
                                            modifier = Modifier.clickable { onOpenGarmentDetail(target) }
                                        )
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "Type" to target.clothesType.ifBlank { "Dress" },
                                        "Material" to target.material.ifBlank { "-" },
                                        "Thick" to target.thickClothes.ifBlank { "-" },
                                        "Without AC Hot" to (target.noAircondHotFeeling.ifBlank { target.hotAtNoon }).ifBlank { "-" },
                                        "With AC Hot" to target.aircondHotFeeling.ifBlank { "-" },
                                        "Fan Sleep" to target.fanSleepHotFeeling.ifBlank { "-" },
                                        "diao dai" to target.diaoDai.ifBlank { "-" },
                                        "side breast 聚拢" to (target.sideBreastGatherBra.ifBlank { target.sideBreastOk }).ifBlank { "-" },
                                        "side breast side out" to target.sideBreastSideOutBra.ifBlank { "-" },
                                        "sit breast" to target.sitBreast.ifBlank { "-" },
                                        "stand breast" to target.standBreast.ifBlank { "-" },
                                        "Long dress" to target.longDress.ifBlank { "-" },
                                        "Beautiful" to target.beautiful.ifBlank { "-" },
                                        "Wet" to target.wet.ifBlank { "dry" },
                                        "Price/CPW" to "$${target.price} ($cpw)"
                                    ).forEach { (k, v) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200)
                                        ) {
                                            Text(
                                                text = "$k: $v",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate700,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isGeneratingClothesAi) return@Button
                                isGeneratingClothesAi = true
                                val fallbackBuilder = {
                                    GeminiAiService.buildDefaultGarmentErgonomicReport(context, target, totalWashes, cpw)
                                }
                                coroutineScope.launch {
                                    try {
                                        val resp = GeminiAiService.generateGarmentAuditReport(
                                            context = context,
                                            garment = target,
                                            versionNumber = 1,
                                            fallbackGenerator = fallbackBuilder
                                        )
                                        generatedClothesReport = resp.text
                                        generatedClothesNote = resp.statusNote
                                        if (resp.text.isNotBlank()) {
                                            viewModel?.addGarmentAiReportVersion(
                                                garmentId = target.id,
                                                content = resp.text,
                                                model = "gemini-3.5-flash"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("AdvisorScreen", "Error generating clothes AI report", e)
                                        Toast.makeText(context, "生成报告失败: ${e.localizedMessage ?: "未知异常"}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingClothesAi = false
                                    }
                                }
                            },
                            enabled = !isGeneratingClothesAi,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            if (isGeneratingClothesAi) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("正在仅基于 Clothes 字段生成单品报告...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("生成单品字段 AI 报告 (仅用 Clothes 数据)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!generatedClothesReport.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = generatedClothesNote ?: "单品字段专属 AI 报告 (Clothes Fields Only)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                onSaveReport?.invoke(
                                                    "单品报告: ${target.name}",
                                                    "Clothes 单品AI",
                                                    generatedClothesReport ?: ""
                                                )
                                                Toast.makeText(context, "已归档单品报告", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("归档保存", fontSize = 10.sp)
                                        }
                                    }
                                    Text(
                                        text = generatedClothesReport ?: "",
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

        // =====================================================================
        // TAB 3 CONTENT: 穿搭报告字段专属分析 (WearingReport Field-Only AI)
        // =====================================================================
        if (activeAdvisorTab == "report") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "穿搭报告专属 AI 分析 (WearingReport Field-Only AI)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "严格仅使用该次穿搭报告自身的 WearingReport 字段 + WearingReport 字典定义 (20项)，不混入衣橱表",
                        fontSize = 11.sp,
                        color = Slate500
                    )

                    OutlinedTextField(
                        value = reportSearchQuery,
                        onValueChange = { reportSearchQuery = it },
                        placeholder = { Text("搜索穿搭报告名称或日期...", fontSize = 12.sp, color = Slate400) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Slate900)
                    )

                    val filteredReports = remember(reports, reportSearchQuery) {
                        val q = reportSearchQuery.trim().lowercase(Locale.ROOT)
                        reports.filter { r ->
                            q.isBlank() ||
                                r.name.lowercase(Locale.ROOT).contains(q) ||
                                r.wearDate.lowercase(Locale.ROOT).contains(q) ||
                                r.reportDetail.lowercase(Locale.ROOT).contains(q)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredReports.take(35).forEach { r ->
                            val sel = activeReportTarget?.id == r.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) BrandPurple else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (sel) BrandPurple else Slate200),
                                modifier = Modifier.clickable {
                                    activeReportTarget = r
                                    generatedReportAnalysis = r.aiWearingAnalysis.ifBlank { null }
                                }
                            ) {
                                Text(
                                    text = r.name.ifBlank { r.wearDate.ifBlank { "穿搭报告" } },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else Slate800,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    activeReportTarget?.let { rep ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "报告字段: ${rep.name.ifBlank { "未命名报告" }} (${rep.wearDate.ifBlank { rep.created }})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "shoulderFeel" to rep.shoulderFeel.ifBlank { "normal" },
                                        "midBreast" to rep.midBreast.ifBlank { "normal" },
                                        "nightMidBreast" to rep.nightMidBreast.ifBlank { "normal" },
                                        "sweat" to rep.sweat.ifBlank { "none" },
                                        "nightSweat" to rep.nightSweat.ifBlank { "none" },
                                        "legFeel" to rep.legFeel.ifBlank { "normal" },
                                        "nightLegFeel" to rep.nightLegFeel.ifBlank { "normal" }
                                    ).forEach { (k, v) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200)
                                        ) {
                                            Text(
                                                text = "$k: $v",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate700,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isGeneratingReportAi) return@Button
                                isGeneratingReportAi = true
                                val fallbackBuilder = {
                                    buildString {
                                        appendLine("### 📋 穿搭报告专属字段分析: ${rep.name}")
                                        appendLine("*(数据范围：仅使用该次 WearingReport 字段 + WearingReport 字典定义)*")
                                        appendLine("- **日期**: ${rep.wearDate.ifBlank { rep.created }}")
                                        appendLine("- **日间体感**: 肩部 `shoulderFeel=${rep.shoulderFeel.ifBlank { "normal" }}` · 胸部 `midBreast=${rep.midBreast.ifBlank { "normal" }}` · 出汗 `sweat=${rep.sweat.ifBlank { "none" }}` · 腿部 `legFeel=${rep.legFeel.ifBlank { "normal" }}`")
                                        appendLine("- **夜间体感**: 夜间胸部 `nightMidBreast=${rep.nightMidBreast.ifBlank { "normal" }}` · 夜间盗汗 `nightSweat=${rep.nightSweat.ifBlank { "none" }}` · 夜间腿部 `nightLegFeel=${rep.nightLegFeel.ifBlank { "normal" }}`")
                                        appendLine("- **备注与环境详情**: ${rep.reportDetail.ifBlank { "常规记录" }}")
                                    }
                                }
                                coroutineScope.launch {
                                    try {
                                        val resp = GeminiAiService.generateSessionReportAnalysis(
                                            context = context,
                                            report = rep,
                                            versionNumber = 1,
                                            fallbackGenerator = fallbackBuilder
                                        )
                                        generatedReportAnalysis = resp.text
                                        generatedReportNote = resp.statusNote
                                        isGeneratingReportAi = false
                                        if (resp.text.isNotBlank()) {
                                            viewModel?.addWearingAiReportVersion(
                                                reportId = rep.id,
                                                content = resp.text,
                                                model = "gemini-3.6-flash"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        isGeneratingReportAi = false
                                        android.util.Log.e("AdvisorScreen", "Error generating wearing AI report", e)
                                        Toast.makeText(context, "生成穿着报告失败: ${e.localizedMessage ?: "未知异常"}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isGeneratingReportAi,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Text(
                                text = if (isGeneratingReportAi) "正在仅基于 WearingReport 字段生成分析..." else "生成穿搭报告字段 AI 分析 (仅用 WearingReport 数据)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!generatedReportAnalysis.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = generatedReportNote ?: "仅基于 WearingReport 字段 + 字典定义",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPurple
                                    )
                                    Text(
                                        text = generatedReportAnalysis ?: "",
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

        // =====================================================================
        // TAB 4 CONTENT: 关联记录诊断 (Junction Field-Only AI)
        // =====================================================================
        if (activeAdvisorTab == "junction") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "关联记录专属 AI 诊断 (Junction Field AI)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "基于衣物×穿搭关联记录字段 (duration 穿着时长 & washStated 洗涤状态) + Junction 字典定义",
                        fontSize = 11.sp,
                        color = Slate500
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        junctions.take(35).forEach { j ->
                            val sel = activeJunctionTarget?.id == j.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) BrandPurple else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (sel) BrandPurple else Slate200),
                                modifier = Modifier.clickable {
                                    activeJunctionTarget = j
                                    generatedJunctionAnalysis = j.aiJunctionAnalysis.ifBlank { null }
                                }
                            ) {
                                Text(
                                    text = "${j.clothesTitle.ifBlank { "衣物" }} × ${j.wearDate.ifBlank { "记录" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sel) Color.White else Slate800,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    activeJunctionTarget?.let { junc ->
                        val hrs = String.format(Locale.US, "%.2f", junc.duration / 3600.0)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${junc.clothesTitle} × ${junc.wearingReportTitle}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "duration: ${junc.duration}s (${hrs}h) | washStated: ${junc.washStated.ifBlank { "need wash" }} | wearDate: ${junc.wearDate}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Slate600
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isGeneratingJunctionAi) return@Button
                                isGeneratingJunctionAi = true
                                val fallbackBuilder = {
                                    "### 🔗 关联记录字段诊断 (${junc.clothesTitle})\n- **穿着时长 (`duration`)**: ${junc.duration} 秒（约 $hrs 小时）\n- **洗涤状态 (`washStated`)**: `${junc.washStated.ifBlank { "need wash" }}`\n- **建议**: ${if (junc.washStated.contains("need", true)) "处于待清洗状态，建议尽快安排洗护。" else "已完成清洗归档。"}"
                                }
                                coroutineScope.launch {
                                    try {
                                        val resp = GeminiAiService.generateJunctionAnalysis(
                                            context = context,
                                            junction = junc,
                                            fallbackGenerator = fallbackBuilder
                                        )
                                        generatedJunctionAnalysis = resp.text
                                        generatedJunctionNote = resp.statusNote
                                        isGeneratingJunctionAi = false
                                        if (resp.text.isNotBlank()) {
                                            viewModel?.addJunctionAiReportVersion(
                                                junctionId = junc.id,
                                                content = resp.text,
                                                model = "gemini-3.6-flash"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        isGeneratingJunctionAi = false
                                        android.util.Log.e("AdvisorScreen", "Error generating junction AI report", e)
                                        Toast.makeText(context, "生成搭配诊断失败: ${e.localizedMessage ?: "未知异常"}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isGeneratingJunctionAi,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Text(
                                text = if (isGeneratingJunctionAi) "正在生成 Junction 关联诊断..." else "生成关联记录 AI 诊断 (Junction 字典)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!generatedJunctionAnalysis.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = generatedJunctionNote ?: "Junction 关联记录字段 AI 诊断",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPurple
                                    )
                                    Text(
                                        text = generatedJunctionAnalysis ?: "",
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

        // =====================================================================
        // TAB 5 CONTENT: 全维度 AI 报告管理归档中心 (All AI Reports Archive)
        // Inside main scrollable column so all reports can be scrolled smoothly!
        // =====================================================================
        if (activeAdvisorTab == "archive") {
            AllAiReportsArchiveView(
                clothes = clothes,
                reports = reports,
                junctions = junctions,
                macroReports = macroReports,
                onUpdateReport = { item, newTitle, newContent ->
                    when (item.type) {
                        "macro" -> viewModel?.updateMacroAiReport(item.versionId, newTitle, newContent)
                        "clothes" -> viewModel?.updateGarmentAiReportVersion(item.sourceEntityId, item.versionId, title = newTitle, content = newContent)
                        "wearing" -> viewModel?.updateWearingAiReportVersion(item.sourceEntityId, item.versionId, title = newTitle, content = newContent)
                        "junction" -> viewModel?.updateJunctionAiReportVersion(item.sourceEntityId, item.versionId, title = newTitle, content = newContent)
                    }
                },
                onDeleteReport = { item ->
                    when (item.type) {
                        "macro" -> viewModel?.deleteMacroAiReport(item.versionId)
                        "clothes" -> viewModel?.deleteGarmentAiReportVersion(item.sourceEntityId, item.versionId)
                        "wearing" -> viewModel?.deleteWearingAiReportVersion(item.sourceEntityId, item.versionId)
                        "junction" -> viewModel?.deleteJunctionAiReportVersion(item.sourceEntityId, item.versionId)
                    }
                },
                onSetPrimary = { item ->
                    when (item.type) {
                        "clothes" -> viewModel?.setPrimaryGarmentAiReportVersion(item.sourceEntityId, item.versionId)
                        "wearing" -> viewModel?.setPrimaryWearingAiReportVersion(item.sourceEntityId, item.versionId)
                        "junction" -> viewModel?.setPrimaryJunctionAiReportVersion(item.sourceEntityId, item.versionId)
                    }
                },
                onSelectGarment = onSelectGarment,
                onSelectReport = onSelectReport
            )
        }
    }

    if (showSavedReportsModal) {
        SavedReportsListModal(
            reports = savedReports,
            onDeleteReport = { id -> onDeleteSavedReport?.invoke(id) },
            onDismiss = { showSavedReportsModal = false }
        )
    }

    if (showApiKeyModal) {
        AlertDialog(
            onDismissRequest = { showApiKeyModal = false },
            title = {
                Text("配置 Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "配置 Gemini API Key 后，四大 AI 模块（全局宏观报告、单品字段报告、穿搭报告分析、关联记录诊断）将实时调用 Gemini 2.5 Flash 并严格注入《字段与数据中英字典》。",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = { customApiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        GeminiAiService.saveCustomApiKey(context, customApiKeyInput)
                        hasApiKey = GeminiAiService.hasValidApiKey(context)
                        showApiKeyModal = false
                        Toast.makeText(context, "已保存 API Key", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyModal = false }) {
                    Text("取消")
                }
            }
        )
    }
}
