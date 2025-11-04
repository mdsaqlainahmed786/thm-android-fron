package com.thehotelmedia.android.adapters.userTypes.business

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.PostPreviewActivity
import com.thehotelmedia.android.activity.ViewEventDetailsActivity
import com.thehotelmedia.android.databinding.InsightMediaItemBinding
import com.thehotelmedia.android.extensions.moveToViewer
import com.thehotelmedia.android.modals.insight.Posts
import com.thehotelmedia.android.modals.insight.Stories

class InsightMediaAdapter(
    private val context: Context,
    private var storiesList: ArrayList<Stories>,
    private var postsList: ArrayList<Posts>,
) : RecyclerView.Adapter<InsightMediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: InsightMediaItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = InsightMediaItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

//    override fun getItemCount(): Int {
//        return 10
//    }


    override fun getItemCount(): Int {
        return if (storiesList.isNotEmpty()) {
            storiesList.size
        } else {
            postsList.size
        }
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val binding = holder.binding

        // Set default padding
        val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = 0

        // Add padding to the first item
        if (position == 0) {
            layoutParams.marginStart = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
        }

        binding.root.layoutParams = layoutParams

        var thumbnailUrl = ""
        var mediaUri = ""
        var mimeType = ""
        var mediaType = ""


        if (storiesList.isNotEmpty()){
            val story = storiesList[position]
            mediaUri = Uri.parse(story.sourceUrl ?: "").toString()
            thumbnailUrl = Uri.parse(story.thumbnailUrl ?: "").toString()
            mimeType = story.mimeType ?: "" // Use the mimeType directly from your data

            if (mimeType.startsWith("video")) {
                // Load video thumbnail
                Glide.with(context)
                    .load(thumbnailUrl)
                    .into(binding.mediaImageView)

                // Show play icon overlay for video
                binding.playIconImageView.visibility = View.VISIBLE
            } else {
                // Load image directly
                Glide.with(context)
                    .load(mediaUri)
                    .into(binding.mediaImageView)

                // Hide play icon for images
                binding.playIconImageView.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                context.moveToViewer(mediaType,mediaUri)
            }


        }else{
            val postId = postsList[position].Id ?: ""
            val postType = postsList[position].postType ?: ""

            val mediaRef = postsList[position].mediaRef
            if (mediaRef.isNotEmpty()){
                mediaType = mediaRef[0].mediaType ?: ""
                mediaUri = mediaRef[0].sourceUrl ?: ""
                thumbnailUrl = mediaRef[0].thumbnailUrl ?: ""
                if (mediaType == "image") {

                    // Load image directly
                    Glide.with(context)
                        .load(mediaUri)
                        .placeholder(R.drawable.ic_post_placeholder)
                        .into(binding.mediaImageView)
                    // Hide play icon for images
                    binding.playIconImageView.visibility = View.GONE

                } else {
                    // Load video thumbnail
                    Glide.with(context)
                        .asBitmap()
                        .load(thumbnailUrl)
                        .placeholder(R.drawable.ic_post_placeholder)
                        .frame(1000000) // Get frame at 1 second
                        .into(binding.mediaImageView)
                    // Show play icon overlay for video
                    binding.playIconImageView.visibility = View.VISIBLE
                }
            }


            binding.root.setOnClickListener {
                if (postType == "event"){
                    moveToEventDetailsActivity(postId)
                }else{
                    moveToPostPreviewScreen(postId)
                }
            }


        }
//        binding.root.setOnClickListener {
//            context.moveToViewer(mediaType,mediaUri)
//        }

    }


    private fun moveToPostPreviewScreen(postId: String) {
        val intent = Intent(context, PostPreviewActivity::class.java)
        intent.putExtra("FROM", "Notification")
        intent.putExtra("POST_ID", postId)
        context.startActivity(intent)
    }

    private fun moveToEventDetailsActivity(postId: String) {
        val intent = Intent(context, ViewEventDetailsActivity::class.java).apply {
            putExtra("POST_ID", postId)
        }
        context.startActivity(intent)
    }


}