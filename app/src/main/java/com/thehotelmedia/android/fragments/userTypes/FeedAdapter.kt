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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.activity.ViewAllSuggestionActivity
import com.thehotelmedia.android.activity.ViewEventDetailsActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.SuggestedBusinessAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaActionCallback
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaPagerAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.home.StoryAdapter
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.TagPeopleBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.databinding.EventItemsLayoutBinding
import com.thehotelmedia.android.databinding.FeedHeaderLayoutBinding
import com.thehotelmedia.android.databinding.PostItemsLayoutBinding
import com.thehotelmedia.android.databinding.ReviewItemsLayoutBinding
import com.thehotelmedia.android.databinding.SuggestionItemsLayoutBinding
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.getEmojiForRating
import com.thehotelmedia.android.extensions.isFutureDateOrTime
import com.thehotelmedia.android.extensions.moveToPostPreviewScreen
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

// DiffUtil callback to calculate the differences between old and new items
val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Data>() {
    override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
        return oldItem.Id == newItem.Id // Assuming _id is unique
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

class FeedAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val parentFragmentManager: FragmentManager,
    private val viewLifecycleOwner: LifecycleOwner,
    private val headerUserProfilePic: String,
    private val postRecyclerView: RecyclerView,
    private val ownerUserId: String,
    private val onIdsUpdated: (List<String>) -> Unit
) : PagingDataAdapter<Data, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    private var dotsIndicator: SpringDotsIndicator? = null

    private var activePosition = 0 // No active position initially

    companion object {
        private const val VIEW_TYPE_HEADER = -1
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_REVIEW = 1
        private const val VIEW_TYPE_EVENT = 2
        private const val VIEW_ALL_SUGGESTION = 3
    }

    private var previousVisible = true // Track previous visibility status
    private var currentlyVisiblePosition = RecyclerView.NO_POSITION // To store the currently visible position

    private lateinit var mediaPagerAdapter : MediaPagerAdapter
    private lateinit var suggestedBusinessAdapter: SuggestedBusinessAdapter

    private var currentlyPlayingPosition = -1 // Track which video is currently playing


        private val visiblePositions = mutableSetOf<Int>()

    private val selectedIds = mutableListOf<String>()
    private val handlerMap = mutableMapOf<String, Job>()
    
    // Cache for collaborators to avoid redundant API calls
    private val collaboratorsCache = mutableMapOf<String, List<com.thehotelmedia.android.modals.collaboration.CollaborationUser>>()
    // Store original names to restore them if no collaborators
    private val originalNamesCache = mutableMapOf<String, String>()
    // In-memory cache for collaborator display text (postId -> "User1 and User2")
    private val collaboratorTextCache = mutableMapOf<String, CharSequence>()



    private fun onItemActive(id: String, isActive: Boolean) {
        if (isActive) {
            val job = CoroutineScope(Dispatchers.Main).launch {
                delay(3000)  // 3 seconds wait
                if (!selectedIds.contains(id)) { // Should not already exist
                    selectedIds.add(id)
                    onIdsUpdated(selectedIds)  // Call lambda function to update IDs
                }
            }
            handlerMap[id] = job
        } else {
            handlerMap[id]?.cancel()
            handlerMap.remove(id)
        }
    }


    // Function to remove all IDs and notify activity
    fun removeAllIds() {
        if (selectedIds.isNotEmpty()) {
            selectedIds.clear()
            println("All IDs removed")
            onIdsUpdated(selectedIds)  // Notify activity with empty list
        }
    }


            // Add a new ViewHolder for the header using binding
    inner class HeaderViewHolder(private val binding: FeedHeaderLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {

            val storyAdapter = StoryAdapter(context, headerUserProfilePic)
            if (binding.recyclerView.adapter == null) {
                binding.recyclerView.adapter = storyAdapter.withLoadStateFooter(footer = LoaderAdapter())
            }
            individualViewModal.getStories().observe(viewLifecycleOwner) { data ->
                viewLifecycleOwner.lifecycleScope.launch {
                    storyAdapter.submitData(data)
                }
            }



        }
    }


    inner class PostViewHolder(val binding: PostItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Data,isActive: Boolean) {
            try {
                // Cancel any pending async updates for this view to prevent stale data
                val postId = post.Id ?: ""
                if (pendingCollaboratorRequests.containsKey(postId)) {
                    // Remove pending request if view is being rebound (prevents stale updates)
                    pendingCollaboratorRequests.remove(postId)
                }
                setPostData(post,binding,isActive)
            } catch (e: Exception) {
                // Catch any exceptions to prevent crashes
                android.util.Log.e("FeedAdapter", "Error binding post data: ${e.message}", e)
                // Set default values to prevent UI from breaking
                binding.nameTv.text = "User"
                binding.collaboratorsProfileContainer.visibility = View.GONE
            }
        }
    }


    inner class ReviewViewHolder(val binding: ReviewItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Data,isActive: Boolean) {
            setReviewData(review,binding,isActive)
        }
    }



    inner class EventViewHolder(val binding: EventItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: Data,isActive: Boolean) {
            setEventData(event,binding,isActive)
        }
    }

    inner class SuggestionViewHolder(val binding: SuggestionItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(suggestios: Data,isActive: Boolean) {
            println("Asdjaksjdk    fsssaaaa   $suggestios")
            setSuggestionData(suggestios,binding,isActive)
        }
    }



    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_HEADER // Return header view type for position 0
        } else {
            val item = getItem(position - 1)
            return when (item?.postType) {
                "post" -> VIEW_TYPE_POST
                "review" -> VIEW_TYPE_REVIEW
                "event" -> VIEW_TYPE_EVENT
                "suggestion" -> VIEW_ALL_SUGGESTION
                else -> throw IllegalArgumentException("Invalid post type: ${item?.postType}")
            }
        }
    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = FeedHeaderLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
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
            VIEW_ALL_SUGGESTION -> {
                val binding = SuggestionItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SuggestionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val isActive = position == activePosition
        if (holder is HeaderViewHolder) {
            // Bind header-specific data
            holder.bind()
        } else {
            val actualPosition = position - 1
            val totalItemCount = itemCount
            val isLastItem = position == totalItemCount - 1

            when (val item = getItem(actualPosition)) {
                is Data -> {
                    when (holder) {
                        is PostViewHolder -> {
                            holder.bind(item , isActive)
                            applyPadding(holder.binding.root,isLastItem)
                        }
                        is ReviewViewHolder -> {
                            holder.bind(item , isActive)
                            applyPadding(holder.binding.root,isLastItem)
                        }
                        is EventViewHolder -> {
                            holder.bind(item , isActive)
                            applyPadding(holder.binding.root,isLastItem)
                        }
                        is SuggestionViewHolder -> {
                            holder.bind(item , isActive)
                            applyPadding(holder.binding.root,isLastItem)
                        }
                    }
                }
                else -> throw IllegalArgumentException("Invalid item")
            }
        }
    }

    private fun applyPadding(view: View, isLastItem: Boolean) {
        val paddingStart = dpToPx(context, 12)
        val paddingEnd = dpToPx(context, 12)
        val bottom = dpToPx(context, 100)
        val paddingBottom = if (isLastItem) bottom else 0 // Add bottom padding to the last item
        view.setPadding(paddingStart, view.paddingTop, paddingEnd, paddingBottom)
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }


    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }


    private fun setPostData(post: Data, binding: PostItemsLayoutBinding,isActive: Boolean) {
        try {
            // Media setup
            val mediaList = post.mediaRef ?: arrayListOf()
            val postId = post.Id ?: ""

        onItemActive(postId, isActive)

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

        println("adjgafjkgjdsf   viewsCount  $viewsCount")
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        if (mediaList.isNotEmpty()){
            dotsIndicator = binding.indicatorLayout.apply {
                dotsClickable = false
                setDotIndicatorColor(ContextCompat.getColor(context, R.color.blue))
                setStrokeDotsIndicatorColor(ContextCompat.getColor(context, R.color.grey))
            }
            mediaPagerAdapter = MediaPagerAdapter(context, mediaList, isActive,postId,isPostLiked,likeCount,commentCount)
            { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                post.comments = updatedCommentCount
                post.likedByMe = updatedIsLikedByMe
                post.likes = updatedLikeCount
                updateLikeBtn(updatedIsLikedByMe, binding.likeIv)
                binding.likeTv.text = updatedLikeCount.toString()
                binding.commentTv.text = updatedCommentCount.toString()
                // You can also update UI elements in the activity here
            }
            binding.viewPager.adapter = mediaPagerAdapter
            binding.mediaLayout.visibility = View.VISIBLE
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

//            binding.locationTv.text = "$state, $country"

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
        
        // Check cache FIRST - if we have cached text, use it immediately (no flicker)
        val cachedText = collaboratorTextCache[postId]
        if (cachedText != null) {
            // Only update if text is different (prevents redundant setText calls during rebind)
            val currentText = binding.nameTv.text?.toString() ?: ""
            if (currentText != cachedText.toString()) {
                binding.nameTv.text = cachedText
            }
            // Don't process collaborators again if we have cached text - saves CPU cycles
            return
        }
        
        // Calculate final name text synchronously (before any UI updates)
        val finalNameText: CharSequence = try {
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
                        // Build collaborator text with blue styling
                        val collaboratorText = buildCollaboratorText(validCollaborators, name)
                        // Cache the result for future rebinds
                        collaboratorTextCache[postId] = collaboratorText
                        collaboratorText
                    } else {
                        // If we have collaborators but no valid ones (only IDs), mark for API call
                        val collaboratorsWithOnlyIds = collaborators.filter { it._id != userId && it.name.isNullOrEmpty() && !it._id.isNullOrEmpty() }
                        if (collaboratorsWithOnlyIds.isNotEmpty()) {
                            // Load collaborators from API (async, will update later)
                            loadCollaboratorsForPost(postId, binding, name, userId)
                        }
                        // Cache normal name to prevent re-processing
                        collaboratorTextCache[postId] = name
                        name
                    }
                } catch (e: Exception) {
                    // If there's any error processing collaborators, just show normal name
                    android.util.Log.e("FeedAdapter", "Error processing collaborators: ${e.message}", e)
                    // Cache normal name
                    collaboratorTextCache[postId] = name
                    name
                }
            } else {
                // Cache normal name
                collaboratorTextCache[postId] = name
                name
            }
        } catch (e: Exception) {
            // If there's any error accessing collaborators, just show normal name
            android.util.Log.e("FeedAdapter", "Error accessing collaborators field: ${e.message}", e)
            // Cache normal name
            collaboratorTextCache[postId] = name
            name
        }
        
        // Only set text if it's different (prevents redundant updates during rebind)
        if (binding.nameTv.text.toString() != finalNameText.toString()) {
            binding.nameTv.text = finalNameText
        }


        // Post content
        val content = post.content.orEmpty().trim()
        val feelings = post.feelings.orEmpty().trim()
        val location = post.location?.placeName.orEmpty().trim()



        val lat  = post.location?.lat ?: 0.00
        val lng  = post.location?.lng ?: 0.00
        val taggedPeople = post.taggedRef


