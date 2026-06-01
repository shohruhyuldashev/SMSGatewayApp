package com.cyberbro.smsgateway.sms

import android.content.Context
import com.cyberbro.smsgateway.service.ForegroundSmsService
import com.cyberbro.smsgateway.storage.SmsDatabase
import com.cyberbro.smsgateway.storage.entities.SmsTaskEntity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import java.util.UUID

class SmsQueue(private val context: Context) {
    private val db = SmsDatabase.getInstance(context)

    /**
     * Enqueue an SMS for sending.
     * - If the foreground service is running: dispatch immediately (0 delay).
     * - Otherwise: wait up to 5 seconds for the service to start, then fire.
     * Priority HIGH tasks always get 0 delay.
     */
    suspend fun enqueue(
        phone: String,
        message: String,
        simSlot: Int = 1,
        priority: String = "normal"
    ) {
        val task = SmsTaskEntity(
            id = UUID.randomUUID().toString(),
            phoneNumber = phone,
            message = message,
            status = "queued",
            priority = priority,
            simSlot = simSlot
        )
        db.smsTaskDao().insert(task)

        // High priority or service already running → send immediately
        val delaySeconds = when {
            priority == "high" -> 0L
            ForegroundSmsService.isRunning(context) -> 0L
            else -> 5L   // max 5 second grace period
        }

        val workRequest = OneTimeWorkRequestBuilder<SmsSendWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
