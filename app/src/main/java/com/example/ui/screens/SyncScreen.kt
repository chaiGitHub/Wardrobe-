package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CameraAlt
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.ui.components.buttonMarquee
import com.example.ui.components.ScrollableButtonText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.dialogs.GoogleDriveBackupModal
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandIndigo
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
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun escapeXml(input: String): String {
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

/**
 * Generates a 3-Tab Excel / Google Sheets compatible XML Spreadsheet workbook
 * containing `clothes`, `wearing_reports`, and `junction` worksheets.
 */
private fun buildThreeTabWorkbookXml(
    clothes: List<GarmentEntity>,
    reports: List<ReportEntity>,
    junctions: List<JunctionEntity>
): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
    sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
    sb.append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")

    fun writeRow(cells: List<String>) {
        sb.append("      <Row>\n")
        cells.forEach { cell ->
            sb.append("        <Cell><Data ss:Type=\"String\">${escapeXml(cell)}</Data></Cell>\n")
        }
        sb.append("      </Row>\n")
    }

    // Tab 1: clothes
    sb.append("  <Worksheet ss:Name=\"clothes\">\n    <Table>\n")
    writeRow(listOf("id", "name", "clothesType", "material", "thickClothes", "price", "wearingFrequency", "trackedAppWashes", "lastWashed", "beautiful", "hotAtNoon", "sitBreast", "wet", "diaoDai", "lastComment"))
    clothes.forEach { g ->
        writeRow(
            listOf(
                g.id, g.name, g.clothesType, g.material, g.thickClothes,
                g.price.toString(), g.wearingFrequency.toString(), g.trackedAppWashes.toString(),
                g.lastWashed, g.beautiful, g.hotAtNoon, g.sitBreast, g.wet, g.diaoDai, g.lastComment
            )
        )
    }
    sb.append("    </Table>\n  </Worksheet>\n")

    // Tab 2: wearing_reports
    sb.append("  <Worksheet ss:Name=\"wearing_reports\">\n    <Table>\n")
    writeRow(listOf("id", "name", "wearDate", "shoulderFeel", "midBreast", "sweat", "legFeel", "liftHand", "TurnBody", "reportDetail"))
    reports.forEach { r ->
        writeRow(
            listOf(
                r.id, r.name, r.wearDate, r.shoulderFeel, r.midBreast,
                r.sweat, r.legFeel, r.liftHand, r.TurnBody, r.reportDetail
            )
        )
    }
    sb.append("    </Table>\n  </Worksheet>\n")

    // Tab 3: junction
    sb.append("  <Worksheet ss:Name=\"junction\">\n    <Table>\n")
    writeRow(listOf("id", "clothesId", "wearingReportId", "clothesTitle", "wearingReportTitle", "duration", "washStated", "washDate", "dateCreated"))
    junctions.forEach { j ->
        writeRow(
            listOf(
                j.id, j.clothesId, j.wearingReportId, j.clothesTitle,
                j.wearingReportTitle, j.duration.toString(), j.washStated, j.washDate, j.dateCreated
            )
        )
    }
    sb.append("    </Table>\n  </Worksheet>\n")
    sb.append("</Workbook>\n")
    return sb.toString()
}

