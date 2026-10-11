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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
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
import com.example.util.ImageUtils
import java.text.NumberFormat
import java.util.Locale

enum class GarmentSortOption(val label: String) {
    TOTAL_WASHES("Total Washed Count"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    COST_PER_WEAR("Cost-Per-Wear ($/w)"),
    NAME("Name (A-Z)")
}

data class CategoryTab(val id: String, val label: String, val count: Int)

data class PresetOption(val id: String, val label: String)

private fun deriveComfortRating(g: GarmentEntity): Int {
    if (g.NotComfortable.contains("not", ignoreCase = true) && !g.NotComfortable.contains("comfortable", ignoreCase = true)) return 3
    if (g.beautiful.contains("beautiful", ignoreCase = true)) return 5
    return 5
}

private fun deriveBreathabilityRating(g: GarmentEntity): Int {
    val heat = g.noAircondHotFeeling.ifBlank { g.hotAtNoon }
    if (heat.contains("Not hot", ignoreCase = true) || heat.contains("not so cold", ignoreCase = true)) return 5
    if (heat.contains("Hot", ignoreCase = true)) return 3
    if (g.wet.contains("dry", ignoreCase = true)) return 4
    return 4
}

private fun deriveProtectionRating(g: GarmentEntity): Int {
    if (g.sitBreast.contains("ok", ignoreCase = true) || g.StandBreast.contains("ok", ignoreCase = true)) return 5
    if (g.antiSeeThrough.isNotBlank() && !g.antiSeeThrough.contains("transparent", ignoreCase = true)) return 4
    return 3
}

private fun deriveDurabilityRating(g: GarmentEntity): Int {
    val totalWashes = g.wearingFrequency + g.trackedAppWashes
    return when {
        totalWashes >= 10 -> 5
        totalWashes >= 4 -> 5
        else -> 4
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    onBatchUpdateTags: ((List<String>, String, Boolean, (Int) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPreset by remember { mutableStateOf("all") }
    var currentSort by remember { mutableStateOf(GarmentSortOption.TOTAL_WASHES) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var closetViewMode by remember { mutableStateOf("cards") } // "cards", "grid", "table"

    // Multi-select / Batch Tag Manager state (matching web app "Select Mode" / "Exit Select")
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedGarmentIds = remember { mutableStateListOf<String>() }
    var targetTag by remember { mutableStateOf("") }

    // Precompute stats per garment
    val statsById = remember(clothes, junctions) {
        clothes.associate { g -> g.id to DrilldownUtils.calculateGarmentStats(g.id, junctions) }
    }

    // Rank map by total washed count
    val washRankById = remember(clothes) {
        clothes
            .sortedByDescending { it.wearingFrequency + it.trackedAppWashes }
            .mapIndexed { index, g -> g.id to (index + 1) }
            .toMap()
    }

    // Dynamic Category tabs computed from real clothes data
    val categoryTabs = remember(clothes) {
        val grouped = clothes.groupBy { it.clothesType.ifBlank { "Uncategorized" } }
        val list = mutableListOf(CategoryTab("All", "All", clothes.size))
        grouped.entries
            .sortedByDescending { it.value.size }
            .forEach { (type, items) ->
                list.add(CategoryTab(type, type, items.size))
            }
        list
    }

    val presets = remember {
        listOf(
            PresetOption("all", "All Items"),
            PresetOption("dirty", "Dirty / Need Wash"),
            PresetOption("protection_4", "High Protection >=4"),
            PresetOption("breathability_4", "High Breathability >=4"),
            PresetOption("best_value", "Best Value <=\$10/w")
        )
    }

    // Filtered & Sorted Garments
    val filteredGarments by remember(clothes, junctions, selectedCategory, selectedPreset, searchQuery, currentSort, statsById) {
        derivedStateOf {
            clothes.filter { g ->
                val gStats = statsById[g.id]
                val catMatch = if (selectedCategory == "All") {
                    true
                } else {
                    g.clothesType.ifBlank { "Uncategorized" }.equals(selectedCategory, ignoreCase = true)
                }

                val totalWashes = (g.wearingFrequency + g.trackedAppWashes).coerceAtLeast(gStats?.totalWears ?: 0)
                val cpw = if (g.price > 0 && totalWashes > 0) g.price / totalWashes else Double.MAX_VALUE

                val presetMatch = when (selectedPreset) {
                    "dirty" -> gStats?.needWash == true
                    "protection_4" -> deriveProtectionRating(g) >= 4
                    "breathability_4" -> deriveBreathabilityRating(g) >= 4
                    "best_value" -> cpw <= 10.0
                    else -> true
                }

                val q = searchQuery.trim().lowercase(Locale.US)
                val searchMatch = if (q.isEmpty()) {
                    true
                } else {
                    g.name.lowercase(Locale.US).contains(q) ||
                        g.material.lowercase(Locale.US).contains(q) ||
                        g.thickClothes.lowercase(Locale.US).contains(q) ||
                        g.clothesType.lowercase(Locale.US).contains(q) ||
                        g.impression.lowercase(Locale.US).contains(q)
                }

                catMatch && presetMatch && searchMatch
            }.let { list ->
                when (currentSort) {
                    GarmentSortOption.TOTAL_WASHES -> list.sortedByDescending { it.wearingFrequency + it.trackedAppWashes }
                    GarmentSortOption.PRICE_LOW_HIGH -> list.sortedBy { if (it.price > 0) it.price else Double.MAX_VALUE }
                    GarmentSortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
                    GarmentSortOption.COST_PER_WEAR -> list.sortedBy {
                        val washes = it.wearingFrequency + it.trackedAppWashes
                        if (it.price > 0 && washes > 0) it.price / washes else Double.MAX_VALUE
                    }
                    GarmentSortOption.NAME -> list.sortedBy { it.name.lowercase(Locale.US) }
                }
            }
        }
    }

    val totalValueFormatted = remember(clothes) {
        val sum = clothes.sumOf { it.price }.toInt()
        NumberFormat.getIntegerInstance(Locale.US).format(sum)
    }

    val dirtyCount = remember(statsById) {
        statsById.values.count { it.needWash }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenWidth = maxWidth
        val isTablet = screenWidth >= 600.dp && screenWidth < 960.dp
        val isLaptop = screenWidth >= 960.dp
        val isWide = isTablet || isLaptop

        // Calculate responsive column counts for Cards and Grid modes
        val cardsColumns = when {
            screenWidth >= 1100.dp -> 3
            screenWidth >= 680.dp -> 2
            else -> 1
        }
        val gridColumns = when {
            screenWidth >= 1200.dp -> 5
            screenWidth >= 960.dp -> 4
            screenWidth >= 600.dp -> 3
            else -> 2
        }

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
            // 1. HEADER ROW: Title + Stats on Left, [Cards | Grid | Table] Switcher on Right (Screenshot 222706)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Garment Inventory",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${filteredGarments.size} of ${clothes.size} items displayed · Total Value: $$totalValueFormatted",
                                        fontSize = 12.sp,
                                        color = Slate600
                                    )
                                    if (dirtyCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "$dirtyCount items need washing",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Top-Right: Select Mode Button + View Mode Switcher
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ClosetSelectModeButton(
                                    isSelectMode = isSelectMode,
                                    selectedCount = selectedGarmentIds.size,
                                    onClick = {
                                        isSelectMode = !isSelectMode
                                        if (!isSelectMode) selectedGarmentIds.clear()
                                    }
                                )
                                ClosetViewModeSwitcher(
                                    closetViewMode = closetViewMode,
                                    onModeChange = { closetViewMode = it }
                                )
                            }
                        }
                    } else {
                        // Phone/Compact: Stack title/stats on top, clean full-width action bar below
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Garment Inventory",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${filteredGarments.size} of ${clothes.size} items displayed · Total Value: $$totalValueFormatted",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                                if (dirtyCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "$dirtyCount items need washing",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ClosetSelectModeButton(
                                    isSelectMode = isSelectMode,
                                    selectedCount = selectedGarmentIds.size,
                                    onClick = {
                                        isSelectMode = !isSelectMode
                                        if (!isSelectMode) selectedGarmentIds.clear()
                                    }
                                )
                                ClosetViewModeSwitcher(
                                    closetViewMode = closetViewMode,
                                    onModeChange = { closetViewMode = it }
                                )
                            }
                        }
                    }
                }
            }

