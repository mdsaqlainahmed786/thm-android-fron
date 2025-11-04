package com.thehotelmedia.android.customClasses.fireBase

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.firebase.FirebaseApp
import com.thehotelmedia.android.BuildConfig
import java.util.Locale

class MyApplication : Application() {


    override fun onCreate() {
        super.onCreate().toString()
        FirebaseApp.initializeApp(this)

        // Initialize Facebook SDK
        FacebookSdk.setClientToken(BuildConfig.FACEBOOK_CLIENT_SECRETE)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

//        setupActivityListener()
    }


    private fun setupActivityListener() {
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })
    }


}