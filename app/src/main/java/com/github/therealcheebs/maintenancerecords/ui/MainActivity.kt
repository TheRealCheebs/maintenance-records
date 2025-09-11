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
import com.github.therealcheebs.maintenancerecords.workers.RetryPublishWorker
import com.github.therealcheebs.maintenancerecords.R
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import java.util.Date
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.BackoffPolicy
import androidx.work.WorkManager
import com.google.android.material.appbar.MaterialToolbar
import android.view.Menu


class MainActivity : AppCompatActivity() {
    private var keySelected = false

    override fun onResume() {
        super.onResume()
        updateToolbarTitle()
        loadRecordsForCurrentKey()
    }
    private val MIN_BACKOFF_MILLIS = 10_000L // 10 seconds

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure NostrClient is initialized only once
        NostrClient.initialize(applicationContext)

        // Check if we need to set up keys
        if (NostrClient.needsKeySetup()) {
            // Redirect to key manager
            val intent = Intent(this, NostrKeyManagerActivity::class.java)
            intent.putExtra("first_time", true)
            startActivity(intent)
            finish()
            return
        }

        // If returning from key creation, mark key as selected
        val keyJustCreated = intent.getBooleanExtra("key_just_created", false)
        if (keyJustCreated && NostrClient.getCurrentKeyInfo() != null) {
            keySelected = true
        }

        // Show key selection dialog if keys exist and key not selected
        if (NostrClient.getAllKeys().size > 0 && !keySelected) {
            showKeySelectionDialog(
                this,
                onKeySelected = { keyInfo ->
                    keySelected = true
                    loadRecordsForCurrentKey()
                    updateToolbarTitle()
                },
                onManageKeys = {
                    val intent = Intent(this, NostrKeyManagerActivity::class.java)
                    startActivity(intent)
                },
                onDialogDismissed = {
                    // Only load records if a key was selected
                    if (NostrClient.getCurrentKeyInfo() != null) {
                        keySelected = true
                        loadRecordsForCurrentKey()
                        updateToolbarTitle()
                    }
                }
            )
        }

        ToolbarHelper.setupToolbar(
            activity = this,
            toolbar = binding.toolbar,
            title = "Maintenance Records",
            showBackButton = false,
            onMenuItemClick = { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_key_manager -> {
                        startActivity(Intent(this, NostrKeyManagerActivity::class.java))
                        true
                    }
                    R.id.action_select_key -> {
                        showKeySelectionDialog(
                            this,
                            onKeySelected = { keyInfo ->
                                loadRecordsForCurrentKey()
                                updateToolbarTitle()
                            },
                            onManageKeys = {
                                val intent = Intent(this, NostrKeyManagerActivity::class.java)
                                startActivity(intent)
                            },
                            onDialogDismissed = {
                                loadRecordsForCurrentKey()
                                updateToolbarTitle()
                            }
                        )
                        true
                    }
                    else -> false
                }
            }
        )
        setupRecyclerView()
        setupClickListeners()
        scheduleRetryPublishWorker()
    }

    private fun updateToolbarTitle() {
        val keyName = NostrClient.getCurrentKeyInfo()?.name
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        if (keyName != null) {
            toolbar.title = "Maintenance Records - $keyName"
        } else {
            toolbar.title = "Maintenance Records"
        }
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

    }

    private fun loadRecordsForCurrentKey() {
        val currentKey = NostrClient.getCurrentKeyInfo()
        val emptyStateView = findViewById<android.view.View>(R.id.emptyStateView)
        val emptyStateText = emptyStateView?.findViewById<android.widget.TextView>(R.id.emptyStateText)
        val btnCreateRecord = findViewById<android.widget.Button>(R.id.btnCreateRecord)
        val btnTransferOwnership = findViewById<android.widget.Button>(R.id.btnTransferOwnership)

        if (currentKey == null) {
            adapter.submitList(emptyList())
            emptyStateView?.visibility = android.view.View.VISIBLE
            emptyStateText?.text = "No key selected. Please set up or select a key."
            btnCreateRecord?.setOnClickListener {
                Toast.makeText(this, "Please select a key before creating a record.", Toast.LENGTH_SHORT).show()
            }
            btnTransferOwnership?.setOnClickListener {
                Toast.makeText(this, "Please select a key before transferring ownership.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val currentPubkey = currentKey.publicKey
        val db = MaintenanceRecordDatabase.getDatabase(applicationContext)
        val eventDao = db.localNostrEventDao()
        val eventRepo = LocalNostrEventRepository(eventDao)

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

    private fun scheduleRetryPublishWorker() {
        val workRequest = OneTimeWorkRequestBuilder<RetryPublishWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}
