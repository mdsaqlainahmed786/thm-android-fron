package com.thehotelmedia.android.adapters.dropDown

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.MultipleSelectedItemsBinding
import com.thehotelmedia.android.R

class TimeingAdapter(
    private val context: Context,
    private val options: List<String>,
    private val onSelected: (Int) -> Unit,
    private val selectedTiming: String?
) : RecyclerView.Adapter<TimeingAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1 // Track the selected item

    inner class ViewHolder(val binding: MultipleSelectedItemsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MultipleSelectedItemsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return options.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val option = options[position]

        // Set the text
        binding.text.text = option

        // Check if the option matches selectedTiming
        if (option == selectedTiming) {
            // Change background and text color for selected item
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.selected_text_popup_background_selected)
        } else {
            // Change background and text color for unselected item
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.selected_text_popup_background_unselected)
        }

        // Set click listener to handle item selection
        holder.itemView.setOnClickListener {
            onSelected(position) // Notify when an item is selected

            // Update the selected position and refresh the adapter
            val previousSelectedPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousSelectedPosition) // Refresh previous item
            notifyItemChanged(selectedPosition) // Refresh newly selected item
        }
    }
}
