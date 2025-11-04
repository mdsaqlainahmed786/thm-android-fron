package com.thehotelmedia.android.customClasses

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class NonSwipeableViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {
    override fun onInterceptTouchEvent(ev: android.view.MotionEvent?): Boolean {
        return false // Disable swiping
    }

    override fun onTouchEvent(ev: android.view.MotionEvent?): Boolean {
        return false // Disable swiping
    }
}