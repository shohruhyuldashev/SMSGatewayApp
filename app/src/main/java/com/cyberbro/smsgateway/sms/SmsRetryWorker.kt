package com.cyberbro.smsgateway.sms

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyberbro.smsgateway.storage.SmsDatabase

class SmsRetryWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SmsRetryWorker"
        private const val MAX_RETRY_COUNT = 3
    }

    override suspend fun doWork(): Result {
        val db = SmsDatabase.getInstance(applicationContext)
        val pending = db.smsTaskDao().getPendingTasks()
        val wrapper = SmsManagerWrapper(applicationContext)

        pending.forEach { task ->
            // Extract retry count from status (e.g., "retry:2" → 2)
            val retryCount = if (task.status.startsWith("retry")) {
                task.status.substringAfter(":", "0").toIntOrNull() ?: 0
            } else 0

            if (retryCount >= MAX_RETRY_COUNT) {
                Log.w(TAG, "SMS to ${task.phoneNumber} exceeded max retries ($MAX_RETRY_COUNT), marking as failed")
                db.smsTaskDao().update(
                    task.copy(status = "failed", updatedAt = System.currentTimeMillis())
                )
                return@forEach
            }

            try {
                db.smsTaskDao().update(
                    task.copy(status = "sending", updatedAt = System.currentTimeMillis())
                )
                wrapper.sendSms(task.phoneNumber, task.message, task.simSlot)
                db.smsTaskDao().update(
                    task.copy(status = "sent", updatedAt = System.currentTimeMillis())
                )
            } catch (error: Exception) {
                Log.e(TAG, "Retry failed for ${task.phoneNumber} (attempt ${retryCount + 1}/$MAX_RETRY_COUNT)", error)
                val newStatus = "retry:${retryCount + 1}"
                db.smsTaskDao().update(
                    task.copy(status = newStatus, updatedAt = System.currentTimeMillis())
                )
            }
        }
        return Result.success()
    }
}
