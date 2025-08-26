package com.github.therealcheebs.maintenancerecords

import android.app.Application
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient

class MaintenanceRecordsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NostrClient.initialize(this)
    }
}
