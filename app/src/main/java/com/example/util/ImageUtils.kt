package com.example.util

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

object ImageUtils {

    private val TIMESTAMP_PREFIX_REGEX = Regex("^\\d{10,}[-_](.+)$")
    private val NORMALIZE_NAME_REGEX = Regex("[^a-zA-Z0-9._-]")

    /**
     * Resolves an image URL, JSON object string, or relative path into an accessible URI/URL string.
     * Returns null if a local relative path does not exist on disk or if an S3 URL is expired.
     */
    fun resolveImagePath(rawPath: String?, context: Context? = null): String? {
        if (rawPath.isNullOrBlank()) return null

        val trimmed = rawPath.trim()

        // If rawPath contains multiple pipe-delimited candidates from a JSONObject Picture entry,
        // try resolving a local file from ANY candidate first, then fall back to valid remote URLs.
        if (trimmed.contains("||")) {
            val candidates = trimmed.split("||").map { it.trim() }.filter { it.isNotBlank() }
            if (context != null) {
                for (cand in candidates) {
                    val localMatch = findLocalFileUri(cand, context)
                    if (localMatch != null) return localMatch
                }
            }
            for (cand in candidates) {
                if (isValidRemoteOrContentUri(cand)) return cand
            }
            return null
        }

        // If rawPath is accidentally a serialized JSONObject string, extract candidates and resolve
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val extracted = extractCandidatesFromJsonObjectString(trimmed)
            if (extracted.isNotEmpty()) {
                return resolveImagePath(extracted.joinToString("||"), context)
            }
            return null
        }

        // Direct file://, content://, or data:image/ URI
        if (trimmed.startsWith("content://") || trimmed.startsWith("data:image/")) {
            return trimmed
        }
        if (trimmed.startsWith("file://")) {
            val pathPart = Uri.parse(trimmed).path
            if (pathPart != null) {
                val f = File(pathPart)
                if (f.exists() && f.isFile && f.length() > 0) {
                    return Uri.fromFile(f).toString()
                }
            }
            // If file:// path had raw spaces or wasn't found via Uri.parse, try local lookup
            if (context != null) {
                val fallback = findLocalFileUri(trimmed.removePrefix("file://"), context)
                if (fallback != null) return fallback
            }
            return null
        }

        // Always check if we have a local file on disk first (even if rawPath is a URL whose filename is in photos/)
        if (context != null) {
            val localMatch = findLocalFileUri(trimmed, context)
            if (localMatch != null) return localMatch
        }

        // If it's a valid non-expired remote URL
        if (isValidRemoteOrContentUri(trimmed)) {
            return trimmed
        }

