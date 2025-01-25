package com.github.aar0u.quickhub.util

object FileUtils {
    fun trimFromBeginning(
        path: String,
        prefix: String,
    ): String {
        return path.removePrefix(prefix).removePrefix("/")
    }

    fun formatFileSize(size: Long): String {
        if (size == 0L) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val i = Math.floor(Math.log(size.toDouble()) / Math.log(1024.0)).toInt()

        return String.format("%.2f %s", size / Math.pow(1024.0, i.toDouble()), units[i])
    }
}
