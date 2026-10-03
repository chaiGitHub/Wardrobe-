package com.example.ui.drilldown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
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

@Composable
fun DrillRecordRow(
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

    val firstPhotoModel = ImageUtils.resolveFirstValidPhoto(garment.pictures, context)?.let {
        ImageUtils.resolveImageModel(it, context)
    }

    val totalWashedCount = garment.wearingFrequency + garment.trackedAppWashes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("drill_row_${garment.id}")
            .background(Color.White)
            .clickable { onSelect(garment) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo Thumbnail (40x40)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                if (firstPhotoModel != null) {
                    AsyncImage(
                        model = firstPhotoModel,
                        contentDescription = garment.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Garment Title & Category / Material
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = garment.name.ifBlank { "Unnamed Garment" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = garment.clothesType.ifBlank { "Garment" },
                        fontSize = 11.sp,
                        color = Slate500,
                        fontWeight = FontWeight.Medium
                    )
                    if (garment.material.isNotBlank()) {
                        Text(text = "·", fontSize = 11.sp, color = Slate400)
                        Text(
                            text = garment.material,
                            fontSize = 11.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Wash Status Pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (stats.needWash) BrandAmber.copy(alpha = 0.15f) else BrandEmerald.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (stats.needWash) BrandAmber else BrandEmerald
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (stats.needWash) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (stats.needWash) Color(0xFFB45309) else BrandEmerald,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = if (stats.needWash) "Need Wash" else "Clean",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.needWash) Color(0xFFB45309) else BrandEmerald
                    )
                }
            }

            // Washes count
            Text(
                text = "${totalWashedCount}w",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700
            )

            // Price / Cost
            if (garment.price > 0) {
                Text(
                    text = String.format(Locale.US, "$%.0f", garment.price),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }

        HorizontalDivider(color = Slate100, thickness = 1.dp)
    }
}
