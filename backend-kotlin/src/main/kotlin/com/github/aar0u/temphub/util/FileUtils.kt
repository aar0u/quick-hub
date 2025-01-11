package com.github.aar0u.temphub.util

object FileUtils {
    fun trimFromBeginning(
        path: String,
        prefix: String,
    ): String {
        return path.removePrefix(prefix).removePrefix("/")
    }

    fun formatFileSize(size: Long): String {
        val kilobyte = 1024L
        val megabyte = kilobyte * 1024
        val gigabyte = megabyte * 1024
        val terabyte = gigabyte * 1024

        return when {
            size < kilobyte -> "$size B"
            size < megabyte -> "${size.div(kilobyte)} KB"
            size < gigabyte -> "${size.div(megabyte)} MB"
            size < terabyte -> "${size.div(gigabyte)} GB"
            else -> "${size.div(terabyte)} TB"
        }
    }
}
