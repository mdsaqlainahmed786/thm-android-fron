package com.thehotelmedia.android.adapters.userTypes.individual.chat

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.SocketModals.chatScreen.Messages
import com.thehotelmedia.android.activity.InboxScreenActivity
import com.thehotelmedia.android.databinding.ChatListItemsLayoutBinding
import com.thehotelmedia.android.extensions.calculateDaysAgoInSmall
import com.thehotelmedia.android.extensions.censorAbusiveWords
import com.thehotelmedia.android.extensions.loadAbusiveWordsFromJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatListAdapter(private val context: Context) : PagingDataAdapter<Messages, ChatListAdapter.ViewHolder>(MessagesComparator)  {
    inner class ViewHolder(val binding: ChatListItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ChatListItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

//    override fun getItemCount(): Int {
//        return messagesList?.size ?: 3
//    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val chat = getItem(position)

        var message = ""
        val userId = chat?.lookupID ?: ""
        val name = chat?.name ?: ""
        val type = chat?.type ?: ""
        val userName = chat?.username ?: ""

        val profilePic = chat?.profilePic?.large ?: ""
        val unseenCount = chat?.unseenCount ?: 0
        val createdAt = chat?.createdAt ?: "0"



        message = if (type == "text"){
            chat?.message.toString().trim() ?: ""
        }else{
            type
        }


        if (unseenCount > 0) {
            binding.countTv.visibility = View.VISIBLE
        }else{
            binding.countTv.visibility = View.GONE
        }


        val abusiveWords = context.loadAbusiveWordsFromJson() // Load from JSON
        val cleanMessageText = message.censorAbusiveWords(abusiveWords)

        binding.nameTv.text = name
        binding.msgTv.text = cleanMessageText
        binding.countTv.text = unseenCount.toString()
        binding.timeTv.text = calculateDaysAgoInSmall(createdAt)
        Glide.with(context).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)

//        val customDrawable = CustomShapeDrawable(
//            context = context,  // or your activity/fragment context
//        )

//        binding.chatLayout.background = customDrawable

        holder.itemView.setOnClickListener {
            val intent = Intent(context, InboxScreenActivity::class.java)
            intent.putExtra("NAME",name)
            intent.putExtra("USER_NAME",userName)
            intent.putExtra("PROFILE_PIC",profilePic)
            intent.putExtra("USER_ID",userId)
            context.startActivity(intent)
        }

    }

    // Global function to update data
    fun updateData(newData: PagingData<Messages>) {
        CoroutineScope(Dispatchers.Main).launch {
            // Submit the new data to the adapter
            submitData(newData)
        }
    }

    object MessagesComparator : DiffUtil.ItemCallback<Messages>() {
        override fun areItemsTheSame(oldItem: Messages, newItem: Messages): Boolean {
            return oldItem.username == newItem.username  // Assuming Messages has an 'id' field
        }

        override fun areContentsTheSame(oldItem: Messages, newItem: Messages): Boolean {
            return oldItem == newItem
        }
    }

}