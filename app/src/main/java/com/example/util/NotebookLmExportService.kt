package com.example.util

import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import java.time.LocalDate

data class NotebookLmTopicDoc(
    val id: String,
    val title: String,
    val filename: String,
    val description: String,
    val content: String,
    val wordCount: Int,
    val charCount: Int
)

object NotebookLmExportService {

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /**
     * Generates pure factual wardrobe inventory and garment dossiers for NotebookLM.
     * Preliminary in-app AI opinions are excluded so NotebookLM performs unbiased deep reasoning.
     */
    fun generateWardrobeCatalogMarkdown(clothes: List<GarmentEntity>): String {
        val today = LocalDate.now().toString()
        val lines = mutableListOf<String>()

        lines.add("# Wardrobe Catalog & Garment Ergonomic Profiles")
        lines.add("Generated: $today | Source: Wardrobe Sensory & Care Management System")
        lines.add("Total Garments Cataloged: ${clothes.size}")
        lines.add("")
        lines.add("> **NotebookLM Usage Guide**: This document contains complete inventory profiles for all cataloged garments. Each garment includes purchase economics, material composition, strap/chest silhouette evaluations, thermal comfort across air conditioning and sleep settings, and accumulated wear statistics. NOTE: All preliminary AI-generated critiques are excluded so NotebookLM performs unbiased, deep synthetic analysis directly on the pure empirical facts.")
        lines.add("")
        lines.add("## 1. Wardrobe Summary Index")
        lines.add("| Garment Name | Category | Material | Thickness | Price | Total Wears | Wash Status | May Throw |")
        lines.add("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |")

        for (item in clothes) {
            val name = item.name.ifBlank { "Unnamed" }
            val type = item.clothesType.ifBlank { "Garment" }
            val mat = item.material.ifBlank { "(Unspecified)" }
            val thick = item.thickClothes.ifBlank { "(Unspecified)" }
            val priceStr = if (item.price > 0.0) "$${"%.2f".format(item.price)}" else "N/A"
            val totalWears = item.wearingFrequency + item.trackedAppWashes
            val washStatus = if (item.trackedAppWashes > 0) "Clean" else "Need Wash"
            val declutter = item.mayThrow.ifBlank { "No" }
            lines.add("| **$name** | $type | $mat | $thick | $priceStr | $totalWears | $washStatus | $declutter |")
        }

        lines.add("")
        lines.add("---")
        lines.add("")
        lines.add("## 2. Individual Garment Ergonomic & Sensory Dossiers")
        lines.add("")

        clothes.forEachIndexed { index, item ->
            val name = item.name.ifBlank { "Garment #${index + 1}" }
            val type = item.clothesType.ifBlank { "Garment" }
            val totalWears = item.wearingFrequency + item.trackedAppWashes
            val priceStr = if (item.price > 0.0) "$${"%.2f".format(item.price)}" else "Not recorded"
            val costPerWear = if (item.price > 0.0 && totalWears > 0) "$${"%.2f".format(item.price / totalWears)}" else "N/A"

            lines.add("### Garment ${index + 1}: $name")
            lines.add("- **Unique System ID**: `${item.id}`")
            lines.add("- **Category**: $type")
            lines.add("- **Price / Purchase Cost**: $priceStr")
            lines.add("- **Cost Per Wear**: $costPerWear (over $totalWears recorded wear cycles)")
            lines.add("- **Last Washed Date**: ${item.lastWashed.ifBlank { "No wash date recorded" }}")
            lines.add("- **Declutter Status**: ${item.mayThrow.ifBlank { "Keep / Undecided" }}")
            lines.add("")
            lines.add("#### Physical Attributes & Aesthetics")
            lines.add("- **Material Composition**: ${item.material.ifBlank { "Not specified" }}")
            lines.add("- **Thickness**: ${item.thickClothes.ifBlank { "Standard / Not specified" }}")
            lines.add("- **Aesthetic Rating**: ${item.beautiful.ifBlank { "Unrated" }}")
            if (item.beautifulReason.isNotBlank()) lines.add("  - *Aesthetic Notes*: ${item.beautifulReason}")
            if (item.descriptions.isNotBlank()) lines.add("- **Visual Notes**: ${item.descriptions}")
            if (item.descriptions2.isNotBlank()) lines.add("- **Silhouette / Fit**: ${item.descriptions2}")
            lines.add("")
            lines.add("#### Ergonomics & Body Fit Evaluation")
            lines.add("- **Shoulder Straps / Diao Dai**: ${item.diaoDai.ifBlank { "N/A" }}")
            lines.add("- **Sitting Chest Silhouette**: ${item.sitBreast.ifBlank { "N/A" }}")
            if (item.sitBreastGatherBra.isNotBlank()) lines.add("  - *With Push-up Bra*: ${item.sitBreastGatherBra}")
            if (item.sitBreastSideOutBra.isNotBlank()) lines.add("  - *With Side-Support Bra*: ${item.sitBreastSideOutBra}")
            lines.add("- **Standing Chest Silhouette**: ${item.standBreast.ifBlank { "N/A" }}")
            lines.add("- **Side Chest Enclosure**: ${item.sideBreastOk.ifBlank { "N/A" }}")
            lines.add("")
            lines.add("#### Environmental & Thermal Response")
            lines.add("- **Air Conditioned Setting (Upper Body)**: ${item.aircondHotFeeling.ifBlank { "Neutral" }}")
            if (item.aircondHotExplain.isNotBlank()) lines.add("  - *AC Thermal Notes*: ${item.aircondHotExplain}")
            lines.add("- **Air Conditioned Setting (Legs)**: ${item.aircondLegFeeling.ifBlank { "Neutral" }}")
            lines.add("- **Natural Heat Setting (Upper Body)**: ${item.noAircondHotFeeling.ifBlank { item.hotAtNoon.ifBlank { "Neutral" } }}")
            if (item.noAircondHotExplain.isNotBlank()) lines.add("  - *Natural Thermal Notes*: ${item.noAircondHotExplain}")
            lines.add("- **Fan Sleep Setting (Upper Body)**: ${item.fanSleepHotFeeling.ifBlank { "Neutral" }}")
            if (item.fanSleepHotExplain.isNotBlank()) lines.add("  - *Fan Sleep Notes*: ${item.fanSleepHotExplain}")
            if (item.lastComment.isNotBlank()) lines.add("- **Action Log / Note**: ${item.lastComment}")
            lines.add("")
            lines.add("---")
            lines.add("")
        }

        return lines.joinToString("\n")
    }

