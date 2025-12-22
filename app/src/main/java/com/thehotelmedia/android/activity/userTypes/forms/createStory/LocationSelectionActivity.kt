package com.thehotelmedia.android.activity.userTypes.forms.createStory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.PlacesAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityLocationSelectionBinding
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.openAppSettings
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class LocationSelectionActivity : BaseActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityLocationSelectionBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private lateinit var mMap: GoogleMap
    private val activity = this@LocationSelectionActivity
    private val tag: String = "LocationSelectionActivity"
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    
    private var selectedPlaceName: String? = null
    private var selectedPlaceLat: Double? = null
    private var selectedPlaceLng: Double? = null
    private var selectedPlaceAddress: String? = null
    private var selectedMarker: com.google.android.gms.maps.model.Marker? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                // Permissions granted, proceed to fetch location
                locationHelper.checkAndRequestLocation()
            } else {
                Toast.makeText(this, "Location permission is required to show nearby places", Toast.LENGTH_SHORT).show()
            }
        }

    private val locationSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // User enabled location
                checkLocationPermission()
            } else {
                // User clicked "No Thanks" (Denied enabling location)
                Toast.makeText(this, "Location is required to show nearby places", Toast.LENGTH_SHORT).show()
            }
        }

    private val autocompleteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestCode = AUTOCOMPLETE_REQUEST_CODE
        val resultCode = result.resultCode
        val data = result.data

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    Log.d(tag, "Place selected: ${place.name}, ${place.address}, latLng ${place.latLng}")

                    selectedPlaceName = place.name?.toString() ?: ""
                    selectedPlaceAddress = place.address?.toString() ?: ""
                    val latitude = place.latLng?.latitude
                    val longitude = place.latLng?.longitude

                    if (latitude != null && longitude != null) {
                        selectedPlaceLat = latitude
                        selectedPlaceLng = longitude
                        // Enable the "Add location" button
                        binding.addLocationBtn.isEnabled = true
                        binding.addLocationBtn.alpha = 1.0f
                        // Update map to show selected location
                        previewLocationOnMap(latitude, longitude, selectedPlaceName ?: "Selected location")
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data)
                    Toast.makeText(this, "${status.statusMessage}", Toast.LENGTH_LONG).show()
                    Log.d(tag, status.statusMessage ?: "Unknown error")
                }
                Activity.RESULT_CANCELED -> {
                    Log.d(tag, "Search canceled")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setColorFilter(ContextCompat.getColor(this, R.color.icon_color))

        // Initialize progress bar
        progressBar = CustomProgressBar(this)

        // Initialize Places API
        initPlaces()

        // Initialize map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Initialize UI
        initUI()

        // Check location permission and fetch nearby places
        checkLocationPermission()
    }

    private fun initPlaces() {
        val apiKey = BuildConfig.MAPS_API_KEY
        if (!Places.isInitialized()) {
            apiKey?.let { Places.initialize(applicationContext, it) }
        }
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]

        binding.backBtn.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.searchBtn.setOnClickListener {
            try {
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
                autocompleteLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        binding.addLocationBtn.setOnClickListener {
            if (selectedPlaceName != null && selectedPlaceLat != null && selectedPlaceLng != null) {
                returnSelectedLocation()
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }

        binding.previewOnMapBtn.setOnClickListener {
            if (selectedPlaceLat != null && selectedPlaceLng != null) {
                previewLocationOnMap(selectedPlaceLat!!, selectedPlaceLng!!, selectedPlaceName ?: "Selected location")
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
            }
        }

        // Initially disable the "Add location" button
        binding.addLocationBtn.isEnabled = false
        binding.addLocationBtn.alpha = 0.5f

        // Observe nearby places result
        individualViewModal.getNearbyPlacesResult.observe(activity) { result ->
            if (result.status == "OK") {
                val placeList = result.results
                if (placeList.isNotEmpty()) {
                    val placeAdapter = PlacesAdapter(activity, placeList) { placeId, name, address, lat, lng ->
                        onPlaceSelected(placeId, name, address, lat, lng)
                    }
                    binding.placesRv.adapter = placeAdapter
                } else {
                    Toast.makeText(activity, "No nearby places found", Toast.LENGTH_SHORT).show()
                }
            } else {
                val msg = result.status
                if (msg != "OK") {
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        individualViewModal.loading.observe(activity) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(activity) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPlaceSelected(_placeId: String, name: String, address: String, lat: Double, lng: Double) {
        selectedPlaceName = name
        selectedPlaceAddress = address
        selectedPlaceLat = lat
        selectedPlaceLng = lng
        
        // Enable the "Add location" button
        binding.addLocationBtn.isEnabled = true
        binding.addLocationBtn.alpha = 1.0f
        
        // Update map to show selected location
        previewLocationOnMap(lat, lng, name)
    }

    private fun returnSelectedLocation() {
        val resultIntent = Intent().apply {
            putExtra("PLACE_NAME", selectedPlaceName)
            putExtra("PLACE_ADDRESS", selectedPlaceAddress)
            putExtra("PLACE_LAT", selectedPlaceLat)
            putExtra("PLACE_LNG", selectedPlaceLng)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
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
                // Handle the location callback - fetch nearby places and update map
                val lat = latitude.toString()
                val lng = longitude.toString()
                getNearbyPlaces(lat, lng)
                updateMapLocation(latitude, longitude)
            },
            errorCallback = { errorMessage ->
                // Handle error callback
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )

        // Now check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }

    private fun checkLocationEnabled() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(false)

        val settingsClient = LocationServices.getSettingsClient(this)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Location is enabled
            locationHelper.checkAndRequestLocation()
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Show dialog to enable location
                    val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(exception.resolution).build()
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

    private fun getNearbyPlaces(lat: String, lng: String) {
        individualViewModal.getNearbyPlaces(lat, lng)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Disable default UI controls
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = false
            isMapToolbarEnabled = false
            isMyLocationButtonEnabled = false
        }

        // Enable gestures for map interaction (users can explore the map)
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true

        // Store the GoogleMap instance
        mMap = googleMap

        // Apply custom style to the map
        try {
            val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
            if (!success) {
                Log.d(tag, "Map style parsing failed.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Can't find map style. Error: ${e.message}")
        }

        // If we already have location, update map camera
        if (::locationHelper.isInitialized) {
            // Map will be updated when location is received
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        if (::mMap.isInitialized) {
            val currentLatLng = LatLng(latitude, longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            mMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))
        }
    }

    private fun previewLocationOnMap(latitude: Double, longitude: Double, title: String) {
        if (::mMap.isInitialized) {
            val locationLatLng = LatLng(latitude, longitude)
            
            // Remove previous marker if exists
            selectedMarker?.remove()
            
            // Add new marker for selected location
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(locationLatLng)
                    .title(title)
            )
            
            // Animate camera to show the location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15f))
        }
    }

    companion object {
        const val RESULT_PLACE_NAME = "PLACE_NAME"
        const val RESULT_PLACE_ADDRESS = "PLACE_ADDRESS"
        const val RESULT_PLACE_LAT = "PLACE_LAT"
        const val RESULT_PLACE_LNG = "PLACE_LNG"
    }
}

