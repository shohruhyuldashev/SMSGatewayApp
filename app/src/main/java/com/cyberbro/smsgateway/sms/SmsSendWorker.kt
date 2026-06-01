package com.cyberbro.smsgateway.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyberbro.smsgateway.storage.SmsDatabase

class SmsSendWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = SmsDatabase.getInstance(applicationContext)
        val pending = db.smsTaskDao().getPendingTasks()
        val wrapper = SmsManagerWrapper(applicationContext)

        pending.forEach { task ->
            try {
                // Mark as "sending" before attempting
                db.smsTaskDao().update(
                    task.copy(status = "sending", updatedAt = System.currentTimeMillis())
                )
                wrapper.sendSms(task.phoneNumber, task.message, task.simSlot)
                db.smsTaskDao().update(
                    task.copy(status = "sent", updatedAt = System.currentTimeMillis())
                )
            } catch (error: Exception) {
                android.util.Log.e("SmsSendWorker", "Failed to send SMS to ${task.phoneNumber}", error)
                db.smsTaskDao().update(
                    task.copy(status = "failed", updatedAt = System.currentTimeMillis())
                )
            }
        }
        return Result.success()
    }
}
