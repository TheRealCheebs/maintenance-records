package com.github.therealcheebs.maintenancerecords.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MaintenanceRecord::class], version = 1)
abstract class MaintenanceRecordDatabase : RoomDatabase() {
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: MaintenanceRecordDatabase? = null

        fun getDatabase(context: Context): MaintenanceRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MaintenanceRecordDatabase::class.java,
                    "maintenance_record_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
