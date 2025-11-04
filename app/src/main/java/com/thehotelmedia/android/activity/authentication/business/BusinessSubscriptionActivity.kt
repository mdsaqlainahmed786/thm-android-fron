package com.thehotelmedia.android.activity.authentication.business

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.authentication.business.SubscriptionAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityBusinessSubscriptionBinding
import com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans.SubscriptionData
import com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans.BusinessSubscriptionPlansModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class BusinessSubscriptionActivity : BaseActivity() {

    private lateinit var binding: ActivityBusinessSubscriptionBinding
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private var dotsIndicatorSubs: SpringDotsIndicator? = null

    private lateinit var authViewModel: AuthViewModel
    private val activity = this@BusinessSubscriptionActivity
    private lateinit var preferenceManager : PreferenceManager
    private var selectedBusinessId : String = ""
    private var selectedSubBusinessId : String = ""
    private var subscriptionsList: List<SubscriptionData> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        val progressBar = CustomProgressBar(activity) // 'this' refers to the context


        selectedBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_BUSINESS_ID,"").toString()
        selectedSubBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_SUB_BUSINESS_ID,"").toString()

        getSubscriptionData()


        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

        binding.nextBtn.setOnClickListener {

            val currentPosition = binding.subscriptionsVp.currentItem
            val actualPosition = currentPosition % subscriptionsList.size
            val currentSubscription = subscriptionsList[actualPosition]
            val subscriptionId = currentSubscription.id

            // Display the subscription ID in a toast message
            val intent = Intent(this, ReviewSummaryActivity::class.java)
            intent.putExtra("SUBSCRIPTION_ID", currentSubscription.id)
            startActivity(intent)
        }


        authViewModel.getSubscriptionPlansResult.observe(activity) { result ->
            if (result.status == true) {
                handelSubscriptionResults(result)

            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.loading.observe(activity) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        authViewModel.toast.observe(activity) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
        }




    }

    private fun handelSubscriptionResults(result: BusinessSubscriptionPlansModal?) {
        subscriptionsList = result?.subscriptionData!!
        if (subscriptionsList.isNotEmpty()){
            setViewPager()
        }
    }

    private fun getSubscriptionData() {
        authViewModel.getSubscriptionPlans()
    }


    private fun setViewPager() {
        subscriptionAdapter = SubscriptionAdapter(this, subscriptionsList)
        binding.subscriptionsVp.adapter = subscriptionAdapter
        binding.subscriptionsVp.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.subscriptionsVp.offscreenPageLimit = 1

        val pageMargin = resources.getDimensionPixelOffset(com.intuit.sdp.R.dimen._10sdp).toFloat()
        val pageOffset = resources.getDimensionPixelOffset(com.intuit.sdp.R.dimen._10sdp).toFloat()

        binding.subscriptionsVp.setPageTransformer { page, position ->
            val offset = position * -(2 * pageOffset + pageMargin)
            page.translationX = if (ViewCompat.getLayoutDirection(binding.subscriptionsVp) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                -offset
            } else {
                offset
            }
            page.scaleY = 1f - 0.1f * kotlin.math.abs(position)
        }

        binding.indicatorLayoutSubs.apply {
            setDotIndicatorColor(ContextCompat.getColor(this@BusinessSubscriptionActivity, R.color.blue))
            setStrokeDotsIndicatorColor(ContextCompat.getColor(this@BusinessSubscriptionActivity, R.color.grey))
            setViewPager2(binding.subscriptionsVp)
        }
    }

}