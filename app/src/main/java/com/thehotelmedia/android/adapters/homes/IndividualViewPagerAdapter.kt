package com.thehotelmedia.android.adapters.homes

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualChatFragment
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualHomeFragment
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualProfileFragment
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualSearchFragment

class IndividualViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    private val fragments = listOf(
        IndividualHomeFragment(),
        IndividualSearchFragment(),
        Fragment(),  // Placeholder for addPlus (No action for now)
        IndividualChatFragment(),
        IndividualProfileFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}