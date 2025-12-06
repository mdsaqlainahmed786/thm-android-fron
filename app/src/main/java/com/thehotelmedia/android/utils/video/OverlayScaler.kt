package com.thehotelmedia.android.utils.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log

/**
 * Utility class for scaling overlay bitmaps to match video resolution.
 * 
 * This class handles scaling while preserving:
 * - Alpha channel transparency
 * - Aspect ratio (if needed)
 * - Visual quality
 * 
 * Uses Canvas and Matrix for high-quality scaling that maintains alpha channel.
 */
object OverlayScaler {
    
    private const val TAG = "OverlayScaler"
    
    /**
     * Scales an overlay bitmap to match target video dimensions.
     * 
     * @param overlayBitmap The original overlay bitmap to scale
     * @param targetWidth Target video width
     * @param targetHeight Target video height
     * @param originalViewWidth Original view width where overlay was captured
     * @param originalViewHeight Original view height where overlay was captured
     * @return Scaled bitmap matching video dimensions, or null if scaling fails
     */
    fun scaleOverlayToVideo(
        overlayBitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        originalViewWidth: Int,
        originalViewHeight: Int
    ): Bitmap? {
        return try {
            if (overlayBitmap.isRecycled) {
                Log.e(TAG, "Overlay bitmap is recycled")
                return null
            }
            
            if (targetWidth <= 0 || targetHeight <= 0) {
                Log.e(TAG, "Invalid target dimensions: $targetWidth x $targetHeight")
                return null
            }
            
            if (originalViewWidth <= 0 || originalViewHeight <= 0) {
                Log.e(TAG, "Invalid original view dimensions: $originalViewWidth x $originalViewHeight")
                return null
            }
            
            Log.d(TAG, "Scaling overlay: ${overlayBitmap.width}x${overlayBitmap.height} -> $targetWidth x $targetHeight")
            Log.d(TAG, "Original view: $originalViewWidth x $originalViewHeight")
            
            // Calculate scale factors
            val scaleX = targetWidth.toFloat() / originalViewWidth.toFloat()
            val scaleY = targetHeight.toFloat() / originalViewHeight.toFloat()
            
            Log.d(TAG, "Scale factors: scaleX=$scaleX, scaleY=$scaleY")
            
            // Create target bitmap with alpha support
            val scaledBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            scaledBitmap.eraseColor(Color.TRANSPARENT)
            
            val canvas = Canvas(scaledBitmap)
            
            // Create scale matrix
            val scaleMatrix = Matrix().apply {
                setScale(scaleX, scaleY)
            }
            
            // Use Paint with proper alpha blending to preserve transparency
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                // SRC_OVER mode ensures proper alpha blending
                // This composites the source (overlay) over the destination (transparent background)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            
            // Draw scaled overlay
            canvas.drawBitmap(overlayBitmap, scaleMatrix, paint)
            
            // Verify scaled bitmap has content
            if (!verifyScaledBitmapContent(scaledBitmap)) {
                Log.w(TAG, "Scaled bitmap appears empty after scaling")
                scaledBitmap.recycle()
                return null
            }
            
            Log.d(TAG, "Scaled overlay created: ${scaledBitmap.width}x${scaledBitmap.height}, " +
                    "config=${scaledBitmap.config}, hasAlpha=${scaledBitmap.hasAlpha()}")
            
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error scaling overlay: ${e.message}", e)
            null
        }
    }
    
    /**
     * Verifies the scaled bitmap has non-transparent content
     */
    private fun verifyScaledBitmapContent(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        
        // Sample pixels to check for content
        val sampleSize = minOf(100, width * height)
        val stepX = maxOf(1, width / 10)
        val stepY = maxOf(1, height / 10)
        
        var pixelCount = 0
        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                if (pixelCount >= sampleSize) break
                
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel shr 24) and 0xFF
                if (alpha > 0) {
                    return true
                }
                pixelCount++
            }
        }
        
        // If sampling didn't find content, do a more thorough check
        val thoroughPixels = IntArray(minOf(1000, width * height))
        val checkWidth = minOf(100, width)
        val checkHeight = minOf(100, height)
        bitmap.getPixels(thoroughPixels, 0, checkWidth, 0, 0, checkWidth, checkHeight)
        
        for (pixel in thoroughPixels) {
            if ((pixel shr 24) and 0xFF > 0) {
                return true
            }
        }
        
        return false
    }
}

