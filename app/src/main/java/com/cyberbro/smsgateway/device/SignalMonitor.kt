package com.cyberbro.smsgateway.device

import android.content.Context
import android.telephony.TelephonyManager

class SignalMonitor(private val context: Context) {
    fun getSignalStrength(): String {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephony.networkOperatorName ?: "Unknown"
    }
}
