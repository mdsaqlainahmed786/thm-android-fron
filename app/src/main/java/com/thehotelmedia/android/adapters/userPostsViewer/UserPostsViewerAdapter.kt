package com.thehotelmedia.android.adapters.userPostsViewer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.thehotelmedia.android.R
import com.thehotelmedia.android.adapters.MediaItems
import com.thehotelmedia.android.adapters.MediaType
import com.thehotelmedia.android.adapters.VideoImageViewerAdapter
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.databinding.ItemUserPostViewerBinding
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.util.Locale

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
        var commentCount: Int = 0,
        var isSaved: Boolean = false,
        var isFollowing: Boolean = false
    )

    inner class PostViewHolder(
        val binding: ItemUserPostViewerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentExoPlayer: ExoPlayer? = null
        private var postId: String = ""
        private var mediaAdapter: VideoImageViewerAdapter? = null
        private var currentMediaIsVideo: Boolean = false
        private var currentPost: Data? = null

        fun bind(post: Data) {
            postId = post.Id ?: ""
            currentPost = post
            val mediaList = post.mediaRef

            val state = postStates.getOrPut(postId) {
                PostState(
                    isLiked = post.likedByMe ?: false,
                    likeCount = post.likes ?: 0,
                    commentCount = post.comments ?: 0,
                    isSaved = post.savedByMe ?: false,
                    isFollowing = when {
                        post.postedBy?.isFollowedByMe == true -> true
                        post.postedBy?.businessProfileRef?.isFollowedByMe == true -> true
                        else -> false
                    }
                )
            }
            state.isLiked = post.likedByMe ?: state.isLiked
            state.likeCount = post.likes ?: state.likeCount
            state.commentCount = post.comments ?: state.commentCount
            state.isSaved = post.savedByMe ?: state.isSaved
            state.isFollowing = when {
                post.postedBy?.isFollowedByMe == true -> true
                post.postedBy?.businessProfileRef?.isFollowedByMe == true -> true
                else -> state.isFollowing
            }
            val firstMediaType = mediaList.firstOrNull()?.mediaType?.lowercase()
            currentMediaIsVideo = firstMediaType == "video"
            updateLayoutForMediaType(currentMediaIsVideo)

            updateLikeButton(state.isLiked, state.likeCount)
            updateCommentsText(state.commentCount)
            updateShareCount(post.shared ?: 0)

            val timestamp = formatTimestamp(post.createdAt)

            bindProfileSection(post, state, timestamp)
            bindInfoSection(state.commentCount, post.shared ?: 0, post, timestamp)

            setupMedia(post, mediaList, state)
            setupLikeButton(postId, state)
            setupCommentButton(postId, state)
            setupShareButton(postId, post.userID ?: "")
            setupSaveButton(postId, state)

            binding.reelViewCommentsTv.setOnClickListener {
                binding.reelCommentBtn.performClick()
            }
            binding.photoViewCommentsTv.setOnClickListener {
                binding.photoCommentBtn.performClick()
            }
        }

        private fun setupMedia(
            post: Data,
            mediaList: List<com.thehotelmedia.android.modals.feeds.feed.MediaRef>,
            state: PostState
        ) {
            if (mediaList.isEmpty()) return

            val exoPlayer = exoPlayers.getOrPut(postId) {
                ExoPlayer.Builder(context).build()
            }

            currentExoPlayer = exoPlayer

            val mediaItems = mediaList.map { mediaRef ->
                val mediaType = when (mediaRef.mediaType?.lowercase()) {
                    "image" -> MediaType.IMAGE
                    "video" -> MediaType.VIDEO
                    else -> MediaType.IMAGE
                }
                MediaItems(mediaType, mediaRef.sourceUrl ?: "", mediaRef.Id ?: "")
            }

            mediaAdapter = VideoImageViewerAdapter(
                context,
                mediaItems,
                exoPlayer,
                mediaList.firstOrNull()?.Id ?: "",
                postId,
                individualViewModal,
                { _ -> },
                { mediaType ->
                    val isVideo = mediaType.equals(Constants.VIDEO, ignoreCase = true)
                    if (currentMediaIsVideo != isVideo) {
                        currentMediaIsVideo = isVideo
                        updateLayoutForMediaType(isVideo)
                        currentPost?.let { bindProfileSection(it, state, formatTimestamp(it.createdAt)) }
                        currentPost?.let {
                            bindInfoSection(
                                state.commentCount,
                                it.shared ?: 0,
                                it,
                                formatTimestamp(it.createdAt)
                            )
                        }
                    }
                }
            )

            binding.mediaViewPager.adapter = mediaAdapter
            binding.mediaViewPager.isUserInputEnabled = false
        }

        private fun setupLikeButton(postId: String, state: PostState) {
            val listener = View.OnClickListener {
                individualViewModal.likePost(postId)
                state.isLiked = !state.isLiked
                state.likeCount = if (state.isLiked) state.likeCount + 1 else state.likeCount - 1
                updateLikeButton(state.isLiked, state.likeCount)
                onLikeUpdated(postId, state.isLiked, state.likeCount)
            }
            binding.reelLikeBtn.setOnClickListener(listener)
            binding.photoLikeBtn.setOnClickListener(listener)
        }

        private fun setupCommentButton(postId: String, state: PostState) {
            val listener = View.OnClickListener {
                val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                    arguments = Bundle().apply {
                        putString("POST_ID", postId)
                        putInt("COMMENTS_COUNT", state.commentCount)
                    }
                }
                bottomSheetFragment.onCommentSent = { comment ->
                    if (comment.isNotEmpty()) {
                        state.commentCount++
                        updateCommentsText(state.commentCount)
                        onCommentUpdated(postId, state.commentCount)
                    }
                }
                bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
            }
            binding.reelCommentBtn.setOnClickListener(listener)
            binding.photoCommentBtn.setOnClickListener(listener)
        }

        private fun setupShareButton(postId: String, userId: String) {
            val listener = View.OnClickListener {
                context.sharePostWithDeepLink(postId, userId)
            }
            binding.reelShareBtn.setOnClickListener(listener)
            binding.photoShareBtn.setOnClickListener(listener)
        }

        private fun setupSaveButton(postId: String, state: PostState) {
            updateSaveButton(state.isSaved)
            val toggleSave: (View) -> Unit = {
                individualViewModal.savePost(postId)
                state.isSaved = !state.isSaved
                updateSaveButton(state.isSaved)
            }
            binding.reelSaveBtn.setOnClickListener(toggleSave)
            binding.reelSaveIv.setOnClickListener(toggleSave)
            binding.photoSaveIv.setOnClickListener(toggleSave)
        }

        private fun updateLikeButton(isLiked: Boolean, likeCount: Int) {
            val likeIcon = if (isLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon_white
            binding.reelLikeIv.setImageResource(likeIcon)
            binding.photoLikeIv.setImageResource(likeIcon)

            val formatted = formatCount(likeCount)
            binding.reelLikeCountTv.text = formatted
            binding.photoLikeTv.text = formatted
            binding.photoLikeCountTv.text = context.getString(R.string.default_like_count, formatted)
        }

        private fun updateSaveButton(isSaved: Boolean) {
            val reelIcon = if (isSaved) R.drawable.ic_save_icon_white else R.drawable.ic_unsave_icon
            val photoIcon = if (isSaved) R.drawable.ic_save_icon_white else R.drawable.ic_unsave_icon

            binding.reelSaveIv.setImageResource(reelIcon)
            binding.photoSaveIv.setImageResource(photoIcon)

            val tint = if (isSaved) null else ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            binding.reelSaveIv.imageTintList = tint
            binding.photoSaveIv.imageTintList = tint
        }

        private fun updateCommentsText(commentCount: Int) {
            val formatted = formatCount(commentCount)
            val commentText = if (commentCount > 0) {
                context.getString(R.string.view_all_comments, formatted)
            } else {
                context.getString(R.string.add_a_comment)
            }
            binding.reelCommentCountTv.text = formatted
            binding.photoCommentTv.text = formatted
            binding.reelViewCommentsTv.text = commentText
            binding.photoViewCommentsTv.text = commentText
        }

        private fun updateShareCount(shareCount: Int) {
            val formatted = formatCount(shareCount)
            binding.reelShareCountTv.text = formatted
            binding.photoShareTv.text = formatted
        }

        private fun bindProfileSection(post: Data, state: PostState, timestamp: String) {
            val postedBy = post.postedBy
            val accountType = postedBy?.accountType
            var displayName = ""
            var profilePic = ""

            if (accountType == "individual") {
                displayName = postedBy?.name.orEmpty()
                profilePic = postedBy?.profilePic?.large.orEmpty()
            } else {
                val businessProfile = postedBy?.businessProfileRef
                displayName = businessProfile?.name.orEmpty()
                profilePic = businessProfile?.profilePic?.large.orEmpty()
            }

            val safeName = if (displayName.isNotBlank()) displayName else context.getString(R.string.app_name)
            binding.reelUsernameTv.text = safeName
            binding.photoUsernameTv.text = safeName

            val subtitleParts = mutableListOf<String>()
            val location = post.location?.placeName.orEmpty()
            if (location.isNotBlank()) {
                subtitleParts.add(location)
            }
            if (timestamp.isNotBlank()) {
                subtitleParts.add(timestamp)
            }

            val subtitleText = subtitleParts.joinToString(" • ")
            if (subtitleText.isNotBlank()) {
                binding.reelSubtitleTv.text = subtitleText
                binding.reelSubtitleTv.visibility = View.VISIBLE
                binding.photoSubtitleTv.text = subtitleText
                binding.photoSubtitleTv.visibility = View.VISIBLE
            } else {
                binding.reelSubtitleTv.visibility = View.GONE
                binding.photoSubtitleTv.visibility = View.GONE
            }

            Glide.with(context)
                .load(profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.reelProfileIv)

            Glide.with(context)
                .load(profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.photoProfileIv)

            setupFollowButton(post, state)
        }

        private fun bindInfoSection(commentCount: Int, shareCount: Int, post: Data, timestamp: String) {
            val caption = post.content.orEmpty().trim()
            if (caption.isNotEmpty()) {
                val username = binding.reelUsernameTv.text
                val captionText = "$username $caption"
                binding.reelCaptionTv.text = captionText
                binding.reelCaptionTv.visibility = View.VISIBLE
                binding.photoCaptionTv.text = captionText
                binding.photoCaptionTv.visibility = View.VISIBLE
            } else {
                binding.reelCaptionTv.visibility = View.GONE
                binding.photoCaptionTv.visibility = View.GONE
            }

            updateCommentsText(commentCount)
            updateShareCount(shareCount)

            if (timestamp.isNotBlank()) {
                val formatted = timestamp.uppercase(Locale.getDefault())
                binding.reelTimestampTv.text = formatted
                binding.reelTimestampTv.visibility = View.VISIBLE
                binding.photoTimestampTv.text = formatted
                binding.photoTimestampTv.visibility = View.VISIBLE
            } else {
                binding.reelTimestampTv.visibility = View.GONE
                binding.photoTimestampTv.visibility = View.GONE
            }
        }

        private fun setupFollowButton(post: Data, state: PostState) {
            val postOwnerId = post.postedBy?.Id ?: post.userID.orEmpty()
            val isSelf = postOwnerId.isBlank() || postOwnerId == ownerUserId
            if (!currentMediaIsVideo || isSelf) {
                binding.reelFollowButton.visibility = View.GONE
                binding.reelFollowButton.setOnClickListener(null)
                return
            }

            binding.reelFollowButton.visibility = View.VISIBLE
            updateFollowButton(state.isFollowing)
            binding.reelFollowButton.setOnClickListener {
                val shouldFollow = !state.isFollowing
                state.isFollowing = shouldFollow
                updateFollowButton(state.isFollowing)
                if (shouldFollow) {
                    individualViewModal.followUser(postOwnerId)
                } else {
                    individualViewModal.unFollowUser(postOwnerId)
                }
            }
        }

        private fun updateFollowButton(isFollowing: Boolean) {
            if (isFollowing) {
                binding.reelFollowButton.text = context.getString(R.string.following)
                binding.reelFollowButton.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.reelFollowButton.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                binding.reelFollowButton.strokeColor =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.reelFollowButton.text = context.getString(R.string.follow)
                binding.reelFollowButton.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.reelFollowButton.backgroundTintList =
                    ColorStateList.valueOf(Color.TRANSPARENT)
                binding.reelFollowButton.strokeColor =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            }
        }

        private fun updateLayoutForMediaType(isVideo: Boolean) {
            currentMediaIsVideo = isVideo
            val reelVisibility = if (isVideo) View.VISIBLE else View.GONE
            val photoVisibility = if (isVideo) View.GONE else View.VISIBLE

            binding.reelTopGradient.visibility = reelVisibility
            binding.reelBottomGradient.visibility = reelVisibility
            binding.reelActionColumn.visibility = reelVisibility
            binding.reelInfoContainer.visibility = reelVisibility

            binding.photoHeaderContainer.visibility = photoVisibility
            binding.photoFooterContainer.visibility = photoVisibility
        }

        private fun formatTimestamp(createdAt: String?): String {
            return createdAt?.let {
                calculateDaysAgo(it, context)
            } ?: ""
        }

        fun onViewAttachedToWindow() {
            currentExoPlayer?.play()
            bindingAdapterPosition.let { position ->
                if (position != RecyclerView.NO_POSITION) {
                    onPostScrolled(position, currentExoPlayer)
                }
            }
        }

        fun onViewDetachedFromWindow() {
            currentExoPlayer?.pause()
        }

        fun release() {
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

