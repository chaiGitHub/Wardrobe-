package com.example.ui.dialogs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.BrandPurpleLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Locale

data class PreviousEntry(
    val value: String,
    val count: Int,
    val labelCn: String? = null,
    val meaningCn: String? = null,
    val sources: List<String> = emptyList()
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviousEntriesModal(
    isOpen: Boolean,
    fieldLabel: String,
    entries: List<PreviousEntry>,
    currentValue: String = "",
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    var searchQuery by remember { mutableStateOf("") }

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) entries
        else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            entries.filter { entry ->
                entry.value.lowercase(Locale.ROOT).contains(q) ||
                (entry.labelCn?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                (entry.meaningCn?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                entry.sources.any { s -> s.lowercase(Locale.ROOT).contains(q) }
            }
        }
    }

    val exactMatchExists = remember(entries, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        entries.any { it.value.lowercase(Locale.ROOT) == q }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .testTag("previous_entries_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandPurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Choose $fieldLabel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${entries.size} recorded values found. Can search by source clothes.",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200)

                // Search box (with source search support)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate100.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search or type new $fieldLabel (or source clothes e.g. dress)...",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(14.dp))
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPurple,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                // Quick Clear / Leave Empty button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            onSelect("")
                            onClose()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                            Text("Clear / Leave Empty", fontSize = 12.sp, color = Slate600, fontWeight = FontWeight.Medium)
                        }
                        if (currentValue.isBlank()) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = BrandPurple, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // If typing a custom new value not in existing entries
                if (searchQuery.isNotBlank() && !exactMatchExists) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(searchQuery.trim())
                                onClose()
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = BrandPurpleLight.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPurple.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Use custom: \"${searchQuery.trim()}\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPurple
                            )
                        }
                    }
                }

                // Entries List (Cards matching Image 1)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredEntries.isEmpty() && (searchQuery.isBlank() || exactMatchExists)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No previous values found for $fieldLabel.",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }

                    items(filteredEntries, key = { it.value }) { entry ->
                        val isSelected = currentValue.equals(entry.value, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(entry.value)
                                    onClose()
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFFFAF5FF) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BrandPurple else Slate200
                            ),
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Title & Chinese Label Pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.value,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (!entry.labelCn.isNullOrBlank() && entry.labelCn != entry.value) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF3E8FF)
                                        ) {
                                            Text(
                                                text = entry.labelCn,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7E22CE),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // Meaning CN / 体感定义 box (Purple Box as in Screenshot)
                                if (!entry.meaningCn.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFAF5FF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = BrandPurple,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "Meaning CN / 体感定义:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF581C87)
                                                )
                                            }
                                            Text(
                                                text = entry.meaningCn,
                                                fontSize = 11.sp,
                                                color = Slate800,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }

                                // Badges: Used count & 来源衣物:
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Slate100
                                    ) {
                                        Text(
                                            text = if (entry.count > 0) "Used ${entry.count} ${if (entry.count == 1) "time" else "times"}" else "Preset Option",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate600,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // 来源衣物 (Source clothes pills)
                                if (entry.sources.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Checkroom,
                                                contentDescription = null,
                                                tint = BrandPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "来源衣物:",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700
                                            )
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            entry.sources.forEach { src ->
                                                val q = searchQuery.trim().lowercase(Locale.ROOT)
                                                val isMatch = q.isNotBlank() && src.lowercase(Locale.ROOT).contains(q)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isMatch) Color(0xFFE9D5FF) else Color(0xFFF3E8FF),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.5.dp,
                                                        if (isMatch) BrandPurple else Color(0xFFD8B4FE)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Text("👕", fontSize = 10.sp)
                                                        Text(
                                                            text = src,
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isMatch) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = Color(0xFF581C87)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Bottom action: 选择此项 (Use)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            onSelect(entry.value)
                                            onClose()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF7E22CE) else BrandPurple
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isSelected) "✓ 当前已选" else "选择此项 (Use)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
