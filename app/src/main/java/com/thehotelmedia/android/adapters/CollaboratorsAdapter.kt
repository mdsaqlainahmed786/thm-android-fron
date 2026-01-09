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
import com.thehotelmedia.android.modals.feeds.feed.Collaborator

class CollaboratorsAdapter(
    private val context: Context,
    private val items: List<Collaborator>
) : RecyclerView.Adapter<CollaboratorsAdapter.ViewHolder>() {

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

        val collaborator = items[position]
        val userId = collaborator._id ?: ""

        val profilePic = collaborator.profilePic?.large ?: ""
        val name = collaborator.name ?: ""

        Glide.with(context)
            .load(profilePic)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.profileIv)

        binding.nameTv.text = name
        binding.userNameTv.text = "" // Collaborator doesn't have username field
        binding.likeIv.visibility = View.GONE
        binding.menuBtn.visibility = View.GONE // Hide menu button for collaborators

        holder.itemView.setOnClickListener {
            if (userId.isNotEmpty()) {
                moveToBusinessProfileDetailsActivity(userId)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun moveToBusinessProfileDetailsActivity(userId: String) {
        val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
        intent.putExtra("USER_ID", userId)
        context.startActivity(intent)
    }
}






