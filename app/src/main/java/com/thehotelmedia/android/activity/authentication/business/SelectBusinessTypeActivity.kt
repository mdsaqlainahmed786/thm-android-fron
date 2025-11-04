package com.thehotelmedia.android.activity.authentication.business

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.ActivitySelectBusinessTypeBinding
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel

class SelectBusinessTypeActivity : BaseActivity() {

    private lateinit var binding: ActivitySelectBusinessTypeBinding
    private var selectBusinessType : String = ""
    private var selectBusinessId : String = ""

    private lateinit var authViewModel: AuthViewModel
    private val activity = this@SelectBusinessTypeActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectBusinessTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val authRepo = AuthRepo(activity)
        authViewModel = ViewModelProvider(activity, ViewModelFactory(authRepo))[AuthViewModel::class.java]
        val progressBar = CustomProgressBar(activity)
        getBusinessType()

//        binding.businessTv.setDropDownBackgroundDrawable(
//            ContextCompat.getDrawable(this, R.drawable.rounded_edit_text_background_normal)
//        )


        binding.businessTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.blured_background))
        binding.businessTv.dropDownVerticalOffset = binding.businessTv.height + 30





        val hotel = getString(R.string.hotel)
        val barsClubs = getString(R.string.bars_clubs)
        val homeStays = getString(R.string.home_stays)
        val marriageBanquets = getString(R.string.marriage_banquets)
        val restaurant = getString(R.string.restaurant)
//        val businesses = listOf(
//            Businesses(hotel, R.drawable.ic_hotel,"1"),
//            Businesses(barsClubs, R.drawable.ic_bar_clubs,"2"),
//            Businesses(homeStays, R.drawable.ic_home_stays,"3"),
//            Businesses(marriageBanquets, R.drawable.ic_marriage_banquets,"4"),
//            Businesses(restaurant, R.drawable.ic_restaurant,"5")
//        )
//        setBusinessAdapter(businesses)

        binding.backBtn.setOnClickListener {
            this.onBackPressedDispatcher.onBackPressed()
        }

        binding.nextBtn.setOnClickListener {


            if (selectBusinessType.isNotEmpty()){
                moveToBusinessTypeDetails(selectBusinessType,selectBusinessId)
            }else{
                CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseSelectTypeFirst(this))
            }


//            when (selectBusinessType) {
//                hotel -> {
//                    moveToBusinessTypeDetails(selectBusinessType,selectBusinessId)
//                }
//                barsClubs -> {
//                    moveToBusinessTypeDetails(selectBusinessType,selectBusinessId)
//                }
//                homeStays -> {
//                    moveToBusinessTypeDetails(selectBusinessType,selectBusinessId)
//                }
//                marriageBanquets -> {
//                    moveToBusinessTypeDetails(selectBusinessType,selectBusinessId)
//                }
//                restaurant -> {
//                    moveToBusinessTypeDetails(selectBusinessType,selectBusinessId)
//                }
//                else -> {
//                    CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseSelectTypeFirst(this))
//                }
//            }
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

    private fun getBusinessType() {
        authViewModel.getBusinessType()
    }

    private fun moveToBusinessTypeDetails(selectBusinessType: String, selectBusinessId: String) {
        val intent = Intent(this, BusinessTypeDetailsActivity::class.java)
        intent.putExtra("SELECTED_BUSINESS_TYPE", selectBusinessType)
        intent.putExtra("SELECTED_BUSINESS_ID", selectBusinessId)
        startActivity(intent)
    }

    private fun setBusinessAdapter(businesses: List<Businesses>) {
        val businessTypeAdapter = BusinessTypeAdapter(this, businesses)
        binding.businessTv.setAdapter(businessTypeAdapter)
        binding.businessTv.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as Businesses
            binding.businessTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))
            selectBusinessType = selectedItem.name
            selectBusinessId = selectedItem.id
            println("DrawableError    selectedItem.imageResId ${selectedItem.iconUrl}")


//            try{
//                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_save_icon)
//                binding.businessTv.setCompoundDrawablesRelative(drawable, null, drawable, null)
//                Log.e("DrawableError", "Drawable Set")
//            }catch (e: Exception){
//                Log.e("DrawableError", "e.message ${e.message}")
//            }
        }
    }

}

