package com.thehotelmedia.android.adapters.onBoarding

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import com.thehotelmedia.android.R

class OnboardViewpagerAdapter(private var context: Context) : PagerAdapter() {
    private var images = intArrayOf(
        R.drawable.onboard_pic_one,
        R.drawable.onboard_pic_two,
        R.drawable.onboard_pic_three,
        R.drawable.onboard_pic_four,
    )
    private var heading = intArrayOf(
        R.string.onboard_heading_one,
        R.string.onboard_heading_two,
        R.string.onboard_heading_three,
        R.string.onboard_heading_four
    )
    private var description = intArrayOf(
        R.string.onboard_desc_one,
        R.string.onboard_desc_two,
        R.string.onboard_desc_three,
        R.string.onboard_desc_four
    )

    override fun getCount(): Int {
        return heading.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as ConstraintLayout
    }

    @SuppressLint("MissingInflatedId", "LocalSuppress")
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = layoutInflater.inflate(R.layout.onboarding_view_pager, container, false)
        val onboardIv = view.findViewById<View>(R.id.iv_onboard) as ImageView

        onboardIv.setImageResource(images[position])

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }
}