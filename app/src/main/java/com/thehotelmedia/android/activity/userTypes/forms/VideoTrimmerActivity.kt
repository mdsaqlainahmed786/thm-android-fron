package com.thehotelmedia.android.activity.userTypes.forms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
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
import com.thehotelmedia.android.utils.video.OverlayScaler
import com.thehotelmedia.android.utils.video.VideoExportEngine
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.widget.FrameLayout
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.view.Surface
import android.view.View
import android.media.ImageReader
import android.media.Image
import android.opengl.GLUtils
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import java.nio.ByteBuffer

class VideoTrimmerActivity : BaseActivity() {

    companion object {
        private const val VIDEO_URI_KEY = "video_uri"
        private const val REQUEST_WRITE_PERMISSION = 1001
        private const val MIN_VIDEO_DURATION = 5
    }

    data class OverlayPosition(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
        val type: String
    )
    
    private lateinit var binding: ActivityVideoTrimmerBinding
    private var videoUri: Uri? = null
    private var overlayBitmapPath: String? = null
    private var overlayViewWidth: Int = 0
    private var overlayViewHeight: Int = 0
    private var overlayPositions: List<OverlayPosition> = emptyList()
    private var overlayPreviewLoaded = false // Guard to prevent multiple loads
    private var from: String = ""
    private var businessesType: String = ""
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var progressBar: CustomProgressBar
    private lateinit var giffProgressBar: GiffProgressBar
    private lateinit var successGiff: SuccessGiff
    private lateinit var individualViewModal: IndividualViewModal
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoTrimmerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUri = intent.getStringExtra(VIDEO_URI_KEY)?.let { Uri.parse(it) }
        from = intent.getStringExtra("FROM") ?: ""
        
        // Get overlay bitmap path if provided
        overlayBitmapPath = intent.getStringExtra("overlay_bitmap_path")
        overlayViewWidth = intent.getIntExtra("overlay_view_width", 0)
        overlayViewHeight = intent.getIntExtra("overlay_view_height", 0)
        
        Log.d("VideoTrimmerActivity", "onCreate: overlayBitmapPath=$overlayBitmapPath, overlayViewWidth=$overlayViewWidth, overlayViewHeight=$overlayViewHeight")
        
        // Parse overlay positions
        val positionsJson = intent.getStringExtra("overlay_positions")
        Log.d("VideoTrimmerActivity", "onCreate: positionsJson length=${positionsJson?.length ?: 0}")
        if (positionsJson != null && positionsJson.isNotEmpty()) {
            overlayPositions = positionsJson.split("|").mapNotNull { posStr ->
                val parts = posStr.split(",")
                if (parts.size == 5) {
                    try {
                        OverlayPosition(
                            left = parts[0].toInt(),
                            top = parts[1].toInt(),
                            width = parts[2].toInt(),
                            height = parts[3].toInt(),
                            type = parts[4]
                        )
                    } catch (e: Exception) {
                        Log.e("VideoTrimmerActivity", "Error parsing overlay position: $posStr", e)
                        null
                    }
                } else {
                    null
                }
            }
            Log.d("VideoTrimmerActivity", "Parsed ${overlayPositions.size} overlay positions")
        } else {
            Log.w("VideoTrimmerActivity", "No overlay positions received or positionsJson is empty")
        }
        
        if (overlayBitmapPath != null) {
            val overlayFile = File(overlayBitmapPath)
            Log.d("VideoTrimmerActivity", "Overlay bitmap path received: $overlayBitmapPath")
            Log.d("VideoTrimmerActivity", "Overlay file exists: ${overlayFile.exists()}, size: ${if (overlayFile.exists()) overlayFile.length() else 0} bytes")
            Log.d("VideoTrimmerActivity", "Overlay view dimensions: ${overlayViewWidth}x${overlayViewHeight}")
        } else {
            Log.w("VideoTrimmerActivity", "No overlay bitmap path received in intent")
        }

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

        setSavingState(false)
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.save.setOnClickListener { handleSaveAction() }

        if (from == "CreateStory") {
            videoDuration = 30
        }

        initializePlayer()
        loadOverlayPreview()

        individualViewModal.createStoryResult.observe(this) { result ->
            Log.d("VideoTrimmerActivity", "createStoryResult received: status=${result.status}, message=${result.message}")
            setSavingState(false)
            giffProgressBar.hide()
            if (result.status == true) {
                result.message?.let { msg ->
                    Log.d("VideoTrimmerActivity", "Story created successfully, showing success screen")
                    runOnUiThread {
                        successGiff.show(msg) {
                            navigateToMainActivity(businessesType == business_type_individual)
                        }
                    }
                } ?: run {
                    Log.w("VideoTrimmerActivity", "Story created but no message provided")
                    runOnUiThread {
                        successGiff.show("Story created successfully") {
                            navigateToMainActivity(businessesType == business_type_individual)
                        }
                    }
                }
            } else {
                Log.e("VideoTrimmerActivity", "Story creation failed: ${result.message}")
                CustomSnackBar.showSnackBar(binding.root, result.message.orEmpty())
            }
        }

        individualViewModal.loading.observe(this) { isLoading ->
            Log.d("VideoTrimmerActivity", "Loading state changed: $isLoading")
            if (isLoading == true) {
                giffProgressBar.show()
            } else {
                giffProgressBar.hide()
            }
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
            // Validate the video file before returning
            if (validateVideoFile(tempFile)) {
            tempFile
            } else {
                Log.e("VideoTrimmerActivity", "Video file validation failed")
                tempFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e("VideoTrimmerActivity", "Error copying video file: ${e.message}", e)
            null
        }
    }
    

