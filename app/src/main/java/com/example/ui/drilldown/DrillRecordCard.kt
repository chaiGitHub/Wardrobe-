package com.example.ui.drilldown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
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
import com.example.util.ImageUtils
import org.json.JSONArray
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrillRecordCard(
    garment: GarmentEntity,
    preset: DrillPreset,
    junctions: List<JunctionEntity>,
    onSelect: (GarmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats = remember(garment.id, junctions) {
        DrilldownUtils.calculateGarmentStats(garment.id, junctions)
    }

    // Extract first photo URL or file path if available
    val firstPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
        ImageUtils.resolveImageModel(it, context)
    }

    val totalWashedCount = garment.wearingFrequency + garment.trackedAppWashes
    val costPerWear = remember(garment.price, totalWashedCount) {
        if (garment.price > 0 && totalWashedCount > 0) {
            String.format(Locale.US, "$%.2f", garment.price / totalWashedCount)
        } else if (garment.price > 0) {
            String.format(Locale.US, "$%.2f", garment.price)
        } else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("drill_card_${garment.id}")
            .clickable { onSelect(garment) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Photo & Header Badges Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                if (firstPhotoModel != null) {
                    AsyncImage(
                        model = firstPhotoModel,
                        contentDescription = garment.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Photo",
                            fontSize = 11.sp,
                            color = Slate400,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Category pill - Top Left
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = garment.clothesType.ifBlank { "Garment" }.uppercase(Locale.ROOT),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Laundry / Wash State badge - Top Right
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (stats.needWash) BrandAmber.copy(alpha = 0.15f) else BrandEmerald.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (stats.needWash) BrandAmber else BrandEmerald
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (stats.needWash) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (stats.needWash) Color(0xFFB45309) else BrandEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (stats.needWash) "Need Wash" else "Clean",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.needWash) Color(0xFFB45309) else BrandEmerald
                        )
                    }
                }
            }

            // Card Body Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Garment Name
                Text(
                    text = garment.name.ifBlank { "Unnamed Garment" },
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Material & Thickness Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = garment.material.ifBlank { "Unspecified material" },
                        fontSize = 11.sp,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (garment.price > 0) {
                        Text(
                            text = String.format(Locale.US, "$%.2f", garment.price),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats Banner: Cost Per Wear & Total Washes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate100)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalLaundryService,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$totalWashedCount washes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                    }

                    if (costPerWear != null) {
                        Text(
                            text = "$costPerWear/wear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurple
                        )
                    }
                }

                // Dynamic Preset Attributes (visible fields chips)
                val extraFields = remember(preset.visibleFields) {
                    preset.visibleFields.filter {
                        it != "Name" && it != "Clothes Type" && it != "wash stated" && it != "price"
                    }
                }

                if (extraFields.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        extraFields.take(4).forEach { fieldKey ->
                            val value = DrilldownUtils.getFieldValue(garment, fieldKey, junctions)
                            if (value != "(Not specified)" && value.isNotBlank()) {
                                val fieldLabel = ALL_AVAILABLE_FIELDS.find { it.key == fieldKey }?.label ?: fieldKey
                                val isUndefined = value.equals("undefined", ignoreCase = true)
                                Surface(
                                    color = if (isUndefined) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "$fieldLabel: $value",
                                        fontSize = 10.sp,
                                        fontStyle = if (isUndefined) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                        color = if (isUndefined) Slate400 else Slate600,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
    }
}
