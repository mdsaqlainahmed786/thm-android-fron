package com.thehotelmedia.android.adapters.userTypes.individual.search

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.bottomSheets.BlockUserBottomSheetFragment
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.OFFICIAL
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.databinding.ProfileListItemsLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.followerFollowing.FollowFollowingData


class ProfilesAdapter(
    private val context: Context,
    private val onMenuClicked: (position: Int,id: String,view :View) -> Unit,
    private val screen: String, )
    : PagingDataAdapter<FollowFollowingData, ProfilesAdapter.ViewHolder>(ProfileDiffCallback())     {
    inner class ViewHolder(val binding: ProfileListItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfileListItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val profile = getItem(position)
        profile?.let {
            val binding = holder.binding
            val accountType = it.accountType?.capitalizeFirstLetter()
            val userId = it.Id.toString()
            val role = it.role ?: ""

            if (role == OFFICIAL){
                binding.verifyIcon.visibility = View.VISIBLE
            }else{
                binding.verifyIcon.visibility = View.GONE
            }
            var name = ""
            var description = ""
            var profilePic = ""
            if (accountType == business_type_individual){
                name = it.name.toString()
                description = it.username.toString()
                profilePic = it.profilePic?.medium.toString()
                binding.profileCardView.strokeColor = ContextCompat.getColor(context, R.color.transparent)
                binding.typeLayout.visibility = ViewGroup.GONE
                binding.chatLayout.setBackgroundResource(R.drawable.background_profile_small)
            }else{
                name = it.businessProfileRef?.name.toString()
                profilePic = it.businessProfileRef?.profilePic?.medium.toString()
                val address = it.businessProfileRef?.address
                val street =  address?.street.toString()
                val city =  address?.city.toString()
                val state =  address?.state.toString()
                val country =  address?.country.toString()
                val zipCode =  address?.zipCode.toString()
                  description = "$city, $state, $country, $zipCode"

                val businessType = it.businessProfileRef?.businessTypeRef?.name
                val businessTypeIcon = it.businessProfileRef?.businessTypeRef?.icon
                binding.businessTypeTv.text = businessType
                Glide.with(context).load(businessTypeIcon)
                    .placeholder(R.drawable.ic_hotel)
                    .transform(ColorFilterTransformation(ContextCompat.getColor(context, R.color.text_color)))
                    .into(binding.businessTypeIcon)
                binding.typeLayout.visibility = ViewGroup.VISIBLE
                binding.profileCardView.strokeColor = ContextCompat.getColor(context, R.color.post_stroke)
                binding.chatLayout.setBackgroundResource(R.drawable.background_profile)
            }

            binding.nameTv.text = name
            binding.locationTv.text = description

            Glide.with(context).load(profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(binding.profileIv)
            binding.menuBtn.setOnClickListener { view ->
                onMenuClicked(position,userId,view)
//                onProfileMenuClicked(position)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("FROM", screen)
                context.startActivity(intent)
            }

        }


//        val customDrawable = CustomShapeDrawable(
//            context = context,  // or your activity/fragment context
//        )
//
//        binding.chatLayout.background = customDrawable


    }

    private fun getFragmentManager(): FragmentManager? {
        return when (context) {
            is FragmentActivity -> context.supportFragmentManager
            is Fragment -> context.childFragmentManager
            else -> null
        }
    }

    private fun onProfileMenuClicked(position: Int) {
        val fragmentManager = getFragmentManager()
        fragmentManager?.let {
            val bottomSheetFragment = BlockUserBottomSheetFragment()
            bottomSheetFragment.show(it, bottomSheetFragment.tag)
        }
    }

    class ProfileDiffCallback : DiffUtil.ItemCallback<FollowFollowingData>() {
        override fun areItemsTheSame(oldItem: FollowFollowingData, newItem: FollowFollowingData): Boolean {
            return oldItem.Id == newItem.Id
        }
        override fun areContentsTheSame(oldItem: FollowFollowingData, newItem: FollowFollowingData): Boolean {
            return oldItem == newItem
        }
    }

}

