package com.example.melody.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.melody.domain.model.LyricLine
import com.example.melody.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import java.util.Locale

class MediaStoreScanner(private val context: Context) {

    companion object {
        // Umbral mínimo de duración (5 segundos) y tamaño (1 KB) para filtrar clips/sonidos cortos
        private const val MIN_DURATION_MS = 5000L
        private const val MIN_SIZE_BYTES = 1024L
        private val ALBUM_ART_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")
        private val LRC_LINE_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
    }

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val lrcMapByDirAndBaseName = scanMediaStoreLrcFiles()

        val collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val baseProjection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_NOTIFICATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            baseProjection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }

        val projection = baseProjection.toTypedArray()

        val selection = buildString {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            append(" AND ${MediaStore.Audio.Media.DURATION} >= ?")
            append(" AND ${MediaStore.Audio.Media.SIZE} > ?")
            append(" AND (${MediaStore.Audio.Media.IS_RINGTONE} = 0 OR ${MediaStore.Audio.Media.IS_RINGTONE} IS NULL)")
            append(" AND (${MediaStore.Audio.Media.IS_ALARM} = 0 OR ${MediaStore.Audio.Media.IS_ALARM} IS NULL)")
            append(" AND (${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 OR ${MediaStore.Audio.Media.IS_NOTIFICATION} IS NULL)")
        }

        val selectionArgs = arrayOf(
            MIN_DURATION_MS.toString(),
            MIN_SIZE_BYTES.toString()
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)?.trim()
                    val rawArtist = cursor.getString(artistCol)?.trim()
                    val rawAlbum = cursor.getString(albumCol)?.trim()
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val fileSize = cursor.getLong(sizeCol).coerceAtLeast(0L)

                    if (duration <= 0L) continue

                    val rawDataPath = if (dataCol >= 0) cursor.getString(dataCol)?.trim().orEmpty() else ""
                    val displayName = if (displayNameCol >= 0) cursor.getString(displayNameCol)?.trim().orEmpty() else ""
                    val relativePath = if (relativePathCol >= 0) cursor.getString(relativePathCol)?.trim().orEmpty() else ""

                    val (folderPath, folderName) = resolveFolderInfo(rawDataPath, relativePath)
                    val effectiveFilePath = when {
                        rawDataPath.isNotEmpty() -> rawDataPath
                        folderPath.isNotEmpty() && displayName.isNotEmpty() -> "$folderPath/$displayName"
                        else -> displayName
                    }

                    val title = if (rawTitle.isNullOrEmpty()) {
                        displayName.substringBeforeLast('.', "Canción desconocida")
                    } else {
                        rawTitle
                    }
                    val artist = if (rawArtist.isNullOrEmpty() || rawArtist == "<unknown>") {
                        "Artista desconocido"
                    } else {
                        rawArtist
                    }
                    val album = if (rawAlbum.isNullOrEmpty() || rawAlbum == "<unknown>") {
                        "Álbum desconocido"
                    } else {
                        rawAlbum
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId).toString()
                    } else {
                        null
                    }

                    // Detección de archivo .lrc local o letras embebidas (estilo Lark Player o.l05)
                    val lrcPath = findLrcForTrack(
                        filePath = rawDataPath,
                        folderPath = folderPath,
                        displayName = displayName,
                        title = title,
                        lrcLookup = lrcMapByDirAndBaseName
                    )
                    val hasEmbeddedLyrics = lrcPath == null && checkEmbeddedLyricsInFile(rawDataPath)
                    val hasLyrics = lrcPath != null || hasEmbeddedLyrics

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            filePath = effectiveFilePath,
                            folderName = folderName,
                            folderPath = folderPath,
                            fileSize = fileSize,
                            hasLyrics = hasLyrics,
                            lrcFilePath = lrcPath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songs
    }

    private fun resolveFolderInfo(dataPath: String, relativePath: String): Pair<String, String> {
        if (dataPath.isNotEmpty()) {
            val file = File(dataPath)
            val parent = file.parentFile
            if (parent != null) {
                val pPath = parent.absolutePath
                val pName = parent.name.ifEmpty { "Memoria interna" }
                return pPath to pName
            }
        }
        if (relativePath.isNotEmpty()) {
            val cleaned = relativePath.trimEnd('/', '\\')
            val name = cleaned.substringAfterLast('/', cleaned).ifEmpty { "Música" }
            return "/storage/emulated/0/$cleaned" to name
        }
        return "/storage/emulated/0/Music" to "Music"
    }

    private fun scanMediaStoreLrcFiles(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val filesUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )
            val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%.lrc", "%.lrc")

            context.contentResolver.query(
                filesUri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol)?.trim().orEmpty() else ""
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol)?.trim().orEmpty() else ""
                    val id = if (idCol >= 0) cursor.getLong(idCol) else -1L

                    val fileName = displayName.ifEmpty { File(dataPath).name }
                    if (!fileName.lowercase(Locale.ENGLISH).endsWith(".lrc")) continue

                    val baseName = fileName.substringBeforeLast('.').lowercase(Locale.ENGLISH)
                    val parentDir = if (dataPath.isNotEmpty()) {
                        File(dataPath).parent?.lowercase(Locale.ENGLISH).orEmpty()
                    } else {
                        ""
                    }

                    val targetRef = if (dataPath.isNotEmpty() && File(dataPath).exists()) {
                        dataPath
                    } else if (id > 0L) {
                        ContentUris.withAppendedId(filesUri, id).toString()
                    } else {
                        dataPath
                    }

                    if (targetRef.isNotEmpty()) {
                        if (parentDir.isNotEmpty()) {
                            result["$parentDir|$baseName"] = targetRef
                        }
                        result["*|$baseName"] = targetRef
                    }
                }
            }
        } catch (_: Exception) {
            // Ignorar si el proveedor no permite consultar MediaStore.Files
        }
        return result
    }

    private fun findLrcForTrack(
        filePath: String,
        folderPath: String,
        displayName: String,
        title: String,
        lrcLookup: Map<String, String>
    ): String? {
        val candidateBaseNames = buildList {
            if (filePath.isNotEmpty()) {
                add(File(filePath).nameWithoutExtension)
            }
            if (displayName.isNotEmpty()) {
                add(displayName.substringBeforeLast('.'))
            }
            if (title.isNotEmpty()) {
                add(title)
            }
        }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        // 1. Verificar archivos .lrc directamente en la misma carpeta (como Lark Player o.l05.g)
        if (folderPath.isNotEmpty()) {
            val dir = File(folderPath)
            if (dir.exists() && dir.isDirectory) {
                for (base in candidateBaseNames) {
                    val exactFile = File(dir, "$base.lrc")
                    if (exactFile.exists() && exactFile.isFile) {
                        return exactFile.absolutePath
                    }
                    val upperFile = File(dir, "$base.LRC")
                    if (upperFile.exists() && upperFile.isFile) {
                        return upperFile.absolutePath
                    }
                }
                // Búsqueda con filtro por prefijo insensible a mayúsculas
                val lrcFiles = dir.listFiles { _, name ->
                    name.lowercase(Locale.ENGLISH).endsWith(".lrc")
                }
                if (!lrcFiles.isNullOrEmpty()) {
                    for (base in candidateBaseNames) {
                        val lowerBase = base.lowercase(Locale.ENGLISH)
                        val match = lrcFiles.firstOrNull { candidate ->
                            val candidateName = candidate.nameWithoutExtension.lowercase(Locale.ENGLISH)
                            candidateName == lowerBase || candidateName.startsWith(lowerBase)
                        }
                        if (match != null) return match.absolutePath
                    }
                }
            }
        }

        // 2. Verificar en el índice consultado de MediaStore.Files
        val lowerFolder = folderPath.lowercase(Locale.ENGLISH)
        for (base in candidateBaseNames) {
            val lowerBase = base.lowercase(Locale.ENGLISH)
            lrcLookup["$lowerFolder|$lowerBase"]?.let { return it }
            lrcLookup["*|$lowerBase"]?.let { return it }
        }

        return null
    }

    /**
     * Inspecciona rápidamente la cabecera ID3v2 del archivo de audio para detectar
     * si contiene tramas de letras sincronizadas (SYLT) o no sincronizadas (USLT).
     */
    private fun checkEmbeddedLyricsInFile(filePath: String): Boolean {
        if (filePath.isEmpty()) return false
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() < 128L) return false
            file.inputStream().use { input ->
                val header = ByteArray(10)
                if (input.read(header) != 10) return false
                if (header[0] != 'I'.code.toByte() ||
                    header[1] != 'D'.code.toByte() ||
                    header[2] != '3'.code.toByte()
                ) {
                    return false
                }
                val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F)
                val bytesToInspect = tagSize.coerceIn(0, 64 * 1024)
                if (bytesToInspect <= 0) return false
                val tagBytes = ByteArray(bytesToInspect)
                val readCount = input.read(tagBytes)
                if (readCount <= 0) return false
                val ascii = String(tagBytes, 0, readCount, Charsets.ISO_8859_1)
                ascii.contains("USLT") || ascii.contains("SYLT")
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readLyricsForSong(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        val rawText = loadRawLyricsText(song) ?: return@withContext emptyList()
        parseLrcContent(rawText)
    }

    private fun loadRawLyricsText(song: Song): String? {
        val lrcRef = song.lrcFilePath
        if (!lrcRef.isNullOrBlank()) {
            try {
                if (lrcRef.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(lrcRef))?.use { stream ->
                        return stream.readBytes().toString(Charset.forName("UTF-8"))
                    }
                } else {
                    val file = File(lrcRef)
                    if (file.exists() && file.canRead()) {
                        return file.readText(Charsets.UTF_8)
                    }
                }
            } catch (_: Exception) {
                // Continuar al fallback
            }
        }

        // Intentar encontrar el archivo .lrc nuevamente en tiempo real si se agregó después
        if (song.folderPath.isNotEmpty()) {
            val found = findLrcForTrack(
                filePath = song.filePath,
                folderPath = song.folderPath,
                displayName = File(song.filePath).name,
                title = song.title,
                lrcLookup = emptyMap()
            )
            if (found != null) {
                try {
                    val file = File(found)
                    if (file.exists() && file.canRead()) {
                        return file.readText(Charsets.UTF_8)
                    }
                } catch (_: Exception) {
                }
            }
        }

        return null
    }

    private fun parseLrcContent(rawContent: String): List<LyricLine> {
        val timestampedLines = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<LyricLine>()

        rawContent.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            // Ignorar metadatos de cabecera LRC como [ti:...], [ar:...], [al:...], [by:...]
            if (line.startsWith("[") && line.endsWith("]") && !line.substring(1, line.length - 1).firstOrNull()
                    .let { it != null && it.isDigit() }
            ) {
                return@forEach
            }

            val match = LRC_LINE_REGEX.matchEntire(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionRaw = match.groupValues[3]
                val millis = when (fractionRaw.length) {
                    1 -> (fractionRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionRaw.toLongOrNull() ?: 0L) * 10L
                    3 -> fractionRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val text = match.groupValues[4].trim()
                val timeMs = (minutes * 60_000L) + (seconds * 1_000L) + millis
                if (text.isNotEmpty()) {
                    timestampedLines.add(LyricLine(timeMs = timeMs, text = text))
                }
            } else {
                val cleaned = line.replace(Regex("""\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?]"""), "").trim()
                if (cleaned.isNotEmpty()) {
                    plainLines.add(LyricLine(timeMs = null, text = cleaned))
                }
            }
        }

        return if (timestampedLines.isNotEmpty()) {
            timestampedLines.sortedBy { it.timeMs ?: 0L }
        } else {
            plainLines
        }
    }
}
