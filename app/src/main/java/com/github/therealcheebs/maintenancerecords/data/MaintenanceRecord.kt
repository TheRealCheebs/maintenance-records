package com.github.therealcheebs.maintenancerecords.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "maintenance_records")
data class MaintenanceRecord(
    @PrimaryKey val id: String,
    val itemId: String,
    val description: String,
    val technician: String,
    val cost: Double,
    val createdAt: Long,
    val nostrEventId: String? = null,
    var verified: Boolean = false
)
