package com.cyberbro.smsgateway.api

object HealthEndpoint {
    fun status() = mapOf(
        "status" to "ok",
        "service" to "CyberBro SMS Gateway",
        "version" to "1.0"
    )
}
