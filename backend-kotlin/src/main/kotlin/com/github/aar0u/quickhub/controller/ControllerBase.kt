package com.github.aar0u.quickhub.controller


abstract class ControllerBase {

    companion object {
        const val HEADER_CONTENT_RANGE = "Content-Range"
        const val HEADER_CONTENT_LENGTH = "Content-Length"
        const val MIME_JSON = "application/json"
        const val MIME_STREAM = "application/octet-stream"
    }
}
