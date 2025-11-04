package com.thehotelmedia.android.fragments.userTypes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.activity.ViewEventDetailsActivity
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaPagerAdapter
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.EditPostBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.TagPeopleBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.EventItemsLayoutBinding
import com.thehotelmedia.android.databinding.PostItemsLayoutBinding
import com.thehotelmedia.android.databinding.ReviewItemsLayoutBinding
import com.thehotelmedia.android.extensions.EncryptionHelper
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.getEmojiForRating
import com.thehotelmedia.android.extensions.isFutureDateOrTime
import com.thehotelmedia.android.extensions.moveToPostPreviewScreen
import com.thehotelmedia.android.extensions.openGoogleMaps
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.setRatingWithStars
import com.thehotelmedia.android.extensions.shareEventsWithDeepLink
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.extensions.updateTextWithAnimation
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class SavedFeedAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val parentFragmentManager: FragmentManager,
    private val ownerUserId: String,
    private val from: String,
    private val lifecycleScope: LifecycleCoroutineScope,
) : PagingDataAdapter<Data, RecyclerView.ViewHolder>(SAVED_FEED_DIFF_CALLBACK()) {
    private var dotsIndicator: SpringDotsIndicator? = null
    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_REVIEW = 1
        private const val VIEW_TYPE_EVENT = 2
    }


    private var activePosition = 0 // No active position initially

    private lateinit var mediaPagerAdapter : MediaPagerAdapter




    inner class PostViewHolder(val binding: PostItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Data, isActive: Boolean, position: Int) {
            setPostData(post,binding,isActive,position)

        }
    }




    inner class ReviewViewHolder(val binding: ReviewItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Data, isActive: Boolean, position: Int) {
            setReviewData(review,binding,isActive,position)
        }
    }



    inner class EventViewHolder(val binding: EventItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: Data, isActive: Boolean, position: Int) {
            setEventData(event,binding,isActive,position)

        }
    }



    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item?.postType) {
            "post" -> VIEW_TYPE_POST
            "review" -> VIEW_TYPE_REVIEW
            "event" -> VIEW_TYPE_EVENT
            else -> throw IllegalArgumentException("Invalid post type: ${item?.postType}")
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_POST -> {
                val binding = PostItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding)
            }
            VIEW_TYPE_REVIEW -> {
                val binding = ReviewItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReviewViewHolder(binding)
            }
            VIEW_TYPE_EVENT -> {
                val binding = EventItemsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                EventViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val isActive = position == activePosition
        when (val item = getItem(position)) {
            is Data -> {
                when (holder) {
                    is PostViewHolder -> holder.bind(item,isActive,position)
                    is ReviewViewHolder -> holder.bind(item,isActive,position)
                    is EventViewHolder -> holder.bind(item,isActive,position)
                }
            }
            else -> throw IllegalArgumentException("Invalid item")
        }
    }

    private fun generateDeepLink(encryptedId: String, encryptedName: String): String {
        val baseUrl = "https://thehotelmedia.com/post"
        return "$baseUrl?type=$encryptedId&name=$encryptedName"
    }
    private fun shareContent(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }


    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }


    private fun setPostData(
        post: Data,
        binding: PostItemsLayoutBinding,
        isActive: Boolean,
        position: Int
    ) {
        val itemData = getItem(position)
        // Media setup
        val mediaList = post.mediaRef

        val postId = post.Id ?: ""

        dotsIndicator = binding.indicatorLayout.apply {
            dotsClickable = false
            setDotIndicatorColor(ContextCompat.getColor(context, R.color.blue))
            setStrokeDotsIndicatorColor(ContextCompat.getColor(context, R.color.grey))
        }

        // Save/Like state handling
        var isPostSaved = post.savedByMe ?: false
        var isPostLiked = post.likedByMe ?: false
        // Like, Comment, and Share counts
        var likeCount = post.likes ?: 0
        var commentCount = post.comments ?: 0
        val shareCount = post.shared ?: 0
        binding.likeTv.text = formatCount(likeCount)
        binding.commentTv.text = formatCount(commentCount)
        binding.shareTv.text = formatCount(shareCount)

        val viewsCount = post.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        if (mediaList.isNotEmpty()){
            mediaPagerAdapter = MediaPagerAdapter(
                context,
                mediaList,
                isActive,
                postId,
                isPostLiked,
                likeCount,
                commentCount
            ){ updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                updateLikeBtn(updatedIsLikedByMe, binding.likeIv)
                binding.likeTv.text = updatedLikeCount.toString()
                binding.commentTv.text = updatedCommentCount.toString()
                // You can also update UI elements in the activity here
            }
            binding.viewPager.adapter = mediaPagerAdapter
            binding.mediaLayout.visibility = View.VISIBLE
        }else{
            binding.mediaLayout.visibility = View.GONE
        }

//        mediaPagerAdapter = MediaPagerAdapter(context, mediaList, isActive)
//        binding.viewPager.adapter = mediaPagerAdapter

//        dotsIndicator?.attachTo(binding.viewPager)
//        binding.indicatorLayout.visibility = if (mediaList.size == 1) View.GONE else View.VISIBLE
        if (mediaList.size > 1) {
            binding.indicatorLayout.visibility = View.VISIBLE
            dotsIndicator?.attachTo(binding.viewPager)
        } else {
            binding.indicatorLayout.visibility = View.GONE
        }

        val createdAt = post.createdAt ?: ""
        val formatedCreatedAt = calculateDaysAgo(createdAt,context)

        // Handle user details
        val postedBy = post.postedBy
        val accountType = postedBy?.accountType
        val userId = postedBy?.Id ?: ""
        var name = ""
        var profilePic = ""

        if (accountType == "individual") {
            binding.businessTypeLayout.visibility = View.GONE
            binding.profileCv.strokeColor = ContextCompat.getColor(context, R.color.transparent)
            name = postedBy.name ?: ""
            profilePic = postedBy.profilePic?.large ?: ""


            binding.locationTv.text = formatedCreatedAt
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

            val address = businessProfile?.address
            val state = address?.state
            val country = address?.country
            val locationText = "$state, $country"
            // Update the TextView with animation
            binding.locationTv.updateTextWithAnimation(isActive, locationText, formatedCreatedAt)

        }

        // Set user name and profile picture
        binding.nameTv.text = name

        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.profileIv)

