package com.github.therealcheebs.maintenancerecords.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.github.therealcheebs.maintenancerecords.nostr.NostrEvent

@JsonClass(generateAdapter = true)
data class OwnershipTransfer(
    // Local database fields
    @Json(name = "local_id") val localId: Long = 0L,
    @Json(name = "created_at") val createdAt: Long,

    // Core transfer data
    @Json(name = "item_id") val itemId: String,
    @Json(name = "previous_owner_pubkey") val previousOwnerPubkey: String,
    @Json(name = "new_owner_pubkey") val newOwnerPubkey: String,
    @Json(name = "transfer_date") val transferDate: Long,
    @Json(name = "notes") val notes: String = "",

    // Nostr-related fields
    @Json(name = "transfer_event_id") val transferEventId: String? = null,
    @Json(name = "verified") val verified: Boolean = false,

    // Status flags
    @Json(name = "is_completed") val isCompleted: Boolean = false,
    @Json(name = "is_revoked") val isRevoked: Boolean = false
) {
    // Convert to NostrEvent
    fun toNostrEvent(): NostrEvent {
        val content = mapOf(
            "item_id" to itemId,
            "previous_owner" to previousOwnerPubkey,
            "new_owner" to newOwnerPubkey,
            "transfer_date" to transferDate,
            "notes" to notes
        ).toJson()

        val tags = mutableListOf<List<String>>()
        tags.add(listOf("d", itemId)) // Identifier tag
        tags.add(listOf("p", previousOwnerPubkey)) // Previous owner
        tags.add(listOf("p", newOwnerPubkey)) // New owner
        tags.add(listOf("t", "ownership_transfer")) // Type tag

        return NostrEvent(
            id = "", // Will be generated during signing
            pubkey = previousOwnerPubkey,
            createdAt = System.currentTimeMillis() / 1000,
            kind = NostrEvent.KIND_OWNERSHIP_TRANSFER,
            tags = tags,
            content = content,
            sig = "" // Will be generated during signing
        )
    }

    // Create from NostrEvent
    companion object {
        fun fromNostrEvent(event: NostrEvent): OwnershipTransfer {
            val content = event.content.toMap()

            return OwnershipTransfer(
                itemId = content["item_id"] ?: "",
                previousOwnerPubkey = event.pubkey,
                newOwnerPubkey = content["new_owner"] ?: "",
                transferDate = content["transfer_date"]?.toLongOrNull() ?: System.currentTimeMillis() / 1000,
                notes = content["notes"] ?: "",
                transferEventId = event.id,
                verified = event.isVerified(),
                createdAt = event.createdAt
            )
        }
    }
}
