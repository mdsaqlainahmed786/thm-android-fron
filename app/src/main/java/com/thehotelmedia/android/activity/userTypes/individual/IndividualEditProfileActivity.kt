package com.thehotelmedia.android.activity.userTypes.individual

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.IndividualEditProfileDetailsActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.IndividualBillingAddressActivity
import com.thehotelmedia.android.activity.userTypes.profile.EditAmenitiesActivity
import com.thehotelmedia.android.activity.userTypes.profile.EditCategoryActivity
import com.thehotelmedia.android.activity.userTypes.profile.VerifyChangePasswordActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityIndividualEditProfileBinding
import com.thehotelmedia.android.extensions.getMimeType
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream


class IndividualEditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityIndividualEditProfileBinding
    private lateinit var preferenceManager : PreferenceManager
    private var type = ""
    private var email = ""
    private var address = ""
    private val activity = this@IndividualEditProfileActivity
    private var logoImage : String = ""
    private val logoLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { it ->
                // Save the selected image to a file
                val savedUri = saveImageToStorageFromUri(it)
                // Display the selected image in the ImageView
                savedUri?.let {
                    openCropImageScreen(it)
                }
            }
        }

    private val cropActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                // Now you have the cropped image URI, you can print it or do whatever you want with it

