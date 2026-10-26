package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.BrandRose
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.util.GeminiAiService
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionDetailModal(
    isOpen: Boolean,
    report: ReportEntity?,
    junctions: List<JunctionEntity>,
    clothes: List<GarmentEntity>,
    onClose: () -> Unit,
    onSelectGarment: (GarmentEntity) -> Unit,
    onUpdateWashStatus: (junctionId: String, status: String) -> Unit,
    onDeleteReport: (String) -> Unit,
    onEditReport: ((ReportEntity) -> Unit)? = null,
    onUpdateReport: ((ReportEntity) -> Unit)? = null,
    onDeleteJunction: ((String) -> Unit)? = null,
    onSelectJunctionForAi: ((JunctionEntity) -> Unit)? = null
) {
    if (!isOpen || report == null) return
    val context = LocalContext.current

    val sessionJunctions = remember(report.id, report.name, junctions) {
        junctions.filter { j ->
            j.wearingReportId == report.id ||
            (report.name.isNotBlank() && (j.wearingReportTitle.equals(report.name, ignoreCase = true) || j.wearingReportId.equals(report.name, ignoreCase = true))) ||
            (report.id.isNotBlank() && j.wearingReportTitle.equals(report.id, ignoreCase = true))
        }
    }

    // Format dates nicely (e.g. "Mon, Sep 21, 2026" and "Sep 21, 2026")
    val formattedLongDate = remember(report.wearDate) {
        formatReportDate(report.wearDate, "EEE, MMM d, yyyy")
    }
    val formattedShortDate = remember(report.wearDate) {
        formatReportDate(report.wearDate, "EEE, MMM d")
    }
    val formattedFooterDate = remember(report.wearDate) {
        formatReportDate(report.wearDate, "MMM d, yyyy")
    }

    val totalDurationFormatted = remember(sessionJunctions) {
        val maxSec = sessionJunctions.maxOfOrNull { it.duration } ?: 0L
        formatHoursCompact(if (maxSec > 0) maxSec else 1200L)
    }
    val hasDirtyLaundry = remember(sessionJunctions) {
        sessionJunctions.any { it.washStated.contains("need wash", ignoreCase = true) || it.washStated.isBlank() }
    }

    // Outfit Photos state (resolved against internal storage photos if present)
    val outfitPhotos = remember(report.id, report.pictures) {
        val list = mutableStateListOf<String>()
        list.addAll(ImageUtils.resolveAllValidPhotos(report.pictures, context))
        list
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val savedPath = ImageUtils.copyUriToInternalStorage(context, uri, "session_${report.id}") ?: uri.toString()
            outfitPhotos.add(savedPath)
        }
        val arr = JSONArray().apply { outfitPhotos.forEach { put(it) } }
        onUpdateReport?.invoke(report.copy(pictures = arr.toString()))
    }

    // Custom Environmental Attributes
    val customAttributes = remember { mutableStateMapOf<String, String>() }
    var showAddAttributeDialog by remember { mutableStateOf(false) }
    var newAttrKey by remember { mutableStateOf("") }
    var newAttrVal by remember { mutableStateOf("") }

    // Collapsible Technical & Cloud Junction Data
    var isTechnicalExpanded by remember { mutableStateOf(false) }

    // Active Junction for AI Diagnosis Modal
    var activeAiJunction by remember { mutableStateOf<JunctionEntity?>(null) }

    val todayFormatted = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
    }

    var pendingWashTarget by remember { mutableStateOf<Pair<JunctionEntity, String>?>(null) }
    var pendingDeleteJunction by remember { mutableStateOf<JunctionEntity?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("session_detail_modal"),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP BAR (IMG_9720: <- | REPORT Mon, Sep 21 / Title | +AI | Edit | Delete | Close)
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Slate700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF3E8FF),
                                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                                    ) {
                                        Text(
                                            text = "REPORT",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF7C3AED),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = formattedShortDate,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate600,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = report.name.ifBlank { "Wearing Report" },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Action Buttons: +AI, Edit, Delete, Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // +AI Purple Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.clickable {
                                    val firstJ = sessionJunctions.firstOrNull() ?: JunctionEntity(
                                        id = "temp_${report.id}",
                                        wearingReportId = report.id,
                                        wearingReportTitle = report.name,
                                        clothesId = clothes.firstOrNull()?.id ?: "",
                                        clothesTitle = clothes.firstOrNull()?.name ?: "Outfit Session",
                                        duration = 1200L,
                                        wearDate = report.wearDate
                                    )
                                    if (onSelectJunctionForAi != null) {
                                        onSelectJunctionForAi(firstJ)
                                    } else {
                                        activeAiJunction = firstJ
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Analysis",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "+AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Edit Circle Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onEditReport?.invoke(report) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Report",
                                        tint = Slate700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Delete Circle Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        onDeleteReport(report.id)
                                        onClose()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Report",
                                        tint = Slate600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Close Circle Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onClose() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Slate600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // MAIN SCROLLABLE CONTENT
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2. DARK PURPLE GRADIENT HERO BANNER (IMG_9720)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1E1B4B),
                                            Color(0xFF4C1D95),
                                            Color(0xFF3B0764)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Top Badges FlowRow
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Date Pill
                                    HeroPillBadge(
                                        icon = Icons.Default.CalendarToday,
                                        text = formattedLongDate
                                    )
                                    // Duration Pill
                                    HeroPillBadge(
                                        icon = Icons.Default.Schedule,
                                        text = totalDurationFormatted
                                    )
                                    // Garment count Pill
                                    HeroPillBadge(
                                        icon = Icons.Default.Checkroom,
                                        text = "${sessionJunctions.size} Garments"
                                    )
                                    // Laundry status Pill
                                    if (hasDirtyLaundry) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFF78350F).copy(alpha = 0.65f),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.WarningAmber,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Laundry Pending",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFDE68A)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Large Report Title
                                Text(
                                    text = report.name.ifBlank { "Wearing Report" },
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 28.sp
                                )

                                // Categories & Tags Pills ([[NOTES]], [[WEARING REPORT]], #journal)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("[[NOTES]]", "[[WEARING REPORT]]").forEach { cat ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF6D28D9).copy(alpha = 0.45f),
                                            border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEDE9FE),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.1f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = Color(0xFFDDD6FE),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "#journal",
                                                fontSize = 11.sp,
                                                color = Color(0xFFDDD6FE),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. OUTFIT PHOTOS CARD (IMG_9720)
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF3E8FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = Color(0xFF9333EA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "OUTFIT PHOTOS (${outfitPhotos.size})",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Visual log for this wearing session",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Slate500
                                        )
                                    }
                                }

                                if (outfitPhotos.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = Slate400,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "No outfit photos captured for this session yet.",
                                                    fontSize = 13.sp,
                                                    color = Slate500
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .clickable { photoPickerLauncher.launch("image/*") }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Upload,
                                                    contentDescription = null,
                                                    tint = Color(0xFF7C3AED),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Add\nPhoto",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF7C3AED)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(outfitPhotos) { url ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(110.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(Slate100)
                                                        .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = ImageUtils.resolveImageModel(url, context),
                                                        contentDescription = "Session photo",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFE11D48))
                                                            .clickable {
                                                                outfitPhotos.remove(url)
                                                                val arr = JSONArray().apply { outfitPhotos.forEach { put(it) } }
                                                                onUpdateReport?.invoke(report.copy(pictures = arr.toString()))
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Delete photo",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                                            Icon(imageVector = Icons.Default.Upload, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add Another Photo", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. GARMENTS IN THIS OUTFIT CARD (IMG_9721)
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF3E8FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Checkroom,
                                            contentDescription = null,
                                            tint = Color(0xFF9333EA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "GARMENTS IN THIS OUTFIT (${sessionJunctions.size})",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Individual items worn, laundry tracking & AI diagnostics",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Slate500
                                        )
                                    }
                                }

                                if (sessionJunctions.isEmpty()) {
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No specific garments linked to this session.",
                                            fontSize = 13.sp,
                                            color = Slate500,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                } else {
                                    sessionJunctions.forEach { junction ->
                                        val garment = clothes.find { it.id == junction.clothesId }
                                            ?: clothes.find { junction.clothesTitle.isNotBlank() && it.name.equals(junction.clothesTitle, ignoreCase = true) }
                                            ?: clothes.find { junction.clothesId.isNotBlank() && it.name.equals(junction.clothesId, ignoreCase = true) }
                                        val photoModel = remember(garment?.pictures) {
                                            ImageUtils.resolveFirstValidPhoto(garment?.pictures, context)?.let {
                                                ImageUtils.resolveImageModel(it, context)
                                            }
                                        }
                                        val isNeedWash = junction.washStated.contains("need wash", ignoreCase = true) || junction.washStated.isBlank()
                                        val jDurationStr = formatHoursCompact(junction.duration)

                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Top Row: Thumbnail + Category/Duration/Status + Name + Delete/Arrow
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background( Color(0xFFF8FAFC))
                                                            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                                                            .clickable { garment?.let { onSelectGarment(it) } },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (photoModel != null) {
                                                            AsyncImage(
                                                                model = photoModel,
                                                                contentDescription = garment?.name,
                                                                contentScale = ContentScale.Fit,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.Checkroom,
                                                                contentDescription = null,
                                                                tint = Color(0xFF9333EA),
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }
                                                    }

                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        // Type Pill + Duration + Status Badge
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    color = Color(0xFFF3E8FF)
                                                                ) {
                                                                    Text(
                                                                        text = (garment?.clothesType ?: junction.clothesType).ifBlank { "DRESS" }.uppercase(),
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = Color(0xFF7C3AED),
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                    )
                                                                }
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Schedule,
                                                                        contentDescription = null,
                                                                        tint = Slate400,
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                    Text(
                                                                        text = jDurationStr,
                                                                        fontSize = 11.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = Slate600
                                                                    )
                                                                }
                                                            }

                                                            // Clean / Need wash pill badge
                                                            Surface(
                                                                shape = RoundedCornerShape(20.dp),
                                                                color = if (isNeedWash) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (isNeedWash) Color(0xFFF59E0B) else Color(0xFF10B981)
                                                                ),
                                                                modifier = Modifier.clickable {
                                                                    if (isNeedWash) {
                                                                        val gName = garment?.name ?: junction.clothesTitle.ifBlank { "Garment" }
                                                                        pendingWashTarget = junction to gName
                                                                    } else {
                                                                        onUpdateWashStatus(junction.id, "need wash")
                                                                    }
                                                                }
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = if (isNeedWash) Icons.Default.WarningAmber else Icons.Default.CheckCircleOutline,
                                                                        contentDescription = null,
                                                                        tint = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857),
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                    Text(
                                                                        text = if (isNeedWash) "Need wash" else "Clean",
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // Garment Title + Trash & Chevron
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = garment?.name ?: junction.clothesTitle.ifBlank { "Garment" },
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Slate900,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clickable { garment?.let { onSelectGarment(it) } }
                                                            )

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.DeleteOutline,
                                                                    contentDescription = "Remove from outfit",
                                                                    tint = Slate400,
                                                                    modifier = Modifier
                                                                        .size(18.dp)
                                                                        .clickable { pendingDeleteJunction = junction }
                                                                )
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                                    contentDescription = "Inspect garment",
                                                                    tint = Slate400,
                                                                    modifier = Modifier
                                                                        .size(20.dp)
                                                                        .clickable { garment?.let { onSelectGarment(it) } }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                HorizontalDivider(color = Slate100)

                                                // Wash Status Action Row (IMG_9721: Cleaned [2026-08-20] OR Wash date: [29 Sep 2026] + Mark Washed)
                                                if (!isNeedWash) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircleOutline,
                                                            contentDescription = null,
                                                            tint = Color(0xFF059669),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "Cleaned",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF059669)
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color(0xFFECFDF5),
                                                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                                                        ) {
                                                            Text(
                                                                text = junction.washedDate.ifBlank {
                                                                    junction.washDate.ifBlank {
                                                                        garment?.lastWashed?.ifBlank { "2026-08-20" } ?: "2026-08-20"
                                                                    }
                                                                },
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF047857),
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "Wash date:",
                                                                fontSize = 12.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Slate500
                                                            )
                                                            Surface(
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = Color.White,
                                                                border = BorderStroke(1.dp, Slate200)
                                                            ) {
                                                                Text(
                                                                    text = todayFormatted,
                                                                    fontSize = 12.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Slate900,
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                                )
                                                            }
                                                        }

                                                        Button(
                                                            onClick = {
                                                                val gName = garment?.name ?: junction.clothesTitle.ifBlank { "Garment" }
                                                                pendingWashTarget = junction to gName
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                            modifier = Modifier.height(34.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.WaterDrop,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Mark Washed",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }

                                                // "AI Outfit Diagnosis" Pill Button (IMG_9721)
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFFAF5FF),
                                                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                                    modifier = Modifier.clickable {
                                                        if (onSelectJunctionForAi != null) {
                                                            onSelectJunctionForAi(junction)
                                                        } else {
                                                            activeAiJunction = junction
                                                        }
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AutoAwesome,
                                                            contentDescription = null,
                                                            tint = Color(0xFF7C3AED),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Text(
                                                            text = "AI Outfit Diagnosis",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF7C3AED)
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

                    // 5. COMFORT & FIT EXPERIENCE CARD (IMG_9722)
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFE4E6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "COMFORT & FIT EXPERIENCE",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Day & night sensory wear feelings",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Slate500
                                        )
                                    }
                                }

                                HorizontalDivider(color = Slate100)

                                // SHOULDER FEEL box
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "SHOULDER FEEL",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate500
                                        )
                                        Text(
                                            text = report.shoulderFeel.ifBlank { "No shoulder strain recorded." },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                // MID BREAST box (Day & Night)
                                DayNightSensoryBox(
                                    title = "MID BREAST",
                                    dayValue = report.midBreast,
                                    nightValue = report.nightMidBreast
                                )

                                // SWEAT & BREATHABILITY box (Day & Night)
                                DayNightSensoryBox(
                                    title = "SWEAT & BREATHABILITY",
                                    dayValue = report.sweat,
                                    nightValue = report.nightSweat
                                )

                                // LEG COMFORT box (Day & Night)
                                DayNightSensoryBox(
                                    title = "LEG COMFORT",
                                    dayValue = report.legFeel,
                                    nightValue = report.nightLegFeel
                                )

                                if (report.reportDetail.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "REPORT DETAIL",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate500
                                            )
                                            Text(
                                                text = report.reportDetail,
                                                fontSize = 13.sp,
                                                color = Slate800,
                                                lineHeight = 19.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. ENVIRONMENTAL & CUSTOM CONTEXT CARD (IMG_9723)
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
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
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFF3E8FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = Color(0xFF9333EA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "ENVIRONMENTAL & CUSTOM\nCONTEXT",
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Slate900,
                                                lineHeight = 17.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Record weather, room temperature, activity or commute details for this wearing session.",
                                                fontSize = 12.sp,
                                                color = Slate500,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { showAddAttributeDialog = true },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "Add\nAttribute",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                if (customAttributes.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No custom attributes added yet. Click \"Add Attribute\" above to record extra specifications.",
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = Slate400,
                                            modifier = Modifier.padding(18.dp)
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        customAttributes.forEach { (k, v) ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFF8FAFC),
                                                border = BorderStroke(1.dp, Slate200),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(k.uppercase(), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Slate500)
                                                        Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                                    }
                                                    IconButton(onClick = { customAttributes.remove(k) }, modifier = Modifier.size(28.dp)) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Slate400, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. TECHNICAL & CLOUD JUNCTION DATA ACCORDION (IMG_9723, IMG_9724, IMG_9725, IMG_9726)
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Accordion Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isTechnicalExpanded = !isTechnicalExpanded }
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = null,
                                            tint = Slate600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TECHNICAL & CLOUD\nJUNCTION DATA",
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Slate900,
                                                lineHeight = 17.sp
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    text = "${sessionJunctions.size} Junction\nRows",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Slate600,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tap to inspect raw junction table records, wash date sync fields, and database schema properties",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Slate500,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isTechnicalExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = Slate500
                                    )
                                }

                                // Expanded Technical Table & Schema Cards
                                AnimatedVisibility(visible = isTechnicalExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp)
                                            .padding(bottom = 18.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        HorizontalDivider(color = Slate100)

                                        // Section 1: REPORT CLOTHES JUNCTION TABLE RECORDS
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "REPORT CLOTHES JUNCTION TABLE RECORDS",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Slate700
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFF3E8FF)
                                            ) {
                                                Text(
                                                    text = "${sessionJunctions.size} rows",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7C3AED),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        // Horizontally Scrollable Junction Table (IMG_9724 & IMG_9725)
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                            ) {
                                                Column {
                                                    // Table Header
                                                    Row(
                                                        modifier = Modifier
                                                            .background(Color(0xFFF8FAFC))
                                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        TableHeaderCell("#", 48.dp)
                                                        TableHeaderCell("CLOTHES(NEW)", 150.dp)
                                                        TableHeaderCell("DURATION", 90.dp)
                                                        TableHeaderCell("WASH STATED", 95.dp)
                                                        TableHeaderCell("WASH DATE", 110.dp)
                                                        TableHeaderCell("WEAR DATE", 95.dp)
                                                        TableHeaderCell("JUNCTION ID", 130.dp)
                                                        TableHeaderCell("ACTIONS", 80.dp)
                                                    }

                                                    HorizontalDivider(color = Slate200)

                                                    sessionJunctions.forEachIndexed { idx, j ->
                                                        val g = clothes.find { it.id == j.clothesId }
                                                            ?: clothes.find { j.clothesTitle.isNotBlank() && it.name.equals(j.clothesTitle, ignoreCase = true) }
                                                            ?: clothes.find { j.clothesId.isNotBlank() && it.name.equals(j.clothesId, ignoreCase = true) }
                                                        val isDirty = j.washStated.contains("need wash", ignoreCase = true) || j.washStated.isBlank()
                                                        val rowNum = if (j.name > 0) j.name else (101 + idx)
                                                        val durFormatted = formatHoursCompact(j.duration)

                                                        Row(
                                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // #
                                                            Text(
                                                                text = rowNum.toString(),
                                                                fontSize = 13.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Slate600,
                                                                modifier = Modifier.width(48.dp)
                                                            )

                                                            // CLOTHES(NEW)
                                                            Column(modifier = Modifier.width(150.dp)) {
                                                                Text(
                                                                    text = g?.name ?: j.clothesTitle.ifBlank { "Garment" },
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Slate900
                                                                )
                                                                Text(
                                                                    text = j.clothesId,
                                                                    fontSize = 10.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = Slate400,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }

                                                            // DURATION
                                                            Column(modifier = Modifier.width(90.dp)) {
                                                                Text(
                                                                    text = durFormatted,
                                                                    fontSize = 12.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = Slate800
                                                                )
                                                                Text(
                                                                    text = "(${j.duration}s)",
                                                                    fontSize = 10.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = Slate400
                                                                )
                                                            }

                                                            // WASH STATED
                                                            Box(modifier = Modifier.width(95.dp)) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                                                ) {
                                                                    Text(
                                                                        text = if (isDirty) "need\nwash" else "Clean",
                                                                        fontSize = 11.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                    )
                                                                }
                                                            }

                                                            // WASH DATE
                                                            Box(modifier = Modifier.width(110.dp)) {
                                                                val wd = j.washedDate.ifBlank { j.washDate }
                                                                if (wd.isNotBlank() && !isDirty) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = Color(0xFFF3E8FF),
                                                                        border = BorderStroke(1.dp, Color(0xFFD8B4FE))
                                                                    ) {
                                                                        Text(
                                                                            text = wd,
                                                                            fontSize = 11.sp,
                                                                            fontFamily = FontFamily.Monospace,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color(0xFF4C1D95),
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                        )
                                                                    }
                                                                } else {
                                                                    Text(
                                                                        text = "empty\n(needs wash)",
                                                                        fontSize = 10.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        fontStyle = FontStyle.Italic,
                                                                        color = Slate400
                                                                    )
                                                                }
                                                            }

                                                            // WEAR DATE
                                                            Text(
                                                                text = j.wearDate.ifBlank { report.wearDate },
                                                                fontSize = 12.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Slate800,
                                                                modifier = Modifier.width(95.dp)
                                                            )

                                                            // JUNCTION ID
                                                            Text(
                                                                text = j.id,
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Slate500,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.width(130.dp)
                                                            )

                                                            // ACTIONS
                                                            Row(
                                                                modifier = Modifier.width(80.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.AutoAwesome,
                                                                    contentDescription = "AI",
                                                                    tint = Color(0xFF9333EA),
                                                                    modifier = Modifier
                                                                        .size(16.dp)
                                                                        .clickable { activeAiJunction = j }
                                                                )
                                                                Icon(
                                                                    imageVector = Icons.Default.DeleteOutline,
                                                                    contentDescription = "Delete",
                                                                    tint = Slate400,
                                                                    modifier = Modifier
                                                                        .size(16.dp)
                                                                        .clickable { pendingDeleteJunction = j }
                                                                )
                                                            }
                                                        }
                                                        HorizontalDivider(color = Slate100)
                                                    }
                                                }
                                            }
                                        }

                                        // Section 2: EXACT DATABASE FIELDS & SCHEMA VALUES (IMG_9726)
                                        Text(
                                            text = "EXACT DATABASE FIELDS & SCHEMA VALUES",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate700
                                        )

                                        SchemaValueBox(label = "NAME", value = report.name.ifBlank { "(empty)" })
                                        SchemaValueBox(label = "WEAR DATE", value = report.wearDate.ifBlank { "(empty)" })
                                        SchemaValueBox(
                                            label = "CREATED",
                                            value = report.created.ifBlank { "${report.wearDate}T04:20:38.578Z" },
                                            mono = true
                                        )
                                        SchemaValueBox(label = "CATEGORIES", value = "[[Notes]], [[Wearing Report]]")
                                        SchemaValueBox(label = "TAGS", value = "#journal")
                                        SchemaValueBox(
                                            label = "PICTURES",
                                            value = if (outfitPhotos.isEmpty()) "(empty)" else "${outfitPhotos.size} attached",
                                            mono = true
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Report ID:\n${report.id}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate400
                                            )
                                            Text(
                                                text = "Records\nSynced: ${sessionJunctions.size}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate400
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // 8. STICKY BOTTOM BAR (IMG_9720–IMG_9726: "Sep 21, 2026" | [Edit Report] | [Close])
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedFooterDate,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate500
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEditReport?.invoke(report) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Slate200),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Edit Report",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            }

                            Button(
                                onClick = onClose,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Close",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Environmental Attribute Dialog
    if (showAddAttributeDialog) {
        AlertDialog(
            onDismissRequest = { showAddAttributeDialog = false },
            title = { Text("Add Session Attribute", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newAttrKey,
                        onValueChange = { newAttrKey = it },
                        label = { Text("Attribute Name (e.g. Weather, Temp)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newAttrVal,
                        onValueChange = { newAttrVal = it },
                        label = { Text("Value (e.g. 28°C Sunny, Office AC)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAttrKey.isNotBlank() && newAttrVal.isNotBlank()) {
                            customAttributes[newAttrKey.trim()] = newAttrVal.trim()
                            newAttrKey = ""
                            newAttrVal = ""
                            showAddAttributeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAttributeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Junction AI Diagnosis Modal
    activeAiJunction?.let { junc ->
        val g = clothes.find { it.id == junc.clothesId }
            ?: clothes.find { junc.clothesTitle.isNotBlank() && it.name.equals(junc.clothesTitle, ignoreCase = true) }
            ?: clothes.find { junc.clothesId.isNotBlank() && it.name.equals(junc.clothesId, ignoreCase = true) }
        JunctionAIModal(
            junction = junc,
            garment = g,
            report = report,
            onDismiss = { activeAiJunction = null }
        )
    }

    // Wash Guard Confirmation Dialog
    pendingWashTarget?.let { (junc, gName) ->
        val washDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        WashGuardDialog(
            garmentName = gName,
            initialWashDate = washDateStr,
            onConfirm = { date ->
                onUpdateWashStatus(junc.id, "Clean")
                pendingWashTarget = null
            },
            onDismiss = { pendingWashTarget = null }
        )
    }

    // Confirmation Dialog for unlinking / deleting Junction
    pendingDeleteJunction?.let { target ->
        val g = clothes.find { it.id == target.clothesId }
            ?: clothes.find { target.clothesTitle.isNotBlank() && it.name.equals(target.clothesTitle, ignoreCase = true) }
            ?: clothes.find { target.clothesId.isNotBlank() && it.name.equals(target.clothesId, ignoreCase = true) }
        val gName = g?.name ?: target.clothesTitle.ifBlank { "Garment" }
        AlertDialog(
            onDismissRequest = { pendingDeleteJunction = null },
            title = {
                Text("解除单品搭配关联？", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "确定要从本次穿搭记录中解除「$gName」的搭配关联吗？\n\n注意：这仅会移除本次穿着中的单品关联记录（Junction 关联记录），不会删除衣物本体或本篇穿搭日记。",
                    fontSize = 13.sp,
                    color = Slate600
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteJunction?.invoke(target.id)
                        pendingDeleteJunction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("确认解除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteJunction = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun HeroPillBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFDDD6FE),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DayNightSensoryBox(
    title: String,
    dayValue: String,
    nightValue: String
) {
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
            Text(
                text = title,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )

            // Day row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Day",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dayValue.ifBlank { "None" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate900,
                    lineHeight = 20.sp
                )
            }

            // Night row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = "Night",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (nightValue.isBlank()) "Night: None" else "Night: $nightValue",
                    fontSize = 13.sp,
                    fontStyle = if (nightValue.isBlank()) FontStyle.Italic else FontStyle.Normal,
                    color = if (nightValue.isBlank()) Slate400 else Slate700
                )
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        color = Slate600,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun SchemaValueBox(label: String, value: String, mono: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate500
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                fontWeight = if (mono) FontWeight.Medium else FontWeight.Bold,
                color = Slate900
            )
        }
    }
}

fun formatReportDate(rawDate: String, pattern: String): String {
    if (rawDate.isBlank()) return "Unspecified date"
    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US)
    )
    for (p in parsers) {
        try {
            val d = p.parse(rawDate.trim())
            if (d != null) {
                return SimpleDateFormat(pattern, Locale.US).format(d)
            }
        } catch (_: Exception) {}
    }
    return rawDate
}

fun formatHoursCompact(seconds: Long): String {
    if (seconds <= 0L) return "0h"
    val hours = seconds / 3600.0
    return if (hours == kotlin.math.floor(hours)) {
        "${hours.toLong()}h"
    } else {
        val formatted = String.format(Locale.US, "%.1f", hours)
        if (formatted.endsWith(".0")) "${formatted.substringBefore(".")}h" else "${formatted}h"
    }
}