            // 3. FILTER & SORT CONTROLS CARD (Screenshot 222706)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Category Pills + [Select Mode] + [Add Clothes] Buttons (Responsive for Compact/Wide)
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryPillsFlow(
                                    categoryTabs = categoryTabs,
                                    selectedCategory = selectedCategory,
                                    onSelect = { selectedCategory = it },
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 10.dp)
                                ) {
                                    ClosetSelectModeButton(
                                        isSelectMode = isSelectMode,
                                        selectedCount = selectedGarmentIds.size,
                                        onClick = {
                                            isSelectMode = !isSelectMode
                                            if (!isSelectMode) selectedGarmentIds.clear()
                                        }
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF9333EA),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onAddGarmentClick() }
                                            .testTag("add_garment_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Clothes",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Add Clothes",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Phone/Compact: Dedicated category pills row + clean prominent Action Row
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CategoryPillsFlow(
                                    categoryTabs = categoryTabs,
                                    selectedCategory = selectedCategory,
                                    onSelect = { selectedCategory = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ClosetSelectModeButton(
                                        isSelectMode = isSelectMode,
                                        selectedCount = selectedGarmentIds.size,
                                        onClick = {
                                            isSelectMode = !isSelectMode
                                            if (!isSelectMode) selectedGarmentIds.clear()
                                        }
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF9333EA),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onAddGarmentClick() }
                                            .testTag("add_garment_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Clothes",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Add Clothes",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ClosetSearchField(
                            searchQuery = searchQuery,
                            onSearchChanged = onSearchChanged,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = Slate100)

                        // Smart Filter Presets + Sort By Dropdown (Column with horizontal scroll)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: SMART FILTER horizontal scrollable chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "SMART FILTER:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate500
                                    )
                                }

                                presets.forEach { preset ->
                                    val isSelected = selectedPreset == preset.id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF059669) else Slate100,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedPreset = preset.id }
                                    ) {
                                        Text(
                                            text = preset.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Slate700,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            // Row 2: Sort by Dropdown
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Sort by:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate500
                                )
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { isSortMenuOpen = true }
                                            .testTag("closet_sort_dropdown")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = currentSort.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate800
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Slate500,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isSortMenuOpen,
                                        onDismissRequest = { isSortMenuOpen = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        GarmentSortOption.entries.forEach { option ->
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
                                                                tint = BrandEmerald,
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
                        }
                    }
                }
            }

            // 2. BATCH TAG MANAGER PANEL (Shown when Select Mode is active)
            if (isSelectMode) {
                item {
                    BatchTagManagerCard(
                        totalCount = filteredGarments.size,
                        selectedCount = selectedGarmentIds.size,
                        targetTag = targetTag,
                        onTargetTagChange = { targetTag = it },
                        onSelectAll = {
                            selectedGarmentIds.clear()
                            selectedGarmentIds.addAll(filteredGarments.map { it.id })
                        },
                        onDeselectAll = {
                            selectedGarmentIds.clear()
                        },
                        onExitSelect = {
                            isSelectMode = false
                            selectedGarmentIds.clear()
                        },
                        onAddTag = {
                            if (selectedGarmentIds.isNotEmpty() && targetTag.isNotBlank()) {
                                onBatchUpdateTags?.invoke(selectedGarmentIds.toList(), targetTag, true) { count ->
                                    Toast.makeText(context, "Added tag \"$targetTag\" to $count items!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onRemoveTag = {
                            if (selectedGarmentIds.isNotEmpty() && targetTag.isNotBlank()) {
                                onBatchUpdateTags?.invoke(selectedGarmentIds.toList(), targetTag, false) { count ->
                                    Toast.makeText(context, "Removed tag \"$targetTag\" from $count items!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            // 4. GARMENT DISPLAY MODES: Responsive Cards (1/2/3-col), Grid (2/3/4/5-col), or Table
            when (closetViewMode) {
                "grid" -> {
                    items(
                        filteredGarments.chunked(gridColumns),
                        key = { row -> "grid_row_${row.first().id}" }
                    ) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { garment ->
                                val isSelected = selectedGarmentIds.contains(garment.id)
                                GarmentCompactGridCard(
                                    garment = garment,
                                    stats = statsById[garment.id],
                                    washRank = washRankById[garment.id],
                                    isSelectMode = isSelectMode,
                                    isSelected = isSelected,
                                    onToggleSelect = {
                                        if (isSelected) selectedGarmentIds.remove(garment.id) else selectedGarmentIds.add(garment.id)
                                    },
                                    onClick = {
                                        if (isSelectMode) {
                                            if (isSelected) selectedGarmentIds.remove(garment.id) else selectedGarmentIds.add(garment.id)
                                        } else {
                                            onGarmentClick(garment)
                                        }
                                    },
                                    onUploadPhoto = { uriStr ->
                                        onUpdateGarmentPhoto?.invoke(garment, uriStr)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(gridColumns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                "table" -> {
                    item {
                        GarmentClosetTable(
                            garments = filteredGarments,
                            statsById = statsById,
                            isSelectMode = isSelectMode,
                            selectedIds = selectedGarmentIds.toSet(),
                            onToggleSelect = { gId ->
                                if (selectedGarmentIds.contains(gId)) selectedGarmentIds.remove(gId) else selectedGarmentIds.add(gId)
                            },
                            minTableWidth = if (isWide) (screenWidth - 48.dp).coerceAtLeast(860.dp) else 860.dp,
                            onGarmentClick = { garment ->
                                if (isSelectMode) {
                                    if (selectedGarmentIds.contains(garment.id)) selectedGarmentIds.remove(garment.id) else selectedGarmentIds.add(garment.id)
                                } else {
                                    onGarmentClick(garment)
                                }
                            }
                        )
                    }
                }

                else -> {
                    if (cardsColumns == 1) {
                        items(filteredGarments, key = { it.id }) { garment ->
                            val isSelected = selectedGarmentIds.contains(garment.id)
                            GarmentStudioCard(
                                garment = garment,
                                stats = statsById[garment.id],
                                washRank = washRankById[garment.id],
                                isSelectMode = isSelectMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    if (isSelected) selectedGarmentIds.remove(garment.id) else selectedGarmentIds.add(garment.id)
                                },
                                onClick = {
                                    if (isSelectMode) {
                                        if (isSelected) selectedGarmentIds.remove(garment.id) else selectedGarmentIds.add(garment.id)
                                    } else {
                                        onGarmentClick(garment)
                                    }
                                },
                                onUploadPhoto = { uriStr ->
                                    onUpdateGarmentPhoto?.invoke(garment, uriStr)
                                }
                            )
                        }
                    } else {
                        items(
                            filteredGarments.chunked(cardsColumns),
                            key = { row -> "card_row_${row.first().id}" }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                rowItems.forEach { garment ->
                                    val isSelected = selectedGarmentIds.contains(garment.id)
                                    GarmentStudioCard(
                                        garment = garment,
                                        stats = statsById[garment.id],
                                        washRank = washRankById[garment.id],
                                        isSelectMode = isSelectMode,
                                        isSelected = isSelected,
                                        onToggleSelect = {
                                            if (isSelected) selectedGarmentIds.remove(garment.id) else selectedGarmentIds.add(garment.id)
                                        },
                                        onClick = {
                                            if (isSelectMode) {
                                                if (isSelected) selectedGarmentIds.remove(garment.id) else selectedGarmentIds.add(garment.id)
                                            } else {
                                                onGarmentClick(garment)
                                            }
                                        },
                                        onUploadPhoto = { uriStr ->
                                            onUpdateGarmentPhoto?.invoke(garment, uriStr)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(cardsColumns - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ClosetSelectModeButton(
    isSelectMode: Boolean,
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelectMode) Color(0xFF7E22CE) else Color.White,
        border = BorderStroke(1.dp, if (isSelectMode) Color(0xFF6B21A8) else Slate200),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("closet_select_mode_btn")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isSelectMode) Icons.Default.CheckCircle else Icons.Default.Checklist,
                contentDescription = null,
                tint = if (isSelectMode) Color.White else Slate700,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (isSelectMode) "Exit Select" else "Select Mode",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelectMode) Color.White else Slate700
            )
            if (isSelectMode && selectedCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Text(
                        text = selectedCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF7E22CE),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClosetViewModeSwitcher(
    closetViewMode: String,
    onModeChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val modes = listOf(
                Triple("cards", "Cards", Icons.Default.GridView),
                Triple("grid", "Grid", Icons.Default.Apps),
                Triple("table", "Table", Icons.Default.TableRows)
            )
            modes.forEach { (mode, label, icon) ->
                val isSelected = closetViewMode == mode
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) Slate900 else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onModeChange(mode) }
                        .testTag("closet_view_$mode")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Slate600,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Slate600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPillsFlow(
    categoryTabs: List<CategoryTab>,
    selectedCategory: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categoryTabs.forEach { tab ->
            val isSelected = selectedCategory == tab.id
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Slate900 else Slate100,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(tab.id) }
                    .testTag("category_tab_${tab.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) Color.White else Slate700
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White
                    ) {
                        Text(
                            text = "${tab.count}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Slate600,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosetSearchField(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChanged,
        placeholder = {
            Text(
                text = "Search name, material, occasion, or notes...",
                fontSize = 12.sp,
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
                modifier = Modifier.size(16.dp)
            )
        },
        singleLine = true,
        modifier = modifier
            .height(44.dp)
            .testTag("search_by_name_input"),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Slate200,
            focusedBorderColor = BrandEmerald
        )
    )
}

/**
 * Matches Screenshot_20261001-222706.png Garment Card:
 * - Upper image area with:
 *   - Top-Left: #1 Most Washed (16x) dark trophy pill (for top 3)
 *   - Top-Right: Clean (emerald) or Dirty (8.0h) (amber) pill
 *   - Bottom-Left: $85 dark pill + CPW: $5.31/wash white pill
 *   - Bottom-Right: Category pill
 * - Lower body with:
 *   - Title + Total Washed (16x) on right
 *   - Material · thickClothes / seasonsubtitle
 *   - 2x2 Attribute Grid (COMFORT, BREATHABILITY, PROTECTION, DURABILITY)
 *   - Footer: Last washed date + App logged wears (hours)
 */
@Composable
fun GarmentStudioCard(
    garment: GarmentEntity,
    junction: JunctionEntity? = null,
    stats: GarmentStats? = null,
    washRank: Int? = null,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onClick: () -> Unit,
    onUploadPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalWashedCount = garment.wearingFrequency + garment.trackedAppWashes
    val effectiveWears = if (totalWashedCount > 0) totalWashedCount else (stats?.totalWears ?: 0)
    val cpw = if (garment.price > 0 && effectiveWears > 0) {
        String.format(Locale.US, "%.2f", garment.price / effectiveWears)
    } else null

    val resolvedPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
        ImageUtils.resolveImageModel(it, context)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPhoto(uri.toString())
        }
    }

    val isNeedWash = stats?.needWash ?: (junction?.washStated?.equals("need wash", ignoreCase = true) == true)
    val unwashedHours = stats?.unwashedHours ?: if (junction != null && junction.duration > 0) {
        String.format(Locale.US, "%.1f", junction.duration / 3600.0)
    } else "0.0"

    val comfort = deriveComfortRating(garment)
    val breathability = deriveBreathabilityRating(garment)
    val protection = deriveProtectionRating(garment)
    val durability = deriveDurabilityRating(garment)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("garment_studio_card_${garment.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF9333EA) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Upper Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFFF1F5F9))
            ) {
                // Selection Checkbox in Select Mode
                if (isSelectMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF7E22CE) else Color.White.copy(alpha = 0.9f),
                        border = BorderStroke(2.dp, if (isSelected) Color.White else Color(0xFF9333EA)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .size(30.dp)
                            .clickable { onToggleSelect?.invoke() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
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
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = garment.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
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
                                    text = "Upload Photo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Top-Left: Rank Badge (#1 Most Washed (16x))
                if (washRank != null && washRank <= 3 && totalWashedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate900.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "#$washRank Most Washed (${totalWashedCount}x)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-Right: Clean / Dirty (8.0h) Badge
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isNeedWash) Color(0xFFF59E0B) else Color(0xFF10B981),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isNeedWash) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (isNeedWash) "Dirty (${unwashedHours}h)" else "Clean",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Bottom-Left: Price + CPW Pills
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (garment.price > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = "$${garment.price.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (cpw != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = "CPW: $$cpw/wash",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Bottom-Right: Category Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = garment.clothesType.ifBlank { "Garment" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Lower Card Body (Screenshot 222706)
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title + Total Washed Count on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = garment.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val mat = garment.material.ifBlank { "100% Cotton" }
                        val thick = garment.thickClothes.ifBlank { "Year-round" }
                        val extra = listOfNotNull(
                            (garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }).takeIf { it.isNotBlank() },
                            garment.sitBreast.takeIf { it.isNotBlank() }
                        ).joinToString(", ")
                        Text(
                            text = if (extra.isNotBlank()) "$mat · $thick · $extra" else "$mat · $thick",
                            fontSize = 11.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${totalWashedCount}x",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Total Washed",
                            fontSize = 9.sp,
                            color = Slate400
                        )
                    }
                }

                // 2x2 Attribute Grid: COMFORT | BREATHABILITY / PROTECTION | DURABILITY
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttributeMetricBox(
                            label = "COMFORT",
                            value = "$comfort/5",
                            modifier = Modifier.weight(1f)
                        )
                        AttributeMetricBox(
                            label = "BREATHABILITY",
                            value = "$breathability/5",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttributeMetricBox(
                            label = "PROTECTION",
                            value = "$protection/5",
                            modifier = Modifier.weight(1f)
                        )
                        AttributeMetricBox(
                            label = "DURABILITY",
                            value = "$durability/5",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = Slate100)

                // Footer: Last washed & App logged wears
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastWashedDisplay = stats?.latestWashDate
                        ?: garment.lastWashed.ifBlank { "—" }
                    Text(
                        text = "Last washed: $lastWashedDisplay",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                    val appWears = stats?.totalWears ?: 0
                    val appHours = stats?.totalHours ?: "0.0"
                    Text(
                        text = "App logged: $appWears wears (${appHours}h)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                }
            }
        }
    }
}

@Composable
private fun AttributeMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Slate100),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate800
            )
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

/**
 * Matches Screenshot_20261001-224201.png 2-Column (and Tablet/Laptop multi-column) Grid Card:
 * - Square/portrait photo area with:
 *   - Top-Left: #1 Washed dark badge (for top 3)
 *   - Top-Right: Clean (green) or Dirty (amber) pill
 *   - Bottom-Left: $85 dark pill
 * - Compact Footer:
 *   - Garment Name (bold)
 *   - Category on left | "16x  $5.31/w" on right
 */
@Composable
fun GarmentCompactGridCard(
    garment: GarmentEntity,
    stats: GarmentStats? = null,
    washRank: Int? = null,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onClick: () -> Unit,
    onUploadPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalWashedCount = garment.wearingFrequency + garment.trackedAppWashes
    val effectiveWears = if (totalWashedCount > 0) totalWashedCount else (stats?.totalWears ?: 0)
    val cpw = if (garment.price > 0 && effectiveWears > 0) {
        String.format(Locale.US, "%.2f", garment.price / effectiveWears)
    } else null

    val resolvedPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
        ImageUtils.resolveImageModel(it, context)
    }
    val isNeedWash = stats?.needWash ?: false

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
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF9333EA) else Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .background(Color(0xFFF1F5F9))
            ) {
                // Selection Checkbox in Select Mode
                if (isSelectMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF7E22CE) else Color.White.copy(alpha = 0.9f),
                        border = BorderStroke(2.dp, if (isSelected) Color.White else Color(0xFF9333EA)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(24.dp)
                            .clickable { onToggleSelect?.invoke() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
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
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
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

                // Top-left: #1 Washed dark badge
                if (washRank != null && washRank <= 3 && totalWashedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate900.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "#$washRank Washed",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Top-right: Clean / Dirty pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isNeedWash) Color(0xFFF59E0B) else Color(0xFF10B981),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isNeedWash) "Dirty" else "Clean",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Bottom-left: $85 dark pill
                if (garment.price > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate900.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "$${garment.price.toInt()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = garment.clothesType.ifBlank { "Item" },
                        fontSize = 10.sp,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${totalWashedCount}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669)
                        )
                        if (cpw != null) {
                            Text(
                                text = "$$cpw/w",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Matches Screenshot_20261001-224404.png Table View:
 * Columns: ITEM | CATEGORY | STATUS | WASHED | PRICE | CPW ($/W) | COMFORT / DURABILITY | PROTECTION
 */
@Composable
fun GarmentClosetTable(
    garments: List<GarmentEntity>,
    statsById: Map<String, GarmentStats>,
    isSelectMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelect: ((String) -> Unit)? = null,
    minTableWidth: Dp = 860.dp,
    onGarmentClick: (GarmentEntity) -> Unit
) {
    val context = LocalContext.current
    val hScroll = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                    .width(minTableWidth)
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectMode) {
                    Text("SEL", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.width(36.dp))
                }
                Text("ITEM", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(2.2f))
                Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.0f))
                Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.1f))
                Text("WASHED", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.9f))
                Text("PRICE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.7f))
                Text("CPW ($/W)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.9f))
                Text("COMFORT / DURABILITY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(1.4f))
                Text("PROTECTION", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Slate500, modifier = Modifier.weight(0.9f))
            }
            HorizontalDivider(color = Slate200)

            garments.forEach { garment ->
                val stats = statsById[garment.id]
                val totalWashes = garment.wearingFrequency + garment.trackedAppWashes
                val effectiveWears = if (totalWashes > 0) totalWashes else (stats?.totalWears ?: 0)
                val cpw = if (garment.price > 0 && effectiveWears > 0) {
                    String.format(Locale.US, "$%.2f", garment.price / effectiveWears)
                } else "—"
                val resolvedPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
                    ImageUtils.resolveImageModel(it, context)
                }
                val isDirty = stats?.needWash == true
                val isSelected = selectedIds.contains(garment.id)

                Row(
                    modifier = Modifier
                        .width(minTableWidth)
                        .background(if (isSelected) Color(0xFFFAF5FF) else Color.Transparent)
                        .clickable { onGarmentClick(garment) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectMode) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF7E22CE) else Color.White,
                            border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF7E22CE) else Slate400),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onToggleSelect?.invoke(garment.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    // ITEM (Avatar + Name + Material)
                    Row(
                        modifier = Modifier.weight(2.2f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100),
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
                                Icon(
                                    imageVector = Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = garment.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${garment.material.ifBlank { "Cotton" }} · ${garment.thickClothes.ifBlank { "Normal" }}",
                                fontSize = 10.sp,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // CATEGORY
                    Box(modifier = Modifier.weight(1.0f)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate100
                        ) {
                            Text(
                                text = garment.clothesType.ifBlank { "Garment" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // STATUS
                    Box(modifier = Modifier.weight(1.1f)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = if (isDirty) "Dirty (${stats?.unwashedHours ?: "0.0"}h)" else "Clean",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirty) Color(0xFFB45309) else Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // WASHED
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text(
                            text = "${totalWashes}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "${stats?.totalWears ?: 0} app logs",
                            fontSize = 9.sp,
                            color = Slate400
                        )
                    }

                    // PRICE
                    Text(
                        text = if (garment.price > 0) "$${garment.price.toInt()}" else "—",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.weight(0.7f)
                    )

                    // CPW ($/W)
                    Text(
                        text = cpw,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600,
                        modifier = Modifier.weight(0.9f)
                    )

                    // COMFORT / DURABILITY
                    Text(
                        text = "C:${deriveComfortRating(garment)}/5 · D:${deriveDurabilityRating(garment)}/5",
                        fontSize = 11.sp,
                        color = Slate700,
                        modifier = Modifier.weight(1.4f)
                    )

                    // PROTECTION
                    Text(
                        text = "${deriveProtectionRating(garment)}/5",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                HorizontalDivider(color = Slate100)
            }
        }
    }
}

/**
 * Batch Tag Manager Banner Card (matching web app A2 selection toolbar).
 * Allows batch selecting clothes and adding/removing tags like "ai", "unai", etc.
 */
@Composable
fun BatchTagManagerCard(
    totalCount: Int,
    selectedCount: Int,
    targetTag: String,
    onTargetTagChange: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onExitSelect: () -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Color(0xFF9333EA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Selected counter + Select All / Deselect All + Exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF3E8FF)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF7E22CE),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$selectedCount of $totalCount Selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6B21A8)
                            )
                        }
                    }

                    TextButton(
                        onClick = onSelectAll,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Select All ($totalCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                    }

                    Text("•", color = Slate400, fontSize = 12.sp)

                    TextButton(
                        onClick = onDeselectAll,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Deselect All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Slate600)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier.clickable { onExitSelect() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Exit Selection",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }
                }
            }

            // Row 2: Target Tag Controls
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFAF5FF),
                border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = Color(0xFF7E22CE),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Target Tag:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        }

                        // Preset chips: 🤖 ai, unai
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (targetTag.equals("ai", ignoreCase = true)) Color(0xFF7E22CE) else Color.White,
                                border = BorderStroke(1.dp, if (targetTag.equals("ai", ignoreCase = true)) Color(0xFF7E22CE) else Color(0xFFE9D5FF)),
                                modifier = Modifier.clickable { onTargetTagChange("ai") }
                            ) {
                                Text(
                                    text = "🤖 ai",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetTag.equals("ai", ignoreCase = true)) Color.White else Color(0xFF6B21A8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (targetTag.equals("unai", ignoreCase = true)) Slate800 else Color.White,
                                border = BorderStroke(1.dp, if (targetTag.equals("unai", ignoreCase = true)) Slate800 else Slate400),
                                modifier = Modifier.clickable { onTargetTagChange("unai") }
                            ) {
                                Text(
                                    text = "unai",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetTag.equals("unai", ignoreCase = true)) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = targetTag,
                            onValueChange = onTargetTagChange,
                            placeholder = { Text("Type tag name...", fontSize = 11.sp, color = Slate400) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Slate200,
                                focusedBorderColor = Color(0xFF7E22CE)
                            )
                        )
                    }

                    // Action buttons: Add Tag, Remove Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onAddTag,
                            enabled = selectedCount > 0 && targetTag.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7E22CE),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Add Tag \"${targetTag.ifBlank { "..." }}\"",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onRemoveTag,
                            enabled = selectedCount > 0 && targetTag.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFBE123C)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Remove Tag",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
