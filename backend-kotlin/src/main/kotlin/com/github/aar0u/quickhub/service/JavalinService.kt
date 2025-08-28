package com.github.aar0u.quickhub.service

import com.github.aar0u.quickhub.controller.FileController
import com.github.aar0u.quickhub.controller.TextController
import com.github.aar0u.quickhub.model.Config
import io.javalin.Javalin
import io.javalin.http.Context
import org.slf4j.LoggerFactory

interface CallBackListener {
    fun onFileReceived(file: java.io.File)
    fun onContentRequested(path: String)
}

class JavalinService(private val config: Config, private val listener: CallBackListener? = null) {
    private val log = LoggerFactory.getLogger(JavalinService::class.java)
    private val textController = TextController()
    val fileController = FileController(config, listener)

    lateinit var app: Javalin

    fun start(): Javalin {
        val staticDir = java.io.File("../${config.staticDir}").absolutePath
        app = Javalin.create { cfg ->
            cfg.staticFiles.add(staticDir, io.javalin.http.staticfiles.Location.EXTERNAL)
        }.apply {
            get("/") { ctx -> ctx.redirect("/pad.html") }
            get("/text/list") { ctx -> textController.handleTextList(ctx) }
            post("/text/add") { ctx -> textController.handleTextAdd(ctx) }
            post("/file/list") { ctx -> fileController.handleFileList(ctx) }
            post("/file/check") { ctx -> fileController.handleFileCheck(ctx) }
            post("/file/add") { ctx -> fileController.handleFileAdd(ctx) }
            get("/file/*") { ctx -> fileController.handleFileRequest(ctx) }
        }
        app.start(config.port)
        log.info("Javalin server started on ${config.host}:${config.port} from ${config.workingDir}")
        return app
    }
}
