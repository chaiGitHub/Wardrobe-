package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun AppHeader(
    onLogWearClick: (() -> Unit)? = null,
    onAddItemClick: (() -> Unit)? = null,
    clothesCount: Int = 0,
    reportsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 700.dp
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1440.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            horizontal = if (isWide) 24.dp else 14.dp,
                            vertical = if (isWide) 12.dp else 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Dark square shirt logo + "Wardrobe" title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate900),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = "Wardrobe Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Wardrobe",
                                fontSize = if (isWide) 18.sp else 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                                maxLines = 1
                            )
                            if (isWide && (clothesCount > 0 || reportsCount > 0)) {
                                Text(
                                    text = "$clothesCount garments · $reportsCount wear logs",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Right: [+ Log Wear] (dark button) and [+ Item] (emerald button)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onLogWearClick != null) {
                            Button(
                                onClick = onLogWearClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Slate900,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = if (isWide) 14.dp else 12.dp,
                                    vertical = 0.dp
                                ),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("header_log_wear_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Log Wear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (onAddItemClick != null) {
                            Button(
                                onClick = onAddItemClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = if (isWide) 14.dp else 12.dp,
                                    vertical = 0.dp
                                ),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("header_add_item_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Item",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
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

    Surface(
        color = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 700.dp
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1440.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            horizontal = if (isWide) 24.dp else 10.dp,
                            vertical = 6.dp
                        )
                        .horizontalScroll(scrollState),
                    horizontalArrangement = if (isWide) Arrangement.spacedBy(8.dp) else Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabPill(
                        title = "Closet",
                        icon = Icons.Default.Checkroom,
                        isSelected = activeTab == AppTab.CLOSET,
                        onClick = { onTabSelected(AppTab.CLOSET) },
                        testTag = "nav_tab_closet"
                    )

                    TabPill(
                        title = "Journal",
                        icon = Icons.Default.Description,
                        isSelected = activeTab == AppTab.JOURNAL,
                        onClick = { onTabSelected(AppTab.JOURNAL) },
                        testTag = "nav_tab_journal"
                    )

                    TabPill(
                        title = "Drilldown",
                        icon = Icons.Default.AccountTree,
                        isSelected = activeTab == AppTab.DRILLDOWN,
                        onClick = { onTabSelected(AppTab.DRILLDOWN) },
                        testTag = "nav_tab_drilldown"
                    )

                    TabPill(
                        title = "Calendar",
                        icon = Icons.Default.CalendarMonth,
                        isSelected = activeTab == AppTab.CALENDAR,
                        onClick = { onTabSelected(AppTab.CALENDAR) },
                        testTag = "nav_tab_calendar"
                    )

                    TabPill(
                        title = "Laundry",
                        icon = Icons.Default.Opacity,
                        badge = if (dirtyCount > 0) dirtyCount.toString() else null,
                        isSelected = activeTab == AppTab.LAUNDRY,
                        onClick = { onTabSelected(AppTab.LAUNDRY) },
                        testTag = "nav_tab_laundry"
                    )

                    TabPill(
                        title = "Storage",
                        icon = Icons.Default.Storage,
                        isSelected = activeTab == AppTab.SYNC,
                        onClick = { onTabSelected(AppTab.SYNC) },
                        testTag = "nav_tab_sync"
                    )

                    TabPill(
                        title = "AI Advisor",
                        icon = Icons.Default.AutoAwesome,
                        isSelected = activeTab == AppTab.ADVISOR,
                        onClick = { onTabSelected(AppTab.ADVISOR) },
                        testTag = "nav_tab_advisor"
                    )
                }
                HorizontalDivider(color = Slate200)
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
    testTag: String
) {
    val activeBg = Color(0xFFECFDF5)
    val activeText = Color(0xFF047857)
    val inactiveText = Slate600

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) activeBg else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) activeText else inactiveText,
                modifier = Modifier.size(15.dp)
            )

            Text(
                text = title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) activeText else inactiveText,
                maxLines = 1
            )

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
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
