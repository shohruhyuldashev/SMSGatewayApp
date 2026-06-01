package com.cyberbro.smsgateway.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cyberbro.smsgateway.storage.entities.SmsTaskEntity

@Dao
interface SmsTaskDao {
    @Insert
    suspend fun insert(task: SmsTaskEntity)

    @Update
    suspend fun update(task: SmsTaskEntity)

    @Query("SELECT * FROM sms_tasks WHERE status IN ('queued', 'retry') ORDER BY CASE priority WHEN 'high' THEN 0 WHEN 'normal' THEN 1 ELSE 2 END, createdAt ASC")
    suspend fun getPendingTasks(): List<SmsTaskEntity>

    @Query("SELECT * FROM sms_tasks ORDER BY createdAt DESC")
    suspend fun getAllTasks(): List<SmsTaskEntity>

    @Query("SELECT * FROM sms_tasks WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getTasksByStatus(status: String): List<SmsTaskEntity>

    @Query("SELECT * FROM sms_tasks ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getTasksPaged(limit: Int, offset: Int): List<SmsTaskEntity>

    @Query("SELECT COUNT(*) FROM sms_tasks WHERE status IN ('queued', 'retry')")
    suspend fun getPendingTaskCount(): Int

    @Query("SELECT COUNT(*) FROM sms_tasks WHERE status = 'sent'")
    suspend fun getSentTaskCount(): Int

    @Query("SELECT COUNT(*) FROM sms_tasks WHERE status = 'delivered'")
    suspend fun getDeliveredTaskCount(): Int

    @Query("SELECT COUNT(*) FROM sms_tasks WHERE status = 'failed'")
    suspend fun getFailedTaskCount(): Int

    @Query("SELECT COUNT(*) FROM sms_tasks WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM sms_tasks WHERE status = :status AND createdAt >= :sinceMs")
    suspend fun getCountByStatusSince(status: String, sinceMs: Long): Int

    @Query("SELECT * FROM sms_tasks WHERE id = :id")
    suspend fun getTaskById(id: String): SmsTaskEntity?

    @Query("DELETE FROM sms_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String): Int

    @Query("DELETE FROM sms_tasks WHERE status IN ('queued', 'retry')")
    suspend fun clearQueue(): Int

    @Query("SELECT COUNT(*) FROM sms_tasks")
    suspend fun getTotalCount(): Int
}
