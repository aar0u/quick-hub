package com.github.aar0u.temphub.util

import java.net.NetworkInterface

object NetworkUtils {
    fun getIpAddresses(): Map<String, List<String>> {
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { !it.isLoopback && it.isUp }
            .filter { iface ->
                val name = iface.name.lowercase()
                name == "en0" || // macOS Wi-Fi
                    name == "eth0" || // Linux Ethernet
                    name.contains("wi-fi") || // Windows Wi-Fi
                    name.contains("wireless") || // Generic Wireless
                    name.contains("wlan") || // Linux Wi-Fi
                    name.contains("wlp") // Linux Wi-Fi (newer naming)
            }
            .map { iface ->
                iface.name to
                    iface.inetAddresses.asSequence()
                        .filter { !it.isLoopbackAddress }
                        .filter { it.hostAddress.contains('.') } // Only IPv4
                        .map { it.hostAddress }
                        .toList()
            }
            .filter { it.second.isNotEmpty() }
            .toMap()
    }
}
