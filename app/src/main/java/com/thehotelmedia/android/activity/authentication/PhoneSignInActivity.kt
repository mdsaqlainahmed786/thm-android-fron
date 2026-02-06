package com.thehotelmedia.android.activity.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customDialog.OtpDialogManager
import com.thehotelmedia.android.databinding.ActivityPhoneSignInBinding
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.thehotelmedia.android.extensions.getAndroidDeviceId

class PhoneSignInActivity : BaseActivity() {

    private lateinit var binding: ActivityPhoneSignInBinding
    private lateinit var otpDialogManager: OtpDialogManager
    private lateinit var authViewModel: AuthViewModel
    private lateinit var progressBar: CustomProgressBar

    private var pendingDialCode: String = ""
    private var pendingPhoneNumber: String = ""
    private var deviceId: String = ""
    private var notificationToken: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var currentLanguage: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otpDialogManager = OtpDialogManager(this)
        progressBar = CustomProgressBar(this)

        val authRepo = AuthRepo(this)
        authViewModel = ViewModelProvider(this, ViewModelFactory(authRepo))[AuthViewModel::class.java]

        readIntentExtras()
        setupObservers()
        setupListeners()
    }

    private fun readIntentExtras() {
        deviceId = intent.getStringExtra(EXTRA_DEVICE_ID).orEmpty()
        notificationToken = intent.getStringExtra(EXTRA_NOTIFICATION_TOKEN).orEmpty()
        latitude = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        longitude = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        currentLanguage = intent.getStringExtra(EXTRA_LANGUAGE).orEmpty()
    }

    private fun setupObservers() {
        authViewModel.loading.observe(this) { loading ->
            if (loading == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        authViewModel.socialLoginResult.observe(this) { result ->
            if (result?.status == true) {
                otpDialogManager.dismissDialogs()
                val loginJson = Gson().toJson(result)
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_LOGIN_RESULT, loginJson)
                    putExtra(EXTRA_DIAL_CODE, pendingDialCode)
                    putExtra(EXTRA_PHONE_NUMBER, pendingPhoneNumber)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else if (result != null) {
                CustomSnackBar.showSnackBar(binding.root, result.message ?: MessageStore.somethingWentWrong(this))
            }
        }

        authViewModel.toast.observe(this) {
            val msg = it?.trim().orEmpty()
            if (msg.isNotEmpty()) {
                val displayMsg =
                    if (msg.equals("Not Found", ignoreCase = true)) {
                        getString(com.thehotelmedia.android.R.string.no_user_registered_with_this_mobile_number)
                    } else {
                        msg
                    }
                CustomSnackBar.showSnackBar(binding.root, displayMsg)
            }
        }
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
            initialPhoneNumber = phoneNumber,
            dismissOnSuccess = false
        ) { verifiedDialCode, verifiedPhoneNumber ->
            onOtpVerified(verifiedDialCode, verifiedPhoneNumber)
        }
    }

    private fun onOtpVerified(dialCode: String, phoneNumber: String) {
        pendingDialCode = dialCode
        pendingPhoneNumber = phoneNumber
        authViewModel.verifyOtpLogin(
            dialCode = dialCode,
            phoneNumber = phoneNumber,
            deviceID = if (deviceId.isNotEmpty()) deviceId else getAndroidDeviceId(),
            notificationToken = notificationToken,
            lat = latitude,
            lng = longitude,
            language = currentLanguage.ifEmpty { "en" }
        )
    }

    companion object {
        const val EXTRA_DIAL_CODE = "extra_dial_code"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_LOGIN_RESULT = "extra_login_result"
        const val EXTRA_DEVICE_ID = "extra_device_id"
        const val EXTRA_NOTIFICATION_TOKEN = "extra_notification_token"
        const val EXTRA_LAT = "extra_latitude"
        const val EXTRA_LNG = "extra_longitude"
        const val EXTRA_LANGUAGE = "extra_language"
    }
}

