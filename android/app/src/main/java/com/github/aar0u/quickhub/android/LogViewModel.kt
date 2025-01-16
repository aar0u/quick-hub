package com.github.aar0u.quickhub.android

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogViewModel : ViewModel() {
    private val _logText = mutableStateOf("")
    val logText: State<String> = _logText

    private var logJob: Job? = null

    fun startLogCapture() {
        logJob?.cancel() // 防止重复任务
        logJob = viewModelScope.launch(Dispatchers.IO) {
            val process = ProcessBuilder(
                "logcat",
                "-T", "0", // 从头开始读取
                "-v", "time", // 使用时间戳格式
                "-s", "quick-hub:V"
            ).start()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.contains("beginning of main")) return@forEach
                    val cleanedLine =
                        line.substringBefore(".") + " " + line.substringAfter("):")
                    withContext(Dispatchers.Main) {
                        _logText.value += "$cleanedLine\n"
                    }
                }
            }
        }
    }

    override fun onCleared() {
        _logText.value = ""
        logJob?.cancel() // ViewModel销毁时停止日志
        logJob = null
        super.onCleared()
    }
}