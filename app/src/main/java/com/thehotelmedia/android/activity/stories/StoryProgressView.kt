package com.thehotelmedia.android.activity.stories

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat

class StoryProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        // Set progress drawable color to blue
        progressDrawable = ContextCompat.getDrawable(context, android.R.color.holo_blue_dark)

        max = 100
        progress = 0
    }

    private var animator: ValueAnimator? = null

    init {
        addView(progressBar)
    }

    fun setDuration(duration: Long) {
        // Customize the duration for progress bar completion
        progressBar.max = 100
    }

    fun startProgress(duration: Long) {
        // Animate the progress bar from 0 to 100 over the specified duration
        animator = ValueAnimator.ofInt(0, 100).apply {
            this.duration = duration
            interpolator = LinearInterpolator()  // Makes the progress smooth and linear
            addUpdateListener { animation ->
                val progressValue = animation.animatedValue as Int
                progressBar.progress = progressValue
            }
            start()
        }
    }

    fun resetProgress() {
        animator?.cancel()
        progressBar.progress = 0
    }

    fun pauseProgress() {
        animator?.pause()
    }

    fun resumeProgress() {
        animator?.resume()
    }
}
