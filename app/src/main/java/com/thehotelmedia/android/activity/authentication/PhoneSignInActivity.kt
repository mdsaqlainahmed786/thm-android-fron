package com.thehotelmedia.android.activity.authentication

import android.content.Intent
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.ActivityPhoneSignInBinding
import com.thehotelmedia.android.customDialog.OtpDialogManager
import android.app.Activity

class PhoneSignInActivity : BaseActivity() {

    private lateinit var binding: ActivityPhoneSignInBinding
    private lateinit var otpDialogManager: OtpDialogManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otpDialogManager = OtpDialogManager(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.phoneEt.addTextChangedListener {
            if (!it.isNullOrBlank()) {
                binding.phoneEt.error = null
            }
        }

        binding.sendOtpBtn.setOnClickListener {
            val dialCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            val phoneNumber = binding.phoneEt.text?.toString()?.trim().orEmpty()

            if (isValidPhoneNumber(phoneNumber)) {
                startOtpFlow(dialCode, phoneNumber)
            } else {
                binding.phoneEt.error = getString(com.thehotelmedia.android.R.string.contact_number_small)
                CustomSnackBar.showSnackBar(binding.root, getString(com.thehotelmedia.android.R.string.contact_number_small))
            }
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length in 7..15 && phone.all { it.isDigit() }
    }

    private fun startOtpFlow(dialCode: String, phoneNumber: String) {
        otpDialogManager.startPhoneVerificationFlow(
            initialDialCode = dialCode,
            initialPhoneNumber = phoneNumber
        ) { verifiedDialCode, verifiedPhoneNumber ->
            onOtpVerified(verifiedDialCode, verifiedPhoneNumber)
        }
    }

    private fun onOtpVerified(dialCode: String, phoneNumber: String) {
        CustomSnackBar.showSnackBar(binding.root, "Phone verified successfully.")

        val resultIntent = Intent().apply {
            putExtra(EXTRA_DIAL_CODE, dialCode)
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_DIAL_CODE = "extra_dial_code"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }
}