//        val taggedRefString = generateTaggedRefString(post.taggedRef)
//        setDescriptionTextAndClick(binding, content, feelings, taggedRefString, location)
        if (content.isNotEmpty() || feelings.isNotEmpty() || location.isNotEmpty() || taggedPeople.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                val taggedRefString = generateTaggedRefString(post.taggedRef)
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
            binding.likeTv.text =  formatCount(likeCount)
            post.likes = likeCount
            post.likedByMe = isPostLiked

            mediaPagerAdapter.updateLikeBtn(isPostLiked,likeCount)
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
                    post.comments = commentCount
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

            context.sharePostWithDeepLink(postId,ownerUserId)
        }


        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view,postId)
        }
        } catch (e: Exception) {
            // Catch any exceptions to prevent crashes
            android.util.Log.e("FeedAdapter", "Error in setPostData: ${e.message}", e)
            // Set safe defaults
            binding.nameTv.text = "User"
            binding.collaboratorsProfileContainer.visibility = View.GONE
        }
    }

    private fun setReviewData(review: Data, binding: ReviewItemsLayoutBinding,isActive: Boolean) {
        // Rating
        val rating = review.rating?.toInt() ?: 0
        val averageRating = review.reviewedBusinessProfileRef?.rating ?: 0.0

        // Save, Like, and Comment buttons actions
        val postId = review.Id.toString()
        val googleReviewedBusiness = review.googleReviewedBusiness ?: ""
        val publicUserID = review.publicUserID ?: ""


        onItemActive(postId, isActive)

        binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
        binding.ratingTV.setRatingWithStars(rating)
        binding.ratingTypeTV.text = getEmojiForRating(rating)


        val viewsCount = review.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        // Blur views
        context.blurTheView(binding.topBlurView)
        context.blurTheView(binding.bottomBlurView)

        // Review Details
        val postedBy = review.postedBy ?: return
        val createdAt = review.createdAt ?: ""
        var commentCount = review.comments ?: 0
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

        // Like, Comment, and Share Counts

        val likeCount = review.likes ?: 0
        val shareCount = review.shared ?: 0


        binding.likeTv.text = formatCount(likeCount)
        binding.commentTv.text = formatCount(commentCount)
        binding.shareTv.text = formatCount(shareCount)

        // Content and Date
        binding.contentTv.text = review.content
        binding.daysTv.text = calculateDaysAgo(createdAt,context)

        // Post interaction states
        var isPostSaved = review.savedByMe ?: false
        var isPostLiked = review.likedByMe ?: false
        updateSaveBtn(isPostSaved, binding.saveIv)
        updateLikeBtn(isPostLiked, binding.likeIv)

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
//        // User layout click action
//        binding.userLayout.setOnClickListener {
//            moveToBusinessProfileDetailsActivity(review.postedBy?.Id ?: "")
//        }
        binding.userLayout.setOnClickListener {
            if (publicUserID.isNotEmpty()){
                Toast.makeText(context,context.getString(R.string.profile_not_associated_thm), Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(review.postedBy?.Id ?: "")
            }
        }


        binding.businessLayout.setOnClickListener {
            if (googleReviewedBusiness.isNotEmpty()){
                Toast.makeText(context,context.getString(R.string.profile_not_associated_thm),Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(review.reviewedBusinessProfileRef?.userId ?: "")
            }
        }

        // Share button click
        binding.shareBtn.setOnClickListener {
            context.sharePostWithDeepLink(postId,ownerUserId)
        }

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view, postId)
        }
    }





    private fun setEventData(event: Data, binding: EventItemsLayoutBinding,isActive: Boolean) {
        // Extract necessary data
        val averageRating = event.postedBy?.businessProfileRef?.businessRating ?: 0.0
        val shareCount = event.shared ?: 0
        val userId = event.postedBy?.Id ?: ""
        val postId = event.Id ?: ""

        onItemActive(postId, isActive)

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

        val viewsCount = event.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = viewsCount.toString()
        }

        // Set UI elements
        binding.shareTv.text = formatCount(shareCount)
        binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
        binding.nameTv.text = name