//        binding.profileIv.loadImageInBackground(context, profilePic, R.drawable.ic_profile_placeholder)



        // Post content
        val content = post.content.orEmpty().trim()
        val feelings = post.feelings.orEmpty().trim()
        val location = post.location?.placeName.orEmpty().trim()
        val taggedPeople = post.taggedRef
        val lat  = post.location?.lat ?: 0.00
        val lng  = post.location?.lng ?: 0.00

        if (content.isNotEmpty() || feelings.isNotEmpty() || location.isNotEmpty() || taggedPeople.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                val taggedRefString = generateTaggedRefString(taggedPeople)
                withContext(Dispatchers.Main) {
                    setDescriptionTextAndClick(binding, content, feelings, taggedRefString, location,taggedPeople,lat,lng)
                }
            }
            binding.allDescriptionTv.visibility =View.VISIBLE
        }else{
            binding.allDescriptionTv.visibility =View.GONE
        }


        updateSaveBtn(isPostSaved, binding.saveIv)
        updateLikeBtn(isPostLiked, binding.likeIv)

        // Save button click
        binding.saveIv.setOnClickListener {
            savePost(postId)
            isPostSaved = !isPostSaved
            updateSaveBtn(isPostSaved, binding.saveIv)
        }

        // Like button click
        binding.likeBtn.setOnClickListener {
            likePost(postId)
            isPostLiked = !isPostLiked
            binding.likeIv.setImageResource(if (isPostLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon)
            likeCount = if (isPostLiked) likeCount + 1 else likeCount - 1
            binding.likeTv.text = formatCount(likeCount)
            post.likes = likeCount
            post.likedByMe = isPostLiked
        }

        // Comment button click
        binding.commentBtn.setOnClickListener {
            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putInt("COMMENTS_COUNT", commentCount)
                }
            }
            bottomSheetFragment.onCommentSent = { comment ->
                if (comment.isNotEmpty()) {
                    commentCount++
                    binding.commentTv.text = formatCount(commentCount)
                    notifyDataSetChanged()
                }
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

        // User profile click
        binding.userLayout.setOnClickListener {
            moveToBusinessProfileDetailsActivity(userId)
        }

        // Share button click
        binding.shareBtn.setOnClickListener {
            val encryptedId = EncryptionHelper.encrypt("categoryId")
            val encryptedName = EncryptionHelper.encrypt("categoryName")
            val link = generateDeepLink(encryptedId, encryptedName)
            val message = "Hey there, Look post here:\n\n$link"
            shareContent(message)
        }

        // Share button click
        binding.shareBtn.setOnClickListener {
            context.sharePostWithDeepLink(postId,ownerUserId)
        }

        // Show edit and delete buttons only for the owner of the post
        val isOwner = (post.userID ?: "") == ownerUserId
        binding.editBtn.visibility = if (isOwner) View.VISIBLE else View.GONE
        binding.deleteBtn.visibility = if (isOwner) View.VISIBLE else View.GONE

        binding.editBtn.setOnClickListener {
            val currentContent = post.content.orEmpty()
            val currentFeeling = post.feelings
            val currentMedia = post.mediaRef
            val postId = post.Id ?: ""
            
            if (postId.isNotEmpty()) {
                com.thehotelmedia.android.activity.userTypes.forms.EditPostActivity.start(
                    context,
                    postId,
                    currentContent,
                    currentFeeling,
                    currentMedia
                )
            }
        }

        binding.deleteBtn.setOnClickListener {
            val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDeletePost(context))
            bottomSheet.onYesClicked = {
                if (itemData != null) {
                    removeItem(itemData)
                    individualViewModal.deletePost(postId)
                }
            }
            bottomSheet.onNoClicked = {
                // User cancelled, do nothing
            }
            bottomSheet.show(parentFragmentManager, "YesOrNoBottomSheet")
        }

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view, postId, itemData)
        }
    }

    private fun setReviewData(
        review: Data,
        binding: ReviewItemsLayoutBinding,
        isActive: Boolean,
        position: Int
    ) {
        val itemData = getItem(position)
        // Rating
        val rating = review.rating?.toInt() ?: 0
        val averageRating = review.reviewedBusinessProfileRef?.rating ?: 0.0
        binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
        binding.ratingTV.setRatingWithStars(rating)
        binding.ratingTypeTV.text = getEmojiForRating(rating)

        val googleReviewedBusiness = review.googleReviewedBusiness ?: ""
        val publicUserID = review.publicUserID ?: ""

        // Blur views
        context.blurTheView(binding.topBlurView)
        context.blurTheView(binding.bottomBlurView)
        var commentCount = review.comments ?: 0
        // Review Details
        val postedBy = review.postedBy ?: return
        val createdAt = review.createdAt ?: ""
        val userName = postedBy.name ?: ""
        val userProfilePic = postedBy.profilePic?.large ?: ""
        val businessName = review.reviewedBusinessProfileRef?.name ?: ""
        val businessProfilePic = review.reviewedBusinessProfileRef?.profilePic?.large ?: ""
        val coverImage = review.reviewedBusinessProfileRef?.coverImage ?: ""
        val businessType = review.reviewedBusinessProfileRef?.businessTypeRef?.name
        val businessSubType = review.reviewedBusinessProfileRef?.businessSubtypeRef?.name
        val city = review.reviewedBusinessProfileRef?.address?.city
        val state = review.reviewedBusinessProfileRef?.address?.state

        // Set user and business info
        binding.userNameTv.text = userName

        Glide.with(context).load(userProfilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.userProfileIv)

//        binding.userProfileIv.loadImageInBackground(context, userProfilePic, R.drawable.ic_profile_placeholder)
        binding.hotelNameTv.text = businessName

        Glide.with(context).load(businessProfilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        Glide.with(context).load(coverImage).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.imageView)

//        binding.hotelProfileIv.loadImageInBackground(context, businessProfilePic, R.drawable.ic_profile_placeholder)
//        binding.imageView.loadImageInBackground(context, coverImage.toString(), R.drawable.ic_post_placeholder)
        binding.hotelTypeTv.apply {
            visibility = if (businessType.isNullOrBlank()) View.GONE else View.VISIBLE
            text = when {
                businessType.isNullOrBlank() -> ""
                businessSubType.isNullOrBlank() -> businessType
                else -> "$businessType - $businessSubType"
            }
        }




        binding.location.text = "$city, $state"


        var likesCount = review.likes ?: 0
        var sharedCount = review.shared ?: 0

        // Like, Comment, and Share Counts
        binding.likeTv.text = formatCount(likesCount)
        binding.commentTv.text = formatCount(commentCount)
        binding.shareTv.text = formatCount(sharedCount)

        val viewsCount = review.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        // Content and Date
        binding.contentTv.text = review.content
        binding.daysTv.text = calculateDaysAgo(createdAt,context)

        // Post interaction states
        var isPostSaved = review.savedByMe ?: false
        var isPostLiked = review.likedByMe ?: false
        updateSaveBtn(isPostSaved, binding.saveIv)
        updateLikeBtn(isPostLiked, binding.likeIv)

        // Save, Like, and Comment buttons actions
        val postId = review.Id.toString()
        binding.saveIv.setOnClickListener {
            savePost(postId)
            isPostSaved = !isPostSaved
            updateSaveBtn(isPostSaved, binding.saveIv)
        }
        binding.likeBtn.setOnClickListener {
            likePost(postId)
            isPostLiked = !isPostLiked
            binding.likeIv.setImageResource(if (isPostLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon)
            val likes = review.likes?.let { it + if (isPostLiked) 1 else -1 } ?: 0
            binding.likeTv.text = formatCount(likes)
            review.likes = review.likes?.plus(if (isPostLiked) 1 else -1)
        }
        binding.commentBtn.setOnClickListener {
            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putInt("COMMENTS_COUNT", commentCount)
                }
            }
            bottomSheetFragment.onCommentSent = { comment ->
                if (comment.isNotEmpty()) {
                    commentCount++
                    binding.commentTv.text = formatCount(commentCount)
                }
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }


        binding.imageView.setOnClickListener {
            context.moveToPostPreviewScreen(postId)
        }

        // User layout click action
        binding.userLayout.setOnClickListener {
            if (publicUserID.isNotEmpty()){
                Toast.makeText(context,context.getString(R.string.profile_not_associated_thm), Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(review.postedBy?.Id ?: "")
            }
        }


        binding.businessLayout.setOnClickListener {
            if (googleReviewedBusiness.isNotEmpty()){
                Toast.makeText(context,context.getString(R.string.profile_not_associated_thm),
                    Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(review.reviewedBusinessProfileRef?.userId ?: "")
            }
        }
        // Share button click
        binding.shareBtn.setOnClickListener {
            context.sharePostWithDeepLink(postId,ownerUserId)
        }

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view, postId, itemData)
        }
    }





    private fun setEventData(
        event: Data,
        binding: EventItemsLayoutBinding,
        isActive: Boolean,
        position: Int
    ) {
        val itemData = getItem(position)
        // Extract necessary data
        val averageRating = event.postedBy?.businessProfileRef?.businessRating ?: 0.0
        val shareCount = event.shared ?: 0
        val userId = event.postedBy?.Id ?: ""
        val postId = event.Id ?: ""
        val name = event.postedBy?.businessProfileRef?.name ?: ""
        val profilePic = event.postedBy?.businessProfileRef?.profilePic?.large ?: ""
        val mediaRef = event.mediaRef
        val coverImage = if (mediaRef.isEmpty()){
            ""
        }else{
            event.mediaRef[0].sourceUrl ?: ""
        }
        val businessType = event.postedBy?.businessProfileRef?.businessTypeRef?.name
        val businessSubType = event.postedBy?.businessProfileRef?.businessSubtypeRef?.name
        val eventName = event.name ?: ""
        val venue = event.venue ?: ""
        val dateString = event.startDate ?: ""
        val timeString = event.startTime ?: ""
        var interestedPeople = event.interestedPeople ?: 0


        val startDate = event.startDate ?: ""
        val startTime = event.startTime ?: ""
        if (isFutureDateOrTime(startDate,startTime)) {
            binding.joiningBtn.visibility = View.VISIBLE
        } else {
            binding.joiningBtn.visibility = View.GONE
        }


        // Set UI elements
        if (interestedPeople != 0){
            binding.peopleInterestedTv.text = "$interestedPeople ${context.getString(R.string.interested_people)}"
            binding.peopleInterestedTv.visibility = View.VISIBLE
        }else{
            binding.peopleInterestedTv.visibility = View.GONE
        }

        binding.shareTv.text = formatCount(shareCount)

        val viewsCount = event.views ?: 0
        if (viewsCount > 0){
            binding.viewsBtn.visibility = View.VISIBLE
            binding.viewsTv.text = formatCount(viewsCount)
        }

        binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
        binding.nameTv.text = name

        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        Glide.with(context).load(coverImage).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.coverImage)


