package com.thehotelmedia.android.adapters.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.databinding.ReviewItemsLayoutBinding
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.getEmojiForRating
import com.thehotelmedia.android.extensions.moveToPostPreviewScreen
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.setRatingWithStars
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.modals.search.SearchData
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class SearchReviewsAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val childFragmentManager: FragmentManager,
    private val ownerUserId: String,
)  : PagingDataAdapter<SearchData, SearchReviewsAdapter.ViewHolder>(
    SearchReviewsDiffCallback()
)     {
    inner class ViewHolder(val binding: ReviewItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ReviewItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val review = getItem(position)
        review?.let {
            val binding = holder.binding
            // Rating
            val rating = review.rating?.toInt() ?: 0
            val googleReviewedBusiness = review.googleReviewedBusiness ?: ""
            val publicUserID = review.publicUserID ?: ""

            val averageRating = review.reviewedBusinessProfileRef?.rating ?: 0.0
            binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)
            binding.ratingTV.setRatingWithStars(rating)
            binding.ratingTypeTV.text = getEmojiForRating(rating)

            // Blur views
            context.blurTheView(binding.topBlurView)
            context.blurTheView(binding.bottomBlurView)

            // Review Details
            val postedBy = review.postedBy
            val postId = review.Id ?: ""
            val createdAt = review.createdAt.toString()
            val userName = postedBy?.name.toString()
            val userProfilePic = postedBy?.profilePic?.medium.toString()

            val businessName = review.reviewedBusinessProfileRef?.name.toString()
            val businessProfilePic = review.reviewedBusinessProfileRef?.profilePic?.medium.toString()
            val coverImage = review.reviewedBusinessProfileRef?.coverImage?.toString()

            val userId = review.postedBy?.Id.toString()
            // Set user info
            binding.userNameTv.text = userName
            Glide.with(context).load(userProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.userProfileIv)

            // Set business info
            binding.hotelNameTv.text = businessName
            Glide.with(context).load(businessProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
            Glide.with(context).load(coverImage).placeholder(R.drawable.ic_post_placeholder).into(binding.imageView)

            // Set business location and type
            val businessType = review.reviewedBusinessProfileRef?.businessTypeRef?.name
            val businessSubType = review.reviewedBusinessProfileRef?.businessSubtypeRef?.name
            val city = review.reviewedBusinessProfileRef?.address?.city
            val state = review.reviewedBusinessProfileRef?.address?.state


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
            var likeCount = review.likes ?: 0
            var commentCount = review.comments ?: 0
            val shareCount = review.shared ?: 0 // Replace with actual share count if available
            binding.likeTv.text = formatCount(likeCount)
            binding.commentTv.text = formatCount(commentCount)
            binding.shareTv.text = formatCount(shareCount)

            val viewsCount = review.views ?: 0
            if (viewsCount > 0){
                binding.viewsBtn.visibility = View.VISIBLE
                binding.viewsTv.text = formatCount(viewsCount)
            }

            // Content
            val content = review.content
            binding.contentTv.text = content

            // Date calculation
            binding.daysTv.text = calculateDaysAgo(createdAt,context)


            var isPostSaved = review.savedByMe ?: false
            var isPostLiked = review.likedByMe ?: false
// Set the initial icon based on the saved state
            updateSaveBtn(isPostSaved,binding.saveIv)
            updateLikeBtn(isPostLiked,binding.likeIv)
            val id = review.Id.toString()
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
                    }
                    // Handle the comment here, update UI, etc.
                }
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

            binding.imageView.setOnClickListener {
                context.moveToPostPreviewScreen(postId)
            }



            binding.userLayout.setOnClickListener {
                if (publicUserID.isNotEmpty()){
                    Toast.makeText(context,context.getString(R.string.profile_not_associated_thm), Toast.LENGTH_SHORT).show()
                }else{
                    moveToBusinessProfileDetailsActivity(userId)
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
                context.sharePostWithDeepLink(postId, ownerUserId)
            }

            binding.menuBtn.setOnClickListener { view ->
                showMenuDialog(view,postId)
            }

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

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }



    class SearchReviewsDiffCallback : DiffUtil.ItemCallback<SearchData>() {
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


}