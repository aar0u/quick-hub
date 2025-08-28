package com.github.aar0u.quickhub.service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.logging.ConsoleHandler
import java.util.logging.Filter

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger(this::class.simpleName)

    companion object {
        val ignoredMessages = arrayOf("Broken pipe", "Connection or outbound has closed", "Connection reset by peer")
    }

    fun shouldIgnoreMessage(message: String?): Boolean {
        return message?.let { ignoredMessages.any { ignored -> message.contains(ignored, true) } } ?: true
    }

    // Javalin migration: logger configuration not required
}
