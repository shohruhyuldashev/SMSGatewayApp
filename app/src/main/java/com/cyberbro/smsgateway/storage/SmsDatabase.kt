package com.cyberbro.smsgateway.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cyberbro.smsgateway.storage.entities.SmsTaskEntity

@Database(entities = [SmsTaskEntity::class], version = 3, exportSchema = false)
abstract class SmsDatabase : RoomDatabase() {
    abstract fun smsTaskDao(): SmsTaskDao

    companion object {
        @Volatile
        private var INSTANCE: SmsDatabase? = null

        fun getInstance(context: Context): SmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    "cyberbro_sms_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
