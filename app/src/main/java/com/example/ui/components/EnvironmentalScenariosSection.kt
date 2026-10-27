package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GarmentEntity
import com.example.ui.dialogs.FieldDictionaryService
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/**
 * Reusable Edit-mode component rendering expandable cards for the 3 environmental scenarios:
 * 1. When With Aircond
 * 2. When Without Aircond
 * 3. When Sleep Under Fan
 */
@Composable
fun EnvironmentalScenariosEditSection(
    // Scenario 1: When with aircond
    aircondHotFeeling: String,
    onAircondHotChange: (String) -> Unit,
    onAircondHotPrevious: () -> Unit,
    aircondHotExplain: String,
    onAircondHotExplainChange: (String) -> Unit,
    aircondLegFeeling: String,
    onAircondLegChange: (String) -> Unit,
    onAircondLegPrevious: () -> Unit,
    aircondLegExplain: String,
    onAircondLegExplainChange: (String) -> Unit,

    // Scenario 2: When without aircond
    noAircondHotFeeling: String,
    onNoAircondHotChange: (String) -> Unit,
    onNoAircondHotPrevious: () -> Unit,
    noAircondHotExplain: String,
    onNoAircondHotExplainChange: (String) -> Unit,
    noAircondLegFeeling: String,
    onNoAircondLegChange: (String) -> Unit,
    onNoAircondLegPrevious: () -> Unit,
    noAircondLegExplain: String,
    onNoAircondLegExplainChange: (String) -> Unit,

    // Scenario 3: When sleep under fan
    fanSleepHotFeeling: String,
    onFanSleepHotChange: (String) -> Unit,
    onFanSleepHotPrevious: () -> Unit,
    fanSleepHotExplain: String,
    onFanSleepHotExplainChange: (String) -> Unit,
    fanSleepLegFeeling: String,
    onFanSleepLegChange: (String) -> Unit,
    onFanSleepLegPrevious: () -> Unit,
    fanSleepLegExplain: String,
    onFanSleepLegExplainChange: (String) -> Unit,

    allClothes: List<GarmentEntity> = emptyList(),
    dictionaryService: FieldDictionaryService? = null,
    modifier: Modifier = Modifier
) {
    // Keep track of which scenarios and explain sub-fields are expanded
    var aircondCardExpanded by remember { mutableStateOf(true) }
    var noAircondCardExpanded by remember { mutableStateOf(true) }
    var fanSleepCardExpanded by remember { mutableStateOf(true) }

    val explainExpandedMap = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        ) {
            Text(
                text = "ENVIRONMENTAL SCENARIOS (3 MODES)",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Text(
                    text = "EXPANDABLE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Scenario 1: WHEN WITH AIRCOND
        ScenarioCard(
            title = "WHEN WITH AIRCOND",
            subtitle = "开空调时环境感受",
            icon = Icons.Default.AcUnit,
            iconTint = Color(0xFF0284C7),
            cardBorderColor = Color(0xFFBAE6FD),
            headerBgColor = Color(0xFFF0F9FF),
            summaryText = buildSummary(aircondHotFeeling, aircondLegFeeling),
            isExpanded = aircondCardExpanded,
            onToggleExpand = { aircondCardExpanded = !aircondCardExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Hot feeling
                ScenarioFieldWithPrevious(
                    label = "HOT FEELING",
                    value = aircondHotFeeling,
                    placeholder = "e.g. not so cold, cold, comfortable",
                    onValueChange = onAircondHotChange,
                    onOpenPrevious = onAircondHotPrevious,
                    fieldKey = "when with aircond: hot feeling",
                    explainValue = aircondHotExplain,
                    onExplainChange = onAircondHotExplainChange,
                    explainPlaceholder = "例如: due to thick chiffon material, thick clothes so no so cold.",
                    isExplainExpanded = explainExpandedMap["aircond_hot"] ?: false,
                    onToggleExplainExpand = { explainExpandedMap["aircond_hot"] = !(explainExpandedMap["aircond_hot"] ?: false) },
                    allClothes = allClothes,
                    dictionaryService = dictionaryService
                )

                // Leg feeling
                ScenarioFieldWithPrevious(
                    label = "LEG FEELING",
                    value = aircondLegFeeling,
                    placeholder = "e.g. not so cold, chilly knees",
                    onValueChange = onAircondLegChange,
                    onOpenPrevious = onAircondLegPrevious,
                    fieldKey = "when with aircond: leg feeling",
                    explainValue = aircondLegExplain,
                    onExplainChange = onAircondLegExplainChange,
                    explainPlaceholder = "例如: due to middle long dress, thick chiffon material, thick clothes.",
                    isExplainExpanded = explainExpandedMap["aircond_leg"] ?: false,
                    onToggleExplainExpand = { explainExpandedMap["aircond_leg"] = !(explainExpandedMap["aircond_leg"] ?: false) },
                    allClothes = allClothes,
                    dictionaryService = dictionaryService
                )
            }
        }

        // Scenario 2: WHEN WITHOUT AIRCOND (Migrated from legacy Hot at noon)
        ScenarioCard(
            title = "WHEN WITHOUT AIRCOND",
            subtitle = "没开空调时环境感受 (原 Hot at noon)",
            icon = Icons.Default.WbSunny,
            iconTint = Color(0xFFEA580C),
            cardBorderColor = Color(0xFFFED7AA),
            headerBgColor = Color(0xFFFFF7ED),
            summaryText = buildSummary(noAircondHotFeeling, noAircondLegFeeling),
            isExpanded = noAircondCardExpanded,
            onToggleExpand = { noAircondCardExpanded = !noAircondCardExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Hot feeling
                ScenarioFieldWithPrevious(
                    label = "HOT FEELING",
                    value = noAircondHotFeeling,
                    placeholder = "e.g. hot, middle hot, very hot",
                    onValueChange = onNoAircondHotChange,
                    onOpenPrevious = onNoAircondHotPrevious,
                    fieldKey = "when without aircond: hot feeling",
                    explainValue = noAircondHotExplain,
                    onExplainChange = onNoAircondHotExplainChange,
                    explainPlaceholder = "例如: due to thick chiffon material, thick clothes.",
                    isExplainExpanded = explainExpandedMap["no_aircond_hot"] ?: false,
                    onToggleExplainExpand = { explainExpandedMap["no_aircond_hot"] = !(explainExpandedMap["no_aircond_hot"] ?: false) },
                    allClothes = allClothes,
                    dictionaryService = dictionaryService
                )

                // Leg feeling
                ScenarioFieldWithPrevious(
                    label = "LEG FEELING",
                    value = noAircondLegFeeling,
                    placeholder = "e.g. middle hot, sticky, breathable",
                    onValueChange = onNoAircondLegChange,
                    onOpenPrevious = onNoAircondLegPrevious,
                    fieldKey = "when without aircond: leg feeling",
                    explainValue = noAircondLegExplain,
                    onExplainChange = onNoAircondLegExplainChange,
                    explainPlaceholder = "例如: due to middle long dress, thick chiffon material, thick clothes.little more swear",
                    isExplainExpanded = explainExpandedMap["no_aircond_leg"] ?: false,
                    onToggleExplainExpand = { explainExpandedMap["no_aircond_leg"] = !(explainExpandedMap["no_aircond_leg"] ?: false) },
                    allClothes = allClothes,
                    dictionaryService = dictionaryService
                )
            }
        }

        // Scenario 3: WHEN SLEEP UNDER FAN
        ScenarioCard(
            title = "WHEN SLEEP UNDER FAN",
            subtitle = "风扇下睡觉环境感受",
            icon = Icons.Default.Air,
            iconTint = Color(0xFF7C3AED),
            cardBorderColor = Color(0xFFE9D5FF),
            headerBgColor = Color(0xFFFAF5FF),
            summaryText = buildSummary(fanSleepHotFeeling, fanSleepLegFeeling),
            isExpanded = fanSleepCardExpanded,
            onToggleExpand = { fanSleepCardExpanded = !fanSleepCardExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Hot feeling
                ScenarioFieldWithPrevious(
                    label = "HOT FEELING",
                    value = fanSleepHotFeeling,
                    placeholder = "e.g. not hot, comfortable, cool",
                    onValueChange = onFanSleepHotChange,
                    onOpenPrevious = onFanSleepHotPrevious,
                    fieldKey = "when sleep under fan: hot feeling",
                    explainValue = fanSleepHotExplain,
                    onExplainChange = onFanSleepHotExplainChange,
                    explainPlaceholder = "例如: under fan wind bigger and cold bed help reduce feeling of hot",
                    isExplainExpanded = explainExpandedMap["fan_sleep_hot"] ?: false,
                    onToggleExplainExpand = { explainExpandedMap["fan_sleep_hot"] = !(explainExpandedMap["fan_sleep_hot"] ?: false) },
                    allClothes = allClothes,
                    dictionaryService = dictionaryService
                )

                // Leg feeling
                ScenarioFieldWithPrevious(
                    label = "LEG FEELING",
                    value = fanSleepLegFeeling,
                    placeholder = "e.g. middle cold, cold, comfortable",
                    onValueChange = onFanSleepLegChange,
                    onOpenPrevious = onFanSleepLegPrevious,
                    fieldKey = "when sleep under fan: leg feeling",
                    explainValue = fanSleepLegExplain,
                    onExplainChange = onFanSleepLegExplainChange,
                    explainPlaceholder = "例如: due to middle long dress, inner thin, easy fold up 裙底，even it is thick clothes outer",
                    isExplainExpanded = explainExpandedMap["fan_sleep_leg"] ?: false,
                    onToggleExplainExpand = { explainExpandedMap["fan_sleep_leg"] = !(explainExpandedMap["fan_sleep_leg"] ?: false) },
                    allClothes = allClothes,
                    dictionaryService = dictionaryService
                )
            }
        }
    }
}

/**
 * Reusable View-mode component rendering expandable cards for the 3 environmental scenarios
 * in GarmentDetailDialog (View Mode).
 */
@Composable
fun EnvironmentalScenariosViewSection(
    garment: GarmentEntity,
    fieldExplanations: Map<String, String>,
    modifier: Modifier = Modifier
) {
    var aircondExpanded by remember { mutableStateOf(true) }
    var noAircondExpanded by remember { mutableStateOf(true) }
    var fanSleepExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        ) {
            Text(
                text = "ENVIRONMENTAL SCENARIO OBSERVATIONS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Text(
                    text = "3 MODES",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Scenario 1: WHEN WITH AIRCOND
        ScenarioCard(
            title = "WHEN WITH AIRCOND",
            subtitle = "开空调时环境感受",
            icon = Icons.Default.AcUnit,
            iconTint = Color(0xFF0284C7),
            cardBorderColor = Color(0xFFBAE6FD),
            headerBgColor = Color(0xFFF0F9FF),
            summaryText = buildSummary(garment.aircondHotFeeling, garment.aircondLegFeeling),
            isExpanded = aircondExpanded,
            onToggleExpand = { aircondExpanded = !aircondExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScenarioViewRow(
                    label = "hot feeling:",
                    value = garment.aircondHotFeeling.ifBlank { "—" },
                    explanation = garment.aircondHotExplain.ifBlank { fieldExplanations["when with aircond: hot feeling"] ?: fieldExplanations["aircondHotFeeling"] }
                )
                ScenarioViewRow(
                    label = "leg feeling:",
                    value = garment.aircondLegFeeling.ifBlank { "—" },
                    explanation = garment.aircondLegExplain.ifBlank { fieldExplanations["when with aircond: leg feeling"] ?: fieldExplanations["aircondLegFeeling"] }
                )
            }
        }

        // Scenario 2: WHEN WITHOUT AIRCOND (Migrated from hotAtNoon)
        val effectiveNoAircondHot = garment.noAircondHotFeeling.ifBlank { garment.hotAtNoon }
        ScenarioCard(
            title = "WHEN WITHOUT AIRCOND",
            subtitle = "没开空调时环境感受",
            icon = Icons.Default.WbSunny,
            iconTint = Color(0xFFEA580C),
            cardBorderColor = Color(0xFFFED7AA),
            headerBgColor = Color(0xFFFFF7ED),
            summaryText = buildSummary(effectiveNoAircondHot, garment.noAircondLegFeeling),
            isExpanded = noAircondExpanded,
            onToggleExpand = { noAircondExpanded = !noAircondExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScenarioViewRow(
                    label = "hot feeling:",
                    value = effectiveNoAircondHot.ifBlank { "—" },
                    explanation = garment.noAircondHotExplain.ifBlank { fieldExplanations["when without aircond: hot feeling"] ?: fieldExplanations["Hot at noon"] }
                )
                ScenarioViewRow(
                    label = "leg feeling:",
                    value = garment.noAircondLegFeeling.ifBlank { "—" },
                    explanation = garment.noAircondLegExplain.ifBlank { fieldExplanations["when without aircond: leg feeling"] ?: fieldExplanations["noAircondLegFeeling"] }
                )
            }
        }

        // Scenario 3: WHEN SLEEP UNDER FAN
        ScenarioCard(
            title = "WHEN SLEEP UNDER FAN",
            subtitle = "风扇下睡觉环境感受",
            icon = Icons.Default.Air,
            iconTint = Color(0xFF7C3AED),
            cardBorderColor = Color(0xFFE9D5FF),
            headerBgColor = Color(0xFFFAF5FF),
            summaryText = buildSummary(garment.fanSleepHotFeeling, garment.fanSleepLegFeeling),
            isExpanded = fanSleepExpanded,
            onToggleExpand = { fanSleepExpanded = !fanSleepExpanded }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScenarioViewRow(
                    label = "hot feeling:",
                    value = garment.fanSleepHotFeeling.ifBlank { "—" },
                    explanation = garment.fanSleepHotExplain.ifBlank { fieldExplanations["when sleep under fan: hot feeling"] ?: fieldExplanations["fanSleepHotFeeling"] }
                )
                ScenarioViewRow(
                    label = "leg feeling:",
                    value = garment.fanSleepLegFeeling.ifBlank { "—" },
                    explanation = garment.fanSleepLegExplain.ifBlank { fieldExplanations["when sleep under fan: leg feeling"] ?: fieldExplanations["fanSleepLegFeeling"] }
                )
            }
        }
    }
}

