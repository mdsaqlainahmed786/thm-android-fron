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
                    
                    // Remove old listener if exists
                    (binding.playerView.tag as? Player.Listener)?.let { 
                        exoPlayer.removeListener(it) 
                    }
                    
                    val listener = object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            // Hide thumbnail when video starts rendering
                            if (position == currentPlayingPosition) {
                                binding.videoThumbnail.visibility = View.GONE
                            }
                        }
                        
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY && position == currentPlayingPosition) {
                                // Ensure thumbnail is hidden when ready
                                binding.videoThumbnail.visibility = View.GONE
                                if (!exoPlayer.isPlaying && exoPlayer.playWhenReady) {
                                    exoPlayer.play()
                                }
                            }
                        }
                        
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (position == currentPlayingPosition) {
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
                    
                    // Only add listener if this is the current position
                    if (position == currentPlayingPosition || (currentPlayingPosition == -1 && position == 0)) {
                        exoPlayer.addListener(listener)
                        binding.playerView.tag = listener
                    }

                    // Only attach player and play if this is the current position
                    if (position == currentPlayingPosition || (currentPlayingPosition == -1 && position == 0)) {
                        // This is the current playing position - attach player and play
                        binding.playerView.player = exoPlayer
                        exoPlayer.clearMediaItems()
                        exoPlayer.volume = 1f
                        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                        val media = MediaItem.fromUri(mediaItem.uri)
                        exoPlayer.setMediaItem(media)
                        exoPlayer.prepare()
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
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
        
        currentPlayingPosition = position
        
        // Pause and stop previous video
        if (previousPosition != -1 && previousPosition < mediaList.size) {
            exoPlayer.pause()
            exoPlayer.stop()
            // Notify previous item to update (detach player, show thumbnail)
            notifyItemChanged(previousPosition)
        }
        
        // Notify video change callback - this should update user info
        onVideoChanged?.invoke(position)
        
        // Play current video - notify item changed to attach player and play
        if (position >= 0 && position < mediaList.size) {
            val mediaItem = mediaList[position]
            if (mediaItem.type == MediaType.VIDEO) {
                // Notify item changed to trigger bind() which will attach player and play
                notifyItemChanged(position)
            } else {
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount() = mediaList.size

}
