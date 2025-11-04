package com.thehotelmedia.android.adapters.stories

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.bottomSheets.TwoVerticalOptionBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.databinding.LikesItemLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.setTopMarginIfFirstItem
import com.thehotelmedia.android.modals.storiesActions.likeStory.LikeData
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LikedStoryAdapter(
    private val context: Context,
    private val childFragmentManager: FragmentManager,
    private val individualViewModal: IndividualViewModal,
    private val coroutineScope: CoroutineScope): PagingDataAdapter<LikeData, LikedStoryAdapter.ViewHolder>(COMPARATOR)  {

    private var verticalTwoOptionBottomSheet: TwoVerticalOptionBottomSheetFragment? = null

    inner class ViewHolder(val binding: LikesItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LikesItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        // Apply padding to the first item
        binding.root.setTopMarginIfFirstItem(position, context, com.intuit.sdp.R.dimen._10sdp)

        val likes = getItem(position)
        val userId = likes?.Id ?: ""
        likes?.let { it ->
            val binding = holder.binding

            var profilePic = ""
            var name = ""
            var userName = ""
            val accountType = it.accountType?.capitalizeFirstLetter() ?: ""

            if (accountType == business_type_individual){
                profilePic = it.profilePic?.large ?: ""
                name = it.name ?: ""
                userName = it.username ?: ""
                binding.profileCardView.strokeWidth = 0
            }else{
                profilePic = it.businessProfileRef?.profilePic?.large ?: ""
                name = it.businessProfileRef?.name ?: ""
                userName = it.businessProfileRef?.username ?: ""
            }

            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
            binding.nameTv.text = name
            binding.userNameTv.text = userName

            binding.menuBtn.setOnClickListener {
                openBottomSheet(userId,userName,binding,likes)
            }



        }


        holder.itemView.setOnClickListener {
            moveToBusinessProfileDetailsActivity(userId)
        }



    }

    private fun openBottomSheet(
        userId: String,
        userName: String,
        binding: LikesItemLayoutBinding,
        views: LikeData
    ) {
        verticalTwoOptionBottomSheet = TwoVerticalOptionBottomSheetFragment.newInstance(
            title = userName,
            blockButtonText = "Block",
            viewProfileButtonText = "Remove Follower"
        ).apply {
            onBlockClick = {
                // Handle block click

                binding.root.postDelayed({
                    // Show the BottomSheetFragment
                    val bottomSheet = YesOrNoBottomSheetFragment.newInstance("${getString(R.string.do_you_really_want_to_block)} $userName?")
                    bottomSheet.onYesClicked = {
                        // Handle Yes button click
                        removeItem(views)
                        individualViewModal.blockUser(userId)
                        bottomSheet.dismiss()
                        verticalTwoOptionBottomSheet?.dismiss()
                    }
                    bottomSheet.onNoClicked = {
                        verticalTwoOptionBottomSheet?.dismiss()

                    }
                    bottomSheet.show(childFragmentManager, "YesOrNoBottomSheet")
                }, 100)

            }
            onViewProfileClick = {
                // Handel Remove Follower
                binding.root.postDelayed({
                    // Show the BottomSheetFragment
                    val bottomSheet = YesOrNoBottomSheetFragment.newInstance("${getString(R.string.do_you_really_want_to_unfollow)} $userName?")
                    bottomSheet.onYesClicked = {
                        // Handle Yes button click
                        removeItem(views)
                        individualViewModal.unFollowUser(userId)
                        bottomSheet.dismiss()
                        verticalTwoOptionBottomSheet?.dismiss()
                    }
                    bottomSheet.onNoClicked = {
                        verticalTwoOptionBottomSheet?.dismiss()

                    }
                    bottomSheet.show(childFragmentManager, "YesOrNoBottomSheet")
                }, 100)
            }
            onDismissCallback = {

            }
        }
        verticalTwoOptionBottomSheet?.show(childFragmentManager, "TwoVerticalOptionBottomSheet")
    }

    private fun removeItem(item: LikeData) {
        coroutineScope.launch {
            val currentList = snapshot().items // Get the current items
            val updatedList = currentList.filter { it.Id != item.Id } // Remove the clicked item

            // Convert the updated list to PagingData and submit it
            val newPagingData = PagingData.from(updatedList)
            submitData(newPagingData) // Submit the new data
            notifyDataSetChanged()
        }
    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
        val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
        intent.putExtra("USER_ID", userId)
        context.startActivity(intent)
    }


    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<LikeData>() {
            override fun areItemsTheSame(oldItem: LikeData, newItem: LikeData): Boolean {
                return oldItem.Id == newItem.Id
            }

            override fun areContentsTheSame(oldItem: LikeData, newItem: LikeData): Boolean {
                return oldItem == newItem
            }
        }
    }

}