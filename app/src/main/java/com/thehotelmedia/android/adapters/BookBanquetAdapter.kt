package com.thehotelmedia.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.FeaturesItemsLayoutBinding

class BookBanquetAdapter(
    private val context: Context,
    private val items: List<String>,
    private val onItemSelected: (String) -> Unit // Lambda function to return selected item
) : RecyclerView.Adapter<BookBanquetAdapter.ViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION // Initially, nothing is selected

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FeaturesItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding

        binding.tvItem.text = items[position]

        // Update the selection icon
        if (position == selectedPosition) {
            binding.imageView.setImageResource(R.drawable.ic_round_selected_checkbox) // Selected icon
        } else {
            binding.imageView.setImageResource(R.drawable.ic_round_unselected_checkbox) // Unselected icon
        }


        // Handle item click
        binding.root.setOnClickListener {
            val previousSelectedPosition = selectedPosition
            selectedPosition = position

            // Notify changes to update UI
            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(selectedPosition)

            // Pass selected item back to the activity
            onItemSelected(items[position])
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(val binding: FeaturesItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)
}
