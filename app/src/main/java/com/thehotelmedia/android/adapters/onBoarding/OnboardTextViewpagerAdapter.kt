package com.thehotelmedia.android.adapters.onBoarding

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.PagerAdapter
import com.thehotelmedia.android.R

class OnboardTextViewpagerAdapter(private var context: Context) : PagerAdapter() {

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
        val view: View = layoutInflater.inflate(R.layout.text_viewpager, container, false)
        val onboardHeading: TextView = view.findViewById<View>(R.id.tv_heading) as TextView
        val onboardDesc: TextView = view.findViewById<View>(R.id.tv_text) as TextView
        onboardHeading.setText(heading[position])
        onboardDesc.setText(description[position])

        // Get the string resources
        val headingString = context.getString(heading[position])
        val descriptionString = context.getString(description[position])


        onboardHeading.text = headingString
        // Set description directly
        onboardDesc.text = descriptionString


        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as ConstraintLayout)
    }


}