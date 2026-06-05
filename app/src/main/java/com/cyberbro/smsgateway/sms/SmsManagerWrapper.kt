package com.cyberbro.smsgateway.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class SmsManagerWrapper(private val context: Context) {

    fun sendSms(phoneNumber: String, message: String, simSlot: Int = 1) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PermissionChecker.PERMISSION_GRANTED) {
            throw SecurityException("SEND_SMS permission is required")
        }

        val smsManager = getSmsManagerForSlot(simSlot)
        val parts = smsManager.divideMessage(message)
        if (parts.size == 1) {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } else {
            val sentIntents = ArrayList<PendingIntent?>(parts.size)
            val deliveryIntents = ArrayList<PendingIntent?>(parts.size)
            parts.forEach { _ ->
                sentIntents.add(null)
                deliveryIntents.add(null)
            }
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents)
        }
    }

    /**
     * Resolves the correct SmsManager for a given SIM slot (1-based).
     * On Android 12+ uses the non-deprecated createForSubscriptionId.
     * Falls back to getDefault() if subscription lookup fails.
     */
    @Suppress("DEPRECATION")
    private fun getSmsManagerForSlot(slot: Int): SmsManager {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PermissionChecker.PERMISSION_GRANTED) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val activeList = subscriptionManager.activeSubscriptionInfoList
                if (activeList != null) {
                    // slot is 1-based, simSlotIndex is 0-based
                    val targetSlotIndex = slot - 1
                    val subInfo = activeList.find { it.simSlotIndex == targetSlotIndex }
                    if (subInfo != null) {
                        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                                .createForSubscriptionId(subInfo.subscriptionId)
                        } else {
                            SmsManager.getSmsManagerForSubscriptionId(subInfo.subscriptionId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SmsManagerWrapper", "Failed to get SmsManager for slot $slot, falling back to default", e)
        }
        // Fallback
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
    }
}
