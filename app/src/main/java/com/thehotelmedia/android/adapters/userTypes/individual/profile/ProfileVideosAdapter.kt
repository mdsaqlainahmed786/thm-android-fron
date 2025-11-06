package com.thehotelmedia.android.adapters.userTypes.individual.profile

import android.R.attr.bitmap
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.VideoDecoder
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.customClasses.Constants.VIDEO
import com.thehotelmedia.android.databinding.ProfileVideosItemsLayoutBinding
import com.thehotelmedia.android.extensions.moveToViewer
import com.thehotelmedia.android.extensions.moveToUserPostsViewer
import com.thehotelmedia.android.modals.profileData.video.Data
import java.io.File


class ProfileVideosAdapter (private val context: Context, private val userId: String = "") : PagingDataAdapter<Data, ProfileVideosAdapter.ViewHolder>(COMPARATOR)  {

    inner class ViewHolder(val binding: ProfileVideosItemsLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Data) {
            val mediaUri = item.sourceUrl ?: ""
            val thumbnailUrl = item.thumbnailUrl ?: ""
            val viewCount = item.views ?: 0

            if (viewCount > 0){
                binding.viewLayout.visibility = View.VISIBLE
                binding.viewCount.text = viewCount.toString()
            }else{
                binding.viewLayout.visibility = View.GONE
            }
            Glide.with(context)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_post_placeholder)
                .error(R.drawable.ic_post_placeholder)
                .thumbnail(0.1f) // Load thumbnail first for faster display
                .centerCrop()
                .skipMemoryCache(false) // Use memory cache
                .into(binding.imageView)




            binding.root.setOnClickListener {
                if (userId.isNotEmpty()) {
                    // Launch UserPostsViewerActivity with video filter - only scroll through videos
                    context.moveToUserPostsViewer(userId, null, "video")
                } else {
                    // Fallback to old viewer if userId not available
                    context.moveToViewer(VIDEO, mediaUri)
                }
            }

        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfileVideosItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        item?.let { holder.bind(it) }
    }
    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<Data>() {
            override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
                return oldItem.Id == newItem.Id
            }

            override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
                return oldItem == newItem
            }
        }
    }

}