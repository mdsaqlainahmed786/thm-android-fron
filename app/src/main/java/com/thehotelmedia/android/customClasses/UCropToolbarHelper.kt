package com.thehotelmedia.android.customClasses

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
        val topPadding = (0.1 * toolbar.resources.displayMetrics.density).toInt()
        toolbar.setPadding(
            toolbar.paddingLeft,
            topPadding,
            toolbar.paddingRight,
            toolbar.paddingBottom
        )
        
        // Also adjust the buttons if found
        findAndAdjustButtons(toolbar)
        
        Log.d("UCropToolbarHelper", "Toolbar padding adjusted: top=$topPadding")
    }
    
    private fun findAndAdjustButtons(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            
            if (child is ImageButton) {
                // Add margin to buttons - using 12dp for subtle spacing
                val layoutParams = child.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.let {
                    val topMargin = (0.1 * child.resources.displayMetrics.density).toInt()
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

