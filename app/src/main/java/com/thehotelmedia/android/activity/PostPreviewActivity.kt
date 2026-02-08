package com.thehotelmedia.android.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.AttachmentAdapter
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.comments.CommentsAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaPagerAdapter
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.SharePostBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.TagPeopleBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityPostPreviewBinding
import com.thehotelmedia.android.extensions.EncryptionHelper
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.getEmojiForRating
import com.thehotelmedia.android.extensions.openGoogleMaps
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.setRatingWithStars
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.utils.findTooLongStoryVideoSeconds
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PostPreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityPostPreviewBinding

    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private var dotsIndicator: SpringDotsIndicator? = null
    private lateinit var preferenceManager: PreferenceManager
    private var ownerUserId = ""
    private var postId = ""
    private var parentId = ""
    private lateinit var commentsAdapter: CommentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
    }

    private fun initUi() {
        progressBar = CustomProgressBar(this)
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        preferenceManager = PreferenceManager.getInstance(this)
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()
        progressBar.show()
        binding.postLayout.visibility = View.GONE
        binding.reviewLayout.visibility = View.GONE


        if (intent.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            println("sdjakjfdska;k   uri  $uri")
            uri?.let {
                println("sdjakjfdska;k   it  $it")
                val postID = it.getQueryParameter("postID").toString()
                val userID = it.getQueryParameter("userID").toString()

                // Extract the encrypted values directly
                val encryptedPostID = it.getQueryParameter("postID").toString()
                val encryptedUserID = it.getQueryParameter("userID").toString()

                // Decrypt the values
                val decryptedPostID = EncryptionHelper.decrypt(encryptedPostID)
                val decryptedUserID = EncryptionHelper.decrypt(encryptedUserID)
                postId = decryptedPostID

                individualViewModal.getSharedPosts(postID,userID)


                // Now, you have the category ID and name, you can use them to populate the UI or perform any necessary actions
            }
        }else{
            val from = intent.getStringExtra("FROM")
            postId = intent.getStringExtra("POST_ID") ?: ""
            getPostData(postId)
        }

        if (postId.isBlank() || postId == "null") {
            CustomSnackBar.showSnackBar(binding.root, "Invalid post")
            finish()
            return
        }

        println("Afsdklaslk   $postId")

        binding.replyLayout.visibility = View.GONE

        binding.cancelReplyingBtn.setOnClickListener {
            binding.replyLayout.visibility = View.GONE
            parentId = ""
        }


        binding.sendCommentBtn.setOnClickListener {
            val comment =  binding.commentEt.text.toString().trim()

            if(comment.isNotEmpty()){
                println("sadakjhajkhfsaj   $comment")
                sendComment(comment)
            }

        }

        getCommentsData()


        binding.backBtn.setOnClickListener {
            finish()
        }

        individualViewModal.getSinglePostsResult.observe(this) { result ->
            if (result.status == true) {
                val data = result.data
                val type = data?.postType
                if (type == "post"){
                    binding.postLayout.visibility = View.VISIBLE
                    binding.reviewLayout.visibility = View.GONE
                    handelPostData(data)
                }else if (type == "review"){
                    binding.postLayout.visibility = View.GONE
                    binding.reviewLayout.visibility = View.VISIBLE
                    handelReviewData(data)
                }
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }



        individualViewModal.getSharedPostsResult.observe(this){result->
            if (result.status == true){
                val msg = result.message.toString()

                val data = result.data
                val type = data?.postType
                if (type == "post"){
                    binding.postLayout.visibility = View.VISIBLE
                    binding.reviewLayout.visibility = View.GONE
                    handelPostData(data)
                }else if (type == "review"){
                    binding.postLayout.visibility = View.GONE
                    binding.reviewLayout.visibility = View.VISIBLE
                    handelReviewData(data)
                }

//                val postType = result.data

            }
        }


        individualViewModal.loading.observe(this){
            if (it == true){
//                progressBar.show() // To show the giff progress bar
            }else{
                progressBar.hide() // To hide the giff progress bar
            }
        }

        individualViewModal.toast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

        individualViewModal.reportToast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }


    }

    private fun sendComment(comment: String) {
        binding.commentEt.text?.clear()
        individualViewModal.createComment(postId,comment,parentId)
        Handler(Looper.getMainLooper()).postDelayed({
            getCommentsData()
        }, 2000) // 1000 milliseconds = 1 seconds
    }

    private fun getCommentsData() {

        commentsAdapter = CommentsAdapter(
            this,
            ::onReplyClick,
            individualViewModal,
            supportFragmentManager,
            ownerUserId
        )

        if (binding.commentsRv.adapter == null) {
            binding.commentsRv.adapter = commentsAdapter.withLoadStateFooter(
                footer = LoaderAdapter { commentsAdapter.retry() }
            )
        }

        individualViewModal.getComments(postId).observe(this) {
            this.lifecycleScope.launch {
                isLoading()
                commentsAdapter.submitData(it)
            }
        }
    }
    private fun isLoading() {
        commentsAdapter.addLoadStateListener {

            val isLoading = it.refresh is LoadState.Loading

//            val itemCount = commentsAdapter.itemCount
//            binding.commentsTv.text = "$itemCount Comments"

            val isEmpty = it.refresh is LoadState.NotLoading &&
                    commentsAdapter.itemCount == 0
            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.commentsRv.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.commentsRv.visibility = View.VISIBLE
            }

        }
    }

    private fun onReplyClick(id: String, name: String, profilePic: String) {
        openKeyboard()
        parentId = id
        binding.replyLayout.visibility = View.VISIBLE
        Glide.with(this).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.replyingIv)
        binding.replyingTv.text = "${getString(R.string.replying_to)} ${name}"

    }
    private fun openKeyboard() {
        // Request focus on the EditText
        binding.commentEt.requestFocus()

        // Show the soft keyboard
        val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.commentEt, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun getPostData(postId: String?) {
        individualViewModal.getSinglePosts(postId.toString())
    }

    private fun handelPostData(data: Data) {

        // Media setup
        val mediaList = data.mediaRef

        val postId = data.Id ?: ""

        dotsIndicator = binding.indicatorLayout.apply {
            dotsClickable = false
            setDotIndicatorColor(ContextCompat.getColor(context, R.color.blue))
            setStrokeDotsIndicatorColor(ContextCompat.getColor(context, R.color.grey))
        }
        // Save/Like state handling
        var isPostSaved = data.savedByMe ?: false
        var isPostLiked = data.likedByMe ?: false
        // Like, Comment, and Share counts
        var likeCount = data.likes ?: 0
        var commentCount = data.comments ?: 0
        val shareCount = data.shared ?: 0
        binding.likeTv.text = formatCount(likeCount)
        binding.commentTv.text =formatCount(commentCount)
        binding.shareTv.text = formatCount(shareCount)

        val viewsCount = data.views ?: 0
        if (viewsCount > 0){
            binding.postViewsBtn.visibility = View.VISIBLE
            binding.postViewsTv.text = formatCount(viewsCount)
        }


        updateSaveBtn(isPostSaved, binding.saveIv)
        updateLikeBtn(isPostLiked, binding.likeIv)

        if (mediaList.isNotEmpty()){
            val mediaPagerAdapter = MediaPagerAdapter(
                this,
                mediaList,
                true,
                postId,
                isPostLiked,
                likeCount,
                commentCount,
                null, // individualViewModal
                { updatedIsLikedByMe, updatedLikeCount, updatedCommentCount ->
                    updateLikeBtn(updatedIsLikedByMe, binding.likeIv)
                    binding.likeTv.text = updatedLikeCount.toString()
                    binding.commentTv.text = updatedCommentCount.toString()
                    // You can also update UI elements in the activity here
                }
            )
            binding.viewPager.adapter = mediaPagerAdapter
            binding.mediaLayout.visibility = View.VISIBLE
        }else{
            binding.mediaLayout.visibility = View.GONE
        }


//        dotsIndicator?.attachTo(binding.viewPager)
//        binding.indicatorLayout.visibility = if (mediaList.size == 1) View.GONE else View.VISIBLE
        if (mediaList.size > 1) {
            binding.indicatorLayout.visibility = View.VISIBLE
            dotsIndicator?.attachTo(binding.viewPager)
        } else {
            binding.indicatorLayout.visibility = View.GONE
        }


        // Handle user details
        val postedBy = data.postedBy
        val accountType = postedBy?.accountType
        val userId = postedBy?.Id ?: ""
        var name = ""
        var profilePic = ""

        if (accountType == "individual") {
            binding.businessTypeLayout.visibility = View.GONE
            binding.profileCv.strokeColor = ContextCompat.getColor(this, R.color.transparent)
            name = postedBy.name ?: ""
            profilePic = postedBy.profilePic?.large ?: ""

            binding.locationTv.visibility = View.GONE
        } else {
            binding.businessTypeLayout.visibility = View.VISIBLE
            val businessProfile = postedBy?.businessProfileRef
            val businessType = businessProfile?.businessTypeRef?.name ?: ""
            val businessSubType = businessProfile?.businessSubtypeRef?.name ?: ""
            val averageRating = businessProfile?.businessRating ?: 0.0

            binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
            binding.typeTv.text = "$businessType - $businessSubType"
            binding.profileCv.strokeColor = ContextCompat.getColor(this, R.color.post_stroke)

            name = businessProfile?.name ?: ""
            profilePic = businessProfile?.profilePic?.large ?: ""


            val address = businessProfile?.address
            val state = address?.state
            val country = address?.country
            binding.locationTv.text = "$state, $country"
            binding.locationTv.visibility = View.VISIBLE
        }

        // Set user name and profile picture
        binding.nameTv.text = name
        Glide.with(this).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.profileIv)

//        binding.profileIv.loadImageInBackground(this, profilePic, R.drawable.ic_profile_placeholder)


        // Post content
        val content = data.content ?: ""
        val feelings = data.feelings ?: ""
        val location = data.location?.placeName ?: ""
        val lat = data.location?.lat ?: 0.00
        val lng = data.location?.lng ?: 0.00

        val taggedPeople = data.taggedRef

//        val taggedRefString = generateTaggedRefString(post.taggedRef)
//        setDescriptionTextAndClick(binding, content, feelings, taggedRefString, location)
        if (content.isNotEmpty() || feelings.isNotEmpty() || location.isNotEmpty() || taggedPeople.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                val taggedRefString = generateTaggedRefString(data.taggedRef)
                withContext(Dispatchers.Main) {
                    setDescriptionTextAndClick(binding, content, feelings, taggedRefString, location,taggedPeople,lat,lng)
                }
            }
            binding.allDescriptionTv.visibility =View.VISIBLE
        }else{
            binding.allDescriptionTv.visibility =View.GONE
        }



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
            data.likes = likeCount

        }

        // Comment button click
        binding.commentBtn.setOnClickListener {
//            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
//                arguments = Bundle().apply {
//                    putString("POST_ID", postId)
//                    putInt("COMMENTS_COUNT", commentCount)
//                }
//            }
//            bottomSheetFragment.onCommentSent = { comment ->
//                if (comment.isNotEmpty()) {
//                    commentCount++
//                    binding.commentTv.text = commentCount.toString()
//                }
//            }
//            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        // User profile click
        binding.userLayout.setOnClickListener {
            moveToBusinessProfileDetailsActivity(userId)
        }


        // Share button click
        binding.shareBtn.setOnClickListener {
            if (postId.isNotBlank() && ownerUserId.isNotBlank()) {
                val selectedMedia = if (mediaList.isNotEmpty()) {
                    val currentIndex = binding.viewPager.currentItem.coerceIn(0, mediaList.size - 1)
                    mediaList.getOrNull(currentIndex)
                } else null

                val mediaType = selectedMedia?.mediaType?.lowercase(Locale.getDefault())
                SharePostBottomSheetFragment.newInstance(
                    postId = postId,
                    ownerUserId = ownerUserId,
                    mediaType = mediaType,
                    mediaUrl = selectedMedia?.sourceUrl,
                    thumbnailUrl = selectedMedia?.thumbnailUrl,
                    mediaId = selectedMedia?.Id,
                    mediaDurationSeconds = selectedMedia?.duration
                ).show(supportFragmentManager, SharePostBottomSheetFragment::class.java.simpleName)
            } else {
                this.sharePostWithDeepLink(postId, ownerUserId)
            }
        }


        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view, postId, data, canShareToStory = true)
        }

    }

    private fun handelReviewData(data: Data) {

        val rating = data.rating?.toInt() ?: 0
        val averageRating = data.reviewedBusinessProfileRef?.rating ?: 0.0

        val mediaAttachment = data.mediaRef
        if (mediaAttachment.isNotEmpty()) {
            binding.attachmentRv.visibility = View.VISIBLE
            val attachmentAdapter = AttachmentAdapter(this,mediaAttachment)
            binding.attachmentRv.adapter = attachmentAdapter
        }else{
            binding.attachmentRv.visibility = View.GONE
        }

        val googleReviewedBusiness = data.googleReviewedBusiness ?: ""
        val publicUserID = data.publicUserID ?: ""

        binding.averageRatingTvReview.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
        binding.averageRatingTV.setRatingWithStars(rating)
        binding.ratingTypeTV.text = getEmojiForRating(rating)

        // Blur views
        this.blurTheView(binding.topBlurView)
        this.blurTheView(binding.bottomBlurView)

        // Review Details
        val postedBy = data.postedBy ?: return
        val createdAt = data.createdAt ?: ""
        val userName = postedBy.name ?: ""
        val userProfilePic = postedBy.profilePic?.large ?: ""
        val businessName = data.reviewedBusinessProfileRef?.name ?: ""
        val businessProfilePic = data.reviewedBusinessProfileRef?.profilePic?.large ?: ""
        val coverImage = data.reviewedBusinessProfileRef?.coverImage ?: ""
        val businessType = data.reviewedBusinessProfileRef?.businessTypeRef?.name
        val businessSubType = data.reviewedBusinessProfileRef?.businessSubtypeRef?.name
        val city = data.reviewedBusinessProfileRef?.address?.city
        val state = data.reviewedBusinessProfileRef?.address?.state

        // Set user and business info
        binding.userNameTv.text = userName
        Glide.with(this).load(userProfilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.userProfileIv)

        binding.hotelNameTv.text = businessName

        Glide.with(this).load(businessProfilePic).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        Glide.with(this).load(coverImage).placeholder(R.drawable.ic_post_placeholder).error(R.drawable.ic_post_placeholder).into(binding.imageView)



        binding.hotelTypeTv.apply {
            visibility = if (businessType.isNullOrBlank()) View.GONE else View.VISIBLE
            text = when {
                businessType.isNullOrBlank() -> ""
                businessSubType.isNullOrBlank() -> businessType
                else -> "$businessType - $businessSubType"
            }
        }

        binding.location.text = "$city, $state"

        // Like, Comment, and Share Counts
        binding.likeTvReview.text = formatCount(data.likes ?: 0)
        binding.commentTvReview.text = formatCount(data.comments ?: 0)
        binding.shareTvReview.text = formatCount(data.shared ?: 0)

        val viewsCount = data.views ?: 0
        if (viewsCount > 0){
            binding.reviewsViewsBtn.visibility = View.VISIBLE
            binding.reviewsViewsTv.text = formatCount(viewsCount)
        }

        // Content and Date
        binding.contentTv.text = data.content
        binding.daysTv.text = calculateDaysAgo(createdAt,this)

        // Post interaction states
        var isPostSaved = data.savedByMe ?: false
        var isPostLiked = data.likedByMe ?: false
        updateSaveBtn(isPostSaved, binding.saveIvReview)
        updateLikeBtn(isPostLiked, binding.likeIvReview)

        // Save, Like, and Comment buttons actions
        val postId = data.Id.toString()
        binding.saveIvReview.setOnClickListener {
            savePost(postId)
            isPostSaved = !isPostSaved
            updateSaveBtn(isPostSaved, binding.saveIvReview)
        }
        binding.likeBtnReview.setOnClickListener {
            likePost(postId)
            isPostLiked = !isPostLiked
            binding.likeIvReview.setImageResource(if (isPostLiked) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon)
            val likes = data.likes?.let { it + if (isPostLiked) 1 else -1 } ?: 0
            binding.likeTvReview.text = formatCount(likes)
            data.likes = data.likes?.plus(if (isPostLiked) 1 else -1)
        }

        val commentCount = data.comments ?: 0



        binding.commentBtnReview.setOnClickListener {
//            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
//                arguments = Bundle().apply {
//                    putString("POST_ID", postId)
//                    putInt("COMMENTS_COUNT", data.comments ?: 0)
//                }
//                onCommentSent = { comment ->
//                    if (comment.isNotEmpty()) {
//                        binding.commentTvReview.text = (data.comments ?: 0 + 1).toString()
//                    }
//                }
//            }
//            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        // User layout click action
        binding.userLayoutReview.setOnClickListener {
            if (publicUserID.isNotEmpty()){
                Toast.makeText(this,getString(R.string.profile_not_associated_thm), Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(data.postedBy?.Id ?: "")
            }
        }


        // User profile click
        binding.businessLayout.setOnClickListener {
            if (googleReviewedBusiness.isNotEmpty()){
                Toast.makeText(this,getString(R.string.profile_not_associated_thm), Toast.LENGTH_SHORT).show()
            }else{
                moveToBusinessProfileDetailsActivity(data.reviewedBusinessProfileRef?.userId ?: "")
            }

        }

        // Share button click
        binding.shareBtnReview.setOnClickListener {
            this.sharePostWithDeepLink(postId, ownerUserId)
        }

        binding.menuBtnReview.setOnClickListener { view ->
            showMenuDialog(view, postId, data, canShareToStory = false)
        }

    }

    private fun savePost(id: String) {
        individualViewModal.savePost(id)
    }
    private fun likePost(id: String) {
        individualViewModal.likePost(id)
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

    private fun generateTaggedRefString(taggedRef: List<TaggedRef>): String {
        return when (taggedRef.size) {
            0 -> "" // No tagged references
            1 -> taggedRef[0].name ?: "" // Single tagged reference
            2 -> "${taggedRef[0].name} and ${taggedRef[1].name}" // Two tagged references
            else -> "${taggedRef[0].name} and ${taggedRef.size - 1} others" // More than two tagged references
        }
    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(this, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
    }

    private fun showMenuDialog(view: View?, postId: String, post: Data? = null, canShareToStory: Boolean = false) {
        // Inflate the dropdown menu layout
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.single_post_menu_dropdown_item, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
//        val blockBtn: TextView = dropdownView.findViewById(R.id.blockBtn)
        val reportBtn: TextView? = dropdownView.findViewById(R.id.reportBtn)
        val addToStoryBtn: TextView? = dropdownView.findViewById(R.id.addToStoryBtn)
//        val shareBtn: TextView = dropdownView.findViewById(R.id.shareBtn)


        reportBtn?.setOnClickListener {
            reportPost(postId)
            popupWindow.dismiss()
        }

        val shouldShowAddToStory = canShareToStory && isStoryShareEligible(post)
        addToStoryBtn?.visibility = if (shouldShowAddToStory) View.VISIBLE else View.GONE
        addToStoryBtn?.setOnClickListener {
            publishPostToStory(postId, post)
            popupWindow.dismiss()
        }


        // Set the background drawable to make the popup more visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(view)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }

    private fun isStoryShareEligible(post: Data?): Boolean {
        if (post == null) return false
        if (isMyPost(post)) return false
        if (!hasShareableMedia(post)) return false
        val isFollowing = post.postedBy?.isFollowedByMe == true ||
                post.postedBy?.businessProfileRef?.isFollowedByMe == true
        return isFollowing
    }

    private fun hasShareableMedia(post: Data): Boolean {
        if (post.mediaRef.isEmpty()) return false
        return post.mediaRef.any { media ->
            val type = media.mediaType?.lowercase(Locale.getDefault())
            val mimeType = media.mimeType?.lowercase(Locale.getDefault())
            val isImage = type == "image" || (mimeType?.startsWith("image") == true)
            val isVideo = type == "video" || (mimeType?.startsWith("video") == true)
            val hasSource = !media.sourceUrl.isNullOrBlank()
            (isImage || isVideo) && hasSource
        }
    }

    private fun isMyPost(post: Data): Boolean {
        if (ownerUserId.isBlank()) return false
        val directOwnerId = post.userID
        val postedById = post.postedBy?.Id
        return ownerUserId.equals(directOwnerId, ignoreCase = true) ||
                ownerUserId.equals(postedById, ignoreCase = true)
    }

    private fun publishPostToStory(postId: String, post: Data?) {
        if (postId.isBlank()) return

        val tooLongSeconds = findTooLongStoryVideoSeconds(post?.mediaRef)
        if (tooLongSeconds != null) {
            Toast.makeText(this, R.string.story_video_too_long, Toast.LENGTH_SHORT).show()
            return
        }

        individualViewModal.publishPostToStory(postId)
    }


    private fun setDescriptionTextAndClick(
        binding: ActivityPostPreviewBinding,
        description: String,
        feeling: String,
        people: String,
        location: String,
        taggedPeople: ArrayList<TaggedRef>,
        lat: Double,
        lng: Double
    ) {
        val textColor = ContextCompat.getColor(this, R.color.blue_50)

        val bgColor = ContextCompat.getColor(this, R.color.transparent)

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
                    startActivity(intent)
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
                // Convert the taggedPeople list to JSON
                val taggedPeopleJson = Gson().toJson(taggedPeople)

                // Pass the JSON to the BottomSheetFragment
                val bottomSheetFragment = TagPeopleBottomSheetFragment.newInstance(taggedPeopleJson)
                bottomSheetFragment.show(supportFragmentManager, "TagPeopleBottomSheet")
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
                openGoogleMaps(lat, lng)
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
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

    }

}