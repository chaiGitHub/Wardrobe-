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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
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
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.LinearProgressIndicator

@Composable
fun AppHeader(
    onLogWearClick: (() -> Unit)? = null,
    onAddItemClick: (() -> Unit)? = null,
    onPullFromCloudClick: (() -> Unit)? = null,
    onPushToCloudClick: (() -> Unit)? = null,
    isCloudConnected: Boolean = true,
    isSyncing: Boolean = false,
    syncProgress: Int = 0,
    syncMessage: String = "",
    clothesCount: Int = 0,
    reportsCount: Int = 0,
    activeTab: AppTab = AppTab.CLOSET,
    onTabSelected: ((AppTab) -> Unit)? = null,
    dirtyCount: Int = 0,
    onFontSizeClick: (() -> Unit)? = null,
    onNotebookLmClick: (() -> Unit)? = null,
    isAiPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 600.dp
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1440.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            horizontal = if (isWide) 12.dp else 8.dp,
                            vertical = if (isWide) 6.dp else 4.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Gradient purple/indigo square shirt logo + "Wardrobe & Wear Journal" title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable { onTabSelected?.invoke(AppTab.CLOSET) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6B21A8), Color(0xFF3730A3))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = "Wardrobe Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isWide) "Wardrobe & Wear Journal" else "Wardrobe",
                                    fontSize = if (isWide) 17.sp else 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900,
                                    maxLines = 1
                                )

                                // Cloud status indicator pill
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isCloudConnected) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, if (isCloudConnected) Color(0xFFA7F3D0) else Color(0xFFFDE68A))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isCloudConnected) Color(0xFF059669) else Color(0xFFD97706))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCloudConnected) "Cloud" else "Local",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCloudConnected) Color(0xFF047857) else Color(0xFFB45309)
                                        )
                                    }
                                }

                                if (isAiPaused) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.clickable { onTabSelected?.invoke(AppTab.ADVISOR) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "⏸️ Zero Token",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "Local-First · Google Firestore",
                                fontSize = 10.sp,
                                color = Slate500,
                                maxLines = 1
                            )
                        }
                    }

                    // Middle: Desktop Navigation Tabs (matching webapp nav: hidden md:flex)
                    if (isWide && onTabSelected != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                DesktopTabButton(
                                    title = "Closet (${clothesCount})",
                                    icon = Icons.Default.Checkroom,
                                    isSelected = activeTab == AppTab.CLOSET,
                                    iconColor = Color(0xFF9333EA),
                                    onClick = { onTabSelected(AppTab.CLOSET) }
                                )
                                DesktopTabButton(
                                    title = "Laundry Care",
                                    icon = Icons.Default.Opacity,
                                    isSelected = activeTab == AppTab.LAUNDRY,
                                    iconColor = Color(0xFF2563EB),
                                    badge = if (dirtyCount > 0) "$dirtyCount" else null,
                                    badgeColor = Color(0xFFF59E0B),
                                    onClick = { onTabSelected(AppTab.LAUNDRY) }
                                )
                                DesktopTabButton(
                                    title = "Wear Journal (${reportsCount})",
                                    icon = Icons.Default.Description,
                                    isSelected = activeTab == AppTab.JOURNAL,
                                    iconColor = Color(0xFF4F46E5),
                                    onClick = { onTabSelected(AppTab.JOURNAL) }
                                )
                                DesktopTabButton(
                                    title = "Drill-Down Report",
                                    icon = Icons.Default.AccountTree,
                                    isSelected = activeTab == AppTab.DRILLDOWN,
                                    iconColor = Color(0xFF059669),
                                    onClick = { onTabSelected(AppTab.DRILLDOWN) }
                                )
                                DesktopTabButton(
                                    title = "AI Styling",
                                    icon = Icons.Default.AutoAwesome,
                                    isSelected = activeTab == AppTab.ADVISOR,
                                    iconColor = Color(0xFFD97706),
                                    onClick = { onTabSelected(AppTab.ADVISOR) }
                                )
                                DesktopTabButton(
                                    title = "Calendar",
                                    icon = Icons.Default.CalendarMonth,
                                    isSelected = activeTab == AppTab.CALENDAR,
                                    iconColor = Color(0xFF0284C7),
                                    onClick = { onTabSelected(AppTab.CALENDAR) }
                                )
                                DesktopTabButton(
                                    title = "Storage",
                                    icon = Icons.Default.Storage,
                                    isSelected = activeTab == AppTab.SYNC,
                                    iconColor = Slate700,
                                    onClick = { onTabSelected(AppTab.SYNC) }
                                )
                            }
                        }
                    }

                    // Right: Action area matching webapp App.tsx header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        // Pull from Cloud button (blue tint)
                        if (onPullFromCloudClick != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPullFromCloudClick() }
                                    .testTag("header_pull_cloud_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Pull",
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    if (isWide) {
                                        Text(
                                            text = "Pull",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1D4ED8)
                                        )
                                    }
                                }
                            }
                        }

                        // Push to Cloud button (emerald tint)
                        if (onPushToCloudClick != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPushToCloudClick() }
                                    .testTag("header_push_cloud_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Push",
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    if (isWide) {
                                        Text(
                                            text = "Push",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            }
                        }

                        // Log Outfit button (Purple 700 with + icon)
                        if (onLogWearClick != null) {
                            Button(
                                onClick = onLogWearClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6B21A8),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("header_log_wear_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isWide) "Log Outfit" else "Log",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Add Item button
                        if (onAddItemClick != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF059669),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddItemClick() }
                                    .testTag("header_add_item_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Item",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Item",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Google NotebookLM Knowledge Base export button
                        if (onNotebookLmClick != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNotebookLmClick() }
                                    .testTag("header_notebooklm_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text("📚", fontSize = 11.sp)
                                    Text(
                                        text = "NotebookLM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandIndigo
                                    )
                                }
                            }
                        }

                        // Font Size Switcher button (Aa)
                        if (onFontSizeClick != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onFontSizeClick() }
                                    .testTag("header_font_size_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Aa",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate800
                                    )
                                }
                            }
                        }
                    }
                }

                // Sync Progress Banner (Animated when sync active)
                if (isSyncing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F3FF))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
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
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = BrandPurple,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = syncMessage.ifBlank { "Synchronizing data..." },
                                    fontSize = 11.sp,
                                    color = BrandPurple,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "$syncProgress%",
                                fontSize = 11.sp,
                                color = BrandPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { syncProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = BrandPurple,
                            trackColor = Color(0xFFDDD6FE)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBar(
    activeTab: AppTab,
    clothesCount: Int,
    dirtyCount: Int,
    journalCount: Int,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val isWide = maxWidth >= 600.dp
            Row(
                modifier = Modifier
                    .widthIn(max = 1440.dp)
                    .fillMaxWidth()
                    .padding(horizontal = if (isWide) 24.dp else 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                data class NavTabSpec(
                    val tab: AppTab,
                    val title: String,
                    val icon: ImageVector,
                    val badge: String?,
                    val activeColor: Color,
                    val testTag: String
                )

                val navTabs = listOf(
                    NavTabSpec(
                        tab = AppTab.CLOSET,
                        title = "Closet",
                        icon = Icons.Default.Checkroom,
                        badge = if (clothesCount > 0) clothesCount.toString() else null,
                        activeColor = BrandPurple,
                        testTag = "nav_tab_closet"
                    ),
                    NavTabSpec(
                        tab = AppTab.LAUNDRY,
                        title = "Laundry",
                        icon = Icons.Default.Opacity,
                        badge = if (dirtyCount > 0) dirtyCount.toString() else null,
                        activeColor = Color(0xFF2563EB),
                        testTag = "nav_tab_laundry"
                    ),
                    NavTabSpec(
                        tab = AppTab.JOURNAL,
                        title = "Journal",
                        icon = Icons.Default.Description,
                        badge = if (journalCount > 0) journalCount.toString() else null,
                        activeColor = BrandIndigo,
                        testTag = "nav_tab_journal"
                    ),
                    NavTabSpec(
                        tab = AppTab.DRILLDOWN,
                        title = "Drill-Down",
                        icon = Icons.Default.AccountTree,
                        badge = null,
                        activeColor = BrandEmerald,
                        testTag = "nav_tab_drilldown"
                    ),
                    NavTabSpec(
                        tab = AppTab.ADVISOR,
                        title = "AI Styling",
                        icon = Icons.Default.AutoAwesome,
                        badge = null,
                        activeColor = BrandAmber,
                        testTag = "nav_tab_advisor"
                    ),
                    NavTabSpec(
                        tab = AppTab.SYNC,
                        title = "Storage",
                        icon = Icons.Default.Storage,
                        badge = null,
                        activeColor = Slate900,
                        testTag = "nav_tab_sync"
                    )
                )

                navTabs.forEach { spec ->
                    val isSelected = activeTab == spec.tab || (spec.tab == AppTab.JOURNAL && activeTab == AppTab.CALENDAR)
                    val activeBg = spec.activeColor.copy(alpha = 0.12f)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) activeBg else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTabSelected(spec.tab) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag(spec.testTag)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.title,
                                    tint = if (isSelected) spec.activeColor else Slate500,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (spec.badge != null) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 12.dp, bottom = 8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (spec.tab == AppTab.LAUNDRY) Color(0xFFF59E0B) else Slate700
                                            )
                                            .padding(horizontal = 4.dp, vertical = 0.5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = spec.badge,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = spec.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) spec.activeColor else Slate600,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.buttonMarquee()
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

@Composable
private fun DesktopTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    iconColor: Color = Color(0xFF7C3AED),
    badge: String? = null,
    badgeColor: Color = Color(0xFFF59E0B),
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) iconColor else Color(0xFF64748B),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569)
            )
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
