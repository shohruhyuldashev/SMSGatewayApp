package com.cyberbro.smsgateway.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RateLimiter {
    private val hits = ConcurrentHashMap<String, Pair<Long, Int>>()
    private val WINDOW_MS = TimeUnit.MINUTES.toMillis(1)
    private const val MAX_REQUESTS = 60

    fun allow(apiKey: String): Boolean {
        val now = System.currentTimeMillis()
        val pair = hits[apiKey]
        return if (pair == null || now - pair.first > WINDOW_MS) {
            hits[apiKey] = now to 1
            true
        } else if (pair.second < MAX_REQUESTS) {
            hits[apiKey] = pair.first to pair.second + 1
            true
        } else {
            false
        }
    }
}
