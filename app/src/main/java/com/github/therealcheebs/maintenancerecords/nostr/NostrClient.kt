package com.github.therealcheebs.maintenancerecords.nostr

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.data.TechnicianSignoff
import com.github.therealcheebs.maintenancerecords.data.OwnershipTransfer
import com.github.therealcheebs.maintenancerecords.data.NostrEvent

object NostrClient {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var keyPair: KeyPair
    private val httpClient = OkHttpClient()
    private val activeWebSockets = ConcurrentHashMap<String, WebSocket>()
    private val eventSubscriptions = ConcurrentHashMap<String, (NostrEvent) -> Unit>()

    // Moshi instance for JSON serialization
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(NostrEvent::class.java)
    private val ownershipAdapter = moshi.adapter(OwnershipTransfer::class.java)
    private val technicianAdapter = moshi.adapter(TechnicianSignoff::class.java)
    private val maintenanceAdapter = moshi.adapter(MaintenanceRecord::class.java)
    private val messageAdapter = moshi.adapter(Map::class.java)

    // Nostr event kinds
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

    // Custom kinds for our maintenance app
    const val KIND_MAINTENANCE_RECORD = 30000
    const val KIND_OWNERSHIP_TRANSFER = 30001
    const val KIND_TECHNICIAN_SIGNOFF = 30002

    fun initialize(appContext: Context) {
        context = appContext
        prefs = context.getSharedPreferences("nostr_prefs", Context.MODE_PRIVATE)
        initializeKeyPair()
    }

    private fun initializeKeyPair() {
        val privateKeyHex = prefs.getString("private_key", null)
        val publicKeyHex = prefs.getString("public_key", null)

        if (privateKeyHex != null && publicKeyHex != null) {
            // Load existing key pair
            keyPair = KeyPair(
                decodePublicKey(publicKeyHex),
                decodePrivateKey(privateKeyHex)
            )
        } else {
            // Generate new key pair
            val keyGen = KeyPairGenerator.getInstance("EC")
            keyGen.initialize(256)
            keyPair = keyGen.generateKeyPair()

            // Save keys
            prefs.edit()
                .putString("private_key", encodePrivateKey(keyPair.private))
                .putString("public_key", encodePublicKey(keyPair.public))
                .apply()
        }
    }

