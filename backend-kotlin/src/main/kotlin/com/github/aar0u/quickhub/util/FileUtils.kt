package com.github.aar0u.quickhub.util

object FileUtils {
    fun normalizePath(path: String): String {
        // Convert all backslashes to forward slashes for consistency
        var normalized = path.replace("\\", "/")

        // Replace multiple consecutive slashes with a single slash
        normalized = normalized.replace(Regex("/+"), "/")

        // Remove any trailing slash or dot (current directory)
        while ((normalized.endsWith("/") || normalized.endsWith(".")) && normalized != "/" && normalized != ".") {
            normalized = normalized.dropLast(1)
        }

        return normalized
    }

    fun trimFromBeginning(
        path: String,
        prefix: String,
    ): String {
        // Normalize both paths to ensure consistent separators
        val normalizedPath = normalizePath(path)
        val normalizedPrefix = normalizePath(prefix)

        val trimmed = normalizedPath.removePrefix(normalizedPrefix)
        return trimmed.removePrefix("/").removePrefix("\\")
    }

    fun formatFileSize(size: Long): String {
        if (size == 0L) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val i = Math.floor(Math.log(size.toDouble()) / Math.log(1024.0)).toInt()

        return String.format("%.2f %s", size / Math.pow(1024.0, i.toDouble()), units[i])
    }
}
