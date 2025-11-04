package com.thehotelmedia.android.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.imageview.ShapeableImageView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityViewEventDetailsBinding
import com.thehotelmedia.android.extensions.EncryptionHelper
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.blurTheView
import com.thehotelmedia.android.extensions.calculateDistanceInKm
import com.thehotelmedia.android.extensions.formatDate
import com.thehotelmedia.android.extensions.formatTime
import com.thehotelmedia.android.extensions.isFutureDateOrTime
import com.thehotelmedia.android.extensions.moveToViewer
import com.thehotelmedia.android.extensions.setUpReadMore
import com.thehotelmedia.android.extensions.shareEventsWithDeepLink
import com.thehotelmedia.android.modals.viewPostEvent.ViewPostEventModal
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class ViewEventDetailsActivity : BaseActivity() , OnMapReadyCallback {

    private lateinit var binding: ActivityViewEventDetailsBinding
    private val activity = this@ViewEventDetailsActivity
    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var postId = ""
    private var streamingLink = ""
    private var businessName = ""
    private var eventImage = ""
    private var isJoined = false
    private var savedByMe = false
    private var userLatitude = 0.0
    private var userLongitude = 0.0
    private var lat = 0.0
    private var lng = 0.0
    private lateinit var mMap: GoogleMap
    private lateinit var preferenceManager: PreferenceManager
    private var ownerUserId = ""

    private lateinit var locationHelper: LocationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.hasDataLayout.visibility = View.GONE

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val individualRepo = IndividualRepo(activity)

        preferenceManager = PreferenceManager.getInstance(this)
        progressBar = CustomProgressBar(activity)
        individualViewModal = ViewModelProvider(activity, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

        progressBar.show()

        initUI()
    }

    private fun checkLocationPermission() {
        // Define the permission launcher
        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                    // Permissions granted, proceed to fetch location
                    locationHelper.checkAndRequestLocation()
                } else {
                    userLatitude = DEFAULT_LAT
                    userLongitude = DEFAULT_LNG
                    getEventData(postId)
                }
            }

        // Initialize the LocationHelper with required callbacks
        locationHelper = LocationHelper(
            context = this,
            permissionLauncher = permissionLauncher,
            locationCallback = { latitude, longitude ->
                userLatitude = latitude
                userLongitude = longitude
                getEventData(postId)
                // Handle the location callback
            },
            errorCallback = { errorMessage ->
                // Handle error callback
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                finish()
            }
        )

        // Check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }


    private fun initUI() {
//        postId = intent.getStringExtra("POST_ID").toString()

        if (intent.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            println("sdjakjfdska;k   uri  $uri")
            uri?.let {
                println("sdjakjfdska;k   it  $it")
                val postID = it.getQueryParameter("postID").toString()
                val userID = it.getQueryParameter("userID").toString()

                // Extract the encrypted values directly
                val encryptedPostID = it.getQueryParameter("postID").toString()
                val encryptedUserID = it.getQueryParameter("userID").toString()

                // Decrypt the values
                val decryptedPostID = EncryptionHelper.decrypt(encryptedPostID)
                val decryptedUserID = EncryptionHelper.decrypt(encryptedUserID)

                postId = decryptedPostID

                individualViewModal.getSharedPosts(postID,userID)


                // Now, you have the category ID and name, you can use them to populate the UI or perform any necessary actions
            }
        }else{

            postId = intent.getStringExtra("POST_ID").toString()
        }

        checkLocationPermission()





        val imageViews = listOf(
            binding.imageView1,
            binding.imageView2,
            binding.imageView3,
            binding.imageView4,
            binding.imageView5,
            binding.imageView6
        )
//        getEventData(userId)


        blurTheView(binding.shareBlurView)
        blurTheView(binding.joiningBlurView)
        blurTheView(binding.saveBlurView)

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.streamLinkTv.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(streamingLink))
            startActivity(intent)
        }

        binding.joiningBtn.setOnClickListener {
            joinEvent(postId)
            isJoined = !isJoined  // Flip the state
            updateJoiningBtn(isJoined,binding.joiningIv,binding.joiningTv)

        }

        binding.eventImageView.setOnClickListener {
            this.moveToViewer(IMAGE,eventImage)
        }

        binding.shareBlurView.setOnClickListener {

            this.shareEventsWithDeepLink(postId,ownerUserId)

        }

        binding.saveBlurView.setOnClickListener {
            savePost(postId)
            savedByMe = !savedByMe
            updateSaveBtn(savedByMe, binding.saveIv)
        }


        individualViewModal.getAllPostsResult.observe(activity) { result ->
            if (result.status == true) {
                handelEventDataResults(result,imageViews)
            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        individualViewModal.loading.observe(activity) {
            if (it == true) {
//                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(activity) {
            Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinEvent(userId: String) {
        individualViewModal.joinEvent(userId)
    }
    private fun savePost(id: String) {
        individualViewModal.savePost(id)
    }

    private fun handelEventDataResults(
        result: ViewPostEventModal?,
        imageViews: List<ShapeableImageView>
    ) {
        val data = result?.data
        val businessProfile = data?.postedBy?.businessProfileRef


        val type = data?.type ?: ""
        val startDate = data?.startDate ?: ""
        val startTime = data?.startTime ?: ""
        val endDate = data?.endDate ?: ""
        val endTime = data?.endTime ?: ""
        if (isFutureDateOrTime(startDate,startTime)) {
            binding.joiningBlurView.visibility = View.VISIBLE
        } else {
            binding.joiningBlurView.visibility = View.GONE
        }

        val eventName = data?.name ?: ""
        val description = data?.content ?: ""
        val venueName = data?.venue ?: ""
        streamingLink = data?.streamingLink ?: ""
        val interestedPeople = data?.interestedPeople ?: 0

        val profilePic = businessProfile?.profilePic?.medium ?: ""
//        eventImage = data?.mediaRef?.get(0)?.sourceUrl ?: ""

        val mediaRef = data?.mediaRef
        val eventImage = if (mediaRef?.isEmpty() == true){
            ""
        }else{
            data?.mediaRef?.get(0)?.sourceUrl ?: ""
        }

        businessName = businessProfile?.name ?: ""


        val businessProfileAddress = businessProfile?.address
        val businessStreet = businessProfileAddress?.street ?: ""
        val businessCity = businessProfileAddress?.city ?: ""
        val businessState = businessProfileAddress?.state ?: ""
        val businessZipCode = businessProfileAddress?.zipCode ?: ""
        val businessCountry = businessProfileAddress?.country ?: ""

        val location = data?.location
        val venueAddress = location?.placeName ?: ""
        lat = location?.lat ?: 0.0
        lng = location?.lng ?: 0.0


        val distanceInKm = calculateDistanceInKm(userLatitude, userLongitude, lat, lng).toInt()

        if (type == "online"){
            binding.addressLayout.visibility = View.GONE
            binding.venueLayout.visibility = View.GONE
        }else{
            binding.addressLayout.visibility = View.VISIBLE
            binding.venueLayout.visibility = View.VISIBLE
        }


        val formatedDate = if (startDate == endDate){
            formatDate(startDate)
        }else{
            "${formatDate(startDate)} - ${formatDate(endDate)}"
        }

        binding.startEndDateTv.text = formatedDate
        binding.startEndTimeTv.text = "${formatTime(startTime)} - ${formatTime(endTime)}"


        binding.peopleJoinedTv.text = "$interestedPeople ${getString(R.string.people_are_joined)}"
        binding.venueDistanceTv.text = "${distanceInKm} ${getString(R.string.km_distance_form_your_location)}"

        binding.descriptionTv.setUpReadMore(description, 150)
        Glide.with(activity).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.profileIv)
        Glide.with(activity).load(eventImage).placeholder(R.drawable.ic_post_placeholder).into(binding.eventImageView)

        binding.eventNameTv.text = eventName
        binding.venueNameTv.text = venueName
        binding.addressTv.text = "$businessStreet, $businessCity, $businessState, $businessZipCode, $businessCountry"
        binding.venueAddressTv.text = venueAddress
        binding.venueAddressTv2.text = venueAddress
        binding.fullNameTv.text = businessName

        if (streamingLink.isEmpty()){
            binding.streamLinkLayout.visibility = View.GONE
        }else{
            binding.streamLinkLayout.visibility = View.VISIBLE
        }

        binding.streamLinkTv.text = streamingLink

        isJoined = data?.imJoining ?: false
        savedByMe = data?.savedByMe ?: false
        updateJoiningBtn(isJoined,binding.joiningIv,binding.joiningTv)

        updateSaveBtn(savedByMe,binding.saveIv)


        val eventJoiningRef = data?.eventJoinsRef

//        if (!eventJoiningRef.isNullOrEmpty()){
//            binding.peopleJoinedLayout.visibility = View.VISIBLE
//        }else{
//            binding.peopleJoinedLayout.visibility = View.GONE
//        }

        // Check if the eventJoiningRef is not null and not empty
        if (!eventJoiningRef.isNullOrEmpty()) {
            binding.peopleJoinedLayout.visibility = View.VISIBLE

            // Get the minimum value between the size of eventJoiningRef and 6 (to show only the first 6 people)
            val numberOfPeopleToShow = eventJoiningRef.size.coerceAtMost(6)

            // Loop through the imageViews and assign profile pictures
            for (i in 0 until 6) {
                if (i < numberOfPeopleToShow) {
                    // Set the profile picture from the list
                    val person = eventJoiningRef[i]
                    val profilePicUrl = person.profilePic?.medium // Assuming 'url' is the field in ProfilePic

                    // Load the profile picture into the ImageView (using Glide or another image loading library)
                    Glide.with(activity)
                        .load(profilePicUrl)
                        .placeholder(R.drawable.ic_profile_placeholder) // Placeholder image
                        .into(imageViews[i])

                    // Make the ImageView visible
                    imageViews[i].visibility = View.VISIBLE
                } else {
                    // Hide unused ImageViews
                    imageViews[i].visibility = View.GONE
                }
            }
        } else {
            binding.peopleJoinedLayout.visibility = View.GONE
        }

        setupMap()
        binding.hasDataLayout.visibility = View.VISIBLE
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(activity) // Load the map when it's ready
    }

    private fun updateJoiningBtn(postSaved: Boolean, joiningIv: ImageView, joiningTv: TextView) {
        if (postSaved) {
            joiningIv.setImageResource(R.drawable.ic_filled_star_blue)
            joiningTv.text = "Joined"
        } else {
            joiningIv.setImageResource(R.drawable.ic_outline_star_white_)
            joiningTv.text = "Joining ?"
        }
    }

    private fun updateSaveBtn(postSaved: Boolean, saveIv: ImageView) {
        if (postSaved) {
            saveIv.setImageResource(R.drawable.ic_save_icon_white)
        } else {
            saveIv.setImageResource(R.drawable.ic_save_white)
        }
    }

    private fun getEventData(id: String) {
        individualViewModal.getAllPosts(id)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Disable default UI controls
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false // Disable zoom controls
            isCompassEnabled = false      // Disable compass
            isMapToolbarEnabled = true   // Disable map toolbar
            isMyLocationButtonEnabled = false // Disable my location button
            isScrollGesturesEnabled = false
        }


        // When the map is ready, store the GoogleMap instance
        mMap = googleMap


//        // Apply custom style to the map
//        try {
//            val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
//
//            if (!success) {
////                Log.e(TAG, "Style parsing failed.")
//                Toast.makeText(this, "Style parsing failed.", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
////            Log.e(TAG, "Can't find style. Error: ", e)
//            Toast.makeText(this, "Can't find style. Error: \", $e", Toast.LENGTH_SHORT).show()
//        }

        val currentLatLng = LatLng(lat, lng)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        mMap.addMarker(MarkerOptions().position(currentLatLng).title(businessName))

    }
}
