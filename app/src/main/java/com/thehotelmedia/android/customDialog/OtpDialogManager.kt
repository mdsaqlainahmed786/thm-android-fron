package com.thehotelmedia.android.customDialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.widget.TextView
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.DialogSendOtpBinding
import com.thehotelmedia.android.databinding.DialogVerifyOtpBinding
import com.thehotelmedia.android.extensions.showToast
import java.util.concurrent.TimeUnit

class OtpDialogManager(private val context: Context) {

    private var sendOtpDialog: AlertDialog? = null
    private var verifyOtpDialog: AlertDialog? = null
    private var selectedCountryCode: String = "+91"
    private val resendOtpTimerDuration: Long = 60000 // 60 seconds
    private val progressBar = CustomProgressBar(context)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var onOtpVerifiedCallback: ((String, String) -> Unit)? = null
    private var onCodeSentCallback: ((String, String) -> Unit)? = null
    private var pendingCredential: PhoneAuthCredential? = null
    private var currentDialCode: String = "+91"
    private var currentPhoneNumber: String = ""
    private var verifyOtpBinding: DialogVerifyOtpBinding? = null

    /**
     * Public entry point to start the OTP verification flow.
     */
    fun startPhoneVerificationFlow(
        initialDialCode: String?,
        initialPhoneNumber: String?,
        onOtpVerified: (String, String) -> Unit
    ) {
        val safeDialCode = initialDialCode?.takeIf { it.isNotBlank() } ?: selectedCountryCode
        val safePhoneNumber = initialPhoneNumber?.takeIf { it.isNotBlank() } ?: ""
        showSendOtpDialog(safeDialCode, safePhoneNumber) { dialCode, phoneNumber ->
            showVerifyOtpDialog(dialCode, phoneNumber, onOtpVerified)
        }
    }

    /**
     * Show Send OTP Dialog
     */
    private fun showSendOtpDialog(
        initialDialCode: String,
        number: String,
        onProceed: (String, String) -> Unit
    ) {
        val binding = DialogSendOtpBinding.inflate(LayoutInflater.from(context))
        sendOtpDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

        selectedCountryCode = initialDialCode
        currentDialCode = initialDialCode

        if (number.isNotEmpty()) {
            binding.contactEt.setText(number)
        }

        setCountryOnPicker(binding, initialDialCode)

        // Set country code change listener
        binding.countryCodePicker.setOnCountryChangeListener {
            selectedCountryCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
        }

        binding.proceedBtn.setOnClickListener {
            val phoneNumber = binding.contactEt.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                currentDialCode = selectedCountryCode
                currentPhoneNumber = phoneNumber
                onCodeSentCallback = onProceed
                startPhoneNumberVerification(isResend = false)
            } else {
//                binding.contactEt.error = "Enter a valid phone number"
                context.showToast("Enter a valid phone number")
            }
        }

        sendOtpDialog?.show()
    }

    /**
     * Show Verify OTP Dialog
     */
    private fun showVerifyOtpDialog(
        dialCode: String,
        number: String,
        onOtpVerified: (String, String) -> Unit
    ) {
        val binding = DialogVerifyOtpBinding.inflate(LayoutInflater.from(context))
        verifyOtpBinding = binding
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

        onOtpVerifiedCallback = onOtpVerified

        startResendOtpTimer(binding.resendOtp) // Start the timer when the dialog is shown

        binding.nextBtn.setOnClickListener {
            val otp = binding.otpEt.text.toString().trim()
            if (otp.isNotEmpty()) {
                verifyOtpWithCode(otp)
            } else {
//                binding.otpEt.error = "Enter a valid OTP"
                context.showToast("Enter a valid OTP")
            }
        }

        binding.resendOtp.setOnClickListener {
            if (resendToken != null) {
                onCodeSentCallback = { _, _ -> startResendOtpTimer(binding.resendOtp) }
                startPhoneNumberVerification(isResend = true)
            } else {
                context.showToast("Please wait before requesting a new OTP.")
            }
        }

        verifyOtpDialog?.show()

        // If OTP was auto-retrieved before dialog shown
        pendingCredential?.let { credential ->
            pendingCredential = null
            credential.smsCode?.let { binding.otpEt.setText(it) }
            signInWithPhoneAuthCredential(credential)
        }
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

    private fun setCountryOnPicker(binding: DialogSendOtpBinding, dialCode: String) {
        val countryCodeWithoutPlus = dialCode.replace("+", "")
        countryCodeWithoutPlus.toIntOrNull()?.let {
            binding.countryCodePicker.setCountryForPhoneCode(it)
            binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
        }
    }

    private fun startPhoneNumberVerification(isResend: Boolean) {
        val activity = context as? Activity
            ?: throw IllegalArgumentException("Context must be an Activity to start phone number verification")

        val phoneNumber = formatPhoneNumber(currentDialCode, currentPhoneNumber)
        progressBar.show()

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(resendOtpTimerDuration, TimeUnit.MILLISECONDS)
            .setActivity(activity)
            .setCallbacks(phoneAuthCallbacks)

        if (isResend) {
            resendToken?.let { optionsBuilder.setForceResendingToken(it) }
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            progressBar.hide()
            pendingCredential = credential
            verifyOtpBinding?.otpEt?.setText(credential.smsCode)
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(exception: FirebaseException) {
            progressBar.hide()
            val errorMessage = when (exception) {
                is FirebaseAuthInvalidCredentialsException -> "Invalid phone number. Please check and try again."
                is FirebaseTooManyRequestsException -> "Too many requests. Please try again later."
                else -> exception.localizedMessage ?: "Failed to send OTP."
            }
            context.showToast(errorMessage)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            progressBar.hide()
            this@OtpDialogManager.verificationId = verificationId
            resendToken = token
            context.showToast("OTP sent successfully.")
            sendOtpDialog?.dismiss()
            onCodeSentCallback?.invoke(currentDialCode, currentPhoneNumber)
            onCodeSentCallback = null
        }
    }

    private fun verifyOtpWithCode(otp: String) {
        val verificationId = verificationId
        if (verificationId.isNullOrEmpty()) {
            context.showToast("OTP was not sent. Please try again.")
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        progressBar.show()
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                progressBar.hide()
                if (task.isSuccessful) {
                    context.showToast("OTP verified successfully.")
                    verifyOtpDialog?.dismiss()
                    onOtpVerifiedCallback?.invoke(currentDialCode, currentPhoneNumber)
                    onOtpVerifiedCallback = null
                    pendingCredential = null
                    verificationId = null
                    resendToken = null
                    firebaseAuth.signOut()
                } else {
                    val exception = task.exception
                    val message = if (exception is FirebaseAuthInvalidCredentialsException) {
                        "Invalid OTP. Please try again."
                    } else {
                        exception?.localizedMessage ?: "OTP verification failed."
                    }
                    context.showToast(message)
                }
            }
            .addOnFailureListener { exception ->
                progressBar.hide()
                val message = if (exception is FirebaseAuthInvalidCredentialsException) {
                    "Invalid OTP. Please try again."
                } else {
                    exception.localizedMessage ?: "OTP verification failed."
                }
                context.showToast(message)
            }
    }

    private fun formatPhoneNumber(dialCode: String, phoneNumber: String): String {
        val sanitizedDialCode = if (dialCode.startsWith("+")) dialCode else "+$dialCode"
        val sanitizedNumber = phoneNumber.replace("\\s+".toRegex(), "")
        return sanitizedDialCode + sanitizedNumber
    }
}
