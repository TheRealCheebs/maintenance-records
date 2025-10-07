package com.github.therealcheebs.maintenancerecords.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.therealcheebs.maintenancerecords.databinding.ActivityNostrVerifyBinding
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import kotlinx.coroutines.launch
import com.github.therealcheebs.maintenancerecords.R

class NostrVerifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNostrVerifyBinding
    private lateinit var eventId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNostrVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ToolbarHelper.setupToolbar(
            activity = this,
            toolbar = binding.toolbar,
            title = "Verify Record",
            showBackButton = true,
            onMenuItemClick = { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_key_manager -> {
                        val intent = Intent(this, NostrKeyManagerActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    // should there be action_select_key here?
                    else -> false
                }
           }
        )

        eventId = intent.getStringExtra("EVENT_ID") ?: run {
            finish()
            return
        }

        binding.textEventId.text = eventId
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonVerify.setOnClickListener {
            verifyOnNostr()
        }
    }

    private fun verifyOnNostr() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.buttonVerify.isEnabled = false

        lifecycleScope.launch {
            try {
                //val isVerified = NostrClient.verifyRecord(eventId)
                val isVerified = false // Placeholder until implemented

                binding.progressBar.visibility = android.view.View.GONE
                binding.buttonVerify.isEnabled = true

                if (isVerified) {
                    binding.textVerificationResult.text = "✓ Record verified on Nostr"
                    binding.textVerificationResult.setTextColor(
                        resources.getColor(android.R.color.holo_green_dark, null)
                    )
                } else {
                    binding.textVerificationResult.text = "✗ Verification failed"
                    binding.textVerificationResult.setTextColor(
                        resources.getColor(android.R.color.holo_red_dark, null)
                    )
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.buttonVerify.isEnabled = true
                binding.textVerificationResult.text = "Error: ${e.message}"
                binding.textVerificationResult.setTextColor(
                    resources.getColor(android.R.color.holo_red_dark, null)
                )
            }
        }
    }
}
