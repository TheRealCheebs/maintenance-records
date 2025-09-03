package com.github.therealcheebs.maintenancerecords.data

import com.github.therealcheebs.maintenancerecords.data.LocalNostrEvent
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.nostr.NostrEvent
import com.squareup.moshi.Moshi

fun LocalNostrEvent.toMaintenanceRecordOrNull(): MaintenanceRecord? {
    if (eventType != NostrEvent.KIND_MAINTENANCE_RECORD) return null
    return try {
        val moshi = Moshi.Builder().build()
        val nostrEventAdapter = moshi.adapter(NostrEvent::class.java)
        val nostrEvent = nostrEventAdapter.fromJson(eventJson)
        if (nostrEvent != null) {
            MaintenanceRecord.fromNostrEvent(nostrEvent, pubkey)
        } else {
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("EventMapper", "Failed to parse MaintenanceRecord: ${e.message}")
        android.util.Log.e("EventMapper", "JSON: $eventJson")
        null
    }
}
