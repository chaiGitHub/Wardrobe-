package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleDark
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun AppHeader(
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(BrandPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checkroom,
                    contentDescription = "Wardrobe Logo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Wardrobe & Wear Journal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1
                )
                Text(
                    text = "100% Local Application · Disk Persistence",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Slate500,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TopNavigationBar(
    activeTab: AppTab,
    clothesCount: Int,
    dirtyCount: Int,
    journalCount: Int,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate100)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabPill(
            title = "Closet ($clothesCount)",
            icon = Icons.Default.Checkroom,
            isSelected = activeTab == AppTab.CLOSET,
            activeColor = BrandPurple,
            onClick = { onTabSelected(AppTab.CLOSET) },
            testTag = "nav_tab_closet"
        )

        TabPill(
            title = "Wear Calendar",
            icon = Icons.Default.CalendarMonth,
            isSelected = activeTab == AppTab.CALENDAR,
            activeColor = BrandPurple,
            onClick = { onTabSelected(AppTab.CALENDAR) },
            testTag = "nav_tab_calendar"
        )

        TabPill(
            title = "Laundry Care",
            icon = Icons.Default.LocalLaundryService,
            badge = if (dirtyCount > 0) dirtyCount.toString() else null,
            isSelected = activeTab == AppTab.LAUNDRY,
            activeColor = BrandBlue,
            onClick = { onTabSelected(AppTab.LAUNDRY) },
            testTag = "nav_tab_laundry"
        )

        TabPill(
            title = "Wear Journal ($journalCount)",
            icon = Icons.Default.MenuBook,
            isSelected = activeTab == AppTab.JOURNAL,
            activeColor = BrandIndigo,
            onClick = { onTabSelected(AppTab.JOURNAL) },
            testTag = "nav_tab_journal"
        )

        TabPill(
            title = "Drill-Down Report",
            icon = Icons.Default.FilterList,
            isSelected = activeTab == AppTab.DRILLDOWN,
            activeColor = BrandEmerald,
            onClick = { onTabSelected(AppTab.DRILLDOWN) },
            testTag = "nav_tab_drilldown"
        )

        TabPill(
            title = "AI Styling",
            icon = Icons.Default.AutoAwesome,
            isSelected = activeTab == AppTab.ADVISOR,
            activeColor = BrandAmber,
            onClick = { onTabSelected(AppTab.ADVISOR) },
            testTag = "nav_tab_advisor"
        )

        TabPill(
            title = "Local Storage",
            icon = Icons.Default.CloudDownload,
            isSelected = activeTab == AppTab.SYNC,
            activeColor = BrandPurpleDark,
            onClick = { onTabSelected(AppTab.SYNC) },
            testTag = "nav_tab_sync"
        )
    }
}

@Composable
private fun TabPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    badge: String? = null,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) activeColor else Color.White,
        border = if (isSelected) null else CardDefaults.outlinedCardBorder(),
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else Slate600,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = if (isSelected) Color.White else Slate700,
                maxLines = 1
            )

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else BrandAmber)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        color = if (isSelected) activeColor else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        "all" to "All Items",
        "favorites" to "🌟 beautiful: Beautiful",
        "sit-ok" to "💺 sit breast ok",
        "need-wash" to "🧺 wash stated: need wash",
        "chiffon" to "🪶 material: chiffon",
        "not-hot" to "☀️ Hot at noon: Not hot"
    )

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (key, label) ->
            val isSelected = selectedFilter == key
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(key) },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandPurpleLight,
                    selectedLabelColor = BrandPurple,
                    containerColor = Color.White,
                    labelColor = Slate600
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) BrandPurple else Slate200,
                    selectedBorderColor = BrandPurple,
                    borderWidth = 1.dp
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("filter_chip_$key")
            )
        }
    }
}
