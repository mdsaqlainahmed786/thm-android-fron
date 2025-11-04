package com.thehotelmedia.android.activity.userTypes.profile

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.FollowingFollowerTabPagerAdapter
import com.thehotelmedia.android.databinding.ActivityFollowerFollowingBinding
import com.thehotelmedia.android.fragments.followingFollower.FollowerFragment
import com.thehotelmedia.android.fragments.followingFollower.FollowingFragment

class FollowerFollowingActivity : BaseActivity() {

    private lateinit var binding: ActivityFollowerFollowingBinding
    private lateinit var pager: ViewPager
    private lateinit var tab: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowerFollowingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
    }

    private fun initUi() {
        val userName = intent.getStringExtra("USERNAME")
        val from = intent.getStringExtra("FROM")
        val userId = intent.getStringExtra("USERID")
        binding.titleTv.text = userName

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val comicRegular = ResourcesCompat.getFont(this, R.font.comic_regular)

        val follower = getString(R.string.follower)
        val following = getString(R.string.following)
        val adapter = FollowingFollowerTabPagerAdapter(supportFragmentManager)

        // Pass the userId to each fragment via Bundle
        val followerFragment = FollowerFragment().apply {
            arguments = Bundle().apply {
                putString("USER_ID", userId)
            }
        }

        val followingFragment = FollowingFragment().apply {
            arguments = Bundle().apply {
                putString("USER_ID", userId)
            }
        }

        adapter.addFragment(followerFragment, follower)
        adapter.addFragment(followingFragment, following)
        pager = binding.viewPager
        pager.adapter = adapter
        tab = binding.tabLayout

//        setupTabLayout()


        // Set up TabLayout with ViewPager
        tab.setupWithViewPager(pager)

        // Set tab titles and ensure they are in normal case
        for (i in 0 until tab.tabCount) {
            val tabView = (tab.getChildAt(0) as ViewGroup).getChildAt(i) as ViewGroup
            val tabTextView = tabView.getChildAt(1) as TextView
            tabTextView.text = adapter.getPageTitle(i) // Set the tab title
            tabTextView.isAllCaps = false // Disable automatic capitalization
            tabTextView.typeface = comicRegular// Set the default font (unselected font)
        }

        // Set the selected tab based on 'from' value
        if (from != null) {
            if (from.equals("Follower", ignoreCase = true)) {
                pager.currentItem = 0 // Select "Follower" tab
            } else if (from.equals("Following", ignoreCase = true)) {
                pager.currentItem = 1 // Select "Following" tab
            }
        }

        // Set tab selection listener
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (comicRegular != null) {
                    applyFontFamily(tab, comicRegular, true)
                }
                pager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                if (comicRegular != null) {
                    applyFontFamily(tab, comicRegular, true)
                }
                tab.view.setBackgroundColor(Color.TRANSPARENT)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }



    private fun applyFontFamily(tab: TabLayout.Tab, typeface: Typeface, isSelected: Boolean) {
        val viewGroup = (binding.tabLayout.getChildAt(0) as ViewGroup).getChildAt(tab.position) as ViewGroup
        val tabTextView = viewGroup.getChildAt(1) as TextView // Assumes TextView is at index 1
        tabTextView.typeface = if (isSelected) typeface else Typeface.DEFAULT
    }
}
