package com.github.aar0u.quickhub.model

data class ApiResponse(
    val status: String,
    val message: String,
    var data: Any? = null,
)
