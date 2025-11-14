package com.thehotelmedia.android.activity.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.google.common.util.concurrent.ListenableFuture
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.databinding.ActivityCustomCameraBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CustomCameraActivity : BaseActivity() {

    private lateinit var binding: ActivityCustomCameraBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var camera: Camera? = null
    private var captureMode: CaptureMode = CaptureMode.PHOTO
    private var flashState: FlashState = FlashState.AUTO
    private var recordingTimer: CountDownTimer? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val dataUri = result.data?.data ?: return@registerForActivityResult
            deliverResult(dataUri, resolveMediaType(dataUri))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        setupUi()
        if (hasAllPermissions()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeRecording?.close()
        recordingTimer?.cancel()
        cameraExecutor.shutdown()
    }

    private fun setupUi() = with(binding) {
        val title = intent.getStringExtra(EXTRA_CAMERA_TITLE)
        if (!title.isNullOrBlank()) {
            titleLabel.text = title
        }

        closeButton.setOnClickListener { finish() }
        flashButton.setOnClickListener { toggleFlash() }
        switchCameraButton.setOnClickListener { toggleCamera() }
        galleryButton.setOnClickListener { openGallery() }
        captureButton.setOnClickListener { handleCapture() }
        photoMode.setOnClickListener { setCaptureMode(CaptureMode.PHOTO) }
        videoMode.setOnClickListener { setCaptureMode(CaptureMode.VIDEO) }

        updateModeUi()
        updateFlashIcon()
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindUseCases()
            } catch (ex: Exception) {
                Toast.makeText(this, ex.localizedMessage ?: getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashState.imageCaptureMode)
            .build()

        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.FHD, Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityThan(Quality.SD)
        )

        videoCapture = VideoCapture.withOutput(
            Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
        )

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
            applyFlashStateToTorch()
        } catch (ex: Exception) {
            Toast.makeText(this, ex.localizedMessage ?: getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCapture() {
        when (captureMode) {
            CaptureMode.PHOTO -> takePhoto()
            CaptureMode.VIDEO -> toggleRecording()
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val photoFile = File(cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(this@CustomCameraActivity, exception.message ?: getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    runOnUiThread {
                        deliverResult(savedUri, MEDIA_TYPE_IMAGE)
                    }
                }
            }
        )
    }

    private fun toggleRecording() {
        val capture = videoCapture ?: return
        val currentRecording = activeRecording
        if (currentRecording != null) {
            stopRecording()
            return
        }

        val outputDirectory = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        val videoFile = File(outputDirectory, "camera_video_${System.currentTimeMillis()}.mp4")
        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

        val recording = capture.output
            .prepareRecording(this, fileOutputOptions)
            .apply {
                if (ContextCompat.checkSelfPermission(this@CustomCameraActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this), Consumer { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onRecordingStarted()
                    is VideoRecordEvent.Finalize -> onRecordingFinished(event, Uri.fromFile(videoFile))
                }
            })

        activeRecording = recording
    }

    private fun onRecordingStarted() {
        runOnUiThread {
            binding.recordingTimer.visibility = View.VISIBLE
            binding.recordingProgress.visibility = View.VISIBLE
            binding.recordingProgress.isIndeterminate = true
            binding.actionHint.text = getString(R.string.camera_hint_video)
            recordingTimer?.cancel()
            recordingTimer = object : CountDownTimer(MAX_VIDEO_DURATION_MS, 1000L) {
                var elapsed = 0
                override fun onTick(millisUntilFinished: Long) {
                    elapsed++
                    val minutes = elapsed / 60
                    val seconds = elapsed % 60
                    binding.recordingTimer.text = String.format("%02d:%02d", minutes, seconds)
                }

                override fun onFinish() {
                    stopRecording()
                }
            }.start()
        }
    }

    private fun onRecordingFinished(event: VideoRecordEvent.Finalize, outputUri: Uri) {
        recordingTimer?.cancel()
        activeRecording = null
        runOnUiThread {
            binding.recordingTimer.visibility = View.GONE
            binding.recordingProgress.visibility = View.GONE
            binding.actionHint.text = getString(R.string.camera_hint_photo)
        }

        if (event.hasError()) {
            event.error?.let {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        deliverResult(outputUri, MEDIA_TYPE_VIDEO)
    }

    private fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun setCaptureMode(mode: CaptureMode) {
        if (captureMode == mode) return
        captureMode = mode
        updateModeUi()
        binding.actionHint.text = if (mode == CaptureMode.PHOTO) {
            getString(R.string.camera_hint_photo)
        } else {
            getString(R.string.camera_hint_video)
        }
    }

    private fun updateModeUi() = with(binding) {
        photoMode.isSelected = captureMode == CaptureMode.PHOTO
        videoMode.isSelected = captureMode == CaptureMode.VIDEO
    }

    private fun toggleCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        bindUseCases()
    }

    private fun toggleFlash() {
        flashState = when (flashState) {
            FlashState.AUTO -> FlashState.ON
            FlashState.ON -> FlashState.OFF
            FlashState.OFF -> FlashState.AUTO
        }
        imageCapture?.flashMode = flashState.imageCaptureMode
        applyFlashStateToTorch()
        updateFlashIcon()
    }

    private fun applyFlashStateToTorch() {
        camera?.cameraControl?.enableTorch(flashState == FlashState.ON)
    }

    private fun updateFlashIcon() {
        val tint = when (flashState) {
            FlashState.ON -> ContextCompat.getColor(this, R.color.blue)
            else -> ContextCompat.getColor(this, android.R.color.white)
        }
        binding.flashButton.setColorFilter(tint)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
        galleryLauncher.launch(Intent.createChooser(intent, getString(R.string.gallery)))
    }

    private fun deliverResult(uri: Uri, mediaType: String) {
        val resultIntent = Intent().apply {
            putExtra(RESULT_MEDIA_URI, uri.toString())
            putExtra(RESULT_MEDIA_TYPE, mediaType)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun resolveMediaType(uri: Uri): String {
        val type = contentResolver.getType(uri) ?: return MEDIA_TYPE_IMAGE
        return if (type.startsWith("video")) MEDIA_TYPE_VIDEO else MEDIA_TYPE_IMAGE
    }

    private fun hasAllPermissions(): Boolean =
        REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private enum class CaptureMode {
        PHOTO, VIDEO
    }

    private enum class FlashState(val imageCaptureMode: Int) {
        AUTO(ImageCapture.FLASH_MODE_AUTO),
        ON(ImageCapture.FLASH_MODE_ON),
        OFF(ImageCapture.FLASH_MODE_OFF)
    }

    companion object {
        const val EXTRA_CAMERA_TITLE = "extra_camera_title"
        const val RESULT_MEDIA_URI = "result_media_uri"
        const val RESULT_MEDIA_TYPE = "result_media_type"
        const val MEDIA_TYPE_IMAGE = "image"
        const val MEDIA_TYPE_VIDEO = "video"
        private const val REQUEST_CAMERA_PERMISSION = 4001
        private const val MAX_VIDEO_DURATION_MS = 30_000L

        private val REQUIRED_PERMISSIONS = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}

