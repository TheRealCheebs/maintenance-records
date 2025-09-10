package com.github.therealcheebs.maintenancerecords.data

import com.github.therealcheebs.maintenancerecords.data.LocalNostrEventRepository
import com.github.therealcheebs.maintenancerecords.data.toMaintenanceRecordOrNull
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RecordLoader {
    /**
     * Loads all maintenance records for the given pubkey from local database.
     */
    suspend fun loadLocalRecordsForPubkey(eventRepo: LocalNostrEventRepository, pubkey: String): List<MaintenanceRecord> =
        withContext(Dispatchers.IO) {
            val events = eventRepo.getAllByPubkey(pubkey)
            events.mapNotNull { it.toMaintenanceRecordOrNull() }
        }

    /**
     * Imports records from Nostr relays for the given pubkey and saves them to local database.
     * Returns the imported records.
     */
    suspend fun importRecordsFromRelays(eventRepo: LocalNostrEventRepository, pubkey: String): List<MaintenanceRecord> =
        withContext(Dispatchers.IO) {
            val nostrEvents = NostrClient.fetchEventsForKey(pubkey, kind = null)
            nostrEvents.forEach { eventRepo.insert(event = it.toLocalNostrEvent()) }
            val updatedEvents = eventRepo.getAllByPubkey(pubkey)
            updatedEvents.mapNotNull { it.toMaintenanceRecordOrNull() }
        }
}
