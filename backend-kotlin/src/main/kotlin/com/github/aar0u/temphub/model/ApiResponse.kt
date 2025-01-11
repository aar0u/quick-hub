package com.github.aar0u.temphub.model

data class ApiResponse(
    val status: String,
    val message: String,
    var data: Any? = null,
)
