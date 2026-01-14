package com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.NotificationActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.search.SearchEventAdapter
import com.thehotelmedia.android.adapters.search.SearchPostAdapter
import com.thehotelmedia.android.adapters.search.SearchProfileAdapter
import com.thehotelmedia.android.adapters.search.SearchReviewsAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.search.OptionsAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.search.SingleTagAdapter
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentIndividualSearchBinding
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.NotificationDotUtil
import com.thehotelmedia.android.fragments.VideoPlayerManager
import com.thehotelmedia.android.modals.Business.businessType.Data
import com.thehotelmedia.android.repository.AuthRepo
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.authViewModel.AuthViewModel
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale


class IndividualSearchFragment : Fragment() {
    private lateinit var locationHelper: LocationHelper

    private lateinit var binding: FragmentIndividualSearchBinding
    private var initialKm = 40 // Set your initial km value here
    private var query = ""
    private var type = "business"
    private var from = ""
    private var ownerUserId = ""
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar
    private lateinit var searchProfileAdapter: SearchProfileAdapter
    private lateinit var searchPostAdapter: SearchPostAdapter
    private lateinit var searchEventAdapter: SearchEventAdapter
    private lateinit var searchReviewAdapter: SearchReviewsAdapter
    private lateinit var singleTagAdapter: SingleTagAdapter
    private var activePosition = 0
    private var posts = ""
    private var reviews = ""
    private var events = ""
    private var users = ""
    private var nearBy = ""
    private var business = ""

    private var formattedBusinessTags: List<String> = emptyList()

    private var lat = DEFAULT_LAT
    private var lng = DEFAULT_LNG
    private var currentLat = 0.0
    private var currentLng = 0.0
    private var apiKey: String? = null


    private var selectedFilterTag: String? = null

