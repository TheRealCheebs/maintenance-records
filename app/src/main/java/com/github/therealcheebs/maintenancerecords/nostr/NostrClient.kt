package com.github.therealcheebs.maintenancerecords.nostr

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Types
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
import com.github.therealcheebs.maintenancerecords.data.KeyInfo

object NostrClient {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var keyPair: KeyPair
    private lateinit var encryptedPrefs: SharedPreferences
    private var isInitialized: Boolean = false
    private val httpClient = OkHttpClient()
    private val activeWebSockets = ConcurrentHashMap<String, WebSocket>()
    private val eventSubscriptions = ConcurrentHashMap<String, (NostrEvent) -> Unit>()

    private var currentKeyAlias: String? = null
    // Moshi instance for JSON serialization
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(NostrEvent::class.java)
    private val messageAdapter = moshi.adapter(Map::class.java)
    private val keyInfoListAdapter = moshi.adapter<List<KeyInfo>>(Types.newParameterizedType(List::class.java, KeyInfo::class.java))

    fun initialize(appContext: Context) {
        if (isInitialized) return
        context = appContext
        prefs = context.getSharedPreferences("nostr_prefs", Context.MODE_PRIVATE)
        setupEncryptedStorage()
        loadCurrentKeyFromStorage()
        isInitialized = true
    }

    private fun setupEncryptedStorage() {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "nostr_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun loadCurrentKeyFromStorage() {
        currentKeyAlias = encryptedPrefs.getString("last_used_key", null)

        // If no key is set but we have keys, use the first one
        if (currentKeyAlias == null) {
            val allKeys = getAllKeys()
            if (allKeys.isNotEmpty()) {
                val defaultKey = allKeys.find { it.isDefault }
                currentKeyAlias = defaultKey?.alias ?: allKeys.first().alias
                saveCurrentKeyToStorage()
            }
        }
    }

    private fun saveCurrentKeyToStorage() {
        currentKeyAlias?.let { alias ->
            encryptedPrefs.edit()
                .putString("last_used_key", alias)
                .apply()
        }
    }

    // Key management methods
    suspend fun generateNewKey(name: String = "Key ${System.currentTimeMillis()}"): String =
        withContext(Dispatchers.IO) {
            val keyGen = KeyPairGenerator.getInstance("EC")
            keyGen.initialize(256)
            val keyPair = keyGen.generateKeyPair()

            val alias = "key_${System.currentTimeMillis()}"
            val keyInfo = KeyInfo(
                alias = alias,
                name = name,
                publicKey = encodePublicKey(keyPair.public),
                privateKey = encodePrivateKey(keyPair.private),
                createdAt = System.currentTimeMillis(),
                isDefault = getAllKeys().isEmpty() // First key is default
            )

            saveKeyInfo(keyInfo)
            setCurrentKey(alias)

            alias
        }

    suspend fun importKey(
        privateKey: String,
        name: String = "Imported Key"
    ): String = withContext(Dispatchers.IO) {
        try {
            // Parse the private key and derive public key
            val (publicKey, parsedPrivateKey) = parseAndValidateKey(privateKey)

            val alias = "imported_${System.currentTimeMillis()}"
            val keyInfo = KeyInfo(
                alias = alias,
                name = name,
                publicKey = publicKey,
                privateKey = parsedPrivateKey,
                createdAt = System.currentTimeMillis(),
                isDefault = getAllKeys().isEmpty() // First key is default
            )

            saveKeyInfo(keyInfo)
            setCurrentKey(alias)

            alias
        } catch (e: Exception) {
            throw Exception("Failed to import key: ${e.message}")
        }
    }

    private fun parseAndValidateKey(keyInput: String): Pair<String, String> {
        return when {
            // nsec format (private key)
            keyInput.startsWith("nsec1") -> {
                val privateKey = decodeBech32(keyInput)
                val publicKey = derivePublicKeyFromPrivate(privateKey)
                Pair(publicKey, privateKey)
            }
            // ncryptsec format (encrypted private key)
            keyInput.startsWith("ncryptsec1") -> {
                throw Exception("Encrypted private keys (ncryptsec) are not supported yet")
            }
            // Hex format (private key)
            keyInput.matches(Regex("^[a-fA-F0-9]{64}$")) -> {
                val publicKey = derivePublicKeyFromPrivate(keyInput)
                Pair(publicKey, keyInput)
            }
            // npub format (public key) - we need the private key
            keyInput.startsWith("npub1") -> {
                throw Exception("You provided a public key (npub). Please provide a private key (nsec or hex).")
            }
            else -> {
                throw Exception("Invalid key format. Please provide a valid Nostr private key.")
            }
        }
    }

