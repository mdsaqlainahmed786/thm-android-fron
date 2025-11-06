package com.thehotelmedia.android.adapters

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.activity.JobDetailActivity
import com.thehotelmedia.android.activity.PostPreviewActivity
import com.thehotelmedia.android.activity.ViewEventDetailsActivity
import com.thehotelmedia.android.activity.userTypes.individual.BookTableBanquetActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.BookingSummaryActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.PostJobActivity
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.databinding.NotificationItemsLayoutBinding
import com.thehotelmedia.android.extensions.calculateDaysAgoInSmall
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.notifications.NotificationData

class NotificationAdapter(
    private val context: Context,
    private val onDeclineClick: (String, Int) -> Unit,
    private val onAcceptClick: (String?) -> Unit,
    private val onFollowClick: (String?) -> Unit,
    private val onCollaborationAcceptClick: (String, String) -> Unit,
    private val onCollaborationDeclineClick: (String, String) -> Unit,
    private val ownerUserId: String
) : PagingDataAdapter<NotificationData, NotificationAdapter.ViewHolder>(NotificationDiffCallback())   {

    companion object {
        // Track collaboration responses locally (notificationId -> "accepted" or "rejected")
        // This is in-memory cache for immediate UI updates
        private val collaborationResponses = mutableMapOf<String, String>()
        
        // SharedPreferences name for persisting collaboration responses across app restarts
        private const val PREFS_NAME = "collaboration_responses"
        
        // Load all persisted responses into memory
        // This is called on adapter initialization and whenever we need to check responses
        internal fun loadCacheIfNeeded(context: Context) {
            // Only load if cache is empty (to avoid reloading unnecessarily)
            if (collaborationResponses.isNotEmpty()) {
                return
            }
            
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val allEntries = prefs.all
                var loadedCount = 0
                for ((key, value) in allEntries) {
                    if (key.startsWith("collab_response_") && value is String) {
                        val id = key.removePrefix("collab_response_")
                        collaborationResponses[id] = value
                        loadedCount++
                    }
                }
                android.util.Log.d("NotificationAdapter", "Loaded $loadedCount persisted collaboration responses from SharedPreferences: ${collaborationResponses.keys}")
            } catch (e: Exception) {
                android.util.Log.e("NotificationAdapter", "Error loading collaboration responses cache: ${e.message}", e)
            }
        }
        
        fun markCollaborationResponded(context: Context, notificationId: String, postId: String, action: String) {
            // Load cache if not already loaded
            loadCacheIfNeeded(context)
            
            // Store in memory for immediate UI update (using both notificationId and postId as keys)
            collaborationResponses[notificationId] = action
            if (postId.isNotEmpty() && postId != "null") {
                collaborationResponses["post_$postId"] = action
            }
            
            // Persist to SharedPreferences so it survives app restarts
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("collab_response_$notificationId", action)
            if (postId.isNotEmpty() && postId != "null") {
                editor.putString("collab_response_post_$postId", action)
            }
            editor.apply() // Use apply() for async, but ensure it's written
            
            android.util.Log.d("NotificationAdapter", "Marked notification $notificationId (postId: $postId) as $action and persisted")
        }
        
        fun getCollaborationResponse(context: Context, notificationId: String, postId: String = ""): String? {
            // Load cache if not already loaded
            loadCacheIfNeeded(context)
            
            // First check in-memory cache by notificationId (fast)
            val memoryResponse = collaborationResponses[notificationId]
            if (memoryResponse != null) {
                return memoryResponse
            }
            
            // Also check by postId if available (in case notificationId changes)
            if (postId.isNotEmpty() && postId != "null") {
                val postResponse = collaborationResponses["post_$postId"]
                if (postResponse != null) {
                    return postResponse
                }
            }
            
            // If not in memory, check SharedPreferences (persisted across app restarts)
            // This is a fallback in case memory cache was cleared
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // Check by notificationId first
            val key = "collab_response_$notificationId"
            var persistedResponse = prefs.getString(key, null)
            
            // If not found and postId is available, check by postId
            if (persistedResponse == null && postId.isNotEmpty() && postId != "null") {
                val postKey = "collab_response_post_$postId"
                persistedResponse = prefs.getString(postKey, null)
            }
            
            if (persistedResponse != null) {
                // Restore to memory cache for faster future access
                collaborationResponses[notificationId] = persistedResponse
                if (postId.isNotEmpty() && postId != "null") {
                    collaborationResponses["post_$postId"] = persistedResponse
                }
                android.util.Log.d("NotificationAdapter", "Restored notification $notificationId (postId: $postId) response: $persistedResponse from SharedPreferences")
                return persistedResponse
            }
            
            return null
        }
        
        fun clearCollaborationResponses(context: Context) {
            collaborationResponses.clear()
            // Optionally clear SharedPreferences if needed (for testing/debugging)
            // val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // prefs.edit().clear().apply()
        }
    }
    
    init {
        // Load persisted responses on adapter initialization
        Companion.loadCacheIfNeeded(context)
    }

    private val notifications = mutableListOf<NotificationData>()

    inner class ViewHolder(val binding: NotificationItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = NotificationItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }




    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)
        notification?.let { it ->

            val binding = holder.binding
            // Set title and time
            val id = it.Id.toString()
            val jobId = it.metadata?.jobID ?: ""
            val bookingID = it.metadata?.bookingID ?: ""
            val bookingType = it.metadata?.type ?: ""
            val postId = it.metadata?.postID ?: ""
            val metaDataPostType = it.metadata?.postType ?: ""
            val userID = it.userID.toString()
            val title = it.description.toString()
            val connectionId = it.metadata?.connectionID ?: ""
            val type = it.type.toString()
            val time = calculateDaysAgoInSmall(it.createdAt.toString())
            val words = title.split(" ")
            // Check if the length exceeds 100 characters
            val truncatedTitle = if (title.length > 60) {
                title.take(60) + "..."
            } else {
                title
            }
            val truncatedTime = if (title.length > 60) {
                "more $time"
            } else {
                time
            }

            val isRequested = it.isRequested ?: false
            val isConnected = it.isConnected ?: false

            // Check if this is a collaboration notification (case-insensitive)
            val isCollaborationNotification = type.contains("collaboration", ignoreCase = true) || 
                                               type == "collaboration-invite" || 
                                               type == "collaboration"
            
            // Check if collaboration is accepted or rejected (from backend or persisted local tracking)
            // Pass both notificationId and postId for more reliable lookup
            val localResponse = getCollaborationResponse(context, id, postId)
            
            // Debug logging to verify state
            if (isCollaborationNotification) {
                android.util.Log.d("NotificationAdapter", "Checking collaboration response for notificationId: $id, postId: $postId, type: $type, localResponse: $localResponse")
            }
            
            val isCollaborationAccepted = type.contains("collaboration-accepted", ignoreCase = true) || 
                                         type == "collaboration-accepted" ||
                                         localResponse == "accept"
            val isCollaborationRejected = type.contains("collaboration-rejected", ignoreCase = true) || 
                                         type == "collaboration-rejected" ||
                                         type.contains("collaboration-declined", ignoreCase = true) ||
                                         localResponse == "reject"

            if (type == "follow-request"){
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.VISIBLE
                binding.followingBtn.visibility = View.GONE
            }else if(type == "accept-follow-request"){
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.VISIBLE
            }else if(type == "following"){

                if (!isRequested && !isConnected){
                binding.followBtn.visibility = View.VISIBLE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.GONE
                }else if (isRequested && !isConnected){
                    binding.followBtn.visibility = View.GONE
                    binding.acceptDeclineLayout.visibility = View.GONE
                    binding.followingBtn.visibility = View.VISIBLE
                    binding.followingTv.text = "Requested"
                }else{
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.VISIBLE
                }

//                binding.followBtn.visibility = View.VISIBLE
//                binding.acceptDeclineLayout.visibility = View.GONE
//                binding.followingBtn.visibility = View.GONE
            }else if(isCollaborationAccepted){
                // Show "Accepted" status for accepted collaboration
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.VISIBLE
                binding.followingTv.text = context.getString(R.string.accepted)
            }else if(isCollaborationRejected){
                // Show "Rejected" status for rejected collaboration
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.VISIBLE
                binding.followingTv.text = context.getString(R.string.rejected)
            }else if(isCollaborationNotification){
                // Show accept/reject buttons for pending collaboration invitations (same UI as follow request)
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.VISIBLE
                binding.followingBtn.visibility = View.GONE
            }else{
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.GONE
            }

            // Handle button clicks using lambda functions
            // Only show buttons if collaboration is pending (not accepted/rejected)
            if (isCollaborationNotification && !isCollaborationAccepted && !isCollaborationRejected && postId.isNotEmpty() && postId != "null") {
                // Use collaboration-specific handlers
                binding.declineBtn.setOnClickListener {
                    // Mark as responded locally (persisted) before API call
                    // Use both notificationId and postId for reliable persistence
                    markCollaborationResponded(context, id, postId, "reject")
                    // Update UI immediately
                    notifyItemChanged(position)
                    onCollaborationDeclineClick(postId, id)
                }
                binding.acceptBtn.setOnClickListener {
                    // Mark as responded locally (persisted) before API call
                    // Use both notificationId and postId for reliable persistence
                    markCollaborationResponded(context, id, postId, "accept")
                    // Update UI immediately
                    notifyItemChanged(position)
                    onCollaborationAcceptClick(postId, id)
                }
            } else if (!isCollaborationNotification) {
                // Use regular follow request handlers (only for non-collaboration notifications)
                binding.declineBtn.setOnClickListener {
                    onDeclineClick(connectionId, position)
                }
                binding.acceptBtn.setOnClickListener {
                    onAcceptClick(connectionId)
                }
            } else {
                // For accepted/rejected collaborations, remove button listeners
                binding.declineBtn.setOnClickListener(null)
                binding.acceptBtn.setOnClickListener(null)
            }
            binding.followBtn.setOnClickListener {
                onFollowClick(connectionId)
            }

            val accountType = it.usersRef?.accountType?.capitalizeFirstLetter() ?: ""
            var profilePic = ""
            if (accountType == business_type_individual){
                profilePic = it.usersRef?.profilePic?.medium.toString()
                binding.profileCardView.strokeColor = ContextCompat.getColor(context, R.color.transparent)
            }else{
                profilePic = it.usersRef?.businessProfileRef?.profilePic?.medium.toString()
                binding.profileCardView.strokeColor = ContextCompat.getColor(context, R.color.post_stroke)
            }

            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profilePic)

            // Create a SpannableString with both title and time
            val fullText = "$truncatedTitle $truncatedTime"
            val spannable = SpannableString(fullText)

            // Get colors
            val whiteColor = ContextCompat.getColor(context, R.color.text_color)
            val greyColor = ContextCompat.getColor(context, R.color.text_color_50)

            // Apply white color to the title
            spannable.setSpan(
                ForegroundColorSpan(whiteColor),
                0,
                truncatedTitle.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply grey color to the time
            spannable.setSpan(
                ForegroundColorSpan(greyColor),
                truncatedTitle.length + 1,  // Start after the title and a space
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set the spannable text to the TextView
            binding.descriptionTv.text = spannable


            binding.root.setOnClickListener {
                // Check for collaboration type first (case-insensitive)
                if (type.contains("collaboration", ignoreCase = true)) {
                    if (postId.isNotEmpty() && postId != "null") {
                        moveToPostPreviewScreen(postId)
                    }
                } else {
                    when (type) {
                        "comment" -> {
                            if (metaDataPostType == "event"){
                                moveToEventDetailsActivity(postId)
                            }else{
                                moveToPostPreviewScreen(postId)
                            }
//                        moveToPostPreviewScreen(postId)
                        }
                        "like-post" -> {
                            moveToPostPreviewScreen(postId)
                        }
                        "reply" -> {
                            moveToPostPreviewScreen(postId)
                        }
                        "like-comment" -> {
                            moveToBusinessProfileDetailsActivity(userID)
                        }
                        "follow-request" -> {
                            moveToBusinessProfileDetailsActivity(userID)
                        }
                        "accept-follow-request" -> {
                            moveToBusinessProfileDetailsActivity(userID)
                        }
                        "following" -> {
                            moveToBusinessProfileDetailsActivity(userID)
                        }
                        "tagged" -> {
                            moveToPostPreviewScreen(postId)
                        }
                        "event-join" -> {
                            moveToEventDetailsActivity(postId)
                        }
                        "like-a-story" -> {
                            moveToBusinessProfileDetailsActivity(userID)
                        }
                        "collaboration-invite", "collaboration" -> {
                            if (postId.isNotEmpty() && postId != "null") {
                                moveToPostPreviewScreen(postId)
                            }
                        }
                        "job" -> {
                            val intent = Intent(context, JobDetailActivity::class.java)
                            intent.putExtra("JOB_ID", jobId)
                            context.startActivity(intent)
                        }
                        "booking" -> {
                            when (bookingType) {
                                "book-banquet" -> {
                                    val intent = Intent(context, BookTableBanquetActivity::class.java)
                                    intent.putExtra("BOOKING_ID", bookingID)
                                    intent.putExtra("BOOKING_TYPE", bookingType)
                                    intent.putExtra("FROM", "NOTIFICATION")
                                    context.startActivity(intent)
                                }
                                "book-table" -> {
                                    val intent = Intent(context, BookTableBanquetActivity::class.java)
                                    intent.putExtra("BOOKING_ID", bookingID)
                                    intent.putExtra("BOOKING_TYPE", bookingType)
                                    intent.putExtra("FROM", "NOTIFICATION")
                                    context.startActivity(intent)
                                }
                                else -> {
                                    val intent = Intent(context, BookingSummaryActivity::class.java)
                                    intent.putExtra("BOOKING_ID", bookingID)
                                    intent.putExtra("FROM", "NOTIFICATION")
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                }
//            val intent = Intent(this, JobDetailActivity::class.java)
//                startActivity(intent)


//                if(postId.isNotEmpty()){
//                    moveToPostPreviewScreen(postId)
//                }else if (userID.isNotEmpty()){
//                    moveToBusinessProfileDetailsActivity(userID)
//                }

            }

        }

    }

    private fun moveToPostPreviewScreen(postId: String) {
        val intent = Intent(context, PostPreviewActivity::class.java)
        intent.putExtra("FROM", "Notification")
        intent.putExtra("POST_ID", postId)
        context.startActivity(intent)
    }

//    private fun moveToProfileDetailsActivity(userId: String) {
//        println("af;ljasklfjakl   $userId")
//        val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
//        intent.putExtra("USER_ID", userId)
//        context.startActivity(intent)
//    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }
    private fun moveToEventDetailsActivity(postId: String) {
        val intent = Intent(context, ViewEventDetailsActivity::class.java).apply {
            putExtra("POST_ID", postId)
        }
        context.startActivity(intent)
    }


    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationData>() {
        override fun areItemsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem == newItem
        }
    }





}

