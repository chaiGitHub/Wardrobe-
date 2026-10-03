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

    // AI Advice Generator (Option B: Live Gemini 2.5 Flash + Google Search Grounding + Garment & Wearing Report History)
    fun generateAIAdvice(garment: GarmentEntity) {
        viewModelScope.launch {
            _isGeneratingAdvice.value = true
            _aiAdviceText.value = null

            val appContext = getApplication<Application>()
            val allJunctions = junctions.value
            val allReports = reports.value
            val allClothes = clothes.value
            val stats = com.example.ui.drilldown.DrilldownUtils.calculateGarmentStats(garment.id, allJunctions)
            val garmentJunctions = allJunctions.filter { it.clothesId == garment.id }
            val reportsById = allReports.associateBy { it.id }

            val fallbackBuilder = {
                val totalWears = maxOf(1, garment.wearingFrequency + garment.trackedAppWashes)
                val cpw = String.format(java.util.Locale.US, "%.2f", garment.price / totalWears)

                buildString {
                    append("### 🌟 Overall Assessment: Observed vs. Inference\n\n")
                    append("- **Observed Facts:** **${garment.name}** is a $${garment.price} ${garment.material.ifBlank { "apparel" }} (${garment.clothesType}). Thickness rating: \"${garment.thickClothes.ifBlank { "standard" }}\". Logged wears/washes: **$totalWears** (${garmentJunctions.size} tracked wearing report sessions).\n")
                    append("- **Sensory Profiles:** Rated **${garment.hotAtNoon.ifBlank { "Neutral" }}** at midday; strap style: **\"${garment.diaoDai.ifBlank { "Standard" }}\"**; bust comfort sitting: **${garment.sitBreast.ifBlank { "OK" }}**, standing: **${garment.standBreast.ifBlank { "Standard" }}**.\n")
                    append("- **Declutter Stance:** ${if (garment.mayThrow.contains("throw", ignoreCase = true)) "Flagged for declutter evaluation (${garment.mayThrow})." else "Active wardrobe keeper."}\n\n")
                    append("---\n\n### 🌡️ Comfort & Climate Diagnosis\n")
                    append("- **Thermal Behavior:** ${if (garment.hotAtNoon.contains("Hot", ignoreCase = true)) "High heat accumulation at midday. Pair with cool linen layers or reserve for air-conditioned settings." else "Comfortable thermal dissipation throughout daytime."}\n")
                    append("- **Fabric & Moisture:** Fabric breathability: ${garment.wet.ifBlank { "Dry / breathable" }}. Inner seam tactile contact: ${if (garment.material.contains("chiffon", ignoreCase = true)) "Synthetic fibers may cause micro-friction during walking." else "Soft daily hand-feel."}\n\n")
                    append("---\n\n### 👗 Ergonomic Fit & Support\n")
                    append("- **Chest & Shoulder Strain:** ${garment.diaoDai.ifBlank { "No shoulder tension recorded." }}\n")
                    append("- **Sitting vs. Standing Posture:** ${garment.sitBreast.ifBlank { "Neutral sitting posture." }}\n\n")
                    append("---\n\n### 💰 Cost-Per-Wear (CPW) & Value\n")
                    append("- **Original Price:** $${garment.price}\n")
                    append("- **Total Wear Cycles:** $totalWears\n")
                    append("- **Amortized Cost Per Wear:** **$$cpw / wear**\n")
                    append("- **Value Score:** ${if (garment.price / totalWears < 5.0) "Excellent value amortized." else "Room for higher wear frequency to maximize utility."}\n\n")
                    append("---\n\n### 📋 Recommended Next Actions\n")
                    append("1. **Rotation Cycle:** Wear during morning or indoor air-conditioned activities to mitigate midday heat.\n")
                    append("2. **Care & Softening:** Gentle cycle cold wash with fabric softener to maximize tactile softness.\n")
                    append("3. **Disposition:** ${if (garment.mayThrow.contains("throw", ignoreCase = true)) "Evaluate after 2 more test wears whether sensory comfort matches styling expectations." else "Retain firmly in active seasonal rotation."}")
                }
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
            _isGeneratingAdvice.value = false
        }
    }
}
