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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.activity.ViewEventDetailsActivity
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.databinding.EventItemsLayoutBinding
import com.thehotelmedia.android.extensions.formatCount
import com.thehotelmedia.android.extensions.isFutureDateOrTime
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.shareEventsWithDeepLink
import com.thehotelmedia.android.modals.search.SearchData
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.text.SimpleDateFormat
import java.util.Locale


class SearchEventAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val childFragmentManager: FragmentManager,
    private val ownerUserId: String,
)  : PagingDataAdapter<SearchData, SearchEventAdapter.ViewHolder>(
    SearchEventDiffCallback()
)     {
    inner class ViewHolder(val binding: EventItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = EventItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val event = getItem(position)
        event?.let {
            val binding = holder.binding
            val averageRating = event.postedBy?.businessProfileRef?.businessRating ?: 0.0
            val shareCount = event.shared ?: 0
            binding.shareTv.text = formatCount(shareCount)
            binding.averageRatingTv.setRatingWithStar(averageRating, R.drawable.ic_rating_star)

            val postId = event.Id.toString()
            val userId = event.postedBy?.Id.toString()

            val name = event.postedBy?.businessProfileRef?.name.toString()
            binding.nameTv.text = name
            val profilePic = event.postedBy?.businessProfileRef?.profilePic?.medium.toString()
            val mediaRef = event.mediaRef
            val coverImage = if (mediaRef.isEmpty()){
                ""
            }else{
                event.mediaRef[0].sourceUrl ?: ""
            }

            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
            Glide.with(context).load(coverImage).placeholder(R.drawable.ic_post_placeholder).into(binding.coverImage)

            // Set business location and type
            val businessType = event.postedBy?.businessProfileRef?.businessTypeRef?.name
            val businessSubType = event.postedBy?.businessProfileRef?.businessSubtypeRef?.name
//            val city = event.reviewedBusinessProfileRef?.address?.city ?: ""
//            val state = event.reviewedBusinessProfileRef?.address?.state ?: ""
            binding.typeTv.text = "$businessType - $businessSubType"

            val eventName = event.name ?: ""
            val venue = event.venue ?: ""
            val dateString = event.startDate ?: ""
            val timeString = event.startTime ?: ""
            var interestedPeople = event.interestedPeople ?: 0

            val viewsCount = event.views ?: 0
            if (viewsCount > 0){
                binding.viewsBtn.visibility = View.VISIBLE
                binding.viewsTv.text = formatCount(viewsCount)
            }


            val address = event.postedBy?.businessProfileRef?.address
            val state = address?.state
            val country = address?.country
            binding.locationTv.text = "$state, $country"

            val startDate = event.startDate ?: ""
            val startTime = event.startTime ?: ""
            if (isFutureDateOrTime(startDate,startTime)) {
                binding.joiningBtn.visibility = View.VISIBLE
            } else {
                binding.joiningBtn.visibility = View.GONE
            }

            // Joining status
            var isJoined = event.imJoining ?: false
            updateJoiningBtn(isJoined, binding.joiningIv, binding.joiningTv)

            val dateInputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = dateInputFormat.parse(dateString)

            // Parse the time string into a Date object
            val timeInputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val parsedTime = timeInputFormat.parse(timeString)

            // Format the date as "Wed, 09 Oct"
            val dateOutputFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
            val formattedDate = parsedDate?.let { dateOutputFormat.format(it) } ?: ""

            // Format the time as "05:26 AM"
            val timeOutputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = parsedTime?.let { timeOutputFormat.format(it) } ?: ""


            if (interestedPeople != 0){
                binding.peopleInterestedTv.text = "$interestedPeople ${context.getString(R.string.interested_people)}"
                binding.peopleInterestedTv.visibility = View.VISIBLE
            }else{
                binding.peopleInterestedTv.visibility = View.GONE
            }
            binding.eventNameTv.text = eventName.trim()
            binding.eventVenueTv.text = venue.trim()
            binding.dateTimeTv.text = "$formattedDate at $formattedTime"

            if (venue.isEmpty()){
                binding.eventVenueTv.visibility = View.GONE
            }

            // Bind event data
//            binding.eventTextView.text = event.content


            var isPostSaved = event.savedByMe ?: false
// Set the initial icon based on the saved state
            updateSaveBtn(isPostSaved,binding.saveIv)
            val id = event.Id.toString()
            binding.saveIv.setOnClickListener {
                savePost(id)  // Assuming savePost updates the saved state
                // Toggle the saved state
                isPostSaved = !isPostSaved  // Flip the state
                updateSaveBtn(isPostSaved,binding.saveIv)
            }


            binding.userLayout.setOnClickListener {
                moveToBusinessProfileDetailsActivity(userId)
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

            // Share button click
            binding.shareBtn.setOnClickListener {
                context.shareEventsWithDeepLink(postId,ownerUserId)
            }

            binding.menuBtn.setOnClickListener { view ->
                showMenuDialog(view,id)
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
                bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
            }


            binding.coverImage.setOnClickListener {
                val intent = Intent(context, ViewEventDetailsActivity::class.java).apply {
                    putExtra("POST_ID", postId)
                }
                context.startActivity(intent)
            }


        }
    }
    private fun savePost(id: String) {
        individualViewModal.savePost(id)
    }
    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }

    private fun updateSaveBtn(postSaved: Boolean, saveIv: ImageView) {
        if (postSaved) {
            saveIv.setImageResource(R.drawable.ic_save_icon)
        } else {
            saveIv.setImageResource(R.drawable.ic_unsave_icon)
        }
    }

    private fun updateJoiningBtn(postSaved: Boolean, joiningIv: ImageView, joiningTv: TextView) {
        if (postSaved) {
            joiningIv.setImageResource(R.drawable.ic_filled_star_blue)
            joiningTv.text = "Joined"
        } else {
            joiningIv.setImageResource(R.drawable.ic_outline_star_white)
            joiningTv.text = "Joining ?"
        }
    }

    private fun joinEvent(id: String) {
        individualViewModal.joinEvent(id)
    }




    class SearchEventDiffCallback : DiffUtil.ItemCallback<SearchData>() {
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