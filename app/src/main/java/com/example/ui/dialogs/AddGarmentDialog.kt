package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.GarmentEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddGarmentDialog(
    existingClothes: List<GarmentEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (GarmentEntity) -> Unit
) {
    val context = LocalContext.current

    // Basic fields
    var name by remember { mutableStateOf("") }
    var clothesType by remember { mutableStateOf("Dress") }
    var material by remember { mutableStateOf("soft chiffon") }
    var priceStr by remember { mutableStateOf("") }

    // Ergonomics & Attributes (from AddClothesModal.tsx)
    var thickClothes by remember { mutableStateOf("01 not thick clothes") }
    var hotAtNoon by remember { mutableStateOf("Not hot") }
    var diaoDai by remember { mutableStateOf("diao dai") }
    var sideBreast by remember { mutableStateOf("side breast shape ok") }
    var sitBreast by remember { mutableStateOf("sit breast shape ok") }
    var centerChest by remember { mutableStateOf("no center chest") }
    var beautiful by remember { mutableStateOf("Beautiful") }
    var beautifulReason by remember { mutableStateOf("") }

    // Screenshot focus fields: WET, WET REASON, LONG DRESS, DESCRIPTIONS, LAST COMMENT OR ACTION
    var wet by remember { mutableStateOf("dry") }
    var wetReason by remember { mutableStateOf("") }
    var longDress by remember { mutableStateOf("short dress") }
    var descriptions by remember { mutableStateOf("") }
    var lastComment by remember { mutableStateOf("") }

    // Uploaded photo URIs/strings
    val photoUrls = remember { mutableStateListOf<String>() }

    // Photo picker launcher (Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            photoUrls.add(uri.toString())
        }
    }

    // Active PreviousEntriesModal state: fieldName to onSelect callback
    var activePreviousPicker by remember {
        mutableStateOf<Pair<String, (String) -> Unit>?>(null)
    }

    // Helper to calculate previous values for a field from existing clothes
    fun getPreviousValuesForField(fieldKey: String): List<PreviousEntry> {
        val map = mutableMapOf<String, Int>()
        existingClothes.forEach { g ->
            val v = when (fieldKey) {
                "Clothes Type" -> g.clothesType
                "material" -> g.material
                "thick clothes" -> g.thickClothes
                "Hot at noon" -> g.hotAtNoon
                "diao dai" -> g.diaoDai
                "side breast ok" -> g.sideBreastOk
                "sit breast ok" -> g.sitBreast
                "center chest" -> g.standBreast
                "beautiful" -> g.beautiful
                "wet" -> g.wet
                "wet reason" -> ""
                "long dress" -> ""
                "Descriptions" -> g.lastComment
                "last comment or action" -> ""
                else -> ""
            }.trim()
            if (v.isNotBlank() && v != "(Not specified)") {
                map[v] = (map[v] ?: 0) + 1
            }
        }

        // Add standard default presets if map is empty
        when (fieldKey) {
            "wet" -> if (map.isEmpty()) listOf("dry", "wet", "quick-dry", "semi-dry").forEach { map[it] = 1 }
            "wet reason" -> if (map.isEmpty()) listOf("quick-drying fabric", "air hung dry", "dryer low heat", "cotton breath").forEach { map[it] = 1 }
            "long dress" -> if (map.isEmpty()) listOf("short dress", "long dress", "midi dress", "maxi dress", "knee length", "above knee").forEach { map[it] = 1 }
            "Descriptions" -> if (map.isEmpty()) listOf("Styling notes, fit details...", "irrate body, beast no beautiful, hard to wear, hot, throw", "comfortable daily wear", "silk smooth breathable").forEach { map[it] = 1 }
            "last comment or action" -> if (map.isEmpty()) listOf("Needs gentle cycle", "hand wash only", "hang dry immediately", "iron reverse").forEach { map[it] = 1 }
            "Clothes Type" -> if (map.isEmpty()) listOf("Dress", "Pant", "Tshirt", "bra", "skirt", "sweater", "jacket", "coat", "top", "blouse", "cardigan", "shorts", "Other").forEach { map[it] = 1 }
            "Hot at noon" -> if (map.isEmpty()) listOf("Not hot", "Very hot", "Warm", "Cool").forEach { map[it] = 1 }
            "diao dai" -> if (map.isEmpty()) listOf("diao dai", "not diao dai", "wide strap", "halter").forEach { map[it] = 1 }
            "side breast ok" -> if (map.isEmpty()) listOf("side breast shape ok", "friction at armpit", "no friction").forEach { map[it] = 1 }
            "sit breast ok" -> if (map.isEmpty()) listOf("sit breast shape ok", "tight when seated", "very loose").forEach { map[it] = 1 }
            "beautiful" -> if (map.isEmpty()) listOf("Beautiful", "not so beautiful", "Standard", "Casual").forEach { map[it] = 1 }
            "thick clothes" -> if (map.isEmpty()) listOf("01 not thick clothes", "02 medium", "03 thick clothes", "ultra-thin").forEach { map[it] = 1 }
        }

        return map.entries.map { PreviousEntry(it.key, it.value) }.sortedByDescending { it.count }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .testTag("add_garment_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (matching screenshot: Purple checkroom badge, "Add New Clothes", close 'x')
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = BrandPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Add New Clothes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 17.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Scrollable Form Container
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card 1: Basic Information
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFAF5FF),
                        border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "BASIC CLOTHES INFORMATION",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF581C87)
                            )

                            // Name
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "NAME *",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("e.g. 062 black knitted top", fontSize = 13.sp, color = Slate400) },
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
                            }

                            // Clothes Type with Previous
                            FieldWithPreviousButton(
                                label = "CLOTHES TYPE *",
                                value = clothesType,
                                placeholder = "e.g. Dress, Pant, Shoe, Tshirt, Bra...",
                                onValueChange = { clothesType = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "Clothes Type" to { clothesType = it }
                                }
                            )

                            // Material with Previous
                            FieldWithPreviousButton(
                                label = "MATERIAL",
                                value = material,
                                placeholder = "e.g. soft chiffon, silk",
                                onValueChange = { material = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "material" to { material = it }
                                }
                            )

                            // Price
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "PRICE ($)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                OutlinedTextField(
                                    value = priceStr,
                                    onValueChange = { priceStr = it },
                                    placeholder = { Text("e.g. 59.90", fontSize = 13.sp, color = Slate400) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            }
                        }
                    }

                    // Card 2: Attributes & Ergonomics (Matching screenshot layout)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "CLOTHES ATTRIBUTES & ERGONOMICS",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )

                            // Thick Clothes
                            FieldWithPreviousButton(
                                label = "THICK CLOTHES",
                                value = thickClothes,
                                placeholder = "01 not thick clothes",
                                onValueChange = { thickClothes = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "thick clothes" to { thickClothes = it }
                                }
                            )

                            // Hot at noon
                            FieldWithPreviousButton(
                                label = "HOT AT NOON",
                                value = hotAtNoon,
                                placeholder = "Not hot",
                                onValueChange = { hotAtNoon = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "Hot at noon" to { hotAtNoon = it }
                                }
                            )

                            // Diao dai
                            FieldWithPreviousButton(
                                label = "DIAO DAI",
                                value = diaoDai,
                                placeholder = "diao dai / not diao dai",
                                onValueChange = { diaoDai = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "diao dai" to { diaoDai = it }
                                }
                            )

                            // Side breast ok
                            FieldWithPreviousButton(
                                label = "SIDE BREAST OK",
                                value = sideBreast,
                                placeholder = "side breast shape ok",
                                onValueChange = { sideBreast = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "side breast ok" to { sideBreast = it }
                                }
                            )

                            // Sit breast ok
                            FieldWithPreviousButton(
                                label = "SIT BREAST OK",
                                value = sitBreast,
                                placeholder = "sit breast shape ok",
                                onValueChange = { sitBreast = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "sit breast ok" to { sitBreast = it }
                                }
                            )

                            // Center chest
                            FieldWithPreviousButton(
                                label = "CENTER CHEST",
                                value = centerChest,
                                placeholder = "no center chest",
                                onValueChange = { centerChest = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "center chest" to { centerChest = it }
                                }
                            )

                            // Beautiful
                            FieldWithPreviousButton(
                                label = "BEAUTIFUL",
                                value = beautiful,
                                placeholder = "Beautiful",
                                onValueChange = { beautiful = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "beautiful" to { beautiful = it }
                                }
                            )
                        }
                    }

                    // Card 3: Exact Screenshot Card (WET, WET REASON, LONG DRESS)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // WET (Dropdown / Picker with Previous)
                            DropdownFieldWithPrevious(
                                label = "WET",
                                selectedValue = wet,
                                options = listOf("dry", "wet", "quick-dry", "semi-dry"),
                                onValueChange = { wet = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "wet" to { wet = it }
                                }
                            )

                            // WET REASON
                            FieldWithPreviousButton(
                                label = "WET REASON",
                                value = wetReason,
                                placeholder = "quick-drying fabric",
                                onValueChange = { wetReason = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "wet reason" to { wetReason = it }
                                }
                            )

                            // LONG DRESS
                            FieldWithPreviousButton(
                                label = "LONG DRESS",
                                value = longDress,
                                placeholder = "short dress",
                                onValueChange = { longDress = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "long dress" to { longDress = it }
                                }
                            )
                        }
                    }

                    // Card 4: DESCRIPTIONS & LAST COMMENT OR ACTION (from screenshot)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // DESCRIPTIONS
                            FieldWithPreviousButton(
                                label = "DESCRIPTIONS",
                                value = descriptions,
                                placeholder = "Styling notes, fit details...",
                                onValueChange = { descriptions = it },
                                isMultiline = true,
                                onOpenPrevious = {
                                    activePreviousPicker = "Descriptions" to { descriptions = it }
                                }
                            )

                            // LAST COMMENT OR ACTION
                            FieldWithPreviousButton(
                                label = "LAST COMMENT OR ACTION",
                                value = lastComment,
                                placeholder = "e.g. Needs gentle cycle",
                                onValueChange = { lastComment = it },
                                onOpenPrevious = {
                                    activePreviousPicker = "last comment or action" to { lastComment = it }
                                }
                            )
                        }
                    }

                    // Card 5: CLOTHES PHOTOS (0) (Matching screenshot exactly)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header row with "CLOTHES PHOTOS (count)" and [Upload Photo] button
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
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = BrandPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CLOTHES PHOTOS (${photoUrls.size})",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                }

                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Upload Photo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Show attached thumbnails if any
                            if (photoUrls.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(photoUrls) { url ->
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                                        ) {
                                            AsyncImage(
                                                model = ImageUtils.resolveImagePath(url, context),
                                                contentDescription = "Clothes photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = { photoUrls.remove(url) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Purple Dashed Box for upload CTA (Matching screenshot)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFAF5FF))
                                    .border(
                                        BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { photoPickerLauncher.launch("image/*") }
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "No photos uploaded yet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF581C87)
                                    )
                                    Text(
                                        text = "Click here to attach photos from your camera or files",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9333EA)
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer (matching screenshot: Cancel on left, "Add to Wardrobe" purple on right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .border(BorderStroke(1.dp, Color(0xFFF1F5F9)))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val newId = "rec_clothes_${System.currentTimeMillis()}"
                            val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                            val createdDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

                            val photoJson = JSONArray(photoUrls).toString()

                            val combinedComments = listOfNotNull(
                                descriptions.trim().ifBlank { null },
                                lastComment.trim().ifBlank { null }?.let { "Action: $it" },
                                longDress.trim().ifBlank { null }?.let { "Length: $it" }
                            ).joinToString("; ")

                            val newGarment = GarmentEntity(
                                id = newId,
                                name = name.ifBlank { "Untitled $clothesType" },
                                clothesType = clothesType,
                                material = material,
                                thickClothes = thickClothes,
                                hotAtNoon = hotAtNoon,
                                diaoDai = diaoDai,
                                sideBreastOk = sideBreast,
                                standBreast = centerChest,
                                sitBreast = sitBreast,
                                beautiful = beautiful,
                                wet = wet,
                                price = parsedPrice,
                                wearingFrequency = 0,
                                trackedAppWashes = 0,
                                lastWashed = "",
                                mayThrow = "keep",
                                lastComment = combinedComments,
                                pictures = photoJson,
                                tags = "[]",
                                created = createdDate
                            )
                            onSave(newGarment)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = "Add to Wardrobe",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Previous Entries Modal
    activePreviousPicker?.let { (fieldKey, onValueSelected) ->
        val entries = remember(fieldKey) { getPreviousValuesForField(fieldKey) }
        PreviousEntriesModal(
            isOpen = true,
            fieldLabel = fieldKey,
            entries = entries,
            onSelect = { selectedValue ->
                onValueSelected(selectedValue)
                activePreviousPicker = null
            },
            onClose = { activePreviousPicker = null }
        )
    }
}

/**
 * Reusable Form Field matching screenshot with field label on left,
 * "⏱ Previous" pill button on right, and an outlined input field.
 */
@Composable
private fun FieldWithPreviousButton(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onOpenPrevious: () -> Unit,
    isMultiline: Boolean = false
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

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.clickable { onOpenPrevious() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Previous",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
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
            singleLine = !isMultiline,
            minLines = if (isMultiline) 3 else 1,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Slate200,
                focusedBorderColor = BrandPurple
            )
        )
    }
}

/**
 * Reusable Dropdown Field matching screenshot's "WET" row:
 * Label + "⏱ Previous" on top, Select box with chevron.
 */
@Composable
private fun DropdownFieldWithPrevious(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    onOpenPrevious: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.clickable { onOpenPrevious() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BrandPurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Previous",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate600
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedValue.ifBlank { "Select..." },
                        fontSize = 13.sp,
                        color = Slate900
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Slate600
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 13.sp) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
