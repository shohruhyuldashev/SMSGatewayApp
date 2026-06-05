package com.cyberbro.smsgateway

import android.app.Application
import android.util.Log
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.cyberbro.smsgateway.api.KtorServer
import com.cyberbro.smsgateway.sms.SmsRetryWorker
import java.util.concurrent.TimeUnit

class CyberBroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            KtorServer.start(applicationContext)
        } catch (e: Exception) {
            Log.e("CyberBroApp", "Failed to start KtorServer, will retry on service start", e)
        }
        try {
            val retryRequest = PeriodicWorkRequestBuilder<SmsRetryWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sms_retry_work",
                ExistingPeriodicWorkPolicy.KEEP,
                retryRequest
            )
        } catch (e: Exception) {
            Log.e("CyberBroApp", "Failed to schedule retry worker", e)
        }
    }
}
