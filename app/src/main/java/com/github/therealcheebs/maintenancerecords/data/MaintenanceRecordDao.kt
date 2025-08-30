package com.github.therealcheebs.maintenancerecords.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MaintenanceRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MaintenanceRecord)

    @Query("SELECT * FROM MaintenanceRecord WHERE itemId = :itemId LIMIT 1")
    suspend fun getRecordById(itemId: String): MaintenanceRecord?

    @Query("SELECT * FROM MaintenanceRecord")
    suspend fun getAllRecords(): List<MaintenanceRecord>

    @Query("SELECT * FROM MaintenanceRecord WHERE currentOwnerPubkey = :currentOwnerPubkey")
    suspend fun getRecordsByOwner(currentOwnerPubkey: String): List<MaintenanceRecord>

    @Update
    suspend fun update(record: MaintenanceRecord)
}
