package com.thehotelmedia.android.adapters.userTypes.individual.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.modals.Business.businessType.Data

class SingleTagAdapter(
    private val context: Context,
    private val tagList: ArrayList<Data>,
    private val onTagSelected: (List<String>) -> Unit
) : RecyclerView.Adapter<SingleTagAdapter.TagViewHolder>() {

    private val selectedTags = mutableSetOf<String>() // For multi-selection


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tagList[position]
        holder.tagTextView.text = tag.name

        Glide.with(context).load(tag.icon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(context, R.color.icon_color_60)))
            .into(holder.iconIv)

        // Highlight if selected
        if (selectedTags.contains(tag.id)) {
            holder.itemView.setBackgroundResource(R.drawable.tag_selected_background)
            holder.tagTextView.setTextColor(ContextCompat.getColor(context, R.color.selected_text_color))
            holder.iconIv.setColorFilter(ContextCompat.getColor(context, R.color.selected_text_color))

        } else {
            holder.itemView.setBackgroundResource(R.drawable.tag_unselected_background)
            holder.tagTextView.setTextColor(ContextCompat.getColor(context, R.color.unselected_text_color))
            holder.iconIv.setColorFilter(ContextCompat.getColor(context, R.color.unselected_text_color))
        }

        // Handle click to toggle selection
        holder.itemView.setOnClickListener {
            if (selectedTags.contains(tag.id)) {
                selectedTags.remove(tag.id)  // Deselect if already selected
            } else {
                selectedTags.add(tag.id.toString())     // Select if not selected
            }

            notifyItemChanged(position)     // Refresh UI for the clicked item
            onTagSelected(selectedTags.toList())  // Notify the selected tags
        }
    }

    override fun getItemCount(): Int {
        return tagList.size
    }

    // Function to unselect all tags
    fun unselectAllTags() {
        selectedTags.clear()  // Clear the selected tags
        notifyDataSetChanged()  // Notify the adapter to refresh the UI
        onTagSelected(selectedTags.toList())  // Notify that no tags are selected
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tagTextView: TextView = itemView.findViewById(R.id.tagTextView)
        val iconIv: ImageView = itemView.findViewById(R.id.icons)
    }
}

