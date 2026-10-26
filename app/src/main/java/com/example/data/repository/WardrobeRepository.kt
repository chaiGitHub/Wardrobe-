package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.WardrobeDatabase
import com.example.data.model.GarmentEntity
import com.example.data.model.JunctionEntity
import com.example.data.model.ReportEntity
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class WardrobeRepository(private val context: Context) {
    private val database = WardrobeDatabase.getDatabase(context)
    private val dao = database.wardrobeDao()

    val allClothes: Flow<List<GarmentEntity>> = dao.getAllClothes()
    val allReports: Flow<List<ReportEntity>> = dao.getAllReports()
    val allJunctions: Flow<List<JunctionEntity>> = dao.getAllJunctions()

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("wardrobe_seed_sync_prefs", Context.MODE_PRIVATE)
        val seedVersion = prefs.getInt("seed_imported_version", 0)
        val count = dao.getClothesCount()
        if (count == 0 || seedVersion < 6) {
            try {
                copySeedPhotosToDisk()
                context.assets.open("initial_seed.json").use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    val jsonStr = reader.readText()
                    importFromJsonString(jsonStr, clearExisting = false)
                }
                prefs.edit().putInt("seed_imported_version", 6).apply()
            } catch (e: Exception) {
                Log.e("WardrobeRepo", "Seed database error: ${e.message}", e)
            }
        }
    }

    private fun copySeedPhotosToDisk() {
        try {
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()
            copyAssetFolder("seed_photos", photosDir)
        } catch (e: Exception) {
            Log.e("WardrobeRepo", "Copy seed photos error: ${e.message}", e)
        }
    }

    private fun copyAssetFolder(assetPath: String, destDir: File) {
        val list = context.assets.list(assetPath) ?: return
        val photosDir = File(context.filesDir, "photos")
        for (item in list) {
            val subAsset = "$assetPath/$item"
            val subList = context.assets.list(subAsset)
            if (!subList.isNullOrEmpty()) {
                val subDest = File(destDir, item)
                if (!subDest.exists()) subDest.mkdirs()
                copyAssetFolder(subAsset, subDest)
            } else {
                val outFile = File(destDir, item)
                if (!outFile.exists() || outFile.length() == 0L) {
                    try {
                        context.assets.open(subAsset).use { input ->
                            FileOutputStream(outFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (_: Exception) {}
                }
                val flatFile = File(photosDir, item)
                if (!flatFile.exists() || flatFile.length() == 0L) {
                    try {
                        context.assets.open(subAsset).use { input ->
                            FileOutputStream(flatFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    suspend fun resetDatabaseToSeed(): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.assets.open("initial_seed.json").use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val jsonStr = reader.readText()
                importFromJsonString(jsonStr, clearExisting = true)
            }
            Result.success("Database restored to default seed collection (52 garments, 33 reports, 111 wear records).")
        } catch (e: Exception) {
            Log.e("WardrobeRepo", "Reset to seed error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun emptyDatabase(deletePhotos: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        try {
            dao.clearClothes()
            dao.clearReports()
            dao.clearJunctions()
            if (deletePhotos) {
                val photosDir = File(context.filesDir, "photos")
                if (photosDir.exists()) {
                    photosDir.deleteRecursively()
                }
            }
            Result.success("Entire local database emptied.")
        } catch (e: Exception) {
            Log.e("WardrobeRepo", "Empty database error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun insertGarment(garment: GarmentEntity) = dao.insertGarment(garment)
    suspend fun updateGarment(garment: GarmentEntity) = dao.updateGarment(garment)
    suspend fun deleteGarment(id: String) = dao.deleteGarment(id)

    suspend fun insertReport(report: ReportEntity) = dao.insertReport(report)
    suspend fun deleteReport(id: String) {
        dao.deleteReport(id)
        dao.deleteJunctionsForReport(id)
    }
    suspend fun deleteJunctionsForReport(reportId: String) = dao.deleteJunctionsForReport(reportId)

    suspend fun deduplicateReports(): Int = withContext(Dispatchers.IO) {
        val allReportsList = dao.getAllReportsSnapshot()
        val seenKeys = mutableSetOf<String>()
        var removedCount = 0
        for (report in allReportsList) {
            val key = "${report.wearDate.trim()}|${report.name.trim().lowercase(Locale.ROOT)}|${report.shoulderFeel.trim().lowercase(Locale.ROOT)}"
            if (seenKeys.contains(key)) {
                dao.deleteReport(report.id)
                dao.deleteJunctionsForReport(report.id)
                removedCount++
            } else {
                seenKeys.add(key)
            }
        }
        removedCount
    }

    private data class WashUndoGarmentSnapshot(
        val garment: GarmentEntity
    )

    private data class WashUndoJunctionSnapshot(
        val junction: JunctionEntity
    )

    private data class WashUndoEntry(
        val description: String,
        val garments: List<WashUndoGarmentSnapshot>,
        val junctions: List<WashUndoJunctionSnapshot>
    )

    private val washUndoStack = mutableListOf<WashUndoEntry>()

    suspend fun insertJunction(junction: JunctionEntity) = dao.insertJunction(junction)
    suspend fun updateJunction(junction: JunctionEntity) = dao.updateJunction(junction)
    suspend fun updateWashStatus(id: String, status: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dao.updateWashStatus(id, status, if (status == "wash" || status == "washed") today else "")
    }
    suspend fun deleteJunction(id: String) = dao.deleteJunction(id)

    suspend fun markGarmentWashed(garmentId: String, washDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())): String? = withContext(Dispatchers.IO) {
        val cleanDate = if (washDate.length >= 10) washDate.substring(0, 10) else washDate.ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val allJunctionsList = dao.getAllJunctionsSnapshot()
        val targetJunctions = allJunctionsList.filter {
            it.clothesId == garmentId && it.washStated.equals("need wash", ignoreCase = true)
        }
        val garment = dao.getGarmentById(garmentId)

        val junctionSnapshots = targetJunctions.map { WashUndoJunctionSnapshot(it) }
        val garmentSnapshots = if (garment != null) listOf(WashUndoGarmentSnapshot(garment)) else emptyList()

        for (j in targetJunctions) {
            dao.updateJunction(
                j.copy(
                    washStated = "wash",
                    washDate = cleanDate,
                    washedDate = cleanDate
                )
            )
        }

        if (garment != null) {
            val newTracked = garment.trackedAppWashes + 1
            dao.updateGarment(
                garment.copy(
                    trackedAppWashes = newTracked,
                    lastWashed = "${cleanDate}T00:00:00.000Z",
                    wet = "dry"
                )
            )
        }

        if (targetJunctions.isNotEmpty() || garment != null) {
            val gName = garment?.name ?: "Garment"
            val desc = "Washed \"$gName\" on $cleanDate"
            washUndoStack.add(WashUndoEntry(desc, garmentSnapshots, junctionSnapshots))
            if (washUndoStack.size > 20) washUndoStack.removeAt(0)
            desc
        } else {
            null
        }
    }

    suspend fun markAllLaundryWashed(washDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())): String? = withContext(Dispatchers.IO) {
        val cleanDate = if (washDate.length >= 10) washDate.substring(0, 10) else washDate.ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val allJunctionsList = dao.getAllJunctionsSnapshot()
        val dirtyJunctions = allJunctionsList.filter { it.washStated.equals("need wash", ignoreCase = true) }
        if (dirtyJunctions.isEmpty()) return@withContext null

        val dirtyGarmentIds = dirtyJunctions.map { it.clothesId }.toSet()
        val allClothesList = dao.getAllClothesSnapshot()
        val affectedGarments = allClothesList.filter { dirtyGarmentIds.contains(it.id) }

        val junctionSnapshots = dirtyJunctions.map { WashUndoJunctionSnapshot(it) }
        val garmentSnapshots = affectedGarments.map { WashUndoGarmentSnapshot(it) }

        for (j in dirtyJunctions) {
            dao.updateJunction(
                j.copy(
                    washStated = "wash",
                    washDate = cleanDate,
                    washedDate = cleanDate
                )
            )
        }

        for (g in affectedGarments) {
            dao.updateGarment(
                g.copy(
                    trackedAppWashes = g.trackedAppWashes + 1,
                    lastWashed = "${cleanDate}T00:00:00.000Z",
                    wet = "dry"
                )
            )
        }

        val desc = "Marked all ${dirtyGarmentIds.size} hamper items as washed on $cleanDate"
        washUndoStack.add(WashUndoEntry(desc, garmentSnapshots, junctionSnapshots))
        if (washUndoStack.size > 20) washUndoStack.removeAt(0)
        desc
    }

    suspend fun undoLastWash(): Boolean = withContext(Dispatchers.IO) {
        if (washUndoStack.isEmpty()) return@withContext false
        val lastEntry = washUndoStack.removeAt(washUndoStack.lastIndex)
        for (gSnap in lastEntry.garments) {
            dao.updateGarment(gSnap.garment)
        }
        for (jSnap in lastEntry.junctions) {
            dao.updateJunction(jSnap.junction)
        }
        true
    }

    suspend fun deduplicateJunctions(): Int = withContext(Dispatchers.IO) {
        val allJunctionsList = dao.getAllJunctionsSnapshot()
        val seenKeys = mutableSetOf<String>()
        var removedCount = 0
        for (j in allJunctionsList) {
            val key = "${j.wearingReportId}___${j.clothesId}"
            if (seenKeys.contains(key)) {
                dao.deleteJunction(j.id)
                removedCount++
            } else {
                seenKeys.add(key)
            }
        }
        removedCount
    }

    private fun resolveAndEnrichPicturesJson(
        rawPicturesArray: JSONArray?,
        fallbackSingleUrl: String = "",
        extraZipUris: List<String> = emptyList(),
        entityId: String = ""
    ): String {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        val resultArray = JSONArray()
        val seenResolved = mutableSetOf<String>()

        if (rawPicturesArray != null) {
            for (i in 0 until rawPicturesArray.length()) {
                val obj = rawPicturesArray.optJSONObject(i)
                if (obj != null) {
                    // Check if any field contains base64 data:image
                    val rawUrl = obj.optString("url", obj.optString("dataUrl", ""))
                    if (rawUrl.startsWith("data:image/") && rawUrl.contains("base64,")) {
                        val savedUri = saveBase64ImageToDisk(rawUrl, "${entityId}_$i", photosDir)
                        if (savedUri != null) {
                            obj.put("localUri", savedUri)
                            seenResolved.add(savedUri)
                        }
                    } else {
                        val cands = ImageUtils.parsePhotoList("[$obj]")
                        val resolved = cands.firstNotNullOfOrNull { ImageUtils.resolveImagePath(it, context) }
                        if (!resolved.isNullOrBlank()) {
                            obj.put("localUri", resolved)
                            seenResolved.add(resolved)
                        }
                    }
                    resultArray.put(obj)
                } else {
                    val str = rawPicturesArray.optString(i, "").trim()
                    if (str.isNotBlank() && str != "null") {
                        if (str.startsWith("data:image/") && str.contains("base64,")) {
                            val savedUri = saveBase64ImageToDisk(str, "${entityId}_$i", photosDir)
                            if (savedUri != null) {
                                resultArray.put(savedUri)
                                seenResolved.add(savedUri)
                            }
                        } else {
                            val resolved = ImageUtils.resolveImagePath(str, context)
                            if (!resolved.isNullOrBlank()) {
                                resultArray.put(resolved)
                                seenResolved.add(resolved)
                            } else {
                                resultArray.put(str)
                            }
                        }
                    }
                }
            }
        } else if (fallbackSingleUrl.isNotBlank()) {
            val resolved = ImageUtils.resolveImagePath(fallbackSingleUrl, context) ?: fallbackSingleUrl
            resultArray.put(resolved)
            seenResolved.add(resolved)
        }

        // Also attach any photos discovered inside garments/<id>/ or reports/<id>/ folders in the ZIP
        for (extraUri in extraZipUris) {
            if (extraUri.isNotBlank() && extraUri !in seenResolved) {
                resultArray.put(extraUri)
                seenResolved.add(extraUri)
            }
        }

        return resultArray.toString()
    }

    private fun saveBase64ImageToDisk(dataUrl: String, nameHint: String, photosDir: File): String? {
        return try {
            val base64Part = dataUrl.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
            if (bytes.isEmpty()) return null
            val ext = when {
                dataUrl.contains("image/png", ignoreCase = true) -> "png"
                dataUrl.contains("image/webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val safeName = nameHint.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val outFile = File(photosDir, "b64_${safeName}.$ext")
            FileOutputStream(outFile).use { it.write(bytes) }
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun importFromJsonString(
        jsonStr: String,
        clearExisting: Boolean,
        garmentZipPhotos: Map<String, List<String>> = emptyMap(),
        reportZipPhotos: Map<String, List<String>> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        val (clothesArray, reportsArray, junctionsArray) = if (jsonStr.trimStart().startsWith("[")) {
            val arr = try { JSONArray(jsonStr) } catch (_: Exception) { JSONArray() }
            Triple(arr, JSONArray(), JSONArray())
        } else {
            val root = try { JSONObject(jsonStr) } catch (_: Exception) { JSONObject() }
            val clothes = root.optJSONArray("clothes")
                ?: root.optJSONArray("garments")
                ?: root.optJSONArray("Clothes")
                ?: root.optJSONObject("data")?.optJSONArray("clothes")
                ?: root.optJSONObject("tables")?.optJSONArray("clothes")
                ?: JSONArray()
            val reports = root.optJSONArray("reports")
                ?: root.optJSONArray("wearingReports")
                ?: root.optJSONArray("wearing_reports")
                ?: root.optJSONArray("wearing_report")
                ?: root.optJSONObject("data")?.optJSONArray("reports")
                ?: root.optJSONObject("tables")?.optJSONArray("reports")
                ?: JSONArray()
            val junctions = root.optJSONArray("junctions")
                ?: root.optJSONArray("reportClothes")
                ?: root.optJSONArray("report_clothes_junctions")
                ?: root.optJSONArray("report_clothes")
                ?: root.optJSONObject("data")?.optJSONArray("junctions")
                ?: JSONArray()
            Triple(clothes, reports, junctions)
        }

        val parsedClothes = mutableListOf<GarmentEntity>()
        for (i in 0 until clothesArray.length()) {
            val rawObj = clothesArray.getJSONObject(i)
            val obj = rawObj.optJSONObject("fields") ?: rawObj
            val id = rawObj.optString("id", obj.optString("id", UUID.randomUUID().toString()))
            val name = obj.optString("name", obj.optString("Name", "Untitled Garment"))
            val type = obj.optString("clothesType", obj.optString("Clothes Type", ""))
            val material = obj.optString("material", obj.optString("materialProp", ""))
            val thick = obj.optString("thick clothes", obj.optString("thickClothes", ""))
            val hotNoon = obj.optString("Hot at noon", obj.optString("hotAtNoon", ""))
            val diaoDai = obj.optString("diao dai", obj.optString("diaoDai", ""))
            val sideOk = obj.optString("side breast ok", obj.optString("side breast shape", obj.optString("sideBreastOk", "")))
            val sideGather = obj.optString("sideBreastGatherBra", obj.optString("side breast ok: with 聚拢 bra", obj.optString("sidebreastgatherbra", sideOk)))
            val sideSideOut = obj.optString("sideBreastSideOutBra", obj.optString("side breast ok: with side out bra", obj.optString("sidebreastsideoutbra", obj.optString("sidebreastsifeoutbra", ""))))

            val aircondHot = obj.optString("aircondHotFeeling", obj.optString("when with aircond: hot feeling", ""))
            val aircondHotExp = obj.optString("aircondHotExplain", obj.optString("when with aircond: hot explanation", ""))
            val aircondLeg = obj.optString("aircondLegFeeling", obj.optString("when with aircond: leg feeling", ""))
            val aircondLegExp = obj.optString("aircondLegExplain", obj.optString("when with aircond: leg explanation", ""))

            val noAircondHot = obj.optString("noAircondHotFeeling", obj.optString("when without aircond: hot feeling", hotNoon))
            val noAircondHotExp = obj.optString("noAircondHotExplain", obj.optString("when without aircond: hot explanation", ""))
            val noAircondLeg = obj.optString("noAircondLegFeeling", obj.optString("when without aircond: leg feeling", ""))
            val noAircondLegExp = obj.optString("noAircondLegExplain", obj.optString("when without aircond: leg explanation", ""))

            val fanSleepHot = obj.optString("fanSleepHotFeeling", obj.optString("when sleep under fan: hot feeling", ""))
            val fanSleepHotExp = obj.optString("fanSleepHotExplain", obj.optString("when sleep under fan: hot explanation", ""))
            val fanSleepLeg = obj.optString("fanSleepLegFeeling", obj.optString("when sleep under fan: leg feeling", ""))
            val fanSleepLegExp = obj.optString("fanSleepLegExplain", obj.optString("when sleep under fan: leg explanation", ""))

            val standOk = obj.optString("center chest", obj.optString("stand breast", obj.optString("standBreast", obj.optString("stand breast ok", obj.optString("standBreastOk", "")))))
            val sitOk = obj.optString("sit breast ok", obj.optString("sit breast", obj.optString("sitBreast", "")))
            val sitGather = obj.optString("sitBreastGatherBra", obj.optString("sit breast ok: With 聚拢bra", obj.optString("sit breast ok: With 聚拢bra:", obj.optString("sit breast ok: with 聚拢 bra", obj.optString("sitbreastgatherbra", sitOk)))))
            val sitSideOut = obj.optString("sitBreastSideOutBra", obj.optString("sit breast ok: with side out bra", obj.optString("sitbreastsideoutbra", "")))
            val beautiful = obj.optString("beautiful", "")
            val beautifulReason = obj.optString("beatiful reason", obj.optString("beautiful reason", obj.optString("beautifulReason", "")))
            val wet = obj.optString("wet", "")
            val wetReason = obj.optString("wet reason", obj.optString("wetReason", ""))
            val descriptions = obj.optString("Descriptions", obj.optString("descriptions", ""))
            val descriptions2 = obj.optString("Descriptions 2", obj.optString("descriptions2", ""))
            val longDress = obj.optString("long dress", obj.optString("longDress", ""))
            val price = obj.optDouble("price", 0.0)
            val freq = obj.optDouble("wearing frequency(how many times legacy washed)", obj.optDouble("wearingFrequency", 0.0)).toInt()
            val appWashes = obj.optInt("Tracked App Washes", obj.optInt("trackedAppWashes", 0))
            val lastWashed = obj.optString("Last Washed", obj.optString("lastWashed", ""))
            val mayThrow = obj.optString("may throw", obj.optString("mayThrow", ""))
            val comment = obj.optString("last comment or action", obj.optString("lastComment", ""))
            val aiAnalysis = obj.optString("AI Clothes Analysis", obj.optString("aiClothesAnalysis", ""))
            val aiReports = obj.optJSONArray("aiReports")?.toString() ?: "[]"
            val rawPicsArr = obj.optJSONArray("Pictures")
                ?: obj.optJSONArray("pictures")
                ?: obj.optJSONArray("photos")
            val singlePicStr = obj.optString("imageUrl", obj.optString("photoUrl", ""))
            val pictures = resolveAndEnrichPicturesJson(
                rawPicturesArray = rawPicsArr,
                fallbackSingleUrl = singlePicStr,
                extraZipUris = garmentZipPhotos[id].orEmpty(),
                entityId = id
            )
            val tags = obj.optJSONArray("tags")?.toString() ?: "[]"
            val created = obj.optString("created", "")
            val customFieldsObj = obj.optJSONObject("customFields")
                ?: obj.optJSONObject("extraAttributes")
                ?: JSONObject()
            val feObj = obj.optJSONObject("fieldExplanations")
            if (feObj != null && !customFieldsObj.has("_fieldExplanations")) {
                customFieldsObj.put("_fieldExplanations", feObj)
            }
            val deletedFieldsArr = obj.optJSONArray("_deletedCustomFields")
            if (deletedFieldsArr != null && !customFieldsObj.has("_deletedCustomFields")) {
                customFieldsObj.put("_deletedCustomFields", deletedFieldsArr)
            }
            val customFields = customFieldsObj.toString()

            parsedClothes.add(
                GarmentEntity(
                    id = id,
                    name = name,
                    clothesType = type,
                    material = material,
                    thickClothes = thick,
                    hotAtNoon = hotNoon,
                    diaoDai = diaoDai,
                    sideBreastOk = sideOk,
                    sideBreastGatherBra = sideGather,
                    sideBreastSideOutBra = sideSideOut,
                    aircondHotFeeling = aircondHot,
                    aircondHotExplain = aircondHotExp,
                    aircondLegFeeling = aircondLeg,
                    aircondLegExplain = aircondLegExp,
                    noAircondHotFeeling = noAircondHot,
                    noAircondHotExplain = noAircondHotExp,
                    noAircondLegFeeling = noAircondLeg,
                    noAircondLegExplain = noAircondLegExp,
                    fanSleepHotFeeling = fanSleepHot,
                    fanSleepHotExplain = fanSleepHotExp,
                    fanSleepLegFeeling = fanSleepLeg,
                    fanSleepLegExplain = fanSleepLegExp,
                    standBreast = standOk,
                    sitBreast = sitOk,
                    sitBreastGatherBra = sitGather,
                    sitBreastSideOutBra = sitSideOut,
                    beautiful = beautiful,
                    beautifulReason = beautifulReason,
                    wet = wet,
                    wetReason = wetReason,
                    descriptions = descriptions,
                    descriptions2 = descriptions2,
                    longDress = longDress,
                    price = price,
                    wearingFrequency = freq,
                    trackedAppWashes = appWashes,
                    lastWashed = lastWashed,
                    mayThrow = mayThrow,
                    lastComment = comment,
                    aiClothesAnalysis = aiAnalysis,
                    aiReports = aiReports,
                    pictures = pictures,
                    tags = tags,
                    created = created,
                    customFields = customFields
                )
            )
        }

        val parsedReports = mutableListOf<ReportEntity>()
        for (i in 0 until reportsArray.length()) {
            val rawObj = reportsArray.getJSONObject(i)
            val obj = rawObj.optJSONObject("fields") ?: rawObj
            val id = rawObj.optString("id", obj.optString("id", UUID.randomUUID().toString()))
            val name = obj.optString("name", obj.optString("Name", "Wearing Report"))
            val wearDate = obj.optString("wearDate", obj.optString("wear date", ""))
            val created = obj.optString("created", "")
            val categories = obj.optJSONArray("categories")?.toString() ?: "[]"
            val shoulder = obj.optString("shoulderFeel", obj.optString("shoulder feel", ""))
            val midBreast = obj.optString("midBreast", obj.optString("mid breast", ""))
            val nightMidBreast = obj.optString("nightMidBreast", obj.optString("night mid breast", ""))
            val sweat = obj.optString("sweatProp", obj.optString("sweat", ""))
            val nightSweat = obj.optString("nightSweat", obj.optString("night sweat", ""))
            val leg = obj.optString("legFeel", obj.optString("leg feel", ""))
            val nightLeg = obj.optString("nightLegFeel", obj.optString("night leg feel", ""))
            val detail = obj.optString("reportDetail", obj.optString("report detail", ""))
            val aiWearingAnalysis = obj.optString("AI Wearing Analysis", obj.optString("aiWearingAnalysis", ""))
            val aiReports = obj.optJSONArray("aiReports")?.toString() ?: "[]"
            val customFieldsObj = obj.optJSONObject("customFields")
                ?: obj.optJSONObject("extraAttributes")
                ?: JSONObject()
            val feReportObj = obj.optJSONObject("fieldExplanations")
            if (feReportObj != null && !customFieldsObj.has("_fieldExplanations")) {
                customFieldsObj.put("_fieldExplanations", feReportObj)
            }
            val deletedReportFields = obj.optJSONArray("_deletedCustomFields")
            if (deletedReportFields != null && !customFieldsObj.has("_deletedCustomFields")) {
                customFieldsObj.put("_deletedCustomFields", deletedReportFields)
            }
            val customFields = customFieldsObj.toString()
            val rawReportPicsArr = obj.optJSONArray("Pictures")
                ?: obj.optJSONArray("pictures")
                ?: obj.optJSONArray("photos")
            val pictures = resolveAndEnrichPicturesJson(
                rawPicturesArray = rawReportPicsArr,
                extraZipUris = reportZipPhotos[id].orEmpty(),
                entityId = id
            )
            val tags = obj.optJSONArray("tags")?.toString() ?: "[]"

            parsedReports.add(
                ReportEntity(
                    id = id,
                    name = name,
                    wearDate = wearDate,
                    created = created,
                    categories = categories,
                    shoulderFeel = shoulder,
                    midBreast = midBreast,
                    nightMidBreast = nightMidBreast,
                    sweat = sweat,
                    nightSweat = nightSweat,
                    legFeel = leg,
                    nightLegFeel = nightLeg,
                    reportDetail = detail,
                    aiWearingAnalysis = aiWearingAnalysis,
                    aiReports = aiReports,
                    customFields = customFields,
                    pictures = pictures,
                    tags = tags
                )
            )
        }

        val parsedJunctions = mutableListOf<JunctionEntity>()
        for (i in 0 until junctionsArray.length()) {
            val rawObj = junctionsArray.getJSONObject(i)
            val obj = rawObj.optJSONObject("fields") ?: rawObj
            val id = rawObj.optString("id", obj.optString("id", UUID.randomUUID().toString()))
            val name = obj.optInt("name", i + 1)
            val reportObj = obj.optJSONObject("wearing report id")
            val reportId = obj.optString("wearingReportId", "").ifBlank {
                reportObj?.optString("id", "") ?: obj.optString("wearing report id", "")
            }
            val reportTitle = obj.optString("wearingReportTitle", "").ifBlank {
                reportObj?.optString("title", "") ?: ""
            }
            val clothesObj = obj.optJSONObject("clothes(new)")
            val clothesId = obj.optString("clothesId", "").ifBlank {
                clothesObj?.optString("id", "") ?: obj.optString("clothes(new)", "")
            }
            val clothesTitle = obj.optString("clothesTitle", "").ifBlank {
                clothesObj?.optString("title", "") ?: ""
            }
            val clothesType = obj.optString("clothesType", obj.optString("Clothes Type (from clothes(new))", ""))
            val duration = obj.optLong("durationVal", obj.optLong("duration", 0L))
            val washStated = obj.optString("washStated", obj.optString("wash stated", "need wash"))
            val washDate = obj.optString("washDate", obj.optString("wash date", ""))
            val washedDate = obj.optString("washedDate", obj.optString("washed date", ""))
            val wearDate = obj.optString("wearDate", obj.optString("wear date", ""))
            val analysis = obj.optString("AI Junction Analysis", "")

            parsedJunctions.add(
                JunctionEntity(
                    id = id,
                    name = name,
                    wearingReportId = reportId,
                    wearingReportTitle = reportTitle,
                    clothesId = clothesId,
                    clothesTitle = clothesTitle,
                    clothesType = clothesType,
                    duration = duration,
                    washStated = washStated,
                    washDate = washDate,
                    washedDate = washedDate,
                    wearDate = wearDate,
                    aiJunctionAnalysis = analysis
                )
            )
        }

        if (clearExisting) {
            dao.clearClothes()
            dao.clearReports()
            dao.clearJunctions()
        }

        dao.insertClothes(parsedClothes)
        dao.insertReports(parsedReports)
        dao.insertJunctions(parsedJunctions)
        Triple(parsedClothes.size, parsedReports.size, parsedJunctions.size)
    }

    suspend fun restoreFromZipUri(
        uri: Uri,
        clearExisting: Boolean = false,
        onProgress: (String, Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress("Opening backup archive...", 15)
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()

            var drilldownPresetsZipContent: String? = null
            var databaseJsonContent: String? = null
            var fallbackJsonContent: String? = null
            var photoCount = 0
            val garmentZipPhotos = mutableMapOf<String, MutableList<String>>()
            val reportZipPhotos = mutableMapOf<String, MutableList<String>>()

            val timestampPrefixRegex = Regex("^\\d{10,}[-_](.+)$")
            val garmentFolderRegex = Regex("(?:^|/)garments/([^/]+)/")
            val reportFolderRegex = Regex("(?:^|/)reports/([^/]+)/")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val rawName = entry.name
                        val cleanPath = rawName.trimStart('/').replace('\\', '/')
                        if (!entry.isDirectory &&
                            !cleanPath.startsWith("__MACOSX") &&
                            !cleanPath.endsWith(".DS_Store", ignoreCase = true) &&
                            !cleanPath.contains("..")
                        ) {
                            val lowerName = cleanPath.lowercase(Locale.ROOT)
                            if (lowerName == "drilldown_presets.json" || lowerName.endsWith("/drilldown_presets.json") ||
                                lowerName == "presets.json" || lowerName.endsWith("/presets.json") ||
                                lowerName == "report_presets.json" || lowerName.endsWith("/report_presets.json")
                            ) {
                                drilldownPresetsZipContent = zis.bufferedReader(Charsets.UTF_8).readText()
                            } else if (lowerName == "database.json" || lowerName.endsWith("/database.json") ||
                                lowerName == "initialdata.json" || lowerName.endsWith("/initialdata.json") ||
                                lowerName == "local_db.json" || lowerName.endsWith("/local_db.json") ||
                                lowerName == "initial_seed.json" || lowerName.endsWith("/initial_seed.json") ||
                                lowerName == "teable_dump.json" || lowerName.endsWith("/teable_dump.json") ||
                                lowerName == "backup.json" || lowerName.endsWith("/backup.json")
                            ) {
                                databaseJsonContent = zis.bufferedReader(Charsets.UTF_8).readText()
                            } else if (lowerName.endsWith(".json")) {
                                val text = zis.bufferedReader(Charsets.UTF_8).readText()
                                if (text.contains("\"clothes\"") || text.contains("\"garments\"") || text.contains("\"Clothes\"")) {
                                    fallbackJsonContent = text
                                }
                            } else {
                                val isLikelyPhoto = lowerName.contains("photos/") ||
                                    lowerName.contains("uploads/") ||
                                    lowerName.contains("garments/") ||
                                    lowerName.contains("reports/") ||
                                    lowerName.contains("images/") ||
                                    lowerName.contains("media/") ||
                                    lowerName.contains("table/") ||
                                    lowerName.endsWith(".jpg") ||
                                    lowerName.endsWith(".jpeg") ||
                                    lowerName.endsWith(".png") ||
                                    lowerName.endsWith(".webp") ||
                                    lowerName.endsWith(".gif") ||
                                    lowerName.endsWith(".bmp") ||
                                    lowerName.endsWith(".heic") ||
                                    lowerName.endsWith(".avif")

                                if (isLikelyPhoto) {
                                    val bytes = zis.readBytes()
                                    if (bytes.isNotEmpty()) {
                                        val fileName = File(cleanPath).name
                                        val primaryPhotoFile = File(photosDir, fileName)
                                        FileOutputStream(primaryPhotoFile).use { it.write(bytes) }

                                        // Also write stripped filename without <timestamp>_ prefix for direct lookup
                                        val strippedName = timestampPrefixRegex.find(fileName)?.groupValues?.getOrNull(1)
                                        if (!strippedName.isNullOrBlank() && strippedName != fileName) {
                                            val strippedFile = File(photosDir, strippedName)
                                            if (!strippedFile.exists()) {
                                                FileOutputStream(strippedFile).use { it.write(bytes) }
                                            }
                                        }

                                        // Also preserve relative path inside context.filesDir (e.g. uploads/garments/... or garments/...)
                                        try {
                                            val relFile = File(context.filesDir, cleanPath)
                                            if (relFile.canonicalPath.startsWith(context.filesDir.canonicalPath) &&
                                                relFile.canonicalPath != primaryPhotoFile.canonicalPath
                                            ) {
                                                relFile.parentFile?.mkdirs()
                                                FileOutputStream(relFile).use { it.write(bytes) }
                                            }
                                        } catch (_: Exception) {}

                                        val localFileUri = Uri.fromFile(primaryPhotoFile).toString()
                                        garmentFolderRegex.find(cleanPath)?.groupValues?.getOrNull(1)?.let { gId ->
                                            garmentZipPhotos.getOrPut(gId) { mutableListOf() }.add(localFileUri)
                                        }
                                        reportFolderRegex.find(cleanPath)?.groupValues?.getOrNull(1)?.let { rId ->
                                            reportZipPhotos.getOrPut(rId) { mutableListOf() }.add(localFileUri)
                                        }

                                        photoCount++
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val finalJson = databaseJsonContent ?: fallbackJsonContent
            if (finalJson == null) {
                return@withContext Result.failure(Exception("Invalid backup: missing database.json inside ZIP archive"))
            }

            // Extract, restore and save drilldownPresets from ZIP or database.json to both file and SharedPreferences
            try {
                var presetsArrayToSave: JSONArray? = null
                if (!drilldownPresetsZipContent.isNullOrBlank()) {
                    try {
                        presetsArrayToSave = JSONArray(drilldownPresetsZipContent)
                    } catch (_: Exception) {
                        try {
                            val obj = JSONObject(drilldownPresetsZipContent)
                            presetsArrayToSave = obj.optJSONArray("drilldownPresets") ?: obj.optJSONArray("presets")
                        } catch (_: Exception) {}
                    }
                }

                if (presetsArrayToSave == null || presetsArrayToSave.length() == 0) {
                    val rootObj = JSONObject(finalJson)
                    presetsArrayToSave = rootObj.optJSONArray("drilldownPresets")
                        ?: rootObj.optJSONArray("presets")
                        ?: rootObj.optJSONArray("drillPresets")
                }

                if (presetsArrayToSave != null && presetsArrayToSave.length() > 0) {
                    val presetsString = presetsArrayToSave.toString(2)
                    // 1. Write file
                    val presetsFile = File(context.filesDir, "drilldown_presets.json")
                    FileOutputStream(presetsFile).use { fos ->
                        fos.write(presetsString.toByteArray(Charsets.UTF_8))
                    }
                    // 2. Write to SharedPreferences so ReportPresetService picks it up instantly
                    val prefs = context.getSharedPreferences("wardrobe_drilldown_presets_v3", Context.MODE_PRIVATE)
                    prefs.edit().putString("custom_presets_json_v3", presetsArrayToSave.toString()).apply()
                    Log.i("WardrobeRepo", "Successfully restored ${presetsArrayToSave.length()} drilldown presets from backup into SharedPreferences!")
                }
            } catch (e: Exception) {
                Log.w("WardrobeRepo", "Drilldown presets restore notice: ${e.message}")
            }

            onProgress("Importing database records & linking $photoCount photos...", 60)
            val counts = importFromJsonString(
                jsonStr = finalJson,
                clearExisting = clearExisting,
                garmentZipPhotos = garmentZipPhotos,
                reportZipPhotos = reportZipPhotos
            )
            onProgress("Restore complete!", 100)

            val modeLabel = if (clearExisting) "Full Overwrite" else "Smart Merge"
            Result.success("Restored ($modeLabel): ${counts.first} garments, ${counts.second} journal reports, ${counts.third} outfit sessions, and $photoCount photos.")
        } catch (e: Exception) {
            Log.e("WardrobeRepo", "Restore ZIP failed", e)
            Result.failure(e)
        }
    }

    suspend fun createDatabaseJsonString(): String = withContext(Dispatchers.IO) {
        val clothesList = dao.getAllClothesSnapshot()
        val reportsList = dao.getAllReportsSnapshot()
        val junctionsList = dao.getAllJunctionsSnapshot()

        val root = JSONObject()
        root.put("version", "2.0")
        root.put("app", "Wardrobe & Wear Journal")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        root.put("totalClothes", clothesList.size)
        root.put("totalReports", reportsList.size)
        root.put("totalJunctions", junctionsList.size)

        // Read and embed drilldownPresets from SharedPreferences first, then file or asset fallback
        val presetsArr = try {
            val prefs = context.getSharedPreferences("wardrobe_drilldown_presets_v3", Context.MODE_PRIVATE)
            val spPresets = prefs.getString("custom_presets_json_v3", null)
            if (!spPresets.isNullOrBlank()) {
                JSONArray(spPresets)
            } else {
                val presetsFile = File(context.filesDir, "drilldown_presets.json")
                val raw = if (presetsFile.exists()) {
                    presetsFile.readText(Charsets.UTF_8)
                } else {
                    context.assets.open("drilldown_presets.json").bufferedReader().use { it.readText() }
                }
                JSONArray(raw)
            }
        } catch (_: Exception) {
            JSONArray()
        }
        root.put("totalDrilldownPresets", presetsArr.length())
        root.put("drilldownPresets", presetsArr)
        root.put("presets", presetsArr)

        val clothesArr = JSONArray()
        for (g in clothesList) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("Name", g.name)
            obj.put("Clothes Type", g.clothesType)
            obj.put("material", g.material)
            obj.put("thick clothes", g.thickClothes)
            obj.put("Hot at noon", if (g.noAircondHotFeeling.isNotBlank()) g.noAircondHotFeeling else g.hotAtNoon)
            obj.put("noAircondHotFeeling", g.noAircondHotFeeling)
            obj.put("noAircondHotExplain", g.noAircondHotExplain)
            obj.put("noAircondLegFeeling", g.noAircondLegFeeling)
            obj.put("noAircondLegExplain", g.noAircondLegExplain)
            obj.put("aircondHotFeeling", g.aircondHotFeeling)
            obj.put("aircondHotExplain", g.aircondHotExplain)
            obj.put("aircondLegFeeling", g.aircondLegFeeling)
            obj.put("aircondLegExplain", g.aircondLegExplain)
            obj.put("fanSleepHotFeeling", g.fanSleepHotFeeling)
            obj.put("fanSleepHotExplain", g.fanSleepHotExplain)
            obj.put("fanSleepLegFeeling", g.fanSleepLegFeeling)
            obj.put("fanSleepLegExplain", g.fanSleepLegExplain)
            obj.put("diao dai", g.diaoDai)
            obj.put("side breast ok", if (g.sideBreastGatherBra.isNotBlank()) g.sideBreastGatherBra else g.sideBreastOk)
            obj.put("sideBreastGatherBra", g.sideBreastGatherBra)
            obj.put("sideBreastSideOutBra", g.sideBreastSideOutBra)
            obj.put("stand breast", g.standBreast)
            obj.put("sit breast ok", if (g.sitBreastGatherBra.isNotBlank()) g.sitBreastGatherBra else g.sitBreast)
            obj.put("sitBreastGatherBra", g.sitBreastGatherBra)
            obj.put("sitBreastSideOutBra", g.sitBreastSideOutBra)
            obj.put("beautiful", g.beautiful)
            obj.put("wet", g.wet)
            obj.put("price", g.price)
            obj.put("wearing frequency(how many times legacy washed)", g.wearingFrequency)
            obj.put("Tracked App Washes", g.trackedAppWashes)
            obj.put("Last Washed", g.lastWashed)
            obj.put("may throw", g.mayThrow)
            obj.put("last comment or action", g.lastComment)
            try { obj.put("Pictures", JSONArray(g.pictures)) } catch (e: Exception) { obj.put("Pictures", JSONArray()) }
            try { obj.put("tags", JSONArray(g.tags)) } catch (e: Exception) { obj.put("tags", JSONArray()) }
            obj.put("created", g.created)
            clothesArr.put(obj)
        }
        root.put("clothes", clothesArr)

        val reportsArr = JSONArray()
        for (r in reportsList) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("wearDate", r.wearDate)
            obj.put("created", r.created)
            obj.put("shoulderFeel", r.shoulderFeel)
            obj.put("midBreast", r.midBreast)
            obj.put("nightMidBreast", r.nightMidBreast)
            obj.put("sweat", r.sweat)
            obj.put("nightSweat", r.nightSweat)
            obj.put("legFeel", r.legFeel)
            obj.put("nightLegFeel", r.nightLegFeel)
            obj.put("reportDetail", r.reportDetail)
            try { obj.put("Pictures", JSONArray(r.pictures)) } catch (e: Exception) { obj.put("Pictures", JSONArray()) }
            try { obj.put("tags", JSONArray(r.tags)) } catch (e: Exception) { obj.put("tags", JSONArray()) }
            reportsArr.put(obj)
        }
        root.put("reports", reportsArr)

        val junctionsArr = JSONArray()
        for (j in junctionsList) {
            val obj = JSONObject()
            obj.put("id", j.id)
            obj.put("name", j.name)
            obj.put("wearing report id", j.wearingReportId)
            obj.put("wearingReportTitle", j.wearingReportTitle)
            obj.put("clothes(new)", j.clothesId)
            obj.put("clothesTitle", j.clothesTitle)
            obj.put("clothesType", j.clothesType)
            obj.put("duration", j.duration)
            obj.put("wash stated", j.washStated)
            obj.put("wearDate", j.wearDate)
            obj.put("wash date", j.washedDate)
            junctionsArr.put(obj)
        }
        root.put("junctions", junctionsArr)
        root.put("reportClothes", junctionsArr)

        root.toString(2)
    }

    suspend fun exportFullBackupZip(destinationUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonContent = createDatabaseJsonString()

            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                ZipOutputStream(out).use { zos ->
                    // 1. Write database.json
                    zos.putNextEntry(ZipEntry("database.json"))
                    zos.write(jsonContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // 2. Also write drilldown_presets.json and presets.json at root
                    try {
                        val prefs = context.getSharedPreferences("wardrobe_drilldown_presets_v3", Context.MODE_PRIVATE)
                        val spPresets = prefs.getString("custom_presets_json_v3", null)
                        val presetsBytes = if (!spPresets.isNullOrBlank()) {
                            spPresets.toByteArray(Charsets.UTF_8)
                        } else {
                            val presetsFile = File(context.filesDir, "drilldown_presets.json")
                            if (presetsFile.exists()) presetsFile.readBytes() else {
                                context.assets.open("drilldown_presets.json").readBytes()
                            }
                        }
                        zos.putNextEntry(ZipEntry("drilldown_presets.json"))
                        zos.write(presetsBytes)
                        zos.closeEntry()

                        zos.putNextEntry(ZipEntry("presets.json"))
                        zos.write(presetsBytes)
                        zos.closeEntry()
                    } catch (_: Exception) {}

                    // 3. Pack photos
                    val photosDir = File(context.filesDir, "photos")
                    if (photosDir.exists() && photosDir.isDirectory) {
                        photosDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                zos.putNextEntry(ZipEntry("photos/${file.name}"))
                                file.inputStream().use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            Result.success("Full backup ZIP created and saved successfully.")
        } catch (e: Exception) {
            Log.e("WardrobeRepo", "Export ZIP failed", e)
            Result.failure(e)
        }
    }

    suspend fun exportDatabaseJson(destinationUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonContent = createDatabaseJsonString()
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
            }
            Result.success("initialData.json exported successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTempBackupZipFile(): File = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tempZip = File(cacheDir, "wardrobe_backup_$timeStamp.zip")

        val jsonContent = createDatabaseJsonString()
        FileOutputStream(tempZip).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zos.putNextEntry(ZipEntry("database.json"))
                zos.write(jsonContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Also write drilldown_presets.json and presets.json
                try {
                    val prefs = context.getSharedPreferences("wardrobe_drilldown_presets_v3", Context.MODE_PRIVATE)
                    val spPresets = prefs.getString("custom_presets_json_v3", null)
                    val presetsBytes = if (!spPresets.isNullOrBlank()) {
                        spPresets.toByteArray(Charsets.UTF_8)
                    } else {
                        val presetsFile = File(context.filesDir, "drilldown_presets.json")
                        if (presetsFile.exists()) presetsFile.readBytes() else {
                            context.assets.open("drilldown_presets.json").readBytes()
                        }
                    }
                    zos.putNextEntry(ZipEntry("drilldown_presets.json"))
                    zos.write(presetsBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("presets.json"))
                    zos.write(presetsBytes)
                    zos.closeEntry()
                } catch (_: Exception) {}

                val photosDir = File(context.filesDir, "photos")
                if (photosDir.exists() && photosDir.isDirectory) {
                    photosDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            zos.putNextEntry(ZipEntry("photos/${file.name}"))
                            file.inputStream().use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
        }
        tempZip
    }
}
