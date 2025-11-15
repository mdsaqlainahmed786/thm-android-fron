package com.thehotelmedia.android.activity.camera

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
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
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.animation.AccelerateDecelerateInterpolator
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
    private var flashState: FlashState = FlashState.AUTO
    private var videoRecordingTimer: CountDownTimer? = null
    private var captureCountdown: CountDownTimer? = null
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressTriggered = false
    private var isRecording = false
    private var isTimerRunning = false
    private var timerOption: TimerOption = TimerOption.OFF
    private var filterStyle: FilterStyle = FilterStyle.NONE
    private var initialTouchY = 0f
    private var initialZoomRatio = 1f
    private var minZoomRatio = 1f
    private var maxZoomRatio = 1f
    private var recordingIndicatorAnimator: ObjectAnimator? = null
    private val longPressRunnable = Runnable {
        longPressTriggered = true
        startRecording()
    }

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
        videoRecordingTimer?.cancel()
        captureCountdown?.cancel()
        longPressHandler.removeCallbacks(longPressRunnable)
        recordingIndicatorAnimator?.cancel()
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
        captureButton.setOnTouchListener { _, event -> handleCaptureTouch(event) }
        timerButton.setOnClickListener { cycleTimerOption() }
        filterButton.setOnClickListener { cycleFilterStyle() }

        updateFlashIcon()
        binding.actionHint.setText(R.string.camera_hint_photo)
        applyFilterStyle()
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
            // Get zoom range
            camera?.cameraInfo?.zoomState?.value?.let { zoomState ->
                minZoomRatio = zoomState.minZoomRatio
                maxZoomRatio = zoomState.maxZoomRatio
            }
            applyFlashStateToTorch()
        } catch (ex: Exception) {
            Toast.makeText(this, ex.localizedMessage ?: getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCaptureTouch(event: MotionEvent): Boolean {
        if (isTimerRunning) return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchY = event.y
                if (isRecording) {
                    // Get current zoom when starting a new drag gesture during recording
                    camera?.cameraInfo?.zoomState?.value?.let { zoomState ->
                        initialZoomRatio = zoomState.zoomRatio
                    }
                    return true
                }
                longPressTriggered = false
                binding.captureInnerCircle.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isRecording) {
                    // Calculate zoom based on vertical drag
                    val deltaY = initialTouchY - event.y // Positive when dragging up (zoom in)
                    val screenHeight = binding.root.height.toFloat()
                    if (screenHeight > 0) {
                        // Use a sensitivity factor for smoother zoom control
                        val sensitivity = 2.0f // Adjust this to make zoom more/less sensitive
                        val dragRatio = (deltaY / screenHeight) * sensitivity
                        
                        // Calculate new zoom ratio (drag up = zoom in, drag down = zoom out)
                        val zoomRange = maxZoomRatio - minZoomRatio
                        val newZoomRatio = (initialZoomRatio + (dragRatio * zoomRange)).coerceIn(minZoomRatio, maxZoomRatio)
                        
                        // Apply zoom
                        camera?.cameraControl?.setZoomRatio(newZoomRatio)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                binding.captureInnerCircle.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                longPressHandler.removeCallbacks(longPressRunnable)
                if (longPressTriggered || isRecording) {
                    stopRecording()
                } else {
                    triggerPhotoCapture()
                }
                return true
            }
        }
        return false
    }

    private fun triggerPhotoCapture() {
        if (isRecording) return
        longPressTriggered = false
        if (timerOption.durationMillis > 0) {
            startCaptureCountdown(timerOption.durationMillis)
        } else {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        binding.actionHint.setText(R.string.camera_hint_photo)
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
    private fun startCaptureCountdown(durationMillis: Long) {
        captureCountdown?.cancel()
        isTimerRunning = true
        binding.recordingStatusLayout.visibility = View.VISIBLE
        binding.recordingIndicator.visibility = View.GONE
        captureCountdown = object : CountDownTimer(durationMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = ((millisUntilFinished + 999L) / 1000L).toInt()
                binding.recordingTimer.text = seconds.toString()
                binding.actionHint.text = getString(R.string.camera_timer_running, seconds)
            }

            override fun onFinish() {
                binding.recordingStatusLayout.visibility = View.GONE
                binding.actionHint.setText(R.string.camera_hint_photo)
                isTimerRunning = false
                takePhoto()
            }
        }.start()
    }

    private fun startRecording() {
        if (isRecording || isTimerRunning) return
        val capture = videoCapture ?: return

        // Capture initial zoom ratio when recording starts
        camera?.cameraInfo?.zoomState?.value?.let { zoomState ->
            initialZoomRatio = zoomState.zoomRatio
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
            isRecording = true
            captureCountdown?.cancel()
            isTimerRunning = false
            binding.recordingStatusLayout.visibility = View.VISIBLE
            binding.recordingIndicator.visibility = View.GONE
            showRecordingIndicator()
            binding.actionHint.setText(R.string.camera_hint_video_hold)
            videoRecordingTimer?.cancel()
            videoRecordingTimer = object : CountDownTimer(MAX_VIDEO_DURATION_MS, 1000L) {
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
        videoRecordingTimer?.cancel()
        activeRecording = null
        runOnUiThread {
            isRecording = false
            binding.recordingStatusLayout.visibility = View.GONE
            hideRecordingIndicator()
            binding.actionHint.setText(R.string.camera_hint_photo)
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
        if (isRecording) {
            activeRecording?.stop()
            longPressTriggered = false
            hideRecordingIndicator()
        }
    }

    private fun cycleTimerOption() {
        timerOption = timerOption.next()
        val tintColor = if (timerOption == TimerOption.OFF) {
            Color.WHITE
        } else {
            ContextCompat.getColor(this, R.color.blue)
        }
        binding.timerButton.imageTintList = ColorStateList.valueOf(tintColor)
        Toast.makeText(this, getString(timerOption.messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun cycleFilterStyle() {
        filterStyle = filterStyle.next()
        applyFilterStyle()
        Toast.makeText(this, getString(filterStyle.messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun applyFilterStyle() {
        if (filterStyle == FilterStyle.NONE) {
            binding.filterOverlay.visibility = View.GONE
            binding.filterButton.imageTintList = ColorStateList.valueOf(Color.WHITE)
        } else {
            binding.filterOverlay.visibility = View.VISIBLE
            binding.filterOverlay.setBackgroundColor(filterStyle.overlayColor)
            binding.filterOverlay.alpha = filterStyle.overlayAlpha
            binding.filterButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
        }
    }

    private fun showRecordingIndicator() {
        binding.recordingIndicator.visibility = View.VISIBLE
        recordingIndicatorAnimator?.cancel()
        recordingIndicatorAnimator = ObjectAnimator.ofFloat(binding.recordingIndicator, View.ALPHA, 1f, 0.2f).apply {
            duration = 500
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun hideRecordingIndicator() {
        recordingIndicatorAnimator?.cancel()
        recordingIndicatorAnimator = null
        binding.recordingIndicator.visibility = View.GONE
        binding.recordingIndicator.alpha = 1f
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

    private enum class FlashState(val imageCaptureMode: Int) {
        AUTO(ImageCapture.FLASH_MODE_AUTO),
        ON(ImageCapture.FLASH_MODE_ON),
        OFF(ImageCapture.FLASH_MODE_OFF)
    }

    private enum class TimerOption(val durationMillis: Long, val messageRes: Int) {
        OFF(0L, R.string.camera_timer_off),
        THREE(3_000L, R.string.camera_timer_three),
        TEN(10_000L, R.string.camera_timer_ten);

        fun next(): TimerOption = when (this) {
            OFF -> THREE
            THREE -> TEN
            TEN -> OFF
        }
    }

    private enum class FilterStyle(val overlayColor: Int, val overlayAlpha: Float, val messageRes: Int) {
        NONE(Color.TRANSPARENT, 0f, R.string.camera_filter_none),
        WARM(Color.parseColor("#FFB74D"), 0.25f, R.string.camera_filter_warm),
        COOL(Color.parseColor("#64B5F6"), 0.25f, R.string.camera_filter_cool);

        fun next(): FilterStyle = when (this) {
            NONE -> WARM
            WARM -> COOL
            COOL -> NONE
        }
    }

    companion object {
        const val EXTRA_CAMERA_TITLE = "extra_camera_title"
        const val RESULT_MEDIA_URI = "result_media_uri"
        const val RESULT_MEDIA_TYPE = "result_media_type"
        const val MEDIA_TYPE_IMAGE = "image"
        const val MEDIA_TYPE_VIDEO = "video"
        private const val REQUEST_CAMERA_PERMISSION = 4001
        private const val MAX_VIDEO_DURATION_MS = 30_000L

        private const val LONG_PRESS_DURATION = 350L

        private val REQUIRED_PERMISSIONS = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}