//        binding.profileIv.loadImageInBackground(context, profilePic, R.drawable.ic_profile_placeholder)
//        binding.coverImage.loadImageInBackground(context, coverImage, R.drawable.ic_post_placeholder)
        binding.typeTv.text = "$businessType - $businessSubType"
        binding.eventNameTv.text = eventName.trim()
        binding.eventVenueTv.text = venue.trim()
        binding.dateTimeTv.text = formatEventDateTime(dateString, timeString)


        val address = event.postedBy?.businessProfileRef?.address
        val state = address?.state
        val country = address?.country
        binding.locationTv.text = "$state, $country"


        // Hide venue if empty
        if (venue.isEmpty()) binding.eventVenueTv.visibility = View.GONE

        // Joining status
        var isJoined = event.imJoining ?: false
        updateJoiningBtn(isJoined, binding.joiningIv, binding.joiningTv)

        // Save post state
        var isPostSaved = event.savedByMe ?: false
        updateSaveBtn(isPostSaved, binding.saveIv)

        // Handle button clicks
        binding.saveIv.setOnClickListener {
            savePost(event.Id.toString())
            isPostSaved = !isPostSaved
            updateSaveBtn(isPostSaved, binding.saveIv)
        }

        binding.coverImage.setOnClickListener {
            val intent = Intent(context, ViewEventDetailsActivity::class.java).apply {
                putExtra("POST_ID", postId)
            }
            context.startActivity(intent)
        }

        binding.joiningBtn.setOnClickListener {
            joinEvent(event.Id.toString())
            isJoined = !isJoined
            updateJoiningBtn(isJoined, binding.joiningIv, binding.joiningTv)
            val updatedInterestedPeople = if (isJoined){
                interestedPeople + 1
            }else{
                interestedPeople - 1
            }
            interestedPeople = updatedInterestedPeople
            binding.peopleInterestedTv.text = "$updatedInterestedPeople ${context.getString(R.string.interested_people)}"
        }

        binding.userLayout.setOnClickListener {
            moveToBusinessProfileDetailsActivity(userId)
        }

        // Share button click
        binding.shareBtn.setOnClickListener {
            context.shareEventsWithDeepLink(postId,ownerUserId)
        }

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view,postId,itemData)
        }


        var commentCount = event.comments ?: 0
        binding.commentTv.text = formatCount(commentCount)


        // Comment button click
        binding.commentBtn.setOnClickListener {
            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putInt("COMMENTS_COUNT", commentCount)
                }
            }
            bottomSheetFragment.onCommentSent = { comment ->
                if (comment.isNotEmpty()) {
                    commentCount++
                    binding.commentTv.text = formatCount(commentCount)
                }
            }
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

    }

    private fun formatEventDateTime(dateString: String, timeString: String): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(timeString)
        val formattedDate = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(date)
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(time)
        return "$formattedDate at $formattedTime"
    }

