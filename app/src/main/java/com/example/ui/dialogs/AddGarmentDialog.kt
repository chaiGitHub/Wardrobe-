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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.ErgonomicExplainField
import com.example.ui.components.EnvironmentalScenariosEditSection
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.GarmentEntity
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
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

@Composable
fun AddGarmentDialog(
    existingClothes: List<GarmentEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (GarmentEntity) -> Unit
) {
    val context = LocalContext.current

    // Basic fields
    var name by remember { mutableStateOf("") }
    var clothesType by remember { mutableStateOf("Dress") }
    var material by remember { mutableStateOf("soft chiffon") }
    var priceStr by remember { mutableStateOf("") }

    // Ergonomics & Attributes (from AddClothesModal.tsx)
    var thickClothes by remember { mutableStateOf("01 not thick clothes") }
    var hotAtNoon by remember { mutableStateOf("Not hot") }
    var diaoDai by remember { mutableStateOf("diao dai") }

    // Dedicated Side Breast Bra fields (2 fields with previous pickers and explain notes)
    var sideBreastGatherBra by remember { mutableStateOf("side breast shape ok") }
    var sideBreastSideOutBra by remember { mutableStateOf("side breast shape ok") }

    // 3 Environmental Scenarios (With aircond, without aircond, sleep under fan)
    var aircondHotFeeling by remember { mutableStateOf("not so cold") }
    var aircondHotExplain by remember { mutableStateOf("due to thick chiffon material, thick clothes so no so cold.") }
    var aircondLegFeeling by remember { mutableStateOf("not so cold") }
    var aircondLegExplain by remember { mutableStateOf("due to middle long dress, thick chiffon material, thick clothes.") }

    var noAircondHotFeeling by remember { mutableStateOf("hot") }
    var noAircondHotExplain by remember { mutableStateOf("due to thick chiffon material, thick clothes.") }
    var noAircondLegFeeling by remember { mutableStateOf("middle hot") }
    var noAircondLegExplain by remember { mutableStateOf("due to middle long dress, thick chiffon material, thick clothes.little more swear") }

    var fanSleepHotFeeling by remember { mutableStateOf("not hot") }
    var fanSleepHotExplain by remember { mutableStateOf("under fan wind bigger and cold bed help reduce feeling of hot") }
    var fanSleepLegFeeling by remember { mutableStateOf("middle cold") }
    var fanSleepLegExplain by remember { mutableStateOf("due to middle long dress, inner thin, easy fold up 裙底，even it is thick clothes outer") }

    // Dedicated Sit Breast Bra fields
    var sitBreastGatherBra by remember { mutableStateOf("sit breast shape ok") }
    var sitBreastSideOutBra by remember { mutableStateOf("sit breast shape ok") }
    var sitBreast by remember { mutableStateOf("sit breast shape ok") }
    var centerChest by remember { mutableStateOf("no center chest") }
    var beautiful by remember { mutableStateOf("Beautiful") }
    var beautifulReason by remember { mutableStateOf("") }

    // Screenshot focus fields: WET, WET REASON, LONG DRESS, DESCRIPTIONS, LAST COMMENT OR ACTION
    var wet by remember { mutableStateOf("dry") }
    var wetReason by remember { mutableStateOf("") }
    var longDress by remember { mutableStateOf("short dress") }
    var descriptions by remember { mutableStateOf("") }
    var lastComment by remember { mutableStateOf("") }

    // Field-level explanations for new garments matching webapp _fieldExplanations
    val fieldExplanations = remember { mutableStateMapOf<String, String>() }
    val expandedExplainFields = remember { mutableStateMapOf<String, Boolean>() }
    val dictionaryService = remember { FieldDictionaryService(context) }

    // Uploaded photo URIs/strings
    val photoUrls = remember { mutableStateListOf<String>() }

    // Photo picker launcher (Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            photoUrls.add(uri.toString())
        }
    }

    // Active PreviousEntriesModal state: fieldName to onSelect callback
    var activePreviousPicker by remember {
        mutableStateOf<Pair<String, (String) -> Unit>?>(null)
    }

    // Helper to calculate previous values for a field from existing clothes
    fun getPreviousValuesForField(fieldKey: String): List<PreviousEntry> {
        val map = mutableMapOf<String, Int>()
        val sourcesMap = mutableMapOf<String, MutableSet<String>>()
        existingClothes.forEach { g ->
            val v = when (fieldKey) {
                "Clothes Type" -> g.clothesType
                "material" -> g.material
                "thick clothes" -> g.thickClothes
                "Hot at noon" -> g.noAircondHotFeeling.ifBlank { g.hotAtNoon }
                "diao dai" -> g.diaoDai
                "side breast ok", "side breast ok: with 聚拢 bra" -> g.sideBreastGatherBra.ifBlank { g.sideBreastOk }
                "side breast ok: with side out bra" -> g.sideBreastSideOutBra
                "when with aircond: hot feeling" -> g.aircondHotFeeling
                "when with aircond: leg feeling" -> g.aircondLegFeeling
                "when without aircond: hot feeling" -> g.noAircondHotFeeling.ifBlank { g.hotAtNoon }
                "when without aircond: leg feeling" -> g.noAircondLegFeeling
                "when sleep under fan: hot feeling" -> g.fanSleepHotFeeling
                "when sleep under fan: leg feeling" -> g.fanSleepLegFeeling
                "sit breast ok", "sit breast ok: With 聚拢bra:", "sit breast ok: with 聚拢 bra" -> g.sitBreastGatherBra.ifBlank { g.sitBreast }
                "sit breast ok: with side out bra" -> g.sitBreastSideOutBra
                "center chest" -> g.standBreast
                "beautiful" -> g.beautiful
                "wet" -> g.wet
                "wet reason" -> ""
                "long dress" -> ""
                "Descriptions" -> g.lastComment
                "last comment or action" -> ""
                else -> ""
            }.trim()
            if (v.isNotBlank() && v != "(Not specified)") {
                map[v] = (map[v] ?: 0) + 1
                val gName = g.name.ifBlank { "Garment" }.trim()
                if (gName.isNotBlank()) {
                    sourcesMap.getOrPut(v) { mutableSetOf() }.add(gName)
                }
            }
        }

        // Add standard default presets if map is empty
        when (fieldKey) {
            "wet" -> if (map.isEmpty()) listOf("dry", "wet", "quick-dry", "semi-dry").forEach { map[it] = 1 }
            "wet reason" -> if (map.isEmpty()) listOf("quick-drying fabric", "air hung dry", "dryer low heat", "cotton breath").forEach { map[it] = 1 }
            "long dress" -> if (map.isEmpty()) listOf("short dress", "long dress", "midi dress", "maxi dress", "knee length", "above knee").forEach { map[it] = 1 }
            "Descriptions" -> if (map.isEmpty()) listOf("Styling notes, fit details...", "irrate body, beast no beautiful, hard to wear, hot, throw", "comfortable daily wear", "silk smooth breathable").forEach { map[it] = 1 }
            "last comment or action" -> if (map.isEmpty()) listOf("Needs gentle cycle", "hand wash only", "hang dry immediately", "iron reverse").forEach { map[it] = 1 }
            "Clothes Type" -> if (map.isEmpty()) listOf("Dress", "Pant", "Tshirt", "bra", "skirt", "sweater", "jacket", "coat", "top", "blouse", "cardigan", "shorts", "Other").forEach { map[it] = 1 }
            "Hot at noon", "when without aircond: hot feeling" -> if (map.isEmpty()) listOf("hot", "middle hot", "not hot", "very hot", "stuffy").forEach { map[it] = 1 }
            "when without aircond: leg feeling" -> if (map.isEmpty()) listOf("middle hot", "hot", "not hot", "sticky", "breathable").forEach { map[it] = 1 }
            "when with aircond: hot feeling" -> if (map.isEmpty()) listOf("not so cold", "cold", "comfortable cool", "very cold", "not cold").forEach { map[it] = 1 }
            "when with aircond: leg feeling" -> if (map.isEmpty()) listOf("not so cold", "cold", "comfortable", "chilly knees").forEach { map[it] = 1 }
            "when sleep under fan: hot feeling" -> if (map.isEmpty()) listOf("not hot", "middle cold", "comfortable", "cool", "sweaty back").forEach { map[it] = 1 }
            "when sleep under fan: leg feeling" -> if (map.isEmpty()) listOf("middle cold", "cold", "not hot", "comfortable", "breezy").forEach { map[it] = 1 }
            "diao dai" -> if (map.isEmpty()) listOf("diao dai", "not diao dai", "wide strap", "halter").forEach { map[it] = 1 }
            "side breast ok", "side breast ok: with 聚拢 bra" -> if (map.isEmpty()) listOf("side breast shape ok", "no friction", "well gathered, no overflow", "mild bulge", "friction at armpit").forEach { map[it] = 1 }
            "side breast ok: with side out bra" -> if (map.isEmpty()) listOf("side breast shape ok", "full coverage no overflow", "no friction", "slight side exposure", "wide band comfortable").forEach { map[it] = 1 }
            "sit breast ok", "sit breast ok: With 聚拢bra:", "sit breast ok: with 聚拢 bra" -> if (map.isEmpty()) listOf("sit breast shape ok", "tight when seated", "very loose", "no underwire pressure", "comfortable when seated").forEach { map[it] = 1 }
            "sit breast ok: with side out bra" -> if (map.isEmpty()) listOf("sit breast shape ok", "full coverage relaxed", "no side bulge when seated", "tight underwire", "relaxed fit").forEach { map[it] = 1 }
            "beautiful" -> if (map.isEmpty()) listOf("Beautiful", "not so beautiful", "Standard", "Casual").forEach { map[it] = 1 }
            "thick clothes" -> if (map.isEmpty()) listOf("01 not thick clothes", "02 medium", "03 thick clothes", "ultra-thin").forEach { map[it] = 1 }
        }

        val dictFields = try { dictionaryService.getFields() } catch (_: Exception) { emptyList() }
        val normKey = fieldKey.trim().lowercase(java.util.Locale.ROOT)
        val dictValues = dictFields.find {
            it.name.trim().lowercase(java.util.Locale.ROOT) == normKey ||
            it.name.replace(" ", "").lowercase(java.util.Locale.ROOT) == normKey.replace(" ", "")
        }?.typicalValues ?: emptyList()

        return map.entries.map { e ->
            val typical = dictValues.find { it.valueEn.equals(e.key, ignoreCase = true) }
            val labelCn = typical?.valueCn?.ifBlank { null }?.takeIf { it != e.key }
            val meaningCn = typical?.meaningCn?.ifBlank { null } ?: "真实衣物历史记录值: \"${e.key}\""
            PreviousEntry(
                value = e.key,
                count = e.value,
                labelCn = labelCn,
                meaningCn = meaningCn,
                sources = sourcesMap[e.key]?.toList() ?: emptyList()
            )
        }.sortedByDescending { it.count }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("add_garment_dialog"),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (matching screenshot: Back button, Purple checkroom badge, "Add New Clothes", close 'x')
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Add New Clothes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 17.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Scrollable Form Container
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card 1: Basic Information
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFAF5FF),
                        border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "BASIC CLOTHES INFORMATION",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )

                            // Name
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "NAME *",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("e.g. 062 black knitted top", fontSize = 13.sp, color = Slate400) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = Slate200,
                                        focusedBorderColor = BrandPurple
                                    )
                                )
                                ErgonomicExplainField(
                                    fieldKey = "Name",
                                    fieldLabel = "Garment Name",
                                    value = fieldExplanations["Name"] ?: "",
                                    onValueChange = { fieldExplanations["Name"] = it },
                                    isExpanded = expandedExplainFields["Name"] == true,
                                    onToggleExpand = { expandedExplainFields["Name"] = !(expandedExplainFields["Name"] ?: false) },
                                    allClothes = existingClothes,
                                    customPlaceholder = "补充命名原因或此单品特别备注...",
                                    dictionaryService = dictionaryService
                                )
                            }

                            // Clothes Type with Previous
                            FieldWithPreviousButton(
                                label = "CLOTHES TYPE *",
                                value = clothesType,
                                placeholder = "e.g. Dress, Pant, Shoe, Tshirt, Bra...",
                                onValueChange = { clothesType = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "Clothes Type" to { clothesType = it }
                                },
                                fieldKey = "Clothes Type",
                                explainValue = fieldExplanations["Clothes Type"] ?: "",
                                onExplainChange = { fieldExplanations["Clothes Type"] = it },
                                isExplainExpanded = expandedExplainFields["Clothes Type"] == true,
                                onToggleExplainExpand = { expandedExplainFields["Clothes Type"] = !(expandedExplainFields["Clothes Type"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Material with Previous
                            FieldWithPreviousButton(
                                label = "MATERIAL",
                                value = material,
                                placeholder = "e.g. soft chiffon, silk",
                                onValueChange = { material = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "material" to { material = it }
                                },
                                fieldKey = "material",
                                explainValue = fieldExplanations["material"] ?: "",
                                onExplainChange = { fieldExplanations["material"] = it },
                                isExplainExpanded = expandedExplainFields["material"] == true,
                                onToggleExplainExpand = { expandedExplainFields["material"] = !(expandedExplainFields["material"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Price
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "PRICE ($)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                OutlinedTextField(
                                    value = priceStr,
                                    onValueChange = { priceStr = it },
                                    placeholder = { Text("e.g. 59.90", fontSize = 13.sp, color = Slate400) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.White,
                                        focusedContainerColor = Color.White,
                                        unfocusedBorderColor = Slate200,
                                        focusedBorderColor = BrandPurple
                                    )
                                )
                                ErgonomicExplainField(
                                    fieldKey = "price",
                                    fieldLabel = "Price",
                                    value = fieldExplanations["price"] ?: "",
                                    onValueChange = { fieldExplanations["price"] = it },
                                    isExpanded = expandedExplainFields["price"] == true,
                                    onToggleExpand = { expandedExplainFields["price"] = !(expandedExplainFields["price"] ?: false) },
                                    allClothes = existingClothes,
                                    customPlaceholder = "价格、购入渠道或性价比体感备注...",
                                    dictionaryService = dictionaryService
                                )
                            }
                        }
                    }

                    // Card 2: Attributes & Ergonomics (Matching screenshot layout)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "CLOTHES ATTRIBUTES & ERGONOMICS",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )

                            // Thick Clothes
                            FieldWithPreviousButton(
                                label = "THICK CLOTHES",
                                value = thickClothes,
                                placeholder = "01 not thick clothes",
                                onValueChange = { thickClothes = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "thick clothes" to { thickClothes = it }
                                },
                                fieldKey = "thick clothes",
                                explainValue = fieldExplanations["thick clothes"] ?: "",
                                onExplainChange = { fieldExplanations["thick clothes"] = it },
                                isExplainExpanded = expandedExplainFields["thick clothes"] == true,
                                onToggleExplainExpand = { expandedExplainFields["thick clothes"] = !(expandedExplainFields["thick clothes"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // 3 Environmental Scenarios Expandable Cards
                            EnvironmentalScenariosEditSection(
                                aircondHotFeeling = aircondHotFeeling,
                                onAircondHotChange = { aircondHotFeeling = it },
                                onAircondHotPrevious = {
                                    activePreviousPicker = "when with aircond: hot feeling" to { aircondHotFeeling = it }
                                },
                                aircondHotExplain = aircondHotExplain,
                                onAircondHotExplainChange = { aircondHotExplain = it },
                                aircondLegFeeling = aircondLegFeeling,
                                onAircondLegChange = { aircondLegFeeling = it },
                                onAircondLegPrevious = {
                                    activePreviousPicker = "when with aircond: leg feeling" to { aircondLegFeeling = it }
                                },
                                aircondLegExplain = aircondLegExplain,
                                onAircondLegExplainChange = { aircondLegExplain = it },

                                noAircondHotFeeling = noAircondHotFeeling,
                                onNoAircondHotChange = {
                                    noAircondHotFeeling = it
                                    hotAtNoon = it
                                },
                                onNoAircondHotPrevious = {
                                    activePreviousPicker = "when without aircond: hot feeling" to {
                                        noAircondHotFeeling = it
                                        hotAtNoon = it
                                    }
                                },
                                noAircondHotExplain = noAircondHotExplain,
                                onNoAircondHotExplainChange = { noAircondHotExplain = it },
                                noAircondLegFeeling = noAircondLegFeeling,
                                onNoAircondLegChange = { noAircondLegFeeling = it },
                                onNoAircondLegPrevious = {
                                    activePreviousPicker = "when without aircond: leg feeling" to { noAircondLegFeeling = it }
                                },
                                noAircondLegExplain = noAircondLegExplain,
                                onNoAircondLegExplainChange = { noAircondLegExplain = it },

                                fanSleepHotFeeling = fanSleepHotFeeling,
                                onFanSleepHotChange = { fanSleepHotFeeling = it },
                                onFanSleepHotPrevious = {
                                    activePreviousPicker = "when sleep under fan: hot feeling" to { fanSleepHotFeeling = it }
                                },
                                fanSleepHotExplain = fanSleepHotExplain,
                                onFanSleepHotExplainChange = { fanSleepHotExplain = it },
                                fanSleepLegFeeling = fanSleepLegFeeling,
                                onFanSleepLegChange = { fanSleepLegFeeling = it },
                                onFanSleepLegPrevious = {
                                    activePreviousPicker = "when sleep under fan: leg feeling" to { fanSleepLegFeeling = it }
                                },
                                fanSleepLegExplain = fanSleepLegExplain,
                                onFanSleepLegExplainChange = { fanSleepLegExplain = it },

                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Diao dai
                            FieldWithPreviousButton(
                                label = "DIAO DAI",
                                value = diaoDai,
                                placeholder = "diao dai / not diao dai",
                                onValueChange = { diaoDai = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "diao dai" to { diaoDai = it }
                                },
                                fieldKey = "diao dai",
                                explainValue = fieldExplanations["diao dai"] ?: "",
                                onExplainChange = { fieldExplanations["diao dai"] = it },
                                isExplainExpanded = expandedExplainFields["diao dai"] == true,
                                onToggleExplainExpand = { expandedExplainFields["diao dai"] = !(expandedExplainFields["diao dai"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Side breast ok: with 聚拢 bra
                            FieldWithPreviousButton(
                                label = "SIDE BREAST OK: WITH 聚拢 BRA",
                                value = sideBreastGatherBra,
                                placeholder = "side breast shape ok, no overflow",
                                onValueChange = { sideBreastGatherBra = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "side breast ok: with 聚拢 bra" to { sideBreastGatherBra = it }
                                },
                                fieldKey = "side breast ok: with 聚拢 bra",
                                explainValue = fieldExplanations["side breast ok: with 聚拢 bra"] ?: "",
                                onExplainChange = { fieldExplanations["side breast ok: with 聚拢 bra"] = it },
                                isExplainExpanded = expandedExplainFields["side breast ok: with 聚拢 bra"] == true,
                                onToggleExplainExpand = { expandedExplainFields["side breast ok: with 聚拢 bra"] = !(expandedExplainFields["side breast ok: with 聚拢 bra"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Side breast ok: with side out bra
                            FieldWithPreviousButton(
                                label = "SIDE BREAST OK: WITH SIDE OUT BRA",
                                value = sideBreastSideOutBra,
                                placeholder = "side breast shape ok, full coverage",
                                onValueChange = { sideBreastSideOutBra = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "side breast ok: with side out bra" to { sideBreastSideOutBra = it }
                                },
                                fieldKey = "side breast ok: with side out bra",
                                explainValue = fieldExplanations["side breast ok: with side out bra"] ?: "",
                                onExplainChange = { fieldExplanations["side breast ok: with side out bra"] = it },
                                isExplainExpanded = expandedExplainFields["side breast ok: with side out bra"] == true,
                                onToggleExplainExpand = { expandedExplainFields["side breast ok: with side out bra"] = !(expandedExplainFields["side breast ok: with side out bra"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Sit breast ok: With 聚拢bra:
                            FieldWithPreviousButton(
                                label = "SIT BREAST OK: WITH 聚拢BRA:",
                                value = sitBreastGatherBra,
                                placeholder = "sit breast shape ok",
                                onValueChange = { 
                                    sitBreastGatherBra = it 
                                    sitBreast = it
                                },
                                onOpenPrevious = {
                                    activePreviousPicker = "sit breast ok: With 聚拢bra:" to { 
                                        sitBreastGatherBra = it 
                                        sitBreast = it
                                    }
                                },
                                fieldKey = "sit breast ok: With 聚拢bra:",
                                explainValue = fieldExplanations["sit breast ok: With 聚拢bra:"] ?: fieldExplanations["sit breast ok"] ?: "",
                                onExplainChange = { 
                                    fieldExplanations["sit breast ok: With 聚拢bra:"] = it 
                                    fieldExplanations["sit breast ok"] = it
                                },
                                isExplainExpanded = expandedExplainFields["sit breast ok: With 聚拢bra:"] == true,
                                onToggleExplainExpand = { expandedExplainFields["sit breast ok: With 聚拢bra:"] = !(expandedExplainFields["sit breast ok: With 聚拢bra:"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Sit breast ok: with side out bra
                            FieldWithPreviousButton(
                                label = "SIT BREAST OK: WITH SIDE OUT BRA",
                                value = sitBreastSideOutBra,
                                placeholder = "sit breast shape ok",
                                onValueChange = { sitBreastSideOutBra = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "sit breast ok: with side out bra" to { sitBreastSideOutBra = it }
                                },
                                fieldKey = "sit breast ok: with side out bra",
                                explainValue = fieldExplanations["sit breast ok: with side out bra"] ?: "",
                                onExplainChange = { fieldExplanations["sit breast ok: with side out bra"] = it },
                                isExplainExpanded = expandedExplainFields["sit breast ok: with side out bra"] == true,
                                onToggleExplainExpand = { expandedExplainFields["sit breast ok: with side out bra"] = !(expandedExplainFields["sit breast ok: with side out bra"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Center chest
                            FieldWithPreviousButton(
                                label = "CENTER CHEST",
                                value = centerChest,
                                placeholder = "no center chest",
                                onValueChange = { centerChest = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "center chest" to { centerChest = it }
                                },
                                fieldKey = "center chest",
                                explainValue = fieldExplanations["center chest"] ?: "",
                                onExplainChange = { fieldExplanations["center chest"] = it },
                                isExplainExpanded = expandedExplainFields["center chest"] == true,
                                onToggleExplainExpand = { expandedExplainFields["center chest"] = !(expandedExplainFields["center chest"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // Beautiful
                            FieldWithPreviousButton(
                                label = "BEAUTIFUL",
                                value = beautiful,
                                placeholder = "Beautiful",
                                onValueChange = { beautiful = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "beautiful" to { beautiful = it }
                                },
                                fieldKey = "beautiful",
                                explainValue = fieldExplanations["beautiful"] ?: "",
                                onExplainChange = { fieldExplanations["beautiful"] = it },
                                isExplainExpanded = expandedExplainFields["beautiful"] == true,
                                onToggleExplainExpand = { expandedExplainFields["beautiful"] = !(expandedExplainFields["beautiful"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )
                        }
                    }

                    // Card 3: Exact Screenshot Card (WET, WET REASON, LONG DRESS)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // WET (Dropdown / Picker with Previous)
                            DropdownFieldWithPrevious(
                                label = "WET",
                                selectedValue = wet,
                                options = listOf("dry", "wet", "quick-dry", "semi-dry"),
                                onValueChange = { wet = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "wet" to { wet = it }
                                },
                                fieldKey = "wet",
                                explainValue = fieldExplanations["wet"] ?: "",
                                onExplainChange = { fieldExplanations["wet"] = it },
                                isExplainExpanded = expandedExplainFields["wet"] == true,
                                onToggleExplainExpand = { expandedExplainFields["wet"] = !(expandedExplainFields["wet"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // WET REASON
                            FieldWithPreviousButton(
                                label = "WET REASON",
                                value = wetReason,
                                placeholder = "quick-drying fabric",
                                onValueChange = { wetReason = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "wet reason" to { wetReason = it }
                                },
                                fieldKey = "wet reason",
                                explainValue = fieldExplanations["wet reason"] ?: "",
                                onExplainChange = { fieldExplanations["wet reason"] = it },
                                isExplainExpanded = expandedExplainFields["wet reason"] == true,
                                onToggleExplainExpand = { expandedExplainFields["wet reason"] = !(expandedExplainFields["wet reason"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // LONG DRESS
                            FieldWithPreviousButton(
                                label = "LONG DRESS",
                                value = longDress,
                                placeholder = "short dress",
                                onValueChange = { longDress = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "long dress" to { longDress = it }
                                },
                                fieldKey = "long dress",
                                explainValue = fieldExplanations["long dress"] ?: "",
                                onExplainChange = { fieldExplanations["long dress"] = it },
                                isExplainExpanded = expandedExplainFields["long dress"] == true,
                                onToggleExplainExpand = { expandedExplainFields["long dress"] = !(expandedExplainFields["long dress"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )
                        }
                    }

                    // Card 4: DESCRIPTIONS & LAST COMMENT OR ACTION (from screenshot)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // DESCRIPTIONS
                            FieldWithPreviousButton(
                                label = "DESCRIPTIONS",
                                value = descriptions,
                                placeholder = "Styling notes, fit details...",
                                onValueChange = { descriptions = it },
                                isMultiline = true,
                                onOpenPrevious = {
                                    activePreviousPicker = "Descriptions" to { descriptions = it }
                                },
                                fieldKey = "Descriptions",
                                explainValue = fieldExplanations["Descriptions"] ?: "",
                                onExplainChange = { fieldExplanations["Descriptions"] = it },
                                isExplainExpanded = expandedExplainFields["Descriptions"] == true,
                                onToggleExplainExpand = { expandedExplainFields["Descriptions"] = !(expandedExplainFields["Descriptions"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )

                            // LAST COMMENT OR ACTION
                            FieldWithPreviousButton(
                                label = "LAST COMMENT OR ACTION",
                                value = lastComment,
                                placeholder = "e.g. Needs gentle cycle",
                                onValueChange = { lastComment = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "last comment or action" to { lastComment = it }
                                },
                                fieldKey = "last comment or action",
                                explainValue = fieldExplanations["last comment or action"] ?: "",
                                onExplainChange = { fieldExplanations["last comment or action"] = it },
                                isExplainExpanded = expandedExplainFields["last comment or action"] == true,
                                onToggleExplainExpand = { expandedExplainFields["last comment or action"] = !(expandedExplainFields["last comment or action"] ?: false) },
                                allClothes = existingClothes,
                                dictionaryService = dictionaryService
                            )
                        }
                    }

                    // Card 5: CLOTHES PHOTOS (0) (Matching screenshot exactly)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header row with "CLOTHES PHOTOS (count)" and [Upload Photo] button
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
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = BrandPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CLOTHES PHOTOS (${photoUrls.size})",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                }

                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Upload Photo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Show attached thumbnails if any
                            if (photoUrls.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(photoUrls) { url ->
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                                        ) {
                                            AsyncImage(
                                                model = ImageUtils.resolveImagePath(url, context),
                                                contentDescription = "Clothes photo",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = { photoUrls.remove(url) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Purple Dashed Box for upload CTA (Matching screenshot)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFAF5FF))
                                    .border(
                                        BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { photoPickerLauncher.launch("image/*") }
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "No photos uploaded yet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF581C87)
                                    )
                                    Text(
                                        text = "Click here to attach photos from your camera or files",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9333EA)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer (matching screenshot: Cancel on left, "Add to Wardrobe" purple on right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .border(BorderStroke(1.dp, Color(0xFFF1F5F9)))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val newId = "rec_clothes_${System.currentTimeMillis()}"
                            val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                            val createdDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

                            val photoJson = JSONArray(photoUrls).toString()

                            val customFieldsObj = JSONObject()
                            val explainSubObj = JSONObject()
                            fieldExplanations.forEach { (k, v) ->
                                if (v.isNotBlank()) {
                                    explainSubObj.put(k, v)
                                    customFieldsObj.put("${k}_explain", v)
                                }
                            }
                            // Save environmental scenario explanations
                            if (aircondHotExplain.isNotBlank()) explainSubObj.put("when with aircond: hot feeling", aircondHotExplain)
                            if (aircondLegExplain.isNotBlank()) explainSubObj.put("when with aircond: leg feeling", aircondLegExplain)
                            if (noAircondHotExplain.isNotBlank()) explainSubObj.put("when without aircond: hot feeling", noAircondHotExplain)
                            if (noAircondLegExplain.isNotBlank()) explainSubObj.put("when without aircond: leg feeling", noAircondLegExplain)
                            if (fanSleepHotExplain.isNotBlank()) explainSubObj.put("when sleep under fan: hot feeling", fanSleepHotExplain)
                            if (fanSleepLegExplain.isNotBlank()) explainSubObj.put("when sleep under fan: leg feeling", fanSleepLegExplain)

                            if (explainSubObj.length() > 0) {
                                customFieldsObj.put("_fieldExplanations", explainSubObj)
                            }

                            val combinedComments = listOfNotNull(
                                descriptions.trim().ifBlank { null },
                                lastComment.trim().ifBlank { null }?.let { "Action: $it" },
                                longDress.trim().ifBlank { null }?.let { "Length: $it" }
                            ).joinToString("; ")

                            val effectiveHotAtNoon = noAircondHotFeeling.ifBlank { "Not hot" }
                            val newGarment = GarmentEntity(
                                id = newId,
                                name = name.ifBlank { "Untitled $clothesType" },
                                clothesType = clothesType,
                                material = material,
                                thickClothes = thickClothes,
                                hotAtNoon = effectiveHotAtNoon,
                                diaoDai = diaoDai,
                                sideBreastOk = sideBreastGatherBra,
                                sideBreastGatherBra = sideBreastGatherBra,
                                sideBreastSideOutBra = sideBreastSideOutBra,
                                aircondHotFeeling = aircondHotFeeling,
                                aircondHotExplain = aircondHotExplain,
                                aircondLegFeeling = aircondLegFeeling,
                                aircondLegExplain = aircondLegExplain,
                                noAircondHotFeeling = noAircondHotFeeling,
                                noAircondHotExplain = noAircondHotExplain,
                                noAircondLegFeeling = noAircondLegFeeling,
                                noAircondLegExplain = noAircondLegExplain,
                                fanSleepHotFeeling = fanSleepHotFeeling,
                                fanSleepHotExplain = fanSleepHotExplain,
                                fanSleepLegFeeling = fanSleepLegFeeling,
                                fanSleepLegExplain = fanSleepLegExplain,
                                standBreast = centerChest,
                                sitBreast = sitBreastGatherBra.ifBlank { sitBreast },
                                sitBreastGatherBra = sitBreastGatherBra,
                                sitBreastSideOutBra = sitBreastSideOutBra,
                                beautiful = beautiful,
                                wet = wet,
                                wetReason = wetReason,
                                descriptions = descriptions,
                                longDress = longDress,
                                price = parsedPrice,
                                wearingFrequency = 0,
                                trackedAppWashes = 0,
                                lastWashed = "",
                                mayThrow = "keep",
                                lastComment = combinedComments,
                                pictures = photoJson,
                                tags = "[]",
                                created = createdDate,
                                customFields = customFieldsObj.toString()
                            )
                            onSave(newGarment)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = "Add to Wardrobe",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // Previous Entries Modal
    activePreviousPicker?.let { (fieldKey, onValueSelected) ->
        val entries = remember(fieldKey) { getPreviousValuesForField(fieldKey) }
        PreviousEntriesModal(
            isOpen = true,
            fieldLabel = fieldKey,
            entries = entries,
            onSelect = { selectedValue ->
                onValueSelected(selectedValue)
                activePreviousPicker = null
            },
            onClose = { activePreviousPicker = null }
        )
    }
}

/**
 * Reusable Form Field matching screenshot with field label on left,
 * "⏱ Previous" pill button on right, and an outlined input field.
 */
@Composable
private fun FieldWithPreviousButton(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onOpenPrevious: () -> Unit,
    isMultiline: Boolean = false,
    fieldKey: String = label,
    explainValue: String = "",
    onExplainChange: ((String) -> Unit)? = null,
    isExplainExpanded: Boolean = true,
    onToggleExplainExpand: (() -> Unit)? = null,
    allClothes: List<GarmentEntity> = emptyList(),
    dictionaryService: FieldDictionaryService? = null,
    explainPlaceholder: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                color = Slate600
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.clickable { onOpenPrevious() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Previous",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = !isMultiline,
            minLines = if (isMultiline) 3 else 1,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Slate200,
                focusedBorderColor = BrandPurple
            )
        )

        if (onExplainChange != null) {
            ErgonomicExplainField(
                fieldKey = fieldKey,
                fieldLabel = label,
                value = explainValue,
                onValueChange = onExplainChange,
                isExpanded = isExplainExpanded,
                onToggleExpand = onToggleExplainExpand ?: {},
                allClothes = allClothes,
                customPlaceholder = explainPlaceholder,
                dictionaryService = dictionaryService
            )
        }
    }
}

/**
 * Reusable Dropdown Field matching screenshot's "WET" row:
 * Label + "⏱ Previous" on top, Select box with chevron.
 */
@Composable
private fun DropdownFieldWithPrevious(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    onOpenPrevious: () -> Unit,
    fieldKey: String = label,
    explainValue: String = "",
    onExplainChange: ((String) -> Unit)? = null,
    isExplainExpanded: Boolean = false,
    onToggleExplainExpand: (() -> Unit)? = null,
    allClothes: List<GarmentEntity> = emptyList(),
    dictionaryService: FieldDictionaryService? = null,
    explainPlaceholder: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                color = Slate600
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.clickable { onOpenPrevious() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Previous",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedValue.ifBlank { "Select..." },
                        fontSize = 13.sp,
                        color = Slate900
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Slate600
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 13.sp) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (onExplainChange != null) {
            ErgonomicExplainField(
                fieldKey = fieldKey,
                fieldLabel = label,
                value = explainValue,
                onValueChange = onExplainChange,
                isExpanded = isExplainExpanded,
                onToggleExpand = onToggleExplainExpand ?: {},
                allClothes = allClothes,
                customPlaceholder = explainPlaceholder,
                dictionaryService = dictionaryService
            )
        }
    }
}
