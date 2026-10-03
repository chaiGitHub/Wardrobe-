package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.util.ImageUtils
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.dialogs.GoogleDriveBackupModal
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.BrandRose
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(
    clothes: List<GarmentEntity>,
    reports: List<ReportEntity>,
    junctions: List<JunctionEntity>,
    isRestoring: Boolean,
    restoreStatus: String?,
    onRestoreFromZip: (Uri) -> Unit,
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
    val scrollState = rememberScrollState()
    var showGoogleDriveModal by remember { mutableStateOf(false) }
    var showResetConfirmModal by remember { mutableStateOf(false) }
    var showEmptyConfirmModal by remember { mutableStateOf(false) }
    var alsoDeletePhotos by remember { mutableStateOf(false) }
    var photoFilterMode by remember { mutableStateOf("all") } // "all", "missing", "assigned"
    var targetGarmentForPhoto by remember { mutableStateOf<GarmentEntity?>(null) }

    val quickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val target = targetGarmentForPhoto
        if (uri != null && target != null) {
            onUpdateGarmentPhoto?.invoke(target, uri.toString())
            Toast.makeText(context, "Photo assigned to ${target.name}!", Toast.LENGTH_SHORT).show()
        }
        targetGarmentForPhoto = null
    }

    // Junction breakdown metrics matching IMG_9742.png
    val totalJunctions = junctions.size
    val needWashJunctions = remember(junctions) {
        junctions.count { it.washStated.equals("need wash", ignoreCase = true) }
    }
    val cleanJunctions = totalJunctions - needWashJunctions
    val withWashDateJunctions = remember(junctions) {
        junctions.count { it.washDate.isNotBlank() || it.washedDate.isNotBlank() }
    }

    // 1. Launcher for selecting a ZIP file to restore
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onRestoreFromZip(uri)
        }
    }

    // 2. Launcher for saving a Full Backup ZIP (User can choose Google Drive or local storage)
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportFullBackup?.invoke(uri)
        }
    }

    // 3. Launcher for saving raw database.json
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportJson?.invoke(uri)
        }
    }

    val timeStamp = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local Storage & Cloud Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = "Offline-first Room database with Google Drive & ZIP archive sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )
                }
            }
        }

        // Local Storage Status Card (Matching IMG_9742.png)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = BrandEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Local Storage",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
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
                            color = BrandEmerald,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // 3 Collection Cards in a compact 3-column horizontal row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CollectionStatCard(
                        icon = Icons.Default.Checkroom,
                        title = "GARMENTS",
                        count = clothes.size,
                        collectionName = "clothes",
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
                        count = reports.size,
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
                        count = junctions.size,
                        collectionName = "junctions",
                        bgColor = Color(0xFFFAF5FF).copy(alpha = 0.7f),
                        borderColor = Color(0xFFE9D5FF),
                        headerColor = Color(0xFF7E22CE),
                        numberColor = Color(0xFF3B0764),
                        subtitleColor = Color(0xFF9333EA),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Junction Wash Date Breakdown Box (Matching IMG_9742.png)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Junction Wash Date Breakdown",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Clean: $cleanJunctions",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "Need Wash: $needWashJunctions",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "With Wash Date: $withWashDateJunctions",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple
                            )
                        }

                        // Dual Progress Bar (Emerald for Clean, Amber for Need Wash)
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
            }
        }

        // Google Drive Cloud Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Google Drive Cloud Backup & Restore",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Backup and restore full ZIP archives (database.json + all photos)",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Text(
                    text = "On Android, you can save backups directly into your Google Drive account, share via the Google Drive app, or restore existing backups exported from your web app.",
                    fontSize = 12.sp,
                    color = Slate600,
                    lineHeight = 16.sp
                )

                // Action Buttons for Backup & Export
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Save ZIP Backup directly to Google Drive / Files
                    Button(
                        onClick = {
                            createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip")
                        },
                        enabled = !isExporting && !isRestoring,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_zip_button")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Creating Archive...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Save Backup to Google Drive / Device (.ZIP)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 2. One-Tap Share to Google Drive App
                    OutlinedButton(
                        onClick = { onShareZip?.invoke() },
                        enabled = !isExporting && !isRestoring,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_drive_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("1-Tap Share / Upload to Google Drive App", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                    }

                    // Google Drive Modal Dialog Launcher Button (matching webapp)
                    OutlinedButton(
                        onClick = { showGoogleDriveModal = true },
                        enabled = !isExporting && !isRestoring,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_drive_modal_button")
                    ) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Google Drive Sync Settings & Archive Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandEmerald)
                    }

                    // 3. Export raw JSON
                    OutlinedButton(
                        onClick = {
                            createJsonLauncher.launch("wardrobe-database-$timeStamp.json")
                        },
                        enabled = !isExporting && !isRestoring,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_json_button")
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Export Raw Database Only (.JSON)", fontSize = 12.sp, color = Slate700)
                    }

                    // 4. Deduplicate Database Button
                    if (onDeduplicate != null) {
                        OutlinedButton(
                            onClick = onDeduplicate,
                            enabled = !isExporting && !isRestoring,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deduplicate_db_button")
                        ) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Clean Up Duplicate Reports & Junctions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                        }
                    }
                }

                if (exportStatus != null) {
                    val isError = exportStatus.startsWith("Failed") || exportStatus.startsWith("Error")
                    Surface(
                        color = if (isError) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Info else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) Color(0xFFDC2626) else BrandEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = exportStatus,
                                fontSize = 12.sp,
                                color = if (isError) Color(0xFF991B1B) else Color(0xFF065F46),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(color = Slate100)

                // Restore Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Restore from Google Drive / Local ZIP Archive",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Text(
                    text = "Select a Full Backup (.zip) from Google Drive or device Downloads to restore all garments, reports, junctions, and photos.",
                    fontSize = 12.sp,
                    color = Slate600,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = {
                        zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    },
                    enabled = !isRestoring && !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_zip_button")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Restoring Archive...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Select Backup .ZIP from Google Drive / Files", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (restoreStatus != null) {
                    val isError = restoreStatus.startsWith("Failed")
                    Surface(
                        color = if (isError) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Info else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) Color(0xFFDC2626) else BrandEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = restoreStatus,
                                fontSize = 12.sp,
                                color = if (isError) Color(0xFF991B1B) else Color(0xFF065F46),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Database Lifecycle & Reset Management Card (Matching IMG_9743.png)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color(0xFFFEE2E2)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.size(42.dp)
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

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Database Lifecycle & Reset Management",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "ADMIN TOOLS",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Instantly empty the entire local database or restore it back to original seed data.",
                            fontSize = 11.sp,
                            color = Slate500,
                            lineHeight = 15.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFFEF2F2))

                // 1. Reset to Seed Data (Amber Card)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
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
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = { showResetConfirmModal = true },
                            enabled = !isRestoring && !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_to_seed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reset Database to Seed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Empty Database (Red Card)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
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
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = { showEmptyConfirmModal = true },
                            enabled = !isRestoring && !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_database_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Empty Entire Database",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Photo Gallery & Quick Assignment Card (Matching Screenshot_20260930-102348.png)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            val filteredForGallery = remember(clothes, photoFilterMode, restoreStatus) {
                clothes.filter { g ->
                    val hasValidLocal = ImageUtils.resolveFirstValidPhoto(g.pictures, context) != null
                    when (photoFilterMode) {
                        "missing" -> !hasValidLocal
                        "assigned" -> hasValidLocal
                        else -> true
                    }
                }
            }

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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Photo Gallery & Quick Assignment",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Batch-inspect or replace local garment photos (${filteredForGallery.size} shown)",
                                fontSize = 10.sp,
                                color = Slate500
                            )
                        }
                    }

                    // Filter toggle: All | Missing | Assigned
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "all" to "All",
                            "missing" to "Missing",
                            "assigned" to "Has Photo"
                        ).forEach { (mode, label) ->
                            val isSel = photoFilterMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) BrandPurple else Slate100,
                                modifier = Modifier.clickable { photoFilterMode = mode }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 2-Column Compact Grid of Garments for Quick Photo Assignment
                filteredForGallery.take(16).chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPair.forEach { garment ->
                            val resolvedPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
                                ImageUtils.resolveImageModel(it, context)
                            }
                            val hasValidPhoto = resolvedPhotoModel != null

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(74.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (resolvedPhotoModel != null) {
                                            AsyncImage(
                                                model = resolvedPhotoModel,
                                                contentDescription = garment.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.ImageNotSupported,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "No Local Photo",
                                                    fontSize = 9.sp,
                                                    color = Slate500
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = garment.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrandPurpleLight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                targetGarmentForPhoto = garment
                                                quickPhotoLauncher.launch("image/*")
                                            }
                                    ) {
                                        Text(
                                            text = if (hasValidPhoto) "Replace Photo" else "Assign Photo",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (rowPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Text(
                            text = "Your original initial dataset will be loaded into local storage and synced to disk immediately.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
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
                            Text(
                                text = "Tip: You can use \"Save Backup (.ZIP)\" or \"Export Raw Database (.JSON)\" above before emptying.",
                                fontSize = 10.sp,
                                color = Color(0xFFB91C1C)
                            )
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
            onSaveToDrive = {
                createZipLauncher.launch("wardrobe-full-backup-$timeStamp.zip")
            },
            onShareToDrive = {
                onShareZip?.invoke()
            },
            onRestoreFromDrive = {
                zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            }
        )
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = headerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = numberColor
            )
            Text(
                text = collectionName,
                fontSize = 9.sp,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
