package com.thehotelmedia.android.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.MediaItems
import com.thehotelmedia.android.adapters.MediaType
import com.thehotelmedia.android.adapters.VideoImageViewerAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaActionCallback
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.SharePostBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.TwoVerticalOptionBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.Constants.VIDEO
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityVideoImageViewerBinding
import com.thehotelmedia.android.downloadManager.MediaDownloadManager
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.viewpager2.widget.ViewPager2
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.extensions.getTimeAgo
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.modals.feeds.feed.MediaRef
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoImageViewer : DarkBaseActivity() {

    private lateinit var binding: ActivityVideoImageViewerBinding
    private lateinit var adapter: VideoImageViewerAdapter
    private lateinit var mediaList: List<MediaItems>
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var individualRepo: IndividualRepo
    private var exoPlayer: ExoPlayer? = null

    private lateinit var preferenceManager: PreferenceManager
    private var isLikedByMe = false
    private var likeCount = 0
    private var commentCount = 0
    private var postId = ""
    private lateinit var mediaDownloadManager: MediaDownloadManager
    private var isLandscape = false // To track the current state of rotation
    private var isPostSaved = false
    private var isFollowedByMe = false
    private var postOwnerUserId = ""
    
    // Feed data for vertical scrolling
    private var videoPostsList = mutableListOf<Data>()
    private var currentVideoPosition = 0
    private var previousVideoPosition = -1 // Track previous position to detect scroll direction
    private var initialPostId = ""
    private var currentLat = DEFAULT_LAT
    private var currentLng = DEFAULT_LNG
    private var mediaThumbnailUrl = ""
    private var mediaUrl = ""
    private var mediaId = ""
    private var mediaType: String? = null
    private var wasPlayingBeforePause = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()


        onBackPressedDispatcher.addCallback(this) {
            handelBackPress()
        }
    }

    private fun handelBackPress() {
        // Handle back press
        MediaActionCallback.onMediaAction?.invoke(isLikedByMe, likeCount, commentCount)
        finish() // or custom behavior
    }


    private fun initUi() {

        binding.rotateBtn.setOnClickListener {
            if (isLandscape) {
                isLandscape = false
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                isLandscape = true
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        exoPlayer = ExoPlayer.Builder(this).build()

        val individualRepo = IndividualRepo(this)
        this.individualRepo = individualRepo
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        preferenceManager = PreferenceManager.getInstance(this)
        val ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()
        mediaDownloadManager = MediaDownloadManager(this)

        binding.backBtn.setOnClickListener {
            handelBackPress()
        }

        binding.topLeftBackBtn.setOnClickListener {
            handelBackPress()
        }

        mediaType = intent.getStringExtra("MEDIA_TYPE")
        mediaUrl = intent.getStringExtra("MEDIA_URL") ?: ""
        mediaId = intent.getStringExtra("MEDIA_ID") ?: ""
        postId = intent.getStringExtra("POST_ID") ?: ""
        initialPostId = postId
        mediaThumbnailUrl = intent.getStringExtra("THUMBNAIL_URL") ?: ""
        val from = intent.getStringExtra("FROM") ?: ""
        isLikedByMe = intent.getBooleanExtra("LIKED_BY_ME",false)
        likeCount = intent.getIntExtra("LIKE_COUNT",0)
        commentCount = intent.getIntExtra("COMMENT_COUNT",0)
        
        // Get location for feed fetching
        currentLat = intent.getDoubleExtra("LAT", DEFAULT_LAT)
        currentLng = intent.getDoubleExtra("LNG", DEFAULT_LNG)

        println("Afsafaskhkjasdk  IN_VIDEO_IMAGE_VIEWER $isLikedByMe")

        if (from == "CHAT"){
            binding.downloadNow.visibility = View.VISIBLE
        }

        binding.downloadNow.setOnClickListener {

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val fileName = "THM $mediaType $currentDate"


            val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDownloadMedia(this))
            bottomSheet.onYesClicked = {

                if(mediaType == VIDEO){
                    mediaDownloadManager.downloadM3U8Video(fileName, mediaUrl)
                }else{
                    mediaDownloadManager.downloadFileFromUrl(fileName, mediaUrl)
                }

            }
            bottomSheet.onNoClicked = {

            }
            bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")

//            mediaDownloadManager.downloadFileFromUrl(fileName, mediaUrl)
        }


        updateLikeBtn(isLikedByMe, binding.likeIv)

        binding.likeTv.text = likeCount.toString()
        binding.commentTv.text = commentCount.toString()

        if (postId.isNotEmpty()){
            binding.postBtnLayout.visibility = View.VISIBLE
            binding.userInfoLayout.visibility = View.VISIBLE
            // Fetch post data to populate user info
            fetchPostData()
        }else{
            binding.postBtnLayout.visibility = View.GONE
            binding.userInfoLayout.visibility = View.GONE
        }


//        binding.likeBtn.setOnClickListener {
//            likeCount += 1
//            MediaActionCallback.onMediaAction?.invoke(isLikedByMe, likeCount, commentCount)
//        }
        binding.likeBtn.setOnClickListener {
            likePost(postId)
            isLikedByMe = !isLikedByMe
            binding.likeIv.setImageResource(if (isLikedByMe) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon_white)
            likeCount = if (isLikedByMe) likeCount + 1 else likeCount - 1
            binding.likeTv.text = likeCount.toString()
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
                    commentCount += 1
                    binding.commentTv.text = commentCount.toString()
                }
            }
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }


        // Share button click
        binding.shareBtn.setOnClickListener {
            val normalizedType = mediaType?.lowercase(Locale.getDefault()) ?: ""
            val shareThumbnail = when (normalizedType) {
                Constants.VIDEO -> mediaThumbnailUrl
                else -> mediaThumbnailUrl.ifBlank { mediaUrl }
            }

            if (mediaUrl.isNotBlank() && normalizedType.isNotBlank()) {
                SharePostBottomSheetFragment.newInstance(
                    postId = postId,
                    ownerUserId = ownerUserId,
                    mediaType = normalizedType,
                    mediaUrl = mediaUrl,
                    thumbnailUrl = shareThumbnail,
                    mediaId = mediaId
                ).show(supportFragmentManager, SharePostBottomSheetFragment::class.java.simpleName)
            } else if (postId.isNotBlank() && ownerUserId.isNotBlank()) {
                sharePostWithDeepLink(postId, ownerUserId)
            } else {
                Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        }

        // Bookmark button click
        binding.bookmarkBtn.setOnClickListener {
            savePost(postId)
            isPostSaved = !isPostSaved
            updateBookmarkBtn(isPostSaved, binding.bookmarkIv)
        }

        // Menu button click
        binding.menuBtn.setOnClickListener {
            showMenuOptions()
        }

        // Unfollow button click
        binding.unfollowBtn.setOnClickListener {
            if (postOwnerUserId.isNotEmpty()) {
                unfollowUser(postOwnerUserId)
            }
        }

        // View all comments click
        binding.viewAllCommentsTv.setOnClickListener {
            openCommentsBottomSheet()
        }

        // Profile picture click - navigate to profile
        binding.userProfilePic.setOnClickListener {
            if (postOwnerUserId.isNotEmpty()) {
                val intent = android.content.Intent(this, com.thehotelmedia.android.activity.BusinessProfileDetailsActivity::class.java)
                intent.putExtra("USER_ID", postOwnerUserId)
                startActivity(intent)
            }
        }

        // Username click - navigate to profile
        binding.userNameTv.setOnClickListener {
            if (postOwnerUserId.isNotEmpty()) {
                val intent = android.content.Intent(this, com.thehotelmedia.android.activity.BusinessProfileDetailsActivity::class.java)
                intent.putExtra("USER_ID", postOwnerUserId)
                startActivity(intent)
            }
        }



        // Set up ViewPager2 for vertical scrolling
        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.viewPager.isUserInputEnabled = true
        
        // Prevent overscroll (scrolling before first or after last item)
        binding.viewPager.overScrollMode = View.OVER_SCROLL_NEVER
        
        // Fetch feed data and filter video posts
        fetchFeedAndSetupVideos()

        // Observe post data
        individualViewModal.getSinglePostsResult.observe(this) { result ->
            if (result.status == true) {
                val data = result.data
                data?.let {
                    populateUserInfo(it)
                }
            }
        }

        // Observe follow/unfollow results
        individualViewModal.followUserResult.observe(this) { result ->
            if (result.status == true) {
                isFollowedByMe = true
                binding.unfollowBtn.visibility = View.VISIBLE
            }
        }

        individualViewModal.unFollowUserResult.observe(this) { result ->
            if (result.status == true) {
                isFollowedByMe = false
                binding.unfollowBtn.visibility = View.GONE
            }
        }

        // Observe save post result
        individualViewModal.savePostResult.observe(this) { result ->
            if (result.status == true) {
                // Bookmark state is already updated in the click handler
                // This observer can be used for additional UI updates if needed
            }
        }

    }

    private fun onMediaTypeChanged(mediaType: String) {
        if (mediaType == Constants.IMAGE){
            binding.rotateBtn.visibility = View.GONE
        }else{
            binding.rotateBtn.visibility = View.VISIBLE
        }
    }

    private fun onControllerVisible(controllerVisible: Boolean) {
        // Interaction buttons are now always visible, only show/hide title layout
        if (controllerVisible){
            if (postId.isNotEmpty()){
                binding.titleLayout.visibility = View.VISIBLE
            }
        }else{
            if (postId.isNotEmpty()){
                binding.titleLayout.visibility = View.GONE
            }
        }
    }

    private fun fetchPostData() {
        if (postId.isNotEmpty()) {
            individualViewModal.getSinglePosts(postId)
        }
    }

    private fun populateUserInfo(data: com.thehotelmedia.android.modals.feeds.feed.Data) {
        val postedBy = data.postedBy
        val accountType = postedBy?.accountType
        postOwnerUserId = postedBy?.Id ?: ""
        
        var name = ""
        var profilePic = ""

        if (accountType == "individual") {
            name = postedBy?.name ?: ""
            profilePic = postedBy?.profilePic?.large ?: ""
        } else {
            val businessProfile = postedBy?.businessProfileRef
            name = businessProfile?.name ?: ""
            profilePic = businessProfile?.profilePic?.large ?: ""
        }

        // Set user info
        binding.userNameTv.text = name
        Glide.with(this)
            .load(profilePic)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .into(binding.userProfilePic)

        // Set time ago
        val createdAt = data.createdAt ?: ""
        if (createdAt.isNotEmpty()) {
            binding.timeAgoTv.text = getTimeAgo(createdAt)
        }

        // Set follow status
        isFollowedByMe = postedBy?.isFollowedByMe ?: false
        binding.unfollowBtn.visibility = if (isFollowedByMe) View.VISIBLE else View.GONE

        // Set saved status
        isPostSaved = data.savedByMe ?: false
        updateBookmarkBtn(isPostSaved, binding.bookmarkIv)

        // Set view all comments
        val comments = data.comments ?: 0
        if (comments > 0) {
            binding.viewAllCommentsTv.text = "View all $comments comments"
            binding.viewAllCommentsTv.visibility = View.VISIBLE
        } else {
            binding.viewAllCommentsTv.visibility = View.GONE
        }
        
        // Set post content/caption
        val content = data.content?.trim() ?: ""
        if (content.isNotEmpty()) {
            binding.postContentTv.text = content
            binding.postContentTv.visibility = View.VISIBLE
        } else {
            binding.postContentTv.visibility = View.GONE
        }
    }

    private fun updateBookmarkBtn(isSaved: Boolean, bookmarkIv: ImageView) {
        if (isSaved) {
            bookmarkIv.setImageResource(R.drawable.ic_save_icon_white)
        } else {
            bookmarkIv.setImageResource(R.drawable.ic_unsave_icon_white)
        }
    }

    private fun savePost(id: String) {
        individualViewModal.savePost(id)
    }

    private fun unfollowUser(userId: String) {
        individualViewModal.unFollowUser(userId)
    }

    private fun showMenuOptions() {
        // Show menu options (report, etc.)
        val menuOptions = TwoVerticalOptionBottomSheetFragment.newInstance(
            title = "",
            blockButtonText = "Report",
            viewProfileButtonText = "View Profile"
        ).apply {
            onBlockClick = {
                // Handle report
                dismiss()
            }
            onViewProfileClick = {
                // Navigate to profile
                dismiss()
                if (postOwnerUserId.isNotEmpty()) {
                    val intent = android.content.Intent(this@VideoImageViewer, com.thehotelmedia.android.activity.BusinessProfileDetailsActivity::class.java)
                    intent.putExtra("USER_ID", postOwnerUserId)
                    startActivity(intent)
                }
            }
        }
        menuOptions.show(supportFragmentManager, "MenuOptions")
    }

    private fun openCommentsBottomSheet() {
        val bottomSheetFragment = CommentsBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("POST_ID", postId)
                putInt("COMMENTS_COUNT", commentCount)
            }
        }
        bottomSheetFragment.onCommentSent = { comment ->
            if (comment.isNotEmpty()) {
                commentCount += 1
                binding.commentTv.text = commentCount.toString()
                if (commentCount > 0) {
                    binding.viewAllCommentsTv.text = "View all $commentCount comments"
                    binding.viewAllCommentsTv.visibility = View.VISIBLE
                }
            }
        }
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    private fun fetchFeedAndSetupVideos() {
        // First, set up initial video with current data (only if it's a video)
        // This is temporary - will be replaced when feed data loads
        if (mediaType == VIDEO) {
            mediaList = listOf(MediaItems(MediaType.VIDEO, mediaUrl, mediaId, postId, mediaThumbnailUrl))
        } else {
            mediaList = listOf(MediaItems(MediaType.IMAGE, mediaUrl, mediaId, postId, mediaThumbnailUrl))
        }

        adapter = VideoImageViewerAdapter(this, mediaList,exoPlayer!!,individualViewModal,::onControllerVisible,::onMediaTypeChanged, showControls = false, ::onVideoChanged)
        binding.viewPager.adapter = adapter

        // Initialize previous position to -1 to indicate initial load (not a scroll)
        // This prevents loader from showing on initial video click
        previousVideoPosition = -1
        binding.loadingIndicator.visibility = View.GONE
        
        // Set up page change listener for video playback management
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val mediaListSize = adapter.getMediaList().size
                
                // Always hide loader when page selection is complete
                binding.loadingIndicator.visibility = View.GONE
                
                if (position >= 0 && position < mediaListSize) {
                    adapter.setCurrentPosition(position)
                    onVideoChanged(position)
                    
                    // Update previous position AFTER processing to track scroll direction
                    // This is used in onPageScrolled to determine if we're scrolling down
                    previousVideoPosition = position
                } else if (position < 0) {
                    // Prevent scrolling before first item
                    binding.viewPager.post {
                        binding.viewPager.setCurrentItem(0, false)
                    }
                } else if (position >= mediaListSize && mediaListSize > 0) {
                    // Prevent scrolling after last item
                    binding.viewPager.post {
                        binding.viewPager.setCurrentItem(mediaListSize - 1, false)
                    }
                }
            }
            
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                val mediaListSize = adapter.getMediaList().size
                
                // Only show loader when:
                // 1. Scrolling DOWN (position > previousVideoPosition OR positionOffset > 0)
                // 2. Near bottom (position >= mediaListSize - 2)
                // 3. Actually scrolling (positionOffset > 0)
                // 4. Not on initial load (previousVideoPosition >= 0)
                val isScrollingDown = position > previousVideoPosition || (position == previousVideoPosition && positionOffset > 0)
                val isNearBottom = position >= mediaListSize - 2 && mediaListSize > 0
                val isActuallyScrolling = positionOffset > 0
                val isNotInitialLoad = previousVideoPosition >= 0
                
                if (isScrollingDown && isNearBottom && isActuallyScrolling && isNotInitialLoad) {
                    binding.loadingIndicator.visibility = View.VISIBLE
                } else {
                    binding.loadingIndicator.visibility = View.GONE
                }
            }
        })
        
        // Start playing initial video if it's a video
        if (mediaType == VIDEO && mediaList.isNotEmpty()) {
            adapter.setCurrentPosition(0)
        }

        // Fetch feed data and filter video posts
        lifecycleScope.launch {
            fetchFeedDataDirectly()
        }
    }
    
    private suspend fun fetchFeedDataDirectly() = withContext(Dispatchers.IO) {
        val allPosts = mutableListOf<Data>()
        var pageNumber = 1
        var totalPages: Int? = null
        var hasMorePages = true
        
        // Fetch all pages of feed data until we reach the total pages
        while (hasMorePages) {
            try {
                val response = individualRepo.getFeed(pageNumber, 20, currentLat, currentLng)
                if (response.isSuccessful) {
                    val feedModal = response.body()
                    val feedData = feedModal?.data ?: emptyList()
                    
                    // Get totalPages from first response if not set
                    if (totalPages == null) {
                        totalPages = feedModal?.totalPages
                    }
                    
                    if (feedData.isEmpty()) {
                        hasMorePages = false
                    } else {
                        allPosts.addAll(feedData)
                        
                        // Check if we've reached the last page
                        if (totalPages != null && totalPages > 0 && pageNumber >= totalPages) {
                            hasMorePages = false
                        } else {
                            pageNumber++
                        }
                    }
                } else {
                    hasMorePages = false
                }
            } catch (e: Exception) {
                hasMorePages = false
            }
        }
        
        withContext(Dispatchers.Main) {
            // Only process if we're viewing a video
            if (mediaType == VIDEO) {
                // Filter to get only video posts
                videoPostsList.clear()
                videoPostsList.addAll(allPosts.filter { post ->
                    post.mediaRef.any { media ->
                        media.mediaType?.lowercase() == "video" || 
                        media.mimeType?.lowercase()?.startsWith("video") == true
                    }
                })
                
                // Find initial video position in the filtered list
                currentVideoPosition = videoPostsList.indexOfFirst { it.Id == initialPostId }
                
                if (currentVideoPosition == -1 && initialPostId.isNotEmpty()) {
                    // If initial post not found in filtered list, try to find it in all posts
                    val initialPost = allPosts.find { it.Id == initialPostId }
                    initialPost?.let {
                        // Check if it has video
                        val hasVideo = it.mediaRef.any { media ->
                            media.mediaType?.lowercase() == "video" || 
                            media.mimeType?.lowercase()?.startsWith("video") == true
                        }
                        
                        if (hasVideo) {
                            // Find where this post should be in the feed order
                            // Get all posts before this one in the original feed
                            val initialPostIndexInFeed = allPosts.indexOfFirst { it.Id == initialPostId }
                            
                            if (initialPostIndexInFeed >= 0) {
                                // Filter posts that come before this one and have videos
                                val postsBefore = allPosts.subList(0, initialPostIndexInFeed).filter { post ->
                                    post.mediaRef.any { media ->
                                        media.mediaType?.lowercase() == "video" || 
                                        media.mimeType?.lowercase()?.startsWith("video") == true
                                    }
                                }
                                
                                // Find the correct position: after all videos that come before it
                                val correctPosition = postsBefore.size
                                
                                // Check if it's already in the list at a different position
                                val existingIndex = videoPostsList.indexOfFirst { existing -> existing.Id == it.Id }
                                if (existingIndex == -1) {
                                    // Not in list, insert at correct position
                                    if (correctPosition <= videoPostsList.size) {
                                        videoPostsList.add(correctPosition, it)
                                        currentVideoPosition = correctPosition
                                    } else {
                                        // If position is beyond list, add at end
                                        videoPostsList.add(it)
                                        currentVideoPosition = videoPostsList.size - 1
                                    }
                                } else {
                                    // Already in list, use existing position
                                    currentVideoPosition = existingIndex
                                }
                            } else {
                                // Post not found in feed, add at beginning (new post)
                                if (videoPostsList.none { existing -> existing.Id == it.Id }) {
                                    videoPostsList.add(0, it)
                                    currentVideoPosition = 0
                                }
                            }
                        } else {
                            // Post doesn't have video but we're viewing a video
                            // This shouldn't happen, but handle it by adding at beginning
                            if (videoPostsList.none { existing -> existing.Id == it.Id }) {
                                videoPostsList.add(0, it)
                                currentVideoPosition = 0
                            }
                        }
                    }
                }
                
                // If still no position found, default to 0 (but this shouldn't happen)
                if (currentVideoPosition == -1) {
                    if (videoPostsList.isNotEmpty()) {
                        currentVideoPosition = 0
                    }
                }
                
                // Update adapter with video posts (only if we have videos from feed)
                if (videoPostsList.isNotEmpty()) {
                    updateAdapterWithVideoPosts()
                    
                    // Set initial position - ensure it's valid
                    if (currentVideoPosition >= 0 && currentVideoPosition < videoPostsList.size) {
                        // Use postMessage to ensure this runs after adapter is updated
                        binding.viewPager.post {
                            binding.viewPager.setCurrentItem(currentVideoPosition, false)
                            adapter.setCurrentPosition(currentVideoPosition)
                            // Trigger video change callback to update UI
                            onVideoChanged(currentVideoPosition)
                        }
                    } else if (currentVideoPosition == -1 && videoPostsList.isNotEmpty()) {
                        // Fallback: if position not found, use 0
                        binding.viewPager.post {
                            binding.viewPager.setCurrentItem(0, false)
                            adapter.setCurrentPosition(0)
                            onVideoChanged(0)
                        }
                    }
                } else {
                    // No videos from feed, keep the initial video
                    // Ensure we're at position 0 and can't scroll
                    binding.viewPager.post {
                        binding.viewPager.setCurrentItem(0, false)
                        adapter.setCurrentPosition(0)
                    }
                }
            }
        }
    }

    private fun updateAdapterWithVideoPosts() {
        // Convert video posts to MediaItems with postId
        val newMediaList = videoPostsList.mapNotNull { post ->
            val videoMedia = post.mediaRef.firstOrNull { media ->
                media.mediaType?.lowercase() == "video" || 
                media.mimeType?.lowercase()?.startsWith("video") == true
            }
            videoMedia?.let {
                MediaItems(
                    MediaType.VIDEO, 
                    it.sourceUrl ?: "", 
                    it.Id ?: "",
                    post.Id ?: "",
                    it.thumbnailUrl ?: ""
                )
            }
        }
        
        // Update adapter
        if (newMediaList.isNotEmpty()) {
            adapter.updateMediaList(newMediaList)
        }
    }

    private fun onVideoChanged(position: Int) {
        // Get the media item at this position to find the correct postId
        if (position >= 0 && position < adapter.getMediaList().size) {
            val mediaItem = adapter.getMediaList()[position]
            val currentPostId = mediaItem.postId
            
            // Find the post in videoPostsList using postId (more reliable than position)
            val post = videoPostsList.find { it.Id == currentPostId }
            
            if (post != null) {
                postId = post.Id ?: ""
                currentVideoPosition = videoPostsList.indexOf(post)
                
                // Update UI with new post data
                populateUserInfo(post)
                
                // Update like/comment counts
                isLikedByMe = post.likedByMe ?: false
                likeCount = post.likes ?: 0
                commentCount = post.comments ?: 0
                
                binding.likeTv.text = likeCount.toString()
                binding.commentTv.text = commentCount.toString()
                updateLikeBtn(isLikedByMe, binding.likeIv)
                
                // Update bookmark state
                isPostSaved = post.savedByMe ?: false
                updateBookmarkBtn(isPostSaved, binding.bookmarkIv)
                
                // Fetch full post details for complete info
                if (postId.isNotEmpty()) {
                    individualViewModal.getSinglePosts(postId)
                }
            } else if (position < videoPostsList.size) {
                // Fallback to position-based lookup if postId not found
                val post = videoPostsList[position]
                postId = post.Id ?: ""
                currentVideoPosition = position
                
                populateUserInfo(post)
                isLikedByMe = post.likedByMe ?: false
                likeCount = post.likes ?: 0
                commentCount = post.comments ?: 0
                
                binding.likeTv.text = likeCount.toString()
                binding.commentTv.text = commentCount.toString()
                updateLikeBtn(isLikedByMe, binding.likeIv)
                
                isPostSaved = post.savedByMe ?: false
                updateBookmarkBtn(isPostSaved, binding.bookmarkIv)
                
                if (postId.isNotEmpty()) {
                    individualViewModal.getSinglePosts(postId)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause playback to stop audio, but don't stop() to keep media loaded for when user returns
        exoPlayer?.let {
            wasPlayingBeforePause = it.isPlaying
            it.playWhenReady = false
            it.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume playback if it was playing before pause
        exoPlayer?.let {
            if (wasPlayingBeforePause && mediaType == VIDEO) {
                it.playWhenReady = true
                it.play()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop and release player completely
        exoPlayer?.let {
            it.playWhenReady = false
            it.pause()
            it.stop()
            it.release()
        }
        exoPlayer = null
    }

    private fun updateLikeBtn(postLiked: Boolean, likeIv: ImageView) {
        if (postLiked) {
            likeIv.setImageResource(R.drawable.ic_like_icon)
        } else {
            likeIv.setImageResource(R.drawable.ic_unlike_icon_white)
        }
    }

    private fun likePost(id: String) {
        individualViewModal.likePost(id)
    }


}