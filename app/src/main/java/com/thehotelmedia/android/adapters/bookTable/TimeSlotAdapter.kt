package com.thehotelmedia.android.adapters.bookTable

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.ItemTimeSlotBinding


data class TimeSlot(
    val time: String,
    var isSelected: Boolean = false
)
class TimeSlotAdapter(
    private val context: Context,
    private var timeSlots: List<TimeSlot>,
    private val onItemClick: (TimeSlot) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition: Int = -1 // To track selected position

    inner class TimeSlotViewHolder(val binding: ItemTimeSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(timeSlot: TimeSlot, position: Int) {
            binding.txtTimeSlot.text = timeSlot.time

            // Set background based on selection
            if (position == selectedPosition) {
                // Selected color
                binding.cardTimeSlot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.blue_70))
//                binding.txtTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.background_color))

            } else {
                // Unselected color
                binding.cardTimeSlot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background_color))
//                binding.txtTimeSlot.setTextColor(ContextCompat.getColor(context, R.color.background_color))
            }

            binding.root.setOnClickListener {
                if (selectedPosition != position) {
                    val previousSelected = selectedPosition
                    selectedPosition = position

                    notifyItemChanged(previousSelected) // Unselect previous
                    notifyItemChanged(selectedPosition) // Select new

                    onItemClick(timeSlot) // Send callback
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        holder.bind(timeSlots[position], position)
    }

    override fun getItemCount(): Int = timeSlots.size
}
