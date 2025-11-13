package com.thehotelmedia.android.fragments.userTypes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.activity.ViewEventDetailsActivity
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaPagerAdapter
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.EditPostBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.SharePostBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.TagPeopleBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.EventItemsLayoutBinding
import com.thehotelmedia.android.databinding.PostItemsLayoutBinding
import com.thehotelmedia.android.databinding.ReviewItemsLayoutBinding
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.getEmojiForRating
import com.thehotelmedia.android.extensions.isFutureDateOrTime
import com.thehotelmedia.android.extensions.moveToPostPreviewScreen
import com.thehotelmedia.android.extensions.moveToUserPostsViewer
import com.thehotelmedia.android.extensions.openGoogleMaps
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.setRatingWithStars
import com.thehotelmedia.android.extensions.shareEventsWithDeepLink
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.extensions.updateTextWithAnimation
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef
import com.thehotelmedia.android.modals.collaboration.CollaborationUser
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class SavedFeedAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val parentFragmentManager: FragmentManager,
    private val ownerUserId: String,
    private val from: String,
    private val lifecycleScope: LifecycleCoroutineScope,
    private var enableStoryShare: Boolean,
    private var viewerFollowsOwner: Boolean,
) : PagingDataAdapter<Data, RecyclerView.ViewHolder>(SAVED_FEED_DIFF_CALLBACK()) {
    var onStoryShareRequested: ((String) -> Unit)? = null
    private val sharedStoryPostIds = mutableSetOf<String>()
    private var dotsIndicator: SpringDotsIndicator? = null
    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_REVIEW = 1
        private const val VIEW_TYPE_EVENT = 2
    }


    private var activePosition = 0 // No active position initially

    private lateinit var mediaPagerAdapter : MediaPagerAdapter
    
    // Cache for collaborators to avoid redundant API calls
    private val collaboratorsCache = mutableMapOf<String, List<CollaborationUser>>()
    // Store original names to restore them if no collaborators
    private val originalNamesCache = mutableMapOf<String, String>()
    // In-memory cache for collaborator display text (postId -> "User1 and User2")
    // This is loaded from SharedPreferences on initialization
    private val collaboratorTextCache = mutableMapOf<String, CharSequence>()
    
    // SharedPreferences name for persisting collaborator text cache
    private val prefsName = "collaborator_text_cache"
    
    init {
        // Load persisted collaborator text from SharedPreferences on adapter creation
        loadCollaboratorTextCache()
    }
    
    private fun loadCollaboratorTextCache() {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                if (key.startsWith("collab_text_") && value is String) {
                    val postId = key.removePrefix("collab_text_")
                    // Store as plain string - we'll rebuild SpannableString when needed
                    collaboratorTextCache[postId] = value
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SavedFeedAdapter", "Error loading collaborator text cache: ${e.message}", e)
        }
    }
    
    private fun saveCollaboratorTextToPrefs(postId: String, text: CharSequence) {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val key = "collab_text_$postId"
            prefs.edit().putString(key, text.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("SavedFeedAdapter", "Error saving collaborator text to prefs: ${e.message}", e)
        }
    }




    inner class PostViewHolder(val binding: PostItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Data, isActive: Boolean, position: Int) {
            // Cancel any pending async updates for this view to prevent stale data
            val postId = post.Id ?: ""
            if (pendingCollaboratorRequests.containsKey(postId)) {
                // Remove pending request if view is being rebound (prevents stale updates)
                pendingCollaboratorRequests.remove(postId)
            }
            setPostData(post,binding,isActive,position)
        }
    }




    inner class ReviewViewHolder(val binding: ReviewItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Data, isActive: Boolean, position: Int) {
            setReviewData(review,binding,isActive,position)
        }
    }



    inner class EventViewHolder(val binding: EventItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: Data, isActive: Boolean, position: Int) {
            setEventData(event,binding,isActive,position)

        }
    }



    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item?.postType) {
            "post" -> VIEW_TYPE_POST
            "review" -> VIEW_TYPE_REVIEW
            "event" -> VIEW_TYPE_EVENT
            else -> throw IllegalArgumentException("Invalid post type: ${item?.postType}")
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_POST -> {
                val binding = PostItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding)
            }
            VIEW_TYPE_REVIEW -> {
                val binding = ReviewItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReviewViewHolder(binding)
            }
            VIEW_TYPE_EVENT -> {
                val binding = EventItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                EventViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val isActive = position == activePosition
        when (val item = getItem(position)) {
            is Data -> {
                when (holder) {
                    is PostViewHolder -> holder.bind(item,isActive,position)
                    is ReviewViewHolder -> holder.bind(item,isActive,position)
                    is EventViewHolder -> holder.bind(item,isActive,position)
                }
            }
            else -> throw IllegalArgumentException("Invalid item")
        }
    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }


    private fun setPostData(
        post: Data,
        binding: PostItemsLayoutBinding,
        isActive: Boolean,
        position: Int
    ) {
        try {
        val itemData = getItem(position)
        // Media setup
            val mediaList = post.mediaRef ?: arrayListOf()

        val postId = post.Id ?: ""

        dotsIndicator = binding.indicatorLayout.apply {
            dotsClickable = false
            setDotIndicatorColor(ContextCompat.getColor(context, R.color.blue))
            setStrokeDotsIndicatorColor(ContextCompat.getColor(context, R.color.grey))
        }

        // Save/Like state handling
        var isPostSaved = post.savedByMe ?: false
        var isPostLiked = post.likedByMe ?: false
        // Like, Comment, and Share counts
        var likeCount = post.likes ?: 0
        var commentCount = post.comments ?: 0
        val shareCount = post.shared ?: 0
        binding.likeTv.text = formatCount(likeCount)
        binding.commentTv.text = formatCount(commentCount)
        binding.shareTv.text = formatCount(shareCount)

        val viewsCount = post.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        if (mediaList.isNotEmpty()){
            mediaPagerAdapter = MediaPagerAdapter(
                context,
                mediaList,
                isActive,
                postId,
                isPostLiked,
                likeCount,
                commentCount
            ){ updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                updateLikeBtn(updatedIsLikedByMe, binding.likeIv)
                binding.likeTv.text = updatedLikeCount.toString()
                binding.commentTv.text = updatedCommentCount.toString()
                // You can also update UI elements in the activity here
            }
            binding.viewPager.adapter = mediaPagerAdapter
            binding.mediaLayout.visibility = View.VISIBLE
            
            // Add click listener to open post viewer
            binding.mediaLayout.setOnClickListener {
                val userId = post.userID ?: post.postedBy?.Id ?: ""
                if (userId.isNotEmpty()) {
                    context.moveToUserPostsViewer(userId, postId)
                }
            }
        }else{
            binding.mediaLayout.visibility = View.GONE
        }

//        mediaPagerAdapter = MediaPagerAdapter(context, mediaList, isActive)
//        binding.viewPager.adapter = mediaPagerAdapter

//        dotsIndicator?.attachTo(binding.viewPager)
//        binding.indicatorLayout.visibility = if (mediaList.size == 1) View.GONE else View.VISIBLE
        if (mediaList.size > 1) {
            binding.indicatorLayout.visibility = View.VISIBLE
            dotsIndicator?.attachTo(binding.viewPager)
        } else {
            binding.indicatorLayout.visibility = View.GONE
        }

        val createdAt = post.createdAt ?: ""
        val formatedCreatedAt = calculateDaysAgo(createdAt,context)

        // Handle user details
        val postedBy = post.postedBy
        val accountType = postedBy?.accountType
        val userId = postedBy?.Id ?: ""
        var name = ""
        var profilePic = ""

        if (accountType == "individual") {
            binding.businessTypeLayout.visibility = View.GONE
            binding.profileCv.strokeColor = ContextCompat.getColor(context, R.color.transparent)
            name = postedBy.name ?: ""
            profilePic = postedBy.profilePic?.large ?: ""


            binding.locationTv.text = formatedCreatedAt
        } else {
            binding.businessTypeLayout.visibility = View.VISIBLE
            val businessProfile = postedBy?.businessProfileRef
            val businessType = businessProfile?.businessTypeRef?.name ?: ""
            val businessSubType = businessProfile?.businessSubtypeRef?.name ?: ""
            val averageRating = businessProfile?.businessRating ?: 0.0

            binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
            binding.typeTv.text = "$businessType - $businessSubType"
            binding.profileCv.strokeColor = ContextCompat.getColor(context, R.color.post_stroke)

            name = businessProfile?.name ?: ""
            profilePic = businessProfile?.profilePic?.large ?: ""

            val address = businessProfile?.address
            val state = address?.state
            val country = address?.country
            val locationText = "$state, $country"
            // Update the TextView with animation
            binding.locationTv.updateTextWithAnimation(isActive, locationText, formatedCreatedAt)

        }

        // Set user profile picture
        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.profileIv)

        // Display collaborators directly from post data (no separate API call)
        // Hide avatar container completely - we only show text
        binding.collaboratorsProfileContainer.visibility = View.GONE
        
        // Set tag FIRST to identify this view
        binding.nameTv.tag = userId
        
        // Calculate final name text synchronously (before any UI updates)
        // Check cache FIRST - if we have cached text, use it immediately (no flicker)
        val cachedText = collaboratorTextCache[postId]
        val finalNameText: CharSequence = if (cachedText != null) {
            // Use cached text - rebuild SpannableString if it contains "and" (collaborator format)
            val cachedString = cachedText.toString()
            if (cachedString.contains(" and ") && cachedString.length > cachedString.indexOf(" and ") + 5) {
                // Rebuild SpannableString with blue styling and clickable spans for collaborator name
                val spannableString = android.text.SpannableString(cachedString)
                val mainUserNameEndIndex = cachedString.indexOf(" and")
                val collaboratorStartIndex = cachedString.indexOf("and") + 4 // Start after "and "
                val endIndex = cachedString.length
                
                // Make main user name clickable
                if (mainUserNameEndIndex > 0) {
                    spannableString.setSpan(
                        object : android.text.style.ClickableSpan() {
                            override fun onClick(widget: android.view.View) {
                                moveToBusinessProfileDetailsActivity(userId)
                            }
                            override fun updateDrawState(ds: android.text.TextPaint) {
                                super.updateDrawState(ds)
                                ds.isUnderlineText = false
                                // Explicitly set text color to ensure visibility
                                ds.color = ContextCompat.getColor(context, R.color.text_color)
                            }
                        },
                        0,
                        mainUserNameEndIndex,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    // Also set foreground color span to ensure the text is visible
                    spannableString.setSpan(
                        android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_color)),
                        0,
                        mainUserNameEndIndex,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                
                // Make collaborator name blue and clickable
                if (collaboratorStartIndex < endIndex && collaboratorStartIndex > 0 && collaboratorStartIndex < cachedString.length) {
                    // Get collaborator ID from post data if available
                    val collaborators = post.collaborators
                    val collaboratorId = if (collaborators != null && collaborators.isNotEmpty()) {
                        collaborators.firstOrNull()?._id ?: ""
                    } else {
                        ""
                    }
                    
                    // Set blue color
                    spannableString.setSpan(
                        android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)),
                        collaboratorStartIndex,
                        endIndex,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    
                    // Make clickable if we have collaborator ID
                    if (collaboratorId.isNotEmpty()) {
                        spannableString.setSpan(
                            object : android.text.style.ClickableSpan() {
                                override fun onClick(widget: android.view.View) {
                                    moveToBusinessProfileDetailsActivity(collaboratorId)
                                }
                                override fun updateDrawState(ds: android.text.TextPaint) {
                                    super.updateDrawState(ds)
                                    ds.color = ContextCompat.getColor(context, R.color.blue)
                                    ds.isUnderlineText = false
                                }
                            },
                            collaboratorStartIndex,
                            endIndex,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                spannableString
            } else {
                // Plain name - make it clickable
                val spannableString = android.text.SpannableString(cachedString)
                spannableString.setSpan(
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: android.view.View) {
                            moveToBusinessProfileDetailsActivity(userId)
                        }
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            // Explicitly set text color to ensure visibility
                            ds.color = ContextCompat.getColor(context, R.color.text_color)
                        }
                    },
                    0,
                    cachedString.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Also set foreground color span to ensure the text is visible
                spannableString.setSpan(
                    android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_color)),
                    0,
                    cachedString.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableString
            }
        } else try {
            val collaborators = post.collaborators
            if (collaborators != null && collaborators.isNotEmpty()) {
                try {
                    // Filter out:
                    // 1. Post owner (by userId)
                    // 2. Collaborators without names (only have IDs from string array response)
                    // 3. Empty/null names
                    val validCollaborators = collaborators.filterNotNull()
                        .filter { 
                            try {
                                !it.name.isNullOrEmpty() && it._id != userId
                            } catch (e: Exception) {
                                false
                            }
                        }
                    
                    if (validCollaborators.isNotEmpty()) {
                        // Build collaborator text with blue styling and clickable spans
                        val collaboratorText = buildCollaboratorText(validCollaborators, name, userId, binding)
                        // Cache the result for future rebinds (both memory and persistent storage)
                        collaboratorTextCache[postId] = collaboratorText
                        saveCollaboratorTextToPrefs(postId, collaboratorText)
                        collaboratorText
                    } else {
                        // If we have collaborators but no valid ones (only IDs), mark for API call
                        val collaboratorsWithOnlyIds = collaborators.filter { it._id != userId && it.name.isNullOrEmpty() && !it._id.isNullOrEmpty() }
                        if (collaboratorsWithOnlyIds.isNotEmpty()) {
                            // Load collaborators from API (async, will update later)
                            loadCollaboratorsForPost(postId, binding, name, userId)
                        }
                        // Cache normal name to prevent re-processing (but don't persist normal names)
                        collaboratorTextCache[postId] = name
                        name
                    }
                } catch (e: Exception) {
                    // If there's any error processing collaborators, just show normal name
                    android.util.Log.e("SavedFeedAdapter", "Error processing collaborators: ${e.message}", e)
                    // Don't cache errors
                    name
                }
            } else {
                // No collaborators - don't cache normal names
                name
            }
        } catch (e: Exception) {
            // If there's any error accessing collaborators, just show normal name
            android.util.Log.e("SavedFeedAdapter", "Error accessing collaborators field: ${e.message}", e)
            name
        }
        
        // Always set the text (don't skip if it's the same - ensures collaborator text is displayed)
        // The comparison was preventing collaborator text from being set in some cases
        binding.nameTv.text = finalNameText
        
        // Make TextView clickable if it contains clickable spans (collaborator text)
        if (finalNameText is android.text.Spannable) {
            binding.nameTv.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        } else {
            binding.nameTv.movementMethod = null
        }
        
        // Debug logging to trace collaborator display
        if (finalNameText.toString().contains(" and ")) {
            android.util.Log.d("SavedFeedAdapter", "Setting collaborator text for postId: $postId, text: $finalNameText")
        }



        // Post content
        val content = post.content.orEmpty().trim()
        val feelings = post.feelings.orEmpty().trim()
        val location = post.location?.placeName.orEmpty().trim()
        val taggedPeople = post.taggedRef
        val lat  = post.location?.lat ?: 0.00
        val lng  = post.location?.lng ?: 0.00

        if (content.isNotEmpty() || feelings.isNotEmpty() || location.isNotEmpty() || taggedPeople.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                val taggedRefString = generateTaggedRefString(taggedPeople)
                withContext(Dispatchers.Main) {
                    setDescriptionTextAndClick(binding, content, feelings, taggedRefString, location,taggedPeople,lat,lng)
                }
            }
            binding.allDescriptionTv.visibility =View.VISIBLE
        }else{
            binding.allDescriptionTv.visibility =View.GONE
        }


        updateSaveBtn(isPostSaved, binding.saveIv)
        updateLikeBtn(isPostLiked, binding.likeIv)

        // Save button click
        binding.saveIv.setOnClickListener {
            savePost(postId)
            isPostSaved = !isPostSaved
            updateSaveBtn(isPostSaved, binding.saveIv)
        }

        // Like button click
        binding.likeBtn.setOnClickListener {
            likePost(postId)
            isPostLiked = !isPostLiked
            binding.likeIv.setImageResource(if (isPostLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon)
            likeCount = if (isPostLiked) likeCount + 1 else likeCount - 1
            binding.likeTv.text = formatCount(likeCount)
            post.likes = likeCount
            post.likedByMe = isPostLiked
        }

        // Comment button click
        binding.commentBtn.setOnClickListener {
            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putInt("COMMENTS_COUNT", commentCount)
                }
            }
            bottomSheetFragment.onCommentSent = { comment ->
                if (comment.isNotEmpty()) {
                    commentCount++
                    binding.commentTv.text = formatCount(commentCount)
                    notifyDataSetChanged()
                }
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

        // User profile click
        binding.userLayout.setOnClickListener {
            moveToBusinessProfileDetailsActivity(userId)
        }

        // Share button click
        binding.shareBtn.setOnClickListener {
            if (postId.isNotBlank() && ownerUserId.isNotBlank()) {
                val selectedMedia = if (mediaList.isNotEmpty()) {
                    val currentIndex = binding.viewPager.currentItem.coerceIn(0, mediaList.size - 1)
                    mediaList.getOrNull(currentIndex)
                } else null

                val mediaType = selectedMedia?.mediaType?.lowercase(Locale.getDefault())
                SharePostBottomSheetFragment.newInstance(
                    postId = postId,
                    ownerUserId = ownerUserId,
                    mediaType = mediaType,
                    mediaUrl = selectedMedia?.sourceUrl,
                    thumbnailUrl = selectedMedia?.thumbnailUrl,
                    mediaId = selectedMedia?.Id
                ).show(parentFragmentManager, SharePostBottomSheetFragment::class.java.simpleName)
            } else {
                context.sharePostWithDeepLink(postId, ownerUserId)
            }
        }

        // Hide edit and delete buttons - they are now in the menu
        binding.editBtn.visibility = View.GONE
        binding.deleteBtn.visibility = View.GONE

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view, postId, itemData, post, canShareToStory = enableStoryShare)
        }
        } catch (e: Exception) {
            // Catch any exceptions to prevent crashes
            android.util.Log.e("SavedFeedAdapter", "Error in setPostData: ${e.message}", e)
            // Set safe defaults
            binding.nameTv.text = "User"
            binding.collaboratorsProfileContainer.visibility = View.GONE
        }
    }

    private fun setReviewData(
        review: Data,
        binding: ReviewItemsLayoutBinding,
        isActive: Boolean,
        position: Int
    ) {
        val itemData = getItem(position)
        // Rating
        val rating = review.rating?.toInt() ?: 0
        val averageRating = review.reviewedBusinessProfileRef?.rating ?: 0.0
        binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
        binding.ratingTV.setRatingWithStars(rating)
        binding.ratingTypeTV.text = getEmojiForRating(rating)

        val googleReviewedBusiness = review.googleReviewedBusiness ?: ""
        val publicUserID = review.publicUserID ?: ""

        // Blur views
        context.blurTheView(binding.topBlurView)
        context.blurTheView(binding.bottomBlurView)
        var commentCount = review.comments ?: 0
        // Review Details
        val postedBy = review.postedBy ?: return
        val createdAt = review.createdAt ?: ""
        val userName = postedBy.name ?: ""
        val userProfilePic = postedBy.profilePic?.large ?: ""
        val businessName = review.reviewedBusinessProfileRef?.name ?: ""
        val businessProfilePic = review.reviewedBusinessProfileRef?.profilePic?.large ?: ""
        val coverImage = review.reviewedBusinessProfileRef?.coverImage ?: ""
        val businessType = review.reviewedBusinessProfileRef?.businessTypeRef?.name
        val businessSubType = review.reviewedBusinessProfileRef?.businessSubtypeRef?.name
        val city = review.reviewedBusinessProfileRef?.address?.city
        val state = review.reviewedBusinessProfileRef?.address?.state

        // Set user and business info
        binding.userNameTv.text = userName

        Glide.with(context).load(userProfilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.userProfileIv)

//        binding.userProfileIv.loadImageInBackground(context, userProfilePic, R.drawable.ic_profile_placeholder)
        binding.hotelNameTv.text = businessName

        Glide.with(context).load(businessProfilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        Glide.with(context).load(coverImage).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.imageView)

//        binding.hotelProfileIv.loadImageInBackground(context, businessProfilePic, R.drawable.ic_profile_placeholder)
//        binding.imageView.loadImageInBackground(context, coverImage.toString(), R.drawable.ic_post_placeholder)
        binding.hotelTypeTv.apply {
            visibility = if (businessType.isNullOrBlank()) View.GONE else View.VISIBLE
            text = when {
                businessType.isNullOrBlank() -> ""
                businessSubType.isNullOrBlank() -> businessType
                else -> "$businessType - $businessSubType"
            }
        }




        binding.location.text = "$city, $state"


        var likesCount = review.likes ?: 0
        var sharedCount = review.shared ?: 0

        // Like, Comment, and Share Counts
        binding.likeTv.text = formatCount(likesCount)
        binding.commentTv.text = formatCount(commentCount)
        binding.shareTv.text = formatCount(sharedCount)

        val viewsCount = review.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        // Content and Date
        binding.contentTv.text = review.content
        binding.daysTv.text = calculateDaysAgo(createdAt,context)

        // Post interaction states
        var isPostSaved = review.savedByMe ?: false
        var isPostLiked = review.likedByMe ?: false
        updateSaveBtn(isPostSaved, binding.saveIv)
        updateLikeBtn(isPostLiked, binding.likeIv)

        // Save, Like, and Comment buttons actions
        val postId = review.Id.toString()
        binding.saveIv.setOnClickListener {
            savePost(postId)
            isPostSaved = !isPostSaved
            updateSaveBtn(isPostSaved, binding.saveIv)
        }
        binding.likeBtn.setOnClickListener {
            likePost(postId)
            isPostLiked = !isPostLiked
            binding.likeIv.setImageResource(if (isPostLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon)
            val likes = review.likes?.let { it + if (isPostLiked) 1 else -1 } ?: 0
            binding.likeTv.text = formatCount(likes)
            review.likes = review.likes?.plus(if (isPostLiked) 1 else -1)
        }
        binding.commentBtn.setOnClickListener {
            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putInt("COMMENTS_COUNT", commentCount)
                }
            }
            bottomSheetFragment.onCommentSent = { comment ->
                if (comment.isNotEmpty()) {
                    commentCount++
                    binding.commentTv.text = formatCount(commentCount)
                }
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }


        binding.imageView.setOnClickListener {
            context.moveToPostPreviewScreen(postId)
        }

        // User layout click action
        binding.userLayout.setOnClickListener {
            if (publicUserID.isNotEmpty()){
                Toast.makeText(context,context.getString(R.string.profile_not_associated_thm), Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(review.postedBy?.Id ?: "")
            }
        }


        binding.businessLayout.setOnClickListener {
            if (googleReviewedBusiness.isNotEmpty()){
                Toast.makeText(context,context.getString(R.string.profile_not_associated_thm),
                    Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(review.reviewedBusinessProfileRef?.userId ?: "")
            }
        }
        // Share button click
        binding.shareBtn.setOnClickListener {
            context.sharePostWithDeepLink(postId, ownerUserId)
        }

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view, postId, itemData, review)
        }
    }





    private fun setEventData(
        event: Data,
        binding: EventItemsLayoutBinding,
        isActive: Boolean,
        position: Int
    ) {
        val itemData = getItem(position)
        // Extract necessary data
        val averageRating = event.postedBy?.businessProfileRef?.businessRating ?: 0.0
        val shareCount = event.shared ?: 0
        val userId = event.postedBy?.Id ?: ""
        val postId = event.Id ?: ""
        val name = event.postedBy?.businessProfileRef?.name ?: ""
        val profilePic = event.postedBy?.businessProfileRef?.profilePic?.large ?: ""
        val mediaRef = event.mediaRef
        val coverImage = if (mediaRef.isEmpty()){
            ""
        }else{
            event.mediaRef[0].sourceUrl ?: ""
        }
        val businessType = event.postedBy?.businessProfileRef?.businessTypeRef?.name
        val businessSubType = event.postedBy?.businessProfileRef?.businessSubtypeRef?.name
        val eventName = event.name ?: ""
        val venue = event.venue ?: ""
        val dateString = event.startDate ?: ""
        val timeString = event.startTime ?: ""
        var interestedPeople = event.interestedPeople ?: 0


        val startDate = event.startDate ?: ""
        val startTime = event.startTime ?: ""
        if (isFutureDateOrTime(startDate,startTime)) {
            binding.joiningBtn.visibility = View.VISIBLE
        } else {
            binding.joiningBtn.visibility = View.GONE
        }


        // Set UI elements
        if (interestedPeople != 0){
            binding.peopleInterestedTv.text = "$interestedPeople ${context.getString(R.string.interested_people)}"
            binding.peopleInterestedTv.visibility = View.VISIBLE
        }else{
            binding.peopleInterestedTv.visibility = View.GONE
        }

        binding.shareTv.text = formatCount(shareCount)

        val viewsCount = event.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
        binding.nameTv.text = name

        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        Glide.with(context).load(coverImage).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.coverImage)


