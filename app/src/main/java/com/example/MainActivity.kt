package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.GarmentEntity
import com.example.data.model.ReportEntity
import com.example.ui.AppTab
import com.example.ui.WardrobeViewModel
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.AppHeader
import com.example.ui.dialogs.AddGarmentDialog
import com.example.ui.dialogs.AddReportDialog
import com.example.ui.dialogs.FieldDictionaryModal
import com.example.ui.dialogs.FontSizeModal
import com.example.ui.dialogs.NotebookLmPreviewModal
import com.example.ui.dialogs.GarmentDetailDialog
import com.example.ui.dialogs.JunctionAIModal
import com.example.ui.dialogs.SessionDetailModal
import com.example.ui.dialogs.WashGuardDialog
import com.example.ui.screens.AdvisorScreen
import com.example.ui.screens.ClosetScreen
import com.example.ui.screens.DrilldownScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.LaundryScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.util.ThemeUtils

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var fontScale by remember { mutableStateOf(ThemeUtils.getSavedFontScale(context)) }
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = fontScale
                )
            ) {
                MyApplicationTheme {
                    WardrobeMainApp(
                        currentFontScale = fontScale,
                        onFontScaleChange = { newScale ->
                            fontScale = newScale
                            ThemeUtils.saveFontScale(context, newScale)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WardrobeMainApp(
    viewModel: WardrobeViewModel = viewModel(),
    currentFontScale: Float = 1.0f,
    onFontScaleChange: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val clothes by viewModel.clothes.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val junctions by viewModel.junctions.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val closetFilter by viewModel.closetFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedGarmentForAdvice by viewModel.selectedGarmentForAdvice.collectAsState()
    val aiAdviceText by viewModel.aiAdviceText.collectAsState()
    val isGeneratingAdvice by viewModel.isGeneratingAdvice.collectAsState()
    val savedAiReports by viewModel.savedAiReports.collectAsState()
    val activeJunctionForAi by viewModel.activeJunctionForAi.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isAiPaused by viewModel.isAiPaused.collectAsState()

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    val showAddGarmentDialog by viewModel.showAddGarmentDialog.collectAsState()
    val showAddReportDialog by viewModel.showAddReportDialog.collectAsState()
    val editingReport by viewModel.editingReport.collectAsState()
    val selectedGarmentForDetail by viewModel.selectedGarmentForDetail.collectAsState()
    val selectedReportForDetail by viewModel.selectedReportForDetail.collectAsState()
    var showFieldDictionaryModal by remember { mutableStateOf(false) }
    var showNotebookLmModal by remember { mutableStateOf(false) }
    var showFontSizeModal by remember { mutableStateOf(false) }
    var washTargetGarment by remember { mutableStateOf<GarmentEntity?>(null) }

    val dirtyCount = remember(clothes, junctions) {
        val dirtyGarmentIds = junctions
            .filter { it.washStated.equals("need wash", ignoreCase = true) }
            .map { it.clothesId }
            .toSet()
        if (clothes.isNotEmpty()) {
            clothes.count { it.id in dirtyGarmentIds }
        } else {
            dirtyGarmentIds.size
        }
    }

    BackHandler(
        enabled = showAddGarmentDialog ||
                showAddReportDialog ||
                selectedGarmentForDetail != null ||
                selectedReportForDetail != null ||
                activeJunctionForAi != null ||
                washTargetGarment != null ||
                showFieldDictionaryModal ||
                showNotebookLmModal ||
                showFontSizeModal ||
                activeTab != AppTab.CLOSET
    ) {
        when {
            washTargetGarment != null -> washTargetGarment = null
            showAddGarmentDialog -> viewModel.openAddGarment(false)
            showAddReportDialog -> viewModel.closeAddReport()
            selectedGarmentForDetail != null -> viewModel.openGarmentDetail(null)
            selectedReportForDetail != null -> viewModel.openReportDetail(null)
            activeJunctionForAi != null -> viewModel.selectJunctionForAi(null)
            showFieldDictionaryModal -> showFieldDictionaryModal = false
            showNotebookLmModal -> showNotebookLmModal = false
            showFontSizeModal -> showFontSizeModal = false
            activeTab != AppTab.CLOSET -> viewModel.setTab(AppTab.CLOSET)
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                onLogWearClick = {
                    viewModel.openAddReport()
                },
                onAddItemClick = {
                    viewModel.openAddGarment(true)
                },
                onPullFromCloudClick = {
                    isSyncing = true
                    syncMessage = "Pulling from cloud..."
                    viewModel.pullFromCloud { msg ->
                        isSyncing = false
                        syncMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onPushToCloudClick = {
                    isSyncing = true
                    syncMessage = "Pushing to cloud..."
                    viewModel.pushToCloud { msg ->
                        isSyncing = false
                        syncMessage = msg
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                isCloudConnected = true,
                isSyncing = isSyncing,
                syncProgress = 0,
                syncMessage = syncMessage,
                clothesCount = clothes.size,
                reportsCount = reports.size,
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) },
                dirtyCount = dirtyCount,
                onFontSizeClick = { showFontSizeModal = true },
                onNotebookLmClick = { showNotebookLmModal = true },
                isAiPaused = isAiPaused
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                activeTab = activeTab,
                clothesCount = clothes.size,
                dirtyCount = dirtyCount,
                journalCount = reports.size,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (activeTab) {
                AppTab.CLOSET -> ClosetScreen(
                    clothes = clothes,
                    junctions = junctions,
                    selectedFilter = closetFilter,
                    searchQuery = searchQuery,
                    onFilterSelected = { viewModel.setClosetFilter(it) },
                    onSearchChanged = { viewModel.setSearchQuery(it) },
                    onGarmentClick = { viewModel.openGarmentDetail(it) },
                    onAddGarmentClick = { viewModel.openAddGarment(true) },
                    onUpdateGarmentPhoto = { garment, photoUri ->
                        val picturesList = try {
                            val arr = org.json.JSONArray(garment.pictures)
                            val list = mutableListOf<String>()
                            for (i in 0 until arr.length()) list.add(arr.getString(i))
                            if (!list.contains(photoUri)) list.add(0, photoUri)
                            org.json.JSONArray(list).toString()
                        } catch (_: Exception) {
                            org.json.JSONArray(listOf(photoUri)).toString()
                        }
                        viewModel.addOrUpdateGarment(garment.copy(pictures = picturesList))
                    },
                    onBatchUpdateTags = { ids, tag, isAdd, callback ->
                        viewModel.batchUpdateTags(ids, tag, isAdd, callback)
                    }
                )
                AppTab.LAUNDRY -> LaundryScreen(
                    junctions = junctions,
                    clothes = clothes,
                    reports = reports,
                    onMarkWash = { jId, status ->
                        viewModel.markJunctionWashStatus(jId, status)
                    },
                    onMarkGarmentWashed = { gId, dateStr, callback ->
                        viewModel.markGarmentWashed(gId, dateStr, callback)
                    },
                    onMarkAllWashed = { dateStr, callback ->
                        viewModel.markAllLaundryWashed(dateStr, callback)
                    },
                    onUndoLastWash = { callback ->
                        viewModel.undoLastWash(callback)
                    },
                    onSelectGarment = { viewModel.openGarmentDetail(it) }
                )
                AppTab.JOURNAL, AppTab.CALENDAR -> JournalScreen(
                    reports = reports,
                    junctions = junctions,
                    clothes = clothes,
                    onAddReportClick = {
                        viewModel.openAddReport()
                    },
                    onDeleteReportClick = { viewModel.deleteReport(it) },
                    onSelectGarment = { viewModel.openGarmentDetail(it) },
                    onUpdateWashStatus = { jId, status ->
                        viewModel.markJunctionWashStatus(jId, status)
                    },
                    onMarkGarmentWashed = { gId, dateStr ->
                        viewModel.markGarmentWashed(gId, dateStr)
                    },
                    onDeduplicateClick = {
                        viewModel.deduplicateReports { count ->
                            Toast.makeText(context, "Deduplicated $count records", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSaveOrUpdateReport = { report, gPairs, washMap ->
                        viewModel.addWearingReport(report, gPairs, washMap)
                    },
                    onUpdateReportOnly = { viewModel.updateReport(it) },
                    onDeleteJunctionClick = { viewModel.deleteJunction(it) }
                )
                AppTab.DRILLDOWN -> DrilldownScreen(
                    clothes = clothes,
                    reports = reports,
                    junctions = junctions,
                    onSelectGarment = { viewModel.openGarmentDetail(it) },
                    onSelectReport = { viewModel.openReportDetail(it) },
                    onDeleteGarment = { viewModel.deleteGarment(it) },
                    viewModel = viewModel
                )
                AppTab.ADVISOR -> AdvisorScreen(
                    clothes = clothes,
                    junctions = junctions,
                    reports = reports,
                    selectedGarment = selectedGarmentForAdvice,
                    aiAdvice = aiAdviceText,
                    isGenerating = isGeneratingAdvice,
                    onSelectGarment = { viewModel.selectGarmentForAdvice(it) },
                    onOpenGarmentDetail = { viewModel.openGarmentDetail(it) },
                    onSelectReport = { viewModel.openReportDetail(it) },
                    viewModel = viewModel,
                    savedReports = savedAiReports,
                    onSaveReport = { title, cat, content ->
                        viewModel.saveAiReport(title, cat, content)
                    },
                    onDeleteSavedReport = { viewModel.deleteSavedAiReport(it) },
                    onOpenNotebookLmModal = { showNotebookLmModal = true }
                )
                AppTab.SYNC -> SyncScreen(
                    clothes = clothes,
                    reports = reports,
                    junctions = junctions,
                    isRestoring = false,
                    restoreStatus = null,
                    onRestoreFromZip = { uri, clearExisting ->
                        viewModel.restoreFromZip(uri, clearExisting)
                    },
                    isExporting = isExporting,
                    exportStatus = null,
                    onExportFullBackup = { uri ->
                        viewModel.exportFullBackup(uri)
                    },
                    onExportJson = { uri ->
                        viewModel.exportDatabaseJson(uri)
                    },
                    onShareZip = {
                        viewModel.shareBackupZip(context)
                    },
                    onDeduplicate = {
                        viewModel.deduplicateReports { count ->
                            Toast.makeText(context, "Deduplicated $count records", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onResetToSeed = {
                        viewModel.resetDatabaseToSeed { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEmptyDatabase = { deletePhotos ->
                        viewModel.emptyDatabase(deletePhotos) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onUpdateGarmentPhoto = { garment, photoUri ->
                        val picturesList = try {
                            val arr = org.json.JSONArray(garment.pictures)
                            val list = mutableListOf<String>()
                            for (i in 0 until arr.length()) list.add(arr.getString(i))
                            if (!list.contains(photoUri)) list.add(0, photoUri)
                            org.json.JSONArray(list).toString()
                        } catch (_: Exception) {
                            org.json.JSONArray(listOf(photoUri)).toString()
                        }
                        viewModel.addOrUpdateGarment(garment.copy(pictures = picturesList))
                    },
                    onOpenNotebookLmModal = { showNotebookLmModal = true }
                )
            }
        }
    }

    if (showAddGarmentDialog) {
        AddGarmentDialog(
            existingClothes = clothes,
            onDismiss = { viewModel.openAddGarment(false) },
            onSave = { newGarment ->
                viewModel.addOrUpdateGarment(newGarment)
                viewModel.openAddGarment(false)
            }
        )
    }

    selectedGarmentForDetail?.let { garment ->
        val currentGarment = clothes.find { it.id == garment.id } ?: garment
        GarmentDetailDialog(
            garment = currentGarment,
            existingClothes = clothes,
            junctions = junctions,
            reports = reports,
            isAiPaused = isAiPaused,
            onDismiss = { viewModel.openGarmentDetail(null) },
            onDelete = { id ->
                viewModel.deleteGarment(id)
                viewModel.openGarmentDetail(null)
            },
            onSaveGarment = { updated ->
                viewModel.addOrUpdateGarment(updated)
                viewModel.openGarmentDetail(updated)
            },
            onMarkWashNow = { g ->
                washTargetGarment = g
            },
            onSelectReport = { report ->
                viewModel.openReportDetail(report)
            }
        )
    }

    if (showAddReportDialog) {
        val editJunctions = editingReport?.let { rep ->
            junctions.filter { j ->
                j.wearingReportId == rep.id ||
                (rep.name.isNotBlank() && (j.wearingReportTitle.equals(rep.name, ignoreCase = true) || j.wearingReportId.equals(rep.name, ignoreCase = true))) ||
                (rep.id.isNotBlank() && j.wearingReportTitle.equals(rep.id, ignoreCase = true))
            }
        } ?: emptyList()
        AddReportDialog(
            clothes = clothes,
            existingReports = reports,
            initialReport = editingReport,
            initialJunctions = editJunctions,
            allJunctions = junctions,
            onDismiss = {
                viewModel.closeAddReport()
            },
            onSave = { report, gPairs ->
                viewModel.addWearingReport(report, gPairs)
                viewModel.closeAddReport()
            },
            onSaveWithWashMap = { report, gPairs, washMap ->
                viewModel.addWearingReport(report, gPairs, washMap)
                viewModel.closeAddReport()
            }
        )
    }

    selectedReportForDetail?.let { report ->
        SessionDetailModal(
            isOpen = true,
            report = report,
            junctions = junctions,
            clothes = clothes,
            onClose = { viewModel.openReportDetail(null) },
            onSelectGarment = { g -> viewModel.openGarmentDetail(g) },
            onUpdateWashStatus = { jId, status -> viewModel.markJunctionWashStatus(jId, status) },
            onDeleteReport = { rId ->
                viewModel.deleteReport(rId)
                viewModel.openReportDetail(null)
            },
            onEditReport = { r ->
                viewModel.openAddReport(r)
            },
            onUpdateReport = { updated -> viewModel.updateReport(updated) },
            onDeleteJunction = { jId -> viewModel.deleteJunction(jId) },
            onSelectJunctionForAi = { junc -> viewModel.selectJunctionForAi(junc) }
        )
    }

    activeJunctionForAi?.let { junction ->
        val garment = clothes.find { it.id == junction.clothesId }
        val report = reports.find { it.id == junction.wearingReportId }
        JunctionAIModal(
            junction = junction,
            garment = garment,
            report = report,
            onDismiss = { viewModel.selectJunctionForAi(null) },
            onSaveAiReport = { title, content ->
                viewModel.saveAiReport(title, "Junction AI", content)
            }
        )
    }

    washTargetGarment?.let { garment ->
        WashGuardDialog(
            garmentName = garment.name,
            onConfirm = { washDate ->
                viewModel.markGarmentWashed(garment.id, washDate)
                washTargetGarment = null
            },
            onDismiss = { washTargetGarment = null }
        )
    }

    if (showFieldDictionaryModal) {
        FieldDictionaryModal(
            onDismiss = { showFieldDictionaryModal = false }
        )
    }

    if (showNotebookLmModal) {
        NotebookLmPreviewModal(
            clothes = clothes,
            reports = reports,
            junctions = junctions,
            onDismiss = { showNotebookLmModal = false }
        )
    }

    if (showFontSizeModal) {
        FontSizeModal(
            currentScale = currentFontScale,
            onScaleSelected = { scale ->
                onFontScaleChange(scale)
                showFontSizeModal = false
            },
            onDismiss = { showFontSizeModal = false }
        )
    }
}
