package com.thehotelmedia.android.adapters.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.ChildAgeItemLayoutBinding

class ChildrenAgeAdapter(
    private val childIndex: Int,
    private val onAgeSelected: (Int, Int?) -> Unit // Callback to notify parent
) : RecyclerView.Adapter<ChildrenAgeAdapter.ChildViewHolder>() {

    private val ages = (1..17).toList() // Generate list of ages from 1 to 18
    private var selectedAge: Int? = null // Track the selected age

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val binding = ChildAgeItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChildViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        val item = ages[position] // Get the current age
        holder.bind(item)

        // Set margin for the last item
        if (position == ages.size - 1) { // Check if it's the last item
            val params = holder.binding.circularTextView.layoutParams as ViewGroup.MarginLayoutParams
            params.rightMargin = 20 // Adjust the margin as needed
            holder.binding.circularTextView.layoutParams = params
        }
    }

    override fun getItemCount(): Int = ages.size // The total number of ages (1 to 18)

    inner class ChildViewHolder(val binding: ChildAgeItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(age: Int) {
            binding.circularTextView.text = age.toString() // Set the age in the TextView

            // Set background drawable based on selection
            val backgroundDrawable = if (age == selectedAge) {
                ContextCompat.getDrawable(itemView.context, R.drawable.selected_age_background)
            } else {
                ContextCompat.getDrawable(itemView.context, R.drawable.unselected_age_background)
            }
            binding.circularTextView.background = backgroundDrawable

            binding.circularTextView.setOnClickListener {
                // Toggle selection: If selected, unselect it; otherwise, select it
                if (selectedAge == age) {
                    selectedAge = null // Unselect if it's already selected
                } else {
                    selectedAge = age // Select the age if it's not selected
                }

                // Notify parent (activity/fragment) with the updated age (null for unselecting)
                onAgeSelected(childIndex, selectedAge)
                notifyDataSetChanged() // Notify RecyclerView that data changed
            }
        }
    }
}

