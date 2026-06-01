package com.cyberbro.smsgateway.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

class LocalNetworkInfo(private val context: Context) {
    fun getConnectionType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Unknown"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Unknown"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }

    fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
                ?: "Unknown"
        } catch (exception: Exception) {
            "Unknown"
        }
    }

    fun getApiEndpoint(port: Int, useHttps: Boolean = false): String {
        val scheme = if (useHttps) "https" else "http"
        val ip = getLocalIpAddress()
        return if (ip == "Unknown") {
            "$scheme://<ip-address>:$port"
        } else {
            "$scheme://$ip:$port"
        }
    }
}
