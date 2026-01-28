package com.thehotelmedia.android.activity.authentication.business

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.authentication.business.TagAdapter
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.databinding.ActivityBusinessTypeDetailsBinding
import com.thehotelmedia.android.extensions.hideKeyboard
import com.thehotelmedia.android.extensions.setEmailTextWatcher
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import java.io.IOException
import java.util.Locale

class BusinessTypeDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityBusinessTypeDetailsBinding
    private var selectedBusinessType: String = ""
    private var selectedBusinessTypeInLanguage: String = ""
    private var selectBusinessId: String = ""
    private var selectedSubBusinessType: String = ""
    private var selectedSubBusinessId: String = ""
    private var selectedCountryCode: String = "+91"
    private lateinit var tagAdapter: TagAdapter
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private lateinit var authViewModel: AuthViewModel
    private val tag: String = "BUSINESS_TYPE_DETAILS"
    private var geocoder: Geocoder? = null
    private var apiKey: String? = null

    private var street = ""
    private var city = ""
    private var state = ""
    private var zipcode = ""
    private var country = ""
    private var addressName = ""
    private var placeID = ""
    private var newLatitude = 0.0
    private var newLongitude = 0.0

    private val autocompleteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestCode = AUTOCOMPLETE_REQUEST_CODE
        val resultCode = result.resultCode
        val data = result.data
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    Log.d(tag, "Place: ${place.name}, ${place.id}, ${place.address}, latLng ${place.latLng}, places $place")
                    placeID = place.id?.toString() ?: "null"
                    val latitude = place.latLng?.latitude
                    val longitude = place.latLng?.longitude
                    addressName = place.name.toString()
                    if (latitude != null && longitude != null) {
                        getAddressFromLatLong(latitude, longitude)
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data)
                    Toast.makeText(this, "${status.statusMessage}", Toast.LENGTH_LONG).show()
                    Log.d(tag, status.statusMessage ?: "Unknown error")
                }
                Activity.RESULT_CANCELED -> {
                    Log.wtf("Error", "Operation canceled")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure the window resizes when the IME (keyboard) is shown so the form can scroll above it.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        binding = ActivityBusinessTypeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initPlaces()
        initUI()
    }

    private fun initUI() {
        val authRepo = AuthRepo(this)
        authViewModel = ViewModelProvider(this, ViewModelFactory(authRepo)).get(AuthViewModel::class.java)
        val progressBar = CustomProgressBar(this)
        binding.emailEt.setEmailTextWatcher()

        selectedBusinessType = intent.getStringExtra("SELECTED_BUSINESS_TYPE") ?: ""

        when (selectedBusinessType) {
            "Hotel" -> {
                selectedBusinessTypeInLanguage = getString(R.string.hotel)
            }
            "Bars / Clubs" -> {
                selectedBusinessTypeInLanguage = getString(R.string.bars_clubs)
            }
            "Home Stays" -> {
                selectedBusinessTypeInLanguage = getString(R.string.home_stays)
            }
            "Marriage Banquets" -> {
                selectedBusinessTypeInLanguage = getString(R.string.marriage_banquets)
            }
            "Restaurant" -> {
                selectedBusinessTypeInLanguage = getString(R.string.restaurant)
            }
        }

        selectBusinessId = intent.getStringExtra("SELECTED_BUSINESS_ID") ?: ""
        setUpUi()
        getSubBusinessType()

        binding.countryCodePicker.setOnCountryChangeListener {
            selectedCountryCode = binding.countryCodePicker.selectedCountryCodeWithPlus
            binding.countryFlagImageView.setImageResource(binding.countryCodePicker.selectedCountryFlagResourceId)
            Log.d(tag, "Selected Country Code: $selectedCountryCode")
        }

        binding.contactEt.setOnFocusChangeListener { _, hasFocus ->
            binding.contactLayout.setBackgroundResource(
                if (hasFocus) R.drawable.rounded_edit_text_background_focused
                else R.drawable.rounded_edit_text_background_normal
            )
        }

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.nextBtn.setOnClickListener {

            val name = binding.nameEt.text.toString().trim()
            val phoneNumber = binding.contactEt.text.toString().trim()
            val webSiteLink = binding.webLinkEt.text.toString().trim()
            val email = binding.emailEt.text.toString().trim()
            val gst = binding.gstEt.text.toString().trim()
            val description = binding.descriptionEt.text.toString().trim()

            if (isValid(name,phoneNumber,webSiteLink,email,description,gst)) {

                moveToManagerInfoActivity(name,phoneNumber,webSiteLink,email,gst,description)


            }

        }

        binding.addressEt.setOnClickListener {
            try{
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
                autocompleteLauncher.launch(intent)
            }catch (e: Exception){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }

        }


        authViewModel.subBusinessResult.observe(this) { result ->
            if (result.status == true) {
                val subBusinessesData = result.data
                val subBusinesses = subBusinessesData.map { dataItem ->
                    Businesses(dataItem.name.toString(), "", dataItem.id.toString())
                }
                setBusinessAdapter(subBusinesses)
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.loading.observe(this) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        authViewModel.toast.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToManagerInfoActivity(name: String, phoneNumber: String, webSiteLink: String, email: String, gst: String, description: String) {
                val intent = Intent(this, ManagerInfoActivity::class.java)
                intent.putExtra("BUSINESS_NAME", name)
                intent.putExtra("BUSINESS_DIAL_CODE", selectedCountryCode)
                intent.putExtra("BUSINESS_PHONE_NUMBER", phoneNumber)
                intent.putExtra("BUSINESS_WEBSITE_LINK", webSiteLink)
                intent.putExtra("BUSINESS_EMAIL", email)
                intent.putExtra("BUSINESS_GST", gst)
                intent.putExtra("SELECTED_BUSINESS_TYPE", selectedBusinessType)
                intent.putExtra("SELECTED_BUSINESS_ID", selectBusinessId)
                intent.putExtra("SELECTED_SUB_BUSINESS_TYPE", selectedSubBusinessType)
                intent.putExtra("SELECTED_SUB_BUSINESS_ID", selectedSubBusinessId)
                intent.putExtra("BUSINESS_DESCRIPTION", description)
                intent.putExtra("STREET", street)
                intent.putExtra("CITY", city)
                intent.putExtra("STATE", state)
                intent.putExtra("ZIPCODE", zipcode)
                intent.putExtra("COUNTRY", country)
                intent.putExtra("PLACED_ID", placeID)
                intent.putExtra("LATITUDE", newLatitude)
                intent.putExtra("LONGITUDE", newLongitude)
                startActivity(intent)
    }

    private fun getSubBusinessType() {
        authViewModel.getSubBusinessType(selectBusinessId)
    }


    private fun setUpUi() {
        val name = getString(R.string.name)
        val type = getString(R.string.type)
        binding.nameTv.text = "$selectedBusinessTypeInLanguage $name"
        binding.nameEt.hint = "$selectedBusinessTypeInLanguage $name"
        binding.typesTv.text = "$selectedBusinessTypeInLanguage $type"

        when (selectedBusinessType) {
            getString(R.string.hotel) -> binding.nameEt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hotel, 0, 0, 0)
            getString(R.string.bars_clubs) -> binding.nameEt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bar_clubs, 0, 0, 0)
            getString(R.string.home_stays) -> binding.nameEt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_home_stays, 0, 0, 0)
            getString(R.string.marriage_banquets) -> binding.nameEt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_marriage_banquets, 0, 0, 0)
            getString(R.string.restaurant) -> binding.nameEt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_restaurant, 0, 0, 0)
        }
    }

    private fun setBusinessAdapter(businesses: List<Businesses>) {
        binding.businessTypesTv.setDropDownBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.blured_background))
        binding.businessTypesTv.dropDownVerticalOffset = binding.businessTypesTv.height + 30

        val businessTypeAdapter = BusinessTypeAdapter(this, businesses)
        binding.businessTypesTv.setAdapter(businessTypeAdapter)
        binding.businessTypesTv.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as Businesses
            binding.businessTypesTv.setTextColor(ContextCompat.getColor(this, R.color.text_color))
            selectedSubBusinessType = selectedItem.name
            selectedSubBusinessId = selectedItem.id
        }
    }

    private fun getAddressFromLatLong(lat: Double, long: Double) {
        try {
            geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address> = geocoder?.getFromLocation(lat, long, 1) ?: return
            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                street = address.thoroughfare ?: ""
                if (street.isEmpty()){
                    street = addressName
                }
                city = address.locality ?: ""
                state = address.adminArea ?: ""
                zipcode = address.postalCode ?: ""
                country = address.countryName ?: ""
                newLatitude = lat
                newLongitude = long
//                binding.addressTv.text = "$street, $city, $state, $zipcode, $country"
                binding.addressEt.setText("$street, $city, $state, $zipcode, $country")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, MessageStore.errorFetchingAddress(this), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPlaces() {
        apiKey = BuildConfig.MAPS_API_KEY
        if (!Places.isInitialized()) {
            apiKey?.let { Places.initialize(applicationContext, it) }
        }
    }

    private fun isValid(
        name: String,
        phoneNumber: String,
        webSiteLink: String,
        email: String,
        description: String,
        gst: String
    ): Boolean {
        if (name.isBlank()) {
            CustomSnackBar.showSnackBar(binding.root, "${MessageStore.pleaseEnter(this)} $selectedBusinessType ${MessageStore.name(this)}")
            binding.nameEt.requestFocus()
            return false
        }else if(addressName.isBlank()){
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseSelectAddress(this))
            hideKeyboard()
            return false
        }else if(phoneNumber.isBlank()){
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterContactNumber(this))
            binding.contactEt.requestFocus()
            return false
        } else if (phoneNumber.length != 10) {  // Check for exactly 10 digits
            CustomSnackBar.showSnackBar(binding.root, MessageStore.enterValid10DigitNumber(this))
            binding.contactEt.requestFocus()
            return false
        } else if(webSiteLink.isNotBlank() && !Patterns.WEB_URL.matcher(webSiteLink).matches()){
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterValidWebsiteLink(this))
            binding.webLinkEt.requestFocus()
            return false
        } else if(email.isBlank()){
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterYourEmail(this))
            binding.emailEt.requestFocus()
            return false
        }else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterValidEmail(this))
            binding.emailEt.requestFocus()
            return false
        }else if(selectedSubBusinessType.isBlank()){
            CustomSnackBar.showSnackBar(binding.root, "${MessageStore.pleaseSelect(this)} $selectedBusinessType ${MessageStore.type(this)}")
            hideKeyboard()
            return false
        }else if(gst.isNotEmpty() && gst.length != 15){
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterGstinNumber(this))
            hideKeyboard()
            return false
        }

        else if(description.isBlank()){
            CustomSnackBar.showSnackBar(binding.root, MessageStore.pleaseEnterDescription(this))
            binding.descriptionEt.requestFocus()
            return false
        }

        return true
    }

}
