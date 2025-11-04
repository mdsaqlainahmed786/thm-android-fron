package com.thehotelmedia.android.apiService

import android.content.Context
import android.content.Intent
import android.util.Log
import com.thehotelmedia.android.activity.authentication.SignInActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.extensions.getAndroidDeviceId
import com.thehotelmedia.android.repository.AuthRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class AuthInterceptor(private val context: Context) : Interceptor {

    private val tag = "AUTH_INTERCEPTOR"
    private val preferenceManager = PreferenceManager.getInstance(context)
    private val authRepo = AuthRepo(context)
    @Volatile private var isTokenRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = getAccessToken()
        val request = chain.request()

        return if (apiKey.isNotEmpty()) {
            val newRequest = request.newBuilder()
                .header("x-access-token", apiKey)
                .build()

            var response: Response? = null
            try {
                response = chain.proceed(newRequest)
                // Check for unauthorized or forbidden status codes
                if (response.code == 401 || response.code == 403) {

                    response.close() // Close the previous response before retrying
                    response = handleUnauthorized(chain, request)
                }
            } catch (e: Exception) {
                response?.close() // Ensure the response is closed in case of an exception
                throw e
            }
            response ?: throw IOException("Response is null")
        } else {
            createUnauthorizedResponse(request)
        }
    }

    private fun handleUnauthorized(chain: Interceptor.Chain, originalRequest: Request): Response {
        return synchronized(this) {
            if (!isTokenRefreshing) {
                isTokenRefreshing = true
                try {
                    // Refresh token and retry the request
                    val newApiKey = refreshTokenAndGetNewAccessToken()
                    Log.d(tag, "newApiKey, $newApiKey")

                    if (newApiKey.isNotEmpty()) {
                        preferenceManager.putString(PreferenceManager.Keys.ACCESS_TOKEN, newApiKey)
                        Log.d(tag, "Token refreshed, retrying request...")

                        val newRequest = originalRequest.newBuilder()
                            .header("x-access-token", newApiKey)
                            .build()
                        return chain.proceed(newRequest)
                    } else {
                        Log.d(tag, "Token refresh failed or returned empty")
                        createUnauthorizedResponse(originalRequest)
                    }
                } catch (e: IOException) {
                    Log.e(tag, "IOException during token refresh: ${e.message}")
                    createUnauthorizedResponse(originalRequest)
                } finally {
                    isTokenRefreshing = false
                }
            } else {
                createUnauthorizedResponse(originalRequest)
            }
        }
    }

    private fun createUnauthorizedResponse(request: Request): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody(null))
            .build()
    }

    private fun getAccessToken(): String {
        return preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN, "") ?: ""
    }

    private fun refreshTokenAndGetNewAccessToken(): String {
        Log.d(tag, "Refreshing access token...")
        var newApiKey = ""
        runBlocking {
            try {
                val response = withContext(Dispatchers.IO) {
                    authRepo.refreshToken()
                }
                if (response.isSuccessful) {
                    response.body()?.data?.let { data ->
                        newApiKey = data.accessToken ?: ""
                        val newRefreshToken = data.refreshToken ?: ""
                        val deviceId = context.getAndroidDeviceId()

                        // Update preferences with new tokens
                        preferenceManager.putString(PreferenceManager.Keys.ACCESS_TOKEN, newApiKey)
                        preferenceManager.putString(PreferenceManager.Keys.REFRESH_TOKEN, newRefreshToken)
                        val cookies = "SessionToken=$newRefreshToken; UserDeviceID=$deviceId; X-Access-Token=$newApiKey"
                        preferenceManager.putString(PreferenceManager.Keys.COOKIES, cookies)

                        Log.d(tag, "Refreshed access token: $newApiKey")
                        Log.d(tag, "Refreshed refresh token: $newRefreshToken")
                    } ?: run {
                        Log.d(tag, "Token refresh failed: empty response body")
                        logOut()
                    }
                } else {
                    Log.d(tag, "Token refresh failed: ${response.code()} - ${response.message()}")
                    logOut()
                }
            } catch (e: Exception) {
                Log.e(tag, "Token refresh failed with exception: ${e.message}")
                logOut()
            }
        }
        return newApiKey
    }

    private fun logOut() {
        // Hide progress bar safely
        val progressBar = CustomProgressBar(context)
        progressBar.hide()


        preferenceManager.clearPreferences()
        val intent = Intent(context, SignInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }
}