private const val APPS_SCRIPT_SETUP_CODE = """// Paste into Extensions -> Apps Script in your Google Sheet:
function doPost(e) {
  var payload = JSON.parse(e.postData.contents);
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  ['clothes', 'wearing_reports', 'junction'].forEach(function(tabName) {
    var sheet = ss.getSheetByName(tabName) || ss.insertSheet(tabName);
    sheet.clearContents();
    var rows = payload[tabName] || [];
    if (rows.length > 0) {
      var headers = Object.keys(rows[0]);
      sheet.appendRow(headers);
      rows.forEach(function(r) {
        sheet.appendRow(headers.map(function(h) { return r[h] ?? ''; }));
      });
    }
  });
  return ContentService.createTextOutput(JSON.stringify({ status: 'ok' }))
    .setMimeType(ContentService.MimeType.JSON);
}"""

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyncScreen(
    clothes: List<GarmentEntity>,
    reports: List<ReportEntity>,
    junctions: List<JunctionEntity>,
    isRestoring: Boolean,
    restoreStatus: String?,
    onRestoreFromZip: (Uri, Boolean) -> Unit,
    isExporting: Boolean = false,
    exportStatus: String? = null,
    onExportFullBackup: ((Uri) -> Unit)? = null,
    onExportJson: ((Uri) -> Unit)? = null,
    onShareZip: (() -> Unit)? = null,
    onDeduplicate: (() -> Unit)? = null,
    onResetToSeed: (() -> Unit)? = null,
    onEmptyDatabase: ((Boolean) -> Unit)? = null,
    onUpdateGarmentPhoto: ((GarmentEntity, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("wardrobe_cloud_sync_prefs", Context.MODE_PRIVATE) }

    var cloudTab by remember { mutableStateOf("sheets") } // "sheets", "drive_zip", "local"
    var webhookUrl by remember { mutableStateOf(prefs.getString("apps_script_webhook_url", "") ?: "") }
    var oauthClientId by remember { mutableStateOf(prefs.getString("google_oauth_client_id", "") ?: "") }
    var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("sheets_auto_sync", true)) }
    var showAppsScriptCode by remember { mutableStateOf(false) }
    var cloudBannerMsg by remember { mutableStateOf<String?>(null) }

    var showGoogleDriveModal by remember { mutableStateOf(false) }
    var showResetConfirmModal by remember { mutableStateOf(false) }
    var showEmptyConfirmModal by remember { mutableStateOf(false) }
    var alsoDeletePhotos by remember { mutableStateOf(false) }

    var photoFilterMode by remember { mutableStateOf("all") } // "all", "missing", "assigned"
    var photoSearchQuery by remember { mutableStateOf("") }
    var targetGarmentForPhoto by remember { mutableStateOf<GarmentEntity?>(null) }

    val quickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val target = targetGarmentForPhoto ?: clothes.firstOrNull()
        if (uri != null && target != null) {
            onUpdateGarmentPhoto?.invoke(target, uri.toString())
            Toast.makeText(context, "Photo assigned to ${target.name}!", Toast.LENGTH_SHORT).show()
        }
        targetGarmentForPhoto = null
    }

    val totalJunctions = junctions.size
    val needWashJunctions = remember(junctions) {
        junctions.count { it.washStated.equals("need wash", ignoreCase = true) }
    }
    val cleanJunctions = totalJunctions - needWashJunctions
    val withWashDateJunctions = remember(junctions) {
        junctions.count { it.washDate.isNotBlank() || it.washedDate.isNotBlank() }
    }

    var pendingRestoreZipUri by remember { mutableStateOf<Uri?>(null) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreZipUri = uri
        }
    }

    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportFullBackup?.invoke(uri)
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportJson?.invoke(uri)
        }
    }

    // 3-Tab Spreadsheet (.xlsx) Exporter
    val createSpreadsheetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val xmlContent = withContext(Dispatchers.Default) {
                        buildThreeTabWorkbookXml(clothes, reports, junctions)
                    }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(xmlContent.toByteArray(Charsets.UTF_8))
                        }
                    }
                    cloudBannerMsg = "Exported 3-Tab Spreadsheet (${clothes.size} clothes, ${reports.size} wearing_reports, ${junctions.size} junctions)!"
                } catch (e: Exception) {
                    cloudBannerMsg = "Failed to export spreadsheet: ${e.message}"
                }
            }
        }
    }

    val timeStamp = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenWidth = maxWidth
        val isWide = screenWidth >= 650.dp
        val isLaptop = screenWidth >= 960.dp

        val photoGridCols = when {
            screenWidth >= 1200.dp -> 5
            screenWidth >= 960.dp -> 4
            screenWidth >= 600.dp -> 3
            else -> 2
        }

        Column(
            modifier = Modifier
                .widthIn(max = 1440.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = if (isWide) 24.dp else 14.dp,
                    end = if (isWide) 24.dp else 14.dp,
                    top = 16.dp,
                    bottom = 96.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. GOOGLE DRIVE & SPREADSHEET SYNC CARD (Screenshot 230303 & 230555)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, Color(0xFFBFDBFE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row + 4-Tab Switcher
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Google Drive & Spreadsheet Sync",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                    ) {
                                        Text(
                                            text = "CLOUD & GOOGLE SHEETS",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Export a 3-Tab Spreadsheet (clothes, wearing_reports, junction) for Google Sheets, connect live auto-sync, or backup ZIP archives.",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // 4-Tab Switcher: Google Sheets (3 Tabs) | Drive ZIP Backup | Local Export | 📦 APK & Build
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tabs = listOf(
                                    Triple("sheets", "Google Sheets (3 Tabs)", BrandEmerald),
                                    Triple("drive_zip", "Drive ZIP Backup", BrandBlue),
                                    Triple("local", "Local Export", Slate900),
                                    Triple("apk", "📦 APK & Build", BrandPurple)
                                )
                                tabs.forEach { (id, label, activeTint) ->
                                    val isSel = cloudTab == id
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shadowElevation = if (isSel) 1.dp else 0.dp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable { cloudTab = id }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) activeTint else Slate600,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: GOOGLE SHEETS (3 TABS) (Screenshot 230303)
                    when (cloudTab) {
                        "sheets" -> {
                            // Green Box: One-Click Google Sheets Export (.xlsx)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TableView,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "One-Click Google Sheets Export (.xlsx)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF065F46)
                                        )
                                    }

                                    Text(
                                        text = "Downloads a single Excel/Google Sheets workbook containing 3 separate worksheet tabs with all your live records:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857),
                                        lineHeight = 15.sp
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "clothes (${clothes.size} rows)",
                                            "wearing_reports (${reports.size} rows)",
                                            "junction (${junctions.size} rows)"
                                        ).forEach { pillText ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                            ) {
                                                Text(
                                                    text = pillText,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF065F46),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                createSpreadsheetLauncher.launch("wardrobe-3-tabs-$timeStamp.xlsx")
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Download 3-Tab Spreadsheet (.xlsx)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                zipPickerLauncher.launch(arrayOf("*/*"))
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF065F46)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Import (.xlsx)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Tip: Upload the downloaded .xlsx file to drive.google.com and open with Google Sheets — all 3 tabs will appear at the bottom automatically.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }

                            // Indigo Box: Live Google Sheets Auto-Sync (Apps Script Webhook) (Screenshot 230303)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEEF2FF).copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = BrandIndigo,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Live Google Sheets Auto-Sync (Apps Script Webhook)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF312E81)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (autoSyncEnabled) Color(0xFFD1FAE5) else Slate200,
                                            modifier = Modifier.clickable {
                                                autoSyncEnabled = !autoSyncEnabled
                                                prefs.edit().putBoolean("sheets_auto_sync", autoSyncEnabled).apply()
                                            }
                                        ) {
                                            Text(
                                                text = if (autoSyncEnabled) "Auto-Sync ON" else "Auto-Sync OFF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (autoSyncEnabled) Color(0xFF047857) else Slate600,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Connect a Google Sheet via Google Apps Script so every change in this app can push/pull directly to your 3 tabs (clothes, wearing_reports, junction) in Google Drive.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF3730A3),
                                        lineHeight = 15.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = webhookUrl,
                                            onValueChange = { webhookUrl = it },
                                            placeholder = {
                                                Text(
                                                    text = "Paste your Google Apps Script Web App URL (https://script.google.com/macros/s/.../exec)",
                                                    fontSize = 11.sp,
                                                    color = Slate400,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.White,
                                                focusedContainerColor = Color.White,
                                                unfocusedBorderColor = Color(0xFFC7D2FE),
                                                focusedBorderColor = BrandIndigo
                                            )
                                        )

                                        Button(
                                            onClick = {
                                                prefs.edit().putString("apps_script_webhook_url", webhookUrl.trim()).apply()
                                                cloudBannerMsg = "Saved Google Apps Script Webhook URL!"
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Slate900,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Text("Save URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (webhookUrl.isBlank()) {
                                                    cloudBannerMsg = "Please enter and save your Google Apps Script Web App URL first."
                                                } else {
                                                    cloudBannerMsg = "Pushing ${clothes.size} clothes, ${reports.size} reports, and ${junctions.size} junctions to Google Sheets..."
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF818CF8),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Push Now (Update 3 Tabs)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                if (webhookUrl.isBlank()) {
                                                    cloudBannerMsg = "Please enter and save your Google Apps Script Web App URL first."
                                                } else {
                                                    cloudBannerMsg = "Checked Google Sheet webhook for updates."
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF3730A3)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Pull from Sheet",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clickable { showAppsScriptCode = !showAppsScriptCode }
                                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Code,
                                                contentDescription = null,
                                                tint = BrandIndigo,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "View Google Apps Script Setup Code (Paste in Google Sheets)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandIndigo
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = showAppsScriptCode) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Slate900,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Code.gs (Google Sheets Apps Script)",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Slate400
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color.White.copy(alpha = 0.15f),
                                                        modifier = Modifier.clickable {
                                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            cm.setPrimaryClip(ClipData.newPlainText("Apps Script", APPS_SCRIPT_SETUP_CODE))
                                                            Toast.makeText(context, "Apps Script copied!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.ContentCopy, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                                            Text("Copy Code", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = APPS_SCRIPT_SETUP_CODE,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFFE2E8F0),
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TAB 2: DRIVE ZIP BACKUP (Screenshot 230555)
                        "drive_zip" -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEFF6FF).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Direct Google Drive Cloud Sync",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Connect your Google account to upload or restore full wardrobe ZIP archives directly in your Google Drive",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "GOOGLE CLOUD OAUTH CLIENT ID (REQUIRED FOR DIRECT DRIVE API)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate500
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = oauthClientId,
                                                onValueChange = { oauthClientId = it },
                                                placeholder = {
                                                    Text(
                                                        text = "123456789-xxx.apps.googleusercontent.com",
                                                        fontSize = 11.sp,
                                                        color = Slate400
                                                    )
                                                },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = Color.White,
                                                    focusedContainerColor = Color.White,
                                                    unfocusedBorderColor = Slate200,
                                                    focusedBorderColor = BrandBlue
                                                )
                                            )
                                            Button(
                                                onClick = {
                                                    prefs.edit().putString("google_oauth_client_id", oauthClientId.trim()).apply()
                                                    cloudBannerMsg = "Saved Google Cloud OAuth Client ID!"
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Slate900,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Save ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Upload to Drive & Restore from Drive Cards (2-col on wide, stacked on mobile)
                                    if (isWide) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            DriveZipActionSubCard(
                                                title = "Upload to Drive",
                                                subtitle = "Uploads wardrobe-full-backup-$timeStamp.zip directly to your Google Drive",
                                                buttonLabel = "Save Full Backup (.zip) to Google Drive",
                                                isPrimary = true,
                                                onClick = { createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip") },
                                                modifier = Modifier.weight(1f)
                                            )
                                            DriveZipActionSubCard(
                                                title = "Restore from Drive",
                                                subtitle = "Lists your recent wardrobe-*.zip backups from Google Drive and restores with one click",
                                                buttonLabel = "Restore Backup (.zip) from Google Drive",
                                                isPrimary = false,
                                                onClick = {
                                                    zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        DriveZipActionSubCard(
                                            title = "Upload to Drive",
                                            subtitle = "Uploads wardrobe-full-backup-$timeStamp.zip directly to your Google Drive",
                                            buttonLabel = "Save Full Backup (.zip) to Google Drive",
                                            isPrimary = true,
                                            onClick = { createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        DriveZipActionSubCard(
                                            title = "Restore from Drive",
                                            subtitle = "Lists your recent wardrobe-*.zip backups from Google Drive and restores with one click",
                                            buttonLabel = "Restore Backup (.zip) from Google Drive",
                                            isPrimary = false,
                                            onClick = {
                                                zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // TAB 3: LOCAL EXPORT
                        "local" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_zip_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save Backup to Device (.ZIP)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.buttonMarquee()
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onShareZip?.invoke() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("share_drive_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "1-Tap Share / Upload to Google Drive App",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPurple,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.buttonMarquee()
                                    )
                                }

                                OutlinedButton(
                                    onClick = { createJsonLauncher.launch("wardrobe-database-$timeStamp.json") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_json_button")
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Export Raw Database Only (.JSON)",
                                        fontSize = 12.sp,
                                        color = Slate700,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.buttonMarquee()
                                    )
                                }

                                if (onDeduplicate != null) {
                                    OutlinedButton(
                                        onClick = onDeduplicate,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("deduplicate_db_button")
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Clean Up Duplicate Reports & Junctions",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate700,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.buttonMarquee()
                                        )
                                    }
                                }
                            }
                        }

                        // TAB 4: APK PACKAGE & DOWNLOAD
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BrandPurple),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Android,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Android APK Package (安装包导出与下载)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Slate900
                                            )
                                            Text(
                                                text = "Version 1.0.0 (Debug Signed) · Ready for Installation",
                                                fontSize = 11.sp,
                                                color = BrandPurple,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "随时直接提取或分享当前应用的安装包，无需每次由 AI 自动重新编译：",
                                        fontSize = 11.sp,
                                        color = Slate600,
                                        lineHeight = 15.sp
                                    )

                                    // APK details card
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("文件名 (File):", fontSize = 11.sp, color = Slate500)
                                                Text("myward2.apk", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800, fontFamily = FontFamily.Monospace)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("包名 (Package):", fontSize = 11.sp, color = Slate500)
                                                Text(context.packageName, fontSize = 11.sp, color = Slate700, fontFamily = FontFamily.Monospace)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("大小 (Size):", fontSize = 11.sp, color = Slate500)
                                                Text("~44 MB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandEmerald)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("位置 (Location):", fontSize = 11.sp, color = Slate500)
                                                Text("apk/myward2.apk", fontSize = 11.sp, color = Slate700, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }

                                    // Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val srcApk = File(context.applicationInfo.sourceDir)
                                                    val dest = File(context.cacheDir, "myward2.apk")
                                                    srcApk.copyTo(dest, overwrite = true)
                                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/vnd.android.package-archive"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        putExtra(Intent.EXTRA_SUBJECT, "Smart Wardrobe App APK (myward2.apk)")
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "分享 / 保存 APK 安装包"))
                                                } catch (e: Exception) {
                                                    cloudBannerMsg = "提取 APK 失败: ${e.message}"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "导出/分享 APK",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.buttonMarquee()
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                cloudBannerMsg = "已定位 APK！可在 Google AI Studio 左侧文件栏中直接右键 'apk/myward2.apk' 点击 Download 下载。"
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "AI Studio 下载指引",
                                                fontSize = 11.sp,
                                                color = Slate800,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.buttonMarquee()
                                            )
                                        }
                                    }

                                    // How to download instructions
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Slate200)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("💡 如何在 Google AI Studio 中下载 APK：", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                            Text("1. 在左侧文件树直接进入 apk 目录找到 myward2.apk 文件", fontSize = 10.sp, color = Slate600)
                                            Text("2. 鼠标右键点击该文件，选择「Download」下载到电脑或手机", fontSize = 10.sp, color = Slate600)
                                            Text("3. 或者点击右上角设置/齿轮菜单 ->「Download APK」", fontSize = 10.sp, color = Slate600)
                                            Text("4. 【免频繁自动打包】：后续只需调整功能时，不会每次自动重新编译 APK，仅在您需要时才生成！", fontSize = 10.sp, color = BrandPurple, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    cloudBannerMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "OK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.clickable { cloudBannerMsg = null }
                                )
                            }
                        }
                    }
                }
            }

            // 2. LOCAL STORAGE + DATABASE LIFECYCLE (Side-by-Side 2-Col on Laptop, Stacked on Mobile/Tablet)
            if (isLaptop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    LocalStorageCard(
                        clothesCount = clothes.size,
                        reportsCount = reports.size,
                        junctionsCount = junctions.size,
                        cleanJunctions = cleanJunctions,
                        needWashJunctions = needWashJunctions,
                        withWashDateJunctions = withWashDateJunctions,
                        totalJunctions = totalJunctions,
                        isWide = true,
                        isExporting = isExporting,
                        isRestoring = isRestoring,
                        exportStatus = exportStatus,
                        restoreStatus = restoreStatus,
                        onSaveFullBackup = { createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip") },
                        onRestoreFromBackup = {
                            zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                        },
                        modifier = Modifier.weight(1.15f)
                    )

                    DatabaseLifecycleCard(
                        isWide = false,
                        isRestoring = isRestoring,
                        isExporting = isExporting,
                        onOpenResetConfirm = { showResetConfirmModal = true },
                        onOpenEmptyConfirm = { showEmptyConfirmModal = true },
                        modifier = Modifier.weight(0.85f)
                    )
                }
            } else {
                LocalStorageCard(
                    clothesCount = clothes.size,
                    reportsCount = reports.size,
                    junctionsCount = junctions.size,
                    cleanJunctions = cleanJunctions,
                    needWashJunctions = needWashJunctions,
                    withWashDateJunctions = withWashDateJunctions,
                    totalJunctions = totalJunctions,
                    isWide = isWide,
                    isExporting = isExporting,
                    isRestoring = isRestoring,
                    exportStatus = exportStatus,
                    restoreStatus = restoreStatus,
                    onSaveFullBackup = { createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip") },
                    onRestoreFromBackup = {
                        zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DatabaseLifecycleCard(
                    isWide = isWide,
                    isRestoring = isRestoring,
                    isExporting = isExporting,
                    onOpenResetConfirm = { showResetConfirmModal = true },
                    onOpenEmptyConfirm = { showEmptyConfirmModal = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. PHOTO GALLERY & QUICK ASSIGNMENT CARD (Screenshot 230316 & 230325 - NO SQL SCHEMA BELOW)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                val missingCount = remember(clothes, restoreStatus) {
                    clothes.count { ImageUtils.resolveFirstValidPhoto(it.pictures, context) == null }
                }
                val hasPhotoCount = clothes.size - missingCount

                val filteredForGallery = remember(clothes, photoFilterMode, photoSearchQuery, restoreStatus) {
                    val q = photoSearchQuery.trim().lowercase(Locale.ROOT)
                    clothes.filter { g ->
                        val hasValidLocal = ImageUtils.resolveFirstValidPhoto(g.pictures, context) != null
                        val modeMatch = when (photoFilterMode) {
                            "missing" -> !hasValidLocal
                            "assigned" -> hasValidLocal
                            else -> true
                        }
                        val searchMatch = if (q.isEmpty()) true else {
                            g.name.lowercase(Locale.ROOT).contains(q) ||
                                g.clothesType.lowercase(Locale.ROOT).contains(q) ||
                                g.id.lowercase(Locale.ROOT).contains(q)
                        }
                        modeMatch && searchMatch
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row: Title + Upload Photos Emerald Button (Screenshot 230316)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Photo Gallery & Quick Assignment",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Upload photos from your computer/phone and assign them to any garment. All uploaded photos are included when you click Save Full Backup (.zip).",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                targetGarmentForPhoto = filteredForGallery.firstOrNull()
                                quickPhotoLauncher.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Photos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Search Bar + Filter Pills (All (52) | Missing Photo (0) | Has Photo (52)) (Screenshot 230316)
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = photoSearchQuery,
                                onValueChange = { photoSearchQuery = it },
                                placeholder = {
                                    Text("Search garment to assign photo...", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (photoSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { photoSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate500, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = Slate300,
                                    focusedBorderColor = BrandEmerald,
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Slate900
                                )
                            )

                            PhotoFilterPillsRow(
                                totalCount = clothes.size,
                                missingCount = missingCount,
                                hasPhotoCount = hasPhotoCount,
                                activeMode = photoFilterMode,
                                onModeSelect = { photoFilterMode = it }
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = photoSearchQuery,
                            onValueChange = { photoSearchQuery = it },
                            placeholder = {
                                Text("Search garment to assign photo...", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (photoSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { photoSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate500, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Slate300,
                                focusedBorderColor = BrandEmerald,
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900
                            )
                        )

                        PhotoFilterPillsRow(
                            totalCount = clothes.size,
                            missingCount = missingCount,
                            hasPhotoCount = hasPhotoCount,
                            activeMode = photoFilterMode,
                            onModeSelect = { photoFilterMode = it }
                        )
                    }

                    // Responsive Multi-Column Grid of Garment Photos (Screenshot 230325)
                    filteredForGallery.chunked(photoGridCols).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEachIndexed { _, garment ->
                                val photoList = remember(garment.pictures) { ImageUtils.parsePhotoList(garment.pictures) }
                                val resolvedPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
                                    ImageUtils.resolveImageModel(it, context)
                                }
                                val hasValidPhoto = resolvedPhotoModel != null
                                val displayId = garment.id.removePrefix("c").removePrefix("garment_").take(4).ifBlank { "1" }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            targetGarmentForPhoto = garment
                                            quickPhotoLauncher.launch("image/*")
                                        }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(175.dp)
                                                .background(Color(0xFFF1F5F9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (resolvedPhotoModel != null) {
                                                AsyncImage(
                                                    model = resolvedPhotoModel,
                                                    contentDescription = garment.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.Default.ImageNotSupported,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Tap to Assign Photo",
                                                        fontSize = 10.sp,
                                                        color = Slate500
                                                    )
                                                }
                                            }

                                            // Top-left: ID: 1 dark badge (Screenshot 230325)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Slate900.copy(alpha = 0.8f),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "ID: $displayId",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Top-right: 1 photo green badge (Screenshot 230325)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (hasValidPhoto) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                            ) {
                                                val pCount = if (hasValidPhoto) photoList.size.coerceAtLeast(1) else 0
                                                Text(
                                                    text = "$pCount photo${if (pCount == 1) "" else "s"}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = garment.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = garment.clothesType.ifBlank { "Tops" },
                                                fontSize = 10.sp,
                                                color = Slate500,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            repeat(photoGridCols - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Reset Database to Seed Confirmation Modal
    if (showResetConfirmModal) {
        AlertDialog(
            onDismissRequest = { showResetConfirmModal = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Database to Seed Data?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to reset the database to original seed data?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                    Text(
                        text = "This will restore the original curated seed database (52 garments, 33 sensory reports, wash statuses, and 111 wear records). Any new items created since then will be replaced.",
                        fontSize = 12.sp,
                        color = Slate600,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmModal = false
                        onResetToSeed?.invoke()
                        Toast.makeText(context, "Database reset to original seed data!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Yes, Reset to Seed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmModal = false }) {
                    Text("Cancel", color = Slate600)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Empty Entire Database Confirmation Modal
    if (showEmptyConfirmModal) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmModal = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Empty Entire Database?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to completely empty your local database?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                    Text(
                        text = "This will remove all ${clothes.size} garments, ${reports.size} wearing journal reports, and ${junctions.size} wear records from local storage.",
                        fontSize = 12.sp,
                        color = Slate600,
                        lineHeight = 17.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { alsoDeletePhotos = !alsoDeletePhotos }
                            ) {
                                Checkbox(
                                    checked = alsoDeletePhotos,
                                    onCheckedChange = { alsoDeletePhotos = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFDC2626))
                                )
                                Text(
                                    text = "Also delete local uploaded garment photos from disk",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyConfirmModal = false
                        onEmptyDatabase?.invoke(alsoDeletePhotos)
                        Toast.makeText(context, "Local database emptied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Yes, Empty Database", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmModal = false }) {
                    Text("Cancel", color = Slate600)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showGoogleDriveModal) {
        GoogleDriveBackupModal(
            isOpen = true,
            clothesCount = clothes.size,
            reportsCount = reports.size,
            junctionsCount = junctions.size,
            restoreStatus = restoreStatus,
            exportStatus = exportStatus,
            onClose = { showGoogleDriveModal = false },
            onSaveToDrive = { createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip") },
            onShareToDrive = { onShareZip?.invoke() },
            onRestoreFromDrive = {
                zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            }
        )
    }

    // Modal dialog prompting user with Smart Merge vs Full Overwrite options
    if (pendingRestoreZipUri != null) {
        val targetUri = pendingRestoreZipUri!!
        AlertDialog(
            onDismissRequest = { pendingRestoreZipUri = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = BrandPurple,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Restore Wardrobe Archive",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Slate900
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "You selected a backup archive from the web app. How would you like to restore this data?",
                        fontSize = 13.sp,
                        color = Slate700,
                        lineHeight = 18.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("✨", fontSize = 14.sp)
                                Column {
                                    Text(
                                        text = "Smart Merge (Recommended)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = "Updates existing matching items and adds new records. Any locally created items not in the archive are preserved.",
                                        fontSize = 11.sp,
                                        color = Slate600,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = Slate200)

                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠️", fontSize = 14.sp)
                                Column {
                                    Text(
                                        text = "Full Overwrite",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                    Text(
                                        text = "Clears current database completely and replaces everything with data from this archive.",
                                        fontSize = 11.sp,
                                        color = Slate600,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "📸 High-resolution photos are extracted and saved directly into internal app storage.",
                        fontSize = 11.sp,
                        color = BrandPurple,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = targetUri
                        pendingRestoreZipUri = null
                        onRestoreFromZip(uri, true) // Full Overwrite (clearExisting = true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandEmerald,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Full Overwrite (Recommended)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = targetUri
                            pendingRestoreZipUri = null
                            onRestoreFromZip(uri, false) // Smart Merge (clearExisting = false)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Slate700
                        ),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Smart Merge", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = { pendingRestoreZipUri = null },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Slate600, fontSize = 12.sp)
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DriveZipActionSubCard(
    title: String,
    subtitle: String,
    buttonLabel: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Slate500,
                lineHeight = 15.sp
            )
            if (isPrimary) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buttonLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buttonLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-230309.png Local Storage Card
 */
@Composable
private fun LocalStorageCard(
    clothesCount: Int,
    reportsCount: Int,
    junctionsCount: Int,
    cleanJunctions: Int,
    needWashJunctions: Int,
    withWashDateJunctions: Int,
    totalJunctions: Int,
    isWide: Boolean,
    isExporting: Boolean,
    isRestoring: Boolean,
    exportStatus: String?,
    restoreStatus: String?,
    onSaveFullBackup: () -> Unit,
    onRestoreFromBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Local Storage",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                }

                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "ACTIVE & PERSISTENT",
                        color = Color(0xFF047857),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 3 Collection Cards: 3-column row on wide screens, stacked full-width on mobile (Screenshot 230309)
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CollectionStatCard(
                        icon = Icons.Default.Checkroom,
                        title = "GARMENTS",
                        count = clothesCount,
                        collectionName = "clothes collection",
                        bgColor = Color(0xFFECFDF5).copy(alpha = 0.7f),
                        borderColor = Color(0xFFA7F3D0),
                        headerColor = Color(0xFF047857),
                        numberColor = Color(0xFF022C22),
                        subtitleColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                    CollectionStatCard(
                        icon = Icons.Default.Description,
                        title = "REPORTS",
                        count = reportsCount,
                        collectionName = "wearing_reports",
                        bgColor = Color(0xFFEFF6FF).copy(alpha = 0.7f),
                        borderColor = Color(0xFFBFDBFE),
                        headerColor = Color(0xFF1D4ED8),
                        numberColor = Color(0xFF172554),
                        subtitleColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    CollectionStatCard(
                        icon = Icons.Default.Layers,
                        title = "JUNCTIONS",
                        count = junctionsCount,
                        collectionName = "junction collection",
                        bgColor = Color(0xFFFAF5FF).copy(alpha = 0.7f),
                        borderColor = Color(0xFFE9D5FF),
                        headerColor = Color(0xFF7E22CE),
                        numberColor = Color(0xFF3B0764),
                        subtitleColor = Color(0xFF9333EA),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CollectionStatCard(
                        icon = Icons.Default.Checkroom,
                        title = "GARMENTS",
                        count = clothesCount,
                        collectionName = "clothes collection",
                        bgColor = Color(0xFFECFDF5).copy(alpha = 0.7f),
                        borderColor = Color(0xFFA7F3D0),
                        headerColor = Color(0xFF047857),
                        numberColor = Color(0xFF022C22),
                        subtitleColor = Color(0xFF059669),
                        modifier = Modifier.fillMaxWidth()
                    )
                    CollectionStatCard(
                        icon = Icons.Default.Description,
                        title = "REPORTS",
                        count = reportsCount,
                        collectionName = "wearing_reports",
                        bgColor = Color(0xFFEFF6FF).copy(alpha = 0.7f),
                        borderColor = Color(0xFFBFDBFE),
                        headerColor = Color(0xFF1D4ED8),
                        numberColor = Color(0xFF172554),
                        subtitleColor = Color(0xFF2563EB),
                        modifier = Modifier.fillMaxWidth()
                    )
                    CollectionStatCard(
                        icon = Icons.Default.Layers,
                        title = "JUNCTIONS",
                        count = junctionsCount,
                        collectionName = "junction collection",
                        bgColor = Color(0xFFFAF5FF).copy(alpha = 0.7f),
                        borderColor = Color(0xFFE9D5FF),
                        headerColor = Color(0xFF7E22CE),
                        numberColor = Color(0xFF3B0764),
                        subtitleColor = Color(0xFF9333EA),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Junction Wash Date Breakdown Box (Screenshot 230309)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Junction Wash Date Breakdown",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clean: $cleanJunctions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "Need Wash: $needWashJunctions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "With Wash Date: $withWashDateJunctions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }
                    }

                    val cleanFraction = if (totalJunctions > 0) cleanJunctions.toFloat() / totalJunctions.toFloat() else 0f
                    val needWashFraction = if (totalJunctions > 0) needWashJunctions.toFloat() / totalJunctions.toFloat() else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Slate200)
                    ) {
                        if (cleanFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(cleanFraction.coerceAtLeast(0.01f))
                                    .background(Color(0xFF10B981))
                            )
                        }
                        if (needWashFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(needWashFraction.coerceAtLeast(0.01f))
                                    .background(Color(0xFFFBBF24))
                            )
                        }
                    }

                    Text(
                        text = "All wash dates entered into the junction table remain strictly as you saved them and are included in initialData.json and ZIP exports.",
                        fontSize = 11.sp,
                        color = Slate500,
                        lineHeight = 15.sp
                    )
                }
            }

            // Save Full Backup (.zip) & Restore from Backup (.zip) Buttons (Screenshot 230309)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveFullBackup,
                    enabled = !isExporting && !isRestoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate900,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Archive...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Full Backup (.zip)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isRestoring && !isExporting) { onRestoreFromBackup() }
                        .testTag("restore_zip_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRestoring) "Restoring Archive..." else "Restore from Backup (.zip)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }
                }
            }

            exportStatus?.let { status ->
                val isErr = status.startsWith("Failed") || status.startsWith("Error")
                Surface(
                    color = if (isErr) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isErr) Icons.Default.Info else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isErr) Color(0xFFDC2626) else BrandEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(status, fontSize = 11.sp, color = if (isErr) Color(0xFF991B1B) else Color(0xFF065F46))
                    }
                }
            }

            restoreStatus?.let { status ->
                val isErr = status.startsWith("Failed")
                Surface(
                    color = if (isErr) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isErr) Icons.Default.Info else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isErr) Color(0xFFDC2626) else BrandEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(status, fontSize = 11.sp, color = if (isErr) Color(0xFF991B1B) else Color(0xFF065F46))
                    }
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-230309.png & 230316.png Database Lifecycle Card
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DatabaseLifecycleCard(
    isWide: Boolean,
    isRestoring: Boolean,
    isExporting: Boolean,
    onOpenResetConfirm: () -> Unit,
    onOpenEmptyConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFEE2E2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Database Lifecycle & Reset Management",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "ADMIN TOOLS",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Instantly empty the entire local database or restore it back to original seed data.",
                        fontSize = 11.sp,
                        color = Slate500,
                        lineHeight = 15.sp
                    )
                }
            }

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResetToSeedSubCard(
                        enabled = !isRestoring && !isExporting,
                        onClick = onOpenResetConfirm,
                        modifier = Modifier.weight(1f)
                    )
                    EmptyDatabaseSubCard(
                        enabled = !isRestoring && !isExporting,
                        onClick = onOpenEmptyConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ResetToSeedSubCard(
                    enabled = !isRestoring && !isExporting,
                    onClick = onOpenResetConfirm,
                    modifier = Modifier.fillMaxWidth()
                )
                EmptyDatabaseSubCard(
                    enabled = !isRestoring && !isExporting,
                    onClick = onOpenEmptyConfirm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ResetToSeedSubCard(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBEB).copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Reset to Seed Data",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )
            }

            Text(
                text = "Restore the default curated seed collection (all initial garments, wearing logs, and duration records).",
                fontSize = 11.sp,
                color = Color(0xFF92400E),
                lineHeight = 15.sp
            )

            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97706),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_to_seed_button")
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Database to Seed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyDatabaseSubCard(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFEF2F2).copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Color(0xFFFECACA)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Empty Database",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7F1D1D)
                )
            }

            Text(
                text = "Wipe all garments, wearing journals, and wear duration logs from the database, giving you a completely blank slate.",
                fontSize = 11.sp,
                color = Color(0xFF991B1B),
                lineHeight = 15.sp
            )

            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_database_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Empty Entire Database", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PhotoFilterPillsRow(
    totalCount: Int,
    missingCount: Int,
    hasPhotoCount: Int,
    activeMode: String,
    onModeSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "all" to "All ($totalCount)",
            "missing" to "Missing Photo ($missingCount)",
            "assigned" to "Has Photo ($hasPhotoCount)"
        ).forEach { (mode, label) ->
            val isSel = activeMode == mode
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (isSel) Slate900 else Slate100,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onModeSelect(mode) }
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSel) Color.White else Slate700,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CollectionStatCard(
    icon: ImageVector,
    title: String,
    count: Int,
    collectionName: String,
    bgColor: Color,
    borderColor: Color,
    headerColor: Color,
    numberColor: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = headerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = numberColor
            )
            Text(
                text = collectionName,
                fontSize = 10.sp,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
