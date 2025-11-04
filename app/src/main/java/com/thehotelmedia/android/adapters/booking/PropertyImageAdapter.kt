package com.thehotelmedia.android.adapters.booking

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.PropertyItemLayoutBinding
import com.thehotelmedia.android.extensions.moveToViewer
import com.thehotelmedia.android.modals.booking.roomDetails.RoomImagesRef


class PropertyImageAdapter(private val context: Context,private val  roomImagesRef: ArrayList<RoomImagesRef>) :
    RecyclerView.Adapter<PropertyImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: PropertyItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = PropertyItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = roomImagesRef[position]
        val image = imageItem.sourceUrl ?: ""
        Glide.with(context).load(image).placeholder(R.drawable.ic_post_placeholder).into(holder.binding.propertyIV)

        // Apply 12dp right margin only to the last item
        if (position == itemCount - 1) {
            val params = holder.binding.root.layoutParams as ViewGroup.MarginLayoutParams
            params.rightMargin = (12 * context.resources.displayMetrics.density).toInt()
            holder.binding.root.layoutParams = params
        }


        holder.itemView.setOnClickListener {
            context.moveToViewer("image", image)
        }

    }

    override fun getItemCount(): Int = roomImagesRef.size
}
