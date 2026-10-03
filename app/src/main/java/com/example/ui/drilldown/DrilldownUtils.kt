package com.example.ui.drilldown

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import org.json.JSONArray
import org.json.JSONObject
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
     * Get formatted display string for any attribute key
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
                garment.lastWashed.ifBlank { "(Never washed)" }
            }
            "price" -> {
                if (garment.price > 0) String.format(Locale.US, "$%.2f", garment.price) else "$0.00"
            }
            "Clothes Type", "clothesType" -> garment.clothesType.ifBlank { "(Not specified)" }
            "material" -> garment.material.ifBlank { "(Not specified)" }
            "thick clothes", "thickClothes" -> garment.thickClothes.ifBlank { "(Not specified)" }
            "Hot at noon", "hotAtNoon" -> garment.hotAtNoon.ifBlank { "(Not specified)" }
            "beautiful" -> garment.beautiful.ifBlank { "(Not specified)" }
            "beatiful reason", "beautiful reason" -> garment.beautifulReason.ifBlank { "(Not specified)" }
            "diao dai", "diaoDai" -> garment.diaoDai.ifBlank { "(Not specified)" }
            "side breast ok", "side breast shape", "sideBreastOk" -> garment.sideBreastOk.ifBlank { "(Not specified)" }
            "sit breast ok", "sit breast", "sitBreast" -> garment.sitBreast.ifBlank { "(Not specified)" }
            "center chest", "stand breast", "standBreast" -> garment.standBreast.ifBlank { "(Not specified)" }
            "wet" -> garment.wet.ifBlank { "(Not specified)" }
            "wet reason" -> garment.wetReason.ifBlank { "(Not specified)" }
            "may throw", "mayThrow" -> garment.mayThrow.ifBlank { "(Not specified)" }
            "long dress", "longDress" -> garment.longDress.ifBlank { "(Not specified)" }
            "Name", "name" -> garment.name.ifBlank { "(Unnamed Garment)" }
            "Descriptions" -> garment.descriptions.ifBlank { "(No notes)" }
            "Descriptions 2" -> garment.descriptions2.ifBlank { "(No notes)" }
            "last comment or action" -> garment.lastComment.ifBlank { "(No notes)" }
            else -> "(Not specified)"
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
class ReportPresetService(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wardrobe_drilldown_presets_v2", Context.MODE_PRIVATE)

    companion object {
        private const val PRESETS_KEY = "custom_presets_json"
        private const val ACTIVE_PRESET_ID_KEY = "active_preset_id"
    }

    fun getPresets(): List<DrillPreset> {
        val jsonStr = prefs.getString(PRESETS_KEY, null) ?: return DEFAULT_DRILL_PRESETS
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
                        sortField = obj.optString("sortField", "wearing frequency(how many times legacy washed)"),
                        sortOrder = obj.optString("sortOrder", "desc"),
                        visibleFields = if (visible.isNotEmpty()) visible else listOf("Clothes Type", "material", "Hot at noon"),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
            if (list.isNotEmpty()) list else DEFAULT_DRILL_PRESETS
        } catch (e: Exception) {
            DEFAULT_DRILL_PRESETS
        }
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
        savePresets(DEFAULT_DRILL_PRESETS)
        return DEFAULT_DRILL_PRESETS
    }

    fun getActivePresetId(): String {
        return prefs.getString(ACTIVE_PRESET_ID_KEY, DEFAULT_DRILL_PRESETS.first().id)
            ?: DEFAULT_DRILL_PRESETS.first().id
    }

    fun setActivePresetId(id: String) {
        prefs.edit().putString(ACTIVE_PRESET_ID_KEY, id).apply()
    }
}
