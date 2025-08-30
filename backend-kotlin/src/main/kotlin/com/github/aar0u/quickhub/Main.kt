package com.github.aar0u.quickhub

import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.service.HttpService
import java.io.File

fun main(args: Array<String>) {
    val workingDir = args.getOrNull(0)?.let { dir ->
        val file = File(dir)
        if (!file.exists()) {
            file.mkdirs()
        }
        file.absolutePath
    } ?: "/Volumes/RAMDisk"

    val config = Config(workingDir = workingDir, useHttps = true)
    HttpService(config).start()

    // Keep the main thread alive
    Thread.currentThread().join()
}
