package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityIndividualBillingAddressBinding
import com.thehotelmedia.android.extensions.toggleEnable
import com.thehotelmedia.android.modals.updateAddress.UpdateAddressModal
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.io.IOException
import java.util.Locale

class IndividualBillingAddressActivity : BaseActivity() {

    private lateinit var binding: ActivityIndividualBillingAddressBinding
    private lateinit var preferenceManager : PreferenceManager
    private val activity = this@IndividualBillingAddressActivity
    private val tag: String = "INDIVIDUAL_BILLING_ADDRESS"
    private var geocoder: Geocoder? = null
    private var apiKey: String? = null
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private var street = ""
    private var city = ""
    private var state = ""
    private var zipcode = ""
    private var country = ""
    private var addressName = ""
    private var placeID = ""
    private var newLatitude = 0.0
    private var newLongitude = 0.0
    private lateinit var individualViewModal: IndividualViewModal

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
        binding = ActivityIndividualBillingAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initPlaces()
        initUi()
    }
    private fun initPlaces() {
        apiKey = BuildConfig.MAPS_API_KEY
        if (!Places.isInitialized()) {
            apiKey?.let { Places.initialize(applicationContext, it) }
        }
    }

    private fun initUi() {
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        val progressBar = CustomProgressBar(activity)
        preferenceManager = PreferenceManager.getInstance(activity)


        val street = preferenceManager.getString(PreferenceManager.Keys.USER_STREET,"")
        val city = preferenceManager.getString(PreferenceManager.Keys.USER_CITY,"")
        val state = preferenceManager.getString(PreferenceManager.Keys.USER_STATE,"")
        val country = preferenceManager.getString(PreferenceManager.Keys.USER_COUNTRY,"")
        val zipCode = preferenceManager.getString(PreferenceManager.Keys.USER_ZIPCODE,"")

        val address = "$street, $city, $state, $country, $zipCode"

        if (!country.isNullOrEmpty()){
            binding.addressEt.setText(address)
            binding.doneBtn.toggleEnable(true)
        }else{
            binding.doneBtn.toggleEnable(false)
        }


        binding.addressEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Call getTaggedList when the text changes
                val searchText = s.toString()
                if (searchText.length >= 3){
                    binding.doneBtn.toggleEnable(true)
                }else{
                    binding.doneBtn.toggleEnable(false)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.addressEt.setOnClickListener {
            try{
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
                autocompleteLauncher.launch(intent)
            }catch (e: Exception){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }

        }

        binding.doneBtn.setOnClickListener {
            updateAddress()
        }


        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }



        individualViewModal.updateAddressResult.observe(activity){result->
            if (result.status==true){
                Handler(Looper.getMainLooper()).postDelayed({
                    handelUpdateAddressData(result)
                }, 800) // 1-second delay

            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.loading.observe(activity){
            if (it == true){
                progressBar.show() // To show the progress bar
                println("sdahfajkhfajk    show")
            }else{
                progressBar.hide() // To hide the progress bar
                println("sdahfajkhfajk    hide")
            }
        }

        individualViewModal.toast.observe(activity){
//            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }

    private fun handelUpdateAddressData(result: UpdateAddressModal) {
        val userStreet = result.data?.street ?: ""
        val userCity = result.data?.city ?: ""
        val userState = result.data?.state ?: ""
        val userZipCode = result.data?.zipCode ?: ""
        val userCountry = result.data?.country ?: ""
        val userLat = result.data?.lat ?: 0.0
        val userLng = result.data?.lng ?: 0.0

        preferenceManager.putString(PreferenceManager.Keys.USER_STREET, userStreet)
        preferenceManager.putString(PreferenceManager.Keys.USER_CITY, userCity)
        preferenceManager.putString(PreferenceManager.Keys.USER_STATE, userState)
        preferenceManager.putString(PreferenceManager.Keys.USER_COUNTRY, userCountry)
        preferenceManager.putString(PreferenceManager.Keys.USER_ZIPCODE, userZipCode)
        preferenceManager.putString(PreferenceManager.Keys.USER_LATITUDE, userLat.toString())
        preferenceManager.putString(PreferenceManager.Keys.USER_LONGITUDE, userLng.toString())
        onBackPressedDispatcher.onBackPressed()
    }

    private fun updateAddress() {
        individualViewModal.updateAddress(street,city,state,zipcode,country,newLatitude.toString(),newLongitude.toString())
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


}