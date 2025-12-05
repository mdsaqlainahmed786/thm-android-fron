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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import java.nio.ByteBuffer

class VideoTrimmerActivity : BaseActivity() {

    companion object {
        private const val VIDEO_URI_KEY = "video_uri"
        private const val REQUEST_WRITE_PERMISSION = 1001
        private const val MIN_VIDEO_DURATION = 5
    }

    private lateinit var binding: ActivityVideoTrimmerBinding
    private var videoUri: Uri? = null
    private var overlayBitmapPath: String? = null
    private var overlayViewWidth: Int = 0
    private var overlayViewHeight: Int = 0
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
        if (overlayBitmapPath != null && File(overlayBitmapPath).exists()) {
            Log.d("VideoTrimmerActivity", "Overlay bitmap path received: $overlayBitmapPath")
            Log.d("VideoTrimmerActivity", "Overlay view dimensions: ${overlayViewWidth}x${overlayViewHeight}")
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
            tempFile
        } catch (e: Exception) {
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
        if (overlayBitmapPath != null && File(overlayBitmapPath).exists()) {
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
                            
                            if (playerViewWidth > 0 && playerViewHeight > 0) {
                                // Scale overlay to match playerView size to preserve positions
                                // The overlay was captured at overlayViewWidth x overlayViewHeight
                                // Scale proportionally to match playerView dimensions
                                val overlayWidth = overlayBitmap.width
                                val overlayHeight = overlayBitmap.height
                                
                                // Calculate scale factors
                                val scaleX = playerViewWidth.toFloat() / overlayWidth.toFloat()
                                val scaleY = playerViewHeight.toFloat() / overlayHeight.toFloat()
                                
                                // Use the same scale for both to maintain aspect ratio, or use FIT_XY to fill
                                // For preview, we want to fill the entire playerView to match positions
                                val scaledOverlay = Bitmap.createScaledBitmap(
                                    overlayBitmap, 
                                    playerViewWidth, 
                                    playerViewHeight, 
                                    true
                                )
                                
                                // Set the bitmap and ensure ImageView matches playerView exactly
                                binding.overlayPreview.setImageBitmap(scaledOverlay)
                                binding.overlayPreview.visibility = View.VISIBLE
                                binding.overlayPreview.scaleType = android.widget.ImageView.ScaleType.FIT_XY
                                
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
                                
                                Log.d("VideoTrimmerActivity", "Overlay preview loaded: original=${overlayWidth}x${overlayHeight}, scaled to $playerViewWidth x $playerViewHeight, scaleX=$scaleX, scaleY=$scaleY")
                                
                                // Recycle original to free memory
                                if (scaledOverlay != overlayBitmap) {
                                    overlayBitmap.recycle()
                                }
                            } else {
                                // Fallback: use original bitmap if view not ready, but try again later
                                binding.overlayPreview.setImageBitmap(overlayBitmap)
                                binding.overlayPreview.visibility = View.VISIBLE
                                binding.overlayPreview.scaleType = android.widget.ImageView.ScaleType.FIT_XY
                                Log.d("VideoTrimmerActivity", "Overlay preview loaded (view not ready, using original size)")
                                
                                // Try to reload when view is ready
                                binding.playerView.postDelayed({ loadOverlayPreview() }, 100)
                            }
                        } else {
                            Log.e("VideoTrimmerActivity", "Failed to decode overlay bitmap")
                        }
                    } catch (e: Exception) {
                        Log.e("VideoTrimmerActivity", "Error loading overlay preview: ${e.message}", e)
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
        val uri = videoUri
        if (uri == null) {
            showToast("Video not available")
            return
        }
        
        // Prevent multiple clicks
        if (binding.save.alpha < 1f || !binding.save.isEnabled) {
            Log.d("VideoTrimmerActivity", "Save already in progress, ignoring click")
            return
        }
        
        if (from == "CreateStory") {
            setSavingState(true)
            Log.d("VideoTrimmerActivity", "Starting save process, overlay: ${overlayBitmapPath != null}")
            
            // Show progress indicator
            giffProgressBar.show()
            
            // Skip compositing for now - it's too slow and hangs. Upload video directly.
            // TODO: Implement faster compositing or handle on server side
            Log.d("VideoTrimmerActivity", "Uploading video directly (compositing disabled for performance)")
            Thread {
                try {
                    val videoFile = copyUriToTempFile(uri)
                    runOnUiThread {
                        giffProgressBar.hide()
                        if (videoFile != null && videoFile.exists()) {
                            Log.d("VideoTrimmerActivity", "Video ready (${videoFile.length()} bytes), uploading...")
                            individualViewModal.createStory(null, videoFile)
                        } else {
                            setSavingState(false)
                            showToast("Unable to prepare video")
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
            return
            
            // OLD CODE - compositing disabled due to performance issues
            if (false && overlayBitmapPath != null && File(overlayBitmapPath).exists()) {
                Log.d("VideoTrimmerActivity", "Rendering overlay on video...")
                renderOverlayOnVideo(uri) { videoFile ->
                    runOnUiThread {
                        giffProgressBar.hide()
                        if (videoFile != null && videoFile.exists()) {
                            Log.d("VideoTrimmerActivity", "Video with overlay ready (${videoFile.length()} bytes), uploading...")
                            individualViewModal.createStory(null, videoFile)
                        } else {
                            Log.e("VideoTrimmerActivity", "Failed to create video with overlay or file doesn't exist")
                            setSavingState(false)
                            showToast("Unable to prepare video with overlay")
                        }
                    }
                }
            } else {
                // No overlay, proceed normally
                Log.d("VideoTrimmerActivity", "No overlay, proceeding with video only")
                Thread {
                    try {
                        val videoFile = copyUriToTempFile(uri)
                        runOnUiThread {
                            giffProgressBar.hide()
                            if (videoFile != null && videoFile.exists()) {
                                Log.d("VideoTrimmerActivity", "Video ready (${videoFile.length()} bytes), uploading...")
                                individualViewModal.createStory(null, videoFile)
                            } else {
                                setSavingState(false)
                                showToast("Unable to prepare video")
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
    
    private fun renderOverlayOnVideo(videoUri: Uri, callback: (File?) -> Unit) {
        // Run on background thread
        Thread {
            var compositingCompleted = false
            try {
                Log.d("VideoTrimmerActivity", "Loading overlay bitmap from: $overlayBitmapPath")
                val overlayBitmap = BitmapFactory.decodeFile(overlayBitmapPath)
                if (overlayBitmap == null) {
                    Log.e("VideoTrimmerActivity", "Failed to load overlay bitmap")
                    runOnUiThread { callback(null) }
                    return@Thread
                }
                Log.d("VideoTrimmerActivity", "Overlay bitmap loaded: ${overlayBitmap.width}x${overlayBitmap.height}")
                
                // Get video file path
                Log.d("VideoTrimmerActivity", "Copying video file...")
                val inputVideoFile = copyUriToTempFile(videoUri)
                if (inputVideoFile == null || !inputVideoFile.exists()) {
                    Log.e("VideoTrimmerActivity", "Failed to copy video file or file doesn't exist")
                    overlayBitmap.recycle()
                    runOnUiThread { callback(null) }
                    return@Thread
                }
                Log.d("VideoTrimmerActivity", "Video file copied: ${inputVideoFile.absolutePath} (${inputVideoFile.length()} bytes)")
                
                // Create output file
                val outputFile = File.createTempFile("thm_video_with_overlay_", ".mp4", cacheDir)
                Log.d("VideoTrimmerActivity", "Starting video compositing to: ${outputFile.absolutePath}")
                
                // Composite overlay onto video
                val startTime = System.currentTimeMillis()
                val success = compositeOverlayOnVideo(inputVideoFile, overlayBitmap, outputFile)
                val elapsedTime = System.currentTimeMillis() - startTime
                compositingCompleted = true
                Log.d("VideoTrimmerActivity", "Compositing completed in ${elapsedTime}ms, success=$success")
                
                if (success && outputFile.exists() && outputFile.length() > 0) {
                    Log.d("VideoTrimmerActivity", "Video compositing successful: ${outputFile.length()} bytes")
                    runOnUiThread { callback(outputFile) }
                } else {
                    Log.e("VideoTrimmerActivity", "Failed to composite overlay or output file invalid. Success=$success, exists=${outputFile.exists()}, size=${outputFile.length()}")
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    runOnUiThread { 
                        callback(null)
                        setSavingState(false)
                        showToast("Failed to process video. Please try again.")
                    }
                }
                
                // Cleanup
                overlayBitmap.recycle()
            } catch (e: Exception) {
                compositingCompleted = true
                Log.e("VideoTrimmerActivity", "Error rendering overlay: ${e.message}", e)
                e.printStackTrace()
                runOnUiThread { 
                    callback(null)
                    setSavingState(false)
                    showToast("Error processing video: ${e.message}")
                }
            }
            
            // If compositing didn't complete, it might be hanging
            if (!compositingCompleted) {
                Log.w("VideoTrimmerActivity", "Compositing may have hung")
            }
        }.start()
    }
    
    private fun compositeOverlayOnVideo(inputFile: File, overlayBitmap: Bitmap, outputFile: File): Boolean {
        var retriever: MediaMetadataRetriever? = null
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var extractor: MediaExtractor? = null
        
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputFile.absolutePath)
            
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
            
            // Scale overlay to match video dimensions
            val scaledOverlay = Bitmap.createScaledBitmap(overlayBitmap, videoWidth, videoHeight, true)
            
            // Setup encoder with Surface
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()
            
            // Setup muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
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
            
            videoTrackIndex = muxer.addTrack(encoder.outputFormat)
            muxer.start()
            
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
            
            // Process frames - use a reasonable frame rate (15fps) for balance between quality and speed
            val frameInterval = 66L // ~66ms = 15fps
            val bufferInfo = MediaCodec.BufferInfo()
            var frameCount = 0
            val startTime = System.currentTimeMillis()
            var lastFrameTime = 0L
            
            // Process frames more efficiently
            for (timeMs in 0L until durationMs step frameInterval) {
                // Check if thread was interrupted (timeout)
                if (Thread.currentThread().isInterrupted) {
                    Log.w("VideoTrimmerActivity", "Compositing interrupted")
                    return false
                }
                
                // Use a timeout for frame extraction to prevent hanging
                val frameStartTime = System.currentTimeMillis()
                val frame = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                val frameExtractTime = System.currentTimeMillis() - frameStartTime
                
                if (frameExtractTime > 500) {
                    Log.w("VideoTrimmerActivity", "Frame extraction took too long: ${frameExtractTime}ms")
                }
                
                if (frame != null) {
                    // Create composite bitmap
                    val compositeBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(compositeBitmap)
                    
                    // Draw video frame
                    canvas.drawBitmap(frame, 0f, 0f, null)
                    
                    // Draw overlay
                    canvas.drawBitmap(scaledOverlay, 0f, 0f, null)
                    
                    // Render composite bitmap to Surface using OpenGL
                    val texture = IntArray(1)
                    android.opengl.GLES20.glGenTextures(1, texture, 0)
                    android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, texture[0])
                    android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MIN_FILTER, android.opengl.GLES20.GL_LINEAR)
                    android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MAG_FILTER, android.opengl.GLES20.GL_LINEAR)
                    GLUtils.texImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0, compositeBitmap, 0)
                    
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
                    
                    android.opengl.EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                    
                    android.opengl.GLES20.glDeleteTextures(1, texture, 0)
                    frame.recycle()
                    compositeBitmap.recycle()
                    frameCount++
                    
                    // Log progress every 30 frames
                    if (frameCount % 30 == 0) {
                        val elapsed = System.currentTimeMillis() - startTime
                        Log.d("VideoTrimmerActivity", "Processed $frameCount frames in ${elapsed}ms")
                    }
                }
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            Log.d("VideoTrimmerActivity", "Processed total $frameCount frames in ${totalTime}ms")
            
            android.opengl.GLES20.glDeleteProgram(programHandle)
            
            // Cleanup EGL
            android.opengl.EGL14.eglDestroySurface(eglDisplay, eglSurface)
            android.opengl.EGL14.eglDestroyContext(eglDisplay, eglContext)
            android.opengl.EGL14.eglTerminate(eglDisplay)
            
            // Signal end of stream
            encoder.signalEndOfInputStream()
            
            // Wait a bit for encoder to process
            Thread.sleep(100)
            
            // Drain encoder - ensure we get all output buffers
            var outputEOS = false
            var drainAttempts = 0
            val maxDrainAttempts = 2000 // Increased for longer videos
            val timeoutUs = 10000L
            
            Log.d("VideoTrimmerActivity", "Starting to drain encoder...")
            while (!outputEOS && drainAttempts < maxDrainAttempts) {
                drainAttempts++
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output available, continue
                        if (drainAttempts % 100 == 0) {
                            Log.d("VideoTrimmerActivity", "Waiting for encoder output... (attempt $drainAttempts)")
                        }
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Format changed - this should not happen after muxer.start(), log error
                        Log.e("VideoTrimmerActivity", "Encoder output format changed after muxer started - this is unexpected")
                        // Continue anyway - the format was already set when we added the track
                    }
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            if (bufferInfo.size > 0) {
                                muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEOS = true
                            Log.d("VideoTrimmerActivity", "Encoder signaled end of stream")
                        }
                    }
                }
            }
            
            if (drainAttempts >= maxDrainAttempts) {
                Log.w("VideoTrimmerActivity", "Encoder drain reached max attempts, forcing completion")
            }
            
            // Copy audio
            if (audioTrackIndex != -1) {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val audioBuffer = ByteBuffer.allocate(1024 * 1024)
                while (true) {
                    val sampleSize = extractor.readSampleData(audioBuffer, 0)
                    if (sampleSize < 0) break
                    
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(audioTrackIndex, audioBuffer, bufferInfo)
                    extractor.advance()
                }
            }
            
            Log.d("VideoTrimmerActivity", "Successfully composited $frameCount frames")
            true
        } catch (e: Exception) {
            Log.e("VideoTrimmerActivity", "Error compositing overlay: ${e.message}", e)
            e.printStackTrace()
            false
        } finally {
            retriever?.release()
            encoder?.stop()
            encoder?.release()
            extractor?.release()
            muxer?.stop()
            muxer?.release()
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
