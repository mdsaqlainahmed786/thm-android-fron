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
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.ViewedStoriesManager
import com.thehotelmedia.android.databinding.StoryItemsLayoutBinding
import com.thehotelmedia.android.databinding.HeaderItemLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.Stories.Stories

class StoryAdapter(private val context: Context,private val userProfilePic: String) : PagingDataAdapter<Stories, RecyclerView.ViewHolder>(StoriesDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 10
    }

    private val preferenceManager = PreferenceManager.getInstance(context)

    /**
     * Calculate if all stories have been viewed by the current user.
     * Returns true only if ALL stories in storiesRef have been viewed by the current user.
     * Returns false if any story has not been viewed, or if storiesRef is empty.
     * 
     * Uses hybrid approach:
     * 1. If backend says seenByMe = true, trust it (most reliable after viewing)
     * 2. If backend says seenByMe = false/null, check locally via viewsRef
     * 3. This handles both immediate updates (backend) and delayed updates (local check)
     */
    private fun areAllStoriesViewedByCurrentUser(stories: Stories): Boolean {
        val currentUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "") ?: ""
        
        // If no user ID, can't determine if viewed
        if (currentUserId.isEmpty()) {
            android.util.Log.w("StoryAdapter", "No current user ID found")
            return false
        }
        
        val storiesRef = stories.storiesRef
        
        // If no stories, return false (no stories to view)
        if (storiesRef.isEmpty()) {
            return false
        }
        
        // Get all story IDs
        val storyIds = storiesRef.mapNotNull { it.Id }.filter { it.isNotEmpty() }
        
        if (storyIds.isEmpty()) {
            return false
        }
        
        // Check 1: Check SharedPreferences first (immediate update after viewing)
        // This ensures the ring disappears immediately after viewing, even before backend refreshes.
        // We scope this per logged-in user so multiple accounts do not share viewed state.
        val allViewedLocally = ViewedStoriesManager.areAllStoriesViewed(context, currentUserId, storyIds)
        if (allViewedLocally) {
            android.util.Log.d("StoryAdapter", "✅ All ${storyIds.size} stories viewed locally (from SharedPreferences) for user $currentUserId")
            return true
        }

        // Check 2: Verify by checking viewsRef from backend data
        // We no longer blindly trust stories.seenByMe because it may stay true
        // even after new stories are added; instead we rely on concrete viewsRef.
        android.util.Log.d("StoryAdapter", "Backend seenByMe=${stories.seenByMe}, checking viewsRef: ${storiesRef.size} stories for user $currentUserId")
        
        // Check if current user has viewed ALL stories based on backend viewsRef
        var allViewed = true
        val viewedStoryIdsFromBackend = mutableListOf<String>()
        
        for ((index, story) in storiesRef.withIndex()) {
            val storyId = story.Id ?: "unknown"
            
            // Check viewsRef from backend data
            val viewsRef = story.viewsRef ?: emptyList()
            val hasViewedInBackend = viewsRef.any { viewer -> 
                viewer.Id?.equals(currentUserId, ignoreCase = true) == true
            }
            
            if (hasViewedInBackend) {
                viewedStoryIdsFromBackend.add(storyId)
                android.util.Log.d("StoryAdapter", "Story $storyId (index $index) viewed (from backend viewsRef)")
            } else {
                // Story not viewed
                android.util.Log.d("StoryAdapter", "❌ Story $storyId (index $index) NOT viewed by user $currentUserId. ViewsRef size: ${viewsRef.size}")
                allViewed = false
                break
            }
        }
        
        // If all stories are viewed according to backend, mark them locally for this user
        if (allViewed && viewedStoryIdsFromBackend.isNotEmpty()) {
            ViewedStoriesManager.markAllStoriesAsViewed(context, currentUserId, viewedStoryIdsFromBackend)
            android.util.Log.d("StoryAdapter", "✅ All ${storiesRef.size} stories viewed by user $currentUserId (from backend viewsRef)")
        }
        
        return allViewed
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



                // Apply shiny blue ring for user's own stories if they have stories
                if (it.storiesRef.isEmpty()){
                    // No stories - show normal appearance
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.et_stroke)
                    holder.binding.ringContainer.setBackgroundResource(0)
                }else{
                    // User has stories - show shiny blue ring
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.blue)
                    holder.binding.ringContainer.setBackgroundResource(R.drawable.shiny_blue_ring_border)
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

                // Calculate seenByMe based on whether ALL stories have been viewed by current user
                // This ensures the ring reappears when new stories are added
                val seenByMe = areAllStoriesViewedByCurrentUser(it)

                println("asdnfkashdfjk    seenByMe: $seenByMe, storiesCount: ${it.storiesRef.size}")

                // Apply shiny blue ring for unseen stories, normal appearance for seen stories
                if (seenByMe){
                    // Story has been viewed - show normal appearance (gray stroke, no ring)
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.et_stroke)
                    holder.binding.ringContainer.setBackgroundResource(0)
                }else{
                    // Story is unseen - show shiny blue ring
                    holder.binding.imageView.strokeColor = ContextCompat.getColorStateList(context, R.color.blue)
                    holder.binding.ringContainer.setBackgroundResource(R.drawable.shiny_blue_ring_border)
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
            // Check if storiesRef has changed (new stories added or removed)
            // This ensures the adapter refreshes when new stories are added
            val storiesRefChanged = oldItem.storiesRef.size != newItem.storiesRef.size ||
                    oldItem.storiesRef.mapNotNull { it.Id }.toSet() != newItem.storiesRef.mapNotNull { it.Id }.toSet()
            
            // Check if viewsRef has changed for any story (user viewed a story)
            // Only check if sizes match, otherwise we know it changed
            val viewsRefChanged = if (oldItem.storiesRef.size == newItem.storiesRef.size) {
                oldItem.storiesRef.zip(newItem.storiesRef).any { (old, new) ->
                    val oldViewsRef = old.viewsRef ?: emptyList()
                    val newViewsRef = new.viewsRef ?: emptyList()
                    oldViewsRef.size != newViewsRef.size ||
                    oldViewsRef.mapNotNull { it.Id }.toSet() != newViewsRef.mapNotNull { it.Id }.toSet()
                }
            } else {
                true // Different sizes means viewsRef definitely changed
            }
            
            // Return false if storiesRef or viewsRef changed, so the adapter rebinds the item
            // This ensures the ring visibility updates when new stories are added or viewed
            return !storiesRefChanged && !viewsRefChanged && oldItem.id == newItem.id &&
                   oldItem.name == newItem.name && oldItem.accountType == newItem.accountType
        }
    }
}

