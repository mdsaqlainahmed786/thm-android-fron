package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.os.Bundle
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : BaseActivity() {

    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }
}