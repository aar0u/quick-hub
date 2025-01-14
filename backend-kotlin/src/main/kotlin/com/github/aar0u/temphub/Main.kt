package com.github.aar0u.temphub

import com.github.aar0u.temphub.model.Config
import com.github.aar0u.temphub.service.HttpService
import java.io.File

fun main(args: Array<String>) {
    val workingDir =
        args.getOrNull(0)?.let { dir ->
            val file = File(dir)
            if (file.exists()) file.absolutePath else null
        } ?: "/Volumes/RAMDisk"

    val config = Config(workingDir = workingDir)
    HttpService(config).start()

    // Keep the main thread alive
    Thread.currentThread().join()
}
