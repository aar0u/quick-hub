package com.github.aar0u.quickhub.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger(this::class.simpleName)
}
