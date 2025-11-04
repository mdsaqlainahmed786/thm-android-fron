package com.thehotelmedia.android.activity.userTypes.profile

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityEditCategoryBinding
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class EditCategoryActivity : BaseActivity() {

    private lateinit var binding: ActivityEditCategoryBinding
    private var selectBusinessType : String = ""
    private var selectBusinessId : String = ""

    private lateinit var preferenceManager : PreferenceManager

    private lateinit var authViewModel: AuthViewModel
    private val activity = this@EditCategoryActivity
    private var selectedSubBusinessType: String = ""
    private var selectedSubBusinessId: String = ""
    private lateinit var individualViewModal: IndividualViewModal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {

        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        preferenceManager = PreferenceManager.getInstance(activity)
        val progressBar = CustomProgressBar(activity)
        getBusinessType()
        binding.businessTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.blured_background))
        binding.businessTv.dropDownVerticalOffset = binding.businessTv.height + 30
        binding.businessTypesTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.blured_background))
        binding.businessTypesTv.dropDownVerticalOffset = binding.businessTv.height + 30

        selectBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_BUSINESS_ID, "").toString()
        selectedSubBusinessId = preferenceManager.getString(PreferenceManager.Keys.USER_SUB_BUSINESS_ID, "").toString()




        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        

        binding.doneBtn.setOnClickListener {
            if (selectBusinessId.isEmpty()){
                CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseSelectBusinessType(this))
            }else if (selectedSubBusinessId.isEmpty()){
                CustomSnackBar.showSnackBar(binding.root,MessageStore.pleaseSelectSubBusinessType(this))
            }else{
                editCategory()
            }
        }



        authViewModel.businessTypeResult.observe(activity){result->
            if (result.status==true){
                println("dsfjahjkfhajk   ${result.data}")
                val businessesData = result.data

                // Map the data to Businesses objects
                val businesses = businessesData.map { dataItem ->
                    val name = dataItem.name.toString()  // Extract the name from the data item
                    val id = dataItem.id.toString()       // Extract the id from the data item
                    val icon = dataItem.icon.toString()       // Extract the icon from the data item
                    // Set the icon by downloading it or using a placeholder
                    // You can either handle URL loading with an image loader like Glide, or set a default icon
                    Businesses(name, icon,id)
                }

                // Set the adapter with the new businesses list
                setBusinessAdapter(businesses)


            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        authViewModel.subBusinessResult.observe(this) { result ->
            if (result.status == true) {
                val subBusinessesData = result.data
                val subBusinesses = subBusinessesData.map { dataItem ->
                    Businesses(dataItem.name.toString(), "", dataItem.id.toString())
                }
                setSubBusinessAdapter(subBusinesses)
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
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



        individualViewModal.editCategoryResult.observe(activity){result->
            if (result.status==true){

                preferenceManager.putString(PreferenceManager.Keys.USER_BUSINESS_ID, selectBusinessId)
                preferenceManager.putString(PreferenceManager.Keys.USER_BUSINESS_NAME, selectBusinessType)
                preferenceManager.putString(PreferenceManager.Keys.USER_SUB_BUSINESS_ID, selectedSubBusinessId)
                val intent = Intent(this, EditAmenitiesActivity::class.java)
                startActivity(intent)
                finish()

            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.loading.observe(activity){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        individualViewModal.toast.observe(activity){
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }




    }


    private fun getBusinessType() {
        authViewModel.getBusinessType()
    }


    private fun setBusinessAdapter(businesses: List<Businesses>) {
        val businessTypeAdapter = BusinessTypeAdapter(this, businesses)
        binding.businessTv.setAdapter(businessTypeAdapter)

        // Set pre-selected BusinessType based on selectBusinessId
        businesses.forEachIndexed { index, business ->
            if (business.id == selectBusinessId) {
                binding.businessTv.setText(business.name, false) // Set the name without triggering the dropdown
                binding.businessTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))

                selectBusinessType = business.name
                selectBusinessId = business.id

//                val iconUrl =  business.iconUrl
//                if (iconUrl.isNotEmpty()){
//                    binding.businessIv.visibility = View.VISIBLE
//                    Glide.with(this).asDrawable().load(iconUrl).placeholder(R.drawable.ic_save_icon).into(binding.businessIv)
//                }else{
//                    binding.businessIv.visibility = View.GONE
//                }

                getSubBusinessType() // Load the sub-businesses
            }
        }

        // Handle manual selection of BusinessType
        binding.businessTv.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as Businesses
            binding.businessTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))
            selectBusinessType = selectedItem.name
            selectBusinessId = selectedItem.id

//            val iconUrl = selectedItem.iconUrl
//            if (iconUrl.isNotEmpty()){
//                binding.businessIv.visibility = View.VISIBLE
//                Glide.with(this).asDrawable().load(iconUrl).placeholder(R.drawable.ic_save_icon).into(binding.businessIv)
//            }else{
//                binding.businessIv.visibility = View.GONE
//            }



            // Clear the selected sub-business when a new business is selected
            selectedSubBusinessType = ""
            selectedSubBusinessId = ""
            binding.businessTypesTv.setText("Types") // Clear the sub-business TextView
            binding.businessTypesTv.setTextColor(ContextCompat.getColor(this, R.color.white_50)) // Reset text color
            binding.businessTypesTv.setAdapter(null) // Clear the sub-business adapter

            // Fetch and load sub-business types for the newly selected business
            getSubBusinessType()
        }
    }



    private fun setSubBusinessAdapter(businesses: List<Businesses>) {
        val businessTypeAdapter = BusinessTypeAdapter(this, businesses)
        binding.businessTypesTv.setAdapter(businessTypeAdapter)

        // Set pre-selected SubBusinessType based on selectedSubBusinessId
        businesses.forEachIndexed { index, subBusiness ->
            if (subBusiness.id == selectedSubBusinessId) {
                binding.businessTypesTv.setText(subBusiness.name, false) // Set the name without triggering the dropdown
                binding.businessTypesTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))
                selectedSubBusinessType = subBusiness.name
                selectedSubBusinessId = subBusiness.id
            }
        }

        // Handle manual selection of SubBusinessType
        binding.businessTypesTv.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as Businesses
            binding.businessTypesTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))
            selectedSubBusinessType = selectedItem.name
            selectedSubBusinessId = selectedItem.id
        }
    }

    private fun getSubBusinessType() {
        authViewModel.getSubBusinessType(selectBusinessId)
    }


//    private fun moveToBusinessTypeDetails(selectBusinessType: String, selectBusinessId: String) {
//
//        Toast.makeText(activity,"selectBusinessType $selectBusinessType\nselectBusinessId $selectBusinessId", Toast.LENGTH_SHORT).show()
//
////        val intent = Intent(this, BusinessTypeDetailsActivity::class.java)
////        intent.putExtra("SELECTED_BUSINESS_TYPE", selectBusinessType)
////        intent.putExtra("SELECTED_BUSINESS_ID", selectBusinessId)
////        startActivity(intent)
//    }


    private fun editCategory() {
        individualViewModal.editCategory(selectBusinessId,selectedSubBusinessId)
    }

}