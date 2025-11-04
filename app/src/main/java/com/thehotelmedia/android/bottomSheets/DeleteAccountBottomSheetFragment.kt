package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.authentication.SignInActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentDeleteAccountBottomSheetBinding
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel


class DeleteAccountBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentDeleteAccountBottomSheetBinding
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var authViewModel: AuthViewModel


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        // You can also set a custom enter animation here if needed
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return bottomSheetDialog
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_delete_account_bottom_sheet, container, false)

        initUI()
        return binding.root
    }

    private fun initUI() {
        val authRepo = AuthRepo(requireContext())
        authViewModel = ViewModelProvider(this, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(requireContext()) // 'this' refers to the context
        preferenceManager = PreferenceManager.getInstance(requireContext())


        binding.yesBtn.setOnClickListener {
            // Handle log out action
            deleteAccount()
        }

        binding.noBtn.setOnClickListener {
            dismiss() // Close the BottomSheet
        }


        authViewModel.deleteAccountResult.observe(requireActivity()){result->
            if (result.status==true){
                moveToSignInActivity()
            }else{
                val msg = result.message
                Toast.makeText(requireActivity(),msg, Toast.LENGTH_SHORT).show()
                moveToSignInActivity()
            }
        }


        authViewModel.loading.observe(requireActivity()){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        authViewModel.toast.observe(requireActivity()){
            Toast.makeText(requireActivity(),it, Toast.LENGTH_SHORT).show()
        }



    }


    private fun moveToSignInActivity() {

        preferenceManager.clearPreferences()
        val intent = Intent(requireActivity(), SignInActivity::class.java)
        startActivity(intent)
        // Finish the current activity to prevent going back
        requireActivity().finishAffinity()
    }

    private fun deleteAccount() {
        authViewModel.deleteAccount()
    }


}