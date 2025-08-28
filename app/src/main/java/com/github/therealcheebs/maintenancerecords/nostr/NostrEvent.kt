package com.github.therealcheebs.maintenancerecords.nostr

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NostrEvent(
    @Json(name = "id") val id: String,
    @Json(name = "pubkey") val pubkey: String,
    @Json(name = "created_at") val createdAt: Long,
    @Json(name = "kind") val kind: Int,
    @Json(name = "tags") val tags: List<List<String>>,
    @Json(name = "content") val content: String,
    @Json(name = "sig") val sig: String
) {
    companion object {
        // Event kinds
        const val KIND_TEXT_NOTE = 1
        const val KIND_RECOMMEND_RELAY = 2
        const val KIND_CONTACTS = 3
        const val KIND_ENCRYPTED_DIRECT_MESSAGE = 4
        const val KIND_DELETE = 5
        const val KIND_REACTION = 7
        const val KIND_CHANNEL_CREATION = 40
        const val KIND_CHANNEL_META = 41
        const val KIND_CHANNEL_MESSAGE = 42
        const val KIND_CHANNEL_HIDE_MESSAGE = 43
        const val KIND_CHANNEL_MUTED_USER = 44

        // Custom kinds for maintenance tracking
        const val KIND_MAINTENANCE_RECORD = 30000
        const val KIND_OWNERSHIP_TRANSFER = 30001
        const val KIND_TECHNICIAN_SIGNOFF = 30002
    }

    // Helper methods
    fun isVerified(): Boolean {
        // Implementation would verify the signature
        // This would be implemented in NostrClient
        return false
    }

    fun getTagValues(tagName: String): List<String> {
        return tags.filter { it.size >= 2 && it[0] == tagName }.map { it[1] }
    }

    fun getFirstTagValue(tagName: String): String? {
        return tags.find { it.size >= 2 && it[0] == tagName }?.get(1)
    }
}
