package com.thehotelmedia.android.activity.authentication.individual

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.SignInActivity
import com.thehotelmedia.android.activity.authentication.TermsAndConditionsActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityIndividualMediaBinding
import com.thehotelmedia.android.extensions.getMimeType
import com.thehotelmedia.android.modals.authentication.individual.uploadProfilePic.IndividualProfilePicModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

class IndividualMediaActivity : BaseActivity() {

    private lateinit var binding: ActivityIndividualMediaBinding
    private var imageFile: File? = null
    private var userAcceptedTerms: Boolean = false
    private lateinit var authViewModel: AuthViewModel
    private lateinit var preferenceManager: PreferenceManager
    private val activity = this@IndividualMediaActivity

    private val galleryLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val savedUri = saveImageToStorageFromUri(it)
                savedUri?.let { uri ->
                    openCropImageScreen(uri)
                }
            }
        }

    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val savedUri = saveImageToStorage(imageBitmap)
                savedUri?.let { uri ->
                    openCropImageScreen(uri)
                }
            }
        }

    private val cropActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                    updateImageView(resultUri)
                    imageFile = File(resultUri.path!!)
                    Log.d("CroppedImage", "Cropped image URI: $resultUri")
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Log.e("CropError", "Error cropping image: $cropError")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIndividualMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel =
            ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(activity)
        preferenceManager = PreferenceManager.getInstance(activity)
        userAcceptedTerms = PreferenceManager.getInstance(activity)
            .getBoolean(PreferenceManager.Keys.USER_ACCEPTED_TERMS, false)

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            if (imageFile == null) {
                CustomSnackBar.showSnackBar(binding.root, "Please select profile picture")
            } else {
                uploadProfilePic()
            }
        }
        binding.skipBtn.setOnClickListener {
            if (!userAcceptedTerms) {
                moveToTermsAndCondition()
            } else {
                moveToBottomNavigationIndividual()
            }
        }
        binding.cameraLayout.setOnClickListener {
            checkCameraPermission()
        }
        binding.galleryLayout.setOnClickListener {
//            checkGalleryPermission()
            pickImageFromGallery()
        }

        authViewModel.updateIndividualProfilePicResult.observe(activity) { result ->
            if (result.status == true) {
                handleProfilePicResult(result)
            } else {
                val msg = result.message
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.loading.observe(activity) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        authViewModel.toast.observe(activity) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleProfilePicResult(result: IndividualProfilePicModal?) {
        val userSmallProfilePic = result?.data?.profilePic?.small.toString()
        val userMediumProfilePic = result?.data?.profilePic?.medium.toString()
        val userLargeProfilePic = result?.data?.profilePic?.large.toString()
        preferenceManager.putString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, userSmallProfilePic)
        preferenceManager.putString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, userMediumProfilePic)
        preferenceManager.putString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, userLargeProfilePic)

        if (!userAcceptedTerms) {
            moveToTermsAndCondition()
        } else {
            moveToBottomNavigationIndividual()
        }
    }

    private fun moveToTermsAndCondition() {
        val intent = Intent(this, TermsAndConditionsActivity::class.java)
        intent.putExtra("From", business_type_individual)
        startActivity(intent)
    }

    private fun moveToBottomNavigationIndividual() {
        val intent = Intent(this, BottomNavigationIndividualMainActivity::class.java)
        startActivity(intent)
    }

    private fun uploadProfilePic() {

        imageFile?.let { file ->

            // Get dynamic MIME type
            val mimeType = getMimeType(file) ?: "application/octet-stream" // Default if unknown
            // Create RequestBody with dynamic MIME type
            val requestFile = RequestBody.create(mimeType.toMediaTypeOrNull(), file)
            // Create MultipartBody.Part
            val body = MultipartBody.Part.createFormData("profilePic", file.name, requestFile)
            // Call the ViewModel function to update the profile picture
            authViewModel.updateIndividualProfilePic(body)
        } ?: run {
            CustomSnackBar.showSnackBar(binding.root,MessageStore.noImageSelected(this))
        }

    }

    // Permission handling for gallery access
    private fun checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSION_CODE
            )
        } else {
            pickImageFromGallery()
        }
    }

    // Permission handling for camera access
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            captureImageFromCamera()
        }
    }

    // Handle the permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    captureImageFromCamera()
                } else {
                    CustomSnackBar.showSnackBar(binding.root,MessageStore.cameraPermissionDenied(this))
                }
            }
            GALLERY_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery()
                } else {
                    CustomSnackBar.showSnackBar(binding.root,MessageStore.galleryPermissionDenied(this))
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun captureImageFromCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun saveImageToStorageFromUri(uri: Uri): Uri? {
        val imagesDir = File(this.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "profile_image_${System.currentTimeMillis()}.jpg")
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

    private fun saveImageToStorage(bitmap: Bitmap): Uri? {
        val imagesDir = File(this.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "profile_image_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(imageFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
            return Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun openCropImageScreen(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage${System.currentTimeMillis()}.jpg"))
        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .start(this@IndividualMediaActivity, cropActivityResultLauncher)
    }

    private fun updateImageView(uri: Uri) {
        binding.profileIv.setImageURI(uri)
    }

    companion object {
        const val CAMERA_PERMISSION_CODE = 100
        private const val GALLERY_PERMISSION_CODE = 101
    }
}
