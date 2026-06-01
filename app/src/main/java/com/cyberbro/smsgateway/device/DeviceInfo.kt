package com.cyberbro.smsgateway.device

import android.content.Context
import android.os.Build

class DeviceInfo(private val context: Context) {
    fun getDeviceSummary(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }
}
