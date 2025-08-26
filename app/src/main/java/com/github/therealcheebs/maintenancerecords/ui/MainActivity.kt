package com.github.therealcheebs.maintenancerecords.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.therealcheebs.maintenancerecords.databinding.ActivityMainBinding
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
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

        // Load sample data (replace with actual data loading)
        loadSampleData()
    }

    private fun setupClickListeners() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, CreateRecordActivity::class.java))
        }
    }

    private fun loadSampleData() {
        val sampleRecords = listOf(
            MaintenanceRecord(
                id = "rec_001",
                itemId = "vehicle_001",
                description = "Oil change",
                technician = "Quick Lube",
                cost = 75.00,
                createdAt = System.currentTimeMillis() / 1000 - 86400,
                verified = false
            ),
            MaintenanceRecord(
                id = "rec_002",
                itemId = "vehicle_001",
                description = "Tire rotation",
                technician = "Tire Shop",
                cost = 50.00,
                createdAt = System.currentTimeMillis() / 1000 - 172800,
                verified = true
            )
        )

        adapter.submitList(sampleRecords)
    }
}
