package com.github.aar0u.quickhub.android

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val maxLog = 500

class LogViewModel : ViewModel() {
    private val _logList = mutableStateListOf<String>()
    val logList: SnapshotStateList<String> = _logList

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
                    withContext(Dispatchers.Main) {
                        addLog(line.substringBefore(".") + " " + line.substringAfter("):"))
                    }
                }
            }
        }
    }

    private fun addLog(log: String) {
        _logList.add(log)

        if (_logList.size > maxLog) {
            _logList.removeAt(0)
        }
    }

    override fun onCleared() {
        _logList.clear()
        logJob?.cancel() // ViewModel销毁时停止日志
        logJob = null
        super.onCleared()
    }
}