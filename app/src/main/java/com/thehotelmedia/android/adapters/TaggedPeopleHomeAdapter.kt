package com.thehotelmedia.android.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.databinding.LikesItemLayoutBinding
import com.thehotelmedia.android.modals.feeds.feed.TaggedRef


class TaggedPeopleHomeAdapter(
    private val context: Context,
    private val items: List<TaggedRef>,
    private val ownerUserId: String
) : RecyclerView.Adapter<TaggedPeopleHomeAdapter.ViewHolder>() {

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

        val view = items[position]
        val userId = view.Id ?: ""

        var profilePic = ""
        var name = ""
        var userName = ""
        val accountType = view.accountType ?: ""

        if (accountType == "individual") {
            profilePic = view.profilePic?.large ?: ""
            name = view.name ?: ""
            userName = view.username ?: ""
            binding.profileCardView.strokeWidth = 0
        } else {
            profilePic = view.businessProfileRef?.profilePic?.large ?: ""
            name = view.businessProfileRef?.name ?: ""
            userName = view.username ?: ""
        }

        Glide.with(context)
            .load(profilePic)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.profileIv)

        binding.nameTv.text = name
        binding.userNameTv.text = userName
        binding.likeIv.visibility = View.GONE

        holder.itemView.setOnClickListener {
            moveToBusinessProfileDetailsActivity(userId)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            intent.putExtra("USER_ID", userId)
            context.startActivity(intent)
    }
}