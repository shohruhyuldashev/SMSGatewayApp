package com.cyberbro.smsgateway.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cyberbro.smsgateway.R

class ForegroundSmsService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "cyberbro_sms_channel")
            .setContentTitle("CyberBro SMS Gateway")
            .setContentText("SMS gateway is running")
            .setSmallIcon(R.drawable.icon)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "cyberbro_sms_channel",
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        fun isRunning(context: android.content.Context): Boolean {
            val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            return activityManager.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == ForegroundSmsService::class.java.name }
        }
    }
}
