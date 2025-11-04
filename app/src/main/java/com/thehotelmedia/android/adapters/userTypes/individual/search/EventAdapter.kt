package com.thehotelmedia.android.adapters.userTypes.individual.search

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.EventItemsLayoutBinding
import com.thehotelmedia.android.extensions.setRatingWithStar


class EventAdapter(
    private val context: Context,
    private val onMenuClicked: (position: Int) -> Unit,
    private val onJoiningClicked: (position: Int, isLiked: Boolean) -> Unit,
    private val onShareClicked: (position: Int) -> Unit,
    private val onSaveClicked: (position: Int, isSaved: Boolean) -> Unit
) : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: EventItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = EventItemsLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    override fun getItemCount(): Int {
        return 5
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val binding = holder.binding
//        val customDrawable = CustomShapeDrawable(
//            context = context,  // or your activity/fragment context
//        )
//
//        binding.postLayout.background = customDrawable

        val rating = 5.0 // Replace with your dynamic rating
        binding.averageRatingTv.setRatingWithStar(rating, R.drawable.ic_rating_star)

        // Initialize like count and like state
        var shareCount = 11 // Replace with your dynamic like count

        binding.shareTv.text = shareCount.toString()



        var isPostSaved = false
        var isJoined = false


        if (isJoined){
            binding.joiningIv.setImageResource(R.drawable.ic_save_icon)
        }else{
            binding.joiningIv.setImageResource(R.drawable.ic_unsave_icon)
        }
        if (isPostSaved){
            binding.saveIv.setImageResource(R.drawable.ic_save_icon)
        }else{
            binding.saveIv.setImageResource(R.drawable.ic_unsave_icon)
        }


        binding.menuBtn.setOnClickListener {
            onMenuClicked(position)
        }

        binding.joiningBtn.setOnClickListener {
            // Update like count with animation
            if (isJoined) {
                binding.joiningIv.setImageResource(R.drawable.ic_unsave_icon)
                isJoined = false
            } else {
                binding.joiningIv.setImageResource(R.drawable.ic_save_icon)
                isJoined = true
            }

            onJoiningClicked(position, isJoined)
        }


        binding.shareBtn.setOnClickListener {
            onShareClicked(position)
        }


        binding.saveIv.setOnClickListener {
            // Update like count with animation
            if (isPostSaved) {
                binding.saveIv.setImageResource(R.drawable.ic_unsave_icon)
                isPostSaved = false
            } else {
                binding.saveIv.setImageResource(R.drawable.ic_save_icon)
                isPostSaved = true
            }

            onSaveClicked(position, isPostSaved)
        }

    }



}