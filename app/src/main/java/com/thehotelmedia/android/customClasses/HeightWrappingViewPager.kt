package com.thehotelmedia.android.customClasses

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager

class HeightWrappingViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    private var currentPosition = 0

    init {
        addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                currentPosition = position
                requestLayout() // Trigger a re-measurement when the page changes
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpecVar = heightMeasureSpec

        // Measure the height of the current fragment
        val child = getChildAt(currentPosition)
        child?.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val height = child?.measuredHeight ?: 0

        if (height > 0) {
            heightMeasureSpecVar = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpecVar)
    }
}