    private lateinit var authViewModel: AuthViewModel
    private var searchJob: Job? = null
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private val autocompleteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestCode = AUTOCOMPLETE_REQUEST_CODE
        val resultCode = result.resultCode
        val data = result.data
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    Log.d(tag, "Place: ${place.name}, ${place.id}, ${place.address}, latLng ${place.latLng}, places $place")
                    val latitude = place.latLng?.latitude ?: 0.0
                    val longitude = place.latLng?.longitude ?: 0.0
                    getAddressFromLatLng(latitude, longitude,"")
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data)
                    Toast.makeText(requireContext(), "${status.statusMessage}", Toast.LENGTH_LONG).show()
                    Log.d(tag, status.statusMessage ?: "Unknown error")
                }
                Activity.RESULT_CANCELED -> {
                    Log.wtf("Error", "Operation canceled")
                }
            }
        }
    }
    private val changeLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestCode = AUTOCOMPLETE_REQUEST_CODE
        val resultCode = result.resultCode
        val data = result.data
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    Log.d(tag, "Place: ${place.name}, ${place.id}, ${place.address}, latLng ${place.latLng}, places $place")
                    val latitude = place.latLng?.latitude ?: 0.0
                    val longitude = place.latLng?.longitude ?: 0.0
                    getAddressFromLatLng(latitude, longitude,"current")
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data)
                    Toast.makeText(requireContext(), "${status.statusMessage}", Toast.LENGTH_LONG).show()
                    Log.d(tag, status.statusMessage ?: "Unknown error")
                }
                Activity.RESULT_CANCELED -> {
                    Log.wtf("Error", "Operation canceled")
                }
            }
        }
    }

    private fun initPlaces() {
        apiKey = BuildConfig.MAPS_API_KEY
        if (!Places.isInitialized()) {
            apiKey?.let { Places.initialize(requireContext().applicationContext, it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            from = it.getString("FROM") ?: ""
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_individual_search, container, false)
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireActivity())
        preferenceManager = PreferenceManager.getInstance(requireContext())
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()


        val authRepo = AuthRepo(requireContext())
        authViewModel = ViewModelProvider(this, ViewModelFactory(authRepo))[AuthViewModel::class.java]

        posts = getString(R.string.posts)
        reviews = getString(R.string.reviews)
        events = getString(R.string.events)
        users = getString(R.string.users)
        nearBy = getString(R.string.nearby)
        business = getString(R.string.business)


        initPlaces()
        checkLocationPermission()
        inItUI()
        initializeAndUpdateNotificationDot()
        return binding.root
    }


    private fun initializeAndUpdateNotificationDot() {
        // Call this in onCreate or whenever needed
        NotificationDotUtil.initializeAndUpdateNotificationDot(
            requireContext(), // Context (Activity)
            binding.redDotView, // The red dot view
            preferenceManager // Your preference manager
        )
    }

    override fun onPause() {
        VideoPlayerManager.pausePlayer()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to avoid memory leaks
        NotificationDotUtil.unregisterReceiver(requireContext())
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
                    // Permissions not granted, handle the case
                    lat = DEFAULT_LAT
                    lng = DEFAULT_LNG
                    // Initialize UI even when location permission is denied
                    showFilterItemsLayout("")
                }
            }

        // Initialize the LocationHelper with required callbacks
        locationHelper = LocationHelper(
            context = requireContext(),
            permissionLauncher = permissionLauncher,
            locationCallback = { latitude, longitude ->
                currentLat = latitude
                currentLng = longitude
                getAddressFromLatLng(latitude, longitude,"current")
            },
            errorCallback = { errorMessage ->
                // Handle error callback
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                lat = DEFAULT_LAT
                lng = DEFAULT_LNG
                // Initialize UI even when location fetch fails
                showFilterItemsLayout("")
            }
        )

        // Check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }


    private fun getAddressFromLatLng(latitude: Double, longitude: Double, data: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)


            lat = latitude
            lng = longitude

            if (!addresses.isNullOrEmpty()) {
                val address: Address = addresses[0]
                val fullAddress = address.getAddressLine(0)  // Full address
                val city = address.locality
                val state = address.adminArea
                val country = address.countryName
                val postalCode = address.postalCode
                if (data == "current"){
                    binding.locationEt.hint = fullAddress
                    binding.locationEt.text?.clear()
                    showFilterItemsLayout("")
                }else{
                    binding.locationEt.setText(fullAddress)
                    binding.locationEt.hint = ""
                }
//                showFilterItemsLayout("")

            } else {
                lat = DEFAULT_LAT
                lng = DEFAULT_LNG
                binding.locationEt.setText("No address found!")
                // Initialize UI when current location geocoding fails
                if (data == "current") {
                    showFilterItemsLayout("")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun inItUI() {
        getBusinessType()

        if (from == "BUSINESS"){
            binding.titleLayout.visibility = View.GONE
            binding.blankView.visibility = View.GONE
            binding.blankView2.visibility = View.GONE
        }else{
            binding.titleLayout.visibility = View.VISIBLE
            binding.blankView.visibility = View.VISIBLE
            binding.blankView2.visibility = View.VISIBLE
        }

        searchProfileAdapter = SearchProfileAdapter(requireContext(),individualViewModal,childFragmentManager,ownerUserId)
        searchPostAdapter = SearchPostAdapter(requireContext(),individualViewModal,childFragmentManager,ownerUserId)
        searchEventAdapter = SearchEventAdapter(requireContext(),individualViewModal,childFragmentManager,ownerUserId)
        searchReviewAdapter = SearchReviewsAdapter(requireContext(),individualViewModal,childFragmentManager,ownerUserId)

        binding.searchEt.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

                // Do something after the text is changed, if necessary
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do something before the text is changed, if necessary
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val input = s.toString().trim()
                query = input
                getAddressFromLatLng(currentLat, currentLng, "current")




            }

        })




        binding.changeLocationBtn.setOnClickListener {
            try{
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(requireContext())
                changeLocationLauncher.launch(intent)
            }catch (e: Exception){
                Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show()
            }
        }




        // Set initial progress
        binding.kmSlider.progress = initialKm
        binding.distanceTv.text = "$initialKm km"

        binding.filterBtn.setOnClickListener {
            binding.filterLayout.visibility = View.VISIBLE
            binding.filterBtn.visibility = View.GONE
            binding.cancelFilterBtn.visibility = View.VISIBLE
            binding.itemsLayout.visibility = View.GONE
        }

        binding.notificationBtn.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            requireContext().startActivity(intent)
        }
        binding.locationEt.setOnClickListener {
            try{
                val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(requireContext())
                autocompleteLauncher.launch(intent)
            }catch (e: Exception){
                Toast.makeText(requireContext(), e.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        binding.applyFilterBtn.setOnClickListener {
            binding.searchEt.text?.clear()
            showFilterItemsLayout("Apply")
        }

        binding.cancelFilterBtn.setOnClickListener {
            singleTagAdapter.unselectAllTags()
            lat = currentLat
            lng = currentLng
            getAddressFromLatLng(currentLat,currentLng,"current")
            showFilterItemsLayout("")
        }

        binding.searchEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // When EditText is focused
                binding.searchLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_focused)
            } else {
                // When EditText loses focus
                binding.searchLayout.setBackgroundResource(R.drawable.rounded_edit_text_background_normal)
            }
        }

