package com.thehotelmedia.android.activity.authentication.business

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.individual.ValidationResult
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customDialog.OtpDialogManager
import com.thehotelmedia.android.databinding.ActivityManagerInfoBinding
import com.thehotelmedia.android.extensions.setEmailTextWatcher
import com.thehotelmedia.android.modals.authentication.business.businessSignUp.BusinessSignUpModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class ManagerInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityManagerInfoBinding
    private var selectedCountryCode : String = "+91"

    private var isPasswordVisible = false
    private lateinit var otpDialogManager: OtpDialogManager

    private lateinit var businessName: String
    private lateinit var businessDialCode: String
    private lateinit var businessPhoneNumber: String
    private lateinit var businessWebsiteLink: String
    private lateinit var businessEmail: String
    private lateinit var businessGst: String
    private lateinit var selectedBusinessType: String
    private lateinit var selectedBusinessId: String
    private lateinit var selectedSubBusinessType: String
    private lateinit var selectedSubBusinessId: String
    private lateinit var businessDescription: String
    private lateinit var street: String
    private lateinit var city: String
    private lateinit var state: String
    private lateinit var zipcode: String
    private lateinit var country: String
    private lateinit var placeID: String
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@ManagerInfoActivity

    private var isBusinessPhoneVerified: Boolean = false
    private var verifiedBusinessDialCode: String = ""
    private var verifiedBusinessPhoneNumber: String = ""

    private var isManagerPhoneVerified: Boolean = false
    private var verifiedManagerDialCode: String = ""
    private var verifiedManagerPhoneNumber: String = ""




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManagerInfoBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(activity) // 'this' refers to the context
        otpDialogManager = OtpDialogManager(activity)
        binding.emailEt.setEmailTextWatcher()


        businessName = intent.getStringExtra("BUSINESS_NAME").orEmpty()
        businessDialCode = intent.getStringExtra("BUSINESS_DIAL_CODE").orEmpty()
        businessPhoneNumber = intent.getStringExtra("BUSINESS_PHONE_NUMBER").orEmpty()
        businessWebsiteLink = intent.getStringExtra("BUSINESS_WEBSITE_LINK").orEmpty()
        businessEmail = intent.getStringExtra("BUSINESS_EMAIL").orEmpty()
        businessGst = intent.getStringExtra("BUSINESS_GST").orEmpty()
        selectedBusinessType = intent.getStringExtra("SELECTED_BUSINESS_TYPE").orEmpty()
        selectedBusinessId = intent.getStringExtra("SELECTED_BUSINESS_ID").orEmpty()
        selectedSubBusinessType = intent.getStringExtra("SELECTED_SUB_BUSINESS_TYPE").orEmpty()
        selectedSubBusinessId = intent.getStringExtra("SELECTED_SUB_BUSINESS_ID").orEmpty()
        businessDescription = intent.getStringExtra("BUSINESS_DESCRIPTION").orEmpty()
        placeID = intent.getStringExtra("PLACED_ID").orEmpty()
        street = intent.getStringExtra("STREET") ?: "Mohali"
        city = intent.getStringExtra("CITY") ?: "Mohali"
        state = intent.getStringExtra("STATE") ?: "Mohali"
        zipcode = intent.getStringExtra("ZIPCODE") ?: "140055"
        country = intent.getStringExtra("COUNTRY") ?: "India"
        latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        longitude = intent.getDoubleExtra("LONGITUDE", 0.0)

        isBusinessPhoneVerified = intent.getBooleanExtra("BUSINESS_PHONE_VERIFIED", false)
        verifiedBusinessDialCode = intent.getStringExtra("VERIFIED_BUSINESS_DIAL_CODE").orEmpty()
        verifiedBusinessPhoneNumber = intent.getStringExtra("VERIFIED_BUSINESS_PHONE_NUMBER").orEmpty()



        setPasswordEt()

        // Set a listener for country code change
        binding.countryCodePicker.setOnCountryChangeListener {
            selectedCountryCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            // Update the ImageView with the selected country's flag
            binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
            println("Selected Country Code: $selectedCountryCode")
            // Optionally, set the selected country code to the EditText
            resetManagerPhoneVerification()
        }

        binding.contactEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // When EditText is focused
                binding.contactLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_focused)
            } else {
                // When EditText loses focus
                binding.contactLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_normal)
            }
        }
        binding.contactEt.addTextChangedListener {
            val current = it?.toString()?.trim().orEmpty()
            if (isManagerPhoneVerified && current != verifiedManagerPhoneNumber) {
                resetManagerPhoneVerification()
            }
        }


        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            val result = validateFields(binding.nameEt, binding.emailEt, binding.passwordEt, binding.contactEt)
            if (result.isValid) {
                // Safety: Manager screen should only be reachable after business phone OTP verification.
                if (!isBusinessPhoneVerified || verifiedBusinessPhoneNumber.isBlank() || verifiedBusinessDialCode.isBlank()) {
                    CustomSnackBar.showSnackBar(binding.root, "Please verify the business contact number first.")
                    return@setOnClickListener
                }

                val dialCode = selectedCountryCode
                val phoneNumber = binding.contactEt.text.toString().trim()
                if (isManagerPhoneVerified && verifiedManagerDialCode == dialCode && verifiedManagerPhoneNumber == phoneNumber) {
                    businessSignUp()
                } else {
                    otpDialogManager.startPhoneVerificationForNumber(
                        dialCode = dialCode,
                        phoneNumber = phoneNumber,
                        dismissOnSuccess = true
                    ) { verifiedDialCode, verifiedPhone ->
                        isManagerPhoneVerified = true
                        verifiedManagerDialCode = verifiedDialCode
                        verifiedManagerPhoneNumber = verifiedPhone
                        businessSignUp()
                    }
                }

            } else {
                CustomSnackBar.showSnackBar(binding.root, result.errorMessage.toString())
            }
        }



        authViewModel.businessSignUpResult.observe(this) { result ->
            if (result.status == true) {
                handelBusinessSignUp(result)

            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.loading.observe(this) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        authViewModel.toast.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }



    }

    private fun handelBusinessSignUp(result: BusinessSignUpModal?) {
        val email = result?.data?.email
        val intent = Intent(this, BusinessVerifyEmailActivity::class.java)
        intent.putExtra("EMAIL_ADDRESS", email)
        startActivity(intent)
    }


    private fun validateFields(
        nameEt: AppCompatEditText,
        emailEt: AppCompatEditText,
        passwordEt: AppCompatEditText,
        contactEt: AppCompatEditText
    ): ValidationResult {
        val fields = listOf(
            nameEt to ::validateName,
            emailEt to ::validateEmail,
            passwordEt to ::validatePassword,
            contactEt to ::validateContact
        )

        for ((editText, validator) in fields) {
            val result = validator(editText)
            if (!result.isValid) {
//                editText.error = result.errorMessage
                editText.requestFocus()
                return ValidationResult(false, result.errorMessage)
            }
        }

        return ValidationResult(true, null)
    }

    private fun validateName(editText: EditText): ValidationResult {
        val text = editText.text.toString().trim()
        return if (text.isEmpty()) {
            ValidationResult(false, MessageStore.enterFullName(this))
        } else {
            ValidationResult(true, null)
        }
    }

    private fun validateEmail(editText: EditText): ValidationResult {
        val text = editText.text.toString().trim()
        return when {
            text.isEmpty() -> ValidationResult(false, MessageStore.enterEmailAddress(this))
            !Patterns.EMAIL_ADDRESS.matcher(text).matches() -> ValidationResult(false, MessageStore.enterValidEmailAddress(this))
            else -> ValidationResult(true, null)
        }
    }
