package com.thehotelmedia.android.customClasses

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object UCropToolbarHelper {
    
    fun adjustToolbarPadding(activity: Activity) {
        // Use a handler to ensure the view hierarchy is fully inflated
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val rootView = activity.window.decorView.rootView as? ViewGroup
                rootView?.let { findAndAdjustToolbar(it) }
            } catch (e: Exception) {
                Log.e("UCropToolbarHelper", "Error adjusting toolbar: ${e.message}")
            }
        }, 100) // Small delay to ensure UCrop has finished inflating views
    }
    
    private fun findAndAdjustToolbar(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            
            if (child is Toolbar) {
                adjustToolbar(child)
                return
            } else if (child is ViewGroup) {
                findAndAdjustToolbar(child)
            }
        }
    }
    
    private fun adjustToolbar(toolbar: Toolbar) {
        // Add top padding to push toolbar down - using 40dp for reasonable spacing
        val topPadding = (-10 * toolbar.resources.displayMetrics.density).toInt()
        toolbar.setPadding(
            toolbar.paddingLeft,
            topPadding,
            toolbar.paddingRight,
            toolbar.paddingBottom
        )
        
        // Also adjust the buttons and title if found
        findAndAdjustButtons(toolbar)
        findAndAdjustTitle(toolbar)
        
        Log.d("UCropToolbarHelper", "Toolbar padding adjusted: top=$topPadding")
    }
    
    private fun findAndAdjustTitle(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            
            if (child is TextView && child.text.toString().contains("Edit Image", ignoreCase = true)) {
                // Move title up to align with buttons - using translationY for immediate effect
                val translationY = (-8 * child.resources.displayMetrics.density)
                child.translationY = translationY
                
                // Also try adjusting padding
                val currentPaddingTop = child.paddingTop
                val newPaddingTop = (currentPaddingTop - (8 * child.resources.displayMetrics.density)).toInt().coerceAtLeast(0)
                child.setPadding(
                    child.paddingLeft,
                    newPaddingTop,
                    child.paddingRight,
                    child.paddingBottom
                )
                
                // Try margin as well
                val layoutParams = child.layoutParams
                when (layoutParams) {
                    is ViewGroup.MarginLayoutParams -> {
                        layoutParams.topMargin = (-8 * child.resources.displayMetrics.density).toInt()
                        child.layoutParams = layoutParams
                    }
                    is android.widget.LinearLayout.LayoutParams -> {
                        layoutParams.topMargin = (-8 * child.resources.displayMetrics.density).toInt()
                        child.layoutParams = layoutParams
                    }
                }
                
                child.requestLayout() // Force layout update
                Log.d("UCropToolbarHelper", "Title adjusted: translationY=$translationY, paddingTop=$newPaddingTop")
                return
            } else if (child is ViewGroup) {
                findAndAdjustTitle(child)
            }
        }
    }
    
    private fun findAndAdjustButtons(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            
            if (child is ImageButton) {
                // Add margin to buttons - using 12dp for subtle spacing
                val layoutParams = child.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.let {
                    val topMargin = (-10 * child.resources.displayMetrics.density).toInt()
                    it.topMargin = topMargin
                    child.layoutParams = it
                    child.requestLayout() // Force layout update
                }
            } else if (child is ViewGroup) {
                findAndAdjustButtons(child)
            }
        }
    }
}

