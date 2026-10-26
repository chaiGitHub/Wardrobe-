package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.util.CalendarCalculations
import com.example.util.GeminiAiService
import com.example.util.ImageUtils
import com.example.util.ThemeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JunctionAIModal(
    junction: JunctionEntity,
    garment: GarmentEntity?,
    report: ReportEntity?,
    onDismiss: () -> Unit,
    onSaveAiReport: ((title: String, content: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }
    var generatedAdvice by remember { mutableStateOf<String?>(null) }
    var hasCopied by remember { mutableStateOf(false) }
    val isAiPaused = remember(context) { GeminiAiService.isAiPaused(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("junction_ai_modal"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEDE9FE), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Junction Sensory Diagnosis",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Text(
                                text = "Wear Session AI Analysis & Care Fit",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_junction_modal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate500
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = Slate100
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Garment & Session Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            val photoModel = remember(garment?.pictures) {
                                ImageUtils.resolveFirstValidPhoto(garment?.pictures, context)?.let {
                                    ImageUtils.resolveImageModel(it, context)
                                }
                            }
                            if (photoModel != null) {
                                AsyncImage(
                                    model = photoModel,
                                    contentDescription = garment?.name,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                val seed = garment?.name ?: "Wear"
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(ImageUtils.getAvatarBackgroundColor(seed), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ImageUtils.getInitials(seed),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ImageUtils.getAvatarTextColor(seed)
                                        )
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = garment?.name ?: junction.clothesTitle.ifBlank { "Garment Session" },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${garment?.clothesType ?: "Apparel"} • ${garment?.material.orEmpty().ifBlank { "Fabric" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Duration badge
                                    val durLabel = CalendarCalculations.formatDuration(junction.duration)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Slate500,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = durLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = Slate700
                                                )
                                            )
                                        }
                                    }

                                    // Wash badge
                                    val washStyle = ThemeUtils.getWashStatusStyle(junction.washStated)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = washStyle.backgroundColor
                                    ) {
                                        Text(
                                            text = junction.washStated.ifBlank { "clean" },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = washStyle.textColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Occasion / Report Info
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Associated Wear Journal",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report?.name ?: junction.wearingReportTitle.ifBlank { "Daily Routine" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Logged Date: ${junction.wearDate.ifBlank { "Recent" }}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )

                            val reportTags = ImageUtils.parsePhotoList(report?.tags)
                            if (reportTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    reportTags.take(5).forEach { tag ->
                                        val style = ThemeUtils.getTagBadgeStyle(tag)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = style.backgroundColor
                                        ) {
                                            Text(
                                                text = tag.trim(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 10.sp,
                                                    color = style.textColor
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sensory Observation Matrix
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFAF5FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Sensory & Ergonomic Ratings",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPurple
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Midday Heat Feel",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                                    )
                                    Text(
                                        text = garment?.hotAtNoon.orEmpty().ifBlank { "Standard / Normal" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sitting Bust Ergonomics",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                                    )
                                    Text(
                                        text = garment?.sitBreast.orEmpty().ifBlank { "OK / Relaxed" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Strap Feel (Diao Dai)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                                    )
                                    Text(
                                        text = garment?.diaoDai.orEmpty().ifBlank { "Comfortable" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Fabric Contact Feel",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                                    )
                                    Text(
                                        text = garment?.sideBreastOk.orEmpty().ifBlank { "No friction" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                }
                            }
                        }
                    }

                    // AI Diagnosis Result Box
                    if (generatedAdvice != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFBFBFE),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandPurple)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gemini Sensory Analysis",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("AI Sensory Advice", generatedAdvice)
                                            clipboard.setPrimaryClip(clip)
                                            hasCopied = true
                                            Toast.makeText(context, "Copied analysis to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (hasCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = if (hasCopied) BrandEmerald else Slate500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = generatedAdvice ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Slate800,
                                        lineHeight = 19.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Button
                Button(
                    onClick = {
                        if (!isAnalyzing) {
                            coroutineScope.launch {
                                isAnalyzing = true
                                val gName = garment?.name ?: junction.clothesTitle
                                val gType = garment?.clothesType ?: "Garment"
                                val material = garment?.material.orEmpty()
                                val durHours = String.format("%.1f", junction.duration / 3600.0)
                                val sitBust = garment?.sitBreast.orEmpty().ifBlank { "standard" }
                                val heat = garment?.hotAtNoon.orEmpty().ifBlank { "moderate" }
                                val straps = garment?.diaoDai.orEmpty().ifBlank { "standard" }

                                val fallbackBuilder = {
                                    buildString {
                                        append("### 🔬 Sensory Session Evaluation: $gName ($gType)\n\n")
                                        append("**Wear Duration:** $durHours hours | **Wash State:** ${junction.washStated}\n\n")
                                        append("#### 1. Thermal & Moisture Equilibrium\n")
                                        if (heat.contains("Hot", ignoreCase = true)) {
                                            append("- Midday thermal retention was noted as elevated ($heat). Since the piece is made of $material, moisture trapping may decrease comfort in temperatures >26°C. Recommended for morning/evening or air-conditioned settings.\n")
                                        } else {
                                            append("- Excellent thermal neutrality maintained ($heat). Material ($material) provided suitable passive ventilation across the $durHours hr wearing window.\n")
                                        }
                                        append("\n#### 2. Ergonomics & Tactile Pressure\n")
                                        append("- **Bust/Chest:** Sitting posture evaluated as \"$sitBust\". No restrictive underwire pinch detected.\n")
                                        append("- **Shoulders/Straps:** $straps. Weight distribution along trapezius remains balanced.\n\n")
                                        append("#### 3. Wash Care & Maintenance Guidance\n")
                                        if (junction.washStated.equals("need wash", ignoreCase = true) || junction.duration > 14400) {
                                            append("- **Action Needed:** Garment reached $durHours hrs of contact wear. Wash on delicate cycle inside mesh bag to prevent friction wear on fabric fibers.\n")
                                        } else {
                                            append("- **Rotation Opportunity:** Light duration logged. May be aired out on cedar hanger for 24 hours prior to next wear.\n")
                                        }
                                    }
                                }

                                val aiResp = GeminiAiService.generateJunctionAnalysis(
                                    context = context,
                                    junction = junction,
                                    garment = garment,
                                    report = report,
                                    fallbackGenerator = fallbackBuilder
                                )
                                val analysis = aiResp.text

                                generatedAdvice = analysis
                                isAnalyzing = false
                                onSaveAiReport?.invoke("Sensory Diagnosis: $gName", analysis)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_junction_ai_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPurple,
                        contentColor = Color.White
                    ),
                    enabled = !isAnalyzing && !isAiPaused
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Running AI Sensory Diagnosis...")
                    } else if (isAiPaused) {
                        Text("⏸️ AI 诊断已暂停 (Zero Token Mode)")
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (generatedAdvice == null) "Run Gemini Sensory Diagnosis" else "Re-run Diagnosis")
                    }
                }
            }
        }
    }
}
