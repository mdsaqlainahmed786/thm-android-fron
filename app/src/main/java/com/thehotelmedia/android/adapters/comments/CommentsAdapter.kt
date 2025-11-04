package com.thehotelmedia.android.adapters.comments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.databinding.CommentsItemBinding
import com.thehotelmedia.android.extensions.calculateDaysAgo
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.feeds.getComments.Data
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class CommentsAdapter(
    private val context: Context,
    private val onItemClick: (String, String, String) -> Unit,
    private val individualViewModal: IndividualViewModal,
    private val childFragmentManager: FragmentManager,
    private val ownerUserId: String
)
    : PagingDataAdapter<Data, CommentsAdapter.ViewHolder>(COMPARATOR) {

    inner class ViewHolder(val binding: CommentsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Data) {
            val id = item.Id ?: ""
            val userId = item.userID ?: ""
            val postId = item.postID ?: ""
            val message = item.message
            val createdAt = item.createdAt.toString()
            var likeCount = item.likes ?: 0
            var isLiked = item.likedByMe ?: false

            val replyRef = item.repliesRef
            if (replyRef.isNotEmpty()){
                binding.innerCommentRv.visibility = View.GONE
                binding.showHideReplyBtn.visibility = View.VISIBLE
                binding.showHideReplyBtn.text = "View Replies"
                val innerCommentAdapter = InnerCommentAdapter(context,replyRef, individualViewModal,childFragmentManager,ownerUserId)
                binding.innerCommentRv.adapter = innerCommentAdapter
            }else{
                binding.innerCommentRv.visibility = View.GONE
                binding.showHideReplyBtn.visibility = View.GONE
            }


            val accountType = item.commentedBy?.accountType.toString().capitalizeFirstLetter()
            var name = ""
            var profilePic = ""
            if (accountType == business_type_individual){
                name = item.commentedBy?.name ?: ""
                profilePic = item.commentedBy?.profilePic?.medium ?: ""
            }else{
                name = item.commentedBy?.businessProfileRef?.name.toString()
                profilePic = item.commentedBy?.businessProfileRef?.profilePic?.medium.toString()
            }

            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.imageView)
            binding.userNameTv.text = name
            binding.commentTv.text = message
            binding.timeTv.text = "(${calculateDaysAgo(createdAt,context)})"
            binding.likeTv.text = likeCount.toString()

            if (isLiked) {
                binding.likeIv.setImageResource(R.drawable.ic_like_icon)
            } else {
                binding.likeIv.setImageResource(R.drawable.ic_unlike_icon_60)
            }
            binding.imageView.setOnClickListener {
                moveToBusinessProfileDetailsActivity(userId)
            }

            binding.likeBtn.setOnClickListener {
                likeComment(id)  // Assuming savePost updates the saved state
                // Toggle the saved state
                isLiked = !isLiked  // Flip the state
                if (isLiked) {
                    binding.likeIv.setImageResource(R.drawable.ic_like_icon)
                    likeCount++
                } else {
                    binding.likeIv.setImageResource(R.drawable.ic_unlike_icon_60)
                    likeCount--
                }
                binding.likeTv.text = likeCount.toString()
            }


            binding.replyBtn.setOnClickListener {
                // Pass the id and name to the lambda function
                    onItemClick(id, name,profilePic)
            }

            binding.showHideReplyBtn.setOnClickListener {
                if (binding.innerCommentRv.visibility == View.GONE) {
                    // Show the replies and change button text to "Hide Replies"
                    binding.innerCommentRv.visibility = View.VISIBLE
                    binding.showHideReplyBtn.text = "Hide Replies"
                } else {
                    // Hide the replies and change button text to "Show Replies"
                    binding.innerCommentRv.visibility = View.GONE
                    binding.showHideReplyBtn.text = "View Replies"
                }
            }
            binding.menuBtn.setOnClickListener { view ->
                showMenuDialog(view,id)
            }


        }
    }

    private fun likeComment(id: String) {
        individualViewModal.likeComments(id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CommentsItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        item?.let { holder.bind(it) }
    }

    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<Data>() {
            override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
                return oldItem.Id == newItem.Id
            }

            override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
                return oldItem == newItem
            }
        }
    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }


    private fun showMenuDialog(view: View?, commentId: String) {
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
            reportComment(commentId)
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

    private fun reportComment(commentId: String) {

        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", commentId)
                putString("TYPE", "comment")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportComment(commentId,selectedReason)
            }
        }
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)

    }


}
