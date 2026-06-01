package com.cyberbro.smsgateway.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_tasks")
data class SmsTaskEntity(
    @PrimaryKey val id: String,
    val phoneNumber: String,
    val message: String,
    val status: String,          // queued | sending | sent | delivered | failed | retry
    val priority: String = "normal", // low | normal | high
    val simSlot: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
