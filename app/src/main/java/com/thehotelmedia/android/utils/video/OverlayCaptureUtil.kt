package com.thehotelmedia.android.utils.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.thehotelmedia.android.R

/**
 * Utility class for capturing overlay UI elements (text, emojis, stickers) into a bitmap.
 * 
 * This class captures all overlay views from a ViewGroup (like PhotoEditorView) and renders
 * them into a single transparent ARGB_8888 bitmap, preserving their exact positions and
 * visual appearance.
 * 
 * Key features:
 * - Captures text views, emoji views, and other overlay elements
 * - Preserves exact positions using getHitRect()
 * - Handles hardware acceleration by temporarily disabling it during capture
 * - Ensures proper layout and measurement before drawing
 * - Verifies content by sampling pixels
 */
object OverlayCaptureUtil {
    
    private const val TAG = "OverlayCaptureUtil"
    
    /**
     * Data class to store information about captured overlays
     */
    data class OverlayInfo(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
        val type: String // "TEXT" or "EMOJI"
    )
    
    /**
     * Captures all overlay views from the given parent view into a bitmap.
     * 
     * @param parentView The ViewGroup containing overlay views (e.g., PhotoEditorView)
     * @param sourceView The source view to exclude from capture (e.g., the video/image view)
     * @return Pair of (overlay bitmap, list of overlay info). Bitmap is null if capture fails.
     */
    fun captureOverlays(
        parentView: ViewGroup,
        sourceView: View
    ): Pair<Bitmap?, List<OverlayInfo>> {
        return try {
            val width = parentView.width
            val height = parentView.height
            
            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Invalid view dimensions: $width x $height")
                return Pair(null, emptyList())
            }
            
            // Create transparent bitmap to capture overlays
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(bitmap)
            
            val overlayInfos = mutableListOf<OverlayInfo>()
            val bounds = android.graphics.Rect()
            var overlayCount = 0
            
            // Iterate through all children to find overlay views
            val childCount = parentView.childCount
            for (i in 0 until childCount) {
                val child = parentView.getChildAt(i) ?: continue
                
                // Skip the source view - only capture overlay views
                if (child == sourceView || child.visibility != View.VISIBLE) {
                    continue
                }
                
                // Check if this is a text overlay
                val textView = child.findViewById<TextView>(R.id.tvPhotoEditorText)
                val isTextOverlay = textView != null
                
                // Check if this is an emoji overlay
                val isEmojiOverlay = detectEmojiOverlay(child)
                
                if (isTextOverlay || isEmojiOverlay) {
                    overlayCount++
                    
                    // Get the view's actual bounds in parent coordinates
                    // This accounts for both layout position and translation
                    child.getHitRect(bounds)
                    
                    val actualLeft = bounds.left
                    val actualTop = bounds.top
                    val childWidth = bounds.width().coerceAtLeast(1)
                    val childHeight = bounds.height().coerceAtLeast(1)
                    
                    // Store overlay info for later use
                    overlayInfos.add(OverlayInfo(
                        left = actualLeft,
                        top = actualTop,
                        width = childWidth,
                        height = childHeight,
                        type = if (isTextOverlay) "TEXT" else "EMOJI"
                    ))
                    
                    // Prepare text overlay for capture
                    if (isTextOverlay && child is ViewGroup) {
                        prepareTextViewForCapture(child, textView)
                    }
                    
                    // Ensure proper layout before drawing
                    ensureViewLayout(child, childWidth, childHeight, textView)
                    
                    // Draw the overlay view to canvas
                    drawOverlayToCanvas(canvas, child, actualLeft, actualTop, isTextOverlay, textView)
                    
                    // Verify content was drawn
                    verifyOverlayDrawn(bitmap, actualLeft, actualTop, childWidth, childHeight, isTextOverlay)
                }
            }
            
            Log.d(TAG, "Captured $overlayCount overlay(s) from $childCount children")
            
            // Verify bitmap has content
            val hasContent = verifyBitmapContent(bitmap)
            if (overlayCount > 0 && !hasContent) {
                Log.w(TAG, "Overlay bitmap appears empty despite $overlayCount overlays")
            }
            
            Pair(bitmap, overlayInfos)
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing overlay bitmap: ${e.message}", e)
            Pair(null, emptyList())
        }
    }
    
    /**
     * Detects if a view is an emoji overlay
     */
    private fun detectEmojiOverlay(view: View): Boolean {
        return when {
            // Direct ImageView with drawable
            view is ImageView && view.drawable != null -> true
            // ViewGroup containing ImageView children
            view is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    if (child is ImageView && child.drawable != null) {
                        return true
                    }
                }
                false
            }
            else -> false
        }
    }
    
    /**
     * Prepares text view for capture by ensuring visibility and hiding edit icons
     */
    private fun prepareTextViewForCapture(parent: ViewGroup, textView: TextView?) {
        // Hide edit icon
        val editIcon = parent.findViewById<ImageView>(R.id.imgPhotoEditorEdit)
        editIcon?.visibility = View.GONE
        
        // Ensure TextView is visible and properly configured
        textView?.let {
            it.visibility = View.VISIBLE
            it.alpha = 1f
            // Fallback to white if text color is transparent
            if (it.currentTextColor == Color.TRANSPARENT) {
                it.setTextColor(Color.WHITE)
            }
        }
        
        // Ensure all children are visible (except edit icon)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child != editIcon) {
                child.visibility = View.VISIBLE
                child.alpha = 1f
            }
        }
    }
    
    /**
     * Ensures view is properly measured and laid out before drawing
     */
    private fun ensureViewLayout(
        view: View,
        width: Int,
        height: Int,
        textView: TextView?
    ) {
        when (view) {
            is ViewGroup -> {
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                view.layout(0, 0, width, height)
                
                // Also layout TextView if present
                textView?.let {
                    val textWidth = it.width.takeIf { w -> w > 0 } ?: width
                    val textHeight = it.height.takeIf { h -> h > 0 } ?: height
                    it.measure(
                        View.MeasureSpec.makeMeasureSpec(textWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(textHeight, View.MeasureSpec.EXACTLY)
                    )
                    it.layout(0, 0, textWidth, textHeight)
                    it.invalidate()
                }
            }
            is ImageView -> {
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                view.layout(0, 0, width, height)
            }
        }
    }
    
    /**
     * Draws overlay view to canvas with proper handling of hardware acceleration
     */
    private fun drawOverlayToCanvas(
        canvas: Canvas,
        view: View,
        left: Int,
        top: Int,
        isTextOverlay: Boolean,
        textView: TextView?
    ) {
        canvas.save()
        canvas.translate(left.toFloat(), top.toFloat())
        
        // Temporarily disable hardware acceleration for proper rendering
        val wasHardwareAccelerated = view.isHardwareAccelerated
        val textViewWasHardware = if (isTextOverlay && textView != null) {
            textView.isHardwareAccelerated
        } else {
            false
        }
        
        if (wasHardwareAccelerated) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        if (textViewWasHardware && textView != null) {
            textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        
        // Draw the view (ViewGroup.draw() will draw all children including TextView)
        view.draw(canvas)
        
        // Restore hardware acceleration
        if (wasHardwareAccelerated) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        if (textViewWasHardware && textView != null) {
            textView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        
        canvas.restore()
    }
    
    /**
     * Verifies that overlay content was actually drawn to the bitmap
     */
    private fun verifyOverlayDrawn(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        isTextOverlay: Boolean
    ) {
        val centerX = (left + width / 2).coerceIn(0, bitmap.width - 1)
        val centerY = (top + height / 2).coerceIn(0, bitmap.height - 1)
        val pixel = bitmap.getPixel(centerX, centerY)
        val alpha = (pixel shr 24) and 0xFF
        
        val overlayType = if (isTextOverlay) "TEXT" else "EMOJI"
        if (alpha > 0) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            Log.d(TAG, "$overlayType overlay drawn - center pixel ($centerX, $centerY): ARGB($alpha, $r, $g, $b)")
        } else {
            Log.w(TAG, "$overlayType overlay drawn but center pixel is transparent")
        }
    }
    
    /**
     * Verifies the bitmap has non-transparent content
     */
    private fun verifyBitmapContent(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (pixel in pixels) {
            if ((pixel shr 24) and 0xFF != 0) {
                return true
            }
        }
        return false
    }
}

