package com.thehotelmedia.android.adapters.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
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
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.OFFICIAL
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.databinding.ProfileListItemsLayoutBinding
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.modals.search.SearchData
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class SearchProfileAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val childFragmentManager: FragmentManager,
    private val ownerUserId: String,
)  : PagingDataAdapter<SearchData, SearchProfileAdapter.ViewHolder>(
    SearchProfileDiffCallback()
)     {
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
            var name = ""
            var description = ""
            var profilePic = ""
            val role = it.role ?: ""
            if (role == OFFICIAL){
                binding.verifyIcon.visibility = View.VISIBLE
            }else{
                binding.verifyIcon.visibility = View.GONE
            }

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
                Glide.with(context)
                    .load(businessTypeIcon)
                    .placeholder(R.drawable.ic_standard)
                    .transform(ColorFilterTransformation(ContextCompat.getColor(context, R.color.icon_color_60)))
                    .into(binding.businessTypeIcon)

                binding.typeLayout.visibility = ViewGroup.VISIBLE
                binding.profileCardView.strokeColor = ContextCompat.getColor(context, R.color.post_stroke)
                binding.chatLayout.setBackgroundResource(R.drawable.background_profile)
            }
            val id = it.Id.toString()

            binding.nameTv.text = name
            binding.locationTv.text = description

            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)



            holder.itemView.setOnClickListener {
                moveToBusinessProfileDetailsActivity(id)
            }



            binding.menuBtn.setOnClickListener { view ->
                showMenuDialog(view,id)
            }

        }

    }

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
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

    class SearchProfileDiffCallback : DiffUtil.ItemCallback<SearchData>() {
        override fun areItemsTheSame(oldItem: SearchData, newItem: SearchData): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: SearchData, newItem: SearchData): Boolean {
            return oldItem == newItem
        }
    }

    private fun showMenuDialog(view: View?, userId: String) {
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
            reportUser(userId)
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


    private fun reportUser(userId: String) {

        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", userId)
                putString("TYPE", "user")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportUser(userId,selectedReason)
            }
        }
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)

    }

}