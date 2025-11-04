package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.ActivityDocumentsBinding
import com.thehotelmedia.android.extensions.moveToViewer
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class DocumentsActivity : BaseActivity() {

    private lateinit var binding: ActivityDocumentsBinding
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    private  var businessRegistration = ""
    private var addressProof = ""
    private var businessDocumentType = ""
    private var addressDocumentType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {

        progressBar = CustomProgressBar(this)
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]


        getSavedDocuments()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        binding.businessDocIv.setOnClickListener {
            if (businessDocumentType == "pdf") {
                openPdfInDevice(businessRegistration)
            } else if (businessDocumentType == "image") {
                this.moveToViewer("image", businessRegistration)
            }
        }

        binding.addressDocIv.setOnClickListener {
            if (addressDocumentType == "pdf") {
                openPdfInDevice(addressProof)
            } else if (addressDocumentType == "image") {
                this.moveToViewer("image", addressProof)
            }
        }




        individualViewModal.getSavedDocumentResult.observe(this){result->
            if (result.status == true){
                val msg = result.message.toString()

                val data = result.data[0]
                businessRegistration = data.businessRegistration ?: ""
                addressProof = data.addressProof ?: ""
                businessDocumentType = ""
                addressDocumentType = ""

                // Check if the URL points to a PDF or an image based on file extension
                if (businessRegistration.endsWith(".pdf", ignoreCase = true)) {
                    businessDocumentType = "pdf"
                    binding.businessTv.text = "BusinessRegistration.pdf"
                } else if (businessRegistration.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp)$", RegexOption.IGNORE_CASE))) {
                    businessDocumentType = "image"
                    Glide.with(this).load(businessRegistration).placeholder(R.drawable.ic_post_placeholder).into(binding.businessDocIv)
                } else {
                    businessDocumentType = ""
                }


                // Check if the URL points to a PDF or an image based on file extension
                if (addressProof.endsWith(".pdf", ignoreCase = true)) {
                    addressDocumentType = "pdf"
                    binding.addressTv.text = "AddressProof.pdf"
                } else if (addressProof.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp)$", RegexOption.IGNORE_CASE))) {
                    addressDocumentType = "image"
                    Glide.with(this).load(addressProof).placeholder(R.drawable.ic_post_placeholder).into(binding.addressDocIv)
                } else {
                    addressDocumentType = ""
                }



            }
        }


        individualViewModal.loading.observe(this){
            if (it == true){
                progressBar.show() // To show the giff progress bar
            }else{
                progressBar.hide() // To hide the giff progress bar
            }
        }

        individualViewModal.toast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }


    // Function to open a PDF from the given URL
    private fun openPdfInDevice(pdfUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(pdfUrl), "application/pdf")
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
    }


    private fun getSavedDocuments() {
        individualViewModal.getSavedBusinessDocuments()
    }
}