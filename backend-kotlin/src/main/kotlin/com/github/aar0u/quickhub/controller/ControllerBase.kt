package com.github.aar0u.quickhub.controller

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import fi.iki.elonen.NanoHTTPD

abstract class ControllerBase {
    protected val gson: Gson = GsonBuilder().create()

    companion object {
        internal const val MIME_JSON = "application/json"
    }

    protected fun parseJsonBody(session: NanoHTTPD.IHTTPSession): Map<String, String> {
        val map = mutableMapOf<String, String>()
        session.parseBody(map)
        val jsonData = map["postData"] ?: "{}"
        return gson.fromJson(jsonData, object : TypeToken<Map<String, String>>() {})
    }
}
