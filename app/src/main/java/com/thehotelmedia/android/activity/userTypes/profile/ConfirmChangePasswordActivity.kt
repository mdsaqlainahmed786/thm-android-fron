package com.thehotelmedia.android.activity.userTypes.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityConfirmChangePasswordBinding
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class ConfirmChangePasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityConfirmChangePasswordBinding
    private var isFirstPasswordVisible = false
    private var isSecondPasswordVisible = false
    private var userEmailAddress : String = ""

    private var businessType : String = ""
    private var resetToken : String = ""
    private lateinit var authViewModel: AuthViewModel
    private val activity = this@ConfirmChangePasswordActivity
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }


    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        val progressBar = CustomProgressBar(activity) // 'this' refers to the context

        businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        userEmailAddress = intent.getStringExtra("EMAIL_ADDRESS") ?: ""
        resetToken = intent.getStringExtra("RESET_TOKEN") ?: ""
        setPasswordEt1()
        setPasswordEt2()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.doneBtn.setOnClickListener {
                if (!validateFirstPassword()) return@setOnClickListener
                if (!validateSecondPassword()) return@setOnClickListener
                if (!doPasswordsMatch()) return@setOnClickListener

                val password = binding.passwordEt1.text.toString().trim()
                changePassword(password)
                // If all validations pass, show valid data message

        }


        authViewModel.changePasswordResult.observe(activity){result->
            if (result.status==true){
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root, msg)

                // Delay for 1 seconds (1000 milliseconds) to show the Snackbar before navigating back
                binding.root.postDelayed({
                    if (businessType == business_type_individual){
                        val intent = Intent(activity, BottomNavigationIndividualMainActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    }else{
                        val intent = Intent(activity, BottomNavigationBusinessMainActivity::class.java)
                        startActivity(intent)
                        finishAffinity()
                    }
                }, 1000)


            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        authViewModel.loading.observe(activity){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        authViewModel.toast.observe(activity){
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }


    }

    private fun changePassword(password: String) {
        authViewModel.changePassword(password,resetToken,userEmailAddress)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setPasswordEt1() {
        binding.passwordEt1.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.passwordEt1.right - binding.passwordEt1.compoundDrawables[DRAWABLE_END].bounds.width())) {
                    isFirstPasswordVisible = !isFirstPasswordVisible
                    togglePasswordVisibility(isFirstPasswordVisible, binding.passwordEt1)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setPasswordEt2() {
        binding.passwordEt2.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.passwordEt2.right - binding.passwordEt2.compoundDrawables[DRAWABLE_END].bounds.width())) {
                    isSecondPasswordVisible = !isSecondPasswordVisible
                    togglePasswordVisibility(isSecondPasswordVisible, binding.passwordEt2)
                    return@setOnTouchListener true
                }
            }
            false
        }

    }
    private fun togglePasswordVisibility(isVisible: Boolean, editText: AppCompatEditText) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_password_icon,
                0,
                R.drawable.ic_show_password,
                0
            )
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_password_icon,
                0,
                R.drawable.ic_hide_password,
                0
            )
        }
        editText.setSelection(editText.text?.length ?: 0) // Keep the cursor at the end of the text
    }

    private fun validateFirstPassword(): Boolean {
        val password = binding.passwordEt1.text.toString()

        // Check if first password is entered
        if (password.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterYourPassword(this))
            binding.passwordEt1.requestFocus()
            return false
        }

        // Check if the first password is strong
        if (!isStrongPassword(password)) {
            CustomSnackBar.showSnackBar(binding.root,  MessageStore.passwordNotStrongEnough(this))
            binding.passwordEt1.requestFocus()
            return false
        }

        return true
    }
    private fun validateSecondPassword(): Boolean {
        val password = binding.passwordEt2.text.toString()

        // Check if second password is entered
        if (password.isEmpty()) {
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseConfirmYourPassword(this))
            binding.passwordEt2.requestFocus()
            return false
        }

        return true
    }
    private fun isStrongPassword(password: String): Boolean {
        // Ensure the password is at least 8 characters long and contains a digit, uppercase, lowercase, and special character
        val passwordPattern = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!])(?=\\S+\$).{8,}\$")
        return passwordPattern.matches(password)
    }
    private fun doPasswordsMatch(): Boolean {
        val password1 = binding.passwordEt1.text.toString()
        val password2 = binding.passwordEt2.text.toString()

        // Check if passwords match
        if (password1 != password2) {
            CustomSnackBar.showSnackBar(binding.root, MessageStore.passwordDontMatch(this))
            binding.passwordEt2.requestFocus()
            return false
        }

        return true
    }

}