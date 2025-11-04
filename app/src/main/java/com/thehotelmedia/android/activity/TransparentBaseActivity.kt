package com.thehotelmedia.android.activity

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.theme.ThemeHelper

abstract class TransparentBaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarAppearance()
        ThemeHelper.applyTheme(this) // Apply before inflating UI
    }

//    private fun setStatusBarAppearance() {
//        // Set status bar color to black
//        window.statusBarColor = resources.getColor(R.color.black, theme)
//
//        // Set status bar icons to light mode (white)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
//        } else {
//            @Suppress("DEPRECATION")
//            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
//                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
//        }
//    }

    private fun setStatusBarAppearance() {
        // Make the status bar transparent
        window.statusBarColor = Color.TRANSPARENT

        // Optional: Set navigation bar color (you can change this if needed)
        window.navigationBarColor = resources.getColor(R.color.black, theme)

        // Extend content to system bars (replaces enableEdgeToEdge())
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Ensure status bar icons are white
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    }

    protected fun applyInsetsListener(viewId: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(viewId)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)

            insets
        }
    }
}
