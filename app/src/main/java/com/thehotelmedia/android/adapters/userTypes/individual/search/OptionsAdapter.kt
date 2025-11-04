package com.thehotelmedia.android.adapters.userTypes.individual.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.OptionsItemTagBinding


class OptionsAdapter(
    private val context: Context,
    private val tagList: List<String>,
    private val onTagSelected: (String?) -> Unit
) : RecyclerView.Adapter<OptionsAdapter.TagViewHolder>() {

    private var selectedTag: String? = null

    inner class TagViewHolder(val binding: OptionsItemTagBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = OptionsItemTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return tagList.size
    }
    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val binding = holder.binding
        val tag = tagList[position]
        binding.tagTextView.text = tag

        // Set background color based on whether the item is selected or not
        if (tag == selectedTag) {
            holder.itemView.setBackgroundResource(R.drawable.tag_selected_background_without_stroke) // Blue background for selected
            binding.tagTextView.setTextColor(ContextCompat.getColor(context, R.color.selected_text_color))

        } else {
            holder.itemView.setBackgroundResource(R.drawable.tag_unselected_background_without_stroke) // Default background
            binding.tagTextView.setTextColor(ContextCompat.getColor(context, R.color.unselected_text_color))

        }

        // Handle item click
        holder.itemView.setOnClickListener {
            if (tag != selectedTag) { // Only change selection if the tag is different
                val previousSelectedTag = selectedTag
                selectedTag = tag
                // Notify the previous selected tag to update its view
                notifyItemChanged(tagList.indexOf(previousSelectedTag))
                // Notify the current selected tag to update its view
                notifyItemChanged(position)
                onTagSelected(selectedTag) // Notify the activity of the selected tag
            }
        }
    }

    // Method to update the selected tag
    fun setSelectedTag(tag: String) {
        val previousSelectedTag = selectedTag
        selectedTag = tag

        notifyItemChanged(tagList.indexOf(previousSelectedTag)) // Update the previous selected item
        notifyItemChanged(tagList.indexOf(selectedTag)) // Update the new selected item
    }


}