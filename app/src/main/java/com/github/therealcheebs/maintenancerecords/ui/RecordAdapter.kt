package com.github.therealcheebs.maintenancerecords.ui


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.therealcheebs.maintenancerecords.data.MaintenanceRecord
import com.github.therealcheebs.maintenancerecords.databinding.ItemRecordBinding
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(
    private val onRecordClick: (MaintenanceRecord) -> Unit
) : ListAdapter<MaintenanceRecord, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

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

        fun bind(record: MaintenanceRecord) {
            binding.textItem.text = record.itemId
            binding.textDescription.text = record.description
            // Use createdAt (seconds) to display date with year
            val date = Date(record.createdAt * 1000L)
            binding.textDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)

            binding.root.setOnClickListener {
                onRecordClick(record)
            }
        }
    }
}

class RecordDiffCallback : DiffUtil.ItemCallback<MaintenanceRecord>() {
    override fun areItemsTheSame(oldItem: MaintenanceRecord, newItem: MaintenanceRecord): Boolean {
        return oldItem.itemId == newItem.itemId
    }

    override fun areContentsTheSame(oldItem: MaintenanceRecord, newItem: MaintenanceRecord): Boolean {
        return oldItem == newItem
    }
}
