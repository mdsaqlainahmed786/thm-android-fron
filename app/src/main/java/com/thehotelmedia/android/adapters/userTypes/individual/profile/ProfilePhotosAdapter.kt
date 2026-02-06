package com.thehotelmedia.android.adapters.userTypes.individual.profile

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.databinding.ProfilePhotosItemsLayoutBinding
import com.thehotelmedia.android.extensions.moveToViewer
import com.thehotelmedia.android.extensions.moveToUserPostsViewer
import com.thehotelmedia.android.modals.profileData.image.ImageData

class ProfilePhotosAdapter(
    private val context: Context,
    private val userId: String = ""
)  : PagingDataAdapter<ImageData, ProfilePhotosAdapter.ViewHolder>(COMPARATOR)  {

//    inner class ViewHolder(val binding: ProfilePhotosItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    inner class ViewHolder(val binding: ProfilePhotosItemsLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ImageData, position: Int) {
            val mediaUri = item.sourceUrl.orEmpty()
            Glide.with(context)
                .load(mediaUri)
                .placeholder(R.drawable.ic_post_placeholder)
                .error(R.drawable.ic_post_placeholder)
                .thumbnail(0.1f) // Load thumbnail first for faster display
                .centerCrop()
                .skipMemoryCache(false) // Use memory cache
                .into(binding.imageView)

            binding.root.setOnClickListener {
                if (userId.isNotEmpty()) {
                    // Launch UserPostsViewerActivity with image filter - only scroll through photos
                    // Pass media ID, sourceUrl, and position index for best-effort matching
                    val mediaId = item.Id ?: ""
                    val clickPos = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
                    context.moveToUserPostsViewer(userId, null, "image", mediaId, mediaUri, clickPos)
                } else {
                    // Fallback to old viewer if userId not available
                    context.moveToViewer(IMAGE, mediaUri)
                }
            }

        }
    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfilePhotosItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        item?.let { holder.bind(it, position) }
    }
    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<ImageData>() {
            override fun areItemsTheSame(oldItem: ImageData, newItem: ImageData): Boolean {
                return oldItem.Id == newItem.Id
            }

            override fun areContentsTheSame(oldItem: ImageData, newItem: ImageData): Boolean {
                return oldItem == newItem
            }
        }
    }
}
