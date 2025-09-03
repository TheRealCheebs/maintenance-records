package com.github.therealcheebs.maintenancerecords.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update

@Dao
interface LocalNostrEventDao {
    @Insert
    suspend fun insert(event: LocalNostrEvent)

    @Query("SELECT * FROM local_nostr_events WHERE pubkey = :pubkey ORDER BY createdAt DESC")
    suspend fun getAllByPubkey(pubkey: String): List<LocalNostrEvent>

    @Query("SELECT * FROM local_nostr_events WHERE id = :id")
    suspend fun getById(id: String): LocalNostrEvent

    @Delete
    suspend fun delete(event: LocalNostrEvent)

    @Update
    suspend fun update(event: LocalNostrEvent)
}