//                binding.profileIv.setImageURI(resultUri)
                logoImage = resultUri.path.toString()
                uploadMedia()

            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            // Handle crop error
            Log.e("CropError", "Error cropping image: $cropError")
        }
    }


    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIndividualEditProfileBinding.inflate(layoutInflater)

        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackBtnClick()
            }
        })



        initUI()
    }

    private fun onBackBtnClick() {
        // Get the updated name from the user's input or use a default name
        val updatedName = "EditProfile" // Replace this with the actual updated name
        // Prepare the result to send back
        val resultIntent = Intent()
        resultIntent.putExtra("updatedName", updatedName)
        // Set the result and close the activity
        setResult(Activity.RESULT_OK, resultIntent)
        // Finish the activity
        finish()
    }

    override fun onResume() {
        super.onResume()
        initUI()
    }



    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(activity)
        preferenceManager = PreferenceManager.getInstance(activity)
        type = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        if (type == "Individual"){
            binding.categoryLayout.visibility = View.GONE
            binding.amenitiesLayout.visibility = View.GONE
            binding.userTypeLayout.visibility = View.INVISIBLE
            binding.billingAddressLayout.visibility = View.VISIBLE
            binding.menuLayout.visibility = View.GONE
        }else{
            binding.categoryLayout.visibility = View.VISIBLE
            binding.amenitiesLayout.visibility = View.VISIBLE
            binding.userTypeLayout.visibility = View.VISIBLE
            binding.billingAddressLayout.visibility = View.GONE
            // Show menu option only for restaurants
            val businessName = preferenceManager.getString(PreferenceManager.Keys.USER_BUSINESS_NAME, "").toString()
            val isRestaurant = businessName.equals(getString(R.string.restaurant), ignoreCase = true)
            binding.menuLayout.visibility = if (isRestaurant) View.VISIBLE else View.GONE
        }
        getAndSetData()


        binding.backBtn.setOnClickListener {
            onBackBtnClick()
        }
        binding.editProfileBtn.setOnClickListener {
            pickLogoImageFromGallery()
        }
        binding.categoryBtn.setOnClickListener {
            val intent = Intent(this, EditCategoryActivity::class.java)
            startActivity(intent)
        }
        binding.amenitiesBtn.setOnClickListener {
            val intent = Intent(this, EditAmenitiesActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.menuBtn.setOnClickListener {
            // Get business profile ID from user profile
            val userId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()
            if (userId.isNotEmpty()) {
                val intent = Intent(this, UploadMenuActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        }
        binding.individualBillingAddressBtn.setOnClickListener {
            val intent = Intent(this, IndividualBillingAddressActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.editNameBtn.setOnClickListener {
            moveToEditDetails("Name","Enter your full name below, and we'll use it to personalize your experience.")
        }
        binding.editEmailBtn.setOnClickListener {
            moveToEditDetails("Email", "Please provide your email address below, and we'll use it to send you important updates and notifications.")
        }
        binding.contactBtn.setOnClickListener {
            moveToEditDetails("Contact Number", "Please enter your contact number below. We'll use it to reach out to you if necessary.")
        }
        binding.passwordBtn.setOnClickListener {
            val intent = Intent(this, VerifyChangePasswordActivity::class.java)
            intent.putExtra("EMAIL_ADDRESS", email)
            startActivity(intent)
        }
        binding.editBioBtn.setOnClickListener {
            moveToEditDetails("Bio", "Share a little about yourself. This will help others get to know you better.")
        }



        authViewModel.updateIndividualProfilePicResult.observe(activity){result->
            if (result.status==true){
                val profilePic = result.data?.profilePic?.medium
                Glide.with(activity).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
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



    private fun moveToEditDetails(type: String, description: String) {
        val intent = Intent(this, IndividualEditProfileDetailsActivity::class.java)
        intent.putExtra("TYPE", type)
        intent.putExtra("DESCRIPTION", description)
        startActivity(intent)
    }



    private fun getAndSetData() {

        val businessName = preferenceManager.getString(PreferenceManager.Keys.USER_BUSINESS_NAME,"")
        val fullName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME,"")
        val profilePic = preferenceManager.getString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC,"")
        email = preferenceManager.getString(PreferenceManager.Keys.USER_EMAIL,"").toString()
        val dialCode = preferenceManager.getString(PreferenceManager.Keys.USER_DIAL_CODE,"")
        val phoneNumber = preferenceManager.getString(PreferenceManager.Keys.USER_PHONE_NUMBER,"")
        val bio = preferenceManager.getString(PreferenceManager.Keys.USER_DESCRIPTION,"-----")

        val street = preferenceManager.getString(PreferenceManager.Keys.USER_STREET,"")
        val city = preferenceManager.getString(PreferenceManager.Keys.USER_CITY,"")
        val state = preferenceManager.getString(PreferenceManager.Keys.USER_STATE,"")
        val country = preferenceManager.getString(PreferenceManager.Keys.USER_COUNTRY,"")
        val zipCode = preferenceManager.getString(PreferenceManager.Keys.USER_ZIPCODE,"")


        address = if (country.isNullOrEmpty()){
            "-------"
        }else{
            "$street, $city, $state, $country, $zipCode"
        }

        val amenitiesRefList = preferenceManager.getAmenitiesList(PreferenceManager.Keys.AMENITIES_REF_LIST)
        // Null aur empty check ke baad first item remove karo
        if (!amenitiesRefList.isNullOrEmpty()) {
            amenitiesRefList.removeAt(0)
        }
        val amenitiesNames = amenitiesRefList?.joinToString(separator = ",\n") { it.name.toString() }

        Glide.with(activity).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        binding.categoryTv.text = businessName
        binding.fullNameTv.text = fullName
        binding.emailTv.text = email
        binding.contactNumberTv.text = "$dialCode $phoneNumber"
        binding.bioTv.text = bio
        binding.amenitiesTv.text = amenitiesNames
        binding.individualBillingAddressTv.text = address

        if (phoneNumber.isNullOrEmpty()){
            binding.contactLayout.visibility = View.GONE
        }


    }

    private fun pickLogoImageFromGallery() {
        logoLauncher.launch("image/*")
    }

    private fun saveImageToStorageFromUri(uri: Uri): Uri? {
        val imagesDir = File(this.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "logo_image.jpg")
        try {
            this.applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(imageFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    private fun openCropImageScreen(uri: Uri) {
        UCrop.of(uri, Uri.fromFile(File(this.cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg")))
            .withAspectRatio(1f, 1f)
            .start(this, cropActivityResultLauncher)
    }

    private fun uploadMedia() {
//        // Convert the logoImage Uri to File and then to MultipartBody.Part
//        val logoFile = File(logoImage)
//        val logoRequestBody = logoFile.asRequestBody("image/*".toMediaTypeOrNull())
//        val logoPart = MultipartBody.Part.createFormData("profilePic", logoFile.name, logoRequestBody)


        val logoFile = File(logoImage)
// Get dynamic MIME type
        val mimeType = getMimeType(logoFile) ?: "application/octet-stream" // Default type if unknown
// Create RequestBody with dynamic MIME type
        val logoRequestBody = logoFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val logoPart = MultipartBody.Part.createFormData("profilePic", logoFile.name, logoRequestBody)


        authViewModel.updateIndividualProfilePic(logoPart)

    }
}