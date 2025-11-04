package com.thehotelmedia.android.activity.authentication.forgetPassword

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.individual.ValidationResult
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.ActivityResetPasswordBinding
import com.thehotelmedia.android.extensions.setEmailTextWatcher
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class ResetPasswordActivity : BaseActivity() {
    private lateinit var binding: ActivityResetPasswordBinding


    private lateinit var authViewModel: AuthViewModel
    private val activity = this@ResetPasswordActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
    }

    private fun initUi() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(activity)
        binding.emailEt.setEmailTextWatcher()

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

        binding.nextBtn.setOnClickListener {
            val result = validateFields(binding.emailEt)
            val email = binding.emailEt.text.toString().trim()
            if (result.isValid) {
                resetPassword(email)
            } else {
                CustomSnackBar.showSnackBar(binding.root, result.errorMessage.toString())
            }
        }


        authViewModel.forgotPasswordResult.observe(activity){result->
            if (result.status==true){
                val email = result.data?.email
                val intent = Intent(this, ConfirmResetEmailActivity::class.java)
                intent.putExtra("EMAIL_ADDRESS", email)
                startActivity(intent)
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

    private fun resetPassword(email: String) {
        authViewModel.forgotPassword(email)
    }


    private fun validateFields(
        emailEt: AppCompatEditText,
    ): ValidationResult {
        val fields = listOf(
            emailEt to ::validateEmail,
        )
        for ((editText, validator) in fields) {
            val result = validator(editText)
            if (!result.isValid) {
//                editText.error = result.errorMessage
                editText.requestFocus()
                return ValidationResult(false, result.errorMessage)
            }
        }

        return ValidationResult(true, null)
    }

    private fun validateEmail(editText: EditText): ValidationResult {
        val text = editText.text.toString().trim()
        return when {
            text.isEmpty() -> ValidationResult(false, "Enter email address.")
            !Patterns.EMAIL_ADDRESS.matcher(text).matches() -> ValidationResult(false, "Enter valid email address.")
            else -> ValidationResult(true, null)
        }
    }
}