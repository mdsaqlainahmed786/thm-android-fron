package com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.NotificationActivity
import com.thehotelmedia.android.activity.userTypes.business.BusinessSearchActivity
import com.thehotelmedia.android.activity.userTypes.forms.createStory.CreateStoryActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LAT
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_LNG
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_PDF_MB
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_VIDEO_LENGTH
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentIndividualHomeBinding
import com.thehotelmedia.android.extensions.LocationHelper
import com.thehotelmedia.android.extensions.NotificationDotUtil
import com.thehotelmedia.android.extensions.animateVisibilityToggle
import com.thehotelmedia.android.extensions.setOnSwipeListener
import android.view.MotionEvent
import com.thehotelmedia.android.extensions.openWeatherApp
import com.thehotelmedia.android.extensions.setWeatherAndTemperature
import com.thehotelmedia.android.extensions.toAQI
import com.thehotelmedia.android.extensions.toAqiType
import com.thehotelmedia.android.fragments.VideoPlayerManager
import com.thehotelmedia.android.fragments.userTypes.FeedAdapter
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class IndividualHomeFragment : Fragment() {
    private lateinit var locationHelper: LocationHelper
    private lateinit var binding: FragmentIndividualHomeBinding
    private lateinit var preferenceManager: PreferenceManager
    private var type = ""
    private var profilePic = ""
    private var ownerUserId = ""
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var feedAdapter: FeedAdapter
//    private lateinit var storyAdapter: StoryAdapter
    private lateinit var progressBar: CustomProgressBar
    private var postIds: List<String> = emptyList()
    private var activePosition = 1 // No active position initially
    private var currentLat = DEFAULT_LAT
    private var currentLng = DEFAULT_LNG
    private val batchSize = 30
    private val handler = Handler(Looper.getMainLooper())
    private var isScrolling = false
    val debounceRunnable = Runnable {
        if (isScrolling) {
            val layoutManager = binding.postRecyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()
            if (firstVisibleItem != RecyclerView.NO_POSITION && firstVisibleItem != activePosition) {
                updateActivePosition(firstVisibleItem)
            }
            isScrolling = false
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_individual_home, container, false)
        progressBar = CustomProgressBar(requireActivity())
        checkLocationPermission()

        initUI()
        initializeAndUpdateNotificationDot()
        setupSwipeGestures()
        setupRecyclerViewSwipeGestures()

        return binding.root
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
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
                    Toast.makeText(requireContext(), "Location permissions denied", Toast.LENGTH_SHORT).show()
                }
            }

        // Initialize the LocationHelper with required callbacks
        locationHelper = LocationHelper(
            context = requireContext(),
            permissionLauncher = permissionLauncher,
            locationCallback = { latitude, longitude ->
                // Handle the location callback
                currentLat = latitude
                currentLng = longitude
                getFeedData("")
            },
            errorCallback = { errorMessage ->
                // Handle error callback
                Toast.makeText(requireContext(), "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        )

        // Check and request location permission when needed
        locationHelper.checkAndRequestLocation()
    }



    private fun initializeAndUpdateNotificationDot() {
        NotificationDotUtil.initializeAndUpdateNotificationDot(
            requireContext(), // Context (Activity)
            binding.redDotView, // The red dot view
            preferenceManager // Your preference manager
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationDotUtil.unregisterReceiver(requireContext()) // Unregister receiver
    }

    private fun initUI() {

        preferenceManager = PreferenceManager.getInstance(requireContext())
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]

        type = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        profilePic = preferenceManager.getString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, "").toString()
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

//        storyAdapter = StoryAdapter(requireContext(), profilePic)
        feedAdapter = FeedAdapter(requireContext(), individualViewModal, parentFragmentManager,viewLifecycleOwner,profilePic,binding.postRecyclerView,ownerUserId,::onIdActive)

        if (type == business_type_individual) {
            binding.searchBtn.visibility = View.GONE
        } else {
            binding.searchBtn.visibility = View.VISIBLE
        }

        binding.notificationBtn.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            requireContext().startActivity(intent)
        }

        binding.searchBtn.setOnClickListener {
            val intent = Intent(requireContext(), BusinessSearchActivity::class.java)
            requireContext().startActivity(intent)
        }

        getSetWeatherAndAQI()

        binding.weatherBtn.openWeatherApp()

        // Location services enabled check
        if (!isLocationEnabled()) {
            getFeedData("")
            return
        }

//        getFeedData("")

        // Swipe refresh layout listener
        binding.swipeRefreshLayout.setOnRefreshListener {
//            getStoryData()
//            refreshHandler.removeCallbacksAndMessages(null)
            // Scroll to top immediately when refresh starts
            binding.postRecyclerView.scrollToPosition(0)
            getFeedData("refresh")
//            binding.postRecyclerView.adapter?.notifyItemChanged(0)
        }

//        binding.switchThemeButton.setOnClickListener {
//            ThemeHelper.toggleTheme(requireActivity())
//            requireActivity().recreate() // Restart activity to apply theme change
//        }

        // Fragment Result Listener to receive data
        parentFragmentManager.setFragmentResultListener("home_key", this) { _, bundle ->
            val refresh = bundle.getString("refresh")
            binding.postRecyclerView.scrollToPosition(0)

//            Toast.makeText(requireContext(), refresh, Toast.LENGTH_SHORT).show()

            // Post the task with a 10 second delay
//            refreshHandler.postDelayed({
//                Toast.makeText(requireContext(), refresh, Toast.LENGTH_SHORT).show()
//                getFeedData("refresh")
//            }, 5000)
        }

//        // RecyclerView scroll listener to detect top position
//        binding.postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//
//                // Check if the first item is at the top
//                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
//                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
//
//                // Enable SwipeRefresh only if the first item is visible at the top
//                binding.swipeRefreshLayout.isEnabled = firstVisiblePosition == 0
//            }
//        })


        binding.postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)


                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val firstVisibleItemPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0

                val firstItemView = layoutManager?.findViewByPosition(firstVisibleItemPosition)
                val isTopVisible = firstVisibleItemPosition == 0 && firstItemView?.top == 0

                binding.swipeRefreshLayout.isEnabled = isTopVisible

