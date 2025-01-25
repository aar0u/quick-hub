package com.github.aar0u.quickhub.controller

import com.github.aar0u.quickhub.model.ApiResponse
import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.model.FileInfo
import com.github.aar0u.quickhub.service.HttpService
import com.github.aar0u.quickhub.service.Loggable
import com.github.aar0u.quickhub.util.FileUtils
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.getMimeTypeForFile
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

class FileController(private val config: Config) : Loggable {
    private val gson = GsonBuilder().create()

    fun handleFileList(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val map = mutableMapOf<String, String>()
        session.parseBody(map)
        val jsonData = map["postData"] ?: "{}"
        val jsonObject = gson.fromJson(jsonData, object : TypeToken<Map<String, String>>() {})
        val dirname = jsonObject["dirname"] ?: ""
        val fullPath = File(Paths.get(config.workingDir, dirname).toString())
        log.info("Listing {}", fullPath)

        val fileInfos = mutableListOf<FileInfo>()
        if (fullPath.absolutePath != config.workingDir) {
            fileInfos.add(
                FileInfo(
                    name = "..",
                    path = FileUtils.trimFromBeginning(fullPath.parent ?: config.workingDir, config.workingDir),
                    type = "directory",
                ),
            )
        }

        if (!fullPath.exists()) {
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                gson.toJson(
                    ApiResponse(
                        status = "failed",
                        message = "Error listing files",
                        data =
                        mapOf(
                            "folder" to FileUtils.trimFromBeginning(fullPath.absolutePath, config.workingDir),
                            "files" to fileInfos,
                        ),
                    ),
                ),
            )
        }

        fullPath.listFiles()?.filter { !it.name.startsWith(".") }?.forEach { file ->
            fileInfos.add(
                FileInfo(
                    name = file.name,
                    path = FileUtils.trimFromBeginning(file.absolutePath, config.workingDir),
                    type = if (file.isDirectory) "directory" else "file",
                    size = if (file.isDirectory) null else file.length(),
                    uploadTime =
                    config.dateTimeFormatter?.format(
                        LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(file.lastModified()),
                            java.time.ZoneId.systemDefault(),
                        ),
                    ),
                ),
            )
        }

        val response =
            ApiResponse(
                status = "success",
                message = "Files listed successfully",
                data =
                mapOf(
                    "folder" to FileUtils.trimFromBeginning(fullPath.absolutePath, config.workingDir),
                    "files" to fileInfos,
                ),
            )

        return newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            gson.toJson(response),
        )
    }

    fun handleFileCheck(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val map = mutableMapOf<String, String>()
        session.parseBody(map)
        val jsonData = map["postData"] ?: "{}"
        val jsonObject = gson.fromJson(jsonData, object : TypeToken<Map<String, String>>() {})
        val filename = jsonObject["filename"] ?: ""
        val dirname = jsonObject["dirname"] ?: ""

        if (filename.isBlank()) {
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST,
                "application/json",
                gson.toJson(
                    ApiResponse(
                        status = "failed",
                        message = "No filename provided",
                    ),
                ),
            )
        }

        val filePath = Paths.get(config.workingDir, dirname, filename).toString()
        if (!config.overwrite && File(filePath).exists()) {
            log.info(
                """
                File already exists:
                $filePath
                """.trimIndent(),
            )
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                gson.toJson(
                    ApiResponse(
                        status = "failed",
                        message = "File already exists",
                    ),
                ),
            )
        }

        return newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            gson.toJson(
                ApiResponse(
                    status = "success",
                    message = "File can be uploaded",
                ),
            ),
        )
    }

    fun handleFileAdd(
        session: NanoHTTPD.IHTTPSession,
        listener: HttpService.OnFileReceivedListener? = null,
    ): NanoHTTPD.Response {
        val metadata =
            session.headers["x-file-metadata"]?.let { metadataStr ->
                try {
                    gson.fromJson(metadataStr, object : TypeToken<Map<String, String>>() {})
                } catch (e: Exception) {
                    log.error("Failed to parse metadata", e)
                    mutableMapOf()
                }
            } ?: mutableMapOf()

        val uploadDir = Paths.get(config.workingDir, metadata["dirname"] ?: "").toString()
        File(uploadDir).mkdirs() // Ensure upload directory exists

        // Get filename from metadata or fallback to temp file name
        val filename = metadata["filename"]
        val targetFile = File(Paths.get(uploadDir, filename).toString())

        log.info("Upload started: ${targetFile.absolutePath}")

        val map = mutableMapOf<String, String>()
        session.parseBody(map)

        val tempFilePath = map["files"] ?: ""
        log.info("Temp file: {}", tempFilePath)
        val tempFile = File(tempFilePath)

        try {
            targetFile.parentFile?.mkdirs()
            tempFile.copyTo(targetFile, config.overwrite)

            val stats = targetFile.length()
            val fileSizeFormatted = FileUtils.formatFileSize(stats)
            log.info(
                """
                Upload completed:
                - File: ${targetFile.absolutePath}
                - Size: $fileSizeFormatted (${String.format("%,d", stats)} bytes)
                - MIME type: ${getMimeTypeForFile(filename)}
                """.trimIndent(),
            )

            listener?.onFileReceived(targetFile)
        } catch (e: Exception) {
            log.error("Failed to handle file", e)
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                "application/json",
                gson.toJson(
                    ApiResponse(
                        status = "failed",
                        message = "Failed to handle file: ${e.message}",
                    ),
                ),
            )
        }

        return newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "application/json",
            gson.toJson(
                ApiResponse(
                    status = "success",
                    message = "Files uploaded",
                ),
            ),
        )
    }

    fun handleFileDownload(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val filename = session.uri.removePrefix("/files/download/")
        val file = File(Paths.get(config.workingDir, filename).toString())

        if (!file.exists()) {
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.NOT_FOUND,
                NanoHTTPD.MIME_PLAINTEXT,
                "File not found",
            )
        }

        val rangeHeader = session.headers["range"]
        return if (rangeHeader != null) {
            log.info("Range started: ${file.absolutePath} $rangeHeader")
            handleRangeRequest(file, rangeHeader)
        } else {
            log.info("Download started: ${file.absolutePath}")
            newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                getMimeTypeForFile(file.name),
                FileInputStream(file),
                file.length(),
            )
        }
    }

    private fun handleRangeRequest(file: File, rangeHeader: String): NanoHTTPD.Response {
        val ranges = rangeHeader.substringAfter("bytes=").split("-")
        val start = ranges[0].toLongOrNull() ?: 0

        // Limit buffer to 8MB chunks
        val maxChunkSize = 8 * 1024 * 1024L
        val chunkEnd = start + maxChunkSize - 1
        val end = ranges.getOrNull(1)?.toLongOrNull()?.coerceAtMost(chunkEnd) ?: minOf(file.length() - 1, chunkEnd)

        if (start >= file.length() || end >= file.length() || start > end) {
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE,
                NanoHTTPD.MIME_PLAINTEXT,
                "Invalid range",
            )
        }

        val length = end - start + 1
        val buffer = ByteArray(length.toInt())
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            raf.readFully(buffer)
        }

        val mimeType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
        val response =
            newFixedLengthResponse(NanoHTTPD.Response.Status.PARTIAL_CONTENT, mimeType, buffer.inputStream(), length)
        response.addHeader("Content-Range", "bytes $start-$end/${file.length()}")
        response.addHeader("Content-Length", length.toString())
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }
}
