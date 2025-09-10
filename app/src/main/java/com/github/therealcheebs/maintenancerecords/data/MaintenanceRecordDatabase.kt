package com.github.therealcheebs.maintenancerecords.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEvent
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEventDao

@Database(entities = [LocalNostrEvent::class], version = 1, exportSchema = true)
abstract class MaintenanceRecordDatabase : RoomDatabase() {
    abstract fun localNostrEventDao(): LocalNostrEventDao

    companion object {
        @Volatile
        private var INSTANCE: MaintenanceRecordDatabase? = null

        fun getDatabase(context: Context): MaintenanceRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MaintenanceRecordDatabase::class.java,
                    "maintenance_record_db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
