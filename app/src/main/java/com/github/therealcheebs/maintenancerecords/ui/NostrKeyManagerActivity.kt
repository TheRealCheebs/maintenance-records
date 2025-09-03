package com.github.therealcheebs.maintenancerecords.ui

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.github.therealcheebs.maintenancerecords.databinding.ActivityNostrKeyManagerBinding
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import com.github.therealcheebs.maintenancerecords.data.KeyInfo
import kotlinx.coroutines.launch

class NostrKeyManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNostrKeyManagerBinding
    private lateinit var keyAdapter: KeyAdapter

    // Camera permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startQrScanner()
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNostrKeyManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        loadKeys()
    }

    private fun setupRecyclerView() {
        keyAdapter = KeyAdapter { keyInfo, action ->
            when (action) {
                KeyAdapter.Action.SELECT -> selectKey(keyInfo)
                KeyAdapter.Action.DELETE -> deleteKey(keyInfo)
                KeyAdapter.Action.RENAME -> renameKey(keyInfo)
                KeyAdapter.Action.EXPORT -> exportKey(keyInfo)
                KeyAdapter.Action.SET_DEFAULT -> setDefaultKey(keyInfo)
            }
        }

        binding.recyclerViewKeys.apply {
            layoutManager = LinearLayoutManager(this@NostrKeyManagerActivity)
            adapter = keyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonGenerateKey.setOnClickListener {
            generateNewKey()
        }

        binding.buttonImportText.setOnClickListener {
            showImportTextDialog()
        }

        binding.buttonImportQr.setOnClickListener {
            checkCameraPermissionAndStartQrScanner()
        }

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonCopyPublicKey.setOnClickListener {
            val currentKey = NostrClient.getCurrentKeyInfo()
            if (currentKey != null) {
                copyToClipboard(currentKey.publicKey, "Public key")
            } else {
                Toast.makeText(this, "No public key to copy", Toast.LENGTH_SHORT).show()
            }
        }

        // Check if this is first-time setup
        if (intent.getBooleanExtra("first_time", false)) {
            binding.textViewTitle.text = "Welcome! Set up your Nostr key"
            binding.buttonBack.visibility = android.view.View.GONE
        }
    }

    private fun loadKeys() {
        lifecycleScope.launch {
            val keys = NostrClient.getAllKeys()
            keyAdapter.submitList(keys)

            val currentKey = NostrClient.getCurrentKeyInfo()
            if (currentKey != null) {
                binding.textViewTitle.text = currentKey.name
                binding.textViewTitle.isClickable = true
                binding.textViewTitle.setOnClickListener {
                    val editText = android.widget.EditText(this@NostrKeyManagerActivity).apply {
                        setText(currentKey.name)
                        setSelection(text.length)
                    }
                    androidx.appcompat.app.AlertDialog.Builder(this@NostrKeyManagerActivity)
                        .setTitle("Edit Key Nickname")
                        .setView(editText)
                        .setPositiveButton("Save") { _, _ ->
                            val newName = editText.text.toString().trim()
                            if (newName.isNotBlank()) {
                                NostrClient.renameKey(currentKey.alias, newName)
                                loadKeys()
                                Toast.makeText(this@NostrKeyManagerActivity, "Key nickname updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                binding.textViewPublicKey.text = currentKey.publicKey
            } else {
                binding.textViewTitle.text = "No key selected"
                binding.textViewTitle.isClickable = false
                binding.textViewPublicKey.text = "No public key"
            }

            if (keys.isEmpty()) {
                binding.layoutNoKeys.visibility = android.view.View.VISIBLE
                binding.recyclerViewKeys.visibility = android.view.View.GONE
            } else {
                binding.layoutNoKeys.visibility = android.view.View.GONE
                binding.recyclerViewKeys.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun generateNewKey() {
        val editText = android.widget.EditText(this).apply {
            hint = "Enter nickname for new key"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setLines(1)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("New Key Nickname")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val nickname = editText.text.toString().trim().ifBlank { "Key ${System.currentTimeMillis()}" }
                lifecycleScope.launch {
                    try {
                        val alias = NostrClient.generateNewKey(nickname)
                        loadKeys()
                        Toast.makeText(this@NostrKeyManagerActivity, "New key generated successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@NostrKeyManagerActivity, "Failed to generate key: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun selectKey(keyInfo: KeyInfo) {
        NostrClient.setCurrentKey(keyInfo.alias)
        NostrClient.loadKeyPairForCurrentKey()
        loadKeys()
        Toast.makeText(this, "Selected: ${keyInfo.name}", Toast.LENGTH_SHORT).show()
    }

    private fun deleteKey(keyInfo: KeyInfo) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Key")
            .setMessage("Are you sure you want to delete '${keyInfo.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                NostrClient.deleteKey(keyInfo.alias)
                loadKeys()
                Toast.makeText(this, "Key deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameKey(keyInfo: KeyInfo) {
        val editText = android.widget.EditText(this).apply {
            setText(keyInfo.name)
            setSelection(text.length)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Rename Key")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotBlank()) {
                    NostrClient.renameKey(keyInfo.alias, newName)
                    loadKeys()
                    Toast.makeText(this, "Key renamed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setDefaultKey(keyInfo: KeyInfo) {
        NostrClient.setDefaultKey(keyInfo.alias)
        loadKeys()
        Toast.makeText(this, "Default key set", Toast.LENGTH_SHORT).show()
    }

    private fun exportKey(keyInfo: KeyInfo) {
        val exportText = """
            Name: ${keyInfo.name}
            Public Key: ${keyInfo.publicKey}
            Private Key: ${keyInfo.privateKey}

            nsec format: ${convertToNsec(keyInfo.privateKey)}
            npub format: ${convertToNpub(keyInfo.publicKey)}
        """.trimIndent()

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Export Key: ${keyInfo.name}")
            .setMessage(exportText)
            .setPositiveButton("Copy") { _, _ ->
                copyToClipboard(exportText, "Nostr key")
            }
            .setNegativeButton("Close", null)
            .show()

        dialog.findViewById<android.widget.TextView>(android.R.id.message)?.setTextIsSelectable(true)
    }

    private fun showImportTextDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "Enter private key (hex, nsec1, or ncryptsec1...)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setLines(3)
        }

        val nameEditText = android.widget.EditText(this).apply {
            hint = "Key name (optional)"
        }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            addView(nameEditText)
            addView(editText)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Import Private Key")
            .setView(layout)
            .setPositiveButton("Import") { _, _ ->
                val keyInput = editText.text.toString().trim()
                val name = nameEditText.text.toString().trim().takeIf { it.isNotBlank() } ?: "Imported Key"

                if (keyInput.isNotBlank()) {
                    importKey(keyInput, name)
                } else {
                    Toast.makeText(this, "Please enter a private key", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importKey(keyInput: String, name: String) {
        lifecycleScope.launch {
            try {
                NostrClient.importKey(keyInput, name)
                loadKeys()
                Toast.makeText(this@NostrKeyManagerActivity, "Key imported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@NostrKeyManagerActivity, "Failed to import key: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermissionAndStartQrScanner() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startQrScanner()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startQrScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a QR code containing your Nostr private key")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    private fun copyToClipboard(text: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun convertToNsec(privateKey: String): String {
        // Placeholder - implement proper bech32 encoding
        return "nsec1${privateKey.take(8)}..."
    }

    private fun convertToNpub(publicKey: String): String {
        // Placeholder - implement proper bech32 encoding
        return "npub1${publicKey.take(8)}..."
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val scannedText = result.contents
            importKey(scannedText, "Scanned Key")
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
