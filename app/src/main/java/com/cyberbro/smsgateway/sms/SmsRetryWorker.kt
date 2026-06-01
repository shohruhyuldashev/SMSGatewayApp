package com.cyberbro.smsgateway.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cyberbro.smsgateway.storage.SmsDatabase

class SmsRetryWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = SmsDatabase.getInstance(applicationContext)
        val pending = db.smsTaskDao().getPendingTasks()
        val wrapper = SmsManagerWrapper(applicationContext)
        pending.forEach { task ->
            try {
                wrapper.sendSms(task.phoneNumber, task.message)
                db.smsTaskDao().update(task.copy(status = "sent"))
            } catch (error: Exception) {
                db.smsTaskDao().update(task.copy(status = "retry"))
            }
        }
        return Result.success()
    }
}
