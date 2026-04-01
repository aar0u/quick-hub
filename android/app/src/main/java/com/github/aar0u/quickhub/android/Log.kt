package com.github.aar0u.quickhub.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Log {
    private const val MAX_LOG = 300
    private lateinit var logFile: File
    val file: File get() = logFile

    private var _webView: WebView? = null
    val webView: WebView? get() = _webView
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        captureSlf4j()

        logFile = File(context.filesDir, "app_logs.txt")
        if (!logFile.exists()) {
            logFile.createNewFile()
        } else if (logFile.length() > 5 * 1024 * 1024) {
            logFile.writeText("Log cleared on ${getCurrentTimestamp()}\n")
        }
        i(this::class.simpleName, "LogFile: $logFile")
    }

    fun setWebView(wv: WebView) {
        _webView = wv
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun pushToWebView(text: String) {
        _webView?.let { wv ->
            val escaped = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
            mainHandler.post {
                wv.evaluateJavascript("onLogEntry('$escaped')", null)
            }
        }
    }

    @Synchronized
    fun i(tag: String?, message: String) {
        "${getCurrentTimestamp()} INFO/$tag: $message".also {
            pushToWebView(it)
            logFile.appendText("$it\n")
        }
        Log.i(tag, message)
    }

    @Synchronized
    fun e(tag: String?, message: String) {
        "${getCurrentTimestamp()} ERROR/$tag: $message".also {
            pushToWebView(it)
            logFile.appendText("$it\n")
        }
        Log.e(tag, message)
    }

    private fun captureSlf4j() {
        val loggerContext = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)

        if (rootLogger.getAppender("CUSTOM") != null) {
            return
        }

        val customAppender = object : AppenderBase<ILoggingEvent>() {
            override fun append(event: ILoggingEvent) {
                val message = event.formattedMessage
                val level = event.level.toString()
                val loggerName = event.loggerName

                if (level.equals("ERROR", ignoreCase = true)) {
                    e(loggerName, message)
                } else {
                    i(loggerName, message)
                }
            }
        }.apply {
            name = "CUSTOM"
            start()
        }

        rootLogger.addAppender(customAppender)
    }
}