//    private fun updateJoiningStatus(isJoined: Boolean, joiningIv: ImageView, joiningTv: TextView) {
//        if (isJoined) {
//            joiningIv.setImageResource(R.drawable.ic_filled_star_blue)
//            joiningTv.text = "Joined"
//        } else {
//            joiningIv.setImageResource(R.drawable.ic_outline_star_white)
//            joiningTv.text = "Joining ?"
//        }
//    }




    private fun updateJoiningBtn(postSaved: Boolean, joiningIv: ImageView, joiningTv: TextView) {
        if (postSaved) {
            joiningIv.setImageResource(R.drawable.ic_filled_star_blue)
            joiningTv.text = "Joined"
        } else {
            joiningIv.setImageResource(R.drawable.ic_outline_star_white)
            joiningTv.text = "Joining ?"
        }
    }

    private fun setDescriptionTextAndClick(binding: PostItemsLayoutBinding,description: String,feeling: String,people: String,location: String,taggedPeople: ArrayList<TaggedRef>,lat: Double, lng: Double) {
        val textColor = ContextCompat.getColor(context, R.color.blue_50)

        val bgColor = ContextCompat.getColor(context, R.color.transparent)

        val parts = mutableListOf<String>()
        if (description.isNotEmpty()) parts.add(description)
        if (feeling.isNotEmpty()) parts.add(feeling)
        if (people.isNotEmpty()) parts.add("with $people")
        if (location.isNotEmpty()) parts.add("at $location")

// Join the non-empty parts with a separator
        val finalText = parts.joinToString(" - ")
//        val finalText = "$description - $feeling - with $peoples - at $location"
        val spannableString = SpannableString(finalText)

        // Get the start and end indices of each portion of text
        val feelingStart = finalText.indexOf(feeling)
        val feelingEnd = feelingStart + feeling.length
        val peoplesStart = finalText.indexOf(people)
        val peoplesEnd = peoplesStart + people.length
        val locationStart = finalText.indexOf(location)
        val locationEnd = locationStart + location.length


        // Use the default Kotlin URL regex
        val matcher = Constants.URL_PATTERN_MATCHER.matcher(finalText)

        // Highlight and make URLs clickable
        while (matcher.find()) {
            val url = matcher.group(0)
            val urlStart = matcher.start()
            val urlEnd = matcher.end()

            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Open the URL in Chrome
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = textColor // Set the link color to blue
                    ds.isUnderlineText = true // Make the text underlined
                    ds.bgColor = bgColor // Ensure background color is transparent
                }
            }, urlStart, urlEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }


        // Set color and clickable span for feeling
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
//                Toast.makeText(context, "Feeling", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, feelingStart, feelingEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set color and clickable span for peoples
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
//                Toast.makeText(context, "peoples", Toast.LENGTH_SHORT).show()
                // Convert the taggedPeople list to JSON
                val taggedPeopleJson = Gson().toJson(taggedPeople)

                // Pass the JSON to the BottomSheetFragment
                val bottomSheetFragment = TagPeopleBottomSheetFragment.newInstance(taggedPeopleJson)
                bottomSheetFragment.show(parentFragmentManager, "TagPeopleBottomSheet")
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, peoplesStart, peoplesEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set color and clickable span for location
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
//                Toast.makeText(context, "Location", Toast.LENGTH_SHORT).show()
                context.openGoogleMaps(lat, lng)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, locationStart, locationEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the spannable text and make the TextView clickable
        binding.allDescriptionTv.text = spannableString
        binding.allDescriptionTv.setBackgroundResource(R.drawable.transparent_background)
        binding.allDescriptionTv.movementMethod = LinkMovementMethod.getInstance()
    }
    private fun generateTaggedRefString(taggedRef: List<TaggedRef>): String {
        return when (taggedRef.size) {
            0 -> "" // No tagged references
            1 -> taggedRef[0].name ?: "" // Single tagged reference
            2 -> "${taggedRef[0].name} and ${taggedRef[1].name}" // Two tagged references
            else -> "${taggedRef[0].name} and ${taggedRef.size - 1} others" // More than two tagged references
        }
    }

    private fun savePost(id: String) {
        individualViewModal.savePost(id)
    }
    private fun likePost(id: String) {
        individualViewModal.likePost(id)
    }

    private fun joinEvent(id: String) {
        individualViewModal.joinEvent(id)
    }


    private fun updateSaveBtn(postSaved: Boolean, saveIv: ImageView) {
        if (postSaved) {
            saveIv.setImageResource(R.drawable.ic_save_icon)
        } else {
            saveIv.setImageResource(R.drawable.ic_unsave_icon)
        }
    }
    private fun updateLikeBtn(postLiked: Boolean, likeIv: ImageView) {
        if (postLiked) {
            likeIv.setImageResource(R.drawable.ic_like_icon)
        } else {
            likeIv.setImageResource(R.drawable.ic_unlike_icon)
        }
    }


