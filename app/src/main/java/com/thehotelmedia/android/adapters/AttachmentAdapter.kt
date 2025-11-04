package com.thehotelmedia.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.databinding.ItemAttachmentBinding
import com.thehotelmedia.android.extensions.moveToViewer
import com.thehotelmedia.android.modals.feeds.feed.MediaRef


class AttachmentAdapter(
    private val context: Context,
    private val mediaAttachment: ArrayList<MediaRef>
) :
    RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder>() {

    inner class AttachmentViewHolder(private val binding: ItemAttachmentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(mediaRef: MediaRef) {
            val thumbnailUrl = mediaRef.thumbnailUrl ?: ""
            val mediaUrl = mediaRef.sourceUrl ?: ""
            val mediaType = mediaRef.mediaType ?: ""

            if (mediaType == IMAGE){
                binding.playIconImageView.visibility = View.GONE
            }else{
                binding.playIconImageView.visibility = View.VISIBLE
            }

            // Load the image using Glide or another image loading library
            Glide.with(context)
                .load(thumbnailUrl)
                .placeholder(R.drawable.ic_post_placeholder)
                .into(binding.imageView)

            binding.root.setOnClickListener {
                context.moveToViewer(mediaType,mediaUrl)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        val binding = ItemAttachmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AttachmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        holder.bind(mediaAttachment[position])
    }

    override fun getItemCount(): Int = mediaAttachment.size
}