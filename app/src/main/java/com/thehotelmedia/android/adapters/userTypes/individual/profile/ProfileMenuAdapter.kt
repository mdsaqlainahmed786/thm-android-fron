package com.thehotelmedia.android.adapters.userTypes.individual.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.ProfileMenuItemsLayoutBinding
import com.thehotelmedia.android.modals.menu.MenuItem

class ProfileMenuAdapter(
    private val context: Context,
    private val onItemClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, ProfileMenuAdapter.ViewHolder>(COMPARATOR) {

    inner class ViewHolder(val binding: ProfileMenuItemsLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuItem) {
            // Get the media object (it's a single object, not an array)
            val mediaItem = item.media
            
            if (mediaItem == null) {
                // No media found, show placeholder
                binding.typeIcon.setImageResource(R.drawable.ic_post_placeholder)
                binding.typeTv.text = "Menu"
                binding.menuImageView.setImageResource(R.drawable.ic_post_placeholder)
                return
            }

            // Determine type from mimeType or mediaType
            val mimeType = mediaItem.mimeType?.lowercase().orEmpty()
            val mediaType = mediaItem.mediaType?.lowercase().orEmpty()
            val imageUrl = mediaItem.sourceUrl
            val thumbnailUrl = mediaItem.thumbnailUrl

            when {
                mimeType.contains("pdf") || mediaType.contains("pdf") -> {
                    binding.typeIcon.setImageResource(R.drawable.ic_post_placeholder)
                    binding.typeTv.text = "PDF"
                    // For PDF, show thumbnail if available, otherwise show default icon
                    if (!thumbnailUrl.isNullOrEmpty()) {
                        Glide.with(context)
                            .load(thumbnailUrl)
                            .placeholder(R.drawable.ic_post_placeholder)
                            .error(R.drawable.ic_post_placeholder)
                            .centerCrop()
                            .into(binding.menuImageView)
                    } else {
                        binding.menuImageView.setImageResource(R.drawable.ic_post_placeholder)
                    }
                }
                mimeType.startsWith("image") || mediaType == "im" -> {
                    binding.typeIcon.setImageResource(R.drawable.ic_photos)
                    binding.typeTv.text = "Image"
                    // Use thumbnail if available for faster loading, otherwise use sourceUrl
                    val urlToLoad = if (!thumbnailUrl.isNullOrEmpty()) thumbnailUrl else imageUrl
                    if (!urlToLoad.isNullOrEmpty()) {
                        Glide.with(context)
                            .load(urlToLoad)
                            .placeholder(R.drawable.ic_post_placeholder)
                            .error(R.drawable.ic_post_placeholder)
                            .centerCrop()
                            .into(binding.menuImageView)
                    } else {
                        binding.menuImageView.setImageResource(R.drawable.ic_post_placeholder)
                    }
                }
                else -> {
                    binding.typeIcon.setImageResource(R.drawable.ic_post_placeholder)
                    binding.typeTv.text = "Menu"
                    binding.menuImageView.setImageResource(R.drawable.ic_post_placeholder)
                }
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProfileMenuItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    companion object {
        private val COMPARATOR = object : DiffUtil.ItemCallback<MenuItem>() {
            override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
                return oldItem.Id == newItem.Id
            }

            override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