//        binding.profileIv.loadImageInBackground(context, profilePic, R.drawable.ic_profile_placeholder)
//        binding.coverImage.loadImageInBackground(context, coverImage, R.drawable.ic_post_placeholder)
        binding.typeTv.text = "$businessType - $businessSubType"
        binding.eventNameTv.text = eventName.trim()
        binding.eventVenueTv.text = venue.trim()
        binding.dateTimeTv.text = formatEventDateTime(dateString, timeString)


        val address = event.postedBy?.businessProfileRef?.address
        val state = address?.state
        val country = address?.country
        binding.locationTv.text = "$state, $country"


        // Hide venue if empty
        if (venue.isEmpty()) binding.eventVenueTv.visibility = View.GONE

        // Joining status
        var isJoined = event.imJoining ?: false
        updateJoiningBtn(isJoined, binding.joiningIv, binding.joiningTv)

        // Save post state
        var isPostSaved = event.savedByMe ?: false
        updateSaveBtn(isPostSaved, binding.saveIv)

        // Handle button clicks
        binding.saveIv.setOnClickListener {
            savePost(event.Id.toString())
            isPostSaved = !isPostSaved
            updateSaveBtn(isPostSaved, binding.saveIv)
        }

        binding.coverImage.setOnClickListener {
            val intent = Intent(context, ViewEventDetailsActivity::class.java).apply {
                putExtra("POST_ID", postId)
            }
            context.startActivity(intent)
        }

        binding.joiningBtn.setOnClickListener {
            joinEvent(event.Id.toString())
            isJoined = !isJoined
            updateJoiningBtn(isJoined, binding.joiningIv, binding.joiningTv)
            val updatedInterestedPeople = if (isJoined){
                interestedPeople + 1
            }else{
                interestedPeople - 1
            }
            interestedPeople = updatedInterestedPeople
            binding.peopleInterestedTv.text = "$updatedInterestedPeople ${context.getString(R.string.interested_people)}"
        }

        binding.userLayout.setOnClickListener {
            moveToBusinessProfileDetailsActivity(userId)
        }

        // Share button click
        binding.shareBtn.setOnClickListener {
            context.shareEventsWithDeepLink(postId,ownerUserId)
        }

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view, postId, itemData, event)
        }


        var commentCount = event.comments ?: 0
        binding.commentTv.text = formatCount(commentCount)


        // Comment button click
        binding.commentBtn.setOnClickListener {
            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putInt("COMMENTS_COUNT", commentCount)
                }
            }
            bottomSheetFragment.onCommentSent = { comment ->
                if (comment.isNotEmpty()) {
                    commentCount++
                    binding.commentTv.text = formatCount(commentCount)
                }
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

    }

    private fun formatEventDateTime(dateString: String, timeString: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(timeString)
        val formattedDate = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(date)
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(time)
        return "$formattedDate at $formattedTime"
    }

