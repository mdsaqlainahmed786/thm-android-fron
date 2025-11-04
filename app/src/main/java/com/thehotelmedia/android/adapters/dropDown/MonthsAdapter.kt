package com.thehotelmedia.android.adapters.dropDown

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.SelectedMonthsTimingItemBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MonthsAdapter(
    private val context: Context,
    private val weeksList: List<Pair<String, Pair<String, String>>>,
    private val selectedMonth: String,
    private val onTimeSelected: (startTime: String, endTime: String) -> Unit
) : RecyclerView.Adapter<MonthsAdapter.ViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    init {
        selectedPosition = getSelectedPosition().also { position ->
            if (position != RecyclerView.NO_POSITION) {
                val week = weeksList[position]
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val startDate = String.format("%02d", week.second.first.toIntOrNull() ?: 1)
                val endDate = String.format("%02d", week.second.second.toIntOrNull() ?: 1)

                val formattedStartDate = getFormattedDate(currentYear, selectedMonth, startDate)
                val formattedEndDate = getFormattedDate(currentYear, selectedMonth, endDate)

                // Invoke the lambda callback with the formatted dates for the initially selected item
                onTimeSelected(formattedStartDate, formattedEndDate)
            }
        }
    }

    inner class ViewHolder(val binding: SelectedMonthsTimingItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SelectedMonthsTimingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return weeksList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val week = weeksList[position]
        binding.dayTv.text = week.first

        val startDate = String.format("%02d", week.second.first.toIntOrNull() ?: 1)
        val endDate = String.format("%02d", week.second.second.toIntOrNull() ?: 1)

        binding.dateTv.text = "$startDate - $endDate"

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

        binding.root.setOnClickListener {
            notifyItemChanged(selectedPosition)
            selectedPosition = position
            notifyItemChanged(selectedPosition)

            // Get the current year
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            // Format start date and end date
            val formattedStartDate = getFormattedDate(currentYear, selectedMonth, startDate)
            val formattedEndDate = getFormattedDate(currentYear, selectedMonth, endDate)

            // Invoke the lambda callback with the formatted dates
            onTimeSelected(formattedStartDate, formattedEndDate)
        }

        // Set default padding
        val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = 0

        // Add padding to the first item
        if (position == 0) {
            layoutParams.marginStart = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
        }

        binding.root.layoutParams = layoutParams
    }

    private fun getFormattedDate(year: Int, monthName: String, day: String): String {
        val month = getMonthNumber(monthName)

        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day.toIntOrNull() ?: 1)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return dateFormat.format(calendar.time)
    }

    private fun getMonthNumber(monthName: String): Int {
        return when (monthName) {
            "January" -> 1
            "February" -> 2
            "March" -> 3
            "April" -> 4
            "May" -> 5
            "June" -> 6
            "July" -> 7
            "August" -> 8
            "September" -> 9
            "October" -> 10
            "November" -> 11
            "December" -> 12
            else -> 1
        }
    }

    private fun getSelectedPosition(): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentDate = Calendar.getInstance().time

        for ((index, week) in weeksList.withIndex()) {
            val startDate = week.second.first.toIntOrNull() ?: 1
            val endDate = week.second.second.toIntOrNull() ?: 1

            val startFormattedDate = getFormattedDate(currentYear, selectedMonth, startDate.toString())
            val endFormattedDate = getFormattedDate(currentYear, selectedMonth, endDate.toString())

            val startCalendar = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).parse(startFormattedDate)
            val endCalendar = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).parse(endFormattedDate)

            if (currentDate.after(startCalendar) && currentDate.before(endCalendar)) {
                return index
            }
        }

        return RecyclerView.NO_POSITION
    }
}
