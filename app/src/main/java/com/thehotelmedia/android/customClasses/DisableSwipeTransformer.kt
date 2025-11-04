package com.thehotelmedia.android.customClasses
import android.view.View
import androidx.viewpager.widget.ViewPager

class DisableSwipeTransformer : ViewPager.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // Do nothing to disable swiping effect
    }
}