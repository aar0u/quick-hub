package com.github.aar0u.temphub.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger("quick-hub")
}
