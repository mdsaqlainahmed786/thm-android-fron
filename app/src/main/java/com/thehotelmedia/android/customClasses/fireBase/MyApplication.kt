package com.thehotelmedia.android.customClasses.fireBase

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.thehotelmedia.android.customClasses.UCropToolbarHelper

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        // Register activity lifecycle callbacks to adjust UCrop toolbar
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Check if this is UCropActivity
                if (activity.javaClass.name == "com.yalantis.ucrop.UCropActivity") {
                    // Adjust toolbar padding after a short delay
                    UCropToolbarHelper.adjustToolbarPadding(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                // Also adjust on resume in case toolbar is recreated
                if (activity.javaClass.name == "com.yalantis.ucrop.UCropActivity") {
                    UCropToolbarHelper.adjustToolbarPadding(activity)
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}