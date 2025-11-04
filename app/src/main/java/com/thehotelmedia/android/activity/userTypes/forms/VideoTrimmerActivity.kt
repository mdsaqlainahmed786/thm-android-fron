package com.thehotelmedia.android.activity.userTypes.forms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.*
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_VIDEO_LENGTH
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.databinding.ActivityVideoTrimmerBinding
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.io.File

class VideoTrimmerActivity : BaseActivity() {

    companion object {
        private const val VIDEO_URI_KEY = "video_uri"
        private const val REQUEST_WRITE_PERMISSION = 1001
        private const val MIN_VIDEO_DURATION = 5
    }

    private lateinit var binding: ActivityVideoTrimmerBinding
    private var videoUri: Uri? = null
    private var from: String = ""
    private var businessesType: String = ""
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var progressBar: CustomProgressBar
    private lateinit var giffProgressBar: GiffProgressBar
    private lateinit var successGiff: SuccessGiff
    private lateinit var individualViewModal: IndividualViewModal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoTrimmerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUri = intent.getStringExtra(VIDEO_URI_KEY)?.let { Uri.parse(it) }
        from = intent.getStringExtra("FROM") ?: ""

        if (videoUri == null) {
            CustomSnackBar.showSnackBar(binding.root, "Invalid video URI")
            finish()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            checkStoragePermission()
        }

        initUI()
    }

    // No external trimmer: we pass through the original video, or upload it directly for stories
    private val startResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    private fun initUI() {
        preferenceManager = PreferenceManager.getInstance(this)
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]

        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "") ?: ""
        var videoDuration = preferenceManager.getInt(PreferenceManager.Keys.VIDEO_DURATION_INT, DEFAULT_VIDEO_LENGTH)

        giffProgressBar = GiffProgressBar(this)
        successGiff = SuccessGiff(this)
        progressBar = CustomProgressBar(this)

        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (from == "CreateStory") {
            videoDuration = 30
        }

        // Immediately proceed without trimming
        videoUri?.let { uri ->
            if (from == "CreateStory") {
                val videoFile = copyUriToTempFile(uri)
                if (videoFile != null) {
                    individualViewModal.createStory(null, videoFile)
                } else {
                    showToast("Unable to prepare video")
                    finish()
                    return
                }
            } else {
                val resultIntent = Intent().apply {
                    putExtra("trimmed_video_uri", uri.toString())
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        individualViewModal.createStoryResult.observe(this) { result ->
            if (result.status == true) {
                result.message?.let { msg ->
                    runOnUiThread {
                        successGiff.show(msg) {
                            navigateToMainActivity(businessesType == business_type_individual)
                        }
                    }
                }
            } else {
                CustomSnackBar.showSnackBar(binding.root, result.message.orEmpty())
            }
        }

        individualViewModal.loading.observe(this) { isLoading ->
            if (isLoading == true) giffProgressBar.show() else giffProgressBar.hide()
        }

        individualViewModal.toast.observe(this) {
            CustomSnackBar.showSnackBar(binding.root, it)
        }
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            CustomSnackBar.showSnackBar(binding.root, MessageStore.permissionRequiredToSaveVideo(this))
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("thm_video_", ".mp4", cacheDir)
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
