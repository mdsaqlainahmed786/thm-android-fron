package com.thehotelmedia.android.customClasses.fireBase

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {


    override fun onCreate() {
        super.onCreate().toString()
        FirebaseApp.initializeApp(this)
    }
}