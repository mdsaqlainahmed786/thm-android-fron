package com.thehotelmedia.android.adapters.userTypes.individual.forms

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.AttachedMediaItemBinding

class AttachedMediaAdapter(
    private val context: Context,
    private val mediaList: MutableList<String>,
    private val onMediaUpdated: (MutableList<String>) -> Unit
) : RecyclerView.Adapter<AttachedMediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: AttachedMediaItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = AttachedMediaItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val binding = holder.binding
        val mediaUri = Uri.parse(mediaList[position])

        val mimeType = context.contentResolver.getType(mediaUri)
        val fileName = mediaUri.toString()

        println("afadhsjkafh   $mediaUri")



//        if (mimeType?.startsWith("video") == true) {
       /* if (fileName.startsWith("video", true) || fileName.endsWith(".mp4", true)) {
            // Load video thumbnail
            Glide.with(context)
                .asBitmap()
                .load(mediaUri)
                .frame(1000000) // Get frame at 1 second
                .into(binding.mediaImageView)

            // Show play icon overlay for video
            binding.playIconImageView.visibility = View.VISIBLE
        }*/ if (fileName.startsWith("video", true) || fileName.endsWith(".mp4", true)) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(mediaList[position])
                val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    binding.mediaImageView.setImageBitmap(bitmap)
                } else {
                    binding.mediaImageView.setImageResource(R.drawable.ic_image_placeholder_image) // Fallback
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.mediaImageView.setImageResource(R.drawable.ic_image_placeholder_image) // Handle error
            } finally {
                retriever.release()
            }

            // Show play icon overlay for video
            binding.playIconImageView.visibility = View.VISIBLE
        }else {
            // Load image directly
            Glide.with(context)
                .load(mediaUri)
                .into(binding.mediaImageView)

            // Hide play icon for images
            binding.playIconImageView.visibility = View.GONE
        }

        // Handle cancel button click
        binding.cancelBtn.setOnClickListener {
            // Remove the item from the list
            mediaList.removeAt(position)
            // Notify the adapter about item removal
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, mediaList.size)
            // Call the onListUpdated callback with the updated list
            onMediaUpdated(mediaList)
        }
    }

    fun updateMediaList(newMediaList: List<String>) {
        mediaList.clear()
        mediaList.addAll(newMediaList)
        notifyDataSetChanged()
        onMediaUpdated(mediaList)
    }

}
