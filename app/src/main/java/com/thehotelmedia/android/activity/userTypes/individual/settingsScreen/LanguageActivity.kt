package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.onBoarding.OnBoardingActivity
import com.thehotelmedia.android.customClasses.Constants.LANGUAGE_CODE
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityLanguageBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.util.*

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var currentLanguageCode: String = "en"

    private var from = ""
    private var language = ""
    private lateinit var individualViewModal: IndividualViewModal
    private val activity = this@LanguageActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the saved language code from shared preferences
        val sharedPreferences = getSharedPreferences("ONBOARD", MODE_PRIVATE)
        currentLanguageCode = sharedPreferences.getString(LANGUAGE_CODE, "en").orEmpty()

        // Inflate the layout using view binding
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the UI components
        initUI()
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        val progressBar = CustomProgressBar(activity)


        from = intent.getStringExtra("FROM") ?: ""

        if (from == "Settings"){
            binding.backBtn.visibility = View.VISIBLE
            binding.btnNext.visibility = View.GONE
        }else{
            binding.backBtn.visibility = View.GONE
            binding.btnNext.visibility = View.VISIBLE
        }


        setupLanguageSelection()
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnNext.setOnClickListener {
            val intent = Intent(this, OnBoardingActivity::class.java)
            startActivity(intent)
            finish()
        }



        individualViewModal.updateLanguageResult.observe(activity){result->
            if (result.status==true){
                setLocale(language)
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.loading.observe(activity) {
            if (it == true) {
                progressBar.show() // To show the progress bar
            } else {
                progressBar.hide() // To hide the progress bar
            }
        }




    }

    private fun setupLanguageSelection() {
        // Map each CheckBox to its corresponding language code
        val checkBoxes = listOf(
            binding.radioEnglish to "en",
            binding.radioHindi to "hi",
            binding.radioMarathi to "mr",
            binding.radioGujarati to "gu",
            binding.radioKannada to "kn",
            binding.radioTelugu to "te",
        )

        // Set the current language CheckBox as checked
        checkBoxes.forEach { (checkBox, languageCode) ->
            checkBox.isChecked = (languageCode == currentLanguageCode)
        }

        val singleSelectionListener = { selectedCheckBox: CheckBox ->
            checkBoxes.forEach { (checkBox, _) ->
                checkBox.isChecked = checkBox == selectedCheckBox
            }
        }

        // Set up click listeners for each CheckBox
        checkBoxes.forEach { (checkBox, languageCode) ->
            checkBox.setOnClickListener {
                singleSelectionListener(checkBox)
                saveSelectedLanguage(languageCode)
                language = languageCode

                if (from == "Settings"){
                    changeLanguage(languageCode)
                }else{
                    setNormalLocale(languageCode)
                }

            }
        }
    }

    private fun changeLanguage(languageCode: String) {
        individualViewModal.updateLanguage(languageCode)
    }

    private fun saveSelectedLanguage(languageCode: String) {
        val sharedPreferences = getSharedPreferences("ONBOARD", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(LANGUAGE_CODE, languageCode)
        editor.apply()
    }

    private fun setLocale(languageCode: String) {
//        val locale = Locale(languageCode)
//        Locale.setDefault(locale)
//        val config = Configuration(resources.configuration)
//        config.setLocale(locale)
//        resources.updateConfiguration(config, resources.displayMetrics)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)

        // Restart the app to apply changes
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun setNormalLocale(languageCode: String) {
//        val locale = Locale(languageCode)
//        Locale.setDefault(locale)
//        val config = Configuration()
//        config.locale = locale
//        resources.updateConfiguration(config, resources.displayMetrics)

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        createConfigurationContext(config)
    }
}
