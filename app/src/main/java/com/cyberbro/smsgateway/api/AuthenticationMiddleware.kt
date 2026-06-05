package com.cyberbro.smsgateway.api

import com.cyberbro.smsgateway.security.ApiKeyStore
import android.content.Context
import io.ktor.server.application.*

object AuthenticationMiddleware {
    private const val HEADER_API_KEY = "x-api-key"

    fun authorize(call: ApplicationCall, context: Context): Boolean {
        // Check header (case-insensitive) and query parameter
        val provided = call.request.headers[HEADER_API_KEY]
            ?: call.request.queryParameters["api_key"]
            ?: call.request.queryParameters["X-API-key"]
        val stored = ApiKeyStore(context).getApiKey()
        if (stored == "default-demo-key") return false // block default key
        return !provided.isNullOrBlank() && provided == stored
    }
}
