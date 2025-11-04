package com.thehotelmedia.android.adapters.authentication.business

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R

class ImageAdapter(
    private val items: MutableList<Uri>,
    private val onPlusClick: () -> Unit,
    private val onCancelClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_IMAGE = 0
    private val VIEW_TYPE_PLUS = 1

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) VIEW_TYPE_PLUS else VIEW_TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
                ImageViewHolder(view)
            }
            VIEW_TYPE_PLUS -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plus, parent, false)
                PlusViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageViewHolder) {
            val imageUri = items[position]
            holder.imageView.setImageURI(imageUri)
            holder.cancelIcon.setOnClickListener {
                onCancelClick(position)
            }


        } else if (holder is PlusViewHolder) {
            holder.plusIcon.setOnClickListener { onPlusClick() }
        }
    }

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view)
        val cancelIcon: ImageView = view.findViewById(R.id.cancel_icon)
    }

    inner class PlusViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val plusIcon: RelativeLayout = view.findViewById(R.id.plus_icon)
    }
}
