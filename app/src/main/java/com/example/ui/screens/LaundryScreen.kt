package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
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

@OptIn(ExperimentalLayoutApi::class)
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

    val needWashBundles = remember(garmentBundles) {
        garmentBundles
            .filter { it.stats.needWash }
            .sortedByDescending { it.stats.unwashedSeconds }
    }

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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenWidth = maxWidth
        val isWide = screenWidth >= 600.dp
        val isLaptop = screenWidth >= 920.dp
        val columns = if (isLaptop) 2 else 1

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1440.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = if (isWide) 24.dp else 14.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HEADER & WASH BATCH CONTROLS CARD (Screenshot 230258)
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Laundry & Wash Tracker",
                                fontSize = if (isWide) 22.sp else 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            Text(
                                text = "Items with \"need wash\" status and unwash time rollup from junction records",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }

                        // 3 KPI Boxes (Screenshot 230258)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatusStatBox(
                                count = "${needWashBundles.size}",
                                label = "Garments Need Wash",
                                bgColor = Color(0xFFFFFBEB),
                                borderColor = Color(0xFFFDE68A),
                                valueColor = Color(0xFFB45309),
                                modifier = Modifier.weight(1f)
                            )
                            StatusStatBox(
                                count = "${totalUnwashedHours}h",
                                label = "Unwashed Rollup ($totalUnwashedSessions logs)",
                                bgColor = Color(0xFFFAF5FF),
                                borderColor = Color(0xFFE9D5FF),
                                valueColor = BrandPurple,
                                modifier = Modifier.weight(1.25f)
                            )
                            StatusStatBox(
                                count = "${cleanBundles.size}",
                                label = "Clean & Ready",
                                bgColor = Color(0xFFECFDF5),
                                borderColor = Color(0xFFA7F3D0),
                                valueColor = Color(0xFF047857),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Wash Date Picker + Undo + Wash All Bar (Screenshot 230258)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Wash Date:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate600
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
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
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = washDateStr,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900
                                            )
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Wash Date",
                                                tint = Slate500,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
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
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .testTag("laundry_undo_btn")
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .testTag("laundry_wash_all_btn")
                                        ) {
                                            Icon(Icons.Default.Opacity, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Wash All (${needWashBundles.size})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

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

            // 2. SEGMENTED PILL TABS: Needs Washing (24) | Clean & Ready (28) (Screenshot 230258)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDirtyTab = selectedTab == 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDirtyTab) Color(0xFFF59E0B) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 0 }
                                .testTag("laundry_tab_0")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDirtyTab) Color.White else Slate600,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Needs Washing (${needWashBundles.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDirtyTab) Color.White else Slate700
                                )
                            }
                        }

                        val isCleanTab = selectedTab == 1
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCleanTab) Color(0xFF059669) else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 1 }
                                .testTag("laundry_tab_1")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isCleanTab) Color.White else Slate600,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Clean & Ready (${cleanBundles.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCleanTab) Color.White else Slate700
                                )
                            }
                        }
                    }
                }
            }

            // 3. GARMENT LAUNDRY CARDS (1-col on mobile, 2-col on laptop)
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
            } else if (columns == 1) {
                items(activeBundles, key = { it.garment.id }) { bundle ->
                    GarmentLaundryCard(
                        bundle = bundle,
                        washDateStr = washDateStr,
                        reportsById = reportsById,
                        onSelectGarment = { onSelectGarment?.invoke(bundle.garment) },
                        onMarkSingleJunctionClean = { jId ->
                            onMarkWash(jId, "Clean")
                            statusBannerMessage = "Marked wearing session as washed!"
                        },
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
            } else {
                items(
                    activeBundles.chunked(columns),
                    key = { row -> "laundry_row_${row.first().garment.id}" }
                ) { rowBundles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        rowBundles.forEach { bundle ->
                            GarmentLaundryCard(
                                bundle = bundle,
                                washDateStr = washDateStr,
                                reportsById = reportsById,
                                onSelectGarment = { onSelectGarment?.invoke(bundle.garment) },
                                onMarkSingleJunctionClean = { jId ->
                                    onMarkWash(jId, "Clean")
                                    statusBannerMessage = "Marked wearing session as washed!"
                                },
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
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowBundles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun StatusStatBox(
    count: String,
    label: String,
    bgColor: Color,
    borderColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = count,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = valueColor,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Matches Screenshot_20261001-230258.png Garment Laundry Card
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GarmentLaundryCard(
    bundle: GarmentLaundryBundle,
    washDateStr: String,
    reportsById: Map<String, ReportEntity>,
    onSelectGarment: () -> Unit,
    onMarkSingleJunctionClean: (String) -> Unit,
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isNeedWash) Color(0xFFFDE68A) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Left Garment Info + Right Unwashed Rollup Box & Mark Washed Button (Screenshot 230258)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectGarment() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
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
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = garment.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = garment.clothesType.ifBlank { "Tops" },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (isNeedWash) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "${bundle.unwashedJunctions.size} unwashed wears",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Total washed: ${totalWashedCount}x",
                                fontSize = 10.sp,
                                color = Slate500
                            )
                            val lastWash = stats.latestWashDate ?: garment.lastWashed.ifBlank { "—" }
                            Text(
                                text = "Last wash: $lastWash",
                                fontSize = 10.sp,
                                color = Slate500
                            )
                        }
                    }
                }

                // Right Side: Unwashed Rollup Box + [Mark Washed (Date)] Button (Screenshot 230258)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNeedWash) Color(0xFFFFFBEB) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, if (isNeedWash) Color(0xFFFDE68A) else Color(0xFFA7F3D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${stats.unwashedHours}h",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857)
                            )
                            Text(
                                text = "UNWASHED ROLLUP",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isNeedWash) Color(0xFFD97706) else Color(0xFF059669)
                            )
                        }
                    }

                    if (isNeedWash) {
                        Button(
                            onClick = onMarkGarmentClean,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("mark_garment_clean_${garment.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark Washed ($washDateStr)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Unwashed Wearing Sessions List (Screenshot 230258)
            if (isNeedWash && bundle.unwashedJunctions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "UNWASHED WEARING SESSIONS (${bundle.unwashedJunctions.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate500
                            )
                        }

                        bundle.unwashedJunctions.forEach { j ->
                            val wDate = CalendarCalculations.resolveJunctionWearDate(j, reportsById)
                            val report = reportsById[j.wearingReportId]
                            val rTitle = report?.name?.ifBlank { j.wearingReportTitle }
                                ?: j.wearingReportTitle.ifBlank { "casual" }
                            val hrs = String.format(Locale.US, "%.1fh worn", j.duration / 3600.0)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "${wDate.ifBlank { "Logged" }} · $rTitle",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "24°C · Casual · $hrs",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onMarkSingleJunctionClean(j.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Wash",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
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
    }
}
