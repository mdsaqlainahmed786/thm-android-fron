package com.thehotelmedia.android.adapters.userTypes.individual.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.stories.ViewStoriesActivity
import com.thehotelmedia.android.activity.userTypes.forms.createStory.CreateStoryActivity
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.StoryItemsLayoutBinding
import com.thehotelmedia.android.databinding.HeaderItemLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.Stories.Stories

class StoryAdapter(private val context: Context,private val userProfilePic: String) : PagingDataAdapter<Stories, RecyclerView.ViewHolder>(StoriesDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    // ViewHolder for list items
    inner class ItemViewHolder(val binding: StoryItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder for header (static image)
    inner class HeaderViewHolder(val binding: HeaderItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    // Override getItemCount to return the correct item count, including one header
    override fun getItemCount(): Int {
        val itemCount = super.getItemCount()
        return if (itemCount > 0) itemCount - 1 else 0  // Exclude the header
    }

    override fun getItemViewType(position: Int): Int {
        // If position is 0, it's the header; otherwise, it's an item
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = HeaderItemLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = StoryItemsLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ItemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val story = getItem(position)

        if (holder is HeaderViewHolder) {
            // Bind the first item data to the header
            story?.let {



                if (it.storiesRef.isEmpty()){
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.et_stroke)
                }else{
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.blue)
                }
                Glide.with(context).load(userProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(holder.binding.imageView)
                // You can bind the first item data to the header view here
                // Example: holder.binding.headerTextView.text = it.title
            }

            // Handle click on header (static image)
            holder.binding.root.setOnClickListener {

                if (story?.storiesRef?.isEmpty() == false){
                    val firstStory = snapshot().items.firstOrNull()
                    firstStory?.let {
                        val gson = Gson()
                        val jsonString = gson.toJson(listOf(it))
                        val intent = Intent(context, ViewStoriesActivity::class.java).apply {
                            putExtra("StoriesJson", jsonString)
                        }
                        context.startActivity(intent)
                    }
                }else{
                    Toast.makeText(context,MessageStore.noStoriesAvailable(context),Toast.LENGTH_SHORT).show()
                }



//                // Fetch the story at position 0
//                val firstStory = snapshot().firstOrNull()
//                if (firstStory != null) {
//                    // Convert the single object to JSON
//                    val gson = Gson()
//                    var jsonString = gson.toJson(firstStory)
//                    jsonString = "[$jsonString]"
//                    println("sadjfgajsg  jsonString $jsonString")
//
//                    // Pass the JSON string to the next activity
//                    val intent = Intent(context, ViewStoriesActivity::class.java)
//                    intent.putExtra("StoriesJson", jsonString)
//                    context.startActivity(intent)
//                } else {
//                    println("sadjfgajsg position $position is null")
//                }

            }

            holder.binding.addStoryBtn.setOnClickListener {
                val intent = Intent(context, CreateStoryActivity::class.java)
                context.startActivity(intent)
            }



        } else if (holder is ItemViewHolder) {
            // Bind the remaining stories to item views
            story?.let {

                val accountType = it.accountType?.capitalizeFirstLetter().toString()
                val name = it.name
                val stories = it.storiesRef
                var profilePic = ""
                if (accountType == business_type_individual){
                    profilePic = it.profilePic?.large.toString()
                }else{
                    profilePic = it.businessProfileRef?.profilePic?.large.toString()
                }

                val seenByMe = it.seenByMe ?: false

                println("asdnfkashdfjk    $seenByMe")

                if (seenByMe){
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.et_stroke)
                }else{
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.blue)
                }



                Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(holder.binding.imageView)

                // Handle item click
                holder.itemView.setOnClickListener {

                    if (story.storiesRef.isNotEmpty()){
                        val storiesList = snapshot().items.subList(position, snapshot().items.size)
                        val jsonString = Gson().toJson(storiesList)
                        val intent = Intent(context, ViewStoriesActivity::class.java).apply {
                            putExtra("StoriesJson", jsonString)
                        }
                        context.startActivity(intent)
                    }else{
                        Toast.makeText(context,MessageStore.noStoriesAvailable(context),Toast.LENGTH_SHORT).show()
                    }





//                    // Get the entire list of stories
//                    val storiesList = snapshot() // Use snapshot() to get the full list in a PagingDataAdapter
//
//                    if (storiesList.isNotEmpty()) {
//                        // Get the sublist from the clicked position to the last item
//                        val subList = storiesList.subList(position, storiesList.size)
//
//                        // Convert the sublist to JSON
//                        val gson = Gson()
//                        val jsonString = gson.toJson(subList)
//                        println("sadjfgajsg  jsonString $jsonString")
//
//                        // Pass the JSON string to the next activity
//                        val intent = Intent(context, ViewStoriesActivity::class.java)
//                        intent.putExtra("StoriesJson", jsonString)
//                        context.startActivity(intent)
//                    } else {
//                        println("sadjfgajsg position $position is null")
//                    }



                }
            }


        }
    }

    class StoriesDiffCallback : DiffUtil.ItemCallback<Stories>() {
        override fun areItemsTheSame(oldItem: Stories, newItem: Stories): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Stories, newItem: Stories): Boolean {
            return oldItem == newItem
        }
    }
}

