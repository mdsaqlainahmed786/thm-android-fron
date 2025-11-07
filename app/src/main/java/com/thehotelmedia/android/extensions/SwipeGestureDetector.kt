package com.thehotelmedia.android.extensions

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

/**
 * Utility class to detect horizontal swipe gestures
 */
class SwipeGestureDetector(
    private val onSwipeLeft: (() -> Unit)? = null,  // Right to Left swipe
    private val onSwipeRight: (() -> Unit)? = null  // Left to Right swipe
) : GestureDetector.OnGestureListener {

    companion object {
        private const val SWIPE_THRESHOLD = 50  // Lower threshold for better responsiveness
        private const val SWIPE_VELOCITY_THRESHOLD = 50  // Lower velocity threshold
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {
        // No implementation needed
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        // No implementation needed
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null) return false

        val diffX = e2.x - e1.x
        val diffY = e2.y - e1.y

        // Check if horizontal movement is greater than vertical (horizontal swipe)
        if (Math.abs(diffX) > Math.abs(diffY)) {
            // Check if the swipe meets the threshold
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                when {
                    diffX > 0 -> {
                        // Swipe Right (Left to Right)
                        onSwipeRight?.invoke()
                        return true
                    }
                    else -> {
                        // Swipe Left (Right to Left)
                        onSwipeLeft?.invoke()
                        return true
                    }
                }
            }
        }
        return false
    }
}

/**
 * Extension function to attach swipe gesture detection to a view
 * This will intercept horizontal swipe gestures across the entire view, including child views like RecyclerView
 */
fun View.setOnSwipeListener(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
) {
    val gestureDetector = GestureDetector(context, SwipeGestureDetector(onSwipeLeft, onSwipeRight))
    var initialX = 0f
    var initialY = 0f
    var isHorizontalSwipe = false
    var hasIntercepted = false
    
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                isHorizontalSwipe = false
                hasIntercepted = false
                // Process down event for gesture detection
                gestureDetector.onTouchEvent(event)
                // Don't consume - allow RecyclerView to receive it for scrolling
                false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = Math.abs(event.x - initialX)
                val deltaY = Math.abs(event.y - initialY)
                
                // Detect horizontal swipe early (lower threshold for better responsiveness)
                if (!hasIntercepted && deltaX > 20 && deltaX > deltaY * 1.1) {
                    isHorizontalSwipe = true
                    hasIntercepted = true
                    // Prevent RecyclerView from scrolling when we detect horizontal swipe
                    if (v is androidx.recyclerview.widget.RecyclerView) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
                
                // Always process move events for gesture detection
                gestureDetector.onTouchEvent(event)
                
                // If we've intercepted a horizontal swipe, consume the event to prevent RecyclerView scrolling
                if (hasIntercepted && isHorizontalSwipe) {
                    true
                } else {
                    // Let RecyclerView handle vertical scrolling
                    false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Process the final event
                val result = gestureDetector.onTouchEvent(event)
                
                // Reset interception
                if (hasIntercepted) {
                    if (v is androidx.recyclerview.widget.RecyclerView) {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                
                isHorizontalSwipe = false
                hasIntercepted = false
                
                // Only consume if a swipe was detected
                result
            }
            else -> {
                gestureDetector.onTouchEvent(event)
                false
            }
        }
    }
}

