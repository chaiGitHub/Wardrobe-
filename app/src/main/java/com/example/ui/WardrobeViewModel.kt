package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.data.repository.WardrobeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.ui.dialogs.SavedAiReportItem
import com.example.ui.dialogs.AiReportVersion
import com.example.ui.dialogs.AiReportVersionHelper
import com.example.ui.dialogs.UniversalAiReportItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AppTab(val title: String, val badge: String = "") {
    CLOSET("Closet"),
    CALENDAR("Wear Calendar"),
    LAUNDRY("Laundry Care"),
    JOURNAL("Wear Journal"),
    DRILLDOWN("Drill-Down Report"),
    ADVISOR("AI Styling"),
    SYNC("Local Storage")
}

data class DrillPreset(
    val id: String,
    val name: String,
    val description: String,
    val groupTiers: List<String>
)

class WardrobeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WardrobeRepository(application)

    val clothes: StateFlow<List<GarmentEntity>> = repository.allClothes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val junctions: StateFlow<List<JunctionEntity>> = repository.allJunctions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTab = MutableStateFlow(AppTab.CLOSET)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _closetFilter = MutableStateFlow("all")
    val closetFilter: StateFlow<String> = _closetFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Drill-down Presets
    val drillPresets = listOf(
        DrillPreset(
            id = "preset-bra-strap",
            name = "Ergonomic & Strap Comfort",
            description = "Deep dive by shoulder strap styling, bust ergonomics, and material feel.",
            groupTiers = listOf("diao dai", "sit breast", "side breast shape")
        ),
        DrillPreset(
            id = "preset-wash-care",
            name = "Wash Care & Laundry Priority",
            description = "Exclusive drill by wash backlog state, clothing category, and fabric material.",
            groupTiers = listOf("wash stated", "clothesType", "material")
        ),
        DrillPreset(
            id = "preset-sensory-heat",
            name = "Sensory Heat & Moisture",
            description = "Drill by midday heat sensitivity, moisture performance, and thickness.",
            groupTiers = listOf("hotAtNoon", "wet", "thickClothes")
        ),
        DrillPreset(
            id = "preset-declutter-audit",
            name = "Declutter & Discard Audit",
            description = "Audit garments flagged for possible decluttering or disposal by category.",
            groupTiers = listOf("mayThrow", "clothesType", "beautiful")
        )
    )

    private val _selectedPreset = MutableStateFlow(drillPresets[0])
    val selectedPreset: StateFlow<DrillPreset> = _selectedPreset.asStateFlow()

    // AI Advisor State
    private val _selectedGarmentForAdvice = MutableStateFlow<GarmentEntity?>(null)
    val selectedGarmentForAdvice: StateFlow<GarmentEntity?> = _selectedGarmentForAdvice.asStateFlow()

    private val _aiAdviceText = MutableStateFlow<String?>(null)
    val aiAdviceText: StateFlow<String?> = _aiAdviceText.asStateFlow()

    private val _isGeneratingAdvice = MutableStateFlow(false)
    val isGeneratingAdvice: StateFlow<Boolean> = _isGeneratingAdvice.asStateFlow()

    // Saved AI Reports archive
    private val _savedAiReports = MutableStateFlow<List<SavedAiReportItem>>(
        listOf(
            SavedAiReportItem(
                id = "rep-seed-1",
                title = "Sensory Diagnosis: Striped Knit Top",
                category = "Sensory Diagnosis",
                dateCreated = "2026-09-24",
                content = "Thermal neutrality is well-maintained during midday. Shoulder strap tension is minimal. Ideal for indoor work and air-conditioned sessions."
            ),
            SavedAiReportItem(
                id = "rep-seed-2",
                title = "Audit: Chiffon Midi Dress",
                category = "Garment Audit",
                dateCreated = "2026-09-23",
                content = "Lightweight chiffon fabric requires cold delicate wash. Excellent aesthetic score with zero sitting pressure."
            )
        )
    )
    val savedAiReports: StateFlow<List<SavedAiReportItem>> = _savedAiReports.asStateFlow()

    // Active session for Junction AI modal
    private val _activeJunctionForAi = MutableStateFlow<JunctionEntity?>(null)
    val activeJunctionForAi: StateFlow<JunctionEntity?> = _activeJunctionForAi.asStateFlow()

    fun selectJunctionForAi(junction: JunctionEntity?) {
        _activeJunctionForAi.value = junction
    }

    fun saveAiReport(title: String, category: String = "Sensory Diagnosis", content: String) {
        val newItem = SavedAiReportItem(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            dateCreated = java.time.LocalDate.now().toString(),
            content = content
        )
        _savedAiReports.value = listOf(newItem) + _savedAiReports.value
    }

    fun deleteSavedAiReport(id: String) {
        _savedAiReports.value = _savedAiReports.value.filter { it.id != id }
    }

    // =========================================================================
    // MULTI-VERSION AI REPORTS MANAGEMENT (Matching Web App wardrobeDataService)
    // =========================================================================

    private val macroPrefs = application.getSharedPreferences("wardrobe_macro_ai_reports_v1", Context.MODE_PRIVATE)

    private fun loadMacroReports(): List<UniversalAiReportItem> {
        val raw = macroPrefs.getString("macro_reports", null)
        val list = mutableListOf<UniversalAiReportItem>()
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        UniversalAiReportItem(
                            id = obj.optString("id", "macro_$i"),
                            type = "macro",
                            typeLabel = "全局穿搭顾问",
                            sourceEntityId = obj.optString("id", "macro_$i"),
                            sourceEntityTitle = obj.optString("occasion", "不限温度/常规天气"),
                            versionId = obj.optString("id", "macro_$i"),
                            title = obj.optString("title", "穿搭体感报告 - 常规天气"),
                            timestamp = obj.optString("timestamp", "2026-09-27T10:37:00"),
                            model = obj.optString("model", "gemini-3.1-flash-lite"),
                            isPrimary = true,
                            content = obj.optString("content", "全局穿搭体感分析：推荐优先考虑轻薄透气、散热优良的上身衣物。"),
                            metaInfo = obj.optString("metaInfo", "全衣橱智能扫描")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (list.isEmpty()) {
            // Seed macro reports matching Screenshot_20261003-122012.png
            list.add(
                UniversalAiReportItem(
                    id = "macro_seed_1",
                    type = "macro",
                    typeLabel = "全局穿搭顾问",
                    sourceEntityId = "macro_seed_1",
                    sourceEntityTitle = "不限温度/常规天气",
                    versionId = "macro_seed_1",
                    title = "穿搭体感报告 - 常规天气 (9/27/2026)",
                    timestamp = "2026-09-27T10:37:00",
                    model = "gemini-3.1-flash-lite",
                    isPrimary = true,
                    content = """【全衣橱宏观穿搭体感诊断】\n\n- **温度与环境适应性**：常规天气下，全衣橱中 70% 的透气棉麻与真丝单品均具备良好的体感宽容度。\n- **中午炎热防御**：中午烈日气温升高时，推荐避开厚重化纤款式，优先选择带肩部支撑的透气单品。\n- **久坐与活动建议**：腹部余量充足、底围舒适的单品更适合全天工作与出行。""",
                    metaInfo = "全衣橱智能扫描 · 关联 12 件单品"
                )
            )
            list.add(
                UniversalAiReportItem(
                    id = "macro_seed_2",
                    type = "macro",
                    typeLabel = "全局穿搭顾问",
                    sourceEntityId = "macro_seed_2",
                    sourceEntityTitle = "不限温度/常规天气",
                    versionId = "macro_seed_2",
                    title = "穿搭体感报告 - 常规天气 (9/27/2026)",
                    timestamp = "2026-09-27T10:01:00",
                    model = "gemini-3.1-flash-lite",
                    isPrimary = true,
                    content = """【早间全套搭配建议】\n\n基于当前衣橱数据，建议优先选择自然垂坠、不易产生静电摩擦的面料组合。""",
                    metaInfo = "全衣橱智能扫描"
                )
            )
        }
        return list
    }

    private fun saveMacroReports(list: List<UniversalAiReportItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("occasion", item.sourceEntityTitle)
                put("title", item.title)
                put("timestamp", item.timestamp)
                put("model", item.model)
                put("content", item.content)
                put("metaInfo", item.metaInfo)
            }
            arr.put(obj)
        }
        macroPrefs.edit().putString("macro_reports", arr.toString()).apply()
    }

    private val _macroAiReports = MutableStateFlow<List<UniversalAiReportItem>>(loadMacroReports())
    val macroAiReports: StateFlow<List<UniversalAiReportItem>> = _macroAiReports.asStateFlow()

    fun addMacroAiReport(title: String, occasion: String, content: String, model: String = "gemini-3.1-flash-lite", metaInfo: String = "") {
        try {
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val id = "macro_${System.currentTimeMillis()}"
            val item = UniversalAiReportItem(
                id = id,
                type = "macro",
                typeLabel = "全局穿搭顾问",
                sourceEntityId = id,
                sourceEntityTitle = occasion.ifBlank { "不限温度/常规天气" },
                versionId = id,
                title = title.ifBlank { "穿搭体感报告 - ${occasion.ifBlank { "常规天气" }} (${java.time.LocalDate.now()})" },
                timestamp = now,
                model = model,
                isPrimary = true,
                content = content,
                metaInfo = metaInfo.ifBlank { "全衣橱智能扫描" }
            )
            val updated = listOf(item) + _macroAiReports.value
            _macroAiReports.value = updated
            saveMacroReports(updated)
        } catch (e: Exception) {
            android.util.Log.e("WardrobeViewModel", "Failed to add macro AI report", e)
        }
    }

    fun updateMacroAiReport(id: String, newTitle: String, newContent: String) {
        val updated = _macroAiReports.value.map {
            if (it.id == id || it.versionId == id) it.copy(title = newTitle, content = newContent) else it
        }
        _macroAiReports.value = updated
        saveMacroReports(updated)
    }

    fun deleteMacroAiReport(id: String) {
        val updated = _macroAiReports.value.filterNot { it.id == id || it.versionId == id }
        _macroAiReports.value = updated
        saveMacroReports(updated)
    }

    // 1. Garment AI Multi-Version Methods
    fun addGarmentAiReportVersion(garmentId: String, content: String, title: String? = null, model: String = "gemini-3.6-flash", isPrimary: Boolean? = null) {
        viewModelScope.launch {
            try {
                val garment = clothes.value.find { it.id == garmentId } ?: return@launch
                val existing = AiReportVersionHelper.parseVersions(garment.aiReports, garment.aiClothesAnalysis, "Initial Report")
                val primaryFlag = isPrimary ?: existing.isEmpty()
                val newVer = AiReportVersion(
                    id = "garment_ai_${System.currentTimeMillis()}_${(1000..9999).random()}",
                    timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                    title = title ?: "Report #${existing.size + 1}",
                    model = model,
                    content = content,
                    isPrimary = primaryFlag
                )
                val updatedList = listOf(newVer) + existing.map { if (primaryFlag) it.copy(isPrimary = false) else it }
                val primary = updatedList.find { it.isPrimary } ?: updatedList.firstOrNull()
                val updatedGarment = garment.copy(
                    aiReports = AiReportVersionHelper.serializeVersions(updatedList),
                    aiClothesAnalysis = primary?.content ?: ""
                )
                repository.updateGarment(updatedGarment)
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to add garment AI report version", e)
            }
        }
    }

    fun updateGarmentAiReportVersion(garmentId: String, versionId: String, title: String? = null, content: String? = null, isPrimary: Boolean? = null) {
        viewModelScope.launch {
            try {
                val garment = clothes.value.find { it.id == garmentId } ?: return@launch
                val existing = AiReportVersionHelper.parseVersions(garment.aiReports, garment.aiClothesAnalysis, "Initial Report")
                val updatedList = existing.map { v ->
                    if (v.id == versionId) {
                        v.copy(
                            title = title ?: v.title,
                            content = content ?: v.content,
                            isPrimary = isPrimary ?: v.isPrimary
                        )
                    } else if (isPrimary == true) {
                        v.copy(isPrimary = false)
                    } else {
                        v
                    }
                }
                val primary = updatedList.find { it.isPrimary } ?: updatedList.firstOrNull()
                val updatedGarment = garment.copy(
                    aiReports = AiReportVersionHelper.serializeVersions(updatedList),
                    aiClothesAnalysis = primary?.content ?: ""
                )
                repository.updateGarment(updatedGarment)
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to update garment AI report version", e)
            }
        }
    }

    fun deleteGarmentAiReportVersion(garmentId: String, versionId: String) {
        viewModelScope.launch {
            try {
                val garment = clothes.value.find { it.id == garmentId } ?: return@launch
                val existing = AiReportVersionHelper.parseVersions(garment.aiReports, garment.aiClothesAnalysis, "Initial Report")
                val remaining = existing.filterNot { it.id == versionId }.toMutableList()
                if (remaining.isNotEmpty() && !remaining.any { it.isPrimary }) {
                    remaining[0] = remaining[0].copy(isPrimary = true)
                }
                val primary = remaining.find { it.isPrimary } ?: remaining.firstOrNull()
                val updatedGarment = garment.copy(
                    aiReports = AiReportVersionHelper.serializeVersions(remaining),
                    aiClothesAnalysis = primary?.content ?: ""
                )
                repository.updateGarment(updatedGarment)
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to delete garment AI report version", e)
            }
        }
    }

    fun setPrimaryGarmentAiReportVersion(garmentId: String, versionId: String) {
        updateGarmentAiReportVersion(garmentId, versionId, isPrimary = true)
    }

    // 2. Wearing Report AI Multi-Version Methods
    fun addWearingAiReportVersion(reportId: String, content: String, title: String? = null, model: String = "gemini-3.6-flash", isPrimary: Boolean? = null) {
        viewModelScope.launch {
            try {
                val rep = reports.value.find { it.id == reportId } ?: return@launch
                val existing = AiReportVersionHelper.parseVersions(rep.aiReports, rep.aiWearingAnalysis, "Initial Report")
                val primaryFlag = isPrimary ?: existing.isEmpty()
                val newVer = AiReportVersion(
                    id = "wearing_ai_${System.currentTimeMillis()}_${(1000..9999).random()}",
                    timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                    title = title ?: "Report #${existing.size + 1}",
                    model = model,
                    content = content,
                    isPrimary = primaryFlag
                )
                val updatedList = listOf(newVer) + existing.map { if (primaryFlag) it.copy(isPrimary = false) else it }
                val primary = updatedList.find { it.isPrimary } ?: updatedList.firstOrNull()
                val updatedReport = rep.copy(
                    aiReports = AiReportVersionHelper.serializeVersions(updatedList),
                    aiWearingAnalysis = primary?.content ?: ""
                )
                repository.insertReport(updatedReport)
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to add wearing AI report version", e)
            }
        }
    }

    fun updateWearingAiReportVersion(reportId: String, versionId: String, title: String? = null, content: String? = null, isPrimary: Boolean? = null) {
        viewModelScope.launch {
            try {
                val rep = reports.value.find { it.id == reportId } ?: return@launch
                val existing = AiReportVersionHelper.parseVersions(rep.aiReports, rep.aiWearingAnalysis, "Initial Report")
                val updatedList = existing.map { v ->
                    if (v.id == versionId) {
                        v.copy(
                            title = title ?: v.title,
                            content = content ?: v.content,
                            isPrimary = isPrimary ?: v.isPrimary
                        )
                    } else if (isPrimary == true) {
                        v.copy(isPrimary = false)
                    } else {
                        v
                    }
                }
                val primary = updatedList.find { it.isPrimary } ?: updatedList.firstOrNull()
                val updatedReport = rep.copy(
                    aiReports = AiReportVersionHelper.serializeVersions(updatedList),
                    aiWearingAnalysis = primary?.content ?: ""
                )
                repository.insertReport(updatedReport)
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to update wearing AI report version", e)
            }
        }
    }

    fun deleteWearingAiReportVersion(reportId: String, versionId: String) {
        viewModelScope.launch {
            try {
                val rep = reports.value.find { it.id == reportId } ?: return@launch
                val existing = AiReportVersionHelper.parseVersions(rep.aiReports, rep.aiWearingAnalysis, "Initial Report")
                val remaining = existing.filterNot { it.id == versionId }.toMutableList()
                if (remaining.isNotEmpty() && !remaining.any { it.isPrimary }) {
                    remaining[0] = remaining[0].copy(isPrimary = true)
                }
                val primary = remaining.find { it.isPrimary } ?: remaining.firstOrNull()
                val updatedReport = rep.copy(
                    aiReports = AiReportVersionHelper.serializeVersions(remaining),
                    aiWearingAnalysis = primary?.content ?: ""
                )
                repository.insertReport(updatedReport)
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to delete wearing AI report version", e)
            }
        }
    }

    fun setPrimaryWearingAiReportVersion(reportId: String, versionId: String) {
        updateWearingAiReportVersion(reportId, versionId, isPrimary = true)
    }

    // 3. Junction AI Multi-Version Methods
    fun addJunctionAiReportVersion(junctionId: String, content: String, title: String? = null, model: String = "gemini-3.6-flash", isPrimary: Boolean? = null) {
        viewModelScope.launch {
            try {
                val junc = junctions.value.find { it.id == junctionId } ?: return@launch
                val existing = AiReportVersionHelper.parseVersions("", junc.aiJunctionAnalysis, "搭配诊断 #1")
                val primaryFlag = isPrimary ?: existing.isEmpty()
                val newVer = AiReportVersion(
                    id = "junction_ai_${System.currentTimeMillis()}_${(1000..9999).random()}",
                    timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                    title = title ?: "搭配诊断 #${existing.size + 1}",
                    model = model,
                    content = content,
                    isPrimary = primaryFlag
                )
                val updatedList = listOf(newVer) + existing.map { if (primaryFlag) it.copy(isPrimary = false) else it }
                val primary = updatedList.find { it.isPrimary } ?: updatedList.firstOrNull()
                val updatedJunction = junc.copy(
                    aiJunctionAnalysis = primary?.content ?: ""
                )
                repository.updateJunction(updatedJunction)
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to add junction AI report version", e)
            }
        }
    }

    fun updateJunctionAiReportVersion(junctionId: String, versionId: String, title: String? = null, content: String? = null, isPrimary: Boolean? = null) {
        viewModelScope.launch {
            val junc = junctions.value.find { it.id == junctionId } ?: return@launch
            val updatedJunction = junc.copy(
                aiJunctionAnalysis = content ?: junc.aiJunctionAnalysis
            )
            repository.updateJunction(updatedJunction)
        }
    }

    fun deleteJunctionAiReportVersion(junctionId: String, versionId: String) {
        viewModelScope.launch {
            val junc = junctions.value.find { it.id == junctionId } ?: return@launch
            val updatedJunction = junc.copy(
                aiJunctionAnalysis = ""
            )
            repository.updateJunction(updatedJunction)
        }
    }

    fun setPrimaryJunctionAiReportVersion(junctionId: String, versionId: String) {
        // Active
    }

    // Restore Progress State
    private val _restoreStatus = MutableStateFlow<String?>(null)
    val restoreStatus: StateFlow<String?> = _restoreStatus.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    // Export Progress State
    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    fun setTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun setClosetFilter(filter: String) {
        _closetFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPreset(preset: DrillPreset) {
        _selectedPreset.value = preset
    }

    fun selectGarmentForAdvice(garment: GarmentEntity?) {
        _selectedGarmentForAdvice.value = garment
        _aiAdviceText.value = null
        if (garment != null) {
            generateAIAdvice(garment)
        }
    }

    // CRUD actions
    fun addOrUpdateGarment(garment: GarmentEntity) {
        viewModelScope.launch {
            if (garment.id.isBlank()) {
                repository.insertGarment(garment.copy(id = UUID.randomUUID().toString()))
            } else {
                repository.insertGarment(garment)
            }
        }
    }

    fun deleteGarment(id: String) {
        viewModelScope.launch {
            repository.deleteGarment(id)
        }
    }

    fun markJunctionWashStatus(junctionId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateWashStatus(junctionId, newStatus)
        }
    }

    fun markGarmentWashed(
        garmentId: String,
        washDate: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
        onComplete: ((String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val msg = repository.markGarmentWashed(garmentId, washDate)
            onComplete?.invoke(msg)
        }
    }

    fun markAllLaundryWashed(
        washDate: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
        onComplete: ((String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val msg = repository.markAllLaundryWashed(washDate)
            onComplete?.invoke(msg)
        }
    }

    fun undoLastWash(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val ok = repository.undoLastWash()
            onComplete?.invoke(ok)
        }
    }

    fun batchUpdateTags(
        garmentIds: List<String>,
        tag: String,
        isAdd: Boolean,
        onComplete: ((Int) -> Unit)? = null
    ) {
        val cleanTag = tag.trim()
        if (cleanTag.isBlank() || garmentIds.isEmpty()) {
            onComplete?.invoke(0)
            return
        }
        viewModelScope.launch {
            var updatedCount = 0
            val currentClothes = clothes.value
            currentClothes.filter { it.id in garmentIds }.forEach { garment ->
                val currentTags = try {
                    val arr = org.json.JSONArray(garment.tags)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val t = arr.optString(i).trim()
                        if (t.isNotBlank()) list.add(t)
                    }
                    list
                } catch (_: Exception) {
                    mutableListOf<String>()
                }

                val hasTag = currentTags.any { it.equals(cleanTag, ignoreCase = true) }
                val changed = if (isAdd) {
                    if (!hasTag) {
                        currentTags.add(cleanTag)
                        true
                    } else false
                } else {
                    if (hasTag) {
                        currentTags.removeAll { it.equals(cleanTag, ignoreCase = true) }
                        true
                    } else false
                }

                if (changed) {
                    val newTagsJson = org.json.JSONArray(currentTags).toString()
                    repository.updateGarment(garment.copy(tags = newTagsJson))
                    updatedCount++
                }
            }
            onComplete?.invoke(updatedCount)
        }
    }

    fun deleteJunction(junctionId: String) {
        viewModelScope.launch {
            repository.deleteJunction(junctionId)
        }
    }

    fun updateReport(report: ReportEntity) {
        viewModelScope.launch {
            repository.insertReport(report)
        }
    }

    fun deduplicateReports(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val repCount = repository.deduplicateReports()
            val juncCount = repository.deduplicateJunctions()
            onComplete?.invoke(repCount + juncCount)
        }
    }

    fun addWearingReport(
        report: ReportEntity,
        selectedGarmentIds: List<Pair<String, Long>>,
        washStatMap: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            val isEditing = report.id.isNotBlank()
            val reportId = if (!isEditing) "rec_report_${System.currentTimeMillis()}" else report.id
            val createdTime = if (report.created.isBlank()) {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
            } else report.created
            val newReport = report.copy(id = reportId, created = createdTime)
            repository.insertReport(newReport)

            if (isEditing) {
                repository.deleteJunctionsForReport(reportId)
            }

            // Insert junctions
            selectedGarmentIds.forEachIndexed { index, (gId, dur) ->
                val garment = clothes.value.find { it.id == gId }
                val jId = "rec_junction_${System.currentTimeMillis()}_$index"
                val washStated = washStatMap[gId] ?: "need wash"
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                repository.insertJunction(
                    JunctionEntity(
                        id = jId,
                        name = 101 + index,
                        wearingReportId = reportId,
                        wearingReportTitle = newReport.name,
                        clothesId = gId,
                        clothesTitle = garment?.name ?: "",
                        clothesType = garment?.clothesType ?: "",
                        duration = dur,
                        washStated = washStated,
                        washDate = if (washStated.equals("Clean", ignoreCase = true) || washStated.equals("washed", ignoreCase = true)) today else "",
                        washedDate = if (washStated.equals("Clean", ignoreCase = true) || washStated.equals("washed", ignoreCase = true)) today else "",
                        wearDate = newReport.wearDate
                    )
                )
            }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
        }
    }

    fun resetDatabaseToSeed(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.resetDatabaseToSeed()
            val msg = res.getOrElse { "Error resetting database: ${it.message}" }
            _exportStatus.value = msg
            onComplete?.invoke(msg)
        }
    }

    fun emptyDatabase(deletePhotos: Boolean = false, onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.emptyDatabase(deletePhotos)
            val msg = res.getOrElse { "Error emptying database: ${it.message}" }
            _exportStatus.value = msg
            onComplete?.invoke(msg)
        }
    }

    // Restore ZIP from user document picker
    fun restoreFromZip(uri: Uri) {
        viewModelScope.launch {
            _isRestoring.value = true
            _restoreStatus.value = "Starting restore..."
            val result = repository.restoreFromZipUri(uri) { message, progress ->
                _restoreStatus.value = "$message ($progress%)"
            }
            if (result.isSuccess) {
                _restoreStatus.value = "Success: ${result.getOrNull()}"
            } else {
                _restoreStatus.value = "Failed: ${result.exceptionOrNull()?.message}"
            }
            _isRestoring.value = false
        }
    }

    // Export Full ZIP Backup directly to user destination (Google Drive or Files)
    fun exportFullBackup(destinationUri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportStatus.value = "Creating full ZIP backup..."
            val result = repository.exportFullBackupZip(destinationUri)
            if (result.isSuccess) {
                _exportStatus.value = "Success: Full wardrobe ZIP backup saved to Google Drive / Storage."
            } else {
                _exportStatus.value = "Failed: ${result.exceptionOrNull()?.message}"
            }
            _isExporting.value = false
        }
    }

    // Export raw JSON
    fun exportDatabaseJson(destinationUri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportStatus.value = "Exporting database.json..."
            val result = repository.exportDatabaseJson(destinationUri)
            if (result.isSuccess) {
                _exportStatus.value = "Success: JSON database exported."
            } else {
                _exportStatus.value = "Failed: ${result.exceptionOrNull()?.message}"
            }
            _isExporting.value = false
        }
    }

    // Share ZIP Backup via System Sheet (User can tap "Save to Drive" or share to Google Drive app)
    fun shareBackupZip(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportStatus.value = "Preparing backup archive..."
            try {
                val tempZip = repository.createTempBackupZipFile()
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempZip
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Wardrobe Full Backup Archive")
                    putExtra(Intent.EXTRA_TEXT, "Wardrobe & Wear Journal Full Backup ZIP archive.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Save to Google Drive / Share Backup").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                _exportStatus.value = "Archive ready! Select Google Drive in the share sheet to upload."
            } catch (e: Exception) {
                _exportStatus.value = "Failed to share archive: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    // AI Advice Generator (Option B: Live Gemini 3.5 Flash + Garment & Wearing Report History)
    fun generateAIAdvice(garment: GarmentEntity) {
        viewModelScope.launch {
            try {
                _isGeneratingAdvice.value = true
                _aiAdviceText.value = null

                val appContext = getApplication<Application>()
                val allJunctions = junctions.value
                val allReports = reports.value
                val allClothes = clothes.value
                val stats = try {
                    com.example.ui.drilldown.DrilldownUtils.calculateGarmentStats(garment.id, allJunctions)
                } catch (e: Exception) {
                    null
                }
                val garmentJunctions = allJunctions.filter { it.clothesId == garment.id }
                val reportsById = allReports.associateBy { it.id }

                val fallbackBuilder = {
                    val totalWears = maxOf(1, garment.wearingFrequency + garment.trackedAppWashes)
                    val cpw = String.format(java.util.Locale.US, "$%.2f", garment.price / totalWears)
                    com.example.util.GeminiAiService.buildDefaultGarmentErgonomicReport(appContext, garment, totalWears, cpw)
                }

                val aiResponse = com.example.util.GeminiAiService.generateGarmentAuditReport(
                    context = appContext,
                    garment = garment,
                    stats = stats,
                    garmentJunctions = garmentJunctions,
                    reportsById = reportsById,
                    allClothes = allClothes,
                    versionNumber = 1,
                    fallbackGenerator = fallbackBuilder
                )

                _aiAdviceText.value = aiResponse.text
            } catch (e: Exception) {
                android.util.Log.e("WardrobeViewModel", "Failed to generate AI advice", e)
                val totalWears = maxOf(1, garment.wearingFrequency + garment.trackedAppWashes)
                val cpw = String.format(java.util.Locale.US, "$%.2f", garment.price / totalWears)
                _aiAdviceText.value = com.example.util.GeminiAiService.buildDefaultGarmentErgonomicReport(getApplication(), garment, totalWears, cpw)
            } finally {
                _isGeneratingAdvice.value = false
            }
        }
    }
}
