package com.thehotelmedia.android.activity.userTypes.individual

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.AboutUsActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.BlockedUserActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.DocumentsActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.HelpAndSupportActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.LanguageActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.SavedPostActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.SubscriptionActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.TransactionsActivity
import com.thehotelmedia.android.bottomSheets.AccountDeactivateBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.DeleteAccountBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.LogoutBottomSheetFragment
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityIndividualSeetingsBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.activity.JobDetailActivity
import com.thehotelmedia.android.activity.splashScreen.SplashScreenActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.BookingHistoryActivity
import com.thehotelmedia.android.activity.userTypes.individual.settingsScreen.PostJobActivity
import com.thehotelmedia.android.customClasses.theme.ThemeHelper

class IndividualSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityIndividualSeetingsBinding
    private lateinit var preferenceManager : PreferenceManager
    private var type = ""
    private var userName = ""
    private var isPrivateAcc = false
    private var isNotificationEnabled = false
    private var isDarkTheme = false
    private lateinit var individualViewModal: IndividualViewModal
    private val activity = this@IndividualSettingsActivity
    private val socketViewModel: SocketViewModel by viewModels()

    private fun areNotificationsEnabled(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Default behavior for pre-Oreo devices
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {

        ThemeHelper.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityIndividualSeetingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

//    override fun onResume() {
//        super.onResume()
//        socketViewModel.connectSocket(userName)
//
//        isNotificationEnabled = areNotificationsEnabled()
//        binding.notificationSwitch.isChecked = isNotificationEnabled
//        if (isNotificationEnabled) {
//            binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
//            binding.notificationSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
//        } else {
//            binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
//            binding.notificationSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
//        }
//        updateStatus()
//    }

    private fun initUI() {

        preferenceManager = PreferenceManager.getInstance(this)
        type = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        isPrivateAcc = preferenceManager.getBoolean(PreferenceManager.Keys.IS_PRIVATE_ACCOUNT, false)
        userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
//        isNotificationEnabled = preferenceManager.getBoolean(PreferenceManager.Keys.IS_NOTIFICATION_ENABLED, false)
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]

        checkSwitches()

        if (type == "Individual"){
            binding.documentsLayout.visibility = View.GONE
            binding.postJobLayout.visibility = View.GONE
            binding.bookingHistoryLayout.visibility = View.VISIBLE
            binding.privateAccBtn.visibility = View.VISIBLE
            binding.privateAccView.visibility = View.VISIBLE
        }else{
            binding.documentsLayout.visibility = View.VISIBLE
            binding.postJobLayout.visibility = View.VISIBLE
            binding.bookingHistoryLayout.visibility = View.GONE
            binding.privateAccBtn.visibility = View.GONE
            binding.privateAccView.visibility = View.GONE
        }

//        if (!BuildConfig.DEBUG){
//            binding.bookingHistoryLayout.visibility = View.GONE
//            binding.postJobLayout.visibility = View.GONE
//        }

        setPrivateSwitch()
        setNotificationSwitch()
        setThemeSwitch()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.bookingHistoryBtn.setOnClickListener {
            val intent = Intent(this, BookingHistoryActivity::class.java)
            startActivity(intent)
        }

        binding.transactionsBtn.setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            startActivity(intent)
        }
        binding.documentsBtn.setOnClickListener {
            val intent = Intent(this, DocumentsActivity::class.java)
            startActivity(intent)
        }
        binding.postJobBtn.setOnClickListener {
            val intent = Intent(this, PostJobActivity::class.java)
            startActivity(intent)
        }
        binding.languageBtn.setOnClickListener {
            val intent = Intent(this, LanguageActivity::class.java)
            intent.putExtra("FROM", "Settings")
            startActivity(intent)
        }
        binding.privacyPolicyBtn.setOnClickListener {
//            val intent = Intent(this, PrivacyPolicyActivity::class.java)
//            startActivity(intent)
            openAppOrBrowser(this,"${BuildConfig.ADMIN_DOMAIN}/privacy-policy")
        }
        binding.savedPostBtn.setOnClickListener {
            val intent = Intent(this, SavedPostActivity::class.java)
            startActivity(intent)
        }
        binding.blockUserBtn.setOnClickListener {
            val intent = Intent(this, BlockedUserActivity::class.java)
            startActivity(intent)
        }
        binding.aboutUsBtn.setOnClickListener {
//            val intent = Intent(this, AboutUsActivity::class.java)
//            startActivity(intent)
            openAppOrBrowser(this,"${BuildConfig.ADMIN_DOMAIN}/about-us")

        }
        binding.termsConditionsBtn.setOnClickListener {
//            val intent = Intent(this, TermsConditionsActivity::class.java)
//            startActivity(intent)
            openAppOrBrowser(this,"${BuildConfig.ADMIN_DOMAIN}/terms-and-conditions")
        }

        binding.subscriptionBtn.setOnClickListener {
            val intent = Intent(this, SubscriptionActivity::class.java)
            startActivity(intent)
        }
        binding.helpSupportBtn.setOnClickListener {
            val intent = Intent(this, HelpAndSupportActivity::class.java)
            startActivity(intent)
        }

        binding.accountDeactivateBtn.setOnClickListener {
            AccountDeactivateBottomSheetFragment().show(this.supportFragmentManager, AccountDeactivateBottomSheetFragment().tag)
        }
        binding.deleteAccountBtn.setOnClickListener {
            DeleteAccountBottomSheetFragment().show(this.supportFragmentManager, DeleteAccountBottomSheetFragment().tag)
        }
        binding.logOutBtn.setOnClickListener {
            LogoutBottomSheetFragment().show(this.supportFragmentManager, LogoutBottomSheetFragment().tag)
        }


    }


    private fun openAppOrBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage("com.android.chrome")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If the app is not installed, open the URL in the browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }


    private fun setPrivateSwitch() {
        binding.privateAccSwitch.setOnCheckedChangeListener { _, isChecked ->
            isPrivateAcc = isChecked
            if (isChecked) {
                binding.privateAccSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
                binding.privateAccSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            } else {
                binding.privateAccSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                binding.privateAccSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
            }
            updateStatus()
        }
    }

    private fun setNotificationSwitch() {
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
//            openNotificationSettings()

            isNotificationEnabled = isChecked
            if (isChecked) {
                binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
                binding.notificationSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            } else {
                binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                binding.notificationSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
            }
            updateStatus()
        }
    }

    private fun setThemeSwitch() {

        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                binding.themeSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
                binding.themeSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            } else {
                binding.themeSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                binding.themeSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
            }

            ThemeHelper.toggleTheme(this)
