package com.github.therealcheebs.maintenancerecords.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.therealcheebs.maintenancerecords.databinding.ItemKeyBinding
import com.github.therealcheebs.maintenancerecords.data.KeyInfo
import com.github.therealcheebs.maintenancerecords.nostr.NostrClient

class KeyAdapter(
    private val onKeyAction: (KeyInfo, Action) -> Unit
) : ListAdapter<KeyInfo, KeyAdapter.KeyViewHolder>(KeyDiffCallback()) {

    enum class Action {
        SELECT, DELETE, RENAME, EXPORT, SET_DEFAULT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyViewHolder {
        val binding = ItemKeyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KeyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KeyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class KeyViewHolder(
        private val binding: ItemKeyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(keyInfo: KeyInfo) {
            binding.textKeyName.text = keyInfo.name
            binding.textKeyAlias.text = keyInfo.alias
            binding.textPublicKey.text = keyInfo.publicKey.take(16) + "..."

            // Show current selection
            val currentAlias = NostrClient.getCurrentKeyAlias()
            binding.cardKey.isSelected = (keyInfo.alias == currentAlias)

            // Show default badge
            binding.textDefaultBadge.visibility = if (keyInfo.isDefault) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Set up click listeners
            binding.cardKey.setOnClickListener {
                onKeyAction(keyInfo, Action.SELECT)
            }

            binding.buttonSelect.setOnClickListener {
                onKeyAction(keyInfo, Action.SELECT)
            }

            binding.buttonDelete.setOnClickListener {
                onKeyAction(keyInfo, Action.DELETE)
            }

            binding.buttonRename.setOnClickListener {
                onKeyAction(keyInfo, Action.RENAME)
            }

            binding.buttonExport.setOnClickListener {
                onKeyAction(keyInfo, Action.EXPORT)
            }

            binding.buttonSetDefault.setOnClickListener {
                onKeyAction(keyInfo, Action.SET_DEFAULT)
            }
        }
    }
}

class KeyDiffCallback : DiffUtil.ItemCallback<KeyInfo>() {
    override fun areItemsTheSame(oldItem: KeyInfo, newItem: KeyInfo): Boolean {
        return oldItem.alias == newItem.alias
    }

    override fun areContentsTheSame(oldItem: KeyInfo, newItem: KeyInfo): Boolean {
        return oldItem == newItem
    }
}
