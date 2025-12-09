package com.thehotelmedia.android.utils.video

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Main engine for exporting video with overlays baked in.
 * 
 * This class handles the complete pipeline:
 * 1. Reads video metadata (dimensions, frame rate, duration)
 * 2. Sets up MediaCodec encoder with proper configuration
 * 3. Sets up MediaMuxer for MP4 output
 * 4. Sets up OpenGL context for rendering to encoder's input surface
 * 5. Processes each frame: extracts → composites → encodes
 * 6. Handles audio track copying
 * 7. Manages presentation timestamps
 * 8. Handles encoder buffer draining
 * 
 * The output is a valid MP4 file with overlays permanently embedded.
 */
class VideoExportEngine {
    
    companion object {
        private const val TAG = "VideoExportEngine"
        private const val TIMEOUT_USEC = 10000L
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
        private const val BIT_RATE = 2_000_000 // 2 Mbps
        private const val I_FRAME_INTERVAL = 1 // Key frame every second
        private const val MIN_FRAME_RATE = 15
        private const val MAX_FRAME_RATE = 60
        private const val DEFAULT_FRAME_RATE = 30
    }
    
    /**
     * Exports video with overlay baked in.
     * 
     * @param inputVideoFile Input video file
     * @param overlayBitmap Scaled overlay bitmap (already scaled to video dimensions)
     * @param outputFile Output MP4 file
     * @return true if export succeeds, false otherwise
     */
    fun exportVideoWithOverlay(
        inputVideoFile: File,
        overlayBitmap: Bitmap?,
        outputFile: File
    ): Boolean {
        Log.e("ZZZZZZ_EXPORT", "EXPORT STARTED")
        var retriever: MediaMetadataRetriever? = null
        var extractor: MediaExtractor? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var eglDisplay: EGLDisplay? = null
        var eglContext: EGLContext? = null
        var eglSurface: EGLSurface? = null
        var programHandle = 0
        var inputSurface: android.view.Surface? = null
        
        return try {
            Log.d(TAG, "Starting video export: ${inputVideoFile.absolutePath} -> ${outputFile.absolutePath}")
            
            // Step 1: Get video metadata
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputVideoFile.absolutePath)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            if (durationMs == 0L) {
                Log.e(TAG, "Failed to get video duration")
                return false
            }
            
            val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            if (videoWidth == 0 || videoHeight == 0) {
                Log.e(TAG, "Failed to get video dimensions")
                return false
            }
            
            Log.d(TAG, "Video metadata: ${videoWidth}x${videoHeight}, ${durationMs}ms")
            
            // Step 2: Get frame rate
            val frameRate = extractFrameRate(inputVideoFile, retriever)
            Log.d(TAG, "Using frame rate: ${frameRate}fps")
            
            // Step 3: Setup encoder
            val format = MediaFormat.createVideoFormat(MIME_TYPE, videoWidth, videoHeight)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            
            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()
            
            Log.d(TAG, "Encoder configured: ${videoWidth}x${videoHeight} @ ${frameRate}fps")
            
            // Step 4: Setup muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // Step 5: Copy audio track
            extractor = MediaExtractor()
            extractor.setDataSource(inputVideoFile.absolutePath)
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = muxer.addTrack(trackFormat)
                    break
                }
            }
            
            // Step 6: Setup OpenGL
            val eglSetup = setupOpenGL(inputSurface!!, videoWidth, videoHeight)
            eglDisplay = eglSetup.first
            eglContext = eglSetup.second
            eglSurface = eglSetup.third
            programHandle = eglSetup.fourth
            
            // Step 7: Wait for encoder output format
            val bufferInfo = MediaCodec.BufferInfo()
            var videoTrackIndex = -1
            var muxerStarted = false
            
            // Render dummy frame to trigger encoder output format
            renderDummyFrame(eglDisplay!!, eglSurface!!, programHandle, videoWidth, videoHeight)
            
            // Wait for output format
            var formatReceived = false
            var attempts = 0
            while (!formatReceived && attempts < 100) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = encoder.outputFormat
                        // Ensure frame rate is valid
                        if (!outputFormat.containsKey(MediaFormat.KEY_FRAME_RATE) ||
                            outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE) == null ||
                            outputFormat.getInteger(MediaFormat.KEY_FRAME_RATE)!! <= 0) {
                            outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                        }
                        videoTrackIndex = muxer.addTrack(outputFormat)
                        muxer.start()
                        muxerStarted = true
                        formatReceived = true
                        Log.d(TAG, "Muxer started with video track index: $videoTrackIndex")
                    }
                    outputBufferIndex >= 0 -> {
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
                attempts++
                if (!formatReceived) Thread.sleep(10)
            }
            
            if (!muxerStarted) {
                Log.e(TAG, "Failed to get encoder output format")
                return false
            }
            
            // Step 8: Process frames
            val frameInterval = (1000L / frameRate).toLong()
            var frameCount = 0
            var videoSamplesWritten = 0
            var lastPresentationTimeNs = 0L
            
            // Store frameCount reference for diagnostics
            val frameCountRef = intArrayOf(0)
            
            Log.d(TAG, "Processing frames: duration=${durationMs}ms, ~${durationMs / frameInterval} frames at ${frameRate}fps")
            
            for (timeMs in 0L until durationMs step frameInterval) {
                if (Thread.currentThread().isInterrupted) {
                    Log.w(TAG, "Export interrupted")
                    return false
                }
                
                // Extract frame
                val frame = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame == null) {
                    Log.w(TAG, "Failed to extract frame at ${timeMs}ms")
                    continue
                }
                
                // CRITICAL: Verify overlay bitmap before compositing (first frame only)
                if (frameCountRef[0] == 0 && overlayBitmap != null) {
                    Log.d(TAG, "=== OVERLAY BITMAP VERIFICATION (Frame 0) ===")
                    Log.d(TAG, "Overlay bitmap: ${overlayBitmap.width}x${overlayBitmap.height}, " +
                            "config=${overlayBitmap.config}, hasAlpha=${overlayBitmap.hasAlpha()}, " +
                            "isRecycled=${overlayBitmap.isRecycled}")
                    
                    // Count non-transparent pixels
                    var nonTransparentCount = 0
                    val sampleSize = minOf(1000, overlayBitmap.width * overlayBitmap.height)
                    val stepX = maxOf(1, overlayBitmap.width / 20)
                    val stepY = maxOf(1, overlayBitmap.height / 20)
                    for (y in 0 until overlayBitmap.height step stepY) {
                        for (x in 0 until overlayBitmap.width step stepX) {
                            val pixel = overlayBitmap.getPixel(x, y)
                            if ((pixel shr 24) and 0xFF > 0) {
                                nonTransparentCount++
                            }
                        }
                    }
                    Log.d(TAG, "Overlay non-transparent pixels (sampled): $nonTransparentCount / $sampleSize")
                    
                    if (nonTransparentCount == 0) {
                        Log.e(TAG, "*** CRITICAL: Overlay bitmap has NO non-transparent pixels! ***")
                    }
                }
                
                // Calculate presentation time BEFORE rendering (FIXED ORDER)
                val presentationTimeNs = timeMs * 1_000_000L
                val finalPresentationTimeNs = if (presentationTimeNs > lastPresentationTimeNs) {
                    presentationTimeNs
                } else {
                    lastPresentationTimeNs + (1_000_000_000L / frameRate)
                }
                lastPresentationTimeNs = finalPresentationTimeNs
                
                // Set presentation time BEFORE rendering (CRITICAL FIX)
                setPresentationTime(eglDisplay!!, eglSurface!!, finalPresentationTimeNs)
                
                // Composite frame with overlay
                val compositeFrame = VideoFrameCompositor.compositeFrame(
                    frame,
                    overlayBitmap,
                    videoWidth,
                    videoHeight
                )
                
                if (compositeFrame == null) {
                    Log.w(TAG, "Failed to composite frame at ${timeMs}ms")
                    frame.recycle()
                    continue
                }
                
                // Render to encoder
                val renderSuccess = renderFrameToEncoder(
                    eglDisplay!!,
                    eglSurface!!,
                    programHandle,
                    compositeFrame,
                    videoWidth,
                    videoHeight,
                    frameCountRef[0]
                )
                
                if (!renderSuccess && frameCountRef[0] < 3) {
                    Log.e(TAG, "*** RENDER FAILED for frame ${frameCountRef[0]} ***")
                }
                
                // Present to encoder (presentation time already set)
                EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                
                // Check for OpenGL errors
                val glError = GLES20.glGetError()
                if (glError != GLES20.GL_NO_ERROR && frameCountRef[0] < 3) {
                    Log.e(TAG, "*** OpenGL ERROR after frame ${frameCountRef[0]}: $glError ***")
                }
                Thread.sleep(10) // Give encoder time to process
                
                // Drain encoder output
                drainEncoderOutput(encoder, bufferInfo, muxer, videoTrackIndex, muxerStarted)
                
                // Cleanup
                frame.recycle()
                compositeFrame.recycle()
                frameCount++
                frameCountRef[0] = frameCount
                
                if (frameCount % 30 == 0) {
                    Log.d(TAG, "Progress: $frameCount frames processed")
                }
            }
            
            // Step 9: Signal end of stream
            signalEndOfStream(encoder, eglDisplay!!, eglSurface!!, programHandle, videoWidth, videoHeight)
            
            // Step 10: Final drain
            drainEncoderOutput(encoder, bufferInfo, muxer, videoTrackIndex, muxerStarted, true)
            
            // Step 11: Copy audio
            if (audioTrackIndex >= 0) {
                copyAudioTrack(extractor!!, muxer, audioTrackIndex)
            }
            
            // Step 12: Stop muxer
            muxer.stop()
            muxer.release()
            
            Log.d(TAG, "Export completed: $frameCount frames, output: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            
            // Verify output file
            if (outputFile.exists() && outputFile.length() > 0) {
                true
            } else {
                Log.e(TAG, "Output file is invalid")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during export: ${e.message}", e)
            false
        } finally {
            // Cleanup
            try {
                inputSurface?.release()
                encoder?.stop()
                encoder?.release()
                extractor?.release()
                retriever?.release()
                
                if (eglDisplay != null && eglSurface != null) {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                if (eglDisplay != null && eglContext != null) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                }
                if (eglDisplay != null) {
                    EGL14.eglTerminate(eglDisplay)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup: ${e.message}", e)
            }
        }
    }
    
    /**
     * Extracts frame rate from video file
     */
    private fun extractFrameRate(videoFile: File, retriever: MediaMetadataRetriever): Int {
        // Try MediaExtractor first (more reliable)
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(videoFile.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    if (trackFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        val frameRate = trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                        if (frameRate != null && frameRate >= MIN_FRAME_RATE && frameRate <= MAX_FRAME_RATE) {
                            extractor.release()
                            return frameRate
                        }
                    }
                }
            }
            extractor.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get frame rate from extractor: ${e.message}")
        }
        
        // Fallback to metadata
        try {
            val frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            val frameRate = frameRateStr?.toIntOrNull()
            if (frameRate != null && frameRate >= MIN_FRAME_RATE && frameRate <= MAX_FRAME_RATE) {
                return frameRate
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get frame rate from metadata: ${e.message}")
        }
        
        // Default
        return DEFAULT_FRAME_RATE
    }
    
    /**
     * Sets up OpenGL context for rendering to encoder surface
     */
    private fun setupOpenGL(
        surface: android.view.Surface,
        width: Int,
        height: Int
    ): Quadruple<EGLDisplay, EGLContext, EGLSurface, Int> {
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        
        val attribList = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)
        
        val eglContext = EGL14.eglCreateContext(
            eglDisplay,
            configs[0],
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
            0
        )
        
        val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        
        GLES20.glViewport(0, 0, width, height)
        
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
        
        val vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        val programHandle = GLES20.glCreateProgram()
        GLES20.glAttachShader(programHandle, vertexShaderHandle)
        GLES20.glAttachShader(programHandle, fragmentShaderHandle)
        GLES20.glLinkProgram(programHandle)
        
        return Quadruple(eglDisplay, eglContext, eglSurface, programHandle)
    }
    
    /**
     * Loads a shader
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
    
    /**
     * Renders a dummy frame to trigger encoder output format
     */
    private fun renderDummyFrame(
        eglDisplay: EGLDisplay,
        eglSurface: EGLSurface,
        programHandle: Int,
        width: Int,
        height: Int
    ) {
        val dummyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        dummyBitmap.eraseColor(android.graphics.Color.BLACK)
        renderFrameToEncoder(eglDisplay, eglSurface, programHandle, dummyBitmap, width, height)
        dummyBitmap.recycle()
    }
    
    /**
     * Renders a frame to the encoder
     * CRITICAL: Added diagnostics and error checking
     */
    private fun renderFrameToEncoder(
        eglDisplay: EGLDisplay,
        eglSurface: EGLSurface,
        programHandle: Int,
        bitmap: Bitmap,
        width: Int,
        height: Int,
        frameCount: Int = 0
    ): Boolean {
        return try {
            // Check for OpenGL errors before starting
            var glError = GLES20.glGetError()
            if (glError != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "OpenGL error before render: $glError")
            }
            
            // Create texture
            val texture = IntArray(1)
            GLES20.glGenTextures(1, texture, 0)
            glError = GLES20.glGetError()
            if (glError != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "OpenGL error after glGenTextures: $glError")
                return false
            }
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            
            // CRITICAL: Upload bitmap to texture
            // Log bitmap state before upload (first frame only)
            if (frameCount < 3) {
                Log.d(TAG, "Uploading bitmap to texture: ${bitmap.width}x${bitmap.height}, " +
                        "config=${bitmap.config}, hasAlpha=${bitmap.hasAlpha()}, isRecycled=${bitmap.isRecycled}")
                
                // Sample bitmap pixel before upload
                val sampleX = minOf(100, bitmap.width - 1)
                val sampleY = minOf(100, bitmap.height - 1)
                val samplePixel = bitmap.getPixel(sampleX, sampleY)
                val sampleAlpha = (samplePixel shr 24) and 0xFF
                val sampleR = (samplePixel shr 16) and 0xFF
                val sampleG = (samplePixel shr 8) and 0xFF
                val sampleB = samplePixel and 0xFF
                Log.d(TAG, "Bitmap sample pixel at ($sampleX, $sampleY): ARGB($sampleAlpha, $sampleR, $sampleG, $sampleB)")
            }
            
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            
            glError = GLES20.glGetError()
            if (glError != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "*** OpenGL ERROR after texImage2D: $glError ***")
                GLES20.glDeleteTextures(1, texture, 0)
                return false
            } else if (frameCount < 3) {
                Log.d(TAG, "Texture uploaded successfully")
            }
            
            // Clear and draw
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            
            GLES20.glUseProgram(programHandle)
            glError = GLES20.glGetError()
            if (glError != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "OpenGL error after glUseProgram: $glError")
            }
            
            val positionHandle = GLES20.glGetAttribLocation(programHandle, "aPosition")
            val texCoordHandle = GLES20.glGetAttribLocation(programHandle, "aTexCoord")
            val textureHandle = GLES20.glGetUniformLocation(programHandle, "uTexture")
            
            if (positionHandle < 0 || texCoordHandle < 0 || textureHandle < 0) {
                Log.e(TAG, "*** Invalid shader attribute locations: pos=$positionHandle, tex=$texCoordHandle, texHandle=$textureHandle ***")
                GLES20.glDeleteTextures(1, texture, 0)
                return false
            }
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
            GLES20.glUniform1i(textureHandle, 0)
            
            // Draw fullscreen quad
            val vertices = floatArrayOf(
                -1f, -1f, 0f, 1f,  // Bottom-left: position (x,y), texture (u,v)
                1f, -1f, 1f, 1f,   // Bottom-right
                -1f, 1f, 0f, 0f,   // Top-left
                1f, 1f, 1f, 0f      // Top-right
            )
            val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(vertices)
            vertexBuffer.position(0)
            
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 4 * 4, vertexBuffer)
            GLES20.glEnableVertexAttribArray(positionHandle)
            vertexBuffer.position(2)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 4 * 4, vertexBuffer)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            
            glError = GLES20.glGetError()
            if (glError != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "*** OpenGL ERROR after glDrawArrays: $glError ***")
                GLES20.glDeleteTextures(1, texture, 0)
                return false
            } else if (frameCount < 3) {
                Log.d(TAG, "Frame drawn successfully to encoder surface")
            }
            
            // Cleanup texture (safe to delete after draw)
            GLES20.glDeleteTextures(1, texture, 0)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in renderFrameToEncoder: ${e.message}", e)
            false
        }
    }
    
    /**
     * Sets presentation time for a frame
     */
    private fun setPresentationTime(eglDisplay: EGLDisplay, eglSurface: EGLSurface, timeNs: Long) {
        try {
            val eglExtClass = Class.forName("android.opengl.EGLExt")
            val method = eglExtClass.getMethod(
                "eglPresentationTimeANDROID",
                EGLDisplay::class.java,
                EGLSurface::class.java,
                Long::class.java
            )
            method.invoke(null, eglDisplay, eglSurface, timeNs)
        } catch (e: Exception) {
            // EGL extension not available, encoder will use automatic timing
        }
    }
    
    /**
     * Drains encoder output buffers
     */
    private fun drainEncoderOutput(
        encoder: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        videoTrackIndex: Int,
        muxerStarted: Boolean,
        isEOS: Boolean = false
    ) {
        if (!muxerStarted) return
        
        var drained = false
        var iterations = 0
        while (!drained && iterations < 200) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, if (isEOS) TIMEOUT_USEC else 0)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    drained = true
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // Should not happen after muxer started
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
                        drained = true
                    }
                }
            }
            iterations++
        }
    }
    
    /**
     * Signals end of stream to encoder
     */
    private fun signalEndOfStream(
        encoder: MediaCodec,
        eglDisplay: EGLDisplay,
        eglSurface: EGLSurface,
        programHandle: Int,
        width: Int,
        height: Int
    ) {
        // Render one more frame to signal EOS
        val dummyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        dummyBitmap.eraseColor(android.graphics.Color.BLACK)
        renderFrameToEncoder(eglDisplay, eglSurface, programHandle, dummyBitmap, width, height)
        dummyBitmap.recycle()
        
        setPresentationTime(eglDisplay, eglSurface, Long.MAX_VALUE)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }
    
    /**
     * Copies audio track from input to output
     */
    private fun copyAudioTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        audioTrackIndex: Int
    ) {
        val buffer = ByteBuffer.allocate(64 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()
        
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) {
                break
            }
            
            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags
            
            muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }
    }
    
    /**
     * Helper data class for returning multiple values
     */
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}

