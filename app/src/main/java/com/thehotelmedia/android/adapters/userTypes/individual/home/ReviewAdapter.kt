package com.thehotelmedia.android.adapters.userTypes.individual.home

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.ReviewItemsLayoutBinding
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.setRatingWithStar
import com.thehotelmedia.android.extensions.setRatingWithStars

class ReviewAdapter(
    private val context: Context,
    private val onMenuClicked: (position: Int) -> Unit,
    private val onLikeClicked: (position: Int, isLiked: Boolean) -> Unit,
    private val onCommentClicked: (position: Int) -> Unit,
    private val onShareClicked: (position: Int) -> Unit,
    private val onSaveClicked: (position: Int, isSaved: Boolean) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {



    inner class ViewHolder(val binding: ReviewItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ReviewItemsLayoutBinding.inflate(
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
        blurView(binding)
//        val customDrawable = CustomShapeDrawable(
//            context = context,  // or your activity/fragment contextD
//        )
//
//        binding.postLayout.background = customDrawable

        val rating = 3.0 // Replace with your dynamic rating
        binding.averageRatingTv.setRatingWithStar(rating, R.drawable.ic_rating_star)
        binding.ratingTV.setRatingWithStars(rating.toInt())



        // Initialize like count and like state
        var likeCount = 12 // Replace with your dynamic like count
        var commentCount = 10 // Replace with your dynamic like count
        var shareCount = 11 // Replace with your dynamic like count


        binding.likeTv.text = likeCount.toString()
        binding.commentTv.text = commentCount.toString()
        binding.shareTv.text = shareCount.toString()



        var isPostLiked = false // Replace with your actual logic to determine if liked or not

        var isPostSaved = false
        var isPostCommented = false // Replace with your actual logic to determine if liked or not

        if (isPostLiked){
            binding.likeIv.setImageResource(R.drawable.ic_like_icon)
        }else{
            binding.likeIv.setImageResource(R.drawable.ic_unlike_icon)
        }
        if (isPostSaved){
            binding.saveIv.setImageResource(R.drawable.ic_save_icon)
        }else{
            binding.saveIv.setImageResource(R.drawable.ic_unsave_icon)
        }


        binding.menuBtn.setOnClickListener {
            onMenuClicked(position)
        }


        binding.likeBtn.setOnClickListener {
            val likeAnimation = if (isPostLiked) {
                AnimationUtils.loadAnimation(context, R.anim.top_to_bottom)
            } else {
                AnimationUtils.loadAnimation(context, R.anim.bottom_to_top)
            }

            // Update like count with animation
            if (isPostLiked) {
                binding.likeIv.setImageResource(R.drawable.ic_unlike_icon)
                likeCount--
                isPostLiked = false
            } else {
                binding.likeIv.setImageResource(R.drawable.ic_like_icon)
                likeCount++
                isPostLiked = true
            }

            // Apply the text animation
            binding.likeTv.startAnimation(likeAnimation)
            animateLikeCount(binding.likeTv, if (isPostLiked) likeCount - 1 else likeCount + 1, likeCount)

            onLikeClicked(position, isPostLiked)
        }


        binding.commentBtn.setOnClickListener {
            onCommentClicked(position)
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

    private fun blurView(binding: ReviewItemsLayoutBinding) {
        context.blurTheView(binding.topBlurView)
        context.blurTheView(binding.bottomBlurView)
    }

    private fun animateLikeCount(textView: TextView, startValue: Int, endValue: Int) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = 300 // Duration in milliseconds
        animator.addUpdateListener { valueAnimator ->
            textView.text = valueAnimator.animatedValue.toString()
        }
        animator.start()
    }


}