package com.github.therealcheebs.maintenancerecords.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KeyInfo(
    @Json(name = "alias") val alias: String,
    @Json(name = "name") val name: String,
    @Json(name = "public_key") val publicKey: String,
    @Json(name = "private_key") val privateKey: String,
    @Json(name = "created_at") val createdAt: Long,
    @Json(name = "is_default") val isDefault: Boolean = false
)
