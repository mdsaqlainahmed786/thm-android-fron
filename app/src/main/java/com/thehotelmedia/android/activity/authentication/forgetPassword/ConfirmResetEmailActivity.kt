package com.thehotelmedia.android.activity.authentication.forgetPassword

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityConfirmResetEmailBinding
import com.thehotelmedia.android.extensions.getAndroidDeviceId
import com.thehotelmedia.android.modals.authentication.forgetPassword.forgetPassword.ForgetPasswordModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class ConfirmResetEmailActivity : BaseActivity() {

    private lateinit var binding: ActivityConfirmResetEmailBinding

    private var userEmailAddress : String = ""
    private var deviceId : String = ""
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@ConfirmResetEmailActivity
    private lateinit var preferenceManager : PreferenceManager

    // Define the countdown time (in milliseconds)
    private val resendOtpTimerDuration: Long = 60000 // 60 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmResetEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
        startResendOtpTimer() // Start the timer when the activity is created
    }

    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        userEmailAddress = intent.getStringExtra("EMAIL_ADDRESS") ?: ""
        val progressBar = CustomProgressBar(activity) // 'this' refers to the context
        deviceId = activity.getAndroidDeviceId()

        binding.emailTv.text = userEmailAddress

        binding.verificationCodeEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // When EditText is focused
                binding.verificationCodeLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_focused)
            } else {
                // When EditText loses focus
                binding.verificationCodeLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_normal)
            }
        }


        binding.nextBtn.setOnClickListener {
            val otp = binding.verificationCodeEt.text.toString()
            if (otp.isEmpty()){
                CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterVerificationCode(this))
            }else{
                verifyForgotEmail(otp)
            }


        }

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

        binding.resendOtp.setOnClickListener {
            resendOtp() // Implement the resend OTP functionality here
        }



        authViewModel.verifyForgotPasswordResult.observe(activity){result->
            if (result.status==true){
                handleVerifyEmailResult(result)
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }
        authViewModel.reSendOtpResult.observe(activity){result->
            if (result.status==true){
                startResendOtpTimer()
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        authViewModel.loading.observe(activity){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        authViewModel.toast.observe(activity){
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }


    }

    private fun verifyForgotEmail(otp: String) {
        authViewModel.verifyForgotPassword(userEmailAddress,otp)
    }

    private fun handleVerifyEmailResult(result: ForgetPasswordModal) {

        val email = result.data?.email.toString()
        val resetToken = result.data?.resetToken.toString()

        val intent = Intent(this, ConfirmPasswordActivity::class.java)
        intent.putExtra("EMAIL_ADDRESS", email)
        intent.putExtra("RESET_TOKEN", resetToken)
        startActivity(intent)
        finish()

    }



    // Method to start the countdown timer for the resend OTP
    private fun startResendOtpTimer() {
        binding.resendOtp.isEnabled = false // Disable the button
        object : CountDownTimer(resendOtpTimerDuration, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                // Update the TextView with the remaining time
                val secondsRemaining = millisUntilFinished / 1000
                binding.resendOtp.text = "${getString(R.string.resend_otp_in)} $secondsRemaining ${getString(R.string.sec)}"
            }

            override fun onFinish() {
                // Once the timer finishes, enable the resend button and set the text to "Resend OTP"
                binding.resendOtp.text = getString(R.string.resend_otp)
                binding.resendOtp.isEnabled = true // Enable the button
            }
        }.start()
    }

    // Method to handle the resend OTP logic
    private fun resendOtp() {
        authViewModel.reSendOtp(userEmailAddress,"forgot-password")
    }

}