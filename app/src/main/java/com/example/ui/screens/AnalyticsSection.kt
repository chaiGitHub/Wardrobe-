package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.ui.drilldown.DrilldownUtils
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
import java.text.NumberFormat
import java.util.Locale

private data class GarmentCpwEntry(
    val garment: GarmentEntity,
    val wears: Int,
    val price: Double,
    val cpw: Double
)

private data class ErgonomicMetricBar(
    val label: String,
    val positiveCount: Int,
    val negativeCount: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WardrobeAnalyticsSection(
    clothes: List<GarmentEntity>,
    junctions: List<JunctionEntity>,
    onSelectGarment: ((GarmentEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

    // Compute wears and CPW for each garment matching web app Analytics.tsx
    val analyticsData = remember(clothes, junctions) {
        var totalValue = 0.0
        var pricedCount = 0
        var pricedWearsSum = 0
        var totalHistoricalWears = 0
        val cpwList = mutableListOf<GarmentCpwEntry>()

        clothes.forEach { g ->
            val stats = DrilldownUtils.calculateGarmentStats(g.id, junctions)
            val totalWashed = g.wearingFrequency + g.trackedAppWashes
            val effectiveWears = if (totalWashed > 0) totalWashed else stats.totalWears
            totalHistoricalWears += effectiveWears

            if (g.price > 0) {
                totalValue += g.price
                pricedCount++
                val wearsForCpw = maxOf(1, effectiveWears)
                pricedWearsSum += effectiveWears
                cpwList.add(
                    GarmentCpwEntry(
                        garment = g,
                        wears = effectiveWears,
                        price = g.price,
                        cpw = g.price / wearsForCpw.toDouble()
                    )
                )
            }
        }

        val avgCpw = if (pricedWearsSum > 0) totalValue / pricedWearsSum else 0.0
        val bestValueList = cpwList.filter { it.wears > 0 }.sortedBy { it.cpw }
        val highestCpwList = cpwList.sortedByDescending { it.cpw }

        Triple(
            mapOf(
                "totalValue" to totalValue,
                "pricedCount" to pricedCount.toDouble(),
                "pricedWearsSum" to pricedWearsSum.toDouble(),
                "avgCpw" to avgCpw,
                "totalHistoricalWears" to totalHistoricalWears.toDouble()
            ),
            bestValueList,
            highestCpwList
        )
    }

    val summaryMap = analyticsData.first
    val bestValueLeaderboard = analyticsData.second
    val underutilizedList = analyticsData.third
    val champion = bestValueLeaderboard.firstOrNull()

    // Category breakdown (matching IMG_9740: Dress, Tops, Uncategorized, Outerwear)
    val categoryCounts = remember(clothes) {
        clothes.groupBy { it.clothesType.trim().ifBlank { "Uncategorized" } }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
    }

    // Comfort & Ergonomics Breakdown (matching IMG_9740 & IMG_9741)
    val ergonomicBars = remember(clothes) {
        val middayCoolPos = clothes.count { it.hotAtNoon.contains("not hot", ignoreCase = true) || it.hotAtNoon.contains("cool", ignoreCase = true) }
        val middayCoolNeg = clothes.count { it.hotAtNoon.isNotBlank() } - middayCoolPos

        val braFreePos = clothes.count { it.diaoDai.contains("not diao dai", ignoreCase = true) }
        val braFreeNeg = clothes.count { it.diaoDai.isNotBlank() } - braFreePos

        val bustFitPos = clothes.count { it.sitBreast.contains("ok", ignoreCase = true) || it.sitBreast.contains("comfort", ignoreCase = true) }
        val bustFitNeg = clothes.count { it.sitBreast.isNotBlank() } - bustFitPos

        val beautyPos = clothes.count { it.beautiful.contains("beautiful", ignoreCase = true) && !it.beautiful.contains("not", ignoreCase = true) }
        val beautyNeg = clothes.count { it.beautiful.isNotBlank() } - beautyPos

        val drynessPos = clothes.count { it.wet.contains("dry", ignoreCase = true) }
        val drynessNeg = clothes.count { it.wet.isNotBlank() } - drynessPos

        listOf(
            ErgonomicMetricBar("Midday Coolness", middayCoolPos, maxOf(0, middayCoolNeg)),
            ErgonomicMetricBar("Bra-Free Support", braFreePos, maxOf(0, braFreeNeg)),
            ErgonomicMetricBar("Bust Fit Comfort", bustFitPos, maxOf(0, bustFitNeg)),
            ErgonomicMetricBar("Beauty Rating", beautyPos, maxOf(0, beautyNeg)),
            ErgonomicMetricBar("Dryness", drynessPos, maxOf(0, drynessNeg))
        )
    }

    // Fabric / Material Composition (matching IMG_9741 Donut Chart)
    val materialBreakdown = remember(clothes) {
        clothes.groupBy { it.material.trim().ifBlank { "other" } }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 4 KPI Summary Cards (Matching IMG_9738.png)
        KpiMetricCard(
            icon = Icons.Default.AttachMoney,
            iconBg = Color(0xFFF3E8FF),
            iconTint = BrandPurple,
            title = "TOTAL WARDROBE VALUE",
            value = "$${numberFormat.format(summaryMap["totalValue"]?.toLong() ?: 0L)}",
            subtitle = "Across ${summaryMap["pricedCount"]?.toInt() ?: 0} priced garments (${clothes.size} total)"
        )

        KpiMetricCard(
            icon = Icons.Default.TrendingDown,
            iconBg = Color(0xFFECFDF5),
            iconTint = BrandEmerald,
            title = "AVERAGE COST / WEAR",
            value = String.format(Locale.US, "$%.2f", summaryMap["avgCpw"] ?: 0.0),
            subtitle = "Total investment divided by ${numberFormat.format(summaryMap["pricedWearsSum"]?.toLong() ?: 0L)} wears"
        )

        KpiMetricCard(
            icon = Icons.Default.EmojiEvents,
            iconBg = Color(0xFFFFFBEB),
            iconTint = Color(0xFFD97706),
            title = "BEST VALUE CHAMPION",
            value = champion?.garment?.name ?: "N/A",
            subtitle = if (champion != null) {
                String.format(Locale.US, "$%.2f per wear (%d wears, $%.0f)", champion.cpw, champion.wears, champion.price)
            } else "No priced wears yet"
        )

        KpiMetricCard(
            icon = Icons.Default.Checkroom,
            iconBg = Color(0xFFEFF6FF),
            iconTint = Color(0xFF2563EB),
            title = "TOTAL HISTORICAL WEARS",
            value = numberFormat.format(summaryMap["totalHistoricalWears"]?.toLong() ?: 0L),
            subtitle = "Combined Notion history + logged sessions"
        )

        // 2. Cost-Per-Wear Leaderboard (Best Value) (Matching IMG_9738 & IMG_9739)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Cost-Per-Wear Leaderboard (Best Value)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                val topBest = bestValueLeaderboard.take(8)
                val maxBestWears = (topBest.maxOfOrNull { it.wears } ?: 1).coerceAtLeast(1)

                topBest.forEachIndexed { index, entry ->
                    LeaderboardItemRow(
                        rank = index + 1,
                        name = entry.garment.name,
                        subtitle = "${entry.wears} wears · $${entry.price.toInt()}",
                        badgeText = String.format(Locale.US, "$%.2f/wear", entry.cpw),
                        progress = (entry.wears.toFloat() / maxBestWears.toFloat()).coerceIn(0.12f, 1f),
                        isBestValue = true,
                        onClick = { onSelectGarment?.invoke(entry.garment) }
                    )
                }
            }
        }

        // 3. Underutilized Investments (Highest Cost / Wear) (Matching IMG_9739 & IMG_9740)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Underutilized Investments (Highest Cost / Wear)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                val topUnder = underutilizedList.take(8)
                val maxUnderCpw = (topUnder.maxOfOrNull { it.cpw } ?: 1.0).coerceAtLeast(1.0)

                topUnder.forEachIndexed { index, entry ->
                    LeaderboardItemRow(
                        rank = index + 1,
                        name = entry.garment.name,
                        subtitle = "${entry.wears} wears · $${entry.price.toInt()}",
                        badgeText = String.format(Locale.US, "$%.2f/wear", entry.cpw),
                        progress = (entry.cpw / maxUnderCpw).toFloat().coerceIn(0.15f, 1f),
                        isBestValue = false,
                        onClick = { onSelectGarment?.invoke(entry.garment) }
                    )
                }
            }
        }

        // 4. Garments by Category Bar Chart (Matching IMG_9740.png)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Garments by Category",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                val maxCatCount = (categoryCounts.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    categoryCounts.forEach { (category, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                color = Slate600,
                                modifier = Modifier.width(86.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF8FAFC))
                            ) {
                                val fraction = (count.toFloat() / maxCatCount.toFloat()).coerceIn(0.03f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                                        .background(BrandPurple)
                                )
                            }

                            Text(
                                text = count.toString(),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                modifier = Modifier.width(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Comfort & Ergonomics Breakdown Stacked Chart (Matching IMG_9740 & IMG_9741)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Comfort & Ergonomics Breakdown",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                val maxTotal = (ergonomicBars.maxOfOrNull { it.positiveCount + it.negativeCount } ?: 1).coerceAtLeast(1)

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ergonomicBars.forEach { bar ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = bar.label,
                                fontSize = 11.sp,
                                color = Slate600,
                                modifier = Modifier.width(96.dp),
                                lineHeight = 14.sp
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF8FAFC))
                            ) {
                                val posFrac = (bar.positiveCount.toFloat() / maxTotal.toFloat()).coerceAtLeast(0f)
                                val negFrac = (bar.negativeCount.toFloat() / maxTotal.toFloat()).coerceAtLeast(0f)
                                val remFrac = (1f - posFrac - negFrac).coerceAtLeast(0f)

                                if (posFrac > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(posFrac)
                                            .background(Color(0xFF10B981))
                                    )
                                }
                                if (negFrac > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(negFrac)
                                            .background(Color(0xFFEF4444))
                                    )
                                }
                                if (remFrac > 0.01f) {
                                    Spacer(modifier = Modifier.weight(remFrac))
                                }
                            }
                        }
                    }
                }

                // Scale ticks row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 106.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0", "15", "30", "45", "60").forEach { tick ->
                        Text(
                            text = tick,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate400
                        )
                    }
                }
            }
        }

        // 6. Fabric / Material Composition Donut Chart (Matching IMG_9741.png)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fabric / Material Composition",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    modifier = Modifier.align(Alignment.Start)
                )

                val palette = listOf(
                    Color(0xFF9333EA), // purple - not pure cotton
                    Color(0xFF2563EB), // blue - pure cotton
                    Color(0xFF10B981), // emerald - sand
                    Color(0xFFF59E0B), // amber - lace
                    Color(0xFFEF4444), // red - chiffon
                    Color(0xFFEC4899), // pink - other
                    Color(0xFF6366F1)  // indigo
                )

                val totalItems = clothes.size.coerceAtLeast(1)

                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(165.dp)) {
                        val strokeWidth = 38.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        var startAngle = -90f
                        val gapAngle = 2.5f

                        materialBreakdown.forEachIndexed { idx, entry ->
                            val sweep = (entry.value.toFloat() / totalItems.toFloat()) * 360f
                            val color = palette[idx % palette.size]
                            drawArc(
                                color = color,
                                startAngle = startAngle + gapAngle / 2f,
                                sweepAngle = (sweep - gapAngle).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                }

                // Legend matching IMG_9741.png
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    materialBreakdown.forEachIndexed { idx, entry ->
                        val color = palette[idx % palette.size]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                            Text(
                                text = entry.key,
                                fontSize = 12.sp,
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
            }

            Text(
                text = value,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Slate900
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Slate500
            )
        }
    }
}

@Composable
private fun LeaderboardItemRow(
    rank: Int,
    name: String,
    subtitle: String,
    badgeText: String,
    progress: Float,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isBestValue) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                        border = BorderStroke(
                            1.dp,
                            if (isBestValue) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
                        ),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$rank",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isBestValue) Color(0xFF047857) else Color(0xFFB45309)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isBestValue) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isBestValue) Color(0xFF047857) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Slate100)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isBestValue) Color(0xFF10B981) else Color(0xFFF59E0B))
                )
            }
        }
    }
}
