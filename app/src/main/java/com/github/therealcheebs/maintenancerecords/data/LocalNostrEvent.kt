package com.github.therealcheebs.maintenancerecords.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class NostrEventState {
    Draft, Pending, Published, Failed
}

@Entity(tableName = "local_nostr_events")
data class LocalNostrEvent(
    @PrimaryKey val id: String, // Nostr event id
    val eventJson: String,
    val eventType: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val state: NostrEventState = NostrEventState.Draft,
    val pubkey: String
)

class NostrEventStateConverter {
    @TypeConverter
    fun fromState(state: NostrEventState): String = state.name

    @TypeConverter
    fun toState(name: String): NostrEventState = NostrEventState.valueOf(name)
}
