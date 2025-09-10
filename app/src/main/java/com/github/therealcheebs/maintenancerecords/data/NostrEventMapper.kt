package com.github.therealcheebs.maintenancerecords.data

import com.github.therealcheebs.maintenancerecords.nostr.NostrEvent
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEvent
import com.github.therealcheebs.maintenancerecords.data.NostrEventState

fun NostrEvent.toLocalNostrEvent(initialState: NostrEventState = NostrEventState.Published): LocalNostrEvent {
    return LocalNostrEvent(
        id = this.id,
        eventJson = this.toJson(),
        eventType = this.kind,
        createdAt = this.createdAt,
        updatedAt = this.createdAt,
        state = initialState,
        pubkey = this.pubkey
    )
}

fun List<NostrEvent>.toLocalNostrEvents(initialState: NostrEventState = NostrEventState.Published): List<LocalNostrEvent> =
    this.map { it.toLocalNostrEvent(initialState) }
