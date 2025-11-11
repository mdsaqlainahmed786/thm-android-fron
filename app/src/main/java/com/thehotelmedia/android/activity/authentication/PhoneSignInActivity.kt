package com.thehotelmedia.android.activity.authentication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityPhoneSignInBinding
import com.thehotelmedia.android.databinding.DialogVerifyOtpBinding

class PhoneSignInActivity : BaseActivity() {

    private lateinit var binding: ActivityPhoneSignInBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)

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
                showOtpDialog(dialCode, phoneNumber)
            } else {
                binding.phoneEt.error = getString(com.thehotelmedia.android.R.string.contact_number_small)
                CustomSnackBar.showSnackBar(binding.root, getString(com.thehotelmedia.android.R.string.contact_number_small))
            }
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.length in 7..15 && phone.all { it.isDigit() }
    }

    private fun showOtpDialog(dialCode: String, phoneNumber: String) {
        val dialogBinding = DialogVerifyOtpBinding.inflate(layoutInflater)
        val maskedNumber = "$dialCode $phoneNumber"
        dialogBinding.phoneTextView.text = maskedNumber
        dialogBinding.otpEt.text?.clear()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.resendOtp.setOnClickListener {
            CustomSnackBar.showSnackBar(binding.root, MOCK_OTP_MESSAGE)
        }

        dialogBinding.nextBtn.setOnClickListener {
            val enteredOtp = dialogBinding.otpEt.text?.toString()?.trim().orEmpty()
            if (enteredOtp == MOCK_OTP) {
                dialog.dismiss()
                onOtpVerified(dialCode, phoneNumber)
            } else {
                dialogBinding.otpEt.error = getString(com.thehotelmedia.android.R.string.enter_verification_code_small)
                CustomSnackBar.showSnackBar(binding.root, getString(com.thehotelmedia.android.R.string.enter_verification_code_small))
            }
        }

        dialog.setOnShowListener {
            CustomSnackBar.showSnackBar(binding.root, MOCK_OTP_MESSAGE)
        }

        dialog.show()
    }

    private fun onOtpVerified(dialCode: String, phoneNumber: String) {
        preferenceManager.putString(PreferenceManager.Keys.USER_DIAL_CODE, dialCode)
        preferenceManager.putString(PreferenceManager.Keys.USER_PHONE_NUMBER, phoneNumber)

        CustomSnackBar.showSnackBar(binding.root, "Phone verified successfully.")
        navigateToFeeds()
    }

    private fun navigateToFeeds() {
        val intent = Intent(this, BottomNavigationIndividualMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val MOCK_OTP = "123456"
        private const val MOCK_OTP_MESSAGE = "Mock OTP is $MOCK_OTP"
    }
}

