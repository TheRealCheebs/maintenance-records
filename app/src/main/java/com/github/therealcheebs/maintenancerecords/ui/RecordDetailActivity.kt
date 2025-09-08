package com.github.therealcheebs.maintenancerecords.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.therealcheebs.maintenancerecords.databinding.ActivityRecordDetailBinding
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecordDatabase
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEventRepository
import com.github.therealcheebs.maintenancerecords.data.toMaintenanceRecordOrNull
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEvent
import com.github.therealcheebs.maintenancerecords.R
import java.text.SimpleDateFormat
import kotlinx.coroutines.launch
import java.util.*
import com.google.android.material.appbar.MaterialToolbar
import android.view.Menu


class RecordDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val eventId = intent.getStringExtra("RECORD_ID") ?: run {
            finish()
            return
        }

        val db = MaintenanceRecordDatabase.getDatabase(applicationContext)
        val eventDao = db.localNostrEventDao()
        val eventRepo = LocalNostrEventRepository(eventDao)

        ToolbarHelper.setupToolbar(
            activity = this,
            toolbar = binding.toolbar,
            title = "Record Details",
            showBackButton = true,
            onMenuItemClick = { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_key_manager -> {
                        val intent = Intent(this, NostrKeyManagerActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
           }
        )

        lifecycleScope.launch {
            val event = eventRepo.getById(eventId)
            if (event == null) {
                Toast.makeText(this@RecordDetailActivity, "Record not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            val record = event.toMaintenanceRecordOrNull()
            setupViews(record, event)
            setupClickListeners(record)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupViews(record: MaintenanceRecord?, event: LocalNostrEvent) {
        if (record != null) {
            binding.textItemId.text = record.itemId
            binding.textDescription.text = record.description
            binding.textTechnician.text = record.technician
            binding.textCost.text = "$${record.cost}"
            val date = Date(record.createdAt * 1000L)
            binding.textDate.text = SimpleDateFormat("MMM dd yyyy", Locale.getDefault()).format(date)
            binding.textVerificationStatus.text = if (record.verified) {
                "Verified on Nostr"
            } else {
                "Not verified"
            }
        } else {
            binding.textItemId.text = event.id
            binding.textDescription.text = event.eventType.toString()
            binding.textTechnician.text = ""
            binding.textCost.text = ""
            val date = Date(event.createdAt * 1000L)
            binding.textDate.text = SimpleDateFormat("MMM dd yyyy", Locale.getDefault()).format(date)
            binding.textVerificationStatus.text = event.state.name
        }
    }

    private fun setupClickListeners(record: MaintenanceRecord?) {
        binding.buttonVerify.setOnClickListener {
            val eventId = record?.nostrEventId ?: return@setOnClickListener
            val intent = Intent(this, NostrVerifyActivity::class.java).apply {
                putExtra("EVENT_ID", eventId)
            }
            startActivity(intent)
        }

        binding.buttonPublish.setOnClickListener {
            // Implement publish
            Toast.makeText(this, "publish per notes isn't implemented yet", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}
