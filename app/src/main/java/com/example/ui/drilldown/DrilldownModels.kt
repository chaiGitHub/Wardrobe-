package com.example.ui.drilldown

import com.example.data.model.GarmentEntity

/**
 * Report Field Definition matching webapp fields
 */
data class ReportFieldDef(
    val key: String,
    val label: String,
    val category: String, // "classification", "comfort", "fit", "care"
    val type: String = "string" // "string", "number"
)

/**
 * Hierarchical Drill-down Preset
 */
data class DrillPreset(
    val id: String,
    val name: String,
    val description: String = "",
    val groupTiers: List<String>,
    val sortField: String = "wearing frequency(how many times legacy washed)",
    val sortOrder: String = "desc", // "asc" or "desc"
    val visibleFields: List<String> = listOf(
        "Clothes Type",
        "material",
        "Hot at noon",
        "beautiful",
        "wearing frequency(how many times legacy washed)",
        "wash stated"
    ),
    val isCustom: Boolean = false
)

/**
 * Summary for a subgroup tier
 */
data class GroupSummary(
    val field: String,
    val value: String,
    val count: Int,
    val cleanCount: Int,
    val needWashCount: Int,
    val sampleGarments: List<GarmentEntity>
)

/**
 * Breadcrumb step in the drill-down navigation hierarchy
 */
data class DrilldownBreadcrumb(
    val tierIndex: Int,
    val field: String,
    val value: String
)

/**
 * All 20 available fields matching the original webapp exactly
 */
val ALL_AVAILABLE_FIELDS: List<ReportFieldDef> = listOf(
    ReportFieldDef("Clothes Type", "Category / Type", "classification"),
    ReportFieldDef("material", "Material", "classification"),
    ReportFieldDef("thick clothes", "Thickness", "comfort"),
    ReportFieldDef("Hot at noon", "Noon Heat Feel", "comfort"),
    ReportFieldDef("beautiful", "Aesthetic Score", "fit"),
    ReportFieldDef("diao dai", "Strap Design", "fit"),
    ReportFieldDef("side breast ok", "Side Fit", "fit"),
    ReportFieldDef("sit breast ok", "Seated Bust Fit", "fit"),
    ReportFieldDef("center chest", "Center Chest Cut", "fit"),
    ReportFieldDef("wet", "Moisture / Dryness", "comfort"),
    ReportFieldDef("long dress", "Length Cut", "classification"),
    ReportFieldDef("may throw", "Retention / Discard", "classification"),
    ReportFieldDef("wash stated", "Laundry State", "care"),
    ReportFieldDef("wearing frequency(how many times legacy washed)", "Wear Frequency", "care", "number"),
    ReportFieldDef("Total Washed Count", "Total Washed Count", "care", "number"),
    ReportFieldDef("Tracked App Washes", "Tracked App Washes", "care", "number"),
    ReportFieldDef("Last Washed", "Last Washed", "care"),
    ReportFieldDef("price", "Price / Cost", "care", "number"),
    ReportFieldDef("Name", "Garment Name", "classification"),
    ReportFieldDef("Descriptions", "Description / Notes", "classification")
)

/**
 * Default standard presets matching the webapp
 */
val DEFAULT_DRILL_PRESETS: List<DrillPreset> = listOf(
    DrillPreset(
        id = "preset-thickness-climate",
        name = "Thickness & Climate",
        description = "Hierarchical drill by clothing category, noon thermal feel, and fabric thickness.",
        groupTiers = listOf("Clothes Type", "Hot at noon", "thick clothes"),
        sortField = "wearing frequency(how many times legacy washed)",
        sortOrder = "desc",
        visibleFields = listOf(
            "Clothes Type",
            "material",
            "thick clothes",
            "Hot at noon",
            "beautiful",
            "wearing frequency(how many times legacy washed)",
            "wash stated"
        )
    ),
    DrillPreset(
        id = "preset-aesthetic-fit",
        name = "Aesthetic & Fit Ratings",
        description = "Drill down by category, aesthetic score, and strap/chest silhouette.",
        groupTiers = listOf("Clothes Type", "beautiful", "diao dai"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf("Clothes Type", "beautiful", "diao dai", "side breast ok", "material", "price")
    ),
    DrillPreset(
        id = "preset-wash-care",
        name = "Wash Care & Laundry Priority",
        description = "Exclusive drill by wash backlog state, clothing category, and fabric material.",
        groupTiers = listOf("wash stated", "Clothes Type", "material"),
        sortField = "wearing frequency(how many times legacy washed)",
        sortOrder = "desc",
        visibleFields = listOf("wash stated", "Clothes Type", "material", "Tracked App Washes", "Total Washed Count", "Last Washed")
    ),
    DrillPreset(
        id = "preset-sensory-heat",
        name = "Sensory Heat & Moisture",
        description = "Drill by midday heat sensitivity, moisture performance, and thickness.",
        groupTiers = listOf("Hot at noon", "wet", "thick clothes"),
        sortField = "wearing frequency(how many times legacy washed)",
        sortOrder = "desc",
        visibleFields = listOf("Clothes Type", "Hot at noon", "wet", "thick clothes", "material", "beautiful")
    ),
    DrillPreset(
        id = "preset-declutter-audit",
        name = "Declutter & Discard Audit",
        description = "Audit garments flagged for possible decluttering or disposal by category.",
        groupTiers = listOf("may throw", "Clothes Type", "beautiful"),
        sortField = "wearing frequency(how many times legacy washed)",
        sortOrder = "asc",
        visibleFields = listOf("may throw", "Clothes Type", "material", "beautiful", "price", "wearing frequency(how many times legacy washed)")
    )
)
