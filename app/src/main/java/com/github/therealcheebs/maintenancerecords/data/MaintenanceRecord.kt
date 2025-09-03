package com.github.therealcheebs.maintenancerecords.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.github.therealcheebs.maintenancerecords.nostr.NostrEvent
import java.util.Date

@JsonClass(generateAdapter = true)
data class MaintenanceRecord(
    @Json(name = "created_at") val createdAt: Long,
    @Json(name = "updated_at") val updatedAt: Long,

    // Core maintenance data
    @Json(name = "item_id") val itemId: String,
    @Json(name = "description") val description: String,
    @Json(name = "technician") val technician: String,
    @Json(name = "cost") val cost: Double,
    @Json(name = "date_performed") val datePerformed: Long,
    @Json(name = "mileage") val mileage: Long? = null,
    @Json(name = "notes") val notes: String = "",

    // Nostr-related fields
    @Json(name = "nostr_event_id") val nostrEventId: String? = null,
    @Json(name = "verified") val verified: Boolean = false,
    @Json(name = "technician_signoff_event_id") val technicianSignoffEventId: String? = null,

    // Ownership tracking
    @Json(name = "current_owner_pubkey") val currentOwnerPubkey: String,
    @Json(name = "previous_owner_pubkey") val previousOwnerPubkey: String? = null,

    // Status flags
    @Json(name = "is_deleted") val isDeleted: Boolean = false,
    @Json(name = "is_encrypted") val isEncrypted: Boolean = false
) {
    // Convert to NostrEvent
    fun toNostrEvent(): NostrEvent {
        val content = mapOf(
            "item_id" to itemId,
            "description" to description,
            "technician" to technician,
            "cost" to cost,
            "date_performed" to datePerformed,
            "mileage" to mileage,
            "notes" to notes
        ).toJson()

        val tags = mutableListOf<List<String>>()
        tags.add(listOf("d", itemId)) // Identifier tag
        tags.add(listOf("t", "maintenance")) // Type tag
        mileage?.let { tags.add(listOf("mileage", it.toString())) }

        return NostrEvent(
            id = "", // Will be generated during signing
            pubkey = currentOwnerPubkey,
            createdAt = System.currentTimeMillis() / 1000,
            kind = NostrEvent.KIND_MAINTENANCE_RECORD,
            tags = tags,
            content = content,
            sig = "" // Will be generated during signing
        )
    }

    // Create from NostrEvent
    companion object {
        fun fromNostrEvent(event: NostrEvent, ownerPubkey: String): MaintenanceRecord {
            val content = event.content.toMap()

            return MaintenanceRecord(
                itemId = content["item_id"] ?: "",
                description = content["description"] ?: "",
                technician = content["technician"] ?: "",
                cost = content["cost"]?.toDoubleOrNull() ?: 0.0,
                datePerformed = content["date_performed"]?.toLongOrNull() ?: event.createdAt,
                mileage = content["mileage"]?.toLongOrNull(),
                notes = content["notes"] ?: "",
                nostrEventId = event.id,
                verified = event.isVerified(),
                currentOwnerPubkey = ownerPubkey,
                createdAt = event.createdAt,
                updatedAt = event.createdAt
            )
        }
    }
}
