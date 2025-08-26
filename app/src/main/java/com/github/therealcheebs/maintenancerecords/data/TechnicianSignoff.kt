package com.github.therealcheebs.maintenancerecords.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TechnicianSignoff(
    val recordId: String,
    val itemId: String,
    val notes: String,
    val technician: String,
    val createdAt: Long,
    val signature: String?
)
