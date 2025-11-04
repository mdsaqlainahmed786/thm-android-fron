package com.thehotelmedia.android.adapters.userTypes.individual.forms.review

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.databinding.ReviewRatingItemBinding
import com.thehotelmedia.android.extensions.getEmojiForRating
import com.thehotelmedia.android.R
import com.thehotelmedia.android.modals.checkinData.checkInData.ReviewQuestions

class ReviewRatingAdapter(
    private val context: Context,
    private val reviewQuestions: ArrayList<ReviewQuestions>,
    private val onRatingSelected: (String, Int) -> Unit
) : RecyclerView.Adapter<ReviewRatingAdapter.ViewHolder>()  {

    inner class ViewHolder(val binding: ReviewRatingItemBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ReviewRatingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
       return reviewQuestions.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val question = reviewQuestions[position].question.toString()
        val questionId = reviewQuestions[position].id.toString()

        binding.QuestionsTv.text = question


        binding.firstStar.setOnClickListener { onStarClick(1,binding,questionId) }
        binding.secondStar.setOnClickListener { onStarClick(2, binding, questionId) }
        binding.thirdStar.setOnClickListener { onStarClick(3, binding, questionId) }
        binding.fourthStar.setOnClickListener { onStarClick(4, binding, questionId) }
        binding.fifthStar.setOnClickListener { onStarClick(5, binding, questionId) }





    }
    // Common click listener for stars
    private fun onStarClick(
        selectedRating: Int,
        binding: ReviewRatingItemBinding,
        question: String
    ) {
        var rating = 0
        rating = selectedRating
        binding.ratingTv.text = getEmojiForRating(rating)
        binding.ratingTv.textSize = 24f

        // Play animation for all stars up to the selected rating
        for (i in 1..5) {
            val starView = when (i) {
                1 -> binding.firstStar
                2 -> binding.secondStar
                3 -> binding.thirdStar
                4 -> binding.fourthStar
                5 -> binding.fifthStar
                else -> null
            }
            starView?.let { view ->
                if (i <= rating) {
                    view.setAnimation(R.raw.star_animation) // Set animation
                    view.playAnimation() // Play animation
                    view.clearColorFilter() // Remove any previous color filters
                } else {
                    view.cancelAnimation() // Stop animation
                    view.progress = 0f // Reset animation progress
                    view.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.ic_unselecetd_star))
                }
            }
        }

        onRatingSelected(question, rating)


    }

}