//    private fun updateJoiningStatus(isJoined: Boolean, joiningIv: ImageView, joiningTv: TextView) {
//        if (isJoined) {
//            joiningIv.setImageResource(R.drawable.ic_filled_star_blue)
//            joiningTv.text = "Joined"
//        } else {
//            joiningIv.setImageResource(R.drawable.ic_outline_star_white)
//            joiningTv.text = "Joining ?"
//        }
//    }




    private fun updateJoiningBtn(postSaved: Boolean, joiningIv: ImageView, joiningTv: TextView) {
        if (postSaved) {
            joiningIv.setImageResource(R.drawable.ic_filled_star_blue)
            joiningTv.text = "Joined"
        } else {
            joiningIv.setImageResource(R.drawable.ic_outline_star_white)
            joiningTv.text = "Joining ?"
        }
    }

    private fun setDescriptionTextAndClick(binding: PostItemsLayoutBinding,description: String,feeling: String,people: String,location: String,taggedPeople: ArrayList<TaggedRef>,lat: Double, lng: Double) {
        val textColor = ContextCompat.getColor(context, R.color.blue_50)

        val bgColor = ContextCompat.getColor(context, R.color.transparent)

        val parts = mutableListOf<String>()
        if (description.isNotEmpty()) parts.add(description)
        if (feeling.isNotEmpty()) parts.add(feeling)
        if (people.isNotEmpty()) parts.add("with $people")
        if (location.isNotEmpty()) parts.add("at $location")

// Join the non-empty parts with a separator
        val finalText = parts.joinToString(" - ")
//        val finalText = "$description - $feeling - with $peoples - at $location"
        val spannableString = SpannableString(finalText)

        // Get the start and end indices of each portion of text
        val feelingStart = finalText.indexOf(feeling)
        val feelingEnd = feelingStart + feeling.length
        val peoplesStart = finalText.indexOf(people)
        val peoplesEnd = peoplesStart + people.length
        val locationStart = finalText.indexOf(location)
        val locationEnd = locationStart + location.length


        // Use the default Kotlin URL regex
        val matcher = Constants.URL_PATTERN_MATCHER.matcher(finalText)

        // Highlight and make URLs clickable
        while (matcher.find()) {
            val url = matcher.group(0)
            val urlStart = matcher.start()
            val urlEnd = matcher.end()

            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Open the URL in Chrome
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = textColor // Set the link color to blue
                    ds.isUnderlineText = true // Make the text underlined
                    ds.bgColor = bgColor // Ensure background color is transparent
                }
            }, urlStart, urlEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }


        // Set color and clickable span for feeling
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
//                Toast.makeText(context, "Feeling", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, feelingStart, feelingEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set color and clickable span for peoples
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
//                Toast.makeText(context, "peoples", Toast.LENGTH_SHORT).show()
                // Convert the taggedPeople list to JSON
                val taggedPeopleJson = Gson().toJson(taggedPeople)

                // Pass the JSON to the BottomSheetFragment
                val bottomSheetFragment = TagPeopleBottomSheetFragment.newInstance(taggedPeopleJson)
                bottomSheetFragment.show(parentFragmentManager, "TagPeopleBottomSheet")
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, peoplesStart, peoplesEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set color and clickable span for location
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
//                Toast.makeText(context, "Location", Toast.LENGTH_SHORT).show()
                context.openGoogleMaps(lat, lng)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, locationStart, locationEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the spannable text and make the TextView clickable
        binding.allDescriptionTv.text = spannableString
        binding.allDescriptionTv.setBackgroundResource(R.drawable.transparent_background)
        binding.allDescriptionTv.movementMethod = LinkMovementMethod.getInstance()
    }
    private fun generateTaggedRefString(taggedRef: List<TaggedRef>): String {
        return when (taggedRef.size) {
            0 -> "" // No tagged references
            1 -> taggedRef[0].name ?: "" // Single tagged reference
            2 -> "${taggedRef[0].name} and ${taggedRef[1].name}" // Two tagged references
            else -> "${taggedRef[0].name} and ${taggedRef.size - 1} others" // More than two tagged references
        }
    }

    private fun savePost(id: String) {
        individualViewModal.savePost(id)
    }
    private fun likePost(id: String) {
        individualViewModal.likePost(id)
    }

    private fun joinEvent(id: String) {
        individualViewModal.joinEvent(id)
    }


    private fun updateSaveBtn(postSaved: Boolean, saveIv: ImageView) {
        if (postSaved) {
            saveIv.setImageResource(R.drawable.ic_save_icon)
        } else {
            saveIv.setImageResource(R.drawable.ic_unsave_icon)
        }
    }
    private fun updateLikeBtn(postLiked: Boolean, likeIv: ImageView) {
        if (postLiked) {
            likeIv.setImageResource(R.drawable.ic_like_icon)
        } else {
            likeIv.setImageResource(R.drawable.ic_unlike_icon)
        }
    }


