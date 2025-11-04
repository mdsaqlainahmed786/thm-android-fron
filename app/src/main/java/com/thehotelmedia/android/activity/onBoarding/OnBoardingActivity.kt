package com.thehotelmedia.android.activity.onBoarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager.widget.ViewPager
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.TransparentBaseActivity
import com.thehotelmedia.android.activity.authentication.SignInActivity
import com.thehotelmedia.android.adapters.onBoarding.OnboardTextViewpagerAdapter
import com.thehotelmedia.android.adapters.onBoarding.OnboardViewpagerAdapter
import com.thehotelmedia.android.databinding.ActivityOnBoardingBinding

class OnBoardingActivity : TransparentBaseActivity() {
    private lateinit var binding: ActivityOnBoardingBinding
    private var onboardVp: ViewPager? = null
    private var textVp: ViewPager? = null
    private var adapter: OnboardViewpagerAdapter? = null
    private var textAdapter: OnboardTextViewpagerAdapter? = null
    private var dotsIndicator: SpringDotsIndicator? = null
    private var activity = this@OnBoardingActivity

    private val handler = Handler()
    private var isAutoScroll = true
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyInsetsListener(R.id.onBoardingMain)
        binding.Prog.animate().rotationBy(-90f).setDuration(0).start()
        updateProgress(0)
//        startLogoAnimation()
        initUI()
    }

    // Helper method to convert dp to pixels
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun initUI() {
        onboardVp = binding.onboardVp
        textVp = binding.textViewpager
        dotsIndicator = findViewById(R.id.indicator_layout)
        dotsIndicator?.dotsClickable = false
        dotsIndicator?.setDotIndicatorColor(ContextCompat.getColor(this, R.color.blue))
        dotsIndicator?.setStrokeDotsIndicatorColor(ContextCompat.getColor(this, R.color.grey))

        // Initialize adapters
        adapter = OnboardViewpagerAdapter(activity)
        textAdapter = OnboardTextViewpagerAdapter(activity)
        textVp?.adapter = textAdapter
        onboardVp?.adapter = adapter
        dotsIndicator?.attachTo(onboardVp!!)
        dotsIndicator?.attachTo(textVp!!)

        // Set initial page to the first page
        onboardVp?.currentItem = 0
        textVp?.currentItem = 0

        // Setup Page Change Listeners
        setupPageChangeListeners()

        // Handle button clicks
        binding.btnNext.setOnClickListener {
            if (currentPage < 3) {
                currentPage++
                onboardVp?.setCurrentItem(currentPage, true)
                textVp?.setCurrentItem(currentPage, true)
                updateProgress(currentPage)

            } else {
                val onBoarding = getSharedPreferences("ONBOARD", MODE_PRIVATE)
                val editor = onBoarding.edit()
                editor.putBoolean("FIRST_TIME", false)
                editor.apply()
                val intent = Intent(activity, SignInActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        binding.btnSkip.setOnClickListener {
            val intent = Intent(activity, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupPageChangeListeners() {
        binding.textViewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateProgress(position)
                binding.onboardVp.currentItem = position
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })

        binding.onboardVp.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateProgress(position)
                binding.textViewpager.currentItem = position
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun updateProgress(position: Int) {
        val progress = when (position) {
            0 -> 25
            1 -> 50
            2 -> 75
            else -> 100
        }
        binding.Prog.progress = progress
    }
}
