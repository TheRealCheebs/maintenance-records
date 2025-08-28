package com.github.therealcheebs.maintenancerecords.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.github.therealcheebs.maintenancerecords.nostr.NostrEvent
import java.util.UUID

@JsonClass(generateAdapter = true)
data class TechnicianSignoff(
    // Local database fields
    @Json(name = "local_id") val localId: Long = 0L,
    @Json(name = "created_at") val createdAt: Long,

    // Core signoff data
    @Json(name = "record_id") val recordId: String, // Local record ID
    @Json(name = "nostr_event_id") val nostrEventId: String, // The maintenance record event ID
    @Json(name = "technician_pubkey") val technicianPubkey: String,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "warranty_info") val warrantyInfo: String? = null,
    @Json(name = "next_service_date") val nextServiceDate: Long? = null,
    @Json(name = "next_service_mileage") val nextServiceMileage: Long? = null,

    // Nostr-related fields
    @Json(name = "signoff_event_id") val signoffEventId: String? = null,
    @Json(name = "verified") val verified: Boolean = false,

    // Status flags
    @Json(name = "is_revoked") val isRevoked: Boolean = false
) {
    // Convert to NostrEvent
    fun toNostrEvent(): NostrEvent {
        val content = mapOf(
            "record_id" to nostrEventId,
            "notes" to notes,
            "warranty_info" to warrantyInfo,
            "next_service_date" to nextServiceDate,
            "next_service_mileage" to nextServiceMileage
        ).toJson()

        val tags = mutableListOf<List<String>>()
        tags.add(listOf("e", nostrEventId)) // Reference to maintenance record
        tags.add(listOf("p", technicianPubkey)) // Technician's pubkey
        tags.add(listOf("t", "technician_signoff")) // Type tag

        return NostrEvent(
            id = "", // Will be generated during signing
            pubkey = technicianPubkey,
            createdAt = System.currentTimeMillis() / 1000,
            kind = NostrEvent.KIND_TECHNICIAN_SIGNOFF,
            tags = tags,
            content = content,
            sig = "" // Will be generated during signing
        )
    }

    // Create from NostrEvent
    companion object {
        fun fromNostrEvent(event: NostrEvent): TechnicianSignoff {
            val content = event.content.toMap()

            return TechnicianSignoff(
                recordId = UUID.randomUUID().toString(), // Local ID, not from event TODO: might verify it doesn't exist locally
                nostrEventId = event.getFirstTagValue("e") ?: "",
                technicianPubkey = event.pubkey,
                notes = content["notes"] ?: "",
                warrantyInfo = content["warranty_info"],
                nextServiceDate = content["next_service_date"]?.toLongOrNull(),
                nextServiceMileage = content["next_service_mileage"]?.toLongOrNull(),
                signoffEventId = event.id,
                verified = event.isVerified(),
                createdAt = event.createdAt
            )
        }
    }
}
