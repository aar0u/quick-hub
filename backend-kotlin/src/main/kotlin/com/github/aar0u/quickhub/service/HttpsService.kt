package com.github.aar0u.quickhub.service

import com.github.aar0u.quickhub.model.Config
import fi.iki.elonen.NanoHTTPD
import java.lang.Thread.sleep
import java.net.URL
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class HttpsService(
    private val config: Config,
    private val httpService: HttpService,
) : Loggable {

    private lateinit var nanoHTTPD: NanoHTTPD
    private val keyStore = "/server.p12"
    private val pwd = "changeit"

    fun start(): HttpsService {
        javaClass.getResource(keyStore)?.let { resource ->
            runCatching {
                createHttpsServer(createSslContext(resource)).also {
                    nanoHTTPD = it
                    it.start()
                }
                log.info("HTTPS enabled on port ${config.httpsPort}")
            }.onFailure {
                log.error("HTTPS failed to start: ${it.message}")
            }
        } ?: log.warn("HTTPS: required server.p12 not found")
        return this
    }

    fun stop() {
        if (::nanoHTTPD.isInitialized) {
            nanoHTTPD.stop()
            while (nanoHTTPD.isAlive) {
                sleep(100)
            }
            log.info("HTTPS stopped on port ${config.httpsPort}")
        }
    }

    fun isRunning(): Boolean {
        return ::nanoHTTPD.isInitialized && nanoHTTPD.isAlive
    }

    private fun createHttpsServer(sslContext: SSLContext): NanoHTTPD {
        return object : NanoHTTPD(config.host, config.httpsPort) {
            override fun serve(session: IHTTPSession): Response {
                return httpService.serve(session)
            }
        }.also {
            nanoHTTPD = it
            it.makeSecure(sslContext.serverSocketFactory, null)
        }
    }

    private fun createSslContext(resource: URL): SSLContext {
        return SSLContext.getInstance("TLSv1.2").apply {
            init(
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
                    init(
                        KeyStore.getInstance("PKCS12").apply {
                            resource.openStream().use {
                                load(it, pwd.toCharArray())
                            }
                        },
                        pwd.toCharArray(),
                    )
                }.keyManagers,
                null,
                null,
            )
        }
    }
}
