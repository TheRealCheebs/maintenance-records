package com.github.therealcheebs.maintenancerecords.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.therealcheebs.maintenancerecords.data.LocalNostrEvent
import com.github.therealcheebs.maintenancerecords.databinding.ItemRecordBinding
import com.github.therealcheebs.maintenancerecords.data.toMaintenanceRecordOrNull
import com.github.therealcheebs.maintenancerecords.data.NostrEventState
import java.text.SimpleDateFormat
import java.util.*


class RecordAdapter(
    private val onRecordClick: (LocalNostrEvent) -> Unit
) : ListAdapter<LocalNostrEvent, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordViewHolder(binding)
    }


    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    inner class RecordViewHolder(
        private val binding: ItemRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: LocalNostrEvent) {
            // MaintenanceRecords
            val maintenanceRecord = event.toMaintenanceRecordOrNull()
            if (maintenanceRecord != null) {
                binding.textItem.text = maintenanceRecord.itemId
                binding.textDescription.text = maintenanceRecord.description
                binding.textDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(maintenanceRecord.datePerformed * 1000L)
            } else {
                binding.textItem.text = event.id
                binding.textDescription.text = event.eventType.toString()
                binding.textDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(event.createdAt * 1000L)
            }
            binding.textStatusIndicator.text = event.state.name

            // Set color based on status
            val color = when (event.state) {
                NostrEventState.Failed -> android.graphics.Color.RED
                NostrEventState.Published -> android.graphics.Color.parseColor("#388E3C") // Green
                NostrEventState.Pending -> android.graphics.Color.parseColor("#FFA000") // Yellow
                NostrEventState.Draft -> android.graphics.Color.DKGRAY
            }
            binding.textStatusIndicator.setTextColor(color)

            binding.root.setOnClickListener {
                onRecordClick(event)
            }
        }
    }
}


class RecordDiffCallback : DiffUtil.ItemCallback<LocalNostrEvent>() {
    override fun areItemsTheSame(oldItem: LocalNostrEvent, newItem: LocalNostrEvent): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: LocalNostrEvent, newItem: LocalNostrEvent): Boolean {
        return oldItem == newItem
    }
}