    // Key encoding/decoding methods
    private fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }

    private fun encodePrivateKey(privateKey: PrivateKey): String {
        return Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
    }

    private fun decodePublicKey(hex: String): PublicKey {
        val keyBytes = Base64.decode(hex, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }

    private fun decodePrivateKey(hex: String): PrivateKey {
        val keyBytes = Base64.decode(hex, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePrivate(spec)
    }

    fun getPublicKey(): String {
        return encodePublicKey(keyPair.public)
    }

    fun getPrivateKey(): String {
        return encodePrivateKey(keyPair.private)
    }

    // Event creation and signing
    suspend fun createMaintenanceRecord(record: MaintenanceRecord): NostrEvent = withContext(Dispatchers.IO) {
        val content = maintenanceAdapter.toJson(record)
        signEvent(createEvent(KIND_MAINTENANCE_RECORD, content, listOf(listOf("d", record.id))))
    }

    suspend fun createOwnershipTransfer(
        itemId: String,
        newOwnerPubkey: String
    ): NostrEvent = withContext(Dispatchers.IO) {
        val contentObj = OwnershipTransfer(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            previousOwner = getPublicKey(),
            newOwner = newOwnerPubkey,
            createdAt = System.currentTimeMillis() / 1000,
            signature = null // will be set after signing
        )
        
        val content = ownershipAdapter.toJson(contentObj)
        signEvent(createEvent(KIND_OWNERSHIP_TRANSFER, content, listOf(listOf("d", itemId))))
    }

    suspend fun createTechnicianSignoff(
        recordId: String,
        notes: String = ""
    ): NostrEvent = withContext(Dispatchers.IO) {
        val contentObj = TechnicianSignoff(
            itemId = UUID.randomUUID().toString(),
            recordId = recordId,
            technician = getPublicKey(),
            notes = notes,
            createdAt = System.currentTimeMillis() / 1000,
            signature = null // will be set after signing
        )
        val content = technicianAdapter.toJson(contentObj)
        signEvent(createEvent(KIND_TECHNICIAN_SIGNOFF, content, listOf(listOf("e", recordId))))
    }

    private fun createEvent(
        kind: Int,
        content: String,
        tags: List<List<String>> = emptyList()
    ): NostrEvent {
        return NostrEvent(
            id = null, // will be set after signing
            pubkey = getPublicKey(),
            createdAt = System.currentTimeMillis() / 1000,
            kind = kind,
            tags = tags,
            content = content,
            signature = null // will be set after signing
        )
    }

    private fun signEvent(event: NostrEvent): NostrEvent {
        // Create a copy without id and sig for eventId generation
        val eventForId = event.copy(id = null, signature = null)
        val serialized = adapter.toJson(eventForId)
        val eventId = generateEventId(serialized)
        val signature = sign(eventId, keyPair.private)
        // Return a new NostrEvent with id and sig set
        return event.copy(id = eventId, signature = signature)
    }

    private fun generateEventId(serialized: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(serialized.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sign(data: String, privateKey: PrivateKey): String {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())
        val signedData = signature.sign()
        return Base64.encodeToString(signedData, Base64.NO_WRAP)
    }

    // Event verification
    fun verifyEvent(event: NostrEvent): Boolean {
        return try {
            val eventId = event.id ?: return false
            val publicKey = event.pubkey
            val signature = event.signature ?: return false

            // Recreate event ID without signature
            val eventForId = event.copy(id = null, signature = null)
            val serialized = adapter.toJson(eventForId)
            val recreatedId = generateEventId(serialized)

            if (recreatedId != eventId) {
                return false
            }

            // Verify signature
            val pubKey = decodePublicKey(publicKey)
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(pubKey)
            sig.update(eventId.toByteArray())
            sig.verify(Base64.decode(signature, Base64.DEFAULT))
        } catch (e: Exception) {
            false
        }
    }

    // Relay communication
    suspend fun publishToRelays(event: NostrEvent, relayUrls: List<String> = getDefaultRelays()): Boolean =
        withContext(Dispatchers.IO) {
            val results = relayUrls.map { url ->
                try {
                    publishToRelay(url, event)
                } catch (e: Exception) {
                    false
                }
            }
            results.any { it }
        }

    private suspend fun publishToRelay(relayUrl: String, event: NostrEvent): Boolean {
        val webSocket = connectToRelay(relayUrl)
        val future = CompletableFuture<Boolean>()

        // Create the message as a map and serialize with Moshi
        val messageMap = mapOf(
            "type" to "EVENT",
            "event" to event
        )
        val message = messageAdapter.toJson(messageMap)

        webSocket.send(message)

        // Wait for OK response
        val subscriptionId = "pub-${System.currentTimeMillis()}"
        eventSubscriptions[subscriptionId] = { nostrEvent ->
            // You may need to adjust this logic based on your relay response format
            if (nostrEvent.kind == KIND_MAINTENANCE_RECORD) { // Example check
                future.complete(true)
                eventSubscriptions.remove(subscriptionId)
            }
        }

        return future.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    suspend fun queryRelays(
        filter: Map<String, Any>,
        relayUrls: List<String> = getDefaultRelays(),
        onEvent: (NostrEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        relayUrls.forEach { url ->
            try {
                queryRelay(url, filter, onEvent)
            } catch (e: Exception) {
                // Log error but continue with other relays
            }
        }
    }

    private suspend fun queryRelay(
        relayUrl: String,
        filter: Map<String, Any>,
        onEvent: (NostrEvent) -> Unit
    ) {
        val webSocket = connectToRelay(relayUrl)
        val subscriptionId = "sub-${System.currentTimeMillis()}"

        // Create the message as a map and serialize with Moshi
        val messageMap = mapOf(
            "type" to "REQ",
            "subscription_id" to subscriptionId,
            "filter" to filter
        )
        val message = messageAdapter.toJson(messageMap)

        webSocket.send(message)

        // Handle events
        eventSubscriptions[subscriptionId] = { nostrEvent ->
            // You may need to adjust this logic based on your relay response format
            onEvent(nostrEvent)
        }
    }

    private fun connectToRelay(relayUrl: String): WebSocket {
        return activeWebSockets.getOrPut(relayUrl) {
            val listener = object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        // Deserialize the incoming message to NostrEvent
                        val event = adapter.fromJson(text)
                        // You may need to extract subscriptionId from the message if present
                        // For now, assume subscriptionId is not used or is part of the event
                        // If you have a wrapper object, parse it accordingly
                        // Example: val wrapper = moshi.adapter(Wrapper::class.java).fromJson(text)
                        // val subscriptionId = wrapper?.subscription_id
                        // val event = wrapper?.event
                        // if (subscriptionId != null && eventSubscriptions.containsKey(subscriptionId)) {
                        //     eventSubscriptions[subscriptionId]?.invoke(event)
                        // }
                        // For now, just call all subscriptions
                        eventSubscriptions.forEach { (_, callback) ->
                            if (event != null) callback(event)
                        }
                    } catch (e: Exception) {
                        // Handle malformed JSON
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    activeWebSockets.remove(relayUrl)
                }
            }

            val request = Request.Builder()
                .url(relayUrl)
                .build()

            httpClient.newWebSocket(request, listener)
        }
    }

    private fun getDefaultRelays(): List<String> {
        return listOf(
            "wss://relay.nostr.example",
            "wss://nostr-relay.example",
            "wss://relay.damus.io"
        )
    }

    // Encryption for private messages
    suspend fun encryptForRecipient(content: String, recipientPubkey: String): String =
        withContext(Dispatchers.IO) {
            try {
                val recipientKey = decodePublicKey(recipientPubkey)
                val sharedSecret = deriveSharedSecret(keyPair.private, recipientKey)
                val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val secretKey = SecretKeySpec(sharedSecret, "AES")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

                val encrypted = cipher.doFinal(content.toByteArray())
                val combined = iv + encrypted

                Base64.encodeToString(combined, Base64.NO_WRAP)
            } catch (e: Exception) {
                content // Fallback to unencrypted if encryption fails
            }
        }

    suspend fun decryptFromSender(encryptedContent: String, senderPubkey: String): String =
        withContext(Dispatchers.IO) {
            try {
                val senderKey = decodePublicKey(senderPubkey)
                val sharedSecret = deriveSharedSecret(keyPair.private, senderKey)
                val combined = Base64.decode(encryptedContent, Base64.DEFAULT)

                val iv = combined.sliceArray(0..15)
                val encrypted = combined.sliceArray(16 until combined.size)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val secretKey = SecretKeySpec(sharedSecret, "AES")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                String(cipher.doFinal(encrypted))
            } catch (e: Exception) {
                encryptedContent // Fallback to original content if decryption fails
            }
        }

    private fun deriveSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        val sharedSecret = keyAgreement.generateSecret()
        // Use SHA-256 to derive a proper key
        val digest = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        return digest.sliceArray(0..31) // Use first 32 bytes
    }

    // Utility methods
    fun clearKeys() {
        prefs.edit()
            .remove("private_key")
            .remove("public_key")
            .apply()
        initializeKeyPair()
    }

    fun exportKeys(): Map<String, String> {
        return mapOf(
            "public_key" to getPublicKey(),
            "private_key" to getPrivateKey()
        )
    }

    fun importKeys(publicKey: String, privateKey: String) {
        prefs.edit()
            .putString("public_key", publicKey)
            .putString("private_key", privateKey)
            .apply()
        initializeKeyPair()
    }

    suspend fun closeConnections() {
        activeWebSockets.values.forEach { it.close(1000, "Normal closure") }
        activeWebSockets.clear()
        eventSubscriptions.clear()
    }
}