//    private fun validatePassword(editText: EditText): ValidationResult {
//        val text = editText.text.toString().trim()
//        return when {
//            text.isEmpty() -> ValidationResult(false, "Enter password.")
//            text.length < 6 -> ValidationResult(false, "Password must be at least 6 characters.")
//            else -> ValidationResult(true, null)
//        }
//    }
private fun validatePassword(editText: EditText): ValidationResult {
    val password = editText.text.toString().trim()

    return when {
        password.isEmpty() -> ValidationResult(false, MessageStore.enterPassword(this))
        password.length < 8 -> ValidationResult(false, MessageStore.passwordAtLeast8Characters(this))
        !password.contains(Regex("[A-Z]")) -> ValidationResult(false, MessageStore.passwordOneUppercase(this))
        !password.contains(Regex("[a-z]")) -> ValidationResult(false, MessageStore.passwordOneLowercase(this))
        !password.contains(Regex("[0-9]")) -> ValidationResult(false, MessageStore.passwordOneNumber(this))
        !password.contains(Regex("[!@#\$%^&*(),.?\":{}|<>]")) -> ValidationResult(false, MessageStore.passwordOneSpecialCharacter(this))
        else -> ValidationResult(true, null)
    }
}

    private fun validateContact(editText: EditText): ValidationResult {
        val text = editText.text.toString().trim()

        return when {
            text.isEmpty() -> ValidationResult(false, MessageStore.enterContactNumber(this))
            text.length != 10 -> ValidationResult(false, MessageStore.contactShouldBe10Digits(this))  // Check for 10 digits
            !Patterns.PHONE.matcher(text).matches() -> ValidationResult(false, MessageStore.enterValidContactNumber(this))
            else -> ValidationResult(true, null)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setPasswordEt() {
        binding.passwordEt.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.passwordEt.right - binding.passwordEt.compoundDrawables[DRAWABLE_END].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    togglePasswordVisibility(isPasswordVisible, binding.passwordEt)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
    private fun togglePasswordVisibility(isVisible: Boolean, editText: AppCompatEditText) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_password_icon,
                0,
                R.drawable.ic_show_password,
                0
            )
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_password_icon,
                0,
                R.drawable.ic_hide_password,
                0
            )
        }
        editText.setSelection(editText.text?.length ?: 0) // Keep the cursor at the end of the text
    }

    private fun businessSignUp() {
        if (!isManagerPhoneVerified || verifiedManagerDialCode.isBlank() || verifiedManagerPhoneNumber.isBlank()) {
            CustomSnackBar.showSnackBar(binding.root, "Please verify your contact number.")
            return
        }
        val name = binding.nameEt.text.toString().trim()
        val email = binding.emailEt.text.toString().trim()
        val password = binding.passwordEt.text.toString().trim()
        authViewModel.businessSignUp(email,name,password,verifiedManagerDialCode,verifiedManagerPhoneNumber,businessName,businessEmail,businessDialCode,
            businessPhoneNumber,selectedBusinessId,selectedSubBusinessId,businessDescription,businessWebsiteLink,businessGst,street,city,state,country,
            zipcode,latitude.toString(),longitude.toString(),"business",placeID)
    }

    private fun resetManagerPhoneVerification() {
        isManagerPhoneVerified = false
        verifiedManagerDialCode = ""
        verifiedManagerPhoneNumber = ""
    }



}