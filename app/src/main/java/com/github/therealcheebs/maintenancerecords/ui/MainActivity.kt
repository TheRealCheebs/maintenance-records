package com.github.therealcheebs.maintenancerecords.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.therealcheebs.maintenancerecords.databinding.ActivityMainBinding
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecordDatabase
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEventRepository
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

        // Ensure NostrClient is initialized only once
        NostrClient.initialize(applicationContext)

        // Show active key alias/name at the top and in toolbar
        val keyInfo = NostrClient.getCurrentKeyInfo()
        val aliasOrName = keyInfo?.name ?: "Unknown Key"
        binding.toolbar.title = "Maintenance Records - $aliasOrName"

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
            NostrClient.loadKeyPairForCurrentKey()
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
                putExtra("RECORD_ID", record.id)
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
    
    private fun loadRecordsForCurrentKey() {
        val currentKey = NostrClient.getCurrentKeyInfo()
        if (currentKey == null) {
            Toast.makeText(this, "No active key found. Please set up a key.", Toast.LENGTH_LONG).show()
            return
        }
        val currentPubkey = currentKey.publicKey
        val db = MaintenanceRecordDatabase.getDatabase(applicationContext)
        val eventDao = db.localNostrEventDao()
        val eventRepo = LocalNostrEventRepository(eventDao)
        val emptyStateView = findViewById<android.view.View>(R.id.emptyStateView)
        val emptyStateText = emptyStateView?.findViewById<android.widget.TextView>(R.id.emptyStateText)
        val btnCreateRecord = findViewById<android.widget.Button>(R.id.btnCreateRecord)
        val btnTransferOwnership = findViewById<android.widget.Button>(R.id.btnTransferOwnership)

        lifecycleScope.launch {
            val events = eventRepo.getAllByPubkey(currentPubkey)
            adapter.submitList(events)
            if (events.isEmpty()) {
                emptyStateText?.text = "No records found for ${currentKey.name}"
                ImportRecordsDialogFragment(
                    eventRepo = eventRepo,
                    pubkey = currentPubkey,
                    onRecordsImported = {
                        lifecycleScope.launch {
                            val updatedEvents = eventRepo.getAllByPubkey(currentPubkey)
                            adapter.submitList(updatedEvents)
                            if (updatedEvents.isEmpty()) {
                                emptyStateView?.visibility = android.view.View.VISIBLE
                                emptyStateText?.text = "No records found for ${currentKey.name}"
                            } else {
                                emptyStateView?.visibility = android.view.View.GONE
                            }
                        }
                    }
                ).show(supportFragmentManager, "ImportRecordsDialogFragment")
                emptyStateView?.visibility = android.view.View.VISIBLE
            } else {
                emptyStateView?.visibility = android.view.View.GONE
            }
        }

        btnCreateRecord?.setOnClickListener {
            startActivity(Intent(this, CreateRecordActivity::class.java))
        }
        btnTransferOwnership?.setOnClickListener {
            // TODO: Replace with your OwnershipTransferActivity or logic
            Toast.makeText(this, "Transfer Ownership selected", Toast.LENGTH_SHORT).show()
        }
    }
}