package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clothes")
data class GarmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val clothesType: String = "",        // e.g. "Dress", "bra", "Top", "Bottom", "Sleepwear"
    val material: String = "",           // e.g. "Chiffon", "Cotton"
    val thickClothes: String = "",       // e.g. "03 thick clothes", "thin"
    val hotAtNoon: String = "",          // e.g. "Hot", "Not hot"
    val diaoDai: String = "",            // e.g. "not diao dai", "diao dai"
    val sideBreastOk: String = "",       // legacy side breast ok
    val sideBreastGatherBra: String = "", // side breast ok: with 聚拢 bra
    val sideBreastSideOutBra: String = "", // side breast ok: with side out bra
    val aircondHotFeeling: String = "",  // when with aircond: hot feeling
    val aircondHotExplain: String = "",  // when with aircond: hot explanation
    val aircondLegFeeling: String = "",  // when with aircond: leg feeling
    val aircondLegExplain: String = "",  // when with aircond: leg explanation
    val noAircondHotFeeling: String = "", // when without aircond: hot feeling (migrated from hotAtNoon)
    val noAircondHotExplain: String = "", // when without aircond: hot explanation
    val noAircondLegFeeling: String = "", // when without aircond: leg feeling
    val noAircondLegExplain: String = "", // when without aircond: leg explanation
    val fanSleepHotFeeling: String = "", // when sleep under fan: hot feeling
    val fanSleepHotExplain: String = "", // when sleep under fan: hot explanation
    val fanSleepLegFeeling: String = "", // when sleep under fan: leg feeling
    val fanSleepLegExplain: String = "", // when sleep under fan: leg explanation
    val standBreast: String = "",        // e.g. "center chest" / "stand breast"
    val sitBreast: String = "",          // e.g. "sit breast ok"
    val beautiful: String = "",          // e.g. "Beautiful", "not so beautiful"
    val beautifulReason: String = "",    // e.g. "beatiful reason"
    val wet: String = "",                // e.g. "dry", "wet"
    val wetReason: String = "",          // e.g. "wet reason"
    val descriptions: String = "",       // e.g. "Descriptions"
    val descriptions2: String = "",      // e.g. "Descriptions 2"
    val longDress: String = "",          // e.g. "long dress", "short dress", "middle dress"
    val price: Double = 0.0,
    val wearingFrequency: Int = 0,       // legacy washed count
    val trackedAppWashes: Int = 0,
    val lastWashed: String = "",
    val mayThrow: String = "",           // e.g. "may throw", "keep"
    val lastComment: String = "",        // e.g. "last comment or action"
    val aiClothesAnalysis: String = "",  // e.g. "AI Clothes Analysis"
    val aiReports: String = "[]",        // JSON array of AI report versions
    val pictures: String = "[]",         // JSON array of image paths/filenames
    val tags: String = "[]",             // JSON array of tags
    val created: String = "",
    val customFields: String = "{}"
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val name: String,
    val wearDate: String = "",
    val created: String = "",
    val categories: String = "[]",
    val shoulderFeel: String = "",
    val midBreast: String = "",
    val nightMidBreast: String = "",
    val sweat: String = "",
    val nightSweat: String = "",
    val legFeel: String = "",
    val nightLegFeel: String = "",
    val reportDetail: String = "",
    val aiWearingAnalysis: String = "",
    val aiReports: String = "[]",
    val customFields: String = "{}",
    val pictures: String = "[]",
    val tags: String = "[]"
)

@Entity(tableName = "junctions")
data class JunctionEntity(
    @PrimaryKey val id: String,
    val name: Int = 0,
    val wearingReportId: String,
    val wearingReportTitle: String = "",
    val clothesId: String,
    val clothesTitle: String = "",
    val clothesType: String = "",
    val duration: Long = 0,             // duration in seconds
    val washStated: String = "need wash", // "need wash", "washing", "washed"
    val washDate: String = "",
    val washedDate: String = "",
    val wearDate: String = "",
    val aiJunctionAnalysis: String = ""
)

val GarmentEntity.StandBreast: String
    get() = standBreast

val GarmentEntity.NotComfortable: String
    get() = try {
        org.json.JSONObject(customFields).optString("Not comfortable", "")
    } catch (_: Exception) {
        ""
    }

val GarmentEntity.antiSeeThrough: String
    get() = try {
        org.json.JSONObject(customFields).optString("anti see through", "")
    } catch (_: Exception) {
        ""
    }

val GarmentEntity.impression: String
    get() = descriptions2.ifBlank {
        try {
            org.json.JSONObject(customFields).optString("impression", "")
        } catch (_: Exception) {
            ""
        }
    }

val ReportEntity.dateCreated: String
    get() = created.ifBlank { wearDate }

val ReportEntity.TurnBody: String
    get() = try {
        org.json.JSONObject(customFields).optString("Turn body", "")
    } catch (_: Exception) {
        ""
    }

val ReportEntity.liftHand: String
    get() = try {
        org.json.JSONObject(customFields).optString("lift hand", "")
    } catch (_: Exception) {
        ""
    }

val JunctionEntity.dateCreated: String
    get() = wearDate.ifBlank { washDate }

