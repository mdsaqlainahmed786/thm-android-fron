package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.ChildrenItemLayoutBinding

class ChildrenAdapter(
    private val context: Context,
    private val childrenNumber: Int,
    private val onChildrenAgesUpdated: (List<Int>) -> Unit // Callback to update the selected ages list
) : RecyclerView.Adapter<ChildrenAdapter.ParentViewHolder>() {

    private val childrenAges: MutableList<Int?> = MutableList(childrenNumber) { null } // Store selected ages

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentViewHolder {
        val binding = ChildrenItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParentViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = childrenNumber

    inner class ParentViewHolder(private val binding: ChildrenItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(childIndex: Int) {
            val formattedString = context.getString(R.string.child_age_with_index, childIndex + 1)
            binding.textViewTitle.text = formattedString

            val childAdapter = ChildrenAgeAdapter(childIndex) { selectedChildIndex, selectedAge ->
                // Update the selected age in the list
                childrenAges[selectedChildIndex] = selectedAge

                // Notify parent with updated age list (filter out null values)
                onChildrenAgesUpdated(childrenAges.filterNotNull())
            }
            binding.childrenAgeRv.adapter = childAdapter
        }
    }
}



