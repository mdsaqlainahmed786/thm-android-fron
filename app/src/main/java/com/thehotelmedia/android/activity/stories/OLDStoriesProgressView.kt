package com.thehotelmedia.android.activity.stories

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.thehotelmedia.android.R

class OLDStoriesProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val progressBars = mutableListOf<ProgressBar>()

    fun setStoryCount(count: Int) {
//        removeAllViews()
//        progressBars.clear()
//        for (i in 0 until count) {
//            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
//                max = 1000
//                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
//                    marginEnd = 5
//                }
//            }
//            addView(progressBar)
//            progressBars.add(progressBar)
//        }

        removeAllViews()
        progressBars.clear()
        for (i in 0 until count) {
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 5
                }
                progressDrawable = context.getDrawable(R.drawable.progress_bar_drawable) // Set custom blue and white drawable
//                background = context.getDrawable(R.drawable.progress_bar_background) // Set the white background
            }
            addView(progressBar)
            progressBars.add(progressBar)
        }
    }

    fun startProgress(index: Int, duration: Long) {
        progressBars[index].progress = 0
        ObjectAnimator.ofInt(progressBars[index], "progress", 1000).apply {
            this.duration = duration
            start()
        }
    }

    fun fillProgress(index: Int) {
        // Fill the progress bar instantly
        progressBars[index].progress = 1000
    }

    // Reset the progress to 0 instantly
    fun resetProgress(index: Int) {
        progressBars[index].progress = 0
    }


}
