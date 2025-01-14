package com.github.aar0u.quickhub.model

data class FileInfo(
    val name: String,
    val path: String,
    val type: String,
    val size: Long? = null,
    val uploadTime: String? = null,
)
