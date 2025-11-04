package com.thehotelmedia.android.fragments.helpAndSupport

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentContactUsBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class ContactUsFragment : Fragment() {

    private lateinit var binding: FragmentContactUsBinding

    private lateinit var preferenceManager : PreferenceManager
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private var email = ""
    private var fullName = ""
    private var whatsAppNo = "+91 7738727020"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contact_us, container, false)

        initUi()

        return binding.root
    }

    private fun initUi() {
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        preferenceManager = PreferenceManager.getInstance(requireContext())
        email = preferenceManager.getString(PreferenceManager.Keys.USER_EMAIL, "") ?: ""
        fullName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME, "") ?: ""

        binding.nameEt.setText(fullName)
        binding.emailEt.setText(email)



        binding.whatsappBtn.setOnClickListener {
            if (isAppInstalled(requireActivity(), "com.whatsapp")) {
                openWhatsApp(whatsAppNo.toLong(), MessageStore.helloHotelMediaTeam(requireContext()))
            } else {
                CustomSnackBar.showSnackBar(binding.root,MessageStore.deviceNoWhatsapp(requireContext()))
            }
        }

        binding.nextBtn.setOnClickListener {
            val message = binding.descriptionEt.text.toString().trim()
            if (message.isNotEmpty()) {
                hitContactUsApi(message)
            }else{
                CustomSnackBar.showSnackBar(binding.root,MessageStore.enterYourMessage(requireContext()))
            }

        }


        individualViewModal.contactUsResult.observe(viewLifecycleOwner){result->
            if (result.status == true){
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
                binding.root.postDelayed({
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }, 1000)
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }

        individualViewModal.loading.observe(viewLifecycleOwner){
            if (it == true){
                progressBar.show()
            }else{
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }


    }
    private fun openWhatsApp(phoneNumber: Long, message: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        intent.data = Uri.parse(url)
        startActivity(intent)
    }
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        return try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun hitContactUsApi(message: String) {
        val fullNameData =  binding.nameEt.text.toString().trim()
        val fullEmail =  binding.emailEt.text.toString().trim()

        individualViewModal.contactUs(fullNameData,fullEmail,message)

    }
}