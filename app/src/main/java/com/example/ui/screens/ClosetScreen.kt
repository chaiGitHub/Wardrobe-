package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.drilldown.GarmentStats
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
import com.example.util.ImageUtils

enum class GarmentSortOption(val label: String) {
    TOTAL_WASHES("Total Washed Count"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    COST_PER_WEAR("Cost-Per-Wear ($/w)"),
    NAME("Name (A-Z)")
}

data class CategoryTab(val id: String, val label: String, val count: Int)

data class PresetOption(val id: String, val label: String, val icon: String? = null)

@Composable
fun ClosetScreen(
    clothes: List<GarmentEntity>,
    junctions: List<JunctionEntity> = emptyList(),
    selectedFilter: String,
    searchQuery: String,
    onFilterSelected: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onGarmentClick: (GarmentEntity) -> Unit,
    onAddGarmentClick: () -> Unit,
    onUpdateGarmentPhoto: ((GarmentEntity, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPreset by remember { mutableStateOf("all") }
    var currentSort by remember { mutableStateOf(GarmentSortOption.TOTAL_WASHES) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var showFieldDictionaryModal by remember { mutableStateOf(false) }
    var closetViewMode by remember { mutableStateOf("cards") } // "cards", "grid", "table"

    // Dynamic Category tabs computed from real clothes data
    val categoryTabs = remember(clothes) {
        val groups = clothes.groupBy { it.clothesType.ifBlank { "Other" } }
        val list = mutableListOf(CategoryTab("All", "All Items", clothes.size))
        // Prioritize common types
        val sortedKeys = groups.keys.sortedWith(compareByDescending<String> { groups[it]?.size ?: 0 }.thenBy { it })
        sortedKeys.forEach { key ->
            list.add(CategoryTab(key, key, groups[key]?.size ?: 0))
        }
        list
    }

    // Quick Presets matching web app ClosetView & IMG_9732.png
    val presets = listOf(
        PresetOption("all", "All Items"),
        PresetOption("not-hot", "Hot at noon: Not hot", "☀️"),
        PresetOption("favorites", "Beautiful Only", "✨"),
        PresetOption("need-wash", "Needs Wash", "⚠️"),
        PresetOption("chiffon", "material: chiffon", "🪶"),
        PresetOption("sit-ok", "sit breast ok", "💺")
    )

    // Filter & Sort
    val filteredGarments by remember(clothes, junctions, selectedCategory, selectedPreset, searchQuery, currentSort) {
        derivedStateOf {
            clothes.filter { g ->
                // Category match
                val matchesCategory = if (selectedCategory == "All") true
                else g.clothesType.equals(selectedCategory, ignoreCase = true)

                // Search query match
                val q = searchQuery.trim().lowercase()
                val matchesSearch = q.isBlank() ||
                        g.name.lowercase().contains(q) ||
                        g.material.lowercase().contains(q) ||
                        g.thickClothes.lowercase().contains(q) ||
                        g.clothesType.lowercase().contains(q)

                // Preset match
                val stats = DrilldownUtils.calculateGarmentStats(g.id, junctions)
                val matchesPreset = when (selectedPreset) {
                    "not-hot" -> g.hotAtNoon.contains("not", ignoreCase = true)
                    "need-wash" -> stats.needWash
                    "sit-ok" -> g.sitBreast.contains("ok", ignoreCase = true)
                    "chiffon" -> g.material.contains("chiffon", ignoreCase = true)
                    "favorites" -> g.beautiful.contains("beautiful", ignoreCase = true) && !g.beautiful.contains("not", ignoreCase = true)
                    else -> true
                }

                matchesCategory && matchesSearch && matchesPreset
            }.sortedWith { a, b ->
                val totalWashesA = a.wearingFrequency + a.trackedAppWashes
                val totalWashesB = b.wearingFrequency + b.trackedAppWashes
                when (currentSort) {
                    GarmentSortOption.TOTAL_WASHES -> totalWashesB.compareTo(totalWashesA)
                    GarmentSortOption.PRICE_LOW_HIGH -> {
                        val pA = if (a.price > 0) a.price else Double.MAX_VALUE
                        val pB = if (b.price > 0) b.price else Double.MAX_VALUE
                        pA.compareTo(pB)
                    }
                    GarmentSortOption.PRICE_HIGH_LOW -> b.price.compareTo(a.price)
                    GarmentSortOption.COST_PER_WEAR -> {
                        val statsA = DrilldownUtils.calculateGarmentStats(a.id, junctions)
                        val statsB = DrilldownUtils.calculateGarmentStats(b.id, junctions)
                        val wearsA = if (totalWashesA > 0) totalWashesA else statsA.totalWears
                        val wearsB = if (totalWashesB > 0) totalWashesB else statsB.totalWears
                        val cpwA = if (a.price > 0 && wearsA > 0) a.price / wearsA else Double.MAX_VALUE
                        val cpwB = if (b.price > 0 && wearsB > 0) b.price / wearsB else Double.MAX_VALUE
                        cpwA.compareTo(cpwB)
                    }
                    GarmentSortOption.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                }
            }
        }
    }

    if (showFieldDictionaryModal) {
        com.example.ui.dialogs.FieldDictionaryModal(
            onDismiss = { showFieldDictionaryModal = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP CONTROL CARD (Compact layout for mobile screens)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    // Row 1: Horizontal scroll of category tabs: All Items (52), Dress (44), Tops (6)...
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryTabs.forEach { tab ->
                            val isSelected = selectedCategory == tab.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) BrandPurple else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { selectedCategory = tab.id }
                            ) {
                                Text(
                                    text = "${tab.label} (${tab.count})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Slate700,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Row 2: Compact Manage Fields + Add Clothes + Sort selector on a single row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .height(34.dp)
                                .clickable { showFieldDictionaryModal = true }
                                .testTag("manage_fields_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = Slate700,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Manage Fields",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = onAddGarmentClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("add_clothes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Clothes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Compact Sort Selector
                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clickable { isSortMenuOpen = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp)
                                ) {
                                    Text(
                                        text = currentSort.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Default.UnfoldMore,
                                        contentDescription = null,
                                        tint = Slate600,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isSortMenuOpen,
                                onDismissRequest = { isSortMenuOpen = false }
                            ) {
                                GarmentSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(option.label, fontSize = 12.sp)
                                                if (currentSort == option) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = BrandPurple,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            currentSort = option
                                            isSortMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Row 3: Presets row (PRESETS: All Items | ☀️ Hot at noon: Not hot)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "PRESETS:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                        }

                        presets.forEach { preset ->
                            val isSelected = selectedPreset == preset.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFF3E8FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFDDD6FE) else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable { selectedPreset = preset.id }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (preset.icon != null) {
                                        Text(text = preset.icon, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = preset.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF581C87) else Slate600,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Row 4: Search bar + Compact [Cards | Grid | Table] Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = {
                                Text(
                                    text = "Search by Name, material, thick clothes...",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Slate400,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("search_by_name_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Slate200,
                                focusedBorderColor = BrandPurple
                            )
                        )

                        // View Mode Switcher: Cards | Grid | Table
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf(
                                    "cards" to "Cards",
                                    "grid" to "Grid",
                                    "table" to "Table"
                                ).forEach { (mode, label) ->
                                    val isSelected = closetViewMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                                        modifier = Modifier
                                            .clickable { closetViewMode = mode }
                                            .testTag("closet_view_$mode")
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) BrandPurple else Slate600,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // GARMENT DISPLAY MODES: Cards (1-col), Grid (2-col), or Table
        when (closetViewMode) {
            "grid" -> {
                items(filteredGarments.chunked(2), key = { pair -> pair.first().id }) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { garment ->
                            val stats = remember(garment.id, junctions) {
                                DrilldownUtils.calculateGarmentStats(garment.id, junctions)
                            }
                            GarmentCompactGridCard(
                                garment = garment,
                                stats = stats,
                                onClick = { onGarmentClick(garment) },
                                onUploadPhoto = { uriStr ->
                                    onUpdateGarmentPhoto?.invoke(garment, uriStr)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            "table" -> {
                item {
                    GarmentClosetTable(
                        garments = filteredGarments,
                        junctions = junctions,
                        onGarmentClick = onGarmentClick
                    )
                }
            }
            else -> {
                items(filteredGarments, key = { it.id }) { garment ->
                    val stats = remember(garment.id, junctions) {
                        DrilldownUtils.calculateGarmentStats(garment.id, junctions)
                    }
                    GarmentStudioCard(
                        garment = garment,
                        stats = stats,
                        onClick = { onGarmentClick(garment) },
                        onUploadPhoto = { uriStr ->
                            onUpdateGarmentPhoto?.invoke(garment, uriStr)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * Replicates the exact Garment Card shown in screenshot IMG_9638.PNG:
 * - Top-left badge: ⚠️ need wash (5.0h) or 💧 Clean
 * - Top-right badge: Dress (soft pink pill)
 * - Missing image dashed area with:
 *     - Amber slashed image icon
 *     - Garment title: "white marry old"
 *     - Subtitle: "Original S3 link expired"
 *     - Purple button: "📷 Upload Local Photo"
 *     - Bottom-left sparkle ribbon: "✨ Beautiful"
 * - Info section:
 *     - Bold title: "white marry old"
 *     - Material & thickness: "lace · 03 thick clothes"
 *     - Pills row: 🌡 Hot | not diao dai | 🛡 sit breast shape ok | 💧 dry
 *     - Footer metrics: 🕒 161 washes ... $44 ($0.27/w)
 */
@Composable
fun GarmentStudioCard(
    garment: GarmentEntity,
    junction: JunctionEntity? = null,
    stats: GarmentStats? = null,
    onClick: () -> Unit,
    onUploadPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalWashedCount = garment.wearingFrequency + garment.trackedAppWashes
    val effectiveWears = if (totalWashedCount > 0) totalWashedCount else (stats?.totalWears ?: 0)
    val cpw = if (garment.price > 0 && effectiveWears > 0) {
        String.format(java.util.Locale.US, "%.2f", garment.price / effectiveWears)
    } else null

    val resolvedPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
        ImageUtils.resolveImageModel(it, context)
    }

    // Local image upload launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPhoto(uri.toString())
        }
    }

    // Determine need wash status & wear hours from GarmentStats (matching web app getGarmentStats)
    val isNeedWash = stats?.needWash ?: (junction?.washStated?.equals("need wash", ignoreCase = true) == true)
    val unwashedHours = stats?.unwashedHours ?: if (junction != null && junction.duration > 0) {
        String.format(java.util.Locale.US, "%.1f", junction.duration / 3600.0)
    } else "0.0"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("garment_studio_card_${garment.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Upper Box: Image Area or S3 Expired Fallback Box (Matching screenshot)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(
                        BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                // If real local image exists and is valid
                if (resolvedPhotoModel != null) {
                    AsyncImage(
                        model = resolvedPhotoModel,
                        contentDescription = garment.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    // S3 Expired / Missing photo dashed card (Matching screenshot exactly)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B), // amber-500
                            modifier = Modifier.size(38.dp)
                        )

                        Text(
                            text = garment.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )

                        Text(
                            text = "Original S3 link expired",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate600
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Purple [Upload Local Photo] button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BrandPurple,
                            modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Upload Local Photo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Top Left Overlay: Wash status badge (⚠️ need wash (5.0h) / Clean)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isNeedWash) Color(0xFFF59E0B) else Color(0xFF059669),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isNeedWash) Icons.Default.Warning else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (isNeedWash) "need wash (${unwashedHours}h)" else "Clean",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Top Right Overlay: Garment Type badge (e.g., Dress soft pink pill)
                val type = garment.clothesType.ifBlank { "Garment" }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF1F2), // rose-50
                    border = BorderStroke(1.dp, Color(0xFFFFE4E6)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = type,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48), // rose-600
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Bottom Left Overlay: ✨ Beautiful ribbon if marked beautiful
                if (garment.beautiful.contains("beautiful", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB), // amber-50
                        border = BorderStroke(1.dp, Color(0xFFFEF3C7)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "✨ Beautiful",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309), // amber-700
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Lower Section: Details & Ergonomics Chips (Matching screenshot)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title
                Text(
                    text = garment.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                // Subtitle: material · thick clothes
                val materialText = garment.material.ifBlank { "fabric" }
                val thickText = garment.thickClothes.ifBlank { "01 normal" }
                Text(
                    text = "$materialText · $thickText",
                    fontSize = 12.sp,
                    color = Slate600
                )

                // Ergonomic Pills Row matching screenshot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hot at noon pill (e.g. 🌡 Hot / Not hot)
                    if (garment.hotAtNoon.isNotBlank()) {
                        val isHot = garment.hotAtNoon.contains("Hot", ignoreCase = true) && !garment.hotAtNoon.contains("Not hot", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isHot) Color(0xFFFFF7ED) else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (isHot) Color(0xFFFFEDD5) else Color(0xFFDCFCE7))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isHot) "🌡️ Hot" else "🌤️ Cool",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isHot) Color(0xFFC2410C) else Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    // diao dai pill
                    if (garment.diaoDai.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Text(
                                text = garment.diaoDai,
                                fontSize = 11.sp,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // sit breast ok pill
                    if (garment.sitBreast.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAF5FF),
                            border = BorderStroke(1.dp, Color(0xFFF3E8FF))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = garment.sitBreast,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF6B21A8)
                                )
                            }
                        }
                    }

                    // wet pill (e.g. 💧 dry / wet)
                    if (garment.wet.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = garment.wet,
                                    fontSize = 11.sp,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Footer Metrics: 🕒 161 washes ... $44 ($0.27/w)
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
                            tint = Slate400,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$totalWashedCount washes",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (garment.price > 0) {
                            val priceDisplay = if (garment.price % 1.0 == 0.0) {
                                "$${garment.price.toInt()}"
                            } else {
                                String.format(java.util.Locale.US, "$%.2f", garment.price)
                            }
                            Text(
                                text = priceDisplay,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            if (cpw != null) {
                                Text(
                                    text = "($$cpw/w)",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        } else {
                            Text(
                                text = "price: No data",
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Badge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun GarmentCompactGridCard(
    garment: GarmentEntity,
    stats: GarmentStats? = null,
    onClick: () -> Unit,
    onUploadPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalWashedCount = garment.wearingFrequency + garment.trackedAppWashes
    val resolvedPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
        ImageUtils.resolveImageModel(it, context)
    }
    val isNeedWash = stats?.needWash ?: false
    val unwashedHours = stats?.unwashedHours ?: "0.0"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPhoto(uri.toString())
        }
    }

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("garment_grid_card_${garment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(BorderStroke(1.dp, Slate200), RoundedCornerShape(12.dp))
            ) {
                if (resolvedPhotoModel != null) {
                    AsyncImage(
                        model = resolvedPhotoModel,
                        contentDescription = garment.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandPurple,
                            modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                        ) {
                            Text(
                                text = "Upload Photo",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Top-left wash pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isNeedWash) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                ) {
                    Text(
                        text = if (isNeedWash) "Need wash (${unwashedHours}h)" else "Clean",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNeedWash) Color(0xFFB45309) else Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
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
                    text = "${garment.clothesType.ifBlank { "Item" }} · ${garment.material.ifBlank { "No mat" }}",
                    fontSize = 10.sp,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalWashedCount w",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    Text(
                        text = if (garment.price > 0) "$${garment.price.toInt()}" else "No $",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }
            }
        }
    }
}

@Composable
fun GarmentClosetTable(
    garments: List<GarmentEntity>,
    junctions: List<JunctionEntity>,
    onGarmentClick: (GarmentEntity) -> Unit
) {
    val hScroll = rememberScrollState()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScroll)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GARMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, modifier = Modifier.width(130.dp))
                Text("TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, modifier = Modifier.width(65.dp))
                Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, modifier = Modifier.width(85.dp))
                Text("MATERIAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, modifier = Modifier.width(85.dp))
                Text("WASHES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, modifier = Modifier.width(60.dp))
                Text("PRICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, modifier = Modifier.width(55.dp))
            }
            HorizontalDivider(color = Slate200)

            garments.forEach { garment ->
                val stats = remember(garment.id, junctions) {
                    DrilldownUtils.calculateGarmentStats(garment.id, junctions)
                }
                val totalWashes = garment.wearingFrequency + garment.trackedAppWashes
                Row(
                    modifier = Modifier
                        .clickable { onGarmentClick(garment) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = garment.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(130.dp)
                    )
                    Text(
                        text = garment.clothesType.ifBlank { "—" },
                        fontSize = 11.sp,
                        color = Slate700,
                        maxLines = 1,
                        modifier = Modifier.width(65.dp)
                    )
                    Box(modifier = Modifier.width(85.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (stats.needWash) Color(0xFFFEF3C7) else Color(0xFFECFDF5)
                        ) {
                            Text(
                                text = if (stats.needWash) "Need wash" else "Clean",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.needWash) Color(0xFFB45309) else Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = garment.material.ifBlank { "—" },
                        fontSize = 11.sp,
                        color = Slate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(85.dp)
                    )
                    Text(
                        text = "$totalWashes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = if (garment.price > 0) "$${garment.price.toInt()}" else "—",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        modifier = Modifier.width(55.dp)
                    )
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}
