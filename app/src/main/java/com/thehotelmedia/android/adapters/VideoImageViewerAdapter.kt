package com.thehotelmedia.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.databinding.VideoImageViewerItemMediaBinding
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

data class MediaItems(val type: MediaType, val uri: String, val id: String, val postId: String = "", val thumbnailUrl: String = "")

enum class MediaType {
    IMAGE, VIDEO
}

class VideoImageViewerAdapter(
    private val context: Context,
    private var mediaList: List<MediaItems>,
    private val exoPlayer: ExoPlayer,
    private val individualViewModal: IndividualViewModal,
    private val controllerVisible: (Boolean) -> Unit,
    private val onMediaTypeChanged: (String) -> Unit,
    private val showControls: Boolean = false, // Default to false for reels-like experience
    private val onVideoChanged: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<VideoImageViewerAdapter.MediaViewHolder>() {
    
    private var currentPlayingPosition = -1
    
    // Expose mediaList for external access
    fun getMediaList(): List<MediaItems> = mediaList
    
    fun updateMediaList(newMediaList: List<MediaItems>) {
        mediaList = newMediaList
        notifyDataSetChanged()
    }


    inner class MediaViewHolder(private val binding: VideoImageViewerItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        private fun prepareVideoForPosition(position: Int, mediaItem: MediaItems) {
            // Double-check position is still current and player is attached
            if (position == currentPlayingPosition && binding.playerView.player == exoPlayer) {
                // Stop and clear any existing playback to ensure clean state
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                
                // Reset player settings
                exoPlayer.volume = 1f
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                
                // Set up new media item
                val media = MediaItem.fromUri(mediaItem.uri)
                exoPlayer.setMediaItem(media)
                
                // Prepare and start playback
                exoPlayer.prepare()
                exoPlayer.seekTo(0)
                exoPlayer.playWhenReady = true
            }
        }

        fun bind(mediaItem: MediaItems, position: Int) {
            when (mediaItem.type) {
                MediaType.IMAGE -> {
                    binding.imageView.visibility = View.VISIBLE
                    binding.videoLayout.visibility = View.GONE
                    binding.videoThumbnail.visibility = View.GONE
                    binding.playPauseOverlay.visibility = View.GONE
                    (binding.playerView.tag as? Player.Listener)?.let {
                        exoPlayer.removeListener(it)
                        binding.playerView.tag = null
                    }
                    binding.playerView.setOnClickListener(null)
                    onMediaTypeChanged(Constants.IMAGE)
                    Glide.with(context)
                        .load(mediaItem.uri)
                        .placeholder(R.drawable.ic_post_placeholder)
                        .into(binding.imageView)

                }
                MediaType.VIDEO -> {
                    // Track view for current video
                    if (position == currentPlayingPosition || currentPlayingPosition == -1) {
                        if (mediaItem.id.isNotEmpty() && mediaItem.postId.isNotEmpty()){
                            individualViewModal.viewMedia(mediaItem.postId, mediaItem.id)
                        }
                    }
                    onMediaTypeChanged(Constants.VIDEO)

                    binding.imageView.visibility = View.GONE
                    binding.videoLayout.visibility = View.VISIBLE
                    
                    // Show thumbnail initially
                    binding.videoThumbnail.visibility = View.VISIBLE
                    val thumbnailUrl = if (mediaItem.thumbnailUrl.isNotEmpty()) mediaItem.thumbnailUrl else mediaItem.uri
                    Glide.with(context)
                        .load(thumbnailUrl)
                        .placeholder(R.drawable.ic_post_placeholder)
                        .into(binding.videoThumbnail)
                    
                    binding.playPauseOverlay.visibility = View.GONE

                    // Attach player to this view
                    binding.playerView.player = exoPlayer
                    binding.playerView.useController = showControls
                    if (showControls) {
                        binding.playerView.controllerShowTimeoutMs = 3000
                        binding.playerView.controllerAutoShow = true
                        controllerVisible(true)
                    } else {
                        controllerVisible(false)
                    }

                    // Remove old listener if exists
                    (binding.playerView.tag as? Player.Listener)?.let { 
                        exoPlayer.removeListener(it) 
                    }
                    
                    val listener = object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            // Hide thumbnail when video starts rendering
                            if (position == currentPlayingPosition) {
                                binding.root.post {
                                    binding.videoThumbnail.visibility = View.GONE
                                }
                            }
                        }
                        
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (position == currentPlayingPosition) {
                                when (playbackState) {
                                    Player.STATE_READY -> {
                                        // Video is ready - hide thumbnail and ensure it's playing
                                        binding.root.post {
                                            binding.videoThumbnail.visibility = View.GONE
                                            if (!exoPlayer.isPlaying && exoPlayer.playWhenReady) {
                                                exoPlayer.play()
                                            }
                                        }
                                    }
                                    Player.STATE_BUFFERING -> {
                                        // Keep thumbnail visible while buffering
                                        binding.root.post {
                                            binding.videoThumbnail.visibility = View.VISIBLE
                                        }
                                    }
                                }
                            }
                        }
                        
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (position == currentPlayingPosition) {
                                binding.root.post {
                                    if (isPlaying) {
                                        binding.playPauseOverlay.visibility = View.GONE
                                        binding.videoThumbnail.visibility = View.GONE
                                    } else {
                                        if (!showControls) {
                                            binding.playPauseOverlay.setImageResource(R.drawable.ic_play_circle)
                                            binding.playPauseOverlay.visibility = View.VISIBLE
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Always add listener for current position to handle state changes
                    if (position == currentPlayingPosition || (currentPlayingPosition == -1 && position == 0)) {
                        exoPlayer.addListener(listener)
                        binding.playerView.tag = listener
                    }

                    // Only attach player and play if this is the current position
                    if (position == currentPlayingPosition || (currentPlayingPosition == -1 && position == 0)) {
                        // This is the current playing position - attach player and play
                        // Ensure video layout is visible
                        binding.videoLayout.visibility = View.VISIBLE
                        binding.playerView.visibility = View.VISIBLE
                        
                        // Keep thumbnail visible until video renders (prevents black screen)
                        binding.videoThumbnail.visibility = View.VISIBLE
                        
                        // CRITICAL: For scrolling up, we need to ensure proper player state reset
                        // First, detach player from view to reset any existing state
                        if (binding.playerView.player != null) {
                            binding.playerView.player = null
                        }
                        
                        // Wait a frame to ensure view is ready, then attach player
                        binding.root.post {
                            // Double-check position is still current after post
                            if (position == currentPlayingPosition && binding.playerView.player == null) {
                                // Attach player to view
                                binding.playerView.player = exoPlayer
                                
                                // Wait for view to be attached and laid out before preparing
                                if (binding.playerView.isAttachedToWindow && binding.playerView.width > 0 && binding.playerView.height > 0) {
                                    // View is attached and laid out - prepare immediately
                                    prepareVideoForPosition(position, mediaItem)
                                } else {
                                    // View not ready yet - wait for it
                                    var attachListener: View.OnAttachStateChangeListener? = null
                                    var layoutListener: View.OnLayoutChangeListener? = null
                                    
                                    attachListener = object : View.OnAttachStateChangeListener {
                                        override fun onViewAttachedToWindow(v: View) {
                                            binding.playerView.removeOnAttachStateChangeListener(this)
                                            // Wait for layout as well
                                            if (binding.playerView.width > 0 && binding.playerView.height > 0) {
                                                if (position == currentPlayingPosition && binding.playerView.player == exoPlayer) {
                                                    prepareVideoForPosition(position, mediaItem)
                                                }
                                            } else {
                                                // Wait for layout
                                                layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                                                    binding.playerView.removeOnLayoutChangeListener(layoutListener!!)
                                                    if (position == currentPlayingPosition && binding.playerView.player == exoPlayer) {
                                                        prepareVideoForPosition(position, mediaItem)
                                                    }
                                                }
                                                binding.playerView.addOnLayoutChangeListener(layoutListener!!)
                                            }
                                        }
                                        override fun onViewDetachedFromWindow(v: View) {
                                            binding.playerView.removeOnAttachStateChangeListener(this)
                                            layoutListener?.let { binding.playerView.removeOnLayoutChangeListener(it) }
                                        }
                                    }
                                    binding.playerView.addOnAttachStateChangeListener(attachListener)
                                    
                                    // Fallback: use post with delay if attach listener doesn't fire
                                    binding.root.postDelayed({
                                        if (position == currentPlayingPosition && 
                                            binding.playerView.player == exoPlayer &&
                                            binding.playerView.isAttachedToWindow &&
                                            binding.playerView.width > 0 &&
                                            binding.playerView.height > 0) {
                                            prepareVideoForPosition(position, mediaItem)
                                        }
                                    }, 150)
                                }
                            }
                        }
                        
                        if (currentPlayingPosition == -1) {
                            currentPlayingPosition = position
                        }
                    } else {
                        // Not current position - detach player and show thumbnail
                        // This prevents black screen and ensures proper recycling
                        binding.playerView.player = null
                        binding.videoThumbnail.visibility = View.VISIBLE
                    }
                    
                    // Set click listener based on whether controls are enabled
                    if (showControls) {
                        // Let the controller handle play/pause
                        binding.playerView.setOnClickListener(null)
                    } else {
                        // For reels: tap to play/pause
                        binding.playerView.setOnClickListener {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                binding.playPauseOverlay.setImageResource(R.drawable.ic_play_circle)
                                binding.playPauseOverlay.visibility = View.VISIBLE
                            } else {
                                exoPlayer.play()
                                binding.playPauseOverlay.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }


    }

    fun play() {
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.playWhenReady = false
        exoPlayer.pause()
    }

    fun stopAndReset() {
        exoPlayer.stop()
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = VideoImageViewerItemMediaBinding.inflate(LayoutInflater.from(context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        if (position < mediaList.size) {
            holder.bind(mediaList[position], position)
        }
    }
    
    fun setCurrentPosition(position: Int) {
        val previousPosition = currentPlayingPosition
        
        // Only update if position actually changed
        if (previousPosition == position) {
            return
        }
        
        // Pause and stop previous video, clear media items
        // This ensures clean state before switching to new video
        if (previousPosition != -1 && previousPosition < mediaList.size) {
            exoPlayer.pause()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
        
        // Update current position BEFORE notifying items
        // This ensures bind() sees the correct currentPlayingPosition
        currentPlayingPosition = position
        
        // Notify video change callback - this should update user info
        onVideoChanged?.invoke(position)
        
        // Post notifications to avoid calling during scroll callback (IllegalStateException)
        // Use Handler to post to main looper
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            // Notify previous item to update (detach player, show thumbnail)
            if (previousPosition != -1 && previousPosition < mediaList.size) {
                notifyItemChanged(previousPosition)
            }
            
            // Play current video - notify item changed to attach player and play
            // This must happen after updating currentPlayingPosition so bind() sees the correct value
            if (position >= 0 && position < mediaList.size) {
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount() = mediaList.size

}
