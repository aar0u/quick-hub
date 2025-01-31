package com.github.aar0u.quickhub.android

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Log {
    private const val MAX_LOG = 300
    private lateinit var logFile: File

    private val _logList = MutableStateFlow<List<String>>(emptyList())
    val logList: StateFlow<List<String>> = _logList // expose logList as a StateFlow

    fun init(context: Context) {
        logFile = File(context.filesDir, "app_logs.txt")
        if (!logFile.exists()) {
            logFile.createNewFile()
        } else if (logFile.length() > 5 * 1024 * 1024) { // clear log when larger than 5MB
            logFile.writeText("Log cleared on ${getCurrentTimestamp()}\n")
        }
        Log.i(this::class.simpleName, "LogFile: $logFile")
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun addLog(log: String) {
        val currentLogs = _logList.value.toMutableList().apply {
            add(log)
            if (size > MAX_LOG) removeAt(0)
        }
        _logList.value = currentLogs
    }

    @Synchronized
    fun i(tag: String?, message: String) {
        "${getCurrentTimestamp()} INFO/$tag: $message".also {
            addLog(it)
            logFile.appendText("$it\n")
        }
        Log.i(tag, message)
    }

    @Synchronized
    fun e(tag: String?, message: String) {
        "${getCurrentTimestamp()} ERROR/$tag: $message".also {
            addLog(it)
            logFile.appendText("$it\n")
        }
        Log.e(tag, message)
    }
}
