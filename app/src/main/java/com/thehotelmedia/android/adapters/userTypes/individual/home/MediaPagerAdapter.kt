package com.thehotelmedia.android.adapters.userTypes.individual.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.VideoImageViewer
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.customClasses.Constants.VIDEO
import com.thehotelmedia.android.databinding.ItemMediaBinding
import com.thehotelmedia.android.extensions.moveToFeedPostsViewer
import com.thehotelmedia.android.extensions.moveToUserPostsViewer
import com.thehotelmedia.android.fragments.VideoPlayerManager
import com.thehotelmedia.android.modals.feeds.feed.MediaRef
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

// Singleton to hold the callback
object MediaActionCallback {
    var onMediaAction: ((likedByMe: Boolean, likeCount: Int, commentCount: Int) -> Unit)? = null
}

object GlobalMuteState {
    var isMuted: Boolean = true
}


class MediaPagerAdapter(
    private val context: Context,
    private val mediaList: ArrayList<MediaRef>,
    private val isActive: Boolean,
    val postId: String, // Made public to check if adapter is for same post
    private var isPostLiked: Boolean,
    private var likeCount: Int,
    private val commentCount: Int,
    private val individualViewModal: IndividualViewModal? = null,
    private val onLikeClicked: (isLikedByMe: Boolean, likeCount: Int, commentCount: Int) -> Unit,
    private val isScrollingDown: Boolean = true, // Track scroll direction for buffering control
    private val postOwnerId: String? = null,
    private val openPostViewerOnTap: Boolean = false,
    private val feedPostJson: String? = null,
    private val feedLat: Double = DEFAULT_LAT,
    private val feedLng: Double = DEFAULT_LNG
) : RecyclerView.Adapter<MediaPagerAdapter.ViewHolder>() {


    private var currentPlayingPosition = -1
    private val viewHolders = mutableListOf<ViewHolder>()

    inner class ViewHolder(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        
        // Store references to adapter's state for this ViewHolder
        // These will be updated in bind() and when adapter state changes
        private var currentIsPostLiked = isPostLiked
        private var currentLikeCount = likeCount
        private var playerListener: com.google.android.exoplayer2.Player.Listener? = null
        private var playerRetryCount = 0
        private var lastBoundPlaybackUrl: String? = null
        
        fun updateState(newIsPostLiked: Boolean, newLikeCount: Int) {
            android.util.Log.d("MediaPagerAdapter", "ViewHolder.updateState() called: newIsPostLiked=$newIsPostLiked, newLikeCount=$newLikeCount (old: currentIsPostLiked=$currentIsPostLiked, currentLikeCount=$currentLikeCount)")
            currentIsPostLiked = newIsPostLiked
            currentLikeCount = newLikeCount
            android.util.Log.d("MediaPagerAdapter", "ViewHolder state updated: currentIsPostLiked=$currentIsPostLiked, currentLikeCount=$currentLikeCount")
        }
        
        fun cleanup() {
            // Clean up player listener to prevent memory leaks
            val player = binding.playerView.player
            if (player != null && playerListener != null) {
                player.removeListener(playerListener!!)
                playerListener = null
            }
            binding.bufferingProgress.visibility = View.GONE
        }

        private fun isVideo(mediaItem: MediaRef): Boolean {
            val type = mediaItem.mediaType?.lowercase()
            val mime = mediaItem.mimeType?.lowercase()
            val sourceUrl = mediaItem.sourceUrl?.lowercase() ?: ""
            val duration = mediaItem.duration
            
            // Check multiple indicators that this is a video:
            // 1. mediaType is "video"
            // 2. mimeType starts with "video"
            // 3. sourceUrl has video file extension
            // 4. duration is set (videos have duration, images don't)
            val isVideoByType = type == VIDEO.lowercase() || type == "Video" || type == "VIDEO"
            val isVideoByMime = mime?.startsWith("video") == true
            val isVideoByExtension = sourceUrl.endsWith(".mp4") || sourceUrl.endsWith(".mov") || 
                                     sourceUrl.endsWith(".avi") || sourceUrl.endsWith(".mkv") ||
                                     sourceUrl.endsWith(".3gp") || sourceUrl.endsWith(".webm") ||
                                     sourceUrl.contains(".m3u8") // HLS stream
            val isVideoByDuration = duration != null && duration > 0
            
            val result = isVideoByType || isVideoByMime || isVideoByExtension || isVideoByDuration
            
            // Log for debugging
            if (result) {
                android.util.Log.d("MediaPagerAdapter", "Video detected: type=$type, mime=$mime, hasDuration=$isVideoByDuration, hasVideoExt=$isVideoByExtension, sourceUrl=${mediaItem.sourceUrl?.take(50)}")
            } else {
                android.util.Log.d("MediaPagerAdapter", "Not a video: type=$type, mime=$mime, duration=$duration, sourceUrl=${mediaItem.sourceUrl?.take(50)}")
            }
            
            return result
        }

        fun bind(mediaItem: MediaRef, position: Int) {
            // Update local state from adapter state
            // This ensures state is synced when ViewHolder is rebound
            currentIsPostLiked = isPostLiked
            currentLikeCount = likeCount
            android.util.Log.d("MediaPagerAdapter", "bind() called: syncing state from adapter - isPostLiked=$isPostLiked, likeCount=$likeCount")
            
            if (!isActive) {
                // This ViewPager belongs to a non‑active post. We must NOT release the
                // global player here because it may be currently used by the truly
                // active post in another adapter instance (which causes the black screen).
                // Instead, just detach any player from this ViewHolder and show the
                // appropriate thumbnail.
                val currentPlayer = binding.playerView.player
                if (currentPlayer != null && playerListener != null) {
                    currentPlayer.removeListener(playerListener!!)
                    playerListener = null
                }
                binding.playerView.player = null
                // Clear any existing listeners to prevent memory leaks
                binding.videoLayout.setOnTouchListener(null)
                binding.playerView.setOnTouchListener(null)
                binding.playerView.setOnLongClickListener(null)
                binding.bufferingProgress.visibility = View.GONE
                currentPlayingPosition = -1

                if (isVideo(mediaItem)) {
                    // Show video thumbnail and allow tapping to open full‑screen viewer
                    setupVideoThumbnail(mediaItem, postId)
                } else {
                    setupImageView(mediaItem, postId)
                }
                return
            }

            // If the current position changes, release the previous player
            if (currentPlayingPosition != position) {
                // Don't fully release during fast scrolling/page changes; it can race with bind() and
                // leave some videos stuck. We'll reuse the shared player and just pause; the next
                // initializePlayer() call will stop/clear and prepare the new media item.
                VideoPlayerManager.pausePlayer()
                currentPlayingPosition = position
            }

            if (isVideo(mediaItem)) {
                // Validate sourceUrl before initializing player
                // For videos, prefer sourceUrl, but fallback to thumbnailUrl if sourceUrl is missing
                val playbackUrl = (mediaItem.sourceUrl?.takeIf { it.isNotBlank() }
                    ?: mediaItem.thumbnailUrl?.takeIf { it.isNotBlank() })
                
                if (playbackUrl.isNullOrEmpty()) {
                    android.util.Log.e("MediaPagerAdapter", "Video mediaItem has both empty sourceUrl and thumbnailUrl, showing placeholder")
                    setupVideoThumbnail(mediaItem, postId)
                } else {
                    // Validate URL format
                    val isValidUrl = playbackUrl.startsWith("http://") || playbackUrl.startsWith("https://") ||
                                    playbackUrl.startsWith("file://") || playbackUrl.startsWith("content://")
                    
                    if (!isValidUrl) {
                        android.util.Log.e("MediaPagerAdapter", "Video playbackUrl has invalid format: ${playbackUrl.take(50)}")
                        setupVideoThumbnail(mediaItem, postId)
                    } else {
                        try {
                            android.util.Log.d("MediaPagerAdapter", "Initializing video player with URL: ${playbackUrl.take(80)}")
                            val player = VideoPlayerManager.initializePlayer(context, playbackUrl)
                            // Reset retry state when we switch to a new URL.
                            if (lastBoundPlaybackUrl != playbackUrl) {
                                lastBoundPlaybackUrl = playbackUrl
                                playerRetryCount = 0
                            }
                            setupVideoPlayer(mediaItem, player, postId, playbackUrl)
                        } catch (e: Exception) {
                            android.util.Log.e("MediaPagerAdapter", "Error initializing video player: ${e.message}", e)
                            // Fallback to thumbnail if player initialization fails
                            setupVideoThumbnail(mediaItem, postId)
                        }
                    }
                }
            } else {
                setupImageView(mediaItem, postId)
            }
        }

        private fun setupVideoPlayer(
            mediaItem: MediaRef,
            player: ExoPlayer,
            postId: String,
            playbackUrl: String
        ) {
            val sourceUrl = playbackUrl
            val id = mediaItem.Id ?: ""

            // Hide buffering progress initially
            binding.bufferingProgress.visibility = View.GONE

            // Show thumbnail first while player prepares
            binding.videoLayout.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.muteIcon.visibility = View.GONE
            
            // Load thumbnail
            Glide.with(context)
                .load(mediaItem.thumbnailUrl ?: sourceUrl)
                .placeholder(R.drawable.ic_post_placeholder)
                .error(R.drawable.ic_post_placeholder)
                .into(binding.imageView)

            // Attach player to PlayerView
            binding.playerView.player = player

            // Set the video player to loop
            player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            // Ensure we always attempt playback once prepared.
            player.playWhenReady = true
            
            // Remove any existing listener first to prevent duplicates
            if (playerListener != null) {
                player.removeListener(playerListener!!)
            }
            
            // Create a listener to handle playback state and buffering
            playerListener = object : com.google.android.exoplayer2.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        com.google.android.exoplayer2.Player.STATE_READY -> {
                            // Player is ready, switch from thumbnail to video view
                            // This fixes the issue when scrolling up - video will be shown when ready
                            binding.imageView.visibility = View.GONE
                            binding.videoLayout.visibility = View.VISIBLE
                            binding.muteIcon.visibility = View.VISIBLE
                            binding.bufferingProgress.visibility = View.GONE // Hide buffering when ready
                            player.playWhenReady = true // Start playing
                        }
                        com.google.android.exoplayer2.Player.STATE_BUFFERING -> {
                            // Show buffering indicator only when scrolling down
                            // Don't show when scrolling up or when video layout is not visible
                            if (isScrollingDown && binding.videoLayout.visibility == View.VISIBLE) {
                                binding.bufferingProgress.visibility = View.VISIBLE
                            } else {
                                binding.bufferingProgress.visibility = View.GONE
                            }
                        }
                        com.google.android.exoplayer2.Player.STATE_ENDED, 
                        com.google.android.exoplayer2.Player.STATE_IDLE -> {
                            binding.bufferingProgress.visibility = View.GONE
                        }
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    // Hide buffering when video starts playing
                    if (isPlaying) {
                        binding.bufferingProgress.visibility = View.GONE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    android.util.Log.e(
                        "MediaPagerAdapter",
                        "Player error for postId=$postId url=${sourceUrl.take(120)} code=${error.errorCodeName} msg=${error.message}",
                        error
                    )
                    binding.bufferingProgress.visibility = View.GONE

                    // Self-heal: a lot of transient network / decoder issues recover on retry.
                    // We retry a couple of times before giving up and leaving the thumbnail.
                    if (playerRetryCount < 2) {
                        playerRetryCount++
                        android.util.Log.w(
                            "MediaPagerAdapter",
                            "Retrying playback ($playerRetryCount/2) postId=$postId url=${sourceUrl.take(120)}"
                        )
                        binding.bufferingProgress.visibility = View.VISIBLE
                        try {
                            player.seekTo(0)
                            player.prepare()
                            player.playWhenReady = true
                        } catch (e: Exception) {
                            android.util.Log.e("MediaPagerAdapter", "Retry failed: ${e.message}", e)
                            binding.bufferingProgress.visibility = View.GONE
                        }
                    } else {
                        // Give up: show thumbnail (still tappable to open viewer).
                        binding.videoLayout.visibility = View.GONE
                        binding.imageView.visibility = View.VISIBLE
                        binding.muteIcon.visibility = View.GONE
                    }
                }
            }
            
            // Check if player is already ready
            if (player.playbackState == com.google.android.exoplayer2.Player.STATE_READY) {
                // Player is already ready, show video immediately
                // This is important when scrolling up - if player was already prepared, show video right away
                binding.imageView.visibility = View.GONE
                binding.videoLayout.visibility = View.VISIBLE
                binding.muteIcon.visibility = View.VISIBLE
                binding.bufferingProgress.visibility = View.GONE
                player.playWhenReady = true
                // Still add listener for buffering state changes
                player.addListener(playerListener!!)
            } else {
                // Wait for player to be ready before showing video view
                player.addListener(playerListener!!)
            }


//            // Mute/UnMute Functionality
//            var isMuted = false
//            binding.muteIcon.setImageResource(R.drawable.ic_volume_up) // Set initial icon
//
//            binding.muteIcon.setOnClickListener {
//                isMuted = !isMuted
//                if (isMuted) {
//                    player.volume = 0f // Mute the player
//                    binding.muteIcon.setImageResource(R.drawable.ic_volume_off)
//                } else {
//                    player.volume = 1f // UnMute the player
//                    binding.muteIcon.setImageResource(R.drawable.ic_volume_up)
//                }
//            }

            // Set the mute icon based on global mute state
            updateMuteIconAndState(player)

            // Mute/UnMute Functionality
            binding.muteIcon.setOnClickListener {
                GlobalMuteState.isMuted = !GlobalMuteState.isMuted
                updateMuteIconAndState(player)
            }

            // Setup gesture detector for double-tap on video
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    android.util.Log.d("MediaPagerAdapter", "onDoubleTap DETECTED (video)! postId=$postId, currentIsPostLiked=$currentIsPostLiked, currentLikeCount=$currentLikeCount")
                    showHeartAnimation(binding.videoLayout)
                    
                    // Like the post
                    if (postId.isNotEmpty()) {
                        // Only like if not already liked
                        if (!currentIsPostLiked) {
                            // Get current count and increment
                            val currentCount = currentLikeCount
                            val newLikeCount = currentCount + 1
                            
                            android.util.Log.d("MediaPagerAdapter", "Double-tap (video): currentCount=$currentCount, newLikeCount=$newLikeCount, currentIsPostLiked=$currentIsPostLiked")
                            
                            // Update local state FIRST
                            currentLikeCount = newLikeCount
                            currentIsPostLiked = true
                            
                            // Update adapter state
                            isPostLiked = true
                            likeCount = newLikeCount

                            // CRITICAL: Update UI immediately via callback - MUST be called (even if individualViewModal is null)
                            android.util.Log.d("MediaPagerAdapter", "Calling onLikeClicked (video) with: isLiked=true, likeCount=$newLikeCount")
                            onLikeClicked(true, newLikeCount, commentCount)
                            android.util.Log.d("MediaPagerAdapter", "onLikeClicked callback completed (video)")

                            // Call API only if individualViewModal is available
                            if (individualViewModal != null) {
                                individualViewModal.likePost(postId)
                            } else {
                                android.util.Log.w("MediaPagerAdapter", "individualViewModal is null (video), skipping API call")
                            }
                        } else {
                            android.util.Log.d("MediaPagerAdapter", "Post already liked (video), skipping increment. currentIsPostLiked=$currentIsPostLiked")
                        }
                    } else {
                        android.util.Log.d("MediaPagerAdapter", "postId is empty (video)")
                    }
                    return true // Consume the event
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    // Single tap should ALWAYS open the full‑screen viewer for videos,
                    // regardless of the current play state. This matches the product
                    // requirement that tapping a video opens the viewer.
                    player.pause()

                    // Feed: open reels-style post viewer instead of legacy viewer
                    if (openPostViewerOnTap && postId.isNotBlank()) {
                        if (!feedPostJson.isNullOrBlank()) {
                            context.moveToFeedPostsViewer(feedPostJson, postId, feedLat, feedLng)
                        } else if (!postOwnerId.isNullOrBlank()) {
                            context.moveToUserPostsViewer(postOwnerId, postId)
                        }
                        return true
                    }

                    val intent = Intent(context, VideoImageViewer::class.java).apply {
                        putExtra("MEDIA_URL", sourceUrl)
                        putExtra("MEDIA_TYPE", VIDEO)
                        putExtra("MEDIA_ID", id)
                        putExtra("POST_ID", postId)
                        putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl ?: sourceUrl)
                        putExtra("LIKED_BY_ME", currentIsPostLiked)
                        putExtra("LIKE_COUNT", currentLikeCount)
                        putExtra("COMMENT_COUNT", commentCount)
                        putExtra("LAT", DEFAULT_LAT)
                        putExtra("LNG", DEFAULT_LNG)
                    }

                    MediaActionCallback.onMediaAction = { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                        onLikeClicked(updatedIsLikedByMe, updatedLikeCount, updatedCommentCount)
                    }

                    context.startActivity(intent)
                    return true
                }
            })

            // Make video layout clickable and focusable to receive touch events
            binding.videoLayout.isClickable = true
            binding.videoLayout.isFocusable = true
            // While the player is preparing we show `imageView` (thumbnail). That view must
            // also receive taps; otherwise videos look "frozen" and tapping does nothing.
            binding.imageView.isClickable = true
            binding.imageView.isFocusable = true
            
            // Set up gesture detector on the video layout (parent container)
            // This ensures gestures are detected even if playerView intercepts some touches
            binding.videoLayout.setOnTouchListener { v, event ->
                val handled = gestureDetector.onTouchEvent(event)
                if (!handled && event.action == MotionEvent.ACTION_UP) {
                    // Fallback: behave the same as a confirmed single tap and open viewer.
                    player.pause()

                        // Feed: open reels-style post viewer instead of legacy viewer
                        if (openPostViewerOnTap && postId.isNotBlank()) {
                            if (!feedPostJson.isNullOrBlank()) {
                                context.moveToFeedPostsViewer(feedPostJson, postId, feedLat, feedLng)
                            } else if (!postOwnerId.isNullOrBlank()) {
                                context.moveToUserPostsViewer(postOwnerId, postId)
                            }
                            return@setOnTouchListener true
                        }

                    val intent = Intent(context, VideoImageViewer::class.java).apply {
                        putExtra("MEDIA_URL", sourceUrl)
                        putExtra("MEDIA_TYPE", VIDEO)
                        putExtra("MEDIA_ID", id)
                        putExtra("POST_ID", postId)
                        putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl ?: sourceUrl)
                        putExtra("LIKED_BY_ME", currentIsPostLiked)
                        putExtra("LIKE_COUNT", currentLikeCount)
                        putExtra("COMMENT_COUNT", commentCount)
                        putExtra("LAT", DEFAULT_LAT)
                        putExtra("LNG", DEFAULT_LNG)
                    }

                    MediaActionCallback.onMediaAction = { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                        onLikeClicked(updatedIsLikedByMe, updatedLikeCount, updatedCommentCount)
                    }

                    context.startActivity(intent)
                }
                handled
            }

            // Also bind the same gesture handling to the thumbnail view that is visible
            // until the player reaches STATE_READY.
            binding.imageView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event) || false
            }
            
            // Also set on playerView to catch touches directly on the player
            binding.playerView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event) || false
            }


            binding.playerView.setOnLongClickListener {
                player.pause()
                true // Indicate that the long-click event has been handled
            }

