package com.github.aar0u.quickhub.controller

import com.github.aar0u.quickhub.model.ApiResponse
import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.model.FileInfo
import com.github.aar0u.quickhub.service.Loggable
import com.github.aar0u.quickhub.util.FileUtils
import io.javalin.http.Context
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.aar0u.quickhub.service.CallBackListener
import java.io.File
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime


class FileController(private val config: Config, private val listener: CallBackListener? = null) : Loggable,
    ControllerBase() {
    private val objectMapper = jacksonObjectMapper()

    fun handleFileList(ctx: Context) {
        val body = ctx.body()
        val dirname =
            if (body.isNotBlank()) objectMapper.readValue<Map<String, Any>>(body)["dirname"] as? String ?: "" else ""
        val fullPath = File(Paths.get(config.workingDir, dirname).normalize().toString())
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
            ctx.status(200).json(
                ApiResponse(
                    status = "failed",
                    message = "Error listing files",
                    data = mapOf(
                        "folder" to FileUtils.trimFromBeginning(fullPath.absolutePath, config.workingDir),
                        "files" to fileInfos,
                    ),
                )
            )
            return
        }

        fullPath.listFiles()?.filter { !it.name.startsWith(".") }
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })?.forEach { file ->
                fileInfos.add(
                    FileInfo(
                        name = file.name,
                        path = FileUtils.trimFromBeginning(file.absolutePath, config.workingDir),
                        type = if (file.isDirectory) "directory" else "file",
                        size = if (file.isDirectory) null else file.length(),
                        uploadTime = config.dateTimeFormatter?.format(
                            LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(file.lastModified()),
                                java.time.ZoneId.systemDefault(),
                            ),
                        ),
                    ),
                )
            }

        val response = ApiResponse(
            status = "success",
            message = "Files listed successfully",
            data = mapOf(
                "folder" to FileUtils.trimFromBeginning(fullPath.absolutePath, config.workingDir),
                "files" to fileInfos,
            ),
        )

        ctx.status(200).json(response)
        return
    }

    fun handleFileCheck(ctx: Context) {
        val body = ctx.body()
        val jsonObject = if (body.isNotBlank()) objectMapper.readValue<Map<String, Any>>(body) else emptyMap()
        val filename = jsonObject["filename"] as? String ?: ""
        val dirname = jsonObject["dirname"] as? String ?: ""

        if (filename.isBlank()) {
            ctx.status(400).json(
                ApiResponse(
                    status = "failed",
                    message = "No filename provided",
                )
            )
            return
        }

        val filePath = Paths.get(config.workingDir, dirname, filename).toString()
        if (!config.overwrite && File(filePath).exists()) {
            log.info(
                """
                File already exists:
                $filePath
                """.trimIndent(),
            )
            ctx.status(200).json(
                ApiResponse(
                    status = "failed",
                    message = "File already exists",
                )
            )
            return
        }

        ctx.status(200).json(
            ApiResponse(
                status = "success",
                message = "File can be uploaded",
            )
        )
        return
    }


    fun handleFileAdd(ctx: Context) {
        val metadataStr = ctx.header("x-file-metadata")
        val metadata = metadataStr?.let {
            runCatching {
                val decode = URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                objectMapper.readValue<Map<String, String>>(decode)
            }.getOrNull()
        } ?: run {
            log.warn("x-file-metadata is null or failed to decode")
            mutableMapOf()
        }

        val uploadDir = Paths.get(config.workingDir, metadata["dirname"] ?: "").toString()
        File(uploadDir).mkdirs()
        val filename = metadata["filename"] ?: ctx.uploadedFiles().firstOrNull()?.filename() ?: "uploaded"
        val targetFile = File(Paths.get(uploadDir, filename).toString())

        log.info("Upload started: ${targetFile.absolutePath}")

        val uploaded = ctx.uploadedFiles().firstOrNull()
        if (uploaded == null) {
            ctx.status(400).json(
                ApiResponse(
                    status = "failed",
                    message = "No file uploaded",
                )
            )
            return
        }

        try {
            uploaded.content().use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val stats = targetFile.length()
            val fileSizeFormatted = FileUtils.formatFileSize(stats)
            log.info(
                """
                Upload completed:
                - File: ${targetFile.absolutePath}
                - Size: $fileSizeFormatted (${String.format("%,d", stats)} bytes)
                - MIME type: ${Files.probeContentType(targetFile.toPath()) ?: MIME_STREAM}
            """.trimIndent()
            )
            listener?.onFileReceived(targetFile)
        } catch (e: Exception) {
            log.error("Failed to handle file", e)
            ctx.status(500).json(
                ApiResponse(
                    status = "failed",
                    message = "Failed to handle file: ${e.message}",
                )
            )
            return
        }

        ctx.status(200).json(
            ApiResponse(
                status = "success",
                message = "Files uploaded",
            )
        )
        return
    }

    fun handleFileRequest(ctx: Context) {
        val rawFilename = ctx.path().removePrefix("/file/")
        val filename = URLDecoder.decode(rawFilename, StandardCharsets.UTF_8.name())

        val file = File(Paths.get(config.workingDir, filename).toString())
        val rangeHeader = ctx.header("Range")
        log.info("Get file: ${file.name} ${rangeHeader?.let { "($it)" } ?: ""}")

        if (!file.exists()) {
            ctx.status(404).result("File not found")
            return
        }

        val mimeType = Files.probeContentType(file.toPath()) ?: MIME_STREAM
        ctx.contentType(mimeType)

        if (rangeHeader == null) {
            ctx.header(HEADER_CONTENT_LENGTH, file.length().toString())
            ctx.result(file.inputStream())
        } else {
            val ranges = rangeHeader.substringAfter("bytes=").split("-")
            val start = ranges[0].toLongOrNull() ?: 0
            val end = ranges.getOrNull(1)?.toLongOrNull()?.coerceAtMost(file.length() - 1) ?: (file.length() - 1)
            val chunkSize = (end - start) + 1
            ctx.status(206)
            ctx.header("Accept-Ranges", "bytes")
            ctx.header(HEADER_CONTENT_LENGTH, chunkSize.toString())
            ctx.header(HEADER_CONTENT_RANGE, "bytes $start-$end/${file.length()}")
            val maxChunkSize = 2 * 1024 * 1024L
            if (end == file.length() - 1) {
                val fis = file.inputStream()
                fis.channel.position(start)
                ctx.result(fis)
            } else if (end > start) {
                val actualChunk = chunkSize.coerceAtMost(maxChunkSize)
                val buffer = ByteArray(actualChunk.toInt())
                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(start)
                    raf.readFully(buffer)
                }
                ctx.result(buffer.inputStream())
            }
        }
        return
    }
}
