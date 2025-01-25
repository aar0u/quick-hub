package com.github.aar0u.quickhub.controller

import com.github.aar0u.quickhub.model.ApiResponse
import com.github.aar0u.quickhub.service.Loggable
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import java.time.LocalDateTime

class TextController : Loggable, ControllerBase() {
    private val history: MutableList<Map<String, String>> =
        mutableListOf(
            mapOf(
                "timestamp" to LocalDateTime.now().toString(),
                "text" to "Started",
            ),
        )

    fun handleTextList(): Response {
        val response =
            ApiResponse(
                status = "success",
                message = "Load successfully",
                data = history,
            )
        return newFixedLengthResponse(
            Response.Status.OK,
            MIME_JSON,
            gson.toJson(response),
        )
    }

    fun handleTextAdd(session: IHTTPSession): Response {
        val jsonObject = parseJsonBody(session)
        val text = jsonObject["text"] ?: ""

        log.info(
            """
            Saved:
            $text
            """.trimIndent(),
        )

        history.add(
            mapOf(
                "timestamp" to LocalDateTime.now().toString(),
                "text" to text,
            ),
        )
        val response =
            ApiResponse(
                status = "success",
                message = "Saved successfully",
            )
        return newFixedLengthResponse(
            Response.Status.OK,
            MIME_JSON,
            gson.toJson(response),
        )
    }
}
