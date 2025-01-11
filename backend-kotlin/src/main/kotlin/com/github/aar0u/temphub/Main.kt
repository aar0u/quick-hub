package com.github.aar0u.temphub

import com.github.aar0u.temphub.model.Config
import com.github.aar0u.temphub.service.HttpServiceNano
import com.github.aar0u.temphub.util.NetworkUtils
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
    val workingDir =
        args.getOrNull(0)?.let { dir ->
            val file = File(dir)
            if (file.exists()) file.absolutePath else null
        } ?: "/Volumes/RAMDisk"

    val config = Config(workingDir = workingDir)
    val nanoHttpService = HttpServiceNano(config)

    try {
        nanoHttpService.start()
        logger.info("Server started on port ${config.port}")

        // Print all available interfaces
        NetworkUtils.getIpAddresses().forEach { (name, addresses) ->
            addresses.forEach { address ->
                logger.info("$name: http://$address:${config.port}")
            }
        }

        // Keep the main thread alive
        Thread.currentThread().join()
    } catch (e: Exception) {
        logger.error("Server error: ${e.message}", e)
    }
}