//        setLookingForFlexList()
//        mostlySearchedFlexList()
        binding.mostRecentlyTv.visibility = View.GONE

        binding.kmSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                initialKm = progress
                binding.distanceTv.text = "$initialKm km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Optional: Do something when tracking starts
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Optional: Do something when tracking stops
            }
        })



        authViewModel.businessTypeResult.observe(viewLifecycleOwner){result->
            if (result.status==true){
                val businessesData = result.data
                setLookingForFlexList(businessesData)
            }else{
                val msg = result.message
                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
            }
        }
        individualViewModal.toast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }
        individualViewModal.reportToast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }


    }

    private fun getBusinessType() {
        authViewModel.getBusinessType()
    }



    private fun showFilterItemsLayout(data: String) {
//        query = ""
        binding.filterLayout.visibility = View.GONE
        binding.filterBtn.visibility = View.VISIBLE
        binding.cancelFilterBtn.visibility = View.GONE
        binding.itemsLayout.visibility = View.VISIBLE
        val tags = if (data.isNotEmpty()) {
            listOf(business, users, posts, events, reviews)
        } else {
            if (selectedFilterTag.isNullOrEmpty()){
                listOf(nearBy, users, posts, events, reviews)
            }else{
                listOf(business, users, posts, events, reviews)
            }

        }

//        val tags = if (data.isNotEmpty()) {
//            listOf(business, users, posts, events, reviews)
//        } else {
//            listOf(nearBy, users, posts, events, reviews)
//        }

        val optionAdapter = OptionsAdapter(requireContext(),tags,::onOptionSelected)
        binding.optionsRv.adapter = optionAdapter

        if (selectedFilterTag.isNullOrEmpty()){
            onOptionSelected(tags[0]) // Call the selection function
            optionAdapter.setSelectedTag(tags[0]) // Update the adapter to mark the first item as selected
        }else{
            if (data.isNotEmpty()){
                onOptionSelected(tags[0]) // Call the selection function
                optionAdapter.setSelectedTag(tags[0]) // Update the adapter to mark the first item as selected
            }else{
                onOptionSelected(selectedFilterTag) // Call the selection function
                optionAdapter.setSelectedTag(selectedFilterTag!!) // Update the adapter to mark the first item as selected
            }
        }

//        // Manually select the first item after the adapter is set
//            onOptionSelected(tags[0]) // Call the selection function
//            optionAdapter.setSelectedTag(tags[0]) // Update the adapter to mark the first item as selected

    }


    private fun onOptionSelected(tags: String?) {
        selectedFilterTag = tags

        when (tags) {
            business -> {
                VideoPlayerManager.pausePlayer()
                type = "business"
                getSearchData()
            } nearBy -> {
                VideoPlayerManager.pausePlayer()
                type = "business"
                getSearchData()
            }
            users -> {
                VideoPlayerManager.pausePlayer()
                type = "users"
                getSearchData()
            }
            posts -> {
                type = "posts"
                getSearchData()
            }
            events -> {
                VideoPlayerManager.pausePlayer()
                type = "events"
                getSearchData()
            }
            reviews -> {
                VideoPlayerManager.pausePlayer()
                type = "reviews"
                getSearchData()
            }
        }
    }



    private fun setLookingForFlexList(businessesData: ArrayList<Data>) {

        singleTagAdapter = SingleTagAdapter(requireContext(),businessesData,::onLookingForTagSelected)
        binding.lookingForRv.adapter = singleTagAdapter
        val layoutManager = FlexboxLayoutManager(requireContext())
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.FLEX_START
        binding.lookingForRv.layoutManager = layoutManager
    }
    private fun mostlySearchedFlexList() {
        // Sample data
//        val tags = listOf("Maple Leaf Hotel", "CP67", "Pyramid Microbrewery | Café | Lounge | Bar Mohali", "TDI Club Retreat", "Grand Imperial, Sahibzada Ajit Singh Nagar")
//
//        val tagAdapter = SingleTagAdapter(tags,::onMostlySearchedTagSelected)
//        binding.mostlySearchedRv.adapter = tagAdapter
//        val layoutManager = FlexboxLayoutManager(requireContext())
//        layoutManager.flexDirection = FlexDirection.ROW
//        layoutManager.justifyContent = JustifyContent.FLEX_START
//        binding.mostlySearchedRv.layoutManager = layoutManager
    }

    private fun onLookingForTagSelected(selectedTags: List<String>) {
        formattedBusinessTags = selectedTags
//        Toast.makeText(requireContext(), formattedTags, Toast.LENGTH_SHORT).show()
//        Toast.makeText(requireContext(), selectedTags.joinToString(", "), Toast.LENGTH_SHORT).show()
//        setDelayThanWork()
    }
    private fun onMostlySearchedTagSelected(selectedTags: List<String>) {
        Toast.makeText(requireContext(), selectedTags.joinToString(", "), Toast.LENGTH_SHORT).show()

//        setDelayThanWork()
    }

    private fun getSearchData() {
        val adapter = getAdapterForType(type)
        binding.itemsRv.adapter = adapter.withLoadStateFooter(LoaderAdapter())
        binding.itemsRv.itemAnimator = null

        individualViewModal.getSearchData(query, type,formattedBusinessTags,initialKm.toString(),lat,lng).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading(adapter)
                adapter.submitData(it)
            }
        }

        // Scroll listener to track active item
        binding.itemsRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()

                if (firstVisibleItem != RecyclerView.NO_POSITION && firstVisibleItem != activePosition) {
                    updateActivePosition(firstVisibleItem)
                }
            }
        })


    }

    private fun updateActivePosition(newPosition: Int) {
        if (newPosition != activePosition) {
            val previousActivePosition = activePosition
            activePosition = newPosition
            // Notify adapter to update views
            searchPostAdapter.setActivePosition(activePosition)
            searchPostAdapter.notifyItemChanged(previousActivePosition)
            searchPostAdapter.notifyItemChanged(activePosition)
        }
    }

    private fun getAdapterForType(type: String) = when (type) {
        "business" -> searchProfileAdapter
        "users" -> searchProfileAdapter
        "posts" -> searchPostAdapter
        "events" -> searchEventAdapter
        "reviews" -> searchReviewAdapter
        else -> searchProfileAdapter
//        else -> throw IllegalArgumentException("Invalid type: $type")
    }

    private fun isLoading(adapter: PagingDataAdapter<*, *>) {
        adapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            val isEmpty = it.refresh is LoadState.NotLoading && adapter.itemCount == 0

            if (query.isEmpty()) {
                if (isLoading) {
                    progressBar.show()
                } else {
                    progressBar.hide()
                }
            }





            // Handle empty state
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.hasDataLayout.visibility = View.GONE

                if (from != "BUSINESS"){
                    val layoutParams = binding.noDataFoundLayout.layoutParams as LinearLayout.LayoutParams
                    layoutParams.bottomMargin = 100 // Set your desired margin
                    binding.noDataFoundLayout.layoutParams = layoutParams
                }
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.hasDataLayout.visibility = View.VISIBLE
            }
        }
    }



}


