package com.github.aar0u.quickhub.android

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
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
    var isServerRunning = false
        private set
    var isBusy = false

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
            isServerRunning = true
            isBusy = false
            notifyServerState()
        }
    }

    fun stopServer() {
        serverCoroutine?.cancel()
        serverCoroutine = serverScope.launch {
            httpService?.stop()
            httpService = null
            isServerRunning = false
            isBusy = false
            notifyServerState()
        }
    }

    private fun notifyServerState() {
        val wv = Log.webView ?: return
        Handler(Looper.getMainLooper()).post {
            wv.evaluateJavascript("onServerStateChanged($isServerRunning, $isBusy)", null)
        }
    }
}
