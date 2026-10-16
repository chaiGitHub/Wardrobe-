package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GarmentEntity
import com.example.ui.AppTab
import com.example.ui.WardrobeViewModel
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.AppHeader
import com.example.ui.components.TopNavigationBar
import com.example.ui.dialogs.AddGarmentDialog
import com.example.ui.dialogs.AddReportDialog
import com.example.ui.dialogs.ApkExportModal
import com.example.ui.dialogs.ConfirmModal
import com.example.ui.dialogs.GarmentDetailDialog
import com.example.ui.dialogs.JunctionAIModal
import com.example.ui.dialogs.WashGuardDialog
import com.example.ui.screens.AdvisorScreen
import com.example.ui.screens.ClosetScreen
import com.example.ui.screens.DrilldownScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.LaundryScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.WearCalendar
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ImageUtils
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: WardrobeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                WardrobeApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun WardrobeApp(viewModel: WardrobeViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val clothes by viewModel.clothes.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val junctions by viewModel.junctions.collectAsStateWithLifecycle()

    val closetFilter by viewModel.closetFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()

    val selectedGarmentForAdvice by viewModel.selectedGarmentForAdvice.collectAsStateWithLifecycle()
    val aiAdvice by viewModel.aiAdviceText.collectAsStateWithLifecycle()
    val isGeneratingAdvice by viewModel.isGeneratingAdvice.collectAsStateWithLifecycle()

    val restoreStatus by viewModel.restoreStatus.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val savedAiReports by viewModel.savedAiReports.collectAsStateWithLifecycle()
    val activeJunctionForAi by viewModel.activeJunctionForAi.collectAsStateWithLifecycle()

    // Dialog states
    var showAddGarmentDialog by remember { mutableStateOf(false) }
    var showAddReportDialog by remember { mutableStateOf(false) }
    var showApkModal by remember { mutableStateOf(false) }
    var selectedGarmentDetail by remember { mutableStateOf<GarmentEntity?>(null) }
    var pendingWashGarment by remember { mutableStateOf<GarmentEntity?>(null) }

    // Count of distinct garments needing wash (matching web app dirtyClothesCount)
    val dirtyCount = remember(clothes, junctions) {
        clothes.count { g ->
            junctions.any { j ->
                j.clothesId == g.id && j.washStated.equals("need wash", ignoreCase = true)
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                onLogWearClick = { showAddReportDialog = true },
                onAddItemClick = { showAddGarmentDialog = true },
                onApkDownloadClick = { showApkModal = true },
                onPullFromCloudClick = {
                    viewModel.pullFromCloud { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onPushToCloudClick = {
                    viewModel.pushToCloud { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                isCloudConnected = true,
                isSyncing = isSyncing,
                syncProgress = syncProgress,
                syncMessage = syncMessage,
                clothesCount = clothes.size,
                reportsCount = reports.size,
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) },
                dirtyCount = dirtyCount
            )
        },
        bottomBar = {
            if (!isWideScreen) {
                AppBottomNavigationBar(
                    activeTab = activeTab,
                    clothesCount = clothes.size,
                    dirtyCount = dirtyCount,
                    journalCount = reports.size,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }
        },
        containerColor = Color(0xFFFAFAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AppTab.CLOSET -> {
                    ClosetScreen(
                        clothes = clothes,
                        junctions = junctions,
                        selectedFilter = closetFilter,
                        searchQuery = searchQuery,
                        onFilterSelected = { viewModel.setClosetFilter(it) },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onGarmentClick = { selectedGarmentDetail = it },
                        onAddGarmentClick = { showAddGarmentDialog = true },
                        onUpdateGarmentPhoto = { garment, newPhotoUri ->
                            val savedUri = ImageUtils.copyUriToInternalStorage(
                                context,
                                android.net.Uri.parse(newPhotoUri),
                                garment.id
                            )
                            viewModel.addOrUpdateGarment(
                                garment.copy(pictures = JSONArray().put(savedUri).toString())
                            )
                        },
                        onBatchUpdateTags = { ids, tag, isAdd, cb ->
                            viewModel.batchUpdateTags(ids, tag, isAdd, cb)
                        }
                    )
                }

                AppTab.CALENDAR -> {
                    WearCalendar(
                        reports = reports,
                        junctions = junctions,
                        clothes = clothes,
                        onSelectReport = { viewModel.setTab(AppTab.JOURNAL) },
                        onSelectGarment = { selectedGarmentDetail = it },
                        onOpenLogModal = { showAddReportDialog = true },
                        onMarkGarmentWashed = { gId, wDate ->
                            viewModel.markGarmentWashed(gId, wDate)
                        }
                    )
                }

                AppTab.LAUNDRY -> {
                    LaundryScreen(
                        junctions = junctions,
                        clothes = clothes,
                        reports = reports,
                        onMarkWash = { id, status -> viewModel.markJunctionWashStatus(id, status) },
                        onMarkGarmentWashed = { gId, wDate, cb ->
                            viewModel.markGarmentWashed(gId, wDate, cb)
                        },
                        onMarkAllWashed = { wDate, cb ->
                            viewModel.markAllLaundryWashed(wDate, cb)
                        },
                        onUndoLastWash = { cb ->
                            viewModel.undoLastWash(cb)
                        },
                        onSelectGarment = { selectedGarmentDetail = it }
                    )
                }

                AppTab.JOURNAL -> {
                    JournalScreen(
                        reports = reports,
                        junctions = junctions,
                        clothes = clothes,
                        onAddReportClick = { showAddReportDialog = true },
                        onDeleteReportClick = { viewModel.deleteReport(it) },
                        onSelectGarment = { selectedGarmentDetail = it },
                        onUpdateWashStatus = { id, status -> viewModel.markJunctionWashStatus(id, status) },
                        onMarkGarmentWashed = { gId, wDate -> viewModel.markGarmentWashed(gId, wDate) },
                        onDeduplicateClick = {
                            viewModel.deduplicateReports { totalCleaned ->
                                val msg = if (totalCleaned > 0) {
                                    "已清理 $totalCleaned 条重复穿搭日记与冗余单品关联！"
                                } else {
                                    "未发现重复记录，穿搭数据十分干净！"
                                }
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onSaveOrUpdateReport = { report, links, washMap ->
                            viewModel.addWearingReport(report, links, washMap)
                        },
                        onUpdateReportOnly = { updated -> viewModel.updateReport(updated) },
                        onDeleteJunctionClick = { jId -> viewModel.deleteJunction(jId) }
                    )
                }

                AppTab.DRILLDOWN -> {
                    DrilldownScreen(
                        clothes = clothes,
                        reports = reports,
                        junctions = junctions,
                        onSelectGarment = { selectedGarmentDetail = it },
                        onDeleteGarment = { viewModel.deleteGarment(it) }
                    )
                }

                AppTab.ADVISOR -> {
                    AdvisorScreen(
                        clothes = clothes,
                        junctions = junctions,
                        reports = reports,
                        selectedGarment = selectedGarmentForAdvice,
                        aiAdvice = aiAdvice,
                        isGenerating = isGeneratingAdvice,
                        onSelectGarment = { viewModel.selectGarmentForAdvice(it) },
                        onOpenGarmentDetail = { selectedGarmentDetail = it },
                        savedReports = savedAiReports,
                        onSaveReport = { title, cat, content -> viewModel.saveAiReport(title, cat, content) },
                        onDeleteSavedReport = { viewModel.deleteSavedAiReport(it) },
                        viewModel = viewModel
                    )
                }

                AppTab.SYNC -> {
                    SyncScreen(
                        clothes = clothes,
                        reports = reports,
                        junctions = junctions,
                        restoreStatus = restoreStatus,
                        isRestoring = isRestoring,
                        onRestoreFromZip = { uri, clearExisting -> viewModel.restoreFromZip(uri, clearExisting) },
                        exportStatus = exportStatus,
                        isExporting = isExporting,
                        onExportFullBackup = { viewModel.exportFullBackup(it) },
                        onExportJson = { viewModel.exportDatabaseJson(it) },
                        onShareZip = { viewModel.shareBackupZip(context) },
                        onDeduplicate = { viewModel.deduplicateReports() },
                        onResetToSeed = { viewModel.resetDatabaseToSeed() },
                        onEmptyDatabase = { deletePhotos -> viewModel.emptyDatabase(deletePhotos) },
                        onUpdateGarmentPhoto = { garment, newPhotoUri ->
                            val savedUri = ImageUtils.copyUriToInternalStorage(
                                context,
                                android.net.Uri.parse(newPhotoUri),
                                garment.id
                            )
                            viewModel.addOrUpdateGarment(
                                garment.copy(pictures = JSONArray().put(savedUri).toString())
                            )
                        }
                    )
                }
            }
        }

        // Dialogs
        if (showAddGarmentDialog) {
            AddGarmentDialog(
                existingClothes = clothes,
                onDismiss = { showAddGarmentDialog = false },
                onSave = { garment ->
                    viewModel.addOrUpdateGarment(garment)
                    showAddGarmentDialog = false
                }
            )
        }

        if (showAddReportDialog) {
            AddReportDialog(
                clothes = clothes,
                existingReports = reports,
                onDismiss = { showAddReportDialog = false },
                onSave = { report, links ->
                    viewModel.addWearingReport(report, links)
                    showAddReportDialog = false
                },
                onSaveWithWashMap = { report, links, washMap ->
                    viewModel.addWearingReport(report, links, washMap)
                    showAddReportDialog = false
                }
            )
        }

        selectedGarmentDetail?.let { garment ->
            val currentGarment = clothes.find { it.id == garment.id } ?: garment
            GarmentDetailDialog(
                garment = currentGarment,
                existingClothes = clothes,
                junctions = junctions,
                reports = reports,
                onDismiss = { selectedGarmentDetail = null },
                onDelete = { id ->
                    viewModel.deleteGarment(id)
                    selectedGarmentDetail = null
                },
                onSaveGarment = { updated ->
                    viewModel.addOrUpdateGarment(updated)
                    selectedGarmentDetail = updated
                },
                onMarkWashNow = { g ->
                    pendingWashGarment = g
                }
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
                    viewModel.saveAiReport(title, "Sensory Diagnosis", content)
                }
            )
        }

        // Wash Guard Confirmation Dialog for Garment Detail
        pendingWashGarment?.let { garment ->
            val now = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            WashGuardDialog(
                garmentName = garment.name,
                initialWashDate = now,
                onConfirm = { date ->
                    viewModel.markGarmentWashed(garment.id, date) {
                        android.widget.Toast.makeText(context, "Marked \"${garment.name}\" as washed!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    pendingWashGarment = null
                },
                onDismiss = { pendingWashGarment = null }
            )
        }

        // Dedicated In-App APK Download & Export Modal
        if (showApkModal) {
            ApkExportModal(
                onDismiss = { showApkModal = false }
            )
        }
    }
}