        return null
    }

    fun resolvePhotoModel(context: Context?, rawPath: String?): Any? {
        return resolveImageModel(rawPath, context)
    }

    /**
     * Resolves an image path/URL/JSON candidate into the optimal model for Coil's `AsyncImage`.
     * For local files on disk, returns a `java.io.File` directly so filenames with spaces,
     * parentheses, or Unicode characters load reliably without URI parsing ambiguity.
     */
    fun resolveImageModel(rawPath: String?, context: Context? = null): Any? {
        val resolved = resolveImagePath(rawPath, context) ?: return null
        if (resolved.startsWith("file://")) {
            val pathPart = Uri.parse(resolved).path
            if (!pathPart.isNullOrBlank()) {
                val f = File(pathPart)
                if (f.exists() && f.isFile && f.length() > 0) return f
            }
            val rawFile = File(resolved.removePrefix("file://"))
            if (rawFile.exists() && rawFile.isFile && rawFile.length() > 0) return rawFile
        } else if (resolved.startsWith("/data/") || resolved.startsWith("/storage/")) {
            val f = File(resolved)
            if (f.exists() && f.isFile && f.length() > 0) return f
        }
        return resolved
    }

    /**
     * Convenience helper that parses a garment/report `pictures` JSON string and returns
     * the first valid, displayable photo URI/URL (or null if none exist on disk).
     */
    fun resolveFirstValidPhoto(photoJson: String?, context: Context?): String? {
        val list = parsePhotoList(photoJson)
        for (item in list) {
            val resolved = resolveImagePath(item, context)
            if (!resolved.isNullOrBlank()) return resolved
        }
        return null
    }

    /**
     * Convenience helper that returns all valid, displayable photo URIs/URLs for a `pictures` JSON string.
     */
    fun resolveAllValidPhotos(photoJson: String?, context: Context?): List<String> {
        return parsePhotoList(photoJson).mapNotNull { resolveImagePath(it, context) }.distinct()
    }

    private fun isValidRemoteOrContentUri(candidate: String): Boolean {
        if (candidate.startsWith("content://") || candidate.startsWith("data:image/")) return true
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            val lower = candidate.lowercase()
            // Teable / S3 presigned URLs from old backups are expired
            if (lower.contains("amazonaws.com") || lower.contains("x-amz-expires") || lower.contains("expired")) {
                return false
            }
            return true
        }
        return false
    }

    private fun findLocalFileUri(candidate: String, context: Context): String? {
        val clean = candidate
            .substringBefore("?")
            .substringBefore("#")
            .trim()
        if (clean.isBlank() || clean.startsWith("data:")) return null

        // If candidate is an absolute path on device
        if (clean.startsWith("/data/") || clean.startsWith("/storage/")) {
            val absFile = File(clean)
            if (absFile.exists() && absFile.isFile && absFile.length() > 0) {
                return Uri.fromFile(absFile).toString()
            }
        }

        val relPath = clean
            .removePrefix("file://")
            .trimStart('/')
        val decodedRelPath = try { Uri.decode(relPath) } catch (_: Exception) { relPath }

        val filesDir = context.filesDir
        val photosDir = File(filesDir, "photos")

        // 1. Check exact relative path variations inside filesDir and cacheDir
        val directCandidates = listOf(
            File(filesDir, relPath),
            File(filesDir, decodedRelPath),
            File(filesDir, relPath.removePrefix("uploads/")),
            File(filesDir, decodedRelPath.removePrefix("uploads/")),
            File(filesDir, "uploads/$relPath"),
            File(filesDir, "photos/$relPath"),
            File(context.cacheDir, relPath)
        )
        for (f in directCandidates) {
            if (f.exists() && f.isFile && f.length() > 0) {
                return Uri.fromFile(f).toString()
            }
        }

        // 2. Check filename variations inside filesDir/photos
        val rawFileName = File(relPath).name
        val decodedFileName = File(decodedRelPath).name
        if (rawFileName.isBlank()) return null

        val strippedRaw = TIMESTAMP_PREFIX_REGEX.find(rawFileName)?.groupValues?.getOrNull(1)
        val strippedDecoded = TIMESTAMP_PREFIX_REGEX.find(decodedFileName)?.groupValues?.getOrNull(1)
        val sanitizedRaw = rawFileName.replace(NORMALIZE_NAME_REGEX, "_")
        val sanitizedDecoded = decodedFileName.replace(NORMALIZE_NAME_REGEX, "_")

        val nameCandidates = listOfNotNull(
            rawFileName,
            decodedFileName,
            strippedRaw,
            strippedDecoded,
            sanitizedRaw,
            sanitizedDecoded
        ).distinct()

        for (nameCand in nameCandidates) {
            val f = File(photosDir, nameCand)
            if (f.exists() && f.isFile && f.length() > 0) {
                return Uri.fromFile(f).toString()
            }
        }

        // 3. Scan photosDir for suffix / timestamp-prefixed / case-insensitive / normalized matches
        if (photosDir.exists() && photosDir.isDirectory) {
            val allPhotos = photosDir.listFiles() ?: emptyArray()
            val targetNamesLower = nameCandidates.map { it.lowercase() }.toSet()
            val targetNormalizedLower = nameCandidates.map {
                it.replace(NORMALIZE_NAME_REGEX, "_").lowercase()
            }.toSet()

            for (file in allPhotos) {
                if (!file.isFile || file.length() == 0L) continue
                val fName = file.name
                val fLower = fName.lowercase()
                val fStrippedLower = (TIMESTAMP_PREFIX_REGEX.find(fName)?.groupValues?.getOrNull(1) ?: fName).lowercase()
                val fNormLower = fName.replace(NORMALIZE_NAME_REGEX, "_").lowercase()
                val fStrippedNormLower = fStrippedLower.replace(NORMALIZE_NAME_REGEX, "_").lowercase()

                if (fLower in targetNamesLower ||
                    fStrippedLower in targetNamesLower ||
                    fNormLower in targetNormalizedLower ||
                    fStrippedNormLower in targetNormalizedLower ||
                    targetNamesLower.any { t ->
                        t.length >= 4 && (
                            fLower.endsWith("_$t") ||
                            fLower.endsWith("-$t") ||
                            fNormLower.endsWith("_$t") ||
                            fNormLower.endsWith("-$t")
                        )
                    }
                ) {
                    return Uri.fromFile(file).toString()
                }
            }
        }

        return null
    }

    /**
     * Copies a user-selected content:// URI into internal storage `filesDir/photos/`
     * so it persists across app restarts and is included in ZIP backups.
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri, prefix: String = "photo"): String {
        return try {
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mimeType.contains("png", ignoreCase = true) -> "png"
                mimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val safePrefix = prefix.replace(NORMALIZE_NAME_REGEX, "_").take(24).ifBlank { "photo" }
            val destFile = File(photosDir, "${System.currentTimeMillis()}_${safePrefix}.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                Uri.fromFile(destFile).toString()
            } else {
                uri.toString()
            }
        } catch (_: Exception) {
            uri.toString()
        }
    }

    /**
     * Extracts initials from a garment or user title (e.g. "White Linen Shirt" -> "WL", "Chiffon Dress" -> "CD").
     */
    fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "WW"
        val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
            words.isNotEmpty() && words[0].length >= 2 -> words[0].substring(0, 2).uppercase()
            words.isNotEmpty() -> words[0].uppercase()
            else -> "WW"
        }
    }

    /**
     * Generates a stable pleasant background color for an avatar/placeholder based on text hash.
     */
    fun getAvatarBackgroundColor(seedText: String?): Color {
        val palette = listOf(
            Color(0xFFEDE9FE), // Soft purple
            Color(0xFFE0E7FF), // Soft indigo
            Color(0xFFDBEAFE), // Soft blue
            Color(0xFFE0F2FE), // Soft sky
            Color(0xFFCCFBF1), // Soft teal
            Color(0xFFD1FAE5), // Soft emerald
            Color(0xFFFEF3C7), // Soft amber
            Color(0xFFFFEDD5), // Soft orange
            Color(0xFFFCE7F3), // Soft pink
            Color(0xFFF3E8FF)  // Soft fuchsia
        )
        if (seedText.isNullOrBlank()) return palette[0]
        val hash = abs(seedText.hashCode())
        return palette[hash % palette.size]
    }

    /**
     * Returns a contrasting dark text color matching the avatar background color.
     */
    fun getAvatarTextColor(seedText: String?): Color {
        val textPalette = listOf(
            Color(0xFF6D28D9), // Purple 700
            Color(0xFF4338CA), // Indigo 700
            Color(0xFF1D4ED8), // Blue 700
            Color(0xFF0369A1), // Sky 700
            Color(0xFF0F766E), // Teal 700
            Color(0xFF047857), // Emerald 700
            Color(0xFFB45309), // Amber 700
            Color(0xFFC2410C), // Orange 700
            Color(0xFFBE185D), // Pink 700
            Color(0xFF7E22CE)  // Fuchsia 700
        )
        if (seedText.isNullOrBlank()) return textPalette[0]
        val hash = abs(seedText.hashCode())
        return textPalette[hash % textPalette.size]
    }

    private fun extractCandidatesFromJsonObject(obj: JSONObject): List<String> {
        val candidates = mutableListOf<String>()
        val keysInPriorityOrder = listOf(
            "localUri",
            "resolvedPath",
            "storagePath",
            "path",
            "filename",
            "name",
            "url",
            "originalUrl",
            "lgThumbnailUrl",
            "smThumbnailUrl",
            "presignedUrl",
            "dataUrl",
            "id"
        )
        for (key in keysInPriorityOrder) {
            val v = obj.optString(key, "").trim()
            if (v.isNotBlank() && v != "null" && !v.startsWith("blob:")) {
                candidates.add(v)
            }
        }
        return candidates.distinct()
    }

    private fun extractCandidatesFromJsonObjectString(jsonObjStr: String): List<String> {
        return try {
            extractCandidatesFromJsonObject(JSONObject(jsonObjStr))
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Parses JSON array string (of strings OR JSONObjects) or single path into a list of
     * candidate descriptor strings that `resolveImagePath` can resolve.
     */
    fun parsePhotoList(photoJson: String?): List<String> {
        if (photoJson.isNullOrBlank()) return emptyList()
        val trimmed = photoJson.trim()
        if (trimmed == "[]" || trimmed == "{}") return emptyList()

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return try {
                val array = JSONArray(trimmed)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        val cands = extractCandidatesFromJsonObject(obj)
                        if (cands.isNotEmpty()) {
                            list.add(cands.joinToString("||"))
                        }
                    } else {
                        val str = array.optString(i, "").trim()
                        if (str.isNotBlank() && str != "null") {
                            if (str.startsWith("{") && str.endsWith("}")) {
                                val cands = extractCandidatesFromJsonObjectString(str)
                                if (cands.isNotEmpty()) list.add(cands.joinToString("||"))
                            } else {
                                list.add(str)
                            }
                        }
                    }
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val cands = extractCandidatesFromJsonObjectString(trimmed)
            return if (cands.isNotEmpty()) listOf(cands.joinToString("||")) else emptyList()
        }

        return listOf(trimmed)
    }
}
