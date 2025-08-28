package com.github.aar0u.quickhub.controller

import com.github.aar0u.quickhub.model.ApiResponse
import com.github.aar0u.quickhub.service.Loggable
import io.javalin.http.Context
import java.time.LocalDateTime

class TextController : Loggable, ControllerBase() {
    private val history: MutableList<Map<String, String>> =
        mutableListOf(
            mapOf(
                "timestamp" to LocalDateTime.now().toString(),
                "text" to "Started",
            ),
        )

    fun handleTextList(ctx: Context) {
        val response =
            ApiResponse(
                status = "success",
                message = "Load successfully",
                data = history,
            )
        ctx.status(200).json(response)
        return
    }

    fun handleTextAdd(ctx: Context) {
        val jsonObject = ctx.bodyAsClass(Map::class.java)
        val text = jsonObject["text"] as? String ?: ""

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
        ctx.status(200).json(response)
        return
    }
}
