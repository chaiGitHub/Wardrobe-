package com.example.ui.dialogs

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.ui.drilldown.ALL_AVAILABLE_FIELDS
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.drilldown.ReportFieldDef
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
import org.json.JSONArray
import java.util.Locale

/**
 * Filter field definition with associated icon and display title
 */
data class ClothesFilterField(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val category: String = "general"
)

// Standard 13 filter fields matching closet module screenshot
val DEFAULT_CLOTHES_FILTER_KEYS = listOf(
    "material",
    "Clothes Type",
    "side breast ok",
    "beautiful",
    "wash stated",
    "tags",
    "thick clothes",
    "Hot at noon",
    "when with aircond: hot feeling",
    "when without aircond: hot feeling",
    "sit breast ok",
    "diao dai",
    "long dress"
)

fun getFilterFieldIcon(key: String): ImageVector {
    return when {
        key.contains("material", ignoreCase = true) -> Icons.Default.Spa
        key.contains("Clothes Type", ignoreCase = true) || key.contains("dress", ignoreCase = true) -> Icons.Default.Checkroom
        key.contains("breast", ignoreCase = true) || key.contains("chest", ignoreCase = true) -> Icons.Default.Shield
        key.contains("beautiful", ignoreCase = true) -> Icons.Default.Star
        key.contains("wash", ignoreCase = true) -> Icons.Default.LocalLaundryService
        key.contains("tag", ignoreCase = true) -> Icons.Default.Label
        key.contains("aircond", ignoreCase = true) || key.contains("hot", ignoreCase = true) || key.contains("noon", ignoreCase = true) -> Icons.Default.Spa
        else -> Icons.Default.FilterAlt
    }
}

fun getFilterFieldLabel(key: String): String {
    return when (key) {
        "material" -> "Material (材质)"
        "Clothes Type" -> "Clothes Type (种类)"
        "side breast ok" -> "Side breast ok (副乳包裹)"
        "beautiful" -> "Appearance (美观评级)"
        "wash stated" -> "Laundry Status (洗护状态)"
        "tags" -> "Tags (标签)"
        "thick clothes" -> "Thickness (厚薄)"
        "Hot at noon" -> "Hot at noon (中午热感)"
        "when with aircond: hot feeling" -> "With Aircond: Hot feeling"
        "when without aircond: hot feeling" -> "Without Aircond: Hot feeling"
        "sit breast ok" -> "Sit breast ok (坐姿胸型)"
        "diao dai" -> "Shoulder Strap (吊带)"
        "long dress" -> "Dress Length (裙长)"
        "wet" -> "Moisture (排汗)"
        "price" -> "Price Range"
        "wearing frequency(how many times legacy washed)" -> "Legacy Wash Count"
        "Total Washed Count" -> "Total Washed Count"
        else -> ALL_AVAILABLE_FIELDS.find { it.key == key }?.label ?: key
    }
}

