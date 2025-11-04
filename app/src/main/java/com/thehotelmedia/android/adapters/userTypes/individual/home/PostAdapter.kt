package com.thehotelmedia.android.adapters.userTypes.individual.home

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BusinessProfileDetailsActivity
import com.thehotelmedia.android.databinding.PostItemsLayoutBinding
import com.thehotelmedia.android.extensions.setRatingWithStar

class PostAdapter(
    private val context: Context,
    private val onMenuClicked: (position: Int) -> Unit,
    private val onLikeClicked: (position: Int, isLiked: Boolean) -> Unit,
    private val onCommentClicked: (position: Int) -> Unit,
    private val onShareClicked: (position: Int) -> Unit,
    private val onSaveClicked: (position: Int, isSaved: Boolean) -> Unit
) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {
    private var dotsIndicator: SpringDotsIndicator? = null

    private lateinit var mediaPagerAdapter : MediaPagerAdapter

    val mediaList = listOf(
        MediaItems(MediaType.IMAGE, "https://i.pinimg.com/564x/34/37/a2/3437a2c17ac6e710f946908182ea7100.jpg"),
//        MediaItem(MediaType.VIDEO, "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
        MediaItems(MediaType.IMAGE, "https://i.pinimg.com/736x/4e/cc/8b/4ecc8b8eb93397cdb4292026efd768f0.jpg"),
//        MediaItem(MediaType.VIDEO, "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
        MediaItems(MediaType.IMAGE, "https://upload.wikimedia.org/wikipedia/en/a/aa/Sanji_%28One_Piece%29.jpg"),
//        MediaItem(MediaType.VIDEO, "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
        MediaItems(MediaType.IMAGE, "https://i.pinimg.com/236x/b7/5c/aa/b75caa0d2b34465f7025ac7952dbe16f.jpg"),
//        MediaItem(MediaType.VIDEO, "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4")
    )

    inner class ViewHolder(val binding: PostItemsLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostItemsLayoutBinding.inflate(
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
        dotsIndicator = binding.indicatorLayout
//        val customDrawable = CustomShapeDrawable(
//            context = context,  // or your activity/fragment context
//        )

//        binding.postLayout.background = customDrawable

        val rating = 5.0 // Replace with your dynamic rating
        binding.averageRatingTv.setRatingWithStar(rating, R.drawable.ic_rating_star)

        setDescriptionTextAndClick(binding)

        dotsIndicator?.dotsClickable = false
        dotsIndicator?.setDotIndicatorColor(ContextCompat.getColor(context, R.color.blue))
        dotsIndicator?.setStrokeDotsIndicatorColor(ContextCompat.getColor(context, R.color.grey))
//         mediaPagerAdapter = MediaPagerAdapter(context, mediaList)
//        binding.viewPager.adapter = mediaPagerAdapter
        dotsIndicator?.attachTo(binding.viewPager)



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

        binding.nameTv.setOnClickListener {
            val intent = Intent(context, BusinessProfileDetailsActivity::class.java)
            context.startActivity(intent)
        }



    }

    private fun animateLikeCount(textView: TextView, startValue: Int, endValue: Int) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = 300 // Duration in milliseconds
        animator.addUpdateListener { valueAnimator ->
            textView.text = valueAnimator.animatedValue.toString()
        }
        animator.start()
    }

    private fun setDescriptionTextAndClick(binding: PostItemsLayoutBinding) {
        val textColor = ContextCompat.getColor(context, R.color.blue_50)

        val bgColor = ContextCompat.getColor(context, R.color.transparent)

        val description = "Just remember, even Everest started as a small hill – conquer one step at a time with a smile.”"
        val feeling = "Feeling Happy"
        val peoples = "Arvind Kumar Singh and 10 others"
        val location = "SAS Nagar Mohali"

        val finalText = "$description - $feeling - with $peoples - at $location"
        val spannableString = SpannableString(finalText)

        // Get the start and end indices of each portion of text
        val feelingStart = finalText.indexOf(feeling)
        val feelingEnd = feelingStart + feeling.length
        val peoplesStart = finalText.indexOf(peoples)
        val peoplesEnd = peoplesStart + peoples.length
        val locationStart = finalText.indexOf(location)
        val locationEnd = locationStart + location.length

        // Set color and clickable span for feeling
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "Feeling", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, feelingStart, feelingEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set color and clickable span for peoples
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "peoples", Toast.LENGTH_SHORT).show()

            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, peoplesStart, peoplesEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set color and clickable span for location
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "Location", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = textColor // Set text color
                ds.isUnderlineText = false // Remove underline
                ds.bgColor = bgColor // Ensure background color is transparent
            }
        }, locationStart, locationEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the spannable text and make the TextView clickable
        binding.allDescriptionTv.text = spannableString
        binding.allDescriptionTv.setBackgroundResource(R.drawable.transparent_background)
        binding.allDescriptionTv.movementMethod = LinkMovementMethod.getInstance()
    }

}

data class MediaItems(
    val type: MediaType,
    val mediaUrl: String
)

enum class MediaType {
    IMAGE,
    VIDEO
}