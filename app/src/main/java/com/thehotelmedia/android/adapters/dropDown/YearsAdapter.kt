package com.thehotelmedia.android.adapters.dropDown

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.SelectedYearsTimingItemBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class YearsAdapter(
    private val context: Context,
    private val monthsList: List<Pair<String, String>>, // List of months and their numbers
    private val selectedYear: String?,
    private val onDateSelected: (String) -> Unit
) : RecyclerView.Adapter<YearsAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1 // Track the selected position

    // Map of full month names to abbreviations
    private val monthAbbreviations = mapOf(
        "January" to "Jan",
        "February" to "Feb",
        "March" to "Mar",
        "April" to "Apr",
        "May" to "May",
        "June" to "Jun",
        "July" to "Jul",
        "August" to "Aug",
        "September" to "Sep",
        "October" to "Oct",
        "November" to "Nov",
        "December" to "Dec"
    )

    inner class ViewHolder(val binding: SelectedYearsTimingItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SelectedYearsTimingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return monthsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val month = monthsList[position]

        // Use the map to convert full month name to abbreviation
        val monthAbbr = monthAbbreviations[month.first] ?: month.first // If not found, use the full month name

        binding.dayTv.text = monthAbbr // Set the abbreviated month
        binding.dateTv.text = month.second

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

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition) // Update the previous item
            notifyItemChanged(selectedPosition) // Update the new item
            val month = monthsList[position]

            // Get the selected year and month, and format it
            selectedYear?.let { year ->
                val selectedMonth = month.second // e.g., "01" for January
                val selectedDay = "01" // Set a default day if not available in your list

                // Combine the year, month, and day
                val dateString = "$year-$selectedMonth-$selectedDay"

                // Create a SimpleDateFormat to parse the date string
                val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdfInput.parse(dateString) ?: Date()

                // Format the date to the desired format
                val sdfOutput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val formattedDate = sdfOutput.format(date)

                // Print the formatted year and month
                println("Selected Date: $formattedDate")
                onDateSelected(formattedDate)
            }
        }
    }


    // Helper function to get the current time formatted
    private fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSSXXX", Locale.getDefault())
        return sdf.format(Date())
    }

    fun updateSelectedPositionForCurrentDate() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val currentYear = calendar.get(Calendar.YEAR).toString()

        // Find the position of the item that matches the current month and year
        monthsList.forEachIndexed { index, pair ->
            if (pair.second == String.format("%02d", currentMonth) && selectedYear == currentYear) {
                selectedPosition = index
                notifyDataSetChanged() // Refresh the RecyclerView to highlight the selected item

                // Get the selected month and set a default day
                val selectedMonth = pair.second // e.g., "01" for January
                val selectedDay = "01" // Set a default day if not available in your list

                // Combine the year, month, and day
                val dateString = "$currentYear-$selectedMonth-$selectedDay"

                // Create a SimpleDateFormat to parse the date string
                val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdfInput.parse(dateString) ?: Date()

                // Format the date to the desired format
                val sdfOutput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val formattedDate = sdfOutput.format(date)

                // Print the formatted date
                println("Selected Date: $formattedDate")
                onDateSelected(formattedDate)

                return@forEachIndexed
            }
        }
    }






}