//            binding.videoLayout.setOnClickListener {
//                context.moveToViewer(VIDEO, sourceUrl)
//            }
        }

        private fun updateMuteIconAndState(player: ExoPlayer) {
            // Set mute state globally
            if (GlobalMuteState.isMuted) {
                player.volume = 0f // Mute the player
                binding.muteIcon.setImageResource(R.drawable.ic_volume_off)
            } else {
                player.volume = 1f // Unmute the player
                binding.muteIcon.setImageResource(R.drawable.ic_volume_up)
            }
        }

        private fun setupImageView(mediaItem: MediaRef, postId: String) {
            val sourceUrl = mediaItem.sourceUrl
            val id = mediaItem.Id
            binding.videoLayout.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            // Hide mute icon for pure image items
            binding.muteIcon.visibility = View.GONE
//            binding.imageView.loadImageInBackground(context, sourceUrl.orEmpty(), R.drawable.ic_post_placeholder)

            // Use thumbnailUrl as fallback if sourceUrl is empty
            val imageUrl = if (sourceUrl.isNullOrEmpty()) {
                mediaItem.thumbnailUrl ?: ""
            } else {
                sourceUrl
            }
            
            if (imageUrl.isNotEmpty()) {
                Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.imageView)
            } else {
                // If both sourceUrl and thumbnailUrl are empty, show placeholder
                binding.imageView.setImageResource(R.drawable.ic_post_placeholder)
            }

            // Make imageView clickable and focusable to receive touch events
            binding.imageView.isClickable = true
            binding.imageView.isFocusable = true

            // Setup gesture detector for double-tap
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    android.util.Log.d("MediaPagerAdapter", "onDoubleTap DETECTED! postId=$postId, currentIsPostLiked=$currentIsPostLiked, currentLikeCount=$currentLikeCount")
                    showHeartAnimation(binding.imageView)
                    
                    // Like the post - always show animation, but only increment if not already liked
                    if (postId.isNotEmpty()) {
                        android.util.Log.d("MediaPagerAdapter", "postId is valid: $postId")
                        // Only like if not already liked
                        if (!currentIsPostLiked) {
                            android.util.Log.d("MediaPagerAdapter", "Post is NOT liked, proceeding with like action")
                            // Get current count and increment
                            val currentCount = currentLikeCount
                            val newLikeCount = currentCount + 1
                            
                            android.util.Log.d("MediaPagerAdapter", "Double-tap: currentCount=$currentCount, newLikeCount=$newLikeCount, currentIsPostLiked=$currentIsPostLiked")
                            
                            // Update local state FIRST
                            currentLikeCount = newLikeCount
                            currentIsPostLiked = true
                            
                            // Update adapter state
                            isPostLiked = true
                            likeCount = newLikeCount

                            // CRITICAL: Update UI immediately via callback - MUST be called (even if individualViewModal is null)
                            android.util.Log.d("MediaPagerAdapter", "Calling onLikeClicked with: isLiked=true, likeCount=$newLikeCount")
                            onLikeClicked(true, newLikeCount, commentCount)
                            android.util.Log.d("MediaPagerAdapter", "onLikeClicked callback completed")

                            // Call API only if individualViewModal is available
                            if (individualViewModal != null) {
                                individualViewModal.likePost(postId)
                            } else {
                                android.util.Log.w("MediaPagerAdapter", "individualViewModal is null, skipping API call")
                            }
                        } else {
                            android.util.Log.d("MediaPagerAdapter", "Post already liked, skipping increment. currentIsPostLiked=$currentIsPostLiked")
                        }
                    } else {
                        android.util.Log.d("MediaPagerAdapter", "postId is empty")
                    }
                    return true // Consume the event
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    // Feed: open reels-style post viewer instead of legacy viewer
                    if (openPostViewerOnTap && postId.isNotBlank()) {
                        if (!feedPostJson.isNullOrBlank()) {
                            context.moveToFeedPostsViewer(feedPostJson, postId, feedLat, feedLng)
                        } else if (!postOwnerId.isNullOrBlank()) {
                            context.moveToUserPostsViewer(postOwnerId, postId)
                        }
                        return true
                    }

                    // Single tap - open legacy viewer
                    val intent = Intent(context, VideoImageViewer::class.java).apply {
                        putExtra("MEDIA_URL", sourceUrl)
                        putExtra("MEDIA_TYPE", IMAGE)
                        putExtra("MEDIA_ID", id)
                        putExtra("POST_ID", postId)
                        putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl ?: sourceUrl)
                        putExtra("LIKED_BY_ME", currentIsPostLiked)
                        putExtra("LIKE_COUNT", currentLikeCount)
                        putExtra("COMMENT_COUNT", commentCount)
                        putExtra("LAT", DEFAULT_LAT)
                        putExtra("LNG", DEFAULT_LNG)
                    }

                    MediaActionCallback.onMediaAction = { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                        onLikeClicked(updatedIsLikedByMe, updatedLikeCount, updatedCommentCount)
                    }

                    context.startActivity(intent)
                    return true
                }
            })

            binding.imageView.setOnTouchListener { _, event ->
                val handled = gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_DOWN) {
                    android.util.Log.d("MediaPagerAdapter", "Touch event received on imageView: action=${event.action}")
                }
                handled
            }
        }

        /**
         * Shows a static thumbnail for video when the post is not the active one,
         * but still allows tapping to open the full‑screen video viewer.
         */
        private fun setupVideoThumbnail(mediaItem: MediaRef, postId: String) {
            val sourceUrl = mediaItem.sourceUrl
            val id = mediaItem.Id

            binding.videoLayout.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.muteIcon.visibility = View.GONE

            Glide.with(context)
                .load(mediaItem.thumbnailUrl ?: sourceUrl)
                .placeholder(R.drawable.ic_post_placeholder)
                .error(R.drawable.ic_post_placeholder)
                .into(binding.imageView)

            binding.imageView.isClickable = true
            binding.imageView.isFocusable = true

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    // Feed: open reels-style post viewer instead of legacy viewer
                    if (openPostViewerOnTap && postId.isNotBlank()) {
                        if (!feedPostJson.isNullOrBlank()) {
                            context.moveToFeedPostsViewer(feedPostJson, postId, feedLat, feedLng)
                        } else if (!postOwnerId.isNullOrBlank()) {
                            context.moveToUserPostsViewer(postOwnerId, postId)
                        }
                        return true
                    }

                    // Open full‑screen VIDEO viewer from thumbnail
                    val intent = Intent(context, VideoImageViewer::class.java).apply {
                        putExtra("MEDIA_URL", sourceUrl)
                        putExtra("MEDIA_TYPE", VIDEO)
                        putExtra("MEDIA_ID", id)
                        putExtra("POST_ID", postId)
                        putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl ?: sourceUrl)
                        putExtra("LIKED_BY_ME", currentIsPostLiked)
                        putExtra("LIKE_COUNT", currentLikeCount)
                        putExtra("COMMENT_COUNT", commentCount)
                        putExtra("LAT", DEFAULT_LAT)
                        putExtra("LNG", DEFAULT_LNG)
                    }

                    MediaActionCallback.onMediaAction = { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                        onLikeClicked(updatedIsLikedByMe, updatedLikeCount, updatedCommentCount)
                    }

                    context.startActivity(intent)
                    return true
                }
            })

            binding.imageView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }
        }

        /**
         * Shows a heart animation in the center of the post
         * Simple fade in and fade up animation like YouTube
         * @param container The view container (imageView or videoLayout)
         */
        private fun showHeartAnimation(container: View) {
            val heartView = ImageView(context)
            heartView.setImageResource(R.drawable.ic_like_icon)
            
            // Convert 100dp to pixels for heart size
            val heartSizePx = (100 * context.resources.displayMetrics.density).toInt()
            
            // Enable hardware acceleration for smooth animations
            heartView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // Add to the root FrameLayout (binding.root) - this is the parent of both imageView and videoLayout
            val parent = binding.root as? FrameLayout ?: return
            
            // Create layout params for centering
            val layoutParams = FrameLayout.LayoutParams(heartSizePx, heartSizePx).apply {
                gravity = android.view.Gravity.CENTER
            }
            heartView.layoutParams = layoutParams
            
            parent.addView(heartView)
            
            // Ensure the view is measured and positioned correctly
            heartView.post {
                // Double-check positioning after measurement
                val parentWidth = parent.width
                val parentHeight = parent.height
                if (parentWidth > 0 && parentHeight > 0) {
                    // Ensure heart is centered
                    heartView.x = (parentWidth - heartSizePx) / 2f
                    heartView.y = (parentHeight - heartSizePx) / 2f
                }
            }
            
            // Start simple fade in and fade up animation
            startHeartAnimation(heartView, parent)
        }
        
        /**
         * Creates and starts a simple heart animation: fade in and fade up
         * Similar to YouTube's animation style
         */
        private fun startHeartAnimation(heartView: ImageView, parent: ViewGroup) {
            val density = context.resources.displayMetrics.density
            val floatUpPx = -60f * density // Float up 60dp
            
            // Fade in - 200ms
            val fadeIn = ObjectAnimator.ofFloat(heartView, "alpha", 0f, 1f)
            fadeIn.duration = 200
            fadeIn.interpolator = AccelerateInterpolator()
            
            // Float up and fade out - 600ms, starts immediately (overlaps with fade in)
            val floatUp = ObjectAnimator.ofFloat(heartView, "translationY", 0f, floatUpPx)
            floatUp.duration = 600
            floatUp.interpolator = DecelerateInterpolator()
            
            val fadeOut = ObjectAnimator.ofFloat(heartView, "alpha", 1f, 0f)
            fadeOut.duration = 600
            fadeOut.startDelay = 200 // Start fading out after fade in completes
            fadeOut.interpolator = AccelerateInterpolator()
            
            // Combine animations
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(fadeIn, floatUp, fadeOut)
            
            // Cleanup after animation
            animatorSet.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    parent.removeView(heartView)
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    parent.removeView(heartView)
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            
            animatorSet.start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)
        viewHolders.add(holder)
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Ensure ViewHolder is in the list (in case it was recycled and reused)
        if (!viewHolders.contains(holder)) {
            viewHolders.add(holder)
            android.util.Log.d("MediaPagerAdapter", "Added ViewHolder back to list in onBindViewHolder. Total ViewHolders: ${viewHolders.size}")
        }
        holder.bind(mediaList[position], position)
    }

    override fun getItemCount(): Int = mediaList.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder)
        // Clean up player listener to prevent memory leaks
        holder.cleanup()
        // Do NOT release the global player here; the active post / adapter is
        // responsible for stopping playback when focus moves. Releasing from a
        // recycled ViewHolder can kill playback for the currently active post.
        holder.itemView.findViewById<com.google.android.exoplayer2.ui.PlayerView?>(R.id.playerView)?.player = null
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // ViewPager2's internal RecyclerView can detach/attach during fast scrolling or
        // adapter swaps. Releasing the shared player here causes flaky "video stays still"
        // behaviour because another active post may be trying to use it.
        // We only pause here; the owning screen (e.g. `IndividualHomeFragment.onDestroyView`)
        // is responsible for a full release when leaving the feed.
        VideoPlayerManager.pausePlayer()
    }

    fun updateLikeBtn(postLiked: Boolean, count: Int) {
        // Update adapter state FIRST
        isPostLiked = postLiked
        likeCount = count
        
        android.util.Log.d("MediaPagerAdapter", "updateLikeBtn called: postLiked=$postLiked, count=$count, adapter state updated. ViewHolders count: ${viewHolders.size}")
        
        // Update all ViewHolders' state so double-tap works correctly after like/unlike
        viewHolders.forEach { holder ->
            holder.updateState(postLiked, count)
            android.util.Log.d("MediaPagerAdapter", "Updated ViewHolder state: postLiked=$postLiked, count=$count")
        }
        
        android.util.Log.d("MediaPagerAdapter", "updateLikeBtn completed: updated ${viewHolders.size} ViewHolders")
    }

}