//    private fun showMenuDialog(view: View?, postId: String) {
//
//        // Inflate the dropdown menu layout
//        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        val dropdownView = inflater.inflate(R.layout.single_post_menu_dropdown_item, null)
//
//        // Create the PopupWindow
//        val popupWindow = PopupWindow(
//            dropdownView,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            true
//        )
//
//        // Find TextViews and set click listeners
////        val blockBtn: TextView = dropdownView.findViewById(R.id.blockBtn)
//        val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
////        val shareBtn: TextView = dropdownView.findViewById(R.id.shareBtn)
//
//        reportBtn.setOnClickListener {
//            reportPost(postId)
//            popupWindow.dismiss()
//        }
//
//        // Set the background drawable to make the popup more visually appealing
//        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))
//        // Show the popup window
//        popupWindow.showAsDropDown(view)
//        // Optionally, dismiss the popup when clicking outside of it
//        popupWindow.setOnDismissListener {
//            // Handle any actions you want to perform when the popup is dismissed
//        }
//    }


    private fun showMenuDialog(view: View?, postId: String, itemData: Data?, post: Data?, canShareToStory: Boolean = false) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Check if user is the owner of the post
        val isOwner = post?.let { (it.userID ?: "") == ownerUserId } ?: false

        // Decide which layout to use based on the value of `from`
        val dropdownView = if (from == "Profile") {
            inflater.inflate(R.layout.delete_edit_post_menu_dropdown_item, null)
        } else {
            inflater.inflate(R.layout.single_post_menu_dropdown_item, null)
        }

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
        if (from == "Profile") {
            val editBtn: TextView? = dropdownView.findViewById(R.id.editBtn)
            val deleteBtn: TextView? = dropdownView.findViewById(R.id.deleteBtn)
            val reportBtn: TextView? = dropdownView.findViewById(R.id.reportBtn)
            val addToStoryBtn: TextView? = dropdownView.findViewById(R.id.addToStoryBtn)

            // Show/hide edit and delete buttons based on ownership
            editBtn?.visibility = if (isOwner) View.VISIBLE else View.GONE
            deleteBtn?.visibility = if (isOwner) View.VISIBLE else View.GONE

            // Edit button click listener
            editBtn?.setOnClickListener {
                val currentContent = post?.content.orEmpty()
                val currentFeeling = post?.feelings
                val currentMedia = post?.mediaRef ?: emptyList()
                val location = post?.location
                val placeName = location?.placeName
                val lat = location?.lat
                val lng = location?.lng
                
                if (postId.isNotEmpty()) {
                    com.thehotelmedia.android.activity.userTypes.forms.EditPostActivity.start(
                        context,
                        postId,
                        currentContent,
                        currentFeeling,
                        currentMedia,
                        placeName,
                        lat,
                        lng
                    )
                }
                popupWindow.dismiss()
            }

            // Delete button click listener
            deleteBtn?.setOnClickListener {
                val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDeletePost(context))
                bottomSheet.onYesClicked = {
                    if (itemData != null) {
                        removeItem(itemData)
                        individualViewModal.deletePost(postId)
                    }
                }
                bottomSheet.onNoClicked = {
                    // User cancelled, do nothing
                }
                bottomSheet.show(parentFragmentManager, "YesOrNoBottomSheet")
                popupWindow.dismiss()
            }

            // Report button click listener
            reportBtn?.setOnClickListener {
                reportPost(postId)
                popupWindow.dismiss()
            }

            val shouldShowAddToStory = canShareToStory && !isOwner && isStoryShareEligible(post)
            addToStoryBtn?.visibility = if (shouldShowAddToStory) View.VISIBLE else View.GONE
            addToStoryBtn?.setOnClickListener {
                onStoryShareRequested?.invoke(postId)
                popupWindow.dismiss()
            }
        } else {
            val reportBtn: TextView? = dropdownView.findViewById(R.id.reportBtn)
            val addToStoryBtn: TextView? = dropdownView.findViewById(R.id.addToStoryBtn)

            reportBtn?.setOnClickListener {
                reportPost(postId)
                popupWindow.dismiss()
            }

            val shouldShowAddToStory = canShareToStory && isStoryShareEligible(post)
            addToStoryBtn?.visibility = if (shouldShowAddToStory) View.VISIBLE else View.GONE
            addToStoryBtn?.setOnClickListener {
                onStoryShareRequested?.invoke(postId)
                popupWindow.dismiss()
            }
        }

        // Set the background drawable to make the popup visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(view)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }

    private fun isStoryShareEligible(post: Data?): Boolean {
        if (post == null) return false
        if (isMyPost(post)) return false
        if (!hasShareableMedia(post)) return false
        if (!post.Id.isNullOrBlank() && sharedStoryPostIds.contains(post.Id)) return false
        val isFollowing = viewerFollowsOwner ||
                post.postedBy?.isFollowedByMe == true ||
                post.postedBy?.businessProfileRef?.isFollowedByMe == true
        return isFollowing
    }

    private fun hasShareableMedia(post: Data): Boolean {
        if (post.mediaRef.isEmpty()) return false
        return post.mediaRef.any { media ->
            val type = media.mediaType?.lowercase(Locale.getDefault())
            val mimeType = media.mimeType?.lowercase(Locale.getDefault())
            val isImage = type == "image" || (mimeType?.startsWith("image") == true)
            val isVideo = type == "video" || (mimeType?.startsWith("video") == true)
            val hasSource = !media.sourceUrl.isNullOrBlank()
            (isImage || isVideo) && hasSource
        }
    }

    fun updateViewerFollowState(isFollowing: Boolean) {
        viewerFollowsOwner = isFollowing
        notifyDataSetChanged()
    }

    fun markPostShared(postId: String) {
        sharedStoryPostIds.add(postId)
        notifyDataSetChanged()
    }

    private fun isMyPost(post: Data): Boolean {
        if (ownerUserId.isBlank()) return false
        val directOwnerId = post.userID
        val postedById = post.postedBy?.Id
        return ownerUserId.equals(directOwnerId, ignoreCase = true) ||
                ownerUserId.equals(postedById, ignoreCase = true)
    }

    private fun publishPostToStory(postId: String) {
        if (postId.isBlank()) return
        individualViewModal.publishPostToStory(postId)
    }


    private fun reportPost(postId: String) {

        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", postId)
                putString("TYPE", "post")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportPosts(postId,selectedReason)
            }
        }
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)

    }

    fun setActivePosition(position: Int) {
        activePosition = position
    }

    class SAVED_FEED_DIFF_CALLBACK : DiffUtil.ItemCallback<Data>() {
        override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
            // Compare key fields that affect UI, including collaborators
            return oldItem.Id == newItem.Id &&
                   oldItem.likes == newItem.likes &&
                   oldItem.comments == newItem.comments &&
                   oldItem.savedByMe == newItem.savedByMe &&
                   oldItem.likedByMe == newItem.likedByMe &&
                   oldItem.views == newItem.views &&
                   collaboratorsEqual(oldItem.collaborators, newItem.collaborators)
        }
        
        // Helper function to compare collaborator lists efficiently
        private fun collaboratorsEqual(old: ArrayList<com.thehotelmedia.android.modals.feeds.feed.Collaborator>?, 
                                       new: ArrayList<com.thehotelmedia.android.modals.feeds.feed.Collaborator>?): Boolean {
            if (old == null && new == null) return true
            if (old == null || new == null) return false
            if (old.size != new.size) return false
            
            // Compare by IDs only (fast comparison)
            val oldIds = old.mapNotNull { it._id }.sorted()
            val newIds = new.mapNotNull { it._id }.sorted()
            return oldIds == newIds
        }
    }


    private fun removeItem(item: Data) {
        lifecycleScope.launch {
            val currentList = snapshot().items // Get the current items
            val updatedList = currentList.filter { it.Id != item.Id } // Remove the clicked item

            // Convert the updated list to PagingData and submit it
            val newPagingData = PagingData.from(updatedList)
            submitData(newPagingData) // Submit the new data
            notifyDataSetChanged()
        }
    }

    // Map to track pending collaborator requests: postId -> (binding, mainUserName, userId)
    private val pendingCollaboratorRequests = mutableMapOf<String, Triple<PostItemsLayoutBinding, String, String>>()
    private var collaboratorObserverInitialized = false
    
    // Load collaborator details from API when only IDs are available
    private fun loadCollaboratorsForPost(postId: String, binding: PostItemsLayoutBinding, mainUserName: String, postOwnerId: String) {
        // Check if we already have a pending request for this post
        if (pendingCollaboratorRequests.containsKey(postId)) {
            android.util.Log.d("SavedFeedAdapter", "Already have pending request for postId: $postId")
            return
        }
        
        // Store the request info
        pendingCollaboratorRequests[postId] = Triple(binding, mainUserName, postOwnerId)
        android.util.Log.d("SavedFeedAdapter", "Stored pending request for postId: $postId, total pending: ${pendingCollaboratorRequests.size}")
        
        // Initialize observer once if not already done
        if (!collaboratorObserverInitialized) {
            collaboratorObserverInitialized = true
            // Use observeForever since SavedFeedAdapter doesn't have a LifecycleOwner
            individualViewModal.postCollaboratorsResult.observeForever { result ->
                if (result.status == true && result.data != null) {
                    android.util.Log.d("SavedFeedAdapter", "Received collaborator result, data size: ${result.data.size}, pending requests: ${pendingCollaboratorRequests.size}")
                    
                    // Find the first pending request (responses come sequentially)
                    val pendingRequest = pendingCollaboratorRequests.entries.firstOrNull()
                    
                    if (pendingRequest != null) {
                        val (requestedPostId, requestData) = pendingRequest
                        val (pendingBinding, pendingMainUserName, pendingPostOwnerId) = requestData
                        
                        android.util.Log.d("SavedFeedAdapter", "Processing collaborator data for postId: $requestedPostId, collaborators count: ${result.data.size}")
                        
                        // Remove from pending requests
                        pendingCollaboratorRequests.remove(requestedPostId)
                        
                        // Convert CollaborationUser to Collaborator
                        val collaborators = result.data.map { collabUser ->
                            com.thehotelmedia.android.modals.feeds.feed.Collaborator(
                                _id = collabUser._id,
                                name = collabUser.name,
                                profilePic = collabUser.profilePic?.let { pic ->
                                    com.thehotelmedia.android.modals.feeds.feed.CollaboratorProfilePic(
                                        small = pic.small,
                                        medium = pic.medium,
                                        large = pic.large
                                    )
                                }
                            )
                        }
                        
                        android.util.Log.d("SavedFeedAdapter", "Converted collaborators: ${collaborators.map { "${it._id}:${it.name}" }}")
                        
                        // Filter out post owner and empty names
                        val validCollaborators = collaborators.filter { 
                            !it.name.isNullOrEmpty() && it._id != pendingPostOwnerId 
                        }
                        
                        android.util.Log.d("SavedFeedAdapter", "Valid collaborators after filtering: ${validCollaborators.size}, names: ${validCollaborators.map { it.name }}")
                        
                        if (validCollaborators.isNotEmpty()) {
                            // Verify binding is still valid (check if it's still for the same post)
                            val currentPostId = pendingBinding.nameTv.tag as? String ?: ""
                            if (currentPostId == pendingPostOwnerId || currentPostId.isEmpty()) {
                                // Verify the binding tag matches to prevent stale updates
                                val verifyTag = pendingBinding.nameTv.tag as? String ?: ""
                                if (verifyTag == pendingPostOwnerId || verifyTag.isEmpty()) {
                                    // Build collaborator text and cache it
                                    val collaboratorText = buildCollaboratorText(validCollaborators, pendingMainUserName, pendingPostOwnerId, pendingBinding)
                                    // Cache the result for future rebinds (both memory and persistent storage)
                                    collaboratorTextCache[requestedPostId] = collaboratorText
                                    saveCollaboratorTextToPrefs(requestedPostId, collaboratorText)
                                    // Set tag and text together to prevent flickering
                                    pendingBinding.nameTv.tag = pendingPostOwnerId
                                    // Only update if text is different
                                    if (pendingBinding.nameTv.text.toString() != collaboratorText.toString()) {
                                        pendingBinding.nameTv.text = collaboratorText
                                        // Make TextView clickable if it contains clickable spans
                                        if (collaboratorText is android.text.Spannable) {
                                            pendingBinding.nameTv.movementMethod = android.text.method.LinkMovementMethod.getInstance()
                                        }
                                    }
                                    android.util.Log.d("SavedFeedAdapter", "Displaying collaborators: ${validCollaborators.map { it.name }}")
                                } else {
                                    android.util.Log.d("SavedFeedAdapter", "Binding tag mismatch, skipping update")
                                }
                            } else {
                                android.util.Log.d("SavedFeedAdapter", "Binding was recycled, skipping update for postId: $requestedPostId")
                            }
                        } else {
                            android.util.Log.d("SavedFeedAdapter", "No valid collaborators to display for postId: $requestedPostId")
                        }
                    } else {
                        android.util.Log.d("SavedFeedAdapter", "No pending request found for collaborator result")
                    }
                } else {
                    android.util.Log.d("SavedFeedAdapter", "Collaborator result failed or empty, status: ${result.status}, message: ${result.message}")
                }
            }
        }
        
        // Make the API call
        android.util.Log.d("SavedFeedAdapter", "Calling getPostCollaborators for postId: $postId")
        individualViewModal.getPostCollaborators(postId)
    }
    
    // Build collaborator text with blue styling (returns SpannableString)
    private fun buildCollaboratorText(
        collaborators: List<com.thehotelmedia.android.modals.feeds.feed.Collaborator>, 
        mainUserName: String,
        mainUserId: String,
        binding: PostItemsLayoutBinding
    ): CharSequence {
        return try {
            if (collaborators.isEmpty()) {
                // Even for single user, make it clickable
                val spannableString = android.text.SpannableString(mainUserName)
                spannableString.setSpan(
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: android.view.View) {
                            moveToBusinessProfileDetailsActivity(mainUserId)
                        }
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            // Explicitly set text color to ensure visibility
                            ds.color = ContextCompat.getColor(context, R.color.text_color)
                        }
                    },
                    0,
                    mainUserName.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Also set foreground color span to ensure the text is visible
                spannableString.setSpan(
                    android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_color)),
                    0,
                    mainUserName.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spannableString
            }

            // Format text: "mainUserName and collaboratorName" or "mainUserName and X others"
            val collaboratorName = when {
                collaborators.size == 1 -> collaborators[0]?.name ?: ""
                else -> "${collaborators.size} others"
            }
            
            if (collaboratorName.isEmpty()) {
                val spannableString = android.text.SpannableString(mainUserName)
                spannableString.setSpan(
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: android.view.View) {
                            moveToBusinessProfileDetailsActivity(mainUserId)
                        }
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            // Explicitly set text color to ensure visibility
                            ds.color = ContextCompat.getColor(context, R.color.text_color)
                        }
                    },
                    0,
                    mainUserName.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Also set foreground color span to ensure the text is visible
                spannableString.setSpan(
                    android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_color)),
                    0,
                    mainUserName.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                return spannableString
            }
            
            val fullText = "$mainUserName and $collaboratorName"
            
            // Create SpannableString to style collaborator name in blue and make both names clickable
            val spannableString = android.text.SpannableString(fullText)
            
            // Get collaborator ID(s) - use first collaborator's ID if single, or empty if multiple
            val collaboratorId = if (collaborators.size == 1) {
                collaborators[0]?._id ?: ""
            } else {
                "" // For "X others", we can't navigate to a specific profile
            }
            
            // Make main user name clickable (from start to "and")
            val mainUserNameEndIndex = fullText.indexOf(" and")
            if (mainUserNameEndIndex > 0) {
                spannableString.setSpan(
                    object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: android.view.View) {
                            moveToBusinessProfileDetailsActivity(mainUserId)
                        }
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            // Explicitly set text color to ensure visibility
                            ds.color = ContextCompat.getColor(context, R.color.text_color)
                        }
                    },
                    0,
                    mainUserNameEndIndex,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // Also set foreground color span to ensure the text is visible
                spannableString.setSpan(
                    android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_color)),
                    0,
                    mainUserNameEndIndex,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            // Make collaborator name clickable and blue (from "and " to end)
            val collaboratorStartIndex = fullText.indexOf("and") + 4 // Start after "and "
            val endIndex = fullText.length
            
            if (collaboratorStartIndex < endIndex && collaboratorStartIndex > 0 && collaboratorStartIndex < fullText.length) {
                // Set blue color
                spannableString.setSpan(
                    android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)),
                    collaboratorStartIndex,
                    endIndex,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // Make clickable only if we have a single collaborator ID
                if (collaboratorId.isNotEmpty()) {
                    spannableString.setSpan(
                        object : android.text.style.ClickableSpan() {
                            override fun onClick(widget: android.view.View) {
                                moveToBusinessProfileDetailsActivity(collaboratorId)
                            }
                            override fun updateDrawState(ds: android.text.TextPaint) {
                                super.updateDrawState(ds)
                                ds.color = ContextCompat.getColor(context, R.color.blue)
                                ds.isUnderlineText = false
                            }
                        },
                        collaboratorStartIndex,
                        endIndex,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            
            spannableString
        } catch (e: Exception) {
            // If there's any error, just return the main user name
            android.util.Log.e("SavedFeedAdapter", "Error building collaborator text: ${e.message}", e)
            mainUserName
        }
    }
    
    // Display collaborators inline with blue text styling (no avatars, no API calls)
    // DEPRECATED: This function is no longer used - we now build collaborator text directly in setPostData
    private fun displayCollaboratorsInline(collaborators: List<com.thehotelmedia.android.modals.feeds.feed.Collaborator>, binding: PostItemsLayoutBinding, mainUserName: String, mainUserId: String) {
        binding.nameTv.text = buildCollaboratorText(collaborators, mainUserName, mainUserId, binding)
        if (binding.nameTv.text is android.text.Spannable) {
            binding.nameTv.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        }
    }

    // DEPRECATED: Old loadCollaborators function - no longer used (collaborators come with post data)
    // This function is kept for reference but should not be called
    private fun loadCollaborators_DEPRECATED(postId: String, binding: PostItemsLayoutBinding, postOwnerId: String, mainUserName: String) {
        // This function is deprecated - collaborators are now loaded with post data
        // Keeping for reference only
    }

    // Format and display collaborators like Instagram: main user avatar + overlapping collaborator avatars + "mainUser and collaborator" text
    private fun displayCollaborators(collaborators: List<CollaborationUser>, binding: PostItemsLayoutBinding, postOwnerId: String, mainUserName: String) {
        // Verify binding is still for this post (prevent stale updates from recycled views)
        val currentPostId = binding.nameTv.tag as? String ?: ""
        if (currentPostId != postOwnerId) {
            // View was recycled, don't update
            return
        }
        
        // Double-check: Filter out the post owner and empty names
        val validCollaborators = collaborators.filter { 
            !it.name.isNullOrEmpty() && it._id != postOwnerId 
        }
        
        if (validCollaborators.isEmpty()) {
            // No valid collaborators - hide UI and restore original name
            binding.collaboratorsProfileContainer.visibility = View.GONE
            binding.collaboratorProfileIv1.visibility = View.GONE
            binding.collaboratorProfileIv2.visibility = View.GONE
            binding.collaboratorProfileIv3.visibility = View.GONE
            // Restore original name only if binding is still for this post
            if (binding.nameTv.tag as? String == postOwnerId) {
                binding.nameTv.text = mainUserName
            }
            return
        }

        // Show overlapping collaborator profile pictures (up to 2, overlapping)
        val profilePicsToShow = validCollaborators.take(2)
        
        // Show first collaborator profile picture
        if (profilePicsToShow.isNotEmpty()) {
            val firstCollaborator = profilePicsToShow[0]
            val profilePicUrl1 = firstCollaborator.profilePic?.large ?: firstCollaborator.profilePic?.medium ?: firstCollaborator.profilePic?.small
            if (!profilePicUrl1.isNullOrEmpty()) {
                Glide.with(context)
                    .load(profilePicUrl1)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.collaboratorProfileImage1)
                binding.collaboratorProfileIv1.visibility = View.VISIBLE
            } else {
                binding.collaboratorProfileIv1.visibility = View.GONE
            }
        } else {
            binding.collaboratorProfileIv1.visibility = View.GONE
        }
        
        // Show second collaborator profile picture (overlapping)
        if (profilePicsToShow.size >= 2) {
            val secondCollaborator = profilePicsToShow[1]
            val profilePicUrl2 = secondCollaborator.profilePic?.large ?: secondCollaborator.profilePic?.medium ?: secondCollaborator.profilePic?.small
            if (!profilePicUrl2.isNullOrEmpty()) {
                Glide.with(context)
                    .load(profilePicUrl2)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(binding.collaboratorProfileImage2)
                binding.collaboratorProfileIv2.visibility = View.VISIBLE
            } else {
                binding.collaboratorProfileIv2.visibility = View.GONE
            }
        } else {
            binding.collaboratorProfileIv2.visibility = View.GONE
        }
        
        // Hide third profile picture
        binding.collaboratorProfileIv3.visibility = View.GONE

        // Format text: "mainUserName and collaboratorName" or "mainUserName and X others"
        val collaborationText = when {
            validCollaborators.size == 1 -> {
                val collaboratorName = validCollaborators[0].name ?: ""
                "$mainUserName and $collaboratorName"
            }
            validCollaborators.size == 2 -> {
                val collaboratorName = validCollaborators[0].name ?: ""
                "$mainUserName and $collaboratorName"
            }
            else -> {
                val remaining = validCollaborators.size
                "$mainUserName and $remaining others"
            }
        }

        // Final verification before updating UI
        if (binding.nameTv.tag as? String == postOwnerId) {
            binding.nameTv.text = collaborationText
            binding.collaboratorsProfileContainer.visibility = View.VISIBLE
        }
    }
}
