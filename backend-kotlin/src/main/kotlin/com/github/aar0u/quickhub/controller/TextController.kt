package com.github.aar0u.quickhub.controller

import com.github.aar0u.quickhub.model.ApiResponse
import com.github.aar0u.quickhub.service.Loggable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import java.time.LocalDateTime

class TextController : Loggable {
    private val history: MutableList<Map<String, String>> =
        mutableListOf(
            mapOf(
                "timestamp" to LocalDateTime.now().toString(),
                "text" to "Started",
            ),
        )
    private val gson: Gson = GsonBuilder().create()

    fun handleTextList(): Response {
        val response =
            ApiResponse(
                status = "success",
                message = "Load successfully",
                data = history,
            )
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            gson.toJson(response),
        )
    }

    fun handleTextAdd(session: IHTTPSession): Response {
        val map = mutableMapOf<String, String>()
        session.parseBody(map)
        val jsonData = map["postData"] ?: "{}"
        val jsonObject = gson.fromJson(jsonData, object : TypeToken<Map<String, String>>() {})
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
            "application/json",
            gson.toJson(response),
        )
    }
}
