package com.thehotelmedia.android.activity

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.theme.ThemeHelper
import com.thehotelmedia.android.extensions.isInternetAvailable

abstract class BaseActivity : AppCompatActivity() {

    private var noInternetDialog: Dialog? = null

    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            // Internet is available
            noInternetDialog?.dismiss()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            // Internet is lost
            showNoInternetDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarAppearance()

        ThemeHelper.applyTheme(this) // Apply before inflating UI

//
//        // Prevent screenshots and screen recording
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE
//        )


        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)


        // Check for network connectivity
        checkNetworkConnectivity()


    }

    private fun checkNetworkConnectivity() {
        val isConnected = this.isInternetAvailable()

        if (isConnected) {
            noInternetDialog?.dismiss()
        } else {
            showNoInternetDialog()
        }
    }

    private fun setStatusBarAppearance() {
        // Set status bar color to black
        window.statusBarColor = resources.getColor(R.color.background_color, theme)

        val isDarkTheme = ThemeHelper.isDarkModeEnabled(this)
        var lightStatusIcon = false
        if (isDarkTheme) {
            lightStatusIcon = false
        }else{
            lightStatusIcon = true
        }

        // Set status bar icons to light mode (white)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = lightStatusIcon
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

    protected fun applyInsetsListener(viewId: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(viewId)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun showNoInternetDialog() {
        // Inflate the no internet layout
        noInternetDialog = Dialog(this)
        noInternetDialog?.let { dlg ->
            dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dlg.setContentView(R.layout.no_internet_layout) // Use your layout here
            dlg.setCancelable(false)
            dlg.setCanceledOnTouchOutside(false)
            val layoutParams = dlg.window?.attributes
            layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams?.height = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams?.gravity = Gravity.CENTER
            dlg.window?.attributes = layoutParams


            val btnRetry = dlg.findViewById<View>(R.id.btnRetry)
            btnRetry.setOnClickListener {
                if (isInternetAvailable()) {
                    noInternetDialog?.dismiss()
                } else {
                    // Optionally show a message indicating no connection
                }
            }


        }
        noInternetDialog?.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

}
