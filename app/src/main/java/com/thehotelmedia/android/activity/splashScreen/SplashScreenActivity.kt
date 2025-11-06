package com.thehotelmedia.android.activity.splashScreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.TransparentBaseActivity
import com.thehotelmedia.android.activity.authentication.SignInActivity
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.LanguageActivity
import com.thehotelmedia.android.customClasses.Constants.LANGUAGE_CODE
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.theme.ThemeHelper
import com.thehotelmedia.android.databinding.ActivitySplashScreenBinding
import com.thehotelmedia.android.extensions.AppUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : TransparentBaseActivity() {
    private lateinit var appUpdateHelper: AppUpdateHelper

    private lateinit var binding: ActivitySplashScreenBinding
    private val activity = this
    companion object {
        private const val SPLASH_SCREEN_TIME_OUT = 2000
    }

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Small delay to ensure theme is fully applied and SharedPreferences are ready
        // This is especially important when restarting after theme change
        Handler().postDelayed({
            initUI()
            finish()
        }, SPLASH_SCREEN_TIME_OUT.toLong())

    }

    private fun initUI() {
        checkForAppUpdate()
        sessionMaintain()
    }

    override fun onResume() {
        super.onResume()
        appUpdateHelper = AppUpdateHelper(this, this)
        // Check if an update needs to be resumed
        appUpdateHelper.resumeUpdateIfNeeded()
    }

    private fun checkForAppUpdate() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                appUpdateHelper.checkForUpdate()
            } catch (e: Exception) {
                // Handle potential errors, e.g., show a Toast or log the error
                Log.wtf("SPLASH_SCREEN", "Error during update check", e)
            }
        }
    }

    private fun sessionMaintain() {
        val preferenceManager = PreferenceManager.getInstance(applicationContext)
        
        // Verify we're using the same SharedPreferences instance
        val sharedPrefs = applicationContext.getSharedPreferences("YOWS_USER_PREF", Context.MODE_PRIVATE)
        Log.d("SESSION_DEBUG", "SharedPreferences file: YOWS_USER_PREF, contains ACCESS_TOKEN: ${sharedPrefs.contains("ACCESS_TOKEN")}, contains BUSINESS_TYPE: ${sharedPrefs.contains("BUSINESS_TYPE")}")

        // Retry logic to handle potential timing issues with SharedPreferences
        // Only retry if token is empty (user should be logged in)
        var token = ""
        var businessType = ""
        var retryCount = 0
        val maxRetries = 5 // Increased retries
        
        // Try to read token with retry logic (only retry if token is empty)
        while (retryCount < maxRetries && token.isEmpty()) {
            if (retryCount > 0) {
                // Small delay before retry to allow SharedPreferences to commit
                try {
                    Thread.sleep(100) // Increased delay
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                Log.d("SESSION_DEBUG", "Retrying token read, attempt ${retryCount + 1}")
            }
            
            // Get token with proper null handling - try both PreferenceManager and direct SharedPreferences
            token = preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN, "")?.takeIf { it.isNotEmpty() } ?: ""
            
            // If still empty, try reading directly from SharedPreferences
            if (token.isEmpty()) {
                val directToken = sharedPrefs.getString("ACCESS_TOKEN", "")?.takeIf { it.isNotEmpty() } ?: ""
                if (directToken.isNotEmpty()) {
                    Log.d("SESSION_DEBUG", "Token found via direct SharedPreferences read")
                    token = directToken
                }
            }
            
            retryCount++
        }
        
        // Read business type with retry logic
        businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "")?.trim() ?: ""
        if (businessType.isEmpty()) {
            // Try reading directly from SharedPreferences
            businessType = sharedPrefs.getString("BUSINESS_TYPE", "")?.trim() ?: ""
        }
        
        val businessAccIsApproved = preferenceManager.getBoolean(PreferenceManager.Keys.BUSINESS_ACC_APPROVED, false)
        
        // CRITICAL DEBUG LOGGING - Log exact values before navigation
        Log.d("SESSION_DEBUG", "=== SESSION CHECK RESULTS ===")
        Log.d("SESSION_DEBUG", "Token: ${if (token.isNotEmpty()) "${token.take(20)}... (length: ${token.length})" else "EMPTY"}")
        Log.d("SESSION_DEBUG", "BusinessType: '$businessType' (length: ${businessType.length})")
        Log.d("SESSION_DEBUG", "BusinessAccApproved: $businessAccIsApproved")
        Log.d("SESSION_DEBUG", "Retries needed: $retryCount")
        Log.d("SESSION_DEBUG", "Token isEmpty check: ${token.isEmpty()}")
        Log.d("SESSION_DEBUG", "Token isNotEmpty check: ${token.isNotEmpty()}")
        
        // Verify critical data exists
        if (token.isEmpty()) {
            Log.e("SESSION_DEBUG", "CRITICAL: Token is empty after $maxRetries retries! This should not happen if user is logged in.")
            // Check if key exists but value is empty
            val tokenExists = sharedPrefs.contains("ACCESS_TOKEN")
            val tokenValue = sharedPrefs.getString("ACCESS_TOKEN", null)
            Log.e("SESSION_DEBUG", "Key exists: $tokenExists, Value: ${if (tokenValue != null) "exists (length: ${tokenValue.length})" else "null"}")
        }
        
        val onBoarding = getSharedPreferences("ONBOARD", MODE_PRIVATE)
        val languageCode = onBoarding.getString(LANGUAGE_CODE, "en").orEmpty()
        setNormalLocale(languageCode)
        
        // Check if user is logged in FIRST
        // IMPORTANT: Check for null/empty explicitly to avoid defaulting to login incorrectly
        val hasValidToken = token.isNotEmpty() && token.isNotBlank()
        
        Log.d("SESSION_DEBUG", "=== NAVIGATION DECISION ===")
        Log.d("SESSION_DEBUG", "hasValidToken: $hasValidToken")
        Log.d("SESSION_DEBUG", "token.isEmpty(): ${token.isEmpty()}")
        Log.d("SESSION_DEBUG", "token.isBlank(): ${token.isBlank()}")
        
        // If user is logged in, skip first-time check and navigate directly
        if (hasValidToken) {
            // Normalize business type for comparison (trim and compare case-insensitively)
            val normalizedBusinessType = businessType.lowercase().trim()
            val normalizedIndividualType = business_type_individual.lowercase().trim()
            
            Log.d("SESSION_DEBUG", "Normalized BusinessType: '$normalizedBusinessType'")
            Log.d("SESSION_DEBUG", "Expected Individual Type: '$normalizedIndividualType'")
            Log.d("SESSION_DEBUG", "Types match: ${normalizedBusinessType == normalizedIndividualType}")
            Log.d("SESSION_DEBUG", "BusinessType isEmpty: ${businessType.isEmpty()}")
            
            if (normalizedBusinessType == normalizedIndividualType) {
                // Individual account - navigate directly to main activity
                Log.d("SESSION_DEBUG", "✓ DECISION: Navigating to IndividualMainActivity")
                val intent = Intent(activity, BottomNavigationIndividualMainActivity::class.java)
                startActivity(intent)
                finish()
            } else if (businessType.isNotEmpty()) {
                // Business account - check approval status
                Log.d("SESSION_DEBUG", "Business account detected, checking approval...")
                if (!businessAccIsApproved) {
                    Log.w("SESSION_DEBUG", "✗ DECISION: Business account not approved, redirecting to SignIn")
                    val intent = Intent(activity, SignInActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.d("SESSION_DEBUG", "✓ DECISION: Navigating to BusinessMainActivity")
                    val intent = Intent(activity, BottomNavigationBusinessMainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                // Business type is empty but token exists - retry reading it one more time
                Log.w("SESSION_DEBUG", "Token exists but business type is empty, attempting one more read...")
                // Wait a bit more before retry
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                
                val retryBusinessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "")?.trim() ?: ""
                Log.d("SESSION_DEBUG", "Retry BusinessType: '$retryBusinessType'")
                
                if (retryBusinessType.isNotEmpty()) {
                    val normalizedRetryType = retryBusinessType.lowercase().trim()
                    if (normalizedRetryType == normalizedIndividualType) {
                        Log.d("SESSION_DEBUG", "✓ DECISION: Retry successful, navigating to IndividualMainActivity")
                        val intent = Intent(activity, BottomNavigationIndividualMainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.w("SESSION_DEBUG", "✗ DECISION: Retry found business type but not individual, redirecting to SignIn")
                        val intent = Intent(activity, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Log.e("SESSION_DEBUG", "✗ DECISION: Business type still empty after retry, redirecting to SignIn")
                    Log.e("SESSION_DEBUG", "This is unusual - token exists but business type is missing")
                    val intent = Intent(activity, SignInActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        } else {
            // User is not logged in - check if this is first time
            val isFirstTime = onBoarding.getBoolean("FIRST_TIME", true)
            Log.d("SESSION_DEBUG", "✗ DECISION: No valid token found after $maxRetries retries")
            Log.d("SESSION_DEBUG", "Token value was: '${if (token.isEmpty()) "EMPTY" else "BLANK"}'")
            Log.d("SESSION_DEBUG", "User not logged in, isFirstTime: $isFirstTime")
            
            if (isFirstTime) {
                Log.d("SESSION_DEBUG", "First time user, navigating to LanguageActivity")
                val intent = Intent(activity, LanguageActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Log.d("SESSION_DEBUG", "Not first time and no token, navigating to SignInActivity")
                val intent = Intent(activity, SignInActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }


    private fun setNormalLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)

        println("afkdska   languageCode  $languageCode")
    }





}