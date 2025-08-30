package com.github.therealcheebs.maintenancerecords.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.therealcheebs.maintenancerecords.databinding.ActivityRecordDetailBinding
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import java.text.SimpleDateFormat
import java.util.*

class RecordDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordDetailBinding
    private lateinit var record: MaintenanceRecord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recordId = intent.getStringExtra("RECORD_ID") ?: run {
            finish()
            return
        }

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
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
    }

    private fun setupClickListeners() {
        binding.buttonVerify.setOnClickListener {
            val intent = Intent(this, NostrVerifyActivity::class.java).apply {
                putExtra("EVENT_ID", record.nostrEventId)
            }
            startActivity(intent)
        }

        binding.buttonTransfer.setOnClickListener {
            // Implement ownership transfer
            Toast.makeText(this, "Ownership transfer not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
