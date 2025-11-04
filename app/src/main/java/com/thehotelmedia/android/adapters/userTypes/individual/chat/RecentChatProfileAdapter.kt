package com.thehotelmedia.android.adapters.userTypes.individual.chat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.SocketModals.users.UsersModal
import com.thehotelmedia.android.activity.InboxScreenActivity
import com.thehotelmedia.android.databinding.ChatRecentItemsLayoutBinding

class RecentChatProfileAdapter(private val context: Context,private val  users: ArrayList<UsersModal>) : RecyclerView.Adapter<RecentChatProfileAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ChatRecentItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ChatRecentItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding

        val userId = users[position].Id ?: ""
        val name = users[position].name ?: ""
        val userName = users[position].username ?: ""
        val profilePic = users[position].profilePic?.large ?: ""
        val isOnline = users[position].isOnline ?: false

        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.imageView)
        if (isOnline){
            binding.isOnlineIcon.visibility = View.VISIBLE
        }else{
            binding.isOnlineIcon.visibility = View.GONE
        }


        holder.itemView.setOnClickListener {
            val intent = Intent(context, InboxScreenActivity::class.java)
            intent.putExtra("NAME",name)
            intent.putExtra("USER_NAME",userName)
            intent.putExtra("PROFILE_PIC",profilePic)
            intent.putExtra("USER_ID",userId)
            context.startActivity(intent)
        }


        // Set default padding
        val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = 0

        // Add padding to the first item
        if (position == 0) {
            layoutParams.marginStart = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
        }

        binding.root.layoutParams = layoutParams
    }

}