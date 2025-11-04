package com.thehotelmedia.android.adapters.userTypes.individual.forms.tagPeople

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.userTypes.forms.createPost.TagPeople
import com.thehotelmedia.android.databinding.SelectedTagPeopleItemsLayoutBinding
import java.util.ArrayList

class SelectedTagPeopleAdapter(
    private val context: Context,
    private val  tagPeople: ArrayList<TagPeople>,
    private val onItemUnselected: (TagPeople) -> Unit,
    private val onListUpdated: (ArrayList<TagPeople>) -> Unit
) : RecyclerView.Adapter<SelectedTagPeopleAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: SelectedTagPeopleItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SelectedTagPeopleItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return tagPeople.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        setFrontPadding(binding,position)

        val profilePic = tagPeople[position].profilePic
        val name = tagPeople[position].name
        binding.nameTv.text = name

        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.imageView)

        onListUpdated(tagPeople)


        binding.cancelBtn.setOnClickListener {
            val removedItem = tagPeople[position]
            tagPeople.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, tagPeople.size)

            // Notify the TagPeopleListAdapter that the item has been unselected
            onItemUnselected(removedItem)

            onListUpdated(tagPeople)
        }


    }

    private fun setFrontPadding(binding: SelectedTagPeopleItemsLayoutBinding, position: Int) {
        // Set default padding
        val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = 0

        // Add padding to the first item
        if (position == 0) {
            layoutParams.marginStart = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
        }
        binding.root.layoutParams = layoutParams
    }

}