package com.thehotelmedia.android.adapters.homes

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.thehotelmedia.android.fragments.userTypes.business.BusinessInsightFragment
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualChatFragment
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualHomeFragment
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualProfileFragment


class BusinessViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragments = listOf(
        IndividualHomeFragment(),
        BusinessInsightFragment(),
        Fragment(),  // Placeholder for addPlus (No action for now)
        IndividualChatFragment(),
        IndividualProfileFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}