    /**
     * Generates chronological wear sessions detailing real-world outfit performance.
     */
    fun generateWearingReportsMarkdown(
        reports: List<ReportEntity>,
        clothes: List<GarmentEntity>,
        junctions: List<JunctionEntity>
    ): String {
        val today = LocalDate.now().toString()
        val clothesMap = clothes.associateBy { it.id }
        val lines = mutableListOf<String>()

        lines.add("# Wearing Sensory Reports & Wear Journal")
        lines.add("Generated: $today | Source: Wardrobe Sensory & Care Management System")
        lines.add("Total Wear Sessions Recorded: ${reports.size}")
        lines.add("")
        lines.add("> **NotebookLM Usage Guide**: This document contains chronological wear sessions detailing real-world outfit performance. Each entry records the worn outfit, bra pairing, session duration, shoulder strap pain, breast silhouette comfort, sweating/dampness, nighttime sleep comfort under fans, and wash confirmations. NOTE: All in-app AI diagnostic critiques are excluded so NotebookLM operates solely on pure, ground-truth empirical wear telemetry.")
        lines.add("")
        lines.add("## 1. Wear Sessions Chronological Index")
        lines.add("| Date | Report Title | Sweat | Shoulder Feel | Mid Breast | Leg Feel |")
        lines.add("| :--- | :--- | :--- | :--- | :--- | :--- |")

        val sortedReports = reports.sortedByDescending { it.wearDate }
        for (r in sortedReports) {
            val date = r.wearDate.ifBlank { "Unspecified date" }
            val title = r.name.ifBlank { "Wear Report" }
            val sweat = r.sweat.ifBlank { "Normal" }
            val shoulder = r.shoulderFeel.ifBlank { "Normal" }
            val breast = r.midBreast.ifBlank { "Normal" }
            val leg = r.legFeel.ifBlank { "Normal" }
            lines.add("| $date | **$title** | $sweat | $shoulder | $breast | $leg |")
        }

        lines.add("")
        lines.add("---")
        lines.add("")
        lines.add("## 2. Detailed Session Wear Dossiers")
        lines.add("")

        sortedReports.forEachIndexed { index, r ->
            lines.add("### Session ${index + 1}: ${r.name.ifBlank { "Session #${r.id}" }}")
            lines.add("- **Session ID**: `${r.id}`")
            lines.add("- **Wear Date**: ${r.wearDate.ifBlank { "Unrecorded" }}")
            lines.add("- **Shoulder Feel & Strap Tension**: ${r.shoulderFeel.ifBlank { "Comfortable / No strain" }}")
            lines.add("- **Mid Breast / Bust Comfort**: ${r.midBreast.ifBlank { "Normal" }}")
            if (r.nightMidBreast.isNotBlank()) lines.add("- **Night Mid Breast**: ${r.nightMidBreast}")
            lines.add("- **Sweat & Thermal Regulation**: ${r.sweat.ifBlank { "Normal" }}")
            if (r.nightSweat.isNotBlank()) lines.add("- **Night Sweat / Dampness**: ${r.nightSweat}")
            lines.add("- **Leg & Lower Body Feel**: ${r.legFeel.ifBlank { "Comfortable" }}")
            if (r.nightLegFeel.isNotBlank()) lines.add("- **Night Leg Feel**: ${r.nightLegFeel}")
            if (r.reportDetail.isNotBlank()) {
                lines.add("- **Detailed Diary Notes**:")
                lines.add("  > ${r.reportDetail}")
            }

            // Linked garments
            val linkedJunctions = junctions.filter { it.wearingReportId == r.id }
            if (linkedJunctions.isNotEmpty()) {
                lines.add("- **Garments Worn in this Session**:")
                for (j in linkedJunctions) {
                    val g = clothesMap[j.clothesId]
                    val gName = g?.name ?: j.clothesTitle.ifBlank { "Garment #${j.clothesId}" }
                    val gType = g?.clothesType ?: j.clothesType
                    val durStr = if (j.duration > 0) formatDuration(j.duration) else "Standard wear"
                    val washSt = if (j.washStated.isNotBlank()) j.washStated else "need wash"
                    lines.add("  - **$gName** ($gType) | Duration: $durStr | Wash state: $washSt")
                }
            }
            lines.add("")
            lines.add("---")
            lines.add("")
        }

        return lines.joinToString("\n")
    }

    /**
     * Generates laundry maintenance records, wash frequency counters, and hygiene backlog.
     */
    fun generateLaundryCareMarkdown(
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity>,
        junctions: List<JunctionEntity>
    ): String {
        val today = LocalDate.now().toString()
        val lines = mutableListOf<String>()

        lines.add("# Laundry Care Cycles & Wash History")
        lines.add("Generated: $today | Source: Wardrobe Sensory & Care Management System")
        lines.add("")
        lines.add("> **NotebookLM Usage Guide**: This document details garment hygiene cycles, accumulated unwashed duration, wash methods, and fabric care requirements. Use this document in NotebookLM to query laundry workloads, fabric wear rates, wash backlogs, and seasonal washing routines.")
        lines.add("")
        lines.add("## 1. Wash Care Priority & Status")
        lines.add("| Garment | Category | Total Washes | Last Washed Date | Current Status |")
        lines.add("| :--- | :--- | :--- | :--- | :--- |")

        for (g in clothes) {
            val totalWashes = g.wearingFrequency + g.trackedAppWashes
            val status = if (g.trackedAppWashes > 0) "Clean" else "Needs Attention"
            lines.add("| **${g.name.ifBlank { "Garment" }}** | ${g.clothesType} | $totalWashes washes | ${g.lastWashed.ifBlank { "Never" }} | $status |")
        }

        lines.add("")
        lines.add("## 2. Recent Wash Logs from Wear Sessions")
        val washedJunctions = junctions.filter { it.washDate.isNotBlank() || it.washStated == "washed" }
        if (washedJunctions.isEmpty()) {
            lines.add("*No junction wash dates recorded yet.*")
        } else {
            lines.add("| Garment | Wash Date | Wear Session |")
            lines.add("| :--- | :--- | :--- |")
            for (j in washedJunctions) {
                val gName = j.clothesTitle.ifBlank { "Item #${j.clothesId}" }
                val wDate = if (j.washDate.isNotBlank()) j.washDate else j.wearDate
                lines.add("| **$gName** | $wDate | ${j.wearingReportTitle.ifBlank { "Session #${j.wearingReportId}" }} |")
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * Generates a glossary of sensory, ergonomic, and aesthetic terminology used across the app.
     */
    fun generateGlossaryMarkdown(): String {
        return """# Wardrobe Sensory & Care Glossary

## 1. Ergonomic & Silhouette Fields
- **diao dai (吊带/肩带)**: Shoulder strap structure and tension evaluation. Distinguishes whether garment has shoulder straps, wide bands, or strapless design, and whether it exerts shoulder strain.
- **sit breast ok (久坐胸型/腹部舒适度)**: Evaluates comfort when sitting for long periods (work/dining). Assesses whether underwire or fabric pinches the lower ribcage or stomach.
- **stand breast (站立胸型/中央支撑)**: Aesthetic silhouette and support when standing upright.
- **side breast ok (副乳包容/侧收能力)**: Evaluates whether side fabric covers or exposes axillary breast tissue with gathered or side-support bras.

## 2. Environmental & Climate Performance
- **when with aircond: hot feeling**: Thermal perception in cold air-conditioned rooms (24-26°C).
- **when without aircond: hot feeling**: Thermal perception in warm natural environments, high midday summer heat.
- **when sleep under fan: hot feeling**: Nighttime sleep comfort and breathability under fan ventilation.
- **sweat (排汗与透气)**: Level of perspiration dissipation during daily activities.

## 3. Care & Economic Metrics
- **Cost Per Wear (CPW)**: Purchase price divided by total verified wear sessions.
- **trackedAppWashes**: Verified laundry cycles completed.
- **may throw (断舍离标记)**: Audit marker indicating whether the garment is under review for donation, tailoring, or disposal.
"""
    }

    /**
     * Retrieves all 4 documents ready for export or preview.
     */
    fun getAllTopicDocs(
        clothes: List<GarmentEntity>,
        reports: List<ReportEntity>,
        junctions: List<JunctionEntity>
    ): List<NotebookLmTopicDoc> {
        val catalog = generateWardrobeCatalogMarkdown(clothes)
        val reportsDoc = generateWearingReportsMarkdown(reports, clothes, junctions)
        val laundry = generateLaundryCareMarkdown(clothes, reports, junctions)
        val glossary = generateGlossaryMarkdown()

        fun countWords(s: String) = s.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        return listOf(
            NotebookLmTopicDoc(
                id = "catalog",
                title = "1. 衣橱全量单品与工学档案 (Wardrobe Catalog)",
                filename = "wardrobe_catalog_and_profiles.md",
                description = "涵盖全部已录入衣物的物理材质、价格CPW、肩带与胸型工学、空调及睡眠温感数据。完全剥离AI预设观点，适合 NotebookLM 构建深层衣橱画像。",
                content = catalog,
                wordCount = countWords(catalog),
                charCount = catalog.length
            ),
            NotebookLmTopicDoc(
                id = "reports",
                title = "2. 穿搭日记与体感时序记录 (Wear Sensory Reports)",
                filename = "wearing_reports_and_logs.md",
                description = "按时间顺序汇总每一场穿戴会话，记录当日排汗、肩部勒感、夜间风扇体感与搭配单品，提供纯客观真实穿着时序遥测。",
                content = reportsDoc,
                wordCount = countWords(reportsDoc),
                charCount = reportsDoc.length
            ),
            NotebookLmTopicDoc(
                id = "laundry",
                title = "3. 洗涤养护周期与洁净状态 (Laundry Care Cycles)",
                filename = "laundry_and_care_cycles.md",
                description = "统计各单品累计洗涤频次、未洗累积时长及洗涤确认记录，专供 NotebookLM 分析洗衣负荷与衣物耐洗损耗。",
                content = laundry,
                wordCount = countWords(laundry),
                charCount = laundry.length
            ),
            NotebookLmTopicDoc(
                id = "glossary",
                title = "4. 体感工学与字段双语词典 (Sensory & Care Glossary)",
                filename = "sensory_and_care_glossary.md",
                description = "中英文专业词汇与工学字段定义对照表，确保 NotebookLM 对专有名词有准确理解。",
                content = glossary,
                wordCount = countWords(glossary),
                charCount = glossary.length
            )
        )
    }
}