//        binding.profileIv.loadImageInBackground(context, profilePic, R.drawable.ic_profile_placeholder)
//        binding.coverImage.loadImageInBackground(context, coverImage, R.drawable.ic_post_placeholder)


        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        Glide.with(context).load(coverImage).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.coverImage)




        binding.typeTv.text = "$businessType - $businessSubType"
        binding.eventNameTv.text = eventName.trim()
        binding.eventVenueTv.text = venue.trim()
        binding.dateTimeTv.text = formatEventDateTime(dateString, timeString)

        if (interestedPeople != 0){
            binding.peopleInterestedTv.text = "$interestedPeople ${context.getString(R.string.interested_people)}"
            binding.peopleInterestedTv.visibility = View.VISIBLE
        }else{
            binding.peopleInterestedTv.visibility = View.GONE
        }


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
            showMenuDialog(view, postId)
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


    private fun setSuggestionData(
        suggestios: Data,
        binding: SuggestionItemsLayoutBinding,
        active: Boolean
    ) {
        val suggestionList = suggestios.suggestionData

//        if (suggestionList.isEmpty()){
//            binding.root.visibility = View.GONE
//            return
//        }


        binding.viewAllBtn.setOnClickListener {
            val intent = Intent(context, ViewAllSuggestionActivity::class.java)
            context.startActivity(intent)
        }

        suggestedBusinessAdapter = SuggestedBusinessAdapter(context,suggestionList,ownerUserId)
        binding.suggestedBusinessRv.adapter = suggestedBusinessAdapter

    }

    private fun formatEventDateTime(dateString: String, timeString: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(timeString)
        val formattedDate = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(date)
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(time)
        return "$formattedDate at $formattedTime"
    }





    private fun updateJoiningBtn(postSaved: Boolean, joiningIv: ImageView, joiningTv: TextView) {
        if (postSaved) {
            joiningIv.setImageResource(R.drawable.ic_filled_star_blue)
            joiningTv.text = "Joined"
        } else {
            joiningIv.setImageResource(R.drawable.ic_outline_star_white)
            joiningTv.text = "Joining ?"
        }
    }

    private fun setDescriptionTextAndClick(
        binding: PostItemsLayoutBinding,
        description: String,
        feeling: String,
        people: String,
        location: String,
        taggedPeople: ArrayList<TaggedRef>,
        lat: Double,
        lng: Double
    ) {
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
//        val matcher = Patterns.WEB_URL.matcher(finalText)
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

    fun removeMediaAddapter() {
        println("adsfaklsdfk   Stoping adpter")
        try {
//            mediaPagerAdapter.stopAllPlayers()
        }catch (e:Exception){
            println("adsfaklsdfk   Stoping  adpter $e")
        }

    }



    private fun showMenuDialog(view: View?, postId: String) {
        // Inflate the dropdown menu layout
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.single_post_menu_dropdown_item, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
//        val blockBtn: TextView = dropdownView.findViewById(R.id.blockBtn)
        val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
//        val shareBtn: TextView = dropdownView.findViewById(R.id.shareBtn)



        reportBtn.setOnClickListener {
            reportPost(postId)
            popupWindow.dismiss()
        }


        // Set the background drawable to make the popup more visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(view)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
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

    // Initialize collaborator observer once
    private var collaboratorObserverInitialized = false
    
    // Map to track pending collaborator requests: postId -> (binding, mainUserName, userId)
    private val pendingCollaboratorRequests = mutableMapOf<String, Triple<PostItemsLayoutBinding, String, String>>()
    
    // Build collaborator text with blue styling (returns SpannableString)
    private fun buildCollaboratorText(collaborators: List<com.thehotelmedia.android.modals.feeds.feed.Collaborator>, mainUserName: String): CharSequence {
        return try {
            if (collaborators.isEmpty()) {
                return mainUserName
            }

            // Format text: "mainUserName and collaboratorName" or "mainUserName and X others"
            val collaboratorName = when {
                collaborators.size == 1 -> collaborators[0]?.name ?: ""
                else -> "${collaborators.size} others"
            }
            
            if (collaboratorName.isEmpty()) {
                return mainUserName
            }
            
            val fullText = "$mainUserName and $collaboratorName"
            
            // Create SpannableString to style collaborator name in blue
            val spannableString = android.text.SpannableString(fullText)
            val startIndex = fullText.indexOf("and") + 4 // Start after "and "
            val endIndex = fullText.length
            
            if (startIndex < endIndex && startIndex > 0 && startIndex < fullText.length) {
                spannableString.setSpan(
                    android.text.style.ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue)),
                    startIndex,
                    endIndex,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            spannableString
        } catch (e: Exception) {
            // If there's any error, just return the main user name
            mainUserName
        }
    }
    
    // Display collaborators inline with blue text styling (no avatars, no API calls)
    private fun displayCollaboratorsInline(collaborators: List<com.thehotelmedia.android.modals.feeds.feed.Collaborator>, binding: PostItemsLayoutBinding, mainUserName: String) {
        binding.nameTv.text = buildCollaboratorText(collaborators, mainUserName)
    }

    // Load collaborator details from API when only IDs are available
    private fun loadCollaboratorsForPost(postId: String, binding: PostItemsLayoutBinding, mainUserName: String, postOwnerId: String) {
        // Check if we already have a pending request for this post
        if (pendingCollaboratorRequests.containsKey(postId)) {
            android.util.Log.d("FeedAdapter", "Already have pending request for postId: $postId")
            return
        }
        
        // Store the request info
        pendingCollaboratorRequests[postId] = Triple(binding, mainUserName, postOwnerId)
        android.util.Log.d("FeedAdapter", "Stored pending request for postId: $postId, total pending: ${pendingCollaboratorRequests.size}")
        
        // Initialize observer once if not already done
        if (!collaboratorObserverInitialized) {
            collaboratorObserverInitialized = true
            individualViewModal.postCollaboratorsResult.observe(viewLifecycleOwner) { result ->
                if (result.status == true && result.data != null) {
                    android.util.Log.d("FeedAdapter", "Received collaborator result, data size: ${result.data.size}, pending requests: ${pendingCollaboratorRequests.size}")
                    
                    // Try to match by finding the first pending request
                    // Since responses come sequentially, process the first one
                    val pendingRequest = pendingCollaboratorRequests.entries.firstOrNull()
                    
                    if (pendingRequest != null) {
                        val (requestedPostId, requestData) = pendingRequest
                        val (pendingBinding, pendingMainUserName, pendingPostOwnerId) = requestData
                        
                        android.util.Log.d("FeedAdapter", "Processing collaborator data for postId: $requestedPostId, collaborators count: ${result.data.size}")
                        
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
                        
                        android.util.Log.d("FeedAdapter", "Converted collaborators: ${collaborators.map { "${it._id}:${it.name}" }}")
                        
                        // Filter out post owner and empty names
                        val validCollaborators = collaborators.filter { 
                            !it.name.isNullOrEmpty() && it._id != pendingPostOwnerId 
                        }
                        
                        android.util.Log.d("FeedAdapter", "Valid collaborators after filtering: ${validCollaborators.size}, names: ${validCollaborators.map { it.name }}")
                        
                        if (validCollaborators.isNotEmpty()) {
                            // Verify binding is still valid (check if it's still for the same post)
                            val currentPostId = pendingBinding.nameTv.tag as? String ?: ""
                            if (currentPostId == pendingPostOwnerId || currentPostId.isEmpty()) {
                                // Verify the binding tag matches to prevent stale updates
                                val verifyTag = pendingBinding.nameTv.tag as? String ?: ""
                                if (verifyTag == pendingPostOwnerId || verifyTag.isEmpty()) {
                                    // Build collaborator text and cache it
                                    val collaboratorText = buildCollaboratorText(validCollaborators, pendingMainUserName)
                                    // Cache the result for future rebinds
                                    collaboratorTextCache[requestedPostId] = collaboratorText
                                    // Set tag and text together to prevent flickering
                                    pendingBinding.nameTv.tag = pendingPostOwnerId
                                    // Only update if text is different
                                    if (pendingBinding.nameTv.text.toString() != collaboratorText.toString()) {
                                        pendingBinding.nameTv.text = collaboratorText
                                    }
                                    android.util.Log.d("FeedAdapter", "Displaying collaborators: ${validCollaborators.map { it.name }}")
                                } else {
                                    android.util.Log.d("FeedAdapter", "Binding tag mismatch, skipping update")
                                }
                            } else {
                                android.util.Log.d("FeedAdapter", "Binding was recycled, skipping update for postId: $requestedPostId")
                            }
                        } else {
                            android.util.Log.d("FeedAdapter", "No valid collaborators to display for postId: $requestedPostId")
                        }
                    } else {
                        android.util.Log.d("FeedAdapter", "No pending request found for collaborator result")
                    }
                } else {
                    android.util.Log.d("FeedAdapter", "Collaborator result failed or empty, status: ${result.status}, message: ${result.message}")
                }
            }
        }
        
        // Make the API call
        android.util.Log.d("FeedAdapter", "Calling getPostCollaborators for postId: $postId")
        individualViewModal.getPostCollaborators(postId)
    }

    // Format and display collaborators like Instagram: main user avatar + overlapping collaborator avatars + "mainUser and collaborator" text
    // DEPRECATED: This function is no longer used - we now use displayCollaboratorsInline instead
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

        // Show overlapping collaborator profile pictures (up to 2-3, overlapping)
        val profilePicsToShow = validCollaborators.take(2) // Show max 2 overlapping avatars
        
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
        
        // Hide third profile picture (we'll show max 2)
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

    fun setActivePosition(position: Int) {
        activePosition = position
    }
}


