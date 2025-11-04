package com.thehotelmedia.android.adapters.imageEditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.DeadObjectException
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.ItemFilterBinding
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoFilter
import java.util.Locale

class FilterAdapter(
    private val context: Context,
    private val photoEditor: PhotoEditor, // Pass PhotoEditor instance to apply filters
    private val filterList: List<PhotoFilter>,
    private val imageUri: Uri?
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private var selectedPosition: Int = 0


    private lateinit var imageFilters: PhotoEditor


    inner class FilterViewHolder(private val binding: ItemFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            // Initialize PhotoEditor once
            imageFilters = PhotoEditor.Builder(context, binding.filterImageView)
                .setPinchTextScalable(true)
                .setClipSourceImage(true)
                .build()
        }

        fun bind(filter: PhotoFilter) {



            val filterName = filter.name
            val formattedFilterName = formatString(filterName)
            binding.filterName.setTextColor(
                if (position == selectedPosition) {
                    // Color for selected item
                    context.getColor(R.color.blue) // Replace with your blue color resource
                } else {
                    // Color for non-selected items
                    context.getColor(R.color.text_color_60) // Replace with your default color resource
                }
            )

//            Glide.with(context).load(imageUri).into(binding.filterImageView)


            Glide.with(context)
                .asBitmap()
                .load(imageUri)
                .apply(
                    RequestOptions()
                        .error(android.R.color.darker_gray)
                        .circleCrop()  // Apply rounded corners with radius of 20dp
                )
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        binding.filterImageView.source.setImageBitmap(resource)
                        binding.filterImageView.source.scaleType = ImageView.ScaleType.FIT_CENTER

                        // Apply filter effect after the image is loaded
//                        binding.filterImageView.post {
//                            applyFilter(filter)
//                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle cleanup if necessary
                        binding.filterImageView.source.setImageDrawable(placeholder)
                    }
                })


            binding.filterName.text = formattedFilterName

            binding.root.setOnClickListener {

                // Update the selected position
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition

                // Notify adapter that item has changed
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                // Apply filter when clicked
                photoEditor.setFilterEffect(filter)
            }
        }
        private fun applyFilter(filter: PhotoFilter) {
            try {
                imageFilters.setFilterEffect(filter)
            } catch (e: DeadObjectException) {
                // Handle the case where the PhotoEditor is no longer available
                Log.e("PhotoEditorError", "PhotoEditor is no longer available: ${e.message}")
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filterList[position]
        holder.bind(filter)
    }

    override fun getItemCount(): Int {
        return filterList.size
    }
    fun formatString(input: String): String {
        // Convert the string to lowercase and replace underscores with spaces
        val result = input.toLowerCase(Locale.getDefault()).replace("_", " ")
        // Capitalize the first letter of each word
        return result.split(" ").joinToString(" ") { it.capitalize(Locale.getDefault()) }
    }
}
