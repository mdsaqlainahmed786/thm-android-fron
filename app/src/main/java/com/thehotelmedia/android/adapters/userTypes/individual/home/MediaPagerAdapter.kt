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
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.VideoImageViewer
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.customClasses.Constants.VIDEO
import com.thehotelmedia.android.databinding.ItemMediaBinding
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
    private val onLikeClicked: (isLikedByMe: Boolean, likeCount: Int, commentCount: Int) -> Unit
) : RecyclerView.Adapter<MediaPagerAdapter.ViewHolder>() {


    private var currentPlayingPosition = -1
    private val viewHolders = mutableListOf<ViewHolder>()

    inner class ViewHolder(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        
        // Store references to adapter's state for this ViewHolder
        // These will be updated in bind() and when adapter state changes
        private var currentIsPostLiked = isPostLiked
        private var currentLikeCount = likeCount
        
        fun updateState(newIsPostLiked: Boolean, newLikeCount: Int) {
            android.util.Log.d("MediaPagerAdapter", "ViewHolder.updateState() called: newIsPostLiked=$newIsPostLiked, newLikeCount=$newLikeCount (old: currentIsPostLiked=$currentIsPostLiked, currentLikeCount=$currentLikeCount)")
            currentIsPostLiked = newIsPostLiked
            currentLikeCount = newLikeCount
            android.util.Log.d("MediaPagerAdapter", "ViewHolder state updated: currentIsPostLiked=$currentIsPostLiked, currentLikeCount=$currentLikeCount")
        }

        private fun isVideo(mediaItem: MediaRef): Boolean {
            val type = mediaItem.mediaType?.lowercase()
            val mime = mediaItem.mimeType?.lowercase()
            return type == VIDEO.lowercase() || (mime?.startsWith("video") == true)
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
                binding.playerView.player = null
                // Clear any existing listeners to prevent memory leaks
                binding.videoLayout.setOnTouchListener(null)
                binding.playerView.setOnTouchListener(null)
                binding.playerView.setOnLongClickListener(null)
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
                VideoPlayerManager.releasePlayer()
                currentPlayingPosition = position
            }

            if (isVideo(mediaItem)) {
                val player = VideoPlayerManager.initializePlayer(context, mediaItem.sourceUrl)
                setupVideoPlayer(mediaItem, player, postId)
            } else {
                setupImageView(mediaItem, postId)
            }
        }

        private fun setupVideoPlayer(
            mediaItem: MediaRef,
            player: ExoPlayer,
            postId: String
        ) {
            val sourceUrl = mediaItem.sourceUrl
            if (sourceUrl.isNullOrEmpty()) return
            val id = mediaItem.Id ?: ""

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
            
            // Check if player is already ready, otherwise wait for it
            if (player.playbackState == com.google.android.exoplayer2.Player.STATE_READY) {
                // Player is already ready, show video immediately
                binding.imageView.visibility = View.GONE
                binding.videoLayout.visibility = View.VISIBLE
                binding.muteIcon.visibility = View.VISIBLE
                player.playWhenReady = true
            } else {
                // Wait for player to be ready before showing video view
                val listener = object : com.google.android.exoplayer2.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == com.google.android.exoplayer2.Player.STATE_READY) {
                            // Player is ready, switch from thumbnail to video view
                            binding.imageView.visibility = View.GONE
                            binding.videoLayout.visibility = View.VISIBLE
                            binding.muteIcon.visibility = View.VISIBLE
                            player.playWhenReady = true // Start playing
                            // Remove listener to avoid memory leaks
                            player.removeListener(this)
                        }
                    }
                }
                player.addListener(listener)
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

                    val intent = Intent(context, VideoImageViewer::class.java).apply {
                        putExtra("MEDIA_URL", sourceUrl)
                        putExtra("MEDIA_TYPE", VIDEO)
                        putExtra("MEDIA_ID", id)
                        putExtra("POST_ID", postId)
                        putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl)
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
            
            // Set up gesture detector on the video layout (parent container)
            // This ensures gestures are detected even if playerView intercepts some touches
            binding.videoLayout.setOnTouchListener { v, event ->
                val handled = gestureDetector.onTouchEvent(event)
                if (!handled && event.action == MotionEvent.ACTION_UP) {
                    // Fallback: behave the same as a confirmed single tap and open viewer.
                    player.pause()

                    val intent = Intent(context, VideoImageViewer::class.java).apply {
                        putExtra("MEDIA_URL", sourceUrl)
                        putExtra("MEDIA_TYPE", VIDEO)
                        putExtra("MEDIA_ID", id)
                        putExtra("POST_ID", postId)
                        putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl)
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

            Glide.with(context).load(sourceUrl).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.imageView)

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
                    // Single tap - open viewer
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
        // Do NOT release the global player here; the active post / adapter is
        // responsible for stopping playback when focus moves. Releasing from a
        // recycled ViewHolder can kill playback for the currently active post.
        holder.itemView.findViewById<com.google.android.exoplayer2.ui.PlayerView?>(R.id.playerView)?.player = null
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        VideoPlayerManager.releasePlayer()
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
