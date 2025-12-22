package com.thehotelmedia.android.activity.userTypes.individual

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityUploadMenuBinding
import com.thehotelmedia.android.extensions.getMimeType
import com.thehotelmedia.android.modals.userProfile.UserProfileModel
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class UploadMenuActivity : BaseActivity() {

    private lateinit var binding: ActivityUploadMenuBinding
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private lateinit var preferenceManager: PreferenceManager
    private var userId: String = ""
    private var businessProfileId: String = ""
    private val selectedFiles = mutableListOf<Uri>()
    private val filePaths = mutableListOf<String>()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                if (data.clipData != null) {
                    // Multiple files selected
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        data.clipData!!.getItemAt(i)?.uri?.let { uri ->
                            selectedFiles.add(uri)
                            saveFileFromUri(uri)
                        }
                    }
                } else {
                    // Single file selected
                    data.data?.let { uri ->
                        selectedFiles.add(uri)
                        saveFileFromUri(uri)
                    }
                }
                updateSelectedFilesUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager.getInstance(this)
        userId = intent.getStringExtra("USER_ID") ?: ""
        
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)

        initUI()
    }

    private fun initUI() {
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.selectFilesBtn.setOnClickListener {
            openFilePicker()
        }

        binding.uploadBtn.setOnClickListener {
            if (filePaths.isNotEmpty()) {
                uploadMenu()
            } else {
                CustomSnackBar.showSnackBar(binding.root, "Please select at least one file")
            }
        }

        // Get user profile to fetch businessProfileId
        if (userId.isNotEmpty()) {
            individualViewModal.getUserProfileById(userId)
        }

        individualViewModal.userProfileByIdResult.observe(this) { result ->
            if (result?.status == true) {
                businessProfileId = result.data?.businessProfileID ?: ""
                if (businessProfileId.isEmpty()) {
                    CustomSnackBar.showSnackBar(binding.root, "Business profile not found")
                    finish()
                }
            } else {
                CustomSnackBar.showSnackBar(binding.root, result?.message ?: "Failed to load profile")
            }
        }

        individualViewModal.uploadMenuResult.observe(this) { result ->
            if (result?.status == true) {
                Toast.makeText(this, getString(R.string.menu_uploaded_successfully), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                CustomSnackBar.showSnackBar(binding.root, result?.message ?: "Upload failed")
            }
        }

        individualViewModal.loading.observe(this) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(this) {
            CustomSnackBar.showSnackBar(binding.root, it)
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        filePickerLauncher.launch(intent)
    }

    private fun saveFileFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileName(uri) ?: "menu_${System.currentTimeMillis()}"
            val file = File(cacheDir, fileName)
            
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            filePaths.add(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            CustomSnackBar.showSnackBar(binding.root, "Error saving file: ${e.message}")
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun updateSelectedFilesUI() {
        if (filePaths.isNotEmpty()) {
            binding.selectedFilesTv.text = "${filePaths.size} file(s) selected"
            binding.selectedFilesTv.visibility = View.VISIBLE
            binding.uploadBtn.isEnabled = true
            showPreviews()
        } else {
            binding.selectedFilesTv.visibility = View.GONE
            binding.uploadBtn.isEnabled = false
            clearPreviews()
        }
    }

    private fun clearPreviews() {
        binding.previewContainer.removeAllViews()
        binding.previewContainer.visibility = View.GONE
    }

    private fun showPreviews() {
        binding.previewContainer.removeAllViews()
        binding.previewContainer.visibility = View.VISIBLE

        selectedFiles.forEach { uri ->
            val mimeType = contentResolver.getType(uri) ?: ""
            if (mimeType.startsWith("image/")) {
                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.menu_preview_image_height)
                    ).apply {
                        bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageURI(uri)
                }
                binding.previewContainer.addView(imageView)
            } else {
                val fileName = getFileName(uri) ?: uri.lastPathSegment ?: "menu.pdf"
                val textView = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
                    }
                    text = "$fileName (PDF)"
                    setTextColor(resources.getColor(R.color.text_color, theme))
                    textSize = 14f
                }
                binding.previewContainer.addView(textView)
            }
        }
    }

    private fun uploadMenu() {
        if (businessProfileId.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root, "Business profile ID not available")
            return
        }

        val fileParts = filePaths.map { filePath ->
            val file = File(filePath)
            val mimeType = getMimeType(file) ?: Files.probeContentType(file.toPath()) ?: "application/octet-stream"
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            // Backend expects the multipart field name to be "menu"
            MultipartBody.Part.createFormData("menu", file.name, requestBody)
        }

        individualViewModal.uploadMenu(businessProfileId, fileParts)
    }
}

