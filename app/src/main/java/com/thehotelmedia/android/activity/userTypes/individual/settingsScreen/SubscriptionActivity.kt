package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.business.ReviewSummaryActivity
import com.thehotelmedia.android.adapters.authentication.business.SubscriptionAdapter
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customDialog.CancelSubscriptionDialog
import com.thehotelmedia.android.databinding.ActivitySubscriptionBinding
import com.thehotelmedia.android.modals.authentication.business.BusinessSubscriptionPlans.SubscriptionData
import com.thehotelmedia.android.modals.subscriptions.SubscriptionsModal
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class SubscriptionActivity : BaseActivity() {

    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private lateinit var individualViewModel: IndividualViewModal
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var progressBar: CustomProgressBar

    private var selectedBusinessId: String = ""
    private var country: String = ""
    private var subscriptionsList: List<SubscriptionData> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(this)
        individualViewModel = ViewModelProvider(
            this,
            ViewModelFactory(null, individualRepo)
        )[IndividualViewModal::class.java]

        preferenceManager = PreferenceManager.getInstance(this)
        progressBar = CustomProgressBar(this)

        selectedBusinessId = preferenceManager.getString(
            PreferenceManager.Keys.USER_BUSINESS_ID, ""
        ).orEmpty()
        country = preferenceManager.getString(
            PreferenceManager.Keys.USER_COUNTRY, ""
        ).orEmpty()

        setupListeners()
        observeViewModel()
        getSubscriptionData()
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.cancelSubscriptionBtn.setOnClickListener {

            val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

//            val cancelDialog = CancelSubscriptionDialog {
//                individualViewModel.cancelSubscriptions()
//                onBackPressedDispatcher.onBackPressed()
//            }
//            cancelDialog.show(supportFragmentManager, "cancelSubscriptionDialog")
        }

        binding.nextBtn.setOnClickListener {
            if (country.isEmpty()) {
                CustomSnackBar.showSnackBar(
                    binding.root,
                    MessageStore.pleaseAddBillingAddress(this)
                )
                startActivity(Intent(this, IndividualBillingAddressActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val currentPosition = binding.subscriptionsVp.currentItem % subscriptionsList.size
            val currentSubscription = subscriptionsList[currentPosition]

            if (currentSubscription.price == 0) {
                CustomSnackBar.showSnackBar(
                    binding.root,
                    MessageStore.freeSubscription(this)
                )
                return@setOnClickListener
            }


            Intent(this, ReviewSummaryActivity::class.java).apply {
                putExtra("SUBSCRIPTION_ID", currentSubscription.id)
                putExtra("FROM", "SettingsSubscription")
                startActivity(this)
            }
        }
    }

    private fun observeViewModel() {
        individualViewModel.subscriptionsResult.observe(this) { result ->
            if (result.status == true) {
                handleSubscriptionResults(result)
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        individualViewModel.loading.observe(this) { isLoading ->
            if (isLoading == true) progressBar.show() else progressBar.hide()
        }

        individualViewModel.toast.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSubscriptionData() {
        individualViewModel.getSubscriptionsData()
    }

    private fun handleSubscriptionResults(result: SubscriptionsModal) {
        val subscription = result.data?.subscription
        binding.cancelSubscriptionBtn.visibility =
            if (!subscription?.id.isNullOrEmpty()) View.VISIBLE else View.INVISIBLE

        subscription?.let {
            binding.daysRemainingTv.text = "${it.remainingDays} days remaining"
            binding.subscriptionTypeTv.text = it.name
            Glide.with(this)
                .load(it.image)
                .placeholder(R.drawable.ic_standard)
                .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.faded_round_btn)))
                .into(binding.ivSubscription)
        }

        subscriptionsList = result.data?.plans.orEmpty()
        if (subscriptionsList.isNotEmpty()) {
            setupViewPager()
        }
    }

    private fun setupViewPager() {
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
            setDotIndicatorColor(ContextCompat.getColor(this@SubscriptionActivity, R.color.blue))
            setStrokeDotsIndicatorColor(ContextCompat.getColor(this@SubscriptionActivity, R.color.grey))
            setViewPager2(binding.subscriptionsVp)
        }
    }
}
