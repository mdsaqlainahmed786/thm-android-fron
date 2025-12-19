package com.thehotelmedia.android.activity.userTypes.forms


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.gson.Gson
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.PlacesAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityCheckInBinding
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.capitalizeFirstLetter
import com.thehotelmedia.android.extensions.openAppSettings
import com.thehotelmedia.android.modals.checkinData.checkInData.ReviewQuestions
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.thehotelmedia.android.activity.TransparentBaseActivity

class CheckInActivity : TransparentBaseActivity(), OnMapReadyCallback {
    private lateinit var locationHelper: LocationHelper
    // Define the permission launcher as a class property
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                // Permissions granted, proceed to fetch location
                locationHelper.checkAndRequestLocation()
            } else {
                this.openAppSettings()
            }
        }
    private lateinit var binding: ActivityCheckInBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val activity = this@CheckInActivity
    private lateinit var individualViewModal: IndividualViewModal
    private var apiKey: String? = null
    private val tag: String = "CHECK_IN_ACTIVITY"
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private var placeID = ""
    private var placeName = ""
    private var placeAddress = ""
    private var addressName = ""
    private var geocoder: Geocoder? = null
    private lateinit var progressBar: CustomProgressBar
    private var locationDialog: AlertDialog? = null



    private val autocompleteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestCode = AUTOCOMPLETE_REQUEST_CODE
        val resultCode = result.resultCode
        val data = result.data
        
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    Log.d(tag, "Place: ${place.name}, ${place.id}, ${place.address}, latLng ${place.latLng}, places $place")

                    placeID = place.id?.toString() ?: ""
                    placeName = place.name?.toString() ?: ""
                    placeAddress = place.address?.toString() ?: ""
                    val latitude = place.latLng?.latitude
                    val longitude = place.latLng?.longitude
                    addressName = place.name?.toString() ?: ""



                    if (latitude != null && longitude != null) {

                        getCheckInData()

//                        getAddressFromLatLong(latitude, longitude)
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


    private val locationSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            println("afldjskhafk   $result")
            if (result.resultCode == Activity.RESULT_OK) {
                // User enabled location
                checkLocationPermission()
            } else {
                // User clicked "No Thanks" (Denied enabling location)
                finish()
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setColorFilter(ContextCompat.getColor(this, R.color.white))


        // Initialize the progress bar
        progressBar = CustomProgressBar(this)
        progressBar.show()

        // Use a handler to delay showing the full-screen view
        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.hide() // Hide progress bar after delay


            // Initialize the FusedLocationProviderClient to get the user's current location
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            // Initialize the map fragment
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(activity) // Load the map when it's ready
            initPlaces()
            initUI()


        }, 3000) // Adjust the delay time as needed





    }

    private fun initUI() {
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]




        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        binding.searchBtn.setOnClickListener {
            try{
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
                autocompleteLauncher.launch(intent)
            }catch (e: Exception){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }

        }

        individualViewModal.getNearbyPlacesResult.observe(activity){result->
            if (result.status == "OK"){
                val placeList = result.results

                val placeAdapter = PlacesAdapter(activity, placeList,::onPlaceSelected)
                binding.placesRv.adapter = placeAdapter

            }else{
                val msg = result.status
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }

        individualViewModal.getCheckInDataResult.observe(activity){result->
            if (result.status == true){

                val mapApiKey = "&key=${BuildConfig.MAPS_API_KEY}"

                val businessId = result.data?.businessProfileRef?.id ?: ""
                val typeOfId = result.data?.businessProfileRef?.type ?: ""
                var coverImage = result.data?.businessProfileRef?.coverImage ?: ""
                var profileImage = result.data?.businessProfileRef?.profilePic?.medium ?: ""

                if (businessId.isEmpty()){
                    coverImage = "$coverImage$mapApiKey"
                    profileImage = "$profileImage$mapApiKey"
                }



                val businessName = result.data?.businessProfileRef?.name?.capitalizeFirstLetter() ?: ""
                var street = result.data?.businessProfileRef?.address?.street ?: ""
                val city = result.data?.businessProfileRef?.address?.city ?: ""
                val state = result.data?.businessProfileRef?.address?.state ?: ""
                val zipCode = result.data?.businessProfileRef?.address?.zipCode ?: ""
                val country = result.data?.businessProfileRef?.address?.country ?: ""
                val lat = result.data?.businessProfileRef?.address?.lat ?: 0.0
                val long = result.data?.businessProfileRef?.address?.lng ?: 0.0

                val reviewQuestion = result.data?.reviewQuestions
                if (street.isBlank()){
                    street = businessName
                }

                moveToPreviousScreen(businessName,street, city, state, zipCode, country, lat, long, coverImage, profileImage,reviewQuestion,businessId,typeOfId)

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

    private fun onPlaceSelected(id: String,name: String,address: String, lat: Double, lng: Double) {
        placeID = id
        placeName = name
        placeAddress = address

        getCheckInData()
//        getAddressFromLatLong(lat, lng)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        // Disable default UI controls
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false // Disable zoom controls
            isCompassEnabled = false      // Disable compass
            isMapToolbarEnabled = false   // Disable map toolbar
            isMyLocationButtonEnabled = false // Disable my location button
        }

        // Optional: You can also disable gestures like scrolling and zooming
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true

        // When the map is ready, store the GoogleMap instance
        mMap = googleMap

        // Apply custom style to the map
        try {
            val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

            if (!success) {
//                Log.e(TAG, "Style parsing failed.")
                Toast.makeText(this, "Style parsing failed.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
//            Log.e(TAG, "Can't find style. Error: ", e)
            Toast.makeText(this, "Can't find style. Error: \", $e", Toast.LENGTH_SHORT).show()
        }
        checkLocationPermission()
    }

    // Function to check if location services are enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private fun checkLocationPermission() {

        // Location services enabled check
        if (!isLocationEnabled()) {
            checkLocationEnabled()
            return
        }

        // Initialize the LocationHelper with required callbacks
        locationHelper = LocationHelper(
            context = this,
            permissionLauncher = permissionLauncher,
            locationCallback = { latitude, longitude ->
                // Handle the location callback
                val currentLatLng = LatLng(latitude, longitude)
                val lat = latitude.toString()
                val lng = longitude.toString()
                getNearByData(lat,lng)

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
            },
            errorCallback = { errorMessage ->
                // Handle error callback
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        // Now check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }

    private fun checkLocationEnabled() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(false) // Always show the location enable dialog

        val settingsClient = LocationServices.getSettingsClient(this)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Location is enabled
            Toast.makeText(this, "Location is enabled", Toast.LENGTH_SHORT).show()
            locationHelper.checkAndRequestLocation()
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show dialog to enable location
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Error handling
                    Toast.makeText(this, "Error enabling location", Toast.LENGTH_SHORT).show()
                }
            } else {
                // If location settings can't be resolved
                Toast.makeText(this, "Location is disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun moveToPreviousScreen(
        businessName: String,
        street: String,
        city: String,
        state: String,
        zipcode: String,
        country: String,
        lat: Double,
        long: Double,
        coverImage: String,
        profileImage: String,
        reviewQuestion: ArrayList<ReviewQuestions>?,
        businessId: String,
        typeOfId: String,
    ) {

        val gson = Gson()
        val reviewQuestionJson = gson.toJson(reviewQuestion)

        // Create an intent to store the data
        val resultIntent = Intent().apply {
            putExtra("PLACE_ADDRESS", placeAddress)
            putExtra("PLACE_NAME", businessName)
            putExtra("PLACE_ID", placeID)
            putExtra("STREET", street)
            putExtra("CITY", city)
            putExtra("STATE", state)
            putExtra("ZIPCODE", zipcode)
            putExtra("COUNTRY", country)
            putExtra("LAT", lat)
            putExtra("LNG", long)
            putExtra("COVER_IMAGE", coverImage)
            putExtra("PROFILE_PIC", profileImage)
            putExtra("REVIEW_QUESTION", reviewQuestionJson)
            putExtra("BUSINESS_ID", businessId)
            putExtra("TYPE_OF_ID", typeOfId)
        }

        // Set result code and data
        setResult(RESULT_OK, resultIntent)

        // Finish the activity and go back to MainActivity
        finish()
    }

    private fun getNearByData(lat: String, lng: String) {
        individualViewModal.getNearbyPlaces(lat,lng)
    }
    private fun getCheckInData() {
        println("asfjklasfdjkasdfh   $placeID")
        individualViewModal.getCheckInData(placeID,"")
    }
    private fun initPlaces() {
        apiKey = BuildConfig.MAPS_API_KEY
        if (!Places.isInitialized()) {
            apiKey?.let { Places.initialize(applicationContext, it) }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }




}
