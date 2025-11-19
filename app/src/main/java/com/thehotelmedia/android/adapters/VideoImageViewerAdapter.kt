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

data class MediaItems(val type: MediaType, val uri: String, val id: String)

enum class MediaType {
    IMAGE, VIDEO
}

class VideoImageViewerAdapter(
    private val context: Context,
    private val mediaList: List<MediaItems>,
    private val exoPlayer: ExoPlayer,
    private val mediaId: String,
    private val postId: String,
    private val individualViewModal: IndividualViewModal,
    private val controllerVisible: (Boolean) -> Unit,
    private val onMediaTypeChanged: (String) -> Unit
) : RecyclerView.Adapter<VideoImageViewerAdapter.MediaViewHolder>() {


    inner class MediaViewHolder(private val binding: VideoImageViewerItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItems) {
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
                    if (mediaId.isNotEmpty() && postId.isNotEmpty()){
                        individualViewModal.viewMedia(postId, mediaId)
                    }
                    onMediaTypeChanged(Constants.VIDEO)

                    binding.imageView.visibility = View.GONE
                    binding.videoThumbnail.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(mediaItem.uri)
                        .placeholder(R.drawable.ic_post_placeholder)
                        .into(binding.videoThumbnail)
                    binding.videoLayout.visibility = View.VISIBLE
                    binding.playPauseOverlay.visibility = View.GONE

                    binding.playerView.player = exoPlayer
                    binding.playerView.useController = true
                    binding.playerView.controllerShowTimeoutMs = 3000
                    binding.playerView.controllerAutoShow = true
                    controllerVisible(true)

                    exoPlayer.clearMediaItems()
                    exoPlayer.volume = 1f
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    val media = MediaItem.fromUri(mediaItem.uri)
                    exoPlayer.setMediaItem(media)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = false

                    (binding.playerView.tag as? Player.Listener)?.let { exoPlayer.removeListener(it) }
                    val listener = object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            binding.videoThumbnail.visibility = View.GONE
                        }
                    }
                    exoPlayer.addListener(listener)
                    binding.playerView.tag = listener
                    
                    // Remove custom click listener - let the controller handle play/pause
                    binding.playerView.setOnClickListener(null)
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
        holder.bind(mediaList[position])
    }

    override fun getItemCount() = mediaList.size

}
