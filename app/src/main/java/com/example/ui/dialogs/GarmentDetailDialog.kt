package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.ErgonomicExplainField
import com.example.ui.components.EnvironmentalScenariosEditSection
import com.example.ui.components.EnvironmentalScenariosViewSection
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.drilldown.DrilldownUtils
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandRose
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GarmentAiReportVersion(
    val id: String,
    val title: String,
    val timestamp: String,
    val content: String,
    val isPrimary: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GarmentDetailDialog(
    garment: GarmentEntity,
    existingClothes: List<GarmentEntity> = emptyList(),
    junctions: List<JunctionEntity> = emptyList(),
    reports: List<ReportEntity> = emptyList(),
    isAiPaused: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onSaveGarment: ((GarmentEntity) -> Unit)? = null,
    onMarkWashNow: ((GarmentEntity) -> Unit)? = null,
    onSelectReport: ((ReportEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val effectiveAiPaused = isAiPaused || remember(context) { GeminiAiService.isAiPaused(context) }
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isGeneratingAiReport by remember { mutableStateOf(false) }

    // Editable form state initialized with garment data (matching all fields from Add Clothes Modal)
    var editName by remember(garment) { mutableStateOf(garment.name) }
    var editClothesType by remember(garment) { mutableStateOf(garment.clothesType) }
    var editMaterial by remember(garment) { mutableStateOf(garment.material) }
    var editPriceStr by remember(garment) { mutableStateOf(if (garment.price > 0) garment.price.toString() else "") }

    var editThickClothes by remember(garment) { mutableStateOf(garment.thickClothes) }
    var editHotAtNoon by remember(garment) { mutableStateOf(garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }) }
    var editDiaoDai by remember(garment) { mutableStateOf(garment.diaoDai) }
    var editSideBreast by remember(garment) { mutableStateOf(garment.sideBreastGatherBra.ifBlank { garment.sideBreastOk }) }

    // Dedicated Side Breast Bra fields
    var editSideBreastGatherBra by remember(garment) { mutableStateOf(garment.sideBreastGatherBra.ifBlank { garment.sideBreastOk }) }
    var editSideBreastSideOutBra by remember(garment) { mutableStateOf(garment.sideBreastSideOutBra) }

    // 3 Environmental Scenarios
    var editAircondHotFeeling by remember(garment) { mutableStateOf(garment.aircondHotFeeling) }
    var editAircondHotExplain by remember(garment) { mutableStateOf(garment.aircondHotExplain) }
    var editAircondLegFeeling by remember(garment) { mutableStateOf(garment.aircondLegFeeling) }
    var editAircondLegExplain by remember(garment) { mutableStateOf(garment.aircondLegExplain) }

    var editNoAircondHotFeeling by remember(garment) { mutableStateOf(garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }) }
    var editNoAircondHotExplain by remember(garment) { mutableStateOf(garment.noAircondHotExplain) }
    var editNoAircondLegFeeling by remember(garment) { mutableStateOf(garment.noAircondLegFeeling) }
    var editNoAircondLegExplain by remember(garment) { mutableStateOf(garment.noAircondLegExplain) }

    var editFanSleepHotFeeling by remember(garment) { mutableStateOf(garment.fanSleepHotFeeling) }
    var editFanSleepHotExplain by remember(garment) { mutableStateOf(garment.fanSleepHotExplain) }
    var editFanSleepLegFeeling by remember(garment) { mutableStateOf(garment.fanSleepLegFeeling) }
    var editFanSleepLegExplain by remember(garment) { mutableStateOf(garment.fanSleepLegExplain) }
    var editSitBreast by remember(garment) { mutableStateOf(garment.sitBreastGatherBra.ifBlank { garment.sitBreast }) }
    var editSitBreastGatherBra by remember(garment) { mutableStateOf(garment.sitBreastGatherBra.ifBlank { garment.sitBreast }) }
    var editSitBreastSideOutBra by remember(garment) { mutableStateOf(garment.sitBreastSideOutBra) }
    var editCenterChest by remember(garment) { mutableStateOf(garment.standBreast) }
    var editBeautiful by remember(garment) { mutableStateOf(garment.beautiful) }
    var editBeautifulReason by remember(garment) { mutableStateOf(garment.beautifulReason) }

    var editWet by remember(garment) { mutableStateOf(garment.wet.ifBlank { "dry" }) }
    var editWetReason by remember(garment) { mutableStateOf(garment.wetReason) }
    var editLongDress by remember(garment) { mutableStateOf(garment.longDress) }
    var editMayThrow by remember(garment) { mutableStateOf(garment.mayThrow) }
    var editWearingFrequency by remember(garment) { mutableStateOf(garment.wearingFrequency.toString()) }
    var editTrackedAppWashes by remember(garment) { mutableStateOf(garment.trackedAppWashes.toString()) }

    var editDescriptions by remember(garment) { mutableStateOf(garment.descriptions) }
    var editDescriptions2 by remember(garment) { mutableStateOf(garment.descriptions2) }
    var editLastCommentAction by remember(garment) { mutableStateOf(garment.lastComment) }

    // Field-level explanations (mapping fieldKey to explanation text) matching Web App _fieldExplanations
    val editFieldExplanations = remember(garment) {
        val map = mutableStateMapOf<String, String>()
        try {
            if (garment.customFields.isNotBlank() && garment.customFields != "{}") {
                val obj = JSONObject(garment.customFields)
                val subObj = obj.optJSONObject("_fieldExplanations")
                if (subObj != null) {
                    val keys = subObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = subObj.optString(k, "")
                    }
                }
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k.endsWith("_explain")) {
                        val pureKey = k.removeSuffix("_explain")
                        if (!map.containsKey(pureKey)) {
                            map[pureKey] = obj.optString(k, "")
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        map
    }

    val fieldExplanationsInView = remember(garment.customFields) {
        editFieldExplanations.toMap()
    }
    val expandedExplainFields = remember { mutableStateMapOf<String, Boolean>() }
    val dictionaryService = remember { FieldDictionaryService(context) }

    // Extract tags
    val tagsList = remember(garment.tags) {
        try {
            val arr = JSONArray(garment.tags)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val t = arr.optString(i)
                if (t.isNotBlank()) list.add(t)
            }
            if (list.isEmpty()) listOf("clothes", "test") else list
        } catch (_: Exception) {
            listOf("clothes", "test")
        }
    }

    // Photo URLs state — resolve valid local photos from ZIP backup or user uploads
    val photoUrls = remember(garment.pictures) {
        val resolved = ImageUtils.resolveAllValidPhotos(garment.pictures, context)
        mutableStateListOf<String>().apply { addAll(resolved) }
    }

    // Photo picker launcher — persists chosen photos into internal storage filesDir/photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val savedUri = ImageUtils.copyUriToInternalStorage(context, uri, garment.id)
            if (!photoUrls.contains(savedUri)) {
                photoUrls.add(savedUri)
            }
        }
        val updatedJson = JSONArray(photoUrls).toString()
        onSaveGarment?.invoke(garment.copy(pictures = updatedJson))
    }

    // Active PreviousEntriesModal state: fieldName to onSelect callback
    var activePreviousPicker by remember {
        mutableStateOf<Pair<String, (String) -> Unit>?>(null)
    }

    // Type Choose Dropdown
    var clothesTypeDropdownExpanded by remember { mutableStateOf(false) }
    val clothesTypeOptions = listOf("Dress", "Pant", "Tshirt", "bra", "skirt", "sweater", "jacket", "coat", "top", "blouse", "cardigan", "shorts", "Other")

    // Accurate stats calculations matching web app localDatabaseService.getGarmentStats
    val garmentStats = remember(garment.id, junctions) {
        DrilldownUtils.calculateGarmentStats(garment.id, junctions)
    }
    val garmentJunctions = remember(garment.id, garment.name, junctions) {
        junctions.filter { j ->
            j.clothesId == garment.id ||
            (garment.name.isNotBlank() && (j.clothesTitle.equals(garment.name, ignoreCase = true) || j.clothesId.equals(garment.name, ignoreCase = true)))
        }
    }
    val reportsById = remember(reports) { reports.associateBy { it.id } }

    val totalWears = garmentStats.totalWears
    val totalWashesCount = garment.wearingFrequency + garment.trackedAppWashes
    val wearDenominator = if (totalWashesCount > 0) totalWashesCount else totalWears
    val cpw = if (garment.price > 0 && wearDenominator > 0) {
        String.format(Locale.US, "%.2f", garment.price / wearDenominator)
    } else "0.00"
    val totalWearsDisplay = "$totalWears times"
    val totalDurationHours = "${garmentStats.totalDurationHours} hrs"
    val unwashedHours = garmentStats.unwashedHours
    val isNeedWash = garmentStats.needWash

    // Formatted created date
    val displayCreatedDate = remember(garment.created) {
        if (garment.created.isNotBlank()) {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val d = parser.parse(garment.created)
                if (d != null) SimpleDateFormat("M/d/yyyy", Locale.US).format(d) else "6/1/2026"
            } catch (_: Exception) {
                "6/1/2026"
            }
        } else "6/1/2026"
    }

    // Helper to calculate previous values for a field from existing clothes
    fun getPreviousValuesForField(fieldKey: String): List<PreviousEntry> {
        val countMap = mutableMapOf<String, Int>()
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
                "may throw" -> g.mayThrow
                "Descriptions" -> g.lastComment
                else -> ""
            }.trim()
            if (v.isNotBlank() && v != "(Not specified)") {
                countMap[v] = (countMap[v] ?: 0) + 1
                val gName = g.name.ifBlank { "Garment" }.trim()
                if (gName.isNotBlank()) {
                    sourcesMap.getOrPut(v) { mutableSetOf() }.add(gName)
                }
            }
        }

        // Default suggestions if map is empty
        when (fieldKey) {
            "Clothes Type" -> if (countMap.isEmpty()) clothesTypeOptions.forEach { countMap[it] = 1 }
            "material" -> if (countMap.isEmpty()) listOf("lace", "soft chiffon", "cotton", "silk", "wool", "linen", "polyester", "denim").forEach { countMap[it] = 1 }
            "thick clothes" -> if (countMap.isEmpty()) listOf("01 not thick clothes", "02 medium", "03 thick clothes", "ultra-thin").forEach { countMap[it] = 1 }
            "Hot at noon", "when without aircond: hot feeling" -> if (countMap.isEmpty()) listOf("hot", "middle hot", "not hot", "very hot", "stuffy").forEach { countMap[it] = 1 }
            "when without aircond: leg feeling" -> if (countMap.isEmpty()) listOf("middle hot", "hot", "not hot", "sticky", "breathable").forEach { countMap[it] = 1 }
            "when with aircond: hot feeling" -> if (countMap.isEmpty()) listOf("not so cold", "cold", "comfortable cool", "very cold", "not cold").forEach { countMap[it] = 1 }
            "when with aircond: leg feeling" -> if (countMap.isEmpty()) listOf("not so cold", "cold", "comfortable", "chilly knees").forEach { countMap[it] = 1 }
            "when sleep under fan: hot feeling" -> if (countMap.isEmpty()) listOf("not hot", "middle cold", "comfortable", "cool", "sweaty back").forEach { countMap[it] = 1 }
            "when sleep under fan: leg feeling" -> if (countMap.isEmpty()) listOf("middle cold", "cold", "not hot", "comfortable", "breezy").forEach { countMap[it] = 1 }
            "diao dai" -> if (countMap.isEmpty()) listOf("not diao dai", "diao dai", "wide strap", "halter").forEach { countMap[it] = 1 }
            "side breast ok", "side breast ok: with 聚拢 bra" -> if (countMap.isEmpty()) listOf("side breast shape ok", "no friction", "well gathered, no overflow", "mild bulge", "friction at armpit").forEach { countMap[it] = 1 }
            "side breast ok: with side out bra" -> if (countMap.isEmpty()) listOf("side breast shape ok", "full coverage no overflow", "no friction", "slight side exposure", "wide band comfortable").forEach { countMap[it] = 1 }
            "sit breast ok", "sit breast ok: With 聚拢bra:", "sit breast ok: with 聚拢 bra" -> if (countMap.isEmpty()) listOf("sit breast shape ok", "tight when seated", "very loose", "no underwire pressure", "comfortable when seated").forEach { countMap[it] = 1 }
            "sit breast ok: with side out bra" -> if (countMap.isEmpty()) listOf("sit breast shape ok", "full coverage relaxed", "no side bulge when seated", "tight underwire", "relaxed fit").forEach { countMap[it] = 1 }
            "center chest" -> if (countMap.isEmpty()) listOf("center chest", "no center chest", "low cut", "modest").forEach { countMap[it] = 1 }
            "beautiful" -> if (countMap.isEmpty()) listOf("Beautiful", "not so beautiful", "Standard", "Casual").forEach { countMap[it] = 1 }
            "wet" -> if (countMap.isEmpty()) listOf("dry", "wet", "quick-dry", "semi-dry").forEach { countMap[it] = 1 }
            "long dress" -> if (countMap.isEmpty()) listOf("long dress", "short dress", "midi dress", "maxi dress").forEach { countMap[it] = 1 }
            "may throw" -> if (countMap.isEmpty()) listOf("test", "keep", "may throw", "donate", "repair").forEach { countMap[it] = 1 }
            "last comment or action" -> if (countMap.isEmpty()) listOf("test", "Needs gentle cycle", "hand wash only", "hang dry immediately").forEach { countMap[it] = 1 }
        }

        val dictFields = try { dictionaryService.getFields() } catch (_: Exception) { emptyList() }
        val dictValues = dictFields.find { it.name.equals(fieldKey, ignoreCase = true) }?.typicalValues ?: emptyList()

        return countMap.entries.map { e ->
            val typical = dictValues.find { it.valueEn.equals(e.key, ignoreCase = true) }
            PreviousEntry(
                value = e.key,
                count = e.value,
                labelCn = typical?.valueCn?.ifBlank { null }?.takeIf { it != e.key },
                meaningCn = typical?.meaningCn?.ifBlank { null },
                sources = sourcesMap[e.key]?.toList() ?: emptyList()
            )
        }.sortedByDescending { it.count }
    }

    // Function to commit changes
    fun handleSaveChanges() {
        val customFieldsObj = try {
            if (garment.customFields.isNotBlank() && garment.customFields != "{}") JSONObject(garment.customFields) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }
        val explainSubObj = JSONObject()
        editFieldExplanations.forEach { (k, v) ->
            if (v.isNotBlank()) {
                explainSubObj.put(k, v)
                customFieldsObj.put("${k}_explain", v)
            } else {
                customFieldsObj.remove("${k}_explain")
            }
        }
        // Save environmental scenario explanations
        if (editAircondHotExplain.isNotBlank()) explainSubObj.put("when with aircond: hot feeling", editAircondHotExplain)
        if (editAircondLegExplain.isNotBlank()) explainSubObj.put("when with aircond: leg feeling", editAircondLegExplain)
        if (editNoAircondHotExplain.isNotBlank()) explainSubObj.put("when without aircond: hot feeling", editNoAircondHotExplain)
        if (editNoAircondLegExplain.isNotBlank()) explainSubObj.put("when without aircond: leg feeling", editNoAircondLegExplain)
        if (editFanSleepHotExplain.isNotBlank()) explainSubObj.put("when sleep under fan: hot feeling", editFanSleepHotExplain)
        if (editFanSleepLegExplain.isNotBlank()) explainSubObj.put("when sleep under fan: leg feeling", editFanSleepLegExplain)

        customFieldsObj.put("_fieldExplanations", explainSubObj)

        val effectiveHotAtNoon = editNoAircondHotFeeling.ifBlank { editHotAtNoon }
        val updated = garment.copy(
            name = editName.ifBlank { garment.name },
            clothesType = editClothesType.ifBlank { garment.clothesType },
            material = editMaterial,
            price = editPriceStr.toDoubleOrNull() ?: garment.price,
            thickClothes = editThickClothes,
            hotAtNoon = effectiveHotAtNoon,
            diaoDai = editDiaoDai,
            sideBreastOk = editSideBreastGatherBra,
            sideBreastGatherBra = editSideBreastGatherBra,
            sideBreastSideOutBra = editSideBreastSideOutBra,
            aircondHotFeeling = editAircondHotFeeling,
            aircondHotExplain = editAircondHotExplain,
            aircondLegFeeling = editAircondLegFeeling,
            aircondLegExplain = editAircondLegExplain,
            noAircondHotFeeling = editNoAircondHotFeeling,
            noAircondHotExplain = editNoAircondHotExplain,
            noAircondLegFeeling = editNoAircondLegFeeling,
            noAircondLegExplain = editNoAircondLegExplain,
            fanSleepHotFeeling = editFanSleepHotFeeling,
            fanSleepHotExplain = editFanSleepHotExplain,
            fanSleepLegFeeling = editFanSleepLegFeeling,
            fanSleepLegExplain = editFanSleepLegExplain,
            sitBreast = editSitBreastGatherBra.ifBlank { editSitBreast },
            sitBreastGatherBra = editSitBreastGatherBra,
            sitBreastSideOutBra = editSitBreastSideOutBra,
            standBreast = editCenterChest,
            beautiful = editBeautiful,
            beautifulReason = editBeautifulReason,
            wet = editWet,
            wetReason = editWetReason,
            longDress = editLongDress,
            mayThrow = editMayThrow,
            wearingFrequency = editWearingFrequency.toIntOrNull() ?: garment.wearingFrequency,
            trackedAppWashes = editTrackedAppWashes.toIntOrNull() ?: garment.trackedAppWashes,
            descriptions = editDescriptions,
            descriptions2 = editDescriptions2,
            lastComment = editLastCommentAction,
            customFields = customFieldsObj.toString()
        )
        onSaveGarment?.invoke(updated)
        isEditing = false
    }

    fun handleCancelEditing() {
        // Reset to original values
        editName = garment.name
        editClothesType = garment.clothesType
        editMaterial = garment.material
        editPriceStr = if (garment.price > 0) garment.price.toString() else ""
        editThickClothes = garment.thickClothes
        editHotAtNoon = garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }
        editDiaoDai = garment.diaoDai
        editSideBreast = garment.sideBreastGatherBra.ifBlank { garment.sideBreastOk }
        editSideBreastGatherBra = garment.sideBreastGatherBra.ifBlank { garment.sideBreastOk }
        editSideBreastSideOutBra = garment.sideBreastSideOutBra
        editAircondHotFeeling = garment.aircondHotFeeling
        editAircondHotExplain = garment.aircondHotExplain
        editAircondLegFeeling = garment.aircondLegFeeling
        editAircondLegExplain = garment.aircondLegExplain
        editNoAircondHotFeeling = garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }
        editNoAircondHotExplain = garment.noAircondHotExplain
        editNoAircondLegFeeling = garment.noAircondLegFeeling
        editNoAircondLegExplain = garment.noAircondLegExplain
        editFanSleepHotFeeling = garment.fanSleepHotFeeling
        editFanSleepHotExplain = garment.fanSleepHotExplain
        editFanSleepLegFeeling = garment.fanSleepLegFeeling
        editFanSleepLegExplain = garment.fanSleepLegExplain
        editSitBreast = garment.sitBreastGatherBra.ifBlank { garment.sitBreast }
        editSitBreastGatherBra = garment.sitBreastGatherBra.ifBlank { garment.sitBreast }
        editSitBreastSideOutBra = garment.sitBreastSideOutBra
        editCenterChest = garment.standBreast
        editBeautiful = garment.beautiful
        editBeautifulReason = garment.beautifulReason
        editWet = garment.wet.ifBlank { "dry" }
        editWetReason = garment.wetReason
        editLongDress = garment.longDress
        editMayThrow = garment.mayThrow
        editDescriptions = garment.descriptions
        editDescriptions2 = garment.descriptions2
        editLastCommentAction = garment.lastComment
        editFieldExplanations.clear()
        try {
            if (garment.customFields.isNotBlank() && garment.customFields != "{}") {
                val obj = JSONObject(garment.customFields)
                val subObj = obj.optJSONObject("_fieldExplanations")
                if (subObj != null) {
                    val keys = subObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        editFieldExplanations[k] = subObj.optString(k, "")
                    }
                }
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k.endsWith("_explain")) {
                        val pureKey = k.removeSuffix("_explain")
                        if (!editFieldExplanations.containsKey(pureKey)) {
                            editFieldExplanations[pureKey] = obj.optString(k, "")
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        isEditing = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("garment_detail_dialog"),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ==============================
                // TOP HEADER BAR
                // ==============================
                if (isEditing) {
                    // EDIT MODE HEADER (Matching screenshot IMG_9642 top)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left: Back button + Editing Garment Record
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { handleCancelEditing() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Slate700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Editing Garment Record",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Right: Fullscreen toggle, Cancel, Save Changes
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isFullScreen = !isFullScreen },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Slate500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Cancel Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate300),
                                modifier = Modifier.clickable { handleCancelEditing() }
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate600,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Save Changes Outlined Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate900),
                                modifier = Modifier
                                    .clickable { handleSaveChanges() }
                                    .testTag("save_changes_top_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        tint = Slate900,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Save Changes",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // VIEW MODE HEADER
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = garment.clothesType.ifBlank { "Dress" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    onDelete(garment.id)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_garment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Garment",
                                    tint = BrandRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Slate300),
                                modifier = Modifier
                                    .clickable { isEditing = true }
                                    .testTag("edit_garment_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Slate700,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Edit Garment",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
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
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // ==============================
                // SCROLLABLE BODY
                // ==============================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isEditing) {
                        // ------------------------------------------
                        // EDIT MODE CONTENT (Matching Screenshot IMG_9642)
                        // ------------------------------------------

                        // CARD 1: CORE INFORMATION (Soft Purple Tinted Card)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFAF5FF), // purple-50/40
                            border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "CORE INFORMATION",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF581C87) // purple-900
                                )

                                // 1. GARMENT NAME *
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "GARMENT NAME *",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
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
                                        value = editFieldExplanations["Name"] ?: "",
                                        onValueChange = { editFieldExplanations["Name"] = it },
                                        isExpanded = expandedExplainFields["Name"] == true,
                                        onToggleExpand = { expandedExplainFields["Name"] = !(expandedExplainFields["Name"] ?: false) },
                                        allClothes = existingClothes,
                                        customPlaceholder = "补充命名原因或此单品特别备注...",
                                        dictionaryService = dictionaryService
                                    )
                                }

                                // 2. CLOTHES TYPE * (with Previous pill + Choose button)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CLOTHES TYPE *",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate600
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.clickable {
                                                activePreviousPicker = "Clothes Type" to { selected -> editClothesType = selected }
                                            }
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
                                        OutlinedTextField(
                                            value = editClothesType,
                                            onValueChange = { editClothesType = it },
                                            placeholder = { Text("e.g. Dress, bra, Tshirt...", fontSize = 13.sp, color = Slate400) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            trailingIcon = {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFF3E8FF),
                                                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                                    modifier = Modifier
                                                        .padding(end = 4.dp)
                                                        .clickable { clothesTypeDropdownExpanded = true }
                                                ) {
                                                    Text(
                                                        text = "Choose",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF6B21A8),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.White,
                                                focusedContainerColor = Color.White,
                                                unfocusedBorderColor = Slate200,
                                                focusedBorderColor = BrandPurple
                                            )
                                        )

                                        DropdownMenu(
                                            expanded = clothesTypeDropdownExpanded,
                                            onDismissRequest = { clothesTypeDropdownExpanded = false }
                                        ) {
                                            clothesTypeOptions.forEach { opt ->
                                                DropdownMenuItem(
                                                    text = { Text(opt, fontSize = 12.sp) },
                                                    onClick = {
                                                        editClothesType = opt
                                                        clothesTypeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    ErgonomicExplainField(
                                        fieldKey = "Clothes Type",
                                        fieldLabel = "Clothes Type",
                                        value = editFieldExplanations["Clothes Type"] ?: "",
                                        onValueChange = { editFieldExplanations["Clothes Type"] = it },
                                        isExpanded = expandedExplainFields["Clothes Type"] == true,
                                        onToggleExpand = { expandedExplainFields["Clothes Type"] = !(expandedExplainFields["Clothes Type"] ?: false) },
                                        allClothes = existingClothes,
                                        customPlaceholder = "例如: dress normally more feminine. Got better waist and breast cutting...",
                                        dictionaryService = dictionaryService
                                    )
                                }

                                // 3. MATERIAL (with Previous pill)
                                EditFieldWithPrevious(
                                    label = "MATERIAL",
                                    value = editMaterial,
                                    placeholder = "e.g. Chiffon, Cotton, Silk, lace",
                                    onValueChange = { editMaterial = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "material" to { editMaterial = it }
                                    },
                                    fieldKey = "material",
                                    explainValue = editFieldExplanations["material"] ?: "",
                                    onExplainChange = { editFieldExplanations["material"] = it },
                                    allClothes = existingClothes,
                                    isExplainExpanded = expandedExplainFields["material"] == true,
                                    onToggleExplainExpand = { expandedExplainFields["material"] = !(expandedExplainFields["material"] ?: false) },
                                    dictionaryService = dictionaryService
                                )

                                // 4. PRICE ($)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "PRICE ($)",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    OutlinedTextField(
                                        value = editPriceStr,
                                        onValueChange = { editPriceStr = it },
                                        placeholder = { Text("e.g. 44", fontSize = 13.sp, color = Slate400) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                        value = editFieldExplanations["price"] ?: "",
                                        onValueChange = { editFieldExplanations["price"] = it },
                                        isExpanded = expandedExplainFields["price"] == true,
                                        onToggleExpand = { expandedExplainFields["price"] = !(expandedExplainFields["price"] ?: false) },
                                        allClothes = existingClothes,
                                        customPlaceholder = "价格、购入渠道或性价比体感备注...",
                                        dictionaryService = dictionaryService
                                    )
                                }
                            }
                        }

                        // CARD 2: GARMENT ATTRIBUTES & ERGONOMICS (White card with gray border)
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
                                    text = "GARMENT ATTRIBUTES & ERGONOMICS",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )

                                // thick clothes
                                EditFieldWithPrevious(
                                    label = "THICK CLOTHES",
                                    value = editThickClothes,
                                    placeholder = "e.g. 01 not thick clothes, 03 thick clothes",
                                    onValueChange = { editThickClothes = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "thick clothes" to { editThickClothes = it }
                                    },
                                    explainValue = editFieldExplanations["thick clothes"] ?: "",
                                    onExplainChange = { editFieldExplanations["thick clothes"] = it }
                                )

                                // 3 Environmental Scenarios Expandable Cards
                                EnvironmentalScenariosEditSection(
                                    aircondHotFeeling = editAircondHotFeeling,
                                    onAircondHotChange = { editAircondHotFeeling = it },
                                    onAircondHotPrevious = {
                                        activePreviousPicker = "when with aircond: hot feeling" to { editAircondHotFeeling = it }
                                    },
                                    aircondHotExplain = editAircondHotExplain,
                                    onAircondHotExplainChange = { editAircondHotExplain = it },
                                    aircondLegFeeling = editAircondLegFeeling,
                                    onAircondLegChange = { editAircondLegFeeling = it },
                                    onAircondLegPrevious = {
                                        activePreviousPicker = "when with aircond: leg feeling" to { editAircondLegFeeling = it }
                                    },
                                    aircondLegExplain = editAircondLegExplain,
                                    onAircondLegExplainChange = { editAircondLegExplain = it },

                                    noAircondHotFeeling = editNoAircondHotFeeling,
                                    onNoAircondHotChange = {
                                        editNoAircondHotFeeling = it
                                        editHotAtNoon = it
                                    },
                                    onNoAircondHotPrevious = {
                                        activePreviousPicker = "when without aircond: hot feeling" to {
                                            editNoAircondHotFeeling = it
                                            editHotAtNoon = it
                                        }
                                    },
                                    noAircondHotExplain = editNoAircondHotExplain,
                                    onNoAircondHotExplainChange = { editNoAircondHotExplain = it },
                                    noAircondLegFeeling = editNoAircondLegFeeling,
                                    onNoAircondLegChange = { editNoAircondLegFeeling = it },
                                    onNoAircondLegPrevious = {
                                        activePreviousPicker = "when without aircond: leg feeling" to { editNoAircondLegFeeling = it }
                                    },
                                    noAircondLegExplain = editNoAircondLegExplain,
                                    onNoAircondLegExplainChange = { editNoAircondLegExplain = it },

                                    fanSleepHotFeeling = editFanSleepHotFeeling,
                                    onFanSleepHotChange = { editFanSleepHotFeeling = it },
                                    onFanSleepHotPrevious = {
                                        activePreviousPicker = "when sleep under fan: hot feeling" to { editFanSleepHotFeeling = it }
                                    },
                                    fanSleepHotExplain = editFanSleepHotExplain,
                                    onFanSleepHotExplainChange = { editFanSleepHotExplain = it },
                                    fanSleepLegFeeling = editFanSleepLegFeeling,
                                    onFanSleepLegChange = { editFanSleepLegFeeling = it },
                                    onFanSleepLegPrevious = {
                                        activePreviousPicker = "when sleep under fan: leg feeling" to { editFanSleepLegFeeling = it }
                                    },
                                    fanSleepLegExplain = editFanSleepLegExplain,
                                    onFanSleepLegExplainChange = { editFanSleepLegExplain = it },

                                    allClothes = existingClothes,
                                    dictionaryService = dictionaryService
                                )

                                // diao dai
                                EditFieldWithPrevious(
                                    label = "DIAO DAI",
                                    value = editDiaoDai,
                                    placeholder = "e.g. not diao dai, diao dai",
                                    onValueChange = { editDiaoDai = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "diao dai" to { editDiaoDai = it }
                                    },
                                    explainValue = editFieldExplanations["diao dai"] ?: "",
                                    onExplainChange = { editFieldExplanations["diao dai"] = it }
                                )

                                // side breast ok: with 聚拢 bra
                                EditFieldWithPrevious(
                                    label = "SIDE BREAST OK: WITH 聚拢 BRA",
                                    value = editSideBreastGatherBra,
                                    placeholder = "e.g. side breast shape ok, no overflow",
                                    onValueChange = { editSideBreastGatherBra = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "side breast ok: with 聚拢 bra" to { editSideBreastGatherBra = it }
                                    },
                                    fieldKey = "side breast ok: with 聚拢 bra",
                                    explainValue = editFieldExplanations["side breast ok: with 聚拢 bra"] ?: "",
                                    onExplainChange = { editFieldExplanations["side breast ok: with 聚拢 bra"] = it },
                                    allClothes = existingClothes,
                                    isExplainExpanded = expandedExplainFields["side breast ok: with 聚拢 bra"] == true,
                                    onToggleExplainExpand = { expandedExplainFields["side breast ok: with 聚拢 bra"] = !(expandedExplainFields["side breast ok: with 聚拢 bra"] ?: false) },
                                    dictionaryService = dictionaryService
                                )

                                // side breast ok: with side out bra
                                EditFieldWithPrevious(
                                    label = "SIDE BREAST OK: WITH SIDE OUT BRA",
                                    value = editSideBreastSideOutBra,
                                    placeholder = "e.g. side breast shape ok, full coverage",
                                    onValueChange = { editSideBreastSideOutBra = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "side breast ok: with side out bra" to { editSideBreastSideOutBra = it }
                                    },
                                    fieldKey = "side breast ok: with side out bra",
                                    explainValue = editFieldExplanations["side breast ok: with side out bra"] ?: "",
                                    onExplainChange = { editFieldExplanations["side breast ok: with side out bra"] = it },
                                    allClothes = existingClothes,
                                    isExplainExpanded = expandedExplainFields["side breast ok: with side out bra"] == true,
                                    onToggleExplainExpand = { expandedExplainFields["side breast ok: with side out bra"] = !(expandedExplainFields["side breast ok: with side out bra"] ?: false) },
                                    dictionaryService = dictionaryService
                                )

                                // sit breast ok: With 聚拢bra:
                                EditFieldWithPrevious(
                                    label = "SIT BREAST OK: WITH 聚拢BRA:",
                                    value = editSitBreastGatherBra,
                                    placeholder = "e.g. sit breast shape ok, comfortable when seated",
                                    onValueChange = {
                                        editSitBreastGatherBra = it
                                        editSitBreast = it
                                    },
                                    onOpenPrevious = {
                                        activePreviousPicker = "sit breast ok: With 聚拢bra:" to {
                                            editSitBreastGatherBra = it
                                            editSitBreast = it
                                        }
                                    },
                                    fieldKey = "sit breast ok: With 聚拢bra:",
                                    explainValue = editFieldExplanations["sit breast ok: With 聚拢bra:"] ?: editFieldExplanations["sit breast ok"] ?: "",
                                    onExplainChange = {
                                        editFieldExplanations["sit breast ok: With 聚拢bra:"] = it
                                        editFieldExplanations["sit breast ok"] = it
                                    },
                                    allClothes = existingClothes,
                                    isExplainExpanded = expandedExplainFields["sit breast ok: With 聚拢bra:"] == true,
                                    onToggleExplainExpand = { expandedExplainFields["sit breast ok: With 聚拢bra:"] = !(expandedExplainFields["sit breast ok: With 聚拢bra:"] ?: false) },
                                    dictionaryService = dictionaryService
                                )

                                // sit breast ok: with side out bra
                                EditFieldWithPrevious(
                                    label = "SIT BREAST OK: WITH SIDE OUT BRA",
                                    value = editSitBreastSideOutBra,
                                    placeholder = "e.g. sit breast shape ok, relaxed fit",
                                    onValueChange = { editSitBreastSideOutBra = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "sit breast ok: with side out bra" to { editSitBreastSideOutBra = it }
                                    },
                                    fieldKey = "sit breast ok: with side out bra",
                                    explainValue = editFieldExplanations["sit breast ok: with side out bra"] ?: "",
                                    onExplainChange = { editFieldExplanations["sit breast ok: with side out bra"] = it },
                                    allClothes = existingClothes,
                                    isExplainExpanded = expandedExplainFields["sit breast ok: with side out bra"] == true,
                                    onToggleExplainExpand = { expandedExplainFields["sit breast ok: with side out bra"] = !(expandedExplainFields["sit breast ok: with side out bra"] ?: false) },
                                    dictionaryService = dictionaryService
                                )

                                // center chest
                                EditFieldWithPrevious(
                                    label = "CENTER CHEST",
                                    value = editCenterChest,
                                    placeholder = "e.g. center chest, no center chest",
                                    onValueChange = { editCenterChest = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "center chest" to { editCenterChest = it }
                                    },
                                    explainValue = editFieldExplanations["center chest"] ?: "",
                                    onExplainChange = { editFieldExplanations["center chest"] = it }
                                )

                                // beautiful
                                EditFieldWithPrevious(
                                    label = "BEAUTIFUL",
                                    value = editBeautiful,
                                    placeholder = "e.g. Beautiful, not so beautiful",
                                    onValueChange = { editBeautiful = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "beautiful" to { editBeautiful = it }
                                    },
                                    explainValue = editFieldExplanations["beautiful"] ?: "",
                                    onExplainChange = { editFieldExplanations["beautiful"] = it }
                                )

                                // beautiful reason
                                EditFieldWithPrevious(
                                    label = "BEAUTIFUL REASON",
                                    value = editBeautifulReason,
                                    placeholder = "e.g. elegant lace patterns",
                                    onValueChange = { editBeautifulReason = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "beautiful reason" to { editBeautifulReason = it }
                                    },
                                    explainValue = editFieldExplanations["beautiful reason"] ?: "",
                                    onExplainChange = { editFieldExplanations["beautiful reason"] = it }
                                )

                                // wet
                                EditFieldWithPrevious(
                                    label = "WET",
                                    value = editWet,
                                    placeholder = "e.g. dry, wet, quick-dry",
                                    onValueChange = { editWet = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "wet" to { editWet = it }
                                    },
                                    explainValue = editFieldExplanations["wet"] ?: "",
                                    onExplainChange = { editFieldExplanations["wet"] = it }
                                )

                                // wet reason
                                EditFieldWithPrevious(
                                    label = "WET REASON",
                                    value = editWetReason,
                                    placeholder = "e.g. breathable lace airflow",
                                    onValueChange = { editWetReason = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "wet reason" to { editWetReason = it }
                                    },
                                    explainValue = editFieldExplanations["wet reason"] ?: "",
                                    onExplainChange = { editFieldExplanations["wet reason"] = it }
                                )

                                // long dress
                                EditFieldWithPrevious(
                                    label = "LONG DRESS",
                                    value = editLongDress,
                                    placeholder = "e.g. long dress, short dress",
                                    onValueChange = { editLongDress = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "long dress" to { editLongDress = it }
                                    },
                                    explainValue = editFieldExplanations["long dress"] ?: "",
                                    onExplainChange = { editFieldExplanations["long dress"] = it }
                                )

                                // may throw
                                EditFieldWithPrevious(
                                    label = "MAY THROW",
                                    value = editMayThrow,
                                    placeholder = "e.g. test, keep in closet, donate",
                                    onValueChange = { editMayThrow = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "may throw" to { editMayThrow = it }
                                    },
                                    explainValue = editFieldExplanations["may throw"] ?: "",
                                    onExplainChange = { editFieldExplanations["may throw"] = it }
                                )

                                // wearing frequency (legacy)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "WEARING FREQUENCY (LEGACY)",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    OutlinedTextField(
                                        value = editWearingFrequency,
                                        onValueChange = { editWearingFrequency = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("e.g. 160", fontSize = 13.sp, color = Slate400) },
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
                                }

                                // Tracked App Washes
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "TRACKED APP WASHES",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    OutlinedTextField(
                                        value = editTrackedAppWashes,
                                        onValueChange = { editTrackedAppWashes = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("e.g. 1", fontSize = 13.sp, color = Slate400) },
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
                                }
                            }
                        }

                        // CARD 3: DESCRIPTIONS & OBSERVATIONS
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
                                    text = "DESCRIPTIONS & OBSERVATIONS",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )

                                EditFieldWithPrevious(
                                    label = "DESCRIPTIONS",
                                    value = editDescriptions,
                                    placeholder = "e.g. a affordable center chest , and hot thick dress",
                                    onValueChange = { editDescriptions = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "Descriptions" to { editDescriptions = it }
                                    },
                                    explainValue = editFieldExplanations["Descriptions"] ?: "",
                                    onExplainChange = { editFieldExplanations["Descriptions"] = it },
                                    isMultiline = true
                                )

                                EditFieldWithPrevious(
                                    label = "DESCRIPTIONS 2",
                                    value = editDescriptions2,
                                    placeholder = "Optional secondary notes...",
                                    onValueChange = { editDescriptions2 = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "Descriptions 2" to { editDescriptions2 = it }
                                    },
                                    explainValue = editFieldExplanations["Descriptions 2"] ?: "",
                                    onExplainChange = { editFieldExplanations["Descriptions 2"] = it },
                                    isMultiline = true
                                )

                                EditFieldWithPrevious(
                                    label = "LAST COMMENT OR ACTION",
                                    value = editLastCommentAction,
                                    placeholder = "e.g. test, Hand wash cycle",
                                    onValueChange = { editLastCommentAction = it },
                                    onOpenPrevious = {
                                        activePreviousPicker = "last comment or action" to { editLastCommentAction = it }
                                    },
                                    explainValue = editFieldExplanations["last comment or action"] ?: "",
                                    onExplainChange = { editFieldExplanations["last comment or action"] = it }
                                )
                            }
                        }

                    } else {
                        // ------------------------------------------
                        // VIEW MODE CONTENT (From screenshots IMG_9639, 9640, 9641)
                        // ------------------------------------------

                        // Title section
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "GARMENT RECORD · NAME",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )

                            Text(
                                text = garment.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )

                            val thick = garment.thickClothes.ifBlank { "03 thick clothes" }
                            val mat = garment.material.ifBlank { "lace" }
                            Text(
                                text = "$mat · $thick · long dress",
                                fontSize = 12.sp,
                                color = Slate600
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                ) {
                                    Text(
                                        text = "Date: 25 Sep 2026",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate700,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isNeedWash) Color(0xFFF59E0B) else Color(0xFF059669),
                                    modifier = Modifier.clickable {
                                        onMarkWashNow?.invoke(garment)
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isNeedWash) Icons.Default.Warning else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isNeedWash) "Wash Now (${unwashedHours}h)" else "Clean",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Photos Section
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
                                            text = "GARMENT PHOTOS (${photoUrls.size})",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate700
                                        )
                                    }

                                    Text(
                                        text = "Click photo to view fullscreen",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BrandPurple
                                    )
                                }

                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (photoUrls.isNotEmpty()) {
                                        photoUrls.forEach { rawUrl ->
                                            val model = ImageUtils.resolvePhotoModel(context, rawUrl)
                                            Box(
                                                modifier = Modifier
                                                    .size(140.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color(0xFFF1F5F9))
                                                    .border(BorderStroke(1.dp, Slate200), RoundedCornerShape(16.dp))
                                                    .clickable { photoPickerLauncher.launch("image/*") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (model != null) {
                                                    AsyncImage(
                                                        model = model,
                                                        contentDescription = "Garment photo",
                                                        contentScale = ContentScale.Fit,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = Slate400,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .border(BorderStroke(1.dp, Slate200), RoundedCornerShape(16.dp))
                                                .clickable { photoPickerLauncher.launch("image/*") },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Add Photo",
                                                tint = Slate400,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4 Metrics Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                label = "TOTAL WEARS",
                                value = totalWearsDisplay,
                                valueColor = Slate900,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "TOTAL DURATION",
                                value = totalDurationHours,
                                valueColor = Slate900,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                label = "WASHES COUNT",
                                value = totalWashesCount.toString(),
                                valueColor = Slate900,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "COST PER WEAR",
                                value = "$$cpw",
                                valueColor = BrandPurple,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Attributes & Ergonomics spec table
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "GARMENT ATTRIBUTES & ERGONOMICS",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                ErgonomicRow("Clothes Type:", garment.clothesType.ifBlank { "—" }, explanation = fieldExplanationsInView["Clothes Type"])
                                ErgonomicRow("material:", garment.material.ifBlank { "—" }, explanation = fieldExplanationsInView["material"])
                                ErgonomicRow("thick clothes:", garment.thickClothes.ifBlank { "—" }, boldValue = true, explanation = fieldExplanationsInView["thick clothes"])
                                ErgonomicRow("Hot feeling (without AC):", (garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }).ifBlank { "—" }, boldValue = true, explanation = fieldExplanationsInView["when without aircond: hot feeling"] ?: fieldExplanationsInView["Hot at noon"])
                                ErgonomicRow("diao dai:", garment.diaoDai.ifBlank { "—" }, explanation = fieldExplanationsInView["diao dai"])
                                ErgonomicRow("side breast ok: with 聚拢 bra:", (garment.sideBreastGatherBra.ifBlank { garment.sideBreastOk }).ifBlank { "—" }, explanation = fieldExplanationsInView["side breast ok: with 聚拢 bra"] ?: fieldExplanationsInView["side breast ok"])
                                ErgonomicRow("side breast ok: with side out bra:", garment.sideBreastSideOutBra.ifBlank { "—" }, explanation = fieldExplanationsInView["side breast ok: with side out bra"])
                                ErgonomicRow("sit breast ok: With 聚拢bra:", (garment.sitBreastGatherBra.ifBlank { garment.sitBreast }).ifBlank { "—" }, explanation = fieldExplanationsInView["sit breast ok: With 聚拢bra:"] ?: fieldExplanationsInView["sit breast ok"])
                                ErgonomicRow("sit breast ok: with side out bra:", garment.sitBreastSideOutBra.ifBlank { "—" }, explanation = fieldExplanationsInView["sit breast ok: with side out bra"])
                                ErgonomicRow("center chest:", garment.standBreast.ifBlank { "—" }, explanation = fieldExplanationsInView["center chest"])
                                ErgonomicRow("beautiful:", garment.beautiful.ifBlank { "—" }, explanation = fieldExplanationsInView["beautiful"])
                                ErgonomicRow("wet:", garment.wet.ifBlank { "—" }, explanation = fieldExplanationsInView["wet"])
                                ErgonomicRow("long dress:", garment.longDress.ifBlank { "—" }, explanation = fieldExplanationsInView["long dress"])
                                ErgonomicRow("may throw:", garment.mayThrow.ifBlank { "—" }, explanation = fieldExplanationsInView["may throw"])
                                ErgonomicRow("price:", if (garment.price > 0) "$${garment.price}" else "$0", explanation = fieldExplanationsInView["price"])
                                ErgonomicRow("wearing frequency(legacy):", garment.wearingFrequency.toString())
                                ErgonomicRow("Total Washed Count:", totalWashesCount.toString(), boldValue = true)
                                ErgonomicRow("Tracked App Washes:", garment.trackedAppWashes.toString())
                                ErgonomicRow("Last Washed:", garment.lastWashed.ifBlank { "Never" }, explanation = fieldExplanationsInView["Last Washed"])
                                ErgonomicRow("unwash time rollup:", if (garmentStats.unwashedSeconds > 0) "${garmentStats.unwashedHours}h (${garmentStats.unwashedSeconds}s)" else "0s")
                                ErgonomicRow("created:", displayCreatedDate)
                                ErgonomicRow("wet reason:", garment.wetReason.ifBlank { "No data" }, isItalic = garment.wetReason.isBlank(), explanation = fieldExplanationsInView["wet reason"])
                                ErgonomicRow("beatiful reason:", garment.beautifulReason.ifBlank { "No data" }, isItalic = garment.beautifulReason.isBlank(), explanation = fieldExplanationsInView["beautiful reason"] ?: fieldExplanationsInView["beatiful reason"])
                            }
                        }

                        // 3 Environmental Scenarios Expandable Cards in View Mode
                        EnvironmentalScenariosViewSection(
                            garment = garment,
                            fieldExplanations = fieldExplanationsInView
                        )

                        // Descriptions & Observations
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "DESCRIPTIONS & OBSERVATIONS",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Descriptions:",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    Text(
                                        text = garment.descriptions.ifBlank { garment.lastComment.ifBlank { "No data" } },
                                        fontSize = 13.sp,
                                        color = Slate800
                                    )
                                    val descExplain = fieldExplanationsInView["Descriptions"]
                                    if (!descExplain.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFAF5FF),
                                            border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = BrandPurple,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = descExplain,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF6B21A8),
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Descriptions 2:",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    Text(
                                        text = garment.descriptions2.ifBlank { "No data" },
                                        fontSize = 12.sp,
                                        fontStyle = if (garment.descriptions2.isBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                        color = if (garment.descriptions2.isBlank()) Slate400 else Slate800
                                    )
                                    val desc2Explain = fieldExplanationsInView["Descriptions 2"]
                                    if (!desc2Explain.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFAF5FF),
                                            border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = BrandPurple,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = desc2Explain,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF6B21A8),
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "last comment or action:",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate600
                                    )
                                    Text(
                                        text = garment.lastComment.ifBlank { garment.mayThrow.ifBlank { "No data" } },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate800
                                    )
                                    val lastCommentExplain = fieldExplanationsInView["last comment or action"]
                                    if (!lastCommentExplain.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFAF5FF),
                                            border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = BrandPurple,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = lastCommentExplain,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF6B21A8),
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Attributes Section (Matching IMG_9729.png)
                        var customAttrsMap by remember(garment.customFields) {
                            val map = linkedMapOf<String, String>()
                            try {
                                if (garment.customFields.isNotBlank() && garment.customFields != "{}") {
                                    val obj = JSONObject(garment.customFields)
                                    val keys = obj.keys()
                                    while (keys.hasNext()) {
                                        val k = keys.next()
                                        map[k] = obj.optString(k, "")
                                    }
                                }
                            } catch (_: Exception) {}
                            mutableStateOf<Map<String, String>>(map)
                        }
                        var showAddAttrModal by remember { mutableStateOf(false) }
                        var newAttrName by remember { mutableStateOf("") }
                        var newAttrValue by remember { mutableStateOf("") }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CUSTOM ATTRIBUTES",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF3E8FF),
                                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                    modifier = Modifier
                                        .clickable { showAddAttrModal = true }
                                        .testTag("add_custom_attribute_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = BrandPurple,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Add Attribute",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple
                                        )
                                    }
                                }
                            }

                            if (customAttrsMap.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No custom attributes added yet. Click '+ Add Attribute' in Edit Mode.",
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Slate400,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    customAttrsMap.forEach { (attrKey, attrVal) ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = attrKey,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate500
                                                    )
                                                    Text(
                                                        text = attrVal,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Slate900
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = customAttrsMap.toMutableMap().apply { remove(attrKey) }
                                                        customAttrsMap = updated
                                                        editFieldExplanations.remove(attrKey.removeSuffix("_explain"))
                                                        val json = JSONObject(updated).toString()
                                                        onSaveGarment?.invoke(garment.copy(customFields = json))
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Attribute",
                                                        tint = Slate400,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showAddAttrModal) {
                            AlertDialog(
                                onDismissRequest = { showAddAttrModal = false },
                                title = {
                                    Text("Add Custom Attribute", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = newAttrName,
                                            onValueChange = { newAttrName = it },
                                            label = { Text("Attribute Name (e.g., Lining, Neckline)", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = newAttrValue,
                                            onValueChange = { newAttrValue = it },
                                            label = { Text("Value", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val k = newAttrName.trim()
                                            val v = newAttrValue.trim()
                                            if (k.isNotEmpty() && v.isNotEmpty()) {
                                                val updated = customAttrsMap.toMutableMap().apply { put(k, v) }
                                                customAttrsMap = updated
                                                val json = JSONObject(updated).toString()
                                                onSaveGarment?.invoke(garment.copy(customFields = json))
                                                newAttrName = ""
                                                newAttrValue = ""
                                                showAddAttrModal = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                                    ) {
                                        Text("Save Attribute", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddAttrModal = false }) {
                                        Text("Cancel", color = Slate600)
                                    }
                                },
                                containerColor = Color.White,
                                shape = RoundedCornerShape(20.dp)
                            )
                        }

                        // Multi-Version AI 衣物人体工学与穿搭总结 Card (Matching IMG_9729.png & IMG_9730.png)
                        var aiReportVersions by remember(garment.aiReports, garment.aiClothesAnalysis) {
                            val list = mutableListOf<GarmentAiReportVersion>()
                            try {
                                if (garment.aiReports.isNotBlank() && garment.aiReports != "[]") {
                                    val arr = JSONArray(garment.aiReports)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(
                                            GarmentAiReportVersion(
                                                id = obj.optString("id", "rep_$i"),
                                                title = obj.optString("title", "Initial Report 12/13"),
                                                timestamp = obj.optString("timestamp", ""),
                                                content = obj.optString("content", ""),
                                                isPrimary = obj.optBoolean("isPrimary", i == 0)
                                            )
                                        )
                                    }
                                }
                            } catch (_: Exception) {}
                            if (list.isEmpty() && garment.aiClothesAnalysis.isNotBlank()) {
                                list.add(
                                    GarmentAiReportVersion(
                                        id = "initial_seed",
                                        title = "Initial Report 12/13",
                                        timestamp = "2025-12-13",
                                        content = garment.aiClothesAnalysis,
                                        isPrimary = true
                                    )
                                )
                            }
                            mutableStateOf<List<GarmentAiReportVersion>>(list)
                        }

                        var activeReportId by remember(aiReportVersions) {
                            mutableStateOf(aiReportVersions.firstOrNull()?.id ?: "")
                        }
                        val activeAiVersion = aiReportVersions.find { it.id == activeReportId } ?: aiReportVersions.firstOrNull()
                        var hasCopiedAiReport by remember(activeReportId) { mutableStateOf(false) }

                        if (!effectiveAiPaused) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFFAF5FF),
                                border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header row: Sparkle + Title + [生成新版本] button (IMG_9729.png)
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
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = BrandPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "AI 衣物人体工学与穿搭总结",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3B0764),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Button(
                                        onClick = {
                                            if (isGeneratingAiReport) return@Button
                                            isGeneratingAiReport = true
                                            val nextNum = aiReportVersions.size + 1
                                            val dateLabel = SimpleDateFormat("MM/dd", Locale.US).format(Date())
                                            val fallbackBuilder = {
                                                GeminiAiService.buildDefaultGarmentErgonomicReport(context, garment, totalWashesCount, "$$cpw")
                                            }

                                            coroutineScope.launch {
                                                try {
                                                    val aiResp = GeminiAiService.generateGarmentAuditReport(
                                                        context = context,
                                                        garment = garment,
                                                        stats = garmentStats,
                                                        garmentJunctions = garmentJunctions,
                                                        reportsById = reportsById,
                                                        allClothes = existingClothes,
                                                        versionNumber = nextNum,
                                                        fallbackGenerator = fallbackBuilder
                                                    )
                                                    val generatedContent = aiResp.text
                                                    val newVersion = GarmentAiReportVersion(
                                                        id = "rep_${System.currentTimeMillis()}",
                                                        title = if (aiReportVersions.isEmpty()) "Initial Report $dateLabel" else "Report #$nextNum ($dateLabel)",
                                                        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                                                        content = generatedContent,
                                                        isPrimary = aiReportVersions.isEmpty()
                                                    )
                                                    val updatedList = listOf(newVersion) + aiReportVersions
                                                    aiReportVersions = updatedList
                                                    activeReportId = newVersion.id

                                                    val arr = JSONArray()
                                                    updatedList.forEach { v ->
                                                        arr.put(
                                                            JSONObject().apply {
                                                                put("id", v.id)
                                                                put("title", v.title)
                                                                put("timestamp", v.timestamp)
                                                                put("content", v.content)
                                                                put("isPrimary", v.isPrimary)
                                                            }
                                                        )
                                                    }
                                                    onSaveGarment?.invoke(
                                                        garment.copy(
                                                            aiClothesAnalysis = generatedContent,
                                                            aiReports = arr.toString()
                                                        )
                                                    )
                                                    Toast.makeText(context, "已生成新版本 AI 诊断报告！", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    android.util.Log.e("GarmentDetailDialog", "Failed to generate AI report", e)
                                                    Toast.makeText(context, "生成报告失败: ${e.localizedMessage ?: "未知异常"}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isGeneratingAiReport = false
                                                }
                                            }
                                        },
                                        enabled = !isGeneratingAiReport,
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("generate_new_ai_version_button")
                                    ) {
                                        if (isGeneratingAiReport) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("生成中...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("生成新版本", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                // Version Tabs + Copy Button Row (IMG_9729.png)
                                if (aiReportVersions.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            aiReportVersions.forEach { ver ->
                                                val isSelected = ver.id == activeAiVersion?.id
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) BrandPurple else Color.White,
                                                    border = BorderStroke(1.dp, if (isSelected) BrandPurple else Color(0xFFD8B4FE)),
                                                    modifier = Modifier.clickable { activeReportId = ver.id }
                                                ) {
                                                    Text(
                                                        text = ver.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else Slate700,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                            modifier = Modifier.clickable {
                                                val textToCopy = activeAiVersion?.content ?: ""
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Garment Report", textToCopy))
                                                hasCopiedAiReport = true
                                                Toast.makeText(context, "已复制报告内容", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (hasCopiedAiReport) Icons.Default.Check else Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = if (hasCopiedAiReport) Color(0xFF059669) else Slate600,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (hasCopiedAiReport) "Copied" else "Copy",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (hasCopiedAiReport) Color(0xFF059669) else Slate700
                                                )
                                            }
                                        }
                                    }
                                }

                                // Report Content Box
                                if (activeAiVersion != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = activeAiVersion.content,
                                            fontSize = 12.sp,
                                            color = Slate800,
                                            lineHeight = 19.sp,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White.copy(alpha = 0.8f),
                                        border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "当前单品暂无 AI 诊断报告。点击右上角「生成新版本」按钮，系统将根据衣物的面料、厚度、Hot at noon、diao dai、sit breast 等体感记录生成专属人体工学诊断总结。",
                                            fontSize = 11.sp,
                                            color = Slate500,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                        // Wearing History (N sessions) Section matching IMG_9730.png & IMG_9731.png
                        var selectedReportForSessionModal by remember { mutableStateOf<ReportEntity?>(null) }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "WEARING HISTORY (${garmentJunctions.size} SESSIONS)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                                Text(
                                    text = "click session to view/edit",
                                    fontSize = 11.sp,
                                    color = BrandPurple
                                )
                            }

                            if (garmentJunctions.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No wear sessions recorded for this garment yet.",
                                        fontSize = 12.sp,
                                        color = Slate400,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                garmentJunctions.forEach { j ->
                                    val rep = reportsById[j.wearingReportId]
                                        ?: reports.find { r ->
                                            (j.wearingReportTitle.isNotBlank() && r.name.equals(j.wearingReportTitle, ignoreCase = true)) ||
                                            (j.wearingReportId.isNotBlank() && r.name.equals(j.wearingReportId, ignoreCase = true)) ||
                                            (j.wearingReportTitle.isNotBlank() && r.id.equals(j.wearingReportTitle, ignoreCase = true))
                                        }
                                        ?: ReportEntity(
                                            id = j.wearingReportId.ifBlank { "rep_${j.id}" },
                                            name = j.wearingReportTitle.ifBlank { "Wearing Session" },
                                            wearDate = j.wearDate
                                        )
                                    val wDate = rep?.wearDate?.ifBlank { rep.created } ?: j.wearDate
                                    val normWearDate = if (wDate.length >= 10) wDate.substring(0, 10) else wDate
                                    val rTitle = rep?.name?.ifBlank { j.wearingReportTitle } ?: j.wearingReportTitle.ifBlank { "Wearing Session" }
                                    val hrs = String.format(Locale.US, "%.1f", j.duration / 3600.0)
                                    val sessionDirty = j.washStated.equals("need wash", ignoreCase = true)
                                    val wshDate = j.washDate.ifBlank { j.washedDate }
                                    val cleanWashDate = if (wshDate.length >= 10) wshDate.substring(0, 10) else wshDate

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (rep != null) {
                                                    if (onSelectReport != null) {
                                                        onSelectReport(rep)
                                                    } else {
                                                        selectedReportForSessionModal = rep
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarToday,
                                                        contentDescription = null,
                                                        tint = BrandPurple,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = normWearDate.ifBlank { "Wear Session" },
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate900
                                                    )
                                                }

                                                Text(
                                                    text = rTitle,
                                                    fontSize = 12.sp,
                                                    color = Slate500,
                                                    maxLines = 1
                                                )

                                                // Status pill below title matching IMG_9731.png ("need wash" or "Cleaned (wash date: 2025-07-06)")
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (sessionDirty) Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                                ) {
                                                    val pillText = if (sessionDirty) {
                                                        "need wash"
                                                    } else if (cleanWashDate.isNotBlank()) {
                                                        "Cleaned (wash date: $cleanWashDate)"
                                                    } else {
                                                        "Cleaned"
                                                    }
                                                    Text(
                                                        text = pillText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (sessionDirty) Color(0xFFB45309) else Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "$hrs hrs",
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Slate600
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = "Open Session",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedReportForSessionModal != null) {
                            SessionDetailModal(
                                isOpen = true,
                                report = selectedReportForSessionModal,
                                junctions = junctions,
                                clothes = existingClothes,
                                onClose = { selectedReportForSessionModal = null },
                                onSelectGarment = {},
                                onUpdateWashStatus = { _, _ -> },
                                onDeleteReport = { selectedReportForSessionModal = null }
                            )
                        }

                        // Tags
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "🏷️", fontSize = 12.sp)
                                Text(
                                    text = "TAGS:",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                tagsList.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Slate300)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // ==============================
                // BOTTOM BAR (ID on left, Action on right matching screenshot)
                // ==============================
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ID: ${garment.id}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate400,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (isEditing) {
                        // Cancel text button + Purple [Save Changes] button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate600,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clickable { handleCancelEditing() }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            )

                            Button(
                                onClick = { handleSaveChanges() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                modifier = Modifier.testTag("save_changes_bottom_button")
                            ) {
                                Text(
                                    text = "Save Changes",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Close",
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
    }

    // Previous Entries Modal
    activePreviousPicker?.let { (fieldKey, onValueSelected) ->
        val entries = remember(fieldKey) { getPreviousValuesForField(fieldKey) }
        val currentVal = when (fieldKey) {
            "Clothes Type" -> editClothesType
            "material" -> editMaterial
            "thick clothes" -> editThickClothes
            "Hot at noon" -> editHotAtNoon
            "diao dai" -> editDiaoDai
            "side breast ok: with 聚拢 bra" -> editSideBreastGatherBra
            "side breast ok: with side out bra" -> editSideBreastSideOutBra
            "when with aircond: hot feeling" -> editAircondHotFeeling
            "when with aircond: leg feeling" -> editAircondLegFeeling
            "when without aircond: hot feeling" -> editNoAircondHotFeeling
            "when without aircond: leg feeling" -> editNoAircondLegFeeling
            "when sleep under fan: hot feeling" -> editFanSleepHotFeeling
            "when sleep under fan: leg feeling" -> editFanSleepLegFeeling
            "sit breast ok: With 聚拢bra:", "sit breast ok: with 聚拢 bra" -> editSitBreastGatherBra
            "sit breast ok: with side out bra" -> editSitBreastSideOutBra
            "center chest" -> editCenterChest
            "beautiful" -> editBeautiful
            "wet" -> editWet
            "may throw" -> editMayThrow
            "Descriptions" -> editDescriptions
            else -> ""
        }
        PreviousEntriesModal(
            isOpen = true,
            fieldLabel = fieldKey,
            entries = entries,
            currentValue = currentVal,
            onSelect = { selectedValue ->
                onValueSelected(selectedValue)
                activePreviousPicker = null
            },
            onClose = { activePreviousPicker = null }
        )
    }
}

/**
 * Reusable edit field with label, Previous button, outlined text field,
 * and ErgonomicExplainField directly underneath matching the webapp's xd component.
 */
@Composable
private fun EditFieldWithPrevious(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onOpenPrevious: () -> Unit,
    explainValue: String = "",
    onExplainChange: ((String) -> Unit)? = null,
    isMultiline: Boolean = false,
    fieldKey: String = label,
    allClothes: List<GarmentEntity> = emptyList(),
    isExplainExpanded: Boolean = false,
    onToggleExplainExpand: (() -> Unit)? = null,
    dictionaryService: FieldDictionaryService? = null,
    explainPlaceholder: String? = null
) {
    var localExplainExpanded by remember { mutableStateOf(false) }
    val effectiveExpanded = onToggleExplainExpand?.let { isExplainExpanded } ?: localExplainExpanded
    val effectiveToggle = onToggleExplainExpand ?: { localExplainExpanded = !localExplainExpanded }

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

            // Previous Pill Button
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
            minLines = if (isMultiline) 2 else 1,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Slate200,
                focusedBorderColor = BrandPurple
            )
        )

        // Directly below input: Ergonomic Explain Field matching web app xd
        if (onExplainChange != null) {
            ErgonomicExplainField(
                fieldKey = fieldKey,
                fieldLabel = label,
                value = explainValue,
                onValueChange = onExplainChange,
                isExpanded = effectiveExpanded,
                onToggleExpand = effectiveToggle,
                allClothes = allClothes,
                customPlaceholder = explainPlaceholder,
                dictionaryService = dictionaryService
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun ErgonomicRow(
    label: String,
    value: String,
    boldValue: Boolean = false,
    isItalic: Boolean = false,
    explanation: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = Slate500
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.SemiBold,
            fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
            color = if (isItalic) Slate400 else Slate900,
            modifier = Modifier.fillMaxWidth()
        )
        if (!explanation.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF5FF),
                border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = explanation,
                        fontSize = 11.sp,
                        color = Color(0xFF6B21A8),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
