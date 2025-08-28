package com.github.aar0u.quickhub

import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.service.HttpService
import java.io.File

fun main(args: Array<String>) {
    val workingDir = args.getOrNull(0)?.let { dir ->
        val file = File(dir)
        if (file.exists()) file.absolutePath else null
    } ?: "/Volumes/RAMDisk"

    val config = Config(workingDir = workingDir, useHttps = true)
    val app = com.github.aar0u.quickhub.service.JavalinService(config).start()
    // Javalin blocks main thread by default
}
