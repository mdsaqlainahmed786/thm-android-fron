package com.thehotelmedia.android.adapters.socket

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.SocketModals.chatScreen.Messages
import com.thehotelmedia.android.databinding.ChatListItemsLayoutBinding
import com.thehotelmedia.android.extensions.calculateDaysAgoInSmall
import com.thehotelmedia.android.extensions.censorAbusiveWords
import com.thehotelmedia.android.extensions.loadAbusiveWordsFromJson

class ShareChatListAdapter(
    private val context: Context,
    private val onChatSelected: (Messages) -> Unit
) : RecyclerView.Adapter<ShareChatListAdapter.ViewHolder>() {

    private val items: MutableList<Messages> = mutableListOf()

    inner class ViewHolder(val binding: ChatListItemsLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Messages?) {
            if (item == null) return

            val name = item.name.orEmpty()
            val profilePic = item.profilePic?.large.orEmpty()
            val unseenCount = item.unseenCount ?: 0
            val createdAt = item.createdAt.orEmpty()
            val type = item.type.orEmpty()
            val rawMessage = item.message.orEmpty()

            val messagePreview = if (type.equals("text", ignoreCase = true)) {
                rawMessage
            } else {
                type
            }

            val abusiveWords = context.loadAbusiveWordsFromJson()
            val cleanMessageText = messagePreview.censorAbusiveWords(abusiveWords)

            binding.nameTv.text = name
            binding.msgTv.text = cleanMessageText
            binding.countTv.text = unseenCount.toString()
            binding.countTv.visibility = if (unseenCount > 0) View.VISIBLE else View.GONE
            binding.timeTv.text = calculateDaysAgoInSmall(createdAt)

            Glide.with(context)
                .load(profilePic)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.profileIv)

            binding.root.setOnClickListener { onChatSelected(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ChatListItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Messages>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}