//            recreate()

            val intent = Intent(this, SplashScreenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        }
    }


    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } else {
            // For devices below Android Oreo, the settings might be in a different place
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }


    private fun checkSwitches() {
        isNotificationEnabled = areNotificationsEnabled()
        isDarkTheme = ThemeHelper.isDarkModeEnabled(this)
        binding.privateAccSwitch.isChecked = isPrivateAcc
        binding.notificationSwitch.isChecked = isNotificationEnabled
        binding.themeSwitch.isChecked = isDarkTheme
        if (isPrivateAcc){
            binding.privateAccSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
            binding.privateAccSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        } else {
            binding.privateAccSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            binding.privateAccSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
        }
        if (isNotificationEnabled){
            binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
            binding.notificationSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        }else{
            binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            binding.notificationSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
        }

        if (isDarkTheme){
            binding.themeSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
            binding.themeSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        }else{
            binding.themeSwitch.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
            binding.themeSwitch.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
        }





    }

    private fun updateStatus() {
        individualViewModal.updateStatus(isPrivateAcc,isNotificationEnabled)
        preferenceManager.putBoolean(PreferenceManager.Keys.IS_PRIVATE_ACCOUNT, isPrivateAcc)
        preferenceManager.putBoolean(PreferenceManager.Keys.IS_NOTIFICATION_ENABLED, isNotificationEnabled)
    }


}