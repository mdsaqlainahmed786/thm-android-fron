package com.thehotelmedia.android.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.databinding.SusgestedBusinessItemLayoutBinding
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.modals.feeds.feed.SuggestionData

class SuggestedBusinessAdapter(
    private val context: Context,
    private val suggestionList: ArrayList<SuggestionData>,
    private val ownerUserId: String
): RecyclerView.Adapter<SuggestedBusinessAdapter.ViewHolder>()  {


    inner class ViewHolder(val binding: SusgestedBusinessItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SusgestedBusinessItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return suggestionList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding

        val hotelName = suggestionList[position].name
        val businessUserId = suggestionList[position].userID ?: ""
        val rating = suggestionList[position].rating ?: 0.0
        val profilePic = suggestionList[position].profilePic?.large

        val businessType = suggestionList[position].businessTypeRef?.name
        val address = suggestionList[position].address

        val city = address?.city
        val state = address?.state
        val country = address?.country



        binding.hotelNameTv.text = hotelName
        binding.businessTypesTv.text = businessType
        binding.addressTv.text = "$city, $state, $country"
        binding.ratingTv.setRatingWithStar(rating, R.drawable.ic_rating_star)
        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)


        binding.cancelBtn.setOnClickListener {
            suggestionList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, suggestionList.size)
        }

        binding.root.setOnClickListener {
            moveToBusinessProfileDetailsActivity(businessUserId)
        }


        val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        if (position == suggestionList.size - 1) {
            // Apply 12dp end margin to the last item
            layoutParams.marginEnd = 0
        } else {
            // Reset margin for other items
            layoutParams.marginEnd = (12 * context.resources.displayMetrics.density).toInt()
        }
        binding.root.layoutParams = layoutParams
    }


    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }

}