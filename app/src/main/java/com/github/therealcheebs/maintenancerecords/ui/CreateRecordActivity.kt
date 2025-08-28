package com.github.therealcheebs.maintenancerecords.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.therealcheebs.maintenancerecords.databinding.ActivityCreateRecordBinding
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateRecordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateRecordBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupDateField()
        setupClickListeners()
        setDefaultValues()
    }
    
    private fun setupDateField() {
        // Set current date as default
        updateDateField()
        
        // Make date field clickable
        binding.editDatePerformed.setOnClickListener {
            showDatePicker()
        }
        
        // Make date field focusable
        binding.editDatePerformed.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker()
            }
        }
    }
    
    private fun setDefaultValues() {
        // Set current date
        updateDateField()
        
        // Clear focus from date field
        binding.editDatePerformed.clearFocus()
    }
    
    private fun updateDateField() {
        binding.editDatePerformed.setText(dateFormat.format(calendar.time))
    }
    
    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateField()
        }
        
        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveRecord()
        }
        
        // Add a cancel button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun saveRecord() {
        // Get values from input fields
        val itemId = binding.editItemId.text.toString().trim()
        val description = binding.editDescription.text.toString().trim()
        val technician = binding.editTechnician.text.toString().trim()
        val costString = binding.editCost.text.toString().trim()
        val mileageString = binding.editMileage.text.toString().trim()
        val notes = binding.editNotes.text.toString().trim()
        
        // Validate required fields
        if (itemId.isBlank()) {
            binding.editItemId.error = "Item ID is required"
            binding.editItemId.requestFocus()
            return
        }
        
        if (description.isBlank()) {
            binding.editDescription.error = "Description is required"
            binding.editDescription.requestFocus()
            return
        }
        
        if (technician.isBlank()) {
            binding.editTechnician.error = "Technician is required"
            binding.editTechnician.requestFocus()
            return
        }
        
        // Parse cost
        val cost = if (costString.isBlank()) {
            0.0
        } else {
            costString.toDoubleOrNull() ?: run {
                binding.editCost.error = "Invalid cost"
                binding.editCost.requestFocus()
                return
            }
        }
        
        // Parse mileage (optional)
        val mileage = if (mileageString.isBlank()) {
            null
        } else {
            mileageString.toLongOrNull() ?: run {
                binding.editMileage.error = "Invalid mileage"
                binding.editMileage.requestFocus()
                return
            }
        }
        
        // Get current user's public key
        val currentOwnerPubkey = try {
            NostrClient.getPublicKey()
        } catch (e: Exception) {
            Toast.makeText(this, "Please set up your Nostr key first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create MaintenanceRecord object
        val record = MaintenanceRecord(
            itemId = itemId,
            description = description,
            technician = technician,
            cost = cost,
            datePerformed = calendar.time.time / 1000L, // Store as Unix timestamp
            mileage = mileage,
            notes = notes,
            currentOwnerPubkey = currentOwnerPubkey,
            createdAt = System.currentTimeMillis() / 1000L, // Current time as Unix timestamp
            updatedAt = System.currentTimeMillis() / 1000L // Current time as Unix timestamp
        )
        
        // Save the record
        lifecycleScope.launch {
            try {
                // Create Nostr event and publish
                val event = NostrClient.signMaintenanceRecord(record)
                
                val success = NostrClient.publishToRelays(event)
                if (success) {
                    // Update the record with the event ID
                    
                    // Save to local database (you'll need to implement this)
                    // database.insertRecord(updatedRecord)
                    
                    Toast.makeText(this@CreateRecordActivity, "Record saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CreateRecordActivity, "Failed to publish to Nostr", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CreateRecordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}