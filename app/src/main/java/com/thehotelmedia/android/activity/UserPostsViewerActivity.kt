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
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch
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
    private var currentExoPlayer: ExoPlayer? = null
    private var activePosition: Int = RecyclerView.NO_POSITION
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var snapHelper: PagerSnapHelper

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

        binding.backBtn.setOnClickListener {
            finish()
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
            filterMediaType // Pass filterMediaType to adapter
        )

        layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.postsRecyclerView.layoutManager = layoutManager
        binding.postsRecyclerView.adapter = adapter.withLoadStateFooter(footer = LoaderAdapter())

        // Use PagerSnapHelper only for videos/reels (not for photos feed-style)
        if (filterMediaType != "image") {
            snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(binding.postsRecyclerView)
        }

        // Enable nested scrolling for smooth vertical scrolling
        binding.postsRecyclerView.isNestedScrollingEnabled = true

        // Only add snap scroll listener for videos/reels
        if (filterMediaType != "image") {
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
            // When data is loaded, try to scroll to the target media/index
            if (refreshState is LoadState.NotLoading && adapter.itemCount > 0) {
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

    private fun loadPosts() {
        if (userId.isNotEmpty()) {
            individualViewModal.getPostsData(userId).observe(this) { pagingData ->
                lifecycleScope.launch {
                    // Filter posts by media type if specified
                    val filteredPagingData = if (filterMediaType != null) {
                        pagingData.filter { post ->
                            // Only include posts where ALL media match the filter type
                            // This ensures photos-only or videos-only scrolling
                            val mediaRef = post.mediaRef
                            if (mediaRef.isNotEmpty()) {
                                mediaRef.all { media ->
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
                    // This ensures we wait for the filtered data to be ready
                }
            }
        }
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
        currentExoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (activePosition != RecyclerView.NO_POSITION) {
            currentExoPlayer?.play()
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


