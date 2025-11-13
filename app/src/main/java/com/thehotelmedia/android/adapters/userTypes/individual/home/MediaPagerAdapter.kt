package com.thehotelmedia.android.adapters.userTypes.individual.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.VideoImageViewer
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.customClasses.Constants.VIDEO
import com.thehotelmedia.android.databinding.ItemMediaBinding
import com.thehotelmedia.android.fragments.VideoPlayerManager
import com.thehotelmedia.android.modals.feeds.feed.MediaRef

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
    private val postId: String,
    private var isPostLiked: Boolean,
    private var likeCount: Int,
    private val commentCount: Int,
    private val onLikeClicked: (isLikedByMe: Boolean, likeCount: Int, commentCount: Int) -> Unit
) : RecyclerView.Adapter<MediaPagerAdapter.ViewHolder>() {


    private var currentPlayingPosition = -1

    inner class ViewHolder(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaRef, position: Int) {
            if (!isActive) {


                VideoPlayerManager.releasePlayer() // Ensure no active player if inactive
                currentPlayingPosition = -1
//                setupImageView(mediaItem.sourceUrl, mediaItem.Id, postId) // Default to image handling
                if (mediaItem.mediaType == VIDEO){

                    binding.videoLayout.visibility = View.GONE
                    binding.imageView.visibility = View.VISIBLE

                    Glide.with(context).load(mediaItem.thumbnailUrl).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.imageView)


                }else{
                    setupImageView(mediaItem, postId) // Default to image handling
                }
                return
            }

            // If the current position changes, release the previous player
            if (currentPlayingPosition != position) {
                VideoPlayerManager.releasePlayer()
                currentPlayingPosition = position
            }

            if (mediaItem.mediaType == VIDEO) {
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

            binding.videoLayout.visibility = View.VISIBLE
            binding.imageView.visibility = View.GONE
            binding.playerView.player = player

            // Set the video player to loop
            player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            player.playWhenReady = true // Start playing


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


            binding.playerView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    if (player.isPlaying) {
//                        context.moveToViewer(, sourceUrl,id,postId,isPostLiked)

                        println("Afsafaskhkjasdk  IN_MEDIA_PAGER_ADAPTER $isPostLiked")
                        val intent = Intent(context, VideoImageViewer::class.java).apply {
                            putExtra("MEDIA_URL", sourceUrl)
                            putExtra("MEDIA_TYPE", VIDEO)
                            putExtra("MEDIA_ID", id)
                            putExtra("POST_ID", postId)
                            putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl)
                            putExtra("LIKED_BY_ME", isPostLiked)
                            putExtra("LIKE_COUNT", likeCount)
                            putExtra("COMMENT_COUNT", commentCount)
                        }

                        MediaActionCallback.onMediaAction = { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                            onLikeClicked(updatedIsLikedByMe, updatedLikeCount, updatedCommentCount)
                        }

                        context.startActivity(intent)



                    } else {
                        player.play()
                    }
                }
                false
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
//            binding.imageView.loadImageInBackground(context, sourceUrl.orEmpty(), R.drawable.ic_post_placeholder)

            Glide.with(context).load(sourceUrl).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.imageView)

            binding.imageView.setOnClickListener {
//                context.moveToViewer(IMAGE, sourceUrl.orEmpty(),id,postId,isPostLiked)



                val intent = Intent(context, VideoImageViewer::class.java).apply {
                    putExtra("MEDIA_URL", sourceUrl)
                    putExtra("MEDIA_TYPE", IMAGE)
                    putExtra("MEDIA_ID", id)
                    putExtra("POST_ID", postId)
                    putExtra("THUMBNAIL_URL", mediaItem.thumbnailUrl ?: sourceUrl)
                    putExtra("LIKED_BY_ME", isPostLiked)
                    putExtra("LIKE_COUNT", likeCount)
                    putExtra("COMMENT_COUNT", commentCount)
                }

                MediaActionCallback.onMediaAction = { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                    onLikeClicked(updatedIsLikedByMe, updatedLikeCount, updatedCommentCount)
                }

                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mediaList[position], position)
    }

    override fun getItemCount(): Int = mediaList.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (holder.bindingAdapterPosition == currentPlayingPosition) {
            VideoPlayerManager.releasePlayer()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        VideoPlayerManager.releasePlayer()
    }

    fun updateLikeBtn(postLiked: Boolean, count: Int) {
        isPostLiked = postLiked
        likeCount = count
    }


}
