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

@Deprecated("")
class LogViewModel : ViewModel() {
    private val _logList = mutableStateListOf<String>()
    val logList: SnapshotStateList<String> = _logList

    private var logJob: Job? = null

    fun startLogCapture() {
        logJob?.cancel() // prevent duplicate task
        logJob = viewModelScope.launch(Dispatchers.IO) {
            val process = ProcessBuilder(
                "logcat",
                "-T", "0", // read from the beginning
                "-v", "time", // use time stamp format
                "-s", "quick-hub:V"
            ).start()
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.contains("--------- beginning of")) return@forEach
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
        logJob?.cancel() // stop log when ViewModel is cleared
        logJob = null
        super.onCleared()
    }
}