package com.github.therealcheebs.maintenancerecords.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.therealcheebs.maintenancerecords.databinding.ActivityMainBinding
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecordDatabase
import com.github.therealcheebs.maintenancerecords.data.KeyInfo
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import com.github.therealcheebs.maintenancerecords.R
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import java.util.Date


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show active key alias/name at the top
        val keyInfo = NostrClient.getCurrentKeyInfo()
        val aliasOrName = keyInfo?.name ?: "Unknown Key"

        // Check if we need to set up keys
        if (NostrClient.needsKeySetup()) {
            // Redirect to key manager
            val intent = Intent(this, NostrKeyManagerActivity::class.java)
            intent.putExtra("first_time", true)
            startActivity(intent)
            finish()
            return
        }

        // Show key selection dialog if multiple keys exist
        if (NostrClient.getAllKeys().size > 1) {
            showKeySelectionDialog()
        }

        setupRecyclerView()
        setupClickListeners()
    }
    
    private fun showKeySelectionDialog() {
        val dialog = KeySelectionDialog()
        dialog.setOnKeySelected { keyInfo ->
            NostrClient.setCurrentKey(keyInfo.alias)
            Toast.makeText(this, "Using key: ${keyInfo.name}", Toast.LENGTH_SHORT).show()
            loadRecordsForCurrentKey()
        }
        
        dialog.setOnManageKeys {
            val intent = Intent(this, NostrKeyManagerActivity::class.java)
            startActivity(intent)
        }
        
        dialog.show(supportFragmentManager, "KeySelectionDialog")
    }
    
    private fun setupRecyclerView() {
        adapter = RecordAdapter { record ->
            val intent = Intent(this, RecordDetailActivity::class.java).apply {
                putExtra("RECORD_ID", record.itemId)
            }
            startActivity(intent)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }
    
    private fun setupClickListeners() {
        binding.fab.setOnClickListener {
            val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_fab_menu, null)
            bottomSheet.setContentView(sheetView)

            sheetView.findViewById<android.widget.TextView>(R.id.menu_create_record).setOnClickListener {
                bottomSheet.dismiss()
                startActivity(Intent(this, CreateRecordActivity::class.java))
            }
            sheetView.findViewById<android.widget.TextView>(R.id.menu_ownership_transfer).setOnClickListener {
                bottomSheet.dismiss()
                // TODO: Replace with your OwnershipTransferActivity or logic
                Toast.makeText(this, "Ownership Transfer selected", Toast.LENGTH_SHORT).show()
            }

            bottomSheet.show()
        }
        
        // Add menu option for key management
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_key_manager -> {
                    startActivity(Intent(this, NostrKeyManagerActivity::class.java))
                    true
                }
                R.id.action_select_key -> {
                    showKeySelectionDialog()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadSampleData() {
        val sampleRecords = listOf(
            MaintenanceRecord(
                localId = 1L,
                createdAt = 1693500000000, // Example date
                updatedAt = 1693503600000,
                itemId = "vehicle_001",
                description = "Oil change and filter replacement",
                technician = "Quick Lube",
                cost = 75.00,
                datePerformed = 1693496400000,
                mileage = 12000L,
                notes = "Used synthetic oil",
                nostrEventId = "event_abc123",
                verified = true,
                technicianSignoffEventId = "signoff_xyz789",
                currentOwnerPubkey = "npub1ownerpubkeyabc",
                previousOwnerPubkey = "npub1prevownerpubkeyxyz",
                isDeleted = false,
                isEncrypted = false
            ),
            MaintenanceRecord(
                localId = 2L,
                createdAt = 1693586400000, // Example date
                updatedAt = 1693590000000,
                itemId = "vehicle_002",
                description = "Tire rotation and balance",
                technician = "Tire Shop",
                cost = 50.00,
                datePerformed = 1693582800000,
                mileage = 15000L,
                notes = "Front tires to back",
                nostrEventId = "event_def456",
                verified = false,
                technicianSignoffEventId = null,
                currentOwnerPubkey = "npub1ownerpubkeydef",
                previousOwnerPubkey = null,
                isDeleted = false,
                isEncrypted = false
            )
        )
        
        adapter.submitList(sampleRecords)
    }
    
    private fun loadRecordsForCurrentKey() {
        val currentKey = NostrClient.getCurrentKeyInfo()
        if (currentKey == null) {
            Toast.makeText(this, "No active key found. Please set up a key.", Toast.LENGTH_LONG).show()
            return
        }
        val currentPubkey = currentKey.publicKey
        val db = MaintenanceRecordDatabase.getDatabase(applicationContext)
        lifecycleScope.launch {
            val records = db.maintenanceRecordDao().getRecordsByOwner(currentPubkey)
            adapter.submitList(records)
            if (records.isEmpty()) {
                runOnUiThread {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("No Records Found")
                        .setMessage("There is no record activity for this key.")
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Import Records") { _, _ ->
                            lifecycleScope.launch {
                                try {
                                    val nostrRecords = NostrClient.fetchRecordsForKey(currentPubkey)
                                    val db = MaintenanceRecordDatabase.getDatabase(applicationContext)
                                    nostrRecords.forEach { db.maintenanceRecordDao().insert(it) }
                                    val updatedRecords = db.maintenanceRecordDao().getRecordsByOwner(currentPubkey)
                                    adapter.submitList(updatedRecords)
                                    Toast.makeText(this@MainActivity, "Records imported successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .show()
                }
            }
        }
    }
}