//                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
//                val firstVisiblePosition = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: RecyclerView.NO_POSITION
//
//                // Ensure the first item is fully visible
//                binding.swipeRefreshLayout.isEnabled = firstVisiblePosition == 0 && recyclerView.computeVerticalScrollOffset() == 0
            }
        })

        individualViewModal.toast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }
        individualViewModal.reportToast.observe(viewLifecycleOwner){
//            CustomSnackBar.showSnackBar(binding.root,it)
            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
        }

        getSubscriptionData()



        individualViewModal.getSubscriptionDetailsResult.observe(viewLifecycleOwner){result->
            if (result.status==true){
                result.data?.uploadLimit?.let { uploadLimits ->
                    var pdfSize = DEFAULT_PDF_MB // Default size if not provided
                    var videoDuration = DEFAULT_VIDEO_LENGTH // Default duration if not provided
                    for (limit in uploadLimits) {
                        when (limit.fileType) {
                            "pdf" -> pdfSize = limit.size ?: pdfSize
                            "video" -> videoDuration = limit.size ?: videoDuration
                        }
                    }

                    preferenceManager.putInt(PreferenceManager.Keys.PDF_SIZE_INT, pdfSize)
                    preferenceManager.putInt(PreferenceManager.Keys.VIDEO_DURATION_INT, videoDuration)


                    println("PDF Size: $pdfSize MB, Video Duration: $videoDuration seconds")
                }

            }else{
                val msg = result.message
                Toast.makeText(requireContext(),msg, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun getSubscriptionData() {
        individualViewModal.getSubscriptionDetails()

    }

    private fun onIdActive(postId: List<String>) {
        postIds = postId
    }

    private fun getSetWeatherAndAQI() {

        val tempInKelvin = preferenceManager.getDouble(PreferenceManager.Keys.WEATHER_TEMP, 0.0)
        val pm25Value = preferenceManager.getDouble(PreferenceManager.Keys.AQI_PM25, 0.0)
        val aqi =  preferenceManager.getInt(PreferenceManager.Keys.AQI, 0)
        val weatherType = preferenceManager.getString(PreferenceManager.Keys.WEATHER_TYPE, "").toString()

        if (aqi != 0){
            val aqiType = aqi.toAqiType()
            val overallAqi = pm25Value.toAQI() // Calling the extension function
            binding.aqiTv.text = aqiType
            binding.aqiNumberTv.text = overallAqi.toString()
            Glide.with(this).asGif().load(R.raw.aqi).into(binding.aqiIv)
            requireContext().setWeatherAndTemperature(tempInKelvin, weatherType,binding.weatherTv,binding.weatherIv)
            binding.weatherLayout.animateVisibilityToggle(binding.aqiLayout, binding.aqiNumberLayout)

        }


    }


    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
//        refreshHandler.removeCallbacksAndMessages(null)
        if (postIds.size >= batchSize){
            individualViewModal.postViews(postIds)
        }
        VideoPlayerManager.pausePlayer()
        super.onPause()
    }

    private fun getFeedData(refresh: String) {

        if (binding.postRecyclerView.adapter == null) {
            binding.postRecyclerView.adapter = feedAdapter.withLoadStateFooter(footer = LoaderAdapter())
            binding.postRecyclerView.isNestedScrollingEnabled = false
            binding.postRecyclerView.setItemViewCacheSize(10) // Increased cache size for smoother scrolling
            binding.postRecyclerView.setHasFixedSize(false) // Allow RecyclerView to optimize layout
            binding.postRecyclerView.itemAnimator = null
        }

        binding.postRecyclerView.viewTreeObserver.addOnScrollChangedListener {
            handler.postDelayed(debounceRunnable, 100)
        }

        binding.postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                isScrolling = true
                handler.removeCallbacks(debounceRunnable)
            }
        })

        individualViewModal.getFeeds(currentLat,currentLng).observe(viewLifecycleOwner) { data ->
            lifecycleScope.launch {
                isLoading(refresh)
                feedAdapter.submitData(data)
                
                // Scroll to top after data is submitted, especially on refresh
                if (refresh.isNotEmpty()){
                    // Post to ensure layout is complete before scrolling
                    binding.postRecyclerView.post {
                        binding.postRecyclerView.scrollToPosition(0)
                    }
                }
                // Remove notifyDataSetChanged() - PagingDataAdapter handles updates automatically via DiffUtil
            }
        }
    }

    private fun updateActivePosition(newPosition: Int) {
        if (newPosition != activePosition) {
            val previousActivePosition = activePosition
            activePosition = if (newPosition == 0){
                1
            }else{
                newPosition
            }
            // Notify adapter to update views
            feedAdapter.setActivePosition(activePosition)
            feedAdapter.notifyItemChanged(previousActivePosition)
            feedAdapter.notifyItemChanged(activePosition)
        }
    }

    private fun isLoading(refresh: String) {
        feedAdapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is LoadState.Loading
            val isEmpty = loadState.refresh is LoadState.NotLoading && feedAdapter.itemCount == 0

            if (isLoading) {
//                if (refresh.isEmpty()) {
                    // Show progress bar only for non-refresh loads
//                    progressBar.show()
//                }
            } else {
                // Hide progress bar and refresh indicator when loading completes
                Handler(Looper.getMainLooper()).post {
                    progressBar.hide()
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    // Ensure scroll to top after refresh completes
                    if (refresh.isNotEmpty()) {
                        binding.postRecyclerView.post {
                            binding.postRecyclerView.scrollToPosition(0)
                        }
                    }
                }
            }

            // Handle the empty state layout visibility
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeGestures() {
        // Add swipe detection to root view
        val swipeLeftAction = {
            // Swipe right -> left: Open messages tab
            val activity = requireActivity()
            if (activity is com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity) {
                // Access the ViewPager from the activity
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                viewPager?.setCurrentItem(3, true) // Navigate to messages tab (position 3)
                // Also update the bottom navigation
                activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                    ?.selectedItemId = R.id.chatFrag
            }
        }
        
        val swipeRightAction = {
            // Swipe left -> right: Open story creation page
            val intent = Intent(requireContext(), CreateStoryActivity::class.java)
            startActivity(intent)
        }
        
        // Add to root view
        binding.root.setOnSwipeListener(
            onSwipeLeft = swipeLeftAction,
            onSwipeRight = swipeRightAction
        )
    }
    
    private var swipeInitialX = 0f
    private var swipeInitialY = 0f
    private var isHorizontalSwipe = false
    
    private fun setupRecyclerViewSwipeGestures() {
        val SWIPE_THRESHOLD = 80f
        val DETECTION_THRESHOLD = 20f
        
        binding.postRecyclerView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    swipeInitialX = event.x
                    swipeInitialY = event.y
                    isHorizontalSwipe = false
                    false // Don't consume, let RecyclerView handle it
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - swipeInitialX
                    val deltaY = event.y - swipeInitialY
                    val absDeltaX = Math.abs(deltaX)
                    val absDeltaY = Math.abs(deltaY)
                    
                    // Detect horizontal swipe early
                    if (!isHorizontalSwipe && absDeltaX > DETECTION_THRESHOLD && absDeltaX > absDeltaY * 1.1) {
                        isHorizontalSwipe = true
                        // Prevent RecyclerView and SwipeRefreshLayout from handling the touch
                        binding.postRecyclerView.parent?.requestDisallowInterceptTouchEvent(true)
                        binding.swipeRefreshLayout.isEnabled = false
                    }
                    
                    // If horizontal swipe detected, consume the event
                    if (isHorizontalSwipe) {
                        true
                    } else {
                        false // Let RecyclerView handle vertical scrolling
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isHorizontalSwipe) {
                        val deltaX = event.x - swipeInitialX
                        val absDeltaX = Math.abs(deltaX)
                        
                        // Check if swipe distance meets threshold
                        if (absDeltaX > SWIPE_THRESHOLD) {
                            if (deltaX > 0) {
                                // Swipe Right (Left to Right): Open story creation page
                                val intent = Intent(requireContext(), CreateStoryActivity::class.java)
                                startActivity(intent)
                            } else {
                                // Swipe Left (Right to Left): Open messages tab
                                val activity = requireActivity()
                                if (activity is com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity) {
                                    val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                                    viewPager?.setCurrentItem(3, true)
                                    activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                                        ?.selectedItemId = R.id.chatFrag
                                }
                            }
                        }
                        
                        // Re-enable SwipeRefreshLayout
                        binding.swipeRefreshLayout.isEnabled = true
                        binding.postRecyclerView.parent?.requestDisallowInterceptTouchEvent(false)
                        isHorizontalSwipe = false
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

}
