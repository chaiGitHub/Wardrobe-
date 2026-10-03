package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.drilldown.GarmentStats
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.CalendarCalculations
import com.example.util.ImageUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class GarmentLaundryBundle(
    val garment: GarmentEntity,
    val stats: GarmentStats,
    val unwashedJunctions: List<JunctionEntity>,
    val allGarmentJunctions: List<JunctionEntity>
)

@Composable
fun LaundryScreen(
    junctions: List<JunctionEntity>,
    clothes: List<GarmentEntity>,
    reports: List<ReportEntity> = emptyList(),
    onMarkWash: (String, String) -> Unit,
    onMarkGarmentWashed: ((String, String, (String?) -> Unit) -> Unit)? = null,
    onMarkAllWashed: ((String, (String?) -> Unit) -> Unit)? = null,
    onUndoLastWash: (((Boolean) -> Unit) -> Unit)? = null,
    onSelectGarment: ((GarmentEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var washDateStr by remember {
        mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)))
    }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var canUndo by remember { mutableStateOf(false) }

    val reportsById = remember(reports) { reports.associateBy { it.id } }

    // Group by Garment (matching web app LaundryCare component)
    val garmentBundles = remember(clothes, junctions) {
        clothes.map { garment ->
            val stats = DrilldownUtils.calculateGarmentStats(garment.id, junctions)
            val allJ = junctions.filter { it.clothesId == garment.id }
            val dirtyJ = allJ.filter { it.washStated.equals("need wash", ignoreCase = true) }
            GarmentLaundryBundle(
                garment = garment,
                stats = stats,
                unwashedJunctions = dirtyJ,
                allGarmentJunctions = allJ
            )
        }
    }

    // Dirty garments sorted by accumulated unwashed wear seconds descending
    val needWashBundles = remember(garmentBundles) {
        garmentBundles
            .filter { it.stats.needWash }
            .sortedByDescending { it.stats.unwashedSeconds }
    }

    // Clean garments sorted by most recently washed / total washes
    val cleanBundles = remember(garmentBundles) {
        garmentBundles
            .filter { !it.stats.needWash }
            .sortedWith(
                compareByDescending<GarmentLaundryBundle> { it.garment.lastWashed }
                    .thenByDescending { it.garment.wearingFrequency + it.garment.trackedAppWashes }
            )
    }

    val totalUnwashedSessions = remember(needWashBundles) {
        needWashBundles.sumOf { it.unwashedJunctions.size }
    }
    val totalUnwashedHours = remember(needWashBundles) {
        String.format(Locale.US, "%.1f", needWashBundles.sumOf { it.stats.unwashedSeconds } / 3600.0)
    }

    val activeBundles = if (selectedTab == 0) needWashBundles else cleanBundles

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HEADER & WASH BATCH CONTROLS CARD
        item {
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
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Laundry & Wash Tracker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = "Items with \"need wash\" status and unwash time rollup from junction records",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalLaundryService,
                                contentDescription = "Laundry",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // KPI Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusStatBox(
                            count = "${needWashBundles.size}",
                            label = "Garments Need Wash",
                            color = BrandAmber,
                            modifier = Modifier.weight(1f)
                        )
                        StatusStatBox(
                            count = "${totalUnwashedHours}h",
                            label = "Unwashed Rollup ($totalUnwashedSessions logs)",
                            color = BrandPurple,
                            modifier = Modifier.weight(1.2f)
                        )
                        StatusStatBox(
                            count = "${cleanBundles.size}",
                            label = "Clean & Ready",
                            color = BrandEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Slate100)

                    // Wash Date Picker + Undo + Wash All Bar (Matching Web App LaundryCare)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Wash Date Selector
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val parts = washDateStr.split("-")
                                    val y = parts.getOrNull(0)?.toIntOrNull() ?: LocalDate.now().year
                                    val m = (parts.getOrNull(1)?.toIntOrNull() ?: LocalDate.now().monthValue) - 1
                                    val d = parts.getOrNull(2)?.toIntOrNull() ?: LocalDate.now().dayOfMonth
                                    DatePickerDialog(
                                        context,
                                        { _, selY, selM, selD ->
                                            washDateStr = String.format(Locale.US, "%04d-%02d-%02d", selY, selM + 1, selD)
                                        },
                                        y, m, d
                                    ).show()
                                }
                                .testTag("laundry_wash_date_picker")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "Wash Date",
                                    tint = Slate600,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Wash Date: $washDateStr",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canUndo && onUndoLastWash != null) {
                                OutlinedButton(
                                    onClick = {
                                        onUndoLastWash { ok ->
                                            if (ok) {
                                                statusBannerMessage = "Reverted last wash action."
                                                canUndo = false
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("laundry_undo_btn")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Undo Wash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (needWashBundles.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        if (onMarkAllWashed != null) {
                                            onMarkAllWashed(washDateStr) { msg ->
                                                statusBannerMessage = msg ?: "Marked all ${needWashBundles.size} garments as washed on $washDateStr!"
                                                canUndo = true
                                            }
                                        } else {
                                            needWashBundles.forEach { b ->
                                                b.unwashedJunctions.forEach { j -> onMarkWash(j.id, "Clean") }
                                            }
                                            statusBannerMessage = "Marked ${needWashBundles.size} garments as washed!"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("laundry_wash_all_btn")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Wash All (${needWashBundles.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Feedback Banner
                    statusBannerMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BrandEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Text(
                                    text = "Dismiss",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.clickable { statusBannerMessage = null }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. QUEUE TABS
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = BrandBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = BrandBlue
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Need Wash (${needWashBundles.size} Garments)",
                                fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("laundry_tab_0")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Clean & Ready (${cleanBundles.size} Garments)",
                                fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("laundry_tab_1")
                    )
                }
            }
        }

        // 3. GARMENT LAUNDRY CARDS
        if (activeBundles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "All Clean",
                            tint = BrandEmerald,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All Caught Up! Zero Dirty Clothes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = "Every garment in your wardrobe is currently clean and ready to wear.",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }
            }
        } else {
            items(activeBundles, key = { it.garment.id }) { bundle ->
                GarmentLaundryCard(
                    bundle = bundle,
                    washDateStr = washDateStr,
                    reportsById = reportsById,
                    onSelectGarment = { onSelectGarment?.invoke(bundle.garment) },
                    onMarkGarmentClean = {
                        if (onMarkGarmentWashed != null) {
                            onMarkGarmentWashed(bundle.garment.id, washDateStr) { msg ->
                                statusBannerMessage = msg ?: "Marked \"${bundle.garment.name}\" as washed on $washDateStr!"
                                canUndo = true
                            }
                        } else {
                            bundle.unwashedJunctions.forEach { j -> onMarkWash(j.id, "Clean") }
                            statusBannerMessage = "Marked \"${bundle.garment.name}\" as washed!"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusStatBox(
    count: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GarmentLaundryCard(
    bundle: GarmentLaundryBundle,
    washDateStr: String,
    reportsById: Map<String, ReportEntity>,
    onSelectGarment: () -> Unit,
    onMarkGarmentClean: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val garment = bundle.garment
    val stats = bundle.stats
    val isNeedWash = stats.needWash
    val totalWashedCount = garment.wearingFrequency + garment.trackedAppWashes

    val firstPhoto = remember(garment.pictures) {
        ImageUtils.parsePhotoList(garment.pictures).firstOrNull()
    }
    val imageModel = remember(firstPhoto) {
        ImageUtils.resolvePhotoModel(context, firstPhoto)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("laundry_garment_card_${garment.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isNeedWash) Color(0xFFFDE68A) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Thumbnail + Title + Unwash Time Rollup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectGarment() },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = garment.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = garment.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isNeedWash) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = if (isNeedWash) "NEED WASH" else "CLEAN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Category & Material chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (garment.clothesType.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = garment.clothesType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (garment.material.isNotBlank()) {
                            Text(
                                text = garment.material,
                                fontSize = 11.sp,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Unwash Time Rollup & Total Washes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isNeedWash) Color(0xFFD97706) else Slate400,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isNeedWash) {
                                    "Unwash Rollup: ${stats.unwashedHours} hrs (${bundle.unwashedJunctions.size} sessions)"
                                } else {
                                    "Unwash Rollup: 0.0 hrs (${stats.totalWears} total sessions)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNeedWash) Color(0xFFB45309) else Slate600
                            )
                        }
                    }

                    Text(
                        text = "Total Washed: $totalWashedCount (Legacy: ${garment.wearingFrequency}, App: ${garment.trackedAppWashes}) • Last: ${garment.lastWashed.ifBlank { "Never" }}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate500
                    )
                }
            }

            // Contributing Unwashed Sessions Breakdown
            if (isNeedWash && bundle.unwashedJunctions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFBEB))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "UNWASHED WEAR SESSIONS (${bundle.unwashedJunctions.size}):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF92400E)
                    )
                    bundle.unwashedJunctions.forEach { j ->
                        val wDate = CalendarCalculations.resolveJunctionWearDate(j, reportsById)
                        val rTitle = reportsById[j.wearingReportId]?.name?.ifBlank { j.wearingReportTitle }
                            ?: j.wearingReportTitle.ifBlank { "Wearing Session" }
                        val hrs = String.format(Locale.US, "%.1f", j.duration / 3600.0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${wDate.ifBlank { "Logged" }} • $rTitle",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$hrs hrs",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                // Mark as Washed & Clean Button
                Button(
                    onClick = onMarkGarmentClean,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mark_garment_clean_${garment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalLaundryService,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mark as Washed & Clean ($washDateStr)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
