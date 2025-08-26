package com.github.therealcheebs.maintenancerecords.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OwnershipTransfer(
    val id: String,
    val itemId: String,
    val previousOwner: String,
    val newOwner: String,
    val createdAt: Long,
    val signature: String?
)