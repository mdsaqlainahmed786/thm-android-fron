package com.thehotelmedia.android.adapters.search

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaPagerAdapter
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.SharePostBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.TagPeopleBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.databinding.PostItemsLayoutBinding
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.openGoogleMaps
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.extensions.updateTextWithAnimation
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef
import com.thehotelmedia.android.modals.search.SearchData
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


class SearchPostAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val childFragmentManager: FragmentManager,
    private val ownerUserId: String,
)  : PagingDataAdapter<SearchData, SearchPostAdapter.ViewHolder>(SearchPostDiffCallback())  {
    private var activePosition = 0 // No active position initially
    private var dotsIndicator: SpringDotsIndicator? = null
    private lateinit var mediaPagerAdapter : MediaPagerAdapter
    
    // Track which posts have expanded text (read more clicked)
    private val expandedPosts = mutableSetOf<String>()

    inner class ViewHolder(val binding: PostItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int,) {

        val isActive = position == activePosition

        val post = getItem(position)
        post?.let {
            val binding = holder.binding
            val mediaList = post.mediaRef
            val postId = post.Id.toString()
            dotsIndicator = binding.indicatorLayout.apply {
                dotsClickable = false
                setDotIndicatorColor(ContextCompat.getColor(context, R.color.blue))
                setStrokeDotsIndicatorColor(ContextCompat.getColor(context, R.color.grey))
            }

            var isPostSaved = post.savedByMe ?: false
            var isPostLiked = post.likedByMe ?: false

            // Like, Comment, and Share counts
            var likeCount = post.likes ?: 0
            var commentCount = post.comments ?: 0
            val shareCount = post.shared ?: 0 // Replace with actual share count if available
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
                dotsIndicator?.attachTo(binding.viewPager)
            }else{
                binding.mediaLayout.visibility = View.GONE
            }

//            mediaPagerAdapter = MediaPagerAdapter(context, mediaList, isActive)
//            binding.viewPager.adapter = mediaPagerAdapter
//            dotsIndicator?.attachTo(binding.viewPager)

            binding.indicatorLayout.visibility = if (mediaList.size == 1) View.GONE else View.VISIBLE

            // Handle postedBy and accountType details
            val postedBy = post.postedBy
            val accountType = postedBy?.accountType
            val userId = postedBy?.Id.toString()

            val createdAt = post.createdAt ?: ""
            val formatedCreatedAt = calculateDaysAgo(createdAt,context)

            var name = ""
            var profilePic = ""

            if (accountType == "individual") {
                binding.businessTypeLayout.visibility = View.GONE
                name = postedBy.name ?: ""
                profilePic = postedBy.profilePic?.medium ?: ""
                val newStrokeColor = ContextCompat.getColor(context, R.color.transparent) // Replace with your color resource
                binding.profileCv.strokeColor = newStrokeColor

                binding.locationTv.text = formatedCreatedAt
            } else {
                val businessType = postedBy?.businessProfileRef?.businessTypeRef?.name ?: ""
                val businessSubType = postedBy?.businessProfileRef?.businessSubtypeRef?.name ?: ""
                val averageRating = postedBy?.businessProfileRef?.businessRating ?: 0.0
                val newStrokeColor = ContextCompat.getColor(context, R.color.post_stroke) // Replace with your color resource
                binding.profileCv.strokeColor = newStrokeColor
                binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
                binding.typeTv.text = "$businessType - $businessSubType"
                binding.businessTypeLayout.visibility = View.VISIBLE
                name = postedBy?.businessProfileRef?.name ?: ""
                profilePic = postedBy?.businessProfileRef?.profilePic?.medium ?: ""



                val address = postedBy?.businessProfileRef?.address
                val state = address?.state
                val country = address?.country
                val locationText = "$state, $country"
                // Update the TextView with animation
                binding.locationTv.updateTextWithAnimation(isActive, locationText, formatedCreatedAt)
            }

            // Set user name and profile picture
            binding.nameTv.text = name
            Glide.with(context)
                .load(profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(binding.profileIv)



            // Post content
            val content = post.content.orEmpty().trim()
            val feelings = post.feelings.orEmpty().trim()
            val location = post.location?.placeName.orEmpty().trim()
            val lat  = post.location?.lat ?: 0.00
            val lng  = post.location?.lng ?: 0.00
            val taggedRef = post.taggedRef

            if (content.isNotEmpty() || feelings.isNotEmpty() || location.isNotEmpty() || taggedRef.isNotEmpty()){
                CoroutineScope(Dispatchers.IO).launch {
                    val taggedRefString = generateTaggedRefString(taggedRef)
                    withContext(Dispatchers.Main) {
                        setDescriptionTextAndClick(binding, content, feelings, taggedRefString, location,taggedRef,lat,lng, postId)
                    }
                }
                binding.allDescriptionTv.visibility =View.VISIBLE
            }else{
                binding.allDescriptionTv.visibility =View.GONE
            }


            // Set the initial icon based on the saved state
            updateSaveBtn(isPostSaved,binding.saveIv)
            updateLikeBtn(isPostLiked,binding.likeIv)
            val id = post.Id.toString()
            binding.saveIv.setOnClickListener {
                savePost(id)  // Assuming savePost updates the saved state
                // Toggle the saved state
                isPostSaved = !isPostSaved  // Flip the state
                updateSaveBtn(isPostSaved,binding.saveIv)
            }

            binding.likeBtn.setOnClickListener {
                likePost(id)  // Assuming savePost updates the saved state
                // Toggle the saved state
                isPostLiked = !isPostLiked  // Flip the state
                if (isPostLiked) {
                    binding.likeIv.setImageResource(R.drawable.ic_like_icon)
                    likeCount++
                } else {
                    binding.likeIv.setImageResource(R.drawable.ic_unlike_icon)
                    likeCount--
                }
                binding.likeTv.text = formatCount(likeCount)
                post.likes = likeCount
                post.likedByMe = isPostLiked
            }

            binding.commentBtn.setOnClickListener {
                val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                    arguments = Bundle().apply {
                        putString("POST_ID", id)
                        putInt("COMMENTS_COUNT", commentCount)
                    }
                }
                // Set the lambda function to handle the comment sent from the bottom sheet
                bottomSheetFragment.onCommentSent = { comment ->
                    println("Comment received from bottom sheet: $comment")
                    if (comment.isNotEmpty()) {
                        commentCount++
                        binding.commentTv.text = formatCount(commentCount)


                        notifyDataSetChanged()
                    }
                    // Handle the comment here, update UI, etc.
                }
//                bottomSheetFragment.show(context.parentFragmentManager, bottomSheetFragment.tag)

                if (context is Fragment) {
                    val fragment = context as Fragment
                    fragment.parentFragmentManager.let {
                        bottomSheetFragment.show(it, bottomSheetFragment.tag)
                    }
                } else if (context is AppCompatActivity) {
                    val activity = context as AppCompatActivity
                    activity.supportFragmentManager.let {
                        bottomSheetFragment.show(it, bottomSheetFragment.tag)
                    }
                }
            }

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
                        mediaId = selectedMedia?.Id
                    ).show(childFragmentManager, SharePostBottomSheetFragment::class.java.simpleName)
                } else {
                    context.sharePostWithDeepLink(postId, ownerUserId)
                }
            }

            binding.menuBtn.setOnClickListener { view ->
                showMenuDialog(view,id)
            }
        }



    }
    private fun likePost(id: String) {
        individualViewModal.likePost(id)
    }
    private fun savePost(id: String) {
        individualViewModal.savePost(id)
    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }


    private fun generateTaggedRefString(taggedRef: List<TaggedRef>): String {
        return when (taggedRef.size) {
            0 -> "" // No tagged references
            1 -> taggedRef[0].name ?: "" // Single tagged reference
            2 -> "${taggedRef[0].name} and ${taggedRef[1].name}" // Two tagged references
            else -> "${taggedRef[0].name} and ${taggedRef.size - 1} others" // More than two tagged references
        }
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
    private fun setDescriptionTextAndClick(binding: PostItemsLayoutBinding,description: String,feeling: String,people: String,location: String,taggedPeople: ArrayList<TaggedRef>,lat: Double, lng: Double, postId: String) {
        val textColor = ContextCompat.getColor(context, R.color.blue_50)
        val bgColor = ContextCompat.getColor(context, R.color.transparent)
        val readMoreColor = ContextCompat.getColor(context, R.color.blue_50)

        val parts = mutableListOf<String>()
        if (description.isNotEmpty()) parts.add(description)
        if (feeling.isNotEmpty()) parts.add(feeling)
        if (people.isNotEmpty()) parts.add("with $people")
        if (location.isNotEmpty()) parts.add("at $location")

        // Join the non-empty parts with a separator
        val finalText = parts.joinToString(" - ")
        val isExpanded = expandedPosts.contains(postId)
        
        // Use character-based truncation for reliability
        // Approximate 2-3 lines = ~40-50 characters (adjust based on your font size)
        val maxCharsForTruncation = 40
        val needsTruncation = finalText.length > maxCharsForTruncation
        
        val displayText = if (needsTruncation && !isExpanded) {
            // Truncate text and add ellipsis
            finalText.substring(0, maxCharsForTruncation).trimEnd() + "..."
        } else {
            finalText
        }
        
        // Set max lines based on expanded state
        binding.allDescriptionTv.maxLines = if (isExpanded) Int.MAX_VALUE else 3
        binding.allDescriptionTv.ellipsize = null
        
        val spannableString = SpannableString(displayText)
        
        // Get the start and end indices of each portion of text (from displayText, not finalText)
        val feelingStart = if (feeling.isNotEmpty()) displayText.indexOf(feeling) else -1
        val feelingEnd = if (feelingStart >= 0) feelingStart + feeling.length else -1
        val peoplesStart = if (people.isNotEmpty()) displayText.indexOf(people) else -1
        val peoplesEnd = if (peoplesStart >= 0) peoplesStart + people.length else -1
        val locationStart = if (location.isNotEmpty()) displayText.indexOf(location) else -1
        val locationEnd = if (locationStart >= 0) locationStart + location.length else -1

        // Use the default Kotlin URL regex
        val matcher = Constants.URL_PATTERN_MATCHER.matcher(displayText)

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
        if (feelingStart >= 0 && feelingEnd > feelingStart) {
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
        }

        // Set color and clickable span for peoples
        if (peoplesStart >= 0 && peoplesEnd > peoplesStart) {
            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
//                Toast.makeText(context, "peoples", Toast.LENGTH_SHORT).show()
                    // Convert the taggedPeople list to JSON
                    val taggedPeopleJson = Gson().toJson(taggedPeople)

                    // Pass the JSON to the BottomSheetFragment
                    val bottomSheetFragment = TagPeopleBottomSheetFragment.newInstance(taggedPeopleJson)
                    bottomSheetFragment.show(childFragmentManager, "TagPeopleBottomSheet")
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = textColor // Set text color
                    ds.isUnderlineText = false // Remove underline
                    ds.bgColor = bgColor // Ensure background color is transparent
                }
            }, peoplesStart, peoplesEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Set color and clickable span for location
        if (locationStart >= 0 && locationEnd > locationStart) {
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
        }
        
        // Add "Read more" / "Read less" functionality
        if (needsTruncation) {
            val readMoreText = if (isExpanded) " Read less" else " Read more"
            val fullSpannableText = SpannableString(displayText + readMoreText)
            
            // Copy all existing spans to the new spannable
            val spans = spannableString.getSpans(0, spannableString.length, Any::class.java)
            spans.forEach { span ->
                val start = spannableString.getSpanStart(span)
                val end = spannableString.getSpanEnd(span)
                val flags = spannableString.getSpanFlags(span)
                fullSpannableText.setSpan(span, start, end, flags)
            }
            
            // Add clickable span for "Read more" / "Read less"
            val readMoreStart = displayText.length
            val readMoreEnd = fullSpannableText.length
            
            fullSpannableText.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Toggle expanded state
                    if (isExpanded) {
                        expandedPosts.remove(postId)
                    } else {
                        expandedPosts.add(postId)
                    }
                    // Find the position and notify item changed
                    var foundPosition = -1
                    for (i in 0 until itemCount) {
                        val item = getItem(i)
                        if (item?.Id?.toString() == postId) {
                            foundPosition = i
                            break
                        }
                    }
                    if (foundPosition >= 0) {
                        notifyItemChanged(foundPosition)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = readMoreColor // Set the link color
                    ds.isUnderlineText = false // No underline
                    ds.bgColor = bgColor // Ensure background color is transparent
                }
            }, readMoreStart, readMoreEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            binding.allDescriptionTv.text = fullSpannableText
        } else {
            binding.allDescriptionTv.text = spannableString
        }
        
        // Set max lines after setting text
        binding.allDescriptionTv.maxLines = if (isExpanded) Int.MAX_VALUE else 3
        
        binding.allDescriptionTv.setBackgroundResource(R.drawable.transparent_background)
        binding.allDescriptionTv.movementMethod = LinkMovementMethod.getInstance()
    }

    class SearchPostDiffCallback : DiffUtil.ItemCallback<SearchData>() {
        override fun areItemsTheSame(oldItem: SearchData, newItem: SearchData): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: SearchData, newItem: SearchData): Boolean {
            return oldItem == newItem
        }
    }

    private fun showMenuDialog(view: View?, postId: String) {
        // Inflate the dropdown menu layout
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
        val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
//        val shareBtn: TextView = dropdownView.findViewById(R.id.shareBtn)



        reportBtn.setOnClickListener {
            reportPost(postId)
            popupWindow.dismiss()
        }


        // Set the background drawable to make the popup more visually appealing
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
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)

    }

    fun setActivePosition(position: Int) {
        activePosition = position
    }


}