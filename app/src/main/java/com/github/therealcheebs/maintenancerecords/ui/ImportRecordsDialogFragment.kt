package com.github.therealcheebs.maintenancerecords.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEventRepository
import com.github.therealcheebs.maintenancerecords.data.RecordLoader
import kotlinx.coroutines.launch

class ImportRecordsDialogFragment(
    private val eventRepo: LocalNostrEventRepository,
    private val pubkey: String,
    private val onRecordsImported: (() -> Unit)? = null
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("No Records Found")
            .setMessage("There is no record activity for this key.")
            .setPositiveButton("OK", null)
            .setNegativeButton("Import Records") { _, _ ->
                lifecycleScope.launch {
                    val ctx = context ?: return@launch
                    try {
                        RecordLoader.importRecordsFromRelays(eventRepo, pubkey)
                        Toast.makeText(ctx, "Records imported successfully", Toast.LENGTH_SHORT).show()
                        onRecordsImported?.invoke()
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .create()
    }
}
