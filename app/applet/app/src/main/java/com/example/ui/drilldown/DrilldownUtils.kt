package com.example.ui.drilldown

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Garment statistics derived from wear junctions
 */
data class GarmentStats(
    val needWash: Boolean,
    val unwashedSeconds: Long,
    val unwashedHours: String,
    val totalWears: Int,
    val totalDurationHours: Long,
    val totalDurationSeconds: Long = totalDurationHours * 3600L,
    val totalHours: String = String.format(Locale.US, "%.1f", totalDurationHours.toDouble()),
    val latestWashDate: String = ""
)

object DrilldownUtils {

    /**
     * Compute statistics for a garment based on wear history junctions
     */
    fun calculateGarmentStats(garmentId: String, junctions: List<JunctionEntity>): GarmentStats {
        val matches = junctions.filter { it.clothesId == garmentId }
        val needWashItems = matches.filter { it.washStated.equals("need wash", ignoreCase = true) }
        val needWash = needWashItems.isNotEmpty()
        val unwashedSec = needWashItems.sumOf { it.duration }
        val unwashedHrs = String.format(Locale.US, "%.1f", unwashedSec / 3600.0)
        val totalWears = matches.size
        val totalSec = matches.sumOf { it.duration }
        val totalDurationHrs = kotlin.math.round(totalSec / 3600.0).toLong()
        val totalHrsStr = String.format(Locale.US, "%.1f", totalSec / 3600.0)
        val latestWash = matches
            .mapNotNull { j -> j.washedDate.ifBlank { j.washDate }.takeIf { it.isNotBlank() } }
            .maxOrNull() ?: ""

        return GarmentStats(
            needWash = needWash,
            unwashedSeconds = unwashedSec,
            unwashedHours = unwashedHrs,
            totalWears = totalWears,
            totalDurationHours = totalDurationHrs,
            totalDurationSeconds = totalSec,
            totalHours = totalHrsStr,
            latestWashDate = latestWash
        )
    }

    /**
     * Get formatted display string for any attribute key.
     * Returns "undefined" if empty, blank, or missing.
     */
    fun getFieldValue(garment: GarmentEntity, fieldKey: String, junctions: List<JunctionEntity>): String {
        return when (fieldKey) {
            "wash stated" -> {
                val stats = calculateGarmentStats(garment.id, junctions)
                if (stats.needWash) "Need Wash" else "Clean"
            }
            "Total Washed Count" -> {
                (garment.wearingFrequency + garment.trackedAppWashes).toString()
            }
            "Tracked App Washes" -> {
                garment.trackedAppWashes.toString()
            }
            "wearing frequency(how many times legacy washed)" -> {
                garment.wearingFrequency.toString()
            }
            "Last Washed" -> {
                garment.lastWashed.ifBlank { "undefined" }
            }
            "created" -> {
                garment.created.ifBlank { "undefined" }
            }
            "price" -> {
                if (garment.price > 0) String.format(Locale.US, "$%.2f", garment.price) else "$0.00"
            }
            "Clothes Type", "clothesType" -> garment.clothesType.ifBlank { "undefined" }
            "material", "materialProp" -> garment.material.ifBlank { "undefined" }
            "thick clothes", "thickClothes" -> garment.thickClothes.ifBlank { "undefined" }
            "Hot at noon", "hotAtNoon" -> (garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }).ifBlank { "undefined" }
            "when without aircond: hot feeling" -> (garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }).ifBlank { "undefined" }
            "when without aircond: leg feeling" -> garment.noAircondLegFeeling.ifBlank { "undefined" }
            "when with aircond: hot feeling" -> garment.aircondHotFeeling.ifBlank { "undefined" }
            "when with aircond: leg feeling" -> garment.aircondLegFeeling.ifBlank { "undefined" }
            "when sleep under fan: hot feeling" -> garment.fanSleepHotFeeling.ifBlank { "undefined" }
            "when sleep under fan: leg feeling" -> garment.fanSleepLegFeeling.ifBlank { "undefined" }
            "beautiful" -> garment.beautiful.ifBlank { "undefined" }
            "beatiful reason", "beautiful reason", "beautifulReason" -> garment.beautifulReason.ifBlank { "undefined" }
            "diao dai", "diaoDai" -> garment.diaoDai.ifBlank { "undefined" }
            "side breast ok", "side breast shape", "sideBreastOk" -> (garment.sideBreastGatherBra.ifBlank { garment.sideBreastOk }).ifBlank { "undefined" }
            "side breast ok: with 聚拢 bra" -> (garment.sideBreastGatherBra.ifBlank { garment.sideBreastOk }).ifBlank { "undefined" }
            "side breast ok: with side out bra" -> garment.sideBreastSideOutBra.ifBlank { "undefined" }
            "sit breast ok", "sit breast", "sitBreast" -> (garment.sitBreastGatherBra.ifBlank { garment.sitBreast }).ifBlank { "undefined" }
            "sit breast ok: With 聚拢bra", "sit breast ok: With 聚拢bra:", "sit breast ok: with 聚拢 bra" -> (garment.sitBreastGatherBra.ifBlank { garment.sitBreast }).ifBlank { "undefined" }
            "sit breast ok: with side out bra" -> garment.sitBreastSideOutBra.ifBlank { "undefined" }
            "center chest", "stand breast", "standBreast" -> garment.standBreast.ifBlank { "undefined" }
            "wet" -> garment.wet.ifBlank { "undefined" }
            "wet reason", "wetReason" -> garment.wetReason.ifBlank { "undefined" }
            "may throw", "mayThrow" -> garment.mayThrow.ifBlank { "undefined" }
            "long dress", "longDress" -> garment.longDress.ifBlank { "undefined" }
            "Name", "name" -> garment.name.ifBlank { "(Unnamed Garment)" }
            "Descriptions", "descriptions" -> garment.descriptions.ifBlank { "undefined" }
            "Descriptions 2", "descriptions2" -> garment.descriptions2.ifBlank { "undefined" }
            "last comment or action", "lastComment" -> garment.lastComment.ifBlank { "undefined" }
            else -> "undefined"
        }
    }

    /**
     * Extract numerical value for sorting
     */
    fun getNumericalSortValue(garment: GarmentEntity, fieldKey: String, junctions: List<JunctionEntity>): Double {
        return when (fieldKey) {
            "wash stated" -> {
                val stats = calculateGarmentStats(garment.id, junctions)
                if (stats.needWash) 1.0 else 0.0
            }
            "Total Washed Count" -> (garment.wearingFrequency + garment.trackedAppWashes).toDouble()
            "wearing frequency(how many times legacy washed)" -> garment.wearingFrequency.toDouble()
            "Tracked App Washes" -> garment.trackedAppWashes.toDouble()
            "price" -> garment.price
            else -> {
                val raw = getFieldValue(garment, fieldKey, junctions)
                raw.replace("$", "").trim().toDoubleOrNull() ?: 0.0
            }
        }
    }
}

