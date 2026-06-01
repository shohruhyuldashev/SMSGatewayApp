package com.cyberbro.smsgateway.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class SmsManagerWrapper(private val context: Context) {
    fun sendSms(phoneNumber: String, message: String, simSlot: Int = 1) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PermissionChecker.PERMISSION_GRANTED) {
            throw SecurityException("SEND_SMS permission is required")
        }
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            SmsManager.getSmsManagerForSubscriptionId(simSlot)
        } else {
            SmsManager.getDefault()
        }
        val parts = smsManager.divideMessage(message)
        val sentIntents = ArrayList<PendingIntent?>(parts.size)
        val deliveryIntents = ArrayList<PendingIntent?>(parts.size)
        parts.forEach { _ ->
            sentIntents.add(null)
            deliveryIntents.add(null)
        }
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents)
    }
}
