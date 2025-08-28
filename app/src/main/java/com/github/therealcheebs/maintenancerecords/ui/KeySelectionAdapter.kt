package com.github.therealcheebs.maintenancerecords.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.therealcheebs.maintenancerecords.databinding.ItemKeySelectionBinding
import com.github.therealcheebs.maintenancerecords.data.KeyInfo

class KeySelectionAdapter(
    private val onKeySelected: (KeyInfo) -> Unit
) : ListAdapter<KeyInfo, KeySelectionAdapter.KeySelectionViewHolder>(KeySelectionDiffCallback()) {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeySelectionViewHolder {
        val binding = ItemKeySelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KeySelectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KeySelectionViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    fun setSelectedKey(alias: String) {
        val keys = currentList
        val position = keys.indexOfFirst { it.alias == alias }
        if (position != -1 && position != selectedPosition) {
            val previousSelected = selectedPosition
            selectedPosition = position
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(position)
        }
    }

    inner class KeySelectionViewHolder(
        private val binding: ItemKeySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(keyInfo: KeyInfo, isSelected: Boolean) {
            binding.textKeyName.text = keyInfo.name
            binding.textKeyAlias.text = keyInfo.alias
            binding.textPublicKey.text = keyInfo.publicKey.take(16) + "..."
            
            // Show default badge if this key is the default
            binding.textDefaultBadge.visibility = if (keyInfo.isDefault) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Set radio button state
            binding.radioButton.isChecked = isSelected
            
            // Set up click listeners
            binding.root.setOnClickListener {
                onKeySelected(keyInfo)
            }
            
            binding.radioButton.setOnClickListener {
                onKeySelected(keyInfo)
            }
        }
    }
}

class KeySelectionDiffCallback : DiffUtil.ItemCallback<KeyInfo>() {
    override fun areItemsTheSame(oldItem: KeyInfo, newItem: KeyInfo): Boolean {
        return oldItem.alias == newItem.alias
    }

    override fun areContentsTheSame(oldItem: KeyInfo, newItem: KeyInfo): Boolean {
        return oldItem == newItem
    }
}