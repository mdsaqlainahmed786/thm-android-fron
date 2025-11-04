package com.thehotelmedia.android.adapters.bookTable

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.SelectedWeeksTimingItemBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SelectedDateAdapter(
    private val context: Context,
    private val weekDaysAndDates: List<Pair<String, String>>, // Accept a list of pairs (dayOfWeek, dayOfMonth)
    private val onDateSelected: (String) -> Unit,
    private val selectedMonth: String,
    private val selectedYear: String,
) : RecyclerView.Adapter<SelectedDateAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1 // Track the selected position

    init {
        val currentDate = LocalDate.now()
        val currentDayOfMonth = currentDate.dayOfMonth.toString()

        val currentMonthNumber = getMonthNumber(selectedMonth)
        val currentYearNumber = selectedYear.toInt()

        if (currentMonthNumber == currentDate.monthValue && currentYearNumber == currentDate.year) {
            selectedPosition = weekDaysAndDates.indexOfFirst { it.second == currentDayOfMonth }
            if (selectedPosition != -1) {
                notifyDateSelected(currentDayOfMonth.toInt()) // Send selected date immediately
            }else{
                onDateSelected("")
            }
        }else{
            onDateSelected("")
        }
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
                notifyDateSelected(dayOfMonth.toInt())
                updateSelection(position)
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
        val monthNumber = getMonthNumber(selectedMonth) ?: return

        // Construct full selected date
        val selectedDate = LocalDate.of(selectedYear.toInt(), monthNumber, dayOfMonth)

        // Format as "YYYY-MM-DD"
        val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        // Send properly formatted date to activity
        onDateSelected(formattedDate)
    }


    /**
     * Converts month name (e.g., "Apr") to its corresponding number (e.g., 4).
     */
    private fun getMonthNumber(monthName: String): Int? {
        val months = mapOf(
            "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4, "May" to 5, "Jun" to 6,
            "Jul" to 7, "Aug" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12
        )
        return months[monthName]
    }
}
