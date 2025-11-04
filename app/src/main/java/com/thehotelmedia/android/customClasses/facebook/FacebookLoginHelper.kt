package com.thehotelmedia.android.customClasses.facebook

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

class FacebookLoginHelper(
    private val activity: ComponentActivity,
    private val callback: (String?) -> Unit
) {
    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    fun getCallbackManager(): CallbackManager = callbackManager

    fun login() {
        LoginManager.getInstance().logInWithReadPermissions(
            activity, listOf("email", "public_profile")
        )

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                val token = loginResult.accessToken.token
                Log.d("FB_LOGIN", "Success: $token")

                if (token.isNullOrEmpty()) {
                    Log.e("FB_LOGIN", "Token is null or empty!")
                    callback(null)
                } else {
                    callback(token)
                }

                // Logout after fetching token
                LoginManager.getInstance().logOut()
                Log.d("FB_LOGIN", "User Logged Out After Token Fetch")
            }

            override fun onCancel() {
                Log.d("FB_LOGIN", "Login Cancelled")
                callback(null)
            }

            override fun onError(exception: FacebookException) {
                Log.e("FB_LOGIN", "Error: ${exception.message}")
                callback(null)
            }
        })
    }
}
