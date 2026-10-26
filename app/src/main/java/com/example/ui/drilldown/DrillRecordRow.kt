package com.example.ui.drilldown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.util.ImageUtils

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

    val otherFields = remember(preset.visibleFields) {
        preset.visibleFields.filter { it != "Name" && it != "wash stated" }
    }

    Row(
        modifier = modifier
            .testTag("drill_row_${garment.id}")
            .background(Color.White)
            .clickable { onSelect(garment) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Thumbnail + Clothes Full Name (No truncation)
        Row(
            modifier = Modifier.width(260.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        contentScale = ContentScale.Fit,
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
            Column {
                Text(
                    text = garment.name.ifBlank { "Unnamed Garment" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    softWrap = false
                )
                Text(
                    text = garment.clothesType.ifBlank { "Garment" },
                    fontSize = 10.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Wash Status Pill
        Box(modifier = Modifier.width(110.dp)) {
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = if (stats.needWash) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (stats.needWash) Color(0xFFB45309) else BrandEmerald,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (stats.needWash) "Need Wash" else "Clean",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.needWash) Color(0xFFB45309) else BrandEmerald,
                        softWrap = false
                    )
                }
            }
        }

        // Dynamic columns based on visibleFields (Show full text, no truncation)
        otherFields.forEach { fieldKey ->
            val value = DrilldownUtils.getFieldValue(garment, fieldKey, junctions)
            val isUndefined = value.equals("undefined", ignoreCase = true)
            Box(modifier = Modifier.width(130.dp)) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontStyle = if (isUndefined) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                    color = if (isUndefined) Slate400 else Slate700,
                    softWrap = false
                )
            }
        }

        // Action Arrow
        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterEnd) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
