package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Guard / Confirmation dialog for washing a garment.
 * Matches the layout from the reference web application and screenshot:
 * - TopBar with Back arrow, "Wash \"$garmentName\"?", and Close 'X'
 * - Center droplet icon in blue squircle
 * - Title "Wash \"$garmentName\"?"
 * - Confirmation card with wear reset details and wash date banner
 * - Bottom action bar with "Cancel" and "Yes, Mark as Washed"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WashGuardDialog(
    garmentName: String,
    initialWashDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    onConfirm: (washDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedWashDate by remember(initialWashDate) {
        mutableStateOf(initialWashDate.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) })
    }

    val openDatePicker = {
        try {
            val clean = com.example.util.CalendarCalculations.normalizeDate(selectedWashDate)
            val cal = java.util.Calendar.getInstance()
            if (clean.length >= 10) {
                val parts = clean.substring(0, 10).split("-")
                if (parts.size == 3) {
                    val y = parts[0].toIntOrNull() ?: cal.get(java.util.Calendar.YEAR)
                    val m = (parts[1].toIntOrNull() ?: (cal.get(java.util.Calendar.MONTH) + 1)) - 1
                    val d = parts[2].toIntOrNull() ?: cal.get(java.util.Calendar.DAY_OF_MONTH)
                    cal.set(y, m, d)
                }
            }
            android.app.DatePickerDialog(
                context,
                { _, y, m, d ->
                    selectedWashDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("wash_guard_dialog"),
            containerColor = Color(0xFFFCFDFD),
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Wash \"$garmentName\"?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Slate700
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Slate500
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        )
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column {
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("wash_guard_cancel_btn"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Slate300),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Slate700
                                )
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700
                                )
                            }

                            Button(
                                onClick = { onConfirm(selectedWashDate) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("wash_guard_confirm_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1D4ED8),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Yes, Mark as Washed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large droplet icon in soft blue rounded container
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFDBEAFE), shape = RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Wash Droplet",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Heading: Wash "Hugo bra new 3"?
                Text(
                    text = "Wash \"$garmentName\"?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Confirmation Details Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // "Are you sure you want to mark \"Hugo bra new 3\" as washed?"
                        val annotatedText = buildAnnotatedString {
                            append("Are you sure you want to mark ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Slate900)) {
                                append("\"$garmentName\"")
                            }
                            append(" as washed?")
                        }
                        Text(
                            text = annotatedText,
                            fontSize = 15.sp,
                            color = Slate700,
                            lineHeight = 22.sp
                        )

                        // Wash date banner pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { openDatePicker() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Wash date: ",
                                        fontSize = 14.sp,
                                        color = Slate700
                                    )
                                    Text(
                                        text = selectedWashDate,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Explanatory note
                        Text(
                            text = "This resets unwashed wear hours for this garment and records this wash cycle. You can easily undo if clicked by mistake.",
                            fontSize = 13.sp,
                            color = Slate500,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}