/**
 * Service to persist and manage report presets matching webapp ReportPresetService
 */
class ReportPresetService(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wardrobe_drilldown_presets_v3", Context.MODE_PRIVATE)

    companion object {
        private const val PRESETS_KEY = "custom_presets_json_v3"
        private const val ACTIVE_PRESET_ID_KEY = "active_preset_id_v3"
    }

    private fun loadPresetsFromAsset(): List<DrillPreset> {
        return try {
            context.assets.open("drilldown_presets.json").use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val jsonStr = reader.readText()
                parsePresetsJson(jsonStr)
            }
        } catch (e: Exception) {
            DEFAULT_DRILL_PRESETS
        }
    }

    private fun parsePresetsJson(jsonStr: String): List<DrillPreset> {
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<DrillPreset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val tiers = mutableListOf<String>()
                val tiersArray = obj.optJSONArray("groupTiers")
                if (tiersArray != null) {
                    for (t in 0 until tiersArray.length()) tiers.add(tiersArray.getString(t))
                }
                val visible = mutableListOf<String>()
                val visibleArray = obj.optJSONArray("visibleFields")
                if (visibleArray != null) {
                    for (v in 0 until visibleArray.length()) visible.add(visibleArray.getString(v))
                }
                list.add(
                    DrillPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        groupTiers = if (tiers.isNotEmpty()) tiers else listOf("Clothes Type"),
                        sortField = obj.optString("sortField", "Name"),
                        sortOrder = obj.optString("sortOrder", "asc"),
                        visibleFields = if (visible.isNotEmpty()) visible else listOf("Clothes Type", "material", "Hot at noon"),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
            val baseList = if (list.isNotEmpty()) list else DEFAULT_DRILL_PRESETS
            val savedIds = baseList.map { it.id }.toSet()
            val missingDefaults = DEFAULT_DRILL_PRESETS.filter { it.id !in savedIds }
            baseList + missingDefaults
        } catch (e: Exception) {
            DEFAULT_DRILL_PRESETS
        }
    }

    fun getPresets(): List<DrillPreset> {
        val jsonStr = prefs.getString(PRESETS_KEY, null)
        if (jsonStr != null) {
            val parsed = parsePresetsJson(jsonStr)
            if (parsed.isNotEmpty()) return parsed
        }
        val fromAsset = loadPresetsFromAsset()
        savePresets(fromAsset)
        return fromAsset
    }

    fun savePresets(presets: List<DrillPreset>) {
        val array = JSONArray()
        for (p in presets) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("description", p.description)
            obj.put("sortField", p.sortField)
            obj.put("sortOrder", p.sortOrder)
            obj.put("isCustom", p.isCustom)
            val tiers = JSONArray()
            p.groupTiers.forEach { tiers.put(it) }
            obj.put("groupTiers", tiers)
            val visible = JSONArray()
            p.visibleFields.forEach { visible.put(it) }
            obj.put("visibleFields", visible)
            array.put(obj)
        }
        prefs.edit().putString(PRESETS_KEY, array.toString()).apply()
    }

    fun savePreset(preset: DrillPreset): List<DrillPreset> {
        val current = getPresets().toMutableList()
        val index = current.indexOfFirst { it.id == preset.id }
        if (index >= 0) {
            current[index] = preset
        } else {
            current.add(preset)
        }
        savePresets(current)
        return current
    }

    fun deletePreset(id: String): List<DrillPreset> {
        val current = getPresets().filter { it.id != id }
        val result = if (current.isNotEmpty()) current else DEFAULT_DRILL_PRESETS
        savePresets(result)
        return result
    }

    fun resetToDefaults(): List<DrillPreset> {
        val defaults = loadPresetsFromAsset()
        savePresets(defaults)
        return defaults
    }

    fun getActivePresetId(): String {
        return prefs.getString(ACTIVE_PRESET_ID_KEY, DEFAULT_DRILL_PRESETS.first().id)
            ?: DEFAULT_DRILL_PRESETS.first().id
    }

    fun setActivePresetId(id: String) {
        prefs.edit().putString(ACTIVE_PRESET_ID_KEY, id).apply()
    }
}
