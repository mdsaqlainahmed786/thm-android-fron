package com.thehotelmedia.android.activity.authentication.individual

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.LANGUAGE_CODE
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.ActivityIndividualInfoBinding
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.setEmailTextWatcher
import com.thehotelmedia.android.modals.authentication.individual.signUp.IndividualSignUpModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

data class ValidationResult(val isValid: Boolean, val errorMessage: String?)
class IndividualInfoActivity : BaseActivity() {

    private lateinit var locationHelper: LocationHelper
    private var userLat = 0.0
    private var userLng = 0.0
    // Define the permission launcher as a class property
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Permissions granted, proceed to fetch location
                locationHelper.checkAndRequestLocation()
            } else {
                userLat = DEFAULT_LAT
                userLng = DEFAULT_LNG
            }
        }

    private lateinit var binding: ActivityIndividualInfoBinding
    private var isPasswordVisible = false
    private var selectedCountryCode = "+91"
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@IndividualInfoActivity



    private var selectedProfessions: String = ""
    private var finalSelectedProfession: String = ""
    private var currentLanguage = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIndividualInfoBinding.inflate(layoutInflater)

        setContentView(binding.root)
        checkLocationPermission()
        initUI()

    }

    private fun checkLocationPermission() {
        // Initialize the LocationHelper with required callbacks
        locationHelper = LocationHelper(
            context = this,
            permissionLauncher = permissionLauncher,
            locationCallback = { latitude, longitude ->
                userLat = latitude
                userLng = latitude
            },
            errorCallback = { errorMessage ->
                // Handle error callback
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        // Now check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }


    private fun initUI() {

        val onBoarding = getSharedPreferences("ONBOARD", MODE_PRIVATE)
        currentLanguage = onBoarding.getString(LANGUAGE_CODE, "en").orEmpty()

        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(activity) // 'this' refers to the context
        setPasswordEt()

        getProfessions()
        binding.emailEt.setEmailTextWatcher()

        // Set a listener for country code change
        binding.countryCodePicker.setOnCountryChangeListener {
            selectedCountryCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            // Update the ImageView with the selected country's flag
            binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
            println("Selected Country Code: $selectedCountryCode")
            // Optionally, set the selected country code to the EditText
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

        binding.nextBtn.setOnClickListener {
//            val intent = Intent(this, IndividualVerifyEmailActivity::class.java)
//            intent.putExtra("EMAIL_ADDRESS", binding.emailEt.text.toString())
//            startActivity(intent)
            val result = validateFields(binding.nameEt, binding.emailEt, binding.passwordEt, binding.contactEt)
            if (result.isValid) {

                individualLogin()

                // Proceed with form submission
            } else {
                // Focus will automatically be set on the first invalid field
                CustomSnackBar.showSnackBar(binding.root, result.errorMessage.toString())
            }
        }

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

        authViewModel.individualSignUpResult.observe(activity){result->
            if (result.status==true){
                handleIndividualLoginResult(result)
            }else{
                val msg = result.message
                Toast.makeText(activity,msg,Toast.LENGTH_SHORT).show()
            }
        }


        authViewModel.professionResult.observe(this) { result ->
            if (result.status == true) {
                val professionData = result.professionData
                val professions = professionData.map { dataItem ->
                    Businesses(dataItem.name.toString(), "", "")
                }
                setProfessionsAdapter(professions)
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(activity,it,Toast.LENGTH_SHORT).show()
        }
    }

    private fun getProfessions() {
        authViewModel.getProfession()
    }

    private fun setProfessionsAdapter(professions: List<Businesses>) {
        binding.businessTypesTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.blured_background))
        binding.businessTypesTv.dropDownVerticalOffset = binding.businessTypesTv.height + 30

        val businessTypeAdapter = BusinessTypeAdapter(this, professions)
        binding.businessTypesTv.setAdapter(businessTypeAdapter)
        binding.businessTypesTv.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as Businesses
            binding.businessTypesTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))
            val selectedOption = selectedItem.name
            selectedProfessions = selectedOption

            if (selectedProfessions == "Others"){
                binding.otherProfessionEt.visibility = View.VISIBLE
            }else{
                binding.otherProfessionEt.visibility = View.GONE
            }
        }
    }

    private fun handleIndividualLoginResult(result: IndividualSignUpModal?) {
        val email = result?.data?.email
        val intent = Intent(this, IndividualVerifyEmailActivity::class.java)
        intent.putExtra("EMAIL_ADDRESS", email)
        startActivity(intent)
    }

    private fun individualLogin() {
        val fullName = binding.nameEt.text.toString().trim()
        val email = binding.emailEt.text.toString().trim()
        val password = binding.passwordEt.text.toString().trim()
        val phoneNumber = binding.contactEt.text.toString().trim()
        authViewModel.individualSignUp("individual",email,selectedCountryCode,phoneNumber,fullName,password,finalSelectedProfession,userLat,userLng,currentLanguage)
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

        // Check if selectedProfessions is empty
        if (selectedProfessions.isBlank()) {
            return ValidationResult(false, MessageStore.selectProfession(this))
        }
        if (selectedProfessions == "Others"){
            finalSelectedProfession = binding.otherProfessionEt.text.toString().trim()
            if (finalSelectedProfession.isBlank()){
                return ValidationResult(false, MessageStore.enterProfession(this))
            }
        }else{
           finalSelectedProfession = selectedProfessions
        }


        return ValidationResult(true, null)
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


}