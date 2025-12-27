package com.thehotelmedia.android.activity.authentication.business

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.TermsAndConditionsActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.ActivitySupportingDocumentsBinding
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

class SupportingDocumentsActivity : BaseActivity() {

    private lateinit var binding: ActivitySupportingDocumentsBinding
    private lateinit var documentType: String  // Store the document type here
    private lateinit var authViewModel: AuthViewModel
    private var businessDoc: String = ""
    private var addressDoc: String = ""
    private val activity = this@SupportingDocumentsActivity
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val STORAGE_PERMISSION_REQUEST_CODE = 1002

    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val savedUri = saveImageToStorage(imageBitmap)
                savedUri?.let {
                    openCropImageScreen(it)
                }
            }
        }

    private val galleryLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val savedUri = saveImageToStorageFromUri(it)
                savedUri?.let {
                    openCropImageScreen(it)
                }
            }
        }

    private val pdfPickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { pdfUri ->
                    Log.d("PDFPicker", "PDF Uri: $pdfUri")
                    val file = getFileFromUri(pdfUri)
                    if (file != null) {
                        if (documentType == "Business") {
                            binding.businessDocIv.setImageURI(null)
                            binding.businessTv.text = file.name
                            businessDoc = file.path
                        } else {
                            binding.addressDocIv.setImageURI(null)
                            binding.addressTv.text = file.name
                            addressDoc = file.path
                        }
                    }
                }
            }
        }

    private val cropActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                    if (documentType == "Business") {
                        binding.businessDocIv.setImageURI(resultUri)
                        businessDoc = resultUri.path.toString()

                    } else if (documentType == "Address") {
                        binding.addressDocIv.setImageURI(resultUri)
                        addressDoc = resultUri.path.toString()
                    }
                    Log.d("CroppedImage", "Cropped image URI: $resultUri")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportingDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val authRepo = AuthRepo(this)
        authViewModel =
            ViewModelProvider(this, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(this)

        // Add "(Optional)" text to the main title
        val supportingDocumentsText = getString(R.string.supporting_documents) + " (Optional)"
        binding.textView2.text = supportingDocumentsText
        
        // Set the individual labels without "(Optional)"
        binding.textView3.text = getString(R.string.business_registration)
        binding.address.text = getString(R.string.address_proof)

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            // Documents are now optional - allow proceeding with or without documents
            if (businessDoc.isEmpty() && addressDoc.isEmpty()) {
                // No documents provided - skip API call and navigate directly
                val intent = Intent(activity, TermsAndConditionsActivity::class.java)
                intent.putExtra("From", "Business")
                startActivity(intent)
            } else {
                // At least one document provided - upload documents
                uploadDocuments()
            }
        }

        binding.businessDocIv.setOnClickListener {
            showFileChooserDialog("Business")
        }
        binding.addressDocIv.setOnClickListener {
            showFileChooserDialog("Address")
        }


        authViewModel.supportingDocumentsResult.observe(activity){result->
            if (result.status==true){
                val intent = Intent(activity, TermsAndConditionsActivity::class.java)
                intent.putExtra("From", "Business")
                startActivity(intent)
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

    private fun showFileChooserDialog(type: String) {
        documentType = type
        val options = arrayOf("Gallery", "Camera", "File")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openGallery()
                1 -> openCamera()
                2 -> {
                    if (isPermissionGranted()) {
                        openFilePicker()
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
        builder.show()
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openFilePicker() {
        val pdfPickerIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pdfPickerLauncher.launch(Intent.createChooser(pdfPickerIntent, "Select PDF"))
    }
//
//    private fun openCamera() {
//        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        cameraLauncher.launch(cameraIntent)
//    }
//
//    private fun openGallery() {
//        galleryLauncher.launch("image/*")
//    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            // Request camera permission
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

//    private fun openGallery() {
//        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            Manifest.permission.READ_MEDIA_IMAGES
//        } else {
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        }
//
//        if (ContextCompat.checkSelfPermission(this, permission)
//            == PackageManager.PERMISSION_GRANTED) {
//            galleryLauncher.launch("image/*")
//        } else {
//            // Request storage permission
//            ActivityCompat.requestPermissions(
//                this, arrayOf(permission), STORAGE_PERMISSION_REQUEST_CODE
//            )
//        }
//    }
    private fun openGallery() {
    galleryLauncher.launch("image/*")
//        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            Manifest.permission.READ_MEDIA_IMAGES
//        } else {
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        }
//
//        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
//            // Permission granted, open gallery
//            galleryLauncher.launch("image/*")
//        } else {
//            // Request storage permission
//            ActivityCompat.requestPermissions(
//                this, arrayOf(permission), STORAGE_PERMISSION_REQUEST_CODE
//            )
//        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, open camera
                    openCamera()
                } else {
                    // Permission denied, show a message
                    CustomSnackBar.showSnackBar(binding.root,MessageStore.cameraPermissionRequired(this))
                }
            }
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, open gallery
                    openGallery()
                } else {
                    // Permission denied, show a message

                    CustomSnackBar.showSnackBar(binding.root,MessageStore.storagePermissionRequired(this))
                }
            }
        }
    }

    private fun saveImageToStorage(bitmap: Bitmap): Uri? {
        val imagesDir =
            File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "profile_image.jpg")
        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            return Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun saveImageToStorageFromUri(uri: Uri): Uri? {
        val imagesDir =
            File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val imageFile = File(imagesDir, "profile_image.jpg")
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
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
        UCrop.of(
            uri,
            Uri.fromFile(File(cacheDir, "cropped_image_${System.currentTimeMillis()}.jpg"))
        )
            .withAspectRatio(210f, 297f)
            .start(this, cropActivityResultLauncher)
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(cacheDir, contentResolver.getFileName(uri))
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        return file
    }

    fun android.content.ContentResolver.getFileName(uri: Uri): String {
        var name = "default_name.pdf"
        val cursor = query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name =
                    it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }

    private fun uploadDocuments() {
        // Create multipart parts only if documents are provided
        val businessMultipart: MultipartBody.Part? = if (businessDoc.isNotEmpty()) {
            val businessFile = File(businessDoc)
            val businessMimeType = getMimeType(businessFile)
            val businessRequestBody = RequestBody.create(businessMimeType.toMediaTypeOrNull(), businessFile)
            MultipartBody.Part.createFormData("businessRegistration", businessFile.name, businessRequestBody)
        } else {
            null
        }

        val addressMultipart: MultipartBody.Part? = if (addressDoc.isNotEmpty()) {
            val addressFile = File(addressDoc)
            val addressMimeType = getMimeType(addressFile)
            val addressRequestBody = RequestBody.create(addressMimeType.toMediaTypeOrNull(), addressFile)
            MultipartBody.Part.createFormData("addressProof", addressFile.name, addressRequestBody)
        } else {
            null
        }

        authViewModel.supportingDocuments(businessMultipart, addressMultipart)
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.toLowerCase()) {
            "jpg", "jpeg", "png" -> "image/${file.extension}"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream" // Default MIME type
        }
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted, open the file picker
                openFilePicker()
            } else {
                // Permission is denied, show a message to the user
                CustomSnackBar.showSnackBar(binding.root,MessageStore.permissionDeniedCannotAccessFile(this))
            }
        }
}