@Composable
private fun ScenarioViewRow(
    label: String,
    value: String,
    explanation: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate100, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Slate600
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }
        // Permanent explain field arranged beautifully below related scenario item
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAF5FF), RoundedCornerShape(6.dp))
                .border(BorderStroke(0.5.dp, Color(0xFFF3E8FF)), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${label.removeSuffix(":")} explain:",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = BrandPurple
            )
            Text(
                text = if (!explanation.isNullOrBlank()) explanation else "No explain added",
                fontSize = 11.sp,
                fontStyle = if (explanation.isNullOrBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                color = if (explanation.isNullOrBlank()) Slate400 else Color(0xFF6B21A8),
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScenarioCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    cardBorderColor: Color,
    headerBgColor: Color,
    summaryText: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Clickable Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Text(
                            text = if (!isExpanded && summaryText.isNotBlank()) summaryText else subtitle,
                            fontSize = 11.sp,
                            color = if (!isExpanded && summaryText.isNotBlank()) Slate700 else Slate500,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ScenarioFieldWithPrevious(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onOpenPrevious: () -> Unit,
    fieldKey: String,
    explainValue: String,
    onExplainChange: (String) -> Unit,
    explainPlaceholder: String,
    isExplainExpanded: Boolean,
    onToggleExplainExpand: () -> Unit,
    allClothes: List<GarmentEntity>,
    dictionaryService: FieldDictionaryService?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Slate600
            )

            // Previous pill button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenPrevious)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Previous entries",
                        tint = Slate500,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Previous",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Slate200,
                focusedBorderColor = BrandPurple
            )
        )

        // Ergonomic explain field
        ErgonomicExplainField(
            fieldKey = fieldKey,
            fieldLabel = label,
            value = explainValue,
            onValueChange = onExplainChange,
            isExpanded = isExplainExpanded,
            onToggleExpand = onToggleExplainExpand,
            allClothes = allClothes,
            customPlaceholder = explainPlaceholder,
            dictionaryService = dictionaryService
        )
    }
}

private fun buildSummary(hot: String, leg: String): String {
    val h = hot.trim().ifBlank { "—" }
    val l = leg.trim().ifBlank { "—" }
    return "Hot: $h · Leg: $l"
}
