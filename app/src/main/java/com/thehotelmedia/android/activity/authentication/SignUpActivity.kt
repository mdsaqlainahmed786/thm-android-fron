package com.thehotelmedia.android.activity.authentication

import android.content.Intent
import android.os.Bundle
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.authentication.business.SelectBusinessTypeActivity
import com.thehotelmedia.android.activity.authentication.individual.IndividualInfoActivity
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.ActivitySignUpBinding

class SignUpActivity : BaseActivity() {

    private lateinit var binding: ActivitySignUpBinding
    var selectedType : String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()

    }

    private fun initUI() {

        binding.individualBtn.setOnClickListener {
            selectIndividual()

        }
        binding.businessBtn.setOnClickListener {
            selectBusiness()
        }
        binding.nextBtn.setOnClickListener {
            if(selectedType.isEmpty()){
                CustomSnackBar.showSnackBar(binding.root, MessageStore.selectTypeFirst(this))
            }else if (selectedType == "Individual"){
                val intent = Intent(this, IndividualInfoActivity::class.java)
                startActivity(intent)
            }else if (selectedType == "Business"){
                val intent = Intent(this, SelectBusinessTypeActivity::class.java)
                startActivity(intent)
            }

        }

    }
    private fun selectIndividual() {
        selectedType = "Individual"
        updateButtonState(isIndividualSelected = true)
    }

    private fun selectBusiness() {
        selectedType = "Business"
        updateButtonState(isIndividualSelected = false)
    }
    private fun updateButtonState(isIndividualSelected: Boolean) {
        binding.individualBtn.isSelected = isIndividualSelected
        binding.individualTv.isSelected = isIndividualSelected
        binding.individualIv.isSelected = isIndividualSelected

        binding.businessBtn.isSelected = !isIndividualSelected
        binding.businessTv.isSelected = !isIndividualSelected
        binding.businessIv.isSelected = !isIndividualSelected
    }
}