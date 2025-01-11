package com.github.aar0u.temphub.service

import com.github.aar0u.temphub.controller.FileController
import com.github.aar0u.temphub.controller.TextController
import com.github.aar0u.temphub.model.Config
import fi.iki.elonen.NanoHTTPD
import org.slf4j.LoggerFactory
import java.io.File

class HttpServiceNano(private val config: Config) : NanoHTTPD(config.host, config.port) {
    private val log = LoggerFactory.getLogger(HttpServiceNano::class.java)

    private val textController = TextController()
    private val fileController = FileController(config)

    private val routes =
        mapOf(
            "/" to { _ -> serveRoot() },
            "/text/list" to { _ -> textController.handleTextList() },
            "/text/add" to textController::handleTextAdd,
            "/files/list" to fileController::handleFileList,
            "/files/check" to fileController::handleFileCheck,
            "/files/add" to fileController::handleFileAdd,
        )

    init {
        File(config.workingDir).mkdirs()
    }

    override fun serve(session: IHTTPSession): Response {
        return try {
            // Ensure UTF-8 encoding for all incoming requests
            val contentType =
                session.headers["content-type"]?.let {
                    if (!it.contains("charset")) "$it; charset=UTF-8" else it
                } ?: "text/plain; charset=UTF-8"

            session.headers["content-type"] = contentType

            // First try exact matches
            routes[session.uri]?.invoke(session)
                // Then try file download handler
                ?: if (session.uri.startsWith("/files/download/")) {
                    fileController.handleFileDownload(session)
                } else {
                    serveStaticFile(session.uri)
                }
        } catch (e: Exception) {
            log.error("Error handling request: ${e.message}", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Internal server error: ${e.message}",
            )
        }
    }

    private fun serveRoot(): Response = serveStaticFile("/pad.html")

    private fun serveStaticFile(path: String): Response {
        // First, try to load from classpath (inside JAR)
        val inputStream = javaClass.getResourceAsStream("${config.staticDir}$path")

        if (inputStream == null) {
            log.info("Not found: {} in {}", path, javaClass.getResource(config.staticDir))
            // If not found in classpath, return 404
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "404 Not Found",
            )
        }
        val contentType =
            when {
                path.endsWith(".html") -> "text/html"
                path.endsWith(".css") -> "text/css"
                path.endsWith(".js") -> "application/javascript"
                path.endsWith(".json") -> "application/json"
                path.endsWith(".png") -> "image/png"
                path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
                path.endsWith(".gif") -> "image/gif"
                else -> "application/octet-stream"
            }

        return newFixedLengthResponse(
            Response.Status.OK,
            contentType,
            inputStream,
            inputStream.available().toLong(),
        )
    }
}
