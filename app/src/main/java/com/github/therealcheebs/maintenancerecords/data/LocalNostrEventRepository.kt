package com.github.therealcheebs.maintenancerecords.data

class LocalNostrEventRepository(private val dao: LocalNostrEventDao) {
    suspend fun insert(event: LocalNostrEvent) {
        val encryptedEvent = event.copy(eventJson = CryptoManager.encrypt(event.eventJson))
        dao.insert(encryptedEvent)
    }

    suspend fun getAllByPubkey(pubkey: String): List<LocalNostrEvent> {
        return dao.getAllByPubkey(pubkey).map {
            it.copy(eventJson = CryptoManager.decrypt(it.eventJson))
        }
    }
    
    suspend fun getById(id: String): LocalNostrEvent? {
        return dao.getById(id)?.let {
            it.copy(eventJson = CryptoManager.decrypt(it.eventJson))
        }
    }

    suspend fun delete(event: LocalNostrEvent) {
        dao.delete(event)
    }

    suspend fun update(event: LocalNostrEvent) {
        val encryptedEvent = event.copy(eventJson = CryptoManager.encrypt(event.eventJson))
        dao.update(encryptedEvent)
    }
}
