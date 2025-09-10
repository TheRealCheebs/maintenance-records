package com.github.therealcheebs.maintenancerecords.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalNostrEventDaoTest {
    private lateinit var db: MaintenanceRecordDatabase
    private lateinit var dao: LocalNostrEventDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MaintenanceRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.localNostrEventDao()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    @Test
    fun insertAndGetEvent() = runBlocking {
        val event = LocalNostrEvent(
            id = "test_id",
            eventJson = "{}",
            eventType = 1,
            createdAt = System.currentTimeMillis() / 1000L,
            updatedAt = System.currentTimeMillis() / 1000L,
            state = NostrEventState.Pending,
            pubkey = "test_pubkey"
        )
        dao.insert(event)
        val result = dao.getById(event.id)
        assertEquals(event, result)
    }
}
