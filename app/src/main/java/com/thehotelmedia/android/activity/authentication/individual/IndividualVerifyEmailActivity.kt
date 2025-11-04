package com.thehotelmedia.android.activity.authentication.individual

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.Constants.N_A
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityIndividualVerifyEmailBinding
import com.thehotelmedia.android.extensions.getAndroidDeviceId
import com.thehotelmedia.android.modals.authentication.individual.verifyEmail.IndividualVerifyEmailModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class IndividualVerifyEmailActivity : BaseActivity() {

    private lateinit var binding: ActivityIndividualVerifyEmailBinding
    private var userEmailAddress : String = ""
    private var deviceId : String = ""
    private var fcmToken : String = N_A
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@IndividualVerifyEmailActivity
    private lateinit var preferenceManager : PreferenceManager

    // Define the countdown time (in milliseconds)
    private val resendOtpTimerDuration: Long = 60000 // 60 seconds


    private var retryCount = 0
    private val maxRetries = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIndividualVerifyEmailBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initUI()
        startResendOtpTimer() // Start the timer when the activity is created

    }
//    private fun getFcmToken() {
//        FirebaseMessaging.getInstance().token
//            .addOnCompleteListener { task ->
//                if (!task.isSuccessful) {
//                    Log.w("FCM_TOEKN", "Fetching FCM registration token failed", task.exception)
//                    return@addOnCompleteListener
//                }
//                // Get the token
//                fcmToken = task.result
//                Log.d("FCM_TOEKN", "FCM Registration Token: $fcmToken")
//                // Now you can send this token to your server or use it as needed
//            }
//    }


    private fun getFcmTokenWithRetry() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Success: Token mil gaya
                    val token = task.result
                    Log.d("FCM_TOKEN", "FCM Registration Token: $token")
                    fcmToken = token
                    retryCount = 0 // Reset retry count
                    return@addOnCompleteListener
                }

                // Fail: Retry if limit not reached
                Log.w("FCM_TOKEN", "Attempt ${retryCount + 1} failed", task.exception)
                retryCount++
                if (retryCount < maxRetries) {
                    getFcmTokenWithRetry() // Try again
                } else {
                    Log.e("FCM_TOKEN", "Failed after $maxRetries attempts")
                }
            }
    }


    private fun initUI() {
//        getFcmToken()
        getFcmTokenWithRetry()
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
                CustomSnackBar.showSnackBar(binding.root, "Please enter verification code")
            }else{
                verifyEmail(otp)
            }


        }

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

        binding.resendOtp.setOnClickListener {
            resendOtp() // Implement the resend OTP functionality here
        }



        authViewModel.individualVerifyEmailResult.observe(activity){result->
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



    private fun verifyEmail(otp: String) {
        authViewModel.individualVerifyEmail(userEmailAddress,otp,deviceId,fcmToken)
    }

    private fun handleVerifyEmailResult(result: IndividualVerifyEmailModal) {

        val userId = result.data?.id.toString()
        val accessToken = result.data?.accessToken.toString()
        val refreshToken = result.data?.refreshToken.toString()
        val fullName = result.data?.fullName.toString()
        val email = result.data?.email.toString()
        val dialCode = result.data?.dialCode.toString()
        val phoneNumber = result.data?.phoneNumber.toString()

        val cookies = "SessionToken=$refreshToken; UserDeviceID=$deviceId; X-Access-Token= $accessToken"

        preferenceManager.putString(PreferenceManager.Keys.COOKIES, cookies)
        preferenceManager.putString(PreferenceManager.Keys.USER_ID, userId)
        preferenceManager.putString(PreferenceManager.Keys.ACCESS_TOKEN, accessToken)
        preferenceManager.putString(PreferenceManager.Keys.REFRESH_TOKEN, refreshToken)
        preferenceManager.putString(PreferenceManager.Keys.USER_FULL_NAME, fullName)
        preferenceManager.putString(PreferenceManager.Keys.USER_EMAIL, email)
        preferenceManager.putString(PreferenceManager.Keys.USER_DIAL_CODE, dialCode)
        preferenceManager.putString(PreferenceManager.Keys.USER_PHONE_NUMBER, phoneNumber)
        preferenceManager.putString(PreferenceManager.Keys.BUSINESS_TYPE, business_type_individual)



        navigateToMediaActivity()

    }

    private fun navigateToMediaActivity() {
        val intent = Intent(this, IndividualMediaActivity::class.java)
        startActivity(intent)
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
        authViewModel.reSendOtp(userEmailAddress,"email-verification")
    }


}