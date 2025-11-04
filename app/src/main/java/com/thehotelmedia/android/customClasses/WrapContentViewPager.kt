package com.thehotelmedia.android.customClasses

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager

class WrapContentViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSpec = heightMeasureSpec

        if (widthMode != MeasureSpec.UNSPECIFIED) {
            val maxHeight = getChildAt(0)?.let { child ->
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                child.measuredHeight
            } ?: 0

            heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, heightSpec)
    }

    fun recalculateHeight(position: Int) {
        val child = getChildAt(position)
        child?.let {
            it.measure(
                MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            val height = it.measuredHeight
            val layoutParams = layoutParams
            layoutParams.height = height
            setLayoutParams(layoutParams)
            requestLayout() // Ensure the layout is updated
        }
    }
}
