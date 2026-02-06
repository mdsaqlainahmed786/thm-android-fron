package com.thehotelmedia.android.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.filter
import androidx.paging.map
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.userPostsViewer.UserPostsViewerAdapter
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityUserPostsViewerBinding
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.modals.feeds.feed.Address
import com.thehotelmedia.android.modals.feeds.feed.BusinessProfileRef
import com.thehotelmedia.android.modals.feeds.feed.BusinessTypeRef
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.modals.feeds.feed.MediaRef
import com.thehotelmedia.android.modals.feeds.feed.PostedBy
import com.thehotelmedia.android.modals.feeds.feed.ProfilePic
import com.thehotelmedia.android.modals.profileData.image.ImageData
import com.thehotelmedia.android.modals.profileData.profile.BusinessSubtypeRef
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class UserPostsViewerActivity : DarkBaseActivity() {

    private lateinit var binding: ActivityUserPostsViewerBinding
    private lateinit var adapter: UserPostsViewerAdapter
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager: PreferenceManager
    private var ownerUserId = ""
    private var userId = ""
    private var initialPostId: String? = null
    private var initialMediaId: String? = null
    private var initialMediaUrl: String? = null
    private var initialIndex: Int? = null
    private var filterMediaType: String? = null // "image" or "video" to filter posts
    private var forceReelUi: Boolean = false
    private var currentExoPlayer: ExoPlayer? = null
    private var activePosition: Int = RecyclerView.NO_POSITION
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var snapHelper: PagerSnapHelper
    private var imagesLoaded = false // Flag to prevent loading images multiple times

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserPostsViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun initUi() {
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        preferenceManager = PreferenceManager.getInstance(this)
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

        userId = intent.getStringExtra("USER_ID") ?: ""
        initialPostId = intent.getStringExtra("INITIAL_POST_ID")
        initialMediaId = intent.getStringExtra("INITIAL_MEDIA_ID")
        initialMediaUrl = intent.getStringExtra("INITIAL_MEDIA_URL")
        initialIndex = if (intent.hasExtra("INITIAL_INDEX")) intent.getIntExtra("INITIAL_INDEX", -1).takeIf { it >= 0 } else null
        filterMediaType = intent.getStringExtra("FILTER_MEDIA_TYPE")
        // Reels-style UI can be forced via intent, and is also enabled for feed-opened posts.
        // IMPORTANT: Do NOT enable reels UI for profile Photos/Videos viewers; only feed-opened should use it.
        val explicitForce = intent.getBooleanExtra("FORCE_REEL_UI", false)
        forceReelUi = explicitForce || (initialPostId != null && filterMediaType == null)

        // Set title based on media filter
        if (filterMediaType == "video") {
            binding.titleTv.text = getString(R.string.videos)
        } else {
            binding.titleTv.text = getString(R.string.photos)
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        // Reels-style viewer chrome when opened from feed
        if (forceReelUi) {
            binding.titleLayout.visibility = View.GONE
            binding.reelTopOverlay.visibility = View.VISIBLE
            binding.reelBackBtn.setOnClickListener { finish() }
            // Edge-to-edge content like reels
            binding.postsRecyclerView.setPadding(0, 0, 0, 0)
            // Hide legacy global menu overlay (reels uses in-item menu button)
            binding.globalMenuBtnContainer.visibility = View.GONE
            // Dark background
            binding.main.setBackgroundColor(android.graphics.Color.BLACK)
            binding.postsRecyclerView.setBackgroundColor(android.graphics.Color.BLACK)
        } else {
            binding.reelTopOverlay.visibility = View.GONE
        }

        setupRecyclerView()
        loadPosts()
        observeFollowState()
    }

    private fun setupRecyclerView() {
        adapter = UserPostsViewerAdapter(
            this,
            individualViewModal,
            supportFragmentManager,
            ownerUserId,
            ::onPostScrolled,
            ::onLikeUpdated,
            ::onCommentUpdated,
            filterMediaType, // Pass filterMediaType to adapter
            forceReelUi = forceReelUi
        )

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.postsRecyclerView.layoutManager = layoutManager
        binding.postsRecyclerView.adapter = adapter.withLoadStateFooter(
            footer = LoaderAdapter { adapter.retry() }
        )

        // Use PagerSnapHelper for reels-style paging (videos, and images when forceReelUi is enabled)
        if (forceReelUi || filterMediaType != "image") {
            snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.postsRecyclerView)
        }

        // Enable nested scrolling for smooth vertical scrolling
        binding.postsRecyclerView.isNestedScrollingEnabled = true

        // Only add snap scroll listener for reels/paging mode
        if (forceReelUi || filterMediaType != "image") {
            binding.postsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val snapView = snapHelper.findSnapView(layoutManager) ?: return
                        val position = layoutManager.getPosition(snapView)
                        if (position != RecyclerView.NO_POSITION) {
                            updateActivePosition(position)
                        }
                    }
                }
            })
        } else {
            // For feed-style photos, update active position on scroll
            binding.postsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (firstVisible != RecyclerView.NO_POSITION && firstVisible != activePosition) {
                        updateActivePosition(firstVisible)
                    }
                }
            })
        }

        adapter.addLoadStateListener { loadStates ->
            val refreshState = loadStates.refresh
            // When data is loaded, check if we need to load images instead
            if (refreshState is LoadState.NotLoading) {
                if (adapter.itemCount == 0 && filterMediaType == "image" && userId.isNotEmpty() && !imagesLoaded) {
                    // No posts found, try loading images directly
                    imagesLoaded = true
                    loadImagesAsPosts()
                } else if (adapter.itemCount > 0) {
                    // We have posts, try to scroll to the target media/index
                    if (activePosition == RecyclerView.NO_POSITION) {
                        // Trigger scroll if we have media/index to scroll to
                        if (initialPostId != null || initialMediaId != null || initialMediaUrl != null || initialIndex != null) {
                            lifecycleScope.launch {
                                when {
                                    initialPostId != null -> {
                                        scrollToPost(initialPostId!!)
                                    }
                                    initialMediaId != null || initialMediaUrl != null || initialIndex != null -> {
                                        scrollToPostByMedia(initialMediaId, initialMediaUrl, initialIndex)
                                    }
                                }
                            }
                        } else {
                            // No specific target, just show first item
                            binding.postsRecyclerView.post {
                                val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
                                val target = if (firstVisible != RecyclerView.NO_POSITION) firstVisible else 0
                                updateActivePosition(target)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadPosts() {
        if (userId.isNotEmpty()) {
            individualViewModal.getPostsData(userId).observe(this) { pagingData ->
                lifecycleScope.launch {
                    // Filter posts by media type if specified
                    val filteredPagingData = if (filterMediaType != null) {
                        pagingData.filter { post ->
                            // Include posts that have at least one media matching the filter type
                            // This allows posts with mixed media to show up when filtering
                            val mediaRef = post.mediaRef
                            if (mediaRef.isNotEmpty()) {
                                mediaRef.any { media ->
                                    media.mediaType?.lowercase() == filterMediaType?.lowercase()
                                }
                            } else {
                                false // Skip posts with no media
                            }
                        }
                    } else {
                        pagingData
                    }
                    
                    adapter.submitData(filteredPagingData)
                    
                    // Don't scroll here - let the LoadStateListener handle it after data is loaded
                    // The LoadStateListener will check if posts are empty and load images if needed
                }
            }
        }
    }
    
    private fun loadImagesAsPosts() {
        if (userId.isNotEmpty()) {
            individualViewModal.getImages(userId).observe(this) { imagePagingData ->
                lifecycleScope.launch {
                    // Convert ImageData to Data (post format) using map
                    val postsPagingData = imagePagingData.map { imageData ->
                        convertImageToPost(imageData)
                    }
                    
                    adapter.submitData(postsPagingData)
                    
                    // Scroll to the initial image if specified
                    if (initialMediaId != null || initialMediaUrl != null || initialIndex != null) {
                        kotlinx.coroutines.delay(300)
                        scrollToPostByMedia(initialMediaId, initialMediaUrl, initialIndex)
                    }
                }
            }
        }
    }
    
    private fun convertImageToPost(imageData: ImageData): Data {
        // Convert ImageData to MediaRef
        val mediaRef = MediaRef(
            Id = imageData.Id,
            mediaType = imageData.mediaType ?: "image",
            mimeType = imageData.mimeType,
            sourceUrl = imageData.sourceUrl,
            thumbnailUrl = null,
            duration = null
        )
        
        // Get user profile info for PostedBy (use cached value from observeFollowState)
        val userProfile = individualViewModal.userProfileByIdResult.value?.data
        val userBusinessProfileRef = userProfile?.businessProfileRef
        
        // Convert userProfile.ProfilePic to feeds.feed.ProfilePic
        val feedProfilePic = userProfile?.profilePic?.let {
            ProfilePic(
                small = it.small,
                medium = it.medium,
                large = it.large
            )
        }
        
        // Convert userProfile.BusinessProfileRef to feeds.feed.BusinessProfileRef
        val feedBusinessProfileRef = userBusinessProfileRef?.let {
            BusinessProfileRef(
                Id = it.Id,
                profilePic = it.profilePic?.let { pic ->
                    ProfilePic(
                        small = pic.small,
                        medium = pic.medium,
                        large = pic.large
                    )
                },
                name = it.name,
                businessRating = it.rating,
                address = it.address?.let { addr ->
                    Address(
                        street = addr.street,
                        city = addr.city,
                        state = addr.state,
                        zipCode = addr.zipCode,
                        country = addr.country,
                        lat = addr.lat,
                        lng = addr.lng
                    )
                },
                businessTypeRef = it.businessTypeRef?.let { typeRef ->
                    BusinessTypeRef(
                        Id = typeRef.Id,
                        icon = typeRef.icon,
                        name = typeRef.name
                    )
                },
                businessSubtypeRef = it.businessSubtypeRef?.let { subtypeRef ->
                    BusinessSubtypeRef(
                        Id = subtypeRef.Id,
                        name = subtypeRef.name
                    )
                },
                isFollowedByMe = userProfile?.isConnected
            )
        }
        
        val postedBy = PostedBy(
            Id = userId,
            accountType = userProfile?.accountType,
            businessProfileID = userProfile?.businessProfileID,
            name = userProfile?.name,
            profilePic = feedProfilePic,
            businessProfileRef = feedBusinessProfileRef,
            isFollowedByMe = userProfile?.isConnected
        )
        
        // Create a Data (post) object from the image
        // Use a unique ID to avoid conflicts
        // Provide a valid createdAt date (use current time) to avoid parsing errors
        val currentDateTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        
        return Data(
            Id = "img_${imageData.Id}",
            mediaRef = arrayListOf(mediaRef),
            postedBy = postedBy,
            createdAt = currentDateTime,
            likes = 0,
            comments = 0,
            views = 0,
            shared = 0,
            likedByMe = false,
            savedByMe = false,
            content = null,
            userID = userId
        )
    }

    private fun scrollToPost(postId: String) {
        // Wait for adapter to load items, then scroll to the post
        lifecycleScope.launch {
            var attempts = 0
            while (attempts < 50) { // Max 5 seconds
                val itemCount = adapter.itemCount
                if (itemCount > 0) {
                    // Find the position of the post using the adapter's helper method
                    val position = adapter.findPostPosition(postId)
                    if (position >= 0) {
                        binding.postsRecyclerView.scrollToPosition(position)
                        binding.postsRecyclerView.post {
                            updateActivePosition(position)
                        }
                        return@launch
                    }
                }
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
    }

    private fun scrollToPostByMedia(mediaId: String?, mediaUrl: String?, fallbackIndex: Int?) {
        // Wait for adapter to load items, then scroll to the post containing this media
        lifecycleScope.launch {
            var attempts = 0
            var lastItemCount = 0
            var foundPosition = -1
            
            // Try both media matching and index-based scrolling in parallel
            while (attempts < 80) { // Max 8 seconds total
                val itemCount = adapter.itemCount
                if (itemCount > 0) {
                    var position = -1
                    
                    // Strategy 1: Try to find by media ID/URL
                    if (mediaId != null && mediaId.isNotEmpty()) {
                        position = adapter.findPostPositionByMediaId(mediaId)
                    }
                    if (position < 0 && mediaUrl != null && mediaUrl.isNotEmpty()) {
                        position = adapter.findPostPositionByMediaUrl(mediaUrl)
                    }
                    
                    // Strategy 2: If media matching fails and we have an index, use it
                    // This is best-effort since filtering may change the order
                    if (position < 0 && fallbackIndex != null && fallbackIndex >= 0) {
                        // Use the index, clamped to valid range
                        position = fallbackIndex.coerceIn(0, itemCount - 1)
                    }
                    
                    if (position >= 0) {
                        foundPosition = position
                        // Scroll immediately
                        binding.postsRecyclerView.scrollToPosition(position)
                        
                        // Wait for layout, then update active position
                        binding.postsRecyclerView.post {
                            // Give it a moment for the scroll to complete
                            binding.postsRecyclerView.postDelayed({
                                // Double-check we're at the right position
                                val currentFirst = layoutManager.findFirstCompletelyVisibleItemPosition()
                                val targetPos = if (currentFirst != RecyclerView.NO_POSITION && 
                                    kotlin.math.abs(currentFirst - position) <= 2) {
                                    currentFirst
                                } else {
                                    position
                                }
                                updateActivePosition(targetPos)
                            }, 400)
                        }
                        return@launch
                    }
                    
                    // If item count increased, reset attempts (more data loaded)
                    if (itemCount > lastItemCount) {
                        attempts = 0
                        lastItemCount = itemCount
                    }
                }
                kotlinx.coroutines.delay(100)
                attempts++
            }
            
            // Final fallback: use index if available, otherwise show first item
            if (foundPosition < 0) {
                val finalPosition = if (fallbackIndex != null && fallbackIndex >= 0 && adapter.itemCount > 0) {
                    fallbackIndex.coerceIn(0, adapter.itemCount - 1)
                } else {
                    0
                }
                binding.postsRecyclerView.post {
                    binding.postsRecyclerView.scrollToPosition(finalPosition)
                    binding.postsRecyclerView.postDelayed({
                        updateActivePosition(finalPosition)
                    }, 400)
                }
            }
        }
    }

    private fun onPostScrolled(position: Int, exoPlayer: ExoPlayer?) {
        // Pause previous player
        currentExoPlayer?.pause()
        // Set current player
        currentExoPlayer = exoPlayer
        // Play current player
        if (position == activePosition) {
            exoPlayer?.play()
        } else {
            exoPlayer?.pause()
        }
    }

    private fun onLikeUpdated(postId: String, isLiked: Boolean, likeCount: Int) {
        // Update like state in adapter
        adapter.updateLikeState(postId, isLiked, likeCount)
    }

    private fun onCommentUpdated(postId: String, commentCount: Int) {
        // Update comment count in adapter
        adapter.updateCommentCount(postId, commentCount)
    }

    private fun updateActivePosition(position: Int) {
        if (position == activePosition || position < 0) return
        val itemCount = adapter.itemCount
        if (itemCount == 0 || position >= itemCount) return
        val previous = activePosition
        activePosition = position
        adapter.setActivePosition(position)
        if (forceReelUi) {
            val views = adapter.getPostAt(position)?.views ?: 0
            binding.reelViewsTv.text = formatCount(views)
        }
        if (previous != RecyclerView.NO_POSITION && previous != position) {
            val previousHolder = binding.postsRecyclerView.findViewHolderForAdapterPosition(previous)
            when (previousHolder) {
                is UserPostsViewerAdapter.PostViewHolder -> previousHolder.updateActiveState(false)
                is UserPostsViewerAdapter.FeedViewHolder -> previousHolder.updateActiveState(false)
            }
        }
        val currentHolder = binding.postsRecyclerView.findViewHolderForAdapterPosition(position)
        when (currentHolder) {
            is UserPostsViewerAdapter.PostViewHolder -> currentHolder.updateActiveState(true)
            is UserPostsViewerAdapter.FeedViewHolder -> currentHolder.updateActiveState(true)
        }
    }

    private fun observeFollowState() {
        if (userId.isNotEmpty()) {
            individualViewModal.getUserProfileById(userId)
        }

        individualViewModal.userProfileByIdResult.observe(this) { result ->
            if (result.status == true) {
                val data = result.data
                val targetId = data?.Id ?: userId
                val isFollowing = data?.isConnected == true
                val isRequested = data?.isRequested == true
                adapter.setUserFollowState(targetId, isFollowing, isRequested)
            }
        }

        individualViewModal.followUserResult.observe(this) { result ->
            if (result.status == true) {
                val status = result.data?.status?.lowercase(Locale.getDefault())
                val targetId = result.data?.following ?: userId
                val isFollowingStatus = status == "accepted" || status == "connected"
                val isRequestedStatus = status == "pending" || status == "requested" || status == "sent"

                val (isFollowing, isRequested) = when {
                    isFollowingStatus -> true to false
                    isRequestedStatus -> false to true
                    else -> false to true
                }
                adapter.setUserFollowState(targetId, isFollowing, isRequested)
            }
        }

        individualViewModal.unFollowUserResult.observe(this) { result ->
            if (result.status == true) {
                adapter.setUserFollowState(userId, false, false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause all players to stop audio, but don't stop() to keep media loaded for resume
        adapter.pauseAllPlayers()
        currentExoPlayer?.let {
            it.playWhenReady = false
            it.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume playback if we have an active position
        if (activePosition != RecyclerView.NO_POSITION) {
            currentExoPlayer?.let {
                it.playWhenReady = true
                it.play()
            }
        }
    }

    override fun onDestroy() {
        adapter.stopAllPlayers()
        currentExoPlayer?.release()
        currentExoPlayer = null
        super.onDestroy()
    }

    override fun finish() {
        adapter.stopAllPlayers()
        currentExoPlayer?.pause()
        currentExoPlayer = null
        super.finish()
    }
}


