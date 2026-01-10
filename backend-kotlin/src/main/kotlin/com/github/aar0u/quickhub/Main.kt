package com.github.aar0u.quickhub

import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.service.HttpService
import com.github.aar0u.quickhub.util.FileUtils
import java.io.File

fun main(args: Array<String>) {
    val workingDir = args.getOrNull(0)?.let { dir ->
        val file = File(dir)
        if (!file.exists()) {
            file.mkdirs()
        }
        file.absolutePath
    } ?: run {
        System.getProperty("user.dir")
    }

    // Normalize the working directory path to ensure consistent format
    val normalizedWorkingDir = FileUtils.normalizePath(workingDir)
    val config = Config(workingDir = normalizedWorkingDir, useHttps = true)
    HttpService(config).start()

    // Keep the main thread alive
    Thread.currentThread().join()
}
