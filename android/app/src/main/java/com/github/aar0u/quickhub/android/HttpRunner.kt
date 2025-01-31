package com.github.aar0u.quickhub.android

import androidx.compose.runtime.mutableStateOf
import com.github.aar0u.quickhub.model.Config
import com.github.aar0u.quickhub.service.HttpService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

object HttpRunner {
    var isServerRunning = mutableStateOf(false)
    var isBusy = mutableStateOf(false)

    private var httpService: HttpService? = null
    private var serverCoroutine: Job? = null
    private val serverScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, exception ->
            Log.e("ServerScope", "Coroutine error $exception")
        })

    fun startServer(config: Config, listener: HttpService.CallBackListener) {
        serverCoroutine?.cancel()
        serverCoroutine = serverScope.launch {
            httpService = HttpService(config, listener)
            httpService?.start()
            delay(500)
            isServerRunning.value = true
            isBusy.value = false
        }
    }

    fun stopServer() {
        serverCoroutine?.cancel()
        serverCoroutine = serverScope.launch {
            httpService?.stop() //it's blocking hence no delay
            httpService = null
            isServerRunning.value = false
            isBusy.value = false
        }
    }
}