    private fun initializePlayer() {
        val uri = videoUri ?: return
        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            binding.playerView.player = player
            player.setMediaItem(MediaItem.fromUri(uri))
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.prepare()
            player.playWhenReady = true
        }
    }
    
    private fun loadOverlayPreview() {
        // Prevent multiple calls
        if (overlayPreviewLoaded) {
            Log.d("VideoTrimmerActivity", "Overlay preview already loaded, skipping")
            return
        }
        
        if (overlayBitmapPath != null && File(overlayBitmapPath).exists()) {
            // Mark as loading to prevent concurrent calls
            overlayPreviewLoaded = true
            
            // Wait for playerView to be laid out before loading overlay
            binding.playerView.post {
                // Also wait a bit more to ensure layout is complete
                binding.playerView.postDelayed({
                    try {
                        val overlayBitmap = BitmapFactory.decodeFile(overlayBitmapPath)
                        if (overlayBitmap != null) {
                            // Get the playerView dimensions
                            val playerViewWidth = binding.playerView.width
                            val playerViewHeight = binding.playerView.height
                            
                            Log.d("VideoTrimmerActivity", "Overlay bitmap: ${overlayBitmap.width}x${overlayBitmap.height}, PlayerView: ${playerViewWidth}x${playerViewHeight}, Original view: ${overlayViewWidth}x${overlayViewHeight}")
                            
                            if (playerViewWidth > 0 && playerViewHeight > 0 && overlayViewWidth > 0 && overlayViewHeight > 0) {
                                // Scale the entire overlay bitmap directly to match playerView dimensions
                                // The overlay bitmap already contains all overlays at their correct positions
                                // This matches how compositing works
                                val scaledOverlay = Bitmap.createBitmap(playerViewWidth, playerViewHeight, Bitmap.Config.ARGB_8888)
                                scaledOverlay.eraseColor(Color.TRANSPARENT)
                                val canvas = Canvas(scaledOverlay)
                                
                                // Use Canvas and Matrix for scaling to preserve alpha channel
                                val scaleMatrix = Matrix().apply {
                                    setScale(
                                        playerViewWidth.toFloat() / overlayViewWidth.toFloat(),
                                        playerViewHeight.toFloat() / overlayViewHeight.toFloat()
                                    )
                                }
                                
                                // Use Paint with proper alpha blending to preserve transparency
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    isFilterBitmap = true
                                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                                }
                                
                                // Draw the entire overlay bitmap scaled to playerView dimensions
                                canvas.drawBitmap(overlayBitmap, scaleMatrix, paint)
                                
                                Log.d("VideoTrimmerActivity", "Scaled entire overlay bitmap: ${overlayBitmap.width}x${overlayBitmap.height} -> ${playerViewWidth}x${playerViewHeight}")
                                
                                // Verify the scaled overlay has content
                                var hasContent = false
                                val pixels = IntArray(playerViewWidth * playerViewHeight)
                                scaledOverlay.getPixels(pixels, 0, playerViewWidth, 0, 0, playerViewWidth, playerViewHeight)
                                for (pixel in pixels) {
                                    if ((pixel shr 24) and 0xFF != 0) { // Check alpha channel
                                        hasContent = true
                                        break
                                    }
                                }
                                
                                if (!hasContent) {
                                    Log.w("VideoTrimmerActivity", "Scaled overlay bitmap appears to be empty (all transparent)")
                                } else {
                                    Log.d("VideoTrimmerActivity", "Scaled overlay bitmap has content, size: ${scaledOverlay.width}x${scaledOverlay.height}")
                                }
                                
                                // Set the bitmap - use FIT_XY to fill exactly without centering
                                binding.overlayPreview.setImageBitmap(scaledOverlay)
                                binding.overlayPreview.visibility = View.VISIBLE
                                binding.overlayPreview.alpha = 1f // Ensure fully opaque
                                binding.overlayPreview.scaleType = android.widget.ImageView.ScaleType.FIT_XY
                                
                                Log.d("VideoTrimmerActivity", "Overlay preview set: visibility=${binding.overlayPreview.visibility}, alpha=${binding.overlayPreview.alpha}, scaleType=${binding.overlayPreview.scaleType}")
                                
                                // CRITICAL: Create new layout params to prevent any centering
                                val layoutParams = FrameLayout.LayoutParams(
                                    playerViewWidth,
                                    playerViewHeight
                                ).apply {
                                    gravity = android.view.Gravity.NO_GRAVITY
                                    leftMargin = 0
                                    topMargin = 0
                                    rightMargin = 0
                                    bottomMargin = 0
                                }
                                binding.overlayPreview.layoutParams = layoutParams
                                
                                // Force position to (0,0) - NO CENTERING
                                binding.overlayPreview.x = 0f
                                binding.overlayPreview.y = 0f
                                binding.overlayPreview.translationX = 0f
                                binding.overlayPreview.translationY = 0f
                                
                                // Force layout update
                                binding.overlayPreview.requestLayout()
                                
                                // Aggressively prevent centering - multiple checks
                                binding.overlayPreview.post {
                                    val lp = FrameLayout.LayoutParams(playerViewWidth, playerViewHeight).apply {
                                        gravity = android.view.Gravity.NO_GRAVITY
                                        leftMargin = 0
                                        topMargin = 0
                                    }
                                    binding.overlayPreview.layoutParams = lp
                                    binding.overlayPreview.x = 0f
                                    binding.overlayPreview.y = 0f
                                }
                                
                                // Check again after delay
                                binding.overlayPreview.postDelayed({
                                    val lp = binding.overlayPreview.layoutParams as? FrameLayout.LayoutParams
                                    lp?.let {
                                        it.gravity = android.view.Gravity.NO_GRAVITY
                                        it.leftMargin = 0
                                        it.topMargin = 0
                                        binding.overlayPreview.layoutParams = it
                                    }
                                    binding.overlayPreview.x = 0f
                                    binding.overlayPreview.y = 0f
                                }, 50)
                                
                                // Continuous monitoring to prevent centering
                                var listener: android.view.ViewTreeObserver.OnGlobalLayoutListener? = null
                                listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
                                    val lp = binding.overlayPreview.layoutParams as? FrameLayout.LayoutParams
                                    lp?.let {
                                        if (it.gravity != android.view.Gravity.NO_GRAVITY) {
                                            it.gravity = android.view.Gravity.NO_GRAVITY
                                            binding.overlayPreview.layoutParams = it
                                        }
                                        if (it.leftMargin != 0 || it.topMargin != 0) {
                                            it.leftMargin = 0
                                            it.topMargin = 0
                                            binding.overlayPreview.layoutParams = it
                                        }
                                    }
                                    if (binding.overlayPreview.x != 0f || binding.overlayPreview.y != 0f) {
                                        binding.overlayPreview.x = 0f
                                        binding.overlayPreview.y = 0f
                                    }
                                }
                                binding.overlayPreview.viewTreeObserver.addOnGlobalLayoutListener(listener)
                                
                                // Also use a periodic check as backup
                                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                val positionCheckRunnable = object : Runnable {
                                    override fun run() {
                                        if (binding.overlayPreview.visibility == View.VISIBLE) {
                                            val lp = binding.overlayPreview.layoutParams as? FrameLayout.LayoutParams
                                            lp?.let {
                                                if (it.gravity != android.view.Gravity.NO_GRAVITY) {
                                                    it.gravity = android.view.Gravity.NO_GRAVITY
                                                    binding.overlayPreview.layoutParams = it
                                                }
                                            }
                                            if (binding.overlayPreview.x != 0f || binding.overlayPreview.y != 0f) {
                                                binding.overlayPreview.x = 0f
                                                binding.overlayPreview.y = 0f
                                            }
                                        }
                                        handler.postDelayed(this, 200) // Check every 200ms
                                    }
                                }
                                handler.postDelayed(positionCheckRunnable, 200)
                                
                                // Calculate scale factors for logging
                                val scaleX = playerViewWidth.toFloat() / overlayViewWidth.toFloat()
                                val scaleY = playerViewHeight.toFloat() / overlayViewHeight.toFloat()
                                Log.d("VideoTrimmerActivity", "Overlay preview loaded: original=${overlayViewWidth}x${overlayViewHeight}, scaled to $playerViewWidth x $playerViewHeight, scaleX=$scaleX, scaleY=$scaleY, ${overlayPositions.size} overlays")
                                
                                // Recycle original to free memory
                                overlayBitmap.recycle()
                            } else {
                                // Fallback: use original bitmap if view not ready, but try again later
                                // Reset the flag so we can retry
                                overlayPreviewLoaded = false
                                binding.overlayPreview.setImageBitmap(overlayBitmap)
                                binding.overlayPreview.visibility = View.VISIBLE
                                binding.overlayPreview.scaleType = android.widget.ImageView.ScaleType.FIT_XY
                                Log.d("VideoTrimmerActivity", "Overlay preview loaded (view not ready, using original size)")
                                
                                // Try to reload when view is ready (only once)
                                binding.playerView.postDelayed({ 
                                    if (!overlayPreviewLoaded) {
                                        loadOverlayPreview() 
                                    }
                                }, 100)
                            }
                        } else {
                            Log.e("VideoTrimmerActivity", "Failed to decode overlay bitmap")
                            overlayPreviewLoaded = false // Reset on error
                        }
                    } catch (e: Exception) {
                        Log.e("VideoTrimmerActivity", "Error loading overlay preview: ${e.message}", e)
                        overlayPreviewLoaded = false // Reset on error
                    }
                }, 200)
            }
        } else {
            binding.overlayPreview.visibility = View.GONE
        }
    }

    private fun releasePlayer() {
        binding.playerView.player = null
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun handleSaveAction() {
        Log.e("ZZZZZZ", "HANDLE SAVE ACTION CALLED, from=" + from)
        val uri = videoUri
        Log.e("ZZZZZZ", "VIDEO URI: " + uri)
        if (uri == null) {
            showToast("Video not available")
            return
        }
        
        // Prevent multiple clicks
        if (binding.save.alpha < 1f || !binding.save.isEnabled) {
            Log.d("VideoTrimmerActivity", "Save already in progress, ignoring click")
            return
        }
        
        Log.e("ZZZZZZ", "CHECKING from == CreateStory")
        if (from == "CreateStory") {
            Log.e("ZZZZZZ", "INSIDE from == CreateStory block")
            setSavingState(true)
            Log.e("ZZZZZZ", "overlayBitmapPath = " + overlayBitmapPath)
            val overlayFile = overlayBitmapPath?.let { File(it) }
            val overlayExists = overlayFile?.exists() == true
            Log.e("ZZZZZZ", "handleSaveAction() -> overlayExists=$overlayExists path=$overlayBitmapPath")
            Log.e("ZZZZZZ", "OVERLAY FILE EXISTS? " + overlayExists)
            Log.e("ZZZZZZ", "OVERLAY CHECK: path=$overlayBitmapPath, file=$overlayFile, exists=$overlayExists")
            Log.d("VideoTrimmerActivity", "=== SAVE ACTION STARTED ===")
            Log.d("VideoTrimmerActivity", "Overlay path: $overlayBitmapPath")
            Log.d("VideoTrimmerActivity", "Overlay file exists: $overlayExists")
            if (overlayExists && overlayFile != null) {
                Log.d("VideoTrimmerActivity", "Overlay file size: ${overlayFile.length()} bytes")
            }
            Log.d("VideoTrimmerActivity", "Overlay view dimensions: ${overlayViewWidth}x${overlayViewHeight}")
            Log.d("VideoTrimmerActivity", "Overlay positions count: ${overlayPositions.size}")
            
            // Show progress indicator
            giffProgressBar.show()
            
            // If overlay exists, render it onto video
            if (overlayExists) {
                Log.d("VideoTrimmerActivity", "*** OVERLAY DETECTED - STARTING COMPOSITING ***")
                renderOverlayOnVideo(uri) { videoFile ->
                    runOnUiThread {
                        giffProgressBar.hide()
                        if (videoFile != null && videoFile.exists() && validateVideoFile(videoFile)) {
                            Log.d("VideoTrimmerActivity", "Video with overlay ready (${videoFile.length()} bytes), uploading...")
                            individualViewModal.createStory(null, videoFile)
                        } else {
                            Log.e("VideoTrimmerActivity", "Composited video is invalid or missing, falling back to original")
                            // Fallback to original video
                            Thread {
                                try {
                                    val originalVideoFile = copyUriToTempFile(uri)
                                    runOnUiThread {
                                        if (originalVideoFile != null && originalVideoFile.exists() && validateVideoFile(originalVideoFile)) {
                                            Log.d("VideoTrimmerActivity", "Using original video without overlay (${originalVideoFile.length()} bytes)")
                                            individualViewModal.createStory(null, originalVideoFile)
                                        } else {
                            setSavingState(false)
                                            showToast("Unable to prepare video")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("VideoTrimmerActivity", "Error in fallback: ${e.message}", e)
                                    runOnUiThread {
                                        setSavingState(false)
                                        showToast("Error processing video")
                                    }
                                }
                            }.start()
                        }
                    }
                }
            } else {
                // No overlay, proceed normally
                Log.w("VideoTrimmerActivity", "*** NO OVERLAY - PROCEEDING WITH VIDEO ONLY ***")
                Log.w("VideoTrimmerActivity", "Reason: overlayBitmapPath=${overlayBitmapPath != null}, fileExists=${overlayFile?.exists() == true}")
                Thread {
                    try {
                        val videoFile = copyUriToTempFile(uri)
                        runOnUiThread {
                            giffProgressBar.hide()
                            if (videoFile != null && videoFile.exists() && validateVideoFile(videoFile)) {
                                Log.d("VideoTrimmerActivity", "Video ready (${videoFile.length()} bytes), uploading...")
                                individualViewModal.createStory(null, videoFile)
                            } else {
                                setSavingState(false)
                                showToast("Video file is invalid or corrupted. Please try again.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("VideoTrimmerActivity", "Error copying video: ${e.message}", e)
                        runOnUiThread {
                            giffProgressBar.hide()
                            setSavingState(false)
                            showToast("Error: ${e.message}")
                        }
                    }
                }.start()
            }
        } else {
            val resultIntent = Intent().apply {
                putExtra("trimmed_video_uri", uri.toString())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
    
    private fun validateVideoFile(videoFile: File): Boolean {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.e("VideoTrimmerActivity", "Video file does not exist or is empty")
            return false
        }
        
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            
            retriever.release()
            
            // Check frame rate from track format
            var frameRate = 0f
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(videoFile.absolutePath)
                for (i in 0 until extractor.trackCount) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") && trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE)?.toFloat() ?: 0f
                        break
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                Log.w("VideoTrimmerActivity", "Failed to get frame rate for validation: ${e.message}")
            }
            
            val isValid = duration > 0 && width > 0 && height > 0 && 
                          mimeType.startsWith("video/") && 
                          frameRate >= 15f // Minimum 15fps
            
            if (!isValid) {
                Log.w("VideoTrimmerActivity", "Video validation failed: duration=$duration, size=${width}x${height}, mime=$mimeType, frameRate=$frameRate")
            } else {
                Log.d("VideoTrimmerActivity", "Video validation passed: duration=$duration, size=${width}x${height}, frameRate=$frameRate")
            }
            
            isValid
        } catch (e: Exception) {
            Log.e("VideoTrimmerActivity", "Error validating video file: ${e.message}", e)
            false
        }
    }
    
    private fun renderOverlayOnVideo(videoUri: Uri, callback: (File?) -> Unit) {
        Log.d("VideoTrimmerActivity", "=== renderOverlayOnVideo CALLED ===")
        Log.d("VideoTrimmerActivity", "Video URI: $videoUri")
        Log.d("VideoTrimmerActivity", "Overlay bitmap path: $overlayBitmapPath")
        Log.d("VideoTrimmerActivity", "Overlay file exists: ${overlayBitmapPath?.let { File(it).exists() } == true}")
        Log.d("VideoTrimmerActivity", "Overlay view dimensions: ${overlayViewWidth}x${overlayViewHeight}")
        Log.d("VideoTrimmerActivity", "Overlay positions: ${overlayPositions.size}")
        
        // Use AtomicBoolean to track if callback was already called
        val callbackCalled = java.util.concurrent.atomic.AtomicBoolean(false)
        val safeCallback = { file: File? ->
            if (callbackCalled.compareAndSet(false, true)) {
                callback(file)
            } else {
                Log.d("VideoTrimmerActivity", "Callback already called, ignoring duplicate")
            }
        }
        
        // Run on background thread with progress updates and timeout
        val compositingThread = Thread {
            Log.e("ZZZZZZ", "renderOverlayOnVideo() -> Thread started")
            Log.e("ZZZZZZ", "COMPOSITING THREAD STARTED")
            var compositingCompleted = false
            try {
                Log.d("VideoTrimmerActivity", "Starting video compositing process...")
                Log.d("VideoTrimmerActivity", "Loading overlay bitmap from: $overlayBitmapPath")
                // Decode bitmap with alpha support to ensure transparency is preserved
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888 // Ensure alpha channel is preserved
                    inMutable = false // We don't need to modify the original
                }
                val overlayBitmap = BitmapFactory.decodeFile(overlayBitmapPath, options)
                Log.e("ZZZZZZ", "overlayBitmap loaded?=${overlayBitmap != null}")
                if (overlayBitmap == null) {
                    Log.e("VideoTrimmerActivity", "Failed to load overlay bitmap")
                    runOnUiThread { safeCallback(null) }
                    return@Thread
                }
                Log.d("VideoTrimmerActivity", "Overlay bitmap loaded: ${overlayBitmap.width}x${overlayBitmap.height}, config=${overlayBitmap.config}, hasAlpha=${overlayBitmap.hasAlpha()}")
                
                // Verify overlay bitmap has content
                var hasOverlayContent = false
                var nonTransparentPixels = 0
                val overlayPixels = IntArray(overlayBitmap.width * overlayBitmap.height)
                overlayBitmap.getPixels(overlayPixels, 0, overlayBitmap.width, 0, 0, overlayBitmap.width, overlayBitmap.height)
                for (pixel in overlayPixels) {
                    val alpha = (pixel shr 24) and 0xFF
                    if (alpha > 0) { // Check alpha channel
                        hasOverlayContent = true
                        nonTransparentPixels++
                        if (nonTransparentPixels == 1) {
                            // Log first non-transparent pixel for debugging
                            val r = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = pixel and 0xFF
                            Log.d("VideoTrimmerActivity", "First non-transparent pixel: ARGB($alpha, $r, $g, $b)")
                        }
                        if (nonTransparentPixels >= 10) break // Sample first 10 pixels
                    }
                }
                if (!hasOverlayContent) {
                    Log.e("VideoTrimmerActivity", "Overlay bitmap is empty (all transparent pixels), skipping compositing")
                    overlayBitmap.recycle()
                    val videoFile = copyUriToTempFile(videoUri)
                    runOnUiThread { 
                        if (videoFile != null && videoFile.exists()) {
                            Log.d("VideoTrimmerActivity", "Using video without overlay (overlay was empty)")
                            safeCallback(videoFile)
                        } else {
                            safeCallback(null)
                        }
                    }
                    return@Thread
                }
                Log.d("VideoTrimmerActivity", "Overlay bitmap has content ($nonTransparentPixels+ non-transparent pixels), proceeding with compositing")
                
                // Get video file path
                Log.d("VideoTrimmerActivity", "Copying video file...")
                val inputVideoFile = copyUriToTempFile(videoUri)
                if (inputVideoFile == null || !inputVideoFile.exists()) {
                    Log.e("VideoTrimmerActivity", "*** COMPOSITING FAILED: Failed to copy video file or file doesn't exist ***")
                    overlayBitmap.recycle()
                    runOnUiThread { safeCallback(null) }
                    return@Thread
                }
                Log.d("VideoTrimmerActivity", "*** COMPOSITING: Video file copied: ${inputVideoFile.absolutePath} (${inputVideoFile.length()} bytes) ***")
                
                // Create output file
                val outputFile = File.createTempFile("thm_video_with_overlay_", ".mp4", cacheDir)
                Log.d("VideoTrimmerActivity", "*** COMPOSITING: Starting video compositing to: ${outputFile.absolutePath} ***")
                
                // Composite overlay onto video with timeout
                val startTime = System.currentTimeMillis()
                Log.d("VideoTrimmerActivity", "*** COMPOSITING: Starting compositing process... ***")
                Log.d("VideoTrimmerActivity", "*** COMPOSITING: Input video: ${inputVideoFile.absolutePath} (${inputVideoFile.length()} bytes) ***")
                Log.d("VideoTrimmerActivity", "*** COMPOSITING: Overlay bitmap: ${overlayBitmap.width}x${overlayBitmap.height}, config=${overlayBitmap.config}, hasAlpha=${overlayBitmap.hasAlpha()}, positions count: ${overlayPositions.size} ***")
                Log.d("VideoTrimmerActivity", "*** COMPOSITING: Output file: ${outputFile.absolutePath} ***")
                
                // Get video dimensions for scaling
                val retrieverForDimensions = MediaMetadataRetriever()
                retrieverForDimensions.setDataSource(inputVideoFile.absolutePath)
                val videoWidth = retrieverForDimensions.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                val videoHeight = retrieverForDimensions.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                retrieverForDimensions.release()
                
                if (videoWidth == 0 || videoHeight == 0) {
                    Log.e("VideoTrimmerActivity", "Failed to get video dimensions")
                    overlayBitmap.recycle()
                    runOnUiThread { safeCallback(null) }
                    return@Thread
                }
                
                // Scale overlay to video dimensions
                Log.e("ZZZZZZ", "Scaling overlay…")
                val scaledOverlay = OverlayScaler.scaleOverlayToVideo(
                    overlayBitmap,
                    videoWidth,
                    videoHeight,
                    overlayViewWidth,
                    overlayViewHeight
                )
                overlayBitmap.recycle() // Recycle original after scaling
                
                if (scaledOverlay == null) {
                    Log.e("VideoTrimmerActivity", "Failed to scale overlay bitmap")
                    runOnUiThread { safeCallback(null) }
                    return@Thread
                }
                
                Log.d("VideoTrimmerActivity", "Scaled overlay: ${scaledOverlay.width}x${scaledOverlay.height}")
                
                // CRITICAL DIAGNOSTIC: Save overlay bitmap for debugging
                try {
                    val debugDir = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES), "video_debug")
                    if (!debugDir.exists()) {
                        debugDir.mkdirs()
                    }
                    val debugOverlayFile = File(debugDir, "debug_overlay.png")
                    FileOutputStream(debugOverlayFile).use { out ->
                        scaledOverlay.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    Log.d("VideoTrimmerActivity", "*** SAVED OVERLAY BITMAP: ${debugOverlayFile.absolutePath} ***")
                    
                    // Count non-transparent pixels
                    var nonTransparentCount = 0
                    val pixels = IntArray(scaledOverlay.width * scaledOverlay.height)
                    scaledOverlay.getPixels(pixels, 0, scaledOverlay.width, 0, 0, 
                            scaledOverlay.width, scaledOverlay.height)
                    for (pixel in pixels) {
                        if ((pixel shr 24) and 0xFF > 0) {
                            nonTransparentCount++
                        }
                    }
                    Log.d("VideoTrimmerActivity", "Overlay bitmap: ${scaledOverlay.width}x${scaledOverlay.height}, " +
                            "non-transparent pixels: $nonTransparentCount / ${pixels.size}")
                } catch (e: Exception) {
                    Log.w("VideoTrimmerActivity", "Failed to save debug overlay: ${e.message}")
                }
                
                // Use the new VideoExportEngine
                Log.e("ZZZZZZ", "ABOUT TO CALL exportVideoWithOverlay()")
                val exportEngine = VideoExportEngine()
                val success = exportEngine.exportVideoWithOverlay(inputVideoFile, scaledOverlay, outputFile)
                Log.e("ZZZZZZ", "exportVideoWithOverlay() RETURNED success=$success")
                scaledOverlay.recycle() // Recycle scaled overlay after export
                val elapsedTime = System.currentTimeMillis() - startTime
                compositingCompleted = true
                Log.d("VideoTrimmerActivity", "*** COMPOSITING: Completed in ${elapsedTime}ms, success=$success, output exists=${outputFile.exists()}, output size=${outputFile.length()} ***")
                
                if (success && outputFile.exists() && outputFile.length() > 0) {
                    // Validate the output video file
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(outputFile.absolutePath)
                        val outputDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        val outputWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                        val outputHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                        
                        // Check frame rate - it should be reasonable (at least 15fps)
                        var outputFrameRate = 0f
                        try {
                            val extractorForValidation = MediaExtractor()
                            extractorForValidation.setDataSource(outputFile.absolutePath)
                            for (i in 0 until extractorForValidation.trackCount) {
                                val trackFormat = extractorForValidation.getTrackFormat(i)
                                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                                if (mime.startsWith("video/") && trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                                    outputFrameRate = trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE)?.toFloat() ?: 0f
                                    break
                                }
                            }
                            extractorForValidation.release()
                        } catch (e: Exception) {
                            Log.w("VideoTrimmerActivity", "Failed to get frame rate from output video: ${e.message}")
                        }
                        
                        if (outputDuration > 0 && outputWidth > 0 && outputHeight > 0 && outputFrameRate >= 15f) {
                    val fileSizeMB = outputFile.length() / (1024.0 * 1024.0)
                            Log.d("VideoTrimmerActivity", "Video compositing successful: ${String.format("%.2f", fileSizeMB)} MB, duration=${outputDuration}ms, size=${outputWidth}x${outputHeight}, frameRate=${outputFrameRate}fps")
                            retriever.release()
                            overlayBitmap.recycle()
                            runOnUiThread { safeCallback(outputFile) }
                            return@Thread
                        } else {
                            Log.e("VideoTrimmerActivity", "Output video validation failed: duration=$outputDuration, size=${outputWidth}x${outputHeight}, frameRate=${outputFrameRate}fps")
                            retriever.release()
                            if (outputFile.exists()) {
                                outputFile.delete()
                            }
                            // Fallback to video without overlay - don't throw, handle it below
                            compositingCompleted = true
                            val videoFile = copyUriToTempFile(videoUri)
                            runOnUiThread {
                                if (videoFile != null && videoFile.exists()) {
                                    Log.d("VideoTrimmerActivity", "Composited video invalid, using original video as fallback")
                                    safeCallback(videoFile)
                                } else {
                                    safeCallback(null)
                                    setSavingState(false)
                                    showToast("Failed to process video. Please try again.")
                                }
                            }
                            overlayBitmap.recycle()
                            return@Thread
                        }
                    } catch (e: Exception) {
                        Log.e("VideoTrimmerActivity", "Error validating output video: ${e.message}", e)
                        retriever.release()
                        if (outputFile.exists()) {
                            outputFile.delete()
                        }
                        // Fallback to video without overlay
                        compositingCompleted = true
                        val videoFile = copyUriToTempFile(videoUri)
                        runOnUiThread {
                            if (videoFile != null && videoFile.exists()) {
                                Log.d("VideoTrimmerActivity", "Validation error, using original video as fallback")
                                safeCallback(videoFile)
                            } else {
                                safeCallback(null)
                                setSavingState(false)
                                showToast("Failed to process video. Please try again.")
                            }
                        }
                        overlayBitmap.recycle()
                        return@Thread
                    }
                } else {
                    Log.e("VideoTrimmerActivity", "Failed to composite overlay. Success=$success, exists=${outputFile.exists()}, size=${outputFile.length()}")
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    // Fallback: try uploading video without overlay
                    Log.w("VideoTrimmerActivity", "Falling back to video without overlay")
                    try {
                        val videoFile = copyUriToTempFile(videoUri)
                    runOnUiThread { 
                            if (videoFile != null && videoFile.exists()) {
                                Log.d("VideoTrimmerActivity", "Using video without overlay as fallback")
                                safeCallback(videoFile)
                            } else {
                                safeCallback(null)
                                setSavingState(false)
                                showToast("Failed to process video. Please try again.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("VideoTrimmerActivity", "Error in fallback: ${e.message}", e)
                        runOnUiThread { 
                            safeCallback(null)
                        setSavingState(false)
                        showToast("Failed to add text to video. Please try again.")
                        }
                    }
                }
                
                // Cleanup
                overlayBitmap.recycle()
            } catch (e: Exception) {
                Log.e("ZZZZZZ", "EXCEPTION IN COMPOSITING THREAD: ${e.message}", e)
                compositingCompleted = true
                Log.e("VideoTrimmerActivity", "Error rendering overlay: ${e.message}", e)
                e.printStackTrace()
                // On error, try fallback to video without overlay
                try {
                    val videoFile = copyUriToTempFile(videoUri)
                runOnUiThread { 
                        if (videoFile != null && videoFile.exists()) {
                            Log.d("VideoTrimmerActivity", "Error occurred, using video without overlay as fallback")
                            safeCallback(videoFile)
                        } else {
                            safeCallback(null)
                    setSavingState(false)
                            showToast("Error processing video. Please try again.")
                        }
                    }
                } catch (e2: Exception) {
                    Log.e("VideoTrimmerActivity", "Error in error fallback: ${e2.message}", e2)
                    runOnUiThread { 
                        safeCallback(null)
                        setSavingState(false)
                        showToast("Error processing video. Please try again.")
                    }
                }
            }
        }
        
        compositingThread.start()
        
        // Add timeout mechanism - automatically fallback to video without overlay if it takes too long
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (compositingThread.isAlive) {
                Log.w("VideoTrimmerActivity", "Compositing timeout - falling back to video without overlay")
                compositingThread.interrupt()
                
                // Automatically fallback to uploading video without overlay
                runOnUiThread {
                    try {
                        Log.d("VideoTrimmerActivity", "Timeout: Using video without overlay")
                        val videoFile = copyUriToTempFile(videoUri)
                        if (videoFile != null && videoFile.exists()) {
                            Log.d("VideoTrimmerActivity", "Video ready without overlay (${videoFile.length()} bytes), uploading...")
                            safeCallback(videoFile)
                        } else {
                            setSavingState(false)
                            giffProgressBar.hide()
                            safeCallback(null)
                            showToast("Video processing timeout. Please try again.")
                        }
                    } catch (e: Exception) {
                        Log.e("VideoTrimmerActivity", "Error in timeout fallback: ${e.message}", e)
                        setSavingState(false)
                        giffProgressBar.hide()
                        safeCallback(null)
                        showToast("Video processing timeout. Please try again.")
                    }
                }
            }
        }, 30000) // 30 second timeout - faster fallback
    }
    
    private fun compositeOverlayOnVideo(inputFile: File, overlayBitmap: Bitmap, outputFile: File): Boolean {
        Log.d("VideoTrimmerActivity", "*** compositeOverlayOnVideo CALLED ***")
        Log.d("VideoTrimmerActivity", "*** Input file: ${inputFile.absolutePath} (${inputFile.length()} bytes) ***")
        Log.d("VideoTrimmerActivity", "*** Overlay bitmap: ${overlayBitmap.width}x${overlayBitmap.height}, config=${overlayBitmap.config}, hasAlpha=${overlayBitmap.hasAlpha()} ***")
        Log.d("VideoTrimmerActivity", "*** Output file: ${outputFile.absolutePath} ***")
        
        var retriever: MediaMetadataRetriever? = null
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var extractor: MediaExtractor? = null
        var muxerStopped = false
        var scaledOverlay: Bitmap? = null
        
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputFile.absolutePath)
            Log.d("VideoTrimmerActivity", "*** MediaMetadataRetriever initialized ***")
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            
            if (durationMs == 0L) {
                Log.e("VideoTrimmerActivity", "Failed to get video duration")
                return false
            }
            
            val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            
            if (videoWidth == 0 || videoHeight == 0) {
                Log.e("VideoTrimmerActivity", "Failed to get video dimensions")
                return false
            }
            
            // Get original video frame rate from track format - more reliable than metadata
            var originalFrameRate = 30 // Default to 30fps
            try {
                val extractorForFrameRate = MediaExtractor()
                extractorForFrameRate.setDataSource(inputFile.absolutePath)
                for (i in 0 until extractorForFrameRate.trackCount) {
                    val trackFormat = extractorForFrameRate.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/")) {
                        // Try to get frame rate from track format
                        if (trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            val frameRate = trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                            if (frameRate != null && frameRate >= 15 && frameRate <= 60) {
                                originalFrameRate = frameRate
                                Log.d("VideoTrimmerActivity", "Got frame rate from track format: $originalFrameRate fps")
                                break
                            } else {
                                Log.w("VideoTrimmerActivity", "Frame rate from track format is invalid: $frameRate, using default")
                            }
                        }
                    }
                }
                extractorForFrameRate.release()
            } catch (e: Exception) {
                Log.w("VideoTrimmerActivity", "Failed to get frame rate from track format: ${e.message}, using default 30fps")
            }
            
            // Fallback: try metadata if track format didn't work
            if (originalFrameRate == 30) {
                try {
                    val frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    val frameRate = frameRateStr?.toIntOrNull()
                    if (frameRate != null && frameRate >= 15 && frameRate <= 60) {
                        originalFrameRate = frameRate
                        Log.d("VideoTrimmerActivity", "Got frame rate from metadata: $originalFrameRate fps")
                    } else if (frameRate != null) {
                        Log.w("VideoTrimmerActivity", "Frame rate from metadata is invalid: $frameRate, using default 30fps")
                    }
                } catch (e: Exception) {
                    Log.w("VideoTrimmerActivity", "Failed to get frame rate from metadata: ${e.message}")
                }
            }
            
            // Ensure minimum frame rate of 15fps to avoid near-zero frame rates
            if (originalFrameRate < 15) {
                Log.w("VideoTrimmerActivity", "Frame rate $originalFrameRate is too low, using minimum 15fps")
                originalFrameRate = 15
            }
            
            Log.d("VideoTrimmerActivity", "Using frame rate: $originalFrameRate fps for encoding")
            
            // Scale the entire overlay bitmap directly to match video dimensions
            // This preserves all overlays at their correct relative positions
            // The overlay bitmap already contains all overlays drawn at their correct positions
            val overlayBitmapWidth = overlayBitmap.width
            val overlayBitmapHeight = overlayBitmap.height
            
            Log.d("VideoTrimmerActivity", "Scaling overlay bitmap: ${overlayBitmapWidth}x${overlayBitmapHeight} -> ${videoWidth}x${videoHeight}")
            Log.d("VideoTrimmerActivity", "Overlay view dimensions: ${overlayViewWidth}x${overlayViewHeight}, positions count: ${overlayPositions.size}")
            
            // Create scaled overlay using Canvas to preserve alpha channel
            // This ensures transparency is maintained during scaling
            scaledOverlay = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
            scaledOverlay.eraseColor(Color.TRANSPARENT)
            val scaleCanvas = Canvas(scaledOverlay)
            // Use Paint with proper alpha blending to preserve transparency during scaling
            val scalePaint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                // Explicitly set SRC_OVER mode to ensure proper alpha blending during scaling
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            // Scale and draw the overlay bitmap to match video dimensions
            val scaleMatrix = Matrix().apply {
                setScale(
                    videoWidth.toFloat() / overlayBitmapWidth.toFloat(),
                    videoHeight.toFloat() / overlayBitmapHeight.toFloat()
                )
            }
            scaleCanvas.drawBitmap(overlayBitmap, scaleMatrix, scalePaint)
            
            Log.d("VideoTrimmerActivity", "Scaled overlay created: ${scaledOverlay.width}x${scaledOverlay.height}, config=${scaledOverlay.config}, hasAlpha=${scaledOverlay.hasAlpha()}")
            
            // Verify the scaled overlay has content
            if (scaledOverlay != null) {
                var hasContent = false
                // Sample pixels from the scaled overlay to verify it has content
                val sampleSize = minOf(100, videoWidth * videoHeight)
                val pixels = IntArray(sampleSize)
                val stepX = maxOf(1, videoWidth / 10)
                val stepY = maxOf(1, videoHeight / 10)
                var pixelIndex = 0
                for (y in 0 until videoHeight step stepY) {
                    for (x in 0 until videoWidth step stepX) {
                        if (pixelIndex < sampleSize) {
                            pixels[pixelIndex] = scaledOverlay.getPixel(x, y)
                            if ((pixels[pixelIndex] shr 24) and 0xFF > 0) { // Check alpha channel
                                hasContent = true
                            }
                            pixelIndex++
                        }
                    }
                    if (hasContent) break
                }
                
                if (!hasContent) {
                    Log.w("VideoTrimmerActivity", "Scaled overlay appears to be empty (all transparent pixels sampled)")
                    // Try a more thorough check - sample more pixels
                    var thoroughCheck = false
                    val thoroughPixels = IntArray(minOf(1000, videoWidth * videoHeight))
                    scaledOverlay.getPixels(thoroughPixels, 0, minOf(100, videoWidth), 0, 0, minOf(100, videoWidth), minOf(100, videoHeight))
                    for (pixel in thoroughPixels) {
                        if ((pixel shr 24) and 0xFF > 0) {
                            thoroughCheck = true
                            break
                        }
                    }
                    if (!thoroughCheck) {
                        Log.e("VideoTrimmerActivity", "Scaled overlay is confirmed empty after thorough check, skipping compositing")
                        scaledOverlay?.recycle()
                        return false
            } else {
                        Log.d("VideoTrimmerActivity", "Scaled overlay has content after thorough check")
                    }
                } else {
                    Log.d("VideoTrimmerActivity", "Scaled overlay has content (verified with sampling)")
                }
            }
            
            // Verify scaled overlay has content
            if (scaledOverlay != null) {
                var hasContent = false
                val pixels = IntArray(videoWidth * videoHeight)
                scaledOverlay.getPixels(pixels, 0, videoWidth, 0, 0, videoWidth, videoHeight)
                for (pixel in pixels) {
                    if ((pixel shr 24) and 0xFF != 0) { // Check alpha channel
                        hasContent = true
                        break
                    }
                }
                if (!hasContent) {
                    Log.w("VideoTrimmerActivity", "Scaled overlay is empty (all transparent), skipping compositing")
                    scaledOverlay?.recycle()
                    return false
                }
            }
            
            // Setup encoder with Surface
            // Use original video frame rate for smooth playback
            // Ensure frame rate is valid (minimum 15fps, maximum 60fps)
            val validFrameRate = originalFrameRate.coerceIn(15, 60)
            if (validFrameRate != originalFrameRate) {
                Log.w("VideoTrimmerActivity", "Frame rate $originalFrameRate adjusted to $validFrameRate for encoder")
            }
            
            // Force to 30fps if frame rate extraction failed (default was 30)
            // This ensures we always have a reasonable frame rate
            val finalFrameRate = if (originalFrameRate == 30 && validFrameRate == 30) {
                // If we defaulted to 30, try to get a better estimate from the video
                // Otherwise use 30 as a safe default
                30
            } else {
                validFrameRate
            }
            
            Log.d("VideoTrimmerActivity", "Frame rate: original=$originalFrameRate, valid=$validFrameRate, final=$finalFrameRate")
            
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, finalFrameRate)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            Log.d("VideoTrimmerActivity", "Encoder configured: ${videoWidth}x${videoHeight} @ ${finalFrameRate}fps")
            
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()
            
            // Setup muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1
            
            // Copy audio track
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = muxer.addTrack(trackFormat)
                    break
                }
            }
            
            // Wait for encoder to produce output format before adding track
            // The encoder output format is only available after it processes the first frame
            // We'll add the track when we get the first output buffer
            
            // Setup EGL for rendering to Surface
            val eglDisplay = android.opengl.EGL14.eglGetDisplay(android.opengl.EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            android.opengl.EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
            
            val attribList = intArrayOf(
                android.opengl.EGL14.EGL_RENDERABLE_TYPE, android.opengl.EGL14.EGL_OPENGL_ES2_BIT,
                android.opengl.EGL14.EGL_RED_SIZE, 8,
                android.opengl.EGL14.EGL_GREEN_SIZE, 8,
                android.opengl.EGL14.EGL_BLUE_SIZE, 8,
                android.opengl.EGL14.EGL_ALPHA_SIZE, 8,
                android.opengl.EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            android.opengl.EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)
            
            val eglContext = android.opengl.EGL14.eglCreateContext(eglDisplay, configs[0], android.opengl.EGL14.EGL_NO_CONTEXT, intArrayOf(android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, android.opengl.EGL14.EGL_NONE), 0)
            val eglSurface = android.opengl.EGL14.eglCreateWindowSurface(eglDisplay, configs[0], inputSurface, intArrayOf(android.opengl.EGL14.EGL_NONE), 0)
            android.opengl.EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            
            android.opengl.GLES20.glViewport(0, 0, videoWidth, videoHeight)
            
            // Create shader program
            val vertexShader = """
                attribute vec4 aPosition;
                attribute vec2 aTexCoord;
                varying vec2 vTexCoord;
                void main() {
                    gl_Position = aPosition;
                    vTexCoord = aTexCoord;
                }
            """.trimIndent()
            
            val fragmentShader = """
                precision mediump float;
                varying vec2 vTexCoord;
                uniform sampler2D uTexture;
                void main() {
                    gl_FragColor = texture2D(uTexture, vTexCoord);
                }
            """.trimIndent()
            
            val vertexShaderHandle = loadShader(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShader)
            val fragmentShaderHandle = loadShader(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShader)
            val programHandle = android.opengl.GLES20.glCreateProgram()
            android.opengl.GLES20.glAttachShader(programHandle, vertexShaderHandle)
            android.opengl.GLES20.glAttachShader(programHandle, fragmentShaderHandle)
            android.opengl.GLES20.glLinkProgram(programHandle)
            
            val positionHandle = android.opengl.GLES20.glGetAttribLocation(programHandle, "aPosition")
            val texCoordHandle = android.opengl.GLES20.glGetAttribLocation(programHandle, "aTexCoord")
            val textureHandle = android.opengl.GLES20.glGetUniformLocation(programHandle, "uTexture")
            
            // CRITICAL: Wait for encoder output format BEFORE processing frames
            // The encoder must produce its output format (stsd atom) before we can add track to muxer
            val bufferInfo = MediaCodec.BufferInfo()
            var videoTrackIndex = -1
            var muxerStarted = false
            
            Log.d("VideoTrimmerActivity", "Waiting for encoder output format...")
            // Render a dummy frame to trigger encoder output format
            val dummyBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
            dummyBitmap.eraseColor(Color.BLACK)
            val dummyTexture = IntArray(1)
            android.opengl.GLES20.glGenTextures(1, dummyTexture, 0)
            android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, dummyTexture[0])
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MIN_FILTER, android.opengl.GLES20.GL_LINEAR)
            android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MAG_FILTER, android.opengl.GLES20.GL_LINEAR)
            GLUtils.texImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0, dummyBitmap, 0)
            android.opengl.GLES20.glClearColor(0f, 0f, 0f, 1f)
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
            android.opengl.GLES20.glUseProgram(programHandle)
            android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE0)
            android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, dummyTexture[0])
            android.opengl.GLES20.glUniform1i(textureHandle, 0)
            val dummyVertices = floatArrayOf(-1f, -1f, 0f, 1f, 1f, -1f, 1f, 1f, -1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f)
            val dummyVertexBuffer = java.nio.ByteBuffer.allocateDirect(dummyVertices.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            dummyVertexBuffer.put(dummyVertices)
            dummyVertexBuffer.position(0)
            android.opengl.GLES20.glVertexAttribPointer(positionHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 4 * 4, dummyVertexBuffer)
            android.opengl.GLES20.glEnableVertexAttribArray(positionHandle)
            dummyVertexBuffer.position(2)
            android.opengl.GLES20.glVertexAttribPointer(texCoordHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 4 * 4, dummyVertexBuffer)
            android.opengl.GLES20.glEnableVertexAttribArray(texCoordHandle)
            android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4)
            // For Surface-based encoding, presentation time is handled automatically by the encoder
            android.opengl.EGL14.eglSwapBuffers(eglDisplay, eglSurface)
            android.opengl.GLES20.glDeleteTextures(1, dummyTexture, 0)
            dummyBitmap.recycle()
            
            // Wait for encoder output format
            var formatReceived = false
            var attempts = 0
            while (!formatReceived && attempts < 100) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = encoder.outputFormat
                        // Ensure the output format has the correct frame rate
                        if (!outputFormat.containsKey(MediaFormat.KEY_FRAME_RATE) || 
                            outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE) == null ||
                            outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)!! <= 0) {
                            Log.w("VideoTrimmerActivity", "Encoder output format missing or invalid frame rate, setting to $finalFrameRate")
                            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, finalFrameRate)
                        } else {
                            val outputFrameRate = outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)!!
                            if (outputFrameRate < 15 || outputFrameRate > 60) {
                                Log.w("VideoTrimmerActivity", "Encoder output format has invalid frame rate $outputFrameRate, overriding with $validFrameRate")
                                outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, validFrameRate)
                            } else {
                                Log.d("VideoTrimmerActivity", "Encoder output format frame rate: $outputFrameRate fps")
                            }
                        }
                        videoTrackIndex = muxer.addTrack(outputFormat)
                        muxer.start()
                        muxerStarted = true
                        formatReceived = true
                        Log.d("VideoTrimmerActivity", "Encoder output format received, muxer started with video track index: $videoTrackIndex, frame rate: ${outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)}")
                    }
                    outputBufferIndex >= 0 -> {
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
                attempts++
                if (!formatReceived) Thread.sleep(10)
            }
            
            if (!muxerStarted) {
                Log.e("VideoTrimmerActivity", "Failed to get encoder output format after $attempts attempts")
                return false
            }
            
            // Process frames at validated frame rate
            // Calculate frame interval based on final frame rate
            val frameInterval = (1000L / finalFrameRate).toLong() // e.g., 33ms for 30fps
            var frameCount = 0
            val startTime = System.currentTimeMillis()
            val totalFrames = (durationMs / frameInterval).toInt()
            
            Log.d("VideoTrimmerActivity", "Starting compositing: duration=${durationMs}ms, will process ~$totalFrames frames at ${finalFrameRate}fps (interval=${frameInterval}ms)")
            
            // Process frames more efficiently
            // Also drain encoder output periodically while processing
            var outputEOSReceived = false
            var videoSamplesWritten = 0
            var lastPresentationTimeNs = 0L // Track last presentation time to ensure monotonic increase
            
            for (timeMs in 0L until durationMs step frameInterval) {
                // Check if thread was interrupted (timeout)
                if (Thread.currentThread().isInterrupted) {
                    Log.w("VideoTrimmerActivity", "Compositing interrupted")
                    return false
                }
                
                // Extract frame - use CLOSEST_SYNC for faster extraction
                val frame = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                
                if (frame != null) {
                    // Create composite bitmap
                    val compositeBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(compositeBitmap)
                    
                    // Draw video frame
                    canvas.drawBitmap(frame, 0f, 0f, null)
                    
                    // Draw overlay with proper alpha blending
                    if (scaledOverlay != null) {
                        if (frameCount == 0) {
                            Log.d("VideoTrimmerActivity", "Drawing overlay: ${scaledOverlay.width}x${scaledOverlay.height} (hasAlpha=${scaledOverlay.hasAlpha()}, config=${scaledOverlay.config}) onto ${compositeBitmap.width}x${compositeBitmap.height} at (0, 0)")
                            // Sample a few pixels from scaledOverlay to verify it has content
                            var sampleCount = 0
                            var nonTransparentSamples = 0
                            for (y in 0 until minOf(10, scaledOverlay.height) step maxOf(1, scaledOverlay.height / 5)) {
                                for (x in 0 until minOf(10, scaledOverlay.width) step maxOf(1, scaledOverlay.width / 5)) {
                                    val pixel = scaledOverlay.getPixel(x, y)
                                    val alpha = (pixel shr 24) and 0xFF
                                    if (alpha > 0) {
                                        nonTransparentSamples++
                                        val r = (pixel shr 16) and 0xFF
                                        val g = (pixel shr 8) and 0xFF
                                        val b = pixel and 0xFF
                                        if (nonTransparentSamples == 1) {
                                            Log.d("VideoTrimmerActivity", "First overlay sample pixel at ($x, $y): ARGB($alpha, $r, $g, $b)")
                                        }
                                    }
                                    sampleCount++
                                }
                            }
                            Log.d("VideoTrimmerActivity", "Overlay sampling: $nonTransparentSamples/$sampleCount pixels have alpha > 0")
                        }
                        // Use Paint with proper alpha blending to ensure transparency is preserved
                        // SRC_OVER is the default blending mode, but we explicitly set it to ensure alpha is handled correctly
                        val paint = Paint().apply {
                            isAntiAlias = true
                            isFilterBitmap = true
                            // Explicitly set SRC_OVER mode to ensure proper alpha blending
                            // This mode composites the source (overlay) over the destination (video frame)
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                        }
                        canvas.drawBitmap(scaledOverlay, 0f, 0f, paint)
                        
                        // Verify overlay was drawn on first frame
                        if (frameCount == 0) {
                            // Sample pixels from composite to see if overlay is visible
                            var compositeSampleCount = 0
                            var compositeNonTransparent = 0
                            for (y in 0 until minOf(10, videoHeight) step maxOf(1, videoHeight / 5)) {
                                for (x in 0 until minOf(10, videoWidth) step maxOf(1, videoWidth / 5)) {
                                    val pixel = compositeBitmap.getPixel(x, y)
                                    val alpha = (pixel shr 24) and 0xFF
                                    if (alpha > 0) {
                                        compositeNonTransparent++
                                    }
                                    compositeSampleCount++
                                }
                            }
                            Log.d("VideoTrimmerActivity", "Composite bitmap after overlay: $compositeNonTransparent/$compositeSampleCount pixels have alpha > 0")
                        }
                        
                        // Verify overlay was drawn (check first frame only to avoid performance impact)
                        if (frameCount == 0) {
                            // Sample pixels from the scaled overlay to verify it has non-transparent content
                            var overlayHasContent = false
                            val overlaySampleSize = minOf(100, scaledOverlay.width * scaledOverlay.height)
                            val overlayPixels = IntArray(overlaySampleSize)
                            val stepX = maxOf(1, scaledOverlay.width / 10)
                            val stepY = maxOf(1, scaledOverlay.height / 10)
                            var sampleIndex = 0
                            for (y in 0 until scaledOverlay.height step stepY) {
                                for (x in 0 until scaledOverlay.width step stepX) {
                                    if (sampleIndex < overlaySampleSize) {
                                        overlayPixels[sampleIndex] = scaledOverlay.getPixel(x, y)
                                        if ((overlayPixels[sampleIndex] shr 24) and 0xFF > 0) {
                                            overlayHasContent = true
                                            Log.d("VideoTrimmerActivity", "Overlay has non-transparent pixel at ($x, $y): alpha=${(overlayPixels[sampleIndex] shr 24) and 0xFF}")
                                        }
                                        sampleIndex++
                                    }
                                }
                                if (overlayHasContent) break
                            }
                            
                            // Sample pixels from composite to verify overlay was drawn
                            var compositeHasOverlay = false
                            val compositeSampleSize = minOf(100, videoWidth * videoHeight)
                            val compositePixels = IntArray(compositeSampleSize)
                            val compositeStepX = maxOf(1, videoWidth / 10)
                            val compositeStepY = maxOf(1, videoHeight / 10)
                            var compositeIndex = 0
                            for (y in 0 until videoHeight step compositeStepY) {
                                for (x in 0 until videoWidth step compositeStepX) {
                                    if (compositeIndex < compositeSampleSize) {
                                        compositePixels[compositeIndex] = compositeBitmap.getPixel(x, y)
                                        // We can't easily distinguish overlay from video, but we can verify the bitmap has content
                                        if ((compositePixels[compositeIndex] shr 24) and 0xFF > 0) {
                                            compositeHasOverlay = true
                                        }
                                        compositeIndex++
                                    }
                                }
                                if (compositeHasOverlay) break
                            }
                            
                            Log.d("VideoTrimmerActivity", "First frame verification: overlayHasContent=$overlayHasContent, compositeHasContent=$compositeHasOverlay, overlaySize=${scaledOverlay.width}x${scaledOverlay.height}, compositeSize=${compositeBitmap.width}x${compositeBitmap.height}")
                        }
                    } else {
                        if (frameCount == 0) {
                            Log.w("VideoTrimmerActivity", "First frame: scaledOverlay is null, overlay will not be visible")
                        }
                    }
                    
                    // Render composite bitmap to Surface using OpenGL
                    val texture = IntArray(1)
                    android.opengl.GLES20.glGenTextures(1, texture, 0)
                    android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, texture[0])
                    android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MIN_FILTER, android.opengl.GLES20.GL_LINEAR)
                    android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MAG_FILTER, android.opengl.GLES20.GL_LINEAR)
                    // Enable alpha channel support for the texture
                    android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_WRAP_S, android.opengl.GLES20.GL_CLAMP_TO_EDGE)
                    android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_WRAP_T, android.opengl.GLES20.GL_CLAMP_TO_EDGE)
                    GLUtils.texImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0, compositeBitmap, 0)
                    
                    // Note: The compositeBitmap already has the overlay composited onto the video frame,
                    // so we don't need blending. The texture will be rendered as-is.
                    // However, we ensure the texture format supports alpha by using ARGB_8888 bitmap
                    
                    // Clear and draw texture as fullscreen quad
                    android.opengl.GLES20.glClearColor(0f, 0f, 0f, 1f)
                    android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
                    
                    android.opengl.GLES20.glUseProgram(programHandle)
                    android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE0)
                    android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, texture[0])
                    android.opengl.GLES20.glUniform1i(textureHandle, 0)
                    
                    // Draw fullscreen quad
                    val vertices = floatArrayOf(
                        -1f, -1f, 0f, 1f,
                        1f, -1f, 1f, 1f,
                        -1f, 1f, 0f, 0f,
                        1f, 1f, 1f, 0f
                    )
                    val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
                    vertexBuffer.put(vertices)
                    vertexBuffer.position(0)
                    
                    android.opengl.GLES20.glVertexAttribPointer(positionHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 4 * 4, vertexBuffer)
                    android.opengl.GLES20.glEnableVertexAttribArray(positionHandle)
                    vertexBuffer.position(2)
                    android.opengl.GLES20.glVertexAttribPointer(texCoordHandle, 2, android.opengl.GLES20.GL_FLOAT, false, 4 * 4, vertexBuffer)
                    android.opengl.GLES20.glEnableVertexAttribArray(texCoordHandle)
                    android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4)
                    
                    // Set presentation time for this frame (in nanoseconds)
                    // Use time-based calculation instead of frame-based to ensure correct timing
                    // This prevents "Dropping frame that's going backward in time" errors
                    val presentationTimeNs = timeMs * 1_000_000L // Convert milliseconds to nanoseconds
                    // Ensure presentation time is always strictly increasing
                    val finalPresentationTimeNs = if (presentationTimeNs > lastPresentationTimeNs) {
                        presentationTimeNs
                    } else {
                        // If somehow time went backward, increment from last time
                        lastPresentationTimeNs + (1_000_000_000L / finalFrameRate)
                    }
                    lastPresentationTimeNs = finalPresentationTimeNs
                    
                    try {
                        // Use reflection to call eglPresentationTimeANDROID if available
                        val eglExtClass = Class.forName("android.opengl.EGLExt")
                        val eglPresentationTimeMethod = eglExtClass.getMethod(
                            "eglPresentationTimeANDROID",
                            android.opengl.EGLDisplay::class.java,
                            android.opengl.EGLSurface::class.java,
                            Long::class.java
                        )
                        eglPresentationTimeMethod.invoke(null, eglDisplay, eglSurface, finalPresentationTimeNs)
                        if (frameCount % 30 == 0) { // Log every 30 frames to reduce spam
                            Log.d("VideoTrimmerActivity", "Set presentation time: ${finalPresentationTimeNs}ns (${timeMs}ms, frame $frameCount)")
                        }
                    } catch (e: Exception) {
                        // EGL extension not available, encoder will use automatic timing
                        if (frameCount == 0) {
                        Log.d("VideoTrimmerActivity", "EGL presentation time extension not available, using automatic timing")
                        }
                    }
                    
                    // Log overlay drawing for first few frames to verify it's working
                    val overlayInfo = if (scaledOverlay != null) {
                        "overlay: ${scaledOverlay.width}x${scaledOverlay.height}"
                    } else {
                        "no overlay"
                    }
                    if (frameCount < 3) {
                        Log.d("VideoTrimmerActivity", "Frame ${frameCount + 1}: Composite ${videoWidth}x${videoHeight}, $overlayInfo, time=${timeMs}ms, presentationTime=${finalPresentationTimeNs}ns")
                    }
                    
                    // Present frame to encoder
                    android.opengl.EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                    
                    // Give encoder a moment to process the frame
                    // Use a small delay to prevent overwhelming the encoder
                    // The actual frame rate is controlled by presentation timestamps
                    Thread.sleep(10)
                    
                    // Drain encoder output buffers AFTER submitting frame
                    // This ensures we process all available output buffers after each frame
                    if (!outputEOSReceived && muxerStarted) {
                        // Drain all available buffers aggressively
                        var drained = false
                        var drainIterations = 0
                        val maxDrainIterations = 200 // Increased limit to drain more buffers
                        while (!drained && drainIterations < maxDrainIterations) {
                            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                            when {
                                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                                    drained = true
                                }
                                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    // Format changed - this should not happen after muxer started
                                    if (!muxerStarted) {
                                        val outputFormat = encoder.outputFormat
                                        if (!outputFormat.containsKey(MediaFormat.KEY_FRAME_RATE) || 
                                            outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE) == null ||
                                            outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)!! <= 0) {
                                            Log.w("VideoTrimmerActivity", "Encoder output format missing or invalid frame rate during drain, setting to $validFrameRate")
                                            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, validFrameRate)
                                        } else {
                                            val outputFrameRate = outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)!!
                                            if (outputFrameRate < 15 || outputFrameRate > 60) {
                                                Log.w("VideoTrimmerActivity", "Encoder output format has invalid frame rate $outputFrameRate during drain, overriding with $finalFrameRate")
                                                outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, finalFrameRate)
                                            }
                                        }
                                        videoTrackIndex = muxer.addTrack(outputFormat)
                                        muxer.start()
                                        muxerStarted = true
                                        Log.d("VideoTrimmerActivity", "Muxer started with video track index: $videoTrackIndex")
                                    } else {
                                        Log.w("VideoTrimmerActivity", "Encoder output format changed after muxer started")
                                    }
                                }
                                outputBufferIndex >= 0 -> {
                                    if (!muxerStarted) {
                                        Log.e("VideoTrimmerActivity", "Received output buffer before muxer started")
                                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                                        continue
                                    }
                                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                                    if (outputBuffer != null && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                        if (bufferInfo.size > 0) {
                                            muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                            videoSamplesWritten++
                                            if (videoSamplesWritten <= 5 || videoSamplesWritten % 30 == 0) {
                                                Log.d("VideoTrimmerActivity", "Written video sample $videoSamplesWritten: size=${bufferInfo.size}, pts=${bufferInfo.presentationTimeUs}us, flags=${bufferInfo.flags}")
                                            }
                                        }
                                    }
                                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                        outputEOSReceived = true
                                        Log.d("VideoTrimmerActivity", "Encoder signaled end of stream during processing (frame $frameCount)")
                                        // Don't break here - continue processing remaining frames
                                        // The EOS flag just means no more input, but we may still have output buffers
                                    }
                                }
                            }
                            drainIterations++
                        }
                    }
                    
                    android.opengl.GLES20.glDeleteTextures(1, texture, 0)
                    frame.recycle()
                    compositeBitmap.recycle()
                    frameCount++
                    
                    // Log progress every 10 frames to show activity
                    if (frameCount % 10 == 0) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (frameCount * 100) / totalFrames
                        Log.d("VideoTrimmerActivity", "Compositing progress: $frameCount/$totalFrames frames ($progress%) in ${elapsed}ms, samples written: $videoSamplesWritten")
                    }
                } else {
                    Log.w("VideoTrimmerActivity", "Failed to extract frame at ${timeMs}ms")
                }
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            Log.d("VideoTrimmerActivity", "Compositing complete: processed $frameCount frames in ${totalTime}ms (avg ${totalTime/frameCount}ms per frame)")
            
            android.opengl.GLES20.glDeleteProgram(programHandle)
            
            // Cleanup EGL
            android.opengl.EGL14.eglDestroySurface(eglDisplay, eglSurface)
            android.opengl.EGL14.eglDestroyContext(eglDisplay, eglContext)
            android.opengl.EGL14.eglTerminate(eglDisplay)
            
            // Ensure muxer is started before signaling end of stream
            if (!muxerStarted) {
                // Try to get output format one more time
                var formatReceived = false
                var attempts = 0
                while (!formatReceived && attempts < 100) {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                    when {
                        outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                            formatReceived = true
                            Log.d("VideoTrimmerActivity", "Got encoder format and started muxer")
                        }
                        outputBufferIndex >= 0 -> {
                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                        }
                    }
                    attempts++
                }
                if (!muxerStarted) {
                    Log.e("VideoTrimmerActivity", "Failed to get encoder output format - no frames encoded")
                    return false
                }
            }
            
            // Signal end of stream
            encoder.signalEndOfInputStream()
            
            // Wait a bit for encoder to process
            Thread.sleep(100)
            
            // Drain encoder - ensure we get all output buffers
            var outputEOS = false
            var drainAttempts = 0
            val maxDrainAttempts = 2000 // Increased for longer videos
            val maxDrainTimeMs = 30000L // Maximum 30 seconds for draining
            val timeoutUs = 10000L
            val drainStartTime = System.currentTimeMillis()
            
            Log.d("VideoTrimmerActivity", "Starting to drain encoder... (already written $videoSamplesWritten samples)")
            while (!outputEOS && drainAttempts < maxDrainAttempts) {
                // Check timeout
                if (System.currentTimeMillis() - drainStartTime > maxDrainTimeMs) {
                    Log.w("VideoTrimmerActivity", "Encoder drain timeout after ${maxDrainTimeMs}ms, forcing completion")
                    break
                }
                
                // Check if thread was interrupted
                if (Thread.currentThread().isInterrupted) {
                    Log.w("VideoTrimmerActivity", "Encoder drain interrupted")
                    break
                }
                
                drainAttempts++
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output available, continue
                        if (drainAttempts % 100 == 0) {
                            val elapsed = System.currentTimeMillis() - drainStartTime
                            Log.d("VideoTrimmerActivity", "Waiting for encoder output... (attempt $drainAttempts, ${elapsed}ms elapsed, samples: $videoSamplesWritten)")
                        }
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Format changed - this should not happen after muxer.start()
                        if (!muxerStarted) {
                            videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                            Log.d("VideoTrimmerActivity", "Got encoder format during drain and started muxer")
                        } else {
                        Log.e("VideoTrimmerActivity", "Encoder output format changed after muxer started - this is unexpected")
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        if (!muxerStarted) {
                            Log.e("VideoTrimmerActivity", "Received output buffer before muxer started during drain")
                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                            continue
                        }
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            if (bufferInfo.size > 0) {
                                muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                videoSamplesWritten++
                                if (videoSamplesWritten % 10 == 0) {
                                    Log.d("VideoTrimmerActivity", "Written $videoSamplesWritten video samples so far")
                                }
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEOS = true
                            Log.d("VideoTrimmerActivity", "Encoder signaled end of stream, total video samples: $videoSamplesWritten")
                        }
                    }
                }
            }
            
            if (drainAttempts >= maxDrainAttempts) {
                Log.w("VideoTrimmerActivity", "Encoder drain reached max attempts ($maxDrainAttempts), forcing completion")
            }
            val drainTime = System.currentTimeMillis() - drainStartTime
            Log.d("VideoTrimmerActivity", "Encoder drain completed in ${drainTime}ms after $drainAttempts attempts, total samples: $videoSamplesWritten")
            
            // Verify we encoded at least some frames
            if (frameCount == 0) {
                Log.e("VideoTrimmerActivity", "No frames were encoded - video will be invalid")
                return false
            }
            
            // Verify video samples were written
            if (videoSamplesWritten == 0) {
                Log.e("VideoTrimmerActivity", "No video samples were written to muxer - video will be invalid")
                return false
            }
            
            Log.d("VideoTrimmerActivity", "Total video samples written: $videoSamplesWritten")
            
            // Verify muxer started
            if (!muxerStarted) {
                Log.e("VideoTrimmerActivity", "Muxer was never started - video file will be invalid")
                return false
            }
            
            // Copy audio
            if (audioTrackIndex != -1 && muxerStarted) {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val audioBuffer = ByteBuffer.allocate(1024 * 1024)
                var audioSamples = 0
                while (true) {
                    val sampleSize = extractor.readSampleData(audioBuffer, 0)
                    if (sampleSize < 0) break
                    
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(audioTrackIndex, audioBuffer, bufferInfo)
                    extractor.advance()
                    audioSamples++
                }
                Log.d("VideoTrimmerActivity", "Written $audioSamples audio samples")
            }
            
            Log.d("VideoTrimmerActivity", "Successfully composited $frameCount frames, muxerStarted=$muxerStarted")
            
            // Stop muxer before cleanup
            try {
                muxer?.stop()
                muxerStopped = true
                Log.d("VideoTrimmerActivity", "Muxer stopped successfully")
            } catch (e: Exception) {
                Log.e("VideoTrimmerActivity", "Error stopping muxer: ${e.message}", e)
                return false
            }
            
            // Final validation - check if file is valid
            val fileSize = outputFile.length()
            if (fileSize == 0L) {
                Log.e("VideoTrimmerActivity", "Output file is empty")
                return false
            }
            
            Log.d("VideoTrimmerActivity", "Compositing completed successfully, file size: $fileSize bytes")
            true
        } catch (e: Exception) {
            Log.e("VideoTrimmerActivity", "Error compositing overlay: ${e.message}", e)
            e.printStackTrace()
            false
        } finally {
            // Recycle scaled overlay if it was created separately
            scaledOverlay?.let {
                if (it != overlayBitmap) {
                    try {
                        it.recycle()
                    } catch (e: Exception) {
                        Log.w("VideoTrimmerActivity", "Error recycling scaled overlay: ${e.message}")
                    }
                }
            }
            retriever?.release()
            try {
                encoder?.stop()
            } catch (e: Exception) {
                Log.w("VideoTrimmerActivity", "Error stopping encoder: ${e.message}")
            }
            try {
                encoder?.release()
            } catch (e: Exception) {
                Log.w("VideoTrimmerActivity", "Error releasing encoder: ${e.message}")
            }
            extractor?.release()
            // Only stop muxer if it wasn't already stopped
            if (!muxerStopped) {
                try {
                    muxer?.stop()
                } catch (e: Exception) {
                    Log.w("VideoTrimmerActivity", "Error stopping muxer in finally: ${e.message}")
                }
            }
            try {
                muxer?.release()
            } catch (e: Exception) {
                Log.w("VideoTrimmerActivity", "Error releasing muxer: ${e.message}")
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = android.opengl.GLES20.glCreateShader(type)
        android.opengl.GLES20.glShaderSource(shader, shaderCode)
        android.opengl.GLES20.glCompileShader(shader)
        return shader
    }
    
    private fun setSavingState(isSaving: Boolean) {
        binding.save.isEnabled = !isSaving
        binding.save.alpha = if (isSaving) 0.4f else 1f
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
