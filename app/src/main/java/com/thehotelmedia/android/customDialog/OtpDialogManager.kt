package com.thehotelmedia.android.customDialog

import android.app.AlertDialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.DialogSendOtpBinding
import com.thehotelmedia.android.databinding.DialogVerifyOtpBinding
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class OtpDialogManager(private val context: Context) {

    private var sendOtpDialog: AlertDialog? = null
    private var verifyOtpDialog: AlertDialog? = null
    private var selectedCountryCode: String = "+91"
    private val resendOtpTimerDuration: Long = 60000 // 60 seconds
    private val progressBar = CustomProgressBar(context)
    private lateinit var individualViewModal: IndividualViewModal

    init {
        // Ensure context is a ViewModelStoreOwner before initializing ViewModel
        if (context is ViewModelStoreOwner) {
            val individualRepo = IndividualRepo(context.applicationContext)
            individualViewModal = ViewModelProvider(
                context,
                ViewModelFactory(null, individualRepo, null)
            )[IndividualViewModal::class.java]
        } else {
            throw IllegalArgumentException("Context must be a ViewModelStoreOwner")
        }
    }

    /**
     * Show Send OTP Dialog
     */
    fun showSendOtpDialog(number: String, onProceed: (String, String) -> Unit) {
        val binding = DialogSendOtpBinding.inflate(LayoutInflater.from(context))
        sendOtpDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

        binding.contactEt.setText(number)

        // Set country code change listener
        binding.countryCodePicker.setOnCountryChangeListener {
            selectedCountryCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
        }

        binding.proceedBtn.setOnClickListener {
            val phoneNumber = binding.contactEt.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                individualViewModal.sentOtpToNumber(selectedCountryCode, phoneNumber)
            } else {
//                binding.contactEt.error = "Enter a valid phone number"
                context.showToast("Enter a valid phone number")
            }
        }

        // Observe LiveData safely
        (context as? LifecycleOwner)?.let { lifecycleOwner ->
            individualViewModal.sentOtpToNumberResult.observe(lifecycleOwner) { result ->
                progressBar.hide()
                if (result.status == true) {
                    context.showToast(result.message.toString())
                    sendOtpDialog?.dismiss()
                    onProceed(selectedCountryCode, binding.contactEt.text.toString().trim())
                } else {
                    context.showToast(result.message.toString())
                }
            }

            individualViewModal.otpLoading.observe(lifecycleOwner) { isLoading ->
                if (isLoading) progressBar.show() else progressBar.hide()
            }

//            individualViewModal.otpToast.observe(lifecycleOwner) {
//                context.showToast(it.toString())
//            }
        }

        sendOtpDialog?.show()
    }

    /**
     * Show Verify OTP Dialog
     */
    fun showVerifyOtpDialog(dialCode: String, number: String, onOtpVerified: (String, String) -> Unit) {
        val binding = DialogVerifyOtpBinding.inflate(LayoutInflater.from(context))
        val phoneNumber = "$dialCode $number"

        // Mask last 5 digits
        val maskedPhoneNumber = phoneNumber.take(phoneNumber.length - 5) + "*****"
        binding.phoneTextView.text = maskedPhoneNumber

        verifyOtpDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

        binding.nextBtn.setOnClickListener {
            val otp = binding.otpEt.text.toString().trim()
            if (otp.isNotEmpty()) {
                individualViewModal.verifyNumberWithOtp(dialCode, number, otp)
            } else {
//                binding.otpEt.error = "Enter a valid OTP"
                context.showToast("Enter a valid OTP")
            }
        }

        binding.resendOtp.setOnClickListener {
            individualViewModal.sentOtpToNumber(dialCode, number)
        }

        // Observe LiveData safely
        (context as? LifecycleOwner)?.let { lifecycleOwner ->
            individualViewModal.verifyNumberWithOtpResult.observe(lifecycleOwner) { result ->
                progressBar.hide()
                if (result.status == true) {
                    context.showToast(result.message.toString())
                    verifyOtpDialog?.dismiss()
                    onOtpVerified(dialCode, number)
                } else {
                    context.showToast(result.data?.message.toString())
                }
            }

            individualViewModal.sentOtpToNumberResult.observe(lifecycleOwner) { result ->
                progressBar.hide()
                if (result.status == true) {
                    startResendOtpTimer(binding.resendOtp)
                } else {
                    context.showToast(result.message.toString())
                }
            }

            individualViewModal.otpLoading.observe(lifecycleOwner) { isLoading ->
                if (isLoading) progressBar.show() else progressBar.hide()
            }

//            individualViewModal.otpToast.observe(lifecycleOwner) {
//                context.showToast(it.toString())
//            }
        }

        startResendOtpTimer(binding.resendOtp) // Start the timer when the dialog is shown

        verifyOtpDialog?.show()
    }

    /**
     * Start the countdown timer for the resend OTP button
     */
    private fun startResendOtpTimer(resendOtp: TextView) {
        resendOtp.isEnabled = false // Disable the button
        object : CountDownTimer(resendOtpTimerDuration, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update the TextView with the remaining time
                val secondsRemaining = millisUntilFinished / 1000
                resendOtp.text = "${context.getString(R.string.resend_otp_in)} $secondsRemaining ${context.getString(R.string.sec)}"
            }

            override fun onFinish() {
                // Once the timer finishes, enable the resend button and reset text
                resendOtp.text = context.getString(R.string.resend_otp)
                resendOtp.isEnabled = true
            }
        }.start()
    }
}
