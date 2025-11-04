package com.thehotelmedia.android.adapters.userTypes.individual.forms

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.activity.userTypes.forms.createPost.TagPeople
import com.thehotelmedia.android.databinding.TaggedUserItemBinding

class TagsAdapter(
    context: Context,
    private val tagList: ArrayList<TagPeople>,
    private val onListUpdated: (ArrayList<TagPeople>) -> Unit
) : RecyclerView.Adapter<TagsAdapter.TagViewHolder>() {


    inner class TagViewHolder(val binding: TaggedUserItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = TaggedUserItemBinding.inflate(
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
        binding.tagTextView.text = tag.name

        // Handle cancel button click
        binding.cancelBtn.setOnClickListener {
            // Remove the item from the list
            tagList.removeAt(position)
            // Notify the adapter about item removal
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, tagList.size)
            // Call the onListUpdated callback with the updated list
            onListUpdated(tagList)
        }


    }
}