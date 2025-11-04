package com.thehotelmedia.android.adapters

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
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.databinding.BusinessProfileItemsLayoutBinding
import com.thehotelmedia.android.modals.suggestedBusiness.SuggestionData
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class SuggestionAdapter(
    private val context: Context,
    private val individualViewModal: IndividualViewModal,
    private val supportFragmentManager: FragmentManager,
    private val ownerUserid: String
) : PagingDataAdapter<SuggestionData, SuggestionAdapter.ViewHolder>(
    SuggestionDiffCallback()
)   {

    inner class ViewHolder(val binding: BusinessProfileItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)



    class SuggestionDiffCallback : DiffUtil.ItemCallback<SuggestionData>() {
        override fun areItemsTheSame(oldItem: SuggestionData, newItem: SuggestionData): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: SuggestionData, newItem: SuggestionData): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BusinessProfileItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggestion = getItem(position)
        suggestion?.let { it ->
            val binding = holder.binding

            val userID = it.userID ?: ""
            val name = it.name
            val profilePic = it.profilePic?.large ?: ""
            binding.verifyIcon.visibility = View.GONE
//            val role = it.role ?: ""
//            if (role == OFFICIAL){
//                binding.verifyIcon.visibility = View.VISIBLE
//            }else{
//                binding.verifyIcon.visibility = View.GONE
//            }

            val businessType = it.businessTypeRef?.name.toString().trim()
            val businessTypeIcon = it.businessTypeRef?.icon.orEmpty()

            val address = it.address
            val street =  address?.street.toString()
            val city =  address?.city.toString()
            val state =  address?.state.toString()
            val country =  address?.country.toString()
            val zipCode =  address?.zipCode.toString()
            val description = "$city, $state, $country, $zipCode"



            binding.businessTypeTv.text = businessType
            binding.nameTv.text = name
            binding.locationTv.text = description
            Glide.with(context).load(businessTypeIcon)
                .placeholder(R.drawable.ic_hotel)
                .transform(ColorFilterTransformation(ContextCompat.getColor(context, R.color.icon_color_60)))
                .into(binding.businessTypeIcon)
            Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)

            holder.itemView.setOnClickListener {
                moveToBusinessProfileDetailsActivity(userID)
            }
            binding.menuBtn.setOnClickListener { view ->
                showMenuDialog(view,userID)
            }

        }
    }
    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
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
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

    }


}