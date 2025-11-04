package com.thehotelmedia.android.adapters.dropDown

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.SelectedWeeksTimingItemBinding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

class WeeksAdapter(
    private val context: Context,
    private val weekDaysAndDates: List<Pair<String, String>>, // Accept a list of pairs (dayOfWeek, dayOfMonth)
    private val onDateSelected: (String) -> Unit,
    private val selectedWeek: String
) : RecyclerView.Adapter<WeeksAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1 // Track the selected position


    init {
        val currentDayOfMonth = LocalDate.now().dayOfMonth.toString()
        selectedPosition = weekDaysAndDates.indexOfFirst { it.second == currentDayOfMonth }
        updateSelection(selectedPosition)
    }


    inner class ViewHolder(val binding: SelectedWeeksTimingItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SelectedWeeksTimingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = weekDaysAndDates.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (dayOfWeek, dayOfMonth) = weekDaysAndDates[position]

        val binding = holder.binding

        binding.dayTv.text = dayOfWeek
        binding.dateTv.text = String.format("%02d", dayOfMonth.toInt())

        // Update UI based on selection
        updateUI(binding, position)

        // Handle item click
        holder.itemView.setOnClickListener {
            if (selectedPosition != position) {
                updateSelection(position)
                notifyDateSelected(dayOfMonth.toInt())
            }
        }
    }

    private fun updateUI(binding: SelectedWeeksTimingItemBinding, position: Int) {
        if (position == selectedPosition) {
            binding.root.setBackgroundResource(R.drawable.selected_time_background)
            binding.filterBtn.setCardBackgroundColor(ContextCompat.getColor(context, R.color.insight_selected_background))
            binding.filterBtn.strokeColor = ContextCompat.getColor(context, R.color.insight_selected_stroke)
            binding.dateTv.setTextColor(ContextCompat.getColor(context, R.color.insight_selected_date))
            binding.dayTv.setTextColor(ContextCompat.getColor(context, R.color.insight_selected_day))
        } else {
            binding.root.setBackgroundResource(R.drawable.unselecetd_time_background)
            binding.filterBtn.setCardBackgroundColor(ContextCompat.getColor(context, R.color.insight_unselected_background))
            binding.filterBtn.strokeColor = ContextCompat.getColor(context, R.color.insight_unselected_stroke)
            binding.dateTv.setTextColor(ContextCompat.getColor(context, R.color.insight_unselected_date))
            binding.dayTv.setTextColor(ContextCompat.getColor(context, R.color.insight_unselected_day))
        }
    }

    private fun updateSelection(newPosition: Int) {
        notifyItemChanged(selectedPosition) // Notify the previous item to refresh
        selectedPosition = newPosition
        notifyItemChanged(selectedPosition) // Notify the new item to refresh
    }

    private fun notifyDateSelected(dayOfMonth: Int) {
        val currentDateTime = LocalDate.now().atTime(
            LocalDateTime.now().toLocalTime()
        )
        val selectedDateTime = LocalDateTime.of(
            currentDateTime.year,
            currentDateTime.month,
            dayOfMonth,
            currentDateTime.hour,
            currentDateTime.minute,
            currentDateTime.second,
            currentDateTime.nano
        )
        val isoDateTime = selectedDateTime.atOffset(ZoneOffset.UTC).format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        )
        onDateSelected(isoDateTime)
    }

    fun setSelectedDate(dayOfMonth: String) {
        val position = weekDaysAndDates.indexOfFirst { it.second == dayOfMonth }
        if (position != -1 && position != selectedPosition) {
            updateSelection(position)
            notifyDateSelected(dayOfMonth.toInt())
        }
    }
}
