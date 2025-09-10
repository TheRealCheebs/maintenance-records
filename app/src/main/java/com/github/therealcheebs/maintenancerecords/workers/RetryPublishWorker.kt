package com.github.therealcheebs.maintenancerecords.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecordDatabase
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEventRepository
import com.github.therealcheebs.maintenancerecords.data.NostrEventState
import com.github.therealcheebs.maintenancerecords.data.toNostrEventOrNull
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import kotlinx.coroutines.runBlocking

class RetryPublishWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Run blocking for Room and NostrClient calls
        return runBlocking {
            val db = MaintenanceRecordDatabase.getDatabase(applicationContext)
            val eventDao = db.localNostrEventDao()
            val eventRepo = LocalNostrEventRepository(eventDao)

            // Get all events with Pending or Failed status
            val eventsToRetry = eventRepo.getAllWithStatus(listOf(NostrEventState.Pending.toString(), NostrEventState.Failed.toString()))
            var anySuccess = false

            val relaysHealth = NostrClient.checkRelaysHealth()
            if (relaysHealth.isEmpty()) {
                // No relays, skip publishing
                return@runBlocking Result.success() // or keep notes in Pending state
            } 

            for (event in eventsToRetry) {
                val nostrEvent = event.toNostrEventOrNull() ?: continue
                val success = NostrClient.publishToRelays(nostrEvent)
                val newState = if (success) NostrEventState.Published else NostrEventState.Failed
                val updatedEvent = event.copy(state = newState)
                eventRepo.update(updatedEvent)
                if (success) anySuccess = true
            }

            if (anySuccess) Result.success() else Result.retry()
        }
    }
}

// Schedule this worker using WorkManager with backoff criteria in your app's startup or sync logic.