//    private fun showMenuDialog(view: View?, postId: String) {
//
//        // Inflate the dropdown menu layout
//        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        val dropdownView = inflater.inflate(R.layout.single_post_menu_dropdown_item, null)
//
//        // Create the PopupWindow
//        val popupWindow = PopupWindow(
//            dropdownView,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            true
//        )
//
//        // Find TextViews and set click listeners
////        val blockBtn: TextView = dropdownView.findViewById(R.id.blockBtn)
//        val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
////        val shareBtn: TextView = dropdownView.findViewById(R.id.shareBtn)
//
//        reportBtn.setOnClickListener {
//            reportPost(postId)
//            popupWindow.dismiss()
//        }
//
//        // Set the background drawable to make the popup more visually appealing
//        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))
//        // Show the popup window
//        popupWindow.showAsDropDown(view)
//        // Optionally, dismiss the popup when clicking outside of it
//        popupWindow.setOnDismissListener {
//            // Handle any actions you want to perform when the popup is dismissed
//        }
//    }


    private fun showMenuDialog(view: View?, postId: String, itemData: Data?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Decide which layout to use based on the value of `from`
        val dropdownView = if (from == "Profile") {
            inflater.inflate(R.layout.delete_edit_post_menu_dropdown_item, null)
        } else {
            inflater.inflate(R.layout.single_post_menu_dropdown_item, null)
        }

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
        if (from == "Profile") {
            val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
            val deleteBtn: TextView = dropdownView.findViewById(R.id.deleteBtn)

            reportBtn.setOnClickListener {
                reportPost(postId)
                popupWindow.dismiss()
            }

            deleteBtn.setOnClickListener {



                val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDeletePost(context))
                bottomSheet.onYesClicked = {
                    if (itemData != null) {
                        removeItem(itemData)
                        individualViewModal.deletePost(postId)
                    }
                }
                bottomSheet.onNoClicked = {

                }
                bottomSheet.show(parentFragmentManager, "YesOrNoBottomSheet")




                popupWindow.dismiss()
            }
        } else {
            val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
            reportBtn.setOnClickListener {
                reportPost(postId)
                popupWindow.dismiss()
            }
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

    private fun reportPost(postId: String) {

        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", postId)
                putString("TYPE", "post")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportPosts(postId,selectedReason)
            }
        }
        bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)

    }

    fun setActivePosition(position: Int) {
        activePosition = position
    }

    class SAVED_FEED_DIFF_CALLBACK : DiffUtil.ItemCallback<Data>() {
        override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
            return oldItem == newItem
        }
    }


    private fun removeItem(item: Data) {
        lifecycleScope.launch {
            val currentList = snapshot().items // Get the current items
            val updatedList = currentList.filter { it.Id != item.Id } // Remove the clicked item

            // Convert the updated list to PagingData and submit it
            val newPagingData = PagingData.from(updatedList)
            submitData(newPagingData) // Submit the new data
            notifyDataSetChanged()
        }
    }


}
