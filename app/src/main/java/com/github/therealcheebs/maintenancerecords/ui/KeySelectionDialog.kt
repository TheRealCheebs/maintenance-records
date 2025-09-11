package com.github.therealcheebs.maintenancerecords.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.therealcheebs.maintenancerecords.databinding.DialogKeySelectionBinding
import com.github.therealcheebs.maintenancerecords.databinding.ItemKeySelectionBinding
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient
import com.github.therealcheebs.maintenancerecords.data.KeyInfo
import android.widget.Toast

class KeySelectionDialog : DialogFragment() {
    
    private var _binding: DialogKeySelectionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: KeySelectionAdapter
    private var selectedKey: KeyInfo? = null
    private var onKeySelected: ((KeyInfo) -> Unit)? = null
    private var onManageKeys: (() -> Unit)? = null
    private var onDialogDismissed: (() -> Unit)? = null
    fun setOnDialogDismissed(listener: () -> Unit) {
        onDialogDismissed = listener
    }
    
    fun setOnKeySelected(listener: (KeyInfo) -> Unit) {
        onKeySelected = listener
    }
    
    fun setOnManageKeys(listener: () -> Unit) {
        onManageKeys = listener
    }
    
    fun setSelectedKey(key: KeyInfo) {
        selectedKey = key
        adapter.setSelectedKey(key.alias)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogKeySelectionBinding.inflate(layoutInflater)
        
        // Setup RecyclerView
        adapter = KeySelectionAdapter { keyInfo ->
            selectedKey = keyInfo
            adapter.setSelectedKey(keyInfo.alias)
        }
        
        binding.recyclerViewKeys.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewKeys.adapter = adapter
        
        // Load keys
        val keys = NostrClient.getAllKeys()
        adapter.submitList(keys)
        
        // Set initial selection
        val currentKeyAlias = NostrClient.getCurrentKeyAlias()
        if (currentKeyAlias != null) {
            val currentKey = keys.find { it.alias == currentKeyAlias }
            if (currentKey != null) {
                selectedKey = currentKey
                adapter.setSelectedKey(currentKey.alias)
            }
        }
        
        // Setup button listeners
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }
        
        binding.buttonManageKeys.setOnClickListener {
            onManageKeys?.invoke()
            dismiss()
        }
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Nostr Key")
            .setView(binding.root)
            .setPositiveButton("OK") { _, _ ->
                selectedKey?.let { key ->
                    onKeySelected?.invoke(key)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }
}

fun showKeySelectionDialog(
    activity: androidx.fragment.app.FragmentActivity,
    onKeySelected: (KeyInfo) -> Unit,
    onManageKeys: () -> Unit,
    onDialogDismissed: () -> Unit
) {
    val dialog = KeySelectionDialog()
    dialog.setOnKeySelected { keyInfo ->
        NostrClient.setCurrentKey(keyInfo.alias)
        NostrClient.loadKeyPairForCurrentKey()
        Toast.makeText(activity, "Using key: ${keyInfo.name}", Toast.LENGTH_SHORT).show()
        onKeySelected(keyInfo)
    }
    dialog.setOnManageKeys {
        onManageKeys()
    }
    dialog.setOnDialogDismissed {
        onDialogDismissed()
    }
    dialog.show(activity.supportFragmentManager, "KeySelectionDialog")
}