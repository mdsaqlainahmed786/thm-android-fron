package com.thehotelmedia.android.activity.splashScreen

import android.annotation.SuppressLint
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

        val token = preferenceManager.getString(PreferenceManager.Keys.ACCESS_TOKEN, "").toString()
        val businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        println("safakhsdhkfhasjk   $businessType")
        val businessAccIsApproved = preferenceManager.getBoolean(PreferenceManager.Keys.BUSINESS_ACC_APPROVED, false)
        val onBoarding = getSharedPreferences("ONBOARD", MODE_PRIVATE)

        val languageCode = onBoarding.getString(LANGUAGE_CODE, "en").orEmpty()
        setNormalLocale(languageCode)
        val isFirstTime = onBoarding.getBoolean("FIRST_TIME", true)
        if (isFirstTime) {
            val intent = Intent(activity, LanguageActivity::class.java)
            startActivity(intent)
            finish()
//            val intent = Intent(activity, OnBoardingActivity::class.java)
//            startActivity(intent)
//            finish()
        } else {
            if (token.isNotEmpty()) {
                if (businessType == business_type_individual){
                    val intent = Intent(activity, BottomNavigationIndividualMainActivity::class.java)
                    startActivity(intent)
                    finish()
                }else{
                    if(!businessAccIsApproved){
                        val intent = Intent(activity, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                    }else{
                        val intent = Intent(activity, BottomNavigationBusinessMainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
//                    val intent = Intent(activity, BottomNavigationBusinessMainActivity::class.java)
//                    startActivity(intent)
//                    finish()
                }
//                val intent = Intent(activity, SignInActivity::class.java)
//                startActivity(intent)
//                finish()
            }else {
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