    fun setCurrentKey(alias: String) {
        currentKeyAlias = alias
        saveCurrentKeyToStorage()
    }

    fun getCurrentKeyAlias(): String? = currentKeyAlias

    fun hasAnyKeys(): Boolean {
        return getAllKeys().isNotEmpty()
    }

    fun needsKeySetup(): Boolean {
        return !hasAnyKeys()
    }

    fun getAllKeyAliases(): List<String> {
        val keysJson = encryptedPrefs.getString("saved_keys", "[]")
        return try {
            val keyList = keyInfoListAdapter.fromJson(keysJson) ?: emptyList()
            keyList.map { it.alias }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllKeys(): List<KeyInfo> {
        val keysJson = encryptedPrefs.getString("saved_keys", "[]")
        return try {
            keyInfoListAdapter.fromJson(keysJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getKeyInfo(alias: String): KeyInfo? {
        val keys = getAllKeys()
        return keys.find { it.alias == alias }
    }

    fun getCurrentKeyInfo(): KeyInfo? {
        return currentKeyAlias?.let { getKeyInfo(it) }
    }

    private fun saveKeyInfo(keyInfo: KeyInfo) {
        val existingKeys = getAllKeys().toMutableList()

        // Remove if key with same alias exists
        existingKeys.removeAll { it.alias == keyInfo.alias }

        // Add the new key
        existingKeys.add(keyInfo)

        // Save back to preferences
        val keysJson = keyInfoListAdapter.toJson(existingKeys)

        encryptedPrefs.edit()
            .putString("saved_keys", keysJson)
            .apply()
    }

    fun deleteKey(alias: String) {
        val existingKeys = getAllKeys().toMutableList()
        existingKeys.removeAll { it.alias == alias }

        val keysJson = keyInfoListAdapter.toJson(existingKeys)

        encryptedPrefs.edit()
            .putString("saved_keys", keysJson)
            .apply()

        // If we deleted the current key, switch to another one
        if (currentKeyAlias == alias) {
            val remainingKeys = getAllKeyAliases()
            if (remainingKeys.isNotEmpty()) {
                setCurrentKey(remainingKeys.first())
            } else {
                currentKeyAlias = null
                // Clear the saved key since there are no keys left
                encryptedPrefs.edit()
                    .remove("last_used_key")
                    .apply()
            }
        }
    }

    fun renameKey(alias: String, newName: String) {
        val keyInfo = getKeyInfo(alias)
        keyInfo?.let {
            val updatedKey = it.copy(name = newName)
            saveKeyInfo(updatedKey)
        }
    }

    fun setDefaultKey(alias: String) {
        val keys = getAllKeys().toMutableList()
        val updatedKeys = keys.map { it.copy(isDefault = (it.alias == alias)) }
        val keysJson = keyInfoListAdapter.toJson(updatedKeys)

        encryptedPrefs.edit()
            .putString("saved_keys", keysJson)
            .apply()
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

    // Bech32 decoding (simplified - use a proper library in production)
    private fun decodeBech32(bech32: String): String {
        if (bech32.startsWith("nsec1")) {
            val data = bech32.substring(5).dropLast(6)
            return convertBech32ToHex(data)
        }
        throw Exception("Invalid bech32 format")
    }

    private fun convertBech32ToHex(data: String): String {
        // This is a placeholder implementation
        // In production, use a proper bech32 library
        return data.map { it.code }.joinToString("") { it.toString(16).padStart(2, '0') }
    }

    private fun derivePublicKeyFromPrivate(privateKey: String): String {
        // This should be implemented properly using cryptographic operations
        // For now, return a placeholder
        return "placeholder_public_key"
    }

    fun getPublicKey(): String {
        return encodePublicKey(keyPair.public)
    }

    fun getPrivateKey(): String {
        return encodePrivateKey(keyPair.private)
    }

    // Event creation and signing
    suspend fun signMaintenanceRecord(record: MaintenanceRecord): NostrEvent = withContext(Dispatchers.IO) {
        signEvent(record.toNostrEvent())
    }

    suspend fun signOwnershipTransfer(transfer: OwnershipTransfer): NostrEvent = withContext(Dispatchers.IO) {
        signEvent(transfer.toNostrEvent())
    }

    suspend fun signTechnicianSignoff(techSignoff: TechnicianSignoff): NostrEvent = withContext(Dispatchers.IO) {
        signEvent(techSignoff.toNostrEvent())
    }

    private fun createEvent(
        kind: Int,
        content: String,
        tags: List<List<String>> = emptyList()
    ): NostrEvent {
        return NostrEvent(
            id = "", // will be set after signing
            pubkey = getPublicKey(),
            createdAt = System.currentTimeMillis() / 1000,
            kind = kind,
            tags = tags,
            content = content,
            sig = "" // will be set after signing
        )
    }

    private fun signEvent(event: NostrEvent): NostrEvent {
        // Create a copy without id and sig for eventId generation
        val eventForId = event.copy(id = "", sig = "")
        val serialized = adapter.toJson(eventForId)
        val eventId = generateEventId(serialized)
        val signature = sign(eventId, keyPair.private)
        // Return a new NostrEvent with id and sig set
        return event.copy(id = eventId, sig = signature)
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
            val eventId = event.id
            val publicKey = event.pubkey
            val signature = event.sig

            // Recreate event ID without signature
            val eventForId = event.copy(id = "", sig = "")
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
            if (nostrEvent.kind == NostrEvent.KIND_MAINTENANCE_RECORD) { // Example check
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

    /**
     * Fetches all Nostr events for the given pubkey from Nostr relays. If kind is provided, filters by kind.
     */
    suspend fun fetchEventsForKey(pubkey: String, kind: Int? = null): List<NostrEvent> = withContext(Dispatchers.IO) {
        val filter = mutableMapOf<String, Any>("pubkey" to pubkey)
        if (kind != null) {
            filter["kind"] = kind
        }
        val relayUrls = getDefaultRelays()

        val eventList = Collections.synchronizedList(mutableListOf<NostrEvent>())
        val eventCallback: (NostrEvent) -> Unit = { event ->
            eventList.add(event)
        }

        // Query relays for matching events
        queryRelays(filter, relayUrls, eventCallback)

        // Wait briefly for events to arrive (simple approach)
        Thread.sleep(2000) // You may want a more robust async solution

        eventList.toList()
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
            // "wss://relay.nostr.example",
            // "wss://nostr-relay.example",
            // "wss://relay.damus.io"
        )
    }
    
    suspend fun checkRelayConnection(relayUrl: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = Request.Builder()
                .url(relayUrl)
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkRelaysHealth(relayUrls: List<String> = getDefaultRelays()): Map<String, Boolean> =
        withContext(Dispatchers.IO) {
            val results = relayUrls.map { url ->
                val isHealthy = checkRelayConnection(url)
                url to isHealthy
            }
            results.toMap()
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
            encryptedPrefs.edit()
                .remove("saved_keys")
                .remove("last_used_key")
                .apply()
            currentKeyAlias = null
        }

    fun exportKeys(): Map<String, String> {
        val keyInfo = getCurrentKeyInfo()
        return if (keyInfo != null) {
            mapOf(
                "public_key" to keyInfo.publicKey,
                "private_key" to keyInfo.privateKey,
                "alias" to keyInfo.alias,
                "name" to keyInfo.name
            )
        } else {
            emptyMap()
        }
    }

    fun importKeys(publicKey: String, privateKey: String) {
        val alias = "imported_${System.currentTimeMillis()}"
        val keyInfo = KeyInfo(
            alias = alias,
            name = "Imported Key",
            publicKey = publicKey,
            privateKey = privateKey,
            createdAt = System.currentTimeMillis(),
            isDefault = getAllKeys().isEmpty()
        )
        saveKeyInfo(keyInfo)
        setCurrentKey(alias)
    }

    suspend fun closeConnections() {
        activeWebSockets.values.forEach { it.close(1000, "Normal closure") }
        activeWebSockets.clear()
        eventSubscriptions.clear()
    }

    fun loadKeyPairForCurrentKey() {
        val keyInfo = getCurrentKeyInfo()
        if (keyInfo != null) {
            val publicKey = decodePublicKey(keyInfo.publicKey)
            val privateKey = decodePrivateKey(keyInfo.privateKey)
            keyPair = KeyPair(publicKey, privateKey)
        } else {
            throw IllegalStateException("No current key info available")
        }
    }
}
