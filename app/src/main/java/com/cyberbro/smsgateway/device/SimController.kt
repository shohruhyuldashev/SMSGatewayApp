package com.cyberbro.smsgateway.device

import android.content.Context
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import kotlinx.serialization.Serializable

@Serializable
data class SimSlotInfo(
    val slot: Int,
    val carrier: String,
    val active: Boolean,
    val signal: Int = 0
)

class SimController(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cyberbro_sim", Context.MODE_PRIVATE)

    fun getSimState(): String {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return when (telephony.simState) {
            TelephonyManager.SIM_STATE_READY          -> "READY"
            TelephonyManager.SIM_STATE_ABSENT         -> "ABSENT"
            TelephonyManager.SIM_STATE_PIN_REQUIRED   -> "PIN_REQUIRED"
            TelephonyManager.SIM_STATE_PUK_REQUIRED   -> "PUK_REQUIRED"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "NETWORK_LOCKED"
            else                                       -> "UNKNOWN"
        }
    }

    fun getCarrierName(): String {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephony.simOperatorName.takeIf { it.isNotBlank() }
            ?: telephony.networkOperatorName.takeIf { it.isNotBlank() }
            ?: "Unknown"
    }

    /**
     * Returns info about available SIM slots.
     * On Android < S we only expose SIM1 via the default telephony manager.
     */
    fun getSimSlots(): List<SimSlotInfo> {
        val result = mutableListOf<SimSlotInfo>()
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
            val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
            
            if (activeSubscriptionInfoList != null) {
                for (subInfo in activeSubscriptionInfoList) {
                    val slotIndex = subInfo.simSlotIndex + 1 // 0-based to 1-based (SIM 1, SIM 2)
                    val carrier = subInfo.carrierName?.toString() ?: "Unknown"
                    result.add(
                        SimSlotInfo(
                            slot = slotIndex,
                            carrier = carrier,
                            active = true,
                            signal = 0
                        )
                    )
                }
            }
        }
        
        // Fallback if no permission or list is empty
        if (result.isEmpty()) {
            val carrier1 = telephony.simOperatorName.takeIf { !it.isNullOrBlank() } ?: "Unknown"
            result.add(
                SimSlotInfo(
                    slot = 1,
                    carrier = carrier1,
                    active = telephony.simState == TelephonyManager.SIM_STATE_READY,
                    signal = 0
                )
            )
        }

        // Ensure we always return at least Slot 1 and Slot 2 structures for UI consistency
        val finalResult = mutableListOf<SimSlotInfo>()
        for (i in 1..2) {
            val existing = result.find { it.slot == i }
            if (existing != null) {
                finalResult.add(existing)
            } else {
                finalResult.add(SimSlotInfo(slot = i, carrier = "Unknown", active = false, signal = 0))
            }
        }

        return finalResult
    }

    fun getDefaultSlot(): Int = prefs.getInt("default_slot", 1)

    fun setDefaultSlot(slot: Int) {
        prefs.edit().putInt("default_slot", slot).apply()
    }
}
