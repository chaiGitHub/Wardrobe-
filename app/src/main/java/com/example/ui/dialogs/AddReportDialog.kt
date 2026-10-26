package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
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
import com.example.util.ImageUtils
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OutfitGarmentEntry(
    val garment: GarmentEntity,
    val hours: Int = 4,
    val minutes: Int = 0,
    val washStated: String = "need wash" // "need wash" or "Clean"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddReportDialog(
    clothes: List<GarmentEntity>,
    existingReports: List<ReportEntity> = emptyList(),
    initialReport: ReportEntity? = null,
    initialJunctions: List<JunctionEntity> = emptyList(),
    allJunctions: List<JunctionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ReportEntity, List<Pair<String, Long>>) -> Unit,
    onSaveWithWashMap: ((ReportEntity, List<Pair<String, Long>>, Map<String, String>) -> Unit)? = null
) {
    val context = LocalContext.current
    val dictionaryService = remember { FieldDictionaryService(context) }
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayDisplay = remember { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()) }

    var wearDate by remember {
        mutableStateOf(initialReport?.wearDate?.ifBlank { todayIso } ?: todayIso)
    }
    var isAutoNameMode by remember {
        mutableStateOf(initialReport == null)
    }
    var title by remember {
        mutableStateOf(initialReport?.name?.ifBlank { "$todayIso 穿搭记录" } ?: "$todayIso 穿搭记录")
    }
    var shoulderFeel by remember {
        mutableStateOf(initialReport?.shoulderFeel ?: "relaxed and comfortable")
    }
    var midBreast by remember {
        mutableStateOf(initialReport?.midBreast ?: "good support, no digging")
    }
    var nightMidBreast by remember {
        mutableStateOf(initialReport?.nightMidBreast ?: "")
    }
    var sweat by remember {
        mutableStateOf(initialReport?.sweat ?: "low / dry")
    }
    var nightSweat by remember {
        mutableStateOf(initialReport?.nightSweat ?: "")
    }
    var legFeel by remember {
        mutableStateOf(initialReport?.legFeel ?: "free movement")
    }
    var nightLegFeel by remember {
        mutableStateOf(initialReport?.nightLegFeel ?: "")
    }
    var reportDetail by remember {
        mutableStateOf(initialReport?.reportDetail ?: "")
    }

    // Selected garments in outfit
    val chosenGarments = remember(initialReport?.id, initialJunctions) {
        val list = mutableStateListOf<OutfitGarmentEntry>()
        if (initialReport != null && initialJunctions.isNotEmpty()) {
            initialJunctions.forEach { j ->
                val g = clothes.find { it.id == j.clothesId }
                    ?: clothes.find { j.clothesTitle.isNotBlank() && it.name.equals(j.clothesTitle, ignoreCase = true) }
                    ?: clothes.find { j.clothesId.isNotBlank() && it.name.equals(j.clothesId, ignoreCase = true) }
                    ?: GarmentEntity(
                        id = j.clothesId.ifBlank { j.clothesTitle },
                        name = j.clothesTitle.ifBlank { j.clothesId.ifBlank { "Garment" } },
                        clothesType = j.clothesType.ifBlank { "Dress" }
                    )
                val totalMin = (j.duration / 60).toInt()
                val hr = totalMin / 60
                val min = totalMin % 60
                list.add(
                    OutfitGarmentEntry(
                        garment = g,
                        hours = hr,
                        minutes = min,
                        washStated = j.washStated.ifBlank { "need wash" }
                    )
                )
            }
        }
        list
    }

    // Closet picker toggle & filters
    var isClosetPickerOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var closetSearchQuery by remember { mutableStateOf("") }

    // Outfit of the day photos
    val photoUrls = remember {
        val list = mutableStateListOf<String>()
        if (initialReport != null) {
            list.addAll(ImageUtils.parsePhotoList(initialReport.pictures))
        }
        list
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            photoUrls.add(uri.toString())
        }
    }

    // Previous entries modal state
    var activePreviousPicker by remember {
        mutableStateOf<Pair<String, (String) -> Unit>?>(null)
    }

    fun getPreviousEntries(fieldKey: String): List<PreviousEntry> {
        val countMap = mutableMapOf<String, Int>()
        val sourcesMap = mutableMapOf<String, MutableSet<String>>()

        // Build reportId -> garment names map from allJunctions and initialJunctions
        val reportGarmentsMap = mutableMapOf<String, MutableSet<String>>()
        val combinedJunctions = (allJunctions + initialJunctions).distinctBy { it.id.ifBlank { "${it.wearingReportId}_${it.clothesId}" } }
        combinedJunctions.forEach { j ->
            val gName = j.clothesTitle.ifBlank {
                clothes.find { it.id == j.clothesId }?.name ?: ""
            }.trim()
            if (gName.isNotBlank() && j.wearingReportId.isNotBlank()) {
                reportGarmentsMap.getOrPut(j.wearingReportId) { mutableSetOf() }.add(gName)
            }
        }

        existingReports.forEach { r ->
            val v = when (fieldKey) {
                "SHOULDER FEEL" -> r.shoulderFeel
                "MID BREAST" -> r.midBreast
                "NIGHT MID BREAST" -> r.nightMidBreast
                "SWEAT" -> r.sweat
                "NIGHT SWEAT" -> r.nightSweat
                "LEG FEEL" -> r.legFeel
                "NIGHT LEG FEEL" -> r.nightLegFeel
                "REPORT DETAIL" -> r.reportDetail
                else -> {
                    try {
                        org.json.JSONObject(r.customFields).optString(fieldKey, "")
                    } catch (_: Exception) { "" }
                }
            }.trim()
            if (v.isNotBlank()) {
                countMap[v] = (countMap[v] ?: 0) + 1
                val gNames = reportGarmentsMap[r.id] ?: emptySet()
                if (gNames.isNotEmpty()) {
                    sourcesMap.getOrPut(v) { mutableSetOf() }.addAll(gNames)
                }
            }
        }

        // Fallback suggestions if history is empty
        when (fieldKey) {
            "SHOULDER FEEL" -> if (countMap.isEmpty()) listOf(
                "relaxed and comfortable",
                "the not diao dai make the shoulder no so irrate and pain",
                "strap digs slightly after 3 hours",
                "no shoulder tension"
            ).forEach { countMap[it] = 1 }
            "MID BREAST" -> if (countMap.isEmpty()) listOf(
                "good support, no digging",
                "mid breast is ok sizd when pair with [[small bra]] when sit",
                "comfortable when seated",
                "slightly tight at chest"
            ).forEach { countMap[it] = 1 }
            "NIGHT MID BREAST" -> if (countMap.isEmpty()) listOf(
                "comfortable for evening",
                "no pressure at night",
                "relaxed fit"
            ).forEach { countMap[it] = 1 }
            "SWEAT" -> if (countMap.isEmpty()) listOf(
                "low / dry",
                "thick clothes also more hot and swear",
                "breathable and cool",
                "slight sweat at noon"
            ).forEach { countMap[it] = 1 }
            "NIGHT SWEAT" -> if (countMap.isEmpty()) listOf(
                "dry and pleasant",
                "cool in aircon",
                "warm at night"
            ).forEach { countMap[it] = 1 }
            "LEG FEEL" -> if (countMap.isEmpty()) listOf(
                "free movement",
                "like dress not pant, skirt quite middle thick , no so cold leg",
                "soft drape on legs"
            ).forEach { countMap[it] = 1 }
            "NIGHT LEG FEEL" -> if (countMap.isEmpty()) listOf(
                "airy and easy",
                "warm enough for night"
            ).forEach { countMap[it] = 1 }
            "REPORT DETAIL" -> if (countMap.isEmpty()) listOf(
                "Comfortable indoor office and walk session",
                "Worn during sunny afternoon commute",
                "Relaxed home wear"
            ).forEach { countMap[it] = 1 }
        }

        val dictFields = try { dictionaryService.getFields() } catch (_: Exception) { emptyList() }
        val normKey = fieldKey.trim().lowercase(Locale.ROOT)
        val dictValues = dictFields.find {
            it.name.trim().lowercase(Locale.ROOT) == normKey ||
            it.name.replace(" ", "").lowercase(Locale.ROOT) == normKey.replace(" ", "")
        }?.typicalValues ?: emptyList()

        return countMap.entries.map { e ->
            val typical = dictValues.find { it.valueEn.equals(e.key, ignoreCase = true) }
            val labelCn = typical?.valueCn?.ifBlank { null }?.takeIf { it != e.key }
            val meaningCn = typical?.meaningCn?.ifBlank { null } ?: "真实穿搭历史记录值: \"${e.key}\""
            val autoExtracted = Regex("\\[\\[(.*?)\\]\\]").findAll(e.key).map { it.groupValues[1] }.toList()
            val sources = (sourcesMap[e.key]?.toList() ?: emptyList()).ifEmpty { autoExtracted }
            PreviousEntry(
                value = e.key,
                count = e.value,
                labelCn = labelCn,
                meaningCn = meaningCn,
                sources = sources
            )
        }.sortedByDescending { it.count }
    }

    val categories = remember(clothes) {
        val dynamic = clothes.map { it.clothesType.trim() }.filter { it.isNotBlank() }.distinct()
        val defaults = listOf("Dress", "bra", "Tshirt", "Pant", "skirt", "sweater", "jacket")
        listOf("All") + (defaults + dynamic).distinct()
    }

    val filteredCloset = remember(clothes, selectedCategory, closetSearchQuery) {
        clothes.filter { g ->
            val matchesCat = selectedCategory == "All" || g.clothesType.equals(selectedCategory, ignoreCase = true)
            val q = closetSearchQuery.trim().lowercase(Locale.ROOT)
            val matchesQuery = q.isEmpty() ||
                g.name.lowercase(Locale.ROOT).contains(q) ||
                g.material.lowercase(Locale.ROOT).contains(q) ||
                g.clothesType.lowercase(Locale.ROOT).contains(q)
            matchesCat && matchesQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("add_report_dialog"),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header (matches IMG_9686: Back button + sparkle purple circle + "Log Wearing Report & Session" + X)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (initialReport != null) "Edit Wearing Report & Session" else "Log Wearing Report & Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            fontSize = 17.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Main Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. WEAR DATE & NAME (REPORT TITLE WITH AUTO-NAME)
                    val buildAutoTitle = { dateStr: String, entries: List<OutfitGarmentEntry> ->
                        val garmentPart = if (entries.isNotEmpty()) {
                            entries.take(3).joinToString(" + ") { it.garment.name }
                        } else {
                            "穿搭记录"
                        }
                        "${dateStr.ifBlank { todayIso }} · $garmentPart"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WEAR DATE (穿着日期)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate100,
                                modifier = Modifier.clickable {
                                    wearDate = todayIso
                                    if (isAutoNameMode) {
                                        title = buildAutoTitle(todayIso, chosenGarments)
                                    }
                                }
                            ) {
                                Text(
                                    text = "设为今天 ($todayIso)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = wearDate,
                            onValueChange = {
                                wearDate = it
                                if (isAutoNameMode) {
                                    title = buildAutoTitle(it, chosenGarments)
                                }
                            },
                            placeholder = { Text(todayDisplay, color = Slate400, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_wear_date"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9333EA),
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NAME (REPORT TITLE · 自动命名)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF3E8FF),
                                border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                modifier = Modifier
                                    .clickable {
                                        isAutoNameMode = true
                                        title = buildAutoTitle(wearDate, chosenGarments)
                                    }
                                    .testTag("btn_auto_name_report")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "✨ 自动生成名称",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C3AED)
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                isAutoNameMode = false
                            },
                            placeholder = { Text("e.g. $todayIso · 穿搭记录", color = Slate400, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_report_title"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF9333EA),
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    // 2. OUTFIT CLOTHES & INDIVIDUAL WASH STATUS CARD (IMG_9686, IMG_9688, IMG_9689)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFFAF5FF).copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "OUTFIT CLOTHES & INDIVIDUAL WASH STATUS (${chosenGarments.size})",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4C1D95),
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                text = "Each garment in your report tracks its own duration and wash state.",
                                fontSize = 12.sp,
                                color = Color(0xFF9333EA)
                            )

                            // "+ Add Garment to Outfit" button
                            Button(
                                onClick = { isClosetPickerOpen = !isClosetPickerOpen },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("btn_toggle_closet_picker"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Garment to Outfit",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }

                            // Expandable Closet Picker (IMG_9688)
                            if (isClosetPickerOpen) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("closet_picker_panel"),
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 2.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Search & Pick from Closet (${clothes.size} total)",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = Slate900
                                            )
                                            IconButton(
                                                onClick = { isClosetPickerOpen = false },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Close Picker",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Category filter chips (FlowRow like IMG_9688)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            categories.forEach { cat ->
                                                val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) Color(0xFF7C3AED) else Color(0xFFF1F5F9),
                                                    modifier = Modifier.clickable { selectedCategory = cat }
                                                ) {
                                                    Text(
                                                        text = cat,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else Slate700,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Search input
                                        OutlinedTextField(
                                            value = closetSearchQuery,
                                            onValueChange = { closetSearchQuery = it },
                                            placeholder = {
                                                Text(
                                                    "Type garment name or material...",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF475569)
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = null,
                                                    tint = Color(0xFF475569),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = if (closetSearchQuery.isNotEmpty()) {
                                                {
                                                    IconButton(onClick = { closetSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            tint = Slate500,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            } else null,
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF9333EA),
                                                unfocusedBorderColor = Slate300,
                                                focusedContainerColor = Color(0xFFF8FAFC),
                                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                                focusedTextColor = Slate900,
                                                unfocusedTextColor = Slate900
                                            )
                                        )

                                        // Garment items list
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 280.dp)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            filteredCloset.forEach { garment ->
                                                val alreadyAdded = chosenGarments.any { it.garment.id == garment.id }
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (!alreadyAdded) {
                                                                chosenGarments.add(OutfitGarmentEntry(garment = garment))
                                                                if (isAutoNameMode) {
                                                                    title = buildAutoTitle(wearDate, chosenGarments)
                                                                }
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = garment.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                color = Slate900
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = listOfNotNull(
                                                                    garment.clothesType.takeIf { it.isNotBlank() },
                                                                    garment.material.takeIf { it.isNotBlank() }
                                                                ).joinToString(" · ").ifBlank { "Apparel" },
                                                                fontSize = 12.sp,
                                                                color = Slate500
                                                            )
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = if (alreadyAdded) Color(0xFFD1FAE5) else Color(0xFFF3E8FF),
                                                            modifier = Modifier.clickable {
                                                                if (!alreadyAdded) {
                                                                    chosenGarments.add(OutfitGarmentEntry(garment = garment))
                                                                } else {
                                                                    chosenGarments.removeAll { it.garment.id == garment.id }
                                                                }
                                                                if (isAutoNameMode) {
                                                                    title = buildAutoTitle(wearDate, chosenGarments)
                                                                }
                                                            }
                                                        ) {
                                                            Text(
                                                                text = if (alreadyAdded) "Added" else "+ Add",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (alreadyAdded) Color(0xFF047857) else Color(0xFF7C3AED),
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Chosen Garments List or Empty Dashed Card (IMG_9686 vs IMG_9689)
                            if (chosenGarments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.7f))
                                        .border(
                                            width = 1.5.dp,
                                            color = Color(0xFFD8B4FE),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { isClosetPickerOpen = true }
                                        .padding(vertical = 28.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Checkroom,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No clothes added yet",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF581C87)
                                        )
                                        Text(
                                            text = "Click \"Add Garment to Outfit\" to select the clothes you wore",
                                            fontSize = 12.sp,
                                            color = Color(0xFF9333EA),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    chosenGarments.forEachIndexed { index, item ->
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Category pill + Garment Name
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Color(0xFFF3E8FF)
                                                    ) {
                                                        Text(
                                                            text = item.garment.clothesType.ifBlank { "Item" },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF7C3AED),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = item.garment.name,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 15.sp,
                                                        color = Slate900
                                                    )
                                                }

                                                // Material · Thickness subtitle
                                                val sub = listOfNotNull(
                                                    item.garment.material.takeIf { it.isNotBlank() },
                                                    item.garment.thickClothes.takeIf { it.isNotBlank() }
                                                ).joinToString(" · ")
                                                if (sub.isNotBlank()) {
                                                    Text(
                                                        text = sub,
                                                        fontSize = 12.sp,
                                                        color = Slate500
                                                    )
                                                }

                                                // Duration Box: [ 4 ] hr : [ 0 ] min (IMG_9689)
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, Slate200)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Schedule,
                                                            contentDescription = null,
                                                            tint = Color(0xFF9333EA),
                                                            modifier = Modifier.size(16.dp)
                                                        )

                                                        // Hours input pill
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color.White,
                                                            border = BorderStroke(1.dp, Slate200)
                                                        ) {
                                                            BasicTextField(
                                                                value = item.hours.toString(),
                                                                onValueChange = { v ->
                                                                    val parsed = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                                    chosenGarments[index] = item.copy(hours = parsed.coerceIn(0, 48))
                                                                },
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                textStyle = TextStyle(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = Slate900,
                                                                    textAlign = TextAlign.Center
                                                                ),
                                                                modifier = Modifier
                                                                    .width(42.dp)
                                                                    .padding(vertical = 4.dp)
                                                            )
                                                        }

                                                        Text(
                                                            text = "hr :",
                                                            fontSize = 12.sp,
                                                            color = Slate600,
                                                            fontWeight = FontWeight.Medium
                                                        )

                                                        // Minutes input pill
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Color.White,
                                                            border = BorderStroke(1.dp, Slate200)
                                                        ) {
                                                            BasicTextField(
                                                                value = item.minutes.toString(),
                                                                onValueChange = { v ->
                                                                    val parsed = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                                    chosenGarments[index] = item.copy(minutes = parsed.coerceIn(0, 59))
                                                                },
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                textStyle = TextStyle(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = Slate900,
                                                                    textAlign = TextAlign.Center
                                                                ),
                                                                modifier = Modifier
                                                                    .width(42.dp)
                                                                    .padding(vertical = 4.dp)
                                                            )
                                                        }

                                                        Text(
                                                            text = "min",
                                                            fontSize = 12.sp,
                                                            color = Slate600,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }

                                                // Bottom Row: Wash state pill ("! need wash" vs "Clean") + Delete icon (IMG_9689)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    val isNeedWash = item.washStated.equals("need wash", ignoreCase = true)
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isNeedWash) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isNeedWash) Color(0xFFF59E0B) else Color(0xFF10B981)
                                                        ),
                                                        modifier = Modifier.clickable {
                                                            val nextState = if (isNeedWash) "Clean" else "need wash"
                                                            chosenGarments[index] = item.copy(washStated = nextState)
                                                        }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isNeedWash) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857),
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                            Text(
                                                                text = if (isNeedWash) "need wash" else "Clean",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857)
                                                            )
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { chosenGarments.removeAt(index) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Remove garment",
                                                            tint = Slate400,
                                                            modifier = Modifier.size(18.dp)
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

                    // 3. SENSORY & COMFORT REVIEW CARD (IMG_9687, IMG_9689)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "SENSORY & COMFORT REVIEW",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate700,
                                letterSpacing = 0.5.sp
                            )

                            SensoryFieldWithPrevious(
                                label = "SHOULDER FEEL",
                                value = shoulderFeel,
                                placeholder = "e.g. relaxed and comfortable",
                                tag = "input_shoulder_feel",
                                onValueChange = { shoulderFeel = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "SHOULDER FEEL" to { shoulderFeel = it }
                                }
                            )

                            SensoryFieldWithPrevious(
                                label = "MID BREAST",
                                value = midBreast,
                                placeholder = "e.g. good support, no digging",
                                tag = "input_mid_breast",
                                onValueChange = { midBreast = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "MID BREAST" to { midBreast = it }
                                }
                            )

                            SensoryFieldWithPrevious(
                                label = "NIGHT MID BREAST",
                                value = nightMidBreast,
                                placeholder = "e.g. comfortable for evening",
                                tag = "input_night_mid_breast",
                                onValueChange = { nightMidBreast = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "NIGHT MID BREAST" to { nightMidBreast = it }
                                }
                            )

                            SensoryFieldWithPrevious(
                                label = "SWEAT",
                                value = sweat,
                                placeholder = "e.g. low / dry",
                                tag = "input_sweat",
                                onValueChange = { sweat = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "SWEAT" to { sweat = it }
                                }
                            )

                            SensoryFieldWithPrevious(
                                label = "NIGHT SWEAT",
                                value = nightSweat,
                                placeholder = "e.g. dry and pleasant",
                                tag = "input_night_sweat",
                                onValueChange = { nightSweat = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "NIGHT SWEAT" to { nightSweat = it }
                                }
                            )

                            SensoryFieldWithPrevious(
                                label = "LEG FEEL",
                                value = legFeel,
                                placeholder = "e.g. free movement",
                                tag = "input_leg_feel",
                                onValueChange = { legFeel = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "LEG FEEL" to { legFeel = it }
                                }
                            )

                            SensoryFieldWithPrevious(
                                label = "NIGHT LEG FEEL",
                                value = nightLegFeel,
                                placeholder = "e.g. airy and easy",
                                tag = "input_night_leg_feel",
                                onValueChange = { nightLegFeel = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "NIGHT LEG FEEL" to { nightLegFeel = it }
                                }
                            )

                            SensoryFieldWithPrevious(
                                label = "REPORT DETAIL",
                                value = reportDetail,
                                placeholder = "Comprehensive wearing notes, weather, activities...",
                                tag = "input_report_detail",
                                singleLine = false,
                                minLines = 3,
                                onValueChange = { reportDetail = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "REPORT DETAIL" to { reportDetail = it }
                                }
                            )
                        }
                    }

                    // 4. OUTFIT OF THE DAY PHOTOS CARD (IMG_9687)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF9333EA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "OUTFIT OF THE DAY PHOTOS (${photoUrls.size})",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate800
                                    )
                                }

                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Attach Photo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (photoUrls.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(photoUrls) { url ->
                                        Box(
                                            modifier = Modifier
                                                .size(84.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Slate100)
                                                .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                                        ) {
                                            AsyncImage(
                                                model = ImageUtils.resolveImagePath(url, context),
                                                contentDescription = "Outfit photo",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE11D48))
                                                    .clickable { photoUrls.remove(url) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Sticky Bottom Footer (IMG_9686, IMG_9687, IMG_9689)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${chosenGarments.size} ${if (chosenGarments.size == 1) "garment" else "garments"}\nchosen",
                        fontSize = 12.sp,
                        color = Slate600,
                        lineHeight = 15.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Slate700
                            )
                        }

                        Button(
                            onClick = {
                                val finalTitle = title.ifBlank { "$wearDate wearing report" }
                                val picsJson = JSONArray().apply {
                                    photoUrls.forEach { put(it) }
                                }.toString()

                                val reportEntity = ReportEntity(
                                    id = initialReport?.id ?: "",
                                    name = finalTitle,
                                    wearDate = wearDate.ifBlank { todayIso },
                                    created = initialReport?.created ?: "",
                                    shoulderFeel = shoulderFeel,
                                    midBreast = midBreast,
                                    nightMidBreast = nightMidBreast,
                                    sweat = sweat,
                                    nightSweat = nightSweat,
                                    legFeel = legFeel,
                                    nightLegFeel = nightLegFeel,
                                    reportDetail = reportDetail,
                                    pictures = picsJson,
                                    tags = initialReport?.tags ?: "[\"#journal\"]"
                                )

                                val links = chosenGarments.map { entry ->
                                    val durationSec = (entry.hours * 3600L) + (entry.minutes * 60L)
                                    entry.garment.id to maxOf(60L, durationSec)
                                }
                                val washMap = chosenGarments.associate { it.garment.id to it.washStated }

                                if (onSaveWithWashMap != null) {
                                    onSaveWithWashMap(reportEntity, links, washMap)
                                } else {
                                    onSave(reportEntity, links)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (chosenGarments.isEmpty() && title.isBlank()) Color(0xFFC084FC) else Color(0xFF9333EA)
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("save_report_button")
                        ) {
                            Text(
                                text = "Save Wearing\nReport",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Historical Previous Entries Picker Modal
    activePreviousPicker?.let { (fieldLabel, onSelectCallback) ->
        PreviousEntriesModal(
            isOpen = true,
            fieldLabel = fieldLabel,
            entries = getPreviousEntries(fieldLabel),
            onSelect = { chosenVal -> onSelectCallback(chosenVal) },
            onClose = { activePreviousPicker = null }
        )
    }
}

@Composable
private fun SensoryFieldWithPrevious(
    label: String,
    value: String,
    placeholder: String,
    tag: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
    onOpenPrevious: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate700,
                letterSpacing = 0.4.sp
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.clickable { onOpenPrevious() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Previous",
                        tint = Color(0xFF9333EA),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Previous",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Slate400, fontSize = 13.sp) },
            singleLine = singleLine,
            minLines = minLines,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF9333EA),
                unfocusedBorderColor = Slate200,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}
