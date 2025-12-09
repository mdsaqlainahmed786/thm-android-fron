package com.thehotelmedia.android.utils.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for compositing video frames with overlay bitmaps.
 * 
 * This class handles the frame-by-frame compositing process:
 * - Takes a video frame bitmap
 * - Takes a scaled overlay bitmap
 * - Composites them using proper alpha blending
 * - Returns the composite bitmap ready for encoding
 * 
 * Uses PorterDuff.Mode.SRC_OVER for proper alpha blending that preserves
 * transparency in the overlay while compositing over the video frame.
 */
object VideoFrameCompositor {
    
    private const val TAG = "VideoFrameCompositor"
    private var frameCounter = 0
    
    /**
     * Composites a video frame with an overlay bitmap.
     * 
     * @param videoFrame The video frame bitmap (from MediaMetadataRetriever)
     * @param overlayBitmap The scaled overlay bitmap to composite on top
     * @param frameWidth Target frame width
     * @param frameHeight Target frame height
     * @return Composite bitmap with overlay baked in, or null if compositing fails
     */
    fun compositeFrame(
        videoFrame: Bitmap,
        overlayBitmap: Bitmap?,
        frameWidth: Int,
        frameHeight: Int
    ): Bitmap? {
        return try {
            if (videoFrame.isRecycled) {
                Log.e(TAG, "Video frame is recycled")
                return null
            }
            
            frameCounter++
            val isFirstFrame = frameCounter == 1
            
            // CRITICAL DIAGNOSTIC: Log overlay bitmap state
            if (overlayBitmap == null) {
                Log.w(TAG, "*** OVERLAY BITMAP IS NULL - compositing video frame only ***")
            } else if (overlayBitmap.isRecycled) {
                Log.e(TAG, "*** OVERLAY BITMAP IS RECYCLED - compositing video frame only ***")
                return null
            } else {
                Log.d(TAG, "Overlay bitmap provided: ${overlayBitmap.width}x${overlayBitmap.height}, " +
                        "config=${overlayBitmap.config}, hasAlpha=${overlayBitmap.hasAlpha()}")
                
                // Sample overlay pixel to verify content
                val overlaySampleX = minOf(100, overlayBitmap.width - 1)
                val overlaySampleY = minOf(100, overlayBitmap.height - 1)
                val overlayPixel = overlayBitmap.getPixel(overlaySampleX, overlaySampleY)
                val overlayAlpha = (overlayPixel shr 24) and 0xFF
                val overlayR = (overlayPixel shr 16) and 0xFF
                val overlayG = (overlayPixel shr 8) and 0xFF
                val overlayB = overlayPixel and 0xFF
                Log.d(TAG, "Overlay sample pixel at ($overlaySampleX, $overlaySampleY): " +
                        "ARGB($overlayAlpha, $overlayR, $overlayG, $overlayB)")
            }
            
            // Create composite bitmap
            val compositeBitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(compositeBitmap)
            
            // Draw video frame first (as the base layer)
            canvas.drawBitmap(videoFrame, 0f, 0f, null)
            
            // Sample original frame pixel for comparison
            val originalSampleX = minOf(100, videoFrame.width - 1)
            val originalSampleY = minOf(100, videoFrame.height - 1)
            val originalPixel = videoFrame.getPixel(originalSampleX, originalSampleY)
            val originalAlpha = (originalPixel shr 24) and 0xFF
            val originalR = (originalPixel shr 16) and 0xFF
            val originalG = (originalPixel shr 8) and 0xFF
            val originalB = originalPixel and 0xFF
            
            // Draw overlay on top with proper alpha blending
            if (overlayBitmap != null && !overlayBitmap.isRecycled) {
                val paint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                    // SRC_OVER mode composites the source (overlay) over the destination (video frame)
                    // This preserves the overlay's alpha channel, making transparent areas show the video underneath
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                }
                
                canvas.drawBitmap(overlayBitmap, 0f, 0f, paint)
                
                // CRITICAL DIAGNOSTIC: Compare composite with original
                val compositePixel = compositeBitmap.getPixel(originalSampleX, originalSampleY)
                val compositeAlpha = (compositePixel shr 24) and 0xFF
                val compositeR = (compositePixel shr 16) and 0xFF
                val compositeG = (compositePixel shr 8) and 0xFF
                val compositeB = compositePixel and 0xFF
                
                val pixelDiff = kotlin.math.abs(originalR - compositeR) + 
                                kotlin.math.abs(originalG - compositeG) + 
                                kotlin.math.abs(originalB - compositeB)
                
                // For first 3 frames, do detailed comparison
                if (frameCounter <= 3) {
                    Log.d(TAG, "=== FRAME $frameCounter COMPOSITE VERIFICATION ===")
                    Log.d(TAG, "Original pixel at ($originalSampleX, $originalSampleY): " +
                            "ARGB($originalAlpha, $originalR, $originalG, $originalB)")
                    Log.d(TAG, "Composite pixel at ($originalSampleX, $originalSampleY): " +
                            "ARGB($compositeAlpha, $compositeR, $compositeG, $compositeB)")
                    Log.d(TAG, "Pixel difference: $pixelDiff (threshold: 10)")
                    
                    if (pixelDiff < 10) {
                        Log.w(TAG, "*** WARNING: Composite pixel is too similar to original - overlay may not be drawn ***")
                    } else {
                        Log.d(TAG, "*** SUCCESS: Composite pixel differs from original - overlay is present ***")
                    }
                    
                    // Sample multiple pixels to find differences
                    var differentPixels = 0
                    val samplePoints = listOf(
                        Pair(50, 50), Pair(100, 100), Pair(200, 200),
                        Pair(videoFrame.width / 4, videoFrame.height / 4),
                        Pair(videoFrame.width / 2, videoFrame.height / 2),
                        Pair(videoFrame.width * 3 / 4, videoFrame.height * 3 / 4)
                    )
                    
                    val mismatches = mutableListOf<String>()
                    for ((x, y) in samplePoints) {
                        val safeX = x.coerceIn(0, videoFrame.width - 1)
                        val safeY = y.coerceIn(0, videoFrame.height - 1)
                        val origPix = videoFrame.getPixel(safeX, safeY)
                        val compPix = compositeBitmap.getPixel(safeX, safeY)
                        
                        val origR = (origPix shr 16) and 0xFF
                        val origG = (origPix shr 8) and 0xFF
                        val origB = origPix and 0xFF
                        val compR = (compPix shr 16) and 0xFF
                        val compG = (compPix shr 8) and 0xFF
                        val compB = compPix and 0xFF
                        
                        val diff = kotlin.math.abs(origR - compR) + 
                                   kotlin.math.abs(origG - compG) + 
                                   kotlin.math.abs(origB - compB)
                        
                        if (diff > 10) {
                            differentPixels++
                            if (mismatches.size < 10) {
                                mismatches.add("($safeX, $safeY): orig=ARGB($origR,$origG,$origB), " +
                                        "comp=ARGB($compR,$compG,$compB), diff=$diff")
                            }
                        }
                    }
                    
                    Log.d(TAG, "Sampled ${samplePoints.size} pixels: $differentPixels differ significantly")
                    if (mismatches.isNotEmpty()) {
                        Log.d(TAG, "First ${mismatches.size} mismatching pixels:")
                        mismatches.forEach { Log.d(TAG, "  $it") }
                    }
                    
                    // Save debug images for first frame only
                    if (isFirstFrame) {
                        try {
                            val debugDir = File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_MOVIES), "video_debug")
                            if (!debugDir.exists()) {
                                debugDir.mkdirs()
                            }
                            
                            // Save original frame
                            val originalFile = File(debugDir, "debug_original_1.png")
                            FileOutputStream(originalFile).use { out ->
                                videoFrame.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            Log.d(TAG, "*** SAVED ORIGINAL FRAME: ${originalFile.absolutePath} ***")
                            
                            // Save overlay bitmap if available
                            if (overlayBitmap != null && !overlayBitmap.isRecycled) {
                                val overlayFile = File(debugDir, "debug_overlay.png")
                                FileOutputStream(overlayFile).use { out ->
                                    overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                Log.d(TAG, "*** SAVED OVERLAY BITMAP: ${overlayFile.absolutePath} ***")
                            }
                            
                            // Save composite frame
                            val compositeFile = File(debugDir, "debug_composite_1.png")
                            FileOutputStream(compositeFile).use { out ->
                                compositeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            Log.d(TAG, "*** SAVED COMPOSITE FRAME: ${compositeFile.absolutePath} ***")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to save debug images: ${e.message}")
                        }
                    }
                }
            } else {
                Log.w(TAG, "Overlay bitmap is null or recycled - compositing video frame only")
            }
            
            compositeBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error compositing frame: ${e.message}", e)
            null
        }
    }
    
    /**
     * Verifies that a composite frame has content (for debugging)
     */
    fun verifyCompositeFrame(bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled) return false
        
        val width = bitmap.width
        val height = bitmap.height
        
        // Sample a few pixels to verify content
        val sampleSize = minOf(25, width * height)
        val stepX = maxOf(1, width / 5)
        val stepY = maxOf(1, height / 5)
        
        var sampleCount = 0
        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                if (sampleCount >= sampleSize) break
                
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel shr 24) and 0xFF
                if (alpha > 0) {
                    return true
                }
                sampleCount++
            }
        }
        
        return false
    }
}

