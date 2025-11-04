package com.thehotelmedia.android.adapters.authentication.business

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R

class TagAdapter(
    private val tagList: List<String>,
    private val onTagSelected: (List<String>) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    private val selectedTags = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tagList[position]
        holder.tagTextView.text = tag

        // Set background color based on whether the item is selected or not
        if (selectedTags.contains(tag)) {
            holder.itemView.setBackgroundResource(R.drawable.tag_selected_background) // Blue background for selected
        } else {
            holder.itemView.setBackgroundResource(R.drawable.tag_unselected_background) // Default background
        }

        // Handle item click
        holder.itemView.setOnClickListener {
            if (selectedTags.contains(tag)) {
                selectedTags.remove(tag)
            } else {
                selectedTags.add(tag)
            }
            notifyItemChanged(position)
            onTagSelected(selectedTags.toList()) // Notify the activity of the selected tags
        }
    }

    override fun getItemCount(): Int {
        return tagList.size
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tagTextView: TextView = itemView.findViewById(R.id.tagTextView)
    }
}
