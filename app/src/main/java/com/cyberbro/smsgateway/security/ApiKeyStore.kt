package com.cyberbro.smsgateway.security

import android.content.Context

class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences("cyberbro_security", Context.MODE_PRIVATE)

    fun getApiKey(): String = prefs.getString("api_key", "default-demo-key") ?: "default-demo-key"

    fun setApiKey(apiKey: String) {
        prefs.edit().putString("api_key", apiKey).apply()
    }
}
