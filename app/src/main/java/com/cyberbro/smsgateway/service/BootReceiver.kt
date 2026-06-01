package com.cyberbro.smsgateway.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cyberbro.smsgateway.sms.SmsRetryWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val retryRequest = PeriodicWorkRequestBuilder<SmsRetryWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sms_retry_work",
                ExistingPeriodicWorkPolicy.KEEP,
                retryRequest
            )
        }
    }
}
