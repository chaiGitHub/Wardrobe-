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
import com.example.ui.dialogs.ApkExportModal
import com.example.ui.dialogs.FieldDictionaryModal
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WardrobeMainApp()
            }
        }
    }
}

@Composable
fun WardrobeMainApp(
    viewModel: WardrobeViewModel = viewModel()
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

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    var showAddGarmentDialog by remember { mutableStateOf(false) }
    var showAddReportDialog by remember { mutableStateOf(false) }
    var editingReport by remember { mutableStateOf<ReportEntity?>(null) }
    var selectedGarmentForDetail by remember { mutableStateOf<GarmentEntity?>(null) }
    var selectedReportForDetail by remember { mutableStateOf<ReportEntity?>(null) }
    var showApkExportModal by remember { mutableStateOf(false) }
    var showFieldDictionaryModal by remember { mutableStateOf(false) }
    var washTargetGarment by remember { mutableStateOf<GarmentEntity?>(null) }

    val dirtyCount = remember(junctions) {
        junctions.count { it.washStated.equals("need wash", ignoreCase = true) }
    }

    BackHandler(
        enabled = showAddGarmentDialog ||
                showAddReportDialog ||
                selectedGarmentForDetail != null ||
                selectedReportForDetail != null ||
                activeJunctionForAi != null ||
                washTargetGarment != null ||
                showApkExportModal ||
                showFieldDictionaryModal ||
                activeTab != AppTab.CLOSET
    ) {
        when {
            washTargetGarment != null -> washTargetGarment = null
            showAddGarmentDialog -> showAddGarmentDialog = false
            showAddReportDialog -> {
                showAddReportDialog = false
                editingReport = null
            }
            selectedGarmentForDetail != null -> selectedGarmentForDetail = null
            selectedReportForDetail != null -> selectedReportForDetail = null
            activeJunctionForAi != null -> viewModel.selectJunctionForAi(null)
            showApkExportModal -> showApkExportModal = false
            showFieldDictionaryModal -> showFieldDictionaryModal = false
            activeTab != AppTab.CLOSET -> viewModel.setTab(AppTab.CLOSET)
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                onLogWearClick = {
                    editingReport = null
                    showAddReportDialog = true
                },
                onAddItemClick = {
                    showAddGarmentDialog = true
                },
                onApkDownloadClick = {
                    showApkExportModal = true
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
                dirtyCount = dirtyCount
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
                    onGarmentClick = { selectedGarmentForDetail = it },
                    onAddGarmentClick = { showAddGarmentDialog = true },
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
                    onSelectGarment = { selectedGarmentForDetail = it }
                )
                AppTab.JOURNAL, AppTab.CALENDAR -> JournalScreen(
                    reports = reports,
                    junctions = junctions,
                    clothes = clothes,
                    onAddReportClick = {
                        editingReport = null
                        showAddReportDialog = true
                    },
                    onDeleteReportClick = { viewModel.deleteReport(it) },
                    onSelectGarment = { selectedGarmentForDetail = it },
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
                    onSelectGarment = { selectedGarmentForDetail = it },
                    onSelectReport = { selectedReportForDetail = it },
                    onDeleteGarment = { viewModel.deleteGarment(it) }
                )
                AppTab.ADVISOR -> AdvisorScreen(
                    clothes = clothes,
                    junctions = junctions,
                    reports = reports,
                    selectedGarment = selectedGarmentForAdvice,
                    aiAdvice = aiAdviceText,
                    isGenerating = isGeneratingAdvice,
                    onSelectGarment = { viewModel.selectGarmentForAdvice(it) },
                    onOpenGarmentDetail = { selectedGarmentForDetail = it },
                    onSelectReport = { selectedReportForDetail = it },
                    viewModel = viewModel,
                    savedReports = savedAiReports,
                    onSaveReport = { title, cat, content ->
                        viewModel.saveAiReport(title, cat, content)
                    },
                    onDeleteSavedReport = { viewModel.deleteSavedAiReport(it) }
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
                    }
                )
            }
        }
    }

    if (showAddGarmentDialog) {
        AddGarmentDialog(
            existingClothes = clothes,
            onDismiss = { showAddGarmentDialog = false },
            onSave = { newGarment ->
                viewModel.addOrUpdateGarment(newGarment)
                showAddGarmentDialog = false
            }
        )
    }

    selectedGarmentForDetail?.let { garment ->
        GarmentDetailDialog(
            garment = garment,
            existingClothes = clothes,
            junctions = junctions,
            reports = reports,
            onDismiss = { selectedGarmentForDetail = null },
            onDelete = { id ->
                viewModel.deleteGarment(id)
                selectedGarmentForDetail = null
            },
            onSaveGarment = { updated ->
                viewModel.addOrUpdateGarment(updated)
                selectedGarmentForDetail = null
            },
            onMarkWashNow = { g ->
                washTargetGarment = g
            },
            onSelectReport = { report ->
                selectedReportForDetail = report
            }
        )
    }

    if (showAddReportDialog) {
        AddReportDialog(
            clothes = clothes,
            existingReports = reports,
            initialReport = editingReport,
            onDismiss = {
                showAddReportDialog = false
                editingReport = null
            },
            onSave = { report, gPairs ->
                viewModel.addWearingReport(report, gPairs)
                showAddReportDialog = false
                editingReport = null
            },
            onSaveWithWashMap = { report, gPairs, washMap ->
                viewModel.addWearingReport(report, gPairs, washMap)
                showAddReportDialog = false
                editingReport = null
            }
        )
    }

    selectedReportForDetail?.let { report ->
        SessionDetailModal(
            isOpen = true,
            report = report,
            junctions = junctions,
            clothes = clothes,
            onClose = { selectedReportForDetail = null },
            onSelectGarment = { g -> selectedGarmentForDetail = g },
            onUpdateWashStatus = { jId, status -> viewModel.markJunctionWashStatus(jId, status) },
            onDeleteReport = { rId ->
                viewModel.deleteReport(rId)
                selectedReportForDetail = null
            },
            onEditReport = { r ->
                editingReport = r
                showAddReportDialog = true
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

    if (showApkExportModal) {
        ApkExportModal(
            onDismiss = { showApkExportModal = false }
        )
    }

    if (showFieldDictionaryModal) {
        FieldDictionaryModal(
            onDismiss = { showFieldDictionaryModal = false }
        )
    }
}
