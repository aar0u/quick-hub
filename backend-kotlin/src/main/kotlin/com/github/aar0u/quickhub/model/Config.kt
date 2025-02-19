package com.github.aar0u.quickhub.model

import java.time.format.DateTimeFormatter

data class Config(
    val workingDir: String,
    val port: Int = System.getenv("HTTP_PORT")?.toIntOrNull() ?: 3006,
    val httpsPort: Int = System.getenv("HTTPS_PORT")?.toIntOrNull() ?: 8443,
    val useHttps: Boolean = false,
    val host: String = System.getenv("HOST") ?: "0.0.0.0",
    val dateTimeFormatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    val staticDir: String = "static",
    val overwrite: Boolean = false,
)
