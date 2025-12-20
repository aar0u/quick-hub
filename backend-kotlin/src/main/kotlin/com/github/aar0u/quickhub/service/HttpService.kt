package com.github.aar0u.quickhub.service

import com.github.aar0u.quickhub.controller.FileController
import com.github.aar0u.quickhub.controller.TextController
import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.util.NetworkUtils
import fi.iki.elonen.NanoHTTPD
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Paths

class HttpService(private val config: Config, private val listener: CallBackListener? = null) :
    NanoHTTPD(config.host, config.port) {
    private val log = LoggerFactory.getLogger(HttpService::class.java)
    private val textController = TextController()
    private val fileController = FileController(config)
    private lateinit var httpsService: HttpsService

    interface CallBackListener {
        fun onFileReceived(file: File)
        fun onContentRequested(path: String): InputStream
    }

    private val routes = mapOf(
        "/" to { _ -> serveRoot() },
        "/text/list" to { _ -> textController.handleTextList() },
        "/text/add" to textController::handleTextAdd,
        "/file/list" to fileController::handleFileList,
        "/file/check" to fileController::handleFileCheck,
        "/file/add" to { session -> fileController.handleFileAdd(session, listener) },
    )

    init {
        File(config.workingDir).mkdirs()
    }

    override fun start() {
        try {
            super.start()
            log.info("Server started on ${config.host}:${config.port} from ${config.workingDir}")

            if (config.useHttps) {
                httpsService = HttpsService(config, this).start()
            }

            NetworkUtils.getIpAddresses().forEach { (name, addresses) ->
                addresses.forEach { address ->
                    log.info("$name: http://$address:${config.port}")
                    if (::httpsService.isInitialized && httpsService.isRunning()) {
                        log.info("$name: https://$address:${config.httpsPort}")
                    }
                }
            }
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
        if (::httpsService.isInitialized) {
            httpsService.stop()
        }
    }

    override fun serve(session: IHTTPSession): Response {
        return try {
            // Ensure UTF-8 encoding for all incoming requests
            val contentType = session.headers["content-type"]?.let {
                if (!it.contains("charset")) "$it; charset=UTF-8" else it
            } ?: "text/plain; charset=UTF-8"

            session.headers["content-type"] = contentType

            // First try exact matches
            routes[session.uri]?.invoke(session)
                // Then try file get handler
                ?: if (session.uri.startsWith("/file/")) {
                    fileController.handleFileRequest(session)
                } else {
                    serveStaticFile(session.uri, listener)
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

    private fun serveRoot(): Response = serveStaticFile("/pad.html", null)

    private fun serveStaticFile(path: String, listener: CallBackListener?): Response {
        // Try to load from classpath (inside JAR) first
        val inputStream = javaClass.getResourceAsStream("/${config.staticDir}$path")
            ?: File("../${config.staticDir}$path").takeIf { it.exists() }?.inputStream()
            ?: listener?.onContentRequested(path)

        if (inputStream == null) {
            log.info("Not static file: {}", path)
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
}