/**
 * Modal dialog for filtering clothes by any combination of attributes
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClothesFilterModal(
    isOpen: Boolean,
    clothes: List<GarmentEntity>,
    junctions: List<JunctionEntity>,
    activeFilters: Map<String, Set<String>>,
    onApplyFilters: (Map<String, Set<String>>) -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clothes_filter_prefs_v1", Context.MODE_PRIVATE) }

    // Manage visible filter field keys (persisted in SharedPreferences)
    var visibleFieldKeys by remember {
        val saved = prefs.getStringSet("visible_fields_v1", null)
        mutableStateOf(saved?.toList() ?: DEFAULT_CLOTHES_FILTER_KEYS)
    }

    // Active filter selection (fieldKey -> set of selected values)
    val workingFilters = remember(activeFilters) {
        mutableStateMapOf<String, Set<String>>().apply {
            putAll(activeFilters)
        }
    }

    // Modal tabs: 0 = "Apply Filters", 1 = "Manage Filter Fields"
    var selectedTab by remember { mutableStateOf(0) }

    // Search query within Manage Filter Fields
    var manageFieldSearchQuery by remember { mutableStateOf("") }

    // Field-level search queries in Apply Filters
    val fieldSearchQueries = remember { mutableStateMapOf<String, String>() }

    // Intercept physical or gesture back button
    BackHandler(enabled = isOpen) {
        onApplyFilters(workingFilters.toMap())
        onClose()
    }

    // Precalculate distinct options and counts for all available fields
    val optionsByField = remember(clothes, junctions) {
        val map = mutableMapOf<String, List<Pair<String, Int>>>()

        // 1. Tags
        val tagCounts = mutableMapOf<String, Int>()
        clothes.forEach { g ->
            val parsedTags = mutableListOf<String>()
            try {
                val arr = JSONArray(g.tags)
                for (i in 0 until arr.length()) {
                    val t = arr.getString(i).trim()
                    if (t.isNotBlank()) parsedTags.add(t)
                }
            } catch (e: Exception) {
                g.tags.split(",").forEach {
                    val t = it.trim()
                    if (t.isNotBlank()) parsedTags.add(t)
                }
            }
            parsedTags.distinct().forEach { tag ->
                val displayTag = if (tag.startsWith("#")) tag else "#$tag"
                tagCounts[displayTag] = (tagCounts[displayTag] ?: 0) + 1
            }
        }
        if (tagCounts.isNotEmpty()) {
            map["tags"] = tagCounts.toList().sortedByDescending { it.second }
        }

        // 2. Laundry Status
        val statsById = clothes.associate { it.id to DrilldownUtils.calculateGarmentStats(it.id, junctions) }
        val cleanCount = statsById.values.count { !it.needWash }
        val needWashCount = statsById.values.count { it.needWash }
        val washList = mutableListOf<Pair<String, Int>>()
        if (cleanCount > 0) washList.add("clean / washed" to cleanCount)
        if (needWashCount > 0) washList.add("need wash" to needWashCount)
        map["wash stated"] = washList

        // 3. Other fields from ALL_AVAILABLE_FIELDS
        ALL_AVAILABLE_FIELDS.forEach { fieldDef ->
            val fieldKey = fieldDef.key
            if (fieldKey != "wash stated" && fieldKey != "Name" && fieldKey != "Descriptions" && fieldKey != "Descriptions 2") {
                val valueCounts = mutableMapOf<String, Int>()
                clothes.forEach { g ->
                    val v = DrilldownUtils.getFieldValue(g, fieldKey, junctions)
                    if (v.isNotBlank() && !v.equals("undefined", ignoreCase = true) && !v.equals("(Not specified)", ignoreCase = true)) {
                        valueCounts[v] = (valueCounts[v] ?: 0) + 1
                    }
                }
                if (valueCounts.isNotEmpty()) {
                    map[fieldKey] = valueCounts.toList().sortedByDescending { it.second }
                }
            }
        }
        map
    }

    val totalActiveConditions = workingFilters.values.sumOf { it.size }

    Dialog(
        onDismissRequest = {
            onApplyFilters(workingFilters.toMap())
            onClose()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val maxModalHeight = maxHeight * 0.92f
            val isTablet = maxWidth >= 600.dp

            Surface(
                modifier = Modifier
                    .widthIn(max = if (isTablet) 720.dp else 520.dp)
                    .fillMaxWidth(if (isTablet) 0.85f else 0.95f)
                    .heightIn(max = maxModalHeight)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("clothes_filter_modal"),
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. TOP HEADER (Icon, Title, Subtitle, Active Count Badge, Clear All, Close X)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Clothes Filters",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (totalActiveConditions > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF9333EA)
                                    ) {
                                        Text(
                                            text = "$totalActiveConditions active",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${visibleFieldKeys.size} filter fields visible • Follows your wardrobe data",
                                fontSize = 11.sp,
                                color = Slate500,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (totalActiveConditions > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { workingFilters.clear() }
                                    .testTag("clear_all_filters_button")
                            ) {
                                Text(
                                    text = "Clear all",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE11D48),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyFilters(workingFilters.toMap())
                                onClose()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Slate500,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // 2. TAB CONTROLS (Apply Filters vs Manage Filter Fields)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 0 }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) BrandPurple else Slate600,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Apply Filters (${visibleFieldKeys.size} Fields)",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) BrandPurple else Slate600
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 1 }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) BrandPurple else Slate600,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Manage Filter Fields (${visibleFieldKeys.size}/${ALL_AVAILABLE_FIELDS.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) BrandPurple else Slate600
                                )
                            }
                        }
                    }

                    // 3. TAB CONTENT (Scrollable Column)
                    val maxScrollHeight = (maxModalHeight - 150.dp).coerceAtLeast(220.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxScrollHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedTab == 0) {
                            // TAB 0: APPLY FILTERS
                            // Info Card with link to Manage Fields
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFF7E22CE),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "DATA-DRIVEN WARDROBE FILTERS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF7E22CE)
                                            )
                                        }
                                        Text(
                                            text = "⚙️ 增减字段",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7E22CE),
                                            modifier = Modifier
                                                .clickable { selectedTab = 1 }
                                                .padding(4.dp)
                                        )
                                    }
                                    Text(
                                        text = "Every option is extracted from your actual clothes records (like lace, plain, and custom ratings). Untick unwanted fields in \"Manage Filter Fields\".",
                                        fontSize = 11.sp,
                                        color = Slate600,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            // Filter Field Cards
                            visibleFieldKeys.forEach { fieldKey ->
                                val options = optionsByField[fieldKey].orEmpty()
                                val selectedValues = workingFilters[fieldKey].orEmpty()
                                val fieldLabel = getFilterFieldLabel(fieldKey)
                                val fieldIcon = getFilterFieldIcon(fieldKey)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Field Header Row
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
                                                    imageVector = fieldIcon,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6D28D9),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = fieldLabel,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate800
                                                )
                                            }
                                            if (options.isNotEmpty()) {
                                                val allSelected = selectedValues.size == options.size
                                                Text(
                                                    text = if (allSelected) "Clear" else "Select all",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandPurple,
                                                    modifier = Modifier
                                                        .clickable {
                                                            if (allSelected) {
                                                                workingFilters.remove(fieldKey)
                                                            } else {
                                                                workingFilters[fieldKey] = options.map { it.first }.toSet()
                                                            }
                                                        }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Optional search for fields with many values
                                        if (options.size > 5) {
                                            var query by remember(fieldKey) {
                                                mutableStateOf(fieldSearchQueries[fieldKey] ?: "")
                                            }
                                            OutlinedTextField(
                                                value = query,
                                                onValueChange = {
                                                    query = it
                                                    fieldSearchQueries[fieldKey] = it
                                                },
                                                placeholder = {
                                                    Text(
                                                        text = "Filter ${options.size} $fieldLabel values...",
                                                        fontSize = 11.sp,
                                                        color = Slate400
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = null,
                                                        tint = Slate400,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (query.isNotEmpty()) {
                                                        IconButton(onClick = {
                                                            query = ""
                                                            fieldSearchQueries.remove(fieldKey)
                                                        }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Clear",
                                                                tint = Slate400,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedBorderColor = Slate200,
                                                    focusedBorderColor = BrandPurple,
                                                    unfocusedContainerColor = Color(0xFFF8FAFC),
                                                    focusedContainerColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp)
                                            )
                                        }

                                        val query = (fieldSearchQueries[fieldKey] ?: "").trim().lowercase(Locale.ROOT)
                                        val filteredOptions = if (query.isEmpty()) {
                                            options
                                        } else {
                                            options.filter { it.first.lowercase(Locale.ROOT).contains(query) }
                                        }

                                        if (filteredOptions.isEmpty()) {
                                            Text(
                                                text = if (options.isEmpty()) "No data found in current wardrobe records" else "No matching values",
                                                fontSize = 11.sp,
                                                color = Slate400,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        } else {
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                filteredOptions.forEach { (optionValue, count) ->
                                                    val isSelected = selectedValues.contains(optionValue)
                                                    Surface(
                                                        shape = RoundedCornerShape(20.dp),
                                                        color = if (isSelected) Color(0xFF7E22CE) else Color(0xFFF8FAFC),
                                                        border = BorderStroke(
                                                            width = 1.dp,
                                                            color = if (isSelected) Color(0xFF7E22CE) else Color(0xFFE2E8F0)
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(20.dp))
                                                            .clickable {
                                                                val currentSet = workingFilters[fieldKey].orEmpty().toMutableSet()
                                                                if (isSelected) {
                                                                    currentSet.remove(optionValue)
                                                                } else {
                                                                    currentSet.add(optionValue)
                                                                }
                                                                if (currentSet.isEmpty()) {
                                                                    workingFilters.remove(fieldKey)
                                                                } else {
                                                                    workingFilters[fieldKey] = currentSet
                                                                }
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else Slate400)
                                                            )
                                                            Text(
                                                                text = optionValue,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) Color.White else Slate700
                                                            )
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = if (isSelected) Color(0xFF6B21A8) else Slate200
                                                            ) {
                                                                Text(
                                                                    text = count.toString(),
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isSelected) Color.White else Slate600,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
                        } else {
                            // TAB 1: MANAGE FILTER FIELDS
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SELECT FIELDS TO SHOW AS FILTERS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF3E8FF),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val allKeys = ALL_AVAILABLE_FIELDS.map { it.key }
                                                    visibleFieldKeys = allKeys
                                                    prefs.edit().putStringSet("visible_fields_v1", allKeys.toSet()).apply()
                                                }
                                        ) {
                                            Text(
                                                text = "Select All",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandPurple,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Slate200),
                                            color = Color.White,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    visibleFieldKeys = DEFAULT_CLOTHES_FILTER_KEYS
                                                    prefs.edit().putStringSet("visible_fields_v1", DEFAULT_CLOTHES_FILTER_KEYS.toSet()).apply()
                                                }
                                        ) {
                                            Text(
                                                text = "Reset (${DEFAULT_CLOTHES_FILTER_KEYS.size})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = manageFieldSearchQuery,
                                    onValueChange = { manageFieldSearchQuery = it },
                                    placeholder = { Text("Search filter fields...", fontSize = 12.sp, color = Slate400) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Slate400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Slate200,
                                        focusedBorderColor = BrandPurple
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                )

                                val q = manageFieldSearchQuery.trim().lowercase(Locale.ROOT)
                                val availableFieldDefs = ALL_AVAILABLE_FIELDS.filter {
                                    it.key != "Name" && it.key != "Descriptions" && it.key != "Descriptions 2"
                                }.filter {
                                    q.isEmpty() || it.label.lowercase(Locale.ROOT).contains(q) || it.key.lowercase(Locale.ROOT).contains(q)
                                }

                                availableFieldDefs.forEach { fieldDef ->
                                    val isChecked = visibleFieldKeys.contains(fieldDef.key)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val updated = if (isChecked) {
                                                    visibleFieldKeys.filter { it != fieldDef.key }
                                                } else {
                                                    visibleFieldKeys + fieldDef.key
                                                }
                                                visibleFieldKeys = updated
                                                prefs.edit().putStringSet("visible_fields_v1", updated.toSet()).apply()
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChecked) Color(0xFFFBF8FF) else Color.White
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isChecked) Color(0xFFE9D5FF) else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    val updated = if (checked) {
                                                        visibleFieldKeys + fieldDef.key
                                                    } else {
                                                        visibleFieldKeys.filter { it != fieldDef.key }
                                                    }
                                                    visibleFieldKeys = updated
                                                    prefs.edit().putStringSet("visible_fields_v1", updated.toSet()).apply()
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF9333EA)
                                                )
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = getFilterFieldLabel(fieldDef.key),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate800
                                                )
                                                if (fieldDef.description.isNotBlank()) {
                                                    Text(
                                                        text = fieldDef.description,
                                                        fontSize = 10.sp,
                                                        color = Slate500,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    HorizontalDivider(color = Slate100)

                    // 4. BOTTOM STICKY BAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (totalActiveConditions > 0) {
                                "$totalActiveConditions filter condition${if (totalActiveConditions > 1) "s" else ""} applied"
                            } else {
                                "No filters applied"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalActiveConditions > 0) Color(0xFF7E22CE) else Slate500
                        )

                        Button(
                            onClick = {
                                onApplyFilters(workingFilters.toMap())
                                onClose()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.testTag("apply_clothes_filters_done_button")
                        ) {
                            Text(
                                text = "Done / View Results",
                                fontSize = 12.sp,
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
