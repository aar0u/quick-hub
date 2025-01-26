package com.github.aar0u.quickhub.service

import com.github.aar0u.quickhub.controller.FileController
import com.github.aar0u.quickhub.controller.TextController
import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.util.NetworkUtils
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.ConsoleHandler
import java.util.logging.Filter
import java.util.logging.Logger

class HttpService(private val config: Config, private val listener: OnFileReceivedListener? = null) :
    NanoHTTPD(config.host, config.port), Loggable {
    private val textController = TextController()
    private val fileController = FileController(config)

    fun interface OnFileReceivedListener {
        fun onFileReceived(file: File)
    }

    private val routes =
        mapOf(
            "/" to { _ -> serveRoot() },
            "/text/list" to { _ -> textController.handleTextList() },
            "/text/add" to textController::handleTextAdd,
            "/file/list" to fileController::handleFileList,
            "/file/check" to fileController::handleFileCheck,
            "/file/add" to { session -> fileController.handleFileAdd(session, listener) },
        )

    init {
        configureNanoHTTPDLogger()
        File(config.workingDir).mkdirs()
    }

    override fun start() {
        try {
            NetworkUtils.getIpAddresses().forEach { (name, addresses) ->
                addresses.forEach { address ->
                    log.info("$name: http://$address:${config.port}")
                }
            }
            super.start()
            log.info("Server started on port ${config.port}")
        } catch (e: Exception) {
            stop()
            log.error("Server error: ${e.message}", e)
        }
    }

    override fun stop() {
        super.stop()
        while (super.isAlive()) {
            sleep(100)
        }
        log.info("Server stopped on port ${config.port}")
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
                // Then try file get handler
                ?: if (session.uri.startsWith("/file/get/")) {
                    fileController.handleFileRequest(session)
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
            ?: File("../static$path").takeIf { it.exists() }?.inputStream()

        if (inputStream == null) {
            log.info("Not found: {}{} in {}", config.staticDir, path, javaClass.getResource("/"))
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "404 Not Found",
            )
        }
        val contentType = Files.probeContentType(Paths.get(path)) ?: "application/octet-stream"

        return newFixedLengthResponse(
            Response.Status.OK,
            contentType,
            inputStream,
            inputStream.available().toLong(),
        )
    }

    private fun configureNanoHTTPDLogger() {
        val logger = Logger.getLogger(NanoHTTPD::class.java.name)
        // disable parent handlers
        logger.useParentHandlers = false
        // add a custom handler
        logger.addHandler(object : ConsoleHandler() {
            init {
                filter = Filter { record ->
                    log.warn("Ignored client interruption: ${record.thrown.message}")
                    record.thrown.message?.contains("Broken pipe") != true
                }
            }
        })
    }
}
