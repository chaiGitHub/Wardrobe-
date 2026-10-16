package com.example.ui.drilldown

import com.example.data.model.GarmentEntity

/**
 * Report Field Definition matching webapp fields
 */
data class ReportFieldDef(
    val key: String,
    val label: String,
    val category: String, // "classification", "comfort", "fit", "care"
    val type: String = "string", // "string", "number"
    val description: String = ""
)

/**
 * Hierarchical Drill-down Preset
 */
data class DrillPreset(
    val id: String,
    val name: String,
    val description: String = "",
    val groupTiers: List<String>,
    val sortField: String = "Name",
    val sortOrder: String = "asc", // "asc" or "desc"
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
 * All available fields including multi-scenario climate, breast ergonomics, materials, care, and audit
 */
val ALL_AVAILABLE_FIELDS: List<ReportFieldDef> = listOf(
    // Climate & Thermal Ergonomics
    ReportFieldDef(
        key = "when with aircond: hot feeling",
        label = "when with aircond: hot feeling",
        category = "comfort",
        description = "Thermal sensation in AC room (not cold, not so cold, hot, undefined)"
    ),
    ReportFieldDef(
        key = "when with aircond: leg feeling",
        label = "when with aircond: leg feeling",
        category = "comfort",
        description = "Leg coldness/thermal feel in AC room (not cold, not so cold, hot, undefined)"
    ),
    ReportFieldDef(
        key = "when without aircond: hot feeling",
        label = "when without aircond: hot feeling",
        category = "comfort",
        description = "Upper body thermal feeling without AC (hot, not hot, comfortable, undefined)"
    ),
    ReportFieldDef(
        key = "when without aircond: leg feeling",
        label = "when without aircond: leg feeling",
        category = "comfort",
        description = "Leg feeling at normal room temperature without AC (not cold, hot, undefined)"
    ),
    ReportFieldDef(
        key = "when sleep under fan: hot feeling",
        label = "when sleep under fan: hot feeling",
        category = "comfort",
        description = "Upper body thermal comfort while sleeping under fan (cool, hot, undefined)"
    ),
    ReportFieldDef(
        key = "when sleep under fan: leg feeling",
        label = "when sleep under fan: leg feeling",
        category = "comfort",
        description = "Leg temperature and airflow feeling when sleeping under fan (not cold, hot, undefined)"
    ),
    ReportFieldDef(
        key = "Hot at noon",
        label = "Hot at noon",
        category = "comfort",
        description = "Midday peak heat sensation (Hot, Middle, Not hot, undefined)"
    ),

    // Breast & Bra Ergonomics
    ReportFieldDef(
        key = "sit breast ok: With 聚拢bra",
        label = "sit breast ok: With 聚拢bra",
        category = "fit",
        description = "Sitting breast silhouette and cleavage stability with gathering/push-up bra (ok, not ok, undefined)"
    ),
    ReportFieldDef(
        key = "sit breast ok: with side out bra",
        label = "sit breast ok: with side out bra",
        category = "fit",
        description = "Sitting breast shape with side-cut/side-out bra (ok, ugly, undefined)"
    ),
    ReportFieldDef(
        key = "side breast ok: with 聚拢 bra",
        label = "side breast ok: with 聚拢 bra",
        category = "fit",
        description = "Side profile breast shape & firmness with gathering bra (ok, side out, undefined)"
    ),
    ReportFieldDef(
        key = "side breast ok: with side out bra",
        label = "side breast ok: with side out bra",
        category = "fit",
        description = "Side profile breast silhouette with side-out bra (ok, side breast out, undefined)"
    ),
    ReportFieldDef(
        key = "sit breast ok",
        label = "sit breast ok",
        category = "fit",
        description = "General sitting breast posture silhouette (sit breast shape ok, stand breast ugly, undefined)"
    ),
    ReportFieldDef(
        key = "side breast ok",
        label = "side breast ok",
        category = "fit",
        description = "General side profile breast fullness (side breast shape ok, side breast out, undefined)"
    ),
    ReportFieldDef(
        key = "center chest",
        label = "center chest",
        category = "fit",
        description = "Center sternum comfort & gore pressure (center chest, no center chest, undefined)"
    ),
    ReportFieldDef(
        key = "diao dai",
        label = "diao dai (Strap)",
        category = "fit",
        description = "Shoulder strap cut & pain/slip feeling (diao dai, not diao dai, wide diao dai, undefined)"
    ),

    // Clothes Classification & Properties
    ReportFieldDef(
        key = "Clothes Type",
        label = "Clothes Type",
        category = "classification",
        description = "Garment category (Dress, bra, Tshirt, Pant, etc.)"
    ),
    ReportFieldDef(
        key = "material",
        label = "material",
        category = "classification",
        description = "Fabric material composition (chiffon, cotton, lace, silk, etc.)"
    ),
    ReportFieldDef(
        key = "long dress",
        label = "long dress (Length)",
        category = "classification",
        description = "Dress length style (short dress, middle dress, long dress, undefined)"
    ),
    ReportFieldDef(
        key = "thick clothes",
        label = "thick clothes (Thickness)",
        category = "comfort",
        description = "Material thickness level (01 not thick, 02 middle thick, 03 thick, undefined)"
    ),
    ReportFieldDef(
        key = "wet",
        label = "wet (Moisture)",
        category = "comfort",
        description = "Sweat wetness and drying condition (dry, little wet, wet, undefined)"
    ),
    ReportFieldDef(
        key = "wet reason",
        label = "wet reason",
        category = "comfort",
        description = "Cause of dampness, airflow retention, or machine drying behavior"
    ),
    ReportFieldDef(
        key = "beautiful",
        label = "beautiful",
        category = "fit",
        description = "Aesthetic evaluation & silhouette flattering score (Beautiful, Middle beautiful, not so beautiful, undefined)"
    ),
    ReportFieldDef(
        key = "beatiful reason",
        label = "beatiful reason",
        category = "fit",
        description = "Specific cut, color, or design factors making the outfit flattering"
    ),
    ReportFieldDef(
        key = "may throw",
        label = "may throw (Declutter)",
        category = "classification",
        description = "Wardrobe audit flag (may throw, no, may throw due to waist, undefined)"
    ),
    ReportFieldDef(
        key = "Name",
        label = "Name",
        category = "classification",
        description = "Garment primary name or title in wardrobe"
    ),
    ReportFieldDef(
        key = "Descriptions",
        label = "Descriptions",
        category = "classification",
        description = "Primary wear & fit observation notes"
    ),
    ReportFieldDef(
        key = "Descriptions 2",
        label = "Descriptions 2",
        category = "classification",
        description = "Secondary ergonomic & maintenance notes"
    ),
    ReportFieldDef(
        key = "last comment or action",
        label = "last comment or action",
        category = "classification",
        description = "Latest user recommendation or wearing verdict"
    ),

    // Washing & Care
    ReportFieldDef(
        key = "wash stated",
        label = "wash stated",
        category = "care",
        description = "Current laundry state (Clean vs Need Wash)"
    ),
    ReportFieldDef(
        key = "Total Washed Count",
        label = "Total Washed Count",
        category = "care",
        type = "number",
        description = "Total cumulative wash cycles (Legacy count + App tracked washes)"
    ),
    ReportFieldDef(
        key = "Tracked App Washes",
        label = "Tracked App Washes",
        category = "care",
        type = "number",
        description = "Count of washes recorded within the app"
    ),
    ReportFieldDef(
        key = "wearing frequency(how many times legacy washed)",
        label = "wearing frequency(legacy)",
        category = "care",
        type = "number",
        description = "Historical wash/wear frequency recorded prior to app"
    ),
    ReportFieldDef(
        key = "price",
        label = "Price / Cost",
        category = "care",
        type = "number",
        description = "Original purchase price of garment"
    ),
    ReportFieldDef(
        key = "Last Washed",
        label = "Last Washed",
        category = "care",
        description = "Date and time when garment was last washed"
    ),
    ReportFieldDef(
        key = "created",
        label = "Created Date",
        category = "classification",
        description = "Initial entry timestamp in wardrobe system"
    )
)

/**
 * Default standard presets matching the 13 built-in scenarios from web repository
 */
val DEFAULT_DRILL_PRESETS: List<DrillPreset> = listOf(
    DrillPreset(
        id = "preset-aircond-hot",
        name = "Aircond Suitable: Upper Body (not cold / hot)",
        description = "Tier 1: when with aircond: hot feeling (not cold, not so cold, hot, undefined). Tier 2: Clothes Type (dress/bra).",
        groupTiers = listOf("when with aircond: hot feeling", "Clothes Type"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "when with aircond: hot feeling",
            "when with aircond: leg feeling",
            "material",
            "thick clothes"
        )
    ),
    DrillPreset(
        id = "preset-aircond-leg",
        name = "Aircond Suitable: Leg Feeling (not cold leg)",
        description = "Tier 1: when with aircond: leg feeling (not cold, not so cold, hot, undefined). Tier 2: Clothes Type.",
        groupTiers = listOf("when with aircond: leg feeling", "Clothes Type"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "when with aircond: leg feeling",
            "when with aircond: hot feeling",
            "material",
            "long dress"
        )
    ),
    DrillPreset(
        id = "preset-without-aircond-hot",
        name = "Normal / No Aircond: Upper Body (hot feeling)",
        description = "Tier 1: when without aircond: hot feeling (not cold, hot, undefined). Tier 2: Clothes Type (dress/bra).",
        groupTiers = listOf("when without aircond: hot feeling", "Clothes Type"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "when without aircond: hot feeling",
            "when without aircond: leg feeling",
            "material",
            "thick clothes"
        )
    ),
    DrillPreset(
        id = "preset-without-aircond-leg",
        name = "Normal / No Aircond: Leg Feeling (not cold leg)",
        description = "Tier 1: when without aircond: leg feeling (not cold, not so cold, hot, undefined). Tier 2: Clothes Type.",
        groupTiers = listOf("when without aircond: leg feeling", "Clothes Type"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "when without aircond: leg feeling",
            "when without aircond: hot feeling",
            "material",
            "thick clothes"
        )
    ),
    DrillPreset(
        id = "preset-sleep-fan-hot",
        name = "Sleep Under Fan: Upper Body (hot feeling)",
        description = "Tier 1: when sleep under fan: hot feeling (not cold, hot, undefined). Tier 2: Clothes Type.",
        groupTiers = listOf("when sleep under fan: hot feeling", "Clothes Type"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "when sleep under fan: hot feeling",
            "when sleep under fan: leg feeling",
            "material",
            "long dress"
        )
    ),
    DrillPreset(
        id = "preset-sleep-fan-leg",
        name = "Sleep Under Fan: Leg Feeling (not cold leg)",
        description = "Tier 1: when sleep under fan: leg feeling (not cold, not so cold, hot, undefined). Tier 2: Clothes Type.",
        groupTiers = listOf("when sleep under fan: leg feeling", "Clothes Type"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "when sleep under fan: leg feeling",
            "when sleep under fan: hot feeling",
            "material",
            "long dress"
        )
    ),
    DrillPreset(
        id = "preset-wet-performance",
        name = "Clothes Wet / Dry Performance",
        description = "Tier 1: Clothes Type (dress/bra). Tier 2: wet (dry, little wet, wet, undefined).",
        groupTiers = listOf("Clothes Type", "wet"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "wet",
            "wet reason",
            "material",
            "Hot at noon"
        )
    ),
    DrillPreset(
        id = "preset-breast-gather-bra",
        name = "Breast Fit: With 聚拢bra",
        description = "Tier 1: Clothes Type (dress/bra). Tier 2: sit breast ok: With 聚拢bra.",
        groupTiers = listOf("Clothes Type", "sit breast ok: With 聚拢bra"),
        sortField = "Name",
        sortOrder = "asc",
        visibleFields = listOf(
            "Clothes Type",
            "sit breast ok: With 聚拢bra",
            "side breast ok: with 聚拢 bra",
            "beautiful",
            "diao dai"
        )
    ),
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
