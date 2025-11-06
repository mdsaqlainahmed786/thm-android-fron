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

class UserPostsViewerActivity : DarkBaseActivity() {

    private lateinit var binding: ActivityUserPostsViewerBinding
    private lateinit var adapter: UserPostsViewerAdapter
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager: PreferenceManager
    private var ownerUserId = ""
    private var userId = ""
    private var initialPostId: String? = null
    private var filterMediaType: String? = null // "image" or "video" to filter posts
    private var currentExoPlayer: ExoPlayer? = null

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
        filterMediaType = intent.getStringExtra("FILTER_MEDIA_TYPE")

        binding.backBtn.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = UserPostsViewerAdapter(
            this,
            individualViewModal,
            supportFragmentManager,
            ownerUserId,
            ::onPostScrolled,
            ::onLikeUpdated,
            ::onCommentUpdated
        )

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.postsRecyclerView.layoutManager = layoutManager
        binding.postsRecyclerView.adapter = adapter.withLoadStateFooter(footer = LoaderAdapter())

        // Use PagerSnapHelper for Reel-like snap scrolling
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.postsRecyclerView)

        // Enable nested scrolling for smooth vertical scrolling
        binding.postsRecyclerView.isNestedScrollingEnabled = true
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
                    
                    // Scroll to initial post after data is loaded
                    initialPostId?.let { postId ->
                        scrollToPost(postId)
                    }
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
                        return@launch
                    }
                }
                kotlinx.coroutines.delay(100)
                attempts++
            }
        }
    }

    private fun onPostScrolled(position: Int, exoPlayer: ExoPlayer?) {
        // Pause previous player
        currentExoPlayer?.pause()
        // Set current player
        currentExoPlayer = exoPlayer
        // Play current player
        exoPlayer?.play()
    }

    private fun onLikeUpdated(postId: String, isLiked: Boolean, likeCount: Int) {
        // Update like state in adapter
        adapter.updateLikeState(postId, isLiked, likeCount)
    }

    private fun onCommentUpdated(postId: String, commentCount: Int) {
        // Update comment count in adapter
        adapter.updateCommentCount(postId, commentCount)
    }

    override fun onPause() {
        super.onPause()
        currentExoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        currentExoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentExoPlayer?.release()
        currentExoPlayer = null
    }
}

