package com.thehotelmedia.android.activity.userTypes.business

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.databinding.ActivityBusinessSearchBinding
import com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation.IndividualSearchFragment

class BusinessSearchActivity : BaseActivity() {
    
    private lateinit var binding : ActivityBusinessSearchBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
        // Register the back button callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleOnBackPressedBtn()
            }
        })
    }

    private fun initUI() {
        openMyFragment()
        binding.backBtn.setOnClickListener {
            handleOnBackPressedBtn() // Call your custom back handler
        }

    }
    private fun openMyFragment() {
        val myFragment = IndividualSearchFragment()
        val bundle = Bundle()
        bundle.putString("FROM", "BUSINESS")
        myFragment.arguments = bundle
        val transaction = supportFragmentManager.beginTransaction()
        // Optionally, add animations
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        // Replace the current fragment with the new one
        transaction.replace(R.id.fragment_container, myFragment)
        transaction.addToBackStack(null) // Optional: to allow going back
        transaction.commit()
    }

    private fun handleOnBackPressedBtn() {
        // Check if there are any fragments in the back stack
        if (supportFragmentManager.backStackEntryCount > 0) {
            // If yes, pop the back stack
            supportFragmentManager.popBackStack()
            // Check if back stack is now empty
            // Delay the finish() by 2 seconds (2000 milliseconds)
            Handler(Looper.getMainLooper()).postDelayed({
                finish() // Close the activity after the delay
            }, 200)
        } else {
            finish() // If no fragments, finish the activity directly
        }
    }


}