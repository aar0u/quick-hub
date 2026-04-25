package com.github.aar0u.quickhub.controller

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

abstract class ControllerBase {
    protected val gson: Gson = GsonBuilder().create()

    companion object {
        const val HEADER_CONTENT_RANGE = "Content-Range"
        const val HEADER_CONTENT_LENGTH = "Content-Length"
        const val MIME_JSON = "application/json"
        const val MIME_STREAM = "application/octet-stream"
    }

    protected fun parseJsonBody(session: NanoHTTPD.IHTTPSession): Map<String, String> {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (contentLength <= 0) {
            return emptyMap()
        }

        val charset = resolveCharset(session.headers["content-type"])
        val output = ByteArrayOutputStream(contentLength)
        val buffer = ByteArray(8192)
        var totalRead = 0

        while (totalRead < contentLength) {
            val toRead = minOf(buffer.size, contentLength - totalRead)
            val read = session.inputStream.read(buffer, 0, toRead)
            if (read <= 0) {
                break
            }
            output.write(buffer, 0, read)
            totalRead += read
        }

        val jsonData = output.toByteArray().toString(charset).ifBlank { "{}" }
        return gson.fromJson<Map<String, String>>(jsonData, object : TypeToken<Map<String, String>>() {}.type)
            ?: emptyMap()
    }

    private fun resolveCharset(contentType: String?): Charset {
        val value = contentType
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim('"', '\'', ' ')

        return runCatching { Charset.forName(value) }.getOrDefault(StandardCharsets.UTF_8)
    }
}
