package com.cyberbro.smsgateway.api

import android.content.Context
import com.cyberbro.smsgateway.sms.SmsQueue

object SmsGatewayApi {
    suspend fun sendSms(request: SmsRequest, context: Context) {
        SmsQueue(context).enqueue(
            phone = request.phone,
            message = request.message,
            simSlot = request.sim,
            priority = request.priority
        )
    }
}
