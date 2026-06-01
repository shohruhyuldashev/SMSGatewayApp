package com.cyberbro.smsgateway.api

import com.cyberbro.smsgateway.security.ApiKeyStore
import android.content.Context
import io.ktor.server.application.*

object AuthenticationMiddleware {
    private const val HEADER_API_KEY = "X-API-key"

    fun authorize(call: ApplicationCall, context: Context): Boolean {
        val provided = call.request.headers[HEADER_API_KEY]
            ?: call.request.queryParameters[HEADER_API_KEY]
        val stored = ApiKeyStore(context).getApiKey()
        return !provided.isNullOrBlank() && provided == stored
    }
}
