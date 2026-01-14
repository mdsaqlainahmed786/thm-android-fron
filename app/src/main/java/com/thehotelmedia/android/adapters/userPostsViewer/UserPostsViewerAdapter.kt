package com.thehotelmedia.android.adapters.userPostsViewer

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.adapters.MediaItems
import com.thehotelmedia.android.adapters.MediaType
import com.thehotelmedia.android.adapters.VideoImageViewerAdapter
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.SharePostBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ItemUserPostViewerBinding
import com.thehotelmedia.android.databinding.PostItemsLayoutBinding
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaPagerAdapter
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import java.util.Locale

class UserPostsViewerAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val fragmentManager: FragmentManager,
    private val ownerUserId: String,
    private val onPostScrolled: (Int, ExoPlayer?) -> Unit,
    private val onLikeUpdated: (String, Boolean, Int) -> Unit,
    private val onCommentUpdated: (String, Int) -> Unit,
    private val filterMediaType: String? = null // "image" for feed-style, "video" or null for reel-style
) : PagingDataAdapter<Data, RecyclerView.ViewHolder>(POST_DIFF_CALLBACK) {

    private val exoPlayers = mutableMapOf<String, ExoPlayer>()
    private val postStates = mutableMapOf<String, PostState>()
    private val followStateByUser = mutableMapOf<String, FollowState>()
    private var activePosition: Int = RecyclerView.NO_POSITION
    private val preferenceManager = PreferenceManager.getInstance(context)

    data class PostState(
        var isLiked: Boolean = false,
        var likeCount: Int = 0,
        var commentCount: Int = 0,
        var isSaved: Boolean = false,
        var isFollowing: Boolean = false,
        var isRequested: Boolean = false
    )

    private data class FollowState(
        var isFollowing: Boolean,
        var isRequested: Boolean
    )

    inner class PostViewHolder(
        val binding: ItemUserPostViewerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentExoPlayer: ExoPlayer? = null
        var postId: String = ""
            private set
        private var activePlayer: ExoPlayer? = null
        private var mediaAdapter: VideoImageViewerAdapter? = null
        private var currentMediaIsVideo: Boolean = false
        private var currentPost: Data? = null
        private var isHolderActive: Boolean = false
        private var currentOwnerId: String = ""

        fun bind(post: Data, isActive: Boolean) {
            postId = post.Id ?: ""
            currentPost = post
            isHolderActive = isActive

            val mediaList = post.mediaRef
            val postOwnerId = extractOwnerId(post)
            currentOwnerId = postOwnerId

            val cachedFollowState = followStateByUser[postOwnerId]

            val state = postStates.getOrPut(postId) {
                PostState(
                    isLiked = post.likedByMe ?: false,
                    likeCount = post.likes ?: 0,
                    commentCount = post.comments ?: 0,
                    isSaved = post.savedByMe ?: false,
                    isFollowing = cachedFollowState?.isFollowing ?: when {
                        post.postedBy?.isFollowedByMe == true -> true
                        post.postedBy?.businessProfileRef?.isFollowedByMe == true -> true
                        else -> false
                    },
                    isRequested = cachedFollowState?.isRequested ?: false
                )
            }

            cachedFollowState?.let {
                state.isFollowing = it.isFollowing
                state.isRequested = it.isRequested
            }

            if (cachedFollowState == null) {
                this@UserPostsViewerAdapter.applyFollowState(postOwnerId, state.isFollowing, state.isRequested, notify = false)
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
            this@UserPostsViewerAdapter.applyFollowState(postOwnerId, state.isFollowing, state.isRequested, notify = false)

            currentMediaIsVideo = mediaList.firstOrNull()?.mediaType?.equals("video", ignoreCase = true) == true
            updateLayoutForMediaType(currentMediaIsVideo)

            updateLikeButton(state.isLiked, state.likeCount)
            updateCommentsText(state.commentCount)
            updateShareCount(post.shared ?: 0)

            val timestamp = formatTimestamp(post.createdAt)
            bindProfileSection(post, state, timestamp)
            bindInfoSection(state.commentCount, post.shared ?: 0, post, timestamp)

            setupMedia(mediaList, state)
            setupLikeButton(state)
            setupCommentButton(state)
            setupShareButton(post.userID ?: "")
            setupSaveButton(state)
            setupFollowButton(post, state)
            setupMenuButton(post, state)
            setupProfileIconClick(post)

            binding.reelViewCommentsTv.setOnClickListener {
                binding.reelCommentBtn.performClick()
            }
            binding.photoViewCommentsTv.setOnClickListener {
                binding.photoCommentBtn.performClick()
            }

            updateActiveState(isHolderActive)
        }

        private fun setupMedia(
            mediaList: List<com.thehotelmedia.android.modals.feeds.feed.MediaRef>,
            state: PostState
        ) {
            if (mediaList.isEmpty()) return

            val exoPlayer = exoPlayers.getOrPut(postId) {
                ExoPlayer.Builder(context).build()
            }

            currentExoPlayer = exoPlayer

            (binding.mediaViewPager.getChildAt(0)?.findViewById<com.google.android.exoplayer2.ui.PlayerView>(R.id.playerView)?.tag as? Player.Listener)?.let {
                exoPlayer.removeListener(it)
            }

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
                individualViewModal,
                { controllerVisible -> 
                    // Handle controller visibility if needed
                },
                { mediaType ->
                    val isVideo = mediaType.lowercase() == Constants.VIDEO.lowercase()
                    if (currentMediaIsVideo != isVideo) {
                        currentMediaIsVideo = isVideo
                        updateLayoutForMediaType(isVideo)
                        currentPost?.let {
                            val timestamp = formatTimestamp(it.createdAt)
                            bindProfileSection(it, state, timestamp)
                            bindInfoSection(state.commentCount, it.shared ?: 0, it, timestamp)
                            setupFollowButton(it, state)
                            setupMenuButton(it, state)
                        }
                        updateActiveState(isHolderActive)
                    }
                },
                showControls = false // Reels should not have controls
            )

            exoPlayer.volume = 1f
            exoPlayer.playWhenReady = false

            binding.mediaViewPager.adapter = mediaAdapter
            binding.mediaViewPager.isUserInputEnabled = false
        }

        private fun startPlayback() {
            pauseOtherPlayers(postId)
            currentExoPlayer?.let {
                if (it.playbackState == Player.STATE_IDLE) {
                    it.prepare()
                }
                it.playWhenReady = true
                it.volume = 1f
                it.play()
            }
            activePlayer = currentExoPlayer
        }

        private fun stopPlayback() {
            activePlayer?.let {
                it.playWhenReady = false
                it.pause()
                it.seekTo(0)
                it.volume = 0f
            }
            activePlayer = null
        }

        fun updateActiveState(isActive: Boolean) {
            isHolderActive = isActive
            if (currentMediaIsVideo) {
                if (isActive && itemView.isAttachedToWindow) {
                    startPlayback()
                } else {
                    stopPlayback()
                }
            } else if (!isActive) {
                stopPlayback()
            }
        }

        private fun setupLikeButton(state: PostState) {
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

        private fun setupCommentButton(state: PostState) {
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

        private fun setupShareButton(userId: String) {
            val listener = View.OnClickListener {
                if (postId.isNotBlank() && userId.isNotBlank()) {
                    val mediaList = currentPost?.mediaRef
                    val selectedMedia = mediaList?.let { list ->
                        if (list.isNotEmpty()) {
                            val currentIndex = binding.mediaViewPager.currentItem.coerceIn(0, list.size - 1)
                            list.getOrNull(currentIndex)
                        } else null
                    }

                    val mediaType = selectedMedia?.mediaType?.lowercase(Locale.getDefault())
                    SharePostBottomSheetFragment.newInstance(
                        postId = postId,
                        ownerUserId = userId,
                        mediaType = mediaType,
                        mediaUrl = selectedMedia?.sourceUrl,
                        thumbnailUrl = selectedMedia?.thumbnailUrl,
                        mediaId = selectedMedia?.Id
                    ).show(fragmentManager, SharePostBottomSheetFragment::class.java.simpleName)
                } else {
                    context.sharePostWithDeepLink(postId, userId)
                }
            }
            binding.reelShareBtn.setOnClickListener(listener)
            binding.photoShareBtn.setOnClickListener(listener)
        }

        private fun setupSaveButton(state: PostState) {
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
            val icon = if (isLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon_white
            val formatted = formatCount(likeCount)
            binding.reelLikeIv.setImageResource(icon)
            binding.photoLikeIv.setImageResource(icon)
            binding.reelLikeCountTv.text = formatted
            binding.photoLikeTv.text = formatted
            binding.photoLikeCountTv.text = context.getString(R.string.default_like_count, formatted)
            currentPost?.likes = likeCount
            currentPost?.likedByMe = isLiked
        }

        private fun updateSaveButton(isSaved: Boolean) {
            val reelIcon = if (isSaved) R.drawable.ic_save_icon_white else R.drawable.ic_unsave_icon
            val photoIcon = if (isSaved) R.drawable.ic_save_icon_white else R.drawable.ic_unsave_icon
            val tint = if (isSaved) null else ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))

            binding.reelSaveIv.setImageResource(reelIcon)
            binding.reelSaveIv.imageTintList = tint

            binding.photoSaveIv.setImageResource(photoIcon)
            binding.photoSaveIv.imageTintList = tint

            currentPost?.savedByMe = isSaved
        }

        private fun updateCommentsText(commentCount: Int) {
            val formatted = formatCount(commentCount)
            val label = if (commentCount > 0) {
                context.getString(R.string.view_all_comments, formatted)
            } else {
                context.getString(R.string.add_a_comment)
            }
            binding.reelCommentCountTv.text = formatted
            binding.photoCommentTv.text = formatted
            binding.reelViewCommentsTv.text = label
            binding.photoViewCommentsTv.text = label
            currentPost?.comments = commentCount
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
            val postOwnerId = currentOwnerId.ifBlank { extractOwnerId(post) }
            currentOwnerId = postOwnerId
            val isSelf = postOwnerId.isBlank() || postOwnerId == ownerUserId
            if (!currentMediaIsVideo || isSelf) {
                binding.reelFollowButton.visibility = View.GONE
                binding.reelFollowButton.setOnClickListener(null)
                return
            }

            binding.reelFollowButton.visibility = View.VISIBLE
            updateFollowButton(state.isFollowing, state.isRequested)
            binding.reelFollowButton.setOnClickListener {
                if (state.isFollowing) {
                    state.isFollowing = false
                    state.isRequested = false
                    updateFollowButton(state.isFollowing, state.isRequested)
                    applyFollowState(postOwnerId, state.isFollowing, state.isRequested)
                    individualViewModal.unFollowUser(postOwnerId)
                } else {
                    state.isFollowing = false
                    state.isRequested = true
                    updateFollowButton(state.isFollowing, state.isRequested)
                    applyFollowState(postOwnerId, state.isFollowing, state.isRequested)
                    individualViewModal.followUser(postOwnerId)
                }
            }
        }

        private fun setupMenuButton(post: Data, state: PostState) {
            if (currentMediaIsVideo) {
                // For videos: show menu button below save icon
                binding.reelMenuBtn.visibility = View.VISIBLE
                binding.reelMenuBtn.setOnClickListener { view ->
                    this@UserPostsViewerAdapter.showPostMenu(view, post, postId)
                }
                // Hide photo menu button for videos
                binding.photoMenuBtn.visibility = View.GONE
                binding.photoMenuBtn.setOnClickListener(null)
            } else {
                // For photos: show menu button in header for all posts
                binding.photoMenuBtn.visibility = View.VISIBLE
                binding.photoMenuBtn.setOnClickListener { view ->
                    this@UserPostsViewerAdapter.showPostMenu(view, post, postId)
                }
                // Hide reel menu button for photos
                binding.reelMenuBtn.visibility = View.GONE
                binding.reelMenuBtn.setOnClickListener(null)
            }
        }

        private fun setupProfileIconClick(post: Data) {
            val postOwnerId = currentOwnerId.ifBlank { extractOwnerId(post) }
            
            // Set up click listener for reel profile icon (videos)
            binding.reelProfileIv.setOnClickListener {
                if (postOwnerId.isNotEmpty()) {
                    val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
                    intent.putExtra("USER_ID", postOwnerId)
                    context.startActivity(intent)
                }
            }
            
            // Set up click listener for photo profile icon (photos)
            binding.photoProfileIv.setOnClickListener {
                if (postOwnerId.isNotEmpty()) {
                    val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
                    intent.putExtra("USER_ID", postOwnerId)
                    context.startActivity(intent)
                }
            }
            
            // Also set up click listener for username text views
            binding.reelUsernameTv.setOnClickListener {
                if (postOwnerId.isNotEmpty()) {
                    val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
                    intent.putExtra("USER_ID", postOwnerId)
                    context.startActivity(intent)
                }
            }
            
            binding.photoUsernameTv.setOnClickListener {
                if (postOwnerId.isNotEmpty()) {
                    val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
                    intent.putExtra("USER_ID", postOwnerId)
                    context.startActivity(intent)
                }
            }
        }

        private fun updateFollowButton(isFollowing: Boolean, isRequested: Boolean) {
            val button = binding.reelFollowButton
            val white = ContextCompat.getColor(context, R.color.white)
            val black = ContextCompat.getColor(context, R.color.black)
            val outlineWidth = context.resources.getDimensionPixelSize(R.dimen.follow_button_outline_width)
            val cornerRadius = context.resources.getDimensionPixelSize(R.dimen.follow_button_corner_radius)
            val rippleColor = ContextCompat.getColor(context, R.color.white_40)
            val outlineDrawable = ContextCompat.getDrawable(context, R.drawable.bg_follow_outline_dark)
            val filledDrawable = ContextCompat.getDrawable(context, R.drawable.bg_follow_filled_white)

            button.cornerRadius = cornerRadius
            button.rippleColor = ColorStateList.valueOf(rippleColor)
            val horizontalPadding = context.resources.getDimensionPixelSize(R.dimen.follow_button_padding_horizontal)
            val verticalPadding = context.resources.getDimensionPixelSize(R.dimen.follow_button_padding_vertical)
            button.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            button.iconPadding = 0

            button.icon = null

            val transparent = ContextCompat.getColor(context, android.R.color.transparent)

            when {
                isRequested -> {
                    button.text = context.getString(R.string.requested)
                    button.strokeWidth = outlineWidth
                    button.strokeColor = ColorStateList.valueOf(white)
                    button.setTextColor(white)
                    button.backgroundTintList = ColorStateList.valueOf(transparent)
                }
                isFollowing -> {
                    button.text = context.getString(R.string.unfollow)
                    button.strokeWidth = outlineWidth
                    button.strokeColor = ColorStateList.valueOf(white)
                    button.setTextColor(white)
                    button.backgroundTintList = ColorStateList.valueOf(transparent)
                    button.background = filledDrawable
                }
                else -> {
                    button.text = context.getString(R.string.follow)
                    button.strokeWidth = outlineWidth
                    button.strokeColor = ColorStateList.valueOf(white)
                    button.setTextColor(white)
                    button.backgroundTintList = ColorStateList.valueOf(transparent)
                    button.background = outlineDrawable
                }
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
            if (!isVideo) {
                binding.reelFollowButton.visibility = View.GONE
                currentExoPlayer?.pause()
            }

            binding.photoHeaderContainer.visibility = photoVisibility
            binding.photoFooterContainer.visibility = photoVisibility
        }

        private fun formatTimestamp(createdAt: String?): String {
            return createdAt?.let {
                calculateDaysAgo(it, context)
            } ?: ""
        }

        fun onViewAttachedToWindow() {
            updateActiveState(isHolderActive)
            bindingAdapterPosition.let { position ->
                if (position != RecyclerView.NO_POSITION) {
                    onPostScrolled(position, currentExoPlayer)
                }
            }
        }

        fun onViewDetachedFromWindow() {
            stopPlayback()
        }

        fun release() {
            stopPlayback()
            currentExoPlayer = null
            activePlayer = null
        }

        fun refreshLikeState(state: PostState) {
            updateLikeButton(state.isLiked, state.likeCount)
        }

        fun refreshFollowState(post: Data, state: PostState) {
            setupFollowButton(post, state)
        }
    }

    private fun pauseOtherPlayers(exceptPostId: String) {
        exoPlayers.forEach { (id, player) ->
            if (id != exceptPostId) {
                player.playWhenReady = false
                player.pause()
                player.seekTo(0)
                player.volume = 0f
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (filterMediaType == "image") {
            VIEW_TYPE_FEED
        } else {
            VIEW_TYPE_REEL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FEED -> {
                val binding = PostItemsLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FeedViewHolder(binding)
            }
            else -> {
                val binding = ItemUserPostViewerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                PostViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val isActive = position == activePosition
        when (holder) {
            is PostViewHolder -> item?.let { holder.bind(it, isActive) }
            is FeedViewHolder -> item?.let { holder.bind(it, isActive) }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            payloads.forEach { payload ->
                when (payload) {
                    PAYLOAD_ACTIVE -> {
                        val isActive = position == activePosition
                        when (holder) {
                            is PostViewHolder -> holder.updateActiveState(isActive)
                            is FeedViewHolder -> holder.updateActiveState(isActive)
                        }
                    }
                    PAYLOAD_LIKE -> {
                        val postId = getItem(position)?.Id ?: return@forEach
                        postStates[postId]?.let { state ->
                            when (holder) {
                                is PostViewHolder -> holder.refreshLikeState(state)
                                is FeedViewHolder -> holder.refreshLikeState(state)
                            }
                        }
                    }
                    PAYLOAD_FOLLOW_STATE -> {
                        val post = getItem(position) ?: return@forEach
                        postStates[post.Id ?: return@forEach]?.let { state ->
                            when (holder) {
                                is PostViewHolder -> holder.refreshFollowState(post, state)
                                is FeedViewHolder -> { /* Feed doesn't have follow button */ }
                            }
                        }
                    }
                    else -> {
                        super.onBindViewHolder(holder, position, payloads)
                        return
                    }
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setActivePosition(position: Int) {
        if (position == activePosition) return
        val previous = activePosition
        activePosition = position
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous, PAYLOAD_ACTIVE)
        }
        if (activePosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(activePosition, PAYLOAD_ACTIVE)
        }
    }

    inner class FeedViewHolder(
        val binding: PostItemsLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var mediaPagerAdapter: MediaPagerAdapter? = null
        private var dotsIndicator: SpringDotsIndicator? = null
        var postId: String = ""
            private set
        private var currentPost: Data? = null
        private var isHolderActive: Boolean = false

        fun bind(post: Data, isActive: Boolean) {
            postId = post.Id ?: ""
            currentPost = post
            isHolderActive = isActive

            val state = postStates.getOrPut(postId) {
                PostState(
                    isLiked = post.likedByMe ?: false,
                    likeCount = post.likes ?: 0,
                    commentCount = post.comments ?: 0,
                    isSaved = post.savedByMe ?: false
                )
            }

            state.isLiked = post.likedByMe ?: state.isLiked
            state.likeCount = post.likes ?: state.likeCount
            state.commentCount = post.comments ?: state.commentCount
            state.isSaved = post.savedByMe ?: state.isSaved

            setupPostData(post, state)
            updateActiveState(isActive)
        }

        private fun setupPostData(post: Data, state: PostState) {
            val mediaList = post.mediaRef ?: emptyList()
            val postOwnerId = extractOwnerId(post)

            // Setup media
            if (mediaList.isNotEmpty()) {
                dotsIndicator = binding.indicatorLayout.apply {
                    dotsClickable = false
                    setDotIndicatorColor(ContextCompat.getColor(context, R.color.blue))
                    setStrokeDotsIndicatorColor(ContextCompat.getColor(context, R.color.grey))
                }

                mediaPagerAdapter = MediaPagerAdapter(
                    context,
                    ArrayList(mediaList),
                    isHolderActive,
                    postId,
                    state.isLiked,
                    state.likeCount,
                    state.commentCount,
                    individualViewModal,
                    { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                        state.isLiked = updatedIsLikedByMe
                        state.likeCount = updatedLikeCount
                        state.commentCount = updatedCommentCount
                        updateLikeButton(state.isLiked, state.likeCount)
                        binding.likeTv.text = formatCount(state.likeCount)
                        binding.commentTv.text = formatCount(state.commentCount)
                    }
                )

                binding.viewPager.adapter = mediaPagerAdapter
                binding.mediaLayout.visibility = View.VISIBLE

                if (mediaList.size > 1) {
                    binding.indicatorLayout.visibility = View.VISIBLE
                    dotsIndicator?.attachTo(binding.viewPager)
                } else {
                    binding.indicatorLayout.visibility = View.GONE
                }
            } else {
                binding.mediaLayout.visibility = View.GONE
            }

            // Setup profile info
            val postedBy = post.postedBy
            val accountType = postedBy?.accountType
            var name = ""
            var profilePic = ""

            if (accountType == "individual") {
                binding.businessTypeLayout.visibility = View.GONE
                binding.profileCv.strokeColor = ContextCompat.getColor(context, R.color.transparent)
                name = postedBy?.name ?: ""
                profilePic = postedBy?.profilePic?.large ?: ""
            } else {
                binding.businessTypeLayout.visibility = View.VISIBLE
                val businessProfile = postedBy?.businessProfileRef
                val businessType = businessProfile?.businessTypeRef?.name ?: ""
                val businessSubType = businessProfile?.businessSubtypeRef?.name ?: ""
                val averageRating = businessProfile?.businessRating ?: 0.0

                binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
                binding.typeTv.text = "$businessType - $businessSubType"
                binding.profileCv.strokeColor = ContextCompat.getColor(context, R.color.post_stroke)

                name = businessProfile?.name ?: ""
                profilePic = businessProfile?.profilePic?.large ?: ""
            }

            Glide.with(context)
                .load(profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.profileIv)

            binding.nameTv.text = name

            val createdAt = post.createdAt ?: ""
            val formattedCreatedAt = calculateDaysAgo(createdAt, context)
            binding.locationTv.text = formattedCreatedAt

            // Setup content
            val content = post.content.orEmpty().trim()
            if (content.isNotEmpty()) {
                binding.allDescriptionTv.text = content
                binding.allDescriptionTv.visibility = View.VISIBLE
            } else {
                binding.allDescriptionTv.visibility = View.GONE
            }

            // Setup engagement
            updateLikeButton(state.isLiked, state.likeCount)
            binding.commentTv.text = formatCount(state.commentCount)
            binding.shareTv.text = formatCount(post.shared ?: 0)
            updateSaveButton(state.isSaved)

            // Setup click listeners
            binding.likeBtn.setOnClickListener {
                individualViewModal.likePost(postId)
                state.isLiked = !state.isLiked
                state.likeCount = if (state.isLiked) state.likeCount + 1 else state.likeCount - 1
                updateLikeButton(state.isLiked, state.likeCount)
                onLikeUpdated(postId, state.isLiked, state.likeCount)
                mediaPagerAdapter?.updateLikeBtn(state.isLiked, state.likeCount)
            }

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

            binding.shareBtn.setOnClickListener {
                if (postId.isNotBlank() && postOwnerId.isNotBlank()) {
                    val selectedMedia = if (mediaList.isNotEmpty()) {
                        val currentIndex = binding.viewPager.currentItem.coerceIn(0, mediaList.size - 1)
                        mediaList.getOrNull(currentIndex)
                    } else null

                    val mediaType = selectedMedia?.mediaType?.lowercase(Locale.getDefault())
                    SharePostBottomSheetFragment.newInstance(
                        postId = postId,
                        ownerUserId = postOwnerId,
                        mediaType = mediaType,
                        mediaUrl = selectedMedia?.sourceUrl,
                        thumbnailUrl = selectedMedia?.thumbnailUrl,
                        mediaId = selectedMedia?.Id
                    ).show(fragmentManager, SharePostBottomSheetFragment::class.java.simpleName)
                } else {
                    context.sharePostWithDeepLink(postId, postOwnerId)
                }
            }

            binding.saveIv.setOnClickListener {
                individualViewModal.savePost(postId)
                state.isSaved = !state.isSaved
                updateSaveButton(state.isSaved)
            }

            // Setup menu button
            val isOwner = this@UserPostsViewerAdapter.isPostOwner(post)
            binding.menuBtn.setOnClickListener { view ->
                this@UserPostsViewerAdapter.showPostMenu(view, post, postId)
            }

            // Setup profile tap navigation
            binding.userLayout.setOnClickListener {
                val profileUserId = postOwnerId.ifBlank { extractOwnerId(post) }
                if (profileUserId.isNotEmpty()) {
                    // Navigate to user profile
                    val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
                    intent.putExtra("USER_ID", profileUserId)
                    context.startActivity(intent)
                }
            }
        }

        private fun updateLikeButton(isLiked: Boolean, likeCount: Int) {
            binding.likeIv.setImageResource(if (isLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon)
            binding.likeTv.text = formatCount(likeCount)
        }

        private fun updateSaveButton(isSaved: Boolean) {
            binding.saveIv.setImageResource(if (isSaved) R.drawable.ic_save_icon_white else R.drawable.ic_unsave_icon)
        }

        fun updateActiveState(isActive: Boolean) {
            isHolderActive = isActive
            // Recreate MediaPagerAdapter with new active state if media exists
            currentPost?.let { post ->
                val mediaList = post.mediaRef ?: emptyList()
                if (mediaList.isNotEmpty()) {
                    val state = postStates[postId] ?: return@let
                    // Recreate adapter with updated active state
                    mediaPagerAdapter = MediaPagerAdapter(
                        context,
                        ArrayList(mediaList),
                        isActive,
                        postId,
                        state.isLiked,
                        state.likeCount,
                        state.commentCount,
                        individualViewModal,
                        { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                            state.isLiked = updatedIsLikedByMe
                            state.likeCount = updatedLikeCount
                            state.commentCount = updatedCommentCount
                            updateLikeButton(state.isLiked, state.likeCount)
                            binding.likeTv.text = formatCount(state.likeCount)
                            binding.commentTv.text = formatCount(state.commentCount)
                        }
                    )
                    binding.viewPager.adapter = mediaPagerAdapter
                }
            }
        }

        fun refreshLikeState(state: PostState) {
            updateLikeButton(state.isLiked, state.likeCount)
        }
    }

    fun isPositionActive(position: Int): Boolean = position == activePosition

    fun setUserFollowState(userId: String, isFollowing: Boolean, isRequested: Boolean) {
        applyFollowState(userId, isFollowing, isRequested)
    }

    private fun applyFollowState(userId: String, isFollowing: Boolean, isRequested: Boolean, notify: Boolean = true) {
        val existing = followStateByUser[userId]
        if (existing != null && existing.isFollowing == isFollowing && existing.isRequested == isRequested) {
            return
        }
        followStateByUser[userId] = FollowState(isFollowing, isRequested)

        if (!notify) return

        for (i in 0 until itemCount) {
            val post = getItem(i)
            if (extractOwnerId(post) == userId) {
                post?.Id?.let { postId ->
                    postStates[postId]?.let { state ->
                        state.isFollowing = isFollowing
                        state.isRequested = isRequested
                    }
                }
                notifyItemChanged(i, PAYLOAD_FOLLOW_STATE)
            }
        }
    }

    private fun extractOwnerId(post: Data?): String {
        post ?: return ""
        val postedBy = post.postedBy
        return when {
            !postedBy?.Id.isNullOrBlank() -> postedBy?.Id ?: ""
            !postedBy?.businessProfileRef?.Id.isNullOrBlank() -> postedBy?.businessProfileRef?.Id ?: ""
            !post.userID.isNullOrBlank() -> post.userID ?: ""
            else -> ""
        }
    }

    /**
     * Check if the current user is the owner of a post
     */
    private fun isPostOwner(post: Data): Boolean {
        val currentUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").orEmpty()
        val postUserId = post.userID?.trim().orEmpty()
        val postedById = post.postedBy?.Id?.trim().orEmpty()
        
        return (postUserId.isNotEmpty() && (postUserId == ownerUserId.trim() || postUserId == currentUserId)) || 
               (postedById.isNotEmpty() && (postedById == ownerUserId.trim() || postedById == currentUserId))
    }

    /**
     * Show the post menu dialog (Edit/Delete for owners, Report for others)
     */
    private fun showPostMenu(view: View?, post: Data, postId: String) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.delete_edit_post_menu_dropdown_item, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Check ownership
        val isOwner = isPostOwner(post)

        // Find TextViews and set click listeners
        val editBtn: TextView? = dropdownView.findViewById(R.id.editBtn)
        val deleteBtn: TextView? = dropdownView.findViewById(R.id.deleteBtn)
        val reportBtn: TextView? = dropdownView.findViewById(R.id.reportBtn)
        val addToStoryBtn: TextView? = dropdownView.findViewById(R.id.addToStoryBtn)

        // Show/hide buttons based on ownership
        if (isOwner) {
            // Owner sees Edit and Delete options
            editBtn?.visibility = View.VISIBLE
            deleteBtn?.visibility = View.VISIBLE
            reportBtn?.visibility = View.GONE
            addToStoryBtn?.visibility = View.GONE
        } else {
            // Others see only Report option
            editBtn?.visibility = View.GONE
            deleteBtn?.visibility = View.GONE
            reportBtn?.visibility = View.VISIBLE
            addToStoryBtn?.visibility = View.GONE
        }

        // Edit button click listener
        editBtn?.setOnClickListener {
            val currentContent = post.content.orEmpty()
            val currentFeeling = post.feelings
            val currentMedia = post.mediaRef ?: emptyList()
            val location = post.location
            val placeName = location?.placeName
            val lat = location?.lat
            val lng = location?.lng
            
            if (postId.isNotEmpty()) {
                com.thehotelmedia.android.activity.userTypes.forms.EditPostActivity.start(
                    context,
                    postId,
                    currentContent,
                    currentFeeling,
                    currentMedia,
                    placeName,
                    lat,
                    lng
                )
            }
            popupWindow.dismiss()
        }

        // Delete button click listener
        deleteBtn?.setOnClickListener {
            val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDeletePost(context))
            bottomSheet.onYesClicked = {
                individualViewModal.deletePost(postId)
                popupWindow.dismiss()
            }
            bottomSheet.onNoClicked = {
                // User cancelled, do nothing
            }
            bottomSheet.show(fragmentManager, "YesOrNoBottomSheet")
            popupWindow.dismiss()
        }

        // Report button click listener
        reportBtn?.setOnClickListener {
            reportPost(postId)
            popupWindow.dismiss()
        }

        // Set the background drawable to make the popup visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(view)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }

    /**
     * Report a post
     */
    private fun reportPost(postId: String) {
        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", postId)
                putString("TYPE", "post")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportPosts(postId, selectedReason)
            }
        }
        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
    }

    fun pauseAllPlayers() {
        // Pause all players without stopping (keeps media loaded for resume)
        exoPlayers.values.forEach { player ->
            player.playWhenReady = false
            player.pause()
        }
    }

    fun stopAllPlayers() {
        // Stop all players completely (for destroy/finish)
        exoPlayers.values.forEach { player ->
            player.playWhenReady = false
            player.pause()
            player.stop()
            player.seekTo(0)
            player.volume = 0f
        }
        activePosition = RecyclerView.NO_POSITION
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        when (holder) {
            is PostViewHolder -> holder.onViewAttachedToWindow()
            is FeedViewHolder -> { /* Feed doesn't need special handling */ }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        when (holder) {
            is PostViewHolder -> holder.onViewDetachedFromWindow()
            is FeedViewHolder -> { /* Feed doesn't need special handling */ }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is PostViewHolder -> holder.release()
            is FeedViewHolder -> { /* Feed doesn't need special cleanup */ }
        }
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
                notifyItemChanged(position, PAYLOAD_LIKE)
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

    /**
     * Find the position of a post that contains a specific media ID
     */
    fun findPostPositionByMediaId(mediaId: String): Int {
        for (i in 0 until itemCount) {
            val item = getItem(i)
            val mediaRef = item?.mediaRef ?: emptyList()
            if (mediaRef.any { it.Id == mediaId }) {
                return i
            }
        }
        return -1
    }

    /**
     * Find the position of a post that contains a specific media URL (sourceUrl)
     * Handles URL matching with normalization (removes query params, trailing slashes, etc.)
     */
    fun findPostPositionByMediaUrl(mediaUrl: String): Int {
        if (mediaUrl.isBlank()) return -1
        
        // Normalize the input URL for comparison
        val normalizedInput = normalizeUrl(mediaUrl)
        
        for (i in 0 until itemCount) {
            val item = getItem(i)
            val mediaRef = item?.mediaRef ?: emptyList()
            if (mediaRef.any { 
                val sourceUrl = it.sourceUrl ?: ""
                if (sourceUrl.isNotEmpty()) {
                    val normalizedSource = normalizeUrl(sourceUrl)
                    // Try exact match first, then normalized match
                    sourceUrl == mediaUrl || normalizedSource == normalizedInput
                } else {
                    false
                }
            }) {
                return i
            }
        }
        return -1
    }
    
    /**
     * Normalize URL for comparison by removing query parameters and trailing slashes
     */
    private fun normalizeUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val scheme = uri.scheme ?: ""
            val authority = uri.authority ?: ""
            val path = uri.path ?: ""
            val normalizedPath = path.trimEnd('/')
            "$scheme://$authority$normalizedPath"
        } catch (e: Exception) {
            // If parsing fails, just return the original URL
            url.trimEnd('/')
        }
    }

    companion object {
        private const val VIEW_TYPE_REEL = 0
        private const val VIEW_TYPE_FEED = 1
        private const val PAYLOAD_ACTIVE = "payload_active"
        private const val PAYLOAD_LIKE = "payload_like"
        private const val PAYLOAD_FOLLOW_STATE = "payload_follow_state"

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
        exoPlayers.values.forEach {
            it.playWhenReady = false
            it.pause()
            it.seekTo(0)
            it.stop()
            it.release()
        }
        exoPlayers.clear()
        postStates.clear()
    }
}

