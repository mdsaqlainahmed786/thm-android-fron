package com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customDialog.OtpDialogManager
import com.thehotelmedia.android.databinding.ActivityIndividualEditProfileDetailsBinding
import com.thehotelmedia.android.extensions.setEmailTextWatcher
import com.thehotelmedia.android.extensions.toggleEnable
import com.thehotelmedia.android.modals.editProfile.EditProfileModal
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class IndividualEditProfileDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityIndividualEditProfileDetailsBinding
    private var from : String = ""
    private var description : String = ""
    private var isPasswordVisible = false

    private val activity = this@IndividualEditProfileDetailsActivity

    private lateinit var preferenceManager : PreferenceManager
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var otpDialogManager: OtpDialogManager

    private var fullName : String = ""
    private var email : String = ""
    private var dialCode : String = ""
    private var phoneNumber : String = ""
    private var bio : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIndividualEditProfileDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        val progressBar = CustomProgressBar(activity)

        otpDialogManager = OtpDialogManager(this)

        preferenceManager = PreferenceManager.getInstance(activity)
        getDataSetData()

        binding.emailEt.setEmailTextWatcher()
        binding.emailEt.isEnabled = false

        from = intent.getStringExtra("TYPE").toString()
        description = intent.getStringExtra("DESCRIPTION").toString()

        binding.titleTv.text = from
        binding.descriptionTv.text = description
        when (from) {
            "Name" -> {
                binding.doneBtn.toggleEnable(true)
                updateVisibility(nameVisible = true)
            }
            "Email" -> {
                binding.doneBtn.toggleEnable(false)
                updateVisibility(emailVisible = true)
            }
            "Contact Number" -> {
                binding.doneBtn.toggleEnable(false)
                updateVisibility(contactVisible = true)
            }
//            "Password" -> updateVisibility(passwordVisible = true)
            "Bio" -> {
                binding.doneBtn.toggleEnable(true)
                updateVisibility(bioVisible = true)
            }
        }

        binding.contactEt.setOnClickListener {
            otpDialogManager.showSendOtpDialog (phoneNumber){ newDialCode,newPhoneNumber ->
                otpDialogManager.showVerifyOtpDialog(newDialCode,newPhoneNumber) { finalDialCode,finalPhoneNumber ->
                    dialCode = finalDialCode
                    phoneNumber = finalPhoneNumber

                    preferenceManager.putString(PreferenceManager.Keys.USER_DIAL_CODE,finalDialCode)
                    preferenceManager.putString(PreferenceManager.Keys.USER_PHONE_NUMBER,finalPhoneNumber)
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // Set a listener for country code change
        binding.countryCodePicker.setOnCountryChangeListener {
            dialCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            // Update the ImageView with the selected country's flag
            binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
            // Optionally, set the selected country code to the EditText
        }

        binding.doneBtn.setOnClickListener {
            if (validateInputs()) {
                editProfile()
                // Add logic to save data or proceed further
            }
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        individualViewModal.editProfileResult.observe(activity){result->
            if (result.status==true){

                Handler(Looper.getMainLooper()).postDelayed({
                    handelEditProfileData(result)
                }, 800) // 1-second delay
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }

        individualViewModal.loading.observe(activity){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        individualViewModal.toast.observe(activity){
//            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
            CustomSnackBar.showSnackBar(binding.root,it)

        }
    }

    private fun handelEditProfileData(result: EditProfileModal?) {
        preferenceManager.putString(PreferenceManager.Keys.USER_FULL_NAME,fullName)
        preferenceManager.putString(PreferenceManager.Keys.USER_DESCRIPTION,bio)
        onBackPressedDispatcher.onBackPressed()
    }

    private fun editProfile() {
        fullName = binding.nameEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        phoneNumber = binding.contactEt.text.toString().trim()
        bio = binding.bioEt.text.toString().trim()
        individualViewModal.editProfile(fullName,email,dialCode, phoneNumber, bio)
    }

    private fun updateVisibility(
        nameVisible: Boolean = false,
        emailVisible: Boolean = false,
        contactVisible: Boolean = false,
//        passwordVisible: Boolean = false,
        bioVisible: Boolean = false
    ) {
        binding.nameLayout.visibility = if (nameVisible) View.VISIBLE else View.GONE
        binding.emailLayout.visibility = if (emailVisible) View.VISIBLE else View.GONE
        binding.contactLayout.visibility = if (contactVisible) View.VISIBLE else View.GONE
//        binding.passwordLayout.visibility = if (passwordVisible) View.VISIBLE else View.GONE
        binding.bioLayout.visibility = if (bioVisible) View.VISIBLE else View.GONE
    }

    private fun getDataSetData() {
        fullName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME, "").toString()
        email = preferenceManager.getString(PreferenceManager.Keys.USER_EMAIL, "").toString()
        dialCode = preferenceManager.getString(PreferenceManager.Keys.USER_DIAL_CODE, "").toString()
        phoneNumber = preferenceManager.getString(PreferenceManager.Keys.USER_PHONE_NUMBER, "").toString()
        bio = preferenceManager.getString(PreferenceManager.Keys.USER_DESCRIPTION, "").toString()

        if (dialCode.isNotEmpty()){
            binding.countryCodePicker.setCountryForPhoneCode(dialCode.toInt())
            val flagResId = binding.countryCodePicker.selectedCountryFlagResourceId
            // Set the flag in the ImageView
            binding.countryFlagImageView.setImageResource(flagResId)
        }
        binding.contactEt.setText(phoneNumber)

//        binding.passwordEt.isEnabled = false
//        binding.passwordEt.setHint("* * * * * * * * * *")
        binding.nameEt.setText(fullName)
        binding.emailEt.setText(email)
        binding.bioEt.setText(bio)

    }

    private fun validateInputs(): Boolean {
        // Validate Name
        if (binding.nameLayout.visibility == View.VISIBLE) {
            val name = binding.nameEt.text.toString()
            if (name.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseEnterYourName(this))
                return false
            }
        }
        // Validate Email
        if (binding.emailLayout.visibility == View.VISIBLE) {
            val email = binding.emailEt.text.toString()
            if (email.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseEnterYourEmail(this))
                return false
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseEnterValidEmail(this))
                return false
            }
        }
        // Validate Contact Number
        if (binding.contactLayout.visibility == View.VISIBLE) {
            val contactNumber = binding.contactEt.text.toString()
            if (contactNumber.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseEnterContactNumber(this))
                return false
            }
            if (!contactNumber.matches(Regex("\\d{10,15}"))) {  // Validates contact number (10 to 15 digits)
                CustomSnackBar.showSnackBar(binding.root,MessageStore.enterValid10DigitNumber(this))
                return false
            }
        }
        // Validate Bio
        if (binding.bioLayout.visibility == View.VISIBLE) {
            val bio = binding.bioEt.text.toString()
            if (bio.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseEnterYourBio(this))
                return false
            }
        }
        // If all validations pass
        return true
    }

}