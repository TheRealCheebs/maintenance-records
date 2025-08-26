package com.github.therealcheebs.maintenancerecords.ui

import java.util.UUID
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.therealcheebs.maintenancerecords.databinding.ActivityCreateRecordBinding
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import kotlinx.coroutines.launch

class CreateRecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateRecordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveRecord()
        }
    }

    private fun saveRecord() {
        val itemId = binding.editItemId.text.toString()
        val description = binding.editDescription.text.toString()
        val technician = binding.editTechnician.text.toString()
        val cost = binding.editCost.text.toString().toDoubleOrNull() ?: 0.0

        if (itemId.isBlank() || description.isBlank() || technician.isBlank()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val record = MaintenanceRecord(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            description = description,
            technician = technician,
            cost = cost,
            createdAt = System.currentTimeMillis() / 1000
        )

        lifecycleScope.launch {
            try {
                val event = NostrClient.createMaintenanceRecord(record)

                // Save to local database (implementation needed)
                // repository.insertRecord(record)

                Toast.makeText(this@CreateRecordActivity, "Record saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CreateRecordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
