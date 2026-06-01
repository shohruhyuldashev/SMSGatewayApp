package com.cyberbro.smsgateway

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.cyberbro.smsgateway.api.KtorServer
import com.cyberbro.smsgateway.sms.SmsRetryWorker
import java.util.concurrent.TimeUnit

class CyberBroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KtorServer.start(applicationContext)
        val retryRequest = PeriodicWorkRequestBuilder<SmsRetryWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sms_retry_work",
            ExistingPeriodicWorkPolicy.KEEP,
            retryRequest
        )
    }
}
