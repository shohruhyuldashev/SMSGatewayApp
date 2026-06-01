package com.cyberbro.smsgateway.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class BatteryMonitor(private val context: Context) {

    fun getBatteryStatus(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra("level", -1) ?: -1
        val scale = intent?.getIntExtra("scale", -1) ?: -1
        return if (level >= 0 && scale > 0) "${(level * 100) / scale}%" else "Unknown"
    }

    fun getBatteryLevelInt(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra("level", -1) ?: -1
        val scale = intent?.getIntExtra("scale", -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }
}
