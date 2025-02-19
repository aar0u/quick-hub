package com.github.aar0u.quickhub.service

import fi.iki.elonen.NanoHTTPD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.logging.ConsoleHandler
import java.util.logging.Filter

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger(this::class.simpleName)

    companion object {
        val ignoredMessages = arrayOf("Broken pipe", "Connection or outbound has closed")
    }

    fun shouldIgnoreMessage(message: String?): Boolean {
        return message?.let { ignoredMessages.any { ignored -> message.contains(ignored) } } ?: true
    }

    fun configureNanoHTTPDLogger() {
        val logger = java.util.logging.Logger.getLogger(NanoHTTPD::class.java.name)
        // disable parent handlers
        logger.useParentHandlers = false
        // add a custom handler
        logger.addHandler(object : ConsoleHandler() {
            init {
                filter = Filter { record ->
                    !shouldIgnoreMessage(record.thrown.message)
                }
            }
        })
    }
}
