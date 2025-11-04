package com.thehotelmedia.android.adapters

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.activity.JobDetailActivity
import com.thehotelmedia.android.activity.PostPreviewActivity
import com.thehotelmedia.android.activity.ViewEventDetailsActivity
import com.thehotelmedia.android.activity.userTypes.individual.BookTableBanquetActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.BookingSummaryActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.PostJobActivity
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.databinding.NotificationItemsLayoutBinding
import com.thehotelmedia.android.extensions.calculateDaysAgoInSmall
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.notifications.NotificationData

class NotificationAdapter(
    private val context: Context,
    private val onDeclineClick: (String, Int) -> Unit,
    private val onAcceptClick: (String?) -> Unit,
    private val onFollowClick: (String?) -> Unit,
    private val ownerUserId: String
) : PagingDataAdapter<NotificationData, NotificationAdapter.ViewHolder>(NotificationDiffCallback())   {


    private val notifications = mutableListOf<NotificationData>()

    inner class ViewHolder(val binding: NotificationItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = NotificationItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }




    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)
        notification?.let { it ->

            val binding = holder.binding
            // Set title and time
            val id = it.Id.toString()
            val jobId = it.metadata?.jobID ?: ""
            val bookingID = it.metadata?.bookingID ?: ""
            val bookingType = it.metadata?.type ?: ""
            val postId = it.metadata?.postID.toString()
            val metaDataPostType = it.metadata?.postType ?: ""
            val userID = it.userID.toString()
            val title = it.description.toString()
            val connectionId = it.metadata?.connectionID ?: ""
            val type = it.type.toString()
            val time = calculateDaysAgoInSmall(it.createdAt.toString())
            val words = title.split(" ")
            // Check if the length exceeds 100 characters
            val truncatedTitle = if (title.length > 60) {
                title.take(60) + "..."
            } else {
                title
            }
            val truncatedTime = if (title.length > 60) {
                "more $time"
            } else {
                time
            }

            val isRequested = it.isRequested ?: false
            val isConnected = it.isConnected ?: false


            if (type == "follow-request"){
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.VISIBLE
                binding.followingBtn.visibility = View.GONE
            }else if(type == "accept-follow-request"){
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.VISIBLE
            }else if(type == "following"){

                if (!isRequested && !isConnected){
                binding.followBtn.visibility = View.VISIBLE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.GONE
                }else if (isRequested && !isConnected){
                    binding.followBtn.visibility = View.GONE
                    binding.acceptDeclineLayout.visibility = View.GONE
                    binding.followingBtn.visibility = View.VISIBLE
                    binding.followingTv.text = "Requested"
                }else{
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.VISIBLE
                }

//                binding.followBtn.visibility = View.VISIBLE
//                binding.acceptDeclineLayout.visibility = View.GONE
//                binding.followingBtn.visibility = View.GONE
            }else{
                binding.followBtn.visibility = View.GONE
                binding.acceptDeclineLayout.visibility = View.GONE
                binding.followingBtn.visibility = View.GONE
            }

            // Handle button clicks using lambda functions
            binding.declineBtn.setOnClickListener {
                onDeclineClick(connectionId, position)
            }
            binding.acceptBtn.setOnClickListener {
                onAcceptClick(connectionId)
            }
            binding.followBtn.setOnClickListener {
                onFollowClick(connectionId)
            }

            val accountType = it.usersRef?.accountType?.capitalizeFirstLetter() ?: ""
            var profilePic = ""
            if (accountType == business_type_individual){
                profilePic = it.usersRef?.profilePic?.medium.toString()
                binding.profileCardView.strokeColor = ContextCompat.getColor(context, R.color.transparent)
            }else{
                profilePic = it.usersRef?.businessProfileRef?.profilePic?.medium.toString()
                binding.profileCardView.strokeColor = ContextCompat.getColor(context, R.color.post_stroke)
            }

            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profilePic)

            // Create a SpannableString with both title and time
            val fullText = "$truncatedTitle $truncatedTime"
            val spannable = SpannableString(fullText)

            // Get colors
            val whiteColor = ContextCompat.getColor(context, R.color.text_color)
            val greyColor = ContextCompat.getColor(context, R.color.text_color_50)

            // Apply white color to the title
            spannable.setSpan(
                ForegroundColorSpan(whiteColor),
                0,
                truncatedTitle.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Apply grey color to the time
            spannable.setSpan(
                ForegroundColorSpan(greyColor),
                truncatedTitle.length + 1,  // Start after the title and a space
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Set the spannable text to the TextView
            binding.descriptionTv.text = spannable


            binding.root.setOnClickListener {
                when (type) {
                    "comment" -> {
                        if (metaDataPostType == "event"){
                            moveToEventDetailsActivity(postId)
                        }else{
                            moveToPostPreviewScreen(postId)
                        }
//                        moveToPostPreviewScreen(postId)
                    }
                    "like-post" -> {
                        moveToPostPreviewScreen(postId)
                    }
                    "reply" -> {
                        moveToPostPreviewScreen(postId)
                    }
                    "like-comment" -> {
                        moveToBusinessProfileDetailsActivity(userID)
                    }
                    "follow-request" -> {
                        moveToBusinessProfileDetailsActivity(userID)
                    }
                    "accept-follow-request" -> {
                        moveToBusinessProfileDetailsActivity(userID)
                    }
                    "following" -> {
                        moveToBusinessProfileDetailsActivity(userID)
                    }
                    "tagged" -> {
                        moveToPostPreviewScreen(postId)
                    }
                    "event-join" -> {
                        moveToEventDetailsActivity(postId)
                    }
                    "like-a-story" -> {
                        moveToBusinessProfileDetailsActivity(userID)
                    }
                    "job" -> {
                        val intent = Intent(context, JobDetailActivity::class.java)
                        intent.putExtra("JOB_ID", jobId)
                        context.startActivity(intent)
                    }
                    "booking" -> {

                        when (bookingType) {
                            "book-banquet" -> {
                                val intent = Intent(context, BookTableBanquetActivity::class.java)
                                intent.putExtra("BOOKING_ID", bookingID)
                                intent.putExtra("BOOKING_TYPE", bookingType)
                                intent.putExtra("FROM", "NOTIFICATION")
                                context.startActivity(intent)
                            }
                            "book-table" -> {
                                val intent = Intent(context, BookTableBanquetActivity::class.java)
                                intent.putExtra("BOOKING_ID", bookingID)
                                intent.putExtra("BOOKING_TYPE", bookingType)
                                intent.putExtra("FROM", "NOTIFICATION")
                                context.startActivity(intent)
                            }
                            else -> {
                                val intent = Intent(context, BookingSummaryActivity::class.java)
                                intent.putExtra("BOOKING_ID", bookingID)
                                intent.putExtra("FROM", "NOTIFICATION")
                                context.startActivity(intent)
                            }
                        }




                    }
                }
//            val intent = Intent(this, JobDetailActivity::class.java)
//                startActivity(intent)


//                if(postId.isNotEmpty()){
//                    moveToPostPreviewScreen(postId)
//                }else if (userID.isNotEmpty()){
//                    moveToBusinessProfileDetailsActivity(userID)
//                }

            }

        }



    }

    private fun moveToPostPreviewScreen(postId: String) {
        val intent = Intent(context, PostPreviewActivity::class.java)
        intent.putExtra("FROM", "Notification")
        intent.putExtra("POST_ID", postId)
        context.startActivity(intent)
    }

//    private fun moveToProfileDetailsActivity(userId: String) {
//        println("af;ljasklfjakl   $userId")
//        val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
//        intent.putExtra("USER_ID", userId)
//        context.startActivity(intent)
//    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }
    private fun moveToEventDetailsActivity(postId: String) {
        val intent = Intent(context, ViewEventDetailsActivity::class.java).apply {
            putExtra("POST_ID", postId)
        }
        context.startActivity(intent)
    }


    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationData>() {
        override fun areItemsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem == newItem
        }
    }





}

