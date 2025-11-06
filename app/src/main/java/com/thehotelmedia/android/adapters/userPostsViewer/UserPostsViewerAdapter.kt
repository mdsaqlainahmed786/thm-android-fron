package com.thehotelmedia.android.adapters.userPostsViewer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.MediaItems
import com.thehotelmedia.android.adapters.MediaType
import com.thehotelmedia.android.adapters.VideoImageViewerAdapter
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.databinding.ItemUserPostViewerBinding
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class UserPostsViewerAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val fragmentManager: FragmentManager,
    private val ownerUserId: String,
    private val onPostScrolled: (Int, ExoPlayer?) -> Unit,
    private val onLikeUpdated: (String, Boolean, Int) -> Unit,
    private val onCommentUpdated: (String, Int) -> Unit
) : PagingDataAdapter<Data, UserPostsViewerAdapter.PostViewHolder>(POST_DIFF_CALLBACK) {

    private val exoPlayers = mutableMapOf<String, ExoPlayer>()
    private val postStates = mutableMapOf<String, PostState>()

    data class PostState(
        var isLiked: Boolean = false,
        var likeCount: Int = 0,
        var commentCount: Int = 0
    )

    inner class PostViewHolder(
        val binding: ItemUserPostViewerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentExoPlayer: ExoPlayer? = null
        private var postId: String = ""
        private var mediaAdapter: VideoImageViewerAdapter? = null

        fun bind(post: Data) {
            postId = post.Id ?: ""
            val mediaList = post.mediaRef

            // Initialize or get post state
            val state = postStates.getOrPut(postId) {
                PostState(
                    isLiked = post.likedByMe ?: false,
                    likeCount = post.likes ?: 0,
                    commentCount = post.comments ?: 0
                )
            }

            // Update UI with current state
            updateLikeButton(state.isLiked, state.likeCount)
            binding.commentTv.text = formatCount(state.commentCount)

            // Setup media
            setupMedia(post, mediaList)

            // Setup interaction buttons
            setupLikeButton(postId, state)
            setupCommentButton(postId, state)
            setupShareButton(postId, post.userID ?: "")
        }

        private fun setupMedia(post: Data, mediaList: List<com.thehotelmedia.android.modals.feeds.feed.MediaRef>) {
            if (mediaList.isEmpty()) return

            // Get or create ExoPlayer for this post
            val exoPlayer = exoPlayers.getOrPut(postId) {
                ExoPlayer.Builder(context).build().apply {
                    prepare()
                }
            }

            currentExoPlayer = exoPlayer

            // Convert mediaRef to MediaItems
            val mediaItems = mediaList.map { mediaRef ->
                val mediaType = when (mediaRef.mediaType?.lowercase()) {
                    "image" -> MediaType.IMAGE
                    "video" -> MediaType.VIDEO
                    else -> MediaType.IMAGE
                }
                MediaItems(mediaType, mediaRef.sourceUrl ?: "", mediaRef.Id ?: "")
            }

            // Create adapter for ViewPager2
            mediaAdapter = VideoImageViewerAdapter(
                context,
                mediaItems,
                exoPlayer,
                mediaList.firstOrNull()?.Id ?: "",
                postId,
                individualViewModal,
                { controllerVisible ->
                    // Handle controller visibility if needed
                },
                { mediaType ->
                    // Handle media type change if needed
                }
            )

            binding.mediaViewPager.adapter = mediaAdapter
            binding.mediaViewPager.isUserInputEnabled = false // Disable swipe in ViewPager, we'll scroll via RecyclerView
        }

        private fun setupLikeButton(postId: String, state: PostState) {
            binding.likeBtn.setOnClickListener {
                individualViewModal.likePost(postId)
                state.isLiked = !state.isLiked
                state.likeCount = if (state.isLiked) state.likeCount + 1 else state.likeCount - 1
                updateLikeButton(state.isLiked, state.likeCount)
                onLikeUpdated(postId, state.isLiked, state.likeCount)
            }
        }

        private fun setupCommentButton(postId: String, state: PostState) {
            binding.commentBtn.setOnClickListener {
                val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                    arguments = Bundle().apply {
                        putString("POST_ID", postId)
                        putInt("COMMENTS_COUNT", state.commentCount)
                    }
                }
                bottomSheetFragment.onCommentSent = { comment ->
                    if (comment.isNotEmpty()) {
                        state.commentCount++
                        binding.commentTv.text = formatCount(state.commentCount)
                        onCommentUpdated(postId, state.commentCount)
                    }
                }
                bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
            }
        }

        private fun setupShareButton(postId: String, userId: String) {
            binding.shareBtn.setOnClickListener {
                context.sharePostWithDeepLink(postId, userId)
            }
        }

        private fun updateLikeButton(isLiked: Boolean, likeCount: Int) {
            binding.likeIv.setImageResource(
                if (isLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon_white
            )
            binding.likeTv.text = formatCount(likeCount)
        }

        fun onViewAttachedToWindow() {
            // Play video when view becomes visible
            currentExoPlayer?.play()
            bindingAdapterPosition.let { position ->
                if (position != RecyclerView.NO_POSITION) {
                    onPostScrolled(position, currentExoPlayer)
                }
            }
        }

        fun onViewDetachedFromWindow() {
            // Pause video when view becomes invisible
            currentExoPlayer?.pause()
        }

        fun release() {
            // Release ExoPlayer when view is recycled
            currentExoPlayer?.pause()
            currentExoPlayer = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemUserPostViewerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val item = getItem(position)
        item?.let { holder.bind(it) }
    }

    override fun onViewAttachedToWindow(holder: PostViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.release()
    }

    fun updateLikeState(postId: String, isLiked: Boolean, likeCount: Int) {
        postStates[postId]?.let { state ->
            state.isLiked = isLiked
            state.likeCount = likeCount
            // Find position and notify change
            var position = -1
            for (i in 0 until itemCount) {
                if (getItem(i)?.Id == postId) {
                    position = i
                    break
                }
            }
            if (position >= 0) {
                notifyItemChanged(position)
            }
        }
    }

    fun updateCommentCount(postId: String, commentCount: Int) {
        postStates[postId]?.let { state ->
            state.commentCount = commentCount
            // Find position and notify change
            var position = -1
            for (i in 0 until itemCount) {
                if (getItem(i)?.Id == postId) {
                    position = i
                    break
                }
            }
            if (position >= 0) {
                notifyItemChanged(position)
            }
        }
    }

    /**
     * Public method to get post at a specific position
     * Wraps the protected getItem method
     */
    fun getPostAt(position: Int): Data? {
        return try {
            getItem(position)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find the position of a post by its ID
     */
    fun findPostPosition(postId: String): Int {
        for (i in 0 until itemCount) {
            val item = getItem(i)
            if (item?.Id == postId) {
                return i
            }
        }
        return -1
    }

    companion object {
        private val POST_DIFF_CALLBACK = object : DiffUtil.ItemCallback<Data>() {
            override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
                return oldItem.Id == newItem.Id
            }

            override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Release all ExoPlayers
        exoPlayers.values.forEach { it.release() }
        exoPlayers.clear()
        postStates.clear()
    }
}

