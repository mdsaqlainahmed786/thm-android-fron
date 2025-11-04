package com.thehotelmedia.android.activity.authentication.business

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.authentication.business.ImageAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customDialog.PhotoVideoDialog
import com.thehotelmedia.android.databinding.ActivityBusinessTypeMediaBinding
import com.thehotelmedia.android.extensions.getMimeType
import com.thehotelmedia.android.modals.authentication.business.uploadPropertyPictureModal.UploadPropertyPictureModal
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class BusinessTypeMediaActivity : BaseActivity() {

    private lateinit var binding: ActivityBusinessTypeMediaBinding
//    private var selectedBusinessType : String = ""
    private lateinit var adapter: ImageAdapter
    private val propertyImageList = mutableListOf<Uri>()
    private val formatedPropertyImageList = mutableListOf<String>()
    private var logoImage : String = ""
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@BusinessTypeMediaActivity
    private lateinit var preferenceManager : PreferenceManager

    private var propertyImageLimit = 10

    private lateinit var imageUri: Uri


    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            propertyImageList.add(imageUri)
            adapter.notifyDataSetChanged()
            Log.d("SelectedImages", "Camera image: $imageUri")
        }
    }

    private fun pickImageFromCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }
    }

    private fun openCamera() {
        val imageFile = File(externalCacheDir, "camera_image_${System.currentTimeMillis()}.jpg")
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile)
        cameraLauncher.launch(imageUri)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
            uris?.let { uriList ->



                if (propertyImageList.size + uriList.size > propertyImageLimit) {
                    Toast.makeText(this, getString(R.string.image_limit_with_number, propertyImageLimit), Toast.LENGTH_SHORT).show()
                } else {
                    propertyImageList.addAll(uriList)
                    adapter.notifyDataSetChanged()
                    Log.d("SelectedImages", "Selected images URIs: $propertyImageList")
                }


            }
        }


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

                binding.logoIv.setImageURI(resultUri)
                logoImage = resultUri.path.toString()
//                val file = File(getRealPathFromURI(resultUri)!!)
//                val mediaType: okhttp3.MediaType? = "image/*".toMediaTypeOrNull()
//                val requestBody: RequestBody = file.asRequestBody(mediaType)
//                val pic = MultipartBody.Part.createFormData("profile_pic", file.name, requestBody)
//                updateProfilePic(pic)
                Log.d("CroppedImage", "Cropped image URI: $resultUri")
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            // Handle crop error
            Log.e("CropError", "Error cropping image: $cropError")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBusinessTypeMediaBinding.inflate(layoutInflater)

        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        val progressBar = CustomProgressBar(activity)
//        selectedBusinessType = intent.getStringExtra("SELECTED_BUSINESS_TYPE") ?: ""
//        setUpUi()

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            if (logoImage.isEmpty()){
                CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseSelectLogo(this))
            }else if (propertyImageList.isEmpty()){
                CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseSelectPropertyPicture(this))
            }else{
                uploadMedia()
//            val intent = Intent(this, SupportingDocumentsActivity::class.java)
//            startActivity(intent)
            }

        }

        binding.logoIv.setOnClickListener {
            pickLogoImageFromGallery()
        }


        adapter = ImageAdapter(propertyImageList, {
            // Handle plus icon click
//            pickImageFromGallery()
//            openCamera()
            showPhotoVideoDialog()
        }, { position ->
            // Handle cancel icon click
            if (position < propertyImageList.size) {
                propertyImageList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, propertyImageList.size)
            }
        })

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter


        authViewModel.updateIndividualProfilePicResult.observe(activity){result->
            if (result.status==true){
                val userSmallProfilePic = result?.data?.profilePic?.small.toString()
                val userMediumProfilePic = result?.data?.profilePic?.medium.toString()
                val userLargeProfilePic = result?.data?.profilePic?.large.toString()
                preferenceManager.putString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, userSmallProfilePic)
                preferenceManager.putString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, userMediumProfilePic)
                preferenceManager.putString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, userLargeProfilePic)
                uploadPropertyPictures()
//                handleProfilePicResult(result)
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }
        authViewModel.uploadPropertyPictureResult.observe(activity){result->
            if (result.status==true){
//                uploadPropertyPictures()
                handleProfilePicResult(result)
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


    private fun showPhotoVideoDialog() {
        // Create an instance of PhotoVideoDialog
        val dialog = PhotoVideoDialog(
            activity = this,
            title = "Upload",
            photoText = "Gallery",
            videoText = "Camera",
            photoIconResId = R.drawable.ic_gallery_blue,
            videoIconResId = R.drawable.ic_camera_blue,
            photoClickListener = { pickImageFromGallery() },
            videoClickListener = { pickImageFromCamera() },
            onDismissListener = {
                // Handle dialog dismiss event
//                println("Dialog dismissed")
            },
            autoCancel = true
        )
        // Show the dialog
        dialog.show()
    }

    private fun uriToFile(uri: Uri): File? {
        val inputStream = contentResolver.openInputStream(uri)
        return inputStream?.use { stream ->
            val tempFile = File.createTempFile("upload", ".jpg") // Create a temporary file
            tempFile.outputStream().use { outputStream ->
                stream.copyTo(outputStream) // Copy the input stream to the temporary file
            }
            tempFile // Return the temporary file
        } ?: run {
            Log.e("uriToFile", "Could not open InputStream for URI: $uri")
            null
        }
    }

    private fun uploadPropertyPictures() {
        propertyImageList.forEach { uri ->
            uriToFile(uri)?.let { formatedPropertyImageList.add(it.toString()) }
        }

        val imageParts = formatedPropertyImageList.map { imagePath ->
            val validPath = imagePath.replace("file:", "")
            val file = File(validPath)
            val mimeType = Files.probeContentType(file.toPath()) ?: "image/*" // Dynamic MIME type
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("images", file.name, requestBody)
        }

        if (imageParts.isNotEmpty()) {
            authViewModel.uploadPropertyPicture(imageParts)
        } else {
            Log.e("UploadMedia", "No images to upload.")
        }
    }

    private fun handleProfilePicResult(result: UploadPropertyPictureModal) {

//        val userSmallProfilePic = result?.data?.profilePic?.small.toString()
//        val userMediumProfilePic = result?.data?.profilePic?.medium.toString()
//        val userLargeProfilePic = result?.data?.profilePic?.large.toString()
//        preferenceManager.putString(PreferenceManager.Keys.USER_SMALL_PROFILE_PIC, userSmallProfilePic)
//        preferenceManager.putString(PreferenceManager.Keys.USER_MEDIUM_PROFILE_PIC, userMediumProfilePic)
//        preferenceManager.putString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, userLargeProfilePic)

        //handelProperty Picture here

        val intent = Intent(this, SupportingDocumentsActivity::class.java)
        startActivity(intent)


    }


    private fun uploadMedia() {
        // Convert the logoImage Uri to File and then to MultipartBody.Part
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



    private fun pickLogoImageFromGallery() {
        logoLauncher.launch("image/*")
    }

    private fun pickImageFromGallery() {
        galleryLauncher.launch("image/*")
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




}